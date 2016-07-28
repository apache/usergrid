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

import json
import logging
import traceback
import requests
import time

__author__ = 'Jeff.West@yahoo.com'


class UsergridQueryIterator(object):
    def __init__(self,
                 url,
                 operation='GET',
                 sleep_time=10,
                 page_delay=0,
                 headers=None,
                 data=None):

        if not data:
            data = {}
        if not headers:
            headers = {}

        self.page_counter = 0
        self.total_retrieved = 0
        self.logger = logging.getLogger('usergrid.UsergridQuery')
        self.data = data
        self.headers = headers
        self.url = url
        self.operation = operation
        self.next_cursor = None
        self.entities = []
        self.count_retrieved = 0
        self._pos = 0
        self.last_response = None
        self.page_delay = page_delay
        self.sleep_time = sleep_time
        self.session = None

    def _get_next_response(self, attempts=0):

        if self.session is None:
            self.session = requests.Session()

        try:
            if self.operation == 'PUT':
                op = self.session.put
            elif self.operation == 'DELETE':
                op = self.session.delete
            else:
                op = self.session.get

            target_url = self.url

            if self.next_cursor is not None:
                delim = '&' if '?' in target_url else '?'
                target_url = '%s%scursor=%s' % (self.url, delim, self.next_cursor)

            self.logger.debug('Operation=[%s] URL=[%s]' % (self.operation, target_url))

            r = op(target_url, data=json.dumps(self.data), headers=self.headers)

            if r.status_code == 200:
                r_json = r.json()
                count_retrieved = len(r_json.get('entities', []))
                self.total_retrieved += count_retrieved
                self.logger.debug('Retrieved [%s] entities in [%s]th page in [%s], total from [%s] is [%s]' % (
                    count_retrieved, self.page_counter, r.elapsed, self.url, self.total_retrieved))

                return r_json

            elif r.status_code in [401, 404] and 'service_resource_not_found' in r.text:
                self.logger.error('Query Not Found [%s] on URL=[%s]: %s' % (r.status_code, target_url, r.text))
                raise SystemError('Query Not Found [%s] on URL=[%s]: %s' % (r.status_code, target_url, r.text))

            else:
                if attempts < 10:
                    self.logger.error('Sleeping %s after HTTP [%s] for retry attempt=[%s] on URL=[%s], response: %s' % (
                        self.sleep_time, r.status_code, attempts, target_url, r.text))

                    time.sleep(self.sleep_time)

                    return self._get_next_response(attempts=attempts + 1)

                else:
                    raise SystemError('Unable to get next response after %s attempts' % attempts)

        except:
            print traceback.format_exc()

    def next(self):

        if self.last_response is None:
            self.logger.debug('getting first page, url=[%s]' % self.url)

            self._process_next_page()

        elif self._pos >= len(self.entities) > 0 and self.next_cursor is not None:

            self.logger.debug('getting next page, count=[%s] url=[%s], cursor=[%s]' % (
                self.count_retrieved, self.url, self.next_cursor))

            self._process_next_page()
            self.logger.debug('Sleeping [%s]s between pages' % self.page_delay)

            time.sleep(self.page_delay)

        if self._pos < len(self.entities):
            response = self.entities[self._pos]
            self._pos += 1
            return response

        raise StopIteration

    def __iter__(self):
        return self

    def _process_next_page(self, attempts=0):

        api_response = self._get_next_response()

        if api_response is None:
            message = 'Unable to retrieve query results from url=[%s]' % self.url
            self.logger.error(message)
            api_response = {}
            raise StopIteration

        self.last_response = api_response

        self.entities = api_response.get('entities', [])
        self.next_cursor = api_response.get('cursor', None)
        self._pos = 0
        self.count_retrieved += len(self.entities)
        self.page_counter += 1

        if self.next_cursor is None:
            self.logger.debug('no cursor in response. Total pages=[%s], entities=[%s] url=[%s]' % (
                self.page_counter, self.count_retrieved, self.url))
