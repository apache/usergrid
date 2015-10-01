/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.service.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.dao.ProviderParamsDao;
import org.apache.usergrid.chop.webapp.dao.UserDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicProviderParams;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ShiroRealm extends AuthorizingRealm {

    private static final Logger LOG = LoggerFactory.getLogger( ShiroRealm.class );

    private static final String DEFAULT_USER = "user";
    private static final String DEFAULT_PASSWORD = "pass";
    private static String authenticatedUser = "user";


    public ShiroRealm() {
        super( new MemoryConstrainedCacheManager(), new SimpleCredentialsMatcher() );
    }


    public static boolean authenticateUser( String username, String password ) {
        try {
            if ( !SecurityUtils.getSubject().isAuthenticated() ) {
                if ( username == null ) {
                    throw new AuthenticationException( "Username is null" );
                }
                if ( password == null ) {
                    throw new AuthenticationException( "Password is null" );
                }

                LOG.info( String.format( "Authenticating  user %s", username) );

                if ( username.equalsIgnoreCase( "user" ) && password.equals( "pass" ) ) {
                    initUserData();
                }
                User user = InjectorFactory.getInstance( UserDao.class ).get( username.toLowerCase() );
                if ( user == null || user.getPassword() == null || !user.getPassword().equalsIgnoreCase( password ) ) {
                    throw new AuthenticationException( "Authentication failed" );
                }

                SecurityUtils.getSubject().login( new UsernamePasswordToken( username, password ) );
                authenticatedUser = username;
            }
            return true;

        } catch ( Exception e ) {
            LOG.error( "Error in findUser", e );
        }
        return false;
    }


    @Override
    protected AuthenticationInfo doGetAuthenticationInfo( AuthenticationToken authenticationToken ) throws AuthenticationException {

        try {
            UsernamePasswordToken token = ( UsernamePasswordToken ) authenticationToken;
            token.setRememberMe( true );

            String username = token.getUsername();
            String password = String.valueOf( token.getPassword() );

            if ( username == null ) {
                throw new AuthenticationException( "Authentication failed" );
            }

            LOG.info( String.format( "Authenticating user %s", username ) );

            if ( username.equals( username ) && password.equals( "pass" ) ) {
                initUserData();

            }
            User user = InjectorFactory.getInstance( UserDao.class ).get( username.toLowerCase() );
            if ( user == null || user.getPassword() == null || !user.getPassword().equalsIgnoreCase( password ) ) {
                throw new AuthenticationException( "Authentication failed" );
            }

            return new SimpleAuthenticationInfo( username, password, this.getName() );
        } catch ( Exception e ) {
            LOG.error( "Error while authenticating", e );
            throw new AuthenticationException( "Authentication failed", e );
        }

    }


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo( PrincipalCollection principals ) {
        try {
            if ( principals == null ) {
                throw new AuthorizationException( "PrincipalCollection method argument cannot be null." );
            }

            Collection<String> principalsList = principals.byType( String.class );

            if ( principalsList.isEmpty() ) {
                throw new AuthorizationException( "Empty principals list!" );
            }

            String username = ( String ) principals.getPrimaryPrincipal();

            Set<String> roles = new HashSet<String>();
            roles.add( "role1" );

            LOG.info( String.format( "Authorizing user %s with roles %s", username, roles ) );

            return new SimpleAuthorizationInfo( roles );

        } catch ( Exception e ) {
            LOG.error( "Error while authorizing", e );
            throw new AuthorizationException( "Authorization failed", e );
        }
    }


    private static void initUserData() throws Exception {

        UserDao userDao = InjectorFactory.getInstance( UserDao.class );
        User user = userDao.get( DEFAULT_USER );

        if ( user != null ) {
            return;
        }

        InjectorFactory.getInstance( UserDao.class ).save( new User( DEFAULT_USER, DEFAULT_PASSWORD ) );
        InjectorFactory.getInstance( ProviderParamsDao.class ).save( new BasicProviderParams( DEFAULT_USER ) );
    }


    public static void logout(){
        SecurityUtils.getSubject().logout();
    }


    public static String getDefaultUser() {
        return DEFAULT_USER;
    }


    public static String getAuthenticatedUser() {
        return authenticatedUser;
    }

    public static void setAuthenticatedUser( String authenticatedUser ) {
        ShiroRealm.authenticatedUser = authenticatedUser;
    }

    public static boolean isAuthenticatedUserAdmin() {
        return ShiroRealm.getAuthenticatedUser().equals( getDefaultUser() );
    }
}
