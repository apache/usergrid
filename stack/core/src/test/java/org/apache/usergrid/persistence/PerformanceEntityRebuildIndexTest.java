/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence;


import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.Application;
import org.apache.usergrid.CoreApplication;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.usergrid.cassandra.Concurrent;
import static org.junit.Assert.fail;


//@RunWith(JukitoRunner.class)
//@UseModules({ GuiceModule.class })
@Concurrent()
public class PerformanceEntityRebuildIndexTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceEntityRebuildIndexTest.class );

    private static final MetricRegistry registry = new MetricRegistry();
    private Slf4jReporter reporter;

    private static final long RUNTIME = TimeUnit.MINUTES.toMillis( 1 );

    private static final long writeDelayMs = 9;
    //private static final long readDelayMs = 7;

    @Rule
    public Application app = new CoreApplication( setup );


    @Before
    public void startReporting() {

        logger.debug("Starting metrics reporting");
        reporter = Slf4jReporter.forRegistry( registry ).outputTo( logger )
                .convertRatesTo( TimeUnit.SECONDS )
                .convertDurationsTo( TimeUnit.MILLISECONDS ).build();

        reporter.start( 10, TimeUnit.SECONDS );
    }


    @After
    public void printReport() {

        logger.debug("Printing metrics report");
        reporter.report();
        reporter.stop();
    }


    @Test
    public void rebuildIndex() {

        logger.info("Started rebuildIndex()");

        final EntityManager em = app.getEntityManager();
        final long stopTime = System.currentTimeMillis() + RUNTIME;
        final Map<String, Object> entityMap = new HashMap<>();

        entityMap.put( "key1", 1000 );
        entityMap.put( "key2", 2000 );
        entityMap.put( "key3", "Some value" );

        List<EntityRef> entityRefs = new ArrayList<EntityRef>();

        int i = 0;
        while ( System.currentTimeMillis() < stopTime ) {

            entityMap.put( "key", i );
            final Entity created;
            try {
                created = em.create("testType", entityMap );
            } catch (Exception ex) {
                throw new RuntimeException("Error creating entity", ex);
            }

            entityRefs.add( new SimpleEntityRef( created.getType(), created.getUuid() ) );

            if ( i % 100 == 0 ) {
                logger.info("Created {} entities", i );
            }
            i++;

            try { Thread.sleep( writeDelayMs ); } catch (InterruptedException ignored ) {}
        }
        logger.info("Created {} entities", i);


        final String meterName = this.getClass().getSimpleName() + ".rebuildIndex";
        final Meter meter = registry.meter( meterName );
        
        EntityManagerFactory.ProgressObserver po = new EntityManagerFactory.ProgressObserver() {
            int counter = 0;
            @Override
            public void onProgress( EntityRef s, EntityRef t, String etype ) {

                meter.mark();

                logger.debug("Indexing from {}:{} to {}:{} edgeType {}", new Object[] {
                    s.getType(), s.getUuid(), t.getType(), t.getUuid(), etype });

                if ( !logger.isDebugEnabled() && counter % 100 == 0 ) {
                    logger.info("Reindexed {} entities", counter );
                }
                counter++;
            }
        };

        try {
            setup.getEmf().rebuildAllIndexes( po );

            registry.remove( meterName );
            logger.info("Finished rebuildIndex()");

        } catch (Exception ex) {
            logger.error("Error rebuilding index", ex);
            fail();
        }

    }
}
