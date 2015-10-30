# Connecting entities

When creating a connection, if you specify the collection of the entity being connected to, you can create the connection using the value of its 'name' property or its UUID.

## Request syntax

To create a connection, the entity being connected to can either be specified by just its UUID, or both its collection and the value of its 'name' property.

Connect by UUID

    curl -X POST https://api.usergrid.com/<org>/<app>/<connecting_collection>/<connecting_entity>/<relationship>/<connected_entity>
    
Connect by 'name' property

    curl -X POST https://api.usergrid.com/<org>/<app>/<connecting_collection>/<connecting_entity>/<relationship>/<connected_collection>/<connected_entity>

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

'connected_collection' is not required if the entity being connected to is specified by its UUID.

Example request

    curl -X POST http://api.usergrid.com/your-org/your-app/users/Arthur/likes/6c56ffda-9e75-11e3-99fd-8dd1801e534c

Example Response

    {
        "action" : "post",
        "application" : "db1e60a0-417f-11e3-9586-0f1ff3650d20",
        "params" : { },
        "path" : "/users/174785aa-8ea8-11e3-ae1f-eb20e5bce407/likes",
        "uri" : "https://api.usergrid.com/my-org/my-app/users/174785aa-8ea8-11e3-ae1f-eb20e5bce407/likes",
        "entities" : [ {
            "uuid" : "6c56ffda-9e75-11e3-99fd-8dd1801e534c",
            "type" : "user",
            "name" : "Arthur",
            "created" : 1393371291725,
            "modified" : 1393371291725,
            "metadata" : {
                "path" : "/users/174785aa-8ea8-11e3-ae1f-eb20e5bce407/likes/6c56ffda-9e75-11e3-99fd-8dd1801e534c"
            }
        } ],
        "timestamp" : 1393371455487,
        "duration" : 77,
        "organization" : "your-org",
        "applicationName" : "your-app"
    }
	