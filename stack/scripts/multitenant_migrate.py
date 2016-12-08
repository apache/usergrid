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
# To migrate multiple tenants within one cluster.
#
# STEP 1 - SETUP TENANT ONE TOMCAT RUNNING 2.1 NOT IN SERVICE AND INIT MIGRATION
#
#   Login to the Tomcat instance and run this command, specifying both superuser and tenant organization:
#
#       python multitenant_migrate.py --org <org1name> --super <user>:<pass> --init
#
#   This command will setup the database, setup the migration system and update index mappings:
#   - /system/database/setup
#   - /system/migrate/run/migration-system
#   - /system/migrate/run/index_mapping_migration
#
#   Then it will migrate appinfos, re-index the management app and then for each of the specified org's apps
#   it will de-dup connections and re-index the app.
#
#   Write down the 'Re-index start' timestamp when this is finished.
#
# STEP 2 - PUT TENANT ONE TOMCATS IN SERVICE AND DO DELTA MIGRATION
#
#   On the same Tomcat instance and run this command with the --date timestamp you noted in the previous step:
#
#       python multitenant_migrate.py --org <org1name> --super <user>:<pass> --date <timestamp>
#
#   Then it will migrate appinfos, re-index the management app and then for each of the specified org's apps
#   it will de-dup connections and re-index the app with a start-date specified so only data modified since
#   STEP 1 will be re-indexed.
#
# STEP 3 - SETUP TENANT TWO TOMCAT RUNNING 2.1 NOT IN SERVICE
#
#   Login to the Tomcat instance and run this command, specifying both superuser and tenant organization:
#
#       python multitenant_migrate.py --org <org2name> --super <user>:<pass>
#
#   This command will migrate appinfos, re-index the management app and then for each of the specified org's apps
#   it will de-dup connections and re-index the app.
#
#   Write down the 'Re-index start' timestamp when this is finished.

# STEP 4 - PUT TENANT TWO TOMCATS IN SERVICE AND DO DELTA MIGRATION
#
#   On the same Tomcat instance and run this command with the --date timestamp you noted in the previous step:
#
#       python multitenant_migrate.py --org <org2name> --super <user>:<pass> --date <timestamp>
#
#   Then it will migrate appinfos, re-index the management app and then for each of the specified org's apps
#   it will de-dup connections and re-index the app with a start-date specified so only data modified since
#   STEP 1 will be re-indexed.
#
# STEP 5 - FULL DATA MIGRATION (migrates entity data to new format)
#
#   Login to any Tomcat instance in the cluster and run this command:
#
#       python migrate_entity_data.py --super <user>:<pass> --full
#
#   This command will run the full data migration.
#

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

MANAGEMENT_APP_ID = 'b6768a08-b5d5-11e3-a495-11ddb1de66c8'



def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Migration Tool')

    parser.add_argument('--endpoint',
                        help='The endpoint to use for making API requests.',
                        type=str,
                        default='http://localhost:8080')

    parser.add_argument('--super',
                        help='Superuser username and creds <user:pass>',
                        type=str,
                        required=True)

    parser.add_argument('--init',
                        help='Init system and start first migration.',
                        action='store_true',
                        default=False)

    parser.add_argument('--org',
                        help='Name of organization on which to run migration.',
                        type=str,
                        required=False)

    parser.add_argument('--date',
                        help='A date from which to start the migration',
                        type=str)

    parser.add_argument('--full',
                        help='Run full data migration (last step in cluster migration).',
                        action='store_true',
                        default=False)

    my_args = parser.parse_args(sys.argv[1:])

    arg_vars = vars(my_args)

    creds = arg_vars['super'].split(':')
    if len(creds) != 2:
        print('Superuser credentials not properly specified.  Must be "-u <user:pass>". Exiting...')
        exit_on_error()
    else:
        arg_vars['superuser'] = creds[0]
        arg_vars['superpass'] = creds[1]

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
        self.super_user = self.args['superuser']
        self.super_pass = self.args['superpass']
        self.org = self.args['org']
        self.init = self.args['init']
        self.full = self.args['full']

    def run(self):
        self.logger.info('Initializing...')

        if not self.is_endpoint_available():
            exit_on_error('Endpoint is not available, aborting')

        if self.start_date is not None:
            self.logger.info("Date Provided.  Re-index will run from date=[%s]", self.start_date)

        try:

            if self.full:

                # Do full data migration and exit

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

                return

            if self.init:

                # Init the migration system as this is the first migration done on the cluster
                self.run_database_setup()

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

            # Migrate app info
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

            # De-dup management app
            job = self.start_dedup(MANAGEMENT_APP_ID)
            self.logger.info('Started management dedup.  App=[%s], Job=[%s]', MANAGEMENT_APP_ID, job)
            is_running = True
            while is_running:
                time.sleep(STATUS_INTERVAL_SECONDS)
                is_running = self.is_dedup_running(job)
                if not is_running:
                    break

            self.logger.info("Finished dedup. App=[%s], Job=[%s]", MANAGEMENT_APP_ID, job)
            self.metrics['dedup_end_' + MANAGEMENT_APP_ID] = get_current_time()

            # Reindex management app

            job = self.start_app_reindex(MANAGEMENT_APP_ID)
            self.metrics['reindex_start'] = get_current_time()
            self.logger.info('Started management Re-index.  Job=[%s]', job)
            is_running = True
            while is_running:
                time.sleep(STATUS_INTERVAL_SECONDS)
                is_running = self.is_reindex_running(job)
                if not is_running:
                    break

            self.logger.info("Finished management Re-index. Job=[%s]", job)
            self.metrics['reindex_end'] = get_current_time()

            # Dedup and re-index all of organization's apps

            app_ids = self.get_app_ids()
            for app_id in app_ids:

                # De-dup app
                job = self.start_dedup(app_id)
                self.logger.info('Started dedup.  App=[%s], Job=[%s]', app_id, job)
                is_running = True
                while is_running:
                    time.sleep(STATUS_INTERVAL_SECONDS)
                    is_running = self.is_dedup_running(job)
                    if not is_running:
                        break

                self.logger.info("Finished dedup. App=[%s], Job=[%s]", app_id, job)
                self.metrics['dedup_end_' + app_id] = get_current_time()

                # Re-index app
                job = self.start_app_reindex(app_id)
                self.metrics['reindex_start_' + app_id] = get_current_time()
                self.logger.info('Started Re-index.  App=[%s], Job=[%s]', app_id, job)
                is_running = True
                while is_running:
                    time.sleep(STATUS_INTERVAL_SECONDS)
                    is_running = self.is_reindex_running(job)
                    if not is_running:
                        break

                self.logger.info("Finished Re-index. App=[%s], Job=[%s]", app_id, job)
                self.metrics['reindex_end_' + app_id] = get_current_time()

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

    def get_dedup_url(self):
        url = self.endpoint + '/system/connection/dedup'
        return url

    def get_reindex_url(self):
        url = self.endpoint + '/system/index/rebuild'
        return url

    def get_management_reindex_url(self):
          url = self.get_reindex_url() + "/management"
          return url

    def start_core_data_migration(self):
           try:
               r = requests.put(url=self.get_migration_url(), auth=(self.super_user, self.super_pass))
               response = r.json()
               return response
           except requests.exceptions.RequestException as e:
               self.logger.error('Failed to start migration, %s', e)
               exit_on_error(str(e))

    def start_fulldata_migration(self):
        try:
            r = requests.put(url=self.get_migration_url(), auth=(self.super_user, self.super_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def start_migration_system_update(self):
        try:
            # TODO fix this URL
            migrateUrl = self.get_migration_url() + '/' + PLUGIN_MIGRATION_SYSTEM
            r = requests.put(url=migrateUrl, auth=(self.super_user, self.super_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def run_database_setup(self):
        try:
            setupUrl = self.get_database_setup_url()
            r = requests.put(url=setupUrl, auth=(self.super_user, self.super_pass))
            if r.status_code != 200:
                exit_on_error('Database Setup Failed')

        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to run database setup, %s', e)
            exit_on_error(str(e))

    def start_index_mapping_migration(self):
        try:
            migrateUrl = self.get_migration_url() + '/' + PLUGIN_INDEX_MAPPING
            r = requests.put(url=migrateUrl, auth=(self.super_user, self.super_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def start_appinfo_migration(self):
        try:
            migrateUrl = self.get_migration_url() + '/' + PLUGIN_APPINFO
            r = requests.put(url=migrateUrl, auth=(self.super_user, self.super_pass))
            response = r.json()
            return response
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to start migration, %s', e)
            exit_on_error(str(e))

    def reset_data_migration(self):
        version = TARGET_ENTITY_DATA_VERSION - 1
        body = json.dumps({PLUGIN_ENTITYDATA: version, PLUGIN_APPINFO: version})
        try:
            r = requests.put(url=self.get_reset_migration_url(), data=body, auth=(self.super_user, self.super_pass))
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
            r = requests.put(url=self.get_reset_migration_url(), data=body, auth=(self.super_user, self.super_pass))
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
            r = requests.get(url=self.get_migration_status_url(), auth=(self.super_user, self.super_pass))
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
            r = requests.get(url=status_url, auth=(self.super_user, self.super_pass))
            response = r.json()
            return response['status']
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to get reindex status, %s', e)
            # exit_on_error()

    def start_app_reindex(self, appId):
        body = ""
        if self.start_date is not None:
            body = json.dumps({'updated': self.start_date})

        try:
            r = requests.post(url=self.get_reindex_url() + "/" + appId, data=body, auth=(self.super_user, self.super_pass))

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

    def get_dedup_status(self, job):
        status_url = self.get_dedup_url()+'/' + job
        try:
            r = requests.get(url=status_url, auth=(self.super_user, self.super_pass))
            response = r.json()
            return response['status']['status']
        except requests.exceptions.RequestException as e:
            self.logger.error('Failed to get dedup status, %s', e)
            # exit_on_error()

    def start_dedup(self, app_id):
        body = ""
        try:
            r = requests.post(url=self.get_dedup_url() + "/" + app_id, data=body, auth=(self.super_user, self.super_pass))
            if r.status_code == 200:
                response = r.json()
                return response['status']['jobStatusId']
            else:
                self.logger.error('Failed to start dedup, %s', r)
                exit_on_error(str(r))

        except requests.exceptions.RequestException as e:
            self.logger.error('Unable to make API request for dedup, %s', e)
            exit_on_error(str(e))

    def is_dedup_running(self, job):
        status = self.get_dedup_status(job)
        self.logger.info('Dedup status=[%s]', status)
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

    def get_app_ids(self):

        try:

            url = self.endpoint + "/management/organizations"
            r = requests.get(url=url, auth=(self.super_user, self.super_pass))

            if r.status_code != 200:
                exit_on_error('Cannot get app ids: ' + r.text)

            response_json = r.json()

            app_ids = []
            orgs = response_json["organizations"]
            if orgs is not None:
                for org in orgs:
                    if org["name"] == self.org:
                        for app in org["applications"]:
                            app_ids.append(org["applications"][app])
            else:
                e = 'No Orgs in this system'
                self.logger.error(e)
                exit_on_error(e)

            return app_ids

        except requests.exceptions.RequestException as e:
            self.logger.error('Unable to get list of application ids, %s', e)
            exit_on_error(str(e))

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
