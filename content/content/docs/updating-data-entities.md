---
title: Updating Data Entities
category: docs
layout: docs
---

Updating Data Entities
======================

This article describes how to update entities in your Apache Usergrid
account.

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Updating an entity
------------------

One or more properties can be updated with a single PUT request. For
information on updating sub-properties, see [Updating
sub-properties](#update_nested) below.

-   [cURL](#curl_update_entity)
-   [iOS](#ios_update_entity)
-   [Android](#android_update_entity)
-   [JavaScript (HTML5)](#javascript_update_entity)
-   [Ruby](#ruby_update_entity)
-   [Node.js](#nodejs_update_entity)

### Example Request/Response

#### Request:

    curl -X PUT https://api.usergrid.com/your-org/your-app/items/milk -d '{"price":"4.00", "availability":"in-stock"}'

#### Response:

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

### Request Syntax

    curl -X PUT https://api.usergrid.com/<org>/<app>/<collection>/<entity> -d {<property>}

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
| entity                               | Entity UUID or entity name           |
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

    -(NSString*)updateEntity {

        //UUID of the entity to be updated
        NSString *entityID = @"f42752aa-08fe-11e3-8268-5bd5fa5f701f";
        
        //Create an entity object
        NSMutableDictionary *updatedEntity = [[NSMutableDictionary alloc] init ];
        
        //Set entity properties to be updated
        [updatedEntity setObject:@"item" forKey:@"type"]; //Required - entity type
        [updatedEntity setObject:@"in-stock" forKey:@"availability"];
        [updatedEntity setObject:@"4.00" forKey:@"price"];

        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient updateEntity:entityID entity:updatedEntity];

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

### SDK Method

    (ApigeeClientResponse *)updateEntity: (NSString *)entityID entity:(NSDictionary *)updatedEntity

### Parameters

  Parameter       Description
  --------------- -------------------------------------------------------------
  entityID        UUID of the entity to be updated
  updatedEntity   NSMutableDictionary containing the properties to be updated

This example uses the [Apache Usergrid Android
SDK](/app-services-sdks#android).

### Example Request/Response

#### Request:

    //Create client entity
    String ORGNAME = "your-org";
    String APPNAME = "your-app";        
    ApigeeClient apigeeClient = new ApigeeClient(ORGNAME,APPNAME);
    DataClient dataClient = apigeeClient.getDataClient();

    //Create properties object

    String entityID = "fd0def5a-091c-11e3-a60d-eb644ab154cc";

    Map<String, Object> updatedProperties = new HashMap<String, Object>();
    updatedProperties.put("type", "item"); //Required
    updatedProperties.put("availability", "in-stock");
    updatedProperties.put("price", "4.00");

    //call updateEntityAsync to initiate the asynchronous API call
    dataClient.updateEntityAsync(entityID, updatedProperties, new ApiResponseCallback() {   
        
        //If updateEntityAsync fails, catch the error
        @Override
        public void onException(Exception e) { 
            // Error
        }
        
        //If updateEntityAsync is successful, handle the response object
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

    {"action":"put","application":"f34f4222-a166-11e2-a7f7-02e81adcf3d0","entities":[{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"5bb76bca-1657-11e3-903f-9ff6c621a7a4","price":"4.00","created":1378405020796,"name":"milk","modified":1378748497900,"availability":"in-stock","metadata":{"path":"/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"}}],"params":{},"path":"/items","rawResponse":"{
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
        "modified" : 1378748497900,
        "availability" : "in-stock",
        "metadata" : {
          "path" : "/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"
        },
        "name" : "milk",
        "price" : "4.00"
      } ],
      "timestamp" : 1378748497887,
      "duration" : 80,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
    ","uri":"http://api.usergrid.com/your-org/your-app/items","timestamp":1378748497887,"entityCount":1,"firstEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"5bb76bca-1657-11e3-903f-9ff6c621a7a4","price":"4.00","created":1378405020796,"name":"milk","modified":1378748497900,"availability":"in-stock","metadata":{"path":"/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"}},"lastEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"5bb76bca-1657-11e3-903f-9ff6c621a7a4","price":"4.00","created":1378405020796,"name":"milk","modified":1378748497900,"availability":"in-stock","metadata":{"path":"/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"}},"organization":"your-org","duration":80,"applicationName":"your-app"}                
                    

### SDK Method

Asynchronous:

    updateEntityAsync(String entityID, Map<String, Object> updatedProperties, ApiResponseCallback callback)

Synchronous:

    updateEntity(String entityID, Map<String, Object> updatedProperties)

### Parameters

  Parameter           Description
  ------------------- ------------------------------------------------------------------------------------
  entityID            UUID of the entity to be updated
  updatedProperties   Map object containing the properties to be updated. A 'type' property is required.
  callback            Callback function (Asynchronous calls only)

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    //Create the Apache Usergrid client object
    var dataClient = new Usergrid.Client({
        orgName:'your-org',
        appName:'your-app'
    });

    //Specify the UUID of the entity to be updated
    //and the properties to be updated
    var properties = {
        client:dataClient, //Required
        data:{'type':'item',
        uuid:'b3aad0a4-f322-11e2-a9c1-999e12039f87', //UUID of the entity to be updated is required
        price:'4.00',
        availability:'in-stock'
        }
    };

    //Create a new entity object that contains the updated properties
    var entity = new Usergrid.Entity(properties);

    //Call Entity.save() to initiate the API PUT request
    entity.save(function (error,response) {

        if (error) { 
            //error
        } else { 
            //success
        }

    });    
                    

#### Response:

    Object {action: "put", application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0", params: Object, path: "/items", uri: "http://api.usergrid.com/your-org/your-app/items"…}
    action: "put"
    application: "f34f4222-a166-11e2-a7f7-02e8sd83f3d0"
    applicationName: "your-app"
    duration: 62
    entities: Array[1]
    0: Object
    created: 1374534114329
    metadata: Object
    modified: 1376693371847
    name: "milk"
    price: "4.00" //updated
    availability: "in-stock" //updated
    varieties: Array[3] //This property was already present 
    type: "item"
    uuid: "b3aad0a4-f322-11e2-a9c1-999e12039f87"
    varieties: Array[3]
    __proto__: Object
    length: 1
    __proto__: Array[0]
    organization: "your-org"
    params: Object
    path: "/items"
    timestamp: 1376693371836
    uri: "http://api.usergrid.com/your-org/your-app/items"              
                    

### SDK Method

    Entity.save();

This example uses the [Apache Usergrid RubyGem](/app-services-sdks#ruby).

### Example Request/Response

#### Request:

    #Create a client object
    usergrid_api = 'https://api.usergrid.com'
    organization = 'your-org'
    application = 'your-app'

    client = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

    begin

        # Specify the name or uuid of the collection and entity to be updated
        # and the entity properties to be updated
        entity = client['item']['b3aad0a4-f322-11e2-a9c1-999e12039f87'].entity #entity object
        entity.price = '4.00'
        entity.availability = 'in-stock'
        
        # Call save to initiate the API PUT request
        entity.save

    rescue

        #fail

    end
                    

#### Response:

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

### SDK Method

    save

The example assumes use of the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    //Create the Apache Usergrid client object
    var dataClient = new Usergrid.client({
        orgName:'your-org',
        appName:'your-app'
    });

    //Specify the UUID of the entity to be updated
    //and the properties to be updated
    var properties = {
        client:dataClient, //Required
        data:{'type':'item',
        uuid:'b3aad0a4-f322-11e2-a9c1-999e12039f87', //UUID of the entity to be updated is required
        price:'4.00',
        availability:'in-stock'
        }
    };

    //Create a new entity object the contains the updated properties
    var entity = new Usergrid.entity(properties);

    //Call Entity.save() to initiate the API PUT request
    entity.save(function (error,response) {

        if (error) { 
            //error
        } else { 
            //success
        }

    });             
                    

### Example - Response

    {
      "action" : "put",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/items",
      "uri" : "http://api.usergrid.com/your-org/your-app/items",
      "entities" : [ {
        "uuid" : "b3aad0a4-f322-11e2-a9c1-999e12039f87",
        "type" : "item",
        "name" : "milk",
        "created" : 1374534114329,
        "modified" : 1376695962803,
        "metadata" : {
          "path" : "/items/b3aad0a4-f322-11e2-a9c1-999e12039f87"
        },
        "name" : "milk",
        "price" : "4.00",
        "availability" : "in-stock"
      } ],
      "timestamp" : 1376695962790,
      "duration" : 144,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
                    

### SDK Method

    Entity.save();

Updating a sub-property
-----------------------

Data entities may contain sets of sub-properties as nested JSON objects.
Unlike normal entity properties, however, sub-properties cannot be
updated individually. Updating a nested object will cause all
sub-properties within it to be overwritten.

For example, if you have a data entity with the following nested object:

    "varieties": [
        {
          "name": "1%",
          "price": "3.25",
          "SKU": "0393847575533445"
        },
        {
          "name": "whole",
          "price": "3.85",
          "SKU": "0393394956788445"
        }
    ]       
            

and you send this update to Apache Usergrid:

    "varieties": [
        {
          "name": "2%",
          "price": "3.00",
        },
        {
          "price": "4.00",
        }
    ]       
            

this will be the resulting nested object:

    "varieties": [
        {
          "name": "2%",
          "price": "3.00",
        },
        {
          "price": "4.00",
        }
    ]       
            

-   [cURL](#curl_update_subproperty)
-   [iOS](#ios_update_subproperty)
-   [Android](#android_update_subproperty)
-   [JavaScript (HTML5)](#javascript_update_subproperty)
-   [Ruby](#ruby_update_subproperty)
-   [Node.js](#nodejs_update_subproperty)

### Example Request/Response

#### Request:

    curl -X PUT https://api.usergrid.com/your-org/your-app/items/milk -d '{"varieties":[{"name":"1%","price":"3.25"},{"name":"whole","price":"4.00"}]}'

#### Response:

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

### Request Syntax

    curl -X PUT https://api.usergrid.com/<org_id>/<app_id>/<collection>/<uuid|name> -d '{<property> : [{<sub_property>}, {<sub_property>}...]}'

### Parameters

  ---------------------------------------------------------------------------------------------------------------
  Parameter          Description
  ------------------ --------------------------------------------------------------------------------------------
  org\_id            Organization UUID or organization name

  app\_id            Application UUID or application name

  collection         Name of the collection containing the entity to be updated

  uuid|name          UUID or name of the data entity to be updated

  entity\_property   The name of the entity property that contains the nested object to be updated

  sub\_property      Entity properties of the nested object, as a set of key-value pairs in the format:\
                     *{\<property\_name\> : \<property\_value\>, \<property\_name\> : \<property\_value\> ...}*
  ---------------------------------------------------------------------------------------------------------------

This example uses the [Apache Usergrid iOS SDK](/app-services-sdks#ios).

### Example Request/Response

#### Request:

    -(NSString*)updateEntity {

        //UUID of the entity to be updated
        NSString *entityID = @"f42752aa-08fe-11e3-8268-5bd5fa5f701f";
            
        //Define our two sub-properties to include in the update
        NSMutableDictionary *subproperty1 = [[NSMutableDictionary alloc] init];
        NSMutableDictionary *subproperty2 = [[NSMutableDictionary alloc] init];
        [subproperty1 setObject:@"1%" forKey:@"name"];
        [subproperty1 setObject:@"3.25" forKey:@"price"];
        [subproperty2 setObject:@"whole" forKey:@"name"];
        [subproperty2 setObject:@"4.00" forKey:@"price"];
        
        //Put our sub-properties into an NSArray
        NSArray *subproperties = [[NSArray alloc] initWithObjects:props1,props2, nil];

        //Create an NSMutableDictionary to hold our updates
        NSMutableDictionary *updatedEntity = [[NSMutableDictionary alloc] init ];

        //Set the properties to be updated
        [updatedEntity setObject:@"item" forKey:@"type"]; //Required - entity type
        [updatedEntity setObject:props forKey:@"varieties"];
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient updateEntity:entityID entity:updatedEntity];

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

### SDK Method

    (ApigeeClientResponse *)updateEntity: (NSString *)entityID entity:(NSDictionary *)updatedEntity

### Parameters

  Parameter       Description
  --------------- -------------------------------------------------------
  entityID        UUID of the entity to be updated
  updatedEntity   Entity object containing the properties to be updated

This example uses the [Apache Usergrid Android
SDK](/app-services-sdks#android).

### Example Request/Response

#### Request:

    //Create client entity
    String ORGNAME = "your-org";
    String APPNAME = "your-app";        
    ApigeeClient apigeeClient = new ApigeeClient(ORGNAME,APPNAME);
    DataClient dataClient = apigeeClient.getDataClient();

    //UUID of the entity to be updated
    String entityID = "1ceed6ba-1b13-11e3-a7a6-59ffaee069e1";

    //The object we will pass to the API
    Map<String,Object> entityUpdates = new HashMap<String,Object>();

    //The objects we will need to setup the sub-properties
    ArrayList<Map<String,Object>> subPropertyArray = new ArrayList<Map<String,Object>>();
    Map<String,Object> subProperty1 = new HashMap<String,Object>();
    Map<String,Object> subProperty2 = new HashMap<String,Object>();

    //First sub-property
    subProperty1.put("name", "1%");
    subProperty2.put("price", "3.25");

    //Second sub-property
    subProperty2.put("name", "whole");
    subProperty2.put("price", "4.00");

    //Add the sub-properties to the List object
    subPropertyArray.add(subProperty1);
    subPropertyArray.add(subProperty2);

    //Now we put it all together
    entityUpdates.put("type", "item"); //Required
    entityUpdates.put("varieties", subPropertyArray);

            
    //call updateEntityAsync to initiate the asynchronous API call
    dataClient.updateEntityAsync(entityID, updatedProperties, new ApiResponseCallback() {   
        
        //If updateEntityAsync fails, catch the error
        @Override
        public void onException(Exception e) { 
            // Error
        }
        
        //If updateEntityAsync is successful, handle the response object
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

    {"action":"put","application":"f34f4222-a166-11e2-a7f7-02e81adcf3d0","entities":[{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"1ceed6ba-1b13-11e3-a7a6-59ffaee069e1","varieties":[{"name":"1%","price":"3.25"},{"name":"whole","price":"4.00"}],"created":1378925465499,"name":"milk","modified":1378936578609,"metadata":{"path":"/items/1ceed6ba-1b13-11e3-a7a6-59ffaee069e1"}}],"params":{},"path":"/items","rawResponse":"{
      "action" : "put",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/items",
      "uri" : "http://api.usergrid.com/your-org/your-app/items",
      "entities" : [ {
        "uuid" : "1ceed6ba-1b13-11e3-a7a6-59ffaee069e1",
        "type" : "item",
        "name" : "milk",
        "created" : 1378925465499,
        "modified" : 1378936578609,
        "metadata" : {
          "path" : "/items/1ceed6ba-1b13-11e3-a7a6-59ffaee069e1"
        },
        "name" : "milk",
        "varieties" : [ {
          "name" : "1%",
          "price" : "3.25"
        }, {
          "name" : "whole",
          "price" : "4.00"
        } ]
      } ],
      "timestamp" : 1378936578595,
      "duration" : 75,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
    ","uri":"http://api.usergrid.com/your-org/your-app/items","timestamp":1378936578595,"entityCount":1,"firstEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"1ceed6ba-1b13-11e3-a7a6-59ffaee069e1","varieties":[{"name":"1%","price":"3.25"},{"name":"whole","price":"4.00"}],"created":1378925465499,"name":"milk","modified":1378936578609,"metadata":{"path":"/items/1ceed6ba-1b13-11e3-a7a6-59ffaee069e1"}},"lastEntity":{"dataClient":{"accessToken":null,"apiUrl":"https://api.usergrid.com","applicationId":"your-app","clientId":null,"clientSecret":null,"currentOrganization":null,"loggedInUser":null,"organizationId":"your-org"},"type":"item","uuid":"1ceed6ba-1b13-11e3-a7a6-59ffaee069e1","varieties":[{"name":"1%","price":"3.25"},{"name":"whole","price":"4.00"}],"created":1378925465499,"name":"milk","modified":1378936578609,"metadata":{"path":"/items/1ceed6ba-1b13-11e3-a7a6-59ffaee069e1"}},"organization":"your-org","duration":75,"applicationName":"your-app"}
                    

### SDK Method

Asynchronous:

    updateEntityAsync(String entityID, Map<String, Object> updatedProperties, ApiResponseCallback callback)

Synchronous:

    updateEntity(String entityID, Map<String, Object> updatedProperties)

### Parameters

  Parameter           Description
  ------------------- ------------------------------------------------------------------------------------
  entityID            UUID of the entity to be updated
  updatedProperties   Map object containing the properties to be updated. A 'type' property is required.
  callback            Callback function (Asynchronous calls only)

This example uses the [Apache Usergrid JavaScript (HTML5)
SDK](/app-services-sdks#javascript).

### Example Request/Response

#### Request:

    //Create the Apache Usergrid client object
    var dataClient = new Usergrid.Client({
        orgName:'your-org',
        appName:'your-app'
    });

    //Specify the UUID of the entity to be updated
    //and the properties to be updated
    var properties = {
        client:dataClient, //Required
        data:{
            type:'item',
            uuid:'b151ddba-0921-11e3-9f60-2ba945ba461f',
            varieties:[
                {"name":"3%", "price":"3.25", "SKU":"9384752200033"},
                {"name":"whole", "price":"4.00", "SKU":"9384752200033"}
            ]
        }
    };

    //Create a new entity object that contains the updated properties
    var entity = new Usergrid.Entity(properties);

    //Call Entity.save() to initiate the API PUT request
    entity.save(function (error,response) {

        if (error) { 
            //error
        } else { 
            //success
        }

    });    
                    

#### Response:

    Object {action: "put", application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0", params: Object, path: "/items", uri: "http://api.usergrid.com/your-org/your-app/items"…}
    action: "put"
    application: "f34f4222-a166-11e2-a7f7-02e81adcf3d0"
    applicationName: "your-app"
    duration: 66
    entities: Array[1]
        0: Object
        availability: "in-stock"
        created: 1378405020796
        metadata: Object
        modified: 1378760239203
        name: "milk"
        price: "4.00"
        type: "item"
        uri: "http://api.usergrid.com/your-org/your-app/items/5bb76bca-1657-11e3-903f-9ff6c621a7a4"
        uuid: "5bb76bca-1657-11e3-903f-9ff6c621a7a4"
        varieties: Array[2]
            0: Object
                SKU: "9384752200033"
                name: "3%"
                price: "3.25"
                __proto__: Object
            1: Object
                SKU: "9384752200033"
                name: "whole"
                price: "4.00"
                __proto__: Object
            length: 2
            __proto__: Array[0]
        __proto__: Object
        length: 1
    __proto__: Array[0]
    organization: "your-org"
    params: Object
    path: "/items"
    timestamp: 1378760239191
    uri: "http://api.usergrid.com/your-org/your-app/items"
    __proto__: Object
                    

### SDK Method

    Entity.save();

This example uses the [Apache Usergrid RubyGem](/app-services-sdks#ruby).

### Example Request/Response

#### Request:

    #Create a client object
    usergrid_api = 'https://api.usergrid.com'
    organization = 'your-org'
    application = 'your-app'

    client = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

    begin

        # Specify the name or uuid of the collection and entity to be updated
        # and the entity properties to be updated
        entity = client['item']['b3aad0a4-f322-11e2-a9c1-999e12039f87'].entity #entity object
        entity.varieties = [
                {
                    "name" => "1%",
                    "price" => "3.25",
                    "sku" => "0393847575533445"
                },{
                    "name" => "whole",
                    "price" => "3.85",
                    "sku" => "0393394956788445"
                }
            ]
        
        # Call save to initiate the API PUT request
        entity.save

    rescue

        #fail

    end
                    

#### Response:

[[nid:11954]

### SDK Method

    save

This example uses the [Apache Usergrid Node.js
module](/app-services-sdks#nodejs).

### Example Request/Response

#### Request:

    //Create the Apache Usergrid client object
    var dataClient = new Usergrid.client({
        orgName:'your-org',
        appName:'your-app'
    });

    //Specify the UUID of the entity to be updated
    //and the properties to be updated

    var properties = {
        client:dataClient, //Required
        data:{
            type:'item',
            uuid:'b3aad0a4-f322-11e2-a9c1-999e12039f87', //UUID of the entity to be updated is required
            varieties : [
                {
                    "name" : "1%",
                    "price" : "3.25",
                    "sku" : "0393847575533445"
                },{
                    "name" : "whole",
                    "price" : "3.85",
                    "sku" : "0393394956788445"
                },{
                    "name" : "skim",
                    "price" : "4.00",
                    "sku" : "0390299933488445"      
                }
            ]           
        }
    };

    //Create a new entity object the contains the updated properties
    var entity = new Usergrid.entity(properties);

    //Call Entity.save() to initiate the API PUT request
    entity.save(function (error,response) {

        if (error) { 
            //error
        } else { 
            //success
        }

    });             
                

#### Response:

    { action: 'put',
      application: 'f34f4222-a166-11e2-a7f7-02e81adcf3d0',
      params: {},
      path: '/items',
      uri: 'http://api.usergrid.com/your-org/your-app/items',
      entities: 
       [ { uuid: 'b3aad0a4-f322-11e2-a9c1-999e12039f87',
           type: 'item',
           created: 1374534114329,
           modified: 1377039726738,
           metadata: [Object],
           varieties: [Object] } ],
      timestamp: 1377039726724,
      duration: 75,
      organization: 'your-org',
      applicationName: 'your-app' }
                

### SDK Method

    Entity.save();
