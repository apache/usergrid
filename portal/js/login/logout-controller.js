'use strict'
AppServices.Controllers.controller('LogoutCtrl', ['ug', '$scope', '$rootScope', '$routeParams', '$location', 'utility', function (ug, $scope, $rootScope, $routeParams, $location, utility) {
    ug.logout();
    if($scope.use_sso){
      window.location = $rootScope.urls().LOGOUT_URL + '?callback=' +  encodeURIComponent($location.absUrl().split('?')[0]);
    }else{
      $location.path('/login');
      $scope.applyScope();
    }
}]);
