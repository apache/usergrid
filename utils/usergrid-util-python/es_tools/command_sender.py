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

__author__ = 'Jeff West @ ApigeeCorporation'


# Simple utility to send commands, useful to not have to recall the proper format

#
# url = 'http://localhost:9200/_cat/shards'
#
# r = requests.get(url)
#
# response = r.text
#
# print response

data = {
    "commands": [
        {
            "move": {
                "index": "usergrid__a34ad389-b626-11e4-848f-06b49118d7d0__application_target_final",
                "shard": 14,
                "from_node": "elasticsearch018",
                "to_node": "elasticsearch021"
            }
        },
        {
            "move": {
                "index": "usergrid__a34ad389-b626-11e4-848f-06b49118d7d0__application_target_final",
                "shard": 12,
                "from_node": "elasticsearch018",
                "to_node": "elasticsearch009"
            }
        },

    ]
}

r = requests.post('http://localhost:9211/_cluster/reroute', data=json.dumps(data))

print r.text