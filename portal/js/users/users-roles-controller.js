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

AppServices.Controllers.controller('UsersRolesCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.rolesSelected = 'active';
    $scope.usersRolesSelected = false;
    $scope.usersPermissionsSelected = false;
    $scope.name = '';
    $scope.master = '';
    $scope.hasRoles = $scope.hasPermissions = false;
    $scope.permissions = {};

    var clearPermissions = function(){
      if($scope.permissions){
        $scope.permissions.path = '';
        $scope.permissions.getPerm = false;
        $scope.permissions.postPerm = false;
        $scope.permissions.putPerm = false;
        $scope.permissions.deletePerm = false;
      }
      $scope.applyScope()
    };

    var clearRole = function(){
      $scope.name = '';
      $scope.applyScope();
    };

    ug.getRolesTypeAhead();

    $scope.addUserToRoleDialog = function(modalId){
      if ($scope.name) {
        var username =  $rootScope.selectedUser.get('uuid');
        ug.addUserToRole(username, $scope.name);
        $scope.hideModal(modalId);
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a role.');
      }
    };

    $scope.leaveRoleDialog = function(modalId){
      var username =  $rootScope.selectedUser.get('uuid');
      var roles = $rootScope.selectedUser.roles;
      for (var i=0;i<roles.length;i++) {
        if (roles[i].checked) {
          ug.removeUserFromRole(username, roles[i].name);
        }
      }
      $scope.hideModal(modalId);
    };

    $scope.deletePermissionDialog = function(modalId){
      var username =  $rootScope.selectedUser.get('uuid');
      var permissions = $rootScope.selectedUser.permissions;
      for (var i=0;i<permissions.length;i++) {
        if (permissions[i].checked) {
          ug.deleteUserPermission(permissions[i].perm, username);
        }
      }
      $scope.hideModal(modalId);
    };


    $scope.addUserPermissionDialog = function(modalId){
      if ($scope.permissions.path) {
        var permission = $scope.createPermission(null,null,$scope.removeFirstSlash($scope.permissions.path),$scope.permissions);
        var username =  $rootScope.selectedUser.get('uuid');
        ug.newUserPermission(permission, username);
        $scope.hideModal(modalId);
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a name for the permission.');
      }
    }


    if (!$rootScope.selectedUser) {
      $location.path('/users');
      return;
    } else {
      $rootScope.selectedUser.permissions = [];
      $rootScope.selectedUser.roles = [];
      $rootScope.selectedUser.getPermissions(function(err, data){
        $scope.clearCheckbox('permissionsSelectAllCheckBox');
        if (err) {

        } else {
          $scope.hasPermissions = data.data.length > 0;
          $scope.applyScope();
        }

      });

      $rootScope.selectedUser.getRoles(function(err, data){
        if (err) {

        } else {
          $scope.hasRoles = data.entities.length > 0;
          $scope.applyScope();
        }

      });

      $scope.$on('role-update-received', function(event) {

        $rootScope.selectedUser.getRoles(function(err, data){
          $scope.usersRolesSelected = false;

          if (err) {

          } else {
            $scope.hasRoles = data.entities.length > 0;
            clearRole();
            $scope.applyScope();
          }

        });

      });

      $scope.$on('permission-update-received', function(event) {

        $rootScope.selectedUser.getPermissions(function(err, data){
          $scope.usersPermissionsSelected = false;
          if (err) {
          } else {
            clearPermissions();
            $scope.hasPermissions = data.data.length > 0;
            $scope.applyScope();
          }

        });

      });
    }


  }]);