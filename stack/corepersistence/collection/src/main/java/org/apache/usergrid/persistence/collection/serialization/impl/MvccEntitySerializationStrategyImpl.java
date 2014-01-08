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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.ReversedType;
import org.apache.cassandra.db.marshal.UUIDType;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ObjectSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * @author tnine
 */
@Singleton
public class MvccEntitySerializationStrategyImpl implements MvccEntitySerializationStrategy, Migration {


    private static final EntitySerializer SER = new EntitySerializer();

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    private static final CollectionScopedRowKeySerializer<Id> ROW_KEY_SER = 
            new CollectionScopedRowKeySerializer<Id>( ID_SER );

    private static final MultiTennantColumnFamily<CollectionScope, Id, UUID> CF_ENTITY_DATA =
            new MultiTennantColumnFamily<CollectionScope, Id, UUID>( "Entity_Version_Data", ROW_KEY_SER,
                    UUIDSerializer.get() );


    protected final Keyspace keyspace;


    @Inject
    public MvccEntitySerializationStrategyImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public MutationBatch write( final CollectionScope collectionScope, final MvccEntity entity ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entity, "entity is required" );

        final UUID colName = entity.getVersion();
        final Id entityId = entity.getId();

        final Optional<Entity> colValue = entity.getEntity();

        return doWrite( collectionScope, entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.putColumn( colName, SER.toByteBuffer( colValue ) );
            }
        } );
    }


    @Override
    public MvccEntity load( final CollectionScope collectionScope, final Id entityId, final UUID version ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );


        Column<UUID> column;

        try {
            column = keyspace.prepareQuery( CF_ENTITY_DATA ).getKey( ScopedRowKey
                    .fromKey( collectionScope, entityId ) )
                             .getColumn( version ).execute().getResult();
        }

        catch ( NotFoundException e ) {
            //swallow, there's just no column
            return null;
        }
        catch ( ConnectionException e ) {
            throw new CollectionRuntimeException( "An error occurred connecting to cassandra", e );
        }


        return new MvccEntityImpl( entityId, version, getEntity( column, entityId ) );
    }


    @Override
    public List<MvccEntity> load( final CollectionScope collectionScope, final Id entityId, final UUID version,
                                  final int maxSize ) {

        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkArgument( maxSize > 0, "max Size must be greater than 0" );


        ColumnList<UUID> columns = null;
        try {
            columns =
                    keyspace.prepareQuery( CF_ENTITY_DATA ).getKey( ScopedRowKey
                            .fromKey( collectionScope, entityId ) )
                            .withColumnRange( version, null, false, maxSize ).execute().getResult();
        }
        catch ( ConnectionException e ) {
            throw new CollectionRuntimeException( "An error occurred connecting to cassandra", e );
        }


        List<MvccEntity> results = new ArrayList<MvccEntity>( columns.size() );

        for ( Column<UUID> col : columns ) {
            results.add( new MvccEntityImpl( entityId, col.getName(), getEntity( col, entityId ) ) );
        }

        return results;
    }


    @Override
    public MutationBatch clear( final CollectionScope collectionScope, final Id entityId, final UUID version ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );

        final Optional<Entity> value = Optional.absent();

        return doWrite( collectionScope, entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.putColumn( version, SER.toByteBuffer( value ) );
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
        MultiTennantColumnFamilyDefinition cf = new MultiTennantColumnFamilyDefinition( CF_ENTITY_DATA,
                ReversedType.class.getSimpleName() + "(" + UUIDType.class.getSimpleName() + ")",
                BytesType.class.getSimpleName(), BytesType.class.getSimpleName() );


        return Collections.singleton( cf );
    }


    /**
     * Do the write on the correct row for the entity id with the operation
     */
    private MutationBatch doWrite( final CollectionScope collectionScope, final Id entityId, final RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();

        op.doOp( batch.withRow( CF_ENTITY_DATA, ScopedRowKey.fromKey( collectionScope, entityId ) ));

        return batch;
    }


    /**
     * Set the id into the entity if it exists and return it.
     */
    private Optional<Entity> getEntity( final Column<UUID> column, final Id entityId ) {
        final Optional<Entity> deSerialized = column.getValue( SER );

        //Inject the id into it.
        if ( deSerialized.isPresent() ) {
            EntityUtils.setId( deSerialized.get(), entityId );
        }

        return deSerialized;
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
     * TODO: Serializer for the entity. This just uses object serialization, change this to use SMILE before production!
     * We want to retain the Optional wrapper.  It helps us easily mark something as cleaned without removing the column
     * and makes it obvious that the entity could be missing in the api
     */
    private static class EntitySerializer extends AbstractSerializer<Optional<Entity>> {

        private static final ObjectSerializer SER = ObjectSerializer.get();

        //the marker for when we're passed a "null" value
        private static final byte[] EMPTY = new byte[] { 0x0 };


        @Override
        public ByteBuffer toByteBuffer( final Optional<Entity> obj ) {

            //mark this version as empty
            if ( !obj.isPresent() ) {
                return ByteBuffer.wrap( EMPTY );
            }

            return SER.toByteBuffer( obj.get() );
        }


        @Override
        public Optional<Entity> fromByteBuffer( final ByteBuffer byteBuffer ) {

            final ByteBuffer check = byteBuffer.duplicate();

            if ( check.remaining() == 1 && check.get() == EMPTY[0] ) {
                return Optional.absent();
            }

            return Optional.of( ( Entity ) SER.fromByteBuffer( byteBuffer ) );
        }
    }
}
