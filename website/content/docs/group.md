---
title: Group
category: docs
layout: docs
---

Group
=====

A group entity organizes users into a group. Using Apache Usergrid APIs you
can create, retrieve, update, or delete a group. You can also add or
delete a user to or from a group.

See [Group entity properties](#group_properties) for a list of the
system-defined properties for group entities. In addition, you can
create group properties specific to your application.

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Creating a new group
--------------------

Use the POST method to create a new group. Groups use paths to indicate
their unique names. This allows you to create group hierarchies by using
slashes. For this reason, you need to specify a path property for a new
group.

### Request URI

POST /{org\_id}/{app\_id}/groups

### Parameters

  Parameter                 Description
  ------------------------- ----------------------------------------
  arg uuid|string org\_id   Organization UUID or organization name
  arg uuid|string app\_id   Application UUID or application name

### Example - Request

-   [cURL](#curl_create_group)
-   [JavaScript (HTML5)](#javascript_create_group)
-   [Ruby](#ruby_create_group)
-   [Node.js](#nodejs_create_group)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups" -d '{"path":"mynewgroup"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'groups',
        body:{ path:'mynewgroup' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups'].post path: 'mynewgroup'

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'groups',
        body:{ path:'mynewgroup' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

### Example - Response

    {
    "action": "post",
    "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
    "params": {},
    "path": "/groups",
    "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/groups",

    "entities": [
        {
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
        }
     ],
    "timestamp": 1331066016563,
    "duration": 35,
    "organization" : "my-org",
    "applicationName": "my-app"
    }

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
            //error
        } else {
            //success - data will contain raw results from API call
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
            //error
        } else {
            //success - data will contain raw results from API call
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

Getting a group
---------------

Use the GET method to retrieve a group.

### Request URI

GET /{org\_id}/{app\_id}/groups/{uuid|groupname}

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string groupname   UUID or name of the group

### Example - Request

-   [cURL](#curl_get_group)
-   [JavaScript (HTML5)](#javascript_get_group)
-   [Ruby](#ruby_get_group)
-   [Node.js](#nodejs_get_group)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/mynewgroup"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'groups/mynewgroup'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    user = app['mynewgroup'].entity

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'groups/mynewgroup'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

### Example - Response

    {
    "action": "get",
    "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
    "params": {
          "_": [
            "1331066049869"
          ]
    },
    "path": "/groups",
    "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/groups",

    "entities": [
        {
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
        }
    ],
    "timestamp": 1331066050106,
    "duration": 18,
    "organization" : "my-org",
    "applicationName": "my-app"
    }

Updating a group
----------------

Use the PUT method to update a group.

### Request URI

PUT /{org\_id}{app\_id}/groups/{uuid|groupname} {request body}

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string groupname   UUID or name of the group
  request body                Set of entity properties

### Example - Request

-   [cURL](#curl_update_group)
-   [JavaScript (HTML5)](#javascript_update_group)
-   [Ruby](#ruby_update_group)
-   [Node.js](#nodejs_update_group)

<!-- -->

    curl -X PUT "https://api.usergrid.com/my-org/my-app/groups/mynewgroup" -d '("foo":"bar"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'PUT',
        endpoint:'groups/mynewgroup',
        body:{ foo:'bar' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    mynewgroup = app['groups/mynewgroup'].put foo: 'bar'

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'PUT',
        endpoint:'groups/mynewgroup',
        body:{ foo:'bar' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

### Example - Response

    {
    "action": "put",
    "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
    "params": {},
    "path": "/groups",
    "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/groups",

    "entities": [
        {
          "uuid": "a668717b-67cb-11e1-8223-12313d14bde7",
          "type": "group",
          "created": 1331066016571,
          "modified": 1331066092191,
          "foo": "bar",
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
        }
    ],
    "timestamp": 1331066092178,
    "duration": 31,
    "organization" : "my-org",
    "applicationName": "my-app"
    }

Deleting a user from a group
----------------------------

Use the DELETE method to delete a user from the specified group.

### Request URI

DELETE
/{org\_id}/{app\_id}/groups/{uuid|groupname}/users/{uuid|username}

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string groupname   UUID or name of the group
  arg uuid|string username    UUID or username of user to be deleted

### cURL Example - Request

-   [cURL](#curl_delete_user_group)
-   [JavaScript (HTML5)](#javascript_delete_user_group)
-   [Ruby](#ruby_delete_user_group)
-   [Node.js](#nodejs_delete_user_group)

<!-- -->

    curl -X DELETE "https://api.usergrid.com//my-org/my-app/groups/mynewgroup/users/john.doe"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'DELETE',
        endpoint:'groups/mynewgroup/users/john.doe'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/mynewgroup/users/john.doe'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'groups/mynewgroup/users/john.doe'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

### Example - Response

    {
    "action": "delete",
    "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
    "params": {
          "_": [
            "1331066118009"
          ]
    },
    "path": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users",
    "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/groups/a668717b-67cb-11e1-8223-12313d14bde7/users",

    "entities": [
          {
            "uuid": "6fbc8157-4786-11e1-b2bd-22000a1c4e22",
            "type": "user",
            "name": "John Doe",
            "created": 1327517852364015,
            "modified": 1327517852364015,
            "activated": true,
            "email": "john.doe@mail.com",
            "metadata": {
              "connecting": {
                "owners": "/groups/a668717b-67cb-11e1-8223-12313d14bde7/users/6fbc8157-4786-11e1-b2bd-22000a1c4e22/connecting/owners"
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
    "timestamp": 1331066118193,
    "duration": 236,
    "organization" : "my-org",
    "applicationName": "my-app"
    }

Deleting a group
----------------

Use the DELETE method to delete a group.

### Request URI

DELETE /{org\_id}/{app\_id}/groups/{uuid|groupname}

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string groupname   UUID or name of the group

### Example - Request

-   [cURL](#curl_delete_group)
-   [JavaScript (HTML5)](#javascript_delete_group)
-   [Ruby](#ruby_delete_group)
-   [Node.js](#nodejs_delete_group)

<!-- -->

    curl -X DELETE "https://api.usergrid.com//my-org/my-app/groups/mynewgroup"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'DELETE',
        endpoint:'groups/mynewgroup'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/mynewgroup'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'groups/mynewgroup'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

### Example - Response

    {
    "action": "delete",
    "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
    "params": {
        "_": [
          "1331066144280"
        ]
    },
    "path": "/groups",
    "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/groups",

    "entities": [
        {
          "uuid": "a668717b-67cb-11e1-8223-12313d14bde7",
          "type": "group",
          "created": 1331066016571,
          "modified": 1331066092191,
          "foo": "bar",
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
        }
    ],
    "timestamp": 1331066144462,
    "duration": 302,
    "organization" : "my-org",
    "applicationName": "my-app"
    }

Getting a group’s feed
----------------------

Use the GET method to retrieve the feed for a group. This gets a list of
all the activities that have been posted to this group, that is, the
activities for which this group has a relationship (owns).

### Request URI

GET /{org\_id}/{app\_id}/groups/{uuid|groupname}/feed

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string groupname   UUID or name of the group

### Example - Request

-   [cURL](#curl_get_group_feed)
-   [JavaScript (HTML5)](#javascript_get_group_feed)
-   [Ruby](#ruby_get_group_feed)
-   [Node.js](#nodejs_get_group_feed)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/mygroup/feed"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'groups/mygroup/feed'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    activities = app['groups/mygroup/feed'].collection

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'groups/mygroup/feed'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

### Example - Response

    {
        "action": "get",
        "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
        "params":  {},
        "path": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/feed",
        "uri": "https://api.usergrid.com/my-org/my-app/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/feed",
        "entities":  [
           {
            "uuid": "563f5d96-37f3-11e2-a0f7-02e81ae640dc",
            "type": "activity",
            "created": 1353952903811,
            "modified": 1353952903811,
            "actor":  {
              "displayName": "John Doe",
              "image":  {
                "duration": 0,
                "height": 80,
                "url": "http://www.gravatar.com/avatar/",
                "width": 80
              },
              "uuid": "1f3567aa-da83-11e1-afad-12313b01d5c1",
              "email": "john.doe@gmail.com",
            "username": "john.doe"
            },
            "content": "Hello World!",
            "metadata":  {
              "cursor": "gGkAAQMAgGkABgE7PeHCgwCAdQAQVj9dljfzEeKg9wLoGuZA3ACAdQAQVkVRCTfzEeKg9wLoGuZA3AA",
              "path": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/feed/563f5d96-37f3-11e2-a0f7-02e81ae640dc"
            },
            "published": 1353952903811,
            "verb": "post"
          }
        ],
      "timestamp": 1353953272756,
      "duration": 29,
      "organization": "my-org",
      "applicationName": "my-app"

Getting all users in a group
----------------------------

Use the GET method to retrieve all the users in a group.

### Request URI

GET /{org\_id}/{app\_id}/groups/{uuid|groupname}/users

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string groupname   UUID or name of the group

### Example - Request

-   [cURL](#curl_get_group_users)
-   [JavaScript (HTML5)](#javascript_get_group_users)
-   [Ruby](#ruby_get_group_users)
-   [Node.js](#nodejs_get_group_users)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/mygroup/users"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'groups/mygroup/users'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    activities = app['groups/mygroup/users'].collection

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'groups/mygroup/users'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error
        } else {
            //success - data will contain raw results from API call
        }
    });

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

Group properties
----------------

The following are the system-defined properties for group entities. The
system-defined properties are reserved. You cannot use these names to
create other properties for a group entity. In addition the groups name
is reserved for the group collection — you can't use it to name another
collection.

The look-up properties for the entities of type group are UUID and path,
that is, you can use the uuid or path property to reference a group in
an API call. However, you can search on a group using any property of
the group entity. See [Queries and parameters](/queries-and-parameters)
for details on searching.

### General properties

Groups have the following general properties.

  Property   Type     Description
  ---------- -------- ---------------------------------------------------------------------------------
  uuid       UUID     Group’s unique entity ID
  type       string   Type of entity, in this case “user”
  created    long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified   long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  path       string   Valid slash-delimited group path (mandatory)
  title      string   Display name

### Set properties

Groups have the following set properties.

  Set           Type     Description
  ------------- -------- ---------------------------------------
  connections   string   Set of connection types (e.g., likes)
  rolenames     string   Set of roles assigned to a group
  credentials   string   Set of group credentials

### Collections

Groups have the following collections.

  Collection   Type       Description
  ------------ ---------- ------------------------------------------------------
  users        user       Collection of users in the group
  activities   activity   Collection of activities a user has performed
  feed         activity   Inbox of activity notifications a group has received
  roles        role       Set of roles to which a group belongs

 
