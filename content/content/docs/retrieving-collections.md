---
title: Retrieving Collections
category: docs
layout: docs
---

Retrieving Collections
======================

This article describes how to retrieve all of the entities in a
collection.

By default, the Apache Usergrid API returns 10 entities per request. For
collections with more than 10 entities, use the returned 'cursor'
property to retrieve the next 10 entities in the result set. You may
also use the `LIMIT` parameter in a query string to increase the number
of results returned. For more information on using cursors, see
[Managing large sets of results](/working-queries#cursor).

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Retrieving sets of entities from a collection
---------------------------------------------

-   [cURL](#curl_get_collection)
-   [iOS](#ios_get_collection)
-   [Android](#android_get_collection)
-   [JavaScript (HTML5)](#javascript_get_collection)
-   [Ruby](#ruby_get_collection)
-   [Node.js](#nodejs_get_collection)

### Example Request/Response

#### Request:

    curl -X GET "https://api.usergrid.com/your-org/your-app/items"

#### Response:

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

### Request Syntax

    curl -X GET https://api.usergrid.com/<org>/<app>/<collection>

### Parameters

  Parameter    Description
  ------------ ----------------------------------------
  org          Organization UUID or organization name
  app          Application UUID or application name
  collection   Collection UUID or collection name

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)getCollection {

        //specify the entity type that corresponds to the collection to be retrieved
        NSString *type = @"item";
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios

        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        ApigeeCollection *collection = [[ApigeeCollection alloc] init:apigeeClient.dataClient type:type];
        
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

### SDK Method

    (ApigeeCollection*)getCollection:(NSString*)type

### Properties

  Parameter   Description
  ----------- ----------------------------------------------------------------
  type        The entity type associated with the collection to be retrieved

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
    Map<String,Object> queryString =  null; //we don't need any additional query parameters, in this case
        
    //call getCollectionAsync to initiate the asynchronous API call    
    dataClient.getCollectionAsync(type, queryString, new ApiResponseCallback() {    

    //If getEntitiesAsync fails, catch the error
        @Override
        public void onException(Exception e) { 
            // Error
        }
        
        //If getCollectionAsync is successful, handle the response object
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

        {"action":"get","application":"f34f4222-a166-11e2-a7f7-02e81adcf3d0","entities":[{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"5bb76bca-1657-11e3-903f-9ff6c621a7a4","price":"4.00","created":1378405020796,"name":"milk","modified":1378505935248,"availability":"in-stock","metadata":{"path":"/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"}},{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"1a9356ba-1682-11e3-a72a-81581bbaf055","price":"2.50","created":1378423379867,"name":"bread","modified":1378423379867,"metadata":{"path":"/items/1a9356ba-1682-11e3-a72a-81581bbaf055"}}],"params":{},"path":"/items","rawResponse":"{
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
            "modified" : 1378505935248,
            "availability" : "in-stock",
            "metadata" : {
              "path" : "/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"
            },
            "name" : "milk",
            "price" : "4.00"
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
          "timestamp" : 1378512710357,
          "duration" : 39,
          "organization" : "your-org",
          "applicationName" : "your-app"
        }
        ","uri":"http://api.usergrid.com/your-org/your-app/items","timestamp":1378512710357,"entityCount":2,"firstEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"5bb76bca-1657-11e3-903f-9ff6c621a7a4","price":"4.00","created":1378405020796,"name":"milk","modified":1378505935248,"availability":"in-stock","metadata":{"path":"/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"}},"lastEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"1a9356ba-1682-11e3-a72a-81581bbaf055","price":"2.50","created":1378423379867,"name":"bread","modified":1378423379867,"metadata":{"path":"/items/1a9356ba-1682-11e3-a72a-81581bbaf055"}},"organization":"your-org","duration":39,"applicationName":"your-app"}
                        

### SDK Method

Asynchronous:

    getCollectionAsync(String type, Map<String,Object> queryString, ApiResponseCallback callback)

Synchronous:

    ApiResponse getCollection(String type, Map<String,Object> queryString)

### Properties

  Parameter     Description
  ------------- ----------------------------------------------------------------------------------
  type          The entity type being retrieved
  queryString   Map object of entity properties to be matched for the collection to be retrieved
  callback      Callback function (Asynchronous calls only)

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    var dataClient = new Usergrid.Client({
    orgName:'your-org',
    appName:'your-app'
    });

    var options = {
        type:"item", //Required - the type of collection to be retrieved
        client:dataClient //Required
    };

    //Create a collection object to hold the response
    var collection = new Usergrid.Collection(options);

    //Call request to initiate the API call
    collection.fetch(
        function() {
            //success callback
        },
        function() {
            //error callback
        }
    );
                        

#### Response:

    Object {action: "get", application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0", params: Object, path: "/items", uri: "http://api.usergrid.com/your-org/your-app/items"…}
        action: "get"
        application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0"
        applicationName: "your-app"
        count: 2
        duration: 33
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
        timestamp: 1378427598013
        uri: "http://api.usergrid.com/your-org/your-app/items"
        __proto__: Object               
                        

### SDK Method

    Collection.fetch(callback);

### Parameters

  Parameter   Description
  ----------- ----------------------------------------------
  callback    Callback function to handle the API response

This example uses the [Apache Usergrid RubyGem](/app-services-sdks#ruby).

### Example Request/Response

#### Request:

        #Create a client object
        usergrid_api = 'https://api.usergrid.com'
        organization = 'your-org'
        application = 'your-app'
        
        dataClient = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"
        
        begin
        # Retrieve the collection by referencing the [type]
        # and save the response
        response = dataClient['items'].entity
        
        rescue
        #fail
        end
                        
                        

#### Response:

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

### SDK Method

    Application.[<entity_type>].entity

### Parameters

  Parameter      Description
  -------------- ----------------------------------------------------------------
  entity\_type   The entity type associated with the collection to be retrieved

This example uses the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

        var dataClient = new Usergrid.client({
            orgName:'your-org',
            appName:'your-app'
        });
        
        var options = {
            type:"item", //Required - the type of collection to be retrieved
            client:dataClient //Required
        };
        
        //Create a collection object to hold the response
        var collection = new Usergrid.collection(options);
        
        //Call request to initiate the API call
        collection.fetch(function (error, response) {
            if (error) {
                //error
            } else {
                //success      
            }
        });
                        

#### Response:

        { action: 'get',
          application: 'f34f4222-a166-11e2-a7f7-02e81adcf3d0',
          params: {},
          path: '/items',
          uri: 'http://api.usergrid.com/your-org/your-app/items',
          entities: 
           [ { uuid: '5bb76bca-1657-11e3-903f-9ff6c621a7a4',
               type: 'item',
               name: 'milk',
               created: 1378405020796,
               modified: 1378405020796,
               metadata: [Object],
               price: '3.25' },
             { uuid: '1a9356ba-1682-11e3-a72a-81581bbaf055',
               type: 'item',
               name: 'bread',
               created: 1378423379867,
               modified: 1378423379867,
               metadata: [Object],
               price: '2.50' } ],
          timestamp: 1378428161834,
          duration: 33,
          organization: 'your-org',
          applicationName: 'your-app' }             
                        

### SDK Method

    Collection.fetch(callback)

### Parameters

  Parameter   Description
  ----------- ----------------------------------------------
  callback    Callback function to handle the API response

 
