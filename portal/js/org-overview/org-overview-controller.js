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

AppServices.Controllers.controller('OrgOverviewCtrl', ['ug', 'help', '$scope','$rootScope', '$routeParams', '$location', function (ug, help, $scope, $rootScope, $routeParams, $location) {

  var init = function(oldOrg){
    //deal with double firing initialization logic
    var orgName = $scope.currentOrg;
    var orgUUID = '';
    if (orgName &&  $scope.organizations[orgName]) {
      orgUUID = $scope.organizations[orgName].uuid;
    } else {
      console.error('Your current user is not authenticated for this organization.');
      setTimeout(function(){
        $rootScope.$broadcast('change-org',oldOrg || $scope.organizations[Object.keys($scope.organizations)[0]].name );
      },1000)
      return;
    }
    $scope.currentOrganization = {"name":orgName,"uuid":orgUUID};
    $scope.applications = [{"name":"...", "uuid":"..."}];
    $scope.orgAdministrators = [];
    $scope.activities = [];
    $scope.orgAPICredentials = {"client_id":"...", "client_secret":"..."};
    $scope.admin = {};
    $scope.newApp = {};

    ug.getApplications();
    ug.getOrgCredentials();
    ug.getAdministrators();
    ug.getFeed();
  }

  $scope.$on('org-changed',function(args, oldOrg,newOrg){
     init(oldOrg);
  });

  $scope.$on('app-initialized',function(){
    init();
  })

  //-------modal logic
  $scope.regenerateCredentialsDialog =  function(modalId){
    $scope.orgAPICredentials = {client_id:'regenerating...',client_secret:'regenerating...'};
    ug.regenerateOrgCredentials();
    $scope.hideModal(modalId);
  };

  $scope.newAdministratorDialog = function(modalId){
    //todo: put more validate here
    if ($scope.admin.email) {
      ug.createAdministrator($scope.admin.email);
      $scope.hideModal(modalId);
      $rootScope.$broadcast('alert', 'success', 'Administrator created successfully.');
    } else {
      $rootScope.$broadcast('alert', 'error', 'You must specify an email address.');
    }

  };

  $scope.$on('applications-received', function(event, applications) {
    $scope.applications = applications;
    $scope.applyScope();
  });

  $scope.$on('administrators-received', function(event, administrators) {
    $scope.orgAdministrators = administrators;
    $scope.applyScope();

  });

  $scope.$on('org-creds-updated', function(event, credentials) {
    $scope.orgAPICredentials = credentials;
    $scope.applyScope();

  });

  $scope.$on('feed-received', function(event, feed) {
    $scope.activities = feed;
    $scope.applyScope();

  });

  if($scope.activeUI){
    init();
  }

}]);