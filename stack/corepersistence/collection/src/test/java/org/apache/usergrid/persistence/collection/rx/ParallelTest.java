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

package org.apache.usergrid.persistence.collection.rx;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.hystrix.CassandraCommand;

import rx.Observable;
import rx.util.functions.Func1;
import rx.util.functions.FuncN;

import static org.junit.Assert.assertEquals;


/**
 * Simple tests that provides examples of how to perform common operations in RX
 */
public class ParallelTest {

    private static final Logger logger = LoggerFactory.getLogger( ParallelTest.class );


//    @Test( timeout = 5000 )
    @Test
    public void concurrentFunctions() {
        final String input = "input";

        final int size = 9;

        //TODO Tweak our thread pool size beyond 10.

        //latch used to make each thread block to prove correctness
        final CountDownLatch latch = new CountDownLatch( size );

        final List<Observable<String>> observables = new ArrayList<Observable<String>>( size );


        //this is not using a hystrix thread pool as I expected but rather the Rx computation thread pool.  Am I doing this
        //incorrectly?
        for ( int i = 0; i < size; i++ ) {
            observables.add( new CassandraCommand<String>( input ).toObservable().map( new Func1<String, String>() {
                @Override
                public String call( final String s ) {

                    final String threadName = Thread.currentThread().getName();

                    latch.countDown();

                    logger.info( "Function executing on thread {} with latch value {}",
                            threadName, latch.getCount() );


                    try {
                        latch.await();
                    }
                    catch ( InterruptedException e ) {
                        throw new RuntimeException( e );
                    }

                    return s;
                }
            } ) );
        }


        Observable<String> zipped = Observable.zip( observables, new FuncN<String>() {

            @Override
            public String call( final Object... args ) {
                assertEquals( size, args.length );
                return input;
            }
        } );


        String last = zipped.toBlockingObservable().last();


        assertEquals( input, last );
    }
}
