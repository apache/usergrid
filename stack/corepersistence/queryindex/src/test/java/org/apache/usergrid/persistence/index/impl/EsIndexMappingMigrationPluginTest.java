/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index.impl;

import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.core.migration.data.TestProgressObserver;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.migration.EsIndexMappingMigrationPlugin;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void runMigration(){
        EsIndexMappingMigrationPlugin plugin = new EsIndexMappingMigrationPlugin(serialization,provider);
        TestProgressObserver progressObserver = new TestProgressObserver();
        plugin.run(progressObserver);
        assertFalse( "Progress observer should not have failed", progressObserver.isFailed() );
        assertTrue("Progress observer should have update messages", progressObserver.getUpdates().size() > 0);


    }

}
