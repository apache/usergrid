##Version

Current version: **0.9.10**

See change log: 

<https://github.com/apigee/usergrid-javascript-sdk/blob/master/changelog.md>

##Overview
This open source SDK simplifies writing JavaScript / HTML5 applications that connect to App Services. The repo is locatedhere:

<https://github.com/apigee/usergrid-javascript-sdk>

You can download the SDK here:

* Download as a zip file: <https://github.com/apigee/usergrid-javascript-sdk/archive/master.zip>
* Download as a tar.gz file: <https://github.com/apigee/usergrid-javascript-sdk/archive/master.tar.gz>


To find out more about Apigee App Services, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/app_services>



##Getting started
The SDK consists of one JavaScript file, located in the root of the project:

	usergrid.SDK.js

Include this file at the top of your HTML file (in between the head tags):

	<script src="usergrid.SDK.js" type="text/javascript"></script>

Next, specify the Org name and App name you want to use.  You can find this information in the [Admin Portal](http://apigee.com/usergrid). By default, every Org comes with a test App called "Sandbox":

	Usergrid.ApiClient.init('Apigee', 'Sandbox'); //<=put your info here ('orgname', 'appname');
	
You are now ready to make calls against the API.  The simplest way is to use the following format:

	var method = "POST";
	var path = "users";
	var data = {"username":"myuser", "password":"mypass"};
	var params = null;
	var query = new Usergrid.Query(method, path, data, params,
		function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});
	Usergrid.ApiClient.runAppQuery (query);

The preceding example will get create a new user in the users collection.  To see all the users in the database, use this format:

	var method = "GET";
	var path = "users";
	var data = null;
	var params = null;
	var query = new Usergrid.Query(method, path, data, params,
		function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		};
	Usergrid.ApiClient.runAppQuery (query);

You can also add query parameters to help narrow your search results.  For example, to get all the dogs from the dogs collection that have a color of brown:

	var method = "GET";
	var path = "dogs";
	var data = null;
	var params = {"ql":"select * where color='brown' order by created DESC"};
	var query = new Usergrid.Query(method, path, data, params,
		function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});
	Usergrid.ApiClient.runAppQuery (query);

Note: See the Custom queries section below for more information.

The preceding patterns can be used for all four query types (GET, POST, PUT, DELETE).  Take a look at the example app included with this SDK for more comprehensive examples.

##Sample apps
This SDK project includes an example app that exercises the 4 REST methods of the api: GET, POST, PUT, and DELETE, plus a login example.  This app will help you learn how to use the Javascript SDK to get up and running quickly.  It is located in the example directory:

	example/example.html
	
The javascript that powers the app is located in the app.js file:

	example/app.js
	
You can see the sample running live here:

<http://apigee.github.com/usergrid-javascript-sdk/example/example.html>

For a more complex Javascript/HTML5 sample app that also uses this Javascript SDK, check out Messagee:

<https://github.com/apigee/usergrid-sample-html5-messagee>

##Entities and Collections
This SDK also provides object abstraction, in the form of the Entity and Collection extensions, to make interfacing with the API easier. 

##The Entity Object
The Entity object models entities stored in the database. It offers an easy-to-use wrapper for creating, reading, updating, and deleting entities.

Start by creating a new Entity object, where the argument is the name of the collection that the entity will be part of. In the Dogs sample app, here is how a new dogs entity is created in a collection named dogs:

	var dog = new Usergrid.Entity("dogs");

Next, add any needed custom fields. For example:

 	dog.set("name","Dino");
 	dog.set("master","Fred");
 	dog.set("state","hungry");

After the object is complete, save it back to the API, for example:

  	dog.save( function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});

When the entity is saved, the API gives it a UUID that uniquely identifies the entity in the database.  This UUID is stored in the Entity object and will be used for any future calls to the API.  This ensures that the correct entity is updated.  For example, the UUID is used if the object is updated and needs to be saved again:

	dog.set("state", "fed");
	dog.save(function(response) { //updates the same dog entity as before
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		}); 

Or, if the entity is changed in the database (perhaps by another user of your app), and needs to be refreshed:

	dog.fetch( function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		}); //will only work if the UUID for the entity is in the dog object

In this way, multiple clients can update the same object in the database.

If you need to get a property from the object, call the get() method and provide the property as an argument to the method, for example:

	var state = dog.get("state");

If you want to get the entire data object (all properties), use the get method, but pass no arguments:

  var data = dog.get();

If you no longer need the object, call the destroy() method. This deletes the object from database. For example:

	dog.destroy( function(response) { //no real dogs were harmed! 
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		}); 

Although the object is deleted from the database, it remains in your program.  Destroy it if needed by setting it to a null value, for example:

	dog = null; //no real dogs were harmed!

##The Collection Object
The Collection Object models the custom collections you create using the API.  Collections organize entities.  For example, you could create a collection called "dogs".  Then, you can add "dog" entities to it.

To get started, create a new Collection object, where the argument is the type of collection you intend to model. For example:

	var dogs = new Usergrid.Collection('dogs'); //makes a new 'dogs' collection object

If your collection already exists on the server, call the fetch() method to populate your new object with data from the server. For example:

	dogs.fetch( function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});

By default, the dogs.fetch() method uses the API to retrieve the first 10 dog entities and loads them into the dogs Collection object. If you want to add a new entity to the collection, simply create it. For example:

	var dog = new Usergrid.Entity("dogs");
	dog.set("name","fido");

Then add it to the collection (and save it to the API):

	dog.addNewEntity(dog);

Note:  The addNewEntity() method adds the entity to the collection and *also* saves it to the API.  If you have already saved an entity, you can simply call the addEntity() method.

So this:

	var dog = new Usergrid.Entity("dogs");
	dogs.addEntity(dog); //entity is added only
	dog.save( function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});

Is equivalent to this:

	var dog = new Usergrid.Entity("dogs");
	dogs.addNewEntity(dog, function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		}); //entity is added and saved


###Displaying Results
After you populate your Collection object, you can display a list of all the entities currently stored in the Collection object. Here's how it's done in the Dogs app:

	//populate the collection
	dogs.fetch( function(response) {
			//iterate through all the items in this "page" of data
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				var dog = dogs.getNextEntity();
				//display the dog in the list
				$('#mydoglist').append('<li>'+ dog.get('name') + '</li>');
			}
		},
		function (response) {
			//oops!  call didn't work
		});
	

Note: This code snippet only loops through the items currently stored in the Collection object.  If there are more entities in the database that you want to display, either use paging, or a custom query.

###Collection Paging
As your collections grow larger, you may want to use paging to display smaller blocks of data for the user, with "next" and "previous" buttons. The Collection object provides this functionality and it can be seen in the Dogs sample app included with the SDK.

To get started, create a new Collection object. For example:

	var dogs = new Usergrid.Collection('dogs');

Next, bind the previous and next buttons to the getNextPage and getPreviousPage buttons in the new Collection object. For example:

	//bind the next button to the proper method in the collection object
	$('#next-button').bind('click', function() {
		dogs.getNextPage();
	});

	//bind the previous button to the proper method in the collection object
	$('#previous-button').bind('click', function() {
		dogs.getPreviousPage();
	});


Finally, call the fetch() method to pull down results from the API.  In the success callback function, loop through the results, adding checks to determine if the previous or next buttons should be displayed. For example:


	dogs.fetch(
		function() {
			//first empty out all the current dogs in the list
			$('#mydoglist').empty();
			//then hide the next / previous buttons
			$('#next-button').hide();
			$('#previous-button').hide();
			//iterate through all the items in this "page" of data
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				var dog = dogs.getNextEntity();
				//display the dog in the list
				$('#mydoglist').append('<li>'+ dog.get('name') + '</li>');
			}
			//if there is more data, display a "next" button
			if (dogs.hasNextPage()) {
				//show the button
				$('#next-button').show();
			}
			//if there are previous pages, show a "previous" button
			if (dogs.hasPreviousPage()) {
				//show the button
				$('#previous-button').show();
			}
		},
		function () { 
			//oops! call didn't work 
		}
	);


Now, when the user clicks on either the #next-button or the #previous buttons, the click event will call the appropriate method in the collection. The Collection object stores the callback methods that are passed into the get() method, so the same success callback method will be called every time.


###Custom Queries
The system supports custom queries similar to those used in traditional SQL.  This format allows you to specify filters based on many fields and also allows you to order the results as needed. Add a params object to any entity prior to doing a fetch.  For example, to search for dogs that have a color of brown, do this:

	var params = {"ql":"select * where color='brown'"};
	dogs.setQueryParams(params);

There may be a lot of brown dogs, so you can also order them by the entity creation date:

	var params = {"ql":"select * where color='brown' order by created DESC"};
	dogs.setQueryParams(params);

By default, the system will return 10 entities in a result set, even if there are more that match the query. You can specify a larger (or smaller) number, up to a maximum of 999. The limit parameter needs to be separate from the query.  So continuing with the example above, there may be many brown dogs, and we want to get a maximum of 100:

	var params = {"ql":"select * where color='brown' order by created DESC", "limit":"100"}
	dogs.setQueryParams(params);
	
You may also want to just get 100 entities, but you don't want to filter by any fields:

	var params = {"limit":"100"};
	dogs.setQueryParams(params);
	
Or, to get all dogs, but make sure they are ordered:

	var params = {"ql":"order by created DESC"}
	dogs.setQueryParams(params);

There are many cases where expanding the result set is useful.  But be careful: the more results you get back in a single call, the longer it will take to transmit the data back to your app.

You can find more information on custom queries here:

<http://apigee.com/docs/usergrid/content/queries-and-parameters>

##Modeling users with the Entity object

###Making a user object
There is no specific User object in the SDK.  Instead, you simply need to use the Entity object, specifying a type of "users".  Here are some examples:

 First, create a new user:

	var marty = new Entity("users");

 Next, add more data if needed:

 	marty.set("username", "marty");
 	marty.set("name", "Marty McFly");
 	marty.set("City", "Hill Valley");
 	marty.set("State", "California");

 Finally, save the user to the database:

 	marty.save(function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});

 If the user is modified:

 	marty.set("girlfriend","Jennifer");

 Just call save on the user:

	marty.save(function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});

 To refresh the user's information in the database:

	marty.fetch(function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});

 To get properties from the user object:

 	var city = marty.getField("city");

If you no longer need the object, call the delete() method and the object will be deleted from database:

 	marty.destroy(function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});

Although the object is deleted from the database, it remains in your program.  Destroy it if needed by calling:

 	marty = null;

##Extensions

###cURL
[cURL](http://curl.haxx.se/) is an excellent way to make calls directly against the API.  If you would like to see all the cURL equivalent of all the calls your app is making, you can include the cURL extension located here:

	/extensions/usergrid.curl.js

Include this file at the top of your HTML file - AFTER the SDK file:

	<script src="sdk/usergrid.SDK.js" type="text/javascript"></script> 
	<script src="extensions/usergrid.curl.js" type="text/javascript"></script> 

Now, when you run your app in a browser, the cURL calls will be generated and logged to the console.  For more information about how to see these calls in the console of your browser, see this article in the Apigee docs:

<http://apigee.com/docs/usergrid/content/displaying-app-services-api-calls-curl-commands>

More information on cURL can be found here:

<http://curl.haxx.se/>

###Entity and Collection Objects
The Entity and Collection objects, discussed earlier in this file, are available in the extensions directory:

	/extensions/usergrid.entity-collections.js
	
Include this file at the top of your HTML file - AFTER the SDK file:

	<script src="usergrid.SDK.js" type="text/javascript"></script>
	<script src="extensions/usergrid.entity-collection.js" type="text/javascript"></script>


###Persistent Storage
A persistent storage (session) extension has been added.  The file is located here:

	/extensions/usergrid.session.js

Include this file at the top of your HTML file - AFTER the SDK file:

	<script src="sdk/usergrid.SDK.js" type="text/javascript"></script> 
	<script src="extensions/usergrid.session.js" type="text/javascript"></script> 

That is all you have to do.  The session contains methods that will override the storage methods used by the SDK.  Instead of storing the token and currently logged in user in memory, they are now stored in "localstorage", a feature available in most modern browsers.

###Validation
An extension for validation is also provided.  The file is located here:

	/extensions/usergrid.session.js

Include this file at the top of your HTML file - AFTER the SDK file:

	<script src="sdk/usergrid.SDK.js" type="text/javascript"></script> 
	<script src="extensions/usergrid.validation.js" type="text/javascript"></script> 

A variety of functions are provided for verification of many common types such as usernames, passwords, etc.  Feel free to copy and modify these functions for use in your own projects.

##App user log in / log out

###To log a user in
To log app users in, use the Usergrid.ApiClient.logInAppUser() method.  This method takes the supplied username and password and attempts to acquire an access token from the API.  If the method successfully acquires the token, the token is stored in the Usergrid.ApiClient singleton and will be used for all subsequent calls. You can see an example of using the Usergrid.ApiClent.logInAppUser() method in the Messagee Sample app, which you can find here:

<https://github.com/apigee/usergrid-sample-html5-messagee>

To get started, build a login form:

	<form name="form-login" id="form-login">
		<label for="username">Username</label>
		<input type="text" name="username" id="username" class="span4" />
		<label for="password">Password</label>
		<input type="password" name="password" id="password" class="span4" />
	</form>
	<div>
		<a href="#login" id="btn-login" data-role="button">Login</a>
	</div>

Next, bind a click event to the login button:

	$('#btn-login').bind('click', function() {
		login();
	});

Finally, create the login method:

	function login() {
		$('#login-section-error').html('');
		var username = $("#username").val();
		var password = $("#password").val();
		Usergrid.ApiClient.logInAppUser(username, password,
			function (response, user) {
				//login succeeded, so get a reference to the newly create user
				appUser = Usergrid.ApiClient.getLoggedInUser();

				//take more action here
			},
			function () {
				$('#login-section-error').html('There was an error.');
			}
		);
	}

After the user is successfully logged in, you can make calls to the API on their behalf.  Their access token will be stored and used for all future calls.


###To log a user out
To log the user out, call:

	Usergrid.ApiClient.logoutAppUser( function(response) {
			//call was good, do something with the response
		},
		function (response) {
			//oops!  call didn't work
		});

This destroys their token in the ApiClient singleton.


##Application endpoint
In the first part of this file, we showed you how to make calls against the application endpoint using the Query object and the runAppQuery method:

	Usergrid.ApiClient.runAppQuery(query);

##Management endpoint
There is an equivalent endpoint that can be used to run queries against the Management endpoint:

	Usergrid.ApiClient.runManagementQuery(query);
	


##More information
For more information on Apigee App Services, visit <http://apigee.com/about/developers>.

## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-stack), the Usergrid Javascript SDK is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)


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


 
