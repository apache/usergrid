# Usergrid iOS SDK

## Getting Started

### Installing the SDK 
### Building from Source

# Usergrid SDK Reference with Examples

The 66 topics listed below are each documented in the Usergrid documentation and 
for each the docs provide an API reference and example for each of these clients:
curl, iOS, Android, JavaScript, Ruby and Node.js.

## Working with Collections

### 1. Creating collections 

SDK Method

    (ApigeeClientResponse *)apiRequest: (NSString *)url operation:(NSString *)op data:(NSString *)opData
  
Parameters

Parameter Description
--------- ----------- 
url	      A fully-formed url in the following format: https://api.usergrid.com/<org>/<app>/<collection>
op	      The HTTP method - in this case, 'POST'
opData	  No data is being sent, so the value is nil

Example Request/Response

Request:

    -(NSString*)createCollection {

    NSString *url = @"https://api.usergrid.com/your-org/your-app/items";
    NSString *op = @"POST";
    NSString *opData = nil;

        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[ [UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient apiRequest: url operation: op data: opData];

    @try {
        //success
    }
    @catch (NSException * e) {
        //fail
    }

    }
				
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
    
### 2. Retrieving collections

SDK Method

    (ApigeeCollection*)getCollection:(NSString*)type
    
Parameters

Parameter	Description
---------   -----------
type	    The entity type associated with the collection to be retrieved

Example Request/Response

Request:

    -(NSString*)getCollection {

        //specify the entity type that corresponds to the collection to be retrieved
        NSString *type = @"item";
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios

        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[ [UIApplication sharedApplication] delegate];
        
        //Call getCollection: to initiate the API GET request 
        ApigeeCollection *collection = [appDelegate.dataClient getCollection:@"book"];	
    }
                        
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

### 3. Updating collections

SDK Method
(ApigeeClientResponse *)apiRequest: (NSString *)url operation:(NSString *)op data:(NSString *)opData
Properties
Parameter	Description
url	A fully-formed request url in the following format:
https://api.usergrid.com/<org>/<app>/<collection>/?ql=
Note that you must include an empty '?ql=' query string at the end of the URL

op	The HTTP method - in this case, 'PUT'
opData	A JSON-formatted string that contains the entity properties to be updated
Example Request/Response
Show Code
Request:
-(NSString*)updateCollection {

	NSString *url = @"https://api.usergrid.com/your-org/your-app/items/?ql";
	NSString *op = @"PUT";
	NSString *opData = @"{\"availability\":\"in-stock\"}"; //we escape the quotes
	
	//we recommend you call ApigeeClient from your AppDelegate. 
	//for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
	//create an instance of AppDelegate
	AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
	
	//call apiRequest to initiate the API call
	ApigeeClientResponse *response = [appDelegate.dataClient apiRequest: url operation: op data: opData];
	
	@try {
	    //success
	}
	@catch (NSException * e) {
	    //fail
	}

}
				
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

### 4. Deleting collections

SDK Method
(ApigeeClientResponse *)apiRequest: (NSString *)url operation:(NSString *)op data:(NSString *)opData
Properties
Parameter	Description
url	A fully-formed url in the following format:
https://api.usergrid.com/<org>/<app>/<collection>/?ql=
Note that you must include an empty '?ql=' query string at the end of the URL

op	The HTTP method - in this case, 'DELETE'
opData	No data is being sent, so the value is nil
Example Request/Response
The following example will delete the first 5 entities in a collection.

Show Code
Request:
-(NSString*)deleteCollection {

	NSString *url = @"https://api.usergrid.com/your-org/your-app/items/?ql='limit=5'";
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

## Working with Entities

### 5. Creating a custom entity

SDK Method
(ApigeeClientResponse *)createEntity:(NSDictionary *)newEntity
Parameters
Parameter	Description
newEntity	NSDictionary object that contains the entity properties
Example Request/Response
Show Code
Request:
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
	AppDelegate *appDelegate = (AppDelegate *)[ [UIApplication sharedApplication] delegate];
	
	//call createEntity to initiate the API call
	ApigeeClientResponse *response = [appDelegate.dataClient createEntity:entity];
	
	@try {	    
	    //success	    
	}
	@catch (NSException * e) {
	    //fail
	}
    
}
				
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
    
    
### 6. Creating multiple custom entities

Request Syntax
curl -X POST https://api.usergrid.com/<org>/<app>/<entity_type>/ -d '[{<entity>}, {<entity>}, ...]'
Parameters
Parameter	Description
org	Organization UUID or name
app	Application UUID or name
entity_type	Custom entity type to create. API Services will create a corresponding collection if one does not already exist. To add an entity to an existing collections, use the collection name or colleciton UUID in place of the entity type.
entity	Comma-separated list of entity objects to create. Each object should be formatted as a comma-separated list of entity properties, formatted as key-value pairs in the format <property>:<value>
Example Request/Response
Show Code
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

### 7. Creating an entity with sub-properties

SDK Method
(ApigeeClientResponse *)createEntity:(NSDictionary *)newEntity
Parameters
Parameter	Description
newEntity	NSMutableDictionary object that contains the entity properties
Example Request/Response
Show Code
Request:
-(NSString*)newEntity {
    
	//Initialize an object for the new entity to be created
	NSMutableDictionary *entity = [ [NSMutableDictionary alloc] init ];
	
	//Initialize an object for each nested variety object
	NSMutableDictionary *variety_1 = [ [NSMutableDictionary alloc] init ];
    NSMutableDictionary *variety_2 = [ [NSMutableDictionary alloc] init ];
    NSMutableDictionary *variety_3 = [ [NSMutableDictionary alloc] init ];
        
    //Initialize an array to hold the nested variety objects
    NSMutableArray *variety_list = [ [NSMutableArray alloc] init];
	
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
	AppDelegate *appDelegate = (AppDelegate *)[ [UIApplication sharedApplication] delegate];
	
	//call createEntity to initiate the API call
	ApigeeClientResponse *response = [appDelegate.dataClient createEntity:entity];
	
	@try {
	    //success
	}
	@catch (NSException * e) {
	    //fail
	}
    
}
				
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


### 8. Retrieving an entity

SDK Method

    (ApigeeClientResponse *)getEntities: (NSString *)endpoint query:(NSString *)query
    
Properties

Parameter	Description
---------   -----------
endpoint	The collection and entity identifier of the entity to be retrieved.
query	    An optional query string. Requests for a specific entity should set the value to nil
 
Endpoint exported in the following format: <collection>/<entity_UUID_or_name>

Example Request/Response

Request:

    -(NSString*)getEntity {

        //specify the entity collection and UUID or name to be retrieved	
        NSString *endpoint = @"items/b3aad0a4-f322-11e2-a9c1-999e12039f87";	
        
        NSString *query = nil;
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call getEntities to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient getEntities:endpoint queryString:query];
        
        @try {
            //success
        }
        
        @catch (NSException * e) {
            //fail
        }

    }				
				
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

### 9. Retrieving multiple entities

SDK Method

    (ApigeeClientResponse *)getEntities: (NSString *)type queryString:(NSString *)queryString
    
Properties

Parameter	Description
---------   -----------
type	    The entity type being retrieved
queryString	A query string of entity properties to be matched for the entities to be retrieved.
 
Query string is expected in the following format: <property>=<value> OR <property>=<value> OR ...

Example Request/Response

Request:

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


### 10. Updating an entity

SDK Method

    (ApigeeClientResponse *)updateEntity: (NSString *)entityID entity:(NSDictionary *)updatedEntity

Parameters

Parameter	    Description
---------       ----------- 
entityID	    UUID of the entity to be updated
updatedEntity	NSMutableDictionary containing the properties to be updated

Example Request/Response

Request:

    -(NSString*)updateEntity {

        //UUID of the entity to be updated
        NSString *entityID = @"f42752aa-08fe-11e3-8268-5bd5fa5f701f";
        
        //Create an entity object
        NSMutableDictionary *updatedEntity = [ [NSMutableDictionary alloc] init ];
        
        //Set entity properties to be updated
        [updatedEntity setObject:@"item" forKey:@"type"]; //Required - entity type
        [updatedEntity setObject:@"in-stock" forKey:@"availability"];
        [updatedEntity setObject:@"4.00" forKey:@"price"];

        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[ [UIApplication sharedApplication] delegate];
        
        //call updateEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient updateEntity:entityID entity:updatedEntity];

        @try {
            
           //success
            
        }
        @catch (NSException * e) {
            //fail
        }
        
    }
				
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


### 11. Updating a sub-property

SDK Method

    (ApigeeClientResponse *)updateEntity: (NSString *)entityID entity:(NSDictionary *)updatedEntity

Parameters

Parameter	    Description
---------       ----------- 
entityID	    UUID of the entity to be updated
updatedEntity	Entity object containing the properties to be updated

Example Request/Response

Request:

    -(NSString*)updateEntity {

        //UUID of the entity to be updated
        NSString *entityID = @"f42752aa-08fe-11e3-8268-5bd5fa5f701f";
            
        //Define our two sub-properties to include in the update
        NSMutableDictionary *subproperty1 = [ [NSMutableDictionary alloc] init];
        NSMutableDictionary *subproperty2 = [ [NSMutableDictionary alloc] init];
        [subproperty1 setObject:@"1%" forKey:@"name"];
        [subproperty1 setObject:@"3.25" forKey:@"price"];
        [subproperty2 setObject:@"whole" forKey:@"name"];
        [subproperty2 setObject:@"4.00" forKey:@"price"];
        
        //Put our sub-properties into an NSArray
        NSArray *subproperties = [ [NSArray alloc] initWithObjects:props1,props2, nil];

        //Create an NSMutableDictionary to hold our updates
        NSMutableDictionary *updatedEntity = [ [NSMutableDictionary alloc] init ];

        //Set the properties to be updated
        [updatedEntity setObject:@"item" forKey:@"type"]; //Required - entity type
        [updatedEntity setObject:props forKey:@"varieties"];
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[ [UIApplication sharedApplication] delegate];
        
        //call createEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient updateEntity:entityID entity:updatedEntity];

        @try {
            
           //success
            
        }
        @catch (NSException * e) {
            //fail
        }
        
    }
				
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


### 12. Deleting data entities

SDK Method

    (ApigeeClientResponse *)removeEntity: (NSString *)type entityID:(NSString *)entityID
    
Properties

Parameter	Description
---------   -----------
type	    The entity type being deleted
entityID	The UUID or name of the entity to be removed

Example Request/Response

Request:

    -(NSString*)deleteEntity {

        //specify the entity type to be deleted	
        NSString *type = @"item";
        
        //specify the uuid or name of the entity to be deleted
        NSString *entityId = @"milk";
        
        //we recommend you call ApigeeClient from your AppDelegate. 
        //for more information see the iOS SDK install guide: http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios
        //create an instance of AppDelegate
        AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        
        //call removeEntity to initiate the API call
        ApigeeClientResponse *response = [appDelegate.dataClient removeEntity:type entityID:entityId];
        
        @try {
            //success
        }
        @catch (NSException * e) {
            //fail
        }
    }
				
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


## Data Queries

### 13. Querying your data 

## Entity Connections

### 14. Connecting users other data
### 15. Retrieving user connection data
### 16. Disconnecting entities

## Permissions & Roles

### 17. Assigning permissions
### 18. Removing permissions
### 19. Assigning permissions
### 20. Removing permissions
### 21. Creating roles 
### 22. Assigning roles
### 23. Removing roles 
	
## Authentication
	
### 24. Application user authentication (user login)
### 25. Application client authentication
### 26. Admin user authentication
### 27. Organization client authentication
### 28. Revoking tokens (user logout)

# Working with Users & Groups

### 29. Creating users
### 30. Retrieving user data
### 31. Setting or updating password
### 32. Creating groups
### 33. Retrieving group data
### 34. Retrieving a group's users
### 35. Adding users groups
### 36. Deleting user group

## Activities & Feeds

### 37. Posting a user activity
### 38. Posting an activity to a group
### 39. Creating an activity for a user's followers in a group	
### 40. Retrieving a user's activity feed
### 41. Retrieving a group's activity feed

## Events & Counters

### 42. Creating & incrementing counters
### 43. Retrieving counters
### 44. Retrieving counters by time interval

## Managing Orgs & Apps

### 46. Creating an organization
### 47. Getting an organization
### 48. Activating an organization
### 49. Reactivating an organization
### 50. Generating organization client credentials
### 51. Retrieving organization client credentials
### 52. Getting an organization's activity feed
### 53. Getting the applications in an organization
### 54. Getting the admin users in an organization
### 55. Removing an admin user from an organization
### 56. Creating an organization application	
### 57. Generating application credentials
### 58. Getting application credentials

## Managing Admin Users

### 59. Creating an admin user
### 60. Updating an admin user
### 61. Getting an admin user
### 62. Setting an admin user's password
### 63. Resetting an admin user's password
### 64. Activating an admin user
### 65. Reactivating an admin user
### 66. Getting an admin user's activity feed
	
	