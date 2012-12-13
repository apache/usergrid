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

Client = function(options) {
  //usergrid enpoint
  this._uri = 'https://api.usergrid.com';
  
  //Find your Orgname and Appname in the Admin portal (http://apigee.com/usergrid)
  this._orgName = options.orgName;
  this._appName = options.appName;
  
  //authentication info
  this._AUTH_CLIENT_ID = 'CLIENT_ID';
  this._AUTH_APP_USER = 'APP_USER'; 
  this._AUTH_NONE = 'NONE';

  this._authType = options.authType || this._AUTH_NONE;
  this._clientId = options.clientId;
  this._clientSecret = options.clientSecret;
  this._token = null; //given out by the server
  this._user = null;
  
  
  //timeout and callbacks
  this._callTimeout =  options.callTimeout || 30000; //default to 30 seconds
  this._callTimeoutCallback =  options.callTimeoutCallback || null;
  this._logoutCallback =  options.logoutCallback || null;
   
};

/**
 *  
 *
 *  @method 
 *  @return 
 */
Client.prototype.request = function (options, callback){
  var self = this;
  var method = options.method; 
  var endpoint = options.endpoint; 
  var body = options.body || {};
  var qs = options.qs || {};
  var mQuery = options.mQuery || false; //is this a query to the management endpoint?
  if (mQuery) {
    var uri = this._uri + '/' + endpoint;
  } else {
    var uri = this._uri + '/' + this._orgName + '/' + this._appName + '/' + endpoint;
  }
  
  if (this._authType === this._AUTH_CLIENT_ID) {
    qs['client_id'] = this._clientId;
    qs['client_secret'] = this._clientSecret;
  } else if (this._authType === this._AUTH_APP_USER) {
    qs['access_token'] = this._token;     
  } else {
    //fine if only hitting sandbox app, where no credentials are required
    console.log('no authType specified'); 
  }  
  console.log('calling: ' + method + ' ' + uri); 
  this.setQueryStartTime();
  request(
    { method: method
    , uri: uri
    , json: body
    , qs: qs
    }
    , function (err, r, data) {
      self.setQueryEndTime();
      if(r.statusCode === 200){
        console.log('success (time: ' + self.getQueryTotalTime() + '): ' + method + ' ' + uri); 
        callback(err, data);        
      } else {
        err = true;         
        if ( (r.error === "auth_expired_session_token") ||
          (r.error === "unauthorized")   ||
          (r.error === "auth_missing_credentials")   ||
          (r.error === "auth_invalid")) {
          //this error type means the user is not authorized. If a logout function is defined, call it
          console.log(r.error);
          //if the user has specified a logout callback:
          if (typeof(self._logoutCallback) === "function") {
            self._logoutCallback(err, data);
          } else  if (typeof(callback) === "function") {
            callback(err, data);            
          } 
        } else {
          console.log('error: '+ r.statusCode);
          if (typeof(callback) === "function") {
            callback(err, data);            
          }   
        }        
      }
    }
  )
}    

/**
*  A private method to start call timing
*/
Client.prototype.setQueryStartTime = function (){
 this._start = new Date().getTime();
}

/**
*  A private method to stop call timing
*/
Client.prototype.setQueryEndTime = function (){
 this._end = new Date().getTime();
}

/**
*  A private method to get call timing of last call
*/
Client.prototype.getQueryTotalTime = function (){
 var seconds = 0;
 var time = this._end - this._start;
 try {
    seconds = ((time/10) / 60).toFixed(2);
 } catch(e){ return 0; }
 return seconds;
}

/*
*  A public method to log in an app user - stores the token for later use
*
*  @method login
*  @public
*  @params {string} username
*  @params {string} password
*  @params {function} callback
*  @return {response} callback functions return API response object
*/
Client.prototype.login = function (username, password, callback){
  var self = this;
  var options = {
    method:'GET'
    , endpoint:'token' 
    , qs:{"username": username, "password": password, "grant_type": "password"}
  };
  this.request(options, function(err, data) {
    var user = {};
    if (err) {
      console.log('error trying to log user in'); 
    } else {
      user = new Entity('users');
      user.set(data.user);
      self.setLoggedInUser(user);
      self.setToken(data.access_token);
    }
    if (typeof(callback) === "function") {
      callback(err, data, user);
    }
  });
}

/**
*  A public method to test if a user is logged in - does not guarantee that the token is still valid,
*  but rather that one exists, and that there is a valid UUID
*
*  @method isLoggedInAppUser
*  @public
*  @params {object} Query - {method, path, jsonObj, params, successCallback, failureCallback}
*  @return {boolean} Returns true the user is logged in (has token and uuid), false if not
*/
Client.prototype.isLoggedInAppUser = function (){
  var user = this.getLoggedInUser();
  if (this.getToken()) {
    if (user) { 
      if (isUUID(user.get('uuid'))) {
        return true;
      }
    }
  }
}

/*
*  A private method to build the curl call to display on the command line
*
*  @method buildCurlCall
*  @private
*  @param {object} Query
*  @param {string} endpoint
*  @return none
*/
Client.prototype.buildCurlCall = function (options) {
  var curl = 'curl';
  try {
    //peel the data out of the query object
    var method = Query.getMethod().toUpperCase();
    var path = Query.getResource();
    var jsonObj = Query.getJsonObj() || {};
    var params = Query.getQueryParams() || {};
    
    //curl - add the method to the command (no need to add anything for GET)
    if (method === "POST") {curl += " -X POST"; }
    else if (method === "PUT") { curl += " -X PUT"; }
    else if (method === "DELETE") { curl += " -X DELETE"; }
    else { curl += " -X GET"; }
    
    //curl - append the bearer token if this is not the sandbox app
    var application_name = ApiClient.getApplicationName();
    if (application_name) {
      application_name = application_name.toUpperCase();
    }
    if ( _authType === USER && ((application_name != 'SANDBOX' && ApiClient.getToken()) || (getQueryType() === M && ApiClient.getToken())) ) {
      curl += ' -i -H "Authorization: Bearer ' + ApiClient.getToken() + '"';
      Query.setToken(true);
    }
    
    path = ApiClient.getApiUrl() + '/' + endpoint + '/' + path;
    
    //make sure path never has more than one / together
    if (path) {
      //regex to strip multiple slashes
      while(path.indexOf('//') != -1){
        path = path.replace('//', '/');
      }
    }
    
    //curl - append the path
    curl += ' "http://' + path;

    //curl - append params to the path for curl prior to adding the timestamp
    var curl_encoded_params = encodeParams(params);
    if (curl_encoded_params) {
      curl += "?" + curl_encoded_params;
    }
    curl += '"';
    
    jsonObj = JSON.stringify(jsonObj)
    if (jsonObj && jsonObj != '{}') {
      //curl - add in the json obj
      curl += " -d '" + jsonObj + "'";
    }
    
  } catch(e) {
   console.log('Unable to build curl call:' + e);
  }
  //log the curl command to the console
  console.log(curl);
  //store the curl command back in the object
  Query.setCurl(curl);
  
  return curl; 
}

/****************************************************************
* The following functions should be overriden for session storage
*/

/*
*  A public method to get current OAuth token
*
*  @method getToken
*  @public
*  @return {string} the current token
*/
Client.prototype.getToken = function (){
  return this._token;
}

/*
*  A public method to set the current Oauth token
*
*  @method setToken
*  @public
*  @param token - the bearer token
*  @return none
*/
Client.prototype.setToken = function (token){
  this._token=token;
}

/*
 *  A public method to get an Entity object for the current logged in user
 *
 *  @method getLoggedInUser
 *  @public
 *  @return {object} user - Entity object of type user
 */
Client.prototype.getLoggedInUser = function (){
  return this._user;
}

/*
 *  A public method to set an Entity object for the current logged in user
 *
 *  @method setLoggedInUser
 *  @public
 *  @param {object} user - Entity object of type user
 *  @return none
 */
Client.prototype.setLoggedInUser = function (user){
  this._user = user;
}

/**
 *  A public method to log out an app user - clears all user fields from client
 *
 *  @method logoutAppUser
 *  @public
 *  @return none
 */
Client.prototype.logoutAppUser = function (){
  this.setLoggedInUser(null);
  this.setToken(null);
}




/**
 *  A class to Model a Usergrid Entity.
 *
 *  @class Entity
 *  @author Rod Simpson (rod@apigee.com)
 */

/**
 *  Constructor for initializing an entity
 *
 *  @constructor
 *  @param {string} options - options object: {client:client, type:'collection_type', data:{'key':'value'}, uuid:uuid}
 *  @param {uuid} callback
 */
Entity = function(options) {
  this._client = options.client;
  this._type = options.type;
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
Entity.prototype.get = function (field){
  if (field) {
    return this._data[field];
  } else {
    return this._data;
  }
},

/**
 *  adds a specific key value pair or object to the Entity's data
 *
 *  @method set
 *  @param {string} key || {object}
 *  @param {string} value
 *  @return none
 */
Entity.prototype.set = function (key, value){
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
 *  @return none
 */
Entity.prototype.save = function (callback){
  //TODO:  API will be changed soon to accomodate PUTs via name which create new entities
  //       This function should be changed to PUT only at that time, and updated to use
  //       either uuid or name
  var path = this._type;
  var method = 'POST';
  if (isUUID(this.get('uuid'))) {
    method = 'PUT';
    path += '/' + this.get('uuid');
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

  this._client.request(
    { 
      method:method
    , endpoint:path
    , body:data
    } ,
    function (err, retdata) {
      if (err) {
        console.log('could not save entity');
        if (typeof(callback) === 'function'){
          return callback(err);
        }
      } else {
        if (retdata.entities.length) {
          var entity = retdata.entities[0];
          self.set(entity);  
        }
        //if this is a user, update the password if it has been specified;
        if (path === 'users' && entityData.oldpassword && entityData.newpassword) {
          //Note: we have a ticket in to change PUT calls to /users to accept the password change
          //      once that is done, we will remove this call and merge it all into one
          var pwdata = {};
          pwdata.oldpassword = entityData.oldpassword;
          pwdata.newpassword = entityData.newpassword;
          this._client.request(
            { 
              method:'PUT'
            , endpoint:path
            , body:pwdata
            } ,
            function (err, data) {
              if (err) {
                console.log('could not update user'); 
              }
              //remove old and new password fields so they don't end up as part of the entity object
              self.set('oldpassword', null);
              self.set('newpassword', null);
              if (typeof(callback) === 'function'){
                callback(err);
              }
            }
          );     
        } else if (typeof(callback) === 'function'){
          callback(err);
        }
      }      
    }
  );
}

/**
 *  refreshes the entity by making a GET call back to the database
 *
 *  @method fetch
 *  @public
 *  @param {function} callback
 *  @return none
 */
Entity.prototype.fetch = function (callback){
  var path = this._type;
  //if a uuid is available, use that, otherwise, use the name
  if (isUUID(this.get('uuid'))) {
    path += '/' + this.get('uuid');
  } else {
    if (path === "users") {
      if (this.get("username")) {
        path += '/' + this.get("username");
      } else {
        console.log('no username specified');
        if (typeof(callback) === "function"){
          var error = 'no username specified';
          console.log(error); 
          callback(true, error)
        }
      }
    } else {
      if (isUUID(this.get('uuid'))) {
        path += '/' + this.get('uuid');
      } else if (this.get('name')) {
        path += '/' + this.get('name');
      }else {
        console.log('no entity identifier specified');
        if (typeof(callback) === "function"){
          var error = 'no entity identifier specified'; 
          console.log(error); 
          callback(true, error);
        }
      }
    }
  }
  var self = this;
  
  this._client.request(
    { 
      method:'GET'
    , endpoint:path
    } ,
    function (err, data) {
      if (err) {
        console.log('could not get entity');
      } else {
        if (data.user) {
          self.set(data.user);
        } else if (data.entities.length) {
          var entity = data.entities[0];
          self.set(entity);  
        }  
      }
      if (typeof(callback) === "function"){
        callback(err, data);
      }
    }
  );
}

/**
 *  deletes the entity from the database - will only delete
 *  if the object has a valid uuid
 *
 *  @method destroy
 *  @public
 *  @param {function} callback
 *  @return none
 *
 */ 
Entity.prototype.destroy = function (callback){
  var path = this._type;
  if (isUUID(this.get('uuid'))) {
    path += "/" + this.get('uuid');
  } else {
    if (typeof(callback) === "function"){
      var error = 'Error trying to delete object - no uuid specified.';
      console.log(error); 
      callback(true, error);
    }
  }
  var self = this;
  
  this._client.request(
    { method:"DELETE"
    , endpoint:path
    } ,
    function (err, data) {
      if (err) {
        console.log('entity could not be deleted');
      } else {
        self.set(null); 
      }
      if (typeof(callback) === "function"){
        callback(err, data);
      } 
    }
  );
}



/**
 *  The Collection class models Usergrid Collections.  It essentially
 *  acts as a container for holding Entity objects, while providing
 *  additional funcitonality such as paging, and saving
 *
 *  @class Collection
 *  @author Rod Simpson (rod@apigee.com)
 */
/**
 *  Collection is a container class for holding entities
 *
 *  @constructor
 *  @param {string} collectionPath - the type of collection to model
 *  @param {uuid} uuid - (optional), the UUID of the collection if it is known
 */
Collection = function(options, callback) {
  this._client = options.client;
  this._type = options.type;
  this._uuid = options.uuid;
  this._qs = options.qs || {};
  
  //iteration  
  this._list = [];
  this._iterator = -1; //first thing we do is increment, so set to -1
  
  //paging
  this._previous = [];
  this._next = null;
  this._cursor = null
  
  
  var self = this;
  
  //get list of collections, see if this one exists
  this._client.request(
    { method:"GET"
    , endpoint:""
    } ,
    function (err, body) {
      if (err) {
        console.log('error getting collections - check options passed to client');  
        return callback(err);      
      } else {
        //store collection list
        var collections = body.entities[0].metadata.collections;
        if ( collections.hasOwnProperty(self._type) ) {
          //collection exists, so just fetch
          self.fetch(function(err){
            return callback(err);
          });
        } else {
          //collection doesn't exist, post first
          self._client.request(
            { method:"POST"
            , endpoint:self._type
            , json:{}
            , qs:self._qs
            } ,
            function (err, body) {
              if (!err) {
                console.log('collection created'); 
              }
              callback(err, body);
            }
          );
        }
      }
    }
  );
};

/**
 *  
 *
 *  @method 
 *  @return 
 */
Collection.prototype.fetch = function (callback){
  var self = this;
  var qs = this._qs;
  console.log('fetching');
  
  //add in the cursor if one is available
  if (this._cursor) {
    qs.cursor = this._cursor; 
  } else {
    delete qs.cursor; 
  }
  this._client.request(
    { method:"GET"
    , endpoint:this._type
    , qs:this._qs
    } ,
    function (err, data) {
      if(err) {
       console.log('error getting collection'); 
      } else {
        //save the cursor if there is one
        var cursor = data.cursor || null;
        self.saveCursor(cursor);
        if (data.entities) {
          self.resetEntityPointer();
          var count = data.entities.length;
          //save entities locally
          for (var i=0;i<count;i++) {
            var uuid = data.entities[i].uuid;
            if (uuid) {      
              var entityData = data.entities[i] || {};
              var options = {
                type:self._type
                , client:self._client
                , uuid:uuid
                , data:entityData
              }
              var ent = new Entity(options);
              var ct = self._list.length;
              self._list[ct] = ent;
            }
          }
        } 
      }
      if (typeof(callback) === "function"){
        callback(err, data);
      }
    }
  );
}

/**
 *  Adds a new Entity to the collection (saves, then adds to the local object)
 *
 *  @method addNewEntity
 *  @param {object} entity
 *  @return none
 */
Collection.prototype.addEntity = function (entity, callback) {
  //add the entity to the list
  var count = this._list.length;
  this._list[count] = entity;
  //then save the entity
  entity.save(callback);
}

/**
 *  Looks up an Entity by UUID
 *
 *  @method getEntityByUUID
 *  @param {string} UUID
 *  @return {object} returns an entity object, or null if it is not found
 */
Collection.prototype.getEntityByUUID = function (uuid){
  var count = this._list.length;
  var i=0;
  for (i=0; i<count; i++) {
    if (this._list[i].get('uuid') === uuid) {
      return this._list[i];
    }
  }
  return null;
}

/**
 *  Returns the first Entity of the Entity list - does not affect the iterator
 *
 *  @method getFirstEntity
 *  @return {object} returns an entity object
 */
Collection.prototype.getFirstEntity = function (){
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
Collection.prototype.getLastEntity = function (){
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
Collection.prototype.hasNextEntity = function (){
  var next = this._iterator + 1;
  if(next >=0 && next < this._list.length) {
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
Collection.prototype.getNextEntity = function (){
  this._iterator++;
  if(this._iterator >= 0 && this._iterator <= this._list.length) {
    return this._list[this._iterator];
  }
  return false;
}

/**
 *  Entity iteration - Checks to see if there is a "previous"
 *  entity in the list.
 *
 *  @method hasPreviousEntity
 *  @return {boolean} true if there is a previous entity, false if not
 */
Collection.prototype.hasPreviousEntity = function (){
  var previous = this._iterator - 1;
    if(previous >=0 && previous < this._list.length) {
      return true;
    }
    return false;
}

/**
 *  Entity iteration - Gets the "previous" entity in the list.
 *
 *  @method getPreviousEntity
 *  @return {object} entity
 */
Collection.prototype.getPreviousEntity = function (){
   this._iterator--;
    if(this._iterator >= 0 && this._iterator <= this._list.length) {
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
Collection.prototype.resetEntityPointer = function (){
   this._iterator  = -1;
}

/**
 *  Removes the Entity from the collection, then destroys the object on the server
 * 
 *  @method destroyEntity
 *  @param {object} entity
 *  @param {callback} callback
 *  @return none
 */
Collection.prototype.destroyEntity = function (entity, callback) {
  var self = this;
  entity.destroy(function(err){
    if (err) {
      console.log('could not destroy entity'); 
      callback(err);
    } else {
      //destroy was good, so repopulate the collection
      self.fetch(callback);
    }
  });  
}

/**
* Method to save off the cursor just returned by the last API call
*
* @public
* @method saveCursor
* @return none
*/
Collection.prototype.saveCursor = function(cursor) {
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
Collection.prototype.resetPaging = function() {
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
Collection.prototype.hasNextPage = function (){
  return (this._next);
}

/**
 *  Paging - advances the cursor and gets the next
 *  page of data from the API.  Stores returned entities
 *  in the Entity list.
 *
 *  @method getNextPage
 *  @return none
 */
Collection.prototype.getNextPage = function (callback){
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
Collection.prototype.hasPreviousPage = function (){
  return (this._previous.length > 0);
}

/**
 *  Paging - reverts the cursor and gets the previous
 *  page of data from the API.  Stores returned entities
 *  in the Entity list.
 *
 *  @method getPreviousPage
 *  @return none
 */
Collection.prototype.getPreviousPage = function (callback){
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

exports.client = Client;
exports.entity = Entity;
exports.collection = Collection;