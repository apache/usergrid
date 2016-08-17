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

__author__ = 'Jeff.West@yahoo.com'

# The purpose of this script is to update the shard allocation of ElasticSearch for specific indexes to be set to
# specific nodes.  The reason for doing this is to isolate the nodes on which certain indexes run for specific
# customers due to load, disk size or any other factors.


nodes_c32xl = [
    'elasticsearch000eu',
    'elasticsearch001eu',
    'elasticsearch002eu'
]

nodes_c34xl = [
    'elasticsearch015eu',
    'elasticsearch018eu',
    'elasticsearch019eu'
]

nodes = nodes_c34xl

url_base = 'http://localhost:9200'

nodes_string = ",".join(nodes)

payload = {
    "index.routing.allocation.include._host": "",
    "index.routing.allocation.exclude._host": nodes_string
}

# payload = {
#     "index.routing.allocation.include._host": "",
#     "index.routing.allocation.exclude._host": ""
# }

print json.dumps(payload )


r = requests.get(url_base + "/_stats")
indices = r.json()['indices']

print 'retrieved %s indices' % len(indices)

includes = [
    # '70be096e-c2e1-11e4-8a55-12b4f5e28868',
    # 'b0c640af-bc6c-11e4-b078-12b4f5e28868',
    # 'e62e465e-bccc-11e4-b078-12b4f5e28868',
    # 'd82b6413-bccc-11e4-b078-12b4f5e28868',
    # '45914256-c27f-11e4-8a55-12b4f5e28868',
    # '2776a776-c27f-11e4-8a55-12b4f5e28868',
    # 'a54f878c-bc6c-11e4-b044-0e4cd56e19cd',
    # 'ed5b47ea-bccc-11e4-b078-12b4f5e28868',
    # 'bd4874ab-bccc-11e4-b044-0e4cd56e19cd',
    # '3d748996-c27f-11e4-8a55-12b4f5e28868',
    # '1daab807-c27f-11e4-8a55-12b4f5e28868',
    # 'd0c4f0da-d961-11e4-849d-12b4f5e28868',
    # '93e756ac-bc4e-11e4-92ae-12b4f5e28868',
    #
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c8',
    # 'b6768a08-b5d5-11e3-a495-10ddb1de66c3',
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c9',
]

excludes = [
    #
    # '70be096e-c2e1-11e4-8a55-12b4f5e28868',
    # 'b0c640af-bc6c-11e4-b078-12b4f5e28868',
    # 'e62e465e-bccc-11e4-b078-12b4f5e28868',
    # 'd82b6413-bccc-11e4-b078-12b4f5e28868',
    # '45914256-c27f-11e4-8a55-12b4f5e28868',
    # '2776a776-c27f-11e4-8a55-12b4f5e28868',
    # 'a54f878c-bc6c-11e4-b044-0e4cd56e19cd',
    # 'ed5b47ea-bccc-11e4-b078-12b4f5e28868',
    # 'bd4874ab-bccc-11e4-b044-0e4cd56e19cd',
    # '3d748996-c27f-11e4-8a55-12b4f5e28868',
    # '1daab807-c27f-11e4-8a55-12b4f5e28868',
    # 'd0c4f0da-d961-11e4-849d-12b4f5e28868',
    # '93e756ac-bc4e-11e4-92ae-12b4f5e28868',
    #
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c8',
    # 'b6768a08-b5d5-11e3-a495-10ddb1de66c3',
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c9',
]

counter = 0
update = False

for index_name in indices:
    update = False
    counter += 1

    # print 'Checking index %s of %s: %s' % (counter, len(indices), index_name)

    if len(includes) == 0:
        update = True
    else:
        for include in includes:

            if include in index_name:
                update = True

    if len(excludes) > 0:
        for exclude in excludes:
            if exclude in index_name:
                update = False

    if not update:
        print 'Skipping %s of %s: %s' % (counter, len(indices), index_name)
    else:
        print '+++++Processing %s of %s: %s' % (counter, len(indices), index_name)

        url_template = '%s/%s/_settings' % (url_base, index_name)
        print url_template

        success = False

        while not success:

            response = requests.put('%s/%s/_settings' % (url_base, index_name), data=json.dumps(payload))

            if response.status_code == 200:
                success = True
                print '200: %s: %s' % (index_name, response.text)
            else:
                print '%s: %s: %s' % (response.status_code, index_name, response.text)
