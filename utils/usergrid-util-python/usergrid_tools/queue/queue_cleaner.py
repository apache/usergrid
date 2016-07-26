import argparse
import json
import datetime
import os
import time
import sys

import boto
from boto import sqs
from multiprocessing import Process, Queue

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

    parser.add_argument('-c', '--config',
                        help='The queue to load into',
                        type=str,
                        default='%s/.usergrid/queue_monitor.json' % os.getenv("HOME"))

    parser.add_argument('-q', '--queue_name',
                        help='The queue name to send messages to.  If not specified the filename is used',
                        default='entities',
                        type=str)

    my_args = parser.parse_args(sys.argv[1:])

    print str(my_args)

    return vars(my_args)


class Deleter(Process):
    def __init__(self, queue_name, sqs_config, work_queue):
        super(Deleter, self).__init__()
        self.queue_name = queue_name
        self.sqs_config = sqs_config
        self.work_queue = work_queue

    def run(self):
        sqs_conn = boto.sqs.connect_to_region(**self.sqs_config)

        # queue = sqs_conn.get_queue(self.queue_name)

        while True:
                delete_me = self.work_queue.get()
                delete_me.delete()
                print 'foo'


class Worker(Process):
    def __init__(self, queue_name, sqs_config, delete_queue):
        super(Worker, self).__init__()
        self.queue_name = queue_name
        self.sqs_config = sqs_config
        self.delete_queue = delete_queue

    def run(self):

        sqs_conn = boto.sqs.connect_to_region(**self.sqs_config)

        queue = sqs_conn.get_queue(self.queue_name)

        last_size = queue.count()

        print 'Starting Size: %s' % last_size

        delete_counter = 0
        message_counter = 0

        while True:

            messages = queue.get_messages(num_messages=10, visibility_timeout=300)

            for message in messages:
                message_counter += 1
                body = message.get_body()

                try:

                    msg = json.loads(body)

                    if 'entityDeleteEvent' in msg:
                        if msg['entityDeleteEvent']['entityIdScope']['id']['type'] == 'stock':

                            self.delete_queue.put(message)
                            delete_counter += 1

                            if delete_counter % 100 == 0:
                                print 'Deleted %s of %s' % (delete_counter, message_counter)
                    else:
                        # set it eligible to be read again
                        message.change_visibility(0)

                        print json.dumps(msg)

                except:
                    pass




def main():
    args = parse_args()

    queue_name = args.get('queue_name')

    print 'queue_name=%s' % queue_name

    start_time = datetime.datetime.utcnow()
    first_start_time = start_time

    print "first start: %s" % first_start_time

    with open(args.get('config'), 'r') as f:
        config = json.load(f)

    sqs_config = config.get('sqs')
    last_time = datetime.datetime.utcnow()

    work_queue = Queue()

    deleters = [Deleter(queue_name, sqs_config, work_queue) for x in xrange(100)]
    [w.start() for w in deleters]

    workers = [Worker(queue_name, sqs_config, work_queue) for x in xrange(100)]

    [w.start() for w in workers]

    time.sleep(60)

if __name__ == '__main__':
    main()
