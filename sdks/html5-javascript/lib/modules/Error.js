/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0

 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
//noinspection ThisExpressionReferencesGlobalObjectJS

/**
 * Created by ryan bridges on 2014-02-05.
 */
(function(global) {
    //noinspection JSUnusedAssignment
    var name = 'UsergridError',
        short,
        _name = global[name],
        _short = (short && short !== undefined) ? global[short] : undefined;

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
        this.name = name || 'invalid_http_method';
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridInvalidHTTPMethodError.prototype = new UsergridError();

    function UsergridInvalidURIError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name || 'invalid_uri';
        this.timestamp = timestamp || Date.now();
        this.duration = duration || 0;
        this.exception = exception;
    }
    UsergridInvalidURIError.prototype = new UsergridError();

    function UsergridInvalidArgumentError(message, name, timestamp, duration, exception) {
        this.message = message;
        this.name = name || 'invalid_argument';
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
}(this));
