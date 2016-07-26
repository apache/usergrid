from multiprocessing import Pool
import requests
import time

__author__ = 'Jeff West @ ApigeeCorporation'

# utility for updating the replicas of a set of indexes that are no longer needed.  Given:
# A) a set of strings to include when evaluating the index names to update
# B) a set of strings to Exclude when evaluating the index names to update
#
# The general logic is:
# 1) If the include set is empty, or if the index name contains a string in the 'include' set, then update
# 2) If the index contains a string in the exclude list, do not update


url_base = 'http://localhost:9200'

# r = requests.get(url_base + "/_cat/indices?v")
# print r.text

r = requests.get(url_base + "/_stats")

# print json.dumps(r.json(), indent=2)

indices = r.json()['indices']

print 'retrieved %s indices' % len(indices)

NUMBER_VALUE = 1

payload = {
    "index.number_of_replicas": NUMBER_VALUE,
}

# indices = ['usergrid__a34ad389-b626-11e4-848f-06b49118d7d0__application_manual']

includes = [
    # '70be096e-c2e1-11e4-8a55-12b4f5e28868',
    # 'b0c640af-bc6c-11e4-b078-12b4f5e28868',
    # 'e62e465e-bccc-11e4-b078-12b4f5e28868',
    # 'd82b6413-bccc-11e4-b078-12b4f5e28868',
    # '45914256-c27f-11e4-8a55-12b4f5e28868',
    # '2776a776-c27f-11e4-8a55-12b4f5e28868',
    # 'a54f878c-bc6c-11e4-b044-0e4cd56e19cd',
    # 'ed5b47ea-bccc-11e4-b078-12b4f5e28868',
    # 'bd4874ab-bccc-11e4-b044-0e4cd56e19cd',
    # '3d748996-c27f-11e4-8a55-12b4f5e28868',
    # '1daab807-c27f-11e4-8a55-12b4f5e28868',
    # 'd0c4f0da-d961-11e4-849d-12b4f5e28868',
    # '93e756ac-bc4e-11e4-92ae-12b4f5e28868',
]

excludes = [
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c8',
    # 'b6768a08-b5d5-11e3-a495-10ddb1de66c3',
    # 'b6768a08-b5d5-11e3-a495-11ddb1de66c9',
    # 'a34ad389-b626-11e4-848f-06b49118d7d0'
]

counter = 0
update = False
# print 'sleeping 1200s'
# time.sleep(1200)

index_names = sorted([index for index in indices])


def update_shards(index_name):
    update = False
    # counter += 1
    #
    # print 'index %s of %s' % (counter, len(indices))

    if len(includes) == 0:
        update = True
    else:
        for include in includes:

            if include in index_name:
                update = True

    if len(excludes) > 0:
        for exclude in excludes:
            if exclude in index_name:
                update = False

    if update:
        print index_name

        # url = '%s/%s/_settings' % (url_base, index)
        # print url
        #
        # response = requests.get('%s/%s/_settings' % (url_base, index))
        # settings = response.json()
        #
        # index_settings = settings[index]['settings']['index']
        #
        # current_replicas = int(index_settings.get('number_of_replicas'))
        #
        # if current_replicas == NUMBER_VALUE:
        #     continue

        success = False

        while not success:

            response = requests.put('%s/%s/_settings' % (url_base, index_name), data=payload)

            if response.status_code == 200:
                success = True
                print '200: %s: %s' % (index_name, response.text)
            else:
                print '%s: %s: %s' % (response.status_code, index_name, response.text)


pool = Pool(8)

pool.map(update_shards, index_names)
