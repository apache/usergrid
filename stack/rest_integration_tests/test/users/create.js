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
var should = require("should");
var uuid = require("uuid");
var users = require("../../lib/users");

module.exports = {
    test: function() {
        describe("create a user", function() {
            var username = "testuser" + uuid.v1();
            var password = "password";
            it("should successfully return a user object", function(done) {
                users.add({
                    username: username,
                    password: password,
                    name: username + " name",
                    email: username + "@uge2e.com"
                }, function(err, userBody) {
                    should(err).be.null;
                    userBody.should.not.be.null;
                    users.login(username, password, function(err, body) {
                        should(err).be.null;
                        body.should.not.be.null;
                        body.should.have.property("access_token");
                        done();
                    })
                });
            });
        });
    }
}
