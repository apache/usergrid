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
