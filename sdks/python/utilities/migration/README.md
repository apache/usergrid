# Usergrid Data Migrator

## Prerequisites

Install the Usergrid Python SDK: https://github.com/jwest-apigee/usergrid-python

## Overview
The purpose of this document is to provide an overview of the Python Script provided in the same directory which allows you to migrate data, connections and users from one Usergrid platform / org / app to another.  This can be used in the upgrade process from Usergrid 1.0 to 2.x since there is no upgrade path.

This script functions by taking source and target endpoint configurations (with credentials) and a set of command-line parameters to read data from one Usergrid instance and write to another.  It is written in Python and requires Python 2.7.6+.

There are multiple processes at work in the migration to speed the process up.  There is a main thread which reads entities from the API and then publishes the entities with metadata into a Python Queue which has multiple worker processes listening for work.  The number of worker threads is configurable by command line parameters.


# Process to Migrate Data and Connections
Usergrid is a Graph database and allows for connections between entities.  In order for a connection to be made, both the source entity and the target entity must exist.  Therefore, in order to migrate connections it is adviseable to first migrate all the data and then all the connections associated with that data.


# Concepts
As with any migration process there is a source and a target.  The source and target have the following parameters:

* API URL: The HTTP[S] URL where the platform can be reached
* Org: You must specify one org at a time to migrate using this script
* App: You can specify one or more applications to migrate.  If you specify zero applications then all applications will be migrated
* Collection: You can specify one or more collections to migrate.  If you specify zero collections then all applications will be migrated
* QL: You can specify a Query Language predicate to be used.  If none is specified, 'select *' will be used which will migrate all data within a given collection

# Mapping
Using this script it is not necessary to keep the same application name, org name and/or collection name as the source at the target.  For example, you could migrate from /myOrg/myApp/myCollection to /org123/app456/collections789.  



# Configuration Files
Example source/target configuration files:

```
{
  "endpoint": {
    "api_url": "https://api.usergrid.com",
    "limit": 100
  },
  "credentials": {
    "myOrg": {
      "client_id": "foo",
      "client_secret": "bar"
    }
  }
}
```
* api_url: the API URL to access/write data
* limit: Specify the limit of pages to retrieve
* Credentials:
** client ID/Secret for each org inlcuded

# Command Line Parameters

```
Usergrid Org/App Migrator

optional arguments:
  -h, --help            show this help message and exit
  -o ORG, --org ORG     Name of the org to migrate
  -a APP, --app APP     Name of one or more apps to include, specify none to
                        include all apps
  -c COLLECTION, --collection COLLECTION
                        Name of one or more collections to include, specify
                        none to include all collections
  -m {data,connections,credentials,none}, --migrate {data,connections,credentials,none}
                        Specifies what to migrate: data, connections,
                        credentials or none (just iterate the
                        apps/collections)
  -s SOURCE_CONFIG, --source_config SOURCE_CONFIG
                        The path to the source endpoint/org configuration file
  -d TARGET_CONFIG, --target_config TARGET_CONFIG
                        The path to the target endpoint/org configuration file
  -w WORKERS, --workers WORKERS
                        The number of worker processes to do the migration
  --ql QL               The QL to use in the filter for reading data from
                        collections
  --su_username SU_USERNAME
                        Superuser username
  --su_password SU_PASSWORD
                        Superuser Password
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

Simple example to migrate all DATA in all apps from an org named 'myorg' from one endpoint to another:

```
➜  ~  python usergrid_data_migrator.py -o myorg -m data -w 4 -s mySourceConfig.json -d myTargetConfiguration.json
```

Simple example to migrate all CONNECTIONS in all apps from an org named 'myorg' from one endpoint to another:

```
➜  ~  python usergrid_data_migrator.py -o myorg -m connections -w 4 -s mySourceConfig.json -d myTargetConfiguration.json
```


This command:

```
➜  ~  python usergrid_data_migrator.py -o myorg -a app1 -a app2 -m data -w 4 --map_app app1:app_1 --map_app app2:app_2 --map_collection pets:animals --map_org myorg:my_new_org -s mySourceConfig.json -d myTargetConfiguration.json
```
will do the following: 

* migrate Apps named 'app1' and 'app2' in org named 'myorg' from the API endpoint defined in 'mySourceConfig.json' to the API endpoint defined in 'myTargetConfiguration.json'
* In the process:
** data from 'myorg' will ge migrated to the org named 'my_new_org'
** data from 'app1' will be migrated to the app named 'app_1'
** data from 'app2' will be migrated to the app named 'app_2'
** all collections named 'pets' will be overridden at the destination to 'animals'


# FAQ

### Does the process keep the same UUIDs?

* Yes - with this script the same UUIDs can be kept from the source into the destination.  An exception is if you specify going from one collection to another under the same Org hierarchy.

### Does the process keep the ordering of connections by time?

* Yes ordering of connections is maintained in the process. 