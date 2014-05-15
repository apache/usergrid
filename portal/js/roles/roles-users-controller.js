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

AppServices.Controllers.controller('RolesUsersCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.usersSelected = 'active';
    $scope.previous_display = 'none';
    $scope.next_display = 'none';
    $scope.user = {};
    $scope.master = '';
    $scope.hasUsers = false;

    //todo find others and combine this into one controller
    ug.getUsersTypeAhead();

    $scope.usersTypeaheadValues = [];
    $scope.$on('users-typeahead-received', function(event, users) {
      $scope.usersTypeaheadValues = users;
      $scope.applyScope();

    });

    $scope.addRoleToUserDialog = function(modalId){
      if ($scope.user.uuid) {
        var roleName =  $rootScope.selectedRole._data.uuid;
        ug.addUserToRole($scope.user.uuid, roleName);
        $scope.hideModal(modalId)
        $scope.user = null;
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a user.');
      }
    };

    $scope.removeUsersFromGroupDialog = function(modalId){
      var roleName =  $rootScope.selectedRole._data.uuid;
      var users = $scope.rolesCollection.users._list;
      for (var i=0;i<users.length;i++) {
        if (users[i].checked) {
          ug.removeUserFromRole(users[i]._data.uuid, roleName);
        }
      }
      $scope.hideModal(modalId)
    };



    $scope.get = function() {
      var options = {
        type:'roles/'+$rootScope.selectedRole._data.name +'/users'
      }
      $scope.rolesCollection.addCollection('users', options, function(err) {
        $scope.roleUsersSelected =false;
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting users for role');
        } else {
          $scope.hasUsers = $scope.rolesCollection.users._list.length;
          $scope.checkNextPrev();
         $scope.applyScope();
        }
      });
    }

    $scope.resetNextPrev = function() {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    }
    $scope.checkNextPrev = function() {
      $scope.resetNextPrev();
      if ($scope.rolesCollection.users.hasPreviousPage()) {
        $scope.previous_display = 'block';
      }

      if($scope.rolesCollection.users.hasNextPage()) {
        $scope.next_display = 'block';
      }
    }

    if (!$rootScope.selectedRole) {
      $location.path('/roles');
      return;
    } else {
      $scope.get();
    }

    $scope.getPrevious = function () {
      $scope.rolesCollection.users.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of users');
        }
        $scope.checkNextPrev();
        $scope.applyScope();

      });
    };

    $scope.getNext = function () {
      $scope.rolesCollection.users.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of users');
        }
        $scope.checkNextPrev();
        $scope.applyScope();

      });
    };

    $scope.$on('role-update-received', function(event) {
      $scope.get();
    });


}]);