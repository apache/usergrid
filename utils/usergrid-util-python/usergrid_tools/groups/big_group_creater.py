import json
import traceback
from multiprocessing import Pool

import datetime
import urllib3

import requests

group_name = 'precisely-10k'
users = 10000
username_template = 'precisely-10k-%s'

url_data = {
    "api_url": "https://usergrid-e2e-prod.e2e.apigee.net/appservices-2-1/",
    "org": "tempgrey",
    "app": "sandbox",
    "client_id": "",
    "client_secret": "",

}

url_data = {
    "api_url": "http://baas-ug002sr.apigee.net/",
    "org": "apigee-vfmplus",
    "app": "sandbox",
    "client_id": "",
    "client_secret": "",

}

collection_url_template = "{api_url}/{org}/{app}/{collection}"
add_user_url_template = "{api_url}/{org}/{app}/groups/{group_name}/users/{uuid}"


def create_group(name):
    url = collection_url_template.format(collection='groups', **url_data)
    print url
    r = requests.post(url, data=json.dumps({"path": name, "name": name}))

    if r.status_code not in [200, 400]:
        print r.text
        exit()


def create_user(username):
    url = collection_url_template.format(collection='users', **url_data)
    r = requests.post(url, data=json.dumps({"username": username}))

    if r.status_code not in [200, 400]:
        print r.text
        exit()

    print 'Created user %s' % username


def map_user(username):
    try:
        url = add_user_url_template.format(group_name=group_name, uuid=username, **url_data)
        r = requests.post(url, data=json.dumps({"username": username}))

        if r.status_code != 200:
            print r.text
            exit()

        print 'Mapped user %s' % username
    except:
        print traceback.format_exc()


user_names = [username_template % i for i in xrange(0, users)]

pool = Pool(64)

start = datetime.datetime.utcnow()
pool.map(create_user, user_names)

create_group(group_name)

pool.map(map_user, user_names)

finish = datetime.datetime.utcnow()

td = finish - start

print td
