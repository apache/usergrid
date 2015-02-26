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
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.IndexBufferConsumer;
import org.apache.usergrid.persistence.index.IndexBufferProducer;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexOperationMessage;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.replication.ShardReplicationOperationRequestBuilder;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Consumer for IndexOperationMessages
 */
@Singleton
public class EsIndexBufferConsumerImpl implements IndexBufferConsumer {
    private static final Logger log = LoggerFactory.getLogger(EsIndexBufferConsumerImpl.class);

    private final IndexFig config;
    private final FailureMonitorImpl failureMonitor;
    private final Client client;
    private final Observable<List<IndexOperationMessage>> consumer;
    private final Timer flushTimer;
    private final Counter indexSizeCounter;

    @Inject
    public EsIndexBufferConsumerImpl(final IndexFig config, final IndexBufferProducer producer, final EsProvider provider, final MetricsFactory metricsFactory){
        this.flushTimer = metricsFactory.getTimer(EsIndexBufferConsumerImpl.class, "index.buffer.flush");
        this.indexSizeCounter =  metricsFactory.getCounter(EsIndexBufferConsumerImpl.class, "index.buffer.size");
        this.config = config;
        this.failureMonitor = new FailureMonitorImpl(config,provider);
        this.client = provider.getClient();

        //batch up sets of some size and send them in batch
        this.consumer = Observable.create(producer)
            .subscribeOn(Schedulers.io())
            .buffer(config.getIndexBufferTimeout(), TimeUnit.MILLISECONDS, config.getIndexBufferSize())
            .doOnNext(new Action1<List<IndexOperationMessage>>() {
                @Override
                public void call(List<IndexOperationMessage> containerList) {
                    flushTimer.time();
                    if (containerList.size() > 0) {
                        execute(containerList);
                    }
                }
            });
        consumer.subscribe();
    }

    /**
     * Execute the request, check for errors, then re-init the batch for future use
     */
    private void execute(final List<IndexOperationMessage> operationMessages) {

        if (operationMessages == null || operationMessages.size() == 0) {
            return;
        }

        //process and flatten all the messages to builder requests
        Observable<ShardReplicationOperationRequestBuilder> flattenMessages = Observable.from(operationMessages)
            .subscribeOn(Schedulers.io())
            .flatMap(new Func1<IndexOperationMessage, Observable<ShardReplicationOperationRequestBuilder>>() {
                @Override
                public Observable<ShardReplicationOperationRequestBuilder> call(IndexOperationMessage operationMessage) {
                    return Observable.from(operationMessage.getOperations());
                }
            });

        //batch shard operations into a bulk request
        flattenMessages
            .buffer(config.getIndexBatchSize())
            .doOnNext(new Action1<List<ShardReplicationOperationRequestBuilder>>() {
                @Override
                public void call(List<ShardReplicationOperationRequestBuilder> builders) {
                    try {
                        final BulkRequestBuilder bulkRequest = initRequest();
                        for (ShardReplicationOperationRequestBuilder builder : builders) {
                            indexSizeCounter.dec();
                            if (builder instanceof IndexRequestBuilder) {
                                bulkRequest.add((IndexRequestBuilder) builder);
                            }
                            if (builder instanceof DeleteRequestBuilder) {
                                bulkRequest.add((DeleteRequestBuilder) builder);
                            }
                        }
                        sendRequest(bulkRequest);
                    }catch (Exception e){
                        log.error("Failed while sending bulk",e);
                    }
                }
            })
            .toBlocking().lastOrDefault(null);

        //call back all futures
        Observable.from(operationMessages)
            .subscribeOn(Schedulers.io())
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
