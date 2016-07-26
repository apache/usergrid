import json

import requests


__author__ = 'Jeff West @ ApigeeCorporation'

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
