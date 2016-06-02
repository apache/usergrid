
AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);

AppServices.MAX.directive('demoData', ["$location", "$routeParams", "$rootScope",'$route','$log', function ($location, $routeParams, $rootScope,$route,$log) {
  return{
    restrict: 'E',
    transclude: true,
    templateUrl:'performance/demo-data.html',
    link: function linkFn(scope, lElement, attrs) {
      scope.dataVals = {};
      scope.dataVals.showDemoBar=true;
      //only on first load, check for url param to show demo data by default
      $rootScope.demoData = scope.dataVals.demoData = $location.search().demo === 'true' || $rootScope.demoData;
      $rootScope.autoUpdate =  $rootScope.autoUpdate ||  null;
      $rootScope.timerUpdate =  $rootScope.timerUpdate || null;
      $rootScope.autoUpdateTimer =  $rootScope.autoUpdateTimer || 31;
      $rootScope.keepGoing = true;
      $rootScope.currentSecond;

      scope.$watch('dataVals.demoData',function(value,oldvalue){
        if(value !== oldvalue){
          if(value){
            $routeParams.demo = 'true';
            $location.search('demo', 'true');
            $rootScope.demoData = true;
          }else{
            $routeParams.demo = 'false';
            $location.search('demo', 'false');
            $rootScope.demoData = false;
          }
          $route.reload();
        }
      });
      scope.$on('toggle-demo-data',function(){
        scope.dataVals.demoData = !scope.dataVals.demoData;
      });

      $rootScope.toggleAutoUpdate = function(){
        //set auto update interval
        var isPerformancePage = $location.path().slice(0,'/performance'.length) === '/performance';

        var animate = function () {
          if($rootScope.keepGoing && isPerformancePage){
            requestAnimFrame( animate );
            draw();
          }
        }

        var draw = function () {
          if(!$rootScope.$$phase && $rootScope.currentSecond !== new Date().getSeconds() ){
            $rootScope.$apply(function() {
              $rootScope.autoUpdateTimer--;
              if($rootScope.autoUpdateTimer === 0){
                var refreshts = new Date();
                $routeParams.ts = refreshts.getTime();
                $rootScope.autoUpdateTimer = 30;
              }
            });
            $rootScope.currentSecond = new Date().getSeconds();
          }
        }

        if(!$rootScope.autoUpdate && isPerformancePage){
          $log.info('start timers')
          $rootScope.autoUpdate = true;
          $rootScope.keepGoing = true;
          animate();

        }else{
          $rootScope.keepGoing = false;
          $rootScope.autoUpdate = null;
        }

      };
      scope.$on('app-changed',function(){
        scope.dataVals.demoData = false;
      });

      scope.$on('org-changed',function(){
        scope.dataVals.demoData = false;
      });
      scope.$on('toggle-auto-update',function(){
        $rootScope.toggleAutoUpdate();
      });

      scope.toggleDemoData = function(){
        $rootScope.$broadcast('toggle-demo-data');
      };

      if($rootScope.autoUpdate == null){
       // $rootScope.toggleAutoUpdate();
      }
    }
  }
}]);