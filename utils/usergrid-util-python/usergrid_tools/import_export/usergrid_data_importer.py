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

from __future__ import print_function
import os
import uuid
from Queue import Empty
import argparse
import json
import logging
import sys
from multiprocessing import Queue, Process, Pool
import time_uuid

import datetime
from cloghandler import ConcurrentRotatingFileHandler
import requests
import traceback
import time
from sys import platform as _platform

import signal

from usergrid import UsergridQueryIterator
import urllib3

__author__ = 'Jeff.West@yahoo.com'

ECID = str(uuid.uuid1())

logger = logging.getLogger('UsergridImporter')
worker_logger = logging.getLogger('Worker')
collection_worker_logger = logging.getLogger('ExportCollectionWorker')
error_logger = logging.getLogger('ImporterErrorLogger')
status_logger = logging.getLogger('ImporterStatusLogger')

urllib3.disable_warnings()

DEFAULT_CREATE_APPS = False
DEFAULT_RETRY_SLEEP = 10
DEFAULT_PROCESSING_SLEEP = 1

queue = Queue()
QSIZE_OK = False

try:
    queue.qsize()
    QSIZE_OK = True
except:
    pass

session_source = requests.Session()
session_target = requests.Session()


def total_seconds(td):
    return (td.microseconds + (td.seconds + td.days * 24 * 3600) * 10 ** 6) / 10 ** 6


def init_logging(stdout_enabled=True):
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.getLevelName(config.get('log_level', 'INFO')))

    logging.getLogger('requests.packages.urllib3.connectionpool').setLevel(logging.ERROR)
    logging.getLogger('boto').setLevel(logging.ERROR)
    logging.getLogger('urllib3.connectionpool').setLevel(logging.WARN)

    log_formatter = logging.Formatter(
        fmt='%(asctime)s | ' + ECID + ' | %(name)s | %(processName)s | %(levelname)s | %(message)s',
        datefmt='%m/%d/%Y %I:%M:%S %p')

    stdout_logger = logging.StreamHandler(sys.stdout)
    stdout_logger.setFormatter(log_formatter)
    root_logger.addHandler(stdout_logger)

    if stdout_enabled:
        stdout_logger.setLevel(logging.getLevelName(config.get('log_level', 'INFO')))

    # base log file

    log_file_name = '%s/usergrid-exporter-%s.log' % (config.get('log_dir'), ECID)

    # ConcurrentRotatingFileHandler
    rotating_file = ConcurrentRotatingFileHandler(filename=log_file_name,
                                                  mode='a',
                                                  maxBytes=404857600,
                                                  backupCount=0)
    rotating_file.setFormatter(log_formatter)
    rotating_file.setLevel(logging.INFO)

    root_logger.addHandler(rotating_file)

    error_log_file_name = '%s/usergrid-exporter-%s-errors.log' % (config.get('log_dir'), ECID)

    error_rotating_file = ConcurrentRotatingFileHandler(filename=error_log_file_name,
                                                        mode='a',
                                                        maxBytes=404857600,
                                                        backupCount=0)
    error_rotating_file.setFormatter(log_formatter)
    error_rotating_file.setLevel(logging.ERROR)

    root_logger.addHandler(error_rotating_file)


entity_name_map = {
    'users': 'username'
}

config = {}

# URL Templates for Usergrid
org_management_app_url_template = "{api_url}/management/organizations/{org}/applications?client_id={client_id}&client_secret={client_secret}"
org_management_url_template = "{api_url}/management/organizations/{org}/applications?client_id={client_id}&client_secret={client_secret}"
org_url_template = "{api_url}/{org}?client_id={client_id}&client_secret={client_secret}"
app_url_template = "{api_url}/{org}/{app}?client_id={client_id}&client_secret={client_secret}"
collection_url_template = "{api_url}/{org}/{app}/{collection}?client_id={client_id}&client_secret={client_secret}"
collection_query_url_template = "{api_url}/{org}/{app}/{collection}?ql={ql}&client_id={client_id}&client_secret={client_secret}&limit={limit}"
collection_graph_url_template = "{api_url}/{org}/{app}/{collection}?client_id={client_id}&client_secret={client_secret}&limit={limit}"
connection_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}?client_id={client_id}&client_secret={client_secret}"
connecting_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/connecting/{verb}?client_id={client_id}&client_secret={client_secret}"
connection_create_by_uuid_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_uuid}?client_id={client_id}&client_secret={client_secret}"
connection_create_by_name_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_type}/{target_name}?client_id={client_id}&client_secret={client_secret}"
get_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}&connections=none"
get_entity_url_with_connections_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"
put_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"

user_credentials_url_template = "{api_url}/{org}/{app}/users/{uuid}/credentials"

ignore_collections = ['activities', 'queues', 'events', 'notifications']


def use_name_for_collection(collection_name):
    return collection_name in (config.get('use_name_for_collection', []) + ['role', 'user'])


def include_edge(collection_name, edge_name):
    include_edges = config.get('include_edge', [])

    if include_edges is None:
        include_edges = []

    exclude_edges = config.get('exclude_edge', [])

    if exclude_edges is None:
        exclude_edges = []

    if len(include_edges) > 0 and edge_name not in include_edges:
        logger.debug(
            'Skipping edge [%s] since it is not in INCLUDED list: %s' % (edge_name, include_edges))
        return False

    if edge_name in exclude_edges:
        logger.debug(
            'Skipping edge [%s] since it is in EXCLUDED list: %s' % (edge_name, exclude_edges))
        return False

    if (collection_name in ['users', 'user'] and edge_name in ['roles', 'followers', 'groups',
                                                               'feed', 'activities']) \
            or (collection_name in ['device', 'devices'] and edge_name in ['users']) \
            or (collection_name in ['receipts', 'receipt'] and edge_name in ['device', 'devices']):
        # feed and activities are not retrievable...
        # roles and groups will be more efficiently handled from the role/group -> user
        # followers will be handled by 'following'
        # do only this from user -> device
        return False

    return True


def get_source_identifier(source_entity):
    entity_type = source_entity.get('type')

    source_identifier = source_entity.get('uuid')

    if use_name_for_collection(entity_type):

        if entity_type in ['user']:
            source_identifier = source_entity.get('username')
        else:
            source_identifier = source_entity.get('name')

        if source_identifier is None:
            source_identifier = source_entity.get('uuid')
            logger.warn('Using UUID for entity [%s / %s]' % (entity_type, source_identifier))

    return source_identifier


def include_collection(collection_name):
    exclude = config.get('exclude_collection', [])

    if exclude is not None and collection_name in exclude:
        return False

    return True


def get_uuid_time(the_uuid_string):
    return time_uuid.TimeUUID(the_uuid_string).get_datetime()


def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Data Exporter')

    parser.add_argument('--map_app',
                        help="Multiple allowed: A colon-separated string such as 'apples:oranges' which indicates to"
                             " put data from the app named 'apples' from the source endpoint into app named 'oranges' "
                             "in the target endpoint",
                        default=[],
                        action='append')

    parser.add_argument('--map_collection',
                        help="One or more colon-separated string such as 'cats:dogs' which indicates to put data from "
                             "collections named 'cats' from the source endpoint into a collection named 'dogs' in the "
                             "target endpoint, applicable globally to all apps",
                        default=[],
                        action='append')

    parser.add_argument('--map_org',
                        help="One or more colon-separated strings such as 'red:blue' which indicates to put data from "
                             "org named 'red' from the source endpoint into a collection named 'blue' in the target "
                             "endpoint",
                        default=[],
                        action='append')

    parser.add_argument('--log_dir',
                        help='path to the place where logs will be written',
                        default='./',
                        type=str,
                        required=False)

    parser.add_argument('--log_level',
                        help='log level - DEBUG, INFO, WARN, ERROR, CRITICAL',
                        default='INFO',
                        type=str,
                        required=False)

    parser.add_argument('-a', '--app',
                        help='Name of one or more apps to include, specify none to include all apps',
                        required=False,
                        action='append')

    parser.add_argument('-e', '--include_edge',
                        help='Name of one or more edges/connection types to INCLUDE, specify none to include all edges',
                        required=False,
                        action='append')

    parser.add_argument('--exclude_edge',
                        help='Name of one or more edges/connection types to EXCLUDE, specify none to include all edges',
                        required=False,
                        action='append')

    parser.add_argument('--exclude_collection',
                        help='Name of one or more collections to EXCLUDE, specify none to include all collections',
                        required=False,
                        action='append')

    parser.add_argument('-c', '--collection',
                        help='Name of one or more collections to include, specify none to include all collections',
                        default=[],
                        action='append')

    parser.add_argument('-t', '--target',
                        help='The path to the source endpoint/org configuration file',
                        type=str,
                        default='target.json')

    parser.add_argument('--import_path',
                        help='The path to read the exported data from',
                        default=[],
                        action='append',
                        required=True)

    parser.add_argument('--workers',
                        help='The number of worker processes to do the migration',
                        type=int,
                        default=4)

    parser.add_argument('--queue_size_max',
                        help='The max size of entities to allow in the queue',
                        type=int,
                        default=100000)

    parser.add_argument('--nohup',
                        help='specifies not to use stdout for logging',
                        action='store_true')

    my_args = parser.parse_args(sys.argv[1:])

    return vars(my_args)


def init():
    global config

    config['collection_mapping'] = {}
    config['app_mapping'] = {}
    config['org_mapping'] = {}

    if not os.path.isfile(config.get('target')):
        logger.critical('Target Endpoint Config [%s] is not a file' % config.get('target'))
        exit(1)

    with open(config.get('target'), 'r') as f:
        config['target'] = json.load(f)

    if config['exclude_collection'] is None:
        config['exclude_collection'] = []

    if config['exclude_edge'] is None:
        config['exclude_edge'] = []

    for mapping in config.get('map_collection', []):
        parts = mapping.split(':')

        if len(parts) == 2:
            config['collection_mapping'][parts[0]] = parts[1]
        else:
            logger.warning('Skipping Collection mapping: [%s]' % mapping)

    for mapping in config.get('map_app', []):
        parts = mapping.split(':')

        if len(parts) == 2:
            config['app_mapping'][parts[0]] = parts[1]
        else:
            logger.warning('Skipping App mapping: [%s]' % mapping)

    for mapping in config.get('map_org', []):
        parts = mapping.split(':')

        if len(parts) == 2:
            config['org_mapping'][parts[0]] = parts[1]
            logger.info('Mapping Org [%s] to [%s] from mapping [%s]' % (parts[0], parts[1], mapping))
        else:
            logger.warning('Skipping Org mapping: [%s]' % mapping)

    config['target_endpoint'] = config['target'].get('endpoint').copy()


def count_bytes(entity):
    entity_copy = entity.copy()

    if 'metadata' in entity_copy:
        del entity_copy['metadata']

    entity_str = json.dumps(entity_copy)

    return len(entity_str)


def check_response_status(r, url, exit_on_error=True):
    if r.status_code != 200:
        logger.critical('HTTP [%s] on URL=[%s]' % (r.status_code, url))
        logger.critical('Response: %s' % r.text)

        if exit_on_error:
            exit()


def get_target_mapping(org, app, collection_name):
    target_org = config.get('org_mapping', {}).get(org, org)
    target_app = config.get('app_mapping', {}).get(app, app)
    target_collection = config.get('collection_mapping', {}).get(collection_name, collection_name)
    return target_org, target_app, target_collection


def load_entity(org, app, collection, entity, attempts=0):
    if attempts >= 4:
        logger.error('Retry attempts exceeded threshold!')
        return False

    entity_uuid = get_source_identifier(entity)

    target = config['target_endpoint'].copy()
    target.update(config['target']['credentials'][org])

    url = put_entity_url_template.format(
        org=org,
        app=app,
        collection=collection,
        uuid=entity_uuid,
        **target
    )

    r = requests.put(url, data=json.dumps(entity))

    if r.status_code == 200:
        return True

    elif 400 <= r.status_code < 500:
        logger.error('4XX Error [%s] loading entity to URL [%s]: %s' % (r.status_code, url, r.text))
        return False
    else:
        logger.warning('Will attempt retry [%s] after 5XX Error [%s] loading entity to URL: %s' % (
            attempts, r.status_code, r.text))
        time.sleep(5)
        return load_entity(org, app, collection, entity, attempts + 1)


def load_entities_from_file(work_item):
    global config

    success_count = 0
    failed_count = 0
    response = {'success_count': success_count, 'failed_count': failed_count}

    file_path = work_item['file']

    if not os.path.isfile(file_path):
        logger.error('Cannot process path [%s] as it is not a file...' % file_path)
        return response

    if not include_collection(work_item['collection']):
        logger.error('Collection [%s] will be ignored' % (work_item['collection']))
        return response

    org, app, collection = get_target_mapping(work_item['org'], work_item['app'], work_item['collection'])

    if org not in config['target']['credentials']:
        logger.error(
            'Cannot process org [%s -> %s] because it does not have credentials specified in target config file [%s]' % (
                work_item['org'], org, config.get('target')))
        return response

    target = config['target_endpoint'].copy()
    target.update(config['target']['credentials'][org])

    with open(file_path) as f:

        for line in f:
            line = line.rstrip('\n')

            try:
                entity = json.loads(line)

                success = load_entity(org, app, collection, entity)

                if success:
                    success_count += 1
                else:
                    failed_count += 1

            except:
                print(traceback.format_exc())

    logger.warning('Processed %s entities (%s successful and %s failed) from file: %s' % (
        (success_count + failed_count), success_count, failed_count, file_path))

    return {'success_count': success_count, 'failed_count': failed_count}


def create_connection(org, app, collection, entity_id, edge_name, target_uuid, attempts=0):
    if attempts >= 4:
        logger.error('Retry attempts exceeded threshold!')
        return False

    entity_uuid = get_source_identifier(entity_id)

    target = config['target_endpoint'].copy()
    target.update(config['target']['credentials'][org])

    url = connection_create_by_uuid_url_template.format(
        org=org,
        app=app,
        collection=collection,
        uuid=entity_uuid,
        verb=edge_name,
        target_uuid=target_uuid,
        **target
    )

    r = requests.post(url)

    if r.status_code == 200:
        return True

    elif 400 <= r.status_code < 500:
        logger.error('4XX Error [%s] creating connection at URL [%s]: %s' % (r.status_code, url, r.text))
        return False

    else:
        logger.warning('Will attempt retry [%s] after 5XX Error [%s] creating connection at URL: %s' % (
            attempts, r.status_code, r.text))
        time.sleep(5)

        return create_connection(org, app, collection, entity_id, edge_name, target_uuid, attempts + 1)


def create_connections_from_file(work_item):
    global config

    total_success_count = 0
    total_failed_count = 0
    response = {'success_count': total_success_count, 'failed_count': total_failed_count}

    file_path = work_item['file']

    if not os.path.isfile(file_path):
        logger.error('Cannot process path [%s] as it is not a file...' % file_path)
        return response

    if not include_collection(work_item['collection']):
        logger.error('Collection [%s] will be ignored' % (work_item['collection']))
        return response

    org, app, collection = get_target_mapping(work_item['org'], work_item['app'], work_item['collection'])

    if org not in config['target']['credentials']:
        logger.error(
            'Cannot process org [%s -> %s] because it does not have credentials specified in target config file [%s]' % (
                work_item['org'], org, config.get('target')))
        return response

    with open(file_path) as f:

        for line in f:
            line = line.rstrip('\n')

            try:
                example = {
                    "target_uuids": [
                        "254bf4aa-76d3-11e5-98ce-7d9b10bf4310"
                    ],
                    "edge_name": "owns",
                    "entity": {
                        "type": "owner",
                        "uuid": "2c4bcb9a-76d3-11e5-8f26-5fe6eed758aa"
                    }
                }

                connection_set = json.loads(line)
                entity_id = connection_set['entity']
                edge_name = connection_set['edge_name']
                targets = connection_set['target_uuids']

                if not include_edge(entity_id['type'], edge_name):
                    logger.warn('Excluding Edge [%s] from  entity %s' % (edge_name, entity_id))
                    continue

                entity_edge_success = 0
                entity_edge_failure = 0

                for target in reversed(targets):
                    success = create_connection(org, app, collection, entity_id, edge_name, target)

                    if success:
                        total_success_count += 1
                        entity_edge_success += 1
                    else:
                        total_failed_count += 1
                        entity_edge_failure += 1

                if entity_edge_failure > 0:
                    logger.error('Unable to create all connections for entity: %s' % entity_id)

            except:
                print(traceback.format_exc())

    logger.warning('Processed %s connections (%s successful and %s failed) from file: %s' % (
        (total_success_count + total_failed_count), total_success_count, total_failed_count, file_path))

    return {'success_count': total_success_count, 'failed_count': total_failed_count}


def main():
    global config

    config = parse_args()
    init_logging()
    init()

    status_map = {}

    import_path_array = config.get('import_path')

    connection_work = []
    entity_work = []

    for import_path in import_path_array:

        if not os.path.isdir(import_path):
            logger.critical('Skipping import path specified: [%s] is not a directory!' % import_path)
            continue

        orgs = [f for f in os.listdir(import_path) if os.path.isdir(os.path.join(import_path, f))]

        for org in orgs:
            org_dir = os.path.join(import_path, org)

            logger.info('Found Org [%s] at path [%s]' % (org, org_dir))

            apps = [f for f in os.listdir(org_dir) if os.path.isdir(os.path.join(org_dir, f))]

            for app in apps:
                app_dir = os.path.join(org_dir, app)

                logger.info('Found App [%s/%s] at path [%s]' % (org, app, app_dir))

                collections = [f for f in os.listdir(app_dir) if os.path.isdir(os.path.join(app_dir, f))]

                for collection in collections:
                    collection_dir = os.path.join(app_dir, collection)

                    logger.info('Found Collection [%s/%s/%s] at path [%s]' % (org, app, collection, collection_dir))

                    entity_files = [f for f in os.listdir(collection_dir) if
                                    os.path.isfile(os.path.join(collection_dir, f)) and 'entit' in f and os.stat(
                                        os.path.join(collection_dir, f)).st_size > 0]

                    if len(entity_files) > 0:
                        logger.info(
                            'Found entity files for Collection [%s/%s/%s]: %s' % (org, app, collection, entity_files))

                        for entity_file in entity_files:
                            entity_work.append(
                                {
                                    'org': org,
                                    'app': app,
                                    'collection': collection,
                                    'operation': load_entities_from_file,
                                    'file': os.path.join(collection_dir, entity_file)
                                }
                            )

                    connection_files = [f for f in os.listdir(collection_dir) if
                                        os.path.isfile(os.path.join(collection_dir, f)) and 'connec' in f and os.stat(
                                            os.path.join(collection_dir, f)).st_size > 0]

                    if len(connection_files) > 0:

                        for connection_file in connection_files:
                            connection_work.append(
                                {
                                    'org': org,
                                    'app': app,
                                    'collection': collection,
                                    'operation': create_connections_from_file,
                                    'file': os.path.join(collection_dir, connection_file)
                                }
                            )

    logger.info('Creating pool with %s workers' % config.get('workers'))
    pool = Pool(config.get('workers'))

    logger.info('Loading entities, file count is [%s]' % len(entity_work))
    pool.map(load_entities_from_file, entity_work)

    logger.info('Creating Connections, file count is [%s]' % len(connection_work))
    pool.map(create_connections_from_file, connection_work)

    logger.info('Done!')


if __name__ == "__main__":
    main()
