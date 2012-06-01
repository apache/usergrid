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
package org.usergrid.management;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.oauth.AccessInfo;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.services.ServiceResults;

import com.google.common.collect.BiMap;

public interface ManagementService {

	public void activateAdminUser(UUID userId) throws Exception;

	public void activateOrganization(OrganizationInfo organization)
			throws Exception;

	public void activateOrganization(OrganizationInfo organization,
			boolean sendEmail) throws Exception;

	public void addAdminUserToOrganization(UserInfo user,
			OrganizationInfo organization) throws Exception;

	public UUID addApplicationToOrganization(UUID organizationId,
			UUID applicationId) throws Exception;

	public AccessInfo authorizeClient(String clientId, String clientSecret)
			throws Exception;

	public ActivationState handleConfirmationTokenForAdminUser(UUID userId,
			String token) throws Exception;

	public ActivationState handleActivationTokenForAdminUser(UUID userId,
			String token) throws Exception;

	public ActivationState handleActivationTokenForOrganization(
			UUID organizationId, String token) throws Exception;

	public boolean checkPasswordResetTokenForAdminUser(UUID userId, String token)
			throws Exception;

	public UserInfo createAdminUser(String username, String name, String email,
			String password, boolean activated, boolean disabled,
			boolean sendEmail) throws Exception;

	public UserInfo createAdminFrom(User user, String password,
			boolean sendEmail) throws Exception;

	public UserInfo createAdminFromPrexistingPassword(User user,
			String precypheredPassword, boolean sendEmail) throws Exception;

	public UUID createApplication(UUID organizationId, String applicationName)
			throws Exception;

	public UUID createApplication(UUID organizationId, String applicationName,
			Map<String, Object> properties) throws Exception;

	public OrganizationInfo createOrganization(String organizationName,
			UserInfo user, boolean activated, boolean sendEmail)
			throws Exception;

	public OrganizationOwnerInfo createOwnerAndOrganization(
			String organizationName, String username, String name,
			String email, String password, boolean activated, boolean disabled,
			boolean sendEmail) throws Exception;

	public void deactivateAdminUser(UUID userId) throws Exception;

	public void deactivateOrganization(UUID organizationId) throws Exception;

	public void deleteOrganizationApplication(UUID organizationId,
			UUID applicationId) throws Exception;

	public void disableAdminUser(UUID userId) throws Exception;

	public void disableOrganization(UUID organizationId) throws Exception;

	public void enableAdminUser(UUID userId) throws Exception;

	public void enableOrganization(UUID organizationId) throws Exception;

	public UserInfo findAdminUser(String identifier);

	public String getAccessTokenForAdminUser(UUID userId) throws Exception;

	public String getActivationTokenForAdminUser(UUID userId) throws Exception;

	public String getConfirmationTokenForAdminUser(UUID userId)
			throws Exception;

	public String getActivationTokenForOrganization(UUID organizationId)
			throws Exception;

	public ServiceResults getAdminUserActivities(UserInfo user)
			throws Exception;

	public ServiceResults getAdminUserActivity(UserInfo user) throws Exception;

	public UserInfo getAdminUserByEmail(String email) throws Exception;

	public UserInfo getAdminUserByIdentifier(Identifier id) throws Exception;

	public UserInfo getAdminUserByUsername(String username) throws Exception;

	public Entity getAdminUserEntityByIdentifier(Identifier id)
			throws Exception;

	public Entity getAdminUserEntityByUuid(UUID id) throws Exception;

	public Entity getAdminUserEntityFromAccessToken(String token)
			throws Exception;

	public UserInfo getAdminUserInfoFromAccessToken(String token)
			throws Exception;

	public Map<String, Object> getAdminUserOrganizationData(UserInfo user)
			throws Exception;

	public Map<String, Object> getAdminUserOrganizationData(UUID userId)
			throws Exception;

	public List<UserInfo> getAdminUsersForOrganization(UUID organizationId)
			throws Exception;

	public ApplicationInfo getApplicationInfo(String applicationName)
			throws Exception;

	public ApplicationInfo getApplicationInfo(UUID applicationId)
			throws Exception;

	public ApplicationInfo getApplicationInfo(Identifier id) throws Exception;

	public ApplicationInfo getApplicationInfoFromAccessToken(String token)
			throws Exception;

	public BiMap<UUID, String> getApplicationsForOrganization(
			UUID organizationId) throws Exception;

	public BiMap<UUID, String> getApplicationsForOrganizations(
			Set<UUID> organizationIds) throws Exception;

	public String getClientIdForApplication(UUID applicationId);

	public String getClientIdForOrganization(UUID organizationId);

	public String getClientSecretForApplication(UUID applicationId)
			throws Exception;

	public String getClientSecretForOrganization(UUID organizationId)
			throws Exception;

	public ServiceResults getOrganizationActivity(OrganizationInfo organization)
			throws Exception;

	public ServiceResults getOrganizationActivityForAdminUser(
			OrganizationInfo organization, UserInfo user) throws Exception;

	public OrganizationInfo getOrganizationByIdentifier(Identifier id)
			throws Exception;

	public OrganizationInfo getOrganizationByName(String organizationName)
			throws Exception;

	public OrganizationInfo getOrganizationByUuid(UUID id) throws Exception;

	public Map<String, Object> getOrganizationData(OrganizationInfo organization)
			throws Exception;

	public Entity getOrganizationEntityByIdentifier(Identifier id)
			throws Exception;

	public Entity getOrganizationEntityByName(String organizationName)
			throws Exception;

	public Entity getOrganizationEntityByUuid(UUID id) throws Exception;

	public OrganizationInfo getOrganizationForApplication(UUID applicationId)
			throws Exception;

	public OrganizationInfo getOrganizationInfoFromAccessToken(String token)
			throws Exception;

	public BiMap<UUID, String> getOrganizations() throws Exception;

	public BiMap<UUID, String> getOrganizationsForAdminUser(UUID userId)
			throws Exception;

	public String getPasswordResetTokenForAdminUser(UUID userId)
			throws Exception;

	public UserInfo getAdminUserByUuid(UUID id) throws Exception;

	public UUID importApplication(UUID organizationId, Application application)
			throws Exception;

	public OrganizationInfo importOrganization(UUID organizationId,
			OrganizationInfo organizationInfo, Map<String, Object> properties)
			throws Exception;

	public boolean isAdminUserActivated(UUID userId) throws Exception;

	public boolean isAdminUserEnabled(UUID userId) throws Exception;

	public boolean isOrganizationActivated(UUID organizationId)
			throws Exception;

	public boolean isOrganizationEnabled(UUID organizationId) throws Exception;

	public boolean newAdminUsersNeedSysAdminApproval();

	public boolean newAdminUsersRequireConfirmation();

	public String newClientSecretForApplication(UUID applicationId)
			throws Exception;

	public String newClientSecretForOrganization(UUID organizationId)
			throws Exception;

	public boolean newOrganizationsNeedSysAdminApproval();

	public void postOrganizationActivity(UUID organizationId, UserInfo user,
			String verb, EntityRef object, String objectType,
			String objectName, String title, String content) throws Exception;

	public void removeAdminUserFromOrganization(UUID userId, UUID organizationId)
			throws Exception;

	public void removeOrganizationApplication(UUID organizationId,
			UUID applicationId) throws Exception;

	public void startAdminUserActivationFlow(UserInfo user) throws Exception;

	public void sendAdminUserEmail(UserInfo user, String subject, String html)
			throws Exception;

	public void startAdminUserPasswordResetFlow(UserInfo user) throws Exception;

	public void startOrganizationActivationFlow(OrganizationInfo organization)
			throws Exception;

	public void sendOrganizationEmail(OrganizationInfo organization,
			String subject, String html) throws Exception;

	public void setAdminUserPassword(UUID userId, String newPassword)
			throws Exception;

	public void setAdminUserPassword(UUID userId, String oldPassword,
			String newPassword) throws Exception;

	public void setup() throws Exception;

	public UserInfo updateAdminUser(UserInfo user, String username,
			String name, String email) throws Exception;

	public boolean verifyAdminUserPassword(UUID userId, String password)
			throws Exception;

	public UserInfo verifyAdminUserPasswordCredentials(String name,
			String password) throws Exception;

	public UserInfo verifyMongoCredentials(String name, String nonce, String key)
			throws Exception;

	public void activateAppUser(UUID applicationId, UUID userId)
			throws Exception;

	public ActivationState handleActivationTokenForAppUser(UUID applicationId,
			UUID userId, String token) throws Exception;

	public ActivationState handleConfirmationTokenForAppUser(
			UUID applicationId, UUID userId, String token) throws Exception;

	public boolean checkPasswordResetTokenForAppUser(UUID applicationId,
			UUID userId, String token) throws Exception;

	public String getAccessTokenForAppUser(UUID applicationId, UUID userId)
			throws Exception;

	public User getAppUserByIdentifier(UUID applicationId, Identifier identifier)
			throws Exception;

	public void startAppUserPasswordResetFlow(UUID applicationId, User user)
			throws Exception;

	public void startAppUserActivationFlow(UUID applicationId, User user)
			throws Exception;

	public void setAppUserPassword(UUID applicationId, UUID userId,
			String newPassword) throws Exception;

	public void setAppUserPassword(UUID applicationId, UUID userId,
			String oldPassword, String newPassword) throws Exception;

	public User verifyAppUserPasswordCredentials(UUID applicationId,
			String name, String password) throws Exception;

	public UserInfo getAppUserFromAccessToken(String token) throws Exception;

	public void setAppUserPin(UUID applicationId, UUID userId, String newPin)
			throws Exception;

	public void sendAppUserPin(UUID applicationId, UUID userId)
			throws Exception;

	public User verifyAppUserPinCredentials(UUID applicationId, String name,
			String pin) throws Exception;

	public PrincipalCredentialsToken getPrincipalCredentialsTokenForClientCredentials(
			String clientId, String clientSecret) throws Exception;

	public void confirmAdminUser(UUID userId) throws Exception;

	public void unconfirmAdminUser(UUID userId) throws Exception;

	public boolean isAdminUserConfirmed(UUID userId) throws Exception;

	public void countAdminUserAction(UserInfo user, String action)
			throws Exception;

	public boolean newAppUsersNeedAdminApproval(UUID applicationId)
			throws Exception;

	public boolean newAppUsersRequireConfirmation(UUID applicationId)
			throws Exception;

	public User getOrCreateUserForFacebookAccessToken(UUID applicationId,
			String fb_access_token) throws Exception;

}
