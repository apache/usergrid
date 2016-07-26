import redis

r = redis.Redis("localhost", 6379)
for key in r.scan_iter():
    # print '%s: %s' % (r.ttl(key), key)

    if key[0:4] == 'http':
        r.set(key, 1)
        # print 'set value'

    if r.ttl(key) > 3600 \
            or key[0:3] in ['v3:', 'v2', 'v1'] \
            or ':visited' in key:
        r.delete(key)
        print 'delete %s' % key
