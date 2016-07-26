import json
import time
import requests

__author__ = 'Jeff West @ ApigeeCorporation'

# The purpose of this script is to set certain nodes in an ElasticSearch cluster to be excluded from index allocation,
# generally for the purpose of shutting down or restarting the node

SHUTDOWN_NODES = True

nodes = [
    # 'res206wo',
    # 'res207wo',
]

base_url = 'http://localhost:9200'

exclude_nodes = nodes

nodes_string = ",".join(exclude_nodes)

print 'Excluding: ' + nodes_string
url_template = '%s/_cluster/settings' % base_url

status_code = 503

while status_code >= 500:
    r = requests.put(
        '%s/_cluster/settings' % base_url,
        data=json.dumps({
            "transient": {
                "cluster.routing.allocation.exclude._host": nodes_string
            }
        }))

    status_code = r.status_code

    print '%s: %s' % (r.status_code, r.text)

ready = False

nodes_shut_down = []

while not ready:

    ready = True
    nodes_left = 0
    bytes_left = 0

    for node in exclude_nodes:
        node_url = '%s/_nodes/%s/stats' % (base_url, node)
        r = requests.get(node_url)

        if r.status_code == 200:
            # print r.text

            node_stats = r.json()

            for field, data in node_stats.get('nodes').iteritems():
                if data.get('name') == node:
                    size = data.get('indices', {}).get('store', {}).get('size_in_bytes', 1)
                    docs = data.get('indices', {}).get('docs', {}).get('count', 1)

                    if size > 0 and docs > 0:
                        print 'Node: %s - size %s' % (node, size)
                        bytes_left += size
                        ready = False and ready
                        nodes_left += 1
                    else:
                        if SHUTDOWN_NODES:
                            if not node in nodes_shut_down:
                                nodes_shut_down.append(node)
                                shutdown_url = '%s/_cluster/nodes/%s/_shutdown' % (base_url, node)

                                print 'Shutting down node %s: %s' % (node, shutdown_url)

                                r = requests.post(shutdown_url)

                                if r.status_code == 200:
                                    nodes_shut_down.append(node)
                                    print 'Shut down node %s' % node
                                else:
                                    print 'Shutdown failed: %s: %s' % (r.status_code, r.text)
    if not ready:
        print 'NOT READY! Waiting for %s nodes and %s GB' % (nodes_left, bytes_left / 1024.0 / 1000000)
        time.sleep(10)

# print 'READY TO MOVE!'
