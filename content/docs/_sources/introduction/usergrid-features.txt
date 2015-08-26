# Usergrid Features

Usergrid provides developers with access to a flexible data store and enables you to quickly integrate valuable features into your app, including social graphs, user management, data storage, push notifications, performance monitoring, and more.

With Usergrid, developers can set up their own cloud-based data platform in minutes instead of months – no server-side coding or back-end development needed. This allows your developers to focus on developing the rich features and user experience that truly differentiate your app, rather than on the time-consuming details of implementing core back-end services and infrastructure.

## Data storage & management

### Application data

At the core of Usergrid is a flexible platform that can store any type of application data, from simple records like a catalog of books to complex associations like user relationships. No matter what type of data drives your app, you can store it as collections of data entities and immediately perform complex queries or full-text searches on any field. You can also create custom entities with custom properties, giving you the ability to store data and context in a way that makes sense for your app.

To learn more about entities and collections, see [Usergrid Data model](data-model.html).

For a complete list of the default data entities available, see [Models](../rest-endpoint/api-docs.html#models).

### Files & assets

Images, video, and audio are key components of a great app experience. With Usergrid, you can upload and retrieve binary objects from the same data store as the rest of your application data, eliminating the need to set up content delivery networks (CDNs) and easing implementation. We handle all the back-end details that keep your content quickly accessible.

To learn more about files and asset storage, see [Uploading files and assets](../assets-and-files/uploading-assets.html).

## Flexible data querying

One of Usergrid' most powerful features is the ability to perform SQL-style queries and full-text searches on data entities, as well as their properties. This lets you quickly retrieve specific data entities based on multiple criteria, then utilize that data to power social features, target push notifications, perform user analysis, and more.

Learn more about querying app data, see [Data query overview](../data-queries/querying-your-data.html).

## Social

### Entity relationships

You can create relationships between data entities to help build features, improve user experience, and contextualize data. For example, you might associate a user with their devices to capture valuable geolocation data, create relationships between users to build social graphs, or implement popular features such as activity streams.

To learn more about entity relationships, see [Entity connections](../entity-connections/connecting-entities.html).

### Activity streams

A key aspect of social networking apps is the ability to provide and publish data streams of user actions, such as ongoing lists of comments, activities, and tweets. Usergrid simplifies management and routing of these data streams by providing an activity entity that is specifically designed to automatically create a relationship between activities and the user who created them.

To learn more about activities and activity feeds, see [Activity feeds](../user-management/activity.html).

## User management

### Registration and login

You can easily add and manage users by providing the core services necessary to handle secure registration and log in, including OAuth 2.0-compliant client authentication. In addition, any number of default or custom data entities and properties can be associated with a user entity to create complete user profiles.

To learn more about user management, see [User Management](../user-management/user-management.html).

To learn more about authentication, see Authenticating users and application clients.

### Roles & permissions

Applications often require the ability to configure fine-grain control of user access to data, features and functionality. Usergrid solves the implementation details of user access with roles and permissions. Simply create roles that represent user types or access levels, such as Administrator, then assign the necessary permissions to that role. With a single API call, you can then associate your roles with any user or group of users.

To learn more about user roles and permissions, see [Using Permissions](../security-and-auth/securing-your-app.html).

### Groups

Groups are a flexible way to organize your users based on any number of criteria. For example, you might group users based on interests or location to more effectively deliver relevant content, target offers, or customize campaigns. You can also take advantage of the groups entity to enable group-based social networking activities, such as allowing your users to create private information feeds or circles of friends.

To learn more about groups, see [Working with group data](../user-management/groups.html).

### Third-party authentication

In addition to supporting user management and OAuth-based login for your app, Usergrid also makes it easy to integrate third-party authentication through such popular services as Facebook, Twitter and other OAuth-enabled accounts. Providing third-party sign-in can improve user experience, while increasing adoption, giving you access to valuable information from social networks and services.

To learn more about using third-party sign-in, see [Facebook sign in](../security-and-auth/facebook-sign.html).

## Geolocation

The device entity allows you to capture geolocation data from your users' GPS-enabled devices to more effectively target campaigns, push notifications, offers and more. Geolocation also gives you an important data point for contextualizing and analyzing trends and user behavior.

To learn more about geolocation, see [Geolocation](../geolocation/geolocation.html).

## Push notifications (Coming soon...)

__(Coming Usergrid 2.0)__ Push notifications are the most effective way to engage your users with relevant content, and thanks to Usergrid, implementing them can be done in minutes. Simply register your app and your user's devices with a notification provider, such as Apple Push Notification Service or Google Cloud Messaging, then use the Usergrid notification entity to send millions of push notifications a month at no cost. When used in conjunction with queries of user and application data, push notifications become a powerful tool for leveraging user data, ensuring relevancy and driving engagement.

To learn more about push notifications, see [Push notifications overview](../push-notifications/tbd.html).
