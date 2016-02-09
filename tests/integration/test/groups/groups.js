/**
 * Created by russo on 2/4/16.
 */
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

var should = require("should");
var uuid = require("uuid");
var users = require("../../lib/users");
var devices = require("../../lib/devices");
var groups = require("../../lib/groups");
var connections = require("../../lib/connections");
var async = require('async');


var DEVICE_TOKEN = "APA91bGxRGnMK8tKgVPzSlxtCFvwSVqx0xEPjA06sBmiK0kQsiwUt6ipSYF0iPRHyUgpXle0P8OlRWJADkQrcN7yxG4pLMg1CVmrqDu8tfSe63mZ-MRU2IW0cOhmosqzC9trl33moS3OvT7qjDjkP4Qq8LYdwwYC5A";

module.exports = {
    test: function () {

        var username = "groupuser";
        var password = "password";
        var usersArray = [];
        for (var i = 0; i < 5; i++) {
            usersArray.push({
                "username": username + "-" + i,
                "password": password,
                "name": username + "-" + i,
                "email": username + "-" + i + "@uge2e.com"
            });
        }

        // build devices
        var name = "device";
        var devicesArray = [];
        for (var j = 0; j < 5; j++) {
            devicesArray.push({
                "name": name + "-" + j,
                "gcm.notifier.id": DEVICE_TOKEN
            });
        }


        describe("users", function () {
            it("should create some users", function (done) {
                this.slow(2000);
                async.each(usersArray, function (user, cb) {
                    users.add(user, function (err, user) {
                        should(err).be.null;
                        user.should.not.be.null;
                        cb(err, user);
                    });
                }, function (err) {

                    done();

                });

            })

        });


        describe("groups", function () {
            it("should create some groups", function (done) {
                this.slow(2000);
                var group1 = {
                    path: "group1"
                };

                var group2 = {
                    path: "group2"
                };

                groups.add(group1, function (err) {
                    if (err) {
                        console.log("failed to create " + "group1:" + err);
                    }

                });

                groups.add(group2, function (err) {
                    if (err) {
                        console.log("failed to create " + "group2:" + err);
                    }
                });

                done();

            })

        });


        describe("groups<->users", function () {
            it("should connect users to groups", function (done) {
                this.slow(2000);
                async.each(usersArray, function (user, cb) {

                    async.series([
                        function (cb) {
                            connections.connect("groups", "group1", "users", user.username, null, function (err) {
                                cb(err, user);
                            });

                        },
                        function (cb) {
                            connections.connect("groups", "group2", "users", user.username, null, function (err) {
                                cb(err, user);

                            });
                        }

                    ], function (err, results) {

                        cb(err);

                    });

                }, function (err) {
                    done();
                });

            })

        });


        // SEND NOTIFICATIONS HERE AND VALIDATE THE NUMBER OF NOTIFICATIONS SENT ARE ACCURATE FOR QUERY


    }
};
