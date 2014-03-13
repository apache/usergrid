'use strict'

AppServices.Controllers.controller('ForgotPasswordCtrl',
  ['ug',
    '$scope',
    '$rootScope',
    '$location',
    '$sce',
    'utility', function (ug,  $scope, $rootScope, $location,$sce) {
    $rootScope.activeUI &&   $location.path('/');
    $scope.forgotPWiframeURL = $sce.trustAsResourceUrl( $scope.apiUrl + '/management/users/resetpw');
  }]);
