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
'use strict';
var entities = require("../../lib/entities");
var should = require("should");
var async = require('async');
var config = require('../../config');

module.exports = {
    test: function() {
        describe("filter " + config.genericTestCollection1 + " with 'contains' queries", function(done) {
            var query1 = "where consistentProperty contains 'somethingConsistent'";
            var maxNumberOfEntities = Math.max(config.numberOfEntities, 100);
            it('should return ' + config.numberOfEntities + ' results ' + query1, function(done) {
                this.timeout(30000);
                entities.getWithQuery(config.genericTestCollection1, query1, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    body.entities.length.should.equal(config.numberOfEntities);
                    body.entities.forEach(function(entity) {
                        entity.consistentProperty.should.equal('somethingConsistent');
                    });
                    done();
                });
            });
            var query2 = "where consistentProperty contains '*ethi*'";
            // skipping this test for now since it doesn't work in 1.0
            it.skip('should return ' + config.numberOfEntities + ' results ' + query2, function(done) {
                entities.getWithQuery(config.genericTestCollection1, query2, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    body.entities.length.should.equal(config.numberOfEntities);
                    body.entities.forEach(function(entity) {
                        entity.consistentProperty.should.equal('somethingConsistent');
                    });
                    done();
                });
            });
            var query3 = "where optionsProperty contains 'aaa*'";
            // this should be updated when running tests against 2.0 - *aaa* instead of aaa*
            it('should return a subset of results ' + query3, function(done) {
                entities.getWithQuery(config.genericTestCollection1, query3, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    if (body.entities.length > 0) {
                        body.entities.length.should.be.greaterThan(0).and.lessThan(config.numberOfEntities + 1);
                        body.entities.forEach(function(entity) {
                            entity.optionsProperty.should.match(/(\b|^)aaa(\b|$)/);
                        });
                    }
                    done();
                });
            });
            var query4 = "where title contains 'tale'";
            it('should return a subset of results ' + query4, function(done) {
                entities.getWithQuery(config.genericTestCollection1, query4, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    if (body.entities.length > 0) {
                        body.entities.length.should.be.greaterThan(0).and.lessThan(config.numberOfEntities + 1);
                        body.entities.forEach(function(entity) {
                            entity.title.should.match(/tale/i);
                        });
                    }
                    done();
                });
            });
            var query5 = "where title contains 'ta*'";
            it('should return a subset of results ' + query5, function(done) {
                entities.getWithQuery(config.genericTestCollection1, query5, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    if (body.entities.length > 0) {
                        body.entities.length.should.be.greaterThan(0).and.lessThan(config.numberOfEntities + 1);
                        body.entities.forEach(function(entity) {
                            entity.title.should.match(/ta.*/i);
                        });
                    }
                    done();
                });
            });
            var query6 = "where consistentProperty contains 'some*'";
            it('should return a subset of results ' + query6, function() {
                entities.getWithQuery('horses', query6, 10, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    body.entities.length.should.be.greaterThan(0).and.lessThan(11);
                });
            });
            var query7 = "where consistentProperty contains 'ssccxxome*'";
            it('should not return a subset of results ' + query7, function() {
                var query = "where firstProperty contains 'ssccxxome*'";
                entities.getWithQuery('horses', query7, 10, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    body.entities.length.should.be.equal(0);

                });
            });
        });
    }
}
