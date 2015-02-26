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
package org.apache.usergrid.persistence.index;

import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.elasticsearch.action.support.replication.ShardReplicationOperationRequestBuilder;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Container for index operations.
 */
public  class IndexOperationMessage {
    private final ConcurrentLinkedQueue<ShardReplicationOperationRequestBuilder> builders;
    private final BetterFuture<IndexOperationMessage> containerFuture;

    public IndexOperationMessage(){
        final IndexOperationMessage parent = this;
        builders = new ConcurrentLinkedQueue<>();
        this.containerFuture = new BetterFuture<>(new Callable<IndexOperationMessage>() {
            @Override
            public IndexOperationMessage call() throws Exception {
                return parent;
            }
        });
    }

    public void addOperation(ShardReplicationOperationRequestBuilder builder){
        builders.add(builder);
    }

    /**
     * return operations for the message
     * @return
     */
    public ConcurrentLinkedQueue<ShardReplicationOperationRequestBuilder> getOperations(){
        return builders;
    }

    /**
     * return the promise
     * @return
     */
    public BetterFuture<IndexOperationMessage> getFuture(){
        return containerFuture;
    }

}
