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

from Queue import Empty
import json
import logging
import sys
from multiprocessing import Queue, Process
import traceback
from logging.handlers import RotatingFileHandler
import time

import argparse

from usergrid import UsergridClient, UsergridError

__author__ = 'Jeff.West@yahoo.com'

logger = logging.getLogger('UsergridIterator')

# SAMPLE CONFIG FILE for source and target
sample_config = {
    "endpoint": {
        "api_url": "https://api.usergrid.com",
        "limit": 100
    },

    "credentials": {
        "myOrg": {
            "client_id": "<<client_id>>",
            "client_secret": "<<client_secret>>"
        }
    }
}


def init_logging(file_enabled=False, stdout_enabled=True):
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)
    logging.getLogger('urllib3.connectionpool').setLevel(logging.WARN)
    logging.getLogger('requests.packages.urllib3.connectionpool').setLevel(logging.WARN)

    log_formatter = logging.Formatter(fmt='%(asctime)s | %(name)s | %(processName)s | %(levelname)s | %(message)s',
                                      datefmt='%m/%d/%Y %I:%M:%S %p')

    if file_enabled:
        log_file_name = './UsergridIterator.log'

        rotating_file = logging.handlers.RotatingFileHandler(filename=log_file_name,
                                                             mode='a',
                                                             maxBytes=204857600,
                                                             backupCount=10)
        rotating_file.setFormatter(log_formatter)
        rotating_file.setLevel(logging.INFO)

        root_logger.addHandler(rotating_file)

    if stdout_enabled:
        stdout_logger = logging.StreamHandler(sys.stdout)
        stdout_logger.setFormatter(log_formatter)
        stdout_logger.setLevel(logging.INFO)

        root_logger.addHandler(stdout_logger)


config = {}


class Worker(Process):
    """
    The worker is used to perform a set of handler functions in a chain.  Work is provided for the Worker thread(s) on
    a JoinableQueue.  The thread will continue until either 1) it is explicitly terminated or 2) until it does not
     receive work on the queue after a consecutive number of attempts (max_empty_count) using the specified timeout
     (queue_timeout)
    """

    def __init__(self,
                 queue,
                 source_client,
                 target_client,
                 max_empty_count=3,
                 queue_timeout=10,
                 function_chain=None):
        """
        This is an example handler function which can transform an entity. Multiple handler functions can be used to
        process a entity.  The response is an entity which will get passed to the next handler in the chain

        :param queue: The queue on which to listen for work
        :param source_client: The UsergridClient of the source Usergrid instance
        :param target_client: The UsergridClient of the target Usergrid instance
        :param max_empty_count: The maximum number of times for a worker to not receive work after checking the queue
        :param queue_timeout: The timeout for waiting for work on the queue
        :param function_chain: An array of function pointers which will be executed in array sequence, expeting the following parameters: org_name, app_name, collection_name, entity, source_client, target_client, attempts=0p
        """

        super(Worker, self).__init__()
        logger.warning('Creating worker!')

        if not function_chain:
            function_chain = []

        self.function_chain = function_chain
        self.queue = queue
        self.source_client = source_client
        self.target_client = target_client
        self.max_empty_count = max_empty_count
        self.queue_timeout = queue_timeout

    def run(self):
        logger.info('starting run()...')
        keep_going = True

        count_processed = 0
        count_failed = 0
        empty_count = 0

        while keep_going:

            try:
                org, app, collection_name, entity = self.queue.get(timeout=self.queue_timeout)

                empty_count = 0
                success = True
                entity_param = entity

                for handler in self.function_chain:

                    if entity_param is not None:
                        try:
                            entity_param = handler(org, app, collection_name, entity_param, self.source_client,
                                                   self.target_client)
                        except Exception, e:
                            logger.error(e)
                            print traceback.format_exc()
                            success = False

                if success:
                    count_processed += 1
                    logger.info('Processed [%sth] SUCCESS app/collection/name/uuid = %s / %s / %s / %s' % (
                        count_processed, app, collection_name, entity.get('name'), entity.get('uuid')))
                else:
                    count_failed += 1
                    logger.warning('Processed [%sth] FAILURE app/collection/name/uuid = %s / %s / %s / %s' % (
                        count_processed, app, collection_name, entity.get('name'), entity.get('uuid')))

            except KeyboardInterrupt, e:
                raise e

            except Empty:
                logger.warning(
                    'No task received after timeout=[%s]! Empty Count=%s' % (self.queue_timeout, empty_count))

                empty_count += 1

                if empty_count >= self.max_empty_count:
                    logger.warning('Stopping work after empty_count=[%s]' % empty_count)
                    keep_going = False

        logger.info('Worker finished!')


def filter_entity(org_name, app_name, collection_name, entity_data, source_client, target_client, attempts=0):
    """
    This is an example handler function which can filter entities. Multiple handler functions can be used to
    process a entity.  The response is an entity which will get passed to the next handler in the chain

    :param org_name: The org name from whence this entity came
    :param app_name: The app name from whence this entity came
    :param collection_name: The collection name from whence this entity came
    :param entity: The entity retrieved from the source instance
    :param source_client: The UsergridClient for the source Usergrid instance
    :param target_client: The UsergridClient for the target Usergrid instance
    :param attempts: the number of previous attempts this function was run (manual, not part of the framework)
    :return: an entity.  If response is None then the chain will stop.
    """

    # return None if you want to stop the chain (filter the entity out)
    if 'blah' in entity_data:
        return None

    # return the entity to keep going
    return entity_data


def transform_entity(org_name, app_name, collection_name, entity_data, source_client, target_client, attempts=0):
    """
    This is an example handler function which can transform an entity. Multiple handler functions can be used to
    process a entity.  The response is an entity which will get passed to the next handler in the chain

    :param org_name: The org name from whence this entity came
    :param app_name: The app name from whence this entity came
    :param collection_name: The collection name from whence this entity came
    :param entity: The entity retrieved from the source instance
    :param source_client: The UsergridClient for the source Usergrid instance
    :param target_client: The UsergridClient for the target Usergrid instance
    :param attempts: the number of previous attempts this function was run (manual, not part of the framework)
    :return: an entity.  If response is None then the chain will stop.
    """
    # this just returns the entity with no transform
    return entity_data


def create_new(org_name, app_name, collection_name, entity_data, source_client, target_client, attempts=0):
    """
    This is an example handler function which can be used to create a new entity in the target instance (based on the
    target_client) parameter. Multiple handler functions can be used to process a entity.  The response is an entity
    which will get passed to the next handler in the chain

    :param org_name: The org name from whence this entity came
    :param app_name: The app name from whence this entity came
    :param collection_name: The collection name from whence this entity came
    :param entity_data: The entity retrieved from the source instance
    :param source_client: The UsergridClient for the source Usergrid instance
    :param target_client: The UsergridClient for the target Usergrid instance
    :param attempts: the number of previous attempts this function was run (manual, not part of the framework)
    :return: an entity.  If response is None then the chain will stop.
    """

    attempts += 1

    if 'metadata' in entity_data: entity_data.pop('metadata')

    target_org = config.get('target_org')
    target_app = config.get('app_mapping', {}).get(app_name, app_name)
    target_collection = config.get('collection_mapping', {}).get(collection_name, collection_name)

    if target_client:
        try:
            c = target_client.org(target_org).app(target_app).collection(target_collection)
            e = c.entity_from_data(entity_data)
            e.put()

        except UsergridError as err:
            logger.error(err)
            raise err

    return None


def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid App/Collection Iterator')

    parser.add_argument('-o', '--org',
                        help='Name of the org to migrate',
                        type=str,
                        required=True)

    parser.add_argument('-a', '--app',
                        help='Multiple, name of apps to include, skip to include all',
                        default=[],
                        action='append')

    parser.add_argument('-c', '--collection',
                        help='Multiple, name of collections to include, skip to include all',
                        default=[],
                        action='append')

    parser.add_argument('--ql',
                        help='The Query string for processing the source collection(s)',
                        type=str,
                        default='select *')

    parser.add_argument('-s', '--source_config',
                        help='The configuration of the source endpoint/org',
                        type=str,
                        default='source.json')

    parser.add_argument('-d', '--target_config',
                        help='The configuration of the target endpoint/org',
                        type=str,
                        default='destination.json')

    parser.add_argument('-w', '--workers',
                        help='The number of worker threads',
                        type=int,
                        default=1)

    parser.add_argument('-f', '--force',
                        help='Force an update regardless of modified date',
                        type=bool,
                        default=False)

    parser.add_argument('--max_empty_count',
                        help='The number of iterations for an individual worker to receive no work before stopping',
                        type=int,
                        default=3)

    parser.add_argument('--queue_timeout',
                        help='The duration in seconds for an individual worker queue poll before Empty is raised',
                        type=int,
                        default=10)

    parser.add_argument('--map_app',
                        help="A colon-separated string such as 'apples:oranges' which indicates to put data from the app named 'apples' from the source endpoint into app named 'oranges' in the target endpoint",
                        default=[],
                        action='append')

    parser.add_argument('--map_collection',
                        help="A colon-separated string such as 'cats:dogs' which indicates to put data from collections named 'cats' from the source endpoint into a collection named 'dogs' in the target endpoint, applicable to all apps",
                        default=[],
                        action='append')

    parser.add_argument('--target_org',
                        help="The org name at the Usergrid destination instance",
                        type=str)

    my_args = parser.parse_args(sys.argv[1:])

    return vars(my_args)


def init():
    global config

    config['collection_mapping'] = {}
    config['app_mapping'] = {}
    config['org_mapping'] = {}

    with open(config.get('source_config'), 'r') as f:
        config['source_config'] = json.load(f)

    with open(config.get('target_config'), 'r') as f:
        config['target_config'] = json.load(f)

    for mapping in config.get('map_collection', []):
        parts = mapping.split(':')

        if len(parts) == 2:
            config['collection_mapping'][parts[0]] = parts[1]
        else:
            logger.warning('Skipping malformed Collection mapping: [%s]' % mapping)

    for mapping in config.get('map_app', []):
        parts = mapping.split(':')

        if len(parts) == 2:
            config['app_mapping'][parts[0]] = parts[1]
        else:
            logger.warning('Skipping malformed App mapping: [%s]' % mapping)

    for mapping in config.get('map_org', []):
        parts = mapping.split(':')

        if len(parts) == 2:
            config['org_mapping'][parts[0]] = parts[1]
        else:
            logger.warning('Skipping Org mapping: [%s]' % mapping)

    if 'source_config' in config:
        config['source_endpoint'] = config['source_config'].get('endpoint').copy()
        config['source_endpoint'].update(config['source_config']['credentials'][config['org']])

    config['target_org'] = config['target_org'] if config['target_org'] else config['org']

    if 'target_config' in config:
        config['target_endpoint'] = config['target_config'].get('endpoint').copy()
        config['target_endpoint'].update(config['target_config']['credentials'][config['target_org']])



class UsergridIterator:
    def __init__(self):
        pass

    def get_to_work(self):
        global config

        queue = Queue()
        logger.warning('Starting workers...')

        apps_to_process = config.get('app')
        collections_to_process = config.get('collection')
        source_org = config['org']
        target_org = config.get('target_org', config.get('org'))

        source_client = None
        target_client = None

        try:
            source_client = UsergridClient(api_url=config['source_endpoint']['api_url'],
                                           org_name=source_org)
            source_client.authenticate_management_client(
                client_credentials=config['source_config']['credentials'][source_org])

        except UsergridError, e:
            logger.critical(e)
            exit()

        if 'target_endpoint' in config:
            try:
                target_client = UsergridClient(api_url=config['target_endpoint']['api_url'],
                                               org_name=target_org)
                target_client.authenticate_management_client(
                    client_credentials=config['target_config']['credentials'][target_org])

            except UsergridError, e:
                logger.critical(e)
                exit()

        function_chain = [filter_entity, transform_entity, create_new]

        workers = [Worker(queue=queue,
                          source_client=source_client,
                          target_client=target_client,
                          function_chain=function_chain,
                          max_empty_count=config.get('max_empty_count', 3),
                          queue_timeout=config.get('queue_timeout', 10))

                   for x in xrange(config.get('workers', 1))]

        [w.start() for w in workers]

        for app in source_client.list_apps():

            if len(apps_to_process) > 0 and app not in apps_to_process:
                logger.warning('Skipping app=[%s]' % app)
                continue

            logger.warning('Processing app=[%s]' % app)

            source_app = source_client.organization(source_org).application(app)

            for collection_name, collection in source_app.list_collections().iteritems():

                if collection_name in ['events', 'queues']:
                    logger.warning('Skipping internal collection=[%s]' % collection_name)
                    continue

                if len(collections_to_process) > 0 and collection_name not in collections_to_process:
                    logger.warning('Skipping collection=[%s]' % collection_name)
                    continue

                logger.warning('Processing collection=%s' % collection_name)

                counter = 0

                try:
                    for entity in collection.query(ql=config.get('ql'),
                                                   limit=config.get('source_endpoint', {}).get('limit', 100)):
                        counter += 1
                        queue.put((config.get('org'), app, collection_name, entity))

                except KeyboardInterrupt:
                    [w.terminate() for w in workers]

            logger.info('Publishing entities complete!')

        # allow workers to finish using join, not wait_for
        [w.join() for w in workers]

    logger.info('All done!!')


def main():
    global config
    config = parse_args()
    init()

    init_logging()

    UsergridIterator().get_to_work()


if __name__ == '__main__':
    main()
