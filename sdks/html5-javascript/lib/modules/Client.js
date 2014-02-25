(function() {
  var name = 'Client', global = this, overwrittenName = global[name], exports;

  Usergrid.Client = function(options) {
    //usergrid endpoint
    this.URI = options.URI || 'https://api.usergrid.com';

    //Find your Orgname and Appname in the Admin portal (http://apigee.com/usergrid)
    if (options.orgName) {
      this.set('orgName', options.orgName);
    }
    if (options.appName) {
      this.set('appName', options.appName);
    }

    //other options
    this.buildCurl = options.buildCurl || false;
    this.logging = options.logging || false;

    //timeout and callbacks
    this._callTimeout =  options.callTimeout || 30000; //default to 30 seconds
    this._callTimeoutCallback =  options.callTimeoutCallback || null;
    this.logoutCallback =  options.logoutCallback || null;
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
  Usergrid.Client.prototype.request = function (options, callback) {
    var self = this;
    var method = options.method || 'GET';
    var endpoint = options.endpoint;
    var body = options.body || {};
    var qs = options.qs || {};
    var mQuery = options.mQuery || false; //is this a query to the management endpoint?
    var orgName = this.get('orgName');
    var appName = this.get('appName');
    var uri;
      var logoutCallback=function(){
          if (typeof(this.logoutCallback) === 'function') {
              return this.logoutCallback(true, 'no_org_or_app_name_specified');
          }
      }.bind(this);
    if(!mQuery && !orgName && !appName){
        return logoutCallback();
    }
    if (mQuery) {
      uri = this.URI + '/' + endpoint;
    } else {
      uri = this.URI + '/' + orgName + '/' + appName + '/' + endpoint;
    }
    if (this.getToken()) {
      qs.access_token = this.getToken();
      /* //could also use headers for the token
       xhr.setRequestHeader("Authorization", "Bearer " + self.getToken());
       xhr.withCredentials = true;
       */
    }
      var req = new Usergrid.Request(method, uri, qs, body, function (err, response) {
          if ([
              "auth_expired_session_token",
              "auth_missing_credentials",
              "auth_unverified_oath",
              "expired_token",
              "unauthorized",
              "auth_invalid"
          ].indexOf(response.error) !== -1) {
              return logoutCallback();
          }
          doCallback(callback, [err, response]);
          //p.done(err, response);
      });
  }

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
    var assetURL = this.URI + '/' + this.orgName + '/' + this.appName + '/assets/' + uuid + '/data';

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
    group.fetch(function(err, data){
      var okToSave = (err && ['service_resource_not_found','no_name_specified','null_pointer'].indexOf(err.name)!==-1) || (!err && getOnExist);
      if (okToSave) {
        group.save(function(err, data){
            doCallback(callback, [err, group, data]);
        });
      } else {
        doCallback(callback, [null, group, data]);
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
    Usergrid.Client.prototype.createEntity = function (options, callback) {
        // todo: replace the check for new / save on not found code with simple save
        // when users PUT on no user fix is in place.
        var getOnExist = options['getOnExist'] || false; //if true, will return entity if one already exists
        delete options['getOnExist']; //so it doesn't become part of our data model
        var entity_data = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(entity_data);
        var self = this;
        entity.fetch(function (err, data) {
            //if the fetch doesn't find what we are looking for, or there is no error, do a save
            var common_errors = ['service_resource_not_found', 'no_name_specified', 'null_pointer'];
            var okToSave = (!err && getOnExist)||(err && err.name && common_errors.indexOf(err.name) !== -1);

            if (okToSave) {
                entity.set(entity_data.data); //add the data again just in case
                entity.save(function (err, data) {
                    doCallback(callback, [err, entity, data]);
                });
            } else {
                doCallback(callback, [null, entity, data]);
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
  Usergrid.Client.prototype.getEntity = function (options, callback) {
    var options = {
      client:this,
      data:options
    }
    var entity = new Usergrid.Entity(options);
    entity.fetch(function(err, data) {
        doCallback(callback, [err, entity, data]);
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
  Usergrid.Client.prototype.restoreEntity = function (serializedObject) {
    var data = JSON.parse(serializedObject);
    var options = {
      client:this,
      data:data
    }
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
  Usergrid.Client.prototype.createCollection = function (options, callback) {
    options.client = this;
    var collection = new Usergrid.Collection(options, function(err, data) {
        doCallback(callback, [err, collection, data]);
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
  Usergrid.Client.prototype.restoreCollection = function (serializedObject) {
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
      endpoint: "users/"+username+"/feed"
    };

    this.request(options, function(err, data){
        if(err) {
            doCallback(callback, [err]);
        } else {
            doCallback(callback, [err, data, data.getEntities()]);
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
  Usergrid.Client.prototype.createUserActivity = function (user, options, callback) {
    options.type = 'users/'+user+'/activities';
    var options = {
      client:this,
      data:options
    }
    var entity = new Usergrid.Entity(options);
    entity.save(function(err, data) {
        doCallback(callback, [err, entity]);
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
        "displayName":username,
        "uuid":user.get("uuid"),
        "username":username,
        "email":user.get("email"),
        "picture":user.get("picture"),
        "image": {
          "duration":0,
          "height":80,
          "url":user.get("picture"),
          "width":80
        }
      },
      "verb":"post",
      "content":content
    };

    this.createUserActivity(username, options, callback);

  };

  /*
   *  A private method to get call timing of last call
   */
  Usergrid.Client.prototype.calcTimeDiff = function () {
    var seconds = 0;
    var time = this._end - this._start;
    try {
      seconds = ((time/10) / 60).toFixed(2);
    } catch(e) {
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
  Usergrid.Client.prototype.setToken = function (token) {
    this.set('token', token);
  };

  /*
   *  A public method to get the OAuth token
   *
   *  @method getToken
   *  @public
   *  @return {string} token
   */
  Usergrid.Client.prototype.getToken = function () {
    return this.get('token');
  };

  Usergrid.Client.prototype.setObject = function(key, value) {
    if (value) {
      value = JSON.stringify(value);
    }
    this.set(key, value);
  };

  Usergrid.Client.prototype.set = function (key, value) {
    var keyStore =  'apigee_' + key;
    this[key] = value;
    if(typeof(Storage)!=="undefined"){
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

  Usergrid.Client.prototype.get = function (key) {
    var keyStore = 'apigee_' + key;
    var value=null;
    if (this[key]) {
      value= this[key];
    } else if(typeof(Storage)!=="undefined") {
      value= localStorage.getItem(keyStore);
    }
    return value  ;
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
      type:"users",
      username:username,
      password:password,
      email:email,
      name:name
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
  Usergrid.Client.prototype.login = function (username, password, callback) {
    var self = this;
    var options = {
      method:'POST',
      endpoint:'token',
      body:{
        username: username,
        password: password,
        grant_type: 'password'
      }
    };
    self.request(options, function(err, data) {
      var user = {};
      if (err) {
        if(self.logging)console.log('error trying to log user in');
      } else {
        var options = {
          client:self,
          data:data.user
        };
        user = new Usergrid.Entity(options);
        self.setToken(data.access_token);
      }
      doCallback(callback, [err, data, user]);
    });
  };


  Usergrid.Client.prototype.reAuthenticateLite = function (callback) {
    var self = this;
    var options = {
      method:'GET',
      endpoint:'management/me',
      mQuery:true
    };
    this.request(options, function(err, response) {
      if (err && self.logging) {
        console.log('error trying to re-authenticate user');
      } else {

        //save the re-authed token and current email/username
          self.setToken(response.data.access_token);

      }
        doCallback(callback, [err]);

    });
  };


  Usergrid.Client.prototype.reAuthenticate = function (email, callback) {
    var self = this;
    var options = {
      method:'GET',
      endpoint:'management/users/'+email,
      mQuery:true
    };
    this.request(options, function(err, response) {
      var organizations = {};
      var applications = {};
      var user = {};
      var data;
      if (err && self.logging) {
        console.log('error trying to full authenticate user');
      } else {
        data = response.data;
        self.setToken(data.token);
        self.set('email', data.email);

        //delete next block and corresponding function when iframes are refactored
        localStorage.setItem('accessToken', data.token);
        localStorage.setItem('userUUID', data.uuid);
        localStorage.setItem('userEmail', data.email);
        //end delete block


        var userData = {
          "username" : data.username,
          "email" : data.email,
          "name" : data.name,
          "uuid" : data.uuid
        };
        var options = {
          client:self,
          data:userData
        };
        user = new Usergrid.Entity(options);

        organizations = data.organizations;
        var org = '';
        try {
          //if we have an org stored, then use that one. Otherwise, use the first one.
          var existingOrg = self.get('orgName');
          org = (organizations[existingOrg])?organizations[existingOrg]:organizations[Object.keys(organizations)[0]];
          self.set('orgName', org.name);
        } catch(e) {
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
      doCallback(callback, [err, data, user, organizations, applications], self);
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
        var options = {
          client: self,
          data: data.user
        }
        user = new Usergrid.Entity(options);
        self.setToken(data.access_token);
      }
      doCallback(callback, [err, data, user], self);
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
  Usergrid.Client.prototype.getLoggedInUser = function (callback) {
    if (!this.getToken()) {
      callback(true, null, null);
    } else {
      var self = this;
      var options = {
        method:'GET',
        endpoint:'users/me'
      };
      this.request(options, function(err, data) {
        if (err) {
          if (self.logging) {
            console.log('error trying to log user in');
          }
          doCallback(callback, [err, data, null], self);
        } else {
          var options = {
            client:self,
            data:data.entities[0]
          };
          var user = new Usergrid.Entity(options);
          doCallback(callback, [null, data, user], self);
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
  Usergrid.Client.prototype.isLoggedIn = function () {
      var token=this.getToken();
    return "undefined" !== typeof token && token!==null;
  };

  /*
   *  A public method to log out an app user - clears all user fields from client
   *
   *  @method logout
   *  @public
   *  @return none
   */
  Usergrid.Client.prototype.logout = function () {
    this.setToken();
  };

  /*
   *  A private method to build the curl call to display on the command line
   *
   *  @method buildCurlCall
   *  @private
   *  @param {object} options
   *  @return {string} curl
   */
  Usergrid.Client.prototype.buildCurlCall = function (options) {
    var curl = ['curl'];
    var method = (options.method || 'GET').toUpperCase();
    var body = options.body;
    var uri = options.uri;

    //curl - add the method to the command (no need to add anything for GET)
    curl.push('-X');
    curl.push((['POST','PUT','DELETE'].indexOf(method)>=0)?method:'GET');

    //curl - append the path
    curl.push(uri);

    if("object"===typeof body && Object.keys(body).length>0 && ['POST','PUT'].indexOf(method)!==-1){
      curl.push('-d');
      curl.push("'"+JSON.stringify(body)+"'");
    }
    curl=curl.join(' ');
    //log the curl command to the console
    console.log(curl);

    return curl;
  }

  Usergrid.Client.prototype.getDisplayImage = function (email, picture, size) {
      size = size || 50;
      var image='https://apigee.com/usergrid/images/user_profile.png';
    try {
      if (picture) {
        image= picture;
      }else if (email.length) {
        image= 'https://secure.gravatar.com/avatar/' + MD5(email) + '?s=' + size + encodeURI("&d=https://apigee.com/usergrid/images/user_profile.png");
      }
    } catch(e) {
      //TODO do something with this error?
    }finally{
      return image;
    }
  }
  global[name] =  Usergrid.Client;
  global[name].noConflict = function() {
    if(overwrittenName){
      global[name] = overwrittenName;
    }
    return exports;
  };
  return global[name];
}());

