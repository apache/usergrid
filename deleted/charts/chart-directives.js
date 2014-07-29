'use strict';

AppServices.Directives.directive('chart', function ($rootScope) {
  return {
    restrict: 'E',
    scope: {
      chartdata: '=chartdata'
    },
    template: '<div></div>',
    replace: true,
    controller: function($scope, $element) {


    },
    link: function (scope, element, attrs) {
      //we need scope.watch because the value is not populated yet
      //http://stackoverflow.com/questions/14619884/angularjs-passing-object-to-directive
      scope.$watch('chartdata', function(chartdata,oldchartdata) {

        if(chartdata){
          //set chart defaults through tag attributes
          var chartsDefaults = {
            chart: {
              renderTo: element[0],
              type: attrs.type || null,
              height: attrs.height || null,
              width: attrs.width || null,
              reflow: true,
              animation: false,
              zoomType: 'x'
//              events: {
//                redraw: resize,
//                load: resize
//              }
            }
          }

          if(attrs.type === 'pie'){
            chartsDefaults.chart.margin = [0, 0, 0, 0];
            chartsDefaults.chart.spacingLeft = 0;
            chartsDefaults.chart.spacingRight = 0;
            chartsDefaults.chart.spacingTop = 0;
            chartsDefaults.chart.spacingBottom = 0;

            if(attrs.titleimage){
              chartdata.title.text = '<img src=\"'+ attrs.titleimage +'\">';
            }

            if(attrs.titleicon){
              chartdata.title.text = '<i class=\"pictogram ' + attrs.titleiconclass + '\">' + attrs.titleicon + '</i>';
            }

            if(attrs.titlecolor){
              chartdata.title.style.color = attrs.titlecolor;
            }

            if(attrs.titleimagetop){
              chartdata.title.style.marginTop = attrs.titleimagetop;
            }

            if(attrs.titleimageleft){
              chartdata.title.style.marginLeft = attrs.titleimageleft;
            }

          }

          if(attrs.type === 'line'){
            chartsDefaults.chart.marginTop = 30;
            chartsDefaults.chart.spacingTop = 50;
//            chartsDefaults.chart.zoomType = null;
          }

          if(attrs.type === 'column'){
            chartsDefaults.chart.marginBottom = 80;
//            chartsDefaults.chart.spacingBottom = 50;
//            chartsDefaults.chart.zoomType = null;
          }

          if(attrs.type === 'area'){
            chartsDefaults.chart.spacingLeft = 0;
            chartsDefaults.chart.spacingRight = 0;
            chartsDefaults.chart.marginLeft = 0;
            chartsDefaults.chart.marginRight = 0;
          }

          Highcharts.setOptions({
            global : {
              useUTC : false
            },
            chart: {
              style: {
                fontFamily: 'marquette-light, Helvetica, Arial, sans-serif'
              }
            }
          });

          //          scope.$parent.$watch('apptest.step',function(step){
          //            xAxis1.labels.step = step
          //            renderChart(chartsDefaults,chartdata);
          //          })

          if(attrs.type === 'line'){
            //------line charts
            var xAxis1 = chartdata.xAxis[0];

            //check for previous setting from service layer or json template... if it doesn't exist use the attr value
            if(!xAxis1.labels.formatter){
              xAxis1.labels.formatter = new Function(attrs.xaxislabel);
            }
            if(!xAxis1.labels.step){
              xAxis1.labels.step = attrs.xaxisstep;
            }
            //end check
          }





          //pull any stringified from template JS and eval it
          if(chartdata.tooltip){
            if(typeof chartdata.tooltip.formatter === 'string'){
              chartdata.tooltip.formatter = new Function(chartdata.tooltip.formatter);
            }
          }

          renderChart(chartsDefaults,chartdata);
        }

      },true)
    }
  }

});


function renderChart(chartsDefaults,chartdata,attrs){
  var newSettings = {};
  $.extend(true, newSettings, chartsDefaults, chartdata);
  var chart = new Highcharts.Chart(newSettings);
}