package org.apache.usergrid.persistence.graph.consistency;


import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 *
 *
 */
public class AmazonSimpleTimeoutQueueTest {
    @Test
    public void queueReadRemove() {

        AWSCredentials awsCredentials =
                new BasicAWSCredentials( System.getProperty( "accessKey" ), System.getProperty( "secretKey" ) );
        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient( awsCredentials );
        String queueName = "testRemove" + UUID.randomUUID();

        final long time = 1l;
        final long timeout = 1;
        final long extentedTimeout = 50;

        // when( timeService.getCurrentTime() ).thenReturn( time );

        TimeoutQueue queue = null;
        try {
            queue = new AmazonSimpleTimeoutQueue( queueName );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            assert ( false );
        }

        try {
            final TestEvent event = new TestEvent();


            AsynchronousMessage<TestEvent> asynchronousMessage = queue.queue( event, timeout );


            assertNotNull( asynchronousMessage );

            assertEquals( event, asynchronousMessage.getEvent() );
            assertEquals( timeout, asynchronousMessage.getTimeout() );

            Collection<AsynchronousMessage<TestEvent>> results = queue.take( 100, timeout );

            assertEquals( "Time not yet elapsed", 0, results.size() );

            //now elapse the time
            final long firstTime = time + timeout;

            try {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e ) {
                e.printStackTrace();
            }

            results = queue.take( 100, extentedTimeout );

            assertEquals( "Time elapsed", 1, results.size() );

            //validate we get a new timeout event since the old one was re-scheduled
            Iterator<AsynchronousMessage<TestEvent>> events = results.iterator();

            AsynchronousMessage<TestEvent> message = events.next();

            assertTrue( event.equals( message.getEvent() ) );


            //now remove it
            queue.remove( message );

            results = queue.take( 100, timeout );

            assertEquals( "Queue now empty", 0, results.size() );
        }
        finally {
            sqsAsyncClient.deleteQueue( queueName );
        }
    }


    @Test
    public void queueReadTimeout() {

        final long time = 1l;
        final long timeout = 5;
        final long extentedTimeout = 50;


        final int queueSize = 100;
        String queueName = "testTimeout" + UUID.randomUUID();
        AWSCredentials awsCredentials =
                new BasicAWSCredentials( System.getProperty( "accessKey" ), System.getProperty( "secretKey" ) );
        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient( awsCredentials );


        TimeoutQueue queue = null;
        try {
            queue = new AmazonSimpleTimeoutQueue( queueName );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            assert ( false );
        }
        try {

            Set<TestEvent> events = new HashSet<TestEvent>();

            for ( int i = 0; i < queueSize; i++ ) {

                final TestEvent event = new TestEvent();

                AsynchronousMessage<TestEvent> asynchronousMessage = queue.queue( event, timeout );

                events.add( event );

                assertNotNull( asynchronousMessage );

                assertEquals( event, asynchronousMessage.getEvent() );
                assertEquals( timeout, asynchronousMessage.getTimeout() );
            }


            Collection<AsynchronousMessage<TestEvent>> results = queue.take( 100, timeout );

           // assertEquals( "Time not yet elapsed", 0, results.size() );

            //now elapse the time
            final long firstTime = time + timeout;

            final int takeSize = 100;

            final int iterations = queueSize;

            //for ( int i = 0; i < iterations; i++ ) {

                //results = queue.take( takeSize, extentedTimeout );

//                if ( results.size() == 0 ) {
//                    break;
//                }

                assertEquals( "Time elapsed", 100, results.size() );

                //validate we get a new timeout event since the old one was re-scheduled
                Iterator<AsynchronousMessage<TestEvent>> eventIterator = results.iterator();

                while ( eventIterator.hasNext() ) {

                    AsynchronousMessage<TestEvent> message = eventIterator.next();

                    //assertTrue( events.remove( message.getEvent() ) );

                    //remove from our queue
                    boolean removed = queue.remove( message );

                    assertTrue( removed );
                }

//            assertEquals( "All elements dequeued", 0, events.size() );
        }
        finally {
            sqsAsyncClient.deleteQueue( queueName );
        }
    }


    @JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "@class" )
    public static class TestEvent implements Serializable {

        @JsonProperty
        UUID version = UUID.randomUUID();


        @Override
        public boolean equals( Object o ) {
            TestEvent derp = ( TestEvent ) o;
            return this.version.equals( derp.version );
        }
    }
}
