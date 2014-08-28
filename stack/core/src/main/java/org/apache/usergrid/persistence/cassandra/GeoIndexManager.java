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


import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.IndexBucketLocator.IndexType;
import org.apache.usergrid.persistence.geo.EntityLocationRef;
import org.apache.usergrid.persistence.geo.GeocellManager;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.hector.CountingMutator;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import org.apache.usergrid.persistence.EntityManager;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_GEOCELL;
import static org.apache.usergrid.persistence.Schema.INDEX_CONNECTIONS;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.logBatchOperation;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;


public class GeoIndexManager {

    private static final Logger logger = LoggerFactory.getLogger( GeoIndexManager.class );

    /**
     * We only ever go down to max resolution of 9 because we use bucket hashing. Every level divides the region by
     * 1/16. Our original "box" is 90 degrees by 45 degrees. We therefore have 90 * (1/16)^(r-1) and 45 * (1/16)^(r-1)
     * for our size where r is the largest bucket resolution. This gives us a size of 90 deg => 0.0000000209547 deg =
     * .2cm and 45 deg => 0.00000001047735 deg = .1 cm
     */
    public static final int MAX_RESOLUTION = 9;


    EntityManager em;
    CassandraService cass;


    public GeoIndexManager() {
    }


    public GeoIndexManager init( EntityManager em ) {
        this.em = em;
        this.cass = em.getCass();
        return this;
    }


    public static Mutator<ByteBuffer> addLocationEntryInsertionToMutator( Mutator<ByteBuffer> m, Object key,
                                                                          EntityLocationRef entry ) {

        DynamicComposite columnName = entry.getColumnName();
        DynamicComposite columnValue = entry.getColumnValue();
        long ts = entry.getTimestampInMicros();

        logBatchOperation( "Insert", ENTITY_INDEX, key, columnName, columnValue, ts );

        HColumn<ByteBuffer, ByteBuffer> column =
                createColumn( columnName.serialize(), columnValue.serialize(), ts, ByteBufferSerializer.get(),
                        ByteBufferSerializer.get() );
        m.addInsertion( bytebuffer( key ), ENTITY_INDEX.toString(), column );

        return m;
    }


    private static Mutator<ByteBuffer> batchAddConnectionIndexEntries( Mutator<ByteBuffer> m,
                                                                       IndexBucketLocator locator, UUID appId,
                                                                       String propertyName, String geoCell,
                                                                       UUID[] index_keys, ByteBuffer columnName,
                                                                       ByteBuffer columnValue, long timestamp ) {

        // entity_id,prop_name
        Object property_index_key =
                key( index_keys[ConnectionRefImpl.ALL], INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell,
                        locator.getBucket( appId, IndexType.CONNECTION, index_keys[ConnectionRefImpl.ALL], geoCell ) );

        // entity_id,entity_type,prop_name
        Object entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL,
                        geoCell,
                        locator.getBucket( appId, IndexType.CONNECTION, index_keys[ConnectionRefImpl.BY_ENTITY_TYPE],
                                geoCell ) );

        // entity_id,connection_type,prop_name
        Object connection_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], INDEX_CONNECTIONS, propertyName,
                        DICTIONARY_GEOCELL, geoCell, locator.getBucket( appId, IndexType.CONNECTION,
                        index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], geoCell ) );

        // entity_id,connection_type,entity_type,prop_name
        Object connection_type_and_entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], INDEX_CONNECTIONS, propertyName,
                        DICTIONARY_GEOCELL, geoCell, locator.getBucket( appId, IndexType.CONNECTION,
                        index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], geoCell ) );

        // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
        addInsertToMutator( m, ENTITY_INDEX, property_index_key, columnName, columnValue, timestamp );

        // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
        addInsertToMutator( m, ENTITY_INDEX, entity_type_prop_index_key, columnName, columnValue, timestamp );

        // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
        addInsertToMutator( m, ENTITY_INDEX, connection_type_prop_index_key, columnName, columnValue, timestamp );

        // composite(property_value,connected_entity_id,entry_timestamp)
        addInsertToMutator( m, ENTITY_INDEX, connection_type_and_entity_type_prop_index_key, columnName, columnValue,
                timestamp );

        return m;
    }


    public static void batchStoreLocationInConnectionsIndex( Mutator<ByteBuffer> m, IndexBucketLocator locator,
                                                             UUID appId, UUID[] index_keys, String propertyName,
                                                             EntityLocationRef location ) {

        logger.debug("batchStoreLocationInConnectionsIndex");

        Point p = location.getPoint();
        List<String> cells = GeocellManager.generateGeoCell( p );

        ByteBuffer columnName = location.getColumnName().serialize();
        ByteBuffer columnValue = location.getColumnValue().serialize();
        long ts = location.getTimestampInMicros();
        for ( String cell : cells ) {
            batchAddConnectionIndexEntries( m, locator, appId, propertyName, cell, index_keys, columnName, columnValue,
                    ts );
        }

        logger.info( "Geocells to be saved for Point({} , {} ) are: {}", new Object[] {
                location.getLatitude(), location.getLongitude(), cells
        } );
    }


    private static Mutator<ByteBuffer> addLocationEntryDeletionToMutator( Mutator<ByteBuffer> m, Object key,
                                                                          EntityLocationRef entry ) {

        DynamicComposite columnName = entry.getColumnName();
        long ts = entry.getTimestampInMicros();

        logBatchOperation( "Delete", ENTITY_INDEX, key, columnName, null, ts );

        m.addDeletion( bytebuffer( key ), ENTITY_INDEX.toString(), columnName.serialize(), ByteBufferSerializer.get(),
                ts + 1 );

        return m;
    }


    private static Mutator<ByteBuffer> batchDeleteConnectionIndexEntries( Mutator<ByteBuffer> m,
                                                                          IndexBucketLocator locator, UUID appId,
                                                                          String propertyName, String geoCell,
                                                                          UUID[] index_keys, ByteBuffer columnName,
                                                                          long timestamp ) {

        // entity_id,prop_name
        Object property_index_key =
                key( index_keys[ConnectionRefImpl.ALL], INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell,
                        locator.getBucket( appId, IndexType.CONNECTION, index_keys[ConnectionRefImpl.ALL], geoCell ) );

        // entity_id,entity_type,prop_name
        Object entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL,
                        geoCell,
                        locator.getBucket( appId, IndexType.CONNECTION, index_keys[ConnectionRefImpl.BY_ENTITY_TYPE],
                                geoCell ) );

        // entity_id,connection_type,prop_name
        Object connection_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], INDEX_CONNECTIONS, propertyName,
                        DICTIONARY_GEOCELL, geoCell, locator.getBucket( appId, IndexType.CONNECTION,
                        index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], geoCell ) );

        // entity_id,connection_type,entity_type,prop_name
        Object connection_type_and_entity_type_prop_index_key =
                key( index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], INDEX_CONNECTIONS, propertyName,
                        DICTIONARY_GEOCELL, geoCell, locator.getBucket( appId, IndexType.CONNECTION,
                        index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], geoCell ) );

        // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
        m.addDeletion( bytebuffer( property_index_key ), ENTITY_INDEX.toString(), columnName,
                ByteBufferSerializer.get(), timestamp );

        // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
        m.addDeletion( bytebuffer( entity_type_prop_index_key ), ENTITY_INDEX.toString(), columnName,
                ByteBufferSerializer.get(), timestamp );

        // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
        m.addDeletion( bytebuffer( connection_type_prop_index_key ), ENTITY_INDEX.toString(), columnName,
                ByteBufferSerializer.get(), timestamp );

        // composite(property_value,connected_entity_id,entry_timestamp)
        m.addDeletion( bytebuffer( connection_type_and_entity_type_prop_index_key ), ENTITY_INDEX.toString(),
                columnName, ByteBufferSerializer.get(), timestamp );

        return m;
    }


    public static void batchDeleteLocationInConnectionsIndex( Mutator<ByteBuffer> m, IndexBucketLocator locator,
                                                              UUID appId, UUID[] index_keys, String propertyName,
                                                              EntityLocationRef location ) {

        logger.debug("batchDeleteLocationInConnectionsIndex");

        Point p = location.getPoint();
        List<String> cells = GeocellManager.generateGeoCell( p );

        ByteBuffer columnName = location.getColumnName().serialize();

        long ts = location.getTimestampInMicros();

        for ( String cell : cells ) {

            batchDeleteConnectionIndexEntries( m, locator, appId, propertyName, cell, index_keys, columnName, ts );
        }

        logger.info( "Geocells to be saved for Point({} , {} ) are: {}", new Object[] {
                location.getLatitude(), location.getLongitude(), cells
        } );
    }


    public static void batchStoreLocationInCollectionIndex( Mutator<ByteBuffer> m, IndexBucketLocator locator,
                                                            UUID appId, Object key, UUID entityId,
                                                            EntityLocationRef location ) {

        Point p = location.getPoint();
        List<String> cells = GeocellManager.generateGeoCell( p );

        for ( int i = 0; i < MAX_RESOLUTION; i++ ) {
            String cell = cells.get( i );

            String indexBucket = locator.getBucket( appId, IndexType.GEO, entityId, cell );

            addLocationEntryInsertionToMutator( m, key( key, DICTIONARY_GEOCELL, cell, indexBucket ), location );
        }

        if ( logger.isInfoEnabled() ) {
            logger.info( "Geocells to be saved for Point({},{}) are: {}", new Object[] {
                    location.getLatitude(), location.getLongitude(), cells
            } );
        }
    }


    public void storeLocationInCollectionIndex( EntityRef owner, String collectionName, UUID entityId,
                                                String propertyName, EntityLocationRef location ) {

        Keyspace ko = cass.getApplicationKeyspace( em.getApplicationId() );
        Mutator<ByteBuffer> m = CountingMutator.createFlushingMutator( ko, ByteBufferSerializer.get() );

        batchStoreLocationInCollectionIndex( m, em.getIndexBucketLocator(), em.getApplicationId(),
                key( owner.getUuid(), collectionName, propertyName ), owner.getUuid(), location );

        batchExecute( m, CassandraService.RETRY_COUNT );
    }


    public static void batchRemoveLocationFromCollectionIndex( Mutator<ByteBuffer> m, IndexBucketLocator locator,
                                                               UUID appId, Object key, EntityLocationRef location ) {

        Point p = location.getPoint();
        List<String> cells = GeocellManager.generateGeoCell( p );

        // delete for every bucket in every resolution
        for ( int i = 0; i < MAX_RESOLUTION; i++ ) {

            String cell = cells.get( i );

            for ( String indexBucket : locator.getBuckets( appId, IndexType.GEO, cell ) ) {

                addLocationEntryDeletionToMutator( m, key( key, DICTIONARY_GEOCELL, cell, indexBucket ), location );
            }
        }

        if ( logger.isInfoEnabled() ) {
            logger.info( "Geocells to be deleted for Point({},{}) are: {}", new Object[] {
                    location.getLatitude(), location.getLongitude(), cells
            } );
        }
    }


    public void removeLocationFromCollectionIndex( EntityRef owner, String collectionName, String propertyName,
                                                   EntityLocationRef location ) {

        Keyspace ko = cass.getApplicationKeyspace( em.getApplicationId() );
        Mutator<ByteBuffer> m = CountingMutator.createFlushingMutator( ko, ByteBufferSerializer.get() );

        batchRemoveLocationFromCollectionIndex( m, em.getIndexBucketLocator(), em.getApplicationId(),
                key( owner.getUuid(), collectionName, propertyName ), location );

        batchExecute( m, CassandraService.RETRY_COUNT );
    }
}
