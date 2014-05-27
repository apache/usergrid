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

package org.apache.usergrid.persistence.core.consistency;


import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
public class AsyncProcessorFactoriesTest {


//    @Test( timeout = 5000 )
    @Test
    public void verifyAsyncExecution() throws InterruptedException {


        final TimeoutQueue queue = mock( TimeoutQueue.class );

        final TimeService timeService = mock( TimeService.class );


        ConsistencyFig fig = mock( ConsistencyFig.class );

        when( fig.getRepairTimeout() ).thenReturn( 0 );

        LocalTimeoutQueueFactory localTimeoutQueueFactory = new LocalTimeoutQueueFactory( timeService );

        AsyncProcessorFactoryImpl asyncProcessorFactory =
                new AsyncProcessorFactoryImpl( localTimeoutQueueFactory, fig );

        AsyncProcessor<EventType1> processor1 = asyncProcessorFactory.getProcessor( EventType1.class );

        AsyncProcessor<EventType2> processor2 = asyncProcessorFactory.getProcessor( EventType2.class );




        final EventType1 eventType1 = new EventType1();
        final EventType2 eventType2 = new EventType2();
        final CountDownLatch eventType1Latch = new CountDownLatch( 1 );
        final CountDownLatch eventType2Latch = new CountDownLatch( 1 );

        final EventType1[] receivedEventType1 = new EventType1[1];
        final EventType2[] receivedEventType2 = new EventType2[1];

        /**
         * Add the complete listener and message listeners
         */
        processor1.addCompleteListener( new CompleteListener<EventType1>() {
            @Override
            public void onComplete( final AsynchronousMessage<EventType1> event ) {
                eventType1Latch.countDown();
            }
        } );

        processor1.addListener( new MessageListener<EventType1, Object>() {
            @Override
            public Observable<Object> receive( final EventType1 event ) {
                receivedEventType1[0] = event;
                return Observable.empty();
            }
        } );


        processor2.addCompleteListener( new CompleteListener<EventType2>() {
            @Override
            public void onComplete( final AsynchronousMessage<EventType2> event ) {
                eventType2Latch.countDown();
            }
        } );

        processor2.addListener( new MessageListener<EventType2, Object>() {
            @Override
            public Observable<Object> receive( final EventType2 event ) {
                receivedEventType2[0] = event;
                return Observable.empty();
            }
        } );

        AsynchronousMessage<EventType1> event1Message = processor1.setVerification( eventType1, 60000 );
        AsynchronousMessage<EventType2> event2Message = processor2.setVerification( eventType2, 60000 );

        processor1.start( event1Message );

        processor2.start( event2Message );

        eventType1Latch.await();
        eventType2Latch.await();


        assertEquals( eventType1, receivedEventType1[0] );

        assertEquals( eventType2, receivedEventType2[0] );
    }


    private static final class EventType1 implements Serializable{

    }


    private static final class EventType2 implements Serializable{

    }
}
