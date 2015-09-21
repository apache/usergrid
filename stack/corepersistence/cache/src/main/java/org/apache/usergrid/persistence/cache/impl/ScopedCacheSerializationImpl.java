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
package org.apache.usergrid.persistence.cache.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.serializers.ObjectSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.core.astyanax.*;
import org.apache.usergrid.persistence.core.shard.ExpandingShardLocator;
import org.apache.usergrid.persistence.core.shard.StringHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Serialize cache to Cassandra.
 */
public class ScopedCacheSerializationImpl<K,V> implements ScopedCacheSerialization<K,V> {

    // row-keys are application ID + consistent hash key
    // column names are K key toString()
    // column values are serialization of V value

    public static final Logger logger = LoggerFactory.getLogger(ScopedCacheSerializationImpl.class);


    private static final CacheRowKeySerializer ROWKEY_SERIALIZER = new CacheRowKeySerializer();

    private static final BucketScopedRowKeySerializer<String> BUCKET_ROWKEY_SERIALIZER =
        new BucketScopedRowKeySerializer<>( ROWKEY_SERIALIZER );

    private static final Serializer<String> COLUMN_NAME_SERIALIZER = StringSerializer.get();

    private static final ObjectSerializer COLUMN_VALUE_SERIALIZER = ObjectSerializer.get();

    public static final MultiTennantColumnFamily<BucketScopedRowKey<String>, String> SCOPED_CACHE
        = new MultiTennantColumnFamily<>( "SCOPED_CACHE",
            BUCKET_ROWKEY_SERIALIZER, COLUMN_NAME_SERIALIZER, COLUMN_VALUE_SERIALIZER );

    /** Number of buckets to hash across */
    private static final int[] NUM_BUCKETS = {20};

    /** How to funnel keys for buckets */
    private static final Funnel<String> MAP_KEY_FUNNEL = new Funnel<String>() {

        @Override
        public void funnel( final String key, final PrimitiveSink into ) {
            into.putString(key, StringHashUtils.UTF8);
        }
    };

    /**
     * Locator to get us all buckets
     */
    private static final ExpandingShardLocator<String>
        BUCKET_LOCATOR = new ExpandingShardLocator<>(MAP_KEY_FUNNEL, NUM_BUCKETS);

    private final Keyspace keyspace;

    private final JsonFactory JSON_FACTORY = new JsonFactory();

    private final ObjectMapper MAPPER = new ObjectMapper( JSON_FACTORY );


    //------------------------------------------------------------------------------------------

    @Inject
    public ScopedCacheSerializationImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public V readValue(CacheScope scope, K key) {

        Preconditions.checkNotNull(scope, "scope is required");
        Preconditions.checkNotNull(key, "key is required");

        // determine bucketed row-key based application UUID
        String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);
        final BucketScopedRowKey<String> keyRowKey =
            BucketScopedRowKey.fromKey(scope.getApplication(), rowKeyString, bucket);

        // determine column name based on K key to string
        String columnName = key.toString();

        try {
            try {
                Column<String> result = keyspace.prepareQuery(SCOPED_CACHE)
                    .getKey(keyRowKey).getColumn( columnName ).execute().getResult();

                result.getByteBufferValue();
                V value = MAPPER.readValue(result.getByteArrayValue(), new TypeReference<V>() {});
                return value;

            } catch (NotFoundException nfe) {
                logger.info("Value not found");

            } catch (IOException ioe) {
                throw new RuntimeException("Unable to read cached value", ioe);
            }

        } catch (ConnectionException e) {
            throw new RuntimeException("Unable to connect to cassandra", e);
        }

        return null;
    }


    @Override
    public void writeValue(CacheScope scope, K key, V value, Integer ttl) {

        Preconditions.checkNotNull( scope, "scope is required");
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required");
        Preconditions.checkNotNull( ttl, "ttl is required");

        // determine bucketed row-key based application UUID

        String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);

        final BucketScopedRowKey<String> keyRowKey =
            BucketScopedRowKey.fromKey( scope.getApplication(), rowKeyString, bucket);

        // determine column name based on K key to string
        String columnName = key.toString();

        // serialize cache item
        byte[] cacheBytes;
        try {
            cacheBytes = MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException("Unable to serialize cache value", jpe);
        }

        // serialize to the entry
        final MutationBatch batch = keyspace.prepareMutationBatch();
        batch.withRow(SCOPED_CACHE, keyRowKey).putColumn(columnName, cacheBytes, ttl);

        executeBatch(batch);
    }


    @Override
    public void invalidate(CacheScope scope) {

        Preconditions.checkNotNull(scope, "scope is required");

        // determine bucketed row-key based application UUID
        String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);
        final BucketScopedRowKey<String> keyRowKey =
            BucketScopedRowKey.fromKey(scope.getApplication(), rowKeyString, bucket);

        final MutationBatch batch = keyspace.prepareMutationBatch();

        batch.withRow(SCOPED_CACHE, keyRowKey).delete();
        executeBatch(batch);
    }


    private class MutationBatchExec implements Callable<Void> {
        private final MutationBatch myBatch;
        private MutationBatchExec(MutationBatch batch) {
            myBatch = batch;
        }
        @Override
        public Void call() throws Exception {
            myBatch.execute();
            return null;
        }
    }


    private void executeBatch(MutationBatch batch) {
        try {
            batch.execute();
        } catch (ConnectionException e) {
            throw new RuntimeException("Unable to connect to cassandra", e);
        }
    }


    //------------------------------------------------------------------------------------------

    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        final MultiTennantColumnFamilyDefinition scopedCache =
            new MultiTennantColumnFamilyDefinition( SCOPED_CACHE,
                BytesType.class.getSimpleName(),
                BytesType.class.getSimpleName(),
                BytesType.class.getSimpleName(),
                MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        return Arrays.asList(scopedCache);
    }


    //------------------------------------------------------------------------------------------

//    /**
//     * Entries for serializing cache entries keys to a row
//     */
//    private static class CacheKey {
//        public final String key;
//
//        private CacheKey( final String key ) {
//            this.key = key;
//        }
//
//        /**
//         * Create a scoped row key from the key
//         */
//        public static ScopedRowKey<CacheKey> fromKey(
//            final CacheScope cacheScope, final String key ) {
//            return ScopedRowKey.fromKey( cacheScope.getApplication(), new CacheKey( key ) );
//        }
//    }

    /**
     * Inner class to serialize cache key
     */
    private static class CacheRowKeySerializer implements CompositeFieldSerializer<String> {

        @Override
        public void toComposite( final CompositeBuilder builder, final String key ) {
            builder.addString(key);
        }

        @Override
        public String fromComposite( final CompositeParser composite ) {
            final String key = composite.readString();
            return key;
        }
    }


//    /**
//     * Inner class to serialize cache value
//     */
//    private static class CacheEntitySerializer implements CompositeFieldSerializer {
//
//        @Override
//        public void toComposite(CompositeBuilder builder, Object value) {
//
//        }
//
//        @Override
//        public Object fromComposite( final CompositeParser composite ) {
//            return null;
//        }
//    }


//    /**
//     * Build the results from the row keys
//     */
//    private static interface ResultsBuilder<T> {
//
//        public T buildResults(final  Rows<ScopedRowKey<CacheKey>, Boolean> rows);
//    }
//
//    public static class StringResultsBuilder implements ResultsBuilder<Map<String, String>>{
//
//        @Override
//        public Map<String, String> buildResults( final Rows<ScopedRowKey<CacheKey>, Boolean> rows ) {
//            final int size = rows.size();
//
//            final Map<String, String> results = new HashMap<>(size);
//
//            for(int i = 0; i < size; i ++){
//
//                final Row<ScopedRowKey<CacheKey>, Boolean> row = rows.getRowByIndex( i );
//
//                final String value = row.getColumns().getStringValue( true, null );
//
//                if(value == null){
//                    continue;
//                }
//
//                results.put( row.getKey().getKey().key,  value );
//            }
//
//            return results;
//        }
//    }
}
