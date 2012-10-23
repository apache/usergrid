##Overview
Apigee provides an SDK that simplifies writing HTML5 (that is, JavaScript) applications that connect to App Services. The Apigee App Services Javascript SDK  is available as an open-source project in github and we welcome your contributions and suggestions. The repository is located at:

<https://github.com/apigee/usergrid-javascript-sdk>

To find out more about Apigee App Services, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/usergrid/>

##Getting started
The SDK consists of one JavaScript file, located in the project at:

	/sdk/usergrid.appSDK.js

Include this file at the top of your HTML file (in between the head tags):

	<script src="sdk/usergrid.appSDK.js" type="text/javascript"></script>

After you do this, you're ready to start building entities and collections to drive your app and model your data.

A minified version of the file is located here:

	/sdk/usergrid.appSDK.min.js

# Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-stack), the Usergrid Javascript SDK is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

##Sample apps
The SDK project includes two simple apps.  

The first is a simple app called Dogs that creates a list of dogs.   The app uses App Services to retrieve a collection of dog entities. The app illustrates how to page through the results, and how to create a new entity.

The second is an app that exercises the 4 REST methods of the api: GET, POST, PUT, and DELETE.  These two apps provide different functionality and will help you learn how to use the Javascript SDK to make your own amazing apps!

For a more complex sample app, check out the Messagee app:

<https://github.com/apigee/usergrid-sample-html5-messagee>

##Entities and Collections
Entities and Collections are used to model the custom data you need to store in your app.  To enable you to use these in your app, the Javascript SDK provides the Entity and the Collection objects. The following sections describe how to create and use these objects and show examples from the Dogs sample app.

##The Entity Object
Start by creating a new Entity object, where the argument is the name of the collection that the entity will be part of. In the Dogs sample app, here is how a new dogs entity is created in a collection named dogs:

	var dog = new Usergrid.Entity("dogs");

Next, add any needed custom fields. For example:

 	dog.set("name","Dino");
 	dog.set("owner","Fred");
 	dog.set("state","hungry");

After the object is complete, save it back to the API, for example:

  	dog.save();

When the entity is saved, the API gives it a UUID that uniquely identifies the entity in the database.  This UUID is stored in the Entity object and will be used for any future calls to the API.  This ensures that the correct entity is updated.  For example, the UUID is used if the object is updated and needs to be saved again:

	dog.set("state", "fed");
	dog.save(); //updates the same dog entity as before

Or, if the entity is changed in the database (perhaps by another user of your app), and needs to be refreshed:

	dog.fetch(); //will only work if the UUID for the entity is in the dog object

In this way, multiple clients can update the same object in the database.

If you need to get a property from the object, call the get() method and provide the property as an argument to the method, for example:

	var state = dog.get("state");

If you no longer need the object, call the destroy() method. This deletes the object from database. For example:

	dog.destroy(); //no real dogs were harmed!

Although the object is deleted from the database, it remains in your program.  Destroy it if needed by setting it to a null value, for example:

	dog = null; //no real dogs were harmed!

##The Collection Object
The Collection Object models the custom collections you create using the API.  Collections organize entities.  For example, you could create a collection called "dogs".  Then, you can add "dog" entities to it.

To get started, create a new Collection object, where the argument is the type of collection you intend to model. For example:

	var dogs = new Usergrid.Collection('dogs'); //makes a new 'dogs' collection object

If your collection already exists on the server, call the get() method to populate your new object with data from the server. For example:

	dogs.get();

By default, the dogs.get() method uses the API to retrieve the first 10 dog entities and loads them into the dogs Collection object. If you want to add a new entity to the collection, simply create it. For example:

	var dog = new Usergrid.Entity("dogs");
	dog.set("name","fido");

Then add it to the collection (and save it to the API):

	dog.addNewEntity(dog);

Note:  The addNewEntity() method adds the entity to the collection and *also* saves it to the API.  If you have already saved an entity, you can simply call the addEntity() method.

So this:

	var dog = new Usergrid.Entity("dogs");
	dog.save();
	dogs.addEntity(dog); //entity is added only

Is equivalent to this:

	var dog = new Usergrid.Entity("dogs");
	dogs.addNewEntity(dog); //entity is added and saved


###Displaying Results
After you populate your Collection object, you can display a list of all the entities currently stored in the Collection object. Here's how it's done in the Dogs app:

	//iterate through all the items in this "page" of data
	while(dogs.hasNextEntity()) {
		//get a reference to the dog
		var dog = dogs.getNextEntity();
		//display the dog in the list
		$('#mydoglist').append('<li>'+ dog.get('name') + '</li>');
	}

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


Finally, call the get() method to pull down results from the API.  In the success callback function, loop through the results, adding checks to determine if the previous or next buttons should be displayed. For example:


	dogs.get(
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
		function () { alert('error'); }
	);


Now, when the user clicks on either the #next-button or the #previous buttons, the click event will call the appropriate method in the collection. The Collection object stores the callback methods that are passed into the get() method, so the same success callback method will be called every time.


###Custom Queries
A custom query allows you to tell the API that you want your results filtered or altered in some way.  The following example specifies that the query results should be ordered by creation date:

	dogs.setQueryParams({'ql':'order by created DESC'});

If you also wanted to get more entities in the result set than the default 10, say 100, you can specify a query similar to the the following (the limit can be a maximum of 999):

	dogs.setQueryParams({'ql':'order by created DESC','limit':'100'});

There are many cases where expanding the result set is useful.  But be careful - the more results you get back in a single call, the longer it will take to transmit the data back to your app.

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

 	marty.save();

 If the user is modified:

 	marty.set("girlfriend","Jennifer");

 Just call save on the user:

	marty.save();

 To refresh the user's information in the database:

	marty.fetch();

 To get properties from the user object:

 	var city = marty.getField("city");

If you no longer need the object, call the delete() method and the object will be deleted from database:

 	marty.destroy();

Although the object is deleted from the database, it remains in your program.  Destroy it if needed by calling:

 	marty = null;

##Persistent Storage
A persistent storage (session) module has been added.  The file is located here:

	/sdk/usergrid.session.js

Include this file at the top of your HTML file - AFTER the SDK file:

	<!--INCLUDE SDK FIRST-->
	<script src="sdk/usergrid.appSDK.js" type="text/javascript"></script> 
	<!--INCLUDE session after-->
	<script src="sdk/usergrid.session.js" type="text/javascript"></script> 

That is all you have to do.  The session contains methods that will override the storage methods used by the SDK.  Instead of storing the token and currently logged in user in memory, they are now stored in "localstorage", a feature available in most modern browsers.

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

	Usergrid.ApiClient.logoutAppUser();

This destroys their token in the ApiClient singleton.


##Direct API calls to the Application and Management endpoints
Creating and managing Entity and Collection objects is sufficient for most purposes.  However, there are times when it is necessary to make a direct call to the API.  The following sections describe how to do this against the Application endpoint as well as the Management endpoint.

**Note:** This method is used in the All Calls sample app.

###The Query object
Calls to both the Application endpoint as well as the Management endpoint require a Query object. The Query object stores information about the API call you want to make.  To get started, create a new Query object, and pass in the relevant data.  In the following example, we simply want to query the list of users:

	queryObj = new Usergrid.Query(
		"GET",
		"users",
		null,
		null,
		function(results) {
			//do something with the results
		},
		function() {
			alert('Error, Unable to retrieve users.');
		}
	);

####Function Signature
The function signature for the Query Object is as follows:

	(method, resource, jsonObj, paramsObj, successCallback, failureCallback)

####method
POST, GET, PUT, or DELETE

####resource
The resource to access (e.g. "users")

####jsonObj
A JSON object that contains the payload of data - only applicable for POST and PUT operations.  In the following example, to JSON object contains update information for a dog  object:

	var jsonObj = {'name':'fido','color':'black','breed':'mutt'};

####paramsObj
A JSON object that contains the query data (for example, to modify the search parameters).  For example, to modify the number of results and order the results descending, use an object like this:

	var paramsObj = {'ql':'order by created DESC','limit':'100'};

####successCallback
The success callback function - will be invoked when the API call is successfully completed.

####failureCallback
The failure callback function - will be invoked if the API call fails.


###Application endpoint
After the query object is ready, pass it as an argument to the appropriate endpoint. To run a query against the Application endpoint:

	Usergrid.ApiClient.runAppQuery(queryObj);

###Management endpoint
To run a query against the Management endpoint:

	Usergrid.ApiClient.runManagementQuery(queryObj);

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


 
