<h1>Usergrid API Reference</h1>
    
Methods are organized by tag. Follow the methods are the [Model Definitions](#models).

<h2>Table of Contents</h2>

* [Access-Tokens](#access-tokens)
* [Activities](#activities)
* [Admin-Users](#admin-users)
* [App-Users](#app-users)
* [Entities-Collections](#entities-collections)
* [Events](#events)
* [Groups](#groups)
* [Notifications](#notifications)
* [Organizations-Applications](#organizations-applications)
* [Permissions-Roles](#permissions-roles)

<br>
<br>


## Methods


### Access-Tokens


<h2 class="usergrid-POST-heading">POST /management/token</h2>

<a name="op-0bzf2cyzsh"></a>

Login with Admin-User or Organization credentials.

<h3>Parameters</h3>

* __login-credentials__ ([LoginCredentials](#logincredentials))
Login credentials either username/password or id/secret. (Specified in body).

<h3>Responses</h3>

__200__

* Description: Object containing access_token.
* Schema: [AccessTokenResponse](#accesstokenresponse)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/token</h2>

<a name="op-svs0512krj"></a>

Login with App-User or Application credentials.

<h3>Parameters</h3>

* __login-credentials__ ([LoginCredentials](#logincredentials))
Login credentials either username/password or id/secret. (Specified in body).

<h3>Responses</h3>

__200__

* Description: Object containing access_token.
* Schema: [AccessTokenResponse](#accesstokenresponse)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

### Activities


<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/groups/{groupId}/feed</h2>

<a name="op-aektocz9dh"></a>

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

<a name="op-nxy02yvl0h"></a>

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

<a name="op-o8teq5gp8a"></a>

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
    

### Admin-Users


<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/users</h2>

<a name="op-fihfmp3tme"></a>

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

<a name="op-bsf4a7eiyt"></a>

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

<a name="op-jkvb4rpfti"></a>

Create a whole new admin user.

<h3>Parameters</h3>

* __CreateAdminUser__ ([CreateAdminUser](#createadminuser))
User entity with fields required for User creation. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An API Response with a entities array containing the newly created Admin User.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/users/resetpw</h2>

<a name="op-ivi39w3d6n"></a>

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

<a name="op-8iu1cdqxul"></a>

Complete the password reset through getting the newpassword and the old one for identification.

<h3>Parameters</h3>

* __ResetPWMsg__ ([ResetPWMsg](#resetpwmsg))
Parameters and value for the Captcha challenge, the admin user&#39;s response to the Captcha challenge, and the admin user&#39;s email address. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [](#)
    
__default__

* Description: 
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/users/{userId}</h2>

<a name="op-ok1xm0q0no"></a>

Retrieve details about an admin user.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An API Response with a entities array containing the Admin User.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /management/users/{userId}</h2>

<a name="op-ukwxjinvyf"></a>

Update the info of an admin user.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An API Response with a entities array containing the updated Admin User
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/users/{userId}/activate</h2>

<a name="op-xxue8ovmq9"></a>

Activate an admin user from a link provIded in an email notification.

<h3>Parameters</h3>

* __userId__ (string)
One of the user&#39;s identification which includes username, real name, email address or UUID. (Specified in path).
* __token__ (string)
Activation token&#39;s query statement. (Specified in query).
* __confirm_email__ (boolean)
Query statement of whether send confimation email or not. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /management/users/{userId}/password</h2>

<a name="op-xpx6gbuzfw"></a>

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

<a name="op-xzwpxdciff"></a>

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
    

### App-Users


<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/users</h2>

<a name="op-jndyp0tykz"></a>

Retrieve users though query statement.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __queryStatement__ (string)
The query statement of the User. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of retrieved user&#39;s info.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/users</h2>

<a name="op-aioxrj1eep"></a>

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

<a name="op-snzs7adf7o"></a>

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

<a name="op-euchoncmpy"></a>

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

<a name="op-2vwm8uuxgr"></a>

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
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/users/{user}/password</h2>

<a name="op-1vbgigzor7"></a>

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
    

### Entities-Collections


<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/users/{userId}/{relation}</h2>

<a name="op-nsdxcm2w07"></a>

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
* __queryStatement__ (string)
The query statement of the user. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of user&#39;s collections info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/{collectionId}</h2>

<a name="op-da7vw2iqa7"></a>

Retrieve collection through query statement.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __queryStatement__ (string)
Any values specified in the query statement should be enclosed in single-quotes. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of retrieved collection&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /{orgId}/{appId}/{collectionId}</h2>

<a name="op-17qn0rvgpm"></a>

Update collection through query statement.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __collectionId__ (string)
One of the collection&#39;s identification which includes name or uuid. (Specified in path).
* __queryStatement__ (string)
Any values specified in the query statement should be enclosed in single-quotes. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of updated collection&#39;s info.
* Schema: [Entity](#entity)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/{collectionId}/{entityId1}/{relation}/{entityId2}</h2>

<a name="op-6dzb3xz0wq"></a>

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

<a name="op-inrrw1fqal"></a>

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

<a name="op-psiiivedhb"></a>

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

<a name="op-7fclidczs6"></a>

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

<a name="op-9n0wuse9mv"></a>

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

<a name="op-rtg6radfaq"></a>

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
    

### Events


<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/events</h2>

<a name="op-z0n83adeua"></a>

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
    

### Groups


<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/groups</h2>

<a name="op-qvluwgdixe"></a>

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
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/groups/{groupId}/activities</h2>

<a name="op-kmrjsftwj3"></a>

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

* Description: 
* Schema: [ActivityFeed](#activityfeed)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/groups/{groupId}/users/{userId}</h2>

<a name="op-79bstryyph"></a>

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

<a name="op-gvqutatb2n"></a>

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
    

<h2 class="usergrid-GET-heading">GET /{org_Id}/{app_Id}/groups/{groupId}</h2>

<a name="op-tj3j6ygyhz"></a>

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

<a name="op-dxpamkwi1o"></a>

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
    

### Notifications


<h2 class="usergrid-POST-heading">POST /{orgId}/{applicationId}/devices</h2>

<a name="op-8xzbizak9m"></a>

Create notifications for user through targeting by location and providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).
* __queryStatement__ (string)
The query statement of the location of the user. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{applicationId}/devices/*/notifications</h2>

<a name="op-ync3aqk68j"></a>

Create notifications for all devices. This request will target all device entities.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{applicationId}/devices/{deviceId}/notifications</h2>

<a name="op-ghzaijhfrd"></a>

Create notifications for a single device. This request will target a specific device entity.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __deviceId__ (string)
One of the device&#39;s identification which includes name or uuid. (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{applicationId}/groups/{path}/notifications</h2>

<a name="op-scytqcmlbb"></a>

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

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{applicationId}/notifications</h2>

<a name="op-wao4e4x2ax"></a>

Retrieve one or more notifications through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-PUT-heading">PUT /{orgId}/{applicationId}/notifications/{notificationId}</h2>

<a name="op-pfkq4xfvl4"></a>

Update a Notification in order to cancel the notifcation or set a new expiration time.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __notificationId__ (string)
One of the notification&#39;s identification which includes name or uuid. (Specified in path).
* __notificationUpdate__ ([NotificationUpdate](#notificationupdate))
Object with Notification fields to be updated. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An API Response object containing an entity of type Notification.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{applicationId}/notifications/{notificationId}</h2>

<a name="op-oajwu1ezyb"></a>

Delete an unsent Notification from the system.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __notificationId__ (string)
One of the notification&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: API Response containing Notification entity that was deleted.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{applicationId}/receipts</h2>

<a name="op-bhxobegwk0"></a>

Retrieve one or more receipts through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved receipt&#39;s info.
* Schema: [Receipt](#receipt)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{applicationId}/users/{userId}/notifications</h2>

<a name="op-uts7flpbdb"></a>

Create notifications for a user. This request will target a specific user entity.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __userId-3__ (string)
One of the user&#39;s identification which includes username or UUID. (Specified in path).
* __notification__ ([CreateNotifications](#createnotifications))
These parameters are used when forming the notification portion of the request. (Specified in body).

<h3>Responses</h3>

__200__

* Description: An array of created notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{applicationId}/{deviceId}/*/receipts</h2>

<a name="op-opyjl7eroi"></a>

Retrieve receipts associated with one or more devices through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __deviceId__ (string)
One of the device&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved receipt&#39;s info.
* Schema: [Receipt](#receipt)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{applicationId}/{notificationId}/*/queue</h2>

<a name="op-5iqasgv3sj"></a>

Retrieve the list of devices associated with one or more notifications before the notifications are sent through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __notificationId__ (string)
One of the notification&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved device&#39;s info.
* Schema: [Device](#device)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{applicationId}/{notificationId}/*/receipts</h2>

<a name="op-unjmumv5kh"></a>

Retrieve receipts for one or more notifications through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __notificationId__ (string)
One of the notification&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved receipt&#39;s info.
* Schema: [Receipt](#receipt)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{applicationId}/{receiptId}/*/notifications</h2>

<a name="op-iejp0wlehp"></a>

Retrieve notifications associated with one or more receipts through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __applicationId__ (string)
One of the application&#39;s identification which includes name or uuid (same as appId). (Specified in path).
* __receiptId__ (string)
One of the receipt&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: An array of retrieved notification&#39;s info.
* Schema: [Notification](#notification)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

### Organizations-Applications


<h2 class="usergrid-POST-heading">POST /management/orgs</h2>

<a name="op-asuflut8jl"></a>

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

<a name="op-brcy1p5pv6"></a>

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
    

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/activate</h2>

<a name="op-volzlcrizg"></a>

Activate an organization from a link provIded in an email notification.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __token__ (string)
Activation token. (Specified in query).
* __confirm_email__ (boolean)
Send confirmation email or not. (Specified in query).

<h3>Responses</h3>

__200__

* Description: An array of complete messages.
* Schema: [Action](#action)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/apps</h2>

<a name="op-jwpqgx4byw"></a>

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

<a name="op-fzlitpxydy"></a>

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
    

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/apps/{appId}/credentials</h2>

<a name="op-k1cjl2fbfn"></a>

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

<a name="op-kb4kkywf0c"></a>

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

<a name="op-j6mwijxj8v"></a>

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

<a name="op-btrg9is9ex"></a>

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
    

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/feed</h2>

<a name="op-rxtw5leuxn"></a>

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
    

<h2 class="usergrid-GET-heading">GET /management/orgs/{orgId}/reactivate</h2>

<a name="op-2y9lik1je1"></a>

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
    

<h2 class="usergrid-GET-heading">GET /management/users/{userId}/feed</h2>

<a name="op-07muhrw2rk"></a>

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
    

### Permissions-Roles


<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/roles</h2>

<a name="op-ehipmupci4"></a>

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

<a name="op-rrdx4m4m0p"></a>

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
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/roles/{roleId}/permissions</h2>

<a name="op-cwmnfuikul"></a>

Remove permissions from a role.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __roleId__ (string)
One of the role&#39;s identification which includes name or uuid. (Specified in path).
* __Permissions__ ([Permissions](#permissions))
The query statement of the url pattern. (Specified in body).

<h3>Responses</h3>

__200__

* Description: Permissions object with array of the deleated Usergrid Permission strings.
* Schema: [Permissions](#permissions)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{appId}/roles/{roleId}/users</h2>

<a name="op-so5cnn3qsk"></a>

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

* Description: An API Response with a entities array of Users.
* Schema: [User](#user)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{appId}/roles/{roleId}/users/{userId}</h2>

<a name="op-fo857v2csw"></a>

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

<a name="op-3hxunwulpo"></a>

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
    

<h2 class="usergrid-DELETE-heading">DELETE /{orgId}/{appId}/roles/{rolename}</h2>

<a name="op-m9gq8yaswj"></a>

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
    

<h2 class="usergrid-GET-heading">GET /{orgId}/{applicationId}/roles/{roleId}/permissions</h2>

<a name="op-8xgaadcvi8"></a>

Retrieve permissions for a Role.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __roleId__ (string)
One of the role&#39;s identification which includes name or uuid. (Specified in path).

<h3>Responses</h3>

__200__

* Description: Permissions object with array of Usergrid Permission strings.
* Schema: [Permissions](#permissions)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

<h2 class="usergrid-POST-heading">POST /{orgId}/{applicationId}/roles/{roleId}/permissions</h2>

<a name="op-bgabubs4iw"></a>

Add permissions to a role through providing all the identifications.

<h3>Parameters</h3>

* __orgId__ (string)
One of the organization&#39;s identification which includes name or uuid. (Specified in path).
* __appId__ (string)
One of the application&#39;s identification which includes name or uuid. (Specified in path).
* __roleId__ (string)
One of the role&#39;s identification which includes name or uuid. (Specified in path).
* __Permissions__ ([Permissions](#permissions))
Permissions object with array of Usergrid Permission strings to be added. (Specified in body).

<h3>Responses</h3>

__200__

* Description: Permissions object with array of Usergrid Permission strings.
* Schema: [Permission](#permission)
    
__default__

* Description: Unexpected error.
* Schema: [Error](#error)
    

## Models
This section lists the properties for the Usergrid Default Entities:

### AccessTokenResponse



__Properties__ 

<table width="80%" class="usergrid-table">
  <tr>
      <th>Name</th>
      <th>Type</th>
      <th>Description</th>
      <th>Required</th>
  </tr>
  <tr>
      <td>access_token</td>
      <td>
           string
      </td>
      <td>Access-token that may be used on subsequent requests.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>expires_in</td>
      <td>
           number
      </td>
      <td>Time (in milliseconds) until access-token expires.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>user</td>
      <td>
           [User](#user)  
      </td>
      <td>User object if login was done as a user.</td>
      <td>false</td>
  </tr>
</table>


### Action



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
      <td>
           string
      </td>
      <td>The requested action.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>status</td>
      <td>
           string
      </td>
      <td>The status of the requested action.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>timestamp</td>
      <td>
           number
      </td>
      <td>The timestamp of the requested action.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>duration</td>
      <td>
           number
      </td>
      <td>The duration of the requested action.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>token</td>
      <td>
           string
      </td>
      <td>The token required for getting an AdminUser.</td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [Receipt](#receipt)
* [Device](#device)
* [Notification](#notification)
* [Role](#role)
* [Event](#event)
* [Group](#group)
* [Credential](#credential)
* [Organization](#organization)
* [AppData](#appdata)
* [User](#user)
* [ActivityFeed](#activityfeed)
      

### ActivityFeed



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
      <td>
           [Entity](#entity)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>category</td>
      <td>
           string
      </td>
      <td>The category of the activity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>metadataproperty</td>
      <td>
           [Metadata](#metadata)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>objectproperty</td>
      <td>
           [Object](#object)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>title</td>
      <td>
           string
      </td>
      <td>The title of the activity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>verb</td>
      <td>
           string
      </td>
      <td>The verb of the activity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>published</td>
      <td>
           number
      </td>
      <td>UTC timestamp of the feed publish time.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### AddRole



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
      <td>
           string
      </td>
      <td>The title of the role.</td>
      <td>true</td>
  </tr>
  <tr>
      <td>role name</td>
      <td>
           string
      </td>
      <td>The name of the role.</td>
      <td>true</td>
  </tr>
</table>


### AppData



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
      <td>
           string
      </td>
      <td>The UUID of tester/sandbox.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>tester/app1</td>
      <td>
           string
      </td>
      <td>The UUID of tester/app1.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>tester/app2</td>
      <td>
           string
      </td>
      <td>The UUID of tester/app2.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [Organization](#organization)
      

### CreateActivity



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
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>uuid</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>username</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>image</td>
      <td>
           [ImageModel](#imagemodel)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>verb</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>content</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
</table>


### CreateAdminUser



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
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>email</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>name</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>password</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### CreateEntities



__Properties__ 

<table width="80%" class="usergrid-table">
  <tr>
      <th>Name</th>
      <th>Type</th>
      <th>Description</th>
      <th>Required</th>
  </tr>
</table>


### CreateEvent



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
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
</table>


### CreateGroup



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
      <td>
           string
      </td>
      <td>The title of the group.</td>
      <td>true</td>
  </tr>
  <tr>
      <td>path</td>
      <td>
           string
      </td>
      <td>The path of the group.</td>
      <td>true</td>
  </tr>
</table>


### CreateNotifications

An array of Notifications to be created.

__Properties__ 

<table width="80%" class="usergrid-table">
  <tr>
      <th>Name</th>
      <th>Type</th>
      <th>Description</th>
      <th>Required</th>
  </tr>
</table>


### CreateOrg



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
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>username</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>name</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>email</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>password</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
</table>


### CreateUser



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
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
</table>


### Credential



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
      <td>
           string
      </td>
      <td>The Id of the client.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>client_secret</td>
      <td>
           string
      </td>
      <td>The secret of the client.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### Device

Represents a single Device that is registered for recieving of Push Notifications.

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
      <td>
           string
      </td>
      <td>Unique entity Id.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>type</td>
      <td>
           string
      </td>
      <td>Type of entity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>name</td>
      <td>
           string
      </td>
      <td>Notifier display name.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>created</td>
      <td>
           number
      </td>
      <td>UTC timestamp in milliseconds of when the entity was created.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>modified</td>
      <td>
           number
      </td>
      <td>UTC timestamp in milliseconds of when the entity was last modified.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>metadata</td>
      <td>
           [Metadata](#metadata)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### Entity



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
      <td>
           string
      </td>
      <td>The UUID of the entity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>type</td>
      <td>
           string
      </td>
      <td>The type of the entity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>created</td>
      <td>
           number
      </td>
      <td>UTC timestamp of entity creation time.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>modified</td>
      <td>
           [Actor](#actor)  
      </td>
      <td>UTC timestamp of entity modified time.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>metadata</td>
      <td>
           [Metadata](#metadata)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>name</td>
      <td>
           string
      </td>
      <td>The name of the entity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>message</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [Role](#role)
* [Event](#event)
* [ActivityFeed](#activityfeed)
      

### Error



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
      <td>
           integer
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>message</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>fields</td>
      <td>
           object
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### Event



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
      <td>
           string
      </td>
      <td>The application name of the event.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>entity</td>
      <td>
           [Entity](#entity)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>url</td>
      <td>
           string
      </td>
      <td>The url of the event.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>applicationId</td>
      <td>
           string
      </td>
      <td>The application UUID of the event.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>parameters</td>
      <td>
           string
      </td>
      <td>The parameters of the event.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>organization</td>
      <td>
           string
      </td>
      <td>The title of the organization.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### Group



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
      <td>
           string
      </td>
      <td>The UUID of the group.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>type</td>
      <td>
           string
      </td>
      <td>The type of the group.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>created</td>
      <td>
           string
      </td>
      <td>The created Id for the group.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>modified</td>
      <td>
           string
      </td>
      <td>The modified Id for the group.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>path</td>
      <td>
           string
      </td>
      <td>The path of the group.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>metadata</td>
      <td>
           [Metadata](#metadata)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>title</td>
      <td>
           string
      </td>
      <td>The title of the group.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### LoginCredentials



__Properties__ 

<table width="80%" class="usergrid-table">
  <tr>
      <th>Name</th>
      <th>Type</th>
      <th>Description</th>
      <th>Required</th>
  </tr>
  <tr>
      <td>grant_type</td>
      <td>
           string
      </td>
      <td>Grant-type must be &#39;password&#39; or &#39;client_credentials&#39;.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>username</td>
      <td>
           string
      </td>
      <td>Username of user attempting login, required only if grant_type is &#39;password&#39;.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>password</td>
      <td>
           string
      </td>
      <td>Password of user attempting login, required only if grant_type is &#39;password&#39;.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>client_id</td>
      <td>
           string
      </td>
      <td>Client-ID portion of credentials, required only if grant_type is &#39;client_credentials&#39;.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>client_secret</td>
      <td>
           string
      </td>
      <td>Client-Secret portion of credentials, required only if grant_type is &#39;client_credentials&#39;.</td>
      <td>false</td>
  </tr>
</table>


### Notification

Represents a Push Notification that is either scheduled, finished or cancelled.

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
      <td>
           string
      </td>
      <td>Unique entity Id.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>type</td>
      <td>
           string
      </td>
      <td>Type of entity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>created</td>
      <td>
           number
      </td>
      <td>UTC timestamp in milliseconds of when the entity was created.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>modified</td>
      <td>
           number
      </td>
      <td>UTC timestamp in milliseconds of when the entity was last modified.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>payloads</td>
      <td>
           string
      </td>
      <td>The push notifications to be delivered.</td>
      <td>true</td>
  </tr>
  <tr>
      <td>errorMessage</td>
      <td>
           string
      </td>
      <td>Error message returned by the notification service (APNs or GCM) if the notification fails entirely.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>scheduled</td>
      <td>
           boolean
      </td>
      <td>Whether the notification is currently scheduled for delivery.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>state</td>
      <td>
           string
      </td>
      <td>The current delivery status of the notification &#39;FINISHED&#39;, &#39;SCHEDULED&#39; or &#39;CANCELED&#39;.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>metadata</td>
      <td>
           [Metadata](#metadata)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### NotificationUpdate

Represents fields that may be updated on a Notification to cause changes in Push Notification processing.

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
      <td>
           boolean
      </td>
      <td>Setting this field to true will cancel a Notification, if it has not yet been sent.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>deliver</td>
      <td>
           number
      </td>
      <td>Specifies the UNIX timestamp time at which the Notification should be sent.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>expired</td>
      <td>
           number
      </td>
      <td>Specifies the UNIX timestamp time at which this Notification has expired.</td>
      <td>false</td>
  </tr>
</table>


### Organization



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
      <td>
           string
      </td>
      <td>The application Id of the owner.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>username</td>
      <td>
           string
      </td>
      <td>The username of the owner.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>name</td>
      <td>
           string
      </td>
      <td>The name of the owner.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>email</td>
      <td>
           string
      </td>
      <td>The email of the owner.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>activated</td>
      <td>
           boolean
      </td>
      <td>Indicate whether the account is activated or not.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>disabled</td>
      <td>
           boolean
      </td>
      <td>Indicate whether the account is disabled or not.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>uuid</td>
      <td>
           string
      </td>
      <td>The UUID of the owner.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>adminUser</td>
      <td>
           boolean
      </td>
      <td>Indicate whether the use is a adminUser or not.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>displayEmail</td>
      <td>
           string
      </td>
      <td>The display of the email of the owner.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>htmldisplayEmail</td>
      <td>
           string
      </td>
      <td>The HTML display of the email of the owner.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>orgname</td>
      <td>
           string
      </td>
      <td>The name of the organization.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>orguuId</td>
      <td>
           string
      </td>
      <td>The UUID of the organization.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>applicationdata</td>
      <td>
           [AppData](#appdata)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### Permissions

Represents a set of Permissions associated with a User or a Role, each being a Usergrid Permission String.

__Properties__ 

<table width="80%" class="usergrid-table">
  <tr>
      <th>Name</th>
      <th>Type</th>
      <th>Description</th>
      <th>Required</th>
  </tr>
  <tr>
      <td>application</td>
      <td>
           string
      </td>
      <td>The UUID of the associated application.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>applicationName</td>
      <td>
           string
      </td>
      <td>The name of the associated application.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>organization</td>
      <td>
           string
      </td>
      <td>The name of the associated organization.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>data</td>
      <td>
           array
      </td>
      <td>Array of strings each being a Usergrid Permission String.</td>
      <td>true</td>
  </tr>
</table>


### Receipt

Represents response received from Notification service indicating success or failure.

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
      <td>
           string
      </td>
      <td>Unique entity Id.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>type</td>
      <td>
           string
      </td>
      <td>Type of entity.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>created</td>
      <td>
           number
      </td>
      <td>UTC timestamp in milliseconds of when the entity was created.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>modified</td>
      <td>
           number
      </td>
      <td>UTC timestamp in milliseconds of when the entity was last modified.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>payloads</td>
      <td>
           string
      </td>
      <td>The push notifications to be delivered.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>errorMessage</td>
      <td>
           string
      </td>
      <td>Error message returned by the notification service (APNs or GCM) if the notification fails entirely.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>errorCode</td>
      <td>
           string
      </td>
      <td>Error code returned by the notification service.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>sent</td>
      <td>
           number
      </td>
      <td>UTC timestamp in milliseconds for when the notification was sent.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>metadata</td>
      <td>
           [Metadata](#metadata)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>


### ResetPW



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
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
  <tr>
      <td>newpassword</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
</table>


### ResetPWMsg



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
      <td>
           string
      </td>
      <td>Parameters and value for the Captcha challenge.</td>
      <td>true</td>
  </tr>
  <tr>
      <td>recaptcha_challenge</td>
      <td>
           string
      </td>
      <td>The admin user&#39;s response to the Captcha challenge.</td>
      <td>true</td>
  </tr>
  <tr>
      <td>email</td>
      <td>
           string
      </td>
      <td></td>
      <td>true</td>
  </tr>
</table>


### Role



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
      <td>
           string
      </td>
      <td>The application name of the event.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>count</td>
      <td>
           number
      </td>
      <td>The numebr of the roles.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>entity</td>
      <td>
           [Entity](#entity)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>url</td>
      <td>
           string
      </td>
      <td>The url of the event.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>applicationId</td>
      <td>
           string
      </td>
      <td>The application UUID of the event.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>parameters</td>
      <td>
           string
      </td>
      <td>The parameters of the event.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>organization</td>
      <td>
           string
      </td>
      <td>The title of the organization.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>path</td>
      <td>
           string
      </td>
      <td>The path of the role.</td>
      <td>false</td>
  </tr>
</table>


### User

Represents a User account which may be a user within an Application&#39;s User collection, or may be an Admin User.

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
      <td>
           string
      </td>
      <td>The application Id of a user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>username</td>
      <td>
           string
      </td>
      <td>The username of a user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>name</td>
      <td>
           string
      </td>
      <td>The name of a user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>email</td>
      <td>
           string
      </td>
      <td>The email of a user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>activated</td>
      <td>
           boolean
      </td>
      <td>Indicate whether the account is activated or not.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>disabled</td>
      <td>
           boolean
      </td>
      <td>Indicate whether the account is disabled or not.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>uuid</td>
      <td>
           string
      </td>
      <td>The UUID of a user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>adminUser</td>
      <td>
           boolean
      </td>
      <td>Indicate whether the use is a adminUser or not.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>displayEmail</td>
      <td>
           string
      </td>
      <td>The display of the email of a user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>htmldisplayEmail</td>
      <td>
           string
      </td>
      <td>The HTML display of the email of a user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>organization</td>
      <td>
           string
      </td>
      <td>The organization of the user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>picture</td>
      <td>
           string
      </td>
      <td>The uri of the user&#39;s picture.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>uri</td>
      <td>
           string
      </td>
      <td>The uri of the user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>path</td>
      <td>
           string
      </td>
      <td>The path of the user.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>completeMsg</td>
      <td>
           [Action](#action)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [AccessTokenResponse](#accesstokenresponse)
      

## Sub-Types
This section lists the properties for sub-types used in Usergrid Default Entities.

### Collections



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
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>feed</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>roles</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>users</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [Metadata](#metadata)
      

### ImageModel



__Properties__ 

<table width="80%" class="usergrid-table">
  <tr>
      <th>Name</th>
      <th>Type</th>
      <th>Description</th>
      <th>Required</th>
  </tr>
  <tr>
      <td>duration</td>
      <td>
           number
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>height</td>
      <td>
           number
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>url</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>wIdth</td>
      <td>
           integer
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>email</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [CreateActivity](#createactivity)
      

### Metadata



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
      <td>
           string
      </td>
      <td>The cursor of the metadata.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>path</td>
      <td>
           string
      </td>
      <td>The path of the metadata.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>sets</td>
      <td>
           [Sets](#sets)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>collections</td>
      <td>
           [Collections](#collections)  
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [Receipt](#receipt)
* [Device](#device)
* [Notification](#notification)
* [Notifier](#notifier)
* [Group](#group)
* [Entity](#entity)
* [ActivityFeed](#activityfeed)
      

### Object



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
      <td>
           string
      </td>
      <td>The display of the name of the object.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>objecttype</td>
      <td>
           string
      </td>
      <td>The type of the object.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>objectuuId</td>
      <td>
           string
      </td>
      <td>The UUID of the object.</td>
      <td>false</td>
  </tr>
  <tr>
      <td>entitytype</td>
      <td>
           string
      </td>
      <td>The entitytype of the object.</td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [ActivityFeed](#activityfeed)
      

### Sets



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
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
  <tr>
      <td>permissions</td>
      <td>
           string
      </td>
      <td></td>
      <td>false</td>
  </tr>
</table>

__Referring Definitions__
  
* [Metadata](#metadata)
      
