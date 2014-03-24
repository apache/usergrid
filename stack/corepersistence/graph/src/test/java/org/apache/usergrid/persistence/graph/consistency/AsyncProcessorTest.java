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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import rx.concurrency.Schedulers;

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


        final TimeoutEvent<TestEvent> timeoutEvent = new TimeoutEvent<TestEvent>() {
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


        AsyncProcessor asyncProcessor = constructProcessor( null, queue );


        //mock up the queue
        when( queue.queue( event, timeout ) ).thenReturn( timeoutEvent );


        TimeoutEvent<TestEvent> returned = asyncProcessor.setVerification( event, timeout );

        //ensure the timeouts are returned from the Queue subsystem
        assertSame( timeoutEvent, returned );
    }


    @Test( timeout = 5000 )
    public void verifyAsyncExecution() throws InterruptedException {

        final TestListener listener = new TestListener();

        final EventBus testBus = new EventBus( "test" );

        testBus.register( listener );


        final TestEvent event = new TestEvent();


        final TimeoutEvent<TestEvent> timeoutEvent = new TimeoutEvent<TestEvent>() {
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


        final AsyncProcessor asyncProcessor = constructProcessor( testBus, queue );

        final CountDownLatch latch = new CountDownLatch( 1 );

        //mock up the ack to allow us to block the test until the async confirm fires
        when( queue.remove( timeoutEvent ) ).thenAnswer( new Answer<Boolean>() {
            @Override
            public Boolean answer( final InvocationOnMock invocation ) throws Throwable {
                latch.countDown();
                return true;
            }
        } );


        asyncProcessor.start( timeoutEvent );


        //block until the event is fired.  The correct invocation is implicitly verified by the remove mock

        latch.await();

        final TestEvent firedEvent = listener.events.peek();

        assertSame( event, firedEvent );
    }


    @Test( timeout = 5000 )
//    @Test
    public void verifyErrorExecution() throws InterruptedException {

        final ErrorListener listener = new ErrorListener();

        final EventBus testBus = new EventBus( "test" );

        testBus.register( listener );


        final TestEvent event = new TestEvent();

        final boolean[] invoked = new boolean[]{false, false};


        final TimeoutEvent<TestEvent> timeoutEvent = new TimeoutEvent<TestEvent>() {
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


        final AsyncProcessorImpl asyncProcessor = constructProcessor( testBus, queue );

        final CountDownLatch latch = new CountDownLatch( 1 );

        final TimeoutEvent<?>[] errorEvents = { null };

        //countdown the latch so the test can proceed
        asyncProcessor.addListener( new AsyncProcessorImpl.ErrorListener() {
            @Override
            public <T> void onError( final TimeoutEvent<T> event, final Throwable t ) {
                errorEvents[0] = event;
                invoked[1] = true;
                latch.countDown();
            }
        } );

        //throw an error if remove is called.  This shouldn't happen
        when( queue.remove( timeoutEvent ) ).then( new Answer<Boolean>() {
            @Override
            public Boolean answer( final InvocationOnMock invocation ) throws Throwable {
                invoked[0] = true;
                return false;
            }
        } );



        //fire the event
        asyncProcessor.start( timeoutEvent );


        //block until the event is fired.  The invocation verification is part of the error listener unlocking
        latch.await();

        final TestEvent firedEvent = listener.events.peek();

        assertSame( event, firedEvent );

        assertFalse("Queue remove should not be invoked", invoked[0]);

        assertTrue("Error listener should be invoked", invoked[1]);

        assertEquals( event, errorEvents[0] );
    }


    /**
     * Construct the async processor
     */
    public AsyncProcessorImpl constructProcessor( EventBus eventBus, TimeoutQueue queue ) {

        return new AsyncProcessorImpl( eventBus, queue, Schedulers.threadPoolForIO() );
    }


    /**
     * Marked class for events, does nothing
     */
    public static class TestEvent {

        public boolean equals(Object other){
            return other == this;
        }
    }


    public static class TestListener {

        public final Stack<TestEvent> events = new Stack<TestEvent>();


        public TestListener() {

        }


        @Subscribe
        public void fireTestEvent( TestEvent e ) {
            events.push( e );
        }
    }


    /**
     * Throw error after the event is fired
     */
    public static class ErrorListener  {

        public final Stack<TestEvent> events = new Stack<TestEvent>();


        @Subscribe
        public void fireTestEvent( final TestEvent e ) {
            events.push( e );
            throw new RuntimeException( "Test Exception thrown.  Failed to process event" );
        }
    }
}
