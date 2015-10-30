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
package org.apache.usergrid.security.shiro.principals;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.HashBiMap;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.security.shiro.Realm;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.apache.usergrid.security.shiro.credentials.AccessTokenCredentials;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.usergrid.security.shiro.utils.SubjectUtils.getPermissionFromPath;


public class ApplicationUserPrincipal extends UserPrincipal {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserPrincipal.class);

    public ApplicationUserPrincipal() {}

    public ApplicationUserPrincipal( UUID applicationId, UserInfo user ) {
        super( applicationId, user );
    }


    @Override
    public String toString() {
        return "user[" + user.getApplicationId() + "/" + user.getUuid() + "]";
    }


    @Override
    public void grant(
        UsergridAuthorizationInfo info,
        EntityManagerFactory emf,
        ManagementService management,
        TokenService tokens) {

        Map<UUID, String> organizationSet = HashBiMap.create();
        Map<UUID, String> applicationSet = HashBiMap.create();
        OrganizationInfo organization = null;
        ApplicationInfo application = null;


        role( info, Realm.ROLE_APPLICATION_USER );

        UUID applicationId = getApplicationId();

        AccessTokenCredentials tokenCredentials = getAccessTokenCredentials();
        TokenInfo token = null;
        if ( tokenCredentials != null ) {
            try {
                token = tokens.getTokenInfo( tokenCredentials.getToken() );
            }
            catch ( Exception e ) {
                logger.error( "Unable to retrieve token info", e );
            }
            logger.debug( "Token: {}", token );
        }

        grant( info, getPermissionFromPath( applicationId, "access" ) );

                /*
                 * grant(info, principal, getPermissionFromPath(applicationId,
                 * "get,put,post,delete", "/users/${user}",
                 * "/users/${user}/feed", "/users/${user}/activities",
                 * "/users/${user}/groups", "/users/${user}/following/*",
                 * "/users/${user}/following/user/*"));
                 */

        EntityManager em = emf.getEntityManager( applicationId );
        try {
            String appName = ( String ) em.getProperty( em.getApplicationRef(), "name" );
            applicationSet.put( applicationId, appName );
            application = new ApplicationInfo( applicationId, appName );
        }
        catch ( Exception e ) {
        }

        try {
            Set<String> permissions = em.getRolePermissions( "default" );
            grant( info, applicationId, permissions );
        }
        catch ( Exception e ) {
            logger.error( "Unable to get user default role permissions", e );
        }

        UserInfo user = getUser();
        try {
            Set<String> permissions = em.getUserPermissions( user.getUuid() );
            grant( info, applicationId, permissions );
        }
        catch ( Exception e ) {
            logger.error( "Unable to get user permissions", e );
        }

        try {
            Set<String> rolenames = em.getUserRoles( user.getUuid() );
            grantAppRoles( info, em, applicationId, token, rolenames );
        }
        catch ( Exception e ) {
            logger.error( "Unable to get user role permissions", e );
        }

        try {

            // this is woefully inefficient, thankfully we cache the info object now

            Results r =
                em.getCollection( new SimpleEntityRef( User.ENTITY_TYPE, user.getUuid() ), "groups", null,
                    1000, Query.Level.IDS, false );
            if ( r != null ) {

                Set<String> rolenames = new HashSet<String>();

                for ( UUID groupId : r.getIds() ) {

                    Results roleResults =
                        em.getCollection( new SimpleEntityRef( Group.ENTITY_TYPE, groupId ), "roles", null,
                            1000, Query.Level.CORE_PROPERTIES, false );

                    for ( Entity entity : roleResults.getEntities() ) {
                        rolenames.add( entity.getName() );
                    }
                }


                grantAppRoles( info, em, applicationId, token, rolenames );
            }
        }
        catch ( Exception e ) {
            logger.error( "Unable to get user group role permissions", e );
        }

        info.setOrganization(organization);
        info.addOrganizationSet(organizationSet);
        info.setApplication(application);
        info.addApplicationSet(applicationSet);
    }
}
