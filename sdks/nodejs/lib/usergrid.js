/**
 *  This package is a collection of classes designed to make working with
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
 */

/**
 *  Query is a class for holding all query information and paging state
 *
 *  @class Query
 *  @author Rod Simpson (rod@apigee.com)
 */

/**
 *  @constructor
 *  @param {string} method
 *  @param {string} path
 *  @param {object} jsonObj
 *  @param {object} paramsObj
 *  @param {function} successCallback
 *  @param {function} failureCallback
 */
Query = function(method, resource, jsonObj, paramsObj, successCallback, failureCallback) {
  //query vars
  this._method = method;
  this._resource = resource;
  this._jsonObj = jsonObj;
  this._paramsObj = paramsObj;
  this._successCallback = successCallback;
  this._failureCallback = failureCallback;

  //curl command - will be populated by runQuery function
  this._curl = '';
  this._token = false;

  //paging vars
  this._cursor = null;
  this._next = null;
  this._previous = [];
  this._start = 0;
  this._end = 0;
};

Query.prototype = {
   setQueryStartTime: function() {
     this._start = new Date().getTime();
   },

   setQueryEndTime: function() {
     this._end = new Date().getTime();
   },

   getQueryTotalTime: function() {
     var seconds = 0;
     var time = this._end - this._start;
     try {
        seconds = ((time/10) / 60).toFixed(2);
     } catch(e){ return 0; }
     return this.getMethod() + " " + this.getResource() + " - " + seconds + " seconds";
   },
  /**
   *  A method to set all settable parameters of the Query at one time
   *
   *  @public
   *  @method validateUsername
   *  @param {string} method
   *  @param {string} path
   *  @param {object} jsonObj
   *  @param {object} paramsObj
   *  @param {function} successCallback
   *  @param {function} failureCallback
   *  @return none
   */
  setAllQueryParams: function(method, resource, jsonObj, paramsObj, successCallback, failureCallback) {
    this._method = method;
    this._resource = resource;
    this._jsonObj = jsonObj;
    this._paramsObj = paramsObj;
    this._successCallback = successCallback;
    this._failureCallback = failureCallback;
  },

  /**
   *  A method to reset all the parameters in one call
   *
   *  @public
   *  @return none
   */
  clearAll: function() {
    this._method = null;
    this._resource = null;
    this._jsonObj = {};
    this._paramsObj = {};
    this._successCallback = null;
    this._failureCallback = null;
  },
  /**
  * Returns the method
  *
  * @public
  * @method getMethod
  * @return {string} Returns method
  */
  getMethod: function() {
    return this._method;
  },

  /**
  * sets the method (POST, PUT, DELETE, GET)
  *
  * @public
  * @method setMethod
  * @return none
  */
  setMethod: function(method) {
    this._method = method;
  },

  /**
  * Returns the resource
  *
  * @public
  * @method getResource
  * @return {string} the resource
  */
  getResource: function() {
    return this._resource;
  },

  /**
  * sets the resource
  *
  * @public
  * @method setResource
  * @return none
  */
  setResource: function(resource) {
    this._resource = resource;
  },

  /**
  * Returns the json Object
  *
  * @public
  * @method getJsonObj
  * @return {object} Returns the json Object
  */
  getJsonObj: function() {
    return this._jsonObj;
  },

  /**
  * sets the json object
  *
  * @public
  * @method setJsonObj
  * @return none
  */
  setJsonObj: function(jsonObj) {
    this._jsonObj = jsonObj;
  },
  /**
  * Returns the Query Parameters object
  *
  * @public
  * @method getQueryParams
  * @return {object} Returns Query Parameters object
  */
  getQueryParams: function() {
    return this._paramsObj;
  },

  /**
  * sets the query parameter object
  *
  * @public
  * @method setQueryParams
  * @return none
  */
  setQueryParams: function(paramsObj) {
    this._paramsObj = paramsObj;
  },

  /**
  * Returns the success callback function
  *
  * @public
  * @method getSuccessCallback
  * @return {function} Returns the successCallback
  */
  getSuccessCallback: function() {
    return this._successCallback;
  },

  /**
  * sets the success callback function
  *
  * @public
  * @method setSuccessCallback
  * @return none
  */
  setSuccessCallback: function(successCallback) {
    this._successCallback = successCallback;
  },

  /**
  * Calls the success callback function
  *
  * @public
  * @method callSuccessCallback
  * @return {boolean} Returns true or false based on if there was a callback to call
  */
  callSuccessCallback: function(response) {
    if (this._successCallback && typeof(this._successCallback ) === "function") {
      this._successCallback(response);
      return true;
    } else {
      return false;
    }
  },

  /**
  * Returns the failure callback function
  *
  * @public
  * @method getFailureCallback
  * @return {function} Returns the failureCallback
  */
  getFailureCallback: function() {
    return this._failureCallback;
  },

  /**
  * sets the failure callback function
  *
  * @public
  * @method setFailureCallback
  * @return none
  */
  setFailureCallback: function(failureCallback) {
    this._failureCallback = failureCallback;
  },

  /**
  * Calls the failure callback function
  *
  * @public
  * @method callFailureCallback
  * @return {boolean} Returns true or false based on if there was a callback to call
  */
  callFailureCallback: function(response) {
    if (this._failureCallback && typeof(this._failureCallback) === "function") {
      this._failureCallback(response);
      return true;
    } else {
      return false;
    }
  },

  /**
  * Returns the curl call
  *
  * @public
  * @method getCurl
  * @return {function} Returns the curl call
  */
  getCurl: function() {
    return this._curl;
  },

  /**
  * sets the curl call
  *
  * @public
  * @method setCurl
  * @return none
  */
  setCurl: function(curl) {
    this._curl = curl;
  },

  /**
  * Returns the Token
  *
  * @public
  * @method getToken
  * @return {function} Returns the Token
  */
  getToken: function() {
    return this._token;
  },

  /**
  * Method to set
  *
  * @public
  * @method setToken
  * @return none
  */
  setToken: function(token) {
    this._token = token;
  },

  /**
  * Resets the paging pointer (back to original page)
  *
  * @public
  * @method resetPaging
  * @return none
  */
  resetPaging: function() {
    this._previous = [];
    this._next = null;
    this._cursor = null;
  },

  /**
  * Method to determine if there is a previous page of data
  *
  * @public
  * @method hasPrevious
  * @return {boolean} true or false based on if there is a previous page
  */
  hasPrevious: function() {
    return (this._previous.length > 0);
  },

  /**
  * Method to set the paging object to get the previous page of data
  *
  * @public
  * @method getPrevious
  * @return none
  */
  getPrevious: function() {
    this._next=null; //clear out next so the comparison will find the next item
    this._cursor = this._previous.pop();
  },

  /**
  * Method to determine if there is a next page of data
  *
  * @public
  * @method hasNext
  * @return {boolean} true or false based on if there is a next page
  */
  hasNext: function(){
    return (this._next);
  },

  /**
  * Method to set the paging object to get the next page of data
  *
  * @public
  * @method getNext
  * @return none
  */
  getNext: function() {
    this._previous.push(this._cursor);
    this._cursor = this._next;
  },

  /**
  * Method to save off the cursor just returned by the last API call
  *
  * @public
  * @method saveCursor
  * @return none
  */
  saveCursor: function(cursor) {
    //if current cursor is different, grab it for next cursor
    if (this._next !== cursor) {
      this._next = cursor;
    }
  },

  /**
  * Method to determine if there is a next page of data
  *
  * @public
  * @method getCursor
  * @return {string} the current cursor
  */
  getCursor: function() {
    return this._cursor;
  }
};



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
 *  @param {string} collectionType - the type of collection to model
 *  @param {uuid} uuid - (optional), the UUID of the collection if it is known
 */
Entity = function(collectionType, uuid) {
  this._collectionType = collectionType;
  this._data = {};
  if (uuid) {
    this._data['uuid'] = uuid;
  }
};

//inherit prototype from Query
Entity.prototype = new Query();

/**
 *  gets the current Entity type
 *
 *  @method getCollectionType
 *  @return {string} collection type
 */
Entity.prototype.getCollectionType = function (){
  return this._collectionType;
}

/**
 *  sets the current Entity type
 *
 *  @method setCollectionType
 *  @param {string} collectionType
 *  @return none
 */
Entity.prototype.setCollectionType = function (collectionType){
  this._collectionType = collectionType;
}

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
 *  adds a specific field or object to the Entity's data
 *
 *  @method set
 *  @param {string} item || {object}
 *  @param {string} value
 *  @return none
 */
Entity.prototype.set = function (item, value){
  if (typeof item === 'object') {
    for(var field in item) {
      this._data[field] = item[field];
    }
  } else if (typeof item === 'string') {
    if (value === null) {
      delete this._data[item];
    } else {
      this._data[item] = value;
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
 *  @param {function} successCallback
 *  @param {function} errorCallback
 *  @return none
 */
Entity.prototype.save = function (successCallback, errorCallback){
  var path = this.getCollectionType();
  //TODO:  API will be changed soon to accomodate PUTs via name which create new entities
  //       This function should be changed to PUT only at that time, and updated to use
  //       either uuid or name
  var method = 'POST';
  if (this.get('uuid')) {
    method = 'PUT';
    if (validation.isUUID(this.get('uuid'))) {
      path += "/" + this.get('uuid');
    }
  }

  //if this is a user, update the password if it has been specified
  var data = {};
  if (path === 'users') {
    data = this.get();
    var pwdata = {};
    //Note: we have a ticket in to change PUT calls to /users to accept the password change
    //      once that is done, we will remove this call and merge it all into one
    if (data.oldpassword && data.newpassword) {
      pwdata.oldpassword = data.oldpassword;
      pwdata.newpassword = data.newpassword;
      this.runAppQuery(new Query('PUT', 'users/'+uuid+'/password', pwdata, null,
        function (response) {
          //not calling any callbacks - this section will be merged as soon as API supports
          //   updating passwords in general PUT call
        },
        function (response) {

        }
      ));
    }
    //remove old and new password fields so they don't end up as part of the entity object
    delete data.oldpassword;
    delete data.newpassword;
  }

  //update the entity
  var self = this;

  data = {};
  var entityData = this.get();
  //remove system specific properties
  for (var item in entityData) {
    if (item === 'metadata' || item === 'created' || item === 'modified' ||
        item === 'type' || item === 'activatted' ) { continue; }
    data[item] = entityData[item];
  }

  this.setAllQueryParams(method, path, data, null,
    function(response) {
      try {
        var entity = response.entities[0];
        self.set(entity);
        if (typeof(successCallback) == "function"){
          successCallback(response);
        }
      } catch (e) {
        if (typeof(errorCallback) == "function"){
          errorCallback(response);
        }
      }
    },
    function(response) {
      if (typeof(errorCallback) == "function"){
        errorCallback(response);
      }
    }
  );
  ApiClient.runAppQuery(this);
}

/**
 *  refreshes the entity by making a GET call back to the database
 *
 *  @method fetch
 *  @public
 *  @param {function} successCallback
 *  @param {function} errorCallback
 *  @return none
 */
Entity.prototype.fetch = function (successCallback, errorCallback){
  var path = this.getCollectionType();
  //if a uuid is available, use that, otherwise, use the name
  if (this.get('uuid')) {
    path += "/" + this.get('uuid');
  } else {
    if (path == "users") {
      if (this.get("username")) {
        path += "/" + this.get("username");
      } else {
        console.log('no username specified');
        if (typeof(errorCallback) == "function"){
          console.log('no username specified');
        }
      }
    } else {
      if (this.get()) {
        path += "/" + this.get();
      } else {
        console.log('no entity identifier specified');
        if (typeof(errorCallback) == "function"){
          console.log('no entity identifier specified');
        }
      }
    }
  }
  var self = this;
  this.setAllQueryParams('GET', path, null, null,
    function(response) {
      try {
        if (response.user) {
          self.set(response.user);
        }
        var entity = response.entities[0];
        self.set(entity);
        if (typeof(successCallback) == "function"){
          successCallback(response);
        }
      } catch (e) {
        if (typeof(errorCallback) == "function"){
          errorCallback(response);
        }
      }
    },
    function(response) {
      if (typeof(errorCallback) == "function"){
          errorCallback(response);
      }
    }
  );
  ApiClient.runAppQuery(this);
}

/**
 *  deletes the entity from the database - will only delete
 *  if the object has a valid uuid
 *
 *  @method destroy
 *  @public
 *  @param {function} successCallback
 *  @param {function} errorCallback
 *  @return none
 *
 */
Entity.prototype.destroy = function (successCallback, errorCallback){
  var path = this.getCollectionType();
  if (this.get('uuid')) {
    path += "/" + this.get('uuid');
  } else {
    console.log('Error trying to delete object - no uuid specified.');
    if (typeof(errorCallback) == "function"){
      errorCallback('Error trying to delete object - no uuid specified.');
    }
  }
  var self = this;
  this.setAllQueryParams('DELETE', path, null, null,
    function(response) {
      //clear out this object
      self.set(null);
      if (typeof(successCallback) == "function"){
        successCallback(response);
      }
    },
    function(response) {
      if (typeof(errorCallback) == "function"){
          errorCallback(response);
      }
    }
  );
  ApiClient.runAppQuery(this);
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
Collection = function(path, uuid) {
  this._path = path;
  this._uuid = uuid;
  this._list = [];
  this._Query = new Query();
  this._iterator = -1; //first thing we do is increment, so set to -1
};

Collection.prototype = new Query();

/**
 *  gets the current Collection path
 *
 *  @method getPath
 *  @return {string} path
 */
Collection.prototype.getPath = function (){
  return this._path;
}

/**
 *  sets the Collection path
 *
 *  @method setPath
 *  @param {string} path
 *  @return none
 */
Collection.prototype.setPath = function (path){
  this._path = path;
}

/**
 *  gets the current Collection UUID
 *
 *  @method getUUID
 *  @return {string} the uuid
 */
Collection.prototype.getUUID = function (){
  return this._uuid;
}

/**
 *  sets the current Collection UUID
 *  @method setUUID
 *  @param {string} uuid
 *  @return none
 */
Collection.prototype.setUUID = function (uuid){
  this._uuid = uuid;
}

/**
 *  Adds an Entity to the collection (adds to the local object)
 *
 *  @method addEntity
 *  @param {object} entity
 *  @param {function} successCallback
 *  @param {function} errorCallback
 *  @return none
 */
Collection.prototype.addEntity = function (entity){
  //then add it to the list
  var count = this._list.length;
  this._list[count] = entity;
}

/**
 *  Adds a new Entity to the collection (saves, then adds to the local object)
 *
 *  @method addNewEntity
 *  @param {object} entity
 *  @return none
 */
Collection.prototype.addNewEntity = function (entity,successCallback, errorCallback) {
  //add the entity to the list
  this.addEntity(entity);
  //then save the entity
  entity.save(successCallback, errorCallback);
}

/**
 *  Removes the Entity from the collection, then destroys the object on the server
 * 
 *  @method destroyEntity
 *  @param {object} entity
 *  @return none
 */
Collection.prototype.destroyEntity = function (entity) {
  //first get the entities uuid
  var uuid = entity.get('uuid');
  //if the entity has a uuid, delete it
  if (validation.isUUID(uuid)) {
    //then remove it from the list
    var count = this._list.length;
    var i=0;
    var reorder = false;
    for (i=0; i<count; i++) {
      if(reorder) {
        this._list[i-1] = this._list[i];
        this._list[i] = null;
      }
      if (this._list[i].get('uuid') == uuid) {
        this._list[i] = null;
        reorder=true;
      }
    }
  }
  //first destroy the entity on the server
  entity.destroy();
}

/**
 *  Looks up an Entity by a specific field - will return the first Entity that
 *  has a matching field
 *
 *  @method getEntityByField
 *  @param {string} field
 *  @param {string} value
 *  @return {object} returns an entity object, or null if it is not found
 */
Collection.prototype.getEntityByField = function (field, value){
  var count = this._list.length;
  var i=0;
  for (i=0; i<count; i++) {
    if (this._list[i].getField(field) == value) {
      return this._list[i];
    }
  }
  return null;
}

/**
 *  Looks up an Entity by UUID
 *
 *  @method getEntityByUUID
 *  @param {string} UUID
 *  @return {object} returns an entity object, or null if it is not found
 */
Collection.prototype.getEntityByUUID = function (UUID){
  var count = this._list.length;
  var i=0;
  for (i=0; i<count; i++) {
    if (this._list[i].get('uuid') == UUID) {
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
 *  gets and array of all entities currently in the colleciton object
 *
 *  @method getEntityList
 *  @return {array} returns an array of entity objects
 */
Collection.prototype.getEntityList = function (){
   return this._list;
}

/**
 *  sets the entity list
 *
 *  @method setEntityList
 *  @param {array} list - an array of Entity objects
 *  @return none
 */
Collection.prototype.setEntityList = function (list){
  this._list = list;
}

/**
 *  Paging -  checks to see if there is a next page od data
 *
 *  @method hasNextPage
 *  @return {boolean} returns true if there is a next page of data, false otherwise
 */
Collection.prototype.hasNextPage = function (){
  return this.hasNext();
}

/**
 *  Paging - advances the cursor and gets the next
 *  page of data from the API.  Stores returned entities
 *  in the Entity list.
 *
 *  @method getNextPage
 *  @return none
 */
Collection.prototype.getNextPage = function (){
  if (this.hasNext()) {
      //set the cursor to the next page of data
      this.getNext();
      //empty the list
      this.setEntityList([]);
      ApiClient.runAppQuery(this);
    }
}

/**
 *  Paging -  checks to see if there is a previous page od data
 *
 *  @method hasPreviousPage
 *  @return {boolean} returns true if there is a previous page of data, false otherwise
 */
Collection.prototype.hasPreviousPage = function (){
  return this.hasPrevious();
}

/**
 *  Paging - reverts the cursor and gets the previous
 *  page of data from the API.  Stores returned entities
 *  in the Entity list.
 *
 *  @method getPreviousPage
 *  @return none
 */
Collection.prototype.getPreviousPage = function (){
  if (this.hasPrevious()) {
      this.getPrevious();
      //empty the list
      this.setEntityList([]);
      ApiClient.runAppQuery(this);
    }
}

/**
 *  clears the query parameters object
 *
 *  @method clearQuery
 *  @return none
 */
Collection.prototype.clearQuery = function (){
  this.clearAll();
}

/**
 *  A method to get all items in the collection, as dictated by the
 *  cursor and the query.  By default, the API returns 10 items in
 *  a given call.  This can be overriden so that more or fewer items
 *  are returned.  The entities returned are all stored in the colleciton
 *  object's entity list, and can be retrieved by calling getEntityList()
 *
 *  @method get
 *  @param {function} successCallback
 *  @param {function} errorCallback
 *  @return none
 */
Collection.prototype.fetch = function (successCallback, errorCallback){
  var self = this;
  var queryParams = this.getQueryParams();
  //empty the list
  this.setEntityList([]);
  this.setAllQueryParams('GET', this.getPath(), null, queryParams,
    function(response) {
      if (response.entities) {
        this.resetEntityPointer();
        var count = response.entities.length;
        for (var i=0;i<count;i++) {
          var uuid = response.entities[i].uuid;
          if (uuid) {
            var entity = new Entity(self.getPath(), uuid);
            //store the data in the entity
            var data = response.entities[i] || {};
            delete data.uuid; //remove uuid from the object
            entity.set(data);
            //store the new entity in this collection
            self.addEntity(entity);
          }
        }
        if (typeof(successCallback) == "function"){
          successCallback(response);
        }
      } else {
        if (typeof(errorCallback) == "function"){
            errorCallback(response);
        }
      }
    },
    function(response) {
      if (typeof(errorCallback) == "function"){
          errorCallback(response);
      }
    }
  );
  ApiClient.runAppQuery(this);
}

/**
 *  A method to save all items currently stored in the collection object
 *  caveat with this method: we can't update anything except the items
 *  currently stored in the collection.
 *
 *  @method save
 *  @param {function} successCallback
 *  @param {function} errorCallback
 *  @return none
 */
Collection.prototype.save = function (successCallback, errorCallback){
  //loop across all entities and save each one
  var entities = this.getEntityList();
  var count = entities.length;
  var jsonObj = [];
  for (var i=0;i<count;i++) {
    entity = entities[i];
    data = entity.get();
    if (entity.get('uuid')) {
      data.uuid = entity.get('uuid');
      jsonObj.push(data);
    }
    entity.save();
  }
  this.setAllQueryParams('PUT', this.getPath(), jsonObj, null,successCallback, errorCallback);
  ApiClient.runAppQuery(this);
}



/*
 *  ApiClient
 *
 *  A Singleton that is the main client for making calls to the API. Maintains
 *  state between calls for the following items:
 *
 *  Token
 *  User (username, email, name, uuid)
 *
 *  Main methods for making calls to the API are:
 *
 *  runAppQuery (Query)
 *  runManagementQuery(Query)
 *
 *  Create a new Query object and then pass it to either of these
 *  two methods for making calls directly to the API.
 *
 *  A method for logging in an app user (to get a OAuth token) also exists:
 *
 *  logInAppUser (username, password, successCallback, failureCallback)
 *
 *  @class ApiClient
 *  @author Rod Simpson (rod@apigee.com)
 *
 */
var M = 'ManagementQuery';
var A = 'ApplicationQuery'
var USER = 'USER_AUTH';
var CSID = 'CLIENT_SECRET';
ApiClient = (function () {
  //API endpoint
  var _apiUrl = "api.usergrid.com";
  var _orgName = null;
  var _appName = null;
  
  var _authType = USER;
  var _token = null;
  var _clientId = null;
  var _clientSecret = null;
  
  var _callTimeout = 30000;
  var _queryType = null;
  var _loggedInUser = null;
  var _logoutCallback = null;
  var _callTimeoutCallback = null;

  /*
   *  A method to set up the ApiClient with orgname and appname
   *
   *  @method init
   *  @public
   *  @param {string} orgName
   *  @param {string} appName
   *  @return none
   *
   */
  function init(orgName, appName){
    this.setOrganizationName(orgName);
    this.setApplicationName(appName);   
  }

  /*
  *  Public method to set the credentials for the client id and secret
  *
  *  @method setClientSecretCombo
  *  @public
  *  @param {string} clientId
  *  @param {string} clientSecret
  *  @return none
  */
  function setClientSecretCombo(clientId, clientSecret){
     _clientId = clientId;
     _clientSecret = clientSecret;
  } 
  
  /*
  *  Public method to enable client authentication
  *
  *  @method enableClientSecretAuth
  *  @public
  *  @return none
  */
  function enableClientSecretAuth() {
    _authType = CSID;
  }
  
  /*
  *  Public method to enable user authentication
  *
  *  @method enableUserAuth
  *  @public
  *  @return none
  */
  function enableUserAuth(){
    _authType = USER;
  }
  
  /*
  *  Public method to disable authentication
  *
  *  @method enableNoAuth
  *  @public
  *  @return none
  */
  function enableNoAuth(){
    _authType = null;
  }
  
  /*
  *  Public method to run calls against the app endpoint
  *
  *  @method runAppQuery
  *  @public
  *  @params {object} Query - {method, path, jsonObj, params, successCallback, failureCallback}
  *  @return none
  */
  function runAppQuery (Query) {
    var endpoint = "/" + this.getOrganizationName() + "/" + this.getApplicationName() + "/";
    setQueryType(A);
    run(Query, endpoint);
  }

  /*
  *  Public method to run calls against the management endpoint
  *
  *  @method runManagementQuery
  *  @public
  *  @params {object} Query - {method, path, jsonObj, params, successCallback, failureCallback}
  *  @return none
  */
  function runManagementQuery (Query) {
    var endpoint = "/management/";
    setQueryType(M);
    run(Query, endpoint)
  }

  /*
    *  A public method to get the organization name to be used by the client
    *
    *  @method getOrganizationName
    *  @public
    *  @return {string} the organization name
    */
  function getOrganizationName() {
     return _orgName;
  }

  /*
    *  A public method to set the organization name to be used by the client
    *
    *  @method setOrganizationName
    *  @param orgName - the organization name
    *  @return none
    */
  function setOrganizationName(organizationName) {
     _orgName = organizationName;
  }

  /*
  *  A public method to get the application name to be used by the client
  *
  *  @method getApplicationName
  *  @public
  *  @return {string} the application name
  */  
  function getApplicationName() {
     return _appName;
  }

  /*
  *  A public method to set the application name to be used by the client
  *
  *  @method setApplicationName
  *  @public
  *  @param appName - the application name
  *  @return none
  */
  function setApplicationName(applicationName) {
     _appName = applicationName;
  }

  /*
  *  A public method to get current OAuth token
  *
  *  @method getToken
  *  @public
  *  @return {string} the current token
  */
  function getToken() {
     return session.getItem('token');
  }

  /*
  *  A public method to set the current Oauth token
  *
  *  @method setToken
  *  @public
  *  @param token - the bearer token
  *  @return none
  */
  function setToken(token) {
     session.setItem('token', token);
  }

  /*
   *  A public method to return the API URL
   *
   *  @method getApiUrl
   *  @public
   *  @return {string} the API url
   */
  function getApiUrl() {
    return _apiUrl;
  }

  /*
   *  A public method to overide the API url
   *
   *  @method setApiUrl
   *  @public
   *  @return none
   */
  function setApiUrl(apiUrl) {
    _apiUrl = apiUrl;
  }

  /*
   *  A public method to return the call timeout amount
   *
   *  @method getCallTimeout
   *  @public
   *  @return {string} the timeout value (an integer) 30000 = 30 seconds
   */
  function getCallTimeout() {
    return _callTimeout;
  }

  /*
   *  A public method to override the call timeout amount
   *
   *  @method setCallTimeout
   *  @public
   *  @return none
   */
  function setCallTimeout(callTimeout) {
    _callTimeout = callTimeout;
  }

  /*
   * Returns the call timeout callback function
   *
   * @public
   * @method setCallTimeoutCallback
   * @return none
   */ 
  function setCallTimeoutCallback(callback) {
    _callTimeoutCallback = callback; 
  }

  /*
   * Returns the call timeout callback function
   *
   * @public
   * @method getCallTimeoutCallback
   * @return {function} Returns the callTimeoutCallback
   */
  function getCallTimeoutCallback() {
    return _callTimeoutCallback; 
  }

  /*
   * Calls the call timeout callback function
   *
   * @public
   * @method callTimeoutCallback
   * @return {boolean} Returns true or false based on if there was a callback to call
   */
  function callTimeoutCallback(response) {
    if (_callTimeoutCallback && typeof(_callTimeoutCallback) === "function") {
      _callTimeoutCallback(response);
      return true;
    } else {
      return false;
    }
  }

  /*
   *  A public method to get the api url of the reset pasword endpoint
   *
   *  @method getResetPasswordUrl
   *  @public
   *  @return {string} the api rul of the reset password endpoint
   */
  function getResetPasswordUrl() {
    return getApiUrl() + "/management/users/resetpw"
  }

  /*
   *  A public method to get an Entity object for the current logged in user
   *
   *  @method getLoggedInUser
   *  @public
   *  @return {object} user - Entity object of type user
   */
  function getLoggedInUser() {
     var data = JSON.parse(_session.getItem('user'));
     var user = new Entity('user');
     user.set(data);
     return user;
  }

  /*
   *  A public method to set an Entity object for the current logged in user
   *
   *  @method setLoggedInUser
   *  @public
   *  @param {object} user - Entity object of type user
   *  @return none
   */
  function setLoggedInUser(user) {
    var data = null;
    if (user) { data = user.get(); }
    session.setItem('user', JSON.stringify(data));
  }

  /*
  *  A public method to log in an app user - stores the token for later use
  *
  *  @method logInAppUser
  *  @public
  *  @params {string} username
  *  @params {string} password
  *  @params {function} successCallback
  *  @params {function} failureCallback
  *  @return {response} callback functions return API response object
  */
  function logInAppUser (username, password, successCallback, failureCallback) {
    var self = this;
    var data = {"username": username, "password": password, "grant_type": "password"};
    this.runAppQuery(new Query('GET', 'token', null, data,
      function (response) {
        var user = new Entity('users');
        user.set('username', response.user.username);
        user.set('name', response.user.name);
        user.set('email', response.user.email);
        user.set('uuid', response.user.uuid);
        self.setLoggedInUser(user);
        self.setToken(response.access_token);
        if (successCallback && typeof(successCallback) == "function") {
          successCallback(response);
        }
      },
      function (response) {
        if (failureCallback && typeof(failureCallback) == "function") {
          failureCallback(response);
        }
      }
     ));
  }

  /*
   *  TODO:  NOT IMPLEMENTED YET - A method to renew an app user's token
   *  Note: waiting for API implementation
   *  @method renewAppUserToken
   *  @public
   *  @return none
   */
  function renewAppUserToken() {

  }

  /**
   *  A public method to log out an app user - clears all user fields from client
   *
   *  @method logoutAppUser
   *  @public
   *  @return none
   */
  function logoutAppUser() {
    this.setLoggedInUser(null);
    this.setToken(null);
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
  function isLoggedInAppUser() {
    var user = this.getLoggedInUser();
    return (this.getToken() && validation.isUUID(user.get('uuid')));
  }

   /*
   *  A public method to get the logout callback, which is called
   *
   *  when the token is found to be invalid
   *  @method getLogoutCallback
   *  @public
   *  @return {string} the api rul of the reset password endpoint
   */
  function getLogoutCallback() {
    return _logoutCallback;
  }

  /*
   *  A public method to set the logout callback, which is called
   *
   *  when the token is found to be invalid
   *  @method setLogoutCallback
   *  @public
   *  @param {function} logoutCallback
   *  @return none
   */
  function setLogoutCallback(logoutCallback) {
    _logoutCallback = logoutCallback;
  }

  /*
   *  A public method to call the logout callback, which is called
   *
   *  when the token is found to be invalid
   *  @method callLogoutCallback
   *  @public
   *  @return none
   */
  function callLogoutCallback() {
    if (_logoutCallback && typeof(_logoutCallback ) == "function") {
      _logoutCallback();
      return true;
    } else {
      return false;
    }
  }

  /**
   *  Private helper method to encode the query string parameters
   *
   *  @method encodeParams
   *  @public
   *  @params {object} params - an object of name value pairs that will be urlencoded
   *  @return {string} Returns the encoded string
   */
  function encodeParams (params) {
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

  /*
   *  A private method to get the type of the current api call - (Management or Application)
   *
   *  @method getQueryType
   *  @private
   *  @return {string} the call type
   */
  function getQueryType() {
    return _queryType;
  }

  /*
   *  A private method to set the type of the current api call - (Management or Application)
   *
   *  @method setQueryType
   *  @private
   *  @param {string} call type
   *  @return none
   */
  function setQueryType(type) {
    _queryType = type;
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
  function buildCurlCall(Query, endpoint) {
    var curl = 'curl';
    try {
      //peel the data out of the query object
      var method = Query.getMethod().toUpperCase();
      var path = Query.getResource();
      var jsonObj = Query.getJsonObj() || {};
      var params = Query.getQueryParams() || {};
      
      //curl - add the method to the command (no need to add anything for GET)
      if (method == "POST") {curl += " -X POST"; }
      else if (method == "PUT") { curl += " -X PUT"; }
      else if (method == "DELETE") { curl += " -X DELETE"; }
      else { curl += " -X GET"; }
      
      //curl - append the bearer token if this is not the sandbox app
      var application_name = ApiClient.getApplicationName();
      if (application_name) {
        application_name = application_name.toUpperCase();
      }
      if ( _authType == USER && ((application_name != 'SANDBOX' && ApiClient.getToken()) || (getQueryType() == M && ApiClient.getToken())) ) {
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

  /**
   *  A private method to validate, prepare,, and make the calls to the API
   *  Use runAppQuery or runManagementQuery to make your calls!
   *
   *  @method run
   *  @private
   *  @params {object} Query - {method, path, jsonObj, params, successCallback, failureCallback}
   *  @params {string} endpoint - used to differentiate between management and app queries
   *  @return {response} callback functions return API response object
   */
  function run (Query, endpoint) {
    //validate parameters
    try {
      //for timing, call start
      Query.setQueryStartTime();
      
      //peel the data out of the query object
      var method = Query.getMethod().toUpperCase();
      var path = Query.getResource();
      var jsonObj = Query.getJsonObj() || {};
      
      //add the client secret and id to the params if available
      var params = Query.getQueryParams() || {};
      if (_authType == CSID && (_clientId != null && _clientSecret != null)) {
        params['client_id'] = _clientId;
        params['client_secret'] = _clientSecret;
      }     
      //add in the cursor if one is available
      if (Query.getCursor()) {
        params.cursor = Query.getCursor();
      } else {
        delete params.cursor;
      } 
      Query.setQueryParams(params); 

      //method - should be GET, POST, PUT, or DELETE only
      if (method != 'GET' && method != 'POST' && method != 'PUT' && method != 'DELETE') {
        throw(new Error('Invalid method - should be GET, POST, PUT, or DELETE.'));
      }
      
      //params - make sure we have a valid json object
      _params = JSON.stringify(params);
      if (!JSON.parse(_params)) {
        throw(new Error('Params object is not valid.'));
      }
    
      //add the endpoint to the path
      path = '/' + endpoint + '/' + path;

      //make sure path never has more than one / together
      if (path) {
        //regex to strip multiple slashes
        while(path.indexOf('//') != -1){
          path = path.replace('//', '/');
        }
      }
      
      //add in a timestamp for gets and deletes - to avoid caching by the browser
      if ((method == "GET") || (method == "DELETE")) {
        params['_'] = new Date().getTime();
      }
    
      //append params to the path
      var encoded_params = encodeParams(params);
      if (encoded_params) {
        path += "?" + encoded_params;
      }

      //jsonObj - make sure we have a valid json object
      jsonObj = JSON.stringify(jsonObj)
      if (!JSON.parse(jsonObj)) {
        throw(new Error('JSON object is not valid.'));
      }
      //but clear it out if it is empty
      if (jsonObj == '{}') {
        jsonObj = null;
      }

    } catch (e) {
      //parameter was invalid
      console.log('error occured running query -' + e.message);
      return false;
    }
    //build the curl call
    buildCurlCall(Query, endpoint);
    
    //make the call to the API
    var http = require('http');

    //params for the call
    var options = {
      host: ApiClient.getApiUrl(),
      path: path,
      method: method
    };
    
    var headers = {};
    
    //add a header with the oauth token if needed
    if (_authType == USER && ApiClient.getToken()) {
      headers['Authorization'] = "Bearer " + ApiClient.getToken();
    }
    
    //add a header for content type if we have a request body
    if (jsonObj) {
      headers['Content-Type'] = "application/json";
    }
    
    //add the headers
    options['headers'] = headers;
     
    //make the call    
    var response = '';
    var req = http.request(options, function(res) {
      res.setEncoding('utf8');
      //capture the data as it comes back  
      res.on('data', function (chunk) {
        response += chunk;
      });
     
      res.on('end', function () {
        //for timing, call end
        Query.setQueryEndTime();
        //for timing, log the total call time
        console.log(Query.getQueryTotalTime());
        //convert the response to an object if possible
        try {response = JSON.parse(response);} catch (e) {}
        if (res.statusCode != 200) {          
          if ( (response.error == "auth_expired_session_token") ||
               (response.error == "unauthorized")   ||
               (response.error == "auth_missing_credentials")   ||
               (response.error == "auth_invalid")) {
            //this error type means the user is not authorized. If a logout function is defined, call it
            console.log(response.error);
            callLogoutCallback();
          }else {          
            Query.callFailureCallback(response);
          }
        } else {
          //query completed succesfully, so store cursor
          var cursor = response.cursor || null;
          Query.saveCursor(cursor);
          console.log(res.statusCode);
          Query.callSuccessCallback(response); 
        }
      });
    });

    //add the request body if there is one
    if (method == 'POST' || method == 'PUT') {
      if (jsonObj) {
        req.write(jsonObj);
      } 
    }
    
    //deal with any errors (usually network errors) 
    req.on('error', function(e) {
      //for timing, call end
      Query.setQueryEndTime();
      //for timing, log the total call time
      console.log(Query.getQueryTotalTime());
      //clear the timeout
      var response = '';
      console.log('API call error: ' + e.message);
      if (e.code == 'ENOTFOUND') {
        response = 'Could not connect to the API';
      } else {
        response = e.message;
      }
      Query.callFailureCallback(response);
    });
   
    //set up the timeout, just in case the meeting runs long
    req.setTimeout(  ApiClient.getCallTimeout(),
      function() {  
        console.log('API call timed out');
        if (ApiClient.getCallTimeoutCallback() === 'function') {
          ApiClient.callTimeoutCallback('API CALL TIMEOUT');
        } else {
          Query.callFailureCallback('API CALL TIMEOUT');
        } 
      }
    );
    
    //signal the end of the request
    req.end();
  }

  return {
    init:init,
    setClientSecretCombo:setClientSecretCombo,
    enableClientSecretAuth:enableClientSecretAuth,
    enableUserAuth:enableUserAuth,
    runAppQuery:runAppQuery,
    runManagementQuery:runManagementQuery,
    getOrganizationName:getOrganizationName,
    setOrganizationName:setOrganizationName,
    getApplicationName:getApplicationName,
    setApplicationName:setApplicationName,
    getToken:getToken,
    setToken:setToken,
    getCallTimeout:getCallTimeout,
    setCallTimeout:setCallTimeout,
    getCallTimeoutCallback:getCallTimeoutCallback,
    setCallTimeoutCallback:setCallTimeoutCallback,
    callTimeoutCallback:callTimeoutCallback,
    getApiUrl:getApiUrl,
    setApiUrl:setApiUrl,
    getResetPasswordUrl:getResetPasswordUrl,
    getLoggedInUser:getLoggedInUser,
    setLoggedInUser:setLoggedInUser,
    logInAppUser:logInAppUser,
    renewAppUserToken:renewAppUserToken,
    logoutAppUser:logoutAppUser,
    isLoggedInAppUser:isLoggedInAppUser,
    getLogoutCallback:getLogoutCallback,
    setLogoutCallback:setLogoutCallback,
    callLogoutCallback:callLogoutCallback
  }
})();

/**
 * validation is a Singleton that provides methods for validating common field types
 *
 * @class validation
 * @author Rod Simpson (rod@apigee.com)
**/
validation = (function () {

  var usernameRegex = new RegExp("^([0-9a-zA-Z\.\-])+$");
  var nameRegex     = new RegExp("^([0-9a-zA-Z@#$%^&!?;:.,'\"~*-=+_\[\\](){}/\\ |])+$");
  var emailRegex    = new RegExp("^(([0-9a-zA-Z]+[_\+.-]?)+@[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4})+$");
  var passwordRegex = new RegExp("^([0-9a-zA-Z@#$%^&!?<>;:.,'\"~*-=+_\[\\](){}/\\ |])+$");
  var pathRegex     = new RegExp("^([0-9a-z./-])+$");
  var titleRegex    = new RegExp("^([0-9a-zA-Z.!-?/])+$");

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validateUsername
    * @param {string} username - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateUsername(username, failureCallback) {
    if (usernameRegex.test(username) && checkLength(username, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getUsernameAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getUsernameAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getUsernameAllowedChars(){
    return 'Length: min 4, max 80. Allowed: A-Z, a-z, 0-9, dot, and dash';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validateName
    * @param {string} name - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateName(name, failureCallback) {
    if (nameRegex.test(name) && checkLength(name, 4, 16)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getNameAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getNameAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getNameAllowedChars(){
    return 'Length: min 4, max 80. Allowed: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . / ? !';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validatePassword
    * @param {string} password - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validatePassword(password, failureCallback) {
    if (passwordRegex.test(password) && checkLength(password, 5, 16)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getPasswordAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getPasswordAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getPasswordAllowedChars(){
    return 'Length: min 5, max 16. Allowed: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . < > / ? !';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validateEmail
    * @param {string} email - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateEmail(email, failureCallback) {
    if (emailRegex.test(email) && checkLength(email, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getEmailAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getEmailAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getEmailAllowedChars(){
    return 'Email must be in standard form: e.g. example@com';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validatePath
    * @param {string} path - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validatePath(path, failureCallback) {
    if (pathRegex.test(path) && checkLength(path, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getPathAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getPathAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getPathAllowedChars(){
    return 'Length: min 4, max 80. Allowed: /, a-z, 0-9, dot, and dash';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validateTitle
    * @param {string} title - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateTitle(title, failureCallback) {
    if (titleRegex.test(title) && checkLength(title, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getTitleAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getTitleAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getTitleAllowedChars(){
    return 'Length: min 4, max 80. Allowed: space, A-Z, a-z, 0-9, dot, dash, /, !, and ?';
  }

  /**
    * Tests if the string is the correct length
    *
    * @public
    * @method checkLength
    * @param {string} string - The string to test
    * @param {integer} min - the lower bound
    * @param {integer} max - the upper bound
    * @return {boolean} Returns true if string is correct length, false if not
    */
  function checkLength(string, min, max) {
    if (string.length > max || string.length < min) {
      return false;
    }
    return true;
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

  return {
    validateUsername:validateUsername,
    getUsernameAllowedChars:getUsernameAllowedChars,
    validateName:validateName,
    getNameAllowedChars:getNameAllowedChars,
    validatePassword:validatePassword,
    getPasswordAllowedChars:getPasswordAllowedChars,
    validateEmail:validateEmail,
    getEmailAllowedChars:getEmailAllowedChars,
    validatePath:validatePath,
    getPathAllowedChars:getPathAllowedChars,
    validateTitle:validateTitle,
    getTitleAllowedChars:getTitleAllowedChars,
    isUUID:isUUID
  }
})();

var sessionEntity = new Entity('session');
session = {
	
	/**
   *  A public function to return the session id (pulls from the cookie)
   *
   *  @method get_session_id
   *  @public
   *  @params {object} request
   *  @return {string} sessionId
   */
  get_session_id: function(request){
    var cookies = {};
    request.headers.cookie && request.headers.cookie.split(';').forEach(function( cookie ) {
      var parts = cookie.split('=');
      cookies[ parts[ 0 ].trim() ] = ( parts[ 1 ] || '' ).trim();
    });
    //get our coookie
    sessionId = cookies['session'];
    return sessionId;
  },

  /**
   *  A public function to start the session.  Tries to get the session data from the database.
   *  If the session object can be retrieved, use it.  If not, then try to create a new one
   *
   *  @method start_session
   *  @public
   *  @params {object} request
   *  @params {object} response
   *  @params {function} successCallback
   *  @params {function} failureCallback
   *  @return none
   */
  start_session: function(request, response, successCallback, failureCallback) { 
    var sessionId = session.get_session_id(request)  
    if (sessionId) {
      sessionEntity.set('uuid', sessionId);
      sessionEntity.fetch(
        function(output) {
          //got the session so store the value for the cookie
          sessionId = sessionEntity.get('uuid'); 
          console.log("Starting session with id:" + sessionId );
          response.setHeader('Set-Cookie', "session="+sessionId); 
          successCallback(output);            
        },
        function (output) {
          //didn't get one back, so try to make a new one
          sessionEntity.set('uuid',null);//wipe out the previous uuid
          session.save_session(response, successCallback, failureCallback, true);              
        }
      );
    } else {
      //make a new session 
      session.save_session(response, successCallback, failureCallback, true);   
    }
  },

  /**
   *  A public function to save the session.
   *
   *  @method save_session
   *  @public
   *  @params {object} request
   *  @params {object} response
   *  @params {function} successCallback
   *  @params {function} failureCallback
   *  @params {bool} startSession
   *  @return none
   */
  save_session: function (response, successCallback, failureCallback, startSession) {
    sessionEntity.save( 
        function(output) {
          //got the session so store the value for the cookie 
          sessionId = sessionEntity.get('uuid');
          console.log("Saved session with id:" + sessionId );
          if (startSession) {
            response.setHeader('Set-Cookie', "session="+sessionId); 
          }
          successCallback(output);            
        },
        function (output) {
          //failureCallback(output);
        }
      );
  },

  /**
   *  A public function to get a single item from the session.  Pulls from local storage, not the database.
   *
   *  @method getItem
   *  @public
   *  @params {string} key
   *  @return the data for the specified key key
   */
  getItem: function(key) {
    return sessionEntity.get(key);
  },

  /**
   *  A public function to set a single item in the session.  Writes to local storage, not the database. 
   *  Must call save_session to save the session to the database
   *
   *  @method setItem
   *  @public
   *  @params {string} key
   *  @params {data} value
   *  @return none
   */
  setItem: function(key, value) {
    console.log('Adding to sesssion...'+key);
    sessionEntity.set(key, value);  
  },
  removeItem: function(key) {
    sessionEntity.set(key, null);
  }, 
  kill_session: function() {
    sessionEntity.destroy(
      function(output) {
        successCallback();  
      },
      function (output) {
        failureCallback();    
      }    
    );
  },
  garbage_collection: function(successCallback, failureCallback){
    //our goal is to delete any sessions that are older than 24 hours
    //how we calculate our value:
    var minutes=1000*60;
    var hours=minutes*60;
    var one_day=hours*24;
    timestamp  = (new Date()).getTime()- one_day; //24 hours ago
    //we want to delete any sessions older than one day:
    params = {"filter":"modified lt "+timestamp}; 
    ApiClient.runAppQuery(new Query('DELETE', 'sessions', null, params,
      successCallback,
      failureCallback
    ));
  }
};

exports.Query = Query;
exports.Entity = Entity;
exports.Collection = Collection;
exports.ApiClient = ApiClient;
exports.session = session;