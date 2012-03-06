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
