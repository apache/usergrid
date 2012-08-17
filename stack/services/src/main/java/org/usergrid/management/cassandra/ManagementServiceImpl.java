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
package org.usergrid.management.cassandra;

import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.usergrid.persistence.CredentialsInfo.*;
import static org.usergrid.persistence.Schema.DICTIONARY_CREDENTIALS;
import static org.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.usergrid.persistence.Schema.PROPERTY_PATH;
import static org.usergrid.persistence.Schema.PROPERTY_SECRET;
import static org.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.usergrid.persistence.entities.Activity.PROPERTY_ACTOR;
import static org.usergrid.persistence.entities.Activity.PROPERTY_ACTOR_NAME;
import static org.usergrid.persistence.entities.Activity.PROPERTY_CATEGORY;
import static org.usergrid.persistence.entities.Activity.PROPERTY_CONTENT;
import static org.usergrid.persistence.entities.Activity.PROPERTY_DISPLAY_NAME;
import static org.usergrid.persistence.entities.Activity.PROPERTY_ENTITY_TYPE;
import static org.usergrid.persistence.entities.Activity.PROPERTY_OBJECT;
import static org.usergrid.persistence.entities.Activity.PROPERTY_OBJECT_ENTITY_TYPE;
import static org.usergrid.persistence.entities.Activity.PROPERTY_OBJECT_NAME;
import static org.usergrid.persistence.entities.Activity.PROPERTY_OBJECT_TYPE;
import static org.usergrid.persistence.entities.Activity.PROPERTY_TITLE;
import static org.usergrid.persistence.entities.Activity.PROPERTY_VERB;
import static org.usergrid.security.AuthPrincipalType.*;
import static org.usergrid.security.oauth.ClientCredentialsInfo.getTypeFromClientId;
import static org.usergrid.security.oauth.ClientCredentialsInfo.getUUIDFromClientId;
import static org.usergrid.security.tokens.TokenCategory.ACCESS;
import static org.usergrid.security.tokens.TokenCategory.EMAIL;
import static org.usergrid.services.ServiceParameter.parameters;
import static org.usergrid.services.ServicePayload.payload;
import static org.usergrid.utils.ConversionUtils.bytes;
import static org.usergrid.utils.ConversionUtils.uuid;
import static org.usergrid.utils.ListUtils.anyNull;
import static org.usergrid.utils.MapUtils.hashMap;
import static org.usergrid.management.AccountCreationProps.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.locking.LockManager;
import org.usergrid.management.*;
import org.usergrid.management.exceptions.DisabledAdminUserException;
import org.usergrid.management.exceptions.IncorrectPasswordException;
import org.usergrid.management.exceptions.UnableToLeaveOrganizationException;
import org.usergrid.management.exceptions.UnactivatedAdminUserException;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Query;
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
import org.usergrid.security.tokens.TokenCategory;
import org.usergrid.security.tokens.TokenInfo;
import org.usergrid.security.tokens.TokenService;
import org.usergrid.security.tokens.exceptions.BadTokenException;
import org.usergrid.security.tokens.exceptions.TokenException;
import org.usergrid.services.ServiceAction;
import org.usergrid.services.ServiceManager;
import org.usergrid.services.ServiceManagerFactory;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;
import org.usergrid.utils.ConversionUtils;
import org.usergrid.utils.JsonUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.usergrid.utils.MailUtils;

public class ManagementServiceImpl implements ManagementService {

    /**
     * Key for the user's pin
     */
    protected static final String USER_PIN = "pin";

    /**
     * Key for the user's oauth secret
     */
    protected static final String USER_TOKEN = "secret";

    /**
     * Key for the user's mongo password
     */
    protected static final String USER_MONGO_PASSWORD = "mongo_pwd";

    /**
     * Key for the user's password
     */
    protected static final String USER_PASSWORD = "password";
    
    

    private static final String TOKEN_TYPE_ACTIVATION = "activate";

    private static final String TOKEN_TYPE_PASSWORD_RESET = "resetpw";

    private static final String TOKEN_TYPE_CONFIRM = "confirm";

    public static final String MANAGEMENT_APPLICATION = "management";

    public static final String APPLICATION_INFO = "application_info";

    private static final Logger logger = LoggerFactory
            .getLogger(ManagementServiceImpl.class);

    public static final String OAUTH_SECRET_SALT = "super secret oauth value";

    protected ServiceManagerFactory smf;

    protected EntityManagerFactory emf;

    protected AccountCreationPropsImpl properties;

    protected LockManager lockManager;

    protected TokenService tokens;

    /**
     * Must be constructed with a CassandraClientPool.
     * 
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
        this.properties = new AccountCreationPropsImpl(properties);
    }

    @Autowired
    public void setTokenService(TokenService tokens) {
        this.tokens = tokens;
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

    @Override
    public void setup() throws Exception {

        if (parseBoolean(properties.getProperty(PROPERTIES_SETUP_TEST_ACCOUNT))) {
            String test_app_name = properties.getProperty(PROPERTIES_TEST_ACCOUNT_APP);
            String test_organization_name = properties.getProperty(PROPERTIES_TEST_ACCOUNT_ORGANIZATION);
            String test_admin_username = properties.getProperty(PROPERTIES_TEST_ACCOUNT_ADMIN_USER_USERNAME);
            String test_admin_name = properties.getProperty(PROPERTIES_TEST_ACCOUNT_ADMIN_USER_NAME);
            String test_admin_email = properties.getProperty(PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL);
            String test_admin_password = properties.getProperty(PROPERTIES_TEST_ACCOUNT_ADMIN_USER_PASSWORD);

            if (anyNull(test_app_name, test_organization_name,
                    test_admin_username, test_admin_name, test_admin_email,
                    test_admin_password)) {
                logger.warn("Missing values for test app, check properties.  Skipping test app setup...");
                return;
            }

          UserInfo user = createAdminUser(test_admin_username,
                  test_admin_name, test_admin_email, test_admin_password,
                  true, false);

          OrganizationInfo organization = createOrganization(
                    test_organization_name, user, true);


            UUID appId = createApplication(organization.getUuid(),
                    buildAppName(test_app_name, organization))
                    .getId();

            postOrganizationActivity(organization.getUuid(), user, "create",
                    new SimpleEntityRef(APPLICATION_INFO, appId),
                    "Application", test_app_name,
                    "<a mailto=\"" + user.getEmail() + "\">" + user.getName()
                            + " (" + user.getEmail()
                            + ")</a> created a new application named "
                            + test_app_name, null);

            boolean superuser_enabled = parseBoolean(properties
                    .getProperty(PROPERTIES_SYSADMIN_LOGIN_ALLOWED));
            String superuser_username = properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_NAME);
            String superuser_email = properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_EMAIL);
            String superuser_password = properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_PASSWORD);

            if (!anyNull(superuser_username, superuser_email,
                    superuser_password)) {
                user = createAdminUser(superuser_username, "Super User",
                        superuser_email, superuser_password, superuser_enabled,
                        !superuser_enabled);
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
        properties.put(PROPERTY_VERB, verb);
        properties.put(PROPERTY_CATEGORY, "admin");
        if (content != null) {
            properties.put(PROPERTY_CONTENT, content);
        }
        if (title != null) {
            properties.put(PROPERTY_TITLE, title);
        }
        properties.put(PROPERTY_ACTOR, new HashMap<String, Object>() {
            {
                put(PROPERTY_DISPLAY_NAME, user.getName());
                put(PROPERTY_OBJECT_TYPE, "person");
                put(PROPERTY_ENTITY_TYPE, "user");
                put(PROPERTY_UUID, user.getUuid());
            }
        });
        properties.put(PROPERTY_OBJECT, new HashMap<String, Object>() {
            {
                put(PROPERTY_DISPLAY_NAME, objectName);
                put(PROPERTY_OBJECT_TYPE, objectType);
                put(PROPERTY_ENTITY_TYPE, object.getType());
                put(PROPERTY_UUID, object.getUuid());
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
            String email, String password) throws Exception {

        boolean activated = !newAdminUsersNeedSysAdminApproval()
                && !newOrganizationsNeedSysAdminApproval();
        boolean disabled = newAdminUsersRequireConfirmation();
        // if we are active and enabled, skip the send email step

        return createOwnerAndOrganization(organizationName, username, name,
                email, password, activated, disabled);

    }

    @Override
    public OrganizationOwnerInfo createOwnerAndOrganization(
            String organizationName, String username, String name,
            String email, String password, boolean activated, boolean disabled) throws Exception {

        lockManager.lockProperty(MANAGEMENT_APPLICATION_ID, "groups", "path");
        lockManager.lockProperty(MANAGEMENT_APPLICATION_ID, "users",
                "username", "email");

        UserInfo user = null;
        OrganizationInfo organization = null;

        try {
            if (areActivationChecksDisabled()) {
                user = createAdminUser(username, name, email, password, true, false);
            } else {
                user = createAdminUser(username, name, email, password,
                        activated, disabled);
            }

            organization = createOrganization(organizationName, user, true);

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
            UserInfo user, boolean activated) throws Exception {

        if ((organizationName == null) || (user == null)) {
            return null;
        }
        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        if (!em.isPropertyValueUniqueForEntity("group", "path",
                organizationName)) {
            throw new DuplicateUniquePropertyExistsException("group", "path",
                    organizationName);
        }
        Group organizationEntity = new Group();
        organizationEntity.setPath(organizationName);
        organizationEntity = em.create(organizationEntity);

        em.addToCollection(organizationEntity, "users", new SimpleEntityRef(
                User.ENTITY_TYPE, user.getUuid()));

        writeUserToken(
                MANAGEMENT_APPLICATION_ID,
                organizationEntity,
                plainTextCredentials(generateOAuthSecretKey(AuthPrincipalType.ORGANIZATION)));

        OrganizationInfo organization = new OrganizationInfo(
                organizationEntity.getUuid(), organizationName);
        postOrganizationActivity(organization.getUuid(), user, "create",
                organizationEntity, "Organization", organization.getName(),
                "<a mailto=\"" + user.getEmail() + "\">" + user.getName()
                        + " (" + user.getEmail()
                        + ")</a> created a new organization account named "
                        + organizationName, null);

        startOrganizationActivationFlow(organization);

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
        // TODO organizationName
        OrganizationInfo organization = getOrganizationByUuid(organizationId);
        UUID applicationId = emf.importApplication(organization.getName(),
                application.getUuid(), application.getName(),
                application.getProperties());

        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        properties.setProperty("name",
                buildAppName(application.getName(), organization));
        Entity app = em.create(applicationId, APPLICATION_INFO,
                application.getProperties());

        writeUserToken(
                MANAGEMENT_APPLICATION_ID,
                app,
                plainTextCredentials(generateOAuthSecretKey(AuthPrincipalType.APPLICATION)));

        addApplicationToOrganization(organizationId, applicationId);
        return applicationId;
    }

    /**
     * Test if the applicationName contains a '/' character, prepend with
     * orgName if it does not, assume it is complete (and that organization is
     * needed) if so.
     *
     * @param applicationName
     * @param organization
     * @return
     */
    private String buildAppName(String applicationName,
            OrganizationInfo organization) {
        return applicationName.contains("/") ? applicationName : organization
                .getName() + "/" + applicationName;
    }

    @Override
    public BiMap<UUID, String> getOrganizations() throws Exception {

        BiMap<UUID, String> organizations = HashBiMap.create();
        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        Results results = em.getCollection(em.getApplicationRef(), "groups",
                null, 10000, Level.ALL_PROPERTIES, false);
        for (Entity entity : results.getEntities()) {

            // TODO T.N. temporary hack to deal with duplicate orgs. Revert this
            // commit after migration
            String path = (String) entity.getProperty("path");

            if (organizations.containsValue(path)) {
                path += "DUPLICATE";
            }

            organizations.put(entity.getUuid(), path);
        }
        return organizations;
    }

    @Override
    public OrganizationInfo getOrganizationInfoFromAccessToken(String token)
            throws Exception {
        Entity entity = geEntityFromAccessToken(token, null, ORGANIZATION);
        if (entity == null) {
            return null;
        }
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
        properties.put(PROPERTY_VERB, verb);
        properties.put(PROPERTY_CATEGORY, "admin");
        if (content != null) {
            properties.put(PROPERTY_CONTENT, content);
        }
        if (title != null) {
            properties.put(PROPERTY_TITLE, title);
        }
        properties.put(PROPERTY_ACTOR, user.getUuid());
        properties.put(PROPERTY_ACTOR_NAME, user.getName());
        properties.put(PROPERTY_OBJECT, object.getUuid());
        properties.put(PROPERTY_OBJECT_ENTITY_TYPE, object.getType());
        properties.put(PROPERTY_OBJECT_TYPE, objectType);
        properties.put(PROPERTY_OBJECT_NAME, objectName);

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


    private UserInfo doCreateAdmin(User user, CredentialsInfo userPassword, CredentialsInfo mongoPassword)
            throws Exception {

        writeUserToken(
                MANAGEMENT_APPLICATION_ID,
                user,
                plainTextCredentials(generateOAuthSecretKey(AuthPrincipalType.ADMIN_USER)));

        writeUserPassword(MANAGEMENT_APPLICATION_ID, user, userPassword);

        writeUserMongoPassword(MANAGEMENT_APPLICATION_ID, user, mongoPassword);

        UserInfo userInfo = new UserInfo(MANAGEMENT_APPLICATION_ID,
                user.getUuid(), user.getUsername(), user.getName(),
                user.getEmail(), user.getActivated(), user.getDisabled());

        // special case for sysadmin only
        if (!user.getEmail().equals(properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_EMAIL))) {
          this.startAdminUserActivationFlow(userInfo);
        }

        return userInfo;
    }

    @Override
    public UserInfo createAdminFromPrexistingPassword(User user,
            String precypheredPassword, String hashType) throws Exception {

        CredentialsInfo ci = new CredentialsInfo();
        ci.setRecoverable(false);
        ci.setCipher("sha-1");
        ci.setSecret(precypheredPassword);
        ci.setRecoverable(false);
        ci.setEncrypted(true);
        ci.setHashType(hashType);

        return doCreateAdmin(
                user,
                ci,
                mongoPasswordCredentials(user.getUsername(),
                        precypheredPassword));

    }

    @Override
    public UserInfo createAdminFrom(User user, String password) throws Exception {
        return doCreateAdmin(user, maybeSaltPassword(password),
                mongoPasswordCredentials(user.getUsername(), password));
    }

    @Override
    public UserInfo createAdminUser(String username, String name, String email,
            String password, boolean activated, boolean disabled) throws Exception {

        if (email == null) {
            return null;
        }
        if (username == null) {
            username = email;
        }
        if (name == null) {
            name = email;
        }

        if (isBlank(password)) {
            password = encodeBase64URLSafeString(bytes(UUID.randomUUID()));
        }

        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

        if (!em.isPropertyValueUniqueForEntity("user", "username", username)) {
            throw new DuplicateUniquePropertyExistsException("user",
                    "username", username);
        }

        if (!em.isPropertyValueUniqueForEntity("user", "email", email)) {
            throw new DuplicateUniquePropertyExistsException("user",
                    "username", username);
        }

        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setEmail(email);
        user.setActivated(activated);
        user.setConfirmed(!newAdminUsersRequireConfirmation()); // only
                                                                // hardcoded
                                                                // param now
                                                                // checked
                                                                // against
                                                                // config
        user.setDisabled(disabled);
        user = em.create(user);

        return createAdminFrom(user, password);
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
                logger.info("Found user {} as a UUID", identifier);
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
                    Identifier.fromEmail(identifier));
            if (entity != null) {
                user = (User) entity.toTypedEntity();
                logger.info("Found user {} as an email address", identifier);
            }
        } catch (Exception e) {
            logger.error("Unable to get user " + identifier
                    + " as an email address, trying username");
        }
        if (user != null) {
            return user;
        }

        try {
            Entity entity = getUserEntityByIdentifier(applicationId,
                    Identifier.fromName(identifier));
            if (entity != null) {
                user = (User) entity.toTypedEntity();
                logger.info("Found user {} as a username", identifier);
            }
        } catch (Exception e) {
            logger.error("Unable to get user " + identifier
                    + " as a username, failed...");
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

        if (!maybeSaltPassword(oldPassword)
                .compare(readUserPasswordCredentials(MANAGEMENT_APPLICATION_ID, userId))) {
            logger.info("Old password doesn't match");
            throw new IncorrectPasswordException("Old password does not match");
        }

        setAdminUserPassword(userId, newPassword);
    }

    @Override
    public void setAdminUserPassword(UUID userId, String newPassword)
            throws Exception {

        if ((userId == null) || (newPassword == null)) {
            return;
        }

        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        Entity user = em.get(userId);

        writeUserPassword(MANAGEMENT_APPLICATION_ID, user,
                maybeSaltPassword(newPassword));
        writeUserMongoPassword(
                MANAGEMENT_APPLICATION_ID,
                user,
                mongoPasswordCredentials((String) user.getProperty("username"),
                        newPassword));
    }

    @Override
    public boolean verifyAdminUserPassword(UUID userId, String password)
            throws Exception {
        if ((userId == null) || (password == null)) {
            return false;
        }

        return maybeSaltPassword(password)
                .compare(readUserPasswordCredentials(MANAGEMENT_APPLICATION_ID, userId));
    }

    @Override
    public UserInfo verifyAdminUserPasswordCredentials(String name,
            String password) throws Exception {
        UserInfo userInfo = null;

        User user = findUserEntity(MANAGEMENT_APPLICATION_ID, name);
        if (user == null) {
            return null;
        }

        if (maybeSaltPassword(password)
                .compare(readUserPasswordCredentials(MANAGEMENT_APPLICATION_ID,user.getUuid()))) {
            userInfo = getUserInfo(MANAGEMENT_APPLICATION_ID, user);
            if (!userInfo.isActivated()) {
                throw new UnactivatedAdminUserException();
            }
            if (userInfo.isDisabled()) {
                throw new DisabledAdminUserException();
            }
            return userInfo;
        }
        logger.info("password compare fail for {}", name);
        return null;

    }

    @Override
    public UserInfo verifyMongoCredentials(String name, String nonce, String key)
            throws Exception {

        Entity user = findUserEntity(MANAGEMENT_APPLICATION_ID, name);

        if (user == null) {
            return null;
        }
        String mongo_pwd = readUserMongoPassword(MANAGEMENT_APPLICATION_ID, user.getUuid()).getSecret();
   

        if (mongo_pwd == null) {
            throw new IncorrectPasswordException(
                    "Your mongo password has not be set");
        }

        String expected_key = DigestUtils.md5Hex(nonce
                + user.getProperty("username") + mongo_pwd);
        
        if (!expected_key.equalsIgnoreCase(key)) {
            throw new IncorrectPasswordException();
        }

        
        UserInfo userInfo = new UserInfo(MANAGEMENT_APPLICATION_ID,
                user.getProperties());
        
  
        if (!userInfo.isActivated()) {
            throw new UnactivatedAdminUserException();
        }
        if (userInfo.isDisabled()) {
            throw new DisabledAdminUserException();
        }
        
        
        return userInfo;
    }

    // TokenType tokenType, String type, AuthPrincipalInfo principal,
    // Map<String, Object> state
    public String getTokenForPrincipal(TokenCategory token_category,
            String token_type, UUID applicationId,
            AuthPrincipalType principal_type, UUID id) throws Exception {

        if (anyNull(token_category, applicationId, principal_type, id)) {
            return null;
        }

        return tokens.createToken(token_category, token_type,
                new AuthPrincipalInfo(principal_type, id, applicationId), null);

    }

    public AuthPrincipalInfo getPrincipalFromAccessToken(String token,
            String expected_token_type,
            AuthPrincipalType expected_principal_type) throws Exception {

        TokenInfo tokenInfo = tokens.getTokenInfo(token);

        if (tokenInfo == null) {
            return null;
        }

        if ((expected_token_type != null)
                && !expected_token_type.equals(tokenInfo.getType())) {
            return null;
        }

        AuthPrincipalInfo principal = tokenInfo.getPrincipal();
        if (principal == null) {
            return null;
        }

        if ((expected_principal_type != null)
                && !expected_principal_type.equals(principal.getType())) {
            return null;
        }

        return principal;
    }

    public Entity geEntityFromAccessToken(String token,
            String expected_token_type,
            AuthPrincipalType expected_principal_type) throws Exception {

        AuthPrincipalInfo principal = getPrincipalFromAccessToken(token,
                expected_token_type, expected_principal_type);
        if (principal == null) {
            return null;
        }

        return geEntityFromPrincipal(principal);
    }

    public Entity geEntityFromPrincipal(AuthPrincipalInfo principal)
            throws Exception {

        EntityManager em = emf
                .getEntityManager(principal.getApplicationId() != null ? principal
                        .getApplicationId() : MANAGEMENT_APPLICATION_ID);
        Entity entity = em.get(principal.getUuid());
        return entity;
    }

    @Override
    public String getAccessTokenForAdminUser(UUID userId) throws Exception {

        return getTokenForPrincipal(ACCESS, null, MANAGEMENT_APPLICATION_ID,
                ADMIN_USER, userId);
    }

    @Override
    public Entity getAdminUserEntityFromAccessToken(String token)
            throws Exception {

        Entity user = geEntityFromAccessToken(token, null, ADMIN_USER);
        return user;
    }

    @Override
    public UserInfo getAdminUserInfoFromAccessToken(String token)
            throws Exception {
        Entity user = getAdminUserEntityFromAccessToken(token);
        return new UserInfo(MANAGEMENT_APPLICATION_ID, user.getProperties());
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

        String path = null;

        for (Entity entity : results.getEntities()) {

            path = (String) entity.getProperty("path");

            if (path != null) {
                path = path.toLowerCase();
            }

            organizations.put(entity.getUuid(), path);
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
    public void addAdminUserToOrganization(UserInfo user,
            OrganizationInfo organization, boolean email) throws Exception {

        if ((user == null) || (organization == null)) {
            return;
        }

        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        em.addToCollection(
                new SimpleEntityRef(Group.ENTITY_TYPE, organization.getUuid()),
                "users", new SimpleEntityRef(User.ENTITY_TYPE, user.getUuid()));

        if (email) {
            sendAdminUserInvitedEmail(user, organization);
        }
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
    public ApplicationInfo createApplication(UUID organizationId, String applicationName)
            throws Exception {

        return createApplication(organizationId, applicationName, null);
    }

    @Override
    public ApplicationInfo createApplication(UUID organizationId, String applicationName,
            Map<String, Object> properties) throws Exception {

        if ((organizationId == null) || (applicationName == null)) {
            return null;
        }

        if (properties == null) {
            properties = new HashMap<String, Object>();
        }

        OrganizationInfo organizationInfo = getOrganizationByUuid(organizationId);

        UUID applicationId = emf.createApplication(organizationInfo.getName(),
                applicationName, properties);

        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        properties.put("name", buildAppName(applicationName, organizationInfo));
        Entity applicationEntity = em.create(applicationId, APPLICATION_INFO,
                properties);

        writeUserToken(
                MANAGEMENT_APPLICATION_ID,
                applicationEntity,
                plainTextCredentials(generateOAuthSecretKey(AuthPrincipalType.APPLICATION)));
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
        return new ApplicationInfo(applicationId, applicationEntity.getName());
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

            String entityName = null;

            for (Entity entity : results.getEntities()) {
                entityName = entity.getName();

                if (entityName != null) {
                    entityName = entityName.toLowerCase();
                }

                applications.put(entity.getUuid(), entityName);

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
    public ApplicationInfo getApplicationInfo(String applicationName)
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
    public ApplicationInfo getApplicationInfo(UUID applicationId)
            throws Exception {
        if (applicationId == null) {
            return null;
        }
        Entity entity = getApplicationInfoEntityById(applicationId);
        if (entity != null) {
            return new ApplicationInfo(applicationId, entity.getName());
        }
        return null;
    }

    @Override
    public ApplicationInfo getApplicationInfo(Identifier id) throws Exception {
        if (id == null) {
            return null;
        }
        if (id.isUUID()) {
            return getApplicationInfo(id.getUUID());
        }
        if (id.isName()) {
            return getApplicationInfo(id.getName());
        }
        return null;
    }

    @Override
    public ApplicationInfo getApplicationInfoFromAccessToken(String token)
            throws Exception {
        Entity entity = geEntityFromAccessToken(token, null, APPLICATION);
        if (entity == null) {
            throw new TokenException(
                    "Could not find an entity for that access token: " + token);
        }
        return new ApplicationInfo(entity.getProperties());
    }

    public Entity getApplicationInfoEntityById(UUID applicationId)
            throws Exception {
        if (applicationId == null) {
            return null;
        }
        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        Entity entity = em.get(applicationId);
        return entity;
    }

    public String getSecret(UUID applicationId, AuthPrincipalType type, UUID id)
            throws Exception {
        if (AuthPrincipalType.ORGANIZATION.equals(type)
                || AuthPrincipalType.APPLICATION.equals(type)) {
            UUID ownerId = AuthPrincipalType.APPLICATION_USER.equals(type) ? applicationId
                    : MANAGEMENT_APPLICATION_ID;

            return getCredentialsSecret(readUserToken(ownerId, id));

        } else if (AuthPrincipalType.ADMIN_USER.equals(type)
                || AuthPrincipalType.APPLICATION_USER.equals(type)) {
            return getCredentialsSecret(readUserPasswordCredentials(applicationId, id));
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
        String secret = generateOAuthSecretKey(type);

        writeUserToken(MANAGEMENT_APPLICATION_ID,
                new SimpleEntityRef(type.getEntityType(), id),
                plainTextCredentials(secret));

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
                ApplicationInfo app = getApplicationInfo(uuid);
                access_info = new AccessInfo()
                        .withExpiresIn(3600)
                        .withAccessToken(
                                getTokenForPrincipal(ACCESS, null,
                                        MANAGEMENT_APPLICATION_ID, type, uuid))
                        .withProperty("application", app.getId());
            } else if (type.equals(AuthPrincipalType.ORGANIZATION)) {
                OrganizationInfo organization = getOrganizationByUuid(uuid);
                access_info = new AccessInfo()
                        .withExpiresIn(3600)
                        .withAccessToken(
                                getTokenForPrincipal(ACCESS, null,
                                        MANAGEMENT_APPLICATION_ID, type, uuid))
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
                ApplicationInfo app = getApplicationInfo(uuid);
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
        return getTokenForPrincipal(EMAIL, TOKEN_TYPE_PASSWORD_RESET,
                MANAGEMENT_APPLICATION_ID, ADMIN_USER, userId);
    }

    @Override
    public boolean checkPasswordResetTokenForAdminUser(UUID userId, String token)
            throws Exception {
        AuthPrincipalInfo principal = null;
        try {
            principal = getPrincipalFromAccessToken(token,
                    TOKEN_TYPE_PASSWORD_RESET, ADMIN_USER);
        } catch (Exception e) {
            logger.error("Unable to verify token", e);
        }
        return (principal != null) && userId.equals(principal.getUuid());
    }

    @Override
    public String getActivationTokenForAdminUser(UUID userId) throws Exception {
        return getTokenForPrincipal(EMAIL, TOKEN_TYPE_ACTIVATION,
                MANAGEMENT_APPLICATION_ID, ADMIN_USER, userId);
    }

    @Override
    public String getConfirmationTokenForAdminUser(UUID userId)
            throws Exception {
        return getTokenForPrincipal(EMAIL, TOKEN_TYPE_CONFIRM,
                MANAGEMENT_APPLICATION_ID, ADMIN_USER, userId);
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
    public void confirmAdminUser(UUID userId) throws Exception {
        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
                "confirmed", true);
    }

    @Override
    public void unconfirmAdminUser(UUID userId) throws Exception {
        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
                "confirmed", false);
    }

    @Override
    public boolean isAdminUserConfirmed(UUID userId) throws Exception {
        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        return Boolean.TRUE.equals(em.getProperty(new SimpleEntityRef(
                User.ENTITY_TYPE, userId), "confirmed"));
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

    private String appendEmailFooter(String msg) {
        return msg + "\n" + properties.getProperty(PROPERTIES_EMAIL_FOOTER);
    }

    @Override
    public void startAdminUserPasswordResetFlow(UserInfo user) throws Exception {
        String token = getPasswordResetTokenForAdminUser(user.getUuid());
        String reset_url = String.format(properties
                .getProperty(PROPERTIES_ADMIN_RESETPW_URL), user.getUuid()
                .toString())
                + "?token=" + token;

        sendHtmlMail(
                properties,
                user.getDisplayEmailAddress(),
                properties.getProperty(PROPERTIES_MAILER_EMAIL),
                "Password Reset",
                appendEmailFooter(emailMsg(hashMap("reset_url", reset_url),
                        PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET)));

    }

    @Override
    public String getActivationTokenForOrganization(UUID organizationId)
            throws Exception {
        return getTokenForPrincipal(EMAIL, TOKEN_TYPE_ACTIVATION,
                MANAGEMENT_APPLICATION_ID, ORGANIZATION, organizationId);
    }

    @Override
    public void startOrganizationActivationFlow(OrganizationInfo organization)
            throws Exception {
        try {
            String token = getActivationTokenForOrganization(organization
                    .getUuid());
            String activation_url = String.format(properties
                    .getProperty(PROPERTIES_ORGANIZATION_ACTIVATION_URL),
                    organization.getUuid().toString())
                    + "?token=" + token;
            List<UserInfo> users = getAdminUsersForOrganization(organization
                    .getUuid());
            String organization_owners = null;
            for (UserInfo user : users) {
                organization_owners = (organization_owners == null) ? user
                        .getHTMLDisplayEmailAddress() : organization_owners
                        + ", " + user.getHTMLDisplayEmailAddress();
            }
            if (newOrganizationsNeedSysAdminApproval()) {
                sendHtmlMail(
                        properties,
                        properties.getProperty(PROPERTIES_SYSADMIN_EMAIL),
                        properties.getProperty(PROPERTIES_MAILER_EMAIL),
                        "Request For Organization Account Activation "
                                + organization.getName(),
                        appendEmailFooter(emailMsg(
                                hashMap("organization_name",
                                        organization.getName()).map(
                                        "activation_url", activation_url).map(
                                        "organization_owners",
                                        organization_owners),
                                PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION)));
                sendOrganizationEmail(
                        organization,
                        "Organization Account Confirmed",
                        emailMsg(
                                hashMap("organization_name",
                                        organization.getName()),
                                PROPERTIES_EMAIL_ORGANIZATION_CONFIRMED_AWAITING_ACTIVATION));
            } else if (properties.newOrganizationsRequireConfirmation()) {
                sendOrganizationEmail(
                        organization,
                        "Organization Account Confirmation",
                        emailMsg(
                                hashMap("organization_name",
                                        organization.getName()).map(
                                        "confirmation_url", activation_url),
                                PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION));
                sendSysAdminNewOrganizationActivatedNotificationEmail(organization);
            } else {
                activateOrganization(organization, false);
                sendSysAdminNewOrganizationActivatedNotificationEmail(organization);
            }
        } catch (Exception e) {
            logger.error(
                    "Unable to send activation emails to "
                            + organization.getName(), e);
        }

    }

    @Override
    public ActivationState handleActivationTokenForOrganization(
            UUID organizationId, String token) throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken(token,
                TOKEN_TYPE_ACTIVATION, ORGANIZATION);
        if ((principal != null) && organizationId.equals(principal.getUuid())) {
            OrganizationInfo organization = this
                    .getOrganizationByUuid(organizationId);
            sendOrganizationActivatedEmail(organization);
            sendSysAdminNewOrganizationActivatedNotificationEmail(organization);

            activateOrganization(organization, false);

            return ActivationState.ACTIVATED;
        }
        return ActivationState.UNKNOWN;
    }

    public void sendOrganizationActivatedEmail(OrganizationInfo organization)
            throws Exception {
        sendOrganizationEmail(
                organization,
                "Organization Account Activated: " + organization.getName(),
                emailMsg(hashMap("organization_name", organization.getName()),
                        PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED));

    }

    public void sendSysAdminNewOrganizationActivatedNotificationEmail(
            OrganizationInfo organization) throws Exception {
        if (properties.notifySysAdminOfNewOrganizations()) {
            List<UserInfo> users = getAdminUsersForOrganization(organization
                    .getUuid());
            String organization_owners = null;
            for (UserInfo user : users) {
                organization_owners = (organization_owners == null) ? user
                        .getHTMLDisplayEmailAddress() : organization_owners
                        + ", " + user.getHTMLDisplayEmailAddress();
            }
            sendHtmlMail(
                    properties,
                    properties.getProperty(PROPERTIES_SYSADMIN_EMAIL),
                    properties.getProperty(PROPERTIES_MAILER_EMAIL),
                    "Organization Account Activated " + organization.getName(),
                    appendEmailFooter(emailMsg(
                            hashMap("organization_name", organization.getName())
                                    .map("organization_owners",
                                            organization_owners),
                            PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATED)));
        }

    }

    @Override
    public void sendOrganizationEmail(OrganizationInfo organization,
            String subject, String html) throws Exception {
        List<UserInfo> users = getAdminUsersForOrganization(organization
                .getUuid());
        for (UserInfo user : users) {
            sendHtmlMail(properties, user.getDisplayEmailAddress(),
                    properties.getProperty(PROPERTIES_MAILER_EMAIL), subject,
                    appendEmailFooter(html));
        }

    }

    @Override
    public void startAdminUserActivationFlow(UserInfo user) throws Exception {
        if (user.isActivated()) {
          sendAdminUserActivatedEmail(user);
          sendSysAdminNewAdminActivatedNotificationEmail(user);
        } else {
            if (newAdminUsersRequireConfirmation()) {
                sendAdminUserConfirmationEmail(user);
            } else if (newAdminUsersNeedSysAdminApproval()) {
                sendSysAdminRequestAdminActivationEmail(user);
            } else {
              // sdg: There seems to be a hole in the logic. The user has been created
              // in an inactive state but nobody is being notified.
              activateAdminUser(user.getUuid());
            }
        }
    }

    @Override
    public ActivationState handleConfirmationTokenForAdminUser(UUID userId,
            String token) throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken(token,
                TOKEN_TYPE_CONFIRM, ADMIN_USER);
        if ((principal != null) && userId.equals(principal.getUuid())) {
            UserInfo user = getAdminUserByUuid(principal.getUuid());
            confirmAdminUser(user.getUuid());
            if (newAdminUsersNeedSysAdminApproval()) {
                sendAdminUserConfirmedAwaitingActivationEmail(user);
                sendSysAdminRequestAdminActivationEmail(user);
                return ActivationState.CONFIRMED_AWAITING_ACTIVATION;
            } else {
                activateAdminUser(principal.getUuid());
                sendAdminUserActivatedEmail(user);
                sendSysAdminNewAdminActivatedNotificationEmail(user);
                return ActivationState.ACTIVATED;
            }
        }
        return ActivationState.UNKNOWN;
    }

    @Override
    public ActivationState handleActivationTokenForAdminUser(UUID userId,
            String token) throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken(token,
                TOKEN_TYPE_ACTIVATION, ADMIN_USER);
        if ((principal != null) && userId.equals(principal.getUuid())) {
            activateAdminUser(principal.getUuid());
            UserInfo user = getAdminUserByUuid(principal.getUuid());
            sendAdminUserActivatedEmail(user);
            sendSysAdminNewAdminActivatedNotificationEmail(user);
            return ActivationState.ACTIVATED;
        }
        return ActivationState.UNKNOWN;
    }

    public void sendAdminUserConfirmationEmail(UserInfo user) throws Exception {
        String token = getConfirmationTokenForAdminUser(user.getUuid());
        String confirmation_url = String.format(properties
                .getProperty(PROPERTIES_ADMIN_CONFIRMATION_URL), user.getUuid()
                .toString())
                + "?token=" + token;
        sendAdminUserEmail(
                user,
                "User Account Confirmation: " + user.getEmail(),
                emailMsg(
                        hashMap("user_email", user.getEmail()).map(
                                "confirmation_url", confirmation_url),
                        PROPERTIES_EMAIL_ADMIN_CONFIRMATION));

    }

    public void sendSysAdminRequestAdminActivationEmail(UserInfo user)
            throws Exception {
        String token = getActivationTokenForAdminUser(user.getUuid());
        String activation_url = String.format(properties
                .getProperty(PROPERTIES_ADMIN_ACTIVATION_URL), user.getUuid()
                .toString())
                + "?token=" + token;
        sendHtmlMail(
                properties,
                properties.getProperty(PROPERTIES_SYSADMIN_EMAIL),
                properties.getProperty(PROPERTIES_MAILER_EMAIL),
                "Request For Admin User Account Activation " + user.getEmail(),
                appendEmailFooter(emailMsg(
                        hashMap("user_email", user.getEmail()).map(
                                "activation_url", activation_url),
                        PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION)));
    }

    public void sendSysAdminNewAdminActivatedNotificationEmail(UserInfo user)
            throws Exception {
        if (properties.notifySysAdminOfNewAdminUsers()) {
            sendHtmlMail(
                    properties,
                    properties.getProperty(PROPERTIES_SYSADMIN_EMAIL),
                    properties.getProperty(PROPERTIES_MAILER_EMAIL),
                    "Admin User Account Activated " + user.getEmail(),
                    appendEmailFooter(emailMsg(
                            hashMap("user_email", user.getEmail()),
                            PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATED)));
        }
    }

    public void sendAdminUserConfirmedAwaitingActivationEmail(UserInfo user)
            throws Exception {
        sendAdminUserEmail(
                user,
                "User Account Confirmed",
                properties.getProperty(PROPERTIES_EMAIL_ADMIN_CONFIRMED_AWAITING_ACTIVATION));

    }

    public void sendAdminUserActivatedEmail(UserInfo user) throws Exception {
        if (properties.notifyAdminOfActivation()) {
            sendAdminUserEmail(user, "User Account Activated",
                    properties.getProperty(PROPERTIES_EMAIL_ADMIN_ACTIVATED));
        }
    }

    public void sendAdminUserInvitedEmail(UserInfo user,
            OrganizationInfo organization) throws Exception {
        sendAdminUserEmail(
                user,
                "User Invited To Organization",
                emailMsg(hashMap("organization_name", organization.getName()),
                        PROPERTIES_EMAIL_ADMIN_INVITED));

    }

    @Override
    public void sendAdminUserEmail(UserInfo user, String subject, String html)
            throws Exception {
        sendHtmlMail(properties, user.getDisplayEmailAddress(),
                properties.getProperty(PROPERTIES_MAILER_EMAIL), subject,
                appendEmailFooter(html));

    }

    @Override
    public void activateOrganization(OrganizationInfo organization)
            throws Exception {
        activateOrganization(organization, true);
    }

     private void activateOrganization(OrganizationInfo organization,
            boolean sendEmail) throws Exception {
        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        em.setProperty(
                new SimpleEntityRef(Group.ENTITY_TYPE, organization.getUuid()),
                "activated", true);
        List<UserInfo> users = getAdminUsersForOrganization(organization
                .getUuid());
        for (UserInfo user : users) {
            if (!user.isActivated()) {
                activateAdminUser(user.getUuid());
            }
        }
        if (sendEmail) {
            startOrganizationActivationFlow(organization);
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
    public boolean checkPasswordResetTokenForAppUser(UUID applicationId,
            UUID userId, String token) throws Exception {
        AuthPrincipalInfo principal = null;
        try {
            principal = getPrincipalFromAccessToken(token,
                    TOKEN_TYPE_PASSWORD_RESET, APPLICATION_USER);
        } catch (Exception e) {
            logger.error("Unable to verify token", e);
        }
        return (principal != null) && userId.equals(principal.getUuid());
    }

    @Override
    public String getAccessTokenForAppUser(UUID applicationId, UUID userId)
            throws Exception {
        return getTokenForPrincipal(ACCESS, null, applicationId,
                APPLICATION_USER, userId);
    }

    @Override
    public UserInfo getAppUserFromAccessToken(String token) throws Exception {
        AuthPrincipalInfo auth_principal = getPrincipalFromAccessToken(token,
                null, APPLICATION_USER);
        if (auth_principal == null) {
            return null;
        }
        UUID appId = auth_principal.getApplicationId();
        if (appId != null) {
            EntityManager em = emf.getEntityManager(appId);
            Entity user = em.get(auth_principal.getUuid());
            if (user != null) {
                return new UserInfo(appId, user.getProperties());
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
    public void startAppUserPasswordResetFlow(UUID applicationId, User user)
            throws Exception {
        String token = getPasswordResetTokenForAppUser(applicationId,
                user.getUuid());
        String reset_url = String.format(
                properties.getProperty(PROPERTIES_USER_RESETPW_URL),
                applicationId.toString(), user.getUuid().toString())
                + "?token=" + token;
        sendHtmlMail(
                properties,
                user.getDisplayEmailAddress(),
                properties.getProperty(PROPERTIES_MAILER_EMAIL),
                "Password Reset",
                appendEmailFooter(emailMsg(hashMap("reset_url", reset_url),
                        PROPERTIES_EMAIL_USER_PASSWORD_RESET)));

    }

    @Override
    public boolean newAppUsersNeedAdminApproval(UUID applicationId)
            throws Exception {
        EntityManager em = emf.getEntityManager(applicationId);
        Boolean registration_requires_admin_approval = (Boolean) em
                .getProperty(new SimpleEntityRef(Application.ENTITY_TYPE,
                        applicationId), "registration_requires_admin_approval");
        return registration_requires_admin_approval != null ? registration_requires_admin_approval
                .booleanValue() : false;
    }

    @Override
    public boolean newAppUsersRequireConfirmation(UUID applicationId)
            throws Exception {
        EntityManager em = emf.getEntityManager(applicationId);
        Boolean registration_requires_email_confirmation = (Boolean) em
                .getProperty(new SimpleEntityRef(Application.ENTITY_TYPE,
                        applicationId),
                        "registration_requires_email_confirmation");
        return registration_requires_email_confirmation != null ? registration_requires_email_confirmation
                .booleanValue() : false;
    }

    public boolean notifyAdminOfNewAppUsers(UUID applicationId)
            throws Exception {
        EntityManager em = emf.getEntityManager(applicationId);
        Boolean notify_admin_of_new_users = (Boolean) em.getProperty(
                new SimpleEntityRef(Application.ENTITY_TYPE, applicationId),
                "notify_admin_of_new_users");
        return notify_admin_of_new_users != null ? notify_admin_of_new_users
                .booleanValue() : false;
    }

    @Override
    public void startAppUserActivationFlow(UUID applicationId, User user)
            throws Exception {
        if (newAppUsersRequireConfirmation(applicationId)) {
            sendAppUserConfirmationEmail(applicationId, user);
        } else if (newAppUsersNeedAdminApproval(applicationId)) {
            sendAdminRequestAppUserActivationEmail(applicationId, user);
        } else {
            sendAppUserActivatedEmail(applicationId, user);
            sendAdminNewAppUserActivatedNotificationEmail(applicationId, user);
        }
    }

    @Override
    public ActivationState handleConfirmationTokenForAppUser(
            UUID applicationId, UUID userId, String token) throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken(token,
                TOKEN_TYPE_CONFIRM, APPLICATION_USER);
        if ((principal != null) && userId.equals(principal.getUuid())) {
            EntityManager em = emf.getEntityManager(applicationId);
            User user = em.get(userId, User.class);
            confirmAppUser(applicationId, user.getUuid());
            if (newAppUsersNeedAdminApproval(applicationId)) {
                sendAppUserConfirmedAwaitingActivationEmail(applicationId, user);
                sendAdminRequestAppUserActivationEmail(applicationId, user);
                return ActivationState.CONFIRMED_AWAITING_ACTIVATION;
            } else {
                activateAppUser(applicationId, principal.getUuid());
                sendAppUserActivatedEmail(applicationId, user);
                sendAdminNewAppUserActivatedNotificationEmail(applicationId,
                        user);
                return ActivationState.ACTIVATED;
            }
        }
        return ActivationState.UNKNOWN;
    }

    @Override
    public ActivationState handleActivationTokenForAppUser(UUID applicationId,
            UUID userId, String token) throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken(token,
                TOKEN_TYPE_ACTIVATION, APPLICATION_USER);
        if ((principal != null) && userId.equals(principal.getUuid())) {
            activateAppUser(applicationId, principal.getUuid());
            EntityManager em = emf.getEntityManager(applicationId);
            User user = em.get(userId, User.class);
            sendAppUserActivatedEmail(applicationId, user);
            sendAdminNewAppUserActivatedNotificationEmail(applicationId, user);
            return ActivationState.ACTIVATED;
        }
        return ActivationState.UNKNOWN;
    }

    public void sendAppUserConfirmationEmail(UUID applicationId, User user)
            throws Exception {
        String token = getConfirmationTokenForAppUser(applicationId,
                user.getUuid());
        String confirmation_url = String.format(
                properties.getProperty(PROPERTIES_USER_CONFIRMATION_URL),
                applicationId.toString(), user.getUuid().toString())
                + "?token=" + token;
        sendAppUserEmail(
                user,
                "User Account Confirmation: " + user.getEmail(),
                emailMsg(hashMap("activation_url", confirmation_url),
                        PROPERTIES_EMAIL_USER_CONFIRMATION));

    }

    public void sendAdminRequestAppUserActivationEmail(UUID applicationId,
            User user) throws Exception {
        String token = getActivationTokenForAppUser(applicationId,
                user.getUuid());
        String activation_url = String.format(
                properties.getProperty(PROPERTIES_USER_ACTIVATION_URL),
                applicationId.toString(), user.getUuid().toString())
                + "?token=" + token;
        OrganizationInfo organization = this
                .getOrganizationForApplication(applicationId);
        this.sendOrganizationEmail(
                organization,
                "Request For User Account Activation " + user.getEmail(),
                emailMsg(hashMap("organization_name", organization.getName())
                        .map("activation_url", activation_url),
                        PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION));
    }

    public void sendAdminNewAppUserActivatedNotificationEmail(
            UUID applicationId, User user) throws Exception {
        if (notifyAdminOfNewAppUsers(applicationId)) {
            OrganizationInfo organization = this
                    .getOrganizationForApplication(applicationId);
            this.sendOrganizationEmail(
                    organization,
                    "New User Account Activated " + user.getEmail(),
                    emailMsg(
                            hashMap("organization_name", organization.getName()),
                            PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION));
        }
    }

    public void sendAppUserConfirmedAwaitingActivationEmail(UUID applicationId,
            User user) throws Exception {
        sendAppUserEmail(
                user,
                "User Account Confirmed",
                properties.getProperty(PROPERTIES_EMAIL_USER_CONFIRMED_AWAITING_ACTIVATION));

    }

    public void sendAppUserActivatedEmail(UUID applicationId, User user)
            throws Exception {
        sendAppUserEmail(user, "User Account Activated",
                properties.getProperty(PROPERTIES_EMAIL_USER_ACTIVATED));
    }

    @Override
    public void activateAppUser(UUID applicationId, UUID userId)
            throws Exception {
        EntityManager em = emf.getEntityManager(applicationId);
        em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
                "activated", true);
    }

    public void confirmAppUser(UUID applicationId, UUID userId)
            throws Exception {
        EntityManager em = emf.getEntityManager(applicationId);
        em.setProperty(new SimpleEntityRef(User.ENTITY_TYPE, userId),
                "confirmed", true);
    }

    @Override
    public void setAppUserPassword(UUID applicationId, UUID userId,
            String newPassword) throws Exception {
        if ((userId == null) || (newPassword == null)) {
            return;
        }

        EntityManager em = emf.getEntityManager(applicationId);
        Entity owner = em.get(userId);


        writeUserPassword(applicationId, owner, maybeSaltPassword(newPassword));

    }

    @Override
    public void setAppUserPassword(UUID applicationId, UUID userId,
            String oldPassword, String newPassword) throws Exception {
        if ((userId == null)) {
            throw new IllegalArgumentException("userId is required");
        }
        if ((oldPassword == null) || (newPassword == null)) {
            throw new IllegalArgumentException(
                    "oldpassword and newpassword are both required");
        }
        // TODO load the user, send the hashType down to maybeSaltPassword
        if (!maybeSaltPassword(oldPassword).compare(readUserPasswordCredentials(applicationId, userId))) {
            throw new IncorrectPasswordException("Old password does not match");
        }


        setAppUserPassword(applicationId, userId, newPassword);

    }

    @Override
    public User verifyAppUserPasswordCredentials(UUID applicationId,
            String name, String password) throws Exception {

        User user = findUserEntity(applicationId, name);
        if (user == null) {
            return null;
        }

        if (maybeSaltPassword(password).compare(readUserPasswordCredentials(applicationId, user.getUuid()))) {
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
        return getTokenForPrincipal(EMAIL, TOKEN_TYPE_PASSWORD_RESET,
                applicationId, APPLICATION_USER, userId);
    }

    public void sendAppUserEmail(User user, String subject, String html)
            throws Exception {
        sendHtmlMail(properties, user.getDisplayEmailAddress(),
                properties.getProperty(PROPERTIES_MAILER_EMAIL), subject,
                appendEmailFooter(html));

    }

    public String getActivationTokenForAppUser(UUID applicationId, UUID userId)
            throws Exception {
        return getTokenForPrincipal(EMAIL, TOKEN_TYPE_ACTIVATION,
                applicationId, APPLICATION_USER, userId);
    }

    public String getConfirmationTokenForAppUser(UUID applicationId, UUID userId)
            throws Exception {
        return getTokenForPrincipal(EMAIL, TOKEN_TYPE_CONFIRM, applicationId,
                APPLICATION_USER, userId);
    }

    @Override
    public void setAppUserPin(UUID applicationId, UUID userId, String newPin)
            throws Exception {
        if ((userId == null) || (newPin == null)) {
            return;
        }

        writeUserPin(applicationId, new SimpleEntityRef(User.ENTITY_TYPE,
                userId), plainTextCredentials(newPin));
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
        String pin = getCredentialsSecret(readUserPin(applicationId, userId));

        sendHtmlMail(
                properties,
                user.getDisplayEmailAddress(),
                properties.getProperty(PROPERTIES_MAILER_EMAIL),
                "Your app pin",
                appendEmailFooter(emailMsg(hashMap(USER_PIN, pin),
                        PROPERTIES_EMAIL_USER_PIN_REQUEST)));

    }

    @Override
    public User verifyAppUserPinCredentials(UUID applicationId, String name,
            String pin) throws Exception {

        User user = findUserEntity(applicationId, name);
        if (user == null) {
            return null;
        }
        if (pin.equals(getCredentialsSecret(readUserPin(applicationId,
                user.getUuid())))) {
            return user;
        }
        return null;
    }

    @Override
    public void countAdminUserAction(UserInfo user, String action)
            throws Exception {
        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        em.incrementAggregateCounters(user.getUuid(), null, null,
                "admin_logins", 1);

    }

    @Override
    public User getOrCreateUserForFacebookAccessToken(UUID applicationId,
            String fb_access_token) throws Exception {

        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING,
                Boolean.TRUE);
        Client client = Client.create(clientConfig);
        WebResource web_resource = client
                .resource("https://graph.facebook.com/me");
        @SuppressWarnings("unchecked")
        Map<String, Object> fb_user = web_resource
                .queryParam("access_token", fb_access_token)
                .accept(MediaType.APPLICATION_JSON).get(Map.class);
        String fb_user_id = (String) fb_user.get("id");
        String fb_user_name = (String) fb_user.get("name");
        String fb_user_username = (String) fb_user.get("username");
        String fb_user_email = (String) fb_user.get("email");

        System.out.println(JsonUtils.mapToFormattedJsonString(fb_user));

        if (applicationId == null) {
            return null;
        }

        User user = null;

        if ((fb_user != null) && !anyNull(fb_user_id, fb_user_name)) {
            EntityManager em = emf.getEntityManager(applicationId);
            Results r = em.searchCollection(em.getApplicationRef(), "users",
                    Query.findForProperty("facebook.id", fb_user_id));

            if (r.size() > 1) {
                logger.error("Multiple users for FB ID: " + fb_user_id);
                throw new BadTokenException(
                        "multiple users with same Facebook ID");
            }

            if (r.size() < 1) {
                Map<String, Object> properties = new LinkedHashMap<String, Object>();

                properties.put("facebook", fb_user);
                properties.put("username",
                        fb_user_username != null ? fb_user_username : "fb_"
                                + fb_user_id);
                properties.put("name", fb_user_name);
                if (fb_user_email != null) {
                    properties.put("email", fb_user_email);
                }
                properties.put("picture", "http://graph.facebook.com/"
                        + fb_user_id + "/picture");
                properties.put("activated", true);

                user = em.create("user", User.class, properties);
            } else {
                user = (User) r.getEntity().toTypedEntity();
                Map<String, Object> properties = new LinkedHashMap<String, Object>();

                properties.put("facebook", fb_user);
                properties.put("picture", "http://graph.facebook.com/"
                        + fb_user_id + "/picture");
                em.updateProperties(user, properties);

                user.setProperty("facebook", fb_user);
                user.setProperty("picture", "http://graph.facebook.com/"
                        + fb_user_id + "/picture");
            }
        } else {
            throw new BadTokenException(
                    "Unable to confirm Facebook access token");
        }

        return user;

    }

    @SuppressWarnings("unchecked")
    @Override
    public User getOrCreateUserForFoursquareAccessToken(UUID applicationId,
            String fq_access_token) throws Exception {

        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING,
                Boolean.TRUE);
        Client client = Client.create(clientConfig);
        WebResource web_resource = client
                .resource("https://api.foursquare.com/v2/users/self");
        Map<String, Object> body = web_resource
                .queryParam("oauth_token", fq_access_token)
                .queryParam("v", "20120623").accept(MediaType.APPLICATION_JSON)
                .get(Map.class);

        Map<String, Object> fq_user = (Map<String, Object>) ((Map<?, ?>) body
                .get("response")).get("user");

        String fq_user_id = (String) fq_user.get("id");
        String fq_user_username = (String) fq_user.get("id");
        String fq_user_email = (String) ((Map<?, ?>) fq_user.get("contact"))
                .get("email");
        String fq_user_picture = (String) ((Map<?, ?>) fq_user.get("photo"))
                .get("suffix");
        String fq_user_name = new String("");

        // Grab the last check-in so we can store that as the user location
        Map<String, Object> fq_location = (Map<String, Object>) ((Map<?, ?>) ((Map<?, ?>) ((ArrayList<?>) ((Map<?, ?>) fq_user
                .get("checkins")).get("items")).get(0)).get("venue"))
                .get("location");

        Map<String, Double> location = new LinkedHashMap<String, Double>();
        location.put("latitude", (Double) fq_location.get("lat"));
        location.put("longitude", (Double) fq_location.get("lng"));

        System.out.println(JsonUtils.mapToFormattedJsonString(location));

        // Only the first name is guaranteed to be here
        try {
            fq_user_name = (String) fq_user.get("firstName") + " "
                    + (String) fq_user.get("lastName");
        } catch (NullPointerException e) {
            fq_user_name = (String) fq_user.get("firstName");
        }

        // System.out.println(JsonUtils.mapToFormattedJsonString(fq_user));

        if (applicationId == null) {
            return null;
        }

        User user = null;

        if ((fq_user != null) && !anyNull(fq_user_id, fq_user_name)) {
            EntityManager em = emf.getEntityManager(applicationId);
            Results r = em.searchCollection(em.getApplicationRef(), "users",
                    Query.findForProperty("foursquare.id", fq_user_id));

            if (r.size() > 1) {
                logger.error("Multiple users for FQ ID: " + fq_user_id);
                throw new BadTokenException(
                        "multiple users with same Foursquare ID");
            }

            if (r.size() < 1) {
                Map<String, Object> properties = new LinkedHashMap<String, Object>();

                properties.put("foursquare", fq_user);
                properties.put("username",
                        fq_user_username != null ? fq_user_username : "fq_"
                                + fq_user_id);
                properties.put("name", fq_user_name);
                if (fq_user_email != null) {
                    properties.put("email", fq_user_email);
                }
                properties.put("picture", "https://is0.4sqi.net/userpix_thumbs"
                        + fq_user_picture);
                properties.put("activated", true);
                properties.put("location", location);

                user = em.create("user", User.class, properties);
            } else {
                user = (User) r.getEntity().toTypedEntity();
                Map<String, Object> properties = new LinkedHashMap<String, Object>();

                properties.put("foursquare", fq_user);
                properties.put("picture", "https://is0.4sqi.net/userpix_thumbs"
                        + fq_user_picture);
                properties.put("location", location);
                em.updateProperties(user, properties);

                user.setProperty("foursquare", fq_user);
                user.setProperty("picture",
                        "https://is0.4sqi.net/userpix_thumbs" + fq_user_picture);
                user.setProperty("location", location);
            }
        } else {
            throw new BadTokenException(
                    "Unable to confirm Foursquare access token");
        }

        return user;

    }

     
      
      /**
       * Persist the user's password credentials info
       * @param appId
       * @param owner
       * @param creds
       * @throws Exception 
       */
      protected void writeUserPassword(UUID appId, EntityRef owner,CredentialsInfo creds) throws Exception{
          writeCreds(appId, owner, creds, USER_PASSWORD);
      }
      
      /**
       * read the user password credential's info
       * @param appId
       * @param ownerId
       * @return
     * @throws Exception 
       */
      protected CredentialsInfo readUserPasswordCredentials(UUID appId, UUID ownerId) throws Exception{
         return readCreds(appId, ownerId, USER_PASSWORD);
      }
      
      /**
       * Write the user's token
       * @param appId
       * @param owner
       * @param token
     * @throws Exception 
       */
      protected void writeUserToken(UUID appId, EntityRef owner, CredentialsInfo token) throws Exception{
          writeCreds(appId, owner, token, USER_TOKEN);
      }
      
      /**
       * Read the credentials info for the user's token
       * @param appId
       * @param ownerId
       * @return
     * @throws Exception 
       */
      protected CredentialsInfo readUserToken(UUID appId, UUID ownerId) throws Exception{
          return readCreds(appId, ownerId, USER_TOKEN);
      }
      
      /**
       * Write the mongo password
       * @param appId
       * @param owner
       * @param password
     * @throws Exception 
       */
      protected void writeUserMongoPassword(UUID appId, EntityRef owner, CredentialsInfo password) throws Exception{
         writeCreds(appId, owner, password, USER_MONGO_PASSWORD);
      }
      
      /**
       * Read the mongo password
       * @param appId
       * @param ownerId
       * @return
     * @throws Exception 
       */
      protected CredentialsInfo readUserMongoPassword(UUID appId, UUID ownerId) throws Exception{
          return readCreds(appId, ownerId, USER_MONGO_PASSWORD);
      }
      
      /**
       * Write the user's pin
       * @param appId
       * @param owner
       * @param pin
       * @throws Exception 
       */
      protected void writeUserPin(UUID appId, EntityRef owner, CredentialsInfo pin) throws Exception{
          writeCreds(appId, owner, pin, USER_PIN);
      }
      
      /**
       * Read the user's pin
       * @param appId
       * @param ownerId
       * @return
     * @throws Exception 
       */
      protected CredentialsInfo readUserPin(UUID appId, UUID ownerId) throws Exception{
          return readCreds(appId, ownerId, USER_PIN);
      }
      
      private void writeCreds(UUID appId, EntityRef owner, CredentialsInfo creds, String key) throws Exception{
          EntityManager em = emf.getEntityManager(appId);
          em.addToDictionary(owner, DICTIONARY_CREDENTIALS, key, creds);
          
      }
      
      private CredentialsInfo readCreds(UUID appId, UUID ownerId, String key) throws Exception{
          EntityManager em = emf.getEntityManager(appId);
          Entity owner = em.get(ownerId);
          return (CredentialsInfo) em.getDictionaryElementValue(owner, DICTIONARY_CREDENTIALS, key);
      }


    public boolean newAdminUsersNeedSysAdminApproval() {
        return properties.newAdminUsersNeedSysAdminApproval();
    }

    public boolean newAdminUsersRequireConfirmation() {
        return properties.newAdminUsersRequireConfirmation();
    }

    public boolean newOrganizationsNeedSysAdminApproval() {
        return properties.newOrganizationsNeedSysAdminApproval();
    }

    private boolean areActivationChecksDisabled() {
        return !(newOrganizationsNeedSysAdminApproval()
                || properties.newOrganizationsRequireConfirmation()
                || newAdminUsersNeedSysAdminApproval()
                || newAdminUsersRequireConfirmation());
    }

    private static void sendHtmlMail(AccountCreationProps props, String to, String from, String subject, String html) {
        MailUtils.sendHtmlMail(props.getMailProperties(), to, from, subject, html);
    }

  public AccountCreationProps getAccountCreationProps() {
    return properties;
  }

  private CredentialsInfo maybeSaltPassword(String password) throws Exception {
    return hashedCredentials(properties.getProperty(PROPERTIES_PASSWORD_SALT, ""),password);
  }
}
