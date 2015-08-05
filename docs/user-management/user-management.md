# User management & social graph
Whether you're developing apps for mobile or the Web, it's almost certain that you will need to be able to handle user management, as well as offer the types of social features users have come to expect from a rich app experience. Usergrid makes all of this easy with default entity types and functionality available right out of the box. From user registration and profiles to login and authentication to activity feeds and social graph, you can create a social experience quickly and easily with just a few types of API calls.

## User management
The default user entity in Usergrid is designed to model app users, meaning registering users and managing their profiles is as simple as sending and updating JSON via the API. Used in conjunction with our available social graph, as well as our OAuth 2.0 authentication and token authorization features, you have all the tools you need to manage your user base.

Learn more about:

* [User management](user-management.html)
* [Authentication & tokens](../security-and-auth/app-security.html)
* [Permissions and roles](../security-and-auth/using-permissions.html)

## Group management
One of the most basic social features of any app is the ability to create groups of users to limit shared access to user or other app data. The default group entity in Usergrid was designed for this exact purpose. Associate a user with as many groups or sub-groups as you need, then apply permissions or roles to define shared access to Usergrid data.

Learn more about:

* [Group management](group.md)
* [Permissions and roles](../security-and-auth/using-permissions.html)

## Social connections
To create a rich social graph, your app needs to be able to create connections between users. Usergrid makes this process lightweight by allowing you to create social connections and generic entity connections between users to model relationships by working with simple URI paths.

For example, you could create a 'likes' relationship between two users with a POST:

    https://api.usergrid.com/your-org/your-app/users/Arthur/likes/users/Ford
    
You could then retrieve all the users Arthur 'likes' with a GET to populate a list in your UI:

    https://api.usergrid.com/your-org/your-app/users/Arthur/likes
    
Learn more about:

* [Social connections](user-connections.html)
* [Generic entity connections](../data-storage/relationship.html)

## Activity feeds
Activity feeds can be an essential way of establishing a social dimension of your user experience. Allow users to actively publish activities, such as status messages, or have your application code passively publish activities based on user actions, such as posting a photo. Activity feeds can be created and shared at both the user and group level, giving you the flexibility to present activity feeds that are most relevant to your users.

Learn more about:

* [Activity feeds]
