import requests
import logging

__author__ = 'Jeff West @ ApigeeCorporation'


# utility for deleting indexes that are no longer needed.  Given:
# A) a set of strings to include when evaluating the index names to delete
# B) a set of strings to Exclude when evaluating the index names to delete
#
# The general logic is:
# 1) If the include set is empty, or if the index name contains a string in the 'include' set, then delete
# 2) If the index contains a string in the exclude list, do not delete

url_base = 'http://localhost:9200'

r = requests.get(url_base + "/_stats")

indices = r.json()['indices']

print 'retrieved %s indices' % len(indices)

NUMBER_VALUE = 0

includes = [
    'rug002sr_euwi',
    # 'rug002mr',
    # 'b6768a08-b5d5-11e3-a495-10ddb1de66c3',
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c9',
]

excludes = [
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c8',
    # 'b6768a08-b5d5-11e3-a495-10ddb1de66c3',
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c9',
    # 'a34ad389-b626-11e4-848f-06b49118d7d0'
]

counter = 0
process = False
delete_counter = 0

for index in indices:
    process = False
    counter += 1

    print 'index %s of %s' % (counter, len(indices))

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
        delete_counter += 1

        url_template = '%s/%s' % (url_base, index)
        print 'DELETING Index [%s] %s at URL %s' % (delete_counter, index, url_template)

        response = requests.delete('%s/%s' % (url_base, index))
