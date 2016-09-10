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
import org.apache.shiro.subject.Subject;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.rest.exceptions.SecurityException;
import org.apache.usergrid.security.shiro.PrincipalCredentialsToken;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.security.Principal;
import java.util.Map;

import static org.apache.usergrid.rest.exceptions.AuthErrorInfo.INVALID_CLIENT_CREDENTIALS_ERROR;
import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;
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
        if(logger.isTraceEnabled()){
            logger.trace("Filtering: {}", request.getUriInfo().getBaseUri());
        }

        if( bypassSecurityCheck(request) ){
            return;
        }

        Map<String, String> auth_types = getAuthTypes( request );
        if ( ( auth_types == null ) || !auth_types.containsKey( AUTH_BASIC_TYPE ) ) {
            return;
        }
        String[] values = Base64.decodeToString( auth_types.get( AUTH_BASIC_TYPE ) ).split( ":" );
        if ( values.length < 2 ) {
            return;
        }
        String name = values[0];
        String password = values[1];

        String sysadmin_login_name = properties.getProperty( "usergrid.sysadmin.login.name" );
        String sysadmin_login_password = properties.getProperty( "usergrid.sysadmin.login.password" );
        boolean sysadmin_login_allowed =
                Boolean.parseBoolean( properties.getProperty( "usergrid.sysadmin.login.allowed" ) );
            if ( name.equalsIgnoreCase( sysadmin_login_name ) && sysadmin_login_allowed ) {

            // short cut with a password check against the configured property
            if( !password.equals( sysadmin_login_password ) ){

                throw mappableSecurityException( "unauthorized", "No system access authorized",
                    SecurityException.REALM );

            }

            try {
                UserInfo userInfo = management.verifyAdminUserPasswordCredentials(name.toLowerCase(), password);
                PrincipalCredentialsToken token = PrincipalCredentialsToken
                        .getFromAdminUserInfoAndPassword(userInfo, password, emf.getManagementAppId());
                Subject subject = SubjectUtils.getSubject();
                subject.login( token );

                if (logger.isTraceEnabled()) {
                    logger.trace("System administrator access allowed");
                }

            } catch (Exception e) {
                logger.error("Unable to validate admin credentials");
                throw mappableSecurityException( "unauthorized", "No system access authorized",
                    SecurityException.REALM );
            }


        }
        // only allow client credentials with http basic auth other than the sysadmin
        else{

            try {
                PrincipalCredentialsToken token =
                    management.getPrincipalCredentialsTokenForClientCredentials( name, password );
                Subject subject = SubjectUtils.getSubject();
                subject.login( token );
            }
            catch ( Exception e ) {
                throw mappableSecurityException( INVALID_CLIENT_CREDENTIALS_ERROR );
            }


        }
    }

}
