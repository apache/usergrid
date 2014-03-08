'use strict'

AppServices.Controllers.controller('OrgCtrl', ['$scope', '$rootScope', 'ug', 'utility',
  function ($scope, $rootScope, ug,utility) {
    $scope.org = {};
    $scope.currentOrgPage = {};
    var createOrgsArray = function () {
      var orgs = [];
      for (var org in $scope.organizations) {
        orgs.push($scope.organizations[org]);
      }
      $scope.orgs = orgs;
      $scope.selectOrganization(orgs[0])
    }

    $scope.selectOrganization = function(org){
      org.usersArray = [];
      for(var user in org.users){
        org.usersArray.push(org.users[user]);
      }
      org.applicationsArray = [];
      for(var app in org.applications){
        org.applicationsArray.push({name:app.replace(org.name+'/',''),uuid:org.applications[app]});
      }
      $scope.selectedOrg =org;
      $scope.applyScope();
      return false;
    }

    $scope.addOrganization = function(modalId){
      $scope.hideModal(modalId);
      ug.addOrganization($rootScope.currentUser,$scope.org.name);
    }

    $scope.$on('user-add-org-success',function(evt,orgs){
      $scope.org = {};
      $scope.applyScope();
      ug.reAuthenticate($rootScope.userEmail,'org-reauthenticate');//call reauthenticate to refresh events

      $rootScope.$broadcast('alert', 'success', 'successfully added the new organization.');
    });

    $scope.$on('user-add-org-error',function(evt,data){
      $rootScope.$broadcast('alert', 'error', 'An error occurred attempting to add the organization.');
    });

    $scope.$on('org-reauthenticate-success',function(){
      createOrgsArray();
      $scope.applyScope();
    });

    $scope.doesOrgHaveUsers = function(org){
      var test =  org.usersArray.length > 1;
      return test;
    };

    $scope.leaveOrganization = function (org) {
      ug.leaveOrganization($rootScope.currentUser, org);
    };

    $scope.$on('user-leave-org-success', function (evt, orgs) {
      ug.reAuthenticate($rootScope.userEmail,'org-reauthenticate');//call reauthenticate to refresh events
      $rootScope.$broadcast('alert', 'success', 'User has left the selected organization(s).');
    });

    $scope.$on('user-leave-org-error',function(evt,data){
      $rootScope.$broadcast('alert', 'error', 'An error occurred attempting to leave the selected organization(s).');

    });
    createOrgsArray();

  }]
);