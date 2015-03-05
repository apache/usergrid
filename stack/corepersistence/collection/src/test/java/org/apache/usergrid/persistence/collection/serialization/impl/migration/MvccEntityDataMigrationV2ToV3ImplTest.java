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

package org.apache.usergrid.persistence.collection.serialization.impl.migration;


import org.junit.Rule;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.CollectionDataVersions;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyV2Impl;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyV3Impl;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;

import com.google.inject.Inject;

import net.jcip.annotations.NotThreadSafe;


@NotThreadSafe
@RunWith( ITRunner.class )
@UseModules( { TestCollectionModule.class } )
public class MvccEntityDataMigrationV2ToV3ImplTest extends AbstractMvccEntityDataMigrationV1ToV3ImplTest{


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    public DataMigrationManager dataMigrationManager;

    @Inject
    private MvccEntitySerializationStrategyV2Impl v2Impl;

    @Inject
    private MvccEntitySerializationStrategyV3Impl v3Impl;

    @Inject
    public MvccEntityDataMigrationImpl mvccEntityDataMigrationImpl;


    @Inject
    public VersionedMigrationSet<MvccEntitySerializationStrategy> versions;



    @Override
    public DataMigrationManager getDataMigrationManager() {
        return dataMigrationManager;
    }


    @Override
    protected MvccEntitySerializationStrategy getExpectedSourceImpl() {
        return v2Impl;
    }


    @Override
    protected MvccEntitySerializationStrategy getExpectedTargetImpl() {
        return v3Impl;
    }


    @Override
    protected CollectionDataVersions getSourceVersion() {
        return CollectionDataVersions.BUFFER_SHORT_FIX;
    }


    @Override
    protected CollectionDataVersions expectedTargetVersion() {
        return CollectionDataVersions.LOG_REMOVAL;
    }
}
