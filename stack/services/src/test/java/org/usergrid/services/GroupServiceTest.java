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

import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;

public class GroupServiceTest extends AbstractServiceTest {

    private static final Logger logger = LoggerFactory
            .getLogger(GroupServiceTest.class);

    @Test
    public void testGroups() throws Exception {

        UUID applicationId = createApplication("testOrganization", "testGroups");

        ServiceManager sm = smf.getServiceManager(applicationId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("path", "test/test");
        properties.put("title", "Test group");

        Entity group = testRequest(sm, ServiceAction.POST, 1, properties,
                "groups").getEntity();
        assertNotNull(group);

        testRequest(sm, ServiceAction.GET, 1, null, "groups", "test", "test");

        testRequest(sm, ServiceAction.GET, 0, null, "groups", "test", "test",
                "messages");

        testRequest(sm, ServiceAction.GET, 1, null, "groups");

        properties = new LinkedHashMap<String, Object>();
        properties.put("username", "edanuff");
        properties.put("email", "ed@anuff.com");

        Entity user = testRequest(sm, ServiceAction.POST, 1, properties,
                "users").getEntity();
        assertNotNull(user);

        testRequest(sm, ServiceAction.GET, 0, null, "groups", "test", "test",
                "users");

        testRequest(sm, ServiceAction.POST, 1, null, "groups", "test", "test",
                "users", user.getUuid());

        testRequest(sm, ServiceAction.GET, 1, null, "groups", "test", "test",
                "users");

        testRequest(sm, ServiceAction.GET, 1, null, "users", user.getUuid(),
                "groups");

        testRequest(sm, ServiceAction.GET, 0, null, "users", user.getUuid(),
                "activities");

        testRequest(sm, ServiceAction.GET, 0, null, "groups", group.getUuid(),
                "users", user.getUuid(), "activities");

    }

    @Test
    public void testPermissions() throws Exception {
        logger.info("PermissionsTest.testPermissions");

        UUID applicationId = createApplication("testOrganization",
                "testPermissions");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("path", "mmmeow");

        Entity group = em.create("group", properties);
        assertNotNull(group);

        em.createGroupRole(group.getUuid(), "admin", 0);
        em.createGroupRole(group.getUuid(), "author", 0);

        em.grantGroupRolePermission(group.getUuid(), "admin", "users:access:*");
        em.grantGroupRolePermission(group.getUuid(), "admin", "groups:access:*");
        em.grantGroupRolePermission(group.getUuid(), "author",
                "assets:access:*");

        ServiceManager sm = smf.getServiceManager(applicationId);

        testDataRequest(sm, ServiceAction.GET, null, "groups", group.getUuid(),
                "rolenames");

        testDataRequest(sm, ServiceAction.GET, null, "groups", group.getUuid(),
                "roles", "admin", "permissions");

        testDataRequest(sm, ServiceAction.GET, null, "groups", group.getUuid(),
                "roles", "author", "permissions");

    }

}
