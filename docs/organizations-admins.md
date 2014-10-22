Organizations & Admins
==================================================

.. toctree::
	concepts/applications
	concepts/events-and-counters
	concepts/roles-and-permissions
	concepts/relationships
	concepts/collections
	concepts/query-language
	concepts/users-devices
	concepts/groups
	concepts/activity
	concepts/assets

Organizations
             

An organization represents the highest level of the Apache Usergrid data
hierarchy. It contains applications (and the entities and collections
they contain) and is associated with one or more administrators. An
organization can be representative of a company, team, or project. It
allows multiple applications  to be shared within the organization with
other administrators.

+----------------+----------+--------------------------------------------------------------------------------------+
| Property       | Type     | Description                                                                          |
+================+==========+======================================================================================+
| uuid           | UUID     | Organization’s unique entity ID                                                      |
+----------------+----------+--------------------------------------------------------------------------------------+
| type           | string   | "organization"                                                                       |
+----------------+----------+--------------------------------------------------------------------------------------+
| created        | long     | `UNIX timestamp <http://en.wikipedia.org/wiki/Unix_time>`__ of entity creation       |
+----------------+----------+--------------------------------------------------------------------------------------+
| modified       | long     | `UNIX timestamp <http://en.wikipedia.org/wiki/Unix_time>`__ of entity modification   |
+----------------+----------+--------------------------------------------------------------------------------------+
| organization   | string   | The name of the organization.                                                        |
+----------------+----------+--------------------------------------------------------------------------------------+
| username       | string   | The username of the administrator.                                                   |
+----------------+----------+--------------------------------------------------------------------------------------+
| name           | string   | The name of the administrator.                                                       |
+----------------+----------+--------------------------------------------------------------------------------------+
| email          | string   | The email address of the administrator.                                              |
+----------------+----------+--------------------------------------------------------------------------------------+
| password       | string   | The password of the administrator. (create-only)                                     |
+----------------+----------+--------------------------------------------------------------------------------------+

Admins
      

An admin user has full access to perform any operation on all
organization accounts of which the admin user is a member. Using the App
services API, you can create, update, or retrieve an admin user. You can
also set or reset an admin user's password, activite or reactivate an
admin user, and get an admin user's activity feed.
