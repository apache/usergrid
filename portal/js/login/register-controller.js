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

AppServices.Controllers.controller('RegisterCtrl', ['ug', '$scope', '$rootScope', '$routeParams', '$location', 'utility', function (ug, $scope, $rootScope, $routeParams, $location, utility) {
  $rootScope.activeUI &&   $location.path('/');

  var init = function () {
    $scope.registeredUser = {};
  }

  init();

  $scope.cancel = function(){
    $location.path('/');
  }
  $scope.register = function () {
    var user = $scope.registeredUser.clone();
    if (user.password === user.confirmPassword) {
      ug.signUpUser(user.orgName, user.userName, user.name, user.email, user.password);
    }else{
      $rootScope.$broadcast('alert', 'error', 'Passwords do not match.' + name);
    }
  };

  $scope.$on('register-error', function (event, data) {
    $scope.signUpSuccess = false;
    $rootScope.$broadcast('alert', 'error', 'Error registering: ' + (data && data.error_description ? data.error_description : name));
  });
  $scope.$on('register-success', function (event, data) {
    $scope.registeredUser = {};
    $scope.signUpSuccess = true;
    init();
    $scope.$apply();
  });


}]);
