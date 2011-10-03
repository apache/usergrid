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

public class SimpleCollectionRef implements CollectionRef {

	public static final String MEMBER_ENTITY_TYPE = "member";

	protected final EntityRef ownerRef;
	protected final String collectionName;
	protected final EntityRef itemRef;
	protected final String type;
	protected final UUID id;

	public SimpleCollectionRef(EntityRef ownerRef, String collectionName,
			EntityRef itemRef) {
		this.ownerRef = ownerRef;
		this.collectionName = collectionName;
		this.itemRef = itemRef;
		type = itemRef.getType() + ":" + MEMBER_ENTITY_TYPE;
		id = CassandraPersistenceUtils.keyID(ownerRef.getUuid(), collectionName, itemRef.getUuid());
	}

	@Override
	public EntityRef getOwnerEntity() {
		return ownerRef;
	}

	@Override
	public String getCollectionName() {
		return collectionName;
	}

	@Override
	public EntityRef getItemRef() {
		return itemRef;
	}

	@Override
	public UUID getUuid() {
		return id;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		if ((type == null) && (id == null)) {
			return "CollectionRef(" + SimpleEntityRef.NULL_ID.toString() + ")";
		}
		if (type == null) {
			return "CollectionRef(" + id.toString() + ")";
		}
		return type + "(" + id + "," + ownerRef + "," + collectionName + ","
				+ itemRef + ")";
	}
}
