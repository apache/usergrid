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



#Direct API calls
For most purposes, some combination of Entity and Collection will likely suffice.  However, there are times when one must make a direct call to the API.  The following sections describe how to do this against the Application endpoint as well as the Management endpoint.

##The Query object
The Query object is a container for the information 



##The ApiClient singleton




