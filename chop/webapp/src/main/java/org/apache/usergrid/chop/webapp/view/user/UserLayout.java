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

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;
import org.junit.Assert;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.chop.api.ProviderParams;
import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.dao.ProviderParamsDao;
import org.apache.usergrid.chop.webapp.dao.UserDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicProviderParams;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.shiro.ShiroRealm;
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

    private final Label formTitle = new Label( "<b>User Information</b>", ContentMode.HTML );
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

    UserLayout( String username, TabSheetManager tabSheetManager, boolean hasAuthority ){
        this.username = username;
        this.tabSheetManager = tabSheetManager;
        addItems( hasAuthority );
        loadData( username, hasAuthority );
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

    private void loadData( String username, boolean hasAuthority ) {

        if ( StringUtils.isEmpty( username ) ) {
            deleteButton.setVisible( false );
            keyListLayout.setVisible( false );
            return;
        }


        User user = userDao.get( username );
        ProviderParams providerParams = providerParamsDao.getByUser( username );

        usernameField.setValue( user.getUsername( ) );
        passwordField.setValue( user.getPassword( ) );

        // if user does not have authority, do not allow credential information to be viewed.
        if ( ! hasAuthority ){
            disableCredentialInformationView( );
        }
        else {
            keyListLayout.loadKeys( username );
            accessKeyField.setValue( providerParams.getAccessKey( ) );
            imageField.setValue( providerParams.getImageId() );
            instanceTypeField.setValue( providerParams.getInstanceType() );
            secretKeyField.setValue( providerParams.getSecretKey() );
            keyPairNameField.setValue( providerParams.getKeyName() );
        }
    }


    private void addItems() {

        FormLayout formLayout = addFormLayout( 300, 350);
        formLayout.addComponent( formTitle );
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

    private void addItems( boolean hasAuthority ) {
        if ( ! hasAuthority ){
            FormLayout formLayout = addFormLayout( 300, 150 );
            formLayout.addComponent( formTitle );
            formLayout.addComponent( usernameField );
            formLayout.addComponent( passwordField );
            formLayout.addComponent( addButtonLayout() );
        }
        else {
            FormLayout formLayout = addFormLayout( 300, 350 );
            formLayout.addComponent( formTitle );
            formLayout.addComponent( usernameField );
            formLayout.addComponent( passwordField );
            formLayout.addComponent( accessKeyField );
            formLayout.addComponent( imageField );
            formLayout.addComponent( instanceTypeField );
            formLayout.addComponent( secretKeyField );
            formLayout.addComponent( keyPairNameField );
            formLayout.addComponent( addButtonLayout() );
        }
        addComponent( keyListLayout, "left: 650px; top: 50px;" );
    }

    private FormLayout addFormLayout( int x, int y ) {

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth( String.format( "%spx", x ) );
        formLayout.setHeight( String.format( "%spx", y ) );
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
        // Check if the selected user is the default user and it is tried to be deleted
        if ( UserListWindow.getSelectedUser().equals( ShiroRealm.getDefaultUser() ) ) {
            Notification.show( "Error", "Default admin user cannot be deleted", Notification.Type.ERROR_MESSAGE );
            return;
        }
        if ( ! ShiroRealm.isAuthenticatedUserAdmin() ) {
            Notification.show( "Error", "Only an admin can delete a user", Notification.Type.ERROR_MESSAGE );
            return;
        }
        userDao.delete( username );
        close();
        Notification.show( "Success", "User deleted successfully", Notification.Type.HUMANIZED_MESSAGE );
    }


    private void saveButtonClicked() {

        String username = usernameField.getValue();
        String password = passwordField.getValue();

        // Check if the selected user is the default user and it's username is tried to be changed
        if ( UserListWindow.getSelectedUser() != null &&
                UserListWindow.getSelectedUser().equals( ShiroRealm.getDefaultUser() ) &&
                isUserNameChanged( username ) ) {
            Notification.show( "Error", "Username of the default user cannot be changed", Notification.Type.ERROR_MESSAGE );
            return;
        }

        if ( StringUtils.isEmpty( username ) || StringUtils.isEmpty( password ) ) {
            Notification.show( "Error", "Please enter username and password", Notification.Type.ERROR_MESSAGE );
            return;
        }

        try {
            // Update the information of an existing user
            if ( UserListWindow.getSelectedUser() != null ){
                userDao.delete( UserListWindow.getSelectedUser() );
                userDao.save( new User( username, password ) );

                UserListWindow.setSelectedUser( username );
                ShiroRealm.setAuthenticatedUser( username );

                BasicProviderParams newProviderParams = new BasicProviderParams(
                        username,
                        instanceTypeField.getValue(),
                        accessKeyField.getValue(),
                        secretKeyField.getValue(),
                        imageField.getValue(),
                        keyPairNameField.getValue()
                );

                ProviderParams oldProviderParams = providerParamsDao.getByUser( UserListWindow.getSelectedUser() );

                Map<String, String> keys = oldProviderParams != null ? oldProviderParams.getKeys() : new HashMap<String, String>();
                newProviderParams.setKeys( keys );

                providerParamsDao.delete( UserListWindow.getSelectedUser() );
                providerParamsDao.save( newProviderParams );

                close();
                Notification.show( "Success", "User information updated successfully", Notification.Type.HUMANIZED_MESSAGE );

            }
            // Create a new user
            else{
                // Check if the new user exists in the system
                if ( userDao.get( username ) != null ) {
                    Notification.show( "Error", "The username " + username +" already exists!", Notification.Type.ERROR_MESSAGE );
                    return;
                }
                doSaveUser( username, password );
            }
        } catch ( Exception e ) {
            Notification.show( "Error", "Error to save user: " + e.getMessage(), Notification.Type.ERROR_MESSAGE );
        }
    }

    public void disableCredentialInformationView(){
        usernameField.setEnabled( false );
        passwordField.setEnabled( false );
        saveButton.setEnabled( false );
        keyListLayout.setEnabled( false );
        keyListLayout.disableKeyLabels();

        keyPairNameField.setVisible( false );
        accessKeyField.setVisible( false );
        imageField.setVisible( false );
        instanceTypeField.setVisible( false );
        secretKeyField.setVisible( false );
    }

    private boolean isUserNameChanged( final String username ) {
        if ( UserListWindow.getSelectedUser() == null ) {
            return false;
        }
        return ! username.equals( UserListWindow.getSelectedUser() );
    }


    private void doSaveUser( String username, String password ) throws IOException {

        Assert.assertTrue(  userDao.get( username ) == null  );

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
