#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#   contributor license agreements.  The ASF licenses this file to You
#  under the Apache License, Version 2.0 (the "License"); you may not
#  use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.  For additional information regarding
#  copyright in this work, please see the NOTICE file in the top level
#  directory of this distribution.

import logging
from usergrid import UsergridError, UsergridCollection
from usergrid.app_templates import app_url_template


class UsergridApplication(object):
    def __init__(self, app_id, client):
        self.app_id = app_id
        self.client = client
        self.logger = logging.getLogger('usergrid.UsergridClient')

    def list_collections(self):
        url = app_url_template.format(app_id=self.app_id,
                                      **self.client.url_data)
        r = self.client.get(url)

        if r.status_code == 200:
            api_response = r.json()
            collection_list = api_response.get('entities')[0].get('metadata', {}).get('collections', {})
            collections = {}

            for collection_name in collection_list:
                collections[collection_name] = UsergridCollection(self.client.org_id,
                                                                  self.app_id,
                                                                  collection_name,
                                                                  self.client)

            return collections

        else:
            raise UsergridError(message='Unable to post to list collections',
                                status_code=r.status_code,
                                api_response=r,
                                url=url)

    def collection(self, collection_name):
        return UsergridCollection(self.client.org_id,
                                  self.app_id,
                                  collection_name,
                                  self.client)

    def authenticate_app_client(self,
                                **kwargs):

        return self.client.authenticate_app_client(self.app_id, **kwargs)
