/* APIGEE JavaScript SDK COLLECTION EXAMPLE APP

This sample app will show you how to perform basic operations on collections using the Apigee JavaScript SDK, including:
	
	- creating an empty collection
	- adding one or more entities to a collection
	- retrieving and paging through a collection
	- deleting entities from a collection
	
This file contains the functions that make the actual API requests. To run the app, open index.html in your browser. */

/* Before we make any requests, we prompt the user for their Apigee organization name, then initialize the SDK by
   instantiating the Apigee.Client class. 
   
   Note that this app is designed to use the unsecured 'sandbox' application that was included when you created your organization. */
   
var dataClient;

function promptClientCredsAndInitializeSDK(){
	var APIGEE_ORGNAME;
	var APIGEE_APPNAME='sandbox';
	if("undefined"===typeof APIGEE_ORGNAME){
	    APIGEE_ORGNAME=prompt("What is the Organization Name you registered at http://apigee.com/usergrid?");
	}
	initializeSDK(APIGEE_ORGNAME,APIGEE_APPNAME);
}            


function initializeSDK(ORGNAME,APPNAME){	
	dataClient = new Apigee.Client({
	    orgName:ORGNAME,
	    appName:APPNAME,
		logging: true, //optional - turn on logging, off by default
		buildCurl: true //optional - log network calls in the console, off by default
	
	});	
}


/* 1. Create an empty collection

	To start, let's declare a function to create an empty collection in Apigee for us to work with: */
	   
function createCollection () {
	/*			
	First, we specify the properties for your new collection:
    
    -The endpoint from 'books' will be the name of your collection, which will
     hold any entities of that type. 
      
     Collection names are the pluralized version of the entity type, e.g. all entities of type 
     book will be saved in the books collection. */

	var properties = {
        endpoint:'books',
        method:'POST' //HTTP method. Since we are creating an entity, we use 'POST'. 
    };
    
    /* Next, we call the request() method. Notice that the method is prepended by dataClient, 
       so that the Apigee API knows what data store we want to work with. */

    dataClient.request(properties, function (error, response) { 
        if (error) { 
           // Error - there was a problem creating the collection
           document.getElementById('result-text').innerHTML
            +=  "Error! Unable to create your collection. "
            +   "Did you enter the correct organization name?"
            +   "<br/><br/>"
            +   "Error message:"
            +	"<pre>" + JSON.stringify(error) + "</pre>";
        } else { 
            // Success - the collection was created properly
            document.getElementById('result-text').innerHTML
            +=  "Success!"
            +	"<br /><br />"
            +	"Here is the response from the Apigee API. By the way, you "
            +   "don't have to create an empty collection. Creating a new entity "
            + 	"will automatically create the corresponding collection for you."
            +	"<br /><br />"
            +   "<pre>" + JSON.stringify(response, undefined, 4) + "</pre>";
        }
    });
}



/* 2. Add entities to a collection

   Now we'll add some entities to our collection so that we have data to work with. */
   
function addEntity () {
   
   /* We start by creating a local colleciton object to work with */				   				   
   var options = {
	   client:dataClient, //our Apigee.Client object
	   type:'book' //the entity type associated with our collection
   }				   
   collection = new Apigee.Collection(options); //this creates our local object
   
   /* Next, we define the entity we want to add */				   				   
   var properties = {
	   title:'A Moveable Feast'
   }
 
   /* And now we call addEntity() to add the entity to our collection! */				   
   collection.addEntity(properties, function (error, response){
	  if (error) {
		  // Error - there was a problem adding the entity
          document.getElementById('result-text').innerHTML
          +=  "Error! Unable to add your entity. "
          +   "<br/><br/>"
          +   "Error message:" 
          +	  "<pre>" + JSON.stringify(error); + "</pre>"
	  } else {
		  // Success - the entity was created properly
          document.getElementById('result-text').innerHTML
          +=  "Success! A single entity has been added to your collection:"
          +	  "<br /><br />"
          +	  "<pre>" + JSON.stringify(response._data, undefined, 4); + "</pre>"
          +	  "<br /><br />"
          +	  "and..."
          
          	/* We can also add multiple entities at once, using the request() method. 
			   To do this, we create a JSON array with all the entities we want to add.*/
			var entityArray = [
			   {
				   type:'book', //the entity type should correspond to the collection type
				   title:'For Whom the Bell Tolls'
			   },
			   {
				   type:'book',
				   title:'The Old Man and the Sea'
			   },
			   {
				   type:'book',
				   title:'A Farewell to Arms'
			   },
			   {
				   type:'book',
				   title:'The Sun Also Rises'
			   }
			]
			
			/* Next we call request() to initiate the POST request, and pass our entity
			   array in the 'body' property of our request properties*/
			var properties = {
				endpoint:'books',
				method:'POST',
				body:entityArray
			}
								
			dataClient.request(properties, function (error, response) { 
		        if (error) { 
		           // Error - there was a problem adding the array
		           document.getElementById('result-text').innerHTML
		            +=  "Error! Unable to add your entity array. "
		            +   "<br/><br/>"
		            +   "Error message:"
		            +	"<pre>" + JSON.stringify(error); + "</pre>"
		        } else { 
		            // Success - the entities were created properly
		            document.getElementById('result-text').innerHTML
		            +=	"<br /><br />"
		            +   "So has your array. We added this array three times, so that there's "
		            + 	"plenty of data in your collection to work with when we look at paging next."
		            +	"<br /><br />"
					+	"<pre>" + JSON.stringify(response.entities, undefined, 4); + "</pre>"
		        }
		    });		          
		}
	});
    
    /* We're going to add the entity array a couple more times so that we have a
       good amount of data to work with when we look at paging later. */
    for (i=1;i<=2;i++) {
        dataClient.request(properties, function (error, response) { 
            if (error) { 
               //Error
            } else { 
                //Success
            }
        });
	}
}



/* 3. Retrieve a collection

   Now that we have data in our collection, let's declare a function to retrieve it: */
   
function retrieveCollection () {
	
	/* We start by creating a local colleciton object to work with */				   				   
	var options = {
	   client:dataClient, //our Apigee.Client object
	   type:'books' //the entity type associated with our collection
	}
	
	collection = new Apigee.Collection(options); //this creates our local object
	
	/* Next we call fetch(), which initiates our GET request to the API: */
	collection.fetch(function (error, response) { 
		if (error) { 
		  // Error - there was a problem retrieving the collection
          document.getElementById('result-text').innerHTML
            +=  "Error! Unable to retrieve your collection. "
            + 	"<br /><br />"
            +   "Check that the 'type' of the collection is correct."
            +   "<br/><br/>"
            +   "Error message:" + JSON.stringify(error);		                 
		} else { 
		  // Success - the collection was found and retrieved by the Apigee API
		  document.getElementById('result-text').innerHTML
            +=  "Success! Here is the collection we retrieved. Notice that only "
            +  	"the first 10 entities in the collection are retrieved. "
            +	"We'll show you how to page through to get more results later."
            +   "<br/><br/>"
            +   "<pre>" + JSON.stringify(response, undefined, 4); + "</pre>"
		} 
	});
	
}



/* 4. Using cursors (paging through a collection)

   By default, the Apigee API only returns the first 10 entities in a collection. 
   This is why our retrieveCollection() function only gave us back the first 
   10 entities in our collection.
   
   To get the next 10 results, we send a new GET request that references the 
   'cursor' property of the previous response by using the hasNextPage() and 
   getNextPage() methods. */
   	         
function pageCollection() {

	/* We start by creating a local colleciton object to work with */				   				   
	var options = {
	   client:dataClient, //our Apigee.Client object
	   type:'book' //the entity type associated with our collection
	}
	
	collection = new Apigee.Collection(options); //this creates our local object				
	
	/* Then we call fetch, just like in our retrieveCollection() function above */
	collection.fetch(function (error, response) { 					
	    if (error) { 
		  // Error - there was a problem retrieving the collection
          document.getElementById('result-text').innerHTML
            +=  "Error! Unable to retrieve your collection. "
            + 	"<br /><br />"	                        
            +   "Check that the 'type' of the collection is correct."
            +   "<br/><br/>"
            +   "Error message:" 
            +	"<pre>" + JSON.stringify(error); + "</pre>"		                 
		} else { 
		  /* Success - the collection was found and retrieved by the Apigee API, so we use 
		     hasNextPage() to check if the 'cursor' property exists. If it does, we use 
		     getNextPage() to initiate another GET and retrieve the next 10 entities. */
			 if (collection.hasNextPage()) {
				 collection.getNextPage(function (error, response){
					if (error) { 
					  // Error - there was a problem retrieving the next set of entities
	                  document.getElementById('result-text').innerHTML
                        +=  "Error! Unable to retrieve the next result set. "
                        +   "<br/><br/>"
                        +   "Error message:" 
                        +	"<pre>" + JSON.stringify(error); + "</pre>"		                 
					} else { 
					  // Success - the next set of entities in the collection was found
					  document.getElementById('result-text').innerHTML
                        +=  "Success! Here are the next 10 entities in our collection."
                        +   "<br/><br/>"
                        +   "<pre>" + JSON.stringify(response, undefined, 4); + "</pre>"
					}	 
				 });
			 } else {
			 	//if hasNextPage() returns false, there was no cursor in the last response
				document.getElementById('result-text').innerHTML
					+= "There are no more entities in this collection."
			 }
		}
	});
	
	/* You can also use these other useful SDK methods for paging through results:
	   - hasPreviousPage() and getPreviousPage() page through the collection in reverse
	   - hasNextEntity() and getNextEntity() iterate through entities in the current result set. */

}


/* 5. Delete a collection

   At this time, it is not possible to delete a collection, but you can delete entities from a 
   collection, including performing batch deletes. Please be aware that removing entities from 
   a collection will delete the entities. */
			
function deleteCollection () {

	/* Let's start by batch deleting the first 5 entities in our collection. To do this, we specify 
	   a query string with the 'qs' property in the format {ql:<query string>}. 
	   
	   In this case, by specifying limit=5 we ask the API to only return the first 5 entities in 
	   the collection, rather than the default 10. */			

	var options = {
		method:'DELETE',
		endpoint:'books',
		qs:{ql:'limit=5'}
	}
		
	/* Next, we call the request() method. Notice that the method is prepended by dataClient, 
       so that the Apigee API knows what data store we want to work with. */				
    dataClient.request(options, function (error, response) { 
        if (error) { 
           // Error - there was a problem deleting the collection
           document.getElementById('result-text').innerHTML
            +=  "Error! Unable to delete your collection. "
            +   "Did you enter the correct organization name?"
            +   "<br/><br/>"
            +   "Error message:" 
            +	"<pre>" + JSON.stringify(error); + "</pre>"
        } else { 
            // Success - all entities in the collection were deleted
            document.getElementById('result-text').innerHTML
            +=  "Success! The first 5 entities in your collection were deleted."
            +	"<br /><br />"
            +	"and..."
        }
    });
    
    /* We can also delete specific entities from our collection using a query string. 
       In this example, we'll delete all entities with the title 'For Whom the Bell Tolls'. */
    
    var options = {
    	method:'DELETE',
		endpoint:'books',
		qs:{ql:"title='For Whom the Bell Tolls'"}
	}

    dataClient.request(options, function (error, response) { 
        if (error) { 
           // Error - there was a problem deleting the entities
           document.getElementById('result-text').innerHTML
            +=  "Error! Unable to delete your collection. "
            +   "Did you enter the correct organization name?"
            +   "<br/><br/>"
            +   "Error message:" + JSON.stringify(error);
        } else { 
            // Success - entities were deleted
            document.getElementById('result-text').innerHTML
            +=  "<br /><br />"
            + 	"All entities with title 'For Whom the Bell Tolls' have been "
            +	"deleted from your collection."
        }
    });
    
}
