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
package org.apache.usergrid.persistence.graph.serialization;


import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;


/**
 *
 *
 */
public class TestCount {

    private static final Logger log = LoggerFactory.getLogger( TestCount.class );


    @Test
    public void mergeTest() {

        final int sizePerObservable = 2000;

        Observable<Integer> input1 = getObservables( sizePerObservable ).flatMap( new Func1<Integer, Observable<?
                extends Integer>>() {
            @Override
            public Observable<? extends Integer> call( final Integer integer ) {
                return getObservables( 100 );
            }
        } );
        Observable<Integer> input2 = getObservables( sizePerObservable ).flatMap( new Func1<Integer, Observable<?
                extends Integer>>() {
            @Override
            public Observable<? extends Integer> call( final Integer integer ) {
                return getObservables( 100 );
            }
        } );

        int returned = Observable.merge( input1, input2 ).buffer( 1000 )
                                 .flatMap( new Func1<List<Integer>, Observable<Integer>>() {
                                             @Override
                                             public Observable<Integer> call( final List<Integer> integers ) {

                                                 //simulates batching a network operation from buffer,
                                                 // then re-emitting the values passed

                                                 try {
                                                     Thread.sleep( 100 );
                                                 }
                                                 catch ( InterruptedException e ) {
                                                     throw new RuntimeException( e );
                                                 }


                                                 return Observable.from( integers );
                                             }
                                         } ).count().defaultIfEmpty( 0 ).toBlocking().last();


        assertEquals( "Count was correct", sizePerObservable * 2 * 100, returned );
    }


    /**
     * Get observables from the sets
     */
    private Observable<Integer> getObservables( int size ) {

        final List<Integer> values = new ArrayList<Integer>( size );

        for ( int i = 0; i < size; i++ ) {
            values.add( i );
        }


        /**
         * Simulates occasional sleeps while we fetch something over the network
         */
        return Observable.create( new Observable.OnSubscribe<Integer>() {
            @Override
            public void call( final Subscriber<? super Integer> subscriber ) {

                final int size = values.size();

                for ( int i = 0; i < size; i++ ) {


                    if ( i % 1000 == 0 ) {
                        //simulate network fetch
                        try {
                            Thread.sleep( 250 );
                        }
                        catch ( InterruptedException e ) {
                            subscriber.onError( e );
                            return;
                        }
                    }

                    final Integer value = values.get( i );

                    log.info( "Emitting {}", value );


                    subscriber.onNext( value );
                }

                subscriber.onCompleted();

                //purposefully no error handling here
            }
        } ).subscribeOn( Schedulers.io() );
    }
}
