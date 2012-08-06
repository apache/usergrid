#Apigee Usergrid Javascript SDK

#Overview
The Apigee Usergrid Javascript SDK was designed to make…

#Usage

##The Entity Object


##The Collection Object

###Getting Started
To use, simply create a new Collection object, where the argument is the type of collection you intend to model:

	var cars = new Collection('cars'); // for the 'cars' collection

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



##Direct API calls






