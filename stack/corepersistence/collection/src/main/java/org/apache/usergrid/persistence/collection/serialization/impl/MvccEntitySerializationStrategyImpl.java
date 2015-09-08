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

import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.exception.DataCorruptionException;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.util.LegacyScopeUtils;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

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

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;


/**
 * @author tnine
 */
public abstract class MvccEntitySerializationStrategyImpl implements MvccEntitySerializationStrategy {

    private static final Logger log = LoggerFactory.getLogger( MvccLogEntrySerializationStrategyImpl.class );


    protected final Keyspace keyspace;
    protected final SerializationFig serializationFig;
    protected final CassandraFig cassandraFig;
    private final MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID>  columnFamily;


    @Inject
    public MvccEntitySerializationStrategyImpl( final Keyspace keyspace, final SerializationFig serializationFig,
                                                final CassandraFig cassandraFig ) {
        this.keyspace = keyspace;
        this.serializationFig = serializationFig;
        this.cassandraFig = cassandraFig;
         this.columnFamily = getColumnFamily();
    }


    @Override
    public MutationBatch write( final ApplicationScope applicationScope, final MvccEntity entity ) {
        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entity, "entity is required" );

        final UUID colName = entity.getVersion();
        final Id entityId = entity.getId();

        return doWrite( applicationScope, entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                    colMutation.putColumn( colName, getEntitySerializer()
                            .toByteBuffer( new EntityWrapper( entity.getStatus(), entity.getEntity() ) ) );
            }
        } );
    }


    @Override
    public EntitySet load( final ApplicationScope applicationScope, final Collection<Id> entityIds,
                           final UUID maxVersion ) {


        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entityIds, "entityIds is required" );
        Preconditions.checkArgument( entityIds.size() > 0, "entityIds is required" );
        Preconditions.checkNotNull( maxVersion, "version is required" );


        if( entityIds.size() > serializationFig.getMaxLoadSize()){
            throw new IllegalArgumentException(  "requested load size cannot be over configured maximum of " + serializationFig.getMaxLoadSize() );
        }


        final Id applicationId = applicationScope.getApplication();
        final Id ownerId = applicationId;

        final List<ScopedRowKey<CollectionPrefixedKey<Id>>> rowKeys = new ArrayList<>( entityIds.size() );


        for ( final Id entityId : entityIds ) {

            final String collectionName = LegacyScopeUtils.getCollectionScopeNameFromEntityType( entityId.getType() );

            final CollectionPrefixedKey<Id> collectionPrefixedKey =
                    new CollectionPrefixedKey<>( collectionName, ownerId, entityId );


            final ScopedRowKey<CollectionPrefixedKey<Id>> rowKey =
                    ScopedRowKey.fromKey( applicationId, collectionPrefixedKey );


            rowKeys.add( rowKey );
        }

        /**
         * Our settings may mean we exceed our maximum thrift buffer size. If we do, we have to make multiple requests, not just one.
         * Perform the calculations and the appropriate request patterns
         *
         */

        final int maxEntityResultSizeInBytes = serializationFig.getMaxEntitySize() * entityIds.size();

        //if we're less than 1, set the number of requests to 1
        final int numberRequests = Math.max(1, maxEntityResultSizeInBytes / cassandraFig.getThriftBufferSize());

        final int entitiesPerRequest = entityIds.size() / numberRequests;


        final Scheduler scheduler;

        //if it's a single request, run it on the same thread
        if(numberRequests == 1){
            scheduler = Schedulers.immediate();
        }
        //if it's more than 1 request, run them on the I/O scheduler
        else{
            scheduler = Schedulers.io();
        }


        final EntitySetImpl entitySetResults = Observable.from( rowKeys )
            //buffer our entities per request, then for that buffer, execute the query in parallel (if neccessary)
            .buffer( entitiesPerRequest ).flatMap( listObservable -> {


                //here, we execute our query then emit the items either in parallel, or on the current thread
                // if we have more than 1 request
                return Observable.just( listObservable ).map( scopedRowKeys -> {

                    try {
                        return keyspace.prepareQuery( columnFamily ).getKeySlice( rowKeys )
                                       .withColumnRange( maxVersion, null, false, 1 ).execute().getResult();
                    }
                    catch ( ConnectionException e ) {
                        throw new CollectionRuntimeException( null, applicationScope,
                            "An error occurred connecting to cassandra", e );
                    }
                } ).subscribeOn( scheduler );
            }, 10 )

            .reduce( new EntitySetImpl( entityIds.size() ), ( entitySet, rows ) -> {
                final Iterator<Row<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID>> latestEntityColumns =
                    rows.iterator();

                while ( latestEntityColumns.hasNext() ) {
                    final Row<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> row = latestEntityColumns.next();

                    final ColumnList<UUID> columns = row.getColumns();

                    if ( columns.size() == 0 ) {
                        continue;
                    }

                    final Id entityId = row.getKey().getKey().getSubKey();

                    final Column<UUID> column = columns.getColumnByIndex( 0 );

                    final MvccEntity parsedEntity =
                        new MvccColumnParser( entityId, getEntitySerializer() ).parseColumn( column );

                    entitySet.addEntity( parsedEntity );
                }


                return entitySet;
            } ).toBlocking().last();

        return entitySetResults;


    }


    @Override
    public Iterator<MvccEntity> loadDescendingHistory( final ApplicationScope applicationScope, final Id entityId,
                                                       final UUID version, final int fetchSize ) {

        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkArgument( fetchSize > 0, "max Size must be greater than 0" );


        final Id applicationId = applicationScope.getApplication();
        final Id ownerId = applicationId;
        final String collectionName = LegacyScopeUtils.getCollectionScopeNameFromEntityType( entityId.getType() );

        final CollectionPrefixedKey<Id> collectionPrefixedKey =
                new CollectionPrefixedKey<>( collectionName, ownerId, entityId );


        final ScopedRowKey<CollectionPrefixedKey<Id>> rowKey =
                ScopedRowKey.fromKey( applicationId, collectionPrefixedKey );


        RowQuery<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> query =
                keyspace.prepareQuery( columnFamily ).getKey( rowKey )
                        .withColumnRange( version, null, false, fetchSize );

        return new ColumnNameIterator( query, new MvccColumnParser( entityId, getEntitySerializer() ), false );
    }


    @Override
    public Iterator<MvccEntity> loadAscendingHistory( final ApplicationScope applicationScope, final Id entityId,
                                                      final UUID version, final int fetchSize ) {

        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkArgument( fetchSize > 0, "max Size must be greater than 0" );


        final Id applicationId = applicationScope.getApplication();
        final Id ownerId = applicationId;
        final String collectionName = LegacyScopeUtils.getCollectionScopeNameFromEntityType( entityId.getType() );

        final CollectionPrefixedKey<Id> collectionPrefixedKey =
                new CollectionPrefixedKey<>( collectionName, ownerId, entityId );


        final ScopedRowKey<CollectionPrefixedKey<Id>> rowKey =
                ScopedRowKey.fromKey( applicationId, collectionPrefixedKey );


        RowQuery<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> query =
                keyspace.prepareQuery( columnFamily ).getKey( rowKey )
                        .withColumnRange( null, version, true, fetchSize );

        return new ColumnNameIterator( query, new MvccColumnParser( entityId, getEntitySerializer() ), false );
    }


    @Override
    public Optional<MvccEntity> load( final ApplicationScope scope, final Id entityId ) {
        final EntitySet results = load( scope, Collections.singleton( entityId ), UUIDGenerator.newTimeUUID() );

        return Optional.fromNullable( results.getEntity( entityId ));
    }


    @Override
    public MutationBatch mark( final ApplicationScope applicationScope, final Id entityId, final UUID version ) {
        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );

        return doWrite( applicationScope, entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.putColumn( version, getEntitySerializer()
                        .toByteBuffer( new EntityWrapper( MvccEntity.Status.COMPLETE, Optional.<Entity>absent() ) ) );
            }
        } );
    }


    @Override
    public MutationBatch delete( final ApplicationScope applicationScope, final Id entityId, final UUID version ) {
        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );


        return doWrite( applicationScope, entityId, new RowOp() {
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
                new MultiTennantColumnFamilyDefinition( columnFamily, BytesType.class.getSimpleName(),
                        ReversedType.class.getSimpleName() + "(" + UUIDType.class.getSimpleName() + ")",
                        BytesType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );


        return Collections.singleton( cf );
    }


    /**
     * Do the write on the correct row for the entity id with the operation
     */
    private MutationBatch doWrite( final ApplicationScope applicationScope, final Id entityId, final RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();

        final Id applicationId = applicationScope.getApplication();
        final Id ownerId = applicationId;
        final String collectionName = LegacyScopeUtils.getCollectionScopeNameFromEntityType( entityId.getType() );

        final CollectionPrefixedKey<Id> collectionPrefixedKey =
                new CollectionPrefixedKey<>( collectionName, ownerId, entityId );


        final ScopedRowKey<CollectionPrefixedKey<Id>> rowKey =
                ScopedRowKey.fromKey( applicationId, collectionPrefixedKey );


        op.doOp( batch.withRow( columnFamily, rowKey ) );

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
                                + " write was truncated.", id, version, e );
                //return an empty entity, we can never load this one, and we don't want it to bring the system
                //to a grinding halt
                return new MvccEntityImpl( id, version, MvccEntity.Status.DELETED, Optional.<Entity>absent(),0 );
            }

            //Inject the id into it.
            if ( deSerialized.entity.isPresent() ) {
                EntityUtils.setId( deSerialized.entity.get(), id );
            }

            return new MvccEntityImpl( id, version, deSerialized.status, deSerialized.entity, 0 );
        }
    }


    /**
     * Return the entity serializer for this instance
     */
    protected abstract AbstractSerializer<EntityWrapper> getEntitySerializer();

    /**
     * Get the column family to perform operations with
     * @return
     */
    protected abstract MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> getColumnFamily();

}
