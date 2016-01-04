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

var entities = require("../lib/entities");
var should = require("should");
var async = require("async");

module.exports = {
    do: function(cb) {
        async.parallel([
                function(cb) {
                    entities.deleteAll('users', function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        body.count.should.equal(0);
                        cb(err);
                    })
                },
                function(cb) {
                    entities.deleteAll('horses', function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        body.count.should.equal(0);
                        cb(err);
                    })
                },
                function(cb) {
                    entities.deleteAll('dogs', function(err, body) {
                        should(err).be.null;
                        body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(0);
                        body.count.should.equal(0);
                        cb(err);
                    })
                }
            ],
            function(err, data) {
                cb(err);
            });
    }
}