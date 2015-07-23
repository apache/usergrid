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
var async = require("async");
var config = require('../../config');

module.exports = {
    test: function() {
        describe("get entities", function() {
            it("should get 1 entity", function(done) {
                entities.get(config.entitiesTestCollection, 1, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(1);
                    body.count.should.equal(1);
                    done();
                })
            });
            it("should get 4 entities", function(done) {
                entities.get(config.entitiesTestCollection, 4, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(4);
                    body.count.should.equal(4);
                    done();
                })
            });
            it("should get 18 entities", function(done) {
                entities.get(config.entitiesTestCollection, 18, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(18);
                    body.count.should.equal(18);
                    done();
                })
            });
        });
    }
};
