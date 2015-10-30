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

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import org.apache.usergrid.persistence.core.executor.TaskExecutorFactory;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.AsyncTaskExecutor;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Implementation for a single task execution system within graph
 */
@Singleton
public class AsyncTaskExecutorImpl implements AsyncTaskExecutor {


    private final ListeningExecutorService taskExecutor;


    @Inject
    public AsyncTaskExecutorImpl(final GraphFig graphFig){
        this.taskExecutor = MoreExecutors.listeningDecorator( TaskExecutorFactory
                    .createTaskExecutor( "GraphTaskExecutor", graphFig.getShardAuditWorkerCount(),
                        graphFig.getShardAuditWorkerQueueSize(), TaskExecutorFactory.RejectionAction.ABORT ) );
    }


    @Override
    public ListeningExecutorService getExecutorService() {
        return this.taskExecutor;
    }
}
