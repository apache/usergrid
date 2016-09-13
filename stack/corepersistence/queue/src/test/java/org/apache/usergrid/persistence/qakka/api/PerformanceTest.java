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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.qakka.core.QueueMessage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.util.*;


public class PerformanceTest {
    private static final Logger logger = LoggerFactory.getLogger( PerformanceTest.class );

    
    @Test
    @Ignore("needs exernal Tomcat an Cassandra")
    public void testSendAndGetMessagePerformance() throws URISyntaxException, JsonProcessingException {

        Client client = ClientBuilder.newClient();
        
        WebTarget target = client.target("http://macsnoopdave2013:8080/api/");
        
        // create a queue

        String queueName = "pt_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        Map<String, Object> queueMap = new HashMap<String, Object>() {{ put("name", queueName); }};
        target.path("queues").request().post( Entity.entity( queueMap, MediaType.APPLICATION_JSON_TYPE));

        // send some messages
        int numMessages = 20000;

        {
            ObjectMapper mapper = new ObjectMapper();
            List<Long> times = new ArrayList<>( numMessages );
            int errorCount = 0;
            int counter = 0;
            
            for (int i = 0; i < numMessages; i++) {

                final int number = i;
                Map<String, Object> messageMap = new HashMap<String, Object>() {{
                    put( "message", "this is message #" + number );
                    put( "valid", true );
                }};
                String body = mapper.writeValueAsString( messageMap );

                long startTime = System.currentTimeMillis();
                Response post = target.path( "queues" ).path( queueName ).path( "messages" )
                        .request().post( Entity.entity( body, MediaType.APPLICATION_OCTET_STREAM_TYPE ) );
                long stopTime = System.currentTimeMillis();
                times.add( stopTime - startTime );

                if ( post.getStatus() != 200 ) {
                    errorCount++;
                }

                if ( ++counter % 500 == 0 ) {
                    logger.debug("Sent {} messages with error count {}", counter, errorCount);
                }

                try { Thread.sleep(5); } catch ( Exception intentionallyIgnored ) {};
            }

            Long total = times.stream().mapToLong( time -> time ).sum();
            Long max = times.stream().max( Comparator.comparing( time -> time ) ).get();
            Long min = times.stream().min( Comparator.comparing( time -> time ) ).get();
            Double average = times.stream().mapToLong( time -> time ).average().getAsDouble();

            logger.debug( "\n>>>>>>> Total send time {}ms, min {}ms, max {}ms, average {}ms errors {}\n\n", 
                    total, min, max, average, errorCount );
        }

        // get all messages, checking for dups

        {
            Set<UUID> messageIds = new HashSet<>();
            List<Long> times = new ArrayList<>( numMessages );
            int errorCount = 0;
            int counter = 0;
            
            for (int j = 0; j < numMessages; j++) {

                long startTime = System.currentTimeMillis();
                Response response = target.path( "queues" ).path( queueName ).path( "messages" ).request().get();
                long stopTime = System.currentTimeMillis();
                times.add( stopTime - startTime );

                if ( ++counter % 500 == 0 ) {
                    logger.debug("Got {} messages with error count {}", counter, errorCount);
                }
                
                if ( response .getStatus() != 200 ) {
                    errorCount++;
                    continue;
                }

                ApiResponse apiResponse = response.readEntity( ApiResponse.class );
                QueueMessage queueMessage = apiResponse.getQueueMessages().iterator().next();

                if (messageIds.contains( queueMessage.getQueueMessageId() )) {
                    Assert.fail( "Message fetched twice: " + queueMessage.getQueueMessageId() );
                } else {
                    messageIds.add( queueMessage.getQueueMessageId() );
                }
            }
            Assert.assertEquals( numMessages, messageIds.size() );

            Long total = times.stream().mapToLong( time -> time ).sum();
            Long max = times.stream().max( Comparator.comparing( time -> time ) ).get();
            Long min = times.stream().min( Comparator.comparing( time -> time ) ).get();
            Double average = times.stream().mapToLong( time -> time ).average().getAsDouble();

            logger.debug( "\n>>>>>>> Total get time {}ms, min {}ms, max {}ms, average {}ms errors {}\n\n", 
                    total, min, max, average, errorCount );
        }
    }
}
