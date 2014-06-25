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
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.app.queue.Queue;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


public class AbstractQueueResourceIT extends AbstractRestIT {
    /**
     * Commands for editing queue information
     *
     * @author tnine
     */
    protected interface QueueCommand {

        /** Perform any modifications on the queue and return it */
        public Queue processQueue( Queue queue );
    }


    /**
     * Interface for handling responses from the queue (per message)
     *
     * @author tnine
     */
    protected interface ResponseHandler {
        /** Do something with the response */
        public void response( JsonNode node );

        /** Validate the results are correct */
        public void assertResults();
    }


    protected class QueueClient implements Callable<Void> {

        private ResponseHandler handler;
        private QueueCommand[] commands;
        private Queue queue;


        protected QueueClient( Queue queue, ResponseHandler handler, QueueCommand... commands ) {
            this.queue = queue;
            this.handler = handler;
            this.commands = commands;
        }


        /*
         * (non-Javadoc)
         *
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public Void call() throws Exception {
            List<JsonNode> entries = null;

            do {
                for ( QueueCommand command : commands ) {
                    queue = command.processQueue( queue );
                }

                entries = queue.getNextPage();

                for ( JsonNode entry : entries ) {
                    handler.response( entry );
                }
            }
            while ( entries.size() > 0 );

            return null;
        }
    }


    protected class NoLastCommand implements QueueCommand {

        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.rest.applications.queues.QueueResourceIT.QueueCommand#
         * processQueue(org.apache.usergrid.rest.test.resource.app.Queue)
         */
        @Override
        public Queue processQueue( Queue queue ) {
            return queue.withLast( null );
        }
    }


    protected class ClientId implements QueueCommand {

        private String clientId;


        public ClientId( String clientId ) {
            this.clientId = clientId;
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.rest.applications.queues.QueueResourceIT.QueueCommand#
         * processQueue(org.apache.usergrid.rest.test.resource.app.Queue)
         */
        @Override
        public Queue processQueue( Queue queue ) {
            return queue.withClientId( clientId );
        }
    }


    /**
     * Simple handler ensure we get up to count messages
     *
     * @author tnine
     */
    protected class IncrementHandler implements ResponseHandler {

        int max;
        AtomicInteger current = new AtomicInteger(0);


        protected IncrementHandler( int max ) {
            this.max = max;
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
         * #response(com.fasterxml.jackson.databind.JsonNode)
         */
        @Override
        public void response( JsonNode node ) {
        	int currentValue = current.getAndIncrement();
            if ( currentValue > max ) {
                fail( String.format( "Received %d messages, but we should only receive %d", current, max ) );
            }

            assertEquals( currentValue, node.get( "id" ).asInt() );
            // current++;
        }


        @Override
        public void assertResults() {
            assertEquals( max, current.get() );
        }
    }


    /**
     * Simple handler ensure we get up to count messages
     *
     * @author tnine
     */
    protected class DecrementHandler implements ResponseHandler {

        int max;
        AtomicInteger current;
        AtomicInteger count = new AtomicInteger(0);


        protected DecrementHandler( int max ) {
            this.max = max;
            current = new AtomicInteger(max - 1);
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
         * #response(com.fasterxml.jackson.databind.JsonNode)
         */
        @Override
        public void response( JsonNode node ) {
        	int currentValue = current.getAndDecrement();
            if ( currentValue < 0 ) {
                fail( String.format( "Received %d messages, but we should only receive %d", current, max ) );
            }

            assertEquals( currentValue, node.get( "id" ).asInt() );
            count.incrementAndGet();
            // current--;
            // count++;
        }


        @Override
        public void assertResults() {
            assertEquals( max, count.get() );
        }
    }


    /**
     * Simple handler ensure we get up to count messages from x to y ascending
     *
     * @author tnine
     */
    protected class ForwardMatchHandler implements ResponseHandler {

        int startValue;
        int count;
        int current = 0;


        protected ForwardMatchHandler( int startValue, int count ) {
            this.startValue = startValue;
            this.count = count;
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
         * #response(com.fasterxml.jackson.databind.JsonNode)
         */
        @Override
        public void response( JsonNode node ) {

            assertEquals( startValue + current, node.get( "id" ).asInt() );

            current++;
        }


        @Override
        public void assertResults() {
            // only ever invoked once
            assertEquals( count, current );
        }
    }


    /**
     * Simple handler ensure we get up to count messages from x to y ascending
     *
     * @author tnine
     */
    protected class ReverseMatchHandler implements ResponseHandler {

        int startValue;
        int count;
        int current = 0;


        protected ReverseMatchHandler( int startValue, int count ) {
            this.startValue = startValue;
            this.count = count;
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
         * #response(com.fasterxml.jackson.databind.JsonNode)
         */
        @Override
        public void response( JsonNode node ) {

            assertEquals( startValue - count, node.get( "id" ).asInt() );

            current++;
        }


        @Override
        public void assertResults() {
            // only ever invoked once
            assertEquals( count, current );
        }
    }


    /**
     * Simple handler to build a list of the message responses
     *
     * @author tnine
     */
    protected class TransactionResponseHandler extends IncrementHandler {

        List<JsonNode> responses = new ArrayList<JsonNode>();


        protected TransactionResponseHandler( int max ) {
            super( max );
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
         * #response(com.fasterxml.jackson.databind.JsonNode)
         */
        @Override
        public void response( JsonNode node ) {
            super.response( node );

            JsonNode transaction = node.get( "transaction" );

            assertNotNull( transaction );

            responses.add( node );
        }


        /** Get transaction ids from messages. Key is messageId, value is transactionId */
        public BiMap<String, String> getTransactionToMessageId() {
            BiMap<String, String> map = HashBiMap.create( responses.size() );

            for ( JsonNode message : responses ) {
                map.put( message.get( "uuid" ).asText(), message.get( "transaction" ).asText() );
            }

            return map;
        }


        /** Get all message ids from the response */
        public List<String> getMessageIds() {
            List<String> results = new ArrayList<String>( responses.size() );

            for ( JsonNode message : responses ) {
                results.add( message.get( "uuid" ).asText() );
            }

            return results;
        }
    }


    /**
     * Simple handler to build a list of the message responses asynchronously. Ensures that no responses are duplicated
     *
     * @author tnine
     */
    protected class AsyncTransactionResponseHandler implements ResponseHandler {

        private TreeMap<Integer, JsonNode> responses = new TreeMap<Integer, JsonNode>();
        private Map<Integer, String> threads = new HashMap<Integer, String>();
        private int max;


        protected AsyncTransactionResponseHandler( int max ) {
            this.max = max;
        }


        /*
         * (non-Javadoc)
         *
         * @see
         * org.apache.usergrid.rest.applications.queues.QueueResourceIT.ResponseHandler
         * #response(com.fasterxml.jackson.databind.JsonNode)
         */
        @Override
        public void response( JsonNode node ) {
            JsonNode transaction = node.get( "transaction" );

            assertNotNull( transaction );

            Integer id = node.get( "id" ).asInt();

            // we shouldn't have this response
            assertNull( String.format( "received id %d twice from thread %s and then thread %s", id, threads.get( id ),
                    Thread.currentThread().getName() ), threads.get( id ) );

            threads.put( id, Thread.currentThread().getName() );

            responses.put( id, node );
        }


        /** Get transaction ids from messages. Key is messageId, value is transactionId */
        public BiMap<String, String> getTransactionToMessageId() {
            BiMap<String, String> map = HashBiMap.create( responses.size() );

            for ( JsonNode message : responses.values() ) {
                map.put( message.get( "uuid" ).asText(), message.get( "transaction" ).asText() );
            }

            return map;
        }


        /** Get all message ids from the response */
        public List<String> getMessageIds() {
            List<String> results = new ArrayList<String>( responses.size() );

            for ( JsonNode message : responses.values() ) {
                results.add( message.get( "uuid" ).asText() );
            }

            return results;
        }


        @Override
        public void assertResults() {
            int count = 0;

            for ( JsonNode message : responses.values() ) {
                assertEquals( count, message.get( "id" ).asInt() );
                count++;
            }

            assertEquals( max, count );
        }
    }


    /**
     * Test that when receiving the messages from a queue, we receive the same amount as "count". Starts from 0 to
     * count-1 for message bodies. Client id optional
     *
     * @param queue the queue
     * @param handler the handler
     * @param commands the commands
     */
    protected void testMessages( Queue queue, ResponseHandler handler, QueueCommand... commands ) {
        List<JsonNode> entries = null;

        do {
            for ( QueueCommand command : commands ) {
                queue = command.processQueue( queue );
            }

            entries = queue.getNextPage();

            for ( JsonNode entry : entries ) {
                handler.response( entry );
            }
        }
        while ( entries.size() > 0 );
    }
}
