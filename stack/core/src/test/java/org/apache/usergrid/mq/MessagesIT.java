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
package org.apache.usergrid.mq;


import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.utils.ImmediateCounterRule;
import org.apache.usergrid.utils.JsonUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



public class MessagesIT extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger( MessagesIT.class );

    @Rule
    public ImmediateCounterRule counterRule = new ImmediateCounterRule( );

    public MessagesIT() {
        super();
    }


    @Ignore("Pending https://issues.apache.org/jira/browse/USERGRID-1116. ")
    @Test
    public void testMessages() throws Exception {
        logger.info( "MessagesIT.testMessages" );

        Message message = new Message();
        message.setStringProperty( "foo", "bar" );
        logger.info( JsonUtils.mapToFormattedJsonString( message ) );

        logger.info( "Posting message #1 to queue /foo/bar" );

        QueueManager qm = app.getQm();
        qm.postToQueue( "/foo/bar", message );

        logger.info( "Getting message #1" );

        message = qm.getMessage( message.getUuid() );
        logger.info( JsonUtils.mapToFormattedJsonString( message ) );

        logger.info( "Getting message from /foo/bar, should be message #1" );

        QueueResults messages = qm.getFromQueue( "/foo/bar", null );
        logger.info( JsonUtils.mapToFormattedJsonString( messages ) );
        assertEquals( 1, messages.size() );

        logger.info( "Getting message from /foo/bar, should empty" );

        messages = qm.getFromQueue( "/foo/bar", null );
        logger.info( JsonUtils.mapToFormattedJsonString( messages ) );
        assertEquals( 0, messages.size() );

        message = new Message();
        message.setStringProperty( "name", "alpha" );
        qm.postToQueue( "/foo/bar", message );

        message = new Message();
        message.setStringProperty( "name", "bravo" );
        qm.postToQueue( "/foo/bar", message );
/*
        messages = qm.getFromQueue("/foo/bar", null);
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(1, messages.size());

		messages = qm.getFromQueue("/foo/bar", null);
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(1, messages.size());

		messages = qm.getFromQueue("/foo/bar", null);
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(0, messages.size());

		messages = qm.getFromQueue("/foo/bar",
				new QueueQuery().withPosition(QueuePosition.END)
						.withPreviousCount(3));
		logger.info(JsonUtils.mapToFormattedJsonString(messages));
		assertEquals(3, messages.size());
*/

        //wait for counters for flush\

        //TODO Re-evaluate queues and make a cleaner interface
//        Map<String, Long> counters = qm.getQueueCounters( "/" );
//        logger.info( "dumping counters...." + counters );
//        logger.info( JsonUtils.mapToFormattedJsonString( counters ) );
//        assertEquals( 1, counters.size() );
//        assertNotNull( counters.get( "/foo/bar/" ) );
//        assertEquals( new Long( 3 ), counters.get( "/foo/bar/" ) );
    }


    @Ignore("Pending https://issues.apache.org/jira/browse/USERGRID-1116. ")
    @Test
    public void testSubscriberSearch() throws Exception {
        QueueManager qm = app.getQm();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "alpha" );
        Queue q = qm.updateQueue( "/foo/1/", properties );
        logger.info( JsonUtils.mapToFormattedJsonString( q ) );

        q = qm.getQueue( "/foo/1/" );
        logger.info( JsonUtils.mapToFormattedJsonString( q ) );
        assertEquals( "alpha", q.getStringProperty( "foo" ) );

        properties = new HashMap<String, Object>();
        properties.put( "foo", "bravo" );
        q = qm.updateQueue( "/foo/2/", properties );
        logger.info( JsonUtils.mapToFormattedJsonString( q ) );

        properties = new HashMap<String, Object>();
        properties.put( "foo", "charlie" );
        q = qm.updateQueue( "/foo/3/", properties );
        logger.info( JsonUtils.mapToFormattedJsonString( q ) );

        qm.subscribeToQueue( "/pubtest/", "/foo/1/" );
        qm.subscribeToQueue( "/pubtest/", "/foo/2/" );
        qm.subscribeToQueue( "/pubtest/", "/foo/3/" );

        QueueSet results = qm.searchSubscribers( "/pubtest/", Query.findForProperty( "foo", "bravo" ) );
        logger.info( JsonUtils.mapToFormattedJsonString( results ) );
        assertEquals( 1, results.size() );

        properties = new HashMap<String, Object>();
        properties.put( "foo", "delta" );
        q = qm.updateQueue( "/foo/2/", properties );
        logger.info( JsonUtils.mapToFormattedJsonString( q ) );

        results = qm.searchSubscribers( "/pubtest/", Query.findForProperty( "foo", "bravo" ) );
        logger.info( JsonUtils.mapToFormattedJsonString( results ) );
        assertEquals( 0, results.size() );

        results = qm.searchSubscribers( "/pubtest/", Query.findForProperty( "foo", "delta" ) );
        logger.info( JsonUtils.mapToFormattedJsonString( results ) );
        assertEquals( 1, results.size() );

        qm.unsubscribeFromQueue( "/pubtest/", "/foo/2/" );

        results = qm.searchSubscribers( "/pubtest/", Query.findForProperty( "foo", "delta" ) );
        logger.info( JsonUtils.mapToFormattedJsonString( results ) );
        assertEquals( 0, results.size() );
    }


    @Ignore("Pending https://issues.apache.org/jira/browse/USERGRID-1116. ")
    @Test
    public void testConsumer() throws Exception {
        logger.info( "Creating messages" );

        QueueManager qm = app.getQm();
        Message message;

        for ( int i = 0; i < 10; i++ ) {
            message = new Message();
            message.setStringProperty( "foo", "bar" + i );

            logger.info( "Posting message #" + i + " to queue /foo/bar: " + message.getUuid() );

            qm.postToQueue( "/foo/bar", message );
        }

        for ( int i = 0; i < 11; i++ ) {
            QueueResults messages = qm.getFromQueue( "/foo/bar", new QueueQuery().withConsumer( "consumer1" ) );
            logger.info( JsonUtils.mapToFormattedJsonString( messages ) );
            if ( i < 10 ) {
                assertEquals( 1, messages.size() );
                assertEquals( "bar" + i, messages.getMessages().get( 0 ).getStringProperty( "foo" ) );
            }
            else {
                assertEquals( 0, messages.size() );
            }
        }

        for ( int i = 0; i < 11; i++ ) {
            QueueResults messages = qm.getFromQueue( "/foo/bar", new QueueQuery().withConsumer( "consumer2" ) );
            logger.info( JsonUtils.mapToFormattedJsonString( messages ) );
            if ( i < 10 ) {
                assertEquals( 1, messages.size() );
                assertEquals( "bar" + i, messages.getMessages().get( 0 ).getStringProperty( "foo" ) );
            }
            else {
                assertEquals( 0, messages.size() );
            }
        }
    }


    @Ignore("Pending https://issues.apache.org/jira/browse/USERGRID-1116. ")
    @Test
    public void testTransactions() throws Exception {
        QueueManager qm = app.getQm();

        String queuePath = "/foo/bar";

        assertFalse( qm.hasMessagesInQueue( queuePath, null ) );
        assertFalse( qm.hasOutstandingTransactions( queuePath, null ) );
        assertFalse( qm.hasPendingReads( queuePath, null ) );

        // create 2 messages
        Message message = new Message();
        message.setStringProperty( "foo", "bar" );
        logger.info( "Posting message to queue " + queuePath + ": " + message.getUuid() );
        Message posted1 = qm.postToQueue( queuePath, message );

        assertTrue( qm.hasMessagesInQueue( queuePath, null ) );
        assertFalse( qm.hasOutstandingTransactions( queuePath, null ) );
        assertTrue( qm.hasPendingReads( queuePath, null ) );

        message = new Message();
        message.setStringProperty( "foo", "bar" );
        logger.info( "Posting message to queue " + queuePath + ": " + message.getUuid() );
        Message posted2 = qm.postToQueue( queuePath, message );

        assertTrue( qm.hasMessagesInQueue( queuePath, null ) );
        assertFalse( qm.hasOutstandingTransactions( queuePath, null ) );
        assertTrue( qm.hasPendingReads( queuePath, null ) );

        // take 1 message
        QueueQuery qq = new QueueQuery();
        qq.setTimeout( 60000000 );
        qq.setLimit( 1 );
        QueueResults qr1 = qm.getFromQueue( queuePath, qq );

        assertEquals( "Only 1 message returned", 1, qr1.getMessages().size() );
        assertEquals( "Expected message 1", posted1.getUuid(), qr1.getLast() );
        assertEquals( "Expected message 1", posted1.getUuid(), qr1.getMessages().get( 0 ).getUuid() );
        assertNotNull( "Expected transaction id", qr1.getMessages().get( 0 ).getTransaction() );

        assertTrue( qm.hasMessagesInQueue( queuePath, null ) );
        assertTrue( qm.hasOutstandingTransactions( queuePath, null ) );
        assertTrue( qm.hasPendingReads( queuePath, null ) );

        // take the 2nd message
        QueueResults qr2 = qm.getFromQueue( queuePath, qq );


        assertEquals( "Only 1 message returned", 1, qr2.getMessages().size() );
        assertEquals( "Expected message 2", posted2.getUuid(), qr2.getLast() );
        assertEquals( "Expected message 2", posted2.getUuid(), qr2.getMessages().get( 0 ).getUuid() );
        assertNotNull( "Expected transaction id", qr2.getMessages().get( 0 ).getTransaction() );


        assertFalse( "Both messages have been returned at least once", qm.hasMessagesInQueue( queuePath, null ) );
        assertTrue( "Two outstanding transactions left", qm.hasOutstandingTransactions( queuePath, null ) );
        assertTrue( qm.hasPendingReads( queuePath, null ) );

        // commit the 1st transaction
        qm.deleteTransaction( queuePath, qr1.getMessages().get( 0 ).getTransaction(), qq );

        assertFalse( "Both messages have been returned at least once", qm.hasMessagesInQueue( queuePath, null ) );
        assertTrue( "One outstanding transaction is left", qm.hasOutstandingTransactions( queuePath, null ) );
        assertTrue( qm.hasPendingReads( queuePath, null ) );

        // commit the 2nd transaction
        qm.deleteTransaction( queuePath, qr2.getMessages().get( 0 ).getTransaction(), qq );

        assertFalse( "Both messages have been returned at least once", qm.hasMessagesInQueue( queuePath, null ) );
        assertFalse( "Both transactions have been removed", qm.hasOutstandingTransactions( queuePath, null ) );
        assertFalse( "Both messages and transactions have been returned", qm.hasPendingReads( queuePath, null ) );
    }
}
