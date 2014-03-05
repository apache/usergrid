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

import com.google.inject.Inject;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import static org.junit.Assert.assertNotNull;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;


@RunWith(JukitoRunner.class)
@UseModules(TestCollectionModule.class)
public class EntityCollectionManagerST {
    private static final Logger log = LoggerFactory.getLogger( EntityCollectionManagerST.class );

    @Inject
    private EntityCollectionManagerFactory factory;

    @ClassRule
    public static CassandraRule rule = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Test
    public void writeThousands() {

        CollectionScope context = new CollectionScopeImpl(
                new SimpleId("organization"), new SimpleId("test"), "test");

        EntityCollectionManager manager = factory.createCollectionManager(context);

        int limit = 10000;

        StopWatch timer = new StopWatch();
        timer.start();

        for (int i = 0; i < limit; i++) {

            Entity newEntity = new Entity(new SimpleId("test"));
            Observable<Entity> observable = manager.write(newEntity);
            Entity returned = observable.toBlockingObservable().lastOrDefault(null);
            assertNotNull("Returned has a id", returned.getId());
            assertNotNull("Returned has a version", returned.getVersion());

            Entity fetched = manager.load( returned.getId() ).toBlockingObservable().last();
            assertNotNull("Returned has a id", fetched.getId());
            assertNotNull("Returned has a version", fetched.getVersion());

            if ( i % 1000 == 0 ) {
                log.info("   Wrote: " + i);
            }
        }

        timer.stop();
        log.info( "Total time to write {} entries {}ms, average {}ms/entry", 
            limit, timer.getTime(), timer.getTime() / limit );
    }
}
