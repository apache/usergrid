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