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


import org.apache.shiro.authc.ExcessiveAttemptsException;
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
        String testKey = OrganizationConfigProps.ORGPROPERTIES_ADMIN_SYSADMIN_EMAIL;
        String testValue = "orgconfigtest@usergrid.com";
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
    public void testWorkflowUrls() throws Exception {

        final String orgName =  uniqueOrg();

        UserInfo user = setup.getMgmtSvc().createAdminUser(null, uniqueUsername(), "Org Config Admin", uniqueEmail(), "test", true, false );
        assertNotNull( user );

        OrganizationInfo org = setup.getMgmtSvc().createOrganization( orgName, user, true );
        assertNotNull( org );

        setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );

        OrganizationConfig orgConfig = setup.getMgmtSvc().getOrganizationConfigByName(orgName);
        assertNotNull(orgConfig);

        String userActivationPath = "/%s/%s/users/%s/activate";
        String defaultApiUrlBase = "http://localhost:8080";
        String userActivationUrlTemplate = orgConfig.getFullUrlTemplate(OrganizationConfigProps.WorkflowUrl.USER_ACTIVATION_URL);
        assertTrue(userActivationUrlTemplate.equals(defaultApiUrlBase + userActivationPath));

        // insert a new URL base for the org
        Map<String, Object> propMap = new HashMap<>();
        String baseKey = OrganizationConfigProps.ORGPROPERTIES_API_URL_BASE;
        String newApiUrlBase = "http://example.org";
        propMap.put(baseKey, newApiUrlBase);
        orgConfig.addProperties(propMap, false);
        setup.getMgmtSvc().updateOrganizationConfig(orgConfig);

        setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );

        // get org config again
        OrganizationConfig orgConfigUpdated = setup.getMgmtSvc().getOrganizationConfigByUuid(org.getUuid());
        assertNotNull(orgConfigUpdated);
        String updatedValue = orgConfigUpdated.getProperty(OrganizationConfigProps.ORGPROPERTIES_API_URL_BASE);
        assertTrue(updatedValue.equals(newApiUrlBase));

        // get new URL
        String newUserActivationUrlTemplate = orgConfigUpdated.getFullUrlTemplate(OrganizationConfigProps.WorkflowUrl.USER_ACTIVATION_URL);
        assertTrue(newUserActivationUrlTemplate.equals(newApiUrlBase + userActivationPath));
    }

    @Test
    public void testNonOrgProperty() throws Exception {
        String testNonOrgProperty = "usergrid.sysadmin.login.name";
        String testOrgProperty = "usergrid.admin.sysadmin.email";

        // get properties directly
        String nonOrgPropsValue = setup.getProps().getProperty(testNonOrgProperty);
        String orgPropsValue = setup.getProps().getProperty(testOrgProperty);
        assertNotNull(nonOrgPropsValue);
        assertNotNull(orgPropsValue);

        final String orgName =  uniqueOrg();

        UserInfo user = setup.getMgmtSvc().createAdminUser(null, uniqueUsername(), "Org Config Admin", uniqueEmail(), "test", true, false );
        assertNotNull( user );

        OrganizationInfo org = setup.getMgmtSvc().createOrganization( orgName, user, true );
        assertNotNull( org );

        setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );

        OrganizationConfig orgConfig = setup.getMgmtSvc().getOrganizationConfigByUuid(org.getUuid());
        assertNotNull(orgConfig);

        // get properties from orgConfig, should equal
        String nonOrgCfgValue = orgConfig.getProperty(testNonOrgProperty);
        String orgCfgValue = orgConfig.getProperty(testOrgProperty);
        assertNotNull(nonOrgCfgValue);
        assertNotNull(orgCfgValue);
        assertTrue(nonOrgPropsValue.equals(nonOrgCfgValue));
        assertTrue(orgPropsValue.equals(orgCfgValue));

        // try to set the org properties (one is org configurable, one is not)
        String newNonOrgPropertyValue = "testNonOrgLoginName";
        String newOrgPropertyValue = "testNonOrgProperty@usergrid.com";

        Map<String, Object> propMap = new HashMap<>();
        propMap.put(testNonOrgProperty, newNonOrgPropertyValue);
        propMap.put(testOrgProperty, newOrgPropertyValue);
        try {
            // true validates that all passed in properties are org-configurable
            orgConfig.addProperties(propMap, true);
            fail("Validation of orgConfig.addProperties should have thrown exception");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        catch (Exception e) {
            fail("Validation of orgConfig.addProperties should have thrown IllegalArgumentException");
        }

        // false doesn't validate, ignores invalid org config items
        orgConfig.addProperties(propMap, false);

        String nonOrgCfgValue2 = orgConfig.getProperty(testNonOrgProperty);
        String orgCfgValue2 = orgConfig.getProperty(testOrgProperty);

        assertNotNull(nonOrgCfgValue2);
        assertNotNull(orgCfgValue2);
        // only org config item should have been updated
        assertFalse(nonOrgCfgValue2.equals(newNonOrgPropertyValue));
        assertTrue(nonOrgCfgValue2.equals(nonOrgCfgValue));
        assertTrue(orgCfgValue2.equals(newOrgPropertyValue));

    }
}