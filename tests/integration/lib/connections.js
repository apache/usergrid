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

function buildUrl(collection1, entity1, collection2, entity2, relationship){

    var url = urls.getAppUrl();
    if ( relationship !== null && relationship !== undefined && relationship !== ""){
        url += collection1 + "/" + entity1 + "/" + relationship + "/" + collection2 + "/" + entity2;
    }else{
        url += collection1 + "/" + entity1 + "/" + collection2 + "/" + entity2;
    }

    return urls.appendOrgCredentials(url);
}

module.exports.connect = function(collection1, entity1, collection2, entity2, relationship, cb) {

    var url = buildUrl(collection1, entity1, collection2, entity2, relationship);

    request.post({
        url : url,
        json: {}
    }, function(err, response, body) {
        var error = responseLib.getError(err, response, body);
        cb(error, error ? null : body.entities.pop());
    });
};


module.exports.disconnect = function(collection1, entity1, collection2, entity2, relationship, cb) {

    var url = buildUrl(collection1, entity1, collection2, entity2, relationship);

    request.del(url, function(err, response, body) {
        var json = JSON.parse(body);
        var error = response.statusCode === 404 ? null : responseLib.getError(err, response);
        cb(error, error ? null : response.statusCode === 404 ? null : json.entities.pop());
    })
};


