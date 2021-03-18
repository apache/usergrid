import random
import requests
import json

# URL Templates for Usergrid
collection_url_template = "{api_url}/{org}/{app}/{collection}?client_id={client_id}&client_secret={client_secret}"
collection_query_url_template = "{api_url}/{org}/{app}/{collection}?ql={ql}&client_id={client_id}&client_secret={client_secret}&limit={limit}"
collection_graph_url_template = "{api_url}/{org}/{app}/{collection}?client_id={client_id}&client_secret={client_secret}&limit={limit}"
connection_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}?client_id={client_id}&client_secret={client_secret}"
connecting_query_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/connecting/{verb}?client_id={client_id}&client_secret={client_secret}"
connection_create_by_uuid_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_uuid}?client_id={client_id}&client_secret={client_secret}"
connection_create_by_name_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_type}/{target_name}?client_id={client_id}&client_secret={client_secret}"

connection_create_by_pairs_url_template = "{api_url}/{org}/{app}/{source_type_id}/{verb}/{target_type_id}?client_id={client_id}&client_secret={client_secret}"

get_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}&connections=none"
get_entity_url_with_connections_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"
put_entity_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}?client_id={client_id}&client_secret={client_secret}"

user_credentials_url_template = "{api_url}/{org}/{app}/users/{uuid}/credentials"

url_data = {
    'api_url': 'https://api.usergrid.com',
    'org': 'org1',
    'app': 'app1',
    'client_id': 'foo',
    'client_secret': 'bar'
}

entity_count = 10

for x in xrange(0, 100):
    data = {
        'name': 'thing%s' % x,
        'value': random.randint(1, 100)
    }

    url = put_entity_url_template.format(collection='things', uuid='thing%s' % x, **url_data)
    print url
    r = requests.put(url, data=json.dumps(data))

    if r.status_code != 200:
        print 'damn'
        exit(1)

    data = {
        'name': 'owner%s' % x,
        'value': random.randint(1, 100)
    }

    url = put_entity_url_template.format(collection='owners', uuid='owner%s' % x, **url_data)
    print url
    r = requests.put(url, data=json.dumps(data))

    connection_create_by_name_url_template = "{api_url}/{org}/{app}/{collection}/{uuid}/{verb}/{target_type}/{target_name}?client_id={client_id}&client_secret={client_secret}"

    url = connection_create_by_name_url_template.format(
        collection='owners',
        uuid='owner%s' % x,
        verb='owns',
        target_type='things',
        target_name='thing%s' % random.randint(0, x),
        **url_data
    )

    print url
    r = requests.post(url)

    if r.status_code != 200:
        print 'damn'
        exit(1)
