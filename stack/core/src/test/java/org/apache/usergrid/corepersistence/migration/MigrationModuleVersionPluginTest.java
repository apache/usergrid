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


import org.junit.Test;

import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyV2Impl;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.CollectionMigrationPlugin;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.TestProgressObserver;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationV2Impl;
import org.apache.usergrid.persistence.graph.serialization.impl.migration.GraphMigrationPlugin;

import com.google.inject.Inject;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests the system sets itself up as wxpected
 */
public class MigrationModuleVersionPluginTest {


    @Test
    public void testNewSystem(){

        //mock up an initial system state
        final int systemState = 3;

        final MigrationInfoSerialization migrationInfoSerialization = mock(MigrationInfoSerialization.class);

        when(migrationInfoSerialization.getSystemVersion()).thenReturn( systemState );




        final int mvccVersion = 2;

        final MvccEntitySerializationStrategyV2Impl serializationStrategyV2 = mock(MvccEntitySerializationStrategyV2Impl.class);
        when(serializationStrategyV2.getImplementationVersion()).thenReturn( mvccVersion );


        final int edgeMetaVersion = 1;

        final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2 = mock(EdgeMetadataSerializationV2Impl.class);
        when(edgeMetadataSerializationV2.getImplementationVersion()).thenReturn(edgeMetaVersion);


        final MigrationModuleVersionPlugin plugin = new MigrationModuleVersionPlugin(migrationInfoSerialization, serializationStrategyV2,  edgeMetadataSerializationV2 );

        final TestProgressObserver testProgressObserver = new TestProgressObserver();

        plugin.run( testProgressObserver );



        //first version that should be set
        verify(migrationInfoSerialization).setVersion( CoreMigrationPlugin.PLUGIN_NAME, CoreDataVersions.ID_MAP_FIX.getVersion() );

        //second version that should be set

        verify(migrationInfoSerialization).setVersion( GraphMigrationPlugin.PLUGIN_NAME, edgeMetaVersion );

        //last version that should be set
        verify(migrationInfoSerialization).setVersion( CollectionMigrationPlugin.PLUGIN_NAME, mvccVersion );


        //set this plugin as run
        verify(migrationInfoSerialization).setVersion( MigrationModuleVersionPlugin.NAME, MigrationSystemVersions.LEGACY_ID_MAPPED.getVersion() );



    }



    @Test
    public void testIdMapping(){

           //mock up an initial system state
        final int systemState = 1;

        final MigrationInfoSerialization migrationInfoSerialization = mock(MigrationInfoSerialization.class);

        when(migrationInfoSerialization.getSystemVersion()).thenReturn( systemState );




        final int mvccVersion = 2;

        final MvccEntitySerializationStrategyV2Impl serializationStrategyV2 = mock(MvccEntitySerializationStrategyV2Impl.class);
        when(serializationStrategyV2.getImplementationVersion()).thenReturn( mvccVersion );


        final int edgeMetaVersion = 1;

        final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2 = mock(EdgeMetadataSerializationV2Impl.class);
        when(edgeMetadataSerializationV2.getImplementationVersion()).thenReturn(edgeMetaVersion);


        final MigrationModuleVersionPlugin plugin = new MigrationModuleVersionPlugin(migrationInfoSerialization, serializationStrategyV2,  edgeMetadataSerializationV2 );

        final TestProgressObserver testProgressObserver = new TestProgressObserver();

        plugin.run( testProgressObserver );


        //first version that should be set
        verify(migrationInfoSerialization).setVersion( CoreMigrationPlugin.PLUGIN_NAME, CoreDataVersions.ID_MAP_FIX.getVersion() );

        //second version that should be set

        verify(migrationInfoSerialization, never()).setVersion( GraphMigrationPlugin.PLUGIN_NAME, edgeMetaVersion );

        //last version that should be set
        verify(migrationInfoSerialization, never()).setVersion( CollectionMigrationPlugin.PLUGIN_NAME, mvccVersion );


        //set this plugin as run
        verify(migrationInfoSerialization).setVersion( MigrationModuleVersionPlugin.NAME, MigrationSystemVersions.LEGACY_ID_MAPPED.getVersion() );




    }


    @Test
    public void testEdgeMigration(){

           //mock up an initial system state
        final int systemState = 2;

        final MigrationInfoSerialization migrationInfoSerialization = mock(MigrationInfoSerialization.class);

        when(migrationInfoSerialization.getSystemVersion()).thenReturn( systemState );




        final int mvccVersion = 2;

        final MvccEntitySerializationStrategyV2Impl serializationStrategyV2 = mock(MvccEntitySerializationStrategyV2Impl.class);
        when(serializationStrategyV2.getImplementationVersion()).thenReturn( mvccVersion );


        final int edgeMetaVersion = 1;

        final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2 = mock(EdgeMetadataSerializationV2Impl.class);
        when(edgeMetadataSerializationV2.getImplementationVersion()).thenReturn(edgeMetaVersion);


        final MigrationModuleVersionPlugin plugin = new MigrationModuleVersionPlugin(migrationInfoSerialization, serializationStrategyV2,  edgeMetadataSerializationV2 );

        final TestProgressObserver testProgressObserver = new TestProgressObserver();

        plugin.run( testProgressObserver );


        //first version that should be set
        verify(migrationInfoSerialization).setVersion( CoreMigrationPlugin.PLUGIN_NAME, CoreDataVersions.ID_MAP_FIX.getVersion() );

        //second version that should be set

        verify(migrationInfoSerialization).setVersion( GraphMigrationPlugin.PLUGIN_NAME, edgeMetaVersion );

        //last version that should be set
        verify(migrationInfoSerialization, never()).setVersion( CollectionMigrationPlugin.PLUGIN_NAME, mvccVersion );


        //set this plugin as run
        verify(migrationInfoSerialization).setVersion( MigrationModuleVersionPlugin.NAME, MigrationSystemVersions.LEGACY_ID_MAPPED.getVersion() );




    }




    @Test
    public void testEntityV2Migration(){

           //mock up an initial system state
        final int systemState = 3;

        final MigrationInfoSerialization migrationInfoSerialization = mock(MigrationInfoSerialization.class);

        when(migrationInfoSerialization.getSystemVersion()).thenReturn( systemState );




        final int mvccVersion = 2;

        final MvccEntitySerializationStrategyV2Impl serializationStrategyV2 = mock(MvccEntitySerializationStrategyV2Impl.class);
        when(serializationStrategyV2.getImplementationVersion()).thenReturn( mvccVersion );


        final int edgeMetaVersion = 1;

        final EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2 = mock(EdgeMetadataSerializationV2Impl.class);
        when(edgeMetadataSerializationV2.getImplementationVersion()).thenReturn(edgeMetaVersion);


        final MigrationModuleVersionPlugin plugin = new MigrationModuleVersionPlugin(migrationInfoSerialization, serializationStrategyV2,  edgeMetadataSerializationV2 );

        final TestProgressObserver testProgressObserver = new TestProgressObserver();

        plugin.run( testProgressObserver );


        //first version that should be set
        verify(migrationInfoSerialization).setVersion( CoreMigrationPlugin.PLUGIN_NAME, CoreDataVersions.ID_MAP_FIX.getVersion() );

        //second version that should be set

        verify(migrationInfoSerialization).setVersion( GraphMigrationPlugin.PLUGIN_NAME, edgeMetaVersion );

        //last version that should be set
        verify(migrationInfoSerialization).setVersion( CollectionMigrationPlugin.PLUGIN_NAME, mvccVersion );


        //set this plugin as run
        verify(migrationInfoSerialization).setVersion( MigrationModuleVersionPlugin.NAME, MigrationSystemVersions.LEGACY_ID_MAPPED.getVersion() );




    }

}
