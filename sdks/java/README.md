# Usergrid Java SDK

Usergrid SDK written for Java 

## Initialization

There are two different ways of initializing the Usergrid Java SDK: 

1. The singleton pattern is both convenient and enables the developer to use a globally available and always-initialized instance of Usergrid. 

```java
Usergrid.initSharedInstance("orgId", "appId");
```

2. The Instance pattern enables the developer to manage instances of the Usergrid client independently and in an isolated fashion. The primary use-case for this is when an application connects to multiple Usergrid targets.

```java
UsergridClient client = new UsergridClient("orgId","appId");
```

_Note: Examples in this readme assume you are using the `Usergrid` shared instance. If you've implemented the instance pattern instead, simply replace `Usergrid` with your client instance variable._

## RESTful operations

When making any RESTful call, a `type` parameter (or `path`) is always required. Whether you specify this as an argument or in an object as a parameter is up to you.

### GET

- To get entities in a collection:

```java
UsergridResponse response = Usergrid.GET("collection");
List<UsergridEntity> entities = response.getEntities();
```

- To get a specific entity in a collection by uuid or name:

```java
UsergridResponse response = Usergrid.GET("collection","<uuid-or-name>");
UsergridEntity entities = response.entity();
```

- To get specific entities in a collection by passing a `UsergridQuery` object:

```java
UsergridQuery query = new UsergridQuery("cats").gt("weight", 2.4)
                                 	.contains("color", "bl*")
                                 .not()
                                 .eq("color", "blue")
                                 .or()
                                 .eq("color", "orange");
	
// this will build out the following query:
// select * where weight > 2.4 and color contains 'bl*' and not color = 'blue' or color = 'orange'
	
UsergridResponse response = Usergrid.GET(query);
List<UsergridEntity> entities = response.getEntities();
```

### POST and PUT

POST and PUT requests both require a JSON body payload. You can pass either a Java object or a `UsergridEntity` instance. While the former works in principle, best practise is to use a `UsergridEntity` wherever practical. When an entity has a uuid or name property and already exists on the server, use a PUT request to update it. If it does not, use POST to create it.

- To create a new entity in a collection (**POST**):

```java
HashMap<String,String> propertyMap = new HashMap<>();
propertyMap.put("cuisine","pizza");
UsergridEntity entity = new UsergridEntity("restaurant","Dino's Deep Dish", propertyMap);	
Usergrid.POST(entity); // entity should now have a uuid property and be created
	
// you can also POST an array of entities:
	
HashMap<String,String> propertyMap = new HashMap<>();
propertyMap.put("cuisine","pizza");

ArrayList<UsergridEntity> entities = new ArrayList<>();
entities.add(new UsergridEntity("restaurant","Dino's Deep Dish", propertyMap));
entities.add(new UsergridEntity("restaurant","Pizza da Napoli", propertyMap));
UsergridResponse response = Usergrid.POST(entities);
List<UsergridEntity> responseEntities = response.getEntities(); // responseEntities should now contain now valid posted entities.
```

- To update an entity in a collection (**PUT**):

```java
HashMap<String,String> propertyMap = new HashMap<>();
propertyMap.put("cuisine","pizza");
UsergridEntity entity = new UsergridEntity("restaurant","Dino's Deep Dish", propertyMap);	
UsergridResponse response = Usergrid.POST(entity);
if( response.ok() ) {
	entity.putProperty("owner","Mia Carrara");
	Usergrid.PUT(entity); // entity now has the property 'owner'
}
	
// or update a set of entities by passing a UsergridQuery object

HashMap<String,String> propertiesToUpdate = new HashMap<>();
propertiesToUpdate.put("cuisine","pizza");
UsergridQuery query = new UsergridQuery("restaurants").eq("cuisine","italian");

UsergridResponse response = Usergrid.PUT(query, propertiesToUpdate);
	
    /* the first 10 entities matching this query criteria will be updated:
    e.g.:
        [
            {
                "type": "restaurant",
                "restaurant": "Il Tarazzo",
                "cuisine": "italian",
                "keywords": ["pasta"]
            },
            {
                "type": "restaurant",
                "restaurant": "Cono Sur Pizza & Pasta",
                "cuisine": "italian",
                "keywords": ["pasta"]
            }
        ]
    */
```

### DELETE

DELETE requests require either a specific entity or a `UsergridQuery` object to be passed as an argument.

- To delete a specific entity in a collection by uuid or name:

```java
UsergridResponse response = Usergrid.DELETE("collection", "<uuid-or-name>"); // if successful, entity will now be deleted
```

- To specific entities in a collection to delete by passing a `UsergridQuery` object:

```java
UsergridQuery query = new UsergridQuery("cats").eq("color","black").or().eq("color","white");
	
// this will build out the following query:
// select * where color = 'black' or color = 'white'
	
UsergridResponse response = Usergrid.DELETE(query); // the first 10 entities matching this query criteria will be deleted
```

## Entity operations and convenience methods

`UsergridEntity` has a number of helper/convenience methods to make working with entities more convenient.

### reload()

Reloads the entity from the server:

```java
entity.reload(); // entity is now reloaded from the server
```

### save()

Saves (or creates) the entity on the server:


```java
entity.putProperty("aNewProperty","A new value");
entity.save(); // entity is now updated on the server
```

### remove()

Deletes the entity from the server:

```java
entity.remove(); // entity is now deleted on the server and the local instance should be destroyed
```

## Authentication, current user, and auth-fallback

### appAuth and authenticateApp()

`Usergrid` can use the app client ID and secret that were passed upon initialization and automatically retrieve an app-level token for these credentials.

```java
Usergrid.setAppAuth(new UsergridAppAuth("<client-id>", "<client-secret>"));
Usergrid.authenticateApp(); // Usergrid.appAuth is authenticated automatically when this call is successful
```

### currentUser, userAuth,  and authenticateUser()

`Usergrid` has a special `currentUser` property. 

By default, when calling `authenticateUser()`, `.currentUser` will be set to this user if the authentication flow is successful.

```java
UsergridUserAuth userAuth = new UsergridUserAuth("<username>","<password>");
Usergrid.authenticateUser(userAuth); // Usergrid.currentUser is set to the authenticated user and the token is stored within that context
```
    
If you want to utilize authenticateUser without setting as the current user, simply pass a `false` boolean value as the second parameter:

```java
UsergridUserAuth userAuth = new UsergridUserAuth("<username>","<password>");
Usergrid.authenticateUser(userAuth,false); // user is authenticated but Usergrid.currentUser is not set.
```

### authMode

Auth-mode defines what the client should pass in for the authorization header. 

By default, `Usergrid.authMode` is set to `.User`, when a `Usergrid.currentUser` is present and authenticated, an API call will be performed using the token for the user. 

If `Usergrid.authMode` is set to `.None`, all API calls will be performed unauthenticated. 

If instead `Usergrid.authMode` is set to `.App`, the API call will instead be performed using client credentials, _if_ they're available (i.e. `authenticateApp()` was performed at some point). 

### usingAuth()

At times it is desireable to have complete, granular control over the authentication context of an API call. 

To facilitate this, the passthrough function `.usingAuth()` allows you to pre-define the auth context of the next API call.

```java
// assume Usergrid.authMode = UsergridAuthMode.NONE.

Map<String, String> permissionsMap = new HashMap<>();
permissionsMap.put("permission","get,post,put,delete:/**");
UsergridResponse response = Usergrid.usingAuth(Usergrid.getAppAuth()).POST("roles/guest/permissions",permissionsMap);

// here we've temporarily used the client credentials to modify permissions
// subsequent calls will not use this auth context
```

## User operations and convenience methods

`UsergridUser` has a number of helper/convenience methods to make working with user entities more convenient. If you are _not_ utilizing the `Usergrid` shared instance, you must pass an instance of `UsergridClient` as the first argument to any of these helper methods.
    
### create()

Creating a new user:

```java
UsergridUser user = new UsergridUser("username","password");
user.create(); // user has now been created and should have a valid uuid
```

### login()

A simpler means of retrieving a user-level token:

```java
user.login("username","password"); // user is now logged in
```

### logout()

Logs out the selected user. You can also use this convenience method on `Usergrid.currentUser`.

```java
user.logout(); // user is now logged out
```

### resetPassword()

Resets the password for the selected user.

```java
// if it was done correctly, the new password will be changed
user.resetPassword("oldPassword", "newPassword");
```

### UsergridUser.CheckAvailable()

This is a class (static) method that allows you to check whether a username or email address is available or not.

```java
boolean available = UsergridUser.checkAvailable("email", null); // 'available' == whether an email already exists for a user

available = UsergridUser.checkAvailable(null, "username"); // 'available' == whether an username already exists for a user

available = UsergridUser.checkAvailable("email", "username"); // 'available' == whether an email or username already exist for a user
```

## Querying and filtering data

### UsergridQuery initialization

The `UsergridQuery` class allows you to build out complex query filters using the Usergrid [query syntax](http://docs.apigee.com/app-services/content/querying-your-data).

The first parameter of the `UsergridQuery` builder pattern should be the collection (or type) you intend to query. You can either pass this as an argument, or as the first builder object:

```java
UsergridQuery query = new UsergridQuery("cats");
// or
UsergridQuery query = new UsergridQuery().collection("cats");
```

You then can layer on additional queries:

```java
UsergridQuery query = new UsergridQuery("cats").gt("weight",2.4).contains("color","bl*")
                                 .not()
                                 .eq("color","white")
                                 .or()
                                 .eq("color","orange");
```

You can also adjust the number of results returned:

```java
UsergridQuery query = new UsergridQuery("cats").eq("color","black").limit(100);
                                 
// returns a maximum of 100 entities
```

And sort the results:

```java
UsergridQuery query = new UsergridQuery("cats").eq("color","black").limit(100).asc("name")
                                 
// sorts by 'name', ascending
```

And you can do geo-location queries:

```java
UsergridQuery query = new UsergridQuery("devices").locationWithin(<distance>, <lat>, <long>);
```

### Using a query in a request

Queries can be passed as parameters to GET, PUT, and DELETE requests:

```java
// Gets entities matching the query.
Usergrid.GET(query);

// Updates the entities matching the query with the new property.
Usergrid.PUT(query, Collections.singletonMap("aNewProperty","A new value"));

// Deletes entities of a given type matching the query.
Usergrid.DELETE(query);
```
### List of query builder objects

`type("string")`

> The collection name to query

`collection("string")`

> An alias for `type`

`eq("key","value")` or 
`equals("key","value")` or 
`filter("key","value")` 

> Equal to (e.g. `where color = 'black'`)

`contains("key","value")` or
`containsString("key","value")` or
`containsWord("key","value")`

> Contains a string (e.g.` where color contains 'bl*'`)

`gt("key","value")` or
`greaterThan("key","value")`

> Greater than (e.g. `where weight > 2.4`)

`gte("key","value")` or 
`greaterThanOrEqual("key","value")`

> Greater than or equal to (e.g. `where weight >= 2.4`)

`lt("key","value")` or `lessThan("key","value")`

> Less than (e.g. `where weight < 2.4`)

`lte("key","value")` or `lessThanOrEqual("key","value")`

> Less than or equal to (e.g. `where weight <= 2.4`)

`not()`

> Negates the next block in the builder pattern, e.g.:

```java
UsergridQuery query = new UsergridQuery("cats").not().eq("color","black");
// select * from cats where not color = 'black'
```

`and()`

> Joins two queries by requiring both of them. `and` is also implied when joining two queries _without_ an operator. E.g.:

```java
UsergridQuery query = new UsergridQuery("cats").eq("color","black").eq("fur","longHair");
// is identical to:
UsergridQuery query = new UsergridQuery("cats").eq("color","black").and().eq("fur","longHair");
```

`or()`

> Joins two queries by requiring only one of them. `or` is never implied. e.g.:

```java
UsergridQuery query = new UsergridQuery("cats").eq("color","black").or().eq("color", "white");
```
    
> When using `or()` and `and()` operators, `and()` joins will take precedence over `or()` joins. You can read more about query operators and precedence [here](http://docs.apigee.com/api-baas/content/supported-query-operators-data-types).

`locationWithin(distanceInMeters, latitude, longitude)`

> Returns entities which have a location within the specified radius. Arguments can be `float` or `int`.

`asc("key")` or `ascending("key")`

> Sorts the results by the specified property, ascending

`desc("key")` or `descending("key")`

> Sorts the results by the specified property, descending

`sort("key",UsergridQuerySortOrder.ASC)`

> Sorts the results by the specified property, in the specified `UsergridQuerySortOrder` (`.ASC` or `.DESC`).
 
`limit(int)`

> The maximum number of entities to return

`cursor("string")`

> A pagination cursor string

`fromString("query string")`

> A special builder property that allows you to input a pre-defined query string. All builder properties will be ignored when this property is defined. For example:
    
```java
UsergridQuery query = new UsergridQuery().fromString("select * where color = 'black' order by name asc");
```

## UsergridResponse object

`UsergridResponse` is the core class that handles both successful and unsuccessful HTTP responses from Usergrid. 

If a request is successful, any entities returned in the response will be automatically parsed into `UsergridEntity` objects and pushed to the `entities` property.

If a request fails, the `error` property will contain information about the problem encountered.

### ok

You can check `UsergridResponse.ok`, a `Bool` value, to see if the response was successful. Any status code `< 400` returns true.

```java
UsergridResponse response = Usergrid.GET("collection");
if( response.ok() ) {
    // woo!
}
```
    
### entity, entities, user, users, first, last

Depending on the call you make, any entities returned in the response will be automatically parsed into `UsergridEntity` objects and pushed to the `entities` property. If you're querying the `users` collection, these will also be `UsergridUser` objects, a subclass of `UsergridEntity`.

- `.first()` returns the first entity in an array of entities; `.entity()` is an alias to `.first()`. If there are no entities, both of these will be undefined.

- `.last()` returns the last entity in an array of entities; if there is only one entity in the array, this will be the same as `.first()` _and_ `.entity()`, and will be undefined if there are no entities in the response.

- `.getEntities()` will either be an array of entities in the response, or an empty array.

- `.user()` is a special alias for `.entity()` for when querying the `users()` collection. Instead of being a `UsergridEntity`, it will be its subclass, `UsergridUser`.

- `.users()` is the same as `.user()`, though behaves as `.getEntities()` does by returning either an array of UsergridUser objects or an empty array.

Examples:

```java
UsergridResponse response = Usergrid.GET("collection");
    // you can access:
    //     response.getEntities() (the returned entities)
    //     response.first() (the first entity)
    //     response.entity() (same as response.first)
    //     response.last() (the last entity returned)

UsergridResponse response = Usergrid.GET("collection","<uuid-or-name>");
    // you can access:
    //     response.entity() (the returned entity) 
    //     response.getEntities() (containing only the returned entity)
    //     response.first() (same as response.entity)
    //     response.last() (same as response.entity)

UsergridResponse response = Usergrid.GET("users");
    // you can access:
    //     response.users() (the returned users)
    //     response.getEntities() (same as response.users)
    //     response.user() (the first user)    
    //     response.entity() (same as response.user)   
    //     response.first() (same as response.user)  
    //     response.last() (the last user)

UsergridResponse response = Usergrid.GET("users","<uuid-or-name>");
    // you can access;
    //     response.users() (containing only the one user)
    //     response.getEntities() (same as response.users)
    //     response.user() (the returned user)    
    //     response.entity() (same as response.user)   
    //     response.first() (same as response.user)  
    //     response.last() (same as response.user)  
```

## Connections

Connections can be managed using `Usergrid.connect()`, `Usergrid.disconnect()`, and `Usergrid.getConnections()`, or entity convenience methods of the same name. 

When retrieving connections via `Usergrid.getConnections()`, you can pass in a optional `UsergridQuery` object in order to filter the connectioned entities returned.

### Connect

Create a connection between two entities:

```java
Usergrid.connect(entity1, "relationship", entity2); // entity1 now has an outbound connection to entity2
```

### Retrieve Connections

Retrieve outbound connections:

```java
Usergrid.getConnections(UsergridDirection.OUT, entity1, "relationship");
    // entities is an array of entities that entity1 is connected to via 'relationship'
    // in this case, we'll see entity2 in the array
```

Retrieve inbound connections:

```java
Usergrid.getConnections(UsergridDirection.IN, entity2, "relationship");
    // entities is an array of entities that connect to entity2 via 'relationship'
    // in this case, we'll see entity1 in the array
```

### Disconnect

Delete a connection between two entities:

```java
Usergrid.disconnect(entity1, "relationship", entity2);
    // entity1's outbound connection to entity2 has been destroyed
```

## Custom UsergridEntity Subclasses

Creating custom subclasses of the base `UsergridEntity` class (just like `UsergridUser` and `UsergridDevice`) is possible.

- To do so, subclass `UsergridEntity` and implement the required methods:

```java
public class ActivityEntity extends UsergridEntity {
	public static final String ACTIVITY_ENTITY_TYPE = "activity";
	
   public ActivityEntity(){
       super(ACTIVITY_ENTITY_TYPE);
   }
}
```
- You will also need to register the custom subclass:

```java
Usergrid.initSharedInstance("orgId","appId");
UsergridEntity.mapCustomSubclassToType("activity", ActivityEntity.class);
```

By registering your custom subclass, the `UsergridEntity` and `UsergridResponse` classes are able to generate instances of these classes based on the an entities `type`.

In the above example, entities which have a `type` value of `activity` can now be cast as `ActivityEntity` objects. e.g.:

```java
UsergridResponse response = Usergrid.GET("activity");
ActivityEntity activityEntity = (ActivityEntity)response.entity();
```
