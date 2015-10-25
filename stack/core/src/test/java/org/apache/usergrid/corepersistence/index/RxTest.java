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

package org.apache.usergrid.corepersistence.index;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test to test some assumptions about RX behaviors
 */
public class RxTest {

    @Test
    public void testPublish() throws InterruptedException {

        final int count = 10;

        final CountDownLatch latch = new CountDownLatch( count );

        final Subscription connectedObservable =
            Observable.range( 0, count ).doOnNext( integer -> latch.countDown() ).subscribeOn( Schedulers.io() )
                      .subscribe();


        final boolean completed = latch.await( 3, TimeUnit.SECONDS );

        assertTrue( "publish1 behaves as expected", completed );

        final boolean completedSubscription = connectedObservable.isUnsubscribed();

        assertTrue( "Subscription complete", completedSubscription );
    }


    @Test
    @Ignore("This seems like it should work, yet blocks forever")
    public void testConnectableObserver() throws InterruptedException {

        final int count = 10;

        final CountDownLatch latch = new CountDownLatch( count );

        final ConnectableObservable<Integer> connectedObservable = Observable.range( 0, count ).publish();


        //connect to our latch, which should run on it's own subscription
        //start our latch running
        connectedObservable.doOnNext( integer -> latch.countDown() ).subscribeOn( Schedulers.io() ).subscribe();


        final Observable<Integer> countObservable = connectedObservable.subscribeOn( Schedulers.io() ).count();

        //start the sequence
        connectedObservable.connect();


        final boolean completed = latch.await( 5, TimeUnit.SECONDS );

        assertTrue( "publish1 behaves as expected", completed );

        final int returnedCount = countObservable.toBlocking().last();

        assertEquals( "Counts the same", count, returnedCount );
    }


    /**
     * Tests that reduce emits
     */
    @Test
    public void testReduceEmpty(){
       final int result =  Observable.range( 0, 100 ).filter( value -> value == -1 ).reduce( 0, ( integer, integer2 ) -> integer + 1 ).toBlocking().last();

        assertEquals(0, result);
    }


}
