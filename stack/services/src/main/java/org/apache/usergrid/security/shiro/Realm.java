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
package org.apache.usergrid.security.shiro;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.apache.usergrid.management.AccountCreationProps;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Results.Level;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.security.shiro.credentials.AccessTokenCredentials;
import org.apache.usergrid.security.shiro.credentials.AdminUserAccessToken;
import org.apache.usergrid.security.shiro.credentials.AdminUserPassword;
import org.apache.usergrid.security.shiro.credentials.ApplicationAccessToken;
import org.apache.usergrid.security.shiro.credentials.ApplicationUserAccessToken;
import org.apache.usergrid.security.shiro.credentials.ClientCredentials;
import org.apache.usergrid.security.shiro.credentials.OrganizationAccessToken;
import org.apache.usergrid.security.shiro.credentials.PrincipalCredentials;
import org.apache.usergrid.security.shiro.principals.AdminUserPrincipal;
import org.apache.usergrid.security.shiro.principals.ApplicationGuestPrincipal;
import org.apache.usergrid.security.shiro.principals.ApplicationPrincipal;
import org.apache.usergrid.security.shiro.principals.ApplicationUserPrincipal;
import org.apache.usergrid.security.shiro.principals.OrganizationPrincipal;
import org.apache.usergrid.security.shiro.principals.PrincipalIdentifier;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.TokenService;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import com.google.common.collect.HashBiMap;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_ALLOWED;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.apache.usergrid.security.shiro.utils.SubjectUtils.getPermissionFromPath;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;


public class Realm extends AuthorizingRealm {
    private static final Logger logger = LoggerFactory.getLogger( Realm.class );

    public final static String ROLE_SERVICE_ADMIN = "service-admin";
    public final static String ROLE_ADMIN_USER = "admin-user";
    public final static String ROLE_ORGANIZATION_ADMIN = "organization-admin";
    public final static String ROLE_APPLICATION_ADMIN = "application-admin";
    public final static String ROLE_APPLICATION_USER = "application-user";

    private EntityManagerFactory emf;
    private ManagementService management;
    private TokenService tokens;


    @Value( "${" + PROPERTIES_SYSADMIN_LOGIN_ALLOWED + "}" )
    private boolean superUserEnabled;
    @Value( "${" + AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_NAME + ":admin}" )
    private String superUser;


    public Realm() {
        setCredentialsMatcher( new AllowAllCredentialsMatcher() );
        setPermissionResolver( new CustomPermissionResolver() );
    }


    public Realm( CacheManager cacheManager ) {
        super( cacheManager );
        setCredentialsMatcher( new AllowAllCredentialsMatcher() );
        setPermissionResolver( new CustomPermissionResolver() );
    }


    public Realm( CredentialsMatcher matcher ) {
        super( new AllowAllCredentialsMatcher() );
        setPermissionResolver( new CustomPermissionResolver() );
    }


    public Realm( CacheManager cacheManager, CredentialsMatcher matcher ) {
        super( cacheManager, new AllowAllCredentialsMatcher() );
        setPermissionResolver( new CustomPermissionResolver() );
    }


    @Override
    public void setCredentialsMatcher( CredentialsMatcher credentialsMatcher ) {
        if ( !( credentialsMatcher instanceof AllowAllCredentialsMatcher ) ) {
            logger.debug( "Replacing {} with AllowAllCredentialsMatcher", credentialsMatcher );
            credentialsMatcher = new AllowAllCredentialsMatcher();
        }
        super.setCredentialsMatcher( credentialsMatcher );
    }


    @Override
    public void setPermissionResolver( PermissionResolver permissionResolver ) {
        if ( !( permissionResolver instanceof CustomPermissionResolver ) ) {
            logger.debug( "Replacing {} with AllowAllCredentialsMatcher", permissionResolver );
            permissionResolver = new CustomPermissionResolver();
        }
        super.setPermissionResolver( permissionResolver );
    }


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    @Autowired
    public void setManagementService( ManagementService management ) {
        this.management = management;
    }


    @Autowired
    public void setTokenService( TokenService tokens ) {
        this.tokens = tokens;
    }


    @Override
    protected AuthenticationInfo doGetAuthenticationInfo( AuthenticationToken token ) throws AuthenticationException {
        PrincipalCredentialsToken pcToken = ( PrincipalCredentialsToken ) token;

        if ( pcToken.getCredentials() == null ) {
            throw new CredentialsException( "Missing credentials" );
        }

        boolean authenticated = false;

        PrincipalIdentifier principal = pcToken.getPrincipal();
        PrincipalCredentials credentials = pcToken.getCredentials();

        if ( credentials instanceof ClientCredentials ) {
            authenticated = true;
        }
        else if ( ( principal instanceof AdminUserPrincipal ) && ( credentials instanceof AdminUserPassword ) ) {
            authenticated = true;
        }
        else if ( ( principal instanceof AdminUserPrincipal ) && ( credentials instanceof AdminUserAccessToken ) ) {
            authenticated = true;
        }
        else if ( ( principal instanceof ApplicationUserPrincipal )
                && ( credentials instanceof ApplicationUserAccessToken ) ) {
            authenticated = true;
        }
        else if ( ( principal instanceof ApplicationPrincipal ) && ( credentials instanceof ApplicationAccessToken ) ) {
            authenticated = true;
        }
        else if ( ( principal instanceof OrganizationPrincipal )
                && ( credentials instanceof OrganizationAccessToken ) ) {
            authenticated = true;
        }

        if ( principal != null ) {
            if ( !principal.isActivated() ) {
                throw new AuthenticationException( "Unactivated identity" );
            }
            if ( principal.isDisabled() ) {
                throw new AuthenticationException( "Disabled identity" );
            }
        }

        if ( !authenticated ) {
            throw new AuthenticationException( "Unable to authenticate" );
        }

        logger.debug( "Authenticated: {}", principal );

        SimpleAuthenticationInfo info =
                new SimpleAuthenticationInfo( pcToken.getPrincipal(), pcToken.getCredentials(), getName() );
        return info;
    }


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo( PrincipalCollection principals ) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        Map<UUID, String> organizationSet = HashBiMap.create();
        Map<UUID, String> applicationSet = HashBiMap.create();
        OrganizationInfo organization = null;
        ApplicationInfo application = null;

        for ( PrincipalIdentifier principal : principals.byType( PrincipalIdentifier.class ) ) {

            if ( principal instanceof OrganizationPrincipal ) {
                // OrganizationPrincipals are usually only through OAuth
                // They have access to a single organization

                organization = ( ( OrganizationPrincipal ) principal ).getOrganization();

                role( info, principal, ROLE_ORGANIZATION_ADMIN );
                role( info, principal, ROLE_APPLICATION_ADMIN );

                grant( info, principal, "organizations:access:" + organization.getUuid() );
                organizationSet.put( organization.getUuid(), organization.getName() );

                Map<UUID, String> applications = null;
                try {
                    applications = management.getApplicationsForOrganization( organization.getUuid() );
                }
                catch ( Exception e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if ( ( applications != null ) && !applications.isEmpty() ) {
                    grant( info, principal, "applications:admin,access,get,put,post,delete:" + StringUtils
                            .join( applications.keySet(), ',' ) );

                    applicationSet.putAll( applications );
                }
            }
            else if ( principal instanceof ApplicationPrincipal ) {
                // ApplicationPrincipal are usually only through OAuth
                // They have access to a single application

                role( info, principal, ROLE_APPLICATION_ADMIN );

                application = ( ( ApplicationPrincipal ) principal ).getApplication();
                grant( info, principal, "applications:admin,access,get,put,post,delete:" + application.getId() );
                applicationSet.put( application.getId(), application.getName() );
            }
            else if ( principal instanceof AdminUserPrincipal ) {
                // AdminUserPrincipals are through basic auth and sessions
                // They have access to organizations and organization
                // applications

                UserInfo user = principal.getUser();

                if ( superUserEnabled && ( superUser != null ) && superUser.equals( user.getUsername() ) ) {
                    // The system user has access to everything

                    role( info, principal, ROLE_SERVICE_ADMIN );
                    role( info, principal, ROLE_ORGANIZATION_ADMIN );
                    role( info, principal, ROLE_APPLICATION_ADMIN );
                    role( info, principal, ROLE_ADMIN_USER );

                    grant( info, principal, "system:access" );

                    grant( info, principal, "organizations:admin,access,get,put,post,delete:*" );
                    grant( info, principal, "applications:admin,access,get,put,post,delete:*" );
                    grant( info, principal, "organizations:admin,access,get,put,post,delete:*:/**" );
                    grant( info, principal, "applications:admin,access,get,put,post,delete:*:/**" );
                    grant( info, principal, "users:access:*" );

                    grant( info, principal, getPermissionFromPath( MANAGEMENT_APPLICATION_ID, "access" ) );

                    grant( info, principal,
                            getPermissionFromPath( MANAGEMENT_APPLICATION_ID, "get,put,post,delete", "/**" ) );
                }
                else {

                    // For regular service users, we find what organizations
                    // they're associated with
                    // An service user can be associated with multiple
                    // organizations

                    grant( info, principal, getPermissionFromPath( MANAGEMENT_APPLICATION_ID, "access" ) );

                    // admin users cannot access the management app directly
                    // so open all permissions
                    grant( info, principal,
                            getPermissionFromPath( MANAGEMENT_APPLICATION_ID, "get,put,post,delete", "/**" ) );

                    role( info, principal, ROLE_ADMIN_USER );

                    try {

                        Map<UUID, String> userOrganizations = management.getOrganizationsForAdminUser( user.getUuid() );

                        if ( userOrganizations != null ) {
                            for ( UUID id : userOrganizations.keySet() ) {
                                grant( info, principal, "organizations:admin,access,get,put,post,delete:" + id );
                            }
                            organizationSet.putAll( userOrganizations );

                            Map<UUID, String> userApplications =
                                    management.getApplicationsForOrganizations( userOrganizations.keySet() );
                            if ( ( userApplications != null ) && !userApplications.isEmpty() ) {
                                grant( info, principal, "applications:admin,access,get,put,post,delete:" + StringUtils
                                        .join( userApplications.keySet(), ',' ) );
                                applicationSet.putAll( userApplications );
                            }

                            role( info, principal, ROLE_ORGANIZATION_ADMIN );
                            role( info, principal, ROLE_APPLICATION_ADMIN );
                        }
                    }
                    catch ( Exception e ) {
                        logger.error( "Unable to construct admin user permissions", e );
                    }
                }
            }
            else if ( principal instanceof ApplicationUserPrincipal ) {

                role( info, principal, ROLE_APPLICATION_USER );

                UUID applicationId = ( ( ApplicationUserPrincipal ) principal ).getApplicationId();

                AccessTokenCredentials tokenCredentials =
                        principal.getAccessTokenCredentials();
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

                grant( info, principal, getPermissionFromPath( applicationId, "access" ) );

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
                    grant( info, principal, applicationId, permissions );
                }
                catch ( Exception e ) {
                    logger.error( "Unable to get user default role permissions", e );
                }

                UserInfo user = principal.getUser();
                try {
                    Set<String> permissions = em.getUserPermissions( user.getUuid() );
                    grant( info, principal, applicationId, permissions );
                }
                catch ( Exception e ) {
                    logger.error( "Unable to get user permissions", e );
                }

                try {
                    Set<String> rolenames = em.getUserRoles( user.getUuid() );
                    grantAppRoles( info, em, applicationId, token, principal, rolenames );
                }
                catch ( Exception e ) {
                    logger.error( "Unable to get user role permissions", e );
                }

                try {
                    //TODO TN.  This is woefully inefficient, but temporary.  Introduce cassandra backed shiro
                    // caching so this only ever happens once.
                    //See USERGRID-779 for details
                    Results r =
                            em.getCollection( new SimpleEntityRef( User.ENTITY_TYPE, user.getUuid() ), "groups", null,
                                    1000, Level.IDS, false );
                    if ( r != null ) {

                        Set<String> rolenames = new HashSet<String>();

                        for ( UUID groupId : r.getIds() ) {

                            Results roleResults =
                                    em.getCollection( new SimpleEntityRef( Group.ENTITY_TYPE, groupId ), "roles", null,
                                            1000, Level.CORE_PROPERTIES, false );

                            for ( Entity entity : roleResults.getEntities() ) {
                                rolenames.add( entity.getName() );
                            }
                        }


                        grantAppRoles( info, em, applicationId, token, principal, rolenames );
                    }
                }
                catch ( Exception e ) {
                    logger.error( "Unable to get user group role permissions", e );
                }
            }
            else if ( principal instanceof ApplicationGuestPrincipal ) {
                role( info, principal, ROLE_APPLICATION_USER );

                UUID applicationId = ( ( ApplicationGuestPrincipal ) principal ).getApplicationId();

                EntityManager em = emf.getEntityManager( applicationId );
                try {
                    String appName = ( String ) em.getProperty( em.getApplicationRef(), "name" );
                    applicationSet.put( applicationId, appName );
                    application = new ApplicationInfo( applicationId, appName );
                }
                catch ( Exception e ) {
                }

                grant( info, principal, getPermissionFromPath( applicationId, "access" ) );

                try {
                    Set<String> permissions = em.getRolePermissions( "guest" );
                    grant( info, principal, applicationId, permissions );
                }
                catch ( Exception e ) {
                    logger.error( "Unable to get user default role permissions", e );
                }
            }
        }

        // Store additional information in the request session to speed up
        // looking up organization info

        Subject currentUser = SecurityUtils.getSubject();
        Session session = currentUser.getSession();
        session.setAttribute( "applications", applicationSet );
        session.setAttribute( "organizations", organizationSet );
        if ( organization != null ) {
            session.setAttribute( "organization", organization );
        }
        if ( application != null ) {
            session.setAttribute( "application", application );
        }

        return info;
    }


    /** Grant all permissions for the role names on this application */
    private void grantAppRoles( SimpleAuthorizationInfo info, EntityManager em, UUID applicationId, TokenInfo token,
                                PrincipalIdentifier principal, Set<String> rolenames ) throws Exception {
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
            grant( info, principal, applicationId, permissions );
            role( info, principal,
                    "application-role:".concat( applicationId.toString() ).concat( ":" ).concat( rolename ) );
        }
    }


    public static void grant( SimpleAuthorizationInfo info, PrincipalIdentifier principal, String permission ) {
        logger.debug( "Principal {} granted permission: {}", principal, permission );
        info.addStringPermission( permission );
    }


    public static void role( SimpleAuthorizationInfo info, PrincipalIdentifier principal, String role ) {
        logger.debug( "Principal {} added to role: {}", principal, role );
        info.addRole( role );
    }


    private static void grant( SimpleAuthorizationInfo info, PrincipalIdentifier principal, UUID applicationId,
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
                    grant( info, principal, permission );
                }
            }
        }
    }


    @Override
    public boolean supports( AuthenticationToken token ) {
        return token instanceof PrincipalCredentialsToken;
    }
}
