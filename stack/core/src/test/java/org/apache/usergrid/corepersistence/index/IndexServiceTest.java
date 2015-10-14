/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.index;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.corepersistence.TestIndexModule;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.CandidateResults;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.impl.EsRunner;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.index.impl.IndexOperation;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createCollectionEdge;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getApplicationScope;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
public class IndexServiceTest {

    @Inject
    public IndexService indexService;


    @Inject
    public GraphManagerFactory graphManagerFactory;

    @Inject
    public IndexProducer indexProducer;

    @Inject
    public EntityCollectionManagerFactory entityCollectionManagerFactory;

    @Inject
    public EntityIndexFactory entityIndexFactory;

    @Inject
    public IndexLocationStrategyFactory indexLocationStrategyFactory;

    @Inject
    public  IndexFig indexFig;

    public GraphManager graphManager;

    public ApplicationScope applicationScope;


    @Before
    public void setup() {
        applicationScope = getApplicationScope( UUIDGenerator.newTimeUUID() );

        graphManager = graphManagerFactory.createEdgeManager( applicationScope );
    }


    @Test
    public void testSingleIndexFromSource() {
        final Entity entity = new Entity( createId( "test" ), UUIDGenerator.newTimeUUID() );
        entity.setField( new StringField( "string", "foo" ) );

        final Edge collectionEdge = createCollectionEdge( applicationScope.getApplication(), "tests", entity.getId() );

        //write the edge
        graphManager.writeEdge( collectionEdge ).toBlocking().last();


        //index the edge
        final Observable<IndexOperationMessage> indexed = indexService.indexEntity( applicationScope, entity );


        //real users should never call to blocking, we're not sure what we'll get
        final IndexOperationMessage results = indexed.toBlocking().last();
        indexProducer.put(results).subscribe();

        final Set<IndexOperation> indexRequests = results.getIndexRequests();

        //ensure our value made it to the index request
        final IndexOperation indexRequest = indexRequests.iterator().next();

        assertNotNull( indexRequest );
    }



//    @Test( timeout = 60000 )
    @Test( )
    public void testSingleCollectionConnection() throws InterruptedException {


        ApplicationScope applicationScope =
            new ApplicationScopeImpl( new SimpleId( UUID.randomUUID(), "application" ) );


        final Entity testEntity = new Entity( createId( "thing" ), UUIDGenerator.newTimeUUID() );
        testEntity.setField( new StringField( "string", "foo" ) );


        //write the entity before indexing
        final EntityCollectionManager collectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );

        collectionManager.write( testEntity ).toBlocking().last();

        final GraphManager graphManager = graphManagerFactory.createEdgeManager( applicationScope );

        //create our collection edge
        final Edge collectionEdge =
            CpNamingUtils.createCollectionEdge( applicationScope.getApplication(), "things", testEntity.getId() );
        graphManager.writeEdge( collectionEdge ).toBlocking().last();



        final Id connectingId = createId( "connecting" );
        final Edge edge = CpNamingUtils.createConnectionEdge( connectingId, "likes", testEntity.getId() );


        final Edge connectionSearch = graphManager.writeEdge( edge ).toBlocking().last();




        //now index
        final int batches = indexService.indexEntity( applicationScope, testEntity )
            .flatMap(mesage -> indexProducer.put(mesage)).count().toBlocking().last();


        assertEquals(1, batches);

        final EntityIndex EntityIndex =
            entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        //query until the collection edge is available
        final SearchEdge collectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( collectionEdge );

        final CandidateResults collectionResults = getResults( EntityIndex, collectionSearchEdge,
            SearchTypes.fromTypes( testEntity.getId().getType() ), 1);

        assertEquals( 1, collectionResults.size() );

        assertEquals( testEntity.getId(), collectionResults.get( 0 ).getId() );


        //query until the connection edge is available
        final SearchEdge connectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( connectionSearch );

        final CandidateResults connectionResults = getResults( EntityIndex, connectionSearchEdge,
            SearchTypes.fromTypes( testEntity.getId().getType() ), 1 );

        assertEquals( 1, connectionResults.size() );

        assertEquals( testEntity.getId(), connectionResults.get( 0 ).getId() );
    }

    /**
     * Tests that when we have large connections, we batch appropriately
     * @throws InterruptedException
     */
//    @Test( timeout = 60000 )
    @Test( )
    public void testConnectingIndexingBatches() throws InterruptedException {


        ApplicationScope applicationScope =
            new ApplicationScopeImpl( new SimpleId( UUID.randomUUID(), "application" ) );


        final Entity testEntity = new Entity( createId( "thing" ), UUIDGenerator.newTimeUUID() );
        testEntity.setField( new StringField( "string", "foo" ) );


        //write the entity before indexing
        final EntityCollectionManager collectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );

        collectionManager.write( testEntity ).toBlocking().last();

        final GraphManager graphManager = graphManagerFactory.createEdgeManager( applicationScope );

        //create our collection edge
        final Edge collectionEdge =
            CpNamingUtils.createCollectionEdge( applicationScope.getApplication(), "things", testEntity.getId() );
        graphManager.writeEdge( collectionEdge ).toBlocking().last();


        /**
         * Write 10k edges 10 at a time in parallel
         */

//        final int edgeCount = indexFig.getIndexBatchSize()*2;
        final int edgeCount = 100;

        final List<Edge> connectionSearchEdges = Observable.range( 0, edgeCount ).flatMap( integer -> {
            final Id connectingId = createId( "connecting" );
            final Edge edge = CpNamingUtils.createConnectionEdge( connectingId, "likes", testEntity.getId() );

            return graphManager.writeEdge( edge ).subscribeOn( Schedulers.io() );
        }).toList().toBlocking().last();


        assertEquals( "All edges saved", edgeCount, connectionSearchEdges.size() );

        //get the first and last edge
        final Edge connectionSearch = connectionSearchEdges.get( 0 );

        final Edge lastSearch = connectionSearchEdges.get( edgeCount - 1 );


        //now index
        final int batches = indexService.indexEntity( applicationScope, testEntity )
            .flatMap(mesage -> indexProducer.put(mesage)).count().toBlocking().last();

        //take our edge count + 1 and divided by batch sizes
        final int expectedSize = ( int ) Math.ceil( ( (double)edgeCount + 1 ) / indexFig.getIndexBatchSize() );

        assertEquals(expectedSize, batches);

        final EntityIndex EntityIndex =
            entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        final SearchEdge collectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( collectionEdge );


        //query until it's available
        final CandidateResults collectionResults = getResults( EntityIndex, collectionSearchEdge,
            SearchTypes.fromTypes( testEntity.getId().getType() ), 1 );

        assertEquals( 1, collectionResults.size() );

        assertEquals( testEntity.getId(), collectionResults.get( 0 ).getId() );


        final SearchEdge connectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( connectionSearch );


        //query until it's available
        final CandidateResults connectionResults = getResults( EntityIndex, connectionSearchEdge,
            SearchTypes.fromTypes( testEntity.getId().getType() ), 1 );

        assertEquals( 1, connectionResults.size() );

        assertEquals( testEntity.getId(), connectionResults.get( 0 ).getId() );


        final SearchEdge lastConnectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( lastSearch );


        //query until it's available
        final CandidateResults lastConnectionResults = getResults( EntityIndex, lastConnectionSearchEdge,
            SearchTypes.fromTypes( testEntity.getId().getType() ),  1 );

        assertEquals( 1, lastConnectionResults.size() );

        assertEquals( testEntity.getId(), lastConnectionResults.get( 0 ).getId() );
    }




    /**
     *This test must do the following steps.
     *1. Delete the connecting edge
     *2. Run the deleteIndexEdge using the search edge that gets returned from the delete call
     *3. Run queries to make sure that the collection entity still exists while the connection search edge is gone.
     * @throws InterruptedException
     */
    @Test
    public void testDeleteSingleConnectingEdge() throws InterruptedException {
        ApplicationScope applicationScope =
            new ApplicationScopeImpl( new SimpleId( UUID.randomUUID(), "application" ) );

        final EntityIndex EntityIndex =
            entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        final GraphManager graphManager = graphManagerFactory.createEdgeManager( applicationScope );

        final Entity testEntity = new Entity( createId( "thing" ), UUIDGenerator.newTimeUUID() );
        testEntity.setField( new StringField( "string", "foo" ) );

        //write entity
        final Edge connectionSearch =
            createTestEntityAndReturnConnectionEdge( applicationScope,graphManager,testEntity );


        final SearchEdge connectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( connectionSearch );

        //step 1
        //(We need to mark then delete things in the graph manager.)
        final Edge toBeMarkedEdge = graphManager.markEdge( connectionSearch ).toBlocking().firstOrDefault( null );
        final Edge toBeDeletedEdge = graphManager.deleteEdge( toBeMarkedEdge ).toBlocking().firstOrDefault( null );

        //step 2
        IndexOperationMessage indexOperationMessage =
            indexService.deleteIndexEdge( applicationScope, toBeDeletedEdge ).toBlocking().lastOrDefault(
            null );

        assertEquals( 1, indexOperationMessage.getDeIndexRequests().size() );

        //ensure that no edges remain
        final CandidateResults connectionResultsEmpty = EntityIndex.search( connectionSearchEdge,
            SearchTypes.fromTypes( "things" ),"select *",10,0 );

        assertEquals(0,connectionResultsEmpty.size());

    }

    @Test
    public void testDeleteMultipleConnectingEdges() throws InterruptedException {
        ApplicationScope applicationScope =
            new ApplicationScopeImpl( new SimpleId( UUID.randomUUID(), "application" ) );

        final EntityIndex EntityIndex =
            entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        final GraphManager graphManager = graphManagerFactory.createEdgeManager( applicationScope );

        final Entity testEntity = new Entity( createId( "thing" ), UUIDGenerator.newTimeUUID() );
        testEntity.setField( new StringField( "string", "foo" ) );


        //write entity
        Edge collectionEdge = createEntityandCollectionEdge( applicationScope, graphManager, testEntity );
        //Write multiple connection edges
        final int edgeCount = 5;

        final List<Edge> connectionSearchEdges = createConnectionSearchEdges( testEntity, graphManager, edgeCount );

        indexService.indexEntity( applicationScope, testEntity ).flatMap(mesage -> indexProducer.put(mesage)).toBlocking().getIterator();

        //query until results are available for collections
        final SearchEdge collectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( collectionEdge );
        getResults( EntityIndex, collectionSearchEdge,
            SearchTypes.fromTypes( testEntity.getId().getType() ), 1 );

        for(int i = 0; i < edgeCount; i++) {
            //query until results are available for connections

            final SearchEdge connectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( connectionSearchEdges.get( i ) );
            getResults( EntityIndex, connectionSearchEdge, SearchTypes.fromTypes( testEntity.getId().getType() ),
                 1 );
        }

        for(Edge connectionSearch:connectionSearchEdges) {
            //step 1
            final Edge toBeMarkedEdge = graphManager.markEdge( connectionSearch ).toBlocking().firstOrDefault( null );
            final Edge toBeDeletedEdge = graphManager.deleteEdge( toBeMarkedEdge ).toBlocking().first();

            final SearchEdge connectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( connectionSearch );

            //step 2
            IndexOperationMessage indexOperationMessage =
                indexService.deleteIndexEdge( applicationScope, toBeDeletedEdge ) .flatMap(mesage ->indexProducer.put(mesage)).toBlocking().lastOrDefault( null );

            //not sure if this is still valid.
            assertEquals( 1, indexOperationMessage.getDeIndexRequests().size() );

            //ensure that no edges remain
            final CandidateResults connectionResultsEmpty = EntityIndex.search( connectionSearchEdge,
                SearchTypes.fromTypes( "things" ),"select *",10,0 );

            assertEquals(0,connectionResultsEmpty.size());
        }
    }


    /**
     * Refactor into two methods . Should only have one responsiblitiy.
     * @param applicationScope
     * @param graphManager
     * @return
     */
    private Edge createTestEntityAndReturnConnectionEdge( final ApplicationScope applicationScope,
                                                          final GraphManager graphManager,
                                                          final Entity testEntity) {
        final EntityCollectionManager collectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );

        final EntityIndex EntityIndex =
            entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        final Edge collectionEdge =
            createEntityandCollectionEdge( applicationScope, graphManager, testEntity );


        //create our connection edge.
        final Id connectingId = createId( "connecting" );
        final Edge connectionEdge = CpNamingUtils.createConnectionEdge( connectingId, "likes", testEntity.getId() );

        final Edge connectionSearch = graphManager.writeEdge( connectionEdge ).toBlocking().last();

        //now index
        indexService.indexEntity( applicationScope, testEntity)
            .flatMap(mesage ->indexProducer.put(mesage)).count().toBlocking().last();

        //query until results are available for collections
        final SearchEdge collectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( collectionEdge );
        getResults( EntityIndex, collectionSearchEdge, SearchTypes.fromTypes( testEntity.getId().getType() ),
            1 );

        //query until results are available for connections
        final SearchEdge connectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( connectionSearch );
        getResults( EntityIndex, connectionSearchEdge, SearchTypes.fromTypes( testEntity.getId().getType() ),
            1 );

        return connectionSearch;
    }


    /**
     * Creates an entity along with the corresponding collection edge.
     * @param applicationScope
     * @param graphManager
     * @param testEntity
     * @return
     */
    private Edge createEntityandCollectionEdge( final ApplicationScope applicationScope,
                                                final GraphManager graphManager, final Entity testEntity) {

        final EntityCollectionManager collectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );

        collectionManager.write( testEntity ).toBlocking().last();

        //create our collection edge
        final Edge collectionEdge =
            CpNamingUtils.createCollectionEdge( applicationScope.getApplication(), testEntity.getId().getType(),
                testEntity.getId() );

        graphManager.writeEdge( collectionEdge ).toBlocking().last();
        return collectionEdge;
    }


    private List<Edge> createConnectionSearchEdges(
        final Entity testEntity, final GraphManager graphManager, final int edgeCount ) {

        final List<Edge> connectionSearchEdges = Observable.range( 0, edgeCount ).flatMap( integer -> {

            //create our connection edge.
            final Id connectingId = createId( "connecting" );
            final Edge connectionEdge = CpNamingUtils.createConnectionEdge( connectingId, "likes", testEntity.getId() );

            return graphManager.writeEdge( connectionEdge ).subscribeOn( Schedulers.io() );
        }, 20).toList().toBlocking().last();


        assertEquals( "All edges saved", edgeCount, connectionSearchEdges.size() );
        return connectionSearchEdges;
    }


    private CandidateResults getResults( final EntityIndex EntityIndex,
                                         final SearchEdge searchEdge, final SearchTypes searchTypes,
                                         final int expectedSize ) {
        final int attempts = 100;

        String ql = "select *";
        for ( int i = 0; i < attempts; i++ ) {
            final CandidateResults candidateResults =
                EntityIndex.search( searchEdge, searchTypes, ql , 100, 0 );

            if ( candidateResults.size() == expectedSize ) {
                return candidateResults;
            }

            try {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException e ) {
                //swallow
            }
        }

        fail( "Could not find candidates of size " + expectedSize + "after " + attempts + " attempts" );

        //we'll never reach this, required for compile
        return null;
    }
}
