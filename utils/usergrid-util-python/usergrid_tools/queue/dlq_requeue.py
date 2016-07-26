import argparse
import json
import datetime
import os
import time
import sys
import uuid
from Queue import Empty

import boto
from boto import sqs
from multiprocessing import Process, Queue

from boto.sqs.message import RawMessage

__author__ = 'Jeff West @ ApigeeCorporation'


def total_seconds(td):
    return (td.microseconds + (td.seconds + td.days * 24.0 * 3600) * 10.0 ** 6) / 10.0 ** 6


def total_milliseconds(td):
    return (td.microseconds + td.seconds * 1000000) / 1000


def get_time_remaining(count, rate):
    if rate == 0:
        return 'NaN'

    seconds = count * 1.0 / rate

    m, s = divmod(seconds, 60)
    h, m = divmod(m, 60)

    return "%d:%02d:%02d" % (h, m, s)


def parse_args():
    parser = argparse.ArgumentParser(description='Usergrid Loader - Queue Monitor')

    parser.add_argument('--readers',
                        help='The queue to load into',
                        type=int,
                        default=10)

    parser.add_argument('--writers',
                        help='The queue to load into',
                        type=int,
                        default=10)

    parser.add_argument('-c', '--config',
                        help='The queue to load into',
                        type=str,
                        default='%s/.usergrid/queue_monitor.json' % os.getenv("HOME"))

    parser.add_argument('--source_queue_name',
                        help='The queue name to send messages to.  If not specified the filename is used',
                        default='entities',
                        type=str)

    parser.add_argument('--target_queue_name',
                        help='The queue name to send messages to.  If not specified the filename is used',
                        default='entities',
                        type=str)

    my_args = parser.parse_args(sys.argv[1:])

    print str(my_args)

    return vars(my_args)


class Writer(Process):
    def __init__(self, queue_name, sqs_config, work_queue):
        super(Writer, self).__init__()
        self.queue_name = queue_name
        self.sqs_config = sqs_config
        self.work_queue = work_queue

    def run(self):
        sqs_conn = boto.sqs.connect_to_region(**self.sqs_config)

        sqs_queue = sqs_conn.get_queue(self.queue_name)
        sqs_queue.set_message_class(RawMessage)
        counter = 0

        # note that there is a better way but this way works.  update would be to use the batch interface

        batch = []

        while True:
            try:
                body = self.work_queue.get(timeout=10)
                counter += 1

                if counter % 100 == 1:
                    print 'WRITER %s' % counter

                batch.append((str(uuid.uuid1()), body, 0))

                if len(batch) >= 10:
                    print 'WRITING BATCH'
                    sqs_queue.write_batch(batch, delay_seconds=300)
                    batch = []

            except Empty:

                if len(batch) > 0:
                    print 'WRITING BATCH'
                    sqs_queue.write_batch(batch, delay_seconds=300)
                    batch = []


class Reader(Process):
    def __init__(self, queue_name, sqs_config, work_queue):
        super(Reader, self).__init__()
        self.queue_name = queue_name
        self.sqs_config = sqs_config
        self.work_queue = work_queue

    def run(self):

        sqs_conn = boto.sqs.connect_to_region(**self.sqs_config)

        sqs_queue = sqs_conn.get_queue(self.queue_name)
        sqs_queue.set_message_class(RawMessage)

        message_counter = 0

        while True:

            messages = sqs_queue.get_messages(num_messages=10)
            print 'Read %s messages' % (len(messages))
            for message in messages:
                message_counter += 1

                if message_counter % 100 == 1:
                    print 'READ: %s' % message_counter

                body = message.get_body()
                self.work_queue.put(body)

            sqs_queue.delete_message_batch(messages)


def main():
    args = parse_args()

    source_queue_name = args.get('source_queue_name')
    target_queue_name = args.get('target_queue_name')

    start_time = datetime.datetime.utcnow()
    first_start_time = start_time

    print "first start: %s" % first_start_time

    with open(args.get('config'), 'r') as f:
        config = json.load(f)

    sqs_config = config.get('sqs')

    work_queue = Queue()

    readers = [Reader(source_queue_name, sqs_config, work_queue) for r in xrange(args.get('readers'))]
    [r.start() for r in readers]

    writers = [Writer(target_queue_name, sqs_config, work_queue) for r in xrange(args.get('writers'))]
    [w.start() for w in writers]


if __name__ == '__main__':
    main()
