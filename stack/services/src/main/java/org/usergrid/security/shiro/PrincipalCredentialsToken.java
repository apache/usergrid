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
			return new PrincipalCredentialsToken(new OrganizationPrincipal(
					organization), new OrganizationAccessToken(token));
		}
		return null;
	}

	public static PrincipalCredentialsToken getFromApplicationInfoAndAccessToken(
			ApplicationInfo application, String token) {

		if (application != null) {
			return new PrincipalCredentialsToken(new ApplicationPrincipal(
					application), new ApplicationAccessToken(token));
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
			return new PrincipalCredentialsToken(new AdminUserPrincipal(user),
					new AdminUserAccessToken(token));
		}
		return null;
	}

	public static PrincipalCredentialsToken getFromAppUserInfoAndAccessToken(
			UserInfo user, String token) {

		if (user != null) {
			return new PrincipalCredentialsToken(new ApplicationUserPrincipal(
					user.getApplicationId(), user),
					new ApplicationUserAccessToken(token));
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
