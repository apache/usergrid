# Collections

## Creating Collections

This article describes how to create collections in Advanced Usergrid. All entities are automatically associated with a corresponding collection based on the type property of the entity. You may create empty collections if you wish, but creating an entity of a new type will automatically create a corresponding collection for you. For example, creating a new custom "item" entity, creates an "items" collection.

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Creating a collection

The following example shows how to create an empty collection. Alternatively, you can create a collection simply by creating a new entity with a 'type' property that corresponds to the collection you wish to create. For more on creating entities, see Creating Custom Data Entities

### Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/<collection_name>

Parameters

Parameter	    Description
---------       -----------
org	            Organization UUID or organization name
app	            Application UUID or application name
collection name	Name of the collection to create. 

If the provided value is not a plural word, Usergrid will pluralize it. For example, providing 'item' will create a collection named 'items' but providing 'items' will not create 'itemses'.

### Example Request/Response

Request:

    curl -X POST "https://api.usergrid.com/your-org/your-app/item"

Response:

    {
      "action" : "post",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/items",
      "uri" : "http://api.usergrid.com/your-org/your-app/items",
      "entities" : [ ],
      "timestamp" : 1378857079220,
      "duration" : 31,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
  
## Retrieving Collections

This article describes how to retrieve all of the entities in a collection.

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> By default, the Usergrid API returns 10 entities per request. For collections with more than 10 entities, use the returned 'cursor' property to retrieve the next 10 entities in the result set. You may also use the LIMIT parameter in a query string to increase the number of results returned. For more information on using cursors, see [Query Parameters](../data-queries/query-parameters.html).</p></div>

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Retrieving sets of entities from a collection

### Request Syntax

    curl -X GET https://api.usergrid.com/<org>/<app>/<collection>
    
Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
collection	Collection UUID or collection name

### Example Request/Response

Request:

    curl -X GET "https://api.usergrid.com/your-org/your-app/items"

Response:

    {
          "action" : "get",
          "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
          "params" : { },
          "path" : "/items",
          "uri" : "http://api.usergrid.com/your-org/your-app/items",
          "entities" : [ {
                "uuid" : "5bb76bca-1657-11e3-903f-9ff6c621a7a4",
                "type" : "item",
                "name" : "milk",
                "created" : 1378405020796,
                "modified" : 1378405020796,
                "metadata" : {
                      "path" : "/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"
                },
                "name" : "milk",
                "price" : "3.25"
          }, {
            "uuid" : "1a9356ba-1682-11e3-a72a-81581bbaf055",
            "type" : "item",
            "name" : "bread",
            "created" : 1378423379867,
            "modified" : 1378423379867,
            "metadata" : {
                  "path" : "/items/1a9356ba-1682-11e3-a72a-81581bbaf055"
            },
            "name" : "bread",
            "price" : "2.50"
          } ],
          "timestamp" : 1378426821261,
          "duration" : 35,
          "organization" : "your-org",
          "applicationName" : "your-app",
          "count" : 2
    }   
    
## Updating Collections

This article describes how to perform batch updates on all entities in a collection. Batch updates require the use of a query string in the request, which can either specify all entities in the collection or a subset of entities for the update to be performed on. For more information on queries, see Querying your data.

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Batch updating entities in a collection

### Request Syntax

    curl -X PUT https://api.usergrid.com/<org>/<app>/<collection>/?ql= -d {<property>}

Note the empty query string (ql=) appended to the URL.

Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
collection	Collection UUID or collection name property	

An entity property to be updated, formatted as a key-value pair. For example:

    {"property_1":"value_1", "property_2":"value_2",...}

### Example Request/Response

Request:

    curl -X PUT https://api.usergrid.com/your-org/your-app/items/?ql= -d '{"availability":"in-stock"}'

Note the empty ?ql= query string.

Response:

    {
      "action" : "put",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : {
        "ql" : [ "" ]
      },
      "path" : "/items",
      "uri" : "http://api.usergrid.com/your-org/your-app/items",
      "entities" : [ {
        "uuid" : "31847b9a-1a62-11e3-be04-8d05e96f700d",
        "type" : "item",
        "name" : "milk",
        "price" : "3.25",
        "availability" : "in-stock"
        "created" : 1378849479113,
        "modified" : 1378849567301,
        "name" : "milk",
      }, {
        "uuid" : "3192ac6a-1a62-11e3-a24f-496ca1d42ce7",
        "type" : "item",
        "name" : "bread",
        "price" : "4.00",
        "availability" : "in-stock"
        "created" : 1378849479206,
        "modified" : 1378849567351,
        "name" : "bread",
      } ],
      "timestamp" : 1378849567280,
      "duration" : 207,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
   
## Deleting Collections
This article describes how to batch delete entities in a collection. Batch deletes require the use of a query string in the request, which specifies a subset of entities to be deleted. For more information on queries, see Querying your data.

Currently, collections cannot be deleted; however, you can delete all of the entities from a collection.

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Batch deleting entities in a collection

### Request Syntax

    curl -X DELETE https://api.usergrid.com/<org>/<app>/<collection>/?ql=<query>
    
Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
collection	Collection UUID or collection name
query	    A query string that specifies the subset of entities to delete 

(for more information on queries, see Querying your data)

### Example Request/Response

The following example will delete the first 5 entities in a collection.

Request:

    curl -X DELETE https://api.usergrid.com/your-org/your-app/items/?ql="limit=5"
    
Response:

    {
      "action" : "delete",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : {
        "ql" : [ "" ]
      },
      "path" : "/items",
      "uri" : "http://api.usergrid.com/your-org/your-app/items",
      "entities" : [ {
        "uuid" : "53fe3700-0abe-11e3-b1f7-1bd100b8059e",
        "type" : "item",
        "name" : "milk",
        "price" : "3.25",
        "created" : 1377129832047,
        "modified" : 1377129832047,
        "metadata" : {
          "path" : "/items/53fe3700-0abe-11e3-b1f7-1bd100b8059e"
        },
        "name" : "milk"
      }, {
        "uuid" : "5ae1fa7a-0abe-11e3-89ab-6be0003c809b",
        "type" : "item",
        "name" : "bread",
        "price" : "4.00",
        "created" : 1377129843607,
        "modified" : 1377129843607,
        "metadata" : {
          "path" : "/items/5ae1fa7a-0abe-11e3-89ab-6be0003c809b"
        },
        "name" : "bread"
      } ],
      "timestamp" : 1378848117272,
      "duration" : 12275,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
    
    