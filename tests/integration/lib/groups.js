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

var request = require("request");
var urls = require("./urls");
var responseLib = require("./response");
module.exports = {};


module.exports.add = function(group, cb) {
    request.put({
        url: urls.appendOrgCredentials(urls.getAppUrl() + "groups/" + group.path),
        json: group
    }, function(err, response, body) {
        var error = responseLib.getError(err, response);
        cb(error, error ? null : body.entities.pop());
    });
};


module.exports.get = function(groupUUID, cb) {
    request.get(urls.appendOrgCredentials(urls.getAppUrl() + "groups/" + groupUUID), function(err, response, body) {
        var json = JSON.parse(body);
        var error = response.statusCode === 404 ? null : responseLib.getError(err, response);
        cb(error, error ? null : response.statusCode === 404 ? null : json.entities.pop());
    })
};