package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.marshal.BooleanType;
import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.exception.DataCorruptionException;
import org.apache.usergrid.persistence.collection.exception.EntityTooLargeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.util.LegacyScopeUtils;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.FieldBuffer;
import org.apache.usergrid.persistence.core.astyanax.FieldBufferBuilder;
import org.apache.usergrid.persistence.core.astyanax.FieldBufferParser;
import org.apache.usergrid.persistence.core.astyanax.FieldBufferSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.EntityMap;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
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
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.BooleanSerializer;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;


/**
 * V3 Serialization Implementation
 */
public class MvccEntitySerializationStrategyV3Impl implements MvccEntitySerializationStrategy {
    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    private static final ScopedRowKeySerializer<Id> ROW_KEY_SER =  new ScopedRowKeySerializer<>( ID_SER );


    private static final MultiTennantColumnFamily<ScopedRowKey<Id>, Boolean> CF_ENTITY_DATA =
            new MultiTennantColumnFamily<>( "Entity_Version_Data_V3", ROW_KEY_SER, BooleanSerializer.get() );

    private static final FieldBufferSerializer FIELD_BUFFER_SERIALIZER = FieldBufferSerializer.get();

    private static final Boolean COL_VALUE = Boolean.TRUE;


    private final EntitySerializer entitySerializer;

    private static final Logger log = LoggerFactory.getLogger( MvccLogEntrySerializationStrategyImpl.class );


    protected final Keyspace keyspace;
    protected final SerializationFig serializationFig;
    protected final CassandraFig cassandraFig;


    @Inject
    public MvccEntitySerializationStrategyV3Impl( final Keyspace keyspace, final SerializationFig serializationFig,
                                                  final CassandraFig cassandraFig ) {
        this.keyspace = keyspace;
        this.serializationFig = serializationFig;
        this.cassandraFig = cassandraFig;
        this.entitySerializer = new EntitySerializer( serializationFig );
    }


    @Override
    public MutationBatch write( final ApplicationScope applicationScope, final MvccEntity entity ) {
        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entity, "entity is required" );

        final Id entityId = entity.getId();
        final UUID version = entity.getVersion();

        return doWrite( applicationScope, entityId, version, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<Boolean> colMutation ) {
                colMutation.putColumn( COL_VALUE,
                        entitySerializer.toByteBuffer( new EntityWrapper( entity.getStatus(), entity.getVersion(), entity.getEntity() ) ) );
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


        if ( entityIds.size() > serializationFig.getMaxLoadSize() ) {
            throw new IllegalArgumentException(
                    "requested load size cannot be over configured maximum of " + serializationFig.getMaxLoadSize() );
        }


        final Id applicationId = applicationScope.getApplication();
        final Id ownerId = applicationId;


        final List<ScopedRowKey<Id>> rowKeys = new ArrayList<>( entityIds.size() );


        for ( final Id entityId : entityIds ) {

            final ScopedRowKey<Id> rowKey =
                    ScopedRowKey.fromKey( applicationId, entityId );


            rowKeys.add( rowKey );
        }

        /**
         * Our settings may mean we exceed our maximum thrift buffer size. If we do, we have to make multiple
         * requests, not just one.
         * Perform the calculations and the appropriate request patterns
         *
         */

        final int maxEntityResultSizeInBytes = serializationFig.getMaxEntitySize() * entityIds.size();

        //if we're less than 1, set the number of requests to 1
        final int numberRequests = Math.max( 1, maxEntityResultSizeInBytes / cassandraFig.getThriftBufferSize() );

        final int entitiesPerRequest = entityIds.size() / numberRequests;


        final Scheduler scheduler;

        //if it's a single request, run it on the same thread
        if ( numberRequests == 1 ) {
            scheduler = Schedulers.immediate();
        }
        //if it's more than 1 request, run them on the I/O scheduler
        else {
            scheduler = Schedulers.io();
        }


        final EntitySetImpl entitySetResults = Observable.from( rowKeys )
            //buffer our entities per request, then for that buffer, execute the query in parallel (if neccessary)
            .buffer( entitiesPerRequest ).flatMap( listObservable -> {


                //here, we execute our query then emit the items either in parallel, or on the current thread
                // if we have more than 1 request
                return Observable.just( listObservable ).map( scopedRowKeys -> {


                    try {
                        return keyspace.prepareQuery( CF_ENTITY_DATA ).getKeySlice( rowKeys )
                                       .withColumnSlice( COL_VALUE ).execute().getResult();
                    }
                    catch ( ConnectionException e ) {
                        throw new CollectionRuntimeException( null, applicationScope,
                            "An error occurred connecting to cassandra", e );
                    }
                } ).subscribeOn( scheduler );
            }, 10 )

            .reduce( new EntitySetImpl( entityIds.size() ), ( entitySet, rows ) -> {
                final Iterator<Row<ScopedRowKey<Id>, Boolean>> latestEntityColumns =
                    rows.iterator();

                while ( latestEntityColumns.hasNext() ) {
                    final Row<ScopedRowKey<Id>, Boolean> row = latestEntityColumns.next();

                    final ColumnList<Boolean> columns = row.getColumns();

                    if ( columns.size() == 0 ) {
                        continue;
                    }

                    final Id entityId = row.getKey().getKey();

                    final Column<Boolean> column = columns.getColumnByIndex( 0 );

                    final MvccEntity parsedEntity =
                        new MvccColumnParser( entityId, entitySerializer ).parseColumn( column );


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



        throw new UnsupportedOperationException( "This version does not support loading history" );
    }


    @Override
    public Iterator<MvccEntity> loadAscendingHistory( final ApplicationScope applicationScope, final Id entityId,
                                                      final UUID version, final int fetchSize ) {

        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkArgument( fetchSize > 0, "max Size must be greater than 0" );

        throw new UnsupportedOperationException( "This version does not support loading history" );
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


        return doWrite( applicationScope, entityId, version, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<Boolean> colMutation ) {
                colMutation.putColumn( COL_VALUE, entitySerializer.toByteBuffer(
                    new EntityWrapper( MvccEntity.Status.COMPLETE, version, Optional.<Entity>absent() ) ) );
            }
        } );
    }


    @Override
    public MutationBatch delete( final ApplicationScope applicationScope, final Id entityId, final UUID version ) {
        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );


        return doWrite( applicationScope, entityId, version, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<Boolean> colMutation ) {
                colMutation.deleteColumn( Boolean.TRUE );
            }
        } );
    }


    @Override
    public java.util.Collection getColumnFamilies() {

        //create the CF entity data.  We want it reversed b/c we want the most recent version at the top of the
        //row for fast seeks
        MultiTennantColumnFamilyDefinition cf =
                new MultiTennantColumnFamilyDefinition( CF_ENTITY_DATA, BytesType.class.getSimpleName(),
                        BooleanType.class.getSimpleName() ,
                        BytesType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );


        return Collections.singleton( cf );
    }


    /**
     * Do the write on the correct row for the entity id with the operation
     */
    private MutationBatch doWrite( final ApplicationScope applicationScope, final Id entityId, final UUID version, final RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();

        final Id applicationId = applicationScope.getApplication();

        final ScopedRowKey<Id> rowKey =
                ScopedRowKey.fromKey( applicationId, entityId );

        final long timestamp = version.timestamp();

        op.doOp( batch.withRow( CF_ENTITY_DATA, rowKey ).setTimestamp( timestamp  ) );

        return batch;
    }


    @Override
    public int getImplementationVersion() {
        return CollectionDataVersions.LOG_REMOVAL.getVersion();
    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp {

        /**
         * The operation to perform on the row
         */
        void doOp( ColumnListMutation<Boolean> colMutation );
    }


    /**
     * Simple bean wrapper for state and entity
     */
    protected static class EntityWrapper {
        protected final MvccEntity.Status status;
        protected final UUID version;
        protected final Optional<Entity> entity;


        protected EntityWrapper( final MvccEntity.Status status, final UUID version, final Optional<Entity> entity ) {
            this.status = status;
            this.version = version;
            this.entity = entity;
        }
    }


    /**
     * Converts raw columns the to MvccEntity representation
     */
    private static final class MvccColumnParser implements ColumnParser<Boolean, MvccEntity> {

        private final Id id;
        private final AbstractSerializer<EntityWrapper> entityJsonSerializer;


        private MvccColumnParser( final Id id, final AbstractSerializer<EntityWrapper> entityJsonSerializer ) {
            this.id = id;
            this.entityJsonSerializer = entityJsonSerializer;
        }


        @Override
        public MvccEntity parseColumn( Column<Boolean> column ) {

            final EntityWrapper deSerialized;

            try {
                deSerialized = column.getValue( entityJsonSerializer );
            }
            catch ( DataCorruptionException e ) {
                log.error(
                        "DATA CORRUPTION DETECTED when de-serializing entity with Id {}.  This means the"
                                + " write was truncated.", id, e );
                //return an empty entity, we can never load this one, and we don't want it to bring the system
                //to a grinding halt
                //TODO fix this
                return new MvccEntityImpl( id, UUIDGenerator.newTimeUUID(), MvccEntity.Status.DELETED, Optional.<Entity>absent() );
            }

            //Inject the id into it.
            if ( deSerialized.entity.isPresent() ) {
                EntityUtils.setId( deSerialized.entity.get(), id );
            }

            return new MvccEntityImpl( id, deSerialized.version, deSerialized.status, deSerialized.entity );
        }
    }


    /**
     * We should only ever create this once, since this impl is a singleton
     */
    public final class EntitySerializer extends AbstractSerializer<EntityWrapper> {


        private final SmileFactory SMILE_FACTORY = new SmileFactory();

        private final ObjectMapper MAPPER = new ObjectMapper( SMILE_FACTORY );


        private SerializationFig serializationFig;


        private byte STATE_COMPLETE = 0;
        private byte STATE_DELETED = 1;

        private byte VERSION = 1;


        public EntitySerializer( final SerializationFig serializationFig ) {
            this.serializationFig = serializationFig;

            //                mapper.enable(SerializationFeature.INDENT_OUTPUT); don't indent output,
            // causes slowness
            MAPPER.enableDefaultTypingAsProperty( ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class" );
        }


        @Override
        public ByteBuffer toByteBuffer( final EntityWrapper wrapper ) {
            if ( wrapper == null ) {
                return null;
            }

            //we always have a max of 3 fields
            FieldBufferBuilder builder = new FieldBufferBuilder( 3 );

            builder.addByte( VERSION );


            //write our version
            builder.addUUID( wrapper.version );

            //mark this version as empty
            if ( !wrapper.entity.isPresent() ) {
                //we're empty
                builder.addByte( STATE_DELETED );


                return FIELD_BUFFER_SERIALIZER.toByteBuffer( builder.build() );
            }


            //we have an entity

            if ( wrapper.status != MvccEntity.Status.COMPLETE ) {
                throw new UnsupportedOperationException( "Only states " + MvccEntity.Status.DELETED + " and " + MvccEntity.Status.COMPLETE + " are supported" );
            }


            builder.addByte( STATE_COMPLETE );


            //Get Entity
            final Entity entity = wrapper.entity.get();
            //Convert to internal entity map
            final EntityMap entityMap = EntityMap.fromEntity( entity );
            final byte[] entityBytes;
            try {
                entityBytes = MAPPER.writeValueAsBytes( entityMap );
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Unable to serialize entity", e );
            }


            final int maxEntrySize = serializationFig.getMaxEntitySize();

            if ( entityBytes.length > maxEntrySize ) {
                throw new EntityTooLargeException( entity, maxEntrySize, entityBytes.length,
                        "Your entity cannot exceed " + maxEntrySize + " bytes. The entity you tried to save was "
                                + entityBytes.length + " bytes" );
            }

            builder.addBytes( entityBytes );

            return FIELD_BUFFER_SERIALIZER.toByteBuffer( builder.build() );
        }


        @Override
        public EntityWrapper fromByteBuffer( final ByteBuffer byteBuffer ) {

            /**
             * We intentionally turn data corruption exceptions when we're unable to de-serialize
             * the data in cassandra.  If this occurs, we'll never be able to de-serialize it
             * and it should be considered lost.  This is an error that is occurring due to a bug
             * in serializing the entity.  This is a lazy recognition + repair signal for deployment with
             * existing systems.
             */

            final FieldBuffer fieldBuffer;

            try {
                fieldBuffer = FIELD_BUFFER_SERIALIZER.fromByteBuffer( byteBuffer );
            }
            catch ( Exception e ) {
                throw new DataCorruptionException( "Unable to de-serialze entity", e );
            }

            final FieldBufferParser parser = new FieldBufferParser( fieldBuffer );


            final byte version = parser.readByte();

            if ( VERSION != version ) {
                throw new UnsupportedOperationException( "A version of type " + version + " is unsupported" );
            }


            final UUID entityVersion = parser.readUUID();

            final byte state = parser.readByte();

            // it's been deleted, remove it

            if ( STATE_DELETED == state ) {
                return new EntityWrapper( MvccEntity.Status.DELETED, entityVersion, Optional.<Entity>absent() );
            }

            EntityMap storedEntity;

            byte[] array = parser.readBytes();
            try {

                //                String[] byteValues = s.substring(1, s.length() - 1).split(",");
                //                byte[] bytes = new byte[byteValues.length];
                //
                //                for (int i=0, len=bytes.length; i<len; i++) {
                //                    bytes[i] = Byte.parseByte(byteValues[i].trim());
                //                }
                //
                //                s = new String(bytes);
                storedEntity = MAPPER.readValue( array, EntityMap.class );
            }
            catch ( Exception e ) {
                throw new DataCorruptionException( "Unable to read entity data", e );
            }

            Entity entityObject = Entity.fromMap( storedEntity );

            final Optional<Entity> entity = Optional.of( entityObject );

            // it's partial by default
            return new EntityWrapper( MvccEntity.Status.COMPLETE, entityVersion, entity );
        }
    }
}
