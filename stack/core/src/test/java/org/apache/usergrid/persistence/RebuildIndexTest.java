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


import java.util.*;

import net.jcip.annotations.NotThreadSafe;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.index.ReIndexRequestBuilder;
import org.apache.usergrid.corepersistence.index.ReIndexService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@NotThreadSafe
public class RebuildIndexTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger(RebuildIndexTest.class);

    private static final MetricRegistry registry = new MetricRegistry();


    private static final int ENTITIES_TO_INDEX = 1000;


    @Before
    public void startReporting() {

        if (logger.isDebugEnabled()) {
            logger.debug("Starting metrics reporting");
        }
    }


    @After
    public void printReport() {
        logger.debug("Printing metrics report");
    }


    @Test(timeout = 240000)
    public void rebuildOneCollectionIndex() throws Exception {

        logger.info("Started rebuildOneCollectionIndex()");

        String rand = RandomStringUtils.randomAlphanumeric(5);
        final UUID appId = setup.createApplication("org_" + rand, "app_" + rand);

        final EntityManager em = setup.getEmf().getEntityManager(appId);

        final ReIndexService reIndexService = setup.getInjector().getInstance(ReIndexService.class);

        // ----------------- create a bunch of entities

        Map<String, Object> entityMap = new HashMap<String, Object>() {{
            put("key1", 1000);
            put("key2", 2000);
            put("key3", "Some value");
        }};


        List<EntityRef> entityRefs = new ArrayList<EntityRef>();
        int herderCount = 0;
        int shepardCount = 0;
        for (int i = 0; i < ENTITIES_TO_INDEX; i++) {

            final Entity entity;

            try {
                entityMap.put("key", i);

                if (i % 2 == 0) {
                    entity = em.create("catherder", entityMap);
                    herderCount++;
                } else {
                    entity = em.create("catshepard", entityMap);
                    shepardCount++;
                }
            } catch (Exception ex) {
                throw new RuntimeException("Error creating entity", ex);
            }

            entityRefs.add(new SimpleEntityRef(entity.getType(), entity.getUuid()));
            if (i % 10 == 0) {
                logger.info("Created {} entities", i);
            }
        }

        logger.info("Created {} entities", ENTITIES_TO_INDEX);
        app.waitForQueueDrainAndRefreshIndex(1000);

        // ----------------- test that we can read them, should work fine

        logger.debug("Read the data");
        retryReadData(em, "catherders", herderCount, 0, 10);
        retryReadData(em, "catshepards", shepardCount, 0, 10);

        // ----------------- delete the system and application indexes

        logger.debug("Deleting apps");
        deleteIndex(em.getApplicationId());

        // ----------------- test that we can read them, should fail

        logger.debug("Reading data, should fail this time ");

        //should be no data
        readData(em, "testTypes", 0, 0);


        //        ----------------- rebuild index for catherders only

        logger.debug("Preparing to rebuild all indexes");


        final ReIndexRequestBuilder builder =
            reIndexService.getBuilder().withApplicationId(em.getApplicationId()).withCollection("catherders");

        ReIndexService.ReIndexStatus status = reIndexService.rebuildIndex(builder);

        assertNotNull(status.getCollectionName(), "Collection name is present");

        logger.info("Rebuilt index");


        waitForRebuild(em.getApplicationId().toString(), status.getCollectionName(), reIndexService);

        //app.waitForQueueDrainAndRefreshIndex(15000);

        // ----------------- test that we can read the catherder collection and not the catshepard

        retryReadData(em, "catherders", herderCount, 0, 30);
        retryReadData(em, "catshepards", 0, 0, 30);
    }


    @Test(timeout = 240000)
    public void rebuildIndex() throws Exception {

        logger.info("Started rebuildIndex()");

        String rand = RandomStringUtils.randomAlphanumeric(5);
        final UUID appId = setup.createApplication("org_" + rand, "app_" + rand);

        final EntityManager em = setup.getEmf().getEntityManager(appId);

        final ReIndexService reIndexService = setup.getInjector().getInstance(ReIndexService.class);

        // ----------------- create a bunch of entities

        Map<String, Object> entityMap = new HashMap<String, Object>() {{
            put("key1", 1000);
            put("key2", 2000);
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

        Entity cat1 = em.create("cat", cat1map);
        Entity cat2 = em.create("cat", cat2map);
        Entity cat3 = em.create("cat", cat3map);

        List<EntityRef> entityRefs = new ArrayList<>();

        for (int i = 0; i < ENTITIES_TO_INDEX; i++) {

            final Entity entity;

            try {
                entityMap.put("key", i);
                entity = em.create("testType", entityMap);

                em.createConnection(entity, "herds", cat1);
                em.createConnection(entity, "herds", cat2);
                em.createConnection(entity, "herds", cat3);
            } catch (Exception ex) {
                throw new RuntimeException("Error creating entity", ex);
            }

            entityRefs.add(new SimpleEntityRef(entity.getType(), entity.getUuid()));
            if (i % 10 == 0) {
                logger.info("Created {} entities", i);
            }
        }

        logger.info("Created {} entities", ENTITIES_TO_INDEX);
        //app.waitForQueueDrainAndRefreshIndex(30000);

        // ----------------- test that we can read them, should work fine

        logger.debug("Read the data");
        final String collectionName = "testtypes";

        retryReadData(em, collectionName, ENTITIES_TO_INDEX, 3, 20);

        readData(em, collectionName, ENTITIES_TO_INDEX, 3);

        // ----------------- delete the system and application indexes

        logger.debug("Deleting app index");

        deleteIndex(em.getApplicationId());

        app.waitForQueueDrainAndRefreshIndex();

        // ----------------- test that we can read them, should fail

        // deleting sytem app index will interfere with other concurrently running tests
        //deleteIndex( CpNamingUtils.SYSTEM_APP_ID );

        // ----------------- test that we can read them, should fail

        logger.debug("Reading data, should fail this time ");

        readData(em, collectionName, 0, 0);


        // ----------------- rebuild index

        logger.debug("Preparing to rebuild all indexes");
        ;


        try {

            final ReIndexRequestBuilder builder =
                reIndexService.getBuilder().withApplicationId(em.getApplicationId());

            ReIndexService.ReIndexStatus status = reIndexService.rebuildIndex(builder);

            assertNotNull(status.getJobId(), "JobId is present");

            logger.info("Rebuilt index, jobID={}", status.getJobId());


            waitForRebuild(status.getJobId(), reIndexService);


            logger.info("Rebuilt index");

        } catch (Exception ex) {
            logger.error("Error rebuilding index", ex);
            fail();
        }

        // ----------------- test that we can read them

        app.waitForQueueDrainAndRefreshIndex(15000);
        readData(em, collectionName, ENTITIES_TO_INDEX, 3);
    }

    @Test(timeout = 120000)
    public void rebuildIndexGeo() throws Exception {

        logger.info("Started rebuildIndexGeo()");

        String rand = RandomStringUtils.randomAlphanumeric(5);
        final UUID appId = setup.createApplication("org_" + rand, "app_" + rand);

        final EntityManager em = setup.getEmf().getEntityManager(appId);

        final ReIndexService reIndexService = setup.getInjector().getInstance(ReIndexService.class);

        // ----------------- create a bunch of entities

        Map<String, Object> cat1map = new HashMap<String, Object>() {{
            put("name", "enzo");
            put("color", "grey");
            put("location", new LinkedHashMap<String, Object>() {{
                put("latitude", -35.746369);
                put("longitude", 150.952183);
            }});
        }};
        final double lat = -34.746369;
        final double lon = 152.952183;
        Map<String, Object> cat2map = new HashMap<String, Object>() {{
            put("name", "marquee");
            put("color", "grey");
            put("location", new LinkedHashMap<String, Object>() {{
                put("latitude", lat);
                put("longitude", lon);
            }});
        }};
        Map<String, Object> cat3map = new HashMap<String, Object>() {{
            put("name", "bertha");
            put("color", "grey");
            put("location", new LinkedHashMap<String, Object>() {{
                put("latitude", -33.746369);
                put("longitude", 150.952183);
            }});
        }};

        Entity cat1 = em.create("cat", cat1map);
        Entity cat2 = em.create("cat", cat2map);
        Entity cat3 = em.create("cat", cat3map);


        logger.info("Created {} entities", ENTITIES_TO_INDEX);
        app.waitForQueueDrainAndRefreshIndex(5000);

        // ----------------- test that we can read them, should work fine

        logger.debug("Read the data");
        final String collectionName = "cats";
        Query q = Query.fromQL("select * where color='grey'").withLimit(1000);
        Results results = em.searchCollectionConsistent(em.getApplicationRef(), collectionName, q, 3);
        assertEquals(3, results.size());


        // ----------------- delete the system and application indexes

        logger.debug("Deleting app index");

        deleteIndex(em.getApplicationId());

        // ----------------- test that we can read them, should fail

        // deleting sytem app index will interfere with other concurrently running tests
        //deleteIndex( CpNamingUtils.SYSTEM_APP_ID );

        // ----------------- test that we can read them, should fail

        logger.debug("Reading data, should fail this time ");

        results = em.searchCollectionConsistent(em.getApplicationRef(), collectionName, q, 0);
        assertEquals(results.size(), 0);

        // ----------------- rebuild index

        logger.debug("Preparing to rebuild all indexes");


        try {

            final ReIndexRequestBuilder builder =
                reIndexService.getBuilder().withApplicationId(em.getApplicationId());

            ReIndexService.ReIndexStatus status = reIndexService.rebuildIndex(builder);

            assertNotNull(status.getJobId(), "JobId is present");

            logger.info("Rebuilt index");

            waitForRebuild(status.getJobId(), reIndexService);

            logger.info("Rebuilt index");

        } catch (Exception ex) {
            logger.error("Error rebuilding index", ex);
            fail();
        }

        // ----------------- test that we can read them

        app.waitForQueueDrainAndRefreshIndex(5000);
        results = em.searchCollectionConsistent(em.getApplicationRef(), collectionName, q, 3);
        assertEquals(results.size(), 3);
        q = Query.fromQL("select * where location within 100 of " + lat + ", " + lon);
        results = em.searchCollectionConsistent(em.getApplicationRef(), collectionName, q, 1);
        assertEquals(results.size(), 1);
    }


    @Test(timeout = 120000)
    public void rebuildUpdatedSince() throws Exception {

        logger.info("Started rebuildUpdatedSince()");

        String rand = RandomStringUtils.randomAlphanumeric(5);
        final UUID appId = setup.createApplication("org_" + rand, "app_" + rand);

        final EntityManager em = setup.getEmf().getEntityManager(appId);

        final ReIndexService reIndexService = setup.getInjector().getInstance(ReIndexService.class);

        // ----------------- create a bunch of entities


        Map<String, Object> entityData = new HashMap<String, Object>() {{
            put("key1", 1000);
        }};


        final Entity firstEntity = em.create("thing", entityData);


        final Entity secondEntity = em.create("thing", entityData);

        app.waitForQueueDrainAndRefreshIndex(15000);

        // ----------------- test that we can read them, should work fine

        logger.debug("Read the data");
        final String collectionName = "things";

        countEntities(em, collectionName, 2);

        // ----------------- delete the system and application indexes

        logger.debug("Deleting app index");

        deleteIndex(em.getApplicationId());

        // ----------------- test that we can read them, should fail

        // deleting sytem app index will interfere with other concurrently running tests
        //deleteIndex( CpNamingUtils.SYSTEM_APP_ID );

        // ----------------- test that we can read them, should fail

        if (logger.isDebugEnabled()) {
            logger.debug("Reading data, should fail this time ");
        }

        countEntities(em, collectionName, 0);

        // ----------------- rebuild index

        final long firstUpdatedTimestamp = firstEntity.getModified();
        final long secondUpdatedTimestamp = secondEntity.getModified();

        assertTrue("second should be updated after second", firstUpdatedTimestamp < secondUpdatedTimestamp);


        try {
            final long updatedTimestamp = secondEntity.getModified();

            if (logger.isDebugEnabled()) {
                logger.debug("Preparing to rebuild all indexes with timestamp {}", updatedTimestamp);
            }

            //set our update timestamp
            final ReIndexRequestBuilder builder =
                reIndexService.getBuilder().withApplicationId(em.getApplicationId()).withStartTimestamp(
                    updatedTimestamp);

            ReIndexService.ReIndexStatus status = reIndexService.rebuildIndex(builder);

            assertNotNull(status.getJobId(), "JobId is present");

            logger.info("Rebuilt index");

            waitForRebuild(status.getJobId(), reIndexService);

            logger.info("Rebuilt index");

        } catch (Exception ex) {
            logger.error("Error rebuilding index", ex);
            fail();
        }

        // ----------------- test that we can read them

        app.waitForQueueDrainAndRefreshIndex(5000);
        countEntities(em, collectionName, 1);
    }


    /**
     * Wait for the rebuild to occur
     */
    private void waitForRebuild(final String jobId, final ReIndexService reIndexService)
        throws InterruptedException, IllegalArgumentException {
        if (jobId != null && !jobId.trim().equals("")) {
            logger.info("waitForRebuild: jobID={}", jobId);
        } else {
            logger.info("waitForRebuild: error, jobId = null or empty");
            throw new IllegalArgumentException("jobId = null or empty");
        }
        while (true) {

            try {
                final ReIndexService.ReIndexStatus updatedStatus = reIndexService.getStatus(jobId);

                if (updatedStatus == null) {
                    logger.info("waitForRebuild: updated status is null");
                } else {
                    logger.info("waitForRebuild: status={} numberProcessed={}", updatedStatus.getStatus().toString(), updatedStatus.getNumberProcessed());

                    if (updatedStatus.getStatus() == ReIndexService.Status.COMPLETE) {
                        break;
                    }
                }
            } catch (IllegalArgumentException iae) {
                //swallow.  Thrown if our job can't be found.  I.E hasn't updated yet
            }


            Thread.sleep(1000);
        }
    }
    
    
    /**
     * Wait for the rebuild to occur
     */
    private void waitForRebuild(final String appId, final String collectionName, final ReIndexService reIndexService)
        throws InterruptedException, IllegalArgumentException {
        if (appId != null && !appId.trim().equals("") && collectionName != null && !collectionName.trim().equals("")) {
            logger.info("waitForRebuild: appId={} collName={}", appId, collectionName);
        } else {
            logger.info("waitForRebuild: error, appId or collName = null or empty");
            throw new IllegalArgumentException("appId or collName = null or empty");
        }
        while (true) {

            try {
                final ReIndexService.ReIndexStatus updatedStatus = reIndexService.getStatusForCollection(appId, collectionName);

                if (updatedStatus == null) {
                    logger.info("waitForRebuild: updated status is null");
                } else {
                    logger.info("waitForRebuild: status={} numberProcessed={}", updatedStatus.getStatus().toString(), updatedStatus.getNumberProcessed());

                    if (updatedStatus.getStatus() == ReIndexService.Status.COMPLETE) {
                        break;
                    }
                }
            } catch (IllegalArgumentException iae) {
                //swallow.  Thrown if our job can't be found.  I.E hasn't updated yet
            }


            Thread.sleep(1000);
        }
    }


    /**
     * Delete app index
     */
    private void deleteIndex(UUID appUuid) {

        Injector injector = SpringResource.getInstance().getBean(Injector.class);
        IndexLocationStrategyFactory indexLocationStrategyFactory = injector.getInstance(IndexLocationStrategyFactory.class);
        EntityIndexFactory eif = injector.getInstance(EntityIndexFactory.class);

        Id appId = new SimpleId(appUuid, Schema.TYPE_APPLICATION);
        ApplicationScope scope = new ApplicationScopeImpl(appId);
        EntityIndex ei = eif.createEntityIndex(
            indexLocationStrategyFactory.getIndexLocationStrategy(scope)
        );

        ei.deleteApplication().toBlocking().lastOrDefault(null);
        app.waitForQueueDrainAndRefreshIndex();
    }

    private int retryReadData(EntityManager em, String collectionName, int expectedEntities, int expectedConnections, int retry) throws Exception {
        int count =  readData(em, collectionName, expectedEntities, expectedConnections);
        while (count != expectedEntities && --retry >=0) {
            count =  readData(em, collectionName, expectedEntities, expectedConnections);
        }
        assertEquals( "Did not get expected entities", expectedEntities, count );
        return count;
    }

    private int readData( EntityManager em, String collectionName, int expectedEntities, int expectedConnections )
        throws Exception {

        app.waitForQueueDrainAndRefreshIndex();

        Query q = Query.fromQL( "select * where key1=1000" ).withLimit( 1000 );
        Results results = em.searchCollectionConsistent( em.getApplicationRef(), collectionName, q, expectedEntities );

        int count = 0;
        while ( true ) {

            for ( Entity e : results.getEntities() ) {

                assertEquals( 2000, e.getProperty( "key2" ) );

                Results catResults =
                    em.searchTargetEntities( e, Query.fromQL( "select *" ).setConnectionType( "herds" ) );
                assertEquals( expectedConnections, catResults.size() );

                if ( count % 100 == 0 ) {
                    logger.info( "read {} entities", count );
                }
                count++;
            }

            if ( results.hasCursor() ) {
                logger.info( "Counted {} : query again with cursor", count );
                q.setCursor( results.getCursor() );
                results = em.searchCollection( em.getApplicationRef(), collectionName, q );
            }
            else {
                break;
            }
        }

        return count;
    }

    private int countEntities( EntityManager em, String collectionName, int expectedEntities)
           throws Exception {

           app.waitForQueueDrainAndRefreshIndex();

           Query q = Query.fromQL( "select * where key1=1000" ).withLimit( 1000 );
           Results results = em.searchCollectionConsistent( em.getApplicationRef(), collectionName, q, expectedEntities );

           int count = 0;
           while ( true ) {

               count += results.size();


               if ( results.hasCursor() ) {
                   logger.info( "Counted {} : query again with cursor", count );
                   q.setCursor( results.getCursor() );
                   results = em.searchCollection( em.getApplicationRef(), collectionName, q );
               }
               else {
                   break;
               }
           }

           assertEquals( "Did not get expected entities", expectedEntities, count );
           return count;
       }


}
