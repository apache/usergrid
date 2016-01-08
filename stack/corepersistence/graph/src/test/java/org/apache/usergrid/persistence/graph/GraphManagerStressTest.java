/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.graph;


import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.StressTest;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(ITRunner.class)
@UseModules(TestGraphModule.class)
@Category(StressTest.class)
public class GraphManagerStressTest {
    private static final Logger logger = LoggerFactory.getLogger( GraphManagerStressTest.class );

    @Inject
    private GraphManagerFactory factory;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    protected ApplicationScope scope;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );
    }


    @Test
    @Category(StressTest.class)
    public void writeThousands() throws InterruptedException {
        EdgeGenerator generator = new EdgeGenerator() {

            private Set<Id> sourceIds = new HashSet<Id>();


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( "source", "test", "target" );

                sourceIds.add( edge.getSourceNode() );

                return edge;
            }


            @Override
            public Observable<MarkedEdge> doSearch( final GraphManager manager ) {


                final long timestamp = System.currentTimeMillis();


                return Observable.create( new Observable.OnSubscribe<MarkedEdge>() {

                    @Override
                    public void call( final Subscriber<? super MarkedEdge> subscriber ) {
                        try {
                            for ( Id sourceId : sourceIds ) {

                                final Iterable<MarkedEdge> edges = manager.loadEdgesFromSource(
                                        new SimpleSearchByEdgeType( sourceId, "test", timestamp, SearchByEdgeType.Order.DESCENDING,  Optional
                                                                                    .<Edge>absent() ) )
                                                                    .toBlocking().toIterable();

                                for ( MarkedEdge edge : edges ) {
                                    logger.debug( "Firing on next for edge {}", edge );

                                    subscriber.onNext( edge );
                                }
                            }
                        }
                        catch ( Throwable throwable ) {
                            subscriber.onError( throwable );
                        }
                    }
                } );


                //TODO T.N keep this code it's exhibiting a failure /exception swallowing with RX when our scheduler
                // is full
                //
                //              return  Observable.create( new Observable.OnSubscribe<Edge>() {
                //
                //                    @Override
                //                    public void call( final Subscriber<? super Edge> subscriber ) {
                //                        for ( Id sourceId : sourceIds ) {
                //
                //                                            final Observable<Edge> edges =
                //                                                    manager.loadEdgesFromSource( new
                // SimpleSearchByEdgeType( sourceId, "test", uuid, null ) );
                //
                //                            edges.subscribe( new Action1<Edge>() {
                //                                @Override
                //                                public void call( final Edge edge ) {
                //                                    subscriber.onNext( edge );
                //                                }
                //                            },
                //
                //                            new Action1<Throwable>() {
                //                                @Override
                //                                public void call( final Throwable throwable ) {
                //                                    subscriber.onError( throwable );
                //                                }
                //                            });
                //                         }
                //                    }
                //                } ) ;


            }
        };

        doTest( generator );
    }


    @Category(StressTest.class)
    @Test
    public void writeThousandsSingleSource() throws InterruptedException {
        EdgeGenerator generator = new EdgeGenerator() {

            private Id sourceId = IdGenerator.createId( "source" );


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( sourceId, "test", IdGenerator.createId( "target" ) );


                return edge;
            }


            @Override
            public Observable<MarkedEdge> doSearch( final GraphManager manager ) {
                return manager.loadEdgesFromSource( new SimpleSearchByEdgeType( sourceId, "test", System.currentTimeMillis(), SearchByEdgeType.Order.DESCENDING,  Optional.<Edge>absent() ) );
            }
        };

        doTest( generator );
    }


    @Test
    @Category(StressTest.class)
    public void writeThousandsSingleTarget() throws InterruptedException {
        EdgeGenerator generator = new EdgeGenerator() {

            private Id targetId = IdGenerator.createId( "target" );


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( IdGenerator.createId( "source" ), "test", targetId );


                return edge;
            }


            @Override
            public Observable<MarkedEdge> doSearch( final GraphManager manager ) {

                return manager.loadEdgesToTarget( new SimpleSearchByEdgeType( targetId, "test", System.currentTimeMillis(), SearchByEdgeType.Order.DESCENDING,  Optional.<Edge>absent() ) );
            }
        };

        doTest( generator );
    }


    /**
     * Execute the test with the generator
     */
    private void doTest( EdgeGenerator generator ) throws InterruptedException {
        GraphManager manager = factory.createEdgeManager( scope );

        int limit = 10000;

        final StopWatch timer = new StopWatch();
        timer.start();
        final Set<Edge> ids = new HashSet<Edge>( limit );

        for ( int i = 0; i < limit; i++ ) {

            Edge edge = generator.newEdge();

            Edge returned = manager.writeEdge( edge ).toBlocking().last();


            assertNotNull( "Returned has a version", returned.getTimestamp() );

            ids.add( returned );

            if ( i % 1000 == 0 ) {
                logger.info( "   Wrote: " + i );
            }
        }

        timer.stop();
        logger.info( "Total time to write {} entries {}ms", limit, timer.getTime() );
        timer.reset();

        timer.start();

        final CountDownLatch latch = new CountDownLatch( 1 );


        generator.doSearch( manager ).subscribe( new Subscriber<Edge>() {
            @Override
            public void onCompleted() {
                timer.stop();
                latch.countDown();
            }


            @Override
            public void onError( final Throwable throwable ) {
                fail( "Exception occurced " + throwable );
            }


            @Override
            public void onNext( final Edge edge ) {
                ids.remove( edge );
            }
        } );


        latch.await();


        assertEquals( 0, ids.size() );


        logger.info( "Total time to read {} entries {}ms", limit, timer.getTime() );
    }


    private interface EdgeGenerator {

        /**
         * Create a new edge to persiste
         */
        public Edge newEdge();

        public Observable<MarkedEdge> doSearch( final GraphManager manager );
    }
}
