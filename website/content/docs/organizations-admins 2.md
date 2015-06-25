---
title: Organizations & Admins
category: docs
layout: docs
---

Organizations
-------------

An organization represents the highest level of the Apache Usergrid data
hierarchy. It contains applications (and the entities and collections
they contain) and is associated with one or more administrators. An
organization can be representative of a company, team, or project. It
allows multiple applications  to be shared within the organization with
other administrators.

Using the App services API, you can create an organization through a
form post and get an organization by UUID or name. In addition, you can
activate or reactivate an organization, generate and retrieve an
organization's client credentials, and get an organization's activity
feed. You can also create an organization application through a form
post, generate and retrieve credentials for the application, and delete
the application. You can also get the applications in an organization.
Additionally, you can add an admin user to an organization, get the
admin users in an organization, and remove an admin user from an
organization.

Property                                Type      Description
--------------------------------------- --------- ---------------------------------------------------------------------
uuid                                    UUID      Organization’s unique entity ID
type                                    string    "organization"
created                                 long      [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
modified                                long      [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
organization                            string    The name of the organization.
username                                string    The username of the administrator.
name                                    string    The name of the administrator.
email                                   string    The email address of the administrator.
password                                string    The password of the administrator. (create-only)



### Activating an organization

Use the GET method to activate an organization from a link provided in
an email notification.

#### Request URL

GET
/organizations|orgs/{org\_name}|{uuid}/activate?token={token}&confirm={confirm\_email}

Parameters

  Parameter                   Description
  --------------------------- -------------------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.
  string token                Activation token (supplied via email).
  boolean confirm\_email      Send confirmation email (false is the default).

 

#### Example - Request

-   [cURL](#curl_activate_org)
-   [JavaScript (HTML5)](#javascript_activate_org)
-   [Ruby](#ruby_activate_org)
-   [Node.js](#nodejs_activate_org)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/activate?token=33dd0563-cd0c-11e1-bcf7-12313d1c4491"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/testorg/activate'].get params: { token: '33dd0563-cd0c-11e1-bcf7-12313d1c4491' }

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/orgs/testorg/activate',
        qs:{token:'33dd0563-cd0c-11e1-bcf7-12313d1c4491'}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "activate organization",
      "timestamp": 1337928462810,              
      "duration": 3342
    }

### Reactivating an organization

Use the GET method to reactivate an organization.

#### Request URI

GET /organizations|orgs/{org\_name}|{uuid}/reactivate

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

 

#### Example - Request

-   [cURL](#curl_reactivate_org)
-   [JavaScript (HTML5)](#javascript_reactivate_org)
-   [Ruby](#ruby_reactivate_org)
-   [Node.js](#nodejs_reactivate_org)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/reactivate"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

 

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/testorg/reactivate']

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/orgs/testorg/reactivate'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "reactivate organization",
      "timestamp": 1349385280891,
      "duration": 3612
    }

### Generating organization client credentials

Use the POST method to generate new credentials for an organization
client.

#### Request URI

POST /organizations|orgs/{org\_name}|{uuid}/credentials

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

#### Example - Request

-   [cURL](#curl_gen_org_credentials)
-   [JavaScript](#javascript_gen_org_credentials)
-   [Ruby](#ruby_gen_org_credentials)
-   [Node.js](#nodejs_gen_org_credentials)

<!-- -->

    curl -X POST "https://api.usergrid.com/management/orgs/credentials"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/credentials'].create

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'management/orgs/credentials'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "generate organization client credentials",
      "timestamp": 1349385795647,
      "duration": 7,
      "credentials":  {
        "client_id": "c2V7N61DY90MCdG78xIxPRxFdQ",                  
        "client_secret": "c2V7WEdXIutZWEkWdySLCt_lYDFVMMN"                      
      }
    }

### Retrieving organization client credentials

Use the GET method to retrieve the credentials for an organization
client.

#### Request URL

GET /organizations|orgs/{org\_name}|{uuid}/credentials

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

#### Example - Request

-   [cURL](#curl_get_org_credentials)
-   [JavaScript (HTML5)](#javascript_get_org_credentials)
-   [Ruby](#ruby_get_org_credentials)
-   [Node.js](#nodejs_get_org_credentials)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/credentials"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/testorg/credentials']

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/orgs/testorg/credentials'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "get organization client credentials",
      "timestamp": 1349386672984,
      "duration": 690,
      "credentials":  {
        "client_id": "c2V7N61DY90MCdG78xIxPRxFdQ",                  
        "client_secret": "c2V7WEdXIutZWEkWdySLCt_lYDFVMMN"                      
      }
    }

### Getting an organization's activity feed

Use the GET method to get an organization's activity feed.

#### Request URI

GET /organizations|orgs/{org\_name}|{uuid}/feed

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

#### Example - Request

-   [cURL](#curl_get_org_feed)
-   [JavaScript (HTML5)](#javascript_get_org_feed)
-   [Ruby](#ruby_get_org_feed)
-   [Node.js](#nodejs_get_org_feed)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/feed"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    activities = mgmt['orgs/testorg/feed'].collection

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/orgs/testorg/feed'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
     {
      "action": "get organization feed",
      "status": "ok",
      "entities":  [
         {
          "uuid": "cf4d981c-fe90-11e1-95c8-12313b122c56",
          "type": "activity",
          "created": 1347643370454,
          "modified": 1347643370454,
          "actor":  {
            "displayName": "tester123",
            "objectType": "person",
            "uuid": "327b527f-cd0c-11e1-bcf7-12313d1c4491",
            "entityType": "user"
          },
          "category": "admin",
          "metadata":  {
            "cursor": "gGkAAQMAgGkABgE5xc3r1gCAdQAQz02YHP6QEeGVyBIxOxIsVgCAdQAQz3SoH_6QEeGVyBIxOxIsVgA",
            "path": "/groups/33dd0563-cd0c-11e1-bcf7-12313d1c4491/feed/cf4d981c-fe90-11e1-95c8-12313b122c56"
          },
    "object":  {
            "displayName": "testapp2",
            "objectType": "Application",
            "uuid": "cede5b7e-fe90-11e1-95c8-12313b122c56",
            "entityType": "application_info"
          },
          "published": 1347643370454,
          "title": "<a mailto="mailto:tester123@hotmail.com">tester123 (tester123@hotmail.com)</a> created a new application named testapp2",
          "verb": "create"
        },...
    ,
      "timestamp": 1349387253811,

### Creating an organization application

Use the POST method to create an application for an organization through
a form post.

#### Request URI

POST /organizations|orgs/{org\_name}|{org\_uuid}/apps {request body}

#### Parameters

Parameter

Description

request body

The following set of organization properties supplied through a form:.

Property

Description

access token (string)

The OAuth2 access token.

name (string)

The name of the application.

 

#### Example - Request

-   [cURL](#curl_create_org_app)
-   [JavaScript (HTML5)](#javascript_create_org_app)
-   [Ruby](#ruby_create_org_app)
-   [Node.js](#nodejs_create_org_app)

<!-- -->

    curl -X -i POST "https://api.usergrid.com/management/orgs/testorg/apps" -d '{"access_token":"YWMtmNIFFBI6EeKvXSIACowF7QAAATpL0bVQtGOWe8PMwLfQ9kD_VKQa7IH4UBQ","name":"testapp1"}'

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

 

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mynewapp = mgmt['orgs/testorg/apps'].create name: 'testapp1', access_token: 'YWMtmNIFFBI6EeKvXSIACowF7QAAATpL0bVQtGOWe8PMwLfQ9kD_VKQa7IH4UBQ'

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'POST',
        endpoint:'management/orgs/testorg/apps',
        body:{ 
              access_token:'YWMtmNIFFBI6EeKvXSIACowF7QAAATpL0bVQtGOWe8PMwLfQ9kD_VKQa7IH4UBQ', 
              name:'testapp1' 
        }    
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call        
        }
    });

#### Example - Response

    {
      "action": "new application for organization",
      "timestamp": 1338914698135,
      "duration": 701
    }

### Deleting an organization application

Use the DELETE method to delete an application from an organization.

#### Request URI

DELETE
/organizations|orgs/{org\_name}|{org\_uuid}/apps/{app\_name}|{app\_uuid}

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.
  string app\_name|arg uuid   Application name or application UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

#### Example - Request

-   [cURL](#curl_delete_org_app)
-   [JavaScript (HTML5)](#javascript_delete_org_app)
-   [Ruby](#ruby_delete_org_app)
-   [Node.js](#nodejs_delete_org_app)

<!-- -->

    curl -X DELETE "https://api.usergrid.com/management/orgs/testorg/apps/testapp1"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

 

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/testorg/apps/testapp1'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'management/orgs/testorg/apps/testapp1'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "delete application from organization",
      "timestamp": 1349817715666,
      "duration": 0
    }

### Generating application credentials

Use the POST method to generate the client ID and client secret
credentials for an application in an organization.

#### Request URI

POST
/organizations|orgs/{org\_name}|{uuid}/applications|apps/{app\_name}|{uuid}/credentials

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.
  string app\_name|arg uuid   Application name or application UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

#### Example - Request

-   [cURL](#curl_gen_app_credentials)
-   [JavaScript (HTML5)](#javascript_gen_app_credentials)
-   [Ruby](#ruby_gen_app_credentials)
-   [Node.js](#nodejs_gen_app_credentials)

<!-- -->

    curl -X POST "https://api.usergrid.com/management/orgs/testorg/apps/testapp1/credentials"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/testorg/apps/testapp1/credentials'].create

The example assumes use of the [Node.js
module](https://github.com/scottganyo/usergrid_iron).

    var options = {
        method:'POST',
        endpoint:'management/orgs/testorg/apps/testapp1/credentials'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — POST failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "generate application client credentials",
      "timestamp": 1349815979529,
      "duration": 535,
      "credentials":  {
        "client_id": "YXA7ygil-f3TEeG-yhIxPQK1cQ",
        "client_secret": "YXA65gYlqja8aYYSAy8Ox3Vg5aRZp48"
      }
    }

### Getting application credentials

Use the GET method to retrieve the client ID and client secret
credentials for an application in an organization.

#### Request URI

GET
/organizations|orgs/{org\_name}|{uuid}/applications|apps/{app\_name}|{uuid}/credentials

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.
  string app\_name|arg uuid   Application name or application UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

#### Example - Request

-   [cURL](#curl_get_app_credentials)
-   [JavaScript (HTML5)](#javascript_get_app_credentials)
-   [Ruby](#ruby_get_app_credentials)
-   [Node.js](#nodejs_get_app_credentials)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/apps/testapp1/credentials"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/testorg/apps/testapp1/credentials']

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/orgs/testorg/apps/testapp1/credentials'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "get application client credentials",
      "timestamp": 1349816819545,
      "duration": 7,
      "credentials":  {
        "client_id": "YXA7ygil-f3TEeG-yhIxPQK1cQ",
        "client_secret": "YXA65gYlqja8aYYSAy8Ox3Vg5aRZp48"
      }
    }

### Getting the applications in an organization

Use the GET method to retrieve the applications in an organization.

#### Request URI

GET /organizations|orgs/{org\_name}|{uuid}/applications|apps

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

#### Example - Request

-   [cURL](#curl_get_apps_org)
-   [JavaScript (HTML5)](#javascript_get_apps_org)
-   [Ruby](#ruby_get_apps_org)
-   [Node.js](#nodejs_get_apps_org)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/apps"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

 

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    apps = mgmt['orgs/testorg/apps'].collection

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/orgs/testorg/apps'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

 

#### Example - Response

    {
      "action": "get organization application",
      "data":  {
        "testorg/sandbox": "3500ba10-cd0c-11e1-bcf8-12313d1c5591",
        "testorg/testapp1": "be09a5f9-fdd3-11e1-beca-12313d027361",
        "testorg/testapp2": "cede5b8e-fe90-11e1-65c8-12313b111c56"    
      },
      "timestamp": 1349815338635,
      "duration": 22
    }

### Adding an admin user to an organization

Use the PUT method to add an existing admin user to an organization.

#### Request URI

PUT
/organizations|orgs/{org\_name}|{org\_uuid}/users/{username|email|uuid}

#### Parameters

  Parameter                               Description
  --------------------------------------- ----------------------------------------------
  string org\_name|arg org\_uuid          Organization name or organization UUID.
  string username|string email|arg uuid   User name, user email address, or user UUID.

 

#### Example - Request

-   [cURL](#curl_add_admin_user)
-   [JavaScript (HTML5)](#javascript_add_admin_user)
-   [Ruby](#ruby_add_admin_user)
-   [Node.js](#nodejs_add_admin_user)

<!-- -->

    curl -X PUT "https://api.usergrid.com/management/orgs/testorg/users/test123"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/testorg/users/test123'].put nil

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'PUT',
        endpoint:'management/orgs/testorg/users/test123'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — PUT failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "add user to organization",
      "status": "ok",
      "data":  {
        "user":  {
          "applicationId": "00000000-0000-0000-0000-000000000001",
          "username": "tester123",
          "name": "test",
          "email": "tester123@hotmail.com",
          "activated": true,
          "disabled": false,
          "uuid": "335b527f-cd0d-11e1-bef8-12331d1c5591",
          "adminUser": true,
          "displayEmailAddress": "tester123 <tester123@hotmail.com>",
          "htmldisplayEmailAddress": "tester123 <<a href="mailto:tester123@hotmail.com">tester123@hotmail.com</a>>"
        }
      },
      "timestamp": 1349390189106,
      "duration": 11808
    }

### Getting the admin users in an organization

Use the GET method to retrieve details about the admin users in an
organization.

#### Request URI

GET /organizations|orgs/{org\_name}|{org\_uuid}/users

#### Parameters

  Parameter                        Description
  -------------------------------- -----------------------------------------
  string org\_name|arg org\_uuid   Organization name or organization UUID.

 

#### Example - Request

-   [cURL](#curl_get_admin_users)
-   [JavaScript (HTML5)](#javascript_get_admin_users)
-   [Ruby](#ruby_get_admin_users)
-   [Node.js](#nodejs_get_admin_users)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/users"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    admins = mgmt['orgs/testorg/users']

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/orgs/testorg/users'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "get organization users",
      "data":  {
        "user":  {
          "applicationId": "00000000-0000-0000-0000-000000000001",
          "username": "tester123",
          "name": "test",
          "email": "tester123@hotmail.com",
          "activated": true,
          "disabled": false,
          "uuid": "335b527f-cd0d-11e1-bef8-12331d1c5591",
          "adminUser": true,
          "displayEmailAddress": "tester123 <tester123@hotmail.com>",
          "htmldisplayEmailAddress": "tester123 <<a href="mailto:tester123@hotmail.com">tester123@hotmail.com</a>>"
        }
      },
      "timestamp": 13494542201685,
      "duration": 10
    }

### Removing an admin user from an organization

Use the DELETE method to remove an admin user from an organization.

#### Request URI

DELETE
/organizations|orgs/{org\_name}|{org\_uuid}/users/{username|email|uuid}

#### Parameters

  Parameter                               Description
  --------------------------------------- ----------------------------------------------
  string org\_name|arg org\_uuid          Organization name or organization UUID.
  string username|string email|arg uuid   User name, user email address, or user UUID.

 

#### Example - Request

-   [cURL](#curl_delete_admin_user)
-   [JavaScript (HTML5)](#javascript_delete_admin_user)
-   [Ruby](#ruby_delete_admin_user)
-   [Node.js](#nodejs_delete_admin_user)

<!-- -->

    curl -X DELETE "https://api.usergrid.com/management/orgs/testorg/users/test123"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['orgs/testorg/users/test123'].delete

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'DELETE',
        endpoint:'management/orgs/testorg/users/test123'
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — DELETE failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

#### Example - Response

    {
      "action": "remove user from organization",
      "status": "ok",
      "data":  {
        "user":  {
          "applicationId": "00000000-0000-0000-0000-000000000001",
          "username": "tester123",
          "name": "test",
          "email": "tester123@hotmail.com",
          "activated": true,
          "disabled": false,
          "uuid": "335b527f-cd0d-11e1-bef8-12331d1c5591",
          "adminUser": true,
          "displayEmailAddress": "tester123 <tester123@hotmail.com>",
          "htmldisplayEmailAddress": "tester123 <<a href="mailto:tester123@hotmail.com">tester123@hotmail.com</a>>"
        }
      },
      "timestamp": 1349453590005,
      "duration": 727
    }



Admins
------

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

### Creating an admin user

Use the POST method to create an admin user.

#### Request URI

POST /management/organizations/{org}/users {request body}

#### Parameters

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

 

#### Example - Request

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

#### Example - Response

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

### Updating an admin user

Use the PUT method to update an admin user.

#### Request URI

PUT /management/organizations/{org}/users/{user|username|email|uuid}
{request body}

#### Parameters

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

 

#### Example - Request

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

#### Example - Response

    {
      "action": "update user info",
      "timestamp": 1349479321874,
      "duration": 0
    }

### Getting an admin user

Use the GET method to retrieve details about an admin user.

#### Request URI

GET /management/organizations/{org}/users/{user|username|email|uuid}

#### Parameters

  Parameter                                           Description
  --------------------------------------------------- -----------------------------------------------
  string user|string username|string email|arg uuid   Admin username, name, email address, or UUID.

 

#### Example - Request

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

#### Example - Response

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

### Setting an admin user's password

Use the PUT method to update an admin user's password.

#### Request URI

PUT
/management/organizations/{org}/users/{user|username|email|uuid}/password
{request body}

#### Parameters

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

 

#### Example - Request

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

#### Example - Response

    {
      "action": "set user password",
      "timestamp": 1349714010142,
      "duration": 0
    }

### Resetting an admin user's password

Resetting an admin user's password is a two step process. In the first
step, you initiate the password reset. This returns a browser page. The
page includes a field for the user to enter his or her email address,
and a field to enter a response to a Captcha challenge. In the second
step, you handle the user's responses from the form.

#### Initiating a password reset

Use the GET method to initiate the password reset.

#### Request URI

GET /management/organizations/{org}/users/resetpw

#### Example - Request

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

#### Example - Response

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

#### Completing a password reset

Use the POST method to complete the password reset.

#### Request URI

POST /management/organizations/{org}/users/resetpw {request body}

#### Parameters

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

 

#### Example - Request

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

#### Example - Response

    {
      "action": "reset user password",
      "timestamp": 13546154010321,
      "duration": 0
    }

### Activating an admin user

Use the GET method to activate an admin user from a link provided in an
email notification.

#### Request URI

GET
/management/organizations/{org}/users/{user|username|email|uuid}/activate?token={token}&confirm={confirm\_email}

#### Parameters

  Parameter                                           Description
  --------------------------------------------------- -------------------------------------------------
  string user|string username|string email|arg uuid   Admin username, name, email address, or UUID.
  string token                                        Activation token (supplied via email).
  boolean confirm\_email                              Send confirmation email (false is the default).

 

#### Example - Request

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

#### Example - Response

    {
      "action": "activate user",
      "timestamp": 1349718021324,
      "duration": 0
    }

### Reactivating an admin user

Use the GET method to reactivate an admin user.

#### Request URI

GET
/management/organizations/{org}/users/{user|username|email|uuid}/reactivate

#### Parameters

  Parameter                                           Description
  --------------------------------------------------- -----------------------------------------------
  string user|string username|string email|arg uuid   Admin username, name, email address, or UUID.

 

#### Example - Request

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

#### Example - Response

    {
      "action": "reactivate user",
      "timestamp": 1349735217217,
      "duration": 3541
    }

### Getting an admin user's activity feed

Use the GET method to retrieve an admin user's activity feed.

#### Request URI

GET
/management/organizations/{org}/users/{user|username|email|uuid}/feed

#### Parameters

  Parameter                                           Description
  --------------------------------------------------- -----------------------------------------------
  string user|string username|string email|arg uuid   Admin username, name, email address, or UUID.

#### Example - Request

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

#### Example - Response

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
