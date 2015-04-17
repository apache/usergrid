/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.index;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.apache.usergrid.persistence.queue.QueueMessage;
import org.apache.usergrid.persistence.queue.QueueScope;
import org.apache.usergrid.persistence.queue.impl.QueueScopeImpl;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class SQSAsyncIndexService implements AsyncIndexService {


    private static final Logger logger = LoggerFactory.getLogger( SQSAsyncIndexService.class );

    /** Hacky, copied from CPEntityManager b/c we can't access it here */
    public static final UUID MANAGEMENT_APPLICATION_ID = UUID.fromString( "b6768a08-b5d5-11e3-a495-11ddb1de66c8" );


    /**
     * Set our TTL to 1 month.  This is high, but in the event of a bug, we want these entries to get removed
     */
    public static final int TTL = 60 * 60 * 24 * 30;

    /**
     * The name to put in the map
     */
    public static final String MAP_NAME = "esqueuedata";


    private static final String QUEUE_NAME = "es_queue";

    private static SmileFactory SMILE_FACTORY = new SmileFactory();

    static {
        SMILE_FACTORY.delegateToTextual( true );
    }


    private final QueueManager queue;
    private final QueryFig queryFig;
    private final ObjectMapper mapper;
    private final Meter readMeter;
    private final Timer readTimer;
    private final Meter writeMeter;
    private final Timer writeTimer;


    @Inject
    public SQSAsyncIndexService( final QueueManagerFactory queueManagerFactory, final QueryFig queryFig,
                                 final MetricsFactory metricsFactory ) {
        final QueueScope queueScope = new QueueScopeImpl( QUEUE_NAME );

        this.queue = queueManagerFactory.getQueueManager( queueScope );
        this.queryFig = queryFig;

        this.writeTimer = metricsFactory.getTimer( SQSAsyncIndexService.class, "write.timer" );
        this.writeMeter = metricsFactory.getMeter( SQSAsyncIndexService.class, "write.meter" );

        this.readTimer = metricsFactory.getTimer( SQSAsyncIndexService.class, "read.timer" );
        this.readMeter = metricsFactory.getMeter( SQSAsyncIndexService.class, "read.meter" );

        this.mapper = new ObjectMapper( SMILE_FACTORY );
        //pretty print, disabling for speed
        //            mapper.enable(SerializationFeature.INDENT_OUTPUT);

    }


    public void offer( final IndexEntityEvent operation ) {
        final Timer.Context timer = this.writeTimer.time();
        this.writeMeter.mark();

        final UUID identifier = UUIDGenerator.newTimeUUID();

        try {

            final String payLoad = toString( operation );

            //signal to SQS
            this.queue.sendMessage( identifier );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to queue message", e );
        }
        finally {
            timer.stop();
        }
    }


    public List<IndexEntityEvent> take( final int takeSize, final long timeout, final TimeUnit timeUnit ) {

        //SQS doesn't support more than 10

        final int actualTake = Math.min( 10, takeSize );

        final Timer.Context timer = this.readTimer.time();

        try {

            List<QueueMessage> messages = queue
                .getMessages( actualTake, queryFig.getIndexQueueTimeout(), ( int ) timeUnit.toMillis( timeout ),
                    String.class );


            final List<IndexEntityEvent> response = new ArrayList<>( messages.size() );

            final List<String> mapEntries = new ArrayList<>( messages.size() );


            if ( messages.size() == 0 ) {
                return Collections.emptyList();
            }

            //add all our keys  for a single round trip
            for ( final QueueMessage message : messages ) {
                mapEntries.add( message.getBody().toString() );
            }


            //load them into our response
            for ( final QueueMessage message : messages ) {

                final String payload = getBody( message );

                //now see if the key was there


                final IndexEntityEvent messageBody;

                try {
                    messageBody = fromString( payload );
                }
                catch ( IOException e ) {
                    logger.error( "Unable to deserialize message from string.  This is a bug", e );
                    throw new RuntimeException( "Unable to deserialize message from string.  This is a bug", e );
                }

                SqsIndexOperationMessage operation = new SqsIndexOperationMessage( message, messageBody );

                response.add( operation );
            }

            readMeter.mark( response.size() );
            return response;
        }
        //stop our timer
        finally {
            timer.stop();
        }
    }


    public void ack( final List<IndexEntityEvent> messages ) {

        //nothing to do
        if ( messages.size() == 0 ) {
            return;
        }

        List<QueueMessage> toAck = new ArrayList<>( messages.size() );

        for ( IndexEntityEvent ioe : messages ) {


            final SqsIndexOperationMessage sqsIndexOperationMessage = ( SqsIndexOperationMessage ) ioe;

            toAck.add( ( ( SqsIndexOperationMessage ) ioe ).getMessage() );
        }

        queue.commitMessages( toAck );
    }


    /** Read the object from Base64 string. */
    private IndexEntityEvent fromString( String s ) throws IOException {
        IndexEntityEvent o = mapper.readValue( s, IndexEntityEvent.class );
        return o;
    }


    /** Write the object to a Base64 string. */
    private String toString( IndexEntityEvent o ) throws IOException {
        return mapper.writeValueAsString( o );
    }


    private String getBody( final QueueMessage message ) {
        return message.getBody().toString();
    }


    /**
     * The message that subclasses our IndexOperationMessage.  holds a pointer to the original message
     */
    public class SqsIndexOperationMessage extends IndexEntityEvent {

        private final QueueMessage message;


        public SqsIndexOperationMessage( final QueueMessage message, final IndexEntityEvent source ) {
            super( source.getApplicationScope(), source.getEntityId(), source.getEntityVersion() );
            this.message = message;
        }


        /**
         * Get the message from our queue
         */
        public QueueMessage getMessage() {
            return message;
        }
    }


    @Override
    public void queueEntityIndexUpdate( final ApplicationScope applicationScope, final Id entityId,
                                        final UUID version ) {

    }
}
