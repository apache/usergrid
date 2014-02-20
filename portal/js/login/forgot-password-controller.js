'use strict'

AppServices.Controllers.controller('ForgotPasswordCtrl',
  ['ug',
    '$scope',
    '$rootScope',
    '$location',
    'utility', function (ug,  $scope, $rootScope, $location) {
    $rootScope.activeUI &&   $location.path('/');
    $scope.forgotPWiframeURL = $scope.apiUrl + '/management/users/resetpw';
  }]);
