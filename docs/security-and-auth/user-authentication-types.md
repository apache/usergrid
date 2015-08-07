# Authentication levels

Usergrid supports four levels of authentication, but only one of them is used when checking a registered user's permissions. The other three levels are useful for authenticating other application or web clients that require higher-level access to your Usergrid application or organization. Because the scope of access that the other authentication levels provide is so broad (and as a result, so powerful), it's a bad practice to use them from a mobile app. Instead, they're better suited to other client apps, such as web applications.

## Configuring authentication levels
Access permissions can only be configured for the 'application user' – this can be done both programmatically and in the admin portal. The application, organization and admin clients cannot be configured, and can only be accessed programmatically via the API.

For more about creating and managing roles and permissions for application users, see [Using Permissions](security-and-auth/using-permissions.html) and [Using Roles](security-and-auth/using-roles.html). For a look at how security features fit together, see [App Security Overview](../security-and-auth/app-security.html).

## User authentication level

<table class="usergrid-table">
<tr>
    <th>Authentication Level</th>
    <th>Description</th>
</tr>
<tr>
    <td>Application user</td>
    <td>This is the standard authentication type you will use to implement user login for your app. The application user level allows access to your Usergrid application as governed by the permission rules you create and associated with users and user groups. For more on setting permissions see [Using Permissions](security-and-auth/using-permissions.html). Each Application User is represented by a User entity in your Usergrid application. For more about the User entity, see User.</td>
</tr>
</table>

## Admin authentication levels

<div class="admonition warning"> <p class="first admonition-title">WARNING</p> <p class="last"> 
Warning: Safe use of admin authentication levels. Never use client ID and client secret, or any hard-coded credentials to authenticate this way from a client-side app, such as a mobile app. A hacker could analyze your app and extract the credentials for malicious use even if those credentials are compiled and in binary format. Even when authenticating with username and password, be cautious when using these authentication levels since they grant broad access to your Usergrid account. See [Security Best Practices](../security-and-auth/securing-your-app.html) for additional considerations in keeping access to your app and its data secure.</p></div>

<table class="usergrid-table">
<tr>
    <th>Authentication Level</th>
    <th>Description</th>
</tr>
<tr>
   <td>Application client</td>
   <td>Grants full access to perform any operation on an Usergrid application (but not other applications within the same organization).
        
   <p>Authentication at this level is useful in a server-side application (not a mobile app) that needs access to resources through the Usergrid API. For example, imagine you created a website that lists every hiking trail in the Rocky Mountains. You would want anyone to be able to view the content, but would not want them to access the Usergrid API and all your data directly. Instead, you would authenticate as an application client in your server-side code to access the data via the API in order to serve it to your website's visitors.</p></td>
</tr>
<tr>
   <td>Organization client</td>
   <td>Grants full access to perform any operation on an Usergrid organization.
        
   <p>This authentication level provides the greatest amount of access to an individual organization, allowing a client to perform any operation on an Usergrid organization and any applications in that organization. This level of access should be used sparingly and carefully.</p></td>
</tr>
<tr>
   <td>Admin user</td>
   <td>Allows full access to perform any operation on all organization accounts of which the admin user is a member.
        
   <p>This authentication level is useful from applications that provide organization-wide administration features. For example, the Usergrid admin portal uses this level of access because it requires full access to the administration features.</p>
        
   Unless you have a specific need for administrative features, such as to run test scripts that require access to management functionality, you should not use the admin user authentication level.</td>
</tr>
</table>




	


	


