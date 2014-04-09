package org.apache.usergrid.persistence.graph.consistency;


import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

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

        TimeService timeService = mock( TimeService.class );

        final long time = 1000l;
        final long timeout = 1000;

        when( timeService.getCurrentTime() ).thenReturn( time );

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

        when( timeService.getCurrentTime() ).thenReturn( firstTime );

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
        when( timeService.getCurrentTime() ).thenReturn( firstTime * 20 );

        results = queue.take( 100, timeout );

        assertEquals( "Queue now empty", 0, results.size() );
    }


    @Test
    public void queueReadTimeout() {

        TimeService timeService = mock( TimeService.class );

        final long time = 1000l;
        final long timeout = 1000;

        final int queueSize = 1000;

        when( timeService.getCurrentTime() ).thenReturn( time );

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

        when( timeService.getCurrentTime() ).thenReturn( firstTime );


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


    public static class TestEvent {

        public boolean equals( Object o ) {
            return this == o;
        }
    }
}
