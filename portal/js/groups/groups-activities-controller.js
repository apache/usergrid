'use strict'

AppServices.Controllers.controller('GroupsActivitiesCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.activitiesSelected = 'active';

    if (!$rootScope.selectedGroup) {
      $location.path('/groups');
      return;
    } else {
      $rootScope.selectedGroup.activities = [];
      $rootScope.selectedGroup.getActivities(function(err, data){
        if (err) {

        } else {
//          $rootScope.selectedGroup.activities = data;
          if(!$rootScope.$$phase) {
            $rootScope.$apply();
          }
        }

      });
    }


  }]);