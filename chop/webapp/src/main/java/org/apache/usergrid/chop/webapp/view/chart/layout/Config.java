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
package org.apache.usergrid.chop.webapp.view.chart.layout;

import org.apache.usergrid.chop.webapp.service.chart.builder.ChartBuilder;
import org.apache.usergrid.chop.webapp.view.main.Breadcrumb;

class Config {

    private final ChartLayoutContext layoutContext;
    private final ChartBuilder chartBuilder;
    private final ChartLayout nextLayout;
    private final String chartId;
    private final String chartFile;
    private final Breadcrumb breadcrumb;

    Config(ChartLayoutContext layoutContext, ChartBuilder chartBuilder, ChartLayout nextLayout, String chartId, String chartFile, Breadcrumb breadcrumb) {
        this.layoutContext = layoutContext;
        this.chartBuilder = chartBuilder;
        this.nextLayout = nextLayout;
        this.chartId = chartId;
        this.chartFile = chartFile;
        this.breadcrumb = breadcrumb;
    }

    ChartLayoutContext getLayoutContext() {
        return layoutContext;
    }

    ChartBuilder getChartBuilder() {
        return chartBuilder;
    }

    ChartLayout getNextLayout() {
        return nextLayout;
    }

    String getChartId() {
        return chartId;
    }

    String getChartFile() {
        return chartFile;
    }

    Breadcrumb getBreadcrumb() {
        return breadcrumb;
    }
}
