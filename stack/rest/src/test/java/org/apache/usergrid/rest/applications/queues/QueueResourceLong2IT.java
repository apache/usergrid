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
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;



public class QueueResourceLong2IT extends AbstractQueueResourceIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void transactionPageSize() throws InterruptedException, IOException {

        Queue queue = context.application().queues().queue( "test" );

        final int count = 100;

        @SuppressWarnings("unchecked") Map<String, ?>[] data = new Map[count];

        for ( int i = 0; i < count; i++ ) {
            data[i] = MapUtils.hashMap( "id", i );
        }

        queue.post( data );

        // now consume and make sure we get each message. We should receive each
        // message, and we'll use this for comparing results later
        final long timeout = 20000;

        // read 50 messages at a time
        queue = queue.withTimeout( timeout ).withLimit( 50 );

        TransactionResponseHandler transHandler = new TransactionResponseHandler( count );

        testMessages( queue, transHandler, new NoLastCommand() );

        long start = System.currentTimeMillis();

        transHandler.assertResults();

        List<String> originalMessageIds = transHandler.getMessageIds();
        BiMap<String, String> transactionInfo = transHandler.getTransactionToMessageId();

        for (String originalMessageId : originalMessageIds) {
            // check the messages come back in the same order, they should
            assertEquals(originalMessageId, originalMessageId);

            assertNotNull(transactionInfo.get(originalMessageId));

            // ack the transaction we were returned
            Transaction transaction =
                    queue.transactions().transaction(transactionInfo.get(originalMessageId));
            transaction.delete();
        }

        // now sleep until our timeout expires
        Thread.sleep( Math.max( 0, timeout - ( System.currentTimeMillis() - start ) ) );

        IncrementHandler incrementHandler = new IncrementHandler( 0 );

        testMessages( queue, incrementHandler, new NoLastCommand() );

        incrementHandler.assertResults();
    }
}
