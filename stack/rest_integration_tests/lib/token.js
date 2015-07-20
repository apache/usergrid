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
var responseLib = require("./response");
var request = require("request");

module.exports= {
    getOrgToken: function (cb) {
        var managementUrl = urls.managementUrl();

        var options = {
            uri: managementUrl + "token",
            method: 'POST',
            json: {client_id: config.org.clientId, client_secret: config.org.clientSecret, grant_type: "client_credentials"}
        };
        request(options, function (err, response, body) {
            var error = responseLib.getError(err,response);
            cb(error, body);
        });
    },
    getManagementToken: function (username, password, cb) {
        var managementUrl = urls.managementUrl();
        var options = {
            uri: managementUrl + "token",
            method: 'POST',
            json: {username: username, password: password, grant_type: "password"}
        };
        request.post(options, function (err, response, body) {
            var error = responseLib.getError(err,response);
            cb(error,body);
        });
    }

};
