/*
 *  This module is a collection of classes designed to make working with
 *  the Appigee App Services API as easy as possible.
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
 */
(function(window, localStorage) {


  //Hack around IE console.log
  window.console = window.console || {};
  window.console.log = window.console.log || function() {};

  //Usergrid namespace encapsulates this SDK
  window.Usergrid = window.Usergrid || {};
  Usergrid = Usergrid || {};
  Usergrid.SDK_VERSION = '0.10.07';
  Usergrid.browser = Usergrid.browser || {};

  Usergrid.Client = function(options, url) {
    //usergrid enpoint
    this.URI = url || 'https://api.usergrid.com';

    //Find your Orgname and Appname in the Admin portal (http://apigee.com/usergrid)
    if (options.orgName) {
      this.set('orgName', options.orgName);
    }
    if (options.appName) {
      this.set('appName', options.appName);
    }

    if (options.keys) {
      this._keys = options.keys;
    }

    //other options
    this.buildCurl = options.buildCurl || false;
    this.logging = options.logging || false;

    //timeout and callbacks
    this._callTimeout = options.callTimeout || 30000; //default to 30 seconds
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
    callback = callback || function() {
      console.error('no callback handed to client.request().')
    };
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
    var developerkey = self.get("developerkey");
    if (developerkey) {
      qs.key = developerkey;
    }

    var isIE9 = Usergrid.browser.isIE9 = $.browser.msie && $.browser.version <=
      9;

    (isIE9) && (method === 'PUT' || method === 'DELETE') && (function() {
      qs['method_override'] = method;
    })();

    //append params to the path
    var encoded_params = encodeParams(qs);
    if (encoded_params) {
      uri += "?" + encoded_params;
    }
    //stringify the body object
    body = options.formData ? null : JSON.stringify(body);
    //so far so good, so run the query
    //*** ie9 hack
    var xhr;
    //check to see if ie9 - if so, convert delete and put calls to a POST
    if (isIE9) { //XDomainRequest
      xhr = new XDomainRequest();
      (method === 'PUT' || method === 'DELETE') && (function() {
        method = 'POST';
      })();
      xhr.open(method, uri);
    } else {
      xhr = new XMLHttpRequest();
      xhr.open(method, uri, true);
      //add content type = json if there is a json payload
      if (!options.formData) {
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.setRequestHeader("Accept", "application/json");
      }
    }
    xhr.isIE9 = isIE9;
    // Handle response.
    xhr.onerror = function(response) {
      self._end = new Date().getTime();
      if (self.logging) {
        console.log("success (time: " + self.calcTimeDiff() + "): " + method +
          " " + uri);
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
        console.log("success (time: " + self.calcTimeDiff() + "): " + method +
          " " + uri);
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
      if (!xhr.isIE9 && xhr.status != 200) {
        //there was an api error
        var error = response.error;
        var error_description = response.error_description;
        if (self.logging) {
          console.log("Error (" + xhr.status + ")(" + error + "): " +
            error_description);
        }
        if (error == "auth_expired_session_token" || error ==
          "auth_missing_credentials" || error == "auth_unverified_oath" ||
          error == "expired_token" || error == "unauthorized" || error ==
          "auth_invalid") {
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
        callback("API CALL TIMEOUT");
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
    xhr.send(options.formData || body);
  };

  Usergrid.Client.prototype.keys = function(o) {
    var a = [];
    for (var propertyName in o) {
      a.push(propertyName);
    }
    return a;
  }

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

    var options = {
      path: options.path,
      client: this,
      data: options
    }

    var group = new Usergrid.Group(options);
    group.fetch(function(err, data) {
      var okToSave = (err && ('service_resource_not_found' === data.error ||
        'no_name_specified' === data.error || 'null_pointer' === data.error ||
        Usergrid.browser.isIE9)) || (!err && getOnExist);
      if (okToSave) {
        group.save(function(err, data) {
          if (typeof(callback) === 'function') {
            callback(err, group);
          }
        });
      } else {
        if (typeof(callback) === 'function') {
          callback(err, group);
        }
      }
    });
  }

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
  var options = {
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
    var getOnExist = options.getOnExist || false; //if true, will return entity if one already exists
    var options = {
      client: this,
      data: options
    }
    var entity = new Usergrid.Entity(options);
    entity.fetch(function(err, data) {
      //if the fetch doesn't find what we are looking for, or there is no error, do a save
      if (Usergrid.browser.isIE9 || 'service_resource_not_found' === data.error ||
        'no_name_specified' === data.error || 'null_pointer' === data.error
      ) {

        entity.set(options.data); //add the data again just in case
        entity.save(function(err, data) {
          if (typeof(callback) === 'function') {
            callback(err, entity, data);
          }
        });

      } else {
        if (getOnExist) {
          // entity exists and they want it returned
          if (typeof(callback) === 'function') {
            callback(err, entity, data);
          }
        } else {
          //entity exists but they want an error to that effect
          err = true;
          callback(err, 'duplicate entity already exists');
        }
      }

    });

  }

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
    }
    var entity = new Usergrid.Entity(options);
    entity.fetch(function(err, data) {
      if (typeof(callback) === 'function') {
        callback(err, entity, data);
      }
    });
  }
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
    }
    var entity = new Usergrid.Entity(options);
    return entity;
  }

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
      if (typeof(callback) === 'function') {
        callback(err, collection, data);
      }
    });
  }

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
  }

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
    }

    this.request(options, function(err, data) {
      if (typeof(callback) === "function") {
        if (err) {
          callback(err);
        } else {
          callback(err, data, data.entities);
        }
      }
    });
  }

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
  Usergrid.Client.prototype.createUserActivity = function(user, options,
    callback) {
    options.type = 'users/' + user + '/activities';
    var options = {
      client: this,
      data: options
    }
    var entity = new Usergrid.Entity(options);
    entity.save(function(err, data) {
      if (typeof(callback) === 'function') {
        callback(err, entity);
      }
    });
  }

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
  Usergrid.Client.prototype.createUserActivityWithEntity = function(user,
    content, callback) {
    var username = user.get("username");
    var options = {
      actor: {
        "displayName": username,
        "uuid": user.get("uuid"),
        "username": username,
        "email": user.get("email"),
        "picture": user.get("picture"),
        "image": {
          "duration": 0,
          "height": 80,
          "url": user.get("picture"),
          "width": 80
        },
      },
      "verb": "post",
      "content": content
    };

    this.createUserActivity(username, options, callback);

  }

  /*
   *  A private method to get call timing of last call
   */
  Usergrid.Client.prototype.calcTimeDiff = function() {
    var seconds = 0;
    var time = this._end - this._start;
    try {
      seconds = ((time / 10) / 60).toFixed(2);
    } catch (e) {
      return 0;
    }
    return seconds;
  }

  /*
   *  A public method to store the OAuth token for later use - uses localstorage if available
   *
   *  @method setToken
   *  @public
   *  @params {string} token
   *  @return none
   */
  Usergrid.Client.prototype.setToken = function(token) {
    this.set('token', token);
  }

  /*
   *  A public method to get the OAuth token
   *
   *  @method getToken
   *  @public
   *  @return {string} token
   */
  Usergrid.Client.prototype.getToken = function() {
    return this.get('token');
  }

  Usergrid.Client.prototype.setObject = function(key, value) {
    if (value) {
      value = JSON.stringify(value);
    }
    this.set(key, value);
  }

  Usergrid.Client.prototype.set = function(key, value) {
    var keyStore = 'apigee_' + key;
    this[key] = value;
    if (typeof(Storage) !== "undefined") {
      if (value) {
        localStorage.setItem(keyStore, value);
      } else {
        localStorage.removeItem(keyStore);
      }
    }
  }

  Usergrid.Client.prototype.getObject = function(key) {
    return JSON.parse(this.get(key));
  }

  Usergrid.Client.prototype.get = function(key) {
    var keyStore = 'apigee_' + key;
    if (this[key]) {
      return this[key];
    } else if (typeof(Storage) !== "undefined") {
      return localStorage.getItem(keyStore);
    }
    return null;
  }

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
  Usergrid.Client.prototype.signup = function(username, password, email, name,
    callback) {
    var self = this;
    var options = {
      type: "users",
      username: username,
      password: password,
      email: email,
      name: name
    };

    this.createEntity(options, callback);
  }

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
      method: 'POST',
      endpoint: 'token',
      body: {
        username: username,
        password: password,
        grant_type: 'password'
      }
    };
    this.request(options, function(err, data) {
      var user = {};
      if (err && self.logging) {
        console.log('error trying to log user in');
      } else {
        var options = {
          client: self,
          data: data.user
        }
        user = new Usergrid.Entity(options);
        self.setToken(data.access_token);
      }
      if (typeof(callback) === 'function') {
        callback(err, data, user);
      }
    });
  }


  Usergrid.Client.prototype.reAuthenticateLite = function(callback) {
    var self = this;
    var options = {
      method: 'GET',
      endpoint: 'management/me',
      mQuery: true
    };
    this.request(options, function(err, response) {
      if (err && self.logging) {
        console.log('error trying to re-authenticate user');
      } else {

        //save the re-authed token and current email/username
        self.setToken(response.access_token);

      }
      if (typeof(callback) === 'function') {
        callback(err);
      }
    });
  }


  Usergrid.Client.prototype.reAuthenticate = function(email, callback) {
    var self = this;
    var options = {
      method: 'GET',
      endpoint: 'management/users/' + email,
      mQuery: true
    };
    this.request(options, function(err, response) {
      var organizations = {};
      var applications = {};
      var user = {};
      if (err && self.logging) {
        console.log('error trying to full authenticate user');
      } else {
        var data = response.data;
        self.setToken(data.token);
        self.set('email', data.email);

        //delete next block and corresponding function when iframes are refactored
        localStorage.setItem('accessToken', data.token);
        localStorage.setItem('userUUID', data.uuid);
        localStorage.setItem('userEmail', data.email);
        //end delete block


        var userData = {
          "username": data.username,
          "email": data.email,
          "name": data.name,
          "uuid": data.uuid
        }
        var options = {
          client: self,
          data: userData
        }
        user = new Usergrid.Entity(options);

        organizations = data.organizations;
        var org = '';
        try {
          //if we have an org stored, then use that one. Otherwise, use the first one.
          var existingOrg = self.get('orgName');
          org = (organizations[existingOrg]) ? organizations[existingOrg] :
            organizations[Object.keys(organizations)[0]];
          self.set('orgName', org.name);
        } catch (e) {
          err = true;
          if (self.logging) {
            console.log('error selecting org');
          }
        } //should always be an org

        applications = self.parseApplicationsArray(org);
        self.selectFirstApp(applications);

        self.setObject('organizations', organizations);
        self.setObject('applications', applications);

      }
      if (typeof(callback) === 'function') {
        callback(err, data, user, organizations, applications);
      }
    });
  }

  Usergrid.Client.prototype.orgLogin = function(username, password, callback) {
    var self = this;
    var options = {
      method: 'POST',
      endpoint: 'management/token',
      mQuery: true,
      body: {
        username: username,
        password: password,
        grant_type: 'password'
      }
    };
    this.request(options, function(err, data) {
      var user = {};
      var organizations = {};
      var applications = {};
      if (err ) {
        if(self.logging) {
          console.log('error trying to log user in');
        }
      } else {
        var options = {
          client: self,
          data: data.user
        }
        user = new Usergrid.Entity(options);
        self.setToken(data.access_token);
        self.set('email', data.user.email);


        //delete next block and corresponding function when iframes are refactored
        localStorage.setItem('accessToken', data.access_token);
        localStorage.setItem('userUUID', data.user.uuid);
        localStorage.setItem('userEmail', data.user.email);
        //end delete block


        organizations = data.user.organizations;
        var org = '';
        try {
          //if we have an org stored, then use that one. Otherwise, use the first one.
          var existingOrg = self.get('orgName');
          org = (organizations[existingOrg]) ? organizations[existingOrg] :
            organizations[Object.keys(organizations)[0]];
          self.set('orgName', org.name);
        } catch (e) {
          err = true;
          if (self.logging) {
            console.log('error selecting org');
          }
        } //should always be an org

        applications = self.parseApplicationsArray(org);
        self.selectFirstApp(applications);

        self.setObject('organizations', organizations);
        self.setObject('applications', applications);

      }
      if (typeof(callback) === 'function') {
        callback(err, data, user, organizations, applications);
      }
    });
  }

  Usergrid.Client.prototype.parseApplicationsArray = function(org) {
    var applications = {};

    for (var key in org.applications) {
      var uuid = org.applications[key];
      var name = key.split("/")[1];
      applications[name] = ({
        uuid: uuid,
        name: name
      });
    }

    return applications;
  }

  Usergrid.Client.prototype.selectFirstApp = function(applications) {
    try {
      //try to select an app if one exists (will use existing if possible)
      var existingApp = this.get('appName');
      var firstApp = Object.keys(applications)[0];
      var appName = (applications[existingApp]) ? existingApp : Object.keys(
        applications)[0];
      this.set('appName', appName);
    } catch (e) {} //may or may not be an application, if no, just fall through
    return appName;
  }

  Usergrid.Client.prototype.createApplication = function(name, callback) {
    var self = this;
    var options = {
      method: 'POST',
      endpoint: 'management/organizations/' + this.get('orgName') +
        '/applications',
      mQuery: true,
      body: {
        name: name
      }
    };
    this.request(options, function(err, response) {
      var applications = {};
      if (err && self.logging) {
        console.log('error trying to create new application');
        if (typeof(callback) === 'function') {
          callback(err, applications);
        }
      } else {
        self.getApplications(callback);
      }
    });
  }

  Usergrid.Client.prototype.getApplications = function(callback) {
    var self = this;
    var options = {
      method: 'GET',
      endpoint: 'management/organizations/' + this.get('orgName') +
        '/applications',
      mQuery: true
    };
    this.request(options, function(err, data) {
      /*
    //grab the applications
    var applicationsData = data.data;
    var applicationNames = self.keys(applicationsData).sort();
    var firstApp = '';
    var applications = [];
    var count = 0;
    for (var i in applicationNames) {
      var name = applicationNames[i];
      var uuid = applicationsData[name];
      name = name.split("/")[1];
      applications.push({uuid:uuid, name:name});
      if (count===0) {
        firstApp = name;
      }
      count++;
    } */

      applications = self.parseApplicationsArray({
        applications: data.data
      });
      self.selectFirstApp(applications);
      self.setObject('applications', applications);

      if (typeof(callback) === 'function') {
        callback(err, applications);
      }
    });
  }

  Usergrid.Client.prototype.getAdministrators = function(callback) {
    var self = this;
    var options = {
      method: 'GET',
      endpoint: 'management/organizations/' + this.get('orgName') +
        '/users',
      mQuery: true
    };
    this.request(options, function(err, data) {
      var administrators = [];
      if (err) {

      } else {
        var administrators = [];
        for (var i in data.data) {
          var admin = data.data[i];
          admin.image = self.getDisplayImage(admin.email, admin.picture);
          administrators.push(admin);
        }
      }

      if (typeof(callback) === 'function') {
        callback(err, administrators);
      }
    });
  }


  Usergrid.Client.prototype.createAdministrator = function(email, callback) {
    var self = this;
    var options = {
      method: 'POST',
      endpoint: 'management/organizations/' + this.get('orgName') +
        '/users',
      mQuery: true,
      body: {
        email: email,
        password: ''
      }
    };
    this.request(options, function(err, response) {
      var admins = {};
      if (err && self.logging) {
        console.log('error trying to create new administrator');
        if (typeof(callback) === 'function') {
          callback(err, admins);
        }
      } else {
        self.getAdministrators(callback);
      }
    });
  }



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
      method: 'GET',
      endpoint: 'auth/facebook',
      qs: {
        fb_access_token: facebookToken
      }
    };
    this.request(options, function(err, data) {
      var user = {};
      if (err && self.logging) {
        console.log('error trying to log user in');
      } else {
        var options = {
          client: self,
          data: data.user
        }
        user = new Usergrid.Entity(options);
        self.setToken(data.access_token);
      }
      if (typeof(callback) === 'function') {
        callback(err, data, user);
      }
    });
  }

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
        method: 'GET',
        endpoint: 'users/me'
      };
      this.request(options, function(err, data) {
        if (err) {
          if (self.logging) {
            console.log('error trying to log user in');
          }
          if (typeof(callback) === 'function') {
            callback(err, data, null);
          }
        } else {
          var options = {
            client: self,
            data: data.entities[0]
          }
          var user = new Usergrid.Entity(options);
          if (typeof(callback) === 'function') {
            callback(err, data, user);
          }
        }
      });
    }
  }

  /*
   *  A public method to test if a user is logged in - does not guarantee that the token is still valid,
   *  but rather that one exists
   *
   *  @method isLoggedIn
   *  @public
   *  @return {boolean} Returns true the user is logged in (has token and uuid), false if not
   */
  Usergrid.Client.prototype.isLoggedIn = function() {
    if (this.getToken()) {
      return true;
    }
    return false;
  }

  /*
   *  A public method to log out an app user - clears all user fields from client
   *
   *  @method logout
   *  @public
   *  @return none
   */
  Usergrid.Client.prototype.logout = function() {
    this.setToken(null);
    this.setObject('organizations', null);
    this.setObject('applications', null);
    this.set('orgName', null);
    this.set('appName', null);
    this.set('email', null);
    this.set("developerkey", null);
  }

  /*
   *  A private method to build the curl call to display on the command line
   *
   *  @method buildCurlCall
   *  @private
   *  @param {object} options
   *  @return {string} curl
   */
  Usergrid.Client.prototype.buildCurlCall = function(options) {
    var curl = 'curl';
    var method = (options.method || 'GET').toUpperCase();
    var body = options.body || {};
    var uri = options.uri;

    //curl - add the method to the command (no need to add anything for GET)
    if (method === 'POST') {
      curl += ' -X POST';
    } else if (method === 'PUT') {
      curl += ' -X PUT';
    } else if (method === 'DELETE') {
      curl += ' -X DELETE';
    } else {
      curl += ' -X GET';
    }

    //curl - append the path
    curl += ' ' + uri;

    //curl - add the body
    if (body !== '"{}"' && method !== 'GET' && method !== 'DELETE') {
      //curl - add in the json obj
      curl += " -d '" + body + "'";
    }

    //log the curl command to the console
    console.log(curl);

    return curl;
  }
  Usergrid.Client.prototype.getDisplayImage = function(email, picture, size) {
    try {
      if (picture) {
        return picture;
      }
      var size = size || 50;
      if (email.length) {
        return 'https://secure.gravatar.com/avatar/' + MD5(email) + '?s=' +
          size;
      } else {
        return 'https://apigee.com/usergrid/img/user_profile.png';
      }
    } catch (e) {
      return 'https://apigee.com/usergrid/img/user_profile.png';
    }
  }

  /*
   *  A class to Model a Usergrid Entity.
   *  Set the type of entity in the 'data' json object
   *
   *  @constructor
   *  @param {object} options {client:client, data:{'type':'collection_type', 'key':'value'}, uuid:uuid}}
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
  }

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
  }

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
    if (typeof key === 'object') {
      for (var field in key) {
        this._data[field] = key[field];
      }
    } else if (typeof key === 'string') {
      if (value === null) {
        delete this._data[key];
      } else {
        this._data[key] = value;
      }
    } else {
      this._data = {};
    }
  }

  /*
   *  Saves the entity back to the database
   *
   *  @method save
   *  @public
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
  Usergrid.Entity.prototype.save = function(callback) {
    var type = this.get('type');
    var method = 'POST';
    if (isUUID(this.get('uuid'))) {
      method = 'PUT';
      type += '/' + this.get('uuid');
    }

    //update the entity
    var self = this;
    var data = {};
    var entityData = this.get();
    //remove system specific properties
    for (var item in entityData) {
      if (item === 'metadata' || item === 'created' || item === 'modified' ||
        item === 'type' || item === 'activated' || item === 'uuid') {
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
        console.log('could not save entity');
        if (typeof(callback) === 'function') {
          return callback(err, retdata, self);
        }
      } else {
        if (retdata.entities) {
          if (retdata.entities.length) {
            var entity = retdata.entities[0];
            self.set(entity);
            //for connections, API returns type
            self.set('type', retdata.path);
          }
        }
        //if this is a user, update the password if it has been specified;
        var needPasswordChange = (self.get('type') === 'user' && entityData
          .oldpassword && entityData.newpassword);
        if (needPasswordChange) {
          //Note: we have a ticket in to change PUT calls to /users to accept the password change
          //      once that is done, we will remove this call and merge it all into one
          var pwdata = {};
          pwdata.oldpassword = entityData.oldpassword;
          pwdata.newpassword = entityData.newpassword;
          var options = {
            method: 'PUT',
            endpoint: type + '/password',
            body: pwdata
          }
          self._client.request(options, function(err, data) {
            if (err && self._client.logging) {
              console.log('could not update user');
            }
            //remove old and new password fields so they don't end up as part of the entity object
            self.set('oldpassword', null);
            self.set('newpassword', null);
            if (typeof(callback) === 'function') {
              callback(err, data, self);
            }
          });
        } else if (typeof(callback) === 'function') {
          callback(err, retdata, self);
        }
      }
    });
  }

  /*
   *  refreshes the entity by making a GET call back to the database
   *
   *  @method fetch
   *  @public
   *  @param {function} callback
   *  @return {callback} callback(err, data)
   */
  Usergrid.Entity.prototype.fetch = function(callback) {
    var type = this.get('type');
    var self = this;

    //if a uuid is available, use that, otherwise, use the name
    if (this.get('uuid')) {
      type += '/' + this.get('uuid');
    } else {
      if (type === 'users') {
        if (this.get('username')) {
          type += '/' + this.get('username');
        } else {
          if (typeof(callback) === 'function') {
            var error = 'no_name_specified';
            if (self._client.logging) {
              console.log(error);
            }
            return callback(true, {
              error: error
            }, self)
          }
        }
      } else if (type === 'a path') {

        ///TODO add code to deal with the type as a path

        if (this.get('path')) {
          type += '/' + encodeURIComponent(this.get('name'));
        } else {
          if (typeof(callback) === 'function') {
            var error = 'no_name_specified';
            if (self._client.logging) {
              console.log(error);
            }
            return callback(true, {
              error: error
            }, self)
          }
        }

      } else {
        if (this.get('name')) {
          type += '/' + encodeURIComponent(this.get('name'));
        } else {
          if (typeof(callback) === 'function') {
            var error = 'no_name_specified';
            if (self._client.logging) {
              console.log(error);
            }
            return callback(true, {
              error: error
            }, self)
          }
        }
      }
    }
    var options = {
      method: 'GET',
      endpoint: type
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('could not get entity');
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
      if (typeof(callback) === 'function') {
        callback(err, data, self);
      }
    });
  }

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
    var type = this.get('type');
    if (isUUID(this.get('uuid'))) {
      type += '/' + this.get('uuid');
    } else {
      if (typeof(callback) === 'function') {
        var error = 'Error trying to delete object - no uuid specified.';
        if (self._client.logging) {
          console.log(error);
        }
        callback(true, error);
      }
    }
    var self = this;
    var options = {
      method: 'DELETE',
      endpoint: type
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('entity could not be deleted');
      } else {
        self.set(null);
      }
      if (typeof(callback) === 'function') {
        callback(err, data);
      }
    });
  }

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

    //connectee info
    var connecteeType = entity.get('type');
    var connectee = this.getEntityId(entity);
    if (!connectee) {
      if (typeof(callback) === 'function') {
        var error = 'Error trying to delete object - no uuid specified.';
        if (self._client.logging) {
          console.log(error);
        }
        callback(true, error);
      }
      return;
    }

    //connector info
    var connectorType = this.get('type');
    var connector = this.getEntityId(this);
    if (!connector) {
      if (typeof(callback) === 'function') {
        var error = 'Error in connect - no uuid specified.';
        if (self._client.logging) {
          console.log(error);
        }
        callback(true, error);
      }
      return;
    }

    var endpoint = connectorType + '/' + connector + '/' + connection + '/' +
      connecteeType + '/' + connectee;
    var options = {
      method: 'POST',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('entity could not be connected');
      }
      if (typeof(callback) === 'function') {
        callback(err, data);
      }
    });
  }

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
    if (isUUID(entity.get('uuid'))) {
      id = entity.get('uuid');
    } else {
      if (type === 'users') {
        id = entity.get('username');
      } else if (entity.get('name')) {
        id = entity.get('name');
      }
    }
    return id;
  }

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
    var connectorType = this.get('type');
    var connector = this.getEntityId(this);
    if (!connector) {
      if (typeof(callback) === 'function') {
        var error = 'Error in getConnections - no uuid specified.';
        if (self._client.logging) {
          console.log(error);
        }
        callback(true, error);
      }
      return;
    }

    var endpoint = connectorType + '/' + connector + '/' + connection + '/';
    var options = {
      method: 'GET',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('entity could not be connected');
      }

      self[connection] = {};

      var length = data.entities.length;
      for (var i = 0; i < length; i++) {
        if (data.entities[i].type === 'user') {
          self[connection][data.entities[i].username] = data.entities[i];
        } else {
          self[connection][data.entities[i].name] = data.entities[i]
        }
      }

      if (typeof(callback) === 'function') {
        callback(err, data, data.entities);
      }
    });

  }

  Usergrid.Entity.prototype.getGroups = function(callback) {

    var self = this;

    var endpoint = 'users' + '/' + this.get('uuid') + '/groups';
    var options = {
      method: 'GET',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('entity could not be connected');
      }

      self['groups'] = data.entities;

      if (typeof(callback) === 'function') {
        callback(err, data, data.entities);
      }
    });

  }

  Usergrid.Entity.prototype.getActivities = function(callback) {

    var self = this;

    var endpoint = this.get('type') + '/' + this.get('uuid') + '/activities';
    var options = {
      method: 'GET',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('entity could not be connected');
      }

      for (entity in data.entities) {
        data.entities[entity].createdDate = (new Date(data.entities[entity]
          .created)).toUTCString();
      }

      self['activities'] = data.entities;

      if (typeof(callback) === 'function') {
        callback(err, data, data.entities);
      }
    });

  }

  Usergrid.Entity.prototype.getFollowing = function(callback) {

    var self = this;

    var endpoint = 'users' + '/' + this.get('uuid') + '/following';
    var options = {
      method: 'GET',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('could not get user following');
      }

      for (entity in data.entities) {
        data.entities[entity].createdDate = (new Date(data.entities[entity]
          .created)).toUTCString();
        var image = self._client.getDisplayImage(data.entities[entity].email,
          data.entities[entity].picture);
        data.entities[entity]._portal_image_icon = image;
      }

      self['following'] = data.entities;

      if (typeof(callback) === 'function') {
        callback(err, data, data.entities);
      }
    });

  }


  Usergrid.Entity.prototype.getFollowers = function(callback) {

    var self = this;

    var endpoint = 'users' + '/' + this.get('uuid') + '/followers';
    var options = {
      method: 'GET',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('could not get user followers');
      }

      for (entity in data.entities) {
        data.entities[entity].createdDate = (new Date(data.entities[entity]
          .created)).toUTCString();
        var image = self._client.getDisplayImage(data.entities[entity].email,
          data.entities[entity].picture);
        data.entities[entity]._portal_image_icon = image;
      }

      self['followers'] = data.entities;

      if (typeof(callback) === 'function') {
        callback(err, data, data.entities);
      }
    });

  }

  Usergrid.Entity.prototype.getRoles = function(callback) {

    var self = this;

    var endpoint = this.get('type') + '/' + this.get('uuid') + '/roles';
    var options = {
      method: 'GET',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('could not get user roles');
      }

      self['roles'] = data.entities;

      if (typeof(callback) === 'function') {
        callback(err, data, data.entities);
      }
    });

  }

  Usergrid.Entity.prototype.getPermissions = function(callback) {

    var self = this;

    var endpoint = this.get('type') + '/' + this.get('uuid') + '/permissions';
    var options = {
      method: 'GET',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('could not get user permissions');
      }

      var permissions = [];
      if (data.data) {
        var perms = data.data;
        var count = 0;

        for (var i in perms) {
          count++;
          var perm = perms[i];
          var parts = perm.split(':');
          var ops_part = "";
          var path_part = parts[0];

          if (parts.length > 1) {
            ops_part = parts[0];
            path_part = parts[1];
          }

          ops_part.replace("*", "get,post,put,delete")
          var ops = ops_part.split(',');
          var ops_object = {}
          ops_object['get'] = 'no';
          ops_object['post'] = 'no';
          ops_object['put'] = 'no';
          ops_object['delete'] = 'no';
          for (var j in ops) {
            ops_object[ops[j]] = 'yes';
          }

          permissions.push({
            operations: ops_object,
            path: path_part,
            perm: perm
          });
        }
      }

      self['permissions'] = permissions;

      if (typeof(callback) === 'function') {
        callback(err, data, data.entities);
      }
    });

  }

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
  Usergrid.Entity.prototype.disconnect = function(connection, entity,
    callback) {

    var self = this;

    //connectee info
    var connecteeType = entity.get('type');
    var connectee = this.getEntityId(entity);
    if (!connectee) {
      if (typeof(callback) === 'function') {
        var error = 'Error trying to delete object - no uuid specified.';
        if (self._client.logging) {
          console.log(error);
        }
        callback(true, error);
      }
      return;
    }

    //connector info
    var connectorType = this.get('type');
    var connector = this.getEntityId(this);
    if (!connector) {
      if (typeof(callback) === 'function') {
        var error = 'Error in connect - no uuid specified.';
        if (self._client.logging) {
          console.log(error);
        }
        callback(true, error);
      }
      return;
    }

    var endpoint = connectorType + '/' + connector + '/' + connection + '/' +
      connecteeType + '/' + connectee;
    var options = {
      method: 'DELETE',
      endpoint: endpoint
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('entity could not be disconnected');
      }
      if (typeof(callback) === 'function') {
        callback(err, data);
      }
    });
  }

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
      this._iterator = options.iterator || -1; //first thing we do is increment, so set to -1

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

  }


  /*
   *  gets the data from the collection object for serialization
   *
   *  @method serialize
   *  @return {object} data
   */
  Usergrid.Collection.prototype.serialize = function() {

    //pull out the state from this object and return it
    var data = {}
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
  }

  Usergrid.Collection.prototype.addCollection = function(collectionName,
    options, callback) {
    self = this;
    options.client = this._client;
    var collection = new Usergrid.Collection(options, function(err, data) {
      if (typeof(callback) === 'function') {

        collection.resetEntityPointer();
        while (collection.hasNextEntity()) {
          var user = collection.getNextEntity();
          var email = user.get('email');
          var image = self._client.getDisplayImage(user.get('email'),
            user.get('picture'));
          user._portal_image_icon = image;
        }

        self[collectionName] = collection;
        callback(err, collection);
      }
    });
  }

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
      method: 'GET',
      endpoint: this._type,
      qs: this.qs
    };
    this._client.request(options, function(err, data) {
      if (err && self._client.logging) {
        console.log('error getting collection');
      } else {
        //save the cursor if there is one
        var cursor = data.cursor || null;
        self.saveCursor(cursor);
        if (data.entities) {
          self.resetEntityPointer();
          var count = data.entities.length;
          //save entities locally
          self._list = []; //clear the local list first
          for (var i = 0; i < count; i++) {
            var uuid = data.entities[i].uuid;
            if (uuid) {
              var entityData = data.entities[i] || {};
              self._baseType = data.entities[i].type; //store the base type in the collection
              entityData.type = self._type; //make sure entities are same type (have same path) as parent collection.
              var entityOptions = {
                client: self._client,
                data: entityData
              };

              var ent = new Usergrid.Entity(entityOptions);
              ent._json = JSON.stringify(entityData, null, 2);
              //if this is a user, add in an icon image
              if (self._type === 'users' || self._type === 'user' || data.entities[
                i].type == 'user') {
                var email = entityData.email;
                var picture = entityData.picture;
                var image = self._client.getDisplayImage(email, picture);
                ent._portal_image_icon = image;
              }
              var ct = self._list.length;
              self._list[ct] = ent;
            }
          }
        }
      }
      if (typeof(callback) === 'function') {
        callback(err, data);
      }
    });
  }

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

      if (err) {
        if (typeof(callback) === 'function') {
          callback(err, entity);
        }
      } else {

        if (entity.type === '/users') {
          var image = self._client.getDisplayImage(entity.email, entity.picture);
          entity._portal_image_icon = image;
        }

        if (!err) {
          //then add the entity to the list
          var count = self._list.length;
          self._list[count] = entity;
        }
        if (typeof(callback) === 'function') {
          callback(err, entity);
        }
      }
    });
  }

  Usergrid.Collection.prototype.addExistingEntity = function(entity) {
    //entity should already exist in the db, so just add it to the list
    var count = this._list.length;
    this._list[count] = entity;
  }


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
          console.log('could not destroy entity');
        }
        if (typeof(callback) === 'function') {
          callback(err, data);
        }
      } else {
        //destroy was good, so repopulate the collection
        self.fetch(callback);
      }
    });
    //remove entity from the local store
    this.removeEntity(entity);
  }


  Usergrid.Collection.prototype.removeEntity = function(entity) {
    var uuid = entity.get('uuid');
    for (key in this._list) {
      var listItem = this._list[key];
      if (listItem.get('uuid') === uuid) {
        return this._list.splice(key, 1);
      }
    }
    return false;
  }

  /*
   *  Looks up an Entity by UUID
   *
   *  @method getEntityByUUID
   *  @param {string} UUID
   *  @param {function} callback
   *  @return {callback} callback(err, data, entity)
   */
  Usergrid.Collection.prototype.getEntityByUUID = function(uuid, callback) {

    for (key in this._list) {
      var listItem = this._list[key];
      if (listItem.get('uuid') === uuid) {
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
    }
    var entity = new Usergrid.Entity(options);
    entity.fetch(callback);
  }

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
  }

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
  }

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
    var hasNextElement = (next >= 0 && next < this._list.length);
    if (hasNextElement) {
      return true;
    }
    return false;
  }

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
    var hasNextElement = (this._iterator >= 0 && this._iterator <= this._list
      .length);
    if (hasNextElement) {
      return this._list[this._iterator];
    }
    return false;
  }

  /*
   *  Entity iteration - Checks to see if there is a "previous"
   *  entity in the list.
   *
   *  @method hasPrevEntity
   *  @return {boolean} true if there is a previous entity, false if not
   */
  Usergrid.Collection.prototype.hasPrevEntity = function() {
    var previous = this._iterator - 1;
    var hasPreviousElement = (previous >= 0 && previous < this._list.length);
    if (hasPreviousElement) {
      return true;
    }
    return false;
  }

  /*
   *  Entity iteration - Gets the "previous" entity in the list.
   *
   *  @method getPrevEntity
   *  @return {object} entity
   */
  Usergrid.Collection.prototype.getPrevEntity = function() {
    this._iterator--;
    var hasPreviousElement = (this._iterator >= 0 && this._iterator <= this._list
      .length);
    if (hasPreviousElement) {
      return this.list[this._iterator];
    }
    return false;
  }

  /*
   *  Entity iteration - Resets the iterator back to the beginning
   *  of the list
   *
   *  @method resetEntityPointer
   *  @return none
   */
  Usergrid.Collection.prototype.resetEntityPointer = function() {
    this._iterator = -1;
  }

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
  }

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
  }

  /*
   *  Paging -  checks to see if there is a next page od data
   *
   *  @method hasNextPage
   *  @return {boolean} returns true if there is a next page of data, false otherwise
   */
  Usergrid.Collection.prototype.hasNextPage = function() {
    return (this._next);
  }

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
  }

  /*
   *  Paging -  checks to see if there is a previous page od data
   *
   *  @method hasPreviousPage
   *  @return {boolean} returns true if there is a previous page of data, false otherwise
   */
  Usergrid.Collection.prototype.hasPreviousPage = function() {
    return (this._previous.length > 0);
  }

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
      this._next = null; //clear out next so the comparison will find the next item
      this._cursor = this._previous.pop();
      //empty the list
      this._list = [];
      this.fetch(callback);
    }
  }


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
  }

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
    var groupEndpoint = 'groups/' + this._path;
    var memberEndpoint = 'groups/' + this._path + '/users';

    var groupOptions = {
      method: 'GET',
      endpoint: groupEndpoint
    }

    var memberOptions = {
      method: 'GET',
      endpoint: memberEndpoint
    }

    this._client.request(groupOptions, function(err, data) {
      if (err) {
        if (self._client.logging) {
          console.log('error getting group');
        }
        if (typeof(callback) === 'function') {
          callback(err, data);
        }
      } else {
        if (data.entities) {
          var groupData = data.entities[0];
          self._data = groupData || {};
          self._client.request(memberOptions, function(err, data) {
            if (err && self._client.logging) {
              console.log('error getting group users');
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
            if (typeof(callback) === 'function') {
              callback(err, data, self._list);
            }
          });
        }
      }
    });
  }

  /*
   *  Retrieves the members of a group.
   *
   *  @method members
   *  @public
   *  @param {function} callback
   *  @return {function} callback(err, data);
   */
  Usergrid.Group.prototype.members = function(callback) {
    if (typeof(callback) === 'function') {
      callback(null, this._list);
    }
  }

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
      endpoint: "groups/" + this._path + "/users/" + options.user.get(
        'username')
    }

    this._client.request(options, function(error, data) {
      if (error) {
        if (typeof(callback) === 'function') {
          callback(error, data, data.entities);
        }
      } else {
        self.fetch(callback);
      }
    });
  }

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
      endpoint: "groups/" + this._path + "/users/" + options.user.get(
        'username')
    }

    this._client.request(options, function(error, data) {
      if (error) {
        if (typeof(callback) === 'function') {
          callback(error, data);
        }
      } else {
        self.fetch(callback);
      }
    });
  }

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
    }

    this._client.request(options, function(err, data) {
      if (err && self.logging) {
        console.log('error trying to log user in');
      }
      if (typeof(callback) === 'function') {
        callback(err, data, data.entities);
      }
    });
  }

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
    var options = {
      actor: {
        "displayName": user.get("username"),
        "uuid": user.get("uuid"),
        "username": user.get("username"),
        "email": user.get("email"),
        "picture": user.get("picture"),
        "image": {
          "duration": 0,
          "height": 80,
          "url": user.get("picture"),
          "width": 80
        },
      },
      "verb": "post",
      "content": options.content
    };

    options.type = 'groups/' + this._path + '/activities';
    var options = {
      client: this._client,
      data: options
    }

    var entity = new Usergrid.Entity(options);
    entity.save(function(err, data) {
      if (typeof(callback) === 'function') {
        callback(err, entity);
      }
    });
  }

  /*
   * Tests if the string is a uuid
   *
   * @public
   * @method isUUID
   * @param {string} uuid The string to test
   * @returns {Boolean} true if string is uuid
   */
  function isUUID(uuid) {
    var uuidValueRegex =
      /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
    if (!uuid) return false;
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
    tail = [];
    var item = [];
    if (params instanceof Array) {
      for (i in params) {
        item = params[i];
        if ((item instanceof Array) && (item.length > 1)) {
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

})(window, sessionStorage);
