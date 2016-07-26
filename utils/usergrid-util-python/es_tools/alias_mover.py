import json

import requests

example_request = {
    "actions": [
        {
            "remove": {
                "index": "apigee-vfmplus",
                "alias": "rug000sr_euwi_1edb82a0-f23c-11e5-bf51-0aa04517d9d9_read_alias"
            }
        },
        {
            "remove": {
                "index": "apigee-vfmplus",
                "alias": "rug000sr_euwi_1edb82a0-f23c-11e5-bf51-0aa04517d9d9_write_alias"
            }
        },
        {
            "remove": {
                "index": "apigee-vfmplus",
                "alias": "rug000sr_euwi_48e5394a-f1fd-11e5-9fdc-06ae5d93d39b_read_alias"
            }
        },
        {
            "remove": {
                "index": "apigee-vfmplus",
                "alias": "rug000sr_euwi_48e5394a-f1fd-11e5-9fdc-06ae5d93d39b_write_alias"
            }
        },
        {
            "remove": {
                "index": "apigee-vfmplus",
                "alias": "rug000sr_euwi_fd7ef86f-f1fb-11e5-b407-02f0703cf0bf_read_alias"
            }
        },
        {
            "remove": {
                "index": "apigee-vfmplus",
                "alias": "rug000sr_euwi_fd7ef86f-f1fb-11e5-b407-02f0703cf0bf_write_alias"
            }
        },
        {
            "add": {
                "index": "apigee-vmplus-docvalues",
                "alias": "rug000sr_euwi_1edb82a0-f23c-11e5-bf51-0aa04517d9d9_read_alias"
            }
        },
        {
            "add": {
                "index": "apigee-vmplus-docvalues",
                "alias": "rug000sr_euwi_1edb82a0-f23c-11e5-bf51-0aa04517d9d9_write_alias"
            }
        },
        {
            "add": {
                "index": "apigee-vmplus-docvalues",
                "alias": "rug000sr_euwi_48e5394a-f1fd-11e5-9fdc-06ae5d93d39b_read_alias"
            }
        },
        {
            "add": {
                "index": "apigee-vmplus-docvalues",
                "alias": "rug000sr_euwi_48e5394a-f1fd-11e5-9fdc-06ae5d93d39b_write_alias"
            }
        },
        {
            "add": {
                "index": "apigee-vmplus-docvalues",
                "alias": "rug000sr_euwi_fd7ef86f-f1fb-11e5-b407-02f0703cf0bf_read_alias"
            }
        },
        {
            "add": {
                "index": "apigee-vmplus-docvalues",
                "alias": "rug000sr_euwi_fd7ef86f-f1fb-11e5-b407-02f0703cf0bf_write_alias"
            }
        }
    ]
}

cluster = 'rug000sr_euwi'

work = {
    # 'remove': {
    #     '2dd3bf6c-02a5-11e6-8623-069e4448b365': 'rug000sr_euwi_applications_3',
    #     '333af5b3-02a5-11e6-81cb-02fe3195fdff': 'rug000sr_euwi_applications_3',
    # },
    'add': {
        '2dd3bf6c-02a5-11e6-8623-069e4448b365': 'apigee-vfmplus-1-no-doc-18',
        '333af5b3-02a5-11e6-81cb-02fe3195fdff': 'apigee-vfmplus-1-no-doc-18',
    }
}

actions = []

for app_id, index in work.get('remove', {}).iteritems():
    actions.append({
        "remove": {
            "index": index,
            "alias": "%s_%s_read_alias" % (cluster, app_id)
        },
    })
    actions.append({
        "remove": {
            "index": index,
            "alias": "%s_%s_write_alias" % (cluster, app_id)
        },
    })

for app_id, index in work['add'].iteritems():
    actions.append({
        "add": {
            "index": index,
            "alias": "%s_%s_read_alias" % (cluster, app_id)
        },
    })
    actions.append({
        "add": {
            "index": index,
            "alias": "%s_%s_write_alias" % (cluster, app_id)
        },
    })

url = 'http://localhost:9200/_aliases'

r = requests.post(url, data=json.dumps({'actions': actions}))

print '%s: %s' % (r.status_code, r.text)
