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
import requests

### This will create an array of org-level management users

users = [
    'me@example.com'
]

for user in users:

    post_body = {
        "username": user,
        "name": user,
        "email": user,
        "password": "test12345"
    }

    print json.dumps(post_body)

    r = requests.post('http://localhost:8080/management/organizations/asdf/users',
                      headers={
                          'Authorization': 'Bearer SADFSDF',
                          'Content-Type': 'application/json'
                      },
                      data=json.dumps(post_body))

    print r.status_code

    print '%s: created (POST) [%s]: %s' % (user, r.status_code, r.text)

    #
    # r = requests.put('http://localhost:8080/management/users/%s' % user,
    #                  headers={
    #                      'Authorization': 'Bearer YWMtFlVrhK8nEeW-AhmxdmpAVAAAAVIYTHxTNSUxpQyUWZQ2LsZxcXSdNtO_lWo',
    #                      'Content-Type': 'application/json'
    #                  },
    #                  data=json.dumps('{"confirmed": true}'))
    #
    # print '%s: confirmed: %s' % (user, r.status_code)
    #
    # r = requests.put('http://localhost:8080/management/users/%s' % user,
    #                  headers={
    #                      'Authorization': 'Bearer YWMtFlVrhK8nEeW-AhmxdmpAVAAAAVIYTHxTNSUxpQyUWZQ2LsZxcXSdNtO_lWo',
    #                      'Content-Type': 'application/json'
    #                  },
    #                  data=json.dumps('{"activated": true}'))
    #
    # print '%s: activated: %s' % (user, r.status_code)
