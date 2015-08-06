# Querying your data

This article describes how to use queries to filter data retrieved from your backend data store. Queries allow you to work with only the data you need, making your app more efficient and manageable by reducing the number of entities returned or acted on by the API. A query can be sent with any GET, PUT or DELETE request. For example, you might query the API to retrieve only the user entities with the property status:'active' to get a list of your users that have active accounts.

For information on more advanced query usage and syntax, see [Query parameters & clauses](query-parameters.html).

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
Query examples in this content are shown unencoded to make them easier to read. Keep in mind that you might need to encode query strings if you're sending them as part of URLs, such as when you're executing them with the cURL tool.
</p></div>

## Basic query usage

The following examples show how to query the Usergrid API to return the first 5 entities in the users collection that contain the property status:'active'.

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
Optimizing queries: As a best practice, you should include no more than 3 parameters in your queries. The API will not prevent you from submitting a query with more than 3 parameters; however, due to the nature of NoSQL, queries with many parameters can quickly become very inefficient.
</p></div>

For more information, see our [Usergrid DBMS overview](../data-store/data-storage-dbms.html) and [Data store best practices](../data-storage/optimizing-access).

### Request Syntax

	https://api.usergrid.com/<org>/<app>/<collection>?ql=<query_statement>

Note: Any values specified in the query statement should be enclosed in single-quotes.

	https://api.usergrid.com/your-org/your-app/users?limit=5&ql=select * where status = 'active'
	
Alternatively, when you use a statement that starts select * where you can omit the first part of the statement and abbreviate it this way:

	https://api.usergrid.com/your-org/your-app/users?limit=5&ql=status = 'active'
	
### Retrieving values for multiple properties

Your query can return multiple kinds of values -- such as the values of multiple properties -- by specifying the property names in your select statement as a comma-separated list.

For example, the following request returns the address and phone number of users whose name is Gladys Kravitz:

	/users?ql=select address,phone_number where name = 'Gladys Kravitz'
	
### Response syntax

When you query your data, the API response is formatted in JavaScript Object Notation (JSON). This is a common format used for parameter and return values in REST web services.

Data corresponding to the response is captured in the response’s entities array. The array will include one JSON-object for each entity returned for the query. Each returned entity will include a number of default properties, including the UUID of the entity, the entity type, and values for properties such as name, username, email, and so on. For a complete list of default properties by entity type, see [Default Data Entities](../rest-endpoints/api-doc.html#models).

For example, the following query for all entities of type user where the name property equals 'Gladys Kravitz':

	/users?ql=select * where name = ‘Gladys Kravitz’

will return the following response:

	{
	  "action" : "get",
	  "application" : "8272c9b0-d86a-11e2-92e2-cdf1ce04c1c0",
	  "params" : {
	    "ql" : [ "select * where name = 'Gladys Kravitz'" ]
	  },
	  "path" : "/users",
	  "uri" : "http://api.usergrid.com/myorg/myapp/users",
	  "entities" : [ {
	    "uuid" : "d0d7d0ba-e97b-11e2-8cef-411c466c4f2c",
	    "type" : "user",
	    "name" : "Gladys Kravitz",
	    "created" : 1373472876859,
	    "modified" : 1373472876859,
	    "username" : "gladys",
	    "email" : "gladys@example.com",
	    "activated" : true,
	    "picture" : "http://www.gravatar.com/avatar/20c57d4f41cf51f2db44165eb058b3b2",
	    "metadata" : {
	      "path" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c",
	      "sets" : {
	        "rolenames" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/rolenames",
	        "permissions" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/permissions"
	      },
	      "connections" : {
	        "firstname" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/firstname",
	        "lastname" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/lastname"
	      },
	      "collections" : {
	        "activities" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/activities",
	        "users" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/users",
	        "feed" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/feed",
	        "groups" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/groups",
	        "roles" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/roles",
	        "following" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/following",
	        "followers" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/followers"
	      }
	    }
	  } ],
	  "timestamp" : 1374694196061,
	  "duration" : 48,
	  "organization" : "myorg",
	  "applicationName" : "myapp",
	  "count" : 1
	}
	
Compare the preceding example with the following for another kind of query. Imagine the following request string, where the query string is asking for only the values of two of the entity’s properties (username and name):

	/users?ql=select username,name where name=’Gladys Kravitz’
	
In the response JSON from this query, the return value is specified as the property of the list item -- here, an array containing only the values of the properties the query asked for, in the order they were requested (username first, then name).

	{
	  "action" : "get",
	  "application" : "8272c9b0-d86a-11e2-92e2-cdf1ce04c1c0",
	  "params" : {
	    "ql" : [ "select username,name where name='Gladys Kravitz'" ]
	  },
	  "path" : "/users",
	  "uri" : "http://api.usergrid.com/myorg/myapp/users",
	  "list" : [ [ "gladys", "Gladys Kravitz" ] ],
	  "timestamp" : 1374697463190,
	  "duration" : 25,
	  "organization" : "myorg",
	  "applicationName" : "myapp",
	  "count" : 1
	}
	
	