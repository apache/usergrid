---
title: Creating Collections
category: docs
layout: docs
---

Creating Collections
====================

This article describes how to create collections in Apache Usergrid. In App
Services, all entities are automatically associated with a corresponding
collection based on the `type` property of the entity. You may create
empty collections if you wish, but creating an entity of a new type will
automatically create a corresponding collection for you. For example,
creating a new custom "item" entity, creates an "items" collection.

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Creating a collection
---------------------

-   [Admin Portal](#portal_create_collection)
-   [cURL](#curl_create_collection)
-   [iOS](#ios_create_collection)
-   [Android](#android_create_collection)
-   [JavaScript](#javascript_create_collection)
-   [Ruby](#ruby_create_collection)
-   [Node.js](#nodejs_create_collection)

The easiest way to create a new, empty collection is by using the *Data
Explorer* tool in the Apache Usergrid Admin Portal by doing the following:

1.  [Login](https://www.apigee.com/usergrid) to the Apache Usergrid Admin
    Portal.
2.  In the left menu, click *Data Explorer*.
3.  In the middle column, click the *Add Collection* button.
4.  In the form, enter the name for the collection you want to create.
    If the provided value is not a plural word, Apache Usergrid will
    pluralize it.
5.  Click *Create*. The new collection will appear in the list.

### Example Request/Response

#### Request:

    curl -X POST "https://api.usergrid.com/your-org/your-app/item"

#### Response:

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

### Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/<collection_name>

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| org                                  | Organization UUID or organization    |
|                                      | name                                 |
+--------------------------------------+--------------------------------------+
| app                                  | Application UUID or application name |
+--------------------------------------+--------------------------------------+
| collection\_name                     | Name of the collection to create. If |
|                                      | the provided value is not a plural   |
|                                      | word, Apache Usergrid will pluralize    |
|                                      | it.                                  |
|                                      |                                      |
|                                      | For example, providing 'item' will   |
|                                      | create a collection named 'items'    |
|                                      | but providing 'items' will not       |
|                                      | create 'itemses'.                    |
+--------------------------------------+--------------------------------------+

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)createCollection {

    NSString *url = @"https://api.usergrid.com/your-org/your-app/items";
    NSString *op = @"POST";
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

### SDK Method

    (ApigeeClientResponse *)apiRequest: (NSString *)url operation:(NSString *)op data:(NSString *)opData

### Properties

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| url                                  | A fully-formed url in the following  |
|                                      | format:                              |
|                                      |     https://api.usergrid.com/<org>/< |
|                                      | app>/<collection>                    |
+--------------------------------------+--------------------------------------+
| op                                   | The HTTP method - in this case,      |
|                                      | 'POST'                               |
+--------------------------------------+--------------------------------------+
| opData                               | No data is being sent, so the value  |
|                                      | is `nil`                             |
+--------------------------------------+--------------------------------------+

Currently, creating an empty collection is not supported by the [App
Services SDK for Android](/app-services-sdks#android).

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
    endpoint:"items",
    method:"POST"
    }

    dataClient.request(options,function (error,response) {

    if (error) { 
        // Error
    } else { 
        // Success
    }

    });    
                    

#### Response:

    Object {action: "post", application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0", params: Object, path: "/items", uri: "http://api.usergrid.com/your-org/your-app/items"…}
    action: "post"
    application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0"
    applicationName: "your-app"
    duration: 29
    entities: Array[0]
    organization: "your-org"
    params: Object
    path: "/items"
    timestamp: 1378872945962
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
|                                      | -   endpoint: the name of the        |
|                                      |     collection to be created         |
|                                      | -   method: the HTTP method for the  |
|                                      |     request, in this case *POST*     |
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
        # Call create_entity to initiate the API call
        # By specifying 'nil' for the request body, we get an empty collection
        client.create_entity('item', nil)
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
      "entities" : [ ],
      "timestamp" : 1378857079220,
      "duration" : 31,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }

### SDK Method

    create_entity(type, properties)

### Properties

  Parameter    Description
  ------------ -----------------------------------------------------------------
  type         Custom entity type that will correspond to the collection
  properties   Object that contains the entity properties – in this case 'nil'

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
    endpoint:"items",
    method:"POST"
    }

    dataClient.request(options,function (error,response) {

        if (error) { 
            // Error
        } else { 
            // Success
        }

    });     
                    

#### Response:

    { action: 'post',
      application: 'f34f4222-a166-11e2-a7f7-02e81adcf3d0',
      params: {},
      path: '/items',
      uri: 'http://api.usergrid.com/your-org/your-app/items',
      entities: [],
      timestamp: 1378873689426,
      duration: 29,
      organization: 'your-org',
      applicationName: 'your-app' }
                    

### SDK Method

    entity.request(options,callback)

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| options                              | A JSON-formatted object containing   |
|                                      | the following properties:            |
|                                      |                                      |
|                                      | -   endpoint: the name of the        |
|                                      |     collection to be created         |
|                                      | -   method: the HTTP method for the  |
|                                      |     request, in this case *POST*     |
+--------------------------------------+--------------------------------------+
| callback                             | Callback function to handle the API  |
|                                      | response                             |
+--------------------------------------+--------------------------------------+

 
