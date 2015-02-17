/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.index.impl;

import com.google.inject.Singleton;
import org.apache.usergrid.persistence.index.IndexBatchBuffer;
import org.apache.usergrid.persistence.index.IndexFig;
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
import rx.Subscriber;
import rx.functions.Action1;

import java.util.List;


/**
 * Classy class class.
 */
@Singleton
public class IndexBatchBufferImpl implements IndexBatchBuffer {

    private static final Logger log = LoggerFactory.getLogger(IndexBatchBufferImpl.class);
    private final Client client;
    private final FailureMonitor failureMonitor;
    private final IndexFig config;
    private final boolean refresh;
    private BulkRequestBuilder bulkRequest;
    private Producer producer;

    public IndexBatchBufferImpl(final Client client, FailureMonitor failureMonitor,final IndexFig config){
        this.client = client;
        this.failureMonitor = failureMonitor;
        this.config = config;
        this.producer = new Producer();
        this.refresh = config.isForcedRefresh();
        init();
    }

    private void clearBulk() {
        bulkRequest = client.prepareBulk();
    }

    private void init() {
        clearBulk();
        Observable.create(producer)
                .buffer(10)
                .doOnNext(new Action1<List<RequestBuilderContainer>>() {
                    @Override
                    public void call(List<RequestBuilderContainer> builderContainerList) {
                        System.out.println("test test!!!");
                        for (RequestBuilderContainer container : builderContainerList) {
                            ShardReplicationOperationRequestBuilder builder = container.getBuilder();
                            if (builder instanceof IndexRequestBuilder) {
                                bulkRequest.add((IndexRequestBuilder) builder);
                                continue;
                            }
                            if (builder instanceof DeleteRequestBuilder) {
                                bulkRequest.add((DeleteRequestBuilder)builder);
                                continue;
                            }
                        }
                        execute();
                    }
                })
                .subscribe();
    }

    public void put(IndexRequestBuilder builder){
        producer.put(new RequestBuilderContainer(builder));
    }

    public void put(DeleteRequestBuilder builder){
        producer.put(new RequestBuilderContainer(builder));
    }

    public void flushAndRefresh(){
        execute(bulkRequest.setRefresh(true));
    }
    public void flush(){
        execute();
    }

    private void execute(){
        execute(bulkRequest.setRefresh(refresh));
    }

    /**
     * Execute the request, check for errors, then re-init the batch for future use
     */
    private void execute( final BulkRequestBuilder request ) {
        //nothing to do, we haven't added anthing to the index
        if ( request.numberOfActions() == 0 ) {
            return;
        }

        final BulkResponse responses;

        try {
            responses = request.execute().actionGet();
        }
        catch ( Throwable t ) {
            log.error( "Unable to communicate with elasticsearch" );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }

        failureMonitor.success();

        for ( BulkItemResponse response : responses ) {
            if ( response.isFailed() ) {
                throw new RuntimeException( "Unable to index documents.  Errors are :"
                        + response.getFailure().getMessage() );
            }
        }

        clearBulk();
    }

    private static class Producer implements Observable.OnSubscribe<RequestBuilderContainer> {

        private Subscriber<? super RequestBuilderContainer> subscriber;

        @Override
        public void call(Subscriber<? super RequestBuilderContainer> subscriber) {
            this.subscriber = subscriber;
        }

        public void put(RequestBuilderContainer r){
            subscriber.onNext(r);
        }
    }

    private static class RequestBuilderContainer{
        private final ShardReplicationOperationRequestBuilder builder;

        public RequestBuilderContainer(ShardReplicationOperationRequestBuilder builder){
            this.builder = builder;
        }

        public ShardReplicationOperationRequestBuilder getBuilder(){
            return builder;
        }
    }

}
