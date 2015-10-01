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


//@RunWith(JukitoRunner.class)
//@UseModules({ GuiceModule.class })


public class RebuildIndexTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger( RebuildIndexTest.class );

    private static final MetricRegistry registry = new MetricRegistry();


    private static final int ENTITIES_TO_INDEX = 1000;


    @Before
    public void startReporting() {

        logger.debug( "Starting metrics reporting" );
    }


    @After
    public void printReport() {
        logger.debug( "Printing metrics report" );
    }


    @Test( timeout = 120000 )
    public void rebuildOneCollectionIndex() throws Exception {

        logger.info( "Started rebuildIndex()" );

        String rand = RandomStringUtils.randomAlphanumeric( 5 );
        final UUID appId = setup.createApplication( "org_" + rand, "app_" + rand );

        final EntityManager em = setup.getEmf().getEntityManager( appId );

        final ReIndexService reIndexService = setup.getInjector().getInstance( ReIndexService.class );

        // ----------------- create a bunch of entities

        Map<String, Object> entityMap = new HashMap<String, Object>() {{
            put( "key1", 1000 );
            put( "key2", 2000 );
            put( "key3", "Some value" );
        }};


        List<EntityRef> entityRefs = new ArrayList<EntityRef>();
        int herderCount = 0;
        int shepardCount = 0;
        for ( int i = 0; i < ENTITIES_TO_INDEX; i++ ) {

            final Entity entity;

            try {
                entityMap.put( "key", i );

                if ( i % 2 == 0 ) {
                    entity = em.create( "catherder", entityMap );
                    herderCount++;
                }
                else {
                    entity = em.create( "catshepard", entityMap );
                    shepardCount++;
                }
            }
            catch ( Exception ex ) {
                throw new RuntimeException( "Error creating entity", ex );
            }

            entityRefs.add( new SimpleEntityRef( entity.getType(), entity.getUuid() ) );
            if ( i % 10 == 0 ) {
                logger.info( "Created {} entities", i );
            }
        }

        logger.info( "Created {} entities", ENTITIES_TO_INDEX );
        app.refreshIndex();

        // ----------------- test that we can read them, should work fine

        logger.debug( "Read the data" );
        readData( em, "catherders", herderCount, 0 );
        readData( em, "catshepards", shepardCount, 0 );

        // ----------------- delete the system and application indexes

        logger.debug( "Deleting apps" );
        deleteIndex( em.getApplicationId() );

        // ----------------- test that we can read them, should fail

        logger.debug( "Reading data, should fail this time " );

        //should be no data
        readData( em, "testTypes", 0, 0 );


        //        ----------------- rebuild index for catherders only

        logger.debug( "Preparing to rebuild all indexes" );


        final ReIndexRequestBuilder builder =
            reIndexService.getBuilder().withApplicationId( em.getApplicationId() ).withCollection( "catherders" );

        ReIndexService.ReIndexStatus status = reIndexService.rebuildIndex( builder );

        assertNotNull( status.getJobId(), "JobId is present" );

        logger.info( "Rebuilt index" );


        waitForRebuild( status, reIndexService );


        // ----------------- test that we can read the catherder collection and not the catshepard

        readData( em, "catherders", herderCount, 0 );
        readData( em, "catshepards", 0, 0 );
    }


    @Test( timeout = 120000 )
    public void rebuildIndex() throws Exception {

        logger.info( "Started rebuildIndex()" );

        String rand = RandomStringUtils.randomAlphanumeric( 5 );
        final UUID appId = setup.createApplication( "org_" + rand, "app_" + rand );

        final EntityManager em = setup.getEmf().getEntityManager( appId );

        final ReIndexService reIndexService = setup.getInjector().getInstance( ReIndexService.class );

        // ----------------- create a bunch of entities

        Map<String, Object> entityMap = new HashMap<String, Object>() {{
            put( "key1", 1000 );
            put( "key2", 2000 );
            put( "key3", "Some value" );
        }};
        Map<String, Object> cat1map = new HashMap<String, Object>() {{
            put( "name", "enzo" );
            put( "color", "orange" );
        }};
        Map<String, Object> cat2map = new HashMap<String, Object>() {{
            put( "name", "marquee" );
            put( "color", "grey" );
        }};
        Map<String, Object> cat3map = new HashMap<String, Object>() {{
            put( "name", "bertha" );
            put( "color", "tabby" );
        }};

        Entity cat1 = em.create( "cat", cat1map );
        Entity cat2 = em.create( "cat", cat2map );
        Entity cat3 = em.create( "cat", cat3map );

        List<EntityRef> entityRefs = new ArrayList<>();

        for ( int i = 0; i < ENTITIES_TO_INDEX; i++ ) {

            final Entity entity;

            try {
                entityMap.put( "key", i );
                entity = em.create( "testType", entityMap );


                em.createConnection( entity, "herds", cat1 );
                em.createConnection( entity, "herds", cat2 );
                em.createConnection( entity, "herds", cat3 );
            }
            catch ( Exception ex ) {
                throw new RuntimeException( "Error creating entity", ex );
            }

            entityRefs.add( new SimpleEntityRef( entity.getType(), entity.getUuid() ) );
            if ( i % 10 == 0 ) {
                logger.info( "Created {} entities", i );
            }
        }

        logger.info( "Created {} entities", ENTITIES_TO_INDEX );
        app.refreshIndex();

        // ----------------- test that we can read them, should work fine

        logger.debug( "Read the data" );
        final String collectionName = "testtypes";
        readData( em, collectionName, ENTITIES_TO_INDEX, 3 );

        // ----------------- delete the system and application indexes

        logger.debug( "Deleting app index" );

        deleteIndex( em.getApplicationId() );

        // ----------------- test that we can read them, should fail

        // deleting sytem app index will interfere with other concurrently running tests
        //deleteIndex( CpNamingUtils.SYSTEM_APP_ID );

        // ----------------- test that we can read them, should fail

        logger.debug( "Reading data, should fail this time " );

        readData( em, collectionName, 0, 0 );



        // ----------------- rebuild index

        logger.debug( "Preparing to rebuild all indexes" );
        ;


        try {

            final ReIndexRequestBuilder builder =
                reIndexService.getBuilder().withApplicationId( em.getApplicationId() );

            ReIndexService.ReIndexStatus status = reIndexService.rebuildIndex( builder );

            assertNotNull( status.getJobId(), "JobId is present" );

            logger.info( "Rebuilt index" );


            waitForRebuild( status, reIndexService );


            logger.info( "Rebuilt index" );

            app.refreshIndex();
        }
        catch ( Exception ex ) {
            logger.error( "Error rebuilding index", ex );
            fail();
        }

        // ----------------- test that we can read them

        Thread.sleep( 2000 );
        readData( em, collectionName, ENTITIES_TO_INDEX, 3 );
    }



    @Test( timeout = 120000 )
    public void rebuildUpdatedSince() throws Exception {

        logger.info( "Started rebuildIndex()" );

        String rand = RandomStringUtils.randomAlphanumeric( 5 );
        final UUID appId = setup.createApplication( "org_" + rand, "app_" + rand );

        final EntityManager em = setup.getEmf().getEntityManager( appId );

        final ReIndexService reIndexService = setup.getInjector().getInstance( ReIndexService.class );

        // ----------------- create a bunch of entities


        Map<String, Object> entityData = new HashMap<String, Object>() {{
            put( "key1", 1000 );
        }};


        final Entity firstEntity = em.create( "thing", entityData );


        final Entity secondEntity = em.create( "thing",  entityData);

        app.refreshIndex();

        // ----------------- test that we can read them, should work fine

        logger.debug( "Read the data" );
        final String collectionName = "things";

        countEntities( em, collectionName, 2 );

        // ----------------- delete the system and application indexes

        logger.debug( "Deleting app index" );

        deleteIndex( em.getApplicationId() );

        // ----------------- test that we can read them, should fail

        // deleting sytem app index will interfere with other concurrently running tests
        //deleteIndex( CpNamingUtils.SYSTEM_APP_ID );

        // ----------------- test that we can read them, should fail

        logger.debug( "Reading data, should fail this time " );

        countEntities( em, collectionName, 0);



        // ----------------- rebuild index

        final long firstUpdatedTimestamp = firstEntity.getModified();
        final long secondUpdatedTimestamp = secondEntity.getModified();

        assertTrue( "second should be updated after second", firstUpdatedTimestamp < secondUpdatedTimestamp );


        try {


            final long updatedTimestamp = secondEntity.getModified();


            logger.debug( "Preparing to rebuild all indexes with timestamp {}", updatedTimestamp );

            //set our update timestamp
            final ReIndexRequestBuilder builder =
                reIndexService.getBuilder().withApplicationId( em.getApplicationId() ).withStartTimestamp(
                    updatedTimestamp );

            ReIndexService.ReIndexStatus status = reIndexService.rebuildIndex( builder );

            assertNotNull( status.getJobId(), "JobId is present" );

            logger.info( "Rebuilt index" );

            waitForRebuild( status, reIndexService );

            logger.info( "Rebuilt index" );

            app.refreshIndex();
        }
        catch ( Exception ex ) {
            logger.error( "Error rebuilding index", ex );
            fail();
        }

        // ----------------- test that we can read them

        Thread.sleep( 2000 );
        countEntities( em, collectionName, 1 );
    }


    /**
     * Wait for the rebuild to occur
     */
    private void waitForRebuild( final ReIndexService.ReIndexStatus status, final ReIndexService reIndexService )
        throws InterruptedException {
        while ( true ) {

            try {
                final ReIndexService.ReIndexStatus updatedStatus = reIndexService.getStatus( status.getJobId() );

                if ( updatedStatus.getStatus() == ReIndexService.Status.COMPLETE ) {
                    break;
                }
            }
            catch ( IllegalArgumentException iae ) {
                //swallow.  Thrown if our job can't be found.  I.E hasn't updated yet
            }


            Thread.sleep( 1000 );
        }
    }


    /**
     * Delete app index
     */
    private void deleteIndex( UUID appUuid ) {

        Injector injector = SpringResource.getInstance().getBean( Injector.class );
        IndexLocationStrategyFactory indexLocationStrategyFactory = injector.getInstance(IndexLocationStrategyFactory.class);
        EntityIndexFactory eif = injector.getInstance( EntityIndexFactory.class );

        Id appId = new SimpleId( appUuid, Schema.TYPE_APPLICATION );
        ApplicationScope scope = new ApplicationScopeImpl( appId );
        EntityIndex ei = eif.createEntityIndex(
            indexLocationStrategyFactory.getIndexLocationStrategy(scope)
        );

        ei.deleteApplication().toBlocking().lastOrDefault( null );
        app.refreshIndex();
    }


    private int readData( EntityManager em, String collectionName, int expectedEntities, int expectedConnections )
        throws Exception {

        app.refreshIndex();

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

        assertEquals( "Did not get expected entities", expectedEntities, count );
        return count;
    }

    private int countEntities( EntityManager em, String collectionName, int expectedEntities)
           throws Exception {

           app.refreshIndex();

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
