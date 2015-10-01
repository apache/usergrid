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
var should = require("should");
var config = require('../../config');

module.exports = {
    test: function() {
        var numberOfRecords = 30;
        var uuid = require("uuid");
        var id = "resttest_"+ uuid.v1().toString().replace("-", "");

        describe("create entities", function() {
            it("should create " + numberOfRecords.toString() + " entities in the " + config.entitiesTestCollection + " collection", function(done) {
                this.slow(numberOfRecords * 500);
                entities.create(config.entitiesTestCollection, numberOfRecords, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(numberOfRecords);
                    body.entities.forEach(function(entity) {
                        entity.should.have.property("uuid").and.match(/(\w{8}(-\w{4}){3}-\w{12}?)/);
                    });
                    done();
                })
            });
            it("should create " + numberOfRecords.toString() + " entities in the " + id + " collection and check for consistency", function(done) {
                this.slow(numberOfRecords * 500);
                entities.createEach(id, numberOfRecords, function(err, bodies) {
                    should(err).be.null;
                    bodies.should.be.an.instanceOf(Array).and.have.lengthOf(numberOfRecords);
                    bodyMap = {};
                    bodies.forEach(function(body){
                        bodyMap[body.uuid] = body;
                    });
                    entities.get(id, numberOfRecords, function (err,entityArray) {
                        should(err).be.null;
                        entityArray.entities.forEach(function(entity){
                            delete(bodyMap[entity.uuid]);
                        });
                        should(Object.keys(bodyMap)).have.lengthOf(0);
                        done();
                    });
                });
            });
        });
    }
};
