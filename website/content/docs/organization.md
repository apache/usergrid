---
title: Organization
category: docs
layout: docs
---

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

Creating an organization
------------------------

Use the POST method to create an organization through a form post.

### Request URI

POST /organizations|orgs {request body}

### Parameters

Parameter

Description

request body

The following set of organization properties supplied through a form:

Property

Description

organization (string)

The name of the organization.

username (string)

The username of the administrator.

name (string)

The name of the administrator.

email (string)

The email address of the administrator.

password (string)

The password of the administrator.

 

### Example - Request

-   [cURL](#curl_create_org)
-   [JavaScript (HTML5)](#javascript_create_org)
-   [Ruby](#ruby_create_org)
-   [Node.js](#nodejs_create_org)


```bash
$ curl -X POST "https://api.usergrid.com/management/orgs" \
       -d '{"password":"test12345","email":"tester123@hotmail.com","name":"test","username":"test123","organization":"testorg"}'
```

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

````ruby
mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
management.create_organization 'testorg', 'test123', 'test', 'tester123@hotmail.com', 'test12345'
````

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

```javascript
var options = {
    method:'POST',
    endpoint:'management/orgs',
    body:{ 
          password:'test12345', 
          email:'tester12345@gmail.com', 
          name:'test', 
          username:'tes123', 
          organization:'testorg' 
    }    
};
client.request(options, function (err, data) {
    if (err) {
        //error — POST failed
    } else {
        //success — data will contain raw results from API call        
    }
});
```

### Example - Response

```javascript
{
  "action": "new organization",
  "status": "ok",
  "data":  {
    "owner":  {
      "applicationId": "00000000-0000-0000-0000-000000000001",
      "username": "tester123",
      "name": "test",
      "email": "tester123@hotmail.com",
      "activated": false,
      "disabled": false,
      "uuid": "48c92c73-0d7e-11e2-98b9-12313d288ee0",
      "adminUser": true,
      "displayEmailAddress": "tester123 <tester123@hotmail.com>",
      "htmldisplayEmailAddress": "tester123 <<a href="mailto:tester123@hotmail.com">tester123@hotmail.com</a>>"
    },
    "organization":  {
      "name": "testorg",
      "uuid": "5de0bb69-0d7f-11e2-87b9-12313d288ff0"
    }
  },
  "timestamp": 1349284674173,
  "duration": 21376
}
```

Getting an organization
-----------------------

Use the GET method to retrieve an organization given a specified UUID or
username.

### Request URI

GET /organizations|orgs/{org\_name}|{uuid}

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

### Example - Request

-   [cURL](#curl_get_org)
-   [JavaScript (HTML5)](#javascript_get_org)
-   [Ruby](#ruby_get_org)
-   [Node.js](#nodejs_get_org)

```bash
curl -X GET "https://api.usergrid.com/management/orgs/testorg"
```

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

```ruby
mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
org = mgmt.organization 'testorg'
```

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

```javascript
var options = {
    method:'GET',
    endpoint:'management/orgs/testorg'
};
client.request(options, function (err, data) {
    if (err) {
        //error — GET failed
    } else {
        //success — data will contain raw results from API call        
    }
});
```

### Example - Response

```javascript
{
  "timestamp": 1349286861746,
  "duration": 18,
  "organization":  {
    "users":  {
      "tester123":  {
        "applicationId": "00000000-0000-0000-0000-000000000001",
        "username": "tester123",
        "name": "test",
        "email": "tester123@hotmail.com",
        "activated": true,
        "disabled": false,
        "uuid": "327b527f-cd0c-11e1-bcf7-12313d1c4491",
        "adminUser": true,
        "displayEmailAddress": "tester123 <tester123@hotmail.com>",
        "htmldisplayEmailAddress": "tester123 <<a href="mailto:tester123@hotmail.com">tester123@hotmail.com</a>>"
      }
    },
    "name": "testorg",
    "applications":  {
      "tester123/sandbox": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "tester123/testapp1": "be08a5f9-fdd3-11e1-beca-12313d027471",
      "tester123/testapp2": "cede5b7e-fe90-11e1-95c8-12313b122c56"
    },
    "uuid": "33dd0563-cd0c-11e1-bcf7-12313d1c4491"
  }
```

Activating an organization
--------------------------

Use the GET method to activate an organization from a link provided in
an email notification.

### Request URL

GET
/organizations|orgs/{org\_name}|{uuid}/activate?token={token}&confirm={confirm\_email}

Parameters

  Parameter                   Description
  --------------------------- -------------------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.
  string token                Activation token (supplied via email).
  boolean confirm\_email      Send confirmation email (false is the default).

 

### Example - Request

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

### Example - Response

    {
      "action": "activate organization",
      "timestamp": 1337928462810,              
      "duration": 3342
    }

Reactivating an organization
----------------------------

Use the GET method to reactivate an organization.

### Request URI

GET /organizations|orgs/{org\_name}|{uuid}/reactivate

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

 

### Example - Request

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

### Example - Response

    {
      "action": "reactivate organization",
      "timestamp": 1349385280891,
      "duration": 3612
    }

Generating organization client credentials
------------------------------------------

Use the POST method to generate new credentials for an organization
client.

### Request URI

POST /organizations|orgs/{org\_name}|{uuid}/credentials

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

### Example - Request

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

### Example - Response

    {
      "action": "generate organization client credentials",
      "timestamp": 1349385795647,
      "duration": 7,
      "credentials":  {
        "client_id": "c2V7N61DY90MCdG78xIxPRxFdQ",                  
        "client_secret": "c2V7WEdXIutZWEkWdySLCt_lYDFVMMN"                      
      }
    }

Retrieving organization client credentials
------------------------------------------

Use the GET method to retrieve the credentials for an organization
client.

### Request URL

GET /organizations|orgs/{org\_name}|{uuid}/credentials

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

### Example - Request

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

### Example - Response

    {
      "action": "get organization client credentials",
      "timestamp": 1349386672984,
      "duration": 690,
      "credentials":  {
        "client_id": "c2V7N61DY90MCdG78xIxPRxFdQ",                  
        "client_secret": "c2V7WEdXIutZWEkWdySLCt_lYDFVMMN"                      
      }
    }

Getting an organization's activity feed
---------------------------------------

Use the GET method to get an organization's activity feed.

### Request URI

GET /organizations|orgs/{org\_name}|{uuid}/feed

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

### Example - Request

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

### Example - Response

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

Creating an organization application
------------------------------------

Use the POST method to create an application for an organization through
a form post.

### Request URI

POST /organizations|orgs/{org\_name}|{org\_uuid}/apps {request body}

### Parameters

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

 

### Example - Request

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

### Example - Response

    {
      "action": "new application for organization",
      "timestamp": 1338914698135,
      "duration": 701
    }

Deleting an organization application
------------------------------------

Use the DELETE method to delete an application from an organization.

### Request URI

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

 

### Example - Request

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

### Example - Response

    {
      "action": "delete application from organization",
      "timestamp": 1349817715666,
      "duration": 0
    }

Generating application credentials
----------------------------------

Use the POST method to generate the client ID and client secret
credentials for an application in an organization.

### Request URI

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

 

### Example - Request

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

### Example - Response

    {
      "action": "generate application client credentials",
      "timestamp": 1349815979529,
      "duration": 535,
      "credentials":  {
        "client_id": "YXA7ygil-f3TEeG-yhIxPQK1cQ",
        "client_secret": "YXA65gYlqja8aYYSAy8Ox3Vg5aRZp48"
      }
    }

Getting application credentials
-------------------------------

Use the GET method to retrieve the client ID and client secret
credentials for an application in an organization.

### Request URI

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

 

### Example - Request

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

### Example - Response

    {
      "action": "get application client credentials",
      "timestamp": 1349816819545,
      "duration": 7,
      "credentials":  {
        "client_id": "YXA7ygil-f3TEeG-yhIxPQK1cQ",
        "client_secret": "YXA65gYlqja8aYYSAy8Ox3Vg5aRZp48"
      }
    }

Getting the applications in an organization
-------------------------------------------

Use the GET method to retrieve the applications in an organization.

### Request URI

GET /organizations|orgs/{org\_name}|{uuid}/applications|apps

Parameters

  Parameter                   Description
  --------------------------- -----------------------------------------
  string org\_name|arg uuid   Organization name or organization UUID.

**Note:** You also need to provide a valid access token with the API
call. See [Authenticating users and application
clients](/authenticating-users-and-application-clients) for details.

 

### Example - Request

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

 

### Example - Response

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

Adding an admin user to an organization
---------------------------------------

Use the PUT method to add an existing admin user to an organization.

### Request URI

PUT
/organizations|orgs/{org\_name}|{org\_uuid}/users/{username|email|uuid}

### Parameters

  Parameter                               Description
  --------------------------------------- ----------------------------------------------
  string org\_name|arg org\_uuid          Organization name or organization UUID.
  string username|string email|arg uuid   User name, user email address, or user UUID.

 

### Example - Request

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

### Example - Response

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

Getting the admin users in an organization
------------------------------------------

Use the GET method to retrieve details about the admin users in an
organization.

### Request URI

GET /organizations|orgs/{org\_name}|{org\_uuid}/users

### Parameters

  Parameter                        Description
  -------------------------------- -----------------------------------------
  string org\_name|arg org\_uuid   Organization name or organization UUID.

 

### Example - Request

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

### Example - Response

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

Removing an admin user from an organization
-------------------------------------------

Use the DELETE method to remove an admin user from an organization.

### Request URI

DELETE
/organizations|orgs/{org\_name}|{org\_uuid}/users/{username|email|uuid}

### Parameters

  Parameter                               Description
  --------------------------------------- ----------------------------------------------
  string org\_name|arg org\_uuid          Organization name or organization UUID.
  string username|string email|arg uuid   User name, user email address, or user UUID.

 

### Example - Request

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

### Example - Response

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

 
