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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
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
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Composites;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ByteBufferSerializer;
import com.netflix.astyanax.serializers.BytesArraySerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;

//import org.codehaus.jackson.JsonGenerationException;
//import org.codehaus.jackson.map.JsonMappingException;
//import org.codehaus.jackson.map.ObjectMapper;
//import org.codehaus.jackson.smile.SmileFactory;


/**
 * @author tnine
 */
@Singleton
public class MvccEntitySerializationStrategyImpl implements MvccEntitySerializationStrategy, Migration {


    private static final EntitySerializer SER = new EntitySerializer();

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    private static final ByteBufferSerializer BUFFER_SERIALIZER = ByteBufferSerializer.get();

    private static final BytesArraySerializer BYTES_ARRAY_SERIALIZER = BytesArraySerializer.get();


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

        return doWrite( collectionScope, entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.putColumn( colName,
                        SER.toByteBuffer( new EntityWrapper( entity.getStatus(), entity.getEntity() ) ) );
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
            column = keyspace.prepareQuery( CF_ENTITY_DATA ).getKey( ScopedRowKey.fromKey( collectionScope, entityId ) )
                             .getColumn( version ).execute().getResult();
        }

        catch ( NotFoundException e ) {
            //swallow, there's just no column
            return null;
        }
        catch ( ConnectionException e ) {
            throw new CollectionRuntimeException( "An error occurred connecting to cassandra", e );
        }


        return getEntity( entityId, column );
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
                    keyspace.prepareQuery( CF_ENTITY_DATA ).getKey( ScopedRowKey.fromKey( collectionScope, entityId ) )
                            .withColumnRange( version, null, false, maxSize ).execute().getResult();
        }
        catch ( ConnectionException e ) {
            throw new CollectionRuntimeException( "An error occurred connecting to cassandra", e );
        }


        List<MvccEntity> results = new ArrayList<MvccEntity>( columns.size() );

        for ( Column<UUID> col : columns ) {
            results.add( getEntity( entityId, col ) );
        }

        return results;
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
                colMutation.putColumn( version, SER.toByteBuffer(
                        new EntityWrapper( MvccEntity.Status.COMPLETE, Optional.<Entity>absent() ) ) );
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
                BytesType.class.getSimpleName(), ReversedType.class.getSimpleName() + "(" + UUIDType.class.getSimpleName() + ")", BytesType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );


        return Collections.singleton( cf );
    }


    /**
     * Do the write on the correct row for the entity id with the operation
     */
    private MutationBatch doWrite( final CollectionScope collectionScope, final Id entityId, final RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();

        op.doOp( batch.withRow( CF_ENTITY_DATA, ScopedRowKey.fromKey( collectionScope, entityId ) ) );

        return batch;
    }


    private MvccEntity getEntity( final Id entityId, final Column<UUID> col ) {

        final UUID version = col.getName();

        final EntityWrapper deSerialized = col.getValue( SER );

        //Inject the id into it.
        if ( deSerialized.entity.isPresent() ) {
            EntityUtils.setId( deSerialized.entity.get(), entityId );
        }


        return new MvccEntityImpl( entityId, version, deSerialized.status, deSerialized.entity );
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
     * Simple bean wrapper for status and entity
     */
    private static class EntityWrapper {
        private final MvccEntity.Status status;
        private final Optional<Entity> entity;


        private EntityWrapper( final MvccEntity.Status status, final Optional<Entity> entity ) {
            this.status = status;
            this.entity = entity;
        }
    }

    public static class EntitySerializer extends AbstractSerializer<EntityWrapper> {

        public static final SmileFactory f = new SmileFactory(  );

        public static ObjectMapper mapper = new ObjectMapper(  );

        private static byte[] STATE_COMPLETE = new byte[] { 0 };
        private static byte[] STATE_DELETED = new byte[] { 1 };
        private static byte[] STATE_PARTIAL = new byte[] { 2 };

        private static byte[] VERSION = new byte[] { 0 };


        //the marker for when we're passed a "null" value
        private static final byte[] EMPTY = new byte[] { 0x0 };

//TODO:Make sure your exceptions provide descriptive error messages.
        @Override
        public ByteBuffer toByteBuffer( final EntityWrapper wrapper ) {
            if ( wrapper == null ) {
                return null;
            }

            CompositeBuilder builder = Composites.newCompositeBuilder();

            builder.addBytes( VERSION );
            //mark this version as empty
            if ( !wrapper.entity.isPresent() ) {
                //we're empty
                builder.addBytes( STATE_DELETED );

                return builder.build();
            }

            //we have an entity

            if ( wrapper.status == MvccEntity.Status.COMPLETE ) {
                builder.addBytes( STATE_COMPLETE );
            }

            else {
                builder.addBytes( STATE_PARTIAL );
            }

            try {
                builder.addBytes( mapper.writeValueAsBytes( wrapper.entity.get() ) );
            }
            catch ( JsonMappingException e ) {
                e.printStackTrace();
            }
            catch ( JsonGenerationException e ) {
                e.printStackTrace();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }

            return builder.build();
        }

        @Override
        public EntityWrapper fromByteBuffer( final ByteBuffer byteBuffer ) {
           CompositeParser parser = Composites.newCompositeParser( byteBuffer );

            byte[] version = parser.read( BYTES_ARRAY_SERIALIZER );

            if ( !Arrays.equals( VERSION, version ) ) {
                throw new UnsupportedOperationException( "A version of type " + version + " is unsupported" );
            }

            byte[] state = parser.read( BYTES_ARRAY_SERIALIZER );

            /**
             * It's been deleted, remove it
             */
            if ( Arrays.equals( STATE_DELETED, state ) ) {
                return new EntityWrapper( MvccEntity.Status.COMPLETE, Optional.<Entity>absent() );
            }

            Entity storedEntity = null;

            ByteBuffer jsonBytes = parser.read(  BUFFER_SERIALIZER );

            try {

                byte[] array = jsonBytes.array();
                int start = jsonBytes.arrayOffset();
                int length = jsonBytes.remaining();
                storedEntity = mapper.readValue( array,start,length,Entity.class);
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }

            final Optional<Entity> entity = Optional.of( storedEntity );

            if ( Arrays.equals( STATE_COMPLETE, state ) ) {
                return new EntityWrapper( MvccEntity.Status.COMPLETE, entity );
            }

            //it's partial by default
            return new EntityWrapper( MvccEntity.Status.PARTIAL, entity );
        }
    }
}
