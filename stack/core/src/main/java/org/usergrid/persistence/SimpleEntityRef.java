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

public class SimpleEntityRef implements EntityRef {

	public static final UUID NULL_ID = new UUID(0, 0);

	protected final String type;

	protected final UUID id;

	public SimpleEntityRef(UUID id) {
		this.id = id;
		type = null;
	}

	public SimpleEntityRef(String type, UUID id) {
		this.type = type;
		this.id = id;
	}

	public SimpleEntityRef(EntityRef entityRef) {
		type = entityRef.getType();
		id = entityRef.getUuid();
	}

	public static EntityRef ref() {
		return new SimpleEntityRef(null, null);
	}

	@Override
	public UUID getUuid() {
		return id;
	}

	@Override
	public String getType() {
		return type;
	}

	public static EntityRef ref(String entityType, UUID entityId) {
		return new SimpleEntityRef(entityType, entityId);
	}

	public static EntityRef ref(UUID entityId) {
		return new SimpleEntityRef(null, entityId);
	}

	public static EntityRef ref(EntityRef ref) {
		return new SimpleEntityRef(ref);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		SimpleEntityRef other = (SimpleEntityRef) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		if ((type == null) && (id == null)) {
			return "EntityRef(" + NULL_ID.toString() + ")";
		}
		if (type == null) {
			return "EntityRef(" + id.toString() + ")";
		}
		return type + "(" + id + ")";
	}

	public static UUID getUuid(EntityRef ref) {
		if (ref == null) {
			return null;
		}
		return ref.getUuid();
	}

	public static String getType(EntityRef ref) {
		if (ref == null) {
			return null;
		}
		return ref.getType();
	}

}
