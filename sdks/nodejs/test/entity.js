/**
* Test suite for Entity object
*
* Run with mocha v. 1.7.x
* http://visionmedia.github.com/mocha/
*
* @author rod simpson (rod@apigee.com)
*/
require("assert");
var usergrid = require('../lib/usergrid.js');

//first set up the client
var client = new usergrid.client({
  orgName:'1hotrod',
  appName:'sandbox',
  authType:'CLIENT_ID',
  clientId:'b3U6y6hRJufDEeGW9hIxOwbREg',
  clientSecret:'b3U6X__fN2l9vd1HVi1kM9nJvgc-h5k'
});

describe('Entity methods', function(){
  var dog = new usergrid.entity({
    client:client,
    data:{type:"dogs"}
  });
  describe('save method', function(){
    it('should save without error', function(done){
      dog.set('name','dougy');
      dog.save(done);
    });
  });
  describe('fetch method', function(){
    it('should fetch without error', function(done){
      dog.fetch(done);
    });
  });
  describe('destroy method', function(){
    it('should destroy without error', function(done){
      dog.destroy(done);
    });
  });
});