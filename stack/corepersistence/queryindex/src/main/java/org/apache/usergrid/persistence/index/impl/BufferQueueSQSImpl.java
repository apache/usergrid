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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.ActionRequestBuilder;

import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.apache.usergrid.persistence.queue.QueueMessage;
import org.apache.usergrid.persistence.queue.QueueScope;
import org.apache.usergrid.persistence.queue.impl.QueueScopeImpl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class BufferQueueSQSImpl implements BufferQueue {

    /** Hacky, copied from CPEntityManager b/c we can't access it here */
    public static final UUID MANAGEMENT_APPLICATION_ID = UUID.fromString( "b6768a08-b5d5-11e3-a495-11ddb1de66c8" );


    private static final String QUEUE_NAME = "es_queue";

    private final QueueManager queue;
    private final IndexFig indexFig;


    @Inject
    public BufferQueueSQSImpl( final QueueManagerFactory queueManagerFactory, final IndexFig indexFig ) {
        final QueueScope scope =
            new QueueScopeImpl( new SimpleId( MANAGEMENT_APPLICATION_ID, "application" ), QUEUE_NAME );

        this.queue = queueManagerFactory.getQueueManager( scope );
        this.indexFig = indexFig;
    }


    @Override
    public void offer( final IndexOperationMessage operation ) {
        final Message toQueue = new Message( operation.getOperations() );




        try {
            this.queue.sendMessage( toQueue );
            operation.getFuture().run();
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to queue message", e );
        }
    }


    @Override
    public List<IndexOperationMessage> take( final int takeSize, final long timeout, final TimeUnit timeUnit ) {

        //loop until we're we're full or we time out
        List<QueueMessage> messages = queue
            .getMessages( takeSize, indexFig.getIndexQueueTimeout(), ( int ) timeUnit.toMillis( timeout ),
                Message.class );


        final List<IndexOperationMessage> response = new ArrayList<>( messages.size() );

        for ( final QueueMessage message : messages ) {

            SqsIndexOperationMessage operation = new SqsIndexOperationMessage( message );

            operation.setOperations( ( ( Message ) message.getBody() ).getData() );

            response.add( operation );
        }

        return response;
    }


    @Override
    public void ack( final List<IndexOperationMessage> messages ) {

        List<QueueMessage> toAck = new ArrayList<>( messages.size() );

        for(IndexOperationMessage ioe: messages){
            toAck.add( ((SqsIndexOperationMessage)ioe).getMessage() );
        }

        queue.commitMessages( toAck );
    }


    /**
     * The message to queue to SQS
     */
    public static final class Message implements Serializable {
        private final Set<BatchRequest> data;


        private Message( final Set<BatchRequest> data ) {this.data = data;}


        public Set<BatchRequest> getData() {
            return data;
        }
    }


    /**
     * The message that subclasses our IndexOperationMessage.  holds a pointer to the original message
     */
    public class SqsIndexOperationMessage extends IndexOperationMessage {

        private final QueueMessage message;


        public SqsIndexOperationMessage( final QueueMessage message ) {this.message = message;}


        /**
         * Get the message from our queue
         */
        public QueueMessage getMessage() {
            return message;
        }
    }
}
