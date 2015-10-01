# Authenticating API requests

With the exception of the 'sandbox' application that is created with every Usergrid organization, all applications are secured by default. This means that to access your data store, a valid access token must be sent with all API requests to authenticate that the requester is authorized to make API calls to the resources they are attempting the access.

This article describes how to use access tokens to access the Usergrid API, and how to manage access tokens, including revoking and changing token time to live.

For information on generating access tokens/authenticating users and clients, see [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html).

## Authenticating with access tokens
When you obtain an access token, you must provide it with every subsequent API call that you make. There are two ways to provide your access token.

You can add the token to the API query string:

    https://<usergrid-host>/{org-name}/{app-name}/users?access_token={access_token}
    
You can include the token in an HTTP authorization header:

    Authorization: Bearer {access_token}

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
Note: The Usergrid documentation assumes you are providing a valid access token with every API call whether or not it is shown explicitly in the examples. Unless the documentation specifically says that you can access an API endpoint without an access token, you should assume that you must provide it. One application that does not require an access token is the sandbox application. The Guest role has been given full permissions (/** for GET, POST, PUT, and DELETE) for this application. This eliminates the need for a token when making application level calls to the sandbox app. For further information on specifying permissions, see [Using Permissions](security-and-auth/using-permissions.html).
</p></div>

## Authenticating with client ID and client secret

Another option for authenticating your API requests is using either your organization client ID and client secret, or your application client ID and client secret, which will authenticate your request as an organization or application admin, respectively. Organization credentials can be found in the 'Org Overview' section of the admin portal, and application credentials can be found in the 'Getting Started' section of the admin portal.

<div class="admonition warning"> <p class="first admonition-title">WARNING</p> <p class="last"> 
Warning: For server-side use only
You should never authenticate this way from a client-side app such as a mobile app. A hacker could analyze your app and extract the credentials for malicious use even if those credentials are compiled and in binary format. See [Security Best Practices](../security-and-auth/securing-your-app.html) for additional considerations in keeping access to your app and its data secure.
</p></div>

This can be a convenient way to authenticate API requests, since there is no need to generate and manage an access token, but please note that you should be very cautious when implementing this type of authentication. Organization-level authentication grants full permission to perform any supported call against your organization and every application in it, and application-level authentication grants full permission to perform any supported call against all of the resources in an application. Should your client id and client secret be compromised, a malicious user would gain broad access to your organization or application.

To authenticate using client id and secret, append the following parameters to your request URL:

    client_id=<your-client-id>&client_secret=<your-client-secret>
    