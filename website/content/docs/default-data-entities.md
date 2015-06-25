---
title: Default Data Entities
category: docs
layout: docs
---

Default Data Entities
=====================

This following describes all of the default data entities available in
Apache Usergrid, and their default properties. Each entity can be accessed
in a corresponding collection, named with the plural form of the entity
name (see [Apache Usergrid Data model](/app-services-data-model-1) for more
information on how data is stored in Apache Usergrid).

Properties marked 'required' must be set for the entity to be
successfully created. Properties marked 'optional' are reserved by the
API but not required. All other properties are automatically set and
returned by the API when the entity is created.

For information on creating custom entities or custom properties for any
entity, see You do not have access to view this node.

Activity
--------

The *activity* entity represents a user activity, and is specifically
designed for use in data streams as defined by the [JSON Activity
Streams 1.0 specification](http://activitystrea.ms/specs/json/1.0/).

For more information on using the activity entity, see
[Activity](/activity).

### Properties

The following are the system-defined properties for activity entities:

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case "activity"         |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| actor                   | ActivityObject          | **Required.** Entity    |
|                         |                         | that performed the      |
|                         |                         | 'action' of the         |
|                         |                         | activity (see [JSON     |
|                         |                         | Activity Streams 1.0    |
|                         |                         | specification](http://a |
|                         |                         | ctivitystrea.ms/specs/j |
|                         |                         | son/1.0/)).             |
|                         |                         | By default, the UUID of |
|                         |                         | the user who performed  |
|                         |                         | the action is recorded  |
|                         |                         | as the value of the     |
|                         |                         | 'uuid' property of this |
|                         |                         | object.                 |
+-------------------------+-------------------------+-------------------------+
| verb                    | string                  | **Required.** The       |
|                         |                         | action performed by the |
|                         |                         | user (for example,      |
|                         |                         | *post*)                 |
+-------------------------+-------------------------+-------------------------+
| published               | long                    | **Required.** [UTC      |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the activity was        |
|                         |                         | published               |
+-------------------------+-------------------------+-------------------------+
| content                 | string                  | *Optional.* Description |
|                         |                         | of the activity         |
+-------------------------+-------------------------+-------------------------+
| icon                    | MediaLink               | *Optional.* Visual      |
|                         |                         | representation of a     |
|                         |                         | media link resource     |
|                         |                         | (see [JSON Activity     |
|                         |                         | Streams 1.0             |
|                         |                         | specification](http://a |
|                         |                         | ctivitystrea.ms/specs/j |
|                         |                         | son/1.0/))              |
+-------------------------+-------------------------+-------------------------+
| category                | string                  | *Optional.* Category    |
|                         |                         | used to organize        |
|                         |                         | activities              |
+-------------------------+-------------------------+-------------------------+
| verb                    | string                  | *Optional.* Action that |
|                         |                         | the actor performs (for |
|                         |                         | example, *post*)        |
+-------------------------+-------------------------+-------------------------+
| published               | long                    | *Optional.* [UTC        |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds when    |
|                         |                         | the activity was        |
|                         |                         | published               |
+-------------------------+-------------------------+-------------------------+
| object                  | ActivityObject          | *Optional.* Object on   |
|                         |                         | which the action is     |
|                         |                         | performed (see [JSON    |
|                         |                         | Activity Streams 1.0    |
|                         |                         | specification](http://a |
|                         |                         | ctivitystrea.ms/specs/j |
|                         |                         | son/1.0/))              |
+-------------------------+-------------------------+-------------------------+
| title                   | string                  | *Optional.* Title or    |
|                         |                         | headline for the        |
|                         |                         | activity                |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | activity entity, as     |
|                         |                         | well as additional data |
|                         |                         | entities associated     |
|                         |                         | with the activity. The  |
|                         |                         | following properties    |
|                         |                         | are included in         |
|                         |                         | metadata:               |
|                         |                         |                         |
|                         |                         | **path**: Path to       |
|                         |                         | retrieve the activity   |
|                         |                         | entity, including the   |
|                         |                         | UUID of the user entity |
|                         |                         | associated with the     |
|                         |                         | activity and the UUID   |
|                         |                         | of the activity entity  |
+-------------------------+-------------------------+-------------------------+

Application
-----------

The *application* entity is the base entity for accessing your
application data in Apache Usergrid. Aside from creating the application
entity, most apps using Apache Usergrid will never need to access the
application entity directly; however, you can add custom entities or
make changes to the default properties of the entity for configuration
purposes via the Apache Usergrid.

For more information on creating and configuring applications in App
Services, see [Creating a New Application with the Admin
Console](/creating-new-application-admin-console).

### Properties

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | String                  | Type of entity, in this |
|                         |                         | case "application"      |
+-------------------------+-------------------------+-------------------------+
| name                    | string                  | *Optional.* Application |
|                         |                         | name                    |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| accesstokenttl          | long                    | *Optional.* Time to     |
|                         |                         | live value for an       |
|                         |                         | access token obtained   |
|                         |                         | within the application  |
+-------------------------+-------------------------+-------------------------+
| organizationName        | string                  | Name of the             |
|                         |                         | organization the        |
|                         |                         | application belongs to  |
+-------------------------+-------------------------+-------------------------+
| applicationName         | string                  | Name of the application |
+-------------------------+-------------------------+-------------------------+
| title                   | string                  | *Optional.* Application |
|                         |                         | title                   |
+-------------------------+-------------------------+-------------------------+
| description             | string                  | *Optional.* Application |
|                         |                         | description             |
+-------------------------+-------------------------+-------------------------+
| activated               | boolean                 | *Optional.* Whether     |
|                         |                         | application is          |
|                         |                         | activated               |
+-------------------------+-------------------------+-------------------------+
| disabled                | boolean                 | *Optional.* Whether     |
|                         |                         | application is          |
|                         |                         | administratively        |
|                         |                         | disabled                |
+-------------------------+-------------------------+-------------------------+
| allowOpenRegistration   | boolean                 | *Optional.* Whether     |
|                         |                         | application allows any  |
|                         |                         | user to register        |
+-------------------------+-------------------------+-------------------------+
| registrationRequiresEma | boolean                 | *Optional.* Whether     |
| ilConfirmation          |                         | registration requires   |
|                         |                         | email confirmation      |
+-------------------------+-------------------------+-------------------------+
| registrationRequiresAdm | boolean                 | *Optional.* Whether     |
| inApproval              |                         | registration requires   |
|                         |                         | admin approval          |
+-------------------------+-------------------------+-------------------------+
| notify\_admin\_of\_new\ | boolean                 | *Optional.* Whether     |
| _users                  |                         | application admins      |
|                         |                         | should be notified of   |
|                         |                         | new users               |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides a         |
|                         |                         | 'collections' object    |
|                         |                         | with the relative paths |
|                         |                         | to all entity           |
|                         |                         | collections associated  |
|                         |                         | with the application.   |
|                         |                         | The following           |
|                         |                         | collections are         |
|                         |                         | included in metadata by |
|                         |                         | default:                |
|                         |                         |                         |
|                         |                         | -   **users**: Path to  |
|                         |                         |     retrieve the /users |
|                         |                         |     collection          |
|                         |                         | -   **groups**: Path to |
|                         |                         |     retrieve the        |
|                         |                         |     /groups collection  |
|                         |                         | -   **folders**: Path   |
|                         |                         |     to retrieve the     |
|                         |                         |     /folders collection |
|                         |                         | -   **events**: Path to |
|                         |                         |     retrieve the        |
|                         |                         |     /events collection  |
|                         |                         | -   **assets**: Path to |
|                         |                         |     retrieve the        |
|                         |                         |     /assets collection  |
|                         |                         | -   **activities**:     |
|                         |                         |     Path to retrieve    |
|                         |                         |     the /activities     |
|                         |                         |     collection          |
|                         |                         | -   **devices**: Path   |
|                         |                         |     to retrieve the     |
|                         |                         |     /devices collection |
|                         |                         | -   **notifiers**: Path |
|                         |                         |     to retrieve the     |
|                         |                         |     /notifiers          |
|                         |                         |     collection          |
|                         |                         | -   **notifications**:  |
|                         |                         |     Path to retrieve    |
|                         |                         |     the /notifications  |
|                         |                         |     collection          |
|                         |                         | -   **receipts**: Path  |
|                         |                         |     to retrieve the     |
|                         |                         |     /receipts           |
|                         |                         |     collection          |
+-------------------------+-------------------------+-------------------------+

Asset
-----

The *asset* entity represents a binary data object stored in App
Services infrastructure, such as an image, video or audio file. The
asset entity does not contain the binary data, but rather contains
information about the data and points to the location where it can be
accessed in Apache Usergrid infrastructure.

For more information on using the asset entity, see [Assets](/assets).

### Properties

The following are the system-defined properties for asset entities:

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case"asset"             |
+-------------------------+-------------------------+-------------------------+
| name                    | string                  | *Optional.* Asset name  |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| owner                   | UUID                    | **Required.** UUID of   |
|                         |                         | the user entity that    |
|                         |                         | owns the asset          |
+-------------------------+-------------------------+-------------------------+
| path                    | string                  | **Required.** Relative  |
|                         |                         | path to the asset       |
+-------------------------+-------------------------+-------------------------+
| content-type            | string                  | MIME media type that    |
|                         |                         | describes the asset     |
|                         |                         | (see [media             |
|                         |                         | types](http://www.iana. |
|                         |                         | org/assignments/media-t |
|                         |                         | ypes))                  |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | asset entity, as well   |
|                         |                         | as additional data      |
|                         |                         | entities associated     |
|                         |                         | with the asset. The     |
|                         |                         | following properties    |
|                         |                         | are included in         |
|                         |                         | metadata:               |
|                         |                         |                         |
|                         |                         | -   **path**: Path to   |
|                         |                         |     retrieve the asset  |
|                         |                         |     entity              |
+-------------------------+-------------------------+-------------------------+

Device
------

The *device* entity represents a unique device that is being used to
access your app. Device entities should be associated with a user
entity. The Apache Usergrid push notification feature requires the device
entity.

For more information on using the device entity, see [Device](/device).

### Properties

The following are the system-defined properties for device entities:

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case "device"           |
+-------------------------+-------------------------+-------------------------+
| name                    | string                  | *Optional.* Device name |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | device entity, as well  |
|                         |                         | as additional data      |
|                         |                         | entities associated     |
|                         |                         | with the user. The      |
|                         |                         | following properties    |
|                         |                         | are included in         |
|                         |                         | metadata:               |
|                         |                         |                         |
|                         |                         | **path**: Path to       |
|                         |                         | retrieve the device     |
|                         |                         | entity, including the   |
|                         |                         | device UUID             |
|                         |                         |                         |
|                         |                         | **collections**: Nested |
|                         |                         | object that contains    |
|                         |                         | paths to data entity    |
|                         |                         | collections associated  |
|                         |                         | with the device.        |
|                         |                         |                         |
|                         |                         | -   receipts: Receipt   |
|                         |                         |     entities associated |
|                         |                         |     with the device     |
|                         |                         | -   users: User         |
|                         |                         |     entities associated |
|                         |                         |     with the device     |
|                         |                         |                         |
|                         |                         |                         |
+-------------------------+-------------------------+-------------------------+

Event
-----

The *event* entity is used to log application data, primarily for
performance and error monitoring. Event entities can be also associated
with users and groups. The event mechanism in Apache Usergrid is optimized
to handle large numbers of events, so it is an ideal mechanism for
logging in your application.

For more information on using the event entity, see [Events and
counters](/events-and-counters).

### Properties

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | String                  | Type of entity, in this |
|                         |                         | case "event"            |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| timestamp               | long                    | **Required.** [UTC      |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the application event   |
|                         |                         | occurred                |
+-------------------------+-------------------------+-------------------------+
| user                    | UUID                    | *Optional.* UUID of     |
|                         |                         | application user that   |
|                         |                         | posted the event        |
+-------------------------+-------------------------+-------------------------+
| group                   | UUID                    | *Optional.* UUID of     |
|                         |                         | application group that  |
|                         |                         | posted the event        |
+-------------------------+-------------------------+-------------------------+
| category                | string                  | *Optional.* Category    |
|                         |                         | used for organizing     |
|                         |                         | similar events          |
+-------------------------+-------------------------+-------------------------+
| counters                | map                     | *Optional.* Counter     |
|                         |                         | used for tracking       |
|                         |                         | number of similar       |
|                         |                         | events                  |
+-------------------------+-------------------------+-------------------------+
| message                 | string                  | *Optional.* Message     |
|                         |                         | describing event. Will  |
|                         |                         | be *null* if no message |
|                         |                         | is specified            |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | event entity, as well   |
|                         |                         | as additional data      |
|                         |                         | entities associated     |
|                         |                         | with the event. The     |
|                         |                         | following properties    |
|                         |                         | are included in         |
|                         |                         | metadata:               |
|                         |                         |                         |
|                         |                         | -   **path**: Path to   |
|                         |                         |     retrieve the event  |
|                         |                         |     entity              |
+-------------------------+-------------------------+-------------------------+

Folder
------

The *folder* entity is used to emulate a file structure for the purpose
of organizing assets or custom entities.

For more information on using the folder entity, see [Folder](/folder).

### Properties

The following are the system-defined properties for folder entities:

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case"folder"            |
+-------------------------+-------------------------+-------------------------+
| name                    | string                  | *Optional.* Folder name |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| owner                   | UUID                    | **Required.** UUID of   |
|                         |                         | the folder’s owner      |
+-------------------------+-------------------------+-------------------------+
| path                    | string                  | **Required.** Relative  |
|                         |                         | path to the folder      |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | folder entity, as well  |
|                         |                         | as additional data      |
|                         |                         | entities associated     |
|                         |                         | with the asset. The     |
|                         |                         | following properties    |
|                         |                         | are included in         |
|                         |                         | metadata:               |
|                         |                         |                         |
|                         |                         | -   **path**: Path to   |
|                         |                         |     retrieve the folder |
|                         |                         |     entity              |
+-------------------------+-------------------------+-------------------------+

Group
-----

The *group* entity allows you to group users based on any criteria.
Multiple group entities can be nested to create sub-groups. Users can
also belong to multiple groups. Examples of uses for the group entity
include grouping users by interest or location.

The look-up properties for the entities of type group are UUID and path,
that is, you can use the uuid or path property to reference a group in
an API call. However, you can search on a group using any property of
the group entity. See [Queries and parameters](/queries-and-parameters)
for details on searching.

For more information on using the group entity, see [Group](/group).

### Properties

The following are the system-defined properties for group entities

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity UUID      |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case “group”            |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| path                    | string                  | **Required.** Relative  |
|                         |                         | path where the group    |
|                         |                         | can be retrieved        |
+-------------------------+-------------------------+-------------------------+
| title                   | string                  | *Optional.* Display     |
|                         |                         | name for the group      |
|                         |                         | entity                  |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | group entity, as well   |
|                         |                         | as additional data      |
|                         |                         | entities associated     |
|                         |                         | with the group. The     |
|                         |                         | following properties    |
|                         |                         | are included in         |
|                         |                         | metadata:               |
|                         |                         |                         |
|                         |                         | **path**: Path to       |
|                         |                         | retrieve the group      |
|                         |                         | entity, including the   |
|                         |                         | group UUID              |
|                         |                         |                         |
|                         |                         | **sets**: Nested object |
|                         |                         | that contains the       |
|                         |                         | 'rolenames' and         |
|                         |                         | 'permissions'           |
|                         |                         | properties.             |
|                         |                         |                         |
|                         |                         | -   rolenames: Path to  |
|                         |                         |     retrieve a list of  |
|                         |                         |     roles associated    |
|                         |                         |     with the group.     |
|                         |                         | -   permissions: Path   |
|                         |                         |     to retrieve a list  |
|                         |                         |     of all permissions  |
|                         |                         |     directly associated |
|                         |                         |     with the group. If  |
|                         |                         |     the group is        |
|                         |                         |     associated with a   |
|                         |                         |     role, the list will |
|                         |                         |     not include         |
|                         |                         |     permissions         |
|                         |                         |     associated with the |
|                         |                         |     role entity.        |
|                         |                         |                         |
|                         |                         |                         |
|                         |                         |                         |
|                         |                         | **collections**: Nested |
|                         |                         | object that contains    |
|                         |                         | paths to data entity    |
|                         |                         | collections associated  |
|                         |                         | with the group.         |
|                         |                         |                         |
|                         |                         | -   activities:         |
|                         |                         |     Activity entities   |
|                         |                         |     associated with the |
|                         |                         |     group               |
|                         |                         | -   feed: A feed of all |
|                         |                         |     activities          |
|                         |                         |     published by users  |
|                         |                         |     associated with the |
|                         |                         |     group               |
|                         |                         | -   roles: Role         |
|                         |                         |     entities associated |
|                         |                         |     with the group      |
|                         |                         | -   users: User         |
|                         |                         |     entities associated |
|                         |                         |     with the group      |
|                         |                         |                         |
|                         |                         |                         |
+-------------------------+-------------------------+-------------------------+

Notification
------------

The *notification* entity represents a push notification, including
notification message and details. A notification entity is sent in
conjunction with a notifier entity to a notification service, such as
Apple Push Notification Service, to initiate a push notification.

For more information on using the notification entity, see [Create &
Manage Notifications](/create-manage-notifications).

### Properties

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case “notification”     |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| payloads                | string                  | **Required.** The push  |
|                         |                         | notifications to be     |
|                         |                         | delivered, formatted as |
|                         |                         | key-value pairs of      |
|                         |                         | notifier entities and   |
|                         |                         | messages                |
|                         |                         | (\<notifier\_name1\>:\< |
|                         |                         | message1\>,             |
|                         |                         | \<notifier\_name2\>:\<m |
|                         |                         | essage2\>,              |
|                         |                         | ...)                    |
+-------------------------+-------------------------+-------------------------+
| errorMessage            | string                  | Error message returned  |
|                         |                         | by the notification     |
|                         |                         | service (APNs or GCM)   |
|                         |                         | if the notification     |
|                         |                         | fails entirely          |
+-------------------------+-------------------------+-------------------------+
| scheduled               | bool                    | Whether the             |
|                         |                         | notification is         |
|                         |                         | currently scheduled for |
|                         |                         | delivery                |
+-------------------------+-------------------------+-------------------------+
| state                   | string                  | The current delivery    |
|                         |                         | status of the           |
|                         |                         | notification:           |
|                         |                         | "FINISHED", "SCHEDULED" |
|                         |                         | or "CANCELED".          |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | notification entity, as |
|                         |                         | well as additional data |
|                         |                         | entities associated     |
|                         |                         | with the notification.  |
|                         |                         | The following           |
|                         |                         | properties are included |
|                         |                         | in metadata:            |
|                         |                         |                         |
|                         |                         | **path**: Path to       |
|                         |                         | retrieve the            |
|                         |                         | notification object     |
|                         |                         |                         |
|                         |                         | **collections**: Nested |
|                         |                         | object that contains    |
|                         |                         | paths to data entity    |
|                         |                         | collections associated  |
|                         |                         | with the notification.  |
|                         |                         |                         |
|                         |                         | -   **queue**: Device   |
|                         |                         |     entities scheduled  |
|                         |                         |     to receive the push |
|                         |                         |     notification        |
|                         |                         | -   **receipts**":      |
|                         |                         |     Receipt entities    |
|                         |                         |     for delivery        |
|                         |                         |     attempts"           |
+-------------------------+-------------------------+-------------------------+

Notifier
--------

The *notifier* entity contains the credentials necessary to securely
access push notification service providers, which in turn send your
notifications to targeted devices.

For more information on using the notifier entity, see [Create
Notifiers](/create-notifiers).

### Properties

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case “notifier”         |
+-------------------------+-------------------------+-------------------------+
| name                    | string                  | *Optional.* Notifier    |
|                         |                         | display name            |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| provider                | string                  | **Required.** Push      |
|                         |                         | notification provider:  |
|                         |                         | "apple" or "google"     |
+-------------------------+-------------------------+-------------------------+
| environment             | string                  | **Required.** The       |
|                         |                         | environment that        |
|                         |                         | corresponds to your     |
|                         |                         | app: "development" or   |
|                         |                         | "production"            |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | notifier entity         |
|                         |                         |                         |
|                         |                         | **path**: Path to       |
|                         |                         | retrieve the            |
|                         |                         | notification object     |
+-------------------------+-------------------------+-------------------------+

Receipt
-------

The *receipt* entity is created after a push notification has been sent
using Apache Usergrid. The receipt is a record of an attempted push
notification, including if the notification was successful, and when it
was sent.

For more information on the receipt entity, see [Create & Manage
Notifications](/create-manage-notifications).

### Properties

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case “receipt”          |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| payload                 | string                  | The push notification   |
|                         |                         | message that was        |
|                         |                         | specified in the        |
|                         |                         | 'payload' property of   |
|                         |                         | the notification entity |
|                         |                         | associated with the     |
|                         |                         | push notification.      |
+-------------------------+-------------------------+-------------------------+
| errorMessage            | string                  | Error message returned  |
|                         |                         | by the notification     |
|                         |                         | service (APNs or GCM)   |
|                         |                         | if delivery of the      |
|                         |                         | notification to a       |
|                         |                         | device fails            |
+-------------------------+-------------------------+-------------------------+
| errorCode               | String                  | Error code returned by  |
|                         |                         | the notification        |
|                         |                         | service, if any.        |
+-------------------------+-------------------------+-------------------------+
| sent                    | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds for     |
|                         |                         | when the notification   |
|                         |                         | was sent                |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | notifier entity         |
|                         |                         |                         |
|                         |                         | **path**: Path to       |
|                         |                         | retrieve the            |
|                         |                         | notification object     |
+-------------------------+-------------------------+-------------------------+

Role
----

The *role* entity is used to define standard permission sets that can be
assigned to user and groups entities. For example, you might create an
Administrator role to easily grant certain users full access to all app
features.

For more information on using the role entity, see [Role](/role).

### Properties

The following are the system-defined properties for role entities:

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case "role"             |
+-------------------------+-------------------------+-------------------------+
| name                    | string                  | *Optional.* Unique name |
|                         |                         | that identifies the     |
|                         |                         | role                    |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| roleName                | string                  | Identical to the value  |
|                         |                         | of the 'name' property  |
|                         |                         | by default              |
+-------------------------+-------------------------+-------------------------+
| title                   | string                  | Identical to the value  |
|                         |                         | of the 'name' property  |
|                         |                         | by default              |
+-------------------------+-------------------------+-------------------------+
| inactivity              | string                  | The amount of time, in  |
|                         |                         | milliseconds, that a    |
|                         |                         | user or group           |
|                         |                         | associated with the     |
|                         |                         | role can be inactive    |
|                         |                         | before they lose the    |
|                         |                         | permissions associated  |
|                         |                         | with that role. By      |
|                         |                         | default, 'inactivity'   |
|                         |                         | is set to 0 so that the |
|                         |                         | user/group never loses  |
|                         |                         | the role.               |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | role entity, as well as |
|                         |                         | additional data         |
|                         |                         | entities associated     |
|                         |                         | with the role. The      |
|                         |                         | following properties    |
|                         |                         | are included in         |
|                         |                         | metadata:               |
|                         |                         |                         |
|                         |                         | **path**: Path to       |
|                         |                         | retrieve the role       |
|                         |                         | entity                  |
|                         |                         |                         |
|                         |                         | **sets**: Nested object |
|                         |                         | that contains the       |
|                         |                         | 'permissions' property. |
|                         |                         |                         |
|                         |                         | -   permissions: Path   |
|                         |                         |     to retrieve a list  |
|                         |                         |     of all permissions  |
|                         |                         |     associated with the |
|                         |                         |     role.               |
|                         |                         |                         |
|                         |                         |                         |
|                         |                         |                         |
|                         |                         | **collections**: Nested |
|                         |                         | object that contains    |
|                         |                         | paths to data entity    |
|                         |                         | collections associated  |
|                         |                         | with the role.          |
|                         |                         |                         |
|                         |                         | -   groups: Group       |
|                         |                         |     entities associated |
|                         |                         |     with the role       |
|                         |                         | -   users: User         |
|                         |                         |     entities associated |
|                         |                         |     with the role       |
|                         |                         |                         |
|                         |                         |                         |
+-------------------------+-------------------------+-------------------------+

User
----

The *user* entity represents a registered user of your app, and includes
optional properties for common user details, such as real name, email
address, and password.

For more information on using the user entity, see [User](/user).

### Properties

The following are the system-defined properties for user entities:

+-------------------------+-------------------------+-------------------------+
| Property                | Type                    | Description             |
+=========================+=========================+=========================+
| uuid                    | UUID                    | Unique entity ID        |
+-------------------------+-------------------------+-------------------------+
| type                    | string                  | Type of entity, in this |
|                         |                         | case “user”             |
+-------------------------+-------------------------+-------------------------+
| created                 | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was created  |
+-------------------------+-------------------------+-------------------------+
| modified                | long                    | [UTC                    |
|                         |                         | timestamp](http://en.wi |
|                         |                         | kipedia.org/wiki/Coordi |
|                         |                         | nated_Universal_Time)   |
|                         |                         | in milliseconds of when |
|                         |                         | the entity was last     |
|                         |                         | modified                |
+-------------------------+-------------------------+-------------------------+
| username                | string                  | **Required.**Valid and  |
|                         |                         | unique username         |
+-------------------------+-------------------------+-------------------------+
| password                | string                  | *Optional.* User        |
|                         |                         | password                |
+-------------------------+-------------------------+-------------------------+
| name                    | string                  | *Optional.* *Optional.* |
|                         |                         | User display name       |
+-------------------------+-------------------------+-------------------------+
| email                   | string                  | *Optional.*User's email |
|                         |                         | address. Note that this |
|                         |                         | is a required field for |
|                         |                         | user entities created   |
|                         |                         | with the User           |
|                         |                         | Management tool in the  |
|                         |                         | Apache Usergrid console.   |
+-------------------------+-------------------------+-------------------------+
| firstname               | string                  | *Optional.* User first  |
|                         |                         | name                    |
+-------------------------+-------------------------+-------------------------+
| middlename              | string                  | *Optional.* User middle |
|                         |                         | name                    |
+-------------------------+-------------------------+-------------------------+
| lastname                | string                  | *Optional.* User last   |
|                         |                         | name                    |
+-------------------------+-------------------------+-------------------------+
| picture                 | string                  | *Optional.* URL where   |
|                         |                         | the user's profile      |
|                         |                         | picture can be          |
|                         |                         | retrieved               |
+-------------------------+-------------------------+-------------------------+
| activated               | boolean                 | Whether the user        |
|                         |                         | account is activated.   |
|                         |                         | Set to 'true' by        |
|                         |                         | default when the user   |
|                         |                         | is created.             |
+-------------------------+-------------------------+-------------------------+
| metadata                | object                  | A nested,               |
|                         |                         | JSON-formatted object   |
|                         |                         | that provides the       |
|                         |                         | relative path to the    |
|                         |                         | user entity, as well as |
|                         |                         | additional data         |
|                         |                         | entities associated     |
|                         |                         | with the user. The      |
|                         |                         | following properties    |
|                         |                         | are included in         |
|                         |                         | metadata:               |
|                         |                         |                         |
|                         |                         | **path**: Path to       |
|                         |                         | retrieve the user       |
|                         |                         | entity                  |
|                         |                         |                         |
|                         |                         | **sets**: Nested object |
|                         |                         | that contains the       |
|                         |                         | 'rolenames' and         |
|                         |                         | 'permissions'           |
|                         |                         | properties.             |
|                         |                         |                         |
|                         |                         | -   rolenames:          |
|                         |                         |     *Deprecated*. Use   |
|                         |                         |     **/users/\\/roles** |
|                         |                         |     instead. Path to    |
|                         |                         |     retrieve a list of  |
|                         |                         |     roles associated    |
|                         |                         |     with the user.      |
|                         |                         | -   permissions: Path   |
|                         |                         |     to retrieve a list  |
|                         |                         |     of all permissions  |
|                         |                         |     directly associated |
|                         |                         |     with the user. If   |
|                         |                         |     the user is         |
|                         |                         |     associated with a   |
|                         |                         |     role or group, the  |
|                         |                         |     list will not       |
|                         |                         |     include permissions |
|                         |                         |     associated with     |
|                         |                         |     those entities.     |
|                         |                         |                         |
|                         |                         |                         |
|                         |                         |                         |
|                         |                         | **collections**: Nested |
|                         |                         | object that contains    |
|                         |                         | paths to data entity    |
|                         |                         | collections associated  |
|                         |                         | with the user.          |
|                         |                         |                         |
|                         |                         | -   activities:         |
|                         |                         |     Activity entities   |
|                         |                         |     associated with the |
|                         |                         |     user                |
|                         |                         | -   devices: Device     |
|                         |                         |     entities associated |
|                         |                         |     with the user       |
|                         |                         | -   feed: A feed of all |
|                         |                         |     activities          |
|                         |                         |     published by the    |
|                         |                         |     user                |
|                         |                         | -   groups: Group       |
|                         |                         |     entities associated |
|                         |                         |     with the user       |
|                         |                         | -   roles: Role         |
|                         |                         |     entities associated |
|                         |                         |     with the user       |
|                         |                         | -   following: Users    |
|                         |                         |     that the user is    |
|                         |                         |     following           |
|                         |                         | -   followers: Users    |
|                         |                         |     that are following  |
|                         |                         |     the user            |
|                         |                         |                         |
|                         |                         |                         |
+-------------------------+-------------------------+-------------------------+


