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
import java.util.Set;

import org.junit.Test;

import org.apache.usergrid.persistence.core.migration.schema.MigrationException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class DataMigrationManagerImplTest {


    @Test
    public void testNoPlugins(){

        final Set<MigrationPlugin> plugins = new HashSet<>(  );

        final  MigrationInfoSerialization migrationInfoSerialization = mock(MigrationInfoSerialization.class);


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization );


        Set<String> pluginNames = migrationManager.getPluginNames();

        assertEquals(0, pluginNames.size());

    }



    @Test
    public void test2Plugins() throws MigrationException {

        final Set<MigrationPlugin> plugins = new HashSet<>(  );

        MigrationPlugin plugin1 = mock(MigrationPlugin.class);

        when(plugin1.getName()).thenReturn("plugin1");

        MigrationPlugin plugin2 = mock(MigrationPlugin.class);

        when(plugin2.getName()).thenReturn("plugin2");

        plugins.add( plugin1 );
        plugins.add( plugin2 );



        final  MigrationInfoSerialization migrationInfoSerialization = mock(MigrationInfoSerialization.class);


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization );


        Set<String> pluginNames = migrationManager.getPluginNames();

        assertEquals(2, pluginNames.size());

        assertTrue(pluginNames.contains( "plugin1"));

        assertTrue(pluginNames.contains( "plugin2" ));

        //now run them

        migrationManager.migrate();

        verify(plugin1).run( any(ProgressObserver.class) );

        verify(plugin2).run( any(ProgressObserver.class) );

    }




    @Test
    public void testRunning() throws MigrationException {

        final Set<MigrationPlugin> plugins = new HashSet<>(  );

        MigrationPlugin plugin1 = mock(MigrationPlugin.class);

        when(plugin1.getName()).thenReturn("plugin1");

        plugins.add( plugin1 );



        final  MigrationInfoSerialization migrationInfoSerialization = mock(MigrationInfoSerialization.class);

        when(migrationInfoSerialization.getStatusCode( "plugin1" )).thenReturn( DataMigrationManagerImpl.StatusCode.RUNNING.status );




        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( plugins, migrationInfoSerialization );


        boolean status = migrationManager.isRunning();

        assertTrue("Status is set", status);


        when(migrationInfoSerialization.getStatusCode( "plugin1" )).thenReturn( DataMigrationManagerImpl.StatusCode.COMPLETE.status );

        status = migrationManager.isRunning();

        assertFalse( "Status is not running", status );


        when(migrationInfoSerialization.getStatusCode( "plugin1" )).thenReturn( DataMigrationManagerImpl.StatusCode.ERROR.status );

       status = migrationManager.isRunning();

       assertFalse("Status is not running", status);
    }



}
