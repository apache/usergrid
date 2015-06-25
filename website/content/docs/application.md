---
title: Application
category: docs
layout: docs
---

Application
===========

You can create a new application in an organization through the [Admin
portal](/admin-portal). The Admin portal creates the new application by
issuing a post against the management endpoint (see the "Creating an
organization application" section in [Organization](/organization) for
details). If you need to create an application programmatically in your
app, you can also use the API to do this. You can access application
entities using your app name or UUID, prefixed with the organization
name or UUID:

[https://api.usergrid.com](http://api.usergrid.com/)/{org\_name|uuid}/{app\_name|uuid}

Most mobile apps never access the application entity directly. For
example you might have a server-side web app that accesses the
application entity for configuration purposes. If you want to access
your application entity programmatically, you can use the API.

Application properties
----------------------

The following are the system-defined properties for application
entities. You can create application-specific properties for an
application entity in addition to the system-defined properties. The
system-defined properties are reserved. You cannot use these names to
create other properties for an application entity. In addition the
applications name is reserved for the applications collection — you
can't use it to name another collection.

The look-up properties for the entities of type application are uuid and
name, that is, you can use the uuid and name properties to reference an
application entity in an API call. However, you can search on a role
using any property of the application entity. See [Queries and
parameters](/queries-and-parameters) for details on searching.

  Property                                Type      Description
  --------------------------------------- --------- ---------------------------------------------------------------------------------
  uuid                                    UUID      Application’s unique entity ID
  type                                    string    "application"
  created                                 long      [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified                                long      [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  name                                    string    Application name (mandatory)
  title                                   string    Application title
  description                             string    Application description
  activated                               boolean   Whether application is activated
  disabled                                boolean   Whether application is administratively disabled
  allowOpenRegistration                   boolean   Whether application allows any user to register
  registrationRequiresEmailConfirmation   boolean   Whether registration requires email confirmation
  registrationRequiresAdminApproval       boolean   Whether registration requires admin approval
  accesstokenttl                          long      Time to live value for an access token obtained within the application

Set properties
--------------

The set properties for applications are listed in the table below.

  Set              Type     Description
  ---------------- -------- ----------------------------------------------------
  collections      string   Set of collections
  rolenames        string   Set of roles assigned to an application
  counters         string   Set of counters assigned to an application
  oauthproviders   string   Set of OAuth providers for the application
  credentials      string   Set of credentials required to run the application

Collections
-----------

The collections for applications are listed in the table below.

  Collection      Type           Description
  --------------- -------------- ----------------------------------------------------------------------------------
  users           user           Collection of users
  groups          group          Collection of groups
  folders         folder         Collection of assets that represent folder-like objects
  events          event          Collection of events posted by the application
  assets          asset          Collection of assets that represent file-like objects
  activities      activity       Collection of activity stream actions
  devices         device         Collection of devices in the service
  notifiers       notifier       Collection of notifiers used for push notifications
  notifications   notification   Collection of push notifications that have been sent or are scheduled to be sent
  receipts        receipt        Collection of receipts from push notifications that were sent

 
