# Organization

An organization represents the highest level of the API Services BaaS data hierarchy. It contains applications (and the entities and collections they contain) and is associated with one or more administrators. An organization can be representative of a company, team, or project. It allows multiple applications  to be shared within the organization with other administrators.

## Creating an organization
Use the POST method to create an organization through a form post.

### Request URI

    POST /organizations|orgs {request body}

Parameters

Expected to be sent form data in the body of the request.

Parameter	           Description
---------              ----------- 
organization (string)  The name of the organization.
username (string)	   The username of the administrator.
name (string)	       The name of the administrator.
email (string)	       The email address of the administrator.
password (string)	   The password of the administrator.


### Example - Request

    curl -X -i POST "https://api.usergrid.com/management/orgs" -d '{"password":"test12345","email":"tester123@hotmail.com","name":"test","username":"test123","organization":"testorg"}'
    
### Example - Response

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

## Getting an organization
Use the GET method to retrieve an organization given a specified UUID or username.

### Request URI

    GET /organizations|orgs/{org_name}|{uuid}

Parameters

Parameter	        Description
---------           -----------
org_name|arg uuid	Organization name or organization UUID.

Note: You also need to provide a valid access token with the API call. 
See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.

### Example - Request

    curl -X GET "https://api.usergrid.com/management/orgs/testorg"
    
### Example - Response

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
    
## Activating an organization
Use the GET method to activate an organization from a link provided in an email notification.

### Request URL

    GET /organizations|orgs/{org_name}|{uuid}/activate?token={token}&confirm={confirm_email}

Parameters

Parameter	       Description
---------          ----------- 
org_name|arg uuid  Organization name or organization UUID.
token	           Activation token (supplied via email).
confirm_email	   (boolean) Send confirmation email (false is the default).
 

### Example - Request

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/activate?token=33dd0563-cd0c-11e1-bcf7-12313d1c4491"
    
### Example - Response

    {
      "action": "activate organization",
      "timestamp": 1337928462810,              
      "duration": 3342
    }

## Reactivating an organization
Use the GET method to reactivate an organization.

### Request URI

    GET /organizations|orgs/{org_name}|{uuid}/reactivate

Parameters

Parameter	                Description
---------                   ----------- 
string org_name|arg uuid	Organization name or organization UUID.
 

### Example - Request

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/reactivate"
    
### Example - Response

    {
      "action": "reactivate organization",
      "timestamp": 1349385280891,
      "duration": 3612
    }
    
## Generating organization client credentials
Use the POST method to generate new credentials for an organization client.

### Request URI

    POST /organizations|orgs/{org_name}|{uuid}/credentials

Parameters

Parameter	                Description
---------                   ----------- 
string org_name|arg uuid	Organization name or organization UUID.

__Note__: You also need to provide a valid access token with the API call. [Authenticating users and application clients](../security_and_auth/authenticating-users-and-application-clients.html) for details.

### Example - Request

    curl -X POST "https://api.usergrid.com/management/orgs/credentials"
    
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

## Retrieving organization client credentials
Use the GET method to retrieve the credentials for an organization client.

### Request URL

    GET /organizations|orgs/{org_name}|{uuid}/credentials

Parameters

Parameter	                Description
---------                   -----------
string org_name|arg uuid	Organization name or organization UUID.

__Note__: You also need to provide a valid access token with the API call. See [Authenticating users and application clients](../security_and_auth/authenticating-users-and-application-clients.html) for details.

### Example - Request

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/credentials"
    
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

## Getting an organization's activity feed
Use the GET method to get an organization's activity feed.

### Request URI

    GET /organizations|orgs/{org_name}|{uuid}/feed

Parameters

Parameter	                Description
---------                   -----------
string org_name|arg uuid	Organization name or organization UUID.

__Note__: You also need to provide a valid access token with the API call. See [Authenticating users and application clients](../security_and_auth/authenticating-users-and-application-clients.html) for details.

### Example - Request

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/feed"
    
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
      "timestamp": 1349387253811
    }
  
## Getting the applications in an organization
Use the GET method to retrieve the applications in an organization.

### Request URI

    GET /organizations|orgs/{org_name}|{uuid}/applications|apps

Parameters

Parameter	                Description
---------                   ----------- 
string org_name|arg uuid	Organization name or organization UUID.

__Note__: You also need to provide a valid access token with the API call. See [Authenticating users and application clients](../security_and_auth/authenticating-users-and-application-clients.html) for details.

### Example - Request

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/apps"

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
    
## Adding an admin user to an organization
Use the PUT method to add an existing admin user to an organization.

### Request URI

    PUT /organizations|orgs/{org_name}|{org_uuid}/users/{username|email|uuid}

Parameters

Parameter	                            Description
---------                               -----------
string org_name|arg org_uuid	        Organization name or organization UUID.
string username|string email|arg uuid	User name, user email address, or user UUID.
 

### Example - Request

    curl -X PUT "https://api.usergrid.com/management/orgs/testorg/users/test123"

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

## Getting the admin users in an organization

Use the GET method to retrieve details about the admin users in an organization.

### Request URI

    GET /organizations|orgs/{org_name}|{org_uuid}/users

Parameters

Parameter               Description
---------               -----------
org_name|arg org_uuid	Organization name or organization UUID.
 
### Example - Request

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/users"

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

## Removing an admin user from an organization

Use the DELETE method to remove an admin user from an organization. All organizations must have a minimum 
of one user with org admin privileges. If you attempt to delete the last org admin in an organization, 
the API will return a 400 Bad Request error.

### Request URI

    DELETE /organizations|orgs/{org_name}|{org_uuid}/users/{username|email|uuid}

Parameters

Parameter                       Description
---------                       -----------
org_name|arg org_uuid	        Organization name or organization UUID.
username|string email|arg uuid	User name, user email address, or user UUID.
 
### Example - Request

    curl -X DELETE "https://api.usergrid.com/management/orgs/testorg/users/test123"

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
    
    