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
