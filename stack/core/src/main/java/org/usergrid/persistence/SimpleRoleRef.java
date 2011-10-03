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
package org.usergrid.persistence;

import java.util.UUID;

import org.usergrid.persistence.cassandra.CassandraPersistenceUtils;
import org.usergrid.persistence.entities.Group;
import org.usergrid.utils.StringUtils;
import org.usergrid.utils.UUIDUtils;

public class SimpleRoleRef implements RoleRef {

	protected final UUID groupId;
	protected final String roleName;
	protected final UUID id;

	public SimpleRoleRef(String roleName) {
		this(null, roleName);
	}

	public SimpleRoleRef(UUID groupId, String roleName) {
		if (groupId != null) {
			this.groupId = groupId;
		} else {
			this.groupId = UUIDUtils.tryExtractUUID(roleName);
		}
		this.roleName = StringUtils.stringOrSubstringAfterLast(
				roleName.toLowerCase(), ':');
		if (groupId == null) {
			id = CassandraPersistenceUtils.keyID("role", this.groupId, this.roleName);
		} else {
			id = CassandraPersistenceUtils.keyID("role", this.roleName);
		}
	}

	public static SimpleRoleRef forRoleEntity(Entity role) {
		if (role == null) {
			return null;
		}
		UUID groupId = (UUID) role.getProperty("group");
		String name = role.getName();
		return new SimpleRoleRef(groupId, name);
	}

	public static SimpleRoleRef forRoleName(String roleName) {
		return new SimpleRoleRef(null, roleName);
	}

	public static SimpleRoleRef forGroupIdAndRoleName(UUID groupId,
			String roleName) {
		return new SimpleRoleRef(groupId, roleName);
	}

	public static UUID getIdForRoleName(String roleName) {
		return forRoleName(roleName).getUuid();
	}

	public static UUID getIdForGroupIdAndRoleName(UUID groupId, String roleName) {
		return forGroupIdAndRoleName(groupId, roleName).getUuid();
	}

	@Override
	public UUID getUuid() {
		return id;
	}

	@Override
	public String getType() {
		return "role";
	}

	@Override
	public EntityRef getGroupRef() {
		return new SimpleEntityRef(Group.ENTITY_TYPE, groupId);
	}

	@Override
	public String getRoleName() {
		return roleName;
	}

	@Override
	public UUID getGroupId() {
		return groupId;
	}

	@Override
	public String getApplicationRoleName() {
		if (groupId == null) {
			return roleName;
		}
		return groupId + ":" + roleName;
	}

}
