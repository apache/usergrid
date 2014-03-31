/* APIGEE JavaScript SDK ENTITY EXAMPLE APP

This sample app will show you how to perform basic entity operation using the Apigee JavaScript SDK, including:
	
	- creating an entity
	- retrieving an entity
	- updating/altering an entity
	- deleting an entity
	
This file contains the functions that make the actual API requests. To run the app, open index.html in your browser. */

/* Before we make any requests, we prompt the user for their Apigee organization name, then initialize the SDK by
   instantiating the Apigee.Client class. 
   
   Note that this app is designed to use the unsecured 'sandbox' application that was included when you created your organization. */
   
var dataClient;
var entityUuid; //saves the UUID of the entity we create so we can perform retrieve, update and delete operations on it 

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


/* 1. Create a new entity

	To start, let's create a function to create an entity and save it on Apigee. */
	   
function createEntity () {
	/* First, we specify the properties for your new entity:
    
    - The type property associates your entity with a collection. When the entity, 
      is created, if the corresponding collection doesn't exist a new collection 
      will automatically be created to hold any entities of the same type. 
      
      Collection names are the pluralized version of the entity type,
      e.g. all entities of type book will be saved in the books collection. 
    
    - Let's also specify some properties for your entity. Properties are formatted 
      as key-value pairs. We've started you off with three properties in addition 
      to type, but you can add any properties you want.    */

	var properties = {
        type:'book',
        title:'The Old Man and the Sea',
        price: 5.50,
        currency: 'USD'
    };
    
    /* Next, we call the createEntity() method. Notice that the method is prepended 
       by dataClient, so that the Apigee API knows what data store we want to work with. */

    dataClient.createEntity(properties, function (errorStatus, entity, errorMessage) { 
        if (errorStatus) { 
           // Error - there was a problem creating the entity
           document.getElementById('result-text').innerHTML
            +=  "Error! Unable to create your entity. "
            +   "Did you enter the correct organization name?"
            +   "<br/><br/>"
            +   "Error message:" 
            +	"<pre>" + JSON.stringify(errorMessage) + "</pre>";
        } else { 
            // Success - the entity was created properly
            document.getElementById('result-text').innerHTML
            +=  "Success!"
            +	"<br /><br />"
            +	"Here is the UUID (universally unique identifier of the"
            +	"entity you created. We've saved it to reference the entity "
            + 	"when we perform retrieve update and delete operations on it:"
            +	"<br /><br />"
            +   JSON.stringify(entity.get('uuid'))
            +	"<br /><br />"
            + 	"And here is the full API response. The entity is stored in the _data property:"
            +   "<br/><br/>"
            +   "<pre>" + JSON.stringify(entity, undefined, 4) + "</pre>";

           entityUuid = entity._data.uuid; //saving the UUID so it's available for our other operations in this app
        }
    });
}




/* 2. Retrieve an entity

   Now that we can create entities, let's define a function to retrieve them: */
   
function retrieveEntity () {
	/*
	- Specify the 'type' of the entity to be retrieved, 'book' in this case.
	- Specify the 'UUID' property of the entity to be retrieved. You can get this from the 
	  response we showed you when the entity was created. */		              
	var properties = { 
		type:'book',
		uuid:entityUuid
	};
	
	/* Next we pass our properties to getEntity(), which initiates our GET request: */
	dataClient.getEntity(properties, function (errorStatus, entity, errorMessage) { 
		if (errorStatus) { 
		  // Error - there was a problem retrieving the entity
          document.getElementById('result-text').innerHTML
            +=  "Error! Unable to retrieve your entity. "
            +   "Check that the 'uuid' of the entity you tried to retrieve is correct."
            +   "<br/><br/>"
            +   "Error message:" 
            + 	"<pre>" + JSON.stringify(errorMessage); + "</pre>"		                  
		} else { 
		  // Success - the entity was found and retrieved by the Apigee API
		  document.getElementById('result-text').innerHTML
            +=  "Success! Here is the entity we retrieved: "
            +   "<br/><br/>"
            +   "<pre>" + JSON.stringify(entity, undefined, 4); + "</pre>"
		} 
	}); 
}



/* 3. Update/alter an entity

   We can easily add new properties to an entity or change existing properties by making a 
   call to the Apigee API. Let's define a function to add a new property and update an existing 
   property, then display the updated entity. */
   	         
function updateEntity() {
   /*
		   - Specify your Apigee.Client object in the 'client' property. In this case, 'dataClient'.
		   - Specify the following in the 'data' property:
		   		- The 'type' and 'uuid' of the entity to be updated so that the API knows what 
		   		  entity you are trying to update.
		   		- New properties to add to the enitity. In this case, we'll add a property 
		   		  to show whether the book is available.
		   		- New values for existing properties. In this case, we are updating the 'price' property. */
	var properties = {	           		
		client:dataClient,
		data:{
			type:'book',
			uuid: entityUuid,
			price: 4.50, //our new price that will replace the existing value of 5.50
			available: true //new property to be added
		}
	};	
	
	/* We need to create a local Entity object to hold our update */
	entity = new Apigee.Entity(properties);
	
	/* Now we call save() to initiate the API PUT request on our Entity object */
	entity.save(function (errorStatus,entity,errorMessage) {
	
	    if (errorStatus) { 
		  /* Error - there was a problem updating the entity */
          document.getElementById('result-text').innerHTML
            +=  "Error! Unable to update your entity. "
            +   "Check that the or 'uuid' of the entity you tried to retrieve is correct."
            +   "<br/><br/>"
            +   "Error message:" 
            + 	"<pre>" + JSON.stringify(errorMessage); + "</pre>"		                 
		} else { 
		  /* Success - the entity was successfully updated */
		  document.getElementById('result-text').innerHTML
            +=  "Success! Here is the updated entity. Notice that the 'available' and "
            + 	"'price' properties have been added:"
            +   "<br/><br/>"            
            +   "<pre>" + JSON.stringify(entity, undefined, 4); + "</pre>"			  
		} 
	
	});

}



/* 4. Delete an entity

   Now that we've created, retrieved and updated our entity, let's delete it. This will 
   permanently remove the entity from your data store. */
			
function deleteEntity () {

	/* - Specify your Apigee.Client object in the 'client' property. In this case, 'dataClient'.
			   - Specify the 'type' and 'uuid' of the entity to be deleted in the 'data' property so
			     that the API knows what entity you are trying to delete. */
			   		
	var properties = {
		client:dataClient,	        			
		data:{
			type:'book',
			uuid:entityUuid
		}
	};
	
	/* We need to create a local Entity object that we can call destroy() on */
	entity = new Apigee.Entity(properties);
	
	/* Then we call the destroy() method to intitiate the API DELETE request */
	entity.destroy(function (error,response) {
	    if (error) { 
			  // Error - there was a problem deleting the entity
              document.getElementById('result-text').innerHTML
                +=  "Error! Unable to delete your entity. "
                +   "Check that the 'uuid' of the entity you tried to delete is correct."
                +   "<br/><br/>"
                +   "Error message:" + JSON.stringify(error);		                 
			} else { 
			  // Success - the entity was successfully deleted
			  document.getElementById('result-text').innerHTML
                +=  "Success! The entity has been deleted."
		}
	});	     
}