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

cluster = 'cluster-1'

work = {
    'remove': {
        'app-id-1': 'from-index',
        'app-id-2': 'from-index'
    },
    'add': {
        'app-id-1': 'to-index',
        'app-id-2': 'to-index'
    }
}

actions = []

for app_id, index in work.get('remove', {}).iteritems():
    actions.append({
        "remove": {
            "index": index,
            "alias": "%s_%s_read_alias" % (cluster, app_id)
        },
    })
    actions.append({
        "remove": {
            "index": index,
            "alias": "%s_%s_write_alias" % (cluster, app_id)
        },
    })

for app_id, index in work['add'].iteritems():
    actions.append({
        "add": {
            "index": index,
            "alias": "%s_%s_read_alias" % (cluster, app_id)
        },
    })
    actions.append({
        "add": {
            "index": index,
            "alias": "%s_%s_write_alias" % (cluster, app_id)
        },
    })

url = 'http://localhost:9200/_aliases'

r = requests.post(url, data=json.dumps({'actions': actions}))

print '%s: %s' % (r.status_code, r.text)
