/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.graph;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.commons.lang.time.StopWatch;

import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.hystrix.HystrixCassandra;
import org.apache.usergrid.persistence.core.migration.MigrationException;
import org.apache.usergrid.persistence.core.migration.MigrationManager;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Injector;

import rx.Observable;
import rx.Subscriber;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


@Ignore("A stress test, not part of functional testing")
public class GraphManagerShardConsistencyIT {
    private static final Logger log = LoggerFactory.getLogger( GraphManagerShardConsistencyIT.class );


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    protected ApplicationScope scope;

    protected int numWorkers;

    @Before
    public void setupOrg() {



        //get the system property of the UUID to use.  If one is not set, use the defualt
        String uuidString = System.getProperty( "org.id", "80a42760-b699-11e3-a5e2-0800200c9a66" );

        scope = new ApplicationScopeImpl( createId( UUID.fromString( uuidString ), "test" ) );

        numWorkers = Integer.parseInt( System.getProperty( "numWorkers", "4" ) );
//        readCount = Integer.parseInt( System.getProperty( "readCount", "20000" ) );
    }


//    @Test
//    public void writeThousandsSingleSource() throws InterruptedException, ExecutionException {
//        EdgeGenerator generator = new EdgeGenerator() {
//
//            private Id sourceId = createId( "source" );
//
//
//            @Override
//            public Edge newEdge() {
//                Edge edge = createEdge( sourceId, "test", createId( "target" ) );
//
//
//                return edge;
//            }
//
//
//            @Override
//            public Observable<Edge> doSearch( final GraphManager manager ) {
//                return manager.loadEdgesFromSource( new SimpleSearchByEdgeType( sourceId, "test", System.currentTimeMillis(), null ) );
//            }
//        };
//
//
//
//        doTest( generator );
//    }


    @Test
    public void writeThousandsSingleTarget() throws InterruptedException, ExecutionException, MigrationException {

        final Id targetId = createId("target");
        final String edgeType = "test";

        EdgeGenerator generator = new EdgeGenerator() {


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( createId( "source" ), edgeType,  targetId );


                return edge;
            }


            @Override
            public Observable<Edge> doSearch( final GraphManager manager ) {
                return manager.loadEdgesToTarget( new SimpleSearchByEdgeType( targetId, "test", System.currentTimeMillis(), null ) );
            }
        };


        /**
         * create 3 injectors.  This way all the caches are independent of one another.  This is the same as
         * multiple nodes
         */
        final List<Injector> injectors = createInjectors(3);


        final GraphFig graphFig = getInstance( injectors, GraphFig.class );

        final long shardSize =  graphFig.getShardSize();

        /**
         * Do 4x shard size so we should have approximately 4 shards
         */
        final long numberOfEdges =  shardSize * 4;

        final long countPerWorker = numberOfEdges/numWorkers;

        final long writeLimit = countPerWorker;


//        HystrixCassandra.ASYNC_GROUP.





        final List<Future<Boolean>> futures = new ArrayList<>();

        for(Injector injector: injectors) {
            final GraphManagerFactory gmf = injector.getInstance( GraphManagerFactory.class );

            futures.addAll( doTest( gmf, generator, writeLimit ) );
        }

        for(Future<Boolean> future: futures){
            future.get();
        }

        //now get all our shards
        final NodeShardAllocation allocation = getInstance( injectors, NodeShardAllocation.class );

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromTargetNode( targetId, edgeType );

        int count = 0;

        while(count < 4) {

            //reset our count.  Ultimately we'll have 4 groups once our compaction completes
            count = 0;

            final Iterator<ShardEntryGroup> groups = allocation.getShards( scope, Optional.<Shard>absent(), directedEdgeMeta );

            while(groups.hasNext()){
                final ShardEntryGroup group = groups.next();

                log.info( "Compaction pending status for group {} is {}", group, group.isCompactionPending() );

                count++;

            }
        }




    }

    private <T> T getInstance(final List<Injector> injectors, Class<T> clazz ){
        return injectors.get( 0 ).getInstance( clazz );
    }


    /**
     * Create new Guice injector environments and return them
     * @param count
     */
    private List<Injector> createInjectors( int count ) throws MigrationException {

        final List<Injector> injectors = new ArrayList<>(count);

        for(int i = 0; i < count; i++){
            final Injector injector = Guice.createInjector( new TestGraphModule() );
            injectors.add( injector );
        }


        final MigrationManager migrationManager = getInstance( injectors, MigrationManager.class );

        migrationManager.migrate();

        return injectors;


    }

    /**
     * Execute the test with the generator
     */
    private List<Future<Boolean>> doTest(final GraphManagerFactory factory, final EdgeGenerator generator, final long writeCount ) throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool( numWorkers );

        List<Future<Boolean>> futures = new ArrayList<>( numWorkers );

        for ( int i = 0; i < numWorkers; i++ ) {
            Future<Boolean> future = executor.submit( new Worker(factory, generator, writeCount ) );

            futures.add( future );
        }


        return futures;
    }


    private class Worker implements Callable<Boolean> {
        private final GraphManagerFactory factory;
        private final EdgeGenerator generator;
        private final long writeLimit;


        private Worker( final GraphManagerFactory factory, final EdgeGenerator generator, final long writeLimit) {
            this.factory = factory;
            this.generator = generator;
            this.writeLimit = writeLimit;
        }


        @Override
        public Boolean call() throws Exception {
            GraphManager manager = factory.createEdgeManager( scope );


            final StopWatch timer = new StopWatch();
            timer.start();

            for ( long i = 0; i < writeLimit; i++ ) {

                Edge edge = generator.newEdge();

                Edge returned = manager.writeEdge( edge ).toBlocking().last();


                assertNotNull( "Returned has a version", returned.getTimestamp() );


                if ( i % 1000 == 0 ) {
                    log.info( "   Wrote: " + i );
                }
            }

            timer.stop();
            log.info( "Total time to write {} entries {} ms", writeLimit, timer.getTime() );

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





