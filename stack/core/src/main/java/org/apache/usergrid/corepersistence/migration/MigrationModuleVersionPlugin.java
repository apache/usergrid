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
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.MigrationPlugin;
import org.apache.usergrid.persistence.core.migration.data.PluginPhase;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationV2Impl;
import org.apache.usergrid.persistence.graph.serialization.impl.migration.GraphMigrationPlugin;

import com.google.inject.Inject;


/**
 * Migration to set our module versions now that we've refactor for sub modules Keeps the EntityIdScope because it won't
 * subscribe to the data provider.
 */
public class MigrationModuleVersionPlugin implements MigrationPlugin{

    public static final String NAME = "migration-system";

    private static final int INITIAL = 0;
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

    /**
     * Appinfo to application_info migration.
     */
    private static final int APPINFO_MIGRATION = 4;


    /**
     * Get versions directly from impls so we know they're accurate
     */
    private final MigrationInfoSerialization migrationInfoSerialization;

    private final MvccEntitySerializationStrategyV2Impl serializationStrategyV2;

    private final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2;


    @Inject
    public MigrationModuleVersionPlugin( final MigrationInfoSerialization migrationInfoSerialization,
                                         final MvccEntitySerializationStrategyV2Impl serializationStrategyV2,
                                         final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2 ) {
        this.migrationInfoSerialization = migrationInfoSerialization;
        this.serializationStrategyV2 = serializationStrategyV2;
        this.edgeMetadataSerializationV2 = edgeMetadataSerializationV2;
    }

    @Override
      public void run( final ProgressObserver observer ) {


        observer.start();

        //we ignore our current version, since it will always be 0
        final int legacyVersion = migrationInfoSerialization.getSystemVersion();



        //now we store versions for each of our modules

        switch ( legacyVersion ) {

            //we need to set the version of the entity data, and our edge shard migration.  The fall through (no break) is deliberate
            //if it's initial, set both
            case INITIAL:

            //if it's entity v2, set all, it's current
            case ENTITY_V2_MIGRATION:
               migrationInfoSerialization.setVersion(
                   CollectionMigrationPlugin.PLUGIN_NAME, serializationStrategyV2.getImplementationVersion() );

            //if it's edge shard, we need to run the v2 migration
            case EDGE_SHARD_MIGRATION:
                //set our shard migration to the migrated version
                migrationInfoSerialization.setVersion(
                    GraphMigrationPlugin.PLUGIN_NAME, edgeMetadataSerializationV2.getImplementationVersion() );

            case ID_MIGRATION:
                migrationInfoSerialization.setVersion(
                    CoreMigrationPlugin.PLUGIN_NAME, CoreDataVersions.ID_MAP_FIX.getVersion() );
        }

        //save the version
        migrationInfoSerialization.setVersion( NAME, getMaxVersion() );

        observer.complete();
      }



    @Override
    public String getName() {
        return NAME;
    }




    @Override
    public int getMaxVersion() {
        return MigrationSystemVersions.LEGACY_ID_MAPPED.getVersion();
    }


    @Override
    public PluginPhase getPhase() {
        return PluginPhase.BOOTSTRAP;
    }
}
