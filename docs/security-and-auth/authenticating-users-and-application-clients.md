# Authenticating users & app clients

To protect your Usergrid application data, one of the steps you'll take is to authenticate your app's users. By ensuring that they are who they say they are, you can help ensure that your application's data is available in secure ways. After you've created permission rules that define access to your application and have associated these rules with users, you'll want to add code that authenticates your user, as described in this topic.

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
You manage access to your application's data by creating permission rules that govern which users can do what. Users authenticated as Application User have access according to these rules. For more about managing permissions, see [Using Permissions](using-permissions.html).
</p></div>


## Authentication levels
Usergrid supports four levels of authentication:

* __Application user__: Grant's user access to an API Services application, based on the roles and permissions assigned to the user.
* __Application client__: Grants full access to perform API requests against an API Services application.
* __Organization client__: Grants full access to perform API requests against an API Services organization.
* __Admin user__: Grants full access to perform API requests against any API Services organization that the user is an admin of.

Because the scope of access provided by the application client, organization client, and admin user authentication levels is so broad (and as a result, so powerful), it's a bad practice to use them from a mobile app or any client-side code. Instead, they're better suited to server-side implementations, such as web applications.

For a more detailed description of available authentication levels, see [Authentication levels](user-authentication-types.html).

## Application user authentication (user login)
Using the username and password values specified when the user entity was created, your app can connect to the Usergrid application endpoint to request an access token. It's also acceptable to use the user's email address in place of the username.

### Using the SDKs
When a user is logged in using the Usergrid iOS, JavaScript, node.JS and Android SDKs, the returned token is automatically stored in the UsergridDataClient (iOS), DataClient (Android), or Usergrid.Client (JavaScript/node.JS) class instance, and will be sent to the API with all subsequent method calls.

#### Request syntax

    curl -X POST "https://api.usergrid.com/<orgName>/<appName>/token" -d '{"grant_type":"password", "username":<username>, "password":<password>}'

#### Example request

    curl -X POST "https://api.usergrid.com/my-org/my-app/token" -d '{"grant_type":"password", "username":"john.doe", "password":"testpw"}'
		
#### Example response

The results include the access token needed to make subsequent API requests on behalf of the application user:

    {
    "access_token": "5wuGd-lcEeCUBwBQVsAACA:F8zeMOlcEeCUBwBQVsAACA:YXU6AAABMq0hdy4",
    "expires_in": 3600,
        "user": {
            ...
        }
    }
		
## Application client authentication
Using your app’s client id and client secret values, your app can connect to the Usergrid application endpoint to request an access token. The client ID and secret for your app can be found in 'Getting Started' section of the API Services admin portal, under 'Server App Credentials'.

<div class="admonition warning"> <p class="first admonition-title">WARNING</p> <p class="last"> 
Warning: You should never authenticate this way from a client-side app such as a mobile app. A hacker could analyze your app and extract the credentials for malicious use even if those credentials are compiled and in binary format. See [Security Best Practices](../security-and-auth/securing-your-app.html) for additional considerations in keeping access to your app and its data secure.
</p></div>

### Request syntax

    curl -X POST "https://api.usergrid.com/<orgName>/<appName>/token" -d '{"grant_type":"client_credentials", "client_id":<application_clientID>, "client_secret":"<application_client_secret>"}'
    
### Example request

    curl -X POST "https://api.usergrid.com/my-org/my-app/token" -d '{"grant_type":"client_credentials", "client_id":"YXB7NAD7EM0MEeJ989xIxPRxEkQ", "client_secret":"YXB7NAUtV9krhhMr8YCw0QbOZH2pxEf"}'
		
### Example response

The results include the access token needed to make subsequent API requests on behalf of the application:

    {
        "access_token": "F8zeMOlcEeCUBwBQVsAACA:YXA6AAABMq0d4Mep_UgbZA0-sOJRe5yWlkq7JrDCkA",
        "expires_in": 3600,
        "application": {
            ...  
        }
    }
		
## Admin user authentication
If you do require admin user access, your app can connect to the Usergrid management endpoint to request an access token. Your app supplies the username and password of an admin user in the request.

<div class="admonition warning"> <p class="first admonition-title">WARNING</p> <p class="last"> 
Warning: Authenticating as an admin user grants full access to one or more organizations and all of the applications contained in those organizations. Due to this, be cautious when implementing this type of authentication in client-side code. Instead, consider implementing admin user access in server-side code only. See [Security Best Practices](../security-and-auth/securing-your-app.html) for additional considerations in keeping access to your app and its data secure.
</p></div>

### Request syntax

    curl -X POST "https://api.usergrid.com/management/token" -d '{"grant_type":"password", "username":<admin_username>, "password":<admin_password>}'
    
### Example Request

    curl -X POST "https://api.usergrid.com/management/token"  -d '{"grant_type":"password", "username":"testadmin", "password":"testadminpw"}'

### Example response

The results include the access token needed to make subsequent API requests on behalf of the admin user:

    {
        "access_token": "f_GUbelXEeCfRgBQVsAACA:YWQ6AAABMqz_xUyYeErOkKjnzN7YQXXlpgmL69fvaA",
        "expires_in": 3600,
        "user": {
            ...
        }
    }		    
		
## Organization client authentication
If you do require organization level access, your app can connect to the Usergrid management endpoint to request an access token. Access to an organization requires the client id and client secret credentials. The client ID and secret for your organization can be found on the 'Org Administration' page of the API Services admin console under 'Organization API Credentials'.

<div class="admonition warning"> <p class="first admonition-title">WARNING</p> <p class="last"> 
Warning: You should never authenticate this way from a client-side app such as a mobile app. A hacker could analyze your app and extract the credentials for malicious use even if those credentials are compiled and in binary format. See [Security Best Practices](../security-and-auth/securing-your-app.html) for additional considerations in keeping access to your app and its data secure.
</p></div>

### Request syntax

    curl -X POST "https://api.usergrid.com/management/token" -d '{"grant_type":"client_credentials", "client_id":<org_clientID>, "client_secret":<org_client_secret>}'
    
### Example request

    curl -X POST "https://api.usergrid.com/management/token" -d '{"grant_type":"client_credentials", "client_id":"YXB7NAD7EM0MEeJ989xIxPRxEkQ", "client_secret":"YXB7NAUtV9krhhMr8YCw0QbOZH2pxEf"}'
	
### Example response

The results include the access token needed to make subsequent API requests to the organization:

    {
        "access_token": "gAuFEOlXEeCfRgBQVsAACA:b3U6AAABMqz-Cn0wtDxxkxmQLgZvTMubcP20FulCZQ",
        "expires_in": 3600,
        "organization": {
            ...
        }
    }
    