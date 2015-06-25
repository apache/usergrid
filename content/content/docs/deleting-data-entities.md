---
title: Deleting Data Entities
category: docs
layout: docs
---

Deleting Data Entities
======================

This article describes how to delete data entities.

**Note:**Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Deleting an entity
------------------

-   [cURL](#curl_delete_entity)
-   [iOS](#ios_delete_entity)
-   [Android](#android_delete_entity)
-   [JavaScript (HTML5)](#javascript_delete_entity)
-   [Ruby](#ruby_delete_entity)
-   [Node.js](#nodejs_delete_entity)

### Example Request/Response

#### Request:

Delete by UUID:

    curl -X DELETE "https://api.usergrid.com/your-org/your-app/items/da4a50dc-38dc-11e2-b2e4-02e81adcf3d0"

Delete by 'name' property:

    curl -X DELETE "https://api.usergrid.com/your-org/your-app/items/milk"

#### Response:

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

### Request Syntax

    curl -X DELETE https://api.usergrid.com/<org>/<app>/<collection>/<entity>

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

    -(NSString*)deleteEntity {

        //specify the entity type to be deleted 
        NSString *type = @"item";
        
        //specify the uuid or name of the entity to be deleted
        NSString *entityId = @"milk";
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient removeEntity:type entityID:entityId];
        
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

### SDK Method

    (ApigeeClientResponse *)removeEntity: (NSString *)type entityID:(NSString *)entityID

### Properties

  Parameter   Description
  ----------- ----------------------------------------------
  type        The entity type being deleted
  entityID    The UUID or name of the entity to be removed

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
    String uuid = "b3aad0a4-f322-11e2-a9c1-999e12039f87";
        
    //call removeEntityAsync to initiate the asynchronous API call    
    dataClient.removeEntityAsync(type, uuid, new ApiResponseCallback() {    

    //If removeEntityAsync fails, catch the error
    @Override
    public void onException(Exception e) { 
        // Error
    }

    //If removeEntityAsync is successful, handle the response object
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

    {"action":"delete","application":"f34f4222-a166-11e2-a7f7-02e81adcf3d0","entities":[{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"d1511d7a-19a1-11e3-b02b-cd5b309a29fa","created":1378766854343,"name":"milk","modified":1378766854343,"metadata":{"path":"/items/d1511d7a-19a1-11e3-b02b-cd5b309a29fa"}}],"params":{},"path":"/items","rawResponse":"{
      "action" : "delete",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/items",
      "uri" : "http://api.usergrid.com/your-org/your-app/items",
      "entities" : [ {
        "uuid" : "d1511d7a-19a1-11e3-b02b-cd5b309a29fa",
        "type" : "item",
        "name" : "milk",
        "created" : 1378766854343,
        "modified" : 1378766854343,
        "metadata" : {
          "path" : "/items/d1511d7a-19a1-11e3-b02b-cd5b309a29fa"
        },
        "name" : "milk",
        "price" : "3.25"   
      } ],
      "timestamp" : 1378767852615,
      "duration" : 276,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
    ","uri":"http://api.usergrid.com/your-org/your-app/items","timestamp":1378767852615,"entityCount":1,"firstEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"d1511d7a-19a1-11e3-b02b-cd5b309a29fa","created":1378766854343,"name":"milk","modified":1378766854343,"metadata":{"path":"/items/d1511d7a-19a1-11e3-b02b-cd5b309a29fa"}},"lastEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"d1511d7a-19a1-11e3-b02b-cd5b309a29fa","created":1378766854343,"name":"milk","price" : "3.25","modified":1378766854343,"metadata":{"path":"/items/d1511d7a-19a1-11e3-b02b-cd5b309a29fa"}},"organization":"your-org","duration":276,"applicationName":"your-app"}
                    

### SDK Method

    removeEntity(String entityType, String entityID)

### Properties

  Parameter    Description
  ------------ ----------------------------------------------
  entityType   The entity type of the entity to be deleted
  entityID     The UUID or name of the entity to be deleted

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    //create the basic client object
    var dataClient = new Usergrid.Client({
        orgName:'your-org',
        appName:'your-app'
    });

    //specify the properties of the entity to be deleted
    //type is required. UUID or name of the entity to be deleted is also required
        var properties = {
        client:client,
        data:{'type':'item',
        uuid:'39d25cca-03ad-11e3-a25d-71468ad53e11'
        }
    };

    //create the entity object
    var entity = new Usergrid.Entity(properties);

    //call destroy() to initiate the API DELETE request
    entity.destroy(function (error) {

    if (error) { 
        // Error
    } else {
        // Success
    }

    });     
                    

#### Response:

The API will respond with 404 Resource Not Found.

### SDK Method

    entity.destroy(callback)

### Parameters

  Parameter   Description
  ----------- ----------------------------------------------
  callback    Callback function to handle the API response

### Example Request/Response

#### Request:

    #Create a client object
    usergrid_api = 'https://api.usergrid.com'
    organization = 'your-org'
    application = 'your-app'

    client = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

    begin
        client['items']['milk'].entity.delete
    rescue
        #fail
    end
                    

#### Response:

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

### SDK Method

    delete

This example uses the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    //create the basic client object
    var dataClient = new Usergrid.client({
    orgName:'your-org',
    appName:'your-app'
    });

    //specify the properties of the entity to be deleted
    //type is required. UUID or name of the entity to be deleted is also required
    var properties = {
        client:dataClient,
        data:{
            'type':'item',
            'uuid':'39d25cca-03ad-11e3-a25d-71468ad53e11'
        }
    };

    //create the entity object
    var entity = new Usergrid.entity(properties);

    //call destroy() to initiate the API DELETE request
    entity.destroy(function (error) {

        if (error) { 
            // Error
        } else {
            // Success
        }

    });     
                    

#### Response:

    { action: 'delete',
      application: 'f34f4222-a166-11e2-a7f7-02e81adcf3d0',
      params: {},
      path: '/items',
      uri: 'http://api.usergrid.com/your-org/your-app/items',
      entities: 
       [ { uuid: 'f97c35ea-1a5b-11e3-b8a1-6f428da9ad88',
           type: 'item',
           name: 'milk',
           price: '3.25',
           created: 1378846808126,
           modified: 1378846808126,
           metadata: [Object] } ],
      timestamp: 1378847145757,
      duration: 285,
      organization: 'your-org',
      applicationName: 'your-app' }             
                    

### SDK Method

    entity.destroy(callback)

### Parameters

  Parameter   Description
  ----------- ----------------------------------------------
  callback    Callback function to handle the API response

 
