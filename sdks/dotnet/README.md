## Version
Current version is v0.1.0. All assemblies are marked with this version number.

## Assemblies
Solution is composed of following projects:

* Usergrid.Sdk - this is the main .NET SDK project.
* Usergrid.Sdk.Tests - this is the unit test project.
* Usergrid.Sdk.IntegrationTests - this is the integration test project.

## Comments and Questions
Please feel free to send your comments/suggestions/questions one of the
several communication platforms. 
<http://usergrid.apache.org/community/>
    
## Overview
We welcome your contributions and suggestions. The repository is located [here][RepositoryLocation].

You can download this package here:

* Download as a development [zip file][ZipFileLocation] SNAPSHOT

## Installing
Usergrid .NET SDK can be installed via:

* Build source and reference Usergrid.Sdk assembly OR
* Use Nuget

### Building from Source
If you prefer to build the assembly from source, head over to [here][RepositoryLocation], download/clone the source files and build. Source can be compiled via VS Studio 2010 or using Xamarin tools.

### Using Nuget
@TODO

### Getting Started
The main facade to all SDK functionality is provided by the Client class (which can also be injected using IClient):

	var client = new Client("YourOrgName", "YourAppName");

By default any method invoked using this client interacts with Usergrid without any authentication tokens. You can use this to access your "Sandbox" application which comes with all new Usergrid accounts - please see [Creating a sandbox app](http://apigee.com/docs/usergrid/content/creating-sandbox-app) for more information.

If you are ready to authenticate, then you can get an access token via login method:

	client.Login("loginId", "loginSecret", AuthType.ClientId);
	
This method authenticates with Usergrid and attaches the retrieved access token to this client instance.

Above example uses your client credentials to access Usergrid - note that this is not the only credentials/method you can use, please see "Authentication" section below.

You are now ready to use this client instance to make calls to Usergrid.

### Running Integration Tests
In order to run the integration tests, do the following:

* Open Usergrid.Sdk.IntegrationTests.dll.config and configure all configuration items according to your own setup.
* Tests have been implemented using NUnit, so you can use any NUnit runner to run the tests.

## Authentication
Usergrid Authentication is explained in [Usergrid Documentation](http://usergrid.apache.org/docs/).

You can access your Usergrid data using following credentials:

### Organization API Credentials
You can find your organisation's credentials on the "Org Overview" page of the [Admin Portal](https://github.com/usergrid/usergrid/tree/master/portal). Once you located your organisation credentials, you can pass them to the Login method instance:

	client.Login("clientId", "clientSecret", AuthType.Organization);

### Application Credentials
You can find an application's credentials on the "App Settings" page of the [Admin Portal](https://github.com/usergrid/usergrid/tree/master/portal). Once you located your application credentials, you can pass them to the Login method:

	client.Login("applicationId", "applicationSecret", AuthType.Application);
	
### Admin and User Credentials
You can find a list of all admin users on the "Org Overview" page of the [Admin Portal](https://github.com/usergrid/usergrid/tree/master/portal). 

You can pass the credentials of a user (admin or normal user) to the Login method:

	client.Login("userLoginId", "userSecret", AuthType.User);

## Entities and Collections
Usergrid stores its data as "Entities" in "Collections".  Entities are essentially JSON objects and Collections are just like folders for storing these objects. You can learn more about Entities and Collections in the App Services docs.

### Entities
```
T CreateEntity<T>(string collection, T entity);
void DeleteEntity(string collection, string name);
void UpdateEntity<T>(string collection, string identifier, T entity);
T GetEntity<T>(string collectionName, string identifer);
```

All Entity CRUD operations are supported as individual methods in Client class. SDK handles JSON de/serialisation of your entities.

Every entity in Usergrid has common default properties, like name, uuid, type, created, modified, etc. While modelling your entities, you should note the following:

* Each entity has a unique UUID assigned to it. You can also assign your own id property to an entity.
* For entities of type user, the username property value must be unique.
* For all other entity types, the name property value must be unique and is immutable. This means that once you set it, it cannot be changed.

You can model your entities in the following ways:

* Inherit from UsergridEntity - base Entity class in .NET SDK. It defines all common/default properties for entities. If you choose to derive your own entities from UsergridEntity, then you don't need to deal with the definition and de/serialisation of those default properties. UsergridEntity code is under Usergrid.SDK/Model/UsergridEntity.cs. Please see ShouldCrudUsergridEntity method in [EntityCrudTests.cs][] under Usergrid.Sdk.IntegrationTests project for example usage.
* Use your own entity classes - if you choose not to inherit from UsergridEntity, then you will need to include the properties that you care about yourself. SDK will still handle the JSON de/serialisation for your entities. Please see ShouldCrudPocoEntity method in [EntityCrudTests.cs][] under Usergrid.Sdk.IntegrationTests project.  

### Collections
```
UsergridCollection<T> GetEntities<T>(string collection, int limit = 10, string query = null);
UsergridCollection<T> GetNextEntities<T>(string collection, string query = null);
UsergridCollection<T> GetPreviousEntities<T>(string collection, string query = null);
```
The Collection object models Collections in the database.  Once you start programming your app, you will likely find that this is the most useful method of interacting with the database.

SDK defines a class UsergridCollection<T> in order to model collections. Implementation inherits from List<T> and also supports paging within the collection items.

Please see @TODO link to EntityPagingTests.cs for example paging implementation.

## Users and Groups
```
T GetUser<T>(string identifer /*username or uuid or email*/) where T : UsergridUser;
void CreateUser<T>(T user) where T : UsergridUser;
void UpdateUser<T>(T user) where T : UsergridUser;
void DeleteUser(string identifer /*username or uuid or email*/);
void ChangePassword(string identifer /*username or uuid or email*/, string oldPassword, string newPassword);
void CreateGroup<T>(T group) where T : UsergridGroup;
void DeleteGroup(string path);
T GetGroup<T>(string identifer /*uuid or path*/) where T : UsergridGroup;
void UpdateGroup<T>(T group) where T : UsergridGroup;
void AddUserToGroup(string groupIdentifier, string userName);
void DeleteUserFromGroup(string groupIdentifier, string userIdentifier);
```
A user entity represents an application user. Using App services APIs you can create, retrieve, update, delete, and query user entities.

A group entity organizes users into a group. Using App Services APIs you can create, retrieve, update, or delete a group. You can also add or delete a user to or from a group.

SDK contains special classes - UsergridUser and UsergridGroup - which models a Usergrid User and Group. You can derive from these classes and add your own properties.
There are a couple of things you need to be aware of when working with users and groups.

For users:
* UserName property must be unique and it is mandatory.
* Email property must be unique
* For more information check the [documentation](http://apigee.com/docs/usergrid/content/user) on users.

For groups:
* Path property must be unique and it is mandatory.
* For more information check the [documentation](http://apigee.com/docs/usergrid/content/user) on groups.
 
Please see [GroupTests.cs][] and [UserManagementTests.cs][] for integration tests.

## Activities and Feeds
```
void PostActivity<T>(string userIdentifier, T activity) where T:UsergridActivity;
void PostActivityToGroup<T>(string groupIdentifier, T activity) where T:UsergridActivity;
void PostActivityToUsersFollowersInGroup<T>(string userIdentifier, string groupIdentifier, T activity) where T:UsergridActivity;
UsergridCollection<T> GetUserActivities<T>(string userIdentifier) where T:UsergridActivity;
UsergridCollection<T> GetGroupActivities<T>(string groupIdentifier) where T:UsergridActivity;
UsergridCollection<T> GetUserFeed<T>(string userIdentifier) where T : UsergridActivity;
UsergridCollection<T> GetGroupFeed<T>(string groupIdentifier) where T : UsergridActivity;
```
An activity is an entity type that represents activity stream (feed) actions.

#### Creating an activity

The SDK provides UsergridActivity and UsergridActor classes to create an activity for a user. Here is some sample code from [ActivitiesTests.cs][] which creates an activity for a user.
```
userFromUsergrid = client.GetUser<UsergridUser> (username);

// Create an activity for this user
var activityEntity = new UsergridActivity {
Actor = new UsergridActor
{
	DisplayName = "Joe Doe",
	Email = userFromUsergrid.Email,
	UserName = userFromUsergrid.UserName,
	Uuid = userFromUsergrid.Uuid,
	Image = new UsergridImage
	{
		Height = 10,
		Width = 20,
		Duration = 0,
		Url = "apigee.com"
	}
},
Content = "Hello Usergrid",
Verb = "post"
};

client.PostActivity (userFromUsergrid.UserName, activityEntity);
```
You can also post an activity to a group with the PostActivityToGroup method `client.PostActivityToGroup (groupName, activityEntity);`


## Devices
```
T GetDevice<T>(string identifer) where T : UsergridDevice;
void UpdateDevice<T>(T device) where T : UsergridDevice;
void CreateDevice<T>(T device) where T : UsergridDevice;
void DeleteDevice(string identifer);
```
You can derive your own class from UserdridDevice to add your own properties and perform CRUD operations on devices.
  You can find the integration tests for devices in [DeviceTests.cs][]

## Connections
```
void CreateConnection(Connection connection);
IList<UsergridEntity> GetConnections(Connection connection);
IList<TConnectee> GetConnections<TConnectee>(Connection connection);
void DeleteConnection(Connection connection);
```

In the SDK, connections are configured with the [Connection][] class, which holds the connector/connectee details and the connection name.ConnectionName property is a mandatory field when managing the connections.  
To create and delete a connection, you'll need to populate all the fields in the connection object.  
Connections can reference any type of entity, if you want to get all entities in a connection, you only need to populate connector details plus the connection name and use the non generic version of the GetConnections method. This will give you a list of UsergridEntity.
```
//get the connections, supply only the connector details
//we get a list of Usergrid entites
IList<UsergridEntity> allConnections = client.GetConnections(new Connection()
    {
        ConnectorCollectionName = "customers", 
        ConnectorIdentifier = "customer1", 
        ConnectionName = "has"
    });
```
To get a list of a specific type of an entity, you also need to populate the connectee connection name and use the generic version of the GetConnections method.
```
//now, just get the orders for customer from the connection
//we need to supply the connectee collection name
IList<Order> orderConnections = client.GetConnections<Order>(new Connection()
    {
        ConnectorCollectionName = "customers", 
        ConnectorIdentifier = "customer1", 
        ConnecteeCollectionName = "orders", 
        ConnectionName = "has"
    });
```
Check [ConnectionTests.cs][] for the integration tests, and documentation on [Entity relationships][UsergridEntityRrelationshipsDoc].

## Push Notifications

```
void CreateNotifierForApple(string notifierName, string environment, string p12CertificatePath);
void CreateNotifierForAndroid(string notifierName, string apiKey);
T GetNotifier<T>(string identifer/*uuid or notifier name*/) where T : UsergridNotifier;
void DeleteNotifier(string notifierName);
```

You can send notifications to devices, users, or groups. But first, you need to register your app with push notification providers. This is explained in detail [here][RegisterYourAppDoc].  
Once you've rgistered your app you need to create a notifier for it. Notifiers, which you explicitly create and add to notifications, contain the credentials necessary to securely access push notification providers--which in turn send your notifications to targeted devices. 
#### Creating notifiers
You need to give your notifier a name and then call the relevant method for your provider to create it. For example, to create a notifier for android :
```
const string notifierName = "test_notifier";
...
client.CreateNotifierForAndroid(notifierName, GoogleApiKey /*e.g. AIzaSyCkXOtBQ7A9GoJsSLqZlod_YjEfxxxxxxx*/);
```
You can check for existance of a notifier with the `GetNotifier` method which expects a notifier name, and returns a [UsergridNotifier][].
####Publishing notifications
To publish a notification you need to  

* Create notification objects with your message

Assuming you have created two notifiers (one for apple one for android)
```
const string appleTestMessge = "test message for Apple";
const string androidTestMessage = "test message for Android";
...
var appleNotification = new AppleNotification(appleNotifierName, appleTestMessge, "chime");
var googleNotification = new AndroidNotification(googleNotifierName, androidTestMessage);
```
*  Setup your recipients

Use NotificationRecipients class, which provides builder methods for setting up recipients. For example if you want to send your message to a user:
```
INotificationRecipients recipients = new NotificationRecipients().AddUserWithName(username);
```
You can check the behaviour of the NotificationRecipients class in [NotificationRecipientsTests][].

*  Setup scheduling

You can schedule a notification to be delivered or to be exipred at a certain date time. This is done using NotificationSchedulerSettings. For example, if you want your message to be delivered tomorrow:
```
var schedulerSettings = new NotificationSchedulerSettings {DeliverAt = DateTime.Now.AddDays(1)};
```

*  and publish

You can publish more than one notification, PublishNotification method accepts an array of notifications, recipients and the scheduler settings:

```
client.PublishNotification(new Notification[] {appleNotification, googleNotification}, recipients, schedulerSettings);
```

Integration tests are in [NotificationTests.cs][], and you can read about the push notifications in Usergrid documentation [here][PushNotificationsDoc].



<!---Code reference-->
[GroupTests.cs]: @TODO
[UserManagementTests.cs]: @TODO
[ActivitiesTests.cs]: @TODO
[DeviceTests.cs]: @TODO
[Connection]: @TODO
[ConnectionTests.cs]: @TODO
[RepositoryLocation]: https://github.com/usergrid/usergrid
[ZipFileLocation]: https://github.com/usergrid/usergrid/archive/master.zip
[TarGzFileLocation]: @TODO
[EntityCrudTests.cs]: @TODO


[UsergridNotifier]: @TODO
[NotificationRecipientsTests]: @TODO
[NotificationTests.cs]: @TODO
<!---Docs-->
[PushNotificationsDoc]: http://apigee.com/docs/usergrid/content/push-notifications
[RegisterYourAppDoc]: @TODO
[UsergridEntityRrelationshipsDoc]:@TODO
