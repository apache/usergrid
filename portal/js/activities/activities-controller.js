AppServices.Controllers.controller('ActivitiesCtrl', ['ug', '$scope', '$rootScope', '$location','$route',
  function (ug, $scope, $rootScope, $location, $route) {
    $scope.$on('app-activities-received',function(evt,data){
      $scope.activities = data;
      $scope.$apply();
    });
    $scope.$on('app-activities-error',function(evt,data){
      $rootScope.$broadcast('alert', 'error', 'Application failed to retreive activities data.');
    });
    ug.getActivities();
  }]);