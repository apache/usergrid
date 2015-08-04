## Methods
## Action Methods>

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/activate</h2>

Activate an organization from a link provIded in an email notification.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __token__ ()
Activation token. (Specified in query).
* __confirm_email__ ()
Send confirmation email or not. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/reactivate</h2>

Reactivate an expired organization.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/users/resetpw</h2>

Initiate the reset of an admin user&#39;s password.

<h3>Parameters</h3>


<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /management/users/resetpw</h2>

Complete the password reset through getting the newpassword and the old one for identification.

<h3>Parameters</h3>

* __ResetPWMsg__ ([ResetPWMsg](#resetpwmsg))
Parameters and value for the Captcha challenge, the admin user&#39;s response to the Captcha challenge, and the admin user&#39;s email address. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/users/{userId}/activate</h2>

Activate an admin user from a link provIded in an email notification.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).
* __token__ ()
Activation token&#39;s query statement. (Specified in query).
* __confirm_email__ ()
Query statement of whether send confimation email or not. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /management/users/{userId}/password</h2>

Update an admin user&#39;s password through getting the newpassword and the old one for identification.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).
* __ResetPW__ ([ResetPW](#resetpw))
The user&#39;s old and new password. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/users/{userId}/reactivate</h2>

Reactivate an expired admin user.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/users/{user}/password</h2>

Set a user&#39;s password or reset the user&#39;s existing password.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __username__ (string)
The username of the user. (Specified in path).
* __ResetPW__ ([ResetPW](#resetpw))
The user&#39;s old and new password. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## ActivityFeed Methods>

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/feed</h2>

Retrieve an organization&#39;s activity feed.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of the organization&#39;s ActivityFeed.
* Schema: [ActivityFeed](#activityfeed)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/users/{userId}/feed</h2>

Retrieve an admin user&#39;s activity feed.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of user&#39;s activity
* Schema: [ActivityFeed](#activityfeed)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/groups/{groupId}/activities</h2>

Create an activity to a specific group. In this case the activity is created in the activities collection and is accessible at the /activities endpoint to users who have the permission to read that endpoint. In addition, a relationship is established between the activity and the group, and because of that, the activity will appear in the group’s feed. The group &#39;owns&#39; the activity. Also, the activity will be published in the feed of all users that are members of the group.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __groupId__ (string)
One of the group&#39;s identification which includes name or uuid. (Specified in path).
* __CreateActivity__ ([CreateActivity](#createactivity))
One or more sets of activity properties. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of user&#39;s activity.
* Schema: [ActivityFeed](#activityfeed)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/groups/{groupId}/feed</h2>

Get a group&#39;s feed through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __groupId__ (string)
One of the group&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of group&#39;s activity.
* Schema: [ActivityFeed](#activityfeed)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/users/{userId}/activities</h2>

Create an activity in the activities collection.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).
* __CreateActivity__ ([CreateActivity](#createactivity))
One or more sets of activity properties. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of user&#39;s activity.
* Schema: [ActivityFeed](#activityfeed)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/users/{userId}/feed</h2>

Retrieve a user&#39;s feed through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of user&#39;s activity feed.
* Schema: [ActivityFeed](#activityfeed)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## AppData Methods>

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/apps</h2>

Retrieve the applications in an organization through providing both Id of application and organization.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved application data.
* Schema: [AppData](#appdata)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /management/orgs/{orgId}/apps/{appId}</h2>

Remove an application from an organization through providing both Id of application and organization.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted application info.
* Schema: [AppData](#appdata)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Credential Methods>

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/apps/{appId}/credentials</h2>

Retrieve the client Id and client secret credentials for an application in an organization.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved credentials info.
* Schema: [Credential](#credential)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /management/orgs/{orgId}/apps/{appId}/credentials</h2>

Generate the client Id and client secret credentials for an application in an organization.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of generated credentials info.
* Schema: [Credential](#credential)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/credentials</h2>

Retrieve the credentials for an organization client.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of Credential
* Schema: [Credential](#credential)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /management/orgs/{orgId}/credentials</h2>

Generate whole new credentials for an organization client.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of Credential
* Schema: [Credential](#credential)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Devices Methods>

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/{notificationId}/*/queue</h2>

Retrieve the list of devices associated with one or more notifications before the notifications are sent through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __notificationId__ (string)
One of the notification&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved device&#39;s info.
* Schema: [Device](#device)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Entity Methods>

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/users/{userId}/{relation}</h2>

Retrieve a user&#39;s collections or connections through query statement.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).
* __relation__ (string)
The relation between user and collections. (Specified in path).
* __queryStatement__ ()
The query statement of the user. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of user&#39;s collections info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/{collectionId}</h2>

Retrieve collection through query statement.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __queryStatement__ ()
Any values specified in the query statement should be enclosed in single-quotes. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of retrieved collection&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /{orgId}/{appId}/{collectionId}</h2>

Update collection through query statement.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __queryStatement__ ()
Any values specified in the query statement should be enclosed in single-quotes. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of updated collection&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/{collectionId}/{entityId1}/{relation}/{entityId2}</h2>

Add an entity to a collection through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __entityId1__ (string)
The Id of the 1st entity. (Specified in path).
* __relation__ (string)
The relation between 1st entity and 2nd entity. (Specified in path).
* __entityId2__ (string)
The Id of the 2nd entity. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of added entity&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/{collectionId}/{entityId1}/{relation}/{entityId2}</h2>

Remove an entity from a collection through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __entityId1__ (string)
The Id of the 1st entity. (Specified in path).
* __relation__ (string)
The relation between 1st entity and 2nd entity. (Specified in path).
* __entityId2__ (string)
The Id of the 2nd entity. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted entity&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/{collectionId}/{entityId}</h2>

Retrieve an entity through providing Id of application, organization, collection and entity.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __entityId__ (string)
One of the entity&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved entity&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /{orgId}/{appId}/{collectionId}/{entityId}</h2>

One or more properties can be updated with a single request.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __entityId__ (string)
One of the entity&#39;s identification which includes name or uuid. (Specified in path).
* __entityproperty__ ([CreateEntities](#createentities))
The properties of the entity. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of updated entity&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/{collectionId}/{entityId}</h2>

Delete an entity from the collection.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __entityId__ (string)
One of the entity&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted entity&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/{entitytype}</h2>

When a new entity is created, Usergrid will automatically create a corresponding collection if one does not already exist. The collection will automatically be named with the plural form of the entity type. 

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __entitytype__ (string)
The entity type to create. (Specified in path).
* __entityproperty__ ([CreateEntities](#createentities))
The properties of the entity. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created custom entity&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Event Methods>

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/events</h2>

Create an event through providing both Id of organization and application.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __CreateEvent__ ([CreateEvent](#createevent))
The required property of the event. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created event&#39;s info.
* Schema: [Event](#event)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Group Methods>

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/groups</h2>

Create a new group through providing both Id of organization and application.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __groupproperty__ ([CreateGroup](#creategroup))
The property of the created group. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created group&#39;s info.
* Schema: [Group](#group)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{org_Id}/{app_Id}/groups/{groupId}</h2>

Get a group through through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __groupId__ (string)
One of the group&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved group&#39;s info.
* Schema: [Group](#group)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /{org_Id}/{app_Id}/groups/{groupId}</h2>

Update a group through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __groupId__ (string)
One of the group&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of updated group&#39;s info.
* Schema: [Group](#group)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Notification Methods>

<h2 class="usergrid-PUT-heading">PUT /my-org/sandbox/notifications/{notificationId}</h2>

Cancel sent notifications through providing the Id of the notification.

<h3>Parameters</h3>

* __notificationId__ (string)
One of the notification&#39;s identification which includes name or uuid. (Specified in path).
* __cancelMsg__ ([CancelMSG](#cancelmsg))
Indicete whether is canceled or not. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of canceled notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/devices</h2>

Create notifications for user through targeting by location and providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).
* __scheduleNotification__ ([ScheduleNotification](#schedulenotification))
To schedule a notification for a later time add the deliver parameter with a UNIX timestamp to the body of your request. (Specified in body).
* __setExpiration__ ([SetExpiration](#setexpiration))
If a push service can&#39;t deliver a message to a device and needs to resend it you can set a notification to expire after a certain date/time. (Specified in body).
* __queryStatement__ ()
The query statement of the location of the user. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/devices/*/notifications</h2>

Create notifications for all devices. This request will target all device entities.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).
* __scheduleNotification__ ([ScheduleNotification](#schedulenotification))
To schedule a notification for a later time add the deliver parameter with a UNIX timestamp to the body of your request. (Specified in body).
* __setExpiration__ ([SetExpiration](#setexpiration))
If a push service can&#39;t deliver a message to a device and needs to resend it you can set a notification to expire after a certain date/time. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/devices/{deviceId}/notifications</h2>

Create notifications for a single device. This request will target a specific device entity.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __deviceId__ (string)
One of the device&#39;s identification which includes name or uuid. (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).
* __scheduleNotification__ ([ScheduleNotification](#schedulenotification))
To schedule a notification for a later time add the deliver parameter with a UNIX timestamp to the body of your request. (Specified in body).
* __setExpiration__ ([SetExpiration](#setexpiration))
If a push service can&#39;t deliver a message to a device and needs to resend it you can set a notification to expire after a certain date/time. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/groups/{path}/notifications</h2>

Create notifications for a group. This request will target all users associated with a specific group entity.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __path__ (string)
The path of the group. (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).
* __scheduleNotification__ ([ScheduleNotification](#schedulenotification))
To schedule a notification for a later time add the deliver parameter with a UNIX timestamp to the body of your request. (Specified in body).
* __setExpiration__ ([SetExpiration](#setexpiration))
If a push service can&#39;t deliver a message to a device and needs to resend it you can set a notification to expire after a certain date/time. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/notifications</h2>

Retrieve one or more notifications through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/notifications/{notificationId}</h2>

Remove unsent notifications through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __notificationId__ (string)
One of the notification&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/users/{userId}/notifications</h2>

Create notifications for a user. This request will target a specific user entity.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).
* __scheduleNotification__ ([ScheduleNotification](#schedulenotification))
To schedule a notification for a later time add the deliver parameter with a UNIX timestamp to the body of your request. (Specified in body).
* __setExpiration__ ([SetExpiration](#setexpiration))
If a push service can&#39;t deliver a message to a device and needs to resend it you can set a notification to expire after a certain date/time. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/{receiptId}/*/notifications</h2>

Retrieve notifications associated with one or more receipts through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __receiptId__ (string)
One of the receipt&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Organization Methods>

<h2 class="usergrid-POST-heading">POST /management/orgs</h2>

Create an organization through a form post.

<h3>Parameters</h3>

* __CreateOrg__ ([CreateOrg](#createorg))
A set of organization properties supplied through a form. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created Organization.
* Schema: [Organization](#organization)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}</h2>

Retrieve an organization given a specified UUID or username.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of created Organization.
* Schema: [Organization](#organization)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Permission Methods>

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/roles/{roleId}/permissions</h2>

Remove permissions from a role. 

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __roleId__ (string)
One of the role&#39;s identification which includes name or uuid. (Specified in path).
* __grant_url_pattern__ ()
The query statement of the url pattern. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of deleted permission&#39;s info.
* Schema: [Permission](#permission)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Receipt Methods>

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/receipts</h2>

Retrieve one or more receipts through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved receipt&#39;s info.
* Schema: [Receipt](#receipt)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/{deviceId}/*/receipts</h2>

Retrieve receipts associated with one or more devices through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __deviceId__ (string)
One of the device&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved receipt&#39;s info.
* Schema: [Receipt](#receipt)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/{notificationId}/*/receipts</h2>

Retrieve receipts for one or more notifications through providing all the identifications.&quot;

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __notificationId__ (string)
One of the notification&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved receipt&#39;s info.
* Schema: [Receipt](#receipt)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## Role Methods>

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/roles</h2>

Retrieve the roles in an application through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved role&#39;s info.
* Schema: [Role](#role)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/roles</h2>

Create a new role through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __roleproperty__ ([AddRole](#addrole))
The required properties of the role. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created role&#39;s info.
* Schema: [Role](#role)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/roles/{rolename}</h2>

Remove a role through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __rolename__ (string)
The name of the role. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted role&#39;s info.
* Schema: [Role](#role)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    
## User Methods>

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/users</h2>

Retrieve details about the admin users in an organization.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved Admin user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /management/orgs/{orgId}/users/{userId}</h2>

Remove an admin user from an organization through providing both Id of application and organization.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __userId-2__ (string)
One of the user&#39;s identification which includes username, email address or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted Admin user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /management/users</h2>

Create a whole new admin user.

<h3>Parameters</h3>

* __adminuserproperty__ ([CreateAdminUser](#createadminuser))
One or more sets of user properties of which username is mandatory and must be unique. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of new created Admin user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/users/{userId}</h2>

Retrieve details about an admin user.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of user&#39;s details
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /management/users/{userId}</h2>

Update the info of an admin user.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of user&#39;s details.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/groups/{groupId}/users/{userId}</h2>

Add a user to a group through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __groupId__ (string)
One of the group&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of added user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/groups/{groupId}/users/{userId}</h2>

Delete user from a group through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __groupId__ (string)
One of the group&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/roles/{roleId}/users</h2>

Retrieve the users in a role through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __roleId__ (string)
One of the role&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/roles/{roleId}/users/{userId}</h2>

Add a user to a role through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __roleId__ (string)
One of the role&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of added user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/roles/{roleId}/users/{userId}</h2>

Remove a user from a role through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __roleId__ (string)
One of the role&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/users</h2>

Retrieve users though query statement.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __queryStatement__ ()
The query statement of the User. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of retrieved user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/users</h2>

Create a user in the users collection through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __CreateUser__ ([CreateUser](#createuser))
The properties of the user. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/users/{userId}</h2>

Retrieve a user through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __userId-2__ (string)
One of the user&#39;s identification which includes username, email address or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /{orgId}/{appId}/users/{userId}</h2>

Update a user through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of updated user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/users/{userId}</h2>

Remove a user through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of deleted user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

## Models
Properties for Usergrid default entities.

## Action

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>action</td>
        <td>string</td>
        <td>The requested action.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>status</td>
        <td>string</td>
        <td>The status of the requested action.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>token</td>
        <td>string</td>
        <td>The token required for getting an AdminUser.</td>
        <td>false</td>
    </tr>
</table>

## ActivityFeed

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>entityproperty</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>category</td>
        <td>string</td>
        <td>The category of the activity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>metadataproperty</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>objectproperty</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>title</td>
        <td>string</td>
        <td>The title of the activity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>verb</td>
        <td>string</td>
        <td>The verb of the activity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Actor

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>displayname</td>
        <td>string</td>
        <td>The display of the name of the actor.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>objecttype</td>
        <td>string</td>
        <td>The type of the actor.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>objectuuId</td>
        <td>string</td>
        <td>The UUID of the actor.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>entitytype</td>
        <td>string</td>
        <td>The entitytype of the actor.</td>
        <td>false</td>
    </tr>
</table>

## AddPermission

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>operation</td>
        <td>string</td>
        <td>A comma-delimited set of HTTP methods (GET, PUT, POST, DELETE) that are allowed for the specified resource path.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>resource_path</td>
        <td>string</td>
        <td>The path to the resources to be accessed.</td>
        <td>true</td>
    </tr>
</table>

## AddRole

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>title</td>
        <td>string</td>
        <td>The title of the role.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>role name</td>
        <td>string</td>
        <td>The name of the role.</td>
        <td>true</td>
    </tr>
</table>

## AdminUserUpdate

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>city</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>state</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## AppData

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>tester/sandbox</td>
        <td>string</td>
        <td>The UUID of tester/sandbox.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>tester/app1</td>
        <td>string</td>
        <td>The UUID of tester/app1.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>tester/app2</td>
        <td>string</td>
        <td>The UUID of tester/app2.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## CancelMSG

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>canceled</td>
        <td>boolean</td>
        <td></td>
        <td>true</td>
    </tr>
</table>

## Collections

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>activities</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>feed</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>roles</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>users</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## CreateActivity

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>displayName</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>image</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>verb</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>content</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
</table>

## CreateAdminUser

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>email</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>password</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## CreateApp

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>token</td>
        <td>string</td>
        <td>The OAuth2 access token.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>The name of the application.</td>
        <td>true</td>
    </tr>
</table>

## CreateEntities

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
</table>

## CreateEntity

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>property</td>
        <td>string</td>
        <td>The property of the entity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>value</td>
        <td>string</td>
        <td>The relevant value of the property.</td>
        <td>false</td>
    </tr>
</table>

## CreateEvent

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>timestamp</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
</table>

## CreateGroup

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>title</td>
        <td>string</td>
        <td>The title of the group.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>path</td>
        <td>string</td>
        <td>The path of the group.</td>
        <td>true</td>
    </tr>
</table>

## CreateNotification

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>notifier</td>
        <td>ref</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>message</td>
        <td>string</td>
        <td>The push notitfication message that will be delivered to the user.</td>
        <td>true</td>
    </tr>
</table>

## CreateNotifications

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
</table>

## CreateOrg

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>organization</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>email</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>password</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
</table>

## CreateUser

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
</table>

## Credential

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>client_Id</td>
        <td>string</td>
        <td>The Id of the client.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>client_secret</td>
        <td>string</td>
        <td>The secret of the client.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Device

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td>Unique entity Id.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>type</td>
        <td>string</td>
        <td>Type of entity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>Notifier display name.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>metadata</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Entity

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td>The UUID of the entity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>type</td>
        <td>string</td>
        <td>The type of the entity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>actorproperty</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>metadata</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>The name of the entity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>message</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Error

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>code</td>
        <td>integer</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>message</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>fields</td>
        <td>object</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Event

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>applicationName</td>
        <td>string</td>
        <td>The application name of the event.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>entity</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>url</td>
        <td>string</td>
        <td>The url of the event.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>applicationId</td>
        <td>string</td>
        <td>The application UUID of the event.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>parameters</td>
        <td>string</td>
        <td>The parameters of the event.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>organization</td>
        <td>string</td>
        <td>The title of the organization.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Group

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td>The UUID of the group.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>type</td>
        <td>string</td>
        <td>The type of the group.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>created</td>
        <td>string</td>
        <td>The created Id for the group.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>modified</td>
        <td>string</td>
        <td>The modified Id for the group.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>path</td>
        <td>string</td>
        <td>The path of the group.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>metadata</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>title</td>
        <td>string</td>
        <td>The title of the group.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## ImageModel

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>url</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>wIdth</td>
        <td>integer</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>email</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Metadata

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>cursor</td>
        <td>string</td>
        <td>The cursor of the metadata.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>path</td>
        <td>string</td>
        <td>The path of the metadata.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>sets</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>collections</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Notification

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td>Unique entity Id.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>type</td>
        <td>string</td>
        <td>Type of entity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>payloads</td>
        <td>string</td>
        <td>The push notifications to be delivered.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>errorMessage</td>
        <td>string</td>
        <td>Error message returned by the notification service (APNs or GCM) if the notification fails entirely.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>scheduled</td>
        <td>boolean</td>
        <td>whether the notification is currently scheduled for delivery.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>state</td>
        <td>string</td>
        <td>The current delivery status of the notification &#39;FINISHED&#39;, &#39;SCHEDULED&#39; or &#39;CANCELED&#39;.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>metadata</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Notifier

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td>Unique entity Id.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>type</td>
        <td>string</td>
        <td>Type of entity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>Notifier display name.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>provider</td>
        <td>string</td>
        <td>Push notification provider &#39;apple&#39; or &#39;google&#39;.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>environment</td>
        <td>string</td>
        <td>The environment that corresponds to your app &#39;development&#39; or &#39;production&#39;.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>metadata</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Object

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>displayname</td>
        <td>string</td>
        <td>The display of the name of the object.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>objecttype</td>
        <td>string</td>
        <td>The type of the object.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>objectuuId</td>
        <td>string</td>
        <td>The UUID of the object.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>entitytype</td>
        <td>string</td>
        <td>The entitytype of the object.</td>
        <td>false</td>
    </tr>
</table>

## Organization

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>applicationId</td>
        <td>string</td>
        <td>The application Id of the owner.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td>The username of the owner.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>The name of the owner.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>email</td>
        <td>string</td>
        <td>The email of the owner.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>activated</td>
        <td>boolean</td>
        <td>Indicate whether the account is activated or not.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>disabled</td>
        <td>boolean</td>
        <td>Indicate whether the account is disabled or not.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td>The UUID of the owner.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>adminUser</td>
        <td>boolean</td>
        <td>Indicate whether the use is a adminUser or not.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>displayEmail</td>
        <td>string</td>
        <td>The display of the email of the owner.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>htmldisplayEmail</td>
        <td>string</td>
        <td>The HTML display of the email of the owner.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>orgname</td>
        <td>string</td>
        <td>The name of the organization.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>orguuId</td>
        <td>string</td>
        <td>The UUID of the organization.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>applicationdata</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Permission

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>applicationName</td>
        <td>string</td>
        <td>The name of the application of the permission.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>entity</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>url</td>
        <td>string</td>
        <td>The url of the permission.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>applicationId</td>
        <td>string</td>
        <td>The UUID of the application.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>parameters</td>
        <td>string</td>
        <td>The parameters of the permission.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>organization</td>
        <td>string</td>
        <td>The organization of the permission.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>permissiondata</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## Receipt

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td>Unique entity Id.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>type</td>
        <td>string</td>
        <td>Type of entity.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>payloads</td>
        <td>string</td>
        <td>The push notifications to be delivered.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>errorMessage</td>
        <td>string</td>
        <td>Error message returned by the notification service (APNs or GCM) if the notification fails entirely.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>errorCode</td>
        <td>string</td>
        <td>Error code returned by the notification service.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>sent</td>
        <td>number</td>
        <td>UTC timestamp in milliseconds for when the notification was sent.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>metadata</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## ResetPW

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>password</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
    <tr>
        <td>newpassword</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
</table>

## ResetPWMsg

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>recaptcha_response</td>
        <td>string</td>
        <td>Parameters and value for the Captcha challenge.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>recaptcha_challenge</td>
        <td>string</td>
        <td>The admin user&#39;s response to the Captcha challenge.</td>
        <td>true</td>
    </tr>
    <tr>
        <td>email</td>
        <td>string</td>
        <td></td>
        <td>true</td>
    </tr>
</table>

## Role

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>applicationName</td>
        <td>string</td>
        <td>The application name of the event.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>entity</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>url</td>
        <td>string</td>
        <td>The url of the event.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>applicationId</td>
        <td>string</td>
        <td>The application UUID of the event.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>parameters</td>
        <td>string</td>
        <td>The parameters of the event.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>organization</td>
        <td>string</td>
        <td>The title of the organization.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>path</td>
        <td>string</td>
        <td>The path of the role.</td>
        <td>false</td>
    </tr>
</table>

## ScheduleNotification

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>timestamp</td>
        <td>string</td>
        <td>UTC timestamp in milliseconds of when schedule notifications.</td>
        <td>true</td>
    </tr>
</table>

## SetExpiration

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>timestamp</td>
        <td>string</td>
        <td>UTC timestamp in milliseconds of when set expirations.</td>
        <td>true</td>
    </tr>
</table>

## Sets

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>rolenames</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
    <tr>
        <td>permissions</td>
        <td>string</td>
        <td></td>
        <td>false</td>
    </tr>
</table>

## User

__Properties__ 

<table width="80%" class="usergrid-table">
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
        <th>Required</th>
    </tr>
    <tr>
        <td>applicationId</td>
        <td>string</td>
        <td>The application Id of a user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td>The username of a user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>The name of a user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>email</td>
        <td>string</td>
        <td>The email of a user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>activated</td>
        <td>boolean</td>
        <td>Indicate whether the account is activated or not.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>disabled</td>
        <td>boolean</td>
        <td>Indicate whether the account is disabled or not.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>uuid</td>
        <td>string</td>
        <td>The UUID of a user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>adminUser</td>
        <td>boolean</td>
        <td>Indicate whether the use is a adminUser or not.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>displayEmail</td>
        <td>string</td>
        <td>The display of the email of a user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>htmldisplayEmail</td>
        <td>string</td>
        <td>The HTML display of the email of a user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>organization</td>
        <td>string</td>
        <td>The organization of the user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>picture</td>
        <td>string</td>
        <td>The uri of the user&#39;s picture.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>uri</td>
        <td>string</td>
        <td>The uri of the user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>path</td>
        <td>string</td>
        <td>The path of the user.</td>
        <td>false</td>
    </tr>
    <tr>
        <td>completeMsg</td>
        <td>ref</td>
        <td></td>
        <td>false</td>
    </tr>
</table>
