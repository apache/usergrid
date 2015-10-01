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

POINTS = $points;
var DATA = $data;
var CHECKBOXES = $("#runnersCheckboxes");

OPTIONS.legend = {
    position: "nw"
};

function addCheckboxes() {

    CHECKBOXES.empty();

    $.each(DATA, function (i, series) {
        CHECKBOXES.append("<input type='checkbox' name='" + series.label
            + "' checked='checked' id='id" + series.label + "'></input>"
            + "<label for='id" + series.label + "'>"
            + series.label + "</label>");
    });

    CHECKBOXES.find("input").click(plot);
}

function getPlotData() {
    var data = [];

    CHECKBOXES.find("input").each(function (i, checkbox) {
        if (checkbox.checked) {
            data.push(DATA[i]);
        }
    });

    return data;
}

function plot() {
    showChart("#iterationsChart", getPlotData());
}

addCheckboxes();
plot();

