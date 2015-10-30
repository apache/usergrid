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
package org.apache.usergrid.rest.security.shiro.filters;


import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.security.Principal;
import java.util.Map;

import static org.apache.usergrid.security.shiro.Realm.ROLE_SERVICE_ADMIN;


@Provider
@PreMatching
public class BasicAuthSecurityFilter extends SecurityFilter {

    private static final Logger logger = LoggerFactory.getLogger( BasicAuthSecurityFilter.class );


    public BasicAuthSecurityFilter() {
        logger.info( "BasicAuthSecurityFilter is installed" );
    }


    @Override
    public void filter( ContainerRequestContext request ) {
        logger.info("Filtering: " + request.getUriInfo().getBaseUri());

        Map<String, String> auth_types = getAuthTypes( request );
        if ( ( auth_types == null ) || !auth_types.containsKey( AUTH_BASIC_TYPE ) ) {
            return;
        }
        String[] values = Base64.decodeToString( auth_types.get( AUTH_BASIC_TYPE ) ).split( ":" );
        if ( values.length < 2 ) {
            return;
        }
        String name = values[0].toLowerCase();
        String password = values[1];

        String sysadmin_login_name = properties.getProperty( "usergrid.sysadmin.login.name" );
        String sysadmin_login_password = properties.getProperty( "usergrid.sysadmin.login.password" );
        boolean sysadmin_login_allowed =
                Boolean.parseBoolean( properties.getProperty( "usergrid.sysadmin.login.allowed" ) );
        if ( name.equals( sysadmin_login_name ) && password.equals( sysadmin_login_password )
                && sysadmin_login_allowed ) {
            request.setSecurityContext( new SysAdminRoleAuthenticator() );
            logger.debug( "System administrator access allowed" );
        }
    }

    private static class SysAdminRoleAuthenticator implements SecurityContext {

        private final Principal principal;


        SysAdminRoleAuthenticator() {
            principal = new Principal() {
                @Override
                public String getName() {
                    return ROLE_SERVICE_ADMIN;
                }
            };
        }


        @Override
        public Principal getUserPrincipal() {
            return principal;
        }


        @Override
        public boolean isUserInRole( String role ) {
            return role.equals( ROLE_SERVICE_ADMIN );
        }


        @Override
        public boolean isSecure() {
            return false;
        }


        @Override
        public String getAuthenticationScheme() {
            return SecurityContext.BASIC_AUTH;
        }
    }
}
