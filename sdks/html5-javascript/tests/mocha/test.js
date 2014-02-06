/*
	Creates a generic usergrid client with logging and buildCurl disabled

 */
function getClient() {
	return new Usergrid.Client({
		orgName: 'yourorgname',
		appName: 'sandbox',
		logging: false, //optional - turn on logging, off by default
		buildCurl: true //optional - turn on curl commands, off by default
	});
}
/*
	A convenience function that will test for the presence of an API error
	and run any number of additional tests
 */
function usergridTestHarness(err, data, done, tests, ignoreError) {
	if (!ignoreError) assert(!err, data.error_description);
	if (tests) {
		if ("function" === typeof tests) {
			tests(err, data);
		} else if (tests.length) {
			tests.forEach(function(test) {
				if ("function" === typeof test) {
					test(err, data);
				}
			})
		}
	}
	done();
}
describe('Usergrid', function() {
	var client = getClient();
	describe('Usergrid CRUD', function() {
		before(function(done) {
			//Make sure our dog doesn't already exist
			client.request({
				method: 'DELETE',
				endpoint: 'users/fred'
			}, function(err, data) {
				done();
			});
		});
		var options = {
			method: 'GET',
			endpoint: 'users'
		};
		it('should CREATE a new user', function(done) {
			client.request({
				method: 'POST',
				endpoint: 'users',
				body: {
					username: 'fred',
					password: 'secret'
				}
			}, function(err, data) {
				usergridTestHarness(err, data, done, [

					function(err, data) {
						assert(true)
					}
				]);
			});
		});
		it('should RETRIEVE an existing user', function(done) {
			client.request({
				method: 'GET',
				endpoint: 'users/fred',
				body: {}
			}, function(err, data) {
				usergridTestHarness(err, data, done, [

					function(err, data) {
						assert(true)
					}
				]);
			});
		});
		it('should UPDATE an existing user', function(done) {
			client.request({
				method: 'PUT',
				endpoint: 'users/fred',
				body: {
					newkey: 'newvalue'
				}
			}, function(err, data) {
				usergridTestHarness(err, data, done, [

					function(err, data) {
						assert(true)
					}
				]);
			});
		});
		it('should DELETE the user from the database', function(done) {
			client.request({
				method: 'DELETE',
				endpoint: 'users/fred'
			}, function(err, data) {
				usergridTestHarness(err, data, done, [

					function(err, data) {
						assert(true)
					}
				]);
			});
		});
	});
	describe('Usergrid REST', function() {
		it('should return a list of users', function(done) {
			client.request({
				method: 'GET',
				endpoint: 'users'
			}, function(err, data) {
				usergridTestHarness(err, data, done, [

					function(err, data) {
						assert(!data.entities);
						console.log(JSON.stringify(data))
					}
				]);
			});
		});
		it('should return no entities when an endpoint does not exist', function(done) {
			client.request({
				method: 'GET',
				endpoint: 'nonexistantendpoint'
			}, function(err, data) {
				usergridTestHarness(err, data, done, [

					function(err, data) {
						assert(!data.entities)
					}
				]);
			});
		});
	});
	describe('Usergrid Entity', function() {
		var dog;
		before(function(done) {
			//Make sure our dog doesn't already exist
			client.request({
				method: 'DELETE',
				endpoint: 'dogs/Rocky'
			}, function(err, data) {
				assert(true);
				done();
			});
		});
		it('should CREATE a new dog', function(done) {
			var options = {
				type: 'dogs',
				name: 'Rocky'
			}
			client.createEntity(options, function(err, entity) {
				assert(!err, "dog not created");
				dog = entity;
                console.log("AFTER CREATE", dog.get());
				done();
			});
		});
		it('should RETRIEVE the dog', function(done) {
			if (!dog) {
				assert(false, "dog not created");
				done();
				return;
			}
            console.log("BEFORE FETCH", dog.get());
			//once the dog is created, you can set single properties:
			dog.fetch(function(err) {
				assert(!err, "dog not fetched");
				done();
			});
		});
		it('should UPDATE the dog', function(done) {
			if (!dog) {
				assert(false, "dog not created");
				done();
				return;
			}
			//once the dog is created, you can set single properties:
			dog.set('breed', 'Dinosaur');

			//the set function can also take a JSON object:
			var data = {
				master: 'Fred',
				state: 'hungry'
			}
			//set is additive, so previously set properties are not overwritten
			dog.set(data);
            console.log("BEFORE SAVE", dog.get());
			//finally, call save on the object to save it back to the database
			dog.save(function(err) {
				assert(!err, "dog not saved");
				done();
			});
		});
		it('should DELETE the dog', function(done) {
			if (!dog) {
				assert(false, "dog not created");
				done();
				return;
			}
			//once the dog is created, you can set single properties:
			dog.destroy(function(err) {
				assert(!err, "dog not removed");
				done();
			});
		});
	});
	describe('Usergrid Collections', function() {
		var client = getClient();
		var dog, dogs = {};

		function loop(done) {
			while (dogs.hasNextEntity()) {
				//get a reference to the dog
				dog = dogs.getNextEntity();
				console.log(dog.get('name'));
			}
			if (done) done();
		}
		before(function(done) {
			//Make sure our dog doesn't already exist
			var options = {
				type: 'dogs',
				qs: {
					limit: 50
				} //limit statement set to 50
			}

			client.createCollection(options, function(err, dogs) {
				if (!err) {
					assert(!err, "could not retrieve list of dogs: " + dogs.error_description);
					//we got 50 dogs, now display the Entities:
					//do doggy cleanup
					if (dogs.hasNextEntity()) {
						while (dogs.hasNextEntity()) {
							//get a reference to the dog
							var dog = dogs.getNextEntity();
							//notice('removing dog ' + dogname + ' from database');
							dog.destroy(function(err, data) {
								assert(!err, dog.get('name') + " not removed: " + data.error_description);
								if (!dogs.hasNextEntity()) {
									done();
								}
							});
						}
					} else {
						done();
					}
				}
			});
		});
		before(function(done) {
			this.timeout(10000);
			var totalDogs = 30;
			Array.apply(0, Array(totalDogs)).forEach(function(x, y) {
				var dogNum = y + 1;
				var options = {
					type: 'dogs',
					name: 'dog' + dogNum,
					index: y
				}
				client.createEntity(options, function(err, dog) {
					assert(!err, " not created: " + dog.error_description);
					if (dogNum === totalDogs) {
						done();
					}
				});
			})
		});
		it('should CREATE a new dogs collection', function(done) {
			var options = {
				type: 'dogs',
				qs: {
					ql: 'order by index'
				}
			}

			client.createCollection(options, function(err, data) {
				assert(!err, "could not create dogs collection: " + data.error_description);
				dogs = data;
				done();
			});
		});
		it('should RETRIEVE dogs from the collection', function(done) {
			loop(done);
		});
		it('should RETRIEVE the next page of dogs from the collection', function(done) {
			if (dogs.hasNextPage()) {
				dogs.getNextPage(function(err) {
					loop(done);
				});
			} else {
				done();
			}
		});
		it('should RETRIEVE the previous page of dogs from the collection', function(done) {
			if (dogs.hasPreviousPage()) {
				dogs.getPreviousPage(function(err) {
					loop(done);
				});
			} else {
				done();
			}
		});
	});
	describe('Usergrid Counters', function() {
		var counter;
		var MINUTE = 1000 * 60;
		var HOUR = MINUTE * 60;
		var time = Date.now() - HOUR;

		it('should CREATE a counter', function(done) {
			counter = new Usergrid.Counter({
				client: client,
				data: {
					category: 'mocha_test',
					timestamp: time,
					name: "test",
					counters: {
						test: 0,
						test_counter: 0
					}
				}
			}, function(err, data) {
				assert(!err, data.error_description);
				console.log(data);
				done();
			});
		});
		it('should save a counter', function(done) {
			counter.save(function(err, data) {
				assert(!err, data.error_description);
				console.log(data);
				done();
			});
		});
		it('should reset a counter', function(done) {
			time += MINUTE * 10
			counter.set("timestamp", time);
			counter.reset({
				name: 'test'
			}, function(err, data) {
				assert(!err, data.error_description);
				console.log(data);
				done();
			});
		});
		it("should increment 'test' counter", function(done) {
			time += MINUTE * 10
			counter.set("timestamp", time);
			counter.increment({
				name: 'test',
				value: 1
			}, function(err, data) {
				assert(!err, data.error_description);
				console.log(data);
				done();
			});
		});
		it("should increment 'test_counter' counter by 4", function(done) {
			time += MINUTE * 10
			counter.set("timestamp", time);
			counter.increment({
				name: 'test_counter',
				value: 4
			}, function(err, data) {
				assert(!err, data.error_description);
				console.log(JSON.stringify(data, null, 4));
				done();
			});
		});
		it("should decrement 'test' counter", function(done) {
			time += MINUTE * 10
			counter.set("timestamp", time);
			counter.decrement({
				name: 'test',
				value: 1
			}, function(err, data) {
				assert(!err, data.error_description);
				console.log(JSON.stringify(data, null, 4));
				done();
			});
		});
		it('should fetch the counter', function(done) {
			counter.fetch(function(err, data) {
				assert(!err, data.error_description);
				console.log(JSON.stringify(data, null, 4));
				console.log(time, Date.now());
				done();
			});
		});
		it('should fetch counter data', function(done) {
			counter.getData({
				resolution: 'all',
				counters: ['test', 'test_counter']
			}, function(err, data) {
				assert(!err, data.error_description);
				console.log(data);
				console.log(time, Date.now());
				done();
			});
		});
	});
	describe('Usergrid Folders and Assets', function() {
		var folder,
			asset,
			user,
			image_type,
			image_url = 'http://placekitten.com/160/90',
			// image_url="https://api.usergrid.com/yourorgname/sandbox/assets/a4025e7a-8ab1-11e3-b56c-5d3c6e4ca93f/data",
			test_image,
			filesystem,
			file_url,
			filename = "kitten.jpg",
			foldername = "kittens",
			folderpath = '/test/' + Math.round(10000 * Math.random()),
			filepath = [folderpath, foldername, filename].join('/');
		before(function(done) {
			var req = new XMLHttpRequest();
			req.open('GET', image_url, true);
			req.responseType = 'blob';
			req.onload = function() {
				test_image = req.response;
				image_type = req.getResponseHeader('Content-Type');
				done();
			}
			req.onerror = function(err) {
				done();
			};
			req.send(null);
		});
		before(function(done) {
			this.timeout(10000);
			client.request({
				method: 'GET',
				endpoint: 'Assets'
			}, function(err, data) {
				var assets = data.entities.filter(function(asset) {
					return asset.name === filename
				});
				if (assets.length) {
					assets.forEach(function(asset) {
						client.request({
							method: 'DELETE',
							endpoint: 'assets/' + asset.uuid
						});
					});
					done();
				} else {
					done();
				}
			});
		});
		before(function(done) {
			this.timeout(10000);
			client.request({
				method: 'GET',
				endpoint: 'folders'
			}, function(err, data) {
				var folders = data.entities.filter(function(folder) {
					return folder.name === foldername
				});
				if (folders.length) {
					folders.forEach(function(folder) {
						client.request({
							method: 'DELETE',
							endpoint: 'folders/' + folder.uuid
						});
					});
					done();
				} else {
					done();
				}
			});
		});
		before(function(done) {
			this.timeout(10000);
			user = new Usergrid.Entity({
				client: client,
				data: {
					type: 'users',
					username: 'assetuser'
				}
			});
			user.fetch(function(err, data) {
				console.log(user);
				if (err) {
					user.save(function() {
						done();
					})
				} else {
					done();
				}
			})
		});
		it('should CREATE a folder', function(done) {
			console.log("FOLDERNAME:", foldername);
			folder = new Usergrid.Folder({
				client: client,
				data: {
					name: foldername,
					owner: user.get("uuid"),
					path: folderpath
				}
			}, function(err, data) {
				assert(!err, data.error_description);
				done();
			});
		});
		it('should CREATE an asset', function(done) {
			asset = new Usergrid.Asset({
				client: client,
				data: {
					name: filename,
					owner: user.get("uuid"),
					path: filepath
				}
			}, function(err, data) {
				assert(!err, data.error_description);
				//console.log(data);
				done();
			});
		});
		it('should upload asset data', function(done) {
			this.timeout(15000);
			setTimeout(function() {
				asset.upload(test_image, function(err, data) {
					assert(!err, data.error_description);
					done();
				});
			}, 10000);
		});
		it('should retrieve asset data', function(done) {
			asset.download(function(err, data) {
				assert(!err, data.error_description);
				assert(data.type == test_image.type, "MIME types don't match");
				assert(data.size == test_image.size, "sizes don't match");
				done();
			});
		});
		it('should add the asset to a folder', function(done) {
			folder.addAsset({
				asset: asset
			}, function(err, data) {
				assert(!err, data.error_description);
				//console.log(data['entities']);
				done();
			})
		});
		it('should list the assets from a folder', function(done) {
			folder.getAssets(function(err, assets) {
				assert(!err, assets.error_description);
				//console.log(folder['assets']);
				done();
			})
		});
		it('should remove the asset from a folder', function(done) {
			folder.removeAsset({
				asset: asset
			}, function(err, data) {
				assert(!err, data.error_description);
				//console.log(data['entities']);
				done();
			})
		});
		it('should DELETE the asset', function(done) {
			asset.destroy(function(err, data) {
				assert(!err, data.error_description);
				//console.log(data);
				done();
			})
		});
		it('should DELETE the folder', function(done) {
			folder.destroy(function(err, data) {
				assert(!err, data.error_description);
				//console.log(data);
				done();
			})
		});
	});
});
