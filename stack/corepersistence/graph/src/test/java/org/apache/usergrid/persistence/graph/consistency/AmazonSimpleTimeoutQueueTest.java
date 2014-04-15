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

        final long timeout = 1;
        final long extentedTimeout = 50;

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

        final long timeout = 5;


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

                assertEquals( "Time elapsed", 100, results.size() );

                //validate we get a new timeout event since the old one was re-scheduled
                Iterator<AsynchronousMessage<TestEvent>> eventIterator = results.iterator();

                while ( eventIterator.hasNext() ) {

                    AsynchronousMessage<TestEvent> message = eventIterator.next();

                    //remove from our queue
                    boolean removed = queue.remove( message );

                    assertTrue( removed );
                }

        }
        finally {
            sqsAsyncClient.deleteQueue( queueName );
        }
    }


    @JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "@class" )
    public static class TestEvent implements Serializable {

        @JsonProperty
        String version = UUID.randomUUID().toString();


        @Override
        public boolean equals( Object o ) {
            TestEvent derp = ( TestEvent ) o;
            return this.version.equals( derp.version );
        }
    }
}
