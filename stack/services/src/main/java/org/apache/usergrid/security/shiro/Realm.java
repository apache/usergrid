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


import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.usergrid.management.AccountCreationProps;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.shiro.credentials.*;
import org.apache.usergrid.security.shiro.principals.*;
import org.apache.usergrid.security.tokens.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_ALLOWED;


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
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
        setPermissionResolver(new CustomPermissionResolver());
    }


    public Realm( CacheManager cacheManager ) {
        super( cacheManager );
        setCredentialsMatcher( new AllowAllCredentialsMatcher() );
        setPermissionResolver(new CustomPermissionResolver());
        setCachingEnabled(true);
        setAuthenticationCachingEnabled(true);
    }


    public Realm( CredentialsMatcher matcher ) {
        super(new AllowAllCredentialsMatcher());
        setPermissionResolver(new CustomPermissionResolver());
    }


    public Realm( CacheManager cacheManager, CredentialsMatcher matcher ) {
        super(cacheManager, new AllowAllCredentialsMatcher());
        setPermissionResolver( new CustomPermissionResolver() );
        setCachingEnabled(true);
        setAuthenticationCachingEnabled(true);
    }


    @Override
    public void setCredentialsMatcher( CredentialsMatcher credentialsMatcher ) {
        if ( !( credentialsMatcher instanceof AllowAllCredentialsMatcher ) ) {
            logger.debug( "Replacing {} with AllowAllCredentialsMatcher", credentialsMatcher );
            credentialsMatcher = new AllowAllCredentialsMatcher();
        }
        super.setCredentialsMatcher(credentialsMatcher);
    }


    @Override
    public void setPermissionResolver( PermissionResolver permissionResolver ) {
        if ( !( permissionResolver instanceof CustomPermissionResolver ) ) {
            logger.debug( "Replacing {} with AllowAllCredentialsMatcher", permissionResolver );
            permissionResolver = new CustomPermissionResolver();
        }
        super.setPermissionResolver(permissionResolver);
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
    protected AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {
        UsergridAuthorizationInfo info = (UsergridAuthorizationInfo)super.getAuthorizationInfo(principals);

        Subject currentUser = SecurityUtils.getSubject();
        Session session = currentUser.getSession();
        session.setAttribute( "applications", info.getApplicationSet());
        session.setAttribute("organizations", info.getOrganizationSet());
        if ( info.getOrganization() != null ) {
            session.setAttribute( "organization", info.getOrganization() );
        }
        if ( info.getApplication() != null ) {
            session.setAttribute( "application", info.getApplication() );
        }

        return info;
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
        UsergridAuthorizationInfo info = new UsergridAuthorizationInfo();

        for ( PrincipalIdentifier principal : principals.byType( PrincipalIdentifier.class ) ) {
            principal.grant( info, emf, management, tokens );
        }

        return info;
    }


    @Override
    public boolean isAuthorizationCachingEnabled() {
        return getCacheManager() != null;
    }

    public boolean isAuthenticationCachingEnabled() {
        return getCacheManager() != null;
    }

    @Override
    public boolean isCachingEnabled() {
        return getCacheManager() != null;
    }

    @Override
    public void setCacheManager(CacheManager cacheManager) {
        super.setCacheManager(cacheManager);
    }

    @Override
    public CacheManager getCacheManager() {
        return super.getCacheManager();
    }

    @Override
    public boolean supports( AuthenticationToken token ) {
        return token instanceof PrincipalCredentialsToken;
    }
}
