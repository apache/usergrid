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

AppServices.Controllers.controller('ProfileCtrl', ['$scope', '$rootScope', 'ug', 'utility',
  function ($scope, $rootScope, ug,utility) {

    $scope.loading = false;

    $scope.saveUserInfo = function(){
      $scope.loading = true;
      ug.updateUser($scope.user);
    };

    $scope.$on('user-update-error',function(){
      $scope.loading = false;
      $rootScope.$broadcast('alert', 'error', 'Error updating user info');
    });

    $scope.$on('user-update-success',function(){
      $scope.loading = false;
      $rootScope.$broadcast('alert', 'success', 'Profile information updated successfully!');
      if($scope.user.oldPassword && $scope.user.newPassword != 'undefined'){
        //update password after userinfo update
        ug.resetUserPassword($scope.user);
      }
    });

    $scope.$on('user-reset-password-success',function(){
      $rootScope.$broadcast('alert', 'success', 'Password updated successfully!');
      $scope.user = $rootScope.currentUser.clone();
    });
    $scope.$on('app-initialized',function(){
      $scope.user = $rootScope.currentUser.clone();
    });

    if($rootScope.activeUI){
      $scope.user = $rootScope.currentUser.clone();
      $scope.applyScope();
    }

  }]);