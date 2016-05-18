'use strict';
AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);

AppServices.MAX.directive('infobox', function () {
  return{
    restrict: 'ECA',
    scope: {
      datasrc: '=datasrc',
      currentcompare: '=currentcompare'
    },
    transclude: true,
    templateUrl: 'performance/includes/info-box.html',
    replace: true,
    link: function linkFn(scope, lElement, attrs) {
      scope.title = attrs.title;
      scope.tooltipName =  'tooltip_' + attrs.title.replace(/\s/g, '_').toLowerCase();
    }
  }
})