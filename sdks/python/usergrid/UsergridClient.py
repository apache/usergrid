# */
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *   http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing,
# * software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# * KIND, either express or implied.  See the License for the
#    * specific language governing permissions and limitations
# * under the License.
# */

import json
import logging
import requests
from usergrid.UsergridAuth import UsergridAppAuth
from usergrid.app_templates import get_entity_url_template, post_collection_url_template, put_entity_url_template, \
    delete_entity_url_template, connect_entities_by_type_template, assign_role_url_template

__author__ = 'Jeff.West@yahoo.com'


def value_error(message):
    raise ValueError(message)


def usergrid_error(r):
    pass


class Usergrid(object):
    client = None

    @staticmethod
    def init(org_id,
             app_id,
             **kwargs):
        Usergrid.client = UsergridClient(org_id, app_id, **kwargs)

    @staticmethod
    def GET(collection, uuid_name, **kwargs):
        return Usergrid.client.GET(collection, uuid_name, **kwargs)

    @staticmethod
    def PUT(collection, uuid_name, data, **kwargs):
        return Usergrid.client.PUT(collection, uuid_name, data, **kwargs)

    @staticmethod
    def POST(collection, data, **kwargs):
        return Usergrid.client.POST(collection, data, **kwargs)

    @staticmethod
    def DELETE(collection, uuid_name, **kwargs):
        return Usergrid.client.DELETE(collection, uuid_name, **kwargs)

    @staticmethod
    def connect_entities(from_entity, relationship, to_entity, **kwargs):
        return Usergrid.client.connect_entities(from_entity, relationship, to_entity, **kwargs)

    @staticmethod
    def disconnect_entities(from_entity, relationship, to_entity, **kwargs):
        return Usergrid.client.disconnect_entities(from_entity, relationship, to_entity, **kwargs)

    @staticmethod
    def assign_role(role_uuid_name, user_entity, **kwargs):
        return Usergrid.client.assign_role(role_uuid_name, user_entity, **kwargs)


class UsergridResponse(object):
    def __init__(self, api_response, client):
        self.api_response = api_response
        self.client = client

        if api_response is None:
            self.ok = False
            self.body = 'No Response'

        else:
            self.headers = api_response.headers

            if api_response.status_code == 200:
                self.ok = True
                self.body = api_response.json()
                self.entities = self.body.get('entities', [])

            else:
                self.ok = False

                if api_response.headers.get('Content-type') == 'application/json':
                    self.body = api_response.json()
                else:
                    self.body = 'HTTP %s: %s' % (api_response.status_code, api_response.text)

    def __str__(self):
        return json.dumps(self.body)

    def first(self):
        return UsergridEntity(entity_data=self.entities[0]) if self.ok and self.entities and len(
                self.entities) > 0 else None

    def entity(self):
        return self.first()

    def last(self):
        return UsergridEntity(entity_data=self.entities[len(self.entities) - 1]) if self.ok and self.entities and len(
                self.entities) > 0 else None

    def has_next_page(self):
        return 'cursor' in self.body if self.ok else False


class UsergridEntity(object):
    def __init__(self, entity_data):
        self.entity_data = entity_data

    def __str__(self):
        return json.dumps(self.entity_data)

    def get(self, name, default=None):
        return self.entity_data.get(name, default)

    def entity_id(self):

        if self.entity_data.get('type', '').lower() in ['users', 'user']:
            return self.entity_data.get('uuid', self.entity_data.get('username'))

        return self.entity_data.get('uuid', self.entity_data.get('name'))

    def can_mutate_or_load(self):
        entity_id = self.entity_id()

        if entity_id is None or self.entity_data.get('type') is None:
            return False

        return True

    def put_property(self, name, value):
        self.entity_data[name] = value

    def put_properties(self, properties):
        if isinstance(properties, dict):
            self.entity_data.update(properties)

    def remove_property(self, name):

        if name is not None and name in self.entity_data:
            del self.entity_data[name]

    def remove_properties(self, properties):
        if isinstance(properties, (list, dict)):
            for property_name in properties:
                self.remove_property(property_name)

    def append(self, array_name, value):
        if array_name in self.entity_data:
            if isinstance(self.entity_data[array_name], list):
                self.entity_data[array_name].append(value)
        else:
            self.entity_data[array_name] = [value]

    def prepend(self, array_name, value):
        if array_name in self.entity_data:
            if isinstance(self.entity_data[array_name], list):
                self.entity_data[array_name].pre(value)
        else:
            self.entity_data[array_name] = [value]

    def insert(self, array_name, value, index):
        if array_name in self.entity_data:
            if isinstance(self.entity_data[array_name], list):
                self.entity_data[array_name].insert(index, value)

    def shift(self, array_name):
        if array_name in self.entity_data:
            if isinstance(self.entity_data[array_name], list):
                value = self.entity_data[array_name][0]
                self.entity_data[array_name] = self.entity_data[array_name][1:]
                return value

        return None

    def reload(self):
        if not self.can_mutate_or_load():
            raise ValueError('Unable to reload entity: No uuid nor name')

        response = Usergrid.GET(collection=self.entity_data.get('type'),
                                uuid_name=self.entity_id())
        if response.ok:
            self.entity_data.update(response.entity().entity_data)

        else:
            raise ValueError('Unable to reload entity: %s' % response)

    def save(self):
        if not self.can_mutate_or_load():
            raise ValueError('Unable to save entity: No uuid nor name')

        response = Usergrid.PUT(collection=self.entity_data.get('type'),
                                uuid_name=self.entity_id(),
                                data=self.entity_data)

        if response.ok and 'uuid' not in self.entity_data:
            self.entity_data['uuid'] = response.entity().get('uuid')

        return response

    def remove(self):
        if not self.can_mutate_or_load():
            raise ValueError('Unable to delete entity: No uuid nor name')

        return Usergrid.DELETE(collection=self.entity_data.get('type'),
                               uuid_name=self.entity_id())

    def get_connections(self, relationship, direction='connecting'):
        pass

    def connect(self, relationship, to_entity):

        if not to_entity.can_mutate_or_load():
            raise ValueError('Unable to connect to entity - no uuid or name')

        if not self.can_mutate_or_load():
            raise ValueError('Unable from connect to entity - no uuid or name')

        return Usergrid.connect_entities(self, relationship, to_entity)

    def disconnect(self, relationship, to_entity):
        if not to_entity.can_mutate_or_load():
            raise ValueError('Unable to connect to entity - no uuid or name')

        if not self.can_mutate_or_load():
            raise ValueError('Unable from connect to entity - no uuid or name')

        return Usergrid.disconnect_entities(self, relationship, to_entity)

    def attach_asset(self, filename, data, content_type):
        pass

    def download_asset(self, content_type=None):
        pass


class UsergridClient(object):
    def __init__(self,
                 org_id,
                 app_id,
                 base_url='http://api.usergrid.com',
                 client_id=None,
                 client_secret=None,
                 token_ttl_seconds=86400,
                 auth_fallback="none"):

        self.base_url = base_url
        self.org_id = org_id
        self.app_id = app_id
        self.auth_fallback = auth_fallback
        self.logger = logging.getLogger('usergrid.UsergridClient')
        self.session = requests.Session()

        self.url_data = {
            'base_url': base_url,
            'org_id': org_id,
            'app_id': app_id
        }

        if client_id and not client_secret:
            value_error('Client ID Specified but not Secret')

        elif client_secret and not client_id:
            value_error('Client ID Specified but not Secret')

        elif client_secret and client_id:
            self.auth = UsergridAppAuth(client_id=client_id,
                                        client_secret=client_secret,
                                        token_ttl_seconds=token_ttl_seconds)

            self.auth.authenticate(self)
            self.session.headers.update({'Authorization': 'Bearer %s' % self.auth.access_token})

    def __str__(self):
        return json.dumps({
            'base_url': self.base_url,
            'org_id': self.org_id,
            'app_id': self.app_id,
            'access_token': self.auth.access_token
        })

    def GET(self, collection, uuid_name, connections='none', auth=None, **kwargs):
        url = get_entity_url_template.format(collection=collection,
                                             uuid_name=uuid_name,
                                             connections=connections,
                                             **self.url_data)
        if auth:
            r = requests.get(url, headers={'Authorization': 'Bearer %s' % auth.access_token})

        else:
            r = self.session.get(url)

        return UsergridResponse(r, self)

    def PUT(self, collection, uuid_name, data, auth=None, **kwargs):
        url = put_entity_url_template.format(collection=collection,
                                             uuid_name=uuid_name,
                                             **self.url_data)

        if auth:
            r = requests.put(url,
                             data=json.dumps(data),
                             headers={'Authorization': 'Bearer %s' % auth.access_token})
        else:
            r = self.session.put(url, data=json.dumps(data))

        return UsergridResponse(r, self)

    def POST(self, collection, data, auth=None, **kwargs):
        url = post_collection_url_template.format(collection=collection,
                                                  **self.url_data)

        if auth:
            r = requests.post(url,
                              data=json.dumps(data),
                              headers={'Authorization': 'Bearer %s' % auth.access_token})
        else:
            r = self.session.post(url, data=json.dumps(data))

        return UsergridResponse(r, self)

    def DELETE(self, collection, uuid_name, auth=None, **kwargs):
        url = delete_entity_url_template.format(collection=collection,
                                                uuid_name=uuid_name,
                                                **self.url_data)

        if auth:
            r = requests.delete(url, headers={'Authorization': 'Bearer %s' % auth.access_token})
        else:
            r = self.session.delete(url)

        return UsergridResponse(r, self)

    def connect_entities(self, from_entity, relationship, to_entity, auth=None, **kwargs):

        url = connect_entities_by_type_template.format(from_collection=from_entity.get('type'),
                                                       from_uuid_name=from_entity.entity_id(),
                                                       relationship=relationship,
                                                       to_collection=to_entity.get('type'),
                                                       to_uuid_name=to_entity.entity_id(),
                                                       **self.url_data)

        if auth:
            r = requests.post(url, headers={'Authorization': 'Bearer %s' % auth.access_token})
        else:
            r = self.session.post(url)

        return UsergridResponse(r, self)

    def assign_role(self, role_uuid_name, entity, auth=None, **kwargs):
        url = assign_role_url_template.format(role_uuid_name=role_uuid_name,
                                              entity_type=entity.get('type'),
                                              entity_uuid_name=entity.entity_id(),
                                              **self.url_data)

        if auth:
            r = requests.delete(url, headers={'Authorization': 'Bearer %s' % auth.access_token})
        else:
            r = self.session.delete(url)

        return UsergridResponse(r, self)

    def disconnect_entities(self, from_entity, relationship, to_entity, auth=None, **kwargs):
            url = connect_entities_by_type_template.format(from_collection=from_entity.get('type'),
                                                           from_uuid_name=from_entity.entity_id(),
                                                           relationship=relationship,
                                                           to_collection=to_entity.get('type'),
                                                           to_uuid_name=to_entity.entity_id(),
                                                           **self.url_data)

            if auth:
                r = requests.delete(url, headers={'Authorization': 'Bearer %s' % auth.access_token})
            else:
                r = self.session.delete(url)

            return UsergridResponse(r, self)


class UsergridUser(object):
    def __init__(self):
        pass


class UsergridAsset(object):
    def __init__(self, filename, data, content_type):
        self.filename = filename
        self.data = data
        self.content_type = content_type
