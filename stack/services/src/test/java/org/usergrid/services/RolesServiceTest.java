/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.entities.Role;

/**
 * @author tnine
 * 
 */
public class RolesServiceTest extends AbstractServiceTest {

    /**
     * Happy path test
     * 
     * @throws Exception
     */
    @Test
    public void createNewRolePost() throws Exception {
        UUID applicationId = createApplication("testOrganization", "createNewRolePost");
        assertNotNull(applicationId);

        createAndTestRoles(applicationId, ServiceAction.POST, "manager", "Manager Title", 600000l);
        createAndTestPermission(applicationId, ServiceAction.POST, "manager", "access:/**");

    }

    /**
     * Happy path test
     * 
     * @throws Exception
     */
    @Test
    public void createNewRolePut() throws Exception {

        UUID applicationId = createApplication("testOrganization", "createNewRolePut");
        assertNotNull(applicationId);

        createAndTestRoles(applicationId, ServiceAction.PUT, "manager", "Manager Title", 600000l);
        createAndTestPermission(applicationId, ServiceAction.PUT, "manager", "access:/**");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noRoleName() throws Exception {

        UUID applicationId = createApplication("testOrganization", "noRoleName");
        assertNotNull(applicationId);

        ServiceManager sm = smf.getServiceManager(applicationId);
        assertNotNull(sm);

        EntityManager em = sm.getEntityManager();
        assertNotNull(em);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("title", "Manager Title");
        props.put("inactivity", 600000l);

        // test creating a new role
        testRequest(sm, ServiceAction.POST, 1, props, "roles");

    }

    @Test(expected = IllegalArgumentException.class)
    public void noPermissionsOnPost() throws Exception {

        UUID applicationId = createApplication("testOrganization", "noPermissionsOnPost");
        assertNotNull(applicationId);

        ServiceManager sm = smf.getServiceManager(applicationId);
        assertNotNull(sm);

        EntityManager em = sm.getEntityManager();
        assertNotNull(em);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("name", "manager");
        props.put("title", "Manager Title");
        props.put("inactivity", 600000l);

        // test creating a new role
        ServiceResults results = testRequest(sm, ServiceAction.POST, 1, props, "roles");

        // check the results
        Entity roleEntity = results.getEntities().get(0);

        assertEquals("manager", roleEntity.getProperty("name"));
        assertEquals("Manager Title", roleEntity.getProperty("title"));
        assertEquals(600000l, roleEntity.getProperty("inactivity"));

        props = new HashMap<String, Object>();
        props.put("misspelledpermission", "access:/**");

        // now grant permissions
        results = invokeService(sm, ServiceAction.POST, props, "roles", "manager", "permissions");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noPermissionsOnPut() throws Exception {

        UUID applicationId = createApplication("testOrganization", "noPermissionsOnPut");
        assertNotNull(applicationId);

        ServiceManager sm = smf.getServiceManager(applicationId);
        assertNotNull(sm);

        EntityManager em = sm.getEntityManager();
        assertNotNull(em);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("name", "manager");
        props.put("title", "Manager Title");
        props.put("inactivity", 600000l);

        // test creating a new role
        ServiceResults results = testRequest(sm, ServiceAction.POST, 1, props, "roles");

        // check the results
        Entity roleEntity = results.getEntities().get(0);

        assertEquals("manager", roleEntity.getProperty("name"));
        assertEquals("Manager Title", roleEntity.getProperty("title"));
        assertEquals(600000l, roleEntity.getProperty("inactivity"));

        props = new HashMap<String, Object>();
        props.put("misspelledpermission", "access:/**");

        // now grant permissions
        results = invokeService(sm, ServiceAction.PUT, props, "roles", "manager", "permissions");
    }

    /**
     * Test deleting all permissions
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void deletePermissions() throws Exception {

        UUID applicationId = createApplication("testOrganization", "deletePermissions");
        assertNotNull(applicationId);

        createAndTestRoles(applicationId, ServiceAction.PUT, "manager", "Manager Title", 600000l);
        createAndTestPermission(applicationId, ServiceAction.PUT, "manager", "access:/**");
        createAndTestPermission(applicationId, ServiceAction.PUT, "manager", "access:/places/**");
        createAndTestPermission(applicationId, ServiceAction.PUT, "manager", "access:/faces/names/**");

        // we know we created the role successfully, now delete it

        ServiceManager sm = smf.getServiceManager(applicationId);
        assertNotNull(sm);

        EntityManager em = sm.getEntityManager();
        assertNotNull(em);

        // check it appears in the application roles

        Query query = new Query();
        query.setPermissions(Collections.singletonList( "access:/places/**"));
        
        // now grant permissions
        ServiceResults results = invokeService(sm, ServiceAction.DELETE, null, "roles", "manager", "permissions", query);

        // check the results has the data element.
        Set<String> data = (Set<String>) results.getData();

        assertTrue(data.contains("access:/**"));
        assertTrue(data.contains("access:/faces/names/**"));
        assertFalse(data.contains("access:/places/**"));

        // check our permissions are there
        Set<String> permissions = em.getRolePermissions("manager");

        assertTrue(permissions.contains("access:/**"));
        assertTrue(data.contains("access:/faces/names/**"));
        assertFalse(data.contains("access:/places/**"));

        query = new Query();
        query.setPermissions(Collections.singletonList( "access:/faces/names/**"));
      
        
        results = invokeService(sm, ServiceAction.DELETE, null, "roles", "manager", "permissions", query);

        // check the results has the data element.
        data = (Set<String>) results.getData();

        assertTrue(data.contains("access:/**"));
        assertFalse(data.contains("access:/faces/names/**"));
        assertFalse(data.contains("access:/places/**"));

        // check our permissions are there
        permissions = em.getRolePermissions("manager");

        assertTrue(permissions.contains("access:/**"));
        assertFalse(data.contains("access:/faces/names/**"));
        assertFalse(data.contains("access:/places/**"));
        
        
        query = new Query();
        query.setPermissions(Collections.singletonList("access:/**"));
        
        results = invokeService(sm, ServiceAction.DELETE, null, "roles", "manager", "permissions", query);

        // check the results has the data element.
        data = (Set<String>) results.getData();

        assertFalse(data.contains("access:/**"));
        assertFalse(data.contains("access:/faces/names/**"));
        assertFalse(data.contains("access:/places/**"));

        // check our permissions are there
        permissions = em.getRolePermissions("manager");

        assertFalse(permissions.contains("access:/**"));
        assertFalse(data.contains("access:/faces/names/**"));
        assertFalse(data.contains("access:/places/**"));


    }
    
    /**
     * Test deleting all permissions
     * 
     * @throws Exception
     */
    @Test
    public void deleteRoles() throws Exception {

        UUID applicationId = createApplication("testOrganization", "deleteRoles");
        assertNotNull(applicationId);

        createAndTestRoles(applicationId, ServiceAction.PUT, "manager", "Manager Title", 600000l);
        createAndTestPermission(applicationId, ServiceAction.PUT, "manager", "access:/**");
        createAndTestPermission(applicationId, ServiceAction.PUT, "manager", "access:/places/**");
        createAndTestPermission(applicationId, ServiceAction.PUT, "manager", "access:/faces/names/**");

        // we know we created the role successfully, now delete it

        ServiceManager sm = smf.getServiceManager(applicationId);
        assertNotNull(sm);

        EntityManager em = sm.getEntityManager();
        assertNotNull(em);

        // check it appears in the application roles

        // now grant permissions
        ServiceResults results = invokeService(sm, ServiceAction.DELETE, null, "roles", "manager");

        assertEquals(1, results.size());
        // check the results has the data element.
        
        Role role = em.get(em.getAlias("role", "manager"), Role.class);
        assertNull(role);
     
        
        // check our permissions are there
        Set<String> permissions = em.getRolePermissions("manager");

        assertEquals(0, permissions.size());
        
       

    }

    /**
     * Create the role with the action and info and test it's created
     * successfully
     * 
     * @param applicationId
     * @param action
     * @throws Exception
     */
    private void createAndTestRoles(UUID applicationId, ServiceAction action, String roleName, String roleTitle,
            long inactivity) throws Exception {

        ServiceManager sm = smf.getServiceManager(applicationId);
        assertNotNull(sm);

        EntityManager em = sm.getEntityManager();
        assertNotNull(em);

        Map<String, Object> props = new HashMap<String, Object>();

        props.put("name", roleName);

        props.put("title", roleTitle);

        props.put("inactivity", inactivity);

        // test creating a new role
        ServiceResults results = testRequest(sm, action, 1, props, "roles");

        // check the results
        Entity roleEntity = results.getEntities().get(0);

        assertEquals(roleName, roleEntity.getProperty("name"));
        assertEquals(roleTitle, roleEntity.getProperty("title"));
        assertEquals(inactivity, roleEntity.getProperty("inactivity"));

        // check the role is correct at the application level
        Map<String, Role> roles = em.getRolesWithTitles(Collections.singleton(roleName));

        Role role = roles.get(roleName);

        assertNotNull(role);
        assertEquals(roleName, role.getName());
        assertEquals(roleTitle, role.getTitle());
        assertEquals(inactivity, role.getInactivity().longValue());
    }

    /**
     * Create the permission and text it exists correctly
     * 
     * @param applicationId
     * @param action
     * @param roleName
     * @param grant
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void createAndTestPermission(UUID applicationId, ServiceAction action, String roleName, String grant)
            throws Exception {

        ServiceManager sm = smf.getServiceManager(applicationId);
        assertNotNull(sm);

        EntityManager em = sm.getEntityManager();
        assertNotNull(em);

        // check it appears in the application roles

        Map<String, Object> props = new HashMap<String, Object>();

        props.put("permission", grant);

        // now grant permissions
        ServiceResults results = invokeService(sm, action, props, "roles", roleName, "permissions");

        // check the results has the data element.
        Set<String> data = (Set<String>) results.getData();

        assertTrue(data.contains(grant));

        // check our permissions are there
        Set<String> permissions = em.getRolePermissions(roleName);

        assertTrue(permissions.contains(grant));
        
        
        //perform a  GET and make sure it's present
        results = invokeService(sm, ServiceAction.GET, props, "roles", roleName, "permissions");

        // check the results has the data element.
        data = (Set<String>) results.getData();

        assertTrue(data.contains(grant));
        
    }

}
