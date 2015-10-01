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

module.exports = {
    create: function(collection, numberOfEntities, cb) {
        var url = urls.appendOrgCredentials(urls.appUrl() + collection);
        var requestArray = []
        geos = random.geo(config.location, 2000, numberOfEntities);
        // console.log(geos);
        for (var i = 0; i < numberOfEntities; i++) {
            requestArray.push({
                consistentProperty: "somethingConsistent",
                randomProperty: "somethingRandom - " + random.randomString(10),
                intProperty: random.randomNumber(5),
                optionsProperty: random.abc(),
                location: geos[i],
                title: "A Tale of Two Cities"
            });
        }
        request.post({
            url: url,
            json: true,
            body: requestArray
        }, function(e, r, body) {
            var error = responseLib.getError(e, r);
            cb(error, error ? error : body);
        });
    },
    createEach: function(collection, numberOfEntities, cb) {
        var url = urls.appendOrgCredentials(urls.appUrl() + collection);
        var requestArray = []
        geos = random.geo(config.location, 2000, numberOfEntities);
        // console.log(geos);
        for (var i = 0; i < numberOfEntities; i++) {
            requestArray.push({
                consistentProperty: "somethingConsistent",
                randomProperty: "somethingRandom - " + random.randomString(10),
                intProperty: random.randomNumber(5),
                optionsProperty: random.abc(),
                location: geos[i],
                title: "A Tale of Two Cities"
            });
        }
        var returnBody = [];
        async.each(requestArray, function(options, cb) {
            request.post({
                url: url,
                json: true,
                body: options
            }, function(e, r, body) {
                var error = responseLib.getError(e, r);
                var entity = body && body.entities ? body.entities.pop() : null;
                entity &&  returnBody.push(entity);
                cb(error, error ? error : entity);
            });
        }, function(err,bodies) {
           cb(err,returnBody);
        });

    },
    deleteAll: function(collection, cb) {
        var url = urls.appendOrgCredentials(urls.appUrl() + collection);
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
        var url = urls.appendOrgCredentials(urls.appUrl() + collection + "/" + uuid);
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
        var url = urls.appendOrgCredentials(urls.appUrl() + collection + "?limit=" + numberOfEntities.toString());
        request.get({
            url: url,
            json: true
        }, function(e, r, body) {
            var error = responseLib.getError(e, r);
            cb(error, error ? error : body);
        })
    },
    getByUuid: function(collection, uuid, cb) {
        var url = urls.appendOrgCredentials(urls.appUrl() + collection + "/"+uuid);
        request.get({
            url: url,
            json: true
        }, function(e, r, body) {
            var error = responseLib.getError(e, r);
            cb(error, error ? error : body);
        })
    },
    getWithQuery: function(collection, query, numberOfEntities, cb) {
        var url = urls.appendOrgCredentials(urls.appUrl() + collection + "?ql=" + encodeURIComponent(query) + "&limit=" + numberOfEntities.toString());
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
    var url = urls.appendOrgCredentials(urls.appUrl() + collection);
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
                    url: urls.appendOrgCredentials(urls.appUrl() + collection + "/" + body.entities[i].uuid),
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
                }, 600); // Mandatory, since it seems to not retrieve entities if you make a request in < 600ms
            });
        } else {
            cb();
        }
    });
}
