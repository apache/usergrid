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

package org.apache.usergrid.utils;


import org.junit.rules.ExternalResource;

import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.count.SimpleBatcher;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;


/**
 * Rule  that sets the batch counters to flush immediately, then returns the state to it's expected state afterwards
 */
public class ImmediateCounterRule extends ExternalResource {

    private final SimpleBatcher batcher;


    public ImmediateCounterRule( ) {
        batcher = ConcurrentProcessSingleton.getInstance().getSpringResource().getBean( SimpleBatcher.class );
    }


    @Override
    protected void before() throws Throwable {
        batcher.setBlockingSubmit( true );
        batcher.setBatchSize( 1 );
        super.before();
    }


    @Override
    protected void after() {
        batcher.setBlockingSubmit( false );
        batcher.setBatchSize( 10000 );
        super.after();
    }
}
