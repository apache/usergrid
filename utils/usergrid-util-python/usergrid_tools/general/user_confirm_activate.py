import json

import requests

### This will make the API calls to activate and confirm an array of users

users = [
    'user1@example.com',
    'user2@example.com'
]

TOKEN = 'ABC123'
URL = "http://localhost:8080/management/users/%s"

s = requests.Session()
s.headers.update({'authorization': 'Bearer %s' % TOKEN})

for user in users:

    r = s.put(URL % user, data=json.dumps({"activated": True}))
    print 'Activated %s: %s' % (user, r.status_code)

    if r.status_code != 200:
        print r.text
        continue

    r = s.put(URL % user, data=json.dumps({"confirmed": True}))

    print 'Confirmed %s: %s' % (user, r.status_code)
