/*
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
 */
package org.apache.usergrid.persistence.collection;


import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.time.StopWatch;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.value.Location;

import com.google.inject.Inject;

import static org.junit.Assert.assertNotNull;


@RunWith(ITRunner.class)
@UseModules(TestCollectionModule.class)
@Ignore("Stress test should not be run in embedded mode")
public class EntityCollectionManagerStressTest {
    private static final Logger log = LoggerFactory.getLogger(
            EntityCollectionManagerStressTest.class );

    @Inject
    private EntityCollectionManagerFactory factory;

      @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Test
    public void writeThousands() {

        ApplicationScope context = new ApplicationScopeImpl(new SimpleId("organization"));

        EntityCollectionManager manager = factory.createCollectionManager(context);

        int limit = 10000;

        StopWatch timer = new StopWatch();
        timer.start();
        Set<Id> ids = new HashSet<Id>();
        for (int i = 0; i < limit; i++) {

            Entity newEntity = new Entity(new SimpleId("test"));
            newEntity.setField(new StringField("name", String.valueOf(i)));
            newEntity.setField(new LocationField("location", new Location(120,40)));

            Entity returned = manager.write(newEntity).toBlocking().last();

            assertNotNull("Returned has a id", returned.getId());
            assertNotNull("Returned has a version", returned.getVersion());

            ids.add(returned.getId());

            if ( i % 1000 == 0 ) {
                log.info("   Wrote: " + i);
            }
        }
        timer.stop();
        log.info( "Total time to write {} entries {}ms", limit, timer.getTime());
        timer.reset();

        timer.start();
        for ( Id id : ids ) {
            Entity entity = manager.load( id ).toBlocking().last();
            assertNotNull("Returned has a id", entity.getId());
            assertNotNull("Returned has a version", entity.getVersion());
        }
        timer.stop();
        log.info( "Total time to read {} entries {}ms", limit, timer.getTime());
    }
}
