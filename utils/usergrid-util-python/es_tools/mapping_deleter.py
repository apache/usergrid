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

url_base = 'http://localhost:9200'

SOURCE_INDEX = '5f20f423-f2a8-11e4-a478-12a5923b55dc__application_v6'

url_template = '%s/{index_name}/_mapping' % url_base

source_index_url = '%s/%s' % (url_base, SOURCE_INDEX)

index_name = SOURCE_INDEX

index_data = requests.get(url_template.format(index_name=index_name)).json()

mappings = index_data.get(index_name, {}).get('mappings', {})

for type_name, mapping_detail in mappings.iteritems():
    print 'Index: %s | Type: %s: | Properties: %s' % (index_name, type_name, len(mappings[type_name]['properties']))

    if type_name == '_default_':
        continue

    r = requests.delete('%s/%s/_mapping/%s' % (url_base, index_name, type_name))

    print '%s: %s' % (r.status_code, r.text)

    # print json.dumps(r.json(), indent=2)
    # time.sleep(5)
    print '---'
