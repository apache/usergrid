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
'use strict'
AppServices.Controllers.controller('UsersCtrl', ['ug', '$scope', '$rootScope', '$location', '$route',
  function (ug, $scope, $rootScope, $location, $route) {

    $scope.newUser = {};
    $scope.usersCollection = {};
    $rootScope.selectedUser = {};
    $scope.previous_display = 'none';
    $scope.next_display = 'none';

    $scope.hasUsers = false;
    $scope.currentUsersPage = {};

    $scope.selectUserPage = function (route) {
      //lokup the template URL with the route. trying to preserve routes in the markup and not hard link to .html
      $scope.currentUsersPage.template = $route.routes[route].templateUrl;
      $scope.currentUsersPage.route = route;
      clearNewUserForm();

    }
    //-----modals
    $scope.deleteUsersDialog = function (modalId) {
      $scope.deleteEntities($scope.usersCollection, 'user-deleted', 'error deleting user');
      $scope.hideModal(modalId);
      clearNewUserForm();
    }

    $scope.$on('user-deleted-error', function () {
      ug.getUsers();
    });
    var clearNewUserForm = function(){
      $scope.newUser ={};
    };
    $scope.newUserDialog = function (modalId) {
      //todo: put more validate here
      switch(true){
        case $scope.newUser.newpassword !== $scope.newUser.repassword :
          $rootScope.$broadcast("alert", "error", "Passwords do not match.");
          break;
        default:
          ug.createUser($scope.newUser.newusername, $scope.newUser.name, $scope.newUser.email, $scope.newUser.newpassword);
          $scope.hideModal(modalId);
          clearNewUserForm();
          break;
      }
    };

    ug.getUsers();
    function sanitizeUsersCollection(){
      var cleanProperties = ["password", "oldpassword", "newpassword"]
      if($scope.usersCollection._list.length > 0){
        $scope.usersCollection._list.forEach(function(user, i){
          Object.keys(user._data).forEach(function(key){
            if(cleanProperties.indexOf(key.toLowerCase()) !== -1){
              // console.warn("Removing %s from %s", key, user._data.uuid);
              delete user._data[key]
            }
          });
          $scope.usersCollection._list[i]=user;
        })
      }
    }

    $scope.$on('users-received', function(event, users) {
      $scope.usersCollection = users;
      $scope.usersSelected = false;
      $scope.hasUsers = users._list.length;
      sanitizeUsersCollection();
      if(users._list.length > 0){
        $scope.selectUser(users._list[0]._data.uuid)
      }

      $scope.checkNextPrev();
      $scope.applyScope();
    });

    $scope.$on('users-create-success', function () {
      sanitizeUsersCollection();
      $rootScope.$broadcast("alert", "success", "User successfully created.");
    });

    $scope.resetNextPrev = function () {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    };

    $scope.checkNextPrev = function () {
      $scope.resetNextPrev();
      if ($scope.usersCollection.hasPreviousPage()) {
        $scope.previous_display = '';
      }
      if ($scope.usersCollection.hasNextPage()) {
        $scope.next_display = '';
      }
    };

    $scope.selectUser = function (uuid) {
      $rootScope.selectedUser = $scope.usersCollection.getEntityByUUID(uuid);
//    $location.path('/users/profile');
      $scope.currentUsersPage.template = 'users/users-profile.html';
      $scope.currentUsersPage.route = '/users/profile';
      $rootScope.$broadcast('user-selection-changed', $rootScope.selectedUser);
    };

    $scope.getPrevious = function () {
      $scope.usersCollection.getPreviousPage(function (err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of users');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    $scope.getNext = function () {
      $scope.usersCollection.getNextPage(function (err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of users');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    $scope.$on('user-deleted', function (event) {
      $rootScope.$broadcast('alert', 'success', 'User deleted successfully.');
      ug.getUsers();
    });
  }]);
