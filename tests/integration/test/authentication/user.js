/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

'use strict';
var token = require("../../lib/token")
var config = require("../../config")
var should = require("should")
var setup = require("../setup")
module.exports = {};

module.exports.test = function() {
    describe('get a user token', function() {
        it('should return valid token', function() {
            var user = setup.users[0];
            token.getAppToken(user.username, user.password, function(err, tokenData) {
                should(err).be.null;
                tokenData.should.have.property('access_token').and.have.lengthOf(63);;
                tokenData.should.have.property('expires_in');
                tokenData.should.have.property('expires_in').which.is.a.Number;
                tokenData.user.username.should.equal(user.username)
            });
        });
    });
};