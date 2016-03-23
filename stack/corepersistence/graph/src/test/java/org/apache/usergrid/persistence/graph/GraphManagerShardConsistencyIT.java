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


import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.apache.usergrid.StressTest;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.migration.schema.MigrationException;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardCache;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.model.entity.Id;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;

import rx.Observable;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class GraphManagerShardConsistencyIT {
    private static final Logger logger = LoggerFactory.getLogger( GraphManagerShardConsistencyIT.class );


    private static final MetricRegistry registry = new MetricRegistry();

    private static final Meter writeMeter = registry.meter( "writeThroughput" );

    private Slf4jReporter reporter;


    protected ApplicationScope scope;


    protected Object originalShardSize;

    protected Object originalShardTimeout;

    protected Object originalShardDelta;

    protected ListeningExecutorService writeExecutor;

    protected ListeningExecutorService deleteExecutor;

    protected int TARGET_NUM_SHARDS = 5;

    protected int POST_WRITE_SLEEP = 2000;



    @Before
    public void setupOrg() {


        originalShardSize = ConfigurationManager.getConfigInstance().getProperty( GraphFig.SHARD_SIZE );

        originalShardTimeout = ConfigurationManager.getConfigInstance().getProperty( GraphFig.SHARD_CACHE_TIMEOUT );

        originalShardDelta = ConfigurationManager.getConfigInstance().getProperty( GraphFig.SHARD_MIN_DELTA );


        ConfigurationManager.getConfigInstance().setProperty( GraphFig.SHARD_SIZE, 10000 );


        final long cacheTimeout = 1000;
        //set our cache timeout to the above value
        ConfigurationManager.getConfigInstance().setProperty( GraphFig.SHARD_CACHE_TIMEOUT, cacheTimeout );


        final long minDelta = ( long ) ( cacheTimeout * 2.5 );

        ConfigurationManager.getConfigInstance().setProperty( GraphFig.SHARD_MIN_DELTA, minDelta );


        // get the system property of the UUID to use.  If one is not set, use the defualt
        String uuidString = System.getProperty( "org.id", "80a42760-b699-11e3-a5e2-0800200c9a66" );

        scope = new ApplicationScopeImpl( IdGenerator.createId( UUID.fromString( uuidString ), "test" ) );


        reporter =
                Slf4jReporter.forRegistry( registry ).outputTo(logger).convertRatesTo( TimeUnit.SECONDS )
                             .convertDurationsTo( TimeUnit.MILLISECONDS ).build();


        reporter.start( 10, TimeUnit.SECONDS );
    }


    @After
    public void tearDown() {
        reporter.stop();
        reporter.report();

        if(writeExecutor != null){
            writeExecutor.shutdownNow();

        }
        if(deleteExecutor != null){
            deleteExecutor.shutdownNow();

        }

    }


    private void createWriteExecutor( final int size ) {
        writeExecutor = MoreExecutors.listeningDecorator( Executors.newFixedThreadPool( size ) );
    }

    private void createDeleteExecutor( final int size ) {
        deleteExecutor = MoreExecutors.listeningDecorator( Executors.newFixedThreadPool( size ) );
    }

    @Test(timeout=300000) // set a timeout so this doesn't run forever in the event that it is failing
    public void writeThousandsSingleSource()
        throws InterruptedException, ExecutionException, MigrationException, UnsupportedEncodingException {

        final Id sourceId = IdGenerator.createId( "sourceWrite" );
        final String edgeType = "testWrite";

        final EdgeGenerator generator = new EdgeGenerator() {


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( sourceId, edgeType, IdGenerator.createId( "targetWrite" ) );


                return edge;
            }


            @Override
            public Observable<MarkedEdge> doSearch( final GraphManager manager ) {
                return manager.loadEdgesFromSource(
                    new SimpleSearchByEdgeType( sourceId, edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                        Optional.<Edge>absent() ) );
            }
        };


        final int numInjectors = 2;

        /**
         * create injectors.  This way all the caches are independent of one another.  This is the same as
         * multiple nodes if there are multiple injectors
         */
        final List<Injector> injectors = createInjectors( numInjectors );


        final GraphFig graphFig = getInstance( injectors, GraphFig.class );

        final long shardSize = graphFig.getShardSize();


        //we don't want to starve the cass runtime since it will be on the same box. Only take 50% of processing
        // power for writes
        final int numProcessors = Runtime.getRuntime().availableProcessors() / 2;

        final int numWorkersPerInjector = numProcessors / numInjectors;



        final long numberOfEdges = shardSize * TARGET_NUM_SHARDS;


        final long workerWriteLimit = numberOfEdges / numWorkersPerInjector / numInjectors;


        createWriteExecutor( numWorkersPerInjector );

        final AtomicLong writeCounter = new AtomicLong();


        //min stop time the min delta + 1 cache cycle timeout
        final long minExecutionTime = graphFig.getShardMinDelta() + graphFig.getShardCacheTimeout() + 120000;


        logger.info( "Writing {} edges per worker on {} workers in {} injectors", workerWriteLimit, numWorkersPerInjector,
            numInjectors );


        final List<Future<Boolean>> futures = new ArrayList<>();


        for ( Injector injector : injectors ) {
            final GraphManagerFactory gmf = injector.getInstance( GraphManagerFactory.class );


            for ( int i = 0; i < numWorkersPerInjector; i++ ) {
                Future<Boolean> future =
                    writeExecutor.submit( new Worker( gmf, generator, workerWriteLimit, minExecutionTime, writeCounter ) );

                futures.add( future );
            }
        }

        /**
         * Wait for all writes to complete
         */
        for ( Future<Boolean> future : futures ) {
            future.get();
        }

        //now get all our shards
        final NodeShardCache cache = getInstance( injectors, NodeShardCache.class );

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( sourceId, edgeType );

        //now submit the readers.
        final GraphManagerFactory gmf = getInstance( injectors, GraphManagerFactory.class );


        final long writeCount = writeCounter.get();
        final long expectedShardCount = writeCount / shardSize;
        final Meter readMeter = registry.meter( "readThroughput-writeTest" );


        final List<Throwable> failures = new ArrayList<>();

        logger.info("Sleeping {}ms before reading to ensure all compactions have completed", POST_WRITE_SLEEP);
        Thread.sleep(POST_WRITE_SLEEP); // let's make sure everything is written

        for(int i = 0; i < 2; i ++) {


            /**
             * Start reading continuously while we migrate data to ensure our view is always correct
             */
            final ListenableFuture<Long> future =
                writeExecutor.submit( new ReadWorker( gmf, generator, writeCount, readMeter ) );


            //add the future
            Futures.addCallback( future, new FutureCallback<Long>() {

                @Override
                public void onSuccess( @Nullable final Long result ) {
                    logger.info( "Successfully ran the read, re-running" );
                    writeExecutor.submit( new ReadWorker( gmf, generator, writeCount, readMeter ) );
                }


                @Override
                public void onFailure( final Throwable t ) {
                    failures.add( t );
                    logger.error( "Failed test!", t );

                    final Iterator<ShardEntryGroup> groups = cache.getReadShardGroup( scope, Long.MAX_VALUE, directedEdgeMeta );

                    while ( groups.hasNext() ) {

                        logger.info( "Shard entry group: {}", groups.next() );

                    }


                }
            } );
        }


        int compactedCount;


        // now start the compaction watcher

        while ( true ) {

            if ( !failures.isEmpty() ) {

                StringBuilder builder = new StringBuilder();

                builder.append( "Read runner failed!\n" );

                for ( Throwable t : failures ) {
                    builder.append( "Exception is: " );
                    ByteArrayOutputStream output = new ByteArrayOutputStream();

                    t.printStackTrace( new PrintWriter( output ) );

                    builder.append( output.toString( "UTF-8" ) );
                    builder.append( "\n\n" );
                }


                fail( builder.toString() );
            }

            // reset our count.  Ultimately we'll have 4 groups once our compaction completes
            compactedCount = 0;

            // we have to get it from the cache, because this will trigger the compaction process
            final Iterator<ShardEntryGroup> groups = cache.getReadShardGroup( scope, Long.MAX_VALUE, directedEdgeMeta );
            final Set<ShardEntryGroup> shardEntryGroups = new HashSet<>();

            while ( groups.hasNext() ) {

                final ShardEntryGroup group = groups.next();
                shardEntryGroups.add( group );

                logger.info( "Compaction pending status for group {} is {}", group, group.isCompactionPending() );

                if ( !group.isCompactionPending() ) {
                    compactedCount++;
                }
            }


            //we're done
            if ( compactedCount >= expectedShardCount ) {

                logger.info( "All compactions complete, sleeping. Compacted shard count={}, expected shard count={}",
                    compactedCount, expectedShardCount );

                break;
            }


            Thread.sleep( 2000 );
        }


        //now continue reading everything for 30 seconds

        Thread.sleep(30000);

        writeExecutor.shutdownNow();
    }


    private <T> T getInstance( final List<Injector> injectors, Class<T> clazz ) {
        return injectors.get( 0 ).getInstance( clazz );
    }


    /**
     * Create new Guice injector environments and return them
     */
    private List<Injector> createInjectors( int count ) throws MigrationException {

        final List<Injector> injectors = new ArrayList<>( count );

        for ( int i = 0; i < count; i++ ) {
            final Injector injector = Guice.createInjector( new TestGraphModule() );
            injectors.add( injector );
        }


        final MigrationManager migrationManager = getInstance( injectors, MigrationManager.class );

        migrationManager.migrate();

        return injectors;
    }


    @Test(timeout=300000) // this test is SLOW as deletes are intensive and shard cleanup is async
    @Category(StressTest.class)
    public void writeThousandsDelete()
        throws InterruptedException, ExecutionException, MigrationException, UnsupportedEncodingException {

        final Id sourceId = IdGenerator.createId( "sourceDelete" );
        final String deleteEdgeType = "testDelete";

        final EdgeGenerator generator = new EdgeGenerator() {


            @Override
            public Edge newEdge() {
                Edge edge = createEdge( sourceId, deleteEdgeType, IdGenerator.createId( "targetDelete" ) );


                return edge;
            }


            @Override
            public Observable<MarkedEdge> doSearch( final GraphManager manager ) {
                return manager.loadEdgesFromSource(
                    new SimpleSearchByEdgeType( sourceId, deleteEdgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                        Optional.<Edge>absent(), false ) );
            }
        };


        final int numInjectors = 3;

        /**
         * create injectors.  This way all the caches are independent of one another.  This is the same as
         * multiple nodes if there are multiple injectors
         */
        final List<Injector> injectors = createInjectors( numInjectors );


        final GraphFig graphFig = getInstance( injectors, GraphFig.class );

        final long shardSize = graphFig.getShardSize();


        //we don't want to starve the cass runtime since it will be on the same box. Only take 50% of processing
        // power for writes
        final int numProcessors = Runtime.getRuntime().availableProcessors() / 2;

        final int numWorkersPerInjector = numProcessors / numInjectors;

        final long numberOfEdges = shardSize * TARGET_NUM_SHARDS;


        final long workerWriteLimit = numberOfEdges / numWorkersPerInjector / numInjectors;

        createDeleteExecutor( numWorkersPerInjector );


        final AtomicLong writeCounter = new AtomicLong();


        //min stop time the min delta + 1 cache cycle timeout
        final long minExecutionTime = graphFig.getShardMinDelta() + graphFig.getShardCacheTimeout();


        logger.info( "Writing {} edges per worker on {} workers in {} injectors", workerWriteLimit, numWorkersPerInjector,
            numInjectors );


        final List<Future<Boolean>> futures = new ArrayList<>();


        for ( Injector injector : injectors ) {
            final GraphManagerFactory gmf = injector.getInstance( GraphManagerFactory.class );


            for ( int i = 0; i < numWorkersPerInjector; i++ ) {
                Future<Boolean> future =
                    deleteExecutor.submit( new Worker( gmf, generator, workerWriteLimit, minExecutionTime, writeCounter ) );

                futures.add( future );
            }
        }

        /**
         * Wait for all writes to complete
         */
        for ( Future<Boolean> future : futures ) {
            future.get();
        }

        // now get all our shards
        final NodeShardCache cache = getInstance( injectors, NodeShardCache.class );

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( sourceId, deleteEdgeType );

        final GraphManagerFactory gmf = getInstance( injectors, GraphManagerFactory.class );


        final long writeCount = writeCounter.get();
        final Meter readMeter = registry.meter( "readThroughput-deleteTest" );


        //check our shard state


        final Iterator<ShardEntryGroup> existingShardGroups =
            cache.getReadShardGroup( scope, Long.MAX_VALUE, directedEdgeMeta );
        int shardCount = 0;

        while ( existingShardGroups.hasNext() ) {
            final ShardEntryGroup group = existingShardGroups.next();

            shardCount++;

            logger.info( "Compaction pending status for group {} is {}", group, group.isCompactionPending() );
        }


        logger.info( "Found {} shard groups", shardCount );


        //now mark and delete all the edges


        final GraphManager manager = gmf.createEdgeManager( scope );

        //sleep occasionally to stop pushing cassandra over

        long count = Long.MAX_VALUE;

        Thread.sleep(3000); // let's make sure everything is written

        long totalDeleted = 0;

        // now do the deletes
        while(count != 0) {

            logger.info("total deleted: {}", totalDeleted);
            if(count != Long.MAX_VALUE) { // count starts with Long.MAX
                logger.info("deleted {} entities, continuing until count is 0", count);
            }
            //take 1000 then sleep
            count = generator.doSearch( manager ).take( 1000 )
                .filter(markedEdge -> {

                    // if it's already been marked let's filter, move on as async deleteEdge()
                    logger.trace("edge already marked, may indicated a problem with gm.deleteEdge(): {}", markedEdge);
                    return !markedEdge.isDeleted();
                })
                .flatMap( edge -> manager.markEdge( edge ))
                     .flatMap( edge -> manager.deleteEdge( edge ) ).countLong().toBlocking().last();

            totalDeleted += count;
            Thread.sleep( 500 );
        }


        // loop with a reader until our shards are gone


        /**
         * Start reading continuously while we migrate data to ensure our view is always correct
         */
        final ListenableFuture<Long> future = deleteExecutor.submit( new ReadWorker( gmf, generator, 0, readMeter ) );

        final List<Throwable> failures = new ArrayList<>();


        //add the future
        Futures.addCallback( future, new FutureCallback<Long>() {

            @Override
            public void onSuccess( @Nullable final Long result ) {
                logger.info( "Successfully ran the read, re-running" );
                deleteExecutor.submit( new ReadWorker( gmf, generator, 0, readMeter ) );
            }


            @Override
            public void onFailure( final Throwable t ) {
                failures.add( t );
                logger.error( "Failed test!", t );
            }
        } );

        Thread.sleep(3000); //  let the edge readers start

        // now loop check the shard count
        while ( true ) {

            if ( !failures.isEmpty() ) {

                StringBuilder builder = new StringBuilder();

                builder.append( "Read runner failed!\n" );

                for ( Throwable t : failures ) {
                    builder.append( "Exception is: " );
                    ByteArrayOutputStream output = new ByteArrayOutputStream();

                    t.printStackTrace( new PrintWriter( output ) );

                    builder.append( output.toString( "UTF-8" ) );
                    builder.append( "\n\n" );
                }


                fail( builder.toString() );
            }

            //reset our count.  Ultimately we'll have 4 groups once our compaction completes
            shardCount = 0;

            //we have to get it from the cache, because this will trigger the compaction process
            final Iterator<ShardEntryGroup> groups = cache.getReadShardGroup( scope, Long.MAX_VALUE, directedEdgeMeta );

            ShardEntryGroup group = null;

            while ( groups.hasNext() ) {

                group = groups.next();

                logger.info( "Shard size for group is {}", group.getReadShards() );

                shardCount += group.getReadShards().size();
            }


            // we're done, 1 shard remains, we have a group, and it's our default shard
            if ( shardCount == 1 && group.getMinShard().getShardIndex() == Shard.MIN_SHARD.getShardIndex()  ) {
                logger.info( "All compactions complete," );

                break;
            }


            Thread.sleep( 2000 );
        }

        //now that we have finished deleting and shards are removed, shutdown
        deleteExecutor.shutdownNow();

        Thread.sleep( 3000 ); // sleep before the next test
    }


    private class Worker implements Callable<Boolean> {
        private final GraphManagerFactory factory;
        private final EdgeGenerator generator;
        private final long writeLimit;
        private final long minExecutionTime;
        private final AtomicLong writeCounter;


        private Worker( final GraphManagerFactory factory, final EdgeGenerator generator, final long writeLimit,
                        final long minExecutionTime, final AtomicLong writeCounter ) {
            this.factory = factory;
            this.generator = generator;
            this.writeLimit = writeLimit;
            this.minExecutionTime = minExecutionTime;
            this.writeCounter = writeCounter;
        }


        @Override
        public Boolean call() throws Exception {
            GraphManager manager = factory.createEdgeManager( scope );


            final long startTime = System.currentTimeMillis();


            for ( long i = 1; i < writeLimit +1 && System.currentTimeMillis() - startTime < minExecutionTime; i++ ) {

                Edge edge = generator.newEdge();

                Edge returned = manager.writeEdge( edge ).toBlocking().last();


                assertNotNull( "Returned has a version", returned.getTimestamp() );


                writeMeter.mark();

                writeCounter.incrementAndGet();


                if ( i % 100 == 0 ) {
                    logger.info( "wrote: " + i );
                }
            }


            return true;
        }
    }


    private class ReadWorker implements Callable<Long> {
        private final GraphManagerFactory factory;
        private final EdgeGenerator generator;
        private final long writeCount;
        private final Meter readMeter;


        private ReadWorker( final GraphManagerFactory factory, final EdgeGenerator generator, final long writeCount,
                            final Meter readMeter ) {
            this.factory = factory;
            this.generator = generator;
            this.writeCount = writeCount;
            this.readMeter = readMeter;
        }


        @Override
        public Long call() throws Exception {


            GraphManager gm = factory.createEdgeManager( scope );


            while ( true ) {


                //do a read to eventually trigger our group compaction. Take 2 pages of columns
                final long returnedEdgeCount = generator.doSearch( gm )

                                                        .doOnNext( edge -> readMeter.mark() )

                                                        .countLong().toBlocking().last();

                logger.info( "Completed reading {} edges", returnedEdgeCount );

                if ( writeCount != returnedEdgeCount ) {
                    logger.warn( "Unexpected edge count returned!!!  Expected {} but was {}", writeCount,
                        returnedEdgeCount );
                }

                assertEquals( "Expected to read same edge count", writeCount, returnedEdgeCount );
            }
        }
    }


    private interface EdgeGenerator {

        /**
         * Create a new edge to persiste
         */
        public Edge newEdge();

        /**
         * Perform the search returning an observable edge
         */
        public Observable<MarkedEdge> doSearch( final GraphManager manager );
    }
}





