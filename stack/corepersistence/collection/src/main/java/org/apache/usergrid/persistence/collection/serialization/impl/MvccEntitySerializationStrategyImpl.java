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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.ReversedType;
import org.apache.cassandra.db.marshal.UUIDType;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.exception.DataCorruptionException;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.EntityRepair;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ByteBufferSerializer;
import com.netflix.astyanax.serializers.BytesArraySerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * @author tnine
 */
public abstract class MvccEntitySerializationStrategyImpl implements MvccEntitySerializationStrategy {

    private static final Logger log = LoggerFactory.getLogger( MvccLogEntrySerializationStrategyImpl.class );



    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    private static final CollectionScopedRowKeySerializer<Id> ROW_KEY_SER =
            new CollectionScopedRowKeySerializer<Id>( ID_SER );

    private static final MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> CF_ENTITY_DATA =
            new MultiTennantColumnFamily<>( "Entity_Version_Data", ROW_KEY_SER, UUIDSerializer.get() );


    protected final Keyspace keyspace;
    protected final SerializationFig serializationFig;
    protected final EntityRepair repair;
    private final AbstractSerializer<EntityWrapper> entityJsonSerializer;


    @Inject
    public MvccEntitySerializationStrategyImpl( final Keyspace keyspace, final SerializationFig serializationFig ) {
        this.keyspace = keyspace;
        this.serializationFig = serializationFig;
        this.repair = new EntityRepairImpl( this, serializationFig );
        this.entityJsonSerializer = getEntitySerializer();
    }


    @Override
    public MutationBatch write( final CollectionScope collectionScope, final MvccEntity entity ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entity, "entity is required" );

        final UUID colName = entity.getVersion();
        final Id entityId = entity.getId();

        return doWrite( collectionScope, entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                try {
                    colMutation.putColumn( colName, entityJsonSerializer
                            .toByteBuffer( new EntityWrapper( entity.getStatus(), entity.getEntity() ) ) );
                }
                catch ( Exception e ) {
                    // throw better exception if we can
                    if ( entity != null || entity.getEntity().get() != null ) {
                        throw new CollectionRuntimeException( entity, collectionScope, e );
                    }
                    throw e;
                }
            }
        } );
    }


    @Override
    public EntitySet load( final CollectionScope collectionScope, final Collection<Id> entityIds,
                           final UUID maxVersion ) {


        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityIds, "entityIds is required" );
        Preconditions.checkArgument( entityIds.size() > 0, "entityIds is required" );
        Preconditions.checkNotNull( maxVersion, "version is required" );


        //didn't put the max in the error message, I don't want to take the string construction hit every time
        Preconditions.checkArgument( entityIds.size() <= serializationFig.getMaxLoadSize(),
                "requested size cannot be over configured maximum" );


        final Id applicationId = collectionScope.getApplication();
        final Id ownerId = collectionScope.getOwner();
        final String collectionName = collectionScope.getName();


        final List<ScopedRowKey<CollectionPrefixedKey<Id>>> rowKeys = new ArrayList<>( entityIds.size() );


        for ( final Id entityId : entityIds ) {
            final CollectionPrefixedKey<Id> collectionPrefixedKey =
                    new CollectionPrefixedKey<>( collectionName, ownerId, entityId );


            final ScopedRowKey<CollectionPrefixedKey<Id>> rowKey =
                    ScopedRowKey.fromKey( applicationId, collectionPrefixedKey );


            rowKeys.add( rowKey );
        }


        final Iterator<Row<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID>> latestEntityColumns;


        try {
            latestEntityColumns = keyspace.prepareQuery( CF_ENTITY_DATA ).getKeySlice( rowKeys )
                                          .withColumnRange( maxVersion, null, false, 1 ).execute().getResult()
                                          .iterator();
        }
        catch ( ConnectionException e ) {
            throw new CollectionRuntimeException( null, collectionScope, "An error occurred connecting to cassandra",
                    e );
        }


        final EntitySetImpl entitySetResults = new EntitySetImpl( entityIds.size() );

        while ( latestEntityColumns.hasNext() ) {
            final Row<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> row = latestEntityColumns.next();

            final ColumnList<UUID> columns = row.getColumns();

            if ( columns.size() == 0 ) {
                continue;
            }

            final Id entityId = row.getKey().getKey().getSubKey();

            final Column<UUID> column = columns.getColumnByIndex( 0 );

            final MvccEntity parsedEntity = new MvccColumnParser( entityId, entityJsonSerializer ).parseColumn( column );

            //we *might* need to repair, it's not clear so check before loading into result sets
            final MvccEntity maybeRepaired = repair.maybeRepair( collectionScope, parsedEntity );

            entitySetResults.addEntity( maybeRepaired );
        }

        return entitySetResults;
    }


    @Override
    public Iterator<MvccEntity> loadHistory( final CollectionScope collectionScope, final Id entityId,
                                             final UUID version, final int fetchSize ) {

        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkArgument( fetchSize > 0, "max Size must be greater than 0" );


        final Id applicationId = collectionScope.getApplication();
        final Id ownerId = collectionScope.getOwner();
        final String collectionName = collectionScope.getName();

        final CollectionPrefixedKey<Id> collectionPrefixedKey =
                new CollectionPrefixedKey<>( collectionName, ownerId, entityId );


        final ScopedRowKey<CollectionPrefixedKey<Id>> rowKey =
                ScopedRowKey.fromKey( applicationId, collectionPrefixedKey );


        RowQuery<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> query =
                keyspace.prepareQuery( CF_ENTITY_DATA ).getKey( rowKey )
                        .withColumnRange( null, version, true, fetchSize );

        return new ColumnNameIterator( query, new MvccColumnParser( entityId, entityJsonSerializer ), false );
    }


    @Override
    public MutationBatch mark( final CollectionScope collectionScope, final Id entityId, final UUID version ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );

        final Optional<Entity> value = Optional.absent();

        return doWrite( collectionScope, entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.putColumn( version, entityJsonSerializer
                        .toByteBuffer( new EntityWrapper( MvccEntity.Status.COMPLETE, Optional.<Entity>absent() ) ) );
            }
        } );
    }


    @Override
    public MutationBatch delete( final CollectionScope collectionScope, final Id entityId, final UUID version ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );


        return doWrite( collectionScope, entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.deleteColumn( version );
            }
        } );
    }


    @Override
    public java.util.Collection getColumnFamilies() {

        //create the CF entity data.  We want it reversed b/c we want the most recent version at the top of the
        //row for fast seeks
        MultiTennantColumnFamilyDefinition cf =
                new MultiTennantColumnFamilyDefinition( CF_ENTITY_DATA, BytesType.class.getSimpleName(),
                        ReversedType.class.getSimpleName() + "(" + UUIDType.class.getSimpleName() + ")",
                        BytesType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );


        return Collections.singleton( cf );
    }


    /**
     * Do the write on the correct row for the entity id with the operation
     */
    private MutationBatch doWrite( final CollectionScope collectionScope, final Id entityId, final RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();

        final Id applicationId = collectionScope.getApplication();
        final Id ownerId = collectionScope.getOwner();
        final String collectionName = collectionScope.getName();

        final CollectionPrefixedKey<Id> collectionPrefixedKey =
                new CollectionPrefixedKey<>( collectionName, ownerId, entityId );


        final ScopedRowKey<CollectionPrefixedKey<Id>> rowKey =
                ScopedRowKey.fromKey( applicationId, collectionPrefixedKey );


        op.doOp( batch.withRow( CF_ENTITY_DATA, rowKey ) );

        return batch;
    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp {

        /**
         * The operation to perform on the row
         */
        void doOp( ColumnListMutation<UUID> colMutation );
    }


    /**
     * Simple bean wrapper for state and entity
     */
    protected static class EntityWrapper {
        protected final MvccEntity.Status status;
        protected final Optional<Entity> entity;


        protected EntityWrapper( final MvccEntity.Status status, final Optional<Entity> entity ) {
            this.status = status;
            this.entity = entity;
        }
    }


    /**
     * Converts raw columns the to MvccEntity representation
     */
    private static final class MvccColumnParser implements ColumnParser<UUID, MvccEntity> {

        private final Id id;
        private final AbstractSerializer<EntityWrapper> entityJsonSerializer;


        private MvccColumnParser( Id id, final AbstractSerializer<EntityWrapper> entityJsonSerializer ) {
            this.id = id;
            this.entityJsonSerializer = entityJsonSerializer;
        }


        @Override
        public MvccEntity parseColumn( Column<UUID> column ) {

            final EntityWrapper deSerialized;
            final UUID version = column.getName();

            try {
                deSerialized = column.getValue( entityJsonSerializer );
            }
            catch ( DataCorruptionException e ) {
              log.error(
                      "DATA CORRUPTION DETECTED when de-serializing entity with Id {} and version {}.  This means the"
                              + " write was truncated.",
                      id, version );
                //return an empty entity, we can never load this one, and we don't want it to bring the system
                //to a grinding halt
                return new MvccEntityImpl( id, version, MvccEntity.Status.DELETED, Optional.<Entity>absent() );
            }

            //Inject the id into it.
            if ( deSerialized.entity.isPresent() ) {
                EntityUtils.setId( deSerialized.entity.get(), id );
            }

            return new MvccEntityImpl( id, version, deSerialized.status, deSerialized.entity );
        }
    }


    /**
     * Return the entity serializer for this instance
     * @return
     */
    protected abstract AbstractSerializer<EntityWrapper> getEntitySerializer();
}
