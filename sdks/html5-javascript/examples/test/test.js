/**
* Test suite for all the examples in the readme
*
* NOTE: No, this test suite doesn't use the traditional format for
* a test suite.  This is because the goal is to require as little
* alteration as possible during the copy / paste operation from this test
* suite to the readme file.
*
* @author rod simpson (rod@apigee.com)
*/

$(document).ready(function () {

//call the runner function to start the process
$('#start-button').bind('click', function() {
	$('#start-button').attr("disabled", "disabled");
	$('#test-output').html('');
	runner(0);
});

var logSuccess = true;
var successCount = 0;
var logError = true;
var errorCount = 0;
var logNotice = true;

var client = new Usergrid.Client({
	orgName:'yourorgname',
	appName:'sandbox',
	logging: true, //optional - turn on logging, off by default
	buildCurl: true //optional - turn on curl commands, off by default
});

function runner(step, arg){
	step++;
	switch(step)
	{
	case 1:
		notice('-----running step '+step+': GET test');
		testGET(step);
		break;
	case 2:
		notice('-----running step '+step+': POST test');
		testPOST(step);
		break;
	case 3:
		notice('-----running step '+step+': PUT test');
		testPUT(step);
		break;
	case 4:
		notice('-----running step '+step+': DELETE test');
		testDELETE(step);
		break;
	case 5:
		notice('-----running step '+step+': prepare database - remove all dogs (no real dogs harmed here!!)');
		cleanupAllDogs(step);
		break;
	case 6:
		notice('-----running step '+step+': make a new dog');
		makeNewDog(step);
		break;
	case 7:
		notice('-----running step '+step+': update our dog');
		updateDog(step, arg);
		break;
	case 8:
		notice('-----running step '+step+': refresh our dog');
		refreshDog(step, arg);
		break;
	case 9:
		notice('-----running step '+step+': remove our dog from database (no real dogs harmed here!!)');
		removeDogFromDatabase(step, arg);
		break;
	case 10:
		notice('-----running step '+step+': make lots of dogs!');
		makeSampleData(step, arg);
		break;
	case 11:
		notice('-----running step '+step+': make a dogs collection and show each dog');
		testDogsCollection(step);
		break;
	case 12:
		notice('-----running step '+step+': get the next page of the dogs collection and show each dog');
		getNextDogsPage(step, arg);
		break;
	case 13:
		notice('-----running step '+step+': get the previous page of the dogs collection and show each dog');
		getPreviousDogsPage(step, arg);
		break;
	case 14:
		notice('-----running step '+step+': remove all dogs from the database (no real dogs harmed here!!)');
		cleanupAllDogs(step);
		break;
	case 15:
		notice('-----running step '+step+': prepare database (remove existing user if present)');
		prepareDatabaseForNewUser(step);
		break;
	case 16:
		notice('-----running step '+step+': create a new user');
		createUser(step);
		break;
	case 17:
		notice('-----running step '+step+': update the user');
		updateUser(step, arg);
		break;
	case 18:
		notice('-----running step '+step+': refresh the user from the database');
		refreshUser(step, arg);
		break;
	case 19:
		notice('-----running step '+step+': refresh the user from the database');
		loginUser(step, arg);
		break;
	case 20:
		notice('-----running step '+step+': remove the user from the database');
		destroyUser(step, arg);
		break;
	default:
		notice('-----test complete!-----');
		notice('Success count= ' + successCount);
		notice('Error count= ' + errorCount);
		notice('-----thank you for playing!-----');
		$('#start-button').removeAttr("disabled");
	}
}

//logging functions
function success(message){
	successCount++;
	if (logSuccess) {
		console.log('SUCCESS: ' + message);
		var html = $('#test-output').html();
		html += ('SUCCESS: ' + message + '\r\n');
		$('#test-output').html(html);
	}
}

function error(message){
	errorCount++
	if (logError) {
		console.log('ERROR: ' + message);
		var html = $('#test-output').html();
		html += ('ERROR: ' + message + '\r\n');
		$('#test-output').html(html);
	}
}

function notice(message){
	if (logNotice) {
		console.log('NOTICE: ' + message);
		var html = $('#test-output').html();
		html += (message + '\r\n');
		$('#test-output').html(html);
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
		type:'dogs',
		name:'Dino'
	}

	client.createEntity(options, function (err, dog) {
		if (err) {
			error('dog not created');
		} else {
			success('dog is created');

			//once the dog is created, you can set single properties:
			dog.set('breed','Dinosaur');

			//the set function can also take a JSON object:
			var data = {
				master:'Fred',
				state:'hungry'
			}
			//set is additive, so previously set properties are not overwritten
			dog.set(data);

			//finally, call save on the object to save it back to the database
			dog.save(function(err){
				if (err){
					error('dog not saved');
				} else {
					success('new dog is saved');
					runner(step, dog);
				}
			});
		}
	});

}

function updateDog(step, dog) {

	//change a property in the object
	dog.set("state", "fed");
	//and save back to the database
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

	//call fetch to refresh the data from the server
	dog.fetch(function(err){
		if (err){
			error('dog not refreshed from database');
		} else {
			//dog has been refreshed from the database
			//will only work if the UUID for the entity is in the dog object
			success('dog entity refreshed from database');
			runner(step, dog);
		}
	});

}

function removeDogFromDatabase(step, dog){

	//the destroy method will delete the entity from the database
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
		type:'dogs',
		name:'dog'+i,
		index:i
	}

	client.createEntity(options, function (err, dog) {
		if (err) {
			error('dog ' + i + ' not created');
		} else {
			if (i >= 30) {
				//data made, ready to go
				success('all dogs made');
				runner(step);
			} else {
				success('dog ' + i + ' made');
				//keep making dogs
				makeSampleData(step, ++i);
			}
		}
	});
}

function testDogsCollection(step) {

	var options = {
		type:'dogs',
		qs:{ql:'order by index'}
	}

	client.createCollection(options, function (err, dogs) {
		if (err) {
			error('could not make collection');
		} else {

			success('new Collection created');

			//we got the dogs, now display the Entities:
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				dog = dogs.getNextEntity();
				var name = dog.get('name');
				notice('dog is called ' + name);
			}

			success('looped through dogs');

			//create a new dog and add it to the collection
			var options = {
				name:'extra-dog',
				fur:'shedding'
			}
			//just pass the options to the addEntity method
			//to the collection and it is saved automatically
			dogs.addEntity(options, function(err, dog, data) {
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

	if (dogs.hasNextPage()) {
		//there is a next page, so get it from the server
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
				success('looped through dogs')
				runner(step, dogs);
			}
		});
	} else {
		getPreviousDogsPage(dogs);
	}

}

function getPreviousDogsPage(step, dogs) {

	if (dogs.hasPreviousPage()) {
		//there is a previous page, so get it from the server
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
		qs:{limit:50} //limit statement set to 50
	}

	client.createCollection(options, function (err, dogs) {
		if (err) {
			error('could not get all dogs');
		} else {
			success('got at most 50 dogs');
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
				var dogname = dog.get('name');
				notice('removing dog ' + dogname + ' from database');
				dog.destroy(function(err, data) {
					if (err) {
						error('dog not removed');
					} else {
						success('dog removed');
					}
				});
			}

			//no need to wait around for dogs to be removed, so go on to next test
			runner(step);
		}
	});
}

function prepareDatabaseForNewUser(step) {
	var options = {
		method:'DELETE',
		endpoint:'users/marty'
	};
	client.request(options, function (err, data) {
		if (err) {
			notice('database ready - no user to delete');
		runner(step);
		} else {
			//data will contain raw results from API call
			success('database ready - user deleted worked');
			runner(step);
		}
	});
}

function createUser(step) {

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
			error('user not saved');
			runner(step, marty);
		} else {
			success('user saved');
			runner(step, marty);
		}
	});

}

function updateUser(step, marty) {

	//add properties cumulatively
	marty.set('state', 'California');
	marty.set("girlfriend","Jennifer");
	marty.save(function(err){
		if (err){
			error('user not updated');
		} else {
			success('user updated');
			runner(step, marty);
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
				//the user has been logged in and the token has been stored
				//in the client. any calls made now will use the token.
				//once a user has logged in, thier user object is stored
				//in the client and you can access it this way:
				var token = client.token;

				//you can also detect if the user is logged in:
				if (client.isLoggedIn()) {
					success('user has been logged in');
					//get the logged in user entity by calling for it:
					client.getLoggedInUser(function(err, data, user) {
						if(err) {
							error('could not get logged in user');
						} else {
							success('got logged in user');
							//you can then info from the user entity object:
							var username = user.get('username');
							notice('logged in user was: ' + username);

							//to log a user out:
							client.logout();

							//verify the logout worked
							if (client.isLoggedIn()) {
								error('logout failed');
							} else {
								success('user has been logged out');
							}

							runner(step, marty);
						}
					});
				}
			}
		}
	);
}

function destroyUser(step, marty) {

	marty.destroy(function(err){
		if (err){
			error('user not deleted from database');
		} else {
			success('user deleted from database');
			marty = null; //blow away the local object
			runner(step);
		}
	});

}

});