# Groups

You can organize app users into groups. Groups have their own Activity Feed, their own permissions and be a useful alternative to Roles, depending on how you model your data. Groups were originaly designed to emulate Facebook Groups, so they will tend to function about the same way Facebook Groups would.

Groups are hierarchical. Every member of the group /groups/california/san-francisco is also a member of the group /groups/california.

Groups are also a great way to model things such a topic subscriptions. For example, you could allow people to subscribe (i.e. become a member of the group and be alerted via Activities) to /groups/memes/dogs/doge or subscribe to all /groups/memes/dogs


### General properties

  Property   Type     Description
  ---------- -------- ---------------------------------------------------------------------------------
  uuid       UUID     Group’s unique entity ID
  type       string   Type of entity, in this case “user”
  created    long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified   long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  path       string   Valid slash-delimited group path (mandatory)
  title      string   Display name

### Set properties

  Set           Type     Description
  ------------- -------- ---------------------------------------
  connections   string   Set of connection types (e.g., likes)
  rolenames     string   Set of roles assigned to a group
  credentials   string   Set of group credentials

### Collections

  Collection   Type       Description
  ------------ ---------- ------------------------------------------------------
  users        user       Collection of users in the group
  activities   activity   Collection of activities a user has performed
  feed         activity   Inbox of activity notifications a group has received
  roles        role       Set of roles to which a group belongs

 
