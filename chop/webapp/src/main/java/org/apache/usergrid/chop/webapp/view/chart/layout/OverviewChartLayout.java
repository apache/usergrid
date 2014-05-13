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


import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.builder.OverviewChartBuilder;
import org.apache.usergrid.chop.webapp.view.main.TabSheetManager;

import com.vaadin.ui.AbsoluteLayout;


public class OverviewChartLayout extends ChartLayout {

    private final TabSheetManager tabSheetManager;

    public OverviewChartLayout( Params params, TabSheetManager tabSheetManager ) {
        super( InjectorFactory.getInstance( OverviewChartBuilder.class ), "overviewChart", "js/overview-chart.js", params );
        this.tabSheetManager = tabSheetManager;
    }


    @Override
    protected void pointClicked( JSONObject json ) throws JSONException {
        super.pointClicked( json );

        String caption = "Commit: " + StringUtils.abbreviate( json.getString( "commitId" ), 10 );
        nextChartButton.setCaption( caption );
        nextChartButton.setVisible( true );
    }


    @Override
    protected void nextChartButtonClicked() {
        AbsoluteLayout layout = new RunsChartLayout( getParams(), tabSheetManager );
        tabSheetManager.addTab( layout, "Runs Chart" );
    }

}
