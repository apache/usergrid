import json
import traceback
import requests

__author__ = 'Jeff West @ ApigeeCorporation'


def total_milliseconds(td):
    return (td.microseconds + td.seconds * 1000000) / 1000


# for Apigee Developer, leave this as is.  For paid BaaS instances change this to https://{your_api_url}/[appservices]
api_url = 'https://api.usergrid.com'

# specify the org[] / app[] / collection[] to delete
# Org and App level are required.  If no collections are specified, all collections will be deleted
# you also need to specify the client_id and secret of each org

data_map = {
    "orgs":
        {
            "myOrg": {
                "apps": {
                    "myApp": {
                        "collections": [
                            'examples'
                        ]
                    }
                },
                "credentials": {
                    "client_id": "foo",
                    "client_secret": "bar"
                }
            }
        }
}
# it is generally not a good idea to delete more than 100 at a time due to latency and resource utilization
url_template = '{api_url}/{org}/{app}/{collection}?limit=250'

session = requests.Session()


def check_response_status(response, message='', exit_on_error=True):
    if response.status_code != 200:
        print 'ERROR: ' + message
        print response.text

        if exit_on_error:
            exit()


def delete_all_collections(org, app, token):
    url = '{api_url}/{org}/{app}'.format(api_url=api_url, org=org, app=app)

    print 'Listing collections at URL: %s' % url

    r = session.get(url)

    if r.status_code != 200:
        print r.text

    collections = []

    delete_collections(org, app, collections, token)


def delete_collections(org, app, collections, token):
    print 'Deleting [%s] collections: %s' % (len(collections), collections)

    for collection in collections:
        print 'Deleting collection [%s]...' % collection

        keep_going = True

        count_with_zero = 0

        while keep_going:

            url = url_template.format(api_url=api_url, org=org, app=app, collection=collection)

            try:
                response = session.get(url)
                check_response_status(response, message='Unable to GET URL: %s' % url)

                count = len(response.json().get('entities'))
                total_ms = total_milliseconds(response.elapsed)

                print 'GET %s from collection %s in %s' % (count, collection, total_ms)
                print 'Deleting...'

                response = session.delete(url)

                check_response_status(response, message='UNABLE TO DELETE on URL: %s' % url)

                try:
                    count = len(response.json().get('entities'))
                    total_ms = total_milliseconds(response.elapsed)

                    print 'Deleted %s from collection %s in %s' % (count, collection, total_ms)

                    if count == 0:
                        count_with_zero += 1
                        print 'Count with ZERO: %s' % count_with_zero

                        # if there are 10 in a row with zero entities returned, we're done
                        if count_with_zero >= 10:
                            keep_going = False
                    else:
                        count_with_zero = 0
                except:
                    print 'Error! HTTP Status: %s response: %s' % (response.status_code, response.text)

            except KeyboardInterrupt:
                exit()

            except:
                print traceback.format_exc()


# iterate the orgs specified in the configuration above
for org, org_data in data_map.get('orgs', {}).iteritems():

    credentials = org_data.get('credentials', {})

    token_request = {
        'grant_type': 'client_credentials',
        'client_id': credentials.get('client_id'),
        'client_secret': credentials.get('client_secret'),
    }

    token_url = '{api_url}/management/token'.format(api_url=api_url)

    r = session.post(token_url, data=json.dumps(token_request))

    check_response_status(r, message='Unable to get Token at URL %s' % token_url)

    token = r.json().get('access_token')
    session.headers.update({'Authorization': 'Bearer ' + token})

    # iterate the apps specified in the config above
    for app, app_data in org_data.get('apps', {}).iteritems():

        collections = app_data.get('collections', [])

        # if the list of collections is empty, delete all collections
        if len(collections) == 0:
            delete_all_collections(org, app, token)

        # Otherwise, delete the specified collections
        else:
            delete_collections(org, app, collections, token)
