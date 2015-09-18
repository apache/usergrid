/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index.impl;

import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.TestProgressObserver;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.migration.EsIndexMappingMigrationPlugin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;

/**
 * Classy class class.
 */

@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
public class EsIndexMappingMigrationPluginTest extends BaseIT {
    @Inject
    public EntityIndexFactory eif;
    @Inject
    public MigrationInfoSerialization serialization;
    @Inject
    EsProvider provider;
    @Test
    public void runMigration() {
        MigrationInfoSerialization serialization = Mockito.mock(MigrationInfoSerialization.class);
        Mockito.when(serialization.getVersion(Mockito.any())).thenReturn(0);
        EsIndexMappingMigrationPlugin plugin = new EsIndexMappingMigrationPlugin(serialization,provider);
        TestProgressObserver progressObserver = new TestProgressObserver();
        plugin.run(progressObserver);

        // check for failures
        assertFalse("Progress observer should not have failed", progressObserver.isFailed());

        // after completed, updates could have size 0 or more (0 if no indices present). testing observer's 'updates'
        // size doesn't help.  TODO update test to ensure an index is present before running

    }

}
