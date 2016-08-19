# -*- coding: utf-8 -*-
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
import logging
from multiprocessing import Pool
import datetime

import argparse
import requests
import time
from logging.handlers import RotatingFileHandler

import sys

__author__ = 'Jeff.West@yahoo.com'


entity_template = {
    "id": "replaced",
    "dataType": "entitlements",
    "mockData": [
        {"importDate": "2015-08-25T23:33:57.124Z", "rowsImported": 2},
        {"role": "line-owner", "route": "/master", "element": "element1", "entitlementId": "entitlement4",
         "property": "show"},
        {"role": "line-owner", "route": "/master", "element": "element2", "entitlementId": "entitlement8",
         "property": "hide"}
    ],
    "nullArray1": [None],
    "nullArray2": [None, None],
    "nullArray3": [None, None],
    "nest1": {
        "nest2": {
            "nest3": [None, None, 'foo']
        }
    }
}

entity_template = {
    "type": "customerstatuses",
    "name": "1234",
    "created": 1454769737888,
    "modified": 1454781811473,
    "address": {
        "zip": "35873",
        "city": "málaga",
        "street": "3430 calle de bravo murillo",
        "state": "melilla"
    },
    "DOB": "787264244",
    "email": "begoña.caballero29@example.com",
    "firstName": "Begoña",
    "lastName": "Caballero",
    "lastSeenDateTime": 1447737158857,
    "locationStatus": "Entrance",
    "loyaltyAccountNumber": "1234",
    "loyaltyLevel": "basic",
    "phone": "966-450-469",
    "profilePictureUrl": "http://api.randomuser.me/portraits/thumb/women/61.jpg",
    "status": "Entrance",
    "storeId": 12121
}

collection_url_template = '{api_url}/{org}/{app}/{collection}'
query_url_template = '{api_url}/{org}/{app}/{collection}?ql=select * where tag=\'{tag}\''
entity_url_template = '{api_url}/{org}/{app}/{collection}/{entity_id}'
token_url_template = '{api_url}/{org}/{app}/token'

config = {}

session = requests.Session()

logger = logging.getLogger('UsergridEntityIndexTest')


def init_logging(stdout_enabled=True):
    root_logger = logging.getLogger()
    log_file_name = './usergrid_index_test.log'
    log_formatter = logging.Formatter(fmt='%(asctime)s | %(name)s | %(processName)s | %(levelname)s | %(message)s',
                                      datefmt='%m/%d/%Y %I:%M:%S %p')

    rotating_file = logging.handlers.RotatingFileHandler(filename=log_file_name,
                                                         mode='a',
                                                         maxBytes=2048576000,
                                                         backupCount=10)
    rotating_file.setFormatter(log_formatter)
    rotating_file.setLevel(logging.INFO)

    root_logger.addHandler(rotating_file)
    root_logger.setLevel(logging.INFO)

    logging.getLogger('urllib3.connectionpool').setLevel(logging.WARN)
    logging.getLogger('requests.packages.urllib3.connectionpool').setLevel(logging.WARN)

    if stdout_enabled:
        stdout_logger = logging.StreamHandler(sys.stdout)
        stdout_logger.setFormatter(log_formatter)
        stdout_logger.setLevel(logging.INFO)
        root_logger.addHandler(stdout_logger)


def test_multiple(number_of_entities, processes):
    global config

    start = datetime.datetime.now()

    logger.info('Creating %s entities w/ url=%s' % (number_of_entities, config['url']))
    created_map = {}

    work_items = []

    for x in xrange(1, number_of_entities + 1):
        entity = entity_template.copy()
        entity['id'] = str(x)
        work_items.append((config['url'], entity))

    responses = processes.map(create_entity, work_items)

    for res in responses:
        if len(res) > 0:
            created_map[res[0]] = res[1]

    stop = datetime.datetime.now()

    logger.info('Created [%s] entities in %s' % (number_of_entities, (stop - start)))

    return created_map


def clear(clear_url):
    logger.info('deleting.... ' + clear_url)

    r = session.delete(clear_url)

    if r.status_code != 200:
        logger.info('error deleting url=' + clear_url)
        logger.info(json.dumps(r.json()))

    else:
        res = r.json()
        len_entities = len(res.get('entities', []))

        if len_entities > 0:
            clear(clear_url)


def test_cleared(q_url):
    r = session.get(q_url)

    if r.status_code != 200:
        logger.info(json.dumps(r.json()))
    else:
        res = r.json()

        if len(res.get('entities', [])) != 0:
            logger.info('DID NOT CLEAR')


def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Indexing Latency Test')

    parser.add_argument('-o', '--org',
                        help='Name of the org to perform the test in',
                        type=str,
                        required=True)

    parser.add_argument('-a', '--app',
                        help='Name of the app to perform the test in',
                        type=str,
                        required=True)

    parser.add_argument('--base_url',
                        help='The URL of the Usergrid Instance',
                        type=str,
                        required=True)

    parser.add_argument('--client_id',
                        help='The Client ID to get a token, if needed',
                        type=str,
                        required=False)

    parser.add_argument('--client_secret',
                        help='The Client Secret to get a token, if needed',
                        type=str,
                        required=False)

    my_args = parser.parse_args(sys.argv[1:])

    return vars(my_args)


def init():
    global config

    url_data = {
        'api_url': config.get('base_url'),
        'org': config.get('org'),
        'app': config.get('app'),
        'collection': datetime.datetime.now().strftime('index-test-%yx%mx%dx%Hx%Mx%S')
    }

    config['url_data'] = url_data
    config['token_url'] = token_url_template.format(**url_data)


def create_entity(name, tag):
    create_me = entity_template.copy()
    start_tag = datetime.datetime.now().strftime('tag-%yx%mx%dx%Hx%Mx%S')
    create_me['tag'] = start_tag

    data = config.get('url_data')
    url = collection_url_template.format(**data)

    r = session.post(url, data=json.dumps(create_me))

    if r.status_code != 200:
        logger.critical('unable to create entity: %s' % r.text)
        return None
    else:
        return r.json().get('entities')[0]


def update_entity(entity_id, tag):
    data = {'tag': tag}
    url = entity_url_template.format(entity_id=entity_id, **config.get('url_data'))
    r = session.put(url, data=json.dumps(data))

    if r.status_code != 200:
        logger.critical('unable to update entity!')
        return False
    else:
        return True


def wait_for_index(entity_id, tag, wait_time=.25):
    start = datetime.datetime.now()

    url = query_url_template.format(tag=tag, **config.get('url_data'))

    logger.info('GET %s' % url)

    entities = []
    elapsed = 0

    while len(entities) <= 0:
        r = session.get(url)

        if r.status_code != 200:
            logger.critical('Unable to query, url=[%s]: %s' % (url, r.text))
            return False
        else:
            res = r.json()
            entities = res.get('entities')
            last_time = datetime.datetime.now()
            elapsed = last_time - start
            logger.info(
                    'Tag [%s] not applied to [%s] after [%s].  Waiting [%s]...' % (tag, entity_id, elapsed, wait_time))
            time.sleep(wait_time)

    logger.info('++Tag applied after [%s]!' % elapsed)


def test_entity_update():
    start_tag = datetime.datetime.now().strftime('tag-%yx%mx%dx%Hx%Mx%S')
    name = datetime.datetime.now().strftime('name-%yx%mx%dx%Hx%Mx%S')
    entity = create_entity(name, start_tag)

    if entity is None:
        logger.critical('Entity not created, cannot continue')
        return

    uuid = entity.get('uuid')

    for x in xrange(0, 10):
        tag = datetime.datetime.now().strftime('tag-%yx%mx%dx%Hx%Mx%S')
        logger.info('Testing tag [%s] on entity [%s]' % (tag, name))
        updated = update_entity(name, tag)
        if updated: wait_for_index(name, tag)

    for x in xrange(0, 10):
        tag = datetime.datetime.now().strftime('tag-%yx%mx%dx%Hx%Mx%S')
        logger.info('Testing tag [%s] on entity [%s]' % (tag, uuid))
        updated = update_entity(uuid, tag)
        if updated: wait_for_index(uuid, tag)


def main():
    global config

    processes = Pool(32)

    config = parse_args()

    init()

    init_logging()

    if config.get('client_id') is not None and config.get('client_secret') is not None:
        token_request = {
            'grant_type': 'client_credentials',
            'client_id': config.get('client_id'),
            'client_secret': config.get('client_secret')
        }

        r = session.post(config.get('token_url'), json.dumps(token_request))

        if r.status_code == 200:
            access_token = r.json().get('access_token')
            session.headers.update({'Authorization': 'Bearer %s' % access_token})
        else:
            logger.critical('unable to get token: %s' % r.text)
            exit(1)

    try:
        test_entity_update()

    except KeyboardInterrupt:
        pass
        processes.terminate()


main()
