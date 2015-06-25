---
title: Role
category: docs
layout: docs
---

Role
====

[See all application
entities](/docs/usergrid/content/application-entities)[](/docs/usergrid/content/application-entities)

A role represents a set of permissions that enable certain operations to
be performed on a specific endpoint. You can assign a user to a role,
and in this way give the user the permissions associated with that role.
For further information about roles and their use, see [Managing access
by defining permission
rules](/managing-access-defining-permission-rules).

Using App services APIs you can create, retrieve, update, delete, and
query roles. See [Role properties](#role_prop) for a list of the
system-defined properties for roles. In addition, you can create role
properties specific to your application.

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Also, the /rolenames endpoint is no longer valid. If your code currently
makes calls to /rolenames, you need to change the calls to use /roles.

Creating a new role
-------------------

Use the POST method to create a new application role.

### Request URI

POST /{org\_id}/{app\_id}/roles {request body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| arg uuid|string org\_id              | Organization UUID or organization    |
|                                      | name                                 |
+--------------------------------------+--------------------------------------+
| arg uuid|string app\_id              | Application UUID or application name |
+--------------------------------------+--------------------------------------+
| request body                         | Role name and title:                 |
|                                      |                                      |
|                                      |     { "name" : "manager", "title" :  |
|                                      | "Manager" }                          |
+--------------------------------------+--------------------------------------+

### Example - Request

-   [cURL](#curl_create_role)
-   [JavaScript (HTML5)](#javascript_create_role)
-   [Ruby](#ruby_create_role)
-   [Node.js](#nodejs_create_role)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/roles/ -d '{"name":"manager","title":"Manager"}'

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    result = app.create_role name: 'manager', title: 'Manager'

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'roles',
        body:{ name:'manager', title:'Manager' }    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

### Example - Response

    {
      "action" : "post",
      "application" : "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
      "params" : {},
      "uri" : "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22",
      "entities" : [
         {
          "uuid": "98e772a3-327a-11e2-aef7-02e81adcf3d0",
          "type": "role",
          "name": "manager",
          "created": 1353351290724,
          "modified": 1353351290724,
          "inactivity": 0,
          "metadata":  {
            "path": "/roles/98e772a3-327a-11e2-aef7-02e81adcf3d0",
            "sets":  {
              "permissions": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/permissions"
            },
            "collections":  {
              "groups": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/groups",
              "users": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users"
            }
          },
          "roleName": "manager",
          "title": "Manager"
        }
      ],
      "timestamp": 1353358474917,
      "duration": 46,
      "duration": 45,
      "organization": "my-org",
      "applicationName": "my-app"
    }

Getting the roles in an application
-----------------------------------

Use the GET method to get the roles for a specific application.

### Request URI

GET /{org\_id}/{app\_id}/roles

### Parameters

  Parameter                 Description
  ------------------------- ----------------------------------------
  arg uuid|string org\_id   Organization UUID or organization name
  arg uuid|string app\_id   Application UUID or application name

### Example - Request

-   [cURL](#curl_get_roles)
-   [JavaScript (HTML5)](#javascript_get_roles)
-   [Ruby](#ruby_get_roles)
-   [Node.js](#nodejs_get_roles)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/roles"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    roles = app['roles'].collection

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'roles'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action" : "get",
      "application" : "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
      "params" : {
        "_" : [ "1328058070002" ]
      },
      "uri" : "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22",
      "entities" : [ 
    {
          "uuid": "70f39f36-1825-379d-8385-7a7fbe9ec74a",
          "type": "role",
          "name": "admin",
          "created": 1342198809382,
          "modified": 1350688633265,
          "inactivity": 0,
          "metadata": {
            "path": "/roles/70f39f36-1825-379d-8385-7a7fbe9ec74a",
            "sets": {
              "permissions": "/roles/70f39f36-1825-379d-8385-7a7fbe9ec74a/permissions"
            },
            "collections": {
              "groups": "/roles/70f39f36-1825-379d-8385-7a7fbe9ec74a/groups",
              "users": "/roles/70f39f36-1825-379d-8385-7a7fbe9ec74a/users"
            }
          },
          "roleName": "admin",
          "title": "Administrator"
        },
        {
          "uuid": "b8f8f336-30c9-3553-b447-6891f3e1e6bf",
          "type": "role",
          "name": "default",
          "created": 1342198809472,
          "modified": 1350688633282,
          "inactivity": 0,
          "metadata": {
            "path": "/roles/b8f8f336-30c9-3553-b447-6891f3e1e6bf",
            "sets": {
              "permissions": "/roles/b8f8f336-30c9-3553-b447-6891f3e1e6bf/permissions"
            },
            "collections": {
              "groups": "/roles/b8f8f336-30c9-3553-b447-6891f3e1e6bf/groups",
              "users": "/roles/b8f8f336-30c9-3553-b447-6891f3e1e6bf/users"
            }
          },
          "roleName": "default",
          "title": "Default"
        },
        {
          "uuid": "bd397ea1-a71c-3249-8a4c-62fd53c78ce7",
          "type": "role",
          "name": "guest",
          "created": 1342198809551,
          "modified": 1350688633299,
          "inactivity": 0,
          "metadata": {
            "path": "/roles/bd397ea1-a71c-3249-8a4c-62fd53c78ce7",
            "sets": {
              "permissions": "/roles/bd397ea1-a71c-3249-8a4c-62fd53c78ce7/permissions"
            },
            "collections": {
              "groups": "/roles/bd397ea1-a71c-3249-8a4c-62fd53c78ce7/groups",
              "users": "/roles/bd397ea1-a71c-3249-8a4c-62fd53c78ce7/users"
            }
          },
          "roleName": "guest",
          "title": "Guest"
        }
      ],
      "timestamp": 1353351684225,
      "duration": 35,
      "organization": "my-org",
      "applicationName": "my-app"
    }

Deleting a role
---------------

Use the DELETE method to delete the specified role and return the
revised set of application roles.

### Request URI

DELETE /{org\_id}/{app\_id}/roles/{rolename}

### Parameters

  Parameter                 Description
  ------------------------- ----------------------------------------
  arg uuid|string org\_id   Organization UUID or organization name
  arg uuid|string app\_id   Application UUID or application name
  arg string rolename       Role name

### Example - Request

-   [cURL](#curl_delete_role)
-   [JavaScript (HTML5)](#javascript_delete_role)
-   [Ruby](#ruby_delete_role)
-   [Node.js](#nodejs_delete_role)

<!-- -->

    curl -X DELETE "https://api.usergrid.com/my-org/my-app/roles/manager"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['roles/manager'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'roles'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action" : "delete",
      "application" : "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
      "params" : {
        "_" : [ "1328058070002" ]
      },
      "path": "/roles",
      "uri" : "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22",
      "entities": [
        {
          "uuid": "382d0991-74bb-3548-8166-6b07e44495ef",
          "type": "role",
          "name": "manager",
          "created": 1353358474924,
          "modified": 1353358474924,
          "inactivity": 0,
          "metadata": {
            "path": "/roles/382d0991-74bb-3548-8166-6b07e44495ef",
            "sets": {
              "permissions": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/permissions"
            },
            "collections": {
              "groups": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/groups",
              "users": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users"
            }
          },
          "roleName": "manager",
          "title": "Manager"
        }
      ],
      "timestamp": 1353359131580,
      "duration": 218,
      "organization": "my-org",
      "applicationName": "my-app"
    }

Getting permissions for an application role
-------------------------------------------

Use the GET method to get permissions for the specific app role.

### Request URI

GET /{org\_id}/{app\_id}/roles/{rolename|role\_id}/permissions

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg  uuid|string rolename   Role UUID or role name

### Example - Request

-   [cURL](#curl_get_role_permissions)
-   [JavaScript (HTML5)](#javascript_get_role_permissions)
-   [Ruby](#ruby_get_role_permissions)
-   [Node.js](#nodejs_get_role_permissions)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/roles/manager/permissions"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    response = app['roles']['guest']['permissions'].entity

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'roles/manager/permissions'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action" : "get",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "uri" : "http://api.usergrid.com/your-org/your-app",
      "entities" : [ ],
      "data" : [ "get,post,put,delete:/**", "post:/devices", "post:/users", "put:/devices/*" ],
      "timestamp" : 1376589421592,
      "duration" : 89,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }

Adding permissions to an application role
-----------------------------------------

Use the POST method to add permissions for the specified application
role.

### Request URI

POST /{org\_id}/{app\_id}/roles/{rolename|role\_id}/permissions {request
body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| arg uuid|string org\_id              | Organization UUID or organization    |
|                                      | name                                 |
+--------------------------------------+--------------------------------------+
| arg uuid|string app\_id              | Application UUID or application name |
+--------------------------------------+--------------------------------------+
| arg uuid|string rolename             | Role UUID or role name               |
+--------------------------------------+--------------------------------------+
| request body                         | JSON object with a single            |
|                                      | permissions property. The value of   |
|                                      | that property is a string containing |
|                                      | a permissions specification. The     |
|                                      | spec is a comma-separated list of    |
|                                      | verbs, followed by a colon, followed |
|                                      | by a URL pattern:                    |
|                                      |                                      |
|                                      |     { "permission" : "get,put,post,d |
|                                      | elete:/users/me/groups" }            |
+--------------------------------------+--------------------------------------+

### Example - Request

-   [cURL](#curl_add_role_permissions)
-   [JavaScript (HTML5)](#javascript_add_role_permissions)
-   [Ruby](#ruby_add_role_permissions)
-   [Node.js](#nodejs_add_role_permissions)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/roles/manager/permissions" -d '{"permission":"get,put,post,delete:/users/me/groups" }'

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['roles/manager/permissions'].post permission:'get,put,post,delete:/users/me/groups'

The example assumes use of the
[Node.js](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'roles/manager/permissions',
        body:{ permission:'get,put,post,delete:/users/me/groups' }    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

### Example - Response

    {
      "action" : "post",
      "application" : "f34f4222-a166-11e2-a7f7-02e815df8rd0",
      "params" : { },
      "uri" : "http://api.usergrid.com/your-org/your-app",
      "entities" : [ ],
      "data" : [ "get,put,post,delete:/users/me/groups" ],
      "timestamp" : 1376590904061,
      "duration" : 21,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }

Deleting permissions from an application role
---------------------------------------------

Use the DELETE method to remove permissions from the specified
application role.

### Request URI

DELETE
/{org\_id}/{app\_id}/roles/{rolename|role\_id}/permissions?permission={permissions\_spec}

### Parameters

  Parameter              Description
  ---------------------- ----------------------------------------------------
  uuid|string org\_id    Organization UUID or organization name
  uuid|string app\_id    Application UUID or application name
  uuid|string rolename   Role UUID or role name
  grant\_url\_pattern    The pattern on which the permissions were granted.

### Example - Request

-   [cURL](#curl_delete_role_permissions)
-   [JavaScript (HTML5)](#javascript_delete_role_permissions)
-   [Ruby](#ruby_delete_role_permissions)
-   [Node.js](#nodejs_delete_role_permissions)

<!-- -->

    curl -X DELETE -H "authorization: Bearer INSERT_BEARER_TOKEN_HERE" "https://api.usergrid.com/my-org/my-app/roles/default/permissions?permission=get,post,delete:/notes"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['roles/manager/permission'].delete permission:'delete'

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'roles/manager/permission',
        body:{ permission:'delete' }    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

### Example - Response

    {
      "action" : "delete",
      "application" : "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
      "params" : {
        "permission":  [
           "delete"
        ]
      },
      "path": "/roles",
      "uri" : "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22",
      "entities":  [
          {
          "uuid": "382d0991-74bb-3548-8166-6b07e44495ef",
          "type": "role",
          "name": "manager",
          "created": 1353359536973,
          "modified": 1353359536973,
          "inactivity": 0,
          "metadata":  {
            "path": "/roles/382d0991-74bb-3548-8166-6b07e44495ef",
            "sets":  {
              "permissions": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/permissions"
            },
            "collections":  {
              "groups": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/groups",
              "users": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users"
            }
          },
          "roleName": "manager",
                "title": "Manager"
              }
       ],
       "timestamp": 1353360762403,
       "duration": 181,
       "organization": "my-org",
       "applicationName": "my-app"
    }

Adding a user to a role
-----------------------

Use the POST method to add a user to a role.

### Request URI

    POST /{org_id}/{app_id}/roles/{role_id}/users/{uuid|username}

or

    POST /{org_id}/{app_id}/users/{uuid|username}/roles/{role_id}

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string role\_id    Role UUID or role name
  arg uuid|string uuid|name   User UUID or username

### Example - Request

-   [cURL](#curl_add_role_user)
-   [JavaScript (HTML5)](#javascript_add_role_user)
-   [Ruby](#ruby_add_role_user)
-   [Node.js](#nodejs_add_role_user)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/roles/manager/users/john.doe"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['roles/manager/users/someuser'].post nil

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'roles/manager/users/john.doe'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action": "post",
      "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params": {},
      "path": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users",
      "uri": "https://api.usergrid.com/my-org/my-app/roles/382d0991-74bb-3548-8166-6b07e44495ef/users",
      "entities": [
        {
          "uuid": "34e26bc9-2d00-11e2-a065-02e81ae640dc",
          "type": "user",
          "name": "John Doe",
          "created": 1352748968504,
          "modified": 1352748968504,
          "activated": true,
          "email": "john.doe@mail.com",
          "metadata": {
            "path": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc",
            "sets": {
              "rolenames": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/rolenames",
              "permissions": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/permissions"
            },
            "connections": {
              "likes": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/likes"
            },
            "collections": {
              "activities": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/activities",
              "devices": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/devices",
              "feed": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/feed",
              "groups": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/groups",
              "roles": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/roles",
              "following": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/following",
              "followers": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/followers"
            }
          },
          "picture": "http://www.gravatar.com/avatar/90f82dba15655b8cc8e3b4d63377576f",
          "username": "someuser"
        }
      ],
      "timestamp": 1352999087337,
      "duration": 65,
      "organization": "my-org",
      "applicationName": "my-app"
    }

Getting the users in a role
---------------------------

Use the GET method to get the users in a role.

### Request URI

    GET /{org_id}/{app_id}/roles/{role_id}/users

### Parameters

  Parameter                  Description
  -------------------------- ----------------------------------------
  arg uuid|string org\_id    Organization UUID or organization name
  arg uuid|string app\_id    Application UUID or application name
  arg uuid|string role\_id   Role UUID or role name

### Example - Request

-   [cURL](#curl_get_role_users)
-   [JavaScript (HTML5)](#javascript_get_role_users)
-   [Ruby](#ruby_get_role_users)
-   [Node.js](#nodejs_get_role_users)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/roles/manager/users"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    users = app['roles/manager/users'].collection

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'roles/manager/users'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action": "get",
      "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params": {
        "_": [
          "1352999121468"
        ]
      },
      "path": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users",
      "uri": "https://api.usergrid.com/my-org/my-app/roles/382d0991-74bb-3548-8166-6b07e44495ef/users",
      "entities": [
        {
          "uuid": "34e26bc9-2d00-11e2-a065-02e81ae640dc",
          "type": "user",
          "name": "John Doe",
          "created": 1352748968504,
          "modified": 1352748968504,
          "activated": true,
          "email": "john.doe@mail.com",
          "metadata": {
            "path": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc",
            "sets": {
              "rolenames": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/rolenames",
              "permissions": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/permissions"
            },
            "connections": {
              "likes": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/likes"
            },
            "collections": {
              "activities": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/activities",
              "devices": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/devices",
              "feed": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/feed",
              "groups": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/groups",
              "roles": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/roles",
              "following": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/following",
              "followers": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/followers"
            }
          },
          "picture": "http://www.gravatar.com/avatar/90f823ba15655b8cc8e3b4d63377576f",
          "username": "john.doe"
        }
      ],
      "timestamp": 1352999119136,
      "duration": 28,
      "organization": "my-org",
      "applicationName": "my-app"

Deleting a user from a role
---------------------------

Use the DELETE method to delete a user from a role.

### Request URI

    DELETE /{org_id}/{app_id}/roles/{role_id}/users/{uuid|username}

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string role\_id    Role UUID or role name
  arg uuid|string uuid|name   User UUID or username

### Example - Request

-   [cURL](#curl_delete_role_user)
-   [JavaScript (HTML5)](#javascript_delete_role_user)
-   [Ruby](#ruby_delete_role_user)
-   [Node.js](#nodejs_delete_role_user)

<!-- -->

    curl -X DELETE "https://api.usergrid.com/my-org/my-app/roles/manager/users/john.doe"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['roles/manager/users/john.doe'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'roles/manager/users/john.doe'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
       "action": "delete",
       "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
       "params": {
         "_": [
           "1353000719265"
         ]
       },
       "path": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users",
       "uri": "equest
    curl -X DELETE "https://api.usergrid.com/my-org/my-app/roles/382d0991-74bb-3548-8166-6b07e44495ef/users",
       "entities": [
         {
           "uuid": "34e26bc9-2d00-11e2-a065-02e81ae640dc",
           "type": "user",
           "name": "John Doe",
           "created": 1352748968504,
           "modified": 1352748968504,
           "activated": true,
           "email": "john.doe@mail.com",
           "metadata": {
             "path": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc",
             "sets": {
               "rolenames": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/rolenames",
               "permissions": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/permissions"
             },
             "connections": {
               "likes": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/likes"
             },
             "collections": {
               "activities": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/activities",
               "devices": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/devices",
               "feed": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/feed",
               "groups": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/groups",
               "roles": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/roles",
               "following": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/following",
               "followers": "/roles/382d0991-74bb-3548-8166-6b07e44495ef/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/followers"
             }
           },
           "picture": "http://www.gravatar.com/avatar/90f823ba13655b8cc8e3b4d63377576f",
           "username": "john.doe"
         }
       ],
       "timestamp": 1353000717531,
       "duration": 338,
       "organization": "my-org",
       "applicationName": "my-app"
     }

Role properties
---------------

The following are the system-defined properties for role entities. You
can create application-specific properties for a role entity in addition
to the system-defined properties. The system-defined properties are
reserved. You cannot use these names to create other properties for a
role entity. In addition the roles name is reserved for the roles
collection — you can't use it to name another collection.

The look-up property for a role is name that is, you can use the name
property to reference a role in an API call. However, you can search on
a role using any property of the role entity. See\

Nonexistent node nid: 9321.

for details on searching.

### General properties

Roles have the following general properties.

  Property   Type     Description
  ---------- -------- ---------------------------------------------------------------------------------
  uuid       UUID     Role’s unique entity ID
  type       string   "role"
  created    long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified   long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  name       string   Unique name identifying the role (mandatory)
  roleName   string   Informal role name
  title      string   Role title

### Set properties

Roles have the following set property.

  Set           Type     Description
  ------------- -------- -------------------------
  permissions   string   Set of user permissions

### Collection properties

Roles have the following collections.

  Collection   Type    Description
  ------------ ------- -----------------------------------------
  users        user    Collection of users assigned to a role
  groups       group   Collection of groups assigned to a role

 
