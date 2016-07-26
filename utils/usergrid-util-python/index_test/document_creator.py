from __future__ import print_function
from Queue import Empty
import json
from multiprocessing import JoinableQueue, Process
import random
import re
import uuid
import sys

import argparse

import loremipsum


def parse_args():
    parser = argparse.ArgumentParser(description='ElasticSearch Index Test 1')

    parser.add_argument('-w', '--workers',
                        help='The number of worker threads',
                        type=int,
                        default=8)

    parser.add_argument('-dc', '--document_count',
                        help='The number of documents per index',
                        type=long,
                        default=100000000)

    parser.add_argument('--output',
                        help='The filename to write to',
                        type=str,
                        default='generated_documents.txt')

    parser.add_argument('--fields_min',
                        help='The min number of fields per document',
                        type=long,
                        default=10)

    parser.add_argument('--fields_max',
                        help='The max number of fields per document',
                        type=long,
                        default=100)

    parser.add_argument('-tp', '--type_prefix',
                        help='The Prefix to use for type names',
                        type=str,
                        default='type_this')

    my_args = parser.parse_args(sys.argv[1:])

    return vars(my_args)


args = parse_args()

sentence_list = loremipsum.get_sentences(10000)


class Worker(Process):
    def __init__(self, work_queue, response_queue):
        super(Worker, self).__init__()
        self.work_queue = work_queue
        self.response_queue = response_queue
        self.sentence_list = loremipsum.get_sentences(1000)
        self.re_first_word = re.compile('([A-z]+)')

    def run(self):
        print('Starting %s ' % self.name)

        while True:
            task = self.work_queue.get(timeout=600)
            field_count = random.randint(task['fields_min'], task['fields_max'])
            document = self.generate_document(field_count)
            flattened_doc = self.process_document(document,
                                                  task['uuid'],
                                                  task['uuid'])

            self.response_queue.put(flattened_doc)

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
    def process_document(document, application_id, uuid):
        response = {
            'entityId': uuid,
            'entityVersion': '1',
            'applicationId': application_id,
            'fields': Worker.get_fields(document)
        }

        return response

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


class Writer(Process):
    def __init__(self, document_queue):
        super(Writer, self).__init__()
        self.document_queue = document_queue

    def run(self):
        keep_going = True

        with open(args['output'], 'w') as f:
            while keep_going:
                try:
                    document = self.document_queue.get(timeout=300)
                    print(json.dumps(document), file=f)

                except Empty:
                    print('done!')
                    keep_going = False


def total_milliseconds(td):
    return (td.microseconds + td.seconds * 1000000) / 1000


def main():
    work_queue = JoinableQueue()
    response_queue = JoinableQueue()

    workers = [Worker(work_queue, response_queue) for x in xrange(args.get('workers'))]

    writer = Writer(response_queue)
    writer.start()

    [worker.start() for worker in workers]

    try:
        total_messages = args.get('document_count')
        batch_size = 100000
        message_counter = 0

        for doc_number in xrange(total_messages):
            message_counter += 1

            for count in xrange(batch_size):
                doc_id = str(uuid.uuid1())

                task = {
                    'fields_min': args['fields_min'],
                    'fields_max': args['fields_max'],
                    'uuid': doc_id
                }

                work_queue.put(task)

        print('Joining queues counter=[%s]...' % message_counter)
        work_queue.join()
        response_queue.join()
        print('Done queue counter=[%s]...' % message_counter)

    except KeyboardInterrupt:
        [worker.terminate() for worker in workers]


main()