'use strict';

AppServices.Controllers.controller('AccountCtrl', ['$scope', '$rootScope', 'ug', 'utility','$route',
  function ($scope, $rootScope, ug,utility,$route) {
    $scope.currentAccountPage = {};
    var route = $scope.use_sso ? '/profile/organizations' : '/profile/profile';
    $scope.currentAccountPage.template = $route.routes[route].templateUrl;
    $scope.currentAccountPage.route = route;
    $scope.applyScope();
    $scope.selectAccountPage = function(route){
      //lokup the template URL with the route. trying to preserve routes in the markup and not hard link to .html
      $scope.currentAccountPage.template = $route.routes[route].templateUrl;
      $scope.currentAccountPage.route = route;

    }
  }

 ]);