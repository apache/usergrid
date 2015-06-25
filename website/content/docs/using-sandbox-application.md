---
title: Using a Sandbox Application
category: docs
layout: docs
---

Using a Sandbox Application
===========================

When you create a new Apigee account (see [Creating an Apigee
Account](/creating-apigee-account)) for Apache Usergrid, Apigee creates a
new application for you on its servers. With the new application, called
"sandbox," you can add your own example data and try out API calls that
do things with the data. Be sure to see [Using the API](/using-api) for
suggestions.

To keep things simple and make it easier for you to try things out, the
sandbox application has all authentication disabled. That way, it
doesn’t require an access token for application-level calls to the API.
Permissions are so open on the sandbox application because its "guest"
role offers full permissions for all access paths -- that is, GET, POST,
PUT, and DELETE for /\*\*. Learn more about roles and permissions in
[Managing access by defining permission
rules](/managing-access-defining-permission-rules).

### A note about security

Keep in mind that the lack of authentication means that **a sandbox
application is not secure enough for important or sensitive data**. A
sandbox is just for experimentation while you learn how Apache Usergrid
works, and should never be used for a production application.

As with other kinds of Apache Usergrid applications, a sandbox application
is an area of the Apache Usergrid data store where you can put your own
data. You can create as many other applications as you like, including
more sandbox applications. When it comes to production (secured)
applications, a good rule of thumb is to create one Apache Usergrid
application for each mobile app you develop.

Creating a New Sandbox Application
----------------------------------

You may want to create (or re-create) a sandbox application. For
example, you may want to create a sandbox application for another
organization or you may want to create another application for testing
purposes.

Giving the guest role full permissions should be used only for testing
and should not be used in production. Before you make your app “live”,
you should remove the guest permissions for /\*\*.

Use the following steps to create a sandbox app:

1.  Create a new application using the admin portal. You can name the
    application whatever you like (including "sandbox").
2.  Set full access permissions for the guest role, as follows:
    1.  In the admin portal, click **Users**, then click **Roles**.
    2.  On the **Roles** page, in the list of roles, click **Guest**.
    3.  For the Guest role, under** Permissions**, click **Add
        Permission**.
    4.  In the **New Permission** dialog, enter the following in the
        **Path** box:\

            /**

    5.  Select the following check boxes: **get**, **post**, **put**,
        and **delete**.
    6.  Click the **Add** button.
    7.  If there are other permissions listed, delete them.


