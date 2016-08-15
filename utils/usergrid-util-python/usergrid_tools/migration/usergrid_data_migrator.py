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
from multiprocessing import Queue, Process
from sets import Set

import boto
from boto import ses, sqs
import time_uuid
import datetime

from boto.sqs.message import RawMessage
from cloghandler import ConcurrentRotatingFileHandler
import requests
import traceback
import redis
import time
from sys import platform as _platform
import signal
from requests.auth import HTTPBasicAuth
from usergrid import UsergridQueryIterator
import urllib3

LARGE_TIMESTAMP_MILLIS = 1584946416000

__author__ = 'Jeff.West@yahoo.com'

ecid = str(uuid.uuid1())
key_version = 'v4'

logger = logging.getLogger('GraphMigrator')
worker_logger = logging.getLogger('Worker')
collection_worker_logger = logging.getLogger('CollectionWorker')
error_logger = logging.getLogger('ErrorLogger')
audit_logger = logging.getLogger('AuditLogger')
status_aggregator_logger = logging.getLogger('StatusLogger')

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

cache = None
config_defaults = {
    'log_dir': '/tmp',
    'skip_cache_read': True,
    'skip_cache_write': True,
    'collection_workers': 2,
    'graph_depth': 1,
    'page_sleep_time': 2,
    'error_retry_sleep': 1,
    'entity_workers': 3,
    'limit': 10
}


def total_seconds(td):
    return (td.microseconds + (td.seconds + td.days * 24 * 3600) * 10 ** 6) / 10 ** 6


def init_logging(log_dir, log_level, org_name, migrate, stdout_enabled=True):
    root_logger = logging.getLogger()
    [h.close() for h in root_logger.handlers]

    root_logger.handlers = []
    root_logger.setLevel(logging.getLevelName(log_level))

    logging.getLogger('requests.packages.urllib3.connectionpool').setLevel(logging.ERROR)
    logging.getLogger('boto').setLevel(logging.ERROR)
    logging.getLogger('urllib3.connectionpool').setLevel(logging.WARN)

    log_formatter = logging.Formatter(
        fmt='%(asctime)s | ' + ecid + ' | %(name)s | %(processName)s | %(levelname)s | %(message)s',
        datefmt='%m/%d/%Y %I:%M:%S %p')

    stdout_logger = logging.StreamHandler(sys.stdout)
    stdout_logger.setFormatter(log_formatter)
    root_logger.addHandler(stdout_logger)

    if stdout_enabled:
        stdout_logger.setLevel(logging.getLevelName(log_level))

    log_file_name = os.path.join(log_dir, '%s-%s-%s-migrator.log' % (org_name, migrate, ecid))

    # ConcurrentRotatingFileHandler
    rotating_file = ConcurrentRotatingFileHandler(filename=log_file_name,
                                                  mode='a',
                                                  maxBytes=404857600,
                                                  backupCount=0)
    rotating_file.setFormatter(log_formatter)
    rotating_file.setLevel(logging.INFO)

    root_logger.addHandler(rotating_file)

    error_log_file_name = os.path.join(log_dir, '%s-%s-%s-migrator-errors.log' % (org_name, migrate, ecid))

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

connection_create_by_pairs_url_template = "{api_url}/{org}/{app}/{source_type_id}/{verb}/{target_type_id}?client_id={client_id}&client_secret={client_secret}"

get_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}&connections=none"
get_entity_url_with_connections_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"
put_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"
permissions_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/permissions?client_id={client_id}&client_secret={client_secret}"

user_credentials_url_template = "{api_url}/{org}/{app}/users/{uuid}/credentials"

ignore_collections = ['activities', 'queues', 'events', 'notifications']


class StatusAggregator(Process):
    def __init__(self, status_queue, worker_queue, final_status_queue):
        super(StatusAggregator, self).__init__()
        self.status_queue = status_queue
        self.worker_queue = worker_queue
        self.final_status_queue = final_status_queue

    def summarize(self, org_results):

        created_max = 267753609
        modified_max = 267753609
        created_min = LARGE_TIMESTAMP_MILLIS
        min_modified = LARGE_TIMESTAMP_MILLIS
        org_iteration_started = LARGE_TIMESTAMP_MILLIS
        org_iteration_finished = 267753609
        count = 0
        bytes = 0

        org_results['summary'] = {
            'iteration_started': org_iteration_started,
            'iteration_finished': org_iteration_finished,
            'created_max': created_max,
            'modified_max': modified_max,
            'created_min': created_min,
            'modified_min': min_modified,
            'count': 0,
            'bytes': 0
        }

        for app, app_data in org_results['apps'].iteritems():
            created_max = 267753609
            modified_max = 267753609
            created_min = LARGE_TIMESTAMP_MILLIS
            min_modified = LARGE_TIMESTAMP_MILLIS

            app_iteration_started = LARGE_TIMESTAMP_MILLIS
            app_iteration_finished = 267753609

            count = 0
            bytes = 0

            for collection, collection_data in app_data.get('collections', {}).iteritems():

                if 'failed_uuids' in collection_data:
                    continue

                count += collection_data['count']
                bytes += collection_data['bytes']

                if collection_data['count'] == 0:
                    if 'modified_max' in collection_data: del collection_data['modified_max']
                    if 'modified_min' in collection_data: del collection_data['modified_min']

                    if 'created_min' in collection_data: del collection_data['created_min']
                    if 'created_max' in collection_data: del collection_data['created_max']

                # APP
                if 'iteration_finished' in collection_data:
                    if app_iteration_finished < collection_data['iteration_finished']:
                        app_iteration_finished = collection_data['iteration_finished']

                if 'iteration_started' in collection_data:
                    if app_iteration_started > collection_data['iteration_started']:
                        app_iteration_started = collection_data['iteration_started']

                if 'modified_max' in collection_data:
                    if modified_max < collection_data['modified_max']:
                        modified_max = collection_data['modified_max']

                if 'modified_min' in collection_data:
                    if min_modified > collection_data['modified_min']:
                        min_modified = collection_data['modified_min']

                if 'created_max' in collection_data:
                    if created_max < collection_data['created_max']:
                        created_max = collection_data['created_max']

                if 'created_min' in collection_data:
                    if created_min > collection_data['created_min']:
                        created_min = collection_data['created_min']

            app_data['summary'] = {
                'iteration_started': app_iteration_started,
                'iteration_finished': app_iteration_finished,
                'created_max': created_max,
                'modified_max': modified_max,
                'created_min': created_min,
                'modified_min': min_modified,
                'count': count,
                'bytes': bytes
            }

            # org
            if app_iteration_started < org_results['summary']['iteration_started']:
                org_results['summary']['iteration_started'] = app_iteration_started

            if app_iteration_finished > org_results['summary']['iteration_finished']:
                org_results['summary']['iteration_finished'] = app_iteration_finished

            if modified_max > org_results['summary']['modified_max']:
                org_results['summary']['modified_max'] = modified_max

            if min_modified < org_results['summary']['modified_min'] :
                org_results['summary']['modified_min'] = min_modified

            if created_max > org_results['summary']['created_max']:
                org_results['summary']['created_max'] = created_max

            if created_min < org_results['summary']['created_min']:
                org_results['summary']['created_min'] = created_min

            org_results['summary'].update({
                'count': org_results['summary']['count'] + count,
                'bytes': org_results['summary']['bytes'] + bytes
            })

        status_aggregator_logger.warn('UPDATED status of org processed: %s' % json.dumps(org_results))

        self.final_status_queue.put(org_results)

        status_file_name = os.path.join(config.get('log_dir'),
                                        '%s-%s-%s-status.json' % (config.get('org'), config.get('migrate'), ecid))

        logger.info('Writing status to file: %s' % status_file_name)

        try:
            with open(status_file_name, 'w') as f:
                json.dump(org_results, f, indent=2)
        except:
            print(traceback.format_exc())

    def run(self):
        keep_going = True

        org_results = {
            'name': config.get('org'),
            'apps': {},
        }

        empty_count = 0
        counter = 0

        while keep_going:

            try:
                app, collection, status_map = self.status_queue.get(timeout=60)
                counter += 1

                if app is None and collection is None and status_map is None:
                    status_aggregator_logger.warning('Poison Pill Received - stopping process')
                    self.summarize(org_results)
                    return

                if app is None:
                    continue

                status_aggregator_logger.info(
                    'Received status update for app/collection: [%s / %s]' % (app, collection))

                empty_count = 0

                if app not in org_results['apps']:
                    org_results['apps'][app] = {
                        'collections': {}
                    }

                for collection, collection_data in status_map.iteritems():
                    if collection not in org_results['apps'][app]['collections']:
                        org_results['apps'][app]['collections'][collection] = {}

                    org_results['apps'][app]['collections'][collection].update(collection_data)

                if counter % 1000 == 1:
                    self.summarize(org_results)

            except KeyboardInterrupt as e:
                status_aggregator_logger.warn('FINAL status of org processed: %s' % json.dumps(org_results))
                self.summarize(org_results)
                raise e

            except Empty:
                if QSIZE_OK:
                    status_aggregator_logger.warn('CURRENT Queue Depth: %s' % self.worker_queue.qsize())

                self.summarize(org_results)
                status_aggregator_logger.warn('CURRENT status of org processed: %s' % json.dumps(org_results))

                status_aggregator_logger.warning('EMPTY! Count=%s' % empty_count)

                empty_count += 1

                if empty_count >= 120:
                    keep_going = False

            except:
                print(traceback.format_exc())

        logger.warn('FINAL status of org processed: %s' % json.dumps(org_results))

        self.summarize(org_results)


class EntityWorker(Process):
    def __init__(self, entity_queue, handler_function, status_queue):
        super(EntityWorker, self).__init__()

        worker_logger.debug('Creating worker!')
        self.queue = entity_queue
        self.handler_function = handler_function
        self.status_queue = status_queue

    def run(self):

        worker_logger.info('starting run()...')
        keep_going = True

        count_processed = 0
        empty_count = 0
        start_time = int(time.time())

        while keep_going:

            try:
                # get an entity with the app and collection name
                app, collection_name, entity = self.queue.get(timeout=120)

                if app is None and collection_name is None and entity is None:
                    logger.warning('Poison Pill - stopping process')
                    return

                empty_count = 0

                # if entity.get('type') == 'user':
                #     entity = confirm_user_entity(app, entity)

                # the handler operation is the specified operation such as migrate_graph
                if self.handler_function is not None:
                    try:
                        message_start_time = int(time.time())
                        processed = self.handler_function(app, collection_name, entity)
                        message_end_time = int(time.time())

                        if processed:
                            count_processed += 1

                            total_time = message_end_time - start_time
                            avg_time_per_message = total_time / count_processed
                            message_time = message_end_time - message_start_time

                            worker_logger.debug('Processed [%sth] entity = %s / %s / %s' % (
                                count_processed, app, collection_name, entity.get('uuid')))

                            if count_processed % 1000 == 1:
                                worker_logger.info(
                                    'Processed [%sth] entity = [%s / %s / %s] in [%s]s - avg time/message [%s]' % (
                                        count_processed, app, collection_name, entity.get('uuid'), message_time,
                                        avg_time_per_message))

                    except KeyboardInterrupt as e:
                        raise e

                    except Exception as e:
                        logger.exception('Error in EntityWorker processing message')
                        print(traceback.format_exc())

            except KeyboardInterrupt as e:
                raise e

            except Empty:
                worker_logger.warning('EMPTY! Count=%s' % empty_count)

                empty_count += 1

                if empty_count >= 2:
                    keep_going = False

            except Exception as e:
                logger.exception('Error in EntityWorker run()')
                print(traceback.format_exc())


class CollectionWorker(Process):
    def __init__(self, work_queue, entity_queue, response_queue):
        super(CollectionWorker, self).__init__()
        collection_worker_logger.debug('Creating worker!')
        self.work_queue = work_queue
        self.response_queue = response_queue
        self.entity_queue = entity_queue

    def run(self):

        collection_worker_logger.info('starting run()...')

        counter = 0
        # created_max = 0
        empty_count = 0
        app = 'ERROR'
        collection_name = 'NOT SET'
        status_map = {}
        sleep_time = 10

        try:

            while True:

                try:
                    app, collection_name = self.work_queue.get(timeout=30)

                    if app is None and collection_name is None:
                        logger.warn('Poison Pill - stopping process')
                        self.response_queue.put((app, collection_name, status_map))
                        keep_going = False
                        break

                    status_map = {
                        collection_name: {
                            'iteration_started': int(round(time.time() * 1000)),
                            'iteration_finished': int(round(time.time() * 1000)),
                            'created_max': 267753609,
                            'modified_max': 267753609,
                            'created_min': 1584946416000,
                            'modified_min': 1584946416000,
                            'count': 0,
                            'bytes': 0
                        }
                    }

                    empty_count = 0

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

                    logger.info('Iterating URL: %s' % source_collection_url)

                    # use the UsergridQuery from the Python SDK to iterate the collection
                    q = UsergridQueryIterator(source_collection_url,
                                              page_delay=config.get('page_sleep_time'),
                                              sleep_time=config.get('error_retry_sleep'))

                    for entity in q:

                        # begin entity loop

                        self.entity_queue.put((app, collection_name, entity))

                        counter += 1

                        if 'created' in entity:

                            try:
                                entity_created = long(entity.get('created'))

                                if entity_created > status_map[collection_name]['created_max']:
                                    status_map[collection_name]['created_max'] = entity_created

                                if entity_created < status_map[collection_name]['created_min']:
                                    status_map[collection_name]['created_min'] = entity_created

                            except ValueError:
                                pass

                        if 'modified' in entity:

                            try:
                                entity_modified = long(entity.get('modified'))

                                if entity_modified > status_map[collection_name]['modified_max']:
                                    status_map[collection_name]['modified_max'] = entity_modified

                                if entity_modified < status_map[collection_name]['modified_min']:
                                    status_map[collection_name]['modified_min'] = entity_modified

                            except ValueError:
                                pass

                        status_map[collection_name]['iteration_finished'] = int(round(time.time() * 1000))
                        status_map[collection_name]['bytes'] += count_bytes(entity)
                        status_map[collection_name]['count'] += 1

                        if counter % 1000 == 1:
                            try:
                                collection_worker_logger.warning(
                                    'Sending stats for app/collection [%s / %s]: %s' % (
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

                        if config.get('entity_sleep_time') > 0:
                            collection_worker_logger.debug(
                                'sleeping for [%s]s per entity...' % (config.get('entity_sleep_time')))
                            time.sleep(config.get('entity_sleep_time'))
                            collection_worker_logger.debug(
                                'STOPPED sleeping for [%s]s per entity...' % (config.get('entity_sleep_time')))

                    # end entity loop
                    collection_worker_logger.warning(
                        'Collection [%s / %s / %s] loop complete!  Max Created entity %s' % (
                            config.get('org'), app, collection_name, status_map[collection_name]['created_max']))

                    collection_worker_logger.warning(
                        'Sending FINAL stats for app/collection [%s / %s]: %s' % (app, collection_name, status_map))

                    self.response_queue.put((app, collection_name, status_map))

                    collection_worker_logger.info('Done! Finished app/collection: %s / %s' % (app, collection_name))

                except KeyboardInterrupt as e:
                    raise e

                except Empty:
                    collection_worker_logger.warning('EMPTY! Count=%s' % empty_count)

                except Exception as e:
                    logger.exception('Error in CollectionWorker processing collection [%s]' % collection_name)
                    print(traceback.format_exc())

        finally:
            self.response_queue.put((app, collection_name, status_map))
            collection_worker_logger.info('FINISHED!')


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

    if (collection_name in ['users', 'user'] and edge_name in ['followers', 'feed', 'activities']) \
            or (collection_name in ['receipts', 'receipt'] and edge_name in ['device', 'devices']):
        # feed and activities are not retrievable...
        # roles and groups will be more efficiently handled from the role/group -> user
        # followers will be handled by 'following'
        # do only this from user -> device
        return False

    return True


def exclude_edge(collection_name, edge_name):
    exclude_edges = config.get('exclude_edge', [])

    if exclude_edges is None:
        exclude_edges = []

    if edge_name in exclude_edges:
        logger.debug('Skipping edge [%s] since it is in EXCLUDED list: %s' % (edge_name, exclude_edges))
        return True

    if (collection_name in ['users', 'user'] and edge_name in ['followers', 'feed', 'activities']) \
            or (collection_name in ['receipts', 'receipt'] and edge_name in ['device', 'devices']):
        # feed and activities are not retrievable...
        # roles and groups will be more efficiently handled from the role/group -> user
        # followers will be handled by 'following'
        # do only this from user -> device
        return True

    return False


def confirm_user_entity(app, source_entity, attempts=0):
    attempts += 1

    source_entity_url = get_entity_url_template.format(org=config.get('org'),
                                                       app=app,
                                                       collection='users',
                                                       uuid=source_entity.get('username'),
                                                       **config.get('source_endpoint'))

    if attempts >= 5:
        logger.warning('Punting after [%s] attempts to confirm user at URL [%s], will use the source entity...' % (
            attempts, source_entity_url))

        return source_entity

    r = requests.get(url=source_entity_url)

    if r.status_code == 200:
        retrieved_entity = r.json().get('entities')[0]

        if retrieved_entity.get('uuid') != source_entity.get('uuid'):
            logger.info(
                'UUID of Source Entity [%s] differs from uuid [%s] of retrieved entity at URL=[%s] and will be substituted' % (
                    source_entity.get('uuid'), retrieved_entity.get('uuid'), source_entity_url))

        return retrieved_entity

    elif 'service_resource_not_found' in r.text:

        logger.warn('Unable to retrieve user at URL [%s], and will use source entity.  status=[%s] response: %s...' % (
            source_entity_url, r.status_code, r.text))

        return source_entity

    else:
        logger.error('After [%s] attempts to confirm user at URL [%s], received status [%s] message: %s...' % (
            attempts, source_entity_url, r.status_code, r.text))

        time.sleep(DEFAULT_RETRY_SLEEP)

        return confirm_user_entity(app, source_entity, attempts)


def create_connection(app, collection_name, source_entity, edge_name, target_entity):
    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    source_identifier = get_source_identifier(source_entity)
    target_identifier = get_source_identifier(target_entity)

    source_type_id = '%s/%s' % (source_entity.get('type'), source_identifier)
    target_type_id = '%s/%s' % (target_entity.get('type'), target_identifier)

    if source_entity.get('type') == 'user':
        source_type_id = '%s/%s' % ('users', source_entity.get('username'))

    if target_entity.get('type') == 'user':
        if edge_name == 'users':
            target_type_id = target_entity.get('uuid')
        else:
            target_type_id = '%s/%s' % ('users', target_entity.get('uuid'))

    if target_entity.get('type') == 'device':
        if edge_name == 'devices':
            target_type_id = target_entity.get('uuid')
        else:
            target_type_id = '%s/%s' % ('devices', target_entity.get('uuid'))

    if target_entity.get('type') == 'receipt':
        if edge_name == 'receipts':
            target_type_id = target_entity.get('uuid')
        else:
            target_type_id = '%s/%s' % ('receipts', target_entity.get('uuid'))

    create_connection_url = connection_create_by_pairs_url_template.format(
        org=target_org,
        app=target_app,
        source_type_id=source_type_id,
        verb=edge_name,
        target_type_id=target_type_id,
        **config.get('target_endpoint'))

    if not config.get('skip_cache_read', False):
        processed = cache.get(create_connection_url)

        if processed not in [None, 'None']:
            logger.debug('Skipping visited Edge: [%s / %s / %s] --[%s]--> [%s / %s / %s]: %s ' % (
                app, collection_name, source_identifier, edge_name, target_app, target_entity.get('type'),
                target_entity.get('name'), create_connection_url))

            return True

    logger.info('Connecting entity [%s / %s / %s] --[%s]--> [%s / %s / %s]: %s ' % (
        app, collection_name, source_identifier, edge_name, target_app, target_entity.get('type'),
        target_entity.get('name', target_entity.get('uuid')), create_connection_url))

    attempts = 0

    while attempts < 5:
        attempts += 1

        r_create = session_target.post(create_connection_url)

        if r_create.status_code == 200:

            if not config.get('skip_cache_write', False):
                cache.set(create_connection_url, 1)

            return True
        else:
            if r_create.status_code >= 500:

                if attempts < 5:
                    logger.warning('FAILED [%s] (will retry) to create connection at URL=[%s]: %s' % (
                        r_create.status_code, create_connection_url, r_create.text))
                    time.sleep(DEFAULT_RETRY_SLEEP)
                else:
                    logger.critical(
                        'FAILED [%s] (WILL NOT RETRY - max attempts) to create connection at URL=[%s]: %s' % (
                            r_create.status_code, create_connection_url, r_create.text))
                    return False

            elif r_create.status_code in [401, 404]:

                if config.get('repair_data', False):
                    logger.warning('FAILED [%s] (WILL attempt repair) to create connection at URL=[%s]: %s' % (
                        r_create.status_code, create_connection_url, r_create.text))
                    migrate_data(app, source_entity.get('type'), source_entity, force=True)
                    migrate_data(app, target_entity.get('type'), target_entity, force=True)

                else:
                    logger.critical('FAILED [%s] (WILL NOT attempt repair) to create connection at URL=[%s]: %s' % (
                        r_create.status_code, create_connection_url, r_create.text))

            else:
                logger.warning('FAILED [%s] (will retry) to create connection at URL=[%s]: %s' % (
                    r_create.status_code, create_connection_url, r_create.text))

    return False


def process_edges(app, collection_name, source_entity, edge_name, connection_stack):
    source_identifier = get_source_identifier(source_entity)

    while len(connection_stack) > 0:

        target_entity = connection_stack.pop()

        if exclude_collection(collection_name) or exclude_collection(target_entity.get('type')):
            logger.debug('EXCLUDING Edge (collection): [%s / %s / %s] --[%s]--> ?' % (
                app, collection_name, source_identifier, edge_name))
            continue

        create_connection(app, collection_name, source_entity, edge_name, target_entity)


def migrate_out_graph_edge_type(app, collection_name, source_entity, edge_name, depth=0):
    if not include_edge(collection_name, edge_name):
        return True

    source_uuid = source_entity.get('uuid')

    key = '%s:edge:out:%s:%s' % (key_version, source_uuid, edge_name)

    if not config.get('skip_cache_read', False):
        date_visited = cache.get(key)

        if date_visited not in [None, 'None']:
            logger.info('Skipping EDGE [%s / %s --%s-->] - visited at %s' % (
                collection_name, source_uuid, edge_name, date_visited))
            return True
        else:
            cache.delete(key)

    if not config.get('skip_cache_write', False):
        cache.set(name=key, value=str(int(time.time())), ex=config.get('visit_cache_ttl', 3600 * 2))

    logger.debug('Visiting EDGE [%s / %s (%s) --%s-->] at %s' % (
        collection_name, source_uuid, get_uuid_time(source_uuid), edge_name, str(datetime.datetime.utcnow())))

    response = True

    source_identifier = get_source_identifier(source_entity)

    count_edges = 0

    logger.debug(
        'Processing edge type=[%s] of entity [%s / %s / %s]' % (edge_name, app, collection_name, source_identifier))

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    connection_query_url = connection_query_url_template.format(
        org=config.get('org'),
        app=app,
        verb=edge_name,
        collection=collection_name,
        uuid=source_identifier,
        limit=config.get('limit'),
        **config.get('source_endpoint'))

    connection_query = UsergridQueryIterator(connection_query_url, sleep_time=config.get('error_retry_sleep'))

    connection_stack = []

    for target_entity in connection_query:
        target_connection_collection = config.get('collection_mapping', {}).get(target_entity.get('type'),
                                                                                target_entity.get('type'))

        target_ok = migrate_graph(app, target_entity.get('type'), source_entity=target_entity, depth=depth)

        if not target_ok:
            logger.critical(
                'Error migrating TARGET entity data for connection [%s / %s / %s] --[%s]--> [%s / %s / %s]' % (
                    app, collection_name, source_identifier, edge_name, app, target_connection_collection,
                    target_entity.get('name', target_entity.get('uuid'))))

        count_edges += 1
        connection_stack.append(target_entity)

    process_edges(app, collection_name, source_entity, edge_name, connection_stack)

    return response


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
    if collection_name in ['events']:
        return False

    include = config.get('collection', [])

    if include is not None and len(include) > 0 and collection_name not in include:
        return False

    exclude = config.get('exclude_collection', [])

    if exclude is not None and collection_name in exclude:
        return False

    return True


def exclude_collection(collection_name):
    exclude = config.get('exclude_collection', [])

    if exclude is not None and collection_name in exclude:
        return True

    return False


def migrate_in_graph_edge_type(app, collection_name, source_entity, edge_name, depth=0):
    source_uuid = source_entity.get('uuid')
    key = '%s:edges:in:%s:%s' % (key_version, source_uuid, edge_name)

    if not config.get('skip_cache_read', False):
        date_visited = cache.get(key)

        if date_visited not in [None, 'None']:
            logger.info('Skipping EDGE [--%s--> %s / %s] - visited at %s' % (
                collection_name, source_uuid, edge_name, date_visited))
            return True
        else:
            cache.delete(key)

    if not config.get('skip_cache_write', False):
        cache.set(name=key, value=str(int(time.time())), ex=config.get('visit_cache_ttl', 3600 * 2))

    logger.debug('Visiting EDGE [--%s--> %s / %s (%s)] at %s' % (
        edge_name, collection_name, source_uuid, get_uuid_time(source_uuid), str(datetime.datetime.utcnow())))

    source_identifier = get_source_identifier(source_entity)

    if exclude_collection(collection_name):
        logger.debug('Excluding (Collection) entity [%s / %s / %s]' % (app, collection_name, source_uuid))
        return True

    if not include_edge(collection_name, edge_name):
        return True

    logger.debug(
        'Processing edge type=[%s] of entity [%s / %s / %s]' % (edge_name, app, collection_name, source_identifier))

    logger.debug('Processing IN edges type=[%s] of entity [ %s / %s / %s]' % (
        edge_name, app, collection_name, source_uuid))

    connecting_query_url = connecting_query_url_template.format(
        org=config.get('org'),
        app=app,
        collection=collection_name,
        uuid=source_uuid,
        verb=edge_name,
        limit=config.get('limit'),
        **config.get('source_endpoint'))

    connection_query = UsergridQueryIterator(connecting_query_url, sleep_time=config.get('error_retry_sleep'))

    response = True

    for e_connection in connection_query:
        logger.debug('Triggering IN->OUT edge migration on entity [%s / %s / %s] ' % (
            app, e_connection.get('type'), e_connection.get('uuid')))

        response = migrate_graph(app, e_connection.get('type'), e_connection, depth) and response

    return response


def migrate_graph(app, collection_name, source_entity, depth=0):
    depth += 1
    source_uuid = source_entity.get('uuid')

    # short circuit if the graph depth exceeds what was specified
    if depth > config.get('graph_depth', 1):
        logger.debug(
            'Reached Max Graph Depth, stopping after [%s] on [%s / %s]' % (depth, collection_name, source_uuid))
        return True
    else:
        logger.debug('Processing @ Graph Depth [%s]' % depth)

    if exclude_collection(collection_name):
        logger.warn('Ignoring entity in filtered collection [%s]' % collection_name)
        return True

    key = '%s:graph:%s' % (key_version, source_uuid)
    entity_tag = '[%s / %s / %s (%s)]' % (app, collection_name, source_uuid, get_uuid_time(source_uuid))

    if not config.get('skip_cache_read', False):
        date_visited = cache.get(key)

        if date_visited not in [None, 'None']:
            logger.debug('Skipping GRAPH %s at %s' % (entity_tag, date_visited))
            return True
        else:
            cache.delete(key)

    logger.info('Visiting GRAPH %s at %s' % (entity_tag, str(datetime.datetime.utcnow())))

    if not config.get('skip_cache_write', False):
        cache.set(name=key, value=str(int(time.time())), ex=config.get('visit_cache_ttl', 3600 * 2))

    # first, migrate data for current node
    response = migrate_data(app, collection_name, source_entity)

    # gather the outbound edge names
    out_edge_names = [edge_name for edge_name in source_entity.get('metadata', {}).get('collections', [])]
    out_edge_names += [edge_name for edge_name in source_entity.get('metadata', {}).get('connections', [])]

    logger.debug('Entity %s has [%s] OUT edges' % (entity_tag, len(out_edge_names)))

    # migrate each outbound edge type
    for edge_name in out_edge_names:

        if not exclude_edge(collection_name, edge_name):
            response = migrate_out_graph_edge_type(app, collection_name, source_entity, edge_name, depth) and response

        if config.get('prune', False):
            prune_edge_by_name(edge_name, app, collection_name, source_entity)

    # gather the inbound edge names
    in_edge_names = [edge_name for edge_name in source_entity.get('metadata', {}).get('connecting', [])]

    logger.debug('Entity %s has [%s] IN edges' % (entity_tag, len(in_edge_names)))

    # migrate each inbound edge type
    for edge_name in in_edge_names:

        if not exclude_edge(collection_name, edge_name):
            response = migrate_in_graph_edge_type(app, collection_name, source_entity, edge_name,
                                                  depth) and response

    return response


def collect_entities(q):
    response = {}

    for e in q:
        response[e.get('uuid')] = e

    return response


def prune_edge_by_name(edge_name, app, collection_name, source_entity):
    if not include_edge(collection_name, edge_name):
        return True

    source_identifier = get_source_identifier(source_entity)
    source_uuid = source_entity.get('uuid')

    entity_tag = '[%s / %s / %s (%s)]' % (app, collection_name, source_uuid, get_uuid_time(source_uuid))

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    target_connection_query_url = connection_query_url_template.format(
        org=target_org,
        app=target_app,
        verb=edge_name,
        collection=target_collection,
        uuid=source_identifier,
        limit=config.get('limit'),
        **config.get('target_endpoint'))

    source_connection_query_url = connection_query_url_template.format(
        org=config.get('org'),
        app=app,
        verb=edge_name,
        collection=collection_name,
        uuid=source_identifier,
        limit=config.get('limit'),
        **config.get('source_endpoint'))

    source_connections = collect_entities(
        UsergridQueryIterator(source_connection_query_url, sleep_time=config.get('error_retry_sleep')))

    target_connections = collect_entities(
        UsergridQueryIterator(target_connection_query_url, sleep_time=config.get('error_retry_sleep')))

    delete_uuids = Set(target_connections.keys()) - Set(source_connections.keys())

    if len(delete_uuids) > 0:
        logger.info('Found [%s] edges to delete for entity %s' % (len(delete_uuids), entity_tag))

        for delete_uuid in delete_uuids:
            delete_connection_url = connection_create_by_uuid_url_template.format(
                org=target_org,
                app=target_app,
                verb=edge_name,
                collection=target_collection,
                uuid=source_identifier,
                target_uuid=delete_uuid,
                **config.get('target_endpoint'))

            attempts = 0

            while attempts < 5:
                attempts += 1

                r = session_target.delete(delete_connection_url)

                if not config.get('skip_cache_write'):
                    cache.delete(delete_connection_url)

                if r.status_code == 200:
                    logger.info('Pruned edge on attempt [%s] URL=[%s]' % (attempts, delete_connection_url))
                    break
                else:
                    logger.error('Error [%s] on attempt [%s] deleting connection at URL=[%s]: %s' % (
                        r.status_code, attempts, delete_connection_url, r.text))
                    time.sleep(DEFAULT_RETRY_SLEEP)

    return True


def prune_graph(app, collection_name, source_entity):
    source_uuid = source_entity.get('uuid')
    key = '%s:prune_graph:%s' % (key_version, source_uuid)
    entity_tag = '[%s / %s / %s (%s)]' % (app, collection_name, source_uuid, get_uuid_time(source_uuid))

    if not config.get('skip_cache_read', False):
        date_visited = cache.get(key)

        if date_visited not in [None, 'None']:
            logger.debug('Skipping PRUNE %s at %s' % (entity_tag, date_visited))
            return True
        else:
            cache.delete(key)

    logger.debug('pruning GRAPH %s at %s' % (entity_tag, str(datetime.datetime.utcnow())))
    if not config.get('skip_cache_write', False):
        cache.set(name=key, value=str(int(time.time())), ex=config.get('visit_cache_ttl', 3600 * 2))

    if collection_name in config.get('exclude_collection', []):
        logger.debug('Excluding (Collection) entity %s' % entity_tag)
        return True

    out_edge_names = [edge_name for edge_name in source_entity.get('metadata', {}).get('collections', [])]
    out_edge_names += [edge_name for edge_name in source_entity.get('metadata', {}).get('connections', [])]

    for edge_name in out_edge_names:
        prune_edge_by_name(edge_name, app, collection_name, source_entity)


def reput(app, collection_name, source_entity, attempts=0):
    source_identifier = source_entity.get('uuid')
    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    try:
        target_entity_url_by_name = put_entity_url_template.format(org=target_org,
                                                                   app=target_app,
                                                                   collection=target_collection,
                                                                   uuid=source_identifier,
                                                                   **config.get('target_endpoint'))

        r = session_source.put(target_entity_url_by_name, data=json.dumps({}))
        if r.status_code != 200:
            logger.info('HTTP [%s]: %s' % (target_entity_url_by_name, r.status_code))
        else:
            logger.debug('HTTP [%s]: %s' % (target_entity_url_by_name, r.status_code))

    except:
        pass


def get_uuid_time(the_uuid_string):
    return time_uuid.TimeUUID(the_uuid_string).get_datetime()


def migrate_permissions(app, collection_name, source_entity, attempts=0):
    if collection_name not in ['roles', 'role', 'group', 'groups']:
        return True

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    source_identifier = get_source_identifier(source_entity)

    source_permissions_url = permissions_url_template.format(org=config.get('org'),
                                                             app=app,
                                                             collection=collection_name,
                                                             uuid=source_identifier,
                                                             **config.get('source_endpoint'))

    r = session_source.get(source_permissions_url)

    if r.status_code != 200:
        logger.error('Unable to get permissions at URL [%s]: %s' % (source_permissions_url, r.text))
        return False

    perm_response = r.json()

    perms = perm_response.get('data', [])

    logger.info('Migrating [%s / %s] with permissions %s' % (collection_name, source_identifier, perms))

    if len(perms) > 0:
        target_permissions_url = permissions_url_template.format(org=target_org,
                                                                 app=target_app,
                                                                 collection=target_collection,
                                                                 uuid=source_identifier,
                                                                 **config.get('target_endpoint'))

        for permission in perms:
            data = {'permission': permission}

            logger.info('Posting permission %s to %s' % (json.dumps(data), target_permissions_url))

            r = session_target.post(target_permissions_url, json.dumps(data))

            if r.status_code != 200:
                logger.error(
                    'ERROR posting permission %s to URL=[%s]: %s' % (
                        json.dumps(data), target_permissions_url, r.text))

    return True


def migrate_data(app, collection_name, source_entity, attempts=0, force=False):
    if config.get('skip_data') and not force:
        return True

    # check the cache to see if this entity has changed
    if not config.get('skip_cache_read', False) and not force:
        try:
            str_modified = cache.get(source_entity.get('uuid'))

            if str_modified not in [None, 'None']:

                modified = long(str_modified)

                logger.debug('FOUND CACHE: %s = %s ' % (source_entity.get('uuid'), modified))

                if modified <= source_entity.get('modified'):

                    modified_date = datetime.datetime.utcfromtimestamp(modified / 1000)
                    e_uuid = source_entity.get('uuid')

                    uuid_datetime = time_uuid.TimeUUID(e_uuid).get_datetime()

                    logger.debug('Skipping ENTITY: %s / %s / %s / %s (%s) / %s (%s)' % (
                        config.get('org'), app, collection_name, e_uuid, uuid_datetime, modified, modified_date))
                    return True
                else:
                    logger.debug('DELETING CACHE: %s ' % (source_entity.get('uuid')))
                    cache.delete(source_entity.get('uuid'))
        except:
            logger.error('Error on checking cache for uuid=[%s]' % source_entity.get('uuid'))
            logger.error(traceback.format_exc())

    if exclude_collection(collection_name):
        logger.warn('Excluding entity in filtered collection [%s]' % collection_name)
        return True

    # handle duplicate user case
    if collection_name in ['users', 'user']:
        source_entity = confirm_user_entity(app, source_entity)

    source_identifier = get_source_identifier(source_entity)

    logger.info('Visiting ENTITY data [%s / %s (%s) ] at %s' % (
        collection_name, source_identifier, get_uuid_time(source_entity.get('uuid')), str(datetime.datetime.utcnow())))

    entity_copy = source_entity.copy()

    if 'metadata' in entity_copy:
        entity_copy.pop('metadata')

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    try:
        target_entity_url_by_name = put_entity_url_template.format(org=target_org,
                                                                   app=target_app,
                                                                   collection=target_collection,
                                                                   uuid=source_identifier,
                                                                   **config.get('target_endpoint'))

        r = session_target.put(url=target_entity_url_by_name, data=json.dumps(entity_copy))

        if attempts > 1:
            logger.warn('Attempt [%s] to migrate entity [%s / %s] at URL [%s]' % (
                attempts, collection_name, source_identifier, target_entity_url_by_name))
        else:
            logger.debug('Attempt [%s] to migrate entity [%s / %s] at URL [%s]' % (
                attempts, collection_name, source_identifier, target_entity_url_by_name))

        if r.status_code == 200:
            # Worked => WE ARE DONE
            logger.info(
                'migrate_data | success=[%s] | attempts=[%s] | entity=[%s / %s / %s] | created=[%s] | modified=[%s]' % (
                    True, attempts, config.get('org'), app, source_identifier, source_entity.get('created'),
                    source_entity.get('modified'),))

            if not config.get('skip_cache_write', False):
                logger.debug('SETTING CACHE | uuid=[%s] | modified=[%s]' % (
                    source_entity.get('uuid'), str(source_entity.get('modified'))))

                cache.set(source_entity.get('uuid'), str(source_entity.get('modified')))

            if collection_name in ['role', 'group', 'roles', 'groups']:
                migrate_permissions(app, collection_name, source_entity, attempts=0)

            if collection_name in ['users', 'user']:
                migrate_user_credentials(app, collection_name, source_entity, attempts=0)

            return True

        else:
            logger.error('Failure [%s] on attempt [%s] to PUT url=[%s], entity=[%s] response=[%s]' % (
                r.status_code, attempts, target_entity_url_by_name, json.dumps(entity_copy), r.text))

            if attempts >= 5:
                logger.critical(
                    'ABORT migrate_data | success=[%s] | attempts=[%s] | created=[%s] | modified=[%s] %s / %s / %s' % (
                        False, attempts, source_entity.get('created'), source_entity.get('modified'), app,
                        collection_name, source_identifier))

                return False

            if r.status_code == 400:

                if target_collection in ['roles', 'role']:
                    return repair_user_role(app, collection_name, source_entity)

                elif target_collection in ['users', 'user']:
                    return handle_user_migration_conflict(app, collection_name, source_entity)

                elif 'duplicate_unique_property_exists' in r.text:
                    logger.error(
                        'WILL NOT RETRY (duplicate) [%s] attempts to PUT url=[%s], entity=[%s] response=[%s]' % (
                            attempts, target_entity_url_by_name, json.dumps(entity_copy), r.text))

                    return False

            elif r.status_code == 403:
                logger.critical(
                    'ABORT migrate_data | success=[%s] | attempts=[%s] | created=[%s] | modified=[%s] %s / %s / %s' % (
                        False, attempts, source_entity.get('created'), source_entity.get('modified'), app,
                        collection_name, source_identifier))
                return False

    except:
        logger.error(traceback.format_exc())
        logger.error('error in migrate_data on entity: %s' % json.dumps(source_entity))

    logger.warn(
        'UNSUCCESSFUL migrate_data | success=[%s] | attempts=[%s] | entity=[%s / %s / %s] | created=[%s] | modified=[%s]' % (
            True, attempts, config.get('org'), app, source_identifier, source_entity.get('created'),
            source_entity.get('modified'),))

    return migrate_data(app, collection_name, source_entity, attempts=attempts + 1)


def handle_user_migration_conflict(app, collection_name, source_entity, attempts=0, depth=0):
    if collection_name in ['users', 'user']:
        return False

    username = source_entity.get('username')
    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    target_entity_url = get_entity_url_template.format(org=target_org,
                                                       app=target_app,
                                                       collection=target_collection,
                                                       uuid=username,
                                                       **config.get('target_endpoint'))

    # There is retry build in, here is the short circuit
    if attempts >= 5:
        logger.critical(
            'Aborting after [%s] attempts to audit user [%s] at URL [%s]' % (attempts, username, target_entity_url))

        return False

    r = session_target.get(url=target_entity_url)

    if r.status_code == 200:
        target_entity = r.json().get('entities')[0]

        if source_entity.get('created') < target_entity.get('created'):
            return repair_user_role(app, collection_name, source_entity)

    elif r.status_code / 100 == 5:
        audit_logger.warning(
            'CONFLICT: handle_user_migration_conflict failed attempt [%s] GET [%s] on TARGET URL=[%s] - : %s' % (
                attempts, r.status_code, target_entity_url, r.text))

        time.sleep(DEFAULT_RETRY_SLEEP)

        return handle_user_migration_conflict(app, collection_name, source_entity, attempts)

    else:
        audit_logger.error(
            'CONFLICT: Failed handle_user_migration_conflict attempt [%s] GET [%s] on TARGET URL=[%s] - : %s' % (
                attempts, r.status_code, target_entity_url, r.text))

        return False


def get_best_source_entity(app, collection_name, source_entity, depth=0):
    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    target_pk = 'uuid'

    if target_collection in ['users', 'user']:
        target_pk = 'username'
    elif target_collection in ['roles', 'role']:
        target_pk = 'name'

    target_name = source_entity.get(target_pk)

    # there should be no target entity now, we just need to decide which one from the source to use
    source_entity_url_by_name = get_entity_url_template.format(org=config.get('org'),
                                                               app=app,
                                                               collection=collection_name,
                                                               uuid=target_name,
                                                               **config.get('source_endpoint'))

    r_get_source_entity = session_source.get(source_entity_url_by_name)

    # if we are able to get at the source by PK...
    if r_get_source_entity.status_code == 200:

        # extract the entity from the response
        entity_from_get = r_get_source_entity.json().get('entities')[0]

        return entity_from_get

    elif r_get_source_entity.status_code / 100 == 4:
        # wasn't found, get by QL and sort
        source_entity_query_url = collection_query_url_template.format(org=config.get('org'),
                                                                       app=app,
                                                                       collection=collection_name,
                                                                       ql='select * where %s=\'%s\' order by created asc' % (
                                                                           target_pk, target_name),
                                                                       limit=config.get('limit'),
                                                                       **config.get('source_endpoint'))

        logger.info('Attempting to determine best entity from query on URL %s' % source_entity_query_url)

        q = UsergridQueryIterator(source_entity_query_url, sleep_time=config.get('error_retry_sleep'))

        desired_entity = None

        entity_counter = 0

        for e in q:
            entity_counter += 1

            if desired_entity is None:
                desired_entity = e

            elif e.get('created') < desired_entity.get('created'):
                desired_entity = e

        if desired_entity is None:
            logger.warn('Unable to determine best of [%s] entities from query on URL %s' % (
                entity_counter, source_entity_query_url))

            return source_entity

        else:
            return desired_entity

    else:
        return source_entity


def repair_user_role(app, collection_name, source_entity, attempts=0, depth=0):
    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    # For the users collection, there seemed to be cases where a USERNAME was created/existing with the a
    # different UUID which caused a 'collision' - so the point is to delete the entity with the differing
    # UUID by UUID and then do a recursive call to migrate the data - now that the collision has been cleared

    target_pk = 'uuid'

    if target_collection in ['users', 'user']:
        target_pk = 'username'
    elif target_collection in ['roles', 'role']:
        target_pk = 'name'

    target_name = source_entity.get(target_pk)

    target_entity_url_by_name = get_entity_url_template.format(org=target_org,
                                                               app=target_app,
                                                               collection=target_collection,
                                                               uuid=target_name,
                                                               **config.get('target_endpoint'))

    logger.warning('Repairing: Deleting name=[%s] entity at URL=[%s]' % (target_name, target_entity_url_by_name))

    r = session_target.delete(target_entity_url_by_name)

    if r.status_code == 200 or (r.status_code in [404, 401] and 'service_resource_not_found' in r.text):
        logger.info('Deletion of entity at URL=[%s] was [%s]' % (target_entity_url_by_name, r.status_code))

        best_source_entity = get_best_source_entity(app, collection_name, source_entity)

        target_entity_url_by_uuid = get_entity_url_template.format(org=target_org,
                                                                   app=target_app,
                                                                   collection=target_collection,
                                                                   uuid=best_source_entity.get('uuid'),
                                                                   **config.get('target_endpoint'))

        r = session_target.put(target_entity_url_by_uuid, data=json.dumps(best_source_entity))

        if r.status_code == 200:
            logger.info('Successfully repaired user at URL=[%s]' % target_entity_url_by_uuid)
            return True

        else:
            logger.critical('Failed to PUT [%s] the desired entity  at URL=[%s]: %s' % (
                r.status_code, target_entity_url_by_name, r.text))
            return False

    else:
        # log an error and keep going if we cannot delete the entity at the specified URL.  Unlikely, but if so
        # then this entity is borked
        logger.critical(
            'Deletion of entity at URL=[%s] FAILED [%s]: %s' % (target_entity_url_by_name, r.status_code, r.text))
        return False


def get_target_mapping(app, collection_name):
    target_org = config.get('org_mapping', {}).get(config.get('org'), config.get('org'))
    target_app = config.get('app_mapping', {}).get(app, app)
    target_collection = config.get('collection_mapping', {}).get(collection_name, collection_name)
    return target_app, target_collection, target_org


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

    parser.add_argument('--force_app',
                        help='Necessary for using 2.0 as a source at times due to API issues.  Forces the specified app(s) to be processed, even if they are not returned from the list of apps in the API call',
                        default=[],
                        action='append')

    parser.add_argument('--use_name_for_collection',
                        help='Name of one or more collections to use [name] instead of [uuid] for creating entities and edges',
                        default=[],
                        action='append')

    parser.add_argument('-m', '--migrate',
                        help='Specifies what to migrate: data, connections, credentials, audit or none (just iterate '
                             'the apps/collections)',
                        type=str,
                        choices=[
                            'data',
                            'prune',
                            'none',
                            'reput',
                            'credentials',
                            'graph',
                            'permissions'
                        ],
                        default='data')

    parser.add_argument('-s', '--source_config',
                        help='The path to the source endpoint/org configuration file',
                        type=str,
                        default='source.json')

    parser.add_argument('-d', '--target_config',
                        help='The path to the target endpoint/org configuration file',
                        type=str,
                        default='destination.json')

    parser.add_argument('--redis_socket',
                        help='The path to the socket for redis to use',
                        type=str)

    parser.add_argument('--limit',
                        help='The number of entities to return per query request',
                        type=int,
                        default=100)

    parser.add_argument('-w', '--entity_workers',
                        help='The number of worker processes to do the migration',
                        type=int,
                        default=16)

    parser.add_argument('--visit_cache_ttl',
                        help='The TTL of the cache of visiting nodes in the graph for connections',
                        type=int,
                        default=3600 * 2)

    parser.add_argument('--error_retry_sleep',
                        help='The number of seconds to wait between retrieving after an error',
                        type=float,
                        default=30)

    parser.add_argument('--page_sleep_time',
                        help='The number of seconds to wait between retrieving pages from the UsergridQueryIterator',
                        type=float,
                        default=0.0)

    parser.add_argument('--entity_sleep_time',
                        help='The number of seconds to wait between retrieving pages from the UsergridQueryIterator',
                        type=float,
                        default=0)

    parser.add_argument('--collection_workers',
                        help='The number of worker processes to do the migration',
                        type=int,
                        default=2)

    parser.add_argument('--queue_size_max',
                        help='The max size of entities to allow in the queue',
                        type=int,
                        default=100000)

    parser.add_argument('--graph_depth',
                        help='The graph depth to traverse to copy',
                        type=int,
                        default=3)

    parser.add_argument('--queue_watermark_high',
                        help='The point at which publishing to the queue will PAUSE until it is at or below low watermark',
                        type=int,
                        default=25000)

    parser.add_argument('--queue_watermark_low',
                        help='The point at which publishing to the queue will RESUME after it has reached the high watermark',
                        type=int,
                        default=5000)

    parser.add_argument('--ql',
                        help='The QL to use in the filter for reading data from collections',
                        type=str,
                        default='select * order by created asc')
    # default='select * order by created asc')

    parser.add_argument('--repair_data',
                        help='Repair data when iterating/migrating graph but skipping data',
                        action='store_true')

    parser.add_argument('--prune',
                        help='Prune the graph while processing (instead of the prune operation)',
                        action='store_true')

    parser.add_argument('--skip_data',
                        help='Skip migrating data (useful for connections only)',
                        action='store_true')

    parser.add_argument('--skip_credentials',
                        help='Skip migrating credentials',
                        action='store_true')

    parser.add_argument('--skip_cache_read',
                        help='Skip reading the cache (modified timestamps and graph edges)',
                        dest='skip_cache_read',
                        action='store_true')

    parser.add_argument('--skip_cache_write',
                        help='Skip updating the cache with modified timestamps of entities and graph edges',
                        dest='skip_cache_write',
                        action='store_true')

    parser.add_argument('--create_apps',
                        help='Create apps at the target if they do not exist',
                        dest='create_apps',
                        action='store_true')

    parser.add_argument('--nohup',
                        help='specifies not to use stdout for logging',
                        action='store_true')

    parser.add_argument('--graph',
                        help='Use GRAPH instead of Query',
                        dest='graph',
                        action='store_true')

    parser.add_argument('--su_username',
                        help='Superuser username',
                        required=False,
                        type=str)

    parser.add_argument('--su_password',
                        help='Superuser Password',
                        required=False,
                        type=str)

    parser.add_argument('--inbound_connections',
                        help='Name of the org to migrate',
                        action='store_true')

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

    my_args = parser.parse_args(sys.argv[1:])

    return vars(my_args)


def init():
    global config

    if config.get('migrate') == 'credentials':

        if config.get('su_password') is None or config.get('su_username') is None:
            message = 'ABORT: In order to migrate credentials, Superuser parameters (su_password, su_username) are required'
            print(message)
            logger.critical(message)
            exit()

    config['collection_mapping'] = {}
    config['app_mapping'] = {}
    config['org_mapping'] = {}

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

    if isinstance(config['source_config'], basestring):
        with open(config.get('source_config'), 'r') as f:
            config['source_config'] = json.load(f)

    if isinstance(config['target_config'], basestring):
        with open(config.get('target_config'), 'r') as f:
            config['target_config'] = json.load(f)

    if config['exclude_collection'] is None:
        config['exclude_collection'] = []

    config['source_endpoint'] = config['source_config'].get('endpoint').copy()
    config['source_endpoint'].update(config['source_config']['credentials'][config['org']])

    target_org = config.get('org_mapping', {}).get(config.get('org'), config.get('org'))

    config['target_endpoint'] = config['target_config'].get('endpoint').copy()
    config['target_endpoint'].update(config['target_config']['credentials'][target_org])


def count_bytes(entity):
    entity_copy = entity.copy()

    if 'metadata' in entity_copy:
        del entity_copy['metadata']

    entity_str = json.dumps(entity_copy)

    return len(entity_str)


def migrate_user_credentials(app, collection_name, source_entity, attempts=0):
    # this only applies to users
    if collection_name not in ['users', 'user'] \
            or config.get('skip_credentials', False):
        return False

    source_identifier = get_source_identifier(source_entity)

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    # get the URLs for the source and target users

    source_url = user_credentials_url_template.format(org=config.get('org'),
                                                      app=app,
                                                      uuid=source_identifier,
                                                      **config.get('source_endpoint'))

    target_url = user_credentials_url_template.format(org=target_org,
                                                      app=target_app,
                                                      uuid=source_identifier,
                                                      **config.get('target_endpoint'))

    # this endpoint for some reason uses basic auth...
    r = requests.get(source_url, auth=HTTPBasicAuth(config.get('su_username'), config.get('su_password')))

    if r.status_code != 200:
        logger.error('Unable to migrate credentials due to HTTP [%s] on GET URL [%s]: %s' % (
            r.status_code, source_url, r.text))

        return False

    source_credentials = r.json()

    logger.info('Putting credentials to [%s]...' % target_url)

    r = requests.put(target_url,
                     data=json.dumps(source_credentials),
                     auth=HTTPBasicAuth(config.get('su_username'), config.get('su_password')))

    if r.status_code != 200:
        logger.error(
            'Unable to migrate credentials due to HTTP [%s] on PUT URL [%s]: %s' % (
                r.status_code, target_url, r.text))
        return False

    logger.info('migrate_user_credentials | success=[%s] | app/collection/name = %s/%s/%s' % (
        True, app, collection_name, source_entity.get('uuid')))

    return True


def check_response_status(r, url, exit_on_error=True):
    if r.status_code != 200:
        logger.critical('HTTP [%s] on URL=[%s]' % (r.status_code, url))
        logger.critical('Response: %s' % r.text)

        if exit_on_error:
            exit()


def do_operation(apps_and_collections, operation):
    status_map = {}

    logger.info('Creating queues...')

    # Mac, for example, does not support the max_size for a queue in Python
    if _platform == "linux" or _platform == "linux2":
        entity_queue = Queue(maxsize=config.get('queue_size_max'))
        final_status_queue = Queue(maxsize=config.get('queue_size_max'))
        collection_queue = Queue(maxsize=config.get('queue_size_max'))
        status_queue = Queue(maxsize=config.get('queue_size_max'))
    else:
        entity_queue = Queue()
        final_status_queue = Queue()
        collection_queue = Queue()
        status_queue = Queue()

    logger.info('Starting entity_workers...')

    collection_count = 0
    # create the entity workers, but only start them (later) if there is work to do
    entity_workers = [EntityWorker(entity_queue, operation, status_queue) for x in xrange(config.get('entity_workers'))]

    # create the collection workers, but only start them (later) if there is work to do
    collection_workers = [CollectionWorker(collection_queue, entity_queue, status_queue) for x in
                          xrange(config.get('collection_workers'))]

    status_aggregator = StatusAggregator(status_queue, entity_queue, final_status_queue)

    try:
        # for each app, publish the (app_name, collection_name) to the queue.
        # this is received by a collection worker who iterates the collection and publishes
        # entities into a queue.  These are received by an individual entity worker which
        # executes the specified operation on the entity

        for app, app_data in apps_and_collections.get('apps', {}).iteritems():
            logger.info('Processing app=[%s]' % app)

            status_map[app] = {
                'iteration_started': int(round(time.time() * 1000)),
                'created_max': 267753609,
                'modified_max': 267753609,
                'created_min': 1584946416000,
                'modified_min': 1584946416000,
                'count': 0,
                'bytes': 0,
                'collections': {}
            }

            # iterate the collections which are returned.
            for collection_name in app_data.get('collections'):
                logger.info('Publishing app / collection: %s / %s' % (app, collection_name))

                collection_count += 1
                collection_queue.put((app, collection_name))

            [collection_queue.put((None, None)) for x in collection_workers]

            logger.info('Finished publishing [%s] collections for app [%s] !' % (collection_count, app))

        # only start the threads if there is work to do
        if collection_count > 0:
            status_aggregator.start()

            # start the worker processes which will iterate the collections
            [w.start() for w in collection_workers]

            # start the worker processes which will do the work of migrating
            [w.start() for w in entity_workers]

            # allow collection workers to finish using join, not wait_for
            [w.join() for w in collection_workers]

            [entity_queue.put((None, None, None)) for x in entity_workers]

            # allow entity workers to finish using join, not wait_for
            [w.join() for w in entity_workers]

            status_queue.put((None, None, None))

            status_aggregator.join()

    except KeyboardInterrupt:
        logger.warning('Keyboard Interrupt, aborting...')
        entity_queue.close()
        collection_queue.close()
        status_queue.close()

        [os.kill(super(EntityWorker, p).pid, signal.SIGINT) for p in entity_workers]
        [os.kill(super(CollectionWorker, p).pid, signal.SIGINT) for p in collection_workers]
        os.kill(super(StatusAggregator, status_aggregator).pid, signal.SIGINT)

        [w.terminate() for w in entity_workers]
        [w.terminate() for w in collection_workers]
        status_aggregator.terminate()

    logger.info('entity_workers DONE!')

    status = {'foo': 'bar'}

    while True:
        try:
            new_status = final_status_queue.get(timeout=1)

            if status is None:
                break
            else:
                status = new_status

        except Empty:
            break

    return status


def filter_apps_and_collections(org_apps):
    app_collecitons = {
        'apps': {

        }
    }

    try:
        selected_apps = config.get('app')

        # iterate the apps retrieved from the org
        for org_app in sorted(org_apps.keys()):
            logger.info('Found SOURCE App: %s' % org_app)

        time.sleep(3)

        for org_app in sorted(org_apps.keys()):
            parts = org_app.split('/')
            app = parts[1]

            # if apps are specified and the current app is not in the list, skip it
            if selected_apps and len(selected_apps) > 0 and app not in selected_apps:
                logger.warning('Skipping app [%s] not included in process list [%s]' % (app, selected_apps))
                continue

            app_collecitons['apps'][app] = {
                'collections': []
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
                logger.info('App=[%s] starting Collections=[%s]' % (app, collections))

                app_collecitons['apps'][app]['collections'] = [c for c in collections if include_collection(c)]
                logger.info('App=[%s] filtered Collections=[%s]' % (app, collections))

    except:
        print(traceback.format_exc())

    return app_collecitons


def confirm_target_org_apps(apps_and_collections):
    for app in apps_and_collections.get('apps'):

        # it is possible to map source orgs and apps to differently named targets.  This gets the
        # target names for each
        target_org = config.get('org_mapping', {}).get(config.get('org'), config.get('org'))
        target_app = config.get('app_mapping', {}).get(app, app)

        # Check that the target Org/App exists.  If not, move on to the next
        target_app_url = app_url_template.format(org=target_org,
                                                 app=target_app,
                                                 **config.get('target_endpoint'))
        logger.info('GET %s' % target_app_url)
        r_target_apps = session_target.get(target_app_url)

        if r_target_apps.status_code != 200:

            if config.get('create_apps', DEFAULT_CREATE_APPS):
                create_app_url = org_management_app_url_template.format(org=target_org,
                                                                        app=target_app,
                                                                        **config.get('target_endpoint'))
                app_request = {'name': target_app}
                r = session_target.post(create_app_url, data=json.dumps(app_request))

                if r.status_code != 200:
                    logger.critical('--create_apps specified and unable to create app [%s] at URL=[%s]: %s' % (
                        target_app, create_app_url, r.text))
                    logger.critical('Process will now exit')
                    exit()
                else:
                    logger.warning('Created app=[%s] at URL=[%s]: %s' % (target_app, create_app_url, r.text))
            else:
                logger.critical('Target application DOES NOT EXIST at [%s] URL=%s' % (
                    r_target_apps.status_code, target_app_url))
                continue


def main():
    global config, cache

    args = parse_args()

    print(args)

    perform_migration(args)


def perform_migration(parameters):
    global config, cache, ecid

    migration_started = int(round(time.time() * 1000))

    ecid = str(uuid.uuid1())
    config = parameters
    init()

    init_logging(parameters.get('log_dir', '/tmp'),
                 parameters.get('log_level', 'INFO'),
                 parameters.get('org', 'NONE'),
                 parameters.get('migrate', 'NONE'))

    logger.warn('Migration starting for org %s' % config['org'])

    try:
        if config.get('redis_socket') is not None:
            cache = redis.Redis(unix_socket_path=config.get('redis_socket'))

        else:
            # this does not try to connect to redis
            cache = redis.StrictRedis(host='localhost', port=6379, db=0)

        # this is necessary to test the connection to redis
        cache.get('usergrid')

    except:
        logger.error(
            'Error connecting to Redis cache, consider using Redis to be able to optimize the migration process...')

        config['use_cache'] = False
        config['skip_cache_read'] = True
        config['skip_cache_write'] = True

    org_apps = {
    }

    force_apps = config.get('force_app', [])

    if force_apps is not None and len(force_apps) > 0:
        logger.warn('Forcing only the following apps to be processed: %s' % force_apps)

        for app in force_apps:
            key = '%s/%s' % (app, app)
            org_apps[key] = app

    if len(org_apps) == 0:
        source_org_mgmt_url = org_management_url_template.format(org=config.get('org'),
                                                                 limit=config.get('limit'),
                                                                 **config.get('source_endpoint'))

        print('Retrieving apps from [%s]' % source_org_mgmt_url)
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

        except Exception:
            logger.exception('ERROR Retrieving apps from [%s]' % source_org_mgmt_url)
            print(traceback.format_exc())
            logger.critical('Unable to retrieve apps from [%s] and will exit' % source_org_mgmt_url)
            exit()

    # Check the specified configuration for what to migrate/audit
    if config.get('migrate') == 'graph':
        operation = migrate_graph

    elif config.get('migrate') == 'data':
        operation = migrate_data

    elif config.get('migrate') == 'prune':
        operation = prune_graph

    elif config.get('migrate') == 'permissions':
        operation = migrate_permissions
        config['collection'] = ['roles', 'groups']
        logger.warn(
            'Since permissions migration was specified, overwriting included collections to be %s...' % config[
                'collection'])

    elif config.get('migrate') == 'credentials':
        operation = migrate_user_credentials
        config['collection'] = ['users']
        logger.warn('Since credential migration was specified, overwriting included collections to be %s' % config[
            'collection'])

    elif config.get('migrate') == 'reput':
        operation = reput

    else:
        operation = None

    # filter out the apps and collections based on the -c and --exclude_collection directives
    apps_and_collections = filter_apps_and_collections(org_apps)

    logger.warn('The following apps/collections will be processed: %s' % json.dumps(apps_and_collections))

    # confirm the apps exist at the target/destination org
    confirm_target_org_apps(apps_and_collections)

    # execute the operation over apps and collections
    status = do_operation(apps_and_collections, operation)

    status['summary']['migration_finished'] = int(round(time.time() * 1000))
    status['summary']['migration_started'] = migration_started

    logger.warn('Migration for org [%s] finished' % config['org'])

    return status


def testfile():
    with open('config.json', 'r') as f:
        config = json.load(f)

    config.update(config_defaults)

    perform_migration(config)


def send_start_notification(config):
    ses_conn = boto.ses.connect_to_region('us-east-1')
    response = ses_conn.send_email(
        source='no-reply@apigee.com',
        subject='Migration of BaaS org [%s] Started' % config['org'],
        to_addresses='jwest@apigee.com',
        body='<EOM>'
    )


def send_complete_notification(config, status):
    ses_conn = boto.ses.connect_to_region('us-east-1')
    response = ses_conn.send_email(
        source='no-reply@apigee.com',
        subject='Migration of BaaS org [%s] Completed' % config['org'],
        to_addresses='jwest@apigee.com',
        body=json.dumps(status, indent=2, sort_keys=True)
    )


def pre_process_config(str_config):
    params = json.loads(str_config)
    params.update(config_defaults)

    params['map_org'] = collapse_mapping(params, 'map_org')
    params['map_app'] = collapse_mapping(params, 'map_app')
    params['map_collection'] = collapse_mapping(params, 'map_collection')

    return params


def collapse_mapping(params, key):
    mappings = []

    for source, target in params.get(key, {}).iteritems():
        mappings.append('%s:%s' % (source, target))

    return mappings


def sqs_listener():
    import os

    if 'AWS_ACCESS_KEY_ID' not in os.environ or 'AWS_SECRET_ACCESS_KEY' not in os.environ:
        print('AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY must be set in the env variables')
        exit(1)

    print("Starting sqs_listener()")
    parameters = {}

    init_logging(parameters.get('log_dir', '/tmp'),
                 parameters.get('log_level', 'INFO'),
                 parameters.get('org', 'NONE'),
                 parameters.get('migrate', 'NONE'))

    logger.info("Starting sqs_listener()")

    queue_name = 'baas-migration-requests'

    try:
        logger.info('Connecting to SQS Queue %s...' % queue_name)

        sqs_conn = boto.sqs.connect_to_region(region_name="us-east-1")

        sqs_queue = sqs_conn.get_queue(queue_name)

        # if the queue cannot be found the SQS_queue object will be null
        if not sqs_queue:
            logger.error('Unable to connect to SQS Queue %s' % queue_name)
            return

        logger.info('Connected to SQS Queue %s' % queue_name)

        # this architecture is designed to consume raw messages.
        # This means that the SQS messages will not have the associated metadata added by SQS
        sqs_queue.set_message_class(RawMessage)

    except Exception as e:
        logger.error(e)
        print(traceback.format_exc())
        return

    message_counter = 0

    try:
        # loop until keyboard kill
        while True:
            try:
                # read messages in a batch of 2x per consumer max
                logger.debug('Reading from SQS...')

                sqs_messages = sqs_queue.get_messages(num_messages=1,
                                                      wait_time_seconds=20)

                logger.debug('Read [%s] messages!' % (len(sqs_messages)))

                if sqs_messages:
                    logger.info('Read [%s] messages!' % len(sqs_messages))

                    # put each message in the local queue
                    for sqs_message in sqs_messages:
                        message_counter += 1

                        try:
                            logger.info(sqs_message.get_body())
                            str_config = sqs_message.get_body()
                            params = pre_process_config(str_config)
                            sqs_message.delete()

                        except Exception as e:
                            logger.error("Error parsing message or enqueueing it", exc_info=True)
                            print(traceback.format_exc())

                        status = perform_migration(params)

                        send_complete_notification(params, status)

            except Empty:
                logger.info('No messages, sleeping 60s')
                time.sleep(60)

    except Exception as e:
        logger.error("Top Level Error", exc_info=True)


if __name__ == "__main__":
    # main()
    sqs_listener()

if __name__ == "__testfile__":
    testfile()

if __name__ == "__sqs_listener__":
    sqs_listener()
