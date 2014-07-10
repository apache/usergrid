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


import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;


/**
 * A utility class that creates graph observables wrapped in Hystrix for timeouts and circuit breakers.
 */
public class HystrixCassandra {



    /**
     * Command group used for realtime user commands
     */
    public static final HystrixCommandGroupKey USER_GROUP = HystrixCommandGroupKey.Factory.asKey( "user" );

    /**
     * Command group for asynchronous operations
     */
    public static final HystrixCommandGroupKey ASYNC_GROUP = HystrixCommandGroupKey.Factory.asKey( "async" );


    /**
     * Execute an user mutation
     */
    public static OperationResult<Void> userMutation( final MutationBatch batch ) {


        return new HystrixCommand<OperationResult<Void>>( USER_GROUP ) {

            @Override
            protected OperationResult<Void> run() {
                try {
                    return batch.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( e );
                }
            }
        }.execute();
    }


    /**
     * Execute an user mutation
     */
    public static <K, C> OperationResult<ColumnList<C>> userQuery( final RowQuery<K, C> query ) {


        return new HystrixCommand<OperationResult<ColumnList<C>>>( USER_GROUP ) {

            @Override
            protected OperationResult<ColumnList<C>> run() {
                try {
                    return query.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( e );
                }
            }
        }.execute();
    }


    /**
     * Execute an asynchronous mutation
     */
    public static OperationResult<Void> asyncMutation( final MutationBatch batch ) {


        return new HystrixCommand<OperationResult<Void>>( ASYNC_GROUP ) {

            @Override
            protected OperationResult<Void> run() {
                try {
                    return batch.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( e );
                }
            }
        }.execute();
    }


    /**
     * Execute an asynchronous query
     */
    public static <K, C> OperationResult<ColumnList<C>> asyncQuery( final RowQuery<K, C> query ) {


        return new HystrixCommand<OperationResult<ColumnList<C>>>( ASYNC_GROUP ) {

            @Override
            protected OperationResult<ColumnList<C>> run() {
                try {
                    return query.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( e );
                }
            }
        }.execute();
    }
}
