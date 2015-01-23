/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest.applications.queues;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.mq.QueuePosition;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.app.queue.Queue;
import org.apache.usergrid.utils.MapUtils;

import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;



public class QueueResourceShortIT extends AbstractQueueResourceIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void inOrder() throws IOException {

        Queue queue = context.application().queues().queue( "test" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        IncrementHandler handler = new IncrementHandler( count );
        // now consume and make sure we get each message. We'll use the default for
        // this
        // test first
        testMessages( queue, handler, new NoLastCommand() );

        handler.assertResults();
    }


    @Test
    public void inOrderPaging() throws IOException {
        Queue queue = context.application().queues().queue( "test" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        queue = queue.withLimit( 15 );

        IncrementHandler handler = new IncrementHandler( count );

        // now consume and make sure we get each message. We'll use the default for
        // this
        // test first
        testMessages( queue, handler, new NoLastCommand() );

        handler.assertResults();
    }


    /** Read all messages with the client, then re-issue the reads from the start position to test we do this
     * properly */
    @Test
    public void startPaging() throws IOException {

        Queue queue = context.application().queues().queue( "test" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        queue = queue.withLimit( 15 );

        // now consume and make sure we get each message. We'll use the default for
        // this test first
        IncrementHandler handler = new IncrementHandler( count );

        testMessages( queue, handler, new NoLastCommand() );
        handler.assertResults();

        queue = queue.withPosition( QueuePosition.START.name() ).withLast( null );

        // now test it again, we should get same results when we explicitly read
        // from start and pass back the last
        handler = new IncrementHandler( count );
        testMessages( queue, handler );
        handler.assertResults();
    }


    @Test
    public void reverseOrderPaging() throws IOException {

        Queue queue = context.application().queues().queue( "test" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        queue = queue.withLimit( 15 );

        IncrementHandler handler = new IncrementHandler( count );

        testMessages( queue, handler );
        handler.assertResults();

        DecrementHandler decrement = new DecrementHandler( 30 );

        queue = queue.withLimit( 15 ).withPosition( QueuePosition.END.name() ).withLast( null );

        testMessages( queue, decrement );
        decrement.assertResults();
    }


    /** Tests that after delete, we can't receive messages */
    @Test
    public void delete() {

        Queue queue = context.application().queues().queue( "test" );

        try {
            queue.delete();
        }
        catch ( UniformInterfaceException uie ) {
            assertEquals( 501, uie.getResponse().getClientResponseStatus().getStatusCode() );
            return;
        }

        fail( "I shouldn't get here" );
    }


    /** Read messages ad-hoc with filtering */
    @Test
    @Ignore("Currently unsupported.  Needs fixed with iterators")
    public void filterForward() throws IOException {

        Queue queue = context.application().queues().queue( "test" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put( "name", "todd" );
            data.put( "id", i );
            data.put( "indexed", true );

            queue.post( data );
        }

        queue = queue.withLimit( 1 ).withPosition( QueuePosition.START.name() )
                     .withFilters( "name = 'todd'", "id >= 10", "id <= 20" ).withLast( null );

        // test it the first time, we should match
        ForwardMatchHandler handler = new ForwardMatchHandler( 10, 10 );
        testMessages( queue, handler );
        handler.assertResults();

        // test it again, shoudl still match
        handler = new ForwardMatchHandler( 10, 10 );
        testMessages( queue, handler );
        handler.assertResults();
    }


    /** Read messages ad-hoc with filtering */
    @Test
    @Ignore("Currently unsupported.  Needs fixed with iterators")
    public void filterReverse() throws IOException {

        Queue queue = context.application().queues().queue( "test" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "name", "todd" ).map( "id", String.valueOf( i ) ).map( "indexed", "true" ) );
        }

        queue = queue.withLimit( 1 ).withPosition( QueuePosition.END.name() )
                     .withFilters( "name = 'todd'", "id >= 20", "id <= 30" ).withLast( null );

        // test it the first time, we should match
        ReverseMatchHandler handler = new ReverseMatchHandler( 30, 10 );
        testMessages( queue, handler );
        handler.assertResults();

        // test it again, shoudl still match
        handler = new ReverseMatchHandler( 10, 10 );
        testMessages( queue, handler );
        handler.assertResults();
    }


    @Test
    public void topic() throws IOException {

        Queue queue = context.application().queues().queue( "test" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        // now consume and make sure we get each message. We'll use the default for
        // this
        // test first

        IncrementHandler handler = new IncrementHandler( count );
        testMessages( queue, handler, new ClientId( "client1" ), new NoLastCommand() );
        handler.assertResults();

        handler = new IncrementHandler( count );
        testMessages( queue, handler, new ClientId( "client2" ), new NoLastCommand() );
        handler.assertResults();

        // change back to client 1, and we shouldn't have anything
        // now consume and make sure we get each message. We'll use the default for
        // this
        // test first
        queue = queue.withClientId( "client1" );

        JsonNode node = queue.getNextEntry();

        assertNull( node );
    }


    @Test
    public void subscribe() throws IOException {

        Queue queue = context.application().queues().queue( "test" );

        queue.subscribers().subscribe( "testsub1" );
        queue.subscribers().subscribe( "testsub2" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        IncrementHandler handler = new IncrementHandler( count );

        testMessages( queue, handler, new NoLastCommand() );

        handler.assertResults();

        // now consume and make sure we get messages in the queue
        queue = context.application().queues().queue( "testsub1" );

        handler = new IncrementHandler( count );

        testMessages( queue, handler, new NoLastCommand() );

        handler.assertResults();

        handler = new IncrementHandler( count );

        queue = context.application().queues().queue( "testsub2" );

        testMessages( queue, handler, new NoLastCommand() );

        handler.assertResults();
    }


    /** Tests that after unsubscribing, we don't continue to deliver messages to other queues */
    @Test
    public void unsubscribe() throws IOException {

        Queue queue = context.application().queues().queue( "test" );

        queue.subscribers().subscribe( "testsub1" );
        queue.subscribers().subscribe( "testsub2" );

        final int count = 30;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        IncrementHandler handler = new IncrementHandler( count );

        testMessages( queue, handler, new NoLastCommand() );

        handler.assertResults();

        handler = new IncrementHandler( count );

        // now consume and make sure we get messages in the queue
        queue = context.application().queues().queue( "testsub1" );

        testMessages( queue, handler, new NoLastCommand() );
        handler.assertResults();

        handler = new IncrementHandler( count );

        queue = context.application().queues().queue( "testsub2" );

        testMessages( queue, handler, new NoLastCommand() );
        handler.assertResults();

        // now unsubscribe the second queue
        queue = context.application().queues().queue( "test" );

        queue.subscribers().unsubscribe( "testsub1" );

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        handler = new IncrementHandler( count );

        testMessages( queue, handler, new NoLastCommand() );
        handler.assertResults();

        // now consume and make sure we don't have messages in the ququq
        queue = context.application().queues().queue( "testsub1" );

        handler = new IncrementHandler( 0 );

        testMessages( queue, handler, new NoLastCommand() );

        handler.assertResults();

        queue = context.application().queues().queue( "testsub2" );

        handler = new IncrementHandler( count );

        testMessages( queue, handler, new NoLastCommand() );

        handler.assertResults();
    }


    @Test
    @Ignore("This is caused by timeuuids getting generated out of order within a millisecond.  Disabling until the "
            + "timeuuid issue is resolved next sprint.  For job scheduling, this is not an issue")
    public void concurrentConsumers() throws InterruptedException, ExecutionException, IOException {

        int consumerSize = 8;
        int count = 10000;
        int batchsize = 100;

        ExecutorService executor = Executors.newFixedThreadPool( consumerSize );

        Queue queue = context.application().queues().queue( "test" );

        // post the messages in batch
        for ( int i = 0; i < count / batchsize; i++ ) {

            @SuppressWarnings("unchecked") Map<String, ?>[] elements = new Map[batchsize];

            for ( int j = 0; j < batchsize; j++ ) {
                elements[j] = MapUtils.hashMap( "id", i * batchsize + j );
            }

            queue.post( elements );
        }

        // now consume and make sure we get each message. We should receive each
        // message, and we'll use this for comparing results later
        final long timeout = 60000;

        // set our timeout and read 10 messages at a time
        queue = queue.withTimeout( timeout ).withLimit( 10 );

        AsyncTransactionResponseHandler transHandler = new AsyncTransactionResponseHandler( count );

        NoLastCommand command = new NoLastCommand();

        List<Future<Void>> futures = new ArrayList<Future<Void>>( consumerSize );

        for ( int i = 0; i < consumerSize; i++ ) {
            Future<Void> future = executor.submit( new QueueClient( queue, transHandler, command ) );

            futures.add( future );
        }

        // wait for tests to finish
        for ( Future<Void> future : futures ) {
            future.get();
        }

        // now assert we're good.

        transHandler.assertResults();
    }
}
