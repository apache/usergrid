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
    

## Deleting and restoring Applications
Usergrid allows you to clean-up your Organizations by deleting old Applications that you no longer need.
With this feature, Applications are not really deleted but they are hidden from view and may be restored later. (At some point in the future, Usergrid may get the ability to completely obliterate an Application, but that ability does not exist at the time of this writing.)

### Delete Application: Request URI

Only an authenticated Admin User can delete and restore Applications. 
To delete an application, you send an authenticated HTTP **DELETE** request to the /management end-point.
The Request URI must specify the Organization and the Application, both by identifier (name or UUID).
Here is the Request URI pattern:

	/management/organizations|orgs/{org_name}|{uuid}/applications|apps/{app_name}
    
Parameters

This is intentionally redundant, but you must confirm that you really want to delete the Application
by specifying its name or UUID, same as that which you used in the Request URI.

Parameter	                     Description
---------                      -----------
string confirm_application_id  Application identifier (either name or UUID)

Note: You also need to provide a valid access token with the API call. See [Authenticating users and application clients](../security_and_auth/authenticating-users-and-application-clients.html) for details.


### Delete Application: Example - Request

This  example deletes an aApplication named 'testapp1'

    curl -X DELETE "https://api.usergrid.com/management/orgs/testorg/apps/testapp1?confirm_application_id=testapp1"
    
### Delete Application: Example - Response

The response echos back the action that was taken and the params, and an HTTP 200 OK status message confirms that the Application has been deleted.

    HTTP/1.1 200 OK
    Access-Control-Allow-Origin: *
	Content-Length: 276
	Content-Type: application/json
	Date: Mon, 06 Jun 2016 18:52:04 GMT
	Server: Apache-Coyote/1.1
	Set-Cookie: rememberMe=deleteMe; Path=/; Max-Age=0; Expires=Sun, 05-Jun-2016 18:52:04 GMT
	{
	    "action": "delete",
	    "application": "d44dfc30-2c13-11e6-8b07-0a669fe1d66e",
	    "applicationName": "delete",
	    "duration": 3,
	    "organization": "test-organization",
	    "params": {
	        "confirm_application_identifier": [
	            "testapp1"
	        ]
	    },
	    "timestamp": 1465239124645
	}

    
### Restore Application: Request URI

To Restore an Application that has been deleted you must know the Application's UUID. If you do a PUT to that application's old URI, using he UUID to identify it, then the Application will be restored.
 
### Restore Application: Example - Request

For example, to restore 'testapp1' that we deleted above:

	curl -X PUT "https://api.usergrid.com/management/orgs/test-organization/apps/d44dfc30-2c13-11e6-8b07-0a669fe1d66e access_token==YWMtZR..."
	
### Restore Application: Example - Response

Here's the response that indicates via HTTP 200 OK that the Application has been restored.
	
	HTTP/1.1 200 OK
	Access-Control-Allow-Origin: *
	Content-Length: 223
	Content-Type: application/json
	Date: Mon, 06 Jun 2016 19:03:16 GMT
	Server: Apache-Coyote/1.1
	Set-Cookie: rememberMe=deleteMe; Path=/; Max-Age=0; Expires=Sun, 05-Jun-2016 19:03:16 GMT
	
	{
	    "action": "restore",
	    "application": "d44dfc30-2c13-11e6-8b07-0a669fe1d66e",
	    "applicationName": "delete",
	    "duration": 3,
	    "organization": "test-organization",
	    "params": {},
	    "timestamp": 1465239796913
	}
    
### Application Delete and Restore Limitations

At the time of this writing there are a couple of limitations regarding Application Delete and Restore:

* Within an Organization, you cannot delete an Application with the same name as an Application that you have deleted before.
* Within an Organization, you cannot restore an Application is an application with the very same name has been added since the orginal one was deleted.

Hopefully, these unnecessary limitations will be fixed soon; they are tracked by this JIRA issue [USERGRID-1299](https://issues.apache.org/jira/browse/USERGRID-1299)




    
    