/* APIGEE JavaScript SDK GEOLOCATION EXAMPLE APP

This sample app will show you how to perform basic entity operation using the Apigee JavaScript SDK, including:
	
	- Create an entity with location data
	- Query the Apigee API for entities based on location

This file contains the functions that make the actual API requests. To run the app, open index.html in your browser. */

var dataClient;
var latitude;
var longitude;

/* First we use the JavaScript geolocation API to retrieve the user's current position, so that we have position data to work with. */
function getLocation () {
	navigator.geolocation.getCurrentPosition(
		function success (location) {
			latitude = location.coords.latitude;
			longitude = location.coords.latitude;
			alert("Successfully retrieved your location. Click the start button to run the sample app!");
		},	
		function error () {
			document.getElementById('result-text').innerHTML
			 +=	"Unable to retrieve your location. Please ensure location sharing is enabled in your browser."
			$('#back-button').remove();
			window.location = '#result-page';
		}
	);
}

/* Next, before we make any requests, we prompt the user for their Apigee organization name, then initialize the SDK by
   instantiating the Apigee.Client class. 
   
   Note that this app is designed to use the unsecured 'sandbox' application that was included when you created your organization. */

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

/* 1. Add location data to an entity

	To start, let's create a function to create an entity with location data and 
	save it on Apigee. */
	   
function createLocation () {
	/*			
	First, we specify the properties for your new entity:
    
    - Set the values of the 'latitude' and 'longitude' properties to a location near you. */

	var properties = {
        type:'device',
        location: {
        	latitude:latitude,
        	longitude:longitude
		}
    };
    
    /* Next, we call the createEntity() method. Notice that the method is prepended by 
       dataClient, so that the Apigee API knows what data store we want to work with. */

    dataClient.createEntity(properties, function (errorStatus, response, errorMessage) { 
        if (errorStatus) { 
           // Error - there was a problem creating the entity
           document.getElementById('result-text').innerHTML
            +=  "Error! Unable to create your entity. "
            +   "Did you enter the correct organization name?"
            +   "<br/><br/>"
            +   "Error message:" 
            + 	"<pre>" + JSON.stringify(errorMessage); + "</pre>"
        } else { 
            // Success - the entity was created properly
            document.getElementById('result-text').innerHTML
            +=  "Success! The entity has been created. Notice the 'location' property:"
            +	"<br /><br />"
            +   "<pre>" + JSON.stringify(response, undefined, 4); + "</pre>"            
        }
    });
}

/* 2. Query the Apigee API based on entity location

   Now let's the user's location and ask the API to return all entities within 
   16000 meters of them. */
   
function queryLocation () {	
	
	/* Distance to query from the user's current location in meters */
	var distance = '16000';
    
    /* Now we form our geolocation query in the format: 
       "location within <distance> of <latitude>,<longitude>" */
	var properties = { 
		endpoint:'/devices',
		method:'GET',
		qs:{ql:'location within ' + distance + ' of ' + latitude + ',' + longitude}
	};
	
	/* And finally we pass our properties to request(), which initiates our GET request: */
	dataClient.request(properties, function (error, response) { 
		if (error) { 
		  // Error - there was a problem retrieving the entity
          document.getElementById('result-text').innerHTML
            +=  "Error! Unable to retrieve your entity. "
            +   "Did you enter the correct organization name?"
            +   "<br/><br/>"
            +   "Error message:" + JSON.stringify(error);		                 
		} else { 
		  // Success - the request was successful and the API returns the results of our query
		  document.getElementById('result-text').innerHTML
            +=  "Success! Here is the entity we retrieved. If you don't see an entity, make "
            + 	"sure you run the 'Create a new entity with location data step' first."
            +   "<br/><br/>"
            +   "<pre>" + JSON.stringify(response, undefined, 4); + "</pre>"
		} 
	}); 
}
