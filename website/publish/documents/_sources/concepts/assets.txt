# Assets

Asset entities are used primarily in Apache Usergrid to manage binary data
objects such as images, video, and audio content. However, an asset does
not have to be used for a binary object. For example, assets can be used
to model a file system.


  Property       Type     Description
  -------------- -------- ---------------------------------------------------------------------------------
  uuid           UUID     Asset’s unique entity ID
  type           string   "asset"
  name           string   Asset name (mandatory)
  created        long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified       long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  owner          UUID     UUID of the asset’s owner (mandatory)
  path           string   Relative path to the asset (mandatory)
  content-type   string   Content type of the asset (for example, “image/jpeg”)

 
