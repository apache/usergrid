# Usergrid Data model

Usergrid models the data for your apps as application-specific collections of data entities managed within an organization. The following is an overview of the component hierarchy that makes up the data model, and is intended to help you understand how data is stored, managed and accessed.

## Organizations
An organization contains one or more applications, and represents administrator-level access. Multiple accounts may be given administrator access to an organization. Accounts can also be members of multiple organizations.

An organization with the same name as your username is automatically created for you when you sign up. By default, you are assigned as the administrator of this organization.

## Applications
In Usergrid, you can create one or more applications within an organization. Applications represent an instance of application data associated with an app, and you may create as many applications as you wish. This allows you to utilize the backend in a way that corresponds to your development process. For example, you might create separate applications for development and production instances of your app. By default, all organization have a sandbox application (see Using a [Sandbox Application](../getting-started/using-a-sandbox-app.html) for important information regarding the default sandbox application).

Each application provides the infrastructure for storing, retrieving, updating and deleting the entities and collections associated with a specific app instance.

## Collections
Usergrid stores all data entities in uniquely-named collections. Collections are created automatically for every entity type, including custom entities, and are named using the plural form of the entity type they store. For example, all user entities are stored in the /users collection, and all device entities in the /devices collection. An entity can belong to only one collection.

Currently, collections cannot be renamed or deleted; however, all of the data entities in a collection can be updated or deleted.

## Entities
An entity represents a basic, JSON-formatted data object that is used by your app, such as a user, device, event or asset. Unlike records in conventional database tables, which have a rigid schema that defines what they can store and how they can be related to each other, Usergrid entities are very flexible. This makes Usergrid a powerful solution for managing data for modern applications, where people, places, and content often need to be associated in a way that is most appropriate from a user perspective.

Here is a simple example of an entity:

    {
      "uuid" : "5c0c1789-d503-11e1-b36a-12313b01d5c1",
      "type" : "user",
      "created" : 1343074620374,
      "modified" : 1355442681264,
      "username" : "john.doe",
      "email" : "jdoe57@mail.com",
      "name" : "John Doe"
    }
    
## Default entities
The following entity types are predefined in Usergrid. For more details, see the [API Reference](../rest-endpoints/api-docs.html)

* [user](../rest-endpoints/api-docs.html#user)
* [group](../rest-endpoints/api-docs.html#group)
* [role](../rest-endpoints/api-docs.html#role)
* [application](../rest-endpoints/api-docs.html#application)
* [activity](../rest-endpoints/api-docs.html#activity)
* [device](../rest-endpoints/api-docs.html#device)
* [asset](../rest-endpoints/api-docs.html#asset)
* [folder](../rest-endpoints/api-docs.html#folder)
* [event](../rest-endpoints/api-docs.html#event)
* [notifier](../rest-endpoints/api-docs.html#notifier)
* [notification](../rest-endpoints/api-docs.html#notification)
* [receipt](../rest-endpoints/api-docs.html#recept)

## Properties

A data entity is a set of properties, which can each contain any JSON-representable value, including a nested JSON document. All entities have predefined properties, but you are free to define any number of custom properties for any entity. Default properties require specific data types for validation purposes, while custom properties can be any JSON data type. Most predefined and all application-defined entity properties are indexed, allowing you to query collections quickly and easily.

Individual or multiple properties can be updated in a single operation; however, partial updating of nested JSON documents is not supported. This means that all properties of a nested document must be provided in a PUT request for the update of the nested document to be processed, even if the some of the values have not changed.

## Default properties
At a minimum, each entity is defined by two properties, both of which are strings: type and UUID. The entity 'type' is the singular form of the collection the entity is stored in. For example, an entity in the 'users' collection has an entity type of 'user'. The entity 'UUID' is an immutable universally unique identifier, which can be used to reference the entity. A UUID is automatically generated for every entity when it is created. You can also create custom entities and entity properties; however, Usergrid reserves certain entity types with pre-defined properties by default. For a complete list of reserved entities and properties, see Default Data Entity Types.

When you access the system via the API, you’ll always provide your organization UUID or name, application UUID or name, and typically the UUID or name of the entity you’re modifying or retrieving.

All entities have the following default properties:

+------------+--------+---------------------------------------------------------------------+
| Property   | Type   | Description                                                         |
+------------+--------+---------------------------------------------------------------------+
| uuid       | UUID   | Entity unique id                                                    |
+------------+--------+---------------------------------------------------------------------+
| type       | string | entity type (for example, user)                                     |
+------------+--------+---------------------------------------------------------------------+
| created    | long   | UTC timestamp in milliseconds of when the entity was created        |
+------------+--------+---------------------------------------------------------------------+
| modified   | long   | UTC timestamp in milliseconds of when the entity was last modified  |
+------------+--------+---------------------------------------------------------------------+

Custom entities also have an optional name property that is a string identifier.
