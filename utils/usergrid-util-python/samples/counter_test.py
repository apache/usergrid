import datetime
import time
import json

import requests

tstamp = time.gmtime() * 1000

s = requests.Session()

s.headers.update({'authorization': 'Bearer YWMt7AHANAKcEeaVR-EahuX8EgAAAVQ7Q56jxQjUsmhJn8rGLTth0XtRrBSIzDA'})
s.headers.update({'content-type': 'application/json'})

url = 'https://host/appservices-new/usergrid/pushtest/events'

body = {
    "timestamp": tstamp,
    "counters": {
        "counters.jeff.west": 1
    }
}

r = s.post(url, data=json.dumps(body))

print r.status_code

time.sleep(30)

r = s.get('https://host/appservices-new/usergrid/pushtest//counters?counter=counters.jeff.west')

print r.text
