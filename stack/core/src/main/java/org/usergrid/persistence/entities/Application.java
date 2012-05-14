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
package org.usergrid.persistence.entities;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityCollection;
import org.usergrid.persistence.annotations.EntityDictionary;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * Applications represent the topmost container for all entities.
 */
@XmlRootElement
public class Application extends TypedEntity {

	public static final String ENTITY_TYPE = "application";

	public static final String COLLECTION_USERS = "users";

	public static final String COLLECTION_GROUPS = "groups";

	public static final String COLLECTION_ASSETS = "assets";

	public static final String COLLECTION_ACTIVITIES = "activities";

	@EntityProperty(indexed = true, fulltextIndexed = false, required = true, mutable = false, aliasProperty = true, basic = true)
	protected String name;

	@EntityProperty(basic = true)
	protected String title;

	@EntityProperty
	protected String description;

	@EntityDictionary(keyType = java.lang.String.class)
	protected Set<String> collections;

	@EntityDictionary(keyType = java.lang.String.class, valueType = java.lang.String.class)
	protected Map<String, String> rolenames;

	@EntityDictionary(keyType = java.lang.String.class)
	protected Set<String> counters;

	@EntityProperty(indexed = false)
	protected Boolean activated;

	@EntityProperty(indexed = false)
	protected Boolean disabled;

	@EntityProperty(name = "allow_open_registration", indexed = false)
	protected Boolean allowOpenRegistration;

	@EntityProperty(name = "registration_requires_email_confirmation", indexed = false)
	protected Boolean registrationRequiresEmailConfirmation;

	@EntityProperty(name = "registration_requires_admin_approval", indexed = false)
	protected Boolean registrationRequiresAdminApproval;

	@EntityProperty(name = "notify_admin_of_new_users", indexed = false)
	protected Boolean notifyAdminOfNewUsers;

	@EntityDictionary(keyType = java.lang.String.class, valueType = OAuthProvider.class)
	protected Map<String, OAuthProvider> oauthproviders;

	@EntityDictionary(keyType = java.lang.String.class, valueType = java.lang.String.class)
	protected Map<String, String> credentials;

	@EntityDictionary(keyType = java.lang.String.class, valueType = WebHook.class)
	protected Map<String, WebHook> webhooks;

	@EntityCollection(type = "activity", propertiesIndexed = { "created",
			"modified", "published", "content" }, subkeys = "verb", reversed = true, sort = "published desc")
	protected List<UUID> activities;

	@EntityCollection(type = "asset", subkeys = { "owner" }, propertiesIndexed = {
			"created", "modified", "path" })
	protected List<UUID> assets;

	@EntityCollection(type = "event", propertiesIndexed = { "created",
			"modified", "content" })
	protected List<UUID> events;

	@EntityCollection(type = "folder", propertiesIndexed = { "created",
			"modified", "path" })
	protected List<UUID> folders;

	@EntityCollection(type = "group", propertiesIndexed = { "created",
			"modified", "path", "name", "title" }, indexingDynamicProperties = true)
	protected List<UUID> groups;

	@EntityCollection(type = "user", propertiesIndexed = { "created",
			"modified", "username", "email", "firstname", "middlename",
			"lastname" }, dictionariesIndexed = { "aliases" }, indexingDynamicProperties = true)
	protected List<UUID> users;

	@EntityCollection(type = "device", propertiesIndexed = {}, indexingDynamicProperties = false)
	protected List<UUID> devices;

	public Application() {
		// id = UUIDUtils.newTimeUUID();
	}

	public Application(UUID id) {
		uuid = id;
	}

	@Override
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean activated() {
		return (activated != null) && activated;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Boolean getActivated() {
		return activated;
	}

	public void setActivated(Boolean activated) {
		this.activated = activated;
	}

	public boolean disabled() {
		return (disabled != null) && disabled;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public boolean allowOpenRegistration() {
		return (allowOpenRegistration != null) && allowOpenRegistration;
	}

	@JsonProperty("allow_open_registration")
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Boolean getAllowOpenRegistration() {
		return allowOpenRegistration;
	}

	@JsonProperty("allow_open_registration")
	public void setAllowOpenRegistration(Boolean allowOpenRegistration) {
		this.allowOpenRegistration = allowOpenRegistration;
	}

	public boolean registrationRequiresEmailConfirmation() {
		return (registrationRequiresEmailConfirmation != null)
				&& registrationRequiresEmailConfirmation;
	}

	@JsonProperty("registration_requires_email_confirmation")
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Boolean getRegistrationRequiresEmailConfirmation() {
		return registrationRequiresEmailConfirmation;
	}

	@JsonProperty("registration_requires_email_confirmation")
	public void setRegistrationRequiresEmailConfirmation(
			Boolean registrationRequiresEmailConfirmation) {
		this.registrationRequiresEmailConfirmation = registrationRequiresEmailConfirmation;
	}

	public boolean registrationRequiresAdminApproval() {
		return (registrationRequiresAdminApproval != null)
				&& registrationRequiresAdminApproval;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	@JsonProperty("registration_requires_admin_approval")
	public Boolean getRegistrationRequiresAdminApproval() {
		return registrationRequiresAdminApproval;
	}

	@JsonProperty("registration_requires_admin_approval")
	public void setRegistrationRequiresAdminApproval(
			Boolean registrationRequiresAdminApproval) {
		this.registrationRequiresAdminApproval = registrationRequiresAdminApproval;
	}

	public boolean notifyAdminOfNewUsers() {
		return (notifyAdminOfNewUsers != null) && notifyAdminOfNewUsers;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	@JsonProperty("notify_admin_of_new_users")
	public Boolean getNotifyAdminOfNewUsers() {
		return notifyAdminOfNewUsers;
	}

	@JsonProperty("notify_admin_of_new_users")
	public void setNotifyAdminOfNewUsers(Boolean notifyAdminOfNewUsers) {
		this.notifyAdminOfNewUsers = notifyAdminOfNewUsers;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getUsers() {
		return users;
	}

	public void setUsers(List<UUID> users) {
		this.users = users;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getGroups() {
		return groups;
	}

	public void setGroups(List<UUID> groups) {
		this.groups = groups;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Set<String> getCollections() {
		return collections;
	}

	public void setCollections(Set<String> collections) {
		this.collections = collections;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, String> getRolenames() {
		return rolenames;
	}

	public void setRolenames(Map<String, String> rolenames) {
		this.rolenames = rolenames;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Set<String> getCounters() {
		return counters;
	}

	public void setCounters(Set<String> counters) {
		this.counters = counters;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getAssets() {
		return assets;
	}

	public void setAssets(List<UUID> assets) {
		this.assets = assets;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, String> getCredentials() {
		return credentials;
	}

	public void setCredentials(Map<String, String> credentials) {
		this.credentials = credentials;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getActivities() {
		return activities;
	}

	public void setActivities(List<UUID> activities) {
		this.activities = activities;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getFolders() {
		return folders;
	}

	public void setFolders(List<UUID> folders) {
		this.folders = folders;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getEvents() {
		return events;
	}

	public void setEvents(List<UUID> events) {
		this.events = events;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getDevices() {
		return devices;
	}

	public void setDevices(List<UUID> devices) {
		this.devices = devices;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, OAuthProvider> getOauthproviders() {
		return oauthproviders;
	}

	public void setOauthproviders(Map<String, OAuthProvider> oauthproviders) {
		this.oauthproviders = oauthproviders;
	}

	@XmlRootElement
	public static class OAuthProvider {
		String clientId;
		String clientSecret;
		String redirectUris;
		String javaScriptOrigins;
		String authorizationEndpointUrl;
		String accessTokenEndpointUrl;
		String requestTokenEndpointUrl;
		String version = "1.0a";

		public OAuthProvider() {
		}

		public OAuthProvider(String clientId, String clientSecret) {
			this.clientId = clientId;
			this.clientSecret = clientSecret;
		}

		public OAuthProvider(String clientId, String clientSecret,
				String redirectUris, String javaScriptOrigins) {
			this.clientId = clientId;
			this.clientSecret = clientSecret;
			this.redirectUris = redirectUris;
			this.javaScriptOrigins = javaScriptOrigins;
		}

		public OAuthProvider(String clientId, String clientSecret,
				String redirectUris, String javaScriptOrigins,
				String authorizationEndpointUrl, String accessTokenEndpointUrl,
				String requestTokenEndpointUrl) {
			this.clientId = clientId;
			this.clientSecret = clientSecret;
			this.redirectUris = redirectUris;
			this.javaScriptOrigins = javaScriptOrigins;
			this.authorizationEndpointUrl = authorizationEndpointUrl;
			this.accessTokenEndpointUrl = accessTokenEndpointUrl;
			this.requestTokenEndpointUrl = requestTokenEndpointUrl;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getRedirectUris() {
			return redirectUris;
		}

		public void setRedirectUris(String redirectUris) {
			this.redirectUris = redirectUris;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getJavaScriptOrigins() {
			return javaScriptOrigins;
		}

		public void setJavaScriptOrigins(String javaScriptOrigins) {
			this.javaScriptOrigins = javaScriptOrigins;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getAuthorizationEndpointUrl() {
			return authorizationEndpointUrl;
		}

		public void setAuthorizationEndpointUrl(String authorizationEndpointUrl) {
			this.authorizationEndpointUrl = authorizationEndpointUrl;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getAccessTokenEndpointUrl() {
			return accessTokenEndpointUrl;
		}

		public void setAccessTokenEndpointUrl(String accessTokenEndpointUrl) {
			this.accessTokenEndpointUrl = accessTokenEndpointUrl;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getRequestTokenEndpointUrl() {
			return requestTokenEndpointUrl;
		}

		public void setRequestTokenEndpointUrl(String requestTokenEndpointUrl) {
			this.requestTokenEndpointUrl = requestTokenEndpointUrl;
		}

		@Override
		public String toString() {
			return "OAuthProvider [clientId=" + clientId + ", clientSecret="
					+ clientSecret + ", redirectUris=" + redirectUris
					+ ", javaScriptOrigins=" + javaScriptOrigins
					+ ", authorizationEndpointUrl=" + authorizationEndpointUrl
					+ ", accessTokenEndpointUrl=" + accessTokenEndpointUrl
					+ ", requestTokenEndpointUrl=" + requestTokenEndpointUrl
					+ ", version=" + version + "]";
		}

	}

	@XmlRootElement
	public static class WebHook {
		String type;
		String uri;

		public WebHook() {
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}
	}

}
