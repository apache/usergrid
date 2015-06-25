---
title: Authenticating users and application clients
category: docs
layout: docs
---

Authenticating users and application clients
============================================

To protect your Apache Usergrid application data, one of the steps you'll
take is to authenticate your app's users. By ensuring that they are who
they say they are, you can help ensure that your application's data is
available in secure ways. After you've created permission rules that
define access to your application and have associated these rules with
users, you'll want to add code that authenticates your user, as
described in this topic.

**Note: **You manage access to your application's data by creating
permission rules that govern which users can do what. Users
authenticated as Application User have access according to these rules.
For more about managing permissions, see [Managing access by defining
permission rules](/managing-access-defining-permission-rules).

Authentication levels
---------------------

Apache Usergrid supports four levels of authentication, but only one of
them is used when checking a registered user's permissions. The other
three levels are useful for authenticating other web applications that
want to access to your Apache Usergrid application. Because the scope of
access that the other authentication levels provide is so broad (and as
a result, so powerful), it's a bad practice to use them from a mobile
app. Instead, they're better suited to other client apps, such as web
applications.

The following table describes each of the authentication levels. Note
that only one of these is configurable using the admin portal. The
others are independent of Application User level access, which is
governed by permission rules you define (usually with the admin portal).
In contrast, the level of acces provided by the others is only as
described in the notes.

+--------------------+--------------------+--------------------+--------------------+
| Authentication     | Description        | Permissions        | Notes              |
| Level              |                    | Configurable in    |                    |
|                    |                    | Admin Portal       |                    |
+====================+====================+====================+====================+
| Application User   | Allows access to   | Yes                | You allow specific |
|                    | your Apache Usergrid  |                    | access to          |
|                    | application as     |                    | application        |
|                    | governed by the    |                    | resources by       |
|                    | permission rules   |                    | creating           |
|                    | you create and     |                    | permission rules,  |
|                    | associated with    |                    | then associating   |
|                    | users and user     |                    | these rules with   |
|                    | groups.            |                    | users. Typically,  |
|                    |                    |                    | you'll begin by    |
|                    |                    |                    | creating roles,    |
|                    |                    |                    | then create        |
|                    |                    |                    | permission rules   |
|                    |                    |                    | for the roles,     |
|                    |                    |                    | then associate the |
|                    |                    |                    | roles with users   |
|                    |                    |                    | (or groups of      |
|                    |                    |                    | users).            |
|                    |                    |                    |                    |
|                    |                    |                    | Each Application   |
|                    |                    |                    | User is            |
|                    |                    |                    | represented by a   |
|                    |                    |                    | User entity in     |
|                    |                    |                    | your Apache Usergrid  |
|                    |                    |                    | application. (For  |
|                    |                    |                    | more about the     |
|                    |                    |                    | User entity, see   |
|                    |                    |                    | [User](/user).)    |
|                    |                    |                    |                    |
|                    |                    |                    | For more about     |
|                    |                    |                    | roles and          |
|                    |                    |                    | permissions, see   |
|                    |                    |                    | [Managing access   |
|                    |                    |                    | by defining        |
|                    |                    |                    | permission         |
|                    |                    |                    | rules](/managing-a |
|                    |                    |                    | ccess-defining-per |
|                    |                    |                    | mission-rules).    |
|                    |                    |                    | For a look at how  |
|                    |                    |                    | security features  |
|                    |                    |                    | fit together, see  |
|                    |                    |                    | [App Security      |
|                    |                    |                    | Overview](/app-sec |
|                    |                    |                    | urity-overview).   |
+--------------------+--------------------+--------------------+--------------------+
| Application        | Allows full access | No                 | Authentication at  |
|                    | to perform any     |                    | this level is      |
|                    | operation on an    |                    | useful in a        |
|                    | Apache Usergrid       |                    | server-side        |
|                    | application        |                    | application (not a |
|                    | account (but not   |                    | mobile app) that   |
|                    | other applications |                    | needs access to    |
|                    | within the same    |                    | resources through  |
|                    | organization).     |                    | the Apache Usergrid   |
|                    | Should not be used |                    | API.               |
|                    | from a mobile      |                    |                    |
|                    | client.            |                    | Imagine that you   |
|                    |                    |                    | created a web site |
|                    |                    |                    | that lists every   |
|                    |                    |                    | hiking trail in    |
|                    |                    |                    | the Rocky          |
|                    |                    |                    | Mountains. Anyone  |
|                    |                    |                    | can go to the web  |
|                    |                    |                    | site and view the  |
|                    |                    |                    | content. However,  |
|                    |                    |                    | you don't want     |
|                    |                    |                    | anyone to have     |
|                    |                    |                    | access to the App  |
|                    |                    |                    | Services API,      |
|                    |                    |                    | where all the data |
|                    |                    |                    | is stored. But you |
|                    |                    |                    | do want to give    |
|                    |                    |                    | your web server    |
|                    |                    |                    | access so that it  |
|                    |                    |                    | can generate the   |
|                    |                    |                    | pages to serve to  |
|                    |                    |                    | the website’s      |
|                    |                    |                    | visitors.          |
|                    |                    |                    |                    |
|                    |                    |                    | **Warning:** You   |
|                    |                    |                    | should never       |
|                    |                    |                    | authenticate this  |
|                    |                    |                    | way from a         |
|                    |                    |                    | client-side app    |
|                    |                    |                    | such as a mobile   |
|                    |                    |                    | app. A hacker      |
|                    |                    |                    | could analyze your |
|                    |                    |                    | app and extract    |
|                    |                    |                    | the credentials    |
|                    |                    |                    | for malicious use  |
|                    |                    |                    | even if those      |
|                    |                    |                    | credentials are    |
|                    |                    |                    | compiled and in    |
|                    |                    |                    | binary format. See |
|                    |                    |                    | [Safe mobile       |
|                    |                    |                    | access](#safe_mobi |
|                    |                    |                    | le)                |
|                    |                    |                    | for additional     |
|                    |                    |                    | considerations in  |
|                    |                    |                    | keeping access to  |
|                    |                    |                    | your app and its   |
|                    |                    |                    | data secure.       |
+--------------------+--------------------+--------------------+--------------------+
| Admin User         | Allows full access | No                 | This               |
|                    | to perform any     |                    | authentication     |
|                    | operation on all   |                    | level is useful    |
|                    | organization       |                    | from applications  |
|                    | accounts of which  |                    | that provide       |
|                    | the admin user is  |                    | organization-wide  |
|                    | a member. Should   |                    | administration     |
|                    | not be used from a |                    | features. For      |
|                    | mobile client.     |                    | example, the App   |
|                    |                    |                    | Services admin     |
|                    |                    |                    | portal uses this   |
|                    |                    |                    | level of access    |
|                    |                    |                    | because it         |
|                    |                    |                    | requires full      |
|                    |                    |                    | access to the      |
|                    |                    |                    | administration     |
|                    |                    |                    | features.          |
|                    |                    |                    |                    |
|                    |                    |                    | Unless you have a  |
|                    |                    |                    | specific need for  |
|                    |                    |                    | administrative     |
|                    |                    |                    | features, such as  |
|                    |                    |                    | to run test        |
|                    |                    |                    | scripts that       |
|                    |                    |                    | require access to  |
|                    |                    |                    | management         |
|                    |                    |                    | functionality, you |
|                    |                    |                    | should not use the |
|                    |                    |                    | admin user         |
|                    |                    |                    | authentication     |
|                    |                    |                    | level.             |
|                    |                    |                    |                    |
|                    |                    |                    | **Note:**          |
|                    |                    |                    | Currently,         |
|                    |                    |                    | organization and   |
|                    |                    |                    | admin user access  |
|                    |                    |                    | are effectively    |
|                    |                    |                    | the same.          |
|                    |                    |                    | Eventually, the    |
|                    |                    |                    | admin user level   |
|                    |                    |                    | will be a          |
|                    |                    |                    | configurable       |
|                    |                    |                    | subset of the      |
|                    |                    |                    | organization level |
|                    |                    |                    | of access.         |
|                    |                    |                    | **Warning:** You   |
|                    |                    |                    | should never       |
|                    |                    |                    | authenticate this  |
|                    |                    |                    | way from a         |
|                    |                    |                    | client-side app    |
|                    |                    |                    | such as a mobile   |
|                    |                    |                    | app. A hacker      |
|                    |                    |                    | could analyze your |
|                    |                    |                    | app and extract    |
|                    |                    |                    | the credentials    |
|                    |                    |                    | for malicious use  |
|                    |                    |                    | even if those      |
|                    |                    |                    | credentials are    |
|                    |                    |                    | compiled and in    |
|                    |                    |                    | binary format. See |
|                    |                    |                    | [Safe mobile       |
|                    |                    |                    | access](#safe_mobi |
|                    |                    |                    | le)                |
|                    |                    |                    | for additional     |
|                    |                    |                    | considerations in  |
|                    |                    |                    | keeping access to  |
|                    |                    |                    | your app and its   |
|                    |                    |                    | data secure.       |
+--------------------+--------------------+--------------------+--------------------+
| Organization       | Full access to     | No                 | Providing the      |
|                    | perform any        |                    | greatest amount of |
|                    | operation on an    |                    | access, this       |
|                    | Apache Usergrid       |                    | authentication     |
|                    | organization.      |                    | level lets a       |
|                    | Should not be used |                    | client perform any |
|                    | from a mobile      |                    | operation on an    |
|                    | client.            |                    | Apache Usergrid       |
|                    |                    |                    | organization. This |
|                    |                    |                    | level of access    |
|                    |                    |                    | should be used     |
|                    |                    |                    | sparingly and      |
|                    |                    |                    | carefully.         |
|                    |                    |                    |                    |
|                    |                    |                    | **Note:**          |
|                    |                    |                    | Currently,         |
|                    |                    |                    | organization and   |
|                    |                    |                    | admin user access  |
|                    |                    |                    | are effectively    |
|                    |                    |                    | the same.          |
|                    |                    |                    | Eventually, the    |
|                    |                    |                    | admin user level   |
|                    |                    |                    | will be a          |
|                    |                    |                    | configurable       |
|                    |                    |                    | subset of the      |
|                    |                    |                    | organization level |
|                    |                    |                    | of access.         |
|                    |                    |                    | **Warning:** You   |
|                    |                    |                    | should never       |
|                    |                    |                    | authenticate this  |
|                    |                    |                    | way from a         |
|                    |                    |                    | client-side app    |
|                    |                    |                    | such as a mobile   |
|                    |                    |                    | app. A hacker      |
|                    |                    |                    | could analyze your |
|                    |                    |                    | app and extract    |
|                    |                    |                    | the credentials    |
|                    |                    |                    | for malicious use  |
|                    |                    |                    | even if those      |
|                    |                    |                    | credentials are    |
|                    |                    |                    | compiled and in    |
|                    |                    |                    | binary format. See |
|                    |                    |                    | [Safe mobile       |
|                    |                    |                    | access](#safe_mobi |
|                    |                    |                    | le)                |
|                    |                    |                    | for additional     |
|                    |                    |                    | considerations in  |
|                    |                    |                    | keeping access to  |
|                    |                    |                    | your app and its   |
|                    |                    |                    | data secure.       |
+--------------------+--------------------+--------------------+--------------------+

Adding code to support authentication
-------------------------------------

### Using an access token

When you obtain an access token, you must provide it with every
subsequent API call that you make. There are two ways to provide your
access token.

-   You can add the token to the API query string:\
    \

        https://api.usergrid.com/{org-name}/{app-name}/users?access_token={access_token}

-   You can include the token in an HTTP authorization header:\
    \

        Authorization: Bearer {access_token}

**Note:** The App services documentation assumes you are providing a
valid access token with every API call whether or not it is shown
explicitly in the examples. Unless the documentation specifically says
that you can access an API endpoint without an access token, you should
assume that you must provide it. One application that does not require
an access token is the sandbox application. The Guest role has been
given full permissions (/\*\* for GET, POST, PUT, and DELETE) for this
application. This eliminates the need for a token when making
application level calls to the sandbox app. For further information on
specifying permissions, see [Managing access by defining permission
rules](/managing-access-defining-permission-rules).

### Authenticating as Application User

Using the username and password values specified when the user entity
was created, your app can connect to the Apache Usergrid application
endpoint to request an access token. (Note that it's also possible to
use the user's email address in place of the username.) Here is an
example in cURL format of a request for application user access:

    curl -X POST -i -H "Content-Type: application/json" "https://api.usergrid.com/my-org/my-app/token" -d '{"grant_type":"password","username":"john.doe","password":"testpw"}'

The results include the access token needed to make subsequent API
requests on behalf of the application user:

    {
    "access_token": "5wuGd-lcEeCUBwBQVsAACA:F8zeMOlcEeCUBwBQVsAACA:YXU6AAABMq0hdy4Lh0ewmmnOWOR-DaepCrpWx9oPmw",
    "expires_in": 3600,
    "user": {
    ...
    }
    }
        

### Authenticating as Application

Using your app’s client id and client secret values, your app can
connect to the Apache Usergrid application endpoint to request an access
token. Here is an example in cURL format of a request for application
access:

    curl -X POST -i -H "Content-Type: application/json" "https://api.usergrid.com/my-org/my-app/token" -d '{"grant_type":"client_credentials","client_id":"YXB7NAD7EM0MEeJ989xIxPRxEkQ","client_secret":"YXB7NAUtV9krhhMr8YCw0QbOZH2pxEf"}'

The results include the access token needed to make subsequent API
requests on behalf of the application:

    {
    "access_token": "F8zeMOlcEeCUBwBQVsAACA:YXA6AAABMq0d4Mep_UgbZA0-sOJRe5yWlkq7JrDCkA",
    "expires_in": 3600,
    "application": {
    ...  
    }
    }
        

**Warning:** You should never authenticate this way from a client-side
app such as a mobile app. A hacker could analyze your app and extract
the credentials for malicious use even if those credentials are compiled
and in binary format. See [Safe mobile access](#safe_mobile) for
additional considerations in keeping access to your app and its data
secure.

### Authenticating as Admin User

If you do require admin user access, your app can connect to the App
Services management endpoint to request an access token. Your app
supplies the username and password of an admin user in the request. Here
is an example in cURL format of a request for admin user access:

    curl -X POST -i -H "Content-Type: application/json" "https://api.usergrid.com/management/token"  -d '{"grant_type":"password","username":"testadmin","password":"testadminpw"}'

The results include the access token needed to make subsequent API
requests on behalf of the admin user:

    {
    "access_token": "f_GUbelXEeCfRgBQVsAACA:YWQ6AAABMqz_xUyYeErOkKjnzN7YQXXlpgmL69fvaA",
    "expires_in": 3600,
    "user": {
    ...
    }
    }
        

**Warning:** You should never authenticate this way from a client-side
app such as a mobile app. A hacker could analyze your app and extract
the credentials for malicious use even if those credentials are compiled
and in binary format. See [Safe mobile access](#safe_mobile) for
additional considerations in keeping access to your app and its data
secure.

### Authenticating as Organization

If you do require organization level access, your app can connect to the
Apache Usergrid management endpoint to request an access token. Access to
an organization requires the client id and client secret credentials.
Here is an example in cURL format of a request for organization access:

    curl -X POST -i -H "Content-Type: application/json" "https://api.usergrid.com/management/token" -d '{"grant_type":"client_credentials","client_id":"YXB7NAD7EM0MEeJ989xIxPRxEkQ","client_secret":"YXB7NAUtV9krhhMr8YCw0QbOZH2pxEf"}'

The results include the access token needed to make subsequent API
requests to the organization:

    {
    "access_token": "gAuFEOlXEeCfRgBQVsAACA:b3U6AAABMqz-Cn0wtDxxkxmQLgZvTMubcP20FulCZQ",
    "expires_in": 3600,
    "organization": {
    ...
    }
    }
        

**Warning:** You should never authenticate this way from a client-side
app such as a mobile app. A hacker could analyze your app and extract
the credentials for malicious use even if those credentials are compiled
and in binary format. See [Safe mobile access](#safe_mobile) for
additional considerations in keeping access to your app and its data
secure.

Authentication token time to live
---------------------------------

An access token has a “time-to-live”, which is the maximum time that the
access token will be valid for use within the application, specified in
milliseconds. By default, all tokens have a system-defined time-to-live
of 24 hours.

### Changing the default time-to-live

You can change the default time-to-live for all application tokens by
updating the Application entity’s accesstokenttl property. 

For example, the following updates an application entity to have a
default time-to-live value of 1800000 miliseconds (30 minutes) for all
tokens:

    curl -X PUT -i -H "Content-Type: application/json" "https://api.usergrid.com/my-org/my-app" -d '{"accesstokenttl":"1800000"}'

### Changing token time-to-live

When you request an access token, you can override its time-to-live by
including a ttl parameter in the request.

The ttl value must be equal to or less than the value of the
accesstokenttl property. If you specify a ttl value greater than the
value of accesstokenttl, an error message is returned that indicates the
maximum time to live value that can be specified.

**Note: **If you set ttl=0, the token will never expire. This can pose a
security risk and should be used with caution.

For example, the following sets a time to live value of 1800000
miliseconds (30 minutes) for an admin user:

    curl -X POST -i -H "Content-Type: application/json" "https://api.usergrid.com/management/token?ttl=1800000" -d '{"grant_type":"client_credentials","client_id":"YXB7NAD7EM0MEeJ989xIxPRxEkQ","client_secret":"YXB7NAUtV9krhhMr8YCw0QbOZH2pxEf"}'

The following sets the same time to live value for an application user.

    curl -X POST -i -H "Content-Type: application/json" "https://api.usergrid.com/my-org/my-app/token?ttl=1800000" -d '{"grant_type":"password","username":"testadmin","password":"testadminpw"}'

Revoking authentication tokens
------------------------------

Under certain circumstances, you may need to explicitly revoke a user's
access token, such as when a user logs out. To revoke a specific
authentication token, send the following `PUT` request:

    curl -X PUT https://api.usergrid.com/<org_name>/<app_name>/users/<user_uuid_or_username>/revoketokens?token="<token>"

If the token is successfully revoked, you will receive the following
response from the API:

    {
      "action" : "revoked user token",
      "timestamp" : 1382050891455,
      "duration" : 24
    }

Safe mobile access
------------------

For mobile access, it is recommended that you connect as an application
user with configured access control policies. Mobile applications are
inherently untrusted because they can be easily examined and even
decompiled.

Any credentials stored in a mobile app should be considered secure only
to the Application User level. This means that if you don’t want the
user to be able to access or delete data in your Apache Usergrid
application, you need to make sure that you don’t enable that capability
through roles or permissions. Because most web applications talk to the
database using some elevated level of permissions, such as root, it’s
generally a good idea for mobile applications to connect with a more
restricted set of permissions. For more information on restricting
access through permission rules, see [Managing access by defining
permission rules](/managing-access-defining-permission-rules).
