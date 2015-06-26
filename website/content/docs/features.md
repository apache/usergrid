---
title: Apache Usergrid Features
category: docs
layout: docs
---

Apache Usergrid Features
========================

Apache Usergrid is a backend-as-a-service (BaaS) solution that enables you
to quickly integrate valuable features into your app, including social
graphs, user management, data storage, push notifications, performance
monitoring, and more.

Using Apache Usergrid, you can set up your own cloud-based data platform in
minutes instead of months – no server-side coding or back-end
development needed. This allows your team to focus on developing the
rich features and user experience that truly differentiate your app,
rather than the time-consuming details of implementing core back-end
services and infrastructure.

Data storage & management
-------------------------

### Application data

At the core of Apache Usergrid is a flexible platform that can store any
type of application data, from simple records like a catalog of books to
complex associations like user relationships. No matter what type of
data drives your app, you can store it in Apache Usergrid infrastructure as
collections of data entities and immediately perform complex queries or
full-text searches on any field. You can also create custom entities
with custom properties, giving you the ability to store data and context
in a way that makes sense for your app.

To learn more about entities and collections, see [Apache Usergrid Data
model](/app-services-data-model-1).

For a complete list of default data entities available in Apache Usergrid,
see [Default Data Entities](/default-data-entities).

### Files & assets

Images, video, and audio are key components of a great app experience.
With Apache Usergrid, you can upload and retrieve binary objects from the
same data store as the rest of your application data, eliminating the
need to set up content delivery networks (CDNs) and easing
implementation. We handle all the back-end details that keep your
content quickly accessible.

To learn more about files and asset storage, see [Uploading files and
assets](/uploading-files-and-assets).

### Flexible data querying

One of Apache Usergrid' most powerful features is the ability to perform
SQL-style queries and full-text searches on data entities, as well as
their properties. This lets you quickly retrieve specific data entities
based on multiple criteria, then utilize that data to power social
features, target push notifications, perform user analysis, and more.

Learn more about querying app data, see [Apache Usergrid data query
overview](/app-services-data-query-overview).

Social
------

### Entity relationships

With Apache Usergrid you can create relationships between data entities to
help build features, improve user experience, and contextualize data.
For example, you might associate a user with their devices to capture
valuable geolocation data, create relationships between users to build
social graphs, or implement popular features such as activity streams.

To learn more about entity relationships, see [Entity
relationships](/entity-relationships).

### Activity streams

A key aspect of social networking apps is the ability to provide and
publish data streams of user actions, such as ongoing lists of comments,
activities, and tweets. Apache Usergrid simplifies management and routing
of these data streams by providing an activity entity that is
specifically designed to automatically create a relationship between
activities and the user who created them.

To learn more about activities and activity feeds, see
[Activity](/activity).

User management
---------------

### Registration and login

Apache Usergrid makes it easy to add and manage users by providing the core
services necessary to handle secure registration and log in, including
OAuth 2.0-compliant client authentication. In addition, any number of
default or custom data entities and properties can be associated with a
user entity to create complete user profiles.

To learn more about user management, see [User](/user).

To learn more about authentication, see [Authenticating users and
application clients](/authenticating-users-and-application-clients).

### Roles & permissions

Applications often require the ability to configure fine-grain control
of user access to data, features and functionality. Apache Usergrid solves
the implementation details of user access with roles and permissions.
Simply create roles that represent user types or access levels, such as
Administrator, then assign the necessary permissions to that role. With
a single API call, you can then associate your roles with any user or
group of users.

To learn more about user roles and permissions, see [Managing access by
defining permission rules](/managing-access-defining-permission-rules).

### Groups

Groups are a flexible way to organize your users based on any number of
criteria. For example, you might group users based on interests or
location to more effectively deliver relevant content, target offers, or
customize campaigns. You can also take advantage of the groups entity to
enable group-based social networking activities, such as allowing your
users to create private information feeds or circles of friends.

To learn more about groups, see [Group](/group).

### Third-party authentication

In addition to supporting user management and OAuth-based login for your
app, Apache Usergrid also makes it easy to integrate third-party
authentication through such popular services as Facebook, Twitter and
other OAuth-enabled accounts. Providing third-party sign-in can improve
user experience, while increasing adoption, giving you access to
valuable information from social networks and services.

To learn more about using third-party sign-in, see [Facebook sign
in](/facebook-sign).

Geolocation
-----------

The Apache Usergrid device entity allows you to capture geolocation data
from your users' GPS-enabled devices to more effectively target
campaigns, push notifications, offers and more. Geolocation also gives
you an important data point for contextualizing and analyzing trends and
user behavior.

To learn more about geolocation, see [Geolocation](/geolocation).

Push notifications
------------------

Push notifications are the most effective way to engage your users with
relevant content, and thanks to Apache Usergrid, implementing them can be
done in minutes. Simply register your app and your user's devices with a
notification provider, such as Apple Push Notification Service or Google
Cloud Messaging, then use the Apache Usergrid notification entity to send
millions of push notifications a month at no cost. When used in
conjunction with queries of user and application data, push
notifications become a powerful tool for leveraging user data, ensuring
relevancy and driving engagement.

To learn more about push notifications, see [Push notifications
overview](/push-notifications-overview).

Configuration management
------------------------

By giving you the ability to push configuration changes directly to
users' devices, Apache Usergrid lets you test and deliver bug fixes and
performance improvements in real time. Remote configuration management
can even be used to push changes to a subset of all users, enabling A/B
testing of fixes, so that you can monitor and collect data to ensure
your updates perform as intended before they are pushed to your entire
user base.

To learn more about configuration management, see [Configure your
app](/configure-your-app).

Error & performance monitoring
------------------------------

One of the largest barriers to the success of an app can be the ability
of developers to respond quickly and precisely to performance issues and
bugs. Apache Usergrid includes data logging and visualization tools that
let you monitor and analyze network performance, usage patterns, crash
statistics and other key metrics, giving you the data necessary to
quickly manage performance issues as they arise.

To learn more about usage monitoring, see [Monitor your app’s
use](/monitor-your-app%E2%80%99s-use)

To learn more about error monitoring, see [Get alerted to crashes and
critical errors](/get-alerted-crashes-and-critical-errors)
