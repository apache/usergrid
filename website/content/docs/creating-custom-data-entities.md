---
title: Creating Custom Data Entities
category: docs
layout: docs
---

Creating Custom Data Entities
=============================

This article describes how to create custom data entities and entity
properties in Apache Usergrid. Entity types correspond to the name of
collection to which they are posted. For example, if you create a new
custom "dog" entity, a "dogs" collection will be created if one did not
already exist. If a "dogs" collection already exists, the new "dog"
entity will be saved in it. All user-defined properties are indexed, and
strings that contain multiple words are keyword-indexed.

The methods cited in this article should be used to create custom data
entities. If you are using one of the [Apache Usergrid
SDKs](/app-services-sdks), use one of the entity type-specific SDK
methods to create [default data entities](/default-data-entities).

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Creating a custom entity
------------------------

When a new entity is created, Apache Usergrid will automatically create a
corresponding collection if one does not already exist. The collection
will automatically be named with the plural form of the entity type. For
example, creating a custom entity of type 'item' will automatically
create a collection named 'items' in which all future 'item' entities
will be saved.

-   [cURL](#curl_create_entity)
-   [iOS](#ios_create_entity)
-   [Android](#android_create_entity)
-   [JavaScript (HTML5)](#javascript_create_entity)
-   [Ruby](#ruby_create_entity)
-   [Node.js](#nodejs_create_entity)

### Example Request/Response

#### Request:

    curl -X POST "https://api.usergrid.com/your-org/your-app/item" -d '{"name":"milk", "price":"3.25"}'

#### Response:

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

### Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/<entity_type> -d '{<property>, <property>, ...}'

### Parameters

  Parameter      Description
  -------------- -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  org            Organization UUID or organization name
  app            Application UUID or application name
  entity\_type   Entity type to create. Apache Usergrid will create a corresponding collection if one does not already exist. To add an entity to an existing collections, use the pluralized collection name for entity\_type.
  property       Comma-separated list of entity properties, formatted as key-value pairs: \<property\>:\<value\> (for a full list of default properties, see [Default Data Entities](/default-data-entities))

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)newEntity {
        
        //create an entity object   
        NSMutableDictionary *entity = [[NSMutableDictionary alloc] init ];
        
        //Set entity properties
        [entity setObject:@"item" forKey:@"type"]; //Required. New entity type to create
        [entity setObject:@"milk" forKey:@"name"];
        [entity setObject:@"3.25" forKey:@"price"];
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient createEntity:entity];
        
        @try {      
            //success       
        }
        @catch (NSException * e) {
            //fail
        }
        
    }
                    

#### Response:

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

### SDK Method

    (ApigeeClientResponse *)createEntity:(NSDictionary *)newEntity

### Parameters

  Parameter   Description
  ----------- ---------------------------------------------------------
  newEntity   NSDictionary object that contains the entity properties

This example uses the [Apache Usergrid Android
SDK](/app-services-sdks#android).

### Example Request/Response

#### Request:

    //Create client entity
            String ORGNAME = "your-org"; 
            String APPNAME = "your-app";
            
            ApigeeClient apigeeClient = new ApigeeClient(ORGNAME,APPNAME,this.getBaseContext());
            DataClient client = apigeeClient.getDataClient();

    //Create properties object
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("type", "item");
    properties.put("name", "milk");
    properties.put("price", "3.25");

    //call createEntityAsync to initiate the asynchronous API call
    apigeeClient.createEntityAsync(properties, new ApiResponseCallback() {
        
        //If createEntityAsync fails, catch the error
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
            "action" : "post",
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

    createEntityAsync(Map<String, Object> properties, ApiResponseCallback callback)

Synchronous:

    ApiResponse createEntity(Map<String, Object> properties)

### Parameters

  Parameter    Description
  ------------ ----------------------------------------------------------------------------------
  properties   Map object that contains the entity properties. The 'type' property is required.
  callback     Callback function (Asynchronous calls only)

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

               
    //Create your client object
    var dataClient = new Usergrid.Client({
        orgName:'your-org',
        appName:'your-app'
    });


    //Set the properties of the entity
    var options = {
        type:'item', //required
        name:'milk',
        price:'3.25'
    };

    //Create the entity and process the results
    client.createEntity(options, function (err, result) {
        if (err) {
            //error
        } else {
            //success          
        }
    });     
                     

#### Response:

    "_client": //Information on the client object used to initiate the call
        "URI":"https://api.usergrid.com",
        "orgName":"your-org",
        "appName":"your-app",
        "buildCurl":false,
        "logging":false,
        "_callTimeout":30000,
        "_callTimeoutCallback":null,
        "logoutCallback":null,
        "_start":1373482218757,
        "_end":1373482219070,
    "_data": //Information on the successfully created entity
        "type":"item",
        "name":"milk",
        "price":"3.25",
        "uuid":"9124211a-e991-11e2-ba6c-e55e3ffa12ef",
        "created":1373482218913,
        "modified":1373482218913,
        "metadata":
            "path":"/items/9124211a-e991-11e2-ba6c-e55e3ffa12ef"                    
                    

### SDK Method

    createEntity(properties, callback)

### Parameters

  Parameter    Description
  ------------ ------------------------------------------------------------------------------
  properties   Object that contains the entity properties. The `type` property is required.
  callback     Callback function

This example uses the [Apache Usergrid RubyGem](/app-services-sdks#ruby).

### Example Request/Response

#### Request:

    #Create a client object
    usergrid_api = 'https://api.usergrid.com'
    organization = 'your-org'
    application = 'your-app'

    client = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

    begin
        # Call create_entity to initiate the API call
        # and save the response
        client.create_entity('item', {'name' => 'milk', 'price' => '3.25'})
    rescue
        #fail
    end
                    

#### Response:

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

### SDK Method

    create_entity(type, properties)

### Properties

  Parameter    Description
  ------------ --------------------------------------------
  type         Custom entity type to create
  properties   Object that contains the entity properties

The example assumes use of the [Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    //Create a client object
    var dataClient = new Usergrid.client({
        orgName:'your-org',
        appName:'your-app'
    });

    //Set the properties for your entity
    var properties = {
        type:"item", //Requried. Type of entity to create.
        name:"milk",
        price:"3.25"
    };

    //Call createEntity to initiate the API call
    client.createEntity(properties, function(error, result){
        if(error) {
            //error
        } else {
            //success
        }
    });
                    

#### Response:

    { 
        type: 'item',
        name: 'milk',
        price: '3.25',
        uuid: '126e29aa-eb40-11e2-85a8-355b0e586a1a',
        created: 1373667119418,
        modified: 1373667119418,
        metadata: { path: '/items/126e29aa-eb40-11e2-85a8-355b0e586a1a' } 
    }           
                    

### SDK Method

    createEntity(properties, callback)

### Parameters

  Parameter    Description
  ------------ ------------------------------------------------------------------------------
  properties   Object that contains the entity properties. The 'type' property is required.
  callback     Callback function

Creating multiple custom entities
---------------------------------

-   [cURL](#curl_create_multiple_entities)
-   [iOS](#ios_create_multiple_entities)
-   [Android](#android_create_multiple_entities)
-   [JavaScript (HTML5)](#javascript_create_multiple_entities)
-   [Ruby](#ruby_create_multiple_entities)
-   [Node.js](#nodejs_create_multiple_entities)

### Example Request/Response

#### Request:

    curl -X POST "https://api.usergrid.com/your-org/your-app/item" -d '[{"name":"milk", "price":"3.25"}, {"name":"bread", "price":"2.50"}]'

#### Response:

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

### Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/<entity_type>/ -d '[{<entity>}, {<entity>}, ...]'

### Parameters

  Parameter      Description
  -------------- ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  org            Organization UUID or name
  app            Application UUID or name
  entity\_type   Custom entity type to create. Apache Usergrid will create a corresponding collection if one does not already exist. To add an entity to an existing collections, use the collection name or colleciton UUID in place of the entity type.
  entity         Comma-separated list of entity objects to create. Each object should be formatted as a comma-separated list of entity properties, formatted as key-value pairs in the format \<property\>:\<value\>

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

               
    -(NSString*)newMultipleEntities {

        //apiRequest requires us to form the full API request, including base URL, org name, and app name.
        //Note that we specify the target collection in the URL string
        NSString * url = @"https://api.usergrid.com/your-org/your-app/items";
        NSString * http_method = @"POST";
        NSString * properties = @"[{\"name\":\"milk\",\"price\":\"3.25\"},{\"name\":\"bread\",\"price\":\"2.50\"}]"; //We escape the double quotes in the request body
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient apiRequest:url operation:http_method data:properties];

        @try {
            
           //success
            
        }
        @catch (NSException * e) {
            //fail
        }
        
    }
                    

#### Response:

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

### SDK Method

The iOS SDK does not have a dedicated method for creating multiple
entities. Instead, you can use the generic apiRequest method to form the
API request manually.

    (ApigeeClientResponse *)apiRequest: (NSString *)url operation:(NSString *)http_method data:(NSString *)properties

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| url                                  | Fully-formed request URL in the      |
|                                      | format:                              |
|                                      |                                      |
|                                      |     https://api.usergrid.com/<your-o |
|                                      | rg>/<your-app>/<collection>          |
+--------------------------------------+--------------------------------------+
| http\_method                         | HTTP Method – in this case POST      |
+--------------------------------------+--------------------------------------+
| properties                           | A JSON array, containing a JSON      |
|                                      | object of entity properties for each |
|                                      | entity to be created. The 'type'     |
|                                      | property is required for each        |
|                                      | entity.                              |
+--------------------------------------+--------------------------------------+

Currently, creating multiple entities is not supported by the [App
Services Android SDK](/app-services-sdks#android).

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    //Create a client object with your organization name and application name.
    var dataClient = new Usergrid.Client({
        orgName:'your-org',
        appName:'your-app'
    });

    //Set the request options: http method, endpoint, body
    var options = {
        method:'POST',
        endpoint:'items', //The collection name
        body:[{"name":"milk", "price":"3.25"},{"name": "bread", "price":"3.25"}] //note the multiple JSON objects
    };

    //Call request() to initiate the API call and process the results
    client.request(options, function (error, response) {
        if (error) {
            //error
        } else {
            //success        
        }
    });
                    

#### Response:

    Object {action: "post", application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0", params: Object, path: "/items", uri: "http://api.usergrid.com/your-org/your-app/items"…}
        action: "post"
        application: "f34f4222-a166-11e2-a7f7-02e8js76g3d0" //your Apache Usergrid application uuid
        applicationName: "your-app"
        duration: 315
        entities: Array[2] //array of entities successfully created
            0: Object //entity 1
                created: 1374099532148
                metadata: Object
                modified: 1374099532148
                name: "milk"
                price: "3.25"
                type: "item"
                uuid: "dc80834a-ef2e-11e2-8a77-cf8d4c2dbd49"
                __proto__: Object
            1: Object //entity 2
                created: 1374099532344
                metadata: Object
                modified: 1374099532344
                name: "bread"
                price: "3.25"
                type: "item"
                uuid: "dc9e6b8a-ef2e-11e2-8e8b-e39a2d988c4f"
        length: 2
        organization: "your-org"
        params: Object
        __proto__: Object
            path: "/items"
            timestamp: 1374099532139
            uri: "http://api.usergrid.com/your-org/your-app/items"              
                    

### SDK Method

    request(options, callback)

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| options                              | Object that contains the following   |
|                                      | properties:                          |
|                                      | -   endpoint: the collection to add  |
|                                      |     the entities to                  |
|                                      | -   method: the HTTP method for the  |
|                                      |     request - in this case *POST*    |
|                                      | -   body: the body of the request –  |
|                                      |     in this case, a JSON-array       |
|                                      |     containing an object with        |
|                                      |     properties for each entity to be |
|                                      |     created.                         |
+--------------------------------------+--------------------------------------+
| callback                             | Callback function                    |
+--------------------------------------+--------------------------------------+

This example uses the [Apache Usergrid RubyGem](/app-services-sdks#ruby).

### Example Request/Response

    #Create a client object
    usergrid_api = 'https://api.usergrid.com'
    organization = 'your-org'
    application = 'your-app'

    client = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

    begin
        # Call create_entity to initiate the API call - note the multiple JSON objects
        # and save the response
        response = client.create_entity 'multi', [{'name'=> 'milk', 'price' => '3.25'},{'name'=> 'bread', 'price' => '2.50'}]
        new_items=response.entity
    rescue
        #fail
    end         
                    

#### Response:

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

### SDK Method

    create_entity(type, properties)

### Properties

  Parameter    Description
  ------------ --------------------------------------------
  type         Custom entity type to create
  properties   Object that contains the entity properties

The example uses the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    //Create a client object
    var dataClient = new Usergrid.client({
        orgName:'your-org',
        appName:'your-app'
    });         
                
    //Set the request options: http method, endpoint, body
    var options = {
    method:'POST',
    endpoint:'items', //The collection name
    body:[{"name":"milk", "price":"3.25"},{"name": "bread", "price":"3.25"}] //note the multiple JSON objects
    };

    //Call request() to initiate the API call and process the results
    client.request(options, function (error, result) {
        if (error) {
            //error
        } else {
            //success — result will contain raw results from API call        
        }
    });

                

#### Response:

    {
        "action": "post",
        "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e221",
        "params":   {},
        "path": "/items",
        "uri": "https://api.usergrid.com/22000a1c4e22-7fsii8t1-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/cats",
        "entities":     [
            {
                "uuid": "187d31d9-0742-11e2-a7b5-12313d21509c",
                "type": "item",
                "name": "milk",
                "price" : "3.25"
                "created": 1348599123463,
                "modified": 1348599123463,
                "metadata":     {
                    "path": "/items/187d31d9-0742-11e2-a7b5-12313d21509c"
                    }
            },
            {
                "uuid": "188f815b-0742-11e2-a7b5-12313d21509c",
                "type": "items",
                "name": "bread",
                "price" : "2.50"
                "created": 1348599123583,
                "modified": 1348599123583,
                "metadata":     {
                                "path": "/cats/187d31d9-0742-11e2-a7b5-12313d21509c"
                }
            }
        ],
        "timestamp":    1348599123461,
        "duration": 415,
        "organization": "your-org",
        "applicationName": "your-app"
    }
                

### SDK Method

    request(options, callback)

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| options                              | Object that contains the following   |
|                                      | properties:                          |
|                                      | -   endpoint: the collection to add  |
|                                      |     the entities to                  |
|                                      | -   method: the HTTP method for the  |
|                                      |     request – in this case *POST*    |
|                                      | -   body: the body of the request.   |
|                                      |     In this case, a JSON-formatted   |
|                                      |     set of objects containing entity |
|                                      |     properties.                      |
+--------------------------------------+--------------------------------------+
| callback                             | Callback function                    |
+--------------------------------------+--------------------------------------+

Creating an entity with sub-properties
--------------------------------------

Any valid JSON object can be stored in an entity, regardless of the
level of complexity, including sub-properties. For example, suppose you
have an 'items' collection that contains an entity named 'milk'. You
might store the different varieties of milk as sub-properties of a
'varieties' property:

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
        

The following examples show how to create a new entity that contains an
entity with sub-properties.

#### Updating sub-properties

An array of sub-properties is treated as a single object in App
Services. This means that sub-properties cannot be updated atomically.
All sub-properties of a given property must be updated as a set.

For more on updating an existing sub-property, see [Updating Data
Entities](/updating-data-entities#update_nested).

-   [cURL](#curl_create_subproperties)
-   [iOS](#ios_create_subproperties)
-   [Android](#android_create_subproperties)
-   [JavaScript (HTML5)](#javascript_create_subproperties)
-   [Ruby](#ruby_create_subproperties)
-   [Node.js](#nodejs_create_subproperties)

### Example Request/Response

#### Request:

    //Note the use of square brackets for specifying multiple nested objects
    curl -X POST "https://api.usergrid.com/your-org/your-app/items" -d '{"varieties":[{"name":"1%","price" : "3.25", "sku" : "0393847575533445"},{"name" : "whole", "price" : "3.85", "sku" : "0393394956788445"}, {"name" : "skim", "price" : "4.00", "sku" : "0390299933488445"}]}'

#### Response:

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

### Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/<collection>/ -d '{"name" : <entity_name>, <property> : [{<sub_property>}, {<sub_property>}...]}'

### Parameters

  -----------------------------------------------------------------------------------------------------
  Parameter       Description
  --------------- -------------------------------------------------------------------------------------
  org             Organization UUID or name

  app             Application UUID or name

  collection      The UUID or name of the collection to add the new entity to

  entity\_name    The name of the new entity

  property        The name of the entity property that will contain the nested JSON object or array

  sub\_property   Entity properties of the nested object, as a set of key-value pairs in the format:\
                  *{\<property\> : \<value\>, \<property\> : \<value\> ...}*
  -----------------------------------------------------------------------------------------------------

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)newEntity {
        
        //Initialize an object for the new entity to be created
        NSMutableDictionary *entity = [[NSMutableDictionary alloc] init ];
        
        //Initialize an object for each nested variety object
        NSMutableDictionary *variety_1 = [[NSMutableDictionary alloc] init ];
        NSMutableDictionary *variety_2 = [[NSMutableDictionary alloc] init ];
        NSMutableDictionary *variety_3 = [[NSMutableDictionary alloc] init ];
            
        //Initialize an array to hold the nested variety objects
        NSMutableArray *variety_list = [[NSMutableArray alloc] init];
        
        [variety_1 setObject:@"1%" forKey:@"name"];
        [variety_1 setObject:@"3.25" forKey:@"price"];
        [variety_1 setObject:@"0393847575533445" forKey:@"sku"];    
        
        [variety_2 setObject:@"whole" forKey:@"name"];
        [variety_2 setObject:@"3.85" forKey:@"price"];
        [variety_2 setObject:@"0393394956788445" forKey:@"sku"];
        
        [variety_3 setObject:@"skim" forKey:@"name"];
        [variety_3 setObject:@"4.00" forKey:@"price"];
        [variety_3 setObject:@"0390299933488445" forKey:@"sku"];
        
        //Add the variety objects to the array
        [variety_list addObject:variety_1];
        [variety_list addObject:variety_2];
        [variety_list addObject:variety_3];
        
        //Set the item entity properties
        [entity setObject:@"item" forKey:@"type"]; //Required. New entity type to create
        [entity setObject:@"milk" forKey:@"name"];
        
        //Set the variety_list array as the value of the 'varieties' property
        [entity setObject:variety_list forKey:@"varieties"];
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient createEntity:entity];
        
        @try {
            //success
        }
        @catch (NSException * e) {
            //fail
        }
        
    }
                    

#### Response:

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

### SDK Method

    (ApigeeClientResponse *)createEntity:(NSDictionary *)newEntity

### Parameters

  Parameter   Description
  ----------- ----------------------------------------------------------------
  newEntity   NSMutableDictionary object that contains the entity properties

This example uses the [Apache Usergrid Android
SDK](/app-services-sdks#android).

### Example Request/Response

#### Request:

    //Create client entity
            String ORGNAME = "your-org"; 
            String APPNAME = "your-app";
            
            ApigeeClient apigeeClient = new ApigeeClient(ORGNAME,APPNAME,this.getBaseContext());
            DataClient client = apigeeClient.getDataClient();

    //Create hashmap object for the properties of the new 'item' entity
    Map<String, Object> properties = new HashMap<String, Object>();

    //Create hashmap object for the each nested 'variety' object
    Map<String, Object> variety_1 = new HashMap<String, Object>();
    Map<String, Object> variety_2 = new HashMap<String, Object>();
    Map<String, Object> variety_3 = new HashMap<String, Object>();
            
    //Add properties for each nested object
    variety_1.put("name","1%");
    variety_1.put("price","3.25");
    variety_1.put("sku","0393847575533445");

    variety_2.put("name","whole");
    variety_2.put("price","3.85");
    variety_2.put("sku","0393394956788445");

    variety_3.put("name","skim");
    variety_3.put("price","4.00");
    variety_3.put("sku","0390299933488445");

    //Create an ArrayList of the 'variety' objects
    ArrayList<Map<String, Object>> variety_list = new ArrayList<Map<String, Object>>();
    variety_list .add(variety_1);
    variety_list .add(variety_2);
    variety_list .add(variety_3);

    //Add the required properties for the 'item' entity
    properties.put("name", "milk");
    properties.put("type", "item");

    //Add 'variety_list' as the value of the 'varieties' property
    properties.put("varieties", variety_list);

    //call createEntityAsync to initiate the asynchronous API call
    dataClient.createEntityAsync(properties, new ApiResponseCallback() {
        
        //If createEntityAsync fails, catch the error
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
            "action" : "post",
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
                "varieties": [
                    {
                        "sku": "0393847575533445",
                        "price": "3.25",
                        "name": "1%"
                    },
                    {
                        "sku": "0393394956788445",
                        "price": "3.85",
                        "name": "whole"
                    },
                    {
                        "sku": "0390299933488445",
                        "price": "4.00",
                        "name": "skim"
                    }
                ]
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

    createEntityAsync(Map<String, Object> properties, ApiResponseCallback callback)

Synchronous:

    ApiResponse createEntity(Map<String, Object> properties)

### Parameters

  Parameter    Description
  ------------ ---------------------------------------------
  properties   Object that contains the entity properties
  callback     Callback function (asynchronous calls only)

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    //Create a client object with your organization name and application name.
    var dataClient = new Usergrid.Client({
        orgName:'your-org',
        appName:'your-app'
    });

                
    var options = {
        method:'POST',
        endpoint:'items',
        body:{ 
            name:'milk', 
            "varieties" : [
                    
                {
                    "name" : "1%",
                    "price" : "3.25",
                    "sku" : "0393847575533445"
                },
                {
                    "name" : "whole",
                    "price" : "3.85",
                    "sku" : "0393394956788445"
                },
                {
                    "name" : "skim",
                    "price" : "4.00",
                    "sku" : "0390299933488445"      
                }
            ]       
        }
    };

    client.createEntity(options, function (error, cat) {
        
        if (error) {
            //error
        } else {
            //success          
        }
    });
                    

#### Response:

    Object {action: "post", application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0", params: Object, path: "/items", uri: "http://api.usergrid.com/your-org/your-app/items"…}
        action: "post"
        application: "f34f4222-a166-11e2-a7f7-02e8js76g3d0" //your Apache Usergrid application uuid
        applicationName: "your-app"
        duration: 315
        entities: Array[1] //array of entities successfully created
            0: Object //entity 1
                created: 1374099532148
                metadata: Object
                modified: 1374099532148
                name: "milk"
                price: "3.25"
                type: "item"
                uuid: "dc80834a-ef2e-11e2-8a77-cf8d4c2dbd49"
                varieties: Array[3] //array of variety objects from variety_list
                    0: Object
                    sku: "0393847575533445"
                    name: "1%"
                    price: "3.25"
                    1: Object
                    sku: "0393394956788445"
                    name: "whole"
                    price: "3.85"
                    2: Object
                    sku: "0390299933488445"
                    name: "skim"
                    price: "4.00"
                    length: 3
        organization: "your-org"
        params: Object
        __proto__: Object
            path: "/items"
            timestamp: 1374099532139
            uri: "http://api.usergrid.com/your-org/your-app/items"              
                    

### SDK Method

    request(options, callback)

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| options                              | Object that contains the following   |
|                                      | properties:                          |
|                                      | -   endpoint: the collection to add  |
|                                      |     the entities to                  |
|                                      | -   method: the HTTP method for the  |
|                                      |     request – in this case *POST*    |
|                                      | -   body: the body of the request.   |
|                                      |     In this case, a JSON-formatted   |
|                                      |     set of objects containing entity |
|                                      |     properties.                      |
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

    properties = {
        name:'milk', 
        varieties:[{
            name:'1%',
            price:'3.25',
            sku:'0393847575533445'
        },{
            name:'whole',
            price:'3.85',
            sku:'0393394956788445'
        },{
            name:'skim',
            price:'4.00',
            sku:'0390299933488445'
        }]
    }

    begin
        # Call create_entity to initiate the API call
        # and save the response
    response = client.create_entity 'item', #{properties}
        new_item=response.entity
    rescue
        #fail
    end
                    

#### Response:

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

### SDK Method

    create_entity(type, properties)

### Properties

  Parameter    Description
  ------------ --------------------------------------------
  type         Entity type to create
  properties   Object that contains the entity properties

The example uses the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    //Create a client object
    var dataClient = new Usergrid.client({
        orgName:'your-org',
        appName:'your-app'
    });

    var options = {
        method:'POST',
        endpoint:'items',
        body:{ 
            name:'milk', 
            varieties : [
                    
                {
                    name : "1%",
                    price : "3.25",
                    sku : "0393847575533445"
                },
                {
                    name : "whole",
                    price : "3.85",
                    sku : "0393394956788445"
                },
                {
                    name : "skim",
                    price : "4.00",
                    sku : "0390299933488445"        
                }
            ]       
        }
    };

    client.createEntity(options, function (error, cat) {
        
        if (error) {
            //error
        } else {
            //success          
        }
    });
                

#### Response:

    { action: 'post',
      application: 'f34f4222-a166-11e2-a7f7-02e81adcf3d0',
      params: {},
      path: '/items',
      uri: 'http://api.usergrid.com/your-org/your-app/items',
      entities: 
       [ { uuid: 'b3aad0a4-f322-11e2-a9c1-999e12039f87',
           type: 'item',
           name: 'milk',
           created: 1374534114329,
           modified: 1374534114329,
           metadata: [Object],
           varieties: [Object] } ],
      timestamp: 1374534114326,
      duration: 109,
      organization: 'your-org',
      applicationName: 'your-app' } 
                    

### SDK Method

    createEntity(properties, callback)

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
picture.](http://173.193.242.189:80/v1/captcha/131019b0bb7096f3fa.png)
([verify using audio](#))

Type the characters you see in the picture above; if you can't read
them, submit the form and a new image will be generated. Not case
sensitive.
