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

import datetime
import requests
import time

__author__ = 'Jeff West @ ApigeeCorporation'

# Utility for monitoring pending tasks in ElasticSearch

def total_milliseconds(td):
    return (td.microseconds + td.seconds * 1000000) / 1000


url_template = "http://localhost:9200/_cat/pending_tasks?v'"

x = 0

SLEEP_TIME = 3

while True:
    x += 13
    try:

        r = requests.get(url=url_template)
        lines = r.text.split('\n')

        print '\n-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-'
        print '+++++++++++++++++++++++++++++++++++++++++++++++++++++++++'
        print datetime.datetime.utcnow()
        if len(lines) > 1:
            print r.text
        else:
            print 'None'

        print '-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-'
        print '-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n'

    except:
        pass

    time.sleep(SLEEP_TIME)

