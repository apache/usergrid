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

# Utility to iterate the mappings for an index and save them locally

url_base = 'http://localhost:9200'

# r = requests.get(url_base + "/_stats")
#
# indices = r.json()['indices']

url_template = '%s/{index_name}/_mapping' % url_base

SOURCE_INDEX = '5f20f423-f2a8-11e4-a478-12a5923b55dc__application_v7'

source_index_url = '%s/%s' % (url_base, SOURCE_INDEX)

index_name = SOURCE_INDEX
print 'Getting ' + url_template.format(index_name=index_name)

r = requests.get(url_template.format(index_name=index_name))
index_data = r.json()

mappings = index_data.get(index_name, {}).get('mappings', {})

for type_name, mapping_detail in mappings.iteritems():
    print 'Index: %s | Type: %s: | Properties: %s' % (index_name, type_name, len(mappings[type_name]['properties']))

    print 'Processing %s' % type_name

    filename = '/Users/ApigeeCorporation/tmp/%s_%s_source_mapping.json' % (
        SOURCE_INDEX, type_name)

    print filename

    with open(filename, 'w') as f:
        json.dump({type_name: mapping_detail}, f, indent=2)

    # print '%s' % (r.status_code, r.text)

    # print json.dumps(r.json(), indent=2)
    # time.sleep(5)
    print 'Done!'
