/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
package org.usergrid.rest.security.shiro.filters;

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
import org.usergrid.management.exceptions.BadAccessTokenException;
import org.usergrid.management.exceptions.ManagementException;
import org.usergrid.rest.exceptions.AuthErrorInfo;
import org.usergrid.security.AuthPrincipalInfo;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.security.shiro.utils.SubjectUtils;

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
				} catch (BadAccessTokenException e1) {
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
						throwBadTokenError();
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
						throwBadTokenError();
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
						throwBadTokenError();
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
						throwBadTokenError();
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

	private void throwBadTokenError() throws OAuthSystemException {

		throw mappableSecurityException(AuthErrorInfo.BAD_ACCESS_TOKEN_ERROR);

	}
}
