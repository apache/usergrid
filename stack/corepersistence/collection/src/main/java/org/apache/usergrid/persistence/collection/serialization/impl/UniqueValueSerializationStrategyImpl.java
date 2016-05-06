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
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Using;
import com.netflix.astyanax.model.*;
import com.netflix.astyanax.util.RangeBuilder;
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
import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
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
import com.netflix.astyanax.query.RowQuery;


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


    public MutationBatch write( final ApplicationScope collectionScope, UniqueValue value ) {


        Preconditions.checkNotNull( value, "value is required" );


        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();
        final Field<?> field = value.getField();

        ValidationUtils.verifyIdentity( entityId );
        ValidationUtils.verifyVersion( entityVersion );


        final EntityVersion ev = new EntityVersion( entityId, entityVersion );
        final UniqueFieldEntry uniqueFieldEntry = new UniqueFieldEntry( entityVersion, field );

        return doWrite( collectionScope, value, new RowOp() {

            @Override
            public void doLookup( final ColumnListMutation<EntityVersion> colMutation ) {
                colMutation.putColumn( ev, COL_VALUE );
            }


            @Override
            public void doLog( final ColumnListMutation<UniqueFieldEntry> colMutation ) {
                colMutation.putColumn( uniqueFieldEntry, COL_VALUE );
            }
        } );
    }


    public MutationBatch write( final ApplicationScope collectionScope, final UniqueValue value,
                                final int timeToLive ) {

        Preconditions.checkNotNull( value, "value is required" );
        Preconditions.checkArgument( timeToLive > 0, "timeToLive must be greater than 0 is required" );

        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();
        final Field<?> field = value.getField();

        ValidationUtils.verifyIdentity( entityId );
        ValidationUtils.verifyVersion( entityVersion );

        final EntityVersion ev = new EntityVersion( entityId, entityVersion );
        final UniqueFieldEntry uniqueFieldEntry = new UniqueFieldEntry( entityVersion, field );

        return doWrite( collectionScope, value, new RowOp() {

            @Override
            public void doLookup( final ColumnListMutation<EntityVersion> colMutation ) {
                colMutation.putColumn( ev, COL_VALUE, timeToLive );
            }


            //we purposefully leave out TTL.  Worst case we issue deletes against tombstoned columns
            //best case, we clean up an invalid secondary index entry when the log is used
            @Override
            public void doLog( final ColumnListMutation<UniqueFieldEntry> colMutation ) {
                colMutation.putColumn( uniqueFieldEntry, COL_VALUE );
            }
        } );
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

        /**
         *  @Override
        public void doLookup( final ColumnListMutation<EntityVersion> colMutation ) {
        colMutation.putColumn( ev, COL_VALUE );
        }


         @Override
         public void doLog( final ColumnListMutation<UniqueFieldEntry> colMutation ) {
         colMutation.putColumn( uniqueFieldEntry, COL_VALUE );
         }
         */
    }


    @Override
    public MutationBatch delete( final ApplicationScope scope, UniqueValue value ) {

        Preconditions.checkNotNull( value, "value is required" );


        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();
        final Field<?> field = value.getField();

        ValidationUtils.verifyIdentity( entityId );
        ValidationUtils.verifyVersion( entityVersion );


        final EntityVersion ev = new EntityVersion( entityId, entityVersion );
        final UniqueFieldEntry uniqueFieldEntry = new UniqueFieldEntry( entityVersion, field );

        return doWrite( scope, value, new RowOp() {

            @Override
            public void doLookup( final ColumnListMutation<EntityVersion> colMutation ) {
                colMutation.deleteColumn( ev );
            }


            @Override
            public void doLog( final ColumnListMutation<UniqueFieldEntry> colMutation ) {
                colMutation.deleteColumn( uniqueFieldEntry );
            }
        } );
    }


    /**
     * Do the column update or delete for the given column and row key
     *
     * @param applicationScope We need to use this when getting the keyspace
     * @param uniqueValue The unique value to write
     * @param op The operation to write
     */
    private MutationBatch doWrite( ApplicationScope applicationScope, UniqueValue uniqueValue, RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();

        final Id applicationId = applicationScope.getApplication();

        final FieldKey fieldKey = createUniqueValueKey( applicationId, uniqueValue.getEntityId().getType(), uniqueValue.getField() );


        op.doLookup( batch.withRow( CF_UNIQUE_VALUES, ScopedRowKey.fromKey( applicationId, fieldKey ) ) );


        final EntityKey entityKey = createEntityUniqueLogKey( applicationId, uniqueValue.getEntityId() );

        op.doLog( batch.withRow( CF_ENTITY_UNIQUE_VALUE_LOG,
            ScopedRowKey.fromKey( applicationId, entityKey ) ) );


        if ( log.isTraceEnabled() ) {
            log.trace( "Writing unique value version={} name={} value={} ",
                    uniqueValue.getEntityVersion(), uniqueValue.getField().getName(),
                    uniqueValue.getField().getValue()
                );
        }


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


    private UniqueValueSet loadLegacy(final ApplicationScope appScope,
                                      final String type, final Collection<Field> fields) throws ConnectionException {
        final List<ScopedRowKey<FieldKey>> keys = new ArrayList<>( fields.size() );

        final Id applicationId = appScope.getApplication();

        for ( Field field : fields ) {

            final FieldKey key = createUniqueValueKey( applicationId, type,  field );


            final ScopedRowKey<FieldKey> rowKey =
                ScopedRowKey.fromKey( applicationId, key );

            keys.add( rowKey );
        }

        final UniqueValueSetImpl uniqueValueSet = new UniqueValueSetImpl( fields.size() );

        Iterator<com.netflix.astyanax.model.Row<ScopedRowKey<FieldKey>, EntityVersion>> results =
            keyspace.prepareQuery( CF_UNIQUE_VALUES ).setConsistencyLevel(com.netflix.astyanax.model.ConsistencyLevel.CL_LOCAL_QUORUM ).getKeySlice( keys )
                .withColumnRange( new RangeBuilder().setLimit( 1 ).build() ).execute().getResult().iterator();


        while ( results.hasNext() )

        {

            final com.netflix.astyanax.model.Row<ScopedRowKey<FieldKey>, EntityVersion> unique = results.next();


            final Field field = parseRowKey( unique.getKey() );

            final Iterator<Column<EntityVersion>> columnList = unique.getColumns().iterator();

            //sanity check, nothing to do, skip it
            if ( !columnList.hasNext() ) {
                continue;
            }

            final EntityVersion entityVersion = columnList.next().getName();


            final UniqueValueImpl uniqueValue =
                new UniqueValueImpl( field, entityVersion.getEntityId(), entityVersion.getEntityVersion() );

            uniqueValueSet.addValue( uniqueValue );
        }

        return uniqueValueSet;

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

            Field field = null;
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


        final Id applicationId = collectionScope.getApplication();

        final EntityKey entityKey = createEntityUniqueLogKey( applicationId, entityId );


        final ScopedRowKey<EntityKey> rowKey =
            ScopedRowKey.fromKey( applicationId, entityKey );


        RowQuery<ScopedRowKey<EntityKey>, UniqueFieldEntry> query =
            keyspace.prepareQuery( CF_ENTITY_UNIQUE_VALUE_LOG ).getKey( rowKey )
                    .withColumnRange( ( UniqueFieldEntry ) null, null, false, serializationFig.getBufferSize() );

        return new ColumnNameIterator( query, new UniqueEntryParser( entityId ), false );
    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private interface RowOp {

        /**
         * Execute the mutation into the lookup CF_UNIQUE_VALUES row
         */
        void doLookup( ColumnListMutation<EntityVersion> colMutation );

        /**
         * Execute the mutation into the lCF_ENTITY_UNIQUE_VALUESLUE row
         */
        void doLog( ColumnListMutation<UniqueFieldEntry> colMutation );
    }


    /**
     * Converts raw columns to the expected output
     */
    private static final class UniqueEntryParser implements ColumnParser<UniqueFieldEntry, UniqueValue> {

        private final Id entityId;


        private UniqueEntryParser( final Id entityId ) {this.entityId = entityId;}


        @Override
        public UniqueValue parseColumn( final Column<UniqueFieldEntry> column ) {
            final UniqueFieldEntry entry = column.getName();

            return new UniqueValueImpl( entry.getField(), entityId, entry.getVersion() );
        }
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


    /**
     * Generate a key that is compatible with the column family
     *
     * @param applicationId The applicationId
     * @param type The type in the field
     * @param field The field we're creating the key for
     */
    protected abstract FieldKey createUniqueValueKey(final Id applicationId, final String type, final Field field );

    /**
     * Parse the row key into the field
     * @param rowKey
     * @return
     */
    protected abstract Field parseRowKey(final ScopedRowKey<FieldKey> rowKey);


    protected abstract List<Object> deserializePartitionKey(ByteBuffer bb);


    protected abstract Object serializeUniqueValueLogColumn(UniqueFieldEntry fieldEntry);

    protected abstract ByteBuffer getPartitionKey(Id applicationId, String entityType, String fieldType, String fieldName, Object fieldValue );

    protected abstract ByteBuffer getLogPartitionKey(final Id applicationId, final Id uniqueValueId);

    protected abstract ByteBuffer serializeUniqueValueColumn(EntityVersion entityVersion);

    protected abstract List<Object> deserializeUniqueValueColumn(ByteBuffer bb);





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
}
