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
Usergrid.Entity.prototype.serialize = function () {
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
Usergrid.Entity.prototype.get = function (field) {
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
Usergrid.Entity.prototype.save = function (callback) {
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
    var password = this.get('password');
    var oldpassword = this.get('oldpassword');
    var newpassword = this.get('newpassword');
  //remove system specific properties
  for (var item in entityData) {
    if (item === 'metadata' || item === 'created' || item === 'modified' ||
          item === 'oldpassword' || item === 'newpassword' || //old and new pw not added to data
      item === 'type' || item === 'activated' || item === 'uuid') {
      continue;
    }
    data[item] = entityData[item];
  }
  var options =  {
    method:method,
    endpoint:type,
    body:data
  };
  //save the entity first
  this._client.request(options, function (err, retdata) {
      //clear out pw info if present
      self.set('password', null);
      self.set('oldpassword', null);
      self.set('newpassword', null);
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
          var path = retdata.path;
          //for connections, API returns type
          while (path.substring(0, 1) === "/") {
            path = path.substring(1);
          }
          self.set('type', path);
        }
      }
      //if this is a user, update the password if it has been specified;
        var needPasswordChange = ((self.get('type') === 'user' || self.get('type') === 'users') && oldpassword && newpassword);
      if (needPasswordChange) {
        //Note: we have a ticket in to change PUT calls to /users to accept the password change
        //      once that is done, we will remove this call and merge it all into one
        var pwdata = {};
          pwdata.oldpassword = oldpassword;
          pwdata.newpassword = newpassword;
        var options = {
          method:'PUT',
          endpoint:type+'/password',
          body:pwdata
        }
        self._client.request(options, function (err, data) {
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
};

/*
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

  //Check for an entity type, then if a uuid is available, use that, otherwise, use the name
  try {
    if (type === undefined) {
      throw 'cannot fetch entity, no entity type specified'
    } else if (this.get('uuid')) {
      type += '/' + this.get('uuid');
    } else if (type === 'users' && this.get('username')) {
      type += '/' + this.get('username');
    } else if (this.get('name')) {
      type += '/' + encodeURIComponent(this.get('name'));
    } else if (typeof(callback) === 'function') {
      throw 'no_name_specified';
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
    method:'GET',
    endpoint:type
  };
  this._client.request(options, function (err, data) {
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
Usergrid.Entity.prototype.destroy = function (callback) {
  var self = this;
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
Usergrid.Entity.prototype.connect = function (connection, entity, callback) {

  var self = this;

  var error;
  //connectee info
  var connecteeType = entity.get('type');
  var connectee = this.getEntityId(entity);
  if (!connectee) {
    if (typeof(callback) === 'function') {
      error = 'Error trying to delete object - no uuid specified.';
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
      error = 'Error in connect - no uuid specified.';
      if (self._client.logging) {
        console.log(error);
      }
      callback(true, error);
    }
    return;
  }

  var endpoint = connectorType + '/' + connector + '/' + connection + '/' + connecteeType + '/' + connectee;
  var options = {
    method:'POST',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('entity could not be connected');
    }
    if (typeof(callback) === 'function') {
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
Usergrid.Entity.prototype.getEntityId = function (entity) {
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
Usergrid.Entity.prototype.getConnections = function (connection, callback) {

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
    method:'GET',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('entity could not be connected');
    }

    self[connection] = {};

    var length = data.entities.length;
    for (var i = 0; i < length; i++) {
      if (data.entities[i].type === 'user'){
        self[connection][data.entities[i].username] = data.entities[i];
      } else {
        self[connection][data.entities[i].name] = data.entities[i]
      }
    }

    if (typeof(callback) === 'function') {
      callback(err, data, data.entities);
    }
  });

};

Usergrid.Entity.prototype.getGroups = function (callback) {

  var self = this;

  var endpoint = 'users' + '/' + this.get('uuid') + '/groups' ;
  var options = {
    method:'GET',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('entity could not be connected');
    }

    self.groups = data.entities;

    if (typeof(callback) === 'function') {
      callback(err, data, data.entities);
    }
  });

};

Usergrid.Entity.prototype.getActivities = function (callback) {

  var self = this;

  var endpoint = this.get('type') + '/' + this.get('uuid') + '/activities' ;
  var options = {
    method:'GET',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('entity could not be connected');
    }

    for (var entity in data.entities) {
      data.entities[entity].createdDate = (new Date(data.entities[entity].created)).toUTCString();
    }

    self.activities = data.entities;

    if (typeof(callback) === 'function') {
      callback(err, data, data.entities);
    }
  });

};

Usergrid.Entity.prototype.getFollowing = function (callback) {

  var self = this;

  var endpoint = 'users' + '/' + this.get('uuid') + '/following' ;
  var options = {
    method:'GET',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('could not get user following');
    }

    for (var entity in data.entities) {
      data.entities[entity].createdDate = (new Date(data.entities[entity].created)).toUTCString();
      var image = self._client.getDisplayImage(data.entities[entity].email, data.entities[entity].picture);
      data.entities[entity]._portal_image_icon =  image;
    }

    self.following = data.entities;

    if (typeof(callback) === 'function') {
      callback(err, data, data.entities);
    }
  });

};


Usergrid.Entity.prototype.getFollowers = function (callback) {

  var self = this;

  var endpoint = 'users' + '/' + this.get('uuid') + '/followers' ;
  var options = {
    method:'GET',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('could not get user followers');
    }

    for (var entity in data.entities) {
      data.entities[entity].createdDate = (new Date(data.entities[entity].created)).toUTCString();
      var image = self._client.getDisplayImage(data.entities[entity].email, data.entities[entity].picture);
      data.entities[entity]._portal_image_icon =  image;
    }

    self.followers = data.entities;

    if (typeof(callback) === 'function') {
      callback(err, data, data.entities);
    }
  });

};

Usergrid.Entity.prototype.getRoles = function (callback) {

  var self = this;

  var endpoint = this.get('type') + '/' + this.get('uuid') + '/roles' ;
  var options = {
    method:'GET',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('could not get user roles');
    }

    self.roles = data.entities;

    if (typeof(callback) === 'function') {
      callback(err, data, data.entities);
    }
  });

};

Usergrid.Entity.prototype.getPermissions = function (callback) {

  var self = this;

  var endpoint = this.get('type') + '/' + this.get('uuid') + '/permissions' ;
  var options = {
    method:'GET',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
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
        ops_object.get = 'no';
        ops_object.post = 'no';
        ops_object.put = 'no';
        ops_object.delete = 'no';
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

    self.permissions = permissions;

    if (typeof(callback) === 'function') {
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
Usergrid.Entity.prototype.disconnect = function (connection, entity, callback) {

  var self = this;

  var error;
  //connectee info
  var connecteeType = entity.get('type');
  var connectee = this.getEntityId(entity);
  if (!connectee) {
    if (typeof(callback) === 'function') {
      error = 'Error trying to delete object - no uuid specified.';
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
      error = 'Error in connect - no uuid specified.';
      if (self._client.logging) {
        console.log(error);
      }
      callback(true, error);
    }
    return;
  }

  var endpoint = connectorType + '/' + connector + '/' + connection + '/' + connecteeType + '/' + connectee;
  var options = {
    method:'DELETE',
    endpoint:endpoint
  };
  this._client.request(options, function (err, data) {
    if (err && self._client.logging) {
      console.log('entity could not be disconnected');
    }
    if (typeof(callback) === 'function') {
      callback(err, data);
    }
  });
};
