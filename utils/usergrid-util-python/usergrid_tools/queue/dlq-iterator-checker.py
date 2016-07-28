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

from multiprocessing.pool import Pool
import argparse
import json
import datetime
import os
import time
import sys

import boto
from boto import sqs
import requests

__author__ = 'Jeff.West@yahoo.com'

sqs_conn = None
sqs_queue = None

# THIS WAS USED TO TAKE MESSAGES OUT OF THE DEAD LETTER AND TEST WHETHER THEY EXISTED OR NOT

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
                        default='4g.json')

    my_args = parser.parse_args(sys.argv[1:])

    print str(my_args)

    return vars(my_args)


def check_exists(sqs_message):
    # checks whether an entity is deleted.  if the entity is found then it prints an error message.
    # this was used when there were many messages going to DLQ and the reason was because the entity had been deleted
    try:
        message = json.loads(sqs_message.get_body())
    except ValueError:
        print 'Unable to decode JSON: %s' % sqs_message.get_body()
        return
    try:
        for event_name, event_data in message.iteritems():
            entity_id_scope = event_data.get('entityIdScope')
            app_id = entity_id_scope.get('applicationScope', {}).get('application', {}).get('uuid')
            entity_id_scope = entity_id_scope.get('id')
            entity_id = entity_id_scope.get('uuid')
            entity_type = entity_id_scope.get('type')

            url = 'http://localhost:8080/{app_id}/{entity_type}/{entity_id}'.format(
                app_id=app_id,
                entity_id=entity_id,
                entity_type=entity_type
            )

            url = 'https://{host}/{basepath}/{app_id}/{entity_type}/{entity_id}'.format(
                host='REPLACE',
                basepath='REPLACE',
                app_id=app_id,
                entity_id=entity_id,
                entity_type=entity_type
            )

            r = requests.get(url=url,
                             headers={
                                 'Authorization': 'Bearer XCA'
                             })

            if r.status_code != 404:
                print 'ERROR/FOUND [%s]: %s' % (r.status_code, url)
            else:
                print '[%s]: %s' % (r.status_code, url)
                deleted = sqs_conn.delete_message_from_handle(sqs_queue, sqs_message.receipt_handle)
                if not deleted:
                    print 'no delete!'

    except KeyboardInterrupt, e:
        raise e


def main():
    global sqs_conn, sqs_queue
    args = parse_args()

    start_time = datetime.datetime.utcnow()
    first_start_time = start_time

    print "first start: %s" % first_start_time

    with open(args.get('config'), 'r') as f:
        config = json.load(f)

    sqs_config = config.get('sqs')

    sqs_conn = boto.sqs.connect_to_region(**sqs_config)
    queue_name = 'baas20sr_usea_baas20sr_usea_index_all_dead'
    sqs_queue = sqs_conn.get_queue(queue_name)

    last_size = sqs_queue.count()

    print 'Last Size: ' + str(last_size)

    pool = Pool(10)

    keep_going = True

    while keep_going:
        sqs_messages = sqs_queue.get_messages(
            num_messages=10,
            visibility_timeout=10,
            wait_time_seconds=10)

        if len(sqs_messages) > 0:
            pool.map(check_exists, sqs_messages)
        else:
            print 'DONE!'
            pool.terminate()
            keep_going = False


if __name__ == '__main__':
    main()
