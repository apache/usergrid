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


import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.count.SimpleBatcher;


/**
 * Rule  that sets the batch counters to flush immediately, then returns the state to it's expected state afterwards
 */
public class ImmediateCounterRule implements TestRule {

    private final SimpleBatcher batcher;


    public ImmediateCounterRule( final CassandraResource cassandraResource ) {
        batcher = cassandraResource.getBean( SimpleBatcher.class );
    }


    @Override
    public Statement apply( final Statement base, final Description description ) {
        return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        setFlush();

                        try {
                            base.evaluate();
                        }
                        finally {
                           clearFlush();
                        }
                    }
                };
    }


    public void setFlush() {


        batcher.setBlockingSubmit( true );
        batcher.setBatchSize( 1 );
    }


    public void clearFlush() {
        batcher.setBlockingSubmit( true );
        batcher.setBatchSize( 1 );
    }
}
