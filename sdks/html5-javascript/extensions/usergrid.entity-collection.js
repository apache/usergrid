
/**
 *  A class to Model a Usergrid Entity.
 *
 *  @class Entity
 *  @author Rod Simpson (rod@apigee.com)
 */
(function () {
  /**
   *  Constructor for initializing an entity
   *
   *  @constructor
   *  @param {string} collectionType - the type of collection to model
   *  @param {uuid} uuid - (optional), the UUID of the collection if it is known
   */
  Usergrid.Entity = function(collectionType, uuid) {
    this._collectionType = collectionType;
    this._data = {};
    if (uuid) {
      this._data['uuid'] = uuid;
    }
  };

  //inherit prototype from Query
  Usergrid.Entity.prototype = new Usergrid.Query();

  /**
   *  gets the current Entity type
   *
   *  @method getCollectionType
   *  @return {string} collection type
   */
  Usergrid.Entity.prototype.getCollectionType = function (){
    return this._collectionType;
  }

  /**
   *  sets the current Entity type
   *
   *  @method setCollectionType
   *  @param {string} collectionType
   *  @return none
   */
  Usergrid.Entity.prototype.setCollectionType = function (collectionType){
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
  Usergrid.Entity.prototype.get = function (field){
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
  Usergrid.Entity.prototype.set = function (item, value){
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
  Usergrid.Entity.prototype.save = function (successCallback, errorCallback){
    var path = this.getCollectionType();
    //TODO:  API will be changed soon to accomodate PUTs via name which create new entities
    //       This function should be changed to PUT only at that time, and updated to use
    //       either uuid or name
    var method = 'POST';
    if (this.isUUID(this.get('uuid'))) {
      method = 'PUT';
      path += "/" + this.get('uuid');
    }

    //if this is a user, update the password if it has been specified
    var data = {};
    if (path == 'users') {
      data = this.get();
      var pwdata = {};
      //Note: we have a ticket in to change PUT calls to /users to accept the password change
      //      once that is done, we will remove this call and merge it all into one
      if (data.oldpassword && data.newpassword) {
        pwdata.oldpassword = data.oldpassword;
        pwdata.newpassword = data.newpassword;
        this.runAppQuery(new Usergrid.Query('PUT', 'users/'+uuid+'/password', pwdata, null,
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
      if (item == 'metadata' || item == 'created' || item == 'modified' ||
          item == 'type' || item == 'activatted' ) { continue; }
      data[item] = entityData[item];
    }

    this.setAllQueryParams(method, path, data, null,
      function(response) {
        try {
          var entity = response.entities[0];
          self.set(entity);
          if (typeof(successCallback) === "function"){
            successCallback(response);
          }
        } catch (e) {
          if (typeof(errorCallback) === "function"){
            errorCallback(response);
          }
        }
      },
      function(response) {
        if (typeof(errorCallback) === "function"){
          errorCallback(response);
        }
      }
    );
    Usergrid.ApiClient.runAppQuery(this);
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
  Usergrid.Entity.prototype.fetch = function (successCallback, errorCallback){
    var path = this.getCollectionType();
    //if a uuid is available, use that, otherwise, use the name
    if (this.isUUID(this.get('uuid'))) {
      path += "/" + this.get('uuid');
    } else {
      if (path == "users") {
        if (this.get("username")) {
          path += "/" + this.get("username");
        } else {
          console.log('no username specified');
          if (typeof(errorCallback) === "function"){
            console.log('no username specified');
          }
        }
      } else {
        if (this.get()) {
          path += "/" + this.get();
        } else {
          console.log('no entity identifier specified');
          if (typeof(errorCallback) === "function"){
            console.log('no entity identifier specified');
          }
        }
      }
    }
    var self = this;
    this.setAllQueryParams('GET', path, null, null,
      function(response) {
        if (response.user) {
          self.set(response.user);
        } else if (response.entities[0]){
          self.set(response.entities[0]);
        }
        if (typeof(successCallback) === "function"){
          successCallback(response);
        }
      },
      function(response) {
        if (typeof(errorCallback) === "function"){
            errorCallback(response);
        }
      }
    );
    Usergrid.ApiClient.runAppQuery(this);
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
  Usergrid.Entity.prototype.destroy = function (successCallback, errorCallback){
    var path = this.getCollectionType();
    if (this.isUUID(this.get('uuid'))) {
      path += "/" + this.get('uuid');
    } else {
      console.log('Error trying to delete object - no uuid specified.');
      if (typeof(errorCallback) === "function"){
        errorCallback('Error trying to delete object - no uuid specified.');
      }
    }
    var self = this;
    this.setAllQueryParams('DELETE', path, null, null,
      function(response) {
        //clear out this object
        self.set(null);
        if (typeof(successCallback) === "function"){
          successCallback(response);
        }
      },
      function(response) {
        if (typeof(errorCallback) === "function"){
            errorCallback(response);
        }
      }
    );
    Usergrid.ApiClient.runAppQuery(this);
  }

})(Usergrid);


/**
 *  The Collection class models Usergrid Collections.  It essentially
 *  acts as a container for holding Entity objects, while providing
 *  additional funcitonality such as paging, and saving
 *
 *  @class Collection
 *  @author Rod Simpson (rod@apigee.com)
 */
(function () {
  /**
   *  Collection is a container class for holding entities
   *
   *  @constructor
   *  @param {string} collectionPath - the type of collection to model
   *  @param {uuid} uuid - (optional), the UUID of the collection if it is known
   */
  Usergrid.Collection = function(path, uuid) {
    this._path = path;
    this._uuid = uuid;
    this._list = [];
    this._Query = new Usergrid.Query();
    this._iterator = -1; //first thing we do is increment, so set to -1
  };

  Usergrid.Collection.prototype = new Usergrid.Query();

  /**
   *  gets the current Collection path
   *
   *  @method getPath
   *  @return {string} path
   */
  Usergrid.Collection.prototype.getPath = function (){
    return this._path;
  }

  /**
   *  sets the Collection path
   *
   *  @method setPath
   *  @param {string} path
   *  @return none
   */
  Usergrid.Collection.prototype.setPath = function (path){
    this._path = path;
  }

  /**
   *  gets the current Collection UUID
   *
   *  @method getUUID
   *  @return {string} the uuid
   */
  Usergrid.Collection.prototype.getUUID = function (){
    return this._uuid;
  }

  /**
   *  sets the current Collection UUID
   *  @method setUUID
   *  @param {string} uuid
   *  @return none
   */
  Usergrid.Collection.prototype.setUUID = function (uuid){
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
  Usergrid.Collection.prototype.addEntity = function (entity){
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
  Usergrid.Collection.prototype.addNewEntity = function (entity, successCallback, errorCallback) {
    //add the entity to the list
    this.addEntity(entity);
    //then save the entity
    entity.save(successCallback, errorCallback);
  }

  Usergrid.Collection.prototype.destroyEntity = function (entity) {
    //first get the entities uuid
    var uuid = entity.get('uuid');
    //if the entity has a uuid, delete it
    if (uuid) {
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
  Usergrid.Collection.prototype.getEntityByField = function (field, value){
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
  Usergrid.Collection.prototype.getEntityByUUID = function (UUID){
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
  Usergrid.Collection.prototype.getFirstEntity = function (){
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
  Usergrid.Collection.prototype.getLastEntity = function (){
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
  Usergrid.Collection.prototype.hasNextEntity = function (){
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
  Usergrid.Collection.prototype.getNextEntity = function (){
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
  Usergrid.Collection.prototype.hasPreviousEntity = function (){
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
  Usergrid.Collection.prototype.getPreviousEntity = function (){
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
  Usergrid.Collection.prototype.resetEntityPointer = function (){
     this._iterator  = -1;
  }

  /**
   *  gets and array of all entities currently in the colleciton object
   *
   *  @method getEntityList
   *  @return {array} returns an array of entity objects
   */
  Usergrid.Collection.prototype.getEntityList = function (){
     return this._list;
  }

  /**
   *  sets the entity list
   *
   *  @method setEntityList
   *  @param {array} list - an array of Entity objects
   *  @return none
   */
  Usergrid.Collection.prototype.setEntityList = function (list){
    this._list = list;
  }

  /**
   *  Paging -  checks to see if there is a next page od data
   *
   *  @method hasNextPage
   *  @return {boolean} returns true if there is a next page of data, false otherwise
   */
  Usergrid.Collection.prototype.hasNextPage = function (){
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
  Usergrid.Collection.prototype.getNextPage = function (){
    if (this.hasNext()) {
        //set the cursor to the next page of data
        this.getNext();
        //empty the list
        this.setEntityList([]);
        Usergrid.ApiClient.runAppQuery(this);
      }
  }

  /**
   *  Paging -  checks to see if there is a previous page od data
   *
   *  @method hasPreviousPage
   *  @return {boolean} returns true if there is a previous page of data, false otherwise
   */
  Usergrid.Collection.prototype.hasPreviousPage = function (){
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
  Usergrid.Collection.prototype.getPreviousPage = function (){
    if (this.hasPrevious()) {
        this.getPrevious();
        //empty the list
        this.setEntityList([]);
        Usergrid.ApiClient.runAppQuery(this);
      }
  }

  /**
   *  clears the query parameters object
   *
   *  @method clearQuery
   *  @return none
   */
  Usergrid.Collection.prototype.clearQuery = function (){
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
  Usergrid.Collection.prototype.fetch = function (successCallback, errorCallback){
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
              var entity = new Usergrid.Entity(self.getPath(), uuid);
              //store the data in the entity
              var data = response.entities[i] || {};
              delete data.uuid; //remove uuid from the object
              entity.set(data);
              //store the new entity in this collection
              self.addEntity(entity);
            }
          }
          if (typeof(successCallback) === "function"){
            successCallback(response);
          }
        } else {
          if (typeof(errorCallback) === "function"){
              errorCallback(response);
          }
        }
      },
      function(response) {
        if (typeof(errorCallback) === "function"){
            errorCallback(response);
        }
      }
    );
    Usergrid.ApiClient.runAppQuery(this);
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
  Usergrid.Collection.prototype.save = function (successCallback, errorCallback){
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
    Usergrid.ApiClient.runAppQuery(this);
  }
})(Usergrid);

