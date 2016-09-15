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


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.avro.generic.GenericData;
import org.apache.usergrid.ExperimentalTest;
import org.junit.Ignore;
import org.junit.Test;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(RxTest.class);


    @Test
    @Category(ExperimentalTest.class )
    public void testPublish() throws InterruptedException {

        final int count = 10;

        final CountDownLatch latch = new CountDownLatch( count+1 );

        final Subscription connectedObservable =
            Observable.range( 0, count ).doOnNext( integer -> latch.countDown() ).doOnCompleted( () -> latch.countDown() ).subscribeOn( Schedulers.io() )
                      .subscribe();


        final boolean completed = latch.await( 3, TimeUnit.SECONDS );

        assertTrue( "publish1 behaves as expected", completed );

        final boolean completedSubscription = connectedObservable.isUnsubscribed();

        assertTrue( "Subscription complete", completedSubscription );
    }


    @Test
    @Category(ExperimentalTest.class )
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

    @Test
    public void testStreamWithinObservable(){

        List<Integer> numbers = new ArrayList<Integer>(5){{
            add(1);
            add(2);
            add(3);
            add(4);
            add(5);
        }};

        Observable.just(numbers).map( integers -> {

            try{

                logger.info("Starting size: {}", String.valueOf(numbers.size()));

                List<StreamResult> results = callStream(integers);

                logger.info("In process size: {}", String.valueOf(results.size()));

                List<Integer> checked = checkResults(results);

                logger.info("Resulting Size: {}", String.valueOf(checked.size()));

                return results;

            }
            catch(Exception e){

                logger.info("Caught exception in observable: {}", e.getMessage());
                return null;


            }

        }).subscribe();







    }

    @Test
    public void someTest(){


        final String uuidtype = "UUIDType";
        final String utf8type = "UTF8Type";

        assertEquals(uuidtype.length(), utf8type.length());

    }

    private List<StreamResult> callStream (final List<Integer> input){

        Stream<StreamResult> results = input.stream().map(integer -> {

            try{



                if(integer.equals(1) || integer.equals(2)){
                    throwSomeException("Ah integer not what we want!");
                }

                return new StreamResult(integer);

            }
            catch(Exception e){

                logger.info("Caught exception in stream: '{}'", e.getMessage());
                return new StreamResult(0);

            }

        });

        return results.collect(Collectors.toList());

    }


    private List<Integer> checkResults(final List<StreamResult> streamResults){

        List<Integer> combined = new ArrayList<>();
        List<Integer> integers = streamResults.stream().filter( streamResult -> streamResult.getNumber() > 0)
            .map(streamResult -> {

                combined.add(streamResult.getNumber());

                return streamResult.getNumber();
            })
            .collect(Collectors.toList());

        Observable.from(combined).map( s -> {
            logger.info("Doing work in another observable with Integer: {}", s);
            return s;
        }).toBlocking().last();


        return integers;

    }


    public class StreamResult {

        private int number;

        public StreamResult( final int number){

            this.number = number;
        }

        public int getNumber(){
            return number;
        }


    }

    public void throwSomeException(String message){

        throw new RuntimeException(message);
    }


}
