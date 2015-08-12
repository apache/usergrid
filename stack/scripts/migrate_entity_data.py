# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# Usage from a machine running Usergrid:
#
# python migrate_entity_data.py -u adminuser:adminpass    (standard data migration and reindex)
# python migrate_entity_data.py -u adminuser:adminpass -f    (force a re-migration )
# python migrate_entity_data.py -u adminuser:adminpass -d <timestamp>    (re-index only from the timestamp specified)
#
#


import sys
import logging
from logging.handlers import RotatingFileHandler
import argparse
import time
import requests
import json


# Version expected in status response post-migration for entity and app-info data
TARGET_VERSION = 2

# Set an interval (in seconds) for checking if re-index and/or migration has finished
STATUS_INTERVAL_SECONDS = 2


def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Migration Tool')

    parser.add_argument('-d', '--date',
                        help='A date from which to start the migration',
                        type=str)

    parser.add_argument('-e', '--endpoint',
                        help='The endpoint to use for making API requests.',
                        type=str,
                        default='http://localhost:8080')

    parser.add_argument('-u', '--user',
                        help='System Admin Credentials used to authenticate with Usergrid  <user:pass>',
                        type=str,
                        required=True)

    parser.add_argument('-f', '--force',
                        help='Force a delta migration.',
                        action='store_true',
                        default=False)

    my_args = parser.parse_args(sys.argv[1:])

    arg_vars = vars(my_args)
    creds = arg_vars['user'].split(':')
    if len(creds) != 2:
        print('Credentials not properly specified.  Must be "-u <user:pass>". Exiting...')
        exit_on_error()
    else:
        arg_vars['user'] = creds[0]
        arg_vars['pass'] = creds[1]

    return arg_vars



class Migrate:
    def __init__(self):
        self.args = parse_args()
        self.start_date = self.args['date']
        self.endpoint = self.args['endpoint']
        self.metrics = {'reindex_start': '',
                        'reindex_end': '',
                        'appinfo_migration_start': '',
                        'appinfo_migration_end': '',
                        'full_data_migration_start': '',
                        'full_data_migration_end': ''}
        self.logger = init_logging(self.__class__.__name__)
        self.admin_user = self.args['user']
        self.admin_pass = self.args['pass']
        self.force_migration = self.args['force']

    def run(self):
        self.logger.info('Initializing...')

        if not self.is_endpoint_available():
            exit_on_error('Endpoint is not available, aborting')

        if self.start_date is not None:
            self.logger.info("Date Provided.  Re-index will run from date=[%s]", self.start_date)

        try:

            # Always run an app info migration first
            if self.is_appinfo_migrated():
                self.logger.info('AppInfo already migrated. Resetting version for re-migration.')
                self.reset_appinfo_migration()
                time.sleep(STATUS_INTERVAL_SECONDS)

            self.start_appinfo_migration()
            self.logger.info('AppInfo Migration Started.')
            self.metrics['appinfo_migration_start'] = get_current_time()

            is_appinfo_migrated = False
            while not is_appinfo_migrated:
                is_appinfo_migrated = self.is_appinfo_migrated()
                time.sleep(STATUS_INTERVAL_SECONDS)
                if is_appinfo_migrated:
                    self.metrics['appinfo_migration_end'] = get_current_time()
                    break
            self.logger.info('AppInfo Migration Ended.')

            # Perform system re-index (it will grab date from input if provided)
            job = self.start_reindex()
            self.metrics['reindex_start'] = get_current_time()
            self.logger.info('Started Re-index.  Job=[%s]', job)
            is_running = True
            while is_running:
                time.sleep(STATUS_INTERVAL_SECONDS)
                is_running = self.is_reindex_running(job)
                if not is_running:
                    break

            self.logger.info("Finished Re-index. Job=[%s]", job)
            self.metrics['reindex_end'] = get_current_time()

            # Only when we do a delta (force migration) do we run the full data migration (includes entity data)
            if self.force_migration:

                self.logger.info('Force option provided. Performing full data migration...')
                if self.is_data_migrated():
                    self.reset_data_migration()
                time.sleep(STATUS_INTERVAL_SECONDS)
                self.is_data_migrated()

                self.start_fulldata_migration()
                self.metrics['full_data_migration_start'] = get_current_time()
                self.logger.info("Full Data Migration Started")
                is_migrated = False
                while not is_migrated:
                    time.sleep(STATUS_INTERVAL_SECONDS)
                    is_migrated = self.is_data_migrated()
                    if is_migrated:
                        break

                self.metrics['full_data_migration_end'] = get_current_time()
                self.logger.info("Full Data Migration completed")

            self.log_metrics()
            self.logger.info("Finished...")

        except KeyboardInterrupt:
            self.log_metrics()
            self.logger.error('Keyboard interrupted migration. Please run again to ensure the migration finished.')

    def get_migration_url(self):
        url = self.endpoint + '/system/migrate/run'
        return url

    def get_reset_migration_url(self):
        url = self.endpoint + '/system/migrate/set'
        return url

    def get_migration_status_url(self):
        url = self.endpoint + '/system/migrate/status'
        return url

    def get_reindex_url(self):
        url = self.endpoint + '/system/index/rebuild'
        return url

    def start_fulldata_migration(self):
        try:
            r = requests.put(url=self.get_migration_url(), auth=(self.admin_user, self.admin_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def start_appinfo_migration(self):
        try:
            migrateUrl = self.get_migration_url() + '/' + 'appinfo-migration'
            r = requests.put(url=migrateUrl, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def reset_data_migration(self):
        version = TARGET_VERSION - 1
        body = json.dumps({'collections-entity-data': version, 'appinfo-migration': version})
        try:
            r = requests.put(url=self.get_reset_migration_url(), data=body, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            self.logger.info('Resetting data migration versions to collections-entity-data=[v%s] '
                             'and appinfo-migration=[v%s]', version, version)
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to reset full data migration versions, %s', e)
            exit_on_error(str(e))

    def reset_appinfo_migration(self):
        version = TARGET_VERSION - 1
        body = json.dumps({'appinfo-migration': version})
        try:
            r = requests.put(url=self.get_reset_migration_url(), data=body, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            self.logger.info('Resetting appinfo migration versions to appinfo-migration=[v%s]', version)
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to reset appinfo migration version, %s', e)
            exit_on_error(str(e))

    def is_data_migrated(self):
        status = self.check_data_migration_status()
        if status is not None:
            entity_version = status['data']['collections-entity-data']
            appinfo_version = status['data']['appinfo-migration']

            if entity_version == TARGET_VERSION and appinfo_version == TARGET_VERSION:
                self.logger.info('Full Data Migration status=[COMPLETE], collections-entity-data=[v%s], '
                                 'appinfo-migration=[v%s]',
                                 entity_version,
                                 appinfo_version)
                return True
            else:
                self.logger.info('Full Data Migration status=[NOTSTARTED/INPROGRESS]')
        return False

    def is_appinfo_migrated(self):
        status = self.check_data_migration_status()
        if status is not None:
            appinfo_version = status['data']['appinfo-migration']

            if appinfo_version == TARGET_VERSION:
                self.logger.info('AppInfo Migration status=[COMPLETE],'
                                 'appinfo-migration=[v%s]',
                                 appinfo_version)
                return True
            else:
                self.logger.info('AppInfo Migration status=[NOTSTARTED/INPROGRESS]')
        return False

    def check_data_migration_status(self):

        try:
            r = requests.get(url=self.get_migration_status_url(), auth=(self.admin_user, self.admin_pass))
            if r.status_code == 200:
                response = r.json()
                return response
            else:
                self.logger.error('Failed to check migration status, %s', r)
                return
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to check migration status, %s', e)
            # exit_on_error()

    def get_reindex_status(self, job):
        status_url = self.get_reindex_url()+'/' + job

        try:
            r = requests.get(url=status_url, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            return response['status']
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to get reindex status, %s', e)
            # exit_on_error()

    def start_reindex(self):
        body = ""
        if self.start_date is not None:
            body = json.dumps({'updated': self.start_date})

        try:
            r = requests.post(url=self.get_reindex_url(), data=body, auth=(self.admin_user, self.admin_pass))

            if r.status_code == 200:
                response = r.json()
                return response['jobId']
            else:
                self.logger.error('Failed to start reindex, %s', r)
                exit_on_error(str(r))
        except requests.exceptions.RequestException as e:
            self.logger.error('Unable to make API request for reindex, %s', e)
            exit_on_error(str(e))

    def is_reindex_running(self, job):
        status = self.get_reindex_status(job)
        self.logger.info('Re-index status=[%s]', status)
        if status != "COMPLETE":
            return True
        else:
            return False

    def is_endpoint_available(self):

        try:
            r = requests.get(url=self.endpoint+'/status')
            if r.status_code == 200:
                return True
        except requests.exceptions.RequestException as e:
            self.logger.error('Endpoint is unavailable, %s', str(e))
            return False

    def log_metrics(self):
        self.logger.info(
            'Re-index start=[%s], ' +
            'Re-index end =[%s], ' +
            'Full Data Migration start=[%s], ' +
            'Full Data Migration end=[%s] ' +
            'AppInfo Migration start=[%s], ' +
            'AppInfo Migration end=[%s] ',
            self.metrics['reindex_start'],
            self.metrics['reindex_end'],
            self.metrics['full_data_migration_start'],
            self.metrics['full_data_migration_end'],
            self.metrics['appinfo_migration_start'],
            self.metrics['appinfo_migration_end']

        )


def get_current_time():
    return str(int(time.time()*1000))


def exit_on_error(e=""):
    print ('Exiting migration script due to error: ' + str(e))
    sys.exit(1)


def init_logging(name):

    logger = logging.getLogger(name)
    log_file_name = './migration.log'
    log_formatter = logging.Formatter(fmt='%(asctime)s | %(name)s | %(levelname)s | %(message)s',
                                      datefmt='%m/%d/%Y %I:%M:%S %p')

    rotating_file = logging.handlers.RotatingFileHandler(filename=log_file_name,
                                                         mode='a',
                                                         maxBytes=104857600,
                                                         backupCount=10)
    rotating_file.setFormatter(log_formatter)
    rotating_file.setLevel(logging.INFO)
    logger.addHandler(rotating_file)
    logger.setLevel(logging.INFO)

    stdout_logger = logging.StreamHandler(sys.stdout)
    stdout_logger.setFormatter(log_formatter)
    stdout_logger.setLevel(logging.INFO)
    logger.addHandler(stdout_logger)

    return logger

if __name__ == '__main__':

    migration = Migrate()
    migration.run()
