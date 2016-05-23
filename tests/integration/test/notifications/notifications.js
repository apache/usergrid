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
var notifiers = require("../../lib/notifiers");
var notifications = require("../../lib/notifications");
var connections = require("../../lib/connections");
var async = require('async');

var GOOGLE_API_KEY = "AIzaSyCIH_7WC0mOqBGMOXyQnFgrBpOePgHvQJM";
var ANDROID_DEVICE_TOKEN = "APA91bGxRGnMK8tKgVPzSlxtCFvwSVqx0xEPjA06sBmiK0kQsiwUt6ipSYF0iPRHyUgpX" +
    "le0P8OlRWJADkQrcN7yxG4pLMg1CVmrqDu8tfSe63mZ-MRU2IW0cOhmosqzC9trl33moS3OvT7qjDjkP4Qq8LYdwwYC5A";

module.exports = {
    test: function () {

        var username = "notificationuser";
        var password = "password";
        var usersArray = [];
        for (var i = 0; i < 5; i++) {
            usersArray.push({
                "username": username + "-" + i,
                "password": password,
                "name": username + "-" + i,
                "email": username + "-" + i + "@uge2e.com",
                "number": i
            });
        }

        // build devices
        var deviceName = "notificationdevice";
        var devicesArray = [];
        for (var j = 0; j < 5; j++) {
            devicesArray.push({
                "name": deviceName + "-" + j,
                "gcm.notifier.id": ANDROID_DEVICE_TOKEN,
                "number": i
            });
        }

        var notifiersArray = [];
        var notifier = {
            name: "gcm",
            provider: "google",
            environment: "environment",
            apiKey: GOOGLE_API_KEY
        };
        notifiersArray.push(notifier);


        var gcmNotification = {

            payloads: {
                gcm: "Usergrid Integration Push Test - GCM"
            },
            debug: true,
            priority: 'high'
        };


        describe("notifiers -> GCM", function () {
            it("should create a GCM notifier", function (done) {
                this.slow(5000);
                async.each(notifiersArray, function (notifier, cb) {
                    notifiers.add(notifier, function (err, notifier) {
                        should(err).be.null;
                        notifier.should.not.be.null;
                        cb(err, notifier);
                    });
                }, function (err) {

                    done();

                });

            })

        });

        describe("users", function () {
            it("should create some users", function (done) {
                this.slow(5000);

                async.eachSeries(usersArray, function (user, cb) {
                    users.add(user, function (err, user) {
                        should(err).be.null;
                        user.should.not.be.null;
                        cb(err, user);
                    });
                }, function (err) {

                    setTimeout(function() {

                        // wait a second before proceeding
                        done();

                    }, 1000);


                });

            })

        });


        describe("devices", function () {
            it("should create some devices", function (done) {
                this.slow(5000);
                async.each(devicesArray, function (device, cb) {
                    devices.add(device, function (err, device) {
                        should(err).be.null;
                        device.should.not.be.null;
                        cb(err, device);
                    });

                }, function (err) {

                    setTimeout(function() {

                        // wait a second before proceeding
                        done();

                    }, 1000);

                });

            })

        });


        describe("user<->devices", function () {
            it("should connect devices to users", function (done) {
                this.slow(5000);
                async.eachSeries(usersArray, function (user, cb) {
                    connections.connect("users", user.username, "devices", devicesArray[user.number].name,
                        null, function (err) {
                            cb(err);
                    });
                }, function (err) {

                    if (err) {
                        console.log("error adding users " + err);
                    }
                    setTimeout(function() {

                        // wait a second before proceeding
                        done();

                    }, 1000);
                });

            })

        });


        describe("groups", function () {
            it("should create some groups", function (done) {
                this.slow(5000);
                var group1 = {
                    path: "notificationgroup1"
                };

                var group2 = {
                    path: "notificationgroup2"
                };

                async.series([
                    function (cb) {

                        groups.add(group1, function (err) {
                            if (err) {
                                console.log("failed to create " + "notificationgroup1:" + err);
                            }
                            cb(err);

                        });
                    }, function (cb) {

                        groups.add(group2, function (err) {
                            if (err) {
                                console.log("failed to create " + "notificationgroup2:" + err);
                            }
                            cb(err);
                        });


                    }
                ], function (err, results) {

                    setTimeout(function() {

                        // wait a second before proceeding
                        done();

                    }, 1000);

                });


            })

        });


        describe("groups<->users", function () {
            it("should connect users to groups", function (done) {
                this.slow(5000);
                async.each(usersArray, function (user, cb) {

                    async.series([
                        function (cb) {
                            connections.connect("groups", "notificationgroup1", "users", user.username, null,
                                function (err) {
                                    cb(err, user);
                            });

                        },
                        function (cb) {
                            connections.connect("groups", "notificationgroup2", "users", user.username, null,
                                function (err) {
                                    cb(err, user);

                            });
                        }

                    ], function (err, results) {

                        cb(err);

                    });

                }, function (err) {
                    setTimeout(function() {

                        // wait a second before proceeding
                        done();

                    }, 1000);
                });

            })

        });


        // SEND NOTIFICATIONS HERE AND VALIDATE THE NUMBER OF NOTIFICATIONS SENT ARE ACCURATE FOR QUERY

        describe("notification -> user - direct path", function () {
            it("should send a single notification to a user", function (done) {
                this.timeout(5000)
                this.slow(5000);
                setTimeout(function () {

                    notifications.send("users/" + usersArray[0].username, gcmNotification,
                        function (err, notification) {
                            should(err).be.null;
                            notification.should.not.be.null;
                            setTimeout(function() {

                                // wait a second before proceeding
                                done();

                            }, 2000);

                    });

                }, 1500)


            })

        });

        describe("notification -> user - via matrix query", function () {
            it("should send a single notification to a user", function (done) {
                this.timeout(5000)
                this.slow(5000);

                setTimeout(function () {

                    notifications.send("users;ql=select * where username = 'notificationuser-0'", gcmNotification,
                        function (err, notification) {
                            should(err).be.null;
                            notification.should.not.be.null;
                            setTimeout(function() {

                                // wait a second before proceeding
                                done();

                            }, 2000);

                    });

                }, 1500);


            })

        });

        describe("notification -> groups - via matrix query", function () {
            it("should send a single notification to groups with the same users", function (done) {
                this.timeout(5000)
                this.slow(5000);
                setTimeout(function () {

                    notifications.send("groups;ql=select * where path = 'notificationgroup1' " +
                        "or path = 'notificationgroup2'", gcmNotification, function (err, notification) {

                            should(err).be.null;
                            notification.should.not.be.null;
                        setTimeout(function() {

                            // wait a second before proceeding
                            done();

                        }, 2000);

                    });

                }, 1500);

            })

        });


    }
};
