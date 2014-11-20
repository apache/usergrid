/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.guice.CurrentImpl;
import org.apache.usergrid.persistence.core.guice.PreviousImpl;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;


/**
 * Version 3 implementation of entity serialization. This will proxy writes and reads so that during
 * migration data goes to both sources and is read from the old source. After the ugprade completes,
 * it will be available from the new source
 */
@Singleton
public class MvccEntitySerializationStrategyProxyImpl implements MvccEntitySerializationStrategy {


    public static final int MIGRATION_VERSION = 3;

    private final DataMigrationManager dataMigrationManager;
    private final Keyspace keyspace;
    private final MvccEntitySerializationStrategy previous;
    private final MvccEntitySerializationStrategy current;


    @Inject
    public MvccEntitySerializationStrategyProxyImpl( final DataMigrationManager dataMigrationManager,
                                                     final Keyspace keyspace,
                                                     @PreviousImpl final MvccEntitySerializationStrategy previous,
                                                     @CurrentImpl final MvccEntitySerializationStrategy current ) {
        this.dataMigrationManager = dataMigrationManager;
        this.keyspace = keyspace;
        this.previous = previous;
        this.current = current;
    }


    @Override
    public MutationBatch write( final CollectionScope context, final MvccEntity entity ) {
        if ( isOldVersion() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( previous.write( context, entity ) );
            aggregateBatch.mergeShallow( current.write( context, entity ) );

            return aggregateBatch;
        }

        return current.write( context, entity );
    }


    @Override
    public EntitySet load( final CollectionScope scope, final Collection<Id> entityIds, final UUID maxVersion ) {
        if ( isOldVersion() ) {
            return previous.load( scope, entityIds, maxVersion );
        }

        return current.load( scope, entityIds, maxVersion );
    }


    @Override
    public Iterator<MvccEntity> loadDescendingHistory( final CollectionScope context, final Id entityId,
                                                       final UUID version, final int fetchSize ) {
        if ( isOldVersion() ) {
            return previous.loadDescendingHistory( context, entityId, version, fetchSize );
        }

        return current.loadDescendingHistory( context, entityId, version, fetchSize );
    }


    @Override
    public Iterator<MvccEntity> loadAscendingHistory( final CollectionScope context, final Id entityId,
                                                      final UUID version, final int fetchSize ) {
        if ( isOldVersion() ) {
            return previous.loadAscendingHistory( context, entityId, version, fetchSize );
        }

        return current.loadAscendingHistory( context, entityId, version, fetchSize );
    }


    @Override
    public MutationBatch mark( final CollectionScope context, final Id entityId, final UUID version ) {
        if ( isOldVersion() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( previous.mark( context, entityId, version ) );
            aggregateBatch.mergeShallow( current.mark( context, entityId, version ) );

            return aggregateBatch;
        }

        return current.mark( context, entityId, version );
    }


    @Override
    public MutationBatch delete( final CollectionScope context, final Id entityId, final UUID version ) {
        if ( isOldVersion() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( previous.delete( context, entityId, version ) );
            aggregateBatch.mergeShallow( current.delete( context, entityId, version ) );

            return aggregateBatch;
        }

        return current.delete( context, entityId, version );
    }


    /**
     * Return true if we're on an old version
     */
    private boolean isOldVersion() {
        return dataMigrationManager.getCurrentVersion() < MIGRATION_VERSION;
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.emptyList();
    }
}
