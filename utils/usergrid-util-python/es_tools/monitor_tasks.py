import datetime
import requests
import time

__author__ = 'Jeff West @ ApigeeCorporation'

# Utility for monitoring pending tasks in ElasticSearch

def total_milliseconds(td):
    return (td.microseconds + td.seconds * 1000000) / 1000


url_template = "http://localhost:9200/_cat/pending_tasks?v'"

x = 0

SLEEP_TIME = 3

while True:
    x += 13
    try:

        r = requests.get(url=url_template)
        lines = r.text.split('\n')

        print '\n-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-'
        print '+++++++++++++++++++++++++++++++++++++++++++++++++++++++++'
        print datetime.datetime.utcnow()
        if len(lines) > 1:
            print r.text
        else:
            print 'None'

        print '-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-'
        print '-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n'

    except:
        pass

    time.sleep(SLEEP_TIME)

