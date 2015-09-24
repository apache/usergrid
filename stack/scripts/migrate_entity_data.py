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
#
#
# Usage from a machine running Usergrid with the new Usergrid version:
#
# ######################################################
# STEP 1 - BEFORE SWITCHING TRAFFIC TO NEW UG VERSION
# ######################################################
#
# python migrate_entity_data.py --user adminuser:adminpass
#
# The above command performs an appinfo migration and system re-index only.  This creates indices in Elasticsearch with
# the updated indexing strategy in the new Usergrid version.
#
# ######################################################
# STEP 2 - AFTER SWITCHING TRAFFIC TO NEW UG VERSION
# ######################################################
#
# python migrate_entity_data.py --user adminuser:adminpass --delta --date <timestamp>
#
# The above command performs an appinfo migration, system re-index using a start date, and full data migration which
# includes entity data.  This step is necessary to ensure Usergrid starts reading and writing data from the latest
# entity version, including delta indexing of any documents create during the time between STEP 1 and STEP 2.  If
# all data has already been migrated (running this a 2nd, 3rd, etc. time), then the appinfo migration will be skipped.



import sys
import logging
from logging.handlers import RotatingFileHandler
import argparse
import time
import requests
import json


# Version expected in status response post-migration for entity and app-info data
TARGET_APPINFO_VERSION=2
TARGET_ENTITY_DATA_VERSION=2
TARGET_CORE_DATA_VERSION=2
TARGET_MIGRATION_SYSTEM_VERSION = 1
TARGET_INDEX_MAPPING_VERSION = 2

# Set an interval (in seconds) for checking if re-index and/or migration has finished
STATUS_INTERVAL_SECONDS = 2

# Set plugin names
PLUGIN_MIGRATION_SYSTEM = 'migration-system'
PLUGIN_APPINFO = 'appinfo-migration'
PLUGIN_ENTITYDATA = 'collections-entity-data'
PLUGIN_INDEX_MAPPING = 'index_mapping_migration'
PLUGIN_CORE_DATA = 'core-data'



def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Migration Tool')

    parser.add_argument('--date',
                        help='A date from which to start the migration',
                        type=str)

    parser.add_argument('--endpoint',
                        help='The endpoint to use for making API requests.',
                        type=str,
                        default='http://localhost:8080')

    parser.add_argument('--user',
                        help='System Admin Credentials used to authenticate with Usergrid  <user:pass>',
                        type=str,
                        required=True)

    parser.add_argument('--delta',
                        help='Run a delta migration.',
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
        self.delta_migration = self.args['delta']

    def run(self):
        self.logger.info('Initializing...')

        if not self.is_endpoint_available():
            exit_on_error('Endpoint is not available, aborting')

        if self.start_date is not None:
            self.logger.info("Date Provided.  Re-index will run from date=[%s]", self.start_date)

        try:

            self.run_database_setup()

            # We need to check and roll the migration system to 1 if not already
            migration_system_updated = self.is_migration_system_updated()

            if not migration_system_updated:
                self.logger.info('Migration system needs to be updated.  Updating migration system..')
                self.start_migration_system_update()
                while not migration_system_updated:
                    time.sleep(STATUS_INTERVAL_SECONDS)
                    migration_system_updated = self.is_migration_system_updated()
                    if migration_system_updated:
                        break

            index_mapping_updated = self.is_index_mapping_updated()

            if not index_mapping_updated:
                self.logger.info('Index Mapping needs to be updated.  Updating index mapping..')
                self.start_index_mapping_migration()
                while not index_mapping_updated:
                    time.sleep(STATUS_INTERVAL_SECONDS)
                    index_mapping_updated = self.is_index_mapping_updated()
                    if index_mapping_updated:
                        break

            # Run AppInfo migration only when both appinfos and collection entity data have not been migrated
            if not self.is_data_migrated():

                #Migrate app info
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


            else:
                self.logger.info('Full Data Migration previously ran... skipping AppInfo migration.')



            # We need to check and roll index mapping version to 1 if not already there

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

            # Only when we do a delta migration do we run the full data migration (includes appinfo and entity data)
            if self.delta_migration:

                self.logger.info('Delta option provided. Performing full data migration...')
                if self.is_data_migrated():
                    self.reset_data_migration()
                time.sleep(STATUS_INTERVAL_SECONDS)
                self.is_data_migrated()

                # self.start_core_data_migration()
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

    def get_database_setup_url(self):
        url = self.endpoint + '/system/database/setup'
        return url

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

    def get_management_reindex_url(self):
          url = self.get_reindex_url() + "/management"
          return url


    def start_core_data_migration(self):
           try:
               r = requests.put(url=self.get_migration_url(), auth=(self.admin_user, self.admin_pass))
               response = r.json()
               return response
           except requests.exceptions.RequestException as e:
               self.logger.error('Failed to start migration, %s', e)
               exit_on_error(str(e))


    def start_fulldata_migration(self):
        try:
            r = requests.put(url=self.get_migration_url(), auth=(self.admin_user, self.admin_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def start_migration_system_update(self):
        try:
            #TODO fix this URL
            migrateUrl = self.get_migration_url() + '/' + PLUGIN_MIGRATION_SYSTEM
            r = requests.put(url=migrateUrl, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def run_database_setup(self):
        try:
            setupUrl = self.get_database_setup_url()
            r = requests.put(url=setupUrl, auth=(self.admin_user, self.admin_pass))
            if r.status_code != 200:
                exit_on_error('Database Setup Failed')

        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to run database setup, %s', e)
            exit_on_error(str(e))

    def start_index_mapping_migration(self):
        try:
            migrateUrl = self.get_migration_url() + '/' + PLUGIN_INDEX_MAPPING
            r = requests.put(url=migrateUrl, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def start_appinfo_migration(self):
        try:
            migrateUrl = self.get_migration_url() + '/' + PLUGIN_APPINFO
            r = requests.put(url=migrateUrl, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def reset_data_migration(self):
        version = TARGET_ENTITY_DATA_VERSION - 1
        body = json.dumps({PLUGIN_ENTITYDATA: version, PLUGIN_APPINFO: version})
        try:
            r = requests.put(url=self.get_reset_migration_url(), data=body, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            self.logger.info('Resetting data migration versions to %s=[%s] '
                             'and %s=[%s]', PLUGIN_ENTITYDATA, version, PLUGIN_APPINFO, version)
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to reset full data migration versions, %s', e)
            exit_on_error(str(e))

    def reset_appinfo_migration(self):
        version = TARGET_APPINFO_VERSION - 1
        body = json.dumps({PLUGIN_APPINFO: version})
        try:
            r = requests.put(url=self.get_reset_migration_url(), data=body, auth=(self.admin_user, self.admin_pass))
            response = r.json()
            self.logger.info('Resetting appinfo migration versions to %s=[%s]', PLUGIN_APPINFO, version)
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to reset appinfo migration version, %s', e)
            exit_on_error(str(e))

    def is_data_migrated(self):
        status = self.check_data_migration_status()
        if status is not None:
            entity_version = status['data'][PLUGIN_ENTITYDATA]
            appinfo_version = status['data'][PLUGIN_APPINFO]
            core_data_version = status['data'][PLUGIN_CORE_DATA]

            if entity_version == TARGET_ENTITY_DATA_VERSION and appinfo_version == TARGET_APPINFO_VERSION and core_data_version == TARGET_CORE_DATA_VERSION:
                self.logger.info('Full Data Migration status=[COMPLETE], %s=[%s], '
                                 '%s=[%s], %s=%s',
                                 PLUGIN_ENTITYDATA,
                                 entity_version,
                                 PLUGIN_APPINFO,
                                 appinfo_version,
                                 PLUGIN_CORE_DATA,
                                 core_data_version)
                return True
            else:
                self.logger.info('Full Data Migration status=[NOTSTARTED/INPROGRESS]')
        return False

    def is_appinfo_migrated(self):
        status = self.check_data_migration_status()
        if status is not None:
            appinfo_version = status['data'][PLUGIN_APPINFO]

            if appinfo_version == TARGET_APPINFO_VERSION:
                self.logger.info('AppInfo Migration status=[COMPLETE],'
                                 '%s=[%s]',
                                 PLUGIN_APPINFO,
                                 appinfo_version)
                return True
            else:
                self.logger.info('AppInfo Migration status=[NOTSTARTED/INPROGRESS]')
        return False

    def is_migration_system_updated(self):
        status = self.check_data_migration_status()
        if status is not None:
            migration_system_version = status['data'][PLUGIN_MIGRATION_SYSTEM]

            if migration_system_version == TARGET_MIGRATION_SYSTEM_VERSION:
                self.logger.info('Migration System CURRENT, %s=[%s]',
                                 PLUGIN_MIGRATION_SYSTEM,
                                 migration_system_version)
                return True
            else:
                self.logger.info('Migration System OLD, %s=[%s]',
                                 PLUGIN_MIGRATION_SYSTEM,
                                 migration_system_version)
        return False

    def is_index_mapping_updated(self):
        status = self.check_data_migration_status()
        if status is not None:
            index_mapping_version = status['data'][PLUGIN_INDEX_MAPPING]

            if index_mapping_version == TARGET_INDEX_MAPPING_VERSION:
                self.logger.info('Index Mapping CURRENT, %s=[%s]',
                                 PLUGIN_INDEX_MAPPING,
                                 index_mapping_version)
                return True
            else:
                self.logger.info('Index Mapping OLD, %s=[%s]',
                                 PLUGIN_INDEX_MAPPING,
                                 index_mapping_version)
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
    log_formatter = logging.Formatter(fmt='%(asctime)s [%(name)s] %(levelname)s %(message)s',
                                      datefmt='%Y-%m-%d %H:%M:%S')

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
