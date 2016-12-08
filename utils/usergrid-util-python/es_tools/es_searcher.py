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

# Simple example of searching for a specific entity in ES

__author__ = 'Jeff.West@yahoo.com'

INDEX_NAME=''

url_template = 'http://localhost:9200/%s/_search' % INDEX_NAME

request = {
    "query": {
        "term": {
            "entityId": "entityId(1a78d0a6-bffb-11e5-bc61-0af922a4f655,superbad)"
        }
    }
}

# url_template = 'http://localhost:9200/_search'
# r = requests.get(url)
r = requests.get(url_template, data=json.dumps(request))

print r.status_code
print json.dumps(r.json(), indent=2)

