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
var config = require("../config/");
var setup = require("./setup");
var teardown = require("./teardown");
var async = require('async');
var request = require('request');
var colors = require('colors');

var entities = require('../lib/entities');

describe("baas 2.0 tests", function() {
    before(function(done) {
        setup.do(function() {
            done();
        })
    });
    describe("authentication", function() {
        require("./authentication/user.js").test();
        require("./authentication/resetPassword.js").test();
        require("./authentication/management.js").test();
        require("./authentication/org.js").test();
    });
    describe("users", function() {
        require("./users/create.js").test();
    });
    describe("entities", function() {
        require("./entities/create.js").test();
        require("./entities/get.js").test();
        require("./entities/update.js").test();
        require("./entities/deleteAll.js").test();
    });
    describe("connections", function() {
        require('./connections/create.js').test();
        require('./connections/get.js').test();
        require('./connections/delete.js').test();
    });
    describe("queries", function() {
        require('./queries/equals.js').test();
        require('./queries/contains.js').test();
        require('./queries/order.js').test();
        require('./queries/comparison.js').test();
        require('./queries/location.js').test();
    });
    describe("notifications", function() {
        // Requires an apple notifier to be created in BaaS portal prior to running this test.
        // See: http://apigee.com/docs/app-services/content/creating-notifiers
        require('./notifications/create.js').test();
    });

    after(function(done) {
        this.timeout(40000);
        teardown.do(function() {
            done();
        });
    });
});
