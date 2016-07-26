import os
import uuid
from Queue import Empty
import argparse
import json
import logging
import sys
from multiprocessing import Queue, Process
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

__author__ = 'Jeff West @ ApigeeCorporation'

ECID = str(uuid.uuid1())
key_version = 'v4'

logger = logging.getLogger('GraphMigrator')
worker_logger = logging.getLogger('Worker')
collection_worker_logger = logging.getLogger('CollectionWorker')
error_logger = logging.getLogger('ErrorLogger')
audit_logger = logging.getLogger('AuditLogger')
status_logger = logging.getLogger('StatusLogger')

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

    # root_logger.setLevel(logging.WARN)

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

    log_file_name = '%s/migrator.log' % config.get('log_dir')

    # ConcurrentRotatingFileHandler
    rotating_file = ConcurrentRotatingFileHandler(filename=log_file_name,
                                                  mode='a',
                                                  maxBytes=404857600,
                                                  backupCount=0)
    rotating_file.setFormatter(log_formatter)
    rotating_file.setLevel(logging.INFO)

    root_logger.addHandler(rotating_file)

    error_log_file_name = '%s/migrator_errors.log' % config.get('log_dir')
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


class StatusListener(Process):
    def __init__(self, status_queue, worker_queue):
        super(StatusListener, self).__init__()
        self.status_queue = status_queue
        self.worker_queue = worker_queue

    def run(self):
        keep_going = True

        org_results = {
            'name': config.get('org'),
            'apps': {},
        }

        empty_count = 0

        while keep_going:

            try:
                app, collection, status_map = self.status_queue.get(timeout=60)
                status_logger.info('Received status update for app/collection: [%s / %s]' % (app, collection))
                empty_count = 0
                org_results['summary'] = {
                    'max_created': -1,
                    'max_modified': -1,
                    'min_created': 1584946416000,
                    'min_modified': 1584946416000,
                    'count': 0,
                    'bytes': 0
                }

                if app not in org_results['apps']:
                    org_results['apps'][app] = {
                        'collections': {}
                    }

                org_results['apps'][app]['collections'].update(status_map)

                try:
                    for app, app_data in org_results['apps'].iteritems():
                        app_data['summary'] = {
                            'max_created': -1,
                            'max_modified': -1,
                            'min_created': 1584946416000,
                            'min_modified': 1584946416000,
                            'count': 0,
                            'bytes': 0
                        }

                        if 'collections' in app_data:
                            for collection, collection_data in app_data['collections'].iteritems():

                                app_data['summary']['count'] += collection_data['count']
                                app_data['summary']['bytes'] += collection_data['bytes']

                                org_results['summary']['count'] += collection_data['count']
                                org_results['summary']['bytes'] += collection_data['bytes']

                                # APP
                                if collection_data.get('max_modified') > app_data['summary']['max_modified']:
                                    app_data['summary']['max_modified'] = collection_data.get('max_modified')

                                if collection_data.get('min_modified') < app_data['summary']['min_modified']:
                                    app_data['summary']['min_modified'] = collection_data.get('min_modified')

                                if collection_data.get('max_created') > app_data['summary']['max_created']:
                                    app_data['summary']['max_created'] = collection_data.get('max_created')

                                if collection_data.get('min_created') < app_data['summary']['min_created']:
                                    app_data['summary']['min_created'] = collection_data.get('min_created')

                                # ORG
                                if collection_data.get('max_modified') > org_results['summary']['max_modified']:
                                    org_results['summary']['max_modified'] = collection_data.get('max_modified')

                                if collection_data.get('min_modified') < org_results['summary']['min_modified']:
                                    org_results['summary']['min_modified'] = collection_data.get('min_modified')

                                if collection_data.get('max_created') > org_results['summary']['max_created']:
                                    org_results['summary']['max_created'] = collection_data.get('max_created')

                                if collection_data.get('min_created') < org_results['summary']['min_created']:
                                    org_results['summary']['min_created'] = collection_data.get('min_created')

                        if QSIZE_OK:
                            status_logger.warn('CURRENT Queue Depth: %s' % self.worker_queue.qsize())

                        status_logger.warn('UPDATED status of org processed: %s' % json.dumps(org_results))

                except KeyboardInterrupt, e:
                    raise e

                except:
                    print traceback.format_exc()

            except KeyboardInterrupt, e:
                status_logger.warn('FINAL status of org processed: %s' % json.dumps(org_results))
                raise e

            except Empty:
                if QSIZE_OK:
                    status_logger.warn('CURRENT Queue Depth: %s' % self.worker_queue.qsize())

                status_logger.warn('CURRENT status of org processed: %s' % json.dumps(org_results))

                status_logger.warning('EMPTY! Count=%s' % empty_count)

                empty_count += 1

                if empty_count >= 120:
                    keep_going = False

            except:
                print traceback.format_exc()

        logger.warn('FINAL status of org processed: %s' % json.dumps(org_results))


class EntityExportWorker(Process):
    def __init__(self, work_queue, response_queue):
        super(EntityExportWorker, self).__init__()
        collection_worker_logger.debug('Creating worker!')
        self.work_queue = work_queue
        self.response_queue = response_queue

    def run(self):

        collection_worker_logger.info('starting run()...')
        keep_going = True

        empty_count = 0
        app = 'NOT SET'
        collection_name = 'NOT SET'
        status_map = {}
        entity_file = None

        try:
            while keep_going:

                try:
                    app, collection_name = self.work_queue.get(timeout=30)
                    empty_count = 0

                    status_map = self.process_collection(app, collection_name)

                    status_map[collection_name]['iteration_finished'] = str(datetime.datetime.now())

                    collection_worker_logger.warning(
                            'Collection [%s / %s / %s] loop complete!  Max Created entity %s' % (
                                config.get('org'), app, collection_name, status_map[collection_name]['max_created']))

                    collection_worker_logger.warning(
                            'Sending FINAL stats for app/collection [%s / %s]: %s' % (app, collection_name, status_map))

                    self.response_queue.put((app, collection_name, status_map))

                    collection_worker_logger.info('Done! Finished app/collection: %s / %s' % (app, collection_name))

                except KeyboardInterrupt, e:
                    raise e

                except Empty:
                    collection_worker_logger.warning('EMPTY! Count=%s' % empty_count)

                    empty_count += 1

                    if empty_count >= 2:
                        keep_going = False

                except Exception, e:
                    logger.exception('Error in CollectionWorker processing collection [%s]' % collection_name)
                    print traceback.format_exc()

        finally:
            if entity_file is not None:
                entity_file.close()

            self.response_queue.put((app, collection_name, status_map))
            collection_worker_logger.info('FINISHED!')

    def process_collection(self, app, collection_name):

        status_map = {
            collection_name: {
                'iteration_started': str(datetime.datetime.now()),
                'max_created': -1,
                'max_modified': -1,
                'min_created': 1584946416000,
                'min_modified': 1584946416000,
                'count': 0,
                'bytes': 0
            }
        }

        # added a flag for using graph vs query/index
        if config.get('graph', False):
            source_collection_url = collection_graph_url_template.format(org=config.get('org'),
                                                                         app=app,
                                                                         collection=collection_name,
                                                                         limit=config.get('limit'),
                                                                         **config.get('source_endpoint'))
        else:
            source_collection_url = collection_query_url_template.format(org=config.get('org'),
                                                                         app=app,
                                                                         collection=collection_name,
                                                                         limit=config.get('limit'),
                                                                         ql="select * %s" % config.get(
                                                                                 'ql'),
                                                                         **config.get('source_endpoint'))
        counter = 0

        # use the UsergridQuery from the Python SDK to iterate the collection
        q = UsergridQueryIterator(source_collection_url,
                                  page_delay=config.get('page_sleep_time'),
                                  sleep_time=config.get('error_retry_sleep'))

        directory = os.path.join(config['export_path'], ECID, config['org'], app)

        if not os.path.exists(directory):
            os.makedirs(directory)

        entity_filename = '_'.join([collection_name, 'entity-data'])
        entity_filename_base = os.path.join(directory, entity_filename)
        entity_file_number = 0
        entity_file_counter = 0
        entity_filename = '%s-%s.txt' % (entity_filename_base, entity_file_number)
        entity_file = open(entity_filename, 'w')

        edge_filename = '_'.join([collection_name, 'edge-data'])
        edge_filename_base = os.path.join(directory, edge_filename)
        edge_file_number = 0
        edge_file_counter = 0
        edge_filename = '%s-%s.txt' % (edge_filename_base, edge_file_number)
        edge_file = open(edge_filename, 'w')

        try:

            for entity in q:
                try:
                    entity_file_counter += 1
                    counter += 1

                    if entity_file_counter > config['entities_per_file']:
                        entity_file.close()
                        entity_file_number += 1
                        entity_file_counter = 0
                        entity_filename = '%s-%s.txt' % (entity_filename_base, entity_file_number)
                        entity_file = open(entity_filename, 'w')

                    entity_file.write('%s\n' % json.dumps(entity))

                    edge_names = get_edge_names(entity)

                    for edge_name in edge_names:
                        if not include_edge(collection_name, edge_name):
                            continue

                        connection_query_url = connection_query_url_template.format(
                                org=config.get('org'),
                                app=app,
                                verb=edge_name,
                                collection=collection_name,
                                uuid=entity.get('uuid'),
                                limit=config.get('limit'),
                                **config.get('source_endpoint'))

                        connection_query = UsergridQueryIterator(connection_query_url,
                                                                 sleep_time=config.get('error_retry_sleep'))

                        target_uuids = []

                        try:
                            for target_entity in connection_query:
                                target_uuids.append(target_entity.get('uuid'))
                        except:
                            logger.exception('Error processing edge [%s] of entity [ %s / %s / %s]' % (
                                edge_name, app, collection_name, entity.get('uuid')))

                        if len(target_uuids) > 0:
                            edge_file_counter += 1

                            edges = {
                                'entity': {
                                    'type': entity.get('type'),
                                    'uuid': entity.get('uuid')
                                },
                                'edge_name': edge_name,
                                'target_uuids': target_uuids
                            }

                            if entity_file_counter > config['entities_per_file']:
                                edge_file.close()
                                edge_file_number += 1
                                edge_file_counter = 0
                                edge_filename = '%s-%s.txt' % (edge_filename_base, edge_file_number)
                                edge_file = open(edge_filename, 'w')

                            edge_file.write('%s\n' % json.dumps(edges))

                    if 'created' in entity:

                        try:
                            entity_created = long(entity.get('created'))

                            if entity_created > status_map[collection_name]['max_created']:
                                status_map[collection_name]['max_created'] = entity_created
                                status_map[collection_name]['max_created_str'] = str(
                                        datetime.datetime.fromtimestamp(entity_created / 1000))

                            if entity_created < status_map[collection_name]['min_created']:
                                status_map[collection_name]['min_created'] = entity_created
                                status_map[collection_name]['min_created_str'] = str(
                                        datetime.datetime.fromtimestamp(entity_created / 1000))

                        except ValueError:
                            pass

                    if 'modified' in entity:

                        try:
                            entity_modified = long(entity.get('modified'))

                            if entity_modified > status_map[collection_name]['max_modified']:
                                status_map[collection_name]['max_modified'] = entity_modified
                                status_map[collection_name]['max_modified_str'] = str(
                                        datetime.datetime.fromtimestamp(entity_modified / 1000))

                            if entity_modified < status_map[collection_name]['min_modified']:
                                status_map[collection_name]['min_modified'] = entity_modified
                                status_map[collection_name]['min_modified_str'] = str(
                                        datetime.datetime.fromtimestamp(entity_modified / 1000))

                        except ValueError:
                            pass

                    status_map[collection_name]['bytes'] += count_bytes(entity)
                    status_map[collection_name]['count'] += 1

                    if counter % 1000 == 1:
                        try:
                            collection_worker_logger.warning(
                                    'Sending incremental stats for app/collection [%s / %s]: %s' % (
                                        app, collection_name, status_map))

                            self.response_queue.put((app, collection_name, status_map))

                            if QSIZE_OK:
                                collection_worker_logger.info(
                                        'Counter=%s, collection queue depth=%s' % (
                                            counter, self.work_queue.qsize()))
                        except:
                            pass

                        collection_worker_logger.warn(
                                'Current status of collections processed: %s' % json.dumps(status_map))
                except KeyboardInterrupt:
                    raise

                except:
                    logger.exception(
                            'Error processing entity %s / %s / %s' % (app, collection_name, entity.get('uuid')))

        except KeyboardInterrupt:
            raise

        except:
            logger.exception('Error processing collection %s / %s ' % (app, collection_name))

        finally:
            if edge_file is not None:
                edge_file.close()

            if entity_file is not None:
                entity_file.close()

        return status_map


def use_name_for_collection(collection_name):
    return collection_name in config.get('use_name_for_collection', [])


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


def get_edge_names(entity):
    out_edge_names = [edge_name for edge_name in entity.get('metadata', {}).get('collections', [])]
    out_edge_names += [edge_name for edge_name in entity.get('metadata', {}).get('connections', [])]

    return out_edge_names


def get_uuid_time(the_uuid_string):
    return time_uuid.TimeUUID(the_uuid_string).get_datetime()


def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Org/App Migrator')

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

    parser.add_argument('-o', '--org',
                        help='Name of the org to migrate',
                        type=str,
                        required=True)

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

    parser.add_argument('-s', '--source_config',
                        help='The path to the source endpoint/org configuration file',
                        type=str,
                        default='source.json')

    parser.add_argument('--export_path',
                        help='The path to save the export files',
                        type=str,
                        default='.')

    parser.add_argument('--limit',
                        help='The number of entities to return per query request',
                        type=int,
                        default=100)

    parser.add_argument('--entities_per_file',
                        help='The number of entities to put in one JSON file',
                        type=int,
                        default=10000)

    parser.add_argument('--error_retry_sleep',
                        help='The number of seconds to wait between retrieving after an error',
                        type=float,
                        default=30)

    parser.add_argument('--page_sleep_time',
                        help='The number of seconds to wait between retrieving pages from the UsergridQueryIterator',
                        type=float,
                        default=.5)

    parser.add_argument('--entity_sleep_time',
                        help='The number of seconds to wait between retrieving pages from the UsergridQueryIterator',
                        type=float,
                        default=.1)

    parser.add_argument('--workers',
                        dest='collection_workers',
                        help='The number of worker processes to do the migration',
                        type=int,
                        default=4)

    parser.add_argument('--queue_size_max',
                        help='The max size of entities to allow in the queue',
                        type=int,
                        default=100000)

    parser.add_argument('--ql',
                        help='The QL to use in the filter for reading data from collections',
                        type=str,
                        default='select * order by created asc')
    # default='select * order by created asc')

    parser.add_argument('--nohup',
                        help='specifies not to use stdout for logging',
                        action='store_true')

    parser.add_argument('--graph',
                        help='Use GRAPH instead of Query',
                        dest='graph',
                        action='store_true')

    my_args = parser.parse_args(sys.argv[1:])

    return vars(my_args)


def init():
    global config

    config['collection_mapping'] = {}
    config['app_mapping'] = {}
    config['org_mapping'] = {}

    with open(config.get('source_config'), 'r') as f:
        config['source_config'] = json.load(f)

    if config['exclude_collection'] is None:
        config['exclude_collection'] = []

    config['source_endpoint'] = config['source_config'].get('endpoint').copy()
    config['source_endpoint'].update(config['source_config']['credentials'][config['org']])


def wait_for(threads, label, sleep_time=60):
    wait = True

    logger.info('Starting to wait for [%s] threads with sleep time=[%s]' % (len(threads), sleep_time))

    while wait:
        wait = False
        alive_count = 0

        for t in threads:

            if t.is_alive():
                alive_count += 1
                logger.info('Thread [%s] is still alive' % t.name)

        if alive_count > 0:
            wait = True
            logger.info('Continuing to wait for [%s] threads with sleep time=[%s]' % (alive_count, sleep_time))
            time.sleep(sleep_time)

    logger.warn('All workers [%s] done!' % label)


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


def main():
    global config

    config = parse_args()
    init()
    init_logging()

    status_map = {}

    org_apps = {
    }

    if len(org_apps) == 0:
        source_org_mgmt_url = org_management_url_template.format(org=config.get('org'),
                                                                 limit=config.get('limit'),
                                                                 **config.get('source_endpoint'))

        print 'Retrieving apps from [%s]' % source_org_mgmt_url
        logger.info('Retrieving apps from [%s]' % source_org_mgmt_url)

        try:
            # list the apps for the SOURCE org
            logger.info('GET %s' % source_org_mgmt_url)
            r = session_source.get(source_org_mgmt_url)

            if r.status_code != 200:
                logger.critical(
                        'Abort processing: Unable to retrieve apps from [%s]: %s' % (source_org_mgmt_url, r.text))
                exit()

            logger.info(json.dumps(r.text))

            org_apps = r.json().get('data')

        except Exception, e:
            logger.exception('ERROR Retrieving apps from [%s]' % source_org_mgmt_url)
            print traceback.format_exc()
            logger.critical('Unable to retrieve apps from [%s] and will exit' % source_org_mgmt_url)
            exit()

    if _platform == "linux" or _platform == "linux2":
        collection_queue = Queue(maxsize=config.get('queue_size_max'))
        collection_response_queue = Queue(maxsize=config.get('queue_size_max'))
    else:
        collection_queue = Queue()
        collection_response_queue = Queue()

    logger.info('Starting entity_workers...')

    status_listener = StatusListener(collection_response_queue, collection_queue)
    status_listener.start()

    # start the worker processes which will iterate the collections
    collection_workers = [EntityExportWorker(collection_queue, collection_response_queue) for x in
                          xrange(config.get('collection_workers'))]
    [w.start() for w in collection_workers]

    try:
        apps_to_process = config.get('app')
        collections_to_process = config.get('collection')

        # iterate the apps retrieved from the org
        for org_app in sorted(org_apps.keys()):
            logger.info('Found SOURCE App: %s' % org_app)

        time.sleep(3)

        for org_app in sorted(org_apps.keys()):
            parts = org_app.split('/')
            app = parts[1]

            # if apps are specified and the current app is not in the list, skip it
            if apps_to_process and len(apps_to_process) > 0 and app not in apps_to_process:
                logger.warning('Skipping app [%s] not included in process list [%s]' % (app, apps_to_process))
                continue

            logger.info('Processing app=[%s]' % app)

            status_map[app] = {
                'iteration_started': str(datetime.datetime.now()),
                'max_created': -1,
                'max_modified': -1,
                'min_created': 1584946416000,
                'min_modified': 1584946416000,
                'count': 0,
                'bytes': 0,
                'collections': {}
            }

            # get the list of collections from the source org/app
            source_app_url = app_url_template.format(org=config.get('org'),
                                                     app=app,
                                                     **config.get('source_endpoint'))
            logger.info('GET %s' % source_app_url)

            r_collections = session_source.get(source_app_url)

            collection_attempts = 0

            # sometimes this call was not working so I put it in a loop to force it...
            while r_collections.status_code != 200 and collection_attempts < 5:
                collection_attempts += 1
                logger.warning('FAILED: GET (%s) [%s] URL: %s' % (r_collections.elapsed, r_collections.status_code,
                                                                  source_app_url))
                time.sleep(DEFAULT_RETRY_SLEEP)
                r_collections = session_source.get(source_app_url)

            if collection_attempts >= 5:
                logger.critical('Unable to get collections at URL %s, skipping app' % source_app_url)
                continue

            app_response = r_collections.json()

            logger.info('App Response: ' + json.dumps(app_response))

            app_entities = app_response.get('entities', [])

            if len(app_entities) > 0:
                app_entity = app_entities[0]
                collections = app_entity.get('metadata', {}).get('collections', {})
                logger.info('Collection List: %s' % collections)

                # iterate the collections which are returned.
                for collection_name, collection_data in collections.iteritems():
                    exclude_collections = config.get('exclude_collection', [])

                    if exclude_collections is None:
                        exclude_collections = []

                    # filter out collections as configured...
                    if collection_name in ignore_collections \
                            or (len(collections_to_process) > 0 and collection_name not in collections_to_process) \
                            or (len(exclude_collections) > 0 and collection_name in exclude_collections) \
                            or (config.get('migrate') == 'credentials' and collection_name != 'users'):
                        logger.warning('Skipping collection=[%s]' % collection_name)

                        continue

                    logger.info('Publishing app / collection: %s / %s' % (app, collection_name))

                    collection_queue.put((app, collection_name))

            status_map[app]['iteration_finished'] = str(datetime.datetime.now())

            logger.info('Finished publishing collections for app [%s] !' % app)

        # allow collection workers to finish
        wait_for(collection_workers, label='collection_workers', sleep_time=30)

        status_listener.terminate()

    except KeyboardInterrupt:
        logger.warning('Keyboard Interrupt, aborting...')
        collection_queue.close()
        collection_response_queue.close()

        [os.kill(super(EntityExportWorker, p).pid, signal.SIGINT) for p in collection_workers]
        os.kill(super(StatusListener, status_listener).pid, signal.SIGINT)

        [w.terminate() for w in collection_workers]
        status_listener.terminate()

    logger.info('entity_workers DONE!')


if __name__ == "__main__":
    main()
