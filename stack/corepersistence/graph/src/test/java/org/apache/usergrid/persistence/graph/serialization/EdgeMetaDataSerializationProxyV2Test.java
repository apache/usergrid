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

package org.apache.usergrid.persistence.graph.serialization;


import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationProxyImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationV2Impl;
import org.apache.usergrid.persistence.graph.serialization.impl.migration.GraphMigrationPlugin;

import com.google.inject.Inject;

import static org.junit.Assert.assertTrue;


/**
 * Test for when V2 is the current version
 */
@RunWith( ITRunner.class )
@UseModules( { TestGraphModule.class } )
public class EdgeMetaDataSerializationProxyV2Test extends EdgeMetadataSerializationTest {


    @Inject
    protected EdgeMetadataSerialization serialization;

    @Inject
    protected EdgeMetadataSerializationV2Impl edgeMetadataSerializationV2;


    @Inject
    protected MigrationInfoSerialization migrationInfoSerialization;

    private int existingVersion;


    /**
     * We need to run our migration to ensure that we are on the current version, and everything still functions
     * correctly
     */
    @Before
    public void setMigrationVersion() {
        existingVersion = migrationInfoSerialization.getVersion( GraphMigrationPlugin.PLUGIN_NAME );

        //set our version equal to the new version so it only delegates to the new version
        migrationInfoSerialization
                .setVersion( GraphMigrationPlugin.PLUGIN_NAME, edgeMetadataSerializationV2.getImplementationVersion() );
    }


    @After
    public void reSetMigrationVersion() {
        migrationInfoSerialization.setVersion(GraphMigrationPlugin.PLUGIN_NAME,  existingVersion );
    }


    @Override
    protected EdgeMetadataSerialization getSerializationImpl() {
        assertTrue( serialization instanceof EdgeMetadataSerializationProxyImpl );

        return serialization;
    }
}
