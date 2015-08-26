# Admin user
An admin user has full access to perform any operation on all organization accounts of which the admin user is a member. Using the API Services BaaS API, you can create, update, or retrieve an admin user. You can also set or reset an admin user's password, activite or reactivate an admin user, and get an admin user's activity feed.

In addition, you can add,  retrieve, or remove an admin user from an organization. For information on these organization-related operations, see [Organization](organization.html).

__Note__: Although not shown in the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security_and_auth/authenticating-users-and-application-clients.html) for details.

## Creating an admin user
Use the POST method to create an admin user.

### Request URI

    POST /management/organizations/{org}/users {request body}

In the request body send a JSON object that represents the new user, for example:

    {
      "username" : "jim.admin",
      "email" : "jim.admin@gmail.com",
      "name" : "Jim Admin",
      "password" : "test12345"
    }
 

### Example - Request

    curl -X -i POST "https://api.usergrid.com/management/organizations/my-org/users" -d '{"username":"jim.admin","name":"Jim Admin","email":"jim.admin@gmail.com","password":"test12345"}'

### Example - Response

    {
      "action": "post",
      "status": "ok",
      "data":  {
        "user":  {
          "applicationId": "00000000-0000-0000-0000-000000000001",
          "username": "jim.admin",
          "name": "Jim Admin",
          "email": "jim.admin@gmail.com",
          "activated": true,
          "disabled": false,
          "uuid": "335b527f-cd0d-11e1-bef8-12331d1c5591",
          "adminUser": true,
          "displayEmailAddress": "jim.admin <jim.admin@gmail.com>",
          "htmldisplayEmailAddress": "jim.admin <a href="mailto:jim.admin@gmail.com">jinm.admin@gmail.com</a>"
        }
      },
      "timestamp": 1349390189106,
      "duration": 11808
    }


## Updating an admin user
Use the PUT method to update an admin user.

### Request URI

    PUT /management/organizations/{org}/users/{user|username|email|uuid} {request body}

Parameters

Parameter	    Description
---------       ----------- 
User identifier Username, name, email address, or UUID.
request body	JSON object containing propties you would like to add/update on user.

For example, to add city and state to user, send this:

    {
      "city" : "San Francisco",
      "state" : "California"
    }
     
### Example - Request

    curl -X -i PUT "https://api.usergrid.com/management/organizations/my-org/users/jim.admin" -d '{"city":"San Francisco","state":"California"}'

### Example - Response

    {
      "action": "update user info",
      "timestamp": 1349479321874,
      "duration": 0
    }

## Getting an admin user
Use the GET method to retrieve details about an admin user.

### Request URI

    GET /management/organizations/{org}/users/{user|username|email|uuid}

Parameters

Parameter	        Description
---------           -----------
User identifier     Admin username, name, email address, or UUID.
 

### Example - Request

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/jim.admin"

### Example - Response

    {
      "action": "get admin user",
      "status": "ok",
      "data":  {
        "username": "jim.admin",
        "token": "YWMt4NqE8Q9GEeLYJhIxPSiO4AAAATo5fQfcG0cEd2h9nwmDmRorkNNrEeQyDOF",
        "email": "edort1@gmail.com",
        "organizations":  {
          "jim.admin":  {
            "users":  {
              "jim.admin":  {
                "applicationId": "00000000-0000-0000-0000-000000000001",
                "username": "jim.admin",
                "name": "Jim Admin",
                "email": "jim.admin@gmail.com",
                "activated": true,
                "disabled": false,
                "uuid": "328b526e-cd0c-11e1-bcf8-12424d1c4491",
                "adminUser": true,
                "displayEmailAddress": "jim.admin <jim.admin@gmail.com>",
                "htmldisplayEmailAddress": "jim.admin <<a href="mailto:jim.admin@gmail.com">jim.admin@gmail.com>"
        },
        ...
        "adminUser": true,
        "activated": true,
        "name": "edort1",
        "applicationId": "00000000-0000-0000-0000-000000000001",
        "uuid": "328b526e-cd0c-11e1-bcf8-12424d1c4491",
        "htmldisplayEmailAddress": "jim.admin <<a href="mailto:jim.admin@gmail.com">jim.admin@gmail.com>>",
        "displayEmailAddress": "jim.admin <jim.admin@gmail.com>",
        "disabled": false
      },
      "timestamp": 1349480786906
    }  

## Setting an admin user's password
Use the PUT method to update an admin user's password.

### Request URI

    PUT /management/users/{user|username|email|uuid}/password {request body}

Parameters

Parameter	        Description
---------           -----------
User identifier     Admin username, name, email address, or UUID.

Expects new and old password to be sent in request body:

    {
      "password": <old_password>
      "newpassword":<new_password>
    }
 
### Example - Request

    curl -X -i PUT "https://api.usergrid.com/management/users/jim.admin/password" -d '{"oldpassword":"test123", "newpassword":"mynewpassword"}'

### Example - Response

    {
      "action": "set user password",
      "timestamp": 1349714010142,
      "duration": 0
    }

## Resetting an admin user's password
Resetting an admin user's password is a two step process. In the first step, you initiate the password reset. This returns a browser page. The page includes a field for the user to enter his or her email address, and a field to enter a response to a Captcha challenge. In the second step, you handle the user's responses from the form.

### Initiating a password reset
Use the GET method to initiate the password reset.

### Request URI

    GET /management/organizations/{org}/users/resetpw

### Example - Request

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/resetpw"

### Example - Response

    <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
    <html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <title>Reset Password</title>
        <link rel="stylesheet" type="text/css" href="../../css/styles.css" />
        <script type="text/javascript">>
            var RecaptchaOptions = {
                theme : 'clean'
            };
        </script>
    </head>
    <body>

        <div class="dialog-area">
            <div class="dialog-form-message">Incorrect Captcha, try again...</div>
            <form class="dialog-form" action="" method="post">
                <fieldset>
                    <p>
                        <label for="email">Please type your <strong>email
                                address</strong> or <strong>username</strong> below.</label>
                    </p>
                    <p>
                        <input class="text_field" id="email" name="email" type="text" />
                    </p>
                    <p id="human-proof"></p>
                    <script type="text/javascript" src="https://www.google.com/recaptcha/api/challenge?k=6LdSTNESAAAAAKHdVglHmMu86_EoYxsJjqQD1IpZ"></script>

                    <p class="buttons">
                        <button type="submit">Submit</button>
                    </p>
                </fieldset>
            </form>
        </div>
    </pre>

## Completing a password reset
Use the POST method to complete the password reset.

### Request URI

    POST /management/organizations/{org}/users/resetpw {request body}

In the request body send parameters and value for the Captcha challenge, the admin user's response to the Captcha challenge, and the admin user's email address, for example:

    {
      "recaptcha_response_field" : "Atistophanes tseFia",
      "recaptcha_challenge_field" : "Atistophanes tseFia",
      "email" : "jim.admin@gmail.com" 
    }

### Example - Request

    curl -X -i POST "https://api.usergrid.com/management/organizations/my-org/users/resetpw" -d '{"recaptcha_response_field":"Atistophanes tseFia","recaptcha_challenge_field":"Atistophanes tseFia","email":"jim.admin@gmail.com"}'

### Example - Response

    {
      "action": "reset user password",
      "timestamp": 13546154010321,
      "duration": 0
    }

## Activating an admin user
Use the GET method to activate an admin user from a link provided in an email notification.

### Request URI

    GET /management/organizations/{org}/users/{user|username|email|uuid}/activate?token={token}&confirm={confirm_email}

Parameters

Parameter	        Description
---------           -----------
User identifier	    Admin username, name, email address, or UUID.
string token	    Activation token (supplied via email).
confirm_email	    Send confirmation email (false is the default).

### Example - Request

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/jim.admin/activate?token=33dd0563-cd0c-11e1-bcf7-12313d1c4491"

### Example - Response

    {
      "action": "activate user",
      "timestamp": 1349718021324,
      "duration": 0
    }

## Reactivating an admin user
Use the GET method to reactivate an admin user.

### Request URI

    GET /management/organizations/{org}/users/{user|username|email|uuid}/reactivate

Parameters

Parameter	        Description
---------           -----------
User identifier	    Admin username, name, email address, or UUID.

## Example - Request

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/jim.admin/reactivate"

### Example - Response

    {
      "action": "reactivate user",
      "timestamp": 1349735217217,
      "duration": 3541
    }

## Getting an admin user's activity feed
Use the GET method to retrieve an admin user's activity feed.

### Request URI

    GET /management/organizations/{org}/users/{user|username|email|uuid}/feed

Parameters

Parameter	        Description
---------           -----------
User identifier	    Admin username, name, email address, or UUID.

### Example - Request

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/jim.admin/feed"

### Example - Response

    {
      "action": "get admin user feed",
      "status": "ok",
     "entities":  [
         {
          "uuid": "cf3e981c-fe80-11e1-95c8-12331b144c65",
          "type": "activity",
          "created": 1347643370454,
          "modified": 1347643370454,
          "actor":  {
            "displayName": "jim.admin",
            "objectType": "person",
            "uuid": "335b527f-cd0d-11e1-bef8-12331d1c5591",
            "entityType": "user"
          },
          "category": "admin",
          "metadata":  {
            "cursor": "gGkAAQMAgGkABgE5xc3r1gCAdQAQz02YHP6QEeGVyBIxOxIsVgCAdQAQz4ZbYf6QEeGVyBIxOxIsVgA",
            "path": "/users/327b527f-cd0c-11e1-bcf7-12313d1c4491/feed/cf4d981c-fe90-11e1-95c8-12313b122c56"
          },
        "object":  {
        ...
        },
        "published": 1342198809251,
                "title": "<a mailto="jim.admingmail.com">jim.admin (jim.admin@gmail.com)</a> created a new organization account named jim.admin",
                "verb": "create"
              }
            ],
      "timestamp": 1349735719320,
    }
