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
from collections import defaultdict

import redis
import time

__author__ = 'Jeff.West@yahoo.com'


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
