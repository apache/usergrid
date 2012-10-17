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

import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.EntityManager;

public class ApplicationsServiceTest extends AbstractServiceTest {

    private static final Logger logger = LoggerFactory
            .getLogger(ApplicationsServiceTest.class);

    @Test
    public void testPermissions() throws Exception {
        logger.info("PermissionsTest.testPermissions");

        UUID applicationId = createApplication("testOrganization",
                "testPermissions");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        // em.createRole("admin", null);
        em.createRole("manager", null, 0);
        em.createRole("member", null, 0);

        em.grantRolePermission("admin", "users:access:*");
        em.grantRolePermission("admin", "groups:access:*");

        ServiceManager sm = smf.getServiceManager(applicationId);


        testDataRequest(sm, ServiceAction.GET, null, "roles", "admin",
                "permissions");

    }

}
