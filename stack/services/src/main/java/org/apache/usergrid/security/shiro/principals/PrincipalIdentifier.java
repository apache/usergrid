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


import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.apache.usergrid.security.shiro.credentials.AccessTokenCredentials;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;


public abstract class PrincipalIdentifier {

    private static final Logger logger = LoggerFactory.getLogger(PrincipalIdentifier.class);

    AccessTokenCredentials accessTokenCredentials;


    public UserInfo getUser() {
        return null;
    }


    public boolean isDisabled() {
        return false;
    }


    public boolean isActivated() {
        return true;
    }


    public AccessTokenCredentials getAccessTokenCredentials() {
        return accessTokenCredentials;
    }


    public void setAccessTokenCredentials( AccessTokenCredentials accessTokenCredentials ) {
        this.accessTokenCredentials = accessTokenCredentials;
    }

    /** Return application UUID or null if none is associated with this prinicipal */

    public abstract UUID getApplicationId();

    public abstract void grant(
        UsergridAuthorizationInfo info,
        EntityManagerFactory emf,
        ManagementService management,
        TokenService tokens);


    protected void grant( UsergridAuthorizationInfo info, String permission ) {
        logger.debug( "Principal {} granted permission: {}", this, permission );
        info.addStringPermission(permission);
    }


    protected void role( UsergridAuthorizationInfo info, String role ) {
        logger.debug( "Principal {} added to role: {}", this, role );
        info.addRole(role);
    }


    protected void grant( UsergridAuthorizationInfo info, UUID applicationId,
                               Set<String> permissions ) {
        if ( permissions != null ) {
            for ( String permission : permissions ) {
                if ( isNotBlank( permission ) ) {
                    String operations = "*";
                    if ( permission.indexOf( ':' ) != -1 ) {
                        operations = stringOrSubstringBeforeFirst( permission, ':' );
                    }
                    if ( isBlank( operations ) ) {
                        operations = "*";
                    }
                    permission = stringOrSubstringAfterFirst( permission, ':' );
                    permission = "applications:" + operations + ":" + applicationId + ":" + permission;
                    grant( info, permission );
                }
            }
        }
    }

    /** Grant all permissions for the role names on this application */
    protected void grantAppRoles(
        UsergridAuthorizationInfo info,
        EntityManager em, UUID applicationId,
        TokenInfo token,
        Set<String> rolenames ) throws Exception {

        Map<String, Role> app_roles = em.getRolesWithTitles( rolenames );

        for ( String rolename : rolenames ) {
            if ( ( app_roles != null ) && ( token != null ) ) {
                Role role = app_roles.get( rolename );
                if ( ( role != null ) && ( role.getInactivity() > 0 ) && ( token.getInactive() > role
                    .getInactivity() ) ) {
                    continue;
                }
            }
            Set<String> permissions = em.getRolePermissions( rolename );
            grant( info, applicationId, permissions );
            role( info, "application-role:".concat( applicationId.toString() ).concat( ":" ).concat( rolename ) );
        }
    }
}
