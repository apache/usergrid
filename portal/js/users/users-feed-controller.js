'use strict'

AppServices.Controllers.controller('UsersFeedCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.activitiesSelected = 'active';
    $scope.activityToAdd = '';
    $scope.activities = [];
    $scope.newActivity = {};
    var getFeed = function(){
      ug.getEntityActivities($rootScope.selectedUser,true);
    };

    if (!$rootScope.selectedUser) {
      $location.path('/users');
      return;
    } else {
      getFeed();
    }

    $scope.$on('users-feed-error',function(){
      $rootScope.$broadcast('alert', 'error', 'could not create activity');
    });
    $scope.$on('users-feed-received',function(evt,entities){
      $scope.activities = entities;
      $scope.applyScope();
    });

  }]);
