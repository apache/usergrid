---
title: Admin user
category: docs
layout: docs
---

Admin user
==========

[See all management
resources](/docs/usergrid/content/management-resources)[![](/docs/sites/docs/files/learnmore%20arrow_0.png)](/docs/usergrid/content/management-resources)

An admin user has full access to perform any operation on all
organization accounts of which the admin user is a member. Using the App
services API, you can create, update, or retrieve an admin user. You can
also set or reset an admin user's password, activite or reactivate an
admin user, and get an admin user's activity feed.

In addition, you can add,  retrieve, or remove an admin user from an
organization. For information on these organization-related operations,
see [Organization](/organization).

**Note:** Although not shown in the API examples below, you need to
provide a valid access token with each API call. See [Authenticating
users and application
clients](/authenticating-users-and-application-clients) for details.

Creating an admin user
----------------------

Use the POST method to create an admin user.

### Request URI

POST /management/organizations/{org}/users {request body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| request body                         | One or more sets of user properties, |
|                                      | of which username is mandatory and   |
|                                      | must be unique:                      |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "username" : "jim.admin",      |
|                                      |       "email" : "jim.admin@gmail.com |
|                                      | ",                                   |
|                                      |       "name" : "Jim Admin",          |
|                                      |       "password" : "test12345"       |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

 

### Example - Request

-   [cURL](#curl_create_admin_user)
-   [JavaScript (HTML5)](#javascript_create_admin_user)
-   [Ruby](#ruby_create_admin_user)
-   [Node.js](#nodejs_create_admin_user)

<!-- -->

    curl -X -i POST "https://api.usergrid.com/management/organizations/my-org/users" -d '{"username":"jim.admin","name":"Jim Admin","email":"jim.admin@gmail.com","password":"test12345"}'

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org/'
    result = mgmt.create_user username: 'jim.admin', name: 'Jim Admin', email: 'jim.admin@gmail.com', password: 'test12345'
    jim_admin = result.entity

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'management/organizations/my-org/users',
        body:{ username:'jim.admin', name:'Jim Admin', email:'jim.admin@gmail.com',  
        password:'test12345' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

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

Updating an admin user
----------------------

Use the PUT method to update an admin user.

### Request URI

PUT /management/organizations/{org}/users/{user|username|email|uuid}
{request body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| string user|string username|string   | Admin username, name, email address, |
| email|arg uuid                       | or UUID.                             |
+--------------------------------------+--------------------------------------+
| request body                         | One or more sets of user properties: |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "city" : "San Francisco",      |
|                                      |       "state" : "California"         |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

 

### Example - Request

-   [cURL](#curl_update_admin_user)
-   [JavaScript (HTML5)](#javascript_update_admin_user)
-   [Ruby](#ruby_update_admin_user)
-   [Node.js](#nodejs_update_admin_user)

<!-- -->

    curl -X -i PUT "https://api.usergrid.com/management/organizations/my-org/users/jim.admin" -d '{"city":"San Francisco","state":"California"}'

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org/'
    jim_admin = mgmt['users/jim.admin'].entity
    jim_admin.city = 'San Francisco'
    jim_admin.state = 'California'
    jim_admin.save

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'PUT',
        endpoint:'management/organizations/my-org/users/jim.admin',
        body:{ email:'john.doe@mail.com', city:'San Francisco', state:'California' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — PUT failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

### Example - Response

    {
      "action": "update user info",
      "timestamp": 1349479321874,
      "duration": 0
    }

Getting an admin user
---------------------

Use the GET method to retrieve details about an admin user.

### Request URI

GET /management/organizations/{org}/users/{user|username|email|uuid}

### Parameters

  Parameter                                           Description
  --------------------------------------------------- -----------------------------------------------
  string user|string username|string email|arg uuid   Admin username, name, email address, or UUID.

 

### Example - Request

-   [cURL](#curl_get_admin_user)
-   [JavaScript (HTML5)](#javascript_get_admin_user)
-   [Ruby](#ruby_get_admin_user)
-   [Node.js](#nodejs_get_admin_user)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/jim.admin"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org/'
    jim_admin = mgmt['users/jim.admin'].entity

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/organizations/my-org/users/jim.admin'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

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
      "timestamp": 1349480786906,

Setting an admin user's password
--------------------------------

Use the PUT method to update an admin user's password.

### Request URI

PUT
/management/organizations/{org}/users/{user|username|email|uuid}/password
{request body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| string user|string username|string   | Admin username, name, email address, |
| email|arg uuid                       | or UUID.                             |
+--------------------------------------+--------------------------------------+
| request body                         | The password property and value:     |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "password": "test123"          |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

 

### Example - Request

-   [cURL](#curl_set_admin_pw)
-   [JavaScript (HTML5)](#javascript_set_admin_pw)
-   [Ruby](#ruby_set_admin_pw)
-   [Node.js](#nodejs_set_admin_pw)

<!-- -->

    curl -X -i PUT "https://api.usergrid.com/management/organizations/my-org/users/jim.admin/password" -d '{"password":"test123"}'

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

 

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org'
    jim_admin = mgmt['users/jim.admin'].entity
    jim_admin.password = 'test123'
    jim_admin.save

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'PUT',
        endpoint:'management/organizations/my-org/users/jim.admin',
        body:{ password:'test123' }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — PUT failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action": "set user password",
      "timestamp": 1349714010142,
      "duration": 0
    }

Resetting an admin user's password
----------------------------------

Resetting an admin user's password is a two step process. In the first
step, you initiate the password reset. This returns a browser page. The
page includes a field for the user to enter his or her email address,
and a field to enter a response to a Captcha challenge. In the second
step, you handle the user's responses from the form.

### Initiating a password reset

Use the GET method to initiate the password reset.

### Request URI

GET /management/organizations/{org}/users/resetpw

### Example - Request

-   [cURL](#curl_init_pw_reset)
-   [JavaScript (HTML5)](#javascript_init_pw_reset)
-   [Ruby](#ruby_init_pw_reset)
-   [Node.js](#nodejs_init_pw_reset)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/resetpw"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org/'
    resetpw = mgmt['users/resetpw']

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/organizations/my-org/users/resetpw'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

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

### Completing a password reset

Use the POST method to complete the password reset.

### Request URI

POST /management/organizations/{org}/users/resetpw {request body}

### Parameters

+--------------------------------------+--------------------------------------+
| Parameter                            | Description                          |
+======================================+======================================+
| request body                         | Parameters and value for the Captcha |
|                                      | challenge, the admin user's response |
|                                      | to the Captcha challenge, and the    |
|                                      | admin user's email address, for      |
|                                      | example:                             |
|                                      |                                      |
|                                      |     {                                |
|                                      |       "recaptcha_response_field" : " |
|                                      | Atistophanes tseFia",                |
|                                      |       "recaptcha_challenge_field" :  |
|                                      | "Atistophanes tseFia",               |
|                                      |       "email" : "jim.admin@gmail.com |
|                                      | "                                    |
|                                      |     }                                |
+--------------------------------------+--------------------------------------+

 

### Example - Request

-   [cURL](#curl_complete_pw_reset)
-   [JavaScript (HTML5)](#javascript_complete_pw_reset)
-   [Ruby](#ruby_complete_pw_reset)
-   [Node.js](#nodejs_complete_pw_reset)

<!-- -->

    curl -X -i POST "https://api.usergrid.com/management/organizations/my-org/users/resetpw" -d '{"recaptcha_response_field":"Atistophanes tseFia","recaptcha_challenge_field":"Atistophanes tseFia","email":"jim.admin@gmail.com"}'

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

 

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org/'
    resetpw = mgmt['users/resetpw']
    resetpw { recaptcha_response_field: 'Atistophanes tseFia', recaptcha_challenge_field: 'Atistophanes tseFia', email: 'jim.admin@gmail.com' }

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'management/organizations/my-org/users/resetpw',
        body:{ 
           recaptcha_response_field:'Atistophanes tseFia', 
           recaptcha_challenge_field:'Atistophanes tseFia', 
           email:'jim.admin@gmail.com' 
        }
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action": "reset user password",
      "timestamp": 13546154010321,
      "duration": 0
    }

Activating an admin user
------------------------

Use the GET method to activate an admin user from a link provided in an
email notification.

### Request URI

GET
/management/organizations/{org}/users/{user|username|email|uuid}/activate?token={token}&confirm={confirm\_email}

### Parameters

  Parameter                                           Description
  --------------------------------------------------- -------------------------------------------------
  string user|string username|string email|arg uuid   Admin username, name, email address, or UUID.
  string token                                        Activation token (supplied via email).
  boolean confirm\_email                              Send confirmation email (false is the default).

 

### Example - Request

-   [cURL](#curl_activate_admin_user)
-   [JavaScript (HTML5)](#javascript_activate_admin_user)
-   [Ruby](#ruby_activate_admin_user)
-   [Node.js](#nodejs_activate_admin_user)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/jim.admin/activate?token=33dd0563-cd0c-11e1-bcf7-12313d1c4491"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org/'
    mgmt['users/jim.admin/activate'].get params: { token: '33dd0563-cd0c-11e1-bcf7-12313d1c4491' }

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/organizations/my-org/users/jim.admin/activate',
        qs:{token:'33dd0563-cd0c-11e1-bcf7-12313d1c4491'}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action": "activate user",
      "timestamp": 1349718021324,
      "duration": 0
    }

Reactivating an admin user
--------------------------

Use the GET method to reactivate an admin user.

### Request URI

GET
/management/organizations/{org}/users/{user|username|email|uuid}/reactivate

### Parameters

  Parameter                                           Description
  --------------------------------------------------- -----------------------------------------------
  string user|string username|string email|arg uuid   Admin username, name, email address, or UUID.

 

### Example - Request

-   [cURL](#curl_reactivate_admin_user)
-   [JavaScript (HTML5)](#javascript_reactivate_admin_user)
-   [Ruby](#ruby_reactivate_admin_user)
-   [Node.js](#nodejs_reactivate_admin_user)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/jim.admin/reactivate"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org/'
    mgmt['users/jim.admin/reactivate']

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/organizations/my-org/users/jim.admin/reactivate'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    {
      "action": "reactivate user",
      "timestamp": 1349735217217,
      "duration": 3541
    }

Getting an admin user's activity feed
-------------------------------------

Use the GET method to retrieve an admin user's activity feed.

### Request URI

GET
/management/organizations/{org}/users/{user|username|email|uuid}/feed

### Parameters

  Parameter                                           Description
  --------------------------------------------------- -----------------------------------------------
  string user|string username|string email|arg uuid   Admin username, name, email address, or UUID.

### Example - Request

-   [cURL](#curl_get_user_feed)
-   [JavaScript (HTML5)](#javascript_get_user_feed)
-   [Ruby](#ruby_get_user_feed)
-   [Node.js](#nodejs_get_user_feed)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/organizations/my-org/users/jim.admin/feed"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/management/organizations/my-org/'
    mgmt['users/jim.admin/feed'].get

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/organizations/my-org/users/jim.admin/feed'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

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
