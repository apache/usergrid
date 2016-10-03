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

package org.apache.usergrid.persistence.qakka.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.inject.Injector;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.api.impl.StartupListener;
import org.apache.usergrid.persistence.qakka.core.QueueMessage;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.fail;


public class QueueResourceTest extends AbstractRestTest {
    private static final Logger logger = LoggerFactory.getLogger( QueueResourceTest.class );

    static private final TypeReference<Map<String,Object>> jsonMapTypeRef
            = new TypeReference<Map<String,Object>>() {};

    @Test
    public void testCreateQueue() throws URISyntaxException {

        // create a queue

        String queueName = "qrt_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        Map<String, Object> queueMap = new HashMap<String, Object>() {{
            put("name", queueName);
        }};
        Response response = target("queues").request()
                .post( Entity.entity( queueMap, MediaType.APPLICATION_JSON_TYPE));

        Assert.assertEquals( 201, response.getStatus() );
        URIStrategy uriStrategy = StartupListener.INJECTOR.getInstance( URIStrategy.class );
        Assert.assertEquals( uriStrategy.queueURI( queueName ).toString(), response.getHeaderString( "location" ) );

        // get queue by name

        response = target("queues").path( queueName ).path( "config" ).request().get();
        Assert.assertEquals( 200, response.getStatus() );
        ApiResponse apiResponse = response.readEntity( ApiResponse.class );
        Assert.assertNotNull( apiResponse.getQueues() );
        Assert.assertFalse( apiResponse.getQueues().isEmpty() );
        Assert.assertEquals( 1, apiResponse.getQueues().size() );
        Assert.assertEquals( queueName, apiResponse.getQueues().iterator().next().getName() );

        response = target("queues").path( queueName ).queryParam( "confirm", true ).request().delete();
        Assert.assertEquals( 200, response.getStatus() );
    }


    @Test
    public void testDeleteQueue() throws URISyntaxException {

        // create a queue

        String queueName = "qrt_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        Map<String, Object> queueMap = new HashMap<String, Object>() {{ put("name", queueName); }};
        Response response = target("queues").request()
                .post( Entity.entity( queueMap, MediaType.APPLICATION_JSON_TYPE));

        Assert.assertEquals( 201, response.getStatus() );
        URIStrategy uriStrategy = StartupListener.INJECTOR.getInstance( URIStrategy.class );
        Assert.assertEquals( uriStrategy.queueURI( queueName ).toString(), response.getHeaderString( "location" ) );

        // delete queue without confirm = true, should fail with bad request

        response = target("queues").path( queueName ).request().delete();
        Assert.assertEquals( 400, response.getStatus() );

        // delete queue with confirm = true

        response = target("queues").path( queueName ).queryParam( "confirm", true ).request().delete();
        Assert.assertEquals( 200, response.getStatus() );

        // cannot get queue by name

        response = target("queues").path( queueName ).path( "config" ).request().get();
        Assert.assertEquals( 404, response.getStatus() );
    }


    @Test
    public void testSendMessageToBadQueue() throws URISyntaxException, JsonProcessingException, InterruptedException {

        String queueName = "bogus_queue_is_bogus";
        Map<String, Object> messageMap = new HashMap<String, Object>() {{ put("dummy_prop", "dummy_value"); }};
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString( messageMap );

        Response response = target("queues").path( queueName ).path( "messages" )
                .request().post( Entity.entity( body, MediaType.APPLICATION_OCTET_STREAM_TYPE ));

        Assert.assertEquals( 404, response.getStatus() );
    }


    @Test
    public void testSendJsonMessagesAsJson() throws URISyntaxException, IOException, InterruptedException {
        sendJsonMessages( true );
    }


    @Test
    public void testSendMessagesJsonAsOctetStream() throws URISyntaxException, IOException, InterruptedException {
        sendJsonMessages( false );
    }


    /**
     * Send 100 JSON payload messages to queue.
     * @param asJson True to send with content-type header 'application/json'
     *               False to send with content-type header 'application/octet stream'
     */
    private void sendJsonMessages( boolean asJson ) throws URISyntaxException, IOException, InterruptedException {

        // create a queue

        String queueName = "qrt_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        Map<String, Object> queueMap = new HashMap<String, Object>() {{
            put( "name", queueName );
        }};
        target( "queues" ).request().post( Entity.entity( queueMap, MediaType.APPLICATION_JSON_TYPE ) );

        // send some messages

        ObjectMapper mapper = new ObjectMapper();

        int numMessages = 100;
        for (int i = 0; i < numMessages; i++) {

            final int number = i;
            Map<String, Object> messageMap = new HashMap<String, Object>() {{
                put( "message", "this is message #" + number );
                put( "valid", true );
            }};
            String body = mapper.writeValueAsString( messageMap );

            Response response;
            if ( asJson ) {
                response = target( "queues" ).path( queueName ).path( "messages" )
                        .request().post( Entity.entity( body, MediaType.APPLICATION_JSON ) );
            } else {
                response = target( "queues" ).path( queueName ).path( "messages" )
                        .queryParam( "contentType", MediaType.APPLICATION_JSON )
                        .request().post( Entity.entity( body, MediaType.APPLICATION_OCTET_STREAM ) );
            }

            Assert.assertEquals( 200, response.getStatus() );
        }

        // get all messages, checking for dups

        checkJsonMessages( queueName, numMessages );

        Response response = target( "queues" ).path( queueName ).queryParam( "confirm", true ).request().delete();
        Assert.assertEquals( 200, response.getStatus() );
    }


    private Set<UUID> checkJsonMessages( String queueName, int numMessages ) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        Set<UUID> messageIds = new HashSet<>();
        for ( int j=0; j<numMessages; j++ ) {

            int retries = 0;
            int maxRetries = 10;
            ApiResponse apiResponse = null;
            while ( retries++ < maxRetries ) {
                Response response = target( "queues" ).path( queueName ).path( "messages" ).request().get();
                apiResponse = response.readEntity( ApiResponse.class );
                if ( !apiResponse.getQueueMessages().isEmpty() ) {
                    break;
                }
                try { Thread.sleep(500); } catch (Exception ignored) {}
            }

            Assert.assertNotNull(   apiResponse );
            Assert.assertNotNull(   apiResponse.getQueueMessages() );
            Assert.assertEquals( 1, apiResponse.getQueueMessages().size() );

            QueueMessage queueMessage = apiResponse.getQueueMessages().iterator().next();
            Map<String, Object> payload = mapper.readValue( queueMessage.getData(), jsonMapTypeRef );

            Assert.assertEquals( queueName, queueMessage.getQueueName() );
            Assert.assertNull( queueMessage.getHref() );
            Assert.assertEquals( true, payload.get("valid") );

            if (messageIds.contains( queueMessage.getQueueMessageId() )) {
                Assert.fail("Message fetched twice: " + queueMessage.getQueueMessageId() );
            } else {
                messageIds.add( queueMessage.getQueueMessageId() );
            }
        }
        Assert.assertEquals( numMessages, messageIds.size() );

        return messageIds;
    }


    @Test
    public void testSendBinaryMessages() throws URISyntaxException, IOException, InterruptedException {

        // create a queue

        String queueName = "qrt_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        Map<String, Object> queueMap = new HashMap<String, Object>() {{
            put( "name", queueName );
        }};
        target( "queues" ).request().post( Entity.entity( queueMap, MediaType.APPLICATION_JSON_TYPE ) );

        // send messages each with image/jpg payload

        InputStream is = getClass().getResourceAsStream("/qakka-duck.jpg");
        byte[] bytes = ByteStreams.toByteArray( is );

        int numMessages = 100;
        for (int i = 0; i < numMessages; i++) {

            Response response = target( "queues" ).path( queueName ).path( "messages" )
                    .queryParam( "contentType", "image/jpg" )
                    .request()
                    .post( Entity.entity( bytes, MediaType.APPLICATION_OCTET_STREAM ));

            Assert.assertEquals( 200, response.getStatus() );
        }

        // get all messages, checking for dups

        checkBinaryMessages( queueName, numMessages );

        Response response = target( "queues" ).path( queueName ).queryParam( "confirm", true ).request().delete();
        Assert.assertEquals( 200, response.getStatus() );
    }


    private Set<UUID> checkBinaryMessages( String queueName, int numMessages ) throws IOException {

        Set<UUID> messageIds = new HashSet<>();
        for ( int j=0; j<numMessages; j++ ) {

            Response response = target( "queues" ).path( queueName ).path( "messages" ).request().get();

            ApiResponse apiResponse = response.readEntity( ApiResponse.class );
            Assert.assertNotNull(   apiResponse.getQueueMessages() );
            Assert.assertFalse(     apiResponse.getQueueMessages().isEmpty() );
            Assert.assertEquals( 1, apiResponse.getQueueMessages().size() );

            QueueMessage queueMessage = apiResponse.getQueueMessages().iterator().next();

            // no data in a binary message
            Assert.assertNull( queueMessage.getData() );

            // data can be found at HREF provided
            Assert.assertNotNull( queueMessage.getHref() );

            Response binaryResponse = target("queues")
                    .path( queueName ).path("data").path( queueMessage.getQueueMessageId().toString() )
                    .request().accept( "image/jpg" ).get();

            Assert.assertEquals( 200, binaryResponse.getStatus() );
            InputStream is = binaryResponse.readEntity( InputStream.class );

            byte[] imageBytes = ByteStreams.toByteArray( is );
            Assert.assertEquals( 11188, imageBytes.length);

            if (messageIds.contains( queueMessage.getQueueMessageId() )) {
                fail("Message fetched twice: " + queueMessage.getQueueMessageId() );
            } else {
                messageIds.add( queueMessage.getQueueMessageId() );
            }
        }
        Assert.assertEquals( numMessages, messageIds.size() );

        return messageIds;
    }


    @Test
    public void testSendMessageAckAndTimeout() throws URISyntaxException, IOException, InterruptedException {

        // create a queue

        String queueName = "qrt_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        Map<String, Object> queueMap = new HashMap<String, Object>() {{ put("name", queueName); }};
        target("queues").request().post( Entity.entity( queueMap, MediaType.APPLICATION_JSON_TYPE));

        // send some messages

        ObjectMapper mapper = new ObjectMapper();

        int numMessages = 100;
        for ( int i=0; i<numMessages; i++ ) {

            final int number = i;
            Map<String, Object> messageMap = new HashMap<String, Object>() {{
                put("message", "this is message #" + number);
                put("valid", true );
            }};
            String body = mapper.writeValueAsString( messageMap );

            Response response = target("queues").path( queueName ).path( "messages" )
                    .request().post( Entity.entity( body, MediaType.APPLICATION_JSON ));

            Assert.assertEquals( 200, response.getStatus() );
        }

        // get all messages, checking for dups

        Set<UUID> messageIds = checkJsonMessages( queueName, numMessages );

        // there should be no more messages available

        Response response = target( "queues" ).path( queueName ).path( "messages" ).request().get();
        ApiResponse apiResponse = response.readEntity( ApiResponse.class );
        Assert.assertNotNull( apiResponse.getQueueMessages() );
        Assert.assertTrue( apiResponse.getQueueMessages().isEmpty() );

        // ack half of the messages

        int count = 0;
        Set<UUID> ackedIds = new HashSet<>();
        for ( UUID queueMessageId : messageIds ) {
            response = target( "queues" )
                    .path( queueName ).path( "messages" ).path( queueMessageId.toString() ).request().delete();
            Assert.assertEquals( 200, response.getStatus() );
            ackedIds.add( queueMessageId );
            if ( ++count >= numMessages/2 ) {
                break;
            }
        }
        messageIds.removeAll( ackedIds );

        // wait for remaining of the messages to timeout

        QakkaFig qakkaFig = StartupListener.INJECTOR.getInstance( QakkaFig.class );
        Thread.sleep( 2*qakkaFig.getQueueTimeoutSeconds() * 1000 );

        // now, the remaining messages cannot be acked because they timed out

        for ( UUID queueMessageId : messageIds ) {
            response = target( "queues" )
                    .path( queueName ).path( "messages" ).path( queueMessageId.toString() ).request().delete();
            Assert.assertEquals( 400, response.getStatus() );
        }

        // and, those same messages should be available again in the queue

        checkJsonMessages( queueName, numMessages/2 );

        response = target( "queues" ).path( queueName ).queryParam( "confirm", true ).request().delete();
        Assert.assertEquals( 200, response.getStatus() );
    }


    @Test
    public void testConvertDelayParameter() {

        Injector injector = StartupListener.INJECTOR;
        QueueResource queueResource = injector.getInstance( QueueResource.class );

        Assert.assertEquals( 0L, queueResource.convertDelayParameter( "" ).longValue() );
        Assert.assertEquals( 0L, queueResource.convertDelayParameter( "0" ).longValue() );
        Assert.assertEquals( 0L, queueResource.convertDelayParameter( "NONE" ).longValue() );
        Assert.assertEquals( 5L, queueResource.convertDelayParameter( "5" ).longValue() );

        try {
            queueResource.convertDelayParameter( "bogus value" );
            fail("Expected exception on bad value");
        } catch ( IllegalArgumentException expected ) {
            // pass
        }
    }

    @Test
    public void testConvertExpirationParameter() {

        Injector injector = StartupListener.INJECTOR;
        QueueResource queueResource = injector.getInstance( QueueResource.class );

        Assert.assertNull( queueResource.convertExpirationParameter( "" ) );
        Assert.assertNull( queueResource.convertExpirationParameter( "NEVER" ) );

        Assert.assertEquals( 5L, queueResource.convertExpirationParameter( "5" ).longValue() );

        try {
            queueResource.convertExpirationParameter( "bogus value" );
            fail("Expected exception on bad value");
        } catch ( IllegalArgumentException expected ) {
            // pass
        }
    }

}

