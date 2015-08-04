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
var sleep = require('sleep');
module.exports = {
    create: function(fromCollection, toCollection, relationship, cb) {
        async.parallel({
            from: function(cb) {
                request.get({
                    url: urls.appendOrgCredentials(urls.appUrl() + fromCollection),
                    json: true
                }, function(e, r, body) {
                    cb(e, body.entities[0]);
                });
            },
            to: function(cb) {
                request.get({
                    url: urls.appendOrgCredentials(urls.appUrl() + toCollection),
                    json: true
                }, function(e, r, body) {
                    cb(e, body.entities[0]);
                });
            }
        }, function(err, results) {
            var url = urls.appUrl() +
                fromCollection + "/" +
                results.from.uuid + "/" +
                relationship + "/" +
                results.to.uuid;
            url = urls.appendOrgCredentials(url)
            request.post({
                url: url,
                json: true
            }, function(e, r, body) {
                var error = responseLib.getError(e, r);
                cb(error, error ? error : body);
            });
        });
    },
    get: function(fromCollection, toCollection, relationship, cb) {
        async.parallel({
            from: function(cb) {
                request.get({
                    url: urls.appendOrgCredentials(urls.appUrl() + fromCollection + "?limit=1"),
                    json: true
                }, function(e, r, body) {
                    var o = {
                        parent: body.entities[0]
                    }
                    request.get({
                        url: urls.appendOrgCredentials(urls.appUrl() + fromCollection + "/" + o.parent.uuid + "/" + relationship),
                        json: true
                    }, function(e, r, body) {
                        o.related = body.entities;
                        cb(e, o);
                    });
                });
            },
            to: function(cb) {
                request.get({
                    url: urls.appendOrgCredentials(urls.appUrl() + toCollection + "?limit=1"),
                    json: true
                }, function(e, r, body) {
                    var o = {
                        parent: body.entities[0]
                    }
                    request.get({
                        url: urls.appendOrgCredentials(urls.appUrl() + toCollection + "/" + o.parent.uuid + "/connecting/" + relationship),
                        json: true
                    }, function(e, r, body) {
                        o.related = body.entities;
                        cb(e, o);
                    });
                });
            }
        }, function(err, results) {
            cb(err, results);
        });
    },
    delete: function(fromCollection, toCollection, relationship, cb) {
        async.parallel({
            from: function(cb) {
                request.get({
                    url: urls.appendOrgCredentials(urls.appUrl() + fromCollection),
                    json: true
                }, function(e, r, body) {
                    cb(e, body.entities[0]);
                });
            },
            to: function(cb) {
                request.get({
                    url: urls.appendOrgCredentials(urls.appUrl() + toCollection),
                    json: true
                }, function(e, r, body) {
                    cb(e, body.entities[0]);
                });
            }
        }, function(err, results) {
            var url = urls.appUrl() +
                fromCollection + "/" +
                results.from.uuid + "/" +
                relationship + "/" +
                //toCollection + "/" +
                results.to.uuid;
            url = urls.appendOrgCredentials(url);
            sleep.sleep(1);
            request.del({
                url: url,
                json: true
            }, function(e, r, body) {
                sleep.sleep(1);
                module.exports.get(fromCollection, toCollection, relationship, function(err, results) {
                    cb(err, results);
                });
            });
        });
    }
};
