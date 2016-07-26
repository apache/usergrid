import json
from multiprocessing import JoinableQueue, Process
import random
import re
import traceback
import uuid
import time
import sys

import argparse
import loremipsum
import requests
from elasticsearch import Elasticsearch

es_hosts = [
    {'host': 'ees000wo', 'port': 9200},
    {'host': 'ees001wo', 'port': 9200},
    {'host': 'ees002wo', 'port': 9200},
    {'host': 'ees003wo', 'port': 9200},
    {'host': 'ees004wo', 'port': 9200},
    {'host': 'ees005wo', 'port': 9200},
    {'host': 'ees006wo', 'port': 9200},
    {'host': 'ees007wo', 'port': 9200},
    {'host': 'ees008wo', 'port': 9200},
    {'host': 'ees009wo', 'port': 9200},
    {'host': 'ees010wo', 'port': 9200},
    {'host': 'ees011wo', 'port': 9200},
    {'host': 'ees012wo', 'port': 9200},
    {'host': 'ees013wo', 'port': 9200},
    {'host': 'ees014wo', 'port': 9200},
    {'host': 'ees015wo', 'port': 9200},
    {'host': 'ees016wo', 'port': 9200},
    {'host': 'ees017wo', 'port': 9200}
]


def parse_args():
    parser = argparse.ArgumentParser(description='ElasticSearch Index Test 1')

    parser.add_argument('-t', '--type_count',
                        help='The number of types to produce',
                        type=int,
                        default=50)

    parser.add_argument('-ic', '--index_count',
                        help='The number of indices to create',
                        type=int,
                        default=50)

    parser.add_argument('-sc', '--shard_count',
                        help='The number of indices to create',
                        type=int,
                        default=50)

    parser.add_argument('-rc', '--replica_count',
                        help='The number of indices to create',
                        type=int,
                        default=1)

    parser.add_argument('-w', '--workers',
                        help='The number of worker threads',
                        type=int,
                        default=8)

    parser.add_argument('-dc', '--document_count',
                        help='The number of documents per index',
                        type=long,
                        default=100000000)

    parser.add_argument('-bs', '--batch_size',
                        help='The size of batches to send to ES',
                        type=long,
                        default=25)

    parser.add_argument('-ip', '--index_prefix',
                        help='The Prefix to use for index names',
                        type=str,
                        default='apigee_ftw')

    parser.add_argument('-tp', '--type_prefix',
                        help='The Prefix to use for type names',
                        type=str,
                        default='type_this')

    parser.add_argument('-s', '--setup',
                        help='The Prefix to use for type names',
                        action='store_true')

    my_args = parser.parse_args(sys.argv[1:])

    return vars(my_args)


args = parse_args()


class APIClient():
    def __init__(self, base_url):
        self.base_url = base_url

    def put(self, path='/', data=None):
        if not data:
            data = {}

        url = '%s%s' % (self.base_url, path)
        r = requests.put(url, json.dumps(data))

        if r.status_code == 200:
            print 'PUT (%s) in %sms' % (r.status_code, total_milliseconds(r.elapsed))
            return r.json()

        raise Exception('HTTP %s calling PUT on URL=[%s]: %s' % (r.status_code, url, r.text))

    def index_docs(self, index, documents, type):

        data = ''

        for doc in documents:
            data += '{ "index" : { "_index" : "%s", "_type" : "%s", "_id" : "%s" } }\n' % (index, type, doc['entityId'])
            data += json.dumps(doc)
            data += '\n'

        url = '%s/_bulk' % self.base_url

        # print data

        r = requests.post(url, data)

        # print json.dumps(r.json(), indent=2)

        if r.status_code == 200:
            print 'PUT (%s) in %sms' % (r.status_code, total_milliseconds(r.elapsed))
            return r.json()

        raise Exception('HTTP %s calling POST URL=[%s]: %s' % (r.status_code, url, r.text))

    def delete(self, index):
        url = '%s%s' % (self.base_url, index)
        r = requests.delete(url)

        if r.status_code == 200:
            print 'DELETE (%s) in %sms' % (r.status_code, total_milliseconds(r.elapsed))
            return r.json()

        raise Exception('HTTP %s calling DELETE URL=[%s]: %s' % (r.status_code, url, r.text))

    def create_index(self, name=None, shards=18 * 3, replicas=1):
        data = {
            "settings": {
                "index": {
                    "action": {
                        "write_consistency": "one"
                    },
                    "number_of_shards": shards,
                    "number_of_replicas": replicas
                }
            }
        }

        try:
            print 'Creating index %s' % name
            response = self.put('/%s/' % name.lower(), data)

            print response

        except Exception, e:
            print traceback.format_exc()

    def delete_index(self, name):
        try:
            response = self.delete('/%s/' % name.lower())

            print response

        except Exception, e:
            print traceback.format_exc()

    def define_type_mapping(self, index_name, type_name):
        try:
            url = '/%s/_mapping/%s' % (index_name, type_name)
            print url

            response = self.put(url, get_type_mapping(type_name))

            print response

        except Exception, e:
            print traceback.format_exc()


class Worker(Process):
    def __init__(self, work_queue):
        super(Worker, self).__init__()
        self.api_client = APIClient('http://%s:9200' % es_hosts[random.randint(0, len(es_hosts) - 1)].get('host'))
        self.work_queue = work_queue
        self.es = Elasticsearch(es_hosts)
        self.sentence_list = loremipsum.get_sentences(1000)
        self.re_first_word = re.compile('([A-z]+)')

    def run(self):
        print 'Starting %s ' % self.name
        counter = 0

        docs = {}

        while True:
            index_batch_size = args.get('batch_size')
            task = self.work_queue.get(timeout=600)
            counter += 1

            document = self.generate_document(task['field_count'])
            flattened_doc = self.process_document(document,
                                                  task['type'],
                                                  task['uuid'],
                                                  task['uuid'])

            index_type_tuple = (task['index'], task['type'])

            # self.handle_document(task['index'], task['type'], task['uuid'], flattened_doc)

            doc_array = docs.get(index_type_tuple)

            if doc_array is None:
                doc_array = []
                docs[index_type_tuple] = doc_array

            doc_array.append(flattened_doc)

            if len(doc_array) >= index_batch_size:
                self.handle_batch(task['index'], task['type'], doc_array)
                doc_array = []

            self.work_queue.task_done()

    def generate_document(self, fields):

        doc = {}

        my_bool = True

        for i in xrange(fields):
            sentence_index = random.randint(0, max((fields / 2) - 1, 1))
            sentence = self.sentence_list[sentence_index]

            if random.random() >= .5:
                key = self.re_first_word.findall(sentence)[1]
            else:
                key = self.re_first_word.findall(sentence)[1] + str(i)

            field_type = random.random()

            if field_type <= 0.3:
                doc[key] = sentence

            elif field_type <= 0.5:
                doc[key] = random.randint(1, 1000000)

            elif field_type <= 0.6:
                doc[key] = random.random() * 1000000000

            elif field_type == 0.7:
                doc[key] = my_bool
                my_bool = not my_bool

            elif field_type == 0.8:
                doc[key] = self.generate_document(max(fields / 5, 1))

            elif field_type <= 1.0:
                doc['mylocation'] = self.generate_location()

        return doc

    @staticmethod
    def get_fields(document, base_name=None):
        fields = []

        for name, value in document.iteritems():
            if base_name:
                field_name = '%s.%s' % (base_name, name)
            else:
                field_name = name

            if isinstance(value, dict):
                fields += Worker.get_fields(value, field_name)
            else:
                value_name = None
                if isinstance(value, basestring):
                    value_name = 'string'

                elif isinstance(value, bool):
                    value_name = 'boolean'

                elif isinstance(value, (int, long)):
                    value_name = 'long'

                elif isinstance(value, float):
                    value_name = 'double'

                if value_name:
                    field = {
                        'name': field_name,
                        value_name: value
                    }
                else:
                    field = {
                        'name': field_name,
                        'string': str(value)
                    }

                fields.append(field)

        return fields

    @staticmethod
    def process_document(document, doc_type, application_id, uuid):
        response = {
            'entityId': uuid,
            'entityVersion': '1',
            'entityType': doc_type,
            'applicationId': application_id,
            'fields': Worker.get_fields(document)
        }

        return response

    def handle_document(self, index, doc_type, uuid, document):

        res = self.es.create(index=index,
                             doc_type=doc_type,
                             id=uuid,
                             body=document)

        print res

    def generate_location(self):
        response = {}

        lat = random.random() * 90.0
        lon = random.random() * 180.0

        lat_neg_true = True if lon > .5 else False
        lon_neg_true = True if lat > .5 else False

        lat = lat * -1.0 if lat_neg_true else lat
        lon = lon * -1.0 if lon_neg_true else lon

        response['location'] = {
            'lat': lat,
            'lon': lon
        }

        return response

    def handle_batch(self, index, doc_type, docs):
        print 'HANDLE BATCH'
        self.api_client.define_type_mapping(index, doc_type)
        self.api_client.index_docs(index, docs, doc_type)


def total_milliseconds(td):
    return (td.microseconds + td.seconds * 1000000) / 1000


def get_type_mapping(type_name):
    return {
        type_name: {
            "_routing": {
                "path": "entityId",
                "required": True
            },
            "properties": {
                "entityId": {
                    "type": "string",
                    "index": "not_analyzed",
                    "doc_values": True
                },
                "entityVersion": {
                    "type": "string",
                    "index": "not_analyzed",
                    "doc_values": True
                },
                "entityType": {
                    "type": "string",
                    "index": "not_analyzed",
                    "doc_values": True
                },
                "applicationId": {
                    "type": "string",
                    "index": "not_analyzed",
                    "doc_values": True
                },
                "nodeId": {
                    "type": "string",
                    "index": "not_analyzed",
                    "doc_values": True
                },
                "edgeName": {
                    "type": "string",
                    "index": "not_analyzed",
                    "doc_values": True
                },
                "entityNodeType": {
                    "type": "string",
                    "index": "not_analyzed",
                    "doc_values": True
                },
                "edgeTimestamp": {
                    "type": "long",
                    "doc_values": True
                },
                "edgeSearch": {
                    "type": "string",
                    "index": "not_analyzed",
                    "doc_values": True
                },
                "fields": {
                    "type": "nested",
                    "properties": {
                        "name": {
                            "type": "string",
                            "index": "not_analyzed",
                            "doc_values": True
                        },
                        "boolean": {
                            "type": "boolean",
                            "doc_values": True
                        },
                        "long": {
                            "type": "long",
                            "doc_values": True
                        },
                        "double": {
                            "type": "double",
                            "doc_values": True
                        },
                        "location": {
                            "type": "geo_point",
                            "lat_lon": True,
                            "geohash": True,
                            "doc_values": True
                        },
                        "string": {
                            "type": "string",
                            "norms": {
                                "enabled": False
                            },
                            "fields": {
                                "exact": {
                                    "type": "string",
                                    "index": "not_analyzed",
                                    "doc_values": True
                                }
                            }
                        },
                        "uuid": {
                            "type": "string",
                            "index": "not_analyzed",
                            "doc_values": True
                        }
                    }
                }
            },
            "_all": {
                "enabled": False
            }

        }
    }


def main():
    INDEX_COUNT = args.get('index_count')
    TYPE_COUNT = args.get('type_count')
    SETUP = args.get('setup')

    indices = []
    types = []
    work_queue = JoinableQueue()

    apiclient = APIClient('http://%s:9200' % es_hosts[random.randint(1, len(es_hosts) - 1)].get('host'))

    workers = [Worker(work_queue) for x in xrange(args.get('workers'))]
    [worker.start() for worker in workers]

    try:
        #
        for x in xrange(TYPE_COUNT):
            type_name = '%s_%s' % (args.get('type_prefix'), x)
            types.append(type_name)

        for x in xrange(INDEX_COUNT):
            index_name = '%s_%s' % (args.get('index_prefix'), x)
            indices.append(index_name)

        if SETUP:
            print 'Running setup...'

            for index_name in indices:
                apiclient.delete_index(index_name)

            time.sleep(5)

            for index_name in indices:
                apiclient.create_index(
                    index_name,
                    shards=args['shard_count'],
                    replicas=args['replica_count'])

                # time.sleep(5)

                # for index_name in indices:
                # for type_name in types:
                # apiclient.define_type_mapping(index_name, type_name)

                # time.sleep(5)

        total_messages = args.get('document_count')
        batch_size = 100000
        message_counter = 0
        fields = random.randint(50, 100)

        while message_counter < total_messages:

            for count in xrange(batch_size):

                for index_name in indices:
                    doc_id = str(uuid.uuid1())

                    task = {
                        'field_count': fields,
                        'uuid': doc_id,
                        'index': index_name,
                        'type': types[random.randint(0, len(types) - 1)]
                    }

                    work_queue.put(task)

            print 'Joining queue counter=[%s]...' % message_counter
            work_queue.join()
            print 'Done queue counter=[%s]...' % message_counter
            message_counter += batch_size

    except KeyboardInterrupt:
        [worker.terminate() for worker in workers]


main()
