# Activity

Most modern applications struggle to manage data streams, such as those
that contain an ongoing list of comments, activities, and tweets. In
particular, mobile applications are prone to generating very large
amounts of data in a data stream. Beyond that, additions to a data
stream must often be routed automatically to subscribers or filtered or
counted.

Usergrid provides an activity entity that is specifically designed
for data streams. An activity is an entity type that represents activity
stream actions (see the [JSON Activity Streams 1.0
specification](http://activitystrea.ms/specs/json/1.0/) for more
information about these actions).

When a user creates an activity, it creates a relationship between the
activity and the user who created it. Because this relationship exists,
the activity will appear in the feed of any of the user’s followers.
Think of the Activities endpoint (``/users/{uuid|username}/activities``) as
an "outbox" of news items created by the user. Think of the Feed
endpoint (``/users/{uuid|username}/feed``) as an "inbox" of news items meant
to be seen or consumed by the user.

A user can also post an activity to a group (located at
``/groups/{uuid|groupname}/activities``). This allows you to emulate
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

Using Usergrid APIs you can create, retrieve, update, and delete
activity entities. 

__Note:__ Although not shown in the API examples below, you need to
provide a valid access token with each API call. See 
[Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.


## Posting activities

Posting a user activity

Use the  POST method to create an activity in the activities collection.

### Request URI

    POST /<org_id>/<app_id>/users/<uuid | username>/activities { request body }

Parameters

Parameter	            Description
---------               -----------
arg uuid|string org_id	Organization UUID or organization name
arg uuid|string app_id	Application UUID or application name
request body	        One or more sets of activity properties

Here's an example request body:

    {
        "actor":{
            "displayName":"John Doe",
            "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
            "username":"john.doe",
            "image":{
                "duration":0,
                "height":80,
                "url":"http://www.gravatar.com/avatar/","width":80},
                "email":"john.doe@gmail.com"
            },
            "verb":"post",
            "content":"Hello World!"
        }
    }
    
### Example - Request

    curl -X POST "https://api.usergrid.com/my-org/my-app/users/john.doe/activities" -d '{"actor":{"displayName":"John Doe","uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1","username":"john.doe", "image":{"duration":0,"height":80,"url":"http://www.gravatar.com/avatar/","width":80}, "email":"john.doe@gmail.com"},"verb":"post","content":"Hello World!"}'
    
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

__Note__: Anytime a logged-in user makes a request, you can substitute "me" for the uuid or username. So the format of a request to create an activity for the currently logged-in user would look like this:

    POST /<org_id>/<app_id>/users/me/activities { request body }

The ``/users/me`` endpoint is accessible only if you provide an access token with the request. If you don't provide an access token with the request, that is, you make an anonymous (or "guest") call, the system will not be able to determine which user to return as ``/users/me``.

When you create an activity it creates a relationship between the activity and the user who created it. In other words, the newly created activity above belongs to john.doe. Another way of saying this is the user "owns" the activity. And because this relationship exists, the activity will appear in the feed of any of the user’s followers (in this example, anyone who is following john.doe). However, it will not appear in the feed of people the user follows. The activity is accessible at the ``/activities`` endpoint to users who have the permission to read that endpoint.

Notice the properties specified in the request body in the previous example are actor, verb, and content. The actor, verb, and content properties are built into the Activity entity (see [Activity entity properties](../rest-endpoints/api-doc.html#activity) ). The actor property specifies properties of the entity that performs the action (here, user john.doe). The gravatar URL is used to create an icon for the activity. And because an Activity is simply an API Services data entity, you can also create custom properties.

The verb parameter is descriptive. You can use it to indicate what type of activity is posted, for example, an image versus text. The value post is defined in the JSON Activity Streams specification as “the act of authoring an object and then publishing it online.“

## Posting an activity to a group

Use the POST method to post an activity to a specific group. In this case the activity is created in the activities collection and is accessible at the ``/activities`` endpoint to users who have the permission to read that endpoint. In addition, a relationship is established between the activity and the group, and because of that, the activity will appear in the group’s feed. The group "owns" the activity. Also, the activity will be published in the feed of all users that are members of the group.

### Request URI

    POST /{org_id}/{app_id}/groups/{uuid|groupname}/activities {request body}

Parameters

Parameter	               Description
---------                  -----------
arg uuid|string org_id	   Organization UUID or organization name
arg uuid|string app_id	   Application UUID or application name
arg uuid|string groupname  UUID or name of the group
request body	           One or more sets of activity properties.

Here's a sample request body:

    {
      "actor":
        {
        "displayName":"John Doe",
        "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
        "username":"john.doe",
        "image":{
          "duration":0,
          "height":80,
          "url":"http://www.gravatar.com/avatar/","width":80},
      "email":"john.doe@gmail.com"},
      "verb":"post",
      "content":"Hello World!"    
    }
    
### Example - Request

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups/mygroup/activities" -d '{"actor":{"displayName":"John Doe","uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1","username":"john.doe", "image":{"duration":0,"height":80,"url":"http://www.gravatar.com/avatar/","width":80}, "email":"john.doe@gmail.com"},"verb":"post","content":"Hello World!"}'
    
Because this relationship exists, this activity will appear in the feed of all users who are members of mygroup. It won't appear in the feeds of the group members’ followers or in feeds of users they follow.

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
    }

## Creating an activity for a user's followers in a group

Use the POST method to create an activity that will be published only in the feeds of users who (1) follow you, and (2) are in the same group to which you posted the activity. This is useful if you want to create specific groups of friends (for example, acquaintances or colleagues) and publish content to them with more precise privacy settings. This allows you to re-create a privacy model similar to Google+’s Circles or Facebook current privacy system.

When you create an activity for a user’s followers in a group:

The activity is accessible at the ``/activities`` endpoint to users who have the permission to read that endpoint. The activity will not be cross-posted to the group’s activity endpoint (``/groups/{uuid|groupname}/activities``)
A relationship is automatically created between the activity entity that was just created and the user within that group (``/groups/{uuid|groupname}/users/{uuid|username}``)
The user within the group (``/groups/{uuid|groupname}/users/{uuid|username}``) becomes the owner of the activity (through the owner property in the activity).

### Request URI

    POST /{org_id}/{app_id}/groups/{uuid|groupname}/users/{uuid|username}/activities {request body}

Parameters

Parameter	                Description
arg uuid|string org_id	    Organization UUID or organization name
arg uuid|string app_id	    Application UUID or application name
arg uuid|string groupname	UUID or name of the group
arg uuid|string username	UUID or name of the user
request body	            One or more sets of activity properties

Example request body:

    {
      "actor":
        {
        "displayName":"John Doe",
        "uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1",
        "username":"john.doe",
        "image":{
          "duration":0,
          "height":80,
          "url":"http://www.gravatar.com/avatar/","width":80},
      "email":"john.doe@gmail.com"},
      "verb":"post",
      "content":"Hello World!"    
    }
    
### Example - Request

    curl -X POST "https://api.usergrid.com/my-org/my-app/groups/mygroup/users/john.doe/activities" -d '{"actor":{"displayName":"John Doe","uuid":"1f3567aa-da83-11e1-afad-12313b01d5c1","username":"john.doe", "image":{"duration":0,"height":80,"url":"http://www.gravatar.com/avatar/","width":80}, "email":"john.doe@gmail.com"},"verb":"post","content":"Hello World!"}'
    
Because this relationship exists, this activity will appear in the feed of all users who are members of mygroup. It won't appear in the feeds of the group members’ followers or in feeds of users they follow.

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
    }

## Retrieving activity feeds

Retrieving a user's activity feed

Use the GET method to retrieve a user’s feed.

### Request URI

    GET /{org_id}/{app_id}/users/{uuid|username}/feed

### Example - Request

    curl -X GET "https://api.usergrid.com/my-org/my-app/users/john.doe/feed"
    
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

## Retrieving a group's activity feed

Use the GET method to retrieve the feed for a group. This gets a list of all the activities that have been posted to this group, that is, the activities for which this group has a relationship (owns).

### Request URI

    GET /{org_id}/{app_id}/groups/{uuid|groupname}/feed
    
Parameters

Parameter	Description
---------   -----------
arg uuid|string org_id	   Organization UUID or organization name
arg uuid|string app_id	   Application UUID or application name
arg uuid|string groupname  UUID or name of the group

### Example - Request

    curl -X GET "https://api.usergrid.com/my-org/my-app/groups/mygroup/feed"
    
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
    }  
  