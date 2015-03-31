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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.impl.EsEntityIndexImpl;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


//@RunWith(JukitoRunner.class)
//@UseModules({ GuiceModule.class })

public class PerformanceEntityRebuildIndexTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceEntityRebuildIndexTest.class );

    private static final MetricRegistry registry = new MetricRegistry();
    private Slf4jReporter reporter;

    private static final long RUNTIME_MS = TimeUnit.SECONDS.toMillis( 10 );

    private static final long WRITE_DELAY_MS = 10;



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
    public void rebuildOneCollectionIndex() throws Exception {

        logger.info("Started rebuildIndex()");

        String rand = RandomStringUtils.randomAlphanumeric(5);
        final UUID appId = setup.createApplication("org_" + rand, "app_" + rand);

        final EntityManager em = setup.getEmf().getEntityManager( appId );

        // ----------------- create a bunch of entities

        Map<String, Object> entityMap = new HashMap<String, Object>() {{
            put("key1", 1000 );
            put("key2", 2000 );
            put("key3", "Some value");
        }};

//        Map<String, Object> cat1map = new HashMap<String, Object>() {{
//            put("name", "enzo");
//            put("color", "orange");
//        }};
//        Map<String, Object> cat2map = new HashMap<String, Object>() {{
//            put("name", "marquee");
//            put("color", "grey");
//        }};
//        Map<String, Object> cat3map = new HashMap<String, Object>() {{
//            put("name", "bertha");
//            put("color", "tabby");
//        }};
//
//        Entity cat1 = em.create("cat", cat1map );
//        Entity cat2 = em.create("cat", cat2map );
//        Entity cat3 = em.create("cat", cat3map );

        final long stopTime = System.currentTimeMillis() + RUNTIME_MS;

        List<EntityRef> entityRefs = new ArrayList<EntityRef>();
        int entityCount = 0;
        int herderCount  = 0;
        int shepardCount = 0;
        while ( System.currentTimeMillis() < stopTime ) {

            final Entity entity;

            try {
                entityMap.put("key", entityCount );

                if ( entityCount % 2 == 0 ) {
                    entity = em.create("catherder", entityMap);
                    herderCount++;
                } else {
                    entity = em.create("catshepard", entityMap);
                    shepardCount++;
                }

                app.refreshIndex();

//                em.createConnection(entity, "herds", cat1);
//                em.createConnection(entity, "herds", cat2);
//                em.createConnection(entity, "herds", cat3);

            } catch (Exception ex) {
                throw new RuntimeException("Error creating entity", ex);
            }

            entityRefs.add(new SimpleEntityRef( entity.getType(), entity.getUuid() ) );
            if ( entityCount % 10 == 0 ) {
                logger.info("Created {} entities", entityCount );
            }

            entityCount++;
            try { Thread.sleep( WRITE_DELAY_MS ); } catch (InterruptedException ignored ) {}
        }

        logger.info("Created {} entities", entityCount);
        app.refreshIndex();

        // ----------------- test that we can read them, should work fine

        logger.debug("Read the data");
        readData( em, "catherders", herderCount, 0);
        readData( em, "catshepards", shepardCount, 0);

        // ----------------- delete the system and application indexes

        logger.debug("Deleting apps");
        deleteIndex( em.getApplicationId() );

        // ----------------- test that we can read them, should fail

        logger.debug("Reading data, should fail this time ");
        try {
            readData( em,  "testTypes", entityCount, 0 );
            fail("should have failed to read data");

        } catch (Exception expected) {}

//        ----------------- rebuild index for catherders only

        logger.debug("Preparing to rebuild all indexes");;

        final String meterName = this.getClass().getSimpleName() + ".rebuildIndex";
        final Meter meter = registry.meter( meterName );

        EntityManagerFactory.ProgressObserver po = new EntityManagerFactory.ProgressObserver() {
            int counter = 0;

            @Override
            public void onProgress( final EntityRef entity ) {

                meter.mark();
                logger.debug("Indexing {}:{}", entity.getType(), entity.getUuid());
                if ( counter % 100 == 0 ) {
                    logger.info("Reindexed {} entities", counter );
                }
                counter++;
            }



        };

        try {

            // do it forwards
            setup.getEmf().rebuildCollectionIndex( em.getApplicationId(), "catherders", false, po );

            // and backwards, just to make sure both cases are covered
            setup.getEmf().rebuildCollectionIndex( em.getApplicationId(), "catherders", true, po );

            reporter.report();
            registry.remove( meterName );
            logger.info("Rebuilt index");

        } catch (Exception ex) {
            logger.error("Error rebuilding index", ex);
            fail();
        }

        // ----------------- test that we can read the catherder collection and not the catshepard

        readData( em, "catherders", herderCount, 0 );
        readData( em, "catshepards", 0, 0 );
    }


    @Test
    public void rebuildIndex() throws Exception {

        logger.info("Started rebuildIndex()");

        String rand = RandomStringUtils.randomAlphanumeric(5);
        final UUID appId = setup.createApplication("org_" + rand, "app_" + rand);

        final EntityManager em = setup.getEmf().getEntityManager(appId);

        // ----------------- create a bunch of entities

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

        final long stopTime = System.currentTimeMillis() + RUNTIME_MS;

        List<EntityRef> entityRefs = new ArrayList<EntityRef>();
        int entityCount = 0;
        while ( System.currentTimeMillis() < stopTime ) {

            final Entity entity;

            try {
                entityMap.put("key", entityCount );
                entity = em.create("testType", entityMap );

                app.refreshIndex();

                em.createConnection(entity, "herds", cat1);
                em.createConnection(entity, "herds", cat2);
                em.createConnection(entity, "herds", cat3);

            } catch (Exception ex) {
                throw new RuntimeException("Error creating entity", ex);
            }

            entityRefs.add(new SimpleEntityRef( entity.getType(), entity.getUuid() ) );
            if ( entityCount % 10 == 0 ) {
                logger.info("Created {} entities", entityCount );
            }

            entityCount++;
            try { Thread.sleep( WRITE_DELAY_MS ); } catch (InterruptedException ignored ) {}
        }

        logger.info("Created {} entities", entityCount);
        app.refreshIndex();

        // ----------------- test that we can read them, should work fine

        logger.debug("Read the data");
        readData( em, "testTypes", entityCount, 3 );

        // ----------------- delete the system and application indexes

        logger.debug("Deleting app index");

        deleteIndex( em.getApplicationId() );

        // ----------------- test that we can read them, should fail

        // deleting sytem app index will interfere with other concurrently running tests
        //deleteIndex( CpNamingUtils.SYSTEM_APP_ID );

        // ----------------- test that we can read them, should fail

        logger.debug("Reading data, should fail this time ");
        try {
            readData( em, "testTypes", entityCount, 3 );
            fail("should have failed to read data");

        } catch (Exception expected) {}

        // ----------------- rebuild index

        logger.debug("Preparing to rebuild all indexes");;

        final String meterName = this.getClass().getSimpleName() + ".rebuildIndex";
        final Meter meter = registry.meter( meterName );

        EntityManagerFactory.ProgressObserver po = new EntityManagerFactory.ProgressObserver() {
            int counter = 0;

            @Override
            public void onProgress( final EntityRef entity ) {

                meter.mark();
                logger.debug("Indexing {}:{}", entity.getType(), entity.getUuid());
                if ( counter % 100 == 0 ) {
                    logger.info("Reindexed {} entities", counter );
                }
                counter++;
            }


        };

        try {

            setup.getEmf().rebuildInternalIndexes( po );

            setup.getEmf().rebuildApplicationIndexes( em.getApplicationId(), po );

            reporter.report();
            registry.remove( meterName );
            logger.info("Rebuilt index");

            app.refreshIndex();

        } catch (Exception ex) {
            logger.error("Error rebuilding index", ex);
            fail();
        }

        // ----------------- test that we can read them

        Thread.sleep(2000);
        readData( em, "testTypes", entityCount, 3 );
    }

    /**
     * Delete app index
     */
    private void deleteIndex( UUID appUuid ) {

        Injector injector = SpringResource.getInstance().getBean( Injector.class );
        EntityIndexFactory eif = injector.getInstance( EntityIndexFactory.class );

        Id appId = new SimpleId( appUuid, Schema.TYPE_APPLICATION );
        ApplicationScope scope = new ApplicationScopeImpl( appId );
        ApplicationEntityIndex ei = eif.createApplicationEntityIndex(scope);

        ei.deleteApplication().toBlocking().lastOrDefault(null);
        app.refreshIndex();

    }


    private int readData( EntityManager em,
        String collectionName, int expectedEntities, int expectedConnections ) throws Exception {

        app.refreshIndex();

        Query q = Query.fromQL("select * where key1=1000");
        q.setLimit(40);
        Results results = em.searchCollection( em.getApplicationRef(), collectionName, q );

        int count = 0;
        while ( true ) {

            for ( Entity e : results.getEntities() ) {

                assertEquals( 2000, e.getProperty("key2"));

                Results catResults = em.searchConnectedEntities(e,
                    Query.fromQL("select *").setConnectionType( "herds" ));
                assertEquals( expectedConnections, catResults.size() );

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

        if ( expectedEntities != -1 && expectedEntities != count ) {
            throw new RuntimeException("Did not get expected "
                + expectedEntities + " entities, instead got " + count );
        }
        return count;
    }
}
