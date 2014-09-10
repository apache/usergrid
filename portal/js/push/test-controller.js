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
 
AppServices.Controllers.controller('TestCtrl', ['ug', '$scope', '$rootScope', '$routeParams', '$location', function (ug, $scope, $rootScope, $routeParams, $location) {
 
  $scope.login = function() {
    $rootScope.currentPath = '/login';
    var username = $scope.login_username;
    var password = $scope.login_password;

    alert(username);
/*
    ug.client().orgLogin(username, password, function (err, data, user, organizations, applications) {
      if (err){
        //let the user know the login was not valid
        $scope.loginMessage = "Error: the username / password combination was not valid";
        if(!$scope.$$phase) {
          $scope.$apply();
        }
      } else {
        $rootScope.$broadcast('loginSuccesful', user, organizations, applications);
      }
    });
    */
  }

  $rootScope.$on('userNotAuthenticated', function(event) {
    $location.path('/login');
    if(!$rootScope.$$phase) {
      $rootScope.$apply();
    }
  });

  $scope.$on('loginSuccesful', function(event, user, organizations, applications) {

    //update org and app dropdowns

    $rootScope.userEmail = user.get('email');

    $rootScope.organizations = ug.client().getObject('organizations');
    $rootScope.applications = ug.client().getObject('applications');
    $rootScope.currentOrg = ug.client().get('orgName');
    $rootScope.currentApp = ug.client().get('appName');


    //if on login page, send to org overview page.  if on a different page, let them stay there
    if ($rootScope.currentPath === '/login' || $rootScope.currentPath === '/login/loading') {
      $location.path('/org-overview');
    } else {
      $location.path($rootScope.currentPath);
    }
    if(!$scope.$$phase) {
      $scope.$apply();
    }
  });
  $scope.$on('reauthSuccesful', function(event) {

    $rootScope.organizations = ug.client().getObject('organizations');
    $rootScope.applications = ug.client().getObject('applications');
    $rootScope.currentOrg = ug.client().get('orgName');
    $rootScope.currentApp = ug.client().get('appName');

    //if on login page, send to org overview page.  if on a different page, let them stay there
    if ($rootScope.currentPath === '/login') {
      $location.path('/org-overview');
    } else {
      $location.path($rootScope.currentPath);
    }
    if(!$scope.$$phase) {
      $scope.$apply();
    }

  });

}]);