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
var entities = require("../lib/entities");
var should = require("should");
var async = require("async");
var config = require("../config");
var inflect = require("i")();

module.exports = {
    do: function(cb) {
        console.log("    teardown");
        async.parallel([
                function(cb) {
                    entities.deleteAll(config.usersCollection, function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        body.count.should.equal(0);
                        cb(err);
                    })
                },
                function(cb) {
                    entities.deleteAll(config.genericTestCollection1, function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        body.count.should.equal(0);
                        cb(err);
                    })
                },
                function(cb) {
                    entities.deleteAll(config.genericTestCollection2, function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        body.count.should.equal(0);
                        cb(err);
                    })
                },
                function(cb) {
                    entities.deleteAll(inflect.pluralize(config.consumableTestCollection), function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        body.count.should.equal(0);
                        cb(err);
                    })
                }
            ],
            function(err, data) {
                console.log("      ✓".green + " done".grey);
                if (cb && typeof(cb) === 'function') cb(err);
            });
    }
}
