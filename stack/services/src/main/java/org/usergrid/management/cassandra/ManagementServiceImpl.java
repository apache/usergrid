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
package org.usergrid.management.cassandra;

import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.usergrid.persistence.CredentialsInfo.checkPassword;
import static org.usergrid.persistence.CredentialsInfo.getCredentialsSecret;
import static org.usergrid.persistence.CredentialsInfo.mongoPasswordCredentials;
import static org.usergrid.persistence.CredentialsInfo.passwordCredentials;
import static org.usergrid.persistence.CredentialsInfo.plainTextCredentials;
import static org.usergrid.persistence.Schema.DICTIONARY_CREDENTIALS;
import static org.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.usergrid.persistence.Schema.PROPERTY_PATH;
import static org.usergrid.persistence.Schema.PROPERTY_SECRET;
import static org.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.usergrid.security.AuthPrincipalType.ADMIN_USER;
import static org.usergrid.security.AuthPrincipalType.APPLICATION_USER;
import static org.usergrid.security.AuthPrincipalType.BASE64_PREFIX_LENGTH;
import static org.usergrid.security.AuthPrincipalType.ORGANIZATION;
import static org.usergrid.security.oauth.ClientCredentialsInfo.getTypeFromClientId;
import static org.usergrid.security.oauth.ClientCredentialsInfo.getUUIDFromClientId;
import static org.usergrid.services.ServiceParameter.parameters;
import static org.usergrid.services.ServicePayload.payload;
import static org.usergrid.utils.ConversionUtils.uuid;
import static org.usergrid.utils.ListUtils.anyNull;
import static org.usergrid.utils.MailUtils.sendHtmlMail;
import static org.usergrid.utils.MapUtils.hashMap;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterLast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.locking.LockManager;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.OrganizationOwnerInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.management.exceptions.BadAccessTokenException;
import org.usergrid.management.exceptions.DisabledAdminUserException;
import org.usergrid.management.exceptions.ExpiredAccessTokenException;
import org.usergrid.management.exceptions.IncorrectPasswordException;
import org.usergrid.management.exceptions.InvalidAccessTokenException;
import org.usergrid.management.exceptions.UnableToLeaveOrganizationException;
import org.usergrid.management.exceptions.UnactivatedAdminUserException;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Results.Level;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.entities.Group;
import org.usergrid.persistence.entities.User;
import org.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.usergrid.security.AuthPrincipalInfo;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.security.oauth.AccessInfo;
import org.usergrid.security.oauth.ClientCredentialsInfo;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.security.shiro.credentials.ApplicationClientCredentials;
import org.usergrid.security.shiro.credentials.OrganizationClientCredentials;
import org.usergrid.security.shiro.principals.ApplicationPrincipal;
import org.usergrid.security.shiro.principals.OrganizationPrincipal;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.services.ServiceAction;
import org.usergrid.services.ServiceManager;
import org.usergrid.services.ServiceManagerFactory;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;
import org.usergrid.utils.ConversionUtils;
import org.usergrid.utils.JsonUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ManagementServiceImpl implements ManagementService {

	public static final String MANAGEMENT_APPLICATION = "management";

	public static final String APPLICATION_INFO = "application_info";

	private static final Logger logger = LoggerFactory
			.getLogger(ManagementServiceImpl.class);

	public static final String OAUTH_SECRET_SALT = "super secret oauth value";
	public static final String TOKEN_SECRET_SALT = "super secret session value";

	// Token is good for 24 hours
	public static final long MAX_TOKEN_AGE = 24 * 60 * 60 * 1000;

	String sessionSecretSalt = TOKEN_SECRET_SALT;

	long maxTokenAge = MAX_TOKEN_AGE;

	public static String EMAIL_MAILER = "usergrid.management.mailer";

	public static String EMAIL_ADMIN_PASSWORD_RESET = "usergrid.management.email.admin-password-reset";

	public static String EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION = "usergrid.management.email.sysadmin-organization-activation";
	public static String EMAIL_ORGANIZATION_ACTIVATION = "usergrid.management.email.organization-activation";
	public static String EMAIL_ORGANIZATION_ACTIVATED = "usergrid.management.email.organization-activated";

	public static String EMAIL_SYSADMIN_ADMIN_ACTIVATION = "usergrid.management.email.sysadmin-admin-activation";
	public static String EMAIL_ADMIN_ACTIVATION = "usergrid.management.email.admin-activation";
	public static String EMAIL_ADMIN_ACTIVATED = "usergrid.management.email.admin-activated";

	public static String EMAIL_ADMIN_USER_ACTIVATION = "usergrid.management.email.admin-user-activation";
	public static String EMAIL_USER_ACTIVATION = "usergrid.management.email.user-activation";
	public static String EMAIL_USER_ACTIVATED = "usergrid.management.email.user-activated";

	public static String EMAIL_USER_PASSWORD_RESET = "usergrid.management.email.user-password-reset";
	public static String EMAIL_USER_PIN_REQUEST = "usergrid.management.email.user-pin";

	protected ServiceManagerFactory smf;

	protected EntityManagerFactory emf;

	protected Properties properties;

	protected LockManager lockManager;

	/**
	 * Must be constructed with a CassandraClientPool.
	 * 
	 * @param cassandraClientPool
	 *            the cassandra client pool
	 */
	public ManagementServiceImpl() {
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		logger.info("ManagementServiceImpl.setEntityManagerFactory");
		this.emf = emf;
	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;

		if (properties != null) {
			maxTokenAge = Long.parseLong(properties.getProperty(
					"usergrid.auth.token_max_age", "" + MAX_TOKEN_AGE));
			maxTokenAge = maxTokenAge > 0 ? maxTokenAge : MAX_TOKEN_AGE;
		}
	}

	@Autowired
	public void setServiceManagerFactory(ServiceManagerFactory smf) {
		this.smf = smf;
	}

	public LockManager getLockManager() {
		return lockManager;
	}

	@Autowired
	public void setLockManager(LockManager lockManager) {
		this.lockManager = lockManager;
	}

	public void setAccessTokenSecretSalt(String sessionSecretSalt) {
		if (sessionSecretSalt != null) {
			this.sessionSecretSalt = sessionSecretSalt;
		}
	}

	public void setMaxTokenAge(long maxTokenAge) {
		this.maxTokenAge = maxTokenAge;
	}

	private String getPropertyValue(String propertyName) {
		String propertyValue = properties.getProperty(propertyName);
		if (isBlank(propertyValue)) {
			logger.warn("Missing value for " + propertyName);
			return null;
		}
		return propertyValue;
	}

	@Override
	public void setup() throws Exception {

		if (parseBoolean(properties.getProperty("usergrid.setup-test-account"))) {
			String test_app_name = getPropertyValue("usergrid.test-account.app");
			String test_organization_name = getPropertyValue("usergrid.test-account.organization");
			String test_admin_username = getPropertyValue("usergrid.test-account.admin-user.username");
			String test_admin_name = getPropertyValue("usergrid.test-account.admin-user.name");
			String test_admin_email = getPropertyValue("usergrid.test-account.admin-user.email");
			String test_admin_password = getPropertyValue("usergrid.test-account.admin-user.password");

			if (anyNull(test_app_name, test_organization_name,
					test_admin_username, test_admin_name, test_admin_email,
					test_admin_password)) {
				logger.warn("Missing values for test app, check properties.  Skipping test app setup...");
				return;
			}

			UserInfo user = createAdminUser(test_admin_username,
					test_admin_name, test_admin_email, test_admin_password,
					true, false, false);

			OrganizationInfo organization = createOrganization(
					test_organization_name, user);
			UUID appId = createApplication(organization.getUuid(),
					test_app_name);

			postOrganizationActivity(organization.getUuid(), user, "create",
					new SimpleEntityRef(APPLICATION_INFO, appId),
					"Application", test_app_name,
					"<a mailto=\"" + user.getEmail() + "\">" + user.getName()
							+ " (" + user.getEmail()
							+ ")</a> created a new application named "
							+ test_app_name, null);

			boolean superuser_enabled = parseBoolean(properties
					.getProperty("usergrid.sysadmin.login.allowed"));
			String superuser_username = getPropertyValue("usergrid.sysadmin.login.name");
			String superuser_email = getPropertyValue("usergrid.sysadmin.login.email");
			String superuser_password = getPropertyValue("usergrid.sysadmin.login.password");

			if (!anyNull(superuser_username, superuser_email,
					superuser_password)) {
				user = createAdminUser(superuser_username, "Super User",
						superuser_email, superuser_password, superuser_enabled,
						!superuser_enabled, false);
			} else {
				logger.warn("Missing values for superuser account, check properties.  Skipping superuser account setup...");
			}
		} else {
			logger.warn("Test app creation disabled");
		}

	}

	public String generateOAuthSecretKey(AuthPrincipalType type) {
		long timestamp = System.currentTimeMillis();
		ByteBuffer bytes = ByteBuffer.allocate(20);
		bytes.put(sha(timestamp + OAUTH_SECRET_SALT + UUID.randomUUID()));
		String secret = type.getBase64Prefix()
				+ encodeBase64URLSafeString(bytes.array());
		return secret;
	}

	@SuppressWarnings("serial")
	@Override
	public void postOrganizationActivity(UUID organizationId,
			final UserInfo user, String verb, final EntityRef object,
			final String objectType, final String objectName, String title,
			String content) throws Exception {
		ServiceManager sm = smf.getServiceManager(MANAGEMENT_APPLICATION_ID);

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("verb", verb);
		properties.put("category", "admin");
		if (content != null) {
			properties.put("content", content);
		}
		if (title != null) {
			properties.put("title", title);
		}
		properties.put("actor", new HashMap<String, Object>() {
			{
				put("displayName", user.getName());
				put("objectType", "person");
				put("entityType", "user");
				put("uuid", user.getUuid());
			}
		});
		properties.put("object", new HashMap<String, Object>() {
			{
				put("displayName", objectName);
				put("objectType", objectType);
				put("entityType", object.getType());
				put("uuid", object.getUuid());
			}
		});

		sm.newRequest(ServiceAction.POST,
				parameters("groups", organizationId, "activities"),
				payload(properties)).execute().getEntity();

	}

	@Override
	public ServiceResults getOrganizationActivity(OrganizationInfo organization)
			throws Exception {
		ServiceManager sm = smf.getServiceManager(MANAGEMENT_APPLICATION_ID);
		return sm.newRequest(ServiceAction.GET,
				parameters("groups", organization.getUuid(), "feed")).execute();
	}

	@Override
	public ServiceResults getOrganizationActivityForAdminUser(
			OrganizationInfo organization, UserInfo user) throws Exception {
		ServiceManager sm = smf.getServiceManager(MANAGEMENT_APPLICATION_ID);
		return sm.newRequest(
				ServiceAction.GET,
				parameters("groups", organization.getUuid(), "users",
						user.getUuid(), "feed")).execute();
	}

	@Override
	public ServiceResults getAdminUserActivity(UserInfo user) throws Exception {
		ServiceManager sm = smf.getServiceManager(MANAGEMENT_APPLICATION_ID);
		return sm.newRequest(ServiceAction.GET,
				parameters("users", user.getUuid(), "feed")).execute();
	}

	@Override
	public OrganizationOwnerInfo createOwnerAndOrganization(
			String organizationName, String username, String name,
			String email, String password, boolean activated, boolean disabled,
			boolean sendEmail) throws Exception {

		lockManager.lockProperty(MANAGEMENT_APPLICATION_ID, "groups", "path");
		lockManager.lockProperty(MANAGEMENT_APPLICATION_ID, "users",
				"username", "email");

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

		UserInfo user = null;
		OrganizationInfo organization = null;

		try {
			if (!em.isPropertyValueUniqueForEntity("user", "username", username)) {
				throw new DuplicateUniquePropertyExistsException("user",
						"username", username);
			}

			if (!em.isPropertyValueUniqueForEntity("user", "email", email)) {
				throw new DuplicateUniquePropertyExistsException("user",
						"username", username);
			}

			if (!em.isPropertyValueUniqueForEntity("group", "path",
					organizationName)) {
				throw new DuplicateUniquePropertyExistsException("group",
						"path", organizationName);
			}

			user = createAdminUser(username, name, email, password, false,
					false, false);

			organization = createOrganization(organizationName, user);

		} finally {
			lockManager.unlockProperty(MANAGEMENT_APPLICATION_ID, "groups",
					"path");
			lockManager.unlockProperty(MANAGEMENT_APPLICATION_ID, "users",
					"username", "email");
		}

		return new OrganizationOwnerInfo(user, organization);

	}

	@Override
	public OrganizationInfo createOrganization(String organizationName,
			UserInfo user) throws Exception {

		if ((organizationName == null) || (user == null)) {
			return null;
		}
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

		Group organizationEntity = new Group();
		organizationEntity.setPath(organizationName);
		organizationEntity = em.create(organizationEntity);

		em.addToCollection(organizationEntity, "users", new SimpleEntityRef(
				User.ENTITY_TYPE, user.getUuid()));

		Map<String, CredentialsInfo> credentials = new HashMap<String, CredentialsInfo>();
		credentials
				.put("secret",
						plainTextCredentials(generateOAuthSecretKey(AuthPrincipalType.ORGANIZATION)));
		em.addMapToDictionary(organizationEntity, DICTIONARY_CREDENTIALS,
				credentials);

		OrganizationInfo organization = new OrganizationInfo(
				organizationEntity.getUuid(), organizationName);
		postOrganizationActivity(organization.getUuid(), user, "create",
				organizationEntity, "Organization", organization.getName(),
				"<a mailto=\"" + user.getEmail() + "\">" + user.getName()
						+ " (" + user.getEmail()
						+ ")</a> created a new organization account named "
						+ organizationName, null);
		return organization;
	}

	@Override
	public OrganizationInfo importOrganization(UUID organizationId,
			OrganizationInfo organizationInfo, Map<String, Object> properties)
			throws Exception {

		if (properties == null) {
			properties = new HashMap<String, Object>();
		}

		String organizationName = null;
		if (organizationInfo != null) {
			organizationName = organizationInfo.getName();
		}
		if (organizationName == null) {
			organizationName = (String) properties.get(PROPERTY_PATH);
		}
		if (organizationName == null) {
			organizationName = (String) properties.get(PROPERTY_NAME);
		}
		if (organizationName == null) {
			return null;
		}

		if (organizationId == null) {
			if (organizationInfo != null) {
				organizationId = organizationInfo.getUuid();
			}
		}
		if (organizationId == null) {
			organizationId = uuid(properties.get(PROPERTY_UUID));
		}
		if (organizationId == null) {
			return null;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		properties.put(PROPERTY_PATH, organizationName);
		properties.put(PROPERTY_SECRET,
				generateOAuthSecretKey(AuthPrincipalType.ORGANIZATION));
		Entity organization = em.create(organizationId, Group.ENTITY_TYPE,
				properties);
		// em.addToCollection(organization, "users", new SimpleEntityRef(
		// User.ENTITY_TYPE, userId));
		return new OrganizationInfo(organization.getUuid(), organizationName);
	}

	@Override
	public UUID importApplication(UUID organizationId, Application application)
			throws Exception {
		UUID applicationId = emf.importApplication(application.getUuid(),
				application.getName(), application.getProperties());

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		properties.put("name", application.getName());
		Entity app = em.create(applicationId, APPLICATION_INFO, null);

		Map<String, CredentialsInfo> credentials = new HashMap<String, CredentialsInfo>();
		credentials
				.put("secret",
						CredentialsInfo
								.plainTextCredentials(generateOAuthSecretKey(AuthPrincipalType.APPLICATION)));
		em.addMapToDictionary(app, DICTIONARY_CREDENTIALS, credentials);

		addApplicationToOrganization(organizationId, applicationId);
		return applicationId;
	}

	@Override
	public BiMap<UUID, String> getOrganizations() throws Exception {

		BiMap<UUID, String> organizations = HashBiMap.create();
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Results results = em.getCollection(em.getApplicationRef(), "groups",
				null, 10000, Level.ALL_PROPERTIES, false);
		for (Entity entity : results.getEntities()) {
			organizations.put(entity.getUuid(),
					(String) entity.getProperty("path"));
		}
		return organizations;
	}

	@Override
	public OrganizationInfo getOrganizationInfoFromAccessToken(String token)
			throws Exception {
		if (!AuthPrincipalType.ORGANIZATION.equals(AuthPrincipalType
				.getFromAccessToken(token))) {
			return null;
		}
		Entity entity = geEntityFromAccessToken(MANAGEMENT_APPLICATION_ID,
				token);
		return new OrganizationInfo(entity.getProperties());
	}

	@Override
	public Entity getOrganizationEntityByName(String organizationName)
			throws Exception {

		if (organizationName == null) {
			return null;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		EntityRef ref = em.getAlias("group", organizationName);
		if (ref == null) {
			return null;
		}
		return getOrganizationEntityByUuid(ref.getUuid());
	}

	@Override
	public OrganizationInfo getOrganizationByName(String organizationName)
			throws Exception {

		if (organizationName == null) {
			return null;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		EntityRef ref = em.getAlias("group", organizationName);
		if (ref == null) {
			return null;
		}
		return getOrganizationByUuid(ref.getUuid());
	}

	@Override
	public Entity getOrganizationEntityByUuid(UUID id) throws Exception {

		if (id == null) {
			return null;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Entity entity = em.get(new SimpleEntityRef(Group.ENTITY_TYPE, id));
		return entity;
	}

	@Override
	public OrganizationInfo getOrganizationByUuid(UUID id) throws Exception {

		Entity entity = getOrganizationEntityByUuid(id);
		if (entity == null) {
			return null;
		}
		return new OrganizationInfo(entity.getProperties());
	}

	@Override
	public Entity getOrganizationEntityByIdentifier(Identifier id)
			throws Exception {
		if (id.isUUID()) {
			return getOrganizationEntityByUuid(id.getUUID());
		}
		if (id.isName()) {
			return getOrganizationEntityByName(id.getName());
		}
		return null;
	}

	@Override
	public OrganizationInfo getOrganizationByIdentifier(Identifier id)
			throws Exception {
		if (id.isUUID()) {
			return getOrganizationByUuid(id.getUUID());
		}
		if (id.isName()) {
			return getOrganizationByName(id.getName());
		}
		return null;
	}

	public void postUserActivity(UserInfo user, String verb, EntityRef object,
			String objectType, String objectName, String title, String content)
			throws Exception {
		ServiceManager sm = smf.getServiceManager(MANAGEMENT_APPLICATION_ID);

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("verb", verb);
		properties.put("category", "admin");
		if (content != null) {
			properties.put("content", content);
		}
		if (title != null) {
			properties.put("title", title);
		}
		properties.put("actor", user.getUuid());
		properties.put("actorName", user.getName());
		properties.put("object", object.getUuid());
		properties.put("objectEntityType", object.getType());
		properties.put("objectType", objectType);
		properties.put("objectName", objectName);

		sm.newRequest(ServiceAction.POST,
				parameters("users", user.getUuid(), "activities"),
				payload(properties)).execute().getEntity();

	}

	@Override
	public ServiceResults getAdminUserActivities(UserInfo user)
			throws Exception {
		ServiceManager sm = smf.getServiceManager(MANAGEMENT_APPLICATION_ID);
		ServiceRequest request = sm.newRequest(ServiceAction.GET,
				parameters("users", user.getUuid(), "feed"));
		ServiceResults results = request.execute();
		return results;
	}

	@Override
	public UserInfo createAdminUser(String username, String name, String email,
			String password, boolean activated, boolean disabled,
			boolean sendEmail) throws Exception {

		if ((username == null) || (name == null) || (email == null)
				|| (password == null)) {
			return null;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

		User user = new User();
		user.setUsername(username);
		user.setName(name);
		user.setEmail(email);
		user.setActivated(activated);
		user.setDisabled(disabled);
		user = em.create(user);

		Map<String, CredentialsInfo> credentials = new HashMap<String, CredentialsInfo>();
		credentials.put("password", passwordCredentials(password));
		credentials.put("mongo_pwd",
				mongoPasswordCredentials(username, password));
		credentials
				.put("secret",
						plainTextCredentials(generateOAuthSecretKey(AuthPrincipalType.ADMIN_USER)));
		em.addMapToDictionary(user, DICTIONARY_CREDENTIALS, credentials);

		UserInfo userInfo = new UserInfo(MANAGEMENT_APPLICATION_ID,
				user.getUuid(), username, name, email, activated, disabled);

		if (sendEmail && !activated) {
			sendAdminUserActivationEmail(userInfo);

		}
		return userInfo;
	}

	public UserInfo getUserInfo(UUID applicationId, Entity entity) {

		if (entity == null) {
			return null;
		}
		return new UserInfo(applicationId, entity.getUuid(),
				(String) entity.getProperty("username"), entity.getName(),
				(String) entity.getProperty("email"),
				ConversionUtils.getBoolean(entity.getProperty("activated")),
				ConversionUtils.getBoolean(entity.getProperty("disabled")));
	}

	public UserInfo getUserInfo(UUID applicationId,
			Map<String, Object> properties) {

		if (properties == null) {
			return null;
		}
		return new UserInfo(applicationId, properties);
	}

	@Override
	public List<UserInfo> getAdminUsersForOrganization(UUID organizationId)
			throws Exception {

		if (organizationId == null) {
			return null;
		}

		List<UserInfo> users = new ArrayList<UserInfo>();

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Results results = em.getCollection(new SimpleEntityRef(
				Group.ENTITY_TYPE, organizationId), "users", null, 10000,
				Level.ALL_PROPERTIES, false);
		for (Entity entity : results.getEntities()) {
			users.add(getUserInfo(MANAGEMENT_APPLICATION_ID, entity));
		}

		return users;
	}

	@Override
	public UserInfo updateAdminUser(UserInfo user, String username,
			String name, String email) throws Exception {

		lockManager.lockProperty(MANAGEMENT_APPLICATION_ID, "users",
				"username", "email");
		try {
			EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

			if (!isBlank(username)) {
				em.setProperty(
						new SimpleEntityRef(User.ENTITY_TYPE, user.getUuid()),
						"username", username);
			}

			if (!isBlank(name)) {
				em.setProperty(
						new SimpleEntityRef(User.ENTITY_TYPE, user.getUuid()),
						"name", name);
			}

			if (!isBlank(email)) {
				em.setProperty(
						new SimpleEntityRef(User.ENTITY_TYPE, user.getUuid()),
						"email", email);
			}

			user = getAdminUserByUuid(user.getUuid());
		} finally {
			lockManager.unlockProperty(MANAGEMENT_APPLICATION_ID, "users",
					"username", "email");
		}

		return user;
	}

	public User getAdminUserEntityByEmail(String email) throws Exception {

		if (email == null) {
			return null;
		}

		return getUserEntityByIdentifier(MANAGEMENT_APPLICATION_ID,
				Identifier.fromEmail(email));
	}

	@Override
	public UserInfo getAdminUserByEmail(String email) throws Exception {
		if (email == null) {
			return null;
		}
		return getUserInfo(
				MANAGEMENT_APPLICATION_ID,
				getUserEntityByIdentifier(MANAGEMENT_APPLICATION_ID,
						Identifier.fromEmail(email)));
	}

	public User getUserEntityByIdentifier(UUID applicationId,
			Identifier indentifier) throws Exception {
		EntityManager em = emf.getEntityManager(applicationId);
		return em.get(em.getUserByIdentifier(indentifier), User.class);
	}

	@Override
	public UserInfo getAdminUserByUsername(String username) throws Exception {
		if (username == null) {
			return null;
		}
		return getUserInfo(
				MANAGEMENT_APPLICATION_ID,
				getUserEntityByIdentifier(MANAGEMENT_APPLICATION_ID,
						Identifier.fromName(username)));
	}

	@Override
	public User getAdminUserEntityByUuid(UUID id) throws Exception {
		if (id == null) {
			return null;
		}
		return getUserEntityByIdentifier(MANAGEMENT_APPLICATION_ID,
				Identifier.fromUUID(id));
	}

	@Override
	public UserInfo getAdminUserByUuid(UUID id) throws Exception {
		return getUserInfo(
				MANAGEMENT_APPLICATION_ID,
				getUserEntityByIdentifier(MANAGEMENT_APPLICATION_ID,
						Identifier.fromUUID(id)));
	}

	@Override
	public User getAdminUserEntityByIdentifier(Identifier id) throws Exception {
		return getUserEntityByIdentifier(MANAGEMENT_APPLICATION_ID, id);
	}

	@Override
	public UserInfo getAdminUserByIdentifier(Identifier id) throws Exception {
		if (id.isUUID()) {
			return getAdminUserByUuid(id.getUUID());
		}
		if (id.isName()) {
			return getAdminUserByUsername(id.getName());
		}
		if (id.isEmail()) {
			return getAdminUserByEmail(id.getEmail());
		}
		return null;
	}

	public User findUserEntity(UUID applicationId, String identifier) {

		User user = null;
		try {
			Entity entity = getUserEntityByIdentifier(applicationId,
					Identifier.fromUUID(UUID.fromString(identifier)));
			if (entity != null) {
				user = (User) entity.toTypedEntity();
				logger.info("Found user " + identifier + " as a UUID");
			}
		} catch (Exception e) {
			logger.error("Unable to get user " + identifier
					+ " as a UUID, trying username...");
		}
		if (user != null) {
			return user;
		}

		try {
			Entity entity = getUserEntityByIdentifier(applicationId,
					Identifier.fromName(identifier));
			if (entity != null) {
				user = (User) entity.toTypedEntity();
				logger.info("Found user " + identifier + " as a username");
			}
		} catch (Exception e) {
			logger.error("Unable to get user " + identifier
					+ " as a username, trying email");
		}
		if (user != null) {
			return user;
		}

		try {
			Entity entity = getUserEntityByIdentifier(applicationId,
					Identifier.fromEmail(identifier));
			if (entity != null) {
				user = (User) entity.toTypedEntity();
				logger.info("Found user " + identifier + " as an email address");
			}
		} catch (Exception e) {
			logger.error("Unable to get user " + identifier
					+ " as an email address, failed...");
		}
		if (user != null) {
			return user;
		}

		return null;
	}

	@Override
	public UserInfo findAdminUser(String identifier) {
		return getUserInfo(MANAGEMENT_APPLICATION_ID,
				findUserEntity(MANAGEMENT_APPLICATION_ID, identifier));
	}

	@Override
	public void setAdminUserPassword(UUID userId, String oldPassword,
			String newPassword) throws Exception {

		if ((userId == null) || (oldPassword == null) || (newPassword == null)) {
			return;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Entity user = em.get(userId);
		if (!checkPassword(oldPassword,
				(CredentialsInfo) em.getDictionaryElementValue(user,
						DICTIONARY_CREDENTIALS, "password"))) {
			logger.info("Old password doesn't match");
			throw new IncorrectPasswordException();
		}
		em.addToDictionary(user, DICTIONARY_CREDENTIALS, "password",
				passwordCredentials(newPassword));
	}

	@Override
	public void setAdminUserPassword(UUID userId, String newPassword)
			throws Exception {

		if ((userId == null) || (newPassword == null)) {
			return;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.addToDictionary(new SimpleEntityRef(User.ENTITY_TYPE, userId),
				DICTIONARY_CREDENTIALS, "password",
				passwordCredentials(newPassword));
	}

	@Override
	public boolean verifyAdminUserPassword(UUID userId, String password)
			throws Exception {

		if ((userId == null) || (password == null)) {
			return false;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Entity user = em.get(userId);
		if (checkPassword(password,
				(CredentialsInfo) em.getDictionaryElementValue(user,
						DICTIONARY_CREDENTIALS, "password"))) {
			return true;
		}

		return false;
	}

	@Override
	public UserInfo verifyAdminUserPasswordCredentials(String name,
			String password) throws Exception {
		UserInfo userInfo = null;

		Entity user = findUserEntity(MANAGEMENT_APPLICATION_ID, name);
		if (user == null) {
			return null;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		if (checkPassword(password,
				(CredentialsInfo) em.getDictionaryElementValue(user,
						DICTIONARY_CREDENTIALS, "password"))) {
			userInfo = getUserInfo(MANAGEMENT_APPLICATION_ID, user);
			if (!userInfo.isActivated()) {
				throw new UnactivatedAdminUserException();
			}
			if (userInfo.isDisabled()) {
				throw new DisabledAdminUserException();
			}
			return userInfo;
		}

		return null;

	}

	@Override
	public UserInfo verifyMongoCredentials(String name, String nonce, String key)
			throws Exception {
		UserInfo userInfo = null;
		Entity user = findUserEntity(MANAGEMENT_APPLICATION_ID, name);
		if (user == null) {
			return null;
		}
		String mongo_pwd = (String) user.getProperty("mongo_pwd");
		if (mongo_pwd == null) {
			userInfo = new UserInfo(MANAGEMENT_APPLICATION_ID,
					user.getProperties());
		}

		if (userInfo == null) {
			String expected_key = DigestUtils.md5Hex(nonce
					+ user.getProperty("username") + mongo_pwd);
			if (expected_key.equalsIgnoreCase(key)) {
				userInfo = new UserInfo(MANAGEMENT_APPLICATION_ID,
						user.getProperties());
			}
		}

		if (userInfo != null) {
			if (!userInfo.isActivated()) {
				throw new UnactivatedAdminUserException();
			}
			if (userInfo.isDisabled()) {
				throw new DisabledAdminUserException();
			}
		}
		return userInfo;
	}

	public String getTokenForPrincipal(UUID applicationId,
			AuthPrincipalType type, UUID id, String tokenSalt, boolean useSecret)
			throws Exception {

		if ((type == null) || (id == null)) {
			return null;
		}

		String principalSecret = null;
		if (useSecret) {
			principalSecret = getSecret(applicationId, type, id);
		}
		String secret = (tokenSalt != null ? tokenSalt : "")
				+ (principalSecret != null ? principalSecret : "");

		return new AuthPrincipalInfo(type, id, applicationId)
				.constructAccessToken(sessionSecretSalt, secret);
	}

	public EntityRef getEntityRefFromAccessToken(UUID applicationId,
			String token, AuthPrincipalType expectedType, long maxAge,
			String tokenSalt, boolean useSecret) throws Exception {

		AuthPrincipalInfo principal = AuthPrincipalInfo
				.getFromAccessToken(token);
		if (principal == null) {
			return null;
		}

		if ((expectedType != null) && !principal.getType().equals(expectedType)) {
			logger.info("Token is not of expected type " + token);
			throw new BadAccessTokenException("Token is not of expected type "
					+ token);
		}

		String digestPart = stringOrSubstringAfterLast(token, ':').substring(
				BASE64_PREFIX_LENGTH);
		ByteBuffer bytes = ByteBuffer.wrap(decodeBase64(digestPart));
		if (bytes.remaining() != 28) {
			String error_str = "Token digest is wrong size: " + digestPart
					+ " is " + bytes.remaining() + " bytes, expected 28";
			logger.info(error_str);
			throw new BadAccessTokenException(error_str);
		}

		long timestamp = bytes.getLong(); // OK
		long current_time = System.currentTimeMillis();
		long age = current_time - timestamp;
		if ((maxAge > 0) && (age > maxAge)) {
			logger.info("Token expired " + (age / 1000 / 60) + " minutes ago");
			throw new ExpiredAccessTokenException("Token expired "
					+ (age / 1000 / 60) + " minutes ago");
		}

		EntityRef user = new SimpleEntityRef(principal.getUuid());

		String principalSecret = null;
		if (useSecret) {
			principalSecret = getSecret(applicationId, principal.getType(),
					principal.getUuid());
		}
		String secret = (tokenSalt != null ? tokenSalt : "")
				+ (principalSecret != null ? principalSecret : "");
		ByteBuffer digest = ByteBuffer.wrap(sha(timestamp + sessionSecretSalt
				+ secret + principal.getUuid()));
		boolean verified = digest.equals(bytes);

		if (!verified) {
			throw new InvalidAccessTokenException();
		}

		return user;
	}

	public Entity geEntityFromAccessToken(UUID applicationId, String token)
			throws Exception {

		EntityManager em = emf
				.getEntityManager(applicationId != null ? applicationId
						: MANAGEMENT_APPLICATION_ID);
		Entity entity = em.get(getEntityRefFromAccessToken(applicationId,
				token, null, maxTokenAge, null, true));
		return entity;
	}

	@Override
	public String getAccessTokenForAdminUser(UUID userId) throws Exception {

		return getTokenForPrincipal(MANAGEMENT_APPLICATION_ID, ADMIN_USER,
				userId, null, true);
	}

	@Override
	public Entity getAdminUserEntityFromAccessToken(String token)
			throws Exception {

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Entity user = em.get(getEntityRefFromAccessToken(
				MANAGEMENT_APPLICATION_ID, token, ADMIN_USER, maxTokenAge,
				null, true));
		return user;
	}

	@Override
	public UserInfo getAdminUserInfoFromAccessToken(String token)
			throws Exception {
		Entity user = getAdminUserEntityFromAccessToken(token);
		return new UserInfo(MANAGEMENT_APPLICATION_ID, user.getProperties());
	}

	@Override
	public UUID getAdminUserIdFromAccessToken(String token) throws Exception {
		return AuthPrincipalInfo.getFromAccessToken(token).getUuid();
	}

	@Override
	public BiMap<UUID, String> getOrganizationsForAdminUser(UUID userId)
			throws Exception {

		if (userId == null) {
			return null;
		}

		BiMap<UUID, String> organizations = HashBiMap.create();
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Results results = em.getCollection(new SimpleEntityRef(
				User.ENTITY_TYPE, userId), "groups", null, 10000,
				Level.ALL_PROPERTIES, false);

		for (Entity entity : results.getEntities()) {
			organizations.put(entity.getUuid(),
					(String) entity.getProperty("path"));
		}

		return organizations;
	}

	@Override
	public Map<String, Object> getAdminUserOrganizationData(UUID userId)
			throws Exception {
		UserInfo user = getAdminUserByUuid(userId);
		return getAdminUserOrganizationData(user);
	}

	@Override
	public Map<String, Object> getAdminUserOrganizationData(UserInfo user)
			throws Exception {

		Map<String, Object> json = new HashMap<String, Object>();

		json.putAll(JsonUtils.toJsonMap(user));
		// json.put(PROPERTY_UUID, user.getUuid());
		// json.put(PROPERTY_NAME, user.getName());
		// json.put(PROPERTY_EMAIL, user.getEmail());
		// json.put(PROPERTY_USERNAME, user.getUsername());

		Map<String, Map<String, Object>> jsonOrganizations = new HashMap<String, Map<String, Object>>();
		json.put("organizations", jsonOrganizations);

		Map<UUID, String> organizations = getOrganizationsForAdminUser(user
				.getUuid());

		for (Entry<UUID, String> organization : organizations.entrySet()) {
			Map<String, Object> jsonOrganization = new HashMap<String, Object>();

			jsonOrganizations.put(organization.getValue(), jsonOrganization);

			jsonOrganization.put(PROPERTY_NAME, organization.getValue());
			jsonOrganization.put(PROPERTY_UUID, organization.getKey());

			BiMap<UUID, String> applications = getApplicationsForOrganization(organization
					.getKey());
			jsonOrganization.put("applications", applications.inverse());

			List<UserInfo> users = getAdminUsersForOrganization(organization
					.getKey());
			Map<String, Object> jsonUsers = new HashMap<String, Object>();
			for (UserInfo u : users) {
				jsonUsers.put(u.getUsername(), u);
			}
			jsonOrganization.put("users", jsonUsers);
		}

		return json;
	}

	@Override
	public Map<String, Object> getOrganizationData(OrganizationInfo organization)
			throws Exception {

		Map<String, Object> jsonOrganization = new HashMap<String, Object>();
		jsonOrganization.putAll(JsonUtils.toJsonMap(organization));

		BiMap<UUID, String> applications = getApplicationsForOrganization(organization
				.getUuid());
		jsonOrganization.put("applications", applications.inverse());

		List<UserInfo> users = getAdminUsersForOrganization(organization
				.getUuid());
		Map<String, Object> jsonUsers = new HashMap<String, Object>();
		for (UserInfo u : users) {
			jsonUsers.put(u.getUsername(), u);
		}
		jsonOrganization.put("users", jsonUsers);

		return jsonOrganization;
	}

	@Override
	public void addAdminUserToOrganization(UUID userId, UUID organizationId)
			throws Exception {

		if ((userId == null) || (organizationId == null)) {
			return;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.addToCollection(new SimpleEntityRef(Group.ENTITY_TYPE,
				organizationId), "users", new SimpleEntityRef(User.ENTITY_TYPE,
				userId));
	}

	@Override
	public void removeAdminUserFromOrganization(UUID userId, UUID organizationId)
			throws Exception {

		if ((userId == null) || (organizationId == null)) {
			return;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

		try {
			if (em.getCollection(
					new SimpleEntityRef(Group.ENTITY_TYPE, organizationId),
					"users", null, 2, Level.IDS, false).size() <= 1) {
				throw new Exception();
			}
		} catch (Exception e) {
			throw new UnableToLeaveOrganizationException(
					"Organizations must have at least one member.");
		}

		em.removeFromCollection(new SimpleEntityRef(Group.ENTITY_TYPE,
				organizationId), "users", new SimpleEntityRef(User.ENTITY_TYPE,
				userId));
	}

	@Override
	public UUID createApplication(UUID organizationId, String applicationName)
			throws Exception {

		if ((organizationId == null) || (applicationName == null)) {
			return null;
		}

		return createApplication(organizationId, applicationName, null);
	}

	@Override
	public UUID createApplication(UUID organizationId, String applicationName,
			Map<String, Object> properties) throws Exception {

		if ((organizationId == null) || (applicationName == null)) {
			return null;
		}

		if (properties == null) {
			properties = new HashMap<String, Object>();
		}

		UUID applicationId = emf.createApplication(applicationName, properties);

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		properties.put("name", applicationName);
		Entity applicationEntity = em.create(applicationId, APPLICATION_INFO,
				properties);

		Map<String, CredentialsInfo> credentials = new HashMap<String, CredentialsInfo>();
		credentials
				.put("secret",
						plainTextCredentials(generateOAuthSecretKey(AuthPrincipalType.APPLICATION)));
		em.addMapToDictionary(applicationEntity, DICTIONARY_CREDENTIALS,
				credentials);

		addApplicationToOrganization(organizationId, applicationId);

		UserInfo user = null;
		// if we call this method before the full stack is initialized
		// we'll get an exception
		try {
			user = SubjectUtils.getUser();
		} catch (UnavailableSecurityManagerException e) {
		}
		if ((user != null) && user.isAdminUser()) {
			postOrganizationActivity(organizationId, user, "create",
					applicationEntity, "Application", applicationName,
					"<a mailto=\"" + user.getEmail() + "\">" + user.getName()
							+ " (" + user.getEmail()
							+ ")</a> created a new application named "
							+ applicationName, null);
		}

		return applicationId;
	}

	@Override
	public OrganizationInfo getOrganizationForApplication(UUID applicationId)
			throws Exception {

		if (applicationId == null) {
			return null;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Results r = em.getConnectingEntities(applicationId, "owns", "group",
				Level.ALL_PROPERTIES);
		Entity entity = r.getEntity();
		if (entity != null) {
			return new OrganizationInfo(entity.getUuid(),
					(String) entity.getProperty("path"));
		}

		return null;
	}

	@Override
	public BiMap<UUID, String> getApplicationsForOrganization(
			UUID organizationId) throws Exception {

		if (organizationId == null) {
			return null;
		}
		BiMap<UUID, String> applications = HashBiMap.create();
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Results results = em.getConnectedEntities(organizationId, "owns",
				APPLICATION_INFO, Level.ALL_PROPERTIES);
		if (!results.isEmpty()) {
			for (Entity entity : results.getEntities()) {
				applications.put(entity.getUuid(), entity.getName());
			}
		}

		return applications;
	}

	@Override
	public BiMap<UUID, String> getApplicationsForOrganizations(
			Set<UUID> organizationIds) throws Exception {
		if (organizationIds == null) {
			return null;
		}
		BiMap<UUID, String> applications = HashBiMap.create();
		for (UUID organizationId : organizationIds) {
			BiMap<UUID, String> organizationApplications = getApplicationsForOrganization(organizationId);
			applications.putAll(organizationApplications);
		}
		return applications;
	}

	@Override
	public UUID addApplicationToOrganization(UUID organizationId,
			UUID applicationId) throws Exception {

		if ((organizationId == null) || (applicationId == null)) {
			return null;
		}

		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.createConnection(new SimpleEntityRef("group", organizationId),
				"owns", new SimpleEntityRef(APPLICATION_INFO, applicationId));

		return applicationId;
	}

	@Override
	public void deleteOrganizationApplication(UUID organizationId,
			UUID applicationId) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeOrganizationApplication(UUID organizationId,
			UUID applicationId) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public ApplicationInfo getApplication(String applicationName)
			throws Exception {
		if (applicationName == null) {
			return null;
		}
		UUID applicationId = emf.lookupApplication(applicationName);
		if (applicationId == null) {
			return null;
		}
		return new ApplicationInfo(applicationId, applicationName.toLowerCase());
	}

	@Override
	public ApplicationInfo getApplication(UUID applicationId) throws Exception {
		if (applicationId == null) {
			return null;
		}
		Entity entity = getApplicationEntityById(applicationId);
		if (entity != null) {
			return new ApplicationInfo(applicationId, entity.getName());
		}
		return null;
	}

	@Override
	public ApplicationInfo getApplication(Identifier id) throws Exception {
		if (id == null) {
			return null;
		}
		if (id.isUUID()) {
			return getApplication(id.getUUID());
		}
		if (id.isName()) {
			return getApplication(id.getName());
		}
		return null;
	}

	@Override
	public ApplicationInfo getApplicationInfoFromAccessToken(String token)
			throws Exception {
		if (!AuthPrincipalType.APPLICATION.equals(AuthPrincipalType
				.getFromAccessToken(token))) {
			return null;
		}
		Entity entity = geEntityFromAccessToken(MANAGEMENT_APPLICATION_ID,
				token);
		return new ApplicationInfo(entity.getProperties());
	}

	@Override
	public Entity getApplicationEntityById(UUID applicationId) throws Exception {
		if (applicationId == null) {
			return null;
		}
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		Entity entity = em.get(applicationId);
		return entity;
	}

	public String getSecret(UUID applicationId, AuthPrincipalType type, UUID id)
			throws Exception {
		EntityManager em = emf
				.getEntityManager(AuthPrincipalType.APPLICATION_USER
						.equals(type) ? applicationId
						: MANAGEMENT_APPLICATION_ID);
		if (AuthPrincipalType.ORGANIZATION.equals(type)
				|| AuthPrincipalType.APPLICATION.equals(type)) {
			return getCredentialsSecret((CredentialsInfo) em
					.getDictionaryElementValue(
							new SimpleEntityRef(type.getEntityType(), id),
							DICTIONARY_CREDENTIALS, "secret"));
		} else if (AuthPrincipalType.ADMIN_USER.equals(type)
				|| AuthPrincipalType.APPLICATION_USER.equals(type)) {
			return getCredentialsSecret((CredentialsInfo) em
					.getDictionaryElementValue(
							new SimpleEntityRef(type.getEntityType(), id),
							DICTIONARY_CREDENTIALS, "password"));
		}
		throw new IllegalArgumentException(
				"Must specify an admin user, organization or application principal");
	}

	@Override
	public String getClientIdForOrganization(UUID organizationId) {
		return ClientCredentialsInfo.getClientIdForTypeAndUuid(
				AuthPrincipalType.ORGANIZATION, organizationId);
	}

	@Override
	public String getClientSecretForOrganization(UUID organizationId)
			throws Exception {
		return getSecret(MANAGEMENT_APPLICATION_ID,
				AuthPrincipalType.ORGANIZATION, organizationId);
	}

	@Override
	public String getClientIdForApplication(UUID applicationId) {
		return ClientCredentialsInfo.getClientIdForTypeAndUuid(
				AuthPrincipalType.APPLICATION, applicationId);
	}

	@Override
	public String getClientSecretForApplication(UUID applicationId)
			throws Exception {
		return getSecret(MANAGEMENT_APPLICATION_ID,
				AuthPrincipalType.APPLICATION, applicationId);
	}

	public String newSecretKey(AuthPrincipalType type, UUID id)
			throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		String secret = generateOAuthSecretKey(type);
		em.addToDictionary(new SimpleEntityRef(type.getEntityType(), id),
				DICTIONARY_CREDENTIALS, "secret", plainTextCredentials(secret));
		return secret;
	}

	@Override
	public String newClientSecretForOrganization(UUID organizationId)
			throws Exception {
		return newSecretKey(AuthPrincipalType.ORGANIZATION, organizationId);
	}

	@Override
	public String newClientSecretForApplication(UUID applicationId)
			throws Exception {
		return newSecretKey(AuthPrincipalType.APPLICATION, applicationId);
	}

	@Override
	public AccessInfo authorizeClient(String clientId, String clientSecret)
			throws Exception {
		if ((clientId == null) || (clientSecret == null)) {
			return null;
		}
		UUID uuid = getUUIDFromClientId(clientId);
		if (uuid == null) {
			return null;
		}
		AuthPrincipalType type = getTypeFromClientId(clientId);
		if (type == null) {
			return null;
		}
		AccessInfo access_info = null;
		if (clientSecret
				.equals(getSecret(MANAGEMENT_APPLICATION_ID, type, uuid))) {
			if (type.equals(AuthPrincipalType.APPLICATION)) {
				ApplicationInfo app = getApplication(uuid);
				access_info = new AccessInfo()
						.withExpiresIn(3600)
						.withAccessToken(
								getTokenForPrincipal(MANAGEMENT_APPLICATION_ID,
										type, uuid, null, true))
						.withProperty("application", app.getId());
			} else if (type.equals(AuthPrincipalType.ORGANIZATION)) {
				OrganizationInfo organization = getOrganizationByUuid(uuid);
				access_info = new AccessInfo()
						.withExpiresIn(3600)
						.withAccessToken(
								getTokenForPrincipal(MANAGEMENT_APPLICATION_ID,
										type, uuid, null, true))
						.withProperty("organization",
								getOrganizationData(organization));
			}
		}
		return access_info;
	}

	@Override
	public PrincipalCredentialsToken getPrincipalCredentialsTokenForClientCredentials(
			String clientId, String clientSecret) throws Exception {
		if ((clientId == null) || (clientSecret == null)) {
			return null;
		}
		UUID uuid = getUUIDFromClientId(clientId);
		if (uuid == null) {
			return null;
		}
		AuthPrincipalType type = getTypeFromClientId(clientId);
		if (type == null) {
			return null;
		}
		PrincipalCredentialsToken token = null;
		if (clientSecret
				.equals(getSecret(MANAGEMENT_APPLICATION_ID, type, uuid))) {
			if (type.equals(AuthPrincipalType.APPLICATION)) {
				ApplicationInfo app = getApplication(uuid);
				token = new PrincipalCredentialsToken(new ApplicationPrincipal(
						app), new ApplicationClientCredentials(clientId,
						clientSecret));

			} else if (type.equals(AuthPrincipalType.ORGANIZATION)) {
				OrganizationInfo organization = getOrganizationByUuid(uuid);
				token = new PrincipalCredentialsToken(
						new OrganizationPrincipal(organization),
						new OrganizationClientCredentials(clientId,
								clientSecret));
			}
		}
		return token;
	}

	public AccessInfo authorizeAppUser(String clientType, String clientId,
			String clientSecret) throws Exception {

		return null;
	}

	@Override
	public String getPasswordResetTokenForAdminUser(UUID userId)
			throws Exception {
		return getTokenForPrincipal(MANAGEMENT_APPLICATION_ID, ADMIN_USER,
				userId, "resetpw", true);
	}

	@Override
	public boolean checkPasswordResetTokenForAdminUser(UUID userId, String token)
			throws Exception {
		EntityRef userRef = null;
		try {
			userRef = getEntityRefFromAccessToken(MANAGEMENT_APPLICATION_ID,
					token, ADMIN_USER, 0, "resetpw", true);
		} catch (Exception e) {
			logger.error("Unable to verify token", e);
		}
		return (userRef != null) && userId.equals(userRef.getUuid());
	}

	@Override
	public String getActivationTokenForAdminUser(UUID userId) throws Exception {
		return getTokenForPrincipal(MANAGEMENT_APPLICATION_ID, ADMIN_USER,
				userId, "activate", true);
	}

	@Override
	public boolean checkActivationTokenForAdminUser(UUID userId, String token)
			throws Exception {
		EntityRef userRef = getEntityRefFromAccessToken(
				MANAGEMENT_APPLICATION_ID, token, ADMIN_USER, 0, "activate",
				true);
		return (userRef != null) && userId.equals(userRef.getUuid());
	}

	@Override
	public void activateAdminUser(UUID userId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
				"activated", true);
	}

	@Override
	public void deactivateAdminUser(UUID userId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
				"activated", false);
	}

	@Override
	public boolean isAdminUserActivated(UUID userId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		return Boolean.TRUE.equals(em.getProperty(new SimpleEntityRef(
				User.ENTITY_TYPE, userId), "activated"));
	}

	@Override
	public void enableAdminUser(UUID userId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
				"disabled", false);
	}

	@Override
	public void disableAdminUser(UUID userId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
				"disabled", true);
	}

	@Override
	public boolean isAdminUserEnabled(UUID userId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		return !Boolean.TRUE.equals(em.getProperty(new SimpleEntityRef(
				User.ENTITY_TYPE, userId), "disabled"));
	}

	private String emailMsg(Map<String, String> values, String propertyName) {
		return new StrSubstitutor(values).replace(properties
				.getProperty(propertyName));
	}

	@Override
	public void sendAdminUserPasswordReminderEmail(UserInfo user)
			throws Exception {
		String token = getPasswordResetTokenForAdminUser(user.getUuid());
		String reset_url = String.format(properties
				.getProperty("usergrid.admin.resetpw.url"), user.getUuid()
				.toString())
				+ "?token=" + token;

		sendHtmlMail(
				properties,
				user.getDisplayEmailAddress(),
				getPropertyValue(EMAIL_MAILER),
				"Password Reset",
				emailMsg(hashMap("reset_url", reset_url),
						EMAIL_ADMIN_PASSWORD_RESET));

	}

	@Override
	public boolean newOrganizationsNeedSysAdminApproval() {
		return parseBoolean(properties
				.getProperty("usergrid.sysadmin.approve.organizations"));
	}

	@Override
	public boolean newAdminUsersNeedSysAdminApproval() {
		return parseBoolean(properties
				.getProperty("usergrid.sysadmin.approve.users"));
	}

	@Override
	public String getActivationTokenForOrganization(UUID organizationId)
			throws Exception {
		return getTokenForPrincipal(MANAGEMENT_APPLICATION_ID,
				AuthPrincipalType.ORGANIZATION, organizationId, "activate",
				true);
	}

	@Override
	public boolean checkActivationTokenForOrganization(UUID organizationId,
			String token) throws Exception {
		EntityRef organizationRef = getEntityRefFromAccessToken(
				MANAGEMENT_APPLICATION_ID, token, ORGANIZATION, 0, "activate",
				true);
		return (organizationRef != null)
				&& organizationId.equals(organizationRef.getUuid());
	}

	@Override
	public void sendOrganizationActivationEmail(OrganizationInfo organization)
			throws Exception {
		try {
			String token = getActivationTokenForOrganization(organization
					.getUuid());
			String activation_url = String.format(properties
					.getProperty("usergrid.organization.activation.url"),
					organization.getUuid().toString())
					+ "?token=" + token;
			if (newOrganizationsNeedSysAdminApproval()) {
				activation_url += "&confirm=true";
				sendHtmlMail(
						properties,
						getPropertyValue("usergrid.sysadmin.email"),
						getPropertyValue(EMAIL_MAILER),
						"Request For Organization Account Activation "
								+ organization.getName(),
						emailMsg(
								hashMap("organization_name",
										organization.getName()).map(
										"activation_url", activation_url),
								EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION));
			} else {
				sendOrganizationEmail(
						organization,
						"Organization Account Confirmation",
						emailMsg(
								hashMap("organization_name",
										organization.getName()).map(
										"activation_url", activation_url),
								EMAIL_ORGANIZATION_ACTIVATION));
			}
		} catch (Exception e) {
			logger.error(
					"Unable to send activation emails to "
							+ organization.getName(), e);
		}

	}

	@Override
	public void sendOrganizationActivatedEmail(OrganizationInfo organization)
			throws Exception {
		sendOrganizationEmail(
				organization,
				"Organization Account Activated: " + organization.getName(),
				emailMsg(hashMap("organization_name", organization.getName()),
						EMAIL_ORGANIZATION_ACTIVATED));

	}

	@Override
	public void sendOrganizationEmail(OrganizationInfo organization,
			String subject, String html) throws Exception {
		List<UserInfo> users = getAdminUsersForOrganization(organization
				.getUuid());
		for (UserInfo user : users) {
			sendHtmlMail(properties, user.getDisplayEmailAddress(),
					getPropertyValue(EMAIL_MAILER), subject, html);
		}

	}

	@Override
	public void sendAdminUserActivationEmail(UserInfo user) throws Exception {
		String token = getActivationTokenForAdminUser(user.getUuid());
		String activation_url = String.format(properties
				.getProperty("usergrid.admin.activation.url"), user.getUuid()
				.toString())
				+ "?token=" + token;
		if (newAdminUsersNeedSysAdminApproval()) {
			activation_url += "&confirm=true";
			sendHtmlMail(
					properties,
					getPropertyValue("usergrid.sysadmin.email"),
					getPropertyValue(EMAIL_MAILER),
					"Request For User Account Activation " + user.getEmail(),
					emailMsg(
							hashMap("user_email", user.getEmail()).map(
									"activation_url", activation_url),
							EMAIL_SYSADMIN_ADMIN_ACTIVATION));
		} else {
			sendAdminUserEmail(
					user,
					"User Account Confirmation: " + user.getEmail(),
					emailMsg(
							hashMap("user_email", user.getEmail()).map(
									"activation_url", activation_url),
							EMAIL_ADMIN_ACTIVATION));
		}

	}

	@Override
	public void sendAdminUserActivatedEmail(UserInfo user) throws Exception {
		sendAdminUserEmail(user, "User Account Activated",
				getPropertyValue(EMAIL_ADMIN_ACTIVATED));

	}

	@Override
	public void sendAdminUserEmail(UserInfo user, String subject, String html)
			throws Exception {
		sendHtmlMail(properties, user.getDisplayEmailAddress(),
				getPropertyValue(EMAIL_MAILER), subject, html);

	}

	@Override
	public void activateOrganization(UUID organizationId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.setProperty(new SimpleEntityRef(Group.ENTITY_TYPE, organizationId),
				"activated", true);
		List<UserInfo> users = getAdminUsersForOrganization(organizationId);
		for (UserInfo user : users) {
			if (!user.isActivated()) {
				activateAdminUser(user.getUuid());
			}
		}
	}

	@Override
	public void deactivateOrganization(UUID organizationId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.setProperty(new SimpleEntityRef(Group.ENTITY_TYPE, organizationId),
				"activated", false);
	}

	@Override
	public boolean isOrganizationActivated(UUID organizationId)
			throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		return Boolean.TRUE.equals(em.getProperty(new SimpleEntityRef(
				Group.ENTITY_TYPE, organizationId), "activated"));
	}

	@Override
	public void enableOrganization(UUID organizationId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.setProperty(new SimpleEntityRef(Group.ENTITY_TYPE, organizationId),
				"disabled", false);
	}

	@Override
	public void disableOrganization(UUID organizationId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		em.setProperty(new SimpleEntityRef(Group.ENTITY_TYPE, organizationId),
				"disabled", true);
	}

	@Override
	public boolean isOrganizationEnabled(UUID organizationId) throws Exception {
		EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
		return !Boolean.TRUE.equals(em.getProperty(new SimpleEntityRef(
				Group.ENTITY_TYPE, organizationId), "disabled"));
	}

	@Override
	public void activateAppUser(UUID applicationId, UUID userId)
			throws Exception {
		EntityManager em = emf.getEntityManager(applicationId);
		em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
				"activated", true);
	}

	@Override
	public boolean checkActivationTokenForAppUser(UUID applicationId,
			UUID userId, String token) throws Exception {
		EntityRef userRef = getEntityRefFromAccessToken(applicationId, token,
				APPLICATION_USER, 0, "activate", true);
		return (userRef != null) && userId.equals(userRef.getUuid());
	}

	@Override
	public boolean checkPasswordResetTokenForAppUser(UUID applicationId,
			UUID userId, String token) throws Exception {
		EntityRef userRef = null;
		try {
			userRef = getEntityRefFromAccessToken(applicationId, token,
					APPLICATION_USER, 0, "resetpw", true);
		} catch (Exception e) {
			logger.error("Unable to verify token", e);
		}
		return (userRef != null) && userId.equals(userRef.getUuid());
	}

	@Override
	public String getAccessTokenForAppUser(UUID applicationId, UUID userId)
			throws Exception {
		return getTokenForPrincipal(applicationId, APPLICATION_USER, userId,
				null, true);
	}

	@Override
	public UserInfo getAppUserFromAccessToken(String token) throws Exception {
		AuthPrincipalInfo auth_principal = AuthPrincipalInfo
				.getFromAccessToken(token);
		if (AuthPrincipalType.APPLICATION_USER.equals(auth_principal.getType())) {
			UUID appId = auth_principal.getApplicationId();
			if (appId != null) {
				EntityManager em = emf.getEntityManager(appId);
				Entity user = em.get(getEntityRefFromAccessToken(appId, token,
						APPLICATION_USER, maxTokenAge, null, true));
				if (user != null) {
					return new UserInfo(appId, user.getProperties());
				}
			}
		}
		return null;
	}

	@Override
	public User getAppUserByIdentifier(UUID applicationId, Identifier identifier)
			throws Exception {
		EntityManager em = emf.getEntityManager(applicationId);
		return em.get(em.getUserByIdentifier(identifier), User.class);
	}

	@Override
	public void sendAppUserPasswordReminderEmail(UUID applicationId, User user)
			throws Exception {
		String token = getPasswordResetTokenForAppUser(applicationId,
				user.getUuid());
		String reset_url = String.format(
				properties.getProperty("usergrid.user.resetpw.url"),
				applicationId.toString(), user.getUuid().toString())
				+ "?token=" + token;
		sendHtmlMail(
				properties,
				user.getDisplayEmailAddress(),
				getPropertyValue(EMAIL_MAILER),
				"Password Reset",
				emailMsg(hashMap("reset_url", reset_url),
						EMAIL_USER_PASSWORD_RESET));

	}

	@Override
	public void sendAppUserActivatedEmail(UUID applicationId, User user)
			throws Exception {
		sendAppUserEmail(user, "User Account Activated",
				getPropertyValue(EMAIL_USER_ACTIVATED));

	}

	@Override
	public void sendAppUserActivationEmail(UUID applicationId, User user)
			throws Exception {
		String token = getActivationTokenForAppUser(applicationId,
				user.getUuid());
		String activation_url = String.format(
				properties.getProperty("usergrid.user.activation.url"),
				applicationId.toString(), user.getUuid().toString())
				+ "?token=" + token;
		sendAppUserEmail(
				user,
				"User Account Confirmation: " + user.getEmail(),
				emailMsg(hashMap("activation_url", activation_url),
						EMAIL_USER_ACTIVATION));
	}

	@Override
	public void setAppUserPassword(UUID applicationId, UUID userId,
			String newPassword) throws Exception {
		if ((userId == null) || (newPassword == null)) {
			return;
		}

		EntityManager em = emf.getEntityManager(applicationId);
		em.addToDictionary(new SimpleEntityRef(User.ENTITY_TYPE, userId),
				DICTIONARY_CREDENTIALS, "password",
				passwordCredentials(newPassword));
	}

	@Override
	public void setAppUserPassword(UUID applicationId, UUID userId,
			String oldPassword, String newPassword) throws Exception {
		if ((userId == null) || (oldPassword == null) || (newPassword == null)) {
			return;
		}

		EntityManager em = emf.getEntityManager(applicationId);
		Entity user = em.get(userId);
		if (!checkPassword(oldPassword,
				(CredentialsInfo) em.getDictionaryElementValue(user,
						DICTIONARY_CREDENTIALS, "password"))) {
			logger.info("Old password doesn't match");
			throw new IncorrectPasswordException();
		}
		em.addToDictionary(user, DICTIONARY_CREDENTIALS, "password",
				passwordCredentials(newPassword));
	}

	@Override
	public User verifyAppUserPasswordCredentials(UUID applicationId,
			String name, String password) throws Exception {

		User user = findUserEntity(applicationId, name);
		if (user == null) {
			return null;
		}

		EntityManager em = emf.getEntityManager(applicationId);
		if (checkPassword(password,
				(CredentialsInfo) em.getDictionaryElementValue(user,
						DICTIONARY_CREDENTIALS, "password"))) {
			if (!user.activated()) {
				throw new UnactivatedAdminUserException();
			}
			if (user.disabled()) {
				throw new DisabledAdminUserException();
			}
			return user;
		}

		return null;
	}

	public String getPasswordResetTokenForAppUser(UUID applicationId,
			UUID userId) throws Exception {
		return getTokenForPrincipal(applicationId, APPLICATION_USER, userId,
				"resetpw", true);
	}

	public void sendAppUserEmail(User user, String subject, String html)
			throws Exception {
		sendHtmlMail(properties, user.getDisplayEmailAddress(),
				getPropertyValue(EMAIL_MAILER), subject, html);

	}

	public String getActivationTokenForAppUser(UUID applicationId, UUID userId)
			throws Exception {
		return getTokenForPrincipal(applicationId, APPLICATION_USER, userId,
				"activate", true);
	}

	@Override
	public void setAppUserPin(UUID applicationId, UUID userId, String newPin)
			throws Exception {
		if ((userId == null) || (newPin == null)) {
			return;
		}

		EntityManager em = emf.getEntityManager(applicationId);
		em.addToDictionary(new SimpleEntityRef(User.ENTITY_TYPE, userId),
				DICTIONARY_CREDENTIALS, "pin", plainTextCredentials(newPin));
	}

	@Override
	public void sendAppUserPin(UUID applicationId, UUID userId)
			throws Exception {
		EntityManager em = emf.getEntityManager(applicationId);
		User user = em.get(userId, User.class);
		if (user == null) {
			return;
		}
		if (user.getEmail() == null) {
			return;
		}
		String pin = getCredentialsSecret((CredentialsInfo) em
				.getDictionaryElementValue(user, DICTIONARY_CREDENTIALS, "pin"));
		sendHtmlMail(properties, user.getDisplayEmailAddress(),
				getPropertyValue(EMAIL_MAILER), "Your app pin",
				emailMsg(hashMap("pin", pin), EMAIL_USER_PIN_REQUEST));

	}

	@Override
	public User verifyAppUserPinCredentials(UUID applicationId, String name,
			String pin) throws Exception {
		EntityManager em = emf.getEntityManager(applicationId);
		User user = findUserEntity(applicationId, name);
		if (user == null) {
			return null;
		}
		if (pin.equals(getCredentialsSecret((CredentialsInfo) em
				.getDictionaryElementValue(user, DICTIONARY_CREDENTIALS, "pin")))) {
			return user;
		}
		return null;
	}

}
