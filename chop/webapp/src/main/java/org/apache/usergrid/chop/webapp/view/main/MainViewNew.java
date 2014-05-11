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


import java.util.List;

import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.view.chart.layout.ChartLayout;
import org.apache.usergrid.chop.webapp.view.chart.layout.ChartLayoutNew;
import org.apache.usergrid.chop.webapp.view.chart.layout.RunsChartLayoutNew;
import org.apache.usergrid.chop.webapp.view.tree.ModuleSelectListener;
import org.apache.usergrid.chop.webapp.view.user.UserListWindow;
import org.apache.usergrid.chop.webapp.view.util.JavaScriptUtil;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;

import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


@Title( "Judo Chop" )
public class MainViewNew extends UI {


    private TabSheet tabSheet;


    @Override
    protected void init( VaadinRequest request ) {

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        setContent( verticalLayout );

        AbsoluteLayout absoluteLayout = new AbsoluteLayout();
        absoluteLayout.setWidth( "1200px" );
        absoluteLayout.setHeight( "700px" );

        verticalLayout.addComponent( absoluteLayout );
        verticalLayout.setComponentAlignment( absoluteLayout, Alignment.MIDDLE_CENTER );

        tabSheet = new TabSheet();
        tabSheet.setHeight( "650px" );

        absoluteLayout.addComponent( tabSheet, "left: 0px; top: 50px;" );

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setWidth( "500px" );
        horizontalLayout.setHeight( "50px" );
        absoluteLayout.addComponent( horizontalLayout, "left: 250px; top: 0px;" );

        // -------------------------------------------------------------

        Button modulesButton = new Button( "Modules" );
        modulesButton.setWidth( "150px" );
        horizontalLayout.addComponent( modulesButton );
        horizontalLayout.setComponentAlignment( modulesButton, Alignment.MIDDLE_CENTER);

        modulesButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                showWindow();
            }
        } );

        // -------------------------------------------------------------

        Button runnersButton = new Button( "Runners" );
        runnersButton.setWidth( "150px" );
        horizontalLayout.addComponent( runnersButton );
        horizontalLayout.setComponentAlignment( runnersButton, Alignment.MIDDLE_CENTER);

        runnersButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                showTab();
            }
        } );


        // -------------------------------------------------------------

        Button usersButton = new Button( "Users" );
        usersButton.setWidth( "150px" );
        horizontalLayout.addComponent( usersButton );
        horizontalLayout.setComponentAlignment( usersButton, Alignment.MIDDLE_CENTER);

        usersButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                UI.getCurrent().addWindow( new UserListWindow() );
            }
        } );

        loadScripts();
    }


    private void loadScripts() {
        JavaScriptUtil.loadFile( "js/jquery.min.js" );
        JavaScriptUtil.loadFile( "js/jquery.flot.min.js" );
    }


    private void showTab() {
        try {
//            AbsoluteLayout layout = new ChartLayoutNew( null );
            AbsoluteLayout layout = new RunsChartLayoutNew( null );
            tabSheet.addTab( layout, "Chart Layout" );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }


    private static void showWindow() {

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();

        // --------------------------------------------

        TreeTable treeTable = getTree();
        treeTable.setWidth( "100%" );
        treeTable.setHeight( "420px" );
        verticalLayout.addComponent( treeTable );
        verticalLayout.setComponentAlignment( treeTable, Alignment.TOP_CENTER );


        // --------------------------------------------

        AbsoluteLayout absoluteLayout = new AbsoluteLayout();
        absoluteLayout.setHeight( "50px" );
        absoluteLayout.setWidth( "100%" );

//        absoluteLayout.addComponent( new Button( "Create" ), "left: 10px; top: 15px;" );
        absoluteLayout.addComponent( new Button( "Close" ), "left: 220px; top: 15px;" );

        verticalLayout.addComponent( absoluteLayout );
        verticalLayout.setComponentAlignment( absoluteLayout, Alignment.BOTTOM_CENTER );


        // --------------------------------------------

        Window window = new Window( "Modules" );
        window.setModal( true );
        window.setResizable( false );
        window.setWidth( "300px" );
        window.setHeight( "500px" );
        window.setContent( verticalLayout );

        UI.getCurrent().addWindow( window );
    }


    public static TreeTable getTree() {

        TreeTable treeTable = new TreeTable();
        treeTable.setSelectable( true );
        treeTable.addContainerProperty( "Group", String.class, "" );
        treeTable.addContainerProperty( "Artifact", String.class, "" );

        addItems(treeTable);

        return treeTable;
    }

    private static void addItems(TreeTable treeTable) {

        treeTable.addItem( new Object[] { "org.apache.usergrid.chop", "chop-runner" }, "item1" );
        treeTable.addItem( new Object[] { "1.0-SNAPSHOT", "" }, "item11" );
        treeTable.setParent( "item11", "item1" );

        treeTable.addItem( new Object[] { "org.apache.usergrid.chop", "chop-client" }, "item2" );
        treeTable.addItem( new Object[] { "org.apache.usergrid", "collection" }, "item3" );
        treeTable.addItem( new Object[] { "org.apache.usergrid.chop", "example" }, "item4" );
    }

}
