---
title: Admin portal
category: docs
layout: docs
---

Admin portal
============

The *admin portal* is the primary administrative user interface for
working with App services. The portal is also the most complete
reference application that demonstrates how to incorporate App services
with JavaScript.

The admin portal is available online at
[https://apigee.com/usergrid/](https://apigee.com/usergrid/). The portal
source code is fully open source and forkable, available for download on
GitHub at
[https://github.com/usergrid/portal](https://github.com/usergrid/portal).
You can easily extend the portal, embed it into your own applications,
or inspect the code to learn best practices associated with the App
services API. The App services JavaScript client is also part of the
GitHub portal project.

The portal opens in your default web browser, either Safari, Firefox, or
Google Chrome. The article [Messagee Example](/messagee-example)
describes how to use the portal to run a sample application, whereas
this section describes portal components in more detail and highlights
App services features.

Account Home page
-----------------

When you log in to the portal, you are presented with a Home page for
managing applications and data for your organization. The Home page
displays:

-   Applications associated with the currently selected organization
-   Administrators that are part of that organization
-   API credentials for the organization
-   Activities performed recently by administrators
-   A menu for building, organizing, and managing application content

Applications
------------

For every app that you build, you need a defined application space
within App services.  By default, a test application called "sandbox" is
created for you automatically. For ease of use, this default app has all
authentication disabled and should be used for learning / testing
purposes only. You may also create as many additional applications as
needed.

This unique application space allows each app to have its own private
infrastructure to store its objects and data. In some cases, if you have
multiple apps that share much of the same data and users, you might
represent them in App services as a single application, which would
allow those apps to share the same data space.

### Application Dashboard

The Dashboard shows a variety of statistical data for the selected
application. You can see the activity level, the total number of
entities, entities created in the last day, week, and month, and other
vital statistics for monitoring application health as well as quota
limits.

### Administration for multiple organizations

The portal is designed to let you work within the context of a single
organization at any one point in time. Because it is possible to be an
administrator for multiple organizations, you can switch between
organizations using the menu in the upper right corner.

Authentication credentials
--------------------------

The portal provides a convenient way to set and retrieve credentials for
API access. If you click your email address at the top of the Home page,
you can set your password for both the portal and for OAuth2
username/password Admin-level access. On the Home page for the selected
organization, you can see OAuth2 client credentials for
organization-wide access to applications owned by that organization.
Clicking the Settings button (on the left sidebar) displays OAuth2
credentials for the selected application. See the discussion on the [App
services security model](/app-services-security-model) for more
information on specifying credentials and authentication.

Exploring the API using the shell
---------------------------------

When you first create a new Organization account, there are no
applications associated with the account. To create an application,
click New Application in your organization's Home page. You are
presented with a dialog box that prompts you for the information needed
to create a new app. When the app is created, a number of options are
enabled in the left sidebar for viewing and administering that
application's data. Before you get started with the high-level tools,
create a new application, go to the left sidebar, and click the Shell
button to try out a few basic API commands.

### Entering API requests in the shell

The shell interface is a simple way to get started using the App
services API. It provides a command-line environment within your web
browser for trying out interactive commands. One thing to keep in mind
when using the shell is that App services API endpoints usually take the
form of:

    https://api.usergrid.com/my-org-id/my-app-id/users

where my-org-id is the organization identifier(uuid), my-app-id is your
application identifier(uuid) and users refers to the collection of user
objects defined for your application. Refer to the [Using the
API](/using-api) section for more information on constructing API
requests.

When using the shell, you can abbreviate a fully qualified path name,
omitting everything through the application identifier. The above URL
request, which lists all the users in the application my-app-id, simply
becomes:

    /users

If you type /users into the shell, it's the same as an HTTP GET command
to the API endpoint for the currently selected portal application. (The
current application is selected using the Applications menu in the left
sidebar.)

To create a new user entity using the shell, enter the following and
press return:

    post /users {"username":"john.doe","email":"john.doe@gmail.com"}

Note that there is a space after the path and before the JSON
representation.

You've now created a new user in your application. You should see
something like the following displayed as a result:

    > post /users {"username":"john.doe","email":"john.doe@gmail.com"}
    /users
    {
      "action": "post",
      "application": "1c8f60e4-da67-11e0-b93d-12313f0204bb",
      "params": {},
      "path": "/users",
      "uri": "https://api.usergrid.com/1c8f60e4-da67-11e0-b93d-12313f0204bb/users",
      "entities": [
        {
          "uuid": "0e47f040-da71-11e0-b93d-12313f0204bb",
          "type": "user",
          "username": "john.doe",
          "email": "john.doe@gmail.com",
          "created": 1315523992687007,
          "modified": 1315523992687007,
          "metadata": {
            "path": "/users/0e47f040-da71-11e0-b93d-12313f0204bb",
            "collections": {
              "activities": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/activities",
              "feed": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/feed",
              "groups": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/groups",
              "messages": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/messages",
              "queue": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/queue",
              "roles": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/roles",
              "following": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/following",
              "followers": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/followers"
            },
            "sets": {
              "rolenames": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/rolenames",
              "permissions": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/permissions"
            }
          }
        }
      ],
      "timestamp": 1315523992585,
      "duration": 162
    }
    >

Enter the following to create another user:

    post /users {"username":"jane.doe","email":"jane.doe@gmail.com"}

You should see something like the following result:

    > post /users {"username":"jane.doe","email":"jane.doe@gmail.com"}
    /users
    {
      "action": "post",
      "application": "1c8f60e4-da67-11e0-b93d-12313f0204bb",
      "params": {},
      "path": "/users",
      "uri": "https://api.usergrid.com/1c8f60e4-da67-11e0-b93d-12313f0204bb/users",
      "entities": [
        {
          "uuid": "78c54a82-da71-11e0-b93d-12313f0204bb",
          "type": "user",
          "username": "jane.doe",
          "email": "jane.doe@gmail.com",
          "created": 1315524171347008,
          "modified": 1315524171347008,
          "metadata": {
            "path": "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
            "collections": {
              "activities": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
              "feed": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
              "groups": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
              "messages": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/messages",
              "queue": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/queue",
              "roles": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
              "following": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
              "followers": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
            },
            "sets": {
              "rolenames": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
              "permissions": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
            }
          }
        }
      ],
      "timestamp": 1315524171329,
      "duration": 110
    }
    >

Now enter the command to list the elements of the users collection
again:

    /users

This time the results should show the two users you added:

    > /users
    /users
    {
      "action": "get",
      "application": "1c8f60e4-da67-11e0-b93d-12313f0204bb",
      "params": {
        "_": [
          "1315524221412"
        ]
      },
      "path": "/users",
      "uri": "https://api.usergrid.com/1c8f60e4-da67-11e0-b93d-12313f0204bb/users",
      "entities": [
        {
          "uuid": "0e47f040-da71-11e0-b93d-12313f0204bb",
          "type": "user",
          "created": 1315523992687007,
          "modified": 1315523992687007,
          "email": "john.doe@gmail.com",
          "metadata": {
            "path": "/users/0e47f040-da71-11e0-b93d-12313f0204bb",
            "collections": {
              "activities": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/activities",
              "feed": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/feed",
              "groups": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/groups",
              "messages": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/messages",
              "queue": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/queue",
              "roles": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/roles",
              "following": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/following",
              "followers": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/followers"
            },
            "sets": {
              "rolenames": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/rolenames",
              "permissions": "/users/0e47f040-da71-11e0-b93d-12313f0204bb/permissions"
            }
          },
          "username": "john.doe"
        },
        {
          "uuid": "78c54a82-da71-11e0-b93d-12313f0204bb",
          "type": "user",
          "created": 1315524171347008,
          "modified": 1315524171347008,
          "email": "jane.doe@gmail.com",
          "metadata": {
            "path": "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
            "collections": {
              "activities": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
              "feed": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
              "groups": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
              "messages": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/messages",
              "queue": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/queue",
              "roles": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
              "following": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
              "followers": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
            },
            "sets": {
              "rolenames": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
              "permissions": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
            }
          },
          "username": "jane.doe"
        }
      ],
      "timestamp": 1315524225021,
      "duration": 59
    }
    >

To retrieve a specific user, try the following:

    /users/jane.doe

You should see results similar to this:

    > /users/jane.doe
    /users/jane.doe
    {
      "action": "get",
      "application": "1c8f60e4-da67-11e0-b93d-12313f0204bb",
      "params": {
        "_": [
          "1315524419746"
        ]
      },
      "path": "/users",
      "uri": "https://api.usergrid.com/1c8f60e4-da67-11e0-b93d-12313f0204bb/users",
      "entities": [
        {
          "uuid": "78c54a82-da71-11e0-b93d-12313f0204bb",
          "type": "user",
          "created": 1315524171347008,
          "modified": 1315524171347008,
          "email": "jane.doe@gmail.com",
          "metadata": {
            "path": "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
            "collections": {
              "activities": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
              "feed": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
              "groups": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
              "messages": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/messages",
              "queue": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/queue",
              "roles": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
              "following": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
              "followers": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
            },
            "sets": {
              "rolenames": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
              "permissions": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
            }
          },
          "username": "jane.doe"
        }
      ],
      "timestamp": 1315524421071,
      "duration": 107
    }
    >

### Adding properties using the shell

Now, let's add some information (entity properties) for the user
jane.doe. You can assign application-specific or system-defined
properties to user entities. (See the You do not have access to view
this node for a list of predefined properties for each system-defined
entity type.) As an example of adding an application-specific property,
let's add the property city to the user jane.doe by typing:

    put /users/jane.doe {"city" : "san francisco" }

You should see results similar to this:

    > put /users/jane.doe {"city" : "san francisco" }
    /users/jane.doe
    {
      "action": "put",
      "application": "1c8f60e4-da67-11e0-b93d-12313f0204bb",
      "params": {},
      "path": "/users",
      "uri": "https://api.usergrid.com/1c8f60e4-da67-11e0-b93d-12313f0204bb/users",
      "entities": [
        {
          "uuid": "78c54a82-da71-11e0-b93d-12313f0204bb",
          "type": "user",
          "created": 1315524171347008,
          "modified": 1315524526405008,
          "city": "san francisco",
          "email": "jane.doe@gmail.com",
          "metadata": {
            "path": "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
            "collections": {
              "activities": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
              "feed": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
              "groups": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
              "messages": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/messages",
              "queue": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/queue",
              "roles": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
              "following": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
              "followers": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
            },
            "sets": {
              "rolenames": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
              "permissions": "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
            }
          },
          "username": "jane.doe"
        }
      ],
      "timestamp": 1315524526343,
      "duration": 84
    }
    >

Let's create another type of object. Type the following:

    post /cats {"name" : "felix"}

You should see results for the collection cats that you created with the
entity felix:

    > post /cats {"name" : "felix"}
    /cats
    {
      "action": "post",
      "application": "1c8f60e4-da67-11e0-b93d-12313f0204bb",
      "params": {},
      "path": "/cats",
      "uri": "https://api.usergrid.com/1c8f60e4-da67-11e0-b93d-12313f0204bb/cats",
      "entities": [
        {
          "uuid": "89a05d85-da72-11e0-b93d-12313f0204bb",
          "type": "cat",
          "created": 1315524629123008,
          "modified": 1315524629123008,
          "metadata": {
            "path": "/cats/89a05d85-da72-11e0-b93d-12313f0204bb"
          },
          "name": "felix"
        }
      ],
      "timestamp": 1315524629068,
      "duration": 107
    }
    >

Now let's update this object by assigning a property color:

    put /cats/felix {"color" : "black"}

You should see something like the following results:

    > put /cats/felix {"color" : "black"}
    /cats/felix
    {
      "action": "put",
      "application": "1c8f60e4-da67-11e0-b93d-12313f0204bb",
      "params": {},
      "path": "/cats",
      "uri": "https://api.usergrid.com/1c8f60e4-da67-11e0-b93d-12313f0204bb/cats",
      "entities": [
        {
          "uuid": "89a05d85-da72-11e0-b93d-12313f0204bb",
          "type": "cat",
          "created": 1315524629123008,
          "modified": 1315524724093008,
          "color": "black",
          "metadata": {
            "path": "/cats/89a05d85-da72-11e0-b93d-12313f0204bb"
          },
          "name": "felix"
        }
      ],
      "timestamp": 1315524724058,
      "duration": 57
    }
    >

You can now click the Collections button in the sidebar to see all of
the collections in your application. There are two entities in the users
collection and one in the cats collection. Feel free to look around and
see how the portal provides a high-level alternative to the API for
viewing and manipulating application objects.

Subsequent sections give details about using the API to access and
modify application entities. If you're ready to start using the App
services API, see the You do not have access to view this node for
details and examples for each API endpoint.

Analytics
---------

On the left sidebar, click Analytics to explore this functionality. You
can specify parameters for data collection, including what data points
you'd like to collect, over what time period, and at what resolution.
When you click the Generate button, the results are displayed in tabular
form and graphically in the lower portion of the window.

Creating a sandbox app
----------------------

When you create a new Apache Usergrid account, an app named sandbox is
automatically created. In some cases you may want to create (or
re-create) the sandbox app. For example, you may want to create a
sandbox app for another organization or you may want to create another
app for testing purposes. See [Using a Sandbox
Application](/using-sandbox-application) for details.

Displaying API calls as cURL commands
-------------------------------------

You can display the equivalent cURL syntax for each API call that you
make through the admin portal. The calls are displayed in the console
area of any of the following browsers: Chrome, Internet Explorer (in the
debugger), Firefox (in Firebug), and Safari. For details, see
[Displaying Apache Usergrid API calls as Curl
commands](/displaying-app-services-api-calls-curl-commands).
