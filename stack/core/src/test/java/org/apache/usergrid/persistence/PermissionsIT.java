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
package org.apache.usergrid.persistence;


import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;



public class PermissionsIT extends AbstractCoreIT {

    private static final Logger logger = LoggerFactory.getLogger( PermissionsIT.class );


    public PermissionsIT() {
        super();
    }


    @Test
    public void testPermissionTimeout() throws Exception {
        UUID applicationId = setup.createApplication( "permissionsTest", "testPermissionTimeout" + UUIDGenerator.newTimeUUID()  );

        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        String name1 = "rolename1";
        String title1 = "roletitle1";
        long inactivity1 = 10000;

        String name2 = "rolename2";
        String title2 = "roletitle2";
        long inactivity2 = 20000;

        em.createRole( name1, title1, inactivity1 );
        em.createRole( name2, title2, inactivity2 );

        String fakeRole = "fakerole";

        Set<String> names = new HashSet<String>();
        names.add( name1 );
        names.add( name2 );
        names.add( fakeRole );

        Map<String, Role> results = em.getRolesWithTitles( names );

        Role existing = results.get( name1 );

        assertNotNull( existing );
        assertEquals( name1, existing.getName() );
        assertEquals( title1, existing.getTitle() );
        assertEquals( inactivity1, existing.getInactivity().longValue() );

        existing = results.get( name2 );

        assertNotNull( existing );
        assertEquals( name2, existing.getName() );
        assertEquals( title2, existing.getTitle() );
        assertEquals( inactivity2, existing.getInactivity().longValue() );

        existing = results.get( fakeRole );

        assertNull( existing );
    }


    @Test
    public void testPermissions() throws Exception {
        logger.info( "PermissionsIT.testPermissions" );

        UUID applicationId = setup.createApplication( "testOrganization"+ UUIDGenerator.newTimeUUID(), "testPermissions" + UUIDGenerator
            .newTimeUUID()  );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        assertNotNull( em );

        // em.createRole("admin", null);
        em.createRole( "manager", null, 0 );
        em.createRole( "member", null, 100000 );

        Map<String, String> roles = em.getRoles();
        assertEquals( "proper number of roles not set", 5, roles.size() );
        dump( "roles", roles );

        em.deleteRole( "member" );

        roles = em.getRoles();
        assertEquals( "proper number of roles not set", 4, roles.size() );
        dump( "roles", roles );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "path", "mmmeow" );

        Entity group = em.create( "group", properties );
        assertNotNull( user );

        em.addToCollection( group, "users", user );

        em.createGroupRole( group.getUuid(), "admin", 0 );
        em.createGroupRole( group.getUuid(), "author", 100000 );

        roles = em.getGroupRoles( group.getUuid() );
        assertEquals( "proper number of group roles not set", 2, roles.size() );
        dump( "group roles", roles );

        em.deleteGroupRole( group.getUuid(), "author" );

        roles = em.getGroupRoles( group.getUuid() );
        assertEquals( "proper number of group roles not set", 1, roles.size() );
        dump( "group roles", roles );

        app.refreshIndex();
        em.addUserToGroupRole( user.getUuid(), group.getUuid(), "admin" );

        app.refreshIndex();
        Results r = em.getUsersInGroupRole( group.getUuid(), "admin", Level.ALL_PROPERTIES );
        assertEquals( "proper number of users in group role not set", 1, r.size() );
        dump( "entities", r.getEntities() );

        em.grantRolePermission( "admin", "users:access:*" );
        em.grantRolePermission( "admin", "groups:access:*" );

        Set<String> permissions = em.getRolePermissions( "admin" );
        assertEquals( "proper number of role permissions not set", 2, permissions.size() );
        dump( "permissions", permissions );

        em.revokeRolePermission( "admin", "groups:access:*" );

        permissions = em.getRolePermissions( "admin" );
        assertEquals( "proper number of role permissions not set", 1, permissions.size() );
        dump( "permissions", permissions );

        em.grantGroupRolePermission( group.getUuid(), "admin", "users:access:*" );
        em.grantGroupRolePermission( group.getUuid(), "admin", "groups:access:*" );

        permissions = em.getGroupRolePermissions( group.getUuid(), "admin" );
        assertEquals( "proper number of group role permissions not set", 2, permissions.size() );
        dump( "group permissions", permissions );

        em.revokeGroupRolePermission( group.getUuid(), "admin", "groups:access:*" );

        permissions = em.getGroupRolePermissions( group.getUuid(), "admin" );
        assertEquals( "proper number of group role permissions not set", 1, permissions.size() );
        dump( "group permissions", permissions );

        roles = em.getRoles();
        assertEquals( "proper number of roles not set", 4, roles.size() );
        dump( "roles", roles );

        em.grantUserPermission( user.getUuid(), "users:access:*" );
        em.grantUserPermission( user.getUuid(), "groups:access:*" );

        permissions = em.getUserPermissions( user.getUuid() );
        assertEquals( "proper number of user permissions not set", 2, permissions.size() );
        dump( "user permissions", permissions );
    }
}
