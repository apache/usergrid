---
title: Folder
category: docs
layout: docs
---

Folder
======

Folder entities are used primarily to organize content into a structure.
For example, you can create a folders collection for media content, and
within it have a ‘music’ folder for music content, and a ‘video’ folder
for video content.

Using Apache Usergrid APIs you can create, retrieve, update, delete, and
query folder entities. See You do not have access to view this node for
descriptions of these APIs.

Folder properties
-----------------

The following are the system-defined properties for foldetr entities.
You can create application-specific properties for a folder entity in
addition to the system-defined properties. The system-defined properties
are reserved. You cannot use these names to create other properties for
a folder entity. In addition the folders name is reserved for the
folders collection — you can't use it to name another collection.

  Property   Type     Description
  ---------- -------- ---------------------------------------------------------------------------------
  uuid       UUID     the folder’s unique entity ID
  type       string   "folder"
  name       string   Folder name (mandatory)
  created    long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified   long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  owner      UUID     UUID of the folder’s owner (mandatory)
  path       string   Relative path to the folder (mandatory)

Folders have the following set properties.

  Set           Type     Description
  ------------- -------- -----------------------------------
  connections   string   set of connections for the folder

 
