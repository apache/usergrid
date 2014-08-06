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
  var groupEndpoint = 'groups/' + this._path;
  var memberEndpoint = 'groups/' + this._path + '/users';

  var groupOptions = {
    method: 'GET',
    endpoint: groupEndpoint
  };

  var memberOptions = {
    method: 'GET',
    endpoint: memberEndpoint
  };

  this._client.request(groupOptions, function(err, response) {
    if (err) {
      if (self._client.logging) {
        console.log('error getting group');
      }
      doCallback(callback, [err, response], self);
    } else {
      var entities = response.getEntities();
      if (entities && entities.length) {
        var groupresponse = entities.shift();
        //self._response = groupresponse || {};
        self._client.request(memberOptions, function(err, response) {
          if (err && self._client.logging) {
            console.log('error getting group users');
          } else {
            self._list = response.getEntities()
              .filter(function(entity) {
                return isUUID(entity.uuid);
              })
              .map(function(entity) {
                return new Usergrid.Entity({
                  type: entity.type,
                  client: self._client,
                  uuid: entity.uuid,
                  response: entity //TODO: deprecate this property
                });
              });
          }
          doCallback(callback, [err, response, self], self);
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
  //doCallback(callback, [null, this._list, this], this);
  return this._list;
};

/*
 *  Adds an existing user to the group, and refreshes the group object.
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
  if (options.user) {
    options = {
      method: "POST",
      endpoint: "groups/" + this._path + "/users/" + options.user.get('username')
    };
    this._client.request(options, function(error, response) {
      if (error) {
        doCallback(callback, [error, response, self], self);
      } else {
        self.fetch(callback);
      }
    });
  } else {
    doCallback(callback, [new UsergridError("no user specified", 'no_user_specified'), null, this], this);
  }
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
  if (options.user) {
    options = {
      method: "DELETE",
      endpoint: "groups/" + this._path + "/users/" + options.user.username
    };
    this._client.request(options, function(error, response) {
      if (error) {
        doCallback(callback, [error, response, self], self);
      } else {
        self.fetch(callback);
      }
    });
  } else {
    doCallback(callback, [new UsergridError("no user specified", 'no_user_specified'), null, this], this);
  }
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
  var options = {
    method: "GET",
    endpoint: "groups/" + this._path + "/feed"
  };
  this._client.request(options, function(err, response) {
    doCallback(callback, [err, response, self], self);
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
  var self = this;
  var user = options.user;
  var entity = new Usergrid.Entity({
    client: this._client,
    data: {
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
      "content": options.content,
      "type": 'groups/' + this._path + '/activities'
    }
  });
  entity.save(function(err, response, entity) {
    doCallback(callback, [err, response, self]);
  });
};
