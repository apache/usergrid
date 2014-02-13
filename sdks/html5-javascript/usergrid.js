/*! usergrid@0.0.0 2014-02-13 */
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

//Logger
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

//Promise
(function(global) {
    var name = "Promise", overwrittenName = global[name], exports;
    function Promise() {
        this.complete = false;
        this.error = null;
        this.result = null;
        this.callbacks = [];
    }
    Promise.prototype.create = function() {
        return new Promise();
    };
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
                //self.logger.info(promises.length)
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

//Ajax
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
                    this.readyState ^ 4 || (self.logger.timeEnd(m + " " + u), clearTimeout(timeout), 
                    p.done(null, this));
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
                //TODO stick that timeout in a config variable
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
//Hack around IE console.log
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

function NOOP() {}

//Usergrid namespace encapsulates this SDK
/*window.Usergrid = window.Usergrid || {};
Usergrid = Usergrid || {};
Usergrid.USERGRID_SDK_VERSION = '0.10.07';*/
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
        //try {
        returnValue = callback.apply(context, params);
    }
    return returnValue;
}

//noinspection ThisExpressionReferencesGlobalObjectJS
(function(global) {
    var name = "Usergrid", overwrittenName = global[name];
    function Usergrid() {
        this.logger = new Logger(name);
    }
    Usergrid.isValidEndpoint = function(endpoint) {
        //TODO actually implement this
        return true;
    };
    var VALID_REQUEST_METHODS = [ "GET", "POST", "PUT", "DELETE" ];
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
        //this.query_params = query_params;
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
    //TODO more granular handling of statusCodes
    Usergrid.Response = function(err, response) {
        var p = new Promise();
        var data = null;
        try {
            data = JSON.parse(response.responseText);
        } catch (e) {
            //this.logger.error("Error parsing response text: ",this.text);
            //this.logger.error("Caught error ", e.message);
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
            //server error
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
        var entities = [];
        if (this.success) {
            entities = this.data ? this.data.entities : this.entities;
        }
        return entities;
    };
    Usergrid.Response.prototype.getEntity = function() {
        var entities = this.getEntities();
        return entities[0];
    };
    //Usergrid.Entity=function(){};
    //Usergrid.Collection=function(){};
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
    Usergrid.Client = function(options) {
        //usergrid endpoint
        this.URI = options.URI || "https://api.usergrid.com";
        //Find your Orgname and Appname in the Admin portal (http://apigee.com/usergrid)
        if (options.orgName) {
            this.set("orgName", options.orgName);
        }
        if (options.appName) {
            this.set("appName", options.appName);
        }
        //other options
        this.buildCurl = options.buildCurl || false;
        this.logging = options.logging || false;
        //timeout and callbacks
        this._callTimeout = options.callTimeout || 3e4;
        //default to 30 seconds
        this._callTimeoutCallback = options.callTimeoutCallback || null;
        this.logoutCallback = options.logoutCallback || null;
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
        var self = this;
        var method = options.method || "GET";
        var endpoint = options.endpoint;
        var body = options.body || {};
        var qs = options.qs || {};
        var mQuery = options.mQuery || false;
        //is this a query to the management endpoint?
        var orgName = this.get("orgName");
        var appName = this.get("appName");
        var uri;
        var logoutCallback = function() {
            if (typeof this.logoutCallback === "function") {
                return this.logoutCallback(true, "no_org_or_app_name_specified");
            }
        }.bind(this);
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
        var req = new Usergrid.Request(method, uri, qs, body, function(err, response) {
            if ([ "auth_expired_session_token", "auth_missing_credentials", "auth_unverified_oath", "expired_token", "unauthorized", "auth_invalid" ].indexOf(response.error) !== -1) {
                return logoutCallback();
            }
            doCallback(callback, [ err, response ]);
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
        //append params to the path
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
        var getOnExist = options.getOnExist || false;
        options = {
            path: options.path,
            client: this,
            data: options
        };
        var group = new Usergrid.Group(options);
        group.fetch(function(err, data) {
            var okToSave = err && [ "service_resource_not_found", "no_name_specified", "null_pointer" ].indexOf(err.name) !== -1 || !err && getOnExist;
            if (okToSave) {
                group.save(function(err, data) {
                    doCallback(callback, [ err, group, data ]);
                });
            } else {
                doCallback(callback, [ null, group, data ]);
            }
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
        // todo: replace the check for new / save on not found code with simple save
        // when users PUT on no user fix is in place.
        var getOnExist = options["getOnExist"] || false;
        //if true, will return entity if one already exists
        delete options["getOnExist"];
        //so it doesn't become part of our data model
        var entity_data = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(entity_data);
        var self = this;
        entity.fetch(function(err, data) {
            //if the fetch doesn't find what we are looking for, or there is no error, do a save
            var common_errors = [ "service_resource_not_found", "no_name_specified", "null_pointer" ];
            var okToSave = !err && getOnExist || err && err.name && common_errors.indexOf(err.name) !== -1;
            if (okToSave) {
                entity.set(entity_data.data);
                //add the data again just in case
                entity.save(function(err, data) {
                    doCallback(callback, [ err, entity, data ]);
                });
            } else {
                doCallback(callback, [ null, entity, data ]);
            }
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
        var options = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(options);
        entity.fetch(function(err, data) {
            doCallback(callback, [ err, entity, data ]);
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
        var collection = new Usergrid.Collection(options, function(err, data) {
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
        var options = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(options);
        entity.save(function(err, data) {
            doCallback(callback, [ err, entity ]);
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
        self.request(options, function(err, data) {
            var user = {};
            if (err) {
                if (self.logging) console.log("error trying to log user in");
            } else {
                var options = {
                    client: self,
                    data: data.user
                };
                user = new Usergrid.Entity(options);
                self.setToken(data.access_token);
            }
            doCallback(callback, [ err, data, user ]);
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
                //save the re-authed token and current email/username
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
                //delete next block and corresponding function when iframes are refactored
                localStorage.setItem("accessToken", data.token);
                localStorage.setItem("userUUID", data.uuid);
                localStorage.setItem("userEmail", data.email);
                //end delete block
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
                    //if we have an org stored, then use that one. Otherwise, use the first one.
                    var existingOrg = self.get("orgName");
                    org = organizations[existingOrg] ? organizations[existingOrg] : organizations[Object.keys(organizations)[0]];
                    self.set("orgName", org.name);
                } catch (e) {
                    err = true;
                    if (self.logging) {
                        console.log("error selecting org");
                    }
                }
                //should always be an org
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
        if (!this.getToken()) {
            callback(true, null, null);
        } else {
            var self = this;
            var options = {
                method: "GET",
                endpoint: "users/me"
            };
            this.request(options, function(err, data) {
                if (err) {
                    if (self.logging) {
                        console.log("error trying to log user in");
                    }
                    doCallback(callback, [ err, data, null ], self);
                } else {
                    var options = {
                        client: self,
                        data: data.entities[0]
                    };
                    var user = new Usergrid.Entity(options);
                    doCallback(callback, [ null, data, user ], self);
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
        //curl - add the method to the command (no need to add anything for GET)
        curl.push("-X");
        curl.push([ "POST", "PUT", "DELETE" ].indexOf(method) >= 0 ? method : "GET");
        //curl - append the path
        curl.push(uri);
        if ("object" === typeof body && Object.keys(body).length > 0 && [ "POST", "PUT" ].indexOf(method) !== -1) {
            curl.push("-d");
            curl.push("'" + JSON.stringify(body) + "'");
        }
        curl = curl.join(" ");
        //log the curl command to the console
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
    if (options) {
        this._data = options.data || {};
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
    var type = this.get("type"), name, endpoint;
    var nameProperties = [ "uuid", "name" ];
    if (type === undefined) {
        throw new UsergridError("cannot fetch entity, no entity type specified", "no_type_specified");
    } else if (type === "users" || type === "user") {
        nameProperties.unshift("username");
    }
    var names = this.get(nameProperties).filter(function(x) {
        return x != null && "undefined" !== typeof x;
    });
    if (names.length === 0) {
        return type;
    } else {
        name = names.shift();
    }
    return [ type, name ].join("/");
};

/*
 *  Saves the entity back to the database
 *
 *  @method save
 *  @public
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 */
Usergrid.Entity.prototype.save = function(callback) {
    var self = this, type = this.get("type"), method = "POST", entityId = this.get("uuid"), data = {}, entityData = this.get(), password = this.get("password"), oldpassword = this.get("oldpassword"), newpassword = this.get("newpassword"), options = {
        method: method,
        endpoint: type
    };
    //update the entity
    if (entityId) {
        options.method = "PUT";
        options.endpoint += "/" + entityId;
    }
    //remove system-specific properties
    Object.keys(entityData).filter(function(key) {
        return ENTITY_SYSTEM_PROPERTIES.indexOf(key) === -1;
    }).forEach(function(key) {
        data[key] = entityData[key];
    });
    options.body = data;
    //save the entity first
    this._client.request(options, function(err, response) {
        var entity = response.getEntity();
        if (entity) {
            self.set(entity);
            self.set("type", /^\//.test(response.path) ? response.path.substring(1) : response.path);
        }
        //      doCallback(callback,[err, self]);
        /*
        TODO move user logic to its own entity
       */
        //clear out pw info if present
        self.set("password", null);
        self.set("oldpassword", null);
        self.set("newpassword", null);
        if (err && self._client.logging) {
            console.log("could not save entity");
            doCallback(callback, [ err, response, self ]);
        } else if (/^users?/.test(self.get("type")) && oldpassword && newpassword) {
            //if this is a user, update the password if it has been specified;
            //Note: we have a ticket in to change PUT calls to /users to accept the password change
            //      once that is done, we will remove this call and merge it all into one
            var options = {
                method: "PUT",
                endpoint: type + "/" + self.get("uuid") + "/password",
                body: {
                    uuid: self.get("uuid"),
                    username: self.get("username"),
                    password: password,
                    oldpassword: oldpassword,
                    newpassword: newpassword
                }
            };
            self._client.request(options, function(err, data) {
                if (err && self._client.logging) {
                    console.log("could not update user");
                }
                //remove old and new password fields so they don't end up as part of the entity object
                self.set({
                    password: null,
                    oldpassword: null,
                    newpassword: null
                });
                doCallback(callback, [ err, data, self ]);
            });
        } else {
            doCallback(callback, [ err, response, self ]);
        }
    });
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
        doCallback(callback, [ err, entity, self ]);
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
    this._client.request(options, function(err, data) {
        if (!err) {
            self.set(null);
        }
        doCallback(callback, [ err, data ]);
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
    var self = this;
    var error;
    //connectee info
    var connecteeType = entity.get("type");
    var connectee = this.getEntityId(entity);
    if (!connectee) {
        if (typeof callback === "function") {
            error = "Error trying to delete object - no uuid specified.";
            if (self._client.logging) {
                console.log(error);
            }
            doCallback(callback, [ true, error ], self);
        }
        return;
    }
    //connector info
    var connectorType = this.get("type");
    var connector = this.getEntityId(this);
    if (!connector) {
        if (typeof callback === "function") {
            error = "Error in connect - no uuid specified.";
            if (self._client.logging) {
                console.log(error);
            }
            doCallback(callback, [ true, error ], self);
        }
        return;
    }
    var endpoint = connectorType + "/" + connector + "/" + connection + "/" + connecteeType + "/" + connectee;
    var options = {
        method: "POST",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("entity could not be connected");
        }
        doCallback(callback, [ err, data ], self);
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
    var id = false;
    if (isUUID(entity.get("uuid"))) {
        id = entity.get("uuid");
    } else {
        if (this.get("type") === "users") {
            id = entity.get("username");
        } else if (entity.get("name")) {
            id = entity.get("name");
        }
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
    //connector info
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
                ops_part.replace("*", "get,post,put,delete");
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
    var self = this;
    var error;
    //connectee info
    var connecteeType = entity.get("type");
    var connectee = this.getEntityId(entity);
    if (!connectee) {
        if (typeof callback === "function") {
            error = "Error trying to delete object - no uuid specified.";
            if (self._client.logging) {
                console.log(error);
            }
            doCallback(callback, [ true, error ], self);
        }
        return;
    }
    //connector info
    var connectorType = this.get("type");
    var connector = this.getEntityId(this);
    if (!connector) {
        if (typeof callback === "function") {
            error = "Error in connect - no uuid specified.";
            if (self._client.logging) {
                console.log(error);
            }
            doCallback(callback, [ true, error ], self);
        }
        return;
    }
    var endpoint = connectorType + "/" + connector + "/" + connection + "/" + connecteeType + "/" + connectee;
    var options = {
        method: "DELETE",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("entity could not be disconnected");
        }
        doCallback(callback, [ err, data ], self);
    });
};

/*
 *  The Collection class models Usergrid Collections.  It essentially
 *  acts as a container for holding Entity objects, while providing
 *  additional funcitonality such as paging, and saving
 *
 *  @constructor
 *  @param {string} options - configuration object
 *  @param {function} callback
 *  @return {callback} callback(err, data)
 */
Usergrid.Collection = function(options, callback) {
    if (options) {
        this._client = options.client;
        this._type = options.type;
        this.qs = options.qs || {};
        //iteration
        this._list = options.list || [];
        this._iterator = options.iterator || -1;
        //first thing we do is increment, so set to -1
        //paging
        this._previous = options.previous || [];
        this._next = options.next || null;
        this._cursor = options.cursor || null;
        //restore entities if available
        if (options.list) {
            var count = options.list.length;
            for (var i = 0; i < count; i++) {
                //make new entity with
                var entity = this._client.restoreEntity(options.list[i]);
                this._list[i] = entity;
            }
        }
    }
    if (callback) {
        //populate the collection
        this.fetch(callback);
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
    //pull out the state from this object and return it
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

Usergrid.Collection.prototype.addCollection = function(collectionName, options, callback) {
    self = this;
    options.client = this._client;
    var collection = new Usergrid.Collection(options, function(err, data) {
        if (typeof callback === "function") {
            collection.resetEntityPointer();
            while (collection.hasNextEntity()) {
                var user = collection.getNextEntity();
                var email = user.get("email");
                var image = self._client.getDisplayImage(user.get("email"), user.get("picture"));
                user._portal_image_icon = image;
            }
            self[collectionName] = collection;
            doCallback(callback, [ err, collection ], self);
        }
    });
};

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
    //add in the cursor if one is available
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
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log("error getting collection");
        } else {
            //save the cursor if there is one
            var cursor = data.cursor || null;
            self.saveCursor(cursor);
            if (data.entities) {
                self.resetEntityPointer();
                var count = data.entities.length;
                //save entities locally
                self._list = [];
                //clear the local list first
                for (var i = 0; i < count; i++) {
                    var uuid = data.entities[i].uuid;
                    if (uuid) {
                        var entityData = data.entities[i] || {};
                        self._baseType = data.entities[i].type;
                        //store the base type in the collection
                        entityData.type = self._type;
                        //make sure entities are same type (have same path) as parent collection.
                        var entityOptions = {
                            type: self._type,
                            client: self._client,
                            uuid: uuid,
                            data: entityData
                        };
                        var ent = new Usergrid.Entity(entityOptions);
                        ent._json = JSON.stringify(entityData, null, 2);
                        var ct = self._list.length;
                        self._list[ct] = ent;
                    }
                }
            }
        }
        doCallback(callback, [ err, data ], self);
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
Usergrid.Collection.prototype.addEntity = function(options, callback) {
    var self = this;
    options.type = this._type;
    //create the new entity
    this._client.createEntity(options, function(err, entity) {
        if (!err) {
            //then add the entity to the list
            var count = self._list.length;
            self._list[count] = entity;
        }
        doCallback(callback, [ err, entity ], self);
    });
};

Usergrid.Collection.prototype.addExistingEntity = function(entity) {
    //entity should already exist in the db, so just add it to the list
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
    entity.destroy(function(err, data) {
        if (err) {
            if (self._client.logging) {
                console.log("could not destroy entity");
            }
            doCallback(callback, [ err, data ], self);
        } else {
            //destroy was good, so repopulate the collection
            self.fetch(callback);
        }
    });
    //remove entity from the local store
    this.removeEntity(entity);
};

Usergrid.Collection.prototype.removeEntity = function(entity) {
    var uuid = entity.get("uuid");
    for (var key in this._list) {
        var listItem = this._list[key];
        if (listItem.get("uuid") === uuid) {
            return this._list.splice(key, 1);
        }
    }
    return false;
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
    for (var key in this._list) {
        var listItem = this._list[key];
        if (listItem.get("uuid") === uuid) {
            return callback(null, listItem);
        }
    }
    //get the entity from the database
    var options = {
        data: {
            type: this._type,
            uuid: uuid
        },
        client: this._client
    };
    var entity = new Usergrid.Entity(options);
    entity.fetch(callback);
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
    //if current cursor is different, grab it for next cursor
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
        //set the cursor to the next page of data
        this._previous.push(this._cursor);
        this._cursor = this._next;
        //empty the list
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
        //clear out next so the comparison will find the next item
        this._cursor = this._previous.pop();
        //empty the list
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
    this._client.request(groupOptions, function(err, data) {
        if (err) {
            if (self._client.logging) {
                console.log("error getting group");
            }
            doCallback(callback, [ err, data ], self);
        } else {
            if (data.entities && data.entities.length) {
                var groupData = data.entities[0];
                self._data = groupData || {};
                self._client.request(memberOptions, function(err, data) {
                    if (err && self._client.logging) {
                        console.log("error getting group users");
                    } else {
                        if (data.entities) {
                            var count = data.entities.length;
                            self._list = [];
                            for (var i = 0; i < count; i++) {
                                var uuid = data.entities[i].uuid;
                                if (uuid) {
                                    var entityData = data.entities[i] || {};
                                    var entityOptions = {
                                        type: entityData.type,
                                        client: self._client,
                                        uuid: uuid,
                                        data: entityData
                                    };
                                    var entity = new Usergrid.Entity(entityOptions);
                                    self._list.push(entity);
                                }
                            }
                        }
                    }
                    doCallback(callback, [ err, data, self._list ], self);
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
    doCallback(callback, [ null, this._list ], this);
};

/*
 *  Adds a user to the group, and refreshes the group object.
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
    var options = {
        method: "POST",
        endpoint: "groups/" + this._path + "/users/" + options.user.get("username")
    };
    this._client.request(options, function(error, data) {
        if (error) {
            doCallback(callback, [ error, data, data.entities ], self);
        } else {
            self.fetch(callback);
        }
    });
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
    var options = {
        method: "DELETE",
        endpoint: "groups/" + this._path + "/users/" + options.user.get("username")
    };
    this._client.request(options, function(error, data) {
        if (error) {
            doCallback(callback, [ error, data ], self);
        } else {
            self.fetch(callback);
        }
    });
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
    var endpoint = "groups/" + this._path + "/feed";
    var options = {
        method: "GET",
        endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
        if (err && self.logging) {
            console.log("error trying to log user in");
        }
        doCallback(callback, [ err, data, data.entities ], self);
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
    var user = options.user;
    options = {
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
    };
    var entity = new Usergrid.Entity(options);
    entity.save(function(err, data) {
        doCallback(callback, [ err, entity ]);
    });
};

/*
 *  A class to model a Usergrid event.
 *
 *  @constructor
 *  @param {object} options {timestamp:0, category:'value', counters:{name : value}}
 *  @returns {callback} callback(err, event)
 */
Usergrid.Counter = function(options, callback) {
    var self = this;
    this._client = options.client;
    this._data = options.data || {};
    this._data.category = options.category || "UNKNOWN";
    this._data.timestamp = options.timestamp || 0;
    this._data.type = "events";
    this._data.counters = options.counters || {};
    doCallback(callback, [ false, self ], self);
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
        return doCallback(callback, [ true, "'name' for increment, decrement must be a number" ], self);
    } else if (isNaN(value)) {
        return doCallback(callback, [ true, "'value' for increment, decrement must be a number" ], self);
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
    if (start) {
        switch (typeof start) {
          case "undefined":
            start_time = 0;
            break;

          case "number":
            start_time = start;
            break;

          case "string":
            start_time = isNaN(start) ? Date.parse(start) : parseInt(start);
            break;

          default:
            start_time = Date.parse(start.toString());
        }
    }
    if (end) {
        switch (typeof end) {
          case "undefined":
            end_time = Date.now();
            break;

          case "number":
            end_time = end;
            break;

          case "string":
            end_time = isNaN(end) ? Date.parse(end) : parseInt(end);
            break;

          default:
            end_time = Date.parse(end.toString());
        }
    }
    var self = this;
    //https://api.usergrid.com/yourorgname/sandbox/counters?counter=test_counter
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
        return doCallback(callback, [ err, data ], self);
    });
};

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
        return doCallback(callback, [ true, new Usergrid.Error("Invalid asset data: 'name', 'owner', and 'path' are required properties.") ], self);
    }
    self.save(function(err, data) {
        if (err) {
            doCallback(callback, [ true, new Usergrid.Error(data) ], self);
        } else {
            if (data && data.entities && data.entities.length) {
                self.set(data.entities[0]);
            }
            doCallback(callback, [ false, self ], self);
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
            self.getAssets(function(err, data) {
                if (err) {
                    doCallback(callback, [ true, new UsergridError(data) ], self);
                } else {
                    doCallback(callback, [ null, self ], self);
                }
            });
        } else {
            doCallback(callback, [ true, new UsergridError(data) ], self);
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
                    doCallback(callback, [ err, new UsergridError(data) ], self);
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
        //nothing to add
        doCallback(callback, [ true, {
            error_description: "No asset specified"
        } ], self);
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
            }, callback);
        }
    } else {
        //nothing to add
        doCallback(callback, [ true, {
            error_description: "No asset specified"
        } ], self);
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
        return doCallback(callback, [ true, new Usergrid.Error("Invalid asset data: 'name', 'owner', and 'path' are required properties.") ], self);
    }
    self.save(function(err, data) {
        if (err) {
            doCallback(callback, [ true, new Usergrid.Error(data) ], self);
        } else {
            if (data && data.entities && data.entities.length) {
                self.set(data.entities[0]);
            }
            doCallback(callback, [ false, self ], self);
        }
    });
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
        //we got a valid UUID
        var folder = Usergrid.Folder({
            uuid: options.folder
        }, function(err, folder) {
            if (err) {
                return callback.call(self, err, folder);
            }
            var endpoint = [ "folders", folder.get("uuid"), "assets", self.get("uuid") ].join("/");
            var options = {
                method: "POST",
                endpoint: endpoint
            };
            this._client.request(options, callback);
        });
    } else {
        doCallback(callback, [ true, new UsergridError("folder not specified") ], self);
    }
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
    if (!(window.File && window.FileReader && window.FileList && window.Blob)) {
        return doCallback(callback, [ true, new UsergridError("The File APIs are not fully supported by your browser.") ], self);
    }
    var self = this;
    var endpoint = [ this._client.URI, this._client.orgName, this._client.appName, "assets", self.get("uuid"), "data" ].join("/");
    //self._client.buildAssetURL(self.get("uuid"));
    var xhr = new XMLHttpRequest();
    xhr.open("POST", endpoint, true);
    xhr.onerror = function(err) {
        //callback(true, err);
        doCallback(callback, [ true, new UsergridError("The File APIs are not fully supported by your browser.") ], self);
    };
    xhr.onload = function(ev) {
        if (xhr.status >= 300) {
            doCallback(callback, [ true, new UsergridError(JSON.parse(xhr.responseText)) ], self);
        } else {
            doCallback(callback, [ null, self ], self);
        }
    };
    var fr = new FileReader();
    fr.onload = function() {
        var binary = fr.result;
        xhr.overrideMimeType("application/octet-stream");
        setTimeout(function() {
            xhr.sendAsBinary(binary);
        }, 1e3);
    };
    fr.readAsBinaryString(data);
};

/*
 *  Download Asset data
 *
 *  @method download
 *  @public
 *  @returns {callback} callback(err, blob) blob is a javascript Blob object.
 */
Usergrid.Asset.prototype.download = function(callback) {
    var self = this;
    var endpoint = [ this._client.URI, this._client.orgName, this._client.appName, "assets", self.get("uuid"), "data" ].join("/");
    var xhr = new XMLHttpRequest();
    xhr.open("GET", endpoint, true);
    xhr.responseType = "blob";
    xhr.onload = function(ev) {
        var blob = xhr.response;
        //callback(null, blob);
        doCallback(callback, [ false, blob ], self);
    };
    xhr.onerror = function(err) {
        callback(true, err);
        doCallback(callback, [ true, new UsergridError(err) ], self);
    };
    xhr.send();
};

//noinspection ThisExpressionReferencesGlobalObjectJS
/**
 * Created by ryan bridges on 2014-02-05.
 */
(function(global) {
    //noinspection JSUnusedAssignment
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
        this.name = name;
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridInvalidHTTPMethodError.prototype = new UsergridError();
    function UsergridInvalidURIError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name;
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridInvalidURIError.prototype = new UsergridError();
    function UsergridKeystoreDatabaseUpgradeNeededError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name;
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridKeystoreDatabaseUpgradeNeededError.prototype = new UsergridError();
    global["UsergridHTTPResponseError"] = UsergridHTTPResponseError;
    global["UsergridInvalidHTTPMethodError"] = UsergridInvalidHTTPMethodError;
    global["UsergridInvalidURIError"] = UsergridInvalidURIError;
    global["UsergridKeystoreDatabaseUpgradeNeededError"] = UsergridKeystoreDatabaseUpgradeNeededError;
    global[name] = UsergridError;
    if (short !== undefined) {
        //noinspection JSUnusedAssignment
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