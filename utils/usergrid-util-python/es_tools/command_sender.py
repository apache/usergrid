import json
import requests

__author__ = 'Jeff West @ ApigeeCorporation'


# Simple utility to send commands, useful to not have to recall the proper format

#
# url = 'http://localhost:9200/_cat/shards'
#
# r = requests.get(url)
#
# response = r.text
#
# print response

data = {
    "commands": [
        {
            "move": {
                "index": "usergrid__a34ad389-b626-11e4-848f-06b49118d7d0__application_target_final",
                "shard": 14,
                "from_node": "res018sy",
                "to_node": "res021sy"
            }
        },
        {
            "move": {
                "index": "usergrid__a34ad389-b626-11e4-848f-06b49118d7d0__application_target_final",
                "shard": 12,
                "from_node": "res018sy",
                "to_node": "res009sy"
            }
        },

    ]
}

r = requests.post('http://localhost:9211/_cluster/reroute', data=json.dumps(data))

print r.text