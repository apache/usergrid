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
import static org.apache.usergrid.tools.DuplicateOrgInterface.Org;
import static org.apache.usergrid.tools.DuplicateOrgInterface.OrgUser;
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
        
        dor.manager = new Manager( numOrgs );

        assertEquals( "must start with dups", 
                2 * numOrgs, (long)dor.manager.getOrgs().count().toBlocking().single());

        dor.startTool( new String[] {}, false ); // false means do not call System.exit()

        assertEquals( "must remove dups", 
                numOrgs, (long)dor.manager.getOrgs().count().toBlocking().single());
       
        dor.manager.getOrgs().doOnNext( new Action1<Org>() {
            @Override
            public void call(Org org) {
                try {
                    assertEquals("remaining orgs should have right number of users",
                            3, dor.manager.getOrgUsers(org).size());

                    assertEquals("remaining orgs should have right number of apps", 
                            3, dor.manager.getOrgApps(org).size());
                    
                } catch (Exception e) {
                    logger.error("Error counting apps or users: " + e.getMessage(), e);
                    fail("Error counting apps or users");
                }
            }
        }).toBlocking().lastOrDefault( null );
    }
    
    
    @org.junit.Test
    public void testMockWithDupsDryRun() throws Exception {

        int numOrgs = 10; // create 10 orgs and a dup for each

        DuplicateOrgRepair dor = new DuplicateOrgRepair();
        dor.manager = new Manager( numOrgs );

        assertEquals( "must start with dups", 
                2 * numOrgs, (long)dor.manager.getOrgs().count().toBlocking().single());

        dor.startTool( new String[] { "-dryrun", "true" }, false ); // false means do not call System.exit()

        assertEquals( "must detect right number of dups", 
                numOrgs, dor.duplicatesByName.keySet().size() );
        
        assertEquals( "dryrun must not remove dups", 
                2 * numOrgs, (long)dor.manager.getOrgs().count().toBlocking().single());
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
        
        dor.startTool( new String[] {}, false );  // false means do not call System.exit()
        
        dor.startTool( new String[] { "dryrun", "true" }, false ); // false means do not call System.exit()

        assertTrue(true); // we're happy if we get to this point
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

        DuplicateOrgRepair dor = new DuplicateOrgRepair();
        dor.manager = dor.createNewRepairManager(); // test the real manager 

        // start the tool so thaht Spring, Cassandra, etc/ gets initialized
        dor.startTool( new String[] { "-dryrun", "true" }, false ); // false means do not call System.exit()
        
        Org org1 = new Org(
            orgOwnerInfo1.getOrganization().getUuid(), orgOwnerInfo1.getOrganization().getName(), 0L);

        Org org2 = new Org(
            orgOwnerInfo2.getOrganization().getUuid(), orgOwnerInfo2.getOrganization().getName(), 0L);

        OrgUser user1 = new OrgUser(
                orgOwnerInfo1.getOwner().getUuid(), orgOwnerInfo1.getOwner().getUsername());

        OrgUser user2 = new OrgUser(
                orgOwnerInfo2.getOwner().getUuid(), orgOwnerInfo2.getOwner().getUsername());

        assertEquals( 1, dor.manager.getUsersOrgs( user1 ).size());
        assertEquals( 1, dor.manager.getOrgUsers( org1 ).size());
        assertEquals( 1, dor.manager.getOrgUsers( org2 ).size());

        dor.manager.addUserToOrg( user1, org2 );

        assertEquals( 2, dor.manager.getUsersOrgs( user1 ).size());
        assertEquals( 1, dor.manager.getOrgUsers( org1 ).size() );
        assertEquals( 2, dor.manager.getOrgUsers( org2 ).size() );

        dor.manager.removeUserFromOrg( user1, org2 );
        
        assertEquals( 1, dor.manager.getUsersOrgs( user1 ).size());
        assertEquals( 1, dor.manager.getOrgUsers( org1 ).size());
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

        ApplicationInfo app12= setup.getMgmtSvc().createApplication(
            orgOwnerInfo1.getOrganization().getUuid(), "app_" + RandomStringUtils.randomAlphanumeric( 10 ));

        // give org2 one app 
        
        ApplicationInfo app21 = setup.getMgmtSvc().createApplication(
            orgOwnerInfo2.getOrganization().getUuid(), "app_" + RandomStringUtils.randomAlphanumeric( 10 ));

        DuplicateOrgRepair dor = new DuplicateOrgRepair();
        dor.manager = dor.createNewRepairManager(); // test the real manager 

        // start the tool so that Spring, Cassandra, etc/ gets initialized
        dor.startTool( new String[] { "-dryrun", "true" }, false ); // false means do not call System.exit()

        Org org1 = new Org(
            orgOwnerInfo1.getOrganization().getUuid(), orgOwnerInfo1.getOrganization().getName(), 0L);

        Org org2 = new Org(
            orgOwnerInfo2.getOrganization().getUuid(), orgOwnerInfo2.getOrganization().getName(), 0L);

        assertEquals( 2, dor.manager.getOrgApps( org1 ).size() );
        assertEquals( 1, dor.manager.getOrgApps( org2 ).size() );

        dor.manager.removeAppFromOrg( app12.getId(), org1 );

        assertEquals( 1, dor.manager.getOrgApps( org1 ).size() );
        assertEquals( 1, dor.manager.getOrgApps( org2 ).size() );

        dor.manager.addAppToOrg( app12.getId(), org2 );
        
        assertEquals( 1, dor.manager.getOrgApps( org1 ).size() );
        assertEquals( 2, dor.manager.getOrgApps( org2 ).size() );
    }
    
    
    /**
     * Mock manager implementation for mockTesting.
     */
    class Manager implements DuplicateOrgInterface {

        Set<Org> orgs;

        Set<OrgUser> orgUsers;

        Map<Org, Set<OrgUser>> usersByOrg = new HashMap<Org, Set<OrgUser>>();

        Map<OrgUser, Set<Org>> orgsByUser = new HashMap<OrgUser, Set<Org>>();

        Map<Org, Set<UUID>> appsByOrg = new HashMap<Org, Set<UUID>>();


        /**
         * Populate manager with orgs and users.
         * Will create a number of orgs and a duplicate for each.
         *
         * @param numOrgs One half of the number of orgs to create.
         */
        public Manager(int numOrgs) {

            for (int i = 0; i < numOrgs; i++) {

                // each org name is duplicated once another org created 20 ms apart

                Org org1 = new Org( UUID.randomUUID(), "org_" + i, System.currentTimeMillis() );
                try {
                    Thread.sleep( 100 );
                } catch (InterruptedException intentionallyIgnored) {
                }
                Org org2 = new Org( UUID.randomUUID(), "org_" + i, System.currentTimeMillis() );

                OrgUser usera = new OrgUser( UUID.randomUUID(), "user_" + i + "_a" );
                OrgUser userb = new OrgUser( UUID.randomUUID(), "user_" + i + "_b" );
                OrgUser userc = new OrgUser( UUID.randomUUID(), "user_" + i + "_c" );

                // add users to orgs 

                Set<OrgUser> org1Users = new HashSet<OrgUser>();
                org1Users.add( usera );
                org1Users.add( userb );
                usersByOrg.put( org1, org1Users );

                Set<OrgUser> org2Users = new HashSet<OrgUser>();
                org2Users.add( userc );
                usersByOrg.put( org2, org2Users );

                // add orgs to users 

                Set<Org> useraOrgs = new HashSet<Org>();
                useraOrgs.add( org1 );
                orgsByUser.put( usera, useraOrgs );

                Set<Org> userbOrgs = new HashSet<Org>();
                userbOrgs.add( org1 );
                orgsByUser.put( userb, userbOrgs );

                Set<Org> usercOrgs = new HashSet<Org>();
                usercOrgs.add( org2 );
                orgsByUser.put( userc, usercOrgs );

                // add some apps to the orgs

                Set<UUID> org1apps = new HashSet<UUID>();
                org1apps.add( UUID.randomUUID() );
                org1apps.add( UUID.randomUUID() );
                appsByOrg.put( org1, org1apps );

                Set<UUID> org2apps = new HashSet<UUID>();
                org2apps.add( UUID.randomUUID() );
                appsByOrg.put( org2, org2apps );
            }
        }


        @Override
        public Observable<Org> getOrgs() throws Exception {
            return Observable.from( usersByOrg.keySet() );
        }

        @Override
        public Observable<OrgUser> getUsers() throws Exception {
            return Observable.from( orgsByUser.keySet() );
        }

        @Override
        public Set<Org> getUsersOrgs(OrgUser user) {
            return orgsByUser.get( user );
        }

        @Override
        public void removeOrg(Org keeper, Org duplicate) throws Exception {
            Set<OrgUser> users = usersByOrg.get( duplicate );
            for (OrgUser user : users) {
                Set<Org> userOrgs = orgsByUser.get( user );
                userOrgs.remove( duplicate );
            }
            usersByOrg.remove( duplicate );
        }

        @Override
        public Set<OrgUser> getOrgUsers(Org org) throws Exception {
            return usersByOrg.get( org );
        }

        @Override
        public void removeUserFromOrg(OrgUser user, Org org) throws Exception {
            
            Set<OrgUser> orgUsers = usersByOrg.get( org );
            orgUsers.remove( user );
            
            Set<Org> usersOrgs = orgsByUser.get( user );
            usersOrgs.remove( org );
        }

        @Override
        public void addUserToOrg(OrgUser user, Org org) throws Exception {
            
            Set<Org> usersOrgs = orgsByUser.get( user );
            usersOrgs.add( org );
            
            Set<OrgUser> orgsUsers = usersByOrg.get( org );
            orgsUsers.add( user );
        }

        @Override
        public Set<UUID> getOrgApps(Org org) {
            return appsByOrg.get( org );
        }

        @Override
        public void removeAppFromOrg(UUID appId, Org org) throws Exception {
            Set<UUID> apps = appsByOrg.get( org );
            apps.remove( appId );
        }

        @Override
        public void addAppToOrg(UUID appId, Org org) throws Exception {
            Set<UUID> apps = appsByOrg.get( org );
            apps.add(appId); 
        }

        @Override
        public void logDuplicates(Map<String, Set<Org>> duplicatesByName) {

            for (String orgName : duplicatesByName.keySet()) {
                Set<Org> orgs = duplicatesByName.get( orgName );
                for (Org org : orgs) {
                    logger.info( "Duplicate org {}:{}", orgName, org.getId() );
                }
            }
        }
    }
}
