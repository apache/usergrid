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

AppServices.Controllers.controller('GroupsDetailsCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    var selectedGroup = $rootScope.selectedGroup.clone();
    $scope.detailsSelected = 'active';
    $scope.json = selectedGroup._json || selectedGroup._data.stringifyJSON();
    $scope.group = selectedGroup._data;
    $scope.group.path =  $scope.group.path.indexOf('/')!=0 ? '/'+$scope.group.path : $scope.group.path;
    $scope.group.title = $scope.group.title;

    if (!$rootScope.selectedGroup) {
      $location.path('/groups');
      return;
    }
    $scope.$on('group-selection-changed',function(evt,selectedGroup){
      $scope.group.path =  selectedGroup._data.path.indexOf('/')!=0 ? '/'+selectedGroup._data.path : selectedGroup._data.path;
      $scope.group.title = selectedGroup._data.title;
      $scope.detailsSelected = 'active';
      $scope.json = selectedGroup._json || selectedGroup._data.stringifyJSON();

    });

    $rootScope.saveSelectedGroup = function(){
      $rootScope.selectedGroup._data.title = $scope.group.title;
      $rootScope.selectedGroup._data.path = $scope.removeFirstSlash( $scope.group.path);
      $rootScope.selectedGroup.save(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error saving group');
        } else {
          $rootScope.$broadcast('alert', 'success', 'group saved');
        }
      });

    }

  }]);