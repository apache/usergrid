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
package org.apache.usergrid.persistence.index.impl;


import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.exceptions.QueryParseException;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
public class EntityConnectionIndexImplTest extends BaseIT {

    private static final Logger log = LoggerFactory.getLogger( EntityConnectionIndexImplTest.class );

    //    @ClassRule
    //    public static ElasticSearchResource es = new ElasticSearchResource();


    @Inject
    public EntityIndexFactory ecif;


    @Test
    public void testBasicOperation() throws IOException, InterruptedException {

        Id appId = new SimpleId( "application" );
        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        // create a muffin
        Entity muffin = new Entity( new SimpleId( UUIDGenerator.newTimeUUID(), "muffin" ) );

        muffin = EntityIndexMapUtils.fromMap( muffin, new HashMap<String, Object>() {{
            put( "size", "Large" );
            put( "flavor", "Blueberry" );
            put( "stars", 5 );
        }} );
        EntityUtils.setVersion( muffin, UUIDGenerator.newTimeUUID() );

        Entity egg = new Entity( new SimpleId( UUIDGenerator.newTimeUUID(), "egg" ) );

        egg = EntityIndexMapUtils.fromMap( egg, new HashMap<String, Object>() {{
            put( "size", "Large" );
            put( "type", "scramble" );
            put( "stars", 5 );
        }} );
        EntityUtils.setVersion( egg, UUIDGenerator.newTimeUUID() );

        Entity oj = new Entity( new SimpleId( UUIDGenerator.newTimeUUID(), "juice" ) );

        oj = EntityIndexMapUtils.fromMap( oj, new HashMap<String, Object>() {{
            put( "size", "Large" );
            put( "type", "pulpy" );
            put( "stars", 3 );
        }} );
        EntityUtils.setVersion( oj, UUIDGenerator.newTimeUUID() );


        // create a person who likes muffins
        Id personId = new SimpleId( UUIDGenerator.newTimeUUID(), "person" );


        assertNotNull( personId );
        assertNotNull( personId.getType() );
        assertNotNull( personId.getUuid() );

        // index connection of "person Dave likes Large Blueberry muffin"

        IndexScope searchScope = new IndexScopeImpl( personId, "likes" );

        //create another scope we index in, want to be sure these scopes are filtered
        IndexScope otherIndexScope =
                new IndexScopeImpl( new SimpleId( UUIDGenerator.newTimeUUID(), "animal" ), "likes" );

        EntityIndex personLikesIndex = ecif.createEntityIndex( applicationScope );
        personLikesIndex.initializeIndex();

        EntityIndexBatch batch = personLikesIndex.createBatch();

        //add to both scopes

        //add a muffin
        batch.index( searchScope, muffin );
        batch.index( otherIndexScope, muffin );

        //add the eggs
        batch.index( searchScope, egg );
        batch.index( otherIndexScope, egg );

        //add the oj
        batch.index( searchScope, oj );
        batch.index( otherIndexScope, oj );

        batch.execute().get();
        personLikesIndex.refresh();


        EsTestUtils.waitForTasks(personLikesIndex);
        Thread.sleep( 1000 );

        // now, let's search for muffins
        CandidateResults likes = personLikesIndex
                .search( searchScope, SearchTypes.fromTypes( muffin.getId().getType() ), Query.fromQL( "select *" ) );
        assertEquals( 1, likes.size() );
        assertEquals( muffin.getId(), likes.get( 0 ).getId() );

        // now, let's search for egg
        likes = personLikesIndex
                .search( searchScope, SearchTypes.fromTypes( egg.getId().getType() ), Query.fromQL( "select *" ) );
        assertEquals( 1, likes.size() );
        assertEquals( egg.getId(), likes.get( 0 ).getId() );

        // search for OJ
        likes = personLikesIndex
                .search( searchScope, SearchTypes.fromTypes( oj.getId().getType() ), Query.fromQL( "select *" ) );
        assertEquals( 1, likes.size() );
        assertEquals( oj.getId(), likes.get( 0 ).getId() );


        //now lets search for all explicitly
        likes = personLikesIndex.search( searchScope,
                SearchTypes.fromTypes( muffin.getId().getType(), egg.getId().getType(), oj.getId().getType() ),
                Query.fromQL( "select *" ) );
        assertEquals( 3, likes.size() );
        assertContains( egg.getId(), likes );
        assertContains( muffin.getId(), likes );
        assertContains( oj.getId(), likes );

        //now lets search for all explicitly
        likes = personLikesIndex.search( searchScope, SearchTypes.allTypes(), Query.fromQL( "select *" ) );
        assertEquals( 3, likes.size() );
        assertContains( egg.getId(), likes );
        assertContains( muffin.getId(), likes );
        assertContains( oj.getId(), likes );


        //now search all entity types with a query that returns a subset
        likes = personLikesIndex.search( searchScope,
                SearchTypes.fromTypes( muffin.getId().getType(), egg.getId().getType(), oj.getId().getType() ),
                Query.fromQL( "select * where stars = 5" ) );
        assertEquals( 2, likes.size() );
        assertContains( egg.getId(), likes );
        assertContains( muffin.getId(), likes );


        //now search with no types, we should get only the results that match
        likes = personLikesIndex
                .search( searchScope, SearchTypes.allTypes(), Query.fromQL( "select * where stars = 5" ) );
        assertEquals( 2, likes.size() );
        assertContains( egg.getId(), likes );
        assertContains( muffin.getId(), likes );
    }


    @Test
    public void testDelete() throws IOException, InterruptedException {

        Id appId = new SimpleId( "application" );
        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        // create a muffin
        Entity muffin = new Entity( new SimpleId( UUIDGenerator.newTimeUUID(), "muffin" ) );

        muffin = EntityIndexMapUtils.fromMap( muffin, new HashMap<String, Object>() {{
            put( "size", "Large" );
            put( "flavor", "Blueberry" );
            put( "stars", 5 );
        }} );
        EntityUtils.setVersion( muffin, UUIDGenerator.newTimeUUID() );

        Entity egg = new Entity( new SimpleId( UUIDGenerator.newTimeUUID(), "egg" ) );

        egg = EntityIndexMapUtils.fromMap( egg, new HashMap<String, Object>() {{
            put( "size", "Large" );
            put( "type", "scramble" );
            put( "stars", 5 );
        }} );
        EntityUtils.setVersion( egg, UUIDGenerator.newTimeUUID() );

        Entity oj = new Entity( new SimpleId( UUIDGenerator.newTimeUUID(), "juice" ) );

        oj = EntityIndexMapUtils.fromMap( oj, new HashMap<String, Object>() {{
            put( "size", "Large" );
            put( "type", "pulpy" );
            put( "stars", 3 );
        }} );
        EntityUtils.setVersion( oj, UUIDGenerator.newTimeUUID() );


        // create a person who likes muffins
        Id personId = new SimpleId( UUIDGenerator.newTimeUUID(), "person" );


        assertNotNull( personId );
        assertNotNull( personId.getType() );
        assertNotNull( personId.getUuid() );

        // index connection of "person Dave likes Large Blueberry muffin"

        IndexScope searchScope = new IndexScopeImpl( personId, "likes" );

        //create another scope we index in, want to be sure these scopes are filtered
        IndexScope otherIndexScope =
                new IndexScopeImpl( new SimpleId( UUIDGenerator.newTimeUUID(), "animal" ), "likes" );

        EntityIndex personLikesIndex = ecif.createEntityIndex( applicationScope );
        personLikesIndex.initializeIndex();

        EntityIndexBatch batch = personLikesIndex.createBatch();

        //add to both scopes

        //add a muffin
        batch.index( searchScope, muffin );
        batch.index( otherIndexScope, muffin );

        //add the eggs
        batch.index( searchScope, egg );
        batch.index( otherIndexScope, egg );

        //add the oj
        batch.index( searchScope, oj );
        batch.index( otherIndexScope, oj );

        batch.execute().get();
        personLikesIndex.refresh();

        EsTestUtils.waitForTasks( personLikesIndex );
        Thread.sleep( 1000 );

        // now, let's search for muffins
        CandidateResults likes = personLikesIndex.search( searchScope,
                SearchTypes.fromTypes( muffin.getId().getType(), egg.getId().getType(), oj.getId().getType() ),
                Query.fromQL( "select *" ) );
        assertEquals( 3, likes.size() );
        assertContains( egg.getId(), likes );
        assertContains( muffin.getId(), likes );
        assertContains( oj.getId(), likes );


        //now delete them
        batch.deindex( searchScope, egg );
        batch.deindex( searchScope, muffin );
        batch.deindex( searchScope, oj );
        batch.execute().get();
        personLikesIndex.refresh();

        likes = personLikesIndex.search( searchScope,
                SearchTypes.fromTypes( muffin.getId().getType(), egg.getId().getType(), oj.getId().getType() ),
                Query.fromQL( "select *" ) );
        assertEquals( 0, likes.size() );
    }


    private void assertContains( final Id id, final CandidateResults results ) {
        for ( CandidateResult result : results ) {
            if ( result.getId().equals( id ) ) {
                return;
            }
        }

        fail( String.format( "Could not find id %s in candidate results", id ) );
    }
}
