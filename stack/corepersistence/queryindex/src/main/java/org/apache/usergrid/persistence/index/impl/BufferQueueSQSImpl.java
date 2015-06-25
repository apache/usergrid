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

package org.apache.usergrid.persistence.index.impl;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexOperationMessage;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;
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


/**
 * This is experimental at best.  Our SQS size limit is a problem.  We shouldn't use this for index operation. Only for
 * performing
 */
@Singleton
public class BufferQueueSQSImpl implements BufferQueue {

    private static final Logger logger = LoggerFactory.getLogger( BufferQueueSQSImpl.class );

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
    private final MapManager mapManager;
    private final IndexFig indexFig;
    private final ObjectMapper mapper;
    private final Meter readMeter;
    private final Timer readTimer;
    private final Meter writeMeter;
    private final Timer writeTimer;


    @Inject
    public BufferQueueSQSImpl( final QueueManagerFactory queueManagerFactory, final IndexFig indexFig,
                               final MapManagerFactory mapManagerFactory, final MetricsFactory metricsFactory ) {
        final QueueScope queueScope =
            new QueueScopeImpl( QUEUE_NAME );

        this.queue = queueManagerFactory.getQueueManager( queueScope );
        this.indexFig = indexFig;

        final MapScope scope = new MapScopeImpl( new SimpleId( MANAGEMENT_APPLICATION_ID, "application" ), MAP_NAME );

        this.mapManager = mapManagerFactory.createMapManager( scope );


        this.writeTimer = metricsFactory.getTimer( BufferQueueSQSImpl.class, "write.timer" );
        this.writeMeter = metricsFactory.getMeter( BufferQueueSQSImpl.class, "write.meter" );

        this.readTimer = metricsFactory.getTimer( BufferQueueSQSImpl.class, "read.timer" );
        this.readMeter = metricsFactory.getMeter( BufferQueueSQSImpl.class, "read.meter" );

        this.mapper = new ObjectMapper( SMILE_FACTORY );
        //pretty print, disabling for speed
        //            mapper.enable(SerializationFeature.INDENT_OUTPUT);

    }


    @Override
    public void offer( final IndexOperationMessage operation ) {

        //no op
        if(operation.isEmpty()){
            operation.getFuture().done();
            return;
        }

        final Timer.Context timer = this.writeTimer.time();
        this.writeMeter.mark();

        final UUID identifier = UUIDGenerator.newTimeUUID();

        try {

            final String payLoad = toString( operation );

            //write to cassandra
            this.mapManager.putString( identifier.toString(), payLoad, TTL );

            //signal to SQS
            this.queue.sendMessage( identifier );
            operation.done();
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to queue message", e );
        }
        finally {
            timer.stop();
        }
    }


    @Override
    public List<IndexOperationMessage> take( final int takeSize, final long timeout, final TimeUnit timeUnit ) {

        //SQS doesn't support more than 10

        final int actualTake = Math.min( 10, takeSize );

        final Timer.Context timer = this.readTimer.time();

        try {

            List<QueueMessage> messages = queue
                .getMessages( actualTake, indexFig.getIndexQueueTimeout(), ( int ) timeUnit.toMillis( timeout ),
                    String.class );



            final List<IndexOperationMessage> response = new ArrayList<>( messages.size() );

            final List<String> mapEntries = new ArrayList<>( messages.size() );


            if(messages.size() == 0){
                return response;
            }

            //add all our keys  for a single round trip
            for ( final QueueMessage message : messages ) {
                mapEntries.add( message.getBody().toString() );
            }

            //look up the values
            final Map<String, String> storedCommands = mapManager.getStrings( mapEntries );


            //load them into our response
            for ( final QueueMessage message : messages ) {

                final String key = getMessageKey( message );

                //now see if the key was there
                final String payload = storedCommands.get( key );

                //the entry was not present in cassandra, ignore this message.  Failure should eventually kick it to
                // a DLQ

                if ( payload == null ) {
                    continue;
                }

                final IndexOperationMessage messageBody;

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


    @Override
    public void ack( final List<IndexOperationMessage> messages ) {

        //nothing to do
        if ( messages.size() == 0 ) {
            return;
        }

        List<QueueMessage> toAck = new ArrayList<>( messages.size() );

        for ( IndexOperationMessage ioe : messages ) {


            final SqsIndexOperationMessage sqsIndexOperationMessage =   ( SqsIndexOperationMessage ) ioe;

            final String key = getMessageKey( sqsIndexOperationMessage.getMessage() );

            //remove it from the map
            mapManager.delete( key  );

            toAck.add( ( ( SqsIndexOperationMessage ) ioe ).getMessage() );
        }

        queue.commitMessages( toAck );
    }


    @Override
    public void fail( final List<IndexOperationMessage> messages, final Throwable t ) {
        //no op, just let it retry after the queue timeout
    }


    /** Read the object from Base64 string. */
    private IndexOperationMessage fromString( String s ) throws IOException {
        IndexOperationMessage o = mapper.readValue( s, IndexOperationMessage.class );
        return o;
    }


    /** Write the object to a Base64 string. */
    private String toString( IndexOperationMessage o ) throws IOException {
        return mapper.writeValueAsString( o );
    }

    private String getMessageKey(final QueueMessage message){
        return message.getBody().toString();
    }

    /**
     * The message that subclasses our IndexOperationMessage.  holds a pointer to the original message
     */
    public class SqsIndexOperationMessage extends IndexOperationMessage {

        private final QueueMessage message;


        public SqsIndexOperationMessage( final QueueMessage message, final IndexOperationMessage source ) {
            this.message = message;
            this.addAllDeIndexRequest( source.getDeIndexRequests() );
            this.addAllIndexRequest( source.getIndexRequests() );
        }


        /**
         * Get the message from our queue
         */
        public QueueMessage getMessage() {
            return message;
        }
    }
}
