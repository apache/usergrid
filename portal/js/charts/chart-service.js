AppServices.Services.factory('charts', function () {

  var lineChart,
    areaChart,
    paretoChart,
    pieChart,
    pieCompare,
    xaxis,
    seriesIndex;

  return {
    convertLineChart: function (chartData, chartTemplate, dataDescription, settings, currentCompare) {

      lineChart = chartTemplate;
//      console.log('chartData',chartData[0])
      if (typeof chartData[0] === 'undefined') {
        chartData[0] = {};
        chartData[0].datapoints = []
      }
      var dataPoints = chartData[0].datapoints,
      dPLength = dataPoints.length,
      label;

      if(currentCompare === 'YESTERDAY'){
//        lineChart = chartTemplate;
        seriesIndex = dataDescription.dataAttr.length;
        label = 'Yesterday ';
      }
      else if(currentCompare === 'LAST_WEEK'){
//        lineChart = chartTemplate;
        seriesIndex = dataDescription.dataAttr.length;
        label = 'Last Week ';
      }else{
        lineChart = chartTemplate;

        seriesIndex = 0;
        lineChart.series = [];
        label = '';
      }
      xaxis = lineChart.xAxis[0];
      xaxis.categories = [];



      //the next 2 setting options are provided in the timeFormat dropdown, so we must inspect them here
      if (settings.xaxisformat) {
        xaxis.labels.formatter = new Function(settings.xaxisformat);
      }
      if (settings.step) {
        xaxis.labels.step = settings.step;
      }
      //end check

      for (var i = 0; i < dPLength; i++) {
        var dp = dataPoints[i];
        xaxis.categories.push(dp.timestamp);
      }

      //check to see if there are multiple "chartGroupNames" in the object, otherwise "NA" will go to the else
      if(chartData.length > 1){

        for (var l = 0; l < chartData.length; l++){

          if(chartData[l].chartGroupName){

            dataPoints = chartData[l].datapoints;
//            dPLength = dataPoints.length;

            lineChart.series[l] = {};
            lineChart.series[l].data = [];
            lineChart.series[l].name = chartData[l].chartGroupName;
            lineChart.series[l].yAxis = 0;
            lineChart.series[l].type = 'line';
            lineChart.series[l].color = dataDescription.colors[i];
            lineChart.series[l].dashStyle = 'solid';

            lineChart.series[l].yAxis.title.text = dataDescription.yAxisLabels;

            plotData(l,dPLength,dataPoints,dataDescription.detailDataAttr,true)
          }
        }
      }else{

      var steadyCounter = 0;

        //loop over incoming data members for axis setup... create empty arrays and settings ahead of time
        //the seriesIndex is for the upcoming compare options - if compare is clicked... if it isn't just use 0 :/
        for (var i = seriesIndex;i < (dataDescription.dataAttr.length + (seriesIndex > 0 ? seriesIndex : 0)); i++) {
          var yAxisIndex = dataDescription.multiAxis ? steadyCounter : 0;
          lineChart.series[i] = {};
          lineChart.series[i].data = [];
          lineChart.series[i].name = label + dataDescription.labels[steadyCounter];
          lineChart.series[i].yAxis = yAxisIndex;
          lineChart.series[i].type = 'line';
          lineChart.series[i].color = dataDescription.colors[i];
          lineChart.series[i].dashStyle = 'solid';

          lineChart.yAxis[yAxisIndex].title.text = dataDescription.yAxisLabels[(dataDescription.yAxisLabels > 1 ? steadyCounter : 0)];
          steadyCounter++;
        }

        plotData(seriesIndex,dPLength,dataPoints,dataDescription.dataAttr,false)
      }

      function plotData(counter,dPLength,dataPoints,dataAttrs,detailedView){
        //massage the data... happy ending
        for (var i = 0; i < dPLength; i++) {
          var dp = dataPoints[i];

          var localCounter = counter;
          //loop over incoming data members
          for (var j = 0; j < dataAttrs.length; j++) {
            if(typeof dp === 'undefined'){
              lineChart.series[localCounter].data.push([i, 0]);
            }else{
              lineChart.series[localCounter].data.push([i, dp[dataAttrs[j]]]);
            }
            if(!detailedView){
            localCounter++;
            }

          }

        }
      }
      return lineChart;
    },

    convertAreaChart: function (chartData, chartTemplate, dataDescription, settings, currentCompare) {

      areaChart = angular.copy(areaChart);
//      console.log('chartData',chartData[0])
      if (typeof chartData[0] === 'undefined') {
        chartData[0] = {};
        chartData[0].datapoints = []
      }
      var dataPoints = chartData[0].datapoints,
        dPLength = dataPoints.length,
        label;

      if(currentCompare === 'YESTERDAY'){
//        areaChart = chartTemplate;
        seriesIndex = dataDescription.dataAttr.length;
        label = 'Yesterday ';
      }
      else if(currentCompare === 'LAST_WEEK'){
//        areaChart = chartTemplate;
        seriesIndex = dataDescription.dataAttr.length;
        label = 'Last Week ';
      }else{
        areaChart = chartTemplate;

        seriesIndex = 0;
        areaChart.series = [];
        label = '';
      }
      xaxis = areaChart.xAxis[0];
      xaxis.categories = [];



      //the next 2 setting options are provided in the timeFormat dropdown, so we must inspect them here
      if (settings.xaxisformat) {
        xaxis.labels.formatter = new Function(settings.xaxisformat);
      }
      if (settings.step) {
        xaxis.labels.step = settings.step;
      }
      //end check

      for (var i = 0; i < dPLength; i++) {
        var dp = dataPoints[i];
        xaxis.categories.push(dp.timestamp);
      }

      //check to see if there are multiple "chartGroupNames" in the object, otherwise "NA" will go to the else
      if(chartData.length > 1){

        for (var l = 0; l < chartData.length; l++){

          if(chartData[l].chartGroupName){

            dataPoints = chartData[l].datapoints;
//            dPLength = dataPoints.length;

            areaChart.series[l] = {};
            areaChart.series[l].data = [];
            areaChart.series[l].fillColor = dataDescription.areaColors[l];
            areaChart.series[l].name = chartData[l].chartGroupName;
            areaChart.series[l].yAxis = 0;
            areaChart.series[l].type = 'area';
            areaChart.series[l].pointInterval = 1;
            areaChart.series[l].color = dataDescription.colors[l];
            areaChart.series[l].dashStyle = 'solid';

            areaChart.series[l].yAxis.title.text = dataDescription.yAxisLabels;

            plotData(l,dPLength,dataPoints,dataDescription.detailDataAttr,true)
          }
        }
      }else{

        var steadyCounter = 0;

        //loop over incoming data members for axis setup... create empty arrays and settings ahead of time
        //the seriesIndex is for the upcoming compare options - if compare is clicked... if it isn't just use 0 :/
        for (var i = seriesIndex;i < (dataDescription.dataAttr.length + (seriesIndex > 0 ? seriesIndex : 0)); i++) {
          var yAxisIndex = dataDescription.multiAxis ? steadyCounter : 0;
          areaChart.series[i] = {};
          areaChart.series[i].data = [];
          areaChart.series[i].fillColor = dataDescription.areaColors[i];
          areaChart.series[i].name = label + dataDescription.labels[steadyCounter];
          areaChart.series[i].yAxis = yAxisIndex;
          areaChart.series[i].type = 'area';
          areaChart.series[i].pointInterval = 1;
          areaChart.series[i].color = dataDescription.colors[i];
          areaChart.series[i].dashStyle = 'solid';

          areaChart.yAxis[yAxisIndex].title.text = dataDescription.yAxisLabels[(dataDescription.yAxisLabels > 1 ? steadyCounter : 0)];
          steadyCounter++;
        }

        plotData(seriesIndex,dPLength,dataPoints,dataDescription.dataAttr,false)
      }

      function plotData(counter,dPLength,dataPoints,dataAttrs,detailedView){
        //massage the data... happy ending
        for (var i = 0; i < dPLength; i++) {
          var dp = dataPoints[i];

          var localCounter = counter;
          //loop over incoming data members
          for (var j = 0; j < dataAttrs.length; j++) {
            if(typeof dp === 'undefined'){
              areaChart.series[localCounter].data.push(0);
            }else{
              areaChart.series[localCounter].data.push(dp[dataAttrs[j]]);
            }
            if(!detailedView){
              localCounter++;
            }

          }

        }
      }
      return areaChart;
    },

    convertParetoChart: function (chartData, chartTemplate, dataDescription, settings, currentCompare) {

      paretoChart = chartTemplate;

      if (typeof chartData === 'undefined') {
        chartData = [];
      }

      var label,
        cdLength = chartData.length,
        compare = false,
        allParetoOptions = [],
        stackedBar = false;

      seriesIndex = 0;

      function getPreviousData(){
        for(var i = 0;i < chartTemplate.series[0].data.length;i++){
          //pulling the "now" values for comparison later, assuming they will be in the 0 index :)
           allParetoOptions.push(chartTemplate.xAxis.categories[i])
        }
      }

      if(typeof dataDescription.dataAttr[1] === 'object'){
        stackedBar = true;
      }

      if(currentCompare === 'YESTERDAY'){
        label = 'Yesterday ';
        compare = true;
        if(stackedBar){
          seriesIndex = dataDescription.dataAttr[1].length;
        }
        getPreviousData()
      }
      else if(currentCompare === 'LAST_WEEK'){
        label = 'Last Week ';
        compare = true;
        if(stackedBar){
          seriesIndex = dataDescription.dataAttr[1].length;
        }
        seriesIndex =
        getPreviousData()
      }else{
        compare = false;
        label = '';
        paretoChart.xAxis.categories = [];
        paretoChart.series = [];
        paretoChart.series[0] = {};
        paretoChart.series[0].data = [];
        paretoChart.legend.enabled = false;
      }

      paretoChart.plotOptions.series.borderColor = dataDescription.borderColor;


      //create a basic compare series (more advanced needed for stacked bar)
      if(compare && !stackedBar){
        paretoChart.series[1] = {};
        paretoChart.series[1].data = [];
        //repopulate array with 0 values based on length of NOW data
        for(var i = 0; i < allParetoOptions.length; i++) {
          paretoChart.series[1].data.push(0);
        }
        paretoChart.legend.enabled = true;
//        paretoChart.series[1].name = label;
//        paretoChart.series[0].name = "Now";
      }

      for (var i = 0; i < cdLength; i++) {
        var bar = chartData[i];

        if(!compare){
          paretoChart.xAxis.categories.push(bar[dataDescription.dataAttr[0]]);

          //if we send multiple attributes to be plotted, assume it's a stacked bar for now
          if(typeof dataDescription.dataAttr[1] === 'object'){
            createStackedBar(dataDescription,paretoChart,paretoChart.series.length);
          }else{
            paretoChart.series[0].data.push(bar[dataDescription.dataAttr[1]]);
            paretoChart.series[0].name = dataDescription.labels[0];
            paretoChart.series[0].color = dataDescription.colors[0];
          }

        }else{

          //check if this is a stacked bar


          var newLabel = bar[dataDescription.dataAttr[0]],
              newValue = bar[dataDescription.dataAttr[1]],
              previousIndex = allParetoOptions.indexOf(newLabel);

              //make sure this label existed in the NOW data
              if(previousIndex > -1){
                if(typeof dataDescription.dataAttr[1] === 'object'){
                  createStackedBar(dataDescription,paretoChart,paretoChart.series.length);
                }else{
                  paretoChart.series[1].data[previousIndex] = newValue;
                  paretoChart.series[1].name = (label !== '' ? label + ' ' + dataDescription.labels[0] : dataDescription.labels[0]);
                  paretoChart.series[1].color = dataDescription.colors[1];
                }
              }else{
                //not found for comparison
              }


        }

      }

      function createStackedBar(dataDescription,paretoChart,startingPoint){

          paretoChart.plotOptions = {
            series: {
              shadow: false,
              borderColor: dataDescription.borderColor,
              borderWidth: 1
            },
            column: {
              stacking: 'normal',
              dataLabels: {
                enabled: true,
                color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white'
              }
            }
          };

          var start = dataDescription.dataAttr[1].length,
            steadyCounter = 0,
            stackName = label;

          if(compare){
            paretoChart.legend.enabled = true;
          }

          for (var f = seriesIndex; f < (start + seriesIndex); f++) {
            if(!paretoChart.series[f]){
              paretoChart.series[f] = {'data':[]}
            }
            paretoChart.series[f].data.push(bar[dataDescription.dataAttr[1][steadyCounter]]);
            paretoChart.series[f].name = (label !== '' ? label + ' ' + dataDescription.labels[steadyCounter] : dataDescription.labels[steadyCounter]);
            paretoChart.series[f].color = dataDescription.colors[f];
            paretoChart.series[f].stack = label;
            steadyCounter++
          }


      }

      return paretoChart;
    },

    convertPieChart: function (chartData, chartTemplate, dataDescription, settings, currentCompare) {

      var label,
        cdLength = chartData.length,
        compare = false;

      pieChart = chartTemplate;

      if(currentCompare === 'YESTERDAY'){
        label = 'Yesterday ';
        compare = false; //override for now to false
      }
      else if(currentCompare === 'LAST_WEEK'){
        label = 'Last Week ';
        compare = false; //override for now to false
      }else{
        compare = false;

        pieChart.series[0].data = [];

        if (pieChart.series[0].dataLabels) {
          if(typeof pieChart.series[0].dataLabels.formatter === 'string'){
            pieChart.series[0].dataLabels.formatter = new Function(pieChart.series[0].dataLabels.formatter);
          }
        }

      }

      pieChart.plotOptions.pie.borderColor = dataDescription.borderColor;

      if(compare){
        pieChart.series[1].data = [];
        if (pieChart.series[1].dataLabels) {
          if(typeof pieChart.series[1].dataLabels.formatter === 'string'){
            pieChart.series[1].dataLabels.formatter = new Function(pieChart.series[1].dataLabels.formatter);
          }
        }
      }

      var tempArray = [];
      for (var i = 0; i < cdLength; i++) {
        var pie = chartData[i];

        tempArray.push({
          name:pie[dataDescription.dataAttr[0]],
          y:pie[dataDescription.dataAttr[1]],
          color:''
        });

      }


      //sort by name prop so we can have a good looking comparison donut
      sortJsonArrayByProperty(tempArray,'name');
      //add colors so they match up
      for (var i = 0; i < tempArray.length; i++) {
          tempArray[i].color = dataDescription.colors[i];
      }

      if(!compare){
        pieChart.series[0].data = tempArray;
      }else{
        pieChart.series[1].data = tempArray;
      }

      return pieChart;
    }
  }


  function sortJsonArrayByProperty(objArray, prop, direction){
    if (arguments.length<2) throw new Error("sortJsonArrayByProp requires 2 arguments");
    var direct = arguments.length>2 ? arguments[2] : 1; //Default to ascending

    if (objArray && objArray.constructor===Array){
      var propPath = (prop.constructor===Array) ? prop : prop.split(".");
      objArray.sort(function(a,b){
        for (var p in propPath){
          if (a[propPath[p]] && b[propPath[p]]){
            a = a[propPath[p]];
            b = b[propPath[p]];
          }
        }
        // convert numeric strings to integers
        a = a.match(/^\d+$/) ? +a : a;
        b = b.match(/^\d+$/) ? +b : b;
        return ( (a < b) ? -1*direct : ((a > b) ? 1*direct : 0) );
      });
    }
  }

});