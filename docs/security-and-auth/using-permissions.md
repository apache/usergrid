# Using permissions

Permissions allow you to define user access to perform GET, POST, PUT, or DELETE operations on specific resources. When the user submits a request via your app code to the Usergrid API, the user’s permissions are checked against the resource paths that the user is trying to access. The request succeeds only if access to the resource is allowed by the permission rules you specify.

## Permissions syntax
In Usergrid, permissions are represented in the following format:

    <operations>:<resource_path>
    
* ``<operations>``: A comma-delimited set of HTTP methods (``GET``, ``PUT``, ``POST``, ``DELETE``) that are allowed for the specified resource path. For example, ``get``, ``post`` would allow only ``GET`` and ``POST`` requests to be made to the specified resource.
* ``<resource_path>``: The path to the resources to be accessed. For example, ``/users`` would apply the permission to the users collection, while ``/users/Tom`` would apply the permission to only the user entity with username 'Tom'.

## Complex paths
Complex paths can be defined using [Apache Ant pattern syntax](http://ant.apache.org/manual/dirtasks.html#patterns). The following special path variables are supported for the construction of complex paths:

<table>
<tr>
   <td>Parameter</td>
   <td>Description</td>
</tr>
<tr>
   <td>*</td>
   <td>Treated as a wildcard. Assigns the permission to all paths at the specified level in the path hierarchy. For example, ``/*`` would match any collection, while ``/users/Tom/*`` would match /users/Tom/likes and ``/users/Tom/owns``.</td>
</tr>
<tr>
   <td>**</td>
   <td>Assigns the permission to the path recursively. For example, ``**/likes`` would match ``/likes`` and ``/users/likes``, while ``/users/**`` would match ``/users`` and ``/users/likes``.</td>
</tr>
<tr>
   <td>\${user}</td>
   <td>Automatically sets the path segment to the UUID of the currently authenticated user. For example, if you sent a request with a valid access token for a user with UUID ``bd397ea1-a71c-3249-8a4c-62fd53c78ce7``, the path ``/users/${user}`` would be interpreted as ``/users/bd397ea1-a71c-3249-8a4c-62fd53c78ce7``, assigning the permission only to that user entity.</td>
</tr>
</table>

## Assigning permissions
Permissions can only be assigned to user, group or role entities. Assigning permissions to roles can be particularly useful, as it allows you to create sets of permissions that represent complex access definitions, which can then be assigned to user and group entities. For more on roles, see [Using Roles](security-and-auth/using-roles.html).
       
### Request syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/<collection>/<entity>/permissions -d '{"permission":<permissions>}'
    
Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
collection	The collection of the entity that the permissions are to be assigned to. 
entity	    The UUID of the entity to assign the permissions to. For users, username and for groups, name are also accepted.
permissions	The permissions to assign to the entity. See [Permissions syntax](security-and-auth/using-permissions.html#permissions-syntax) for format.

For collections, Valid values are users and groups.

### Example request
For example, the following cURL request would give the user 'Tom' POST permission to the /users collection:

    curl -X POST https://api.usergrid.com/your-org/your-app/users/Tom/permissions -d '{"permission":"post:/users"}'
    
### Example response
The newly assigned permission is returned in the data property of the response:

		{
		  "action" : "post",
		  "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
		  "params" : { },
		  "uri" : "https://api.usergrid.com/your-org/your-app",
		  "entities" : [ ],
		  "data" : [ "post:/users" ],
		  "timestamp" : 1402349612382,
		  "duration" : 19,
		  "organization" : "your-org",
		  "applicationName" : "your-app"
		}
		

## Removing permissions
Using a DELETE request, you can remove one of more permissions from a user, group, or role entity.

### Request syntax

    curl -X DELETE https://api.usergrid.com/<org>/<app>/<collection>/<entity>/permissions?=<permissions>
    
Parameters

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name
collection	The collection of the entity that the permissions are to be assigned to. Valid values are users and groups.
entity	    The UUID of the entity to assign the permissions to. For users, username and for groups, name are also accepted.
permissions	The permissions to assign to the entity. See [Permissions syntax](using-permissions.html) for format.


### Example request

    curl -X DELETE https://api.usergrid.com/your-org/your-app/users/Tom/permissions?permission=post:/users
    
### Example response
The deleted permission is returned in the params.permission property of the response:

    {
      "action" : "delete",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : {
        "permission" : [ "post:/users" ]
      },
      "uri" : "https://api.usergrid.com/your-org/your-app",
      "entities" : [ ],
      "data" : [ "post:/assets" ],
      "timestamp" : 1402349951530,
      "duration" : 20,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }		
	