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
        describe("sort " + config.genericTestCollection1 + " with 'order by' queries", function(done) {
            var query1 = "order by created desc";
            it('should return a subset of results ' + query1.replace('order', 'ordered'), function(done) {
                entities.getWithQuery(config.genericTestCollection1, query1, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    var comparisonArray = body.entities;
                    comparisonArray.sort(function(a, b) {
                        return a.created + b.created;
                    });
                    body.entities.should.equal(comparisonArray);
                    done();
                });
            });
            var query2 = "order by created asc";
            it('should return a subset of results ' + query2.replace('order', 'ordered'), function(done) {
                entities.getWithQuery(config.genericTestCollection1, query2, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    var comparisonArray = body.entities;
                    comparisonArray.sort(function(a, b) {
                        return a.created - b.created;
                    });
                    body.entities.should.equal(comparisonArray);
                    done();
                });
            });
            var query3 = "order by optionsProperty desc";
            it('should return a subset of results ' + query3.replace('order', 'ordered'), function(done) {
                entities.getWithQuery(config.genericTestCollection1, query3, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    var comparisonArray = body.entities;
                    comparisonArray.sort(function(a, b) {
                        if (a.optionsProperty > b.optionsProperty) return -1;
                        if (a.optionsProperty < b.optionsProperty) return 1;
                        return 0;
                    });
                    body.entities.should.equal(comparisonArray);
                    done();
                });
            });
            var query4 = "order by optionsProperty asc";
            it('should return a subset of results ' + query4.replace('order', 'ordered'), function(done) {
                entities.getWithQuery(config.genericTestCollection1, query4, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    var comparisonArray = body.entities;
                    comparisonArray.sort(function(a, b) {
                        if (a.optionsProperty < b.optionsProperty) return -1;
                        if (a.optionsProperty > b.optionsProperty) return 1;
                        return 0;
                    });
                    done();
                });
            });
        });
    }
}
