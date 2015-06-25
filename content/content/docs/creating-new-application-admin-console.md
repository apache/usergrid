---
title: Creating a New Application with the Admin Console
category: docs
layout: docs
---

Creating a New Application with the Admin Console
=================================================

You can use the admin portal to create Apache Usergrid applications. An App
Services application represents the data associated with your app.
Through an application, you handle the entities associated with your
app, including users, devices, events, and so on.

> For more about how data in Apache Usergrid fits together, see [App
> Services Data model](/app-services-data-model-1).

Conceptually speaking, an application is like a database. You can create
multiple applications within your Apache Usergrid organization. 

 

You’ll typically have one Apache Usergrid application for each of your
apps. But you might want to create different applications representing
sandbox, development, and production instances of your app.

To create a new application with the admin console:

1.  In the admin console, from the dropdown menu at the far top left,
    select the organization to which you will add the new application.
2.  Click the **ADD NEW APP** button, located at the top of the screen,
    next to the application drop-down.
3.  In the dialog box, enter a new application name, then click the
    **Create** button.
4.  Your new application will automatically be selected in the
    applications drop-down menu.

### Next Steps

After you've created a new application, finish up by thinking about the
following:

-   Have you taken steps to make your application secure?

    If this is going to be a production application used by an app
    publicly, be sure to configure security roles that provide only the
    level of access your users will need. (You should never use a
    sandbox application as a public database. A sandbox is for
    experimenting with example data.)

    For more on security, see [Securing your app](/securing-your-app).
    For more about sandbox applications, see [Using a Sandbox
    Application](/using-sandbox-application).


