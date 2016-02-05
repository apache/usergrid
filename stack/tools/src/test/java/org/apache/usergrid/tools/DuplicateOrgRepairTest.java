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


import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

import java.util.*;

import static junit.framework.Assert.assertTrue;
import static org.apache.usergrid.tools.UserOrgInterface.Org;
import static org.apache.usergrid.tools.UserOrgInterface.OrgUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Test duplicate org repair.
 */
public class DuplicateOrgRepairTest {
    static final Logger logger = LoggerFactory.getLogger( DuplicateOrgRepairTest.class );

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );


    /**
     * Test tool logic with mock manager that returns duplicates
     */
    @org.junit.Test
    public void testMockWithDups() throws Exception {

        int numOrgs = 10; // create 10 orgs and a dup for each

        final DuplicateOrgRepair dor = new DuplicateOrgRepair();

        dor.manager = new MockUserOrgManager( numOrgs );

        assertEquals( "must start with dups",
                2 * numOrgs, (long) dor.manager.getOrgs().count().toBlocking().single() );

        dor.startTool( new String[]{}, false ); // false means do not call System.exit()

        assertEquals( "must remove dups",
                numOrgs, (long) dor.manager.getOrgs().count().toBlocking().single() );

        checkOrgsDeduped( dor );
    }


    @org.junit.Test
    public void testMockWithOneDupsDryRun() throws Exception {

        int numOrgs = 1; // create 1 org and a dup

        DuplicateOrgRepair dor = new DuplicateOrgRepair();
        dor.testing = true;
        dor.manager = new MockUserOrgManager( numOrgs );

        assertEquals( "must start with dups",
                2 * numOrgs, (long) dor.manager.getOrgs().count().toBlocking().single() );

        Iterator<Org> orgIter = ((MockUserOrgManager) dor.manager).usersByOrg.keySet().iterator();
        Org org1 = orgIter.next();
        Org org2 = orgIter.next();
        dor.startTool( new String[]{
                "-org1", org1.getId() + "",
                "-org2", org2.getId() + "",
                "-dryrun", "true"
        }, false ); // false means do not call System.exit()

        assertEquals( "dry-run should not remove dups",
                2 * numOrgs, (long) dor.manager.getOrgs().count().toBlocking().single() );
    }


    @org.junit.Test
    public void testMockWithOneDup() throws Exception {

        int numOrgs = 1; // create 1 org and a dup

        DuplicateOrgRepair dor = new DuplicateOrgRepair();
        dor.testing = true;
        dor.manager = new MockUserOrgManager( numOrgs );

        Iterator<Org> orgIter = ((MockUserOrgManager) dor.manager).usersByOrg.keySet().iterator();
        Org org1 = orgIter.next();
        Org org2 = orgIter.next();
        dor.startTool( new String[]{
                "-org1", org1.getId() + "",
                "-org2", org2.getId() + "",
        }, false ); // false means do not call System.exit()

        assertEquals( "must remove dups",
                numOrgs, (long) dor.manager.getOrgs().count().toBlocking().single() );

        checkOrgsDeduped( dor );
    }


    private void checkOrgsDeduped(final DuplicateOrgRepair dor) throws Exception {
        dor.manager.getOrgs().doOnNext( new Action1<Org>() {
            @Override
            public void call(Org org) {
                try {
                    assertEquals( "remaining orgs should have right number of users",
                            3, dor.manager.getOrgUsers( org ).size() );

                    assertEquals( "remaining orgs should have right number of apps",
                            3, dor.manager.getOrgApps( org ).size() );

                } catch (Exception e) {
                    logger.error( "Error counting apps or users: " + e.getMessage(), e );
                    fail( "Error counting apps or users" );
                }
            }
        } ).toBlocking().lastOrDefault( null );
    }


    @org.junit.Test
    public void testMockWithDupsDryRun() throws Exception {

        int numOrgs = 10; // create 10 orgs and a dup for each

        DuplicateOrgRepair dor = new DuplicateOrgRepair();
        dor.manager = new MockUserOrgManager( numOrgs );

        assertEquals( "must start with dups",
                2 * numOrgs, (long) dor.manager.getOrgs().count().toBlocking().single() );

        dor.startTool( new String[]{"-dryrun", "true"}, false ); // false means do not call System.exit()

        assertEquals( "must detect right number of dups",
                numOrgs, dor.duplicatesByName.keySet().size() );

        assertEquals( "dryrun must not remove dups",
                2 * numOrgs, (long) dor.manager.getOrgs().count().toBlocking().single() );
    }


    /**
     * Smoke test: does "real" manager run without throwing exceptions?
     */
    @org.junit.Test
    public void testManagerNoDups() throws Exception {

        // create two orgs each with owning user

        final String random1 = RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo1 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random1, "user_" + random1, "user_" + random1,
                "user_" + random1 + "@example.com", "password" );

        final String random2 = RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo2 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random2, "user_" + random2, "user_" + random2,
                "user_" + random2 + "@example.com", "password" );

        // Add user1 to org2

        setup.getMgmtSvc().addAdminUserToOrganization(
                orgOwnerInfo1.getOwner(), orgOwnerInfo2.getOrganization(), false );

        DuplicateOrgRepair dor = new DuplicateOrgRepair();

        dor.startTool( new String[]{}, false );  // false means do not call System.exit()

        dor.startTool( new String[]{"dryrun", "true"}, false ); // false means do not call System.exit()

        assertTrue( true ); // we're happy if we get to this point
    }


    @org.junit.Test
    public void testManagerAddUserToOrg() throws Exception {

        // create two orgs each with owning user

        final String random1 = RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo1 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random1, "user_" + random1, "user_" + random1,
                "user_" + random1 + "@example.com", "password" );

        final String random2 = RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo2 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random2, "user_" + random2, "user_" + random2,
                "user_" + random2 + "@example.com", "password" );

        DuplicateOrgRepair dor = new DuplicateOrgRepair(setup.getEmf(), setup.getMgmtSvc());
        dor.manager = dor.createNewRepairManager(); // test the real manager 

        // start the tool so that Spring, Cassandra, etc/ gets initialized
        dor.startTool( new String[]{"-dryrun", "true"}, false ); // false means do not call System.exit()

        Org org1 = new Org(
                orgOwnerInfo1.getOrganization().getUuid(), orgOwnerInfo1.getOrganization().getName(), 0L );

        Org org2 = new Org(
                orgOwnerInfo2.getOrganization().getUuid(), orgOwnerInfo2.getOrganization().getName(), 0L );

        OrgUser user1 = new OrgUser(
                orgOwnerInfo1.getOwner().getUuid(),
                orgOwnerInfo1.getOwner().getUsername(),
                orgOwnerInfo1.getOwner().getEmail() );

        assertEquals( 1, dor.manager.getUsersOrgs( user1 ).size() );
        assertEquals( 1, dor.manager.getOrgUsers( org1 ).size() );
        assertEquals( 1, dor.manager.getOrgUsers( org2 ).size() );

        dor.manager.addUserToOrg( user1, org2 );

        assertEquals( 2, dor.manager.getUsersOrgs( user1 ).size() );
        assertEquals( 1, dor.manager.getOrgUsers( org1 ).size() );
        assertEquals( 2, dor.manager.getOrgUsers( org2 ).size() );

        dor.manager.removeUserFromOrg( user1, org2 );

        assertEquals( 1, dor.manager.getUsersOrgs( user1 ).size() );
        assertEquals( 1, dor.manager.getOrgUsers( org1 ).size() );
        assertEquals( 1, dor.manager.getOrgUsers( org2 ).size() );
    }


    @org.junit.Test
    public void testManagerAddAppToOrg() throws Exception {

        // create two orgs each with owning user

        final String random1 = RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo1 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random1, "user_" + random1, "user_" + random1,
                "user_" + random1 + "@example.com", "password" );

        final String random2 = RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo2 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random2, "user_" + random2, "user_" + random2,
                "user_" + random2 + "@example.com", "password" );

        // give org1 two apps

        ApplicationInfo app11 = setup.getMgmtSvc().createApplication(
                orgOwnerInfo1.getOrganization().getUuid(), "app_" + RandomStringUtils.randomAlphanumeric( 10 ) );

        ApplicationInfo app12 = setup.getMgmtSvc().createApplication(
                orgOwnerInfo1.getOrganization().getUuid(), "app_" + RandomStringUtils.randomAlphanumeric( 10 ) );

        // give org2 one app 

        ApplicationInfo app21 = setup.getMgmtSvc().createApplication(
                orgOwnerInfo2.getOrganization().getUuid(), "app_" + RandomStringUtils.randomAlphanumeric( 10 ) );

        DuplicateOrgRepair dor = new DuplicateOrgRepair(setup.getEmf(), setup.getMgmtSvc());
        dor.manager = dor.createNewRepairManager(); // test the real manager 

        // start the tool so that Spring, Cassandra, etc/ gets initialized
        dor.startTool( new String[]{"-dryrun", "true"}, false ); // false means do not call System.exit()

        Org org1 = new Org(
                orgOwnerInfo1.getOrganization().getUuid(), orgOwnerInfo1.getOrganization().getName(), 0L );

        Org org2 = new Org(
                orgOwnerInfo2.getOrganization().getUuid(), orgOwnerInfo2.getOrganization().getName(), 0L );

        assertEquals( 2, dor.manager.getOrgApps( org1 ).size() );
        assertEquals( 1, dor.manager.getOrgApps( org2 ).size() );

        dor.manager.removeAppFromOrg( app12.getId(), org1 );

        assertEquals( 1, dor.manager.getOrgApps( org1 ).size() );
        assertEquals( 1, dor.manager.getOrgApps( org2 ).size() );

        dor.manager.addAppToOrg( app12.getId(), org2 );

        assertEquals( 1, dor.manager.getOrgApps( org1 ).size() );
        assertEquals( 2, dor.manager.getOrgApps( org2 ).size() );
    }

}    

