##Version
Current Version: **0.10.0**

See change log:

https://github.com/apigee/usergrid-node-module/master/changelog.md

##Overview
This Node.js module, which simplifies the process of making API calls to App Services from within Node.js, is provided by [Apigee](http://apigee.com) and is available as an open-source project on github.  We welcome your contributions and suggestions. The repository is located here:

<https://github.com/apigee/usergrid-node-module>

To find out more about Apigee App Services, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/usergrid/>

 
##Installing
Use npm:

	$ npm install usergrid



##Getting started
Include the module:

	var usergrid = require('usergrid');

Then create a new client:

	var client = new usergrid.client(
		{ 
		  orgName:"yourorgname"
		, appName:"sandbox"
  		} 
	);

The preceding example shows how to use the "Sandbox" testing app, which does not require any authentication.  The "Sandbox" comes with all new App Services accounts.  

If you are ready to use authentication, then create your client this way:

	var client = new usergrid.client(
		{ 
		orgName:"yourorgname"
		, appName:"yourappname"
		, authType:"CLIENT_ID"
  		, clientId:"b3U6y6hRJufDEeGW9hIxOwbREg"
  		, clientSecret:"b3U6X__fN2l9vd1HVi1kM9nJvgc-h5k"
  		} 
	);
	
**Note:** you can find your client secret and client id on the "Properties" page of the [Admin Portal](http://apigee.com/usergrid).

You are now ready to use the usergrid handle to make calls against the API.  

##Make some calls
This module uses the [request](https://github.com/mikeal/request) module by [mikeal](https://github.com/mikeal/request).  The request uses similar syntax although only the subset of options that is relevant to making calls against the App Services API.  To make basic calls against the API, use the following format:

	request(options, callback);
	
For example, to get a list of users:

	var options = {method:"GET", endpoint:"users"};
	client.request(options, function (err, data) {
		if (err) {
			console.log('error');
		} else {
			//data will contain raw results from API call
		}		
	});

Or, to create a new user:

	var options = {method:"POST", endpoint:"users", body:{username:"fred", password:"secret"}};
	client.request(options, function (err, data) {
		if (err) {
			console.log('error');
		} else {
			//data will contain raw results from API call
		}		
	});

Or, to update the new user:

	var options = {method:"PUT", endpoint:"users/fred", body:{newkey:"newvalue"};
	client.request(options, function (err, data) {
		if (err) {
			console.log('error');
		} else {
			//data will contain raw results from API call
		}		
	});

Or to delete the new user:

	var options = {method:"DELETE", endpoint:"users/fred"};
	client.request(options, function (err, data) {
		if (err) {
			console.log('error');
		} else {
			//data will contain raw results from API call
		}		
	});


The Options Object:

* `method` - http method (GET, POST, PUT, or DELETE), defaults to GET
* `qs` - object containing querystring values to be appended to the uri
* `body` - object containing entity body for POST and PUT requests
* `endpoint` - API endpoint, for example "users/fred"
* `mQuery` - boolean, set to true if running management query, defaults to false
* `buildCurl` - boolean, set to true if you want to see equivalent curl commands in console.log, defaults to false

You can make any call to the API using the format above.  However, in practice using the higher level Entity and Collection objects will make life much easier as they take care of much of the heavy lifting.


##Entities and Collections
Usergrid stores its data as "Entities" in "Collections".  Entities are essentially JSON objects and Collections are just containers for storing these objects. To access Entities and Collections, start by creating a new Collection.  In this case, we are creating a collection of dogs:

	var options = {
		type:"dogs"
		, client:client
	}
	var dogs = new usergrid.collection(options, function(err) {	
		if (err) { 
			console.log('error'); 
		} else {
		
			//we got the dogs, now display the Entities:
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				var dog = dogs.getNextEntity();
				var name = dog.get('name');
				console.log('dog is called ' + name);
			}
			  
			//do more things with the collection  
		}
	});   

**note:** all calls to the API will be executed asynchronously, so it is important that you add code to operate on the collection in the callback.

You can also create individual entities:

	var dog = new usergrid.Entity("dogs");

Next, add any needed custom fields. For example:

	dog.set("name","Dino");
	dog.set("master","Fred");
	dog.set("state","hungry");

After the object is complete, you save it back to the API, for example:

	dog.save();
	
Or, you can add it to a collection. This will also save the entity back to the database:

	dogs.addEntity(dog); //dog added to dogs collection

When the entity is saved, the API gives it a UUID that uniquely identifies the entity in the database.  This UUID is stored in the Entity object and will be used for any future calls to the API.  This ensures that the correct entity is updated.  For example, the UUID is used if the object is updated and needs to be saved again:

	dog.set("state", "fed");
	dog.save(); //updates the same dog entity as before

Or, if the entity has changed in the database (perhaps by another user of your app), and needs to be refreshed:

	dog.fetch(); //will only work if the UUID for the entity is in the dog object

In this way, multiple clients can update the same object in the database.

If you need to get a property from the object, call the get() method and provide the property as an argument to the method, for example:

	var state = dog.get("state");

If you no longer need the object, call the destroy() method. This deletes the object from database. For example:

	dog.destroy(); //no real dogs were harmed!

Although the object is deleted from the database, it remains in your program.  Destroy it if needed by setting it to a null value, for example:

	dog = null; //no real dogs were harmed!


##Collection iteration and paging
The Collection object works in Pages of data.  This means that at any given time, the Collection object will have one page of data loaded.  You can iterate across all the entities in the current page of data by using the following pattern:

	//we got the dogs, now display the Entities:
	while(dogs.hasNextEntity()) {
		//get a reference to the dog
		var dog = dogs.getNextEntity();
		var name = dog.get('name');
		console.log('dog is called ' + name);
	}
	
To get the next page of data from the server, use the following methods:
	
	if (dogs.hasNextPage()) {
		dogs.getNextPage(function(err){
			if(!err) {
				//we got the next page of data, so do something with it:
				while(dogs.hasNextEntity()) {
					//get a reference to the dog
					var dog = dogs.getNextEntity();
					var name = dog.get('name');
					console.log('dog is called ' + name);
				}
			}
		}	
	}

You can use the same pattern to get a previous page of data:

	if (dogs.hasPreviousPage()) {
		dogs.getPreviousPage(function(err){
			if(!err) {
				//we got the previous page of data, so do something with it:
				while(dogs.hasNextEntity()) {
					//get a reference to the dog
					var dog = dogs.getNextEntity();
					var name = dog.get('name');
					console.log('dog is called ' + name);
				}
			}
		}	
	}

Several other convenience methods exist to make working with pages of data easier:

* getFirstEntity - gets the first entity of a page
* getLastEntity - gets the last entity of a page
* resetEntityPointer - sets the internal pointer back to the first element of the page
* getEntityByUUID - returns the entity if it is in the current page

By default, the database will return 10 entities per page.  You can change that amount by using the limit statement.  In the following example, the server will return 50 entities per page instead of 10:

	
	var options = {
		type:"dogs"
		, client:client
		, qs={limit:50} //limit statement set to 50
	}
	var dogs = new usergrid.collection(options, function(err) {	
		if (err) { 
			console.log('error'); 
		} else {
		
			//we got the dogs, now display the Entities:
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				var dog = dogs.getNextEntity();
				var name = dog.get('name');
				console.log('dog is called ' + name);
			}
			  
			//do more things with the collection  
		}
	});   




###Custom Queries
A custom query allows you to tell the API that you want your results filtered or altered in some way.  To specify that the query results should be ordered by creation date, use the following syntax:

	var options = {
		type:"dogs"
		, client:client
		, qs={'ql':'order by created DESC'}
	}

If you also wanted to get more entities in the result set than the default 10, say 100, you can specify a query similar to the following (the limit can be a maximum of 999):

	var options = {
		type:"dogs"
		, client:client
		, qs={'ql':'order by created DESC','limit':'100'}
	}

**Note**: there are many cases where expanding the result set is useful.  But be careful - the more results you get back in a single call, the longer it will take to transmit the data back to your app.

Another common requirement is to limit the results to a specific query.  For example, to get all brown dogs, use the following syntax:
	
	var options = {
		type:"dogs"
		, client:client
		, qs={"ql":"select * where color='brown'"}
	}

You can also limit the results returned such that only the fields you specify are returned:

	var options = {
		type:"dogs"
		, client:client
		, qs={"ql":"select name, age where color='brown'"}
	}

You can find more information on custom queries here:

<http://apigee.com/docs/usergrid/content/queries-and-parameters>


##Modeling users with the Entity object
There is no specific User object in the module.  Instead, you simply need to use the Entity object, specifying a type of "users".  Here are some examples:

First, create a new user:

	var marty = new usergrid.Entity("users");

 Next, add more data if needed:

	marty.set("username", "marty");
	marty.set("name", "Marty McFly");
	marty.set("City", "Hill Valley");
	marty.set("State", "California");

Finally, save the user to the database:

	marty.save(function(err){
		if (!err){
			//marty is saved
		}
	});

If the user is modified:

	marty.set("girlfriend","Jennifer");

Just call save on the user:

	marty.save(function(err){
		if (!err){
			//marty is saved
		}
	});

To refresh the user's information in the database:

	marty.fetch(function(err){
		if (!err){
			//marty is refreshed
		}
	});

To get properties from the user object:

	var city = marty.get("city");

If you no longer need the object, call the delete() method and the object will be deleted from database:

	marty.destroy(function(err){
		if (!err){
			//marty is gone
		}
	}););

Although the object is deleted from the database, it remains in your program.  Destroy it if needed by calling:

	marty = null;


###To log a user in
Up to this point, we have shown how you can use the client secret / client id combination to authenticate your calls against the API.  For a server-side Node.js app, this may be all you need.  However, if you do find that your app requires that you authenticate an individual user, this section shows you how.

Logging a user in means sending the user's username and password to the server, and getting back an access (OAuth) token.  You can then use this token to make calls to the API on the User's behalf.

To log app users in, use the logInAppUser() method:

	client.logInAppUser(username, password,
		function (err) {
			if (!err) {
				//token has been automatically saved by the client 
				// object and can be used for the next call if
				// the authType has been set to 'APP_USER' (see below)
				
				//or, to get the user's token:
				var token = client.getToken();
				
				
				//to get the currently logged in user:
				var user = client.user;
				//to get their username: 
				var username = client.user.get('username');      
			}
		}
	);


After the user is successfully logged in, their access token will be stored in the client object and can used for future calls. To do this, first set the authorization type:

	client.authType = usergrid.APP_USER;

After this statement is called, any future calls will attempt to use the user token instead of the client secret / id combo (application level).  If you need to make an application level call using the secret/id combo, simply enable that type of authentication instead:

	client.authType = usergrid.AUTH_CLIENT_ID;

In contrast, to use no authentication, for example, if you are using the default Sandbox app that was automatically created when your account was set up, disable auth:

	client.authType = usergrid.AUTH_NONE;

With this setting enabled, no authentication will be provided to the database. You will likely only ever use this setting if you are testing with the Sandbox app.

Another way to approach the two types of calls would be to pull the token out of the existing client object and make a new client object just for the app user calls:

	//first get the token from the original client (after the user is logged in)
	var token = client.getToken();
	//then make a new client just for the app user
	var appUserClient = new usergrid.client(
	{ 
		orgName:"myorg"
		, appName:"myapp"
		, authType:"APP_USER"
		, token:token
  	});

Now, you can use the client object to make calls with the client secret / client id, and then use the appUserClient object to make calls on behalf of the user.  

To test if a user is logged in:

	client.isAppUserLoggedIn();


To recap, either use the same client object and change auth types before each call, or, make a new client object for user calls.  Either method will work.


###To log a user out
To log the user out, call:

	client.logoutAppUser();
	
Or, if you made a new client object specifically for the app user:

	appUserClient.logoutAppUser();	

This destroys the token and user object in the client, effectively logging the user out.


##Session
You may find that you need to persist your data across page loads.  A Usergrid based session module for use with this one will be coming soon.



##Sample / Test app
After installing Node on your system, navigate to the directory where you put the code repo and run the following command to start the sample app:

	cd path/to/my/code

Then, make sure you navigate into the test directory:

	cd example

And run the command to start the server:

	$ node server.js

This will start the node server. If it was successful, you should see the following on the command line:

 	Server has started.
 	Server running at port 8888, try http://127.0.0.1:8888

If you do, you will then be able to enter the URL into a browser:

	http://127.0.0.1:8888/

This will bring up the All Calls app, which presents you with the option to run any of the standard calls, GET, POST, PUT, or DELETE, as well as make a sample login call.  Default values have been specified for the form fields under each call.

The best way to learn how the code works is to spend some time reviewing the sample project.  Node.js presents a wide array of options when deciding how to set up your application.  We have tried to make this example as simple and clear as possible. 

In the test directory, you will see the `index.js` file.  This is the main entry point of the application.  From there, calls go to the `server.js` file, and are then routed through the `router.js` file to the `controller.js` file, and finally the `view.js` file is called (whew!).  So the call order is like this:

	1. index.js
	2. server.js
	3. router.js
	4. controller.js
	5. view.js

The API calls are all triggered in the `controller.js` file, in the "main" function.  Depending on the querydata parameter, the appropriate function will be called.


## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-node-module), the Usergrid Node module is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

##More information
For more information on Apigee App Services, visit <http://apigee.com/about/developers>.

## Copyright
Copyright 2012 Apigee Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.