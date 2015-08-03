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
var config = require("../config");
var urls = require("./urls");
var random = require("./random");
var responseLib = require("./response");
var async = require('async');
var request = require("request");
var random = require("./random");


module.exports = {

    create: function(message, cb) {
        var notifierName = config.notifierName + "_" + random.randomString(5);

        // Need to ensure at least one device exists in the devices collection
        request.post({
            url: urls.appendOrgCredentials(urls.appUrl() + "notifiers"),
            json: true,
            body: {
                name: notifierName,
                provider: "noop"
            }
        }, function(e, r, body) {
            var error = responseLib.getError(e, r);
            if(error){
                return cb(error)
            }
            request.post({
                url: urls.appendOrgCredentials(urls.appUrl() + "devices"),
                json: true,
                body: {
                    name: "testDevice"
                }
            }, function(e, r, body) {
                payload = {};
                payload[notifierName] = message;
                request.post({
                    url: urls.appendOrgCredentials(urls.appUrl() + "devices;ql=/notifications"),
                    json: true,
                    body: {
                        payloads: payload
                    }
                }, function(e, r, body) {
                    var error = responseLib.getError(e, r);
                    cb(error, error ? error : body);
                });
            });
        });


    }
};
