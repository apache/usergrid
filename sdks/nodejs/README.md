##Overview
Apigee provides this Node.js SDK, which simplifies the process of making API calls to App Services from within Node.js. The Apigee App Services Node.js SDK is available as an open-source project in github and we welcome your contributions and suggestions. The repository is located at:

<https://github.com/apigee/usergrid-node-js-sdk>

To find out more about Apigee App Services, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/usergrid/>


##Standard Javascript SDK
Are you looking for our Standard Javascript SDK?  Find the repo here:

<https://github.com/apigee/usergrid-javascript-sdk>

Use the Javacript SDK if you are building an HTML5 / Javascript app that runs client-side (in a browser or on a platform like PhoneGap or Trigger.io).


##Getting started
To get you started, please note that the SDK consists of one main JavaScript file, located in the project at:

  /lib/usergrid-sdk.js
  
With a dependency on:
  
  /lib/XMLHttpRequest.js
  
Simply include the SDK to begin to use it:

  var sdk = require('usergrid-sdk')
  
Then initialize it with your app and org id:

  sdk.ApiClient.init('apigee', 'nodejs')
  
You are now ready to use the sdk handle to make calls against the API.  See the sample app for more example usage.

##Sample / Test app
After installing Node on your system, navigate to the directory where you put the code repo and run the following command to start the sample app:

  node test/server.js
  
This will start the node server. If it was successful, you should see the following on the command line:

  Server has started.
  Server running at http://127.0.0.1:8888/

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

##Making API calls
There are several methods for making aTo make API calls, and the next few sections describe ways they can be made.  However, in all cases, the calls to the SDK should be prefaced with the handle to the SDK.  

In the example app, we include the SDK like so:

	var sdk = require("../lib/usergrid.appSDK");
	
So all subsequent calls to the SDK should use the "sdk" handle like so:

	sdk.ApiClient.init('apigee', 'nodejs');


##Direct API calls to the Application and Management endpoints
Creating and managing Entity and Collection objects is sufficient for most purposes.  However, there are times when it is necessary to make a direct call to the API.  The following sections describe how to do this against the Application endpoint as well as the Management endpoint.

See examples of this in the `controller.js` file

###The Query object
Calls to both the Application endpoint as well as the Management endpoint require a Query object. The Query object stores information about the API call you want to make.  To get started, create a new Query object, and pass in the relevant data.  In the following example, we simply want to query the list of users:

	queryObj = new sdk.Query(
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

	sdk.ApiClient.runAppQuery(queryObj);

###Management endpoint
To run a query against the Management endpoint:

	sdk.ApiClient.runManagementQuery(queryObj);

###Putting it all together
Both the API call and the Query object can be made in the same call:

 	sdk.ApiClient.runAppQuery (new sdk.Query('GET', 'users', null, null,
    	function(output) {
       		//do something with the return value "output" here  
     	},
     	function (outpot) {
       		//do something with the return error value "output" here 
     	}
  	));

The above call will make a GET call to get all the users in the application.

###To log a user in
To log app users in, use the Usergrid.ApiClient.logInAppUser() method.  This method takes the supplied username and password and attempts to acquire an access token from the API.  If the method successfully acquires the token, the token is stored in the Usergrid.ApiClient singleton and will be used for all subsequent calls. 

	sdk.ApiClient.logInAppUser(username, password,
    	function (output, user) {
      		//do something with the return value "output" here       
    	},
    	function (output) {
      		//do something with the return error value "output" here 
    	}
  	);   
  

After the user is successfully logged in, you can make calls to the API on their behalf.  Their access token will be stored and used for all future calls.


###To log a user out
To log the user out, call:

	sdk.ApiClient.logoutAppUser();

This destroys their token in the ApiClient singleton.


##Entities and Collections
Entities and Collections are used to model the custom data you need to store in your app.  To enable you to use these in your app, the Javascript SDK provides the Entity and the Collection objects. The following sections describe how to create and use these objects and show examples from the Dogs sample app.

##The Entity Object
Start by creating a new Entity object, where the argument is the name of the collection that the entity will be part of. In the Dogs sample app, here is how a new dogs entity is created in a collection named dogs:

	var dog = new sdk.Entity("dogs");

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

	var dogs = new sdk.Collection('dogs'); //makes a new 'dogs' collection object

If your collection already exists on the server, call the get() method to populate your new object with data from the server. For example:

	dogs.get();

By default, the dogs.get() method uses the API to retrieve the first 10 dog entities and loads them into the dogs Collection object. If you want to add a new entity to the collection, simply create it. For example:

	var dog = new sdk.Entity("dogs");
	dog.set("name","fido");

Then add it to the collection (and save it to the API):

	dog.addNewEntity(dog);

Note:  The addNewEntity() method adds the entity to the collection and *also* saves it to the API.  If you have already saved an entity, you can simply call the addEntity() method.

So this:

	var dog = new sdk.Entity("dogs");
	dog.save();
	dogs.addEntity(dog); //entity is added only

Is equivalent to this:

	var dog = new sdk.Entity("dogs");
	dogs.addNewEntity(dog); //entity is added and saved


###Displaying Results
After you populate your Collection object, you can display a list of all the entities currently stored in the Collection object. Here's how it's done in the Dogs app:

	//iterate through all the items in this "page" of data
	while(dogs.hasNextEntity()) {
		//get a reference to the dog
		var dog = dogs.getNextEntity();
		//do something with the next dog in the list
		//value is in dog.get('name');
	}

Note: This code snippet only loops through the items currently stored in the Collection object.  If there are more entities in the database that you want to display, either use paging, or a custom query.


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

	var marty = new sdk.Entity("users");

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


##Session Management
One of the first key features of this SDK is session management. Node.js does not implement any type of session management out of the box.  To over come this, we added session management similar to what is used in PHP.  

The SDK uses a combination of local file storage and cookies to keep user data safe, yet highly available.  Each new user is given a new session id, and a file of the same name is created for them.  By default, these files are stored in the /tmp directory.  This value can be overridden if needed:

	sdk.Usergrid.session.set_session_dir('/path/to/your/session/directory');
	
Currently there is no garbage collection for session files (see next section).  We welcome any contributions!

##Possible enhancements
There are a few items that would likely be needed in a production environment:

1.  Cron job to remove old session files. 
2.  Greater precision on filenames of session files.  Using the epoch time to get the millisecond value works great for a demo, but on a high traffic site, it is possible that there could be name collisions.


## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-stack), the Usergrid Javascript SDK is open source and licensed under the Apache License, Version 2.0.

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