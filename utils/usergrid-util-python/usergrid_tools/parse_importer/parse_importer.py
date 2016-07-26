import json
import logging
from logging.handlers import RotatingFileHandler
import os
from os import listdir
import zipfile
from os.path import isfile
import sys
import argparse
import traceback

from usergrid import Usergrid
from usergrid.UsergridClient import UsergridEntity

__author__ = 'Jeff West @ ApigeeCorporation'

logger = logging.getLogger('UsergridParseImporter')

parse_id_to_uuid_map = {}
global_connections = {}
config = {}


def init_logging(stdout_enabled=True):
    root_logger = logging.getLogger()
    log_file_name = './usergrid_parse_importer.log'
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


def convert_parse_entity(collection, parse_entity):
    parse_entity['type'] = collection

    if 'name' not in parse_entity and collection.lower() != 'users':
        parse_entity['name'] = parse_entity['objectId']

    connections = {}

    for name, value in parse_entity.iteritems():
        if isinstance(value, dict):
            if value.get('__type') == 'Pointer':
                class_name = value.get('className') if value.get('className')[0] != '_' else value.get('className')[1:]
                connections[value.get('objectId')] = class_name

                logger.info('Connection found from [%s: %s] to entity [%s: %s]' % (
                    collection, parse_entity['name'], class_name, value.get('objectId')))

    return UsergridEntity(parse_entity), connections


def build_usergrid_entity(collection, entity_uuid, data=None):
    identifier = {'type': collection, 'uuid': entity_uuid}
    data = {} if data is None else data
    data.update(identifier)
    return UsergridEntity(data)


def load_users_and_roles(working_directory):
    with open(os.path.join(working_directory, '_User.json'), 'r') as f:
        users = json.load(f).get('results', [])
        logger.info('Loaded [%s] Users' % len(users))

    for i, parse_user in enumerate(users):
        logger.info('Loading user [%s]: [%s / %s]' % (i, parse_user['username'], parse_user['objectId']))
        usergrid_user, connections = convert_parse_entity('users', parse_user)
        res = usergrid_user.save()

        if res.ok:
            logger.info('Saved user [%s]: [%s / %s]' % (i, parse_user['username'], parse_user['objectId']))

            if 'uuid' in usergrid_user.entity_data:
                parse_id_to_uuid_map[parse_user['objectId']] = usergrid_user.get('uuid')
        else:
            logger.error(
                    'Error saving user [%s]: [%s / %s] - %s' % (i, parse_user['username'], parse_user['objectId'], res))

    with open(os.path.join(working_directory, '_Role.json'), 'r') as f:
        roles = json.load(f).get('results', [])
        logger.info('Loaded [%s] Roles' % len(roles))

    for i, parse_role in enumerate(roles):
        logger.info('Loading role [%s]: [%s / %s]' % (i, parse_role['name'], parse_role['objectId']))
        usergrid_role, connections = convert_parse_entity('roles', parse_role)
        res = usergrid_role.save()

        if res.ok:
            logger.info('Saved role [%s]: [%s / %s]' % (i, parse_role['name'], parse_role['objectId']))

            if 'uuid' in usergrid_role.entity_data:
                parse_id_to_uuid_map[parse_role['objectId']] = usergrid_role.get('uuid')

        else:
            logger.error(
                    'Error saving role [%s]: [%s / %s] - %s' % (i, parse_role['name'], parse_role['objectId'], res))

    join_file = os.path.join(working_directory, '_Join:users:_Role.json')

    if os.path.isfile(join_file) and os.path.getsize(join_file) > 0:
        with open(join_file, 'r') as f:
            users_to_roles = json.load(f).get('results', [])
            logger.info('Loaded [%s] User->Roles' % len(users_to_roles))

            for user_to_role in users_to_roles:
                role_id = user_to_role['owningId']
                role_uuid = parse_id_to_uuid_map.get(role_id)

                target_role_id = user_to_role['relatedId']
                target_role_uuid = parse_id_to_uuid_map.get(target_role_id)

                if role_uuid is None or target_role_uuid is None:
                    logger.error('Failed on assigning role [%s] to user [%s]' % (role_uuid, target_role_uuid))
                    continue

                target_role_entity = build_usergrid_entity('user', target_role_uuid)

                res = Usergrid.assign_role(role_uuid, target_role_entity)

                if res.ok:
                    logger.info('Assigned role [%s] to user [%s]' % (role_uuid, target_role_uuid))
                else:
                    logger.error('Failed on assigning role [%s] to user [%s]' % (role_uuid, target_role_uuid))

    else:
        logger.info('No Users -> Roles to load')

    join_file = os.path.join(working_directory, '_Join:roles:_Role.json')

    if os.path.isfile(join_file) and os.path.getsize(join_file) > 0:
        with open(join_file, 'r') as f:
            users_to_roles = json.load(f).get('results', [])
            logger.info('Loaded [%s] Roles->Roles' % len(users_to_roles))

            for user_to_role in users_to_roles:
                role_id = user_to_role['owningId']
                role_uuid = parse_id_to_uuid_map.get(role_id)

                target_role_id = user_to_role['relatedId']
                target_role_uuid = parse_id_to_uuid_map.get(target_role_id)

                if role_uuid is None or target_role_uuid is None:
                    logger.error('Failed on assigning role [%s] to role [%s]' % (role_uuid, target_role_uuid))
                    continue

                target_role_entity = build_usergrid_entity('role', target_role_uuid)

                res = Usergrid.assign_role(role_uuid, target_role_entity)

                if res.ok:
                    logger.info('Assigned role [%s] to role [%s]' % (role_uuid, target_role_uuid))
                else:
                    logger.error('Failed on assigning role [%s] to role [%s]' % (role_uuid, target_role_uuid))

    else:
        logger.info('No Roles -> Roles to load')


def process_join_file(working_directory, join_file):
    file_path = os.path.join(working_directory, join_file)

    logger.warn('Processing Join file: %s' % file_path)

    parts = join_file.split(':')

    if len(parts) != 3:
        logger.warn('Did not find expected 3 parts in JOIN filename: %s' % join_file)
        return

    related_type = parts[1]
    owning_type = parts[2].split('.')[0]

    owning_type = owning_type[1:] if owning_type[0] == '_' else owning_type

    with open(file_path, 'r') as f:
        try:
            json_data = json.load(f)

        except ValueError, e:
            print traceback.format_exc(e)
            logger.error('Unable to process file: %s' % file_path)
            return

        entities = json_data.get('results')

        for join in entities:
            owning_id = join.get('owningId')
            related_id = join.get('relatedId')

            owning_entity = build_usergrid_entity(owning_type, parse_id_to_uuid_map.get(owning_id))
            related_entity = build_usergrid_entity(related_type, parse_id_to_uuid_map.get(related_id))

            connect_entities(owning_entity, related_entity, 'joins')
            connect_entities(related_entity, owning_entity, 'joins')


def load_entities(working_directory):
    files = [
        f for f in listdir(working_directory)

        if isfile(os.path.join(working_directory, f))
        and os.path.getsize(os.path.join(working_directory, f)) > 0
        and f not in ['_Join:roles:_Role.json',
                      '_Join:users:_Role.json',
                      '_User.json',
                      '_Product.json',
                      '_Installation.json',
                      '_Role.json']
        ]

    # sort to put join files last...
    for data_file in sorted(files):
        if data_file[0:6] == '_Join:':
            process_join_file(working_directory, data_file)
            continue

        file_path = os.path.join(working_directory, data_file)
        collection = data_file.split('.')[0]

        if collection[0] == '_':
            logger.warn('Found internal type: [%s]' % collection)
            collection = collection[1:]

        if collection not in global_connections:
            global_connections[collection] = {}

        with open(file_path, 'r') as f:

            try:
                json_data = json.load(f)

            except ValueError, e:
                print traceback.format_exc(e)
                logger.error('Unable to process file: %s' % file_path)
                continue

            entities = json_data.get('results')

            logger.info('Found [%s] entities of type [%s]' % (len(entities), collection))

            for parse_entity in entities:
                usergrid_entity, connections = convert_parse_entity(collection, parse_entity)
                response = usergrid_entity.save()

                global_connections[collection][usergrid_entity.get('uuid')] = connections

                if response.ok:
                    logger.info('Saved Entity: %s' % parse_entity)
                else:
                    logger.info('Error saving entity %s: %s' % (parse_entity, response))


def connect_entities(from_entity, to_entity, connection_name):
    connect_response = from_entity.connect(connection_name, to_entity)

    if connect_response.ok:
        logger.info('Successfully connected [%s / %s]--[%s]-->[%s / %s]' % (
            from_entity.get('type'), from_entity.get('uuid'), connection_name, to_entity.get('type'),
            to_entity.get('uuid')))
    else:
        logger.error('Unable to connect [%s / %s]--[%s]-->[%s / %s]: %s' % (
            from_entity.get('type'), from_entity.get('uuid'), connection_name, to_entity.get('type'),
            to_entity.get('uuid'), connect_response))


def create_connections():
    for from_collection, entity_map in global_connections.iteritems():

        for from_entity_uuid, entity_connections in entity_map.iteritems():
            from_entity = build_usergrid_entity(from_collection, from_entity_uuid)

            for to_entity_id, to_entity_collection in entity_connections.iteritems():
                to_entity = build_usergrid_entity(to_entity_collection, parse_id_to_uuid_map.get(to_entity_id))

                connect_entities(from_entity, to_entity, 'pointers')
                connect_entities(to_entity, from_entity, 'pointers')


def parse_args():
    parser = argparse.ArgumentParser(description='Parse.com Data Importer for Usergrid')

    parser.add_argument('-o', '--org',
                        help='Name of the Usergrid Org to import data into - must already exist',
                        type=str,
                        required=True)

    parser.add_argument('-a', '--app',
                        help='Name of the Usergrid Application to import data into - must already exist',
                        type=str,
                        required=True)

    parser.add_argument('--url',
                        help='The URL of the Usergrid Instance',
                        type=str,
                        required=True)

    parser.add_argument('-f', '--file',
                        help='Full or relative path of the data file to import',
                        required=True,
                        type=str)

    parser.add_argument('--tmp_dir',
                        help='Directory where data file will be unzipped',
                        required=True,
                        type=str)

    parser.add_argument('--client_id',
                        help='The Client ID for using OAuth Tokens - necessary if app is secured',
                        required=False,
                        type=str)

    parser.add_argument('--client_secret',
                        help='The Client Secret for using OAuth Tokens - necessary if app is secured',
                        required=False,
                        type=str)

    my_args = parser.parse_args(sys.argv[1:])

    return vars(my_args)


def main():
    global config
    config = parse_args()

    init_logging()

    Usergrid.init(org_id=config.get('org'),
                  app_id=config.get('app'),
                  base_url=config.get('url'),
                  client_id=config.get('client_id'),
                  client_secret=config.get('client_secret'))

    tmp_dir = config.get('tmp_dir')
    file_path = config.get('file')

    if not os.path.isfile(file_path):
        logger.critical('File path specified [%s] is not a file!' % file_path)
        logger.critical('Unable to continue')
        exit(1)

    if not os.path.isdir(tmp_dir):
        logger.critical('Temp Directory path specified [%s] is not a directory!' % tmp_dir)
        logger.critical('Unable to continue')
        exit(1)

    file_name = os.path.basename(file_path).split('.')[0]
    working_directory = os.path.join(tmp_dir, file_name)

    try:
        with zipfile.ZipFile(file_path, 'r') as z:
            logger.warn('Extracting files to directory: %s' % working_directory)
            z.extractall(working_directory)
            logger.warn('Extraction complete')

    except Exception, e:
        logger.critical(traceback.format_exc(e))
        logger.critical('Extraction failed')
        logger.critical('Unable to continue')
        exit(1)

    load_users_and_roles(working_directory)
    load_entities(working_directory)
    create_connections()


if __name__ == '__main__':
    main()
