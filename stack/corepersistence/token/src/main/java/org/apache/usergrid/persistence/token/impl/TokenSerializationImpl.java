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
package org.apache.usergrid.persistence.token.impl;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.*;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionImpl;
import org.apache.usergrid.persistence.core.shard.ExpandingShardLocator;
import org.apache.usergrid.persistence.core.shard.StringHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;


/**
 * Serialize cache to Cassandra.
 */
public class TokenSerializationImpl implements TokenSerialization {

    public static final Logger logger = LoggerFactory.getLogger(TokenSerializationImpl.class);

    private SmileFactory smile = new SmileFactory();

    private ObjectMapper smileMapper = new ObjectMapper( smile );

    private static final String TOKEN_UUID = "uuid";
    private static final String TOKEN_TYPE = "type";
    private static final String TOKEN_CREATED = "created";
    private static final String TOKEN_ACCESSED = "accessed";
    private static final String TOKEN_INACTIVE = "inactive";
    private static final String TOKEN_DURATION = "duration";
    private static final String TOKEN_PRINCIPAL_TYPE = "principal";
    private static final String TOKEN_ENTITY = "entity";
    private static final String TOKEN_APPLICATION = "application";
    private static final String TOKEN_STATE = "state";
    private static final String TOKEN_WORKFLOW_ORG_ID = "workflowOrgId";

    private static final Set<String> TOKEN_PROPERTIES;

    static {
        HashSet<String> set = new HashSet<String>();
        set.add( TOKEN_UUID );
        set.add( TOKEN_TYPE );
        set.add( TOKEN_CREATED );
        set.add( TOKEN_ACCESSED );
        set.add( TOKEN_INACTIVE );
        set.add( TOKEN_PRINCIPAL_TYPE );
        set.add( TOKEN_ENTITY );
        set.add( TOKEN_APPLICATION );
        set.add( TOKEN_STATE );
        set.add( TOKEN_DURATION );
        set.add( TOKEN_WORKFLOW_ORG_ID );
        TOKEN_PROPERTIES = Collections.unmodifiableSet(set);
    }

    private static final HashSet<String> REQUIRED_TOKEN_PROPERTIES = new HashSet<String>();


    static {
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_UUID );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_TYPE );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_CREATED );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_ACCESSED );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_INACTIVE );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_DURATION );
    }



    private static final String TOKENS_TABLE = CQLUtils.quote("Tokens");
    private static final Collection<String> TOKENS_PARTITION_KEYS = Collections.singletonList("key");
    private static final Collection<String> TOKENS_COLUMN_KEYS = Collections.singletonList("column1");
    private static final Map<String, DataType.Name> TOKENS_COLUMNS =
        new HashMap<String, DataType.Name>() {{
            put( "key", DataType.Name.BLOB );
            put( "column1", DataType.Name.BLOB );
            put( "value", DataType.Name.BLOB ); }};
    private static final Map<String, String> TOKENS_CLUSTERING_ORDER =
        new HashMap<String, String>(){{ put( "column1", "ASC" ); }};

    private static final String PRINCIPAL_TOKENS_TABLE = CQLUtils.quote("PrincipalTokens");
    private static final Collection<String> PRINCIPAL_TOKENS_PARTITION_KEYS = Collections.singletonList("key");
    private static final Collection<String> PRINCIPAL_TOKENS_COLUMN_KEYS = Collections.singletonList("column1");
    private static final Map<String, DataType.Name> PRINCIPAL_TOKENS_COLUMNS =
        new HashMap<String, DataType.Name>() {{
            put( "key", DataType.Name.BLOB );
            put( "column1", DataType.Name.UUID );
            put( "value", DataType.Name.BLOB ); }};
    private static final Map<String, String> PRINCIPAL_TOKENS_CLUSTERING_ORDER =
        new HashMap<String, String>(){{ put( "column1", "ASC" ); }};




    private final Session session;
    private final CassandraConfig cassandraConfig;




    @Inject
    public TokenSerializationImpl(final Session session,
                                  final CassandraConfig cassandraConfig ) {
        this.session = session;
        this.cassandraConfig = cassandraConfig;

    }


    @Override
    public void deleteToken(UUID tokenUUID){

    }

    @Override
    public void revokeToken(UUID tokenUUID, ByteBuffer principalKeyBuffer){

    }

    @Override
    public void updateTokenAccessTime(UUID tokenUUID, int accessdTime, int inactiveTime ){

    }

    @Override
    public Map<String, Object> getTokenInfo(UUID tokenUUID){

        Preconditions.checkNotNull(tokenUUID, "token UUID is required");

        List<ByteBuffer> tokenProperties = new ArrayList<>();
        TOKEN_PROPERTIES.forEach( prop ->
            tokenProperties.add(DataType.serializeValue(prop, ProtocolVersion.NEWEST_SUPPORTED)));

        final ByteBuffer key = DataType.text().serialize(tokenUUID, ProtocolVersion.NEWEST_SUPPORTED);

        final Clause inKey = QueryBuilder.eq("key", key);
        final Clause inColumn = QueryBuilder.in("column1", tokenProperties );

        final Statement statement = QueryBuilder.select().all().from(TOKENS_TABLE)
            .where(inKey)
            .and(inColumn)
            .setConsistencyLevel(cassandraConfig.getDataStaxReadCl());

        final ResultSet resultSet = session.execute(statement);
        final List<Row> rows = resultSet.all();

        Map<String, Object> tokenInfo = new HashMap<>();

        rows.forEach( row -> {

            final String name = row.getString("column1");
            final Object value = deserializeColumnValue(name, row.getBytes("value"));

            if (value == null){
                throw new RuntimeException("error deserializing token info for property: "+name);
            }

            tokenInfo.put(name, value);

        });

        return tokenInfo;
    }

    @Override
    public void putTokenInfo(UUID tokenUUID, Map<String, Object> tokenInfo){

    }

    @Override
    public List<UUID> getTokensForPrincipal(ByteBuffer principalKeyBuffer){
        return new ArrayList<>();
    }


    private Object deserializeColumnValue(final String name, final ByteBuffer bb){


        switch (name) {
            case TOKEN_TYPE:
            case TOKEN_PRINCIPAL_TYPE:
                return DataType.text().deserialize(bb, ProtocolVersion.NEWEST_SUPPORTED);
            case TOKEN_CREATED:
            case TOKEN_ACCESSED:
            case TOKEN_INACTIVE:
            case TOKEN_DURATION:
                return DataType.bigint().deserialize(bb, ProtocolVersion.NEWEST_SUPPORTED);
            case TOKEN_ENTITY:
            case TOKEN_APPLICATION:
            case TOKEN_WORKFLOW_ORG_ID:
            case TOKEN_UUID:
                return DataType.uuid().deserialize(bb, ProtocolVersion.NEWEST_SUPPORTED);
            case TOKEN_STATE:
                fromByteBuffer(bb, Object.class);
        }

        return null;
    }


    private Object fromByteBuffer( ByteBuffer byteBuffer, Class<?> clazz ) {
        if ( ( byteBuffer == null ) || !byteBuffer.hasRemaining() ) {
            return null;
        }
        if ( clazz == null ) {
            clazz = Object.class;
        }

        Object obj = null;
        try {
            obj = smileMapper.readValue( byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(),
                byteBuffer.remaining(), clazz );
        }
        catch ( Exception e ) {
            logger.error( "Error parsing SMILE bytes", e );
        }
        return obj;
    }

//    @Override
//    public V readValue(CacheScope scope, K key, TypeReference typeRef ) {
//
//        return readValueCQL( scope, key, typeRef);
//
//    }


//    private V readValueCQL(CacheScope scope, K key, TypeReference typeRef){
//
//        Preconditions.checkNotNull(scope, "scope is required");
//        Preconditions.checkNotNull(key, "key is required");
//
//        final String rowKeyString = scope.getApplication().getUuid().toString();
//        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);
//
//        // determine column name based on K key to string
//        final String columnName = key.toString();
//
//        final Clause inKey = QueryBuilder.eq("key", getPartitionKey(scope, rowKeyString, bucket) );
//        final Clause inColumn = QueryBuilder.eq("column1", DataType.text().serialize(columnName, ProtocolVersion.NEWEST_SUPPORTED) );
//
//        final Statement statement = QueryBuilder.select().all().from(TOKENS_TABLE)
//            .where(inKey)
//            .and(inColumn)
//            .setConsistencyLevel(cassandraConfig.getDataStaxReadCl());
//
//        final ResultSet resultSet = session.execute(statement);
//        final com.datastax.driver.core.Row row = resultSet.one();
//
//        if (row == null){
//
//            if(logger.isDebugEnabled()){
//                logger.debug("Cache value not found for key {}", key );
//            }
//
//            return null;
//        }
//
//
//        try {
//
//            return MAPPER.readValue(row.getBytes("value").array(), typeRef);
//
//        } catch (IOException ioe) {
//            logger.error("Unable to read cached value", ioe);
//            throw new RuntimeException("Unable to read cached value", ioe);
//        }
//
//
//    }


//    @Override
//    public V writeValue(CacheScope scope, K key, V value, Integer ttl) {
//
//        return writeValueCQL( scope, key, value, ttl);
//
//    }
//
//    private V writeValueCQL(CacheScope scope, K key, V value, Integer ttl) {
//
//        Preconditions.checkNotNull( scope, "scope is required");
//        Preconditions.checkNotNull( key, "key is required" );
//        Preconditions.checkNotNull( value, "value is required");
//        Preconditions.checkNotNull( ttl, "ttl is required");
//
//
//        final String rowKeyString = scope.getApplication().getUuid().toString();
//        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);
//
//        // determine column name based on K key to string
//        final String columnName = key.toString();
//
//        // serialize cache item
//        byte[] cacheBytes;
//        try {
//            cacheBytes = MAPPER.writeValueAsBytes(value);
//        } catch (JsonProcessingException jpe) {
//            throw new RuntimeException("Unable to serialize cache value", jpe);
//        }
//
//        final Using timeToLive = QueryBuilder.ttl(ttl);
//
//
//        // convert to ByteBuffer for the blob DataType in Cassandra
//        final ByteBuffer bb = ByteBuffer.allocate(cacheBytes.length);
//        bb.put(cacheBytes);
//        bb.flip();
//
//        final Statement cacheEntry = QueryBuilder.insertInto(TOKENS_TABLE)
//            .using(timeToLive)
//            .value("key", getPartitionKey(scope, rowKeyString, bucket))
//            .value("column1", DataType.text().serialize(columnName, ProtocolVersion.NEWEST_SUPPORTED))
//            .value("value", bb);
//
//
//        session.execute(cacheEntry);
//
//        logger.debug("Wrote cache item to scope {}\n   key/value types {}/{}\n   key:value: {}:{}",
//            scope.getApplication().getUuid(),
//            key.getClass().getSimpleName(),
//            value.getClass().getSimpleName(),
//            key,
//            value);
//
//        return value;
//
//    }
//
//
//
//    @Override
//    public void removeValue(CacheScope scope, K key) {
//
//        removeValueCQL(scope, key);
//
//    }
//
//
//    private void removeValueCQL(CacheScope scope, K key) {
//
//        Preconditions.checkNotNull( scope, "scope is required");
//        Preconditions.checkNotNull( key, "key is required" );
//
//        // determine bucketed row-key based application UUID
//
//        final String rowKeyString = scope.getApplication().getUuid().toString();
//        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);
//
//        // determine column name based on K key to string
//        final String columnName = key.toString();
//
//
//        final Clause inKey = QueryBuilder.eq("key", getPartitionKey(scope, rowKeyString, bucket) );
//        final Clause inColumn = QueryBuilder.eq("column1", DataType.text().serialize(columnName, ProtocolVersion.NEWEST_SUPPORTED) );
//
//        final Statement statement = QueryBuilder.delete().from(TOKENS_TABLE)
//            .where(inKey)
//            .and(inColumn);
//
//        session.execute(statement);
//
//    }
//
//
//    @Override
//    public void invalidate(CacheScope scope) {
//
//        invalidateCQL(scope);
//        logger.debug("Invalidated scope {}", scope.getApplication().getUuid());
//
//    }
//
//    private void invalidateCQL(CacheScope scope){
//
//        Preconditions.checkNotNull(scope, "scope is required");
//
//        // determine bucketed row-key based application UUID
//        final String rowKeyString = scope.getApplication().getUuid().toString();
//        final int bucket = BUCKET_LOCATOR.getCurrentBucket(rowKeyString);
//
//        final Clause inKey = QueryBuilder.eq("key", getPartitionKey(scope, rowKeyString, bucket) );
//
//        final Statement statement = QueryBuilder.delete().from(TOKENS_TABLE)
//            .where(inKey);
//
//        session.execute(statement);
//
//    }
//
    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {

        return Collections.emptyList();
    }

    @Override
    public Collection<TableDefinition> getTables() {

        final TableDefinition tokens =
            new TableDefinitionImpl(
                cassandraConfig.getApplicationKeyspace(),
                TOKENS_TABLE,
                TOKENS_PARTITION_KEYS,
                TOKENS_COLUMN_KEYS,
                TOKENS_COLUMNS,
                TableDefinitionImpl.CacheOption.KEYS,
                TOKENS_CLUSTERING_ORDER);

        final TableDefinition principalTokens =
            new TableDefinitionImpl(
                cassandraConfig.getApplicationKeyspace(),
                PRINCIPAL_TOKENS_TABLE,
                PRINCIPAL_TOKENS_PARTITION_KEYS,
                PRINCIPAL_TOKENS_COLUMN_KEYS,
                PRINCIPAL_TOKENS_COLUMNS,
                TableDefinitionImpl.CacheOption.KEYS,
                PRINCIPAL_TOKENS_CLUSTERING_ORDER);

        return Arrays.asList(tokens, principalTokens);
    }
//
//
//
//    private ByteBuffer getPartitionKey(CacheScope scope, String key, int bucketNumber){
//
//        return serializeKeys(scope.getApplication().getUuid(),
//            scope.getApplication().getType(), bucketNumber, key);
//
//    }

    private static ByteBuffer serializeTokenKey(UUID ownerUUID, String ownerType, int bucketNumber, String rowKeyString ){

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
        return stuff;

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
        return stuff;

    }

}
