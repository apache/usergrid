# Data Importer for Parse.com Application Data Export

## Overview

This Python script uses the Usergrid Python SDK to iterate a data export from Parse.com to import it into a Usergrid instance

## Usage

```
usage: parse_data_importer.py [-h] -o ORG -a APP --url URL -f FILE --tmp_dir
                              TMP_DIR [--client_id CLIENT_ID]
                              [--client_secret CLIENT_SECRET]

Parse.com Data Importer for Usergrid

optional arguments:
  -h, --help            show this help message and exit
  -o ORG, --org ORG     Name of the org to import data into
  -a APP, --app APP     Name of the app to import data into
  --url URL             The URL of the Usergrid Instance to import data into
  -f FILE, --file FILE  Full or relative path of the data file to import
  --tmp_dir TMP_DIR     Directory where data file will be unzipped
  --client_id CLIENT_ID
                        The Client ID for using OAuth Tokens - necessary if
                        app is secured
  --client_secret CLIENT_SECRET
                        The Client Secret for using OAuth Tokens - necessary
                        if app is secured
```

## Features

Support for:
* Roles -> Users
* Roles -> Roles
* Custom entities
* Joins implemented as Graph Edges with the name of 'joins' - in both directions
* Pointers implemented as Graph Edges with the name of 'pointers' - in both directions on an object

No Support for:
* Products - In-App Purchases
* Installations - Will map to 'Devices' at some point - important for Push Notifications perhaps
* Binary Assets (Images) - Work in Progress to complete

## Graph Edges in Usergrid

Usergrid is a Graph Datastore and implements the concept of a Graph Edge in the form of a 'connection'.  Pointers, when found on an object, are implemented as follows:

Source Entity --[Edge Name]--> Target Entity

This is represented as a URL as follows: /{source_collection}/{source_entity_id}/pointers/{optional:target_type}.  A GET on this URL would return a list of entities which have this graph edge.  If a `{target_type}` is specified the results will be limited to entities of that type. 

Examples: 
* `GET /pets/max/pointers` - get the list of entities of all entity types which have a 'pointers' edge to them from the 'pet' 'max'
* `GET /pets/max/pointers/owners` - get the list of entities of owners which have a 'pointers' edge to them from the 'pet' 'max'
* `GET /pets/max/pointers/owners/jeff` - get the owner 'jeff' which has a 'pointers' edge to them from the 'pet' 'max'

## Pointers

Parse.com has support for pointers from one object to another.  For example, for a Pointer from a Pet to an Owner, the object might look as follows:
 
```
{
  "fields" : "...",
  "objectId": "A7Hdad8HD3",
  "owner": {
      "__type": "Pointer",
      "className": "Owner",
      "objectId": "QC41NHJJlU"
  }
}
```


## Joins
Parse.com has support for the concept of a Join as well.  At the moment, Joining Users and Roles is supported and an attempt has been made to support arbitrary Joins based on the format of the `_Join:users:_Role.json` file found in my exported data.  The from/to types appear to be found in the filename.

An example of the Join file is below:

```
{ "results": [
	{
        "owningId": "lxhMWzbeXa",
        "relatedId": "MCU2Cv9nuk"
    }
] }
```


Joins are implemented as Graph Edges with the name of 'joins' - in both directions from the objects where the Join was found
