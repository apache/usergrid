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

import org.junit.Test;

import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertTrue;


/**
 * Validates the Rx scheduler works as expected with publish
 */
public class PublishRxTest {

    @Test
    public void testPublish() throws InterruptedException {

        final int count = 10;

        final CountDownLatch latch = new CountDownLatch( count );

        final Subscription connectedObservable =
            Observable.range( 0, count ).doOnNext( integer -> latch.countDown() ).subscribeOn( Schedulers.io() )
                      .subscribe();


        final boolean completed = latch.await( 5, TimeUnit.SECONDS );

        assertTrue( "publish1 behaves as expected", completed );

        final boolean completedSubscription =  connectedObservable.isUnsubscribed();;

        assertTrue("Subscription complete", completedSubscription);
    }
}
