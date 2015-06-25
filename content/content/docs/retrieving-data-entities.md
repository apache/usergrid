---
title: Retrieving Data Entities
category: docs
layout: docs
---

Retrieving Data Entities
========================

This article describes how to retrieve entities from your Apache Usergrid
account.

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

With the exception of the `user` entity, all data entities can be
retrieved by using their UUID or a 'name' property. The `user` entity
can be retrieved by UUID or the 'username' property. The value for the
'name' or 'username' property must be unique.

Retrieving an entity
--------------------

-   [cURL](#curl_get_entity)
-   [iOS](#ios_get_entity)
-   [Android](#android_get_entity)
-   [JavaScript (HTML5)](#javascript_get_entity)
-   [Ruby](#ruby_get_entity)
-   [Node.js](#nodejs_get_entity)

### Example Request/Response

#### Request:

Retrieve by UUID:

    curl -X GET "https://api.usergrid.com/your-org/your-app/items/da4a50dc-38dc-11e2-b2e4-02e81adcf3d0"

Retrieve by 'name' property:

    curl -X GET "https://api.usergrid.com/your-org/your-app/items/milk"

#### Response:

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

### Request Syntax

    curl -X GET https://api.usergrid.com/<org>/<app>/<collection>/<entity>

### Parameters

  Parameter    Description
  ------------ ----------------------------------------
  org          Organization UUID or organization name
  app          Application UUID or application name
  collection   Collection UUID or collection name
  entity       Entity UUID or entity name

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)getEntity {

        //specify the entity collection and UUID or name to be retrieved    
        NSString *endpoint = @"items/b3aad0a4-f322-11e2-a9c1-999e12039f87"; 
        
        NSString *query = nil;
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient getEntities:endpoint query:query];
        
        @try {
            //success
        }
        
        @catch (NSException * e) {
            //fail
        }

    }               
                    

#### Response:

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

### SDK Method

    (ApigeeClientResponse *)getEntities: (NSString *)endpoint query:(NSString *)query

### Properties

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| endpoint                             | The collection and entity identifier |
|                                      | of the entity to be retrieved in the |
|                                      | following format:                    |
|                                      |                                      |
|                                      |     <collection>/<entity_UUID_or_nam |
|                                      | e>                                   |
+--------------------------------------+--------------------------------------+
| query                                | An optional query string. Requests   |
|                                      | for a specific entity should set the |
|                                      | value to `nil`                       |
+--------------------------------------+--------------------------------------+

\

This example uses the [Apache Usergrid Android
SDK](/app-services-sdks#android).

### Example Request/Response

#### Request:

    //Create client entity
    String ORGNAME = "your-org";
    String APPNAME = "your-app";        
    ApigeeClient apigeeClient = new ApigeeClient(ORGNAME,APPNAME);
    DataClient dataClient = apigeeClient.getDataClient();

    String type = "item"; //entity type to be retrieved
    String query = "uuid = b3aad0a4-f322-11e2-a9c1-999e12039f87;
        
    //call getEntitiesAsync to initiate the asynchronous API call    
    dataClient.getEntitiesAsync(type, query, new ApiResponseCallback() {    

    //If getEntitiesAsync fails, catch the error
    @Override
    public void onException(Exception e) { 
        // Error
    }

    //If createEntityAsync is successful, handle the response object
    @Override
    public void onResponse(ApiResponse response) {
        try { 
            if (response != null) {
                // Success
            }
        } catch (Exception e) { //The API request returned an error
                // Fail
        }
    }
    }); 
                    

#### Response:

    {
    "action":"post",
    "application":"f34f4222-a166-11e2-a7f7-02e81adcf3d0",
    "entities":[{
        "Client":{
            "accessToken":null,
            "apiUrl":"http://api.usergrid.com",
            "applicationId":"your-app",
            "clientId":null,
            "clientSecret":null,
            "currentOrganization":null,
            "loggedInUser":null,
            "organizationId":"your-org"
        },
        "type":"item",
        "uuid":"fd29157a-e980-11e2-afcc-652a12f1ce72",
        "created":1373475098695,
        "modified":1373475098695,
        "metadata":{
            "path":"/items/fd29157a-e980-11e2-afcc-652a12f1ce72"
        }
    }],

    "params":{},
    "path":"/items",
    "rawResponse":"{
        "action" : "get",
        "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
        "params" : { },
        "path" : "/items",
        "uri" : "http://api.usergrid.com/your-org/your-app/items",
        "entities" : [ {
            "uuid" : "fd29157a-e980-11e2-afcc-652a12f1ce72",
            "type" : "item",
            "created" : 1373475098695,
            "modified" : 1373475098695,
            "metadata" : {
                "path" : "/items/fd29157a-e980-11e2-afcc-652a12f1ce72"
            }
        } ],
        "timestamp" : 1373475098689,
        "duration" : 24,
        "organization" : "your-org",
        "applicationName" : "your-app"
    }",
    "uri":"http://api.usergrid.com/your-org/your-app/items",
    "timestamp":1373475098689,
    "entityCount":1,
    "firstEntity":{
        "Client":{
            "accessToken":null,
            "apiUrl":"http://api.usergrid.com",
            "applicationId":"your-app",
            "clientId":null,
            "clientSecret":null,
            "currentOrganization":null,
            "loggedInUser":null,
            "organizationId":"your-org"
        },
        "type":"item",
        "uuid":"fd29157a-e980-11e2-afcc-652a12f1ce72",
        "created":1373475098695,
        "modified":1373475098695,
        "metadata":{
            "path":"/items/fd29157a-e980-11e2-afcc-652a12f1ce72"
        }
    },
    "lastEntity":{
        "Client":{
            "accessToken":null,
            "apiUrl":"http://api.usergrid.com",
            "applicationId":"your-app",
            "clientId":null,
            "clientSecret":null,
            "currentOrganization":null,
            "loggedInUser":null,
            "organizationId":"your-org"
        },
        "type":"item",
        "uuid":"fd29157a-e980-11e2-afcc-652a12f1ce72",
        "created":1373475098695,
        "modified":1373475098695,
        "metadata":{
            "path":"/items/fd29157a-e980-11e2-afcc-652a12f1ce72"
        }
    },
    "organization":"your-org",
    "duration":24,
    "applicationName":"your-app"
    }   
                    

### SDK Method

Asynchronous:

    getEntitiesAsync(String type, String queryString, ApiResponseCallback callback)

Synchronous:

    ApiResponse getEntities(String type, String queryString)

### Properties

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| type                                 | The entity type being retrieved      |
+--------------------------------------+--------------------------------------+
| queryString                          | A query string that specifies the    |
|                                      | property of the entity to be         |
|                                      | retrieved in the following format:   |
|                                      |                                      |
|                                      |     <property>=<value>               |
|                                      |                                      |
|                                      | To retrieve a specific entity, use   |
|                                      | the unique entity *uuid* or *name*   |
|                                      | property.                            |
+--------------------------------------+--------------------------------------+
| callback                             | Callback function (Asynchronous      |
|                                      | calls only)                          |
+--------------------------------------+--------------------------------------+

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    var properties = { 
        'type':'item',
        'name':'milk' //This method also supports retrieval by uuid 
    }; 
        
    client.getEntity(properties, function (error, response) { 
        if (err) { 
          //error 
        } else { 
          //success 
        } 
    });
                    

#### Response:

    Usergrid.Entity {_client: Usergrid.Client, _data: Object, serialize: function, get: function, set: function…}
        _client: Usergrid.Client
            URI: "https://api.usergrid.com"
            _callTimeout: 30000
            _callTimeoutCallback: null
            _end: 1378413997721
            _start: 1378413997493
            appName: "your-app"
            buildCurl: false
            logging: true
            logoutCallback: null
            orgName: "your-org"
            __proto__: Object
        _data: Object
            created: 1378405020796
            metadata: Object
            modified: 1378405020796
            name: "milk"
            price: "3.25"
            type: "item"
            uuid: "5bb76bca-1657-11e3-903f-9ff6c621a7a4"
            __proto__: Object
        __proto__: Object               
                    

### SDK Method

    getEntity(properties, callback)

### Parameters

#### getEntity():

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| properties                           | Object that contains the following   |
|                                      | properties of the entity to be       |
|                                      | retrieved:                           |
|                                      |                                      |
|                                      | -   type: the entity type to be      |
|                                      |     retrieved                        |
|                                      | -   name: the name of the entity to  |
|                                      |     be retrieved                     |
|                                      | -   uuid: the uuid of the entity to  |
|                                      |     be retrieved                     |
|                                      |                                      |
|                                      | You only need to specify name or     |
|                                      | uuid, not both.                      |
+--------------------------------------+--------------------------------------+
| callback                             | Callback function                    |
+--------------------------------------+--------------------------------------+

This example uses the [Apache Usergrid RubyGem](/app-services-sdks#ruby).

### Example Request/Response

#### Request:

    #Create a client object
    usergrid_api = 'https://api.usergrid.com'
    organization = 'your-org'
    application = 'your-app'

    client = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

    begin
    # Retrieve the entity by referencing the [type] and [uuid or name]
    # and save the response
    response = client['items']['milk'].entity

    rescue
    #fail
    end             
                    

#### Response:

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

### SDK Method

    application[<entity_type>][<entity_uuid|entity_name>].entity

### Parameters

Parameter

Description

entity\_type

The entity type to be retrieved

entity\_uuid|entity\_name

The name or UUID of the entity to be retrieved

This example uses the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    var properties = { 
        method:'GET', 
        type:'items',
        name:'da4a50dc-38dc-11e2-b2e4-02e81adcf3d0' 
    }; 

    client.getEntity(properties, function (error, response) { 
        if (error) { 
          //error 
        } else { 
          //success 
        } 
    });
                    

#### Response:

    { _client: 
       { URI: 'https://api.usergrid.com',
         orgName: 'your-org',
         appName: 'your-app',
         authType: 'NONE',
         clientId: undefined,
         clientSecret: undefined,
         token: null,
         buildCurl: false,
         logging: true,
         _callTimeout: 30000,
         _callTimeoutCallback: null,
         logoutCallback: null,
         _start: 1378423148601,
         _end: 1378423149028 },
      _data: 
       { method: 'GET',
         type: 'item',
         name: 'milk',
         uuid: '5bb76bca-1657-11e3-903f-9ff6c621a7a4',
         created: 1378405020796,
         modified: 1378405020796,
         metadata: { path: '/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4' },
         price: '3.25' } }              
                

### SDK Method

    getEntity(properties, callback)

### Parameters

Parameter

Description

Object that contains the following properties of the entity to be
retrieved:

-   type: the entity type to be retrieved
-   name: the name of the entity to be retrieved
-   uuid: the uuid of the entity to be retrieved

You only need to specify name or uuid, not both.

callback

Callback function

Retrieving multiple entities
----------------------------

-   [cURL](#curl_get_multentity)
-   [iOS](#ios_get_multentity)
-   [Android](#android_get_multentity)
-   [JavaScript (HTML5)](#javascript_get_multentity)
-   [Ruby](#ruby_get_multentity)
-   [Node.js](#nodejs_get_multentity)

This example describes how to retrieve multiple entities by UUID. You
can also retrieve a set of entities by using a query string. For more
information on querying your data, see [Basic query
syntax](/basic-query-syntax).

### Example Request/Response

#### Request:

    //note the url-encoded query string
    curl -X GET "https://api.usergrid.com/your-org/your-app/items?ql=name%3D'milk'%20OR%20UUID%3D1a9356ba-1682-11e3-a72a-81581bbaf055&limit="               
                    

**Note:** The query parameter of the request must be url encoded for
curl requests

#### Response:

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

### Request Syntax

    curl -X GET https://api.usergrid.com/<org_id>/<app_id>/<collection>?ql= uuid = <entity_uuid> OR uuid = <entity_uuid>; ...

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| org\_id                              | Organization UUID or organization    |
|                                      | name                                 |
+--------------------------------------+--------------------------------------+
| app\_id                              | Application UUID or application name |
+--------------------------------------+--------------------------------------+
| collection                           | Collection UUID or collection name   |
+--------------------------------------+--------------------------------------+
| query                                | A url-encoded query string of entity |
|                                      | properties to be matched in the      |
|                                      | following format:                    |
|                                      |                                      |
|                                      |     ?ql=uuid="<entity_uuid>"" OR nam |
|                                      | e="<entity_name>" OR...              |
|                                      |                                      |
|                                      | You may also specify the following   |
|                                      | for certain entity types:            |
|                                      |                                      |
|                                      | User entities:                       |
|                                      | `username = <entity_username>`       |
|                                      |                                      |
|                                      | All other entities except groups:    |
|                                      | `name = <entity_name>`               |
+--------------------------------------+--------------------------------------+

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)getEntity {

    //specify the entity type to be retrieved   
    NSString *type = @"item";

    //specify the uuid of the entity to be retrieved in a query string
    NSString *query = @"uuid = b3aad0a4-f322-11e2-a9c1-999e12039f87 or name = 'bread'";

        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient getEntities:type queryString:query];

    @try {
        //success
    }
    @catch (NSException * e) {
        //fail
    }

    }
                    
                    

#### Response:

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

### SDK Method

    (ApigeeClientResponse *)getEntities: (NSString *)type queryString:(NSString *)queryString

### Properties

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| type                                 | The entity type being retrieved      |
+--------------------------------------+--------------------------------------+
| queryString                          | A query string of entity properties  |
|                                      | to be matched for the entities to be |
|                                      | retrieved in the following format:   |
|                                      |     <property>=<value> OR <property> |
|                                      | =<value> OR ...                      |
+--------------------------------------+--------------------------------------+

This example uses the [Apache Usergrid Android
SDK](/app-services-sdks#android).

### Example Request/Response

#### Request:

    //Create client entity
    String ORGNAME = "your-org";
    String APPNAME = "your-app";        
    ApigeeClient apigeeClient = new ApigeeClient(ORGNAME,APPNAME);
    DataClient dataClient = apigeeClient.getDataClient();

    String type = "item"; //entity type to be retrieved
    String query = "uuid = f8726dda-f54a-11e2-b560-575bef89aaed OR name = 'bread'";  //query string specifying the entities to be retrieved

      
    //call getEntitiesAsync to initiate the asynchronous API call    
    dataClient.getEntitiesAsync(type, query, new ApiResponseCallback() {    

    //If getEntitiesAsync fails, catch the error
    @Override
    public void onException(Exception e) { 
        // Error
    }

    //If createEntityAsync is successful, handle the response object
    @Override
    public void onResponse(ApiResponse response) {
        try { 
            if (response != null) {
                // Success
            }
        } catch (Exception e) { //The API request returned an error
                // Fail
        }
    }
    }); 
                    

#### Response:

    {"action":"get","application":"f34f4222-a166-11e2-a7f7-02e81adcf3d0","entities":[{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"5bb76bca-1657-11e3-903f-9ff6c621a7a4","price":"3.25","created":1378405020796,"name":"milk","modified":1378405020796,"metadata":{"path":"/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"}},{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"1a9356ba-1682-11e3-a72a-81581bbaf055","price":"2.50","created":1378423379867,"name":"bread","modified":1378423379867,"metadata":{"path":"/items/1a9356ba-1682-11e3-a72a-81581bbaf055"}}],"params":{"ql":["uuid = 5bb76bca-1657-11e3-903f-9ff6c621a7a4 OR name = 'bread'"]},"path":"/items","rawResponse":"{
      "action" : "get",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : {
        "ql" : [ "uuid = 5bb76bca-1657-11e3-903f-9ff6c621a7a4 OR name = 'bread'" ]
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
      "timestamp" : 1378425390343,
      "duration" : 42,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
    ","uri":"http://api.usergrid.com/your-org/your-app/items","timestamp":1378425390343,"entityCount":2,"firstEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"5bb76bca-1657-11e3-903f-9ff6c621a7a4","price":"3.25","created":1378405020796,"name":"milk","modified":1378405020796,"metadata":{"path":"/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"}},"lastEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"1a9356ba-1682-11e3-a72a-81581bbaf055","price":"2.50","created":1378423379867,"name":"bread","modified":1378423379867,"metadata":{"path":"/items/1a9356ba-1682-11e3-a72a-81581bbaf055"}},"organization":"your-org","duration":42,"applicationName":"your-app"}               
                    

### SDK Method

Asynchronous:

    getEntitiesAsync(String type, String queryString, ApiResponseCallback callback)

Synchronous:

    ApiResponse getEntities(String type, String queryString)

### Properties

  Parameter     Description
  ------------- ---------------------------------------------------------------------------------------------------------------------------------------------------------------------
  type          The entity type being retrieved
  queryString   A query string of entity properties to be matched for the entities to be retrieved in the following format: \<property\>=\<value\> OR \<property\>=\<value\> OR ...
  callback      Callback function (Asynchronous calls only)

This example uses the [Apache Usergrid JavaScript (HTML5)
module](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    var dataClient = new Usergrid.Client({
    orgName:'your-org',
    appName:'your-app'
    });

    var options = {
                endpoint:"items",
                //Define the query - note the use of the 'ql' property
                //Note the use of the single-quote for the string 'bread'
                qs:{ql:"name='bread' or uuid=b3aad0a4-f322-11e2-a9c1-999e12039f87"}
            };

    //Call request to initiate the API call
    dataClient.request(options, function (error, response) {
    if (err) {
        //error — GET failed
    } else {
        //success       
    }
    });
                

#### Response:

    Object {action: "get", application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0", params: Object, path: "/items", uri: "http://api.usergrid.com/your-org/your-app/items"…}
        action: "get"
        application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0"
        applicationName: "your-app"
        count: 2
        duration: 57
        entities: Array[2]
            0: Object
                created: 1378423379867
                metadata: Object
                modified: 1378423379867
                name: "bread"
                price: "2.50"
                type: "item"
                uuid: "1a9356ba-1682-11e3-a72a-81581bbaf055"
                __proto__: Object
            1: Object
                created: 1378405020796
                metadata: Object
                modified: 1378405020796
                name: "milk"
                price: "3.25"
                type: "item"
                uuid: "5bb76bca-1657-11e3-903f-9ff6c621a7a4"
            __proto__: Object
            length: 2
        __proto__: Array[0]
        organization: "your-org"
        params: Object
        path: "/items"
        timestamp: 1378426195611
        uri: "http://api.usergrid.com/your-org/your-app/items"
        __proto__: Object               
                    

### SDK Method

    request(properties,callback)

### Parameters

  Parameter    Description
  ------------ --------------------------------------------
  properties   Object that contains the entity properties
  callback     Callback function

This example uses the [Apache Usergrid RubyGem](/app-services-sdks#ruby).

### Example Request/Response

#### Request:

This example uses the [Apache Usergrid RubyGem](/app-services-sdks#ruby).

    #Create a client object
    usergrid_api = 'https://api.usergrid.com'
    organization = 'your-org'
    application = 'your-app'

    client = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

    begin

    # Specify the name or uuid of the collection that contains them, and a query the specifies the name or uuid of the entities to retrieve
    response = client['items'].query("uuid=a86e614a-efc8-11e2-94fb-a94a8e3669a7 or uuid=71c29a2a-efc9-11e2-a3cc-ed942506cf87").entity

    rescue
    #fail
    end
                    

#### Response:

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

### SDK Method

    Application.[<collection>].query(<query>)

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| collection                           | The name or uuid of the collection   |
|                                      | that contains the data entities to   |
|                                      | be retrieved                         |
+--------------------------------------+--------------------------------------+
| query                                | A query string that specifies the    |
|                                      | property of the entity to be         |
|                                      | retrieved in the following format:   |
|                                      |     <property>=<value>               |
|                                      |                                      |
|                                      | To retrieve a specific entity, use   |
|                                      | the unique entity *uuid* or *name*   |
|                                      | property.                            |
+--------------------------------------+--------------------------------------+

This example uses the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    var dataClient = new Usergrid.client({
    orgName:'your-org',
    appName:'your-app'
    });

    var options = {
                endpoint:"items",
                //Define the query - note the use of the 'ql' property
                //Note the use of the single-quote for the string 'bread'
                qs:{ql:"name='bread' or uuid=b3aad0a4-f322-11e2-a9c1-999e12039f87"}
            };

    //Call request to initiate the API call
    dataClient.request(options, function (error, response) {
        if (error) {
            //error — GET failed
        } else {
            //success       
        }
    });
                    

#### Response:

    { action: 'get',
      application: 'f34f4222-a166-11e2-a7f7-02e81adcf3d0',
      params: { ql: [ 'name=\'bread\' or uuid=5bb76bca-1657-11e3-903f-9ff6c621a7a4' ] },
      path: '/items',
      uri: 'http://api.usergrid.com/your-org/your-app/items',
      entities: 
       [ { uuid: '1a9356ba-1682-11e3-a72a-81581bbaf055',
           type: 'item',
           name: 'bread',
           created: 1378423379867,
           modified: 1378423379867,
           metadata: [Object],
           price: '2.50' },
         { uuid: '5bb76bca-1657-11e3-903f-9ff6c621a7a4',
           type: 'item',
           name: 'milk',
           created: 1378405020796,
           modified: 1378405020796,
           metadata: [Object],
           price: '3.25' } ],
      timestamp: 1378426688958,
      duration: 37,
      organization: 'your-org',
      applicationName: 'your-app',
      count: 2 }                
                    

### SDK Method

    request(properties,callback)

### Parameters

  Parameter    Description
  ------------ --------------------------------------------
  properties   Object that contains the entity properties
  callback     Callback function

Add new comment
---------------

Your name

Email

Provide your email address if you wish to be contacted offline about
your comment.\
We will not display your email address as part of your comment.

Comment \*

We'd love your feedback and perspective! Please be as specific as
possible.

Word verification \*

![Type the characters you see in this
picture.](http://173.193.242.189:80/v1/captcha/131019a6363a732611.png)
([verify using audio](#))

Type the characters you see in the picture above; if you can't read
them, submit the form and a new image will be generated. Not case
sensitive.
