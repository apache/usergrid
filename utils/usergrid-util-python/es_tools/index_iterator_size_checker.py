import json
import re
import traceback
from multiprocessing.pool import Pool
import requests

index_url_template = 'http://localhost:9200/{index_name}/_search?size={size}&from={from_var}&q=-edgeName:zzzcollzzz|logs'

index_names = [
    'es-index-name'
]

baas_url = 'http://localhost:8080/org/{app_id}/{collection}/{entity_id}'

field_part_map = {
    'mockdata': 'mockData'
}


def update_entity_field(entity, field_name, field_value):
    entity_copy = entity.copy()

    worked = True
    is_array = False
    array_length = 0

    try:
        parts = field_name.split('.')

        if parts[len(parts) - 1] != 'size':
            print parts
            exit()

        change_me = entity_copy

        for i, field_part in enumerate(parts):
            field_part = field_part_map.get(field_part, field_part)

            if field_part == 'size':
                break

            if isinstance(change_me, dict):
                if field_part not in change_me:
                    worked = False
                    # print 'ERROR!  field [%s] not in entity: %s' % (field_part, json.dumps(change_me))
                    break

                change_me = change_me[field_part]

            elif isinstance(change_me, list):
                array_length = len(change_me)

                if i == len(parts) - 2 and len(parts) > i + 1 and parts[i + 1] == 'size':

                    for j in xrange(0, len(change_me)):
                        print 'arrau!'
                        change_me[j] = update_entity_field(change_me[j], '.'.join(parts[i:]), field_value)
                        # element['size'] = field_value

                elif len(change_me) == 1:
                    print 'single array'
                    change_me = change_me[0][field_part]
                else:
                    print 'WTF!'
        try:
            change_me['size'] = field_value
        except:
            if array_length != 1:
                print traceback.format_exc()
                print 'damn'

    except:
        print '---Error updating field [%s] in document: %s' % (field_name, json.dumps(entity))
        print traceback.format_exc()

    if array_length > 1:
        print '++++++++ARRAY!!!!! %s' % array_length

    return entity_copy


def update_entity_fields(entity, fields):
    entity_copy = entity.copy()

    for field in fields:
        field_name = field.get('name')

        if 'string' in field:
            field_value = field.get('string')

        elif 'long' in field:
            field_value = field.get('long')

        else:
            print 'WTF! %s' % json.dumps(field)
            return entity_copy

        entity_copy = update_entity_field(entity_copy, field_name, field_value)

    return entity_copy


my = {
    'foo': {
        'bar': {
            'color': 'red'
        }
    }
}

fields = [
    {
        'name': 'foo.size',
        'string': '2'
    },
    {
        'name': 'foo.bar.size',
        'long': 2
    }
]


def work(item):
    try:
        url = 'http://localhost:8080/org/{app_id}/{collection}/{entity_id}'.format(
            app_id=item[0],
            collection=item[1],
            entity_id=item[2]
        )
        print url
        r_get = requests.get(url)

        if r_get.status_code != 200:
            print 'ERROR GETTING ENTITY AT URL: %s' % url
            return

        response_json = r_get.json()

        entities = response_json.get('entities')

        if len(entities) <= 0:
            print 'TOO MANY ENTITIES AT URL: %s' % url
            return

        entity = entities[0]

        new_entity = update_entity_fields(entity, item[3])

        with open('/Users/ApigeeCorporation/tmp/hack/%s.json' % item[2], 'w') as f:
            json.dump(entity, f, indent=2)

        with open('/Users/ApigeeCorporation/tmp/hack/%s_new.json' % item[2], 'w') as f:
            json.dump(new_entity, f, indent=2)

            r_put = requests.put(url, data=json.dumps(new_entity))

            if r_put.status_code == 200:
                print 'PUT [%s]: %s' % (r_put.status_code, url)
                pass
            elif r_put.status_code:
                print 'PUT [%s]: %s | %s' % (r_put.status_code, url, r_put.text)

    except:
        print traceback.format_exc()


POOL_SIZE = 4

counter = 0
size = POOL_SIZE * 10
size = 1000

total_docs = 167501577
start_from = 0
from_var = 0
page = 0

work_items = []

pool = Pool(POOL_SIZE)

keep_going = True

while keep_going:
    work_items = []

    if from_var > total_docs:
        keep_going = False
        break

    from_var = start_from + (page * size)
    page += 1

    for index_name in index_names:

        index_url = index_url_template.format(index_name=index_name, size=size, from_var=from_var)

        print 'Getting URL: ' + index_url

        r = requests.get(index_url)

        if r.status_code != 200:
            print r.text
            exit()

        response = r.json()

        hits = response.get('hits', {}).get('hits')

        re_app_id = re.compile('appId\((.+),')
        re_ent_id = re.compile('entityId\((.+),')
        re_type = re.compile('entityId\(.+,(.+)\)')

        print 'Index: %s | hits: %s' % (index_name, len(hits))

        if len(hits) == 0:
            keep_going = False
            break

        for hit_data in hits:
            source = hit_data.get('_source')

            application_id = source.get('applicationId')

            app_id_find = re_app_id.findall(application_id)

            if len(app_id_find) > 0:
                app_id = app_id_find[0]

                if app_id != '5f20f423-f2a8-11e4-a478-12a5923b55dc':
                    print 'SKIPP APP ID: ' + app_id
                    continue

                entity_id_tmp = source.get('entityId')

                entity_id_find = re_ent_id.findall(entity_id_tmp)
                entity_type_find = re_type.findall(entity_id_tmp)

                if len(entity_id_find) > 0 and len(entity_type_find) > 0:
                    entity_id = entity_id_find[0]
                    collection = entity_type_find[0]
                    fields_to_update = []

                    for field in source.get('fields'):
                        if field.get('name')[-5:] == '.size':
                            fields_to_update.append(field)

                            print json.dumps(source)

                            work_items.append((app_id, collection, entity_id, fields_to_update))

                    counter += 1

    print 'Work Items: %s' % len(work_items)

    try:
        pool.map(work, work_items)


    except:
        print traceback.format_exc()

        try:
            pool.map(work, work_items)
        except:
            pass

    print 'Work Done!'

print 'done: %s' % counter
