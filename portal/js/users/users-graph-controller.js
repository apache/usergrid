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
AppServices.Controllers.controller('UsersGraphCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.graphSelected = 'active';
    $scope.user = '';

    //todo find others and combine this into one controller
    ug.getUsersTypeAhead();



    $scope.followUserDialog = function(modalId){
      if ($scope.user) {
        ug.followUser($scope.user.uuid);
        $scope.hideModal(modalId);
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a user to follow.');
      }
    };

    if (!$rootScope.selectedUser) {
      $location.path('/users');
      return;
    } else {
      $rootScope.selectedUser.activities = [];
      $rootScope.selectedUser.getFollowing(function(err, data){
        if (err) {

        } else {
          if(!$rootScope.$$phase) {
            $rootScope.$apply();
          }
        }

      });

      $rootScope.selectedUser.getFollowers(function(err, data){
        if (err) {

        } else {
          if(!$rootScope.$$phase) {
            $rootScope.$apply();
          }
        }

      });

      $scope.$on('follow-user-received', function(event) {

        $rootScope.selectedUser.getFollowing(function(err, data){
          if (err) {

          } else {
            if(!$rootScope.$$phase) {
              $rootScope.$apply();
            }
          }

        });

      });
    }


  }]);