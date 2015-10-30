/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
'use strict';

AppServices.Services.factory('ug', function(configuration, $rootScope, utility,
  $q, $http, $resource, $log, $location) {

  var requestTimes = [],
    running = false,
    currentRequests = {};

  function reportError(data, config) {
    console.error(data)
  };
  var getAccessToken = function() {
    return sessionStorage.getItem('accessToken');
  };

  return {
    get: function(prop, isObject) {
      return isObject ? this.client().getObject(prop) : this.client().get(
        prop);
    },
    set: function(prop, value) {
      this.client().set(prop, value);

    },
    getUrls: function(qs) {
      var host = $location.host();
      var BASE_URL = '';
      var DATA_URL = '';
      var use_sso = false;
      switch (true) {
        case host === 'usergrid.dev':
          //development
          DATA_URL = 'https://api.usergrid.com';
          break;
        default:
          DATA_URL = Usergrid.overrideUrl;
          break;
      }
      //override with querystring
      DATA_URL = qs['api_url'] || DATA_URL;
      DATA_URL = DATA_URL.lastIndexOf('/') === DATA_URL.length - 1 ? DATA_URL.substring(0,DATA_URL.length-1) : DATA_URL;
      return {
        DATA_URL: DATA_URL,
        LOGIN_URL: BASE_URL + '/accounts/sign_in',
        PROFILE_URL: BASE_URL + '/accounts/my_account',
        LOGOUT_URL: BASE_URL + '/accounts/sign_out',
        apiUrl: DATA_URL,
        use_sso: use_sso
      };
    },
    orgLogin: function(username, password) {
      var self = this;
      this.client().set('email', username);
      this.client().set('token', null);
      this.client().orgLogin(username, password, function(err, data, user,
        organizations, applications) {
        if (err) {
          $rootScope.$broadcast('loginFailed', err, data);
        } else {
          self.initializeCurrentUser(function() {
            $rootScope.$broadcast('loginSuccesful', user, organizations,
              applications);
          });
        }
      });
    },
    checkAuthentication: function(force) {
      var ug = this;
      var client = ug.client();

      var initialize = function() {
          ug.initializeCurrentUser(function() {
            $rootScope.userEmail = client.get('email');
            $rootScope.organizations = client.getObject('organizations');
            $rootScope.applications = client.getObject('applications');
            $rootScope.currentOrg = client.get('orgName');
            $rootScope.currentApp = client.get('appName');
            var size = 0,
              key;
            for (key in $rootScope.applications) {
              if ($rootScope.applications.hasOwnProperty(key)) size++;
            }
            $rootScope.$broadcast('checkAuthentication-success', client.getObject(
              'organizations'), client.getObject('applications'), client.get(
              'orgName'), client.get('appName'), client.get('email'));
          });
        },
        isAuthenticated = function() {
          var authenticated = client.get('token') !== null && client.get(
            'organizations') !== null;
          if (authenticated) {
            initialize();
          }
          return authenticated;
        };
      if (!isAuthenticated() || force) {
        if (!client.get('token')) {
          return $rootScope.$broadcast('checkAuthentication-error',
            'no token', {}, client.get('email'));
        }
        this.client().reAuthenticateLite(function(err) {
          var missingData = err || (!client.get('orgName') || !client.get(
              'appName') || !client.getObject('organizations') || !client
            .getObject('applications'));
          var email = client.get('email');
          if (err || missingData) {
            $rootScope.$broadcast('checkAuthentication-error', err,
              missingData, email);
          } else {
            initialize();
          }
        });
      }
    },
    reAuthenticate: function(email, eventOveride) {
      var ug = this;
      this.client().reAuthenticate(email, function(err, data, user,
        organizations, applications) {
        if (!err) {
          $rootScope.currentUser = user;
        }
        if (!err) {
          $rootScope.userEmail = user.get('email');
          $rootScope.organizations = organizations;
          $rootScope.applications = applications;
          $rootScope.currentOrg = ug.get('orgName');
          $rootScope.currentApp = ug.get('appName');
          $rootScope.currentUser = data;
          $rootScope.currentUser.profileImg = utility.get_gravatar(
            $rootScope.currentUser.email);
        }
        $rootScope.$broadcast((eventOveride || 'reAuthenticate') + '-' + (
            err ? 'error' : 'success'), err, data, user, organizations,
          applications);

      });
    },
    logoutCallback: function() {
      $rootScope.$broadcast('userNotAuthenticated');
    },
    logout: function() {
      $rootScope.activeUI = false;
      $rootScope.userEmail = 'user@apigee.com';
      $rootScope.organizations = {
        "noOrg": {
          name: "No Orgs Found"
        }
      };
      $rootScope.applications = {
        "noApp": {
          name: "No Apps Found"
        }
      };
      $rootScope.currentOrg = 'No Org Found';
      $rootScope.currentApp = 'No App Found';
      sessionStorage.setItem('accessToken', null);
      sessionStorage.setItem('userUUID', null);
      sessionStorage.setItem('userEmail', null);

      this.client().logout();
      this._client = null;
    },
    client: function() {
      var options = {
        buildCurl: Usergrid.options.client.buildCurl || false,
        logging: Usergrid.options.client.logging || false
      };
      if (Usergrid.options && Usergrid.options.client) {
        options.keys = Usergrid.options.client;
      }

      this._client = this._client || new Usergrid.Client(options,
        $rootScope.urls().DATA_URL
      );
      return this._client;
    },
    setClientProperty: function(key, value) {
      this.client().set(key, value);
    },
    getTopCollections: function() {
      var options = {
        method: 'GET',
        endpoint: ''
      }
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error getting collections');
        } else {
          var collections = data.entities[0].metadata.collections;
          $rootScope.$broadcast('top-collections-received', collections);
        }
      });
    },
    createCollection: function(collectionName) {
      var collections = {};
      collections[collectionName] = {};
      var metadata = {
        metadata: {
          collections: collections
        }
      }
      var options = {
        method: 'PUT',
        body: metadata,
        endpoint: ''
      }
      var self = this;
      this.client().request(options, function(err, data) {
        if (err) {
          console.error(err);
          return $rootScope.$broadcast('alert', 'error',
            'error creating collection');
        }
        self.client().createEntity({
          type: collectionName,
          testData: 'test'
        }, function(err, entity) {
          if (err) {
            console.error(err);
            return $rootScope.$broadcast('alert', 'error',
              'error creating collection');
          }
          entity.destroy(function() {
            self.getTopCollections(function(err, collections) {
              if (err) {
                $rootScope.$broadcast('alert', 'error',
                  'error creating collection');
              } else {
                $rootScope.$broadcast('collection-created',
                  collections);
              }
            });
          });

        })


      });
    },
    createCollectionWithEntity: function(collectionName, entityData) {
      try{
        entityData=JSON.parse(entityData);
      }catch(e){
        $rootScope.$broadcast('alert', 'error', 'JSON is not valid');
        return;
      }
      var options = {
        method: 'POST',
        endpoint: collectionName,
        body: entityData
      }
      var self = this;
      self.client().request(options, function(err, entity) {
        if (err) {
          console.error(err);
          return $rootScope.$broadcast('alert', 'error',
              'error creating collection');
        }
        self.getTopCollections(function(err, collections) {
          if (err) {
            $rootScope.$broadcast('alert', 'error',
                'error creating collection');
          } else {
            $rootScope.$broadcast('alert', 'success', 'Collection created successfully.');
            $rootScope.$broadcast('collection-created',
                collections);
          }
        });
      });
    },
    getApplications: function() {
      this.client().getApplications(function(err, applications) {
        if (err) {
          applications && console.error(applications);
        } else {
          $rootScope.$broadcast('applications-received', applications);
        }
      });
    },
    getAdministrators: function() {
      this.client().getAdministrators(function(err, administrators) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error getting administrators');
        }
        $rootScope.$broadcast('administrators-received', administrators);
      });
    },
    createApplication: function(appName) {
      this.client().createApplication(appName, function(err, applications) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error creating application');
        } else {
          $rootScope.$broadcast('applications-created', applications,
            appName);
          $rootScope.$broadcast('applications-received', applications);
        }
      });
    },
    createAdministrator: function(adminName) {
      this.client().createAdministrator(adminName, function(err,
        administrators) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error creating administrator');
        }
        $rootScope.$broadcast('administrators-received', administrators);
      });
    },
    getFeed: function() {
      var options = {
        method: 'GET',
        endpoint: 'management/organizations/' + this.client().get('orgName') +
          '/feed',
        mQuery: true
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting feed');
        } else {
          var feedData = data.entities;
          var feed = [];
          var i = 0;
          for (i = 0; i < feedData.length; i++) {
            var date = (new Date(feedData[i].created)).toUTCString();

            var title = feedData[i].title;

            var n = title.indexOf(">");
            title = title.substring(n + 1, title.length);

            n = title.indexOf(">");
            title = title.substring(n + 1, title.length);

            if (feedData[i].actor) {
              title = feedData[i].actor.displayName + ' ' + title;
            }
            feed.push({
              date: date,
              title: title
            });
          }
          if (i === 0) {
            feed.push({
              date: "",
              title: "No Activities found."
            });
          }

          $rootScope.$broadcast('feed-received', feed);
        }
      });

    },
    createGroup: function(path, title) {
      var options = {
        path: path,
        title: title
      }
      var self = this;
      this.groupsCollection.addEntity(options, function(err) {
        if (err) {
          $rootScope.$broadcast('groups-create-error', err);
        } else {
          $rootScope.$broadcast('groups-create-success', self.groupsCollection);
          $rootScope.$broadcast('groups-received', self.groupsCollection);
        }
      });
    },
    createRole: function(name, title) {
      var options = {
          name: name,
          title: title
        },
        self = this;
      this.rolesCollection.addEntity(options, function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error creating role');
        } else {
          $rootScope.$broadcast('roles-received', self.rolesCollection);
        }
      });
    },
    createUser: function(username, name, email, password) {
      var options = {
        username: username,
        name: name,
        email: email,
        password: password
      }
      var self = this;
      this.usersCollection.addEntity(options, function(err, data) {
        if (err) {
          if (typeof data === 'string') {
            $rootScope.$broadcast("alert", "error", "error: " + data);
          } else {
            $rootScope.$broadcast("alert", "error",
              "error creating user. the email address might already exist."
            );
          }
        } else {
          $rootScope.$broadcast('users-create-success', self.usersCollection);

          $rootScope.$broadcast('users-received', self.usersCollection);
        }
      });
    },
    getCollection: function(type, path, orderBy, query, limit) {
      var options = {
        type: path,
        qs: {}
      }
      if (query) {
        options.qs['ql'] = query;
      }

      //force order by 'created desc' if none exists
      if (options.qs.ql) {
        options.qs['ql'] = options.qs.ql + ' order by ' + (orderBy ||
          'created desc');
      } else {
        options.qs['ql'] = ' order by ' + (orderBy || 'created desc');
      }

      if (limit) {
        options.qs['limit'] = limit;
      }
      this.client().createCollection(options, function(err, collection, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting ' +
            collection._type + ': ' + data.error_description);
          $rootScope.$broadcast(type + '-error', collection);
        } else {
          $rootScope.$broadcast(type + '-received', collection);
        }
        //temporarily adding scope.apply to get working in prod, otherwise the events won't get broadcast
        //todo - we need an apply strategy for 3rd party ug calls!
        if (!$rootScope.$$phase) {
          $rootScope.$apply();
        }
      });
    },
    runDataQuery: function(queryPath, searchString, queryLimit) {
      this.getCollection('query', queryPath, null, searchString, queryLimit);
    },
    runDataPOSTQuery: function(queryPath, body) {
      var self = this;
      var options = {
        method: 'POST',
        endpoint: queryPath,
        body: body
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error: ' + data.error_description);
          $rootScope.$broadcast('error-running-query', data);
        } else {

          var queryPath = data.path;
          //remove preceeding slash
          queryPath = queryPath.replace(/^\//, '');
          self.getCollection('query', queryPath, null, 'order by modified DESC', null);

        }
      });
    },
    runDataPutQuery: function(queryPath, searchString, queryLimit, body) {
      var self = this;
      var options = {
        method: 'PUT',
        endpoint: queryPath,
        body: body
      };

      if (searchString) {
        options.qs['ql'] = searchString;
      }
      if (queryLimit) {
        options.qs['queryLimit'] = queryLimit;
      }

      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error: ' + data.error_description);
        } else {

          var queryPath = data.path;
          self.getCollection('query', queryPath, null,
            'order by modified DESC', null);

        }
      });
    },
    runDataDeleteQuery: function(queryPath, searchString, queryLimit) {
      var self = this;
      var options = {
        method: 'DELETE',
        endpoint: queryPath
      };

      if (searchString) {
        options.qs['ql'] = searchString;
      }
      if (queryLimit) {
        options.qs['queryLimit'] = queryLimit;
      }

      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error: ' + data.error_description);
        } else {

          var queryPath = data.path;
          self.getCollection('query', queryPath, null,
            'order by modified DESC', null);

        }
      });
    },
    getUsers: function() {
      this.getCollection('users', 'users', 'username');
      var self = this;
      $rootScope.$on("users-received", function(evt, users) {
        self.usersCollection = users;
      })
    },
    getGroups: function() {
      this.getCollection('groups', 'groups', 'title');
      var self = this;
      $rootScope.$on('groups-received', function(event, roles) {
        self.groupsCollection = roles;
      });

    },
    getRoles: function() {
      this.getCollection('roles', 'roles', 'name');
      var self = this;
      $rootScope.$on('roles-received', function(event, roles) {
        self.rolesCollection = roles
      });
    },
    getNotifiers: function() {
      var query = '',
        limit = '100',
        self = this;
      this.getCollection('notifiers', 'notifiers', 'created', query, limit);
      $rootScope.$on('notifiers-received', function(event, notifiers) {
        self.notifiersCollection = notifiers;
      });
    },
    getNotificationHistory: function(type) {
      var query = null;
      if (type) {
        query = "select * where state = '" + type + "'";
      }
      this.getCollection('notifications', 'notifications', 'created desc',
        query);
      var self = this;
      $rootScope.$on('notifications-received', function(event, notifications) {
        self.notificationCollection = notifications;
      });
    },
    getNotificationReceipts: function(uuid) {
      this.getCollection('receipts', 'receipts', 'created desc',
        'notificationUUID=' + uuid);
      var self = this;
      $rootScope.$on('receipts-received', function(event, receipts) {
        self.receiptsCollection = receipts;
      });
    },
    getIndexes: function(path) {
      var options = {
        method: 'GET',
        endpoint: path.split('/').concat('indexes').filter(function(bit) {
          return bit && bit.length
        }).join('/')
      }
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'Problem getting indexes: ' + data.error);
        } else {
          $rootScope.$broadcast('indexes-received', data.data);
        }
      });
    },
    sendNotification: function(path, body) {
      var options = {
        method: 'POST',
        endpoint: path,
        body: body
      }
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'Problem creating notification: ' + data.error);
        } else {
          $rootScope.$broadcast('send-notification-complete');
        }
      });
    },
    getRolesUsers: function(username) {
      var self = this;
      var options = {
        type: 'roles/users/' + username,
        qs: {
          ql: 'order by username'
        }
      }
      this.client().createCollection(options, function(err, users) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting users');
        } else {
          $rootScope.$broadcast('users-received', users);

        }
      });
    },
    getTypeAheadData: function(type, searchString, searchBy, orderBy) {

      var self = this;
      var search = '';
      var qs = {
        limit: 100
      };
      if (searchString) {
        search = "select * where " + searchBy + " = '" + searchString + "'";
      }
      if (orderBy) {
        search = search + " order by " + orderBy;
      }
      if (search) {
        qs.ql = search;
      }
      var options = {
        method: 'GET',
        endpoint: type,
        qs: qs
      }
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting ' + type);
        } else {
          var entities = data.entities;
          $rootScope.$broadcast(type + '-typeahead-received', entities);
        }
      });
    },
    getUsersTypeAhead: function(searchString) {
      this.getTypeAheadData('users', searchString, 'username', 'username');
    },
    getGroupsTypeAhead: function(searchString) {
      this.getTypeAheadData('groups', searchString, 'path', 'path');
    },
    getRolesTypeAhead: function(searchString) {
      this.getTypeAheadData('roles', searchString, 'name', 'name');
    },
    getGroupsForUser: function(user) {
      var self = this;
      var options = {
        type: 'users/' + user + '/groups'
      }
      this.client().createCollection(options, function(err, groups) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting groups');
        } else {

          $rootScope.$broadcast('user-groups-received', groups);

        }
      });
    },
    addUserToGroup: function(user, group) {
      var self = this;
      var options = {
        type: 'users/' + user + '/groups/' + group
      }
      this.client().createEntity(options, function(err, entity) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error adding user to group');
        } else {
          $rootScope.$broadcast('user-added-to-group-received');
        }
      });
    },
    addUserToRole: function(user, role) {
      var options = {
        method: 'POST',
        endpoint: 'roles/' + role + '/users/' + user
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error adding user to role');
        } else {
          $rootScope.$broadcast('role-update-received');
        }
      });
    },
    addGroupToRole: function(group, role) {
      var options = {
        method: 'POST',
        endpoint: 'roles/' + role + '/groups/' + group
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error adding group to role');
        } else {
          $rootScope.$broadcast('role-update-received');
        }
      });
    },
    followUser: function(user) {
      var self = this;
      var username = $rootScope.selectedUser.get('uuid');
      var options = {
        method: 'POST',
        endpoint: 'users/' + username + '/following/users/' + user
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error following user');
        } else {
          $rootScope.$broadcast('follow-user-received');
        }
      });
    },
    newPermission: function(permission, type, entity) { //"get,post,put:/mypermission"
      var options = {
        method: 'POST',
        endpoint: type + '/' + entity + '/permissions',
        body: {
          "permission": permission
        }
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error adding permission');
        } else {
          $rootScope.$broadcast('permission-update-received');
        }
      });
    },
    newUserPermission: function(permission, username) {
      this.newPermission(permission, 'users', username)
    },
    newGroupPermission: function(permission, path) {
      this.newPermission(permission, 'groups', path)
    },
    newRolePermission: function(permission, name) {
      this.newPermission(permission, 'roles', name)
    },

    deletePermission: function(permission, type, entity) { //"get,post,put:/mypermission"
      var options = {
        method: 'DELETE',
        endpoint: type + '/' + entity + '/permissions',
        qs: {
          permission: permission
        }
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error deleting permission');
        } else {
          $rootScope.$broadcast('permission-update-received');
        }
      });
    },
    deleteUserPermission: function(permission, user) {
      this.deletePermission(permission, 'users', user);
    },
    deleteGroupPermission: function(permission, group) {
      this.deletePermission(permission, 'groups', group);
    },
    deleteRolePermission: function(permission, rolename) {
      this.deletePermission(permission, 'roles', rolename);
    },
    removeUserFromRole: function(user, role) { //"get,post,put:/mypermission"
      var options = {
        method: 'DELETE',
        endpoint: 'roles/' + role + '/users/' + user
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error removing user from role');
        } else {
          $rootScope.$broadcast('role-update-received');
        }
      });
    },
    removeUserFromGroup: function(group, role) { //"get,post,put:/mypermission"
      var options = {
        method: 'DELETE',
        endpoint: 'roles/' + role + '/groups/' + group
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error',
            'error removing role from the group');
        } else {
          $rootScope.$broadcast('role-update-received');
        }
      });
    },
    createWinNotifier: function(name,sid,apiKey, logging){
      var options = {
        method: 'POST',
        endpoint: 'notifiers',
        body: {
          "sid": sid,
          "apiKey": apiKey,
          "name": name,
          "logging": logging,
          "provider": "windows"
        }
      };
      this.client().request(options, this.notifierResponseReceived);
    },
    createAndroidNotifier: function(name, APIkey) {
      var options = {
        method: 'POST',
        endpoint: 'notifiers',
        body: {
          "apiKey": APIkey,
          "name": name,
          "provider": "google"
        }
      };
      this.client().request(options, this.notifierResponseReceived);
    },
    createAppleNotifier: function(file, name, environment,
      certificatePassword) {

      var provider = 'apple';

      var formData = new FormData();
      formData.append("p12Certificate", file);

      formData.append('name', name);
      formData.append('provider', provider);
      formData.append('environment', environment);
      formData.append('certificatePassword', certificatePassword || "");

      var options = {
        method: 'POST',
        endpoint: 'notifiers',
        formData: formData
      };
      this.client().request(options, this.notifierResponseReceived);
    },
    notifierResponseReceived:function(err, data) {
      if (err) {
        console.error(data);
        $rootScope.$broadcast('alert', 'error', data.error_description || 'error creating notifier');
      } else {
        $rootScope.$broadcast('alert', 'success',
            'New notifier created successfully.');
        $rootScope.$broadcast('notifier-update');
      }
    },
    initializeCurrentUser: function(callback) {
      callback = callback || function() {};
      if ($rootScope.currentUser && !$rootScope.currentUser.reset) {
        callback($rootScope.currentUser);
        return $rootScope.$broadcast('current-user-initialized', '');
      }
      var options = {
        method: 'GET',
        endpoint: 'management/users/' + this.client().get('email'),
        mQuery: true
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'Error getting user info');
        } else {
          $rootScope.currentUser = data.data;
          $rootScope.currentUser.profileImg = utility.get_gravatar(
            $rootScope.currentUser.email);
          $rootScope.userEmail = $rootScope.currentUser.email;
          callback($rootScope.currentUser);
          $rootScope.$broadcast('current-user-initialized', $rootScope.currentUser);
        }
      });
    },

    updateUser: function(user) {
      var body = {};
      body.username = user.username;
      body.name = user.name;
      body.email = user.email;
      var options = {
        method: 'PUT',
        endpoint: 'management/users/' + user.uuid + '/',
        mQuery: true,
        body: body
      };
      var self = this;
      this.client().request(options, function(err, data) {
        self.client().set('email', user.email);
        self.client().set('username', user.username);
        if (err) {
          return $rootScope.$broadcast('user-update-error', data);
        }
        $rootScope.currentUser.reset = true;
        self.initializeCurrentUser(function() {
          $rootScope.$broadcast('user-update-success', $rootScope.currentUser);
        });
      });
    },

    resetUserPassword: function(user) {
      var body = {};
      body.oldpassword = user.oldPassword;
      body.newpassword = user.newPassword;
      body.username = user.username;
      var options = {
        method: 'PUT',
        endpoint: 'management/users/' + user.uuid + '/',
        body: body,
        mQuery: true
      }
      this.client().request(options, function(err, data) {
        if (err) {
          return $rootScope.$broadcast('alert', 'error',
            'Error resetting password');
        }
        //remove old and new password fields so they don't end up as part of the entity object
        $rootScope.currentUser.oldPassword = '';
        $rootScope.currentUser.newPassword = '';
        $rootScope.$broadcast('user-reset-password-success', $rootScope.currentUser);
      });

    },
    getOrgCredentials: function() {
      var options = {
        method: 'GET',
        endpoint: 'management/organizations/' + this.client().get('orgName') +
          '/credentials',
        mQuery: true
      };
      this.client().request(options, function(err, data) {
        if (err && data.credentials) {
          $rootScope.$broadcast('alert', 'error',
            'Error getting credentials');
        } else {
          $rootScope.$broadcast('org-creds-updated', data.credentials);
        }
      });
    },
    regenerateOrgCredentials: function() {
      var self = this;
      var options = {
        method: 'POST',
        endpoint: 'management/organizations/' + this.client().get('orgName') +
          '/credentials',
        mQuery: true
      };
      this.client().request(options, function(err, data) {
        if (err && data.credentials) {
          $rootScope.$broadcast('alert', 'error',
            'Error regenerating credentials');
        } else {
          $rootScope.$broadcast('alert', 'success',
            'Regeneration of credentials complete.');
          $rootScope.$broadcast('org-creds-updated', data.credentials);
        }
      });
    },
    getAppCredentials: function() {
      var options = {
        method: 'GET',
        endpoint: 'credentials'
      };
      this.client().request(options, function(err, data) {
        if (err && data.credentials) {
          $rootScope.$broadcast('alert', 'error',
            'Error getting credentials');
        } else {
          $rootScope.$broadcast('app-creds-updated', data.credentials);
        }
      });
    },

    regenerateAppCredentials: function() {
      var self = this;
      var options = {
        method: 'POST',
        endpoint: 'credentials'
      };
      this.client().request(options, function(err, data) {
        if (err && data.credentials) {
          $rootScope.$broadcast('alert', 'error',
            'Error regenerating credentials');
        } else {
          $rootScope.$broadcast('alert', 'success',
            'Regeneration of credentials complete.');
          $rootScope.$broadcast('app-creds-updated', data.credentials);
        }
      });
    },

    signUpUser: function(orgName, userName, name, email, password) {
      var formData = {
        "organization": orgName,
        "username": userName,
        "name": name,
        "email": email,
        "password": password
      };
      var options = {
        method: 'POST',
        endpoint: 'management/organizations',
        body: formData,
        mQuery: true
      };
      var client = this.client();
      client.request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('register-error', data);
        } else {
          $rootScope.$broadcast('register-success', data);
        }
      });
    },
    resendActivationLink: function(id) {
      var options = {
        method: 'GET',
        endpoint: 'management/users/' + id + '/reactivate',
        mQuery: true
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('resend-activate-error', data);
        } else {
          $rootScope.$broadcast('resend-activate-success', data);
        }
      });
    },
    getAppSettings: function() {
      $rootScope.$broadcast('app-settings-received', {});
    },
    getActivities: function() {
      this.client().request({
        method: 'GET',
        endpoint: 'activities',
        qs: {
          limit: 200
        }
      }, function(err, data) {
        if (err) return $rootScope.$broadcast('app-activities-error', data);
        var entities = data.entities;
        //set picture if there is none and change gravatar to secure
        entities.forEach(function(entity) {
          if (!entity.actor.picture) {
            entity.actor.picture = window.location.protocol + "//" +
              window.location.host + window.location.pathname +
              "img/user_profile.png"
          } else {
            entity.actor.picture = entity.actor.picture.replace(
              /^http:\/\/www.gravatar/i, 'https://secure.gravatar');
            //note: changing this to use the image on apigee.com - since the gravatar default won't work on any non-public domains such as localhost
            //this_data.picture = this_data.picture + encodeURI("?d="+window.location.protocol+"//" + window.location.host + window.location.pathname + "images/user_profile.png");
            if (~entity.actor.picture.indexOf('http')) {
              entity.actor.picture = entity.actor.picture;
            } else {
              entity.actor.picture =
                'https://apigee.com/usergrid/img/user_profile.png';
            }
          }
        });
        $rootScope.$broadcast('app-activities-received', data.entities);
      });
    },
    getEntityActivities: function(entity, isFeed) {
      var route = isFeed ? 'feed' : 'activities'
      var endpoint = entity.get('type') + '/' + entity.get('uuid') + '/' +
        route;
      var options = {
        method: 'GET',
        endpoint: endpoint,
        qs: {
          limit: 200
        }
      };
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast(entity.get('type') + '-' + route + '-error',
            data);
        }
        data.entities.forEach(function(entityInstance) {
          entityInstance.createdDate = (new Date(entityInstance.created))
            .toUTCString();
        });
        $rootScope.$broadcast(entity.get('type') + '-' + route +
          '-received', data.entities);
      });
    },
    addUserActivity: function(user, content) {
      var options = {
        "actor": {
          "displayName": user.get('username'),
          "uuid": user.get('uuid'),
          "username": user.get('username')
        },
        "verb": "post",
        "content": content
      };
      this.client().createUserActivity(user.get('username'), options,
        function(err, activity) { //first argument can be 'me', a uuid, or a username
          if (err) {
            $rootScope.$broadcast('user-activity-add-error', err);
          } else {
            $rootScope.$broadcast('user-activity-add-success', activity);
          }
        });
    },
    runShellQuery: function(method, path, payload) {
      var path = path.replace(/^\//, ''); //remove leading slash if it does
      var options = {
        "method": method,
        "endpoint": path
      };
      if (payload) {
        options["body"] = payload;
      }
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('shell-error', data);
        } else {
          $rootScope.$broadcast('shell-success', data);
        }
      });
    },
    addOrganization: function(user, orgName) {
      var options = {
          method: 'POST',
          endpoint: 'management/users/' + user.uuid + '/organizations',
          body: {
            organization: orgName
          },
          mQuery: true
        },
        client = this.client(),
        self = this;
      client.request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('user-add-org-error', data);
        } else {
          $rootScope.$broadcast('user-add-org-success', $rootScope.organizations);
        }
      });
    },
    leaveOrganization: function(user, org) {
      var options = {
        method: 'DELETE',
        endpoint: 'management/users/' + user.uuid + '/organizations/' + org
          .uuid,
        mQuery: true
      }
      this.client().request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('user-leave-org-error', data);
        } else {
          delete $rootScope.organizations[org.name];
          $rootScope.$broadcast('user-leave-org-success', $rootScope.organizations);
        }
      });
    },
    /**
     * Retrieves page data
     * @param {string} id the name of the single page item to get (a key in the JSON) can be null.
     * @param {string} url location of the file/endpoint.
     * @return {Promise} Resolves to JSON.
     */
    httpGet: function(id, url) {
      var items, deferred;

      deferred = $q.defer();

      $http.get((url || configuration.ITEMS_URL)).
      success(function(data, status, headers, config) {
        var result;
        if (id) {
          angular.forEach(data, function(obj, index) {
            if (obj.id === id) {
              result = obj;
            }
          });
        } else {
          result = data;
        }
        deferred.resolve(result);
      }).
      error(function(data, status, headers, config) {
        $log.error(data, status, headers, config);
        reportError(data, config);
        deferred.reject(data);
      });

      return deferred.promise;
    },

    /**
     * Retrieves page data via jsonp
     * @param {string} url the location of the JSON/RESTful endpoint.
     * @param {string} successCallback function called on success.
     */

    jsonp: function(objectType, criteriaId, params, successCallback) {
      if (!params) {
        params = {};
      }
      params.demoApp = $rootScope.demoData;
      params.access_token = getAccessToken();
      params.callback = 'JSON_CALLBACK';
      var uri = $rootScope.urls().DATA_URL + '/' + $rootScope.currentOrg +
        '/' + $rootScope.currentApp + '/apm/' + objectType + '/' + criteriaId;
      return this.jsonpRaw(objectType, criteriaId, params, uri,
        successCallback);
    },

    jsonpSimple: function(objectType, appId, params) {
      var uri = $rootScope.urls().DATA_URL + '/' + $rootScope.currentOrg +
        '/' + $rootScope.currentApp + '/apm/' + objectType + "/" + appId;
      return this.jsonpRaw(objectType, appId, params, uri);
    },
    calculateAverageRequestTimes: function() {
      if (!running) {
        var self = this;
        running = true;
        setTimeout(function() {
          running = false;
          var length = requestTimes.length < 10 ? requestTimes.length : 10;
          var sum = requestTimes.slice(0, length).reduce(function(a, b) {
            return a + b
          });
          var avg = sum / length;
          self.averageRequestTimes = avg / 1000;
          if (self.averageRequestTimes > 5) {
            $rootScope.$broadcast('request-times-slow', self.averageRequestTimes);
          }
        }, 3000);
      }
    },
    jsonpRaw: function(objectType, appId, params, uri, successCallback) {
      if (typeof successCallback !== 'function') {
        successCallback = null;
      }
      uri = uri || ($rootScope.urls().DATA_URL + '/' + $rootScope.currentOrg +
        '/' + $rootScope.currentApp + '/' + objectType);

      if (!params) {
        params = {};
      }

      var start = new Date().getTime(),
        self = this;

      params.access_token = getAccessToken();
      params.callback = 'JSON_CALLBACK';

      var deferred = $q.defer();

      var diff = function() {
        currentRequests[uri]--;
        requestTimes.splice(0, 0, new Date().getTime() - start);
        self.calculateAverageRequestTimes();
      };

      successCallback && $rootScope.$broadcast("ajax_loading", objectType);
      var reqCount = currentRequests[uri] || 0;
      if (self.averageRequestTimes > 5 && reqCount > 1) {
        setTimeout(function() {
          deferred.reject(new Error('query in progress'));
        }, 50);
        return deferred;
      }
      currentRequests[uri] = (currentRequests[uri] || 0) + 1;

      $http.jsonp(uri, {
        params: params
      }).
      success(function(data, status, headers, config) {
        diff();
        if (successCallback) {
          successCallback(data, status, headers, config);
          $rootScope.$broadcast("ajax_finished", objectType);
        }
        deferred.resolve(data);
      }).
      error(function(data, status, headers, config) {
        diff();
        $log.error("ERROR: Could not get jsonp data. " + uri);
        reportError(data, config);
        deferred.reject(data);
      });

      return deferred.promise;
    },

    resource: function(params, isArray) {
      //temporary url for REST endpoints

      return $resource($rootScope.urls().DATA_URL +
        '/:orgname/:appname/:username/:endpoint', {

        }, {
          get: {
            method: 'JSONP',
            isArray: isArray,
            params: params
          },
          login: {
            method: 'GET',
            url: $rootScope.urls().DATA_URL + '/management/token',
            isArray: false,
            params: params
          },
          save: {
            url: $rootScope.urls().DATA_URL + '/' + params.orgname + '/' +
              params.appname,
            method: 'PUT',
            isArray: false,
            params: params
          }
        });
    },

    httpPost: function(url, callback, payload, headers) {

      var accessToken = getAccessToken();

      if (payload) {
        payload.access_token = accessToken;
      } else {
        payload = {
          access_token: accessToken
        }
      }

      if (!headers) {
        headers = {
          Bearer: accessToken
        };
      }

      $http({
        method: 'POST',
        url: url,
        data: payload,
        headers: headers
      }).
      success(function(data, status, headers, config) {
        callback(data)
      }).
      error(function(data, status, headers, config) {
        reportError(data, config);
        callback(data)
      });

    }
  }
});
