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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.DataCorruptionException;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.util.RangeBuilder;


/**
 * Reads and writes to UniqueValues column family.
 */
public class UniqueValueSerializationStrategyImpl implements UniqueValueSerializationStrategy {

    private static final Logger log = LoggerFactory.getLogger( UniqueValueSerializationStrategyImpl.class );


    private static final CollectionScopedRowKeySerializer<Field> ROW_KEY_SER =
        new CollectionScopedRowKeySerializer<>( UniqueFieldRowKeySerializer.get() );

    private static final EntityVersionSerializer ENTITY_VERSION_SER = new EntityVersionSerializer();

    private static final MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Field>>, EntityVersion>
        CF_UNIQUE_VALUES = new MultiTennantColumnFamily<>( "Unique_Values", ROW_KEY_SER, ENTITY_VERSION_SER );


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    private static final CollectionScopedRowKeySerializer<Id> ENTITY_ROW_KEY_SER =
        new CollectionScopedRowKeySerializer<>( ID_SER );



    private static final MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UniqueFieldEntry>
        CF_ENTITY_UNIQUE_VALUES =
        new MultiTennantColumnFamily<>( "Entity_Unique_Values", ENTITY_ROW_KEY_SER, UniqueFieldEntrySerializer.get() );

    public static final int COL_VALUE = 0x0;


    protected final Keyspace keyspace;


    /**
     * Construct serialization strategy for keyspace.
     *
     * @param keyspace Keyspace in which to store Unique Values.
     */
    @Inject
    public UniqueValueSerializationStrategyImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    public MutationBatch write( final CollectionScope collectionScope, UniqueValue value ) {


        Preconditions.checkNotNull( value, "value is required" );


        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();
        final Field<?> field = value.getField();

        ValidationUtils.verifyIdentity( entityId );
        ValidationUtils.verifyVersion( entityVersion );

        log.debug( "Writing unique value collectionScope={} id={} version={} name={} value={} ttl={} ", new Object[] {
            collectionScope.getName(), entityId, entityVersion, value.getField().getName(), value.getField().getValue()
        } );

        final EntityVersion ev = new EntityVersion( entityId, entityVersion );
        final UniqueFieldEntry uniqueFieldEntry = new UniqueFieldEntry( entityVersion, field );

        return doWrite( collectionScope, value, new UniqueValueSerializationStrategyImpl.RowOp() {

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
    public MutationBatch write( final CollectionScope collectionScope, final UniqueValue value, final int timeToLive ) {

        Preconditions.checkNotNull( value, "value is required" );
        Preconditions.checkArgument( timeToLive > 0, "timeToLive must be greater than 0 is required" );

        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();
        final Field<?> field = value.getField();

        ValidationUtils.verifyIdentity( entityId );
        ValidationUtils.verifyVersion( entityVersion );

        final EntityVersion ev = new EntityVersion( entityId, entityVersion );
        final UniqueFieldEntry uniqueFieldEntry = new UniqueFieldEntry( entityVersion, field );

        return doWrite( collectionScope, value, new UniqueValueSerializationStrategyImpl.RowOp() {

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
    public MutationBatch delete( final CollectionScope scope, UniqueValue value ) {

        Preconditions.checkNotNull( value, "value is required" );


        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();
        final Field<?> field = value.getField();

        ValidationUtils.verifyIdentity( entityId );
        ValidationUtils.verifyVersion( entityVersion );


        final EntityVersion ev = new EntityVersion( entityId, entityVersion );
        final UniqueFieldEntry uniqueFieldEntry = new UniqueFieldEntry( entityVersion, field );

        return doWrite( scope, value, new UniqueValueSerializationStrategyImpl.RowOp() {

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
     * @param collectionScope We need to use this when getting the keyspace
     */
    private MutationBatch doWrite( CollectionScope collectionScope, UniqueValue uniqueValue, RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();
        final CollectionPrefixedKey<Field> uniquePrefixedKey =
            new CollectionPrefixedKey<>( collectionScope.getName(), collectionScope.getOwner(), uniqueValue.getField() );


        op.doLookup( batch
            .withRow( CF_UNIQUE_VALUES, ScopedRowKey.fromKey( collectionScope.getApplication(), uniquePrefixedKey ) ) );


        final Id ownerId = collectionScope.getOwner();
        final String collectionName = collectionScope.getName();

        final CollectionPrefixedKey<Id> collectionPrefixedEntityKey =
            new CollectionPrefixedKey<>( collectionName, ownerId, uniqueValue.getEntityId() );


        op.doLog( batch.withRow( CF_ENTITY_UNIQUE_VALUES,
            ScopedRowKey.fromKey( collectionScope.getApplication(), collectionPrefixedEntityKey ) ) );


        return batch;
    }


    @Override
    public UniqueValueSet load(final CollectionScope collectionScope, final Collection<Field> fields )
            throws ConnectionException {

        Preconditions.checkNotNull( fields, "fields are required" );
        Preconditions.checkArgument( fields.size() > 0, "More than 1 field msut be specified" );


        final List<ScopedRowKey<CollectionPrefixedKey<Field>>> keys = new ArrayList<>( fields.size() );

        final Id applicationId = collectionScope.getApplication();
        final Id ownerId = collectionScope.getOwner();
        final String collectionName = collectionScope.getName();

        for ( Field field : fields ) {

            final CollectionPrefixedKey<Field> collectionPrefixedKey = new CollectionPrefixedKey<>( collectionName, ownerId, field );


            final ScopedRowKey<CollectionPrefixedKey<Field>> rowKey = ScopedRowKey.fromKey(applicationId, collectionPrefixedKey );

            keys.add( rowKey );
        }

        final UniqueValueSetImpl uniqueValueSet = new UniqueValueSetImpl( fields.size() );

        Iterator<Row<ScopedRowKey<CollectionPrefixedKey<Field>>, EntityVersion>> results =
                keyspace.prepareQuery( CF_UNIQUE_VALUES ).getKeySlice( keys )
                        .withColumnRange( new RangeBuilder().setLimit( 1 ).build() ).execute().getResult().iterator();


        while ( results.hasNext() )

        {

            final Row<ScopedRowKey<CollectionPrefixedKey<Field>>, EntityVersion> unique = results.next();


            final Field field = unique.getKey().getKey().getSubKey();

            final Iterator<Column<EntityVersion>> columnList = unique.getColumns().iterator();

            //sanity check, nothing to do, skip it
            if ( !columnList.hasNext()) {
                continue;
            }

            final EntityVersion entityVersion = columnList.next().getName();


            final UniqueValueImpl uniqueValue = new UniqueValueImpl(field, entityVersion.getEntityId(),
                    entityVersion.getEntityVersion() );

            uniqueValueSet.addValue( uniqueValue );
        }

        return uniqueValueSet;
    }


    @Override
    public Iterator<UniqueValue> getAllUniqueFields( final CollectionScope collectionScope, final Id entityId ) {


        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );


        final Id applicationId = collectionScope.getApplication();
        final Id ownerId = collectionScope.getOwner();
        final String collectionName = collectionScope.getName();

        final CollectionPrefixedKey<Id> collectionPrefixedKey =
                new CollectionPrefixedKey<>( collectionName, ownerId, entityId );


        final ScopedRowKey<CollectionPrefixedKey<Id>> rowKey =
                ScopedRowKey.fromKey( applicationId, collectionPrefixedKey );


        RowQuery<ScopedRowKey<CollectionPrefixedKey<Id>>, UniqueFieldEntry> query =
                keyspace.prepareQuery( CF_ENTITY_UNIQUE_VALUES ).getKey( rowKey ).withColumnRange(
                    ( UniqueFieldEntry ) null, null, false, 1000 );

        return new ColumnNameIterator( query, new UniqueEntryParser( entityId ), false );

    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp {

        /**
         * Execute the mutation into the lookup CF_UNIQUE_VALUES row
         * @param colMutation
         */
        void doLookup( ColumnListMutation<EntityVersion> colMutation );

        /**
         * Execute the mutation into the lCF_ENTITY_UNIQUE_VALUESLUE row
         * @param colMutation
         */
        void doLog( ColumnListMutation<UniqueFieldEntry> colMutation);
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
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {

        final MultiTennantColumnFamilyDefinition uniqueLookupCF =
                new MultiTennantColumnFamilyDefinition( CF_UNIQUE_VALUES, BytesType.class.getSimpleName(),
                        ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        final MultiTennantColumnFamilyDefinition uniqueLogCF =
                        new MultiTennantColumnFamilyDefinition( CF_ENTITY_UNIQUE_VALUES, BytesType.class.getSimpleName(),
                                ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                                MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        return Arrays.asList( uniqueLookupCF, uniqueLogCF);
    }
}
