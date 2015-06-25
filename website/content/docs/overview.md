---
title: App Security Overview
category: docs
layout: docs
---

App Security Overview
=====================

Any app you put into production should feature security that protects
your app, your users, and your app's data. Implementing security means
taking steps in your mobile app's code and in your Apache Usergrid
application.

**Warning:** When you register for Apache Usergrid, you get a sandbox
application that you can use to try things out. This application is not
for use in production. By default, the sandbox application is not
protected by any security measures whatsoever. Use the sandbox only for
experimentation, and only with data that isn't in any way sensitive.

When securing your app, follow these high-level steps:

1.  In your Apache Usergrid application, use the admin portal to define
    your app users' access to your app's data and features. You do this
    by creating permission rules, then associating those rules with your
    users. For more information, see [Managing access by defining
    permission rules](/managing-access-defining-permission-rules).
2.  In your app, write code through which your app's users can verify
    who they are to your Apache Usergrid application. You do this by
    writing code that uses their username and password as credentials to
    initially authenticate with the Apache Usergrid application, then uses
    a token thereafter. (This authentication style supports the OAuth
    2.0 model.) For more information, see [Authenticating users and
    application clients](/authenticating-users-and-application-clients).
3.  Be sure to use coding best practices that help ensure that your app
    is protected from malicious attacks. For more information, see
    [Securing your app](/securing-your-app).

The following illustration describes these high-level areas.

![](/docs/sites/docs/files/styles/large/public/security_model_0.png?itok=_fErNYbL)
