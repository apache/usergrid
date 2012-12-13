/**
* Test suite for Client object
*  
* TODO: need to add coverage for the following methods:
* 
* login
* isLoggedInAppUser
* buildCurlCall
* getToken
* setToken
* getLoggedInUser
* setLoggedInUser
* logoutAppUser
* 
* @author rod simpson (rod@apigee.com)
*/
require("assert");
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
  describe('POST Method', function(){
    it('should POST without error', function(done){
      client.request(
        { method:"POST"
        , endpoint:"users"
        , body:{'username':'aaaaaa'}
        } ,done);
    })
  })
  describe('PUT Method', function(){
    it('should PUT without error', function(done){
      client.request(
        { method:"PUT"
        , endpoint:"users/aaaaaa"
        , body:{'fred':'value'}
        } ,done);
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
});

