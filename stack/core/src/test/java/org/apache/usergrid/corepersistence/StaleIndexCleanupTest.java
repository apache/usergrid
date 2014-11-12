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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
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

import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test on read style clean-up of stale ElasticSearch indexes.
 */
public class StaleIndexCleanupTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger( StaleIndexCleanupTest.class );



    /**
     * Test that updating an entity causes the entity's version number to change.
     */
    @Test
    public void testUpdateVersioning() throws Exception {

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

        assertTrue( "New version is greater than old", UUIDComparator.staticCompare( newVersion, oldVersion ) > 0 );

        assertEquals( 2, queryCollectionCp( "things", "thing", "select *" ).size() );
    }


    /**
     * Test that the CpRelationManager cleans up and stale indexes that it finds when it is building search results.
     */
    @Test
    public void testStaleIndexCleanup() throws Exception {

        logger.info( "Started testStaleIndexCleanup()" );

        // TODO: turn off post processing stuff that cleans up stale entities 

        final EntityManager em = app.getEntityManager();

        final int numEntities = 10;
        final int numUpdates = 3;

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
        // should return numEntities becuase it filters out the stale entities
        final int limit = 8;

        //we order by updateCount asc, this forces old versions to appear first, otherwise, we don't clean them up in
        // our versions
        Query q = Query.fromQL( "select * order by updateCount asc" );
        q.setLimit( limit );
        int thingCount = 0;
        String cursor = null;

        int index = 0;


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

                //last entities appear first
                final Entity expected = maxVersions.get( index );
                assertEquals( "correct entity returned", expected, returned );
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
     * Go around EntityManager and get directly from Core Persistence.
     */
    private org.apache.usergrid.persistence.model.entity.Entity getCpEntity( EntityRef eref ) {

        EntityManager em = app.getEntityManager();

        CollectionScope cs = new CollectionScopeImpl( new SimpleId( em.getApplicationId(), TYPE_APPLICATION ),
                new SimpleId( em.getApplicationId(), TYPE_APPLICATION ),
                CpNamingUtils.getCollectionScopeNameFromEntityType( eref.getType() ) );

        EntityCollectionManagerFactory ecmf = CpSetup.getInjector().getInstance( EntityCollectionManagerFactory.class );

        EntityCollectionManager ecm = ecmf.createCollectionManager( cs );

        return ecm.load( new SimpleId( eref.getUuid(), eref.getType() ) ).toBlocking().lastOrDefault( null );
    }


    /**
     * Go around EntityManager and execute query directly against Core Persistence. Results may include stale index
     * entries.
     */
    private CandidateResults queryCollectionCp( final String collName, final String type, final String query ) {

        EntityManager em = app.getEntityManager();

        EntityIndexFactory eif = CpSetup.getInjector().getInstance( EntityIndexFactory.class );

        ApplicationScope as = new ApplicationScopeImpl( new SimpleId( em.getApplicationId(), TYPE_APPLICATION ) );
        EntityIndex ei = eif.createEntityIndex( as );

        IndexScope is = new IndexScopeImpl( new SimpleId( em.getApplicationId(), TYPE_APPLICATION ),
                CpNamingUtils.getCollectionScopeNameFromCollectionName( collName ) );
        Query rcq = Query.fromQL( query );
        rcq.setLimit( 10000 ); // no paging

        return ei.search( is, SearchTypes.fromTypes( type ), rcq );
    }
}
