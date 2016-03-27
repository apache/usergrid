# Using the API
Usergrid uses a pure REST (Representational State Transfer) API built as a collection of resources. Resource locations are described by paths that are related intrinsically to collections and entities in collections.

This section gives several examples of how to construct API requests. To focus on what's important, the examples use an abbreviated path that starts after the application UUID, or application name. For example, instead of giving a fully qualified path name as in:

    https://api.usergrid.com/your-org/your-app/users
   
the example simply lists this:

    /users
    
## Supported HTTP methods
When building a REST API, the challenge is to represent the data and the action upon the data as a path to a resource that can be created, retrieved, updated, or deleted. The HTTP methods POST, GET, PUT, and DELETE correspond to the actions that are applied to resources.

## Base URL
The base url for all requests made to Usergrid depends on where you have Usergrid installed. If you are using Apigee's trial Usergrid service, the base URL is ``https://api.usergrid.com.``

## Request construction
Usergrid interprets the URL resource path as a list of names, UUIDs, or queries. The basic path format is:

    https://api.usergrid.com/<org-uuid|org-name>/<app-uuid|app-name>/<collection-name>/<entity-uuid|entity-name>
    
Note: You cannot mix UUIDs and names in the URL resource path. For example, the following is incorrect:

    https://api.usergrid.com/your-org/62de5d97-d28c-11e1-8d5c-12313b01d5c1/users/john.doe
    
## Accessing collections
To access all entities in a collection, specify the path as follows:

    /users
    
Such a request retrieves the first 10 entities in the collection /users sorted by their entity UUID.

## Accessing entities
To access an entity in a collection, specify the path as follows:

    /<collection>/<uuid|name>

where ``<collection>`` is the collection name, and <uuid|name> is the entity’s uuid or name.

To access a user in the users collection, specify the path as follows:

    /users/<uuid|username|email_address>
    
where ``<uuid|username|email_address>`` is the user’s uuid, username, or email address.

For example, the following request retrieves the entity named dino from the dogs collection:

    /dogs/dino

## Issuing queries
You can issue a query in an API request that retrieves items from a collection. Here is the typical format for queries:

    /<collection>?ql=<query>

where <query> is a query in the query language.

For example, this request retrieves users whose Facebook first name is john:

    /users?ql=select * where facebook.first_name ='john'

For further information about queries, see [Querying your Data](../data-queries/querying-your-data.html)

## Authentication (OAuth)
Usergrid implements the OAuth 2.0 standard for authenticating users, clients and API requests.

Generally, you will generate a token for every user of your app by providing the user's username and password. The token can then be sent with all API requests to ensure each user is only able to access and modify the resources you have granted them rights to.

Note that by default access tokens are not needed to make requests to the default sandbox application in an organization.

For more information on generating and using access tokens, see Authenticating users and application clients and Authenticating API requests.

## Response format
All API methods return a response object that typically contains an array of entities:

    {
      "entities" : [
        ...
      ]
    }

Not everything can be included inside the entity, and some of the data that gets associated with specific entities isn't part of their persistent representation. This is metadata, and it can be part of the response as well as associated with a specific entity. Metadata is just an arbitrary key/value JSON structure.

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

For example, here is the response to a basic GET for a user entity:

    {
      "action" : "get",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/users",
      "uri" : "https://api.usergrid.com/your-org/your-app/users",
      "entities" : [ {
        "uuid" : "503f17da-ec39-11e3-a0dd-a554b7fbd57a",
        "type" : "user",
        "created" : 1401921665485,
        "modified" : 1401921665485,
        "username" : "someUser",
        "email" : "someUser@yourdomain.com",
        "activated" : true,
        "picture" : "http://www.gravatar.com/avatar/dc5d478e9c029853fbd025bed0dc51f8",
        "metadata" : {
          "path" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a",
          "sets" : {
            "rolenames" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/roles",
            "permissions" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/permissions"
          },
          "collections" : {
            "activities" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/activities",
            "devices" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/devices",
            "feed" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/feed",
            "groups" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/groups",
            "roles" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/roles",
            "following" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/following",
            "followers" : "/users/503f17da-ec39-11e3-a0dd-a554b7fbd57a/followers"
          }
        }
      } ],
      "timestamp" : 1401921673597,
      "duration" : 12,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }

## SDKs
To make the integration of Usergrid features into your application code quicker and easier, Usegrid offers SDKs in a variety of languages. The SDKs contain language-specific methods that allow you to issue API requests from your application code in your preferred language. SDKs are available for the following languages:

* iOS
* Android
* JavaScript/HTML5
* Node.js
* Ruby
* .NET

For more information, see SDKs.