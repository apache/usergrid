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


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.exceptions.ManagementException;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.shiro.PrincipalCredentialsToken;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;
import org.apache.usergrid.security.tokens.exceptions.ExpiredTokenException;
import org.apache.usergrid.security.tokens.exceptions.InvalidTokenException;

import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.ParameterStyle;
import org.apache.amber.oauth2.common.utils.OAuthUtils;
import org.apache.amber.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.shiro.subject.Subject;

import com.sun.jersey.api.container.MappableContainerException;
import com.sun.jersey.spi.container.ContainerRequest;
import org.apache.commons.lang.StringUtils;

import static org.apache.usergrid.rest.exceptions.AuthErrorInfo.BAD_ACCESS_TOKEN_ERROR;
import static org.apache.usergrid.rest.exceptions.AuthErrorInfo.EXPIRED_ACCESS_TOKEN_ERROR;
import static org.apache.usergrid.rest.exceptions.AuthErrorInfo.INVALID_AUTH_ERROR;
import static org.apache.usergrid.rest.exceptions.AuthErrorInfo.UNVERIFIED_OAUTH_ERROR;
import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;


@Component
public class OAuth2AccessTokenSecurityFilter extends SecurityFilter {

    public static final String REALM = "Usergrid Authentication";

    private static final Logger LOG = LoggerFactory.getLogger( OAuth2AccessTokenSecurityFilter.class );


    public OAuth2AccessTokenSecurityFilter() {
        LOG.info( "OAuth2AccessTokenSecurityFilter is installed" );
    }


    @Context
    protected HttpServletRequest httpServletRequest;


    @Override
    public ContainerRequest filter( ContainerRequest request ) {

        try {
            try {

                String accessToken = request.getQueryParameters().getFirst( "access_token" );

                if ( StringUtils.isEmpty(accessToken) ) {

                    // Make the OAuth Request out of this request
                    OAuthAccessResourceRequest oauthRequest =
                            new OAuthAccessResourceRequest( httpServletRequest, ParameterStyle.HEADER );

                    // Get the access token
                    accessToken = oauthRequest.getAccessToken();
                }

                if ( StringUtils.isEmpty(accessToken) ) {
                    return request;
                }

                AuthPrincipalInfo principal = null;
                try {
                    TokenInfo tokenInfo = tokens.getTokenInfo( accessToken );
                    principal = tokenInfo.getPrincipal();
                }
                catch ( BadTokenException e1 ) {
                    throw mappableSecurityException( BAD_ACCESS_TOKEN_ERROR );
                }
                catch ( ExpiredTokenException ete ) {
                    throw mappableSecurityException( EXPIRED_ACCESS_TOKEN_ERROR );
                }
                catch ( InvalidTokenException ite ) {
                    throw mappableSecurityException( INVALID_AUTH_ERROR );
                }
                catch ( Exception e ) {
                    if ( LOG.isDebugEnabled() ) {
                        LOG.debug( "Unable to verify OAuth token: " + accessToken, e );
                    } else {
                        LOG.warn( "Unable to verify OAuth token" );
                    }
                    throw mappableSecurityException( UNVERIFIED_OAUTH_ERROR );
                }

                if ( principal == null ) {
                    return request;
                }

                PrincipalCredentialsToken token = null;

                if ( AuthPrincipalType.ADMIN_USER.equals( principal.getType() ) ) {

                    UserInfo user = null;
                    try {
                        user = management.getAdminUserInfoFromAccessToken( accessToken );
                    }
                    catch ( ManagementException e ) {
                        throw new MappableContainerException( e );
                    }
                    catch ( Exception e ) {
                        LOG.error( "failed to get admin user info from access token", e );
                    }
                    if ( user == null ) {
                        throw mappableSecurityException( BAD_ACCESS_TOKEN_ERROR );
                    }

                    token = PrincipalCredentialsToken.getFromAdminUserInfoAndAccessToken(
                            user, accessToken, emf.getManagementAppId() );
                }
                else if ( AuthPrincipalType.APPLICATION_USER.equals( principal.getType() ) ) {

                    UserInfo user = null;
                    try {
                        user = management.getAppUserFromAccessToken( accessToken );
                    }
                    catch ( ManagementException e ) {
                        throw new MappableContainerException( e );
                    }
                    catch ( Exception e ) {
                        LOG.error( "failed to get app user from access token", e );
                    }
                    if ( user == null ) {
                        throw mappableSecurityException( BAD_ACCESS_TOKEN_ERROR );
                    }

                    token = PrincipalCredentialsToken.getFromAppUserInfoAndAccessToken( user, accessToken );
                }
                else if ( AuthPrincipalType.ORGANIZATION.equals( principal.getType() ) ) {

                    OrganizationInfo organization = null;
                    try {
                        organization = management.getOrganizationInfoFromAccessToken( accessToken );
                    }
                    catch ( ManagementException e ) {
                        throw new MappableContainerException( e );
                    }
                    catch ( Exception e ) {
                        LOG.error( "failed to get organization info from access token", e );
                    }
                    if ( organization == null ) {
                        throw mappableSecurityException( BAD_ACCESS_TOKEN_ERROR );
                    }

                    token = PrincipalCredentialsToken
                            .getFromOrganizationInfoAndAccessToken( organization, accessToken );
                }
                else if ( AuthPrincipalType.APPLICATION.equals( principal.getType() ) ) {

                    ApplicationInfo application = null;
                    try {
                        application = management.getApplicationInfoFromAccessToken( accessToken );
                    }
                    catch ( ManagementException e ) {
                        throw new MappableContainerException( e );
                    }
                    catch ( Exception e ) {
                        LOG.error( "failed to get application info from access token", e );
                    }
                    if ( application == null ) {
                        throw mappableSecurityException( BAD_ACCESS_TOKEN_ERROR );
                    }

                    token = PrincipalCredentialsToken.getFromApplicationInfoAndAccessToken( application, accessToken );
                }

                Subject subject = SubjectUtils.getSubject();
                subject.login( token );
            }
            catch ( OAuthProblemException e ) {
                // Check if the error code has been set
                String errorCode = e.getError();
                if ( OAuthUtils.isEmpty( errorCode ) ) {

                    return request;
                }

                throw new MappableContainerException( e );
            }
        }
        catch ( OAuthSystemException ose ) {
            throw new MappableContainerException( ose );
        }

        return request;
    }
}
