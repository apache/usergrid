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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import org.apache.commons.lang.time.StopWatch;

import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.core.migration.MigrationException;
import org.apache.usergrid.persistence.core.migration.MigrationManager;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardCache;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.model.entity.Id;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;

import rx.Observable;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


@Ignore("A stress test, not part of functional testing")
public class GraphManagerShardConsistencyIT {
    private static final Logger log = LoggerFactory.getLogger( GraphManagerShardConsistencyIT.class );


    @ClassRule
    public static CassandraRule rule = new CassandraRule();

    private static final MetricRegistry registry = new MetricRegistry();

    private static final Meter writeMeter = registry.meter( "writeThroughput" );

    private static final Slf4jReporter reporter = Slf4jReporter.forRegistry( registry )
                                                .outputTo( log )
                                                .convertRatesTo( TimeUnit.SECONDS )
                                                .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                .build();



    protected ApplicationScope scope;


    protected Object originalShardSize;

    protected Object originalShardTimeout;

    protected Object originalShardDelta;

    @Before
    public void setupOrg() {


        originalShardSize = ConfigurationManager.getConfigInstance().getProperty( GraphFig.SHARD_SIZE );

        originalShardTimeout = ConfigurationManager.getConfigInstance().getProperty( GraphFig.SHARD_CACHE_TIMEOUT );

        originalShardDelta =  ConfigurationManager.getConfigInstance().getProperty( GraphFig.SHARD_MIN_DELTA );


        ConfigurationManager.getConfigInstance().setProperty( GraphFig.SHARD_SIZE, 10000 );


        final long cacheTimeout = 10000;
        //set our cache timeout to 10 seconds
        ConfigurationManager.getConfigInstance().setProperty( GraphFig.SHARD_CACHE_TIMEOUT, cacheTimeout );


        final long minDelta = ( long ) (cacheTimeout * 2.5);

        ConfigurationManager.getConfigInstance().setProperty( GraphFig.SHARD_MIN_DELTA, minDelta );




        //get the system property of the UUID to use.  If one is not set, use the defualt
        String uuidString = System.getProperty( "org.id", "80a42760-b699-11e3-a5e2-0800200c9a66" );

        scope = new ApplicationScopeImpl( createId( UUID.fromString( uuidString ), "test" ) );



        reporter.start( 10, TimeUnit.SECONDS );
    }


    @After
    public void tearDown(){
        reporter.stop();
        reporter.report();
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

        final Id sourceId = createId("source");
        final String edgeType = "test";

        EdgeGenerator generator = new EdgeGenerator() {


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( sourceId, edgeType,  createId( "target" ) );


                return edge;
            }


            @Override
            public Observable<Edge> doSearch( final GraphManager manager ) {
                return manager.loadEdgesFromSource( new SimpleSearchByEdgeType( sourceId, "test", System.currentTimeMillis(), null ) );
            }
        };


        final int numInjectors = 2;

        /**
         * create 3 injectors.  This way all the caches are independent of one another.  This is the same as
         * multiple nodes
         */
        final List<Injector> injectors = createInjectors(numInjectors);


        final GraphFig graphFig = getInstance( injectors, GraphFig.class );

        final long shardSize =  graphFig.getShardSize();


        //we don't want to starve the cass runtime since it will be on the same box. Only take 50% of processing power for writes
        final int numProcessors = Runtime.getRuntime().availableProcessors() /2 ;

        final int numWorkers = numProcessors/numInjectors;


        /**
         * Do 4x shard size so we should have approximately 4 shards
         */
        final long numberOfEdges =  shardSize * 4;


        final long countPerWorker = numberOfEdges/numWorkers;

        final long writeLimit = countPerWorker;



        //min stop time the min delta + 1 cache cycle timeout
        final long minExecutionTime = graphFig.getShardMinDelta() + graphFig.getShardCacheTimeout();





        final List<Future<Boolean>> futures = new ArrayList<>();

        for(Injector injector: injectors) {
            final GraphManagerFactory gmf = injector.getInstance( GraphManagerFactory.class );

            futures.addAll( doTest( gmf, generator, numWorkers,  writeLimit, minExecutionTime ) );
        }

        for(Future<Boolean> future: futures){
            future.get();
        }

        //now get all our shards
        final NodeShardCache cache = getInstance( injectors, NodeShardCache.class );

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( sourceId, edgeType );

        int count = 0;

        while(true) {

            //reset our count.  Ultimately we'll have 4 groups once our compaction completes
            count = 0;


            //we have to get it from the cache, because this will trigger the compaction process
            final Iterator<ShardEntryGroup> groups = cache.getReadShardGroup( scope, Long.MAX_VALUE, directedEdgeMeta );

            while(groups.hasNext()){
                final ShardEntryGroup group = groups.next();

                log.info( "Compaction pending status for group {} is {}", group, group.isCompactionPending() );

                count++;

            }

            //we're done
            if(count == 4){
                break;
            }

            Thread.sleep(5000);
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
    private List<Future<Boolean>> doTest(final GraphManagerFactory factory, final EdgeGenerator generator, final int numWorkers, final long writeCount, final long minExecutionTime ) throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool( numWorkers );

        List<Future<Boolean>> futures = new ArrayList<>( numWorkers );

        for ( int i = 0; i < numWorkers; i++ ) {
            Future<Boolean> future = executor.submit( new Worker(factory, generator, writeCount, minExecutionTime ) );

            futures.add( future );
        }


        return futures;
    }


    private class Worker implements Callable<Boolean> {
        private final GraphManagerFactory factory;
        private final EdgeGenerator generator;
        private final long writeLimit;
        private final long minExecutionTime;


        private Worker( final GraphManagerFactory factory, final EdgeGenerator generator, final long writeLimit, final long minExecutionTime ) {
            this.factory = factory;
            this.generator = generator;
            this.writeLimit = writeLimit;
            this.minExecutionTime = minExecutionTime;
        }


        @Override
        public Boolean call() throws Exception {
            GraphManager manager = factory.createEdgeManager( scope );



            final long startTime = System.currentTimeMillis();


            for ( long i = 0; i < writeLimit || System.currentTimeMillis() - startTime < minExecutionTime ; i++ ) {

                Edge edge = generator.newEdge();

                Edge returned = manager.writeEdge( edge ).toBlocking().last();


                assertNotNull( "Returned has a version", returned.getTimestamp() );


                writeMeter.mark();

                if ( i % 1000 == 0 ) {
                    log.info( "   Wrote: " + i );
                }
            }


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





