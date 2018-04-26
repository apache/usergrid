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
import java.util.*;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Using;

import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionImpl;
import org.apache.usergrid.persistence.core.shard.ExpandingShardLocator;
import org.apache.usergrid.persistence.core.shard.StringHashUtils;
import org.apache.usergrid.persistence.map.MapKeyResults;
import org.apache.usergrid.persistence.map.MapScope;

import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.apache.commons.lang.StringUtils.isBlank;


@Singleton
public class MapSerializationImpl implements MapSerialization {


    private static final String MAP_ENTRIES_TABLE = CQLUtils.quote("Map_Entries");
    private static final Collection<String> MAP_ENTRIES_PARTITION_KEYS = Collections.singletonList("key");
    private static final Collection<String> MAP_ENTRIES_COLUMN_KEYS = Collections.singletonList("column1");
    private static final Map<String, DataType.Name> MAP_ENTRIES_COLUMNS =
        new HashMap<String, DataType.Name>() {{
            put( "key", DataType.Name.BLOB );
            put( "column1", DataType.Name.BLOB );
            put( "value", DataType.Name.BLOB ); }};
    private static final Map<String, String> MAP_ENTRIES_CLUSTERING_ORDER =
        new HashMap<String, String>(){{ put( "column1", "ASC" ); }};


    private static final String MAP_KEYS_TABLE = CQLUtils.quote("Map_Keys");
    private static final Collection<String> MAP_KEYS_PARTITION_KEYS = Collections.singletonList("key");
    private static final Collection<String> MAP_KEYS_COLUMN_KEYS = Collections.singletonList("column1");
    private static final Map<String, DataType.Name> MAP_KEYS_COLUMNS =
        new HashMap<String, DataType.Name>() {{
            put( "key", DataType.Name.BLOB );
            put( "column1", DataType.Name.BLOB );
            put( "value", DataType.Name.BLOB ); }};
    private static final Map<String, String> MAP_KEYS_CLUSTERING_ORDER =
        new HashMap<String, String>(){{ put( "column1", "ASC" ); }};




    private static final StringResultsBuilderCQL STRING_RESULTS_BUILDER_CQL = new StringResultsBuilderCQL();


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

    private final CassandraConfig cassandraConfig;

    private final Session session;


    @Inject
    public MapSerializationImpl( final CassandraConfig cassandraConfig, final Session session ) {
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

        final BatchStatement batchStatement = new BatchStatement();

        Statement mapEntry;
        Statement mapKey;
        if (ttl > 0){
            Using timeToLive = QueryBuilder.ttl(ttl);

             batchStatement.add(QueryBuilder.insertInto(MAP_ENTRIES_TABLE)
                .using(timeToLive)
                .value("key", getMapEntryPartitionKey(scope, key))
                .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
                .value("value", DataType.text().serialize(value, ProtocolVersion.NEWEST_SUPPORTED)));


            final int bucket = BUCKET_LOCATOR.getCurrentBucket( scope.getName() );
            batchStatement.add(QueryBuilder.insertInto(MAP_KEYS_TABLE)
                .using(timeToLive)
                .value("key", getMapKeyPartitionKey(scope, bucket))
                .value("column1", DataType.text().serialize(key, ProtocolVersion.NEWEST_SUPPORTED))
                .value("value", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED)));
        }else{

            batchStatement.add(QueryBuilder.insertInto(MAP_ENTRIES_TABLE)
                .value("key", getMapEntryPartitionKey(scope, key))
                .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
                .value("value", DataType.text().serialize(value, ProtocolVersion.NEWEST_SUPPORTED)));

            // get a bucket number for the map keys table
            final int bucket = BUCKET_LOCATOR.getCurrentBucket( scope.getName() );

            batchStatement.add(QueryBuilder.insertInto(MAP_KEYS_TABLE)
                .value("key", getMapKeyPartitionKey(scope, bucket))
                .value("column1", DataType.text().serialize(key, ProtocolVersion.NEWEST_SUPPORTED))
                .value("value", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED)));

        }

        session.execute(batchStatement);

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

        final BatchStatement batchStatement = new BatchStatement();

        batchStatement.add(QueryBuilder.insertInto(MAP_ENTRIES_TABLE)
            .value("key", getMapEntryPartitionKey(scope, key))
            .value("column1", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED))
            .value("value", DataType.uuid().serialize(putUuid, ProtocolVersion.NEWEST_SUPPORTED)));



        final int bucket = BUCKET_LOCATOR.getCurrentBucket( scope.getName() );
        batchStatement.add(QueryBuilder.insertInto(MAP_KEYS_TABLE)
            .value("key", getMapKeyPartitionKey(scope, bucket))
            .value("column1", DataType.text().serialize(key, ProtocolVersion.NEWEST_SUPPORTED))
            .value("value", DataType.serializeValue(null, ProtocolVersion.NEWEST_SUPPORTED)));

        session.execute(batchStatement);

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


        final int bucket = BUCKET_LOCATOR.getCurrentBucket( scope.getName() );
        Statement mapKey;
        mapKey = QueryBuilder.insertInto(MAP_KEYS_TABLE)
            .value("key", getMapKeyPartitionKey(scope, bucket))
            .value("column1", DataType.text().serialize(key, ProtocolVersion.NEWEST_SUPPORTED))
            .value("value", DataType.cboolean().serialize(true, ProtocolVersion.NEWEST_SUPPORTED));

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
        final int[] buckets = BUCKET_LOCATOR.getAllBuckets( scope.getName() );
        List<ByteBuffer> mapKeys = new ArrayList<>();
        for( int bucket :  buckets){
            mapKeys.add( getMapKeyPartitionKey(scope, bucket));
        }

        Statement deleteMapKey;
        Clause inKey = QueryBuilder.in("key", mapKeys);
        Clause column1Equals = QueryBuilder.eq("column1", DataType.text().serialize(key, ProtocolVersion.NEWEST_SUPPORTED));
        deleteMapKey = QueryBuilder.delete().from(MAP_KEYS_TABLE)
            .where(inKey).and(column1Equals);
        session.execute(deleteMapKey);


    }


    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {

        // This here only until all traces of Astyanax are removed.
        return Collections.emptyList();

    }


    @Override
    public Collection<TableDefinition> getTables() {

        final TableDefinition mapEntries = new TableDefinitionImpl( cassandraConfig.getApplicationKeyspace(),
            MAP_ENTRIES_TABLE,
            MAP_ENTRIES_PARTITION_KEYS,
            MAP_ENTRIES_COLUMN_KEYS,
            MAP_ENTRIES_COLUMNS,
            TableDefinitionImpl.CacheOption.KEYS,
            MAP_ENTRIES_CLUSTERING_ORDER);

        final TableDefinition mapKeys = new TableDefinitionImpl( cassandraConfig.getApplicationKeyspace(),
            MAP_KEYS_TABLE,
            MAP_KEYS_PARTITION_KEYS,
            MAP_KEYS_COLUMN_KEYS,
            MAP_KEYS_COLUMNS,
            TableDefinitionImpl.CacheOption.KEYS,
            MAP_KEYS_CLUSTERING_ORDER);

        return Arrays.asList( mapEntries, mapKeys );

    }

    @Override
    public MapKeyResults getAllKeys(final MapScope scope, final String cursor, final int limit ){

        final int[] buckets = BUCKET_LOCATOR.getAllBuckets( scope.getName() );
        final List<ByteBuffer> partitionKeys = new ArrayList<>(NUM_BUCKETS.length);

        for (int bucket : buckets) {

            partitionKeys.add(getMapKeyPartitionKey(scope, bucket));
        }

        Clause in = QueryBuilder.in("key", partitionKeys);

        Statement statement;
        if( isBlank(cursor) ){
            statement = QueryBuilder.select().all().from(MAP_KEYS_TABLE)
                .where(in)
                .setFetchSize(limit);
        }else{
            statement = QueryBuilder.select().all().from(MAP_KEYS_TABLE)
                .where(in)
                .setFetchSize(limit)
                .setPagingState(PagingState.fromString(cursor));
        }


        ResultSet resultSet = session.execute(statement);
        PagingState pagingState = resultSet.getExecutionInfo().getPagingState();

        final List<String> keys = new ArrayList<>();
        Iterator<Row> resultIterator = resultSet.iterator();
        int size = 0;
        while( resultIterator.hasNext() && size < limit){

            size++;
            keys.add((String)DataType.text().deserialize(resultIterator.next().getBytes("column1"), ProtocolVersion.NEWEST_SUPPORTED));

        }

        return new MapKeyResults(pagingState != null ? pagingState.toString() : null, keys);

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




    private <T> T getValuesCQL(
        final MapScope scope, final Collection<String> keys, final ResultsBuilderCQL<T> builder ) {

        final List<ByteBuffer> serializedKeys = new ArrayList<>();

        keys.forEach(key -> serializedKeys.add(getMapEntryPartitionKey(scope,key)));

        Clause in = QueryBuilder.in("key", serializedKeys );
        Statement statement = QueryBuilder.select().all().from(MAP_ENTRIES_TABLE)
            .where(in);


        ResultSet resultSet = session.execute(statement);

        return builder.buildResultsCQL( resultSet );
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
        int size = 16+ownerType.getBytes().length+mapName.getBytes().length+mapKey.getBytes().length;
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
        return stuff;

    }


    private ByteBuffer getMapEntryPartitionKey(MapScope scope, String key){

        return serializeKeys(scope.getApplication().getUuid(),
            scope.getApplication().getType(), scope.getName(), key, -1);

    }

    private ByteBuffer getMapKeyPartitionKey(MapScope scope, int bucketNumber){

        return serializeKeys(scope.getApplication().getUuid(),
            scope.getApplication().getType(), scope.getName(), "", bucketNumber);

    }
}
