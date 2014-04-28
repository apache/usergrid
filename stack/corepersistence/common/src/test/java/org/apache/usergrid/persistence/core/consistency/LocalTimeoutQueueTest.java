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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.usergrid.persistence.core.consistency.AsynchronousMessage;
import org.apache.usergrid.persistence.core.consistency.LocalTimeoutQueue;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.consistency.TimeoutQueue;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test timeout queue on the local system
 */
public class LocalTimeoutQueueTest {

    @Test
    public void queueReadRemove() {

        TimeService timeService = Mockito.mock( TimeService.class );

        final long time = 1000l;
        final long timeout = 1000;

        Mockito.when( timeService.getCurrentTime() ).thenReturn( time );

        TimeoutQueue<TestEvent> queue = new LocalTimeoutQueue<TestEvent>( timeService );

        final TestEvent event = new TestEvent();

        AsynchronousMessage<TestEvent> asynchronousMessage = queue.queue( event, timeout );

        assertNotNull( asynchronousMessage );

        assertEquals( event, asynchronousMessage.getEvent() );
        assertEquals( time + timeout, asynchronousMessage.getTimeout() );

        Collection<AsynchronousMessage<TestEvent>> results = queue.take( 100, timeout );

        assertEquals( "Time not yet elapsed", 0, results.size() );

        //now elapse the time
        final long firstTime = time + timeout;

        Mockito.when( timeService.getCurrentTime() ).thenReturn( firstTime );

        results = queue.take( 100, timeout );

        assertEquals( "Time elapsed", 1, results.size() );

        //validate we get a new timeout event since the old one was re-scheduled
        Iterator<AsynchronousMessage<TestEvent>> events = results.iterator();

        AsynchronousMessage<TestEvent> message = events.next();

        assertEquals( event, message.getEvent() );

        assertEquals( firstTime + timeout, message.getTimeout() );


        //now remove it
        queue.remove( message );

        //advance time again (a lot)
        Mockito.when( timeService.getCurrentTime() ).thenReturn( firstTime * 20 );

        results = queue.take( 100, timeout );

        assertEquals( "Queue now empty", 0, results.size() );
    }


    @Test
    public void queueReadTimeout() {

        TimeService timeService = Mockito.mock( TimeService.class );

        final long time = 1000l;
        final long timeout = 1000;

        final int queueSize = 1000;

        Mockito.when( timeService.getCurrentTime() ).thenReturn( time );

        TimeoutQueue<TestEvent> queue = new LocalTimeoutQueue<TestEvent>( timeService );


        Set<TestEvent> events = new HashSet<TestEvent>();

        for ( int i = 0; i < queueSize; i++ ) {

            final TestEvent event = new TestEvent();

            AsynchronousMessage<TestEvent> asynchronousMessage = queue.queue( event, timeout );

            events.add( event );

            assertNotNull( asynchronousMessage );

            assertEquals( event, asynchronousMessage.getEvent() );
            assertEquals( time + timeout, asynchronousMessage.getTimeout() );
        }


        Collection<AsynchronousMessage<TestEvent>> results = queue.take( 100, timeout );

        assertEquals( "Time not yet elapsed", 0, results.size() );

        //now elapse the time
        final long firstTime = time + timeout;

        Mockito.when( timeService.getCurrentTime() ).thenReturn( firstTime );


        final int takeSize = 100;

        final int iterations = queueSize / takeSize;

        for ( int i = 0; i < iterations; i++ ) {

            results = queue.take( takeSize, timeout );

            if ( results.size() == 0 ) {
                break;
            }

            assertEquals( "Time elapsed", 100, results.size() );

            //validate we get a new timeout event since the old one was re-scheduled
            Iterator<AsynchronousMessage<TestEvent>> eventIterator = results.iterator();

            while ( eventIterator.hasNext() ) {

                AsynchronousMessage<TestEvent> message = eventIterator.next();

                assertTrue( events.remove( message.getEvent() ) );

                assertEquals( firstTime + timeout, message.getTimeout() );

                //remove from our queue
                boolean removed = queue.remove( message );

                assertTrue( removed );
            }
        }


        assertEquals( "All elements dequeued", 0, events.size() );
    }


    public static class TestEvent implements Serializable {

        public boolean equals( Object o ) {
            return this == o;
        }
    }
}
