# Usergrid Data Importer

## Prerequisites
* Python 2 (not python 3)

* Install the Usergrid Python SDK: https://github.com/apache/usergrid/tree/master/sdks/python
    * with Pip (requires python-pip to be installed on Linux): `pip install usergrid`

* Install Usergrid Tools version 1.0.0 or higher
    * with Pip (requires python-pip to be installed on Linux): `pip install usergrid-tools`

* A Usergrid Org with one or more apps having data you want to export

* The Client ID and Secret of the Usergrid Org you want to export

* An exported dataset using the format of the Usergrid Data Exporter


## Overview
The purpose of this document is to cover the usergrid_importer Python tool. This tool enables you to import data from a filesystem to a Usergrid instance.  It includes both entity data (graph vertexes) and connections (graph edges).  The format must be in the specification below:

  
# How it Works

1) Iterate the import_path parameters specified, which are Directories
2) Read the Org name from the directory name, list the directories below the Org Directory and get App Names
3) For each App name/directory, list the Collection names/directories beneath
4) Capture all file names of all Entity Data Files and Connection Data Files
5) Load all Entity Data for the app - Entities must be loaded before the Connections can be made
    * This process uses a multiprocessing.Pool to process files in parallel.  For a high-throughput loading process, specify a low number of entities per file and a high number of export workers on the import.  This will process more files in parallel.
6) Load all Connection data for the app
    * The same multiprocessing.Pool approach applies as above.

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


# Target Configuration File

The target endpoint and credentials for one or more orgs is required to be placed in a file whose path is specified on the command line when running the script. 

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
Usergrid Data Importer

optional arguments:
  -h, --help            show this help message and exit
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
  --log_dir LOG_DIR     path to the place where logs will be written
  --log_level LOG_LEVEL
                        log level - DEBUG, INFO, WARN, ERROR, CRITICAL
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
  -t TARGET, --target TARGET
                        The path to the source endpoint/org configuration file
  --import_path IMPORT_PATH
                        The path to read the exported data from
  --workers WORKERS     The number of worker processes to do the migration
  --nohup               specifies not to use stdout for logging
```



## Example Command Line

The following command will import the data from /tmp/usergrid-export/3f242833-59c4-11e6-b53b-c8e0eb16c585 to the endpoint specified in the TARGET.json file.  This presumes that data has been exported to /tmp/usergrid-export/3f242833-59c4-11e6-b53b-c8e0eb16c585 from Usergrid before this command is run. 

```
$ usergrid_data_importer --import_path /tmp/usergrid-export/3f242833-59c4-11e6-b53b-c8e0eb16c585 -t ./TARGET.json
```

The following command will import the data from /tmp/usergrid-export/3f242833-59c4-11e6-b53b-c8e0eb16c585 to the endpoint specified in the TARGET.json file, and:
* Filter the collections to 'pets' and 'owners'.  
* Filter the edges to 'loves', only creating those connections
* Use a pool of 16 threads to process up to 16 files in parallel.  Note that this will be most effective with more than 1-2 files.  Increase the number of files generated by using the --entities_per_file parameter of the Export tool 

```
$ usergrid_data_importer --import_path /tmp/usergrid-export/3f242833-59c4-11e6-b53b-c8e0eb16c585 -t ./TARGET.json --collection pets --collection owners --include_edge loves --workers 16
```

The following command will import the data from /tmp/usergrid-export/3f242833-59c4-11e6-b53b-c8e0eb16c585 to the endpoint specified in the TARGET.json file, and:
* Map the org named 'foo' to 'bar'  
* Map the app named 'apples' to 'oranges'
* Map the collection named 'pets' to 'animals'
* Map the collection named 'owners' to 'people'

```
$ usergrid_data_importer --import_path /tmp/usergrid-export/3f242833-59c4-11e6-b53b-c8e0eb16c585 -t ./TARGET.json --map_org foo:bar --map_app apples:oranges --map_collection pets:animals --map_collection owners:people
```

# FAQ

### Question

* Yes - absolutely 
