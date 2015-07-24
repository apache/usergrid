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
var util = require('util');
var inflect = require('i')();

module.exports = {
    test: function() {
        describe("delete connections", function() {
            var rel1 = "consumed";
            it("should delete the '" + rel1 + "' connection between " + config.genericTestCollection1 + "[0] and " + config.consumableTestCollection + "[0]",
                function(done) {
                    this.slow(10000);
                    this.timeout(15000);
                    connections.delete(config.genericTestCollection1, config.consumableTestCollection, rel1, function(err, r) {
                        should(err).be.null;
                        if (r.from.parent.metadata.hasOwnProperty("connections")) {
                            r.from.parent.metadata.connections.should.not.have.property(rel1);
                        } else {
                            r.from.parent.metadata.should.not.have.property("connections");
                        }
                        r.from.parent.metadata.should.not.have.property("connecting");
                        r.from.related.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        if (r.to.parent.metadata.hasOwnProperty("connecting")) {
                            r.to.parent.metadata.connecting.should.not.have.property(rel1);
                        } else {
                            r.to.parent.metadata.should.not.have.property("connecting");
                        }
                        r.to.related.should.be.an.instanceOf(Array).and.have.lengthOf(0);

                        done();
                    })
                });
            var rel2 = "likes";
            it("should delete the '" + rel2 + "' connection between " + config.genericTestCollection1 + "[0] and " + config.genericTestCollection2 + "[0]",
                function(done) {
                    this.slow(10000);
                    this.timeout(15000);
                    connections.delete(config.genericTestCollection1, config.genericTestCollection2, rel2, function(err, r) {
                        should(err).be.null;
                        if (r.from.parent.metadata.hasOwnProperty("connections")) {
                            r.from.parent.metadata.connections.should.not.have.property(rel2);
                        } else {
                            r.from.parent.metadata.should.not.have.property("connections");
                        }
                        r.from.parent.metadata.should.not.have.property("connecting");
                        r.from.related.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        if (r.to.parent.metadata.hasOwnProperty("connecting")) {
                            r.to.parent.metadata.connecting.should.not.have.property(rel2);
                        } else {
                            r.to.parent.metadata.should.not.have.property("connecting");
                        }
                        r.from.parent.metadata.should.not.have.property("connections");
                        r.to.related.should.be.an.instanceOf(Array).and.have.lengthOf(0);

                        done();
                    })
                });
        });
    }
};
