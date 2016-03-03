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
var config = require("../config/");
var setup = require("./setup");
var teardown = require("./teardown");
var async = require('async');
var request = require('request');
var colors = require('colors');

describe("** Usergrid REST Integration Tests **", function() {
    before(function(done) {
        this.timeout(30000);
        console.log("    setup");
        setup.do(function(err) {
            should(err).be.null;
            console.log("      ✓".green + " done".grey);
            done();
        })
    });
    describe("authentication", function() {
        require("./authentication/user.js").test();
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

    });
    describe("queries", function() {
        require('./queries/integerComparison.js').test();
    });
    describe("groups", function() {
        require("./groups/groups.js").test();
    });
    describe("notifications", function() {
        require("./notifications/notifications.js").test();
    });
    after(function(done) {
        this.timeout(180000);
        console.log("    teardown (sleep 5 sec before)");
        setTimeout(function(){

            teardown.do(function(err) {
                should(err).be.null;
                console.log("      ✓".green + " done".grey);
                done();
            })

        }, 5000);

    });
});
