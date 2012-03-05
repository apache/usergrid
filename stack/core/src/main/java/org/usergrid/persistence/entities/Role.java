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
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityCollection;
import org.usergrid.persistence.annotations.EntityDictionary;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * Groups are used to organize users.
 */
@XmlRootElement
public class Role extends TypedEntity {

	public static final String ENTITY_TYPE = "role";

	@EntityProperty(indexed = true, fulltextIndexed = false, required = true, indexedInConnections = false, aliasProperty = true, mutable = false, unique = true)
	protected String name;

	@EntityProperty(mutable = true)
	protected String roleName;

	@EntityProperty(mutable = true)
	protected String title;

	@EntityDictionary(keyType = java.lang.String.class)
	protected Set<String> permissions;

	@EntityCollection(type = "user", linkedCollection = "roles")
	protected List<UUID> users;

	@EntityCollection(type = "group", linkedCollection = "roles")
	protected List<UUID> groups;

	public Role() {
		// id = UUIDUtils.newTimeUUID();
	}

	public Role(UUID id) {
		this.uuid = id;
	}

	@Override
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
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
	public Set<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<String> permissions) {
		this.permissions = permissions;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getGroups() {
		return groups;
	}

	public void setGroups(List<UUID> groups) {
		this.groups = groups;
	}

}
