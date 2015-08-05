# Users & Devices

Users and Devices are the primary ways to identify access to the system. Devices are great to track anonymous access, while Users allow you to model signing up, signing in, etc. 

Users
-----

## Properties

Property     Type      Description
------------ --------- ---------------------------------------------------------------------------------
  uuid         UUID      User’s unique entity ID
  type         string    Type of entity, in this case “user”
  created      long      [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified     long      [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  username     string    Valid and unique string username (mandatory)
  password     string    User password
  email        string    Valid and unique email address
  name         string    User display name
  activated    boolean   Whether the user account is activated
  disabled     boolean   Whether the user account is administratively disabled
  firstname    string    User first name
  middlename   string    User middle name
  lastname     string    User last name
  picture      string    User picture


## Sets

  Set           Type     Description
  ------------- -------- ---------------------------------------
  connections   string   Set of connection types (e.g., likes)
  rolenames     string   Set of roles assigned to a user
  permissions   string   Set of user permissions
  credentials   string   Set of user credentials

## Relationshops

  Collection   Type       Description
  ------------ ---------- -----------------------------------------------------
  groups       group      Collection of groups to which a user belongs
  devices      device     Collection of devices in the service
  activities   activity   Collection of activities a user has performed
  feed         activity   Inbox of activity notifications a user has received
  roles        role       Set of roles assigned to a user

## Facebook Sign-in

You can authenticate your Apache Usergrid requests by logging into
Facebook. To access Apache Usergrid resources, you need to provide an
access token with each request (unless you use the sandbox app). You can
get an access token by connecting to an appropriate web service endpoint
and providing the correct client credentials — this is further described
in [Authenticating users and application
clients](/authenticating-users-and-application-clients). However, you
can also obtain an access token by logging into Facebook.

To enable authentication to Apache Usergrid through Facebook, do the
following in your app:

1.  Make a login call to the Facebook API (do this using the [Facebook
    SDK](https://developers.facebook.com/docs/sdks/) or
    [API](https://developers.facebook.com/docs/facebook-login/)). If the
    login succeeds, a Facebook access token is returned.
2.  Send the Facebook access token to Apache Usergrid. If the Facebook
    access token is valid and the user does not already exist in App
    Services, Apache Usergrid provisions a new Apache Usergrid user. It also
    returns an Apache Usergrid access token, which you can use for
    subsequent Apache Usergrid API calls. Behind the scenes, Apache Usergrid
    uses the Facebook access token to retrieve the user's profile
    information from Facebook.

    If the Facebook access token is invalid, Facebook returns an OAuth
    authentication error, and the login does not succeed.

The request to authenticate to Apache Usergrid using a Facebook access
token is:

    GET https://api.usergrid.com/{my_org}/{my_app}/auth/facebook?fb_access_token={fb_access_token}

where:

* {my\_org} is the organization UUID or organization name.\
* {my\_app} is the application UUID or application name.\
* {fb\_access\_token} is the Facebook access token.


Devices
-------

## Properties

Property   Type     Description
---------- -------- ---------------------------------------------------------------------------------
  uuid       UUID     Entity unique ID
  type       string   Entity type (e.g., device)
  created    long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified   long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  name       string   Device name (mandatory)


## Relationships

Devices have the following associated collection.

  Collection   Type   Description
  ------------ ------ -----------------------------------------------
  users        user   Collection of users to which a device belongs
