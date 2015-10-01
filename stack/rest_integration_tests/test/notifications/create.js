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
var should = require("should");
var uuid = require("uuid");
var notifications = require("../../lib/notifications");

module.exports = {
    test: function() {
        // Requires an apple notifier to be created in BaaS portal prior to running this test.
        // See: http://apigee.com/docs/app-services/content/creating-notifiers
        describe("create a notification", function() {
            it("should successfully create a notification", function(done) {
                notifications.create("Hello World!", function(err, body) {
                    should(err).be.null;
                    body.entities.should.be.an.instanceOf(Array).and.have.lengthOf(1);
                    body.entities[0].state.should.equal('FINISHED');
                    done();
                });
            });
        });
    }
}
