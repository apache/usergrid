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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Preconditions;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UTF8Type;

import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.BucketScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.BucketScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.shard.ExpandingShardLocator;
import org.apache.usergrid.persistence.core.shard.StringHashUtils;
import org.apache.usergrid.persistence.map.MapScope;

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.serializers.BooleanSerializer;
import com.netflix.astyanax.serializers.StringSerializer;


@Singleton
public class MapSerializationImpl implements MapSerialization {

    private static final MapKeySerializer KEY_SERIALIZER = new MapKeySerializer();

        private static final BucketScopedRowKeySerializer<String> MAP_KEY_SERIALIZER =
                new BucketScopedRowKeySerializer<>( KEY_SERIALIZER );


        private static final MapEntrySerializer ENTRY_SERIALIZER = new MapEntrySerializer();
        private static final ScopedRowKeySerializer<MapEntryKey> MAP_ENTRY_SERIALIZER =
                new ScopedRowKeySerializer<>( ENTRY_SERIALIZER );


        private static final BooleanSerializer BOOLEAN_SERIALIZER = BooleanSerializer.get();

        private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();


    private static final StringResultsBuilder STRING_RESULTS_BUILDER = new StringResultsBuilder();


        /**
         * CFs where the row key contains the source node id
         */
        public static final MultiTennantColumnFamily<ScopedRowKey<MapEntryKey>, Boolean>
            MAP_ENTRIES = new MultiTennantColumnFamily<>(
                "Map_Entries", MAP_ENTRY_SERIALIZER, BOOLEAN_SERIALIZER );


        /**
         * CFs where the row key contains the source node id
         */
        public static final MultiTennantColumnFamily<BucketScopedRowKey<String>, String> MAP_KEYS =
                new MultiTennantColumnFamily<>( "Map_Keys", MAP_KEY_SERIALIZER, STRING_SERIALIZER );

    /**
     * Number of buckets to hash across.
     */
    private static final int[] NUM_BUCKETS = {20};

    /**
     * How to funnel keys for buckets
     */
    private static final Funnel<String> MAP_KEY_FUNNEL = new Funnel<String>() {



        @Override
        public void funnel( final String key, final PrimitiveSink into ) {
            into.putString( key, StringHashUtils.UTF8 );
        }
    };

    /**
     * Locator to get us all buckets
     */
    private static final ExpandingShardLocator<String>
            BUCKET_LOCATOR = new ExpandingShardLocator<>(MAP_KEY_FUNNEL, NUM_BUCKETS);

    private final Keyspace keyspace;


    @Inject
    public MapSerializationImpl( final Keyspace keyspace ) {this.keyspace = keyspace;}


    @Override
    public String getString( final MapScope scope, final String key ) {
        Column<Boolean> col = getValue(scope, key); // TODO: why boolean?
        return (col !=null) ?  col.getStringValue(): null;
    }


    @Override
    public Map<String, String> getStrings(final MapScope scope,  final Collection<String> keys ) {
        return getValues( scope, keys, STRING_RESULTS_BUILDER );
    }


    @Override
    public void putString( final MapScope scope, final String key, final String value ) {
        final RowOp op = new RowOp() {
            @Override
            public void putValue(final ColumnListMutation<Boolean> columnListMutation ) {
                columnListMutation.putColumn( true, value );
            }


            @Override
            public void putKey(final ColumnListMutation<String> keysMutation ) {
                keysMutation.putColumn( key, true );
            }
        };


        writeString( scope, key, value, op );
    }


    @Override
    public void putString( final MapScope scope, final String key, final String value, final int ttl ) {
        Preconditions.checkArgument( ttl > 0, "ttl must be > than 0" );

        final RowOp op = new RowOp() {
            @Override
            public void putValue( final ColumnListMutation<Boolean> columnListMutation ) {
                columnListMutation.putColumn( true, value, ttl );
            }


            @Override
            public void putKey( final ColumnListMutation<String> keysMutation ) {
                keysMutation.putColumn( key, true, ttl );
            }
        };


        writeString( scope, key, value, op );
    }


    /**
     * Write our string index with the specified row op
     * @param scope
     * @param key
     * @param value
     * @param rowOp
     */
    private void writeString( final MapScope scope, final String key, final String value, final RowOp rowOp ) {

        Preconditions.checkNotNull( scope, "mapscope is required" );
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required" );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        //add it to the entry
        final ScopedRowKey<MapEntryKey> entryRowKey = MapEntryKey.fromKey( scope, key );

        //serialize to the
        // entry


        rowOp.putValue( batch.withRow( MAP_ENTRIES, entryRowKey ) );


        //add it to the keys

        final int bucket = BUCKET_LOCATOR.getCurrentBucket( key );

        final BucketScopedRowKey<String> keyRowKey = BucketScopedRowKey.fromKey( scope.getApplication(), key, bucket );

        //serialize to the entry

        rowOp.putKey( batch.withRow( MAP_KEYS, keyRowKey ) );


        executeBatch( batch );
    }


    /**
     * Callbacks for performing row operations
     */
    private static interface RowOp{

        /**
         * Callback to do the row
         * @param columnListMutation The column mutation
         */
        void putValue( final ColumnListMutation<Boolean> columnListMutation );


        /**
         * Write the key
         * @param keysMutation
         */
        void putKey( final ColumnListMutation<String> keysMutation );


    }

    @Override
    public UUID getUuid( final MapScope scope, final String key ) {

        Column<Boolean> col = getValue(scope, key);
        return (col !=null) ?  col.getUUIDValue(): null;
    }


    @Override
    public void putUuid( final MapScope scope, final String key, final UUID putUuid ) {

        Preconditions.checkNotNull(scope, "mapscope is required");
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( putUuid, "value is required" );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        //add it to the entry
        final ScopedRowKey<MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

        //serialize to the entry
        batch.withRow(MAP_ENTRIES, entryRowKey).putColumn(true, putUuid);

        //add it to the keys

        final int bucket = BUCKET_LOCATOR.getCurrentBucket( key );

        final BucketScopedRowKey< String> keyRowKey =
                BucketScopedRowKey.fromKey( scope.getApplication(), key, bucket);

        //serialize to the entry
        batch.withRow(MAP_KEYS, keyRowKey).putColumn(key, true);

        executeBatch(batch);

    }


    @Override
    public Long getLong( final MapScope scope, final String key ) {
        Column<Boolean> col = getValue(scope, key);
        return (col !=null) ?  col.getLongValue(): null;
    }




    @Override
    public void putLong( final MapScope scope, final String key, final Long value ) {

        Preconditions.checkNotNull(scope, "mapscope is required");
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required" );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        //add it to the entry
        final ScopedRowKey<MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

        //serialize to the entry
        batch.withRow(MAP_ENTRIES, entryRowKey).putColumn(true, value);

        //add it to the keys
        final int bucket = BUCKET_LOCATOR.getCurrentBucket( key );

               final BucketScopedRowKey< String> keyRowKey =
                       BucketScopedRowKey.fromKey( scope.getApplication(), key, bucket);

        //serialize to the entry
        batch.withRow(MAP_KEYS, keyRowKey).putColumn(key, true);

        executeBatch(batch);
    }


    @Override
    public void delete( final MapScope scope, final String key ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();
        final ScopedRowKey<MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

        //serialize to the entry
        batch.withRow(MAP_ENTRIES, entryRowKey).delete();

        //add it to the keys, we're not sure which one it may have come from
       final int[] buckets = BUCKET_LOCATOR.getAllBuckets( key );


        final List<BucketScopedRowKey<String>>
                rowKeys = BucketScopedRowKey.fromRange( scope.getApplication(), key, buckets );

        for(BucketScopedRowKey<String> rowKey: rowKeys) {
            batch.withRow( MAP_KEYS, rowKey ).deleteColumn( key );
        }

        executeBatch( batch );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {

        final MultiTennantColumnFamilyDefinition mapEntries =
                new MultiTennantColumnFamilyDefinition( MAP_ENTRIES,
                       BytesType.class.getSimpleName(),
                       BytesType.class.getSimpleName(),
                       BytesType.class.getSimpleName(),
                       MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        final MultiTennantColumnFamilyDefinition mapKeys =
                new MultiTennantColumnFamilyDefinition( MAP_KEYS,
                        BytesType.class.getSimpleName(),
                        UTF8Type.class.getSimpleName(),
                        BytesType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        return Arrays.asList( mapEntries, mapKeys );
    }


    private  Column<Boolean> getValue(MapScope scope, String key) {



        //add it to the entry
        final ScopedRowKey<MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

        //now get all columns, including the "old row key value"
        try {
            final Column<Boolean> result = keyspace.prepareQuery( MAP_ENTRIES )
                    .getKey( entryRowKey ).getColumn( true ).execute().getResult();

            return result;
        }
        catch ( NotFoundException nfe ) {
            //nothing to return
            return null;
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }
    }


    /**
     * Get multiple values, using the string builder
     * @param scope
     * @param keys
     * @param builder
     * @param <T>
     * @return
     */
    private <T> T getValues(final MapScope scope, final Collection<String> keys, final ResultsBuilder<T> builder) {


        final List<ScopedRowKey<MapEntryKey>> rowKeys = new ArrayList<>( keys.size() );

        for(final String key: keys){
             //add it to the entry
            final ScopedRowKey<MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

            rowKeys.add( entryRowKey );

        }



          //now get all columns, including the "old row key value"
          try {
              final Rows<ScopedRowKey<MapEntryKey>, Boolean>
                  rows = keyspace.prepareQuery( MAP_ENTRIES ).getKeySlice( rowKeys ).withColumnSlice( true )
                                                     .execute().getResult();


             return builder.buildResults( rows );
          }
          catch ( NotFoundException nfe ) {
              //nothing to return
              return null;
          }
          catch ( ConnectionException e ) {
              throw new RuntimeException( "Unable to connect to cassandra", e );
          }
      }



    private void executeBatch(MutationBatch batch) {
        try {
            batch.execute();
        } catch (ConnectionException e) {
            throw new RuntimeException("Unable to connect to cassandra", e);
        }
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
        public static ScopedRowKey<MapEntryKey> fromKey(
                final MapScope mapScope, final String key ) {

            return ScopedRowKey.fromKey( mapScope.getApplication(), new MapEntryKey( mapScope.getName(), key ) );
        }
    }


    /**
     * Build the results from the row keys
     * @param <T>
     */
    private static interface ResultsBuilder<T> {

        public T buildResults(final  Rows<ScopedRowKey<MapEntryKey>, Boolean> rows);
    }

    public static class StringResultsBuilder implements ResultsBuilder<Map<String, String>>{

        @Override
        public Map<String, String> buildResults( final Rows<ScopedRowKey<MapEntryKey>, Boolean> rows ) {
            final int size = rows.size();

            final Map<String, String> results = new HashMap<>(size);

            for(int i = 0; i < size; i ++){

                final Row<ScopedRowKey<MapEntryKey>, Boolean> row = rows.getRowByIndex( i );

                final String value = row.getColumns().getStringValue( true, null );

                if(value == null){
                    continue;
                }

               results.put( row.getKey().getKey().key,  value );
            }

            return results;
        }
    }
}
