
Usergrid Services Layer

This is where most of the high-level functionality for accessing Usergrid data
is performed. It functions as the glue between the REST API as exposed by
Jersey and the entity management functionality in usergrid-core. This is also
where security and account management functionality is contained.

The security model of Usergrid is implemented with Apache Shiro:

http://shiro.apache.org/

The services model for Usergrid involves converting the REST web service
requests into collection queries and routing these to a set of collection
handlers.  This provides a "virtual collection" layer on top of the actual
collections that are exposed in the persistence tier.  This makes it possible
to:
- implement logic behavior on top of persistent objects
- enforce object and property visibility
- perform fine-grained permission checks
