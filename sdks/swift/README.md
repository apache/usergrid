# UsergridSDK

[![Platform](https://img.shields.io/cocoapods/p/UsergridSDK.svg?style=flat)](http://cocoadocs.org/docsets/UsergridSDK)
[![CocoaPods Compatible](https://img.shields.io/cocoapods/v/UsergridSDK.svg)](https://cocoapods.org/pods/UsergridSDK)

Usergrid SDK written in Swift 

## Requirements

> **While the Usergrid SDK is written in Swift, the functionality remains compatible with Objective-C.**
    
> **Use `#import <UsergridSDK/UsergridSDK-Swift.h>` in your Objective-C files to enable the use of the SDK.**

- iOS 8.0+ / Mac OS X 10.11+ / tvOS 9.1+ / watchOS 2.1+
- Xcode 7.1+

## Installation

> **Embedded frameworks require a minimum deployment target of iOS 8 or OS X Mavericks (10.9).**

### CocoaPods

> **CocoaPods 0.39.0+ is required to build the UsergridSDK library.**

To integrate the UsergridSDK into your Xcode project using CocoaPods, specify it in your `Podfile`:

```ruby
platform :ios, '8.0'
use_frameworks!

pod 'UsergridSDK'
```

Then, run the following command:

```bash
$ pod install
```

### Embedded Framework

- Open up Terminal, `cd` into your top-level project directory, and run the following command "if" your project is not initialized as a git repository:

```bash
$ git init
```

- Add UsergridSDK as a git submodule by running the following command:

```bash
$ git submodule add https://github.com/apache/usergrid
```

- Open the `sdks/swift` folder, and drag the `UsergridSDK.xcodeproj` into the Project Navigator of your application's Xcode project.

> It should appear nested underneath your application's blue project icon.

- Select the `UsergridSDK.xcodeproj` in the Project Navigator and verify the deployment target matches that of your application target.
- Next, select your application project in the Project Navigator (blue project icon) to navigate to the target configuration window and select the application target under the "Targets" heading in the sidebar.
- In the tab bar at the top of that window, open the "General" panel.
- Click on the `+` button under the "Embedded Binaries" section.
- Select the `UsergridSDK.framework`.

> The `UsergridSDK.framework` is automatically added as a target dependency, linked framework and embedded framework in a copy files build phase which is all you need to build on the simulator and a device.

## Documentation

The documentation for this library is available [here](http://cocoadocs.org/docsets/UsergridSDK).

## Initialization

There are two different ways of initializing the Usergrid Swift SDK: 

1. The singleton pattern is both convenient and enables the developer to use a globally available and always-initialized instance of Usergrid. 

```swift
Usergrid.initSharedInstance(orgId: "orgId", appId: "appId")
```

2. The Instance pattern enables the developer to manage instances of the Usergrid client independently and in an isolated fashion. The primary use-case for this is when an application connects to multiple Usergrid targets.

```swift
let client = UsergridClient(orgId: "orgId", appId: "appId")
```

_Note: Examples in this readme assume you are using the `Usergrid` shared instance. If you've implemented the instance pattern instead, simply replace `Usergrid` with your client instance variable._

## Push Notifications

_Note: You must have an Apple Developer account along with valid provisioning profiles set in order to receive push notifications._

In order to utilize Usergrid push notifications, you must register the device with an Usergrid push notifier identifier.

> For a more thorough example of recieving push notifications and sending push notifications (from the device) refer to the Push sample app located in the `/Samples` folder.

The following code snippet shows how you would register for push notifications and apply the push token within the application delegate.

```swift
import UsergridSDK

@UIApplicationMain class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {

        // Initialize the shared instance of Usergrid.
        Usergrid.initSharedInstance(orgId:"orgId", appId: "appId")

        // Register for APN
        application.registerUserNotificationSettings(UIUserNotificationSettings( forTypes: [.Alert, .Badge, .Sound], categories: nil))
        application.registerForRemoteNotifications()

        return true
    }

    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
        Usergrid.applyPushToken(deviceToken, notifierID: "notifierId") { response in
            // The push notification is now added to Usergrid for this device and this device will now be able to recieve notifications.
        }
    }
}
```

## RESTful operations

When making any RESTful call, a `type` parameter (or `path`) is always required. Whether you specify this as an argument or in an object as a parameter is up to you.

### GET

- To get entities in a collection:

```swift
Usergrid.GET("collection") { response in
    var entities: [UsergridEntity]? = response.entities
}
```

- To get a specific entity in a collection by uuid or name:

```swift
Usergrid.GET("collection", uuidOrName:"<uuid-or-name>") { response in
    var entity: UsergridEntity? = response.entity?
}
```

- To get specific entities in a collection by passing a `UsergridQuery` object:

```swift
var query = UsergridQuery("cats").gt("weight", value: 2.4)
                                 .contains("color", value:"bl*")
                                 .not()
                                 .eq("color", value:"blue")
                                 .or()
                                 .eq("color", value:"orange")
	
// this will build out the following query:
// select * where weight > 2.4 and color contains 'bl*' and not color = 'blue' or color = 'orange'
	
Usergrid.GET(query) { response in
    var entities: [UsergridEntity]? = response.entities
}
```

### POST and PUT

POST and PUT requests both require a JSON body payload. You can pass either a Swift object or a `UsergridEntity` instance. While the former works in principle, best practise is to use a `UsergridEntity` wherever practical. When an entity has a uuid or name property and already exists on the server, use a PUT request to update it. If it does not, use POST to create it.

- To create a new entity in a collection (**POST**):

```swift
var entity = UsergridEntity(type: "restaurant", propertyDict: ["restaurant": "Dino's Deep Dish","cuisine": "pizza"])
	
Usergrid.POST(entity) { response in
    // entity should now have a uuid property and be created
}
	
// you can also POST an array of entities:
	
var entities = [UsergridEntity(type: "restaurant", propertyDict:["restaurant": "Dino's Deep Dish","cuisine": "pizza"]), 
                UsergridEntity(type: "restaurant", propertyDict:["restaurant": "Pizza da Napoli","cuisine": "pizza"])]
	
Usergrid.POST(entities) { response in
    // response.entities should now contain now valid posted entities.
}
```

- To update an entity in a collection (**PUT**):

```swift
var entity = UsergridEntity(type: "restaurant", propertyDict:["restaurant": "Dino's Deep Dish", "cuisine": "pizza"])
	
Usergrid.POST(entity) { response in
    if let responseEntity = response.entity {
        responseEntity["owner"] = "Mia Carrara"
        Usergrid.PUT(responseEntity) { (response) -> Void in
            // entity now has the property 'owner'
        }
    }
}
	
// or update a set of entities by passing a UsergridQuery object
	
var query = UsergridQuery("restaurants").eq("cuisine", value:"italian")
	
Usergrid.PUT(query, jsonBody: ["keywords":["pasta"]]) { response in
	
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
}
```

### DELETE

DELETE requests require either a specific entity or a `UsergridQuery` object to be passed as an argument.

- To delete a specific entity in a collection by uuid or name:

```swift
Usergrid.DELETE("collection", uuidOrName: "<uuid-or-name>") { response in
    // if successful, entity will now be deleted
})
```

- To specific entities in a collection to delete by passing a `UsergridQuery` object:

```swift
let query = UsergridQuery("cats").eq("color", value:"black")
                                 .or()
                                 .eq("color", value:"white")
	
// this will build out the following query:
// select * where color = 'black' or color = 'white'
	
Usergrid.DELETE(query) { response in
    // the first 10 entities matching this query criteria will be deleted
}
```

## Entity operations and convenience methods

`UsergridEntity` has a number of helper/convenience methods to make working with entities more convenient.

### reload()

Reloads the entity from the server:

```swift
entity.reload() { response in
    // entity is now reloaded from the server
}
```

### save()

Saves (or creates) the entity on the server:


```swift
entity["aNewProperty"] = "A new value"
entity.save() { response in
    // entity is now updated on the server
}
```

### remove()

Deletes the entity from the server:

```swift
entity.remove() { response in
    // entity is now deleted on the server and the local instance should be destroyed
}
```

## Authentication, current user, and auth-fallback

### appAuth and authenticateApp()

`Usergrid` can use the app client ID and secret that were passed upon initialization and automatically retrieve an app-level token for these credentials.

```swift
Usergrid.setAppAuth("<client-id>", "<client-secret>")
Usergrid.authenticateApp() { response in
    // Usergrid.appAuth is authenticated automatically when this call is successful
}
```

### currentUser, userAuth,  and authenticateUser()

`Usergrid` has a special `currentUser` property. 

> The current user is stored within the keychain so app relaunches will persist the user's login and token information.  To adjust this feature edit the `Usergrid.persistCurrentUserInKeychain` property upon initialization.

By default, when calling `authenticateUser()`, `.currentUser` will be set to this user if the authentication flow is successful.

```swift
let userAuth = UsergridUserAuth(username: "<username>", password: "<password>")
Usergrid.authenticateUser(userAuth) { auth, user, error in
    // Usergrid.currentUser is set to the authenticated user and the token is stored within that context
}
```
    
If you want to utilize authenticateUser without setting as the current user, simply pass a `false` boolean value as the second parameter:

```swift
let userAuth = UsergridUserAuth(username: "<username>", password: "<password>")
Usergrid.authenticateUser(userAuth,setAsCurrentUser: false) { auth, user, error in
    // user is authenticated but Usergrid.currentUser is not set.
}
```

### authFallback

Auth-fallback defines what the client should do when a user token is not present. 

By default, `Usergrid.authFallback` is set to `.None`, whereby when a token is *not* present, an API call will be performed unauthenticated. 

If instead `Usergrid.authFallback` is set to `.App`, the API call will instead be performed using client credentials, _if_ they're available (i.e. `authenticateApp()` was performed at some point). 

### usingAuth()

At times it is desireable to have complete, granular control over the authentication context of an API call. 

To facilitate this, the passthrough function `.usingAuth()` allows you to pre-define the auth context of the next API call.

```swift
// assume Usergrid.authFallback = .None
    
Usergrid.usingAuth(Usergrid.appAuth!).POST("roles/guest/permissions", jsonBody: ["permission" : "get,post,put,delete:/**"] ) { response in
    // here we've temporarily used the client credentials to modify permissions
    // subsequent calls will not use this auth context
}
```

## User operations and convenience methods

`UsergridUser` has a number of helper/convenience methods to make working with user entities more convenient. If you are _not_ utilizing the `Usergrid` shared instance, you must pass an instance of `UsergridClient` as the first argument to any of these helper methods.
    
### create()

Creating a new user:

```swift
let user = UsergridUser(username: "username", password: "password")
user.create() { response in
    // user has now been created and should have a valid uuid
}
```

### login()

A simpler means of retrieving a user-level token:

```swift
user.login("username", password: "password") { auth, user, error in
	// user is now logged in
}
```

### logout()

Logs out the selected user. You can also use this convenience method on `Usergrid.currentUser`.

```swift
user.logout() { response in
	// user is now logged out
}
```

### resetPassword()

Resets the password for the selected user.

```swift
user.resetPassword("oldPassword", new: "newPassword") { error, didSucceed in
    // if it was done correctly, the new password will be changed
    // 'didSucceed' is a boolean value that indicates whether it was changed successfully
}
```

### UsergridUser.CheckAvailable()

This is a class (static) method that allows you to check whether a username or email address is available or not.

```swift
UsergridUser.checkAvailable("email", username: nil) { error, available in
    // 'available' == whether an email already exists for a user
}

UsergridUser.checkAvailable(nil, username: "username") { error, available in
    // 'available' == whether an username already exists for a user
}

UsergridUser.checkAvailable("email", username: "username") { error, available in
    // 'available' == whether an email or username already exist for a user
}
```

## Querying and filtering data

### UsergridQuery initialization

The `UsergridQuery` class allows you to build out complex query filters using the Usergrid [query syntax](http://docs.apigee.com/app-services/content/querying-your-data).

The first parameter of the `UsergridQuery` builder pattern should be the collection (or type) you intend to query. You can either pass this as an argument, or as the first builder object:

```swift
var query = UsergridQuery("cats")
// or
var query = UsergridQuery().collection("cats")
```

You then can layer on additional queries:

```swift
var query = UsergridQuery("cats").gt("weight", value: 2.4)
                                 .contains("color", value: "bl*")
                                 .not()
                                 .eq("color", value:"white")
                                 .or()
                                 .eq("color", value:"orange")
```

You can also adjust the number of results returned:

```swift
var query = UsergridQuery("cats").eq("color", value: "black")
                                 .limit(100)
                                 
// returns a maximum of 100 entiteis
```

And sort the results:

```swift
var query = UsergridQuery("cats").eq("color", value: "black")
                                 .limit(100)
                                 .asc("name")
                                 
// sorts by 'name', ascending
```

And you can do geo-location queries:

```swift
var query = UsergridQuery("devices").locationWithin(<distance>, latitude: <lat>, longitude: <long>)
```

### Using a query in a request

Queries can be passed as parameters to GET, PUT, and DELETE requests:

```swift
Usergrid.GET("type", query: query) { response in
    // Gets entities of a given type matching the query.
}

Usergrid.PUT(query, jsonBody: ["aNewProperty":"A new value"]) { response in
    // Updates the entities matching the query with the new property.
}

Usergrid.DELETE(query) { response in
    // Deletes entities of a given type matching the query.
}
```
### List of query builder objects

`type("string")`

> The collection name to query

`collection("string")`

> An alias for `type`

`eq("key", value: "value")` or 
`equals("key", value: "value")` or 
`filter("key", value: "value")` 

> Equal to (e.g. `where color = 'black'`)

`contains("key", value: "value")` or
`containsString("key", value: "value")` or
`containsWord("key", value: "value")`

> Contains a string (e.g.` where color contains 'bl*'`)

`gt("key", value: "value")` or
`greaterThan("key", value: "value")`

> Greater than (e.g. `where weight > 2.4`)

`gte("key", value: "value")` or 
`greaterThanOrEqual("key", value: "value")`

> Greater than or equal to (e.g. `where weight >= 2.4`)

`lt("key", value: "value")` or `lessThan("key", value: "value")`

> Less than (e.g. `where weight < 2.4`)

`lte("key", value: "value")` or `lessThanOrEqual("key", value: "value")`

> Less than or equal to (e.g. `where weight <= 2.4`)

`not()`

> Negates the next block in the builder pattern, e.g.:

```swift
var query = UsergridQuery("cats").not().eq("color", value: "black")
// select * from cats where not color = 'black'
```

`and()`

> Joins two queries by requiring both of them. `and` is also implied when joining two queries _without_ an operator. E.g.:

```swift
var query = UsergridQuery("cats").eq("color", value:"black").eq("fur", value:"longHair")
// is identical to:
var query = UsergridQuery("cats").eq("color", value:"black").and().eq("fur", value:"longHair")  
```

`or()`

> Joins two queries by requiring only one of them. `or` is never implied. e.g.:

```swift
var query = UsergridQuery("cats").eq("color",value: "black").or().eq("color", value: "white")
```
    
> When using `or()` and `and()` operators, `and()` joins will take precedence over `or()` joins. You can read more about query operators and precedence [here](http://docs.apigee.com/api-baas/content/supported-query-operators-data-types).

`locationWithin(distanceInMeters, latitude: latitude, longitude: longitude)`

> Returns entities which have a location within the specified radius. Arguments can be `float` or `int`.

`asc("key")` or `ascending("key")`

> Sorts the results by the specified property, ascending

`desc("key")` or `descending("key")`

> Sorts the results by the specified property, descending

`sort("key", value: .Asc)`

> Sorts the results by the specified property, in the specified `UsergridQuerySortOrder` (`.Asc` or `.Desc`).
 
`limit(int)`

> The maximum number of entities to return

`cursor("string")`

> A pagination cursor string

`fromString("query string")`

> A special builder property that allows you to input a pre-defined query string. All builder properties will be ignored when this property is defined. For example:
    
```swift
var query = UsergridQuery().fromString("select * where color = 'black' order by name asc")
```

## UsergridResponse object

`UsergridResponse` is the core class that handles both successful and unsuccessful HTTP responses from Usergrid. 

If a request is successful, any entities returned in the response will be automatically parsed into `UsergridEntity` objects and pushed to the `entities` property.

If a request fails, the `error` property will contain information about the problem encountered.

### ok

You can check `UsergridResponse.ok`, a `Bool` value, to see if the response was successful. Any status code `< 400` returns true.

```swift
Usergrid.GET("collection") { response in
    if response.ok {
        // woo!
    }
}
```
    
### entity, entities, user, users, first, last

Depending on the call you make, any entities returned in the response will be automatically parsed into `UsergridEntity` objects and pushed to the `entities` property. If you're querying the `users` collection, these will also be `UsergridUser` objects, a subclass of `UsergridEntity`.

- `.first` returns the first entity in an array of entities; `.entity` is an alias to `.first`. If there are no entities, both of these will be undefined.

- `.last` returns the last entity in an array of entities; if there is only one entity in the array, this will be the same as `.first` _and_ `.entity`, and will be undefined if there are no entities in the response.

- `.entities` will either be an array of entities in the response, or an empty array.

- `.user` is a special alias for `.entity` for when querying the `users` collection. Instead of being a `UsergridEntity`, it will be its subclass, `UsergridUser`.

- `.users` is the same as `.user`, though behaves as `.entities` does by returning either an array of UsergridUser objects or an empty array.

Examples:

```swift
Usergrid.GET("collection") { response in
    // you can access:
    //     response.entities (the returned entities)
    //     response.first (the first entity)
    //     response.entity (same as response.first)
    //     response.last (the last entity returned)
}

Usergrid.GET("collection", uuidOrName:"<uuid-or-name>") { response in
    // you can access:
    //     response.entity (the returned entity) 
    //     response.entities (containing only the returned entity)
    //     response.first (same as response.entity)
    //     response.last (same as response.entity)
}

Usergrid.GET("users") { response in
    // you can access:
    //     response.users (the returned users)
    //     response.entities (same as response.users)
    //     response.user (the first user)    
    //     response.entity (same as response.user)   
    //     response.first (same as response.user)  
    //     response.last (the last user)
}

Usergrid.GET("users", uuidOrName:"<uuid-or-name>") { response in
    // you can access;
    //     response.users (containing only the one user)
    //     response.entities (same as response.users)
    //     response.user (the returned user)    
    //     response.entity (same as response.user)   
    //     response.first (same as response.user)  
    //     response.last (same as response.user)  
}
```

## Connections

Connections can be managed using `Usergrid.connect()`, `Usergrid.disconnect()`, and `Usergrid.getConnections()`, or entity convenience methods of the same name. 

When retrieving connections via `Usergrid.getConnections()`, you can pass in a optional `UsergridQuery` object in order to filter the connectioned entities returned.

### Connect

Create a connection between two entities:

```swift
Usergrid.connect(entity1, relationship: "relationship", to: entity2) { response in
    // entity1 now has an outbound connection to entity2
}
```

### Retrieve Connections

Retrieve outbound connections:

```swift
Usergrid.getConnections(.Out, entity: entity1, relationship: "relationship", query: nil) { response in
    // entities is an array of entities that entity1 is connected to via 'relationship'
    // in this case, we'll see entity2 in the array
}
```

Retrieve inbound connections:

```swift
Usergrid.getConnections(.In, entity: entity2, relationship: "relationship", query: nil) { response in
    // entities is an array of entities that connect to entity2 via 'relationship'
    // in this case, we'll see entity1 in the array
}
```

### Disconnect

Delete a connection between two entities:

```swift
Usergrid.disconnect(entity1, relationship: "relationship", from: entity2) { response in
    // entity1's outbound connection to entity2 has been destroyed
}
```

## Assets

Assets can be uploaded and downloaded either directly using `Usergrid.uploadAsset()` or `Usergrid.downloadAsset()`, or via `UsergridEntity` convenience methods with the same names. Before uploading an asset, you will need to initialize a `UsergridAsset` instance.

### Initialization

_Note: When initializing a `UsergridAsset` object specifying a file name is optional._

- Using NSData:

```swift
let image = UIImage(contentsOfFile: "path/to/image")
let data = UIImagePNGRepresentation(image)
let asset = UsergridAsset(fileName:"<file-name-or-nil>", data: data!, contentType:"image/png")
```

- Using an UIImage object:

```swift
let image = UIImage(contentsOfFile: "path/to/image")
let asset = UsergridAsset(fileName:"<file-name-or-nil>", image: image!, imageContentType: .Png)
```

- Using a file URL:

```swift
let fileUrl = NSURL(string: "local/path/to/file")
if fileUrl.isFileReferenceURL() {  // This must be a file reference url.
    let asset = UsergridAsset(fileName:"<file-name-or-nil>", fileUrl: fileUrl!, contentType:"<content-type>")
}
```

### Uploading

Upload an image and connect it to an entity:

```swift
let image = UIImage(contentsOfFile: "path/to/image")
let asset = UsergridAsset(fileName:"<file-name-or-nil>", image: image!, imageContentType: .Png)!
Usergrid.uploadAsset(entity,
                     asset: asset,
                     progress: { bytesFinished, bytesExpected in
                        // Monitor the upload progress
                     },
                     completion: { response, asset, error in
                        // The asset is now uploaded to Usergrid and entity.asset == asset
})
```

### Downloading

Download an image which is connected to an entity:

```swift
Usergrid.downloadAsset(entity,
                       contentType: "<expected-content-type>",
                       progress: { bytesFinished, bytesExpected in
                            // Monitor the download progress
                       },
                       completion:{ asset, error in
                            // The asset is now downloaded from Usergrid and entity.asset == asset
})
```

## Custom UsergridEntity Subclasses

Creating custom subclasses of the base `UsergridEntity` class (just like `UsergridUser` and `UsergridDevice`) is possible.

> For a working example of creating a custom subclass refer to the ActivityFeed sample app located in the `/Samples` folder.  The custom subclass there is named `ActivityEntity`.

- To do so, subclass `UsergridEntity` and implement the required methods:

```swift
import UsergridSDK
	
public class ActivityEntity: UsergridEntity {
	
    required public init(type: String, name: String?, propertyDict: [String : AnyObject]?) {
        super.init(type: type, name: name, propertyDict: propertyDict)
    }
	
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
```
- You will also need to register the custom subclass:

```swift
Usergrid.initSharedInstance(orgId: "orgId", appId: "appId")
UsergridEntity.mapCustomType("activity", toSubclass: ActivityEntity.self)
```

By registering your custom subclass, the `UsergridEntity` and `UsergridResponse` classes are able to generate instances of these classes based on the an entities `type`.

In the above example, entities which have a `type` value of `activity` can now be cast as `ActivityEntity` objects. e.g.:

```swift
Usergrid.GET("activity") { response in
    var activityEntities: [ActivityEntity]? = response.entities as? [ActivityEntity]
}
```
