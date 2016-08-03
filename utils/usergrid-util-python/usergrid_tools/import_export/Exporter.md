# Usergrid Data Exporter

## Prerequisites
* Python 2 (not python 3)

* Install the Usergrid Python SDK: https://github.com/apache/usergrid/tree/master/sdks/python
    * with Pip (requires python-pip to be installed on Linux): `pip install usergrid`

* Install Usergrid Tools version 1.0.0 or higher
    * with Pip (requires python-pip to be installed on Linux): `pip install usergrid-tools`

* A Usergrid Org with one or more apps having data you want to export

* The Client ID and Secret of the Usergrid Org you want to export


## Overview
The purpose of this document is to cover the usergrid_exporter Python tool. This tool enables you to export data from a Usergrid instance to a set of files.  It includes both entity data (graph vertexes) and connections (graph edges).

  
# How it Works

This is a multi-threaded process which exports the data and connections for each collection within one thread/process.

## Export Format:

* usergrid-export
    * ECID (ex: 4eb979c2-59c8-11e6-a66b-c8e0eb16c585) - Execution Context ID - a Type 1 UUID which uniquely identifies one execution of the tool
        * Org Name
            * App Name
                * Collection Name
                    - entities-[0..n].txt - a one-line JSON object per entity
                    - connections-[0..n].txt - a one-line JSON object for each connection type of each entity


# Concepts
The export process implies that there is a source of data which is getting exported.

* API URL: The HTTP[S] URL where the platform can be reached
* Org: You must specify one org at a time to migrate using this script
* App: You can optinally specify one or more applications to migrate.  If you specify zero applications then all applications will be migrated
* Collection: You can optionally specify one or more collections to migrate.  If you specify zero collections then all applications will be migrated
* QL: You can specify a Query Language predicate to be used.  If none is specified, 'select *' will be used which will migrate all data within a given collection
* Graph: Graph implies traversal of graph edges which necessarily must exist.  This is an alternative to using query which uses the index data to iterate entities.  


# Source Configuration File

The source endpoint and credentials for one or more orgs is required to be placed in a file whose path is specified on the command line when running the script. 

Example source configuration files:

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
## Elements:
* api_url: the API URL to access/write data
* Credentials:
 * For each org, with the org name (case-sensetive) as the key:
  * client_id - the org-level Client ID. This can be retrieved from the BaaS/Usergrid Portal.
  * client_secret - the org-level Client Secret. This can be retrieved from the BaaS/Usergrid Portal.

# Command Line Parameters

```
Usergrid Data Exporter

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
  -s SOURCE_CONFIG, --source_config SOURCE_CONFIG
                        The path to the source endpoint/org configuration file
  --export_path EXPORT_PATH
                        The path to save the export files
  --limit LIMIT         The number of entities to return per query request
  --entities_per_file ENTITIES_PER_FILE
                        The number of entities to put in one JSON file
  --error_retry_sleep ERROR_RETRY_SLEEP
                        The number of seconds to wait between retrieving after
                        an error
  --page_sleep_time PAGE_SLEEP_TIME
                        The number of seconds to wait between retrieving pages
                        from the UsergridQueryIterator. The page size would be
                        indicated by the '--limit' command-line parameter. An
                        value of 5 for this parameter would indicate to wait
                        for 5 seconds for every {limit} of entities
  --workers COLLECTION_WORKERS
                        The number of worker processes to do the migration
  --queue_size_max QUEUE_SIZE_MAX
                        The max size of entities to allow in the queue
  --ql QL               The QL to use in the filter for reading data from
                        collections
  --nohup               specifies not to use stdout for logging
  --graph               Use GRAPH instead of Query
```

## Example Command Line

The following command will export a single app from the org named 'myorg' to the current directory

```
$ usergrid_data_exporter -o myorg -s myorg.json --export_path /Users/jwest/tmp
```

The following command will export the collection(s) named 'pets' from each app named 'app1' and 'app2' from the org named 'myorg'.  4 processes will be used to process collections in parallel.  If there are <4 collections then only that number of processes will be active.

```
$ usergrid_data_exporter -o myorg -a app1 -a app2 -w 4 -c pets
```

The following command will:
* export from the 'myorg' org
* all collections except those named 'pets'
* all apps
* using the Index (due to QL)
* all data end connections for entities which were created after Wed, 03 Aug 2016 00:23:17 GMT  

```
$ usergrid_data_exporter -o myorg -s myorg.json --export_path /Users/jwest/tmp --exclude_collection pets --ql select * where created > 1470183797000
```


# FAQ

### Question

* Yes - absolutely 
