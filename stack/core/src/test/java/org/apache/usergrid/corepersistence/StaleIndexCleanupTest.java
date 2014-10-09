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

import com.fasterxml.uuid.UUIDComparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test on read style clean-up of stale ElasticSearch indexes.
 */
public class StaleIndexCleanupTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger(StaleIndexCleanupTest.class );

    // take it easy on embedded Cassandra
    private static final long writeDelayMs = 50;
    private static final long readDelayMs = 50;


    /**
     * Test that updating an entity causes the entity's version number to change.
     */
    @Test
    public void testUpdateVersioning() throws Exception {

        final EntityManager em = app.getEntityManager();

        Entity thing = em.create("thing", new HashMap<String, Object>() {{
            put("name", "thing1");
        }});
        em.refreshIndex();
        
        assertEquals( 1, queryCollectionCp("things", "select *").size() );

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = getCpEntity(thing);
        UUID oldVersion = cpEntity.getVersion();

        em.updateProperties(thing, new HashMap<String, Object>() {{
            put("stuff", "widget");
        }});
        em.refreshIndex();

        org.apache.usergrid.persistence.model.entity.Entity cpUpdated = getCpEntity(thing);
        assertEquals( "widget", cpUpdated.getField("stuff").getValue());
        UUID newVersion = cpUpdated.getVersion();

        assertTrue( "New version is greater than old", 
                UUIDComparator.staticCompare( newVersion, oldVersion ) > 0 );

        assertEquals( 2, queryCollectionCp("things", "select *").size() );
    }


    /**
     * Test that the CpRelationManager cleans up and stale indexes that it finds when it is 
     * building search results.
     */
    @Test
    public void testStaleIndexCleanup() throws Exception {

        logger.info("Started testStaleIndexCleanup()");

        // TODO: turn off post processing stuff that cleans up stale entities 

        final EntityManager em = app.getEntityManager();

        final int numEntities = 100;
        final int numUpdates = 5;

        // create lots of entities
        final List<Entity> things = new ArrayList<Entity>();
        for ( int i=0; i<numEntities; i++) {
            final String thingName = "thing" + i;
            things.add( em.create("thing", new HashMap<String, Object>() {{
                put("name", thingName);
            }}));
            Thread.sleep( writeDelayMs );
        }
        em.refreshIndex();

        CandidateResults crs = queryCollectionCp( "things", "select *");
        Assert.assertEquals( "Expect no stale candidates yet", numEntities, crs.size() );

        // update each one a bunch of times
        int count = 0;
        for ( Entity thing : things ) {

            for ( int j=0; j<numUpdates; j++) {

                Entity toUpdate = em.get( thing.getUuid() );
                thing.setProperty( "property"  + j, RandomStringUtils.randomAlphanumeric(10));
                em.update(toUpdate);

                Thread.sleep( writeDelayMs );

                count++;
                if ( count % 100 == 0 ) {
                    logger.info("Updated {} of {} times", count, numEntities * numUpdates);
                }
            }
        }
        em.refreshIndex();

        // query Core Persistence directly for total number of result candidates
        crs = queryCollectionCp("things", "select *");
        Assert.assertEquals( "Expect stale candidates", numEntities * (numUpdates + 1), crs.size());

        // query EntityManager for results and page through them
        // should return numEntities becuase it filters out the stale entities
        Query q = Query.fromQL("select *");
        q.setLimit( 8 ); 
        int thingCount = 0;
        String cursor = null;
        do {
            Results results = em.searchCollection( em.getApplicationRef(), "things", q);
            cursor = results.getCursor();
            if ( cursor != null ) {
                assertEquals( 8, results.size() );
            }
            thingCount += results.size();

        } while ( cursor != null );
        assertEquals( "Expect no stale candidates", numEntities, thingCount );

        // EntityManager should have kicked off a batch cleanup of those stale indexes
        // wait a second for batch cleanup to complete
        Thread.sleep(600);

        // query for total number of result candidates = numEntities
        crs = queryCollectionCp("things", "select *");
        Assert.assertEquals( "Expect stale candidates de-indexed", numEntities, crs.size());
    }


    /** 
     * Go around EntityManager and get directly from Core Persistence.
     */
    private org.apache.usergrid.persistence.model.entity.Entity getCpEntity( EntityRef eref ) {

        EntityManager em = app.getEntityManager();

        CollectionScope cs = new CollectionScopeImpl(
            new SimpleId( em.getApplicationId(), TYPE_APPLICATION),
            new SimpleId( em.getApplicationId(), TYPE_APPLICATION),
            CpNamingUtils.getCollectionScopeNameFromEntityType( eref.getType() ));

        EntityCollectionManagerFactory ecmf = 
                CpSetup.getInjector().getInstance( EntityCollectionManagerFactory.class );

        EntityCollectionManager ecm = ecmf.createCollectionManager(cs);

        return ecm.load( new SimpleId( eref.getUuid(), eref.getType()))
                .toBlocking().lastOrDefault(null);
    } 


    /** 
     * Go around EntityManager and execute query directly against Core Persistence.
     * Results may include stale index entries.
     */
    private CandidateResults queryCollectionCp( String collName, String query ) {

        EntityManager em = app.getEntityManager();

        EntityIndexFactory eif = CpSetup.getInjector().getInstance( EntityIndexFactory.class );

        ApplicationScope as = new ApplicationScopeImpl( 
            new SimpleId( em.getApplicationId(), TYPE_APPLICATION));
        EntityIndex ei = eif.createEntityIndex( as );

        IndexScope is = new IndexScopeImpl(
            new SimpleId( em.getApplicationId(), TYPE_APPLICATION),
            CpNamingUtils.getCollectionScopeNameFromCollectionName( collName ));
        Query rcq = Query.fromQL(query);
        rcq.setLimit(10000); // no paging

        return ei.search( is, rcq );
    }
}
