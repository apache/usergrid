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
var connections = require("../../lib/connections");
var should = require("should");
var config = require('../../config');
var inflect = require('i')();

module.exports = {
    test: function() {
        describe("create connection", function() {
            it("should connect " + config.genericTestCollection1 + "[0] to " + config.consumableTestCollection + "[0] via the relationship 'consumed'",
                function(done) {
                    this.slow(10000);
                    this.timeout(15000);
                    connections.create(config.genericTestCollection1, config.consumableTestCollection, "consumed", function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(1);
                        body.entities[0].type.should.equal(config.consumableTestCollection);
                        done();
                    })
                });
            it("should connect " + config.genericTestCollection1 + "[0] to " + config.genericTestCollection2 + "[0] via the relationship 'likes'",
                function(done) {
                    this.slow(10000);
                    this.timeout(15000);
                    connections.create(config.genericTestCollection1, config.genericTestCollection2, "likes", function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(1);
                        body.entities[0].type.should.equal(inflect.singularize(config.genericTestCollection2));
                        done();
                    })
                });
        });
    }
};
