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

AppServices.Controllers.controller('GroupsCtrl', ['ug', '$scope', '$rootScope', '$location', '$route',
  function (ug, $scope, $rootScope, $location, $route) {

    $scope.groupsCollection = {};
    $rootScope.selectedGroup = {};
    $scope.previous_display = 'none';
    $scope.next_display = 'none';
    $scope.hasGroups = false;
    $scope.newGroup = {path:'',title:''}

    ug.getGroups();

    $scope.currentGroupsPage = {};
//  $scope.$route = $route;

    $scope.selectGroupPage = function(route){
      //lokup the template URL with the route. trying to preserve routes in the markup and not hard link to .html
      $scope.currentGroupsPage.template = $route.routes[route].templateUrl;
      $scope.currentGroupsPage.route = route;
    }

    $scope.newGroupDialog = function(modalId,form){
      //todo: put more validate here
      if ($scope.newGroup.path && $scope.newGroup.title) {
        //$scope.path = $scope.path.replace(' ','');
        ug.createGroup($scope.removeFirstSlash($scope.newGroup.path), $scope.newGroup.title);
        $scope.hideModal(modalId);
        $scope.newGroup = {path:'',title:''}
      } else {
        $rootScope.$broadcast('alert', 'error', 'Missing required information.');
      }
    };

    $scope.deleteGroupsDialog = function(modalId){
      $scope.deleteEntities($scope.groupsCollection, 'group-deleted', 'error deleting group');
      $scope.hideModal(modalId);
      $scope.newGroup = {path:'',title:''}
    };
    $scope.$on('group-deleted',function(){
      $rootScope.$broadcast('alert', 'success', 'Group deleted successfully.');
    });
    $scope.$on('group-deleted-error',function(){
      ug.getGroups();
    });

    $scope.$on("groups-create-success",function(){
      $rootScope.$broadcast('alert', 'success', 'Group created successfully.');
    });

    $scope.$on("groups-create-error",function(){
      $rootScope.$broadcast('alert', 'error', 'Error creating group. Make sure you don\'t have spaces in the path.');
    });

    $scope.$on('groups-received', function(event, groups) {
      $scope.groupBoxesSelected = false;
      $scope.groupsCollection = groups;
      $scope.newGroup.path = '';
      $scope.newGroup.title = '';
      if(groups._list.length > 0 && (!$rootScope.selectedGroup._data || !groups._list.some(function(group){ return $rootScope.selectedGroup._data.uuid === group._data.uuid }))){ // if groups have been received already do not reselect
        $scope.selectGroup(groups._list[0]._data.uuid)
      }
      $scope.hasGroups = groups._list.length > 0;
      $scope.received = true;

      $scope.checkNextPrev();
      $scope.applyScope();
    });

    $scope.resetNextPrev = function() {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    }
    $scope.checkNextPrev = function() {
      $scope.resetNextPrev();
      if ($scope.groupsCollection.hasPreviousPage()) {
        $scope.previous_display = 'block';
      }
      if($scope.groupsCollection.hasNextPage()) {
        $scope.next_display = 'block';
      }
    }

    $scope.selectGroup = function(uuid){
      $rootScope.selectedGroup = $scope.groupsCollection.getEntityByUUID(uuid);
      $scope.currentGroupsPage.template = 'groups/groups-details.html';
      $scope.currentGroupsPage.route = '/groups/details';
      $rootScope.$broadcast('group-selection-changed', $rootScope.selectedGroup);
    }

    $scope.getPrevious = function () {
      $scope.groupsCollection.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of groups');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    $scope.getNext = function () {

      $scope.groupsCollection.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of groups');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    $scope.$on('group-deleted', function(event) {
      $route.reload();
      $scope.master = '';
    });


  }]);