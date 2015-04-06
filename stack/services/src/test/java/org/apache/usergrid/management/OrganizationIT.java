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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;

import org.apache.usergrid.management.cassandra.ManagementServiceImpl;
import org.apache.usergrid.management.exceptions.RecentlyUsedPasswordException;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;
import org.apache.usergrid.security.AuthPrincipalInfo;

import static org.apache.usergrid.TestHelper.uniqueEmail;
import static org.apache.usergrid.TestHelper.uniqueOrg;
import static org.apache.usergrid.TestHelper.uniqueUsername;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;



public class OrganizationIT {

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl();

    @Rule
    public NewOrgAppAdminRule newOrgAppAdminRule = new NewOrgAppAdminRule( setup );


    @Test
    public void testCreateOrganization() throws Exception {
//        UserInfo user =
//                setup.getMgmtSvc().createAdminUser( uniqueUsername(), "Ed Anuff", uniqueEmail(), "test", false, false );
//        assertNotNull( user );

        final String orgName =  uniqueOrg();
        OrganizationOwnerInfo organization = newOrgAppAdminRule.createOwnerAndOrganization( orgName, uniqueUsername(), uniqueEmail(),"Ed Anuff", "test" ); //setup.getMgmtSvc().getOrganizationsForAdminUser( organization.getOwner().getUuid() );
        //createOrganization( orgName, user, false );
        assertNotNull( organization );

        setup.getEntityIndex().refresh();
        Map<UUID, String> userOrganizations = setup.getMgmtSvc().getOrganizationsForAdminUser(
            organization.getOwner().getUuid() );
        assertEquals( "wrong number of organizations", 1, userOrganizations.size() );

        List<UserInfo> users = setup.getMgmtSvc().getAdminUsersForOrganization(
            organization.getOrganization().getUuid() );
        assertEquals( "wrong number of users", 1, users.size() );

        ApplicationInfo applicationInfo = setup.getMgmtSvc().createApplication(organization.getOrganization().getUuid(), "ed-application");
        assertNotNull( applicationInfo.getId() );


        setup.getEntityIndex().refresh();

        Map<UUID, String> applications = setup.getMgmtSvc().getApplicationsForOrganization( organization.getOrganization().getUuid() );
        assertEquals( "wrong number of applications", 1, applications.size() );

        OrganizationInfo organization2 = setup.getMgmtSvc().getOrganizationForApplication( applicationInfo.getId() );
        assertNotNull( organization2 );
        assertEquals( "wrong organization name", organization.getOrganization().getName(), organization2.getName() );

        boolean verified = setup.getMgmtSvc().verifyAdminUserPassword( organization.getOwner().getUuid(), "test" );
        assertTrue( verified );

        setup.getMgmtSvc().activateOrganization( organization2 );

        setup.getEntityIndex().refresh();

        UserInfo u = setup.getMgmtSvc().verifyAdminUserPasswordCredentials( organization.getOwner().getUuid().toString(), "test" );
        assertNotNull( u );

        String token = setup.getMgmtSvc().getAccessTokenForAdminUser( organization.getOwner().getUuid(), 0 );
        assertNotNull( token );

        AuthPrincipalInfo principal =
                ( ( ManagementServiceImpl ) setup.getMgmtSvc() ).getPrincipalFromAccessToken( token, null, null );
        assertNotNull( principal );
        assertEquals( organization.getOwner().getUuid(), principal.getUuid() );

        UserInfo new_user = setup.getMgmtSvc()
                                 .createAdminUser(uniqueUsername(), "Test User", uniqueEmail(),
                                         "testpassword", true, true );
        assertNotNull( new_user );

        setup.getMgmtSvc().addAdminUserToOrganization( new_user, organization2, false );
    }


    @Test
    public void testPasswordHistoryCheck() throws Exception {

        String[] passwords = new String[] { "password1", "password2", "password3", "password4", "password5" };



        UserInfo user = setup.getMgmtSvc()
                             .createAdminUser( uniqueUsername(), "Ed Anuff", uniqueEmail(), passwords[0], true, false );
        assertNotNull( user );

        OrganizationInfo organization = setup.getMgmtSvc().createOrganization( uniqueOrg(), user, true );
        assertNotNull( organization );

        setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );

        // no history, no problem
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[1] );
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[0] );
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[0] );

        // set history to 4
        Map<String, Object> props = new HashMap<String, Object>();
        props.put( OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 3 );
        organization.setProperties( props );
        setup.getMgmtSvc().updateOrganization( organization );

        // check the history
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[1] ); // ok
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[2] ); // ok
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[3] ); // ok
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[4] ); // ok
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[0] ); // ok

        setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );

        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[3] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }

        // set history to 2
        props = new HashMap<String, Object>();
        props.put( OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 2 );
        organization.setProperties( props );
        setup.getMgmtSvc().updateOrganization( organization );

        // check the history
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[1] ); // ok
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[2] ); // ok
        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[0] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }
        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[1] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }
        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[2] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[3] ); // ok
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[0] ); // ok

        // reduce the history to 1
        props = new HashMap<String, Object>();
        props.put( OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 1 );
        organization.setProperties( props );
        setup.getMgmtSvc().updateOrganization( organization );

        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[1] ); // ok
        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[0] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }

        // test history size w/ user belonging to 2 orgs
        OrganizationInfo organization2 = setup.getMgmtSvc().createOrganization(uniqueOrg(), user, false );
        assertNotNull( organization2 );

        setup.getEntityIndex().refresh();
        Map<UUID, String> userOrganizations = setup.getMgmtSvc().getOrganizationsForAdminUser( user.getUuid() );
        assertEquals( "wrong number of organizations", 2, userOrganizations.size() );

        props = new HashMap<String, Object>();
        props.put( OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 2 );
        organization2.setProperties( props );
        setup.getMgmtSvc().updateOrganization( organization2 );

        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[1] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }
        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[0] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }
        setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[2] );
        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[0] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }
        try {
            setup.getMgmtSvc().setAdminUserPassword( user.getUuid(), passwords[1] );
            fail( "password change should fail" );
        }
        catch ( RecentlyUsedPasswordException e ) {
            // ok
        }
    }
}
