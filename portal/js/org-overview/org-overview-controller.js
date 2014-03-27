'use strict'

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