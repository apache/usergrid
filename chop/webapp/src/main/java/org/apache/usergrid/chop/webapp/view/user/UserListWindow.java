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
package org.apache.usergrid.chop.webapp.view.user;


import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.dao.UserDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.view.main.TabSheetManager;
import org.apache.usergrid.chop.webapp.view.util.PopupWindow;

import com.vaadin.data.Property;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.ListSelect;


public class UserListWindow extends PopupWindow {

    private final TabSheetManager tabSheetManager;

    private static boolean createButtonClicked = false;
    private static String currentUser = "user";

    public UserListWindow(TabSheetManager tabSheetManager) {
        super( "Users" );
        this.tabSheetManager = tabSheetManager;
    }


    @Override
    protected void addItems( AbsoluteLayout mainLayout ) {
        addList( mainLayout );
        addCreateButton( mainLayout );
    }

    private void addList( AbsoluteLayout mainLayout ) {

        ListSelect list = new ListSelect();
        list.setWidth( "100%" );
        list.setHeight( "420px" );
        list.setNullSelectionAllowed( false );
        list.setImmediate( true );

        list.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Object value = event.getProperty().getValue();
                if (value != null) {
                    close();
                    currentUser = (String) value;
                    showUser( (String) value );
                }
            }
        });

        loadData( list );

        mainLayout.addComponent( list, "left: 0px; top: 0px;" );
    }

    private void showUser( String username ) {
        tabSheetManager.addTab( new UserLayout( username, tabSheetManager ), "User" );
    }

    private void loadData( ListSelect list ) {

        UserDao userDao = InjectorFactory.getInstance( UserDao.class );

        for ( User user : userDao.getList() ) {
            list.addItem( user.getUsername() );
        }
    }


    private void addCreateButton(AbsoluteLayout mainLayout) {

        Button createButton = new Button( "Create" );

        createButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                close();
                setCreateButtonClicked( true );
                showUser( null );
            }
        } );

        mainLayout.addComponent( createButton, "left: 10px; top: 425px;" );
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(String currentUser) {
        UserListWindow.currentUser = currentUser;
    }

    public static boolean isCreateButtonClicked() {
        return createButtonClicked;
    }

    public static void setCreateButtonClicked(boolean createButtonClicked) {
        UserListWindow.createButtonClicked = createButtonClicked;
    }
}
