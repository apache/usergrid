---
title: Managing access by defining permission rules
category: docs
layout: docs
---

Managing access by defining permission rules
============================================

You control your app users' access to application resources by defining
roles and permission rules. In your Apache Usergrid application, you assign
application users a *role* that represents a set of permissions. Through
these permissions, you allow users to perform certain operations (GET,
POST, PUT, or DELETE) on specific resources. When the user submits a
request via your app code to the Apache Usergrid API, the user’s
permissions are checked against the resource paths that the user is
trying to access. The request succeeds only if access to the resource is
allowed by the permission rules you specify.

You specify roles for unauthenticated users and those who authenticate
as an Application User, as defined in [Authenticating users and
application clients](/authenticating-users-and-application-clients).
Roles are not applied for the other authentication levels: Application,
Admin User, and Organization. Access at these levels can't be restricted
by roles and permission rules. You should allow clients to authenticate
at these levels sparingly and carefully.

Roles included by default
-------------------------

When defining user access to your application's data, you create roles,
specify permission rules for them, then associate users with the roles.
Apache Usergrid includes three predefined roles when you create an
application.

The following table lists the three roles included by default. Note that
two of these are in effect and applied from the time your application is
created (until you change them). Apache Usergrid applies the following
default behavior:

1.  An unauthenticated user is automatically added to the Guest role so
    that they can register for a user account.
2.  A user who has a user account and authenticates with it is
    automatically added to the Default role. **Note that by default,
    this role is very permissive.** Be sure to restrict it with specific
    permission rules before deploying to production.

+-------------------------+-------------------------+-------------------------+
| Role                    | Description             | Notes                   |
+=========================+=========================+=========================+
| Guest                   | Default for             | Grants permission for a |
|                         | unauthenticated users.  | user to create a user   |
|                         | Includes a basic set of | account and for their   |
|                         | permissions for         | device to be            |
|                         | unregistered or         | registered. You can     |
|                         | unauthenticated users.  | change permission rules |
|                         | Users are automatically | based on your goals for |
|                         | added to the Guest role | unregistered user       |
|                         | before they’re          | access. This role is    |
|                         | authenticated. After    | designed to provide     |
|                         | they’re authenticated,  | access for people who   |
|                         | users are automatically | haven't yet registered, |
|                         | added to the Default    | and allow them to       |
|                         | role.                   | register.               |
+-------------------------+-------------------------+-------------------------+
| Default                 | Default for             | By default, **grants    |
|                         | authenticated users.    | full access for all     |
|                         | Includes permissions    | resources in your       |
|                         | for the set of          | application**. A first  |
|                         | operations you want an  | task in securing your   |
|                         | authenticated user to   | application should be   |
|                         | be able to perform.     | to restrict access by   |
|                         | Users are added to this | redefining this role to |
|                         | role after they're      | narrow the access it    |
|                         | authenticated.          | provides. Remove the    |
|                         |                         | default full permission |
|                         |                         | rule and add            |
|                         |                         | restrictive permission  |
|                         |                         | rules for a production  |
|                         |                         | deployment.             |
+-------------------------+-------------------------+-------------------------+
| Administrator           | Unused until you        | Grants no access.       |
|                         | associate it with users | Consider this a blank   |
|                         | or groups. By default,  | slate. Add permission   |
|                         | includes no permissions | rules and associate     |
|                         | that provide access.    | this role with users    |
|                         |                         | and groups as needed.   |
|                         |                         |                         |
|                         |                         | **Note**: The           |
|                         |                         | Administrator role is   |
|                         |                         | *not the same* as an    |
|                         |                         | organization            |
|                         |                         | administrator -- that   |
|                         |                         | is, someone who         |
|                         |                         | authenticates as an     |
|                         |                         | Admin User. The Admin   |
|                         |                         | User is an implicit     |
|                         |                         | user created when you   |
|                         |                         | create an organization. |
|                         |                         | After authenticating,   |
|                         |                         | the Admin User has full |
|                         |                         | access to all of the    |
|                         |                         | administration features |
|                         |                         | of the Apache Usergrid     |
|                         |                         | API. By comparison, the |
|                         |                         | Administrator role is   |
|                         |                         | simply a role           |
|                         |                         | (initially without      |
|                         |                         | permissions) that can   |
|                         |                         | be assigned to any      |
|                         |                         | user.                   |
+-------------------------+-------------------------+-------------------------+

Defining your own roles and permissions
---------------------------------------

When preparing an application for production use, a good first step is
to edit permission rules for the Default role. This role will be applied
for every user who authenticates as an Application User.

The admin portal is the best place to manage roles. While you can manage
roles and permissions programmatically (see [Role](/role)),
security-related calls from a mobile app will pose a security risk.
Consider doing so only from a server-side web application.

For easy-to-read examples, this section expresses permission rules in
this way:

    <operations>:<entity path pattern>

-   \<operations\> is a comma-delimited set of REST operations
    (GET, PUT, POST, DELETE) that are allowed for the specified entity
    path.
-   \<entity path pattern\> is a parameter evaluated using Apache Ant
    pattern matching
    (see [http://ant.apache.org/manual/dirtasks.html\#patterns](http://ant.apache.org/manual/dirtasks.html#patterns)).

For example, in the Default role, first remove the permission rule that
grants full access to all authenticated users. You could then begin by
creating a rule that grants access for the authenticated user to makes
changes only to data associated with their account. 

    GET,PUT,POST,DELETE:/users/me/**

Use the Admin Portal to make role and permission rule changes. On the
left sidebar of the portal, click Users, then click Roles. This displays
the roles defined for the application. To create a role, click the Add
button (it looks like a person's silhouette). To delete a role, select
the role you want to delete and click the Remove button (it looks like a
trash can). To view the privileges in a role, click the role.

Suppose you created a role named "customer". Here’s what the privileges
for the role might look like:

![](/docs/sites/docs/files/worker_permissions.png)

Notice that specific privileges for operations are represented using
checkboxes in the Permission Rules section. The path indicates the
resource path for which the permissions apply. The permissions apply to
all resources in the specified path directory and its subdirectories. As
currently specified, the worker role has GET permission on the base
directory path (/) and all resource paths below it (in other words, all
resource paths).

You can add a permission, by entering the entity path pattern in the
Path field of the Add Permission Rule section, and checking the
operation checkboxes (get, post, put, and delete) as appropriate. For
example, the following adds permission to create a user:

![](/docs/sites/docs/files/add_permission.png)

Click Add, and the permission is added to the role.

![](/docs/sites/docs/files/pemission_list.png)

Permission rule examples
------------------------

Here are some examples to illustrate how permissions are specified:

-   Authenticated user can change any data related to the:\

        POST:/users/*

-   A permission the permits the current user to make any changes to
    resources associated with them:\

        GET,PUT,POST,DELETE:/users/me/**

-   A permission that allows someone to look at a specific user:\

        GET:/users/john.doe

-   A permission that allows the current user to see his activity feed:\

        GET:/users/${user}/feed/*

    The \${user} in the entity path refers to a variable that represents
    the current user’s UUID.

-   A permission allowing linked entities to be read:\

        GET:/users/${user}/**

    The /\*\* in the entity path is a wildcard that matches everything
    under that path. This means that the full specification matches
    multiple resource paths, including, but not limited to, the
    following:

        /users/${user}/feed
        /users/${user}/feed/item1/a/b/c

-   A permission that allows the current user to add himself or another
    user to a group:\

        POST:/groups/${user}/users/**


