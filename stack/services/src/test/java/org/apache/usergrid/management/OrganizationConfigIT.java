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
package org.apache.usergrid.management;


import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.cassandra.ManagementServiceImpl;
import org.apache.usergrid.management.cassandra.OrganizationConfigPropsImpl;
import org.apache.usergrid.management.exceptions.RecentlyUsedPasswordException;
import org.apache.usergrid.persistence.index.utils.MapUtils;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;

import static org.apache.usergrid.TestHelper.*;
import static org.junit.Assert.*;


public class OrganizationConfigIT {

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl();

    @Rule
    public NewOrgAppAdminRule newOrgAppAdminRule = new NewOrgAppAdminRule( setup );

    @Test
    public void testCreateOrganizationConfig() throws Exception {

        final String orgName =  uniqueOrg();

        UserInfo user = setup.getMgmtSvc().createAdminUser(null, uniqueUsername(), "Org Config Admin", uniqueEmail(), "test", true, false );
        assertNotNull( user );

        OrganizationInfo org = setup.getMgmtSvc().createOrganization( orgName, user, true );
        assertNotNull( org );

        setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );

        OrganizationConfig orgConfig = setup.getMgmtSvc().getOrganizationConfigByUuid(org.getUuid());
        assertNotNull(orgConfig);

        // until something added to it, returned orgConfig should match the values in the default

        // empty org config with default properties from config file
        OrganizationConfigProps configProps = new OrganizationConfigPropsImpl(setup.getMgmtSvc().getProperties());
        Map<Object, Object> emptyConfigProps = new HashMap<>();

        OrganizationConfig orgConfigDefault = new OrganizationConfig( configProps, org.getUuid(), org.getName(), emptyConfigProps, false );
        assertTrue(orgConfig.equals(orgConfigDefault));

        // insert a config value for the org
        Map<String, Object> propMap = new HashMap<>();
        String testKey = OrganizationConfigProps.PROPERTIES_ADMIN_RESETPW_URL;
        String testValue = "***TEST VALUE***";
        propMap.put(testKey, testValue);
        orgConfig.addProperties(propMap, false);
        setup.getMgmtSvc().updateOrganizationConfig(orgConfig);

        setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );

        // get org config again
        OrganizationConfig orgConfigUpdated = setup.getMgmtSvc().getOrganizationConfigByUuid(org.getUuid());
        assertNotNull(orgConfigUpdated);
        String updatedValue = orgConfigUpdated.getProperty(testKey);
        assertTrue(updatedValue.equals(testValue));

        // delete the config entry by setting it to ""
        propMap.put(testKey, "");
        orgConfigUpdated.addProperties(propMap, false);
        setup.getMgmtSvc().updateOrganizationConfig(orgConfigUpdated);

        setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );

        // get org config again, should match defaults
        OrganizationConfig orgConfigReset = setup.getMgmtSvc().getOrganizationConfigByUuid(org.getUuid());
        assertNotNull(orgConfigReset);
        assertTrue(orgConfigReset.equals(orgConfigDefault));
    }

    @Test
    public void testOrganizationConfigInvalidKeys() throws Exception {
        // TODO: add test
    }
}