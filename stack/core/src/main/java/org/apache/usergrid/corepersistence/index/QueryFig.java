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
package org.apache.usergrid.corepersistence.index;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Application id cache fig
 */
public interface QueryFig extends GuicyFig {


    /**
     * Amount of time in milliseconds to wait when ES rejects our request before retrying.  Provides simple
     * backpressure
     */
    public static final String FAILURE_REJECTED_RETRY_WAIT_TIME = "elasticsearch.rejected_retry_wait";

    /**
     * The number of worker threads to consume from the queue
     */
    public static final String ELASTICSEARCH_WORKER_COUNT = "elasticsearch.worker_count";

    /**
     * The queue implementation to use.  Values come from <class>QueueProvider.Implementations</class>
     */
    public static final String ELASTICSEARCH_QUEUE_IMPL = "elasticsearch.queue_impl";


    /**
     * The queue implementation to use.  Values come from <class>QueueProvider.Implementations</class>
     */
    public static final String ELASTICSEARCH_QUEUE_OFFER_TIMEOUT = "elasticsearch.queue.offer_timeout";

    /**
     * Amount of time to wait when reading from the queue
     */
    public static final String INDEX_QUEUE_READ_TIMEOUT = "elasticsearch.queue_read_timeout";

    /**
     * Amount of time to wait when reading from the queue in milliseconds
     */
    public static final String INDEX_QUEUE_TRANSACTION_TIMEOUT = "elasticsearch.queue_transaction_timeout";


    String INDEX_QUEUE_SIZE = "elasticsearch.queue_size";


    @Default( "1000" )
    @Key( FAILURE_REJECTED_RETRY_WAIT_TIME )
    long getFailureRetryTime();

    //give us 60 seconds to process the message
    @Default( "60" )
    @Key( INDEX_QUEUE_READ_TIMEOUT )
    int getIndexQueueTimeout();

    @Default( "2" )
    @Key( ELASTICSEARCH_WORKER_COUNT )
    int getWorkerCount();

    @Default( "LOCAL" )
    @Key( ELASTICSEARCH_QUEUE_IMPL )
    String getQueueImplementation();

    @Default( "1000" )
    @Key( ELASTICSEARCH_QUEUE_OFFER_TIMEOUT )
    long getQueueOfferTimeout();

    /**
     * size of the buffer to build up before you send results
     */
    @Default( "1000" )
    @Key( INDEX_QUEUE_SIZE )
    int getIndexQueueSize();


    @Default("30000")
    @Key("elasticsearch.reindex.sample.interval")
    long getReIndexSampleInterval();


}
