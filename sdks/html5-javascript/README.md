# HTML5-JavaScript SDK

##Quickstart
Detailed instructions follow but if you just want a quick example of how to get started with this SDK, here’s a minimal HTML5 file that shows you how to include & initialize the SDK, as well as how to read & write data from Usergrid with it.

```html
<!DOCTYPE html>
<html>
	<head>
		<!-- Don't forget to include the SDK -->
		<script src="/path/to/usergrid.js"></script>
		<!-- You can find the file in the Usergrid github repo
			 https://github.com/usergrid/usergrid/blob/master/sdks/html5-javascript/usergrid.js -->

		<script type="text/javascript">

		
			// Initializing the SDK
			var client = new Usergrid.Client({
				orgName:'yourorgname', // Your Usergrid organization name (or apigee.com username for App Services)
				appName:'sandbox' // Your Usergrid app name
			});

			// Make a new "book" collection and read data
			var options = {
				type:'books',
				qs:{ql:'order by created DESC'}
			}

			var books;

			client.createCollection(options, function (err, collection) {
				books = collection;
				if (err) {
					alert("Couldn't get the list of books.");
				} else {
					while(books.hasNextEntity()) {
						var book = books.getNextEntity();
						alert(book.get("title")); // Output the title of the book
					}
				}
			});

			// Uncomment the next 4 lines if you want to write data

			// book = { "title": "the old man and the sea" };
			// books.addEntity(book, function (error, response) {
			// 	if (error) { alert("Couldn't add the book.");
			// 	} else { alert("The book was added."); } });
		</script>
	</head>
	<body></body>
</html>
```

##Version

Current Version: **0.11.0**

See change log:

<https://github.com/usergrid/usergrid/blob/master/sdks/html5-javascript/changelog.md>

##Comments / questions
For help using this SDK, reach out on the Usergrid google group:

https://groups.google.com/forum/?hl=en#!forum/usergrid

Or just open github issues. 



##Overview
This open source SDK simplifies writing JavaScript / HTML5 applications that connect to Usergrid. The repo is located here:

<https://github.com/usergrid/usergrid/tree/master/sdks/html5-javascript>

You can download this package here:

* Download as a zip file: <https://github.com/usergrid/usergrid/archive/master.zip>
* Download as a tar.gz file: <https://github.com/usergrid/usergrid/archive/master.tar.gz>

The Javascript SDK is in the sdks/html5-javascript folder.

To find out more about Usergrid, see:

<http://usergrid.apache.org>

To view the Usergrid documentation, see:

<http://usergrid.apache.org/docs/>


##Node.js
Want to use Node.js? No problem - use the Usergrid Node Module:

<https://npmjs.org/package/usergrid>

or on github:

<https://github.com/usergrid/usergrid/tree/master/sdks/nodejs>

The syntax for this Javascript SDK and the Usergrid Node module are almost exactly the same so you can easily transition between them.


##About the samples
This SDK comes with a variety of samples that you can use to learn how to connect your project to App Services (Usergrid).

**Note:** All the sample code in this file is pulled directly from the "test" example, so you can rest-assured that it will work! You can find it here:

<https://github.com/usergrid/usergrid/blob/master/sdks/html5-javascript/examples/test/test.html>


##Installing
Once you have downloaded the SDK, add the usergrid.js file to your project. This file is located in the root of the SDK. Include it in the top of your HTML file (in between the head tags):

	<script src="path/to/usergrid.js" type="text/javascript"></script>

##Getting started
You are now ready to create a new `Client` object, which is the main entry point in the SDK:

	var client = new Usergrid.Client({
		orgName:'yourorgname',
		appName:'sandbox',
		logging: true, // Optional - turn on logging, off by default
		buildCurl: true // Optional - turn on curl commands, off by default
	});

The last two items are optional. The `logging` option will enable console.log output from the client, and will output various bits of information (calls that are being made, errors that happen).  The `buildCurl` option will cause cURL equivalent commands of all calls to the API to be displayed in the console.log output (see more about this below).

**Note:** You can find your organization name and application in the [Admin Portal](http://apigee.com/usergrid).

You are now ready to use the `Client` object to make calls against the API.

##Asynchronous vs. synchronous calls (a quick discussion)
This SDK works by making RESTful API calls from your application to the App Services (Usergrid) API. This SDK currently only supports asynchronous calls. 

If an API call is _synchronous_, it means that code execution will block (or wait) for the API call to return before continuing. This SDK does not yet support synchronous calls.

_Asynchronous_ calls, which are supported by this SDK, do not block (or wait) for the API call to return from the server. Execution continues on in your program, and when the call returns from the server, a "callback" function is executed. For example, in the following code, the function `dogCreateCallback` will be called when the `createEntity` call returns from the server.  Meanwhile, execution will continue.

	function dogCreateCallback(err, dog) {
		alert('I will probably be called second');
		if (err) {
			// Error - Dog not created
		} else {
			// Success - Dog was created
		}
	}
	
	client.createEntity({type:'dogs'}, dogCreateCallback);
	
	alert('I will probably be called first');

The result of this is that we cannot guarantee the order of the two alert statements.  Most likely, the alert right after the `createEntity` function will be called first because the API call will take a second or so to complete.

The important point is that program execution will continue asynchronously, the callback function will be called once program execution completes.

##Entities and collections
Usergrid stores its data as _entities_ in _collections_.  Entities are essentially JSON objects and collections are just like folders for storing these objects. You can learn more about entities and collections in the App Services docs:

<http://apigee.com/docs/usergrid/content/data-model>


##Entities
You can easily create new entities, or access existing ones. Here is a simple example that shows how to create a new entity of type "dogs":

	var options = {
		type:'dogs',
		name:'einstein'
	}

	client.createEntity(options, function (err, dog) {
		if (err) {
			//Error - Dog not created
		} else {
			//Success - Dog was created
		}
	});

**Note:** All calls to the API will be executed asynchronously, so it is important that you use a callback.

Once your object is created, you can update properties on it by using the `set` method, then save it back to the database using the `save` method:

	// Once the dog is created, you can set single properties (key, value)
	dog.set('breed','mutt');

	// The set function can also take a JSON object
	var data = {
		master:'Doc',
		state:'hungry'
	}

	// Set is additive, so previously set properties are not overwritten unless a property with the same name exists in the data object
	dog.set(data);

	// And save back to the database
	dog.save(function(err){
		if (err){
			// Error - dog not saved
		} else {
			// Success - dog was saved
		}
	});

**Note:** Using the `set` function will set the properties locally. Make sure you call the `save` method on the entity to save them back to the database!

You can also refresh the object from the database if needed (in case the data has been updated by a different client or device) by using the `fetch` method.  Use the `get` method to retrieve properties from the object:

	// Call fetch to refresh the data from the server
	dog.fetch(function(err){
		if (err){
			// Error - dog not refreshed from database
		} else {
			// Dog has been refreshed from the database
			// Will only work if the UUID for the entity is in the dog object

			// Get single properties from the object using the get method
			var master = dog.get('master');
			var name = dog.get('name');

			// Or, get all the data as a JSON object:
			var data = dog.get();

			// Based on statements above, the data object should look like this:
			/*
			{
				type:'dogs',
				name:'einstein',
				master:'Doc',
				state:'hungry',
				breed:'mutt'
			}
			*/

		}
	});

Use the `destroy` method to remove the entity from the database:

	// The destroy method will delete the entity from the database
	dog.destroy(function(err){
		if (err){
			// Error - dog not removed from database
		} else {
			// Success - dog was removed from database
			dog = null;
			// No real dogs were harmed!
		}
	});

##The collection object
The collection object models collections in the database. Once you start programming your app, you will likely find that this is a useful method of interacting with the database.  Creating a collection will automatically populate the object with entities from the collection.

The following example shows how to create a collection object, then how to use those entities once they have been populated from the server:

	// Options object needs to have the type (which is the collection type)
	// ”qs” is for “query string”. “ql” is for “query language”
	var options = {
		type:'dogs',
		qs:{ql:'order by index'}
	}

	client.createCollection(options, function (err, dogs) {
		if (err) {
			// Error - could not make collection
		} else {
			// Success - new collection created

			// We got the dogs, now display the entities
			while(dogs.hasNextEntity()) {
				// Get a reference to the dog
				dog = dogs.getNextEntity();
				// Do something with the entity
				var name = dog.get('name');
			}

		}
	});

You can also add a new entity of the same type to the collection:

	// Create a new dog and add it to the collection
	var options = {
		name:'extra-dog',
		fur:'shedding'
	}
	// Just pass the options to the addEntity method
	// to the collection and it is saved automatically
	dogs.addEntity(options, function(err, dog, data) {
		if (err) {
			// Error - extra dog not saved or added to collection
		} else {
			// Success - extra dog saved and added to collection
		}
	});

##Collection iteration and paging
At any given time, the collection object will have one page of data loaded. To get the next page of data from the server and iterate across the entities, use the following pattern:

	if (dogs.hasNextPage()) {
		// There is a next page, so get it from the server
		dogs.getNextPage(function(err){
			if (err) {
				// Error - could not get next page of dogs
			} else {
				// Success - got next page of dogs, so do something with it
				while(dogs.hasNextEntity()) {
					// Get a reference to the dog
					dog = dogs.getNextEntity();
					// Do something with the entity
					var name = dog.get('name');
				}
			}
		});
	}

You can use the same pattern with the `hasPreviousPage` and `getPreviousPage` methods to get a previous page of data.

By default, the database will return 10 entities per page.  Use the `qs` (query string) property (more about this below) to set a different limit (up to 999):

	var options = {
		type:'dogs',
		qs:{limit:50} // Set a limit of 50 entities per page
	}

	client.createCollection(options, function (err, dogs) {
		if (err) {
			// Error - could not get all dogs
		} else {
			// Success - got at most 50 dogs

			// We got 50 dogs, now display the entities
			while(dogs.hasNextEntity()) {
				// Get a reference to the dog
				var dog = dogs.getNextEntity();
				// Do something with the entity
				var name = dog.get('name');
			}

			// Wait! What if we want to display them again??
			// Simple! Just reset the entity pointer:
			dogs.resetEntityPointer();
			while(dogs.hasNextEntity()) {
				// Get a reference to the dog
				var dog = dogs.getNextEntity();
				// Do something with the entity
				var name = dog.get('name');
			}

		}
	});

Several convenience methods exist to make working with pages of data easier:

* `resetPaging` - Calls made after this will get the first page of data (the cursor, which points to the current page of data, is deleted)
* `getFirstEntity` - Gets the first entity of a page
* `getLastEntity` - Gets the last entity of a page
* `resetEntityPointer` - Sets the internal pointer back to the first element of the page
* `getEntityByUUID` - Returns the entity if it is in the current page

###Custom Queries
A custom query allows you to tell the API that you want your results filtered or altered in some way.

Use the `qs` property in the options object - "qs" stands for "query string".  By adding a JSON object of key/value pairs to the `qs` property of the options object, you signal that you want those values used for the key/value pairs in the query string that is sent to the server.

For example, to specify that the query results should be ordered by creation date, add the `qs` parameter to the options object:

	var options = {
		type:'dogs',
		qs:{ql:'order by created DESC'}
	};

The `qs` object above will be converted into a query language call that looks like this:

	/dogs?ql=order by created DESC

If you also wanted to get more entities in the result set than the default 10, say 100, you can specify a query similar to the following (the limit can be a maximum of 999):

	dogs.qs = {ql:'order by created DESC',limit:'100'};

The `qs` object above will be converted into a call that looks like this:

	/dogs?ql=order by created DESC&limit=100

**Note**: There are many cases where expanding the result set is useful. But be careful -- the more results you get back in a single call, the longer it will take to transmit the data back to your app.

If you need to change the query on an existing object, simply access the `qs` property directly:

	dogs.qs = {ql:'order by created DESC'};

Then make your fetch call:

	dogs.fetch(...)

Another common requirement is to limit the results to a specific query.  For example, to get all brown dogs, use the following syntax:

	dogs.qs = {ql:"select * where color='brown'"};

You can also limit the results returned such that only the fields you specify are returned:

	dogs.qs = {'ql':"select name, age where color='brown'"};

**Note:** In the two preceding examples that we put single quotes around 'brown', so it will be searched as a string.

You can find more information on custom queries here:

<http://usergrid.apache.org/docs/query-language/>
##Counters
Counters can be used by an application to create custom statistics, such as how many times an a file has been downloaded or how many instances of an application are in use.

###Create the counter instance

**Note:** The count is not altered upon instantiation

	var counter = new Usergrid.Counter({
			client: client,
			data: {
				category: 'usage',
				//a timestamp of '0' defaults to the current time
				timestamp: 0,
				counters: {
					running_instances: 0,
					total_instances: 0
				}
			}
		}, function(err, data) {
			if (err) { 
			   // Error - there was a problem creating the counter
			} else { 
				// Success - the counter was created properly
			}
		});

###Updating counters

When an application starts, we want to increment the 'running_instances' and 'total_instances' counters.

	// add 1 running instance
	counter.increment({
			name: 'running_instances',
			value: 1
		}, function(err, data) {
			// ...
		});

	// add 1 total instance
	counter.increment({
			name: 'total_instances',
			value: 1
		}, function(err, data) {
			// ...
		});

When the application exits, we want to decrement 'running_instances'

	// subtract 1 total instance
	counter.decrement({
			name: 'total_instances',
			value: 1
		}, function(err, data) {
			// ...
		});

Once you have completed your testing, you can reset these values to 0.

	counter.reset({
			name: 'total_instances'
		}, function(err, data) {
			// ...
		});

##Assets

Assets can be attached to any entity as binary data. This can be used by your application to store images and other file types. There is a limit of one asset per entity.

####Attaching an asset

An asset can be attached to any entity using Enity.attachAsset(file, callback). You can also call attachAsset() on the same entity to change the asset attached to it.

	//Create a new entity to attach an asset to - you can also use an existing entity
	var properties = {
	    type:'user',
	    username:'someUser', 
	};

	dataClient.createEntity(properties, function(err, response, entity) {
		if (!err) {
			//The entity was created, so call attachAsset() on it.
			entity.attachAsset(file, function(err, response){
				if (!err){
					//Success - the asset was attached
				} else {
					//Error
				}
			});
		}
	});

##Retrieving Assets

To retrieve the data, call Entity.downloadAsset(callback). A blob is returned
in the success callback.

	entity.downloadAsset(function(err, file){
		if (err) { 
			// Error - there was a problem retrieving the data
		} else { 
			// Success - the asset was downloaded			

			// Create an image tag to hold our downloaded image data
			var img = document.createElement("img");
			
			// Create a FileReader to feed the image
			// into our newly-created element
			var reader = new FileReader();
			reader.onload = (function(aImg) { 
					return function(e) {
						aImg.src = e.target.result;
					}; 
				})(img);
			reader.readAsDataURL(file);
			
			// Append the img element to our page
			document.body.appendChild(img);
		} 
	})



##Modeling users with the entity object
Use the entity object to model users.  Simply specify a type of "users".

**Note:** Remember that user entities use "username", not "name", as the distinct key.
Here is an example:

	// Type is 'users', set additional parameters as needed.
	var options = {
		type:'users',
		username:'marty',
		password:'mysecurepassword',
		name:'Marty McFly',
		city:'Hill Valley'
	}

	client.createEntity(options, function (err, marty) {
		if (err){
			//Error - user not created
		} else {
			//Success - user created
			var name = marty.get('name');
		}
	});

If the user is modified, just call save on the user again:

	// Add properties cumulatively.
	marty.set('state', 'California');
	marty.set('girlfriend','Jennifer');

	marty.save(function(err){
		if (err){
			// Error - user not updated
		} else {
			// Success - user updated
		}
	});

To refresh the user's information in the database:

	marty.fetch(function(err){
		if (err){
			// Error - not refreshed
		} else {
			// Success - user refreshed

			// Do something with the entity
			var girlfriend = marty.get('girlfriend');
		}
	});

###To sign up a new user
When a new user wants to sign up in your app, simply create a form to catch their information, then use the `client.signup` method:

	// Method signature: client.signup(username, password, email, name, callback)
	client.signup('marty', 'mysecurepassword', 'marty@timetravel.com', 'Marty McFly',
		function (err, marty) {
			if (err){
				error('User not created');
				runner(step, marty);
			} else {
				success('User created');
				runner(step, marty);
			}
		}
	);


###To log a user in
Logging a user in means sending the user's username and password to the server, and getting back an access (OAuth) token. You can then use this token to make calls to the API on the user's behalf. The following example shows how to log a user in and log them out:

	username = 'marty';
	password = 'mysecurepassword';
	client.login(username, password, function (err) {
		if (err) {
			// Error - could not log user in
		} else {
			// Success - user has been logged in

			// The login call will return an OAuth token, which is saved
			// in the client. Any calls made now will use the token.
			// Once a user has logged in, their user object is stored
			// in the client and you can access it this way:
			var token = client.token;

			// Then make calls against the API.  For example, you can
			// get the logged in user entity this way:
			client.getLoggedInUser(function(err, data, user) {
				if(err) {
					// Error - could not get logged in user
				} else {
					// Success - got logged in user

					// You can then get info from the user entity object:
					var username = user.get('username');
				}
			});
		}
	});

If you need to change a user's password, set the `oldpassword` and `newpassword` fields, then call save:

	marty.set('oldpassword', 'mysecurepassword');
	marty.set('newpassword', 'mynewsecurepassword');
	marty.save(function(err){
		if (err){
			// Error - user password not updated
		} else {
			// Success - user password updated
		}
	});

To log a user out, call the `logout` function:

	client.logout();

	// verify the logout worked
	if (client.isLoggedIn()) {
		// Error - logout failed
	} else {
		// Success - user has been logged out
	}

###Making connections
Connections are a way to connect two entities with a word, typically a verb.  This is called an _entity relationship_.  For example, if you have a user entity with username of marty, and a dog entity with a name of einstein, then using our RESTful API, you could make a call like this:

	POST users/marty/likes/dogs/einstein

This creates a one-way connection between marty and einstein, where marty "likes" einstein.

Complete documentation on the entity relationships API can be found here:

<http://usergrid.apache.org/docs/relationships/>

For example, say we have a new dog named einstein:

	var options = {
		type:'dogs',
		name:'einstein',
		breed:'mutt'
	}

	client.createEntity(options, function (err, dog) {
		if (err) {
			// Error - new dog not created
		} else {
			// Success - new dog created
		}
	});

Then, we can create a "likes" connection between our user, Marty, and the dog named einstein:

	marty.connect('likes', dog, function (err, data) {
		if (err) {
			// Error - connection not created
		} else {
			// Success - the connection call succeeded
			// Now let’s do a getConnections call to verify that it worked
			marty.getConnections('likes', function (err, data) {
				if (err) {
					// Error - could not get connections
				} else {
					// Success - got all the connections
					// Verify that connection exists
					if (marty.likes.einstein) {
						// Success - connection exists
					} else {
						// Error - connection does not exist
					}
				}
			});
		}
	});

We could have just as easily used any other word as the connection (e.g. "owns", "feeds", "cares-for", etc.).

Now, if you want to remove the connection, do the following:

	marty.disconnect('likes', dog, function (err, data) {
		if (err) {
			// Error - connection not deleted
		} else {
			// Success - the connection has been deleted
			marty.getConnections('likes', function (err, data) {
				if (err) {
					// Error - error getting connections
				} else {
					// Success! - now verify that the connection exists
					if (marty.likes.einstein) {
						// Error - connection still exists
					} else {
						// Success - connection deleted
					}
				}
			});
		}
	});

If you no longer need the object, call the `destroy` method and the object will be deleted from database:

	marty.destroy(function(err){
		if (err){
			// Error - user not deleted from database
		} else {
			// Success - user deleted from database
			marty = null; // Blow away the local object
		}
	});

##Making generic calls
If you find that you need to make calls to the API that fall outside of the scope of the entity and collection objects, you can use the following format to make any REST calls against the API:

	client.request(options, callback);

This format allows you to make almost any call against the Usergrid API. For example, to get a list of users:

	var options = {
		method:'GET',
		endpoint:'users'
	};
	client.request(options, function (err, data) {
		if (err) {
			// Error - GET failed
		} else {
			// Data will contain raw results from API call
			// Success - GET worked
		}
	});

Or, to create a new user:

	var options = {
		method:'POST',
		endpoint:'users',
		body:{ username:'fred', password:'secret' }
	};
	client.request(options, function (err, data) {
		if (err) {
			// Error - POST failed
		} else {
			// Data will contain raw results from API call
			// Success - POST worked
		}
	});

Or, to update a user:

	var options = {
		method:'PUT',
		endpoint:'users/fred',
		body:{ newkey:'newvalue' }
	};
	client.request(options, function (err, data) {
		if (err) {
			// Error - PUT failed
		} else {
			// Data will contain raw results from API call
			// Success - PUT worked
		}
	});

Or, to delete a user:

	var options = {
		method:'DELETE',
		endpoint:'users/fred'
	};
	client.request(options, function (err, data) {
		if (err) {
			// Error - DELETE failed
		} else {
			// Data will contain raw results from API call
			// Success - DELETE worked
		}
	});

The `options` object for the `client.request` function includes the following:

* `method` - HTTP method (`GET`, `POST`, `PUT`, or `DELETE`), defaults to `GET`
* `qs` - object containing querystring values to be appended to the URI
* `body` - object containing entity body for POST and PUT requests
* `endpoint` - API endpoint, for example "users/fred"
* `mQuery` - boolean, set to `true` if running management query, defaults to `false`
* `buildCurl` - boolean, set to `true` if you want to see equivalent curl commands in console.log, defaults to `false`

You can make any call to the API using the format above.  However, in practice using the higher level entity and collection objects will make life easier as they take care of much of the heavy lifting.


###Validation
An extension for validation is provided for you to use in your apps.  The file is located here:

	/extensions/usergrid.session.js

Include this file at the top of your HTML file - AFTER the SDK file:

	<script src="path/to/usergrid.js" type="text/javascript"></script>
	<script src="path/to/extensions/usergrid.validation.js" type="text/javascript"></script>

A variety of functions are provided for verifying many common types such as usernames, passwords, and so on.  Feel free to copy and modify these functions for use in your own projects.


###cURL
[cURL](http://curl.haxx.se/) is an excellent way to make calls directly against the API. As mentioned in the **Getting started** section of this guide, one of the parameters you can add to the new client `options` object is **buildCurl**:

	var client = new Usergrid.Client({
		orgName:'yourorgname',
		appName:'sandbox',
		logging: true, // Optional - turn on logging, off by default
		buildCurl: true // Optional - turn on curl commands, off by default
	});

If you set this parameter to `true`, the SDK will build equivalent `curl` commands and send them to the console.log window. To learn how to see the console log, see this page:

<http://apigee.com/docs/usergrid/content/displaying-app-services-api-calls-curl-commands>

More information on cURL can be found here:

<http://curl.haxx.se/>

## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-node-module), the Usergrid Javascript SDK is open source and licensed under the Apache License, Version 2.0.

1. Fork it.
2. Create your feature branch (`git checkout -b my-new-feature`).
3. Commit your changes (`git commit -am 'Added some feature'`).
4. Push your changes to the upstream branch (`git push origin my-new-feature`).
5. Create new Pull Request (make sure you describe what you did and why your mod is needed).

###Contributing to usergrid.js
usergrid.js and usergrid.min.js are built from modular components using Grunt. If you want to contribute updates to these files, please commit your changes to the modules in /lib/modules. Do not contribute directly to usergrid.js or your changes could get overwritten in a future build.

##More information
For more information on Usergrid, visit <http://usergrid.apache.org/>.
For more information on Apigee App Services, visit <http://developers.apigee.com>.

## Copyright
Copyright 2014 Apigee Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


