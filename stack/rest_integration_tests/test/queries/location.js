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
var response = require('../../lib/response');

module.exports = {
    test: function() {
        describe("filter " + config.genericTestCollection2 + " with location queries", function(done) {
            var locationString = config.location.latitude + ", " + config.location.longitude;
            var query = "location within 1000 of " + locationString;
            maxNumberOfEntities = Math.max(config.numberOfEntities, 100);
            it("should return all results with a location within 1000m of " + locationString, function(done) {
                entities.getWithQuery(config.genericTestCollection2, query, maxNumberOfEntities, function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array);
                    body.entities.forEach(function(entity) {
                        var distance = response.distanceInMeters(config.location, entity.location);
                        distance.should.be.lessThan(1000);
                    });
                    done();
                });
            });
        });
    }
}
