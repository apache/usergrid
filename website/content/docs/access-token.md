---
title: Access token
category: docs
layout: docs
---

Access token
============

[See all management
resources](/docs/usergrid/content/management-resources)[![](/docs/sites/docs/files/learnmore%20arrow_0.png)](/docs/usergrid/content/management-resources)

An access token carries the credentials and authorization information
needed to access other resources through the Apache Usergrid API. Using the
API, you can obtain an access token.

Requesting an access token
--------------------------

Use the POST method to obtain an access token.

### Request URI

The request URL depends on the access type:

  Access Type        Request URL
  ------------------ ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  Application user   POST /\<org\_id\>/\<app\_id\>/token '{"grant\_type":"password", "username":"\<username\>", "password":"\<password\>"[, "ttl":"\<token\_time\_to\_live\>"]}'
  Application        POST /\<org\_id\>/\<app\_id\>/token '{"grant\_type":"client\_credentials", "client\_id":"\<client\_id\>", "client\_secret":"\<client\_secret\>"[, "ttl":"\<token\_time\_to\_live\>"]}'
  Admin User         POST /token '{"grant\_type":"password", "username":"\<username\>", "password":"\<password\>"[, "ttl":"\<token\_time\_to\_live\>"]}'
  Organization       POST /token '{"grant\_type":"client\_credentials", "client\_id":"\<client\_id\>", "client\_secret":"\<client\_secret\>"}'

See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for further
details about access types.

### Parameters

  Parameter Name   Type     Description
  ---------------- -------- ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  client\_id       string   Organization client ID. You can find this in the admin portal.
  client\_secret   string   Organization client secret. You can find this in the admin portal
  username         string   Value of the User entity username property. 
  password         string   Password stored for this user.
  ttl              long     *Optional.* The amount of time, in miliseconds, that this token will be valid before authentication is required again. This must be less than the accesstokenttl property of the application entity the token is being requested for.

 

### Example - Request (Application user)

-   [cURL](#curl_get_token_appuser)
-   [JavaScript (HTML5)](#javascript_get_token_appuser)
-   [Ruby](#ruby_get_token_appuser)
-   [Node.js](#nodejs_get_token_appuser)

<!-- -->

    curl -X POST -i -H "Content-Type: application/json" “https://api.usergrid.com/<org_name>/<app_name>/token”  -d '{"grant_type":"password","username":"testadmin","password":"testadminpw"}'

The example assumes use of the [JavaScript (HTML5)
SDK](https://github.com/apigee/usergrid-javascript-sdk).

    var username = 'testuser';
    var password = 'testpasswd';
    client.login(username, password,
        function (err) {
            if (err) {
                //error — could not log user in
            } else {
                //success — user has been logged in
                var token = client.token;
            }
        }
    );

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    app = Usergrid::Application.new 'https://api.usergrid.com/my-org/my-app/'
    app.login 'testuser', 'testpasswd'
    token = app.auth_token

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var username = 'testuser';
    var password = 'testpasswd';
    client.login(username, password,
        function (err) {
            if (err) {
                //error — could not log user in
            } else {
                //success — user has been logged in
                var token = client.token;
            }
        }
    );

### Example - Response

    {
      "access_token": "5wuGd-lcEeCUBwBQVsAACA:F8zeMOlcEeCUBwBQVsAACA:YXU6AAABMq0hdy4Lh0ewmmnOWOR-DaepCrpWx9oPmw",
      "expires_in": 3600,
      "user": {
        "uuid": "e70b8677-e95c-11e0-9407-005056c00008",
        "type": "user",
        "username": "testuser",
        "email": "testuser@mail.com",
        "activated": true,
        "created": 1317164604367013,
        "modified": 1317164604367013
      }

### Example - Request (Admin user)

-   [cURL](#curl_get_token_adminuser)
-   [JavaScript (HTML5)](#javascript_get_token_adminuser)
-   [Ruby](#ruby_get_token_adminuser)
-   [Node.js](#nodejs_get_token_adminuser)

<!-- -->

    curl -X POST -i -H "Content-Type: application/json" “https://api.usergrid.com/management/token”  -d '{"grant_type":"password","username":"testadmin","password":"testadminpw"}'

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt.login test, testpass
    token = mgmt.auth_token

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var username = 'testuser';
    var password = 'testpasswd';
    client.login(username, password,
        function (err) {
            if (err) {
                //error — could not log admin user in
            } else {
                //success — admin user has been logged in
                var token = client.token;
            }
        }
    );

### Example - Response

    {
      "access_token": "f_GUbelXEeCfRgBQVsAACA:YWQ6AAABMqz_xUyYeErOkKjnzN7YQXXlpgmL69fvaA",
      "expires_in": 3600,
      "user": {
        "username": "test",
        "email": "test@usergrid.com",
        "organizations": {
          "test-organization": {
            "users": {
              "test": {
                "name": "Test User",
                "disabled": false,
                "uuid": "7ff1946d-e957-11e0-9f46-005056c00008",
                "activated": true,
                "username": "test",
                "applicationId": "00000000-0000-0000-0000-000000000001",
                "email": "test@usergrid.com",
                "adminUser": true,
                "mailTo": "Test User "
              }
            },
            "name": "test-organization",
            "applications": {
              "test-app": "8041893b-e957-11e0-9f46-005056c00008"
            },
            "uuid": "800b8510-e957-11e0-9f46-005056c00008"
          }
        },
        "adminUser": true,
        "activated": true,
        "name": "Test User",
        "mailTo": "Test User ",
        "applicationId": "00000000-0000-0000-0000-000000000001",
        "uuid": "7ff1946d-e957-11e0-9f46-005056c00008",
        "disabled": false
      }
    }
