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

__author__ = 'Jeff.West@yahoo.com'


# This will make the API calls to activate and confirm an array of users

users = [
    'user1@example.com',
    'user2@example.com'
]

TOKEN = 'ABC123'
URL = "http://localhost:8080/management/users/%s"

s = requests.Session()
s.headers.update({'authorization': 'Bearer %s' % TOKEN})

for user in users:

    r = s.put(URL % user, data=json.dumps({"activated": True}))
    print 'Activated %s: %s' % (user, r.status_code)

    if r.status_code != 200:
        print r.text
        continue

    r = s.put(URL % user, data=json.dumps({"confirmed": True}))

    print 'Confirmed %s: %s' % (user, r.status_code)
