---
title: Updating Collections
category: docs
layout: docs
---

Updating Collections
====================

This article describes how to perform batch updates on all entities in a
collection. Batch updates require the use of a query string in the
request, which can either specify all entities in the collection or a
subset of entities for the update to be performed on. For more
information on queries, see [Basic query syntax](/basic-query-syntax).

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Batch updating entities in a collection
---------------------------------------

-   [cURL](#curl_update_collection)
-   [iOS](#ios_update_collection)
-   [Android](#android_update_collection)
-   [JavaScript (HTML5)](#javascript_update_collection)
-   [Ruby](#ruby_update_collection)
-   [Node.js](#nodejs_update_collection)

### Example Request/Response

#### Request:

    curl -X PUT https://api.usergrid.com/your-org/your-app/items/?ql= -d '{"availability":"in-stock"}'

Note the empty `?ql=` query string.

#### Response:

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

### Request Syntax

    curl -X PUT https://api.usergrid.com/<org>/<app>/<collection>/?ql= -d {<property>}

Note the empty query string (ql=) appended to the URL.

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| org                                  | Organization UUID or organization    |
|                                      | name                                 |
+--------------------------------------+--------------------------------------+
| app                                  | Application UUID or application name |
+--------------------------------------+--------------------------------------+
| collection                           | Collection UUID or collection name   |
+--------------------------------------+--------------------------------------+
| property                             | An entity property to be updated,    |
|                                      | formatted as a key-value pair. For   |
|                                      | example:                             |
|                                      |                                      |
|                                      |     {"property_1":"value_1", "proper |
|                                      | ty_2":"value_2",...}                 |
+--------------------------------------+--------------------------------------+

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)updateCollection {

        NSString *url = @"https://api.usergrid.com/your-org/your-app/items/?ql";
        NSString *op = @"PUT";
        NSString *opData = @"{\"availability\":\"in-stock\"}"; //we escape the quotes
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient apiRequest: url operation: op data: opData];
        
        @try {
            //success
        }
        @catch (NSException * e) {
            //fail
        }

    }
                    

#### Response:

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

### SDK Method

    (ApigeeClientResponse *)apiRequest: (NSString *)url operation:(NSString *)op data:(NSString *)opData

### Properties

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| url                                  | A fully-formed request url in the    |
|                                      | following format:                    |
|                                      |     https://api.usergrid.com/<org>/< |
|                                      | app>/<collection>/?ql=               |
|                                      |                                      |
|                                      | Note that you must include an empty  |
|                                      | '?ql=' query string at the end of    |
|                                      | the URL                              |
+--------------------------------------+--------------------------------------+
| op                                   | The HTTP method - in this case,      |
|                                      | 'PUT'                                |
+--------------------------------------+--------------------------------------+
| opData                               | A JSON-formatted string that         |
|                                      | contains the entity properties to be |
|                                      | updated                              |
+--------------------------------------+--------------------------------------+

Updating all entities in a collection is not currently supported by the
[Apache Usergrid Android SDK](/app-services-sdks#android).

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    //create the basic client object
    var dataClient = new Usergrid.Client({
        orgName:'your-org',
        appName:'your-app'
    });

    //options for the request
    var options = {
        endpoint:"items/?ql=", //don't forget to append '/?ql='
        method:"PUT",
        body: {"availability":"in-stock"}
    }

    dataClient.request(options,function (error,response) {

        if (error) { 
            // Error
        } else { 
            // Success
        }

    });    
                    

#### Response:

    Object {action: "put", application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0", params: Object, path: "/items", uri: "http://api.usergrid.com/your-org/your-app/items"…}
    action: "put"
    application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0"
    applicationName: "your-app"
    duration: 92
    entities: Array[2]
        0: Object
            availability: "in-stock"
            created: 1378852309294
            modified: 1378853303215
            name: "milk"
            price: "3.25"
            type: "item"
            uuid: "c86ffbf0-1a68-11e3-ab22-3713e5fcf9d2"
            __proto__: Object
        1: Object
            availability: "in-stock"
            created: 1378852309373
            modified: 1378853303256
            name: "bread"
            price: "4.00"
            type: "item"
            uuid: "c87be2da-1a68-11e3-80f4-975f1f8b1f86"
            __proto__: Object
        length: 2
        __proto__: Array[0]
    organization: "your-org"
    params: Object
    path: "/items"
    timestamp: 1378853303201
    uri: "http://api.usergrid.com/your-org/your-app/items"
    __proto__: Object
                    

### SDK Method

    request(options, callback)

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| options                              | A JSON-formatted object containing   |
|                                      | the following properties:            |
|                                      |                                      |
|                                      | -   endpoint: the UUID or name of    |
|                                      |     the collection to be updated,    |
|                                      |     appended by an empty query       |
|                                      |     string in the format:            |
|                                      |     \<collection\>/?ql=              |
|                                      | -   method: the HTTP method for the  |
|                                      |     request – in this case `PUT`     |
|                                      | -   body: the body of the request    |
|                                      |     that specifies the properties to |
|                                      |     be updated in JSON format        |
+--------------------------------------+--------------------------------------+
| callback                             | Callback function to handle the API  |
|                                      | response                             |
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
        collection = client['items'].collection
        collection.update_query({availability: 'in-stock'},"")
    rescue
        #fail
    end
                    

#### Response:

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

### SDK Method

    collection.update_query(properties, query_string)

### Properties

  Parameter       Description
  --------------- --------------------------------------------------------------------------------------------------------------
  properties      JSON-formatted string that contains the entity properties to be updated
  query\_string   A query string that specifies the entities to be updated. Use an empty string (`""`) to update all entities.

The example assumes use of the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    //Create the Apache Usergrid client object
    var dataClient = new Usergrid.client({
        orgName:'your-org',
        appName:'your-app'
    });

    //Specify the options for the request
    var options = {
        endpoint:"items/?ql=", //don't forget to append '/?ql='
        method:"PUT",
        body: {"availability":"in-stock"}
    }

    dataClient.request(options,function (error,response) {

        if (error) { 
            //error
        } else { 
            //success
        }

    });             
                    

#### Response:

    { action: 'put',
      application: 'f34f4222-a166-11e2-a7f7-02e81adcf3d0',
      params: { ql: [ '' ] },
      path: '/items',
      uri: 'http://api.usergrid.com/your-org/your-app/items',
      entities: 
       [ { uuid: 'c86ffbf0-1a68-11e3-ab22-3713e5fcf9d2',
           type: 'item',
           name: 'milk',
           created: 1378852309294,
           modified: 1378855073613,
           availability: 'in-stock' },
         { uuid: 'c87be2da-1a68-11e3-80f4-975f1f8b1f86',
           type: 'item',
           name: 'bread',
           created: 1378852309373,
           modified: 1378855073655,
           availability: 'in-stock' } ],
      timestamp: 1378855073595,
      duration: 106,
      organization: 'your-org',
      applicationName: 'your-app' }
                    

### SDK Method

    request(options,callback);

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| options                              | A JSON-formatted object containing   |
|                                      | the following properties:            |
|                                      |                                      |
|                                      | -   endpoint: the UUID or name of    |
|                                      |     the collection to be updated,    |
|                                      |     appended by an empty query       |
|                                      |     string in the format:            |
|                                      |     \<collection\>/?ql=              |
|                                      | -   method: the HTTP method for the  |
|                                      |     request, in this case *PUT*      |
+--------------------------------------+--------------------------------------+
| callback                             | Callback function to handle the API  |
|                                      | response                             |
+--------------------------------------+--------------------------------------+

 
