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
package org.apache.usergrid.chop.webapp.view.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.UI;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.chart.builder.IterationsChartBuilder;
import org.apache.usergrid.chop.webapp.service.chart.builder.OverviewChartBuilder;
import org.apache.usergrid.chop.webapp.service.chart.builder.RunsChartBuilder;
import org.apache.usergrid.chop.webapp.view.chart.layout.*;
import org.apache.usergrid.chop.webapp.view.tree.ModuleSelectListener;
import org.apache.usergrid.chop.webapp.view.tree.ModuleTreeBuilder;
import org.apache.usergrid.chop.webapp.view.util.JavaScriptUtil;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;

@Title("Judo Chop")
public class MainView extends UI implements ChartLayoutContext, ModuleSelectListener {

    private static Logger LOG = LoggerFactory.getLogger( MainView.class );

    private HorizontalSplitPanel splitPanel;
    private ChartLayout overviewLayout;

    private Breadcrumb breadcrumb = new Breadcrumb(this);
    private Header header = new Header();
    private AbsoluteLayout mainLayout;

    @Override
    protected void init(VaadinRequest request) {
        overviewLayout = initChartLayouts(this, breadcrumb);
        initLayout();
        loadScripts();
    }

    private static ChartLayout initChartLayouts(ChartLayoutContext layoutContext, Breadcrumb breadcrumb) {

        IterationsChartBuilder iterationsChartBuilder = InjectorFactory.getInstance(IterationsChartBuilder.class);
        RunsChartBuilder runsChartBuilder = InjectorFactory.getInstance(RunsChartBuilder.class);
        OverviewChartBuilder overviewChartBuilder = InjectorFactory.getInstance(OverviewChartBuilder.class);

        ChartLayout iterationsLayout = new IterationsChartLayout(layoutContext, iterationsChartBuilder, null, breadcrumb);
        ChartLayout runsLayout = new RunsChartLayout(layoutContext, runsChartBuilder, iterationsLayout, breadcrumb);
        return new OverviewChartLayout(layoutContext, overviewChartBuilder, runsLayout, breadcrumb);
    }

    private void initLayout() {

        splitPanel = new HorizontalSplitPanel();
        splitPanel.setSplitPosition(20);
        splitPanel.setFirstComponent(ModuleTreeBuilder.getTree(this));
        splitPanel.setSecondComponent(initMainContainer());

        setContent(splitPanel);
    }

    private AbsoluteLayout initMainContainer() {

        AbsoluteLayout container = new AbsoluteLayout();
        container.addComponent(header, "left: 0px; top: 0px;");
        container.addComponent(breadcrumb, "left: 0px; top: 30px;");

        mainLayout = UIUtil.addLayout(container, "", "left: 0px; top: 60px;", "1000px", "1000px");

        return container;
    }

    private void loadScripts() {
        JavaScriptUtil.loadFile("js/jquery.min.js");
        JavaScriptUtil.loadFile("js/jquery.flot.min.js");
    }

    @Override
    public void onModuleSelect( String moduleId ) {
        LOG.info( "Selected module: {}", moduleId );
        header.showModule( moduleId );
        show(overviewLayout, new Params(moduleId));
    }

    private void setChartLayout(ChartLayout chartLayout) {
        mainLayout.removeAllComponents();
        mainLayout.addComponent(chartLayout);
    }

    @Override
    public void show(ChartLayout chartLayout, Params params) {
        setChartLayout(chartLayout);
        chartLayout.show(params);
    }

    void show(ChartLayout chartLayout) {
        setChartLayout(chartLayout);
        chartLayout.loadChart();
    }
}
