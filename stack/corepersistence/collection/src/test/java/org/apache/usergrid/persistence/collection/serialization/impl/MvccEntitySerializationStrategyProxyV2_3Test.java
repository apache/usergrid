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


import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.CollectionMigrationPlugin;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;

import com.google.inject.Inject;

import net.jcip.annotations.NotThreadSafe;


@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
@NotThreadSafe//anything that changes the system version state is not safe to be run concurrently
public class MvccEntitySerializationStrategyProxyV2_3Test extends MvccEntitySerializationStrategyV2Test {

    @Inject
    private MvccEntitySerializationStrategy serializationStrategy;

    @Inject
    private MvccEntitySerializationStrategyV2Impl v2Impl;


    @Override
    protected MvccEntitySerializationStrategy getMvccEntitySerializationStrategy() {
        return serializationStrategy;
    }


    @Inject
    protected MigrationInfoSerialization migrationInfoSerialization;

    private int existingVersion;


    /**
     * We need to run our migration to ensure that we are on the current version, and everything still functions
     * correctly
     */
    @Before
    public void setMigrationVersion() {
        existingVersion = migrationInfoSerialization.getVersion( CollectionMigrationPlugin.PLUGIN_NAME);

        //set our new version, so that is will run through the new code
        migrationInfoSerialization.setVersion( CollectionMigrationPlugin.PLUGIN_NAME, v2Impl.getImplementationVersion() );
    }




    @After
    public void reSetMigrationVersion() {
        migrationInfoSerialization.setVersion(CollectionMigrationPlugin.PLUGIN_NAME,  existingVersion );
    }
}
