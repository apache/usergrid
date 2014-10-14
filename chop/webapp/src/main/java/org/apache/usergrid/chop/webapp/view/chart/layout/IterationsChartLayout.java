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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.chop.webapp.dao.RunDao;
import org.apache.usergrid.chop.webapp.dao.RunResultDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.builder.IterationsChartBuilder;
import org.apache.usergrid.chop.webapp.service.util.JsonUtil;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;

import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.themes.Reindeer;


public class IterationsChartLayout extends ChartLayout {

    private RunResultDao runResultDao = InjectorFactory.getInstance(RunResultDao.class);

    private String runResultId;
    protected Button failuresButton;
    protected Params params;


    public IterationsChartLayout( Params params ) {
        super( InjectorFactory.getInstance( IterationsChartBuilder.class ), "iterationsChart", "js/iterations-chart.js",
                params );
    }


    @Override
    protected void addItems() {
        super.addItems();
        addRunnersCheckboxes();
        addFailuresButton();
    }


    protected void addFailuresButton() {

        failuresButton = UIUtil.addButton(this, "Show failures", "left: 1000px; top: 370px;", "180px");
        failuresButton.setStyleName( Reindeer.BUTTON_LINK);
        failuresButton.setVisible( false );

        failuresButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                showFailures();
            }
        });
    }


    private void showFailures() {

        String failures = runResultDao.getFailures(runResultId);
        JSONArray arr = JsonUtil.parseArray( failures );
        String messages = firstMessages(arr);

        Notification.show("Failures", messages, Notification.Type.TRAY_NOTIFICATION);
    }


    private String firstMessages(JSONArray arr) {

        String s = "";
        int len = Math.min(5, arr.length());

        for (int i = 0; i < len; i++) {
            JSONObject json = JsonUtil.get(arr, i);

            s += "* " + StringUtils.abbreviate( json.optString( "message" ), 200 ) + "\n"
                    + StringUtils.abbreviate(json.optString("trace"), 500) + "\n\n";
        }

        return s;
    }


    private void addRunnersCheckboxes() {
        UIUtil.addLayout(this, "runnersCheckboxes", "left: 10px; top: 300px;", "250px", "300px");
    }


    @Override
    protected void pointClicked( JSONObject json ) throws JSONException {
        super.pointClicked( json );

        runResultId = json.optString("id");
        int failures = json.optInt("failures");

        boolean buttonVisible = !StringUtils.isEmpty(runResultId) && failures > 0;
        failuresButton.setVisible(buttonVisible);
    }

}
