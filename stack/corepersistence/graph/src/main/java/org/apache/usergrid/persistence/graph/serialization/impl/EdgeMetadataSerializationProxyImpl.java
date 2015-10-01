/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.graph.serialization.impl;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoCache;
import org.apache.usergrid.persistence.core.migration.data.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.migration.GraphMigrationPlugin;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;


@Singleton
public class EdgeMetadataSerializationProxyImpl implements EdgeMetadataSerialization {

    private static final Logger logger = LoggerFactory.getLogger(EdgeMetadataSerializationProxyImpl.class);

    private final Keyspace keyspace;
    private final VersionedMigrationSet<EdgeMetadataSerialization> versions;
    private final MigrationInfoCache migrationInfoCache;


    /**
     * Handles routing data to the right implementation, based on the current system migration version
     */
    @Inject
    public EdgeMetadataSerializationProxyImpl( final Keyspace keyspace,
                                               final VersionedMigrationSet<EdgeMetadataSerialization> versions,
                                               final MigrationInfoCache migrationInfoCache ) {
        this.keyspace = keyspace;
        this.versions = versions;
        this.migrationInfoCache = migrationInfoCache;
    }


    @Override
    public MutationBatch writeEdge( final ApplicationScope scope, final Edge edge ) {

        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();


        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.writeEdge( scope, edge ) );
            aggregateBatch.mergeShallow( migration.to.writeEdge( scope, edge ) );

            return aggregateBatch;
        }

        return migration.to.writeEdge( scope, edge );
    }


    @Override
    public MutationBatch removeEdgeTypeFromSource( final ApplicationScope scope, final Edge edge ) {
        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.removeEdgeTypeFromSource( scope, edge ) );
            aggregateBatch.mergeShallow( migration.to.removeEdgeTypeFromSource( scope, edge ) );

            return aggregateBatch;
        }

        return migration.to.removeEdgeTypeFromSource( scope, edge );
    }


    @Override
    public MutationBatch removeEdgeTypeFromSource( final ApplicationScope scope, final Id sourceNode, final String type,
                                                   final long timestamp ) {
        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.removeEdgeTypeFromSource( scope, sourceNode, type, timestamp ) );
            aggregateBatch.mergeShallow( migration.to.removeEdgeTypeFromSource( scope, sourceNode, type, timestamp ) );

            return aggregateBatch;
        }

        return migration.to.removeEdgeTypeFromSource( scope, sourceNode, type, timestamp );
    }


    @Override
    public MutationBatch removeIdTypeFromSource( final ApplicationScope scope, final Edge edge ) {

        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.removeIdTypeFromSource( scope, edge ) );
            aggregateBatch.mergeShallow( migration.to.removeIdTypeFromSource( scope, edge ) );

            return aggregateBatch;
        }

        return migration.to.removeIdTypeFromSource( scope, edge );
    }


    @Override
    public MutationBatch removeIdTypeFromSource( final ApplicationScope scope, final Id sourceNode, final String type,
                                                 final String idType, final long timestamp ) {

        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch
                    .mergeShallow( migration.from.removeIdTypeFromSource( scope, sourceNode, type, idType, timestamp ) );
            aggregateBatch.mergeShallow( migration.to.removeIdTypeFromSource( scope, sourceNode, type, idType, timestamp ) );

            return aggregateBatch;
        }

        return migration.to.removeIdTypeFromSource( scope, sourceNode, type, idType, timestamp );
    }


    @Override
    public MutationBatch removeEdgeTypeToTarget( final ApplicationScope scope, final Edge edge ) {

        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.removeEdgeTypeToTarget( scope, edge ) );
            aggregateBatch.mergeShallow( migration.to.removeEdgeTypeToTarget( scope, edge ) );

            return aggregateBatch;
        }

        return migration.to.removeEdgeTypeToTarget( scope, edge );
    }


    @Override
    public MutationBatch removeEdgeTypeToTarget( final ApplicationScope scope, final Id targetNode, final String type,
                                                 final long timestamp ) {

        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.removeEdgeTypeToTarget( scope, targetNode, type, timestamp ) );
            aggregateBatch.mergeShallow( migration.to.removeEdgeTypeToTarget( scope, targetNode, type, timestamp ) );

            return aggregateBatch;
        }

        return migration.to.removeEdgeTypeToTarget( scope, targetNode, type, timestamp );
    }


    @Override
    public MutationBatch removeIdTypeToTarget( final ApplicationScope scope, final Edge edge ) {

        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.removeIdTypeToTarget( scope, edge ) );
            aggregateBatch.mergeShallow( migration.to.removeIdTypeToTarget( scope, edge ) );

            return aggregateBatch;
        }

        return migration.to.removeIdTypeToTarget( scope, edge );
    }


    @Override
    public MutationBatch removeIdTypeToTarget( final ApplicationScope scope, final Id targetNode, final String type,
                                               final String idType, final long timestamp ) {
        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            final MutationBatch aggregateBatch = keyspace.prepareMutationBatch();

            aggregateBatch.mergeShallow( migration.from.removeIdTypeToTarget( scope, targetNode, type, idType, timestamp ) );
            aggregateBatch.mergeShallow( migration.to.removeIdTypeToTarget( scope, targetNode, type, idType, timestamp ) );

            return aggregateBatch;
        }

        return migration.to.removeIdTypeToTarget( scope, targetNode, type, idType, timestamp );
    }


    @Override
    public Iterator<String> getEdgeTypesFromSource( final ApplicationScope scope, final SearchEdgeType search ) {
        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.getEdgeTypesFromSource( scope, search );
        }

        return migration.to.getEdgeTypesFromSource( scope, search );
    }


    @Override
    public Iterator<String> getIdTypesFromSource( final ApplicationScope scope, final SearchIdType search ) {
        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.getIdTypesFromSource( scope, search );
        }

        return migration.to.getIdTypesFromSource( scope, search );
    }


    @Override
    public Iterator<String> getEdgeTypesToTarget( final ApplicationScope scope, final SearchEdgeType search ) {
        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.getEdgeTypesToTarget( scope, search );
        }

        return migration.to.getEdgeTypesToTarget( scope, search );
    }


    @Override
    public Iterator<String> getIdTypesToTarget( final ApplicationScope scope, final SearchIdType search ) {
        final MigrationRelationship<EdgeMetadataSerialization> migration = getMigrationRelationShip();

        if ( migration.needsMigration() ) {
            return migration.from.getIdTypesToTarget( scope, search );
        }

        return migration.to.getIdTypesToTarget( scope, search );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.EMPTY_LIST;
    }




    /**
     * Return true if we're on an old version
     */
    private MigrationRelationship<EdgeMetadataSerialization> getMigrationRelationShip() {
        return this.versions.getMigrationRelationship(
                migrationInfoCache.getVersion( GraphMigrationPlugin.PLUGIN_NAME ) );
    }


    @Override
    public int getImplementationVersion() {
        throw new UnsupportedOperationException( "Proxies do not have an implementation version" );
    }
}
