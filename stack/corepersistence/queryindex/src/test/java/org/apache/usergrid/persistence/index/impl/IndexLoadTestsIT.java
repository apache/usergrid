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
package org.apache.usergrid.persistence.index.impl;


import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.index.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.guice.IndexTestFig;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;


/**
 * This is configuration via the properties in the IndexTestFig object.  Most of these values you won't need to touch.
 * To run this against a live cluster.  You execute this maven command.
 *
 * <command> mvn test -Dtest=IndexLoadTestsIT#testHeavyLoadValidate -Dstresstest.numWorkers=16
 * -Dstresstest.numberOfRecords=10000 </command>
 *
 * This will insert 10000 records for each worker thread.  There will be 16 worker threads.  Validation will occur after
 * the wait timeout (stresstest.validate.wait) of 2 seconds.  Up to 40 concurrent queries (stresstest.readThreads) will
 * be executed to validate each result.
 *
 * By default this test is excluded from surefire, and will need to be run manually
 */
@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
public class IndexLoadTestsIT extends BaseIT {
    private static final Logger log = LoggerFactory.getLogger( IndexLoadTestsIT.class );
    public static final String FIELD_WORKER_INDEX = "workerIndex";
    private static final String FIELD_ORDINAL = "ordinal";
    private static final String FIELD_UNIQUE_IDENTIFIER = "uniqueIdentifier";

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    public IndexTestFig indexTestFig;

    @Inject
    public IndexProducer indexProducer;

    @Inject
    public MetricsFactory metricsFactory;

    private Meter batchWriteTPS;
    private Timer batchWriteTimer;

    private Meter queryTps;
    private Timer queryTimer;

    private Slf4jReporter reporter;

    @Inject
    public IndexFig fig;

    @Inject
    public CassandraFig cassandraFig;
    @Inject
    public EntityIndexFactory eif;

    @Inject
    @Rule
    public ElasticSearchRule elasticSearchRule;
    private SimpleId appId;
    private EntityIndex entityIndex;


    public void before(){
        appId = new SimpleId(UUID.randomUUID(), "application" );

        IndexLocationStrategy strategy = new TestIndexIdentifier(cassandraFig,fig,new ApplicationScopeImpl(appId));


        entityIndex = eif.createEntityIndex( strategy );
    }


    @Before
    public void setupIndexAndMeters() {
        final String userAppId = indexTestFig.getApplicationId();


        //if it's unset, generate one
        final String uniqueIdentifier = UUIDGenerator.newTimeUUID().toString();

        //use the appId supplied, or generate one
        final UUID applicationUUID = UUID.fromString( userAppId );

        final Id applicationId = new SimpleId( applicationUUID, "application" );


        batchWriteTPS = metricsFactory.getMeter( IndexLoadTestsIT.class, "batch.write_tps" );

        batchWriteTimer = metricsFactory.getTimer( IndexLoadTestsIT.class, "batch.write" );

        queryTps = metricsFactory.getMeter( IndexLoadTestsIT.class, "query.tps" );

        queryTimer = metricsFactory.getTimer( IndexLoadTestsIT.class, "query.test" );

        reporter =
            Slf4jReporter.forRegistry( metricsFactory.getRegistry() ).outputTo( log ).convertRatesTo( TimeUnit.SECONDS )
                         .convertDurationsTo( TimeUnit.MILLISECONDS ).build();

        reporter.start( 30, TimeUnit.SECONDS );
    }


    @After
    public void printMetricsBeforeShutdown() {
        //stop the log reporter and print the last report
        reporter.stop();
        reporter.report();
    }


    /**
     * Perform the following 1, spin up the specified number of workers For each worker, insert the specified number of
     * elements
     *
     * Wait the wait time after buffer execution before beginning validate
     *
     * Validate every entity inserted is returned by a search.
     */
    @Test
    public void testHeavyLoadValidate() {
        final String userAppId = indexTestFig.getApplicationId();


        //if it's unset, generate one
        final String uniqueIdentifier = UUIDGenerator.newTimeUUID().toString();

        //use the appId supplied, or generate one
        final UUID applicationUUID = UUID.fromString( userAppId );

        final Id applicationId = new SimpleId( applicationUUID, "application" );
        final ApplicationScope scope = new ApplicationScopeImpl( applicationId );


        final SearchEdge searchEdge = new SearchEdgeImpl( applicationId, "test",  SearchEdge.NodeType.SOURCE );



        //create our index if it doesn't exist

        //delay our verification for indexing to happen
        final Observable<DataLoadResult> dataLoadResults =
            createStreamFromWorkers( searchEdge, uniqueIdentifier ).buffer( indexTestFig.getBufferSize() )
                //perform a delay to let ES index from our batches
                .delay( indexTestFig.getValidateWait(), TimeUnit.MILLISECONDS )
                    //do our search in parallel, otherwise this test will take far too long
                .flatMap( entitiesToValidate -> {
                    return Observable.from( entitiesToValidate ).map( thisentityObservable -> {
                        Entity entityObservable = (Entity) thisentityObservable;

                        final int workerIndex = ( int ) entityObservable.getField( FIELD_WORKER_INDEX ).getValue();
                        final int ordinal = ( int ) entityObservable.getField( FIELD_ORDINAL ).getValue();


                        final Timer.Context queryTimerContext = queryTimer.time();


                        //execute our search
                        final CandidateResults results = entityIndex
                            .search( searchEdge, SearchTypes.fromTypes( searchEdge.getEdgeName() ),
                                "select * where " + FIELD_WORKER_INDEX + "  = " + workerIndex + " AND " + FIELD_ORDINAL
                                    + " = " + ordinal + " AND " + FIELD_UNIQUE_IDENTIFIER + " = '" + uniqueIdentifier
                                    + "'" , 100 , 0);

                        queryTps.mark();
                        queryTimerContext.stop();

                        boolean found;

                        if ( !results.isEmpty() && results.get( 0 ).getId().equals( entityObservable.getId() ) ) {
                            found = true;
                        }
                        else {
                            found = false;
                        }

                        return new EntitySearchResult( entityObservable, found );
                    } ).subscribeOn( Schedulers.io() );
                }, indexTestFig.getConcurrentReadThreads() )

                    //collect all the results into a single data load result
                .collect( () -> new DataLoadResult(), ( dataloadResult, entitySearchResult ) -> {
                    if ( entitySearchResult.found ) {
                        dataloadResult.success();
                        return;
                    }

                    final int ordinal = ( int ) entitySearchResult.searched.getField( FIELD_ORDINAL ).getValue();
                    final int worker = ( int ) entitySearchResult.searched.getField( FIELD_WORKER_INDEX ).getValue();

                    dataloadResult.failed();

                    log.error(
                        "Could not find entity with worker {}, ordinal {}, and Id {} after waiting {} milliseconds",
                        worker, ordinal, entitySearchResult.searched.getId(), indexTestFig.getValidateWait() );
                } );


        //wait for processing to finish
        final DataLoadResult result = dataLoadResults.toBlocking().last();

        final long expectedCount = indexTestFig.getNumberOfRecords() * indexTestFig.getNumberOfWorkers();

        assertEquals( "Excepted to have no failures", 0, result.getFailCount() );

        assertEquals( "Excepted to find all records", expectedCount, result.getSuccessCount() );
    }


    public Observable<Entity> createStreamFromWorkers(  final SearchEdge indexEdge,
                                                       final String uniqueIdentifier ) {

        //create a sequence of observables.  Each index will be it's own worker thread using the Schedulers.newthread()
        return Observable.range( 0, indexTestFig.getNumberOfWorkers() ).flatMap(
            integer -> createWriteObservable(  indexEdge, uniqueIdentifier, integer )
                .subscribeOn( Schedulers.newThread() ) );
    }


    private Observable<Entity> createWriteObservable(  final SearchEdge indexEdge,
                                                      final String uniqueIdentifier, final int workerIndex ) {


        return Observable.range( 0, indexTestFig.getNumberOfRecords() )

            //create our entity
            .map( integer -> {
                final Entity entity = new Entity( indexEdge.getEdgeName() );

                entity.setField( new IntegerField( FIELD_WORKER_INDEX, workerIndex ) );
                entity.setField( new IntegerField( FIELD_ORDINAL, integer ) );
                entity.setField( new StringField( FIELD_UNIQUE_IDENTIFIER, uniqueIdentifier ) );


                EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );

                //add some fields for indexing

                entity.setField( new StringField( "emtpyField", "" ) );
                entity.setField( new StringField( "singleCharField1", "L" ) );
                entity.setField( new StringField( "longStringField", "000000000000001051" ) );
                entity.setField( new StringField( "singleCharField2", "0" ) );
                entity.setField( new StringField( "singleCharField3", "0" ) );
                entity.setField( new StringField( "singleCharField4", "0" ) );
                entity.setField( new StringField( "dept", "VALUE" ) );
                entity.setField( new StringField( "description", "I'm a longer description" ) );

                ArrayField<Long> array = new ArrayField<>("longs");

                array.add( 9315321008910l );
                array.add( 9315321009016l );
                array.add( 9315321009115l );
                array.add( 9315321009313l );
                array.add( 9315321009320l );
                array.add( 9315321984955l );

                entity.setField( array );

                entity.setField( new StringField( "singleCharField5", "N" ) );
                entity.setField( new BooleanField( "booleanField1", true ) );
                entity.setField( new BooleanField( "booleanField2", false ) );
                entity.setField( new StringField( "singleCharField5", "N" ) );
                entity.setField( new StringField( "singleCharField6", "N" ) );
                entity.setField( new StringField( "stringField", "ALL CAPS)); I MEAN IT" ) );
                entity.setField( new DoubleField( "doubleField1", 750.0 ) );
                entity.setField( new StringField( "charField", "AB" ) );
                entity.setField( new StringField( "name", "000000000000001051-1004" ) );


                return entity;
            } )
                //buffer up a batch size
            .buffer( indexTestFig.getBufferSize() ).doOnNext( entities -> {


               AtomicLong edgeCounter = new AtomicLong(  );

                //take our entities and roll them into a batch
                Observable.from( entities ).collect( () -> entityIndex.createBatch(), ( entityIndexBatch, entity ) -> {
                    IndexEdge edge = new IndexEdgeImpl( indexEdge.getNodeId(), indexEdge.getEdgeName(),
                            SearchEdge.NodeType.SOURCE, edgeCounter.incrementAndGet()  );
                    entityIndexBatch.index( edge, entity );
                } ).doOnNext( entityIndexBatch -> {
                    log.info( "Indexing next {} in batch", entityIndexBatch.size() );
                    //gather the metrics
                    final Timer.Context time = batchWriteTimer.time();
                    batchWriteTPS.mark();


                    //execute
                    IndexOperationMessage message = entityIndexBatch.build();
                    indexProducer.put(message);
                    //stop
                    time.close();
                } ).toBlocking().last();
            } )
                //translate back into a stream of entities for the caller to use
            .flatMap( entities -> Observable.from( entities ) );
    }


    /**
     * Class for entity search results
     */
    private static class EntitySearchResult {

        public final Entity searched;
        public final boolean found;


        private EntitySearchResult( final Entity searched, final boolean found ) {
            this.searched = searched;
            this.found = found;
        }
    }


    /**
     * Class for collecting results
     */
    private static final class DataLoadResult {
        private final AtomicLong successCount = new AtomicLong( 0 );
        private final AtomicLong failCount = new AtomicLong( 0 );


        public void success() {
            successCount.addAndGet( 1 );
        }


        public long getSuccessCount() {
            return successCount.get();
        }


        public void failed() {
            failCount.addAndGet( 1 );
        }


        public long getFailCount() {
            return failCount.get();
        }
    }
}
