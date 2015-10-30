# Application
You can create a new application in an organization through the Admin portal. The Admin portal creates the new application by issuing a post against the management endpoint (see the [Creating an Application](../orgs-and-apps/application.html#creating-an-application) section in Organization for details). If you need to create an application programmatically in your app, you can also use the API to do this. You can access application entities using your app name or UUID, prefixed with the organization name or UUID:

    https://api.usergrid.com/{org_name|uuid}/{app_name|uuid}

Most mobile apps never access the application entity directly. For example you might have a server-side web app that accesses the application entity for configuration purposes. If you want to access your application entity programmatically, you can use the API.

## Creating an application
To create an application you POST a JSON object containing (at a minimum) the name of the new application. 
You will also need to pass authentication credentials.

### Request URI

    POST /management/organizations|orgs/{org_name}|{org_uuid}/apps {request body}

Parameters

Parameter	    Sent in       Description
---------       -------       ----------- 
grant_type 	    Query string  Only the value 'client_credentials' is supported.
client_id 	    Query string  The org-level client id for your org, found in the 'Org Administration' menu of Usergrid portal. 
client_secret 	Query string  The org-level client secret for your org, found in the 'Org Administration' menu Usergrid portal.
name            Request Body  The name of the application.

### Example - Request

    curl -X -i POST "https://api.usergrid.com/management/orgs/testorg/apps?grant_type=client_credentials&client_id=b3U68vghI6FmEeKn9wLoGtzz0A&client_secret=b3U6ZuZ5_U8Y-bOaViJt0OyRkJFES-A" -d '{"name":"testapp1"}'
    
### Example - Response

    {
      "action": "new application for organization",
      "timestamp": 1338914698135,
      "duration": 701
    }

## Generating application credentials
Use the POST method to generate the client ID and client secret credentials for an application in an organization.

### Request URI

    POST /organizations|orgs/{org_name}|{uuid}/applications|apps/{app_name}|{uuid}/credentials

Parameters

Parameter	                Description
---------                   ----------- 
string org_name|arg uuid	Organization name or organization UUID.
string app_name|arg uuid	Application name or application UUID.

Note: You also need to provide a valid access token with the API call. See [Authenticating users and application clients](../security_and_auth/authenticating-users-and-application-clients.html) for details.

### Example - Request

    curl -X POST "https://api.usergrid.com/management/orgs/testorg/apps/testapp1/credentials"
    
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

## Getting application credentials
Use the GET method to retrieve the client ID and client secret credentials for an application in an organization.

### Request URI

    GET /organizations|orgs/{org_name}|{uuid}/applications|apps/{app_name}|{uuid}/credentials

Parameters

Parameter	                Description
---------                   -----------
string org_name|arg uuid	Organization name or organization UUID.
string app_name|arg uuid	Application name or application UUID.

Note: You also need to provide a valid access token with the API call. See [Authenticating users and application clients](../security_and_auth/authenticating-users-and-application-clients.html) for details.

### Example - Request

    curl -X GET "https://api.usergrid.com/management/orgs/testorg/apps/testapp1/credentials"
    
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
    
