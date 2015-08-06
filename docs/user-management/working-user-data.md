# Working with User Data
You can store and manage user data as User entities. With user data in your application, you can add support for a wide variety of features common to mobile apps. For example, you can:

* Control access to data by defining permission rules. (See [Security & token authentication](../security-and-auth/app-security.html) for more.)
* Present content specific to each user, such as their list of favorites.
* Support social features, such as letting users "follow" one another, for example.

In mobile applications, data about users is typically added by users themselves when they register through your app. The topics in this section provide specific cURL and SDK-specific examples for getting things done with user data.

## Creating users

A user entity represents an application user. Using API Services you can create, retrieve, update, delete, and query user entities. See [User entity properties](../rest-endpoints/api-doc.html#user) for a list of the system-defined  properties for user entities. In addition, you can create user properties specific to your application.

### Request Syntax

    curl -X POST "https://api.usergrid.com/your-org/your-app/users" -d '{ "username": "john.doe", "email": "john.doe@gmail.com", "name": "John Doe", "password": "test1234" }'
  
Use the POST method to create a new user in the users collection.

### Request URI

    POST /<org_id>/<app_id>/users

Parameters

Parameter	    Description
---------       -----------
uuid | org_id	Organization UUID or organization name.
uuid | app_id	Application UUID or application name.
request body	One or more sets of user properties.

The username is mandatory and must be unique. Here's an example:

    {
        "username" : "john.doe",
        "email" : "john.doe@gmail.com",
        "name" : "John Doe",
        "password" : "test1234"
    }

Although the password parameter is not mandatory, if you don't specify it, the user will not be able to log in using username and password credentials. If a password is not specified for the user, and you're an Admin, you can set a password for the user (see [Changing a User Password](#changing-a-user-password)).

__ Note__: The username can contain any combination of characters, including those that represent letters, numbers, and symbols.

### Example

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Request

    curl -X POST "https://api.usergrid.com/my-org/my-app/users" -d '{"username":"john.doe","email":"john.doe@gmail.com","name":"John Doe"}'
    
### Response

    {
      "action" : "post",
      "application" : "db1e60a0-417f-11e3-9586-0f1ff3650d20",
      "params" : { },
      "path" : "/users",
      "uri" : "https://api.usergrid.com/steventraut/mynewapp/users",
      "entities" : [ {
        "uuid" : "8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc",
        "type" : "user",
        "name" : "John Doe",
        "created" : 1390533228622,
        "modified" : 1390533228622,
        "username" : "john.doe",
        "email" : "john.doe@gmail.com",
        "activated" : true,
        "picture" : "http://www.gravatar.com/avatar/e13743a7f1db7f4246badd6fd6ff54ff",
        "metadata" : {
          "path" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc",
          "sets" : {
            "rolenames" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/roles",
            "permissions" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/permissions"
          },
          "collections" : {
            "activities" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/activities",
            "devices" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/devices",
            "feed" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/feed",
            "groups" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/groups",
            "roles" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/roles",
            "following" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/following",
            "followers" : "/users/8ae8a6ea-84a5-11e3-884d-f18e8f6fb3bc/followers"
          }
        }
      }],
      "timestamp" : 1390533228619,
      "duration" : 142,
      "organization" : "my-org",
      "applicationName" : "my-app"
    }


## Retrieving user data

You can retrieve data about users through cURL or one of the SDKs. Each provides a way to filter the list of users by data associated with the user, such as username or UUID, or other properties in the user entity.

See [User entity properties](../rest-endpoints/api-doc.html#user) for a list of the system-defined  properties for user entities. In addition, you can create user properties specific to your application.

### Request Syntax

    curl -X GET "https://api.usergrid.com/your-org/your-app/users"
    
Use the GET method to retrieve user data.

### Request URI

    GET /<org_id>/<app_id>/users/<uuid | username | email_address | ?ql=query_string>
    
Parameters

Parameter	    Description
---------       -----------
uuid | org_id	Organization UUID or organization name
uuid | app_id	Application UUID or application name
user identifier User UUID, username, or email address. 

The alias ``/users/me`` can be used in place of the current user’s uuid, username, or email address. Note: The ``/users/me`` endpoint is accessible only if you provide an access token with the request (see [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html)). If you make an anonymous ("guest") call, the system will not be able to determine which user to return as ``/users/me``.

__Note__: The username can contain any combination of characters, including those that represent letters, numbers, and symbols.

### Example

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

Requests

    # Get a user by username.
    curl -X GET "https://api.usergrid.com/my-org/my-app/users/jane.doe"

    # Get a user by UUID.
    curl -X GET "https://api.usergrid.com/my-org/my-app/users/a407b1e7-58e8-11e1-ac46-22000a1c5a67e"

    # Get a user by email.
    curl -X GET "https://api.usergrid.com/my-org/my-app/users/jane.doe@gmail.com"

    # Get user data filtering by their city property value.
    curl -X GET "https://api.usergrid.com/my-org/my-app/users?ql=select%20*%20where%20adr.city%3D'Chicago'"

Response

    {
        "action" : "get",
        "application" : "1c8f60e4-da67-11e0-b93d-12313f0204bb8",
        "params" : {
            "_": [
                "1315524419746"
            ]
        },
        "path" : "https://api.usergrid.com/12313f0204bb-1c8f60e4-da67-11e0-b93d/1c8f60e4-da67-11e0-b93d-12313f0204bb/users",
        "uri" : "https://api.usergrid.com/005056c00008-4353136f-e978-11e0-8264/4353136f-e978-11e0-8264-005056c00008/users",
        "entities" : [ {
            "uuid" : "78c54a82-da71-11e0-b93d-12313f0204b",
            "type" : "user",
            "created" : 1315524171347008,
            "modified" : 1315524171347008,
            "activated" : true,
            "email" : "jane.doe@gmail.com",
            "metadata" : {
                "path" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb",
                "sets" : {
                    "rolenames" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/rolenames",
                    "permissions" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/permissions"
                },
                "collections" : {
                    "activities" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/activities",
                    "devices" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/devices",
                    "feed" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/feed",
                    "groups" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/groups",
                    "roles" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/roles",
                    "following" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/following",
                    "followers" : "/users/78c54a82-da71-11e0-b93d-12313f0204bb/followers"
                }
            },
            "username" : "jane.doe"
        }
        ... Additional entities here if data for multiple users was returned...
        ],
        "timestamp" : 1315524421071,
        "duration" : 107,
        "organization" : "my-org",
        "applicationName": "my-app"
    }

## Updating & deleting user data

To update or delete a user, perform an update or delete on the associated user entity as you would any other entity. For more information and code samples, see [Updating Data Entities](../data-storage/entities.html#updating-data-entities) and [Deleting Data Entities](../data-storage/entities.html#deleting-data-entities).

## Changing a user password

Changing a user's password

### Request syntax

    curl -X PUT https://api.usergrid.com/<org>/<app>/users/<username_or_email>/password -d '{oldpassword:<old_password>,newpassword:<new_password>}'
    
Parameters

Parameter	        Description
---------           -----------
org	                Organization UUID or organization name
app	                Application UUID or application name
username_or_email	Username or email of the user entity whose password you want to reset.
old_password	    User entity's old password.
new_password	    User entity's new password.

__Note__: If your request is authenticated with an application-level token, then ``old_password`` is not required. For more, see [Application client authentication](../security-and-auth/authenticating-users-and-application-clients.html#application-client-authentication).

Example request

    curl -X PUT https://api.usergrid.com/my-org/my-app/users/john.doe/password -d '{"newpassword":"foo9876a","oldpassword":"bar1234b"}'
    
Example response

    {
      "action": "set user password",
      "timestamp": 1355185897894,
      "duration": 47
    }
    
## Resetting a user password

Resetting a user's password

Usergrid provides a standard password reset flow that can be implemented to allow a user to reset their password without having to provide their old password. The most common use of this would be a 'Forgot password?' feature in your app.

Note that you can also implement your own password reset flow using application-level authentication and the /password endpoint. For more, see [Changing a user password](#changing-a-user-password).

To use the Usergrid password reset flow, do the following:

### STEP 1: Get the password reset request form.

Make a GET request to the following:

    /users/<username>/resetpw
    
For example, using cURL, a request to reset the password for a user with username 'someUser' would look like this:

    curl -x GET https://api.usergrid.com/your-org/your-app/users/someUser/resetpw
    
### STEP 2: Display the returned password reset request form to the user.

The request to ``/resetpw`` will return the HTML for the standard Usergrid password reset request form that you will display to your user. The request form requires the users to provide their username as well as answer a standard CAPTCHA challenge:

    <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
    <html>
    <head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <title>Reset Password</title>
    <link rel="stylesheet" type="text/css" href="/css/styles.css" />
    </head>
    <body>
        <div class="dialog-area">
            
            <form class="dialog-form" action="" method="post">
                <fieldset>
                    <p>
                        Enter the captcha to have your password reset instructions sent to
                        someUser@adomain.com
                    </p>
                    <p id="human-proof"></p>
                    <script type="text/javascript" src="https://www.google.com/recaptcha/api/challenge?k=6LdSTNESAAAAAKHdVglHmMu86_EoYxsJjqQD1IpZ"></script>

                    <p class="buttons">
                        <input type="submit" value="submit" />
                    </p>
                </fieldset>
            </form>
        </div>
    </body>
    </html>
		
You can apply any additional styling you wish to the form to make it match the style of your app before displaying it to the user.

### STEP 3: Let Usergrid handle the rest!

Once the user submits the form with their username, they will receive an email from Usergrid that contains a link to the password reset form, where they can specify a new password. The user entity will be updated immediately.
