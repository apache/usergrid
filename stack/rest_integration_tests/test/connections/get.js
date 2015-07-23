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
        describe("get connections", function() {
            var rel1 = "consumed";
            it("should see " + config.genericTestCollection1 + "[0] connected to " + config.consumableTestCollection + "[0] by the relationship '" + rel1 + "'",
                function(done) {
                    this.slow(10000);
                    this.timeout(15000);
                    connections.get(config.genericTestCollection1, config.consumableTestCollection, rel1, function(err, r) {
                        should(err).be.null;

                        r.from.parent.metadata.connections.should.have.property(rel1)
                        r.from.parent.metadata.connections[rel1].should.equal(
                            util.format("/%s/%s/%s", inflect.pluralize(config.genericTestCollection1), r.from.parent.uuid, rel1)
                        );
                        r.from.parent.type.should.equal(inflect.singularize(config.genericTestCollection1));
                        r.from.related[0].uuid.should.equal(r.to.parent.uuid);
                        r.from.related[0].type.should.equal(inflect.singularize(config.consumableTestCollection));

                        r.to.parent.metadata.connecting.should.have.property(rel1)
                        r.to.parent.metadata.connecting[rel1].should.equal(
                            util.format("/%s/%s/connecting/%s", inflect.pluralize(config.consumableTestCollection), r.to.parent.uuid, rel1)
                        );
                        r.to.parent.type.should.equal(inflect.singularize(config.consumableTestCollection));
                        r.to.related[0].uuid.should.equal(r.from.parent.uuid);
                        r.to.related[0].type.should.equal(inflect.singularize(config.genericTestCollection1));

                        done();
                    })
                });
            var rel2 = "likes";
            it("should see " + config.genericTestCollection1 + "[0] connected to " + config.genericTestCollection2 + "[0] by the relationship '" + rel2 + "'",
                function(done) {
                    this.slow(10000);
                    this.timeout(15000);
                    connections.get(config.genericTestCollection1, config.genericTestCollection2, rel2, function(err, r) {
                        should(err).be.null;

                        r.from.parent.metadata.connections.should.have.property(rel2)
                        r.from.parent.metadata.connections[rel2].should.equal(
                            util.format("/%s/%s/%s", inflect.pluralize(config.genericTestCollection1), r.from.parent.uuid, rel2)
                        );
                        r.from.parent.type.should.equal(inflect.singularize(config.genericTestCollection1));
                        r.from.related[0].uuid.should.equal(r.to.parent.uuid);
                        r.from.related[0].type.should.equal(inflect.singularize(config.genericTestCollection2));

                        r.to.parent.metadata.connecting.should.have.property(rel2)
                        r.to.parent.metadata.connecting[rel2].should.equal(
                            util.format("/%s/%s/connecting/%s", inflect.pluralize(config.genericTestCollection2), r.to.parent.uuid, rel2)
                        );
                        r.to.parent.type.should.equal(inflect.singularize(config.genericTestCollection2));
                        r.to.related[0].uuid.should.equal(r.from.parent.uuid);
                        r.to.related[0].type.should.equal(inflect.singularize(config.genericTestCollection1));

                        done();
                    })
                });
        });
    }
};
