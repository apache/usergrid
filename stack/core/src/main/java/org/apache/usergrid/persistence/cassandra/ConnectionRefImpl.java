/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.cassandra;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import static org.apache.usergrid.persistence.SimpleEntityRef.ref;
import static org.apache.usergrid.utils.ConversionUtils.ascii;
import static org.apache.usergrid.utils.ConversionUtils.uuidToBytesNullOk;


/** @author edanuff */
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
    public static final UUID NULL_ID = new UUID( 0, 0 );

    private static final Logger logger = LoggerFactory.getLogger( ConnectionRefImpl.class );


    public static final String CONNECTION_ENTITY_TYPE = "Connection";
    public static final String CONNECTION_ENTITY_CONNECTION_TYPE = "connection";


    private final EntityRef connectingEntity;

    private final List<ConnectedEntityRef> pairedConnections;

    private final ConnectedEntityRef connectedEntity;


    /**
     *
     */
    public ConnectionRefImpl() {
        connectingEntity = SimpleEntityRef.ref();
        pairedConnections = Collections.emptyList();
        connectedEntity = new ConnectedEntityRefImpl();
    }


    /**
     * @param connectingEntityType
     * @param connectingEntityId
     * @param connectionType
     * @param connectedEntityType
     * @param connectedEntityId
     */
    public ConnectionRefImpl( String connectingEntityType, UUID connectingEntityId, String connectionType,
                              String connectedEntityType, UUID connectedEntityId ) {

        connectingEntity = ref( connectingEntityType, connectingEntityId );

        pairedConnections = Collections.emptyList();

        connectedEntity = new ConnectedEntityRefImpl( connectionType, connectedEntityType, connectedEntityId );
    }


    /** Create a connection from the source to the target entity */
    public ConnectionRefImpl( EntityRef source, String connectionType, EntityRef target ) {

        this.connectingEntity = ref( source );

        pairedConnections = Collections.emptyList();

        this.connectedEntity = new ConnectedEntityRefImpl( connectionType, target );
    }


    public ConnectionRefImpl( ConnectionRef connection ) {

        connectingEntity = connection.getSourceRefs();

        List<ConnectedEntityRef> pc = connection.getPairedConnections();
        if ( pc == null ) {
            pc = Collections.emptyList();
        }
        pairedConnections = pc;

        connectedEntity = connection.getTargetRefs();
    }


    public ConnectionRefImpl( EntityRef connectingEntity, ConnectedEntityRef... connections ) {

        this.connectingEntity = ref( connectingEntity );

        ConnectedEntityRef ce = new ConnectedEntityRefImpl();
        List<ConnectedEntityRef> pc = Collections.emptyList();
        if ( connections.length > 0 ) {

            ce = connections[connections.length - 1];

            if ( connections.length > 1 ) {
                pc = Arrays.asList( Arrays.copyOfRange( connections, 0, connections.length - 2 ) );
            }
        }
        pairedConnections = pc;
        connectedEntity = ce;
    }


    public ConnectionRefImpl( ConnectionRef connection, ConnectedEntityRef... connections ) {

        if ( connection == null ) {
            throw new NullPointerException( "ConnectionImpl constructor \'connection\' cannot be null" );
        }

        connectingEntity = connection.getSourceRefs();

        if ( connections.length > 0 ) {

            pairedConnections = new ArrayList<ConnectedEntityRef>();
            pairedConnections.addAll( connection.getPairedConnections() );
            pairedConnections.add( connection.getTargetRefs() );

            connectedEntity = connections[connections.length - 1];

            if ( connections.length > 1 ) {
                pairedConnections
                        .addAll( Arrays.asList( Arrays.copyOfRange( connections, 0, connections.length - 2 ) ) );
            }
        }
        else {
            pairedConnections = new ArrayList<ConnectedEntityRef>();
            connectedEntity = new ConnectedEntityRefImpl();
        }
    }


    public ConnectionRefImpl( EntityRef connectingEntity, List<ConnectedEntityRef> pairedConnections,
                              ConnectedEntityRef connectedEntity ) {
        this.connectingEntity = connectingEntity;
        this.pairedConnections = pairedConnections;
        this.connectedEntity = connectedEntity;
    }


    public UUID getSearchIndexId() {
        return null;
    }


    public String getSearchConnectionType() {
        return null;
    }


    public String getSearchResultType() {
        return null;
    }


    public String getSearchIndexName() {
        return null;
    }


    @Override
    public EntityRef getSourceRefs() {
        return connectingEntity;
    }


    /**
     * @return
     */
    public String getConnectingEntityType() {
        if ( connectingEntity == null ) {
            return null;
        }
        return connectingEntity.getType();
    }


    /**
     * @return
     */
    public UUID getConnectingEntityId() {
        if ( connectingEntity == null ) {
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

        if ( ( pairedConnections != null ) && ( pairedConnections.size() > 0 ) ) {
            pairedConnection = pairedConnections.get( 0 );
        }

        return pairedConnection;
    }


    public UUID getFirstPairedConnectedEntityId() {
        ConnectedEntityRef pairedConnection = getFirstPairedConnection();
        if ( pairedConnection != null ) {
            return pairedConnection.getUuid();
        }
        return null;
    }


    public String getFirstPairedConnectedEntityType() {
        ConnectedEntityRef pairedConnection = getFirstPairedConnection();
        if ( pairedConnection != null ) {
            return pairedConnection.getType();
        }
        return null;
    }


    public String getFirstPairedConnectionType() {
        ConnectedEntityRef pairedConnection = getFirstPairedConnection();
        if ( pairedConnection != null ) {
            return pairedConnection.getConnectionType();
        }
        return null;
    }


    @Override
    public ConnectedEntityRef getTargetRefs() {
        return connectedEntity;
    }


    /**
     * @return
     */
    @Override
    public String getConnectionType() {
        if ( connectedEntity == null ) {
            return null;
        }
        return connectedEntity.getConnectionType();
    }


    /**
     * @return
     */
    public String getConnectedEntityType() {
        if ( connectedEntity == null ) {
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


    /** @return connection id */
    @Override
    public UUID getUuid() {
        if ( id == null ) {
            List<ConnectedEntityRef> var = getPairedConnections();
            id = getId( getSourceRefs(), getTargetRefs(),
                    var.toArray(new ConnectedEntityRef[var.size()]));
        }
        return id;
    }


    @Override
    public String getType() {
        return CONNECTION_ENTITY_TYPE;
    }


    @Override
    public Id asId() {
        return new SimpleId(id,  CONNECTION_ENTITY_TYPE );
    }


    public UUID getIndexId() {
        return getIndexId( getSourceRefs(), getConnectionType(), getConnectedEntityType(),
                pairedConnections.toArray(new ConnectedEntityRef[pairedConnections.size()]));
    }


    public UUID getConnectingIndexId() {
        return getIndexId( getSourceRefs(), getConnectionType(), null,
                pairedConnections.toArray(new ConnectedEntityRef[pairedConnections.size()]));
    }


    public ConnectionRefImpl getConnectionToConnectionEntity() {
        return new ConnectionRefImpl( getSourceRefs(),
                new ConnectedEntityRefImpl( CONNECTION_ENTITY_CONNECTION_TYPE, CONNECTION_ENTITY_TYPE, getUuid() ) );
    }


    /** @return index ids */
    public UUID[] getIndexIds() {

        List<ConnectedEntityRef> var = getPairedConnections();
        return getIndexIds( getSourceRefs(), getTargetRefs().getConnectionType(),
                getTargetRefs().getType(), var.toArray(new ConnectedEntityRef[var.size()]));
    }


    static String typeOrDefault( String type ) {
        if ( ( type == null ) || ( type.length() == 0 ) ) {
            return NULL_ENTITY_TYPE;
        }
        return type;
    }


    static UUID idOrDefault( UUID uuid ) {
        if ( uuid == null ) {
            return NULL_ID;
        }
        return uuid;
    }


    public static boolean connectionsNull( ConnectedEntityRef... pairedConnections ) {
        if ( ( pairedConnections == null ) || ( pairedConnections.length == 0 ) ) {
            return true;
        }

        for ( ConnectedEntityRef pairedConnection : pairedConnections ) {
            if ( pairedConnection == null || pairedConnection.getUuid() == null || pairedConnection.getUuid().equals(
                    NULL_ID ) ) {
                return true;
            }
        }

        return false;
    }


    public static ConnectedEntityRef[] getConnections( ConnectedEntityRef... connections ) {
        return connections;
    }


    public static List<ConnectedEntityRef> getConnectionsList( ConnectedEntityRef... connections ) {
        return Arrays.asList( connections );
    }


    /** @return connection id */
    public static UUID getId( UUID connectingEntityId, String connectionType, UUID connectedEntityId ) {
        return getId( connectingEntityId, null, null, connectionType, connectedEntityId );
    }


    /**
     * Connection id is constructed from packed structure of properties strings are truncated to 16 ascii bytes.
     * Connection id is now MD5'd into a UUID via UUID.nameUUIDFromBytes() so, technically string concatenation could be
     * used prior to MD5
     *
     * @return connection id
     */
    public static UUID getId( UUID connectingEntityId, String pairedConnectionType, UUID pairedConnectingEntityId,
                              String connectionType, UUID connectedEntityId ) {

        EntityRef connectingEntity = ref( connectingEntityId );

        ConnectedEntityRef[] pairedConnections =
                getConnections( new ConnectedEntityRefImpl( pairedConnectionType, null, pairedConnectingEntityId ) );

        ConnectedEntityRef connectedEntity = new ConnectedEntityRefImpl( connectionType, null, connectedEntityId );

        return getId( connectingEntity, connectedEntity, pairedConnections );
    }


    public static UUID getId( EntityRef connectingEntity, ConnectedEntityRef connectedEntity,
                              ConnectedEntityRef... pairedConnections ) {
        UUID uuid = null;
        try {

            if ( connectionsNull( pairedConnections ) && connectionsNull( connectedEntity ) ) {
                return connectingEntity.getUuid();
            }

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream( 16 + ( 32 * pairedConnections.length ) );

            byteStream.write( uuidToBytesNullOk( connectingEntity.getUuid() ) );

            for ( ConnectedEntityRef connection : pairedConnections ) {
                String connectionType = connection.getConnectionType();
                UUID connectedEntityID = connection.getUuid();

                byteStream.write( ascii( StringUtils.lowerCase( connectionType ) ) );
                byteStream.write( uuidToBytesNullOk( connectedEntityID ) );
            }

            String connectionType = connectedEntity.getConnectionType();
            if ( connectionType == null ) {
                connectionType = NULL_ENTITY_TYPE;
            }

            UUID connectedEntityID = connectedEntity.getUuid();

            byteStream.write( ascii( StringUtils.lowerCase( connectionType ) ) );
            byteStream.write( uuidToBytesNullOk( connectedEntityID ) );

            byte[] raw_id = byteStream.toByteArray();

            // logger.info("raw connection index id: " +
            // Hex.encodeHexString(raw_id));

            uuid = UUID.nameUUIDFromBytes( raw_id );

            // logger.info("connection index uuid: " + uuid);

        }
        catch ( IOException e ) {
            logger.error( "Unable to create connection UUID", e );
        }
        return uuid;
    }


    /** @return connection index id */
    public static UUID getIndexId( UUID connectingEntityId, String connectionType, String connectedEntityType ) {
        return getIndexId( connectingEntityId, null, null, connectionType, connectedEntityType );
    }


    /** @return connection index id */
    public static UUID getIndexId( UUID connectingEntityId, String pairedConnectionType, UUID pairedConnectingEntityId,
                                   String connectionType, String connectedEntityType ) {

        EntityRef connectingEntity = ref( connectingEntityId );

        ConnectedEntityRef[] pairedConnections =
                getConnections( new ConnectedEntityRefImpl( pairedConnectionType, null, pairedConnectingEntityId ) );

        return getIndexId( connectingEntity, connectionType, connectedEntityType, pairedConnections );
    }


    public static UUID getIndexId( EntityRef connectingEntity, String connectionType, String connectedEntityType,
                                   ConnectedEntityRef... pairedConnections ) {

        UUID uuid = null;
        try {

            if ( connectionsNull( pairedConnections ) && ( ( connectionType == null ) && ( connectedEntityType
                    == null ) ) ) {
                return connectingEntity.getUuid();
            }

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream( 16 + ( 32 * pairedConnections.length ) );

            byteStream.write( uuidToBytesNullOk( connectingEntity.getUuid() ) );

            for ( ConnectedEntityRef connection : pairedConnections ) {
                String type = connection.getConnectionType();
                UUID id = connection.getUuid();

                byteStream.write( ascii( StringUtils.lowerCase( type ) ) );
                byteStream.write( uuidToBytesNullOk( id ) );
            }

            if ( connectionType == null ) {
                connectionType = NULL_ENTITY_TYPE;
            }
            if ( connectedEntityType == null ) {
                connectedEntityType = NULL_ENTITY_TYPE;
            }

            byteStream.write( ascii( StringUtils.lowerCase( connectionType ) ) );
            byteStream.write( ascii( StringUtils.lowerCase( connectedEntityType ) ) );

            byte[] raw_id = byteStream.toByteArray();

            logger.info( "raw connection index id: " + Hex.encodeHexString( raw_id ) );

            uuid = UUID.nameUUIDFromBytes( raw_id );

            logger.info( "connection index uuid: " + uuid );
        }
        catch ( IOException e ) {
            logger.error( "Unable to create connection index UUID", e );
        }
        return uuid;
    }


    /** @return connection index id */
    public static UUID getIndexId( int variant, UUID connectingEntityId, String pairedConnectionType,
                                   UUID pairedConnectingEntityId, String connectionType, String connectedEntityType ) {

        EntityRef connectingEntity = ref( connectingEntityId );

        ConnectedEntityRef[] pairedConnections =
                getConnections( new ConnectedEntityRefImpl( pairedConnectionType, null, pairedConnectingEntityId ) );

        return getIndexId( variant, connectingEntity, connectionType, connectedEntityType, pairedConnections );
    }


    public static UUID getIndexId( int variant, EntityRef connectingEntity, String connectionType,
                                   String connectedEntityType, ConnectedEntityRef... pairedConnections ) {

        switch ( variant ) {

            case ALL:
                if ( connectionsNull( pairedConnections ) ) {
                    return connectingEntity.getUuid();
                }
                else {
                    return getIndexId( connectingEntity, null, null, pairedConnections );
                }

            case BY_ENTITY_TYPE:
                return getIndexId( connectingEntity, null, connectedEntityType, pairedConnections );

            case BY_CONNECTION_TYPE:
                return getIndexId( connectingEntity, connectionType, null, pairedConnections );

            case BY_CONNECTION_AND_ENTITY_TYPE:
                return getIndexId( connectingEntity, connectionType, connectedEntityType, pairedConnections );
        }

        return connectingEntity.getUuid();
    }


    /** @return index ids */
    public static UUID[] getIndexIds( UUID connectingEntityId, String connectionType, String connectedEntityType ) {
        return getIndexIds( connectingEntityId, null, null, connectionType, connectedEntityType );
    }


    /** @return index ids */
    public static UUID[] getIndexIds( UUID connectingEntityId, String pairedConnectionType,
                                      UUID pairedConnectingEntityId, String connectionType,
                                      String connectedEntityType ) {

        UUID[] variants = new UUID[4];

        for ( int i = 0; i < 4; i++ ) {
            variants[i] =
                    getIndexId( i, connectingEntityId, pairedConnectionType, pairedConnectingEntityId, connectionType,
                            connectedEntityType );
        }

        return variants;
    }


    public static UUID[] getIndexIds( EntityRef connectingEntity, String connectionType, String connectedEntityType,
                                      ConnectedEntityRef... pairedConnections ) {

        UUID[] variants = new UUID[4];

        for ( int i = 0; i < 4; i++ ) {
            variants[i] = getIndexId( i, connectingEntity, connectionType, connectedEntityType, pairedConnections );
        }

        return variants;
    }
}
