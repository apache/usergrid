'use strict'

AppServices.Controllers.controller('UsersGraphCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.graphSelected = 'active';
    $scope.user = '';

    //todo find others and combine this into one controller
    ug.getUsersTypeAhead();



    $scope.followUserDialog = function(modalId){
      if ($scope.user) {
        ug.followUser($scope.user.uuid);
        $scope.hideModal(modalId);
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a user to follow.');
      }
    };

    if (!$rootScope.selectedUser) {
      $location.path('/users');
      return;
    } else {
      $rootScope.selectedUser.activities = [];
      $rootScope.selectedUser.getFollowing(function(err, data){
        if (err) {

        } else {
          if(!$rootScope.$$phase) {
            $rootScope.$apply();
          }
        }

      });

      $rootScope.selectedUser.getFollowers(function(err, data){
        if (err) {

        } else {
          if(!$rootScope.$$phase) {
            $rootScope.$apply();
          }
        }

      });

      $scope.$on('follow-user-received', function(event) {

        $rootScope.selectedUser.getFollowing(function(err, data){
          if (err) {

          } else {
            if(!$rootScope.$$phase) {
              $rootScope.$apply();
            }
          }

        });

      });
    }


  }]);