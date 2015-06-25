---
title: REST Endpoints
category: docs
layout: docs
---

REST Endpoints
==============

### Base URL: https://api.usergrid.com/management

 

### Access Token

URI

Verb

Content Types

Action

 

/token
'{"grant\_type":"client\_credentials","client\_id":"{client\_id}","client\_secret":"client\_secret"}'

POST

application/json

Obtain an access token (access type = organization)

[Detail](/access-token)

/token
'{"grant\_type":"password","username":"{username}":"password"="{password}"}'

POST

application/json

Obtain an access token (access type = admin user)

[Detail](/access-token)

/{org\_id}/{app\_id}/token
'{"grant\_type":"client\_credentials","client\_id":"{client\_id}","client\_secret":"{client\_secret}"}'

POST

application/json

Obtain an access token (access type = application)

[Detail](/access-token)

{org\_id}/{app\_id}/token
'{"grant\_type":"password","username":"{username}","password":"{password}"}'

POST

application/json

Obtain an access token (access type = application user)

[Detail](/access-token)

### Admin users

URI

Verb

Content Types

Action

 

/users

POST

application/json

Create an admin user

[Detail](/docs/usergrid/content/admin-user#creating-an-admin-user)

/users/{user|username|email|uuid}

PUT

application/json

Update an admin user

[Detail](/docs/usergrid/content/admin-user#updating-an-admin-user)

/users/{user|username|email|uuid}

GET

application/json

Get an admin user

[Detail](/docs/usergrid/content/admin-user#getting-an-admin-user)

/users/{user|username|email|uuid}/\
 password

PUT

application/json

Set an admin user's password

[Detail](/docs/usergrid/content/admin-user#setting-an-admin-user-s-password)

users/resetpw

GET

application/json

Initiate the reset of an admin user's password

[Detail](/docs/usergrid/content/admin-user#setting-an-admin-user-s-password)

/users/resetpw

POST

application/json

Complete the reset of an admin user's password

[Detail](/docs/usergrid/content/admin-user#setting-an-admin-user-s-password)

/users/{user|username|email|uuid}/activate?\
 token={token}&confirm={confirm\_email}

GET

application/json

Activate an admin user

[Detail](/docs/usergrid/content/admin-user#activating-an-admin-user)

/users/{user|username|email|uuid}/reactivate

GET

application/json

Reactivate an admin user

[Detail](/docs/usergrid/content/admin-user#reactivating-an-admin-user)

/users/{user|username|email|uuid}/feed

GET

application/json

Get an admin user's feed

[Detail](/docs/usergrid/content/admin-user#getting-an-admin-user-s-activity-feed)

### Applications

  -------------------
  See Organizations
  -------------------

### Client authorization

URI

Verb

Content Types

Action

 

/authorize?response\_type={response\_type}&\
 client\_id={client\_id}

GET

application/json

Authorize a client

[Detail](/docs/usergrid/content/client-authorization)

### Organizations

URI

Verb

Content Types

Action

 

/organizations|orgs

POST

application/json

Create an organization

[Detail](/docs/usergrid/content/organization#creating-an-organization)

/organizations|orgs/{org\_name}|{uuid}

GET

application/json

Retrieve an organization

[Detail](/docs/usergrid/content/organization#getting-an-organization)

/organizations|orgs/{org\_name}|{uuid}/\
 activate?token={token}&confirm={confirm\_email}

GET

application/json

Activate an organization

[Detail](/docs/usergrid/content/organization#activating-an-organization)

/organizations|orgs/{org\_name}|{uuid}/\
 reactivate

GET

application/json

Reactivate an organization

[Detail](/docs/usergrid/content/organization#reactivating-an-organization)

/organizations|orgs/{org\_name}|{uuid}/\
 credentials

POST

application/json

Generate organization client credentials

[Detail](/docs/usergrid/content/organization#generating-organization-client-credentials)

/organizations|orgs/{org\_name}|{uuid}/\
 credentials

GET

application/json

Retrieve organization client credentials

[Detail](/docs/usergrid/content/organization#retrieving-organization-client-credentials)

/organizations|orgs/{org\_name}|{uuid}/\
 feed

GET

application/json

Retrieve an organization's activity feed

[Detail](/docs/usergrid/content/organization#getting-an-organization-s-activity-feed)

/organizations|orgs/{org\_name}|{org\_uuid}/\
 apps

POST

application/json

Create an organization application

[Detail](/docs/usergrid/content/organization#creating-an-organization-application)

/organizations|orgs/{org\_name}|{org\_uuid}/\
 apps/{app\_name}|{app\_uuid}

DELETE

application/json

Delete an organization application

[Detail](/docs/usergrid/content/organization#deleting-an-organization-application)

/organizations|orgs/{org\_name}|{uuid}/\
 applications|apps/{app\_name}|{uuid}/\
 credentials

POST

application/json

Generate credentials for an organization application

[Detail](/docs/usergrid/content/organization#generating-an-application-credentials)

/organizations|orgs/{org\_name}|{uuid}/\
 applications|apps/\
 {app\_name}|{uuid}/credentials

GET

application/json

Get credentials for an organization application

[Detail](/docs/usergrid/content/organization#getting-application-credentials)

/organizations|orgs/{org\_name}|{uuid}/\
 applications|apps

GET

application/json

Get the applications in an organization

[Detail](/docs/usergrid/content/organization#getting-the-applications-in-an-organization)

/organizations|orgs/{org\_name}|{org\_uuid}/\
 users/{username|email|uuid}

PUT

application/json

Adding an admin user to an organization

[Detail](/docs/usergrid/content/organization#adding-an-admin-user-to-an-organization)

/organizations|orgs/{org\_name}|{org\_uuid}/ users

GET

application/json

Getting the admin users in an organization

[Detail](/docs/usergrid/content/organization#getting-the-admin-users-in-an-organization)

/organizations|orgs/{org\_name}|{org\_uuid}/\
 users/{username|email|uuid}

DELETE

application/json

Removing an admin user from an organization

[Detail](/docs/usergrid/content/organization#removing-an-admin-user-from-an-organization)

### Base URL: https://api.usergrid.com

### Activities

URI

Verb

Content Types

Action

 

/{org\_id}/{app\_id}/users/\
 {uuid|username}/activities

POST

application/json

Create an activity

[Detail](/docs/usergrid/content/activity#creating-an-activity)

/{org\_id}/{app\_id}/groups/\
 {uuid|groupname}/activities

POST

application/json

Post an activity to a group

[Detail](/docs/usergrid/content/activity#posting-an-activity-to-a-group)

### Assets

  -------------------------------------------------------
  See Collections (other than users, groups, and roles)
  -------------------------------------------------------

### Collections (other than users, groups, and roles)

URI

Verb

Content Types

Action

 

/{org\_id}/{app\_id}/

GET

application/json

Retrieve all collections

[Detail](/docs/usergrid/content/general-purpose-endpoints#create_entity)

/{org\_id}/{app\_id}/{collection}

POST

application/json

Create a new entity or collection

[Detail](/docs/usergrid/content/general-purpose-endpoints#create_entity)

/{org\_id}/{app\_id}/{collection}/\
 {uuid|name}

GET

application/json

Retrieve an entity

[Detail](/docs/usergrid/content/general-purpose-endpoints#get_entity_uuid)

/{org\_id}/{app\_id}/{collection}/\
 {uuid|name}

PUT

application/json

Update an entity

[Detail](/docs/usergrid/content/general-purpose-endpoints#update_entity)

/{org\_id}/{app\_id}/{collection}/\
 {uuid|name}

DELETE

application/json

Delete an entity

[Detail](/docs/usergrid/content/general-purpose-endpoints#delete_uuid)

/{org\_id}/{app\_id}/{collection}?{query}

GET

application/json

Query a collection

[Detail](/docs/usergrid/content/general-purpose-endpoints#query_entity_collection)

/{org\_id}/{app\_id}/{collection}?{query}

PUT

application/json

Update a collection by query

[Detail](/docs/usergrid/content/general-purpose-endpoints#update_collection)

/{org\_id}/{app\_id}/{collection}/{entity\_id}/\
 {relationship}?{query}

GET

application/json

Query an entity's collections or connections

[Detail](/docs/usergrid/content/general-purpose-endpoints#query_entity_collection)

/{org\_id}/{app\_id}/{collection}/\
 {first\_entity\_id}/{relationship}/\
 {second\_entity\_id}\
 or\
 /{org\_id}/{app\_id}/{collection}/\
 {first\_entity\_id}/{relationship}/\
 {second\_entity\_type}/{second\_entity\_id}

POST

application/json

Add an entity to a collection or create a connection

[Detail](/docs/usergrid/content/general-purpose-endpoints#put_entity)

/{org\_id}/{app\_id}/{collection}/\
 {first\_entity\_id}/{relationship}/\
 {second\_entity\_id}\
 or\
 /{org\_id}/{app\_id}/{collection}/\
 {first\_entity\_id}/{relationship}/\
 {second\_entity\_type}/{second\_entity\_id}

DELETE

application/json

Remove an entity from a collection or delete a connection

[Detail](/docs/usergrid/content/general-purpose-endpoints#remove_entity)

### Devices

  -------------------------------------------------------
  See Collections (other than users, groups, and roles)
  -------------------------------------------------------

### Events

URI

Verb

Content Types

Action

 

/{org\_id}/{app\_id}/events

POST

application/json

Create an event

[Detail](/docs/usergrid/content/events-and-counters#new_event)

### Folders

  -------------------------------------------------------
  See Collections (other than users, groups, and roles)
  -------------------------------------------------------

### Groups

URI

Verb

Content Types

Action

 

/{org\_id}/{app\_id}/groups

POST

application/json

Create a new group

[Detail](/docs/usergrid/content/group#create_group)

/{org\_id}/{app\_id}/groups/\
 {uuid|groupname}/users/{uuid|username}

POST

application/json

Add a user to a group

[Detail](/docs/usergrid/content/group#add_user_group)

/{org\_id}/{app\_id}/groups/\
 {uuid|groupname}

GET

application/json

Get a group

[Detail](/docs/usergrid/content/group#get_group)

/{org\_id}{app\_id}/groups/\
 {uuid|groupname}

PUT

application/json

Update a group

[Detail](/docs/usergrid/content/group#update_group)

/{org\_id}/{app\_id}/groups/\
 {uuid|groupname}/users/{uuid|username}

DELETE

application/json

Delete user from a group

[Detail](/docs/usergrid/content/group#delete_user_group)

/{org\_id}/{app\_id}/groups/\
 {uuid|groupname}/feed

GET

application/json

Get a group's feed

[Detail](/docs/usergrid/content/group#getting-a-group-s-feed)

### Roles

URI

Verb

Content Types

Action

 

/{org\_id}/{app\_id}/roles

POST

application/json

Create a new role

[Detail](/docs/usergrid/content/role#create_role)

/{org\_id}/{app\_id}/roles

GET

application/json

Get the roles in an application

[Detail](/docs/usergrid/content/role#get_roles)

/{org\_id}/{app\_id}/roles/{rolename}

DELETE

application/json

Delete a role

[Detail](/docs/usergrid/content/role#delete_role)

/{org\_id}/{app\_id}/roles/\
 {rolename|role\_id}/permissions

GET

application/json

Get permissions for a role

[Detail](/docs/usergrid/content/role#get_permission)

/{org\_id}/{app\_id}/roles/\
 {rolename|role\_id}/permissions

POST

application/json

Add permissions to a role

[Detail](/docs/usergrid/content/role#add_permission)

{org\_id}/{app\_id}/roles/\
 {rolename|role\_id}/permissions?\
 permission={grant\_url\_pattern}

DELETE

application/json

Delete permissions from a role

[Detail](/docs/usergrid/content/role#delete_permission)

/{org\_id}/{app\_id}/roles/{role\_id}/\
 users/{uuid|username}\
 or\
 /{org\_id}/{app\_id}/users/\
 {uuid|username}/roles/{role\_id}

POST

application/json

Add a user to a role

[Detail](/docs/usergrid/content/role#adding-a-user-to-a-role)

/{org\_id}/{app\_id}/roles/{role\_id}/\
 users

GET

application/json

Get the users in a role

[Detail](/docs/usergrid/content/role#getting-the-users-in-a-role)

/{org\_id}/{app\_id}/roles/{role\_id}/\
 users/{uuid|username}

DELETE

application/json

Delete a user from a role

[Detail](/docs/usergrid/content/role#deleting-a-user-from-a-role)

### Users

URI

Verb

Content Types

Action

 

/{org\_id}/{app\_id}/users

POST

application/json

Create a user in the users collection

[Detail](/docs/usergrid/content/user#create_user)

/{org\_id}/{app\_id}/users/{user}/\
 password

POST

application/json

Set a user's password or reset the user's existing password

[Detail](/docs/usergrid/content/user#set_password)

/{org\_id}/{app\_id}/users/\
 {uuid|username|email\_address}

GET

application/json

Retrieve a user

[Detail](/docs/usergrid/content/user#uuid)

/{org\_id}/{app\_id}/users/\
 {uuid|username}

PUT

application/json

Update a user

[Detail](/docs/usergrid/content/user#update_user)

/{org\_id}/{app\_id}/users/{uuid|username}

DELETE

application/json

Delete a user

[Detail](/docs/usergrid/content/user#delete_user)

/{org\_id}/{app\_id}/users?{query}

GET

application/json

Query to get users

[Detail](/docs/usergrid/content/user#query_get)

/{org\_id}/{app\_id}/groups/\
 {uuid|groupname}/users/{uuid|username}

POST

application/json

Add a user to a group

[Detail](/docs/usergrid/content/user#add_user_group)

/{org\_id}/{app\_id}/{collection}/\
 {first\_entity\_id}/{relationship}/\
 {second\_entity\_id}\
 or\
 /{org\_id}/{app\_id}/{collection}/\
 {first\_entity\_id}/{relationship}/\
 {second\_entity\_type}/{second\_entity\_id}

POST

application/json

Add a user to a collection or create a connection

[Detail](/docs/usergrid/content/user#add_collection)

/{org\_id}/{app\_id}/{collection}/\
 {first\_entity\_id}/{relationship}/\
 {second\_entity\_id}\
 or\
 /{org\_id}/{app\_id}/{collection}/\
 {first\_entity\_id}/{relationship}/\
 {second\_entity\_type}/{second\_entity\_id}

DELETE

application/json

Remove a user from a collection or delete a connection

[Detail](/docs/usergrid/content/user#delete_collection)

/{org\_id}/{app\_id}/users/{uuid|username}/\
 {relationship}?{query}

GET

application/json

Query a user's collections or connections

[Detail](/docs/usergrid/content/user#query_collection)

/{org\_id}/{app\_id}/users/\
 {uuid|username}/feed

GET

application/json

Get a user's feed

[Detail](/docs/usergrid/content/user#getting-a-user-s-feed)

### Notifications, Notifiers, and Receipts

  -------------------------------------------------------------------
  See [Create & Manage Notifications](/create-manage-notifications)
  -------------------------------------------------------------------

 
