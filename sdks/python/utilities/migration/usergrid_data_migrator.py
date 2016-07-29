#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#   contributor license agreements.  The ASF licenses this file to You
#  under the Apache License, Version 2.0 (the "License"); you may not
#  use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.  For additional information regarding
#  copyright in this work, please see the NOTICE file in the top level
#  directory of this distribution.

from Queue import Empty
import argparse
import json
import logging
import sys
from multiprocessing import Queue, Process
import requests
import traceback
from logging.handlers import RotatingFileHandler
import time
from usergrid import UsergridQueryIterator
from requests.auth import HTTPBasicAuth
import urllib3

__author__ = 'Jeff West @ ApigeeCorporation'

logger = logging.getLogger('Main')
worker_logger = logging.getLogger('DataMigrator')
data_logger = logging.getLogger('DataMigrator')
audit_logger = logging.getLogger('AuditLogger')
connection_logger = logging.getLogger('ConnectionMigrator')
credential_logger = logging.getLogger('CredentialMigrator')

urllib3.disable_warnings()

DEFAULT_CREATE_APPS = False
DEFAULT_RETRY_SLEEP = 2
DEFAULT_PROCESSING_SLEEP = 5

session_source = requests.Session()
session_destination = requests.Session()


def init_logging(stdout_enabled=True):
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    logging.getLogger('requests.packages.urllib3.connectionpool').setLevel(logging.ERROR)
    logging.getLogger('boto').setLevel(logging.ERROR)
    logging.getLogger('urllib3.connectionpool').setLevel(logging.WARN)

    log_formatter = logging.Formatter(fmt='%(asctime)s | %(name)s | %(processName)s | %(levelname)s | %(message)s',
                                      datefmt='%m/%d/%Y %I:%M:%S %p')

    if stdout_enabled:
        stdout_logger = logging.StreamHandler(sys.stdout)
        stdout_logger.setFormatter(log_formatter)
        stdout_logger.setLevel(logging.INFO)
        root_logger.addHandler(stdout_logger)

    # base log file

    log_file_name = './migrator.log'
    rotating_file = logging.handlers.RotatingFileHandler(filename=log_file_name,
                                                         mode='a',
                                                         maxBytes=204857600,
                                                         backupCount=10)
    rotating_file.setFormatter(log_formatter)
    rotating_file.setLevel(logging.INFO)

    root_logger.addHandler(rotating_file)

    # ERROR LOG

    error_log_file_name = './migrator_errors.log'
    error_rotating_file = logging.handlers.RotatingFileHandler(filename=error_log_file_name,
                                                               mode='a',
                                                               maxBytes=204857600,
                                                               backupCount=10)
    error_rotating_file.setFormatter(log_formatter)
    error_rotating_file.setLevel(logging.ERROR)

    root_logger.addHandler(error_rotating_file)

    # AUDIT LOG

    audit_log_file_name = './migrator_audit.log'
    audit_rotating_file = logging.handlers.RotatingFileHandler(filename=audit_log_file_name,
                                                               mode='a',
                                                               maxBytes=204857000,
                                                               backupCount=10)
    audit_rotating_file.setFormatter(log_formatter)
    audit_rotating_file.setLevel(logging.WARNING)

    audit_logger.addHandler(audit_rotating_file)

    # DATA LOG

    data_log_file_name = './migrator_data.log'
    data_rotating_file = logging.handlers.RotatingFileHandler(filename=data_log_file_name,
                                                              mode='a',
                                                              maxBytes=204857600,
                                                              backupCount=10)
    data_rotating_file.setFormatter(log_formatter)
    data_rotating_file.setLevel(logging.WARNING)

    data_logger.addHandler(data_rotating_file)

    # CONNECTION LOG

    connections_log_file_name = './migrator_connections.log'
    connections_rotating_file = logging.handlers.RotatingFileHandler(filename=connections_log_file_name,
                                                                     mode='a',
                                                                     maxBytes=204857600,
                                                                     backupCount=10)
    connections_rotating_file.setFormatter(log_formatter)
    connections_rotating_file.setLevel(logging.WARNING)

    connection_logger.addHandler(connections_rotating_file)

    # CREDENTIALS LOG

    credential_log_file_name = './migrator_credentials.log'
    credentials_rotating_file = logging.handlers.RotatingFileHandler(filename=credential_log_file_name,
                                                                     mode='a',
                                                                     maxBytes=204857600,
                                                                     backupCount=10)
    credentials_rotating_file.setFormatter(log_formatter)
    credentials_rotating_file.setLevel(logging.WARNING)

    credential_logger.addHandler(credentials_rotating_file)


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
connection_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}?client_id={client_id}&client_secret={client_secret}&limit={limit}"
connection_create_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_uuid}?client_id={client_id}&client_secret={client_secret}"
get_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}&connections=none"

get_entity_url_with_connections_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"
put_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"

user_credentials_url_template = "{api_url}/{org}/{app}/users/{uuid}/credentials"

ignore_collections = ['activities', 'queues', 'events', 'notifications']


class Worker(Process):
    def __init__(self, queue, handler_function):
        super(Worker, self).__init__()
        worker_logger.debug('Creating worker!')
        self.queue = queue
        self.handler_function = handler_function

    def run(self):

        worker_logger.info('starting run()...')
        keep_going = True

        count_processed = 0
        empty_count = 0

        while keep_going:

            try:
                app, collection_name, entity = self.queue.get(timeout=60)
                empty_count = 0

                if self.handler_function is not None:
                    processed = self.handler_function(app, collection_name, entity)

                    if processed:
                        count_processed += 1

                        worker_logger.debug('Processed [%sth] entity = %s/%s/%s' % (
                            count_processed, app, collection_name, entity.get('uuid')))

                        if count_processed % 1000 == 1:
                            worker_logger.info('Processed [%sth] entity = %s/%s/%s' % (
                                count_processed, app, collection_name, entity.get('uuid')))

            except KeyboardInterrupt, e:
                raise e

            except Empty:
                worker_logger.warning('EMPTY! Count=%s' % empty_count)

                empty_count += 1

                if empty_count >= 10:
                    keep_going = False


def migrate_connections(app, collection_name, source_entity, attempts=0):
    attempts += 1
    response = False

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    source_uuid = source_entity.get('uuid')
    count_edges = 0
    count_edge_names = 0
    include_edges = config.get('include_edge', [])

    if include_edges is None:
        include_edges = []

    exclude_edges = config.get('exclude_edge', [])

    if exclude_edges is None:
        exclude_edges = []

    try:
        connections = source_entity.get('metadata', {}).get('collections', {})

        count_edge_names = len(connections)

        connection_logger.debug('Processing [%s] connection types of entity [%s/%s/%s]' % (
            count_edge_names, target_app, collection_name, source_uuid))

        for connection_name in connections:

            if len(include_edges) > 0 and connection_name not in include_edges:
                connection_logger.debug(
                        'Skipping edge [%s] since it is not in INCLUDED list: %s' % (connection_name, include_edges))
                continue

            if connection_name in exclude_edges:
                connection_logger.debug(
                        'Skipping edge [%s] since it is in EXCLUDED list: %s' % (connection_name, exclude_edges))
                continue

            if (collection_name == 'devices' and connection_name in ['users', 'receipts']) or \
                    (collection_name == 'receipts' and connection_name in ['devices']) or \
                    (collection_name == 'users' and connection_name in ['roles', 'followers', 'groups',
                                                                        'feed', 'activities']):
                # feed and activities are not retrievable...
                # roles and groups will be more efficiently handled from the role/group -> user
                # followers will be handled by 'following'
                # do only this from user -> device
                continue

            connection_logger.debug('Processing connections [%s] of entity [%s/%s/%s]' % (
                connection_name, app, collection_name, source_uuid))

            connection_query_url = connection_query_url_template.format(
                    org=config.get('org'),
                    app=app,
                    verb=connection_name,
                    collection=collection_name,
                    uuid=source_uuid,
                    **config.get('source_endpoint'))

            connection_query = UsergridQuery(connection_query_url)

            connection_stack = []

            for e_connection in connection_query:
                target_connection_collection = config.get('collection_mapping', {}).get(e_connection.get('type'),
                                                                                        e_connection.get('type'))

                connection_logger.info('Connecting entity [%s/%s/%s] --[%s]--> [%s/%s/%s]' % (
                    app, collection_name, source_uuid, connection_name, target_app, target_connection_collection,
                    e_connection.get('uuid')))

                count_edges += 1
                connection_stack.append(e_connection)

            while len(connection_stack) > 0:

                e_connection = connection_stack.pop()

                create_connection_url = connection_create_url_template.format(
                        org=target_org,
                        app=target_app,
                        collection=target_collection,
                        uuid=source_uuid,
                        verb=connection_name,
                        target_uuid=e_connection.get('uuid'),
                        **config.get('target_endpoint'))

                connection_logger.info('CREATE: ' + create_connection_url)

                attempts = 0

                while attempts < 5:
                    attempts += 1

                    r_create = requests.post(create_connection_url)

                    if r_create.status_code == 200:
                        response = True
                        break

                    elif r_create.status_code == 401:
                        connection_logger.warning(
                                'FAILED to create connection at URL=[%s]: %s' % (create_connection_url, r_create.text))

                        if attempts < 5:
                            connection_logger.warning('WILL Retry')
                            time.sleep(DEFAULT_RETRY_SLEEP)
                        else:
                            response = False
                            connection_stack = []
                            connection_logger.critical('WILL NOT RETRY: FAILED to create connection at URL=[%s]: %s' % (
                                create_connection_url, r_create.text))

        response = True

    except:
        connection_logger.error(traceback.format_exc())
        connection_logger.critical(
                'error in migrate_connections on entity: \n %s' % json.dumps(source_entity, indent=2))

    if count_edges > 0:
        connection_logger.info(
                'migrate_connections | success=[%s] | entity = %s/%s/%s | edge_types=[%s] | edges=[%s]' % (
                    response, app, collection_name, source_entity.get('uuid'), count_edge_names, count_edges))

    return response


def migrate_user_credentials(app, collection_name, source_entity, attempts=0):
    # this only applies to users
    if collection_name != 'users':
        return False

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    # get the URLs for the source and target users

    source_url = user_credentials_url_template.format(org=config.get('org'),
                                                      app=app,
                                                      uuid=source_entity.get('uuid'),
                                                      **config.get('source_endpoint'))

    target_url = user_credentials_url_template.format(org=target_org,
                                                      app=target_app,
                                                      uuid=source_entity.get('uuid'),
                                                      **config.get('target_endpoint'))

    # this endpoint for some reason uses basic auth...
    r = requests.get(source_url, auth=HTTPBasicAuth(config.get('su_username'), config.get('su_password')))

    if r.status_code != 200:
        credential_logger.error('Unable to migrate credentials due to HTTP [%s] on GET URL [%s]: %s' % (
            r.status_code, source_url, r.text))

        return False

    source_credentials = r.json()

    credential_logger.info('Putting credentials to [%s]...' % target_url)

    r = requests.put(target_url,
                     data=json.dumps(source_credentials),
                     auth=HTTPBasicAuth(config.get('su_username'), config.get('su_password')))

    if r.status_code != 200:
        credential_logger.error(
                'Unable to migrate credentials due to HTTP [%s] on PUT URL [%s]: %s' % (
                    r.status_code, target_url, r.text))
        return False

    credential_logger.info('migrate_user_credentials | success=[%s] | app/collection/name = %s/%s/%s' % (
        True, app, collection_name, source_entity.get('uuid')))

    return True


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


def migrate_data(app, collection_name, source_entity, attempts=0):
    # handle duplicate user case
    if collection_name == 'users':
        source_entity = confirm_user_entity(app, source_entity)

    response = False

    attempts += 1

    if 'metadata' in source_entity:
        source_entity.pop('metadata')

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    try:
        target_entity_url_by_name = put_entity_url_template.format(org=target_org,
                                                                   app=target_app,
                                                                   collection=target_collection,
                                                                   uuid=source_entity.get('uuid'),
                                                                   **config.get('target_endpoint'))

        if attempts >= 5:
            data_logger.critical('ABORT migrate_data | success=[%s] | attempts=[%s] | created=[%s] %s/%s/%s' % (
                True, attempts, source_entity.get('created'), app, collection_name, source_entity.get('uuid')))

            return False

        logger.info('Attempt [%s] to migrate entity [%s/%s] at URL [%s]' % (
            attempts, collection_name, source_entity.get('uuid'), target_entity_url_by_name))

        r = requests.put(url=target_entity_url_by_name, data=json.dumps(source_entity))

        if r.status_code == 200:
            # Worked => WE ARE DONE
            data_logger.info('migrate_data | success=[%s] | attempts=[%s] | created=[%s] %s/%s/%s' % (
                True, attempts, source_entity.get('created'), app, collection_name, source_entity.get('uuid')))

            return True

        elif r.status_code == 400 and target_collection in ['roles']:
            return repair_user_role(app, collection_name, source_entity)

        elif r.status_code == 400 and target_collection in ['users']:
            return handle_user_migration_conflict(app, collection_name, source_entity)

        else:
            data_logger.error('Failure [%s] on attempt [%s] to PUT url=[%s], entity=[%s] response=[%s]' % (
                r.status_code, attempts, target_entity_url_by_name, json.dumps(source_entity), r.text))

    except:
        data_logger.error(traceback.format_exc())
        data_logger.error('error in migrate_data on entity: %s' % json.dumps(source_entity))

    return migrate_data(app, collection_name, source_entity, attempts)


def audit_data(app, collection_name, source_entity, attempts=0):
    if collection_name == 'users':
        return audit_user(app, collection_name, source_entity, attempts)

    # simple process - check the target org/app/collection/{uuid|name} to see if it exists.
    # If it exists, move on.  If not, and repair is enabled then attempt to migrate it now and reattempt audit

    attempts += 1

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    target_entity_url = get_entity_url_template.format(org=target_org,
                                                       app=target_app,
                                                       collection=target_collection,
                                                       uuid=source_entity.get('uuid'),
                                                       **config.get('target_endpoint'))

    r = requests.get(url=target_entity_url)

    # short circuit after 5 attempts
    if attempts >= 5:
        logger.critical('ABORT after [%s] attempts to audit entity [%s/%s] at URL [%s]' % (
            attempts, collection_name, source_entity.get('uuid'), target_entity_url))
        return False

    if r.status_code == 200:
        # entity found, audit success
        return True

    else:
        audit_logger.warning('AUDIT: Repair=[%s] Entity not found on attempt [%s] at TARGET URL=[%s] - [%s]: %s' % (
            config.get('repair'), attempts, target_entity_url, r.status_code, r.text))

        # if repaid is enabled, attempt to migrate the data followed by an audit
        if config.get('repair', False):

            response = migrate_data(app, collection_name, source_entity, attempts=0)

            if response:
                # migration success, sleep for processing
                time.sleep(DEFAULT_PROCESSING_SLEEP)

                # attempt a reaudit after migration
                return audit_data(app, collection_name, source_entity, attempts)

            else:
                audit_logger.critical(
                        'AUDIT: ABORT after attempted migration and entity not found on attempt [%s] at TARGET URL=[%s] - [%s]: %s' % (
                            attempts, target_entity_url, r.status_code, r.text))

    return audit_data(app, collection_name, source_entity, attempts)


def audit_user(app, collection_name, source_entity, attempts=0):
    if collection_name != 'users':
        return False

    attempts += 1
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

    r = requests.get(url=target_entity_url)

    # check to see if the entity is at the target by username
    # if the entity is there, check that the UUID is the same
    # if the UUID is not the same, then check the created date
    # If the created date of the target is after the created date of the source, then overwrite it with the entity in
    # the parameters

    if r.status_code != 200:
        audit_logger.info(
                'Failed attempt [%s] to GET [%s] URL=[%s]: %s' % (attempts, r.status_code, target_entity_url, r.text))

    if r.status_code == 200:
        target_entity = r.json().get('entities')[0]

        best_source_entity = get_best_source_entity(app, collection_name, source_entity)

        # compare uuids to make sure they match

        if target_entity.get('uuid') == best_source_entity.get('uuid') or target_entity.get(
                'created') <= best_source_entity.get('created'):

            if target_entity.get('uuid') != best_source_entity.get('uuid'):
                # UUIDs match and/or created at target is less than best we know of, audit complete
                audit_logger.info('GOOD user [%s] best/source UUID/created=[%s/%s] target UUID/created [%s/%s]' % (
                    username, best_source_entity.get('uuid'), best_source_entity.get('created'),
                    target_entity.get('uuid'),
                    target_entity.get('created')))

            return True

        else:
            audit_logger.warn('Repairing user [%s] best/source UUID/created=[%s/%s] target UUID/created [%s/%s]' % (
                username, best_source_entity.get('uuid'), best_source_entity.get('created'), target_entity.get('uuid'),
                target_entity.get('created')))

            return repair_user_role(app, collection_name, source_entity)

    elif r.status_code / 100 == 4:
        audit_logger.info('AUDIT: HTTP [%s] on URL=[%s]: %s' % (r.status_code, target_entity_url, r.text))

        return repair_user_role(app, collection_name, source_entity)

    else:
        audit_logger.warning('AUDIT: Failed attempt [%s] GET [%s] on TARGET URL=[%s] - : %s' % (
            attempts, r.status_code, target_entity_url, r.text))

        time.sleep(DEFAULT_RETRY_SLEEP)

        return audit_data(app, collection_name, source_entity, attempts)


def handle_user_migration_conflict(app, collection_name, source_entity, attempts=0):
    if collection_name != 'users':
        return False

    attempts += 1
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

    r = requests.get(url=target_entity_url)

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


def get_best_source_entity(app, collection_name, source_entity):
    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    target_pk = 'uuid'

    if target_collection == 'users':
        target_pk = 'username'
    elif target_collection == 'roles':
        target_pk = 'name'

    target_name = source_entity.get(target_pk)

    # there should be no target entity now, we just need to decide which one from the source to use
    source_entity_url_by_name = get_entity_url_template.format(org=config.get('org'),
                                                               app=app,
                                                               collection=collection_name,
                                                               uuid=target_name,
                                                               **config.get('source_endpoint'))

    r_get_source_entity = requests.get(source_entity_url_by_name)

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
                                                                       **config.get('source_endpoint'))

        data_logger.info('Attempting to determine best entity from query on URL %s' % source_entity_query_url)

        q = UsergridQuery(source_entity_query_url)

        desired_entity = None

        entity_counter = 0

        for e in q:
            entity_counter += 1

            if desired_entity is None:
                desired_entity = e

            elif e.get('created') < desired_entity.get('created'):
                desired_entity = e

        if desired_entity is None:
            data_logger.warn('Unable to determine best of [%s] entities from query on URL %s' % (
                entity_counter, source_entity_query_url))

            return source_entity

        else:
            return desired_entity

    else:
        return source_entity


def repair_user_role(app, collection_name, source_entity, attempts=0):
    if not config.get('repair', False):
        return False

    target_app, target_collection, target_org = get_target_mapping(app, collection_name)

    # For the users collection, there seemed to be cases where a USERNAME was created/existing with the a
    # different UUID which caused a 'collision' - so the point is to delete the entity with the differing
    # UUID by UUID and then do a recursive call to migrate the data - now that the collision has been cleared

    target_pk = 'uuid'

    if target_collection == 'users':
        target_pk = 'username'
    elif target_collection == 'roles':
        target_pk = 'name'

    target_name = source_entity.get(target_pk)

    target_entity_url_by_name = get_entity_url_template.format(org=target_org,
                                                               app=target_app,
                                                               collection=target_collection,
                                                               uuid=target_name,
                                                               **config.get('target_endpoint'))

    data_logger.warning('Repairing: Deleting name=[%s] entity at URL=[%s]' % (target_name, target_entity_url_by_name))

    r = requests.delete(target_entity_url_by_name)

    if r.status_code == 200 or (r.status_code in [404, 401] and 'service_resource_not_found' in r.text):
        data_logger.info('Deletion of entity at URL=[%s] was [%s]' % (target_entity_url_by_name, r.status_code))

        best_source_entity = get_best_source_entity(app, collection_name, source_entity)

        target_entity_url_by_uuid = get_entity_url_template.format(org=target_org,
                                                                   app=target_app,
                                                                   collection=target_collection,
                                                                   uuid=best_source_entity.get('uuid'),
                                                                   **config.get('target_endpoint'))

        r = requests.put(target_entity_url_by_uuid, data=json.dumps(best_source_entity))

        if r.status_code == 200:
            data_logger.info('Successfully repaired user at URL=[%s]' % target_entity_url_by_uuid)
            return True

        else:
            data_logger.critical('Failed to PUT [%s] the desired entity  at URL=[%s]: %s' % (
                r.status_code, target_entity_url_by_name, r.text))
            return False

    else:
        # log an error and keep going if we cannot delete the entity at the specified URL.  Unlikely, but if so
        # then this entity is borked
        data_logger.critical(
                'Deletion of entity at URL=[%s] FAILED [%s]: %s' % (target_entity_url_by_name, r.status_code, r.text))
        return False


def get_target_mapping(app, collection_name):
    target_org = config.get('org_mapping', {}).get(config.get('org'), config.get('org'))
    target_app = config.get('app_mapping', {}).get(app, app)
    target_collection = config.get('collection_mapping', {}).get(collection_name, collection_name)
    return target_app, target_collection, target_org


def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Org/App Migrator')

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

    parser.add_argument('-c', '--collection',
                        help='Name of one or more collections to include, specify none to include all collections',
                        default=[],
                        action='append')

    parser.add_argument('-m', '--migrate',
                        help='Specifies what to migrate: data, connections, credentials, audit or none (just iterate '
                             'the apps/collections)',
                        type=str,
                        choices=['data', 'connections', 'credentials', 'none', 'audit_data'],
                        default='data')

    parser.add_argument('-s', '--source_config',
                        help='The path to the source endpoint/org configuration file',
                        type=str,
                        default='source.json')

    parser.add_argument('-d', '--target_config',
                        help='The path to the target endpoint/org configuration file',
                        type=str,
                        default='destination.json')

    parser.add_argument('-w', '--workers',
                        help='The number of worker processes to do the migration',
                        type=int,
                        default=16)

    parser.add_argument('--ql',
                        help='The QL to use in the filter for reading data from collections',
                        type=str,
                        default='select * order by created asc')

    parser.add_argument('--create_apps',
                        dest='create_apps',
                        action='store_true')

    parser.add_argument('--repair',
                        help='Attempt to migrate missing data',
                        dest='repair',
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
            message = 'In order to migrate credentials, Superuser parameters (su_password, su_username) are required'
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

    with open(config.get('source_config'), 'r') as f:
        config['source_config'] = json.load(f)

    with open(config.get('target_config'), 'r') as f:
        config['target_config'] = json.load(f)

    config['source_endpoint'] = config['source_config'].get('endpoint').copy()
    config['source_endpoint'].update(config['source_config']['credentials'][config['org']])

    target_org = config.get('org_mapping', {}).get(config.get('org'), config.get('org'))

    config['target_endpoint'] = config['target_config'].get('endpoint').copy()
    config['target_endpoint'].update(config['target_config']['credentials'][target_org])


def wait_for(threads, sleep_time=60):
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


def get_token(endpoint_config):

    token_request = {}

    pass


def main():
    global config

    config = parse_args()
    init()

    init_logging()

    # get source token
    # token = get_token(config['source_config'])

    # list the apps for the SOURCE org
    source_org_mgmt_url = org_management_url_template.format(org=config.get('org'), **config.get('source_endpoint'))

    logger.info('GET %s' % source_org_mgmt_url)
    r = requests.get(source_org_mgmt_url)

    check_response_status(r, source_org_mgmt_url)

    logger.info(json.dumps(r.json()))

    org_apps = r.json().get('data')

    queue = Queue()
    logger.info('Starting workers...')

    # Check the specified configuration for what to migrate/audit
    if config.get('migrate') == 'connections':
        operation = migrate_connections
    elif config.get('migrate') == 'data':
        operation = migrate_data
    elif config.get('migrate') == 'credentials':
        operation = migrate_user_credentials
    elif config.get('migrate') == 'audit_data':
        operation = audit_data
    else:
        operation = None

    # start the worker processes which will do the work of migrating
    workers = [Worker(queue, operation) for x in xrange(config.get('workers'))]

    try:
        [w.start() for w in workers]

        apps_to_process = config.get('app')
        collections_to_process = config.get('collection')

        # iterate the apps retrieved from the org
        for org_app in sorted(org_apps.keys()):
            logger.info('Found SOURCE App: %s' % org_app)

        time.sleep(3)

        for org_app in sorted(org_apps.keys()):
            max_created = -1
            parts = org_app.split('/')
            app = parts[1]

            # if apps are specified and the current app is not in the list, skip it
            if apps_to_process and len(apps_to_process) > 0 and app not in apps_to_process:
                logger.warning('Skipping app=[%s]' % app)
                continue

            logger.info('Processing app=[%s]' % app)

            # it is possible to map source orgs and apps to differently named targets.  This gets the
            # target names for each
            target_org = config.get('org_mapping', {}).get(config.get('org'), config.get('org'))
            target_app = config.get('app_mapping', {}).get(app, app)

            # Check that the target Org/App exists.  If not, move on to the next
            target_app_url = app_url_template.format(org=target_org,
                                                     app=target_app,
                                                     **config.get('target_endpoint'))
            logger.info('GET %s' % target_app_url)
            r_target_apps = requests.get(target_app_url)

            if r_target_apps.status_code != 200:

                if config.get('create_apps', DEFAULT_CREATE_APPS):
                    create_app_url = org_management_app_url_template.format(org=target_org,
                                                                            app=target_app,
                                                                            **config.get('target_endpoint'))
                    app_request = {'name': target_app}
                    r = requests.post(create_app_url, data=json.dumps(app_request))

                    if r.status_code != 200:
                        logger.critical(
                                'Unable to create app [%s] at URL=[%s]: %s' % (target_app, create_app_url, r.text))
                        continue
                    else:
                        logger.warning('Created app=[%s] at URL=[%s]: %s' % (target_app, create_app_url, r.text))
                else:
                    logger.critical(
                            'Target application does not exist at [%s] URL=%s' % (
                                r_target_apps.status_code, target_app_url))
                    continue

            # get the list of collections from the source org/app
            source_app_url = app_url_template.format(org=config.get('org'),
                                                     app=app,
                                                     **config.get('source_endpoint'))
            logger.info('GET %s' % source_app_url)

            r_collections = requests.get(source_app_url)

            collection_attempts = 0

            # sometimes this call was not working so I put it in a loop to force it...
            while r_collections.status_code != 200 and collection_attempts < 5:
                collection_attempts += 1
                logger.warning('FAILED: GET (%s) [%s] URL: %s' % (r_collections.elapsed, r_collections.status_code,
                                                                  source_app_url))
                time.sleep(DEFAULT_RETRY_SLEEP)
                r_collections = requests.get(source_app_url)

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

                    # filter out collections as configured...
                    if collection_name in ignore_collections \
                            or (len(collections_to_process) > 0 and collection_name not in collections_to_process) \
                            or (config.get('migrate') == 'credentials' and collection_name != 'users'):
                        logger.warning('Skipping collection=[%s]' % collection_name)
                        continue

                    logger.info('Processing collection=%s' % collection_name)
                    counter = 0

                    # added a flag for using graph vs query/index
                    if config.get('graph', False):
                        source_collection_url = collection_graph_url_template.format(org=config.get('org'),
                                                                                     app=app,
                                                                                     collection=collection_name,
                                                                                     **config.get('source_endpoint'))
                    else:
                        source_collection_url = collection_query_url_template.format(org=config.get('org'),
                                                                                     app=app,
                                                                                     collection=collection_name,
                                                                                     ql=config.get('ql'),
                                                                                     **config.get('source_endpoint'))

                    # use the UsergridQuery from the Python SDK to iterate the collection
                    q = UsergridQuery(source_collection_url)

                    for entity in q:
                        if 'created' in entity:
                            try:
                                entity_created = long(entity.get('created'))
                                if entity_created > max_created:
                                    max_created = entity_created

                            except ValueError:
                                pass

                        counter += 1
                        queue.put((app, collection_name, entity))

                    logger.warning('Max Created entity for org/app/collection= %s/%s/%s is %s' % (
                        config.get('org'), app, collection_name, max_created))

            logger.info('DONE!')

        wait_for(workers)

        logger.info('workers DONE!')

    except KeyboardInterrupt:
        logger.warning('Keyboard Interrupt, aborting...')
        [w.terminate() for w in workers]


def check_response_status(r, url, exit_on_error=True):
    if r.status_code != 200:
        logger.critical('HTTP [%s] on URL=[%s]' % (r.status_code, url))
        logger.critical('Response: %s' % r.text)

        if exit_on_error:
            exit()


main()
