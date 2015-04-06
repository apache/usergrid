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
package org.apache.usergrid.management.cassandra;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.exception.ConflictException;
import org.apache.usergrid.management.exceptions.*;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.index.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.usergrid.locking.Lock;
import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.management.AccountCreationProps;
import org.apache.usergrid.management.ActivationState;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.crypto.EncryptionService;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.security.oauth.ClientCredentialsInfo;
import org.apache.usergrid.security.salt.SaltProvider;
import org.apache.usergrid.security.shiro.PrincipalCredentialsToken;
import org.apache.usergrid.security.shiro.credentials.ApplicationClientCredentials;
import org.apache.usergrid.security.shiro.credentials.OrganizationClientCredentials;
import org.apache.usergrid.security.shiro.principals.ApplicationPrincipal;
import org.apache.usergrid.security.shiro.principals.OrganizationPrincipal;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.security.tokens.TokenCategory;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.security.tokens.exceptions.TokenException;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.apache.usergrid.services.ServiceRequest;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.utils.ConversionUtils;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.MailUtils;
import org.apache.usergrid.utils.StringUtils;
import org.apache.usergrid.utils.UUIDUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.locking.LockHelper.getUniqueUpdateLock;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_ACTIVATION_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_CONFIRMATION_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_RESETPW_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_ACTIVATED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_CONFIRMED_AWAITING_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_INVITED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_FOOTER;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ORGANIZATION_CONFIRMED_AWAITING_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_ACTIVATED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_CONFIRMED_AWAITING_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_PASSWORD_RESET;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_PIN_REQUEST;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_MAILER_EMAIL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ORGANIZATION_ACTIVATION_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SETUP_TEST_ACCOUNT;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_ALLOWED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_EMAIL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_NAME;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_PASSWORD;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_TEST_ACCOUNT_ADMIN_USER_NAME;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_TEST_ACCOUNT_ADMIN_USER_PASSWORD;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_TEST_ACCOUNT_ADMIN_USER_USERNAME;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_TEST_ACCOUNT_APP;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_TEST_ACCOUNT_ORGANIZATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USER_ACTIVATION_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USER_CONFIRMATION_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USER_RESETPW_URL;
import static org.apache.usergrid.persistence.CredentialsInfo.getCredentialsSecret;
import static org.apache.usergrid.persistence.Schema.*;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_ACTOR;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_ACTOR_NAME;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_CATEGORY;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_CONTENT;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_DISPLAY_NAME;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_ENTITY_TYPE;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_OBJECT;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_OBJECT_ENTITY_TYPE;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_OBJECT_NAME;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_OBJECT_TYPE;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_TITLE;
import static org.apache.usergrid.persistence.entities.Activity.PROPERTY_VERB;
import org.apache.usergrid.persistence.index.query.Query.Level;
import rx.Observable;

import static org.apache.usergrid.security.AuthPrincipalType.ADMIN_USER;
import static org.apache.usergrid.security.AuthPrincipalType.APPLICATION;
import static org.apache.usergrid.security.AuthPrincipalType.APPLICATION_USER;
import static org.apache.usergrid.security.AuthPrincipalType.ORGANIZATION;
import static org.apache.usergrid.security.oauth.ClientCredentialsInfo.getTypeFromClientId;
import static org.apache.usergrid.security.oauth.ClientCredentialsInfo.getUUIDFromClientId;
import static org.apache.usergrid.security.tokens.TokenCategory.ACCESS;
import static org.apache.usergrid.security.tokens.TokenCategory.EMAIL;
import static org.apache.usergrid.services.ServiceParameter.parameters;
import static org.apache.usergrid.services.ServicePayload.payload;
import static org.apache.usergrid.services.ServiceResults.genericServiceResults;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.ConversionUtils.bytes;
import static org.apache.usergrid.utils.ConversionUtils.uuid;
import static org.apache.usergrid.utils.ListUtils.anyNull;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.apache.usergrid.utils.PasswordUtils.mongoPassword;

import static java.lang.Boolean.parseBoolean;


public class ManagementServiceImpl implements ManagementService {
    private static final Logger logger = LoggerFactory.getLogger( ManagementServiceImpl.class );

    /** Key for the user's pin */
    protected static final String USER_PIN = "pin";

    /** Key for the user's oauth secret */
    protected static final String USER_TOKEN = "secret";

    /** Key for the user's mongo password */
    protected static final String USER_MONGO_PASSWORD = "mongo_pwd";

    /** Key for the user's password */
    protected static final String USER_PASSWORD = "password";

    protected static final String USER_PASSWORD_HISTORY = "password_history";

    private static final String TOKEN_TYPE_ACTIVATION = "activate";

    private static final String TOKEN_TYPE_PASSWORD_RESET = "resetpw";

    private static final String TOKEN_TYPE_CONFIRM = "confirm";

    public static final String OAUTH_SECRET_SALT = "super secret oauth value";

    private static final String ORGANIZATION_PROPERTIES_DICTIONARY = "orgProperties";
    public static final String REGISTRATION_REQUIRES_ADMIN_APPROVAL = "registration_requires_admin_approval";
    public static final String REGISTRATION_REQUIRES_EMAIL_CONFIRMATION = "registration_requires_email_confirmation";
    public static final String NOTIFY_ADMIN_OF_NEW_USERS = "notify_admin_of_new_users";

    protected ServiceManagerFactory smf;

    protected EntityManagerFactory emf;

    protected AccountCreationPropsImpl properties;

    protected LockManager lockManager;

    protected TokenService tokens;

    protected SaltProvider saltProvider;

    @Autowired
    protected MailUtils mailUtils;

    protected EncryptionService encryptionService;


    /** Must be constructed with a CassandraClientPool. */
    public ManagementServiceImpl() {
    }


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        logger.info( "ManagementServiceImpl.setEntityManagerFactory" );
        this.emf = emf;
    }


    @Autowired
    public void setProperties( Properties properties ) {
        this.properties = new AccountCreationPropsImpl( properties );
    }


    /** For testing purposes only */
    public Properties getProperties() {
        return properties.properties;
    }

   @Autowired
    public void setTokenService( TokenService tokens ) {
        this.tokens = tokens;
    }


    @Autowired
    public void setServiceManagerFactory( ServiceManagerFactory smf ) {
        this.smf = smf;
    }


    public LockManager getLockManager() {
        return lockManager;
    }


    @Autowired
    public void setLockManager( LockManager lockManager ) {
        this.lockManager = lockManager;
    }


    /** @param encryptionService the encryptionService to set */
    @Autowired
    public void setEncryptionService( EncryptionService encryptionService ) {
        this.encryptionService = encryptionService;
    }


    @Override
    public void setup() throws Exception {

        if ( getBooleanProperty( PROPERTIES_SETUP_TEST_ACCOUNT ) ) {
            String test_app_name = properties.getProperty( PROPERTIES_TEST_ACCOUNT_APP );
            String test_organization_name = properties.getProperty( PROPERTIES_TEST_ACCOUNT_ORGANIZATION );
            String test_admin_username = properties.getProperty( PROPERTIES_TEST_ACCOUNT_ADMIN_USER_USERNAME );
            String test_admin_name = properties.getProperty( PROPERTIES_TEST_ACCOUNT_ADMIN_USER_NAME );
            String test_admin_email = properties.getProperty( PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL );
            String test_admin_password = properties.getProperty( PROPERTIES_TEST_ACCOUNT_ADMIN_USER_PASSWORD );

            if ( anyNull( test_app_name, test_organization_name, test_admin_username, test_admin_name, test_admin_email,
                    test_admin_password ) ) {
                logger.warn( "Missing values for test app, check properties.  Skipping test app setup..." );
                return;
            }

            OrganizationInfo organization = getOrganizationByName( test_organization_name );

            if ( organization == null ) {
                OrganizationOwnerInfo created =
                        createOwnerAndOrganization( test_organization_name, test_admin_username, test_admin_name,
                                test_admin_email, test_admin_password, true, false );
                organization = created.getOrganization();
            }

            if ( !getApplicationsForOrganization( organization.getUuid() ).containsValue( test_app_name ) ) {
                try {
                    createApplication( organization.getUuid(), test_app_name );
                }catch(ApplicationAlreadyExistsException aaee){
                    logger.debug("The database setup already found an existing application");
                }
            }
        }
        else {
            logger.warn( "Test app creation disabled" );
        }

        if ( superuserEnabled() ) {
            provisionSuperuser();
        }
    }


    public boolean superuserEnabled() {
        boolean superuser_enabled = getBooleanProperty( PROPERTIES_SYSADMIN_LOGIN_ALLOWED );
        String superuser_username = properties.getProperty( PROPERTIES_SYSADMIN_LOGIN_NAME );
        String superuser_email = properties.getProperty( PROPERTIES_SYSADMIN_LOGIN_EMAIL );
        String superuser_password = properties.getProperty( PROPERTIES_SYSADMIN_LOGIN_PASSWORD );

        return superuser_enabled && !anyNull( superuser_username, superuser_email, superuser_password );
    }


    @Override
    public void provisionSuperuser() throws Exception {
        boolean superuser_enabled = getBooleanProperty( PROPERTIES_SYSADMIN_LOGIN_ALLOWED );
        String superuser_username = properties.getProperty( PROPERTIES_SYSADMIN_LOGIN_NAME );
        String superuser_email = properties.getProperty( PROPERTIES_SYSADMIN_LOGIN_EMAIL );
        String superuser_password = properties.getProperty( PROPERTIES_SYSADMIN_LOGIN_PASSWORD );

        if ( !anyNull( superuser_username, superuser_email, superuser_password ) ) {
            UserInfo user = this.getAdminUserByUsername( superuser_username );
            if ( user == null ) {
                createAdminUser( superuser_username, "Super User", superuser_email, superuser_password,
                        superuser_enabled, !superuser_enabled );
            }
            else {
                this.setAdminUserPassword( user.getUuid(), superuser_password );
            }
        }
        else {
            logger.warn(
                    "Missing values for superuser account, check properties.  Skipping superuser account setup..." );
        }
    }


    public String generateOAuthSecretKey( AuthPrincipalType type ) {
        long timestamp = System.currentTimeMillis();
        ByteBuffer bytes = ByteBuffer.allocate( 20 );
        bytes.put( sha( timestamp + OAUTH_SECRET_SALT + UUID.randomUUID() ) );
        String secret = type.getBase64Prefix() + encodeBase64URLSafeString( bytes.array() );
        return secret;
    }


    @SuppressWarnings( "serial" )
    @Override
    public void postOrganizationActivity( UUID organizationId, final UserInfo user, String verb, final EntityRef object,
                                          final String objectType, final String objectName, String title,
                                          String content ) throws Exception {
        ServiceManager sm = smf.getServiceManager( smf.getManagementAppId() );

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( PROPERTY_VERB, verb );
        properties.put( PROPERTY_CATEGORY, "admin" );
        if ( content != null ) {
            properties.put( PROPERTY_CONTENT, content );
        }
        if ( title != null ) {
            properties.put( PROPERTY_TITLE, title );
        }
        properties.put( PROPERTY_ACTOR, new HashMap<String, Object>() {
            {
                put( PROPERTY_DISPLAY_NAME, user.getName() );
                put( PROPERTY_OBJECT_TYPE, "person" );
                put( PROPERTY_ENTITY_TYPE, "user" );
                put( PROPERTY_UUID, user.getUuid() );
            }
        } );
        properties.put( PROPERTY_OBJECT, new HashMap<String, Object>() {
            {
                put( PROPERTY_DISPLAY_NAME, objectName );
                put( PROPERTY_OBJECT_TYPE, objectType );
                put( PROPERTY_ENTITY_TYPE, object.getType() );
                put( PROPERTY_UUID, object.getUuid() );
            }
        } );

        sm.newRequest( ServiceAction.POST, parameters( Schema.COLLECTION_GROUPS, organizationId, "activities" ), payload( properties ) )
          .execute().getEntity();
    }


    @Override
    public ServiceResults getOrganizationActivity( OrganizationInfo organization ) throws Exception {
        ServiceManager sm = smf.getServiceManager( smf.getManagementAppId() );
        return sm.newRequest( ServiceAction.GET, parameters( Schema.COLLECTION_GROUPS, organization.getUuid(), "feed" ) ).execute();
    }


    @Override
    public ServiceResults getOrganizationActivityForAdminUser( OrganizationInfo organization, UserInfo user )
            throws Exception {
        ServiceManager sm = smf.getServiceManager( smf.getManagementAppId() );
        return sm.newRequest( ServiceAction.GET,
                parameters( Schema.COLLECTION_GROUPS, organization.getUuid(), "users", user.getUuid(), "feed" ) ).execute();
    }


    @Override
    public ServiceResults getAdminUserActivity( UserInfo user ) throws Exception {
        ServiceManager sm = smf.getServiceManager( smf.getManagementAppId() );
        return sm.newRequest( ServiceAction.GET, parameters( "users", user.getUuid(), "feed" ) ).execute();
    }


    @Override
    public OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
                                                             String email, String password ) throws Exception {

        logger.debug("createOwnerAndOrganization1");
        boolean activated = !newAdminUsersNeedSysAdminApproval() && !newOrganizationsNeedSysAdminApproval();
        boolean disabled = newAdminUsersRequireConfirmation();
        // if we are active and enabled, skip the send email step

        return createOwnerAndOrganization( organizationName, username, name, email, password, activated, disabled, null,
                null );
    }


    @Override
    public OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
                                                             String email, String password, boolean activated,
                                                             boolean disabled ) throws Exception {
        logger.debug("createOwnerAndOrganization2");
        return createOwnerAndOrganization( organizationName, username, name, email, password,
                activated, disabled, null, null );
    }


    @Override
    public OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
                                                             String email, String password, boolean activated,
                                                             boolean disabled, Map<String, Object> userProperties,
                                                             Map<String, Object> organizationProperties )
            throws Exception {

        logger.debug("createOwnerAndOrganization3");

        /**
         * Only lock on the target values. We don't want lock contention if another
         * node is trying to set the property do a different value
         */
        Lock groupLock =
                getUniqueUpdateLock( lockManager, smf.getManagementAppId(), organizationName, Schema.COLLECTION_GROUPS, PROPERTY_PATH );

        Lock userLock = getUniqueUpdateLock( lockManager, smf.getManagementAppId(), username, "users", "username" );

        Lock emailLock = getUniqueUpdateLock( lockManager, smf.getManagementAppId(), email, "users", "email" );

        UserInfo user = null;
        OrganizationInfo organization = null;

        try {

            groupLock.lock();
            userLock.lock();
            emailLock.lock();
            EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
            if ( !em.isPropertyValueUniqueForEntity( Group.ENTITY_TYPE, PROPERTY_PATH, organizationName ) ) {
                throw new DuplicateUniquePropertyExistsException( Group.ENTITY_TYPE, PROPERTY_PATH, organizationName );
            }
            if ( !validateAdminInfo( username, name, email, password ) ) {
                return null;
            }
            if ( areActivationChecksDisabled() ) {
                user = createAdminUserInternal( username, name, email, password, true, false, userProperties );
            }
            else {
                user = createAdminUserInternal( username, name, email, password, activated, disabled, userProperties );
            }

            logger.debug("User created");
            organization = createOrganizationInternal( organizationName, user, true, organizationProperties );
        }
        finally {
            emailLock.unlock();
            userLock.unlock();
            groupLock.unlock();
        }

        return new OrganizationOwnerInfo( user, organization );
    }


    private OrganizationInfo createOrganizationInternal( String organizationName, UserInfo user, boolean activated )
            throws Exception {
        logger.debug("createOrganizationInternal1");
        return createOrganizationInternal( organizationName, user, activated, null );
    }


    private OrganizationInfo createOrganizationInternal( String organizationName, UserInfo user, boolean activated,
                                                         Map<String, Object> properties ) throws Exception {

        logger.info( "createOrganizationInternal2: {}", organizationName );

        if (  organizationName == null ) {
            logger.debug("organizationName = null");
            return null;
        }
        if ( user == null ) {
            logger.debug("user = null");
            return null;
        }

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

        Group organizationEntity = new Group();
        organizationEntity.setPath( organizationName );
        organizationEntity = em.create( organizationEntity );

        em.addToCollection( organizationEntity, "users", new SimpleEntityRef( User.ENTITY_TYPE, user.getUuid() ) );


        writeUserToken( smf.getManagementAppId(), organizationEntity, encryptionService
                .plainTextCredentials( generateOAuthSecretKey( AuthPrincipalType.ORGANIZATION ), user.getUuid(),
                        smf.getManagementAppId() ) );

        OrganizationInfo organization =
                new OrganizationInfo( organizationEntity.getUuid(), organizationName, properties );
        updateOrganization( organization );

        postOrganizationActivity( organization.getUuid(), user, "create", organizationEntity, "Organization",
                organization.getName(),
                "<a href=\"mailto:" + user.getEmail() + "\">" + user.getName() + " (" + user.getEmail()
                        + ")</a> created a new organization account named " + organizationName, null );

        startOrganizationActivationFlow( organization );



        return organization;
    }


    @Override
    public OrganizationInfo createOrganization( String organizationName, UserInfo user, boolean activated )
            throws Exception {

        if (  organizationName == null ) {
            logger.debug("organizationName = null");
            return null;
        }
        if ( user == null ) {
            logger.debug("user = null");
            return null;
        }

        Lock groupLock = getUniqueUpdateLock(
            lockManager, smf.getManagementAppId(), organizationName, Schema.COLLECTION_GROUPS, PROPERTY_PATH );

        EntityManager em = emf.getEntityManager(smf.getManagementAppId());
        if ( !em.isPropertyValueUniqueForEntity( Group.ENTITY_TYPE, PROPERTY_PATH, organizationName ) ) {
            throw new DuplicateUniquePropertyExistsException( Group.ENTITY_TYPE, PROPERTY_PATH, organizationName );
        }
        try {
            groupLock.lock();
            return createOrganizationInternal( organizationName, user, activated );
        }
        finally {
            groupLock.unlock();
        }
    }


    /** currently only affects properties */
    public void updateOrganization( OrganizationInfo organizationInfo ) throws Exception {
        Map<String, Object> properties = organizationInfo.getProperties();
        if ( properties != null ) {
            EntityRef organizationEntity = new SimpleEntityRef( Group.ENTITY_TYPE, organizationInfo.getUuid() );
            EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
            for ( Map.Entry<String, Object> entry : properties.entrySet() ) {
                if ( "".equals( entry.getValue() ) ) {
                    properties.remove( entry.getKey() );
                    em.removeFromDictionary( organizationEntity, ORGANIZATION_PROPERTIES_DICTIONARY, entry.getKey() );
                }
                else {
                    em.addToDictionary( organizationEntity, ORGANIZATION_PROPERTIES_DICTIONARY, entry.getKey(),
                            entry.getValue() );
                }
            }
        }
    }


    @Override
    public OrganizationInfo importOrganization( UUID organizationId, OrganizationInfo organizationInfo,
                                                Map<String, Object> properties ) throws Exception {

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        if ( !em.isPropertyValueUniqueForEntity( Group.ENTITY_TYPE, PROPERTY_PATH, organizationInfo.getName() ) ) {
            throw new DuplicateUniquePropertyExistsException( Group.ENTITY_TYPE, PROPERTY_PATH, organizationInfo.getName() );
        }
        if ( properties == null ) {
            properties = new HashMap<String, Object>();
        }

        String organizationName = null;
        if ( organizationInfo != null ) {
            organizationName = organizationInfo.getName();
        }
        if ( organizationName == null ) {
            organizationName = ( String ) properties.get( PROPERTY_PATH );
        }
        if ( organizationName == null ) {
            organizationName = ( String ) properties.get( PROPERTY_NAME );
        }
        if ( organizationName == null ) {
            return null;
        }

        if ( organizationId == null ) {
            if ( organizationInfo != null ) {
                organizationId = organizationInfo.getUuid();
            }
        }
        if ( organizationId == null ) {
            organizationId = uuid( properties.get( PROPERTY_UUID ) );
        }
        if ( organizationId == null ) {
            return null;
        }

        properties.put( PROPERTY_PATH, organizationName );
        properties.put( PROPERTY_SECRET, generateOAuthSecretKey( AuthPrincipalType.ORGANIZATION ) );
        Entity organization = em.create( organizationId, Group.ENTITY_TYPE, properties );
        // em.addToCollection(organization, "users", new SimpleEntityRef(
        // User.ENTITY_TYPE, userId));
        return new OrganizationInfo( organization.getUuid(), organizationName );
    }


    @Override
    public UUID importApplication( UUID organizationId, final Application application ) throws Exception {
        throw new UnsupportedOperationException("Import application not supported");
    }


    /**
     * Test if the applicationName contains a '/' character, prepend with orgName if it does not, assume it is complete
     * (and that organization is needed) if so.
     */
    private String buildAppName( String applicationName, OrganizationInfo organization ) {
        return applicationName.contains( "/" ) ? applicationName : organization.getName() + "/" + applicationName;
    }


    @Override
    public List<OrganizationInfo> getOrganizations( UUID startResult, int count ) throws Exception {
        // still need the bimap to search for existing
        BiMap<UUID, String> organizations = HashBiMap.create();
        EntityManager em = emf.getEntityManager(smf.getManagementAppId());
        Results results =
                em.getCollection(em.getApplicationRef(), Schema.COLLECTION_GROUPS, startResult, count, Level.ALL_PROPERTIES, false);
        List<OrganizationInfo> orgs = new ArrayList<OrganizationInfo>( results.size() );
        OrganizationInfo orgInfo;
        for ( Entity entity : results.getEntities() ) {
            // TODO T.N. temporary hack to deal with duplicate orgs. Revert this
            // commit after migration
            String path = ( String ) entity.getProperty( PROPERTY_PATH );

            if ( organizations.containsValue( path ) ) {
                path += "DUPLICATE";
            }
            orgInfo = new OrganizationInfo( entity.getUuid(), path );
            orgs.add( orgInfo );
            organizations.put( entity.getUuid(), path );
        }
        return orgs;
    }


    @Override
    public BiMap<UUID, String> getOrganizations() throws Exception {
        List<OrganizationInfo> orgs = getOrganizations( null, 10000 );
        return buildOrgBiMap( orgs );
    }


    private BiMap<UUID, String> buildOrgBiMap( List<OrganizationInfo> orgs ) {
        BiMap<UUID, String> organizations = HashBiMap.create();
        for ( OrganizationInfo orgInfo : orgs ) {
            organizations.put( orgInfo.getUuid(), orgInfo.getName() );
        }
        return organizations;
    }


    @Override
    public OrganizationInfo getOrganizationInfoFromAccessToken( String token ) throws Exception {
        Entity entity = getEntityFromAccessToken( token, null, ORGANIZATION );
        if ( entity == null ) {
            return null;
        }
        return new OrganizationInfo( entity.getProperties() );
    }


    @Override
    public OrganizationInfo getOrganizationByName( String organizationName ) throws Exception {

        if ( organizationName == null ) {
            return null;
        }

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        EntityRef ref = em.getAlias( Group.ENTITY_TYPE, organizationName );
        if ( ref == null ) {
            return null;
        }
        return getOrganizationByUuid( ref.getUuid() );
    }


    @Override
    public OrganizationInfo getOrganizationByUuid( UUID id ) throws Exception {

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        Entity entity = em.get( new SimpleEntityRef( Group.ENTITY_TYPE, id ) );
        if ( entity == null ) {
            return null;
        }
        Map properties = em.getDictionaryAsMap( entity, ORGANIZATION_PROPERTIES_DICTIONARY );
        OrganizationInfo orgInfo = new OrganizationInfo( entity.getProperties() );
        orgInfo.setProperties( properties );
        return orgInfo;
    }


    @Override
    public OrganizationInfo getOrganizationByIdentifier( Identifier id ) throws Exception {
        if ( id.isUUID() ) {
            return getOrganizationByUuid( id.getUUID() );
        }
        if ( id.isName() ) {
            return getOrganizationByName( id.getName() );
        }
        return null;
    }


    public void postUserActivity( UserInfo user, String verb, EntityRef object, String objectType, String objectName,
                                  String title, String content ) throws Exception {
        ServiceManager sm = smf.getServiceManager( smf.getManagementAppId() );

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( PROPERTY_VERB, verb );
        properties.put( PROPERTY_CATEGORY, "admin" );
        if ( content != null ) {
            properties.put( PROPERTY_CONTENT, content );
        }
        if ( title != null ) {
            properties.put( PROPERTY_TITLE, title );
        }
        properties.put( PROPERTY_ACTOR, user.getUuid() );
        properties.put( PROPERTY_ACTOR_NAME, user.getName() );
        properties.put( PROPERTY_OBJECT, object.getUuid() );
        properties.put( PROPERTY_OBJECT_ENTITY_TYPE, object.getType() );
        properties.put( PROPERTY_OBJECT_TYPE, objectType );
        properties.put( PROPERTY_OBJECT_NAME, objectName );

        sm.newRequest( ServiceAction.POST, parameters( "users", user.getUuid(), "activities" ), payload( properties ) )
          .execute().getEntity();
    }


    @Override
    public ServiceResults getAdminUserActivities( UserInfo user ) throws Exception {
        ServiceManager sm = smf.getServiceManager( smf.getManagementAppId() );
        ServiceRequest request = sm.newRequest( ServiceAction.GET, parameters( "users", user.getUuid(), "feed" ) );
        ServiceResults results = request.execute();
        return results;
    }


    private UserInfo doCreateAdmin( User user, CredentialsInfo userPassword, CredentialsInfo mongoPassword )
            throws Exception {

        writeUserToken( smf.getManagementAppId(), user, encryptionService
                .plainTextCredentials( generateOAuthSecretKey( AuthPrincipalType.ADMIN_USER ), user.getUuid(),
                        smf.getManagementAppId() ) );

        writeUserPassword( smf.getManagementAppId(), user, userPassword );

        writeUserMongoPassword( smf.getManagementAppId(), user, mongoPassword );

        UserInfo userInfo = new UserInfo(
            smf.getManagementAppId(), user.getUuid(), user.getUsername(), user.getName(),
            user.getEmail(), user.getConfirmed(), user.getActivated(), user.getDisabled(),
            user.getDynamicProperties(), true );

        // special case for sysadmin and test account only
        if (    !user.getEmail().equals( properties.getProperty( PROPERTIES_SYSADMIN_LOGIN_EMAIL ) )
             && !user.getEmail().equals( properties .getProperty( PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL ) ) ) {
            this.startAdminUserActivationFlow( userInfo );
        }

        return userInfo;
    }


    @Override
    public UserInfo createAdminFromPrexistingPassword( User user, CredentialsInfo ci ) throws Exception {

        return doCreateAdmin( user, ci,
                // we can't actually set the mongo password. We never have the plain text in
                // this path
                encryptionService.plainTextCredentials( mongoPassword( user.getUsername(), "" ), user.getUuid(),
                        smf.getManagementAppId() ) );
    }


    @Override
    public UserInfo createAdminFrom( User user, String password ) throws Exception {
        return doCreateAdmin( user,
                encryptionService.defaultEncryptedCredentials( password, user.getUuid(), smf.getManagementAppId() ),
                encryptionService.plainTextCredentials( mongoPassword( user.getUsername(), password ), user.getUuid(),
                        smf.getManagementAppId() ) );
    }


    @Override
    public UserInfo createAdminUser( String username, String name, String email, String password, boolean activated,
                                     boolean disabled ) throws Exception {
        return createAdminUser( username, name, email, password, activated, disabled, null );
    }


    @Override
    public UserInfo createAdminUser( String username, String name, String email, String password, boolean activated,
                                     boolean disabled, Map<String, Object> userProperties ) throws Exception {

        if ( !validateAdminInfo( username, name, email, password ) ) {
            return null;
        }
        return createAdminUserInternal( username, name, email, password, activated, disabled, userProperties );
    }


    private boolean validateAdminInfo( String username, String name, String email, String password ) throws Exception {
        if ( email == null ) {
            return false;
        }
        if ( username == null ) {
            username = email;
        }
        if ( name == null ) {
            name = email;
        }

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

        if ( !em.isPropertyValueUniqueForEntity( "user", "username", username ) ) {
            throw new DuplicateUniquePropertyExistsException( "user", "username", username );
        }

        if ( !em.isPropertyValueUniqueForEntity( "user", "email", email ) ) {
            throw new DuplicateUniquePropertyExistsException( "user", "email", email );
        }
        return true;
    }


    private UserInfo createAdminUserInternal( String username, String name, String email, String password,
                                              boolean activated, boolean disabled, Map<String, Object> userProperties )
            throws Exception {
        logger.info( "createAdminUserInternal: {}", username );

        if ( isBlank( password ) ) {
            password = encodeBase64URLSafeString( bytes( UUID.randomUUID() ) );
        }
        if ( username == null ) {
            username = email;
        }
        if ( name == null ) {
            name = email;
        }
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        User user = new User();
        user.setUsername( username );
        user.setName( name );
        user.setEmail( email );
        user.setActivated( activated );
        user.setConfirmed( !newAdminUsersRequireConfirmation() ); // only
        user.setDisabled( disabled );
        if ( userProperties != null ) {
            // double check no 'password' property just to be safe
            userProperties.remove( "password" );
            user.setProperties( userProperties );
        }
        user = em.create( user );

        return createAdminFrom( user, password );
    }


    public UserInfo getUserInfo( UUID applicationId, Entity entity ) {

        if ( entity == null ) {
            return null;
        }
        return new UserInfo( applicationId, entity.getUuid(), ( String ) entity.getProperty( "username" ),
                entity.getName(), ( String ) entity.getProperty( "email" ),
                ConversionUtils.getBoolean( entity.getProperty( "confirmed" ) ),
                ConversionUtils.getBoolean( entity.getProperty( "activated" ) ),
                ConversionUtils.getBoolean( entity.getProperty( "disabled" ) ),
                entity.getDynamicProperties(),
                ConversionUtils.getBoolean( entity.getProperty( "admin" )));
    }


    public UserInfo getUserInfo( UUID applicationId, Map<String, Object> properties ) {

        if ( properties == null ) {
            return null;
        }
        return new UserInfo( applicationId, properties );
    }


    @Override
    public List<UserInfo> getAdminUsersForOrganization( UUID organizationId ) throws Exception {

        if ( organizationId == null ) {
            return null;
        }

        List<UserInfo> users = new ArrayList<UserInfo>();

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        Results results =
                em.getCollection( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "users", null, 10000,
                        Level.ALL_PROPERTIES, false );
        for ( Entity entity : results.getEntities() ) {
            users.add( getUserInfo( smf.getManagementAppId(), entity ) );
        }

        return users;
    }


    @Override
    public UserInfo updateAdminUser( UserInfo user, String username, String name, String email,
                                     Map<String, Object> json ) throws Exception {

        /**
         * Only lock on the target values. We don't want lock contention if another
         * node is trying to set the property do a different value
         */
        Lock usernameLock =
                getUniqueUpdateLock( lockManager, smf.getManagementAppId(), username, "users", "username" );

        Lock emailLock = getUniqueUpdateLock( lockManager, smf.getManagementAppId(), email, "users", "email" );

        try {

            usernameLock.lock();
            emailLock.lock();

            EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

            SimpleEntityRef entityRef = new SimpleEntityRef( User.ENTITY_TYPE, user.getUuid() );
            if ( !isBlank( username ) ) {
                em.setProperty( entityRef, "username", username );
            }
            if ( !isBlank( name ) ) {
                em.setProperty( entityRef, "name", name );
            }
            if ( !isBlank( email ) ) {
                em.setProperty( entityRef, "email", email );
            }
            if ( json != null ) {
                json.remove( "password" );
                json.remove( "oldpassword" );
                json.remove( "newpassword" );
                Map<String, Object> userProperties = user.getProperties();
                userProperties.putAll( json );
                em.updateProperties( entityRef, userProperties );
            }

            user = getAdminUserByUuid( user.getUuid() );
        }
        finally {
            emailLock.unlock();
            usernameLock.unlock();
        }

        return user;
    }


    public User getAdminUserEntityByEmail( String email ) throws Exception {

        if ( email == null ) {
            return null;
        }

        return getUserEntityByIdentifier( smf.getManagementAppId(), Identifier.fromEmail( email ) );
    }


    @Override
    public UserInfo getAdminUserByEmail( String email ) throws Exception {
        if ( email == null ) {
            return null;
        }
        return getUserInfo( smf.getManagementAppId(),
                getUserEntityByIdentifier( smf.getManagementAppId(), Identifier.fromEmail( email ) ) );
    }


    public User getUserEntityByIdentifier( UUID applicationId, Identifier identifier ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        return em.get( em.getUserByIdentifier( identifier ), User.class );
    }


    @Override
    public UserInfo getAdminUserByUsername( String username ) throws Exception {
        if ( username == null ) {
            return null;
        }
        return getUserInfo( smf.getManagementAppId(),
                getUserEntityByIdentifier( smf.getManagementAppId(), Identifier.fromName( username ) ) );
    }


    @Override
    public User getAdminUserEntityByUuid( UUID id ) throws Exception {
        if ( id == null ) {
            return null;
        }
        return getUserEntityByIdentifier( smf.getManagementAppId(), Identifier.fromUUID( id ) );
    }


    @Override
    public UserInfo getAdminUserByUuid( UUID id ) throws Exception {
        return getUserInfo( smf.getManagementAppId(),
                getUserEntityByIdentifier( smf.getManagementAppId(), Identifier.fromUUID( id ) ) );
    }


    @Override
    public User getAdminUserEntityByIdentifier( Identifier id ) throws Exception {
        return getUserEntityByIdentifier( smf.getManagementAppId(), id );
    }


    @Override
    public UserInfo getAdminUserByIdentifier( Identifier id ) throws Exception {
        if ( id.isUUID() ) {
            return getAdminUserByUuid( id.getUUID() );
        }
        if ( id.isName() ) {
            return getAdminUserByUsername( id.getName() );
        }
        if ( id.isEmail() ) {
            return getAdminUserByEmail( id.getEmail() );
        }
        return null;
    }


    public User findUserEntity( UUID applicationId, String identifierString ) {

        User user = null;
        if ( UUIDUtils.isUUID( identifierString ) ) {
            try {
                Entity entity = getUserEntityByIdentifier( applicationId,
                        Identifier.fromUUID( UUID.fromString( identifierString ) ) );
                if ( entity != null ) {
                    user = ( User ) entity.toTypedEntity();
                    logger.info( "Found user {} as a UUID", identifierString );
                }
            }
            catch ( Exception e ) {
                logger.warn( "Unable to get user " + identifierString + " as a UUID, trying username..." );
            }
            return user;
        }
        // now we are either an email or a username. Let Indentifier handle the parsing of such.
        Identifier identifier = Identifier.from( identifierString );

        try {
            Entity entity = getUserEntityByIdentifier( applicationId, identifier );
            if ( entity != null ) {
                user = ( User ) entity.toTypedEntity();
                logger.info( "Found user {} as an {}", identifierString, identifier.getType() );
            }
        }
        catch ( Exception e ) {
            logger.warn( "Unable to get user {} as a {}", identifierString, identifier.getType());
            logger.warn( "Exception", e);
        }
        if ( user != null ) {
            return user;
        }

        return null;
    }


    @Override
    public UserInfo findAdminUser( String identifier ) {
        return getUserInfo( smf.getManagementAppId(), findUserEntity( smf.getManagementAppId(), identifier ) );
    }


    @Override
    public void setAdminUserPassword( UUID userId, String oldPassword, String newPassword ) throws Exception {

        if ( ( userId == null ) || ( oldPassword == null ) || ( newPassword == null ) ) {
            return;
        }
        User user = emf.getEntityManager( smf.getManagementAppId() ).get( userId, User.class );

        if ( !verify( smf.getManagementAppId(), user.getUuid(), oldPassword ) ) {
            throw new IncorrectPasswordException( "Old password does not match" );
        }

        setAdminUserPassword( userId, newPassword );
    }


    private static final String CREDENTIALS_HISTORY = "credentialsHistory";


    @Override
    public void setAdminUserPassword( UUID userId, String newPassword ) throws Exception {

        if ( ( userId == null ) || ( newPassword == null ) ) {
            return;
        }

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        User user = em.get( userId, User.class );

        CredentialsInfo newCredentials =
                encryptionService.defaultEncryptedCredentials( newPassword, user.getUuid(), smf.getManagementAppId() );

        int passwordHistorySize = calculatePasswordHistorySizeForUser( user.getUuid() );
        Map<String, CredentialsInfo> credsMap = cast( em.getDictionaryAsMap( user, CREDENTIALS_HISTORY ) );

        CredentialsInfo currentCredentials = null;
        if ( passwordHistorySize > 0 ) {
            ArrayList<CredentialsInfo> oldCreds = new ArrayList<CredentialsInfo>( credsMap.values() );
            Collections.sort( oldCreds );

            currentCredentials = readUserPasswordCredentials( smf.getManagementAppId(), user.getUuid(), user.getType() );

            // check credential history
            if ( encryptionService.verify( newPassword, currentCredentials, userId, smf.getManagementAppId() ) ) {
                throw new RecentlyUsedPasswordException();
            }
            for ( int i = 0; i < oldCreds.size() && i < passwordHistorySize; i++ ) {
                CredentialsInfo ci = oldCreds.get( i );
                if ( encryptionService.verify( newPassword, ci, userId, smf.getManagementAppId() ) ) {
                    throw new RecentlyUsedPasswordException();
                }
            }
        }

        // remove excess history
        if ( credsMap.size() > passwordHistorySize ) {
            ArrayList<UUID> oldUUIDs = new ArrayList<UUID>( credsMap.size() );
            for ( String uuid : credsMap.keySet() ) {
                oldUUIDs.add( UUID.fromString( uuid ) );
            }
            UUIDUtils.sort( oldUUIDs );
            for ( int i = 0; i < oldUUIDs.size() - passwordHistorySize; i++ ) {
                em.removeFromDictionary( user, CREDENTIALS_HISTORY, oldUUIDs.get( i ).toString() );
            }
        }

        if ( passwordHistorySize > 0 ) {
            UUID uuid = UUIDUtils.newTimeUUID();
            em.addToDictionary( user, CREDENTIALS_HISTORY, uuid.toString(), currentCredentials );
        }

        writeUserPassword( smf.getManagementAppId(), user, newCredentials );
        writeUserMongoPassword( smf.getManagementAppId(), user, encryptionService
                .plainTextCredentials( mongoPassword( ( String ) user.getProperty( "username" ), newPassword ),
                        user.getUuid(), smf.getManagementAppId() ) );

    }


    public int calculatePasswordHistorySizeForUser( UUID userId ) throws Exception {

        logger.debug( "calculatePasswordHistorySizeForUser " + userId.toString() );

        int size = 0;
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

        Results orgResults = em.getCollection( new SimpleEntityRef( User.ENTITY_TYPE, userId ),
                Schema.COLLECTION_GROUPS, null, 10000, Level.REFS, false );

        logger.debug( "    orgResults.size() = " +  orgResults.size() );

        for ( EntityRef orgRef : orgResults.getRefs() ) {
            Map properties = em.getDictionaryAsMap( orgRef, ORGANIZATION_PROPERTIES_DICTIONARY );
            if ( properties != null ) {
                OrganizationInfo orgInfo = new OrganizationInfo( null, null, properties );
                logger.debug( "    orgInfo.getPasswordHistorySize() = " +  orgInfo.getPasswordHistorySize() );
                size = Math.max( orgInfo.getPasswordHistorySize(), size );
            }
        }

        return size;
    }


    @Override
    public boolean verifyAdminUserPassword( UUID userId, String password ) throws Exception {
        if ( ( userId == null ) || ( password == null ) ) {
            return false;
        }
        User user = emf.getEntityManager( smf.getManagementAppId() ).get( userId, User.class );

        return verify( smf.getManagementAppId(), user.getUuid(), password );
    }


    @Override
    public UserInfo verifyAdminUserPasswordCredentials( String name, String password ) throws Exception {
        UserInfo userInfo = null;

        logger.debug("verifyAdminUserPasswordCredentials for {}/{}", name, password);

        User user = findUserEntity( smf.getManagementAppId(), name );
        if ( user == null ) {
            return null;
        }

        if ( verify( smf.getManagementAppId(), user.getUuid(), password ) ) {
            userInfo = getUserInfo( smf.getManagementAppId(), user );

            boolean userIsSuperAdmin = properties.getSuperUser().isEnabled() && properties.getSuperUser().getEmail().equals(userInfo.getEmail());

            boolean testUserEnabled = parseBoolean( properties.getProperty( PROPERTIES_SETUP_TEST_ACCOUNT ) );
            boolean userIsTestUser = testUserEnabled && properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_EMAIL)
                    .equals(userInfo.getEmail());

            if ( !userIsSuperAdmin && !userIsTestUser ) {

                if ( !userInfo.isConfirmed() && newAdminUsersRequireConfirmation() ) {
                    throw new UnconfirmedAdminUserException();
                }
                if ( !userInfo.isActivated() ) {
                    throw new UnactivatedAdminUserException();
                }
                if ( userInfo.isDisabled() ) {
                    throw new DisabledAdminUserException();
                }
            }
            return userInfo;
        }
        logger.info( "password compare fail for {}", name );
        return null;
    }


    @Override
    public UserInfo verifyMongoCredentials( String name, String nonce, String key ) throws Exception {

        Entity user = findUserEntity( smf.getManagementAppId(), name );

        if ( user == null ) {
            return null;
        }

        String mongo_pwd = readUserMongoPassword( smf.getManagementAppId(), user.getUuid(), user.getType() ).getSecret();

        if ( mongo_pwd == null ) {
            throw new IncorrectPasswordException( "Your mongo password has not be set" );
        }

        String expected_key = DigestUtils.md5Hex( nonce + user.getProperty( "username" ) + mongo_pwd );

        if ( !expected_key.equalsIgnoreCase( key ) ) {
            throw new IncorrectPasswordException();
        }

        UserInfo userInfo = new UserInfo( smf.getManagementAppId(), user.getProperties() );

        if ( !userInfo.isActivated() ) {
            throw new UnactivatedAdminUserException();
        }
        if ( userInfo.isDisabled() ) {
            throw new DisabledAdminUserException();
        }

        return userInfo;
    }


    // TokenType tokenType, String type, AuthPrincipalInfo principal,
    // Map<String, Object> state
    public String getTokenForPrincipal( TokenCategory token_category, String token_type, UUID applicationId,
                                        AuthPrincipalType principal_type, UUID id, long duration ) throws Exception {

        if ( anyNull( token_category, applicationId, principal_type, id ) ) {
            return null;
        }

        return tokens
                .createToken( token_category, token_type, new AuthPrincipalInfo( principal_type, id, applicationId ),
                        null, duration );
    }


    public void revokeTokensForPrincipal( AuthPrincipalType principalType, UUID applicationId, UUID id )
            throws Exception {

        if ( anyNull( applicationId, principalType, id ) ) {
            throw new IllegalArgumentException( "applicationId, principal_type and id are required" );
        }

        AuthPrincipalInfo principal = new AuthPrincipalInfo( principalType, id, applicationId );

        tokens.removeTokens( principal );
    }


    public AuthPrincipalInfo getPrincipalFromAccessToken( String token, String expected_token_type,
                                                          AuthPrincipalType expected_principal_type ) throws Exception {

        TokenInfo tokenInfo = tokens.getTokenInfo( token );

        if ( tokenInfo == null ) {
            return null;
        }

        if ( ( expected_token_type != null ) && !expected_token_type.equals( tokenInfo.getType() ) ) {
            return null;
        }

        AuthPrincipalInfo principal = tokenInfo.getPrincipal();
        if ( principal == null ) {
            return null;
        }

        if ( ( expected_principal_type != null ) && !expected_principal_type.equals( principal.getType() ) ) {
            return null;
        }

        return principal;
    }


    public Entity getEntityFromAccessToken( String token, String expected_token_type,
                                            AuthPrincipalType expected_principal_type ) throws Exception {

        AuthPrincipalInfo principal =
                getPrincipalFromAccessToken( token, expected_token_type, expected_principal_type );
        if ( principal == null ) {
            return null;
        }

        return getEntityFromPrincipal( principal );
    }


    public Entity getEntityFromPrincipal( AuthPrincipalInfo principal ) throws Exception {

        EntityManager em = emf.getEntityManager(
            principal.getApplicationId() != null
                ? principal.getApplicationId() : smf.getManagementAppId() );

        Entity entity = em.get( new SimpleEntityRef(
                principal.getType().getEntityType(), principal.getUuid()));

        return entity;
    }


    @Override
    public String getAccessTokenForAdminUser( UUID userId, long duration ) throws Exception {

        return getTokenForPrincipal( ACCESS, null, smf.getManagementAppId(), ADMIN_USER, userId, duration );
    }


    /*
   * (non-Javadoc)
   *
   * @see
   * org.apache.usergrid.management.ManagementService#revokeAccessTokensForAdminUser
   * (java.util.UUID)
   */
    @Override
    public void revokeAccessTokensForAdminUser( UUID userId ) throws Exception {
        revokeTokensForPrincipal( ADMIN_USER, smf.getManagementAppId(), userId );
    }


    @Override
    public void revokeAccessTokenForAdminUser( UUID userId, String token ) throws Exception {
        if ( anyNull( userId, token ) ) {
            throw new IllegalArgumentException( "token is required" );
        }

        Entity user = getAdminUserEntityFromAccessToken( token );
        if ( !user.getUuid().equals( userId ) ) {
            throw new TokenException( "Could not match token : " + token );
        }

        tokens.revokeToken( token );
    }


    @Override
    public Entity getAdminUserEntityFromAccessToken( String token ) throws Exception {

        Entity user = getEntityFromAccessToken( token, null, ADMIN_USER );
        return user;
    }


    @Override
    public UserInfo getAdminUserInfoFromAccessToken( String token ) throws Exception {
        Entity user = getAdminUserEntityFromAccessToken( token );
        return new UserInfo( smf.getManagementAppId(), user.getProperties() );
    }


    @Override
    public BiMap<UUID, String> getOrganizationsForAdminUser( UUID userId ) throws Exception {

        if ( userId == null ) {
            return null;
        }

        BiMap<UUID, String> organizations = HashBiMap.create();
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        Results results = em.getCollection(  new SimpleEntityRef( User.ENTITY_TYPE, userId ),
            Schema.COLLECTION_GROUPS, null, 1000, Level.ALL_PROPERTIES, false );

        String path = null;

        do {
            for ( Entity entity : results.getEntities() ) {

                path = ( String ) entity.getProperty( PROPERTY_PATH );

                if ( path != null ) {
                    path = path.toLowerCase();
                }

                organizations.put( entity.getUuid(), path );
            }

            results = results.getNextPageResults();
        }while(results != null);

        return organizations;
    }


    @Override
    public Map<String, Object> getAdminUserOrganizationData( UUID userId ) throws Exception {
        UserInfo user = getAdminUserByUuid( userId );
        return getAdminUserOrganizationData( user, true );
    }


    @Override
    public Long getLastAdminPasswordChange( UUID userId ) throws Exception {
        CredentialsInfo ci = readUserPasswordCredentials( smf.getManagementAppId(), userId, User.ENTITY_TYPE );
        return ci.getCreated();
    }


    @Override
    public Map<String, Object> getAdminUserOrganizationData( UserInfo user, boolean deep ) throws Exception {

        Map<String, Object> json = new HashMap<String, Object>();

        json.putAll( JsonUtils.toJsonMap( user ) );

        Map<String, Map<String, Object>> jsonOrganizations = new HashMap<String, Map<String, Object>>();
        json.put( "organizations", jsonOrganizations );

        Map<UUID, String> organizations;

        boolean superuser_enabled = getBooleanProperty( PROPERTIES_SYSADMIN_LOGIN_ALLOWED );
        String superuser_username = properties.getProperty( PROPERTIES_SYSADMIN_LOGIN_NAME );
        if ( superuser_enabled && ( superuser_username != null ) && superuser_username.equals( user.getUsername() ) ) {
            organizations = buildOrgBiMap( getOrganizations( null, 10 ) );
        }
        else {
            organizations = getOrganizationsForAdminUser( user.getUuid() );
        }

        for ( Entry<UUID, String> organization : organizations.entrySet() ) {
            Map<String, Object> jsonOrganization = new HashMap<String, Object>();

            jsonOrganizations.put( organization.getValue(), jsonOrganization );

            jsonOrganization.put( PROPERTY_NAME, organization.getValue() );
            jsonOrganization.put( PROPERTY_UUID, organization.getKey() );
            jsonOrganization.put( "properties", getOrganizationByUuid( organization.getKey() ).getProperties() );

            if ( deep ) {
                BiMap<UUID, String> applications = getApplicationsForOrganization( organization.getKey() );
                jsonOrganization.put( "applications", applications.inverse() );

                List<UserInfo> users = getAdminUsersForOrganization( organization.getKey() );
                Map<String, Object> jsonUsers = new HashMap<String, Object>();
                for ( UserInfo u : users ) {
                    jsonUsers.put( u.getUsername(), u );
                }
                jsonOrganization.put( "users", jsonUsers );
            }
        }

        return json;
    }


    @Override
    public Map<String, Object> getOrganizationData( OrganizationInfo organization ) throws Exception {

        Map<String, Object> jsonOrganization = new HashMap<>();
        jsonOrganization.putAll( JsonUtils.toJsonMap( organization ) );

        BiMap<UUID, String> applications = getApplicationsForOrganization( organization.getUuid() );
        jsonOrganization.put( "applications", applications.inverse() );

        List<UserInfo> users = getAdminUsersForOrganization( organization.getUuid() );
        Map<String, Object> jsonUsers = new HashMap<>();
        for ( UserInfo u : users ) {
            jsonUsers.put( u.getUsername(), u );
        }
        jsonOrganization.put( "users", jsonUsers );

        return jsonOrganization;
    }


    @Override
    public void addAdminUserToOrganization( UserInfo user, OrganizationInfo organization, boolean email )
            throws Exception {

        if ( ( user == null ) || ( organization == null ) ) {
            return;
        }

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.addToCollection( new SimpleEntityRef( Group.ENTITY_TYPE, organization.getUuid() ), "users",
                new SimpleEntityRef( User.ENTITY_TYPE, user.getUuid() ) );

        if ( email ) {
            sendAdminUserInvitedEmail( user, organization );
        }
    }


    @Override
    public void removeAdminUserFromOrganization( UUID userId, UUID organizationId ) throws Exception {

        if ( ( userId == null ) || ( organizationId == null ) ) {
            return;
        }

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

        try {
            if ( em.getCollection( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "users", null, 2,
                    Level.IDS, false ).size() <= 1 ) {
                throw new Exception();
            }
        }
        catch ( Exception e ) {
            throw new UnableToLeaveOrganizationException( "Organizations must have at least one member." );
        }

        em.removeFromCollection( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "users",
                new SimpleEntityRef( User.ENTITY_TYPE, userId ) );
    }


    @Override
    public ApplicationInfo createApplication( UUID organizationId, String applicationName ) throws Exception {

        return createApplication( organizationId, applicationName, null );
    }


    @Override
    public ApplicationInfo createApplication( UUID organizationId, String applicationName,
                                              Map<String, Object> properties ) throws Exception {

        if ( ( organizationId == null ) || ( applicationName == null ) ) {
            return null;
        }

        if ( properties == null ) {
            properties = new HashMap<>();
        }

        OrganizationInfo organizationInfo = getOrganizationByUuid( organizationId );
        Entity appInfo = emf.createApplicationV2(
            organizationInfo.getName(), applicationName, properties);

        writeUserToken( smf.getManagementAppId(), appInfo,
            encryptionService.plainTextCredentials(
                generateOAuthSecretKey( AuthPrincipalType.APPLICATION ),
                null,
                smf.getManagementAppId() ) );

        UUID applicationId = addApplicationToOrganization( organizationId, appInfo );

        UserInfo user = null;
        try {
            user = SubjectUtils.getUser();
        }
        catch ( UnavailableSecurityManagerException e ) {
            // occurs in the rare case that this is called before the full stack is initialized
            logger.warn("Error getting user, application created activity will not be created", e);
        }
        if ( ( user != null ) && user.isAdminUser() ) {
            postOrganizationActivity( organizationId, user, "create", appInfo, "Application", applicationName,
                "<a href=\"mailto:" + user.getEmail() + "\">" + user.getName() + " (" + user.getEmail()
                    + ")</a> created a new application named " + applicationName, null );
        }



        return new ApplicationInfo( applicationId, appInfo.getName() );
    }


    @Override
    public void deleteApplication(UUID applicationId) throws Exception {
        emf.deleteApplication( applicationId );
    }


    @Override
    public ApplicationInfo restoreApplication(UUID applicationId) throws Exception {

        ApplicationInfo app = getDeletedApplicationInfo( applicationId );
        if ( app == null ) {
            throw new EntityNotFoundException("Deleted application ID " + applicationId + " not found");
        }

        if ( emf.lookupApplication( app.getName() ) != null ) {
            throw new ConflictException("Cannot restore application, one with that name already exists.");
        }

        // restore application_info entity

        EntityManager em = emf.getEntityManager( emf.getManagementAppId() );
        Entity appInfo = emf.restoreApplication(applicationId);

        // restore token

        writeUserToken( smf.getManagementAppId(), appInfo,
            encryptionService.plainTextCredentials(
                generateOAuthSecretKey( AuthPrincipalType.APPLICATION ),
                null,
                smf.getManagementAppId() ) );

        String orgName = appInfo.getName().split("/")[0];
        EntityRef alias = em.getAlias( Group.ENTITY_TYPE, orgName );
        Entity orgEntity = em.get( alias );

        addApplicationToOrganization( orgEntity.getUuid(), appInfo );

        // create activity

        UserInfo user = null;
        try {
            user = SubjectUtils.getUser();
        }
        catch ( UnavailableSecurityManagerException e ) {
            // occurs in the rare case that this is called before the full stack is initialized
            logger.warn("Error getting user, application restored created activity will not be created", e);
        }
        if ( ( user != null ) && user.isAdminUser() ) {
            postOrganizationActivity( orgEntity.getUuid(), user, "restore", appInfo, "Application", appInfo.getName(),
                "<a href=\"mailto:" + user.getEmail() + "\">" + user.getName() + " (" + user.getEmail()
                    + ")</a> restored an application named " + appInfo.getName(), null );
        }



        return new ApplicationInfo( applicationId, appInfo.getName() );
    }


    @Override
    public OrganizationInfo getOrganizationForApplication( UUID applicationInfoId ) throws Exception {

        if ( applicationInfoId == null ) {
            return null;
        }

        final EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

        Results r = em.getConnectingEntities(
                new SimpleEntityRef(CpNamingUtils.APPLICATION_INFO, applicationInfoId),
                "owns", Group.ENTITY_TYPE, Level.ALL_PROPERTIES );

        Entity entity = r.getEntity();
        if ( entity != null ) {
            return new OrganizationInfo( entity.getUuid(), ( String ) entity.getProperty( "path" ) );
        }

        return null;
    }


    @Override
    public BiMap<UUID, String> getApplicationsForOrganization( UUID organizationGroupId ) throws Exception {

        if ( organizationGroupId == null ) {
            return null;
        }
        final BiMap<UUID, String> applications = HashBiMap.create();
        final EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

        // query for application_info entities
        final Results results = em.getConnectedEntities(
                new SimpleEntityRef(Group.ENTITY_TYPE, organizationGroupId),
                "owns", CpNamingUtils.APPLICATION_INFO, Level.ALL_PROPERTIES );

        final PagingResultsIterator itr = new PagingResultsIterator( results );

        String entityName;

        while ( itr.hasNext() ) {

            final Entity entity = ( Entity ) itr.next();

            entityName = entity.getName();

            if ( entityName != null ) {
                entityName = entityName.toLowerCase();
            }

            // make sure we return applicationId and not the application_info UUID
            UUID applicationId = UUIDUtils.tryExtractUUID(
                entity.getProperty( PROPERTY_APPLICATION_ID ).toString() );

            applications.put( applicationId, entityName );
        }


        return applications;
    }


    @Override
    public BiMap<UUID, String> getApplicationsForOrganizations( Set<UUID> organizationIds ) throws Exception {
        if ( organizationIds == null ) {
            return null;
        }
        BiMap<UUID, String> applications = HashBiMap.create();
        for ( UUID organizationId : organizationIds ) {
            BiMap<UUID, String> organizationApplications = getApplicationsForOrganization( organizationId );
            applications.putAll( organizationApplications );
        }
        return applications;
    }


    /**
     * @return UUID of the application itself (NOT the application_info entity).
     */
    @Override
    public UUID addApplicationToOrganization(UUID organizationId, Entity appInfo) throws Exception {

        UUID applicationId = appInfo.getUuid();

        if ( ( organizationId == null ) || ( applicationId == null ) ) {
            return null;
        }

        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.createConnection( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "owns", appInfo );

        return applicationId;
    }


    @Override
    public void deleteOrganizationApplication( UUID organizationId, UUID applicationId ) throws Exception {
        // TODO Auto-generated method stub

    }


    @Override
    public void removeOrganizationApplication( UUID organizationId, UUID applicationId ) throws Exception {
        // TODO Auto-generated method stub

    }


    @Override
    public ApplicationInfo getApplicationInfo( String applicationName ) throws Exception {
        if ( applicationName == null ) {
            return null;
        }
        UUID applicationId = emf.lookupApplication( applicationName );
        if ( applicationId == null ) {
            return null;
        }
        return new ApplicationInfo( applicationId, applicationName.toLowerCase() );
    }


    @Override
    public ApplicationInfo getApplicationInfo( UUID applicationId ) throws Exception {
        if ( applicationId == null ) {
            return null;
        }
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        EntityRef mgmtAppRef = new SimpleEntityRef( Schema.TYPE_APPLICATION, smf.getManagementAppId() );

        final Results results = em.searchCollection(mgmtAppRef, CpNamingUtils.APPLICATION_INFOS,
            Query.fromQL("select * where " + PROPERTY_APPLICATION_ID + " = " + applicationId.toString()));

        Entity entity = results.getEntity();

        if ( entity != null ) {
            return new ApplicationInfo( applicationId, entity.getName() );
        }
        return null;
    }


    @Override
    public ApplicationInfo getDeletedApplicationInfo(UUID applicationId) throws Exception {
        if ( applicationId == null ) {
            return null;
        }
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        EntityRef mgmtAppRef = new SimpleEntityRef( Schema.TYPE_APPLICATION, smf.getManagementAppId() );

        final Results results = em.searchCollection(mgmtAppRef, CpNamingUtils.DELETED_APPLICATION_INFOS,
            Query.fromQL("select * where " + PROPERTY_APPLICATION_ID + " = " + applicationId.toString()));

        Entity entity = results.getEntity();

        if ( entity != null ) {
            return new ApplicationInfo( applicationId, entity.getName() );
        }
        return null;
    }


    @Override
    public ApplicationInfo getApplicationInfo( Identifier id ) throws Exception {
        if ( id == null ) {
            return null;
        }
        if ( id.isUUID() ) {
            return getApplicationInfo( id.getUUID() );
        }
        if ( id.isName() ) {
            return getApplicationInfo( id.getName() );
        }
        return null;
    }


    @Override
    public ApplicationInfo getApplicationInfoFromAccessToken( String token ) throws Exception {
        Entity entity = getEntityFromAccessToken( token, null, APPLICATION );
        if ( entity == null ) {
            throw new TokenException( "Could not find an entity for that access token: " + token );
        }
        return new ApplicationInfo( entity.getProperties() );
    }


    @Override
    public ServiceResults getApplicationMetadata( UUID applicationId ) throws Exception {

        if ( applicationId == null ) {
            return ServiceResults.genericServiceResults();
        }

        EntityManager em = emf.getEntityManager( applicationId );
        Entity entity = em.get( em.getApplicationRef() );

        Results r = Results.fromEntity( entity );

        Map<String, Object> collections = em.getApplicationCollectionMetadata();
        if ( collections.size() > 0 ) {
            r.setMetadata( em.getApplicationRef().getUuid(), "collections", collections );
        }
        return genericServiceResults( r );
    }


    public String getSecret( UUID applicationId, AuthPrincipalType type, UUID entityId ) throws Exception {

        if ( AuthPrincipalType.ORGANIZATION.equals( type )) {
            UUID ownerId = smf.getManagementAppId();
            return getCredentialsSecret( readUserToken( ownerId, entityId, Group.ENTITY_TYPE ) );

        } else if ( AuthPrincipalType.APPLICATION.equals( type ) ) {
            UUID ownerId = smf.getManagementAppId();
            return getCredentialsSecret( readUserToken( ownerId, entityId, Application.ENTITY_TYPE ) );

        }
        else if ( AuthPrincipalType.ADMIN_USER.equals( type ) || AuthPrincipalType.APPLICATION_USER.equals( type ) ) {
            return getCredentialsSecret( readUserPasswordCredentials( applicationId, entityId, User.ENTITY_TYPE ) );
        }

        throw new IllegalArgumentException( "Must specify an admin user, organization or application principal" );
    }


    @Override
    public String getClientIdForOrganization( UUID organizationId ) {
        return ClientCredentialsInfo.getClientIdForTypeAndUuid( AuthPrincipalType.ORGANIZATION, organizationId );
    }


    @Override
    public String getClientSecretForOrganization( UUID organizationId ) throws Exception {
        return getSecret( smf.getManagementAppId(), AuthPrincipalType.ORGANIZATION, organizationId );
    }


    @Override
    public String getClientIdForApplication( UUID applicationId ) {
        return ClientCredentialsInfo.getClientIdForTypeAndUuid( AuthPrincipalType.APPLICATION, applicationId );
    }


    @Override
    public String getClientSecretForApplication( UUID applicationId ) throws Exception {
        return getSecret( smf.getManagementAppId(), AuthPrincipalType.APPLICATION, applicationId );
    }


    public String newSecretKey( AuthPrincipalType type, UUID id ) throws Exception {
        String secret = generateOAuthSecretKey( type );

        writeUserToken( smf.getManagementAppId(), new SimpleEntityRef( type.getEntityType(), id ),
                encryptionService.plainTextCredentials( secret, id, smf.getManagementAppId() ) );

        return secret;
    }


    @Override
    public String newClientSecretForOrganization( UUID organizationId ) throws Exception {
        return newSecretKey( AuthPrincipalType.ORGANIZATION, organizationId );
    }


    @Override
    public String newClientSecretForApplication( UUID applicationId ) throws Exception {
        return newSecretKey( AuthPrincipalType.APPLICATION, applicationId );
    }


    @Override
    public AccessInfo authorizeClient( String clientId, String clientSecret, long ttl ) throws Exception {
        if ( ( clientId == null ) || ( clientSecret == null ) ) {
            return null;
        }
        UUID uuid = getUUIDFromClientId( clientId );
        if ( uuid == null ) {
            return null;
        }
        AuthPrincipalType type = getTypeFromClientId( clientId );
        if ( type == null ) {
            return null;
        }
        AccessInfo access_info = null;

        if ( clientSecret.equals( getSecret( smf.getManagementAppId(), type, uuid ) ) ) {

            String token = getTokenForPrincipal( ACCESS, null, smf.getManagementAppId(), type, uuid, ttl );

            long duration = tokens.getMaxTokenAgeInSeconds( token );

            access_info = new AccessInfo().withExpiresIn( duration ).withAccessToken( token );

            if ( type.equals( AuthPrincipalType.APPLICATION ) ) {
                ApplicationInfo app = getApplicationInfo( uuid );
                access_info = access_info.withProperty( "application", app.getId() );
            }
            else if ( type.equals( AuthPrincipalType.ORGANIZATION ) ) {
                OrganizationInfo organization = getOrganizationByUuid( uuid );
                access_info = access_info.withProperty( "organization", getOrganizationData( organization ) );
            }
        }
        return access_info;
    }


    @Override
    public PrincipalCredentialsToken getPrincipalCredentialsTokenForClientCredentials( String clientId,
                                                                                       String clientSecret )
            throws Exception {
        if ( ( clientId == null ) || ( clientSecret == null ) ) {
            return null;
        }
        UUID uuid = getUUIDFromClientId( clientId );
        if ( uuid == null ) {
            return null;
        }
        AuthPrincipalType type = getTypeFromClientId( clientId );
        if ( type == null ) {
            return null;
        }

        PrincipalCredentialsToken token = null;
        if ( clientSecret.equals( getSecret( smf.getManagementAppId(), type, uuid))) {
            if ( type.equals( AuthPrincipalType.APPLICATION ) ) {
                ApplicationInfo app = getApplicationInfo( uuid );
                token = new PrincipalCredentialsToken( new ApplicationPrincipal( app ),
                        new ApplicationClientCredentials( clientId, clientSecret ) );
            }
            else if ( type.equals( AuthPrincipalType.ORGANIZATION ) ) {
                OrganizationInfo organization = getOrganizationByUuid( uuid );
                token = new PrincipalCredentialsToken( new OrganizationPrincipal( organization ),
                        new OrganizationClientCredentials( clientId, clientSecret ) );
            }
        }
        return token;
    }


    public AccessInfo authorizeAppUser( String clientType, String clientId, String clientSecret ) throws Exception {

        return null;
    }


    @Override
    public String getPasswordResetTokenForAdminUser( UUID userId, long ttl ) throws Exception {
        return getTokenForPrincipal( EMAIL, TOKEN_TYPE_PASSWORD_RESET, smf.getManagementAppId(), ADMIN_USER, userId,
                ttl );
    }


    @Override
    public boolean checkPasswordResetTokenForAdminUser( UUID userId, String token ) throws Exception {
        AuthPrincipalInfo principal = null;
        try {
            principal = getPrincipalFromAccessToken( token, TOKEN_TYPE_PASSWORD_RESET, ADMIN_USER );
        }
        catch ( Exception e ) {
            logger.error( "Unable to verify token", e );
        }
        return ( principal != null ) && userId.equals( principal.getUuid() );
    }


    @Override
    public String getActivationTokenForAdminUser( UUID userId, long ttl ) throws Exception {
        return getTokenForPrincipal( EMAIL, TOKEN_TYPE_ACTIVATION, smf.getManagementAppId(), ADMIN_USER, userId, ttl );
    }


    @Override
    public String getConfirmationTokenForAdminUser( UUID userId, long ttl ) throws Exception {
        return getTokenForPrincipal( EMAIL, TOKEN_TYPE_CONFIRM, smf.getManagementAppId(), ADMIN_USER, userId, ttl );
    }


    @Override
    public void activateAdminUser( UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "activated", true );
    }


    @Override
    public User deactivateUser( UUID applicationId, UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );

        User user = em.get( userId, User.class );

        if ( user == null ) {
            throw new ManagementException(
                    String.format( "User with id %s does not exist in app %s", userId, applicationId ) );
        }

        user.setActivated( false );
        user.setDeactivated( System.currentTimeMillis() );

        em.update( user );

        // revoke all access tokens for the app
        revokeAccessTokensForAppUser( applicationId, userId );

        return user;
    }


    @Override
    public boolean isAdminUserActivated( UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        return Boolean.TRUE.equals( em.getProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "activated" ) );
    }


    @Override
    public void confirmAdminUser( UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "confirmed", true );
    }


    @Override
    public void unconfirmAdminUser( UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "confirmed", false );
    }


    @Override
    public boolean isAdminUserConfirmed( UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        return Boolean.TRUE.equals( em.getProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "confirmed" ) );
    }


    @Override
    public void enableAdminUser( UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "disabled", false );
    }


    @Override
    public void disableAdminUser( UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "disabled", true );

        revokeAccessTokensForAdminUser( userId );
    }


    @Override
    public boolean isAdminUserEnabled( UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        return !Boolean.TRUE.equals( em.getProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "disabled" ) );
    }


    public String emailMsg( Map<String, String> values, String propertyName ) {
        return new StrSubstitutor( values ).replace( properties.getProperty( propertyName ) );
    }


    private String appendEmailFooter( String msg ) {
        return msg + "\n" + properties.getProperty( PROPERTIES_EMAIL_FOOTER );
    }


    @Override
    public void startAdminUserPasswordResetFlow( UserInfo user ) throws Exception {
        String token = getPasswordResetTokenForAdminUser( user.getUuid(), 0 );

        String reset_url =
                String.format( properties.getProperty( PROPERTIES_ADMIN_RESETPW_URL ), user.getUuid().toString() )
                        + "?token=" + token;

        Map<String, String> pageContext = hashMap( "reset_url", reset_url )
                .map( "reset_url_base", properties.getProperty( PROPERTIES_ADMIN_RESETPW_URL ) )
                .map( "user_uuid", user.getUuid().toString() ).map( "raw_token", token );


        sendHtmlMail( properties, user.getDisplayEmailAddress(), properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                "Password Reset", appendEmailFooter( emailMsg( pageContext, PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET ) ) );
    }


    @Override
    public String getActivationTokenForOrganization( UUID organizationId, long ttl ) throws Exception {
        return getTokenForPrincipal( EMAIL, TOKEN_TYPE_ACTIVATION, smf.getManagementAppId(), ORGANIZATION,
                organizationId, ttl );
    }


    @Override
    public void startOrganizationActivationFlow( OrganizationInfo organization ) throws Exception {
        logger.info( "startOrganizationActivationFlow: {}", organization.getName() );

        try {
            String token = getActivationTokenForOrganization( organization.getUuid(), 0 );
            String activation_url = String.format( properties.getProperty( PROPERTIES_ORGANIZATION_ACTIVATION_URL ),
                    organization.getUuid().toString() ) + "?token=" + token;
            List<UserInfo> users = getAdminUsersForOrganization( organization.getUuid() );
            String organization_owners = null;
            for ( UserInfo user : users ) {
                organization_owners = ( organization_owners == null ) ? user.getHTMLDisplayEmailAddress() :
                                      organization_owners + ", " + user.getHTMLDisplayEmailAddress();
            }
            if ( newOrganizationsNeedSysAdminApproval() ) {
                logger.info( "sending SysAdminApproval confirmation email: {}", organization.getName() );
                sendHtmlMail( properties, properties.getProperty( PROPERTIES_SYSADMIN_EMAIL ),
                        properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                        "Request For Organization Account Activation " + organization.getName(), appendEmailFooter(
                        emailMsg( hashMap( "organization_name", organization.getName() )
                                .map( "activation_url", activation_url )
                                .map( "organization_owners", organization_owners ),
                                PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION ) ) );
                sendOrganizationEmail( organization, "Organization Account Confirmed",
                        emailMsg( hashMap( "organization_name", organization.getName() ),
                                PROPERTIES_EMAIL_ORGANIZATION_CONFIRMED_AWAITING_ACTIVATION ) );
            }
            else if ( properties.newOrganizationsRequireConfirmation() ) {
                logger.info( "sending account confirmation email: {}", organization.getName() );
                sendOrganizationEmail( organization, "Organization Account Confirmation", emailMsg(
                        hashMap( "organization_name", organization.getName() )
                                .map( "confirmation_url", activation_url ),
                        PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION ) );
                sendSysAdminNewOrganizationActivatedNotificationEmail( organization );
            }
            else {
                logger.info( "activating organization (no confirmation): {}", organization.getName() );
                activateOrganization( organization, false );
                sendSysAdminNewOrganizationActivatedNotificationEmail( organization );
            }
        }
        catch ( Exception e ) {
            logger.error( "Unable to send activation emails to " + organization.getName(), e );
        }
    }


    @Override
    public ActivationState handleActivationTokenForOrganization( UUID organizationId, String token ) throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken( token, TOKEN_TYPE_ACTIVATION, ORGANIZATION );
        if ( ( principal != null ) && organizationId.equals( principal.getUuid() ) ) {
            OrganizationInfo organization = this.getOrganizationByUuid( organizationId );
            sendOrganizationActivatedEmail( organization );
            sendSysAdminNewOrganizationActivatedNotificationEmail( organization );

            activateOrganization( organization, false );

            return ActivationState.ACTIVATED;
        }
        return ActivationState.UNKNOWN;
    }


    public void sendOrganizationActivatedEmail( OrganizationInfo organization ) throws Exception {
        sendOrganizationEmail( organization, "Organization Account Activated: " + organization.getName(),
                emailMsg( hashMap( "organization_name", organization.getName() ),
                        PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED ) );
    }


    public void sendSysAdminNewOrganizationActivatedNotificationEmail( OrganizationInfo organization )
            throws Exception {
        if ( properties.notifySysAdminOfNewOrganizations() ) {
            List<UserInfo> users = getAdminUsersForOrganization( organization.getUuid() );
            String organization_owners = null;
            for ( UserInfo user : users ) {
                organization_owners = ( organization_owners == null ) ? user.getHTMLDisplayEmailAddress() :
                                      organization_owners + ", " + user.getHTMLDisplayEmailAddress();
            }
            sendHtmlMail( properties, properties.getProperty( PROPERTIES_SYSADMIN_EMAIL ),
                    properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                    "Organization Account Activated " + organization.getName(), appendEmailFooter( emailMsg(
                    hashMap( "organization_name", organization.getName() )
                            .map( "organization_owners", organization_owners ),
                    PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATED ) ) );
        }
    }


    @Override
    public void sendOrganizationEmail( OrganizationInfo organization, String subject, String html ) throws Exception {
        List<UserInfo> users = getAdminUsersForOrganization( organization.getUuid() );
        for ( UserInfo user : users ) {
            sendHtmlMail( properties, user.getDisplayEmailAddress(), properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                    subject, appendEmailFooter( html ) );
        }
    }


    @Override
    public void startAdminUserActivationFlow( UserInfo user ) throws Exception {
        if ( user.isActivated() ) {
            sendAdminUserConfirmationEmail( user );
            sendAdminUserActivatedEmail( user );
            sendSysAdminNewAdminActivatedNotificationEmail( user );
        }
        else {
            if ( newAdminUsersRequireConfirmation() ) {
                sendAdminUserConfirmationEmail( user );
            }
            else if ( newAdminUsersNeedSysAdminApproval() ) {
                sendSysAdminRequestAdminActivationEmail( user );
            }
            else {
                // sdg: There seems to be a hole in the logic. The user has been
                // created
                // in an inactive state but nobody is being notified.
                activateAdminUser( user.getUuid() );
            }
        }
    }


    @Override
    public ActivationState handleConfirmationTokenForAdminUser( UUID userId, String token ) throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken( token, TOKEN_TYPE_CONFIRM, ADMIN_USER );
        if ( ( principal != null ) && userId.equals( principal.getUuid() ) ) {
            UserInfo user = getAdminUserByUuid( principal.getUuid() );
            confirmAdminUser( user.getUuid() );
            if ( newAdminUsersNeedSysAdminApproval() ) {
                sendAdminUserConfirmedAwaitingActivationEmail( user );
                sendSysAdminRequestAdminActivationEmail( user );
                return ActivationState.CONFIRMED_AWAITING_ACTIVATION;
            }
            else {
                activateAdminUser( principal.getUuid() );
                sendAdminUserActivatedEmail( user );
                sendSysAdminNewAdminActivatedNotificationEmail( user );
                return ActivationState.ACTIVATED;
            }
        }
        return ActivationState.UNKNOWN;
    }


    @Override
    public ActivationState handleActivationTokenForAdminUser( UUID userId, String token ) throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken( token, TOKEN_TYPE_ACTIVATION, ADMIN_USER );
        if ( ( principal != null ) && userId.equals( principal.getUuid() ) ) {
            activateAdminUser( principal.getUuid() );
            UserInfo user = getAdminUserByUuid( principal.getUuid() );
            sendAdminUserActivatedEmail( user );
            sendSysAdminNewAdminActivatedNotificationEmail( user );
            return ActivationState.ACTIVATED;
        }
        return ActivationState.UNKNOWN;
    }


    public void sendAdminUserConfirmationEmail( UserInfo user ) throws Exception {
        String token = getConfirmationTokenForAdminUser( user.getUuid(), 0 );
        String confirmation_url =
                String.format( properties.getProperty( PROPERTIES_ADMIN_CONFIRMATION_URL ), user.getUuid().toString() )
                        + "?token=" + token;
        sendAdminUserEmail( user, "User Account Confirmation: " + user.getEmail(),
                emailMsg( hashMap( "user_email", user.getEmail() ).map( "confirmation_url", confirmation_url ),
                        PROPERTIES_EMAIL_ADMIN_CONFIRMATION ) );
    }


    public void sendSysAdminRequestAdminActivationEmail( UserInfo user ) throws Exception {
        String token = getActivationTokenForAdminUser( user.getUuid(), 0 );
        String activation_url =
                String.format( properties.getProperty( PROPERTIES_ADMIN_ACTIVATION_URL ), user.getUuid().toString() )
                        + "?token=" + token;
        sendHtmlMail( properties, properties.getProperty( PROPERTIES_SYSADMIN_EMAIL ),
                properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                "Request For Admin User Account Activation " + user.getEmail(), appendEmailFooter(
                emailMsg( hashMap( "user_email", user.getEmail() ).map( "activation_url", activation_url ),
                        PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION ) ) );
    }


    public void sendSysAdminNewAdminActivatedNotificationEmail( UserInfo user ) throws Exception {
        if ( properties.notifySysAdminOfNewAdminUsers() ) {
            sendHtmlMail( properties, properties.getProperty( PROPERTIES_SYSADMIN_EMAIL ),
                    properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                    "Admin User Account Activated " + user.getEmail(), appendEmailFooter(
                    emailMsg( hashMap( "user_email", user.getEmail() ), PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATED ) ) );
        }
    }


    public void sendAdminUserConfirmedAwaitingActivationEmail( UserInfo user ) throws Exception {
        sendAdminUserEmail(user, "User Account Confirmed",
                emailMsg( hashMap("confirmed_email",user.getEmail() ),PROPERTIES_EMAIL_ADMIN_CONFIRMED_AWAITING_ACTIVATION ) );
    }


    public void sendAdminUserActivatedEmail( UserInfo user ) throws Exception {
        if ( properties.notifyAdminOfActivation() ) {
            sendAdminUserEmail( user, "User Account Activated",
                    properties.getProperty( PROPERTIES_EMAIL_ADMIN_ACTIVATED ) );
        }
    }


    public void sendAdminUserInvitedEmail( UserInfo user, OrganizationInfo organization ) throws Exception {
        sendAdminUserEmail( user, "User Invited To Organization",
                emailMsg( hashMap( "organization_name", organization.getName() ), PROPERTIES_EMAIL_ADMIN_INVITED ) );
    }


    @Override
    public void sendAdminUserEmail( UserInfo user, String subject, String html ) throws Exception {
        sendHtmlMail( properties, user.getDisplayEmailAddress(), properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                subject, appendEmailFooter( html ) );
    }


    @Override
    public void activateOrganization( OrganizationInfo organization ) throws Exception {
        activateOrganization( organization, true );
    }


    private void activateOrganization( OrganizationInfo organization, boolean sendEmail ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( Group.ENTITY_TYPE, organization.getUuid() ), "activated", true );
        List<UserInfo> users = getAdminUsersForOrganization( organization.getUuid() );
        for ( UserInfo user : users ) {
            boolean confirmed = user.isConfirmed() || !newAdminUsersRequireConfirmation();
            boolean shouldActivate = confirmed && !newAdminUsersRequireConfirmation();
            if ( shouldActivate ) {
                activateAdminUser( user.getUuid() );
            }
        }
        if ( sendEmail ) {
            startOrganizationActivationFlow( organization );
        }

    }


    @Override
    public void deactivateOrganization( UUID organizationId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "activated", false );
    }


    @Override
    public boolean isOrganizationActivated( UUID organizationId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        return Boolean.TRUE.equals(
                em.getProperty( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "activated" ) );
    }


    @Override
    public void enableOrganization( UUID organizationId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "disabled", false );
    }


    @Override
    public void disableOrganization( UUID organizationId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.setProperty( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "disabled", true );
    }


    @Override
    public boolean isOrganizationEnabled( UUID organizationId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        return !Boolean.TRUE.equals(
                em.getProperty( new SimpleEntityRef( Group.ENTITY_TYPE, organizationId ), "disabled" ) );
    }


    @Override
    public boolean checkPasswordResetTokenForAppUser( UUID applicationId, UUID userId, String token ) throws Exception {
        AuthPrincipalInfo principal = null;
        try {
            principal = getPrincipalFromAccessToken( token, TOKEN_TYPE_PASSWORD_RESET, APPLICATION_USER );
        }
        catch ( Exception e ) {
            logger.error( "Unable to verify token", e );
        }
        return ( principal != null ) && userId.equals( principal.getUuid() );
    }


    @Override
    public String getAccessTokenForAppUser( UUID applicationId, UUID userId, long duration ) throws Exception {
        return getTokenForPrincipal( ACCESS, null, applicationId, APPLICATION_USER, userId, duration );
    }


    /*
   * (non-Javadoc)
   *
   * @see
   * org.apache.usergrid.management.ManagementService#revokeAccessTokensForAappUser
   * (java.util.UUID, java.util.UUID)
   */
    @Override
    public void revokeAccessTokensForAppUser( UUID applicationId, UUID userId ) throws Exception {
        revokeTokensForPrincipal( APPLICATION_USER, applicationId, userId );
    }


    @Override
    public void revokeAccessTokenForAppUser( String token ) throws Exception {
        if ( anyNull( token ) ) {
            throw new IllegalArgumentException( "token is required" );
        }

        UserInfo userInfo = getAppUserFromAccessToken( token );
        if ( userInfo == null ) {
            throw new TokenException( "Could not match token : " + token );
        }

        tokens.revokeToken( token );
    }


    @Override
    public UserInfo getAppUserFromAccessToken( String token ) throws Exception {
        AuthPrincipalInfo auth_principal = getPrincipalFromAccessToken( token, null, APPLICATION_USER );
        if ( auth_principal == null ) {
            return null;
        }
        UUID appId = auth_principal.getApplicationId();
        if ( appId != null ) {
            Entity user = getAppUserByIdentifier( appId, Identifier.fromUUID( auth_principal.getUuid() ) );
            if ( user != null ) {
                return new UserInfo( appId, user.getProperties() );
            }
        }
        return null;
    }


    @Override
    public User getAppUserByIdentifier( UUID applicationId, Identifier identifier ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        return em.get( em.getUserByIdentifier( identifier ), User.class );
    }


    @Override
    public void startAppUserPasswordResetFlow( UUID applicationId, User user ) throws Exception {
        String token = getPasswordResetTokenForAppUser( applicationId, user.getUuid() );
        String reset_url =
                buildUserAppUrl( applicationId, properties.getProperty( PROPERTIES_USER_RESETPW_URL ), user, token );
        Map<String, String> pageContext = hashMap( "reset_url", reset_url )
                .map( "reset_url_base", properties.getProperty( PROPERTIES_ADMIN_RESETPW_URL ) )
                .map( "user_uuid", user.getUuid().toString() ).map( "raw_token", token )
                .map( "application_id", applicationId.toString() );
    /*
     * String reset_url = String.format(
     * properties.getProperty(PROPERTIES_USER_RESETPW_URL), oi.getName(),
     * ai.getName(), user.getUuid().toString()) + "?token=" + token;
     */
        sendHtmlMail( properties, user.getDisplayEmailAddress(), properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                "Password Reset", appendEmailFooter( emailMsg( pageContext, PROPERTIES_EMAIL_USER_PASSWORD_RESET ) ) );
    }


    @Override
    public boolean newAppUsersNeedAdminApproval( UUID applicationId ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        Boolean registration_requires_admin_approval = ( Boolean ) em
                .getProperty( new SimpleEntityRef( Application.ENTITY_TYPE, applicationId ),
                        REGISTRATION_REQUIRES_ADMIN_APPROVAL );
        return registration_requires_admin_approval != null && registration_requires_admin_approval.booleanValue();
    }


    @Override
    public boolean newAppUsersRequireConfirmation( UUID applicationId ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        Boolean registration_requires_email_confirmation = ( Boolean ) em
                .getProperty( new SimpleEntityRef( Application.ENTITY_TYPE, applicationId ),
                        REGISTRATION_REQUIRES_EMAIL_CONFIRMATION );
        return registration_requires_email_confirmation != null && registration_requires_email_confirmation.booleanValue();
    }


    public boolean notifyAdminOfNewAppUsers( UUID applicationId ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        Boolean notify_admin_of_new_users = ( Boolean ) em
                .getProperty( new SimpleEntityRef( Application.ENTITY_TYPE, applicationId ),
                        NOTIFY_ADMIN_OF_NEW_USERS );
        return notify_admin_of_new_users != null && notify_admin_of_new_users.booleanValue();
    }


    @Override
    public void startAppUserActivationFlow( UUID applicationId, User user ) throws Exception {
        if ( newAppUsersRequireConfirmation( applicationId ) ) {
            sendAppUserConfirmationEmail( applicationId, user );
        }
        else if ( newAppUsersNeedAdminApproval( applicationId ) ) {
            sendAdminRequestAppUserActivationEmail( applicationId, user );
        }
        else {
            sendAppUserActivatedEmail( applicationId, user );
            sendAdminNewAppUserActivatedNotificationEmail( applicationId, user );
        }
    }


    @Override
    public ActivationState handleConfirmationTokenForAppUser( UUID applicationId, UUID userId, String token )
            throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken( token, TOKEN_TYPE_CONFIRM, APPLICATION_USER );

        if ( ( principal != null ) && userId.equals( principal.getUuid() ) ) {
            EntityManager em = emf.getEntityManager( applicationId );
            User user = em.get( userId, User.class );
            confirmAppUser( applicationId, user.getUuid() );

            if ( newAppUsersNeedAdminApproval( applicationId ) ) {
                sendAppUserConfirmedAwaitingActivationEmail( applicationId, user );
                sendAdminRequestAppUserActivationEmail( applicationId, user );
                return ActivationState.CONFIRMED_AWAITING_ACTIVATION;
            }
            else {
                activateAppUser( applicationId, principal.getUuid() );
                sendAppUserActivatedEmail( applicationId, user );
                sendAdminNewAppUserActivatedNotificationEmail( applicationId, user );
                return ActivationState.ACTIVATED;
            }
        }

        return ActivationState.UNKNOWN;
    }


    @Override
    public ActivationState handleActivationTokenForAppUser( UUID applicationId, UUID userId, String token )
            throws Exception {
        AuthPrincipalInfo principal = getPrincipalFromAccessToken( token, TOKEN_TYPE_ACTIVATION, APPLICATION_USER );
        if ( ( principal != null ) && userId.equals( principal.getUuid() ) ) {
            activateAppUser( applicationId, principal.getUuid() );
            EntityManager em = emf.getEntityManager( applicationId );
            User user = em.get( userId, User.class );
            sendAppUserActivatedEmail( applicationId, user );
            sendAdminNewAppUserActivatedNotificationEmail( applicationId, user );
            return ActivationState.ACTIVATED;
        }
        return ActivationState.UNKNOWN;
    }


    public void sendAppUserConfirmationEmail( UUID applicationId, User user ) throws Exception {
        String token = getConfirmationTokenForAppUser( applicationId, user.getUuid() );
        String confirmation_url =
                buildUserAppUrl( applicationId, properties.getProperty( PROPERTIES_USER_CONFIRMATION_URL ), user,
                        token );

    /*
     * String confirmation_url = String.format(
     * properties.getProperty(PROPERTIES_USER_CONFIRMATION_URL),
     * applicationId.toString(), user.getUuid().toString()) + "?token=" + token;
     */
        sendAppUserEmail( user, "User Account Confirmation: " + user.getEmail(),
                emailMsg( hashMap( "confirmation_url", confirmation_url ), PROPERTIES_EMAIL_USER_CONFIRMATION ) );
    }


    public String buildUserAppUrl( UUID applicationId, String url, User user, String token ) throws Exception {
        ApplicationInfo ai = getApplicationInfo( applicationId );
        OrganizationInfo oi = getOrganizationForApplication( applicationId );
        return String.format( url, oi.getName(), StringUtils.stringOrSubstringAfterFirst( ai.getName(), '/' ),
                user.getUuid().toString() ) + "?token=" + token;
    }


    public void sendAdminRequestAppUserActivationEmail( UUID applicationId, User user ) throws Exception {
        String token = getActivationTokenForAppUser( applicationId, user.getUuid() );
        String activation_url =
                buildUserAppUrl( applicationId, properties.getProperty( PROPERTIES_USER_ACTIVATION_URL ), user, token );
    /*
     * String activation_url = String.format(
     * properties.getProperty(PROPERTIES_USER_ACTIVATION_URL),
     * applicationId.toString(), user.getUuid().toString()) + "?token=" + token;
     */
        OrganizationInfo organization = this.getOrganizationForApplication( applicationId );
        this.sendOrganizationEmail( organization, "Request For User Account Activation " + user.getEmail(), emailMsg(
                hashMap( "organization_name", organization.getName() ).map( "activation_url", activation_url ),
                PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION ) );
    }


    public void sendAdminNewAppUserActivatedNotificationEmail( UUID applicationId, User user ) throws Exception {
        if ( notifyAdminOfNewAppUsers( applicationId ) ) {
            OrganizationInfo organization = this.getOrganizationForApplication( applicationId );
            this.sendOrganizationEmail( organization, "New User Account Activated " + user.getEmail(),
                    emailMsg( hashMap( "organization_name", organization.getName() ),
                            PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION ) );
        }
    }


    public void sendAppUserConfirmedAwaitingActivationEmail( UUID applicationId, User user ) throws Exception {
        sendAppUserEmail( user, "User Account Confirmed",
                properties.getProperty( PROPERTIES_EMAIL_USER_CONFIRMED_AWAITING_ACTIVATION ) );
    }


    public void sendAppUserActivatedEmail( UUID applicationId, User user ) throws Exception {
        sendAppUserEmail( user, "User Account Activated", properties.getProperty( PROPERTIES_EMAIL_USER_ACTIVATED ) );
    }


    @Override
    public void activateAppUser( UUID applicationId, UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        em.setProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "activated", true );
    }


    public void confirmAppUser( UUID applicationId, UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        em.setProperty( new SimpleEntityRef( User.ENTITY_TYPE, userId ), "confirmed", true );
    }


    @Override
    public void setAppUserPassword( UUID applicationId, UUID userId, String newPassword ) throws Exception {
        if ( ( userId == null ) || ( newPassword == null ) ) {
            return;
        }

        EntityManager em = emf.getEntityManager( applicationId );
        User user = em.get( userId, User.class );

        writeUserPassword( applicationId, user,
                encryptionService.defaultEncryptedCredentials( newPassword, user.getUuid(), applicationId ) );
    }


    @Override
    public void setAppUserPassword( UUID applicationId, UUID userId, String oldPassword, String newPassword )
            throws Exception {
        if ( ( userId == null ) ) {
            throw new IllegalArgumentException( "userId is required" );
        }
        if ( ( oldPassword == null ) || ( newPassword == null ) ) {
            throw new IllegalArgumentException( "oldpassword and newpassword are both required" );
        }
        // TODO load the user, send the hashType down to maybeSaltPassword
        User user = emf.getEntityManager( applicationId ).get( userId, User.class );
        if ( !verify( applicationId, user.getUuid(), oldPassword ) ) {
            throw new IncorrectPasswordException( "Old password does not match" );
        }

        setAppUserPassword( applicationId, userId, newPassword );
    }


    @Override
    public User verifyAppUserPasswordCredentials( UUID applicationId, String name, String password ) throws Exception {

        User user = findUserEntity( applicationId, name );
        if ( user == null ) {
            return null;
        }

        if ( verify( applicationId, user.getUuid(), password ) ) {
            if ( !user.activated() ) {
                throw new UnactivatedAppUserException();
            }
            if ( user.disabled() ) {
                throw new DisabledAppUserException();
            }
            return user;
        }

        return null;
    }


    public String getPasswordResetTokenForAppUser( UUID applicationId, UUID userId ) throws Exception {
        return getTokenForPrincipal( EMAIL, TOKEN_TYPE_PASSWORD_RESET, applicationId, APPLICATION_USER, userId, 0 );
    }


    public void sendAppUserEmail( User user, String subject, String html ) throws Exception {
        sendHtmlMail( properties, user.getDisplayEmailAddress(), properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                subject, appendEmailFooter( html ) );
    }


    public String getActivationTokenForAppUser( UUID applicationId, UUID userId ) throws Exception {
        return getTokenForPrincipal( EMAIL, TOKEN_TYPE_ACTIVATION, applicationId, APPLICATION_USER, userId, 0 );
    }


    public String getConfirmationTokenForAppUser( UUID applicationId, UUID userId ) throws Exception {
        return getTokenForPrincipal( EMAIL, TOKEN_TYPE_CONFIRM, applicationId, APPLICATION_USER, userId, 0 );
    }


    @Override
    public void setAppUserPin( UUID applicationId, UUID userId, String newPin ) throws Exception {
        if ( ( userId == null ) || ( newPin == null ) ) {
            return;
        }

        writeUserPin( applicationId, new SimpleEntityRef( User.ENTITY_TYPE, userId ),
                encryptionService.plainTextCredentials( newPin, userId, applicationId ) );
    }


    @Override
    public void sendAppUserPin( UUID applicationId, UUID userId ) throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        User user = em.get( userId, User.class );
        if ( user == null ) {
            return;
        }
        if ( user.getEmail() == null ) {
            return;
        }
        String pin = getCredentialsSecret( readUserPin( applicationId, userId, user.getType() ) );

        sendHtmlMail( properties, user.getDisplayEmailAddress(), properties.getProperty( PROPERTIES_MAILER_EMAIL ),
                "Your app pin",
                appendEmailFooter( emailMsg( hashMap( USER_PIN, pin ), PROPERTIES_EMAIL_USER_PIN_REQUEST ) ) );
    }


    @Override
    public User verifyAppUserPinCredentials( UUID applicationId, String name, String pin ) throws Exception {

        User user = findUserEntity( applicationId, name );
        if ( user == null ) {
            return null;
        }
        if ( pin.equals( getCredentialsSecret( readUserPin( applicationId, user.getUuid(), user.getType() ) ) ) ) {
            return user;
        }
        return null;
    }


    @Override
    public void countAdminUserAction( UserInfo user, String action ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );
        em.incrementAggregateCounters( user.getUuid(), null, null, "admin_logins", 1 );
    }


    /*
   * (non-Javadoc)
   *
   * @see
   * org.apache.usergrid.management.ManagementService#setOrganizationProps(java.util
   * .UUID, java.util.Map)
   */
    @Override
    public void setOrganizationProps( UUID orgId, Map<String, Object> props ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

        Group org = em.get( orgId, Group.class );

        if ( org == null ) {
            throw new EntityNotFoundException( String.format( "Could not find organization with id {}", orgId ) );
        }

        org.setProperties( props );

        em.update( org );
    }


    /*
   * (non-Javadoc)
   *
   * @see
   * org.apache.usergrid.management.ManagementService#getOrganizationProps(java.util
   * .UUID)
   */
    @Override
    public Group getOrganizationProps( UUID orgId ) throws Exception {
        EntityManager em = emf.getEntityManager( smf.getManagementAppId() );

        return em.get( orgId, Group.class );
    }


    /** Persist the user's password credentials info */
    protected void writeUserPassword( UUID appId, EntityRef owner, CredentialsInfo creds ) throws Exception {
        writeCreds( appId, owner, creds, USER_PASSWORD );
    }


    /** read the user password credential's info */
    protected CredentialsInfo readUserPasswordCredentials( UUID appId, UUID ownerId, String ownerType ) throws Exception {
        return readCreds( appId, ownerId, ownerType, USER_PASSWORD );
    }


    /** Write the user's token */
    protected void writeUserToken( UUID appId, EntityRef owner, CredentialsInfo token ) throws Exception {
        writeCreds( appId, owner, token, USER_TOKEN );
    }


    /** Read the credentials info for the user's token */
    protected CredentialsInfo readUserToken( UUID appId, UUID ownerId, String ownerType ) throws Exception {
        return readCreds( appId, ownerId, ownerType, USER_TOKEN );
    }


    /** Write the mongo password */
    protected void writeUserMongoPassword( UUID appId, EntityRef owner, CredentialsInfo password ) throws Exception {
        writeCreds( appId, owner, password, USER_MONGO_PASSWORD );
    }


    /** Read the mongo password */
    protected CredentialsInfo readUserMongoPassword( UUID appId, UUID ownerId, String ownerType ) throws Exception {
        return readCreds( appId, ownerId, ownerType, USER_MONGO_PASSWORD );
    }


    /** Write the user's pin */
    protected void writeUserPin( UUID appId, EntityRef owner, CredentialsInfo pin ) throws Exception {
        writeCreds( appId, owner, pin, USER_PIN );
    }


    /** Read the user's pin */
    protected CredentialsInfo readUserPin( UUID appId, UUID ownerId, String ownerType ) throws Exception {
        return readCreds( appId, ownerId, ownerType, USER_PIN );
    }


    private void writeCreds( UUID appId, EntityRef owner, CredentialsInfo creds, String key ) throws Exception {
        EntityManager em = emf.getEntityManager( appId );
        em.addToDictionary( owner, DICTIONARY_CREDENTIALS, key, creds );
    }


    private CredentialsInfo readCreds( UUID appId, UUID ownerId, String ownerType, String key ) throws Exception {
        EntityManager em = emf.getEntityManager( appId );
        Entity owner = em.get( ownerId );
        return ( CredentialsInfo ) em.getDictionaryElementValue( owner, DICTIONARY_CREDENTIALS, key );
    }


    private Set<CredentialsInfo> readUserPasswordHistory( UUID appId, UUID ownerId ) throws Exception {
        EntityManager em = emf.getEntityManager( appId );
        Entity owner = em.get( new SimpleEntityRef("user", ownerId ));
        return ( Set<CredentialsInfo> ) em
                .getDictionaryElementValue( owner, DICTIONARY_CREDENTIALS, USER_PASSWORD_HISTORY );
    }


    @Override
    public boolean newAdminUsersNeedSysAdminApproval() {
        return properties.newAdminUsersNeedSysAdminApproval();
    }


    @Override
    public boolean newAdminUsersRequireConfirmation() {
        return properties.newAdminUsersRequireConfirmation();
    }


    @Override
    public boolean newOrganizationsNeedSysAdminApproval() {
        return properties.newOrganizationsNeedSysAdminApproval();
    }


    private boolean areActivationChecksDisabled() {
        return !( newOrganizationsNeedSysAdminApproval() || properties.newOrganizationsRequireConfirmation()
                || newAdminUsersNeedSysAdminApproval() || newAdminUsersRequireConfirmation() );
    }


    private void sendHtmlMail( AccountCreationProps props, String to, String from, String subject, String html ) {
        mailUtils.sendHtmlMail( props.getMailProperties(), to, from, subject, html );
    }


    public AccountCreationProps getAccountCreationProps() {
        return properties;
    }


    private boolean verify( UUID applicationId, UUID userId, String password ) throws Exception {
        CredentialsInfo ci = readUserPasswordCredentials( applicationId, userId, User.ENTITY_TYPE );

        if ( ci == null ) {
            return false;
        }

        return encryptionService.verify( password, ci, userId, applicationId );
    }


    /** @return the saltProvider */
    public SaltProvider getSaltProvider() {
        return saltProvider;
    }


    /** @param saltProvider the saltProvider to set */
    public void setSaltProvider( SaltProvider saltProvider ) {
        this.saltProvider = saltProvider;
    }


    @Override
    public Object registerAppWithAPM( OrganizationInfo orgInfo, ApplicationInfo appInfo ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    private String getProperty(String key) {
        String obj = properties.getProperty(key);
        if(StringUtils.isEmpty(obj))
            return null;
        else
            return obj;
    }

    private boolean getBooleanProperty(String key) {
        String obj = getProperty(key);
        if(StringUtils.isEmpty(obj))
            return false;
        else
            return Boolean.parseBoolean(obj);
    }

}
