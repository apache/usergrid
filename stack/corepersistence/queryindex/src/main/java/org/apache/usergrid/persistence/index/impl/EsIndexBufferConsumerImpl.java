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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.IndexBufferConsumer;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexOperationMessage;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumer for IndexOperationMessages
 */
@Singleton
public class EsIndexBufferConsumerImpl implements IndexBufferConsumer {
    private static final Logger log = LoggerFactory.getLogger(EsIndexBufferConsumerImpl.class);

    private final IndexFig config;
    private final FailureMonitorImpl failureMonitor;
    private final Client client;

    private final Timer flushTimer;
    private final Counter indexSizeCounter;
    private final Meter flushMeter;
    private final Timer produceTimer;
    private final BufferQueue bufferQueue;

    //the actively running subscription
    private Subscription subscription;

    private  Observable<List<IndexOperationMessage>> consumer;

    private Object mutex = new Object();

    @Inject
    public EsIndexBufferConsumerImpl( final IndexFig config,  final EsProvider
        provider, final MetricsFactory metricsFactory,   final BufferQueue bufferQueue ){
        this.flushTimer = metricsFactory.getTimer(EsIndexBufferConsumerImpl.class, "index.buffer.flush");
        this.flushMeter = metricsFactory.getMeter(EsIndexBufferConsumerImpl.class, "index.buffer.meter");
        this.indexSizeCounter =  metricsFactory.getCounter(EsIndexBufferConsumerImpl.class, "index.buffer.size");
        this.config = config;
        this.failureMonitor = new FailureMonitorImpl(config,provider);
        this.client = provider.getClient();
        this.produceTimer = metricsFactory.getTimer(EsIndexBufferConsumerImpl.class,"index.buffer.consumer.messageFetch");
        this.bufferQueue = bufferQueue;



        //batch up sets of some size and send them in batch
          start();
    }


    public void start() {
        synchronized ( mutex) {

            final AtomicInteger countFail = new AtomicInteger();

            Observable<List<IndexOperationMessage>> consumer = Observable.create( new Observable.OnSubscribe<List<IndexOperationMessage>>() {
                @Override
                public void call( final Subscriber<? super List<IndexOperationMessage>> subscriber ) {

                    //name our thread so it's easy to see
                    Thread.currentThread().setName( "QueueConsumer_" + Thread.currentThread().getId() );

                    List<IndexOperationMessage> drainList;
                    do {
                        try {


                            Timer.Context timer = produceTimer.time();

                            drainList = bufferQueue.take( config.getIndexBufferSize(), config.getIndexBufferTimeout(),
                                TimeUnit.MILLISECONDS );


                            subscriber.onNext( drainList );


                            timer.stop();

                            countFail.set( 0 );
                        }
                        catch ( EsRejectedExecutionException err ) {
                            countFail.incrementAndGet();
                            log.error(
                                "Elasticsearch rejected our request, sleeping for {} milliseconds before retrying.  " + "Failed {} consecutive times", config.getFailRefreshCount(),
                                countFail.get() );

                            //es  rejected the exception, sleep and retry in the queue
                            try {
                                Thread.sleep( config.getFailureRetryTime() );
                            }
                            catch ( InterruptedException e ) {
                                //swallow
                            }
                        }
                        catch ( Exception e ) {
                            int count = countFail.incrementAndGet();
                            log.error( "failed to dequeue", e );
                            if ( count > 200 ) {
                                log.error( "Shutting down index drain due to repetitive failures" );
                            }
                        }
                    }
                    while ( true );
                }
            } ).subscribeOn( Schedulers.newThread() ).doOnNext( new Action1<List<IndexOperationMessage>>() {
                @Override
                public void call( List<IndexOperationMessage> containerList ) {
                    if ( containerList.size() > 0 ) {
                        flushMeter.mark( containerList.size() );
                        Timer.Context time = flushTimer.time();
                        execute( containerList );
                        time.stop();
                    }
                }
            } )
                //ack after we process
                .doOnNext( new Action1<List<IndexOperationMessage>>() {
                    @Override
                    public void call( final List<IndexOperationMessage> indexOperationMessages ) {
                        bufferQueue.ack( indexOperationMessages );
                    }
                } );

            //start in the background
            subscription = consumer.subscribe();
        }
    }


    public void stop() {
        synchronized ( mutex ) {
            //stop consuming
            if(subscription != null) {
                subscription.unsubscribe();
            }
        }
    }

    /**
     * Execute the request, check for errors, then re-init the batch for future use
     */
    private void execute(final List<IndexOperationMessage> operationMessages) {

        if (operationMessages == null || operationMessages.size() == 0) {
            return;
        }

        //process and flatten all the messages to builder requests
        Observable<IndexOperationMessage> flattenMessages = Observable.from( operationMessages );


        //batch shard operations into a bulk request
        flattenMessages.flatMap( new Func1<IndexOperationMessage, Observable<BatchRequest>>() {
            @Override
            public Observable<BatchRequest> call( final IndexOperationMessage indexOperationMessage ) {
                final Observable<IndexRequest> index = Observable.from( indexOperationMessage.getIndexRequests() );
                final Observable<DeIndexRequest> deIndex = Observable.from( indexOperationMessage.getDeIndexRequests() );

                return Observable.merge( index, deIndex );
            }
        } )
      //collection all the operations into a single stream
       .collect( initRequest(), new Action2<BulkRequestBuilder, BatchRequest>() {
           @Override
           public void call( final BulkRequestBuilder bulkRequestBuilder, final BatchRequest batchRequest ) {
               batchRequest.doOperation( client, bulkRequestBuilder );
           }
       } )
        //send the request off to ES
        .doOnNext( new Action1<BulkRequestBuilder>() {
            @Override
            public void call( final BulkRequestBuilder bulkRequestBuilder ) {
                sendRequest( bulkRequestBuilder );
            }
        } ).toBlocking().last();

        //call back all futures
        Observable.from(operationMessages)
            .doOnNext(new Action1<IndexOperationMessage>() {
                @Override
                public void call(IndexOperationMessage operationMessage) {
                    operationMessage.getFuture().done();
                }
            })
            .toBlocking().lastOrDefault(null);
    }


    /**
     * initialize request
     * @return
     */
    private BulkRequestBuilder initRequest() {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        bulkRequest.setConsistencyLevel(WriteConsistencyLevel.fromString(config.getWriteConsistencyLevel()));
        bulkRequest.setRefresh(config.isForcedRefresh());
        return bulkRequest;
    }

    /**
     * send bulk request
     * @param bulkRequest
     */
    private void sendRequest(BulkRequestBuilder bulkRequest) {
        //nothing to do, we haven't added anthing to the index
        if (bulkRequest.numberOfActions() == 0) {
            return;
        }

        final BulkResponse responses;

        try {
            responses = bulkRequest.execute().actionGet();
        } catch (Throwable t) {
            log.error("Unable to communicate with elasticsearch");
            failureMonitor.fail("Unable to execute batch", t);
            throw t;
        }

        failureMonitor.success();

        for (BulkItemResponse response : responses) {
            if (response.isFailed()) {
                throw new RuntimeException("Unable to index documents.  Errors are :"
                    + response.getFailure().getMessage());
            }
        }
    }
}
