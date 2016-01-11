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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Mock manager implementation for mockTesting.
 */
class MockUserOrgManager implements UserOrgInterface {

    SetMultimap<Org, OrgUser> usersByOrg = HashMultimap.create();

    SetMultimap<OrgUser, Org> orgsByUser = HashMultimap.create();

    SetMultimap<Org, UUID> appsByOrg = HashMultimap.create();

    Map<UUID, Org> orgsById = new HashMap<UUID, Org>();

    Map<UUID, OrgUser> usersById = new HashMap<UUID, OrgUser>();

    /** represents "index" of users by username */
    Map<String, OrgUser> usersByUsername = new HashMap<String, OrgUser>();

    /** represents "index" of users by email */
    Map<String, OrgUser> usersByEmail = new HashMap<String, OrgUser>();

    
    /**
     * Populate manager with orgs and users.
     * Will create a number of orgs and a duplicate for each.
     *
     * @param numOrgs One half of the number of orgs to create.
     */
    public MockUserOrgManager(int numOrgs) throws Exception {
        
        for (int i = 0; i < numOrgs; i++) {

            // create a pair of duplicate orgs 
            
            // org1 is the original and the oldest one, with capital letters
            Org org1 = new Org( UUID.randomUUID(), "OrG_" + i );
            orgsById.put( org1.getId(), org1 );
            pause(100);
            
            // org2 is duplicate, the newest one
            Org org2 = new Org( UUID.randomUUID(), "org_" + i );
            orgsById.put( org2.getId(), org2 );

            // create three users A, B and C
            
            String base = "user_" + i;
            OrgUser usera = new OrgUser( UUID.randomUUID(), base + "_a", base + "_a@example.com" );
            OrgUser userb = new OrgUser( UUID.randomUUID(), base + "_b", base + "_b@example.com" );
            OrgUser userc = new OrgUser( UUID.randomUUID(), base + "_c", base + "_c@example.com" );

            // org1 gets users A and B
            addUserToOrg( usera, org1 );
            addUserToOrg( userb, org1 );

            // org2 gets users C 
            addUserToOrg( userc, org2 );

            // add some apps to the orgs, org1 gets 2 apps
            addAppToOrg( UUID.randomUUID(), org1 );
            addAppToOrg( UUID.randomUUID(), org1 );
           
            // org2 gets 1 app
            addAppToOrg( UUID.randomUUID(), org2 );
        }
    }
    
    void pause( long timems ) {
        try { Thread.sleep( timems ); } catch (InterruptedException intentionallyIgnored) {}
    }

    @Override
    public rx.Observable<Org> getOrgs() throws Exception {
        return rx.Observable.from( usersByOrg.keySet() );
    }

    @Override
    public rx.Observable<OrgUser> getUsers() throws Exception {
        return rx.Observable.from( orgsByUser.keySet() );
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
        usersByOrg.removeAll( duplicate );
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
                DuplicateAdminUserRepairTest.logger.info( "Duplicate org {}:{}", orgName, org.getId() );
            }
        }
    }

    @Override
    public Org getOrg(UUID uuid) throws Exception {
        return orgsById.get(uuid); 
    }

    @Override
    public OrgUser getOrgUser(UUID id) throws Exception {
        return usersById.get(id);
    }

    @Override
    public OrgUser lookupOrgUserByUsername(String username) {
        return usersByUsername.get(username);
    }

    @Override
    public OrgUser lookupOrgUserByEmail(String email) {
        return usersByEmail.get(email);
    }

    @Override
    public void removeOrgUser(OrgUser orgUser) throws Exception {

        usersById.remove( orgUser.getId() );
        usersByUsername.remove( orgUser.getId() );
        usersByEmail.remove( orgUser.getId() );
        
        Set<Org> orgs = orgsByUser.get( orgUser );
        for ( Org org : orgs ) {
            removeUserFromOrg( orgUser, org );
        }
    }

    @Override
    public void updateOrgUser(OrgUser keeper) throws Exception  {
        // ensure 'keeper' user is the one in the indexes
        usersById.put(       keeper.getId(), keeper );
        usersByUsername.put( keeper.getUsername(), keeper );
        usersByEmail.put(    keeper.getEmail(), keeper );
    }

    @Override
    public void setOrgUserName(OrgUser user, String newUserName) throws Exception  {
        user.setUsername( newUserName );
        updateOrgUser( user ); // re-index user
    }

    // implemented for testing only
    OrgUser createOrgUser(UUID id, String name, String email) {
        OrgUser user = new OrgUser( id, name, email );
        usersById.put( user.getId(), user );
        usersByUsername.put( user.getUsername(), user );
        usersByEmail.put( user.getEmail(), user );
        return user;
    }
    
}
