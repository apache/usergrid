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

import com.vaadin.data.Property;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.webapp.dao.RunDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.chart.builder.ChartBuilder;
import org.apache.usergrid.chop.webapp.view.main.Breadcrumb;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunsChartLayout extends ChartLayout {

    private RunDao runDao = InjectorFactory.getInstance(RunDao.class);

    private Map<String, Run> runners = new HashMap<String, Run>();
    private ListSelect runnersListSelect;

    public RunsChartLayout(ChartLayoutContext layoutContext, ChartBuilder chartBuilder, ChartLayout nextLayout, Breadcrumb breadcrumb) {
        super(new Config(
                layoutContext,
                chartBuilder,
                nextLayout,
                "runsChart",
                "js/runs-chart.js",
                breadcrumb
        ));

        addNextChartButton();
    }

    @Override
    protected void addControls() {
        addMainControls();
        addSubControls(430);
        super.addSubControls(600);
    }

    @Override
    protected void addSubControls(int startTop) {

        String position = String.format("left: 750px; top: %spx;", startTop);
        runnersListSelect = UIUtil.addListSelect(this, "Runners:", position, "250px");

        runnersListSelect.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Object value = event.getProperty().getValue();
                if (value != null) {
                    showRunner(value.toString());
                }
            }
        });
    }

    private void showRunner(String runner) {

        Run run = runners.get(runner);

        String text = "- minTime: " + run.getMinTime()
                + "\n- maxTime: " + run.getMaxTime()
                + "\n- avgTime: " + run.getAvgTime()
                + "\n- actualTime: " + run.getActualTime()
                + "\n- iterations: " + run.getIterations()
                + "\n- failures: " + run.getFailures()
                + "\n- ignores: " + run.getIgnores()
                + "\n- threads: " + run.getThreads()
                + "\n- totalTestsRun: " + run.getTotalTestsRun();

        Notification.show(runner, text, Notification.Type.TRAY_NOTIFICATION);
    }

    @Override
    protected void pointClicked(JSONObject json) throws JSONException {
        super.pointClicked(json);
        handleRunNumber();
    }

    private void handleRunNumber() {
        nextChartButton.setCaption("Run: " + params.getRunNumber());
        showRunners();
    }

    private void showRunners() {

        runnersListSelect.removeAllItems();
        runners.clear();

        List<Run> runs = runDao.getList(params.getCommitId(), params.getRunNumber());

        for (Run run : runs) {
            runnersListSelect.addItem(run.getRunner());
            runners.put(run.getRunner(), run);
        }
    }

    @Override
    protected void handleBreadcrumb() {
        String caption = "Commit: " + StringUtils.abbreviate(params.getCommitId(), 10);
        config.getBreadcrumb().setItem(this, caption, 1);
    }
}
