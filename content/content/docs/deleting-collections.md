---
title: Deleting Collections
category: docs
layout: docs
---

Deleting Collections
====================

This article describes how to batch delete entities in a collection.
Batch deletes require the use of a query string in the request, which
can either specify all entities in the collection or a subset of
entities to be deleted. For more information on queries, see [Basic
query syntax](/basic-query-syntax). Currently, collections cannot be
deleted in Apache Usergrid.

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Batch deleting entities in a collection
---------------------------------------

-   [cURL](#curl_delete_collection)
-   [iOS](#ios_delete_collection)
-   [Android](#android_delete_collection)
-   [JavaScript (HTML5)](#javascript_delete_collection)
-   [Ruby](#ruby_delete_collection)
-   [Node.js](#nodejs_delete_collection)

### Example Request/Response

#### Request:

    curl -X DELETE "https://api.usergrid.com/your-org/your-app/items/"

#### Response:

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

### Request Syntax

    curl -X DELETE https://api.usergrid.com/<org>/<app>/<collection>/?ql=

Note that you must include an empty query string (?ql=) at the end of
the URL

### Parameters

  Parameter    Description
  ------------ ----------------------------------------
  org          Organization UUID or organization name
  app          Application UUID or application name
  collection   Collection UUID or collection name

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)deleteCollection {

        NSString *url = @"https://api.usergrid.com/your-org/your-app/items/?ql";
        NSString *op = @"DELETE";
        NSString *opData = nil;
        
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

### SDK Method

    (ApigeeClientResponse *)apiRequest: (NSString *)url operation:(NSString *)op data:(NSString *)opData

### Properties

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| url                                  | A fully-formed url in the following  |
|                                      | format:                              |
|                                      |     https://api.usergrid.com/<org>/< |
|                                      | app>/<collection>/?ql=               |
|                                      |                                      |
|                                      | Note that you must include an empty  |
|                                      | '?ql=' query string at the end of    |
|                                      | the URL                              |
+--------------------------------------+--------------------------------------+
| op                                   | The HTTP method - in this case,      |
|                                      | 'DELETE'                             |
+--------------------------------------+--------------------------------------+
| opData                               | No data is being sent, so the value  |
|                                      | is `nil`                             |
+--------------------------------------+--------------------------------------+

Currently, deleting all entities in a collection is not supported by the
[Apache Usergrid SDK for Android](/app-services-sdks#android).

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
        method:"DELETE",
    }

    dataClient.request(options,function (error,response) {

        if (error) { 
            // Error
        } else { 
            // Success
        }

    });    
                    

#### Response:

The API will respond with 404 Resource Not Found.

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
|                                      |     the collection to be emptied,    |
|                                      |     appended by an empty query       |
|                                      |     string in the format:            |
|                                      |     \<collection\>/?ql=              |
|                                      | -   method: the HTTP method for the  |
|                                      |     request, in this case *DELETE*   |
+--------------------------------------+--------------------------------------+
| callback                             | Callback function to handle the API  |
|                                      | response                             |
+--------------------------------------+--------------------------------------+

### Example Request/Response

#### Request:

    #Create a client object
    usergrid_api = 'https://api.usergrid.com'
    organization = 'your-org'
    application = 'your-app'

    client = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

    begin
        client['item'].delete_query ""
    rescue
        #fail
    end
                    

#### Response:

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

### SDK Method

    delete_query <query_string>

### Properties

  Parameter       Description
  --------------- --------------------------------------------------------------------------------------------------------------------------------
  query\_string   A query string that specifies the entities to be deleted. Use an empty string (`""`) to delete all entities in the collection.

This example uses the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    //create the basic client object
    var dataClient = new Usergrid.client({
        orgName:'your-org',
        appName:'your-app'
    });

    //options for the request
    var options = {
        endpoint:"items/?ql=", //don't forget to append '/?ql='
        method:"DELETE"
    }

    dataClient.request(options,function (error,response) {

        if (error) { 
            // Error
        } else { 
            // Success
        }

    });     
                    

#### Response:

    { action: 'delete',
      application: 'f34f4222-a166-11e2-a7f7-02e81adcf3d0',
      params: { ql: [ '' ] },
      path: '/tests',
      uri: 'http://api.usergrid.com/your-org/your-app/items',
      entities: 
       [ { uuid: '5eac800a-1a61-11e3-95b8-4f685c4bb3d6',
           type: 'item',
           name: 'milk',
           price: '3.25',
           created: 1378849125376,
           modified: 1378849125376,
           metadata: [Object] },
         { uuid: '5eb77c8a-1a61-11e3-aae6-3be70698d378',
           type: 'item',
           name: 'bread',
           price: '4.00',
           created: 1378849125448,
           modified: 1378849125448,
           metadata: [Object] } ],
      timestamp: 1378849137959,
      duration: 648,
      organization: 'your-org',
      applicationName: 'your-app' }
                    

### SDK Method

    entity.destroy()

### Parameters

  Parameter   Description
  ----------- ----------------------------------------------
  callback    Callback function to handle the API response

 
