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

// here we're going to do teardown of BaaS environment - deletion of users, entities, etc.
var users = require("../lib/users");
var entities = require("../lib/entities");
var config = require("../config");
var async = require("async");
var uuid = require("uuid");
var random = require("../lib/random");

module.exports = {
    users: [],
    admins: [],
    do: function(cb) {
        async.parallel([
                function(cb) {
                    // create admin user
                    var id = uuid.v1().toString().replace("-", "");
                    var admin = {
                        username: id + "user",
                        password: "pwd" + id,
                        name: id + " name",
                        email: id + "@uge2e.com"
                    };
                    users.add(admin, function(err, user) {
                        //console.log(user);
                        users.addToRole(user.username, "admin", function(err) {
                            module.exports.admins.push(admin);
                            cb(err, err ? null : admin);
                        });
                    });
                },
                function(cb) {
                    // create app users
                    var size = config.numberOfUsers;
                    var userArr = [];
                    module.exports.users = userArr;
                    for (var i = 0; i < size; i++) {
                        var id = uuid.v1().toString().replace("-", "");
                        userArr.push({
                            username: id + "user",
                            password: "pwd" + id,
                            name: id + "name",
                            email: id + "@uge2e.com"
                        });
                    }
                    async.eachSeries(
                        userArr,
                        function(user, cb) {
                            users.add(user, function(err, user) {
                                module.exports.users.push(user);
                                cb(err, user);
                            });
                        },
                        function(err, localUsers) {
                            cb(err);
                        }
                    )
                },
                function(cb) {
                    // create entities
                    var numberOfRecords = 20;
                    var entity = {
                        firstProperty: "somethingConsistent",
                        secondProperty: "somethingRandom: " + random.randomString(10),
                        thirdPropertyTypeInt: random.randomNumber(5),
                        location: {  // Apigee San Jose
                            latitude: 37.3338716,
                            longitude: -121.894249
                        }
                    };
                    async.series([
                            function(cb) {

                                entities.create('dogs', entity, numberOfRecords, function(err, body) {
                                    cb(err);
                                });
                            },
                            function(cb) {
                                entities.create('horses', entity, numberOfRecords, function(err, body) {
                                    cb(err);
                                });
                            }
                        ],
                        function(err, data) {
                            cb(err);
                        });
                }
            ],
            function(err, data) {
                cb(err);
            });
    }
};