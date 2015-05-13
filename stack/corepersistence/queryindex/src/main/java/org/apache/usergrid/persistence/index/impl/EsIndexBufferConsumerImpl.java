/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index.impl;


import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.future.FutureObservable;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.IndexFig;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;


/**
 * Consumer for IndexOperationMessages
 */
@Singleton
public class EsIndexBufferConsumerImpl implements IndexBufferConsumer {
    private static final Logger log = LoggerFactory.getLogger( EsIndexBufferConsumerImpl.class );

    private final IndexFig config;
    private final FailureMonitorImpl failureMonitor;
    private final Client client;

    private final Timer flushTimer;
    private final Counter indexErrorCounter;
    private final Meter flushMeter;
    private final Timer produceTimer;
    private final IndexFig indexFig;
    private final AtomicLong counter = new AtomicLong();


    private final Counter indexSizeCounter;

    private final Timer offerTimer;


    private final BufferProducer bufferProducer;


    private AtomicLong inFlight = new AtomicLong();


    @Inject
    public EsIndexBufferConsumerImpl( final IndexFig config, final EsProvider provider,
                                      final MetricsFactory metricsFactory, final IndexFig indexFig ) {

        this.flushTimer = metricsFactory.getTimer( EsIndexBufferConsumerImpl.class, "buffer.flush" );
        this.flushMeter = metricsFactory.getMeter( EsIndexBufferConsumerImpl.class, "buffer.meter" );
        this.indexSizeCounter = metricsFactory.getCounter( EsIndexBufferConsumerImpl.class, "buffer.size" );
        this.indexErrorCounter = metricsFactory.getCounter( EsIndexBufferConsumerImpl.class, "error.count" );
        this.offerTimer = metricsFactory.getTimer( EsIndexBufferConsumerImpl.class, "index.buffer.producer.timer" );

        //wire up the gauge of inflight messages
        metricsFactory.addGauge( EsIndexBufferConsumerImpl.class, "inflight.meter", () -> inFlight.longValue() );


        this.config = config;
        this.failureMonitor = new FailureMonitorImpl( config, provider );
        this.client = provider.getClient();
        this.produceTimer =
            metricsFactory.getTimer( EsIndexBufferConsumerImpl.class, "index.buffer.consumer.messageFetch" );
        this.indexFig = indexFig;

        this.bufferProducer = new BufferProducer();

        //batch up sets of some size and send them in batch

        startSubscription();
    }


    public Observable<IndexOperationMessage>  put( IndexOperationMessage message ) {
        Preconditions.checkNotNull(message, "Message cannot be null");
        indexSizeCounter.inc( message.getDeIndexRequests().size() );
        indexSizeCounter.inc( message.getIndexRequests().size() );
        Timer.Context time = offerTimer.time();
        bufferProducer.send( message );
        time.stop();
        return message.observable();
    }


    /**
     * Start the subscription
     */
    private void startSubscription() {


        final Observable<IndexOperationMessage> observable = Observable.create(bufferProducer);

        //buffer on our new thread with a timeout
        observable.buffer( indexFig.getIndexBufferTimeout(), TimeUnit.MILLISECONDS, indexFig.getIndexBufferSize(),
            Schedulers.io() ).flatMap( indexOpBuffer -> {

            //hand off to processor in new observable thread so we can continue to buffer faster
            return Observable.just( indexOpBuffer ).flatMap(
                indexOpBufferObservable -> processBatch( indexOpBufferObservable ) )

                //use the I/O scheduler for thread re-use and efficiency in context switching then use our concurrent
                // flatmap count or higher throughput of batches once buffered
                .subscribeOn( Schedulers.io() );
        }, indexFig.getIndexFlushWorkerCount() )
            //start in the background
            .subscribe();
    }


    /**
     * Process the buffer of batches
     * @param batches
     * @return
     */
    private Observable<IndexOperationMessage> processBatch( final List<IndexOperationMessage> batches ) {


        final Observable<IndexOperationMessage> indexOps = Observable.from( batches );

        //take our stream of batches, then stream then into individual ops for consumption on ES
        final Observable<BatchOperation> batchOps = indexOps.flatMap( batch -> {

            final Set<IndexOperation> indexOperationSet = batch.getIndexRequests();
            final Set<DeIndexOperation> deIndexOperationSet = batch.getDeIndexRequests();

            final int indexOperationSetSize = indexOperationSet.size();
            final int deIndexOperationSetSize = deIndexOperationSet.size();

            log.debug( "Emitting {} add and {} remove operations", indexOperationSetSize, deIndexOperationSetSize );

            indexSizeCounter.dec( indexOperationSetSize );
            indexSizeCounter.dec( deIndexOperationSetSize );

            final Observable<IndexOperation> index = Observable.from( batch.getIndexRequests() );
            final Observable<DeIndexOperation> deIndex = Observable.from( batch.getDeIndexRequests() );

            return Observable.merge( index, deIndex );
        } );

        //buffer into the max size we can send ES and fire them all off until we're completed
        final Observable<BulkRequestBuilder> requests = batchOps.buffer( indexFig.getIndexBatchSize() )
            //flatten the buffer into a single batch execution
            .flatMap( individualOps -> Observable.from( individualOps )
                //collect them
                .collect( () -> initRequest(), ( bulkRequestBuilder, batchOperation ) -> {
                    log.debug( "adding operation {} to bulkRequestBuilder {}", batchOperation, bulkRequestBuilder );
                    batchOperation.doOperation( client, bulkRequestBuilder );
                } ) )
                //write them
            .doOnNext( bulkRequestBuilder -> sendRequest( bulkRequestBuilder ) ).doOnError( t -> log.error( "Unable to process batches", t ) );


        //now that we've processed them all, ack the futures after our last batch comes through
        final Observable<IndexOperationMessage> processedIndexOperations =
            requests.lastOrDefault(null).flatMap( lastRequest ->{
                if(lastRequest!=null){
                    return Observable.from( batches ) ;
                }else{
                    return Observable.empty();
                }
            });

        //subscribe to the operations that generate requests on a new thread so that we can execute them quickly
        //mark this as done
        return processedIndexOperations.doOnNext( processedIndexOp -> processedIndexOp.done()
        ).doOnError(t -> log.error("Unable to ack futures", t) );
    }


    /*

    /**
     * initialize request
     */
    private BulkRequestBuilder initRequest() {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        bulkRequest.setConsistencyLevel( WriteConsistencyLevel.fromString( config.getWriteConsistencyLevel() ) );
        bulkRequest.setRefresh( config.isForcedRefresh() );
        return bulkRequest;
    }


    /**
     * send bulk request
     */
    private void sendRequest( BulkRequestBuilder bulkRequest ) {
        //nothing to do, we haven't added anything to the index
        if ( bulkRequest.numberOfActions() == 0 ) {
            return;
        }

        final BulkResponse responses;


        try {
            responses = bulkRequest.execute().actionGet( indexFig.getWriteTimeout() );
        }
        catch ( Throwable t ) {
            log.error( "Unable to communicate with elasticsearch" );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }

        failureMonitor.success();

        boolean error = false;

        for ( BulkItemResponse response : responses ) {

            if ( response.isFailed() ) {
                // log error and continue processing
                log.error( "Unable to index id={}, type={}, index={}, failureMessage={} ", response.getId(),
                    response.getType(), response.getIndex(), response.getFailureMessage() );

                error = true;
            }
        }

        if ( error ) {
            throw new RuntimeException(
                "Error during processing of bulk index operations one of the responses failed.  Check previous log "
                    + "entries" );
        }
    }


    public static class BufferProducer implements Observable.OnSubscribe<IndexOperationMessage> {

        private Subscriber<? super IndexOperationMessage> subscriber;


        /**
         * Send the data through the buffer
         */
        public void send( final IndexOperationMessage indexOp ) {
            subscriber.onNext( indexOp );
        }


        @Override
        public void call( final Subscriber<? super IndexOperationMessage> subscriber ) {
            //just assigns for later use, doesn't do anything else
            this.subscriber = subscriber;
        }
    }
}
