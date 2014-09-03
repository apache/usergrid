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
var UsergridEventable = function() {
    throw Error("'UsergridEventable' is not intended to be invoked directly");
};

UsergridEventable.prototype = {
    bind: function(event, fn) {
        this._events = this._events || {};
        this._events[event] = this._events[event] || [];
        this._events[event].push(fn);
    },
    unbind: function(event, fn) {
        this._events = this._events || {};
        if (event in this._events === false) return;
        this._events[event].splice(this._events[event].indexOf(fn), 1);
    },
    trigger: function(event) {
        this._events = this._events || {};
        if (event in this._events === false) return;
        for (var i = 0; i < this._events[event].length; i++) {
            this._events[event][i].apply(this, Array.prototype.slice.call(arguments, 1));
        }
    }
};

UsergridEventable.mixin = function(destObject) {
    var props = [ "bind", "unbind", "trigger" ];
    for (var i = 0; i < props.length; i++) {
        if (props[i] in destObject.prototype) {
            console.warn("overwriting '" + props[i] + "' on '" + destObject.name + "'.");
            console.warn("the previous version can be found at '_" + props[i] + "' on '" + destObject.name + "'.");
            destObject.prototype["_" + props[i]] = destObject.prototype[props[i]];
        }
        destObject.prototype[props[i]] = UsergridEventable.prototype[props[i]];
    }
};

(function() {
    var name = "Logger", global = this, overwrittenName = global[name], exports;
    /* logging */
    function Logger(name) {
        this.logEnabled = true;
        this.init(name, true);
    }
    Logger.METHODS = [ "log", "error", "warn", "info", "debug", "assert", "clear", "count", "dir", "dirxml", "exception", "group", "groupCollapsed", "groupEnd", "profile", "profileEnd", "table", "time", "timeEnd", "trace" ];
    Logger.prototype.init = function(name, logEnabled) {
        this.name = name || "UNKNOWN";
        this.logEnabled = logEnabled || true;
        var addMethod = function(method) {
            this[method] = this.createLogMethod(method);
        }.bind(this);
        Logger.METHODS.forEach(addMethod);
    };
    Logger.prototype.createLogMethod = function(method) {
        return Logger.prototype.log.bind(this, method);
    };
    Logger.prototype.prefix = function(method, args) {
        var prepend = "[" + method.toUpperCase() + "][" + name + "]:	";
        if ([ "log", "error", "warn", "info" ].indexOf(method) !== -1) {
            if ("string" === typeof args[0]) {
                args[0] = prepend + args[0];
            } else {
                args.unshift(prepend);
            }
        }
        return args;
    };
    Logger.prototype.log = function() {
        var args = [].slice.call(arguments);
        method = args.shift();
        if (Logger.METHODS.indexOf(method) === -1) {
            method = "log";
        }
        if (!(this.logEnabled && console && console[method])) return;
        args = this.prefix(method, args);
        console[method].apply(console, args);
    };
    Logger.prototype.setLogEnabled = function(logEnabled) {
        this.logEnabled = logEnabled || true;
    };
    Logger.mixin = function(destObject) {
        destObject.__logger = new Logger(destObject.name || "UNKNOWN");
        var addMethod = function(method) {
            if (method in destObject.prototype) {
                console.warn("overwriting '" + method + "' on '" + destObject.name + "'.");
                console.warn("the previous version can be found at '_" + method + "' on '" + destObject.name + "'.");
                destObject.prototype["_" + method] = destObject.prototype[method];
            }
            destObject.prototype[method] = destObject.__logger.createLogMethod(method);
        };
        Logger.METHODS.forEach(addMethod);
    };
    global[name] = Logger;
    global[name].noConflict = function() {
        if (overwrittenName) {
            global[name] = overwrittenName;
        }
        return Logger;
    };
    return global[name];
})();

(function(global) {
    var name = "Promise", overwrittenName = global[name], exports;
    function Promise() {
        this.complete = false;
        this.error = null;
        this.result = null;
        this.callbacks = [];
    }
    Promise.prototype.then = function(callback, context) {
        var f = function() {
            return callback.apply(context, arguments);
        };
        if (this.complete) {
            f(this.error, this.result);
        } else {
            this.callbacks.push(f);
        }
    };
    Promise.prototype.done = function(error, result) {
        this.complete = true;
        this.error = error;
        this.result = result;
        if (this.callbacks) {
            for (var i = 0; i < this.callbacks.length; i++) this.callbacks[i](error, result);
            this.callbacks.length = 0;
        }
    };
    Promise.join = function(promises) {
        var p = new Promise(), total = promises.length, completed = 0, errors = [], results = [];
        function notifier(i) {
            return function(error, result) {
                completed += 1;
                errors[i] = error;
                results[i] = result;
                if (completed === total) {
                    p.done(errors, results);
                }
            };
        }
        for (var i = 0; i < total; i++) {
            promises[i]().then(notifier(i));
        }
        return p;
    };
    Promise.chain = function(promises, error, result) {
        var p = new Promise();
        if (promises === null || promises.length === 0) {
            p.done(error, result);
        } else {
            promises[0](error, result).then(function(res, err) {
                promises.splice(0, 1);
                if (promises) {
                    Promise.chain(promises, res, err).then(function(r, e) {
                        p.done(r, e);
                    });
                } else {
                    p.done(res, err);
                }
            });
        }
        return p;
    };
    global[name] = Promise;
    global[name].noConflict = function() {
        if (overwrittenName) {
            global[name] = overwrittenName;
        }
        return Promise;
    };
    return global[name];
})(this);

(function() {
    var name = "Ajax", global = this, overwrittenName = global[name], exports;
    function partial() {
        var args = Array.prototype.slice.call(arguments);
        var fn = args.shift();
        return fn.bind(this, args);
    }
    function Ajax() {
        this.logger = new global.Logger(name);
        var self = this;
        function encode(data) {
            var result = "";
            if (typeof data === "string") {
                result = data;
            } else {
                var e = encodeURIComponent;
                for (var i in data) {
                    if (data.hasOwnProperty(i)) {
                        result += "&" + e(i) + "=" + e(data[i]);
                    }
                }
            }
            return result;
        }
        function request(m, u, d) {
            var p = new Promise(), timeout;
            self.logger.time(m + " " + u);
            (function(xhr) {
                xhr.onreadystatechange = function() {
                    if (this.readyState === 4) {
                        self.logger.timeEnd(m + " " + u);
                        clearTimeout(timeout);
                        p.done(null, this);
                    }
                };
                xhr.onerror = function(response) {
                    clearTimeout(timeout);
                    p.done(response, null);
                };
                xhr.oncomplete = function(response) {
                    clearTimeout(timeout);
                    self.logger.timeEnd(m + " " + u);
                    self.info("%s request to %s returned %s", m, u, this.status);
                };
                xhr.open(m, u);
                if (d) {
                    if ("object" === typeof d) {
                        d = JSON.stringify(d);
                    }
                    xhr.setRequestHeader("Content-Type", "application/json");
                    xhr.setRequestHeader("Accept", "application/json");
                }
                timeout = setTimeout(function() {
                    xhr.abort();
                    p.done("API Call timed out.", null);
                }, 3e4);
                xhr.send(encode(d));
            })(new XMLHttpRequest());
            return p;
        }
        this.request = request;
        this.get = partial(request, "GET");
        this.post = partial(request, "POST");
        this.put = partial(request, "PUT");
        this.delete = partial(request, "DELETE");
    }
    global[name] = new Ajax();
    global[name].noConflict = function() {
        if (overwrittenName) {
            global[name] = overwrittenName;
        }
        return exports;
    };
    return global[name];
})();

/*
 *  This module is a collection of classes designed to make working with
 *  the Appigee App Services API as easy as possible.
 *  Learn more at http://Usergrid.com/docs/usergrid
 *
 *   Copyright 2012 Usergrid Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  @author rod simpson (rod@Usergrid.com)
 *  @author matt dobson (matt@Usergrid.com)
 *  @author ryan bridges (rbridges@Usergrid.com)
 */
window.console = window.console || {};

window.console.log = window.console.log || function() {};

function extend(subClass, superClass) {
    var F = function() {};
    F.prototype = superClass.prototype;
    subClass.prototype = new F();
    subClass.prototype.constructor = subClass;
    subClass.superclass = superClass.prototype;
    if (superClass.prototype.constructor == Object.prototype.constructor) {
        superClass.prototype.constructor = superClass;
    }
    return subClass;
}

function propCopy(from, to) {
    for (var prop in from) {
        if (from.hasOwnProperty(prop)) {
            if ("object" === typeof from[prop] && "object" === typeof to[prop]) {
                to[prop] = propCopy(from[prop], to[prop]);
            } else {
                to[prop] = from[prop];
            }
        }
    }
    return to;
}

function NOOP() {}

function isValidUrl(url) {
    if (!url) return false;
    var doc, base, anchor, isValid = false;
    try {
        doc = document.implementation.createHTMLDocument("");
        base = doc.createElement("base");
        base.href = base || window.lo;
        doc.head.appendChild(base);
        anchor = doc.createElement("a");
        anchor.href = url;
        doc.body.appendChild(anchor);
        isValid = !(anchor.href === "");
    } catch (e) {
        console.error(e);
    } finally {
        doc.head.removeChild(base);
        doc.body.removeChild(anchor);
        base = null;
        anchor = null;
        doc = null;
        return isValid;
    }
}

/*
 * Tests if the string is a uuid
 *
 * @public
 * @method isUUID
 * @param {string} uuid The string to test
 * @returns {Boolean} true if string is uuid
 */
var uuidValueRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

function isUUID(uuid) {
    return !uuid ? false : uuidValueRegex.test(uuid);
}

/*
 *  method to encode the query string parameters
 *
 *  @method encodeParams
 *  @public
 *  @params {object} params - an object of name value pairs that will be urlencoded
 *  @return {string} Returns the encoded string
 */
function encodeParams(params) {
    var queryString;
    if (params && Object.keys(params)) {
        queryString = [].slice.call(arguments).reduce(function(a, b) {
            return a.concat(b instanceof Array ? b : [ b ]);
        }, []).filter(function(c) {
            return "object" === typeof c;
        }).reduce(function(p, c) {
            !(c instanceof Array) ? p = p.concat(Object.keys(c).map(function(key) {
                return [ key, c[key] ];
            })) : p.push(c);
            return p;
        }, []).reduce(function(p, c) {
            c.length === 2 ? p.push(c) : p = p.concat(c);
            return p;
        }, []).reduce(function(p, c) {
            c[1] instanceof Array ? c[1].forEach(function(v) {
                p.push([ c[0], v ]);
            }) : p.push(c);
            return p;
        }, []).map(function(c) {
            c[1] = encodeURIComponent(c[1]);
            return c.join("=");
        }).join("&");
    }
    return queryString;
}

/*
 *  method to determine whether or not the passed variable is a function
 *
 *  @method isFunction
 *  @public
 *  @params {any} f - any variable
 *  @return {boolean} Returns true or false
 */
function isFunction(f) {
    return f && f !== null && typeof f === "function";
}

/*
 *  a safe wrapper for executing a callback
 *
 *  @method doCallback
 *  @public
 *  @params {Function} callback - the passed-in callback method
 *  @params {Array} params - an array of arguments to pass to the callback
 *  @params {Object} context - an optional calling context for the callback
 *  @return Returns whatever would be returned by the callback. or false.
 */
function doCallback(callback, params, context) {
    var returnValue;
    if (isFunction(callback)) {
        if (!params) params = [];
        if (!context) context = this;
        params.push(context);
        returnValue = callback.apply(context, params);
    }
    return returnValue;
}

(function(global) {
    var name = "Usergrid", overwrittenName = global[name];
    var VALID_REQUEST_METHODS = [ "GET", "POST", "PUT", "DELETE" ];
    function Usergrid() {
        this.logger = new Logger(name);
    }
    Usergrid.isValidEndpoint = function(endpoint) {
        return true;
    };
    Usergrid.Request = function(method, endpoint, query_params, data, callback) {
        var p = new Promise();
        /*
         Create a logger
         */
        this.logger = new global.Logger("Usergrid.Request");
        this.logger.time("process request " + method + " " + endpoint);
        /*
         Validate our input
         */
        this.endpoint = endpoint + "?" + encodeParams(query_params);
        this.method = method.toUpperCase();
        this.data = "object" === typeof data ? JSON.stringify(data) : data;
        if (VALID_REQUEST_METHODS.indexOf(this.method) === -1) {
            throw new UsergridInvalidHTTPMethodError("invalid request method '" + this.method + "'");
        }
        /*
         Prepare our request
         */
        if (!isValidUrl(this.endpoint)) {
            this.logger.error(endpoint, this.endpoint, /^https:\/\//.test(endpoint));
            throw new UsergridInvalidURIError("The provided endpoint is not valid: " + this.endpoint);
        }
        /* a callback to make the request */
        var request = function() {
            return Ajax.request(this.method, this.endpoint, this.data);
        }.bind(this);
        /* a callback to process the response */
        var response = function(err, request) {
            return new Usergrid.Response(err, request);
        }.bind(this);
        /* a callback to clean up and return data to the client */
        var oncomplete = function(err, response) {
            p.done(err, response);
            this.logger.info("REQUEST", err, response);
            doCallback(callback, [ err, response ]);
            this.logger.timeEnd("process request " + method + " " + endpoint);
        }.bind(this);
        /* and a promise to chain them all together */
        Promise.chain([ request, response ]).then(oncomplete);
        return p;
    };
    Usergrid.Response = function(err, response) {
        var p = new Promise();
        var data = null;
        try {
            data = JSON.parse(response.responseText);
        } catch (e) {
            data = {};
        }
        Object.keys(data).forEach(function(key) {
            Object.defineProperty(this, key, {
                value: data[key],
                enumerable: true
            });
        }.bind(this));
        Object.defineProperty(this, "logger", {
            enumerable: false,
            configurable: false,
            writable: false,
            value: new global.Logger(name)
        });
        Object.defineProperty(this, "success", {
            enumerable: false,
            configurable: false,
            writable: true,
            value: true
        });
        Object.defineProperty(this, "err", {
            enumerable: false,
            configurable: false,
            writable: true,
            value: err
        });
        Object.defineProperty(this, "status", {
            enumerable: false,
            configurable: false,
            writable: true,
            value: parseInt(response.status)
        });
        Object.defineProperty(this, "statusGroup", {
            enumerable: false,
            configurable: false,
            writable: true,
            value: this.status - this.status % 100
        });
        switch (this.statusGroup) {
          case 200:
            this.success = true;
            break;

          case 400:
          case 500:
          case 300:
          case 100:
          default:
            this.success = false;
            break;
        }
        if (this.success) {
            p.done(null, this);
        } else {
            p.done(UsergridError.fromResponse(data), this);
        }
        return p;
    };
    Usergrid.Response.prototype.getEntities = function() {
        var entities;
        if (this.success) {
            entities = this.data ? this.data.entities : this.entities;
        }
        return entities || [];
    };
    Usergrid.Response.prototype.getEntity = function() {
        var entities = this.getEntities();
        return entities[0];
    };
    Usergrid.VERSION = Usergrid.USERGRID_SDK_VERSION = "0.11.0";
    global[name] = Usergrid;
    global[name].noConflict = function() {
        if (overwrittenName) {
            global[name] = overwrittenName;
        }
        return Usergrid;
    };
    return global[name];
})(this);

(function() {
    var name = "Client", global = this, overwrittenName = global[name], exports;
    var AUTH_ERRORS = [ "auth_expired_session_token", "auth_missing_credentials", "auth_unverified_oath", "expired_token", "unauthorized", "auth_invalid" ];
    Usergrid.Client = function(options) {
        this.URI = options.URI || "https://api.usergrid.com";
        if (options.orgName) {
            this.set("orgName", options.orgName);
        }
        if (options.appName) {
            this.set("appName", options.appName);
        }
        if (options.qs) {
            this.setObject("default_qs", options.qs);
        }
        this.buildCurl = options.buildCurl || false;
        this.logging = options.logging || false;
    };
    /*
   *  Main function for making requests to the API.  Can be called directly.
   *
   *  options object:
   *  `method` - http method (GET, POST, PUT, or DELETE), defaults to GET
   *  `qs` - object containing querystring values to be appended to the uri
   *  `body` - object containing entity body for POST and PUT requests
   *  `endpoint` - API endpoint, for example 'users/fred'
   *  `mQuery` - boolean, set to true if running management query, defaults to false
   *
   *  @method request
   *  @public
   *  @params {object} options
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.request = function(options, callback) {
        var method = options.method || "GET";
        var endpoint = options.endpoint;
        var body = options.body || {};
        var qs = options.qs || {};
        var mQuery = options.mQuery || false;
        var orgName = this.get("orgName");
        var appName = this.get("appName");
        var default_qs = this.getObject("default_qs");
        var uri;
        /*var logoutCallback=function(){
        if (typeof(this.logoutCallback) === 'function') {
            return this.logoutCallback(true, 'no_org_or_app_name_specified');
        }
    }.bind(this);*/
        if (!mQuery && !orgName && !appName) {
            return logoutCallback();
        }
        if (mQuery) {
            uri = this.URI + "/" + endpoint;
        } else {
            uri = this.URI + "/" + orgName + "/" + appName + "/" + endpoint;
        }
        if (this.getToken()) {
            qs.access_token = this.getToken();
        }
        if (default_qs) {
            qs = propCopy(qs, default_qs);
        }
        var self = this;
        var req = new Usergrid.Request(method, uri, qs, body, function(err, response) {
            /*if (AUTH_ERRORS.indexOf(response.error) !== -1) {
            return logoutCallback();
        }*/
            if (err) {
                doCallback(callback, [ err, response, self ], self);
            } else {
                doCallback(callback, [ null, response, self ], self);
            }
        });
    };
    /*
   *  function for building asset urls
   *
   *  @method buildAssetURL
   *  @public
   *  @params {string} uuid
   *  @return {string} assetURL
   */
    Usergrid.Client.prototype.buildAssetURL = function(uuid) {
        var self = this;
        var qs = {};
        var assetURL = this.URI + "/" + this.orgName + "/" + this.appName + "/assets/" + uuid + "/data";
        if (self.getToken()) {
            qs.access_token = self.getToken();
        }
        var encoded_params = encodeParams(qs);
        if (encoded_params) {
            assetURL += "?" + encoded_params;
        }
        return assetURL;
    };
    /*
   *  Main function for creating new groups. Call this directly.
   *
   *  @method createGroup
   *  @public
   *  @params {string} path
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.createGroup = function(options, callback) {
        var group = new Usergrid.Group({
            path: options.path,
            client: this,
            data: options
        });
        group.save(function(err, response) {
            doCallback(callback, [ err, response, group ], group);
        });
    };
    /*
   *  Main function for creating new entities - should be called directly.
   *
   *  options object: options {data:{'type':'collection_type', 'key':'value'}, uuid:uuid}}
   *
   *  @method createEntity
   *  @public
   *  @params {object} options
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.createEntity = function(options, callback) {
        var entity = new Usergrid.Entity({
            client: this,
            data: options
        });
        entity.save(function(err, response) {
            doCallback(callback, [ err, response, entity ], entity);
        });
    };
    /*
   *  Main function for getting existing entities - should be called directly.
   *
   *  You must supply a uuid or (username or name). Username only applies to users.
   *  Name applies to all custom entities
   *
   *  options object: options {data:{'type':'collection_type', 'name':'value', 'username':'value'}, uuid:uuid}}
   *
   *  @method createEntity
   *  @public
   *  @params {object} options
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.getEntity = function(options, callback) {
        var entity = new Usergrid.Entity({
            client: this,
            data: options
        });
        entity.fetch(function(err, response) {
            doCallback(callback, [ err, response, entity ], entity);
        });
    };
    /*
   *  Main function for restoring an entity from serialized data.
   *
   *  serializedObject should have come from entityObject.serialize();
   *
   *  @method restoreEntity
   *  @public
   *  @param {string} serializedObject
   *  @return {object} Entity Object
   */
    Usergrid.Client.prototype.restoreEntity = function(serializedObject) {
        var data = JSON.parse(serializedObject);
        var options = {
            client: this,
            data: data
        };
        var entity = new Usergrid.Entity(options);
        return entity;
    };
    /*
   *  Main function for creating new counters - should be called directly.
   *
   *  options object: options {timestamp:0, category:'value', counters:{name : value}}
   *
   *  @method createCounter
   *  @public
   *  @params {object} options
   *  @param {function} callback
   *  @return {callback} callback(err, response, counter)
   */
    Usergrid.Client.prototype.createCounter = function(options, callback) {
        var counter = new Usergrid.Counter({
            client: this,
            data: options
        });
        counter.save(callback);
    };
    /*
   *  Main function for creating new assets - should be called directly.
   *
   *  options object: options {name:"photo.jpg", path:"/user/uploads", "content-type":"image/jpeg", owner:"F01DE600-0000-0000-0000-000000000000", file: FileOrBlobObject }
   *
   *  @method createCounter
   *  @public
   *  @params {object} options
   *  @param {function} callback
   *  @return {callback} callback(err, response, counter)
   */
    Usergrid.Client.prototype.createAsset = function(options, callback) {
        var file = options.file;
        if (file) {
            options.name = options.name || file.name;
            options["content-type"] = options["content-type"] || file.type;
            options.path = options.path || "/";
            delete options.file;
        }
        var asset = new Usergrid.Asset({
            client: this,
            data: options
        });
        asset.save(function(err, response, asset) {
            if (file && !err) {
                asset.upload(file, callback);
            } else {
                doCallback(callback, [ err, response, asset ], asset);
            }
        });
    };
    /*
   *  Main function for creating new collections - should be called directly.
   *
   *  options object: options {client:client, type: type, qs:qs}
   *
   *  @method createCollection
   *  @public
   *  @params {object} options
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.createCollection = function(options, callback) {
        options.client = this;
        return new Usergrid.Collection(options, function(err, data, collection) {
            console.log("createCollection", arguments);
            doCallback(callback, [ err, collection, data ]);
        });
    };
    /*
   *  Main function for restoring a collection from serialized data.
   *
   *  serializedObject should have come from collectionObject.serialize();
   *
   *  @method restoreCollection
   *  @public
   *  @param {string} serializedObject
   *  @return {object} Collection Object
   */
    Usergrid.Client.prototype.restoreCollection = function(serializedObject) {
        var data = JSON.parse(serializedObject);
        data.client = this;
        var collection = new Usergrid.Collection(data);
        return collection;
    };
    /*
   *  Main function for retrieving a user's activity feed.
   *
   *  @method getFeedForUser
   *  @public
   *  @params {string} username
   *  @param {function} callback
   *  @return {callback} callback(err, data, activities)
   */
    Usergrid.Client.prototype.getFeedForUser = function(username, callback) {
        var options = {
            method: "GET",
            endpoint: "users/" + username + "/feed"
        };
        this.request(options, function(err, data) {
            if (err) {
                doCallback(callback, [ err ]);
            } else {
                doCallback(callback, [ err, data, data.getEntities() ]);
            }
        });
    };
    /*
   *  Function for creating new activities for the current user - should be called directly.
   *
   *  //user can be any of the following: "me", a uuid, a username
   *  Note: the "me" alias will reference the currently logged in user (e.g. 'users/me/activties')
   *
   *  //build a json object that looks like this:
   *  var options =
   *  {
   *    "actor" : {
   *      "displayName" :"myusername",
   *      "uuid" : "myuserid",
   *      "username" : "myusername",
   *      "email" : "myemail",
   *      "picture": "http://path/to/picture",
   *      "image" : {
   *          "duration" : 0,
   *          "height" : 80,
   *          "url" : "http://www.gravatar.com/avatar/",
   *          "width" : 80
   *      },
   *    },
   *    "verb" : "post",
   *    "content" : "My cool message",
   *    "lat" : 48.856614,
   *    "lon" : 2.352222
   *  }
   *
   *  @method createEntity
   *  @public
   *  @params {string} user // "me", a uuid, or a username
   *  @params {object} options
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.createUserActivity = function(user, options, callback) {
        options.type = "users/" + user + "/activities";
        options = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(options);
        entity.save(function(err, data) {
            doCallback(callback, [ err, data, entity ]);
        });
    };
    /*
   *  Function for creating user activities with an associated user entity.
   *
   *  user object:
   *  The user object passed into this function is an instance of Usergrid.Entity.
   *
   *  @method createUserActivityWithEntity
   *  @public
   *  @params {object} user
   *  @params {string} content
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.createUserActivityWithEntity = function(user, content, callback) {
        var username = user.get("username");
        var options = {
            actor: {
                displayName: username,
                uuid: user.get("uuid"),
                username: username,
                email: user.get("email"),
                picture: user.get("picture"),
                image: {
                    duration: 0,
                    height: 80,
                    url: user.get("picture"),
                    width: 80
                }
            },
            verb: "post",
            content: content
        };
        this.createUserActivity(username, options, callback);
    };
    /*
   *  A private method to get call timing of last call
   */
    Usergrid.Client.prototype.calcTimeDiff = function() {
        var seconds = 0;
        var time = this._end - this._start;
        try {
            seconds = (time / 10 / 60).toFixed(2);
        } catch (e) {
            return 0;
        }
        return seconds;
    };
    /*
   *  A public method to store the OAuth token for later use - uses localstorage if available
   *
   *  @method setToken
   *  @public
   *  @params {string} token
   *  @return none
   */
    Usergrid.Client.prototype.setToken = function(token) {
        this.set("token", token);
    };
    /*
   *  A public method to get the OAuth token
   *
   *  @method getToken
   *  @public
   *  @return {string} token
   */
    Usergrid.Client.prototype.getToken = function() {
        return this.get("token");
    };
    Usergrid.Client.prototype.setObject = function(key, value) {
        if (value) {
            value = JSON.stringify(value);
        }
        this.set(key, value);
    };
    Usergrid.Client.prototype.set = function(key, value) {
        var keyStore = "apigee_" + key;
        this[key] = value;
        if (typeof Storage !== "undefined") {
            if (value) {
                localStorage.setItem(keyStore, value);
            } else {
                localStorage.removeItem(keyStore);
            }
        }
    };
    Usergrid.Client.prototype.getObject = function(key) {
        return JSON.parse(this.get(key));
    };
    Usergrid.Client.prototype.get = function(key) {
        var keyStore = "apigee_" + key;
        var value = null;
        if (this[key]) {
            value = this[key];
        } else if (typeof Storage !== "undefined") {
            value = localStorage.getItem(keyStore);
        }
        return value;
    };
    /*
   * A public facing helper method for signing up users
   *
   * @method signup
   * @public
   * @params {string} username
   * @params {string} password
   * @params {string} email
   * @params {string} name
   * @param {function} callback
   * @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.signup = function(username, password, email, name, callback) {
        var self = this;
        var options = {
            type: "users",
            username: username,
            password: password,
            email: email,
            name: name
        };
        this.createEntity(options, callback);
    };
    /*
   *
   *  A public method to log in an app user - stores the token for later use
   *
   *  @method login
   *  @public
   *  @params {string} username
   *  @params {string} password
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.login = function(username, password, callback) {
        var self = this;
        var options = {
            method: "POST",
            endpoint: "token",
            body: {
                username: username,
                password: password,
                grant_type: "password"
            }
        };
        self.request(options, function(err, response) {
            var user = {};
            if (err) {
                if (self.logging) console.log("error trying to log user in");
            } else {
                var options = {
                    client: self,
                    data: response.user
                };
                user = new Usergrid.Entity(options);
                self.setToken(response.access_token);
            }
            doCallback(callback, [ err, response, user ]);
        });
    };
    Usergrid.Client.prototype.reAuthenticateLite = function(callback) {
        var self = this;
        var options = {
            method: "GET",
            endpoint: "management/me",
            mQuery: true
        };
        this.request(options, function(err, response) {
            if (err && self.logging) {
                console.log("error trying to re-authenticate user");
            } else {
                self.setToken(response.data.access_token);
            }
            doCallback(callback, [ err ]);
        });
    };
    Usergrid.Client.prototype.reAuthenticate = function(email, callback) {
        var self = this;
        var options = {
            method: "GET",
            endpoint: "management/users/" + email,
            mQuery: true
        };
        this.request(options, function(err, response) {
            var organizations = {};
            var applications = {};
            var user = {};
            var data;
            if (err && self.logging) {
                console.log("error trying to full authenticate user");
            } else {
                data = response.data;
                self.setToken(data.token);
                self.set("email", data.email);
                localStorage.setItem("accessToken", data.token);
                localStorage.setItem("userUUID", data.uuid);
                localStorage.setItem("userEmail", data.email);
                var userData = {
                    username: data.username,
                    email: data.email,
                    name: data.name,
                    uuid: data.uuid
                };
                var options = {
                    client: self,
                    data: userData
                };
                user = new Usergrid.Entity(options);
                organizations = data.organizations;
                var org = "";
                try {
                    var existingOrg = self.get("orgName");
                    org = organizations[existingOrg] ? organizations[existingOrg] : organizations[Object.keys(organizations)[0]];
                    self.set("orgName", org.name);
                } catch (e) {
                    err = true;
                    if (self.logging) {
                        console.log("error selecting org");
                    }
                }
                applications = self.parseApplicationsArray(org);
                self.selectFirstApp(applications);
                self.setObject("organizations", organizations);
                self.setObject("applications", applications);
            }
            doCallback(callback, [ err, data, user, organizations, applications ], self);
        });
    };
    /*
   *  A public method to log in an app user with facebook - stores the token for later use
   *
   *  @method loginFacebook
   *  @public
   *  @params {string} username
   *  @params {string} password
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.loginFacebook = function(facebookToken, callback) {
        var self = this;
        var options = {
            method: "GET",
            endpoint: "auth/facebook",
            qs: {
                fb_access_token: facebookToken
            }
        };
        this.request(options, function(err, data) {
            var user = {};
            if (err && self.logging) {
                console.log("error trying to log user in");
            } else {
                var options = {
                    client: self,
                    data: data.user
                };
                user = new Usergrid.Entity(options);
                self.setToken(data.access_token);
            }
            doCallback(callback, [ err, data, user ], self);
        });
    };
    /*
   *  A public method to get the currently logged in user entity
   *
   *  @method getLoggedInUser
   *  @public
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
    Usergrid.Client.prototype.getLoggedInUser = function(callback) {
        var self = this;
        if (!this.getToken()) {
            doCallback(callback, [ new UsergridError("Access Token not set"), null, self ], self);
        } else {
            var options = {
                method: "GET",
                endpoint: "users/me"
            };
            this.request(options, function(err, response) {
                if (err) {
                    if (self.logging) {
                        console.log("error trying to log user in");
                    }
                    console.error(err, response);
                    doCallback(callback, [ err, response, self ], self);
                } else {
                    var options = {
                        client: self,
                        data: response.getEntity()
                    };
                    var user = new Usergrid.Entity(options);
                    doCallback(callback, [ null, response, user ], self);
                }
            });
        }
    };
    /*
   *  A public method to test if a user is logged in - does not guarantee that the token is still valid,
   *  but rather that one exists
   *
   *  @method isLoggedIn
   *  @public
   *  @return {boolean} Returns true the user is logged in (has token and uuid), false if not
   */
    Usergrid.Client.prototype.isLoggedIn = function() {
        var token = this.getToken();
        return "undefined" !== typeof token && token !== null;
    };
    /*
   *  A public method to log out an app user - clears all user fields from client
   *
   *  @method logout
   *  @public
   *  @return none
   */
    Usergrid.Client.prototype.logout = function() {
        this.setToken();
    };
    /*
   *  A public method to destroy access tokens on the server
   *
   *  @method logout
   *  @public
   *  @param {string} username	the user associated with the token to revoke
   *  @param {string} token set to 'null' to revoke the token of the currently logged in user
   *    or set to token value to revoke a specific token
   *  @param {string} revokeAll set to 'true' to revoke all tokens for the user
   *  @return none
   */
    Usergrid.Client.prototype.destroyToken = function(username, token, revokeAll, callback) {
        var options = {
            client: self,
            method: "PUT"
        };
        if (revokeAll === true) {
            options.endpoint = "users/" + username + "/revoketokens";
        } else if (token === null) {
            options.endpoint = "users/" + username + "/revoketoken?token=" + this.getToken();
        } else {
            options.endpoint = "users/" + username + "/revoketoken?token=" + token;
        }
        this.request(options, function(err, data) {
            if (err) {
                if (self.logging) {
                    console.log("error destroying access token");
                }
                doCallback(callback, [ err, data, null ], self);
            } else {
                if (revokeAll === true) {
                    console.log("all user tokens invalidated");
                } else {
                    console.log("token invalidated");
                }
                doCallback(callback, [ err, data, null ], self);
            }
        });
    };
    /*
   *  A public method to log out an app user - clears all user fields from client
   *  and destroys the access token on the server.
   *
   *  @method logout
   *  @public
   *  @param {string} username the user associated with the token to revoke
   *  @param {string} token set to 'null' to revoke the token of the currently logged in user
   *   or set to token value to revoke a specific token
   *  @param {string} revokeAll set to 'true' to revoke all tokens for the user
   *  @return none
   */
    Usergrid.Client.prototype.logoutAndDestroyToken = function(username, token, revokeAll, callback) {
        if (username === null) {
            console.log("username required to revoke tokens");
        } else {
            this.destroyToken(username, token, revokeAll, callback);
            if (revokeAll === true || token === this.getToken() || token === null) {
                this.setToken(null);
            }
        }
    };
    /*
   *  A private method to build the curl call to display on the command line
   *
   *  @method buildCurlCall
   *  @private
   *  @param {object} options
   *  @return {string} curl
   */
    Usergrid.Client.prototype.buildCurlCall = function(options) {
        var curl = [ "curl" ];
        var method = (options.method || "GET").toUpperCase();
        var body = options.body;
        var uri = options.uri;
        curl.push("-X");
        curl.push([ "POST", "PUT", "DELETE" ].indexOf(method) >= 0 ? method : "GET");
        curl.push(uri);
        if ("object" === typeof body && Object.keys(body).length > 0 && [ "POST", "PUT" ].indexOf(method) !== -1) {
            curl.push("-d");
            curl.push("'" + JSON.stringify(body) + "'");
        }
        curl = curl.join(" ");
        console.log(curl);
        return curl;
    };
    Usergrid.Client.prototype.getDisplayImage = function(email, picture, size) {
        size = size || 50;
        var image = "https://apigee.com/usergrid/images/user_profile.png";
        try {
            if (picture) {
                image = picture;
            } else if (email.length) {
                image = "https://secure.gravatar.com/avatar/" + MD5(email) + "?s=" + size + encodeURI("&d=https://apigee.com/usergrid/images/user_profile.png");
            }
        } catch (e) {} finally {
            return image;
        }
    };
    global[name] = Usergrid.Client;
    global[name].noConflict = function() {
        if (overwrittenName) {
            global[name] = overwrittenName;
        }
        return exports;
    };
    return global[name];
})();

var ENTITY_SYSTEM_PROPERTIES = [ "metadata", "created", "modified", "oldpassword", "newpassword", "type", "activated", "uuid" ];

/*
 *  A class to Model a Usergrid Entity.
 *  Set the type and uuid of entity in the 'data' json object
 *
 *  @constructor
 *  @param {object} options {client:client, data:{'type':'collection_type', uuid:'uuid', 'key':'value'}}
 */
Usergrid.Entity = function(options) {
    this._data = {};
    this._client = undefined;
    if (options) {
        this.set(options.data || {});
        this._client = options.client || {};
    }
};

/*
 *  method to determine whether or not the passed variable is a Usergrid Entity
 *
 *  @method isEntity
 *  @public
 *  @params {any} obj - any variable
 *  @return {boolean} Returns true or false
 */
Usergrid.Entity.isEntity = function(obj) {
    return obj && obj instanceof Usergrid.Entity;
};

/*
 *  method to determine whether or not the passed variable is a Usergrid Entity
 *  That has been saved.
 *
 *  @method isPersistedEntity
 *  @public
 *  @params {any} obj - any variable
 *  @return {boolean} Returns true or false
 */
Usergrid.Entity.isPersistedEntity = function(obj) {
    return isEntity(obj) && isUUID(obj.get("uuid"));
};

/*
 *  returns a serialized version of the entity object
 *
 *  Note: use the client.restoreEntity() function to restore
 *
 *  @method serialize
 *  @return {string} data
 */
Usergrid.Entity.prototype.serialize = function() {
    return JSON.stringify(this._data);
};

/*
 *  gets a specific field or the entire data object. If null or no argument
 *  passed, will return all data, else, will return a specific field
 *
 *  @method get
 *  @param {string} field
 *  @return {string} || {object} data
 */
Usergrid.Entity.prototype.get = function(key) {
    var value;
    if (arguments.length === 0) {
        value = this._data;
    } else if (arguments.length > 1) {
        key = [].slice.call(arguments).reduce(function(p, c, i, a) {
            if (c instanceof Array) {
                p = p.concat(c);
            } else {
                p.push(c);
            }
            return p;
        }, []);
    }
    if (key instanceof Array) {
        var self = this;
        value = key.map(function(k) {
            return self.get(k);
        });
    } else if ("undefined" !== typeof key) {
        value = this._data[key];
    }
    return value;
};

/*
 *  adds a specific key value pair or object to the Entity's data
 *  is additive - will not overwrite existing values unless they
 *  are explicitly specified
 *
 *  @method set
 *  @param {string} key || {object}
 *  @param {string} value
 *  @return none
 */
Usergrid.Entity.prototype.set = function(key, value) {
    if (typeof key === "object") {
        for (var field in key) {
            this._data[field] = key[field];
        }
    } else if (typeof key === "string") {
        if (value === null) {
            delete this._data[key];
        } else {
            this._data[key] = value;
        }
    } else {
        this._data = {};
    }
};

Usergrid.Entity.prototype.getEndpoint = function() {
    var type = this.get("type"), nameProperties = [ "uuid", "name" ], name;
    if (type === undefined) {
        throw new UsergridError("cannot fetch entity, no entity type specified", "no_type_specified");
    } else if (/^users?$/.test(type)) {
        nameProperties.unshift("username");
    }
    name = this.get(nameProperties).filter(function(x) {
        return x !== null && "undefined" !== typeof x;
    }).shift();
    return name ? [ type, name ].join("/") : type;
};

/*
 *  Saves the entity back to the database
 *
 *  @method save
 *  @public
 *  @param {function} callback
 *  @return {callback} callback(err, response, self)
 */
Usergrid.Entity.prototype.save = function(callback) {
    var self = this, type = this.get("type"), method = "POST", entityId = this.get("uuid"), changePassword, entityData = this.get(), options = {
        method: method,
        endpoint: type
    };
    if (entityId) {
        options.method = "PUT";
        options.endpoint += "/" + entityId;
    }
    options.body = Object.keys(entityData).filter(function(key) {
        return ENTITY_SYSTEM_PROPERTIES.indexOf(key) === -1;
    }).reduce(function(data, key) {
        data[key] = entityData[key];
        return data;
    }, {});
    self._client.request(options, function(err, response) {
        var entity = response.getEntity();
        if (entity) {
            self.set(entity);
            self.set("type", /^\//.test(response.path) ? response.path.substring(1) : response.path);
        }
        if (err && self._client.logging) {
            console.log("could not save entity");
        }
        doCallback(callback, [ err, response, self ], self);
    });
};

/*
 *
 * Updates the user's password
 */
Usergrid.Entity.prototype.changePassword = function(oldpassword, newpassword, callback) {
    var self = this;
    if ("function" === typeof oldpassword && callback === undefined) {
        callback = oldpassword;
        oldpassword = self.get("oldpassword");
        newpassword = self.get("newpassword");
    }
    self.set({
        password: null,
        oldpassword: null,
        newpassword: null
    });
    if (/^users?$/.test(self.get("type")) && oldpassword && newpassword) {
        var options = {
            method: "PUT",
            endpoint: "users/" + self.get("uuid") + "/password",
            body: {
                uuid: self.get("uuid"),
                username: self.get("username"),
                oldpassword: oldpassword,
                newpassword: newpassword
            }
        };
        self._client.request(options, function(err, response) {
            if (err && self._client.logging) {
                console.log("could not update user");
            }
            doCallback(callback, [ err, response, self ], self);
        });
    } else {
        throw new UsergridInvalidArgumentError("Invalid arguments passed to 'changePassword'");
    }
};

/*
 *  refreshes the entity by making a GET call back to the database
 *
 *  @method fetch
 *  @public
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 */
Usergrid.Entity.prototype.fetch = function(callback) {
    var endpoint, self = this;
    endpoint = this.getEndpoint();
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, response) {
        var entity = response.getEntity();
        if (entity) {
            self.set(entity);
        }
        doCallback(callback, [ err, response, self ], self);
    });
};

/*
 *  deletes the entity from the database - will only delete
 *  if the object has a valid uuid
 *
 *  @method destroy
 *  @public
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 *
 */
Usergrid.Entity.prototype.destroy = function(callback) {
    var self = this;
    var endpoint = this.getEndpoint();
    var options = {
        method: "DELETE",
        endpoint: endpoint
    };
    this._client.request(options, function(err, response) {
        if (!err) {
            self.set(null);
        }
        doCallback(callback, [ err, response, self ], self);
    });
};

/*
 *  connects one entity to another
 *
 *  @method connect
 *  @public
 *  @param {string} connection
 *  @param {object} entity
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 *
 */
Usergrid.Entity.prototype.connect = function(connection, entity, callback) {
    this.addOrRemoveConnection("POST", connection, entity, callback);
};

/*
 *  disconnects one entity from another
 *
 *  @method disconnect
 *  @public
 *  @param {string} connection
 *  @param {object} entity
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 *
 */
Usergrid.Entity.prototype.disconnect = function(connection, entity, callback) {
    this.addOrRemoveConnection("DELETE", connection, entity, callback);
};

/*
 *  adds or removes a connection between two entities
 *
 *  @method addOrRemoveConnection
 *  @public
 *  @param {string} method
 *  @param {string} connection
 *  @param {object} entity
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 *
 */
Usergrid.Entity.prototype.addOrRemoveConnection = function(method, connection, entity, callback) {
    var self = this;
    if ([ "POST", "DELETE" ].indexOf(method.toUpperCase()) == -1) {
        throw new UsergridInvalidArgumentError("invalid method for connection call. must be 'POST' or 'DELETE'");
    }
    var connecteeType = entity.get("type");
    var connectee = this.getEntityId(entity);
    if (!connectee) {
        throw new UsergridInvalidArgumentError("connectee could not be identified");
    }
    var connectorType = this.get("type");
    var connector = this.getEntityId(this);
    if (!connector) {
        throw new UsergridInvalidArgumentError("connector could not be identified");
    }
    var endpoint = [ connectorType, connector, connection, connecteeType, connectee ].join("/");
    var options = {
        method: method,
        endpoint: endpoint
    };
    this._client.request(options, function(err, response) {
        if (err && self._client.logging) {
            console.log("There was an error with the connection call");
        }
        doCallback(callback, [ err, response, self ], self);
    });
};

/*
 *  returns a unique identifier for an entity
 *
 *  @method connect
 *  @public
 *  @param {object} entity
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 *
 */
Usergrid.Entity.prototype.getEntityId = function(entity) {
    var id;
    if (isUUID(entity.get("uuid"))) {
        id = entity.get("uuid");
    } else if (this.get("type") === "users" || this.get("type") === "user") {
        id = entity.get("username");
    } else {
        id = entity.get("name");
    }
    return id;
};

/*
 *  gets an entities connections
 *
 *  @method getConnections
 *  @public
 *  @param {string} connection
 *  @param {object} entity
 *  @param {function} callback
 *  @return {callback} callback(err, data, connections)
 *
 */
Usergrid.Entity.prototype.getConnections = function(connection, callback) {
    var self = this;
    var connectorType = this.get("type");
    var connector = this.getEntityId(this);
    if (!connector) {
        if (typeof callback === "function") {
            var error = "Error in getConnections - no uuid specified.";
            if (self._client.logging) {
                console.log(error);
            }
            doCallback(callback, [ true, error ], self);
        }
        return;
    }
    var endpoint = connectorType + "/" + connector + "/" + connection + "/";
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("entity could not be connected");
        }
        self[connection] = {};
        var length = data && data.entities ? data.entities.length : 0;
        for (var i = 0; i < length; i++) {
            if (data.entities[i].type === "user") {
                self[connection][data.entities[i].username] = data.entities[i];
            } else {
                self[connection][data.entities[i].name] = data.entities[i];
            }
        }
        doCallback(callback, [ err, data, data.entities ], self);
    });
};

Usergrid.Entity.prototype.getGroups = function(callback) {
    var self = this;
    var endpoint = "users" + "/" + this.get("uuid") + "/groups";
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("entity could not be connected");
        }
        self.groups = data.entities;
        doCallback(callback, [ err, data, data.entities ], self);
    });
};

Usergrid.Entity.prototype.getActivities = function(callback) {
    var self = this;
    var endpoint = this.get("type") + "/" + this.get("uuid") + "/activities";
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("entity could not be connected");
        }
        for (var entity in data.entities) {
            data.entities[entity].createdDate = new Date(data.entities[entity].created).toUTCString();
        }
        self.activities = data.entities;
        doCallback(callback, [ err, data, data.entities ], self);
    });
};

Usergrid.Entity.prototype.getFollowing = function(callback) {
    var self = this;
    var endpoint = "users" + "/" + this.get("uuid") + "/following";
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("could not get user following");
        }
        for (var entity in data.entities) {
            data.entities[entity].createdDate = new Date(data.entities[entity].created).toUTCString();
            var image = self._client.getDisplayImage(data.entities[entity].email, data.entities[entity].picture);
            data.entities[entity]._portal_image_icon = image;
        }
        self.following = data.entities;
        doCallback(callback, [ err, data, data.entities ], self);
    });
};

Usergrid.Entity.prototype.getFollowers = function(callback) {
    var self = this;
    var endpoint = "users" + "/" + this.get("uuid") + "/followers";
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("could not get user followers");
        }
        for (var entity in data.entities) {
            data.entities[entity].createdDate = new Date(data.entities[entity].created).toUTCString();
            var image = self._client.getDisplayImage(data.entities[entity].email, data.entities[entity].picture);
            data.entities[entity]._portal_image_icon = image;
        }
        self.followers = data.entities;
        doCallback(callback, [ err, data, data.entities ], self);
    });
};

Usergrid.Client.prototype.createRole = function(roleName, permissions, callback) {
    var options = {
        type: "role",
        name: roleName
    };
    this.createEntity(options, function(err, response, entity) {
        if (err) {
            doCallback(callback, [ err, response, self ]);
        } else {
            entity.assignPermissions(permissions, function(err, data) {
                if (err) {
                    doCallback(callback, [ err, response, self ]);
                } else {
                    doCallback(callback, [ err, data, data.data ], self);
                }
            });
        }
    });
};

Usergrid.Entity.prototype.getRoles = function(callback) {
    var self = this;
    var endpoint = this.get("type") + "/" + this.get("uuid") + "/roles";
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("could not get user roles");
        }
        self.roles = data.entities;
        doCallback(callback, [ err, data, data.entities ], self);
    });
};

Usergrid.Entity.prototype.assignRole = function(roleName, callback) {
    var self = this;
    var type = self.get("type");
    var collection = type + "s";
    var entityID;
    if (type == "user" && this.get("username") != null) {
        entityID = self.get("username");
    } else if (type == "group" && this.get("name") != null) {
        entityID = self.get("name");
    } else if (this.get("uuid") != null) {
        entityID = self.get("uuid");
    }
    if (type != "users" && type != "groups") {
        doCallback(callback, [ new UsergridError("entity must be a group or user", "invalid_entity_type"), null, this ], this);
    }
    var endpoint = "roles/" + roleName + "/" + collection + "/" + entityID;
    var options = {
        method: "POST",
        endpoint: endpoint
    };
    this._client.request(options, function(err, response) {
        if (err) {
            console.log("Could not assign role.");
        }
        doCallback(callback, [ err, response, self ]);
    });
};

Usergrid.Entity.prototype.removeRole = function(roleName, callback) {
    var self = this;
    var type = self.get("type");
    var collection = type + "s";
    var entityID;
    if (type == "user" && this.get("username") != null) {
        entityID = this.get("username");
    } else if (type == "group" && this.get("name") != null) {
        entityID = this.get("name");
    } else if (this.get("uuid") != null) {
        entityID = this.get("uuid");
    }
    if (type != "users" && type != "groups") {
        doCallback(callback, [ new UsergridError("entity must be a group or user", "invalid_entity_type"), null, this ], this);
    }
    var endpoint = "roles/" + roleName + "/" + collection + "/" + entityID;
    var options = {
        method: "DELETE",
        endpoint: endpoint
    };
    this._client.request(options, function(err, response) {
        if (err) {
            console.log("Could not assign role.");
        }
        doCallback(callback, [ err, response, self ]);
    });
};

Usergrid.Entity.prototype.assignPermissions = function(permissions, callback) {
    var self = this;
    var entityID;
    var type = this.get("type");
    if (type != "user" && type != "users" && type != "group" && type != "groups" && type != "role" && type != "roles") {
        doCallback(callback, [ new UsergridError("entity must be a group, user, or role", "invalid_entity_type"), null, this ], this);
    }
    if (type == "user" && this.get("username") != null) {
        entityID = this.get("username");
    } else if (type == "group" && this.get("name") != null) {
        entityID = this.get("name");
    } else if (this.get("uuid") != null) {
        entityID = this.get("uuid");
    }
    var endpoint = type + "/" + entityID + "/permissions";
    var options = {
        method: "POST",
        endpoint: endpoint,
        body: {
            permission: permissions
        }
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("could not assign permissions");
        }
        doCallback(callback, [ err, data, data.data ], self);
    });
};

Usergrid.Entity.prototype.removePermissions = function(permissions, callback) {
    var self = this;
    var entityID;
    var type = this.get("type");
    if (type != "user" && type != "users" && type != "group" && type != "groups" && type != "role" && type != "roles") {
        doCallback(callback, [ new UsergridError("entity must be a group, user, or role", "invalid_entity_type"), null, this ], this);
    }
    if (type == "user" && this.get("username") != null) {
        entityID = this.get("username");
    } else if (type == "group" && this.get("name") != null) {
        entityID = this.get("name");
    } else if (this.get("uuid") != null) {
        entityID = this.get("uuid");
    }
    var endpoint = type + "/" + entityID + "/permissions";
    var options = {
        method: "DELETE",
        endpoint: endpoint,
        qs: {
            permission: permissions
        }
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("could not remove permissions");
        }
        doCallback(callback, [ err, data, data.params.permission ], self);
    });
};

Usergrid.Entity.prototype.getPermissions = function(callback) {
    var self = this;
    var endpoint = this.get("type") + "/" + this.get("uuid") + "/permissions";
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("could not get user permissions");
        }
        var permissions = [];
        if (data.data) {
            var perms = data.data;
            var count = 0;
            for (var i in perms) {
                count++;
                var perm = perms[i];
                var parts = perm.split(":");
                var ops_part = "";
                var path_part = parts[0];
                if (parts.length > 1) {
                    ops_part = parts[0];
                    path_part = parts[1];
                }
                ops_part = ops_part.replace("*", "get,post,put,delete");
                var ops = ops_part.split(",");
                var ops_object = {};
                ops_object.get = "no";
                ops_object.post = "no";
                ops_object.put = "no";
                ops_object.delete = "no";
                for (var j in ops) {
                    ops_object[ops[j]] = "yes";
                }
                permissions.push({
                    operations: ops_object,
                    path: path_part,
                    perm: perm
                });
            }
        }
        self.permissions = permissions;
        doCallback(callback, [ err, data, data.entities ], self);
    });
};

/*
 *  The Collection class models Usergrid Collections.  It essentially
 *  acts as a container for holding Entity objects, while providing
 *  additional funcitonality such as paging, and saving
 *
 *  @constructor
 *  @param {string} options - configuration object
 *  @return {Collection} collection
 */
Usergrid.Collection = function(options) {
    if (options) {
        this._client = options.client;
        this._type = options.type;
        this.qs = options.qs || {};
        this._list = options.list || [];
        this._iterator = options.iterator || -1;
        this._previous = options.previous || [];
        this._next = options.next || null;
        this._cursor = options.cursor || null;
        if (options.list) {
            var count = options.list.length;
            for (var i = 0; i < count; i++) {
                var entity = this._client.restoreEntity(options.list[i]);
                this._list[i] = entity;
            }
        }
    }
};

/*
 *  method to determine whether or not the passed variable is a Usergrid Collection
 *
 *  @method isCollection
 *  @public
 *  @params {any} obj - any variable
 *  @return {boolean} Returns true or false
 */
Usergrid.isCollection = function(obj) {
    return obj && obj instanceof Usergrid.Collection;
};

/*
 *  gets the data from the collection object for serialization
 *
 *  @method serialize
 *  @return {object} data
 */
Usergrid.Collection.prototype.serialize = function() {
    var data = {};
    data.type = this._type;
    data.qs = this.qs;
    data.iterator = this._iterator;
    data.previous = this._previous;
    data.next = this._next;
    data.cursor = this._cursor;
    this.resetEntityPointer();
    var i = 0;
    data.list = [];
    while (this.hasNextEntity()) {
        var entity = this.getNextEntity();
        data.list[i] = entity.serialize();
        i++;
    }
    data = JSON.stringify(data);
    return data;
};

/*Usergrid.Collection.prototype.addCollection = function (collectionName, options, callback) {
  self = this;
  options.client = this._client;
  var collection = new Usergrid.Collection(options, function(err, data) {
    if (typeof(callback) === 'function') {

      collection.resetEntityPointer();
      while(collection.hasNextEntity()) {
        var user = collection.getNextEntity();
        var email = user.get('email');
        var image = self._client.getDisplayImage(user.get('email'), user.get('picture'));
        user._portal_image_icon = image;
      }

      self[collectionName] = collection;
      doCallback(callback, [err, collection], self);
    }
  });
};*/
/*
 *  Populates the collection from the server
 *
 *  @method fetch
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 */
Usergrid.Collection.prototype.fetch = function(callback) {
    var self = this;
    var qs = this.qs;
    if (this._cursor) {
        qs.cursor = this._cursor;
    } else {
        delete qs.cursor;
    }
    var options = {
        method: "GET",
        endpoint: this._type,
        qs: this.qs
    };
    this._client.request(options, function(err, response) {
        if (err && self._client.logging) {
            console.log("error getting collection");
        } else {
            self.saveCursor(response.cursor || null);
            self.resetEntityPointer();
            self._list = response.getEntities().filter(function(entity) {
                return isUUID(entity.uuid);
            }).map(function(entity) {
                var ent = new Usergrid.Entity({
                    client: self._client
                });
                ent.set(entity);
                ent.type = self._type;
                return ent;
            });
        }
        doCallback(callback, [ err, response, self ], self);
    });
};

/*
 *  Adds a new Entity to the collection (saves, then adds to the local object)
 *
 *  @method addNewEntity
 *  @param {object} entity
 *  @param {function} callback
 *  @return {callback} callback(err, data, entity)
 */
Usergrid.Collection.prototype.addEntity = function(entityObject, callback) {
    var self = this;
    entityObject.type = this._type;
    this._client.createEntity(entityObject, function(err, response, entity) {
        if (!err) {
            self.addExistingEntity(entity);
        }
        doCallback(callback, [ err, response, self ], self);
    });
};

Usergrid.Collection.prototype.addExistingEntity = function(entity) {
    var count = this._list.length;
    this._list[count] = entity;
};

/*
 *  Removes the Entity from the collection, then destroys the object on the server
 *
 *  @method destroyEntity
 *  @param {object} entity
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 */
Usergrid.Collection.prototype.destroyEntity = function(entity, callback) {
    var self = this;
    entity.destroy(function(err, response) {
        if (err) {
            if (self._client.logging) {
                console.log("could not destroy entity");
            }
            doCallback(callback, [ err, response, self ], self);
        } else {
            self.fetch(callback);
        }
        self.removeEntity(entity);
    });
};

/*
 * Filters the list of entities based on the supplied criteria function
 * works like Array.prototype.filter
 *
 *  @method getEntitiesByCriteria
 *  @param {function} criteria  A function that takes each entity as an argument and returns true or false
 *  @return {Entity[]} returns a list of entities that caused the criteria function to return true
 */
Usergrid.Collection.prototype.getEntitiesByCriteria = function(criteria) {
    return this._list.filter(criteria);
};

/*
 * Returns the first entity from the list of entities based on the supplied criteria function
 * works like Array.prototype.filter
 *
 *  @method getEntitiesByCriteria
 *  @param {function} criteria  A function that takes each entity as an argument and returns true or false
 *  @return {Entity[]} returns a list of entities that caused the criteria function to return true
 */
Usergrid.Collection.prototype.getEntityByCriteria = function(criteria) {
    return this.getEntitiesByCriteria(criteria).shift();
};

/*
 * Removed an entity from the collection without destroying it on the server
 *
 *  @method removeEntity
 *  @param {object} entity
 *  @return {Entity} returns the removed entity or undefined if it was not found
 */
Usergrid.Collection.prototype.removeEntity = function(entity) {
    var removedEntity = this.getEntityByCriteria(function(item) {
        return entity.uuid === item.get("uuid");
    });
    delete this._list[this._list.indexOf(removedEntity)];
    return removedEntity;
};

/*
 *  Looks up an Entity by UUID
 *
 *  @method getEntityByUUID
 *  @param {string} UUID
 *  @param {function} callback
 *  @return {callback} callback(err, data, entity)
 */
Usergrid.Collection.prototype.getEntityByUUID = function(uuid, callback) {
    var entity = this.getEntityByCriteria(function(item) {
        return item.get("uuid") === uuid;
    });
    if (entity) {
        doCallback(callback, [ null, entity, entity ], this);
    } else {
        var options = {
            data: {
                type: this._type,
                uuid: uuid
            },
            client: this._client
        };
        entity = new Usergrid.Entity(options);
        entity.fetch(callback);
    }
};

/*
 *  Returns the first Entity of the Entity list - does not affect the iterator
 *
 *  @method getFirstEntity
 *  @return {object} returns an entity object
 */
Usergrid.Collection.prototype.getFirstEntity = function() {
    var count = this._list.length;
    if (count > 0) {
        return this._list[0];
    }
    return null;
};

/*
 *  Returns the last Entity of the Entity list - does not affect the iterator
 *
 *  @method getLastEntity
 *  @return {object} returns an entity object
 */
Usergrid.Collection.prototype.getLastEntity = function() {
    var count = this._list.length;
    if (count > 0) {
        return this._list[count - 1];
    }
    return null;
};

/*
 *  Entity iteration -Checks to see if there is a "next" entity
 *  in the list.  The first time this method is called on an entity
 *  list, or after the resetEntityPointer method is called, it will
 *  return true referencing the first entity in the list
 *
 *  @method hasNextEntity
 *  @return {boolean} true if there is a next entity, false if not
 */
Usergrid.Collection.prototype.hasNextEntity = function() {
    var next = this._iterator + 1;
    var hasNextElement = next >= 0 && next < this._list.length;
    if (hasNextElement) {
        return true;
    }
    return false;
};

/*
 *  Entity iteration - Gets the "next" entity in the list.  The first
 *  time this method is called on an entity list, or after the method
 *  resetEntityPointer is called, it will return the,
 *  first entity in the list
 *
 *  @method hasNextEntity
 *  @return {object} entity
 */
Usergrid.Collection.prototype.getNextEntity = function() {
    this._iterator++;
    var hasNextElement = this._iterator >= 0 && this._iterator <= this._list.length;
    if (hasNextElement) {
        return this._list[this._iterator];
    }
    return false;
};

/*
 *  Entity iteration - Checks to see if there is a "previous"
 *  entity in the list.
 *
 *  @method hasPrevEntity
 *  @return {boolean} true if there is a previous entity, false if not
 */
Usergrid.Collection.prototype.hasPrevEntity = function() {
    var previous = this._iterator - 1;
    var hasPreviousElement = previous >= 0 && previous < this._list.length;
    if (hasPreviousElement) {
        return true;
    }
    return false;
};

/*
 *  Entity iteration - Gets the "previous" entity in the list.
 *
 *  @method getPrevEntity
 *  @return {object} entity
 */
Usergrid.Collection.prototype.getPrevEntity = function() {
    this._iterator--;
    var hasPreviousElement = this._iterator >= 0 && this._iterator <= this._list.length;
    if (hasPreviousElement) {
        return this._list[this._iterator];
    }
    return false;
};

/*
 *  Entity iteration - Resets the iterator back to the beginning
 *  of the list
 *
 *  @method resetEntityPointer
 *  @return none
 */
Usergrid.Collection.prototype.resetEntityPointer = function() {
    this._iterator = -1;
};

/*
 * Method to save off the cursor just returned by the last API call
 *
 * @public
 * @method saveCursor
 * @return none
 */
Usergrid.Collection.prototype.saveCursor = function(cursor) {
    if (this._next !== cursor) {
        this._next = cursor;
    }
};

/*
 * Resets the paging pointer (back to original page)
 *
 * @public
 * @method resetPaging
 * @return none
 */
Usergrid.Collection.prototype.resetPaging = function() {
    this._previous = [];
    this._next = null;
    this._cursor = null;
};

/*
 *  Paging -  checks to see if there is a next page od data
 *
 *  @method hasNextPage
 *  @return {boolean} returns true if there is a next page of data, false otherwise
 */
Usergrid.Collection.prototype.hasNextPage = function() {
    return this._next;
};

/*
 *  Paging - advances the cursor and gets the next
 *  page of data from the API.  Stores returned entities
 *  in the Entity list.
 *
 *  @method getNextPage
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 */
Usergrid.Collection.prototype.getNextPage = function(callback) {
    if (this.hasNextPage()) {
        this._previous.push(this._cursor);
        this._cursor = this._next;
        this._list = [];
        this.fetch(callback);
    }
};

/*
 *  Paging -  checks to see if there is a previous page od data
 *
 *  @method hasPreviousPage
 *  @return {boolean} returns true if there is a previous page of data, false otherwise
 */
Usergrid.Collection.prototype.hasPreviousPage = function() {
    return this._previous.length > 0;
};

/*
 *  Paging - reverts the cursor and gets the previous
 *  page of data from the API.  Stores returned entities
 *  in the Entity list.
 *
 *  @method getPreviousPage
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 */
Usergrid.Collection.prototype.getPreviousPage = function(callback) {
    if (this.hasPreviousPage()) {
        this._next = null;
        this._cursor = this._previous.pop();
        this._list = [];
        this.fetch(callback);
    }
};

/*
 *  A class to model a Usergrid group.
 *  Set the path in the options object.
 *
 *  @constructor
 *  @param {object} options {client:client, data: {'key': 'value'}, path:'path'}
 */
Usergrid.Group = function(options, callback) {
    this._path = options.path;
    this._list = [];
    this._client = options.client;
    this._data = options.data || {};
    this._data.type = "groups";
};

/*
 *  Inherit from Usergrid.Entity.
 *  Note: This only accounts for data on the group object itself.
 *  You need to use add and remove to manipulate group membership.
 */
Usergrid.Group.prototype = new Usergrid.Entity();

/*
 *  Fetches current group data, and members.
 *
 *  @method fetch
 *  @public
 *  @param {function} callback
 *  @returns {function} callback(err, data)
 */
Usergrid.Group.prototype.fetch = function(callback) {
    var self = this;
    var groupEndpoint = "groups/" + this._path;
    var memberEndpoint = "groups/" + this._path + "/users";
    var groupOptions = {
        method: "GET",
        endpoint: groupEndpoint
    };
    var memberOptions = {
        method: "GET",
        endpoint: memberEndpoint
    };
    this._client.request(groupOptions, function(err, response) {
        if (err) {
            if (self._client.logging) {
                console.log("error getting group");
            }
            doCallback(callback, [ err, response ], self);
        } else {
            var entities = response.getEntities();
            if (entities && entities.length) {
                var groupresponse = entities.shift();
                self._client.request(memberOptions, function(err, response) {
                    if (err && self._client.logging) {
                        console.log("error getting group users");
                    } else {
                        self._list = response.getEntities().filter(function(entity) {
                            return isUUID(entity.uuid);
                        }).map(function(entity) {
                            return new Usergrid.Entity({
                                type: entity.type,
                                client: self._client,
                                uuid: entity.uuid,
                                response: entity
                            });
                        });
                    }
                    doCallback(callback, [ err, response, self ], self);
                });
            }
        }
    });
};

/*
 *  Retrieves the members of a group.
 *
 *  @method members
 *  @public
 *  @param {function} callback
 *  @return {function} callback(err, data);
 */
Usergrid.Group.prototype.members = function(callback) {
    return this._list;
};

/*
 *  Adds an existing user to the group, and refreshes the group object.
 *
 *  Options object: {user: user_entity}
 *
 *  @method add
 *  @public
 *  @params {object} options
 *  @param {function} callback
 *  @return {function} callback(err, data)
 */
Usergrid.Group.prototype.add = function(options, callback) {
    var self = this;
    if (options.user) {
        options = {
            method: "POST",
            endpoint: "groups/" + this._path + "/users/" + options.user.get("username")
        };
        this._client.request(options, function(error, response) {
            if (error) {
                doCallback(callback, [ error, response, self ], self);
            } else {
                self.fetch(callback);
            }
        });
    } else {
        doCallback(callback, [ new UsergridError("no user specified", "no_user_specified"), null, this ], this);
    }
};

/*
 *  Removes a user from a group, and refreshes the group object.
 *
 *  Options object: {user: user_entity}
 *
 *  @method remove
 *  @public
 *  @params {object} options
 *  @param {function} callback
 *  @return {function} callback(err, data)
 */
Usergrid.Group.prototype.remove = function(options, callback) {
    var self = this;
    if (options.user) {
        options = {
            method: "DELETE",
            endpoint: "groups/" + this._path + "/users/" + options.user.username
        };
        this._client.request(options, function(error, response) {
            if (error) {
                doCallback(callback, [ error, response, self ], self);
            } else {
                self.fetch(callback);
            }
        });
    } else {
        doCallback(callback, [ new UsergridError("no user specified", "no_user_specified"), null, this ], this);
    }
};

/*
 * Gets feed for a group.
 *
 * @public
 * @method feed
 * @param {function} callback
 * @returns {callback} callback(err, data, activities)
 */
Usergrid.Group.prototype.feed = function(callback) {
    var self = this;
    var options = {
        method: "GET",
        endpoint: "groups/" + this._path + "/feed"
    };
    this._client.request(options, function(err, response) {
        doCallback(callback, [ err, response, self ], self);
    });
};

/*
 * Creates activity and posts to group feed.
 *
 * options object: {user: user_entity, content: "activity content"}
 *
 * @public
 * @method createGroupActivity
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, entity)
 */
Usergrid.Group.prototype.createGroupActivity = function(options, callback) {
    var self = this;
    var user = options.user;
    var entity = new Usergrid.Entity({
        client: this._client,
        data: {
            actor: {
                displayName: user.get("username"),
                uuid: user.get("uuid"),
                username: user.get("username"),
                email: user.get("email"),
                picture: user.get("picture"),
                image: {
                    duration: 0,
                    height: 80,
                    url: user.get("picture"),
                    width: 80
                }
            },
            verb: "post",
            content: options.content,
            type: "groups/" + this._path + "/activities"
        }
    });
    entity.save(function(err, response, entity) {
        doCallback(callback, [ err, response, self ]);
    });
};

/*
 *  A class to model a Usergrid event.
 *
 *  @constructor
 *  @param {object} options {timestamp:0, category:'value', counters:{name : value}}
 *  @returns {callback} callback(err, event)
 */
Usergrid.Counter = function(options) {
    this._client = options.client;
    this._data = options.data || {};
    this._data.category = options.category || "UNKNOWN";
    this._data.timestamp = options.timestamp || 0;
    this._data.type = "events";
    this._data.counters = options.counters || {};
};

var COUNTER_RESOLUTIONS = [ "all", "minute", "five_minutes", "half_hour", "hour", "six_day", "day", "week", "month" ];

/*
 *  Inherit from Usergrid.Entity.
 *  Note: This only accounts for data on the group object itself.
 *  You need to use add and remove to manipulate group membership.
 */
Usergrid.Counter.prototype = new Usergrid.Entity();

/*
 * overrides Entity.prototype.fetch. Returns all data for counters
 * associated with the object as specified in the constructor
 *
 * @public
 * @method increment
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */
Usergrid.Counter.prototype.fetch = function(callback) {
    this.getData({}, callback);
};

/*
 * increments the counter for a specific event
 *
 * options object: {name: counter_name}
 *
 * @public
 * @method increment
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */
Usergrid.Counter.prototype.increment = function(options, callback) {
    var self = this, name = options.name, value = options.value;
    if (!name) {
        return doCallback(callback, [ new UsergridInvalidArgumentError("'name' for increment, decrement must be a number"), null, self ], self);
    } else if (isNaN(value)) {
        return doCallback(callback, [ new UsergridInvalidArgumentError("'value' for increment, decrement must be a number"), null, self ], self);
    } else {
        self._data.counters[name] = parseInt(value) || 1;
        return self.save(callback);
    }
};

/*
 * decrements the counter for a specific event
 *
 * options object: {name: counter_name}
 *
 * @public
 * @method decrement
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */
Usergrid.Counter.prototype.decrement = function(options, callback) {
    var self = this, name = options.name, value = options.value;
    self.increment({
        name: name,
        value: -(parseInt(value) || 1)
    }, callback);
};

/*
 * resets the counter for a specific event
 *
 * options object: {name: counter_name}
 *
 * @public
 * @method reset
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */
Usergrid.Counter.prototype.reset = function(options, callback) {
    var self = this, name = options.name;
    self.increment({
        name: name,
        value: 0
    }, callback);
};

/*
 * gets data for one or more counters over a given
 * time period at a specified resolution
 *
 * options object: {
 *                   counters: ['counter1', 'counter2', ...],
 *                   start: epoch timestamp or ISO date string,
 *                   end: epoch timestamp or ISO date string,
 *                   resolution: one of ('all', 'minute', 'five_minutes', 'half_hour', 'hour', 'six_day', 'day', 'week', or 'month')
 *                   }
 *
 * @public
 * @method getData
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */
Usergrid.Counter.prototype.getData = function(options, callback) {
    var start_time, end_time, start = options.start || 0, end = options.end || Date.now(), resolution = (options.resolution || "all").toLowerCase(), counters = options.counters || Object.keys(this._data.counters), res = (resolution || "all").toLowerCase();
    if (COUNTER_RESOLUTIONS.indexOf(res) === -1) {
        res = "all";
    }
    start_time = getSafeTime(start);
    end_time = getSafeTime(end);
    var self = this;
    var params = Object.keys(counters).map(function(counter) {
        return [ "counter", encodeURIComponent(counters[counter]) ].join("=");
    });
    params.push("resolution=" + res);
    params.push("start_time=" + String(start_time));
    params.push("end_time=" + String(end_time));
    var endpoint = "counters?" + params.join("&");
    this._client.request({
        endpoint: endpoint
    }, function(err, data) {
        if (data.counters && data.counters.length) {
            data.counters.forEach(function(counter) {
                self._data.counters[counter.name] = counter.value || counter.values;
            });
        }
        return doCallback(callback, [ err, data, self ], self);
    });
};

function getSafeTime(prop) {
    var time;
    switch (typeof prop) {
      case "undefined":
        time = Date.now();
        break;

      case "number":
        time = prop;
        break;

      case "string":
        time = isNaN(prop) ? Date.parse(prop) : parseInt(prop);
        break;

      default:
        time = Date.parse(prop.toString());
    }
    return time;
}

/*
 *  A class to model a Usergrid folder.
 *
 *  @constructor
 *  @param {object} options {name:"MyPhotos", path:"/user/uploads", owner:"00000000-0000-0000-0000-000000000000" }
 *  @returns {callback} callback(err, folder)
 */
Usergrid.Folder = function(options, callback) {
    var self = this, messages = [];
    console.log("FOLDER OPTIONS", options);
    self._client = options.client;
    self._data = options.data || {};
    self._data.type = "folders";
    var missingData = [ "name", "owner", "path" ].some(function(required) {
        return !(required in self._data);
    });
    if (missingData) {
        return doCallback(callback, [ new UsergridInvalidArgumentError("Invalid asset data: 'name', 'owner', and 'path' are required properties."), null, self ], self);
    }
    self.save(function(err, response) {
        if (err) {
            doCallback(callback, [ new UsergridError(response), response, self ], self);
        } else {
            if (response && response.entities && response.entities.length) {
                self.set(response.entities[0]);
            }
            doCallback(callback, [ null, response, self ], self);
        }
    });
};

/*
 *  Inherit from Usergrid.Entity.
 */
Usergrid.Folder.prototype = new Usergrid.Entity();

/*
 *  fetch the folder and associated assets
 *
 *  @method fetch
 *  @public
 *  @param {function} callback(err, self)
 *  @returns {callback} callback(err, self)
 */
Usergrid.Folder.prototype.fetch = function(callback) {
    var self = this;
    Usergrid.Entity.prototype.fetch.call(self, function(err, data) {
        console.log("self", self.get());
        console.log("data", data);
        if (!err) {
            self.getAssets(function(err, response) {
                if (err) {
                    doCallback(callback, [ new UsergridError(response), resonse, self ], self);
                } else {
                    doCallback(callback, [ null, self ], self);
                }
            });
        } else {
            doCallback(callback, [ null, data, self ], self);
        }
    });
};

/*
 *  Add an asset to the folder.
 *
 *  @method addAsset
 *  @public
 *  @param {object} options {asset:(uuid || Usergrid.Asset || {name:"photo.jpg", path:"/user/uploads", "content-type":"image/jpeg", owner:"F01DE600-0000-0000-0000-000000000000" }) }
 *  @returns {callback} callback(err, folder)
 */
Usergrid.Folder.prototype.addAsset = function(options, callback) {
    var self = this;
    if ("asset" in options) {
        var asset = null;
        switch (typeof options.asset) {
          case "object":
            asset = options.asset;
            if (!(asset instanceof Usergrid.Entity)) {
                asset = new Usergrid.Asset(asset);
            }
            break;

          case "string":
            if (isUUID(options.asset)) {
                asset = new Usergrid.Asset({
                    client: self._client,
                    data: {
                        uuid: options.asset,
                        type: "assets"
                    }
                });
            }
            break;
        }
        if (asset && asset instanceof Usergrid.Entity) {
            asset.fetch(function(err, data) {
                if (err) {
                    doCallback(callback, [ new UsergridError(data), data, self ], self);
                } else {
                    var endpoint = [ "folders", self.get("uuid"), "assets", asset.get("uuid") ].join("/");
                    var options = {
                        method: "POST",
                        endpoint: endpoint
                    };
                    self._client.request(options, callback);
                }
            });
        }
    } else {
        doCallback(callback, [ new UsergridInvalidArgumentError("No asset specified"), null, self ], self);
    }
};

/*
 *  Remove an asset from the folder.
 *
 *  @method removeAsset
 *  @public
 *  @param {object} options {asset:(uuid || Usergrid.Asset || {name:"photo.jpg", path:"/user/uploads", "content-type":"image/jpeg", owner:"F01DE600-0000-0000-0000-000000000000" }) }
 *  @returns {callback} callback(err, folder)
 */
Usergrid.Folder.prototype.removeAsset = function(options, callback) {
    var self = this;
    if ("asset" in options) {
        var asset = null;
        switch (typeof options.asset) {
          case "object":
            asset = options.asset;
            break;

          case "string":
            if (isUUID(options.asset)) {
                asset = new Usergrid.Asset({
                    client: self._client,
                    data: {
                        uuid: options.asset,
                        type: "assets"
                    }
                });
            }
            break;
        }
        if (asset && asset !== null) {
            var endpoint = [ "folders", self.get("uuid"), "assets", asset.get("uuid") ].join("/");
            self._client.request({
                method: "DELETE",
                endpoint: endpoint
            }, function(err, response) {
                if (err) {
                    doCallback(callback, [ new UsergridError(response), response, self ], self);
                } else {
                    doCallback(callback, [ null, response, self ], self);
                }
            });
        }
    } else {
        doCallback(callback, [ new UsergridInvalidArgumentError("No asset specified"), null, self ], self);
    }
};

/*
 *  List the assets in the folder.
 *
 *  @method getAssets
 *  @public
 *  @returns {callback} callback(err, assets)
 */
Usergrid.Folder.prototype.getAssets = function(callback) {
    return this.getConnections("assets", callback);
};

/*
 *  XMLHttpRequest.prototype.sendAsBinary polyfill
 *  from: https://developer.mozilla.org/en-US/docs/DOM/XMLHttpRequest#sendAsBinary()
 *
 *  @method sendAsBinary
 *  @param {string} sData
 */
if (!XMLHttpRequest.prototype.sendAsBinary) {
    XMLHttpRequest.prototype.sendAsBinary = function(sData) {
        var nBytes = sData.length, ui8Data = new Uint8Array(nBytes);
        for (var nIdx = 0; nIdx < nBytes; nIdx++) {
            ui8Data[nIdx] = sData.charCodeAt(nIdx) & 255;
        }
        this.send(ui8Data);
    };
}

/*
 *  A class to model a Usergrid asset.
 *
 *  @constructor
 *  @param {object} options {name:"photo.jpg", path:"/user/uploads", "content-type":"image/jpeg", owner:"F01DE600-0000-0000-0000-000000000000" }
 *  @returns {callback} callback(err, asset)
 */
Usergrid.Asset = function(options, callback) {
    var self = this, messages = [];
    self._client = options.client;
    self._data = options.data || {};
    self._data.type = "assets";
    var missingData = [ "name", "owner", "path" ].some(function(required) {
        return !(required in self._data);
    });
    if (missingData) {
        doCallback(callback, [ new UsergridError("Invalid asset data: 'name', 'owner', and 'path' are required properties."), null, self ], self);
    } else {
        self.save(function(err, data) {
            if (err) {
                doCallback(callback, [ new UsergridError(data), data, self ], self);
            } else {
                if (data && data.entities && data.entities.length) {
                    self.set(data.entities[0]);
                }
                doCallback(callback, [ null, data, self ], self);
            }
        });
    }
};

/*
 *  Inherit from Usergrid.Entity.
 */
Usergrid.Asset.prototype = new Usergrid.Entity();

/*
 *  Add an asset to a folder.
 *
 *  @method connect
 *  @public
 *  @param {object} options {folder:"F01DE600-0000-0000-0000-000000000000"}
 *  @returns {callback} callback(err, asset)
 */
Usergrid.Asset.prototype.addToFolder = function(options, callback) {
    var self = this, error = null;
    if ("folder" in options && isUUID(options.folder)) {
        var folder = Usergrid.Folder({
            uuid: options.folder
        }, function(err, folder) {
            if (err) {
                doCallback(callback, [ UsergridError.fromResponse(folder), folder, self ], self);
            } else {
                var endpoint = [ "folders", folder.get("uuid"), "assets", self.get("uuid") ].join("/");
                var options = {
                    method: "POST",
                    endpoint: endpoint
                };
                this._client.request(options, function(err, response) {
                    if (err) {
                        doCallback(callback, [ UsergridError.fromResponse(folder), response, self ], self);
                    } else {
                        doCallback(callback, [ null, folder, self ], self);
                    }
                });
            }
        });
    } else {
        doCallback(callback, [ new UsergridError("folder not specified"), null, self ], self);
    }
};

Usergrid.Entity.prototype.attachAsset = function(file, callback) {
    if (!(window.File && window.FileReader && window.FileList && window.Blob)) {
        doCallback(callback, [ new UsergridError("The File APIs are not fully supported by your browser."), null, this ], this);
        return;
    }
    var self = this;
    var args = arguments;
    var type = this._data.type;
    var attempts = self.get("attempts");
    if (isNaN(attempts)) {
        attempts = 3;
    }
    if (type != "assets" && type != "asset") {
        var endpoint = [ this._client.URI, this._client.orgName, this._client.appName, type, self.get("uuid") ].join("/");
    } else {
        self.set("content-type", file.type);
        self.set("size", file.size);
        var endpoint = [ this._client.URI, this._client.orgName, this._client.appName, "assets", self.get("uuid"), "data" ].join("/");
    }
    var xhr = new XMLHttpRequest();
    xhr.open("POST", endpoint, true);
    xhr.onerror = function(err) {
        doCallback(callback, [ new UsergridError("The File APIs are not fully supported by your browser.") ], xhr, self);
    };
    xhr.onload = function(ev) {
        if (xhr.status >= 500 && attempts > 0) {
            self.set("attempts", --attempts);
            setTimeout(function() {
                self.attachAsset.apply(self, args);
            }, 100);
        } else if (xhr.status >= 300) {
            self.set("attempts");
            doCallback(callback, [ new UsergridError(JSON.parse(xhr.responseText)), xhr, self ], self);
        } else {
            self.set("attempts");
            self.fetch();
            doCallback(callback, [ null, xhr, self ], self);
        }
    };
    var fr = new FileReader();
    fr.onload = function() {
        var binary = fr.result;
        if (type === "assets" || type === "asset") {
            xhr.overrideMimeType("application/octet-stream");
            xhr.setRequestHeader("Content-Type", "application/octet-stream");
        }
        xhr.sendAsBinary(binary);
    };
    fr.readAsBinaryString(file);
};

/*
 *  Upload Asset data
 *
 *  @method upload
 *  @public
 *  @param {object} data Can be a javascript Blob or File object
 *  @returns {callback} callback(err, asset)
 */
Usergrid.Asset.prototype.upload = function(data, callback) {
    this.attachAsset(data, function(err, response) {
        if (!err) {
            doCallback(callback, [ null, response, self ], self);
        } else {
            doCallback(callback, [ new UsergridError(err), response, self ], self);
        }
    });
};

/*
 *  Download Asset data
 *
 *  @method download
 *  @public
 *  @returns {callback} callback(err, blob) blob is a javascript Blob object.
 */
Usergrid.Entity.prototype.downloadAsset = function(callback) {
    var self = this;
    var endpoint;
    var type = this._data.type;
    var xhr = new XMLHttpRequest();
    if (type != "assets" && type != "asset") {
        endpoint = [ this._client.URI, this._client.orgName, this._client.appName, type, self.get("uuid") ].join("/");
    } else {
        endpoint = [ this._client.URI, this._client.orgName, this._client.appName, "assets", self.get("uuid"), "data" ].join("/");
    }
    xhr.open("GET", endpoint, true);
    xhr.responseType = "blob";
    xhr.onload = function(ev) {
        var blob = xhr.response;
        if (type != "assets" && type != "asset") {
            doCallback(callback, [ null, blob, xhr ], self);
        } else {
            doCallback(callback, [ null, xhr, self ], self);
        }
    };
    xhr.onerror = function(err) {
        callback(true, err);
        doCallback(callback, [ new UsergridError(err), xhr, self ], self);
    };
    if (type != "assets" && type != "asset") {
        xhr.setRequestHeader("Accept", self._data["file-metadata"]["content-type"]);
    } else {
        xhr.overrideMimeType(self.get("content-type"));
    }
    xhr.send();
};

/*
 *  Download Asset data
 *
 *  @method download
 *  @public
 *  @returns {callback} callback(err, blob) blob is a javascript Blob object.
 */
Usergrid.Asset.prototype.download = function(callback) {
    this.downloadAsset(function(err, response) {
        if (!err) {
            doCallback(callback, [ null, response, self ], self);
        } else {
            doCallback(callback, [ new UsergridError(err), response, self ], self);
        }
    });
};

/**
 * Created by ryan bridges on 2014-02-05.
 */
(function(global) {
    var name = "UsergridError", short, _name = global[name], _short = short && short !== undefined ? global[short] : undefined;
    /*
     *  Instantiates a new UsergridError
     *
     *  @method UsergridError
     *  @public
     *  @params {<string>} message
     *  @params {<string>} id       - the error code, id, or name
     *  @params {<int>} timestamp
     *  @params {<int>} duration
     *  @params {<string>} exception    - the Java exception from Usergrid
     *  @return Returns - a new UsergridError object
     *
     *  Example:
     *
     *  UsergridError(message);
     */
    function UsergridError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name;
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridError.prototype = new Error();
    UsergridError.prototype.constructor = UsergridError;
    /*
     *  Creates a UsergridError from the JSON response returned from the backend
     *
     *  @method fromResponse
     *  @public
     *  @params {object} response - the deserialized HTTP response from the Usergrid API
     *  @return Returns a new UsergridError object.
     *
     *  Example:
     *  {
     *  "error":"organization_application_not_found",
     *  "timestamp":1391618508079,
     *  "duration":0,
     *  "exception":"org.usergrid.rest.exceptions.OrganizationApplicationNotFoundException",
     *  "error_description":"Could not find application for yourorgname/sandboxxxxx from URI: yourorgname/sandboxxxxx"
     *  }
     */
    UsergridError.fromResponse = function(response) {
        if (response && "undefined" !== typeof response) {
            return new UsergridError(response.error_description, response.error, response.timestamp, response.duration, response.exception);
        } else {
            return new UsergridError();
        }
    };
    UsergridError.createSubClass = function(name) {
        if (name in global && global[name]) return global[name];
        global[name] = function() {};
        global[name].name = name;
        global[name].prototype = new UsergridError();
        return global[name];
    };
    function UsergridHTTPResponseError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name;
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridHTTPResponseError.prototype = new UsergridError();
    function UsergridInvalidHTTPMethodError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name || "invalid_http_method";
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridInvalidHTTPMethodError.prototype = new UsergridError();
    function UsergridInvalidURIError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name || "invalid_uri";
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridInvalidURIError.prototype = new UsergridError();
    function UsergridInvalidArgumentError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name || "invalid_argument";
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridInvalidArgumentError.prototype = new UsergridError();
    function UsergridKeystoreDatabaseUpgradeNeededError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name;
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridKeystoreDatabaseUpgradeNeededError.prototype = new UsergridError();
    global.UsergridHTTPResponseError = UsergridHTTPResponseError;
    global.UsergridInvalidHTTPMethodError = UsergridInvalidHTTPMethodError;
    global.UsergridInvalidURIError = UsergridInvalidURIError;
    global.UsergridInvalidArgumentError = UsergridInvalidArgumentError;
    global.UsergridKeystoreDatabaseUpgradeNeededError = UsergridKeystoreDatabaseUpgradeNeededError;
    global[name] = UsergridError;
    if (short !== undefined) {
        global[short] = UsergridError;
    }
    global[name].noConflict = function() {
        if (_name) {
            global[name] = _name;
        }
        if (short !== undefined) {
            global[short] = _short;
        }
        return UsergridError;
    };
    return global[name];
})(this);