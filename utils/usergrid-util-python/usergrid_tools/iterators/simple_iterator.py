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

import logging
import sys
import uuid
from logging.handlers import RotatingFileHandler

import datetime
from usergrid import UsergridQueryIterator

__author__ = 'Jeff.West@yahoo.com'


execution_id = str(uuid.uuid4())


def init_logging(stdout_enabled=True):
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    logging.getLogger('requests.packages.urllib3.connectionpool').setLevel(logging.ERROR)
    logging.getLogger('boto').setLevel(logging.ERROR)
    logging.getLogger('urllib3.connectionpool').setLevel(logging.WARN)

    log_formatter = logging.Formatter(
            fmt='%(asctime)s | ' + execution_id + ' | %(name)s | %(levelname)s | %(message)s',
            datefmt='%m/%d/%Y %I:%M:%S %p')

    stdout_logger = logging.StreamHandler(sys.stdout)
    stdout_logger.setFormatter(log_formatter)
    stdout_logger.setLevel(logging.CRITICAL)
    root_logger.addHandler(stdout_logger)

    if stdout_enabled:
        stdout_logger.setLevel(logging.INFO)

    # base log file

    log_dir = './'
    log_file_name = '%s/usergrid_iterator.log' % log_dir

    # ConcurrentLogHandler
    rotating_file = RotatingFileHandler(filename=log_file_name,
                                        mode='a',
                                        maxBytes=404857600,
                                        backupCount=0)
    rotating_file.setFormatter(log_formatter)
    rotating_file.setLevel(logging.INFO)

    root_logger.addHandler(rotating_file)


def main():
    init_logging()

    logger = logging.getLogger('SimpleIterator')

    if len(sys.argv) <= 1:
        logger.critical('usage: usergrid_iterator {url}')
        exit(1)

    url = sys.argv[1]
    logger.info('Beginning to iterate URL: %s' % url)

    q = UsergridQueryIterator(url)

    counter = 0

    start = datetime.datetime.utcnow()
    try:
        for e in q:
            counter += 1
            logger.info('Entity # [%s]: name=[%s] uuid=[%s] created=[%s] modified=[%s]' % (counter, e.get('name'), e.get('uuid'), e.get('created'), e.get('modified')))

    except KeyboardInterrupt:
        logger.critical('KEYBOARD INTERRUPT')
        pass

    finish = datetime.datetime.utcnow()

    logger.info('final entity count is [%s] in  [%s] for query [%s]' % (counter, (finish-start), url))

if __name__ == '__main__':
    main()