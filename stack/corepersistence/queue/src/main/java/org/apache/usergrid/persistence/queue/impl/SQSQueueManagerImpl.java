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
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.queue.*;

import java.util.ArrayList;
import java.util.List;

public class SQSQueueManagerImpl implements QueueManager {
    private final AmazonSQSClient sqs;
    private final QueueScope scope;
    private final QueueFig fig;
    private Queue queue;

    @Inject
    public SQSQueueManagerImpl(@Assisted QueueScope scope, QueueFig fig){
        this.fig = fig;
        this.scope = scope;
        UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        this.sqs = new AmazonSQSClient(ugProvider.getCredentials());
        Regions regions = Regions.fromName(fig.getRegion());
        Region region = Region.getRegion(regions);
        sqs.setRegion(region);
    }


    public Queue createQueue(){
        CreateQueueRequest createQueueRequest = new CreateQueueRequest()
                .withQueueName(getName());
        CreateQueueResult result = sqs.createQueue(createQueueRequest);
        return new Queue(result.getQueueUrl());
    }

    private String getName() {
        return scope.getApplication().getType() + scope.getApplication().getUuid().toString() + scope.getName();
    }

    public Queue getQueue(){
        if(queue == null) {
            for (String queueUrl : sqs.listQueues().getQueueUrls()) {
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

    public void sendMessage(String body){
        SendMessageRequest request = new SendMessageRequest(getQueue().getUrl(),body);
        sqs.sendMessage(request);
    }

    public void sendMessages(List<String> bodies){
        SendMessageBatchRequest request = new SendMessageBatchRequest(getQueue().getUrl());
        List<SendMessageBatchRequestEntry> entries = new ArrayList<>(bodies.size());
        for(String body : bodies){
            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            entry.setMessageBody(body);
            entries.add(entry);
        }
        request.setEntries(entries);
        sqs.sendMessageBatch(request);
    }

    public  List<QueueMessage> getMessages( int limit,int timeout){
        System.out.println("Receiving messages from MyQueue.\n");
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(getQueue().getUrl());
        receiveMessageRequest.setMaxNumberOfMessages(limit);
        receiveMessageRequest.setVisibilityTimeout(timeout);
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
        List<QueueMessage> queueMessages = new ArrayList<>(messages.size());
        for (Message message : messages) {
            QueueMessage queueMessage = new QueueMessage(message.getMessageId(),message.getReceiptHandle(),message.getBody());
            queueMessages.add(queueMessage);
        }
        return queueMessages;
    }

    public void commitMessage( QueueMessage queueMessage){
        sqs.deleteMessage(new DeleteMessageRequest()
                .withQueueUrl(getQueue().getUrl())
                .withReceiptHandle(queueMessage.getHandle()));
    }

    public void commitMessages( List<QueueMessage> queueMessages){
        List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();
        for(QueueMessage message : queueMessages){
            entries.add(new DeleteMessageBatchRequestEntry(message.getMessageId(),message.getHandle()));
        }
        DeleteMessageBatchRequest request = new DeleteMessageBatchRequest(getQueue().getUrl(),entries);
        DeleteMessageBatchResult result = sqs.deleteMessageBatch(request);
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
