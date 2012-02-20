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
package org.usergrid.security.shiro.utils;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.security.shiro.Realm.ROLE_ADMIN_USER;
import static org.usergrid.security.shiro.Realm.ROLE_APPLICATION_ADMIN;
import static org.usergrid.security.shiro.Realm.ROLE_APPLICATION_USER;
import static org.usergrid.security.shiro.Realm.ROLE_ORGANIZATION_ADMIN;
import static org.usergrid.security.shiro.Realm.ROLE_SERVICE_ADMIN;

import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.Identifier;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.security.shiro.principals.UserPrincipal;

import com.google.common.collect.BiMap;

public class SubjectUtils {

	private static final Logger logger = LoggerFactory
			.getLogger(SubjectUtils.class);

	public static boolean isAnonymous() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return true;
		}
		if (!currentUser.isAuthenticated() && !currentUser.isRemembered()) {
			return true;
		}
		return false;
	}

	public static boolean isOrganizationAdmin() {
		if (isServiceAdmin()) {
			return true;
		}
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		return currentUser.hasRole(ROLE_ORGANIZATION_ADMIN);
	}

	public static BiMap<UUID, String> getOrganizations() {
		Subject currentUser = getSubject();
		if (!isOrganizationAdmin()) {
			return null;
		}
		Session session = currentUser.getSession();
		@SuppressWarnings("unchecked")
		BiMap<UUID, String> organizations = (BiMap<UUID, String>) session
				.getAttribute("organizations");
		return organizations;
	}

	public static boolean isPermittedAccessToOrganization(Identifier identifier) {
		if (isServiceAdmin()) {
			return true;
		}
		OrganizationInfo organization = getOrganization(identifier);
		if (organization == null) {
			return false;
		}
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		return currentUser.isPermitted("organizations:access:"
				+ organization.getUuid());
	}

	public static OrganizationInfo getOrganization(Identifier identifier) {
		if (identifier == null) {
			return null;
		}
		UUID organizationId = null;
		String organizationName = null;
		BiMap<UUID, String> organizations = getOrganizations();
		if (organizations == null) {
			return null;
		}
		if (identifier.isName()) {
			organizationName = identifier.getName().toLowerCase();
			organizationId = organizations.inverse().get(organizationName);
		} else if (identifier.isUUID()) {
			organizationId = identifier.getUUID();
			organizationName = organizations.get(organizationId);
		}
		if ((organizationId != null) && (organizationName != null)) {
			return new OrganizationInfo(organizationId, organizationName);
		}
		return null;
	}

	public static OrganizationInfo getOrganization() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return null;
		}
		if (!currentUser.hasRole(ROLE_ORGANIZATION_ADMIN)) {
			return null;
		}
		Session session = currentUser.getSession();
		OrganizationInfo organization = (OrganizationInfo) session
				.getAttribute("organization");
		return organization;
	}

	public static String getOrganizationName() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return null;
		}
		if (!currentUser.hasRole(ROLE_ORGANIZATION_ADMIN)) {
			return null;
		}
		Session session = currentUser.getSession();
		OrganizationInfo organization = (OrganizationInfo) session
				.getAttribute("organization");
		if (organization == null) {
			return null;
		}
		return organization.getName();
	}

	public static UUID getOrganizationId() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return null;
		}
		if (!currentUser.hasRole(ROLE_ORGANIZATION_ADMIN)) {
			return null;
		}
		Session session = currentUser.getSession();
		OrganizationInfo organization = (OrganizationInfo) session
				.getAttribute("organization");
		if (organization == null) {
			return null;
		}
		return organization.getUuid();
	}

	public static Set<String> getOrganizationNames() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return null;
		}
		if (!currentUser.hasRole(ROLE_ORGANIZATION_ADMIN)) {
			return null;
		}
		BiMap<UUID, String> organizations = getOrganizations();
		if (organizations == null) {
			return null;
		}
		return organizations.inverse().keySet();
	}

	public static boolean isApplicationAdmin() {
		if (isServiceAdmin()) {
			return true;
		}
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		boolean admin = currentUser.hasRole(ROLE_APPLICATION_ADMIN);
		return admin;
	}

	public static boolean isPermittedAccessToApplication(Identifier identifier) {
		if (isServiceAdmin()) {
			return true;
		}
		ApplicationInfo application = getApplication(identifier);
		if (application == null) {
			return false;
		}
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		return currentUser.isPermitted("applications:access:"
				+ application.getId());
	}

	public static boolean isApplicationAdmin(Identifier identifier) {
		if (isServiceAdmin()) {
			return true;
		}
		ApplicationInfo application = getApplication(identifier);
		if (application == null) {
			return false;
		}
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		return currentUser.isPermitted("applications:admin:"
				+ application.getId());
	}

	public static ApplicationInfo getApplication(Identifier identifier) {
		if (identifier == null) {
			return null;
		}
		if (!isApplicationAdmin() && !isApplicationUser()) {
			return null;
		}
		String applicationName = null;
		UUID applicationId = null;
		BiMap<UUID, String> applications = getApplications();
		if (applications == null) {
			return null;
		}
		if (identifier.isName()) {
			applicationName = identifier.getName().toLowerCase();
			applicationId = applications.inverse().get(applicationName);
			if (applicationId == null) {
				applicationId = applications.inverse()
						.get(identifier.getName());
			}
		} else if (identifier.isUUID()) {
			applicationId = identifier.getUUID();
			applicationName = applications.get(identifier.getUUID());
		}
		if ((applicationId != null) && (applicationName != null)) {
			return new ApplicationInfo(applicationId, applicationName);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static BiMap<UUID, String> getApplications() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return null;
		}
		if (!currentUser.hasRole(ROLE_APPLICATION_ADMIN)
				&& !currentUser.hasRole(ROLE_APPLICATION_USER)) {
			return null;
		}
		Session session = currentUser.getSession();
		return (BiMap<UUID, String>) session.getAttribute("applications");
	}

	public static boolean isServiceAdmin() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		return currentUser.hasRole(ROLE_SERVICE_ADMIN);
	}

	public static boolean isApplicationUser() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		return currentUser.hasRole(ROLE_APPLICATION_USER);
	}

	public static boolean isAdminUser() {
		if (isServiceAdmin()) {
			return true;
		}
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		return currentUser.hasRole(ROLE_ADMIN_USER);
	}

	public static UserInfo getUser() {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return null;
		}
		if (!(currentUser.getPrincipal() instanceof UserPrincipal)) {
			return null;
		}
		UserPrincipal principal = (UserPrincipal) currentUser.getPrincipal();
		return principal.getUser();
	}

	public static boolean isUserActivated() {
		UserInfo user = getUser();
		if (user == null) {
			return false;
		}
		return user.isActivated();
	}

	public static boolean isUserEnabled() {
		UserInfo user = getUser();
		if (user == null) {
			return false;
		}
		return !user.isDisabled();
	}

	public static boolean isUser(Identifier identifier) {
		if (identifier == null) {
			return false;
		}
		UserInfo user = getUser();
		if (user == null) {
			return false;
		}
		if (identifier.isUUID()) {
			return user.getUuid().equals(identifier.getUUID());
		}
		if (identifier.isEmail()) {
			return user.getEmail().equals(identifier.getEmail());
		}
		if (identifier.isName()) {
			return user.getUsername().equals(identifier.getName());
		}
		return false;
	}

	public static boolean isPermittedAccessToUser(UUID userId) {
		if (isServiceAdmin()) {
			return true;
		}
		if (userId == null) {
			return false;
		}
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return false;
		}
		return currentUser.isPermitted("users:access:" + userId);
	}

	public static String getPermissionFromPath(UUID applicationId,
			String operations, String... paths) {
		String permission = "applications:" + operations + ":" + applicationId;
		String p = StringUtils.join(paths, ',');
		permission += (isNotBlank(p) ? ":" + p : "");
		return permission;
	}

	public static Subject getSubject() {
		Subject currentUser = null;
		try {
			currentUser = SecurityUtils.getSubject();
		} catch (UnavailableSecurityManagerException e) {
			logger.error("getSubject(): Attempt to use Shiro prior to initialization");
		}
		return currentUser;
	}

	public static void checkPermission(String permission) {
		Subject currentUser = getSubject();
		if (currentUser == null) {
			return;
		}
		try {
			currentUser.checkPermission(permission);
		} catch (org.apache.shiro.authz.UnauthenticatedException e) {
			logger.error("checkPermission(): Subject is anonymous");
		}
	}

	public static void loginApplicationGuest(ApplicationInfo application) {
		if (application == null) {
			logger.error("loginApplicationGuest(): Null application");
			return;
		}
		if (isAnonymous()) {
			Subject subject = SubjectUtils.getSubject();
			PrincipalCredentialsToken token = PrincipalCredentialsToken
					.getGuestCredentialsFromApplicationInfo(application);
			subject.login(token);
		} else {
			logger.error("loginApplicationGuest(): Logging in non-anonymous user as guest not allowed");
		}
	}

}
