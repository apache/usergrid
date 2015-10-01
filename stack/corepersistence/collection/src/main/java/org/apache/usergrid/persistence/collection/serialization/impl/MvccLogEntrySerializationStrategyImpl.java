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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.AbstractSerializer;


/**
 * Simple implementation for reading and writing log entries
 *
 * @author tnine
 */
public abstract class MvccLogEntrySerializationStrategyImpl<K> implements MvccLogEntrySerializationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger( MvccLogEntrySerializationStrategyImpl.class );

    private static final StageSerializer SER = new StageSerializer();

    private final MultiTennantColumnFamily<ScopedRowKey<K>, UUID> CF_ENTITY_LOG;


    protected final Keyspace keyspace;
    protected final SerializationFig fig;


    public MvccLogEntrySerializationStrategyImpl( final Keyspace keyspace, final SerializationFig fig ) {
        this.keyspace = keyspace;
        this.fig = fig;
        CF_ENTITY_LOG = getColumnFamily();
    }


    @Override
    public MutationBatch write( final ApplicationScope collectionScope, final MvccLogEntry entry ) {

        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entry, "entry is required" );


        final Stage stage = entry.getStage();
        final UUID colName = entry.getVersion();
        final StageStatus stageStatus = new StageStatus( stage, entry.getState() );

        return doWrite( collectionScope, entry.getEntityId(), entry.getVersion(), colMutation -> {

            //Write the stage with a timeout, it's set as transient
            if ( stage.isTransient() ) {
                colMutation.putColumn( colName, stageStatus, SER, fig.getTimeout() );
                return;
            }

            //otherwise it's persistent, write it with no expiration
            colMutation.putColumn( colName, stageStatus, SER, null );
        } );
    }


    @Override
    public VersionSet load( final ApplicationScope collectionScope, final Collection<Id> entityIds,
                            final UUID maxVersion ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityIds, "entityIds is required" );
        Preconditions.checkArgument( entityIds.size() > 0, "You must specify an Id" );
        Preconditions.checkNotNull( maxVersion, "maxVersion is required" );


        //didnt put the max in the error message, I don't want to take the string construction hit every time
        Preconditions.checkArgument( entityIds.size() <= fig.getMaxLoadSize(),
            "requested size cannot be over configured maximum" );


        final Id applicationId = collectionScope.getApplication();


        final List<ScopedRowKey<K>> rowKeys = new ArrayList<>( entityIds.size() );


        for ( final Id entityId : entityIds ) {
            final ScopedRowKey<K> rowKey = createKey( applicationId, entityId );


            rowKeys.add( rowKey );
        }


        final Iterator<Row<ScopedRowKey<K>, UUID>> latestEntityColumns;


        try {
            latestEntityColumns = keyspace.prepareQuery( CF_ENTITY_LOG ).getKeySlice( rowKeys )
                                          .withColumnRange( maxVersion, null, false, 1 ).execute().getResult()
                                          .iterator();
        }
        catch ( ConnectionException e ) {
            throw new CollectionRuntimeException( null, collectionScope, "An error occurred connecting to cassandra",
                e );
        }


        final VersionSetImpl versionResults = new VersionSetImpl( entityIds.size() );

        while ( latestEntityColumns.hasNext() ) {
            final Row<ScopedRowKey<K>, UUID> row = latestEntityColumns.next();

            final ColumnList<UUID> columns = row.getColumns();

            if ( columns.size() == 0 ) {
                continue;
            }


            final Id entityId = getEntityIdFromKey( row.getKey() );

            final Column<UUID> column = columns.getColumnByIndex( 0 );


            final UUID version = column.getName();

            final StageStatus stageStatus = column.getValue( SER );

            final MvccLogEntry logEntry =
                new MvccLogEntryImpl( entityId, version, stageStatus.stage, stageStatus.state );


            versionResults.addEntry( logEntry );
        }

        return versionResults;
    }


    @Override
    public List<MvccLogEntry> load( final ApplicationScope collectionScope, final Id entityId, final UUID version,
                                    final int maxSize ) {
        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version is required" );
        Preconditions.checkArgument( maxSize > 0, "max Size must be greater than 0" );


        ColumnList<UUID> columns;
        try {

            final Id applicationId = collectionScope.getApplication();

            final ScopedRowKey<K> rowKey = createKey( applicationId, entityId );


            columns =
                keyspace.prepareQuery( CF_ENTITY_LOG ).getKey( rowKey ).withColumnRange( version, null, false, maxSize )
                        .execute().getResult();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to load log entries", e );
        }

        return parseResults( columns, entityId );
    }


    @Override
    public List<MvccLogEntry> loadReversed( final ApplicationScope applicationScope, final Id entityId,
                                            final UUID minVersion, final int maxSize ) {
        ColumnList<UUID> columns;
        try {

            final Id applicationId = applicationScope.getApplication();

            final ScopedRowKey<K> rowKey = createKey( applicationId, entityId );


            columns = keyspace.prepareQuery( CF_ENTITY_LOG ).getKey( rowKey )
                              .withColumnRange( minVersion, null, true, maxSize ).execute().getResult();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to load log entries", e );
        }

        return parseResults( columns, entityId );
    }


    private List<MvccLogEntry> parseResults( final ColumnList<UUID> columns, final Id entityId ) {

        List<MvccLogEntry> results = new ArrayList<MvccLogEntry>( columns.size() );

        for ( Column<UUID> col : columns ) {
            final UUID storedVersion = col.getName();
            final StageStatus stage = col.getValue( SER );

            results.add( new MvccLogEntryImpl( entityId, storedVersion, stage.stage, stage.state ) );
        }

        return results;
    }


    @Override
    public MutationBatch delete( final ApplicationScope context, final Id entityId, final UUID version ) {

        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entityId is required" );
        Preconditions.checkNotNull( version, "version context is required" );

        return doWrite( context, entityId, version, colMutation -> colMutation.deleteColumn( version ) );
    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private interface RowOp {

        /**
         * The operation to perform on the row
         */
        void doOp( ColumnListMutation<UUID> colMutation );
    }


    /**
     * Do the column update or delete for the given column and row key
     *
     * @param collectionScope We need to use this when getting the keyspace
     */
    private MutationBatch doWrite( ApplicationScope collectionScope, Id entityId, UUID version, RowOp op ) {

        final MutationBatch batch = keyspace.prepareMutationBatch();

        final long timestamp = version.timestamp();

        LOG.debug( "Writing version with timestamp '{}'", timestamp );

        final Id applicationId = collectionScope.getApplication();

        final ScopedRowKey<K> key = createKey( applicationId, entityId );

        op.doOp( batch.withRow( CF_ENTITY_LOG, key ) );

        return batch;
    }


    protected abstract MultiTennantColumnFamily<ScopedRowKey<K>, UUID> getColumnFamily();


    protected abstract ScopedRowKey<K> createKey( final Id applicationId, final Id entityId );

    protected abstract Id getEntityIdFromKey( final ScopedRowKey<K> key );


    /**
     * Internal stage shard
     */
    private static class StageCache {
        private Map<Integer, Stage> values = new HashMap<Integer, Stage>( Stage.values().length );


        private StageCache() {
            for ( Stage stage : Stage.values() ) {

                final int stageValue = stage.getId();

                values.put( stageValue, stage );
            }
        }


        /**
         * Get the stage with the byte value
         */
        private Stage getStage( final int value ) {
            return values.get( value );
        }
    }


    /**
     * Internal stage shard
     */
    private static class StatusCache {
        private Map<Integer, MvccLogEntry.State> values =
            new HashMap<Integer, MvccLogEntry.State>( MvccLogEntry.State.values().length );


        private StatusCache() {
            for ( MvccLogEntry.State state : MvccLogEntry.State.values() ) {

                final int statusValue = state.getId();

                values.put( statusValue, state );
            }
        }


        /**
         * Get the stage with the byte value
         */
        private MvccLogEntry.State getStatus( final int value ) {
            return values.get( value );
        }
    }


    public static class StageSerializer extends AbstractSerializer<StageStatus> {

        /**
         * Used for caching the byte => stage mapping
         */
        private static final StageCache CACHE = new StageCache();
        private static final StatusCache STATUS_CACHE = new StatusCache();


        @Override
        public ByteBuffer toByteBuffer( final StageStatus obj ) {

            ByteBuffer byteBuffer = ByteBuffer.allocate( 8 );
            byteBuffer.putInt( obj.stage.getId() );
            byteBuffer.putInt( obj.state.getId() );
            byteBuffer.rewind();
            return byteBuffer;
        }


        @Override
        public StageStatus fromByteBuffer( final ByteBuffer byteBuffer ) {
            int value = byteBuffer.getInt();
            Stage stage = CACHE.getStage( value );
            value = byteBuffer.getInt();
            MvccLogEntry.State state = STATUS_CACHE.getStatus( value );
            return new StageStatus( stage, state );
        }
    }


    public static class StageStatus {
        final Stage stage;
        final MvccLogEntry.State state;


        public StageStatus( Stage stage, MvccLogEntry.State state ) {
            this.stage = stage;
            this.state = state;
        }
    }
}
