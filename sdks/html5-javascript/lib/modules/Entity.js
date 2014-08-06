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


var ENTITY_SYSTEM_PROPERTIES = ['metadata', 'created', 'modified', 'oldpassword', 'newpassword', 'type', 'activated', 'uuid'];

/*
 *  A class to Model a Usergrid Entity.
 *  Set the type and uuid of entity in the 'data' json object
 *
 *  @constructor
 *  @param {object} options {client:client, data:{'type':'collection_type', uuid:'uuid', 'key':'value'}}
 */
Usergrid.Entity = function(options) {
  this._data={};
  this._client=undefined;
  if (options) {
    //this._data = options.data || {};
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
  return (obj && obj instanceof Usergrid.Entity);
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
  return (isEntity(obj) && isUUID(obj.get('uuid')));
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
};

Usergrid.Entity.prototype.getEndpoint = function() {
  var type = this.get('type'),
    nameProperties = ['uuid', 'name'],
    name;
  if (type === undefined) {
    throw new UsergridError('cannot fetch entity, no entity type specified', 'no_type_specified');
  } else if (/^users?$/.test(type)) {
    nameProperties.unshift('username');
  }
  name = this.get(nameProperties)
    .filter(function(x) {
      return (x !== null && "undefined" !== typeof x);
    })
    .shift();
  return (name) ? [type, name].join('/') : type;
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
  var self = this,
    type = this.get('type'),
    method = 'POST',
    entityId = this.get("uuid"),
    changePassword,
    entityData = this.get(),
    options = {
      method: method,
      endpoint: type
    };
  //update the entity if the UUID is present
  if (entityId) {
    options.method = 'PUT';
    options.endpoint += '/' + entityId;
  }
  //remove system-specific properties
  options.body = Object.keys(entityData)
    .filter(function(key) {
      return (ENTITY_SYSTEM_PROPERTIES.indexOf(key) === -1);
    })
    .reduce(function(data, key) {
      data[key] = entityData[key];
      return data;
    }, {});
  self._client.request(options, function(err, response) {
    var entity = response.getEntity();
    if (entity) {
      self.set(entity);
      self.set('type', (/^\//.test(response.path)) ? response.path.substring(1) : response.path);
    }
    if (err && self._client.logging) {
      console.log('could not save entity');
    }
    doCallback(callback, [err, response, self], self);
  });
};

/*
 *
 * Updates the user's password
 */
Usergrid.Entity.prototype.changePassword = function(oldpassword, newpassword, callback) {
  //Note: we have a ticket in to change PUT calls to /users to accept the password change
  //      once that is done, we will remove this call and merge it all into one
  var self = this;
  if ("function" === typeof oldpassword && callback === undefined) {
    callback = oldpassword;
    oldpassword = self.get("oldpassword");
    newpassword = self.get("newpassword");
  }
  //clear out pw info if present
  self.set({
    'password': null,
    'oldpassword': null,
    'newpassword': null
  });
  if ((/^users?$/.test(self.get('type'))) && oldpassword && newpassword) {
    var options = {
      method: 'PUT',
      endpoint: 'users/' + self.get("uuid") + '/password',
      body: {
        uuid: self.get("uuid"),
        username: self.get("username"),
        oldpassword: oldpassword,
        newpassword: newpassword
      }
    };
    self._client.request(options, function(err, response) {
      if (err && self._client.logging) {
        console.log('could not update user');
      }
      //remove old and new password fields so they don't end up as part of the entity object
      doCallback(callback, [err, response, self], self);
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
    method: 'GET',
    endpoint: endpoint
  };
  this._client.request(options, function(err, response) {
    var entity = response.getEntity();
    if (entity) {
      self.set(entity);
    }
    doCallback(callback, [err, response, self], self);
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
    method: 'DELETE',
    endpoint: endpoint
  };
  this._client.request(options, function(err, response) {
    if (!err) {
      self.set(null);
    }
    doCallback(callback, [err, response, self], self);
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
  if (['POST', 'DELETE'].indexOf(method.toUpperCase()) == -1) {
    throw new UsergridInvalidArgumentError("invalid method for connection call. must be 'POST' or 'DELETE'");
  }
  //connectee info
  var connecteeType = entity.get('type');
  var connectee = this.getEntityId(entity);
  if (!connectee) {
    throw new UsergridInvalidArgumentError("connectee could not be identified");
  }

  //connector info
  var connectorType = this.get('type');
  var connector = this.getEntityId(this);
  if (!connector) {
    throw new UsergridInvalidArgumentError("connector could not be identified");
  }

  var endpoint = [connectorType, connector, connection, connecteeType, connectee].join('/');
  var options = {
    method: method,
    endpoint: endpoint
  };
  this._client.request(options, function(err, response) {
    if (err && self._client.logging) {
      console.log('There was an error with the connection call');
    }
    doCallback(callback, [err, response, self], self);
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
    } else  {
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

  //connector info
  var connectorType = this.get('type');
  var connector = this.getEntityId(this);
  if (!connector) {
    if (typeof(callback) === 'function') {
      var error = 'Error in getConnections - no uuid specified.';
      if (self._client.logging) {
        console.log(error);
      }
      doCallback(callback, [true, error], self);
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

    var length = (data && data.entities) ? data.entities.length : 0;
    for (var i = 0; i < length; i++) {
      if (data.entities[i].type === 'user') {
        self[connection][data.entities[i].username] = data.entities[i];
      } else {
        self[connection][data.entities[i].name] = data.entities[i];
      }
    }

    doCallback(callback, [err, data, data.entities], self);
  });

};

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

    self.groups = data.entities;

    doCallback(callback, [err, data, data.entities], self);
  });

};

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

    for (var entity in data.entities) {
      data.entities[entity].createdDate = (new Date(data.entities[entity].created)).toUTCString();
    }

    self.activities = data.entities;

    doCallback(callback, [err, data, data.entities], self);
  });

};

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

    for (var entity in data.entities) {
      data.entities[entity].createdDate = (new Date(data.entities[entity].created)).toUTCString();
      var image = self._client.getDisplayImage(data.entities[entity].email, data.entities[entity].picture);
      data.entities[entity]._portal_image_icon = image;
    }

    self.following = data.entities;

    doCallback(callback, [err, data, data.entities], self);
  });

};


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

    for (var entity in data.entities) {
      data.entities[entity].createdDate = (new Date(data.entities[entity].created)).toUTCString();
      var image = self._client.getDisplayImage(data.entities[entity].email, data.entities[entity].picture);
      data.entities[entity]._portal_image_icon = image;
    }

    self.followers = data.entities;

    doCallback(callback, [err, data, data.entities], self);
  });

};

Usergrid.Client.prototype.createRole = function(roleName, permissions, callback) {
    
    var options = {
        type: 'role',
        name: roleName        
    };

    this.createEntity(options, function(err, response, entity) {
        if (err) {
            doCallback(callback, [ err, response, self ]);    
        } else {
            entity.assignPermissions(permissions, function (err, data) {
                if (err) {
                    doCallback(callback, [ err, response, self ]);    
                } else {
                    doCallback(callback, [ err, data, data.data ], self);
                }
            })
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
    var type = self.get('type');
    var collection = type + 's';
    var entityID;

    if (type == 'user' && this.get('username') != null) {
        entityID = self.get('username');
    } else if (type == 'group' && this.get('name') != null) {
        entityID = self.get('name');
    } else if (this.get('uuid') != null) {
        entityID = self.get('uuid');
    }

    if (type != 'users' && type != 'groups') {
        doCallback(callback, [ new UsergridError('entity must be a group or user', 'invalid_entity_type'), null, this ], this);
    }

    var endpoint = 'roles/' + roleName + '/' + collection + '/' + entityID;
    var options = {
        method: 'POST',
        endpoint: endpoint        
    };

    this._client.request(options, function(err, response) {
        if (err) {
            console.log('Could not assign role.');
        }        
        doCallback(callback, [ err, response, self ]);
    });

};

Usergrid.Entity.prototype.removeRole = function(roleName, callback) {
    
    var self = this;
    var type = self.get('type');
    var collection = type + 's';
    var entityID;

    if (type == 'user' && this.get('username') != null) {
        entityID = this.get('username');
    } else if (type == 'group' && this.get('name') != null) {
        entityID = this.get('name');
    } else if (this.get('uuid') != null) {
        entityID = this.get('uuid');
    }

    if (type != 'users' && type != 'groups') {
        doCallback(callback, [ new UsergridError('entity must be a group or user', 'invalid_entity_type'), null, this ], this);
    }

    var endpoint = 'roles/' + roleName + '/' + collection + '/' + entityID;
    var options = {
        method: 'DELETE',
        endpoint: endpoint        
    };

    this._client.request(options, function(err, response) {
        if (err) {
            console.log('Could not assign role.');
        }        
        doCallback(callback, [ err, response, self ]);
    });

};

Usergrid.Entity.prototype.assignPermissions = function(permissions, callback) {
    var self = this;
    var entityID;
    var type = this.get('type');

    if (type != 'user' && type != 'users' && type != 'group' && type != 'groups' && type != 'role' && type != 'roles') {
        doCallback(callback, [ new UsergridError('entity must be a group, user, or role', 'invalid_entity_type'), null, this ], this);
    }

    if (type == 'user' && this.get('username') != null) {
        entityID = this.get('username');
    } else if (type == 'group' && this.get('name') != null) {
        entityID = this.get('name');
    } else if (this.get('uuid') != null) {
        entityID = this.get('uuid');
    }

    var endpoint = type + '/' + entityID + '/permissions';
    var options = {
        method: 'POST',
        endpoint: endpoint,
        body: {
            'permission': permissions
        }
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log('could not assign permissions');
        }
        doCallback(callback, [ err, data, data.data ], self);
    });
};

Usergrid.Entity.prototype.removePermissions = function(permissions, callback) {
    var self = this;
    var entityID;
    var type = this.get('type');

    if (type != 'user' && type != 'users' && type != 'group' && type != 'groups' && type != 'role' && type != 'roles') {
        doCallback(callback, [ new UsergridError('entity must be a group, user, or role', 'invalid_entity_type'), null, this ], this);
    }

    if (type == 'user' && this.get('username') != null) {
        entityID = this.get('username');
    } else if (type == 'group' && this.get('name') != null) {
        entityID = this.get('name');
    } else if (this.get('uuid') != null) {
        entityID = this.get('uuid');
    }

    var endpoint = type + '/' + entityID + '/permissions';
    var options = {
        method: 'DELETE',
        endpoint: endpoint,
        qs: {
            'permission': permissions
        }
    };
    this._client.request(options, function(err, data) {
        if (err && self._client.logging) {
            console.log('could not remove permissions');
        }
        doCallback(callback, [ err, data, data.params.permission ], self);
    });
};

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

        ops_part=ops_part.replace("*", "get,post,put,delete");
        var ops = ops_part.split(',');
        var ops_object = {};
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

    doCallback(callback, [err, data, data.entities], self);
  });

};
