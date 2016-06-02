'use strict';
AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);

AppServices.MAX.controller('PerformanceCtrl',
  [ 'data',
    'charts',
    '$scope',
    '$routeParams',
    '$location',
    '$rootScope',
    '$compile',
    '$anchorScroll',
    '$log',
    '$q',
    function (data, charts,  $scope, $routeParams, $location, $rootScope, $compile, $anchorScroll, $log,$q) {
      $rootScope.performance = $rootScope.performance || {showHelpButton:true};
      $scope.routeParams = $routeParams;
      $scope.location = $location;
      $scope.showAutoRefresh = Usergrid.options.showAutoRefresh;
      $rootScope.currentAppId = 1;

      var deviceModelFilter,
        osVersionFilter,
        platformFilter,
        appVersionFilter;
      var appConfig = null;

      var getAppConfig = function(){
        var deferred =  $q.defer();
        $scope.$on('app-initialized', function () {
          if (!appConfig) {
            data.jsonp_simple('apigeeMobileConfig', '', {demoApp: $rootScope.demoData}).then(function (appConfig) {
              setAppId(appConfig.instaOpsApplicationId)
              deferred.resolve();
            })
          } else {
            deferred.resolve();
          }
        });
        return deferred.promise;
      }

      var setAppId = function(appId){
//          if(!$rootScope.demoData){
            $rootScope.currentAppId = appId;
//          }
      }

      //setup dropdown list of charts... the entire page data depends on this
      var getChartCriteria = function(){
        return data.jsonp_simple(chartCriteria, $rootScope.currentAppId, {demoApp:$rootScope.demoData});
      }

      var determineChartCriteria = function(chartCriteriaList){
        //store options for dropdown list
        $scope.chartCriteriaOptions = chartCriteriaList;
        //todo redesign the RESt api please :/
        angular.forEach(chartCriteriaList,function(value,key){
          switch(value.chartName){
            case 'by App Versions': $scope.criteriaAppVersions = value.chartCriteriaId; break;
            case 'by Config Overrides': $scope.criteriaConfigOverrides = value.chartCriteriaId; break;
            case 'by Platforms': $scope.criteriaPlatforms = value.chartCriteriaId; break;
            case 'by Device Models': $scope.criteriaDeviceModels = value.chartCriteriaId; break;
            case 'by OS Versions': $scope.criteriaOSVersions = value.chartCriteriaId; break;
            case 'by Network Types': $scope.criteriaNetworkTypes = value.chartCriteriaId; break;
            case 'by Carriers': $scope.criteriaCarriers = value.chartCriteriaId; break;
            case 'by Domain': $scope.criteriaDomain = value.chartCriteriaId; break;
          }
        })

      }


      function initChartFilters(newFilter,chartFilterType){
        deviceModelFilter = {currentCompare: 'NOW', timeFilter: newFilter.timeFilter};
        deviceModelFilter[chartFilterType] = $scope.criteriaDeviceModels;

        osVersionFilter = {currentCompare: 'NOW', timeFilter: newFilter.timeFilter};
        osVersionFilter[chartFilterType] = $scope.criteriaOSVersions;

        platformFilter = {currentCompare: 'NOW', timeFilter: newFilter.timeFilter};
        platformFilter[chartFilterType] = $scope.criteriaPlatforms;

        appVersionFilter = {currentCompare: 'NOW', timeFilter: newFilter.timeFilter};
        appVersionFilter[chartFilterType] = $scope.criteriaAppVersions;
      }

      $rootScope.compareOptions = [
        {}
      ];

      if (!$routeParams.currentCompare) {
        $rootScope.currentCompare = 'NOW';
        $location.search('currentCompare', $rootScope.currentCompare);

      }

      //check for any empty filter then populate default
      if (!$routeParams.timeFilter) {
        $location.search('timeFilter', '1h');
      }

      var updatePreviouslyOn = false;
      $rootScope.compare = function (reference,toggleUpdate) {
        if (reference === $rootScope.currentCompare) {
          $rootScope.currentCompare = 'NOW';
          if(toggleUpdate || updatePreviouslyOn){
            $scope.toggleAutoUpdate();
          }
          $rootScope.hideAutoupdate = false;

        } else {
          $rootScope.currentCompare = reference;
          updatePreviouslyOn =  $rootScope.autoUpdate;
          if($rootScope.currentCompare !== 'NOW' && $rootScope.autoUpdate){
            $scope.toggleAutoUpdate();
          }
          $rootScope.hideAutoupdate = true;
        }
        $location.search('currentCompare', $rootScope.currentCompare);
      }

      //controls how the time is formatted
      var timeformat1 = "return Highcharts.dateFormat('%H:%M', this.value);";
      var timeformat2 = "return Highcharts.dateFormat('%a, %H:%M', this.value);";

      //values for the dropdown time filter above charts
      $rootScope.timeFilters = [
        {"value": "1h", "label": "1 hour", "settings": {"xaxisformat": timeformat1, "step": 3}},
        {"value": "3h", "label": "3 hours", "settings": {"xaxisformat": timeformat1, "step": 6}},
        {"value": "6h", "label": "6 hours", "settings": {"xaxisformat": timeformat1, "step": 10}},
        {"value": "12h", "label": "12 hours", "settings": {"xaxisformat": timeformat1, "step": 15}},
        {"value": "24h", "label": "24 hours", "settings": {"xaxisformat": timeformat2, "step": 1}},
        {"value": "1w", "label": "1 week", "settings": {"xaxisformat": timeformat2, "step": 10}}
      ]

      //end default checks

      $rootScope.selectedtimefilter = validateFilter($location.search().timeFilter, $rootScope.timeFilters, 'value')

      //validate timefilter query param value
      function validateFilter(paramValue, options, property) {
        if (paramValue && paramValue !== "") {
          var matchFound = false;
          for (var i = 0; i < options.length; i++) {
            if (options[i][property] === paramValue) {
              matchFound = true;
              return options[i];
            }
          }
          if (!matchFound) {
            //this is case of chartCriteriaId not being returned in consistent form/id
            $routeParams[chartFilterContext] = options[0].chartCriteriaId;
            return options[0]
          }
        } else {
          return options[0];
        }
      }

      if(!$rootScope.chartTemplate){
        //get the chart template for this view... right now it covers all charts...
        data.get(null, 'js/charts/highcharts.json').then(function (success) {
          $rootScope.chartTemplate = success;
        }, function (fail) {
          $log.error('Problem getting chart template', fail)
        });
      }

//      --------------------------------- Watching event and variables for page change


      //todo - move this out to a directive... manipulating dom should not be in controller
      $rootScope.$on('ajax_loading', function(event,objectType) {
        if(objectType.indexOf('ChartData') > 0){
          var allCharts = document.querySelectorAll('.anim-chart');
          try {
            angular.forEach(allCharts, function(value, key){
              value.classList.add('fade-out');
              value.classList.remove('fade-in')
            });
          } catch (e) {
            $log.error(e)
          }
        }
      })

      //todo - move this out to a directive... manipulating dom should not be in controller
      $rootScope.$on('ajax_finished', function(event,objectType) {
        if(objectType.indexOf('ChartData') > 0){
          var allCharts = document.querySelectorAll('.anim-chart');
          try {
            angular.forEach(allCharts, function(value, key){
              value.classList.add('fade-in');
              value.classList.remove('fade-out')
            });
          } catch (e) {
            $log.error(e)
          }
        }
      })


      //---- main data calls
     $scope.chartDataPresent = false;
     $scope.logDataPresent = false;
     $scope.chartDataWaiting = true;
     $scope.logDataWaiting = true;
     $scope.showlogBaloon = false;
     $scope.currentChartData = '';

     $scope.requestChartData = function(chartData,newFilter,callDesc,callback,runDataCheck){

       data.jsonp(chartData, newFilter, callDesc, function (mydata) {

         //we are currently using this to detect if the SDK has sent any data... complete hack and needs to be done by API
         if(runDataCheck && chartData !== $scope.currentChartData){
           //check for no data and add check for compare to handle now data and no compare data
           if(mydata.chartData && $rootScope.currentCompare === 'NOW'){
             if(mydata.chartData.length === 0){
               $scope.chartDataPresent = false
             } else if(mydata.chartData.length === 1 && mydata.chartData[0].datapoints){
               $scope.chartDataPresent = mydata.chartData[0].datapoints.length !== 0;
             }else{
               $scope.chartDataPresent = true;
             }
           }
           //setting this var only calls the data check once per page nav
           //we have multiple charts on a page and this (requestChartData) gets called on every one along with refresh
           $scope.currentChartData = chartData;

         }

         callback(mydata);
         $scope.chartDataWaiting = false;

       },function (fail) {
         $log.error('Problem getting initial chart data', fail);
       });
     }

     $scope.requestLogData = function(chartData,newFilter,callDesc,callback){
       data.jsonp(chartData, newFilter, callDesc, function (mydata) {

         //check to see if we have any data from logs
         if(typeof mydata[0] !== 'undefined'){
           $scope.logDataPresent = true;
         }

         callback(mydata);
         $scope.logDataWaiting = false;

       },function (fail) {
         $log.error('Problem getting initial log data', fail);
       });
     }

      $scope.convertChartType = function(method){
        $scope.conversionMethod = method;
        var refreshts = new Date();
        $routeParams.ts = refreshts.getTime();
      }


      //global variables used across each setup action
      var chartCriteria = '',
        chartData = '',
        chartErrorMsg = 'Error getting chart data',
        chartFilterContext = '',
        rawData,
        dataDescription;

      $scope.apiPerfSetup = function () {

        //override when navigating pages
        $rootScope.currentCompare = $routeParams.currentCompare = 'NOW';
//        $routeParams.networkChartFilter = '1'

        chartCriteria = 'networkChartCriteria';
        chartData = 'networkChartData';
        rawData = 'networkRawData';
        chartErrorMsg = 'Error getting chart data';
        chartFilterContext = 'networkChartFilter';

        //data description tells the chart renderer what our labels are and what the incoming JSON data keys are that we want to plot
        dataDescription = {
          timeseries1: {
            labels: ['Milliseconds'],
            yAxisLabels: [''],
            dataAttr: ['avgLatency'],
            colors: ['#1b97d1', '#898989', '#ececec', '#ff33ee'],
            detailDataAttr: ['avgLatency'],
            detailYaxisLabel: ['Milliseconds']
          },
          timeseries2: {
            labels: ['Requests', 'Errors'],
            yAxisLabels: [''],
            dataAttr: ['samples', 'errors'],
            colors: ['#54828c', '#ff4300', '#ececec', '#ff33ee'],
            detailDataAttr: ['samples'],
            detailYaxisLabel: ['Request']
          },
          bar1: {
            labels: ['Milliseconds'],
            dataAttr: ['attribute', 'value'],
            colors: [createGradient('rgba(36,151,212,0.6)', 'rgba(119,198,240,0.6)')],
            borderColor: '#1b97d1'
          },
          bar2: {
            labels: ['Requests', 'Errors'],
            dataAttr: ['attribute', ['requests', 'errors']],
            colors: [createGradient('rgba(84,130,140,0.6)', 'rgba(152,182,189,0.6)'),createGradient('rgba(255,62,0,0.5)', 'rgba(255,147,112,0.5)'),
              createGradient('rgba(84,130,140,0.3)', 'rgba(152,182,189,0.3)'),createGradient('rgba(255,62,0,0.2)', 'rgba(255,147,112,0.2)')],
            borderColor: '#54828c'
          }
        };

        //this is the view object/holder for all charts in app usage page
        $scope.perfCharts = {
          totals: "",
          summary: {currentCompare: "NOW"}
        };

         getAppConfig().then(getChartCriteria).then(function(chartCriteriaList){
          if(!chartCriteriaList || !chartCriteriaList.length ){
            return;
          }
          determineChartCriteria(chartCriteriaList);

          var defaultChartCriteria = [];
          if($scope.chartCriteriaOptions.length > 0){
            defaultChartCriteria = $scope.chartCriteriaOptions[0].chartCriteriaId;
          }

          //watch for changes in the location
          $scope.$watch('routeParams', function (newFilter) {

            //check for chartFilter setting in memory or on location path

            if (!newFilter[chartFilterContext]) {
              newFilter[chartFilterContext] = defaultChartCriteria;
            }

            //setup defaults coming from new URL request
            $rootScope.selectedChartCriteria = validateFilter(parseInt(newFilter[chartFilterContext]), $scope.chartCriteriaOptions, 'chartCriteriaId')

            //change the url to match newly selected option
            angular.forEach(newFilter, function (v, k) {
              $location.search(k, v);
            });

            //sync current compare option
            $scope.perfCharts.summary.currentCompare = $rootScope.currentCompare = $location.search().currentCompare

            //determine if this is a chartFilter > 1 (sub filtering for bar charts)
            var mainChart = ($rootScope.selectedChartCriteria.chartName !== 'Overview' ? {'type': 'bar', 'call': 'convertParetoChart', 'template': 'pareto', 'dataDescriptions': ['bar1', 'bar2']} : {'type': 'timeseries', 'call': 'convertLineChart', 'template': 'line', 'dataDescriptions': ['timeseries1', 'timeseries2']}),
              compareExisting = $scope.perfCharts.summary.currentCompare !== 'NOW' && $rootScope.currentCompare !== 'NOW';

            //request chart data for page..
            $scope.requestChartData(chartData,newFilter[chartFilterContext],
              {
                period: newFilter.timeFilter,
                reference: 'NOW',
                chartType: mainChart.type
              }, function(mydata){


                //make a copy of the blank template
                $scope.perfCharts.requestTimes = angular.copy($rootScope.chartTemplate[mainChart.template]);
                $scope.perfCharts.requests = angular.copy($rootScope.chartTemplate[mainChart.template]);

                //if compare - we must make an extra call for NOW data
                if(compareExisting){
                  //get the compare data
                  $scope.perfCharts.requestTimes = angular.copy($scope.perfCharts.requestTimes);
                  $scope.perfCharts.requests = angular.copy($scope.perfCharts.requests);

                  //request chart data for page..
                  $scope.requestChartData(chartData,newFilter[chartFilterContext],
                    {
                      period: newFilter.timeFilter,
                      reference: $rootScope.currentCompare,
                      chartType: mainChart.type
                    }, function(mycomparedata){
                      convertData($rootScope.currentCompare,mycomparedata);
                    });
                }

                convertData('NOW',mydata);
//                The returned JSON for bar charts is a bit weird since we have 2 charts on this page, so easiest way is just check for chart type
//                chartDataCall(deviceModelFilter, 'bar', $scope.ecCharts.deviceModels, 'convertParetoChart', 'bar1', function (chartData) {
                function convertData(forceCompare,srcdata){
                  if (mainChart.type === 'bar') {
                    $scope.perfCharts.requestTimes = charts[mainChart.call](srcdata.responseTimes, $scope.perfCharts.requestTimes, dataDescription[mainChart.dataDescriptions[0]], $rootScope.selectedtimefilter.settings, forceCompare);
                    $scope.perfCharts.requests = charts[mainChart.call](srcdata.requestErrorCounts, $scope.perfCharts.requests, dataDescription[mainChart.dataDescriptions[1]], $rootScope.selectedtimefilter.settings, forceCompare);
                  } else {
                    $scope.perfCharts.requestTimes = charts[mainChart.call](srcdata.chartData, $scope.perfCharts.requestTimes, dataDescription[mainChart.dataDescriptions[0]], $rootScope.selectedtimefilter.settings, forceCompare);
                    $scope.perfCharts.requests = charts[mainChart.call](srcdata.chartData, $scope.perfCharts.requests, dataDescription[mainChart.dataDescriptions[1]], $rootScope.selectedtimefilter.settings, forceCompare);
                  }
                }


                //holder for summary data
                if (compareExisting) {
                  //we need to re-request the now data for proper compare
                  data.jsonp(chartData, newFilter[chartFilterContext],
                    {
                      period: newFilter.timeFilter,
                      reference: $rootScope.currentCompare,
                      chartType: mainChart.type
                    }, function (compareData) {
                      $scope.perfCharts.summary = compareSummaryData(mydata.summaryData, compareData.summaryData, $rootScope.currentCompare)
                    })

                } else {
                  $scope.perfCharts.summary = compareSummaryData($scope.perfCharts.summary, mydata.summaryData, 'NOW')
                }
              },true);


            $scope.itemsPerPage = 100;

            $scope.loadMoreLogs = function (itemsPerPage) {

              if (itemsPerPage) {
                $scope.itemsPerPage = itemsPerPage;
              }

              if($scope.autoUpdate){
                $scope.toggleAutoUpdate();
              }

              data.jsonp(rawData, newFilter[chartFilterContext],
                {
                  period: newFilter.timeFilter,
                  reference: $rootScope.currentCompare,
                  rowCount: $scope.itemsPerPage,
                  url: $rootScope.rawLogSearch.url,
                  networkCarrier: $rootScope.rawLogSearch.networkCarrier,
                  networkType: $rootScope.rawLogSearch.networkType,
                  deviceModel: $rootScope.rawLogSearch.deviceModel,
                  deviceId: $rootScope.rawLogSearch.deviceId,
                  devicePlatform: $rootScope.rawLogSearch.devicePlatform,
                  deviceOperatingSystem: $rootScope.rawLogSearch.deviceOperatingSystem,
                  latency: $rootScope.rawLogSearch.latency,
                  httpStatusCode: $rootScope.rawLogSearch.httpStatusCode,
                  start: ($rootScope.rawLogSearch.start)
                }, function (logData) {
                  $scope.rawLogs = logData;
                });

            };

            //get log data
            //todo remove this is a hack that requests 1 row just to see if there is data
            $scope.requestLogData(rawData,newFilter[chartFilterContext],
              {
                period: newFilter.timeFilter,
                reference: $rootScope.currentCompare,
                rowCount: 1
              }, function (logData) {
                $scope.rawLogs = logData;
              });

            $scope.requestLogData(rawData,newFilter[chartFilterContext],
              {
                period: newFilter.timeFilter,
                reference: $rootScope.currentCompare,
                rowCount: $scope.itemsPerPage
              }, function (logData) {
                $scope.rawLogs = logData;
              });

          }, true)

        }, function (fail) {

        });

      }

      $scope.errorsCrashesSetup = function (ecDetail) {

        //override when navigating pages
        $rootScope.currentCompare = $routeParams.currentCompare = 'NOW';

        //hide detailed chart filters only for errors/crashes combo view
        $scope.ecHideChartFilters = true;

        //don't want pie charts called when routeparams change, they stay the same throughout all compare calls
        var pieChartsCreated = false;

        $scope.ecDetailedViewList = [
          {"label": "Overview", "value": 1},
          {"label": "App Errors", "value": 2},
          {"label": "Crashes", "value": 3}
        ];

        if (!$rootScope.ecDetailedView) {
          $rootScope.ecDetailedView = $scope.ecDetailedViewList[0];
        }

        //this is just a fallback default so that a detailed view (blank) doesn't show up on the combined errors/charts page
        //...otherwords, always default to overview




        chartCriteria = 'logChartCriteria';
        chartData = 'logChartData';
        rawData = 'logRawData';
        chartErrorMsg = 'Error getting chart data';
        chartFilterContext = 'logChartFilter';
        var pieRGB = '255,3,3'
        dataDescription = {
          timeseries: {
            labels: ['App Errors', 'Crashes'],
            yAxisLabels: [''],
            dataAttr: ['errorAndAboveCount', 'crashCount'],
            colors: ['#ff4300', '#800000'],
            detailDataAttr: [],
            detailYaxisLabel: [''],
            multiAxis: false,
            areaColors: [createGradient('#ff8f6b', '#fff'), createGradient('rgba(253,60,60,0.5)', 'rgba(255,3,3,0.5)')]
          },
          bar1: {
            labels: ['Crashes'],
            dataAttr: ['attribute', 'value'],
            colors: [createGradient('rgba(128,0,0,0.5)', 'rgba(255,3,3,0.5)')],
            borderColor: '#fff'
          },
          bar2: {
            labels: ['Crashes'],
            dataAttr: ['attribute', 'value'],
            colors: [createGradient('rgba(128,0,0,0.5)', 'rgba(255,3,3,0.5)')],
            borderColor: '#fff'
          },
          pie: {
            dataAttr: ['attribute', 'percentage'],
            colors: [
              'rgba(255,3,3,0.5)',
              'rgba(255,3,3,0.4)',
              'rgba(255,3,3,0.3)',
              'rgba(255,3,3,0.2)',
              'rgba(255,3,3,0.1)',
              '#ff9191', '#ffa1a1', '#ffb6b6', '#ffcbcb'],
            borderColor: '#ff0303'
          },
          area: {

          }
        };


        //this is the view object/holder for all charts in app usage page
        $scope.ecCharts = {
          totals: "",
          devicePlatform: {},
          appVersion: {},
          summary: {currentCompare: "NOW"}
        };

        getAppConfig().then(getChartCriteria).
          then(function(chartCriteriaList){
            if (!chartCriteriaList || !chartCriteriaList.length) {
              return;
            }
            determineChartCriteria(chartCriteriaList)

            var defaultChartCriteria = [];
          if($scope.chartCriteriaOptions.length > 0){
            defaultChartCriteria = $scope.chartCriteriaOptions[0].chartCriteriaId;
          }

          if ($rootScope.ecDetailedView.value === 1) {
            $routeParams.logChartFilter = defaultChartCriteria;
          }

          $scope.changeEcDetailedView = function (detailedView) {

            $rootScope.ecDetailedView = detailedView;
            $rootScope.compare('NOW',false);



            if (detailedView.value === 1 && $routeParams.logChartFilter != defaultChartCriteria) {
              $routeParams.logChartFilter = defaultChartCriteria;
            } else {
//              manageCharts({});
              //instead of calling managecharts() explicitly, force date updates with routeparam touch timestamp
              $routeParams.ts = new Date().getTime();
            }

          };


          //watch for changes in the location
          //angular executes all watches on load :( which means we double load charts -
          $scope.$watch('routeParams', function (newFilter,oldFilter) {

            //setup sub charts filtering objects
            initChartFilters(newFilter,chartFilterContext);

            if (newFilter) {
              $scope.selectedFilter = newFilter;
            }

            manageCharts($scope.selectedFilter);
            //now we have to resize certain charts because highcharts only resizes/reflows after a window resize,
            //not on a parent container resize...meh
            //todo keeping this code around for chart memory access because there may be wasted memory
//            setTimeout(function(){
//            for(var i = 0; i < Highcharts.charts.length; i++){
//                if(typeof Highcharts.charts[i] !== 'undefined'){
//                  if(Highcharts.charts[i].renderTo.id === 'deviceModels'){
//                    console.log('found one',Highcharts.charts[i]);
//                    Highcharts.charts[i].setSize(Highcharts.charts[i].renderTo.parentNode.offsetWidth,400);
//                  }
//                }
//
//              }
//            },1000)


          }, true)


          function manageCharts(newFilter) {

            var excludeCrash = false;

            //sync current compare option
            $rootScope.currentCompare = $location.search().currentCompare

            //figure out what our top level filter is (avoids entire page request just for crashes/errors/and overview)
            if ($scope.ecDetailedView.value === 1) {
              $scope.ecDetailChartFilters = false;
              rawData = 'logRawData';
              chartData = 'logChartData';
              dataDescription.timeseries.colors = ['rgba(73,73,73,0.9)', 'rgba(255,3,3,0.5)'];
              dataDescription.timeseries.labels = ['App Errors', 'Crashes'];
              dataDescription.timeseries.dataAttr = ['errorAndAboveCount', 'crashCount'];
              dataDescription.timeseries.areaColors = [
                createGradient('rgba(73,73,73,0.5)', 'rgba(255,255,255,0.5)'),
                createGradient('rgba(255,3,3,0.5)', 'rgba(255,3,3,0.2)')
              ]
              dataDescription.bar1.labels = ['Crashes'];
              dataDescription.bar1.colors = [createGradient('rgba(255,3,3,0.5)', 'rgba(255,3,3,0.2)')];
              dataDescription.bar1.borderColor = 'rgba(255,3,3,0.5)';

              dataDescription.bar2.labels = ['Crashes'];
              dataDescription.bar2.colors = [createGradient('rgba(255,3,3,0.5)', 'rgba(255,3,3,0.2)')];
              dataDescription.bar2.borderColor = 'rgba(255,3,3,0.5)';

            } else if ($scope.ecDetailedView.value === 2) {
              $scope.ecDetailChartFilters = true;
              excludeCrash = true;
              rawData = 'logRawData';
              chartData = 'logChartData';
              dataDescription.timeseries.colors = ['#efac25', '#494949', '#ff4300']
              dataDescription.timeseries.labels = ['Warning', 'Errors', 'Critical'];
              dataDescription.timeseries.dataAttr = ['warnCount', 'errorAndAboveCount', 'assertCount'];
              dataDescription.timeseries.detailDataAttr = ['errorAndAboveCount'];
              dataDescription.timeseries.detailYaxisLabel = ['Errors'];
              dataDescription.timeseries.areaColors = [
                createGradient('rgba(239,172,37,0.5)', 'rgba(255,255,255,0.5)'),
                createGradient('rgba(73,73,73,0.5)', 'rgba(255,255,255,0.5)'),
                createGradient('rgba(255,67,0,0.5)', 'rgba(255,255,255,0.5)')]
              dataDescription.bar1.labels = ['Errors'];
              dataDescription.bar1.colors = ['rgba(73,73,73,0.5)', 'rgba(255,255,255,0.5)'];
              dataDescription.bar1.borderColor = '#494949';

            } else if ($scope.ecDetailedView.value === 3) {
              $scope.ecDetailChartFilters = true;
              chartData = 'crashChartData';
              rawData = 'crashRawData';
              dataDescription.timeseries.labels = ['Crashes'];
              dataDescription.timeseries.dataAttr = ['crashCount'];
              dataDescription.timeseries.colors = ['#ff0303']
              dataDescription.timeseries.areaColors = [createGradient('rgba(253,60,60,0.5)', 'rgba(255,3,3,0.5)')];
              dataDescription.timeseries.detailDataAttr = ['crashCount'];
              dataDescription.timeseries.detailYaxisLabel = ['Crashes'];

              dataDescription.bar1.labels = ['Crashes'];
              dataDescription.bar1.colors = [createGradient('rgba(255,3,3,0.5)', 'rgba(255,3,3,0.2)')];
              dataDescription.bar1.borderColor = 'rgba(255,3,3,0.5)';
            }

            //check for chartFilter setting in memory or on location path
            if (!newFilter[chartFilterContext]) {
              newFilter[chartFilterContext] = $location.search().logChartFilter
            }

            $rootScope.selectedChartCriteria = validateFilter(parseInt(newFilter[chartFilterContext]), $scope.chartCriteriaOptions, 'chartCriteriaId')

            //change the url to match newly selected option
            angular.forEach(newFilter, function (v, k) {
              $location.search(k, v);
            });

            //are we in compare mode?
            var compareExisting = $rootScope.currentCompare !== 'NOW';


            //determine if this is a chartFilter > 1 (sub filtering for bar charts)
            var mainChart;
            if($rootScope.selectedChartCriteria.chartName !== 'Overview'){
              mainChart = {'type': 'bar', 'call': 'convertParetoChart', 'template': 'pareto', dataDescriptionKey: 'bar1'};
            }else{
              mainChart = {'type': 'timeseries', 'call': ($scope.conversionMethod || 'convertAreaChart'), 'template': 'area', dataDescriptionKey: 'timeseries'};

            }



            //request chart data for page..
            $scope.requestChartData(chartData,newFilter[chartFilterContext],
              {
                period: newFilter.timeFilter,
                reference: 'NOW',
                chartType: mainChart.type
              }, function(mydata){

                //make a copy of the blank template from highcharts.json
                $scope.ecCharts.totals = angular.copy($rootScope.chartTemplate[mainChart.template]);

                //if compare - we must make an extra call for NOW data
                if(compareExisting){
                  //get the compare data
                  $scope.ecCharts.totals = angular.copy($scope.ecCharts.totals);
                  //request chart data for page..
                  $scope.requestChartData(chartData,newFilter[chartFilterContext],
                    {
                      period: newFilter.timeFilter,
                      reference: $rootScope.currentCompare,
                      chartType: mainChart.type
                    }, function(mycomparedata){
                      $scope.ecCharts.totals = charts[mainChart.call](mycomparedata.chartData, $scope.ecCharts.totals, dataDescription[mainChart.dataDescriptionKey], $rootScope.selectedtimefilter.settings, $rootScope.currentCompare);
                    });
                }

                $scope.ecCharts.totals = charts[mainChart.call](mydata.chartData, $scope.ecCharts.totals, dataDescription[mainChart.dataDescriptionKey], $rootScope.selectedtimefilter.settings, 'NOW');

                var tempString,
                  logDate;

                //provide popups for point click
                //todo - the whole point click thing needs a directive
                if(mainChart.type === 'timeseries'){
                  if ($scope.ecDetailedView.value === 1) {
                    createClickablePoint($scope.ecCharts.totals.series[0], rawData, 'logMessage');
                    createClickablePoint($scope.ecCharts.totals.series[1], 'crashRawData', 'crashSummary');
                  } else if ($scope.ecDetailedView.value === 2) {
                    createClickablePoint($scope.ecCharts.totals.series[0], rawData, 'logMessage', 'C');
                    createClickablePoint($scope.ecCharts.totals.series[1], rawData, 'logMessage', 'E');
                    createClickablePoint($scope.ecCharts.totals.series[2], rawData, 'logMessage', 'W');
                  } else if ($scope.ecDetailedView.value === 3) {
                    createClickablePoint($scope.ecCharts.totals.series[0], rawData, 'crashSummary');
                  }
                }


                function createClickablePoint(series, rawData, messageAttr, logLevel) {

                  series.point = {events: {
                    click: function (event) {
                      var d = new Date(this.category);
                      d.setSeconds(0);
                      d.setMilliseconds(0);
                      d = d.getTime();
                      var index = 0;
                      tempString = '';
                      var chart = this;

                      //todo dont make the call if no plotted data
                      $scope.$apply(function () {
                        data.jsonp(rawData, newFilter[chartFilterContext],
                          {
                            period: newFilter.timeFilter,
                            reference: $rootScope.currentCompare,
                            rowCount: '20',
                            excludeCrash: true,
                            callback: 'JSON_CALLBACK',
                            logLevel: logLevel,
                            fixedTime: d
                          }, function (mydata2) {

                            if (mydata2.length > 0) {
                              for (var i = 0; i < mydata2.length; i++) {
                                logDate = new Date(mydata2[i].timeStamp);
                                var icon;
                                if (mydata2[i].devicePlatform === 'Android') {
                                  icon = '<i class=\"sdk-icon-android\"></i>';
                                } else if (mydata2[i].devicePlatform === 'iPhone OS' || mydata2[i].devicePlatform === 'iPad OS') {
                                  icon = '<i class=\"sdk-icon-ios\"></i>';
                                }
                                tempString += '<div class=\"content-row\">' + icon + '<b>' + logDate.getHours() + ':' + (logDate.getMinutes() < 10 ? '0' : '') + logDate.getMinutes() + ':' + (logDate.getSeconds() < 10 ? '0' : '') + logDate.getSeconds() + '</b><br/><div class=\"log-message\">' + mydata2[i][messageAttr] + '</div></div>';
                              }

                              var popup = angular.element(document.getElementById('pointPopupTemp')),
                                newPopup,
                                holder = angular.element(document.getElementById('popupHolder'));
//                          newPopup = popup.clone();
//

                              var clonedElement = $compile(popup.html())($scope, function (clonedElement, $scope) {
                                //attach the clone to DOM document at the right place
                                holder.append(clonedElement);
                                newPopup = clonedElement;
                              });


                              newPopup.find('h3 span').html('Top ' + chart.series.name + ' at ' + logDate.getHours() + ':' + (logDate.getMinutes() < 10 ? '0' : '') + logDate.getMinutes());
                              newPopup.find('.content').html(tempString);
                              newPopup.removeClass('hide');
                              newPopup.css({'top': event.clientY, 'left': event.clientX});
                            }

                          })

                      })

                    }
                  }
                  };

                }

                //todo make this more high level
                $scope.scrollTo = function (id) {
                  $location.hash(id)
                  $anchorScroll()
                }

                $scope.hidePopup = function (e) {
                  angular.element(e.currentTarget).parent().addClass('hide')
                };

                //todo convert to directive (chart click)
                $scope.popup = function () {
                  return {
                    move: function (divid, xpos, ypos) {
                      var a = document.getElementById(divid);
                      divid.style.left = xpos + 'px';
                      divid.style.top = ypos + 'px';
                    },
                    startMoving: function (evt) {
                      evt = evt || window.event;
                      var posX = evt.clientX,
                        posY = evt.clientY,
                        a = evt.currentTarget,
                        divTop = a.style.top,
                        divLeft = a.style.left;
                      divTop = divTop.replace('px', '');
                      divLeft = divLeft.replace('px', '');
                      var diffX = posX - divLeft,
                        diffY = posY - divTop;
                      document.onmousemove = function (evt) {
                        evt = evt || window.event;
                        var posX = evt.clientX,
                          posY = evt.clientY,
                          aX = posX - diffX,
                          aY = posY - diffY;
                        $scope.popup.move(a, aX, aY);
                      }
                    },
                    stopMoving: function (ele) {
                      var a = document.createElement('script');
                      document.onmousemove = function () {
                      }
                    }
                  }
                }();

                //holder for summary data
                if (compareExisting) {
                  //we need to re-request the now data for proper compare
                  data.jsonp(chartData, newFilter[chartFilterContext],
                    {period: newFilter.timeFilter,
                      reference: $rootScope.currentCompare},
                    function (compareData) {
                      $scope.ecCharts.summary = compareSummaryData(mydata.summaryData, compareData.summaryData, $rootScope.currentCompare)
                    });

                } else {
                  $scope.ecCharts.summary = compareSummaryData($scope.ecCharts.summary, mydata.summaryData, 'NOW');
                }
              },true);

            //************** get log data

            $scope.itemsPerPage = 100;

            //todo remove this is a hack that requests 1 row just to see if there is data
            $scope.requestLogData(rawData, newFilter[chartFilterContext],
              {
                period: newFilter.timeFilter,
                reference: $rootScope.currentCompare,
                excludeCrash: excludeCrash,
                rowCount: 1},

              function (logData) {
                $scope.rawLogs = {rawDataType: rawData, rawData: logData}
              });


            //default log call
            $scope.requestLogData(rawData, newFilter[chartFilterContext],
              {
                period: newFilter.timeFilter,
                reference: $rootScope.currentCompare,
                excludeCrash: excludeCrash,
                rowCount: $scope.itemsPerPage},

              function (logData) {
                $scope.rawLogs = {rawDataType: rawData, rawData: logData}
              });


            //called when doing advanced search
            $scope.loadMoreLogs = function (itemsPerPage) {

              if (itemsPerPage) {
                $scope.itemsPerPage = itemsPerPage;
              }

              if($scope.autoUpdate){
                $scope.toggleAutoUpdate();
              }
//
//              if($rootScope.rawLogSearch.pageNumber === 1){
//                $rootScope.rawLogSearch.pageNumber = 0;
//              }

              data.jsonp(rawData, newFilter[chartFilterContext],
                {
                  period: newFilter.timeFilter,
                  reference: $rootScope.currentCompare,
                  rowCount: $scope.itemsPerPage,
                  excludeCrash: excludeCrash,
                  logMessage: $rootScope.rawLogSearch.logMessage,
                  tag: $rootScope.rawLogSearch.tag,
                  severity: $rootScope.rawLogSearch.severity,
                  devicePlatform: $rootScope.rawLogSearch.devicePlatform,
                  deviceOperatingSystem: $rootScope.rawLogSearch.deviceOperatingSystem,
                  deviceModel: $rootScope.rawLogSearch.deviceModel,
                  deviceId: $rootScope.rawLogSearch.deviceId,
                  start: ($rootScope.rawLogSearch.start)
                },
                function (logData) {
                  $scope.rawLogs = {rawDataType: rawData, rawData: logData}

                });

            };
//            'dataDescriptions': ['bar1', 'bar2']}
            //only make the below calls for overview page+ec combo view
            if ($rootScope.selectedChartCriteria.chartName === 'Overview' && $scope.ecDetailedView.value === 1) {

              //************** Create 2 bar charts for overview page
              //we must clone/copy the template object for multiple like charts on a page
              $scope.ecCharts.deviceModels = angular.copy(($scope.ecCharts.deviceModels || $rootScope.chartTemplate.pareto));
              $scope.ecCharts.platformVersions = angular.copy(($scope.ecCharts.platformVersions || $rootScope.chartTemplate.pareto));

//              {currentCompare: 'NOW', crashChartFilter: $scope.criteriaDeviceModels, timeFilter: newFilter.timeFilter};
              chartDataCall('crashChartData', deviceModelFilter, 'bar', $scope.ecCharts.deviceModels, 'convertParetoChart', 'bar1', function (chartData) {
                $scope.ecCharts.deviceModels = chartData;
              })

              chartDataCall('crashChartData', osVersionFilter, 'bar', $scope.ecCharts.platformVersions, 'convertParetoChart', 'bar2', function (chartData) {
                $scope.ecCharts.platformVersions = chartData;
              })

              if(newFilter.currentCompare === 'NOW'){

                //************** Create 2 pie charts for overview page
                //we must clone/copy the template object for multiple like charts on a page
                $scope.ecCharts.devicePlatform.chart = angular.copy(($scope.ecCharts.devicePlatform.chart || $rootScope.chartTemplate.pie));
                $scope.ecCharts.appVersion.chart = angular.copy(($scope.ecCharts.appVersion.chart || $rootScope.chartTemplate.pie));

                chartDataCall(chartData, platformFilter, 'pie', $scope.ecCharts.devicePlatform.chart, 'convertPieChart', 'pie', function (chartData1, rawData) {
                  $scope.ecCharts.devicePlatform.chart = chartData1;
                  $scope.ecCharts.devicePlatform.data = rawData;
                })

                chartDataCall(chartData, appVersionFilter, 'pie', $scope.ecCharts.appVersion.chart, 'convertPieChart', 'pie', function (chartData2, rawData) {
                  $scope.ecCharts.appVersion.chart = chartData2;
                  $scope.ecCharts.appVersion.data = rawData;
                })

              }
            }

          }

        });


      }

      $scope.appUsageSetup = function () {

        //override when navigating pages
        $rootScope.currentCompare = $routeParams.currentCompare = 'NOW';

        chartCriteria = 'sessionChartCriteria';
        chartData = 'sessionChartData';
        chartErrorMsg = 'Error getting chart data';
        chartFilterContext = 'sessionChartFilter';

        var pieRGB = '27,112,160'
        //data description tells the chart renderer what our labels are and what the incoming JSON data keys are that we want to plot
        dataDescription = {
          timeseries: {
            labels: ['Sessions'],
            yAxisLabels: [''],
            dataAttr: ['numSessions'],
            colors: ['#3ac62f', '#898989', '#ececec', '#ff33ee'],
            detailDataAttr: ['numSessions'],
            detailYaxisLabel: ['Sessions'],
            multiAxis: true
          },
          bar: {
            labels: ['Sessions'],
            dataAttr: ['attribute', 'value'],
            colors: [createGradient('rgba(69,196,0,0.6)','rgba(167,233,132,0.6)')],
            borderColor: '#3ac62f'
          },
          bar1: {
            labels: ['Sessions'],
            dataAttr: ['attribute', 'value'],
            colors: [createGradient('rgba(69,196,0,0.6)','rgba(167,233,132,0.6)')],
            borderColor: '#3ac62f'
          },
          pie: {
            dataAttr: ['attribute', 'percentage'],
            colors: [
              createGradient('rgba(' + pieRGB + ',0.9)', 'rgba(' + pieRGB + ',0.8)'),
              createGradient('rgba(' + pieRGB + ',0.8)', 'rgba(' + pieRGB + ',0.6)'),
              createGradient('rgba(' + pieRGB + ',0.6)', 'rgba(' + pieRGB + ',0.4)'),
              createGradient('rgba(' + pieRGB + ',0.4)', 'rgba(' + pieRGB + ',0.2)'),
              createGradient('rgba(' + pieRGB + ',0.2)', 'rgba(' + pieRGB + ',0.1)')
            ],
            borderColor: '#3ac62f'
          }
        };

        //this is the view object/holder for all charts in app usage page
        $scope.appUsageCharts = {
          totals: "",
          sessionsByModel: "",
          devicePlatform: {},
          appVersion: {},
          summary: {currentCompare: "NOW"}
        };



        getAppConfig().then(getChartCriteria)
          .then(function(chartCriteriaList){
            if(!chartCriteriaList || !chartCriteriaList.length ){
              return;
            }
            determineChartCriteria(chartCriteriaList)

          var defaultChartCriteria = [];
          if($scope.chartCriteriaOptions.length > 0){
            defaultChartCriteria = $scope.chartCriteriaOptions[0].chartCriteriaId;
          }
            //watch for changes in the location
          $scope.$watch('routeParams', function (newFilter) {

            //setup all the manual filters for sub charts
            initChartFilters(newFilter,chartFilterContext)

            //check for chartFilter setting in memory or on location path
            if (!newFilter[chartFilterContext]) {
              newFilter[chartFilterContext] = defaultChartCriteria;
            }
            $rootScope.selectedChartCriteria = validateFilter(parseInt(newFilter[chartFilterContext]), $scope.chartCriteriaOptions, 'chartCriteriaId')

            //change the url to match newly selected option
            angular.forEach(newFilter, function (v, k) {
              $location.search(k, v);
            });

            //sync current compare option
            $scope.appUsageCharts.summary.currentCompare = $rootScope.currentCompare = $location.search().currentCompare

            //determine if this is a chartFilter > 1 (sub filtering for bar charts)
            var mainChart = ($rootScope.selectedChartCriteria.chartName !== 'Overview' ? {'type': 'bar', 'call': 'convertParetoChart', 'template': 'pareto', 'dataDescriptions': ['bar1']} : {'type': 'TIMESERIES', 'call': 'convertLineChart', 'template': 'line', 'dataDescriptions': ['timeseries']}),
              compareExisting = $scope.appUsageCharts.summary.currentCompare !== 'NOW' && $rootScope.currentCompare !== 'NOW';


            //request chart data for page..

            $scope.requestChartData(chartData,newFilter[chartFilterContext],
              {
                period: newFilter.timeFilter,
                reference: 'NOW',
                chartType: mainChart.type
              }, function(mydata){
                //make a copy of the blank template
                $scope.appUsageCharts.totals = angular.copy($rootScope.chartTemplate[mainChart.template]);

                //use the performance service and create proper chart
                $scope.appUsageCharts.totals = charts[mainChart.call](mydata.chartData, $scope.appUsageCharts.totals, dataDescription[mainChart.dataDescriptions[0]], $rootScope.selectedtimefilter.settings, 'NOW');

                //if compare - we must make an extra call for NOW data
                if(compareExisting){
                  //get the compare data
                  $scope.appUsageCharts.totals = angular.copy($scope.appUsageCharts.totals);
                  //request chart data for page..
                  $scope.requestChartData(chartData,newFilter[chartFilterContext],
                    {
                      period: newFilter.timeFilter,
                      reference: $rootScope.currentCompare,
                      chartType: mainChart.type
                    }, function(mycomparedata){
                      $scope.appUsageCharts.totals = charts[mainChart.call](mycomparedata.chartData, $scope.appUsageCharts.totals, dataDescription[mainChart.dataDescriptions[0]], $rootScope.selectedtimefilter.settings, $rootScope.currentCompare);
                    });
                }

                //holder for summary data
                if (compareExisting) {
                  //we need to re-request the now data for proper compare
                  data.jsonp(chartData, newFilter[chartFilterContext],
                    {period: newFilter.timeFilter, reference: $rootScope.currentCompare},
                    function (compareData) {
                      $scope.appUsageCharts.summary = compareSummaryData(mydata.summaryData, compareData.summaryData, $rootScope.currentCompare)
                    });
                } else {
                  $scope.appUsageCharts.summary = compareSummaryData($scope.appUsageCharts.summary, mydata.summaryData, 'NOW')
                }
              },true);



            //we must clone/copy the template object for multiple like charts on a page
            $scope.appUsageCharts.sessionsByModel = angular.copy(($scope.appUsageCharts.sessionsByModel || $rootScope.chartTemplate.pareto));
            $scope.appUsageCharts.sessionsByPlatform = angular.copy(($scope.appUsageCharts.sessionsByPlatform || $rootScope.chartTemplate.pareto));

            chartDataCall(chartData, deviceModelFilter, 'bar', $scope.appUsageCharts.sessionsByModel, 'convertParetoChart', 'bar', function (chartData) {
              $scope.appUsageCharts.sessionsByModel = chartData;
            })

            chartDataCall(chartData, osVersionFilter, 'bar', $scope.appUsageCharts.sessionsByPlatform, 'convertParetoChart', 'bar', function (chartData) {
              $scope.appUsageCharts.sessionsByPlatform = chartData;
            })

            if(newFilter.currentCompare === 'NOW'){
              //we must clone/copy the template object for multiple like charts on a page
              $scope.appUsageCharts.devicePlatform.chart = angular.copy(($scope.appUsageCharts.devicePlatform.chart || $rootScope.chartTemplate.pie));
              $scope.appUsageCharts.appVersion.chart = angular.copy(($scope.appUsageCharts.appVersion.chart || $rootScope.chartTemplate.pie));

              chartDataCall(chartData, platformFilter, 'pie', $scope.appUsageCharts.devicePlatform.chart, 'convertPieChart', 'pie', function (chartData1, rawData) {
                $scope.appUsageCharts.devicePlatform.chart = chartData1;
                $scope.appUsageCharts.devicePlatform.data = rawData;
              })

              chartDataCall(chartData, appVersionFilter, 'pie', $scope.appUsageCharts.appVersion.chart, 'convertPieChart', 'pie', function (chartData2, rawData) {
                $scope.appUsageCharts.appVersion.chart = chartData2;
                $scope.appUsageCharts.appVersion.data = rawData;
              })
            }


          }, true)

        }, function (fail) {

        });


      }


      //functions share across each init

      function chartDataCall(chartData, newFilter, chartType, chartTemplate, convertMethod, dataDescriptionKey, successCallback) {
        data.jsonp(chartData, newFilter[chartFilterContext],
          {period: newFilter.timeFilter,
            reference: newFilter.currentCompare,
            chartType: chartType},
          function (mychartdata) {
            successCallback(charts[convertMethod](mychartdata.chartData, chartTemplate, dataDescription[dataDescriptionKey], $rootScope.selectedtimefilter.settings, newFilter.currentCompare), mychartdata);
          });
      }

      $scope.needHelpDialog = function(modalId){
        data.jsonp_raw('apigeeuihelpemail', '', {useremail: $rootScope.userEmail}).then(
            function (data) {
              $rootScope.$broadcast('alert', 'success', 'Email sent. Our team will be in touch with you shortly.');
              $rootScope.performance.showHelpButton = false;
            },
            function (rejectedData) {
              $rootScope.$broadcast('alert', 'error', 'Problem Sending Email. Try sending an email to mobile@apigee.com.');
            }
        );
        $scope.hideModal(modalId);
      };


    }]);

//data description tells the chart renderer what our labels are and what the incoming JSON data keys are that we want to plot
//util
function createGradient(color1, color2) {
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
  }
}

function compareSummaryData(oldData, newData, currentCompare) {
  for (var key in newData) {
    var newVal, labelVal, color, label, oldVal, percent;
    try {
      oldVal = (oldData[key].hasOwnProperty('value') ? oldData[key].value : oldData[key]);
    } catch (e) {
    }
    newVal = (newData[key].hasOwnProperty('value') ? newData[key].value : newData[key]);

    if (newData.hasOwnProperty(key)) {
      if (currentCompare === 'NOW') {
        color = 'black';
        label = newVal;
      } else {
        if (newVal === 0) {
          percent = 0;
        } else {
          percent = (Math.round((oldVal / newVal) * 100) - 100);
        }

        if (percent < 0) {
          color = 'red';
          label = percent + '%';
        } else if (percent > 0) {
          color = 'green';
          label = '+' + percent + '%'
        } else {
          color = 'black';
          label = 0;
        }
      }

    }
    newData[key] = {color: color, value: newVal, oldValue: oldVal, label: label};
  }
  newData.currentCompare = currentCompare;
  return newData;
}
