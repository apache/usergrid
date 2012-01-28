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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.usergrid.security.shiro.utils.SubjectUtils.getPermissionFromPath;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Results.Level;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.shiro.credentials.AdminUserAccessToken;
import org.usergrid.security.shiro.credentials.AdminUserPassword;
import org.usergrid.security.shiro.credentials.ApplicationAccessToken;
import org.usergrid.security.shiro.credentials.ApplicationUserAccessToken;
import org.usergrid.security.shiro.credentials.ClientCredentials;
import org.usergrid.security.shiro.credentials.OrganizationAccessToken;
import org.usergrid.security.shiro.credentials.PrincipalCredentials;
import org.usergrid.security.shiro.principals.AdminUserPrincipal;
import org.usergrid.security.shiro.principals.ApplicationGuestPrincipal;
import org.usergrid.security.shiro.principals.ApplicationPrincipal;
import org.usergrid.security.shiro.principals.ApplicationUserPrincipal;
import org.usergrid.security.shiro.principals.OrganizationPrincipal;
import org.usergrid.security.shiro.principals.PrincipalIdentifier;

import com.google.common.collect.HashBiMap;

public class Realm extends AuthorizingRealm {

	private static final Logger logger = LoggerFactory.getLogger(Realm.class);

	public final static String ROLE_SERVICE_ADMIN = "service-admin";
	public final static String ROLE_ADMIN_USER = "admin-user";
	public final static String ROLE_ORGANIZATION_ADMIN = "organization-admin";
	public final static String ROLE_APPLICATION_ADMIN = "application-admin";
	public final static String ROLE_APPLICATION_USER = "application-user";

	EntityManagerFactory emf;
	Properties properties;
	ManagementService management;

	@Value("${usergrid.system.login.name:admin}")
	String systemUser;
	@Value("${usergrid.system.login.password:admin}")
	String systemPassword;

	public Realm() {
		setCredentialsMatcher(new AllowAllCredentialsMatcher());
		setPermissionResolver(new CustomPermissionResolver());
	}

	public Realm(CacheManager cacheManager) {
		super(cacheManager);
		setCredentialsMatcher(new AllowAllCredentialsMatcher());
		setPermissionResolver(new CustomPermissionResolver());
	}

	public Realm(CredentialsMatcher matcher) {
		super(new AllowAllCredentialsMatcher());
		setPermissionResolver(new CustomPermissionResolver());
	}

	public Realm(CacheManager cacheManager, CredentialsMatcher matcher) {
		super(cacheManager, new AllowAllCredentialsMatcher());
		setPermissionResolver(new CustomPermissionResolver());
	}

	@Override
	public void setCredentialsMatcher(CredentialsMatcher credentialsMatcher) {
		if (!(credentialsMatcher instanceof AllowAllCredentialsMatcher)) {
			logger.info("Replacing " + credentialsMatcher
					+ " with AllowAllCredentialsMatcher");
			credentialsMatcher = new AllowAllCredentialsMatcher();
		}
		super.setCredentialsMatcher(credentialsMatcher);
	}

	@Override
	public void setPermissionResolver(PermissionResolver permissionResolver) {
		if (!(permissionResolver instanceof CustomPermissionResolver)) {
			logger.info("Replacing " + permissionResolver
					+ " with AllowAllCredentialsMatcher");
			permissionResolver = new CustomPermissionResolver();
		}
		super.setPermissionResolver(permissionResolver);
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Autowired
	public void setManagementService(ManagementService management) {
		this.management = management;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(
			AuthenticationToken token) throws AuthenticationException {
		PrincipalCredentialsToken pcToken = (PrincipalCredentialsToken) token;

		if (pcToken.getCredentials() == null) {
			throw new CredentialsException("Missing credentials");
		}

		boolean authenticated = false;

		PrincipalIdentifier principal = pcToken.getPrincipal();
		PrincipalCredentials credentials = pcToken.getCredentials();

		if (credentials instanceof ClientCredentials) {
			authenticated = true;
		} else if ((principal instanceof AdminUserPrincipal)
				&& (credentials instanceof AdminUserPassword)) {
			authenticated = true;
		} else if ((principal instanceof AdminUserPrincipal)
				&& (credentials instanceof AdminUserAccessToken)) {
			authenticated = true;
		} else if ((principal instanceof ApplicationUserPrincipal)
				&& (credentials instanceof ApplicationUserAccessToken)) {
			authenticated = true;
		} else if ((principal instanceof ApplicationPrincipal)
				&& (credentials instanceof ApplicationAccessToken)) {
			authenticated = true;
		} else if ((principal instanceof OrganizationPrincipal)
				&& (credentials instanceof OrganizationAccessToken)) {
			authenticated = true;
		}

		if (principal != null) {
			if (!principal.isActivated()) {
				throw new AuthenticationException("Unactivated identity");
			}
			if (principal.isDisabled()) {
				throw new AuthenticationException("Disabled identity");
			}
		}

		if (!authenticated) {
			throw new AuthenticationException("Unable to authenticate");
		}
		logger.info("Authenticated: " + principal);

		SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(
				pcToken.getPrincipal(), pcToken.getCredentials(), getName());
		return info;
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(
			PrincipalCollection principals) {
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

		Map<UUID, String> organizationSet = HashBiMap.create();
		Map<UUID, String> applicationSet = HashBiMap.create();
		OrganizationInfo organization = null;
		ApplicationInfo application = null;

		for (PrincipalIdentifier principal : principals
				.byType(PrincipalIdentifier.class)) {

			if (principal instanceof OrganizationPrincipal) {
				// OrganizationPrincipals are usually only through OAuth
				// They have access to a single organization

				organization = ((OrganizationPrincipal) principal)
						.getOrganization();

				role(info, principal, ROLE_ORGANIZATION_ADMIN);
				role(info, principal, ROLE_APPLICATION_ADMIN);

				grant(info, principal,
						"organizations:access:" + organization.getUuid());
				organizationSet.put(organization.getUuid(),
						organization.getName());

				Map<UUID, String> applications = null;
				try {
					applications = management
							.getApplicationsForOrganization(organization
									.getUuid());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if ((applications != null) && !applications.isEmpty()) {
					grant(info,
							principal,
							"applications:admin,access,get,put,post,delete:"
									+ StringUtils.join(applications.keySet(),
											','));

					applicationSet.putAll(applications);
				}

			} else if (principal instanceof ApplicationPrincipal) {
				// ApplicationPrincipal are usually only through OAuth
				// They have access to a single application

				role(info, principal, ROLE_APPLICATION_ADMIN);

				application = ((ApplicationPrincipal) principal)
						.getApplication();
				grant(info, principal,
						"applications:admin,access,get,put,post,delete:"
								+ application.getId());
				applicationSet.put(application.getId(), application.getName());

			} else if (principal instanceof AdminUserPrincipal) {
				// AdminUserPrincipals are through basic auth and sessions
				// They have access to organizations and organization
				// applications

				UserInfo user = ((AdminUserPrincipal) principal).getUser();

				if (user.getUsername().equals("superuser")) {
					// The system user has access to everything

					role(info, principal, ROLE_SERVICE_ADMIN);
					role(info, principal, ROLE_ORGANIZATION_ADMIN);
					role(info, principal, ROLE_APPLICATION_ADMIN);
					role(info, principal, ROLE_ADMIN_USER);

					grant(info, principal, "system:access");

					grant(info, principal,
							"organizations:admin,access,get,put,post,delete:*");
					grant(info, principal,
							"applications:admin,access,get,put,post,delete:*");
					grant(info, principal, "users:access:*");

				} else {

					// For regular service users, we find what organizations
					// they're associated with
					// An service user can be associated with multiple
					// organizations

					grant(info,
							principal,
							getPermissionFromPath(MANAGEMENT_APPLICATION_ID,
									"access"));

					// admin users cannot access the management app directly
					// so open all permissions
					grant(info,
							principal,
							getPermissionFromPath(MANAGEMENT_APPLICATION_ID,
									"get,put,post,delete", "/**"));

					role(info, principal, ROLE_ADMIN_USER);

					try {

						Map<UUID, String> userOrganizations = management
								.getOrganizationsForAdminUser(user.getUuid());

						if (userOrganizations != null) {
							for (UUID id : userOrganizations.keySet()) {
								grant(info, principal,
										"organizations:admin,access,get,put,post,delete:"
												+ id);
							}
							organizationSet.putAll(userOrganizations);

							Map<UUID, String> userApplications = management
									.getApplicationsForOrganizations(userOrganizations
											.keySet());
							if ((userApplications != null)
									&& !userApplications.isEmpty()) {
								grant(info,
										principal,
										"applications:admin,access,get,put,post,delete:"
												+ StringUtils.join(
														userApplications
																.keySet(), ','));
								applicationSet.putAll(userApplications);
							}

							role(info, principal, ROLE_ORGANIZATION_ADMIN);
							role(info, principal, ROLE_APPLICATION_ADMIN);
						}

					} catch (Exception e) {
						logger.error(
								"Unable to construct admin user permissions", e);
					}
				}
			} else if (principal instanceof ApplicationUserPrincipal) {

				role(info, principal, ROLE_APPLICATION_USER);

				UUID applicationId = ((ApplicationUserPrincipal) principal)
						.getApplicationId();

				grant(info, principal,
						getPermissionFromPath(applicationId, "access"));

				grant(info,
						principal,
						getPermissionFromPath(applicationId,
								"get,put,post,delete", "/users/${user}",
								"/users/${user}/feed",
								"/users/${user}/activities",
								"/users/${user}/groups",
								"/users/${user}/following/*",
								"/users/${user}/following/user/*"));

				EntityManager em = emf.getEntityManager(applicationId);
				try {
					String appName = (String) em.getProperty(
							em.getApplicationRef(), "name");
					applicationSet.put(applicationId, appName);
					application = new ApplicationInfo(applicationId, appName);
				} catch (Exception e) {
				}

				try {
					Set<String> permissions = em.getRolePermissions("default");
					grant(info, principal, applicationId, permissions);
				} catch (Exception e) {
					logger.error("Unable to get user default role permissions",
							e);
				}

				UserInfo user = ((ApplicationUserPrincipal) principal)
						.getUser();
				try {
					Set<String> permissions = em.getUserPermissions(user
							.getUuid());
					grant(info, principal, applicationId, permissions);
				} catch (Exception e) {
					logger.error("Unable to get user permissions", e);
				}

				try {
					Set<String> roles = em.getUserRoles(user.getUuid());
					for (String role : roles) {
						Set<String> permissions = em.getRolePermissions(role);
						grant(info, principal, applicationId, permissions);
					}
				} catch (Exception e) {
					logger.error("Unable to get user role permissions", e);
				}

				try {
					Results r = em.getCollection(new SimpleEntityRef(
							User.ENTITY_TYPE, user.getUuid()), "groups", null,
							1000, Level.IDS, false);
					if (r != null) {
						for (UUID groupId : r.getIds()) {
							Map<String, String> groupRoles = em
									.getUserGroupRoles(user.getUuid(), groupId);
							for (String roleName : groupRoles.keySet()) {
								Set<String> permissions = em
										.getGroupRolePermissions(groupId,
												roleName);
								grant(info, principal, applicationId,
										permissions);
							}
						}
					}

				} catch (Exception e) {
					logger.error("Unable to get user group role permissions", e);
				}

			} else if (principal instanceof ApplicationGuestPrincipal) {
				role(info, principal, ROLE_APPLICATION_USER);

				UUID applicationId = ((ApplicationUserPrincipal) principal)
						.getApplicationId();

				EntityManager em = emf.getEntityManager(applicationId);

				grant(info, principal,
						getPermissionFromPath(applicationId, "access"));

				try {
					Set<String> permissions = em.getRolePermissions("guest");
					grant(info, principal, applicationId, permissions);
				} catch (Exception e) {
					logger.error("Unable to get user default role permissions",
							e);
				}
			}
		}

		// Store additional information in the request session to speed up
		// looking up organization info

		Subject currentUser = SecurityUtils.getSubject();
		Session session = currentUser.getSession();
		session.setAttribute("applications", applicationSet);
		session.setAttribute("organizations", organizationSet);
		if (organization != null) {
			session.setAttribute("organization", organization);
		}
		if (application != null) {
			session.setAttribute("application", application);
		}

		return info;
	}

	public static void grant(SimpleAuthorizationInfo info,
			PrincipalIdentifier principal, String permission) {
		logger.info("Principal " + principal + " granted permission: "
				+ permission);
		info.addStringPermission(permission);
	}

	public static void role(SimpleAuthorizationInfo info,
			PrincipalIdentifier principal, String role) {
		logger.info("Principal " + principal + " added to role: " + role);
		info.addRole(role);
	}

	private static void grant(SimpleAuthorizationInfo info,
			PrincipalIdentifier principal, UUID applicationId,
			Set<String> permissions) {
		if (permissions != null) {
			for (String permission : permissions) {
				if (isNotBlank(permission)) {
					String operations = "*";
					if (permission.indexOf(':') != -1) {
						operations = stringOrSubstringBeforeFirst(permission,
								':');
					}
					if (isBlank(operations)) {
						operations = "*";
					}
					permission = stringOrSubstringAfterFirst(permission, ':');
					permission = "applications:" + operations + ":"
							+ applicationId + ":" + permission;
					grant(info, principal, permission);
				}
			}
		}
	}

	@Override
	public boolean supports(AuthenticationToken token) {
		return token instanceof PrincipalCredentialsToken;
	}
}
