'use strict'

AppServices.Controllers.controller('ForgotPasswordCtrl',
  ['ug',
    'data',
    '$scope',
    '$rootScope',
    '$location',
    'utility', function (ug, data, $scope, $rootScope, $location) {
    $rootScope.activeUI &&   $location.path('/');
    $scope.forgotPWiframeURL = $scope.apiUrl + '/management/users/resetpw';
  }]);
