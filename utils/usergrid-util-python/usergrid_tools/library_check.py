import traceback

url_data = {
    "api_url": "https://usergrid-e2e-prod.e2e.apigee.net/appservices-2-1/",
    "org": "",
    "app": "",
    "client_id": "",
    "client_secret": "",

}

collection_url_template = "{api_url}/{org}/{app}/{collection}"

try:
    from usergrid import UsergridQueryIterator

    q = UsergridQueryIterator('')

    print 'Check OK'

except Exception, e:
    print traceback.format_exc(e)
    print 'Check Failed'
