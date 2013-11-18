/**
* Test suite for Collection object
*
* TODO: need to add coverage for the following methods:
*
* getFirstEntity
* getLastEntity
* hasPrevEntity
* getPrevEntity
* resetEntityPointer
* resetPaging
*
* Need to add sample data for paging, check actual results
*
* @author rod simpson (rod@apigee.com)
*/
require("assert");
require('should');
var usergrid = require('../lib/usergrid.js');

//first set up the client
var myclient = new usergrid.client({
  orgName:'1hotrod',
  appName:'sandbox',
  authType:'CLIENT_ID',
  clientId:'b3U6y6hRJufDEeGW9hIxOwbREg',
  clientSecret:'b3U6X__fN2l9vd1HVi1kM9nJvgc-h5k',
  logging: true
});

describe('Collection methods - dogs', function(){
  var doggies = {};

  describe('make new collection', function(){
    it('should make a new collection without error', function(done){
      var options = {
        client:myclient,
        path:"dogs"
      }
      doggies = new usergrid.collection(options, done);
    });
  });

  describe('check collection', function(){
    it('should loop through all collection entities', function(){
      while(doggies.hasNextEntity()) {
        //get a reference to the dog
        var dog = doggies.getNextEntity();
        var data = dog.get();
        data.should.have.property('name');
      }
    });
  });
});

describe('Collection methods - users', function(){
  var users = {};

  describe('make new collection', function(){
    it('should make a new collection without error', function(done){
      var options = {
        client:myclient,
        path:'users'
      }
      users = new usergrid.collection(options, done);
    });
  });

  describe('check collection', function(){
    it('should loop through all collection entities', function(){
      while(users.hasNextEntity()) {
        //get a reference to the dog
        var user = users.getNextEntity();
        var data = user.get();
        data.should.have.property('username');
      }
    });
  });
});

describe('Collection methods - 1 user - barney', function(){
  var users = {};
  var uuid = '';
  var user_barney = {};

  describe('make new collection', function(){
    it('should make a new collection without error', function(done){
      var options = {
        client:myclient,
        path:'users',
        qs:{"ql":"select * where username ='barney'"}
      }
      users = new usergrid.collection(options, done);
    });
  });

  describe('check collection', function(){
    it('should loop through all collection entities', function(){
      while(users.hasNextEntity()) {
        //get a reference to the dog
        var user = users.getNextEntity();
        var data = user.get();
        data.should.have.property('username', 'barney');
      }
    });
  });

  describe('Add 1 user to collection', function(){
    it('should make a new user and add it to collection without error', function(done){
      //first delete the user if he exists (no assertion as the data may or may not be there)
      myclient.request({
        method:'DELETE',
        endpoint:'users/fredflintster'
        }, function(err) {
          /// new entity creation
          var data = {
          	type:'users',
            username: 'fredflintster',
            password: 'barney',
            email: 'email@myemail.com'
          };
          var options = {
            client:myclient,
            data:data
          };
          user_barney = new Entity(options);
          users.addEntity(user_barney, done);
        });
    });
  });

  describe('Get 1 user from collection', function(){
    it('should return user without error', function(done){
      //make sure we get the uuid from barney
      var data = user_barney.get();
      data.should.have.property('uuid');

      var uuid =  user_barney.get('uuid');
      users.getEntityByUUID(uuid, function(err, data, user) {
        user_barney = user;
        var data = user_barney.get();
        data.should.have.property('uuid');
        uuid = user_barney.get('uuid');
        done();
      });
    });
  });

  describe('remove entity from collection', function(){
    it('should remove entity from collection without error', function(done){
      users.destroyEntity(user_barney, done);
    });
  });

});





var messageeClient = new usergrid.client({
  orgName:'apigee',
  appName:'messageeapp',
  authType:'CLIENT_ID',
  clientId:'YXA6URHEY2pCEeG23RIxOAoChA',
  clientSecret:'YXA6ukLeZvwB0JOdmAprY1azi9DtCPY',
  logging: true
});

describe('Collection methods - users paging', function(){
  var users = {};

  describe('make new collection', function(){
    it('should make a new collection without error', function(done){
      var options = {
        path:'users',
        client:messageeClient
      }
      users = new usergrid.collection(options, done);
    });
  });

  describe('check collection', function(){
    it('should loop through all collection entities', function(){
      while(users.hasNextEntity()) {
        //get a reference to the dog
        var user = users.getNextEntity();
        var data = user.get();
        data.should.have.property('username');
        console.log(data.username);
      }
    });
  });

  describe('get next page', function(){
    it('should get next page of users', function(done){
      console.log('starting next page test');
      if (users.hasNextPage()) {
        console.log('next page - yes');
        users.getNextPage(done);
      } else {
        done();
      }
    });
  });

  describe('check collection', function(){
    it('should loop through all collection entities', function(){
      while(users.hasNextEntity()) {
        //get a reference to the dog
        var user = users.getNextEntity();
        var data = user.get();
        data.should.have.property('username');
        console.log(data.username);
      }
    });
  });

  describe('get previous page', function(){
    it('should get previous page of users', function(done){
      if (users.hasPreviousPage()) {
        users.getPreviousPage(done);
      } else {
        done();
      }
    });
  });

  describe('check collection', function(){
    it('should loop through all collection entities', function(){
      while(users.hasNextEntity()) {
        //get a reference to the dog
        var user = users.getNextEntity();
        var data = user.get();
        data.should.have.property('username');
        console.log(data.username);
      }
    });
  });

});

