/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.map.impl;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Using;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UTF8Type;

import org.apache.usergrid.persistence.core.astyanax.BucketScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.BucketScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.shard.ExpandingShardLocator;
import org.apache.usergrid.persistence.core.shard.StringHashUtils;
import org.apache.usergrid.persistence.map.MapScope;

import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.serializers.BooleanSerializer;
import com.netflix.astyanax.serializers.StringSerializer;


@Singleton
public class MapSerializationImpl implements MapSerialization {

    private static final String MAP_KEYS_TABLE = CQLUtils.quote("Map_Keys");
    private static final String MAP_ENTRIES_TABLE = CQLUtils.quote("Map_Entries");

    private static final MapKeySerializer KEY_SERIALIZER = new MapKeySerializer();

    private static final BucketScopedRowKeySerializer<String> MAP_KEY_SERIALIZER =
        new BucketScopedRowKeySerializer<>( KEY_SERIALIZER );


    private static final MapEntrySerializer ENTRY_SERIALIZER = new MapEntrySerializer();
    private static final ScopedRowKeySerializer<MapEntryKey> MAP_ENTRY_SERIALIZER =
        new ScopedRowKeySerializer<>( ENTRY_SERIALIZER );


    private static final BooleanSerializer BOOLEAN_SERIALIZER = BooleanSerializer.get();

    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();


    private static final StringResultsBuilderCQL STRING_RESULTS_BUILDER_CQL = new StringResultsBuilderCQL();


    /**
     * CFs where the row key contains the source node id
     */
    public static final MultiTenantColumnFamily<ScopedRowKey<MapEntryKey>, Boolean> MAP_ENTRIES =
        new MultiTenantColumnFamily<>( "Map_Entries", MAP_ENTRY_SERIALIZER, BOOLEAN_SERIALIZER );


    /**
     * CFs where the row key contains the source node id
     */
    public static final MultiTenantColumnFamily<BucketScopedRowKey<String>, String> MAP_KEYS =
        new MultiTenantColumnFamily<>( "Map_Keys", MAP_KEY_SERIALIZER, STRING_SERIALIZER );

    /**
     * Number of buckets to hash across.
     */
    private static final int[] NUM_BUCKETS = { 20 };

    /**
     * How to funnel keys for buckets
     */
    private static final Funnel<String> MAP_KEY_FUNNEL = ( key, into ) -> into.putString( key, StringHashUtils.UTF8 );


    /**
     * Locator to get us all buckets
     */
    private static final ExpandingShardLocator<String> BUCKET_LOCATOR =
        new ExpandingShardLocator<>( MAP_KEY_FUNNEL, NUM_BUCKETS );

    private final Keyspace keyspace;
    private final CassandraConfig cassandraConfig;

    private final Session session;


    @Inject
    public MapSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                 final Session session ) {
        this.keyspace = keyspace;
        this.session = session;
        this.cassandraConfig = cassandraConfig;
    }


    @Override
    public String getString( final MapScope scope, final String key ) {

        ByteBuffer value = getValueCQL( scope, key, cassandraConfig.getDataStaxReadCl() ) ;
        return value != null ? (String)DataType.text().deserialize(value,ProtocolVersion.NEWEST_SUPPORTED ): null;
    }


    @Override
    public String getStringHighConsistency( final MapScope scope, final String key ) {

        ByteBuffer value = getValueCQL( scope, key, cassandraConfig.getDataStaxReadConsistentCl() ) ;
        return value != null ? (String)DataType.text().deserialize(value,ProtocolVersion.NEWEST_SUPPORTED ): null;
    }


    @Override
    public Map<String, String> getStrings( final MapScope scope, final Collection<String> keys ) {
        return getValuesCQL( scope, keys, STRING_RESULTS_BUILDER_CQL );
    }


    @Override
    public void putString( final MapScope scope, final String key, final String value ) {

        writeStringCQL( scope, key, value, -1 );
    }


    @Override
    public void putString( final MapScope scope, final String key, final String value, final int ttl ) {

        Preconditions.checkArgument( ttl > 0, "ttl must be > than 0" );
        writeStringCQL( scope, key, value, ttl );
    }


    /**
     * Write our string index with the specified row op
     */
    private void writeStringCQL( final MapScope scope, final String key, final String value, int ttl ) {

        Preconditions.checkNotNull( scope, "mapscope is required" );
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required" );

        Statement mapEntry;
        Statement mapKey;
        if (ttl > 0){
            Using timeToLive = QueryBuilder.ttl(ttl);

            mapEntry = QueryBuilder.insertInto(MAP_ENTRIES_TABLE)
                .using(timeToLive)
                .value("key", getMapEntryPartitionKey(scope, key))
                .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
                .value("value", DataType.text().serialize(value, ProtocolVersion.NEWEST_SUPPORTED));


            final int bucket = BUCKET_LOCATOR.getCurrentBucket( key );
            mapKey = QueryBuilder.insertInto(MAP_KEYS_TABLE)
                .using(timeToLive)
                .value("key", getMapKeyPartitionKey(scope, key, bucket))
                .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
                .value("value", DataType.text().serialize(value, ProtocolVersion.NEWEST_SUPPORTED));
        }else{

            mapEntry = QueryBuilder.insertInto(MAP_ENTRIES_TABLE)
                .value("key", getMapEntryPartitionKey(scope, key))
                .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
                .value("value", DataType.text().serialize(value, ProtocolVersion.NEWEST_SUPPORTED));

            // get a bucket number for the map keys table
            final int bucket = BUCKET_LOCATOR.getCurrentBucket( key );

            mapKey = QueryBuilder.insertInto(MAP_KEYS_TABLE)
                .value("key", getMapKeyPartitionKey(scope, key, bucket))
                .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
                .value("value", DataType.text().serialize(value, ProtocolVersion.NEWEST_SUPPORTED));

        }

        session.execute(mapEntry);
        session.execute(mapKey);

    }



    @Override
    public UUID getUuid( final MapScope scope, final String key ) {

        ByteBuffer value = getValueCQL( scope, key, cassandraConfig.getDataStaxReadCl() );
        return value != null ? (UUID)DataType.uuid().deserialize(value, ProtocolVersion.NEWEST_SUPPORTED ) : null;
    }


    @Override
    public void putUuid( final MapScope scope, final String key, final UUID putUuid ) {

        Preconditions.checkNotNull( scope, "mapscope is required" );
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( putUuid, "value is required" );


        Statement mapEntry = QueryBuilder.insertInto(MAP_ENTRIES_TABLE)
            .value("key", getMapEntryPartitionKey(scope, key))
            .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
            .value("value", DataType.uuid().serialize(putUuid, ProtocolVersion.NEWEST_SUPPORTED));

        session.execute(mapEntry);


        final int bucket = BUCKET_LOCATOR.getCurrentBucket( key );
        Statement mapKey;
        mapKey = QueryBuilder.insertInto(MAP_KEYS_TABLE)
            .value("key", getMapKeyPartitionKey(scope, key, bucket))
            .value("column1", DataType.text().serialize(key, ProtocolVersion.NEWEST_SUPPORTED))
            .value("value", DataType.serializeValue(null, ProtocolVersion.NEWEST_SUPPORTED));

        session.execute(mapKey);
    }





    @Override
    public Long getLong( final MapScope scope, final String key ) {

        ByteBuffer value = getValueCQL( scope, key, cassandraConfig.getDataStaxReadCl());
        return value != null ? (Long)DataType.bigint().deserialize(value, ProtocolVersion.NEWEST_SUPPORTED ) : null;
    }


    @Override
    public void putLong( final MapScope scope, final String key, final Long value ) {

        Preconditions.checkNotNull( scope, "mapscope is required" );
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required" );

        Statement mapEntry = QueryBuilder.insertInto(MAP_ENTRIES_TABLE)
            .value("key", getMapEntryPartitionKey(scope, key))
            .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
            .value("value", DataType.bigint().serialize(value, ProtocolVersion.NEWEST_SUPPORTED));

        session.execute(mapEntry);


        final int bucket = BUCKET_LOCATOR.getCurrentBucket( key );
        Statement mapKey;
        mapKey = QueryBuilder.insertInto(MAP_KEYS_TABLE)
            .value("key", getMapKeyPartitionKey(scope, key, bucket))
            .value("column1", DataType.text().serialize(key, ProtocolVersion.NEWEST_SUPPORTED))
            .value("value", DataType.serializeValue(null, ProtocolVersion.NEWEST_SUPPORTED));

        session.execute(mapKey);
    }


    @Override
    public void delete( final MapScope scope, final String key ) {

        Statement deleteMapEntry;
        Clause equalsEntryKey = QueryBuilder.eq("key", getMapEntryPartitionKey(scope, key));
        deleteMapEntry = QueryBuilder.delete().from(MAP_ENTRIES_TABLE)
            .where(equalsEntryKey);
        session.execute(deleteMapEntry);



        // not sure which bucket the value is in, execute a delete against them all
        final int[] buckets = BUCKET_LOCATOR.getAllBuckets( key );
        List<ByteBuffer> mapKeys = new ArrayList<>();
        for( int bucket :  buckets){
            mapKeys.add( getMapKeyPartitionKey(scope, key, bucket));
        }

        Statement deleteMapKey;
        Clause inKey = QueryBuilder.in("key", mapKeys);
        deleteMapKey = QueryBuilder.delete().from(MAP_KEYS_TABLE)
            .where(inKey);
        session.execute(deleteMapKey);


    }


    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {

        final MultiTenantColumnFamilyDefinition mapEntries =
            new MultiTenantColumnFamilyDefinition( MAP_ENTRIES, BytesType.class.getSimpleName(),
                BytesType.class.getSimpleName(), BytesType.class.getSimpleName(),
                MultiTenantColumnFamilyDefinition.CacheOption.KEYS );

        final MultiTenantColumnFamilyDefinition mapKeys =
            new MultiTenantColumnFamilyDefinition( MAP_KEYS, BytesType.class.getSimpleName(),
                UTF8Type.class.getSimpleName(), BytesType.class.getSimpleName(),
                MultiTenantColumnFamilyDefinition.CacheOption.KEYS );

        return Arrays.asList( mapEntries, mapKeys );
    }


    private ByteBuffer getValueCQL( MapScope scope, String key, final ConsistencyLevel consistencyLevel ) {

        Clause in = QueryBuilder.in("key", getMapEntryPartitionKey(scope, key) );
        Statement statement = QueryBuilder.select().all().from(MAP_ENTRIES_TABLE)
            .where(in)
            .setConsistencyLevel(consistencyLevel);

        ResultSet resultSet = session.execute(statement);
        com.datastax.driver.core.Row row = resultSet.one();

        return row != null ? row.getBytes("value") : null;
    }



    private <T> T getValuesCQL( final MapScope scope, final Collection<String> keys, final ResultsBuilderCQL<T> builder ) {

        final List<ByteBuffer> serializedKeys = new ArrayList<>();

        keys.forEach(key -> serializedKeys.add(getMapEntryPartitionKey(scope,key)));

        Clause in = QueryBuilder.in("key", serializedKeys );
        Statement statement = QueryBuilder.select().all().from(MAP_ENTRIES_TABLE)
            .where(in);


        ResultSet resultSet = session.execute(statement);

        return builder.buildResultsCQL( resultSet );
    }




    /**
     * Inner class to serialize and edgeIdTypeKey
     */
    private static class MapKeySerializer implements CompositeFieldSerializer<String> {


        @Override
        public void toComposite( final CompositeBuilder builder, final String key ) {
            builder.addString( key );
        }


        @Override
        public String fromComposite( final CompositeParser composite ) {
            final String key = composite.readString();

            return key;
        }
    }


    /**
     * Inner class to serialize and edgeIdTypeKey
     */
    private static class MapEntrySerializer implements CompositeFieldSerializer<MapEntryKey> {

        @Override
        public void toComposite( final CompositeBuilder builder, final MapEntryKey key ) {

            builder.addString( key.mapName );
            builder.addString( key.key );
        }


        @Override
        public MapEntryKey fromComposite( final CompositeParser composite ) {

            final String mapName = composite.readString();

            final String entryKey = composite.readString();

            return new MapEntryKey( mapName, entryKey );
        }
    }


    /**
     * Entries for serializing map entries and keys to a row
     */
    private static class MapEntryKey {
        public final String mapName;
        public final String key;


        private MapEntryKey( final String mapName, final String key ) {
            this.mapName = mapName;
            this.key = key;
        }


        /**
         * Create a scoped row key from the key
         */
        public static ScopedRowKey<MapEntryKey> fromKey( final MapScope mapScope, final String key ) {

            return ScopedRowKey.fromKey( mapScope.getApplication(), new MapEntryKey( mapScope.getName(), key ) );
        }
    }



    /**
     * Build the results from the row keys
     */

    private interface ResultsBuilderCQL<T> {

        T buildResultsCQL( final ResultSet resultSet );
    }


    public static class StringResultsBuilderCQL implements ResultsBuilderCQL<Map<String, String>> {

        @Override
        public Map<String, String> buildResultsCQL( final ResultSet resultSet ) {


            final Map<String, String> results = new HashMap<>();

            resultSet.all().forEach( row -> {

                @SuppressWarnings("unchecked")
                List<Object> keys = (List) deserializeMapEntryKey(row.getBytes("key"));
                String value = (String)DataType.text().deserialize( row.getBytes("value"),
                    ProtocolVersion.NEWEST_SUPPORTED );

                // the actual string key value is the last element
                results.put((String)keys.get(keys.size() -1), value);

            });

            return results;
        }
    }

    private static Object deserializeMapEntryKey(ByteBuffer bb){

        List<Object> stuff = new ArrayList<>();
        while(bb.hasRemaining()){
            ByteBuffer data = CQLUtils.getWithShortLength(bb);
            if(stuff.size() == 0){
                stuff.add(DataType.uuid().deserialize(data.slice(), ProtocolVersion.NEWEST_SUPPORTED));
            }else{
                stuff.add(DataType.text().deserialize(data.slice(), ProtocolVersion.NEWEST_SUPPORTED));
            }
            byte equality = bb.get(); // we don't use this but take the equality byte off the buffer

        }

        return stuff;

    }

    public static ByteBuffer serializeKeys(UUID ownerUUID, String ownerType, String mapName, String mapKey,
                                           int bucketNumber ){

        List<Object> keys = new ArrayList<>(4);
        keys.add(0, ownerUUID);
        keys.add(1, ownerType);
        keys.add(2, mapName);
        keys.add(3, mapKey);

        if( bucketNumber > 0){
            keys.add(4, bucketNumber);
        }

        // UUIDs are 16 bytes, allocate the buffer accordingly
        int size = 16+ownerType.length()+mapName.length()+mapKey.length();
        if(bucketNumber > 0 ){
            // ints are 4 bytes
            size += 4;
        }

        // we always need to add length for the 2 byte short and 1 byte equality
        size += keys.size()*3;

        ByteBuffer stuff = ByteBuffer.allocate(size);

        for (Object key : keys) {

            ByteBuffer kb = DataType.serializeValue(key, ProtocolVersion.NEWEST_SUPPORTED);
            if (kb == null) {
                kb = ByteBuffer.allocate(0);
            }

            stuff.putShort((short) kb.remaining());
            stuff.put(kb.slice());
            stuff.put((byte) 0);


        }
        stuff.flip();
        return stuff.duplicate();

    }


    private ByteBuffer getMapEntryPartitionKey(MapScope scope, String key){

        return serializeKeys(scope.getApplication().getUuid(),
            scope.getApplication().getType(), scope.getName(), key, -1);

    }

    private ByteBuffer getMapKeyPartitionKey(MapScope scope, String key, int bucketNumber){

        return serializeKeys(scope.getApplication().getUuid(),
            scope.getApplication().getType(), scope.getName(), key, bucketNumber);

    }
}
