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
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;


/**
 * Reads and writes to UniqueValues column family.
 */
public abstract class UniqueValueSerializationStrategyImpl<FieldKey, EntityKey>
    implements UniqueValueSerializationStrategy {

    private static final Logger logger = LoggerFactory.getLogger( UniqueValueSerializationStrategyImpl.class );

    public static final String UUID_TYPE_REVERSED = "UUIDType(reversed=true)";


    private final String TABLE_UNIQUE_VALUES;
    private final String TABLE_UNIQUE_VALUES_LOG;

    public static final int COL_VALUE = 0x0;

    private final Comparator<UniqueValue> uniqueValueComparator = new UniqueValueComparator();


    private final SerializationFig serializationFig;
    protected final CassandraFig cassandraFig;

    private final Session session;
    private final CassandraConfig cassandraConfig;


    /**
     * Construct serialization strategy for keyspace.
     *
     * @param cassandraFig The cassandra configuration
     * @param serializationFig The serialization configuration
     */
    public UniqueValueSerializationStrategyImpl( final CassandraFig cassandraFig,
                                                 final SerializationFig serializationFig,
                                                 final Session session,
                                                 final CassandraConfig cassandraConfig) {
        this.cassandraFig = cassandraFig;
        this.serializationFig = serializationFig;

        this.session = session;
        this.cassandraConfig = cassandraConfig;

        TABLE_UNIQUE_VALUES = getUniqueValuesTable( cassandraFig ).getTableName();
        TABLE_UNIQUE_VALUES_LOG = getEntityUniqueLogTable( cassandraFig ).getTableName();
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


        if ( logger.isTraceEnabled() ) {
            logger.trace( "Building batch statement for unique value entity={} version={} name={} value={} ",
                value.getEntityId().getUuid(), value.getEntityVersion(),
                value.getField().getName(), value.getField().getValue() );
        }



        return batch;
    }


    @Override
    public UniqueValueSet load( final ApplicationScope colScope, final String type, final Collection<Field> fields ) {

        return load( colScope, ConsistencyLevel.valueOf( cassandraFig.getReadCl() ), type, fields, false );

    }

    @Override
    public UniqueValueSet load( final ApplicationScope colScope, final String type, final Collection<Field> fields,
                                boolean useReadRepair) {

        return load( colScope, ConsistencyLevel.valueOf( cassandraFig.getReadCl() ), type, fields, useReadRepair);

    }



    @Override
    public UniqueValueSet load( final ApplicationScope appScope,
                                final ConsistencyLevel consistencyLevel,
                                final String type, final Collection<Field> fields, boolean useReadRepair ) {


        Preconditions.checkNotNull( fields, "fields are required" );
        Preconditions.checkArgument( fields.size() > 0, "More than 1 field must be specified" );

        return loadCQL(appScope, consistencyLevel, type, fields, useReadRepair);

    }


    private UniqueValueSet loadCQL( final ApplicationScope appScope,
                                    final ConsistencyLevel consistencyLevel,
                                    final String type, final Collection<Field> fields, boolean useReadRepair ) {

        Preconditions.checkNotNull( fields, "fields are required" );
        Preconditions.checkArgument( fields.size() > 0, "More than 1 field must be specified" );


        final Id applicationId = appScope.getApplication();

        // row key = app UUID + app type + entityType + field type + field name + field value




        //List<ByteBuffer> partitionKeys = new ArrayList<>( fields.size() );

        final UniqueValueSetImpl uniqueValueSet = new UniqueValueSetImpl( fields.size() );


        for ( Field field : fields ) {

            //log.info(Bytes.toHexString(getPartitionKey(applicationId, type,
            // field.getTypeName().toString(), field.getName(), field.getValue())));

            //partitionKeys.add(getPartitionKey(applicationId, type,
            // field.getTypeName().toString(), field.getName(), field.getValue()));

            final Clause inKey = QueryBuilder.in("key", getPartitionKey(applicationId, type,
                field.getTypeName().toString(), field.getName(), field.getValue()) );

            final Statement statement = QueryBuilder.select().all().from(TABLE_UNIQUE_VALUES)
                .where(inKey)
                .setConsistencyLevel(consistencyLevel);

            final ResultSet resultSet = session.execute(statement);


            Iterator<com.datastax.driver.core.Row> results = resultSet.iterator();

            if( !results.hasNext()){
                if(logger.isTraceEnabled()){
                    logger.trace("No rows returned for unique value lookup of field: {}", field);
                }
            }


            List<UniqueValue> candidates = new ArrayList<>();

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

                Field returnedField = getField(name, value, fieldType);


                final EntityVersion entityVersion = new EntityVersion(
                    new SimpleId((UUID)columnContents.get(1), (String)columnContents.get(2)), (UUID)columnContents.get(0));
//            //sanity check, nothing to do, skip it
//            if ( !columnList.hasNext() ) {
//                if(logger.isTraceEnabled()){
//                    logger.trace("No cells exist in partition for unique value [{}={}]",
//                        field.getName(), field.getValue().toString());
//                }
//                continue;
//            }




                /**
                 *  While iterating the rows, a rule is enforced to only EVER return the oldest UUID for the field.
                 *  This means the UUID with the oldest timestamp ( it was the original entity written for
                 *  the unique value ).
                 *
                 *  We do this to prevent cycling of unique value -> entity UUID mappings as this data is ordered by the
                 *  entity's version and not the entity's timestamp itself.
                 *
                 *  If newer entity UUIDs are encountered, they are removed from the unique value tables, however their
                 *  backing serialized entity data is left in tact in case a cleanup / audit is later needed.
                 */


                final UniqueValue uniqueValue =
                    new UniqueValueImpl(returnedField, entityVersion.getEntityId(), entityVersion.getEntityVersion());

                // set the initial candidate and move on
                if (candidates.size() == 0) {
                    candidates.add(uniqueValue);

                    if (logger.isTraceEnabled()) {
                        logger.trace("First entry for unique value [{}={}] found for application [{}], adding " +
                                "entry with entity id [{}] and entity version [{}] to the candidate list and continuing",
                            returnedField.getName(), returnedField.getValue().toString(), applicationId.getType(),
                            uniqueValue.getEntityId().getUuid(), uniqueValue.getEntityVersion());
                    }

                    continue;
                }

                if(!useReadRepair){

                    // take only the first
                    if (logger.isTraceEnabled()) {
                        logger.trace("Read repair not enabled for this request of unique value [{}={}], breaking out" +
                            " of cell loop", returnedField.getName(), returnedField.getValue().toString());
                    }
                    break;

                } else {


                    final int result = uniqueValueComparator.compare(uniqueValue, candidates.get(candidates.size() - 1));

                    if (result == 0) {

                        // do nothing, only versions can be newer and we're not worried about newer versions of same entity
                        if (logger.isTraceEnabled()) {
                            logger.trace("Current unique value [{}={}] entry has UUID [{}] equal to candidate UUID [{}]",
                                returnedField.getName(), returnedField.getValue().toString(), uniqueValue.getEntityId().getUuid(),
                                candidates.get(candidates.size() -1));
                        }

                        // update candidate w/ latest version
                        candidates.add(uniqueValue);

                    } else if (result < 0) {

                        // delete the duplicate from the unique value index
                        candidates.forEach(candidate -> {

                            logger.warn("Duplicate unique value [{}={}] found for application [{}], removing newer " +
                                    "entry with entity id [{}] and entity version [{}]", returnedField.getName(),
                                returnedField.getValue().toString(), applicationId.getUuid(),
                                candidate.getEntityId().getUuid(), candidate.getEntityVersion());

                            session.execute(deleteCQL(appScope, candidate));

                        });

                        // clear the transient candidates list
                        candidates.clear();

                        if (logger.isTraceEnabled()) {
                            logger.trace("Updating candidate unique value [{}={}] to entity id [{}] and " +
                                    "entity version [{}]", returnedField.getName(), returnedField.getValue().toString(),
                                uniqueValue.getEntityId().getUuid(), uniqueValue.getEntityVersion());

                        }

                        // add our new candidate to the list
                        candidates.add(uniqueValue);


                    } else {

                        logger.warn("Duplicate unique value [{}={}] found for application [{}], removing newer entry " +
                                "with entity id [{}] and entity version [{}].", returnedField.getName(), returnedField.getValue().toString(),
                            applicationId.getUuid(), uniqueValue.getEntityId().getUuid(), uniqueValue.getEntityVersion());

                        // delete the duplicate from the unique value index
                        session.execute(deleteCQL(appScope, uniqueValue));


                    }

                }

            }

            if ( candidates.size() > 0 ) {
                // take the last candidate ( should be the latest version) and add to the result set
                final UniqueValue returnValue = candidates.get(candidates.size() - 1);
                if (logger.isTraceEnabled()) {
                    logger.trace("Adding unique value [{}={}] with entity id [{}] and entity version [{}] to response set",
                        returnValue.getField().getName(), returnValue.getField().getValue().toString(),
                        returnValue.getEntityId().getUuid(), returnValue.getEntityVersion());
                }
                uniqueValueSet.addValue(returnValue);
            }


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
    public abstract Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies();

    @Override
    public abstract Collection<TableDefinition> getTables();

    /**
     * Get the CQL table definition for the unique values log table
     */
    protected abstract TableDefinition getUniqueValuesTable( CassandraFig cassandraFig );


    protected abstract List<Object> deserializePartitionKey(ByteBuffer bb);

    protected abstract ByteBuffer serializeUniqueValueLogColumn(UniqueFieldEntry fieldEntry);

    protected abstract ByteBuffer getPartitionKey(Id applicationId, String entityType, String fieldType, String fieldName, Object fieldValue );

    protected abstract ByteBuffer getLogPartitionKey(final Id applicationId, final Id uniqueValueId);

    protected abstract ByteBuffer serializeUniqueValueColumn(EntityVersion entityVersion);

    protected abstract List<Object> deserializeUniqueValueColumn(ByteBuffer bb);

    protected abstract List<Object> deserializeUniqueValueLogColumn(ByteBuffer bb);



    /**
     * Get the CQL table definition for the unique values log table
     */
    protected abstract TableDefinition getEntityUniqueLogTable( CassandraFig cassandraFig );


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


    private class UniqueValueComparator implements Comparator<UniqueValue> {

        @Override
        public int compare(UniqueValue o1, UniqueValue o2) {

            if( o1.getEntityId().getUuid().equals(o2.getEntityId().getUuid())){

                return 0;

            }else if( o1.getEntityId().getUuid().timestamp() < o2.getEntityId().getUuid().timestamp()){

                return -1;

            }

            // if the UUIDs are not equal and o1's timestamp is not less than o2's timestamp,
            // then o1 must be greater than o2
            return 1;


        }
    }

}
