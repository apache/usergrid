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
    //@Ignore
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

            //makes sure the that orginally visibility timeout on the queued message runs out before
            //we try to take it
            try {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e ) {
                e.printStackTrace();
            }
            Collection<AsynchronousMessage<TestEvent>> results = queue.take( 100, timeout );

            assertEquals( "Time elapsed", 1, results.size() );

            // Now we wait again so that the queue object has an expired visibility timeout to be sure the
            //message is deleted.
            try {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e ) {
                e.printStackTrace();
            }

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

    //@Ignore
    @Test
    public void queueReadTimeout() {

        final long timeout = 1;


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
            Thread.sleep( 1000 );


            Collection<AsynchronousMessage<TestEvent>> results = queue.take( 10, 50 ) ;
            long currentTimeMillis = System.currentTimeMillis();

            //takes from the queue, if 10 seconds have passed without accruing 100 elements
            //then the test fails. otherwise exit when you have 100 elements.
            while(results.size()!=100 && !has10SecondsPassed( currentTimeMillis )) {

                results.addAll( queue.take( 100, 50 ) );
            }

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
        catch ( InterruptedException e ) {
            e.printStackTrace();
        }
        finally {
            sqsAsyncClient.deleteQueue( queueName );
        }
    }

    public boolean has10SecondsPassed (long startTime){
        if(startTime+10000 > System.currentTimeMillis() ){
            return false;
        }
        return true;
    }


    @JsonTypeInfo( use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "@class" )
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
