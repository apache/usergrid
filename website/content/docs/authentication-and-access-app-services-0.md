---
title: Authentication and access in App services
category: docs
layout: docs
---

Authentication and access in App services
=========================================

App services requests are authenticated via OAuth (Open Authorization)
2.0. OAuth is an authentication mechanism that allows users to grant
access to their web resources or mobile apps safely, without having to
share their passwords. The analogy of a valet key is sometimes used to
describe OAuth, because users can permit general access, but limit
access rights to perform certain operations. Instead of an app user
having to share a password, OAuth enables access using a security token
tied specifically to an app and device.

Unlike OAuth 1.0, which requires special support in the client code for
signing requests, OAuth 2.0 can be used by any web service client
libraries. Although the OAuth 2.0 specification isn’t finalized yet, it
is sufficiently complete so that many web service providers, including
Google and Facebook, are now using it for authentication. More
information about OAuth 2.0 is available at
[oauth.net](http://oauth.net/2/).

Access types
------------

App services take advantage of standard OAuth 2.0 mechanisms that
require an access token with data operation requests. To obtain the
access token, you connect to an appropriate web service endpoint and
provide the correct client credentials. The credentials required to get
the token depend on the type of access you need.

There are four access types.

  Access Type        Description
  ------------------ -------------------------------------------------------------------------------------------------------
  Organization       Full access to perform any operation on a App services organization account
  Admin User         Full access to perform any operation on all organization accounts of which the admin user is a member
  Application        Full access to perform any operation on a App services application account
  Application User   Policy-limited access to perform operations on a App services application account

The *Organization* and *Application* access types are intended for
server-side applications because they are "superuser" access mechanisms
with few constraints on what they are permitted to do. When connecting
via OAuth, you supply organization or application client ID and client
secret parameters as client credentials. The Home page of the developer
portal shows these parameters for the organization, while the Settings
page displays them for the currently selected app.

The *Admin User* and *Application User* access types are appropriate for
connections on behalf of a known user account, such as an admin user who
is a member of several organizations and has management rights to
several apps, or an application user authorized for an application. When
connecting via OAuth on behalf of a known user, you supply username and
password parameters as client credentials.

Each access type is discussed below, along with information on supplying
an access token in an API request.

### Organizations

When you sign up for the App services developer portal, you create an
organization account in addition to your personal admin user account.
Access to a single organization requires client ID and client secret
credentials (or an admin user can supply username and password
credentials, as shown in the next section).

As an example of accessing an organization, you can obtain your client
ID and client secret values from the developer portal and connect to the
following URL (substituting the correct values for \<client\_id\> and
\<client\_secret\>):

    https://api.usergrid.com/management/token?grant_type=client_credentials&client_id=<client_id>&client_secret=<client_secret>

The results show the access token needed to make subsequent API
requests, as well as additional information about the organization:

    {
      "access_token": "gAuFEOlXEeCfRgBQVsAACA:b3U6AAABMqz-Cn0wtDxxkxmQLgZvTMubcP20FulCZQ",
      "expires_in": 3600,
      "organization": {
        "users": {
          "test": {
            "name": "Test User",
            "disabled": false,
            "uuid": "7ff1946d-e957-11e0-9f46-005056c00008",
            "activated": true,
            "username": "test",
            "applicationId": "00000000-0000-0000-0000-000000000001",
            "email": "test@usergrid.com",
            "adminUser": true,
            "mailTo": "Test User <test@usergrid.com>"
          }
        },
        "name": "test-organization",
        "applications": {
          "test-app": "8041893b-e957-11e0-9f46-005056c00008"
        },
        "uuid": "800b8510-e957-11e0-9f46-005056c00008"
      }
    }

### Admin users

*Admin Users* are users of the Usergrid.com service as well as members
of one or more organizations. In turn, an organization can have one or
more admin users. Currently all admin users in an organization have full
access permissions after they authenticate using their basic username
and password credentials. In a subsequent release, App services will
support a more fine-grained, delegated administration model in which
access rights for admin users are configurable.

As an example. to authenticate as an admin user, use the username and
password values specified when you created your admin user account and
connect to the following URL (substituting the correct values for
\<username\> and \<password\>):

    https://api.usergrid.com/management/token?grant_type=password&username=<username>&password=<password>

In these results, note the access token needed to make subsequent API
requests on behalf of the admin user:

    {
      "access_token": "f_GUbelXEeCfRgBQVsAACA:YWQ6AAABMqz_xUyYeErOkKjnzN7YQXXlpgmL69fvaA",
      "expires_in": 3600,
      "user": {
        "username": "test",
        "email": "test@usergrid.com",
        "organizations": {
          "test-organization": {
            "users": {
              "test": {
                "name": "Test User",
                "disabled": false,
                "uuid": "7ff1946d-e957-11e0-9f46-005056c00008",
                "activated": true,
                "username": "test",
                "applicationId": "00000000-0000-0000-0000-000000000001",
                "email": "test@usergrid.com",
                "adminUser": true,
                "mailTo": "Test User <test@usergrid.com>"
              }
            },
            "name": "test-organization",
            "applications": {
              "test-app": "8041893b-e957-11e0-9f46-005056c00008"
            },
            "uuid": "800b8510-e957-11e0-9f46-005056c00008"
          }
        },
        "adminUser": true,
        "activated": true,
        "name": "Test User",
        "mailTo": "Test User <test@usergrid.com>",
        "applicationId": "00000000-0000-0000-0000-000000000001",
        "uuid": "7ff1946d-e957-11e0-9f46-005056c00008",
        "disabled": false
      }
    }

Applications
------------

Users can access applications in three ways:

-   With application client ID and client secret credentials
-   With the client ID and client secret credentials of the organization
    that owns the application
-   With username and password credentials of an admin user associated
    with the application’s organization

Using your client ID and client secret values (obtained from the
Application Settings section of the developer portal), you can connect
to the following URL (substituting the correct values for
\<org-name\>,\<app-name\>, \<client\_id\>, and \<client\_secret\>):

    https://api.usergrid.com/<org-name>/<app-name>/token?grant_type=client_credentials&client_id=<client_id>&client_secret=<client_secret>

The results show the access token needed to make subsequent API requests
on behalf of the application:

    {
      "access_token": "F8zeMOlcEeCUBwBQVsAACA:YXA6AAABMq0d4Mep_UgbZA0-sOJRe5yWlkq7JrDCkA",
      "expires_in": 3600,
      "application": {
        "name": "test-app",
        "id": "17ccde30-e95c-11e0-9407-005056c00008"
      }
    }

Application users
-----------------

*Application Users* are members of the "users" collection within an
application. They are the actual users of an app and their data is
stored separately from any other app in App services.

Application users can authenticate with either basic username/password
credentials or OAuth client ID and client secret credentials. When
authenticated, these users can access App services entities depending on
their assigned permissions, their roles, and the permissions assigned to
those roles.

Using the username and password values specified when the application
user was created, you can connect to the following URL (substituting the
correct values for \<org-name\>,\<app-name\>, \<username\>, and
\<password\>):

    https://api.usergrid.com/management/<org-name>/<app-name>/token?grant_type=password&username=<username>&password=<password>

The results show the access token needed to make subsequent API requests
on behalf of the application user:

    {
      "access_token": "5wuGd-lcEeCUBwBQVsAACA:F8zeMOlcEeCUBwBQVsAACA:YXU6AAABMq0hdy4Lh0ewmmnOWOR-DaepCrpWx9oPmw",
      "expires_in": 3600,
      "user": {
        "uuid": "e70b8677-e95c-11e0-9407-005056c00008",
        "type": "user",
        "username": "edanuff",
        "email": "ed@anuff.com",
        "activated": true,
        "created": 1317164604367013,
        "modified": 1317164604367013
      }
    }

Using an access token
---------------------

When you obtain an access token, you must provide it with every
subsequent API call that you make. There are two ways to provide your
access token.

-   You can add the token to the API querystring:\
    \

        https://api.usergrid.com/<org-name>/<app-name>/users?access_token=<access_token>

-   You can include the token in an HTTP authorization header:\
    \

        Authorization: Bearer <access_token>

**Note:** The App services documentation assumes you are providing a
valid access token with every API call whether or not it is shown
explicitly in the examples. Unless the documentation specifically says
that you can access an API endpoint without an access token, you should
assume that you must provide it.

Safe mobile access
------------------

For mobile access, it is recommended that you connect as an application
user with configured access control policies. Mobile applications are
inherently untrusted because they can be easily examined and even
decompiled.

Any credentials stored in a mobile app should be considered secure only
to the level of the application user. This means that if you don’t want
the user to be able to access or delete data in your App services
application, you need to make sure that you don’t enable that capability
via roles or permissions. Because most web applications talk to the
database using some elevated level of permissions, such as root, it’s
generally a good idea for mobile applications to connect with a more
restricted set of permissions. For more information, see [Managing
access by defining permission
rules](/managing-access-defining-permission-rules).
