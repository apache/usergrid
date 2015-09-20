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


import org.apache.shiro.subject.Subject;
import org.apache.usergrid.security.shiro.PrincipalCredentialsToken;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.rest.exceptions.AuthErrorInfo.OAUTH2_INVALID_CLIENT;
import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;


@Provider
@PreMatching
public class ClientCredentialsSecurityFilter extends SecurityFilter {

    private static final Logger logger = LoggerFactory.getLogger( ClientCredentialsSecurityFilter.class );

    @Context
    protected HttpServletRequest httpServletRequest;


    public ClientCredentialsSecurityFilter() {
        logger.info( "ClientCredentialsSecurityFilter is installed" );
    }


    @Override
    public void filter( ContainerRequestContext request ) {
        logger.debug("Filtering: " + request.getUriInfo().getBaseUri());

        String clientId = httpServletRequest.getParameter( "client_id" );
        String clientSecret = httpServletRequest.getParameter( "client_secret" );

        if ( isNotBlank( clientId ) && isNotBlank( clientSecret ) ) {
            try {
                PrincipalCredentialsToken token =
                        management.getPrincipalCredentialsTokenForClientCredentials( clientId, clientSecret );
                Subject subject = SubjectUtils.getSubject();
                subject.login( token );
            }
            catch ( Exception e ) {
                throw mappableSecurityException( OAUTH2_INVALID_CLIENT );
            }
        }
    }
}
