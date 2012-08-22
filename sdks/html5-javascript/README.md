#Apigee App Services Javascript SDK

#Overview
The Apigee App Services Javascript SDK was designed to make developing Apigee App Services powered Javascript apps as easy as possible. This project is open source and we welcome your contributions and suggestions.  The repository is located on github:

<https://github.com/apigee/usergrid-javascript-sdk>

To find out more about Apigee App Services, please go here:

<http://usergrid.apigee.com>

Our docs site is located here:

<http://usergrid.apigee.com/docs>

#Getting started
The SDK consists of just one Javascript file.  It is located in this project here:

	/sdk/usergrid.appSDK.js 

Include this file at the top of your html file (in between the head tags):

	<script src="sdk/usergrid.appSDK.js" type="text/javascript"></script>

Once you have done this, you are ready to start building entities and collections to drive your app and model your data.  

A minified version of the file is located here:

	/sdk/usergrid.appSDK.min.js


#Sample apps
The SDK project includes a simple app called Dogs, that is simply a list of dogs.  It retrieves the collection from the API, shows you how to page through the results, and shows you how to create a new entity.  For a more complex example, check out our Messagee app:

<https://github.com/apigee/usergrid-sample-html5-messagee>

#Entities and Collections
Entities and Collections are used to model the custom data you need to store in your app.  To facilitate using these in your app, the Javascript SDK provides the Entity and the Collection objects. 

##The Entity Object
###Getting Started
To use, simply create a new Entity object, where the argument is the name of the collection that the entity will be a part of:

	var dog = new Usergrid.Entity("dogs");
	
Next add any custom fields needed:

 	dog("name","Dino");
 	dog("owner","Fred");
 	dog("state","hungry");

Once the object is complete, save it back to the API:
 
  	dog.save();
 
When the entity is saved, the API gives it a UUID that uniquely identifies the Entity in the database.  This UUID is stored in our Entity object and will be used for any future calls to the API.  This ensures that the correct entity is updated.  For example, if the object was updated and needed to be saved again:

	dog.set("state", "fed");
	dog.save(); //updates the same dog entity as before
 
Or, if the entity was changed in the database (perhaps by another user of your app), and needed to be refreshed:

	dog.fetch(); //will only work if we have a UUID
 
In this way, multiple clients can update the same object in the database.

If you need to get a property from the object, do this:
 
	var state = dog.get("state");
 
If you don't need the object anymore, simply call the destroy() method and it will be deleted from database:
 
	dog.destroy(); //no real dogs were harmed!
 
The object is now deleted from the database, although it remains in your program.  Destroy it if needed by calling:
 
	dog = null; //no real dogs were harmed!

##The Collection Object

###Getting Started
The Collection Object models the custom collections you create in the API.  Collections are used to organize entities.  For example, you could create a collection called "dogs".  Then, you can add "dog" entities to it.

To get started, simply create a new Collection object, where the argument is the type of collection you intend to model:

	var dogs = new Usergrid.Collection('dogs'); //makes a new 'cars' collection object

If your collection already exists on the server, you can simply call the get() method to populate your new object with data from the server:

	dogs.get();
	
By default, this will pull the first 10 entities from the API and will load them into the cars Collection object. If you want to add a new Entity to the collection, simply create it and add it to the collection:
	
	var dog = new Usergrid.Entity("dogs");
	dog.set("name","fido");

Then add it to the collection (and save it to the API):

	cars.addNewEntity(dog);
	
Note:  the addNewEntity method adds the entity to the collection and *also* saves it to the API.  If you have already saved an entity, you can simply call the addEntity method. 

So this:

	var dog = new Usergrid.Entity("dogs");
	dog.save();
	dogs.addEntity(dog); //entity is added only

Is equivalent to this:

	var dog = new Usergrid.Entity("dogs");
	dogs.addNewEntity(dog); //entity is added and saved


###Displaying Results
Once you have populated your Collection object, you can display a list of all the entities currently stored in the Collection object by using the following code snippet:
 
	//iterate through all the items in this "page" of data
	while(dogs.hasNextEntity()) {
		//get a reference to the dog
		var dog = dogs.getNextEntity();
		//display the dog in the list
		$('#mydoglist').append('<li>'+ dog.get('name') + '</li>');
	}

Remember, this snippet will only loop through the items currently stored in the Collection object.  If there are more entities in the database that you want to display, either use Paging, or a custom query. 

###Collection Paging
As your collections grow larger, you may want to use paging to display smaller blocks of data for the user, with "next" and "previous" buttons. The Collection object provides this functionality.  

To get started, create a new Collection object: 

	var dogs = new Usergrid.Collection('dogs'); 
	
Next, bind the previous and next buttons to the getNextPage and getPreviousPage buttons in the new Collection object.

	//bind the next button to the proper method in the collection object
	$('#next-button').bind('click', function() {
		dogs.getNextPage();
	});

	//bind the previous button to the proper method in the collection object
	$('#previous-button').bind('click', function() {
		dogs.getPreviousPage();
	});


Finally, you will call the get() method to pull down results from the server.  In the success callback function, you will loop through the results, adding checks to determine if the previous / next buttons should be displayed:

	
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
	
	
Now, when the user clicks on either the #next-button or the #previous buttons, the click event will call the appropriate method in the collection.  The Collection object stores the callback methods that are passed into the get() method, so the same success callback method will be called every time.  


###Custom Queries
A custom query allows you to tell the API you want your results filtered / altered in some way.  In the following example, we are specifying that we want the query ordered by the created date:

	dogs.setQueryParams({'ql':'order by created DESC'}); 
	
If we also wanted to get more entities in our result set than the default 10, in this case 100, you can use the following (up to a max of 999):

	dogs.setQueryParams({'ql':'order by created DESC','limit':'100'}); 
		
There are many cases where this is useful.  But be careful - the more results you get back in a single call, the longer it will take to transmit the data back to your app.	

More information on custom queries can be found in our API documentation, located here:

<http://apigee.com/docs>

##Modeling users with the Entity Object

###Making a user object
There is no specific User object in the SDK.  Instead, you simply need to use the Entity object, specifying a type of "users".  Sample usage follows:

 First create a new user: 
 
	var marty = new Entity("users"); 
 	
 Next add more data if needed:
 
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
 
 To refresh the user's info from the database:
  
	marty.get();
 
 To get properties from the user object:
 	
 	var city = marty.getField("city");
 
If you don't need the object anymore, simply call the destroy method and it will be deleted from database:
 
 	marty.delete();
 
the object is now deleted from the database, although it remains in your program.  Destroy it if needed by calling:
 
 	marty = null;
 
##App user log in / log out

###To log a user in
To log app users in, you will use the Usergrid.ApiClient.logInAppUser() method.  This method takes the supplied username and password and attempts to acquire an access token from the API.  If successful, the token is stored in the Usergrid.ApiClient singleton and will be used for all subsequent calls.  

Persistent storage is not used to store the token, so it will be lost if the site is refreshed.  You can use the Local Storage feature of most browsers to cache this token if required.

An example of how to use the Usergrid.ApiClent.logInAppUser() method can be seen in our Messagee Sample app:

<https://github.com/apigee/usergrid-sample-html5-messagee>

To get started, you would first build a login form:

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

Once the user has been logged in, you will be able to make calls to the API on their behalf.  Their access token will be stored and used for all future calls.


###To log a user out
To log the user out, simply call:
	
	Usergrid.ApiClient.logoutAppUser();

This will destroy their token in the ApiClient singleton.


#Direct API calls
For most purposes, some combination of Entity and Collection will likely suffice.  However, there are times when it will be necessary to make a direct call to the API.  The following sections describe how to do this against the Application endpoint as well as the Management endpoint.

##Application and Management endpoints

###The Query object
Both the Application endpoint as well as the Management endpoint calls require a Query object. The Query object stores information about the API call that is to be made.  To get started, make a new Query object, passing the relevant data.  In this case, we simply want to query the list of users:

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
    
###Function Signature
The function signature for the Query Object is as follows:

	(method, resource, jsonObj, paramsObj, successCallback, failureCallback)

###method
POST, GET, PUT, or DELETE

###resource 
The resource to access (e.g. "users")

###jsonObj
A JSON object that contains the payload of data - only applicable for POST and PUT operations.  For example, to update some information of a "dogs" object, use something like this:

	var jsonObj = {'name':'fido','color':'black','breed':'mutt'};

###paramsObj
A JSON object that contains the query data (e.g. to modify the search parameters).  For example, to modify the number of results and order the results descending, use an object like this:

	var paramsObj = {'ql':'order by created DESC','limit':'100'};

###successCallback
The success callback function - will be invoked upon successful completion of the API call.

###failureCallback
The failure callback function - will be invoked if the API call fails.


###Application endpoint
Once the query object is ready, simply pass it as an argument to the appropriate endpoint. To run a query against the Application endpoint:

	Usergrid.ApiClient.runAppQuery(queryObj);

###Management endpoint
To run a query against the Application endpoint:

	Usergrid.ApiClient.runManagementQuery(queryObj);

##More information
For more information on Apigee App Services, visit <http://apigee.com>.



 


