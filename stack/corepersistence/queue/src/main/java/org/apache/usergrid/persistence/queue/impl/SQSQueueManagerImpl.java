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

import com.amazonaws.AmazonClientException;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.queue.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SQSQueueManagerImpl<T> implements QueueManager {
    private static final Logger LOG = LoggerFactory.getLogger(SQSQueueManagerImpl.class);

    private final AmazonSQSClient sqs;
    private final QueueScope scope;
    private final QueueFig fig;
    private final ObjectMapper mapper;
    private Queue queue;
    private static SmileFactory smileFactory = new SmileFactory();


    @Inject
    public SQSQueueManagerImpl(@Assisted QueueScope scope, QueueFig fig){
        this.fig = fig;
        this.scope = scope;
        UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        this.sqs = new AmazonSQSClient(ugProvider.getCredentials());
        Regions regions = Regions.fromName(fig.getRegion());
        Region region = Region.getRegion(regions);
        sqs.setRegion(region);
        try {
            smileFactory.delegateToTextual(true);
            mapper = new ObjectMapper( smileFactory );
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");
        } catch ( Exception e ) {
            throw new RuntimeException("Error setting up mapper", e);
        }
    }

    public Queue createQueue(){
        String name = getName();
        CreateQueueRequest createQueueRequest = new CreateQueueRequest()
                .withQueueName(name);
        CreateQueueResult result = sqs.createQueue(createQueueRequest);
        String url = result.getQueueUrl();
        LOG.info("Created queue with url {}",url);
        return new Queue(url);
    }

    private String getName() {
        String name = scope.getApplication().getType() + scope.getApplication().getUuid().toString() + scope.getName();
        return name;
    }
    public Queue getQueue(){
        if(queue == null) {
            ListQueuesResult result =  sqs.listQueues();
            for (String queueUrl : result.getQueueUrls()) {
                boolean found = queueUrl.contains(getName());
                if (found) {
                    queue = new Queue(queueUrl);
                    break;
                }
            }
        }
        if(queue == null) {
            queue = createQueue();
        }
        return queue;
    }

    @Override
    public List<QueueMessage> getMessages(int limit, int transactionTimeout, int waitTime, Class klass) {
        waitTime = waitTime/1000;
        String url = getQueue().getUrl();
        LOG.info("Getting {} messages from {}",limit,url);
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(url);
        receiveMessageRequest.setMaxNumberOfMessages(limit);
        receiveMessageRequest.setVisibilityTimeout(transactionTimeout);
        receiveMessageRequest.setWaitTimeSeconds(waitTime);
        ReceiveMessageResult result = sqs.receiveMessage(receiveMessageRequest);
        List<Message> messages = result.getMessages();
        LOG.info("Received {} messages from {}",messages.size(),url);
        List<QueueMessage> queueMessages = new ArrayList<>(messages.size());
        for (Message message : messages) {
            Object body ;
            try{
                body = fromString(message.getBody(),klass);
            }catch (Exception e){
                LOG.error("failed to deserialize message", e);
                throw new RuntimeException(e);
            }
            QueueMessage queueMessage = new QueueMessage(message.getMessageId(),message.getReceiptHandle(),body);
            queueMessages.add(queueMessage);
        }
        return queueMessages;
    }

    @Override
    public void commitMessage(QueueMessage queueMessage) {
        String url = getQueue().getUrl();
        LOG.info("Commit message {} to queue {}",queueMessage.getMessageId(),url);

        sqs.deleteMessage(new DeleteMessageRequest()
                .withQueueUrl(url)
                .withReceiptHandle(queueMessage.getHandle()));
    }

    @Override
    public void sendMessages(List<Serializable> bodies) throws IOException {
        String url = getQueue().getUrl();
        LOG.info("Sending Messages...{} to {}",bodies.size(),url);

        SendMessageBatchRequest request = new SendMessageBatchRequest(url);
        List<SendMessageBatchRequestEntry> entries = new ArrayList<>(bodies.size());
        for(Serializable body : bodies){
            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            entry.setMessageBody(toString(body));
            entries.add(entry);
        }
        request.setEntries(entries);
        sqs.sendMessageBatch(request);

    }

    @Override
    public void sendMessage(Serializable body) throws IOException {
        String url = getQueue().getUrl();
        LOG.info("Sending Message...{} to {}",body.toString(),url);
        SendMessageRequest request = new SendMessageRequest(url,toString((Serializable)body));
        sqs.sendMessage(request);
    }

    @Override
    public void commitMessages(List<QueueMessage> queueMessages) {
        String url = getQueue().getUrl();
        LOG.info("Commit messages {} to queue {}",queueMessages.size(),url);
        List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();
        for(QueueMessage message : queueMessages){
            entries.add(new DeleteMessageBatchRequestEntry(message.getMessageId(),message.getHandle()));
        }
        DeleteMessageBatchRequest request = new DeleteMessageBatchRequest(url,entries);
        DeleteMessageBatchResult result = sqs.deleteMessageBatch(request);
        boolean successful = result.getFailed().size() > 0;
        if(!successful){
            LOG.error("Commit failed {} messages", result.getFailed().size());
        }
    }



    /** Read the object from Base64 string. */
    private Object fromString( String s, Class klass ) throws IOException, ClassNotFoundException {
        Object o =  mapper.readValue(s,klass);
        return o;
    }

    /** Write the object to a Base64 string. */
    private  String toString( Serializable o ) throws IOException {
        return mapper.writeValueAsString(o);
    }

    public class UsergridAwsCredentialsProvider implements AWSCredentialsProvider {

        private AWSCredentials creds;

        public  UsergridAwsCredentialsProvider(){
            init();
        }

        private void init() {
            creds = new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return StringUtils.trim(System.getProperty(SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR));
                }

                @Override
                public String getAWSSecretKey() {
                    return StringUtils.trim(System.getProperty(SDKGlobalConfiguration.SECRET_KEY_ENV_VAR));
                }
            };
            if(StringUtils.isEmpty(creds.getAWSAccessKeyId()) || StringUtils.isEmpty(creds.getAWSSecretKey()) ){
                throw new AmazonClientException("could not retrieve credentials from system properties");
            }
        }

        @Override
        public AWSCredentials getCredentials() {
            return creds;
        }


        @Override
        public void refresh() {
            init();
        }
    }
}
