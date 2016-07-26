# -*- coding: utf-8 -*-
import json
import logging
import traceback
from multiprocessing import Pool
import datetime
import socket

import argparse
import requests
import time
from logging.handlers import RotatingFileHandler

import sys

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

url_template = '{api_url}/{org}/{app}/{collection}'
token_url_template = '{api_url}/{org}/{app}/token'

config = {}

session = requests.Session()

logger = logging.getLogger('UsergridBatchIndexTest')


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


def create_entity(work_item):
    global config
    try:
        url = work_item[0]
        entity = work_item[1]

        # entity['name'] = datetime.datetime.now().strftime('name-%yx%mx%dx%Hx%Mx%S')

        logger.info('creating entity [%s] at URL [%s]' % (entity.get('id'), url))

        r = session.post(url, data=json.dumps(entity))

        if r.status_code != 200:
            logger.error('HTTP %s: %s' % (r.status_code, r.text))
            print 'HTTP %s: %s' % (r.status_code, r.text)
            return

        entities = r.json().get('entities', [])
        uuid = entities[0].get('uuid')

        if r.status_code != 200:
            logger.info('%s: %s' % (r.status_code, uuid))
        else:
            logger.info('Created entity UUID=[%s] at URL [%s]' % (uuid, url))

        return uuid, entity

    except Exception, e:
        print traceback.format_exc(e)


def test_multiple(number_of_entities):
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


def wait_for_indexing(created_map, q_url, sleep_time=0.0):
    logger.info('Waiting for indexing of [%s] entities...' % len(created_map))

    count_missing = 100
    start_time = datetime.datetime.now()

    while count_missing > 0:

        entity_map = {}
        r = session.get(q_url)
        res = r.json()
        entities = res.get('entities', [])

        now_time = datetime.datetime.now()
        elapsed = now_time - start_time

        logger.info('Found [%s] of [%s] ([%s] missing) after [%s] entities at url: %s' % (
            len(entities), len(created_map), (len(created_map) - len(entities)), elapsed, q_url))

        count_missing = 0

        for entity in entities:
            entity_map[entity.get('uuid')] = entity

        for uuid, created_entity in created_map.iteritems():
            if uuid not in entity_map:
                count_missing += 1
                logger.info('Missing uuid=[%s] Id=[%s] total missing=[%s]' % (
                    uuid, created_entity.get('id'), count_missing))

        if count_missing > 0:
            logger.info('Waiting for indexing, count_missing=[%s] Total time [%s] Sleeping for [%s]s' % (
                elapsed, count_missing, sleep_time))

            time.sleep(sleep_time)

    stop_time = datetime.datetime.now()
    logger.info('All entities found after %s' % (stop_time - start_time))


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


processes = Pool(32)


def test_url(q_url, sleep_time=0.25):
    test_var = False

    while not test_var:
        r = session.get(q_url)

        if r.status_code == 200:

            if len(r.json().get('entities')) >= 1:
                test_var = True
        else:
            logger.info('non 200')

        if test_var:
            logger.info('Test of URL [%s] Passes')
        else:
            logger.info('Test of URL [%s] Passes')
            time.sleep(sleep_time)


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
        'collection': '%s-%s' % (socket.gethostname(), datetime.datetime.now().strftime('index-test-%yx%mx%dx%Hx%Mx%S'))
    }

    config['url'] = url_template.format(**url_data)
    config['token_url'] = token_url_template.format(**url_data)


def main():
    global config

    # processes = Pool(32)

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
        created_map = test_multiple(999)

        q_url = config.get('url') + "?ql=select * where dataType='entitlements'&limit=1000"

        wait_for_indexing(created_map=created_map,
                          q_url=q_url,
                          sleep_time=1)

        delete_q_url = config.get('url') + "?ql=select * where dataType='entitlements'&limit=1000"

        clear(clear_url=delete_q_url)

    except KeyboardInterrupt:
        processes.terminate()

    processes.terminate()


main()
