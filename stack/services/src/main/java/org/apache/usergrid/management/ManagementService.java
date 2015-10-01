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
package org.apache.usergrid.management;


import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.CredentialsInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.security.shiro.PrincipalCredentialsToken;
import org.apache.usergrid.services.ServiceResults;

import com.google.common.collect.BiMap;
import rx.Observable;


public interface ManagementService {

    public void activateAdminUser( UUID userId ) throws Exception;

    public void activateOrganization( OrganizationInfo organization ) throws Exception;

    public void addAdminUserToOrganization( UserInfo user, OrganizationInfo organization, boolean email )
            throws Exception;

    public AccessInfo authorizeClient( String clientId, String clientSecret, long ttl ) throws Exception;

    public ActivationState handleConfirmationTokenForAdminUser( UUID userId, String token ) throws Exception;

    public ActivationState handleActivationTokenForAdminUser( UUID userId, String token ) throws Exception;

    public ActivationState handleActivationTokenForOrganization( UUID organizationId, String token ) throws Exception;

    public boolean checkPasswordResetTokenForAdminUser( UUID userId, String token ) throws Exception;

    public UserInfo createAdminUser( String username, String name, String email, String password, boolean activated,
                                     boolean disabled ) throws Exception;

    public UserInfo createAdminUser( String username, String name, String email, String password, boolean activated,
                                     boolean disabled, Map<String, Object> userProperties ) throws Exception;

    public UserInfo createAdminFrom( User user, String password ) throws Exception;

    public UserInfo createAdminFromPrexistingPassword( User user, CredentialsInfo ci ) throws Exception;

    public ApplicationInfo createApplication( UUID organizationId, String applicationName ) throws Exception;
    public ApplicationInfo createApplication( UUID organizationId, String applicationName,
                                              Map<String, Object> properties ) throws Exception;
    public ApplicationInfo createApplication( UUID organizationId, String applicationName, UUID applicationId,
                                              Map<String, Object> properties ) throws Exception;

    public OrganizationInfo createOrganization(String organizationName, UserInfo user, boolean activated)
            throws Exception;

    public OrganizationInfo createOrganization(UUID orgUuid, String organizationName, UserInfo user, boolean activated)
            throws Exception;

    public OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
                                                             String email, String password ) throws Exception;

    public OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
                                                             String email, String password, boolean activated,
                                                             boolean disabled ) throws Exception;

    public OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
                                                             String email, String password, boolean activated,
                                                             boolean disabled, Map<String, Object> userProperties,
                                                             Map<String, Object> properties ) throws Exception;

    public void updateOrganization( OrganizationInfo organizationInfo ) throws Exception;

    /** Deactivate the user and return it's current state */
    public User deactivateUser( UUID applicationId, UUID userId ) throws Exception;

    public void deactivateOrganization( UUID organizationId ) throws Exception;

    public UUID addApplicationToOrganization(UUID organizationId, Entity appInfo) throws Exception;

    public void deleteOrganizationApplication( UUID organizationId, UUID applicationId ) throws Exception;

    public void disableAdminUser( UUID userId ) throws Exception;

    public void disableOrganization( UUID organizationId ) throws Exception;

    public void enableAdminUser( UUID userId ) throws Exception;

    public void enableOrganization( UUID organizationId ) throws Exception;

    public UserInfo findAdminUser( String identifier );

    public String getAccessTokenForAdminUser( UUID userId, long duration ) throws Exception;

   /** Revoke all active access tokens for this admin user */
    public void revokeAccessTokensForAdminUser( UUID userId ) throws Exception;

    public void revokeAccessTokenForAdminUser( UUID userId, String token ) throws Exception;

    public String getActivationTokenForAdminUser( UUID userId, long ttl ) throws Exception;

    public String getConfirmationTokenForAdminUser( UUID userId, long ttl ) throws Exception;

    public String getActivationTokenForOrganization( UUID organizationId, long ttl ) throws Exception;

    /** Import an Admin User token generated by some other system */
    public void importTokenForAdminUser( UUID userId, String token, long ttl ) throws Exception;

    public ServiceResults getAdminUserActivities( UserInfo user ) throws Exception;

    public ServiceResults getAdminUserActivity( UserInfo user ) throws Exception;

    public UserInfo getAdminUserByEmail( String email ) throws Exception;

    public UserInfo getAdminUserByIdentifier( Identifier id ) throws Exception;

    public UserInfo getAdminUserByUsername( String username ) throws Exception;

    public Entity getAdminUserEntityByIdentifier( Identifier id ) throws Exception;

    public Entity getAdminUserEntityByUuid( UUID id ) throws Exception;

    public Entity getAdminUserEntityFromAccessToken( String token ) throws Exception;

    public UserInfo getAdminUserInfoFromAccessToken( String token ) throws Exception;

    public Map<String, Object> getAdminUserOrganizationData( UserInfo user, boolean deep ) throws Exception;

    public Map<String, Object> getAdminUserOrganizationData( UUID userId ) throws Exception;

    public List<UserInfo> getAdminUsersForOrganization( UUID organizationId ) throws Exception;

    public ApplicationInfo getApplicationInfo( String applicationName ) throws Exception;

    public ApplicationInfo getApplicationInfo( UUID applicationId ) throws Exception;

    ApplicationInfo getDeletedApplicationInfo(UUID applicationId) throws Exception;

    public ApplicationInfo getApplicationInfo( Identifier id ) throws Exception;

    public ApplicationInfo getApplicationInfoFromAccessToken( String token ) throws Exception;

    public ServiceResults getApplicationMetadata( UUID applicationId ) throws Exception;

    public BiMap<UUID, String> getApplicationsForOrganization( UUID organizationId ) throws Exception;

    public BiMap<UUID, String> getApplicationsForOrganizations( Set<UUID> organizationIds ) throws Exception;

    public String getClientIdForApplication( UUID applicationId );

    public String getClientIdForOrganization( UUID organizationId );

    public String getClientSecretForApplication( UUID applicationId ) throws Exception;

    public String getClientSecretForOrganization( UUID organizationId ) throws Exception;

    public ServiceResults getOrganizationActivity( OrganizationInfo organization ) throws Exception;

    public ServiceResults getOrganizationActivityForAdminUser( OrganizationInfo organization, UserInfo user )
            throws Exception;

    public OrganizationInfo getOrganizationByIdentifier( Identifier id ) throws Exception;

    public OrganizationInfo getOrganizationByName( String organizationName ) throws Exception;

    public OrganizationInfo getOrganizationByUuid( UUID id ) throws Exception;

    public Map<String, Object> getOrganizationData( OrganizationInfo organization ) throws Exception;

    public OrganizationInfo getOrganizationForApplication( UUID applicationId ) throws Exception;

    public OrganizationInfo getOrganizationInfoFromAccessToken( String token ) throws Exception;

    public BiMap<UUID, String> getOrganizations() throws Exception;

    public BiMap<UUID, String> getOrganizationsForAdminUser( UUID userId ) throws Exception;

    public String getPasswordResetTokenForAdminUser( UUID userId, long ttl ) throws Exception;

    public UserInfo getAdminUserByUuid( UUID id ) throws Exception;

    public UUID importApplication( UUID organizationId, Application application ) throws Exception;

    public OrganizationInfo importOrganization( UUID organizationId, OrganizationInfo organizationInfo,
                                                Map<String, Object> properties ) throws Exception;

    public boolean isAdminUserActivated( UUID userId ) throws Exception;

    public boolean isAdminUserEnabled( UUID userId ) throws Exception;

    public boolean isOrganizationActivated( UUID organizationId ) throws Exception;

    public boolean isOrganizationEnabled( UUID organizationId ) throws Exception;

    public boolean newAdminUsersNeedSysAdminApproval();

    public boolean newAdminUsersRequireConfirmation();

    public String newClientSecretForApplication( UUID applicationId ) throws Exception;

    public String newClientSecretForOrganization( UUID organizationId ) throws Exception;

    public boolean newOrganizationsNeedSysAdminApproval();

    public void postOrganizationActivity( UUID organizationId, UserInfo user, String verb, EntityRef object,
                                          String objectType, String objectName, String title, String content )
            throws Exception;

    public void removeAdminUserFromOrganization( UUID userId, UUID organizationId ) throws Exception;

    public void removeOrganizationApplication( UUID organizationId, UUID applicationId ) throws Exception;

    public void startAdminUserActivationFlow( UserInfo user ) throws Exception;

    public void sendAdminUserEmail( UserInfo user, String subject, String html ) throws Exception;

    public void startAdminUserPasswordResetFlow( UserInfo user ) throws Exception;

    public void startOrganizationActivationFlow( OrganizationInfo organization ) throws Exception;

    public void sendOrganizationEmail( OrganizationInfo organization, String subject, String html ) throws Exception;

    public void setAdminUserPassword( UUID userId, String newPassword ) throws Exception;

    public void setAdminUserPassword( UUID userId, String oldPassword, String newPassword ) throws Exception;

    public void setup() throws Exception;

    public UserInfo updateAdminUser( UserInfo user, String username, String name, String email,
                                     Map<String, Object> json ) throws Exception;

    public boolean verifyAdminUserPassword( UUID userId, String password ) throws Exception;

    public UserInfo verifyAdminUserPasswordCredentials( String name, String password ) throws Exception;

    public UserInfo verifyMongoCredentials( String name, String nonce, String key ) throws Exception;

    public void activateAppUser( UUID applicationId, UUID userId ) throws Exception;

    public ActivationState handleActivationTokenForAppUser( UUID applicationId, UUID userId, String token )
            throws Exception;

    public ActivationState handleConfirmationTokenForAppUser( UUID applicationId, UUID userId, String token )
            throws Exception;

    public boolean checkPasswordResetTokenForAppUser( UUID applicationId, UUID userId, String token ) throws Exception;

    public String getAccessTokenForAppUser( UUID applicationId, UUID userId, long duration ) throws Exception;

    public Long getLastAdminPasswordChange( UUID userId ) throws Exception;

    /** Revoke all active access tokens for this admin user */
    public void revokeAccessTokensForAppUser( UUID applicationId, UUID userId ) throws Exception;

    public void revokeAccessTokenForAppUser( String token ) throws Exception;

    public User getAppUserByIdentifier( UUID applicationId, Identifier identifier ) throws Exception;

    public void startAppUserPasswordResetFlow( UUID applicationId, User user ) throws Exception;

    public void startAppUserActivationFlow( UUID applicationId, User user ) throws Exception;

    public void setAppUserPassword( UUID applicationId, UUID userId, String newPassword ) throws Exception;

    public void setAppUserPassword( UUID applicationId, UUID userId, String oldPassword, String newPassword )
            throws Exception;

    public User verifyAppUserPasswordCredentials( UUID applicationId, String name, String password ) throws Exception;

    public UserInfo getAppUserFromAccessToken( String token ) throws Exception;

    public void setAppUserPin( UUID applicationId, UUID userId, String newPin ) throws Exception;

    public void sendAppUserPin( UUID applicationId, UUID userId ) throws Exception;

    public User verifyAppUserPinCredentials( UUID applicationId, String name, String pin ) throws Exception;

    public PrincipalCredentialsToken getPrincipalCredentialsTokenForClientCredentials( String clientId,
                                                                                       String clientSecret )
            throws Exception;

    public void confirmAdminUser( UUID userId ) throws Exception;

    public void unconfirmAdminUser( UUID userId ) throws Exception;

    public boolean isAdminUserConfirmed( UUID userId ) throws Exception;

    public void countAdminUserAction( UserInfo user, String action ) throws Exception;

    public boolean newAppUsersNeedAdminApproval( UUID applicationId ) throws Exception;

    public boolean newAppUsersRequireConfirmation( UUID applicationId ) throws Exception;

    public abstract void provisionSuperuser() throws Exception;

    public void resetSuperUser(String username, String password, String email) throws Exception;

    public List<OrganizationInfo> getOrganizations( UUID startResult, int count ) throws Exception;

    /** Add the properties to the organization */
    public void setOrganizationProps( UUID orgId, Map<String, Object> props ) throws Exception;

    /** Get the organization properties, returns them in the group object */
    public Group getOrganizationProps( UUID orgId ) throws Exception;

    public Object registerAppWithAPM( OrganizationInfo orgInfo, ApplicationInfo appInfo ) throws Exception;

    /** For testing purposes only */
    public Properties getProperties();

    public void deleteApplication(UUID applicationId) throws Exception;

    public ApplicationInfo restoreApplication(UUID applicationId) throws Exception;

    long getApplicationSize(final UUID applicationId);

    long getCollectionSize(final UUID applicationId, final String collectionName);

    Map<String,Long> getEachCollectionSize(final UUID applicationId);

    public OrganizationConfig getOrganizationConfigByName( String organizationName ) throws Exception;

    public OrganizationConfig getOrganizationConfigByUuid( UUID id ) throws Exception;

    public Map<String, Object> getOrganizationConfigData( OrganizationConfig organizationConfig ) throws Exception;

    public OrganizationConfig getOrganizationConfigForApplication( UUID applicationId ) throws Exception;

    public void updateOrganizationConfig( OrganizationConfig organizationConfig ) throws Exception;
    
    /**
     * will delete all entities
     * @param applicationId
     * @return
     */
    Observable<Id> deleteAllEntities(final UUID applicationId,final int limit);
}
