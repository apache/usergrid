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


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Table;
import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.api.StatsSnapshot;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.dao.RunnerDao;
import org.apache.usergrid.chop.webapp.dao.model.RunnerGroup;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.runner.RunnerService;
import org.apache.usergrid.chop.webapp.service.runner.RunnerServiceImpl;


public class RunnersLayout extends VerticalLayout {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final RunnerDao runnerDao = InjectorFactory.getInstance( RunnerDao.class );
    private final ModuleDao moduleDao = InjectorFactory.getInstance( ModuleDao.class );

    // Use RunnerServiceMock for testing
    private final RunnerService runnerService = InjectorFactory.getInstance( RunnerServiceImpl.class );

    private final Accordion accordion = new Accordion();

    public RunnersLayout() {
        addItems();
        loadData();
    }


    private void addItems() {
        addAccordion();
        addRefreshButton();
    }


    private void addRefreshButton()  {

        Button button = new Button( "Refresh" );
        button.setWidth( "100px" );

        button.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                loadData();
            }
        });

        addComponent( button );
        this.setComponentAlignment( button, Alignment.BOTTOM_CENTER );
    }


    private void addAccordion() {
        accordion.setWidth( "800px" );
        accordion.setHeight( "530px" );
        addComponent( accordion );
        this.setComponentAlignment( accordion, Alignment.MIDDLE_CENTER );
    }


    private void loadData() {

        accordion.removeAllComponents();
        Map<RunnerGroup, List<Runner>> runnerGroups = runnerDao.getRunnersGrouped();

        for ( RunnerGroup group : runnerGroups.keySet() ) {
            Table table = getRunnersTable( runnerGroups.get( group ) );
            addGroup( group, table );
        }
    }


    private Table getRunnersTable( List<Runner> runners ) {

        Table table = getTable();

        for ( Runner runner : runners ) {
            addRunnerToTable( table, runner );
        }

        return table;
    }


    private void addRunnerToTable( Table table, Runner runner ) {

        State state = runnerService.getState(runner);
        StatsSnapshot stats = state == State.RUNNING ? runnerService.getStats(runner) : null;

        String percentageComplete = stats != null ? stats.getPercentageComplete() + "%" : "";
        String startTime = stats != null ? DATE_FORMAT.format( new Date( stats.getStartTime() ) ) : "";

        Object[] cells = new Object[] { runner.getUrl(), state.toString(), percentageComplete, startTime };

        table.addItem( cells, runner.getUrl() );
    }


    private void addGroup( RunnerGroup group, Table runnersTable ) {

        Module module = moduleDao.get( group.getModuleId() );

        if (module == null) {
            return;
        }

        String caption = String.format( "%s / %s / %s: commit[%s], user[%s]",
                module.getGroupId(),
                module.getArtifactId(),
                module.getVersion(),
                StringUtils.abbreviate( group.getCommitId(), 10 ),
                group.getUser()
        );

        accordion.addTab( runnersTable, caption );
    }


    private static Table getTable() {

        Table table = new Table();
        table.setSizeFull();

        table.addContainerProperty( "URL", String.class, null );
        table.addContainerProperty( "State", String.class, null );
        table.addContainerProperty( "Complete %", String.class, null );
        table.addContainerProperty( "Start Time", String.class, null );

        return table;
    }

}
