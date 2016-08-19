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

from usergrid import UsergridQueryIterator

__author__ = 'Jeff.West@yahoo.com'


### This iterates a collection using GRAPH and checks whether there are more than on entity with the same name

url = 'https://host/org/app/collection?access_token=foo&limit=1000'

q = UsergridQueryIterator(url)

name_tracker = {}
counter = 0
for e in q:
    counter += 1

    if counter % 1000 == 1:
        print 'Count: %s' % counter

    name = e.get('name')

    if name in name_tracker:
        name_tracker[name].append(e.get('uuid'))

        print 'duplicates for name=[%s]: %s' % (name, name_tracker[name])

    else:
        name_tracker[name] = [e.get('uuid')]
