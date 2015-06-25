---
title: Securing your app
category: docs
layout: docs
---

Securing your app
=================

There a number of actions you should take to ensure that your app is
secure before you put it into production.

Review permissions in your apps
-------------------------------

Prior to launching your app into a production environment, it is
advisable to review all the roles and permissions you have set up.
During development, you may find that you added various permissions
which may or may not still be required once the app is complete. Review
all permissions and delete any that are no longer required.

Also make sure that you review the roles and permissions that are set up
for the sandbox app. By default, every new Apache Usergrid account has an
app named “sandbox” that is already created under your new organization.
This app is no different than any other app that you might create,
except that the Guest role has been given full permissions (that is,
/\*\* for GET, POST, PUT, and DELETE). This eliminates the need for a
token when making application level calls, and can make it much easier
to get your app up and running.

Prior to taking your app live, you should secure it by removing any
Guest permissions for /\*\*. (See [Managing access by defining
permission rules](/managing-access-defining-permission-rules) for
further information about setting permissions.) After you secure your
the app, any calls to the API will need to include an OAuth token. Oauth
tokens (also called access tokens) are obtained by the API in response
to successful authentication calls. Your app saves the token and uses it
for all future calls during that session. Learn more about access tokens
in [Authenticating users and application
clients](/authenticating-users-and-application-clients).

Review test accounts
--------------------

If you created any test user or test administrator accounts during
development, these should also be reviewed for relevancy and security.
Delete any test accounts that are no longer needed. If these accounts
are still needed, make sure that passwords have been secured to the
standards required by your app.

Use https
---------

Make sure that any calls you make to the API are done using the secure
https protocol, and not the insecure http protocol. The proper
application endpoint is: https://api.usergrid.com

If your app is a web app, that is, an app served by a web server, make
sure that the app is served using https.

Acquire access tokens in a secure way
-------------------------------------

There are various methods for acquiring an access token (see
[Authenticating users and application
clients](/authenticating-users-and-application-clients)). One method is
to use the application or organization level client secret-client id
combination. This method should not be used in client applications (this
is, apps that are deployed to a device, and which authenticate and make
calls against the API).

That’s because a hacker could analyze your app (even a compiled, binary
distribution of your app), and retrieve the secret-id combination. Armed
with this information, an attacker could gain full access to the data in
your account.

Instead, use application user credentials. This means that your app’s
users should provide a username and password. Your app would use these
to authenticate against the API and retrieve an access token.

The client secret-client id combination should be used only in secure,
server-side applications where there is no possibility of a hacker
gaining control of the credentials.
