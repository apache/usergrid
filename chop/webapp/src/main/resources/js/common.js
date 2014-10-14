/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

var POINTS = [];
var CATEGORIES = [];

function getPointRadius(i, j) {

    var point = POINTS[i][j];
    var radius = 4;

    if (point.failures > 1000) {
        radius = 15;
    } else if (point.failures > 500) {
        radius = 10;
    } else if (point.failures > 100) {
        radius = 7;
    }

    return radius;
}

function getColor(i, j) {

    var point = POINTS[i][j];
    var color = "silver";

    if (point.failures > 0) {
        color = "red";
    } else if (point.ignores > 0) {
        color = "yellow";
    }

    return color;
}

function drawHook(plot, ctx) {

    var data = plot.getData();
    var axes = plot.getAxes();
    var offset = plot.getPlotOffset();

    for (var i = 0; i < data.length; i++) {
        var series = data[i];

        for (var j = 0; j < series.data.length; j++) {
            var d = ( series.data[j] );
            var x = offset.left + axes.xaxis.p2c(d[0]);
            var y = offset.top + axes.yaxis.p2c(d[1]);
            var r = getPointRadius(i, j);
            var color = getColor(i, j);

            ctx.beginPath();
            ctx.arc(x, y, r, 0, Math.PI * 2, true);
            ctx.closePath();
            ctx.fillStyle = color;
            ctx.fill();
        }
    }
}

function pointClicked(event, pos, item) {
    var point = POINTS[item.seriesIndex][item.dataIndex];
    chartCallback(point.properties);
}

function tickFormatter(value, axis) {
    return CATEGORIES.length > 0 ? CATEGORIES[value] : value;
}

var OPTIONS = {
    legend: {
        show: false
    },
    grid: {
        hoverable: true,
        clickable: true,
        backgroundColor: "white"
    },
    series: {
        lines: { show: true },
        points: { show: true }
    },
    xaxis: {
        minTickSize: 1,
        tickDecimals: 0,
        tickFormatter: tickFormatter
    },
    hooks: { draw: [ drawHook ] }
};

function showChart(chartId, data) {

    if (data == null || data.length < 1) {
        return;
    }

    var chart = $(chartId);

    $.plot(chart, data, OPTIONS);

    chart.bind("plotclick", pointClicked);
}

