'use strict'

AppServices.Controllers.controller('UsersProfileCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.user = $rootScope.selectedUser._data.clone();
    $scope.user.json = $scope.user.json || $rootScope.selectedUser._json;
    $scope.profileSelected = 'active';

    if (!$rootScope.selectedUser) {
      $location.path('/users');
      return;
    }

    $scope.$on('user-selection-changed',function(evt,selectedUser){
      $scope.user = selectedUser._data.clone();
      $scope.user.json = $scope.user.json ||  selectedUser._data.stringifyJSON();
    });

    $scope.saveSelectedUser = function(){
      $rootScope.selectedUser.set($scope.user.clone());
      $rootScope.selectedUser.save(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error saving user');
        } else {
          $rootScope.$broadcast('alert', 'success', 'user saved');
        }
      });

    }

  }]);