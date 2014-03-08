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
  var groupEndpoint = 'groups/'+this._path;
  var memberEndpoint = 'groups/'+this._path+'/users';

  var groupOptions = {
    method:'GET',
    endpoint:groupEndpoint
  }

  var memberOptions = {
    method:'GET',
    endpoint:memberEndpoint
  }

  this._client.request(groupOptions, function(err, data){
    if(err) {
      if(self._client.logging) {
        console.log('error getting group');
      }
      doCallback(callback, [err, data], self);
    } else {
      if(data.entities && data.entities.length) {
        var groupData = data.entities[0];
        self._data = groupData || {};
        self._client.request(memberOptions, function(err, data) {
          if(err && self._client.logging) {
            console.log('error getting group users');
          } else {
            if(data.entities) {
              var count = data.entities.length;
              self._list = [];
              for (var i = 0; i < count; i++) {
                var uuid = data.entities[i].uuid;
                if(uuid) {
                  var entityData = data.entities[i] || {};
                  var entityOptions = {
                    type: entityData.type,
                    client: self._client,
                    uuid:uuid,
                    data:entityData
                  };
                  var entity = new Usergrid.Entity(entityOptions);
                  self._list.push(entity);
                }

              }
            }
          }
          doCallback(callback, [err, data, self._list], self);
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
  doCallback(callback, [null, this._list], this);
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
  var options = {
    method:"POST",
    endpoint:"groups/"+this._path+"/users/"+options.user.get('username')
  }

  this._client.request(options, function(error, data){
    if(error) {
      doCallback(callback, [error, data, data.entities], self);
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
    method:"DELETE",
    endpoint:"groups/"+this._path+"/users/"+options.user.get('username')
  }

  this._client.request(options, function(error, data){
    if(error) {
      doCallback(callback, [error, data], self);
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

  var endpoint = "groups/"+this._path+"/feed";

  var options = {
    method:"GET",
    endpoint:endpoint
  }

  this._client.request(options, function(err, data){
    if (err && self.logging) {
      console.log('error trying to log user in');
    }
    doCallback(callback, [err, data, data.entities], self);
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
Usergrid.Group.prototype.createGroupActivity = function(options, callback){
  var user = options.user;
  options = {
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
  }

  var entity = new Usergrid.Entity(options);
  entity.save(function(err, data) {
    doCallback(callback, [err, entity]);
  });
};
