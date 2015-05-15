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
public interface IndexProcessorFig extends GuicyFig {


    /**
     * Amount of time in milliseconds to wait when ES rejects our request before retrying.  Provides simple
     * backpressure
     */
    String FAILURE_REJECTED_RETRY_WAIT_TIME = "elasticsearch.rejected_retry_wait";

    /**
     * The number of worker threads to consume from the queue
     */
    String ELASTICSEARCH_WORKER_COUNT = "elasticsearch.worker_count";


    /**
     * The queue implementation to use.  Values come from <class>QueueProvider.Implementations</class>
     */
    String ELASTICSEARCH_QUEUE_IMPL = "elasticsearch.queue_impl";


    /**
     * The queue implementation to use.  Values come from <class>QueueProvider.Implementations</class>
     */
    String ELASTICSEARCH_QUEUE_OFFER_TIMEOUT = "elasticsearch.queue.offer_timeout";

    /**
     * Amount of time to wait when reading from the queue
     */
    String INDEX_QUEUE_READ_TIMEOUT = "elasticsearch.queue_read_timeout";



    @Default( "1000" )
    @Key( FAILURE_REJECTED_RETRY_WAIT_TIME )
    long getFailureRetryTime();

    //give us 60 seconds to process the message
    @Default( "60" )
    @Key( INDEX_QUEUE_READ_TIMEOUT )
    int getIndexQueueTimeout();

    @Default( "1" )
    @Key( ELASTICSEARCH_WORKER_COUNT )
    int getWorkerCount();

    @Default( "LOCAL" )
    @Key( ELASTICSEARCH_QUEUE_IMPL )
    String getQueueImplementation();


    @Default("1000")
    @Key("elasticsearch.reindex.flush.interval")
    int getUpdateInterval();


    @Default("false")
    @Key("elasticsearch.queue_impl.resolution")
    boolean resolveSynchronously();
}
