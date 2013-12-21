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


var usergrid = require('./lib/usergrid');

var logSuccess = true;
var successCount = 0;
var logError = true;
var errorCount = 0;
var logNotice = true;
var _unique = new Date().getTime();
var _username = 'marty'+_unique;
var _email = 'marty'+_unique+'@timetravel.com';
var _password = 'password2';
var _newpassword = 'password3';

	var client = new usergrid.client({
		orgName:'yourorgname',
		appName:'yourappname',
		authType:usergrid.AUTH_CLIENT_ID,
		clientId:'<your client id>',
		clientSecret:'<your client secret>',
		logging: false, //optional - turn on logging, off by default
		buildCurl: false //optional - turn on curl commands, off by default
	});


	var client = new usergrid.client({
		orgName:'yourorgname',
		appName:'sandbox',
		logging: true, //optional - turn on logging, off by default
		buildCurl: true //optional - turn on curl commands, off by default
	});


//call the runner function to start the process
client.logout();
runner(0);

function runner(step, arg, arg2){
	step++;
	switch(step)
	{
		case 1:
			notice('-----running step '+step+': DELETE user from DB to prep test');
			clearUser(step);
			break;
		case 2:
			notice('-----running step '+step+': GET test');
			testGET(step);
			break;
		case 3:
			notice('-----running step '+step+': POST test');
			testPOST(step);
			break;
		case 4:
			notice('-----running step '+step+': PUT test');
			testPUT(step);
			break;
		case 5:
			notice('-----running step '+step+': DELETE test');
			testDELETE(step);
			break;
		case 6:
			notice('-----running step '+step+': prepare database - remove all dogs (no real dogs harmed here!!)');
			cleanupAllDogs(step);
			break;
		case 7:
			notice('-----running step '+step+': make a new dog');
			makeNewDog(step);
			break;
		case 8:
			notice('-----running step '+step+': update our dog');
			updateDog(step, arg);
			break;
		case 9:
			notice('-----running step '+step+': refresh our dog');
			refreshDog(step, arg);
			break;
		case 10:
			notice('-----running step '+step+': remove our dog from database (no real dogs harmed here!!)');
			removeDogFromDatabase(step, arg);
			break;
		case 11:
			notice('-----running step '+step+': make lots of dogs!');
			makeSampleData(step, arg);
			break;
		case 12:
			notice('-----running step '+step+': make a dogs collection and show each dog');
			testDogsCollection(step);
			break;
		case 13:
			notice('-----running step '+step+': get the next page of the dogs collection and show each dog');
			getNextDogsPage(step, arg);
			break;
		case 14:
			notice('-----running step '+step+': get the previous page of the dogs collection and show each dog');
			getPreviousDogsPage(step, arg);
			break;
		case 15:
			notice('-----running step '+step+': remove all dogs from the database (no real dogs harmed here!!)');
			cleanupAllDogs(step);
			break;
		case 16:
			notice('-----running step '+step+': prepare database (remove existing user if present)');
			prepareDatabaseForNewUser(step);
			break;
		case 17:
			notice('-----running step '+step+': create a new user');
			createUser(step);
			break;
		case 18:
			notice('-----running step '+step+': update the user');
			updateUser(step, arg);
			break;
		case 19:
			notice('-----running step '+step+': get the existing user');
			getExistingUser(step, arg);
			break;
		case 20:
			notice('-----running step '+step+': refresh the user from the database');
			refreshUser(step, arg);
			break;
		case 21:
			notice('-----running step '+step+': log user in');
			loginUser(step, arg);
			break;
		case 22:
			notice('-----running step '+step+': change users password');
			changeUsersPassword(step, arg);
			break;
		case 23:
			notice('-----running step '+step+': log user out');
			logoutUser(step, arg);
			break;
		case 24:
			notice('-----running step '+step+': relogin user');
			reloginUser(step, arg);
			break;
		case 25:
			notice('-----running step '+step+': logged in user creates dog');
			createDog(step, arg);
			break;
		case 26:
			notice('-----running step '+step+': logged in user likes dog');
			userLikesDog(step, arg, arg2);
			break;
		case 27:
			notice('-----running step '+step+': logged in user removes likes connection to dog');
			removeUserLikesDog(step, arg, arg2);
			break;
		case 28:
			notice('-----running step '+step+': user removes dog');
			removeDog(step, arg, arg2);
			break;
		case 29:
			notice('-----running step '+step+': log the user out');
			logoutUser(step, arg);
			break;
		case 30:
			notice('-----running step '+step+': remove the user from the database');
			destroyUser(step, arg);
			break;
		case 31:
			notice('-----running step '+step+': try to create existing entity');
			createExistingEntity(step, arg);
			break;
		case 32:
			notice('-----running step '+step+': try to create new entity with no name');
			createNewEntityNoName(step, arg);
			break;
		case 33:
			notice('-----running step '+step+': clean up users');
      cleanUpUsers(step, arg);
			break;
		case 34:
			notice('-----running step '+step+': clean up dogs');
      cleanUpDogs(step, arg);
			break;
		default:
			notice('-----test complete!-----');
			notice('Success count= ' + successCount);
			notice('Error count= ' + errorCount);
			notice('-----thank you for playing!-----');
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
function clearUser(step) {
  var options = {
    method:'DELETE',
    endpoint:'users/fred'
  };
  client.request(options, function (err, data) {
    //data will contain raw results from API call
    success('User cleared from DB');
    runner(step);
  });
}

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
		name:'Ralph'+_unique
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
		name:'dog'+_unique+i,
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
		endpoint:'users/',
    qs:{ql:"select * where username ='marty*'"}
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
	client.signup(_username, _password, _email, 'Marty McFly',
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

function getExistingUser(step, marty) {

	var options = {
		type:'users',
		username:_username
	}
	client.getEntity(options, function(err, existingUser){
		if (err){
			error('existing user not retrieved');
		} else {
			success('existing user was retrieved');

			var username = existingUser.get('username');
			if (username === _username){
				success('got existing user username');
			} else {
				error('could not get existing user username');
			}
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
	username = _username;
	password = _password;
	client.login(username, password,
		function (err) {
			if (err) {
				error('could not log user in');
			} else {
				success('user has been logged in');

				//the login call will return an OAuth token, which is saved
				//in the client object for later use.  Access it this way:
				var token = client.token;

				//then make a new client just for the app user, then use this
				//client to make calls against the API
				var appUserClient = new usergrid.client({
					orgName:'yourorgname',
					appName:'yourappname',
					authType:usergrid.AUTH_APP_USER,
					token:token
				});

				//alternitavely, you can change the authtype of the client:
				client.authType = usergrid.AUTH_APP_USER;

				//Then make calls against the API.  For example, you can
				//get the user entity this way:
				client.getLoggedInUser(function(err, data, user) {
					if(err) {
						error('could not get logged in user');
					} else {
						success('got logged in user');

						//you can then get info from the user entity object:
						var username = user.get('username');
						notice('logged in user was: ' + username);

						runner(step, user);
					}
				});

			}
		}
	);
}

function changeUsersPassword(step, marty) {

	marty.set('oldpassword', _password);
	marty.set('newpassword', _newpassword);
	marty.save(function(err){
		if (err){
			error('user password not updated');
		} else {
			success('user password updated');
			runner(step, marty);
		}
	});

}

function logoutUser(step, marty) {

	//to log the user out, call the logout() method
	client.logout();

	//verify the logout worked
	if (client.isLoggedIn()) {
		error('logout failed');
	} else {
		success('user has been logged out');
	}

	runner(step, marty);
}

function reloginUser(step, marty) {

	username = _username
	password = _newpassword;
	client.login(username, password,
		function (err) {
		if (err) {
			error('could not relog user in');
		} else {
			success('user has been re-logged in');
			runner(step, marty);
		}
		}
	);
}



//TODO: currently, this code assumes permissions have been set to support user actions.  need to add code to show how to add new role and permission programatically
//
//first create a new permission on the default role:
//POST "https://api.usergrid.com/yourorgname/yourappname/roles/default/permissions" -d '{"permission":"get,post,put,delete:/dogs/**"}'
//then after user actions, delete the permission on the default role:
//DELETE "https://api.usergrid.com/yourorgname/yourappname/roles/default/permissions?permission=get%2Cpost%2Cput%2Cdelete%3A%2Fdogs%2F**"


function createDog(step, marty) {
  //see if marty can create a new dog now that he is logged in

	var options = {
		type:'dogs',
		name:'einstein',
		breed:'mutt'
	}

	client.createEntity(options, function (err, dog) {
		if (err) {
			error('POST new dog by logged in user failed');
		} else {
			success('POST new dog by logged in user succeeded');
			runner(step, marty, dog);
		}
	});

}

function userLikesDog(step, marty, dog) {

	marty.connect('likes', dog, function (err, data) {
		if (err) {
			error('connection not created');
			runner(step, marty);
		} else {

			//call succeeded, so pull the connections back down
			marty.getConnections('likes', function (err, data) {
				if (err) {
						error('could not get connections');
				} else {
					//verify that connection exists
					if (marty.likes.einstein) {
						success('connection exists');
					} else {
						error('connection does not exist');
					}

					runner(step, marty, dog);
				}
			});
		}
	});

}

function removeUserLikesDog(step, marty, dog) {

	marty.disconnect('likes', dog, function (err, data) {
		if (err) {
			error('connection not deleted');
			runner(step, marty);
		} else {

			//call succeeded, so pull the connections back down
			marty.getConnections('likes', function (err, data) {
				if (err) {
					error('error getting connections');
				} else {
					//verify that connection exists
					if (marty.likes.einstein) {
						error('connection still exists');
					} else {
						success('connection deleted');
					}

					runner(step, marty, dog);
				}
			});
		}
	});

}

function removeDog(step, marty, dog) {

	//now delete the dog from the database
	dog.destroy(function(err, data) {
		if (err) {
			error('dog not removed');
		} else {
			success('dog removed');
		}
	});
	runner(step, marty);
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

function createExistingEntity(step, marty) {

	var options = {
		type:'dogs',
		name:'einstein'
	}

	client.createEntity(options, function (err, dog) {
		if (err) {
			error('Create new entity to use for existing entity failed');
		} else {
			success('Create new entity to use for existing entity succeeded');

			var uuid = dog.get('uuid');
			//now create new entity, but use same entity name of einstein.  This means that
			//the original einstein entity now exists.  Thus, the new einstein entity should
			//be the same as the original + any data differences from the options var:

			options = {
				type:'dogs',
				name:'einstein',
				breed:'mutt'
			}
			client.createEntity(options, function (err, newdog) {
				if (err) {
					error('Create new entity to use for existing entity failed');
				} else {
					success('Create new entity to use for existing entity succeeded');

					var newuuid = newdog.get('uuid');
					if (newuuid === uuid) {
						success('UUIDs of new and old entities match');
					} else {
						error('UUIDs of new and old entities do not match');
					}

					var breed = newdog.get('breed');
					if (breed === 'mutt') {
						success('attribute sucesfully set on new entity');
					} else {
						error('attribute not sucesfully set on new entity');
					}

					newdog.destroy(function(err){
						if (err){
							error('existing entity not deleted from database');
						} else {
							success('existing entity deleted from database');
							dog = null; //blow away the local object
							newdog = null; //blow away the local object
							runner(step);
						}
					});

				}
			});
		}
	});

}

function createNewEntityNoName(step, marty) {

	var options = {
   type:"something",
   othervalue:"something else"
	}

	client.createEntity(options, function (err, entity) {
		if (err) {
			error('Create new entity with no name failed');
		} else {
			success('Create new entity with no name succeeded');

      entity.destroy();
      runner(step);
		}
	});

}

function cleanUpUsers(step){

  var options = {
    type:'users',
    qs:{limit:50} //limit statement set to 50
  }

  client.createCollection(options, function (err, users) {
    if (err) {
      error('could not get all users');
    } else {
      success('got users');
      //do doggy cleanup
      while(users.hasNextEntity()) {
        //get a reference to the dog
        var user = users.getNextEntity();
        var username = user.get('name');
        notice('removing dog ' + username + ' from database');
        user.destroy(function(err, data) {
          if (err) {
            error('user not removed');
          } else {
            success('user removed');
          }
        });
      }

      runner(step);
    }
  });

}

function cleanUpDogs(step){

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
