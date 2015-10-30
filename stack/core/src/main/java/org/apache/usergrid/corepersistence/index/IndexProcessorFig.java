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
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Application id cache fig
 */
@FigSingleton
public interface IndexProcessorFig extends GuicyFig {


    String FAILURE_REJECTED_RETRY_WAIT_TIME = "elasticsearch.rejected_retry_wait";

    String ELASTICSEARCH_WORKER_COUNT = "elasticsearch.worker_count";

    String ELASTICSEARCH_QUEUE_IMPL = "elasticsearch.queue_impl";

    String INDEX_QUEUE_READ_TIMEOUT = "elasticsearch.queue_read_timeout";

    String INDEX_QUEUE_VISIBILITY_TIMEOUT = "elasticsearch.queue_visibility_timeout";

    String REINDEX_BUFFER_SIZE = "elasticsearch.reindex.buffer_size";


    /**
     * Set the amount of time to wait when Elasticsearch rejects a requests before
     * retrying.  This provides simple back pressure. (in milliseconds)
     */
    @Default("1000")
    @Key(FAILURE_REJECTED_RETRY_WAIT_TIME)
    long getFailureRetryTime();

    /**
     * Set the read timeout for processing messages in the queue. (in milliseconds)
     */
    @Default("10000")
    @Key(INDEX_QUEUE_READ_TIMEOUT)
    int getIndexQueueTimeout();

    /**
     * Set the visibility timeout for messages received from the queue. (in milliseconds).
     * Received messages will remain 'in flight' until they are ack'd(deleted) or this timeout occurs.
     * If the timeout occurs, the messages will become visible again for re-processing.
     */
    @Default("5000") // 5 seconds
    @Key(INDEX_QUEUE_VISIBILITY_TIMEOUT)
    int getIndexQueueVisibilityTimeout();

    /**
     * The number of worker threads used to read index write requests from the queue.
     */
    @Default("16")
    @Key(ELASTICSEARCH_WORKER_COUNT)
    int getWorkerCount();

    /**
     * Set the implementation to use for queuing.
     * Valid values: TEST, LOCAL, SQS, SNS
     * NOTE: SQS and SNS equate to the same implementation of Amazon queue services.
     */
    @Default("LOCAL")
    @Key(ELASTICSEARCH_QUEUE_IMPL)
    String getQueueImplementation();

    @Default("1000")
    @Key(REINDEX_BUFFER_SIZE)
    int getReindexBufferSize();

    /**
     * Flag to resolve the LOCAL queue implementation service synchronously.
     */
    @Default("false")
    @Key("elasticsearch.queue_impl.resolution")
    boolean resolveSynchronously();

    /**
     * Get the message TTL in milliseconds.  Defaults to 24 hours
     *
     * 24 * 60 * 60 * 1000
     *
     * @return
     */
    @Default("86400000")
    @Key( "elasticsearch.message.ttl" )
    int getIndexMessageTtl();
}
