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


import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.app.queue.Queue;
import org.apache.usergrid.rest.test.resource.app.queue.Transaction;
import org.apache.usergrid.utils.MapUtils;

import com.google.common.collect.BiMap;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;



public class QueueResourceLong3IT extends AbstractQueueResourceIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    /** Tests that if we page before transaction expiration, we're always getting all elements consecutively */
    @Test
    public void transactionPageConsistent() throws InterruptedException, IOException {

        Queue queue = context.application().queues().queue( "test" );

        final int count = 1000;

        @SuppressWarnings("unchecked") Map<String, ?>[] data = new Map[count];

        for ( int i = 0; i < count; i++ ) {
            data[i] = MapUtils.hashMap( "id", i );
        }

        queue.post( data );

        // now consume and make sure we get each message. We should receive each
        // message, and we'll use this for comparing results later
        final long timeout = 20000;

        // read 50 messages at a time
        queue = queue.withTimeout( timeout ).withLimit( 120 );

        IncrementHandler incrementHandler = new IncrementHandler( count );

        testMessages( queue, incrementHandler, new NoLastCommand() );

        incrementHandler.assertResults();
    }


    @Test
    public void transaction10KMax() throws InterruptedException, IOException {

        Queue queue = context.application().queues().queue( "test" );
        queue.post( MapUtils.hashMap( "id", 0 ) );

        queue = queue.withTimeout( 10000 ).withLimit( 10001 );

        try {
            queue.getNextPage();
        }
        catch ( UniformInterfaceException uie ) {

            return;
        }

        fail( "An exception should be thrown" );
    }


    @Test
    public void transactionRenewal() throws InterruptedException, IOException {
        Queue queue = context.application().queues().queue( "test" );

        final int count = 2;

        for ( int i = 0; i < count; i++ ) {
            queue.post( MapUtils.hashMap( "id", i ) );
        }

        // now consume and make sure we get each message. We should receive each
        // message, and we'll use this for comparing results later
        final long timeout = 5000;

        queue = queue.withTimeout( timeout );

        TransactionResponseHandler transHandler = new TransactionResponseHandler( count );

        testMessages( queue, transHandler, new NoLastCommand() );

        transHandler.assertResults();

        List<String> originalMessageIds = transHandler.getMessageIds();
        BiMap<String, String> transactionInfo = transHandler.getTransactionToMessageId();

        // now read again, we shouldn't have any results because our timeout hasn't
        // lapsed
        IncrementHandler incrementHandler = new IncrementHandler( 0 );

        testMessages( queue, incrementHandler, new NoLastCommand() );

        incrementHandler.assertResults();

        // now sleep until our timeout expires
        Thread.sleep( timeout );

        // renew the transactions, then read. We shouldn't get any messages.
        List<String> returned = transHandler.getMessageIds();

        assertTrue( returned.size() > 0 );

        // compare the replayed messages and the make sure they're in the same order
        BiMap<String, String> newTransactions = transHandler.getTransactionToMessageId();

        for ( int i = 0; i < originalMessageIds.size(); i++ ) {
            // check the messages come back in the same order, they should
            assertEquals( originalMessageIds.get( i ), returned.get( i ) );

            assertNotNull( transactionInfo.get( originalMessageIds.get( i ) ) );

            // ack the transaction we were returned
            Transaction transaction =
                    queue.transactions().transaction( newTransactions.get( originalMessageIds.get( i ) ) );
            transaction.renew( timeout );
        }

        // now re-read our messages, we should not get any
        incrementHandler = new IncrementHandler( 0 );
        testMessages( queue, incrementHandler, new NoLastCommand() );

        incrementHandler.assertResults();

        // sleep again before testing the transactions time out (since we're not
        // renewing them)
        Thread.sleep( timeout );

        // now re-read our messages, we should get them all again
        transHandler = new TransactionResponseHandler( count );

        testMessages( queue, transHandler, new NoLastCommand() );

        transHandler.assertResults();

        returned = transHandler.getMessageIds();

        assertTrue( returned.size() > 0 );

        // compare the replayed messages and the make sure they're in the same order
        newTransactions = transHandler.getTransactionToMessageId();

        for ( int i = 0; i < originalMessageIds.size(); i++ ) {
            // check the messages come back in the same order, they should
            assertEquals( originalMessageIds.get( i ), returned.get( i ) );

            assertNotNull( transactionInfo.get( originalMessageIds.get( i ) ) );

            // ack the transaction we were returned
            Transaction transaction =
                    queue.transactions().transaction( newTransactions.get( originalMessageIds.get( i ) ) );
            transaction.delete();
        }

        // now sleep again we shouldn't have any messages since we acked all the
        // transactions
        Thread.sleep( timeout );

        incrementHandler = new IncrementHandler( 0 );

        testMessages( queue, incrementHandler, new NoLastCommand() );

        incrementHandler.assertResults();
    }
}
