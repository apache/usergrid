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
        describe("filter " + config.genericTestCollection1 + " with '=' and '!=' queries", function(done) {
            var query1 = "where consistentProperty = 'somethingConsistent'";
            maxNumberOfEntities = Math.max(config.numberOfEntities, 100);
            it('should return ' + config.numberOfEntities + ' results ' + query1, function(done) {
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

            var query2 = "where title = 'A Tale of Two Cities'";
            maxNumberOfEntities = Math.max(config.numberOfEntities, 100);
            it('should return ' + config.numberOfEntities + ' results ' + query2, function(done) {
                entities.getWithQuery(config.genericTestCollection1, query2, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    body.entities.length.should.equal(config.numberOfEntities);
                    body.entities.forEach(function(entity) {
                        entity.title.should.equal('A Tale of Two Cities');
                    });
                    done();
                });
            });
        });
    }
}
