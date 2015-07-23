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
var async = require('async');
var config = require('../../config');

module.exports = {
    test: function() {
        describe("filter " + config.genericTestCollection2 + " with '>' and '<' queries", function() {
            var query = "where intProperty > 30000";
            numberOfEntities = Math.min(config.numberOfEntities, 10);
            it('should return a subset of results ' + query, function(done) {
                this.timeout(10000);
                entities.getWithQuery(config.genericTestCollection2, query, numberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    if (body.entities.length > 0) {
                        body.entities.length.should.be.greaterThan(0).and.lessThan(numberOfEntities + 1);
                        body.entities.forEach(function(entity) {
                            entity.intProperty.should.be.greaterThan(30000);
                        });
                    }
                    done();
                });
            });
            var query = "where intProperty > 30000 && intProperty < 40000";
            it('should return a subset of results ' + query, function(done) {
                this.timeout(10000);
                entities.getWithQuery(config.genericTestCollection2, query, numberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    if (body.entities.length > 0) {
                        body.entities.length.should.be.greaterThan(0).and.lessThan(numberOfEntities + 1);
                        body.entities.forEach(function(entity) {
                            entity.intProperty.should.be.greaterThan(30000).and.lessThan(40000);
                        });
                    }
                    done();
                });
            });
        });
    }
};
