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


import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.*;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.persistence.queue.*;
import org.apache.usergrid.persistence.queue.Queue;
import org.apache.usergrid.persistence.queue.util.AmazonNotificationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SNSQueueManagerImpl implements QueueManager {

    private static final Logger logger = LoggerFactory.getLogger(SNSQueueManagerImpl.class);

    private final QueueScope scope;
    private ObjectMapper mapper;
    private final QueueFig fig;
    private final AmazonSQSClient sqs;
    private final AmazonSNSClient sns;

    private static SmileFactory smileFactory = new SmileFactory();

    private final LoadingCache<String, String> writeTopicArnMap = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(new CacheLoader<String, String>() {
            @Override
            public String load(String queueName)
                throws Exception {

                String primaryTopicArn = setupMultiRegion(queueName);

                return primaryTopicArn;
            }
        });

    private final LoadingCache<String, Queue> readQueueUrlMap = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(new CacheLoader<String, Queue>() {
            @Override
            public Queue load(String queueName) throws Exception {

                Queue queue = null;

                try {
                    GetQueueUrlResult result = sqs.getQueueUrl(queueName);
                    queue = new Queue(result.getQueueUrl());
                } catch (QueueDoesNotExistException queueDoesNotExistException) {
                    logger.error("Queue {} does not exist, creating", queueName);
                } catch (Exception e) {
                    logger.error("failed to get queue from service", e);
                    throw e;
                }

                if (queue == null) {
                    String primaryTopicArn = setupMultiRegion(queueName);

                    String url = AmazonNotificationUtils.getQueueArnByName(queueName, sqs);
                    queue = new Queue(url);
                }

                return queue;
            }
        });


    @Inject
    public SNSQueueManagerImpl(@Assisted QueueScope scope, QueueFig fig) {
        this.scope = scope;
        this.fig = fig;

        try {
            smileFactory.delegateToTextual(true);
            mapper = new ObjectMapper(smileFactory);
            mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");

            sqs = createSQSClient(getRegion());
            sns = createSNSClient(getRegion());

        } catch (Exception e) {
            throw new RuntimeException("Error setting up mapper", e);
        }
    }

    private String setupMultiRegion(final String queueName)
        throws Exception {

        String primaryTopicArn = AmazonNotificationUtils.getTopicArn(queueName, sns, true);

        String primaryQueueArn = AmazonNotificationUtils.getQueueArnByName(queueName, sqs);

        if (primaryQueueArn == null) {
            String queueUrl = AmazonNotificationUtils.createQueue(queueName, sqs, fig);
            primaryQueueArn = AmazonNotificationUtils.getQueueArnByUrl(queueUrl, sqs);
        }

        AmazonNotificationUtils.subscribeQueueToTopic(primaryTopicArn, primaryQueueArn, sns);

        if (fig.isMultiRegion()) {

            String multiRegion = fig.getRegionList();
            String[] regionNames = multiRegion.split(",");

            final Set<String> arrQueueArns = new HashSet<>(regionNames.length + 1);
            final Map<String, AmazonSNSClient> topicArns = new HashMap<>(regionNames.length + 1);

            arrQueueArns.add(primaryQueueArn);
            topicArns.put(primaryTopicArn, sns);

            for (String regionName : regionNames) {

                Regions regions = Regions.fromName(regionName);
                Region region = Region.getRegion(regions);

                AmazonSQSClient sqsClient = createSQSClient(region);
                AmazonSNSClient snsClient = createSNSClient(region);

                String topicArn = AmazonNotificationUtils.getTopicArn(queueName, snsClient, true);

                topicArns.put(topicArn, snsClient);

                String queueArn = AmazonNotificationUtils.getQueueArnByName(queueName, sqsClient);

                if (queueArn == null) {
                    String queueUrl = AmazonNotificationUtils.createQueue(queueName, sqsClient, fig);
                    queueArn = AmazonNotificationUtils.getQueueArnByUrl(queueUrl, sqsClient);
                }

                arrQueueArns.add(queueArn);
            }

            for (String queueArn : arrQueueArns) {
                for (Map.Entry<String, AmazonSNSClient> topicArnEntry : topicArns.entrySet()) {
                    String topicArn = topicArnEntry.getKey();
                    AmazonSNSClient snsClient = topicArnEntry.getValue();
                    AmazonNotificationUtils.subscribeQueueToTopic(topicArn, queueArn, snsClient);
                }

            }
        }

        return primaryTopicArn;
    }


    private AmazonSNSClient createSNSClient(final Region region) {
        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        final AmazonSNSClient sns = new AmazonSNSClient(ugProvider.getCredentials());

        sns.setRegion(region);

        return sns;
    }


    private String getName() {
        String name = fig.getPrefix() + "_" + scope.getName();

        Preconditions.checkArgument(name.length() <= 80, "Your name must be < than 80 characters");

        return name;
    }

    public Queue getReadQueue() {
        try {
            return readQueueUrlMap.get(getName());
        } catch (ExecutionException ee) {
            throw new RuntimeException(ee);
        }
    }

    public String getWriteTopicArn() {
        try {
            return writeTopicArnMap.get(getName());

        } catch (ExecutionException ee) {
            throw new RuntimeException(ee);
        }
    }

    @Override
    public List<QueueMessage> getMessages(final int limit,
                                          final int transactionTimeout,
                                          final int waitTime,
                                          final Class klass) {

        if (sqs == null) {
            logger.error("SQS is null - was not initialized properly");
            return new ArrayList<>();
        }


        String url = getReadQueue().getUrl();

        if (logger.isDebugEnabled()) logger.debug("Getting {} messages from {}", limit, url);

        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(url);
        receiveMessageRequest.setMaxNumberOfMessages(limit);
        receiveMessageRequest.setVisibilityTimeout(transactionTimeout / 1000);
        receiveMessageRequest.setWaitTimeSeconds(waitTime / 1000);
        ReceiveMessageResult result = sqs.receiveMessage(receiveMessageRequest);
        List<Message> messages = result.getMessages();

        if (logger.isDebugEnabled()) logger.debug("Received {} messages from {}", messages.size(), url);

        List<QueueMessage> queueMessages = new ArrayList<>(messages.size());

        for (Message message : messages) {
            Object body;

            try {
                body = fromString(message.getBody(), klass);
            } catch (Exception e) {
                logger.error("failed to deserialize message", e);
                throw new RuntimeException(e);
            }

            QueueMessage queueMessage = new QueueMessage(message.getMessageId(), message.getReceiptHandle(), body, message.getAttributes().get("type"));
            queueMessages.add(queueMessage);
        }
        return queueMessages;
    }

    @Override
    public void sendMessages(final List bodies) throws IOException {

        if (sns == null) {
            logger.error("SNS client is null, perhaps it failed to initialize successfully");
            return;
        }

        for (Object body : bodies) {
            sendMessage(body);
        }

    }

    @Override
    public void sendMessage(final Object body) throws IOException {

        if (sns == null) {
            logger.error("SNS client is null, perhaps it failed to initialize successfully");
            return;
        }

        final String stringBody = toString(body);

        String topicArn = getWriteTopicArn();

        if (logger.isDebugEnabled()) logger.debug("Publishing Message...{} to arn: {}", stringBody, topicArn);

        PublishResult publishResult = sns.publish(topicArn, toString(body));

        if (logger.isDebugEnabled())
            logger.debug("Published Message ID: {} to arn: {}", publishResult.getMessageId(), topicArn);
    }


    @Override
    public void commitMessage(final QueueMessage queueMessage) {
        String url = getReadQueue().getUrl();
        if (logger.isDebugEnabled()) logger.debug("Commit message {} to queue {}", queueMessage.getMessageId(), url);

        sqs.deleteMessage(new DeleteMessageRequest()
            .withQueueUrl(url)
            .withReceiptHandle(queueMessage.getHandle()));
    }


    @Override
    public void commitMessages(final List<QueueMessage> queueMessages) {
        String url = getReadQueue().getUrl();

        if (logger.isDebugEnabled()) logger.debug("Commit messages {} to queue {}", queueMessages.size(), url);

        List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();

        for (QueueMessage message : queueMessages) {
            entries.add(new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getHandle()));
        }

        DeleteMessageBatchRequest request = new DeleteMessageBatchRequest(url, entries);
        DeleteMessageBatchResult result = sqs.deleteMessageBatch(request);

        boolean successful = result.getFailed().size() <= 0;

        if (!successful) {
            for (BatchResultErrorEntry failed : result.getFailed()) {
                logger.error("Commit failed reason: {} messages id: {}", failed.getMessage(), failed.getId());
            }
        }
    }


    /**
     * Read the object from Base64 string.
     */
    private Object fromString(final String s, final Class klass)
        throws IOException, ClassNotFoundException {

        Object o = mapper.readValue(s, klass);
        return o;
    }

    /**
     * Write the object to a Base64 string.
     */
    private String toString(final Object o) throws IOException {
        return mapper.writeValueAsString(o);
    }


    /**
     * Get the region
     *
     * @return
     */
    private Region getRegion() {
        Regions regions = Regions.fromName(fig.getRegion());
        return Region.getRegion(regions);
    }


    /**
     * Create the SQS client for the specified settings
     */
    private AmazonSQSClient createSQSClient(final Region region) {
        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        final AmazonSQSClient sqs = new AmazonSQSClient(ugProvider.getCredentials());

        sqs.setRegion(region);

        return sqs;
    }


}
