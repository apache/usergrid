# Social Graph Connections

One of the most useful features of Usergrid is the ability to create connections between entities, which allow you to model arbitrary relationships between entities. This feature is particularly powerful when applied to user entities by allowing you to model complex social graphs between users as well as groups of users.

## Following/followers
To make the social graph possibilities of entity connections even easier to achieve, Usergrid also has special support for a default following/followers relationship, which offers these additional features:

Reciprocal connection: If a following connection is made between a user and another user, a reciprocal followers relationship will be created automatically. In contrast, all of other entity connections are one-way, meaning any reciprocal relationship must be created manually.

Activity feed subscription: The followed user's activities will automatically be posted to the following user's activity feed. For example, if Arthur is following Ford, then any activities published by Ford that Arthur is allowed to see will appear in Arthur's activity feed.

## Creating a following/followers connection
To create a following/followers connection between two entities, create the connection as you would any generic entity connection. For full details on creating connections, see [Connecting entities](../entity-connections/connecting-entities.html). 

For example, the following request would create a following/followers relationship between two user entities with the usernames 'Fred' and 'Barney':

    POST https://api.usergrid.com/your-org/your-app/users/barney/following/users/fred

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last">
Please note that this only works when you ``POST`` a ``following`` connection. Creating a follower connection would not create a reciprocal following connection.
</p></div>

This would retrieve a list of the users that Barney is following:

    GET https://api.usergrid.com/your-org/your-app/users/barney/following
    
And this would retrieve a list of users that are following Fred:

    GET https://api.usergrid.com/your-org/your-app/users/fred/followers
    
# Creating other connections
You can extend this connection structure to create connections using any relationship. For example, you could use likes to denote a connection between a user and his dog with this POST:

    POST https://api.usergrid.com/your-org/your-app/users/Fred/likes/dogs/Dino
    
Note that in this case a reciprocal connection is not automatically created. To do so you would need to manually create the reciprocal connection with another POST such as:

    POST https://api.usergrid.com/your-org/your-app/dogs/Dino/liked_by/users/Fred
    
For more information on using entity connections, see [Connecting entities](../entity-connections/connecting-entities.html).
