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
     * Offer the indexoperation message
     * @param operation
     */
    public void offer(final IndexOperationMessage operation);


    /**
     * Perform a take, potentially blocking.  Until takesize is available, or timeout has elapsed
     * @param takeSize
     * @param timeout
     * @param timeUnit
     * @return
     */
    public List<IndexOperationMessage> take(final int takeSize, final long timeout, final TimeUnit timeUnit );
}
