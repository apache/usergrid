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
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.services.ServiceResults;

import com.google.common.collect.BiMap;
import rx.Observable;


public interface ManagementService {

	void activateAdminUser( UUID userId ) throws Exception;

	void activateOrganization( OrganizationInfo organization ) throws Exception;

	void addAdminUserToOrganization( UserInfo user, OrganizationInfo organization, boolean email )
			throws Exception;

	AccessInfo authorizeClient( String clientId, String clientSecret, long ttl ) throws Exception;

	TokenInfo getConfirmationTokenInfoForAdminUser( String token ) throws Exception;

	ActivationState handleConfirmationTokenForAdminUser( UUID userId, String token ) throws Exception;

	ActivationState handleConfirmationTokenForAdminUser( UUID userId, TokenInfo tokenInfo ) throws Exception;

	TokenInfo getActivationTokenInfoForAdminUser( String token ) throws Exception;

	ActivationState handleActivationTokenForAdminUser( UUID userId, String token ) throws Exception;

	ActivationState handleActivationTokenForAdminUser( UUID userId, TokenInfo tokenInfo ) throws Exception;

	ActivationState handleActivationTokenForOrganization( UUID organizationId, String token ) throws Exception;

	TokenInfo getPasswordResetTokenInfoForAdminUser( String token ) throws Exception;

	boolean checkPasswordResetTokenForAdminUser( UUID userId, TokenInfo tokenInfo ) throws Exception;

	boolean checkPasswordResetTokenForAdminUser( UUID userId, String token ) throws Exception;

	UserInfo createAdminUser( UUID organizationId, String username, String name, String email, String password,
									 boolean activated, boolean disabled ) throws Exception;

	UserInfo createAdminUser( UUID organizationId, String username, String name, String email, String password,
									 boolean activated, boolean disabled, Map<String, Object> userProperties ) throws Exception;

	UserInfo createAdminFrom( UUID organizationId, User user, String password ) throws Exception;

	UserInfo createAdminFromPrexistingPassword( UUID organizationId, User user, CredentialsInfo ci ) throws Exception;

	ApplicationInfo createApplication( UUID organizationId, String applicationName ) throws Exception;
	ApplicationInfo createApplication( UUID organizationId, String applicationName,
											  Map<String, Object> properties ) throws Exception;
	ApplicationInfo createApplication(UUID organizationId, String applicationName, UUID applicationId,
											 Map<String, Object> properties, boolean forMigration) throws Exception;

	OrganizationInfo createOrganization(String organizationName, UserInfo user, boolean activated)
			throws Exception;

	OrganizationInfo createOrganization(UUID orgUuid, String organizationName, UserInfo user, boolean activated)
			throws Exception;

	OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
															 String email, String password ) throws Exception;

	OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
															 String email, String password, boolean activated,
															 boolean disabled ) throws Exception;

	OrganizationOwnerInfo createOwnerAndOrganization( String organizationName, String username, String name,
															 String email, String password, boolean activated,
															 boolean disabled, Map<String, Object> userProperties,
															 Map<String, Object> properties ) throws Exception;

	void updateOrganization( OrganizationInfo organizationInfo ) throws Exception;

	/** Deactivate the user and return it's current state */
	User deactivateUser( UUID applicationId, UUID userId ) throws Exception;

	void deactivateOrganization( UUID organizationId ) throws Exception;

	UUID addApplicationToOrganization(UUID organizationId, Entity appInfo) throws Exception;

	void deleteOrganizationApplication( UUID organizationId, UUID applicationId ) throws Exception;

	void disableAdminUser( UUID userId ) throws Exception;

	void disableOrganization( UUID organizationId ) throws Exception;

	void enableAdminUser( UUID userId ) throws Exception;

	void enableOrganization( UUID organizationId ) throws Exception;

	UserInfo findAdminUser( String identifier );

	String getAccessTokenForAdminUser( UUID userId, long duration ) throws Exception;

   /** Revoke all active access tokens for this admin user */
	void revokeAccessTokensForAdminUser( UUID userId ) throws Exception;

	void revokeAccessTokenForAdminUser( UUID userId, String token ) throws Exception;

	String getActivationTokenForAdminUser( UUID userId, long ttl, UUID organizationId ) throws Exception;

	String getConfirmationTokenForAdminUser( UUID userId, long ttl, UUID organizationId ) throws Exception;

	String getActivationTokenForOrganization( UUID organizationId, long ttl ) throws Exception;

	ServiceResults getAdminUserActivities( UserInfo user ) throws Exception;

	ServiceResults getAdminUserActivity( UserInfo user ) throws Exception;

	UserInfo getAdminUserByEmail( String email ) throws Exception;

	UserInfo getAdminUserByIdentifier( Identifier id ) throws Exception;

	UserInfo getAdminUserByUsername( String username ) throws Exception;

	Entity getAdminUserEntityByIdentifier( Identifier id ) throws Exception;

	Entity getAdminUserEntityByUuid( UUID id ) throws Exception;

	Entity getAdminUserEntityFromAccessToken( String token ) throws Exception;

	UserInfo getAdminUserInfoFromAccessToken( String token ) throws Exception;

	Map<String, Object> getAdminUserOrganizationData( UserInfo user, boolean deep ) throws Exception;

	Map<String, Object> getAdminUserOrganizationData( UUID userId ) throws Exception;

	List<UserInfo> getAdminUsersForOrganization( UUID organizationId ) throws Exception;

	ApplicationInfo getApplicationInfo( String applicationName ) throws Exception;

	ApplicationInfo getApplicationInfo( UUID applicationId ) throws Exception;

	ApplicationInfo getDeletedApplicationInfo(UUID applicationId) throws Exception;

	ApplicationInfo getApplicationInfo( Identifier id ) throws Exception;

    void removeAdminUserFromOrganization( UUID userId, UUID organizationId, boolean force ) throws Exception;

    ApplicationInfo getApplicationInfoFromAccessToken( String token ) throws Exception;

	ServiceResults getApplicationMetadata( UUID applicationId ) throws Exception;

	BiMap<UUID, String> getApplicationsForOrganization( UUID organizationId ) throws Exception;

	BiMap<UUID, String> getApplicationsForOrganizations( Set<UUID> organizationIds ) throws Exception;

	String getClientIdForApplication( UUID applicationId );

	String getClientIdForOrganization( UUID organizationId );

	String getClientSecretForApplication( UUID applicationId ) throws Exception;

	String getClientSecretForOrganization( UUID organizationId ) throws Exception;

	ServiceResults getOrganizationActivity( OrganizationInfo organization ) throws Exception;

	ServiceResults getOrganizationActivityForAdminUser( OrganizationInfo organization, UserInfo user )
			throws Exception;

	OrganizationInfo getOrganizationByIdentifier( Identifier id ) throws Exception;

	OrganizationInfo getOrganizationByName( String organizationName ) throws Exception;

	OrganizationInfo getOrganizationByUuid( UUID id ) throws Exception;

	Map<String, Object> getOrganizationData( OrganizationInfo organization ) throws Exception;

	UUID getOrganizationIdForApplication( UUID applicationId ) throws Exception;

	OrganizationInfo getOrganizationForApplication( UUID applicationId ) throws Exception;

	OrganizationInfo getOrganizationInfoFromAccessToken( String token ) throws Exception;

	BiMap<UUID, String> getOrganizations() throws Exception;

	BiMap<UUID, String> getOrganizationsForAdminUser( UUID userId ) throws Exception;

	String getPasswordResetTokenForAdminUser( UUID userId, long ttl, UUID organizationId ) throws Exception;

	UserInfo getAdminUserByUuid( UUID id ) throws Exception;

	UUID importApplication( UUID organizationId, Application application ) throws Exception;

	OrganizationInfo importOrganization( UUID organizationId, OrganizationInfo organizationInfo,
												Map<String, Object> properties ) throws Exception;

	boolean isAdminUserActivated( UUID userId ) throws Exception;

	boolean isAdminUserEnabled( UUID userId ) throws Exception;

	boolean isOrganizationActivated( UUID organizationId ) throws Exception;

	boolean isOrganizationEnabled( UUID organizationId ) throws Exception;

	boolean newAdminUsersNeedSysAdminApproval();

	boolean newAdminUsersRequireConfirmation();

	String newClientSecretForApplication( UUID applicationId ) throws Exception;

	String newClientSecretForOrganization( UUID organizationId ) throws Exception;

	boolean newOrganizationsNeedSysAdminApproval();

	void postOrganizationActivity( UUID organizationId, UserInfo user, String verb, EntityRef object,
										  String objectType, String objectName, String title, String content )
			throws Exception;

	void removeAdminUserFromOrganization( UUID userId, UUID organizationId ) throws Exception;

	void removeOrganizationApplication( UUID organizationId, UUID applicationId ) throws Exception;

	void startAdminUserActivationFlow( UUID organizationId, UserInfo user ) throws Exception;

	void sendAdminUserEmail( UserInfo user, String subject, String html ) throws Exception;

	void startAdminUserPasswordResetFlow( UUID organizationId, UserInfo user ) throws Exception;

	void startOrganizationActivationFlow( OrganizationInfo organization ) throws Exception;

	void sendOrganizationEmail( OrganizationInfo organization, String subject, String html ) throws Exception;

	void setAdminUserPassword( UUID userId, String newPassword ) throws Exception;

	void setAdminUserPassword( UUID userId, String oldPassword, String newPassword ) throws Exception;

	void setup() throws Exception;

	UserInfo updateAdminUser( UserInfo user, String username, String name, String email,
									 Map<String, Object> json ) throws Exception;

	boolean verifyAdminUserPassword( UUID userId, String password ) throws Exception;

	UserInfo verifyAdminUserPasswordCredentials( String name, String password ) throws Exception;

	UserInfo verifyMongoCredentials( String name, String nonce, String key ) throws Exception;

	void activateAppUser( UUID applicationId, UUID userId ) throws Exception;

	ActivationState handleActivationTokenForAppUser( UUID applicationId, UUID userId, String token )
			throws Exception;

	ActivationState handleConfirmationTokenForAppUser( UUID applicationId, UUID userId, String token )
			throws Exception;

	boolean checkPasswordResetTokenForAppUser( UUID applicationId, UUID userId, String token ) throws Exception;

	String getAccessTokenForAppUser( UUID applicationId, UUID userId, long duration ) throws Exception;

	Long getLastAdminPasswordChange( UUID userId ) throws Exception;

	/** Revoke all active access tokens for this admin user */
	void revokeAccessTokensForAppUser( UUID applicationId, UUID userId ) throws Exception;

	void revokeAccessTokenForAppUser( String token ) throws Exception;

	User getAppUserByIdentifier( UUID applicationId, Identifier identifier ) throws Exception;

	void startAppUserPasswordResetFlow( UUID applicationId, User user ) throws Exception;

	void startAppUserActivationFlow( UUID applicationId, User user ) throws Exception;

	void setAppUserPassword( UUID applicationId, UUID userId, String newPassword ) throws Exception;

	void setAppUserPassword( UUID applicationId, UUID userId, String oldPassword, String newPassword )
			throws Exception;

	CredentialsInfo getAppUserCredentialsInfo( final UUID applicationId, final UUID userId ) throws Exception;

	void  setAppUserCredentialsInfo( final UUID applicationId, final UUID userId, final CredentialsInfo credentialsInfo ) throws Exception;


	User verifyAppUserPasswordCredentials( UUID applicationId, String name, String password ) throws Exception;

	UserInfo getAppUserFromAccessToken( String token ) throws Exception;

	void setAppUserPin( UUID applicationId, UUID userId, String newPin ) throws Exception;

	void sendAppUserPin( UUID applicationId, UUID userId ) throws Exception;

	User verifyAppUserPinCredentials( UUID applicationId, String name, String pin ) throws Exception;

	PrincipalCredentialsToken getPrincipalCredentialsTokenForClientCredentials( String clientId,
																					   String clientSecret )
			throws Exception;

	void confirmAdminUser( UUID userId ) throws Exception;

	void unconfirmAdminUser( UUID userId ) throws Exception;

	boolean isAdminUserConfirmed( UUID userId ) throws Exception;

	void countAdminUserAction( UserInfo user, String action ) throws Exception;

	boolean newAppUsersNeedAdminApproval( UUID applicationId ) throws Exception;

	boolean newAppUsersRequireConfirmation( UUID applicationId ) throws Exception;

	void provisionSuperuser() throws Exception;

	void resetSuperUser(String username, String password, String email) throws Exception;

	List<OrganizationInfo> getOrganizations( UUID startResult, int count ) throws Exception;

	/** Add the properties to the organization */
	void setOrganizationProps( UUID orgId, Map<String, Object> props ) throws Exception;

	/** Get the organization properties, returns them in the group object */
	Group getOrganizationProps( UUID orgId ) throws Exception;

	Object registerAppWithAPM( OrganizationInfo orgInfo, ApplicationInfo appInfo ) throws Exception;

	/** For testing purposes only */
	Properties getProperties();

	void deleteApplication(UUID applicationId) throws Exception;

	ApplicationInfo restoreApplication(UUID applicationId) throws Exception;

	long getApplicationSize(final UUID applicationId);

	long getCollectionSize(final UUID applicationId, final String collectionName);

	Map<String,Long> getEachCollectionSize(final UUID applicationId);

	OrganizationConfig getOrganizationConfigDefaultsOnly();

	OrganizationConfig getOrganizationConfigByName( String organizationName ) throws Exception;

	OrganizationConfig getOrganizationConfigByUuid( UUID id ) throws Exception;

	OrganizationConfig getOrganizationConfigForApplication( UUID applicationId ) throws Exception;

	void updateOrganizationConfig( OrganizationConfig organizationConfig ) throws Exception;

	Observable<Id> deleteAllEntities(final UUID applicationId,final int limit);
}
