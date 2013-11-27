package org.apache.usergrid.persistence.collection.serialization;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.cassandra.db.marshal.ReversedType;
import org.apache.cassandra.db.marshal.UUIDType;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.migration.CollectionColumnFamily;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntityImpl;
import org.apache.usergrid.persistence.model.entity.Entity;

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
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ObjectSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/** @author tnine */
@Singleton
public class MvccEntitySerializationStrategyImpl implements MvccEntitySerializationStrategy, Migration {


    private static final EntitySerializer SER = new EntitySerializer();


    private static final ColumnFamily<UUID, UUID> CF_ENTITY_DATA =
            new ColumnFamily<UUID, UUID>( "Entity_Version_Data", UUIDSerializer.get(), UUIDSerializer.get() );


    protected final Keyspace keyspace;


    @Inject
    public MvccEntitySerializationStrategyImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public MutationBatch write( final MvccEntity entity ) {
        Preconditions.checkNotNull( entity, "entity is required" );

        final UUID colName = entity.getVersion();
        final UUID entityId = entity.getUuid();

        final Optional<Entity> colValue = entity.getEntity();

        return doWrite( entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.putColumn( colName, SER.toByteBuffer( colValue ) );
            }
        } );
    }


    @Override
    public MvccEntity load( final CollectionContext context, final UUID entityId, final UUID version )
            throws ConnectionException {
        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );


        Column<UUID> column;

        try {
            column = keyspace.prepareQuery( CF_ENTITY_DATA ).getKey( entityId ).getColumn( version ).execute()
                             .getResult();
        }

        catch ( NotFoundException e ) {
            //swallow, there's just no column
            return null;
        }


        return new MvccEntityImpl( context, entityId, version, column.getValue( SER ) );
    }


    @Override
    public List<MvccEntity> load( final CollectionContext context, final UUID entityId, final UUID version,
                                  final int maxSize ) throws ConnectionException {

        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkArgument( maxSize > 0, "max Size must be greater than 0" );


        ColumnList<UUID> columns = keyspace.prepareQuery( CF_ENTITY_DATA ).getKey( entityId )
                                           .withColumnRange( version, null, false, maxSize ).execute().getResult();


        List<MvccEntity> results = new ArrayList<MvccEntity>( columns.size() );

        for ( Column<UUID> col : columns ) {
            results.add( new MvccEntityImpl( context, entityId, col.getName(), col.getValue( SER ) ) );
        }

        return results;
    }


    @Override
    public MutationBatch clear( final CollectionContext context, final UUID entityId, final UUID version ) {
        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );

        final Optional<Entity> value = Optional.absent();

        return doWrite( entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.putColumn( version, SER.toByteBuffer( value ) );
            }
        } );
    }


    @Override
    public MutationBatch delete( final CollectionContext context, final UUID entityId, final UUID version ) {
        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );


        return doWrite( entityId, new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.deleteColumn( version );
            }
        } );
    }


    @Override
    public Collection<CollectionColumnFamily> getColumnFamilies() {

        //create the CF entity data.  We want it reversed b/c we want the most recent version at the top of the
        //row for fast seeks
        CollectionColumnFamily cf = new CollectionColumnFamily( CF_ENTITY_DATA,
                ReversedType.class.getName() + "(" + UUIDType.class.getName() + ")", true );


        return Collections.singleton( cf );
    }


    /** Do the write on the correct row for the entity id with the operation */
    private MutationBatch doWrite( UUID entityId, RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();

        op.doOp( batch.withRow( CF_ENTITY_DATA, entityId ) );

        return batch;
    }


    /** Simple callback to perform puts and deletes with a common row setup code */
    private static interface RowOp {

        /** The operation to perform on the row */
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
