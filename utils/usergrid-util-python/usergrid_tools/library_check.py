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

import traceback

__author__ = 'Jeff.West@yahoo.com'


url_data = {
    "api_url": "https://usergrid-e2e-prod.e2e.apigee.net/appservices-2-1/",
    "org": "",
    "app": "",
    "client_id": "",
    "client_secret": "",

}

collection_url_template = "{api_url}/{org}/{app}/{collection}"

try:
    from usergrid import UsergridQueryIterator

    q = UsergridQueryIterator('')

    print 'Check OK'

except Exception, e:
    print traceback.format_exc(e)
    print 'Check Failed'
