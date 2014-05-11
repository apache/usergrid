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


import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.chop.webapp.service.DataService;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.chart.Chart;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.Params.FailureType;
import org.apache.usergrid.chop.webapp.service.chart.Params.Metric;
import org.apache.usergrid.chop.webapp.service.chart.builder.OverviewChartBuilder;
import org.apache.usergrid.chop.webapp.service.util.FileUtil;
import org.apache.usergrid.chop.webapp.view.chart.format.Format;
import org.apache.usergrid.chop.webapp.view.chart.layout.item.DetailsTable;
import org.apache.usergrid.chop.webapp.view.chart.layout.item.NoteLayout;
import org.apache.usergrid.chop.webapp.view.util.JavaScriptUtil;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;

import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.JavaScriptFunction;


public class ChartLayoutNew extends AbsoluteLayout implements JavaScriptFunction {

    private DataService dataService = InjectorFactory.getInstance(DataService.class);

    protected final Config config;

    protected ComboBox testNameCombo;
    protected ComboBox metricCombo;
    protected ComboBox percentileCombo;
    protected ComboBox failureCombo;
    protected Button nextChartButton;
    protected DetailsTable detailsTable;
    protected NoteLayout noteLayout;
    protected Params params;

    public ChartLayoutNew( Config config ) {
        this.config = config;
        setSizeFull();
        addItems();
        loadChart();
    }


    protected void addItems() {
        addParamsItems();
        addChartItems();
        addDetailsItems();

        populateTestNames();
    }


    protected void populateTestNames() {
        Set<String> testNames = dataService.getTestNames( "1414303914" );
        UIUtil.populateCombo( testNameCombo, testNames.toArray( new String[] { } ) );
    }


    private void addDetailsItems() {

        detailsTable = new DetailsTable();
        addComponent( detailsTable, "left: 1000px; top: 10px;" );

        noteLayout = new NoteLayout();
        addComponent( noteLayout, "left: 1000px; top: 400px;" );
    }


    private void addChartItems() {

        UIUtil.addLayout( this, "overviewChart", "left: 270px; top: 10px;", "720px", "550px" );

        nextChartButton = new Button( "Next >>" );
        addComponent( nextChartButton, "left: 610px; top: 570px;" );
        nextChartButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {

            }
        } );
    }


    protected void addParamsItems() {

        testNameCombo = UIUtil.createCombo( "Test Name:", null );
        testNameCombo.setWidth( "155px" );

        metricCombo = UIUtil.createCombo( "Metric:", Metric.values() );

        percentileCombo = UIUtil.createCombo( "Percentile:",
                new String[] { "100", "90", "80", "70", "60", "50", "40", "30", "20", "10" } );

        failureCombo = UIUtil.createCombo( "Points to Plot:", FailureType.values() );

        Button submitButton = new Button("Submit");
        submitButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {

            }
        });

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth( "250px" );
        formLayout.setHeight( "250px" );
        formLayout.addStyleName( "outlined" );
        formLayout.setSpacing( true );

        formLayout.addComponent( testNameCombo );
        formLayout.addComponent( metricCombo );
        formLayout.addComponent( percentileCombo );
        formLayout.addComponent( failureCombo );
        formLayout.addComponent( submitButton );

        addComponent( formLayout, "left: 10px; top: 10px;" );

    }


    protected Params getParams() {
        return new Params(
//                params.getModuleId(),
                "1414303914",
                (String) testNameCombo.getValue(),
//                params.getCommitId(),
                null,
//                params.getRunNumber(),
                0,
                (Metric) metricCombo.getValue(),
                Integer.parseInt( ( String ) percentileCombo.getValue() ),
                (FailureType) failureCombo.getValue()
        );
    }


    public void loadChart() {

        OverviewChartBuilder overviewChartBuilder = InjectorFactory.getInstance(OverviewChartBuilder.class);

//        Params params = new Params("1414303914");
        Params params = getParams();

        // BUG: If common.js is loaded separately its content is not visible later
        String chartContent = FileUtil.getContent( "js/common.js" )
//                + FileUtil.getContent( config.getChartFile() );
                + FileUtil.getContent( "js/overview-chart.js" );

//        Chart chart = config.getChartBuilder().getChart( params );
        Chart chart = overviewChartBuilder.getChart( params );

        chartContent = chartContent.replace( "$categories", Format.formatCategories( chart.getCategories() ) );
        chartContent = chartContent.replace( "$points", Format.formatPoints( chart.getSeries() ) );
        chartContent = chartContent.replace( "$data", Format.formatData( chart.getSeries() ) );

        JavaScriptUtil.loadChart( chartContent, "chartCallback", this );
    }


    @Override
    public void call(JSONArray args) throws JSONException {
        JSONObject json = args.getJSONObject(0);

        String caption = "Commit: " + StringUtils.abbreviate( json.getString( "commitId" ), 10 );
        nextChartButton.setCaption(caption);

        pointClicked(json);
    }

    protected void pointClicked(JSONObject json) throws JSONException {
//        params.setCommitId(json.optString("commitId"));
//        params.setRunNumber(json.optInt("runNumber", 0));

        detailsTable.setContent(json);
//        noteLayout.load(params.getCommitId(), params.getRunNumber());
    }


}
