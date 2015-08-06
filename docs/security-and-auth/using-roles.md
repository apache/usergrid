# Using roles
Roles are named sets of one or more permissions, and are useful for defining specific access levels to resources in your API BaaS data store. Multiple roles can be assigned to a user or group, giving you a great deal of flexibility in how access to resources are defined.

For example, in a blogging app you might create a 'reviewer' role that allows GET and PUT access to an articles collection to allow the user to retrieve and update articles, but not allow them to create new articles.

## Default roles
While you can create as many custom roles as you want per application, all API BaaS applications include three default roles. These roles each serve a special purpose and should not be deleted; however, you can and should adjust the permissions assigned to these roles to suit the needs of you app.

The following table describes each pre-defined role, and the permissions that are assigned to them by default.

<table class="usergrid-table">
<tr>
  <th>Role</th>
  <th class="usergrid-30"> Permissions</th>
  <th>Description</th>
</tr>
<tr>
  <td>Guest</td>
  <td>
  
* post: /devices
* post: /users
* put: /devices/*
  
  </td>
  <td>
  
  Assigned to all unauthenticated requests. Includes a basic set of permissions that are commonly needed by unregistered or unauthenticated users. 
  <p>Grants permission for a user to create a user account and for their device to be registered.
      
  </td>
</tr>
<tr>
  <td>Default</td>
  <td>
  
* get, post, put, delete: /**

  </td>
  <td>
  
 Default for authenticated users. Assigns the associated permissions to all users whose requests are authenticated with a valid access token.

 <div class="admonition warning"> <p class="first admonition-title">WARNING</p> <p class="last"> 
 __Note__: By default, __grants full access for all resources in your application__. A first task in securing your application should be to restrict access by redefining this role to narrow the access it provides. Remove the default full permission rule and add restrictive permission rules for a production deployment. 
 </p></div>
  
  </td>
</tr>
<tr>
  <td>Administrator</td>
  <td></td>
  <td></td>
</tr>
<tr>
  <td></td>
  <td></td>
  <td></td>
</tr>
<tr>
  <td></td>
  <td></td>
  <td></td>
</tr>
</table>

Role	Permissions	Description
Guest	
post: /devices
post: /users
put: /devices/*
Assigned to all unauthenticated requests. Includes a basic set of permissions that are commonly needed by unregistered or unauthenticated users.

Grants permission for a user to create a user account and for their device to be registered.

Default	
get, post, put, delete: /**
Default for authenticated users. Assigns the associated permissions to all users whose requests are authenticated with a valid access token.

By default, grants full access for all resources in your application. A first task in securing your application should be to restrict access by redefining this role to narrow the access it provides. Remove the default full permission rule and add restrictive permission rules for a production deployment.
Administrator	
None

Unused until you associate it with users or groups. By default, includes no permissions that provide access.

Grants no access. Consider this a blank slate. Add permission rules and associate this role with users and groups as needed.

Note: The Administrator role is not the same as an organization administrator -- that is, someone who authenticates as an Admin User. The Admin User is an implicit user created when you create an organization. After authenticating, the Admin User has full access to all of the administration features of the API BaaS API. By comparison, the Administrator role is simply a role (initially without permissions) that can be assigned to any user.

## Creating roles
Generally, it is easiest to a create a role for each access type you want to enable in your app. You may, however, assign multiple roles to any user or group entity, so you have the flexibility to define any schema for applying roles that you like.

The following shows how to create a new role and assign permissions to it.

### Request syntax
With cURL requests a role entity is created with a POST request, then permissions must be assigned to it with a separate request. For more on assigning permissions with cURL, see Using permissions.

The following details how to create a new role entity.

    curl -X POST https://api.usergrid.com/<org>/<app>/roles -d '{"name":<roleName>}'
    
Parameters

Parameter Description
--------- -----------
org	      Organization UUID or organization name
app	      Application UUID or application name
roleName  The name of the role to be created

### Example request

    curl -X POST "https://api.usergrid.com/my-org/my-app/roles/ -d '{"name":"manager"}'

### Example response

    {
      "action" : "post",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/roles",
      "uri" : "https://api.usergrid.com/your-org/your-app/roles",
      "entities" : [ {
        "uuid" : "382d0991-74bb-3548-8166-6b07e44495ef",
        "type" : "role",
        "name" : "manager",
        "created" : 1402612783104,
        "modified" : 1402612783104,
        "roleName" : "manager",
        "title" : "manager",
        "inactivity" : 0,
        "metadata" : {
          "path" : "/roles/382d0991-74bb-3548-8166-6b07e44495ef",
          "sets" : {
            "permissions" : "/roles/382d0991-74bb-3548-8166-6b07e44495ef/permissions"
          },
          "collections" : {
            "groups" : "/roles/382d0991-74bb-3548-8166-6b07e44495ef/groups",
            "users" : "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users"
          }
        }
      } ],
      "timestamp" : 1402612783102,
      "duration" : 30,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }		
		
## Assigning roles
Once you have created some roles, you will need to explicitly assign them to a user or group entity. The permissions associated with that role will be granted to the entity immediately for any requests they send that are authenticated by a valid access token. Please note that assigning a role to a group will grant the associated permissions to every user in that group.

The following shows how to assign a role to an entity.

### Request syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/roles/<roleName>/<entityType>/<entityID>

Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
roleName	The name of the role to be created
entityType	The type of the entity the role is being assigned to. 'Group' and 'user' are valid values.
entityID	The UUID of the entity the role is being assigned to. 

For groups, the 'name' property can be used. For users, the 'username' property can be used.

### Example request

    curl -X POST "https://api.usergrid.com/my-org/my-app/roles/manager/users/someUser
    

### Example response
{
  "action" : "post",
  "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
  "params" : { },
  "path" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users",
  "uri" : "https://api.usergrid.com/your-org/your-app/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users",
  "entities" : [ {
    "uuid" : "410b213a-b379-11e3-a0e5-9953085ea376",
    "type" : "user",
    "name" : "someUser",
    "created" : 1395681911491,
    "modified" : 1399070010291,
    "username" : "someUser",
    "activated" : true,
    "file" : "fobnszewobnioerabnoiawegbrn\n",    
    "metadata" : {
      "connecting" : {
        "friends" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/connecting/friends",
        "likes" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/connecting/likes"
      },
      "path" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376",
      "sets" : {
        "rolenames" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/roles",
        "permissions" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/permissions"
      },
      "connections" : {
        "completed" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/completed",
        "follows" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/follows"
      },
      "collections" : {
        "activities" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/activities",
        "devices" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/devices",
        "feed" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/feed",
        "groups" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/groups",
        "roles" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/roles",
        "following" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/following",
        "followers" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/followers"
      }
    }
  } ],
  "timestamp" : 1402965083889,
  "duration" : 41,
  "organization" : "your-org",
  "applicationName" : "your-app"
}		
		
## Removing roles
At times it may be necessary to remove a role from a user or group entity, for example if a user changes jobs, or the duties of a group are altered. Please note that removing a role from a group will remove the associated permissions from every user in that group.

The following shows how to remove a role from an entity.

### Request syntax

    curl -X DELETE https://api.usergrid.com/<org>/<app>/roles/<roleName>/<entityType>/<entityID>
    
Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
roleName	The name of the role to be created
entityType	The type of the entity the role is being removed from. 'Group' and 'user' are valid values.
entityID	The UUID of the entity the role is being removed from. 

For groups, the 'name' property can be used. For users, the 'username' property can be used.

### Example request

    curl -X DELETE https://api.usergrid.com/my-org/my-app/roles/manager/users/someUser
    

### Example response

    {
      "action" : "delete",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users",
      "uri" : "https://api.usergrid.com/your-org/your-app/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users",
      "entities" : [ {
        "uuid" : "410b213a-b379-11e3-a0e5-9953085ea376",
        "type" : "user",
        "name" : "someUser",
        "created" : 1395681911491,
        "modified" : 1399070010291,
        "username" : "someUser",
        "activated" : true,
        "metadata" : {
          "connecting" : {
            "friends" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/connecting/friends",
            "likes" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/connecting/likes"
          },
          "path" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376",
          "sets" : {
            "rolenames" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/roles",
            "permissions" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/permissions"
          },
          "connections" : {
            "completed" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/completed",
            "follows" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/follows"
          },
          "collections" : {
            "activities" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/activities",
            "devices" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/devices",
            "feed" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/feed",
            "groups" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/groups",
            "roles" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/roles",
            "following" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/following",
            "followers" : "/roles/348388de-a5c5-3c1e-9de5-9efc8ad529d8/users/410b213a-b379-11e3-a0e5-9953085ea376/followers"
          }
        }
      } ],
      "timestamp" : 1403214283808,
      "duration" : 358,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }	
    