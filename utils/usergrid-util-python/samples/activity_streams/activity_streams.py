# */
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *   http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing,
# * software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# * KIND, either express or implied.  See the License for the
#    * specific language governing permissions and limitations
# * under the License.
# */

# docs page: http://docs.apigee.com/api-baas/content/creating-activity

# create user 1
# post event for user 1
# check feed for user 1

# create user 2
# user 2 follows user 1
# post event for user 1

# check feed for user 1
# check feed for user 2
import json

import requests

collection_url_template = "{api_url}/{org}/{app}/{collection}"
entity_url_template = "{api_url}/{org}/{app}/{collection}/{entity_id}"
connection_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}"
connection_create_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_uuid}"

user_url_template = "{api_url}/{org}/{app}/users/{username}"
user_feed_url_template = "{api_url}/{org}/{app}/users/{username}/feed"
user_activity_url_template = "{api_url}/{org}/{app}/users/{username}/activities"
user_follows_url_template = "{api_url}/{org}/{app}/users/{user2}/following/users/{user1}"

url_data = {
    'api_url': 'https://amer-apibaas-prod.apigee.net/appservices',
    'org': 'jwest-samples',
    'app': 'feed-example'
}

session = requests.Session()


def create_user(user):
    data = {
        'username': user,
        'email': '%s@example.com' % user
    }

    url = collection_url_template.format(collection='users', **url_data)

    r = session.post(url, json.dumps(data))

    if r.status_code != 200:
        print 'Error creating user [%s] at URL=[%s]: %s' % (user, url, r.text)


def post_activity(user, text):
    activity = {
        "actor": {
            "displayName": user,
            "username": user,
            "image": {
                "duration": 0,
                "height": 80,
                "url": "http://www.gravatar.com/avatar/", "width": 80},
            "email": "%s@example.com" % user
        },
        "verb": "post",
        "content": text
    }

    url = user_activity_url_template.format(username=user, **url_data)

    r = session.post(url, json.dumps(activity))

    if r.status_code != 200:
        print 'Error creating activity for user [%s] at URL=[%s]: %s' % (user, url, r.text)


def get_feed(user):
    url = user_feed_url_template.format(username=user, **url_data)

    r = session.get(url)

    if r.status_code != 200:
        print 'Error getting feed for user [%s] at URL=[%s]: %s' % (user, url, r.text)

    else:
        print '----- START'
        print json.dumps(r.json(), indent=2)
        print '----- END'


def create_follows(user, user_to_follow):
    url = user_follows_url_template.format(user1=user, user2=user_to_follow, **url_data)

    r = session.post(url)

    print r.text

    if r.status_code != 200:
        print 'Error getting creating follows from user [%s] to user [%s] at URL=[%s]: %s' % (
            user, user_to_follow, url, r.text)


def delete_user(username):
    url = user_url_template.format(username=username, **url_data)

    r = session.post(url)

    # print r.text

    if r.status_code != 200:
        print 'Error deleting user [%s] at URL=[%s]: %s' % (username, url, r.text)


user_base = 'natgeo'

user1 = '%s_%s' % (user_base, 1)
user2 = '%s_%s' % (user_base, 2)

create_user(user1)
post_activity(user1, 'Hello World!')

get_feed(user1)

create_user(user2)
create_follows(user2, user1)
post_activity(user2, "I'm here!")
get_feed(user2)

post_activity(user1, 'SEE YA!!')

get_feed(user2)

get_feed(user1)

delete_user(user1)
delete_user(user2)
