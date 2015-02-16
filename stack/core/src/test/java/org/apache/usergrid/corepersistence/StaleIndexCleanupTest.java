/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.corepersistence;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Injector;

import net.jcip.annotations.NotThreadSafe;

import static org.apache.usergrid.corepersistence.CoreModule.EVENTS_DISABLED;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getCollectionScopeNameFromEntityType;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test on read style clean-up of stale ElasticSearch indexes.
 */
@NotThreadSafe
public class StaleIndexCleanupTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger( StaleIndexCleanupTest.class );

    // take it easy on Cassandra
    private static final long writeDelayMs = 0;

    Lock sequential = new ReentrantLock();

    @Before
    public void before() {
        // if tests run in parallel there will likely be a conflict over the allow.stale.entities
        sequential.lock();
    }

    @After
    public void after() {
        System.clearProperty( EVENTS_DISABLED );
    }

    /**
     * Test that updating an entity causes the entity's version number to change.
     */
    @Test
    public void testUpdateVersioning() throws Exception {

        // turn off post processing stuff that cleans up stale entities
        System.setProperty( EVENTS_DISABLED, "true" );

        final EntityManager em = app.getEntityManager();

        Entity thing = em.create( "thing", new HashMap<String, Object>() {{
            put( "name", "thing1" );
        }} );
        em.refreshIndex();

        assertEquals( 1, queryCollectionCp( "things", "thing", "select *" ).size() );

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = getCpEntity( thing );
        UUID oldVersion = cpEntity.getVersion();

        em.updateProperties( thing, new HashMap<String, Object>() {{
            put( "stuff", "widget" );
        }} );
        em.refreshIndex();

        org.apache.usergrid.persistence.model.entity.Entity cpUpdated = getCpEntity( thing );
        assertEquals( "widget", cpUpdated.getField( "stuff" ).getValue() );
        UUID newVersion = cpUpdated.getVersion();

        assertTrue( "New version is greater than old",
                UUIDComparator.staticCompare( newVersion, oldVersion ) > 0 );

        assertEquals( 2, queryCollectionCp( "things", "thing", "select *" ).size() );
    }


    /**
     * Test that the CpRelationManager cleans up and stale indexes that it finds when
     * it is building search results.
     */
    @Test
    @Ignore("Broken until search connections is fixed")
    public void testStaleIndexCleanup() throws Exception {

        logger.info( "Started testStaleIndexCleanup()" );

        // turn off post processing stuff that cleans up stale entities
        System.setProperty( EVENTS_DISABLED, "true" );

        final EntityManager em = app.getEntityManager();

        final int numEntities = 20;
        final int numUpdates = 40;

        final AtomicInteger updateCount =  new AtomicInteger(  );

        // create lots of entities
        final List<Entity> things = new ArrayList<Entity>( numEntities );
        for ( int i = 0; i < numEntities; i++ ) {
            final String thingName = "thing" + i;
            things.add( em.create( "thing", new HashMap<String, Object>() {{
                put( "name", thingName );
                put( "updateCount", updateCount.getAndIncrement() );
            }} ) );

        }
        em.refreshIndex();

        CandidateResults crs = queryCollectionCp( "things", "thing", "select * order by updateCount asc" );
        Assert.assertEquals( "Expect no stale candidates yet", numEntities, crs.size() );

        // update each one a bunch of times
        int count = 0;

        List<Entity> maxVersions = new ArrayList<>( numEntities );

        for ( Entity thing : things ) {

            Entity toUpdate = null;

            for ( int j = 0; j < numUpdates; j++ ) {

                toUpdate = em.get( thing.getUuid() );
                //update the update count, so we'll order from the first entity created to the last
                toUpdate.setProperty( "updateCount", updateCount.getAndIncrement() );
                em.update( toUpdate );

                count++;
                if ( count % 100 == 0 ) {
                    logger.info( "Updated {} of {} times", count, numEntities * numUpdates );
                }
            }

            maxVersions.add( toUpdate );
        }

        em.refreshIndex();

        // query Core Persistence directly for total number of result candidates
        crs = queryCollectionCp( "things", "thing", "select * order by updateCount asc" );
        Assert.assertEquals( "Expect stale candidates", numEntities * ( numUpdates + 1 ), crs.size() );

        // query EntityManager for results and page through them
        // should return numEntities because it filters out the stale entities
        final int limit = 8;

        // we order by updateCount asc, this forces old versions to appear first, otherwise,
        // we don't clean them up in our versions
        Query q = Query.fromQL( "select * order by updateCount asc" );
        q.setLimit( limit );

        int thingCount = 0;
        int index = 0;
        String cursor;

        do {
            Results results = em.searchCollection( em.getApplicationRef(), "things", q );
            thingCount += results.size();

            logger.debug( "Retrieved total of {} entities", thingCount );

            cursor = results.getCursor();
            if ( cursor != null && thingCount < numEntities ) {
                assertEquals( limit, results.size() );
            }

            for ( int i = 0; i < results.size(); i++, index++ ) {

                final Entity returned = results.getEntities().get( i );

                // last entities appear first
                final Entity expected = maxVersions.get( index );
                assertEquals("correct entity returned", expected, returned);

            }
        }
        while ( cursor != null );

        assertEquals( "Expect no stale candidates", numEntities, thingCount );


        em.refreshIndex();


        // query for total number of result candidates = numEntities
        crs = queryCollectionCp( "things", "thing", "select *" );
        Assert.assertEquals( "Expect stale candidates de-indexed", numEntities, crs.size() );
    }


    /**
     * Test that the EntityDeleteImpl cleans up stale indexes on delete. Ensures that when an
     * entity is deleted its old indexes are cleared from ElasticSearch.
     */
    @Test(timeout=30000)
    public void testCleanupOnDelete() throws Exception {

        logger.info("Started testStaleIndexCleanup()");

        // turn off post processing stuff that cleans up stale entities
        System.setProperty( EVENTS_DISABLED, "true" );

        final EntityManager em = app.getEntityManager();

        final int numEntities = 20;
        final int numUpdates = 40;

        // create lots of entities
        final List<Entity> things = new ArrayList<Entity>(numEntities);
        for ( int i=0; i<numEntities; i++) {
            final String thingName = "thing" + i;
            things.add( em.create("thing", new HashMap<String, Object>() {{
                put("name", thingName);
            }}));
            Thread.sleep( writeDelayMs );
        }
        em.refreshIndex();

        CandidateResults crs = queryCollectionCp( "things", "thing", "select *");
        Assert.assertEquals( "Expect no stale candidates yet", numEntities, crs.size() );

        // update each one a bunch of times
        int count = 0;

        List<Entity> maxVersions = new ArrayList<>(numEntities);

        for ( Entity thing : things ) {
            Entity toUpdate = null;

            for ( int j=0; j<numUpdates; j++) {
                toUpdate = em.get( thing.getUuid() );
                toUpdate.setProperty( "property"  + j, RandomStringUtils.randomAlphanumeric(10));

                em.update(toUpdate);

                Thread.sleep( writeDelayMs );
                count++;
                if ( count % 100 == 0 ) {
                    logger.info("Updated {} of {} times", count, numEntities * numUpdates);
                }
            }

            maxVersions.add( toUpdate );
        }
        em.refreshIndex();

        // query Core Persistence directly for total number of result candidates
        crs = queryCollectionCp("things", "thing", "select *");
        Assert.assertEquals( "Expect stale candidates", numEntities * (numUpdates + 1), crs.size());

        // turn ON post processing stuff that cleans up stale entities
        System.setProperty( EVENTS_DISABLED, "false" );

        // delete all entities
        for ( Entity thing : things ) {
            em.delete( thing );
        }

        // wait for indexes to be cleared for the deleted entities
        count = 0;
        do {
            Thread.sleep(100);
            crs = queryCollectionCp("things", "thing", "select *");
            em.refreshIndex();
        } while ( crs.size() > 0 && count++ < 15 );

        Assert.assertEquals( "Expect no candidates", 0, crs.size() );
    }


    /**
     * Test that the EntityDeleteImpl cleans up stale indexes on update. Ensures that when an
     * entity is updated its old indexes are cleared from ElasticSearch.
     */
    @Test(timeout=30000)
    public void testCleanupOnUpdate() throws Exception {

        logger.info( "Started testCleanupOnUpdate()" );

        // turn off post processing stuff that cleans up stale entities
        System.setProperty( EVENTS_DISABLED, "true" );

        final EntityManager em = app.getEntityManager();

        final int numEntities = 20;
        final int numUpdates = 40;

        // create lots of entities
        final List<Entity> things = new ArrayList<Entity>(numEntities);
        for ( int i=0; i<numEntities; i++) {
            final String thingName = "thing" + i;
            things.add( em.create("thing", new HashMap<String, Object>() {{
                put("name", thingName);
            }}));
            Thread.sleep( writeDelayMs );
        }
        em.refreshIndex();

        CandidateResults crs = queryCollectionCp( "things", "thing", "select *");
        Assert.assertEquals( "Expect no stale candidates yet", numEntities, crs.size() );

        // turn off post processing stuff that cleans up stale entities
        System.setProperty( EVENTS_DISABLED, "false" );

        // update each entity a bunch of times

        List<Entity> maxVersions = new ArrayList<>(numEntities);
        int count = 0;
        for ( Entity thing : things ) {
            Entity toUpdate = null;

            for ( int j=0; j<numUpdates; j++) {
                toUpdate = em.get( thing.getUuid() );
                toUpdate.setProperty( "property"  + j, RandomStringUtils.randomAlphanumeric(10));

                em.update(toUpdate);

                Thread.sleep( writeDelayMs );
                count++;
                if ( count % 100 == 0 ) {
                    logger.info("Updated {} of {} times", count, numEntities * numUpdates);
                }
            }

            maxVersions.add( toUpdate );
        }
        em.refreshIndex();

        // wait for indexes to be cleared for the deleted entities
        count = 0;
        do {
            Thread.sleep(100);
            crs = queryCollectionCp("things", "thing", "select *");
            em.refreshIndex();
        } while ( crs.size() > 0 && count++ < 15 );

        // query Core Persistence directly for total number of result candidates
        crs = queryCollectionCp("things", "thing", "select *");
        Assert.assertEquals( "Expect candidates without earlier stale entities", numEntities, crs.size() );
    }


    /**
    /**
     * Go around EntityManager and get directly from Core Persistence.
     */
    private org.apache.usergrid.persistence.model.entity.Entity getCpEntity( EntityRef eref ) {

        EntityManager em = app.getEntityManager();

        CollectionScope cs = getCollectionScopeNameFromEntityType(
                new SimpleId( em.getApplicationId(), TYPE_APPLICATION ), eref.getType() );

        EntityCollectionManagerFactory ecmf =
            SpringResource.getInstance().getBean( Injector.class ).getInstance( EntityCollectionManagerFactory.class );

        EntityCollectionManager ecm = ecmf.createCollectionManager( cs );

        return ecm.load( new SimpleId( eref.getUuid(), eref.getType() ) )
                .toBlocking().lastOrDefault( null );
    }


    /**
     * Go around EntityManager and execute query directly against Core Persistence.
     * Results may include stale index entries.
     */
    private CandidateResults queryCollectionCp(
            final String collName, final String type, final String query ) {

        EntityManager em = app.getEntityManager();

        EntityIndexFactory eif =  SpringResource.getInstance().getBean( Injector.class ).getInstance( EntityIndexFactory.class );

        ApplicationScope as = new ApplicationScopeImpl(
            new SimpleId( em.getApplicationId(), TYPE_APPLICATION ) );
        EntityIndex ei = eif.createEntityIndex( as );

        IndexScope is = new IndexScopeImpl( new SimpleId( em.getApplicationId(), TYPE_APPLICATION ),
                CpNamingUtils.getCollectionScopeNameFromCollectionName( collName ) );
        Query rcq = Query.fromQL( query );

        // TODO: why does this have no effect; max we ever get is 1000 entities
        rcq.setLimit( 10000 ); // no paging

        return ei.search( is, SearchTypes.fromTypes( type ), rcq );
    }
}
