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

import time
import json

import requests

__author__ = 'Jeff.West@yahoo.com'


tstamp = time.gmtime() * 1000

s = requests.Session()

s.headers.update({'authorization': 'Bearer TOKEN'})
s.headers.update({'content-type': 'application/json'})

url = 'https://host/appservices-new/usergrid/pushtest/events'

body = {
    "timestamp": tstamp,
    "counters": {
        "counters.jeff.west": 1
    }
}

r = s.post(url, data=json.dumps(body))

print r.status_code

time.sleep(30)

r = s.get('https://host/appservices-new/usergrid/pushtest/counters?counter=counters.jeff.west')

print r.text
