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
import org.apache.usergrid.persistence.index.impl.BatchRequest;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.support.replication.ShardReplicationOperationRequestBuilder;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Container for index operations.
 */
public  class IndexOperationMessage implements Serializable {
    private final Set<BatchRequest> builders;
    private final BetterFuture<IndexOperationMessage> containerFuture;

    public IndexOperationMessage(){
        final IndexOperationMessage parent = this;
        this.builders = new HashSet<>();
        this.containerFuture = new BetterFuture<>(new Callable<IndexOperationMessage>() {
            @Override
            public IndexOperationMessage call() throws Exception {
                return parent;
            }
        });
    }


    /**
     * Add all our operations in the set
     * @param requests
     */
    public void setOperations(final Set<BatchRequest> requests){
        this.builders.addAll( requests);
    }


    /**
     * Add the operation to the set
     * @param builder
     */
    public void addOperation(BatchRequest builder){
        builders.add(builder);
    }

    /**
     * return operations for the message
     * @return
     */
    public Set<BatchRequest> getOperations(){
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
