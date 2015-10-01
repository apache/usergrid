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
var token = require("../../lib/token")
var users = require("../../lib/users")
var config = require("../../config")
var should = require("should")
var setup = require("../setup")
var async = require("async");

module.exports = {};

module.exports.test = function() {
    describe('reset a user password', function() {
        it('should reset a user\'s password only when the correct old password is provided', function(done) {
            var user = setup.users[0];
            users.login(user.username, user.password, function(err, body) {
                should(err).be.null;
                body.should.have.property('access_token').and.have.lengthOf(63);
                async.parallel({
                        bad: function(cb) {
                            users.resetPassword(user.username, user.password + "_bad", user.password + "_badnew", function(e, r, body) {
                                cb(e, {
                                    r: r,
                                    body: body
                                });
                            });
                        },
                        good: function(cb) {
                            users.resetPassword(user.username, user.password, user.password + "_goodnew", function(e, r, body) {
                                cb(e, {
                                    r: r,
                                    body: body
                                });
                            });
                        }
                    },
                    function(err, results) {
                        results.bad.r.statusCode.should.equal(400);
                        results.bad.body.should.have.property('error').which.equal('auth_invalid_username_or_password');
                        results.bad.body.should.have.property('exception').which.equal('org.apache.usergrid.management.exceptions.IncorrectPasswordException');
                        results.bad.body.should.have.property('error_description').which.equal('Unable to authenticate due to username or password being incorrect');

                        results.good.r.statusCode.should.equal(200);
                        results.good.body.should.have.property('action').which.equal('set user password');
                        results.good.body.should.have.property('duration');

                        done();
                    });

            });
        });
        it('should reset a user\'s password using org credentials', function(done) {
            var user = setup.users[0];
            users.resetPasswordAsAdmin(user.username, user.password, function(e, r, body) {
                r.statusCode.should.equal(200);
                body.should.have.property('action').which.equal('set user password');
                body.should.have.property('duration');

                done();
            });
        })
    });
}
