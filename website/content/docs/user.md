---
title: User
category: docs
layout: docs
---

User
====

[See all application
entities](/docs/usergrid/content/application-entities)[](/docs/usergrid/content/application-entities)

 

A user entity represents an application user. Using App services APIs
you can create, retrieve, update, delete, and query user entities. See
[User entity properties](#user_properties) for a list of the
system-defined  properties for user entities. In addition, you can
create user properties specific to your application.

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Creating a new user
-------------------

Use the POST method to create a new user in the users collection.

### Request URI

POST /{org\_id}/{app\_id}/users {request body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| arg uuid|string org\_id              | Organization UUID or organization    |
|                                      | name                                 |
+--------------------------------------+--------------------------------------+
| arg uuid|string app\_id              | Application UUID or application name |
+--------------------------------------+--------------------------------------+
| request body                         | One or more sets of user properties, |
|                                      | of which username is mandatory and   |
|                                      | must be unique:                      |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "username" : "john.doe",       |
|                                      |       "email" : "john.doe@gmail.com" |
|                                      | ,                                    |
|                                      |       "name" : "John Doe",           |
|                                      |       "password" : "test1234"        |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

Although the password parameter is not mandatory, if you don't specify
it, the user will not be able to log in using username and password
credentials. If a password is not specified for the user, and you're an
Admin, you can set a password for the user (see [Setting a
password](#set_password)).

**Note:** The username can contain any combination of characters,
including those that represent letters, numbers, and symbols.

### Example - Request

-   [cURL](#curl_create_user)
-   [JavaScript (HTML5)](#javascript_create_user)
-   [Ruby](#ruby_create_user)
-   [Node.js](#nodejs_create_user)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/users" -d '{"username":"john.doe","email":"john.doe@gmail.com","name":"John Doe"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'users',
        body:{ username:'john.doe', name:'John Doe', email:'john.doe@gmail.com' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    result = app.create_user username: 'john.doe', name: 'John Doe', email: 'john.doe@gmail.com'
    john_doe = result.entity

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'users',
        body:{ username:'john.doe', name:'John Doe', email:'john.doe@gmail.com' }
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
      "application" : "4353136f-e978-11e0-8264-005056c00008",
      "params" : {
      },
      "path" : "/users",
      "uri" : "https://api.usergrid.com/005056c00008-4353136f-e978-11e0-8264/4353136f-e978-11e0-8264-005056c00008/users",

      "entities" : [ {
        "uuid" : "7d1aa429-e978-11e0-8264-005056c00008",
        "type" : "user",
        "created" : 1317176452536016,
        "modified" : 1317176452536016,
        "activated" : true,
        "email" : "john.doe@gmail.com",
        "metadata" : {
          "path" : "/users/7d1aa429-e978-11e0-8264-005056c00008",
          "sets" : {
            "rolenames" : "/users/7d1aa429-e978-11e0-8264-005056c00008/rolenames",
            "permissions" : "/users/7d1aa429-e978-11e0-8264-005056c00008/permissions"
          },
          "collections" : {
            "activities" : "/users/7d1aa429-e978-11e0-8264-005056c00008/activities",
            "devices" : "/users/7d1aa429-e978-11e0-8264-005056c00008/devices",
            "feed" : "/users/7d1aa429-e978-11e0-8264-005056c00008/feed",
            "groups" : "/users/7d1aa429-e978-11e0-8264-005056c00008/groups",
            "roles" : "/users/7d1aa429-e978-11e0-8264-005056c00008/roles",
            "following" : "/users/7d1aa429-e978-11e0-8264-005056c00008/following",
            "followers" : "/users/7d1aa429-e978-11e0-8264-005056c00008/followers"
          }
        },
        "name" : "John Doe",
        "username" : "john.doe"
      } ],
      "timestamp" : 1317176452528,
      "duration" : 52,
      "organization" : "my-org",
      "applicationName": "my-app"
    }

Setting/updating a user password
--------------------------------

If you're an Admin, use the POST method to initially set a user's
password or reset the user's existing password. If you're a user, use
the POST method to reset your password

To protect password visibility, passwords are stored in an Apache Usergrid
credentials vault. Passwords must be at least 5 characters long and can
include the characters a-z, 0-9, @, \#, \$, %, \^, and &.

### Request URI

POST /{org\_id}/{app\_id}/users/{user}/password {request body}

### Parameters

Parameter

Description

arg uuid|string org\_id

Organization UUID or organization name

arg uuid|string app\_id

Application UUID or application name

arg string user

User email or username

request body

New and old passwords:

    {"newpassword":"foo9876a","oldpassword":"bar1234b"}

**Note:** If you are accessing an endpoint with application-level access
(rather than application user-level access or anonymous access), it is
not necessary to provide an old password. See [Authenticating users and
application clients](/authenticating-users-and-application-clients) for
further information about access levels.

### Example - Request

-   [cURL](#curl_set_password)
-   [JavaScript (HTML5)](#javascript_set_password)
-   [Ruby](#ruby_set_password)
-   [Node.js](#nodejs_set_password)

<!-- -->

    curl -X PUT "https://api.usergrid.com/my-org/my-app/users/john.doe/password" -d '{"newpassword":"foo9876a","oldpassword":"bar1234b"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'PUT',
        endpoint:'users/john.doe',
        body:{ newpassword:'foo9876a' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — PUT failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    john_doe = app['users/john.doe'].entity
    john_doe.newpassword = 'foo9876a'
    john_doe.save

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'PUT',
        endpoint:'users/john.doe',
        body:{ newpassword:'foo9876a' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — PUT failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action": "set user password",
      "timestamp": 1355185897894,
      "duration": 47
    }

Getting a user
--------------

Use the GET method to retrieve a user given a specified UUID, username,
or email address.

### Request URI

GET /{org\_id}/{app\_id}/users/{uuid|username|email\_address}

### Parameters

  Parameter                                      Description
  ---------------------------------------------- -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  arg uuid|string org\_id                        Organization UUID or organization name
  arg uuid|string app\_id                        Application UUID or application name
  arg uuid|string uuid|username|email\_address   User UUID, username, or email address. The alias /users/me can be used in place of the current user’s uuid, username, or email address. **Note:** The /users/me endpoint is accessible only if you provide an access token with the request (see [Authenticating users and application clients](/authenticating-users-and-application-clients)). If you make an anonymous ("guest") call, the system will not be able to determine which user to return as /users/me.

### Example - Request

-   [cURL](#curl_get_user)
-   [JavaScript (HTML5)](#javascript_get_user)
-   [Ruby](#ruby_get_user)
-   [Node.js](#nodejs_get_user)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/users/jane.doe"

OR

    curl -X GET "https://api.usergrid.com/my-org/my-app/users/a407b1e7-58e8-11e1-ac46-22000a1c5a67e"

OR

    curl -X GET "https://api.usergrid.com/my-org/my-app/users/jane.doe@gmail.com"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'users/jane.doe'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    user = app['users/jane.doe'].entity

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'users/jane.doe'
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
      "application" : "1c8f60e4-da67-11e0-b93d-12313f0204bb8",
      "params" : {
        "_": [
        "1315524419746"
        ]
      },
      "path" : "https://api.usergrid.com/12313f0204bb-1c8f60e4-da67-11e0-b93d/1c8f60e4-da67-11e0-b93d-12313f0204bb/users",
      "uri" : "https://api.usergrid.com/005056c00008-4353136f-e978-11e0-8264/4353136f-e978-11e0-8264-005056c00008/users",

      "entities" : [ {
        "uuid" : "78c54a82-da71-11e0-b93d-12313f0204b",
        "type" : "user",
        "created" : 1315524171347008,
        "modified" : 1315524171347008,
        "activated" : true,
        "email" : "jane.doe@gmail.com",
        "metadata" : {
          "path" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
          "sets" : {
            "rolenames" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
            "permissions" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
          },
          "collections" : {
            "activities" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
            "devices" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/devices",
            "feed" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
            "groups" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
            "roles" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
            "following" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
            "followers" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
          }
        },
        "username" : "jane.doe"
      } ],
      "timestamp" : 1315524421071,
      "duration" : 107,
      "organization" : "my-org",
      "applicationName": "my-app"
    }

### Example - Request: Get multiple users

You can retrieve multiple users by specifying their UUIDs in the
resource path. You cannot specify usernames. Use a semicolon (not a
slash) to separate the users collection name from the UUIDs and a
semicolon to separate each UUID.

-   [cURL](#curl_get_multuser)
-   [JavaScript (HTML5)](#javascript_get_multuser)
-   [Ruby](#ruby_get_multuser)
-   [Node.js](#nodejs_get_multuser)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/users;7d1aa429-e978-11e0-8264-005056c00008;a407b1e7-58e8-11e1-ac46-22000a1c5a67e"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'users;7d1aa429-e978-11e0-8264-005056c00008;a407b1e7-58e8-11e1-ac46-22000a1c5a67e'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    users = app['users;7d1aa429-e978-11e0-8264-005056c00008;a407b1e7-58e8-11e1-ac46-22000a1c5a67e'].entity

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'users;7d1aa429-e978-11e0-8264-005056c00008;a407b1e7-58e8-11e1-ac46-22000a1c5a67e'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response: Get multiple users

    {
      "action" : "get",
      "application" : "1c8f60e4-da67-11e0-b93d-12313f0204bb8",
      "params" : { },
      "path" : "/users",
      "uri" : "https://api.usergrid.com/005056c00008-4353136f-e978-11e0-8264/4353136f-e978-11e0-8264-005056c00008/users",
     "entities" : [ {
        "uuid" : "7d1aa429-e978-11e0-8264-005056c00008",
        "type" : "user",
        "created" : 1317176452536016,
        "modified" : 1317176452536016,
        "activated" : true,
        "email" : "john.doe@gmail.com",
        "metadata" : {
          "path" : "/users/7d1aa429-e978-11e0-8264-005056c00008",
          "sets" : {
            "rolenames" : "/users/7d1aa429-e978-11e0-8264-005056c00008/rolenames",
            "permissions" : "/users/7d1aa429-e978-11e0-8264-005056c00008/permissions"
          },
          "collections" : {
            "activities" : "/users/7d1aa429-e978-11e0-8264-005056c00008/activities",
            "devices" : "/users/7d1aa429-e978-11e0-8264-005056c00008/devices",
            "feed" : "/users/7d1aa429-e978-11e0-8264-005056c00008/feed",
            "groups" : "/users/7d1aa429-e978-11e0-8264-005056c00008/groups",
            "roles" : "/users/7d1aa429-e978-11e0-8264-005056c00008/roles",
            "following" : "/users/7d1aa429-e978-11e0-8264-005056c00008/following",
            "followers" : "/users/7d1aa429-e978-11e0-8264-005056c00008/followers"
          }
        },
        "name" : "John Doe",
        "username" : "john.doe"
      }, {
         "uuid" : "78c54a82-da71-11e0-b93d-12313f0204b",
        "type" : "user",
        "created" : 1315524171347008,
        "modified" : 1315524171347008,
        "activated" : true,
        "email" : "jane.doe@gmail.com",
        "metadata" : {
          "path" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
          "sets" : {
            "rolenames" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
            "permissions" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
          },
          "collections" : {
            "activities" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
            "devices" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/devices",
            "feed" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
            "groups" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
            "roles" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
            "following" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
            "followers" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
          }
        },
        "username" : "jane.doe"
      } ],
      "timestamp" : 1360253587934,
      "duration" : 35,
      "organization" : "my-org",
      "applicationName" : "my-app"
    }

Updating a user
---------------

Use the PUT method to update a user with new or changed property values.

### Request URI

PUT /{org\_id}/{app\_id}/users/{uuid|username} {request body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| arg uuid|string org\_id              | Organization UUID or                 |
|                                      | organization name                    |
+--------------------------------------+--------------------------------------+
| arg uuid|string app\_id              | Application UUID or application name |
+--------------------------------------+--------------------------------------+
| arg uuid|string uuid|username        | User UUID or username. The alias     |
|                                      | /users/me can be used in place of    |
|                                      | the current user’s uuid or username. |
|                                      | **Note:** The /users/me endpoint is  |
|                                      | accessible only if you provide an    |
|                                      | access token with the request (see   |
|                                      | [Authenticating users and            |
|                                      | application                          |
|                                      | clients](/authenticating-users-and-a |
|                                      | pplication-clients)).                |
|                                      | If you make an anonymous ("guest")   |
|                                      | call, the system will not be able to |
|                                      | determine which user to return as    |
|                                      | /users/me.                           |
+--------------------------------------+--------------------------------------+
| request body                         | One or more sets of user properties: |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "email" : "john.doe@mail.com", |
|                                      |       "city" : "san francisco"       |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

### Example - Request

-   [cURL](#curl_update_user)
-   [JavaScript (HTML5)](#javascript_update_user)
-   [Ruby](#ruby_update_user)
-   [Node.js](#nodejs_update_user)

<!-- -->

    curl -X PUT "https://api.usergrid.com/my-org/my-app/users/john.doe" -d '{"email":"john.doe@mail.com", "city":"san francisco"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'PUT',
        endpoint:'users/john.doe',
        body:{ email:'john.doe@mail.com', city:'san francisco' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — PUT failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    john_doe = app['users/john.doe'].entity
    john_doe.email = 'john.doe@mail.com'
    john_doe.city = 'san francisco'
    john_doe.save

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'PUT',
        endpoint:'users/john.doe',
        body:{ email:'john.doe@mail.com', city:'san francisco' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — PUT failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

### Example - Response

    {
      "action" : "put",
      "application" : "1c8f60e4-da67-11e0-b93d-12313f0204bb8",
      "params" : {
      },
      "path" : "/users",
      "uri" : "https://api.usergrid.com/005056c00008-4353136f-e978-11e0-8264/4353136f-e978-11e0-8264-005056c00008/users",

      "entities" : [ {
        "uuid" : "7d1aa429-e978-11e0-8264-005056c00008",
        "type" : "user",
        "created" : 1317176452536016,
        "modified" : 1317176452536016,
        "activated" : true,
        "city" : "san francisco",
        "email" : "john.doe@mail.com",
        "metadata" : {
          "path" : "/users/7d1aa429-e978-11e0-8264-005056c00008",
          "sets" : {
            "rolenames" : "/users/7d1aa429-e978-11e0-8264-005056c00008/rolenames",
            "permissions" : "/users/7d1aa429-e978-11e0-8264-005056c00008/permissions"
          },
          "collections" : {
            "activities" : "/users/7d1aa429-e978-11e0-8264-005056c00008/activities",
            "devices" : "/users/7d1aa429-e978-11e0-8264-005056c00008/devices",
            "feed" : "/users/7d1aa429-e978-11e0-8264-005056c00008/feed",
            "groups" : "/users/7d1aa429-e978-11e0-8264-005056c00008/groups",
            "roles" : "/users/7d1aa429-e978-11e0-8264-005056c00008/roles",
            "following" : "/users/7d1aa429-e978-11e0-8264-005056c00008/following",
            "followers" : "/users/7d1aa429-e978-11e0-8264-005056c00008/followers"
          }
        },
        "name" : "John Doe",
        "username" : "john.doe"
      } ],
      "timestamp" : 1317176452528,
      "duration" : 52,
      "organization" : "my-org",
      "applicationName": "my-app"
    }

Deleting a user
---------------

Use the DELETE method to delete the specified user given the UUID or
username.

Returns the contents of the deleted user.

### Request URI

DELETE /{org\_id}/{app\_id}/users/{uuid|username}

### Parameters

  Parameter                       Description
  ------------------------------- ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  arg uuid|string org\_id         Organization UUID or organization name
  arg uuid|string app\_id         Application UUID or application name
  arg uuid|string uuid|username   User UUID or username. The alias /users/me can be used in place of the current user’s uuid or username. **Note:** The /users/me endpoint is accessible only if you provide an access token with the request (see [Authenticating users and application clients](/authenticating-users-and-application-clients)). If you make an anonymous ("guest") call, the system will not be able to determine which user to return as /users/me.
  request body                    One or more sets of user properties

### Example - Request

-   [cURL](#curl_delete_user)
-   [JavaScript (HTML5)](#javascript_delete_user)
-   [Ruby](#ruby_delete_user)
-   [Node.js](#nodejs_delete_user)

<!-- -->

    curl -X DELETE "https://api.usergrid.com/my-org/my-app/users/john.doe"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'DELETE',
        endpoint:'users/john.doe'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    john_doe = app['users/john.doe'].entity
    john_doe.delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'users/john.doe'
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
      "application": "438a1ca1-cf9b-11e0-bcc1-12313f0204bb",
      "params": {
        "_": [
        "1329432186755" ]
      },
      "path": "/users",
      "uri": "https://api.usergrid.com/12313f0204bb-438a1ca1-cf9b-11e0-bcc1/438a1ca1-cf9b-11e0-bcc1-12313f0204bb/users",

      "entities": [
         {
        "uuid": "78c54a82-da71-11e0-b93d-12313f0204bb",
        "type": "user",
        "name": "John Doe",
        "created": 1315524171347008,
        "modified": 1315524526405008,
        "activated": true,
        "city": "san francisco",
        "email": "john.doe@gmail.com",
        "metadata": {
          "path": "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
          "sets": {
            "rolenames": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
            "permissions": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
          },
          "collections": {
            "activities": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
            "devices": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/devices",
            "feed": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
            "groups": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
            "messages": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/messages",
            "queue": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/queue",
            "roles": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
            "following": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
            "followers": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
        },

        "username": "john.doe"
      }
    ],
    "timestamp": 1315524526343,
    "duration": 84,
    "organization": "my-org",
    "applicationName": "my-app"
    }

Querying to get users
---------------------

Use the GET method with a query to retrieve a set of users that meet the
query criteria. Standard querying methods and filters can be applied to
users. See [Queries and parameters](/queries-and-parameters) for details
on options for querying or filtering. By default, a maximum of 10
entities is returned at once.

### Request URI

GET /{org\_id}/{app\_id}/users?{query}

### Parameters

  Parameter                 Description
  ------------------------- -------------------------------------------------------------------------------------
  arg uuid|string org\_id   Organization UUID or organization name
  arg uuid|string app\_id   Application UUID or application name
  optparam string query     Query in the query language. See [Queries and parameters](/queries-and-parameters).

### Example - Request

-   [cURL](#curl_query_users)
-   [JavaScript (HTML5)](#javascript_query_users)
-   [Ruby](#ruby_query_users)
-   [Node.js](#nodejs_query_users)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/users?ql=select%20*%20where%20city%3D'Chicago'"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'users',
        qs:{ql:"select * where city='Chicago'"}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    result = app['users'].query "select * where city = 'Chicago'"
    users = result.collection

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'users',
        qs:{ql:"select * where city='Chicago'"}
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
    "application": "1c8f60e4-da67-11e0-b93d-12313f0204bb",
    "params": {
       "filter": [
         "city='chicago'?_=1329433677419"
       ]
       },
    "path": "/users",
    "uri": "https://api.usergrid.com/12313f0204bb-1c8f60e4-da67-11e0-b93d/1c8f60e4-da67-11e0-b93d-12313f0204bb/users",

    "entities": [
      {
        "uuid": "78c54a82-da71-11e0-b93d-12313f0204bb",
        "type": "user",
        "created": 1315524171347008,
        "modified": 1315524526405008,
        "activated": true,
        "city": "chicago",
        "email": "jdoe57@gmail.com",
        "metadata": {
          "path": "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
          "collections": {
            "activities": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
            "devices": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/devices",
            "feed": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
            "groups": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
            "messages": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/messages",
            "queue": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/queue",
            "roles": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
            "following": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
            "followers": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
            },
          "sets": {
            "rolenames": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
            "permissions": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
          }
        },
        "username": "jane.doe"
      }
      ],
      "timestamp": 1329433677599,
      "duration": 41,
      "organization": "my-org",
      "applicationName": "my-app"
    }

**Note:** As described in the “Getting a user” section, you can use the
username, UUID, or email property in the URL to retrieve a user. You can
also use those properties (as well as any entity properties) in queries
and filters. However, you cannot use the username, UUID, or email
properties in a “contains” search. That’s because these properties are
primary keys, which means that they’re stored in slightly different way
than other properties.

This means that you can issue a query that searches for an exact match
to a username, UUID, or email property. You can also do a wildcard
search based on a partial match to those properties. But, you can’t
issue a query that uses the “contains” keyword to search for a partial
match to those properties.

For example, the following cURL request will return the jane.doe user
entity:

    curl -X GET "https://api.usergrid.com/edort1/sandbox/users?users?ql=username%3D'jane.doe'"

The following cURL request will also work:

    curl -X GET "https://api.usergrid.com/edort1/sandbox/users?ql=username%3D'ja*'"

The following cURL request will fail:

    curl -X GET "https://api.usergrid.com/edort1/sandbox/users?users?ql=username%20'contains%20'ja*'"

Adding a user to a group
------------------------

Use the POST method to add a user to a group. If the named group does
not yet exist, an error message is returned.

### Request URI

POST /{org\_id}/{app\_id}/groups/{uuid|groupname}/users/{uuid|username}

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string groupname   UUID or name of the group
  arg uuid|string username    UUID or username of user

### Example - Request

-   [cURL](#curl_add_user)
-   [JavaScript (HTML5)](#javascript_add_user)
-   [Ruby](#ruby_add_user)
-   [Node.js](#nodejs_add_user)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups/mynewgroup/users/john.doe"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'groups/mynewgroup/users/john.doe'    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/mynewgroup/users/john.doe'].post nil

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'groups/mynewgroup/users/john.doe'    
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
    "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
    "params": {},
    "path": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users",
    "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/groups/a668717b-67cb-11e1-8223-12313d14bde7/users",

    "entities": [
          {
            "uuid": "6fbc8157-4786-11e1-b2bd-22000a1c4e22",
            "type": "user",
            "nanme": "John Doe",
            "created": 1327517852364015,
            "modified": 1327517852364015,
            "activated": true,
            "email": "john.doe@mail.com",
            "metadata": {
              "connecting": {
                "owners":   "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/connecting/owners"
                 },
              "path": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22",
              "sets": {
                "rolenames": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/rolenames",
                "permissions": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/permissions"
                 },
              "collections": {
                "activities": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/activities",
                "devices": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/devices",
                "feed": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/feed",
                "groups": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/groups",
                "roles": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/roles",
                "following": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/following",
                "followers": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/followers"
                 }
            },
            "picture": "https://www.gravatar.com/avatar/90f823ba15655b8cc8e3b4d63377576f",
            "username": "john.doe"
          }
       ],
       "timestamp": 1331066031380,
       "duration": 64,
       "organization" : "my-org",
       "applicationName": "my-app"
    }

Adding a user to a collection or creating a connection
------------------------------------------------------

Use the POST method to add a user to a collection or create a
relationship between a user and another entity.

There are two common use cases, described separately as Use Case 1 and 2
below.

### Request URI - Use Case 1

POST /{org\_id}/{app\_id}/{collection}/{first\_entity\_id}/{relationship}/{second\_entity\_id}

### Parameters - Use Case 1

  Parameter                           Description
  ----------------------------------- --------------------------------------------------
  arg uuid|string org\_id             Organization UUID or organization name
  arg uuid|string app\_id             Application UUID or application name
  arg string collection               Collection name
  arg uuid|string first\_entity\_id   First entity UUID or entity name
  arg string relationship             Collection name or connection type (e.g., likes)
  arg uuid second\_entity\_id         Second entity UUID

If the relationship is a collection (such as a users collection for the
group employees), this request adds the second entity to the first
entity’s collection of the specified name. (Note that employees is an
entity name within the groups collection.) In the case of a group, this
is how you add users as group members.

### Example - Request - Use Case 1 (Add a user to a collection)

-   [cURL](#curl_add_user_collection)
-   [JavaScript (HTML5)](#javascript_add_user_collection)
-   [Ruby](#ruby_add_user_collection)
-   [Node.js](#nodejs_add_user_collection)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups/employees/users/jane.doe"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'groups/employees/users/jane.doe'    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success &mdash: data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/employees/users/jane.doe'].post nil

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'groups/employees/users/jane.doe'    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success &mdash: data will contain raw results from API call        
        }
    });

### Example - Response - Use Case 1 (Add a user to a collection)

    {
    "action": "post",
    "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
    "params": {},
    "path": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users",
    "uri": "https://api.usergrid.com/12313d14bde7-3e163873-6725-11e1-8223/3400ba10-cd0c-11e1-bcf7-12313d1c4491/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users",

    "entities": [
       "uuid": "ab225e6d-d503-11e1-b36a-12313b01d5c1",
       "type": "user",
       "created": 1343074753060,
       "modified": 1350688633477,
       "activated": true,
       "city": "san Francisco",
       "email": "jane.doe@gmail.com",
       "metadata": {
         "path": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1",
         "sets": {
           "rolenames": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/rolenames",
           "permissions": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/permissions"
            },
           "collections": {
              "activities": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/activities",
              "devices": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/devices",
              "feed": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/feed",
              "groups": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/groups",
              "roles": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/roles",
              "following": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/following",
              "followers": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/followers"
            }
          },
          "username": "jane.doe"
        }
    ],
    "timestamp": 1331075231504,
    "duration": 42,
    "organization": "my-org",
    "applicationName": "my-app"
    }

If the relationship is not previously defined for the entity, a
connection is created. For example, if the relationship is specified as
likes, and that relationship has not been previously been defined for
the entity, the second entity is connected to the first with a likes
relationship.

### Example - Request - Use Case 1 (Create a connection)

-   [cURL](#curl_create_connection1)
-   [JavaScript (HTML5)](#javascript_create_connection1)
-   [Ruby](#ruby_create_connection1)
-   [Node.js](#nodejs_create_connection1)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/users/jane.doe/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'users/jane.doe/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0'    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['users/jane.doe/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0'].post nil

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'users/jane.doe/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0'    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

### Example - Response - Use Case 1 (Create a connection)

    {
    "action": "post",
    "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
    "params": {},
    "path": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes",
    "uri": "https://api.usergrid.com/12313d14bde7-3e163873-6725-11e1-8223/3400ba10-cd0c-11e1-bcf7-12313d1c4491/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes",
    entities": [
       "uuid": "d9693ec3-61c9-11e2-9ffc-02e81adcf3d0",
       "type": "user",
       "created": 13430746203740,
       "modified": 1351893399022,
       "activated": true,
       "city": "san Francisco",
       "email": "john.doe@mail.com",
       "metadata": {
         "connecting": {
              "likes": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/connecting/likes"
         },
         "path": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0",
         "sets": {
           "rolenames": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/rolenames",
           "permissions": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/permissions"
            },
           "collections": {
              "activities": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/activities",
              "devices": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/devices",
              "feed": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/feed",
              "groups": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/groups",
              "roles": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/5c0c1789-d503-11e1-b36a-12313b01d5c1/roles",
              "following": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/following",
              "followers": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/followers"
            }
          },
          "username": "john.doe"
        }
    ],
    "timestamp": 1352140583264,
    "duration": 42,
    organization": "my-org",
    "applicationName": "my-app"
    }

### Request URI - Use Case 2

POST /{org\_id}/{app\_id}/{collection}/{first\_entity\_id}/{relationship}/{second\_entity\_type}/{second\_entity\_id}

### Parameters - Use Case 2

  Parameter                            Description
  ------------------------------------ --------------------------------------------------
  arg uuid|string org\_id              Organization UUID or organization name
  arg uuid|string app\_id              Application UUID or application name
  arg string collection                Collection name
  arg uuid|string first\_entity\_id    First entity UUID or entity name
  arg string relationship              Collection name or connection type (e.g., likes)
  arg string second\_entity\_type      Second entity type
  arg uuid|string second\_entity\_id   Second entity UUID or entity name

### Example - Request - Use Case 2

When creating a connection, if you specify the entity type for the
second entity, then you can create the connection using the entity’s
name rather than its UUID.

-   [cURL](#curl_create_connection2)
-   [JavaScript (HTML5)](#javascript_create_connection2)
-   [Ruby](#ruby_create_connection2)
-   [Node.js](#nodejs_create_connection2)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/users/john.doe/likes/food/pizza"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'users/john.doe/likes/food/pizza'    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['users/john.doe/likes/food/pizza'].post nil

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'users/john.doe/likes/food/pizza'    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response - Use Case 2

    {
    "action": "post",
    "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
    "params": {},
    "path": "/users/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/likes",
    "uri": "https://api.usergrid.com/12313d14bde7-3e163873-6725-11e1-8223/3400ba10-cd0c-11e1-bcf7-12313d1c4491/users/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/likes",

    "entities": [
    {
          "uuid": "5c93163a-277b-11e2-8d42-02e81ae640dc",
          "type": "food",
          "name": "pizza",
          "created": 1352142156214,
          "modified": 1352142156214,
          "metadata": {
            "path": "/users/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/likes/5c93163a-277b-11e2-8d42-02e81ae640dc"
          }
        }
    ],
    "timestamp": 1331075231504,
    "duration": 42,
    "organization": "my-org",
    "applicationName": "my-app"
    }

Deleting a user from a collection or deleting a connection
----------------------------------------------------------

Use the DELETE method to delete a user from a collection or delete a
relationship between a user and another entity.

There are two common use cases, described separately as Use Case 1 and 2
below.

### Request URI - Use Case 1

DELETE /{org\_id}/{app\_id}/{collection}/{first\_entity\_id}/{relationship}/{second\_entity\_id}

### Parameters - Use Case 1

  Parameter                           Description
  ----------------------------------- -------------------------------------------------
  arg uuid|string org\_id             Organization UUID or organization name
  arg uuid|string app\_id             Application UUID or application name
  arg string collection               Collection name
  arg uuid|string first\_entity\_id   First entity UUID or entity name
  arg string relationship             Collection name or connection type (e.g., likes
  arg uuid second\_entity\_id         Second entity UUID

If the relationship is a collection (such as a users collection for the
group employees), this request removes the second entity from the first
entity’s collection of the specified name. In the case of a group, this
is how you delete users as group members.

### Example - Request - Use Case 1 (Remove a user from a collection)

-   [cURL](#curl_delete_user_collection)
-   [JavaScript (HTML5)](#javascript_delete_user_collection)
-   [Ruby](#ruby_delete_user_collection)
-   [Node.js](#nodejs_delete_user_collection)

<!-- -->

    curl -X DELETE "https://api.usergrid.com/my-org/my-app/groups/employees/users/jane.doe"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'DELETE',
        endpoint:'groups/employees/users/jane.doe'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/employees/users/jane.doe'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'groups/employees/users/jane.doe'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

### Example - Response - Use Case 1 (Remove a user from a collection)

    {
      "action" : "delete",
      "application" : "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params" : {},
      "path" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users",
      "uri" : "https://api.usergrid.com/005056c00008-4353136f-e978-11e0-8264/3400ba10-cd0c-11e1-bcf7-12313d1c4491/users",

      "entities" : [ {
        "uuid" : "ab225e6d-d503-11e1-b36a-12313b01d5c1",
        "type" : "user",
        "created" : 1343074753060,
        "modified" : 1352227377822,
        "activated" : true,
        "email" : "jane.doe@gmail.com",
        "metadata" : {
          "path" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1",
          "sets" : {
            "rolenames" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/rolenames",
            "permissions" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/permissions"
          },
          "collections" : {
            "activities" : "//groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/activities",
            "devices" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/devices",
            "feed" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc1/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/fee",
            "groups" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/groups",
            "roles" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/roles",
            "following" : /groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/following",
            "followers" : "/groups/3cc5ae7e-64e6-11e2-b6fb-02e81ae640dc/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/followers"
          }
        },
        "username" : "jane.doe"
      } ],
      "timestamp" : 1352238030137,
      "duration" : 477,
      "organization" : "my-org",
      "applicationName": "my-app"
    }

For connections, you provide the connection type. For example, if the
second entity is connected to the first with a likes relationship, the
delete request removes the connection.

### Example - Request - Use Case 1 (Remove a connection)

-   [cURL](#curl_delete_connection)
-   [JavaScript (HTML5)](#javascript_delete_connection)
-   [Ruby](#ruby_delete_connection)
-   [Node.js](#nodejs_delete_connection)

<!-- -->

    curl -X DELETE "https://api.usergrid.com/my-org/my-app/users/jane.doe/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'DELETE',
        endpoint:'users/jane.doe/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['users/jane.doe/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'users/jane.doe/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

### Example - Response - Use Case 1 (Remove a connection)

    {
    "action": "delete",
    "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
    "params": {},
    "path": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes",
    "uri": "https://api.usergrid.com/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes",

    "entities": [
       "uuid": "d9693ec3-61c9-11e2-9ffc-02e81adcf3d0",
       "type": "user",
       "created": 134307462037,
       "modified": 1352227377728,
       "activated": true,
       "city": "san Francisco",
       "email": "john.doe@mail.comm",
       "metadata": {
            "connecting": {
              "likes": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/connecting/likes"
            },
         "path": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0",
         "sets": {
           "rolenames": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/rolenames",
           "permissions": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/permissions"
            },
            "connections": {
              "likes": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/likes"
            },
           "collections": {
              "activities": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/activities",
              "devices": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/devices",
              "feed": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/feed",
              "groups": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/group",
              "roles": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/roles",
              "following": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/following",
              "followers": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/followers"
            }
          },
          "username": "john.doe"
        }
    ],
    "timestamp": 1352240516683,
    "duration": 196,
    "organization": "my-org",
    "applicationName": "my-app"
    }

### Request URI - Use Case 2

DELETE /{org\_id}/{app\_id}/{collection}/{first\_entity\_id}/{relationship}/{second\_entity\_type}/{second\_entity\_id}

### Parameters - Use Case 2

  Parameter                            Description
  ------------------------------------ --------------------------------------------------
  arg uuid|string org\_id              Organization UUID or organization name
  arg uuid|string app\_id              Application UUID or application name
  arg string collection                Collection name
  arg uuid|string first\_entity\_id    First entity UUID or entity name
  arg string relationship              Collection name or connection type (e.g., likes)
  arg string second\_entity\_type      Second entity type
  arg uuid|string second\_entity\_id   Second entity UUID or entity name

### Example - Request - Use Case 2

When removing a connection, if you specify the entity type for the
second entity, then you can delete the connection using the entity’s
name rather than its UUID.

-   [cURL](#curl_delete_connection1)
-   [JavaScript (HTML5)](#javascript_delete_connection1)
-   [Ruby](#ruby_delete_connection1)
-   [Node.js](#nodejs_delete_connection1)

<!-- -->

    curl -X DELETE "https://api.usergrid.com/my-org/my-app/users/test-user-1/follows/test-user-2"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'DELETE',
        endpoint:'users/test-user-1/follows/test-user-2'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['users/test-user-1/follows/test-user-2'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'users/test-user-1/follows/test-user-2'
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
     "action":
     "delete",
     "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
     "params": {},
     "path": "/users/dd26b1d9-2863-11e2-b4c6-02e81ac5a17b/follows",
     "uri": "https://api.usergrid.com/"3400ba10-cd0c-11e1-bcf7-12313d1c4491/users/dd26b1d9-2863-11e2-b4c6-02e81ac5a17b/follows",
     "entities": [],
     "timestamp": 1331075473194,
     "duration": 22,
     "organization": "my-org",
     "applicationName": "my-app"
    }

Querying a user’s collections or connections
--------------------------------------------

Use the GET method with a query to retrieve a user’s collections or
connections that meet the query criteria. Standard querying methods and
filters can be applied to users. See [Queries and
parameters](/queries-and-parameters) for details on options for querying
or filtering. By default, a maximum of 10 entities is returned at once.

### Request URI

GET /{org\_id}/{app\_id}/users/{uuid|username}/{relationship}?{query}

### Parameters

  Parameter                  Description
  -------------------------- -------------------------------------------------------------------------------------
  arg uuid|string org\_id    Organization UUID or organization name
  arg uuid|string app\_id    Application UUID or application name
  arg uuid|string username   Entity UUID or username
  arg string relationship    Collection name or connection type (e.g., likes)
  optparam string query      Query in the query language. See [Queries and parameters](/queries-and-parameters).

### Example - Request

-   [cURL](#curl_query_user_collections)
-   [JavaScript (HTML5)](#javascript_query_user_collections)
-   [Ruby](#ruby_query_user_collections)
-   [Node.js](#nodejs_query_user_collections)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/users/jane.doe/likes?ql=select%20*%20where%20city%3D'milwaukee'"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'users/jane.doe/likes',
        qs:{'ql':"select * where city='milwaukee'"}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['users/jane.doe/likes'].query "select * where city = 'milwaukee'"

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'users',
        users.qs = {'ql':"select * where city='milwaukee'"};
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
      "params":{
        "ql" : [ "select * where city='milwaukee'" ]
      },
      "path": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes",
      "uri": "https://api.usergrid.com/22000a1c4e22-5ea08de5-4d23-11e1-b41d/3400ba10-cd0c-11e1-bcf7-12313d1c4491/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes",

      "entities": [
        {
          "uuid": "895d7c11-5901-11e1-ac46-22000a1c5a67",
          "type": "restaurant",
          "created": 1329439893227,
          "modified": 1329439893227,
          "city": "milwaukee",
          "metadata": {
            "connecting": {
              "likes": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c17/connecting/likes"
              },
              "connection": "likes",
              "cursor": "gGkAAQEAgHMACW1pbHdhdWtlZQCAdQAQiV18EVkBEeGsRiIAChxaZwCAcwAKcmVzdGF1cmFudACAdQAQUrM7mVkCEeGsRiIAChxaZwA",
              "associated": "42039286-c984-3b46-bbfb-28c4abb9b27c",
              "path": "/users/ab225e6d-d503-11e1-b36a-12313b01d5c1/likes/895d7c11-5901-11e1-ac46-22000a1c5a67"
          },
          "name": "Tulep"
        }
      ]
      "timestamp": 1315357451949,
      "duration": 52,
      "organization": "my-org",
      "applicationName": "my-app"
    }

Getting a user’s feed
---------------------

Use the GET method to retrieve a user’s feed.

### Request URI

GET /{org\_id}/{app\_id}/users/{uuid|username}/feed

### Example - Request

-   [cURL](#curl_get_user_feed)
-   [JavaScript (HTML5)](#javascript_get_user_feed)
-   [Ruby](#ruby_get_user_feed)
-   [Node.js](#nodejs_get_user_feed)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/users/john.doe/feed"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'users/john.doe/feed'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    activities = app['users/john.doe/feed'].collection

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'users/john.doe/feed'
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
      "application" : "3400ba10-cd0c-11e1-bcf7-12313d1c44914",
      "params" : {},
      "path" : "/users/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/feed",
      "uri" : "https://api.usergrid.com/3400ba10-cd0c-11e1-bcf7-12313d1c44914/users/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/feed",
      "entities" : [ {
        "uuid" : "ffd79647-f399-11e1-aec3-12313b06ae01",
        "type" : "activity",
        "created" : 1346437854569,
        "modified" : 1346437854569,
        "actor" : {
          "displayName" : "John Doe",
          "image" : {
            "duration" : 0,
            "height" : 80,
            "url" : "http://www.gravatar.com/avatar/",
            "width" : 80
          },
          "uuid" : "d9693ec3-61c9-11e2-9ffc-02e81adcf3d0",
          "email" : "john.doe@gmail.com",
          "username" : "john.doe"
        },
        "content" : "Hello World!",
        "metadata" : {
          "cursor" : "gGkAAQMAgGkABgE5ffM1aQCAdQAQ_9eWR_OZEeGuwxIxOwauAQCAdQAQABlaOvOaEeGuwxIxOwauAQA",
          "path" : "/users/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/feed/ffd79647-f399-11e1-aec3-12313b06ae01"
        },
        "published" : 1346437854569,
        "verb" : "post"
      }, {
        "uuid" : "2482a1c5-e7d0-11e1-96f6-12313b06d112",
        "type" : "activity",
        "created" : 1345141694958,
        "modified" : 1345141694958,
        "actor" : {
          "displayName" : "moab",
          "image" : {
            "duration" : 0,
            "height" : 80,
            "url" : "http://www.gravatar.com/avatar/",
            "width" : 80
          },
          "uuid" : "d9693ec3-61c9-11e2-9ffc-02e81adcf3d0",
          "email" : "massoddb@mfdsadfdsaoabl.com",
          "username" : "moab"
        },
        "content" : "checking in code left and right!!",
        "metadata" : {
          "cursor" : "gGkAAQMAgGkABgE5MLFh7gCAdQAQJIKhxefQEeGW9hIxOwbREgCAdQAQJNEP6ufQEeGW9hIxOwbREgA",
          "path" : "/users/d9693ec3-61c9-11e2-9ffc-02e81adcf3d0/feed/2482a1c5-e7d0-11e1-96f6-12313b06d112"
        },
        "published" : 1345141694958,
        "verb" : "post"
      } ],
      "timestamp" : 1346438331316,
      "duration" : 144,
      "organization": "my-org",
      "applicationName": "my-app"
    }

User properties
---------------

The following are the system-defined properties for user entities. You
can create application-specific properties for a user entity in addition
to the system-defined properties. The system-defined properties are
reserved. You cannot use these names to create other properties for a
user entity. In addition the users name is reserved for the users
collection — you can't use it to name another collection.

  Property     Type      Description
  ------------ --------- ---------------------------------------------------------------------------------
  uuid         UUID      User’s unique entity ID
  type         string    Type of entity, in this case “user”
  created      long      [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified     long      [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  username     string    Valid and unique string username (mandatory)
  password     string    User password
  email        string    Valid and unique email address
  name         string    User display name
  activated    boolean   Whether the user account is activated
  disabled     boolean   Whether the user account is administratively disabled
  firstname    string    User first name
  middlename   string    User middle name
  lastname     string    User last name
  picture      string    User picture

The following property sets are assigned to user entities.

  Set           Type     Description
  ------------- -------- ---------------------------------------
  connections   string   Set of connection types (e.g., likes)
  rolenames     string   Set of roles assigned to a user
  permissions   string   Set of user permissions
  credentials   string   Set of user credentials

Users have the following associated collections.

  Collection   Type       Description
  ------------ ---------- -----------------------------------------------------
  groups       group      Collection of groups to which a user belongs
  devices      device     Collection of devices in the service
  activities   activity   Collection of activities a user has performed
  feed         activity   Inbox of activity notifications a user has received
  roles        role       Set of roles assigned to a user

 
