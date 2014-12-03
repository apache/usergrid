/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.core.hystrix;


import com.netflix.astyanax.Execution;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;


/**
 * A utility class that creates graph observables wrapped in Hystrix for timeouts and circuit breakers.
 */
public class HystrixCassandra {




    /**
     * Command group used for realtime user commands
     */
    public static final HystrixCommand.Setter
            USER_GROUP = HystrixCommand.Setter.withGroupKey(   HystrixCommandGroupKey.Factory.asKey( "user" ) ).andThreadPoolPropertiesDefaults(
            HystrixThreadPoolProperties.Setter().withCoreSize( 100 ) );

    /**
     * Command group for asynchronous operations
     */
    public static final HystrixCommand.Setter
            ASYNC_GROUP = HystrixCommand.Setter.withGroupKey( HystrixCommandGroupKey.Factory.asKey( "async" ) ).andThreadPoolPropertiesDefaults(
            HystrixThreadPoolProperties.Setter().withCoreSize( 50 ) );


    /**
     * Execute an user operation
     */
    public static <R> OperationResult<R> user( final Execution<R> execution) {
        return new HystrixCommand<OperationResult<R>>( USER_GROUP ) {

            @Override
            protected OperationResult<R> run() {
                try {
                    return  execution.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( e );
                }
            }
        }.execute();
    }


    /**
     * Execute an an async operation
     */
    public static <R> OperationResult<R> async( final Execution<R> execution) {


        return new HystrixCommand<OperationResult<R>>( ASYNC_GROUP ) {

            @Override
            protected OperationResult<R> run() {
                try {
                    return  execution.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( e );
                }
            }
        }.execute();
    }


}
