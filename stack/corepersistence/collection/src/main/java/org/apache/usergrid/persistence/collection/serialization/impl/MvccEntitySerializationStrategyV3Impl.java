package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netflix.astyanax.serializers.StringSerializer;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
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
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
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
    public final static int VERSION = 1;

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    private static final ScopedRowKeySerializer<Id> ROW_KEY_SER =  new ScopedRowKeySerializer<>( ID_SER );


    private static final MultiTennantColumnFamily<ScopedRowKey<Id>, Boolean> CF_ENTITY_DATA =
            new MultiTennantColumnFamily<>( "Entity_Version_Data_V3", ROW_KEY_SER, BooleanSerializer.get() );


    private static final Boolean COL_VALUE = Boolean.TRUE;


    private final EntitySerializer entitySerializer;

    private static final Logger log = LoggerFactory.getLogger( MvccEntitySerializationStrategyV3Impl.class );


    protected final Keyspace keyspace;
    protected final SerializationFig serializationFig;
    protected final CassandraFig cassandraFig;


    @Inject
    public MvccEntitySerializationStrategyV3Impl( final Keyspace keyspace, final SerializationFig serializationFig,
                                                  final CassandraFig cassandraFig, final MetricsFactory metricsFactory ) {
        this.keyspace = keyspace;
        this.serializationFig = serializationFig;
        this.cassandraFig = cassandraFig;
        this.entitySerializer = new EntitySerializer( serializationFig, metricsFactory );
    }


    @Override
    public MutationBatch write( final ApplicationScope applicationScope, final MvccEntity entity ) {
        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entity, "entity is required" );

        final Id entityId = entity.getId();
        final UUID version = entity.getVersion();

        Optional<EntityMap> map =  EntityMap.fromEntity(entity.getEntity());
        ByteBuffer byteBuffer = entitySerializer.toByteBuffer(
            new EntityWrapper(entityId,entity.getVersion(), entity.getStatus(), map.isPresent() ? map.get() : null, 0 )
        );

        entity.setSize(byteBuffer.array().length);

        return doWrite( applicationScope, entityId, version, colMutation -> colMutation.putColumn( COL_VALUE, byteBuffer ) );
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
            }, 10 ).collect( () -> new EntitySetImpl( entityIds.size() ), ( ( entitySet, rows ) -> {
                final Iterator<Row<ScopedRowKey<Id>, Boolean>> latestEntityColumns = rows.iterator();

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
               } ) ).toBlocking().last();



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
        Preconditions.checkNotNull(applicationScope, "applicationScope is required");
        Preconditions.checkNotNull(entityId, "entity id is required");
        Preconditions.checkNotNull(version, "version is required");


        return doWrite(applicationScope, entityId, version, colMutation ->
                colMutation.putColumn(COL_VALUE,
                    entitySerializer.toByteBuffer(new EntityWrapper(entityId, version, MvccEntity.Status.DELETED, null, 0))
                )
        );
    }


    @Override
    public MutationBatch delete( final ApplicationScope applicationScope, final Id entityId, final UUID version ) {
        Preconditions.checkNotNull( applicationScope, "applicationScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );


        return doWrite( applicationScope, entityId, version, colMutation -> colMutation.deleteColumn( Boolean.TRUE ) );
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
            Optional<Entity> entity = deSerialized.getOptionalEntity() ;
            return new MvccEntityImpl( id, deSerialized.getVersion(), deSerialized.getStatus(), entity, deSerialized.getSize());
        }
    }

    /**
     * We should only ever create this once, since this impl is a singleton
     */
    public final class EntitySerializer extends AbstractSerializer<EntityWrapper> {


        private final JsonFactory  JSON_FACTORY = new JsonFactory();

        private final ObjectMapper MAPPER = new ObjectMapper( JSON_FACTORY );
        private final Histogram bytesInHistorgram;
        private final Histogram bytesOutHistorgram;
        private final Timer bytesOutTimer;


        private SerializationFig serializationFig;


        public EntitySerializer( final SerializationFig serializationFig, final MetricsFactory metricsFactory) {
            this.serializationFig = serializationFig;
            this.bytesOutHistorgram = metricsFactory.getHistogram(MvccEntitySerializationStrategyV3Impl.class, "bytes.out");
            this.bytesOutTimer = metricsFactory.getTimer(MvccEntitySerializationStrategyV3Impl.class, "bytes.out");
            this.bytesInHistorgram = metricsFactory.getHistogram(MvccEntitySerializationStrategyV3Impl.class, "bytes.in");

            //                mapper.enable(SerializationFeature.INDENT_OUTPUT); don't indent output,
            // causes slowness
            MAPPER.enableDefaultTypingAsProperty( ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class" );
        }


        @Override
        public ByteBuffer toByteBuffer( final EntityWrapper wrapper ) {
            if (wrapper == null) {
                return null;
            }
            final byte[] wrapperBytes;
            //mark this version as empty
            if (wrapper.getEntityMap() == null) {
                //we're empty
                try {
                    return ByteBuffer.wrap(MAPPER.writeValueAsBytes(wrapper));
                } catch (JsonProcessingException jpe) {
                    throw new RuntimeException("Unable to serialize entity", jpe);
                }
            }

            //we have an entity but status is not complete don't allow it
            if (wrapper.getStatus() != MvccEntity.Status.COMPLETE) {
                throw new UnsupportedOperationException("Only states " + MvccEntity.Status.DELETED + " and " + MvccEntity.Status.COMPLETE + " are supported");
            }

            wrapper.setStatus(MvccEntity.Status.COMPLETE);

            //Convert to internal entity map
            try {
                wrapperBytes = MAPPER.writeValueAsBytes(wrapper);

                final int maxEntrySize = serializationFig.getMaxEntitySize();

                bytesInHistorgram.update(wrapperBytes.length);
                if (wrapperBytes.length > maxEntrySize) {
                    throw new EntityTooLargeException(Entity.fromMap(wrapper.getEntityMap()), maxEntrySize, wrapperBytes.length,
                        "Your entity cannot exceed " + maxEntrySize + " bytes. The entity you tried to save was "
                            + wrapperBytes.length + " bytes");
                }
            } catch (JsonProcessingException jpe) {
                throw new RuntimeException("Unable to serialize entity", jpe);
            }

            return ByteBuffer.wrap(wrapperBytes);
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

            EntityWrapper entityWrapper;


            try {
                Timer.Context time = bytesOutTimer.time();
                byte[] arr = byteBuffer.array();
                bytesOutHistorgram.update( arr == null ? 0 : arr.length);
                entityWrapper = MAPPER.readValue(arr, EntityWrapper.class);
                entityWrapper.size = arr.length;
                time.stop();
            }
            catch ( Exception e ) {
                if (log.isDebugEnabled()) {
                    log.debug("Entity Wrapper Deserialized: " + StringSerializer.get().fromByteBuffer(byteBuffer));
                }
                throw new DataCorruptionException("Unable to read entity data", e);
            }

            // it's been deleted, remove it
            if ( entityWrapper.getEntityMap() == null) {
                return new EntityWrapper( entityWrapper.getId(), entityWrapper.getVersion(),MvccEntity.Status.DELETED,null,0 );
            }

            entityWrapper.setStatus(MvccEntity.Status.COMPLETE);

            // it's partial by default
            return entityWrapper;
        }
    }

    /**
     * Simple bean wrapper for state and entity
     */
    public static class EntityWrapper {
        private Id id;
        private MvccEntity.Status status;
        private UUID version;
        private EntityMap entityMap;
        private long size;


        public EntityWrapper( ) {
        }
        public EntityWrapper( final Id id , final UUID version, final MvccEntity.Status status, final EntityMap entity, final long size ) {
            this.setStatus(status);
            this.version=  version;
            this.entityMap = entity;
            this.id = id;
            this.size = size;
        }

        /**
         * do not store status because its based on either the entity being null (deleted) or not null (complete)
         * @return
         */
        @JsonIgnore()
        public MvccEntity.Status getStatus() {
            return status;
        }

        public void setStatus(MvccEntity.Status status) {
            this.status = status;
        }

        @JsonSerialize()
        public Id getId() {
            return id;
        }

        @JsonSerialize()
        public UUID getVersion() {
            return version;
        }


        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        public EntityMap getEntityMap() {
            return entityMap;
        }


        @JsonIgnore
        public Optional<Entity> getOptionalEntity() {
            Entity entity = Entity.fromMap(getEntityMap());
            if(entity!=null){
                entity.setSize(getSize());
            }
            Optional<Entity> entityReturn = Optional.fromNullable(entity);
            //Inject the id into it.
            if (entityReturn.isPresent()) {
                EntityUtils.setId(entityReturn.get(), getId());
                EntityUtils.setVersion(entityReturn.get(), getVersion());
            }
            ;
            return entityReturn;
        }

        @JsonIgnore
        public long getSize() {
            return size;
        }
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
}
