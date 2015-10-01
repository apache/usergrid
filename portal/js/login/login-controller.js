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

AppServices.Controllers.controller('LoginCtrl', ['ug', '$scope', '$rootScope', '$routeParams', '$location', 'utility', function (ug, $scope, $rootScope, $routeParams, $location, utility) {

  $scope.loading = false;
  $scope.login = {};
  $scope.activation = {};
  $scope.requiresDeveloperKey=$scope.options.client.requiresDeveloperKey||false;
  if(!$scope.requiresDeveloperKey && $scope.options.client.apiKey){
    ug.setClientProperty('developerkey', $scope.options.client.apiKey);
  }
  $rootScope.gotoForgotPasswordPage = function(){
    $location.path("/forgot-password");
  };

  $rootScope.gotoSignUp = function(){
    $location.path("/register");
  };

  $scope.login = function() {
//    $rootScope.currentPath = '/login';
    var username = $scope.login.username;
    var password = $scope.login.password;
    $scope.loginMessage = "";
    $scope.loading = true;
    if($scope.requiresDeveloperKey){
      ug.setClientProperty('developerkey', $scope.login.developerkey);
    }

    ug.orgLogin(username, password);

  }
  $scope.$on('loginFailed',function(event, err, data){
    $scope.loading = false;
    //let the user know the login was not valid
    ug.setClientProperty('developerkey', null);
    var errorMessage="An error occurred while attempting to authenticate";
    if (data instanceof XMLHttpRequestProgressEvent){
      $scope.loginMessage = "Error: An error occurred while connecting to "+Usergrid.overrideUrl;
    }else if(status == 400){
      $scope.loginMessage = "Error: the username / password combination was not valid";
    } else {
      $scope.loginMessage = "Error: "+((data.error_description.length>1)?data.error_description:errorMessage);
    }
      console.log("LOGIN RESPONSE", err, data);

    $scope.applyScope();
  });

  $scope.logout = function() {
    ug.logout();
    ug.setClientProperty('developerkey', null);
    if($scope.use_sso){
      window.location = $rootScope.urls().LOGOUT_URL + '?redirect=no&callback=' +  encodeURIComponent($location.absUrl().split('?')[0]);
    }else{
      $location.path('/login');
      $scope.applyScope();
    }
  }

  $rootScope.$on('userNotAuthenticated', function(event) {
    if("/forgot-password"!==$location.path()){
      $location.path('/login');
      $scope.logout();
    }
    $scope.applyScope();
  });


  $scope.$on('loginSuccesful', function(event, user, organizations, applications) {
    $scope.loading = false;
    $scope.login = {};

    // get the first app from logged in user and set this prop in the app for any initial app specific requests
    var firstOrg = Object.keys($rootScope.currentUser.organizations)[0];
    var firstApp = Object.keys($rootScope.currentUser.organizations[firstOrg].applications)[0];
    ug.setClientProperty('appName', firstApp.split("/")[1]);

    //if on login page, send to org overview page.  if on a different page, let them stay there
    if ($rootScope.currentPath === '/login' || $rootScope.currentPath === '/login/loading' || typeof $rootScope.currentPath === 'undefined') {
      $location.path('/org-overview');
    } else {
      $location.path($rootScope.currentPath);
    }
    $scope.applyScope();
  });

  $scope.resendActivationLink = function(modalId){
    var id = $scope.activation.id;
    ug.resendActivationLink(id);
    $scope.activation = {};
    $scope.hideModal(modalId);
  };

  $scope.$on('resend-activate-success',function(evt,data){
    $scope.activationId = '';
    $scope.$apply();
    $rootScope.$broadcast('alert', 'success', 'Activation link sent successfully.');
  });

  $scope.$on('resend-activate-error',function(evt,data){
    $rootScope.$broadcast('alert', 'error', 'Activation link failed to send.');
  });

}]);
