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

import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.themes.Reindeer;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.webapp.dao.RunResultDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.builder.ChartBuilder;
import org.apache.usergrid.chop.webapp.service.util.JsonUtil;
import org.apache.usergrid.chop.webapp.view.main.Breadcrumb;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IterationsChartLayout extends ChartLayout {

    private RunResultDao runResultDao = InjectorFactory.getInstance(RunResultDao.class);

    private String runResultId;

    protected Button failuresButton;

    public IterationsChartLayout(ChartLayoutContext layoutContext, ChartBuilder chartBuilder, ChartLayout nextLayout, Breadcrumb breadcrumb) {
        super(new Config(
                layoutContext,
                chartBuilder,
                nextLayout,
                "iterationsChart",
                "js/iterations-chart.js",
                breadcrumb
        ));
    }

    @Override
    protected void addControls() {
        addMainControls();
        addSubControls(410);
        super.addSubControls(430);
        addRunnersCheckboxes();
    }

    private void addRunnersCheckboxes() {
        UIUtil.addLayout(this, "runnersCheckboxes", "left: 10px; top: 620px;", "720px", "200px");
    }

    @Override
    protected void addSubControls(int startTop) {

        String position = String.format("left: 750px; top: %spx;", startTop);

        failuresButton = UIUtil.addButton(this, "Show failures", position, "250px");
        failuresButton.setStyleName(Reindeer.BUTTON_LINK);

        failuresButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                showFailures();
            }
        });
    }

    private void setControlsReadOnly(boolean readOnly) {
        testNameCombo.setReadOnly(readOnly);
        metricCombo.setReadOnly(readOnly);
    }

    private void doShow(Params params) {
        setControlsReadOnly(false);
        super.show(params);
        setControlsReadOnly(true);
    }

    public void show(Params params) {
        doShow(params);
        noteLayout.load(params.getCommitId(), params.getRunNumber());
        failuresButton.setVisible(false);
    }

    @Override
    protected void pointClicked(JSONObject json) throws JSONException {
        detailsTable.setContent(json);
        handlePointClick(json);
    }

    private void handlePointClick(JSONObject json) {

        runResultId = json.optString("id");
        int failures = json.optInt("failures");

        boolean buttonVisible = !StringUtils.isEmpty(runResultId) && failures > 0;
        failuresButton.setVisible(buttonVisible);
    }

    private void showFailures() {

        String failures = runResultDao.getFailures(runResultId);
        JSONArray arr = JsonUtil.parseArray(failures);
        String messages = firstMessages(arr);

        Notification.show("Failures", messages, Notification.Type.TRAY_NOTIFICATION);
    }

    private String firstMessages(JSONArray arr) {

        String s = "";
        int len = Math.min(5, arr.length());

        for (int i = 0; i < len; i++) {
            JSONObject json = JsonUtil.get(arr, i);

            s += "* " + StringUtils.abbreviate(json.optString("message"), 200) + "\n"
                    + StringUtils.abbreviate(json.optString("trace"), 500) + "\n\n";
        }

        return s;
    }

    @Override
    protected void handleBreadcrumb() {
        String caption = "Run: " + params.getRunNumber();
        config.getBreadcrumb().setItem(this, caption, 2);
    }
}
