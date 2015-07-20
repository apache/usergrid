/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';
var users = require("../../lib/users")
var config = require("../../config")
var should = require("should")
var setup = require("../setup")
module.exports = {};

module.exports.test = function() {
    describe('get a management token', function() {
        it('should return valid token', function(done) {
            var admin = setup.admins[0];
            users.login(admin.username, admin.password, function(err, body) {
                should(err).be.null;
                body.should.have.property('access_token').and.have.lengthOf(63);;
                body.should.have.property('expires_in');
                body.should.have.property('expires_in').which.is.a.Number;
                body.user.username.should.equal(admin.username);
                done();
            });
        });
    });
}
