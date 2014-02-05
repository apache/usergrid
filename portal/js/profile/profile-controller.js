'use strict'

AppServices.Controllers.controller('ProfileCtrl', ['$scope', '$rootScope', 'ug', 'utility',
  function ($scope, $rootScope, ug,utility) {

    $scope.loading = false;

    $scope.saveUserInfo = function(){
      $scope.loading = true;
      ug.updateUser($scope.user);
    };

    $scope.$on('user-update-error',function(){
      $scope.loading = false;
      $rootScope.$broadcast('alert', 'error', 'Error updating user info');
    });

    $scope.$on('user-update-success',function(){
      $scope.loading = false;
      $rootScope.$broadcast('alert', 'success', 'Profile information updated successfully!');
      if($scope.user.oldPassword && $scope.user.newPassword != 'undefined'){
        //update password after userinfo update
        ug.resetUserPassword($scope.user);
      }
    });

    $scope.$on('user-reset-password-success',function(){
      $rootScope.$broadcast('alert', 'success', 'Password updated successfully!');
      $scope.user = $rootScope.currentUser.clone();
    });
    $scope.$on('app-initialized',function(){
      $scope.user = $rootScope.currentUser.clone();
    });

    if($rootScope.activeUI){
      $scope.user = $rootScope.currentUser.clone();
      $scope.applyScope();
    }

  }]);