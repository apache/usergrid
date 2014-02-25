'use strict';

AppServices.Services.factory('ug', function (configuration, $rootScope,utility) {

  return {
    get:function(prop,isObject){
      return isObject ? this.client().getObject(prop) : this.client().get(prop);
    },
    set:function(prop,value){
      this.client().set(prop,value);

    },
    orgLogin:function(username,password){
      var self = this;
      this.client().set('email', username);
      this.client().set('token', null);
      this.client().orgLogin(username,password,function(err, data, user, organizations, applications){
        if(err){
          $rootScope.$broadcast('loginFailed', err,data);
        }else{
          self.initializeCurrentUser(function () {
            $rootScope.$broadcast('loginSuccesful', user, organizations, applications);
          });
        }
      });
    },

    checkAuthentication:function(force){
      var ug = this;
      var client = ug.client();

      var initialize = function () {
            ug.initializeCurrentUser(function () {
              $rootScope.userEmail = client.get('email');
              $rootScope.organizations = client.getObject('organizations');
              $rootScope.applications = client.getObject('applications');
              $rootScope.currentOrg = client.get('orgName');
              $rootScope.currentApp = client.get('appName');
              var size = 0, key;
              for (key in  $rootScope.applications) {
                if ($rootScope.applications.hasOwnProperty(key)) size++;
              }
              $rootScope.addApplications = size < 10;
              $rootScope.$broadcast('checkAuthentication-success', client.getObject('organizations'), client.getObject('applications'), client.get('orgName'), client.get('appName'), client.get('email'));
            });
          },
          isAuthenticated = function () {
            var authenticated = client.get('token') !== null && client.get('organizations') !== null;
            if (authenticated) {
              initialize();
            }
            return authenticated;
          };
      if(!isAuthenticated() || force){
        if(!client.get('token')){
          return $rootScope.$broadcast('checkAuthentication-error','no token',{},client.get('email'));
        }
        this.client().reAuthenticateLite(function(err){
          var missingData = err || ( !client.get('orgName') || !client.get('appName') || !client.getObject('organizations') || !client.getObject('applications'));
          var email  = client.get('email');
          if(err || missingData){
            $rootScope.$broadcast('checkAuthentication-error',err,missingData,email);
          }else{
            initialize();
          }
        });
      }
    },
    reAuthenticate:function(email,eventOveride){
      var ug = this;
      this.client().reAuthenticate(email,function(err, data, user, organizations, applications){
        if(!err){
          $rootScope.currentUser = user;
        }
        if(!err){
          $rootScope.userEmail = user.get('email');
          $rootScope.organizations = organizations;
          $rootScope.applications = applications;
          $rootScope.currentOrg = ug.get('orgName');
          $rootScope.currentApp = ug.get('appName');
          $rootScope.currentUser = user._data;
          $rootScope.currentUser.profileImg = utility.get_gravatar($rootScope.currentUser.email);
        }
        $rootScope.$broadcast((eventOveride || 'reAuthenticate')+'-' + (err ? 'error' : 'success'),err, data, user, organizations, applications);

      });
    },
    logoutCallback: function() {
      $rootScope.$broadcast('userNotAuthenticated');
    },
    logout:function(){
      $rootScope.activeUI = false;
      $rootScope.userEmail = 'user@apigee.com';
      $rootScope.organizations = {"noOrg":{name:"No Orgs Found"}};
      $rootScope.applications = {"noApp":{name:"No Apps Found"}};
      $rootScope.currentOrg = 'No Org Found';
      $rootScope.currentApp = 'No App Found';
      sessionStorage.setItem('accessToken', null);
      sessionStorage.setItem('userUUID', null);
      sessionStorage.setItem('userEmail', null);

      this.client().logout();
      this._client = null;
    },
    client: function(){
      var options = {
        buildCurl:true,
        logging:true
      };
      if(Usergrid.options && Usergrid.options.client){
        options.keys = Usergrid.options.client;
      }

      this._client = this._client || new Usergrid.Client(options,
          $rootScope.urls().DATA_URL
      );
      return this._client;
    },
    getTopCollections: function () {
      var options = {
        method:'GET',
        endpoint: ''
      }
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting collections');
        } else {
          var collections = data.entities[0].metadata.collections;
          $rootScope.$broadcast('top-collections-received', collections);
        }
      });
    },
    createCollection: function (collectionName) {
      var collections = {};
      collections[collectionName] = {};
      var metadata = {
        metadata: {
          collections: collections
        }
      }
      var options = {
        method:'PUT',
        body: metadata,
        endpoint: ''
      }
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error creating collection');
        } else {
          $rootScope.$broadcast('collection-created', collections);
        }
      });
    },
    getApplications: function () {
      this.client().getApplications(function (err, applications) {
        if (err) {
           applications && console.error(applications);
        }else{
          $rootScope.$broadcast('applications-received', applications);
        }
      });
    },
    getAdministrators: function () {
      this.client().getAdministrators(function (err, administrators) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting administrators');
        }
        $rootScope.$broadcast('administrators-received', administrators);
      });
    },
    createApplication: function (appName) {
      this.client().createApplication(appName, function (err, applications) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error creating application');
        }else{
          $rootScope.$broadcast('applications-created', applications,appName);
          $rootScope.$broadcast('applications-received', applications);
        }
      });
    },
    createAdministrator: function (adminName) {
      this.client().createAdministrator(adminName, function (err, administrators) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error creating administrator');
        }
        $rootScope.$broadcast('administrators-received', administrators);
      });
    },
    getFeed: function () {
      var options = {
        method:'GET',
        endpoint:'management/organizations/'+this.client().get('orgName')+'/feed',
        mQuery:true
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting feed');
        } else {
          var feedData = data.entities;
          var feed = [];
          var i=0;
          for (i=0; i < feedData.length; i++) {
            var date = (new Date(feedData[i].created)).toUTCString();

            var title = feedData[i].title;

            var n=title.indexOf(">");
            title = title.substring(n+1,title.length);

            n=title.indexOf(">");
            title = title.substring(n+1,title.length);

            if (feedData[i].actor) {
              title = feedData[i].actor.displayName + ' ' + title;
            }
            feed.push({date:date, title:title});
          }
          if (i === 0) {
            feed.push({date:"", title:"No Activities found."});
          }

          $rootScope.$broadcast('feed-received', feed);
        }
      });

    },
    createGroup: function (path, title) {
      var options = {
        path:path,
        title:title
      }
      var self = this;
      this.groupsCollection.addEntity(options, function(err){
        if (err) {
          $rootScope.$broadcast('groups-create-error', err);
        } else {
          $rootScope.$broadcast('groups-create-success', self.groupsCollection);
          $rootScope.$broadcast('groups-received', self.groupsCollection);
        }
      });
    },
    createRole: function (name, title) {
      var options = {
        name:name,
        title:title
          },
          self = this;
      this.rolesCollection.addEntity(options, function(err){
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error creating role');
        } else {
          $rootScope.$broadcast('roles-received', self.rolesCollection);
        }
      });
    },
    createUser: function (username, name, email, password){
      var options = {
        username:username,
        name:name,
        email:email,
        password:password
      }
      var self = this;
      this.usersCollection.addEntity(options, function(err, data){
        if (err) {
          if (data) {
            $rootScope.$broadcast("alert", "error", "error: " + data);
          } else {
            $rootScope.$broadcast("alert", "error", "error creating user");
          }
        } else {
          $rootScope.$broadcast('users-create-success', self.usersCollection);

          $rootScope.$broadcast('users-received', self.usersCollection);
        }
      });
    },
    getCollection: function (type, path, orderBy, query, limit) {
      var options = {
        type:path,
        qs:{}
      }
      if (query) {
        options.qs['ql'] = query;
      }

      //force order by 'created desc' if none exists
      if (options.qs.ql) {
        options.qs['ql'] = options.qs.ql + ' order by ' + (orderBy || 'created desc');
      } else {
        options.qs['ql'] = ' order by ' + (orderBy || 'created desc');
      }

      if (limit) {
        options.qs['limit'] = limit;
      }
      this.client().createCollection(options, function (err, collection, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting ' + collection._type + ': ' + data.error_description);
        } else {
          $rootScope.$broadcast(type + '-received', collection);
        }
        //temporarily adding scope.apply to get working in prod, otherwise the events won't get broadcast
        //todo - we need an apply strategy for 3rd party ug calls!
        if(!$rootScope.$$phase) {
          $rootScope.$apply();
        }
      });
    },
    runDataQuery: function (queryPath, searchString, queryLimit) {
      this.getCollection('query', queryPath, null, searchString, queryLimit);
    },
    runDataPOSTQuery: function(queryPath, body) {
      var self = this;
      var options = {
        method:'POST',
        endpoint:queryPath,
        body:body
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error: ' + data.error_description);
          $rootScope.$broadcast('error-running-query',  data);
        } else {

          var queryPath = data.path;
          self.getCollection('query', queryPath, null, 'order by modified DESC', null);

        }
      });
    },
    runDataPutQuery: function(queryPath, searchString, queryLimit, body) {
      var self = this;
      var options = {
        method:'PUT',
        endpoint:queryPath,
        body:body
      };

      if (searchString) {
        options.qs['ql'] = searchString;
      }
      if (queryLimit) {
        options.qs['queryLimit'] = queryLimit;
      }

      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error: ' + data.error_description);
        } else {

          var queryPath = data.path;
          self.getCollection('query', queryPath, null, 'order by modified DESC', null);

        }
      });
    },
    runDataDeleteQuery: function(queryPath, searchString, queryLimit) {
      var self = this;
      var options = {
        method:'DELETE',
        endpoint:queryPath
      };

      if (searchString) {
        options.qs['ql'] = searchString;
      }
      if (queryLimit) {
        options.qs['queryLimit'] = queryLimit;
      }

      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error: ' + data.error_description);
        } else {

          var queryPath = data.path;
          self.getCollection('query', queryPath, null, 'order by modified DESC', null);

        }
      });
    },
    getUsers: function () {
      this.getCollection('users','users','username');
      var self = this;
      $rootScope.$on("users-received",function(evt, users){
        self.usersCollection = users;
      })
    },
    getGroups: function () {
      this.getCollection('groups','groups','title');
      var self = this;
      $rootScope.$on('groups-received', function(event, roles) {
        self.groupsCollection = roles;
      });

     },
    getRoles: function () {
      this.getCollection('roles','roles','name');
      var self = this;
      $rootScope.$on('roles-received', function(event, roles) {
        self.rolesCollection = roles
      });
    },
    getNotifiers: function () {
      var query = '',
          limit = '100',
          self = this;
      this.getCollection('notifiers','notifiers','created', query, limit);
      $rootScope.$on('notifiers-received', function(event, notifiers) {
        self.notifiersCollection = notifiers;
      });
    },
    getNotificationHistory: function (type) {
      var query = null;
      if (type) {
        query = "select * where state = '" + type + "'";
      }
      this.getCollection('notifications','notifications', 'created desc', query);
      var self = this;
      $rootScope.$on('notifications-received', function(event, notifications) {
        self.notificationCollection = notifications;
      });
    },
    getNotificationReceipts: function (uuid) {
      this.getCollection('receipts', 'notifications/'+uuid+'/receipts');
      var self = this;
      $rootScope.$on('receipts-received', function(event, receipts) {
        self.receiptsCollection = receipts;
      });
    },
    getIndexes: function (path) {
      var options = {
        method:'GET',
        endpoint: path + '/indexes'
      }
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'Problem getting indexes: ' + data.error);
        } else {
          $rootScope.$broadcast('indexes-received', data.data);
        }
      });
    },
    sendNotification: function(path, body) {
      var options = {
        method:'POST',
        endpoint: path,
        body:body
      }
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'Problem creating notification: ' + data.error);
        } else {
          $rootScope.$broadcast('send-notification-complete');
        }
      });
    },
    getRolesUsers: function (username) {
      var self = this;
      var options = {
        type:'roles/users/'+username,
        qs:{ql:'order by username'}
      }
      this.client().createCollection(options, function (err, users) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting users');
        } else {
          $rootScope.$broadcast('users-received', users);

        }
      });
    },
    getTypeAheadData: function (type, searchString, searchBy, orderBy) {

      var self = this;
      var search = '';
      var qs = {limit: 100};
      if (searchString) {
        search = "select * where "+searchBy+" = '"+searchString+"'";
      }
      if (orderBy) {
        search = search + " order by "+orderBy;
      }
      if (search) {
        qs.ql = search;
      }
      var options = {
        method:'GET',
        endpoint: type,
        qs:qs
      }
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting '+type);
        } else {
          var entities = data.entities;
          $rootScope.$broadcast(type +'-typeahead-received', entities);
        }
      });
    },
    getUsersTypeAhead: function (searchString) {
      this.getTypeAheadData('users', searchString, 'username', 'username');
    },
    getGroupsTypeAhead: function (searchString) {
      this.getTypeAheadData('groups', searchString, 'path', 'path');
    },
    getRolesTypeAhead: function (searchString) {
      this.getTypeAheadData('roles', searchString, 'name', 'name');
    },
    getGroupsForUser: function (user) {
      var self = this;
      var options = {
        type:'users/'+user+'/groups'
      }
      this.client().createCollection(options, function (err, groups) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting groups');
        } else {

          $rootScope.$broadcast('user-groups-received', groups);

        }
      });
    },
    addUserToGroup: function (user, group) {
      var self = this;
      var options = {
        type:'users/'+user+'/groups/'+group
      }
      this.client().createEntity(options, function (err, entity) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error adding user to group');
        } else {
          $rootScope.$broadcast('user-added-to-group-received');
        }
      });
    },
    addUserToRole: function (user, role) {
      var options = {
        method:'POST',
        endpoint:'roles/'+role+'/users/'+user
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error adding user to role');
        } else {
          $rootScope.$broadcast('role-update-received');
        }
      });
    },
    addGroupToRole: function (group, role) {
      var options = {
        method:'POST',
        endpoint:'roles/'+role+'/groups/'+group
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error adding group to role');
        } else {
          $rootScope.$broadcast('role-update-received');
        }
      });
    },
    followUser: function (user) {
      var self = this;
      var username =  $rootScope.selectedUser.get('uuid');
      var options = {
        method:'POST',
        endpoint:'users/'+username+'/following/users/'+user
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error following user');
        } else {
          $rootScope.$broadcast('follow-user-received');
        }
      });
    },
    newPermission: function (permission, type, entity) { //"get,post,put:/mypermission"
      var options = {
        method:'POST',
        endpoint:type+'/'+entity+'/permissions',
        body:{"permission":permission}
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error adding permission');
        } else {
          $rootScope.$broadcast('permission-update-received');
        }
      });
    },
    newUserPermission: function (permission, username) {
      this.newPermission(permission,'users',username)
    },
    newGroupPermission: function (permission, path) {
      this.newPermission(permission,'groups',path)
    },
    newRolePermission: function (permission, name) {
      this.newPermission(permission,'roles',name)
    },

    deletePermission: function (permission, type, entity) { //"get,post,put:/mypermission"
      var options = {
        method:'DELETE',
        endpoint:type+'/'+entity+'/permissions',
        qs:{permission:permission}
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error deleting permission');
        } else {
          $rootScope.$broadcast('permission-update-received');
        }
      });
    },
    deleteUserPermission: function (permission, user) {
      this.deletePermission(permission,'users',user);
    },
    deleteGroupPermission: function (permission, group) {
      this.deletePermission(permission,'groups',group);
    },
    deleteRolePermission: function (permission, rolename) {
      this.deletePermission(permission,'roles',rolename);
    },
    removeUserFromRole: function (user, role) { //"get,post,put:/mypermission"
      var options = {
        method:'DELETE',
        endpoint:'roles/'+role+'/users/'+user
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error removing user from role');
        } else {
          $rootScope.$broadcast('role-update-received');
        }
      });
    },
    removeUserFromGroup: function (group, role) { //"get,post,put:/mypermission"
      var options = {
        method:'DELETE',
        endpoint:'roles/'+role+'/groups/'+group
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error removing role from the group');
        } else {
          $rootScope.$broadcast('role-update-received');
        }
      });
    },
    createAndroidNotifier: function (name, APIkey) {
      var options = {
        method:'POST',
        endpoint:'notifiers',
        body:{"apiKey":APIkey,"name":name,"provider":"google"}
      };
      this.client().request(options, function (err, data) {
        if (err) {
          console.error(data);
          $rootScope.$broadcast('alert', 'error', 'error creating notifier ');
        } else {
          $rootScope.$broadcast('alert', 'success', 'New notifier created successfully.');
          $rootScope.$broadcast('notifier-update');
        }
      });

    },
    createAppleNotifier: function (file, name, environment, certificatePassword ) {

      var provider = 'apple';

      var formData = new FormData();
      formData.append("p12Certificate", file);

      formData.append('name', name);
      formData.append('provider', provider);
      formData.append('environment', environment);
      formData.append('certificatePassword', certificatePassword);

      var options = {
        method:'POST',
        endpoint:'notifiers',
        body:'{"apiKey":APIkey,"name":name,"provider":"google"}',
        formData:formData
      };
      this.client().request(options, function (err, data) {
        if (err) {
          console.error(data);
          $rootScope.$broadcast('alert', 'error', 'error creating notifier.' );
        } else {
          $rootScope.$broadcast('alert', 'success', 'New notifier created successfully.');
          $rootScope.$broadcast('notifier-update');
        }
      });

    },
    deleteNotifier: function (name) {
      var options = {
        method:'DELETE',
        endpoint: 'notifiers/'+name
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error deleting notifier');
        } else {
          $rootScope.$broadcast('notifier-update');
        }
      });

    },
    initializeCurrentUser: function (callback) {
      callback = callback || function(){};
      if($rootScope.currentUser && !$rootScope.currentUser.reset){
        callback($rootScope.currentUser);
        return $rootScope.$broadcast('current-user-initialized', '');
      }
      var options = {
        method:'GET',
        endpoint:'management/users/'+ this.client().get('email'),
        mQuery:true
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'Error getting user info');
        } else {
          $rootScope.currentUser = data.data;
          $rootScope.currentUser.profileImg = utility.get_gravatar($rootScope.currentUser.email);
          $rootScope.userEmail =$rootScope.currentUser.email;
          callback($rootScope.currentUser);
          $rootScope.$broadcast('current-user-initialized', $rootScope.currentUser);
        }
      });
    },

    updateUser: function (user) {
      var body = $rootScope.currentUser;
      body.username = user.username;
      body.name = user.name;
      body.email = user.email;
      var options = {
        method:'PUT',
        endpoint:'management/users/' + user.uuid + '/',
        mQuery:true,
        body:body
      };
      var self = this;
      this.client().request(options, function (err, data) {
        self.client().set('email',user.email);
        self.client().set('username',user.username);
        if (err) {
          return $rootScope.$broadcast('user-update-error',data);
        }
        $rootScope.currentUser.reset = true;
        self.initializeCurrentUser(function(){
          $rootScope.$broadcast('user-update-success', $rootScope.currentUser);
        });
      });
    },

    resetUserPassword: function (user) {
      var pwdata = {};
      pwdata.oldpassword = user.oldPassword;
      pwdata.newpassword = user.newPassword;
      pwdata.username = user.username;
      var options = {
        method:'PUT',
        endpoint:'users/' + pwdata.uuid + '/',
        body:pwdata
      }
      this.client().request(options, function (err, data) {
        if (err) {
         return  $rootScope.$broadcast('alert', 'error', 'Error resetting password');
        }
        //remove old and new password fields so they don't end up as part of the entity object
        $rootScope.currentUser.oldPassword = '';
        $rootScope.currentUser.newPassword = '';
        $rootScope.$broadcast('user-reset-password-success', $rootScope.currentUser);
      });

    },
    getOrgCredentials: function () {
      var options = {
        method:'GET',
        endpoint:'management/organizations/'+this.client().get('orgName')+'/credentials',
        mQuery:true
      };
      this.client().request(options, function (err, data) {
        if (err && data.credentials) {
          $rootScope.$broadcast('alert', 'error', 'Error getting credentials');
        } else {
          $rootScope.$broadcast('org-creds-updated', data.credentials);
        }
      });
    },
    regenerateOrgCredentials: function () {
      var self = this;
      var options = {
        method:'POST',
        endpoint:'management/organizations/'+ this.client().get('orgName') + '/credentials',
        mQuery:true
      };
      this.client().request(options, function(err, data) {
        if (err && data.credentials) {
          $rootScope.$broadcast('alert', 'error', 'Error regenerating credentials');
        } else {
          $rootScope.$broadcast('alert', 'success', 'Regeneration of credentials complete.');
          $rootScope.$broadcast('org-creds-updated', data.credentials);
        }
      });
    },
    getAppCredentials: function () {
      var options = {
        method:'GET',
        endpoint:'credentials'
      };
      this.client().request(options, function (err, data) {
        if (err && data.credentials) {
          $rootScope.$broadcast('alert', 'error', 'Error getting credentials');
        } else {
          $rootScope.$broadcast('app-creds-updated', data.credentials);
        }
      });
    },

    regenerateAppCredentials: function () {
      var self = this;
      var options = {
        method:'POST',
        endpoint:'credentials'
      };
      this.client().request(options, function(err, data) {
        if (err && data.credentials) {
          $rootScope.$broadcast('alert', 'error', 'Error regenerating credentials');
        } else {
          $rootScope.$broadcast('alert', 'success', 'Regeneration of credentials complete.');
          $rootScope.$broadcast('app-creds-updated', data.credentials);
        }
      });
    },

    signUpUser: function(orgName,userName,name,email,password){
      var formData = {
        "organization": orgName,
        "username": userName,
        "name": name,
        "email": email,
        "password": password
      };
      var options = {
        method:'POST',
        endpoint:'management/organizations',
        body:formData,
        mQuery:true
      };
      var client = this.client();
      client.request(options, function(err, data) {
        if (err) {
          $rootScope.$broadcast('register-error', data);
        } else {
          $rootScope.$broadcast('register-success',data);
        }
      });
    },
    resendActivationLink: function(id){
      var options = {
        method: 'GET',
        endpoint: 'management/users/'+id+'/reactivate',
        mQuery:true
      };
      this.client().request(options, function (err, data) {
        if (err) {
          $rootScope.$broadcast('resend-activate-error', data);
        } else {
          $rootScope.$broadcast('resend-activate-success',data);
        }
      });
    },
    getAppSettings: function(){
      $rootScope.$broadcast('app-settings-received',{});
    },
    getActivities: function(){
        this.client().request({method:'GET',endpoint:'activities', qs:{limit:200}},function(err,data){
          if(err) return $rootScope.$broadcast('app-activities-error',data);
          var entities = data.entities;
          //set picture if there is none and change gravatar to secure
          entities.forEach(function(entity) {
            if (!entity.actor.picture) {
              entity.actor.picture = window.location.protocol+ "//" + window.location.host + window.location.pathname + "img/user_profile.png"
            } else {
              entity.actor.picture = entity.actor.picture.replace(/^http:\/\/www.gravatar/i, 'https://secure.gravatar');
              //note: changing this to use the image on apigee.com - since the gravatar default won't work on any non-public domains such as localhost
              //this_data.picture = this_data.picture + encodeURI("?d="+window.location.protocol+"//" + window.location.host + window.location.pathname + "images/user_profile.png");
              if (~entity.actor.picture.indexOf('http')) {
                entity.actor.picture = entity.actor.picture;
              } else {
                entity.actor.picture = 'https://apigee.com/usergrid/img/user_profile.png';
              }
            }
        });
          $rootScope.$broadcast('app-activities-received',data.entities);
        });
    },
    getEntityActivities: function(entity){
        var endpoint = entity.get('type') + '/' + entity.get('uuid') + '/activities' ;
        var options = {
          method:'GET',
          endpoint:endpoint,
          qs:{limit:200}
        };
        this.client().request(options, function (err, data) {
          if(err){
            $rootScope.$broadcast(entity.get('type')+'-activities-error',data);
          }
          data.entities.forEach(function(entityInstance) {
            entityInstance.createdDate = (new Date( entityInstance.created)).toUTCString();
          });
          $rootScope.$broadcast(entity.get('type')+'-activities-received',data.entities);
        });
    },
    addUserActivity:function(user,content){
      var options = {
        "actor": {
        "displayName": user.get('username'),
        "uuid": user.get('uuid'),
        "username":user.get('username')
          },
        "verb": "post",
        "content": content
      };
      this.client().createUserActivity(user.get('username'), options, function(err, activity) { //first argument can be 'me', a uuid, or a username
        if (err) {
          $rootScope.$broadcast('user-activity-add-error', err);
        } else {
          $rootScope.$broadcast('user-activity-add-success', activity);
        }
      });
    },
    runShellQuery:function(method,path,payload){
      var options = {
        "verb": method,
          "endpoint":path
      };
      if(payload){
        options["body"]=payload;
      }
      this.client().request(options,function(err,data){
        if(err) {
          $rootScope.$broadcast('shell-error', data);
        }else{
          $rootScope.$broadcast('shell-success', data);
        }
      });
    },
    addOrganization:function(user,orgName){
      var options = {
        method: 'POST',
        endpoint: 'management/users/'+user.uuid+'/organizations',
        body:{organization:orgName},
        mQuery:true
          }, client = this.client(),self=this;
      client.request(options,function(err,data){
        if(err){
          $rootScope.$broadcast('user-add-org-error', data);
        }else{
          $rootScope.$broadcast('user-add-org-success', $rootScope.organizations);
        }
      });
    },
    leaveOrganization:function(user,org){
      var options = {
        method: 'DELETE',
        endpoint: 'management/users/'+user.uuid+'/organizations/'+org.uuid,
        mQuery:true
      }
      this.client().request(options,function(err,data){
        if(err){
          $rootScope.$broadcast('user-leave-org-error', data);
        }else{
          delete  $rootScope.organizations[org.name];
          $rootScope.$broadcast('user-leave-org-success', $rootScope.organizations);
        }
      });
    }
  }
});
