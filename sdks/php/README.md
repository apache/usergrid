##Quickstart
Detailed instructions follow but if you just want a quick example of how to get started with this SDK, use this minimal PHP file to show you how to include & initialize the SDK, as well as how to read & write data from Usergrid with it.

The file is located in this repository:

	https://github.com/usergrid/usergrid/blob/master/sdks/php/examples/quick_start/index.php
	

```html
<?php
//include autoloader to make sure all files are included
include '../autoloader.inc.php';

//initialize the SDK
$client = new Apache\Usergrid\Client('yourorgname','sandbox');

//reading data
$books = $client->get_collection('books');
//do something with the data
while ($books->has_next_entity()) {
	$book = $books->get_next_entity();
	$title = $book->get('title');
	echo "Next Book's title is: " . $title . "<br>";
}

//writing data
$data = array('title' => 'the old man and the sea', 'type' => 'books');
$book = $books->add_entity($data);
if ($book == FALSE) {
	echo 'write failed';
} else {
	echo 'write succeeded';
}
?>
```

##Version

Current Version: **0.0.1**

See change log:

<https://github.com/usergrid/usergrid/blob/master/sdks/php/changelog.md>

**About this version:**

This is the initial release of this SDK. It is functionally complete with some test coverage.  I plan to add additional test coverage in the near future as well as more examples.  

This SDK is open source, and we welcome any contributions!  

##Comments / Questions
Please feel free to send comments or questions to the various
community communication channels
<http://usergrid.apache.org/community/>

##Overview
This open source SDK simplifies writing PHP applications that connect to App Services. The repo is located here:

<https://github.com/usergrid/usergrid/tree/master/sdks/php>

You can download the release artfacts via the Apache Mirroring newtwork.

A wealth of Usergrid documentation can be found on the site

<http://usergrid.apache.org/docs/>


##About the samples
This SDK comes with several samples to get you started.  They are located in this repo here:

<https://github.com/usergrid/usergrid/tree/master/sdks/php/examples>

The quick start shows how to get a basic file constructed.  The tests directory has the test code that is used to create this readme file.  **Note:** Ignore the //@han and //@solo comments - they are delimiters for the code parsing app we use to generate the readme.

#Installing
Download this repo, then add it to the root of your PHP project.

#Getting started
Once you have the the PHP SDK included in your directory tree, get started by including the SDK:

	 include '../autoloader.inc.php';
	 usergrid_autoload('Apache\\Usergrid\\Client');

Then, start the client, which is the main object for connecting to the SDK.

	 $client = new Apache\Usergrid\Client('1hotrod','sandbox');

Once you have a client created, you will be able to use it to create Entities and Collections, the building blocks of the Usergrid API.

##Entities
This sdk provides an easy way to make new entities. Here is a simple example that shows how to create a new object of type "dogs":

	 $data = array('name' => 'Dino', 'type' => 'dog');
	 $dog = $client->create_entity($data);
	 if ($dog) {
	 	//once you have your entity, use the get() method to retrieve properties
	 	$name = $dog->get('name');
	 } else {
	 	//there was an error creating / retrieving the entity
	 }
	
**Note:** The above method will automatically get the entity if it already exists on the server.  

If you only want to **get** an entity from the server: 

	 $data = array('name' => 'Dino', 'type' => 'dog');
	 $dog = $client->get_entity($data);
	 if ($dog) {
	 	$name = $dog->get('name');
	 } else {
	 	//entity doesn't exist on the server
	 }
	
**Note:** The above method will return **false** if the entity does not exist, and will **not** automatically create a new entity.


You can also refresh the object from the database if needed (in case the data has been updated by a different client or device), by calling the fetch() method:

	 $dog->fetch();

To set properties on the entity, use the set() method:

	 $dog->set('master', 'Fred');
	 $dog->set('breed', 'dinosaur');
	 $dog->set('color', 'purple');

These properties are now set locally, but make sure you call the save() method on the entity to save them back to the database!

	 $result = $dog->save();
	 if (!$result->get_error()) {
	 	//all is well
	 } else {
	 	//there was a problem!
	 }

To get a single property from the entity, use the get method:



To remove the entity from the database:

	 $result = $dog->destroy();


##The Collection object
The Collection object models Collections in the database.  Once you start programming your app, you will likely find that this is the most useful method of interacting with the database.  

**Note:** Collections are automatically created when entities are added.  So you can use the get_collection method even if there are no entities in your collection yet!

Getting a collection will automatically populate the object with entities from the collection. The following example shows how to create a Collection object, then how to use entities once the Collection has been populated with entities from the server:

	 $dogs = $client->get_collection('dogs');


You can also add a new entity of the same type to the collection:

	 $data = array('name' => 'Dino', 'type' => 'dogs');
	 $dino = $dogs->add_entity($data);

**Note:** this will also add the entity to the local collection object as well as creating it on the server


##Collection iteration and paging
The Collection object works in Pages of data.  This means that at any given time, the Collection object will have one page of data loaded.  You can iterate across all the entities in the current page of data by using the following pattern:

	 while ($dogs->has_next_entity()) {
	 	$dog = $dogs->get_next_entity();
	 	//do something with dog
	 	$name = $dog->get('name');
	 }

To iterate over the collection again, you must reset the entity pointer:

	 $dogs->reset_entity_pointer();
	 while ($dogs->has_next_entity()) {
	 	$dog = $dogs->get_next_entity();
	 	//do something with dog
	 	$name = $dog->get('name');
	 }
	
To get the next page of data:

	 $dogs->get_next_page();
	 while ($dogs->has_next_entity()) {
	 	$dog = $dogs->get_next_entity();
	 	$name = $dog->get('name');
	 }

You can use the same pattern to get a previous page of data:

	 $dogs->get_prev_page();
	 while ($dogs->has_next_entity()) {
	 	$dog = $dogs->get_next_entity();
	 	$name = $dog->get('name');
	 }

To get all the subsequent pages of data from the server and iterate across them, use the following pattern:

	 while ($dogs->get_next_page()) {
	 	while ($dogs->has_next_entity()) {
	 		$dog = $dogs->get_next_entity();
	 		$name = $dog->get('name');
	 	}
	 }


By default, the database will return 10 entities per page.  You can change that amount by setting a limit:

	 $data = array('ql'=>'select * where created > 0', 'limit'=>'40');
	 $dogs = $client->get_collection('dogs', $data);

Several other convenience methods exist to make working with pages of data easier:

	* get_first_entity - gets the first entity of a page
	* get_last_entity - gets the last entity of a page
	* reset_entity_pointer - sets the internal pointer back to the first element of the page
	* get_entity_by_uuid - returns the entity if it is in the current page



##Modeling users with the Entity object
There is no specific User object in this SDK.  Instead, you simply need to use the Entity object, specifying a type of "users".  Here is an example:

	 $marty =  $client->signup('marty', 'mysecurepassword','marty@timetravel.com', 'Marty McFly');
	 if ($marty) {
	 	//user created
	 } else {
	 	//there was an error
	 }

If the user is modified, just call save on the user again:

	 $marty->set('state', 'California');
	 $marty->set('girlfriend', 'Jennifer');
	 $result = $marty->save();

To refresh the user's information in the database:



To change the user's password:
	
	 $marty->set('oldpassword', 'mysecurepassword');
	 $marty->set('newpassword', 'mynewsecurepassword');
	 $marty->save();

To log a user in:

	 if ($client->login('marty', 'mysecurepassword')) {
	 	//the login call will return an OAuth token, which is saved
	 	//in the client. Any calls made now will use the token.
	 	//once a user has logged in, their user object is stored
	 	//in the client and you can access it this way:
	 	$token = $client->get_oauth_token();
	 } else {
	 }

To log a user out:

	 $client->log_out();
	 if ($client->is_logged_in()) {
	 	//error - user is still logged in
	 } else {
	 	//success - user was logged out
	 }

If you no longer need the object, call the destroy() method and the object will be removed from database:

	 $result = $marty->destroy();
	 if ($result->get_error()) {
	 	//there was an error deleting the user
	 } else {
	 	//success - user was deleted
	 }
	

##Making generic calls
If you find that you need to make calls to the API that fall outside of the scope of the Entity and Collection objects, you can use the following methods to make any of the REST calls (GET, POST, PUT, DELETE).

GET:

	 $endpoint = 'users/fred';
	 $query_string = array();
	 $result = $client->get($endpoint, $query_string);
	 if ($result->get_error()){
	 	//error - there was a problem getting the entity
	 } else {
	 	//success - entity retrieved
	 }

POST:

	 $endpoint = 'users';
	 $query_string = array();
	 $body = array('username'=>'fred');
	 $result = $client->post($endpoint, $query_string, $body);
	 if ($result->get_error()){
	 	//error - there was a problem creating the entity
	 } else {
	 	//success - entity created
	 }
	
PUT:

	 $endpoint = 'users/fred';
	 $query_string = array();
	 $body = array('dog'=>'dino');
	 $result = $client->put($endpoint, $query_string, $body);
	 if ($result->get_error()){
	 	//error - there was a problem updating the entity
	 } else {
	 	//success - entity updated
	 }
	
DELETE:

	 $endpoint = 'users/idontexist';
	 $query_string = array();
	 try {
	 	$result =  $client->delete($endpoint, $query_string);
	 } catch (Exception $e) {
	 	//entity didn't exist on the server, so UG_404_NotFound is thrown
	 }
	 
	 ?>
	 
	 ?>
	 $endpoint = 'users/fred';
	 $query_string = array();
	 $result =  $client->delete($endpoint, $query_string);
	 if ($result->get_error()){
	 	//error - there was a problem deleting the entity
	 } else {
	 	//success - entity deleted
	 }


You can make any call to the API using the format above.  However, in practice using the higher level Entity and Collection objects will make life easier as they take care of much of the heavy lifting.

#Authentication
By default, every App Services account comes with an app called "Sandbox".  While using the Sandbox app for testing, you will not need to worry about authentication as it has all permissions enabled for unauthenticated users.  However, when you are ready to create your own secured app, there are two authentication methods you will use: app user credentials, where your users sign in to get a token, and system level credentials (a client id / client secret combo).

#App user credentials
To get an Oauth token, users will sign into their accounts with a username and password.  See the "Modeling users with the Entity object" section above for details on how to use the login method.  Once logged in, the user's Oauth token is stored and used for all subsequent calls.

The SDK uses app level credentials by default.


#System level credentials (client id / client secret combo)
If your app needs an elevated level of permissions, then you can use the client id / client secret combo. This is useful if you need to update permissions, do user manipulation, etc. Since the PHP sdk is running server-side, it is safe for you to use the client id / client secret combo on your API calls.  Keep in mind that if you are making API calls on behalf of your application user, say to get resources only they should have access to, such as a feed, it may be more appropriate to use the app user credentials described above.

To use make calls using system level credentials, simply set the auth type and specify your client id and client secret:

	 $testname = 'Use Client Auth type - ';
	 $client->set_auth_type(AUTH_CLIENT_ID);
	 $client->set_client_id('YXA6Us6Rg0b0EeKuRALoGsWhew');
	 $client->set_client_secret('YXA6OYsTy6ENwwnbvjtvsbLpjw5mF40');


## Contributing
We welcome your enhancements!

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

##More information
For more information on Usergrid, visit <http://usergrid.apache.org>.

## Copyright
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


