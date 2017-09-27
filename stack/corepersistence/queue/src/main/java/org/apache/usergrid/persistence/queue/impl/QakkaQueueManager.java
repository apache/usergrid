/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.queue.impl;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.persistence.qakka.core.*;
import org.apache.usergrid.persistence.qakka.core.Queue;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.queue.LegacyQueueManager;
import org.apache.usergrid.persistence.queue.LegacyQueueMessage;
import org.apache.usergrid.persistence.queue.LegacyQueueScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;


public class QakkaQueueManager implements LegacyQueueManager {
    private static final Logger logger = LoggerFactory.getLogger( QakkaQueueManager.class );

    private final LegacyQueueScope    scope;
    private final QueueManager        queueManager;
    private final QueueMessageManager queueMessageManager;
    private final Regions             regions;
    private final Set<String>         queueNames;


    @Inject
    public QakkaQueueManager(
        @Assisted LegacyQueueScope scope,
        QueueManager        queueManager,
        QueueMessageManager queueMessageManager,
        Regions             regions
    ) {
        this.scope = scope;
        this.queueManager = queueManager;
        this.queueMessageManager = queueMessageManager;
        this.regions = regions;
        this.queueNames = new HashSet<>();

        // Can't create queue here, actor system may not yet be started
        //createQueueIfNecessary();
    }


    private synchronized void createQueueIfNecessary() {

        if ( !queueNames.contains( scope.getName() ) && queueManager.getQueueConfig(scope.getName()) == null ) {

            // TODO: read defaults from config
            //queueManager.createQueue( new Queue( queueName, "test-type", region, region, 0L, 5, 10, null ));

            Queue queue = new Queue( scope.getName() );
            queueManager.createQueue( queue );
            queueNames.add( scope.getName() );
        }
    }


    private <T extends Serializable> void doSendMessage( T body, List<String> regions ) throws IOException {

        createQueueIfNecessary();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(body);
        oos.flush();
        oos.close();
        ByteBuffer byteBuffer = ByteBuffer.wrap( bos.toByteArray() );

        queueMessageManager.sendMessages(
            scope.getName(),
            regions,
            null, // delay millis
            null, // expiration seconds
            "application/octet-stream",
            DataType.serializeValue( byteBuffer, ProtocolVersion.NEWEST_SUPPORTED ));
    }


    @Override
    public <T extends Serializable> void sendMessageToLocalRegion(T body, Boolean async) throws IOException {
        List<String> regionsList = regions.getRegions( Regions.LOCAL );
        logger.trace( "Sending message to queue {} local region {}", scope.getName(), regionsList );
        doSendMessage( body, regionsList );
    }


    @Override
    public <T extends Serializable> void sendMessageToAllRegions(T body, Boolean async) throws IOException {
        List<String> regionsList = regions.getRegions( Regions.ALL );
        logger.trace( "Sending message to queue {} all regions {}", scope.getName(), regionsList );
        doSendMessage( body, regionsList );
    }


    @Override
    public List<LegacyQueueMessage> getMessages(int limit, Class klass) {

        createQueueIfNecessary();

        List<LegacyQueueMessage> messages = new ArrayList<>();
        List<QueueMessage> qakkaMessages = queueMessageManager.getNextMessages( scope.getName(), limit );

        for ( QueueMessage qakkaMessage : qakkaMessages ) {

            Object body;
            try {
                ByteBuffer messageData = queueMessageManager.getMessageData( qakkaMessage.getMessageId() );
                ByteBuffer bb = (ByteBuffer)DataType.blob().deserialize(
                    messageData, ProtocolVersion.NEWEST_SUPPORTED );

                ByteArrayInputStream bais = new ByteArrayInputStream( bb.array() );
                ObjectInputStream ios = new ObjectInputStream( bais );
                body = ios.readObject();

            } catch (Throwable t) {
                throw new QakkaRuntimeException( "Error de-serializing object", t );
            }

            LegacyQueueMessage legacyQueueMessage = new LegacyQueueMessage(
                qakkaMessage.getQueueMessageId().toString(),
                null,   // handle
                body,
                null);  // type

            messages.add( legacyQueueMessage );
        }

        return messages;
    }

    @Override
    public long getQueueDepth() {

        createQueueIfNecessary();

        return queueMessageManager.getQueueDepth( scope.getName(), DatabaseQueueMessage.Type.DEFAULT );
    }


    @Override
    public void commitMessage(LegacyQueueMessage queueMessage) {

        createQueueIfNecessary();

        if(logger.isTraceEnabled()){
            logger.trace("Committing message with id: {}", queueMessage.getMessageId());
        }

        UUID queueMessageId  = UUID.fromString( queueMessage.getMessageId() );
        queueMessageManager.ackMessage( scope.getName(), queueMessageId );
    }


    @Override
    public void commitMessages(List<LegacyQueueMessage> queueMessages) {

        for ( LegacyQueueMessage message : queueMessages ) {
            commitMessage( message );
        }
    }

    @Override
    public void sendMessagesAsync( List bodies ) throws IOException {
        sendMessages(bodies);
    }

    @Override
    public void sendMessages( List bodies ) throws IOException {

        for ( Object body : bodies ) {
            sendMessageToLocalRegion( (Serializable)body, null );
        }

    }


    @Override
    public List<LegacyQueueMessage> sendQueueMessages( List<LegacyQueueMessage> queueMessages ) throws IOException {

        List<LegacyQueueMessage> successMessages = new ArrayList<>();
        for ( LegacyQueueMessage queueMessage : queueMessages ) {
            sendMessageToLocalRegion( (Serializable)queueMessage.getBody() , null);
            successMessages.add(queueMessage);
        }

        return successMessages;
    }


    @Override
    public void deleteQueue() {
        queueManager.deleteQueue( scope.getName() );
    }


    @Override
    public void clearQueueNameCache(){
        queueNames.clear();
    }
}
