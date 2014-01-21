
/*
	Creates a generic usergrid client with logging and buildCurl disabled

 */
function getClient(){
	return new Usergrid.Client({
		orgName:'yourorgname',
		appName:'sandbox',
		logging: false, //optional - turn on logging, off by default
		buildCurl: true //optional - turn on curl commands, off by default
	});
}
/*
	A convenience function that will test for the presence of an API error
	and run any number of additional tests
 */
function usergridTestHarness(err, data, done, tests, ignoreError){
	if(!ignoreError) assert(!err, data.error_description);
	if(tests){
		if("function"===typeof tests){
			tests(err, data);
		}else if(tests.length){
			tests.forEach(function(test){
				if("function"===typeof test){
					test(err, data);
				}
			})
		}
	}	
	done();
}
describe('Usergrid', function(){
	var client = getClient();

	before(function(done){
    	//Make sure our dog doesn't already exist
		client.request({method:'DELETE',endpoint:'users/fred'}, function (err, data) {
			done();
	    });
  	});
	describe('Usergrid CRUD', function(){
		var options = {
			method:'GET',
			endpoint:'users'
		};
		it('should CREATE a new user', function(done){
			client.request({method:'POST',endpoint:'users', body:{ username:'fred', password:'secret' }}, function (err, data) {
				usergridTestHarness(err, data, done, [
					function(err, data){assert(true)}
				]);
		    });
		});
		it('should RETRIEVE an existing user', function(done){
			client.request({method:'GET',endpoint:'users/fred', body:{}}, function (err, data) {
				usergridTestHarness(err, data, done, [
					function(err, data){assert(true)}
				]);
		    });
		});
		it('should UPDATE an existing user', function(done){
			client.request({method:'PUT',endpoint:'users/fred', body:{ newkey:'newvalue' }}, function (err, data) {
				usergridTestHarness(err, data, done, [
					function(err, data){assert(true)}
				]);
		    });
		});
		it('should DELETE the user from the database', function(done){
			client.request({method:'DELETE',endpoint:'users/fred'}, function (err, data) {
				usergridTestHarness(err, data, done, [
					function(err, data){assert(true)}
				]);
		    });
		});
	});
	describe('Usergrid REST', function(){
		it('should return a list of users', function(done){
			client.request({method:'GET',endpoint:'users'}, function (err, data) {
				usergridTestHarness(err, data, done, [
					function(err, data){assert(data.entities.length >=0)}
				]);
		    });
		});
		it('should return no entities when an endpoint does not exist', function(done){
			client.request({method:'GET',endpoint:'nonexistantendpoint'}, function (err, data) {
				usergridTestHarness(err, data, done, [
					function(err, data){assert(data.entities.length === 0)}
				]);
		    });
		});
	});
	describe('Usergrid Entity', function(){
		var dog;
		before(function(done){
	    	//Make sure our dog doesn't already exist
	    	client.request({method:'DELETE',endpoint:'dogs/Rocky'}, function (err, data) {
	    		assert(true);
	    		done();
		    });
	  	});
		it('should CREATE a new dog', function(done){
			var options = {
				type:'dogs',
				name:'Rocky'
			}

			client.createEntity(options, function (err, data) {
				assert(!err, "dog not created");
				dog=data;
				done();
			});
		});
		it('should RETRIEVE the dog', function(done){
			if(!dog){
				assert(false, "dog not created");
				done();
				return;
			}
			//once the dog is created, you can set single properties:
			dog.fetch(function(err){
				assert(!err, "dog not fetched");
				done();
			});
		});
		it('should UPDATE the dog', function(done){
			if(!dog){
				assert(false, "dog not created");
				done();
				return;
			}
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
				assert(!err, "dog not saved");
				done();
			});
		});
		it('should DELETE the dog', function(done){
			if(!dog){
				assert(false, "dog not created");
				done();
				return;
			}
			//once the dog is created, you can set single properties:
			dog.destroy(function(err){
				assert(!err, "dog not removed");
				done();
			});
		});

	});
	describe('Usergrid Collections', function(){
		var client = getClient();
		var dog, dogs={};
		function loop(done){
			while(dogs.hasNextEntity()) {
				//get a reference to the dog
				dog = dogs.getNextEntity();
				console.log(dog.get('name'));
			}
			if(done)done();
		}
		before(function(done){
	    	//Make sure our dog doesn't already exist
			var options = {
				type:'dogs',
				qs:{limit:50} //limit statement set to 50
			}

			client.createCollection(options, function (err, dogs) {
				if (!err) {
					assert(!err, "could not retrieve list of dogs: "+dogs.error_description);
					//we got 50 dogs, now display the Entities:
					//do doggy cleanup
					if(dogs.hasNextEntity()){
						while(dogs.hasNextEntity()) {
							//get a reference to the dog
							var dog = dogs.getNextEntity();
							//notice('removing dog ' + dogname + ' from database');
							dog.destroy(function(err, data) {
								assert(!err, dog.get('name')+" not removed: "+data.error_description);
								if(!dogs.hasNextEntity()){
									done();
								}
							});
						}
					}else{
						done();
					}
				}
			});
	  	});
		before(function(done){
			this.timeout(10000);
			var totalDogs=30;
			Array.apply(0, Array(totalDogs)).forEach(function (x, y) { 
				var dogNum=y+1;
				var options = {
					type:'dogs',
					name:'dog'+dogNum,
					index:y
				}
				client.createEntity(options, function (err, dog) {
					assert(!err, " not created: "+dog.error_description);
					if(dogNum===totalDogs){
						done();
					}
				});
			})
		});
		it('should CREATE a new dogs collection', function(done){
			var options = {
				type:'dogs',
				qs:{ql:'order by index'}
			}

			client.createCollection(options, function (err, data) {
				assert(!err, "could not create dogs collection: "+data.error_description);
				dogs=data;
				done();
			});
		});
		it('should RETRIEVE dogs from the collection', function(done){
			loop(done);
		});
		it('should RETRIEVE the next page of dogs from the collection', function(done){
			if(dogs.hasNextPage()){
				dogs.getNextPage(function(err){loop(done);});
			}else{
				done();
			}
		});
		it('should RETRIEVE the previous page of dogs from the collection', function(done){
			if(dogs.hasPreviousPage()){
				dogs.getPreviousPage(function(err){loop(done);});
			}else{
				done();
			}
		});
	});
	describe('Usergrid Events', function(){
		var ev;
		var MINUTE=1000*60;
		var HOUR=MINUTE*60;
		var time=Date.now()-HOUR;

		it('should CREATE an event', function(done){
			ev = new Usergrid.Event({client:client, data:{category:'mocha_test', timestamp:time, name:"test", counters:{test:0,test_counter:0}}}, function(err, data){
				assert(!err, data.error_description);
				console.log(data);
				done();
			});
		});
		it('should save an event', function(done){
			ev.save(function(err, data){
				assert(!err, data.error_description);
				console.log(data);
				done();
			});
		});
		it('should reset a counter', function(done){
			time+=MINUTE*10
			ev.set("timestamp", time);
			ev.reset('test', function(err, data){
				assert(!err, data.error_description);
				console.log(data);
				done();
			});
		});
		it("should increment 'test' counter", function(done){
			time+=MINUTE*10
			ev.set("timestamp", time);
			ev.increment('test', 1, function(err, data){
				assert(!err, data.error_description);
				console.log(data);
				done();
			});
		});
		it("should increment 'test_counter' counter by 4", function(done){
			time+=MINUTE*10
			ev.set("timestamp", time);
			ev.increment('test_counter', 4, function(err, data){
				assert(!err, data.error_description);
				console.log(JSON.stringify(data,null,4));
				done();
			});
		});
		it("should decrement 'test' counter", function(done){
			time+=MINUTE*10
			ev.set("timestamp", time);
			ev.decrement('test', 1, function(err, data){
				assert(!err, data.error_description);
				console.log(JSON.stringify(data,null,4));
				done();
			});
		});	
		it('should fetch event', function(done){
			ev.fetch(function(err, data){
				assert(!err, data.error_description);
				console.log(JSON.stringify(data,null,4));
				console.log(time, Date.now());
				done();
			});
		});
		it('should fetch counter data', function(done){
			ev.getData('all', null, null, ['test', 'test_counter'], function(err, data){
				assert(!err, data.error_description);
				console.log(data);
				console.log(time, Date.now());
				done();
			});
		});
	});
});

