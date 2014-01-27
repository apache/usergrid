'use strict';

AppServices.Directives.directive('balloon', ['$window','$timeout', function ($window,$timeout) {
  return{
    restrict: 'ECA',
    scope: '=',
    template: '' +
      '<div class="baloon {{direction}}" ng-transclude>' +
      '</div>',
    replace: true,
    transclude: true,
    link: function linkFn(scope, lElement, attrs) {
      scope.direction = attrs.direction;
      var runScroll = true;
      var windowEl = angular.element($window);
      windowEl.on('scroll', function() {
        if(runScroll){
          lElement.addClass('fade-out');
          $timeout(function(){lElement.addClass('hide')},1000)
          runScroll = false;
        }
      });
    }
  }
}])