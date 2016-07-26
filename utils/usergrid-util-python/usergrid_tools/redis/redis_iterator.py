import json
from collections import defaultdict

import redis
import time

cache = redis.StrictRedis(host='localhost', port=6379, db=0)
# cache.flushall()

ecid_counter = defaultdict(int)
counter = 0

for key in cache.scan_iter(match='*visited'):

    # print key
    parts = key.split(':')
    ecid = parts[0]

    if ecid != 'd22a6f10-d3ef-47e3-bbe3-e1ccade5a241':
        cache.delete(key)
        ecid_counter[ecid] += 1
        counter +=1

        if counter % 100000 == 0 and counter != 0:
            print json.dumps(ecid_counter, indent=2)
            print 'Sleeping...'
            time.sleep(60)
            print 'AWAKE'

print json.dumps(ecid_counter, indent=2)
