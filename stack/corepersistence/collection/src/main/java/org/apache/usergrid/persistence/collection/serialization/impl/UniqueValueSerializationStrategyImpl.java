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


import java.util.*;

import com.netflix.astyanax.util.RangeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.query.RowQuery;


/**
 * Reads and writes to UniqueValues column family.
 */
public abstract class UniqueValueSerializationStrategyImpl<FieldKey, EntityKey>
    implements UniqueValueSerializationStrategy {

    private static final Logger logger = LoggerFactory.getLogger( UniqueValueSerializationStrategyImpl.class );


    private final MultiTenantColumnFamily<ScopedRowKey<FieldKey>, EntityVersion>
        CF_UNIQUE_VALUES;


    private final MultiTenantColumnFamily<ScopedRowKey<EntityKey>, UniqueFieldEntry>
        CF_ENTITY_UNIQUE_VALUE_LOG ;

    public static final int COL_VALUE = 0x0;

    private final Comparator<UniqueValue> uniqueValueComparator = new UniqueValueComparator();


    private final SerializationFig serializationFig;
    protected final Keyspace keyspace;
    private final CassandraFig cassandraFig;


    /**
     * Construct serialization strategy for keyspace.
     *
     * @param keyspace Keyspace in which to store Unique Values.
     * @param cassandraFig The cassandra configuration
     * @param serializationFig The serialization configuration
     */
    public UniqueValueSerializationStrategyImpl( final Keyspace keyspace, final CassandraFig cassandraFig,
                                                 final SerializationFig serializationFig ) {
        this.keyspace = keyspace;
        this.cassandraFig = cassandraFig;
        this.serializationFig = serializationFig;

        CF_UNIQUE_VALUES = getUniqueValuesCF();
        CF_ENTITY_UNIQUE_VALUE_LOG = getEntityUniqueLogCF();
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


    @Override
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
            //best case, we clean up an invalid secondary index entry when the logger is used
            @Override
            public void doLog( final ColumnListMutation<UniqueFieldEntry> colMutation ) {
                colMutation.putColumn( uniqueFieldEntry, COL_VALUE );
            }
        } );
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


        if ( logger.isTraceEnabled() ) {
            logger.trace( "Building batch statement for unique value entity={} version={} name={} value={} ",
                uniqueValue.getEntityId().getUuid(), uniqueValue.getEntityVersion(),
                uniqueValue.getField().getName(), uniqueValue.getField().getValue() );
        }


        return batch;
    }


    @Override
    public UniqueValueSet load( final ApplicationScope colScope, final String type, final Collection<Field> fields )
        throws ConnectionException {
        return load( colScope, ConsistencyLevel.valueOf( cassandraFig.getReadCL() ), type, fields, false);
    }

    @Override
    public UniqueValueSet load( final ApplicationScope colScope, final String type, final Collection<Field> fields,
                                boolean useReadRepair)
        throws ConnectionException {
        return load( colScope, ConsistencyLevel.valueOf( cassandraFig.getReadCL() ), type, fields, useReadRepair);
    }



    @Override
    public UniqueValueSet load(final ApplicationScope appScope, final ConsistencyLevel consistencyLevel,
                               final String type, final Collection<Field> fields, boolean useReadRepair) throws ConnectionException {

        Preconditions.checkNotNull( fields, "fields are required" );
        Preconditions.checkArgument( fields.size() > 0, "More than 1 field must be specified" );


        final List<ScopedRowKey<FieldKey>> keys = new ArrayList<>( fields.size() );

        final Id applicationId = appScope.getApplication();

        for ( Field field : fields ) {

            final FieldKey key = createUniqueValueKey( applicationId, type,  field );


            final ScopedRowKey<FieldKey> rowKey =
                ScopedRowKey.fromKey( applicationId, key );

            keys.add( rowKey );
        }

        final UniqueValueSetImpl uniqueValueSet = new UniqueValueSetImpl( fields.size() );

        Iterator<Row<ScopedRowKey<FieldKey>, EntityVersion>> results =
            keyspace.prepareQuery( CF_UNIQUE_VALUES ).setConsistencyLevel( consistencyLevel ).getKeySlice( keys )
                .withColumnRange(new RangeBuilder().setLimit(serializationFig.getMaxLoadSize()).build())
                .execute().getResult().iterator();


        while ( results.hasNext() )

        {

            final Row<ScopedRowKey<FieldKey>, EntityVersion> unique = results.next();

            final Field field = parseRowKey( unique.getKey() );

            final Iterator<Column<EntityVersion>> columnList = unique.getColumns().iterator();

            //sanity check, nothing to do, skip it
            if ( !columnList.hasNext() ) {
                continue;
            }


            List<UniqueValue> candidates = new ArrayList<>();

            /**
             *  While iterating the columns, a rule is being enforced to only EVER return the oldest UUID.  This means
             *  the UUID with the oldest timestamp ( it was the original entity written for the unique value ).
             *
             *  We do this to prevent cycling of unique value -> entity UUID mappings as this data is ordered by the
             *  entity's version and not the entity's timestamp itself.
             *
             *  If newer entity UUIDs are encountered, they are removed from the unique value tables, however their
             *  backing serialized entity data is left in tact in case a cleanup / audit is later needed.
             */
            while (columnList.hasNext()) {

                final EntityVersion entityVersion = columnList.next().getName();

                final UniqueValue uniqueValue =
                    new UniqueValueImpl(field, entityVersion.getEntityId(), entityVersion.getEntityVersion());

                // set the initial candidate and move on
                if (candidates.size() == 0) {
                    candidates.add(uniqueValue);
                    continue;
                }

                if(!useReadRepair){

                    // take only the first
                    break;

                } else {


                    final int result = uniqueValueComparator.compare(uniqueValue, candidates.get(candidates.size() - 1));

                    if (result == 0) {

                        // do nothing, only versions can be newer and we're not worried about newer versions of same entity
                        if (logger.isTraceEnabled()) {
                            logger.trace("Current unique value [{}={}] entry has UUID [{}] equal to candidate UUID [{}]",
                                field.getName(), field.getValue().toString(), uniqueValue.getEntityId().getUuid(),
                                candidates.get(candidates.size() -1));
                        }

                        // update candidate w/ latest version
                        candidates.add(uniqueValue);

                    } else if (result < 0) {

                        // delete the duplicate from the unique value index
                        candidates.forEach(candidate -> {

                            try {

                                logger.warn("Duplicate unique value [{}={}] found for application [{}], removing newer " +
                                        "entry with entity id [{}] and entity version [{}]", field.getName(),
                                    field.getValue().toString(), applicationId.getUuid(),
                                    candidate.getEntityId().getUuid(), candidate.getEntityVersion());

                                delete(appScope, candidate).execute();

                            } catch (ConnectionException e) {
                               logger.error( "Unable to connect to cassandra during duplicate repair of [{}={}]",
                                   field.getName(), field.getValue().toString() );
                            }

                        });

                        // clear the transient candidates list
                        candidates.clear();

                        if (logger.isTraceEnabled()) {
                            logger.trace("Updating candidate unique value [{}={}] to entity id [{}] and " +
                                "entity version [{}]", field.getName(), field.getValue().toString(),
                                uniqueValue.getEntityId().getUuid(), uniqueValue.getEntityVersion());

                        }

                        // add our new candidate to the list
                        candidates.add(uniqueValue);


                    } else {

                        logger.warn("Duplicate unique value [{}={}] found for application [{}], removing newer entry " +
                                "with entity id [{}] and entity version [{}].", field.getName(), field.getValue().toString(),
                            applicationId.getUuid(), uniqueValue.getEntityId().getUuid(), uniqueValue.getEntityVersion());

                        // delete the duplicate from the unique value index
                        delete(appScope, uniqueValue).execute();


                    }

                }
            }

            // take the last candidate ( should be the latest version) and add to the result set

            final UniqueValue returnValue = candidates.get(candidates.size() -1);
            if(logger.isTraceEnabled()){
                logger.trace("Adding unique value [{}={}] with entity id [{}] and entity version [{}] to response set",
                    returnValue.getField().getName(), returnValue.getField().getValue().toString(),
                    returnValue.getEntityId().getUuid(), returnValue.getEntityVersion());
            }
            uniqueValueSet.addValue(returnValue);

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


    /**
     * Get the column family for the unique fields
     */
    protected abstract MultiTenantColumnFamily<ScopedRowKey<FieldKey>, EntityVersion> getUniqueValuesCF();


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


    /**
     * Get the column family for the unique field CF
     */
    protected abstract MultiTenantColumnFamily<ScopedRowKey<EntityKey>, UniqueFieldEntry> getEntityUniqueLogCF();

    /**
     * Generate a key that is compatible with the column family
     *
     * @param applicationId The applicationId
     * @param uniqueValueId The uniqueValue
     */
    protected abstract EntityKey createEntityUniqueLogKey(final Id applicationId,  final Id uniqueValueId );



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
