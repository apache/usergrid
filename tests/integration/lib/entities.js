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

var config = require("../config");
var urls = require("./urls");
var responseLib = require("./response");
var async = require('async');
var request = require("request");

module.exports = {
    create: function(collection, entity, numberOfEntities, cb) {
        var url = urls.appendOrgCredentials(urls.getAppUrl() + collection);
        var requestArray = [];
        for (var i = 0; i < numberOfEntities; i++) {
            requestArray.push({
                url: url,
                json: entity
                })
            }
        async.each(requestArray, function(options, cb) {
            request.post(options, function(e, r, body) {
                cb(e);
            });
        }, function(err) {
            request.get({
                url: url + "&limit=" + numberOfEntities,
                json: true
            }, function(e, r, body) {
                var error = responseLib.getError(e, r);
                cb(error, error ? error : body);
            })
        });
    },
    deleteAll: function(collection, cb) {
        var url = urls.appendOrgCredentials(urls.getAppUrl() + collection);
        deleteAllEntities(collection, function(e) {
            request.get({
                url: url,
                json: true
            }, function(e, r, body) {
                var error = responseLib.getError(e, r);
                cb(error, error ? error : body);
            })
        })
    },
    update: function(collection, uuid, body, cb) {
        var url = urls.appendOrgCredentials(urls.getAppUrl() + collection + "/" + uuid);
        request.put({
            url: url,
            body: body,
            json: true
        }, function(e, r, body) {
            var error = responseLib.getError(e, r);
            cb(error, error ? error : body);
        })
    },
    get: function(collection, numberOfEntities, cb) {
        var url = urls.appendOrgCredentials(urls.getAppUrl() + collection + "?limit=" + numberOfEntities.toString());
        request.get({
            url: url,
            json: true
        }, function(e, r, body) {
            var error = responseLib.getError(e, r);
            cb(error, error ? error : body);
        })
    },
    getWithQuery: function(collection, query, numberOfEntities, cb) {
        var url = urls.appendOrgCredentials(urls.getAppUrl() + collection + "?ql=" + encodeURIComponent(query) + "&limit=" + numberOfEntities.toString());
        request.get({
            url: url,
            json: true
        }, function(e, r, body) {
            var error = responseLib.getError(e, r);
            cb(error, error ? error : body);
        })
    }
};

function deleteAllEntities(collection, cb) {
    var url = urls.appendOrgCredentials(urls.getAppUrl() + collection);
    request.get({
        url: url,
        json: true
    }, function(e, r, body) {
        if (body.count === undefined) {
            cb("The 'count' property is not defined at " + url);
        } else if (body.count > 0) {
            var deletes = [];
            for (var i = 0; i < body.count; i++) {
                deletes.push({
                    url: urls.appendOrgCredentials(urls.getAppUrl() + collection + "/" + body.entities[i].uuid),
                    json: true
                });
            }
            async.each(deletes, function(options, cb) {
                request.del(options, function(e) {
                    cb(e);
                });
            }, function(err) {
                setTimeout(function() {
                    deleteAllEntities(collection, function(e) {
                        cb(e);
                    });
                }, 100); // add some delay
            });
        } else {
            cb();
        }
    });
}
