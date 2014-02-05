'use strict'

AppServices.Controllers.controller('RegisterCtrl', ['ug', '$scope', '$rootScope', '$routeParams', '$location', 'utility', function (ug, $scope, $rootScope, $routeParams, $location, utility) {
  $rootScope.activeUI &&   $location.path('/');

  var init = function () {
    $scope.registeredUser = {};
  }

  init();

  $scope.cancel = function(){
    $location.path('/');
  }
  $scope.register = function () {
    var user = $scope.registeredUser.clone();
    if (user.password === user.confirmPassword) {
      ug.signUpUser(user.orgName, user.userName, user.name, user.email, user.password);
    }else{
      $rootScope.$broadcast('alert', 'error', 'Passwords do not match.' + name);
    }
  };

  $scope.$on('register-error', function (event, data) {
    $scope.signUpSuccess = false;
    $rootScope.$broadcast('alert', 'error', 'Error registering: ' + (data && data.error_description ? data.error_description : name));
  });
  $scope.$on('register-success', function (event, data) {
    $scope.registeredUser = {};
    $scope.signUpSuccess = true;
    init();
    $scope.$apply();
  });


}]);
