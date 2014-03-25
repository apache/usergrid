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

package org.apache.usergrid.persistence.graph.hystrix;


import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 *
 *
 */
public class HystrixGraphObservable {

    /**
     * Wrap the observable in the timeout
     * @param observable
     * @param <T>
     * @return
     */
    public static <T> Observable<T> wrap(final Observable<T> observable){
            return new HystrixObservableCommand<T>( HystrixCommandGroupKey.Factory.asKey( "Graph" ) ){

                @Override
                protected Observable<T> run() {
                    return observable;
                }
            }.toObservable( Schedulers.io() );
        }
}
