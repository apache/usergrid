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

package org.apache.usergrid.persistence.graph.consistency;


import java.util.Stack;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.concurrency.Schedulers;
import rx.util.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
public class AsyncProcessorTest {


    @Test
    public void verificationSchedule() {


        final long timeout = 500;
        final TestEvent event = new TestEvent();


        final AsynchronousMessage<TestEvent> asynchronousMessage = new AsynchronousMessage<TestEvent>() {
            @Override
            public TestEvent getEvent() {
                return event;
            }


            @Override
            public long getTimeout() {
                return timeout;
            }
        };

        final TimeoutQueue queue = mock( TimeoutQueue.class );


        AsyncProcessor asyncProcessor = constructProcessor( queue, null );


        //mock up the queue
        when( queue.queue( event, timeout ) ).thenReturn( asynchronousMessage );


        AsynchronousMessage<TestEvent> returned = asyncProcessor.setVerification( event, timeout );

        //ensure the timeouts are returned from the Queue subsystem
        assertSame( asynchronousMessage, returned );
    }


    @Test( timeout = 5000 )
    public void verifyAsyncExecution() throws InterruptedException {

        final TestListener listener = new TestListener();

        final TestEvent event = new TestEvent();


        final AsynchronousMessage<TestEvent> asynchronousMessage = new AsynchronousMessage<TestEvent>() {
            @Override
            public TestEvent getEvent() {
                return event;
            }


            @Override
            public long getTimeout() {
                return 500;
            }
        };

        final TimeoutQueue queue = mock( TimeoutQueue.class );


        final AsyncProcessor asyncProcessor = constructProcessor( queue, listener );

        final CountDownLatch latch = new CountDownLatch( 1 );

        //mock up the ack to allow us to block the test until the async confirm fires
        when( queue.remove( asynchronousMessage ) ).thenAnswer( new Answer<Boolean>() {
            @Override
            public Boolean answer( final InvocationOnMock invocation ) throws Throwable {
                latch.countDown();
                return true;
            }
        } );


        asyncProcessor.start( asynchronousMessage );


        //block until the event is fired.  The correct invocation is implicitly verified by the remove mock

        latch.await();

        final TestEvent firedEvent = listener.events.peek();

        assertSame( event, firedEvent );
    }


//    @Test( timeout = 5000 )
        @Test
    public void verifyErrorExecution() throws InterruptedException {

        final AsynchronousErrorListener listener = new AsynchronousErrorListener();


        final TestEvent event = new TestEvent();

        final boolean[] invoked = new boolean[] { false, false };


        final AsynchronousMessage<TestEvent> asynchronousMessage = new AsynchronousMessage<TestEvent>() {
            @Override
            public TestEvent getEvent() {
                return event;
            }


            @Override
            public long getTimeout() {
                return 500;
            }
        };

        final TimeoutQueue queue = mock( TimeoutQueue.class );


        final AsyncProcessorImpl asyncProcessor = constructProcessor( queue, listener );

        final CountDownLatch latch = new CountDownLatch( 1 );

        final AsynchronousMessage<?>[] errorEvents = { null };

        //countdown the latch so the test can proceed
        asyncProcessor.addErrorListener( new ErrorListener<TestEvent>() {
            @Override
            public void onError( final AsynchronousMessage<TestEvent> event, final Throwable t ) {
                errorEvents[0] = event;
                invoked[1] = true;
                latch.countDown();
            }

        } );

        //throw an error if remove is called.  This shouldn't happen
        when( queue.remove( asynchronousMessage ) ).then( new Answer<Boolean>() {
            @Override
            public Boolean answer( final InvocationOnMock invocation ) throws Throwable {
                invoked[0] = true;
                return false;
            }
        } );


        //fire the event
        asyncProcessor.start( asynchronousMessage );


        //block until the event is fired.  The invocation verification is part of the error listener unlocking
        latch.await();

        final TestEvent firedEvent = listener.events.peek();

        assertSame( event, firedEvent );

        assertFalse( "Queue remove should not be invoked", invoked[0] );

        assertTrue( "Error listener should be invoked", invoked[1] );

        assertEquals( event, errorEvents[0].getEvent() );
    }


    /**
     * Construct the async processor
     */
    public <T> AsyncProcessorImpl<T> constructProcessor( TimeoutQueue<T> queue , MessageListener<T, T> listener) {

        AsyncProcessorImpl<T> processor =  new AsyncProcessorImpl( queue,Schedulers.threadPoolForIO() );
        processor.addListener( listener );

        return processor;
    }


    /**
     * Marked class for events, does nothing
     */
    public static class TestEvent {

        public boolean equals( Object other ) {
            return this == other;
        }
    }


    public static class TestListener implements MessageListener<TestEvent, TestEvent> {

        public final Stack<TestEvent> events = new Stack<TestEvent>();


        public TestListener() {

        }


        @Override
        public Observable<TestEvent> receive( final TestEvent event ) {

            return Observable.from( event ).doOnEach(new Action1<TestEvent>() {
               @Override
               public void call( final TestEvent testEvent ) {
                   events.push( testEvent );
               }
           });
        }
    }


    /**
     * Throw error after the event is fired
     */
    public static class AsynchronousErrorListener implements MessageListener<TestEvent, TestEvent> {

        public final Stack<TestEvent> events = new Stack<TestEvent>();


        @Override
        public Observable<TestEvent> receive( final TestEvent event ) {
            return Observable.from( event ).doOnEach(new Action1<TestEvent>() {
                          @Override
                          public void call( final TestEvent testEvent ) {
                              events.push( testEvent );
                              throw new RuntimeException( "Test Exception thrown.  Failed to process event" );
                          }
                      });
        }


    }
}
