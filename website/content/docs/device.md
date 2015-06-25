---
title: Device
category: docs
layout: docs
---

Device
======

Using App services APIs you can create, retrieve, update, delete, and
query device entities. See You do not have access to view this node for
descriptions of these APIs.

Device properties
-----------------

The following are the system-defined properties for device entities. You
can create application-specific properties for a device entity in
addition to the system-defined properties. The system-defined properties
are reserved. You cannot use these names to create other properties for
a device entity. In addition the devices name is reserved for the
devices collection — you can't use it to name another collection.

  Property   Type     Description
  ---------- -------- ---------------------------------------------------------------------------------
  uuid       UUID     Entity unique ID
  type       string   Entity type (e.g., device)
  created    long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified   long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  name       string   Device name (mandatory)

Associated collection property
------------------------------

Devices have the following associated collection.

  Collection   Type   Description
  ------------ ------ -----------------------------------------------
  users        user   Collection of users to which a device belongs

 
