---
title: Using the API
category: docs
layout: docs
---

Using the API
=============

Apache Usergrid uses a pure REST API. (See [Representational State
Transfer](http://en.wikipedia.org/wiki/Representational_State_Transfer)
at Wikipedia for more information about the principles behind this type
of API.)

A REST API is built as a collection of resources. Resource locations are
described by paths that are related intrinsically to collections and
entities in collections. When building a REST API, the challenge is to
represent the data and the action upon the data as a path to a resource
that can be created, retrieved, updated, or deleted. The HTTP methods
POST, GET, PUT, and DELETE correspond to the actions that are applied to
resources.

In forming Apache Usergrid API requests, resource paths are specified as
URLs. For example, to cause the application my-app to retrieve data
about a user named john.doe, you construct the following API request:

    https://api.usergrid.com/my-org/my-app/users/john.doe

To get a listing of everything the user john.doe likes, use the
following URL:

    https://api.usergrid.com/my-org/my-app/users/john.doe/likes

To limit returned likes to entities of type restaurant, specify the
following URL:

    https://api.usergrid.com/my-org/my-app/users/john.doe/likes/restaurant

Basic API request construction
------------------------------

All Apache Usergrid API requests are made
to [https://api.usergrid.com](https://api.usergrid.com/).

Apache Usergrid interprets the URL resource path as a list of names, UUIDs,
or queries. The basic path format is:

    https://{hostname}/{org-uuid|org-name}/{app-uuid|app-name}/{collection-name}[/{entity-uuid|entity-name}]

where {hostname} is https://api.usergrid.com.

**Note:** You cannot mix UUIDs and names in the URL resource path. For
example, the following is incorrect:

    https://api.usergrid.com/my-org/62de5d97-d28c-11e1-8d5c-12313b01d5c1/users/john.doe

Either use names only in the URL resource path, like this:

    https://api.usergrid.com/my-org/my-app/users/john.doe

or UUIDs only.

This section gives several examples of how to construct Apache Usergrid API
requests. To focus on what's important, the examples use an abbreviated
path that starts after the application UUID, or application name. For
example, instead of giving a fully qualified path name as in:

    https://api.usergrid.com/my-org/my-app/users

the example simply lists this:

    /users

However, remember that in almost all cases, HTTP requests must include
the fully qualified URL, as well as an access token for authentication.

Accessing collections
---------------------

To access all entities in a collection, specify the path as follows:

    /users

Such a request retrieves the first 10 entities in the collection /users
sorted by their entity UUID.

Accessing entities
------------------

To access an entity in a collection, specify the path as follows:

    /{collection}/{uuid|name}

where {collection} is the collection name, and {uuid|name} is the
entity’s uuid or name.

To access a user in the users collection, specify the path as follows:

    /users/{uuid|username|email_address}

where {uuid|username|email\_address} is the user’s uuid, username, or
email address.

For example, the following request retrieves the entity named dino from
the dogs collection:

    /dogs/dino

Anytime a logged-in user makes a request, the user can substitute "me"
for the uuid or username. For example, the following request retrieves
the current user:

    /users/me

**Note:** The /users/me endpoint is accessible only if the user provides
an access token with the request (see [Authenticating users and
application clients](/authenticating-users-and-application-clients)). If
an access token is not provided with the request, that is, the user
makes an anonymous (or "guest") call, the system will not be able to
determine which user to return as /users/me.

Issuing queries
---------------

You can issue a query in an API request that retrieves items from a
collection. Here is the typical format for queries:

    /{collection}?ql={query}

where

{query} is a query in the query language.

For example, this request retrieves users whose Facebook first name is
john:

    /users?ql=select * where facebook.first_name ='john'

For further information about queries, see [Queries and
parameters](/queries-and-parameters).

Format of response data
-----------------------

All API methods return a response object that typically contains an
array of entities:

    {
      "entities" : [
        ...
      ]
    }

Not everything can be included inside the entity, and some of the data
that gets associated with specific entities isn't part of their
persistent representation. This is metadata, and it can be part of the
response as well as associated with a specific entity. Metadata is just
an arbitrary key/value JSON structure.

For example:

    {
      "entities" : {
        {
          "name" : "ed",
          "metadata" : {
            "collections" : ["activities", "groups", "followers"]
          }
        }
      },
      "metadata" : {
        "foo" : ["bar", "baz"]
      }
    }

Here's a full example of the response object with one entity in the
response (note that the Facebook property, which contains the entire
Facebook profile of the user, is not displayed in the example due to its
size):

    {
      "action" : "get",
      "application" : "ddde7630-90b1-11e0-b91b-12313f0204bb",
      "params" : { },
      "path" : "/users",
      "uri" : "https://api.usergrid.com/b91b-12313f0204bb-ddde7630-90b1-11e0/ddde7630-90b1-11e0-b91b-12313f0204bb/users",

      "entities" : [
        {
          "created" : 1307415547108000,
          "facebook" : { ... },
          "uuid" : "1c18ca40-90b2-11e0-b91b-12313f0204bb",
          "metadata" : {
            "path" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb",
            "collections" : {
              "activities" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/activities",
              "feed" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/feed",
              "groups" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/groups",
              "messages" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/messages",
              "queue" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/queue",
              "roles" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/roles",
              "following" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/following",
              "followers" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/followers"
            },
            "sets" : {
              "rolenames" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/rolenames",
              "permissions" : "/users/1c18ca40-90b2-11e0-b91b-12313f0204bb/permissions"
            }
          },
          "modified" : 1307415547108000,
          "name" : "John Doe",
          "picture" : "https://profile.ak.fbcdn.net/hprofile-ak-snc4/41501_217925_2656_q.jpg",
          "type" : "user",
          "username" : "john.doe"
        }
      ],
      "timestamp" : 1309218486419,
      "duration" : 40
    }
