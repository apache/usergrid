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

import static org.usergrid.persistence.SimpleEntityRef.ref;
import static org.usergrid.utils.ConversionUtils.ascii;
import static org.usergrid.utils.ConversionUtils.bytebuffer;
import static org.usergrid.utils.ConversionUtils.string;
import static org.usergrid.utils.ConversionUtils.uuid;
import static org.usergrid.utils.ConversionUtils.uuidToBytesNullOk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.hector.api.beans.HColumn;

import org.apache.cassandra.thrift.ColumnDef;
import org.apache.cassandra.thrift.IndexType;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.ConnectedEntityRef;
import org.usergrid.persistence.ConnectionRef;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.SimpleEntityRef;

/**
 * @author edanuff
 * 
 */
public class ConnectionRefImpl implements ConnectionRef {

	public static final int MAX_LINKS = 1;

	/**
	 * 
	 */
	public static final int ALL = 0;
	/**
	 * 
	 */
	public static final int BY_CONNECTION_TYPE = 1;
	/**
	 * 
	 */
	public static final int BY_ENTITY_TYPE = 2;
	/**
	 * 
	 */
	public static final int BY_CONNECTION_AND_ENTITY_TYPE = 3;

	/**
	 * 
	 */
	public static final String NULL_ENTITY_TYPE = "Null";
	/**
	 * 
	 */
	public static final UUID NULL_ID = new UUID(0, 0);

	private static final Logger logger = LoggerFactory
			.getLogger(ConnectionRefImpl.class);

	/**
	 * 
	 */
	public static final String CONNECTING_ENTITY_TYPE = "connectingEntityType";
	/**
	 * 
	 */
	public static final String CONNECTING_ENTITY_ID = "connectingEntityId";
	/**
	 * 
	 */
	public static final String PAIRED_CONNECTION_TYPE = "pairedConnectionType";
	/**
	 * 
	 */
	public static final String PAIRED_CONNECTING_ENTITY_TYPE = "pairedConnectingEntityType";
	/**
	 * 
	 */
	public static final String PAIRED_CONNECTING_ENTITY_ID = "pairedConnectingEntityId";
	/**
	 * 
	 */
	public static final String CONNECTION_TYPE = "connectionType";
	/**
	 * 
	 */
	public static final String CONNECTED_ENTITY_TYPE = "connectedEntityType";
	/**
	 * 
	 */
	public static final String CONNECTED_ENTITY_ID = "connectedEntityId";

	public static final String CONNECTION_ENTITY_TYPE = "Connection";
	public static final String CONNECTION_ENTITY_CONNECTION_TYPE = "connection";

	public static final String UUID_COMPARATOR = "UUIDType";

	private final EntityRef connectingEntity;

	private final List<ConnectedEntityRef> pairedConnections;

	private final ConnectedEntityRef connectedEntity;

	/**
	 * 
	 */
	public ConnectionRefImpl() {
		connectingEntity = SimpleEntityRef.ref();
		pairedConnections = new ArrayList<ConnectedEntityRef>();
		connectedEntity = new ConnectedEntityRefImpl();
	}

	/**
	 * @param connectingEntityType
	 * @param connectingEntityId
	 * @param connectionType
	 * @param connectedEntityType
	 * @param connectedEntityId
	 */
	public ConnectionRefImpl(String connectingEntityType,
			UUID connectingEntityId, String connectionType,
			String connectedEntityType, UUID connectedEntityId) {

		connectingEntity = ref(connectingEntityType, connectingEntityId);

		pairedConnections = new ArrayList<ConnectedEntityRef>();

		connectedEntity = new ConnectedEntityRefImpl(connectionType,
				connectedEntityType, connectedEntityId);
	}

	public ConnectionRefImpl(EntityRef connectingEntity, String connectionType,
			EntityRef connectedEntity) {

		this.connectingEntity = ref(connectingEntity);

		pairedConnections = new ArrayList<ConnectedEntityRef>();

		this.connectedEntity = new ConnectedEntityRefImpl(connectionType,
				connectedEntity);

	}

	public ConnectionRefImpl(ConnectionRef connection) {

		connectingEntity = connection.getConnectingEntity();

		List<ConnectedEntityRef> pc = connection.getPairedConnections();
		if (pc == null) {
			pc = new ArrayList<ConnectedEntityRef>();
		}
		pairedConnections = pc;

		connectedEntity = connection.getConnectedEntity();

	}

	public ConnectionRefImpl(EntityRef connectingEntity,
			ConnectedEntityRef... connections) {

		this.connectingEntity = ref(connectingEntity);

		ConnectedEntityRef ce = new ConnectedEntityRefImpl();
		List<ConnectedEntityRef> pc = new ArrayList<ConnectedEntityRef>();
		if (connections.length > 0) {

			ce = connections[connections.length - 1];

			if (connections.length > 1) {
				pc = Arrays.asList(Arrays.copyOfRange(connections, 0,
						connections.length - 2));
			}
		}
		pairedConnections = pc;
		connectedEntity = ce;
	}

	public ConnectionRefImpl(ConnectionRef connection,
			ConnectedEntityRef... connections) {

		if (connection == null) {
			throw new NullPointerException(
					"ConnectionImpl constructor \'connection\' cannot be null");
		}

		connectingEntity = connection.getConnectingEntity();

		if (connections.length > 0) {

			pairedConnections = new ArrayList<ConnectedEntityRef>();
			pairedConnections.addAll(connection.getPairedConnections());
			pairedConnections.add(connection.getConnectedEntity());

			connectedEntity = connections[connections.length - 1];

			if (connections.length > 1) {
				pairedConnections.addAll(Arrays.asList(Arrays.copyOfRange(
						connections, 0, connections.length - 2)));
			}
		} else {
			pairedConnections = new ArrayList<ConnectedEntityRef>();
			connectedEntity = new ConnectedEntityRefImpl();

		}
	}

	public ConnectionRefImpl(EntityRef connectingEntity,
			List<ConnectedEntityRef> pairedConnections,
			ConnectedEntityRef connectedEntity) {
		this.connectingEntity = connectingEntity;
		this.pairedConnections = pairedConnections;
		this.connectedEntity = connectedEntity;
	}

	public static ConnectionRefImpl toConnectedEntity(
			ConnectedEntityRef connectedEntity) {
		return new ConnectionRefImpl(ref(), connectedEntity);
	}

	public static ConnectionRefImpl toConnectedEntity(EntityRef entityRef) {
		return new ConnectionRefImpl(ref(), new ConnectedEntityRefImpl(
				entityRef));
	}

	@Override
	public EntityRef getConnectingEntity() {
		return connectingEntity;
	}

	/**
	 * @return
	 */
	public String getConnectingEntityType() {
		if (connectingEntity == null) {
			return null;
		}
		return connectingEntity.getType();
	}

	/**
	 * @return
	 */
	public UUID getConnectingEntityId() {
		if (connectingEntity == null) {
			return null;
		}
		return connectingEntity.getUuid();
	}

	@Override
	public List<ConnectedEntityRef> getPairedConnections() {
		return pairedConnections;
	}

	public ConnectedEntityRef getFirstPairedConnection() {
		ConnectedEntityRef pairedConnection = null;

		if ((pairedConnections != null) && (pairedConnections.size() > 0)) {
			pairedConnection = pairedConnections.get(0);
		}

		return pairedConnection;
	}

	public UUID getFirstPairedConnectedEntityId() {
		ConnectedEntityRef pairedConnection = getFirstPairedConnection();
		if (pairedConnection != null) {
			return pairedConnection.getUuid();
		}
		return null;
	}

	public String getFirstPairedConnectedEntityType() {
		ConnectedEntityRef pairedConnection = getFirstPairedConnection();
		if (pairedConnection != null) {
			return pairedConnection.getType();
		}
		return null;
	}

	public String getFirstPairedConnectionType() {
		ConnectedEntityRef pairedConnection = getFirstPairedConnection();
		if (pairedConnection != null) {
			return pairedConnection.getConnectionType();
		}
		return null;
	}

	@Override
	public ConnectedEntityRef getConnectedEntity() {
		return connectedEntity;
	}

	/**
	 * @return
	 */
	@Override
	public String getConnectionType() {
		if (connectedEntity == null) {
			return null;
		}
		return connectedEntity.getConnectionType();
	}

	/**
	 * @return
	 */
	public String getConnectedEntityType() {
		if (connectedEntity == null) {
			return null;
		}
		return connectedEntity.getType();
	}

	/**
	 * @return
	 */
	public UUID getConnectedEntityId() {
		return connectedEntity.getUuid();
	}

	private UUID id;

	/**
	 * @return connection id
	 */
	@Override
	public UUID getUuid() {
		if (id == null) {
			id = getId(getConnectingEntity(), getConnectedEntity(),
					getPairedConnections().toArray(new ConnectedEntityRef[0]));
		}
		return id;
	}

	@Override
	public String getType() {
		return CONNECTION_ENTITY_TYPE;
	}

	public UUID getIndexId() {
		return getIndexId(getConnectingEntity(), getConnectionType(),
				getConnectedEntityType(),
				pairedConnections.toArray(new ConnectedEntityRef[0]));
	}

	public UUID getConnectingIndexId() {
		return getIndexId(getConnectingEntity(), getConnectionType(), null,
				pairedConnections.toArray(new ConnectedEntityRef[0]));
	}

	public ConnectionRefImpl getConnectionToConnectionEntity() {
		return new ConnectionRefImpl(getConnectingEntity(),
				new ConnectedEntityRefImpl(CONNECTION_ENTITY_CONNECTION_TYPE,
						CONNECTION_ENTITY_TYPE, getUuid()));
	}

	/**
	 * @return index ids
	 */
	public UUID[] getIndexIds() {

		return getIndexIds(getConnectingEntity(), getConnectedEntity()
				.getConnectionType(), getConnectedEntity().getType(),
				getPairedConnections().toArray(new ConnectedEntityRef[0]));

	}

	/**
	 * @param columns
	 */
	public static ConnectionRefImpl loadFromColumns(
			List<HColumn<String, ByteBuffer>> columns) {

		List<ConnectedEntityRef> pairedConnections = new ArrayList<ConnectedEntityRef>();

		Map<String, ByteBuffer> map = CassandraPersistenceUtils
				.getColumnMap(columns);

		String connectingEntityType = filterDefault(string(map
				.get(CONNECTING_ENTITY_TYPE)));
		UUID connectingEntityId = filterDefault(uuid(map
				.get(CONNECTING_ENTITY_ID)));

		EntityRef connectingEntity = ref(connectingEntityType,
				connectingEntityId);

		int i = 0;
		UUID pairedConnectingEntityId = filterDefault(uuid(map
				.get(PAIRED_CONNECTING_ENTITY_ID)));

		while (pairedConnectingEntityId != null) {

			String pairedConnectionType = filterDefault(StringUtils
					.lowerCase(string(map.get(PAIRED_CONNECTION_TYPE + i))));

			String pairedConnectingEntityType = filterDefault(string(map
					.get(PAIRED_CONNECTING_ENTITY_TYPE + i)));
			ConnectedEntityRef pairedConnection = new ConnectedEntityRefImpl(
					pairedConnectionType, pairedConnectingEntityType,
					pairedConnectingEntityId);
			pairedConnections.add(pairedConnection);

			i++;

			pairedConnectingEntityId = filterDefault(uuid(map
					.get(PAIRED_CONNECTING_ENTITY_ID + i)));
		}

		String connectionType = filterDefault(StringUtils.lowerCase(string(map
				.get(CONNECTION_TYPE))));

		String connectedEntityType = filterDefault(string(map
				.get(CONNECTED_ENTITY_TYPE)));
		UUID connectedEntityId = filterDefault(uuid(map
				.get(CONNECTED_ENTITY_ID)));

		ConnectedEntityRef connectedEntity = new ConnectedEntityRefImpl(
				connectionType, connectedEntityType, connectedEntityId);

		return new ConnectionRefImpl(connectingEntity, pairedConnections,
				connectedEntity);

	}

	static String filterDefault(String type) {
		if (NULL_ENTITY_TYPE.equals(type)) {
			return null;
		}
		return type;
	}

	static UUID filterDefault(UUID uuid) {
		if (NULL_ID.equals(uuid)) {
			return null;
		}
		return uuid;
	}

	static String typeOrDefault(String type) {
		if ((type == null) || (type.length() == 0)) {
			return NULL_ENTITY_TYPE;
		}
		return type;
	}

	static UUID idOrDefault(UUID uuid) {
		if (uuid == null) {
			return NULL_ID;
		}
		return uuid;
	}

	/**
	 * @return columns for insert
	 */
	public Map<String, Object> toColumnMap() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put(CONNECTING_ENTITY_TYPE,
				typeOrDefault(getConnectingEntityType()));
		map.put(CONNECTING_ENTITY_ID, idOrDefault(getConnectingEntityId()));

		for (int i = 0; i < MAX_LINKS; i++) {
			String pairedConnectionType = null;
			String pairedConnectingEntityType = null;
			UUID pairedConnectingEntityId = null;

			if (i < pairedConnections.size()) {
				ConnectedEntityRef pairedConnection = pairedConnections.get(i);
				if (pairedConnection != null) {
					pairedConnectionType = pairedConnection.getConnectionType();
					pairedConnectingEntityType = pairedConnection.getType();
					pairedConnectingEntityId = pairedConnection.getUuid();
				}
			}

			map.put(PAIRED_CONNECTION_TYPE + i,
					typeOrDefault(pairedConnectionType));
			map.put(PAIRED_CONNECTING_ENTITY_TYPE + i,
					typeOrDefault(pairedConnectingEntityType));
			map.put(PAIRED_CONNECTING_ENTITY_ID + i,
					idOrDefault(pairedConnectingEntityId));
		}

		map.put(CONNECTION_TYPE, typeOrDefault(getConnectionType()));
		map.put(CONNECTED_ENTITY_TYPE, typeOrDefault(getConnectedEntityType()));
		map.put(CONNECTED_ENTITY_ID, idOrDefault(getConnectedEntityId()));

		return map;
	}

	public void addIndexExpressionsToQuery(
			IndexedSlicesQuery<UUID, String, ByteBuffer> q) {
		if (getConnectingEntityType() != null) {
			q.addEqualsExpression(CONNECTING_ENTITY_TYPE,
					bytebuffer(getConnectingEntityType()));
		}
		if (getConnectingEntityId() != null) {
			q.addEqualsExpression(CONNECTING_ENTITY_ID,
					bytebuffer(getConnectingEntityId()));
		}

		if (pairedConnections != null) {
			int i = 0;
			for (ConnectedEntityRef pairedConnection : pairedConnections) {
				if (pairedConnection != null) {
					String pairedConnectionType = pairedConnection
							.getConnectionType();
					String pairedConnectingEntityType = pairedConnection
							.getType();
					UUID pairedConnectingEntityId = pairedConnection.getUuid();

					if (pairedConnectionType != null) {
						q.addEqualsExpression(PAIRED_CONNECTION_TYPE + i,
								bytebuffer(pairedConnectionType));
					}

					if (pairedConnectingEntityType != null) {
						q.addEqualsExpression(
								PAIRED_CONNECTING_ENTITY_TYPE + i,
								bytebuffer(pairedConnectingEntityType));
					}
					if (pairedConnectingEntityId != null) {
						q.addEqualsExpression(PAIRED_CONNECTING_ENTITY_ID + i,
								bytebuffer(pairedConnectingEntityId));
					}
				}
				i++;
			}
		}

		if (getConnectionType() != null) {
			q.addEqualsExpression(CONNECTION_TYPE,
					bytebuffer(getConnectionType()));
		}

		if (getConnectedEntityType() != null) {
			q.addEqualsExpression(CONNECTED_ENTITY_TYPE,
					bytebuffer(getConnectedEntityType()));
		}
		if (getConnectedEntityId() != null) {
			q.addEqualsExpression(CONNECTED_ENTITY_ID,
					bytebuffer(getConnectedEntityId()));
		}

	}

	public static boolean connectionsNull(
			ConnectedEntityRef... pairedConnections) {
		if ((pairedConnections == null) || (pairedConnections.length == 0)) {
			return true;
		}
		for (ConnectedEntityRef pairedConnection : pairedConnections) {
			if ((pairedConnection != null)
					&& (pairedConnection.getUuid() != null)
					&& !pairedConnection.getUuid().equals(NULL_ID)) {
				return false;
			}
		}
		return true;
	}

	public static ConnectedEntityRef[] getConnections(
			ConnectedEntityRef... connections) {
		return connections;
	}

	public static List<ConnectedEntityRef> getConnectionsList(
			ConnectedEntityRef... connections) {
		return Arrays.asList(connections);
	}

	/**
	 * @param connectingEntityId
	 * @param connectionType
	 * @param connectedEntityId
	 * @return connection id
	 */
	public static UUID getId(UUID connectingEntityId, String connectionType,
			UUID connectedEntityId) {
		return getId(connectingEntityId, null, null, connectionType,
				connectedEntityId);
	}

	/**
	 * 
	 * Connection id is constructed from packed structure of properties strings
	 * are truncated to 16 ascii bytes. Connection id is now MD5'd into a UUID
	 * via UUID.nameUUIDFromBytes() so, technically string concatenation could
	 * be used prior to MD5
	 * 
	 * @param connectingEntityId
	 * @param pairedConnectionType
	 * @param pairedConnectingEntityId
	 * @param connectionType
	 * @param connectedEntityId
	 * @return connection id
	 */
	public static UUID getId(UUID connectingEntityId,
			String pairedConnectionType, UUID pairedConnectingEntityId,
			String connectionType, UUID connectedEntityId) {

		EntityRef connectingEntity = ref(connectingEntityId);

		ConnectedEntityRef[] pairedConnections = getConnections(new ConnectedEntityRefImpl(
				pairedConnectionType, null, pairedConnectingEntityId));

		ConnectedEntityRef connectedEntity = new ConnectedEntityRefImpl(
				connectionType, null, connectedEntityId);

		return getId(connectingEntity, connectedEntity, pairedConnections);
	}

	public static UUID getId(EntityRef connectingEntity,
			ConnectedEntityRef connectedEntity,
			ConnectedEntityRef... pairedConnections) {
		UUID uuid = null;
		try {

			if (connectionsNull(pairedConnections)
					&& connectionsNull(connectedEntity)) {
				return connectingEntity.getUuid();
			}

			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(
					16 + (32 * pairedConnections.length));

			byteStream.write(uuidToBytesNullOk(connectingEntity.getUuid()));

			for (ConnectedEntityRef connection : pairedConnections) {
				String connectionType = connection.getConnectionType();
				UUID connectedEntityID = connection.getUuid();

				byteStream.write(ascii(StringUtils.lowerCase(connectionType)));
				byteStream.write(uuidToBytesNullOk(connectedEntityID));

			}

			String connectionType = connectedEntity.getConnectionType();
			if (connectionType == null) {
				connectionType = NULL_ENTITY_TYPE;
			}

			UUID connectedEntityID = connectedEntity.getUuid();

			byteStream.write(ascii(StringUtils.lowerCase(connectionType)));
			byteStream.write(uuidToBytesNullOk(connectedEntityID));

			byte[] raw_id = byteStream.toByteArray();

			// logger.info("raw connection index id: " +
			// Hex.encodeHexString(raw_id));

			uuid = UUID.nameUUIDFromBytes(raw_id);

			// logger.info("connection index uuid: " + uuid);

		} catch (IOException e) {
			logger.error("Unable to create connection UUID", e);
		}
		return uuid;
	}

	/**
	 * @param connectingEntityId
	 * @param connectionType
	 * @param connectedEntityType
	 * @return connection index id
	 */
	public static UUID getIndexId(UUID connectingEntityId,
			String connectionType, String connectedEntityType) {
		return getIndexId(connectingEntityId, null, null, connectionType,
				connectedEntityType);
	}

	/**
	 * @param connectingEntityId
	 * @param pairedConnectionType
	 * @param pairedConnectingEntityId
	 * @param connectionType
	 * @param connectedEntityType
	 * @return connection index id
	 */
	public static UUID getIndexId(UUID connectingEntityId,
			String pairedConnectionType, UUID pairedConnectingEntityId,
			String connectionType, String connectedEntityType) {

		EntityRef connectingEntity = ref(connectingEntityId);

		ConnectedEntityRef[] pairedConnections = getConnections(new ConnectedEntityRefImpl(
				pairedConnectionType, null, pairedConnectingEntityId));

		return getIndexId(connectingEntity, connectionType,
				connectedEntityType, pairedConnections);
	}

	public static UUID getIndexId(EntityRef connectingEntity,
			String connectionType, String connectedEntityType,
			ConnectedEntityRef... pairedConnections) {

		UUID uuid = null;
		try {

			if (connectionsNull(pairedConnections)
					&& ((connectionType == null) && (connectedEntityType == null))) {
				return connectingEntity.getUuid();
			}

			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(
					16 + (32 * pairedConnections.length));

			byteStream.write(uuidToBytesNullOk(connectingEntity.getUuid()));

			for (ConnectedEntityRef connection : pairedConnections) {
				String type = connection.getConnectionType();
				UUID id = connection.getUuid();

				byteStream.write(ascii(StringUtils.lowerCase(type)));
				byteStream.write(uuidToBytesNullOk(id));

			}

			if (connectionType == null) {
				connectionType = NULL_ENTITY_TYPE;
			}
			if (connectedEntityType == null) {
				connectedEntityType = NULL_ENTITY_TYPE;
			}

			byteStream.write(ascii(StringUtils.lowerCase(connectionType)));
			byteStream.write(ascii(StringUtils.lowerCase(connectedEntityType)));

			byte[] raw_id = byteStream.toByteArray();

			logger.info("raw connection index id: "
					+ Hex.encodeHexString(raw_id));

			uuid = UUID.nameUUIDFromBytes(raw_id);

			logger.info("connection index uuid: " + uuid);

		} catch (IOException e) {
			logger.error("Unable to create connection index UUID", e);
		}
		return uuid;

	}

	/**
	 * @param variant
	 * @param connectingEntityId
	 * @param pairedConnectionType
	 * @param pairedConnectingEntityId
	 * @param connectionType
	 * @param connectedEntityType
	 * @return connection index id
	 */
	public static UUID getIndexId(int variant, UUID connectingEntityId,
			String pairedConnectionType, UUID pairedConnectingEntityId,
			String connectionType, String connectedEntityType) {

		EntityRef connectingEntity = ref(connectingEntityId);

		ConnectedEntityRef[] pairedConnections = getConnections(new ConnectedEntityRefImpl(
				pairedConnectionType, null, pairedConnectingEntityId));

		return getIndexId(variant, connectingEntity, connectionType,
				connectedEntityType, pairedConnections);
	}

	public static UUID getIndexId(int variant, EntityRef connectingEntity,
			String connectionType, String connectedEntityType,
			ConnectedEntityRef... pairedConnections) {

		switch (variant) {

		case ALL:
			if (connectionsNull(pairedConnections)) {
				return connectingEntity.getUuid();
			} else {
				return getIndexId(connectingEntity, null, null,
						pairedConnections);
			}

		case BY_ENTITY_TYPE:
			return getIndexId(connectingEntity, null, connectedEntityType,
					pairedConnections);

		case BY_CONNECTION_TYPE:
			return getIndexId(connectingEntity, connectionType, null,
					pairedConnections);

		case BY_CONNECTION_AND_ENTITY_TYPE:
			return getIndexId(connectingEntity, connectionType,
					connectedEntityType, pairedConnections);
		}

		return connectingEntity.getUuid();
	}

	/**
	 * @param connectingEntityId
	 * @param connectionType
	 * @param connectedEntityType
	 * @return index ids
	 */
	public static UUID[] getIndexIds(UUID connectingEntityId,
			String connectionType, String connectedEntityType) {
		return getIndexIds(connectingEntityId, null, null, connectionType,
				connectedEntityType);
	}

	/**
	 * @param connectingEntityId
	 * @param pairedConnectionType
	 * @param pairedConnectingEntityId
	 * @param connectionType
	 * @param connectedEntityType
	 * @return index ids
	 */
	public static UUID[] getIndexIds(UUID connectingEntityId,
			String pairedConnectionType, UUID pairedConnectingEntityId,
			String connectionType, String connectedEntityType) {

		UUID[] variants = new UUID[4];

		for (int i = 0; i < 4; i++) {
			variants[i] = getIndexId(i, connectingEntityId,
					pairedConnectionType, pairedConnectingEntityId,
					connectionType, connectedEntityType);
		}

		return variants;
	}

	public static UUID[] getIndexIds(EntityRef connectingEntity,
			String connectionType, String connectedEntityType,
			ConnectedEntityRef... pairedConnections) {

		UUID[] variants = new UUID[4];

		for (int i = 0; i < 4; i++) {
			variants[i] = getIndexId(i, connectingEntity, connectionType,
					connectedEntityType, pairedConnections);
		}

		return variants;
	}

	private static List<String> columns = null;

	/**
	 * @return columns for retrieving CF row
	 */
	public static List<String> getColumnNames() {

		if (ConnectionRefImpl.columns != null) {
			return ConnectionRefImpl.columns;
		}

		List<String> columns = new ArrayList<String>();
		columns.add(CONNECTING_ENTITY_TYPE);
		columns.add(CONNECTING_ENTITY_ID);

		for (int i = 0; i < MAX_LINKS; i++) {
			columns.add(PAIRED_CONNECTION_TYPE + i);
			columns.add(PAIRED_CONNECTING_ENTITY_TYPE + i);
			columns.add(PAIRED_CONNECTING_ENTITY_ID + i);
		}

		columns.add(CONNECTION_TYPE);
		columns.add(CONNECTED_ENTITY_TYPE);
		columns.add(CONNECTED_ENTITY_ID);
		ConnectionRefImpl.columns = columns;

		return columns;
	}

	private static Set<String> columnSet = null;

	public static Set<String> getColumnNamesSet() {

		if (ConnectionRefImpl.columnSet != null) {
			return ConnectionRefImpl.columnSet;
		}

		Set<String> columnSet = new LinkedHashSet<String>(getColumnNames());
		ConnectionRefImpl.columnSet = columnSet;

		return columnSet;
	}

	private static List<String> idColumns = null;

	/**
	 * @return columns for retrieving CF row
	 */
	public static List<String> getIdColumnNames() {

		if (ConnectionRefImpl.idColumns != null) {
			return ConnectionRefImpl.idColumns;
		}

		List<String> columns = new ArrayList<String>();
		columns.add(CONNECTING_ENTITY_ID);

		for (int i = 0; i < MAX_LINKS; i++) {
			columns.add(PAIRED_CONNECTING_ENTITY_ID + i);
		}

		columns.add(CONNECTED_ENTITY_ID);
		ConnectionRefImpl.idColumns = columns;

		return columns;
	}

	/**
	 * @return columns for creating CF
	 */
	public static List<ColumnDef> getColumnDefinitions() {
		List<ColumnDef> columns = new ArrayList<ColumnDef>();

		columns.add(newColumnDef(CONNECTING_ENTITY_TYPE, "BytesType"));
		columns.add(newColumnDef(CONNECTING_ENTITY_ID));

		for (int i = 0; i < MAX_LINKS; i++) {
			columns.add(newColumnDef(PAIRED_CONNECTION_TYPE + i, "BytesType"));

			columns.add(newColumnDef(PAIRED_CONNECTING_ENTITY_TYPE + i,
					"BytesType"));
			columns.add(newColumnDef(PAIRED_CONNECTING_ENTITY_ID + i));
		}

		columns.add(newColumnDef(CONNECTION_TYPE, "BytesType"));

		columns.add(newColumnDef(CONNECTED_ENTITY_TYPE, "BytesType"));
		columns.add(newColumnDef(CONNECTED_ENTITY_ID));

		return columns;
	}

	private static ColumnDef newColumnDef(String column_name) {
		return newColumnDef(column_name, UUID_COMPARATOR);
	}

	public static String getIndexes() {
		StringBuffer s = new StringBuffer();

		s.append(CONNECTING_ENTITY_TYPE + ":BytesType,");
		s.append(CONNECTING_ENTITY_ID);
		s.append(",");

		for (int i = 0; i < MAX_LINKS; i++) {
			s.append(PAIRED_CONNECTION_TYPE);
			s.append(i);
			s.append(":BytesType,");

			s.append(PAIRED_CONNECTING_ENTITY_TYPE);
			s.append(i);
			s.append(":BytesType,");

			s.append(PAIRED_CONNECTING_ENTITY_ID);
			s.append(i);
			s.append(",");
		}

		s.append(CONNECTION_TYPE);
		s.append(":BytesType,");

		s.append(CONNECTED_ENTITY_TYPE);
		s.append(":BytesType,");

		s.append(CONNECTED_ENTITY_ID);

		return s.toString();
	}

	private static ColumnDef newColumnDef(String column_name, String comparer) {
		ColumnDef cd = new ColumnDef(bytebuffer(column_name), comparer);
		cd.setIndex_name(column_name);
		cd.setIndex_type(IndexType.KEYS);
		return cd;
	}

}
