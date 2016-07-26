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

# URL Templates for Usergrid
import json
import random

import requests
from multiprocessing import Process, Pool

import time

collection_url_template = "{api_url}/{org}/{app}/{collection}"
entity_url_template = "{api_url}/{org}/{app}/{collection}/{entity_id}"
connection_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}"
connection_create_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_uuid}"

url_data = {
    'api_url': 'https://usergridhost/basepath',
    'org': 'samples',
    'app': 'event-example'
}


session = requests.Session()


class EventGenerator(Process):
    def __init__(self, store_id, event_count, user_array, beacons):
        super(EventGenerator, self).__init__()

        self.store_id = store_id
        self.user_array = user_array
        self.event_count = event_count
        self.beacons = beacons
        self.session = requests.Session()
        self.create_store(self.store_id)
        self.create_users(self.user_array)

    def create_store(self, store_id):
        url = entity_url_template.format(collection='stores', entity_id=store_id, **url_data)

        r = self.session.put(url, data=json.dumps({"name": store_id}))

        if r.status_code != 200:
            print 'Error creating store [%s] at URL=[%s]: %s' % (store_id, url, r.text)

    def create_event(self, user, event):
        print 'creating event: %s' % json.dumps(event)

        url = collection_url_template.format(collection='general-events', **url_data)

        r = self.session.post(url, data=json.dumps(event))

        if r.status_code == 200:
            res = r.json()
            entity = res.get('entities')[0]
            event_uuid = entity.get('uuid')

            # link to user
            create_connection_url = connection_create_url_template.format(collection='users',
                                                                          uuid=user,
                                                                          verb='events',
                                                                          target_uuid=event_uuid,
                                                                          **url_data)

            r_connect = self.session.post(create_connection_url)

            if r_connect.status_code == 200:
                print 'created connection: %s' % create_connection_url

            # link to store
            create_connection_url = connection_create_url_template.format(collection='stores',
                                                                          uuid=event.get('storeId'),
                                                                          verb='events',
                                                                          target_uuid=event_uuid,
                                                                          **url_data)

            r_connect = self.session.post(create_connection_url)

            if r_connect.status_code == 200:
                print 'created connection: %s' % create_connection_url

            if event.get('eventType') == 'beacon':
                # link to beacon
                create_connection_url = connection_create_url_template.format(collection='beacons',
                                                                              uuid=event.get('beaconId'),
                                                                              verb='events',
                                                                              target_uuid=event_uuid,
                                                                              **url_data)

                r_connect = self.session.post(create_connection_url)

                if r_connect.status_code == 200:
                    print 'created connection: %s' % create_connection_url
                else:
                    print 'Error creating connection at URL=[%s]: %s' % (create_connection_url, r.text)

    def run(self):

        for user in self.user_array:

            # store 123
            self.create_event(user, {
                'storeId': self.store_id,
                'eventType': 'enterStore'
            })

            for x in xrange(0, self.event_count):
                beacon_number = random.randint(0, len(self.beacons) - 1)
                beacon_name = self.beacons[beacon_number]

                event = {
                    'beaconId': '%s-%s' % (self.store_id, beacon_name),
                    'storeId': self.store_id,
                    'eventType': 'beacon'
                }

                self.create_event(user, event)

            self.create_event(user, {
                'storeId': self.store_id,
                'eventType': 'exitStore'
            })

    def create_users(self, user_array):
        for user in user_array:
            self.create_user(user)

    def create_user(self, user):
        data = {
            'username': user,
            'email': '%s@example.com' % user
        }

        url = collection_url_template.format(collection='users', **url_data)

        r = self.session.post(url, json.dumps(data))

        if r.status_code != 200:
            print 'Error creating user [%s] at URL=[%s]: %s' % (user, url, r.text)


def create_entity(entity_type, entity_name):
    url = entity_url_template.format(collection=entity_type, entity_id=entity_name, **url_data)
    r = session.put(url, data=json.dumps({'name': entity_name}))

    if r.status_code != 200:
        print 'Error creating %s [%s] at URL=[%s]: %s' % (entity_type, entity_name, url, r.text)


def create_beacon(beacon_name):
    create_entity('beacons', beacon_name)


def create_store(store_name):
    create_entity('stores', store_name)


def main():
    beacons = ["b1", "b2", "b3", "b4", "b5", "b6"]

    stores = ['store_123', 'store_456', 'store_789', 'store_901']

    beacon_names = []

    for store in stores:
        for beacon in beacons:
            beacon_names.append('%s-%s' % (store, beacon))

    pool = Pool(16)

    pool.map(create_beacon, beacon_names)
    pool.map(create_store, stores)

    processes = [
        EventGenerator(stores[0], 100, ['jeff', 'julie'], beacons=beacons),
        EventGenerator(stores[0], 100, ['russo', 'dunker'], beacons=beacons),
        EventGenerator(stores[2], 100, ['jeff', 'julie'], beacons=beacons),
        EventGenerator(stores[2], 100, ['russo', 'dunker'], beacons=beacons),
        EventGenerator(stores[3], 100, ['jeff', 'julie'], beacons=beacons),
        EventGenerator(stores[3], 100, ['russo', 'dunker'], beacons=beacons),
        EventGenerator(stores[1], 100, ['bala', 'shankar'], beacons=beacons),
        EventGenerator(stores[1], 100, ['chet', 'anant'], beacons=beacons)
    ]

    [p.start() for p in processes]

    while len([p for p in processes if p.is_alive()]) > 0:
        print 'Processors active, waiting'
        time.sleep(1)


main()
