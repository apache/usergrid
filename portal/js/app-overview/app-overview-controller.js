/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
'use strict';

AppServices.Controllers.controller('AppOverviewCtrl',
    ['ug',
      'charts',
      '$scope',
      '$rootScope',
      '$log',
      function (ug,  charts, $scope, $rootScope, $log) {
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
            ug.httpGet(null, 'js/charts/highcharts.json').then(function (success) {
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