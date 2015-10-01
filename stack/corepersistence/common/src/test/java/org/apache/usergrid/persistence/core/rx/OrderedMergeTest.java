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
package org.apache.usergrid.persistence.core.rx;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class OrderedMergeTest {

    private static final Logger log = LoggerFactory.getLogger( OrderedMergeTest.class );


    @Test
    public void singleOperator() throws InterruptedException {

        List<Integer> expected = Arrays.asList( 0, 1, 2, 3, 4, 5 );

        Observable<Integer> ints = Observable.from( expected );


        Observable<Integer> ordered = OrderedMerge.orderedMerge( new IntegerComparator(), 10, ints );

        final CountDownLatch latch = new CountDownLatch( 1 );
        final List<Integer> results = new ArrayList();

        ordered.subscribe( new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }


            @Override
            public void onError( final Throwable e ) {
                e.printStackTrace();
                fail( "An error was thrown " );
            }


            @Override
            public void onNext( final Integer integer ) {
                log.info( "onNext invoked with {}", integer );
                results.add( integer );
            }
        } );

        latch.await();


        assertEquals( expected.size(), results.size() );


        for ( int i = 0; i < expected.size(); i++ ) {
            assertEquals( "Same element expected", expected.get( i ), results.get( i ) );
        }
    }


    @Test
    public void multipleOperatorSameThread() throws InterruptedException {

        List<Integer> expected1List = Arrays.asList( 5, 3, 2, 0 );

        Observable<Integer> expected1 = Observable.from( expected1List );

        List<Integer> expected2List = Arrays.asList( 10, 7, 6, 4 );

        Observable<Integer> expected2 = Observable.from( expected2List );

        List<Integer> expected3List = Arrays.asList( 9, 8, 1 );

        Observable<Integer> expected3 = Observable.from( expected3List );


        Observable<Integer> ordered =
                OrderedMerge.orderedMerge( new ReverseIntegerComparator(), 10, expected1, expected2, expected3 );

        final CountDownLatch latch = new CountDownLatch( 1 );
        final List<Integer> results = new ArrayList();

        ordered.subscribe( new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }


            @Override
            public void onError( final Throwable e ) {
                e.printStackTrace();
                fail( "An error was thrown " );
            }


            @Override
            public void onNext( final Integer integer ) {
                log.info( "onNext invoked with {}", integer );
                results.add( integer );
            }
        } );

        latch.await();

        List<Integer> expected = Arrays.asList( 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 );

        assertEquals( expected.size(), results.size() );


        for ( int i = 0; i < expected.size(); i++ ) {
            assertEquals( "Same element expected", expected.get( i ), results.get( i ) );
        }
    }


    @Test
    @Ignore( "Doesn't work until backpressure is implemented" )
    public void multipleOperatorSingleThreadSizeException() throws InterruptedException {

        List<Integer> expected1List = Arrays.asList( 5, 3, 2, 0 );

        Observable<Integer> expected1 = Observable.from(expected1List);

        List<Integer> expected2List = Arrays.asList(10, 7, 6, 4);

        Observable<Integer> expected2 = Observable.from(expected2List);

        List<Integer> expected3List = Arrays.asList(9, 8, 1);

        Observable<Integer> expected3 = Observable.from(expected3List);

        //set our buffer size to 2.  We should easily exceed this since every observable has more than 2 elements

        Observable<Integer> ordered =
                OrderedMerge.orderedMerge(new ReverseIntegerComparator(), 2, expected1, expected2, expected3);

        final CountDownLatch latch = new CountDownLatch( 1 );
        final List<Integer> results = new ArrayList();

        final boolean[] errorThrown = new boolean[1];

        ordered.subscribe( new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }


            @Override
            public void onError( final Throwable e ) {
                log.error( "Expected error thrown", e );

                if ( e.getMessage().contains( "The maximum queue size of 2 has been reached" ) ) {
                    errorThrown[0] = true;
                }

                latch.countDown();
            }


            @Override
            public void onNext( final Integer integer ) {
                log.info( "onNext invoked with {}", integer );
                results.add( integer );
            }
        } );

        latch.await();


        /**
         * Since we're on the same thread, we should blow up before we begin producing elements our size
         */
        assertEquals(0, results.size());

        assertTrue("An exception was thrown", errorThrown[0]);
    }


    @Test
    public void multipleOperatorThreads() throws InterruptedException {

        List<Integer> expected1List = Arrays.asList(5, 3, 2, 0);

        Observable<Integer> expected1 = Observable.from( expected1List ).subscribeOn(Schedulers.io());

        List<Integer> expected2List = Arrays.asList(10, 7, 6, 4);

        Observable<Integer> expected2 = Observable.from( expected2List ).subscribeOn(Schedulers.io());


        List<Integer> expected3List = Arrays.asList(9, 8, 1);

        Observable<Integer> expected3 = Observable.from( expected3List ).subscribeOn(Schedulers.io());


        Observable<Integer> ordered =
                OrderedMerge.orderedMerge( new ReverseIntegerComparator(), 10, expected1, expected2, expected3 );

        final CountDownLatch latch = new CountDownLatch( 1 );
        final List<Integer> results = new ArrayList();

        ordered.subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }


            @Override
            public void onError(final Throwable e) {
                e.printStackTrace();
                fail("An error was thrown ");
            }


            @Override
            public void onNext(final Integer integer) {
                log.info("onNext invoked with {}", integer);
                results.add(integer);
            }
        });

        latch.await();

        List<Integer> expected = Arrays.asList( 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 );

        assertEquals(expected.size(), results.size());


        for ( int i = 0; i < expected.size(); i++ ) {
            assertEquals( "Same element expected", expected.get( i ), results.get( i ) );
        }
    }


    @Test
    @Ignore("Shouldn't throw an exception, should work with current impl.  "
            + "Needs to be changed when backpressure is introduced" )
    public void multipleOperatorMultipleThreadSizeException() throws InterruptedException {

        List<Integer> expected1List = Arrays.asList( 10, 4, 3, 2, 1 );

        Observable<Integer> expected1 = Observable.from( expected1List ).subscribeOn( Schedulers.io() );

        List<Integer> expected2List = Arrays.asList( 9, 8, 7 );

        Observable<Integer> expected2 = Observable.from( expected2List ).subscribeOn( Schedulers.io() );


        List<Integer> expected3List = Arrays.asList( 6, 5, 0 );

        Observable<Integer> expected3 = Observable.from( expected3List ).subscribeOn( Schedulers.io() );


        /**
         * Fails because our first observable will have to buffer the last 4 elements while waiting for the others to
         * proceed
         */
        Observable<Integer> ordered =
                OrderedMerge.orderedMerge( new IntegerComparator(), 2, expected1, expected2, expected3 );

        final CountDownLatch latch = new CountDownLatch( 1 );

        final boolean[] errorThrown = new boolean[1];

        ordered.subscribe( new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }


            @Override
            public void onError( final Throwable e ) {
                log.error("Expected error thrown", e);

                if ( e.getMessage().contains( "The maximum queue size of 2 has been reached" ) ) {
                    errorThrown[0] = true;
                }

                latch.countDown();
            }


            @Override
            public void onNext( final Integer integer ) {
                log.info("onNext invoked with {}", integer);
            }
        } );

        latch.await();


        assertTrue("An exception was thrown", errorThrown[0]);
    }


    /**
     * Tests that with a buffer size much smaller than our inputs, we successfully block observables from
     * producing values when our pressure gets too high.  Eventually, one of these events should begin production, eventually
     * draining all values
     *
     * @throws InterruptedException
     */
    @Test
    public void multipleOperatorMultipleThreadSizePressure() throws InterruptedException {

        List<Integer> expected1List = Arrays.asList( 10, 4, 3, 2, 1 );

        Observable<Integer> expected1 = Observable.from( expected1List ).subscribeOn( Schedulers.io() );

        List<Integer> expected2List = Arrays.asList( 9, 8, 7 );

        Observable<Integer> expected2 = Observable.from( expected2List ).subscribeOn( Schedulers.io() );


        List<Integer> expected3List = Arrays.asList( 6, 5, 0 );

        Observable<Integer> expected3 = Observable.from( expected3List ).subscribeOn( Schedulers.io() );


        /**
         * Fails because our first observable will have to buffer the last 4 elements while waiting for the others to
         * proceed
         */
        Observable<Integer> ordered =
                OrderedMerge.orderedMerge( new ReverseIntegerComparator(), 2, expected1, expected2, expected3 );


        final CountDownLatch latch = new CountDownLatch( 1 );
        final List<Integer> results = new ArrayList();

        ordered.subscribe( new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }


            @Override
            public void onError( final Throwable e ) {
                e.printStackTrace();
                fail("An error was thrown ");
            }


            @Override
            public void onNext( final Integer integer ) {
                log.info( "onNext invoked with {}", integer );
                results.add(integer);
            }
        } );

        latch.await();

        List<Integer> expected = Arrays.asList( 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 );

        assertEquals(expected.size(), results.size());


        for ( int i = 0; i < expected.size(); i++ ) {
            assertEquals( "Same element expected", expected.get( i ), results.get( i ) );
        }
    }


    /**
       * Tests that with a buffer size much smaller than our inputs, we successfully block observables from
       * producing values when our pressure gets too high.  Eventually, one of these events should begin production, eventually
       * draining all values
       *
       * @throws InterruptedException
       */
      @Test
      public void testDuplicateOrderingCorrect() throws InterruptedException {

          List<Integer> expected1List = Arrays.asList( 10, 5, 4,  3, 2, 1 );

          Observable<Integer> expected1 = Observable.from( expected1List ).subscribeOn( Schedulers.io() );

          List<Integer> expected2List = Arrays.asList( 9, 8, 7, 6, 5 );

          Observable<Integer> expected2 = Observable.from( expected2List ).subscribeOn( Schedulers.io() );


          List<Integer> expected3List = Arrays.asList( 9, 6, 5, 3, 2, 1, 0 );

          Observable<Integer> expected3 = Observable.from( expected3List ).subscribeOn( Schedulers.io() );


          /**
           * Fails because our first observable will have to buffer the last 4 elements while waiting for the others to
           * proceed
           */
          Observable<Integer> ordered =
                  OrderedMerge.orderedMerge( new ReverseIntegerComparator(), 2, expected1, expected2, expected3 );


          final CountDownLatch latch = new CountDownLatch( 1 );
          final List<Integer> results = new ArrayList();

          ordered.subscribe( new Subscriber<Integer>() {
              @Override
              public void onCompleted() {
                  latch.countDown();
              }


              @Override
              public void onError( final Throwable e ) {
                  e.printStackTrace();
                  fail( "An error was thrown " );
              }


              @Override
              public void onNext( final Integer integer ) {
                  log.info( "onNext invoked with {}", integer );
                  results.add( integer );
              }
          } );

          latch.await();

          List<Integer> expected = Arrays.asList( 10, 9, 9,  8, 7, 6,  6, 5, 5, 5, 4, 3, 3, 2, 2, 1, 1, 0);

          assertEquals( expected.size(), results.size() );


          for ( int i = 0; i < expected.size(); i++ ) {
              assertEquals( "Same element expected", expected.get( i ), results.get( i ) );
          }
      }


    @Test
    public void testSubscribe(){
        List<Integer> expected = Arrays.asList( 10, 9, 9,  8, 7, 6,  6, 5, 5, 5, 4, 3, 3, 2, 2, 1, 1, 0);

        final AtomicInteger i = new AtomicInteger();
        Observable.from(expected).doOnNext(x -> {
            log.info("print " + x);
            i.set(x);
        }).doOnError(e -> log.error(e.getMessage())).subscribe();
        log.info("last");
        assertTrue(i.get()==0);
    }


    @Test
    public void testSubscribeException() {
        try {
            List<Integer> expected = Arrays.asList(10, 9, 9, 8, 7, 6, 6, 5, 5, 5, 4, 3, 3, 2, 2, 1, 1, 0);

            Observable.from(expected).doOnNext(x -> {
                log.info("print " + x);
                throw new RuntimeException();
            }).doOnError(e -> log.error(e.getMessage())).subscribe();
            log.info("last");
            fail();
        } catch (Exception e) {
        }
    }
    /**
       * Tests that with a buffer size much smaller than our inputs, we successfully block observables from
       * producing values when our pressure gets too high.  Eventually, one of these events should begin production, eventually
       * draining all values
       *
       * @throws InterruptedException
       */
      @Test
      public void testDuplicateOrderingCorrectComparator() throws InterruptedException {

          List<Integer> expected1List = Arrays.asList( 1, 2, 3, 4, 5, 10 );

          Observable<Integer> expected1 = Observable.from( expected1List ).subscribeOn( Schedulers.io() );

          List<Integer> expected2List = Arrays.asList( 5, 6, 7, 8, 9 );

          Observable<Integer> expected2 = Observable.from( expected2List ).subscribeOn( Schedulers.io() );


          List<Integer> expected3List = Arrays.asList( 0, 1, 2, 3, 5, 6, 9 );

          Observable<Integer> expected3 = Observable.from( expected3List ).subscribeOn( Schedulers.io() );


          /**
           * Fails because our first observable will have to buffer the last 4 elements while waiting for the others to
           * proceed
           */
          Observable<Integer> ordered =
                  OrderedMerge.orderedMerge( new IntegerComparator(), 2, expected1, expected2, expected3 );


          final CountDownLatch latch = new CountDownLatch( 1 );
          final List<Integer> results = new ArrayList();

          ordered.subscribe( new Subscriber<Integer>() {
              @Override
              public void onCompleted() {
                  latch.countDown();
              }


              @Override
              public void onError( final Throwable e ) {
                  e.printStackTrace();
                  fail( "An error was thrown " );
              }


              @Override
              public void onNext( final Integer integer ) {
                  log.info( "onNext invoked with {}", integer );
                  results.add( integer );
              }
          } );

          latch.await();

          List<Integer> expected = Arrays.asList(  0, 1, 1,2, 2, 3, 3,4,  5, 5, 5,  6,  6, 7,8,  9, 9,10 );

          assertEquals( expected.size(), results.size() );


          for ( int i = 0; i < expected.size(); i++ ) {
              assertEquals( "Same element expected", expected.get( i ), results.get( i ) );
          }
      }




    private static class IntegerComparator implements Comparator<Integer> {

        @Override
        public int compare( final Integer o1, final Integer o2 ) {
            return Integer.compare( o1, o2 );
        }
    }


    private static class ReverseIntegerComparator implements Comparator<Integer> {

        @Override
        public int compare( final Integer o1, final Integer o2 ) {
            return Integer.compare( o1, o2 ) * -1;
        }
    }





}
