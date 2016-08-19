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
import time
import numpy
import requests

__author__ = 'Jeff.West@yahoo.com'

# This will call a URL over and over to check the latency of the call


def total_milliseconds(td):
    return (td.microseconds + td.seconds * 1000000) / 1000

url_template = "{protocol}://{host}:{port}/{org}/{app}/{collection}?ql={ql}&client_id={client_id}&client_secret={client_secret}"

environments = {

    'local': {
        'protocol': 'http',
        'host': 'localhost',
        'port': 8080,
        'org': 'myOrg',
        'app': 'myApp',
        'collection': 'myEntities',
        'ql': 'select *',
        'client_id': '<<client_id>>',
        'client_secret': '<<client_secret>>'
    }
}

ENV = 'local'

data = environments.get(ENV)
if data is None:
    print 'didn\'t find map entry for data'
    exit(1)

x = 0

SLEEP = .5
count_under_one = 0.0
count_over = 0.0
percent_under_one = 100.0
total_time = 0

print url_template.format(**data)

response_times = []

while True:
    x += 1
    target_url = url_template.format(**data)

    r = requests.get(url=target_url)

    response_time = total_milliseconds(r.elapsed)
    total_time += response_time

    # print '%s / %s' % (r.elapsed, total_milliseconds(r.elapsed))

    the_date = datetime.datetime.utcnow()

    if r.status_code != 200:
        print '%s | %s: %s in %s |  %s' % (the_date, x, r.status_code, response_time, r.text)
    else:
        response_times.append(response_time)

        if response_time < 2000:
            count_under_one += 1
        elif response_time > 10000:
            count_over += 1

        percent_under_one = round(100 * (count_under_one / x), 2)
        percent_over = round(100 * (count_over / x), 2)

        # print '%s | %s: %s in %s | Count: %s | Avg: %s | under 2s: %s / %s%% | over 10s: %s / %s%%' % (
        # the_date, x, r.status_code, response_time, len(r.json().get('entities')), (total_time / x), count_under_one,
        # percent_under_one, count_over, percent_over)

        print '%s | %s: %s in %s | Count: %s | Avg: %s | 99th: %s | 90th: %s | 50th: %s | 75th: %s | 25th: %s' % (
            the_date, x, r.status_code, response_time, r.json().get('count'), (total_time / x),

            numpy.percentile(response_times, 99),
            numpy.percentile(response_times, 90),
            numpy.percentile(response_times, 75),
            numpy.percentile(response_times, 50),
            numpy.percentile(response_times, 25))

    time.sleep(SLEEP)
