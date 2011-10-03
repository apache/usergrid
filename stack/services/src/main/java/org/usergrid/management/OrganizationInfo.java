/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
package org.usergrid.management;

import static org.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.usergrid.persistence.Schema.PROPERTY_PATH;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class OrganizationInfo {

	private UUID id;
	private String name;

	public OrganizationInfo() {
	}
	
	public OrganizationInfo(UUID id, String name) {
		this.id = id;
		this.name = name;
	}

	public OrganizationInfo(Map<String, Object> properties) {
		id = (UUID) properties.get(PROPERTY_UUID);
		name = (String) properties.get(PROPERTY_PATH);
	}

	public UUID getUuid() {
		return id;
	}
	
	public void setUuid(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public static List<OrganizationInfo> fromNameIdMap(Map<String, UUID> map) {
		List<OrganizationInfo> list = new ArrayList<OrganizationInfo>();
		for (Entry<String, UUID> s : map.entrySet()) {
			list.add(new OrganizationInfo(s.getValue(), s.getKey()));
		}
		return list;
	}

	public static List<OrganizationInfo> fromIdNameMap(Map<UUID, String> map) {
		List<OrganizationInfo> list = new ArrayList<OrganizationInfo>();
		for (Entry<UUID, String> s : map.entrySet()) {
			list.add(new OrganizationInfo(s.getKey(), s.getValue()));
		}
		return list;
	}

	public static Map<String, UUID> toNameIdMap(List<OrganizationInfo> list) {
		Map<String, UUID> map = new LinkedHashMap<String, UUID>();
		for (OrganizationInfo i : list) {
			map.put(i.getName(), i.getUuid());
		}
		return map;
	}

	public static Map<UUID, String> toIdNameMap(List<OrganizationInfo> list) {
		Map<UUID, String> map = new LinkedHashMap<UUID, String>();
		for (OrganizationInfo i : list) {
			map.put(i.getUuid(), i.getName());
		}
		return map;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		OrganizationInfo other = (OrganizationInfo) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

}
