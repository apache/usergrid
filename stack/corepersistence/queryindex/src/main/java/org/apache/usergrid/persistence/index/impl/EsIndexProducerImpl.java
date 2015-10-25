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


import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.codahale.metrics.Histogram;


import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.IndexFig;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;


/**
 * Consumer for IndexOperationMessages
 */
@Singleton
public class EsIndexProducerImpl implements IndexProducer {
    private static final Logger log = LoggerFactory.getLogger( EsIndexProducerImpl.class );

    private final IndexFig config;
    private final FailureMonitorImpl failureMonitor;
    private final Client client;
    private final Timer flushTimer;
    private final IndexFig indexFig;
    private final Counter indexSizeCounter;
    private final Histogram roundtripTimer;
    private final Timer indexTimer;


    private AtomicLong inFlight = new AtomicLong();


    @Inject
    public EsIndexProducerImpl(final IndexFig config, final EsProvider provider,
                               final MetricsFactory metricsFactory, final IndexFig indexFig) {
        this.flushTimer = metricsFactory.getTimer(EsIndexProducerImpl.class, "index_buffer.flush");
        this.indexSizeCounter = metricsFactory.getCounter(EsIndexProducerImpl.class, "index_buffer.size");
        this.roundtripTimer = metricsFactory.getHistogram(EsIndexProducerImpl.class, "index_buffer.message_cycle");

        //wire up the gauge of inflight messages
        metricsFactory.addGauge(EsIndexProducerImpl.class, "index_buffer.inflight", () -> inFlight.longValue());


        this.indexTimer = metricsFactory.getTimer( EsIndexProducerImpl.class, "index" );

        this.config = config;
        this.failureMonitor = new FailureMonitorImpl(config, provider);
        this.client = provider.getClient();
        this.indexFig = indexFig;


        //batch up sets of some size and send them in batch

    }

    @Override
    public Observable<IndexOperationMessage> put(EntityIndexBatch message) {
        return put(message.build());
    }

    public Observable<IndexOperationMessage>  put( IndexOperationMessage message ) {
        Preconditions.checkNotNull(message, "Message cannot be null");
        indexSizeCounter.inc(message.getDeIndexRequests().size());
        indexSizeCounter.inc(message.getIndexRequests().size());
        return  processBatch(message);
    }


    /**
     * Process the buffer of batches
     * @param batch
     * @return
     */
    private Observable<IndexOperationMessage> processBatch( final IndexOperationMessage batch ) {

        //take our stream of batches, then stream then into individual ops for consumption on ES
        final Set<IndexOperation> indexOperationSet = batch.getIndexRequests();
        final Set<DeIndexOperation> deIndexOperationSet = batch.getDeIndexRequests();

        final int indexOperationSetSize = indexOperationSet.size();
        final int deIndexOperationSetSize = deIndexOperationSet.size();

        log.debug("Emitting {} add and {} remove operations", indexOperationSetSize, deIndexOperationSetSize);

        indexSizeCounter.dec(indexOperationSetSize);
        indexSizeCounter.dec(deIndexOperationSetSize);

        final Observable<IndexOperation> index = Observable.from(batch.getIndexRequests());
        final Observable<DeIndexOperation> deIndex = Observable.from(batch.getDeIndexRequests());

        //TODO: look at indexing ordering
        final Observable<BatchOperation> batchOps = Observable.merge(index, deIndex);

        //buffer into the max size we can send ES and fire them all off until we're completed
        final Observable<BulkRequestBuilder> requests = batchOps.buffer(indexFig.getIndexBatchSize())
            //flatten the buffer into a single batch execution
            .flatMap(individualOps -> Observable.from(individualOps)
                //collect them
                .collect(() -> initRequest(), (bulkRequestBuilder, batchOperation) -> {
                    log.debug("adding operation {} to bulkRequestBuilder {}", batchOperation, bulkRequestBuilder);
                    batchOperation.doOperation(client, bulkRequestBuilder);
                }))
                //write them
            .doOnNext(bulkRequestBuilder -> sendRequest(bulkRequestBuilder));


        //now that we've processed them all, ack the futures after our last batch comes through
        final Observable<IndexOperationMessage> processedIndexOperations =
            requests.flatMap(lastRequest -> {
                if (lastRequest != null) {
                    return Observable.just(batch);
                } else {
                    return Observable.empty();
                }
            });

        //subscribe to the operations that generate requests on a new thread so that we can execute them quickly
        //mark this as done
        return processedIndexOperations.doOnNext(processedIndexOp -> {
            roundtripTimer.update(System.currentTimeMillis() - processedIndexOp.getCreationTime());
        });
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


        final Timer.Context timer = indexTimer.time();

        try {
            responses = bulkRequest.execute().actionGet( );
        } catch ( Throwable t ) {
            log.error( "Unable to communicate with elasticsearch", t );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }finally{
            timer.stop();
        }

        failureMonitor.success();

        boolean error = false;

        final StringBuilder errorString = new StringBuilder(  );

        boolean hasTooManyRequests= false;
        for ( BulkItemResponse response : responses ) {

            if ( response.isFailed() ) {
                // log error and continue processing
                log.error( "Unable to index id={}, type={}, index={}, failureMessage={} ", response.getId(),
                    response.getType(), response.getIndex(),  response.getFailureMessage() );
                //if index is overloaded on the queue fail.
                if(response.getFailure()!=null && response.getFailure().getStatus() == RestStatus.TOO_MANY_REQUESTS){
                    hasTooManyRequests =true;
                }
                error = true;

                errorString.append( response.getFailureMessage() ).append( "\n" );
            }
        }

        if ( error ) {
            if(hasTooManyRequests){
                try{
                    log.warn("Encountered Queue Capacity Exception from ElasticSearch slowing by "
                        + indexFig.getSleepTimeForQueueError() );
                    Thread.sleep(indexFig.getSleepTimeForQueueError());
                }catch (Exception e){
                    //move on
                }
            }
            throw new RuntimeException(
                "Error during processing of bulk index operations one of the responses failed. \n" + errorString);
        }
    }
}
