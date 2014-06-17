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


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.chop.api.ProviderParams;
import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.dao.ProviderParamsDao;
import org.apache.usergrid.chop.webapp.dao.UserDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicProviderParams;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.view.main.TabSheetManager;

import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;


public class UserLayout extends AbsoluteLayout {

    private final UserDao userDao = InjectorFactory.getInstance( UserDao.class );
    private final ProviderParamsDao providerParamsDao = InjectorFactory.getInstance( ProviderParamsDao.class );

    private final TextField usernameField = new TextField( "Username:" );
    private final PasswordField passwordField = new PasswordField( "Password:" );
    private final TextField accessKeyField = new TextField( "Access Key:" );
    private final TextField imageField = new TextField( "Image ID:" );
    private final TextField instanceTypeField = new TextField( "Instance Type:" );
    private final TextField secretKeyField = new TextField( "Secret Key:" );
    private final TextField keyPairNameField = new TextField( "Key Pair Name:" );

    private final Button saveButton = new Button( "Save" );
    private final Button deleteButton = new Button( "Delete" );
    private final KeyListLayout keyListLayout = new KeyListLayout();

    private final TabSheetManager tabSheetManager;
    private final String username;

    UserLayout( String username, TabSheetManager tabSheetManager ) {
        this.username = username;
        this.tabSheetManager = tabSheetManager;

        addItems();
        loadData( username );
    }


    private void loadData( String username ) {

        if ( StringUtils.isEmpty( username ) ) {
            deleteButton.setVisible( false );
            keyListLayout.setVisible( false );
            return;
        }

        keyListLayout.loadKeys( username );

        User user = userDao.get( username );
        ProviderParams providerParams = providerParamsDao.getByUser( username );

        usernameField.setValue( user.getUsername() );
        passwordField.setValue( user.getPassword() );
        accessKeyField.setValue( providerParams.getAccessKey() );
        imageField.setValue( providerParams.getImageId() );
        instanceTypeField.setValue( providerParams.getInstanceType() );
        secretKeyField.setValue( providerParams.getSecretKey() );
        keyPairNameField.setValue( providerParams.getKeyName() );
    }


    private void addItems() {

        FormLayout formLayout = addFormLayout();
        formLayout.addComponent( usernameField );
        formLayout.addComponent( passwordField );
        formLayout.addComponent( accessKeyField );
        formLayout.addComponent( imageField );
        formLayout.addComponent( instanceTypeField );
        formLayout.addComponent( secretKeyField );
        formLayout.addComponent( keyPairNameField );
        formLayout.addComponent( addButtonLayout() );

        addComponent( keyListLayout, "left: 650px; top: 50px;" );
    }


    private FormLayout addFormLayout() {

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth( "300px" );
        formLayout.setHeight( "300px" );
        formLayout.addStyleName( "outlined" );
        formLayout.setSpacing( true );

        addComponent( formLayout, "left: 350px; top: 50px;" );

        return formLayout;
    }


    private AbsoluteLayout addButtonLayout() {

        AbsoluteLayout layout = new AbsoluteLayout();
        layout.setWidth( "100%" );
        layout.setHeight( "50px" );


        layout.addComponent( saveButton, "left: 0px; top: 20px;" );
        saveButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                saveButtonClicked();
            }
        } );

        layout.addComponent( deleteButton, "left: 70px; top: 20px;" );
        deleteButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                deleteButtonClicked();
            }
        } );

        return layout;
    }


    private void deleteButtonClicked() {
        userDao.delete( username );
        close();
        Notification.show( "Success", "User deleted successfully", Notification.Type.HUMANIZED_MESSAGE );
    }


    private void saveButtonClicked() {

        String username = usernameField.getValue();
        String password = passwordField.getValue();

        if ( StringUtils.isEmpty( username ) || StringUtils.isEmpty( password ) ) {
            Notification.show( "Error", "Please enter username and password", Notification.Type.ERROR_MESSAGE );
            return;
        }

        try {
            if ( UserListWindow.createClicked ){
                userDao.update( new User( username, password ) );

                BasicProviderParams newProviderParams = new BasicProviderParams(
                        username,
                        instanceTypeField.getValue(),
                        accessKeyField.getValue(),
                        secretKeyField.getValue(),
                        imageField.getValue(),
                        keyPairNameField.getValue()
                );

                ProviderParams oldProviderParams = providerParamsDao.getByUser( username );

                Map<String, String> keys = oldProviderParams != null ? oldProviderParams.getKeys() : new HashMap<String, String>();
                newProviderParams.setKeys( keys );

                providerParamsDao.update( newProviderParams );
                close();
                Notification.show( "Success", "User updated successfully", Notification.Type.HUMANIZED_MESSAGE );
                UserListWindow.createClicked = false;
            } else{
                doSaveUser( username, password );
            }
        } catch ( Exception e ) {
            Notification.show( "Error", "Error to save user: " + e.getMessage(), Notification.Type.ERROR_MESSAGE );
        }
    }

    private void doSaveUser( String username, String password ) throws IOException {

        userDao.save( new User( username, password ) );

        BasicProviderParams newProviderParams = new BasicProviderParams(
                username,
                instanceTypeField.getValue(),
                accessKeyField.getValue(),
                secretKeyField.getValue(),
                imageField.getValue(),
                keyPairNameField.getValue()
        );

        ProviderParams oldProviderParams = providerParamsDao.getByUser( username );

        Map<String, String> keys = oldProviderParams != null ? oldProviderParams.getKeys() : new HashMap<String, String>();
        newProviderParams.setKeys( keys );

        providerParamsDao.save( newProviderParams );
        close();
        Notification.show( "Success", "User saved successfully", Notification.Type.HUMANIZED_MESSAGE );
    }


    private void close() {
        tabSheetManager.removeAll();
    }

}
