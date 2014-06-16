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
import org.apache.usergrid.chop.webapp.service.chart.Params;
import org.apache.usergrid.chop.webapp.view.chart.layout.*;
import org.apache.usergrid.chop.webapp.view.log.LogLayout;
import org.apache.usergrid.chop.webapp.view.module.ModuleListWindow;
import org.apache.usergrid.chop.webapp.view.module.ModuleSelectListener;
import org.apache.usergrid.chop.webapp.view.runner.RunnersLayout;
import org.apache.usergrid.chop.webapp.view.user.UserListWindow;
import org.apache.usergrid.chop.webapp.view.util.JavaScriptUtil;

import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;


public class MainView extends AbsoluteLayout implements ModuleSelectListener {

    private TabSheetManager tabSheetManager;

    MainView( ) {
        // mainLayout = addMainLayout();
        addButtons( this );
        addTabSheet( this );

    }
//
//
//    @Override
//    protected void init( VaadinRequest request ) {
//
//        AbsoluteLayout mainLayout = addMainLayout();
//        addButtons( mainLayout );
//        addTabSheet( mainLayout );
//
//        loadScripts();
//    }

    private AbsoluteLayout addMainLayout() {

        AbsoluteLayout absoluteLayout = new AbsoluteLayout();
        absoluteLayout.setWidth( "1300px" );
        absoluteLayout.setHeight( "700px" );

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.addComponent( absoluteLayout );
        verticalLayout.setComponentAlignment( absoluteLayout, Alignment.MIDDLE_CENTER );
        this.addComponent( verticalLayout);
        //setContent( verticalLayout );

        return absoluteLayout;
    }


    private void addButtons( AbsoluteLayout mainLayout ) {

        addButton( mainLayout, 350, "Modules", new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                UI.getCurrent().addWindow( new ModuleListWindow( MainView.this ) );
            }
        });

        addButton( mainLayout, 460, "Runners", new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                tabSheetManager.addTab(new RunnersLayout(), "Runners");
            }
        });

        addButton( mainLayout, 570, "Users", new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                UI.getCurrent().addWindow(new UserListWindow(tabSheetManager));
            }
        });

        addButton( mainLayout, 680, "Logs", new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                tabSheetManager.addTab(new LogLayout(), "Logs");
            }
        });

        addButton( mainLayout, 790, "Logout", new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                logout();
            }
        });
    }


    private static void addButton( AbsoluteLayout mainLayout, int left, String caption, Button.ClickListener listener ) {

        Button button = new Button( caption );
        button.setWidth( "100px" );
        button.addClickListener( listener );

        mainLayout.addComponent( button, String.format( "left: %spx; top: 0px;", left ) );
    }


    private void addTabSheet( AbsoluteLayout mainLayout ) {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setHeight( "650px" );

        tabSheetManager = new TabSheetManager(tabSheet );
        mainLayout.addComponent( tabSheet, "left: 0px; top: 50px;" );
    }


    @Override
    public void onModuleSelect( String moduleId ) {
        AbsoluteLayout layout = new OverviewChartLayout( new Params(moduleId), tabSheetManager );
        tabSheetManager.addTab( layout, "Overview Chart" );
    }

    private void logout() {
        // Close the VaadinServiceSession
        getUI().getSession().close();

        // Invalidate underlying session instead if login info is stored there
        VaadinService.getCurrentRequest().getWrappedSession().invalidate();

        // Redirect to avoid keeping the removed UI open in the browser
        getUI().getPage().setLocation( "/logout" );
    }
}