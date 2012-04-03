Experimental and incomplete implementation of Mongo emulation layer so Mongo
clients can connect to Usergrid.

There are a number of drivers and tools for talking to MongoDB, which has a
data model that is very similar to that of Usergrid. In order to make it
easier for people to get started quickly with Usergrid, it's desirable for
them to be able to integrate it very quickly into their existing applications.
The goal here is to support the Mongo native wire protocol and to map enough
of the query and CRUD operations so that this can happen.

While Mongo is a full-featured database, in practice only a small subset of
it's commands are used. To verify this, a Mongo proxy was written at:

org.usergrid.mongo.testproxy.MongoProxyServer

This proxy serves as a "man in the middle" and decodes and logs all Mongo
commands sent to and from an application and MongoDB. Using a couple of GUI
tools as well as the Mongo command-line client, we were able to see that only
a handful of commands are commonly used for listing databases, collections,
and for querying content.

Mongo uses a wire protocol described here:

http://www.mongodb.org/display/DOCS/Mongo+Wire+Protocol

Mongo uses BSON, which is a binary JSON format, as part of the wire protocol.
For compatibility, we're using the BSON encoder from the Mongo Java driver:

https://github.com/mongodb/mongo-java-driver/

However, since we're using Jackson as our JSON library for the rest of
Usergrid, the idea is to switch over to Bson4Jackson:

https://github.com/michel-kraemer/bson4jackson

The commands Mongo supports are listed here:

http://www.mongodb.org/display/DOCS/List+of+Database+Commands

Mongo's system metadata is listed here:

http://www.mongodb.org/display/DOCS/Mongo+Metadata

Queries are described here:

http://www.mongodb.org/display/DOCS/Mongo+Query+Language


Mapping Usergrid Multi-tenancy to Mongo Security

First steps are to simply be able to handle a client login and map it to a
Usergrid account.

Authentication is described here:

http://www.mongodb.org/display/DOCS/Security+and+Authentication
http://www.mongodb.org/display/DOCS/Implementing+Authentication+in+a+Driver

A user who authenticates against the Mongo 'admin' account will be actually
logging in as a Usergrid admin user who is a member of one or more Usergrid
accounts, each of which contains a set of applications that user is able to
administer.  While they list the databases via the Mongo API, what they will
be seeing is the aggregate list of the applications for all the accounts they
are members of.

For any database (Usergrid application), they'll be able to list the collections.

Note: Internally, Mongo refers to it's collections as "namespaces", which
is a potential point of confusion between previous versions of Usergrid which
used the term "namespace" to describe applications.


