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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.queue.Queue;
import org.apache.usergrid.persistence.queue.QueueFig;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueMessage;
import org.apache.usergrid.persistence.queue.QueueScope;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class SQSQueueManagerImpl implements QueueManager {
    private static final Logger LOG = LoggerFactory.getLogger(SQSQueueManagerImpl.class);


    private  final QueueScope scope;
    private  ObjectMapper mapper;
    private final QueueFig fig;
    private final AmazonSQSClient sqs;

    private static SmileFactory smileFactory = new SmileFactory();

    private LoadingCache<String, Queue> urlMap = CacheBuilder.newBuilder()
            .maximumSize( 1000 )
            .build( new CacheLoader<String, Queue>() {
                @Override
                public Queue load( String queueName ) throws Exception {

                    //the amazon client is not thread safe, we need to create one per queue
                    Queue queue = null;
                    try {
                        GetQueueUrlResult result = sqs.getQueueUrl( queueName );
                        queue = new Queue( result.getQueueUrl() );
                    }catch ( QueueDoesNotExistException queueDoesNotExistException ) {
                        //no op, swallow
                        LOG.error( "Queue {} does not exist, creating", queueName );

                    }
                    catch ( Exception e ) {
                        LOG.error( "failed to get queue from service", e );
                        throw e;
                    }
                    if ( queue == null ) {
                        CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName( queueName );
                        CreateQueueResult result = sqs.createQueue( createQueueRequest );
                        String url = result.getQueueUrl();
                        queue = new Queue( url );
                        LOG.info( "Created queue with url {}", url );
                    }
                    return queue;
                }
            } );


    @Inject
    public SQSQueueManagerImpl( @Assisted QueueScope scope, QueueFig fig ){
        this.scope = scope;
        this.fig = fig;
        try {

            smileFactory.delegateToTextual(true);
            mapper = new ObjectMapper( smileFactory );
            //pretty print, disabling for speed
//            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");

            sqs = createClient();

        } catch ( Exception e ) {
            throw new RuntimeException("Error setting up mapper", e);
        }
    }


    private String getName() {
        String name = fig.getPrefix() + "_" + scope.getName();

        Preconditions.checkArgument(name.length() <= 80, "Your name must be < than 80 characters");

        return name;
    }

    public Queue getQueue() {
        try {
            Queue queue = urlMap.get(getName());
            return queue;
        } catch (ExecutionException ee) {
            throw new RuntimeException(ee);
        }
    }

    @Override
    public List<QueueMessage> getMessages(int limit, int transactionTimeout, int waitTime, Class klass) {
        if(sqs == null){
            LOG.error("Sqs is null");
            return new ArrayList<>();
        }
        waitTime = waitTime/1000;
        String url = getQueue().getUrl();
        LOG.debug( "Getting {} messages from {}", limit, url);
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(url);
        receiveMessageRequest.setMaxNumberOfMessages(limit);
        receiveMessageRequest.setVisibilityTimeout(transactionTimeout);
        receiveMessageRequest.setWaitTimeSeconds(waitTime);
        ReceiveMessageResult result = sqs.receiveMessage(receiveMessageRequest);
        List<Message> messages = result.getMessages();
        LOG.debug( "Received {} messages from {}", messages.size(), url);
        List<QueueMessage> queueMessages = new ArrayList<>(messages.size());
        for (Message message : messages) {
            Object body ;
            try{
                body = fromString(message.getBody(),klass);
            }catch (Exception e){
                LOG.error("failed to deserialize message", e);
                throw new RuntimeException(e);
            }
            QueueMessage queueMessage = new QueueMessage(message.getMessageId(),message.getReceiptHandle(),body,message.getAttributes().get( "type" ));
            queueMessages.add(queueMessage);
        }
        return queueMessages;
    }

    @Override
    public void sendMessages(List bodies) throws IOException {
        if(sqs == null){
            LOG.error("Sqs is null");
            return;
        }
        String url = getQueue().getUrl();
        LOG.debug( "Sending Messages...{} to {}", bodies.size(), url);

        SendMessageBatchRequest request = new SendMessageBatchRequest(url);
        List<SendMessageBatchRequestEntry> entries = new ArrayList<>(bodies.size());
        for(Object body : bodies){
            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            entry.setId(UUID.randomUUID().toString());
            entry.setMessageBody( toString( body ) );
            entry.addMessageAttributesEntry( "type",new MessageAttributeValue().withStringValue( "mytype" ) );
            entries.add(entry);
        }
        request.setEntries(entries);
        sqs.sendMessageBatch(request);

    }

    @Override
    public void sendMessage(Object body) throws IOException {
        if(sqs == null){
            LOG.error("Sqs is null");
            return;
        }
        String url = getQueue().getUrl();
        LOG.debug( "Sending Message...{} to {}", body.toString(), url);

        final String stringBody = toString(body);

        SendMessageRequest request = new SendMessageRequest(url, stringBody);
        sqs.sendMessage(request);
    }


    @Override
    public void commitMessage(QueueMessage queueMessage) {
        String url = getQueue().getUrl();
        LOG.debug( "Commit message {} to queue {}", queueMessage.getMessageId(), url);

        sqs.deleteMessage(new DeleteMessageRequest()
                .withQueueUrl(url)
                .withReceiptHandle(queueMessage.getHandle()));
    }


    @Override
    public void commitMessages(List<QueueMessage> queueMessages) {
        String url = getQueue().getUrl();
        LOG.debug( "Commit messages {} to queue {}", queueMessages.size(), url);
        List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();
        for(QueueMessage message : queueMessages){
            entries.add(new DeleteMessageBatchRequestEntry(message.getMessageId(),message.getHandle()));
        }
        DeleteMessageBatchRequest request = new DeleteMessageBatchRequest(url,entries);
        DeleteMessageBatchResult result = sqs.deleteMessageBatch(request);
        boolean successful = result.getFailed().size() <= 0;
        if(!successful){
            for( BatchResultErrorEntry failed : result.getFailed()) {
                LOG.error("Commit failed reason: {} messages id: {}", failed.getMessage(),failed.getId());
            }
        }
    }



    /** Read the object from Base64 string. */
    private Object fromString( String s, Class klass ) throws IOException, ClassNotFoundException {
        Object o =  mapper.readValue(s,klass);
        return o;
    }

    /** Write the object to a Base64 string. */
    private  String toString( Object o ) throws IOException {
        return mapper.writeValueAsString(o);
    }


    /**
     * Get the region
     * @return
     */
    private Region getRegion() {
        Regions regions = Regions.fromName( fig.getRegion() );
        Region region = Region.getRegion( regions );
        return region;
    }


    /**
     * Create the SQS client for the specified settings
     */
    private AmazonSQSClient createClient() {
        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        final AmazonSQSClient sqs = new AmazonSQSClient( ugProvider.getCredentials() );
        final Region region = getRegion();
        sqs.setRegion( region );

        return sqs;
    }


}
