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


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.index.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.corepersistence.TestIndexModule;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.index.impl.EsRunner;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import net.jcip.annotations.NotThreadSafe;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
@NotThreadSafe
public abstract class AsyncIndexServiceTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    public EntityCollectionManagerFactory entityCollectionManagerFactory;

    @Inject
    public GraphManagerFactory graphManagerFactory;


    @Inject
    public EntityIndexFactory entityIndexFactory;


    @Inject
    public IndexLocationStrategyFactory indexLocationStrategyFactory;

    @Inject
    public EntityManagerFactory emf;


    private AsyncEventService asyncEventService;


    /**
     * Get the async index service
     */
    protected abstract AsyncEventService getAsyncEventService();


    @Before
    public void setup() {
        asyncEventService = getAsyncEventService();
    }


    @Test( )
    public void testMessageIndexing() throws InterruptedException {

        ApplicationScope applicationScope =
            new ApplicationScopeImpl( new SimpleId( UUID.randomUUID(), "application" ) );


        final Entity testEntity = new Entity( createId( "thing" ), UUIDGenerator.newTimeUUID() );
        testEntity.setField( new StringField( "string", "foo" ) );


        //write the entity before indexing
        final EntityCollectionManager collectionManager =
            entityCollectionManagerFactory.createCollectionManager(applicationScope);

        collectionManager.write(testEntity).toBlocking().last();

        final GraphManager graphManager = graphManagerFactory.createEdgeManager( applicationScope );

        //create our collection edge
        final Edge collectionEdge =
            CpNamingUtils.createCollectionEdge( applicationScope.getApplication(), "things", testEntity.getId() );
        graphManager.writeEdge( collectionEdge ).toBlocking().last();


        /**
         * Write 500 edges
         */


        final List<Edge> connectionSearchEdges = Observable.range( 0, 500 ).flatMap(integer -> {
            final Id connectingId = createId("connecting");
            final Edge edge = CpNamingUtils.createConnectionEdge(connectingId, "likes", testEntity.getId());

            return graphManager.writeEdge( edge );
        }).toList().toBlocking().last();


        //queue up processing
        asyncEventService.queueEntityIndexUpdate( applicationScope, testEntity );


        final EntityIndex EntityIndex =
            entityIndexFactory.createEntityIndex( indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        emf.refreshIndex(applicationScope.getApplication().getUuid());

        final SearchEdge collectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource( collectionEdge );

        //query until it's available
        final CandidateResults collectionResults = getResults( EntityIndex, collectionSearchEdge,
            SearchTypes.fromTypes( testEntity.getId().getType() ),  1, 100 );

        assertEquals( 1, collectionResults.size() );

        assertEquals( testEntity.getId(), collectionResults.get( 0 ).getId() );


        final SearchEdge connectionSearchEdge = CpNamingUtils.createSearchEdgeFromSource(connectionSearchEdges.get(connectionSearchEdges.size()-1) );


        //query until it's available
        final CandidateResults connectionResults = getResults( EntityIndex, connectionSearchEdge,
            SearchTypes.fromTypes( testEntity.getId().getType() ), 1, 100 );

        assertEquals( 1, connectionResults.size() );

        assertEquals( testEntity.getId(), connectionResults.get( 0 ).getId() );
    }


    private CandidateResults getResults( final EntityIndex entityIndex,
                                         final SearchEdge searchEdge, final SearchTypes searchTypes, final int expectedSize, final int attempts ) {


        for ( int i = 0; i < attempts; i++ ) {
            final CandidateResults candidateResults =
                entityIndex.search( searchEdge, searchTypes, "select *", 100, 0 );

            if ( candidateResults.size() == expectedSize ) {
                return candidateResults;
            }

            try {
                Thread.sleep( 10000 );
            }
            catch ( InterruptedException e ) {
                //swallow
            }
        }

        fail( "Could not find candidates of size " + expectedSize + " after " + attempts + " attempts" );

        //we'll never reach this, required for compile
        return null;
    }
}
