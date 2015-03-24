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


import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.index.IndexOperationMessage;


/**
 * A temporary interface of our buffer Q to decouple of producer and consumer;
 */
public interface BufferQueue {

    /**
     * Offer the indexoperation message.  Some queues may support not returning the future until ack or fail.
     * Other queues may return the future after ack on the offer.  See the implementation documentation for details.
     * @param operation
     */
    public void offer(final IndexOperationMessage operation);


    /**
     * Perform a take, potentially blocking until up to takesize is available, or timeout has elapsed.
     * May return less than the take size, but will never return null
     *
     * @param takeSize
     * @param timeout
     * @param timeUnit
     * @return A null safe lid
     */
    public List<IndexOperationMessage> take(final int takeSize, final long timeout, final TimeUnit timeUnit );


    /**
     * Ack all messages so they do not appear again.  Meant for transactional queues, and may or may not be implemented.
     * This will set the future as done in in memory operations
     *
     * @param messages
     */
    public void ack(final List<IndexOperationMessage> messages);

    /**
     * Mark these message as failed.  Set the exception in the future on local operation
     *
     * @param messages
     */
    public void fail(final List<IndexOperationMessage> messages, final Throwable t);
}
