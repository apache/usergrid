/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.locks.impl;


import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UUIDType;

import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.StringRowCompositeSerializer;
import org.apache.usergrid.persistence.locks.LockId;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * Serialization of locks
 */
@Singleton
public class LockProposalSerializationImpl implements LockProposalSerialization {

    private static final StringRowCompositeSerializer STRING_SER = StringRowCompositeSerializer.get();

    private static final ScopedRowKeySerializer<String> ROW_KEY_SER = new ScopedRowKeySerializer<>( STRING_SER );


    private static final MultiTennantColumnFamily<ScopedRowKey<String>, UUID> CF_MULTI_REGION_LOCKS =
        new MultiTennantColumnFamily<>( "Multi_Region_Locks", ROW_KEY_SER, UUIDSerializer.get() );


    private static final byte[] EMPTY = new byte[0];


    protected final Keyspace keyspace;
    protected final LockConsistency lockConsistency;


    @Inject
    public LockProposalSerializationImpl( final Keyspace keyspace, final LockConsistency lockConsistency ) {
        this.keyspace = keyspace;
        this.lockConsistency = lockConsistency;
    }


    @Override
    public LockCandidate writeNewValue( final LockId lockId, final UUID proposed, final int expirationInSeconds ) {

        final MutationBatch batch =
            keyspace.prepareMutationBatch().withConsistencyLevel( lockConsistency.getShardWriteConsistency() );

        final Id applicationId = lockId.getApplicationScope().getApplication();

        final ScopedRowKey<String> rowKey = ScopedRowKey.fromKey( applicationId, lockId.generateKey() );

        //put the column with expiration
        batch.withRow( CF_MULTI_REGION_LOCKS, rowKey ).putColumn( proposed, EMPTY, expirationInSeconds );


        try {
            batch.execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }


        return readState( rowKey );
    }


    @Override
    public LockCandidate ackProposed( final LockId lockId, final UUID proposed, final UUID seen,
                                      final int expirationInSeconds ) {

        final MutationBatch batch =
            keyspace.prepareMutationBatch().withConsistencyLevel( lockConsistency.getShardWriteConsistency() );

        final Id applicationId = lockId.getApplicationScope().getApplication();

        final ScopedRowKey<String> rowKey = ScopedRowKey.fromKey( applicationId, lockId.generateKey() );


        //put the column with expiration
        batch.withRow( CF_MULTI_REGION_LOCKS, rowKey ).putColumn( proposed, seen, expirationInSeconds );


        try {
            batch.execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }


        return readState( rowKey );
    }


    @Override
    public LockCandidate pollState( final LockId lockId ) {

        final Id applicationId = lockId.getApplicationScope().getApplication();

        final ScopedRowKey<String> rowKey = ScopedRowKey.fromKey( applicationId, lockId.generateKey() );

        return readState( rowKey );
    }


    /**
     * Read the lock state from the column family
     */
    private LockCandidate readState( final ScopedRowKey<String> rowKey ) {
        //read the first 2 records


        final ColumnList<UUID> results;

        try {
            results = keyspace.prepareQuery( CF_MULTI_REGION_LOCKS )
                              .setConsistencyLevel( lockConsistency.getShardReadConsistency() ).getKey( rowKey )
                              .withColumnRange( ( UUID ) null, null, false, 2 ).execute().getResult();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }


        //should never happen, sanity check.
        if ( results.isEmpty() ) {
            throw new RuntimeException(
                "Unable to read results from cassandra.  There should be at least 1 result left" );
        }


        final UUID proposedLock = results.getColumnByIndex( 0 ).getName();

        //we have 2 columns, populate the proposal
        if ( results.size() == 2 ) {
            final Column<UUID> column = results.getColumnByIndex( 1 );

            final Optional<UUID> secondProposedLock = Optional.of( column.getName() );

            final Optional<UUID> valueSeenBySecond;

            if ( column.hasValue() ) {
                valueSeenBySecond = Optional.of( column.getUUIDValue() );
            }
            else {
                valueSeenBySecond = Optional.absent();
            }

            return new LockCandidate( proposedLock, secondProposedLock, valueSeenBySecond );
        }

        return new LockCandidate( proposedLock, Optional.absent(), Optional.absent() );
    }


    @Override
    public void delete( final LockId lockId, final UUID proposed ) {
        final MutationBatch batch =
            keyspace.prepareMutationBatch().withConsistencyLevel( lockConsistency.getShardWriteConsistency() );

        final Id applicationId = lockId.getApplicationScope().getApplication();

        final ScopedRowKey<String> rowKey = ScopedRowKey.fromKey( applicationId, lockId.generateKey() );


        //put the column with expiration
        batch.withRow( CF_MULTI_REGION_LOCKS, rowKey ).deleteColumn( proposed );


        try {
            batch.execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        //create the CF and sort them by uuid type so time uuid with lowest will be first
        MultiTennantColumnFamilyDefinition cf =
            new MultiTennantColumnFamilyDefinition( CF_MULTI_REGION_LOCKS, BytesType.class.getSimpleName(),
                UUIDType.class.getSimpleName(), UUIDType.class.getSimpleName(),
                MultiTennantColumnFamilyDefinition.CacheOption.ALL, Optional.of( 1 ) );


        return Collections.singleton( cf );
    }
}
