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

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Using;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.*;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionImpl;
import org.apache.usergrid.persistence.core.shard.ExpandingShardLocator;
import org.apache.usergrid.persistence.core.shard.StringHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * Serialize cache to Cassandra.
 */
public class ScopedCacheSerializationImpl<K,V> implements ScopedCacheSerialization<K,V> {

    public static final Logger logger = LoggerFactory.getLogger(ScopedCacheSerializationImpl.class);

    // row-keys are (app UUID, application type, app UUID as string, consistent hash int as bucket number)
    // column names are K key toString()
    // column values are serialization of V value

    private static final String SCOPED_CACHE_TABLE = CQLUtils.quote("SCOPED_CACHE");
    private static final Collection<String> SCOPED_CACHE_PARTITION_KEYS = Collections.singletonList("key");
    private static final Collection<String> SCOPED_CACHE_COLUMN_KEYS = Collections.singletonList("column1");
    private static final Map<String, DataType.Name> SCOPED_CACHE_COLUMNS =
        new HashMap<String, DataType.Name>() {{
            put( "key", DataType.Name.BLOB );
            put( "column1", DataType.Name.BLOB );
            put( "value", DataType.Name.BLOB ); }};
    private static final Map<String, String> SCOPED_CACHE_CLUSTERING_ORDER =
        new HashMap<String, String>(){{ put( "column1", "ASC" ); }};



    /** Number of buckets to hash across */
    private static final int[] NUM_BUCKETS = {20};

    /** How to funnel keys for buckets */
    private static final Funnel<String> MAP_KEY_FUNNEL =
        (Funnel<String>) (key, into) -> into.putString(key, StringHashUtils.UTF8);

    /** Locator to get us all buckets */
    private static final ExpandingShardLocator<String>
        BUCKET_LOCATOR = new ExpandingShardLocator<>(MAP_KEY_FUNNEL, NUM_BUCKETS);


    private final Session session;
    private final CassandraConfig cassandraConfig;
    private final ObjectMapper MAPPER = new ObjectMapper();




    @Inject
    public ScopedCacheSerializationImpl( final Session session,
                                         final CassandraConfig cassandraConfig ) {
        this.session = session;
        this.cassandraConfig = cassandraConfig;

        MAPPER.enableDefaultTyping();
        MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }


    @Override
    public V readValue(CacheScope scope, K key, TypeReference typeRef ) {

        return readValueCQL( scope, key, typeRef);

    }


    private V readValueCQL(CacheScope scope, K key, TypeReference typeRef){

        Preconditions.checkNotNull(scope, "scope is required");
        Preconditions.checkNotNull(key, "key is required");

        final String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);

        // determine column name based on K key to string
        final String columnName = key.toString();

        final Clause inKey = QueryBuilder.eq("key", getPartitionKey(scope, rowKeyString, bucket) );
        final Clause inColumn = QueryBuilder.eq("column1", DataType.text().serialize(columnName, ProtocolVersion.NEWEST_SUPPORTED) );

        final Statement statement = QueryBuilder.select().all().from(SCOPED_CACHE_TABLE)
            .where(inKey)
            .and(inColumn)
            .setConsistencyLevel(cassandraConfig.getDataStaxReadCl());

        final ResultSet resultSet = session.execute(statement);
        final com.datastax.driver.core.Row row = resultSet.one();

        if (row == null){

            if(logger.isDebugEnabled()){
                logger.debug("Cache value not found for key {}", key );
            }

            return null;
        }


        try {

            return MAPPER.readValue(row.getBytes("value").array(), typeRef);

        } catch (IOException ioe) {
            logger.error("Unable to read cached value", ioe);
            throw new RuntimeException("Unable to read cached value", ioe);
        }


    }


    @Override
    public V writeValue(CacheScope scope, K key, V value, Integer ttl) {

        return writeValueCQL( scope, key, value, ttl);

    }

    private V writeValueCQL(CacheScope scope, K key, V value, Integer ttl) {

        Preconditions.checkNotNull( scope, "scope is required");
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required");
        Preconditions.checkNotNull( ttl, "ttl is required");


        final String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);

        // determine column name based on K key to string
        final String columnName = key.toString();

        // serialize cache item
        byte[] cacheBytes;
        try {
            cacheBytes = MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException("Unable to serialize cache value", jpe);
        }

        final Using timeToLive = QueryBuilder.ttl(ttl);


        // convert to ByteBuffer for the blob DataType in Cassandra
        final ByteBuffer bb = ByteBuffer.allocate(cacheBytes.length);
        bb.put(cacheBytes);
        bb.flip();

        final Statement cacheEntry = QueryBuilder.insertInto(SCOPED_CACHE_TABLE)
            .using(timeToLive)
            .value("key", getPartitionKey(scope, rowKeyString, bucket))
            .value("column1", DataType.text().serialize(columnName, ProtocolVersion.NEWEST_SUPPORTED))
            .value("value", bb);


        session.execute(cacheEntry);

        logger.debug("Wrote cache item to scope {}\n   key/value types {}/{}\n   key:value: {}:{}",
            scope.getApplication().getUuid(),
            key.getClass().getSimpleName(),
            value.getClass().getSimpleName(),
            key,
            value);

        return value;

    }



    @Override
    public void removeValue(CacheScope scope, K key) {

        removeValueCQL(scope, key);

    }


    private void removeValueCQL(CacheScope scope, K key) {

        Preconditions.checkNotNull( scope, "scope is required");
        Preconditions.checkNotNull( key, "key is required" );

        // determine bucketed row-key based application UUID

        final String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);

        // determine column name based on K key to string
        final String columnName = key.toString();


        final Clause inKey = QueryBuilder.eq("key", getPartitionKey(scope, rowKeyString, bucket) );
        final Clause inColumn = QueryBuilder.eq("column1", DataType.text().serialize(columnName, ProtocolVersion.NEWEST_SUPPORTED) );

        final Statement statement = QueryBuilder.delete().from(SCOPED_CACHE_TABLE)
            .where(inKey)
            .and(inColumn);

        session.execute(statement);

    }


    @Override
    public void invalidate(CacheScope scope) {

        invalidateCQL(scope);
        logger.debug("Invalidated scope {}", scope.getApplication().getUuid());

    }

    private void invalidateCQL(CacheScope scope){

        Preconditions.checkNotNull(scope, "scope is required");

        // determine bucketed row-key based application UUID
        final String rowKeyString = scope.getApplication().getUuid().toString();
        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);

        final Clause inKey = QueryBuilder.eq("key", getPartitionKey(scope, rowKeyString, bucket) );

        final Statement statement = QueryBuilder.delete().from(SCOPED_CACHE_TABLE)
            .where(inKey);

        session.execute(statement);

    }

    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {

        return Collections.emptyList();
    }

    @Override
    public Collection<TableDefinition> getTables() {

        final TableDefinition scopedCache =
            new TableDefinitionImpl(
                cassandraConfig.getApplicationKeyspace(),
                SCOPED_CACHE_TABLE,
                SCOPED_CACHE_PARTITION_KEYS,
                SCOPED_CACHE_COLUMN_KEYS,
                SCOPED_CACHE_COLUMNS,
                TableDefinitionImpl.CacheOption.KEYS,
                SCOPED_CACHE_CLUSTERING_ORDER);

        return Collections.singletonList(scopedCache);
    }



    private ByteBuffer getPartitionKey(CacheScope scope, String key, int bucketNumber){

        return serializeKeys(scope.getApplication().getUuid(),
            scope.getApplication().getType(), bucketNumber, key);

    }

    private static ByteBuffer serializeKeys(UUID ownerUUID, String ownerType, int bucketNumber, String rowKeyString ){

        List<Object> keys = new ArrayList<>(4);
        keys.add(0, ownerUUID);
        keys.add(1, ownerType);
        keys.add(2, bucketNumber);
        keys.add(3, rowKeyString);

        // UUIDs are 16 bytes, allocate the buffer accordingly
        int size = 16+ownerType.getBytes().length+rowKeyString.getBytes().length;

        // ints are 4 bytes, add for the bucket
        size += 4;


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

}
