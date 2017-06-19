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
import com.datastax.driver.core.querybuilder.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.*;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionImpl;
import org.apache.usergrid.persistence.token.TokenSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;


/**
 * Serialize tokens and their details to Cassandra.
 */
@Singleton
public class TokenSerializationImpl implements TokenSerialization {

    public static final Logger logger = LoggerFactory.getLogger(TokenSerializationImpl.class);

    private SmileFactory smile = new SmileFactory();

    private ObjectMapper smileMapper = new ObjectMapper( smile );

    public static final String TOKEN_UUID = "uuid";
    public static final String TOKEN_TYPE = "type";
    public static final String TOKEN_CREATED = "created";
    public static final String TOKEN_ACCESSED = "accessed";
    public static final String TOKEN_INACTIVE = "inactive";
    public static final String TOKEN_DURATION = "duration";
    public static final String TOKEN_PRINCIPAL_TYPE = "principal";
    public static final String TOKEN_ENTITY = "entity";
    public static final String TOKEN_APPLICATION = "application";
    public static final String TOKEN_STATE = "state";
    public static final String TOKEN_WORKFLOW_ORG_ID = "workflowOrgId";
    public static final String TOKEN_TYPE_ACCESS = "access";

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

    public static final HashSet<String> REQUIRED_TOKEN_PROPERTIES = new HashSet<String>();

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
    public void deleteTokens(final List<UUID> tokenUUIDs, final ByteBuffer principalKeyBuffer){

        Preconditions.checkNotNull(tokenUUIDs, "token UUID list is required");
        Preconditions.checkNotNull(tokenUUIDs, "principalKeyBuffer is required");

        logger.trace("deleteTokens, token UUIDs: {}", tokenUUIDs);

        final BatchStatement batchStatement = new BatchStatement();

        tokenUUIDs.forEach( tokenUUID ->
            batchStatement.add(
                QueryBuilder.delete()
                    .from(TOKENS_TABLE)
                    .where(QueryBuilder
                        .eq("key", DataType.uuid().serialize(tokenUUID, ProtocolVersion.NEWEST_SUPPORTED)))
            )
        );

        batchStatement.add(
            QueryBuilder.delete()
                .from(PRINCIPAL_TOKENS_TABLE)
                .where(QueryBuilder
                    .eq("key", principalKeyBuffer)));


        session.execute(batchStatement);

    }


    @Override
    public void revokeToken(final UUID tokenUUID, final ByteBuffer principalKeyBuffer){

        Preconditions.checkNotNull(tokenUUID, "token UUID is required");

        logger.trace("revokeToken, token UUID: {}", tokenUUID);


        final BatchStatement batchStatement = new BatchStatement();

        batchStatement.add(
            QueryBuilder.delete()
                .from(TOKENS_TABLE)
                .where(QueryBuilder
                    .eq("key", DataType.uuid().serialize(tokenUUID, ProtocolVersion.NEWEST_SUPPORTED))));

        if(principalKeyBuffer != null){
            batchStatement.add(
                QueryBuilder.delete()
                    .from(PRINCIPAL_TOKENS_TABLE)
                    .where(QueryBuilder
                        .eq("key", principalKeyBuffer))
                    .and(QueryBuilder
                        .eq("column1", DataType.uuid().serialize(tokenUUID, ProtocolVersion.NEWEST_SUPPORTED))));
        }

        session.execute(batchStatement);

    }


    @Override
    public void updateTokenAccessTime(UUID tokenUUID, long accessedTime, long inactiveTime, int ttl ){

        Preconditions.checkNotNull(tokenUUID, "token UUID is required");
        Preconditions.checkArgument(accessedTime > -1 , "accessedTime is required to be positive");
        Preconditions.checkArgument(inactiveTime == Long.MIN_VALUE || inactiveTime > -1 , "inactiveTime is required to be positive");
        Preconditions.checkArgument(ttl > -1 , "ttl is required to be positive");

        logger.trace("updateTokenAccessTime, token UUID: {}, accessedTime: {}, inactiveTime: {}, ttl: {}",
            tokenUUID, accessedTime, inactiveTime, ttl);

        final BatchStatement batchStatement = new BatchStatement();
        final Clause inKey =
            QueryBuilder.eq("key", DataType.uuid().serialize(tokenUUID, ProtocolVersion.NEWEST_SUPPORTED));
        final Clause whereTokenAccessed =
            QueryBuilder.eq("column1", DataType.serializeValue(TOKEN_ACCESSED, ProtocolVersion.NEWEST_SUPPORTED));
        final Clause whereTokenInactive =
            QueryBuilder.eq("column1", DataType.serializeValue(TOKEN_INACTIVE, ProtocolVersion.NEWEST_SUPPORTED));

        final Assignment setAccessedTime =
            QueryBuilder.set("value", DataType.serializeValue(accessedTime, ProtocolVersion.NEWEST_SUPPORTED));
        final Assignment setInactiveTime =
            QueryBuilder.set("value", DataType.serializeValue(inactiveTime, ProtocolVersion.NEWEST_SUPPORTED));

        final Using usingTTL = QueryBuilder.ttl(ttl);

        if( inactiveTime != Long.MIN_VALUE){
            batchStatement.add(
                QueryBuilder
                    .update(TOKENS_TABLE)
                    .with(setInactiveTime)
                    .where(inKey).and(whereTokenInactive)
                    .using(usingTTL)
            );
        }

        batchStatement.add(
            QueryBuilder
            .update(TOKENS_TABLE)
            .with(setAccessedTime)
            .where(inKey).and(whereTokenAccessed)
            .using(usingTTL)
        );

        session.execute(batchStatement);

    }


    @Override
    public Map<String, Object> getTokenInfo(UUID tokenUUID){

        Preconditions.checkNotNull(tokenUUID, "token UUID is required");

        List<ByteBuffer> tokenProperties = new ArrayList<>();
        TOKEN_PROPERTIES.forEach( prop ->
            tokenProperties.add(DataType.serializeValue(prop, ProtocolVersion.NEWEST_SUPPORTED)));

        final ByteBuffer key = DataType.uuid().serialize(tokenUUID, ProtocolVersion.NEWEST_SUPPORTED);

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

            final String name = (String)DataType.text()
                .deserialize(row.getBytes("column1"), ProtocolVersion.NEWEST_SUPPORTED);
            final Object value = deserializeColumnValue(name, row.getBytes("value"));

            if (value == null){
                throw new RuntimeException("error deserializing token info for property: "+name);
            }

            tokenInfo.put(name, value);

        });

        logger.trace("getTokenInfo, info: {}", tokenInfo);

        return tokenInfo;
    }


    @Override
    public void putTokenInfo(final UUID tokenUUID, final Map<String, Object> tokenInfo,
                             final ByteBuffer principalKeyBuffer, final int ttl){

        Preconditions.checkNotNull(tokenUUID, "tokenUUID is required");
        Preconditions.checkNotNull(tokenUUID, "tokenInfo is required");
        Preconditions.checkArgument(ttl > -1 , "ttl is required to be positive");

        logger.trace("putTokenInfo, token UUID: {}, tokenInfo: {}, ttl: {}", tokenUUID, tokenInfo, ttl);

        final BatchStatement batchStatement = new BatchStatement();
        final Using usingTTL = QueryBuilder.ttl(ttl);

        tokenInfo.forEach((key, value) -> {

            ByteBuffer valueBuffer;
            if(key.equalsIgnoreCase(TOKEN_STATE)){
                valueBuffer = toByteBuffer(value);
            }else{
                valueBuffer = DataType.serializeValue(value, ProtocolVersion.NEWEST_SUPPORTED);
            }

            batchStatement.add(
                QueryBuilder.insertInto(TOKENS_TABLE)
                    .value("key", DataType.serializeValue(tokenUUID, ProtocolVersion.NEWEST_SUPPORTED))
                    .value("column1", DataType.serializeValue(key, ProtocolVersion.NEWEST_SUPPORTED))
                    .value("value", valueBuffer)
                    .using(usingTTL));

        });

        if(principalKeyBuffer != null){

            batchStatement.add(
                QueryBuilder.insertInto(PRINCIPAL_TOKENS_TABLE)
                    .value("key", principalKeyBuffer)
                    .value("column1", DataType.serializeValue(tokenUUID, ProtocolVersion.NEWEST_SUPPORTED))
                    .value("value", ByteBuffer.wrap( new byte[] { 0 } ))
                    .using(usingTTL));

        }

        session.execute(batchStatement);

    }


    @Override
    public List<UUID> getTokensForPrincipal(ByteBuffer principalKeyBuffer){

        Preconditions.checkNotNull(principalKeyBuffer, "principal key bytebuffer cannot be null");

        Clause inPrincipal = QueryBuilder.eq("key", principalKeyBuffer);
        Statement statement = QueryBuilder
            .select()
            .column("column1")
            .from(PRINCIPAL_TOKENS_TABLE)
            .where(inPrincipal);

        final List<Row> rows = session.execute(statement).all();
        final List<UUID> tokenUUIDs = new ArrayList<>(rows.size());

        rows.forEach(row -> tokenUUIDs.add(row.getUUID("column1")));

        logger.trace("getTokensForPrincipal, token UUIDs: {}", tokenUUIDs);

        return tokenUUIDs;
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
                return fromByteBuffer(bb, Object.class);
        }

        return null;
    }


    private ByteBuffer toByteBuffer( Object obj ) {
        if ( obj == null ) {
            return null;
        }

        byte[] bytes = null;
        try {
            bytes = smileMapper.writeValueAsBytes( obj );
        }
        catch ( Exception e ) {
            logger.error( "Error getting SMILE bytes", e );
        }
        if ( bytes != null ) {
            return ByteBuffer.wrap( bytes );
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

}
