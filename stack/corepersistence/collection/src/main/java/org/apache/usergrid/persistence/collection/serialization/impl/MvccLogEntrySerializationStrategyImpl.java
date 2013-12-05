package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cassandra.db.marshal.ReversedType;
import org.apache.cassandra.db.marshal.UUIDType;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.migration.CollectionColumnFamily;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * Simple implementation for reading and writing log entries
 *
 * @author tnine
 */
@Singleton
public class MvccLogEntrySerializationStrategyImpl implements MvccLogEntrySerializationStrategy, Migration {

    public static final String TIMEOUT_PROP = "collection.stage.transient.timeout";

    private static final StageSerializer SER = new StageSerializer();

    private static final ColumnFamily<UUID, UUID> CF_ENTITY_LOG =
            new ColumnFamily<UUID, UUID>( "Entity_Log", UUIDSerializer.get(), UUIDSerializer.get() );


    protected final Keyspace keyspace;
    protected final int timeout;


    @Inject
    public MvccLogEntrySerializationStrategyImpl( final Keyspace keyspace, @Named( TIMEOUT_PROP ) final int timeout ) {
        this.keyspace = keyspace;
        this.timeout = timeout;
    }


    @Override
    public MutationBatch write( final EntityCollection context, final MvccLogEntry entry ) {

        Preconditions.checkNotNull( entry, "entry is required" );


        final Stage stage = entry.getStage();
        final UUID colName = entry.getVersion();

        return doWrite( context, entry.getEntityId(), new RowOp() {
            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {

                //Write the stage with a timeout, it's set as transient
                if ( stage.isTransient() ) {
                    colMutation.putColumn( colName, stage, SER, timeout );
                    return;
                }

                //otherwise it's persistent, write it with no expiration
                colMutation.putColumn( colName, stage, SER, null );
            }
        } );
    }


    @Override
    public MvccLogEntry load( final EntityCollection context, final UUID entityId, final UUID version )
            throws ConnectionException {
        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );


        Column<UUID> result;

        try {
            result = keyspace.prepareQuery( CF_ENTITY_LOG ).getKey( entityId ).getColumn( version ).execute()
                             .getResult();
        }
        catch ( NotFoundException nfe ) {
            return null;
        }


        final Stage stage = result.getValue( SER );

        return new MvccLogEntryImpl( entityId, version, stage );
    }


    @Override
    public List<MvccLogEntry> load( final EntityCollection context, final UUID entityId, final UUID version,
                                    final int maxSize ) throws ConnectionException {
        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkArgument( maxSize > 0, "max Size must be greater than 0" );


        ColumnList<UUID> columns = keyspace.prepareQuery( CF_ENTITY_LOG ).getKey( entityId )
                                           .withColumnRange( version, null, false, maxSize ).execute().getResult();


        List<MvccLogEntry> results = new ArrayList<MvccLogEntry>( columns.size() );

        for ( Column<UUID> col : columns ) {
            final UUID storedVersion = col.getName();
            final Stage stage = col.getValue( SER );

            results.add( new MvccLogEntryImpl( entityId, storedVersion, stage ) );
        }

        return results;
    }


    @Override
    public MutationBatch delete( final EntityCollection context, final UUID entityId, final UUID version ) {

        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entityId is required" );
        Preconditions.checkNotNull( version, "version context is required" );

        return doWrite( context, entityId, new RowOp() {
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
        CollectionColumnFamily cf = new CollectionColumnFamily( CF_ENTITY_LOG,
                ReversedType.class.getName() + "(" + UUIDType.class.getName() + ")", true );


        return Collections.singleton( cf );
    }


    /** Simple callback to perform puts and deletes with a common row setup code */
    private static interface RowOp {

        /** The operation to perform on the row */
        void doOp( ColumnListMutation<UUID> colMutation );
    }


    /**
     * Do the column update or delete for the given column and row key
     *
     * @param context We need to use this when getting the keyspace
     */
    private MutationBatch doWrite( EntityCollection context, UUID entityId, RowOp op ) {

        final MutationBatch batch = keyspace.prepareMutationBatch();

        op.doOp( batch.withRow( CF_ENTITY_LOG, entityId ) );

        return batch;
    }


    /** Internal stage cache */
    private static class StageCache {
        private Map<Byte, Stage> values = new HashMap<Byte, Stage>( Stage.values().length );


        private StageCache() {
            for ( Stage stage : Stage.values() ) {

                final byte stageValue = stage.getId();

                values.put( stageValue, stage );
            }
        }


        /** Get the stage with the byte value */
        private Stage getStage( final byte value ) {
            return values.get( value );
        }
    }


    public static class StageSerializer extends AbstractSerializer<Stage> {

        /** Used for caching the byte => stage mapping */
        private static final StageCache CACHE = new StageCache();


        @Override
        public ByteBuffer toByteBuffer( final Stage obj ) {
            ByteBuffer buff = ByteBuffer.allocate( 1 );
            buff.put( obj.getId() );
            buff.rewind();
            return buff;
        }


        @Override
        public Stage fromByteBuffer( final ByteBuffer byteBuffer ) {
            final byte value  = byteBuffer.get();

            return CACHE.getStage(value);
        }
    }
}
