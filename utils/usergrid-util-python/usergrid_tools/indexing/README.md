# Usergrid Indexing Latency Tester


# Overview

Indexing of data (to Elasticsearch) in Usergrid is done asynchronously, while persistence (to Cassandra) is done synchronously within the context of an API call.  This means that you can immediately get your data back by UUID but if you use `GET /org/app/collection?ql=select * where field='value'` it is not instantly indexed.  The typical delay is ~25ms.

The purpose of this tool is to test the indexing latency within Usergrid.

```
$ usergrid_index_test -h

usage: usergrid_index_test [-h] -o ORG -a APP --base_url BASE_URL

Usergrid Indexing Latency Test

optional arguments:
  -h, --help           show this help message and exit
  -o ORG, --org ORG    Name of the org to perform the test in
  -a APP, --app APP    Name of the app to perform the test in
  --base_url BASE_URL  The URL of the Usergrid Instance
```