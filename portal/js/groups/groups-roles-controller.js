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

AppServices.Controllers.controller('GroupsRolesCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {


    $scope.rolesSelected = 'active';
    $scope.roles_previous_display = 'none';
    $scope.roles_next_display = 'none';
    $scope.name = '';
    $scope.master = '';
    $scope.hasRoles = false;
    $scope.hasPermissions = false;
    $scope.permissions = {};


    $scope.addGroupToRoleDialog = function(modalId){
      if ($scope.name) {
        var path =  $rootScope.selectedGroup.get('path');
        ug.addGroupToRole(path, $scope.name);
        $scope.hideModal(modalId)
        $scope.name='';
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a role name.');
      }
    };


    $scope.leaveRoleDialog = function(modalId){
      var path =  $rootScope.selectedGroup.get('path');
      var roles = $scope.groupsCollection.roles._list;
      for (var i=0;i<roles.length;i++) {
        if (roles[i].checked) {
          ug.removeUserFromGroup(path, roles[i]._data.name);
        }
      }
      $scope.hideModal(modalId)
    };

    $scope.addGroupPermissionDialog = function(modalId){
      if ($scope.permissions.path) {

        var permission = $scope.createPermission(null,null,$scope.removeFirstSlash($scope.permissions.path),$scope.permissions);
        var path =  $rootScope.selectedGroup.get('path');
        ug.newGroupPermission(permission, path);
        $scope.hideModal(modalId)
        if($scope.permissions){
          $scope.permissions = {};
        }
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a name for the permission.');
      }
    };

    $scope.deleteGroupPermissionDialog = function(modalId){
      var path =  $rootScope.selectedGroup.get('path');
      var permissions = $rootScope.selectedGroup.permissions;
      for (var i=0;i<permissions.length;i++) {
        if (permissions[i].checked) {
          ug.deleteGroupPermission(permissions[i].perm, path);
        }
      }
      $scope.hideModal(modalId)
    };

    $scope.resetNextPrev = function() {
      $scope.roles_previous_display = 'none';
      $scope.roles_next_display = 'none';
      $scope.permissions_previous_display = 'none';
      $scope.permissions_next_display = 'none';
    }
    $scope.resetNextPrev();
    $scope.checkNextPrevRoles = function() {
      $scope.resetNextPrev();
      if ($scope.groupsCollection.roles.hasPreviousPage()) {
        $scope.roles_previous_display = 'block';
      }
      if($scope.groupsCollection.roles.hasNextPage()) {
        $scope.roles_next_display = 'block';
      }
    }
    $scope.checkNextPrevPermissions = function() {
      if ($scope.groupsCollection.permissions.hasPreviousPage()) {
        $scope.permissions_previous_display = 'block';
      }
      if($scope.groupsCollection.permissions.hasNextPage()) {
        $scope.permissions_next_display = 'block';
      }
    }

    $scope.getRoles = function() {

      var path = $rootScope.selectedGroup.get('path');
      var options = {
        type:'groups/'+ path +'/roles'
      }
      $scope.groupsCollection.addCollection('roles', options, function(err) {
        $scope.groupRoleSelected = false;
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting roles for group');
        } else {
          $scope.hasRoles = $scope.groupsCollection.roles._list.length > 0;
          $scope.checkNextPrevRoles();
          $scope.applyScope();
        }
      });
    }

    $scope.getPermissions = function() {

      $rootScope.selectedGroup.permissions = [];
      $rootScope.selectedGroup.getPermissions(function(err, data){
        $scope.groupPermissionsSelected = false;
        $scope.hasPermissions = $scope.selectedGroup.permissions.length;
        if (err) {

        } else {
          $scope.applyScope();
        }
      });

    }

    $scope.getPreviousRoles = function () {
      $scope.groupsCollection.roles.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of roles');
        }
        $scope.checkNextPrevRoles();
        $scope.applyScope();
      });
    };
    $scope.getNextRoles = function () {
      $scope.groupsCollection.roles.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of roles');
        }
        $scope.checkNextPrevRoles();
        $scope.applyScope();
      });
    };
    $scope.getPreviousPermissions = function () {
      $scope.groupsCollection.permissions.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of permissions');
        }
        $scope.checkNextPrevPermissions();
        $scope.applyScope();
      });
    };
    $scope.getNextPermissions = function () {
      $scope.groupsCollection.permissions.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of permissions');
        }
        $scope.checkNextPrevPermissions();
        $scope.applyScope();
      });
    };

    $scope.$on('role-update-received', function(event) {
      $scope.getRoles();
    });

    $scope.$on('permission-update-received', function(event) {
      $scope.getPermissions();
    });

    $scope.$on('groups-received',function(evt,data){
      $scope.groupsCollection = data;
      $scope.getRoles();
      $scope.getPermissions();
    })

    if (!$rootScope.selectedGroup) {
      $location.path('/groups');
      return;
    } else {
      ug.getRolesTypeAhead();
      ug.getGroups();
    }


  }]);