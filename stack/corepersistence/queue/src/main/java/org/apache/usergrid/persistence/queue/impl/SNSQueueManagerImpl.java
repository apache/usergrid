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


import com.amazonaws.AmazonServiceException;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.*;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.persistence.core.guicyfig.ClusterFig;
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
    private final QueueFig fig;
    private final ClusterFig clusterFig;
    private final AmazonSQSClient sqs;
    private final AmazonSNSClient sns;
    private final AmazonSNSAsyncClient snsAsync;


    private final JsonFactory JSON_FACTORY = new JsonFactory();
    private final ObjectMapper mapper = new ObjectMapper(JSON_FACTORY);


    private final LoadingCache<String, String> writeTopicArnMap = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String queueName)
                        throws Exception {

                    return setupTopics(queueName);
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
                        logger.error("Queue {} does not exist, will create", queueName);
                    } catch (Exception e) {
                        logger.error("failed to get queue from service", e);
                        throw e;
                    }

                    if (queue == null) {
                        String url = AmazonNotificationUtils.createQueue(sqs, queueName, fig);
                        queue = new Queue(url);
                    }

                    setupTopics(queueName);

                    return queue;
                }
            });


    @Inject
    public SNSQueueManagerImpl(@Assisted QueueScope scope, QueueFig fig, ClusterFig clusterFig) {
        this.scope = scope;
        this.fig = fig;
        this.clusterFig = clusterFig;

        try {
            sqs = createSQSClient(getRegion());
            sns = createSNSClient(getRegion());
            snsAsync = createAsyncSNSClient(getRegion());

        } catch (Exception e) {
            throw new RuntimeException("Error setting up mapper", e);
        }
    }

    private String setupTopics(final String queueName)
            throws Exception {

        logger.info("Setting up setupTopics SNS/SQS...");

        String primaryTopicArn = AmazonNotificationUtils.getTopicArn(sns, queueName, true);

        if (logger.isDebugEnabled()) logger.debug("SNS/SQS Setup: primaryTopicArn=" + primaryTopicArn);

        String queueUrl = AmazonNotificationUtils.getQueueUrlByName(sqs, queueName);
        String primaryQueueArn = AmazonNotificationUtils.getQueueArnByName(sqs, queueName);

        if (logger.isDebugEnabled()) logger.debug("SNS/SQS Setup: primaryQueueArn=" + primaryQueueArn);

        if (primaryQueueArn == null) {
            if (logger.isDebugEnabled())
                logger.debug("SNS/SQS Setup: primaryQueueArn is null, creating queue...");

            queueUrl = AmazonNotificationUtils.createQueue(sqs, queueName, fig);
            primaryQueueArn = AmazonNotificationUtils.getQueueArnByUrl(sqs, queueUrl);

            if (logger.isDebugEnabled())
                logger.debug("SNS/SQS Setup: New Queue URL=[{}] ARN=[{}]", queueUrl, primaryQueueArn);
        }

        try {

            SubscribeRequest primarySubscribeRequest = new SubscribeRequest(primaryTopicArn, "sqs", primaryQueueArn);
            sns.subscribe(primarySubscribeRequest);

            // ensure the SNS primary topic has permission to send to the primary SQS queue
            List<String> primaryTopicArnList = new ArrayList<>();
            primaryTopicArnList.add(primaryTopicArn);
            AmazonNotificationUtils.setQueuePermissionsToReceive(sqs, queueUrl, primaryTopicArnList);
        } catch (AmazonServiceException e) {
            logger.error(String.format("Unable to subscribe PRIMARY queue=[%s] to topic=[%s]", queueUrl, primaryTopicArn), e);
        }

        if (fig.isMultiRegion() && scope.getRegionImplementation() == QueueScope.RegionImplementation.ALLREGIONS) {

            String multiRegion = fig.getRegionList();

            if (logger.isDebugEnabled())
                logger.debug("MultiRegion Setup specified, regions: [{}]", multiRegion);

            String[] regionNames = multiRegion.split(",");

            final Map<String, String> arrQueueArns = new HashMap<>(regionNames.length + 1);
            final Map<String, String> topicArns = new HashMap<>(regionNames.length + 1);

            arrQueueArns.put(primaryQueueArn, fig.getRegion());
            topicArns.put(primaryTopicArn, fig.getRegion());

            for (String regionName : regionNames) {

                regionName = regionName.trim();
                Regions regions = Regions.fromName(regionName);
                Region region = Region.getRegion(regions);

                AmazonSQSClient sqsClient = createSQSClient(region);
                AmazonSNSClient snsClient = createSNSClient(region); // do this stuff synchronously

                // getTopicArn will create the SNS topic if it doesn't exist
                String topicArn = AmazonNotificationUtils.getTopicArn(snsClient, queueName, true);
                topicArns.put(topicArn, regionName);

                // create the SQS queue if it doesn't exist
                String queueArn = AmazonNotificationUtils.getQueueArnByName(sqsClient, queueName);
                if (queueArn == null) {
                    queueUrl = AmazonNotificationUtils.createQueue(sqsClient, queueName, fig);
                    queueArn = AmazonNotificationUtils.getQueueArnByUrl(sqsClient, queueUrl);
                }

                arrQueueArns.put(queueArn, regionName);
            }

            logger.debug("Creating Subscriptions...");

            for (Map.Entry<String, String> queueArnEntry : arrQueueArns.entrySet()) {
                String queueARN = queueArnEntry.getKey();
                String strSqsRegion = queueArnEntry.getValue();

                Regions sqsRegions = Regions.fromName(strSqsRegion);
                Region sqsRegion = Region.getRegion(sqsRegions);

                AmazonSQSClient subscribeSqsClient = createSQSClient(sqsRegion);

                // ensure the URL used to subscribe is for the correct name/region
                String subscribeQueueUrl = AmazonNotificationUtils.getQueueUrlByName(subscribeSqsClient, queueName);

                // this list used later for adding permissions to queues
                List<String> topicArnList = new ArrayList<>();

                for (Map.Entry<String, String> topicArnEntry : topicArns.entrySet()) {

                    String topicARN = topicArnEntry.getKey();
                    topicArnList.add(topicARN);

                    String strSnsRegion = topicArnEntry.getValue();
                    Regions snsRegions = Regions.fromName(strSnsRegion);
                    Region snsRegion = Region.getRegion(snsRegions);

                    AmazonSNSClient subscribeSnsClient = createSNSClient(snsRegion); // do this stuff synchronously
                    SubscribeRequest subscribeRequest = new SubscribeRequest(topicARN, "sqs", queueARN);

                    try {

                        logger.info("Subscribing Queue ARN/Region=[{} / {}] and Topic ARN/Region=[{} / {}]",
                            queueARN,
                            strSqsRegion,
                            topicARN,
                            strSnsRegion
                        );

                        SubscribeResult subscribeResult = subscribeSnsClient.subscribe(subscribeRequest);
                        String subscriptionARN = subscribeResult.getSubscriptionArn();
                        if(logger.isDebugEnabled()){
                            logger.debug("Successfully subscribed Queue ARN=[{}] to Topic ARN=[{}], subscription ARN=[{}]", queueARN, topicARN, subscriptionARN);
                        }


                    } catch (Exception e) {
                        logger.error(String.format("ERROR Subscribing Queue ARN/Region=[%s / %s] and Topic ARN/Region=[%s / %s]",
                                queueARN,
                                strSqsRegion,
                                topicARN,
                                strSnsRegion), e);


                    }
                }

                logger.info("Adding permission to receive messages...");
                // add permission to each queue, providing a list of topics that it's subscribed to
                AmazonNotificationUtils.setQueuePermissionsToReceive(subscribeSqsClient, subscribeQueueUrl, topicArnList);

            }
        }

        return primaryTopicArn;
    }

    /**
     * The Asynchronous SNS client is used for publishing events to SNS.
     *
     */

    private AmazonSNSAsyncClient createAsyncSNSClient(final Region region) {
        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();

        /**
         * The Async client will use default client configurations (default max conn: 50)
         * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/ClientConfiguration.html
         * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/constant-values.html#com.amazonaws.ClientConfiguration.DEFAULT_MAX_CONNECTIONS
         */

        final AmazonSNSAsyncClient sns = new AmazonSNSAsyncClient(ugProvider.getCredentials());

        sns.setRegion(region);

        return sns;
    }

    /**
     * The Synchronous SNS client is used for creating topics and subscribing queues.
     *
     */
    private AmazonSNSClient createSNSClient(final Region region) {
        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();

        final AmazonSNSClient sns = new AmazonSNSClient(ugProvider.getCredentials());

        sns.setRegion(region);

        return sns;
    }


    private String getName() {
        String name = clusterFig.getClusterName() + "_" + scope.getName() + "_" + scope.getRegionImplementation();

        Preconditions.checkArgument(name.length() <= 80, "Your name must be < than 80 characters");

        return name;
    }

    public Queue getReadQueue() {
        String queueName = getName();

        try {
            return readQueueUrlMap.get(queueName);
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
    public rx.Observable<QueueMessage> getMessages(final int limit,
                                                   final int transactionTimeout,
                                                   final int waitTime,
                                                   final Class klass) {

        if (sqs == null) {
            logger.error("SQS is null - was not initialized properly");
            return rx.Observable.empty();
        }

        String url = getReadQueue().getUrl();

        if (logger.isDebugEnabled()) logger.debug("Getting up to {} messages from {}", limit, url);

        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(url);
        receiveMessageRequest.setMaxNumberOfMessages(limit);
        receiveMessageRequest.setVisibilityTimeout(transactionTimeout / 1000);
        receiveMessageRequest.setWaitTimeSeconds(waitTime / 1000);

        try {
            ReceiveMessageResult result = sqs.receiveMessage(receiveMessageRequest);
            List<Message> messages = result.getMessages();

            if (logger.isDebugEnabled()) logger.debug("Received {} messages from {}", messages.size(), url);

            List<QueueMessage> queueMessages = new ArrayList<>(messages.size());

            for (Message message : messages) {
                Object body;

                try {
                    final JsonNode bodyNode =  mapper.readTree(message.getBody());
                    JsonNode bodyObj = bodyNode.has("Message") ? bodyNode.get("Message") : bodyNode;
                    body = fromString(bodyObj.textValue(), klass);
                } catch (Exception e) {
                    logger.error(String.format("failed to deserialize message: %s", message.getBody()), e);
                    throw new RuntimeException(e);
                }

                QueueMessage queueMessage = new QueueMessage(message.getMessageId(), message.getReceiptHandle(), body, message.getAttributes().get("type"));
                queueMessages.add(queueMessage);
            }

            return rx.Observable.from(queueMessages);

        } catch (com.amazonaws.services.sqs.model.QueueDoesNotExistException dne) {
            logger.error(String.format("Queue does not exist! [%s]", url), dne);
        } catch (Exception e) {
            logger.error(String.format("Programming error getting messages from queue=[%s] exist!", url), e);
        }

        return rx.Observable.from(new ArrayList<>(0));
    }

    @Override
    public void sendMessages(final List bodies) throws IOException {

        if (snsAsync == null) {
            logger.error("SNS client is null, perhaps it failed to initialize successfully");
            return;
        }

        for (Object body : bodies) {
            sendMessage(body);
        }

    }

    @Override
    public void sendMessage(final Object body) throws IOException {

        if (snsAsync == null) {
            logger.error("SNS client is null, perhaps it failed to initialize successfully");
            return;
        }

        final String stringBody = toString(body);

        String topicArn = getWriteTopicArn();

        if (logger.isDebugEnabled()) logger.debug("Publishing Message...{} to arn: {}", stringBody, topicArn);

        PublishRequest publishRequest = new PublishRequest(topicArn, stringBody);

        snsAsync.publishAsync(publishRequest, new AsyncHandler<PublishRequest, PublishResult>() {
                @Override
                public void onError(Exception e) {
                    logger.error("Error publishing message... {}", e);
                }

                @Override
                public void onSuccess(PublishRequest request, PublishResult result) {
                    if (logger.isDebugEnabled()) logger.debug("Successfully published... messageID=[{}],  arn=[{}]", result.getMessageId(), request.getTopicArn());

                }
            });

    }


    @Override
    public void commitMessage(final QueueMessage queueMessage) {
        String url = getReadQueue().getUrl();
        if (logger.isDebugEnabled())
            logger.debug("Commit message {} to queue {}", queueMessage.getMessageId(), url);

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
