# Entities

## Creating Custom Data Entities
This article describes how to create custom data entities and entity properties. Entity types correspond to the name of collection to which they are posted. For example, if you create a new custom "dog" entity, a "dogs" collection will be created if one did not already exist. If a "dogs" collection already exists, the new "dog" entity will be saved in it. All user-defined properties are indexed, and strings that contain multiple words are keyword-indexed.

The methods cited in this article should be used to create custom data entities. If you are using one of the Usergrid SDKs, use one of the entity type-specific SDK methods to create default data entities.

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Creating a custom entity
When a new entity is created, Usergrid will automatically create a corresponding collection if one does not already exist. The collection will automatically be named with the plural form of the entity type. For example, creating a custom entity of type 'item' will automatically create a collection named 'items' in which all future 'item' entities will be saved.

### Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/<entity_type> -d 'json_object'

Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
entity_type	Entity type to create. 
json_object JSON representation of entity properties

(For a full list of default properties, see Default Data Entity Types)

Usergrid will create a corresponding collection if one does not already exist. To add an entity to an existing collections, use the pluralized collection name for entity_type.

### Example Request/Response

Request:

    curl -X POST "https://api.usergrid.com/your-org/your-app/item" -d '{"name":"milk", "price":"3.25"}'
    
Response:

    { 
        "action" : "post", 
        "application" : "4a1edb70-d7a8-11e2-9ce3-f315e5aa568a", 
        "params" : { }, 
        "path" : "/items", "uri" : "http://api.usergrid.com/my-org/my-app/items", 
        "entities" : [ { 
            "uuid" : "83e9b7ea-e8f5-11e2-84df-e94123890c7a", 
            "type" : "item", 
            "name" : "milk", 
            "created" : 1373415195230, 
            "modified" : 1373415195230, 
            "metadata" : { 

                "path" : "/items/83e9b7ea-e8f5-11e2-84df-e94123890c7a" 
            }, 
            "name" : "milk", 
            "price" : "3.25"
        } ], 
        "timestamp" : 1373415195225, 
        "duration" : 635, 
        "organization" : "my-org", 
        "applicationName" : "my-app" 
    }
    
## Creating multiple custom entities

### Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/<entity_type>/ -d 'json_array'
    
Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or name
app	        Application UUID or name
entity_type	Custom entity type to create. 
json_array  JSON array of entities to be created.

Usergrid will create a corresponding collection if one does not already exist. To add an entity to an existing collections, use the collection name or collection UUID in place of the entity type.

### Example Request/Response

Request:

    curl -X POST "https://api.usergrid.com/your-org/your-app/item" -d '[{"name":"milk", "price":"3.25"}, {"name":"bread", "price":"2.50"}]'
    
Response:

    {
        "action" : "post",
        "application" : "f34f4222-a166-11e2-a7f7-02e9sjwsf3d0",
        "params" : { },
        "path" : "/items",
        "uri" : "http://api.usergrid.com/your-org/your-app/items",
        "entities" : [ {
            "uuid" : "f3a8061a-ef0b-11e2-9e92-5f4a65c16193",
            "type" : "item",
            "name" : "milk",
            "price" : "3.25",
            "created" : 1374084538609,
            "modified" : 1374084538609,
            "metadata" : {
                "path" : "/multis/f3a8061a-ef0b-11e2-9e92-5f4a65c16193"
            },
            "name" : "milk"
        }, {
            "uuid" : "f3be262a-ef0b-11e2-a51b-6715d5ef47a6",
            "type" : "item",
            "name" : "bread",
            "price" : "2.50",
            "created" : 1374084538754,
            "modified" : 1374084538754,
            "metadata" : {
                "path" : "/items/f3be262a-ef0b-11e2-a51b-6715d5ef47a6"
            },
            "name" : "bread"
        } ],
        "timestamp" : 1374084538584,
        "duration" : 388,
        "organization" : "your-org",
        "applicationName" : "your-app"
    }

### Creating an entity with sub-properties

Any valid JSON object can be stored in an entity, regardless of the level of complexity, including sub-properties. For example, suppose you have an 'items' collection that contains an entity named 'milk'. You might store the different varieties of milk as sub-properties of a 'varieties' property:

    {
        "type" : "item"
        "name" : "milk"
        "varieties" : [ {
            "name" : "1%",
            "price" : "3.25",
            "sku" : "0393847575533445"
        }, {
            "name" : "whole",
            "price" : "3.85",
            "sku" : "0393394956788445"
        }, {
            "name" : "skim",
            "price" : "4.00",
            "sku" : "0390299933488445"		
        } ]
    }		
	
### Updating sub-properties

An array of sub-properties is treated as a single object. This means that sub-properties cannot be updated atomically. All sub-properties of a given property must be updated as a set.
For more on updating an existing sub-property, see [Updating Data Entities](../data-storage/entities.html#updating-data-entities).

Example Request/Response

    Request:
    //Note the use of square brackets for specifying multiple nested objects
    curl -X POST "https://api.usergrid.com/your-org/your-app/items" -d '{"varieties":[{"name":"1%","price" : "3.25", "sku" : "0393847575533445"},{"name" : "whole", "price" : "3.85", "sku" : "0393394956788445"}, {"name" : "skim", "price" : "4.00", "sku" : "0390299933488445"}]}'
    Response:
    { 
        "action" : "post", 
        "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0", 
        "params" : { }, 
        "path" : "/items", 
        "uri" : "http://api.usergrid.com/your-org/your-app/items", 
        "entities" : [ { 
            "uuid" : "0d7cf92a-effb-11e2-917d-c5e707256e71", 
            "type" : "item", 
            "name" : "milk", 
            "created" : 1374187231666, 
            "modified" : 1374187231666, 
            "metadata" : { 
                "path" : "/items/0d7cf92a-effb-11e2-917d-c5e707256e71" 
            }, 
            "name" : "milk", 
            "varieties" : [ { 
                "name" : "1%", 
                "price" : "3.25", 
                "SKU" : "0393847575533445" 
            }, { 
                "name" : "whole", 
                "price" : "3.85", 
                "SKU" : "0393394956788445" 
            }, { 
                "name" : "skim", 
                "price" : "4.00", 
                "SKU" : "0390299933488445" 
            } ] 
        } ], 
        "timestamp" : 1374187450826, 
        "duration" : 50, 
        "organization" : "your-org", 
        "applicationName" : "your-app" 
    }

.. -----------------------------------------------------------------------------

## Retrieving Data Entities
This article describes how to retrieve entities from your account.

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

With the exception of the user entity, all data entities can be retrieved by using their UUID or a 'name' property. The user entity can be retrieved by UUID or the 'username' property. The value for the 'name' or 'username' property must be unique.

### Retrieving an entity

### Request Syntax

    curl -X GET https://api.usergrid.com/<org>/<app>/<collection>/<entity>

Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
collection	Collection UUID or collection name
entity	    Entity UUID or entity name

### Example Request/Response

Request:

Retrieve by UUID:

    curl -X GET "https://api.usergrid.com/your-org/your-app/items/da4a50dc-38dc-11e2-b2e4-02e81adcf3d0"
    
Retrieve by 'name' property:

    curl -X GET "https://api.usergrid.com/your-org/your-app/items/milk"
    
Response:

    {
        "action" : "get",
        "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
        "params" : { },
        "path" : "/items",
        "uri" : "http://api.usergrid.com/amuramoto/sandbox/items",
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
        } ],
        "timestamp" : 1378405025763,
        "duration" : 31,
        "organization" : "amuramoto",
        "applicationName" : "sandbox"
    }

### Retrieving multiple entities

This example describes how to retrieve multiple entities by UUID. You can also retrieve a set of entities by using a query string. For more information on querying your data, see Querying your data.

### Request Syntax

    curl -X GET https://api.usergrid.com/<org_id>/<app_id>/<collection>?ql= uuid = <entity_uuid> OR uuid = <entity_uuid>; ...
    
Parameters

Parameter	Description
---------   -----------
org_id	    Organization UUID or organization name
app_id	    Application UUID or application name
collection	Collection UUID or collection name
query	    A url-encoded query string of entity properties to be matched.
 
The query must be in Usergrid Query Language, for example:

    ?ql=uuid="<entity_uuid>"" OR name="<entity_name>" OR...
    
You may also specify the following for certain entity types:

User entities: username = <entity_username>

All other entities except groups: name = <entity_name>

### Example Request/Response

Request:

    //note the url-encoded query string
    curl -X GET "https://api.usergrid.com/your-org/your-app/items?ql=name%3D'milk'%20OR%20UUID%3D1a9356ba-1682-11e3-a72a-81581bbaf055&limit="				

Note: The query parameter of the request must be url encoded for curl requests

Response:

    {
          "action" : "get",
          "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
          "params" : {
                "ql" : [ "name='milk' OR UUID=1a9356ba-1682-11e3-a72a-81581bbaf055" ]
          },
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
          "timestamp" : 1378423793729,
          "duration" : 63,
          "organization" : "your-org",
          "applicationName" : "your-app",
          "count" : 2
    }
 
 
.. --------------------------------------------------------------------------------

## Updating Data Entities

This article describes how to update entities in your account.

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Updating an entity
One or more properties can be updated with a single PUT request. For information on updating sub-properties, see Updating sub-properties below.

### Request Syntax

    curl -X PUT https://api.usergrid.com/<org>/<app>/<collection>/<entity> -d {<property>}
    
Parameters

Parameter	Description
---------   -----------
org_id	    Organization UUID or organization name
app_id	    Application UUID or application name
collection	Name of the collection containing the entity to be updated
uuid|name	UUID or name of the data entity to be updated
json_object JSON object with a field for each property to be updated

An entity property to be updated, formatted as a key-value pair. For example:

    {"property_1":"value_1", "property_2":"value_2",...}

### Example Request/Response

Request:

    curl -X PUT https://api.usergrid.com/your-org/your-app/items/milk -d '{"price":"4.00", "availability":"in-stock"}'

Response:

    {
      "action" : "put",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/items",
      "uri" : "http://api.usergrid.com/your-org/your-app/items",
      "entities" : [ {
        "uuid" : "5bb76bca-1657-11e3-903f-9ff6c621a7a4",
        "type" : "item",
        "name" : "milk",
        "created" : 1378405020796,
        "modified" : 1378505705077,
        "availability" : "in-stock",
        "metadata" : {
          "path" : "/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"
        },
        "name" : "milk",
        "price" : "4.00"
      } ],
      "timestamp" : 1378505705050,
      "duration" : 87,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }


### Updating a sub-property

Data entities may contain sets of sub-properties as nested JSON objects. Unlike normal entity properties, however, sub-properties cannot be updated individually. Updating a nested object will cause all sub-properties within it to be overwritten.

For example, if you have a data entity with the following nested object:

### Request Syntax

    curl -X PUT https://api.usergrid.com/<org_id>/<app_id>/<collection>/<uuid|name> -d '{<property> : [{<sub_property>}, {<sub_property>}...]}'

Parameters

Parameter	Description
---------   -----------
org_id	    Organization UUID or organization name
app_id	    Application UUID or application name
collection	Name of the collection containing the entity to be updated
uuid|name	UUID or name of the data entity to be updated
json_object JSON object with a field for each property to be updated

### Example Request/Response

Request:

    curl -X PUT https://api.usergrid.com/your-org/your-app/items/milk -d '{"varieties":[{"name":"1%","price":"3.25"},{"name":"whole","price":"4.00"}]}'

Response:

    {
      "action" : "put",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/items",
      "uri" : "http://api.usergrid.com/your-org/your-app/items",
      "entities" : [ {
        "uuid" : "5bb76bca-1657-11e3-903f-9ff6c621a7a4",
        "type" : "item",
        "name" : "milk",
        "created" : 1378405020796,
        "modified" : 1378761459069,
        "availability" : "in-stock",
        "metadata" : {
          "path" : "/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"
        },
        "name" : "milk",
        "uri" : "http://api.usergrid.com/your-org/your-app/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4",
        "varieties" : [ {
          "name" : "1%",
          "price" : "3.25"
        }, {
          "name" : "whole",
          "price" : "4.00"
        } ]
      } ],
      "timestamp" : 1378761459047,
      "duration" : 62,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
 
## Deleting Data Entities

This article describes how to delete data entities.

__Note__:Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Deleting an entity

### Request Syntax

    curl -X DELETE https://api.usergrid.com/<org>/<app>/<collection>/<entity>
    
Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
collection	Collection UUID or collection name
entity	    Entity UUID or entity name

### Example Request/Response
 
Request:
 
Delete by UUID:
 
    curl -X DELETE "https://api.usergrid.com/your-org/your-app/items/da4a50dc-38dc-11e2-b2e4-02e81adcf3d0" 
    
Delete by 'name' property:
 
     curl -X DELETE "https://api.usergrid.com/your-org/your-app/items/milk"
 
Response:
 
     {
       "action" : "delete",
       "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
       "params" : { },
       "path" : "/items",
       "uri" : "http://api.usergrid.com/your-org/your-app/items",
       "entities" : [ {
         "uuid" : "328fe64a-19a0-11e3-8a2a-ebc6f49d1fc4",
         "type" : "item",
         "name" : "milk",
         "created" : 1378766158500,
         "modified" : 1378766158500,
         "metadata" : {
           "path" : "/items/328fe64a-19a0-11e3-8a2a-ebc6f49d1fc4"
         },
         "name" : "milk",
         "price" : "3.25"
       } ],
       "timestamp" : 1378766172016,
       "duration" : 324,
       "organization" : "your-org",
       "applicationName" : "your-app"
     }
  
 