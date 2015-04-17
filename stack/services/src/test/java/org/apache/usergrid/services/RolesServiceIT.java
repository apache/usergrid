/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.services;


import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;


import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.apache.usergrid.persistence.index.query.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/** @author tnine */

public class RolesServiceIT extends AbstractServiceIT {

    /** Happy path test */
    @Test
    public void createNewRolePost() throws Exception {
        createAndTestRoles( ServiceAction.POST, "manager", "Manager Title", 600000l );
        createAndTestPermission( ServiceAction.POST, "manager", "access:/**" );
    }


    /** Happy path test */
    @Test
    public void createNewRolePut() throws Exception {

        createAndTestRoles( ServiceAction.PUT, "manager", "Manager Title", 600000l );
        createAndTestPermission( ServiceAction.PUT, "manager", "access:/**" );
    }


    @Test(expected =  RequiredPropertyNotFoundException.class)
    public void noRoleName() throws Exception {

        app.put( "title", "Manager Title" );
        app.put( "inactivity", 600000l );

        // test creating a new role
        app.testRequest( ServiceAction.POST, 1, "roles" );
    }


    @Test(expected = IllegalArgumentException.class)
    public void noPermissionsOnPost() throws Exception {
        app.put( "name", "manager" );
        app.put( "title", "Manager Title" );
        app.put( "inactivity", 600000l );

        // test creating a new role
        ServiceResults results = app.testRequest( ServiceAction.POST, 1, "roles" );

        // check the results
        Entity roleEntity = results.getEntities().get( 0 );

        assertEquals( "manager", roleEntity.getProperty( "name" ) );
        assertEquals( "Manager Title", roleEntity.getProperty( "title" ) );
        assertEquals( 600000l, roleEntity.getProperty( "inactivity" ) );

        app.put( "misspelledpermission", "access:/**" );
        app.invokeService( ServiceAction.POST, "roles", "manager", "permissions" );
    }


    @Test(expected = IllegalArgumentException.class)
    public void noPermissionsOnPut() throws Exception {
        app.put( "name", "manager" );
        app.put( "title", "Manager Title" );
        app.put( "inactivity", 600000l );

        // test creating a new role
        ServiceResults results = app.testRequest( ServiceAction.POST, 1, "roles" );

        // check the results
        Entity roleEntity = results.getEntities().get( 0 );

        assertEquals( "manager", roleEntity.getProperty( "name" ) );
        assertEquals( "Manager Title", roleEntity.getProperty( "title" ) );
        assertEquals( 600000l, roleEntity.getProperty( "inactivity" ) );

        app.put( "misspelledpermission", "access:/**" );

        // now grant permissions
        app.invokeService( ServiceAction.PUT, "roles", "manager", "permissions" );
    }


    /** Test deleting all permissions */
    @SuppressWarnings("unchecked")
    @Test
    public void deletePermissions() throws Exception {
        createAndTestRoles( ServiceAction.PUT, "manager", "Manager Title", 600000l );
        createAndTestPermission( ServiceAction.PUT, "manager", "access:/**" );
        createAndTestPermission( ServiceAction.PUT, "manager", "access:/places/**" );
        createAndTestPermission( ServiceAction.PUT, "manager", "access:/faces/names/**" );

        // we know we created the role successfully, now delete it
        // check it appears in the application roles

        Query query = new Query();
        query.setPermissions( Collections.singletonList( "access:/places/**" ) );

        // now grant permissions
        ServiceResults results = app.invokeService( ServiceAction.DELETE, "roles", "manager", "permissions", query );

        // check the results has the data element.
        Set<String> data = ( Set<String> ) results.getData();

        assertTrue( data.contains( "access:/**" ) );
        assertTrue( data.contains( "access:/faces/names/**" ) );
        assertFalse( data.contains( "access:/places/**" ) );

        // check our permissions are there
        Set<String> permissions = app.getRolePermissions( "manager" );

        assertTrue( permissions.contains( "access:/**" ) );
        assertTrue( data.contains( "access:/faces/names/**" ) );
        assertFalse( data.contains( "access:/places/**" ) );

        query = new Query();
        query.setPermissions( Collections.singletonList( "access:/faces/names/**" ) );


        results = app.invokeService( ServiceAction.DELETE, "roles", "manager", "permissions", query );

        // check the results has the data element.
        data = ( Set<String> ) results.getData();

        assertTrue( data.contains( "access:/**" ) );
        assertFalse( data.contains( "access:/faces/names/**" ) );
        assertFalse( data.contains( "access:/places/**" ) );

        // check our permissions are there
        permissions = app.getRolePermissions( "manager" );

        assertTrue( permissions.contains( "access:/**" ) );
        assertFalse( data.contains( "access:/faces/names/**" ) );
        assertFalse( data.contains( "access:/places/**" ) );


        query = new Query();
        query.setPermissions( Collections.singletonList( "access:/**" ) );

        results = app.invokeService( ServiceAction.DELETE, "roles", "manager", "permissions", query );

        // check the results has the data element.
        data = ( Set<String> ) results.getData();

        assertFalse( data.contains( "access:/**" ) );
        assertFalse( data.contains( "access:/faces/names/**" ) );
        assertFalse( data.contains( "access:/places/**" ) );

        // check our permissions are there
        permissions = app.getRolePermissions( "manager" );

        assertFalse( permissions.contains( "access:/**" ) );
        assertFalse( data.contains( "access:/faces/names/**" ) );
        assertFalse( data.contains( "access:/places/**" ) );
    }


    /** Test deleting all permissions */
    @Test
    public void deleteRoles() throws Exception {

        createAndTestRoles( ServiceAction.PUT, "manager", "Manager Title", 600000l );
        createAndTestPermission( ServiceAction.PUT, "manager", "access:/**" );
        createAndTestPermission( ServiceAction.PUT, "manager", "access:/places/**" );
        createAndTestPermission( ServiceAction.PUT, "manager", "access:/faces/names/**" );

        // we know we created the role successfully, now delete it
        ServiceResults results = app.invokeService( ServiceAction.DELETE, "roles", "manager" );
        assertEquals( 1, results.size() );

        setup.getEntityIndex().refresh();

        // check role is gone
        Role role = app.get( app.getAlias( "role", "manager" ), Role.class );
        assertNull( role );

        // check permissions are gone
        Set<String> permissions = app.getRolePermissions( "manager" );
        assertEquals( 0, permissions.size() );
    }


    /**
     * Create the role with the action and info and test it's created successfully
     *
     * @param action the action to take
     */
    private void createAndTestRoles( ServiceAction action, String roleName, String roleTitle, long inactivity )
            throws Exception {
        app.put( "name", roleName );

        app.put( "title", roleTitle );

        app.put( "inactivity", inactivity );

        // test creating a new role
        ServiceResults results = app.testRequest( action, 1, "roles" );

        // check the results
        Entity roleEntity = results.getEntities().get( 0 );

        assertEquals( roleName, roleEntity.getProperty( "name" ) );
        assertEquals( roleTitle, roleEntity.getProperty( "title" ) );
        assertEquals( inactivity, roleEntity.getProperty( "inactivity" ) );

        // check the role is correct at the application level
        Map<String, Role> roles = app.getRolesWithTitles( Collections.singleton( roleName ) );

        Role role = roles.get( roleName );

        assertNotNull( role );
        assertEquals( roleName, role.getName() );
        assertEquals( roleTitle, role.getTitle() );
        assertEquals( inactivity, role.getInactivity().longValue() );
    }


    /**
     * Create the permission and text it exists correctly
     *
     * @param action the action to take
     * @param roleName the name of the role
     * @param grant the permission to grant
     */
    @SuppressWarnings("unchecked")
    private void createAndTestPermission( ServiceAction action, String roleName, String grant ) throws Exception {
        app.put( "permission", grant );

        // now grant permissions
        ServiceResults results = app.invokeService( action, "roles", roleName, "permissions" );

        // check the results has the data element.
        Set<String> data = ( Set<String> ) results.getData();

        assertTrue( data.contains( grant ) );

        // check our permissions are there
        Set<String> permissions = app.getRolePermissions( roleName );

        assertTrue( permissions.contains( grant ) );


        //perform a  GET and make sure it's present
        results = app.invokeService( ServiceAction.GET, "roles", roleName, "permissions" );

        // check the results has the data element.
        data = ( Set<String> ) results.getData();

        assertTrue( data.contains( grant ) );
    }
}
