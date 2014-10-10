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
import org.apache.usergrid.persistence.index.query.Query;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


//@RunWith(JukitoRunner.class)
//@UseModules({ GuiceModule.class })
@Concurrent()
public class PerformanceEntityRepersistTest extends AbstractCoreIT {
    private static final Logger logger = 
            LoggerFactory.getLogger(PerformanceEntityRepersistTest.class );

    private static final MetricRegistry registry = new MetricRegistry();
    private Slf4jReporter reporter;

    private static final long RUNTIME = TimeUnit.MINUTES.toMillis( 1 );

    private static final long writeDelayMs = 100;
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
    public void repersistAll() throws Exception {

        logger.info("Started repersistAll()");

        final EntityManager em = app.getEntityManager();

        // ----------------- create a bunch of entities

        final long stopTime = System.currentTimeMillis() + RUNTIME;

        Map<String, Object> entityMap = new HashMap<String, Object>() {{
            put("key1", 1000 );
            put("key2", 2000 );
            put("key3", "Some value");
        }};
        Map<String, Object> cat1map = new HashMap<String, Object>() {{
            put("name", "enzo");
            put("color", "orange");
        }};
        Map<String, Object> cat2map = new HashMap<String, Object>() {{
            put("name", "marquee");
            put("color", "grey");
        }};
        Map<String, Object> cat3map = new HashMap<String, Object>() {{
            put("name", "bertha");
            put("color", "tabby");
        }};

        Entity cat1 = em.create("cat", cat1map );
        Entity cat2 = em.create("cat", cat2map );
        Entity cat3 = em.create("cat", cat3map );

        List<EntityRef> entityRefs = new ArrayList<EntityRef>();
        int entityCount = 0;
        while ( System.currentTimeMillis() < stopTime ) {

            final Entity entity;

            try {
                entityMap.put("key", entityCount );
                entity = em.create("testType", entityMap );

                em.refreshIndex();

                em.createConnection(entity, "herds", cat1);
                em.createConnection(entity, "herds", cat2);
                em.createConnection(entity, "herds", cat3);

            } catch (Exception ex) {
                throw new RuntimeException("Error creating entity", ex);
            }

            entityRefs.add(new SimpleEntityRef( entity.getType(), entity.getUuid() ) );
            if ( entityCount % 100 == 0 ) {
                logger.info("Created {} entities", entityCount );
            }

            entityCount++;
            try { Thread.sleep( writeDelayMs ); } catch (InterruptedException ignored ) {}
        }

        logger.info("Created {} entities", entityCount);
        em.refreshIndex();

        // ----------------- test that we can read them, should work fine 

        logger.debug("Read the data");
        readData("testTypes", entityCount );

        // ----------------- repersist all

        logger.debug("Preparing to repersist all");;

        final String meterName = this.getClass().getSimpleName() + ".repersist";
        final Meter meter = registry.meter( meterName );
        
        EntityManagerFactory.ProgressObserver po = new EntityManagerFactory.ProgressObserver() {
            int counter = 0;
            @Override
            public void onProgress( EntityRef s, EntityRef t, String etype ) {
                meter.mark();
                logger.debug("Repersisting from {}:{} to {}:{} ", new Object[] {
                    s.getType(), s.getUuid(), t.getType(), t.getUuid(), etype });
                if ( !logger.isDebugEnabled() && counter % 100 == 0 ) {
                    logger.info("Repersisted {} entities", counter );
                }
                counter++;
            }
        };

        try {
            setup.getEmf().repersistAll(po );

            registry.remove( meterName );
            logger.info("Repersist complete");

        } catch (Exception ex) {
            logger.error("Error repersisting", ex);
            fail();
        }
        em.refreshIndex();

        // ----------------- test that we can read them
        
        readData( "testTypes", entityCount );
    }


    private int readData( String collectionName ) throws Exception {
        return readData( collectionName, -1 );
    }


    private int readData( String collectionName, int expected ) throws Exception {

        EntityManager em = app.getEntityManager();

        Query q = Query.fromQL("select * where key1=1000");
        q.setLimit(40);
        Results results = em.searchCollection( em.getApplicationRef(), collectionName, q );

        int count = 0;
        while ( true ) {

            for ( Entity e : results.getEntities() ) {

                assertEquals( 2000, e.getProperty("key2"));

                Results catResults = em.searchConnectedEntities(e, Query.fromQL("select *"));
                assertEquals( 3, catResults.size() );

                if ( count % 100 == 0 ) {
                    logger.info( "read {} entities", count);
                }
                count++;
            }

            if ( results.hasCursor() ) {
                logger.info( "Counted {} : query again with cursor", count);
                q.setCursor( results.getCursor() );
                results = em.searchCollection( em.getApplicationRef(), collectionName, q );

            } else {
                break;
            }
        }

        if ( expected != -1 && expected != count ) {
            throw new RuntimeException("Did not get expected " 
                    + expected + " entities, instead got " + count );
        }
        return count;
    }
}
