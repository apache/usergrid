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
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.CollectionMigrationPlugin;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoCache;
import org.apache.usergrid.persistence.core.migration.data.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;


/**
 * The proxy for performing log entry serialization
 */
@Singleton
public class MvccLogEntrySerializationProxyImpl implements MvccLogEntrySerializationStrategy {

    protected final Keyspace keyspace;
    private final VersionedMigrationSet<MvccLogEntrySerializationStrategy> versions;
    private final MigrationInfoCache migrationInfoCache;


    @Inject
    public MvccLogEntrySerializationProxyImpl( final Keyspace keyspace,
                                               final VersionedMigrationSet<MvccLogEntrySerializationStrategy>
                                                   allVersions,
                                               final MigrationInfoCache migrationInfoCache ) {

        this.keyspace = keyspace;
        this.migrationInfoCache = migrationInfoCache;
        this.versions = allVersions;
    }


    @Override
    public MutationBatch write( final ApplicationScope applicationScope, final MvccLogEntry entry ) {
        final MigrationRelationship<MvccLogEntrySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.write( applicationScope, entry ) );
            aggregateBatch.mergeShallow( migration.to.write( applicationScope, entry ) );

            return aggregateBatch;
        }

        return migration.to.write( applicationScope, entry );
    }


    @Override
    public VersionSet load( final ApplicationScope applicationScope, final Collection<Id> entityIds,
                            final UUID version ) {


        final MigrationRelationship<MvccLogEntrySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.load( applicationScope, entityIds, version );
        }

        return migration.to.load( applicationScope, entityIds, version );
    }


    @Override
    public List<MvccLogEntry> load( final ApplicationScope applicationScope, final Id entityId, final UUID version,
                                    final int maxSize ) {
        final MigrationRelationship<MvccLogEntrySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.load( applicationScope, entityId, version, maxSize );
        }

        return migration.to.load( applicationScope, entityId, version, maxSize );
    }


    @Override
    public List<MvccLogEntry> loadReversed( final ApplicationScope applicationScope, final Id entityId,
                                            final UUID minVersion, final int maxSize ) {

        final MigrationRelationship<MvccLogEntrySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.loadReversed( applicationScope, entityId, minVersion, maxSize );
        }

        return migration.to.loadReversed( applicationScope, entityId, minVersion, maxSize );
    }


    @Override
    public MutationBatch delete( final ApplicationScope applicationScope, final Id entityId, final UUID version ) {
        final MigrationRelationship<MvccLogEntrySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.delete( applicationScope, entityId, version ) );
            aggregateBatch.mergeShallow( migration.to.delete( applicationScope, entityId, version ) );

            return aggregateBatch;
        }

        return migration.to.delete( applicationScope, entityId, version );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.emptyList();
    }


    @Override
    public int getImplementationVersion() {
        throw new UnsupportedOperationException( "Not supported in the proxy" );
    }


    /**
     * Return true if we're on an old version
     */
    private MigrationRelationship<MvccLogEntrySerializationStrategy> getMigrationRelationShip() {
        return this.versions
            .getMigrationRelationship( migrationInfoCache.getVersion( CollectionMigrationPlugin.PLUGIN_NAME ) );
    }
}
