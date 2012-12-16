/**
* Test suite for all the examples in the readme
* 
* NOTE: No, this test suite doesn't use the traditional format for
* a mocha test suite.  This is because the goal is to require as little
* alteration as possible during the copy / paste operation from this test
* suite to the readme file.
* 
* Run with mocha v. 1.7.x
* http://visionmedia.github.com/mocha/
* 
* @author rod simpson (rod@apigee.com)
*/


var usergrid = require('../lib/usergrid');

var logSuccess = false;
var successCount = 0;
var logError = false;
var errorCount = 0;
var logNotice = false;

	var client = new usergrid.client({ 
		orgName:'yourorgname', 
		appName:'yourappname', 
		authType:usergrid.AUTH_CLIENT_ID, 
		clientId:'YXA6Us6Rg0b0EeKuRALoGsWhew', 
		clientSecret:'YXA6OYsTy6ENwwnbvjtvsbLpjw5mF40',
		logging: false, //optional - turn on logging, off by default
		buildCurl: false //optional - turn on curl commands, off by default
	});

	var client = new usergrid.client({ 
		orgName:'yourorgname', 
		appName:'sandbox',
		logging: true, //optional - turn on logging, off by default
	});


//call the runner function to start the process
runner(0);

function runner(step, arg){
	step++;
	switch(step)
	{
	case 1:
		console.log('-----running step 1: GET test');
		testGET(step);
		break;
	case 2:
		console.log('-----running step 2: POST test');
		testPOST(step);
		break;
	case 3:
		console.log('-----running step 3: PUT test');
		testPUT(step);
		break;
	case 4:
		console.log('-----running step 4: DELETE test');
		testDELETE(step);
		break;
	case 5:
		console.log('-----running step 5: make a new dog');
		makeNewDog(step);
		break;
	case 6:
		console.log('-----running step 6: update our dog');
		updateDog(step, arg);
		break;
	case 7:
		console.log('-----running step 7: refresh our dog');
		refreshDog(step, arg);
		break;
	case 8:
		console.log('-----running step 8: remove our dog from database (no real dogs harmed here!!)');
		removeDogFromDatabase(step, arg);
		break;
	case 9:
		console.log('-----running step 9: make lots of dogs!');
		makeSampleData(step, arg);
		break;
	case 10:
		console.log('-----running step 10: make a dogs collection and show each dog');
		testDogsCollection(step);
		break;
	case 11:
		console.log('-----running step 11: get the next page of the dogs collection and show each dog');
		getNextDogsPage(step, arg);
		break;
	case 12:
		console.log('-----running step 12: get the previous page of the dogs collection and show each dog');
		getPreviousDogsPage(step, arg);
		break;
	case 13:
		console.log('-----running step 13: remove all dogs from the database (no real dogs harmed here!!)');
		cleanupAllDogs(step);
		break;
	case 14:
		console.log('-----running step 14: create a new user');
		createUser(step);
		break;
	case 15:
		console.log('-----running step 15: update the user');
		updateUser(step, arg);
		break;
	case 16:
		console.log('-----running step 16: refresh the user from the database');
		refreshUser(step, arg);
		break;
	case 17:
		console.log('-----running step 17: refresh the user from the database');
		loginUser(step, arg);
		break;
	case 18:
		console.log('-----running step 18: remove the user from the database');
		destroyUser(step, arg);
		break;
	default:
		console.log('-----test complete!-----');
		console.log('Success count= ' + successCount);
		console.log('Error count= ' + errorCount);
		console.log('-----thank you for playing!-----');
	}
}

//logging functions
function success(message){
	successCount++;
	if (logSuccess) {
		console.log('SUCCESS: ' + message);
	}
}

function error(message){
	errorCount++
	if (logError) {
		console.log('ERROR: ' + message);
	}
}

function notice(message){
	if (logNotice) {
		console.log('NOTICE: ' + message);
	}
}

//tests
function testGET(step) {
	var options = {
		method:'GET', 
		endpoint:'users'
	};
	client.request(options, function (err, data) {
		if (err) {
			error('GET failed');
		} else {
			//data will contain raw results from API call
			success('GET worked');
			runner(step);
		}
	});
}

function testPOST(step) {
	var options = {
		method:'POST', 
		endpoint:'users', 
		body:{ username:'fred', password:'secret' }
	};
	client.request(options, function (err, data) {
		if (err) {
			error('POST failed');
		} else {
			//data will contain raw results from API call
			success('POST worked');
			runner(step);
		}
	});
}

function testPUT(step) {
	var options = {
		method:'PUT', 
		endpoint:'users/fred', 
		body:{ newkey:'newvalue' }
	};
	client.request(options, function (err, data) {
		if (err) {
			error('PUT failed');
		} else {
			//data will contain raw results from API call
			success('PUT worked');
			runner(step);
		}
	});
}

function testDELETE(step) {
	var options = {  
		method:'DELETE', 
		endpoint:'users/fred'
	};
	client.request(options, function (err, data) {
		if (err) {
			error('DELETE failed');
		} else {
			//data will contain raw results from API call
			success('DELETE worked');
			runner(step);
		}    
	});
}

function makeNewDog(step) {
	
	var options = {
		client:client,
		type:'dogs'					
	}				
	dog = new usergrid.entity(options);
	
	dog.set('name','Dino');
	var data = {
			master:'Fred',
			state:'hungry'
	}
	
	dog.set(data); //set is cumulative
	
	dog.save(function(err){
		if (err){
			error('dog not saved');
		} else {
			success('new dog is saved');
			runner(step, dog);
		}
	});
}

function updateDog(step, dog) {
	dog.set("state", "fed");
	dog.save(function(err){
		if (err){
			error('dog not saved');
		} else {
			success('dog is saved');
			runner(step, dog);
		}
	});
}

function refreshDog(step, dog){
	dog.fetch(function(err){
		if (err){
			error('dog not refreshed from database');
		} else {
			//dog has been refreshed from the database
			//will only work if the UUID for the entity is in the dog object
			success('dog entity refreshed from database');
			var state = dog.get("state");
			notice('dog state: ' + state);
			runner(step, dog);
		}
	}); 
}

function removeDogFromDatabase(step, dog){
	dog.destroy(function(err){
		if (err){
			error('dog not removed from database');
		} else {
			success('dog removed from database'); // no real dogs were harmed!
			dog = null; //no real dogs were harmed!
			runner(step, 1);
		}
	});		
}

function makeSampleData(step, i) {
	notice('making dog '+i);
	var options = {
		client:client,
		type:'dogs',
		data:{'name':'dog'+i, index:i}				
	}	
	dog = new usergrid.entity(options);
	dog.save(function(err, data) {
		if (err) {
			error('sample data not loaded on ');
		} else {
			if (i >= 30) {
				//data made, ready to go
				success('dog ' + i + ' made');
				runner(step);
			} else {
				success('all dogs made');
				//keep making dogs
				makeSampleData(step, ++i);
			}
		}
	});		
}

function testDogsCollection(step) {
	
	var options = {
		type:'dogs',
		qs:{ql:'order by index'},
		client:client
	}
	dogs = new usergrid.collection(options, function(err) {  
		if (err) { 
			error('could not make collection'); 
		} else {
	  
			success('new Collection worked');
			
			//we got the dogs, now display the Entities:
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				dog = dogs.getNextEntity();
				var name = dog.get('name');
				notice('dog is called ' + name);
			}
			
			success('looped through dogs');
			
			//do more things with the collection 	 
			var options = {
				type:'dogs',
				data:{name:'extra-dog'},
				client:client
			}
			dog = new usergrid.entity(options);

			dogs.addEntity(dog, function(err, data) {
				if (err) {
					error('extra dog not saved or added to collection');
				} else {
					success('extra dog saved and added to collection');
					runner(step, dogs);
				}
			}); 

			
		}
	});
}

function getNextDogsPage(step, dogs) {
	//we got the dogs, now display the Entities:

	if (dogs.hasNextPage()) {
		dogs.getNextPage(function(err){
			if (err) {
				error('could not get next page of dogs');
			} else {
				
				success('got next page of dogs');
				
				//we got the next page of data, so do something with it:
				var i = 11;
				while(dogs.hasNextEntity()) {
					//get a reference to the dog
					var dog = dogs.getNextEntity();
					var index = dog.get('index');
					if(i !== index) {
						error('wrong dog loaded: wanted' + i + ', got ' + index);
					}
					notice('got dog ' + i);
					i++
				}
				
				success('looped through dogs');
				
				runner(step, dogs);
			}
		});
	} else {
		getPreviousDogsPage(dogs);
	}

}

function getPreviousDogsPage(step, dogs) {

	if (dogs.hasPreviousPage()) {
		dogs.getPreviousPage(function(err){
			if(err) {
				error('could not get previous page of dogs');
			} else {
				
				success('got next page of dogs');
				
				//we got the previous page of data, so do something with it:
				var i = 1;
				while(dogs.hasNextEntity()) {
					//get a reference to the dog
					var dog = dogs.getNextEntity();
					var index = dog.get('index');
					if(i !== index) {
						error('wrong dog loaded: wanted' + i + ', got ' + index);
					}
					notice('got dog ' + i);
					i++
				}
				
				success('looped through dogs');
				
				runner(step);
			}
		});
	} else {
		getAllDogs();
	}
}

function cleanupAllDogs(step){

	var options = {
		type:'dogs',
		client:client,
		qs:{limit:50} //limit statement set to 50
	}
	var dogs = new usergrid.collection(options, function(err) {  
		if (err) { 
			error('could not get all dogs'); 
		} else {

			//we got 50 dogs, now display the Entities:
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				var dog = dogs.getNextEntity();
				var name = dog.get('name');
				notice('dog is called ' + name);
			}
			
			dogs.resetEntityPointer();
			
			//do doggy cleanup
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				var dog = dogs.getNextEntity();
				var name = dog.get('name'); 
				notice('removing dog ' + name + ' from database');
				dog.destroy(function(err, data) {
					if (err) {
						error('dog ' + name + ' not removed'); 
					} else {
						success('dog ' + name + ' removed'); 
					}
				});
			}
			
			//no need to wait around for dogs to be removed, so go on to next test				
			runner(step);
		}
	});
}

	/*

	var options = {
		type:'dogs',
		client:client,
		qs:{ql:'order by created DESC'}
	}

	var options = {
		type:'dogs'
		client:client,
		qs:{ql:'order by created DESC',limit:'100'}
	}

	var options = {
		type:'dogs',
		client:client,
		qs:{ql:"select * where color='brown'"}
	}

	var options = {
		type:'dogs',
		client:client,
		qs:{'ql':"select name, age where color='brown'"}
	}


*/
function createUser(step) {
	
	var options = {
			client:client,
			type:'users'
	}
	
	var marty = new usergrid.entity(options);
	//set properties individually
	marty.set('username', 'marty');
	//or use one data object
	var data = {
		password:'mysecurepassword',
		name:'Marty McFly',
		city:'Hill Valley',
		state: 'California'
	}
	marty.set(data); //adds properties cumulatively

	marty.save(function(err){
		if (err){
			error('user not saved');
		} else {
			success('user saved');
			
			runner(step, marty);
		}
	});
}

function updateUser(step, marty) {

	marty.set("girlfriend","Jennifer");
	marty.save(function(err){
		if (err){
			error('user not updated');
		} else {
			success('user updated');
			runner(step, marty)
		}
	});
}

function refreshUser(step, marty) {
	marty.fetch(function(err){
		if (err){
			error('not refreshed');
		} else {
			success('user refreshed');
			runner(step, marty);
		}
	});
}

function loginUser(step, marty) {
	username = 'marty';
	password = 'mysecurepassword';
	client.login(username, password,
		function (err) {
			if (err) {
				error('could not log user in');
			} else {
				//token has been automatically saved by the client 
				// object and can be used for the next call if
				// the authType has been set to 'APP_USER' (see below)
				
				
				//to get the currently logged in user:
				var user = client.user;
				//to get their username: 
				var username = client.user.get('username');      
				
				if (client.isLoggedIn()) {
					success('user has been logged in');
				}
				

				//first get the token from the original client (after the user is logged in)
				var token = client.token;
				
				//then make a new client just for the app user
				var appUserClient = new usergrid.client({ 
					orgName:'myorg', 
					appName:'myapp', 
					authType:usergrid.APP_USER, 
					token:token
				});

				appUserClient.logout();  


				//log the user out
				client.logout();
				
				//verify the logout worked
				if (client.isLoggedIn()) {
					error('logout failed');
				} else {
					success('user has been logged out');
				}
				
				runner(step, marty);
				
			}
		}
	);
}

function destroyUser(step, marty) {
	var city = marty.get("city");
	marty.destroy(function(err){
		if (err){
			error('user not deleted from database');
		} else {
			success('user deleted from database');
			marty = null;
			runner(step);
		}
	});
}
	