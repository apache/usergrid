# Retrieving connections

To see all of the connection types associated with an entity, simply retrieve the entity.

All of the connection types the entity has made to other entities will appear in the ``metadata.connections`` property.

All of the connection types that other entities have made to the entity will appear in the ``metadata.connecting`` property.

For user entities, following/followers connections can be accessed by sending a ``GET`` request to the URL in the ``collections.following`` and ``collections.follower``s properties.

Retrieve connected entities by connection type
To get a list of entities a specified entity has connected to with a specific connection type, do the following:

## Request syntax

    curl -X GET https://api.usergrid.com/<org>/<app>/<collection>/<entity>/<relationship>

Parameters

Parameter	    Description
---------       -----------
org	            Organization UUID or organization name
app	            Application UUID or application name
collection	    Name or UUID of the collection of the entity you want to retrieve the connections of.
entity	        Name or UUID of the entity whose connections you want to retrieve
relationship	The connection type you want to retrieve the entities for. 

For example, specifying a relationship of 'likes' would return a list of all entities that have the 'likes' connection with the specified entity.

Example request

    curl -X GET https://api.usergrid.com/your-org/your-app/users/Arthur/likes

Example response

Notice that the entities are returned as a JSON array in the entities property.

    {
      "action" : "get",
      "application" : "dk88fh4r-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes",
      "uri" : "https://api.usergrid.com/your-org/your-app/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes",
      "entities" : [ {
        "uuid" : "5bcc47ca-cfed-11e3-8bde-a7e008061e10",
        "type" : "user",
        "created" : 1398810410556,
        "modified" : 1398810410556,
        "username" : "Ford",
        "activated" : true,
        "metadata" : {
          "connecting" : {
            "likes" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/connecting/likes"
          },
          "path" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10",
          "sets" : {
            "rolenames" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/roles",
            "permissions" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/permissions"
          },
          "collections" : {
            "activities" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/activities",
            "devices" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/devices",
            "feed" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/feed",
            "groups" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/groups",
            "roles" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/roles",
            "following" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/following",
            "followers" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/followers"
          }
        }
      } ],
      "timestamp" : 1398884631067,
      "duration" : 41,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
		
## Retrieve all connected entities

To get a list of all the entities a specified entity has connected to, use the same method as shown above in Retrieve connected entities by connection type, and set the relationship to connections.

All of the entities that have made a connection of that type to the specified entity will be returned in the entities property of the response.

## Retrieve all connecting entities by type

To get a list of all the entities that have created a connection of a specific type to a specified entity, use the same method as shown above in Retrieve connected entities by connection type, and set the relationship to ``connecting/<relationship>``.

All of the entities that have made a connection to the specified entity will be returned in the entities property of the response.

## Retrieve all connecting entities

To get a list of all the entities that have connected to a specified entity, use the same method as shown above in Retrieve connected entities by connection type, and set the relationship to connecting.

All of the entities that have made a connection to the specified entity will be returned in the entities property of the response.