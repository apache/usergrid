# Usergrid Data Migrator

## Prerequisites
* Python 2 (not python 3)

* Install the Usergrid Python SDK: https://github.com/jwest-apigee/usergrid-python

With Pip (requires python-pip to be installed): `pip install usergrid`

* Install Usergrid Tools

With Pip (requires python-pip to be installed): `pip install usergrid-tools`


## Overview
The purpose of this document is to provide an overview of the Python Script provided in the same directory which allows you to migrate data, connections and users from one Usergrid platform / org / app to another.  This can be used in the upgrade process from Usergrid 1.0 to 2.x since there is no upgrade path.

This script functions by taking source and target endpoint configurations (with credentials) and a set of command-line parameters to read data from one Usergrid instance and write to another.  It is written in Python and requires Python 2.7.6+.

There are multiple processes at work in the migration to speed the process up.  There is a main thread which reads entities from the API and then publishes the entities with metadata into a Python Queue which has multiple worker processes listening for work.  The number of worker threads is configurable by command line parameters.


# Process to Migrate Data and Graph (Connections)
Usergrid is a Graph database and allows for connections between entities.  In order for a connection to be made, both the source entity and the target entity must exist.  Therefore, in order to migrate connections it is adviseable to first migrate all the data and then all the connections associated with that data.

# Concepts
As with any migration process there is a source and a target.  The source and target have the following parameters:

* API URL: The HTTP[S] URL where the platform can be reached
* Org: You must specify one org at a time to migrate using this script
* App: You can optinally specify one or more applications to migrate.  If you specify zero applications then all applications will be migrated
* Collection: You can optionally specify one or more collections to migrate.  If you specify zero collections then all applications will be migrated
* QL: You can specify a Query Language predicate to be used.  If none is specified, 'select *' will be used which will migrate all data within a given collection
* Graph: Graph implies traversal of graph edges which necessarily must exist.  This is an alternative to using query which uses the indexing.  

# Graph Loops

When iterating a graph it is possible to get stuck in a loop.  For example:

```
A --follows--> B
B --likes--> C
C --loves--> A
```

There are two options to prevent getting stuck in a loop:
* `graph_depth` option - this will limit the graph depth which will be traversed from a given entity.
* And/Or Marking nodes and edges as 'visited'.  This requires a place to store this state.  See Using Redis in the next section

# Using Redis 

Redis can be used for the following:

If using Redis, version 2.8+ is needed because TTL is used with the 'ex' parameter.

* Keeping track of the modified date for each entity.  When running the script subsequent times after this, entiites which were not modified will not be copied.
* Keeping track of visited nodes for migrating a graph.  This is done with a TTL such that a job can be resumed, but since there is no modified date on an edge you cannot know if there are new edges or not.  Therefore, when the TTL expires the nodes will be visited again
* Keeping track of the URLs for the connections which are created between entities.  This has no TTL.  Subsequent runs will not create connections which are found in Redis which have already been created.


# Mapping
Using this script it is not necessary to keep the same application name, org name and/or collection name as the source at the target.  For example, you could migrate from /myOrg/myApp/myCollection to /org123/app456/collections789.  


# Configuration Files
Example source/target configuration files:

```
{
  "endpoint": {
    "api_url": "https://api.usergrid.com"
  },
  "credentials": {
    "myOrg1": {
      "client_id": "YXA6lpg9sEaaEeONxG0g3Uz44Q",
      "client_secret": "ZdF66u2h3Hc7csOcsEtgewmxalB1Ygg"
    },
    "myOrg2": {
      "client_id": "ZXf63p239sDaaEeONSG0g3Uz44Z",
      "client_secret": "ZdF66u2h3Hc7csOcsEtgewmxajsadfj32"
    }
  }
}
```
* api_url: the API URL to access/write data
* Credentials:
 * For each org, with the org name (case-sensetive) as the key:
  * client_id - the org-level Client ID. This can be retrieved from the BaaS/Usergrid Portal.
  * client_secret - the org-level Client Secret. This can be retrieved from the BaaS/Usergrid Portal.

# Command Line Parameters

```
Usergrid Org/App Data Migrator

optional arguments:
  -h, --help            show this help message and exit
  --log_dir LOG_DIR     path to the place where logs will be written
  --log_level LOG_LEVEL
                        log level - DEBUG, INFO, WARN, ERROR, CRITICAL
  -o ORG, --org ORG     Name of the org to migrate
  -a APP, --app APP     Name of one or more apps to include, specify none to
                        include all apps
  -e INCLUDE_EDGE, --include_edge INCLUDE_EDGE
                        Name of one or more edges/connection types to INCLUDE,
                        specify none to include all edges
  --exclude_edge EXCLUDE_EDGE
                        Name of one or more edges/connection types to EXCLUDE,
                        specify none to include all edges
  --exclude_collection EXCLUDE_COLLECTION
                        Name of one or more collections to EXCLUDE, specify
                        none to include all collections
  -c COLLECTION, --collection COLLECTION
                        Name of one or more collections to include, specify
                        none to include all collections
  --use_name_for_collection USE_NAME_FOR_COLLECTION
                        Name of one or more collections to use [name] instead
                        of [uuid] for creating entities and edges
  -m {data,none,reput,credentials,graph}, --migrate {data,none,reput,credentials,graph}
                        Specifies what to migrate: data, connections,
                        credentials, audit or none (just iterate the
                        apps/collections)
  -s SOURCE_CONFIG, --source_config SOURCE_CONFIG
                        The path to the source endpoint/org configuration file
  -d TARGET_CONFIG, --target_config TARGET_CONFIG
                        The path to the target endpoint/org configuration file
  --limit LIMIT         The number of entities to return per query request
  -w ENTITY_WORKERS, --entity_workers ENTITY_WORKERS
                        The number of worker processes to do the migration
  --visit_cache_ttl VISIT_CACHE_TTL
                        The TTL of the cache of visiting nodes in the graph
                        for connections
  --error_retry_sleep ERROR_RETRY_SLEEP
                        The number of seconds to wait between retrieving after
                        an error
  --page_sleep_time PAGE_SLEEP_TIME
                        The number of seconds to wait between retrieving pages
                        from the UsergridQueryIterator
  --entity_sleep_time ENTITY_SLEEP_TIME
                        The number of seconds to wait between retrieving pages
                        from the UsergridQueryIterator
  --collection_workers COLLECTION_WORKERS
                        The number of worker processes to do the migration
  --queue_size_max QUEUE_SIZE_MAX
                        The max size of entities to allow in the queue
  --graph_depth GRAPH_DEPTH
                        The graph depth to traverse to copy
  --queue_watermark_high QUEUE_WATERMARK_HIGH
                        The point at which publishing to the queue will PAUSE
                        until it is at or below low watermark
  --min_modified MIN_MODIFIED
                        Break when encountering a modified date before this,
                        per collection
  --max_modified MAX_MODIFIED
                        Break when encountering a modified date after this,
                        per collection
  --queue_watermark_low QUEUE_WATERMARK_LOW
                        The point at which publishing to the queue will RESUME
                        after it has reached the high watermark
  --ql QL               The QL to use in the filter for reading data from
                        collections
  --skip_cache_read     Skip reading the cache (modified timestamps and graph
                        edges)
  --skip_cache_write    Skip updating the cache with modified timestamps of
                        entities and graph edges
  --create_apps         Create apps at the target if they do not exist
  --nohup               specifies not to use stdout for logging
  --graph               Use GRAPH instead of Query
  --su_username SU_USERNAME
                        Superuser username
  --su_password SU_PASSWORD
                        Superuser Password
  --inbound_connections
                        Name of the org to migrate
  --map_app MAP_APP     Multiple allowed: A colon-separated string such as
                        'apples:oranges' which indicates to put data from the
                        app named 'apples' from the source endpoint into app
                        named 'oranges' in the target endpoint
  --map_collection MAP_COLLECTION
                        One or more colon-separated string such as 'cats:dogs'
                        which indicates to put data from collections named
                        'cats' from the source endpoint into a collection
                        named 'dogs' in the target endpoint, applicable
                        globally to all apps
  --map_org MAP_ORG     One or more colon-separated strings such as 'red:blue'
                        which indicates to put data from org named 'red' from
                        the source endpoint into a collection named 'blue' in
                        the target endpoint
```

## Example Command Line

Use the following command to migrate DATA AND GRAPH  (no graph edges or connections between entities).  If there are no graph edges (connections) then using `-m graph` is not necessary.  This will copy all data from all apps in the org 'myorg', creating apps in the target org if they do not already exist.  Note that --create_apps will be required if the Apps in the target org have not been created.

```
$ usergrid_data_migrator -o myorg -m graph -w 4 -s mySourceConfig.json -d myTargetConfiguration.json  --create_apps
```

Use the following command to migrate DATA ONLY (no graph edges or connections between entities).  This will copy all data from all apps in the org 'myorg', creating apps in the target org if they do not already exist.  Note that --create_apps will be required if the Apps in the target org have not been created.

```
$ usergrid_data_migrator -o myorg -m data -w 4 -s mySourceConfig.json -d myTargetConfiguration.json --create_apps
```

Use the following command to migrate CREDENTIALS for Application-level Users.  Note that `usergrid.sysadmin.login.allowed=true` must be set in the `usergrid-deployment.properties` file on the source and target Tomcat nodes.

```
$ usergrid_data_migrator -o myorg -m credentails -w 4 -s mySourceConfig.json -d myTargetConfiguration.json --create_apps --su_username foo --su_password bar
```

This command:

```
$ usergrid_data_migrator -o myorg -a app1 -a app2 -m data -w 4 --map_app app1:appplication_1 --map_app app2:application_2 --map_collection pets:animals --map_org myorg:my_new_org -s mySourceConfig.json -d myTargetConfiguration.json
```
will do the following: 

* migrate Apps named 'app1' and 'app2' in org named 'myorg' from the API endpoint defined in 'mySourceConfig.json' to the API endpoint defined in 'myTargetConfiguration.json'
* In the process:
** data from 'myorg' will ge migrated to the org named 'my_new_org'
** data from 'app1' will be migrated to the app named 'application_1'
** data from 'app2' will be migrated to the app named 'application_2'
** all collections named 'pets' will be overridden at the destination to 'animals'


# FAQ

### Does the process keep the same UUIDs?

* Yes - with this script the same UUIDs can be kept from the source into the destination.  An exception is if you specify going from one collection to another under the same Org hierarchy.

### Does the process keep the ordering of connections by time?

* Yes ordering of connections is maintained in the process. 
