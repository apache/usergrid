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

package org.apache.usergrid.persistence.core.migration.data;


import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.InOrder;

import org.apache.usergrid.persistence.core.migration.schema.MigrationException;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class DataMigrationManagerImplTest {


    @Test
    public void testNoPlugins() {

        final Set<MigrationPlugin> plugins = new HashSet<>();

        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );

        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);
        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );


        Set<String> pluginNames = migrationManager.getPluginNames();

        assertEquals( 0, pluginNames.size() );
    }


    @Test
    public void test2Plugins() throws MigrationException {

        final Set<MigrationPlugin> plugins = new HashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin1.getName() ).thenReturn( "plugin1" );

        MigrationPlugin plugin2 = mock( MigrationPlugin.class );
        when( plugin2.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin2.getName() ).thenReturn( "plugin2" );

        plugins.add( plugin1 );
        plugins.add( plugin2 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );
        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization,migrationInfoCache );


        Set<String> pluginNames = migrationManager.getPluginNames();

        assertEquals(2, pluginNames.size());

        assertTrue( pluginNames.contains( "plugin1" ) );

        assertTrue(pluginNames.contains("plugin2"));

        //now run them

        migrationManager.migrate();

        verify( plugin1 ).run(any(ProgressObserver.class));

        verify( plugin2 ).run( any( ProgressObserver.class ) );
        verify(migrationInfoCache,Mockito.times(2)).invalidateAll();
    }

    @Test
    public void testPluginByName() throws MigrationException {

        final Set<MigrationPlugin> plugins = new HashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin1.getName() ).thenReturn( "plugin1" );

        MigrationPlugin plugin2 = mock( MigrationPlugin.class );
        when( plugin2.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin2.getName() ).thenReturn( "plugin2" );

        plugins.add( plugin1 );
        plugins.add( plugin2 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );
        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization,migrationInfoCache );


        Set<String> pluginNames = migrationManager.getPluginNames();

        assertEquals(2, pluginNames.size());

        assertTrue(pluginNames.contains("plugin1"));

        assertTrue(pluginNames.contains("plugin2"));

        //now run them

        migrationManager.migrate("plugin1");

        verify( plugin1 ).run(any(ProgressObserver.class));

    }


    @Test
    public void testPluginExists() throws MigrationException {

        final Set<MigrationPlugin> plugins = new HashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin1.getName() ).thenReturn( "plugin1" );

        MigrationPlugin plugin2 = mock( MigrationPlugin.class );
        when( plugin2.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin2.getName() ).thenReturn( "plugin2" );

        plugins.add( plugin1 );
        plugins.add( plugin2 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );
        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization,migrationInfoCache );


        Set<String> pluginNames = migrationManager.getPluginNames();

        assertEquals(2, pluginNames.size());

        assertTrue(pluginNames.contains("plugin1"));

        assertTrue(pluginNames.contains("plugin2"));

        //now run them

        assertTrue( migrationManager.pluginExists("plugin1") );
        assertTrue( migrationManager.pluginExists("plugin2") );
        assertFalse( migrationManager.pluginExists("plugin3") );


    }

    @Test
      public void test2PluginsPhaseOrder() throws MigrationException {

        final Set<MigrationPlugin> plugins = new HashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.BOOTSTRAP );
        when( plugin1.getName() ).thenReturn( "plugin2a" );

        MigrationPlugin plugin1a = mock( MigrationPlugin.class );
        when( plugin1a.getPhase() ).thenReturn( PluginPhase.BOOTSTRAP );
        when( plugin1a.getName() ).thenReturn( "plugin2" );

        MigrationPlugin plugin2 = mock( MigrationPlugin.class );
        when( plugin2.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin2.getName() ).thenReturn( "plugin1" );

        plugins.add( plugin1 );
        plugins.add( plugin2 );
        plugins.add( plugin1a);


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );
        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );


        assertTrue(migrationManager.getExecutionOrder().get(0).getName() == "plugin2");
        assertTrue(migrationManager.getExecutionOrder().get(1).getName() == "plugin2a");
        assertTrue(migrationManager.getExecutionOrder().get(2).getName() == "plugin1");

    }

    @Test
    public void test2PluginsNameOrder() throws MigrationException {

        final Set<MigrationPlugin> plugins = new HashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin1.getName() ).thenReturn( "plugin2" );

        MigrationPlugin plugin2 = mock( MigrationPlugin.class );
        when( plugin2.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin2.getName() ).thenReturn( "plugin1" );

        plugins.add( plugin1 );
        plugins.add( plugin2 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );

        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);

        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );


        assertTrue(migrationManager.getExecutionOrder().get(0).getName() == "plugin1");
        assertTrue(migrationManager.getExecutionOrder().get(1).getName() == "plugin2");

    }
    @Test
    public void testRunning() throws MigrationException {

        final Set<MigrationPlugin> plugins = new HashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );

        when( plugin1.getName() ).thenReturn( "plugin1" );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        plugins.add( plugin1 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );

        when( migrationInfoSerialization.getStatusCode( "plugin1" ) )
            .thenReturn( DataMigrationManagerImpl.StatusCode.RUNNING.status );

        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);

        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );


        boolean status = migrationManager.isRunning();

        assertTrue( "Status is set", status );


        when( migrationInfoSerialization.getStatusCode( "plugin1" ) )
            .thenReturn( DataMigrationManagerImpl.StatusCode.COMPLETE.status );

        status = migrationManager.isRunning();

        assertFalse( "Status is not running", status );


        when( migrationInfoSerialization.getStatusCode( "plugin1" ) )
            .thenReturn( DataMigrationManagerImpl.StatusCode.ERROR.status );

        status = migrationManager.isRunning();

        assertFalse( "Status is not running", status );
    }


    @Test
    public void testExecutionOrder() throws MigrationException {


        //linked hash set is intentional here.  For iteration order we can boostrap to come second so we can
        //verify it was actually run first
        final Set<MigrationPlugin> plugins = new LinkedHashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );

        when( plugin1.getName() ).thenReturn( "plugin1" );

        //boostrap plugin, should run first
        MigrationPlugin plugin2 = mock( MigrationPlugin.class );
        when( plugin2.getPhase() ).thenReturn( PluginPhase.BOOTSTRAP );

        when( plugin2.getName() ).thenReturn( "plugin2" );

        plugins.add( plugin1 );
        plugins.add( plugin2 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );

        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);

        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );


        Set<String> pluginNames = migrationManager.getPluginNames();

        assertEquals( 2, pluginNames.size() );

        assertTrue( pluginNames.contains( "plugin1" ) );

        assertTrue( pluginNames.contains( "plugin2" ) );

        //now run them

        migrationManager.migrate();


        //we want to verify the bootsrap plugin was called first
        InOrder inOrderVerification = inOrder( plugin1, plugin2 );
        inOrderVerification.verify( plugin2 ).run( any( ProgressObserver.class ) );
        inOrderVerification.verify( plugin1 ).run( any( ProgressObserver.class ) );

        verify(migrationInfoCache, Mockito.times(2)).invalidateAll();
    }


    /**
     * Happy path of version reset
     */
    @Test
    public void testResetToVersion() {

        final String name = "plugin1";
        final int version = 10;

        //linked hash set is intentional here.  For iteration order we can boostrap to come second so we can
        //verify it was actually run first
        final Set<MigrationPlugin> plugins = new LinkedHashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );


        when( plugin1.getName() ).thenReturn( name );
        when( plugin1.getMaxVersion() ).thenReturn( version );

        plugins.add( plugin1 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );

        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);

        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );

        migrationManager.resetToVersion( name, 0 );

        verify( migrationInfoSerialization ).setVersion( name, 0 );


        migrationManager.resetToVersion( name, version );

        verify( migrationInfoSerialization ).setVersion( name, version );
    }


    /**
     * Reset of version that is too high or too low
     */
    @Test( expected = IllegalArgumentException.class )
    public void testResetToInvalidVersions() {
        final String name = "plugin1";
        final int version = 10;

        //linked hash set is intentional here.  For iteration order we can boostrap to come second so we can
        //verify it was actually run first
        final Set<MigrationPlugin> plugins = new LinkedHashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );


        when( plugin1.getName() ).thenReturn( name );
        when( plugin1.getMaxVersion() ).thenReturn( version );

        plugins.add( plugin1 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );

        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );

        migrationManager.resetToVersion( name, version + 1 );
    }


    /**
     * Reset with no plugin name
     */
    @Test( expected = IllegalArgumentException.class )
    public void testResetInvalidName() {
        final String name = "plugin1";
        final int version = 10;

        //linked hash set is intentional here.  For iteration order we can boostrap to come second so we can
        //verify it was actually run first
        final Set<MigrationPlugin> plugins = new LinkedHashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );


        when( plugin1.getName() ).thenReturn( name );
        when( plugin1.getMaxVersion() ).thenReturn( version );

        plugins.add( plugin1 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );

        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);

        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );

        migrationManager.resetToVersion( name + "foo", version );
    }


    @Test
    public void testLastStatus() {

        final String name = "plugin1";
        final String status = "some status";

        //linked hash set is intentional here.  For iteration order we can boostrap to come second so we can
        //verify it was actually run first
        final Set<MigrationPlugin> plugins = new LinkedHashSet<>();

        MigrationPlugin plugin1 = mock( MigrationPlugin.class );
        when( plugin1.getPhase() ).thenReturn( PluginPhase.MIGRATE );


        when( plugin1.getName() ).thenReturn( name );



        plugins.add( plugin1 );


        final MigrationInfoSerialization migrationInfoSerialization = mock( MigrationInfoSerialization.class );
        when(migrationInfoSerialization.getStatusMessage( name )).thenReturn( status  );

        final MigrationInfoCache migrationInfoCache = mock(MigrationInfoCache.class);

        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization, migrationInfoCache );

        final String returnedStatus = migrationManager.getLastStatus( name );


        assertEquals(status, returnedStatus);
    }


}
