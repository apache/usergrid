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

import redis

__author__ = 'Jeff.West@yahoo.com'


r = redis.Redis("localhost", 6379)
for key in r.scan_iter():
    # print '%s: %s' % (r.ttl(key), key)

    if key[0:4] == 'http':
        r.set(key, 1)
        # print 'set value'

    if r.ttl(key) > 3600 \
            or key[0:3] in ['v3:', 'v2', 'v1'] \
            or ':visited' in key:
        r.delete(key)
        print 'delete %s' % key
