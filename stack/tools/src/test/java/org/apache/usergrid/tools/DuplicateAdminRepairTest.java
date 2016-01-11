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
package org.apache.usergrid.tools;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.management.UserInfo;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertNotNull;
import static org.apache.usergrid.tools.UserOrgInterface.OrgUser;
import static org.junit.Assert.assertEquals;


/**
 * Test duplicate admin repair.
 */
public class DuplicateAdminRepairTest {
    
    static final Logger logger = LoggerFactory.getLogger( DuplicateAdminRepairTest.class );
    
    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );

    
    /**
     * Test tool logic with mock manager that returns duplicates
     */
    @org.junit.Test
    public void testMockWithDups() throws Exception {

        DuplicateAdminRepair dor = new DuplicateAdminRepair();
        MockManager mockManager = new MockManager();
        dor.manager = mockManager;
        dor.testing = true;

        // the MockManager creates 2 pairs of duplicate orgs
        assertNotNull( "user1_a", mockManager.getOrgUser( mockManager.user1_a.getId() ));
        assertNotNull( "user1_b", mockManager.getOrgUser( mockManager.user1_b.getId() ));
        assertNotNull( "user2_a", mockManager.getOrgUser( mockManager.user2_a.getId() ));
        assertNotNull( "user2_b", mockManager.getOrgUser( mockManager.user2_b.getId() ));

        // verify that correct users indexed
        assertUsersIndexed( mockManager );
        
        dor.startTool( new String[] {}, false ); // false means do not call System.exit()

        // verify that correct users indexed
        assertUsersIndexed( mockManager );

        // verify that duplicate users are gone (the "a" users were the first ones created)
        assertNull( "must remove user1_a", mockManager.getOrgUser( mockManager.user1_a.getId() ));
        assertNull( "must remove user2_a", mockManager.getOrgUser( mockManager.user2_a.getId() ));

        // and keepers survived
        assertNotNull( "user1_b survived", mockManager.getOrgUser( mockManager.user1_b.getId() ));
        assertNotNull( "user2_b survived", mockManager.getOrgUser( mockManager.user2_b.getId() ));
    }

    
    private void assertUsersIndexed(MockManager mockManager ) {
        assertEquals("user1_b is in the index",
                mockManager.lookupOrgUserByUsername(mockManager.user1_b.getUsername()).getId(),
                mockManager.user1_b.getId() );

        assertEquals("user1_b is in the index",
                mockManager.lookupOrgUserByEmail(mockManager.user1_b.getEmail()).getId(),
                mockManager.user1_b.getId() );

        assertEquals("user2_b is in the index",
                mockManager.lookupOrgUserByUsername(mockManager.user2_b.getUsername()).getId(),
                mockManager.user2_b.getId() );

        assertEquals("user2_b is in the index",
                mockManager.lookupOrgUserByEmail(mockManager.user2_b.getEmail()).getId(),
                mockManager.user2_b.getId() ); 
    }
    

    @org.junit.Test
    public void testDryRun() throws Exception {

        DuplicateAdminRepair dor = new DuplicateAdminRepair();
        MockManager mockManager = new MockManager();
        dor.manager = mockManager;
        dor.testing = true;

        // the MockManager creates 2 pairs of duplicate orgs
        assertNotNull( "user1_a", mockManager.getOrgUser( mockManager.user1_a.getId() ));
        assertNotNull( "user1_b", mockManager.getOrgUser( mockManager.user1_b.getId() ));
        assertNotNull( "user2_a", mockManager.getOrgUser( mockManager.user2_a.getId() ));
        assertNotNull( "user2_b", mockManager.getOrgUser( mockManager.user2_b.getId() ));

        // verify that correct users indexed
        assertUsersIndexed( mockManager );

        dor.startTool( new String[] { "-dryrun", "true" }, false ); // false means do not call System.exit()

        // verify that correct users indexed
        assertUsersIndexed( mockManager );
        
        // insure nothng was deleted by dry-run
        assertNotNull( "dryrun should not delete user1_a", mockManager.getOrgUser( mockManager.user1_a.getId() ));
        assertNotNull( "dryrun should not delete user1_b", mockManager.getOrgUser( mockManager.user1_b.getId() ));
        assertNotNull( "dryrun should not delete user2_a", mockManager.getOrgUser( mockManager.user2_a.getId() ));
        assertNotNull( "dryrun should not delete user2_b", mockManager.getOrgUser( mockManager.user2_b.getId() ));  
    }

    
    /**
     * Smoke test: does "real" manager run without throwing exceptions?
     */
    @org.junit.Test
    public void testManagerNoDups() throws Exception {

        // create two orgs each with owning user

        final String random1 = org.apache.commons.lang.RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo1 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random1, "user_" + random1, "user_" + random1,
                "user_" + random1 + "@example.com", "password" );

        final String random2 = org.apache.commons.lang.RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo2 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random2, "user_" + random2, "user_" + random2,
                "user_" + random2 + "@example.com", "password" );

        // Add user1 to org2

        setup.getMgmtSvc().addAdminUserToOrganization(
                orgOwnerInfo1.getOwner(), orgOwnerInfo2.getOrganization(), false );

        DuplicateAdminRepair dor = new DuplicateAdminRepair();

        dor.startTool( new String[]{}, false );  // false means do not call System.exit()

        assertTrue( true ); // we're happy if we get to this point
    }

    
    @org.junit.Test
    public void testManagerLookupMethods() throws Exception {
        
        // create two orgs each with owning user

        final String random1 = org.apache.commons.lang.RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo1 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random1, "user_" + random1, "user_" + random1,
                "user_" + random1 + "@example.com", "password" );

        final String random2 = org.apache.commons.lang.RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo2 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random2, "user_" + random2, "user_" + random2,
                "user_" + random2 + "@example.com", "password" );

        DuplicateAdminRepair dor = new DuplicateAdminRepair(setup.getEmf(), setup.getMgmtSvc());
        dor.manager = dor.createNewRepairManager(); // test the real manager 

        // start the tool so that Spring, Cassandra, etc/ gets initialized
        dor.startTool( new String[]{"-dryrun", "true"}, false ); // false means do not call System.exit()

        testManagerLookupMethods( dor, orgOwnerInfo1, orgOwnerInfo2, true );
        
        dor.manager.removeOrgUser( dor.manager.getOrgUser( orgOwnerInfo1.getOwner().getUuid() )); 
        dor.manager.removeOrgUser( dor.manager.getOrgUser( orgOwnerInfo2.getOwner().getUuid() ));

        testManagerLookupMethods( dor, orgOwnerInfo1, orgOwnerInfo2, false );
    }
    
    
    private void testManagerLookupMethods( DuplicateAdminRepair dor, 
                                    OrganizationOwnerInfo info1, 
                                    OrganizationOwnerInfo info2,
                                    boolean usersExist ) throws Exception {
        if ( usersExist ) {
            
            assertNotNull( dor.manager.getOrgUser( info1.getOwner().getUuid() ));
            assertNotNull( dor.manager.getOrgUser( info2.getOwner().getUuid() ));

            assertNotNull( dor.manager.lookupOrgUserByEmail( info1.getOwner().getEmail() ));
            assertNotNull( dor.manager.lookupOrgUserByEmail( info2.getOwner().getEmail() ));

            assertNotNull( dor.manager.lookupOrgUserByUsername( info1.getOwner().getUsername() ));
            assertNotNull( dor.manager.lookupOrgUserByUsername( info2.getOwner().getUsername() )); 
            
        } else {

            assertNull( dor.manager.getOrgUser( info1.getOwner().getUuid() ) );
            assertNull( dor.manager.getOrgUser( info2.getOwner().getUuid() ) );

            assertNull( dor.manager.lookupOrgUserByEmail( info1.getOwner().getEmail() ) );
            assertNull( dor.manager.lookupOrgUserByEmail( info2.getOwner().getEmail() ) );

            assertNull( dor.manager.lookupOrgUserByUsername( info1.getOwner().getUsername() ) );
            assertNull( dor.manager.lookupOrgUserByUsername( info2.getOwner().getUsername() ) );
        }
    }


    @org.junit.Test
    public void testManagerOrgUserUpdateMethod() throws Exception {

        // create an org with an admin user
        final String random1 = org.apache.commons.lang.RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo1 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random1, "user_" + random1, "user_" + random1,
                "user_" + random1 + "@example.com", "password" );

        DuplicateAdminRepair dor = new DuplicateAdminRepair(setup.getEmf(), setup.getMgmtSvc());
        dor.manager = dor.createNewRepairManager(); // test the real manager 

        // start the tool so that Spring, Cassandra, etc/ gets initialized
        dor.startTool( new String[]{"-dryrun", "true"}, false ); // false means do not call System.exit()

        UserInfo userInfo = setup.getMgmtSvc().getAdminUserByUuid( orgOwnerInfo1.getOwner().getUuid() );
        OrgUser user = dor.manager.getOrgUser( orgOwnerInfo1.getOwner().getUuid() );
        assertEquals( userInfo.getUsername(), user.getUsername());
        assertEquals( userInfo.getEmail(), user.getEmail());

        // change user's username using updateOrgUser()
        String newUsername = "boom_" + random1;
        user.setUsername(newUsername);
        dor.manager.updateOrgUser( user );
        user = dor.manager.getOrgUser( orgOwnerInfo1.getOwner().getUuid() );
        assertEquals( newUsername, user.getUsername());
        
        // change user's username using setOrgUserName()
        newUsername = "blammo_" + random1;
        dor.manager.setOrgUserName( user, newUsername );
        user = dor.manager.getOrgUser( orgOwnerInfo1.getOwner().getUuid() );
        assertEquals( newUsername, user.getUsername());
    }
    

    /**
     * Extend mock manager to add a pair of duplicate users.
     */
    static class MockManager extends MockUserOrgManager {

        OrgUser user1_a;
        OrgUser user1_b;
        OrgUser user2_a;
        OrgUser user2_b;
        
        public MockManager() throws Exception {
            
            super(1); // ask parent to create one pair of duplicate orgs
            
            Org org = orgsById.values().iterator().next();
          
            String sfx = RandomStringUtils.randomAlphanumeric(10);

            // user1 a and b have duplicate usernames AND DUPLICATE EMAIL ADDRESSES
            
            user1_a = createOrgUser( UUID.randomUUID(), "UserName_"+sfx, "UserName_"+sfx+"@example.com" );
            addUserToOrg( user1_a, org );
            pause(100);

            user1_b = createOrgUser( UUID.randomUUID(), "Username_"+sfx, "Username_"+sfx+"@example.com" );
            addUserToOrg( user1_b, org );
            pause(100);

            // user2 a and b have duplicate usernames AND DIFFERENT EMAIL ADDRESSES 
            
            user2_a = createOrgUser( UUID.randomUUID(), "UserName_"+sfx, "UserName_"+sfx+"@example.com" );
            addUserToOrg( user2_a, org );
            pause(100);

            user2_b = createOrgUser( UUID.randomUUID(), "UserName_"+sfx, "UserName_"+sfx+"@example.com" );
            addUserToOrg( user2_b, org );
            pause(100);

        }
        
    }

}
