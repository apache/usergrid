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


/**
* Test suite for Client object
*
* @author rod simpson (rod@apigee.com)
*/
//require('assert');
require('should');
var usergrid = require('../lib/usergrid.js');

//first set up the client
var myclient = new usergrid.client(
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
      myclient.request(
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
      var curl = myclient.buildCurlCall(options);
      curl.should.equal('curl -X DELETE https://api.usergrid.com/1hotrod/sandbox/users/aaaaaa');
    });
  });


  describe('POST Method', function(){
    it('should POST without error', function(done){
      myclient.request(
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
      var curl = myclient.buildCurlCall(options);
      curl.should.equal("curl -X POST https://api.usergrid.com/1hotrod/sandbox/users -d '{\"username\":\"aaaaaa\",\"password\":\"abcd1234\"}'");
    });
  });


  describe('PUT Method', function(){
    it('should PUT without error', function(done){
      myclient.request(
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
      var curl = myclient.buildCurlCall(options);
      curl.should.equal("curl -X PUT https://api.usergrid.com/1hotrod/sandbox/users -d '{\"fred\":\"value\"}'");
    });
  });


  describe('GET Method', function(){
    it('should GET without error', function(done){
      myclient.request(
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
      var curl = myclient.buildCurlCall(options);
      curl.should.equal('curl -X GET https://api.usergrid.com/1hotrod/sandbox/users/aaaaaa');
    });
  });

  describe('Login Method', function(){
    it('should Login without error and get token', function(done){
      myclient.login('aaaaaa', 'abcd1234', function(err){
        if (err) throw err;

        //test the token first
        var token = myclient.token;
        myclient.should.have.property('token');

        //make sure we get a user back
        var user = myclient.user;
        var data = user.get();
        data.should.have.property('username');

        //test for logged in user
        if (!myclient.isLoggedIn()) throw err;

        //make a query with the app users token
        myclient.authType = usergrid.APP_USER;

        //do a get on /users
        describe('GET Method', function(){
          it('should GET without error', function(done){
            myclient.request(
              { method:"GET"
              , endpoint:"users"
              } ,done);
          });
        });

        //go back to the
        myclient.authType = usergrid.AUTH_CLIENT_ID;

        //erase the token
        myclient.token = null;
        if (myclient.isLoggedIn()) throw err;

        //reset the token
        myclient.token = token;
        if (!myclient.isLoggedIn()) throw err;

        //clear the logged in user
        myclient.user = null;
        if (myclient.isLoggedIn()) throw err;

        //replace the logged in user
        myclient.user = user;
        if (!myclient.isLoggedIn()) throw err;

        //log the user out
        myclient.logout();
        if (myclient.isLoggedIn()) throw err;

        //tests finished
        done();
      });
    });
  })

});

