/**
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

var request = require('request');
var Usergrid = {};
Usergrid.SDK_VERSION = '0.10.02';

//authentication type constants
var AUTH_CLIENT_ID = 'CLIENT_ID';
var AUTH_APP_USER = 'APP_USER';
var AUTH_NONE = 'NONE';

Usergrid.Client = function(options) {
  //usergrid enpoint
  this.URI = 'https://api.usergrid.com';

  //Find your Orgname and Appname in the Admin portal (http://apigee.com/usergrid)
  this.orgName = options.orgName;
  this.appName = options.appName;

  //authentication data
  this.authType = options.authType || AUTH_NONE;
  this.clientId = options.clientId;
  this.clientSecret = options.clientSecret;
  this.token = options.token || null;

  //other options
  this.buildCurl = options.buildCurl || false;
  this.logging = options.logging || false;

  //timeout and callbacks
  this._callTimeout = options.callTimeout || 30000; //default to 30 seconds
  this._callTimeoutCallback = options.callTimeoutCallback || null;
  this.logoutCallback = options.logoutCallback || null;

};

/**
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
Usergrid.Client.prototype.request = function (options, callback) {
  var self = this;
  var method = options.method || 'GET';
  var endpoint = options.endpoint;
  var body = options.body || {};
  var qs = options.qs || {};
  var mQuery = options.mQuery || false; //is this a query to the management endpoint?
  if (mQuery) {
    var uri = this.URI + '/' + endpoint;
  } else {
    var uri = this.URI + '/' + this.orgName + '/' + this.appName + '/' + endpoint;
  }

  if (this.authType === AUTH_CLIENT_ID) {
    qs['client_id'] = this.clientId;
    qs['client_secret'] = this.clientSecret;
  } else if (this.authType === AUTH_APP_USER) {
    qs['access_token'] = this.token;
  }

  if (this.logging) {
    console.log('calling: ' + method + ' ' + uri);
  }
  this._start = new Date().getTime();
  var callOptions = {
    method: method,
    uri: uri,
    json: body,
    qs: qs
  };
  request(callOptions, function (err, r, data) {
    if (self.buildCurl) {
      options.uri = r.request.uri.href;
      self.buildCurlCall(options);
    }
    self._end = new Date().getTime();
    if(r.statusCode === 200) {
      if (self.logging) {
        console.log('success (time: ' + self.calcTimeDiff() + '): ' + method + ' ' + uri);
      }
      callback(err, data);
    } else {
      err = true;
      if ((r.error === 'auth_expired_session_token') ||
        (r.error === 'unauthorized')   ||
        (r.error === 'auth_missing_credentials')   ||
        (r.error === 'auth_invalid')) {
        //this error type means the user is not authorized. If a logout function is defined, call it
        var error = r.body.error;
        var errorDesc = r.body.error_description;
        if (self.logging) {
          console.log('Error (' + r.statusCode + ')(' + error + '): ' + errorDesc)
        }
        //if the user has specified a logout callback:
        if (typeof(self.logoutCallback) === 'function') {
          self.logoutCallback(err, data);
        } else  if (typeof(callback) === 'function') {
          callback(err, data);
        }
      } else {
        var error = r.body.error;
        var errorDesc = r.body.error_description;
        if (self.logging) {
          console.log('Error (' + r.statusCode + ')(' + error + '): ' + errorDesc);
        }
        if (typeof(callback) === 'function') {
          callback(err, data);
        }
      }
    }
  });
}

/**
*  Main function for creating new entities - should be called directly.
*
*  options object: options {client:client, data:{'type':'collection_type', 'key':'value'}, uuid:uuid}}
*
*  @method createEntity
*  @public
*  @params {object} options
*  @param {function} callback
*  @return {callback} callback(err, data)
*/
Usergrid.Client.prototype.createEntity = function (options, callback) {
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
}

/**
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
Usergrid.Client.prototype.createCollection = function (options, callback) {
  options.client = this;
  var collection = new Usergrid.Collection(options, function(err, data) {
    if (typeof(callback) === 'function') {
      callback(err, collection);
    }
  });
}

/**
*  A private method to get call timing of last call
*/
Usergrid.Client.prototype.calcTimeDiff = function () {
 var seconds = 0;
 var time = this._end - this._start;
 try {
    seconds = ((time/10) / 60).toFixed(2);
 } catch(e) { return 0; }
 return seconds;
}

/*
*  A public method to log in an app user - stores the token for later use
*
*  @method login
*  @public
*  @params {string} username
*  @params {string} password
*  @param {function} callback
*  @return {callback} callback(err, data)
*/
Usergrid.Client.prototype.login = function (username, password, callback) {
  var self = this;
  var options = {
    method:'GET',
    endpoint:'token',
    qs:{
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
      user = new Usergrid.Entity('users', data.user);
      self.token = data.access_token;
    }
    if (typeof(callback) === 'function') {
      callback(err, data, user);
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
Usergrid.Client.prototype.loginFacebook = function (facebookToken, callback) {
  var self = this;
  var options = {
    method:'GET',
    endpoint:'auth/facebook',
    qs:{
      fb_access_token: facebookToken
    }
  };
  this.request(options, function(err, data) {
    var user = {};
    if (err && self.logging) {
      console.log('error trying to log user in');
    } else {
      user = new Usergrid.Entity('users', data.user);
      self.token = data.access_token;
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
Usergrid.Client.prototype.getLoggedInUser = function (callback) {
  if (!this.token) {
    callback(true, null, null);
  } else {
    var self = this;
    var options = {
      method:'GET',
      endpoint:'users/me',
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
        var user = new Usergrid.Entity('users', data.user);
        if (typeof(callback) === 'function') {
          callback(err, data, user);
        }
      }
    });
  }
}

/**
*  A public method to test if a user is logged in - does not guarantee that the token is still valid,
*  but rather that one exists
*
*  @method isLoggedIn
*  @public
*  @return {boolean} Returns true the user is logged in (has token and uuid), false if not
*/
Usergrid.Client.prototype.isLoggedIn = function () {
  if (this.token) {
    return true;
  }
  return false;
}

/**
*  A public method to log out an app user - clears all user fields from client
*
*  @method logout
*  @public
*  @return none
*/
Usergrid.Client.prototype.logout = function () {
  this.token = null;
}

/*
*  A private method to build the curl call to display on the command line
*
*  @method buildCurlCall
*  @private
*  @param {object} options
*  @return {string} curl
*/
Usergrid.Client.prototype.buildCurlCall = function (options) {
  var curl = 'curl';
  var method = (options.method || 'GET').toUpperCase();
  var body = options.body || {};
  var uri = options.uri;

  //curl - add the method to the command (no need to add anything for GET)
  if (method === 'POST') {curl += ' -X POST'; }
  else if (method === 'PUT') { curl += ' -X PUT'; }
  else if (method === 'DELETE') { curl += ' -X DELETE'; }
  else { curl += ' -X GET'; }

  //curl - append the path
  curl += ' ' + uri;

  //curl - add the body
  body = JSON.stringify(body)
  if (body !== '"{}"' && method !== 'GET' && method !== 'DELETE') {
    //curl - add in the json obj
    curl += " -d '" + body + "'";
  }

  //log the curl command to the console
  console.log(curl);

  return curl;
}


/**
*  A class to Model a Usergrid Entity.
*  Set the type of entity in the 'data' json object
*
*  @constructor
*  @param {object} options {client:client, data:{'type':'collection_type', 'key':'value'}, uuid:uuid}}
*/
Usergrid.Entity = function(options) {
  this._client = options.client;
  this._data = options.data || {};
};

/**
*  gets a specific field or the entire data object. If null or no argument
*  passed, will return all data, else, will return a specific field
*
*  @method get
*  @param {string} field
*  @return {string} || {object} data
*/
Usergrid.Entity.prototype.get = function (field) {
  if (field) {
    return this._data[field];
  } else {
    return this._data;
  }
}

/**
*  adds a specific key value pair or object to the Entity's data
*  is additive - will not overwrite existing values unless they
*  are explicitly specified
*
*  @method set
*  @param {string} key || {object}
*  @param {string} value
*  @return none
*/
Usergrid.Entity.prototype.set = function (key, value) {
  if (typeof key === 'object') {
    for(var field in key) {
      this._data[field] = key[field];
    }
  } else if (typeof key === 'string') {
    if (value === null) {
      delete this._data[key];
    } else {
      this._data[key] = value;
    }
  } else {
    this._data = null;
  }
}

/**
*  Saves the entity back to the database
*
*  @method save
*  @public
*  @param {function} callback
*  @return {callback} callback(err, data)
*/
Usergrid.Entity.prototype.save = function (callback) {
  //TODO:  API will be changed soon to accomodate PUTs via name which create new entities
  //       This function should be changed to PUT only at that time, and updated to use
  //       either uuid or name
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
        item === 'type' || item === 'activatted' ) { continue; }
    data[item] = entityData[item];
  }
  var options =  {
    method:method,
    endpoint:type,
    body:data
  };
  //save the entity first
  this._client.request(options, function (err, retdata) {
    if (err && self._client.logging) {
      console.log('could not save entity');
      if (typeof(callback) === 'function') {
        return callback(err, retdata, self);
      }
    } else {
      if (retdata.entities.length) {
        var entity = retdata.entities[0];
        self.set(entity);
      }
      //if this is a user, update the password if it has been specified;
      var needPasswordChange = (type === 'users' && entityData.oldpassword && entityData.newpassword);
      if (needPasswordChange) {
        //Note: we have a ticket in to change PUT calls to /users to accept the password change
        //      once that is done, we will remove this call and merge it all into one
        var pwdata = {};
        pwdata.oldpassword = entityData.oldpassword;
        pwdata.newpassword = entityData.newpassword;
        this._client.request(
          {
            method:'PUT',
            endpoint:type,
            body:pwdata
          },
          function (err, data) {
            if (err && self._client.logging) {
              console.log('could not update user');
            }
            //remove old and new password fields so they don't end up as part of the entity object
            self.set('oldpassword', null);
            self.set('newpassword', null);
            if (typeof(callback) === 'function') {
              callback(err, data, self);
            }
          }
        );
      } else if (typeof(callback) === 'function') {
        callback(err, retdata, self);
      }
    }
  });
}

/**
*  refreshes the entity by making a GET call back to the database
*
*  @method fetch
*  @public
*  @param {function} callback
*  @return {callback} callback(err, data)
*/
Usergrid.Entity.prototype.fetch = function (callback) {
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
          var error = 'cannot fetch entity, no username specified';
          if (self._client.logging) {
            console.log(error);
          }
          return callback(true, error, self)
        }
      }
    } else {
      if (this.get('name')) {
        type += '/' + this.get('name');
      } else {
        if (typeof(callback) === 'function') {
          var error = 'cannot fetch entity, no name specified';
          if (self._client.logging) {
            console.log(error);
          }
          return callback(true, error, self)
        }
      }
    }
  }
  var options = {
    method:'GET',
    endpoint:type
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('could not get entity');
    } else {
      if (data.user) {
        self.set(data.user);
      } else if (data.entities.length) {
        var entity = data.entities[0];
        self.set(entity);
      }
    }
    if (typeof(callback) === 'function') {
      callback(err, data, self);
    }
  });
}

/**
*  deletes the entity from the database - will only delete
*  if the object has a valid uuid
*
*  @method destroy
*  @public
*  @param {function} callback
*  @return {callback} callback(err, data)
*
*/
Usergrid.Entity.prototype.destroy = function (callback) {
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
    method:'DELETE',
    endpoint:type
  };
  this._client.request(options, function (err, data) {
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



/**
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
  this._client = options.client;
  this._type = options.type;
  this.qs = options.qs || {};

  //iteration
  this._list = [];
  this._iterator = -1; //first thing we do is increment, so set to -1

  //paging
  this._previous = [];
  this._next = null;
  this._cursor = null


  var self = this;

  //populate the collection
  this.fetch(callback);
}

/**
*  Populates the collection from the server
*
*  @method fetch
*  @param {function} callback
*  @return {callback} callback(err, data)
*/
Usergrid.Collection.prototype.fetch = function (callback) {
  var self = this;
  var qs = this.qs;

  //add in the cursor if one is available
  if (this._cursor) {
    qs.cursor = this._cursor;
  } else {
    delete qs.cursor;
  }
  var options = {
    method:'GET',
    endpoint:this._type,
    qs:this.qs
  };
  this._client.request(options, function (err, data) {
    if(err && self._client.logging) {
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
        for (var i=0;i<count;i++) {
          var uuid = data.entities[i].uuid;
          if (uuid) {
            var entityData = data.entities[i] || {};
            var entityOptions = {
              type:self._type,
              client:self._client,
              uuid:uuid,
              data:entityData
            };
            var ent = new Usergrid.Entity(entityOptions);
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

/**
*  Adds a new Entity to the collection (saves, then adds to the local object)
*
*  @method addNewEntity
*  @param {object} entity
*  @param {function} callback
*  @return {callback} callback(err, data, entity)
*/
Usergrid.Collection.prototype.addEntity = function (options, callback) {
  var self = this;
  options.type = this._type;

  //create the new entity
  this._client.createEntity(options, function (err, entity) {
    if (!err) {
      //then add the entity to the list
      var count = self._list.length;
      self._list[count] = entity;
    }
    if (typeof(callback) === 'function') {
      callback(err, entity);
    }
  });
}

/**
*  Removes the Entity from the collection, then destroys the object on the server
*
*  @method destroyEntity
*  @param {object} entity
*  @param {function} callback
*  @return {callback} callback(err, data)
*/
Usergrid.Collection.prototype.destroyEntity = function (entity, callback) {
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
}

/**
*  Looks up an Entity by UUID
*
*  @method getEntityByUUID
*  @param {string} UUID
*  @param {function} callback
*  @return {callback} callback(err, data, entity)
*/
Usergrid.Collection.prototype.getEntityByUUID = function (uuid, callback) {
  //get the entity from the database
  var options = {
    data: {
    	type: this._type,
    	uuid:uuid
    },
    client: this._client
  }
  var entity = new Usergrid.Entity(options);
  entity.fetch(callback);
}

/**
*  Returns the first Entity of the Entity list - does not affect the iterator
*
*  @method getFirstEntity
*  @return {object} returns an entity object
*/
Usergrid.Collection.prototype.getFirstEntity = function () {
  var count = this._list.length;
  if (count > 0) {
    return this._list[0];
  }
  return null;
}

/**
*  Returns the last Entity of the Entity list - does not affect the iterator
*
*  @method getLastEntity
*  @return {object} returns an entity object
*/
Usergrid.Collection.prototype.getLastEntity = function () {
  var count = this._list.length;
  if (count > 0) {
    return this._list[count-1];
  }
  return null;
}

/**
*  Entity iteration -Checks to see if there is a "next" entity
*  in the list.  The first time this method is called on an entity
*  list, or after the resetEntityPointer method is called, it will
*  return true referencing the first entity in the list
*
*  @method hasNextEntity
*  @return {boolean} true if there is a next entity, false if not
*/
Usergrid.Collection.prototype.hasNextEntity = function () {
  var next = this._iterator + 1;
  var hasNextElement = (next >=0 && next < this._list.length);
  if(hasNextElement) {
    return true;
  }
  return false;
}

/**
*  Entity iteration - Gets the "next" entity in the list.  The first
*  time this method is called on an entity list, or after the method
*  resetEntityPointer is called, it will return the,
*  first entity in the list
*
*  @method hasNextEntity
*  @return {object} entity
*/
Usergrid.Collection.prototype.getNextEntity = function () {
  this._iterator++;
  var hasNextElement = (this._iterator >= 0 && this._iterator <= this._list.length);
  if(hasNextElement) {
    return this._list[this._iterator];
  }
  return false;
}

/**
*  Entity iteration - Checks to see if there is a "previous"
*  entity in the list.
*
*  @method hasPrevEntity
*  @return {boolean} true if there is a previous entity, false if not
*/
Usergrid.Collection.prototype.hasPrevEntity = function () {
  var previous = this._iterator - 1;
  var hasPreviousElement = (previous >=0 && previous < this._list.length);
  if(hasPreviousElement) {
    return true;
  }
  return false;
}

/**
*  Entity iteration - Gets the "previous" entity in the list.
*
*  @method getPrevEntity
*  @return {object} entity
*/
Usergrid.Collection.prototype.getPrevEntity = function () {
   this._iterator--;
   var hasPreviousElement = (this._iterator >= 0 && this._iterator <= this._list.length);
   if(hasPreviousElement) {
    return this.list[this._iterator];
   }
   return false;
}

/**
*  Entity iteration - Resets the iterator back to the beginning
*  of the list
*
*  @method resetEntityPointer
*  @return none
*/
Usergrid.Collection.prototype.resetEntityPointer = function () {
   this._iterator  = -1;
}

/**
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

/**
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

/**
*  Paging -  checks to see if there is a next page od data
*
*  @method hasNextPage
*  @return {boolean} returns true if there is a next page of data, false otherwise
*/
Usergrid.Collection.prototype.hasNextPage = function () {
  return (this._next);
}

/**
*  Paging - advances the cursor and gets the next
*  page of data from the API.  Stores returned entities
*  in the Entity list.
*
*  @method getNextPage
*  @param {function} callback
*  @return {callback} callback(err, data)
*/
Usergrid.Collection.prototype.getNextPage = function (callback) {
  if (this.hasNextPage()) {
    //set the cursor to the next page of data
    this._previous.push(this._cursor);
    this._cursor = this._next;
    //empty the list
    this._list = [];
    this.fetch(callback);
  }
}

/**
*  Paging -  checks to see if there is a previous page od data
*
*  @method hasPreviousPage
*  @return {boolean} returns true if there is a previous page of data, false otherwise
*/
Usergrid.Collection.prototype.hasPreviousPage = function () {
  return (this._previous.length > 0);
}

/**
*  Paging - reverts the cursor and gets the previous
*  page of data from the API.  Stores returned entities
*  in the Entity list.
*
*  @method getPreviousPage
*  @param {function} callback
*  @return {callback} callback(err, data)
*/
Usergrid.Collection.prototype.getPreviousPage = function (callback) {
  if (this.hasPreviousPage()) {
    this._next=null; //clear out next so the comparison will find the next item
    this._cursor = this._previous.pop();
    //empty the list
    this._list = [];
    this.fetch(callback);
  }
}

/**
* Tests if the string is a uuid
*
* @public
* @method isUUID
* @param {string} uuid The string to test
* @returns {Boolean} true if string is uuid
*/
function isUUID (uuid) {
  var uuidValueRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
  if (!uuid) return false;
  return uuidValueRegex.test(uuid);
}

exports.client = Usergrid.Client;
exports.entity = Usergrid.Entity;
exports.collection = Usergrid.Collection;
exports.AUTH_CLIENT_ID = AUTH_CLIENT_ID;
exports.AUTH_APP_USER = AUTH_APP_USER;
exports.AUTH_NONE = AUTH_NONE;