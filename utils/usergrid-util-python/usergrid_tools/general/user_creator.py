import json
import requests

### This will create an array of org-level management users

users = [
    'me@example.com'
]

for user in users:

    post_body = {
        "username": user,
        "name": user,
        "email": user,
        "password": "test12345"
    }

    print json.dumps(post_body)

    r = requests.post('http://localhost:8080/management/organizations/asdf/users',
                      headers={
                          'Authorization': 'Bearer SADFSDF',
                          'Content-Type': 'application/json'
                      },
                      data=json.dumps(post_body))

    print r.status_code

    print '%s: created (POST) [%s]: %s' % (user, r.status_code, r.text)

    #
    # r = requests.put('http://localhost:8080/management/users/%s' % user,
    #                  headers={
    #                      'Authorization': 'Bearer YWMtFlVrhK8nEeW-AhmxdmpAVAAAAVIYTHxTNSUxpQyUWZQ2LsZxcXSdNtO_lWo',
    #                      'Content-Type': 'application/json'
    #                  },
    #                  data=json.dumps('{"confirmed": true}'))
    #
    # print '%s: confirmed: %s' % (user, r.status_code)
    #
    # r = requests.put('http://localhost:8080/management/users/%s' % user,
    #                  headers={
    #                      'Authorization': 'Bearer YWMtFlVrhK8nEeW-AhmxdmpAVAAAAVIYTHxTNSUxpQyUWZQ2LsZxcXSdNtO_lWo',
    #                      'Content-Type': 'application/json'
    #                  },
    #                  data=json.dumps('{"activated": true}'))
    #
    # print '%s: activated: %s' % (user, r.status_code)
