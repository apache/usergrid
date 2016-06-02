'use strict';
AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);

AppServices.MAX.controller('RawLogCtrl', ['data', '$scope', '$rootScope', function (data, $scope, $rootScope) {

  $scope.rawLogSearchShowAdv = false;

  $scope.showStackTrace = function(id){
    alert(id);
  };

  $scope.nextPage = function(){
    $rootScope.rawLogSearch.pageNumber++;
    $rootScope.rawLogSearch.start = ($scope.itemsPerPage * $rootScope.rawLogSearch.pageNumber)
    $scope.loadMoreLogs();
  }

  $scope.prevPage = function(){
    if($rootScope.rawLogSearch.pageNumber > 0){
      $rootScope.rawLogSearch.pageNumber--;
    }
    $rootScope.rawLogSearch.start = ($scope.itemsPerPage * $rootScope.rawLogSearch.pageNumber)
    $scope.loadMoreLogs();
  }


  $rootScope.rawLogSearch = {
    tag: '',
    severity: '',
    devicePlatform: '',
    deviceOperatingSystem: '',
    deviceModel: '',
    deviceId: '',
    start: 0,
    pageNumber: 0
  }

}]);