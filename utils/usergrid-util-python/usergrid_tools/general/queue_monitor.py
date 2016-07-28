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

import argparse
import json
import datetime
import os
import time
import sys

import boto
from boto import sqs

### This monitors an SQS queue and measures the delta message count between polling intervals to infer the amount of time
### remaining to fully drain the queue

__author__ = 'Jeff.West@yahoo.com'


def total_seconds(td):
    return (td.microseconds + (td.seconds + td.days * 24.0 * 3600) * 10.0 ** 6) / 10.0 ** 6


def total_milliseconds(td):
    return (td.microseconds + td.seconds * 1000000) / 1000


def get_time_remaining(count, rate):
    if rate == 0:
        return 'NaN'

    seconds = count * 1.0 / rate

    m, s = divmod(seconds, 60)
    h, m = divmod(m, 60)

    return "%d:%02d:%02d" % (h, m, s)


def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Loader - Queue Monitor')

    parser.add_argument('-c', '--config',
                        help='The queue to load into',
                        type=str,
                        default='%s/.usergrid/queue_monitor.json' % os.getenv("HOME"))

    parser.add_argument('-q', '--queue_name',
                        help='The queue name to send messages to.  If not specified the filename is used',
                        default='entities',
                        type=str)

    my_args = parser.parse_args(sys.argv[1:])

    print str(my_args)

    return vars(my_args)


def main():

    args = parse_args()

    queue_name = args.get('queue_name')

    print 'queue_name=%s' % queue_name

    start_time = datetime.datetime.utcnow()
    first_start_time = start_time

    print "first start: %s" % first_start_time

    with open(args.get('config'), 'r') as f:
        config = json.load(f)

    sqs_config = config.get('sqs')
    last_time = datetime.datetime.utcnow()

    sqs_conn = boto.sqs.connect_to_region(**sqs_config)

    queue = sqs_conn.get_queue(queue_name)

    last_size = queue.count()
    first_size = last_size

    print 'Starting Size: %s' % last_size

    sleep = 10
    time.sleep(sleep)
    rate_sum = 0
    rate_count = 0

    while True:
        size = queue.count()
        time_stop = datetime.datetime.utcnow()

        time_delta = total_seconds(time_stop - last_time)
        agg_time_delta = total_seconds(time_stop - first_start_time)
        agg_size_delta = first_size - size
        agg_messages_rate = 1.0 * agg_size_delta / agg_time_delta

        size_delta = last_size - size
        messages_rate = 1.0 * size_delta / time_delta
        rate_sum += messages_rate
        rate_count += 1

        print '%s | %s | Size: %s | Processed: %s | Last: %s | Avg: %s | Count: %s | agg rate: %s | Remaining: %s' % (
            datetime.datetime.utcnow(),
            queue_name,
            size, size_delta, round(messages_rate, 2),
            round(rate_sum / rate_count, 2), rate_count,
            round(agg_messages_rate, 2),
            get_time_remaining(size, agg_messages_rate))

        last_size = size
        last_time = time_stop

        time.sleep(sleep)


if __name__ == '__main__':
    main()
