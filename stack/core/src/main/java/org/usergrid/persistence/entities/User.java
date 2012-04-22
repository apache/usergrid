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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityCollection;
import org.usergrid.persistence.annotations.EntityDictionary;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * The User entity class for representing users in the service.
 */
@XmlRootElement
public class User extends TypedEntity {

	public static final String ENTITY_TYPE = "user";

	public static final String CONNECTION_FOLLOW = "follow";
	
	public static final String PROP_UUID = "uuid";
	
	public static final String PROP_EMAIL = "email";
	

	@EntityProperty(indexed = true, fulltextIndexed = false, required = true, indexedInConnections = true, aliasProperty = true, unique = true, basic = true)
	protected String username;

	@EntityProperty(indexed = true, unique = true, basic = true)
	protected String email;

	@EntityProperty(indexed = true, fulltextIndexed=true)
	protected String name;

	@EntityProperty(indexed = false)
	protected Boolean activated;

	@EntityProperty(indexed = false)
	protected Boolean confirmed;

	@EntityProperty(indexed = false)
	protected Boolean disabled;

	@EntityProperty(indexed = true)
	protected String firstname;

	@EntityProperty(indexed = true)
	protected String middlename;

	@EntityProperty(indexed = true)
	protected String lastname;

	@EntityProperty(indexed = false)
	protected String picture;

	@EntityDictionary(keyType = java.lang.String.class)
	protected Set<String> connections;

	@EntityDictionary(keyType = java.lang.String.class, valueType = java.lang.String.class)
	protected Map<String, String> rolenames;

	@EntityDictionary(keyType = java.lang.String.class)
	protected Set<String> permissions;

	@EntityDictionary(keyType = java.lang.String.class, valueType = CredentialsInfo.class)
	protected Map<String, CredentialsInfo> credentials;

	@EntityCollection(type = "group", linkedCollection = "users", propertiesIndexed = { "path" }, indexingDynamicProperties = true)
	protected List<UUID> groups;

	@EntityCollection(type = "device", linkedCollection = "users", propertiesIndexed = {}, indexingDynamicProperties = false)
	protected List<UUID> devices;

	@EntityCollection(type = "activity", propertiesIndexed = { "created",
			"published", "content" }, subkeys = { "verb" }, reversed = true, sort = "published desc")
	protected List<UUID> activities;

	@EntityCollection(type = "activity", propertiesIndexed = { "created",
			"published", "content" }, subkeys = { "verb" }, reversed = true, sort = "published desc")
	protected List<UUID> feed;

	@EntityCollection(type = "role", linkedCollection = "users")
	protected List<UUID> roles;

	public User() {
		// id = UUIDUtils.newTimeUUID();
	}

	public User(UUID id) {
		uuid = id;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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
	public String getEmail() {
		return email;
	}

	@JsonIgnore
	public String getDisplayEmailAddress() {
		if (isNotBlank(name)) {
			return name + " <" + email + ">";
		}
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public boolean confirmed() {
		return (confirmed != null) && confirmed;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Boolean getConfirmed() {
		return confirmed;
	}

	public void setConfirmed(Boolean confirmed) {
		this.confirmed = confirmed;
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

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getMiddlename() {
		return middlename;
	}

	public void setMiddlename(String middlename) {
		this.middlename = middlename;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getGroups() {
		return groups;
	}

	public void setGroups(List<UUID> groups) {
		this.groups = groups;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getDevices() {
		return devices;
	}

	public void setDevices(List<UUID> devices) {
		this.devices = devices;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Set<String> getConnections() {
		return connections;
	}

	public void setConnections(Set<String> connections) {
		this.connections = connections;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, String> getRolenames() {
		return rolenames;
	}

	public void setRolenames(Map<String, String> rolenames) {
		this.rolenames = rolenames;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Set<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<String> permissions) {
		this.permissions = permissions;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, CredentialsInfo> getCredentials() {
		return credentials;
	}

	public void setCredentials(Map<String, CredentialsInfo> credentials) {
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
	public List<UUID> getFeed() {
		return feed;
	}

	public void setFeed(List<UUID> feed) {
		this.feed = feed;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getRoles() {
		return roles;
	}

	public void setRoles(List<UUID> roles) {
		this.roles = roles;
	}

}
