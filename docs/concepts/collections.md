# Collections

In Apache Usergrid, all entities are automatically associated with a corresponding
collection based on the `type` property of the entity. You may create
empty collections if you wish, but creating an entity of a new type will
automatically create a corresponding collection for you. For example,
creating a new custom "item" entity, creates an "items" collection.

Queries are always limited in scope to the current collection. That should be your primary consideration for data modeling in Apache Usergrid.

The following collections are reserved in the system

* users
* groups
* activities
* devices
* assets
* folders
* queues
* events
* counters