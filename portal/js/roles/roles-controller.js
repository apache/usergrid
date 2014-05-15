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

AppServices.Controllers.controller('RolesCtrl', ['ug', '$scope', '$rootScope', '$location', '$route',
  function (ug, $scope, $rootScope, $location, $route) {

    $scope.rolesCollection = {};
    $rootScope.selectedRole = {};
    $scope.previous_display = 'none';
    $scope.next_display = 'none';
    $scope.roles_check_all = '';
    $scope.rolename = '';
    $scope.hasRoles = false;
    $scope.newrole = {};

    $scope.currentRolesPage = {};
//  $scope.$route = $route;

    $scope.selectRolePage = function(route){
      //lokup the template URL with the route. trying to preserve routes in the markup and not hard link to .html
      $scope.currentRolesPage.template = $route.routes[route].templateUrl;
      $scope.currentRolesPage.route = route;
    }

    ug.getRoles();

    $scope.newRoleDialog = function(modalId){
      if ($scope.newRole.name) {
        ug.createRole($scope.newRole.name,$scope.newRole.title);
        //add new role here
        $rootScope.$broadcast('alert', 'success', 'Role created successfully.');
        $scope.hideModal(modalId)
        $scope.newRole = {};
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a role name.');
      }
    };

    $scope.deleteRoleDialog = function(modalId){
      //delete role here
      $scope.deleteEntities($scope.rolesCollection, 'role-deleted', 'error deleting role');
      $scope.hideModal(modalId)
    };
    $scope.$on('role-deleted',function(){
      $rootScope.$broadcast('alert', 'success', 'Role deleted successfully.');
      $scope.master = '';
      $scope.newRole = {};
    });
    $scope.$on('role-deleted-error',function(){
      ug.getRoles();
    });
    $scope.$on('roles-received', function(event, roles) {
      $scope.rolesSelected = false;
      $scope.rolesCollection = roles;
      $scope.newRole = {};


      if(roles._list.length > 0){
        $scope.hasRoles = true;
        $scope.selectRole(roles._list[0]._data.uuid)
      }

      $scope.checkNextPrev();
      $scope.applyScope();

    });

    $scope.resetNextPrev = function() {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    }
    $scope.checkNextPrev = function() {
      $scope.resetNextPrev();
      if ($scope.rolesCollection.hasPreviousPage()) {
        $scope.previous_display = 'block';
      }

      if($scope.rolesCollection.hasNextPage()) {
        $scope.next_display = 'block';
      }
    }

    $scope.selectRole = function(uuid){
      $rootScope.selectedRole = $scope.rolesCollection.getEntityByUUID(uuid);
      $scope.currentRolesPage.template = 'roles/roles-settings.html';
      $scope.currentRolesPage.route = '/roles/settings';
      $rootScope.$broadcast('role-selection-changed', $rootScope.selectedRole);
    }

    $scope.getPrevious = function () {
      $scope.rolesCollection.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of roles');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    $scope.getNext = function () {
      $scope.rolesCollection.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of roles');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };




  }]);