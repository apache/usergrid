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
var entities = require("../../lib/entities");
var random = require("../../lib/random");
var should = require("should");
var config = require('../../config');

module.exports = {
    test: function() {
        describe("update entity", function() {
            it("should get a random entity and set 'newProperty' to 'BANJO'", function(done) {
                this.timeout(10000);
                this.slow(5000);
                entities.get(config.entitiesTestCollection, random.randomNumber(10), function(err, body) {
                    var payload = {
                        newProperty: "BANJO"
                    }
                    should(body.entities[0].newProperty).not.exist;
                    entities.update(config.entitiesTestCollection, body.entities[body.entities.length - 1].uuid, payload, function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(1);
                        body.entities[0].newProperty.should.equal("BANJO");
                        done();
                    });
                });
            });
        });
    }
};
