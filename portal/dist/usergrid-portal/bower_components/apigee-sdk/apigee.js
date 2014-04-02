/*! apigee-javascript-sdk@2.0.5 2014-01-02 */
/*
*  This module is a collection of classes designed to make working with
*  the Apigee App Services API as easy as possible.
*  Learn more at http://apigee.com/docs/usergrid
*
*   Copyright 2012 Apigee Corporation
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
*  @author rod simpson (rod@apigee.com)
*  @author matt dobson (matt@apigee.com)
*  @author ryan bridges (rbridges@apigee.com)
*/
(function() {
    var name = "Usergrid", global = global || this, overwrittenName = global[name];
    //authentication type constants for Node.js
    var AUTH_CLIENT_ID = "CLIENT_ID";
    var AUTH_APP_USER = "APP_USER";
    var AUTH_NONE = "NONE";
    if ("undefined" === typeof console) {
        global.console = {
            log: function() {},
            warn: function() {},
            error: function() {},
            dir: function() {}
        };
    }
    function Usergrid() {}
    Usergrid.Client = function(options) {
        //usergrid enpoint
        this.URI = options.URI || "https://api.usergrid.com";
        //Find your Orgname and Appname in the Admin portal (http://apigee.com/usergrid)
        if (options.orgName) {
            this.set("orgName", options.orgName);
        }
        if (options.appName) {
            this.set("appName", options.appName);
        }
        if (options.appVersion) {
            this.set("appVersion", options.appVersion);
        }
        //authentication data
        this.authType = options.authType || AUTH_NONE;
        this.clientId = options.clientId;
        this.clientSecret = options.clientSecret;
        this.setToken(options.token || null);
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
    *  Main function for making requests to the API using node.  
    *  Use Usergrid.Client.prototype.request for cross-platform compatibility.

    *
    *  options object:
    *  `method` - http method (GET, POST, PUT, or DELETE), defaults to GET
    *  `qs` - object containing querystring values to be appended to the uri
    *  `body` - object containing entity body for POST and PUT requests
    *  `endpoint` - API endpoint, for example 'users/fred'
    *  `mQuery` - boolean, set to true if running management query, defaults to false
    *
    *  @method _request_node
    *  @public
    *  @params {object} options
    *  @param {function} callback
    *  @return {callback} callback(err, data)
    */
    Usergrid.Client.prototype._request_node = function(options, callback) {
        global.request = global.request || require("request");
        var request = global.request;
        var self = this;
        var method = options.method || "GET";
        var endpoint = options.endpoint;
        var body = options.body || {};
        var qs = options.qs || {};
        var mQuery = options.mQuery || false;
        //is this a query to the management endpoint?
        var orgName = this.get("orgName");
        var appName = this.get("appName");
        if (!mQuery && !orgName && !appName) {
            if (typeof this.logoutCallback === "function") {
                return this.logoutCallback(true, "no_org_or_app_name_specified");
            }
        }
        if (mQuery) {
            uri = this.URI + "/" + endpoint;
        } else {
            uri = this.URI + "/" + orgName + "/" + appName + "/" + endpoint;
        }
        if (this.authType === AUTH_CLIENT_ID) {
            qs.client_id = this.clientId;
            qs.client_secret = this.clientSecret;
        } else if (this.authType === AUTH_APP_USER) {
            qs.access_token = self.getToken();
        }
        if (this.logging) {
            console.log("calling: " + method + " " + uri);
        }
        this._start = new Date().getTime();
        var callOptions = {
            method: method,
            uri: uri,
            json: body,
            qs: qs
        };
        request(callOptions, function(err, r, data) {
            if (self.buildCurl) {
                options.uri = r.request.uri.href;
                self.buildCurlCall(options);
            }
            self._end = new Date().getTime();
            if (r.statusCode === 200) {
                if (self.logging) {
                    console.log("success (time: " + self.calcTimeDiff() + "): " + method + " " + uri);
                }
                callback(err, data);
            } else {
                err = true;
                if (r.error === "auth_expired_session_token" || r.error === "auth_missing_credentials" || r.error == "auth_unverified_oath" || r.error === "expired_token" || r.error === "unauthorized" || r.error === "auth_invalid") {
                    //this error type means the user is not authorized. If a logout function is defined, call it
                    var error = r.body.error;
                    var errorDesc = r.body.error_description;
                    if (self.logging) {
                        console.log("Error (" + r.statusCode + ")(" + error + "): " + errorDesc);
                    }
                    //if the user has specified a logout callback:
                    if (typeof self.logoutCallback === "function") {
                        self.logoutCallback(err, data);
                    } else if (typeof callback === "function") {
                        callback(err, data);
                    }
                } else {
                    var error = r.body.error;
                    var errorDesc = r.body.error_description;
                    if (self.logging) {
                        console.log("Error (" + r.statusCode + ")(" + error + "): " + errorDesc);
                    }
                    if (typeof callback === "function") {
                        callback(err, data);
                    }
                }
            }
        });
    };
    /*
    *  Main function for making requests to the API using a browser.  
    *  Use Usergrid.Client.prototype.request for cross-platform compatibility.

    *
    *  options object:
    *  `method` - http method (GET, POST, PUT, or DELETE), defaults to GET
    *  `qs` - object containing querystring values to be appended to the uri
    *  `body` - object containing entity body for POST and PUT requests
    *  `endpoint` - API endpoint, for example 'users/fred'
    *  `mQuery` - boolean, set to true if running management query, defaults to false
    *
    *  @method _request_node
    *  @public
    *  @params {object} options
    *  @param {function} callback
    *  @return {callback} callback(err, data)
    */
    Usergrid.Client.prototype._request_xhr = function(options, callback) {
        var self = this;
        var method = options.method || "GET";
        var endpoint = options.endpoint;
        var body = options.body || {};
        var qs = options.qs || {};
        var mQuery = options.mQuery || false;
        //is this a query to the management endpoint?
        var orgName = this.get("orgName");
        var appName = this.get("appName");
        if (!mQuery && !orgName && !appName) {
            if (typeof this.logoutCallback === "function") {
                return this.logoutCallback(true, "no_org_or_app_name_specified");
            }
        }
        var uri;
        if (mQuery) {
            uri = this.URI + "/" + endpoint;
        } else {
            uri = this.URI + "/" + orgName + "/" + appName + "/" + endpoint;
        }
        if (self.getToken()) {
            qs.access_token = self.getToken();
        }
        //append params to the path
        var encoded_params = encodeParams(qs);
        if (encoded_params) {
            uri += "?" + encoded_params;
        }
        //stringify the body object
        body = JSON.stringify(body);
        //so far so good, so run the query
        var xhr = new XMLHttpRequest();
        xhr.open(method, uri, true);
        //add content type = json if there is a json payload
        if (body) {
            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.setRequestHeader("Accept", "application/json");
        }
        // Handle response.
        xhr.onerror = function(response) {
            self._end = new Date().getTime();
            if (self.logging) {
                console.log("success (time: " + self.calcTimeDiff() + "): " + method + " " + uri);
            }
            if (self.logging) {
                console.log("Error: API call failed at the network level.");
            }
            //network error
            clearTimeout(timeout);
            var err = true;
            if (typeof callback === "function") {
                callback(err, response);
            }
        };
        xhr.onload = function(response) {
            //call timing, get time, then log the call
            self._end = new Date().getTime();
            if (self.logging) {
                console.log("success (time: " + self.calcTimeDiff() + "): " + method + " " + uri);
            }
            //call completed
            clearTimeout(timeout);
            //decode the response
            try {
                response = JSON.parse(xhr.responseText);
            } catch (e) {
                response = {
                    error: "unhandled_error",
                    error_description: xhr.responseText
                };
                xhr.status = xhr.status === 200 ? 400 : xhr.status;
                console.error(e);
            }
            if (xhr.status != 200) {
                //there was an api error
                var error = response.error;
                var error_description = response.error_description;
                if (self.logging) {
                    console.log("Error (" + xhr.status + ")(" + error + "): " + error_description);
                }
                if (error == "auth_expired_session_token" || error == "auth_missing_credentials" || error == "auth_unverified_oath" || error == "expired_token" || error == "unauthorized" || error == "auth_invalid") {
                    //these errors mean the user is not authorized for whatever reason. If a logout function is defined, call it
                    //if the user has specified a logout callback:
                    if (typeof self.logoutCallback === "function") {
                        return self.logoutCallback(true, response);
                    }
                }
                if (typeof callback === "function") {
                    callback(true, response);
                }
            } else {
                if (typeof callback === "function") {
                    callback(false, response);
                }
            }
        };
        var timeout = setTimeout(function() {
            xhr.abort();
            if (self._callTimeoutCallback === "function") {
                self._callTimeoutCallback("API CALL TIMEOUT");
            } else {
                self.callback("API CALL TIMEOUT");
            }
        }, self._callTimeout);
        //set for 30 seconds
        if (this.logging) {
            console.log("calling: " + method + " " + uri);
        }
        if (this.buildCurl) {
            var curlOptions = {
                uri: uri,
                body: body,
                method: method
            };
            this.buildCurlCall(curlOptions);
        }
        this._start = new Date().getTime();
        xhr.send(body);
    };
    /*
    *  Main function for making requests to the API using node.  You may call this method directly
    *
    *  options object:
    *  `method` - http method (GET, POST, PUT, or DELETE), defaults to GET
    *  `qs` - object containing querystring values to be appended to the uri
    *  `body` - object containing entity body for POST and PUT requests
    *  `endpoint` - API endpoint, for example 'users/fred'
    *  `mQuery` - boolean, set to true if running management query, defaults to false
    *
    *  @method _request_node
    *  @public
    *  @params {object} options
    *  @param {function} callback
    *  @return {callback} callback(err, data)
    */
    Usergrid.Client.prototype.request = function(options, callback) {
        if ("undefined" !== typeof window) {
            Usergrid.Client.prototype._request_xhr.apply(this, arguments);
        } else {
            Usergrid.Client.prototype._request_node.apply(this, arguments);
        }
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
            var okToSave = err && "service_resource_not_found" === data.error || "no_name_specified" === data.error || "null_pointer" === data.error || !err && getOnExist;
            if (okToSave) {
                group.save(function(err, data) {
                    if (typeof callback === "function") {
                        callback(err, group);
                    }
                });
            } else {
                if (typeof callback === "function") {
                    callback(err, group);
                }
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
        /*
    options = {
      client:this,
      data:options
    }
    var entity = new Usergrid.Entity(options);
    entity.save(function(err, data) {
      if (typeof(callback) === 'function') {
      callback(err, entity);
      }
    });
    */
        var getOnExist = options.getOnExist || false;
        //if true, will return entity if one already exists
        options = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(options);
        entity.fetch(function(err, data) {
            //if the fetch doesn't find what we are looking for, or there is no error, do a save
            var okToSave = err && "service_resource_not_found" === data.error || "no_name_specified" === data.error || "null_pointer" === data.error || !err && getOnExist;
            if (okToSave) {
                entity.set(options.data);
                //add the data again just in case
                entity.save(function(err, data) {
                    if (typeof callback === "function") {
                        callback(err, entity, data);
                    }
                });
            } else {
                if (typeof callback === "function") {
                    callback(err, entity, data);
                }
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
        options = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(options);
        entity.fetch(function(err, data) {
            if (typeof callback === "function") {
                callback(err, entity, data);
            }
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
        options = {
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
            if (typeof callback === "function") {
                callback(err, collection, data);
            }
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
            if (typeof callback === "function") {
                if (err) {
                    callback(err);
                } else {
                    callback(err, data, data.entities);
                }
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
            if (typeof callback === "function") {
                callback(err, entity);
            }
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
        if (this[key]) {
            return this[key];
        } else if (typeof Storage !== "undefined") {
            return localStorage.getItem(keyStore);
        }
        return null;
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
        this.request(options, function(err, data) {
            var user = {};
            if (err && self.logging) {
                console.log("error trying to log user in");
            } else {
                options = {
                    client: self,
                    data: data.user
                };
                user = new Usergrid.Entity(options);
                self.setToken(data.access_token);
            }
            if (typeof callback === "function") {
                callback(err, data, user);
            }
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
                self.setToken(response.access_token);
            }
            if (typeof callback === "function") {
                callback(err);
            }
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
                options = {
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
            if (typeof callback === "function") {
                callback(err, data, user, organizations, applications);
            }
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
            if (typeof callback === "function") {
                callback(err, data, user);
            }
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
                    if (typeof callback === "function") {
                        callback(err, data, null);
                    }
                } else {
                    var options = {
                        client: self,
                        data: data.entities[0]
                    };
                    var user = new Usergrid.Entity(options);
                    if (typeof callback === "function") {
                        callback(err, data, user);
                    }
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
        if (this.getToken() && this.getToken() != "null") {
            return true;
        }
        return false;
    };
    /*
    *  A public method to log out an app user - clears all user fields from client
    *
    *  @method logout
    *  @public
    *  @return none
    */
    Usergrid.Client.prototype.logout = function() {
        this.setToken(null);
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
        var curl = "curl";
        var method = (options.method || "GET").toUpperCase();
        var body = options.body || {};
        var uri = options.uri;
        //curl - add the method to the command (no need to add anything for GET)
        if (method === "POST") {
            curl += " -X POST";
        } else if (method === "PUT") {
            curl += " -X PUT";
        } else if (method === "DELETE") {
            curl += " -X DELETE";
        } else {
            curl += " -X GET";
        }
        //curl - append the path
        curl += " " + uri;
        //curl - add the body
        if ("undefined" !== typeof window) {
            body = JSON.stringify(body);
        }
        //only in node module
        if (body !== '"{}"' && method !== "GET" && method !== "DELETE") {
            //curl - add in the json obj
            curl += " -d '" + body + "'";
        }
        //log the curl command to the console
        console.log(curl);
        return curl;
    };
    Usergrid.Client.prototype.getDisplayImage = function(email, picture, size) {
        try {
            if (picture) {
                return picture;
            }
            var size = size || 50;
            if (email.length) {
                return "https://secure.gravatar.com/avatar/" + MD5(email) + "?s=" + size + encodeURI("&d=https://apigee.com/usergrid/images/user_profile.png");
            } else {
                return "https://apigee.com/usergrid/images/user_profile.png";
            }
        } catch (e) {
            return "https://apigee.com/usergrid/images/user_profile.png";
        }
    };
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
    Usergrid.Entity.prototype.get = function(field) {
        if (field) {
            return this._data[field];
        } else {
            return this._data;
        }
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
    /*
    *  Saves the entity back to the database
    *
    *  @method save
    *  @public
    *  @param {function} callback
    *  @return {callback} callback(err, data)
    */
    Usergrid.Entity.prototype.save = function(callback) {
        var type = this.get("type");
        var method = "POST";
        if (isUUID(this.get("uuid"))) {
            method = "PUT";
            type += "/" + this.get("uuid");
        }
        //update the entity
        var self = this;
        var data = {};
        var entityData = this.get();
        //remove system specific properties
        for (var item in entityData) {
            if (item === "metadata" || item === "created" || item === "modified" || item === "type" || item === "activated" || item === "uuid") {
                continue;
            }
            data[item] = entityData[item];
        }
        var options = {
            method: method,
            endpoint: type,
            body: data
        };
        //save the entity first
        this._client.request(options, function(err, retdata) {
            if (err && self._client.logging) {
                console.log("could not save entity");
                if (typeof callback === "function") {
                    return callback(err, retdata, self);
                }
            } else {
                if (retdata.entities) {
                    if (retdata.entities.length) {
                        var entity = retdata.entities[0];
                        self.set(entity);
                        var path = retdata.path;
                        //for connections, API returns type
                        while (path.substring(0, 1) === "/") {
                            path = path.substring(1);
                        }
                        self.set("type", path);
                    }
                }
                //if this is a user, update the password if it has been specified;
                var needPasswordChange = (self.get("type") === "user" || self.get("type") === "users") && entityData.oldpassword && entityData.newpassword;
                if (needPasswordChange) {
                    //Note: we have a ticket in to change PUT calls to /users to accept the password change
                    //      once that is done, we will remove this call and merge it all into one
                    var pwdata = {};
                    pwdata.oldpassword = entityData.oldpassword;
                    pwdata.newpassword = entityData.newpassword;
                    var options = {
                        method: "PUT",
                        endpoint: type + "/password",
                        body: pwdata
                    };
                    self._client.request(options, function(err, data) {
                        if (err && self._client.logging) {
                            console.log("could not update user");
                        }
                        //remove old and new password fields so they don't end up as part of the entity object
                        self.set("oldpassword", null);
                        self.set("newpassword", null);
                        if (typeof callback === "function") {
                            callback(err, data, self);
                        }
                    });
                } else if (typeof callback === "function") {
                    callback(err, retdata, self);
                }
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
        var type = this.get("type");
        var self = this;
        //Check for an entity type, then if a uuid is available, use that, otherwise, use the name
        try {
            if (type === undefined) {
                throw "cannot fetch entity, no entity type specified";
            } else if (this.get("uuid")) {
                type += "/" + this.get("uuid");
            } else if (type === "users" && this.get("username")) {
                type += "/" + this.get("username");
            } else if (this.get("name")) {
                type += "/" + encodeURIComponent(this.get("name"));
            } else if (typeof callback === "function") {
                throw "no_name_specified";
            }
        } catch (e) {
            if (self._client.logging) {
                console.log(e);
            }
            return callback(true, {
                error: e
            }, self);
        }
        var options = {
            method: "GET",
            endpoint: type
        };
        this._client.request(options, function(err, data) {
            if (err && self._client.logging) {
                console.log("could not get entity");
            } else {
                if (data.user) {
                    self.set(data.user);
                    self._json = JSON.stringify(data.user, null, 2);
                } else if (data.entities) {
                    if (data.entities.length) {
                        var entity = data.entities[0];
                        self.set(entity);
                    }
                }
            }
            if (typeof callback === "function") {
                callback(err, data, self);
            }
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
        var type = this.get("type");
        if (isUUID(this.get("uuid"))) {
            type += "/" + this.get("uuid");
        } else {
            if (typeof callback === "function") {
                var error = "Error trying to delete object - no uuid specified.";
                if (self._client.logging) {
                    console.log(error);
                }
                callback(true, error);
            }
        }
        var options = {
            method: "DELETE",
            endpoint: type
        };
        this._client.request(options, function(err, data) {
            if (err && self._client.logging) {
                console.log("entity could not be deleted");
            } else {
                self.set(null);
            }
            if (typeof callback === "function") {
                callback(err, data);
            }
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
                callback(true, error);
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
                callback(true, error);
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
            if (typeof callback === "function") {
                callback(err, data);
            }
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
            if (type === "users") {
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
                callback(true, error);
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
            var length = data.entities.length;
            for (var i = 0; i < length; i++) {
                if (data.entities[i].type === "user") {
                    self[connection][data.entities[i].username] = data.entities[i];
                } else {
                    self[connection][data.entities[i].name] = data.entities[i];
                }
            }
            if (typeof callback === "function") {
                callback(err, data, data.entities);
            }
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
            if (typeof callback === "function") {
                callback(err, data, data.entities);
            }
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
            if (typeof callback === "function") {
                callback(err, data, data.entities);
            }
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
            if (typeof callback === "function") {
                callback(err, data, data.entities);
            }
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
            if (typeof callback === "function") {
                callback(err, data, data.entities);
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
            if (typeof callback === "function") {
                callback(err, data, data.entities);
            }
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
            if (typeof callback === "function") {
                callback(err, data, data.entities);
            }
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
                callback(true, error);
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
                callback(true, error);
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
            if (typeof callback === "function") {
                callback(err, data);
            }
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
                callback(err, collection);
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
            if (typeof callback === "function") {
                callback(err, data);
            }
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
            if (typeof callback === "function") {
                callback(err, entity);
            }
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
                if (typeof callback === "function") {
                    callback(err, data);
                }
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
                return listItem;
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
        //Usergrid had < while others had <=
        var hasNextElement = this._iterator >= 0 && this._iterator < this._list.length;
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
        } else {
            callback(true);
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
        } else {
            callback(true);
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
                if (typeof callback === "function") {
                    callback(err, data);
                }
            } else {
                if (data.entities) {
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
                        if (typeof callback === "function") {
                            callback(err, data, self._list);
                        }
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
        if (typeof callback === "function") {
            callback(null, this._list);
        }
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
        options = {
            method: "POST",
            endpoint: "groups/" + this._path + "/users/" + options.user.get("username")
        };
        this._client.request(options, function(error, data) {
            if (error) {
                if (typeof callback === "function") {
                    callback(error, data, data.entities);
                }
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
        options = {
            method: "DELETE",
            endpoint: "groups/" + this._path + "/users/" + options.user.get("username")
        };
        this._client.request(options, function(error, data) {
            if (error) {
                if (typeof callback === "function") {
                    callback(error, data);
                }
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
            if (typeof callback === "function") {
                callback(err, data, data.entities);
            }
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
            if (typeof callback === "function") {
                callback(err, entity);
            }
        });
    };
    /*
    * Tests if the string is a uuid
    *
    * @public
    * @method isUUID
    * @param {string} uuid The string to test
    * @returns {Boolean} true if string is uuid
    */
    function isUUID(uuid) {
        var uuidValueRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
        if (!uuid) {
            return false;
        }
        return uuidValueRegex.test(uuid);
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
        var tail = [];
        var item = [];
        var i;
        if (params instanceof Array) {
            for (i in params) {
                item = params[i];
                if (item instanceof Array && item.length > 1) {
                    tail.push(item[0] + "=" + encodeURIComponent(item[1]));
                }
            }
        } else {
            for (var key in params) {
                if (params.hasOwnProperty(key)) {
                    var value = params[key];
                    if (value instanceof Array) {
                        for (i in value) {
                            item = value[i];
                            tail.push(key + "=" + encodeURIComponent(item));
                        }
                    } else {
                        tail.push(key + "=" + encodeURIComponent(value));
                    }
                }
            }
        }
        return tail.join("&");
    }
    Usergrid.SDK_VERSION = "0.10.07";
    Usergrid.NODE_MODULE_VERSION = Usergrid.SDK_VERSION;
    global[name] = {
        client: Usergrid.Client,
        entity: Usergrid.Entity,
        collection: Usergrid.Collection,
        group: Usergrid.Group,
        AUTH_CLIENT_ID: AUTH_CLIENT_ID,
        AUTH_APP_USER: AUTH_APP_USER,
        AUTH_NONE: AUTH_NONE
    };
    global[name].noConflict = function() {
        if (overwrittenName) {
            global[name] = overwrittenName;
        }
        return Usergrid;
    };
})();

(function() {
    var name = "Usergrid", global = this, overwrittenName = global[name];
    var Usergrid = Usergrid || global.Usergrid;
    if (!Usergrid) {
        throw "Usergrid module is required for the monitoring module.";
    }
    /*
   * Logs a user defined verbose message.
   *
   * @method logDebug
   * @public
   * @param {object} options
   *
   */
    Usergrid.client.prototype.logVerbose = function(options) {
        this.monitor.logVerbose(options);
    };
    /*
   * Logs a user defined debug message.
   *
   * @method logDebug
   * @public
   * @param {object} options
   *
   */
    Usergrid.client.prototype.logDebug = function(options) {
        this.monitor.logDebug(options);
    };
    /*
   * Logs a user defined informational message.
   *
   * @method logInfo
   * @public
   * @param {object} options
   *
   */
    Usergrid.client.prototype.logInfo = function(options) {
        this.monitor.logInfo(options);
    };
    /*
   * Logs a user defined warning message.
   *
   * @method logWarn
   * @public
   * @param {object} options
   *
   */
    Usergrid.client.prototype.logWarn = function(options) {
        this.monitor.logWarn(options);
    };
    /*
   * Logs a user defined error message.
   *
   * @method logError
   * @public
   * @param {object} options
   *
   */
    Usergrid.client.prototype.logError = function(options) {
        this.monitor.logError(options);
    };
    /*
   * Logs a user defined assert message.
   *
   * @method logAssert
   * @public
   * @param {object} options
   *
   */
    Usergrid.client.prototype.logAssert = function(options) {
        this.monitor.logAssert(options);
    };
    global[name] = {
        client: Usergrid.client,
        entity: Usergrid.entity,
        collection: Usergrid.collection,
        group: Usergrid.group,
        AUTH_CLIENT_ID: Usergrid.AUTH_CLIENT_ID,
        AUTH_APP_USER: Usergrid.AUTH_APP_USER,
        AUTH_NONE: Usergrid.AUTH_NONE
    };
    global[name].noConflict = function() {
        if (overwrittenName) {
            global[name] = overwrittenName;
        }
        return Usergrid;
    };
    return global[name];
})();

(function() {
    var name = "Apigee", global = this, overwrittenName = global[name];
    var Usergrid = Usergrid || global.Usergrid;
    if (!Usergrid) {
        throw "Usergrid module is required for the monitoring module.";
    }
    var VERBS = {
        get: "GET",
        post: "POST",
        put: "PUT",
        del: "DELETE",
        head: "HEAD"
    };
    var MONITORING_SDKVERSION = "0.0.1";
    var LOGLEVELS = {
        verbose: "V",
        debug: "D",
        info: "I",
        warn: "W",
        error: "E",
        assert: "A"
    };
    var LOGLEVELNUMBERS = {
        verbose: 2,
        debug: 3,
        info: 4,
        warn: 5,
        error: 6,
        assert: 7
    };
    var UNKNOWN = "UNKNOWN";
    var SDKTYPE = "JavaScript";
    //Work around hack because onerror is always called in the window context so we can't store crashes internally
    //This isn't too bad because we are encapsulated.
    var logs = [];
    var metrics = [];
    var Apigee = Usergrid;
    Apigee.prototype = Usergrid.prototype;
    //Apigee.constructor=Apigee;
    //function Apigee() {};
    Apigee.Client = function(options) {
        //Init app monitoring.
        this.monitoringEnabled = options.monitoringEnabled || true;
        if (this.monitoringEnabled) {
            try {
                this.monitor = new Apigee.MonitoringClient(options);
            } catch (e) {
                console.log(e);
            }
        }
        Usergrid.client.call(this, options);
    };
    Apigee.Client.prototype = Usergrid.client.prototype;
    //Apigee.Client.constructor=Apigee.Client;
    //BEGIN APIGEE MONITORING SDK
    //Constructor for Apigee Monitoring SDK
    Apigee.MonitoringClient = function(options) {
        //Needed for the setInterval call for syncing. Have to pass in a ref to ourselves. It blows scope away.
        var self = this;
        this.orgName = options.orgName;
        this.appName = options.appName;
        this.syncOnClose = options.syncOnClose || false;
        //Put this in here because I don't want sync issues with testing.
        this.testMode = options.testMode || false;
        //You best know what you're doing if you're setting this for Apigee monitoring!
        this.URI = typeof options.URI === "undefined" ? "https://api.usergrid.com" : options.URI;
        this.syncDate = timeStamp();
        //Can do a manual config override specifiying raw json as your config. I use this for testing.
        //May be useful down the road. Needs to conform to current config.
        if (typeof options.config !== "undefined") {
            this.configuration = options.config;
            if (this.configuration.deviceLevelOverrideEnabled === true) {
                this.deviceConfig = this.configuration.deviceLevelAppConfig;
            } else if (this.abtestingOverrideEnabled === true) {
                this.deviceConfig = this.configuration.abtestingAppConfig;
            } else {
                this.deviceConfig = this.configuration.defaultAppConfig;
            }
        } else {
            this.configuration = null;
            this.downloadConfig();
        }
        //Don't do anything if configuration wasn't loaded.
        if (this.configuration !== null && this.configuration !== "undefined") {
            //Ensure that we want to sample data from this device.
            var sampleSeed = 0;
            if (this.deviceConfig.samplingRate < 100) {
                sampleSeed = Math.floor(Math.random() * 101);
            }
            //If we're not in the sampling window don't setup data collection at all
            if (sampleSeed < this.deviceConfig.samplingRate) {
                this.appId = this.configuration.instaOpsApplicationId;
                this.appConfigType = this.deviceConfig.appConfigType;
                //Let's monkeypatch logging calls to intercept and send to server.
                if (this.deviceConfig.enableLogMonitoring) {
                    this.patchLoggingCalls();
                }
                var syncIntervalMillis = 3e3;
                if (typeof this.deviceConfig.agentUploadIntervalInSeconds !== "undefined") {
                    syncIntervalMillis = this.deviceConfig.agentUploadIntervalInSeconds * 1e3;
                }
                //Needed for the setInterval call for syncing. Have to pass in a ref to ourselves. It blows scope away.
                if (!this.syncOnClose) {
                    //Old server syncing logic
                    setInterval(function() {
                        self.prepareSync();
                    }, syncIntervalMillis);
                } else {
                    if (isPhoneGap()) {
                        window.addEventListener("pause", function() {
                            self.prepareSync();
                        }, false);
                    } else if (isTrigger()) {
                        forge.event.appPaused.addListener(function(data) {}, function(error) {
                            console.log("Error syncing data.");
                            console.log(error);
                        });
                    } else if (isTitanium()) {} else {
                        window.addEventListener("beforeunload", function(e) {
                            self.prepareSync();
                        });
                    }
                }
                //Setting up the catching of errors and network calls
                if (this.deviceConfig.networkMonitoringEnabled) {
                    this.patchNetworkCalls(XMLHttpRequest);
                }
                window.onerror = Apigee.MonitoringClient.catchCrashReport;
                this.startSession();
                this.sync({});
            }
        } else {
            console.log("Error: Apigee APM configuration unavailable.");
        }
    };
    Apigee.MonitoringClient.prototype.applyMonkeyPatches = function() {
        var self = this;
        //Let's monkeypatch logging calls to intercept and send to server.
        if (self.deviceConfig.enableLogMonitoring) {
            self.patchLoggingCalls();
        }
        //Setting up the catching of errors and network calls
        if (self.deviceConfig.networkMonitoringEnabled) {
            self.patchNetworkCalls(XMLHttpRequest);
        }
    };
    /**
   * Function for retrieving the current Apigee Monitoring configuration.
   *
   * @method downloadConfig
   * @public
   * @params {function} callback
   * NOTE: Passing in a callback makes this call async. Wires it all up for you.
   *
   */
    Apigee.MonitoringClient.prototype.getConfig = function(options, callback) {
        if (typeof options.config !== "undefined") {
            this.configuration = options.config;
            if (this.configuration.deviceLevelOverrideEnabled === true) {
                this.deviceConfig = this.configuration.deviceLevelAppConfig;
            } else if (this.abtestingOverrideEnabled === true) {
                this.deviceConfig = this.configuration.abtestingAppConfig;
            } else {
                this.deviceConfig = this.configuration.defaultAppConfig;
            }
            callback(this.deviceConfig);
        } else {
            this.configuration = null;
            this.downloadConfig(callback);
        }
    };
    /**
   * Function for downloading the current Apigee Monitoring configuration.
   *
   * @method downloadConfig
   * @public
   * @params {function} callback
   * NOTE: Passing in a callback makes this call async. Wires it all up for you.
   *
   */
    Apigee.MonitoringClient.prototype.downloadConfig = function(callback) {
        var configRequest = new XMLHttpRequest();
        var path = this.URI + "/" + this.orgName + "/" + this.appName + "/apm/apigeeMobileConfig";
        //If we have a function lets load the config async else do it sync.
        if (typeof callback === "function") {
            configRequest.open(VERBS.get, path, true);
        } else {
            configRequest.open(VERBS.get, path, false);
        }
        var self = this;
        configRequest.setRequestHeader("Accept", "application/json");
        configRequest.setRequestHeader("Content-Type", "application/json");
        configRequest.onreadystatechange = onReadyStateChange;
        configRequest.send();
        //A little async magic. Let's return the AJAX issue from downloading the configs.
        //Or we can return the parsed out config.
        function onReadyStateChange() {
            if (configRequest.readyState === 4) {
                if (typeof callback === "function") {
                    if (configRequest.status === 200) {
                        callback(null, JSON.parse(configRequest.responseText));
                    } else {
                        callback(configRequest.statusText);
                    }
                } else {
                    if (configRequest.status === 200) {
                        var config = JSON.parse(configRequest.responseText);
                        self.configuration = config;
                        if (config.deviceLevelOverrideEnabled === true) {
                            self.deviceConfig = config.deviceLevelAppConfig;
                        } else if (self.abtestingOverrideEnabled === true) {
                            self.deviceConfig = config.abtestingAppConfig;
                        } else {
                            self.deviceConfig = config.defaultAppConfig;
                        }
                    }
                }
            }
        }
    };
    /**
   * Function for syncing data back to the server. Currently called in the Apigee.MonitoringClient constructor using setInterval.
   *
   * @method sync
   * @public
   * @params {object} syncObject
   *
   */
    Apigee.MonitoringClient.prototype.sync = function(syncObject) {
        //Sterilize the sync data
        var syncData = {};
        syncData.logs = syncObject.logs;
        syncData.metrics = syncObject.metrics;
        syncData.sessionMetrics = this.sessionMetrics;
        syncData.orgName = this.orgName;
        syncData.appName = this.appName;
        syncData.fullAppName = this.orgName + "_" + this.appName;
        syncData.instaOpsApplicationId = this.configuration.instaOpsApplicationId;
        syncData.timeStamp = timeStamp();
        //Send it to the apmMetrics endpoint.
        var syncRequest = new XMLHttpRequest();
        var path = this.URI + "/" + this.orgName + "/" + this.appName + "/apm/apmMetrics";
        syncRequest.open(VERBS.post, path, false);
        syncRequest.setRequestHeader("Accept", "application/json");
        syncRequest.setRequestHeader("Content-Type", "application/json");
        syncRequest.send(JSON.stringify(syncData));
        //Only wipe data if the sync was good. Hold onto it if it was bad.
        if (syncRequest.status === 200) {
            logs = [];
            metrics = [];
            var response = syncRequest.responseText;
        } else {
            //Not much we can do if there was an error syncing data.
            //Log it to console accordingly.
            console.log("Error syncing");
            console.log(syncRequest.responseText);
        }
    };
    /**
   * Function that is called during the window.onerror handler. Grabs all parameters sent by that function.
   *
   * @public
   * @param {string} crashEvent
   * @param {string} url
   * @param {string} line
   *
   */
    Apigee.MonitoringClient.catchCrashReport = function(crashEvent, url, line) {
        logCrash({
            tag: "CRASH",
            logMessage: "Error:" + crashEvent + " for url:" + url + " on line:" + line
        });
    };
    Apigee.MonitoringClient.prototype.startLocationCapture = function() {
        var self = this;
        if (self.deviceConfig.locationCaptureEnabled && typeof navigator.geolocation !== "undefined") {
            var geoSuccessCallback = function(position) {
                self.sessionMetrics.latitude = position.coords.latitude;
                self.sessionMetrics.longitude = position.coords.longitude;
            };
            var geoErrorCallback = function() {
                console.log("Location access is not available.");
            };
            navigator.geolocation.getCurrentPosition(geoSuccessCallback, geoErrorCallback);
        }
    };
    Apigee.MonitoringClient.prototype.detectAppPlatform = function(sessionSummary) {
        var self = this;
        var callbackHandler_Titanium = function(e) {
            //Framework is appcelerator
            sessionSummary.devicePlatform = e.name;
            sessionSummary.deviceOSVersion = e.osname;
            //Get the device id if we want it. If we dont, but we want it obfuscated generate
            //a one off id and attach it to localStorage.
            if (self.deviceConfig.deviceIdCaptureEnabled) {
                if (self.deviceConfig.obfuscateDeviceId) {
                    sessionSummary.deviceId = generateDeviceId();
                } else {
                    sessionSummary.deviceId = e.uuid;
                }
            } else {
                if (this.deviceConfig.obfuscateDeviceId) {
                    sessionSummary.deviceId = generateDeviceId();
                } else {
                    sessionSummary.deviceId = UNKNOWN;
                }
            }
            sessionSummary.deviceModel = e.model;
            sessionSummary.networkType = e.networkType;
        };
        var callbackHandler_PhoneGap = function(e) {
            if ("device" in window) {
                sessionSummary.devicePlatform = window.device.platform;
                sessionSummary.deviceOSVersion = window.device.version;
                sessionSummary.deviceModel = window.device.name;
            } else if (window.cordova) {
                sessionSummary.devicePlatform = window.cordova.platformId;
                sessionSummary.deviceOSVersion = UNKNOWN;
                sessionSummary.deviceModel = UNKNOWN;
            }
            if ("connection" in navigator) {
                sessionSummary.networkType = navigator.connection.type || UNKNOWN;
            }
            //Get the device id if we want it. If we dont, but we want it obfuscated generate
            //a one off id and attach it to localStorage.
            if (self.deviceConfig.deviceIdCaptureEnabled) {
                if (self.deviceConfig.obfuscateDeviceId) {
                    sessionSummary.deviceId = generateDeviceId();
                } else {
                    sessionSummary.deviceId = window.device.uuid;
                }
            } else {
                if (this.deviceConfig.obfuscateDeviceId) {
                    sessionSummary.deviceId = generateDeviceId();
                } else {
                    sessionSummary.deviceId = UNKNOWN;
                }
            }
            return sessionSummary;
        };
        var callbackHandler_Trigger = function(sessionSummary) {
            var os = UNKNOWN;
            if (forge.is.ios()) {
                os = "iOS";
            } else if (forge.is.android()) {
                os = "Android";
            }
            sessionSummary.devicePlatform = UNKNOWN;
            sessionSummary.deviceOSVersion = os;
            //Get the device id if we want it. Trigger.io doesn't expose device id APIs
            if (self.deviceConfig.deviceIdCaptureEnabled) {
                sessionSummary.deviceId = generateDeviceId();
            } else {
                sessionSummary.deviceId = UNKNOWN;
            }
            sessionSummary.deviceModel = UNKNOWN;
            sessionSummary.networkType = forge.is.connection.wifi() ? "WIFI" : UNKNOWN;
            return sessionSummary;
        };
        //We're checking if it's a phonegap app.
        //If so let's use APIs exposed by phonegap to collect device info.
        //If not let's fallback onto stuff we should collect ourselves.
        if (isPhoneGap()) {
            //framework is phonegap.
            sessionSummary = callbackHandler_PhoneGap(sessionSummary);
        } else if (isTrigger()) {
            //Framework is trigger
            sessionSummary = callbackHandler_Trigger(sessionSummary);
        } else if (isTitanium()) {
            Ti.App.addEventListener("analytics:platformMetrics", callbackHandler_Titanium);
        } else {
            //Can't detect framework assume browser.
            //Here we want to check for localstorage and make sure the browser has it
            if (typeof window.localStorage !== "undefined") {
                //If no uuid is set in localstorage create a new one, and set it as the session's deviceId
                if (self.deviceConfig.deviceIdCaptureEnabled) {
                    sessionSummary.deviceId = generateDeviceId();
                }
            }
            if (typeof navigator.userAgent !== "undefined") {
                //Small hack to make all device names consistent.
                var browserData = determineBrowserType(navigator.userAgent, navigator.appName);
                sessionSummary.devicePlatform = browserData.devicePlatform;
                sessionSummary.deviceOSVersion = browserData.deviceOSVersion;
                if (typeof navigator.language !== "undefined") {
                    sessionSummary.localLanguage = navigator.language;
                }
            }
        }
        if (isTitanium()) {
            Ti.App.fireEvent("analytics:attachReady");
        }
        return sessionSummary;
    };
    /**
   * Registers a device with Apigee Monitoring. Generates a new UUID for a device and collects relevant info on it.
   *
   * @method registerDevice
   * @public
   *
   */
    Apigee.MonitoringClient.prototype.startSession = function() {
        if (this.configuration === null || this.configuration === "undefined") {
            return;
        }
        //If the user agent string exists on the device
        var self = this;
        var sessionSummary = {};
        //timeStamp goes first because it is used in other properties
        sessionSummary.timeStamp = timeStamp();
        //defaults for other properties
        sessionSummary.appConfigType = this.appConfigType;
        sessionSummary.appId = this.appId.toString();
        sessionSummary.applicationVersion = "undefined" !== typeof this.appVersion ? this.appVersion.toString() : UNKNOWN;
        sessionSummary.batteryLevel = "-100";
        sessionSummary.deviceCountry = UNKNOWN;
        sessionSummary.deviceId = UNKNOWN;
        sessionSummary.deviceModel = UNKNOWN;
        sessionSummary.deviceOSVersion = UNKNOWN;
        sessionSummary.devicePlatform = UNKNOWN;
        sessionSummary.localCountry = UNKNOWN;
        sessionSummary.localLanguage = UNKNOWN;
        sessionSummary.networkCarrier = UNKNOWN;
        sessionSummary.networkCountry = UNKNOWN;
        sessionSummary.networkSubType = UNKNOWN;
        sessionSummary.networkType = UNKNOWN;
        sessionSummary.sdkType = SDKTYPE;
        sessionSummary.sessionId = randomUUID();
        sessionSummary.sessionStartTime = sessionSummary.timeStamp;
        self.startLocationCapture();
        self.sessionMetrics = self.detectAppPlatform(sessionSummary);
    };
    /**
   * Method to encapsulate the monkey patching of AJAX methods. We pass in the XMLHttpRequest object for monkey patching.
   *
   * @public
   * @param {XMLHttpRequest} XHR
   *
   */
    Apigee.MonitoringClient.prototype.patchNetworkCalls = function(XHR) {
        "use strict";
        var apigee = this;
        var open = XHR.prototype.open;
        var send = XHR.prototype.send;
        XHR.prototype.open = function(method, url, async, user, pass) {
            this._method = method;
            this._url = url;
            open.call(this, method, url, async, user, pass);
        };
        XHR.prototype.send = function(data) {
            var self = this;
            var startTime;
            var oldOnReadyStateChange;
            var method = this._method;
            var url = this._url;
            function onReadyStateChange() {
                if (self.readyState == 4) // complete
                {
                    //gap_exec and any other platform specific filtering here
                    //gap_exec is used internally by phonegap, and shouldn't be logged.
                    var monitoringURL = apigee.getMonitoringURL();
                    if (url.indexOf("/!gap_exec") === -1 && url.indexOf(monitoringURL) === -1) {
                        var endTime = timeStamp();
                        var latency = endTime - startTime;
                        var summary = {
                            url: url,
                            startTime: startTime.toString(),
                            endTime: endTime.toString(),
                            numSamples: "1",
                            latency: latency.toString(),
                            timeStamp: startTime.toString(),
                            httpStatusCode: self.status.toString(),
                            responseDataSize: self.responseText.length.toString()
                        };
                        if (self.status == 200) {
                            //Record the http call here
                            summary.numErrors = "0";
                            apigee.logNetworkCall(summary);
                        } else {
                            //Record a connection failure here
                            summary.numErrors = "1";
                            apigee.logNetworkCall(summary);
                        }
                    } else {}
                }
                if (oldOnReadyStateChange) {
                    oldOnReadyStateChange();
                }
            }
            if (!this.noIntercept) {
                startTime = timeStamp();
                if (this.addEventListener) {
                    this.addEventListener("readystatechange", onReadyStateChange, false);
                } else {
                    oldOnReadyStateChange = this.onreadystatechange;
                    this.onreadystatechange = onReadyStateChange;
                }
            }
            send.call(this, data);
        };
    };
    Apigee.MonitoringClient.prototype.patchLoggingCalls = function() {
        //Hacky way of tapping into this and switching it around but it'll do.
        //We assume that the first argument is the intended log message. Except assert which is the second message.
        var self = this;
        var original = window.console;
        window.console = {
            log: function() {
                self.logInfo({
                    tag: "CONSOLE",
                    logMessage: arguments[0]
                });
                original.log.apply(original, arguments);
            },
            warn: function() {
                self.logWarn({
                    tag: "CONSOLE",
                    logMessage: arguments[0]
                });
                original.warn.apply(original, arguments);
            },
            error: function() {
                self.logError({
                    tag: "CONSOLE",
                    logMessage: arguments[0]
                });
                original.error.apply(original, arguments);
            },
            assert: function() {
                self.logAssert({
                    tag: "CONSOLE",
                    logMessage: arguments[1]
                });
                original.assert.apply(original, arguments);
            },
            debug: function() {
                self.logDebug({
                    tag: "CONSOLE",
                    logMessage: arguments[0]
                });
                original.debug.apply(original, arguments);
            }
        };
        if (isTitanium()) {
            //Patch console.log to work in Titanium as well.
            var originalTitanium = Ti.API;
            window.console.log = function() {
                originalTitanium.info.apply(originalTitanium, arguments);
            };
            Ti.API = {
                info: function() {
                    self.logInfo({
                        tag: "CONSOLE_TITANIUM",
                        logMessage: arguments[0]
                    });
                    originalTitanium.info.apply(originalTitanium, arguments);
                },
                log: function() {
                    var level = arguments[0];
                    if (level === "info") {
                        self.logInfo({
                            tag: "CONSOLE_TITANIUM",
                            logMessage: arguments[1]
                        });
                    } else if (level === "warn") {
                        self.logWarn({
                            tag: "CONSOLE_TITANIUM",
                            logMessage: arguments[1]
                        });
                    } else if (level === "error") {
                        self.logError({
                            tag: "CONSOLE_TITANIUM",
                            logMessage: arguments[1]
                        });
                    } else if (level === "debug") {
                        self.logDebug({
                            tag: "CONSOLE_TITANIUM",
                            logMessage: arguments[1]
                        });
                    } else if (level === "trace") {
                        self.logAssert({
                            tag: "CONSOLE_TITANIUM",
                            logMessage: arguments[1]
                        });
                    } else {
                        self.logInfo({
                            tag: "CONSOLE_TITANIUM",
                            logMessage: arguments[1]
                        });
                    }
                    originalTitanium.log.apply(originalTitanium, arguments);
                }
            };
        }
    };
    /**
   * Prepares data for syncing on window close.
   *
   * @method prepareSync
   * @public
   *
   */
    Apigee.MonitoringClient.prototype.prepareSync = function() {
        var syncObject = {};
        var self = this;
        //Just in case something bad happened.
        if (typeof self.sessionMetrics !== "undefined") {
            syncObject.sessionMetrics = self.sessionMetrics;
        }
        var syncFlag = false;
        this.syncDate = timeStamp();
        //Go through each of the aggregated metrics
        //If there are unreported metrics present add them to the object to be sent across the network
        if (metrics.length > 0) {
            syncFlag = true;
        }
        if (logs.length > 0) {
            syncFlag = true;
        }
        syncObject.logs = logs;
        syncObject.metrics = metrics;
        //If there is data to sync go ahead and do it.
        if (syncFlag && !self.testMode) {
            this.sync(syncObject);
        }
    };
    /**
   * Logs a user defined message.
   *
   * @method logMessage
   * @public
   * @param {object} options
   *
   */
    Apigee.MonitoringClient.prototype.logMessage = function(options) {
        var log = options || {};
        var cleansedLog = {
            logLevel: log.logLevel,
            logMessage: log.logMessage.substring(0, 250),
            tag: log.tag,
            timeStamp: timeStamp()
        };
        logs.push(cleansedLog);
    };
    /**
   * Logs a user defined verbose message.
   *
   * @method logDebug
   * @public
   * @param {object} options
   *
   */
    Apigee.MonitoringClient.prototype.logVerbose = function(options) {
        var logOptions = options || {};
        logOptions.logLevel = LOGLEVELS.verbose;
        if (this.deviceConfig.logLevelToMonitor >= LOGLEVELNUMBERS.verbose) {
            this.logMessage(options);
        }
    };
    /**
   * Logs a user defined debug message.
   *
   * @method logDebug
   * @public
   * @param {object} options
   *
   */
    Apigee.MonitoringClient.prototype.logDebug = function(options) {
        var logOptions = options || {};
        logOptions.logLevel = LOGLEVELS.debug;
        if (this.deviceConfig.logLevelToMonitor >= LOGLEVELNUMBERS.debug) {
            this.logMessage(options);
        }
    };
    /**
   * Logs a user defined informational message.
   *
   * @method logInfo
   * @public
   * @param {object} options
   *
   */
    Apigee.MonitoringClient.prototype.logInfo = function(options) {
        var logOptions = options || {};
        logOptions.logLevel = LOGLEVELS.info;
        if (this.deviceConfig.logLevelToMonitor >= LOGLEVELNUMBERS.info) {
            this.logMessage(options);
        }
    };
    /**
   * Logs a user defined warning message.
   *
   * @method logWarn
   * @public
   * @param {object} options
   *
   */
    Apigee.MonitoringClient.prototype.logWarn = function(options) {
        var logOptions = options || {};
        logOptions.logLevel = LOGLEVELS.warn;
        if (this.deviceConfig.logLevelToMonitor >= LOGLEVELNUMBERS.warn) {
            this.logMessage(options);
        }
    };
    /**
   * Logs a user defined error message.
   *
   * @method logError
   * @public
   * @param {object} options
   *
   */
    Apigee.MonitoringClient.prototype.logError = function(options) {
        var logOptions = options || {};
        logOptions.logLevel = LOGLEVELS.error;
        if (this.deviceConfig.logLevelToMonitor >= LOGLEVELNUMBERS.error) {
            this.logMessage(options);
        }
    };
    /**
   * Logs a user defined assert message.
   *
   * @method logAssert
   * @public
   * @param {object} options
   *
   */
    Apigee.MonitoringClient.prototype.logAssert = function(options) {
        var logOptions = options || {};
        logOptions.logLevel = LOGLEVELS.assert;
        if (this.deviceConfig.logLevelToMonitor >= LOGLEVELNUMBERS.assert) {
            this.logMessage(options);
        }
    };
    /**
   * Internal function for encapsulating crash log catches. Not directly callable.
   * Needed because of funkiness with the errors being thrown solely on the window
   *
   */
    function logCrash(options) {
        var log = options || {};
        var cleansedLog = {
            logLevel: LOGLEVELS.assert,
            logMessage: log.logMessage,
            tag: log.tag,
            timeStamp: timeStamp()
        };
        logs.push(cleansedLog);
    }
    /**
   * Logs a network call.
   *
   * @method logNetworkCall
   * @public
   * @param {object} options
   *
   */
    Apigee.MonitoringClient.prototype.logNetworkCall = function(options) {
        metrics.push(options);
    };
    /**
   * Retrieves monitoring URL.
   *
   * @method getMonitoringURL
   * @public
   * @returns {string} value
   *
   */
    Apigee.MonitoringClient.prototype.getMonitoringURL = function() {
        return this.URI + "/" + this.orgName + "/" + this.appName + "/apm/";
    };
    /**
   * Gets custom config parameters. These are set by user in dashboard.
   *
   * @method getConfig
   * @public
   * @param {string} key
   * @returns {stirng} value
   *
   * TODO: Once there is a dashboard plugged into the API implement this so users can set
   * custom configuration parameters for their applications.
   */
    Apigee.MonitoringClient.prototype.getConfig = function(key) {};
    //TEST HELPERS NOT REALLY MEANT TO BE USED OUTSIDE THAT CONTEXT.
    //Simply exposes some internal data that is collected.
    Apigee.MonitoringClient.prototype.logs = function() {
        return logs;
    };
    Apigee.MonitoringClient.prototype.metrics = function() {
        return metrics;
    };
    Apigee.MonitoringClient.prototype.getSessionMetrics = function() {
        return this.sessionMetrics;
    };
    Apigee.MonitoringClient.prototype.clearMetrics = function() {
        logs = [];
        metrics = [];
    };
    Apigee.MonitoringClient.prototype.mixin = function(destObject) {
        var props = [ "bind", "unbind", "trigger" ];
        for (var i = 0; i < props.length; i++) {
            destObject.prototype[props[i]] = MicroEvent.prototype[props[i]];
        }
    };
    //UUID Generation function unedited
    /** randomUUID.js - Version 1.0
   *
   * Copyright 2008, Robert Kieffer
   *
   * This software is made available under the terms of the Open Software License
   * v3.0 (available here: http://www.opensource.org/licenses/osl-3.0.php )
   *
   * The latest version of this file can be found at:
   * http://www.broofa.com/Tools/randomUUID.js
   *
   * For more information, or to comment on this, please go to:
   * http://www.broofa.com/blog/?p=151
   */
    /**
   * Create and return a "version 4" RFC-4122 UUID string.
   */
    function randomUUID() {
        var s = [], itoh = "0123456789ABCDEF", i;
        // Make array of random hex digits. The UUID only has 32 digits in it, but we
        // allocate an extra items to make room for the '-'s we'll be inserting.
        for (i = 0; i < 36; i++) {
            s[i] = Math.floor(Math.random() * 16);
        }
        // Conform to RFC-4122, section 4.4
        s[14] = 4;
        // Set 4 high bits of time_high field to version
        s[19] = s[19] & 3 | 8;
        // Specify 2 high bits of clock sequence
        // Convert to hex chars
        for (i = 0; i < 36; i++) {
            s[i] = itoh[s[i]];
        }
        // Insert '-'s
        s[8] = s[13] = s[18] = s[23] = "-";
        return s.join("");
    }
    //Generate an epoch timestamp string
    function timeStamp() {
        return new Date().getTime().toString();
    }
    //Generate a device id, and attach it to localStorage.
    function generateDeviceId() {
        var deviceId = "UNKNOWN";
        try {
            if ("undefined" === typeof localStorage) {
                throw new Error("device or platform does not support local storage");
            }
            if (window.localStorage.getItem("uuid") === null) {
                window.localStorage.setItem("uuid", randomUUID());
            }
            deviceId = window.localStorage.getItem("uuid");
        } catch (e) {
            deviceId = randomUUID();
            console.warn(e);
        } finally {
            return deviceId;
        }
    }
    //Helper. Determines if the platform device is phonegap
    function isPhoneGap() {
        return typeof cordova !== "undefined" || typeof PhoneGap !== "undefined" || typeof window.device !== "undefined";
    }
    //Helper. Determines if the platform device is trigger.io
    function isTrigger() {
        return typeof window.forge !== "undefined";
    }
    //Helper. Determines if the platform device is titanium.
    function isTitanium() {
        return typeof Titanium !== "undefined";
    }
    /**
   * @method determineBrowserType
   */
    var BROWSERS = [ "Opera", "MSIE", "Safari", "Chrome", "Firefox" ];
    function createBrowserRegex(browser) {
        return new RegExp("\\b(" + browser + ")\\/([^\\s]+)");
    }
    function createBrowserTest(userAgent, positive, negatives) {
        var matches = BROWSER_REGEX[positive].exec(userAgent);
        negatives = negatives || [];
        if (matches && matches.length && !negatives.some(function(negative) {
            return BROWSER_REGEX[negative].exec(userAgent);
        })) {
            return matches.slice(1, 3);
        }
    }
    var BROWSER_REGEX = [ "Seamonkey", "Firefox", "Chromium", "Chrome", "Safari", "Opera" ].reduce(function(p, c) {
        p[c] = createBrowserRegex(c);
        return p;
    }, {});
    BROWSER_REGEX["MSIE"] = new RegExp(";(MSIE) ([^\\s]+)");
    var BROWSER_TESTS = [ [ "MSIE" ], [ "Opera", [] ], [ "Seamonkey", [] ], [ "Firefox", [ "Seamonkey" ] ], [ "Chromium", [] ], [ "Chrome", [ "Chromium" ] ], [ "Safari", [ "Chromium", "Chrome" ] ] ].map(function(arr) {
        return createBrowserTest(navigator.userAgent, arr[0], arr[1]);
    });
    function determineBrowserType(ua, appName) {
        //var ua = navigator.userAgent;
        var browserName = appName;
        var nameOffset, verOffset, verLength, ix, fullVersion = UNKNOWN;
        var browserData = {
            devicePlatform: UNKNOWN,
            deviceOSVersion: UNKNOWN
        };
        var browserData = BROWSER_TESTS.reduce(function(p, c) {
            return c ? c : p;
        }, "UNKNOWN");
        browserName = browserData[0];
        fullVersion = browserData[1];
        if (browserName === "MSIE") {
            browserName = "Microsoft Internet Explorer";
        }
        browserData.devicePlatform = browserName;
        browserData.deviceOSVersion = fullVersion;
        return browserData;
    }
    global[name] = {
        Client: Apigee.Client,
        Entity: Apigee.entity,
        Collection: Apigee.collection,
        Group: Apigee.group,
        MonitoringClient: Apigee.MonitoringClient,
        AUTH_CLIENT_ID: Apigee.AUTH_CLIENT_ID,
        AUTH_APP_USER: Apigee.AUTH_APP_USER,
        AUTH_NONE: Apigee.AUTH_NONE
    };
    global[name].noConflict = function() {
        if (overwrittenName) {
            global[name] = overwrittenName;
        }
        return Apigee;
    };
    return global[name];
})();