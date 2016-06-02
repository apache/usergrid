'use strict';
AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);


AppServices.MAX.directive('slidecheckbox', ["$rootScope", function ($rootScope) {
  return{
    restrict: 'ECA',
    scope: {
      data: '=data'
    },
    templateUrl: 'performance/checkbox-template.html',
    replace: true,
    transclude: true,
    link: function linkFn(scope, lElement, attrs) {
      scope.label = attrs.label;
    }
  }
}])