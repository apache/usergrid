'use strict'

AppServices.Controllers.controller('AlertCtrl', ['$scope', '$rootScope', '$timeout',
  function ($scope, $rootScope, $timeout) {

    $scope.alertDisplay = 'none';
    $scope.alerts = [];

    //alert types error,success,warning,info

    $scope.$on('alert', function(event, type, message, permanent) {
      $scope.addAlert(type, message, permanent);
    });

    $scope.$on('clear-alerts', function(event, message) {
      $scope.alerts = [];
    });

    $scope.addAlert = function(type, message, permanent) {
      $scope.alertDisplay = 'block';
      $scope.alerts.push({type:type, msg:message});
      $scope.applyScope();
      if(!permanent){
        $timeout(function(){$scope.alerts.shift()},5000);
      }
    };

    $scope.closeAlert = function(index) {
      $scope.alerts.splice(index, 1);
    };


  }]);