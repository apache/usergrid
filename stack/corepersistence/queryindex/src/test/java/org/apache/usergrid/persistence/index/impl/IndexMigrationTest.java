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
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.migration.EsIndexDataMigrationImpl;
import org.apache.usergrid.persistence.index.migration.IndexDataVersions;
import org.apache.usergrid.persistence.index.migration.LegacyIndexIdentifier;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;

/**
 * Classy class class.
 */
@RunWith(EsRunner.class)
@UseModules({ TestIndexModule.class })
public class IndexMigrationTest extends BaseIT{


    @Inject
    public IndexFig fig;
    @Inject
    public IndexIdentifier indexIdentifier;

    @Inject
    public IndexCache indexCache;

    @Inject
    public EsProvider provider;

    @Inject
    public AliasedEntityIndex ei;
    @Inject
    public IndexBufferProducer indexBatchBufferProducer;
    @Inject
    public MetricsFactory metricsFactory;

    @Test
    public void TestMigrate(){
        EsIndexDataMigrationImpl indexDataMigration = new EsIndexDataMigrationImpl(ei,provider, fig, indexIdentifier,indexCache);
        ProgressObserver po = new ProgressObserver() {
            @Override
            public void start() {

            }

            @Override
            public void complete() {

            }

            @Override
            public void failed(int migrationVersion, String reason) {
                fail(reason);
            }

            @Override
            public void failed(int migrationVersion, String reason, Throwable throwable) {
                fail(reason);
            }

            @Override
            public void update(int migrationVersion, String message) {

            }
        };

        TestIndexModule.TestAllApplicationsObservable obs = new TestIndexModule.TestAllApplicationsObservable(indexBatchBufferProducer,provider,indexCache,metricsFactory,fig);
        int version = indexDataMigration.migrate(0, obs, po );
        assertEquals(version, IndexDataVersions.SINGLE_INDEX.getVersion());
    }
}



