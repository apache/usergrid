/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.usergrid.persistence.entities;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityCollection;
import org.usergrid.persistence.annotations.EntityDictionary;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * Groups are used to organize users.
 */
@XmlRootElement
public class Group extends TypedEntity {

	public static final String ENTITY_TYPE = "group";

	public static final String CONNECTION_MEMBERSHIP = "membership";

	@EntityProperty(indexed = true, fulltextIndexed = false, required = true, indexedInConnections = true, aliasProperty = true, pathBasedName = true, mutable = true, unique = true, basic = true)
	protected String path;

	@EntityProperty(basic = true)
	protected String title;

	@EntityDictionary(keyType = java.lang.String.class)
	protected Set<String> connections;

	@EntityDictionary(keyType = java.lang.String.class, valueType = java.lang.String.class)
	protected Map<String, String> rolenames;

	@EntityDictionary(keyType = java.lang.String.class, valueType = CredentialsInfo.class)
	protected Map<String, CredentialsInfo> credentials;

	@EntityCollection(type = "user", propertiesIndexed = { "username", "email" }, linkedCollection = "groups", indexingDynamicProperties = true)
	protected List<UUID> users;

	@EntityCollection(type = "activity", propertiesIndexed = { "created",
			"published", "content" }, subkeys = { "verb" }, reversed = true, sort = "published desc")
	protected List<UUID> activities;

	@EntityCollection(type = "activity", propertiesIndexed = { "created",
			"published", "content" }, subkeys = { "verb" }, reversed = true, sort = "published desc")
	protected List<UUID> feed;

	@EntityCollection(type = "role")
	protected List<UUID> roles;

	public Group() {
		// id = UUIDUtils.newTimeUUID();
	}

	public Group(UUID id) {
		uuid = id;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getUsers() {
		return users;
	}

	public void setUsers(List<UUID> users) {
		this.users = users;
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
	public Map<String, CredentialsInfo> getCredentials() {
		return credentials;
	}

	public void setCredentials(Map<String, CredentialsInfo> credentials) {
		this.credentials = credentials;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getRoles() {
		return roles;
	}

	public void setRoles(List<UUID> roles) {
		this.roles = roles;
	}

}
