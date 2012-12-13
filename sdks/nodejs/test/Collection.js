/**
* Test suite for Collection object
*  
* TODO: need to add coverage for the following methods:
* 
* addEntity
* getEntityByUUID
* getFirstEntity
* getLastEntity
* resetEntityPointer
* destroyEntity
* resetPaging
* 
* @author rod simpson (rod@apigee.com)
*/
require("assert");
require('should');
var usergrid = require('../lib/usergrid.js');
  
//first set up the client
var client = new usergrid.client(
  { 
    orgName:"1hotrod"
  , appName:"sandbox"
  , authType:"CLIENT_ID"
  , clientId:"b3U6y6hRJufDEeGW9hIxOwbREg"
  , clientSecret:"b3U6X__fN2l9vd1HVi1kM9nJvgc-h5k"
  } 
);

describe('Collection methods - dogs', function(){
  var dogs = {};
  
  describe('make new collection', function(){
    it('should make a new collection without error', function(done){
      var options = {
          type:"dogs"
        , client:client
      }
      dogs = new usergrid.collection(options, done);         
    });  
  });
  
  describe('check collection', function(){
    it('should loop through all collection entities', function(){
      while(dogs.hasNextEntity()) {
        //get a reference to the dog
        var dog = dogs.getNextEntity();
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
          type:"users"
        , client:client
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
  
  describe('make new collection', function(){
    it('should make a new collection without error', function(done){
      var options = {
          type:"users"
        , client:client
        , qs:{"ql":"select * where username ='barney'"}
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
});


var messageeClient = new usergrid.client(
  { 
    orgName:"apigee"
  , appName:"messageeapp"
  , authType:"CLIENT_ID"
  , clientId:"YXA6URHEY2pCEeG23RIxOAoChA"
  , clientSecret:"YXA6ukLeZvwB0JOdmAprY1azi9DtCPY"
  } 
);

describe('Collection methods - users paging', function(){
  var users = {};
  
  describe('make new collection', function(){
    it('should make a new collection without error', function(done){
      var options = {
          type:"users"
        , client:messageeClient
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

