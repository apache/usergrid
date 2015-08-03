# Security best practices

There a number of actions you should take to ensure that your app is secure before you put it into production. The following is not an exhaustive list, but offers some common best practices you should consider following to keep your app secure when using the Usergrid.

## Never use the 'sandbox' for a production app
By default, every new Usergrid account has an app named “sandbox” that is already created under your new organization. This app is no different than any other app that you might create, except that the Guest role has been given full permissions (that is, /** for GET, POST, PUT, and DELETE). This eliminates the need for a token when making application level calls, and can make it much easier to get your app up and running; however, it also means that any data in the sandbox application is completely unsecured.

As with any other app, you can secure the sandbox application by updating its roles and permissions. For more on working with permissions and roles, see [Using Permissions](using-permissions.html).

## Review permissions in your apps
Prior to launching your app into a production environment, it is advisable to review all the roles and permissions you have set up, as well as the groups and users you have assigned those permissions and roles to. During development, you may find that you added various permissions which may or may not still be required once the app is complete. Review all permissions and delete any that are no longer required.

Prior to taking your app live, you should secure it by removing any unnecesary Guest permissions. (See [Using Permissions](using-permissions.html) for further information about setting permissions.) After you secure your the app, any calls to the API will need to include an OAuth token. Oauth tokens (also called access tokens) are obtained by the API in response to successful authentication calls. Your app saves the token and uses it for all future calls during that session. Learn more about access tokens in Authenticating users and application clients.

## Edit the 'default' role
When preparing an application for production use, a good first step is to edit permission rules for the Default role. The permissions in this role will be applied to every user who authenticates with a valid access token.

For example, in the Default role, you will most likely first want to remove the permission rule that grants full access to all authenticated users:

    GET,PUT,POST,DELETE:/users/me/**

For more on roles, see [Using Permissions](using-permissions.html).

Review test accounts
If you created any test user or test administrator accounts during development, these should also be reviewed for relevancy and security. Delete any test accounts that are no longer needed. If these accounts are still needed, make sure that passwords have been secured to the standards required by your app.

## Use https
Make sure that any calls you make to the API are done using the secure https protocol, and not the insecure http protocol. 

If your app is a web app, that is, an app served by a web server, make sure that the app is served using https.

## Acquire access tokens in a secure way
There are various methods for acquiring an access token (see [Authenticating users and application clients](authenticating-users-and-application-clients.html). One method is to use the application or organization level client secret-client id combination. This method should not be used in client applications (this is, apps that are deployed to a device, and which authenticate and make calls against the API).

That’s because a hacker could analyze your app (even a compiled, binary distribution of your app), and retrieve the secret-id combination. Armed with this information, an attacker could gain full access to the data in your account.

Instead, use application user credentials. This means that your app’s users should provide a username and password. Your app would use these to authenticate against the API and retrieve an access token.

The client secret-client id combination should be used only in secure, server-side applications where there is no possibility of a hacker gaining control of the credentials.

## Treat mobile clients as untrustworthy
For mobile access, it is recommended that you connect as an application user with configured access control policies. Mobile applications are inherently untrusted because they can be easily examined and even decompiled.

Any credentials stored in a mobile app should be considered secure only to the Application User level. This means that if you don’t want the user to be able to access or delete data in your Usergrid application, you need to make sure that you don’t enable that capability through roles or permissions. Because most web applications talk to the database using some elevated level of permissions, such as root, it’s generally a good idea for mobile applications to connect with a more restricted set of permissions. For more information on restricting access through permission rules, see [Using Permissions](using-permissions.html).
