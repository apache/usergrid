//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

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
	if (!ignoreError) assert(!err, (err)?err.error_description:"unknown");
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
describe('Ajax', function() {
    var dogName="dog"+Math.floor(Math.random()*10000);
    var dogData=JSON.stringify({type:"dog",name:dogName});
    var dogURI='https://api.usergrid.com/yourorgname/sandbox/dogs'
    it('should POST to a URI',function(done){
        Ajax.post(dogURI, dogData).then(function(err, data){
            assert(!err, err);
            done();
        })
    })
    it('should GET a URI',function(done){
        Ajax.get(dogURI+'/'+dogName).then(function(err, data){
            assert(!err, err);
            done();
        })
    })
    it('should PUT to a URI',function(done){
        Ajax.put(dogURI+'/'+dogName, {"favorite":true}).then(function(err, data){
            assert(!err, err);
            done();
        })
    })
    it('should DELETE a URI',function(done){
        Ajax.delete(dogURI+'/'+dogName, dogData).then(function(err, data){
            assert(!err, err);
            done();
        })
    })
});
describe('UsergridError', function() {
    var errorResponse={
        "error":"service_resource_not_found",
        "timestamp":1392067967144,
        "duration":0,
        "exception":"org.usergrid.services.exceptions.ServiceResourceNotFoundException",
        "error_description":"Service resource not found"
    };
    it('should unmarshal a response from Usergrid into a proper Javascript error',function(done){
        var error = UsergridError.fromResponse(errorResponse);
        assert(error.name===errorResponse.error, "Error name not set correctly");
        assert(error.message===errorResponse.error_description, "Error message not set correctly");
        done();
    });
});
describe('Usergrid', function(){
    describe('SDK Version', function(){
        it('should contain a minimum SDK version',function(){
            var parts=Usergrid.VERSION.split('.').map(function(i){return i.replace(/^0+/,'')}).map(function(i){return parseInt(i)});

            assert(parts[1]>=10, "expected minor version >=10");
            assert(parts[1]>10||parts[2]>=8, "expected minimum version >=8");
        });
    });
    describe('Usergrid Request/Response', function() {
        var dogName="dog"+Math.floor(Math.random()*10000);
        var dogData=JSON.stringify({type:"dog",name:dogName});
        var dogURI='https://api.usergrid.com/yourorgname/sandbox/dogs'
        it('should POST to a URI',function(done){
            var req=new Usergrid.Request("POST", dogURI, {}, dogData, function(err, response){
                console.error(err, response);
                assert(!err, err);
                assert(response instanceof Usergrid.Response, "Response is not and instance of Usergrid.Response");
                done();
            })
        })
        it('should GET a URI',function(done){
            var req=new Usergrid.Request("GET", dogURI+'/'+dogName, {}, null, function(err, response){
                assert(!err, err);
                assert(response instanceof Usergrid.Response, "Response is not and instance of Usergrid.Response");
                done();
            })
        })
        it('should GET an array of entity data from the Usergrid.Response object',function(done){
            var req=new Usergrid.Request("GET", dogURI, {}, null, function(err, response){
                assert(!err, err);
                assert(response instanceof Usergrid.Response, "Response is not and instance of Usergrid.Response");
                var entities=response.getEntities();
                assert(entities && entities.length, "Nothing was returned")
                done();
            })
        })
        it('should GET entity data from the Usergrid.Response object',function(done){
            var req=new Usergrid.Request("GET", dogURI+'/'+dogName, {}, null, function(err, response){
                var entity=response.getEntity();
                assert(!err, err);
                assert(response instanceof Usergrid.Response, "Response is not and instance of Usergrid.Response");
                assert(entity, "Nothing was returned")
                done();
            })
        })
        it('should PUT to a URI',function(done){
            var req=new Usergrid.Request("PUT", dogURI+'/'+dogName, {}, {favorite:true}, function(err, response){
                assert(!err, err);
                assert(response instanceof Usergrid.Response, "Response is not and instance of Usergrid.Response");
                done();
            })
        })
        it('should DELETE a URI',function(done){
            var req=new Usergrid.Request("DELETE", dogURI+'/'+dogName, {}, null, function(err, response){
                assert(!err, err);
                assert(response instanceof Usergrid.Response, "Response is not and instance of Usergrid.Response");
                done();
            })
        })
        it('should NOT allow an invalid method',function(done){
            try{
                var req=new Usergrid.Request("INVALID", dogURI+'/'+dogName, {}, null, function(err, response){
                    assert(true, "Should have thrown an UsergridInvalidHTTPMethodError");
                    done();
                })
            }catch(e){
                assert(e instanceof UsergridInvalidHTTPMethodError, "Error is not and instance of UsergridInvalidHTTPMethodError");
                done()
            }
        })
        it('should NOT allow an invalid URI',function(done){
            try{
                var req=new Usergrid.Request("GET", "://apigee.com", {}, null, function(err, response){
                    assert(true, "Should have thrown an UsergridInvalidURIError");
                    done();
                })
            }catch(e){
                assert(e instanceof UsergridInvalidURIError, "Error is not and instance of UsergridInvalidURIError");
                done()
            }
        })
        it('should return a UsergridError object on an invalid URI',function(done){
            var req=new Usergrid.Request("GET", dogURI+'/'+dogName+'zzzzz', {}, null, function(err, response){
                assert(err, "Should have returned an error");
                assert(response instanceof Usergrid.Response, "Response is not and instance of Usergrid.Response");
                assert(err instanceof UsergridError, "Error is not and instance of UsergridError");
                done();
            })
        })
    });
    describe('Usergrid Client', function() {
        var client = getClient();
        describe('Usergrid CRUD request', function() {
            before(function(done) {
                this.timeout(10000);
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
            it('should persist default query parameters', function(done) {
                //create new client with default params
                var client=new Usergrid.Client({
                    orgName: 'yourorgname',
                    appName: 'sandbox',
                    logging: false, //optional - turn on logging, off by default
                    buildCurl: true, //optional - turn on curl commands, off by default
                    qs:{
                        test1:'test1',
                        test2:'test2'
                    }
                });
                var default_qs=client.getObject('default_qs');
                assert(default_qs.test1==='test1', "the default query parameters were not persisted");
                assert(default_qs.test2==='test2', "the default query parameters were not persisted");
                client.request({
                    method: 'GET',
                    endpoint: 'users'
                }, function(err, data) {
                    assert(data.params.test2[0]==='test2', "the default query parameters were not sent to the backend");
                    assert(data.params.test1[0]==='test1', "the default query parameters were not sent to the backend");
                    done();
                });
            });
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
                            assert(!err)
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
                            assert(data.entities.length>=0, "Request should return at least one user");
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
                            assert(data.entities.length===0, "Request should return no entities");
                        }
                    ]);
                });
            });
        });
        describe('Usergrid convenience methods', function(){
            before(function(){ client.logout();});
            it('createEntity',function(done){
                client.createEntity({type:'dog',name:'createEntityTestDog'}, function(err, response, dog){
                    console.warn(err, response, dog);
                    assert(!err, "createEntity returned an error")
                    assert(dog, "createEntity did not return a dog")
                    assert(dog.get("name")==='createEntityTestDog', "The dog's name is not 'createEntityTestDog'")
                    done();
                })
            })
            it('createEntity - existing entity',function(done){
                    client.createEntity({type:'dog',name:'createEntityTestDog'}, function(err, response, dog){
                        try{
                            assert(err, "createEntity should return an error")
                        }catch(e){
                            assert(true, "trying to create an entity that already exists throws an error");
                        }finally{
                            done();
                        }
                    });
            })
            var testGroup;
            it('createGroup',function(done){
                client.createGroup({path:'dogLovers'},function(err, response, group){
                        try{
                            assert(!err, "createGroup returned an error")
                        }catch(e){
                            assert(true, "trying to create a group that already exists throws an error");
                        }finally{
                            done();
                        }
                    /*assert(!err, "createGroup returned an error: "+err);
                    assert(group, "createGroup did not return a group");
                    assert(group instanceof Usergrid.Group, "createGroup did not return a Usergrid.Group");
                    testGroup=group;
                    done();*/
                })
                done();
            })
            it('buildAssetURL',function(done){
                var assetURL='https://api.usergrid.com/yourorgname/sandbox/assets/00000000-0000-0000-000000000000/data';
                assert(assetURL===client.buildAssetURL('00000000-0000-0000-000000000000'), "buildAssetURL doesn't work")
                done();
            })
            var dogEntity;
            it('getEntity',function(done){
                client.getEntity({type:'dog',name:'createEntityTestDog'}, function(err, response, dog){
                    assert(!err, "createEntity returned an error")
                    assert(dog, "createEntity returned a dog")
                    assert(dog.get("uuid")!==null, "The dog's UUID was not returned")
                    dogEntity=dog;
                    done();
                })
            })
            it('restoreEntity',function(done){
                var serializedDog=dogEntity.serialize();
                var dog=client.restoreEntity(serializedDog);
                assert(dog, "restoreEntity did not return a dog")
                assert(dog.get("uuid")===dogEntity.get("uuid"), "The dog's UUID was not the same as the serialized dog")
                assert(dog.get("type")===dogEntity.get("type"), "The dog's type was not the same as the serialized dog")
                assert(dog.get("name")===dogEntity.get("name"), "The dog's name was not the same as the serialized dog")

                done();
            })
            var dogCollection;
            it('createCollection',function(done){
                client.createCollection({type:'dogs'},function(err, response, dogs){
                    assert(!err, "createCollection returned an error");
                    assert(dogs, "createCollection did not return a dogs collection");
                    dogCollection=dogs;
                    done();
                })
            })
            it('restoreCollection',function(done){
                var serializedDogs=dogCollection.serialize();
                var dogs=client.restoreCollection(serializedDogs);
                console.warn('restoreCollection',dogs, dogCollection);
                assert(dogs._type===dogCollection._type, "The dog collection type was not the same as the serialized dog collection")
                assert(dogs._qs==dogCollection._qs, "The query strings do not match")
                assert(dogs._list.length===dogCollection._list.length, "The collections have a different number of entities")
                done();
            })
            var activityUser;
            before(function(done){
                activityUser=new Usergrid.Entity({client:client,data:{"type":"user",'username':"testActivityUser"}});
                console.warn(activityUser);
                activityUser.fetch(function(err, data){
                    console.warn(err, data, activityUser);
                    if(err){
                        activityUser.save(function(err, data){
                            activityUser.set(data);
                            done();
                        });
                    }else{
                        activityUser.set(data);
                        done();
                    }
                })
            })
            it('createUserActivity',function(done){
                 var options = {
                   "actor" : {
                         "displayName" :"Test Activity User",
                             "uuid" : activityUser.get("uuid"),
                             "username" : "testActivityUser",
                             "email" : "usergrid@apigee.com",
                             "image" : {
                                     "height" : 80,
                                     "url" : "http://placekitten.com/80/80",
                                     "width" : 80
                             }
                        },
                        "verb" : "post",
                       "content" : "test post for createUserActivity",
                       "lat" : 48.856614,
                       "lon" : 2.352222
                     };
                client.createUserActivity("testActivityUser", options, function(err, activity){
                    assert(!err, "createUserActivity returned an error");
                    assert(activity, "createUserActivity returned no activity object")
                    done();
                })
            })
            it('createUserActivityWithEntity',function(done){
                    client.createUserActivityWithEntity(activityUser, "Another test activity with createUserActivityWithEntity", function(err, activity){
                        assert(!err, "createUserActivityWithEntity returned an error "+err);
                        assert(activity, "createUserActivityWithEntity returned no activity object")
                        done();
                    })
            })
            it('getFeedForUser',function(done){
                client.getFeedForUser('testActivityUser', function(err, data, items){
                    assert(!err, "getFeedForUser returned an error");
                    assert(data, "getFeedForUser returned no data object")
                    assert(items, "getFeedForUser returned no items array")
                    done();
                })
            })
            var testProperty="____test_item"+Math.floor(Math.random()*10000),
                testPropertyValue="test"+Math.floor(Math.random()*10000),
                testPropertyObjectValue={test:testPropertyValue};
            it('set',function(done){
                client.set(testProperty, testPropertyValue);
                done();
            })
            it('get',function(done){
                var retrievedValue=client.get(testProperty);
                assert(retrievedValue===testPropertyValue, "Get is not working properly");
                done();
            })
            it('setObject',function(done){
                client.set(testProperty, testPropertyObjectValue);
                done();
            })
            it('getObject',function(done){
                var retrievedValue=client.get(testProperty);
                assert(retrievedValue==testPropertyObjectValue, "getObject is not working properly");
                done();
            })
            /*it('setToken',function(done){
                client.setToken("dummytoken");
                done();
            })
            it('getToken',function(done){
                assert(client.getToken()==="dummytoken");
                done();
            })*/
            it('remove property',function(done){
                client.set(testProperty);
                assert(client.get(testProperty)===null);
                done();
            })
            var newUser;
            it('signup',function(done){
                client.signup("newUsergridUser", "Us3rgr1d15Aw3s0m3!", "usergrid@apigee.com", "Another Satisfied Developer", function(err, user){
                    assert(!err, "signup returned an error");
                    assert(user, "signup returned no user object")
                    newUser=user;
                    done();
                })
            })
            it('login',function(done){
                client.login("newUsergridUser", "Us3rgr1d15Aw3s0m3!", function(err, data, user){
                    assert(!err, "login returned an error");
                    assert(user, "login returned no user object")
                    done();
                })
            })
            /*it('reAuthenticateLite',function(done){
                client.reAuthenticateLite(function(err){
                    assert(!err, "reAuthenticateLite returned an error");
                    done();
                })
            })*/
            /*it('reAuthenticate',function(done){
                client.reAuthenticate("usergrid@apigee.com", function(err, data, user, organizations, applications){
                    assert(!err, "reAuthenticate returned an error");
                    done();
                })
            })*/
            /*it('loginFacebook',function(done){
                assert(true, "Not sure how feasible it is to test this with Mocha")
                done();
            })*/
            it('isLoggedIn',function(done){
                assert(client.isLoggedIn()===true, "isLoggedIn is not detecting that we have logged in.")
                done();
            })
            it('getLoggedInUser',function(done){
                setTimeout(function(){
                    client.getLoggedInUser(function(err, data, user){
                        assert(!err, "getLoggedInUser returned an error");
                        assert(user, "getLoggedInUser returned no user object")
                        done();
                    })
                },1000);
            })
            before(function(done){
                //please enjoy this musical interlude.
                setTimeout(function(){done()},1000);
            })
            it('logout',function(done){
                client.logout();
                assert(null===client.getToken(), "we logged out, but the token still remains.")
                done();
            })
            it('getLoggedInUser',function(done){
                client.getLoggedInUser(function(err, data, user){
                    assert(err, "getLoggedInUser should return an error after logout");
                    assert(user, "getLoggedInUser should not return data after logout")
                    done();
                })
            })
            it('isLoggedIn',function(done){
                assert(client.isLoggedIn()===false, "isLoggedIn still returns true after logout.")
                done();
            })
            after(function (done) {
                client.request({
                    method: 'DELETE',
                    endpoint: 'users/newUsergridUser'
                }, function (err, data) {
                    done();
                });

            })
            it('buildCurlCall',function(done){
                done();
            })
            it('getDisplayImage',function(done){
                done();
            })
            after(function(done){
                dogEntity.destroy();
                //dogCollection.destroy();
                //testGroup.destroy();
                done();
            })
        });
    });
    describe('Usergrid Entity', function() {
        var client = getClient();
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
            dog=new Usergrid.Entity({client:client,data:options});
            dog.save(function(err, entity) {
                assert(!err, "dog not created");
                done();
            });
        });
        it('should RETRIEVE the dog', function(done) {
            if (!dog) {
                assert(false, "dog not created");
                done();
                return;
            }
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

        before(function(done) {
            //Make sure our dog doesn't already exist
            var options = {
                type: 'dogs',
                qs: {
                    limit: 50
                } //limit statement set to 50
            }

            client.createCollection(options, function(err, response, dogs) {
                if (!err) {
                    assert(!err, "could not retrieve list of dogs: " + dogs.error_description);
                    //we got 50 dogs, now display the Entities:
                    //do doggy cleanup
                    //if (dogs.hasNextEntity()) {
                    while (dogs.hasNextEntity()) {
                        //get a reference to the dog
                        var dog = dogs.getNextEntity();
                        console.warn(dog);
                        //notice('removing dog ' + dogname + ' from database');
                        if(dog === null) continue;
                        dog.destroy(function(err, data) {
                            assert(!err, dog.get('name') + " not removed: " + data.error_description);
                            if (!dogs.hasNextEntity()) {
                                done();
                            }
                        });
                    }
                    //} else {
                    done();
                    //}
                }
            });
        });
        it('should CREATE a new dogs collection', function(done) {
            var options = {
                client:client,
                type: 'dogs',
                qs: {
                    ql: 'order by index'
                }
            }
            dogs=new Usergrid.Collection(options);
            assert(dogs!==undefined&&dogs!==null, "could not create dogs collection");
            done();
        });
        it('should CREATE dogs in the collection', function(done) {
            this.timeout(10000);
            var totalDogs = 30;
            Array.apply(0, Array(totalDogs)).forEach(function(x, y) {
                var dogNum = y + 1;
                var options = {
                    type: 'dogs',
                    name: 'dog' + dogNum,
                    index: y
                }
                dogs.addEntity(options, function(err, dog) {
                    assert(!err, "dog not created");
                    if (dogNum === totalDogs) {
                        done();
                    }
                });
            })
        });
        it('should RETRIEVE dogs from the collection', function(done) {
            while (dogs.hasNextEntity()) {
                //get a reference to the dog
                dog = dogs.getNextEntity();
            }
            if (done) done();
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
        it('should RETRIEVE an entity by UUID.', function(done) {
            var uuid=dogs.getFirstEntity().get("uuid")
            dogs.getEntityByUUID(uuid,function(err, data){
                assert(!err, "getEntityByUUID returned an error.");
                assert(uuid==data.get("uuid"), "We didn't get the dog we asked for.");
                done();
            })
        });
        it('should RETRIEVE the first entity from the collection', function() {
            assert(dogs.getFirstEntity(), "Could not retrieve the first dog");
        });
        it('should RETRIEVE the last entity from the collection', function() {
            assert(dogs.getLastEntity(), "Could not retrieve the last dog");
        });
        it('should reset the paging', function() {
            dogs.resetPaging();
            assert(!dogs.hasPreviousPage(), "Could not resetPaging");
        });
        it('should reset the entity pointer', function() {
            dogs.resetEntityPointer();
            assert(!dogs.hasPrevEntity(), "Could not reset the pointer");
            assert(dogs.hasNextEntity(), "Dog has no more entities");
            assert(dogs.getNextEntity(), "Could not retrieve the next dog");
        });
        it('should RETRIEVE the next entity from the collection', function() {
            assert(dogs.hasNextEntity(), "Dog has no more entities");
            assert(dogs.getNextEntity(), "Could not retrieve the next dog");
        });
        it('should RETRIEVE the previous entity from the collection', function() {
            assert(dogs.getNextEntity(), "Could not retrieve the next dog");
            assert(dogs.hasPrevEntity(), "Dogs has no previous entities");
            assert(dogs.getPrevEntity(), "Could not retrieve the previous dog");
        });
        it('should DELETE the entities from the collection', function(done) {
            this.timeout(20000);
            function remove(){
                if(dogs.hasNextEntity()){
                    dogs.destroyEntity(dogs.getNextEntity(),function(err, data){
                        assert(!err, "Could not destroy dog.");
                        remove();
                    })
                }else if(dogs.hasNextPage()){
                    dogs.getNextPage();
                    remove();
                }else{
                    done();
                }
            }
            remove();
        });
    });
    describe('Usergrid Counters', function() {
        var client = getClient();
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
            });
            assert(counter, "Counter not created");
            done();
        });
        it('should save a counter', function(done) {
            counter.save(function(err, data) {
                assert(!err, data.error_description);
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
                done();
            });
        });
        it('should fetch the counter', function(done) {
            counter.fetch(function(err, data) {
                assert(!err, data.error_description);
                done();
            });
        });
        it('should fetch counter data', function(done) {
            counter.getData({
                resolution: 'all',
                counters: ['test', 'test_counter']
            }, function(err, data) {
                assert(!err, data.error_description);
                done();
            });
        });
    });
    describe('Usergrid Folders and Assets', function() {
        var client = getClient();
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
                console.error(err);
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
                var assets = [];
                if(data && data.entities && data.entities.length){
                    assets.concat(data.entities.filter(function(asset) {
                        return asset.name === filename
                    }));
                }
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
                var folders = [];
                if(data && data.entities && data.entities.length){
                    folders.concat(data.entities.filter(function(folder) {
                        return folder.name === foldername
                    }));
                }
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
            folder = new Usergrid.Folder({
                client: client,
                data: {
                    name: foldername,
                    owner: user.get("uuid"),
                    path: folderpath
                }
            }, function(err, response, folder) {
                assert(!err, err);
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
            }, function(err, response, asset) {
                if(err){
                    assert(false, err);
                }
                done();
            });
        });
        it('should RETRIEVE an asset', function(done) {
            asset.fetch(function(err, response, entity){
                if(err){
                    assert(false, err);
                }else{
                    asset=entity;
                }
                done();
            })
        });
        it('should upload asset data', function(done) {
            this.timeout(5000);
            asset.upload(test_image, function(err, response, asset) {
                if(err){
                    assert(false, err.error_description);
                }
                done();
            });
        });
        it('should retrieve asset data', function(done) {
            this.timeout(5000);
            asset.download(function(err, response, asset) {
                if(err){
                    assert(false, err.error_description);
                }
                assert(asset.get('content-type') == test_image.type, "MIME types don't match");
                assert(asset.get('size') == test_image.size, "sizes don't match");
                done();
            });
        });
        it('should add the asset to a folder', function(done) {
            folder.addAsset({
                asset: asset
            }, function(err, data) {
                if(err){
                    assert(false, err.error_description);
                }
                done();
            })
        });
        it('should list the assets from a folder', function(done) {
            folder.getAssets(function(err, assets) {
                if(err){
                    assert(false, err.error_description);
                }
                done();
            })
        });
        it('should remove the asset from a folder', function(done) {
            folder.removeAsset({
                asset: asset
            }, function(err, data) {
                if(err){
                    assert(false, err.error_description);
                }
                done();
            })
        });
        after(function(done) {
            asset.destroy(function(err, data) {
                if(err){
                    assert(false, err.error_description);
                }
                done();
            })
        });
        after(function(done) {
            folder.destroy(function(err, data) {
                if(err){
                    assert(false, err.error_description);
                }
                done();
            })
        });
    });
});
