/**
 * @description
 *
 *
 * @param {string}
 *
 *
 */
AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);

AppServices.MAX.directive('timeFilter-log', ["$location", "$routeParams","$rootScope", function ($location,$routeParams,$rootScope) {
  return{
    restrict: 'A',
    transclude: true,
    template: '<li ng-repeat="time in timeFilters" class="filterItem"><a ng-click="changeTimeFilter(time)">{{time.label}}</a></li>',
    link: function linkFn(scope, lElement, attrs) {
      var menuContext = attrs.filter;

      scope.changeTimeFilter = function (newTime) {
        scope.selectedtimefilter = newTime;
        $routeParams.timeFilter = newTime.value;

//        $rootScope.currentCompare = $routeParams.currentCompare = 'NOW';
      }

      lElement.bind('click', function (cevent) {
        menuBindClick(scope,lElement,cevent,menuContext)
      })

    }
  }
}])