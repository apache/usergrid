# Activity

Most modern applications struggle to manage data streams, such as those
that contain an ongoing list of comments, activities, and tweets. In
particular, mobile applications are prone to generating very large
amounts of data in a data stream. Beyond that, additions to a data
stream must often be routed automatically to subscribers or filtered or
counted.

App services provides an activity entity that is specifically designed
for data streams. An activity is an entity type that represents activity
stream actions (see the [JSON Activity Streams 1.0
specification](http://activitystrea.ms/specs/json/1.0/) for more
information about these actions).

When a user creates an activity, it creates a relationship between the
activity and the user who created it. Because this relationship exists,
the activity will appear in the feed of any of the user’s followers.
Think of the Activities endpoint (/users/{uuid|username}/activities) as
an "outbox" of news items created by the user. Think of the Feed
endpoint (/users/{uuid|username}/feed) as an "inbox" of news items meant
to be seen or consumed by the user.

A user can also post an activity to a group (located at
/groups/{uuid|groupname}/activities). This allows you to emulate
Facebook-style group functionality, where a limited number of users can
share content on a common "wall". In any of these cases, there is no
need to construct publish/subscribe relationships manually.

Activity entities are particularly useful in applications that enable
users to post content to activity streams (also called feeds) and to
display activity streams. Some examples of these applications are
Twitter, foursquare, and Pinterest. For example, when a Twitter user
posts a short, 140-character or less, "tweet", that activity gets added
to the user's activity stream for display as well as to the activity
streams of any of the user's followers.

Using App services APIs you can create, retrieve, update, and delete
activity entities. See You do not have access to view this node for
descriptions of these APIs.

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Creating an activity {dir="ltr"}
--------------------

Use the  POST method to create an activity in the activities collection.

### Request URI

POST /{org\_id}/{app\_id}/users/{uuid|username}/activities {request
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
| request body                         | One or more sets of activity         |
|                                      | properties:                          |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "actor":                       |
|                                      |         {                            |
|                                      |         "displayName":"John Doe",    |
|                                      |         "uuid":"1f3567aa-da83-11e1-a |
|                                      | fad-12313b01d5c1",                   |
|                                      |         "username":"john.doe",       |
|                                      |         "image":{                    |
|                                      |           "duration":0,              |
|                                      |           "height":80,               |
|                                      |           "url":"http://www.gravatar |
|                                      | .com/avatar/","width":80},           |
|                                      |       "email":"john.doe@gmail.com"}, |
|                                      |       "verb":"post",                 |
|                                      |       "content":"Hello World!"       |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

### Example - Request

-   [cURL](#curl_create_activity)
-   [JavaScript (HTML5)](#javascript_create_activity)
-   [Ruby](#ruby_create_activity)
-   [Node.js](#nodejs_create_activity)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/users/john.doe/activities" -d '{"actor":{"displayName":"John Doe","uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1","username":"john.doe",
    "image":{"duration":0,"height":80,"url":"http://www.gravatar.com/avatar/","width":80},
    "email":"john.doe@gmail.com"},"verb":"post","content":"Hello World!"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'users/john.doe/activities',
        body:{"actor":
               {"displayName":"John Doe",
                "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
                "username":"john.doe",
                "image":
                 {"duration":0,
                  "height":80,
                  "url":"http://www.gravatar.com/avatar/",
                  "width":80},
                  "email":"john.doe@gmail.com"},
                 "verb":"post",
                 "content":"Hello World!"}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            // success — POST worked. Data will contain raw results from API call        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app.create_activity { actor: { displayName: 'John Doe', uuid: '1f3567aa-da83-11e1-afad-12313b01d5c1', username: 'john.doe', image: { duration: 0, height: 80, url: 'http://www.gravatar.com/avatar/', width: 80 }, email: 'john.doe@gmail.com' }, verb: 'post', content: 'Hello World!' }

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'users/john.doe/activities',
        body:{"actor":
               {"displayName":"John Doe",
                "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
                "username":"john.doe",
                "image":
                 {"duration":0,
                  "height":80,
                  "url":"http://www.gravatar.com/avatar/",
                  "width":80},
                  "email":"john.doe@gmail.com"},
                 "verb":"post",
                 "content":"Hello World!"}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            // success — POST worked. Data will contain raw results from API call        
        }
    });

### Example - Response

    {
     "action" : "post",
     "application" : "5111c463-6a42-11e1-b6dd-1231380a0284",
     "params" : {
     },
     "path" : "/users/1f3567aa-da83-11e1-afad-12313b01d5c1/activities",
     "uri" : "https://api.usergrid.com/5111c463-6a42-11e1-b6dd-1231380a0284/users/1f3567aa-da83-11e1-afad-12313b01d5c1/activities",
     "entities" : [ {
       "uuid" : "da448955-f3aa-11e1-8042-12313d331ae8",
       "type" : "activity",
       "created" : 1346445092974,
       "modified" : 1346445092974,
       "actor" : {
         "displayName" : "John Doe",
         "uuid" : "1f3567aa-da83-11e1-afad-12313b01d5c1",
         "username" : "john.doe",
         "image" : {
           "duration" : 0,
           "height" : 80,
           "url" : "http://www.gravatar.com/avatar/",
           "width" : 80
         },
         "email" : "john.doe@gmail.com"
       },
       "content" : "Hello World!",
       "metadata" : {
         "path" : "/users/1f3567aa-da83-11e1-afad-12313b01d5c1/activities/da448955-f3aa-11e1-8042-12313d331ae8"
       },
       "published" : 1346445092974,
       "verb" : "post"
     } ],
     "timestamp" : 1346445092827,
     "duration" : 1406,
     "organization": "my-org",
     "applicationName": "my-app"
    }

**Note:** Anytime a logged-in user makes a request, you can substitute
"me" for the uuid or username. So the format of a request to create an
activity for the currently logged-in user would look like this:

POST /{org\_id}/{app\_id}/users/me/activities {request body}

The users/me endpoint is accessible only if you provide an access token
with the request. If you don't provide an access token with the request,
that is, you make an anonymous (or "guest") call, the system will not be
able to determine which user to return as /users/me.

When you create an activity it creates a relationship between the
activity and the user who created it. In other words, the newly created
activity above belongs to john.doe. Another way of saying this is the
user "owns" the activity. And because this relationship exists, the
activity will appear in the feed of any of the user’s followers (in this
example, anyone who is following john.doe). However, it will not appear
in the feed of people the user follows. The activity is accessible at
the /activites endpoint to users who have the permission to read that
endpoint.

Notice the properties specified in the request body in the previous
example are *actor*, *verb*, and *content*. The *actor*, *verb*, and
*content* properties are built into the Activity entity (see
[System-defined activity properties](#properties)). The actor property
specifies properties of the entity that performs the action (here, user
john.doe). The gravatar URL is used to create an icon for the
activity. And because an Activity is simply an Apache Usergrid entity, you
can also create custom properties.

The *verb* parameter is descriptive. You can use it to indicate what
type of activity is posted, for example, an image versus text. The
value*post* is defined in the JSON Activity Streams specification as
“the act of authoring an object and then publishing it online.“

Posting an activity to a group {dir="ltr"}
------------------------------

Use the POST method to post an activity to a specific group. In this
case the activity is created in the activities collection and is
accessible at the /activities endpoint to users who have the permission
to read that endpoint. In addition, a relationship is established
between the activity and the group, and because of that, the activity
will appear in the group’s feed. The group "owns" the activity. Also,
the activity will be published in the feed of all users that are members
of the group.

Request URI

POST /{org\_id}/{app\_id}/groups/{uuid|groupname}/activities {request
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
| arg uuid|string groupname            | UUID or name of the group            |
+--------------------------------------+--------------------------------------+
| request body                         | One or more sets of activity         |
|                                      | properties:                          |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "actor":                       |
|                                      |         {                            |
|                                      |         "displayName":"John Doe",    |
|                                      |         "uuid":"1f3567aa-da83-11e1-a |
|                                      | fad-12313b01d5c1",                   |
|                                      |         "username":"john.doe",       |
|                                      |         "image":{                    |
|                                      |           "duration":0,              |
|                                      |           "height":80,               |
|                                      |           "url":"http://www.gravatar |
|                                      | .com/avatar/","width":80},           |
|                                      |       "email":"john.doe@gmail.com"}, |
|                                      |       "verb":"post",                 |
|                                      |       "content":"Hello World!"       |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

### Example - Request

-   [cURL](#curl_post_activity_group)
-   [JavaScript (HTML5)](#javascript_post_activity_group)
-   [Ruby](#ruby_post_activity_group)
-   [Node.js](#nodejs_post_activity_group)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups/mygroup/activities" -d '{"actor":{"displayName":"John Doe","uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1","username":"john.doe",
    "image":{"duration":0,"height":80,"url":"http://www.gravatar.com/avatar/","width":80},
    "email":"john.doe@gmail.com"},"verb":"post","content":"Hello World!"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'groups/mygroup/activities',
        body:{"actor":
               {"displayName":"John Doe",
                "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
                "username":"john.doe",
                "image":
                 {"duration":0,
                  "height":80,
                  "url":"http://www.gravatar.com/avatar/",
                  "width":80},
                  "email":"john.doe@gmail.com"},
                 "verb":"post",
                 "content":"Hello World!"}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — POST worked. Data will contain raw results from API call.        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/mygroup/activities'].post { actor:{ displayName: 'John Doe', uuid : '1f3567aa-da83-11e1-afad-12313b01d5c1', username: 'john.doe', image: { duration: 0, height: 80, url: 'http://www.gravatar.com/avatar/', width: 80 }, email: 'john.doe@gmail.com' }, verb: 'post', content: 'Hello World!' }

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'groups/mygroup/activities',
        body:{"actor":
               {"displayName":"John Doe",
                "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
                "username":"john.doe",
                "image":
                 {"duration":0,
                  "height":80,
                  "url":"http://www.gravatar.com/avatar/",
                  "width":80},
                  "email":"john.doe@gmail.com"},
                 "verb":"post",
                 "content":"Hello World!"}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — POST worked. Data will contain raw results from API call.        
        }
    });

Because this relationship exists, this activity will appear in the feed
of all users who are members of mygroup. It won't appear in the feeds of
the group members’ followers or in feeds of users they follow.

### Example - Response

    {
      "action": "post",
      "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params":  {},
      "path": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/activities",
      "uri": "https://api.usergrid.com/my-org/my-app/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/activities",
      "entities":  [
         {
          "uuid": "563f5d96-37f3-11e2-a0f7-02e81ae640dc",
          "type": "activity",
          "created": 1353952903811,
          "modified": 1353952903811,
          "actor":  {
            "displayName": "John Doe",
            "uuid": "1f3567aa-da83-11e1-afad-12313b01d5c1",
            "username": "john.doe",
            "image":  {
              "duration": 0,
              "height": 80,
              "url": "http://www.gravatar.com/avatar/",
              "width": 80
            },
            "email": "john.doe@gmail.com"
          },
          "content": "Hello World!",
          "metadata":  {
            "path": "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/activities/563f5d96-37f3-11e2-a0f7-02e81ae640dc"
          },
          "published": 1353952903811,
          "verb": "post"
        }
      ],
      "timestamp": 1353952903800,
      "duration": 81,
      "organization": "my-org",
      "applicationName": "my-app"

Creating an activity for a user's followers in a group {dir="ltr"}
------------------------------------------------------

Use the POST method to create an activity that will be published only in
the feeds of users who (1) follow you, and (2) are in the same group to
which you posted the activity. This is useful if you want to create
specific groups of friends (for example, acquaintances or colleagues)
and publish content to them with more precise privacy settings. This
allows you to re-create a privacy model similar to Google+’s Circles or
Facebook current privacy system.

When you create an activity for a user’s followers in a group:

-   The activity is accessible at the /activities endpoint to users who
    have the permission to read that endpoint. The activity will not be
    cross-posted to the group’s activity endpoint
    (/groups/{uuid|groupname}/activities)
-   A relationship is automatically created between the activity entity
    that was just created and the user within that group
    (/groups/{uuid|groupname}/users/{uuid|username})
-   The user within the group
    (/groups/{uuid|groupname}/users/{uuid|username}) becomes the owner
    of the activity (through the owner property in the activity).

### Request URI

POST
/{org\_id}/{app\_id}/groups/{uuid|groupname}/users/{uuid|username}/activities
{request body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| arg uuid|string org\_id              | Organization UUID or organization    |
|                                      | name                                 |
+--------------------------------------+--------------------------------------+
| arg uuid|string app\_id              | Application UUID or application name |
+--------------------------------------+--------------------------------------+
| arg uuid|string groupname            | UUID or name of the group            |
+--------------------------------------+--------------------------------------+
| arg uuid|string username             | UUID or name of the user             |
+--------------------------------------+--------------------------------------+
| request body                         | One or more sets of activity         |
|                                      | properties:                          |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "actor":                       |
|                                      |         {                            |
|                                      |         "displayName":"John Doe",    |
|                                      |         "uuid":"1f3567aa-da83-11e1-a |
|                                      | fad-12313b01d5c1",                   |
|                                      |         "username":"john.doe",       |
|                                      |         "image":{                    |
|                                      |           "duration":0,              |
|                                      |           "height":80,               |
|                                      |           "url":"http://www.gravatar |
|                                      | .com/avatar/","width":80},           |
|                                      |       "email":"john.doe@gmail.com"}, |
|                                      |       "verb":"post",                 |
|                                      |       "content":"Hello World!"       |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

### Example - Request

-   [cURL](#curl_post_activity_user_group)
-   [JavaScript (HTML5)](#javascript_post_activity_user_group)
-   [Ruby](#ruby_post_activity_user_group)
-   [Node.js](#nodejs_post_activity_user_group)

<!-- -->

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups/mygroup/users/john.doe/activities" -d '{"actor":{"displayName":"John Doe","uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1","username":"john.doe",
    "image":{"duration":0,"height":80,"url":"http://www.gravatar.com/avatar/","width":80},
    "email":"john.doe@gmail.com"},"verb":"post","content":"Hello World!"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'POST',
        endpoint:'groups/mygroup/users/john.doe/activities',
        body:{"actor":
               {"displayName":"John Doe",
                "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
                "username":"john.doe",
                "image":
                 {"duration":0,
                  "height":80,
                  "url":"http://www.gravatar.com/avatar/",
                  "width":80},
                  "email":"john.doe@gmail.com"},
                 "verb":"post",
                 "content":"Hello World!"}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — POST worked. Data will contain raw results from API call.        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/mygroup/users/john.doe/activities'].post { actor:{ displayName: 'John Doe', uuid : '1f3567aa-da83-11e1-afad-12313b01d5c1', username: 'john.doe', image: { duration: 0, height: 80, url: 'http://www.gravatar.com/avatar/', width: 80 }, email: 'john.doe@gmail.com' }, verb: 'post', content: 'Hello World!' }

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'groups/mygroup/users/john.doe/activities',
        body:{"actor":
               {"displayName":"John Doe",
                "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
                "username":"john.doe",
                "image":
                 {"duration":0,
                  "height":80,
                  "url":"http://www.gravatar.com/avatar/",
                  "width":80},
                  "email":"john.doe@gmail.com"},
                 "verb":"post",
                 "content":"Hello World!"}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — POST worked. Data will contain raw results from API call.        
        }
    });

Because this relationship exists, this activity will appear in the feed
of all users who are members of mygroup. It won't appear in the feeds of
the group members’ followers or in feeds of users they follow.

### Example - Response

    {
      "action" : "post",
      "application" : "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params" : { },
      "path" : "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/activities",
      "uri" : "https://api.usergrid.com/my-org/my-app/
    /groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/activities",
      "entities" : [ {
        "uuid" : "2440ca58-49ff-11e2-84c0-02e81adcf3d0",
        "type" : "activity",
        "created" : 1355937094825,
        "modified" : 1355937094825,
        "actor" : {
          "displayName" : "John Doe",
          "uuid" : "1f3567aa-da83-11e1-afad-12313b01d5c1",
          "username" : "john.doe",
          "image" : {
            "duration" : 0,
            "height" : 80,
            "url" : "http://www.gravatar.com/avatar/",
            "width" : 80
          },
          "email" : "john.doe@gmail.com"
        },
        "content" : "Happy New Year!",
        "metadata" : {
          "path" : "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/users/34e26bc9-2d00-11e2-a065-02e81ae640dc/activities/2440ca58-49ff-11e2-84c0-02e81adcf3d0"
        },
        "published" : 1355937094825,
        "verb" : "post"
      } ],
      "timestamp" : 1355937094789,
      "duration" : 95,
      "organization" : "my-org",
      "applicationName" : "my-app"

Getting a user’s activities {dir="ltr"}
---------------------------

Use the GET method to retrieve a user’s activities. This returns the
activities posted on the user (that is, to
/users/{uuid|username}/activities), but not the activities of the people
that user follows. To retrieve the user’s activities and activities of
the users he follows, you need to get the user’s feed.

### Request URI

GET /{org\_id}/{app\_id}/users/{uuid|username}/activities

### Parameters

  Parameter                  Description
  -------------------------- ----------------------------------------
  arg uuid|string org\_id    Organization UUID or organization name
  arg uuid|string app\_id    Application UUID or application name
  arg uuid|string username   UUID or name of the user

### Example - Request

-   [cURL](#curl_get_user_activities)
-   [JavaScript (HTML5)](#javascript_get_user_activities)
-   [Ruby](#ruby_get_user_activities)
-   [Node.js](#nodejs_get_user_activities)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/users/john.doe/activities"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'users/john.doe/activities'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success GET worked. Data will contain raw results from API call.       
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['users/john.doe/activities'].get

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'users/john.doe/activities'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success GET worked. Data will contain raw results from API call.       
        }
    });

### Example - Response

    {
      "action" : "get",
      "application" : "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params" : { },
      "path" : "/users/5c0c1789-d503-11e1-b36a-12313b01d5c1/activities",
      "uri" : "https://api.usergrid.com/my-org/my-app/users/5c0c1789-d503-11e1-b36a-12313b01d5c1/activities",
      "entities" : [ {
        "uuid" : "d57e5b00-37f1-11e2-a0f7-02e81ae640dc",
        "type" : "activity",
        "created" : 1353952258301,
        "modified" : 1353952258301,
        "actor" : {
          "displayName" : "John Doe",
          "image" : {
            "duration" : 0,
            "height" : 80,
            "url" : "http://www.gravatar.com/avatar/",
            "width" : 80
          },
          "uuid" : "1f3567aa-da83-11e1-afad-12313b01d5c1",
          "email" : "john.doe@gmail.com",
          "username" : "john.doe"
        },
        "content" : "Hello World!",
        "metadata" : {
          "path" : "/users/5c0c1789-d503-11e1-b36a-12313b01d5c1/activities/d57e5b00-37f1-11e2-a0f7-02e81ae640dc"
        },
        "published" : 1353952258301,
        "verb" : "post"
      }, 
      "timestamp" : 1355933909077,
      "duration" : 39,
      "organization" : "my-org",
      "applicationName" : "my-app"}

Getting a group’s activities {dir="ltr"}
----------------------------

Use the GET method to retrieve a group’s activities. This returns the
activities created on or by the group (that is, to
/groups/{uuid|groupname}/activities), but not the activities of
followers of group members. To retrieve the group’s activities and
activities of followers, you need to get the group’s feed.

### Request URI

GET /{org\_id}/{app\_id}/groups/{uuid|groupname}/activities

### Parameters

  Parameter                   Description
  --------------------------- ----------------------------------------
  arg uuid|string org\_id     Organization UUID or organization name
  arg uuid|string app\_id     Application UUID or application name
  arg uuid|string groupname   UUID or name of the group

### Example - Request

-   [cURL](#curl_get_group_activities)
-   [JavaScript (HTML5)](#javascript_get_group_activities)
-   [Ruby](#ruby_get_group_activities)
-   [Node.js](#nodejs_get_group_activities)

<!-- -->

    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/mygroup/activities"

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var options = {
        method:'GET',
        endpoint:'groups/mygroup/activities'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success GET worked. Data will contain raw results from API call.       
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/mygroup/activities'].get

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'groups/mygroup/activities'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success GET worked. Data will contain raw results from API call.       
        }
    });

### Example - Response

    {
      "action" : "get",
      "application" : "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params" : { },
      "path" : "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/activities",
      "uri" : "https://api.usergrid.com/my-org/my-app//groups/d87edec7-fc4d-11e1-9917-12313d1520f1/activities",
      "entities" : [ {
        "uuid" : "e7a47a41-4310-11e2-8861-02e81adcf3d0",
        "type" : "activity",
        "created" : 1355175065939,
        "modified" : 1355175065939,
        "actor" : {
          "displayName" : "Martin Smith",
          "id" : "tag:example.org,2011:martin",
          "image" : {
            "duration" : 0,
            "height" : 250,
            "url" : "http://example.org/martin/image",
            "width" : 250
          },
          "objectType" : "person",
          "url" : "http://example.org/martin"
        },
        "metadata" : {
          "path" : "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/activities/e7a47a41-4310-11e2-8861-02e81adcf3d0"
        },
        "object" : {
          "id" : "tag:example.org,2011:abc123/xyz",
          "url" : "http://example.org/blog/2011/02/entry"
        },
        "published" : 1355175065939,
        "target" : {
          "url" : "http://example.org/blog/",
          "objectType" : "blog",
          "id" : "tag:example.org,2011:abc123",
          "displayName" : "Martin's Blog"
        },
        "verb" : "post"
      }, {
        "uuid" : "563f5d96-37f3-11e2-a0f7-02e81ae640dc",
        "type" : "activity",
        "created" : 1353952903811,
        "modified" : 1353952903811,
        "actor" : {
          "displayName" : "John Doe",
          "image" : {
            "duration" : 0,
            "height" : 80,
            "url" : "http://www.gravatar.com/avatar/",
            "width" : 80
          },
          "uuid" : "1f3567aa-da83-11e1-afad-12313b01d5c1",
          "email" : "john.doe@gmail.com",
          "username" : "john.doe"
        },
        "content" : "Hello World!",
        "metadata" : {
          "path" : "/groups/d87edec7-fc4d-11e1-9917-12313d1520f1/activities/563f5d96-37f3-11e2-a0f7-02e81ae640dc"
        },
        "published" : 1353952903811,
        "verb" : "post"
      } ],
      "timestamp" : 1355934203039,
      "duration" : 141,
      "organization" : "my-org",
      "applicationName" : "my-app"
    }

Getting a user’s feed {dir="ltr"}
---------------------

Use the GET method to retrieve a user’s feed.

### Request URI

GET /{org\_id}/{app\_id}/users/{uuid|username}/feed

### Parameters

  Parameter                  Description
  -------------------------- ----------------------------------------
  arg uuid|string org\_id    Organization UUID or organization name
  arg uuid|string app\_id    Application UUID or application name
  arg uuid|string username   UUID or name of the user

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
            //success GET worked. Data will contain raw results from API call.       
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['users/john.doe/feed'].get

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'users/john.doe/feed'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success GET worked. Data will contain raw results from API call.       
        }
    });

### Example - Response

    {
      "action" : "get",
      "application" : "5111c463-6a42-11e1-b6dd-1231380a0284",
      "params" : {
        "_" : [ "1346438183429" ]
      },
      "path" : "/users/1f3567aa-da83-11e1-afad-12313b01d5c1/feed",
      "uri" : "https://api.usergrid.com/5111c463-6a42-11e1-b6dd-1231380a0284/users/1f3567aa-da83-11e1-afad-12313b01d5c1/feed",
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
          "uuid" : "1f3567aa-da83-11e1-afad-12313b01d5c1",
          "email" : "john.doe@gmail.com",
          "username" : "john.doe"
        },
        "content" : "Hello World!",
        "metadata" : {
          "cursor" : "gGkAAQMAgGkABgE5ffM1aQCAdQAQ_9eWR_OZEeGuwxIxOwauAQCAdQAQABlaOvOaEeGuwxIxOwauAQA",
          "path" : "/users/1f3567aa-da83-11e1-afad-12313b01d5c1/feed/ffd79647-f399-11e1-aec3-12313b06ae01"
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
          "uuid" : "1f3567aa-da83-11e1-afad-12313b01d5c1",
          "email" : "massoddb@mfdsadfdsaoabl.com",
          "username" : "moab"
        },
        "content" : "checking in code left and right!!",
        "metadata" : {
          "cursor" : "gGkAAQMAgGkABgE5MLFh7gCAdQAQJIKhxefQEeGW9hIxOwbREgCAdQAQJNEP6ufQEeGW9hIxOwbREgA",
          "path" : "/users/1f3567aa-da83-11e1-afad-12313b01d5c1/feed/2482a1c5-e7d0-11e1-96f6-12313b06d112"
        },
        "published" : 1345141694958,
        "verb" : "post"
      } ],
      "timestamp" : 1346438331316,
      "duration" : 144,
      "organization": "my-org",
      "applicationName": "my-app"
    }

When a user creates an activity, a relationship is established between
the activity and the user who created it. The activities in the user’s
feed are based on this relationship as well as  any following
relationships that the user has, and any groups in which the user
belongs. So when a user asks to get his feed, what he gets is a list of
(1) all the activities that the user owns, (2) all the activities posted
by any users this user is following, and (3) any activities owned by any
groups in which this user belongs.

The user john.doe’s feed includes activities posted by user moab because
john.doe follows moab.

Getting a group’s feed {dir="ltr"}
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
            //error — GET failed
        } else {
            //success — GET worked. Data will contain raw results from API call.        
        }
    });

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app['groups/mygroup/feed'].get

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'groups/mygroup/feed'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — GET worked. Data will contain raw results from API call.        
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

Activity properties
-------------------

The following are the system-defined properties for activity entities.
You can create application-specific properties for an activity entity in
addition to the system-defined properties. The system-defined properties
are reserved. You cannot use these names to create other properties for
an activity entity. In addition the activities name is reserved for the
activities collection — you can't use it to name another collection.

  Property    Type             Description
  ----------- ---------------- ------------------------------------------------------------------------------------------------------------------------------------------
  uuid        UUID             Activity’s unique entity ID
  type        string           "activity"
  created     long             [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified    long             [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  actor       ActivityObject   Entity that performs the action of the activity (see [JSON Activity Streams 1.0 specification](http://activitystrea.ms/specs/json/1.0/))
  content     string           Description of the activity
  icon        MediaLink        Visual representation of a media link resource (see [JSON Activity Streams 1.0 specification](http://activitystrea.ms/specs/json/1.0/))
  category    string           Category used to organize activities
  verb        string           Action that the actor performs (for example, *post*)
  published   long             [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) when the activity was published
  object      ActivityObject   Object on which the action is performed (see [JSON Activity Streams 1.0 specification](http://activitystrea.ms/specs/json/1.0/))
  title       string           Title or headline for the activity

Set property 
-------------

Activities have the following set property.

  Set           Type     Description
  ------------- -------- -------------------------------------
  connections   string   Set of connections for the activity

 

Sample app
----------

The Messagee sample app is a simple Twitter-style messaging application
that leverages the activity stream functionality of App services. The
source for the application is available in HTML5 (JavaScript), iOS, and
Android. You can download the source from github at:

-   [https://github.com/apigee/usergrid-sample-html5-messagee](https://github.com/apigee/usergrid-sample-html5-messagee)
     (HTML5)
-   [https://github.com/apache/incubator-usergrid-sample-ios-messagee](https://github.com/apache/incubator-usergrid-sample-ios-messagee)
    (iOS)
-   [https://github.com/apigee/usergrid-sample-android-messagee](https://github.com/apigee/usergrid-sample-android-messagee)
    (Android)

