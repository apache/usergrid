'use strict'

AppServices.Controllers.controller('UsersActivitiesCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.activitiesSelected = 'active';
    $scope.activityToAdd = '';
    $scope.activities = [];
    $scope.newActivity = {};
    var getActivities = function(){
      ug.getEntityActivities($rootScope.selectedUser);
    };

    if (!$rootScope.selectedUser) {
      $location.path('/users');
      return;
    } else {
      getActivities();
    }
    $scope.addActivityToUserDialog = function(modalId){
      ug.addUserActivity($rootScope.selectedUser,$scope.newActivity.activityToAdd);
      $scope.hideModal(modalId)
      $scope.newActivity = {};
    };
    $scope.$on('user-activity-add-error',function(){
      $rootScope.$broadcast('alert', 'error', 'could not create activity');
    });
    $scope.$on('user-activity-add-success',function(){
      $scope.newActivity.activityToAdd = '';
      getActivities();
    });
    $scope.$on('users-activities-error',function(){
      $rootScope.$broadcast('alert', 'error', 'could not create activity');
    });
    $scope.$on('users-activities-received',function(evt,entities){
      $scope.activities = entities;
      $scope.applyScope();
    });

  }]);
