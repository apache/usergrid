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
package org.usergrid.security.shiro;

import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.security.shiro.credentials.AdminUserAccessToken;
import org.usergrid.security.shiro.credentials.AdminUserPassword;
import org.usergrid.security.shiro.credentials.ApplicationAccessToken;
import org.usergrid.security.shiro.credentials.ApplicationGuest;
import org.usergrid.security.shiro.credentials.ApplicationUserAccessToken;
import org.usergrid.security.shiro.credentials.OrganizationAccessToken;
import org.usergrid.security.shiro.credentials.PrincipalCredentials;
import org.usergrid.security.shiro.principals.AdminUserPrincipal;
import org.usergrid.security.shiro.principals.ApplicationGuestPrincipal;
import org.usergrid.security.shiro.principals.ApplicationPrincipal;
import org.usergrid.security.shiro.principals.ApplicationUserPrincipal;
import org.usergrid.security.shiro.principals.OrganizationPrincipal;
import org.usergrid.security.shiro.principals.PrincipalIdentifier;

public class PrincipalCredentialsToken implements
		org.apache.shiro.authc.AuthenticationToken {

	private static final long serialVersionUID = 1L;
	private final PrincipalIdentifier principal;
	private final PrincipalCredentials credential;

	public PrincipalCredentialsToken(PrincipalIdentifier principal,
			PrincipalCredentials credential) {
		this.principal = principal;
		this.credential = credential;
	}

	@Override
	public PrincipalCredentials getCredentials() {
		return credential;
	}

	@Override
	public PrincipalIdentifier getPrincipal() {
		return principal;
	}

	public static PrincipalCredentialsToken getFromAdminUserInfoAndPassword(
			UserInfo user, String password) {

		if (user != null) {
			return new PrincipalCredentialsToken(new AdminUserPrincipal(user),
					new AdminUserPassword(password));
		}
		return null;
	}

	public static PrincipalCredentialsToken getFromOrganizationInfoAndAccessToken(
			OrganizationInfo organization, String token) {

		if (organization != null) {
			OrganizationPrincipal principal = new OrganizationPrincipal(
					organization);
			OrganizationAccessToken credentials = new OrganizationAccessToken(
					token);
			principal.setAccessTokenCredentials(credentials);
			return new PrincipalCredentialsToken(principal, credentials);
		}
		return null;
	}

	public static PrincipalCredentialsToken getFromApplicationInfoAndAccessToken(
			ApplicationInfo application, String token) {

		if (application != null) {
			ApplicationPrincipal principal = new ApplicationPrincipal(
					application);
			ApplicationAccessToken credentials = new ApplicationAccessToken(
					token);
			principal.setAccessTokenCredentials(credentials);
			return new PrincipalCredentialsToken(principal, credentials);
		}
		return null;
	}

	public static PrincipalCredentialsToken getGuestCredentialsFromApplicationInfo(
			ApplicationInfo application) {

		if (application != null) {
			return new PrincipalCredentialsToken(new ApplicationGuestPrincipal(
					application), new ApplicationGuest());
		}
		return null;
	}

	public static PrincipalCredentialsToken getFromAdminUserInfoAndAccessToken(
			UserInfo user, String token) {

		if (user != null) {
			AdminUserPrincipal principal = new AdminUserPrincipal(user);
			AdminUserAccessToken credentials = new AdminUserAccessToken(token);
			principal.setAccessTokenCredentials(credentials);
			return new PrincipalCredentialsToken(principal, credentials);
		}
		return null;
	}

	public static PrincipalCredentialsToken getFromAppUserInfoAndAccessToken(
			UserInfo user, String token) {

		if (user != null) {
			ApplicationUserPrincipal principal = new ApplicationUserPrincipal(
					user.getApplicationId(), user);
			ApplicationUserAccessToken credentials = new ApplicationUserAccessToken(
					token);
			principal.setAccessTokenCredentials(credentials);
			return new PrincipalCredentialsToken(principal, credentials);
		}
		return null;
	}

	public boolean isDisabled() {
		return (principal != null) ? principal.isDisabled() : false;
	}

	public boolean isActivated() {
		return (principal != null) ? principal.isActivated() : true;
	}

}
