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
package org.apache.usergrid.security.shiro.utils;


import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.security.shiro.PrincipalCredentialsToken;
import org.apache.usergrid.security.shiro.principals.UserPrincipal;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

import com.google.common.collect.BiMap;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.apache.usergrid.persistence.index.query.Identifier;
import static org.apache.usergrid.security.shiro.Realm.ROLE_ADMIN_USER;
import static org.apache.usergrid.security.shiro.Realm.ROLE_APPLICATION_ADMIN;
import static org.apache.usergrid.security.shiro.Realm.ROLE_APPLICATION_USER;
import static org.apache.usergrid.security.shiro.Realm.ROLE_ORGANIZATION_ADMIN;
import static org.apache.usergrid.security.shiro.Realm.ROLE_SERVICE_ADMIN;


public class SubjectUtils {

    private static final Logger logger = LoggerFactory.getLogger( SubjectUtils.class );


    public static boolean isAnonymous() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return true;
        }
        return !currentUser.isAuthenticated() && !currentUser.isRemembered();
    }


    public static boolean isOrganizationAdmin() {
        if ( isServiceAdmin() ) {
            return true;
        }
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        return currentUser.hasRole( ROLE_ORGANIZATION_ADMIN );
    }


    public static BiMap<UUID, String> getOrganizations() {
        Subject currentUser = getSubject();
        if ( !isOrganizationAdmin() ) {
            return null;
        }
        Session session = currentUser.getSession();
        BiMap<UUID, String> organizations = HashBiMap.create();
        Map map = (Map)session.getAttribute( "organizations" );
        organizations.putAll(map);
        return organizations;
    }


    public static boolean isPermittedAccessToOrganization( Identifier identifier ) {
        if ( isServiceAdmin() ) {
            return true;
        }
        OrganizationInfo organization = getOrganization( identifier );
        if ( organization == null ) {
            return false;
        }
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        return currentUser.isPermitted( "organizations:access:" + organization.getUuid() );
    }


    public static OrganizationInfo getOrganization( Identifier identifier ) {
        if ( identifier == null ) {
            return null;
        }
        UUID organizationId = null;
        String organizationName = null;
        BiMap<UUID, String> organizations = getOrganizations();
        if ( organizations == null ) {
            return null;
        }
        if ( identifier.isName() ) {
            organizationName = identifier.getName().toLowerCase();
            organizationId = organizations.inverse().get( organizationName );
        }
        else if ( identifier.isUUID() ) {
            organizationId = identifier.getUUID();
            organizationName = organizations.get( organizationId );
        }
        if ( ( organizationId != null ) && ( organizationName != null ) ) {
            return new OrganizationInfo( organizationId, organizationName );
        }
        return null;
    }


    public static OrganizationInfo getOrganization() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return null;
        }
        if ( !currentUser.hasRole( ROLE_ORGANIZATION_ADMIN ) ) {
            return null;
        }
        Session session = currentUser.getSession();
        OrganizationInfo organization = ( OrganizationInfo ) session.getAttribute( "organization" );
        return organization;
    }


    public static String getOrganizationName() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return null;
        }
        if ( !currentUser.hasRole( ROLE_ORGANIZATION_ADMIN ) ) {
            return null;
        }
        Session session = currentUser.getSession();
        OrganizationInfo organization = ( OrganizationInfo ) session.getAttribute( "organization" );
        if ( organization == null ) {
            return null;
        }
        return organization.getName();
    }


    public static UUID getOrganizationId() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return null;
        }
        if ( !currentUser.hasRole( ROLE_ORGANIZATION_ADMIN ) ) {
            return null;
        }
        Session session = currentUser.getSession();
        OrganizationInfo organization = ( OrganizationInfo ) session.getAttribute( "organization" );
        if ( organization == null ) {
            return null;
        }
        return organization.getUuid();
    }


    public static Set<String> getOrganizationNames() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return null;
        }
        if ( !currentUser.hasRole( ROLE_ORGANIZATION_ADMIN ) ) {
            return null;
        }
        BiMap<UUID, String> organizations = getOrganizations();
        if ( organizations == null ) {
            return null;
        }
        return organizations.inverse().keySet();
    }


    public static boolean isApplicationAdmin() {
        if ( isServiceAdmin() ) {
            return true;
        }
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        boolean admin = currentUser.hasRole( ROLE_APPLICATION_ADMIN );
        return admin;
    }


    public static boolean isPermittedAccessToApplication( Identifier identifier ) {
        if ( isServiceAdmin() ) {
            return true;
        }
        ApplicationInfo application = getApplication( identifier );
        if ( application == null ) {
            return false;
        }
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        return currentUser.isPermitted( "applications:access:" + application.getId() );
    }


    public static boolean isApplicationAdmin( Identifier identifier ) {
        if ( isServiceAdmin() ) {
            return true;
        }
        ApplicationInfo application = getApplication( identifier );
        if ( application == null ) {
            return false;
        }
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        return currentUser.isPermitted( "applications:admin:" + application.getId() );
    }


    public static ApplicationInfo getApplication( Identifier identifier ) {
        if ( identifier == null ) {
            return null;
        }
        if ( !isApplicationAdmin() && !isApplicationUser() ) {
            return null;
        }
        String applicationName = null;
        UUID applicationId = null;
        BiMap<UUID, String> applications = getApplications();

        if ( applications == null ) {
            return null;
        }
        if ( identifier.isName() ) {
            applicationName = identifier.getName().toLowerCase();
            applicationId = applications.inverse().get( applicationName );
        }
        else if ( identifier.isUUID() ) {
            applicationId = identifier.getUUID();
            applicationName = applications.get( identifier.getUUID() );
        }
        if ( ( applicationId != null ) && ( applicationName != null ) ) {
            return new ApplicationInfo( applicationId, applicationName );
        }
        return null;
    }


    @SuppressWarnings( "unchecked" )
    public static BiMap<UUID, String> getApplications() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return null;
        }
        if ( !currentUser.hasRole( ROLE_APPLICATION_ADMIN ) && !currentUser.hasRole( ROLE_APPLICATION_USER ) ) {
            return null;
        }
        Session session = currentUser.getSession();

        BiMap<UUID, String> applications = HashBiMap.create();
        Map map = (Map)session.getAttribute( "applications" );
        applications.putAll(map);
        return applications;
    }


    public static boolean isServiceAdmin() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        return currentUser.hasRole( ROLE_SERVICE_ADMIN );
    }


    public static boolean isApplicationUser() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        return currentUser.hasRole( ROLE_APPLICATION_USER );
    }


    public static boolean isAdminUser() {
        if ( isServiceAdmin() ) {
            return true;
        }
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        return currentUser.hasRole( ROLE_ADMIN_USER );
    }


    public static UserInfo getUser() {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return null;
        }
        if ( !( currentUser.getPrincipal() instanceof UserPrincipal ) ) {
            return null;
        }
        UserPrincipal principal = ( UserPrincipal ) currentUser.getPrincipal();
        return principal.getUser();
    }


    public static UserInfo getAdminUser() {
        UserInfo user = getUser();
        if ( user == null ) {
            return null;
        }
        return user.isAdminUser() ? user : null;
    }


    public static UUID getSubjectUserId() {

        UserInfo info = getUser();
        if ( info == null ) {
            return null;
        }

        return info.getUuid();
    }


    public static boolean isUserActivated() {
        UserInfo user = getUser();
        if ( user == null ) {
            return false;
        }
        return user.isActivated();
    }


    public static boolean isUserEnabled() {
        UserInfo user = getUser();
        if ( user == null ) {
            return false;
        }
        return !user.isDisabled();
    }


    public static boolean isUser( Identifier identifier ) {
        if ( identifier == null ) {
            return false;
        }
        UserInfo user = getUser();
        if ( user == null ) {
            return false;
        }
        if ( identifier.isUUID() ) {
            return user.getUuid().equals( identifier.getUUID() );
        }
        if ( identifier.isEmail() ) {
            return user.getEmail().equalsIgnoreCase( identifier.getEmail() );
        }
        if ( identifier.isName() ) {
            return user.getUsername().equals( identifier.getName() );
        }
        return false;
    }


    public static boolean isPermittedAccessToUser( UUID userId ) {
        if ( isServiceAdmin() ) {
            return true;
        }
        if ( userId == null ) {
            return false;
        }
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return false;
        }
        return currentUser.isPermitted( "users:access:" + userId );
    }


    public static String getPermissionFromPath( UUID applicationId, String operations, String... paths ) {
        String permission = "applications:" + operations + ":" + applicationId;
        String p = StringUtils.join( paths, ',' );
        permission += ( isNotBlank( p ) ? ":" + p : "" );
        return permission;
    }


    public static Subject getSubject() {
        Subject currentUser = null;
        try {
            currentUser = SecurityUtils.getSubject();
        }
        catch ( UnavailableSecurityManagerException e ) {
            logger.error( "getSubject(): Attempt to use Shiro prior to initialization" );
        }
        return currentUser;
    }


    public static void checkPermission( String permission ) {
        Subject currentUser = getSubject();
        if ( currentUser == null ) {
            return;
        }
        try {
            currentUser.checkPermission( permission );
        }
        catch ( org.apache.shiro.authz.UnauthenticatedException e ) {
            logger.debug( "checkPermission(): Subject is anonymous" );
        }
    }


    public static void loginApplicationGuest( ApplicationInfo application ) {
        if ( application == null ) {
            logger.error( "loginApplicationGuest(): Null application" );
            return;
        }
        if ( isAnonymous() ) {
            Subject subject = SubjectUtils.getSubject();
            PrincipalCredentialsToken token =
                    PrincipalCredentialsToken.getGuestCredentialsFromApplicationInfo( application );
            subject.login( token );
        }
        else {
            logger.error( "loginApplicationGuest(): Logging in non-anonymous user as guest not allowed" );
        }
    }
}
