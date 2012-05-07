/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.security.shiro.filters;

import static org.usergrid.rest.exceptions.AuthErrorInfo.BAD_ACCESS_TOKEN_ERROR;
import static org.usergrid.rest.exceptions.SecurityException.mappableSecurityException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.ParameterStyle;
import org.apache.amber.oauth2.common.utils.OAuthUtils;
import org.apache.amber.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.management.exceptions.ManagementException;
import org.usergrid.security.AuthPrincipalInfo;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.security.tokens.exceptions.BadTokenException;

import com.sun.jersey.api.container.MappableContainerException;
import com.sun.jersey.spi.container.ContainerRequest;

@Component
public class OAuth2AccessTokenSecurityFilter extends SecurityFilter {

	public static final String REALM = "Usergrid Authentication";

	private static final Logger logger = LoggerFactory
			.getLogger(OAuth2AccessTokenSecurityFilter.class);

	public OAuth2AccessTokenSecurityFilter() {
		logger.info("OAuth2AccessTokenSecurityFilter is installed");
	}

	@Context
	protected HttpServletRequest httpServletRequest;

	@Override
	public ContainerRequest filter(ContainerRequest request) {

		try {
			try {

				String accessToken = request.getQueryParameters().getFirst(
						"access_token");
				if (accessToken == null) {
					// Make the OAuth Request out of this request
					OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(
							httpServletRequest, ParameterStyle.HEADER);

					// Get the access token
					accessToken = oauthRequest.getAccessToken();
				}

				if (accessToken == null) {
					return request;
				}

				AuthPrincipalInfo principal = null;
				try {
					principal = AuthPrincipalInfo
							.getFromAccessToken(accessToken);
				} catch (BadTokenException e1) {
					e1.printStackTrace();
				}

				if (principal == null) {
					return request;
				}

				PrincipalCredentialsToken token = null;

				if (AuthPrincipalType.ADMIN_USER.equals(principal.getType())) {

					UserInfo user = null;
					try {
						user = management
								.getAdminUserInfoFromAccessToken(accessToken);
					} catch (ManagementException e) {
						throw new MappableContainerException(e);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (user == null) {
						throw mappableSecurityException(BAD_ACCESS_TOKEN_ERROR);
					}

					token = PrincipalCredentialsToken
							.getFromAdminUserInfoAndAccessToken(user,
									accessToken);

				} else if (AuthPrincipalType.APPLICATION_USER.equals(principal
						.getType())) {

					UserInfo user = null;
					try {
						user = management
								.getAppUserFromAccessToken(accessToken);
					} catch (ManagementException e) {
						throw new MappableContainerException(e);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (user == null) {
						throw mappableSecurityException(BAD_ACCESS_TOKEN_ERROR);
					}

					token = PrincipalCredentialsToken
							.getFromAppUserInfoAndAccessToken(user, accessToken);

				} else if (AuthPrincipalType.ORGANIZATION.equals(principal
						.getType())) {

					OrganizationInfo organization = null;
					try {
						organization = management
								.getOrganizationInfoFromAccessToken(accessToken);
					} catch (ManagementException e) {
						throw new MappableContainerException(e);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (organization == null) {
						throw mappableSecurityException(BAD_ACCESS_TOKEN_ERROR);
					}

					token = PrincipalCredentialsToken
							.getFromOrganizationInfoAndAccessToken(
									organization, accessToken);

				} else if (AuthPrincipalType.APPLICATION.equals(principal
						.getType())) {

					ApplicationInfo application = null;
					try {
						application = management
								.getApplicationInfoFromAccessToken(accessToken);
					} catch (ManagementException e) {
						throw new MappableContainerException(e);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (application == null) {
						throw mappableSecurityException(BAD_ACCESS_TOKEN_ERROR);
					}

					token = PrincipalCredentialsToken
							.getFromApplicationInfoAndAccessToken(application,
									accessToken);

				}

				Subject subject = SubjectUtils.getSubject();
				subject.login(token);

			} catch (OAuthProblemException e) {
				// Check if the error code has been set
				String errorCode = e.getError();
				if (OAuthUtils.isEmpty(errorCode)) {

					return request;
				}

				throw new MappableContainerException(e);

			}
		} catch (OAuthSystemException ose) {
			throw new MappableContainerException(ose);
		}

		return request;
	}

}
