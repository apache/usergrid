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

var entities = require("../../lib/entities");
var should = require("should");

module.exports = {
    test: function() {
        var query1 = "where thirdPropertyTypeInt > 30000";
        describe("get horses " + query1, function() {
            it('should return a subset of results ' + query1, function() {
                entities.getWithQuery('horses', query1, 10, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    if (body.entities.length > 0) {
                        body.entities.length.should.be.greaterThan(0).and.lessThan(11);
                        body.entities.forEach(function(entity) {
                            entity.thirdPropertyTypeInt.should.be.greaterThan(30000);
                        });
                    }
                });
            });
        });
        var query2 = "where thirdPropertyTypeInt > 30000 && thirdPropertyTypeInt < 40000";
        describe("get horses " + query2, function() {
            it('should return a subset of results ' + query2, function() {
                entities.getWithQuery('horses', query2, 10, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    if (body.entities.length > 0) {
                        body.entities.length.should.be.greaterThan(0).and.lessThan(11);
                        body.entities.forEach(function(entity) {
                            entity.thirdPropertyTypeInt.should.be.greaterThan(30000).and.lessThan(40000);

                        });
                    }
                });
            });
        });

        var query3 = "select * where location within 10000 of 37.3236882,-121.9373442"; //San Jose Airport
        describe("get horses " + query3, function() {
            it('should return a subset of results ' + query3, function() {
                //add some delay
                setTimeout(function(){
                    entities.getWithQuery('horses', query3, 10, function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array);
                        body.entities.length.should.be.greaterThan(0);
                        if (body.entities.length > 0) {
                            body.entities.length.should.be.greaterThan(0).and.lessThan(11);
                            body.entities.forEach(function(entity) {
                                // Apigee San Jose
                                entity.location.latitude.should.be.equal(37.3338716);
                                entity.location.longitude.should.be.equal(-121.894249);
                            });
                        }
                    });

                }, 2000);

            });
        });

    }
};
