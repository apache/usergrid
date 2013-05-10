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

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.commons.lang.math.NumberUtils.toDouble;
import static org.usergrid.persistence.Schema.DICTIONARY_GEOCELL;
import static org.usergrid.persistence.Schema.INDEX_CONNECTIONS;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.logBatchOperation;
import static org.usergrid.utils.ClassUtils.cast;
import static org.usergrid.utils.ConversionUtils.bytebuffer;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterLast;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Id;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Results.Level;
import org.usergrid.utils.UUIDUtils;

import com.beoui.geocell.GeocellManager;
import com.beoui.geocell.GeocellQueryEngine;
import com.beoui.geocell.annotations.Latitude;
import com.beoui.geocell.annotations.Longitude;
import com.beoui.geocell.model.GeocellQuery;
import com.beoui.geocell.model.Point;

public class GeoIndexManager {

    private static final Logger logger = LoggerFactory
            .getLogger(GeoIndexManager.class);

    /**
     * We only ever go down to max resolution of 9 because we use bucket
     * hashing. Every level divides the region by 1/16. Our original "box" is 90
     * degrees by 45 degrees. We therefore have 90 * (1/16)^(r-1) and 45 *
     * (1/16)^(r-1) for our size where r is the largest bucket resolution. This
     * gives us a size of 90 deg => 0.0000000209547 deg = .2cm and 45 deg =>
     * 0.00000001047735 deg = .1 cm
     */
    private static final int MAX_RESOLUTION = 9;

    public static class EntityLocationRef implements EntityRef {

        @Id
        private UUID uuid;

        private String type;

        private UUID timestampUuid = UUIDUtils.newTimeUUID();

        @Latitude
        private double latitude;

        @Longitude
        private double longitude;

        public EntityLocationRef() {
        }

        public EntityLocationRef(EntityRef entity, double latitude,
                double longitude) {
            this(entity.getType(), entity.getUuid(), latitude, longitude);
        }

        public EntityLocationRef(String type, UUID uuid, double latitude,
                double longitude) {
            this.type = type;
            this.uuid = uuid;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public EntityLocationRef(EntityRef entity, UUID timestampUuid,
                double latitude, double longitude) {
            this(entity.getType(), entity.getUuid(), timestampUuid, latitude,
                    longitude);
        }

        public EntityLocationRef(String type, UUID uuid, UUID timestampUuid,
                double latitude, double longitude) {
            this.type = type;
            this.uuid = uuid;
            this.timestampUuid = timestampUuid;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public EntityLocationRef(EntityRef entity, UUID timestampUuid,
                String coord) {
            this.type = entity.getType();
            this.uuid = entity.getUuid();
            this.timestampUuid = timestampUuid;
            this.latitude = toDouble(stringOrSubstringBeforeFirst(coord, ','));
            this.longitude = toDouble(stringOrSubstringAfterLast(coord, ','));
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public UUID getTimestampUuid() {
            return timestampUuid;
        }

        public void setTimestampUuid(UUID timestampUuid) {
            this.timestampUuid = timestampUuid;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public Point getPoint() {
            return new Point(latitude, longitude);
        }

        public DynamicComposite getColumnName() {
            return new DynamicComposite(uuid, type, timestampUuid);
        }

        public DynamicComposite getColumnValue() {
            return new DynamicComposite(latitude, longitude);
        }

        public long getTimestampInMicros() {
            return UUIDUtils.getTimestampInMicros(timestampUuid);
        }

        public long getTimestampInMillis() {
            return UUIDUtils.getTimestampInMillis(timestampUuid);
        }

    }

    EntityManagerImpl em;
    CassandraService cass;

    public GeoIndexManager() {
    }

    public GeoIndexManager init(EntityManagerImpl em) {
        this.em = em;
        this.cass = em.getCass();
        return this;
    }

    public static List<EntityLocationRef> getLocationIndexEntries(
            List<HColumn<ByteBuffer, ByteBuffer>> columns) {
        List<EntityLocationRef> entries = new ArrayList<EntityLocationRef>();
        if (columns != null) {
            EntityLocationRef prevEntry = null;
            for (HColumn<ByteBuffer, ByteBuffer> column : columns) {
                DynamicComposite composite = DynamicComposite
                        .fromByteBuffer(column.getName());
                UUID uuid = composite.get(0, UUIDSerializer.get());
                String type = composite.get(1, StringSerializer.get());
                UUID timestampUuid = composite.get(2, UUIDSerializer.get());
                composite = DynamicComposite.fromByteBuffer(column.getValue());
                Double latitude = composite.get(0, DoubleSerializer.get());
                Double longitude = composite.get(1, DoubleSerializer.get());
                if ((prevEntry != null) && uuid.equals(prevEntry.getUuid())) {
                    prevEntry.setLatitude(latitude);
                    prevEntry.setLongitude(longitude);
                } else {
                    prevEntry = new EntityLocationRef(type, uuid,
                            timestampUuid, latitude, longitude);
                    entries.add(prevEntry);
                }
            }
        }
        return entries;
    }

    public static List<EntityLocationRef> mergeLocationEntries(
            List<EntityLocationRef> list, List<EntityLocationRef>... lists) {
        if ((lists == null) || (lists.length == 0)) {
            return list;
        }
        LinkedHashMap<UUID, EntityLocationRef> merge = new LinkedHashMap<UUID, EntityLocationRef>();
        for (EntityLocationRef loc : list) {
            merge.put(loc.getUuid(), loc);
        }
        for (List<EntityLocationRef> l : lists) {
            for (EntityLocationRef loc : l) {
                if (merge.containsKey(loc.getUuid())) {
                    if (UUIDUtils.compare(loc.getTimestampUuid(),
                            merge.get(loc.getUuid()).getTimestampUuid()) > 0) {
                        merge.put(loc.getUuid(), loc);
                    }
                } else {
                    merge.put(loc.getUuid(), loc);
                }
            }
        }
        return new ArrayList<EntityLocationRef>(merge.values());
    }

    @SuppressWarnings("unchecked")
    public List<EntityLocationRef> query(Object key,
            List<String> curGeocellsUnique, UUID startResult, int count,
            boolean reversed) {

        List<EntityLocationRef> list = new ArrayList<EntityLocationRef>();

        List<Object> keys = new ArrayList<Object>();
        
        IndexBucketLocator locator = em.getIndexBucketLocator();
        UUID appId = em.getApplicationId();
        
        
        for (String geoCell : curGeocellsUnique) {
            
            //add buckets for each geoCell
            
            for(String indexBucket: locator.getBuckets(appId, IndexType.GEO, geoCell)){
                keys.add(key(key, DICTIONARY_GEOCELL, geoCell, indexBucket));
            }
        }
        
        
        try {
            Map<ByteBuffer, List<HColumn<ByteBuffer, ByteBuffer>>> rows = cass
                    .multiGetColumns(
                            cass.getApplicationKeyspace(em.getApplicationId()),
                            ENTITY_INDEX, keys, startResult, null, count,
                            reversed);

            for (List<HColumn<ByteBuffer, ByteBuffer>> columns : rows.values()) {
                List<EntityLocationRef> entries = getLocationIndexEntries(columns);
                list = mergeLocationEntries(list, entries);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public Results proximitySearchCollection(final EntityRef headEntity,
            final String collectionName, final String propertyName,
            Point center, double maxDistance, final UUID startResult,
            final int count, final boolean reversed, Level level)
            throws Exception {

        GeocellQueryEngine gqe = new GeocellQueryEngine() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> List<T> query(GeocellQuery baseQuery,
                    List<String> curGeocellsUnique, Class<T> entityClass) {
                return (List<T>) GeoIndexManager.this
                        .query(key(headEntity.getUuid(), collectionName,
                                propertyName), curGeocellsUnique, startResult,
                                count, reversed);
            }
        };

        return doSearch(center, maxDistance, gqe, count, level);
    }

    public Results proximitySearchConnections(final UUID connectionIndexId,
            final String propertyName, Point center, double maxDistance,
            final UUID startResult, final int count, final boolean reversed,
            Level level) throws Exception {

        GeocellQueryEngine gqe = new GeocellQueryEngine() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> List<T> query(GeocellQuery baseQuery,
                    List<String> curGeocellsUnique, Class<T> entityClass) {
                return (List<T>) GeoIndexManager.this
                        .query(key(connectionIndexId, INDEX_CONNECTIONS,
                                propertyName), curGeocellsUnique, startResult,
                                count, reversed);
            }
        };

        return doSearch(center, maxDistance, gqe, count, level);
    }

    private Results doSearch(Point center, double maxDistance,
            GeocellQueryEngine gqe, int count, Level level) throws Exception {
        List<EntityLocationRef> locations = null;

        GeocellQuery baseQuery = new GeocellQuery();
        try {
            locations = GeocellManager.proximitySearch(center, count,
                    maxDistance, EntityLocationRef.class, baseQuery, gqe,
                    MAX_RESOLUTION);
        } catch (Exception e) {
            e.printStackTrace();
        }

        @SuppressWarnings("unchecked")
        Results results = Results
                .fromRefList((List<EntityRef>) cast(locations));
        return results;
    }

    public static Mutator<ByteBuffer> addLocationEntryInsertionToMutator(
            Mutator<ByteBuffer> m, Object key, EntityLocationRef entry) {

        DynamicComposite columnName = entry.getColumnName();
        DynamicComposite columnValue = entry.getColumnValue();
        long ts = entry.getTimestampInMicros();

        logBatchOperation("Insert", ENTITY_INDEX, key, columnName, columnValue,
                ts);

        HColumn<ByteBuffer, ByteBuffer> column = createColumn(
                columnName.serialize(), columnValue.serialize(), ts,
                ByteBufferSerializer.get(), ByteBufferSerializer.get());
        m.addInsertion(bytebuffer(key), ENTITY_INDEX.toString(), column);

        return m;
    }

    private static Mutator<ByteBuffer> batchAddConnectionIndexEntries(
            Mutator<ByteBuffer> m, IndexBucketLocator locator, UUID appId, String propertyName, String geoCell,
            UUID[] index_keys, ByteBuffer columnName, ByteBuffer columnValue,
            long timestamp) {

        // entity_id,prop_name
        Object property_index_key = key(index_keys[ConnectionRefImpl.ALL],
                INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell, locator.getBucket(appId, IndexType.GEO, index_keys[ConnectionRefImpl.ALL], geoCell));

        // entity_id,entity_type,prop_name
        Object entity_type_prop_index_key = key(
                index_keys[ConnectionRefImpl.BY_ENTITY_TYPE],
                INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell, locator.getBucket(appId, IndexType.GEO, index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], geoCell));

        // entity_id,connection_type,prop_name
        Object connection_type_prop_index_key = key(
                index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE],
                INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell, locator.getBucket(appId, IndexType.GEO, index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], geoCell));

        // entity_id,connection_type,entity_type,prop_name
        Object connection_type_and_entity_type_prop_index_key = key(
                index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE],
                INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell, locator.getBucket(appId, IndexType.GEO, index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], geoCell));


        // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
        addInsertToMutator(m, ENTITY_INDEX, property_index_key, columnName,
                columnValue, timestamp);

        // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
        addInsertToMutator(m, ENTITY_INDEX, entity_type_prop_index_key,
                columnName, columnValue, timestamp);

        // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
        addInsertToMutator(m, ENTITY_INDEX, connection_type_prop_index_key,
                columnName, columnValue, timestamp);

        // composite(property_value,connected_entity_id,entry_timestamp)
        addInsertToMutator(m, ENTITY_INDEX,
                connection_type_and_entity_type_prop_index_key, columnName,
                columnValue, timestamp);

        return m;
    }

    public static void batchStoreLocationInConnectionsIndex(
            Mutator<ByteBuffer> m, IndexBucketLocator locator, UUID appId, UUID[] index_keys, String propertyName,
            EntityLocationRef location) {

        Point p = location.getPoint();
        List<String> cells = GeocellManager.generateGeoCell(p);

        ByteBuffer columnName = location.getColumnName().serialize();
        ByteBuffer columnValue = location.getColumnValue().serialize();
        long ts = location.getTimestampInMicros();
        for (String cell : cells) {
            batchAddConnectionIndexEntries(m, locator, appId, propertyName, cell, index_keys,
                    columnName, columnValue, ts);
        }

        logger.info("Geocells to be saved for Point(" + location.latitude + ","
                + location.longitude + ") are: " + cells);
    }

    private static Mutator<ByteBuffer> addLocationEntryDeletionToMutator(
            Mutator<ByteBuffer> m, Object key, EntityLocationRef entry) {

        DynamicComposite columnName = entry.getColumnName();
        long ts = entry.getTimestampInMicros();

        logBatchOperation("Delete", ENTITY_INDEX, key, columnName, null, ts);

        m.addDeletion(bytebuffer(key), ENTITY_INDEX.toString(),
                columnName.serialize(), ByteBufferSerializer.get(), ts + 1);

        return m;
    }

    private static Mutator<ByteBuffer> batchDeleteConnectionIndexEntries(
            Mutator<ByteBuffer> m, IndexBucketLocator locator, UUID appId, String propertyName, String geoCell,
            UUID[] index_keys, ByteBuffer columnName, long timestamp) {

        // entity_id,prop_name
        Object property_index_key = key(index_keys[ConnectionRefImpl.ALL],
                INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell, locator.getBucket(appId, IndexType.GEO, index_keys[ConnectionRefImpl.ALL], geoCell));

        // entity_id,entity_type,prop_name
        Object entity_type_prop_index_key = key(
                index_keys[ConnectionRefImpl.BY_ENTITY_TYPE],
                INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell, locator.getBucket(appId, IndexType.GEO, index_keys[ConnectionRefImpl.BY_ENTITY_TYPE], geoCell));

        // entity_id,connection_type,prop_name
        Object connection_type_prop_index_key = key(
                index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE],
                INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell, locator.getBucket(appId, IndexType.GEO, index_keys[ConnectionRefImpl.BY_CONNECTION_TYPE], geoCell));

        // entity_id,connection_type,entity_type,prop_name
        Object connection_type_and_entity_type_prop_index_key = key(
                index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE],
                INDEX_CONNECTIONS, propertyName, DICTIONARY_GEOCELL, geoCell, locator.getBucket(appId, IndexType.GEO, index_keys[ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE], geoCell));

        // composite(property_value,connected_entity_id,connection_type,entity_type,entry_timestamp)
        m.addDeletion(bytebuffer(property_index_key), ENTITY_INDEX.toString(),
                columnName, ByteBufferSerializer.get(), timestamp);

        // composite(property_value,connected_entity_id,connection_type,entry_timestamp)
        m.addDeletion(bytebuffer(entity_type_prop_index_key),
                ENTITY_INDEX.toString(), columnName,
                ByteBufferSerializer.get(), timestamp);

        // composite(property_value,connected_entity_id,entity_type,entry_timestamp)
        m.addDeletion(bytebuffer(connection_type_prop_index_key),
                ENTITY_INDEX.toString(), columnName,
                ByteBufferSerializer.get(), timestamp);

        // composite(property_value,connected_entity_id,entry_timestamp)
        m.addDeletion(
                bytebuffer(connection_type_and_entity_type_prop_index_key),
                ENTITY_INDEX.toString(), columnName,
                ByteBufferSerializer.get(), timestamp);

        return m;
    }

    public static void batchDeleteLocationInConnectionsIndex(
            Mutator<ByteBuffer> m, IndexBucketLocator locator, UUID appId, UUID[] index_keys, String propertyName,
            EntityLocationRef location) {

        Point p = location.getPoint();
        List<String> cells = GeocellManager.generateGeoCell(p);

        ByteBuffer columnName = location.getColumnName().serialize();
        
        long ts = location.getTimestampInMicros();
        
        
        for (String cell : cells) {
            
            
            batchDeleteConnectionIndexEntries(m, locator, appId, propertyName, cell,
                    index_keys, columnName, ts);
        }

        logger.info("Geocells to be saved for Point(" + location.latitude + ","
                + location.longitude + ") are: " + cells);
    }

    public static void batchStoreLocationInCollectionIndex(
            Mutator<ByteBuffer> m, IndexBucketLocator locator, UUID appId,
            Object key, UUID entityId, EntityLocationRef location) {

        Point p = location.getPoint();
        List<String> cells = GeocellManager.generateGeoCell(p);

        for (int i = 0; i < MAX_RESOLUTION; i++) {
            String cell = cells.get(i);

            String indexBucket = locator.getBucket(appId, IndexType.GEO,
                    entityId, cell);

            addLocationEntryInsertionToMutator(m,
                    key(key, DICTIONARY_GEOCELL, cell, indexBucket), location);
        }

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Geocells to be saved for Point({},{}) are: {}",
                    new Object[] { location.latitude, location.longitude, cells });
        }
    }

    public void storeLocationInCollectionIndex(EntityRef owner,
            String collectionName, UUID entityId, String propertyName,
            EntityLocationRef location) {

        Keyspace ko = cass.getApplicationKeyspace(em.getApplicationId());
        Mutator<ByteBuffer> m = createMutator(ko, ByteBufferSerializer.get());

        batchStoreLocationInCollectionIndex(m, em.getIndexBucketLocator(),
                em.getApplicationId(),
                key(owner.getUuid(), collectionName, propertyName),
                owner.getUuid(), location);

        batchExecute(m, CassandraService.RETRY_COUNT);

    }

    public static void batchRemoveLocationFromCollectionIndex(
            Mutator<ByteBuffer> m, IndexBucketLocator locator, UUID appId,
            Object key, EntityLocationRef location) {

        Point p = location.getPoint();
        List<String> cells = GeocellManager.generateGeoCell(p);

        //delete for every bucket in every resolution
        for (int i = 0; i < MAX_RESOLUTION; i++) {

            String cell = cells.get(i);

            for (String indexBucket : locator.getBuckets(appId, IndexType.GEO, cell)) {

                addLocationEntryDeletionToMutator(m,
                        key(key, DICTIONARY_GEOCELL, cell, indexBucket), location);
            }
        }

        logger.info("Geocells to be deleted for Point(" + location.latitude
                + "," + location.longitude + ") are: " + cells);
    }

    public void removeLocationFromCollectionIndex(EntityRef owner,
            String collectionName, String propertyName,
            EntityLocationRef location) {

        Keyspace ko = cass.getApplicationKeyspace(em.getApplicationId());
        Mutator<ByteBuffer> m = createMutator(ko, ByteBufferSerializer.get());

        batchRemoveLocationFromCollectionIndex(m, em.getIndexBucketLocator(), em.getApplicationId(),
                key(owner.getUuid(), collectionName, propertyName), location);

        batchExecute(m, CassandraService.RETRY_COUNT);

    }

}
