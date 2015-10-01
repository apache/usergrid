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

import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.CollectionMigrationPlugin;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoCache;
import org.apache.usergrid.persistence.core.migration.data.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ConsistencyLevel;


@Singleton
public class UniqueValueSerializationStrategyProxyImpl implements UniqueValueSerializationStrategy {


    protected final Keyspace keyspace;
    private final VersionedMigrationSet<UniqueValueSerializationStrategy> versions;
    private final MigrationInfoCache migrationInfoCache;


    @Inject
    public UniqueValueSerializationStrategyProxyImpl( final Keyspace keyspace,
                                                      final VersionedMigrationSet<UniqueValueSerializationStrategy>
                                                          allVersions,
                                                      final MigrationInfoCache migrationInfoCache ) {

        this.keyspace = keyspace;
        this.migrationInfoCache = migrationInfoCache;
        this.versions = allVersions;
    }


    @Override
    public MutationBatch write( final ApplicationScope applicationScope, final UniqueValue uniqueValue ) {
        final MigrationRelationship<UniqueValueSerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.write( applicationScope, uniqueValue ) );
            aggregateBatch.mergeShallow( migration.to.write( applicationScope, uniqueValue ) );

            return aggregateBatch;
        }

        return migration.to.write( applicationScope, uniqueValue );
    }


    @Override
    public MutationBatch write( final ApplicationScope applicationScope, final UniqueValue uniqueValue,
                                final int timeToLive ) {
        final MigrationRelationship<UniqueValueSerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.write( applicationScope, uniqueValue, timeToLive ) );
            aggregateBatch.mergeShallow( migration.to.write( applicationScope, uniqueValue, timeToLive ) );

            return aggregateBatch;
        }

        return migration.to.write( applicationScope, uniqueValue, timeToLive );
    }


    @Override
    public UniqueValueSet load( final ApplicationScope applicationScope, final String type,
                                final Collection<Field> fields ) throws ConnectionException {

        final MigrationRelationship<UniqueValueSerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.load( applicationScope, type, fields );
        }

        return migration.to.load( applicationScope, type, fields );
    }


    @Override
    public UniqueValueSet load( final ApplicationScope applicationScope, final ConsistencyLevel consistencyLevel,
                                final String type, final Collection<Field> fields ) throws ConnectionException {

        final MigrationRelationship<UniqueValueSerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.load( applicationScope, type, fields );
        }

        return migration.to.load( applicationScope, type, fields );
    }


    @Override
    public Iterator<UniqueValue> getAllUniqueFields( final ApplicationScope applicationScope, final Id entityId ) {
        final MigrationRelationship<UniqueValueSerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.getAllUniqueFields( applicationScope, entityId );
        }

        return migration.to.getAllUniqueFields( applicationScope, entityId );
    }


    @Override
    public MutationBatch delete( final ApplicationScope applicationScope, final UniqueValue uniqueValue ) {
        final MigrationRelationship<UniqueValueSerializationStrategy> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.delete( applicationScope, uniqueValue ) );
            aggregateBatch.mergeShallow( migration.to.delete( applicationScope, uniqueValue ) );

            return aggregateBatch;
        }

        return migration.to.delete( applicationScope, uniqueValue );
    }


    /**
     * Return true if we're on an old version
     */
    private MigrationRelationship<UniqueValueSerializationStrategy> getMigrationRelationShip() {
        return this.versions
            .getMigrationRelationship( migrationInfoCache.getVersion( CollectionMigrationPlugin.PLUGIN_NAME ) );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.emptyList();
    }


    @Override
    public int getImplementationVersion() {
        throw new UnsupportedOperationException( "Not supported in the proxy" );
    }
}
