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

package org.apache.usergrid.corepersistence.migration;


import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyV2Impl;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.CollectionMigrationPlugin;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.newimpls.DataMigration2;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.newimpls.ProgressObserver;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationV2Impl;
import org.apache.usergrid.persistence.graph.serialization.impl.migration.GraphMigrationPlugin;

import com.google.inject.Inject;


/**
 * Migration to set our module versions now that we've refactor for sub modules Keeps the EntityIdScope because it won't
 * subscribe to the data provider.
 */
public class MigrationModuleVersion implements DataMigration2<EntityIdScope> {


    /**
     * The migration from 0 -> 1 that re-writes all the entity id's into the map module
     */
    private static final int ID_MIGRATION = 1;

    /**
     * The migration from 1-> 2 that shards our edge meta data
     */
    private static final int EDGE_SHARD_MIGRATION = 2;

    /**
     * The migration from 2-> 3 that fixed the short truncation bug
     */
    private static final int ENTITY_V2_MIGRATION = 3;

    private final MigrationInfoSerialization migrationInfoSerialization;

    private final MvccEntitySerializationStrategyV2Impl serializationStrategyV2;

    private final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2;


    @Inject
    public MigrationModuleVersion( final MigrationInfoSerialization migrationInfoSerialization,
                                   final MvccEntitySerializationStrategyV2Impl serializationStrategyV2,
                                   final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2 ) {
        this.migrationInfoSerialization = migrationInfoSerialization;
        this.serializationStrategyV2 = serializationStrategyV2;
        this.edgeMetadataSerializationV2 = edgeMetadataSerializationV2;
    }


    @Override
    public int migrate( final int currentVersion, final MigrationDataProvider<EntityIdScope> migrationDataProvider,
                        final ProgressObserver observer ) {

        //we ignore our current version, since it will always be 0
        final int legacyVersion = migrationInfoSerialization.getSystemVersion();

        //now we store versions for each of our modules

        switch ( legacyVersion ) {
            case ID_MIGRATION:
                //no op, we need to migration everything in all modules
                break;
            //we need to set the version of the entity data, and our edge shard migration.  The fall through (no break) is deliberate
            case ENTITY_V2_MIGRATION:
               migrationInfoSerialization.setVersion( CollectionMigrationPlugin.PLUGIN_NAME, serializationStrategyV2.getImplementationVersion() );
            case EDGE_SHARD_MIGRATION:
                //set our shard migration to the migrated version
                migrationInfoSerialization.setVersion( GraphMigrationPlugin.PLUGIN_NAME, edgeMetadataSerializationV2.getImplementationVersion() );
                break;
        }

        return CoreDataVersions.MIGRATION_VERSION_FIX.getVersion();
    }


    @Override
    public boolean supports( final int currentVersion ) {
        //we move from the migration version fix to the current version
        return CoreDataVersions.INITIAL.getVersion() == currentVersion;
    }


    @Override
    public int getMaxVersion() {
        return CoreDataVersions.MIGRATION_VERSION_FIX.getVersion();
    }
}
