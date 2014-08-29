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

import com.vaadin.server.VaadinService;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.TabSheet;
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.service.shiro.ShiroRealm;
import org.apache.usergrid.chop.webapp.view.module.ModuleLayout;
import org.apache.usergrid.chop.webapp.view.chart.layout.OverviewChartLayout;
import org.apache.usergrid.chop.webapp.view.log.LogLayout;
import org.apache.usergrid.chop.webapp.view.module.ModuleSelectListener;
import org.apache.usergrid.chop.webapp.view.runner.RunnersLayout;
import org.apache.usergrid.chop.webapp.view.user.UserListWindow;


public class MainView extends VerticalLayout implements ModuleSelectListener {

    private TabSheetManager tabSheetManager;
    VerticalLayout tabSheet;
    HorizontalLayout buttons;


    MainView( ) {
        this.setHeight( "100%" );

        VerticalLayout verticalLayoutForButtons = new VerticalLayout();
        verticalLayoutForButtons.setSizeFull();

        buttons = addButtons();
        this.addComponent( buttons );
        setComponentAlignment( buttons , Alignment.TOP_CENTER );

        tabSheet = addTabSheet();
        tabSheet.setSizeFull();
        this.addComponent( tabSheet );
        this.setComponentAlignment( tabSheet, Alignment.TOP_CENTER );

        this.setExpandRatio( buttons, 0.04f );
        this.setExpandRatio( tabSheet, 0.96f );
    }

    private HorizontalLayout addButtons() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();

        /**      Modules Button    */
        Button modules = new Button( "Modules" );
        horizontalLayout.addComponent( modules );
        modules.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                tabSheetManager.addTabWithVerticalLayout( new ModuleLayout( MainView.this ), "Modules" );
            }
        });


        /**      Runners Button    */
        Button runners = new Button( "Runners" );
        horizontalLayout.addComponent( runners );
        runners.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                tabSheetManager.addTabWithVerticalLayout( new RunnersLayout(), "Runners" );
            }
        });

        /**      Users Button    */
        Button users = new Button( "Users" );
        horizontalLayout.addComponent( users );
        users.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent ) {
                UI.getCurrent().addWindow( new UserListWindow( tabSheetManager ) );
            }
        });

        /**      Logs Button    */
        Button logs = new Button( "Logs" );
        horizontalLayout.addComponent( logs );
        logs.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent ) {
                tabSheetManager.addTabWithVerticalLayout( new LogLayout(), "Logs" );
            }
        });

        /**      Logout Button    */
        Button logout = new Button( "Logout" );
        horizontalLayout.addComponent( logout );
        logout.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent ) {
                ShiroRealm.logout();
                redirectToMainView();
            }
        });
        float weight = logout.getHeight();
        horizontalLayout.setHeight( String.valueOf( weight ) );
        return horizontalLayout;
    }

    private VerticalLayout addTabSheet() {
        VerticalLayout tabLayout = new VerticalLayout();
        TabSheet tabSheet = new TabSheet();
        tabSheet.setHeight( "100%" );

        tabSheetManager = new TabSheetManager( tabSheet );
        tabLayout.addComponent( tabSheet );

        return tabLayout;
    }

    @Override
    public void onModuleSelect( String moduleId ) {
        AbsoluteLayout layout = new OverviewChartLayout( new Params(moduleId), tabSheetManager );
        tabSheetManager.addTab( layout, "Overview Chart" );
    }

    private void redirectToMainView() {
        // Close the VaadinServiceSession
        getUI().getSession().close();

        // Invalidate underlying session instead if login info is stored there
        VaadinService.getCurrentRequest().getWrappedSession().invalidate();
        getUI().getPage().setLocation( "/VAADIN" );
    }
}