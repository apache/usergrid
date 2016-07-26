import json
from collections import defaultdict
import requests
import logging

__author__ = 'Jeff West @ ApigeeCorporation'

# This script iterates all the indexes in an ES cluster and aggregates the size by the prefix

url_base = 'http://localhost:9200'

r = requests.get(url_base + "/_stats")
response = r.json()

indices = r.json()['indices']

print 'retrieved %s indices' % len(indices)

NUMBER_VALUE = 0

includes = [
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c9',
]

excludes = [
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c8',
]

counter = 0
process = False

counts = defaultdict(int)
sizes = defaultdict(int)
indexes = {}

for index, index_data in indices.iteritems():
    process = False
    counter += 1

    if 'management' in index:
        print index

    # print 'index %s of %s' % (counter, len(indices))

    if len(includes) == 0:
        process = True
    else:
        for include in includes:

            if include in index:
                process = True

    if len(excludes) > 0:
        for exclude in excludes:
            if exclude in index:
                process = False

    if process:
        # print index
        if '__' in index:
            index_prefix = index.split('__')[0]
        elif '^' in index:
            index_prefix = index.split('^')[0]
        else:
            index_prefix = index.split('_')[0]

        if index_prefix not in indexes:
            indexes[index_prefix] = []

        indexes[index_prefix].append(index)

        counts[index_prefix] += 1
        counts['total'] += 1
        sizes[index_prefix] += (float(index_data.get('total', {}).get('store', {}).get('size_in_bytes')) / 1e+9)
        sizes['total'] += (float(index_data.get('total', {}).get('store', {}).get('size_in_bytes')) / 1e+9)

print 'Number of indices (US-EAST):'
print json.dumps(counts, indent=2)
print 'Size in GB'
print json.dumps(sizes, indent=2)
print json.dumps(indexes, indent=2)
