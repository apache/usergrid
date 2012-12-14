/**
* Test suite for Client object
* 
* @author rod simpson (rod@apigee.com)
*/
//require('assert');
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

describe('Standard Requests', function(){
  describe('DELETE Method', function(){
    it('should DELETE without error', function(done){
      client.request(
        { method:"DELETE"
        , endpoint:"users/aaaaaa"
        } ,done);
    });
  });
  describe('cURL DELETE Method', function(){
    it('should create a valid cURL calll for the DELETE without error', function(){
      var options = {
        method:"DELETE"
        , uri:"https://api.usergrid.com/1hotrod/sandbox/users/aaaaaa"
      }
      var curl = client.buildCurlCall(options);
      curl.should.equal('curl -X DELETE https://api.usergrid.com/1hotrod/sandbox/users/aaaaaa');
    });
  });
  
  
  describe('POST Method', function(){
    it('should POST without error', function(done){
      client.request(
        { method:"POST"
        , endpoint:"users"
        , body:{'username':'aaaaaa', 'password':'abcd1234'}
        } ,done);
    })
  });
  describe('cURL POST Method', function(){
    it('should create a valid cURL calll for the POST without error', function(){
      var options = {
        method:"POST"
        , uri:"https://api.usergrid.com/1hotrod/sandbox/users"
        , body:{'username':'aaaaaa', 'password':'abcd1234'}
      }
      var curl = client.buildCurlCall(options);
      curl.should.equal("curl -X POST https://api.usergrid.com/1hotrod/sandbox/users -d '{\"username\":\"aaaaaa\",\"password\":\"abcd1234\"}'");
    });
  });
  
  
  describe('PUT Method', function(){
    it('should PUT without error', function(done){
      client.request(
        { method:"PUT"
        , endpoint:"users/aaaaaa"
        , body:{'fred':'value'}
        } ,done);
    });
  });
  describe('cURL PUT Method', function(){
    it('should create a valid cURL calll for the PUT without error', function(){
      var options = {
        method:"PUT"
        , uri:"https://api.usergrid.com/1hotrod/sandbox/users"
        , body:{'fred':'value'}
      }
      var curl = client.buildCurlCall(options);
      curl.should.equal("curl -X PUT https://api.usergrid.com/1hotrod/sandbox/users -d '{\"fred\":\"value\"}'");
    });
  });
  
  
  describe('GET Method', function(){
    it('should GET without error', function(done){
      client.request(
        { method:"GET"
        , endpoint:"users/aaaaaa"
        } ,done);
    });
  });
  describe('cURL GET Method', function(){
    it('should create a valid cURL calll for the GET without error', function(){
      var options = {
        method:"GET"
        , uri:"https://api.usergrid.com/1hotrod/sandbox/users/aaaaaa"
      }
      var curl = client.buildCurlCall(options);
      curl.should.equal('curl -X GET https://api.usergrid.com/1hotrod/sandbox/users/aaaaaa');
    });
  });
  
  describe('Login Method', function(){
    it('should Login without error and get token', function(done){
      client.login('aaaaaa', 'abcd1234', function(err){
        if (err) throw err;
        
        //test the token first
        var token = client.getToken();
        client.should.have.property('_token');
        
        //make sure we get a user back
        var user = client.getLoggedInUser(); 
        var data = user.get();
        data.should.have.property('username');
        
        //test for logged in user
        if (!client.isLoggedInAppUser()) throw err;
        
        //erase the token
        client.setToken(null);
        if (client.isLoggedInAppUser()) throw err;
        
        //reset the token
        client.setToken(token);
        if (!client.isLoggedInAppUser()) throw err;
        
        //clear the logged in user
        client.setLoggedInUser(null);
        if (client.isLoggedInAppUser()) throw err;
        
        //replace the logged in user
        client.setLoggedInUser(user);
        if (!client.isLoggedInAppUser()) throw err;
        
        //log the user out
        client.logoutAppUser();
        if (client.isLoggedInAppUser()) throw err;
        
        //tests finished
        done();
      });
    });
  })

});

