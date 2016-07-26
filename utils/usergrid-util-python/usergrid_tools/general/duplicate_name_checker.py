from usergrid import UsergridQueryIterator

### This iterates a collection using GRAPH and checks whether there are more than on entity with the same name

url = 'https://host/org/app/collection?access_token=foo&limit=1000'

q = UsergridQueryIterator(url)

name_tracker = {}
counter = 0
for e in q:
    counter += 1

    if counter % 1000 == 1:
        print 'Count: %s' % counter

    name = e.get('name')

    if name in name_tracker:
        name_tracker[name].append(e.get('uuid'))

        print 'duplicates for name=[%s]: %s' % (name, name_tracker[name])

    else:
        name_tracker[name] = [e.get('uuid')]
