'use strict'

AppServices.Controllers.controller('GroupsDetailsCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    var selectedGroup = $rootScope.selectedGroup.clone();
    $scope.detailsSelected = 'active';
    $scope.json = selectedGroup._json || selectedGroup._data.stringifyJSON();
    $scope.group = selectedGroup._data;
    $scope.group.path =  $scope.group.path.indexOf('/')!=0 ? '/'+$scope.group.path : $scope.group.path;
    $scope.group.title = $scope.group.title;

    if (!$rootScope.selectedGroup) {
      $location.path('/groups');
      return;
    }
    $scope.$on('group-selection-changed',function(evt,selectedGroup){
      $scope.group.path =  selectedGroup._data.path.indexOf('/')!=0 ? '/'+selectedGroup._data.path : selectedGroup._data.path;
      $scope.group.title = selectedGroup._data.title;
      $scope.detailsSelected = 'active';
      $scope.json = selectedGroup._json || selectedGroup._data.stringifyJSON();

    });

    $rootScope.saveSelectedGroup = function(){
      $rootScope.selectedGroup._data.title = $scope.group.title;
      $rootScope.selectedGroup._data.path = $scope.removeFirstSlash( $scope.group.path);
      $rootScope.selectedGroup.save(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error saving group');
        } else {
          $rootScope.$broadcast('alert', 'success', 'group saved');
        }
      });

    }

  }]);