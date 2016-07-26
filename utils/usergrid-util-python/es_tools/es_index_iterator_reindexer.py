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
import re
from multiprocessing.pool import Pool
import requests

# This script iterates an index and issues a PUT request for an empty string to force a reindex of the entity

index_url_template = 'http://elasticsearch013wo:9200/{index_name}/_search?size={size}&from={from_var}'

index_names = [
    'es-index-name'
]

baas_url = 'http://localhost:8080/org/{app_id}/{collection}/{entity_id}'

counter = 0
size = 1000

total_docs = 167501577
from_var = 0
page = 0

work_items = []


def work(item):
    url = 'http://localhost:8080/org/{app_id}/{collection}/{entity_id}'.format(
        app_id=item[0],
        collection=item[1],
        entity_id=item[2]
    )

    r_put = requests.put(url, data=json.dumps({'russo': ''}))

    if r_put.status_code == 200:
        print '[%s]: %s' % (r_put.status_code, url)

    elif r_put.status_code:
        print '[%s]: %s | %s' % (r_put.status_code, url, r.text)


while from_var < total_docs:

    from_var = page * size
    page += 1

    for index_name in index_names:

        index_url = index_url_template.format(index_name=index_name, size=size, from_var=from_var)

        print 'Getting URL: ' + index_url

        r = requests.get(index_url)

        if r.status_code != 200:
            print r.text
            exit()

        response = r.json()

        hits = response.get('hits', {}).get('hits')

        re_app_id = re.compile('appId\((.+),')
        re_ent_id = re.compile('entityId\((.+),')
        re_type = re.compile('entityId\(.+,(.+)\)')

        print 'Index: %s | hits: %s' % (index_name, len(hits))

        for hit_data in hits:
            source = hit_data.get('_source')

            application_id = source.get('applicationId')

            app_id_find = re_app_id.findall(application_id)

            if len(app_id_find) > 0:
                app_id = app_id_find[0]

                if app_id != '5f20f423-f2a8-11e4-a478-12a5923b55dc':
                    continue

                entity_id_tmp = source.get('entityId')

                entity_id_find = re_ent_id.findall(entity_id_tmp)
                entity_type_find = re_type.findall(entity_id_tmp)

                if len(entity_id_find) > 0 and len(entity_type_find) > 0:
                    entity_id = entity_id_find[0]
                    collection = entity_type_find[0]

                    if collection in ['logs', 'log']:
                        print 'skipping logs...'
                        continue

                    work_items.append((app_id, collection, entity_id))

                    counter += 1

pool = Pool(16)

print 'Work Items: %s' % len(work_items)

print 'Starting Work'

pool.map(work, work_items)

print 'done: %s' % counter
