# Working with group data

You can organize app users into groups. Groups have their own Activity Feed, their own permissions and be a useful alternative to Roles, depending on how you model your data. Groups were originally designed to emulate Facebook Groups, so they will tend to function about the same way Facebook Groups would.

Groups are hierarchical. Every member of the group ``/groups/california/san-francisco`` is also a member of the group ``/groups/california``.

Groups are also a great way to model things such a topic subscriptions. For example, you could allow people to subscribe (i.e. become a member of the group and be alerted via Activities) to ``/groups/memes/dogs/doge`` or subscribe to all ``/groups/memes/dogs``.

See the [Group Model section of the API Reference](../rest-endpoints/api-docs.html#group) for a list of the system-defined properties for group entities. In addition, you can create group properties specific to your application.

## Creating groups

A group entity represents an application group of users. You can create, retrieve, update, delete, and query group entities. See [User entity properties](../rest-endpoints/api-doc.html#user) for a list of the system-defined  properties for group entities. In addition, you can create group properties specific to your application.

### Request Syntax

    curl -X POST "https://api.usergrid.com/your-org/your-app/groups" '{ request body }'
    
Use the POST method to create a new group. Groups use paths to indicate their unique names. This allows you to create group hierarchies by using slashes. For this reason, you need to specify a path property for a new group.

### Request URI

    POST /{org_id}/{app_id}/groups

Parameters

Parameter	    Description
---------       -----------
uuid | org_id	Organization UUID or organization name
uuid | app_id	Application UUID or application name
request body	One or more sets of group properties of which path is mandatory.
 
The ``path`` property is required and must be unique, it may include forward slashes to denote hierarchical relationships.

    {
        "path" : "somegroup/somesubgroup",
        "title" : "Some SubGroup"
    }

### Example

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Request

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups" -d '{"path":"mynewgroup"}'
    
### Response

    {
        "action": "post",
        "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
        "params": {},
        "path": "/groups",
        "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/groups",
        "entities": [{
            "uuid": "a668717b-67cb-11e1-8223-12313d14bde7",
            "type": "group",
            "created": 1331066016571,
            "modified": 1331066016571,
            "metadata": {
                "path": "/groups/a668717b-67cb-11e1-8223-12313d14bde7",
                "sets": {
                    "rolenames": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/rolenames",
                    "permissions": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/permissions"
                },
                "collections": {
                    "activities": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/activities",
                    "feed": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/feed",
                    "roles": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/roles",
                    "users": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users"
                }
            },
            "path": "mynewgroup"
        }],
        "timestamp": 1331066016563,
        "duration": 35,
        "organization": "my-org",
        "applicationName": "my-app"
    }

## Retrieving groups

Retrieving group data

You can retrieve data about groups through cURL or one of the SDKs. Each provides a way to filter the list of groups by data associated with the group, such as title or path, or other properties in the group entity.

See the [Group Model section of the API Reference](../rest-endpoints/api-docs.html#group) for a list of the system-defined properties for group entities. In addition, you can create group properties specific to your application.

### Request Syntax

    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/mynewgroup"
    
Use the GET method to retrieve group data.

### Request URI

    GET /<org_id | uuid>/<app_id | uuid>/groups</groupPath | uuid> | <?ql=query_string>

Parameters

Parameter	Description
---------
org_id | uuid	  Organization UUID or organization name
app_id | uuid	  Application UUID or application name
groupPath | uuid  Group UUID or group path, which must be unique.
query_string      A data store query. For more on queries, see [Data queries](../data-queries/querying-your-data.html).

### Request

    # Get a group by the group path, "employees/managers".
    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/employees/managers"

    # Get a group by UUID.
    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/a407b1e7-58e8-11e1-ac46-22000a1c5a67e"

    # Get group data filtering by their title.
    curl -X GET "https://api.usergrid.com/my-org/my-app/groups?ql=select%20*%20where%20title%3D'Management%20Employees'"

### Response

The following is an example of JSON returned by a query for a single group.

    {
        "action" : "get",
        "application" : "db1e60a0-417f-11e3-9586-0f1ff3650d20",
        "params" : { },
        "path" : "/groups",
        "uri" : "https://api.usergrid.com/steventraut/mynewapp/groups",
        "entities" : [ {
            "uuid" : "5005a0fa-6916-11e3-9c1b-b77ec8addc0d",
            "type" : "group",
            "created" : 1387503030399,
            "modified" : 1387503030399,
            "path" : "managers",
            "metadata" : {
                "path" : "/groups/5005a0fa-6916-11e3-9c1b-b77ec8addc0d",
                "sets" : {
                    "rolenames" : "/groups/5005a0fa-6916-11e3-9c1b-b77ec8addc0d/roles",
                    "permissions" : "/groups/5005a0fa-6916-11e3-9c1b-b77ec8addc0d/permissions"
                },
                "collections" : {
                    "activities" : "/groups/5005a0fa-6916-11e3-9c1b-b77ec8addc0d/activities",
                    "feed" : "/groups/5005a0fa-6916-11e3-9c1b-b77ec8addc0d/feed",
                    "roles" : "/groups/5005a0fa-6916-11e3-9c1b-b77ec8addc0d/roles",
                    "users" : "/groups/5005a0fa-6916-11e3-9c1b-b77ec8addc0d/users"
                }
            },
            "title" : "Management Employees"
        } ],
        "timestamp" : 1391020491701,
        "duration" : 15,
        "organization" : "my-org",
        "applicationName" : "my-app"
    }

## Retrieving a group's users

Use the GET method to retrieve all the users in a group.

### Request URI

    GET /{org_id}/{app_id}/groups/{uuid|groupname}/users
    
### Parameters

Parameter	               Description
---------                  --------------
arg uuid|string org_id     Organization UUID or organization name
arg uuid|string app_id     Application UUID or application name
arg uuid|string groupname  UUID or name of the group

### Example - Request

    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/mygroup/users"

### Example - Response

    {
      "action" : "get",
      "application" : "e7127751-6985-11e2-8078-02e81aeb2129",
      "params" : { },
      "path" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users",
      "uri" : "http://api.usergrid.com/myorg/sandbox/groups/d20976ff-802f-11e2-b690-02e81ae61238/users",
      "entities" : [ {
        "uuid" : "cd789b00-698b-11e2-a6e3-02e81ae236e9",
        "type" : "user",
        "name" : "barney",
        "created" : 1359405994314,
        "modified" : 1361894320470,
        "activated" : true,
        "email" : "barney@apigee.com",
        "metadata" : {
          "path" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9",
          "sets" : {
            "rolenames" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/rolenames",
            "permissions" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/permissions"
          },
          "collections" : {
            "activities" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/activities",
            "devices" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/devices",
            "feed" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/feed",
            "groups" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/groups",
            "roles" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/roles",
            "following" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/following",
            "followers" : "/groups/d20976ff-802f-11e2-b690-02e81ae66238/users/cd789b00-698b-11e2-a6e3-02e81aeb26e9/followers"
          }
        },
        "name" : "barney",
        "picture" : "http://www.gravatar.com/avatar/00767101f6b4f2cf5d02ed510dbcf0b4",
        "test" : "fred",
        "username" : "barney"
      } ],
      "timestamp" : 1361903248398,
      "duration" : 24,
      "organization" : "myorg",
      "applicationName" : "sandbox"
    }

## Deleting a group

To delete a group, delete the associated group entity as you would any other entity. Note that this will only delete the group. Any entities in the group will be preserved.

For more information and code samples, see [Deleting Data Entities](../data-storage/entities.html#deleting-data-entities).

## Adding a user to a group

You can add users to groups from client code using cURL commands or one of the SDKs, as described here.

When setting up your application on the server, you might find it easier and more convenient to create and populate groups with the admin portal. There, you can create groups, create roles, and define permission rules that govern user access to data and services in your application. For more information, see [Security & Token Authentication](../security-and-auth/app-security.html).

Use the POST method to add a user to a group. If the named group does not yet exist, an error message is returned.

### Request syntax

    curl -X POST https://api.usergrid.com/<org_id>/<app_id>/groups/<uuid | groupname>/users/<uuid | username>

### Request URI

    POST /<org_id>/<app_id>/groups/<uuid | groupname>/users/<uuid | username>

Parameters

Parameter                   Description
---------                   -----------
arg uuid | string org_id	Organization UUID or organization name
arg uuid | string app_id	Application UUID or application name
arg uuid | string groupname	UUID or name of the group
arg uuid | string username	UUID or username of user

### Example

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Request

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups/mynewgroup/users/john.doe"

### Response

    {
        "action": "post",
        "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
        "params": {},
        "path": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users",
        "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/groups/a668717b-67cb-11e1-8223-12313d14bde7/users",
        "entities": [{
            "uuid": "6fbc8157-4786-11e1-b2bd-22000a1c4e22",
            "type": "user",
            "name": "John Doe",
            "created": 1327517852364015,
            "modified": 1327517852364015,
            "activated": true,
            "email": "john.doe@mail.com",
            "metadata": {
                "connecting": {
                    "owners":"/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/connecting/owners"
                },
                "path": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22",
                "sets": {
                    "rolenames": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/rolenames",
                    "permissions": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/permissions"
                },
                "collections":{
                    "activities":"/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/activities",
                    "devices": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/devices",
                    "feed":"/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/feed",
                    "groups": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/groups",
                    "roles":"/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/roles",
                    "following": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/following",
                    "followers": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/followers"
                }
            },
            "picture": "https://www.gravatar.com/avatar/90f823ba15655b8cc8e3b4d63377576f",
            "username": "john.doe"
        }],
        "timestamp": 1331066031380,
        "duration": 64,
        "organization" : "my-org",
        "applicationName": "my-app"
    }

## Removing a user from a group

Use the DELETE method to remove a user from the specified group.

### Request syntax

    curl -X DELETE https://api.usergrid.com/<org>/<app>/groups/<group>/users/<user>"

Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
group	    UUID or name of the group
user	    UUID, username or email of user to be deleted

### Example request

    curl -X DELETE https://api.usergrid.com/your-org/your-app/groups/someGroup/users/someUser

### Example response

    {
      "action" : "delete",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users",
      "uri" : "https://api.usergrid.com/your-org/your-app/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users",
      "entities" : [ {
        "uuid" : "74d2d7da-e694-11e3-b0c6-4d2664c8e0c3",
        "type" : "user",
        "name" : "someUser",
        "created" : 1401301104077,
        "modified" : 1401301104077,
        "username" : "someUser",
        "email" : "your-org@apigee.com",
        "activated" : true,
        "picture" : "http://www.gravatar.com/avatar/0455fc92de2636fc7a176cc5d298bb78",
        "metadata" : {
          "path" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3",
          "sets" : {
            "rolenames" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/roles",
            "permissions" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/permissions"
          },
          "collections" : {
            "activities" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/activities",
            "devices" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/devices",
            "feed" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/feed",
            "groups" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/groups",
            "roles" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/roles",
            "following" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/following",
            "followers" : "/groups/cd796d0a-b90c-11e3-83de-83ceb9965c26/users/74d2d7da-e694-11e3-b0c6-4d2664c8e0c3/followers"
          }
        }
      } ],
      "timestamp" : 1401751485776,
      "duration" : 220,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
