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

import com.vaadin.ui.Notification;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.usergrid.chop.webapp.service.DataService;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.chart.Chart;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.Params.FailureType;
import org.apache.usergrid.chop.webapp.service.chart.Params.Metric;
import org.apache.usergrid.chop.webapp.service.chart.builder.ChartBuilder;
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


public class ChartLayout extends AbsoluteLayout implements JavaScriptFunction {

    private DataService dataService = InjectorFactory.getInstance( DataService.class );

    private final ChartBuilder chartBuilder;
    private final String chartFile;
    private final String chartId;
    protected final Params params;

    protected Button nextChartButton;

    protected ComboBox testNameCombo;
    protected ComboBox metricCombo;
    protected ComboBox percentileCombo;
    protected ComboBox failureCombo;
    protected DetailsTable detailsTable;
    protected NoteLayout noteLayout;


    public ChartLayout( ChartBuilder chartBuilder, String chartId, String chartFile, Params params ) {
        this.chartBuilder = chartBuilder;
        this.chartId = chartId;
        this.chartFile = chartFile;
        this.params = params;

        init();
        loadChart();
    }


    private void init() {
        setSizeFull();
        addItems();
        populateTestNames();
    }


    protected void addItems() {
        addParamsItems();
        addChartItems();
        addDetailsItems();
    }


    protected void populateTestNames() {
        Set<String> testNames = dataService.getTestNames( params.getModuleId() );
        UIUtil.populateCombo( testNameCombo, testNames.toArray( new String[] { } ) );
    }


    private void addDetailsItems() {
        detailsTable = new DetailsTable();
        addComponent( detailsTable, "left: 1010px; top: 25px;" );

        noteLayout = new NoteLayout();
        addComponent( noteLayout, "left: 1010px; top: 400px;" );
    }


    private void addChartItems() {

        UIUtil.addLayout( this, chartId, "left: 270px; top: 20px;", "720px", "550px" );

        nextChartButton = new Button( "..." );
        nextChartButton.setWidth( "150px" );
        nextChartButton.setVisible( false );
        nextChartButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                nextChartButtonClicked();
            }
        } );

        addComponent( nextChartButton, "left: 565px; top: 580px;" );
    }

    protected void nextChartButtonClicked() {

    }

    protected void addParamsItems() {

        testNameCombo = UIUtil.createCombo( "Test Name:", null );
        testNameCombo.setWidth( "155px" );

        metricCombo = UIUtil.createCombo( "Metric:", Metric.values() );
        metricCombo.setWidth( "155px" );

        percentileCombo = UIUtil.createCombo( "Percentile:",
                new String[] { "100", "90", "80", "70", "60", "50", "40", "30", "20", "10" } );
        percentileCombo.setWidth( "155px" );

        failureCombo = UIUtil.createCombo( "Points to Plot:", FailureType.values() );
        failureCombo.setWidth( "155px" );

        Button submitButton = new Button("Submit");
        submitButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                loadChart();
            }
        });

        FormLayout formLayout = addFormLayout();
        formLayout.addComponent( testNameCombo );
        formLayout.addComponent( metricCombo );
        formLayout.addComponent( percentileCombo );
        formLayout.addComponent( failureCombo );
        formLayout.addComponent( submitButton );
    }

    private FormLayout addFormLayout() {

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth( "250px" );
        formLayout.setHeight( "250px" );
        formLayout.addStyleName( "outlined" );
        formLayout.setSpacing( true );

        addComponent( formLayout, "left: 10px; top: 10px;" );

        return formLayout;
    }


    protected Params getParams() {
        return new Params(
                params.getModuleId(),
                (String) testNameCombo.getValue(),
                params.getCommitId(),
                params.getRunNumber(),
                (Metric) metricCombo.getValue(),
                Integer.parseInt( ( String ) percentileCombo.getValue() ),
                (FailureType) failureCombo.getValue()
        );
    }


    public void loadChart() {

        Params params = getParams();
        if ( params.getTestName() == null ){
            Notification.show( "Warning", "You don't have run results yet !!!", Notification.Type.WARNING_MESSAGE );
            return;
        }

        // BUG: If common.js is loaded separately its content is not visible later
        String chartContent = FileUtil.getContent( "js/common.js" ) + FileUtil.getContent( chartFile );
        Chart chart = chartBuilder.getChart( getParams() );

        chartContent = chartContent.replace( "$categories", Format.formatCategories( chart.getCategories() ) );
        chartContent = chartContent.replace( "$points", Format.formatPoints( chart.getSeries() ) );
        chartContent = chartContent.replace( "$data", Format.formatData( chart.getSeries() ) );

        JavaScriptUtil.loadChart( chartContent, "chartCallback", this );
    }


    @Override
    public void call( JSONArray args ) throws JSONException {
        JSONObject json = args.getJSONObject( 0 );
        pointClicked( json );
    }


    protected void pointClicked( JSONObject json ) throws JSONException {
        params.setCommitId( json.optString( "commitId" ) );
        params.setRunNumber( json.optInt( "runNumber", 0 ) );

        detailsTable.setContent( json );
        noteLayout.load( params.getCommitId(), params.getRunNumber() );
    }
}