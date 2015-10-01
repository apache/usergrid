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


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.time.StopWatch;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import rx.Observable;
import rx.Subscriber;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


@RunWith( ITRunner.class )
@UseModules( TestGraphModule.class )
@Ignore("Not for testing during build.  Kills embedded Cassandra")
public class GraphManagerLoadTest {
    private static final Logger log = LoggerFactory.getLogger( GraphManagerLoadTest.class );

    @Inject
    private GraphManagerFactory factory;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    protected ApplicationScope scope;

    protected int numWorkers;
    protected int writeLimit;
    protected int readCount;


    @Before
    public void setupOrg() {
        //get the system property of the UUID to use.  If one is not set, use the defualt
        String uuidString = System.getProperty( "org.id", "80a42760-b699-11e3-a5e2-0800200c9a66" );

        scope = new ApplicationScopeImpl( IdGenerator.createId( UUID.fromString( uuidString ), "test" ) );

        numWorkers = Integer.parseInt( System.getProperty( "numWorkers", "100" ) );
        writeLimit = Integer.parseInt( System.getProperty( "writeLimit", "10000" ) );
        readCount = Integer.parseInt( System.getProperty( "readCount", "20000" ) );
    }


//    @Ignore
    @Test
    public void writeThousandsSingleSource() throws InterruptedException, ExecutionException {
        EdgeGenerator generator = new EdgeGenerator() {

            private Id sourceId = IdGenerator.createId( "source" );


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( sourceId, "test", IdGenerator.createId( "target" ) );


                return edge;
            }


            @Override
            public Observable<Edge> doSearch( final GraphManager manager ) {
                 return manager.loadEdgesFromSource( new SimpleSearchByEdgeType( sourceId, "test", System.currentTimeMillis(), SearchByEdgeType.Order.DESCENDING,  Optional
                                      .<Edge>absent()) );
            }
        };

        doTest( generator );
    }


    @Test
    @Ignore("Too heavy for normal build process")
    public void writeThousandsSingleTarget() throws InterruptedException, ExecutionException {
        EdgeGenerator generator = new EdgeGenerator() {

            private Id targetId = IdGenerator.createId( "target" );


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( IdGenerator.createId( "source" ), "test", targetId );


                return edge;
            }


            @Override
            public Observable<Edge> doSearch( final GraphManager manager ) {
                return manager.loadEdgesToTarget( new SimpleSearchByEdgeType( targetId, "test", System.currentTimeMillis(), SearchByEdgeType.Order.DESCENDING,  Optional.<Edge>absent() ) );
            }
        };

        doTest( generator );
    }


    /**
     * Execute the test with the generator
     */
    private void doTest( EdgeGenerator generator ) throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool( numWorkers );

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>( numWorkers );

        for ( int i = 0; i < numWorkers; i++ ) {
            Future<Boolean> future = executor.submit( new Worker( generator, writeLimit, readCount ) );

            futures.add( future );
        }


        /**
         * Block on all workers until they're done
         */
        for ( Future<Boolean> future : futures ) {
            future.get();
        }
    }


    private class Worker implements Callable<Boolean> {
        private final EdgeGenerator generator;
        private final int writeLimit;
        private final int readCount;


        private Worker( final EdgeGenerator generator, final int writeLimit, final int readCount ) {
            this.generator = generator;
            this.writeLimit = writeLimit;
            this.readCount = readCount;
        }


        @Override
        public Boolean call() throws Exception {
            GraphManager manager = factory.createEdgeManager( scope );


            final StopWatch timer = new StopWatch();
            timer.start();

            for ( int i = 0; i < writeLimit; i++ ) {

                Edge edge = generator.newEdge();

                Edge returned = manager.writeEdge( edge ).toBlocking().last();


                assertNotNull( "Returned has a version", returned.getTimestamp() );


                if ( i % 1000 == 0 ) {
                    log.info( "   Wrote: " + i );
                }
            }

            timer.stop();
            log.info( "Total time to write {} entries {} ms", writeLimit, timer.getTime() );
            timer.reset();

            timer.start();

            final CountDownLatch latch = new CountDownLatch( 1 );


            generator.doSearch( manager ).take( readCount ).buffer( 1000 ).subscribe( new Subscriber<List<Edge>>() {
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
                public void onNext( final List<Edge> edges ) {
                    log.info("Read {} edges", edges.size());
                }
            } );


            latch.await();


            log.info( "Total time to read {} entries {} ms", readCount, timer.getTime() );

            return true;
        }
    }


    private interface EdgeGenerator {

        /**
         * Create a new edge to persiste
         */
        public Edge newEdge();

        /**
         * Perform the search returning an observable edge
         * @param manager
         * @return
         */
        public Observable<Edge> doSearch( final GraphManager manager );
    }
}
