'use strict'

AppServices.Controllers.controller('AppOverviewCtrl',
    ['ug',
      'data',
      'charts',
      '$scope',
      '$rootScope',
      '$log',
      function (ug, data, charts, $scope, $rootScope, $log) {
        //util
        var createGradient = function (color1, color2) {
          var perShapeGradient = {
            x1: 0,
            y1: 0,
            x2: 0,
            y2: 1
          };
          return {
            linearGradient: perShapeGradient,
            stops: [
              [0, color1],
              [1, color2]
            ]
          };
        };
        $scope.appOverview = {};

        $scope.collections = [];
        $scope.graph = '';
        $scope.$on('top-collections-received', function (event, collections) {
          var dataDescription = {
            bar1: {
              labels: ['Total'],
              dataAttr: ['title', 'count'],
              colors: [createGradient('rgba(36,151,212,0.6)', 'rgba(119,198,240,0.6)')],
              borderColor: '#1b97d1'
            }
          };
          //todo add this to charts service as helper
          $scope.collections = collections;
          var arr = [];
          for (var i in collections) {
            if (collections.hasOwnProperty(i)) {
              arr.push(collections[i]);
            }
          }
          $scope.appOverview = {};
          if (!$rootScope.chartTemplate) {
            //get the chart template for this view... right now it covers all charts...
            data.get(null, 'js/charts/highcharts.json').then(function (success) {
              $rootScope.chartTemplate = success;
              $scope.appOverview.chart = angular.copy($rootScope.chartTemplate.pareto);
              $scope.appOverview.chart = charts.convertParetoChart(arr, $scope.appOverview.chart, dataDescription.bar1, '1h', 'NOW');
              $scope.applyScope();
            }, function (fail) {
              $log.error('Problem getting chart template', fail)
            });
          } else {
            $scope.appOverview.chart = angular.copy($rootScope.chartTemplate.pareto);
            $scope.appOverview.chart = charts.convertParetoChart(arr, $scope.appOverview.chart, dataDescription.bar1, '1h', 'NOW');
            $scope.applyScope();

          }


        });
        $scope.$on('app-initialized',function(){
          ug.getTopCollections();
        });
        if($rootScope.activeUI){
           ug.getTopCollections();
        }


      }]);