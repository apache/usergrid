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
from multiprocessing import Pool

import requests

# URL Templates for Usergrid
import time

org_management_app_url_template = "{api_url}/management/organizations/{org}/applications?client_id={client_id}&client_secret={client_secret}"
org_management_url_template = "{api_url}/management/organizations/{org}/applications?client_id={client_id}&client_secret={client_secret}"
org_url_template = "{api_url}/{org}?client_id={client_id}&client_secret={client_secret}"
app_url_template = "{api_url}/{org}/{app}?client_id={client_id}&client_secret={client_secret}"
collection_url_template = "{api_url}/{org}/{app}/{collection}?client_id={client_id}&client_secret={client_secret}"
collection_query_url_template = "{api_url}/{org}/{app}/{collection}?ql={ql}&client_id={client_id}&client_secret={client_secret}&limit={limit}"
collection_graph_url_template = "{api_url}/{org}/{app}/{collection}?client_id={client_id}&client_secret={client_secret}&limit={limit}"
connection_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}?client_id={client_id}&client_secret={client_secret}"
connecting_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/connecting/{verb}?client_id={client_id}&client_secret={client_secret}"
connection_create_by_uuid_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_uuid}?client_id={client_id}&client_secret={client_secret}"
connection_create_by_name_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_type}/{target_name}?client_id={client_id}&client_secret={client_secret}"
get_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}&connections=none"
get_entity_url_with_connections_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"
put_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"
permissions_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/permissions?client_id={client_id}&client_secret={client_secret}"

user_credentials_url_template = "{api_url}/{org}/{app}/users/{uuid}/credentials"

org = 'myOrg'

config = {
    "endpoint": {
        "api_url": "https://host",
    },
    "credentials": {
        "myOrg": {
            "client_id": "foo-zw",
            "client_secret": "bar"
        }
    }
}

api_url = config.get('endpoint').get('api_url')

all_creds = config.get('credentials')

creds = config.get('credentials').get(org)


def post(**kwargs):
    # print kwargs
    # print "curl -X POST \"%s\" -d '%s'" % (kwargs.get('url'), kwargs.get('data'))
    return requests.post(**kwargs)


def build_role(name, title):
    role = {
        'name': name,
        'roleName': name,
        'inactivity': 0,
        'title': title
    }

    return role


def set_default_role(app):
    print app
    role_name = 'guest'
    role = build_role('guest', 'Guest')

    # # put default role
    role_url = put_entity_url_template.format(org=org,
                                              app=app,
                                              uuid=role_name,
                                              collection='roles',
                                              api_url=api_url,
                                              **creds)
    print 'DELETE ' + role_url

    # # r = requests.delete(role_url)
    # #
    # # if r.status_code != 200:
    # #     print 'ERROR ON DELETE'
    # #     print r.text
    # #
    # # time.sleep(3)
    #
    # # # put default role
    # role_collection_url = collection_url_template.format(org=org,
    #                                                      app=app,
    #                                                      collection='roles',
    #                                                      api_url=api_url,
    #                                                      **creds)
    # print 'POST ' + role_collection_url
    #
    # r = post(url=role_collection_url, data=json.dumps(role))
    #
    # if r.status_code != 200:
    #     print r.text

    permissions_url = permissions_url_template.format(org=org,
                                                      limit=1000,
                                                      app=app,
                                                      collection='roles',
                                                      uuid=role_name,
                                                      api_url=api_url,
                                                      **creds)

    r = post(url=permissions_url, data=json.dumps({'permission': 'post:/users'}))

    r = post(url=permissions_url, data=json.dumps({'permission': 'put:/devices/*'}))
    r = post(url=permissions_url, data=json.dumps({'permission': 'put,post:/devices'}))

    r = post(url=permissions_url, data=json.dumps({'permission': 'put:/device/*'}))
    r = post(url=permissions_url, data=json.dumps({'permission': 'put,post:/device'}))

    if r.status_code != 200:
        print r.text


def list_apps():
    apps = []
    source_org_mgmt_url = org_management_url_template.format(org=org,
                                                             limit=1000,
                                                             api_url=api_url,
                                                             **creds)

    r = requests.get(source_org_mgmt_url)

    print r.text

    data = r.json().get('data')

    for app_uuid in data:

        if 'self-care' in app_uuid:
            parts = app_uuid.split('/')
            apps.append(parts[1])

    return apps


apps = list_apps()

pool = Pool(12)

pool.map(set_default_role, apps)
