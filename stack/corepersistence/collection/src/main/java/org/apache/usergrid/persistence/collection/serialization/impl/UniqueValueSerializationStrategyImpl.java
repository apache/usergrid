/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.*;

import com.datastax.driver.core.*;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Using;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * Reads and writes to UniqueValues column family.
 */
public abstract class UniqueValueSerializationStrategyImpl<FieldKey, EntityKey>
    implements UniqueValueSerializationStrategy {

    private static final Logger log = LoggerFactory.getLogger( UniqueValueSerializationStrategyImpl.class );

    public static final String UUID_TYPE_REVERSED = "UUIDType(reversed=true)";



    private final MultiTenantColumnFamily<ScopedRowKey<FieldKey>, EntityVersion>
        CF_UNIQUE_VALUES;


    private final MultiTenantColumnFamily<ScopedRowKey<EntityKey>, UniqueFieldEntry>
        CF_ENTITY_UNIQUE_VALUE_LOG ;

    private final String TABLE_UNIQUE_VALUES;
    private final String TABLE_UNIQUE_VALUES_LOG;


    private final Map COLUMNS_UNIQUE_VALUES;
    private final Map COLUMNS_UNIQUE_VALUES_LOG;



    public static final int COL_VALUE = 0x0;


    private final SerializationFig serializationFig;
    protected final Keyspace keyspace;
    private final CassandraFig cassandraFig;

    private final Session session;
    private final CassandraConfig cassandraConfig;


    /**
     * Construct serialization strategy for keyspace.
     *
     * @param keyspace Keyspace in which to store Unique Values.
     * @param cassandraFig The cassandra configuration
     * @param serializationFig The serialization configuration
     */
    public UniqueValueSerializationStrategyImpl( final Keyspace keyspace, final CassandraFig cassandraFig,
                                                 final SerializationFig serializationFig,
                                                 final Session session, final CassandraConfig cassandraConfig) {
        this.keyspace = keyspace;
        this.cassandraFig = cassandraFig;
        this.serializationFig = serializationFig;

        this.session = session;
        this.cassandraConfig = cassandraConfig;

        CF_UNIQUE_VALUES = getUniqueValuesCF();
        CF_ENTITY_UNIQUE_VALUE_LOG = getEntityUniqueLogCF();

        TABLE_UNIQUE_VALUES = getUniqueValuesTable().getTableName();
        TABLE_UNIQUE_VALUES_LOG = getEntityUniqueLogTable().getTableName();

        COLUMNS_UNIQUE_VALUES = getUniqueValuesTable().getColumns();
        COLUMNS_UNIQUE_VALUES_LOG = getEntityUniqueLogTable().getColumns();

    }

    @Override
    public BatchStatement writeCQL( final ApplicationScope collectionScope, final UniqueValue value,
                           final int timeToLive  ){


        Preconditions.checkNotNull( value, "value is required" );

        BatchStatement batch = new BatchStatement();

        Using ttl = null;
        if(timeToLive > 0){

            ttl = QueryBuilder.ttl(timeToLive);

        }

        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();
        final Field<?> field = value.getField();

        ValidationUtils.verifyIdentity( entityId );
        ValidationUtils.verifyVersion( entityVersion );

        final EntityVersion ev = new EntityVersion( entityId, entityVersion );
        final UniqueFieldEntry uniqueFieldEntry = new UniqueFieldEntry( entityVersion, field );

        ByteBuffer partitionKey = getPartitionKey(collectionScope.getApplication(), value.getEntityId().getType(),
            field.getTypeName().toString(), field.getName(), field.getValue());

        ByteBuffer logPartitionKey = getLogPartitionKey(collectionScope.getApplication(), value.getEntityId());


        if(ttl != null) {

            Statement uniqueValueStatement = QueryBuilder.insertInto(TABLE_UNIQUE_VALUES)
                .value("key", partitionKey)
                .value("column1", serializeUniqueValueColumn(ev))
                .value("value", DataType.serializeValue(COL_VALUE, ProtocolVersion.NEWEST_SUPPORTED))
                .using(ttl);

            batch.add(uniqueValueStatement);


        }else{

            Statement uniqueValueStatement = QueryBuilder.insertInto(TABLE_UNIQUE_VALUES)
                .value("key", partitionKey)
                .value("column1", serializeUniqueValueColumn(ev))
                .value("value", DataType.serializeValue(COL_VALUE, ProtocolVersion.NEWEST_SUPPORTED));

            batch.add(uniqueValueStatement);

        }

        // we always want to retain the log entry, so never write with the TTL
        Statement uniqueValueLogStatement = QueryBuilder.insertInto(TABLE_UNIQUE_VALUES_LOG)
            .value("key", logPartitionKey)
            .value("column1", serializeUniqueValueLogColumn(uniqueFieldEntry))
            .value("value", DataType.serializeValue(COL_VALUE, ProtocolVersion.NEWEST_SUPPORTED));

        batch.add(uniqueValueLogStatement);



        return batch;

    }


    @Override
    public BatchStatement deleteCQL( final ApplicationScope scope, UniqueValue value){

        Preconditions.checkNotNull( value, "value is required" );

        final BatchStatement batch = new BatchStatement();

        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();
        final Field<?> field = value.getField();

        ValidationUtils.verifyIdentity( entityId );
        ValidationUtils.verifyVersion( entityVersion );


        final EntityVersion ev = new EntityVersion( entityId, entityVersion );
        final UniqueFieldEntry uniqueFieldEntry = new UniqueFieldEntry( entityVersion, field );


        ByteBuffer partitionKey = getPartitionKey( scope.getApplication(), value.getEntityId().getType(),
            value.getField().getTypeName().toString(), value.getField().getName(), value.getField().getValue());

        ByteBuffer columnValue = serializeUniqueValueColumn(ev);

        final Clause uniqueEqKey = QueryBuilder.eq("key", partitionKey );
        final Clause uniqueEqColumn = QueryBuilder.eq("column1", columnValue );
        Statement uniqueDelete = QueryBuilder.delete().from(TABLE_UNIQUE_VALUES).where(uniqueEqKey).and(uniqueEqColumn);
        batch.add(uniqueDelete);



        ByteBuffer logPartitionKey = getLogPartitionKey(scope.getApplication(), entityId);
        ByteBuffer logColumnValue = serializeUniqueValueLogColumn(uniqueFieldEntry);


        final Clause uniqueLogEqKey = QueryBuilder.eq("key", logPartitionKey );
        final Clause uniqueLogEqColumn = QueryBuilder.eq("column1", logColumnValue );

        Statement uniqueLogDelete = QueryBuilder.delete()
            .from(TABLE_UNIQUE_VALUES_LOG).where(uniqueLogEqKey).and( uniqueLogEqColumn);

        batch.add(uniqueLogDelete);



        return batch;
    }


    @Override
    public UniqueValueSet load( final ApplicationScope colScope, final String type, final Collection<Field> fields )
        throws ConnectionException {
        return load( colScope, com.netflix.astyanax.model.ConsistencyLevel.valueOf( cassandraFig.getAstyanaxReadCL() ), type, fields );
    }


    @Override
    public UniqueValueSet load( final ApplicationScope appScope, final com.netflix.astyanax.model.ConsistencyLevel consistencyLevel,
                                final String type, final Collection<Field> fields ) throws ConnectionException {

        Preconditions.checkNotNull( fields, "fields are required" );
        Preconditions.checkArgument( fields.size() > 0, "More than 1 field must be specified" );

        return loadCQL(appScope, com.datastax.driver.core.ConsistencyLevel.LOCAL_QUORUM, type, fields);

        //return loadLegacy( appScope, type, fields);

    }


    private UniqueValueSet loadCQL( final ApplicationScope appScope, final com.datastax.driver.core.ConsistencyLevel consistencyLevel,
                                final String type, final Collection<Field> fields ) throws ConnectionException {

        Preconditions.checkNotNull( fields, "fields are required" );
        Preconditions.checkArgument( fields.size() > 0, "More than 1 field must be specified" );


        final Id applicationId = appScope.getApplication();

        // row key = app UUID + app type + entityType + field type + field name + field value

        List<ByteBuffer> partitionKeys = new ArrayList<>( fields.size() );
        for ( Field field : fields ) {

            //log.info(Bytes.toHexString(getPartitionKey(applicationId, type, field.getTypeName().toString(), field.getName(), field.getValue())));

            partitionKeys.add(getPartitionKey(applicationId, type, field.getTypeName().toString(), field.getName(), field.getValue()));

        }

        final UniqueValueSetImpl uniqueValueSet = new UniqueValueSetImpl( fields.size() );

        final Clause inKey = QueryBuilder.in("key", partitionKeys );

        final Statement statement = QueryBuilder.select().all().from(TABLE_UNIQUE_VALUES)
            .where(inKey)
            .setConsistencyLevel(com.datastax.driver.core.ConsistencyLevel.LOCAL_QUORUM);

        final ResultSet resultSet = session.execute(statement);


        Iterator<com.datastax.driver.core.Row> results = resultSet.iterator();


        while( results.hasNext() ){

            final com.datastax.driver.core.Row unique = results.next();
            ByteBuffer partitionKey = unique.getBytes("key");
            ByteBuffer column = unique.getBytesUnsafe("column1");

            List<Object> keyContents = deserializePartitionKey(partitionKey);
            List<Object> columnContents = deserializeUniqueValueColumn(column);

            FieldTypeName fieldType;
            String name;
            String value;
            if(this instanceof UniqueValueSerializationStrategyV2Impl) {

                 fieldType = FieldTypeName.valueOf((String) keyContents.get(3));
                 name = (String) keyContents.get(4);
                 value = (String) keyContents.get(5);

            }else{

                fieldType = FieldTypeName.valueOf((String) keyContents.get(5));
                name = (String) keyContents.get(6);
                value = (String) keyContents.get(7);

            }

            Field field = getField(name, value, fieldType);


            final EntityVersion entityVersion = new EntityVersion(
                new SimpleId((UUID)columnContents.get(1), (String)columnContents.get(2)), (UUID)columnContents.get(0));


            final UniqueValueImpl uniqueValue =
              new UniqueValueImpl( field, entityVersion.getEntityId(), entityVersion.getEntityVersion() );

            uniqueValueSet.addValue(uniqueValue);

        }

        return uniqueValueSet;

    }




    @Override
    public Iterator<UniqueValue> getAllUniqueFields( final ApplicationScope collectionScope, final Id entityId ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );


        Clause inKey = QueryBuilder.in("key", getLogPartitionKey(collectionScope.getApplication(), entityId));

        Statement statement = QueryBuilder.select().all().from(TABLE_UNIQUE_VALUES_LOG)
            .where(inKey);

        return new AllUniqueFieldsIterator(session, statement, entityId);


    }



    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {

        final MultiTenantColumnFamilyDefinition uniqueLookupCF =
            new MultiTenantColumnFamilyDefinition( CF_UNIQUE_VALUES, BytesType.class.getSimpleName(),
                ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                MultiTenantColumnFamilyDefinition.CacheOption.KEYS );

        final MultiTenantColumnFamilyDefinition uniqueLogCF =
            new MultiTenantColumnFamilyDefinition( CF_ENTITY_UNIQUE_VALUE_LOG, BytesType.class.getSimpleName(),
                ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                MultiTenantColumnFamilyDefinition.CacheOption.KEYS );

        return Arrays.asList( uniqueLookupCF, uniqueLogCF );
    }

    @Override
    public Collection<TableDefinition> getTables() {

        final TableDefinition uniqueValues = getUniqueValuesTable();

        final TableDefinition uniqueValuesLog = getEntityUniqueLogTable();


        return Arrays.asList( uniqueValues, uniqueValuesLog );

    }


    /**
     * Get the column family for the unique fields
     */
    protected abstract MultiTenantColumnFamily<ScopedRowKey<FieldKey>, EntityVersion> getUniqueValuesCF();


    /**
     * Get the CQL table definition for the unique values log table
     */
    protected abstract TableDefinition getUniqueValuesTable();


    protected abstract List<Object> deserializePartitionKey(ByteBuffer bb);

    protected abstract ByteBuffer serializeUniqueValueLogColumn(UniqueFieldEntry fieldEntry);

    protected abstract ByteBuffer getPartitionKey(Id applicationId, String entityType, String fieldType, String fieldName, Object fieldValue );

    protected abstract ByteBuffer getLogPartitionKey(final Id applicationId, final Id uniqueValueId);

    protected abstract ByteBuffer serializeUniqueValueColumn(EntityVersion entityVersion);

    protected abstract List<Object> deserializeUniqueValueColumn(ByteBuffer bb);

    protected abstract List<Object> deserializeUniqueValueLogColumn(ByteBuffer bb);





        /**
         * Get the column family for the unique field CF
         */
    protected abstract MultiTenantColumnFamily<ScopedRowKey<EntityKey>, UniqueFieldEntry> getEntityUniqueLogCF();

    /**
     * Get the CQL table definition for the unique values log table
     */
    protected abstract TableDefinition getEntityUniqueLogTable();

    /**
     * Generate a key that is compatible with the column family
     *
     * @param applicationId The applicationId
     * @param uniqueValueId The uniqueValue
     */
    protected abstract EntityKey createEntityUniqueLogKey(final Id applicationId,  final Id uniqueValueId );


    public class AllUniqueFieldsIterator implements Iterable<UniqueValue>, Iterator<UniqueValue> {

        private final Session session;
        private final Statement query;
        private final Id entityId;

        private Iterator<Row> sourceIterator;



        public AllUniqueFieldsIterator( final Session session, final Statement query, final Id entityId){

            this.session = session;
            this.query = query;
            this.entityId = entityId;

        }


        @Override
        public Iterator<UniqueValue> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {

            if ( sourceIterator == null ) {

                advanceIterator();

                return sourceIterator.hasNext();
            }

            return sourceIterator.hasNext();
        }

        @Override
        public UniqueValue next() {

            com.datastax.driver.core.Row next = sourceIterator.next();

            ByteBuffer column = next.getBytesUnsafe("column1");

            List<Object> columnContents = deserializeUniqueValueLogColumn(column);

            UUID version = (UUID) columnContents.get(0);
            String name = (String) columnContents.get(1);
            String value = (String) columnContents.get(2);
            FieldTypeName fieldType = FieldTypeName.valueOf((String) columnContents.get(3));


            return new UniqueValueImpl(getField(name, value, fieldType), entityId, version);

        }

        private void advanceIterator() {

            sourceIterator = session.execute(query).iterator();
        }
    }

    private Field getField( String name, String value, FieldTypeName fieldType){

        Field field = null;

        switch ( fieldType ) {
            case BOOLEAN:
                field = new BooleanField( name, Boolean.parseBoolean( value ) );
                break;
            case DOUBLE:
                field = new DoubleField( name, Double.parseDouble( value ) );
                break;
            case FLOAT:
                field = new FloatField( name, Float.parseFloat( value ) );
                break;
            case INTEGER:
                field =  new IntegerField( name, Integer.parseInt( value ) );
                break;
            case LONG:
                field = new LongField( name, Long.parseLong( value ) );
                break;
            case STRING:
                field = new StringField( name, value );
                break;
            case UUID:
                field = new UUIDField( name, UUID.fromString( value ) );
                break;
        }

        return field;

    }

}
