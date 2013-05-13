##Quickstart
Detailed instructions follow but if you just want a quick example of how to get started with this SDK, here’s a minimal HTML5 file that shows you how to include & initialize the SDK, as well as how to read & write data from Usergrid with it.

```html
<!DOCTYPE html>
<html>
	<head>
		<!-- Don't forget to download and include the SDK -->
		<!-- It’s available at the root of github.com/apigee/usergrid-javascript-sdk -->
		<script src="path/to/usergrid.js"></script>

		<script type="text/javascript">
		
			// Initializing the SDK
			var client = new Usergrid.Client({
				orgName:'yourorgname', // Your Usergrid organization name (or apigee.com username for App Services)
				appName:'sandbox' // Your Usergrid app name
			});

			// Reading data
			var books = new Usergrid.Collection({ "client":client, "type":"books" });
			books.fetch(
				function() { // Success
					while(books.hasNextEntity()) {
						var book = books.getNextEntity();
						alert(book.get("title")); // Output the title of the book
					}
				}, function() { // Failure
					alert("read failed");
				}
			);

			// Uncomment the next 4 lines if you want to write data
			
			// book = { "title": "the old man and the sea" };
			// books.addEntity(book, function (error, response) {
			// 	if (error) { alert("write failed");
			// 	} else { alert("write succeeded"); } });
		</script>
	</head>
	<body></body>
</html>
```

##Version

Current Version: **0.10.4**

See change log:

<https://github.com/apigee/usergrid-javascript-sdk/blob/master/changelog.md>

**About this version:**

I revved this version of the SDK to 0.10.x because it is a complete rework of the code.  Not only is the code new, but also the way that you use it (more about that below).  I know that many of you have already built apps on the SDK and reworking them to use this new format will likely be a pain.  I apologize for that.

Hopefully that you will find it is worth the upgrade. I think this new version is more streamlined and has better structure.  I also made it consistent syntactically with the new [Node.js Module](https://github.com/apigee/usergrid-node-module).  The idea here is that developers may want to make client-side calls in their Node.js applications, so why not use the same syntax for both.

##Comments / Questions
Please feel free to send comments or questions:

	twitter: @rockerston
	email: rod at apigee.com

Or just open github issues.  I truly want to know what you think, and will address all suggestions / comments / concerns.

Thank you!

Rod


##Overview
This open source SDK simplifies writing JavaScript / HTML5 applications that connect to App Services. The repo is located here:

<https://github.com/apigee/usergrid-javascript-sdk>

You can download this package here:

* Download as a zip file: <https://github.com/apigee/usergrid-javascript-sdk/archive/master.zip>
* Download as a tar.gz file: <https://github.com/apigee/usergrid-javascript-sdk/archive/master.tar.gz>


To find out more about Apigee App Services, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/app_services>


##Node.js
Want to use Node.js? No problem - use the Usergrid Node Module:

<https://npmjs.org/package/usergrid>

or on github:

<https://github.com/apigee/usergrid-node-module>

The syntax for this Javascript SDK and the Usergrid Node module are almost exactly the same so you can easily transition between them.


##About the samples
This SDK comes with a variety of samples that you can use to learn how to connect your project to App Services (Usergrid). See these examples running live now:

<http://apigee.github.com/usergrid-javascript-sdk/>

**Note:** All the sample code in this file is pulled directly from the "test" example, so you can rest-assured that it will work!  You can run them yourself here:

<http://apigee.github.com/usergrid-javascript-sdk/examples/test/test.html>


##Installing
Once you have downloaded the SDK, add the usergrid.js file to your project. This file is located in the root of the SDK. Include it in the top of your HTML file (in between the head tags):

	<script src="path/to/usergrid.js" type="text/javascript"></script>

##Getting started
You are now ready to create a new client, which is the main entry point to the SDK:

	var client = new Usergrid.Client({
		orgName:'yourorgname',
		appName:'sandbox',
		logging: true, //optional - turn on logging, off by default
		buildCurl: true //optional - turn on curl commands, off by default
	});
The last two items are optional. The **logging** option will enable console.log output from the client, and will output various bits of information (calls that are being made, erros that happen).  The **buildCurl** option will cause cURL equivalent commands of all calls to the API to be displayed in the console.log output (see more about this below).

**Note:** you can find your organization name and application in the [Admin Portal](http://apigee.com/usergrid).

You are now ready to use the client to make calls against the API.

##Asynchronous vs. Synchronous calls (a quick discussion)
This SDK works by making RESTful API calls from your application to the App Services (Usergrid) API. This SDK currently only supports Asynchronous calls. 

###Synchronous calls
If an API call is synchronous, it means that code execution will block (or wait) for the API call to return before continuing.  This SDK does not yet support synchronous calls.

###Asynchronous 
Asynchronous calls, which are supported by this SDK, do not block (or wait) for the API call to return from the server.  Execution continues on in your program, and when the call returns from the server, a "callback" function is executed. For example, in the following code, the function called dogCreateCallback will be called when the create dog API call returns from the server.  Meanwhile, execution will continue:


	function dogCreateCallback(err, dog) {
		alert('I will probably be called second');
		if (err) {
			//Error - Dog not created
		} else {
			//Success - Dog was created

		}
	}
	
	client.createEntity({type:'dogs'}, dogCreateCallback);
	
	alert('I will probably be called first');

The result of this is that we cannot guarantee the order of the two alert statements.  Most likely, the alert right after the createEntity function will be called first since the API call will take a second or so to complete.  

The important point is that program execution will continue, and asynchronously, the callback function will be called once program execution completes.



##Entities and Collections
Usergrid stores its data as "Entities" in "Collections".  Entities are essentially JSON objects and Collections are just like folders for storing these objects. You can learn more about Entities and Collections in the App Services docs:

<http://apigee.com/docs/usergrid/content/data-model>


##Entities
You can easily create new entities, or access existing ones. Here is a simple example that shows how to create a new Entity of type "dogs":

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

**note:** all calls to the API will be executed asynchronously, so it is important that you use a callback.

Once your object is created, you an update properties on it by using the "set" method, then save it back to the database using the "save" method

	//once the dog is created, you can set single properties (key, value):
	dog.set('breed','mutt');

	//the set function can also take a JSON object:
	var data = {
		master:'Doc',
		state:'hungry'
	}

	//set is additive, so previously set properties are not overwritten
	dog.set(data);

	//and save back to the database
	dog.save(function(err){
		if (err){
			//Error - dog not saved
		} else {
			//Success - dog was saved
		}
	});

**Note:** Using the "set" function will set the properties locally. Make sure you call the .save() method on the entity to save them back to the database!

You can also refresh the object from the database if needed (in case the data has been updated by a different client or device) by using the fetch method.  Use the get method to retrieve properties from the object:

	//call fetch to refresh the data from the server
	dog.fetch(function(err){
		if (err){
			//Error - dog not refreshed from database
		} else {
			//dog has been refreshed from the database
			//will only work if the UUID for the entity is in the dog object

			//get single properties from the object using "get"
			var master = dog.get('master');
			var name = dog.get('name');

			//or, get all the data as a JSON object:
			var data = dog.get();

			//based on statements above, the data object should look like this:
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

**Wait!** But what if my entity already exists on the server?

During a client.createEntity call, there are two ways that you can choose to handle this situation.  The question is, what should the client do if an entity with the same name, username, or uuid already exists on the server?

	1. Give you back an error.
	2. Give you back the pre-existing entity.

If you want to get back an error when the entity already exists, then simply call the client.createEntity function as already described above. If there is a collision, you will get back a 400 error.

However, if you want the existing entity to be returned, then set the getOnExist flag to true:

	//start by getting the uuid of our existing dog named einstein
	var uuid = dog.get('uuid');

	//now create new entity, but use same entity name of einstein.  This means that
	//the original einstein entity now exists.  Thus, the new einstein entity should
	//be the same as the original + any data differences from the options var:
	options = {
		type:'dogs',
		name:'einstein',
		hair:'long',
		getOnExist:true
	}
	client.createEntity(options, function (err, newdog) {
		if (err) {
			//Error - could not get duplicate dog
		} else {
			//Success - got duplicate dog

			//get the id of the new dog
			var newuuid = newdog.get('uuid');
			if (newuuid === uuid) {
				//Success - UUIDs of new and old entities match
			} else {
				//Error - UUIDs of new and old entities do not match
			}

			//verify that our new attribute was added to the existing entity
			var hair = newdog.get('hair');
			if (hair === 'long') {
				//Success - attribute sucesfully set on new entity
			} else {
				//Error - attribute not sucesfully set on new entity
			}
		}
	});

Alternatively, if you know that an entity exists on the server already, and you just want to retrieve it, use the getEntity method. It will only retrieve the entity if there is a match:

	//again, get the uuid of our existing dog:
	var uuid = dog.get('uuid');

	//now make a new call to get the existing dog from the database
	var options = {
		type:'dogs',
		name:'einstein' //could also use uuid if you know it, or username, if this is a user
	}
	client.getEntity(options, function(err, existingDog){
		if (err){
			//existing dog not retrieved
		} else {
			//existing dog was retrieved

			//get the uuid of the dog we just got from the database
			var newuuid = existingDog.get('uuid');

			//make sure the uuids match
			if (uuid === newuuid){
				//uuids match - got the same entity
			} else {
				//uuids do not match - not the same entity
			}
		}
	});

Use the "destroy" method to remove the entity from the database:

	//the destroy method will delete the entity from the database
	dog.destroy(function(err){
		if (err){
			//Error - dog not removed from database
		} else {
			//Success - dog was removed from database
			dog = null;
			// no real dogs were harmed!
		}
	});

##The Collection object
The Collection object models collections in the database.  Once you start programming your app, you will likely find that this is a useful method of interacting with the database.  Creating a collection will automatically populate the object with entities from the collection.

The following example shows how to create a Collection object, then how to use those entities once they have been populated from the server:

	//options object needs to have the type (which is the collection type)
	var options = {
		type:'dogs',
		qs:{ql:'order by index'}
	}

	client.createCollection(options, function (err, dogs) {
		if (err) {
			//Error - could not make collection
		} else {
			//Success - new collection created

			//we got the dogs, now display the Entities:
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				dog = dogs.getNextEntity();
				//do something with the entity
				var name = dog.get('name');
			}

		}
	});

You can also add a new entity of the same type to the collection:

	//create a new dog and add it to the collection
	var options = {
		name:'extra-dog',
		fur:'shedding'
	}
	//just pass the options to the addEntity method
	//to the collection and it is saved automatically
	dogs.addEntity(options, function(err, dog, data) {
		if (err) {
			//Error - extra dog not saved or added to collection
		} else {
			//Success - extra dog saved and added to collection
		}
	});

##Collection iteration and paging
To provide paging functionality, Usergrid uses a cursor to keep track of what page of the data is currently being used. Keeping track of cursors can be tedious, so the Collection object obscures this complexity, and provides easy-to-use methods for paging.

At any given time, the Collection object will have one page of data loaded. To get the next page of data from the server and iterate across the entities, use the following pattern:

	if (dogs.hasNextPage()) {
		//there is a next page, so get it from the server
		dogs.getNextPage(function(err){
			if (err) {
				//Error - could not get next page of dogs
			} else {
				//Success - got next page of dogs, so do something with it:

				while(dogs.hasNextEntity()) {
					//get a reference to the dog
					dog = dogs.getNextEntity();
					//do something with the entity
					var name = dog.get('name');
				}

			}
		});
	}

You can use the same pattern to get a previous page of data:

	if (dogs.hasPreviousPage()) {
		//there is a previous page, so get it from the server
		dogs.getPreviousPage(function(err){
			if(err) {
				//Error - could not get previous page of dogs
			} else {
				//Success - got next page of dogs, so do something with it:

				while(dogs.hasNextEntity()) {
					//get a reference to the dog
					dog = dogs.getNextEntity();
					//do something with the entity
					var name = dog.get('name');
				}

			}
		});
	}

By default, the database will return 10 entities per page.  Use the "qs" property in the options object (more about this below). Set a limit of up to 999:

	var options = {
		type:'dogs',
		qs:{limit:50} //limit statement set to 50
	}

	client.createCollection(options, function (err, dogs) {
		if (err) {
			//Error - could not get all dogs
		} else {
			//Success - got at most 50 dogs

			//we got 50 dogs, now display the Entities:
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				var dog = dogs.getNextEntity();
				//do something with the entity
				var name = dog.get('name');
			}

			//Wait! What if we want to display them again??
			//Simple!  Just reset the entity pointer:
			dogs.resetEntityPointer();
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				var dog = dogs.getNextEntity();
				//do something with the entity
				var name = dog.get('name');
			}

		}
	});

Several convenience methods exist to make working with pages of data easier:

* resetPaging - calls made after this will get the first page of data (the cursor, which points to the current page of data, is deleted)
* getFirstEntity - gets the first entity of a page
* getLastEntity - gets the last entity of a page
* resetEntityPointer - sets the internal pointer back to the first element of the page
* getEntityByUUID - returns the entity if it is in the current page


###Custom Queries
A custom query allows you to tell the API that you want your results filtered or altered in some way.

Use the "qs" property in the options object - "qs" stands for "query string".  By adding a JSON object of key value pairs to the "qs" property of the options object, you signal that you want those values used for the key/value pairs in the query string that is sent to the server.

For example, to specify that the query results should be ordered by creation date, add the qs parameter to the options object:

	var options = {
		type:'dogs',
		qs:{ql:'order by created DESC'}
	};

The qs object above will be converted into a call that looks like this:

/dogs?ql=order by created DESC

If you also wanted to get more entities in the result set than the default 10, say 100, you can specify a query similar to the following (the limit can be a maximum of 999):

	dogs.qs = {ql:'order by created DESC',limit:'100'};

The qs object above will be converted into a call that looks like this:

/dogs?ql=order by created DESC&limit=100

**Note**: there are many cases where expanding the result set is useful.  But be careful - the more results you get back in a single call, the longer it will take to transmit the data back to your app.

If need to change the query on an existing object.  Simply access the qs property directly:

	dogs.qs = {ql:'order by created DESC'};

Then make your fetch call:

	dogs.fetch(...)

Another common requirement is to limit the results to a specific query.  For example, to get all brown dogs, use the following syntax:

	dogs.qs = {ql:"select * where color='brown'"};

You can also limit the results returned such that only the fields you specify are returned:

	dogs.qs = {'ql':"select name, age where color='brown'"};

**Note:** in the two preceding examples that we put single quotes around 'brown', so it will be searched as a string.

You can find more information on custom queries here:

<http://apigee.com/docs/usergrid/content/queries-and-parameters>


##Modeling users with the Entity object
Use the Entity object to model users.  Simply specify a type of "users".

**Note:** remember that user entities use "username", not "name", as the distinct key
Here is an example:

	//type is 'users', set additional paramaters as needed
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

	//add properties cumulatively
	marty.set('state', 'California');
	marty.set("girlfriend","Jennifer");

	marty.save(function(err){
		if (err){
			//Error - user not updated
		} else {
			//Success - user updated
		}
	});

To refresh the user's information in the database:

	marty.fetch(function(err){
		if (err){
			//Error - not refreshed
		} else {
			//Success - user refreshed

			//do something with the entity
			var girlfriend = marty.get('girlfriend');
		}
	});

###To sign up a new user
When a new user wants to sign up in your app, simply create a form to catch their information, then use the client.signin method:

	//method signature: client.signup(username, password, email, name, callback)
	client.signup('marty', 'mysecurepassword', 'marty@timetravel.com', 'Marty McFly',
		function (err, marty) {
			if (err){
				error('user not created');
				runner(step, marty);
			} else {
				success('user created');
				runner(step, marty);
			}
		}
	);


###To log a user in
Logging a user in means sending the user's username and password to the server, and getting back an access (OAuth) token.  You can then use this token to make calls to the API on the User's behalf. The following example shows how to log a user in and log them out:

	username = 'marty';
	password = 'mysecurepassword';
	client.login(username, password, function (err) {
		if (err) {
			//Error - could not log user in
		} else {
			//Success - user has been logged in

			//the login call will return an OAuth token, which is saved
			//in the client. Any calls made now will use the token.
			//once a user has logged in, their user object is stored
			//in the client and you can access it this way:
			var token = client.token;

			//Then make calls against the API.  For example, you can
			//get the logged in user entity this way:
			client.getLoggedInUser(function(err, data, user) {
				if(err) {
					//Error - could not get logged in user
				} else {
					//Success - got logged in user

					//you can then get info from the user entity object:
					var username = user.get('username');
				}
			});
		}
	});

If you need to change a user's password, set the oldpassword and newpassword fields, then call save:

	marty.set('oldpassword', 'mysecurepassword');
	marty.set('newpassword', 'mynewsecurepassword');
	marty.save(function(err){
		if (err){
			//Error - user password not updated
		} else {
			//Success - user password updated
		}
	});

To log a user out, call the logout function:

	client.logout();

	//verify the logout worked
	if (client.isLoggedIn()) {
		//Error - logout failed
	} else {
		//Success - user has been logged out
	}

###Making connections
Connections are a way to connect two entities with a word, typically a verb.  This is called an entity relationship.  For example, if you have a user entity with username of marty, and a dog entity with a name of einstein, then using our RESTful API, you could make a call like this:

	POST users/marty/likes/dogs/einstein

This creates a one-way connection between marty and einstein, where marty "likes" einstein.

Complete documentation on the entity relationships API can be found here:

<http://apigee.com/docs/usergrid/content/entity-relationships>

For example, say we have a new dog named einstein:

	var options = {
		type:'dogs',
		name:'einstein',
		breed:'mutt'
	}

	client.createEntity(options, function (err, dog) {
		if (err) {
			//Error - new dog not created
		} else {
			//Success - new dog created
		}
	});

Then, we can create a "likes" connection between our user, Marty, and the dog named einstien:

	marty.connect('likes', dog, function (err, data) {
		if (err) {
			//Error - connection not created
		} else {
			//Success - the connection call succeeded
			//now lets do a getConnections call to verify that it worked
			marty.getConnections('likes', function (err, data) {
				if (err) {
					//Error - could not get connections
				} else {
					//Success - got all the connections
					//verify that connection exists
					if (marty.likes.einstein) {
						//Success - connection exists
					} else {
						//Error - connection does not exist
					}

				}
			});
		}
	});

We could have just as easily used any other word as the connection (e.g. "owns", "feeds", "cares-for", etc.).

Now, if you want to remove the connection, do the following:

	marty.disconnect('likes', dog, function (err, data) {
		if (err) {
			//Error - connection not deleted
		} else {
			//Success - the connection has been deleted
			marty.getConnections('likes', function (err, data) {
				if (err) {
					//Error - error getting connections
				} else {
					//Success! - now verify that the connection exists
					if (marty.likes.einstein) {
						//Error - connection still exists
					} else {
						//Success - connection deleted
					}
				}
			});
		}
	});

If you no longer need the object, call the delete() method and the object will be deleted from database:

	marty.destroy(function(err){
		if (err){
			//Error - user not deleted from database
		} else {
			//Success - user deleted from database
			marty = null; //blow away the local object
		}
	});

##Making generic calls
If you find that you need to make calls to the API that fall outside of the scope of the Entity and Collection objects, you can use the following format to make any REST calls against the API:

	client.request(options, callback);

This format allows you to make almost any call against the App Services (Usergrid) API. For example, to get a list of users:

	var options = {
		method:'GET',
		endpoint:'users'
	};
	client.request(options, function (err, data) {
		if (err) {
			//Error - GET failed
		} else {
			//data will contain raw results from API call
			//Success - GET worked
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
			//Error - POST failed
		} else {
			//data will contain raw results from API call
			//Success - POST worked
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
			//Error - PUT failed
		} else {
			//data will contain raw results from API call
			//Success - PUT worked
		}
	});

Or, to delete a user:

	var options = {
		method:'DELETE',
		endpoint:'users/fred'
	};
	client.request(options, function (err, data) {
		if (err) {
			//Error - DELETE failed
		} else {
			//data will contain raw results from API call
			//Success - DELETE worked
		}
	});

The Options Object for the client.request fuction:

* `method` - http method (GET, POST, PUT, or DELETE), defaults to GET
* `qs` - object containing querystring values to be appended to the uri
* `body` - object containing entity body for POST and PUT requests
* `endpoint` - API endpoint, for example "users/fred"
* `mQuery` - boolean, set to true if running management query, defaults to false
* `buildCurl` - boolean, set to true if you want to see equivalent curl commands in console.log, defaults to false

You can make any call to the API using the format above.  However, in practice using the higher level Entity and Collection objects will make life easier as they take care of much of the heavy lifting.


###Validation
An extension for validation is provided for you to use in your apps.  The file is located here:

	/extensions/usergrid.session.js

Include this file at the top of your HTML file - AFTER the SDK file:

	<script src="path/to/usergrid.js" type="text/javascript"></script>
	<script src="path/to/extensions/usergrid.validation.js" type="text/javascript"></script>

A variety of functions are provided for verification of many common types such as usernames, passwords, etc.  Feel free to copy and modify these functions for use in your own projects.


###cURL
[cURL](http://curl.haxx.se/) is an excellent way to make calls directly against the API. As mentioned in the **Getting started** section of this guide, one of the parameters you can add to the new client options object is **buildCurl**:

	var client = new Usergrid.Client({
		orgName:'yourorgname',
		appName:'sandbox',
		logging: true, //optional - turn on logging, off by default
		buildCurl: true //optional - turn on curl commands, off by default
	});

If you set this parameter to true, the SDK will build equivalent curl commands and send them to the console.log window. To learn how to see the console log, see this page:

<http://apigee.com/docs/usergrid/content/displaying-app-services-api-calls-curl-commands>

More information on cURL can be found here:

<http://curl.haxx.se/>

## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-node-module), the Usergrid Javascript SDK is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

##More information
For more information on Apigee App Services, visit <http://apigee.com/about/developers>.

## Copyright
Copyright 2013 Apigee Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

