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

import com.google.common.collect.BiMap;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.util.*;


class UserOrgManager implements UserOrgInterface {

    static final Logger logger = LoggerFactory.getLogger( UserOrgManager.class );

    EntityManagerFactory emf;
    ManagementService managementService;

    public UserOrgManager(EntityManagerFactory emf, ManagementService managementService) {
        this.emf = emf;
        this.managementService = managementService;
    }

    @Override
    public Observable<Org> getOrgs() throws Exception {

        return Observable.create( new Observable.OnSubscribe<Org>() {

            @Override
            public void call(Subscriber<? super Org> subscriber) {
                subscriber.onStart();
                try {
                    int count = 0;

                    Query query = new Query();
                    query.setLimit( ToolBase.MAX_ENTITY_FETCH );
                    query.setResultsLevel( Query.Level.ALL_PROPERTIES );
                    EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
                    Results results = em.searchCollection( em.getApplicationRef(), "groups", query );

                    while (results.size() > 0) {
                        for (Entity orgEntity : results.getList()) {

                            Org org = new Org(
                                orgEntity.getUuid(),
                                orgEntity.getProperty( "path" ) + "",
                                orgEntity.getCreated() );
                            org.sourceValue = orgEntity;

                            subscriber.onNext( org );

                            if (count++ % 1000 == 0) {
                                logger.info( "Emitted {} orgs", count );
                            }

                        }
                        if (results.getCursor() == null) {
                            break;
                        }
                        query.setCursor( results.getCursor() );
                        results = em.searchCollection( em.getApplicationRef(), "groups", query );
                    }

                } catch (Exception e) {
                    subscriber.onError( e );
                }
                subscriber.onCompleted();
            }
        } );
    }

    @Override
    public Observable<OrgUser> getUsers() throws Exception {

        return Observable.create( new Observable.OnSubscribe<OrgUser>() {

            @Override
            public void call(Subscriber<? super OrgUser> subscriber) {
                subscriber.onStart();
                try {
                    int count = 0;

                    Query query = new Query();
                    query.setLimit( ToolBase.MAX_ENTITY_FETCH );
                    query.setResultsLevel( Query.Level.ALL_PROPERTIES );
                    EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
                    Results results = em.searchCollection( em.getApplicationRef(), "users", query );

                    while (results.size() > 0) {
                        for (Entity entity : results.getList()) {

                            OrgUser orgUser = new OrgUser(
                                entity.getUuid(),
                                entity.getProperty( "username" ) + "",
                                entity.getProperty( "email" ) + "",
                                entity.getCreated()
                            );
                            orgUser.sourceValue = entity;

                            subscriber.onNext( orgUser );

                            if (count++ % 1000 == 0) {
                                logger.info( "Emitted {} users", count );
                            }
                        }
                        if (results.getCursor() == null) {
                            break;
                        }
                        query.setCursor( results.getCursor() );
                        results = em.searchCollection( em.getApplicationRef(), "users", query );
                    }

                } catch (Exception e) {
                    subscriber.onError( e );
                }
                subscriber.onCompleted();
            }
        } );
    }

    @Override
    public Set<Org> getUsersOrgs(OrgUser user) throws Exception {

        Set<Org> ret = new HashSet<Org>();

        Map<String, Object> orgData = managementService.getAdminUserOrganizationData( user.getId() );

        Map<String, Object> orgs = (Map<String, Object>) orgData.get( "organizations" );
        for (String orgName : orgs.keySet()) {

            Map<String, Object> orgMap = (Map<String, Object>) orgs.get( orgName );
            Group group = managementService.getOrganizationProps(
                UUID.fromString( orgMap.get( "uuid" ).toString() ) );

            Org org = new Org(
                group.getUuid(),
                group.getPath(),
                group.getCreated()
            );
            ret.add( org );
        }

        return ret;
    }


    @Override
    public void removeOrg(Org keeper, Org duplicate) throws Exception {

        // rename org so that it is no longer a duplicate
        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
        em.delete( new SimpleEntityRef( "group", duplicate.getId() ) );
        logger.info( "Deleted org {}:{}", new Object[]{duplicate.getName(), duplicate.getId()} );

        // fix the org name index
        OrganizationInfo orgInfoKeeper = managementService.getOrganizationByUuid( keeper.getId() );
        try {
            Entity orgKeeper = em.get( keeper.getId() );
            em.update( orgKeeper );
            //managementService.updateOrganizationUniqueIndex( orgInfoKeeper, duplicate.getId() );
            logger.info( "Updated index for keeper {}:{} not dup {}", new Object[]{
                orgInfoKeeper.getName(), orgInfoKeeper.getUuid(), duplicate.getId()} );

        } catch (Exception e) {
            // if there are multiple duplicates this will fail for all but one of them. That's OK
            logger.warn( "Error repairing unique value keeper {} duplicate {}",
                keeper.getId(), duplicate.getId() );
        }
    }


    @Override
    public Set<OrgUser> getOrgUsers(Org org) throws Exception {

        Set<OrgUser> ret = new HashSet<OrgUser>();

        List<UserInfo> userInfos = managementService.getAdminUsersForOrganization( org.getId() );

        for (UserInfo userInfo : userInfos) {
            OrgUser orgUser = new OrgUser( userInfo.getUuid(), userInfo.getUsername(), userInfo.getEmail() );
            ret.add( orgUser );
        }

        return ret;
    }


    @Override
    public void removeUserFromOrg(OrgUser user, Org org) throws Exception {
        // forcefully remove admin user from org
        managementService.removeAdminUserFromOrganization( user.getId(), org.getId(), true );
        logger.info( "Removed user {}:{} from org {}:{}", new Object[]{
            user.getUsername(), user.getId(), org.getName(), org.getId()} );
    }


    @Override
    public void addUserToOrg(OrgUser user, Org org) throws Exception {
        UserInfo userInfo = managementService.getAdminUserByUsername( user.getUsername() );
        OrganizationInfo orgInfo = managementService.getOrganizationByUuid( org.getId() );
        managementService.addAdminUserToOrganization( userInfo, orgInfo, false );
        logger.info( "Added user {}:{} to org {}:{}", new Object[]{
            user.getUsername(), user.getId(), org.getName(), org.getId()} );
    }


    @Override
    public Set<UUID> getOrgApps(Org org) throws Exception {
        BiMap<UUID, String> apps = managementService.getApplicationsForOrganization( org.getId() );
        return apps.keySet();
    }


    @Override
    public void removeAppFromOrg(UUID appId, Org org) throws Exception {
        managementService.removeOrganizationApplication( org.getId(), appId );
        logger.info( "Removed app {} from org {}:{}", new Object[]{
            appId, org.getName(), org.getId()} );
    }


    @Override
    public void addAppToOrg(UUID appId, Org org) throws Exception {
        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
        Entity appEntity = em.getApplication();
        managementService.addApplicationToOrganization( org.getId(), appEntity );
        logger.info( "Added app {} to org {}:{}", new Object[]{
            appId, org.getName(), org.getId()} );
    }


    @Override
    public void logDuplicates(Map<String, Set<Org>> duplicatesByName) {

        for (String orgName : duplicatesByName.keySet()) {
            Set<Org> orgs = duplicatesByName.get( orgName );
            for (Org org : orgs) {
                Entity orgEntity = (Entity) org.sourceValue;

                StringBuilder sb = new StringBuilder();
                sb.append( orgEntity.toString() ).append( ", " );

                try {
                    BiMap<UUID, String> apps =
                        managementService.getApplicationsForOrganization( orgEntity.getUuid() );
                    String sep = "";
                    for (UUID uuid : apps.keySet()) {
                        String appName = apps.get( uuid );
                        sb.append( appName ).append( ":" ).append( uuid ).append( sep );
                        sep = ", ";
                    }

                } catch (Exception e) {
                    logger.error( "Error getting applications for org {}:{}", org.getName(), org.getId() );
                }

                logger.info( sb.toString() );
            }
        }
    }


    @Override
    public Org getOrg(UUID uuid) throws Exception {
        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
        Group entity = em.get( uuid , Group.class );
        if ( entity != null ) {
            Org org = new Org(
                entity.getUuid(),
                entity.getPath(),
                entity.getCreated() );
            org.sourceValue = entity;
            return org;
        }
        return null;
    }


    @Override
    public OrgUser getOrgUser(UUID uuid) throws Exception {
        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
        User entity = em.get( uuid, User.class );
        if ( entity != null ) {
            OrgUser user = new OrgUser(
                entity.getUuid(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getCreated()
            );
            return user;
        }
        return null;
    }


    @Override
    public OrgUser lookupOrgUserByUsername(String username) throws Exception {
        UserInfo info = managementService.getAdminUserByUsername( username );
        return info == null ? null : getOrgUser( info.getUuid() );
    }


    @Override
    public OrgUser lookupOrgUserByEmail(String email) throws Exception {
        UserInfo info = managementService.getAdminUserByEmail( email );
        return info == null ? null : getOrgUser( info.getUuid() );
    }


    @Override
    public void removeOrgUser(OrgUser orgUser) throws Exception {
        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
        em.delete( new SimpleEntityRef( "user", orgUser.getId() ));
    }


    @Override
    public void updateOrgUser(OrgUser targetUserEntity ) throws Exception {
        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
        User user = em.get(targetUserEntity.getId(), User.class);
        user.setUsername( targetUserEntity.getUsername() );
        user.setEmail( targetUserEntity.getEmail() );
        em.update( user );
    }


    @Override
    public void setOrgUserName(OrgUser other, String newUserName ) throws Exception {

        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );

        logger.info( "Setting username to {} for user with username {} and id {}", new Object[] {
            newUserName, other.getUsername(), other.getId()
        } );

        try {
            em.setProperty( new SimpleEntityRef( "user", other.getId() ), "username", newUserName, true );
        }
        catch ( DuplicateUniquePropertyExistsException e ) {
            logger.warn( "More than 1 user has the username of {}.  Setting the username to their username+UUID as a "
                + "fallback", newUserName );

            setOrgUserName( other, String.format( "%s-%s", other.getUsername(), other.getId() ) );
        }
    }


    /**
     * Select best org from a set of duplicates by picking the one that is indexed, or the oldest.
     */
    @Override
    public Org selectBest(Set<Org> orgs) throws Exception {
        Org oldest = null;
        for ( Org org :orgs ) {
            OrganizationInfo info = managementService.getOrganizationByName( org.getName() );
            if ( info != null ) {
                return org;
            }
            if ( oldest == null || org.compareTo( oldest ) < 0 ) {
                oldest = org;
            }
        }
        return oldest;
    }
}
