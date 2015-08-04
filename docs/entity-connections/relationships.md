# Relationships

## Creating connections between entities

One of the most useful features of Usergrid is the ability to create
connections between entities. A simple example of this is the
Twitter-like use of *following*, where one user forms a connection with
another by subscribing to any tweets they post. [Messagee
Example](/messagee-example) walks you through an example of following
other users in our sample app, *Messagee*. Here is the basic format:

    POST https://api.usergrid.com/my-org/my-app/users/fred/following/users/barney

This API call results in two users, Fred and Barney, linked with a
connection where Fred is following Barney.

If you create a *following* connection between two users, Apache Usergrid
automatically creates a virtual connection called *followers* that
mirrors the *following* connection. In other words, if you create a
connection where Fred is following Barney, Apache Usergrid automatically
creates a virtual connection where Fred is a follower of Barney.

Note that there is no mirror connection established. Apache Usergrid only
creates a mirror connection when you create a *following* connection. It
does not create a mirror connection for other verbs such as likes.

You can see all the users that Fred is following, in this case only
Barney, by making the following API call:

    GET https://api.usergrid.com/my-org/my-app/users/fred/following

You can see all of barney’s followers, in this case only Fred, by making
the following API call:

    GET https://api.usergrid.com/my-org/my-app/users/barney/followers

The *followers* connection is a virtual connection because you can’t use
it to link two entities. In other words, you can’t make fred a follower
of barney by using a *followers* connection.  **This is wrong:**

    POST https://api.usergrid.com/my-org/my-app/users/barney/followers/users/fred

To create a *following* connection with the users switched, so that
Barney is following Fred, do this:

    POST https://api.usergrid.com/my-org/my-app/users/barney/following/users/fred

You can now see Fred’s followers (only Barney) by making the following
call:

    GET https://api.usergrid.com/my-org/my-app/users/fred/followers

## Creating other connections

You can extend this connection structure to create connections using any
"verb" that can link two entities. For example, you could use likes to
denote a connection between a user and his dog. First, create a dogs
collection:

    POST https://api.usergrid.com/my-org/my-app/dogs

Then populate this collection with a new dog named Dino:

    POST https://api.usergrid.com/my-org/my-app/dogs {"name" : "dino"}

Then create a likes connection between Fred and his dog Dino:

    POST https://api.usergrid.com/my-org/my-app/users/fred/likes/dogs/dino

Getting connections
-------------------

### Get all connections for an entity

To get a list that only contains the connections, do a GET on the
connections sub-property of the entity:

    GET https://api.usergrid.com/my-org/my-app/users/fred/connections

### Get information on a specific connection type

To get a list of users who like Fred:

    GET https://api.usergrid.com/my-org/my-app/users/fred/connecting/likes

To get a list of all dogs that Fred likes:

    GET https://api.usergrid.com/my-org/my-app/users/fred/likes/dog

Deleting a connection
---------------------

You can delete a connection in a way similar to creating one. Just
replace the POST method with the DELETE method. For example, you can
delete the connection between fred and barney with the following API
call:

    DELETE https://api.usergrid.com/my-org/my-app/users/fred/following/barney
