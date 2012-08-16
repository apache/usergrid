#Apigee Usergrid Javascript SDK

#Overview
The Apigee Usergrid Javascript SDK was designed to make…

#How to use
This SDK comes 

#Entities and Collecitons
Entities and Collections are used to model the custom data you need to store in your app.  To facilitate using these in your app, the Javascript SDK provides the Entity and the Collection objects. 

##The Entity Object
###Getting Started
To use, simply create a new Entity object, where the argument is the type of collection the entity will be a part of:

	var timeMachine = new apigee.Entity("cars");
	
Next add any custom fields needed:

 	timeMachine.setField("model","DeLorean");
 	timeMachine.setField("license-plate","OUTATIME");
 	timeMachine.setField("date","November 5, 1955");

Once the object is complete, save it back to the API:
 
  	timeMachine.save();
 
When the entity is saved, the API gives it a UUID that uniquely identifies the Entity in the database.  This UUID is stored in our Entity object and will be used for any future calls to the API.  This ensures that the correct entity is updated.  For example, if the object was updated and needed to be saved again:

	timeMachine.setField("date", "July 3, 1985");
	timeMachine.save();
 
Or, if the entity was changed in the database (perhaps by another user of your app), and needed to be refreshed:

	timeMachine.get(); //will only work if we have a UUID
 
In this way, multiple clients can update the same object in the database.

If you need to get a property from the object, do this:
 
	var date = timeMachine.getField("date");
 
If you don't need the object anymore, simply call the destroy method and it will be deleted from database:
 
	timeMachine.delete();
 
The object is now deleted from the database, although it remains in your program.  Destroy it if needed by calling:
 
 timeMachine = null;

##The Collection Object

###Getting Started
To use, simply create a new Collection object, where the argument is the type of collection you intend to model:

	var cars = new apigee.Collection('cars'); //makes a new 'cars' collection object

Once the collection is created, you can call the "get" method. By default, this will pull the first 10 entities from the API and will load them into the Collection object.

	cars.get();

###Displaying Results
You can display a list of all the cars in the Collection object by using the following code snipit:
 
	while(cars.hasNextEntity()) {
		var item = cars.getNextEntity();
		$('#mycarlist').append('<li>'+ item.getName() + '</li>');
	}

Remember, this snipit will only loop through the items currently stored in the Collection object.  If there are more entities in the database that you want to display, either use Paging (described below), or use a custom query. 

###Custom Queries
A custom query allows you to tell the API you want your results filtered / altered in some way.  In our case, we want more entities in our result set (the default is 10).  This method is useful in some cases, but be careful because the more results you get back in a single call, the slower the call will be:

	cars.setQueryParams({'ql':'','limit':'100'}); //up to 999

###Collection Paging
An easy way to move through your result set is to use paging.  The Collection object provides a facility for this via the Query Object.

#The User Object

 <pre>
 *  //first create a new user:
 *  var marty = new User("fred"); //<==argument is username)
 *  //next add more data if needed:
 *  marty.setName("Marty McFly");
 *  marty.setField("City", "Hill Valley");
 *  marty.setField("State", "California");
 *  //finally, create the user in the database:
 *  marty.create();
 *  //if the user is updated:
 *  marty.setField("girlfriend","Jennifer");
 *  //call save on the user:
 *  marty.save();
 *
 *  To refresh the user's info from the database:
 *  marty.get();
 *
 *  //to get properties from the user object:
 *  var city = marty.getField("city");
 *
 *  If you don't need the object anymore, simply call the destroy
 *  method and it will be deleted from database:
 *
 *  marty.delete();
 *
 *  //the object is now deleted from the database, although it remains
 *  //in your program.  Destroy it if needed by calling:
 *
 *  marty = null;
 *
 *  </pre>

#Direct API calls
For most purposes, some combination of Entity and Collection will likely suffice.  However, there are times when one must make a direct call to the API.  The following sections describe how to do this against the Application endpoint as well as the Management endpoint.

##The Query object
The Query object is a container for the information 

 The goal of the query object is to make it easy to run any
 *  kind of CRUD call against the API.  This is done as follows:
 *
 *  1. Create a query object:
 *     Query = new Usergrid.Query("GET", "users", null, function() { alert("success"); }, function() { alert("failure"); });
 *
 *  2. Run the query by calling the appropriate endpoint call
 *     runAppQuery(Query);
 *     or
 *     runManagementQuery(Query);
 *
 *  3. Paging - The Usergrid.Query holds the cursor information.  To
 *     use, simply bind click events to functions that call the
 *     getNext and getPrevious methods of the query object.  This
 *     will set the cursor correctly, and the runAppQuery method
 *     can be called again using the same Usergrid.Query:
 *     runAppQuery(Query);
 *
 *  @class Usergrid.Query
 *  @param method REQUIRED - GET, POST, PUT, DELETE
 *  @param path REQUIRED - API resource (e.g. "users" or "users/rod", should not include http URL or org_name/app_name)
 *  @param jsonObj NULLABLE - a json data object to be passed to the API
 *  @param params NULLABLE - query parameters to be encoded and added to the API URL
 *  @param {Function} successCallback function called with response: <pre>
 *  {
 *    alert('Hurray! Everything worked.');
 *  }
 *  @param {Function} failureCallback function called with response if available: <pre>
 *  {
 *    alert('An error occured');
 *  }
 *  </pre>
 *

##The ApiClient singleton




