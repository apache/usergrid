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
package org.usergrid.persistence.cassandra;

import java.util.UUID;

import org.usergrid.persistence.ConnectedEntityRef;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.SimpleEntityRef;

public class ConnectedEntityRefImpl extends SimpleEntityRef implements
		ConnectedEntityRef {

	final String connectionType;

	public ConnectedEntityRefImpl() {
		super(null, null);
		connectionType = null;
	}

	public ConnectedEntityRefImpl(UUID entityId) {
		super(null, entityId);
		connectionType = null;
	}

	public ConnectedEntityRefImpl(EntityRef ref) {
		super(ref);
		connectionType = null;
	}

	public ConnectedEntityRefImpl(String connectionType,
			EntityRef connectedEntity) {
		super(connectedEntity.getType(), connectedEntity.getUuid());
		this.connectionType = connectionType;
	}

	public ConnectedEntityRefImpl(String connectionType, String entityType,
			UUID entityId) {
		super(entityType, entityId);
		this.connectionType = connectionType;
	}

	@Override
	public String getConnectionType() {
		return connectionType;
	}

	public static String getConnectionType(ConnectedEntityRef connection) {
		if (connection == null) {
			return null;
		}
		return connection.getConnectionType();
	}

	public static UUID getConnectedEntityId(ConnectedEntityRef connection) {
		if (connection == null) {
			return null;
		}
		return connection.getUuid();
	}

	public static String getConnectedEntityType(ConnectedEntityRef connection) {
		if (connection == null) {
			return null;
		}
		return connection.getType();
	}

}
