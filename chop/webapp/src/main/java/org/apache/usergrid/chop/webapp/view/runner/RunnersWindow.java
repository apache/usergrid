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
package org.apache.usergrid.chop.webapp.view.runner;

import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Window;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.api.StatsSnapshot;
import org.apache.usergrid.chop.webapp.dao.RunnerDao;
import org.apache.usergrid.chop.webapp.dao.model.RunnerGroup;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.runner.RunnerService;
import org.apache.usergrid.chop.webapp.service.runner.RunnerServiceImpl;
import org.apache.usergrid.chop.webapp.service.runner.RunnerServiceMock;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class RunnersWindow extends Window {

    private final RunnerDao runnerDao = InjectorFactory.getInstance(RunnerDao.class);

    // Use RunnerServiceMock for testing
    private final RunnerService runnerService = InjectorFactory.getInstance(RunnerServiceMock.class);

    private TextArea textArea;

    public RunnersWindow() {
        super("Runners");

        init();
        initLayout();
    }

    private void init() {
        setHeight("100%");
        setWidth("100%");

        center();
        setClosable(true);
        setModal(true);
    }

    private void initLayout() {

        AbsoluteLayout container = new AbsoluteLayout();
        container.setWidth("1000px");
        container.setHeight("1000px");
        setContent(container);

        Button button = UIUtil.addButton(container, "Update", "left: 10px; top: 10px;", "120px");

        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                updateRunners();
            }
        });

        textArea = UIUtil.addTextArea(container, "Runners", "left: 10px; top: 100px;", "900px", "400px");
    }

    private void updateRunners() {

        Map<RunnerGroup, List<Runner>> runnerGroups = runnerDao.getRunnersGrouped();
        String s = "";

        for (RunnerGroup group : runnerGroups.keySet()) {
            s += String.format("\n\n* %s\n", group);

            for (Runner runner : runnerGroups.get(group)) {

                State state = runnerService.getState(runner);
                StatsSnapshot stats = runnerService.getStats(runner);

                s += String.format("%s / %s / %s\n", runner.getUrl(), state, stats);
            }
        }

        textArea.setValue(s);
    }
}
