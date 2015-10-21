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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
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
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
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
import java.util.concurrent.Callable;


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

    private final ObjectMapper MAPPER = new ObjectMapper();


    //------------------------------------------------------------------------------------------

    @Inject
    public ScopedCacheSerializationImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
        //MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.enableDefaultTyping();
        MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }


    @Override
    public V readValue(CacheScope scope, K key, TypeReference typeRef ) {

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
                //V value = MAPPER.readValue(result.getByteArrayValue(), new TypeReference<V>() {});
                V value = MAPPER.readValue(result.getByteArrayValue(), typeRef);

                logger.debug("Read cache item from scope {}\n   key/value types {}/{}\n   key:value: {}:{}",
                    new Object[]{
                        scope.getApplication().getUuid(),
                        key.getClass().getSimpleName(),
                        value.getClass().getSimpleName(),
                        key,
                        value});

                return value;

            } catch (NotFoundException nfe) {
                logger.info("Value not found");

            } catch (IOException ioe) {
                logger.error("Unable to read cached value", ioe);
                throw new RuntimeException("Unable to read cached value", ioe);
            }

        } catch (ConnectionException e) {
            throw new RuntimeException("Unable to connect to cassandra", e);
        }

        logger.info("Cache value not found for key {}", key );

        return null;
    }


    @Override
    public V writeValue(CacheScope scope, K key, V value, Integer ttl) {

        Preconditions.checkNotNull( scope, "scope is required");
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required");
        Preconditions.checkNotNull( ttl, "ttl is required");

        // determine bucketed row-key based application UUID

        String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);

        final BucketScopedRowKey<String> keyRowKey =
            BucketScopedRowKey.fromKey(scope.getApplication(), rowKeyString, bucket);

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

        logger.debug("Wrote cache item to scope {}\n   key/value types {}/{}\n   key:value: {}:{}",
            new Object[] {
                scope.getApplication().getUuid(),
                key.getClass().getSimpleName(),
                value.getClass().getSimpleName(),
                key,
                value});

        return value;
    }


    @Override
    public void removeValue(CacheScope scope, K key) {

        Preconditions.checkNotNull( scope, "scope is required");
        Preconditions.checkNotNull( key, "key is required" );

        // determine bucketed row-key based application UUID

        String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);

        final BucketScopedRowKey<String> keyRowKey =
            BucketScopedRowKey.fromKey(scope.getApplication(), rowKeyString, bucket);

        // determine column name based on K key to string
        String columnName = key.toString();

        final MutationBatch batch = keyspace.prepareMutationBatch();
        batch.withRow(SCOPED_CACHE, keyRowKey).deleteColumn(columnName);

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

        final OperationResult<Void> result = executeBatch(batch);

        logger.debug("Invalidated scope {}", scope.getApplication().getUuid());
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


    private OperationResult<Void> executeBatch(MutationBatch batch) {
        try {
            return batch.execute();

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

}
