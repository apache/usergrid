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

import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.JavaScriptFunction;
import com.vaadin.ui.themes.Reindeer;
import org.apache.usergrid.chop.webapp.service.DataService;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.chart.Chart;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.Params.FailureType;
import org.apache.usergrid.chop.webapp.service.chart.Params.Metric;
import org.apache.usergrid.chop.webapp.service.util.FileUtil;
import org.apache.usergrid.chop.webapp.view.chart.format.Format;
import org.apache.usergrid.chop.webapp.view.chart.layout.item.DetailsTable;
import org.apache.usergrid.chop.webapp.view.chart.layout.item.NoteLayout;
import org.apache.usergrid.chop.webapp.view.util.JavaScriptUtil;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public abstract class ChartLayout extends AbsoluteLayout implements JavaScriptFunction {

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

    protected ChartLayout(Config config) {
        this.config = config;
        setSizeFull();
        addControls();
    }

    protected abstract void handleBreadcrumb();

    @Override
    public void call(JSONArray args) throws JSONException {
        JSONObject json = args.getJSONObject(0);
        pointClicked(json);
    }

    protected void addControls() {
        addMainControls();
        addSubControls(410);
    }

    protected void addMainControls() {

        testNameCombo = UIUtil.addCombo(this, "Test Name:", "left: 10px; top: 30px;", "500px", null);

        metricCombo = UIUtil.addCombo(this, "Metric:", "left: 10px; top: 80px;", "100px", Metric.values());

        percentileCombo = UIUtil.addCombo(this, "Percentile:", "left: 150px; top: 80px;", "100px",
                new String[]{"100", "90", "80", "70", "60", "50", "40", "30", "20", "10"});

        failureCombo = UIUtil.addCombo(this, "Points to Plot:", "left: 290px; top: 80px;", "100px", FailureType.values());

        addSubmitButton();

        UIUtil.addLayout(this, config.getChartId(), "left: 10px; top: 150px;", "720px", "450px");

        detailsTable = new DetailsTable();
        addComponent(detailsTable, "left: 750px; top: 150px;");
    }

    protected void addSubControls(int startTop) {
        noteLayout = new NoteLayout();
        addComponent(noteLayout, String.format("left: 750px; top: %spx;", startTop));
    }

    protected void addSubmitButton() {

        Button button = UIUtil.addButton(this, "Submit", "left: 420px; top: 80px;", "100px");

        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                loadChart();
            }
        });
    }

    protected void addNextChartButton() {

        nextChartButton = UIUtil.addButton(this, "", "left: 750px; top: 120px;", "250px");
        nextChartButton.setStyleName(Reindeer.BUTTON_LINK);

        nextChartButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                config.getLayoutContext().show(config.getNextLayout(), getParams());
            }
        });
    }

    protected Params getParams() {
        return new Params(
                params.getModuleId(),
                (String) testNameCombo.getValue(),
                params.getCommitId(),
                params.getRunNumber(),
                (Metric) metricCombo.getValue(),
                Integer.parseInt((String) percentileCombo.getValue()),
                (FailureType) failureCombo.getValue()
        );
    }

    protected void populateTestNames() {
        Set<String> testNames = dataService.getTestNames(params.getModuleId());
        UIUtil.populateCombo(testNameCombo, testNames.toArray(new String[]{}));
    }

    private void populateControls() {
        populateTestNames();
        UIUtil.select(testNameCombo, params.getTestName());
        UIUtil.select(metricCombo, params.getMetric());
        UIUtil.select(failureCombo, params.getFailureType());
    }

    public void show(Params params) {
        this.params = params;
        populateControls();
        loadChart();
        handleBreadcrumb();
    }

    protected void pointClicked(JSONObject json) throws JSONException {
        params.setCommitId(json.optString("commitId"));
        params.setRunNumber(json.optInt("runNumber", 0));

        detailsTable.setContent(json);
        noteLayout.load(params.getCommitId(), params.getRunNumber());
    }

    public void loadChart() {

        // BUG: If common.js is loaded separately its content is not visible later
        String chartContent = FileUtil.getContent("js/common.js") + FileUtil.getContent(config.getChartFile());
        Chart chart = config.getChartBuilder().getChart(getParams());

        chartContent = chartContent.replace("$categories", Format.formatCategories(chart.getCategories()));
        chartContent = chartContent.replace("$points", Format.formatPoints(chart.getSeries()));
        chartContent = chartContent.replace("$data", Format.formatData(chart.getSeries()));

        JavaScriptUtil.loadChart(chartContent, "chartCallback", this);
    }
}
