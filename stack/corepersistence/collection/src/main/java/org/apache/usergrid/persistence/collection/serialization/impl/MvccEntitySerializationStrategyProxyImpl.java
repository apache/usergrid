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
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.CollectionMigrationPlugin;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoCache;
import org.apache.usergrid.persistence.core.migration.data.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;


/**
 * Version 4 implementation of entity serialization. This will proxy writes and reads so that during
 * migration data goes to both sources and is read from the old source. After the upgrade completes,
 * it will be available from the new source
 */
@Singleton
public class MvccEntitySerializationStrategyProxyImpl implements MvccEntitySerializationStrategy {


    protected final Keyspace keyspace;
    private final VersionedMigrationSet<MvccEntitySerializationStrategy> versions;
    private final MigrationInfoCache migrationInfoCache;


    @Inject
    public MvccEntitySerializationStrategyProxyImpl( final Keyspace keyspace,
                                                     final VersionedMigrationSet<MvccEntitySerializationStrategy> allVersions,
                                                     final MigrationInfoCache migrationInfoCache ) {

        this.keyspace = keyspace;
        this.migrationInfoCache = migrationInfoCache;
        this.versions = allVersions;
    }


    @Override
    public MutationBatch write( final ApplicationScope context, final MvccEntity entity ) {

        final MigrationRelationship<MvccEntitySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.write( context, entity ) );
            aggregateBatch.mergeShallow( migration.to.write( context, entity ) );

            return aggregateBatch;
        }

        return migration.to.write( context, entity );
    }


    @Override
    public EntitySet load( final ApplicationScope scope, final Collection<Id> entityIds, final UUID maxVersion ) {

        final MigrationRelationship<MvccEntitySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.load( scope, entityIds, maxVersion );
        }

        return migration.to.load( scope, entityIds, maxVersion );
    }



    @Override
    public Iterator<MvccEntity> loadDescendingHistory( final ApplicationScope context, final Id entityId,
                                                       final UUID version, final int fetchSize ) {

        final MigrationRelationship<MvccEntitySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration()) {
            return migration.from.loadDescendingHistory( context, entityId, version, fetchSize );
        }

        return migration.to.loadDescendingHistory( context, entityId, version, fetchSize );
    }


    @Override
    public Iterator<MvccEntity> loadAscendingHistory( final ApplicationScope context, final Id entityId,
                                                      final UUID version, final int fetchSize ) {

        final MigrationRelationship<MvccEntitySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.loadAscendingHistory( context, entityId, version, fetchSize );
        }

        return migration.to.loadAscendingHistory( context, entityId, version, fetchSize );
    }

    @Override
    public Optional<MvccEntity> load( final ApplicationScope scope, final Id entityId ) {

        final MigrationRelationship<MvccEntitySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.load( scope, entityId );
        }

        return migration.to.load( scope, entityId );
    }


    @Override
    public MutationBatch mark( final ApplicationScope context, final Id entityId, final UUID version ) {

        final MigrationRelationship<MvccEntitySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.mark( context, entityId, version ) );
            aggregateBatch.mergeShallow( migration.to.mark( context, entityId, version ) );

            return aggregateBatch;
        }

        return migration.to.mark( context, entityId, version );
    }


    @Override
    public MutationBatch delete( final ApplicationScope context, final Id entityId, final UUID version ) {

        final MigrationRelationship<MvccEntitySerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.delete( context, entityId, version ) );
            aggregateBatch.mergeShallow( migration.to.delete( context, entityId, version ) );

            return aggregateBatch;
        }

        return migration.to.delete( context, entityId, version );
    }

    /**
     * Return true if we're on an old version
     */
    private MigrationRelationship<MvccEntitySerializationStrategy> getMigrationRelationShip() {
        return this.versions.getMigrationRelationship(
                migrationInfoCache.getVersion( CollectionMigrationPlugin.PLUGIN_NAME ) );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.emptyList();
    }


    @Override
    public int getImplementationVersion() {
        throw new UnsupportedOperationException("Not supported in the proxy");
    }
}

