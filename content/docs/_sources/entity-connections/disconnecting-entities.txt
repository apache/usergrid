# Disconnecting entities

To disconnect a user from other data, perform a DELETE operation against the same endpoint at which you posted to create the connection.

## Request syntax

Disconnect by UUID

    curl -X DELETE https://api.usergrid.com/<org>/<app>/<connecting_collection>/<connecting_entity>/<relationship>/<connected_entity>
    
Disconnect by 'name' property

    curl -X DELETE https://api.usergrid.com/<org>/<app>/<connecting_collection>/<connecting_entity>/<relationship>/<connected_collection>/<connected_entity>
    
Parameters

Parameter	            Description
---------               -----------
org	                    Organization UUID or organization name
app	                    Application UUID or application name
connecting_collection	Name or UUID of the collection of the connecting entity.
connecting_entity	    Name or UUID of the connecting entity. 
relationship	        Type of connection being created (e.g., likes)
connected_collection	Name or UUID of the collection of the entity being connected to. 
connected_entity	    Name or UUID of the entity being connected to.

If the connecting entity is a 'user' entity, the 'username' should be used rather than the 'name'.

'connected_collection' is not required if the entity being connected to is specified by its UUID.Parameter	

Example request

    curl -X DELETE https://api.usergrid.com/your-org/your-app/users/Arthur/likes/users/Ford

Example response

    {
      "action" : "delete",
      "application" : "k88dh4f-a166-11e2-a7f7-02e81adcf3d0",
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
          "connections" : {
            "friends" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/friends",
            "likes" : "/users/58606d0a-cfed-11e3-a694-dbf5228024a7/likes/5bcc47ca-cfed-11e3-8bde-a7e008061e10/likes"
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
      "timestamp" : 1398962837195,
      "duration" : 85,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
	