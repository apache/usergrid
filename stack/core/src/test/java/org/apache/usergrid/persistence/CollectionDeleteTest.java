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


import com.codahale.metrics.MetricRegistry;
import com.google.inject.Injector;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.index.*;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;


@NotThreadSafe
public class CollectionDeleteTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger( CollectionDeleteTest.class );

    private static final MetricRegistry registry = new MetricRegistry();


    private static final int ENTITIES_TO_DELETE = 1000;
    private static final int ENTITIES_TO_ADD_AFTER_TIME = 3;


    @Before
    public void startReporting() {

        if (logger.isDebugEnabled()) {
            logger.debug("Starting metrics reporting");
        }
    }


    @After
    public void printReport() {
        logger.debug( "Printing metrics report" );
    }


    @Test( timeout = 240000 )
    public void clearOneCollection() throws Exception {

        logger.info( "Started clearOneCollection()" );

        String rand = RandomStringUtils.randomAlphanumeric( 5 );
        final UUID appId = setup.createApplication( "org_" + rand, "app_" + rand );

        final EntityManager em = setup.getEmf().getEntityManager( appId );

        final CollectionDeleteService collectionDeleteService = setup.getInjector().getInstance( CollectionDeleteService.class );

        // ----------------- create a bunch of entities

        Map<String, Object> entityMap = new HashMap<String, Object>() {{
            put( "key1", 1000 );
            put( "key2", 2000 );
            put( "key3", "Some value" );
        }};

        String collectionName = "items";
        String itemType = "item";


        List<EntityRef> entityRefs = new ArrayList<EntityRef>();
        for ( int i = 0; i < ENTITIES_TO_DELETE; i++ ) {

            final Entity entity;

            try {
                entityMap.put( "key", i );
                entity = em.create(itemType, entityMap);
            }
            catch ( Exception ex ) {
                throw new RuntimeException( "Error creating entity", ex );
            }

            entityRefs.add( new SimpleEntityRef( entity.getType(), entity.getUuid() ) );
            if ( i % 10 == 0 ) {
                logger.info( "Created {} entities", i );
            }
        }

        logger.info("Created {} entities", ENTITIES_TO_DELETE);

        app.waitForQueueDrainAndRefreshIndex(10000);

        long timeFirstPutDone = System.currentTimeMillis();
        logger.info("timeFirstPutDone={}", timeFirstPutDone);

        for (int i = 0; i < ENTITIES_TO_ADD_AFTER_TIME; i++) {

            final Entity entity;

            try {
                entityMap.put( "key", ENTITIES_TO_DELETE + i );
                entity = em.create(itemType, entityMap);
            }
            catch ( Exception ex ) {
                throw new RuntimeException( "Error creating entity", ex );
            }

            entityRefs.add( new SimpleEntityRef( entity.getType(), entity.getUuid() ) );
            logger.info( "Created {} entities after delete time created time {} ", i , entity.getCreated());

        }
        logger.info("Created {} entities after delete time", ENTITIES_TO_ADD_AFTER_TIME);


        app.waitForQueueDrainAndRefreshIndex(15000);

        final CollectionDeleteRequestBuilder builder =
            collectionDeleteService.getBuilder()
                .withApplicationId( em.getApplicationId() )
                .withCollection(collectionName)
                .withEndTimestamp(timeFirstPutDone);

        CollectionDeleteService.CollectionDeleteStatus status = collectionDeleteService.deleteCollection(builder);

        assertNotNull( status.getJobId(), "JobId is present" );

        logger.info( "Delete collection" );


        waitForDelete( status, collectionDeleteService );

        //app.waitForQueueDrainAndRefreshIndex(15000);

        // ----------------- test that we can read the entries after the timestamp

        retryReadData( em, collectionName, ENTITIES_TO_ADD_AFTER_TIME, 60);
    }

    /**
     * Wait for the delete to occur
     */
    private void waitForDelete( final CollectionDeleteService.CollectionDeleteStatus status, final CollectionDeleteService collectionDeleteService )
        throws InterruptedException, IllegalArgumentException {
        if (status != null) {
            logger.info("waitForDelete: jobID={}", status.getJobId());
        } else {
            logger.info("waitForDelete: error, status = null");
            throw new IllegalArgumentException("collectionDeleteStatus = null");
        }
        while ( true ) {

            try {
                final CollectionDeleteService.CollectionDeleteStatus updatedStatus =
                    collectionDeleteService.getStatus( status.getJobId() );

                if (updatedStatus == null) {
                    logger.info("waitForDelete: updated status is null");
                } else {
                    logger.info("waitForDelete: status={} numberProcessed={}",
                        updatedStatus.getStatus().toString(), updatedStatus.getNumberProcessed());

                    if ( updatedStatus.getStatus() == CollectionDeleteService.Status.COMPLETE ) {
                        break;
                    }
                }
            }
            catch ( IllegalArgumentException iae ) {
                //swallow.  Thrown if our job can't be found.  I.E hasn't updated yet
            }


            Thread.sleep( 1000 );
        }
    }

    private int retryReadData(EntityManager em, String collectionName, int expectedEntities,  int retry) throws Exception {
        int count = -1;
        do {
            try {
                count = readData(em, collectionName, expectedEntities);
            } catch (Exception ignore) {
                logger.info( "caught exception ", ignore);
            }
            logger.info( "read {} expected {}" , count, expectedEntities);
        } while (count != expectedEntities && --retry >=0);
        assertEquals( "Did not get expected entities", expectedEntities, count );
        return count;
    }

    private int readData(EntityManager em, String collectionName, int expectedEntities)
        throws Exception {

        app.waitForQueueDrainAndRefreshIndex();

        Results results = em.getCollection(em.getApplicationRef(), collectionName, null, expectedEntities,
            Query.Level.ALL_PROPERTIES, false);

        int count = 0;
        List<Entity> list = new ArrayList<>();
        while ( true ) {

            if (results.getEntities().size() == 0) {
                break;
            }

            UUID lastEntityUUID = null;
            for ( Entity e : results.getEntities() ) {

                assertEquals(2000, e.getProperty("key2"));

                if (count % 100 == 0) {
                    logger.info("read {} entities", count);
                }
                lastEntityUUID = e.getUuid();
                count++;
                list.add(e);
            }

            results = em.getCollection(em.getApplicationRef(), collectionName, lastEntityUUID, expectedEntities,
                Query.Level.ALL_PROPERTIES, false);

        }
        logger.info("read {} total entities", count);

        if (count != expectedEntities) {
            logger.info("Expected {} did not match actual {}", expectedEntities, count);
            if (count < 20) {
                for (Entity e : list) {
                    Object key = e.getProperty("key2");
                    logger.info("Entity key {} ceated {}", key, e.getCreated());
                }
            }
        }

        assertEquals( "Did not get expected entities", expectedEntities, count );
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
