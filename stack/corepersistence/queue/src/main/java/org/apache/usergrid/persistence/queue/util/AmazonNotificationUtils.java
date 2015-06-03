package org.apache.usergrid.persistence.queue.util;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.*;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import org.apache.usergrid.persistence.queue.QueueFig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jeff West on 5/25/15.
 */
public class AmazonNotificationUtils {

    private static final Logger logger = LoggerFactory.getLogger(AmazonNotificationUtils.class);

    public static String createQueue(final String queueName,
                                     final AmazonSQSClient sqs,
                                     final QueueFig fig)
        throws Exception {

        final String deadletterQueueName = String.format("%s_dead", queueName);
        final Map<String, String> deadLetterAttributes = new HashMap<>(2);

        deadLetterAttributes.put("MessageRetentionPeriod", fig.getDeadletterRetentionPeriod());

        CreateQueueRequest createDeadLetterQueueRequest = new CreateQueueRequest()
            .withQueueName(deadletterQueueName).withAttributes(deadLetterAttributes);

        final CreateQueueResult deadletterResult = sqs.createQueue(createDeadLetterQueueRequest);

        logger.info("Created deadletter queue with url {}", deadletterResult.getQueueUrl());

        final String deadletterArn = AmazonNotificationUtils.getQueueArnByName(deadletterQueueName, sqs);

        String redrivePolicy = String.format("{\"maxReceiveCount\":\"%s\"," +
            " \"deadLetterTargetArn\":\"%s\"}", fig.getQueueDeliveryLimit(), deadletterArn);

        final Map<String, String> queueAttributes = new HashMap<>(2);
        deadLetterAttributes.put("MessageRetentionPeriod", fig.getRetentionPeriod());
        deadLetterAttributes.put("RedrivePolicy", redrivePolicy);

        CreateQueueRequest createQueueRequest = new CreateQueueRequest().
            withQueueName(queueName)
            .withAttributes(queueAttributes);

        CreateQueueResult result = sqs.createQueue(createQueueRequest);

        String url = result.getQueueUrl();

        logger.info("Created SQS queue with url {}", url);

        return url;
    }


    public static String getQueueArnByName(final String queueName,
                                           final AmazonSQSClient sqs)
        throws Exception {

        String queueUrl = null;

        try {
            GetQueueUrlResult result = sqs.getQueueUrl(queueName);
            queueUrl = result.getQueueUrl();

        } catch (QueueDoesNotExistException queueDoesNotExistException) {
            //no op, swallow
            logger.warn("Queue {} does not exist", queueName);
            return null;

        } catch (Exception e) {
            logger.error(String.format("Failed to get URL for Queue [%s] from SQS", queueName), e);
            throw e;
        }

        if (queueUrl != null) {
            String queueArn = null;

            try {
                GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest(queueUrl)
                    .withAttributeNames("All");

                GetQueueAttributesResult queueAttributesResult = sqs.getQueueAttributes(queueAttributesRequest);
                Map<String, String> sqsAttributeMap = queueAttributesResult.getAttributes();

                return sqsAttributeMap.get("QueueArn");

            } catch (Exception e) {
                logger.error("Failed to get queue URL from service", e);
                throw e;
            }
        }

        return null;
    }

    public static String getQueueArnByUrl(final String queueUrl,
                                          final AmazonSQSClient sqs)
        throws Exception {

        String queueArn = null;

        try {
            GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest(queueUrl)
                .withAttributeNames("All");

            GetQueueAttributesResult queueAttributesResult = sqs.getQueueAttributes(queueAttributesRequest);
            Map<String, String> sqsAttributeMap = queueAttributesResult.getAttributes();

            return sqsAttributeMap.get("QueueArn");

        } catch (Exception e) {
            logger.error("Failed to get queue URL from service", e);
            throw e;
        }
    }

    public static void subscribeQueueToTopic(final String topicArn,
                                             final String queueArn,
                                             final AmazonSNSClient sns)
        throws Exception {

        try {
            SubscribeRequest subscribeRequest = new SubscribeRequest(topicArn, "sqs", queueArn);
            SubscribeResult subscribeResult = sns.subscribe(subscribeRequest);
            String subscriptionArn = subscribeResult.getSubscriptionArn();

            logger.info("Successfully subscribed SQS Queue {} to SNS arn {} with Subscription arn {}", queueArn, topicArn,
                subscriptionArn);

        } catch (AuthorizationErrorException e) {
            logger.error(String.format("AuthorizationErrorException creating/subscribing SQS Queue [%s] to SNS arn [%s]: %s", queueArn, topicArn, e.getMessage()), e);
            throw new Exception("AuthorizationErrorException creating/subscribing SQS queue to SNS", e);
        } catch (SubscriptionLimitExceededException e) {
            logger.error(String.format("SubscriptionLimitExceededException creating/subscribing SQS Queue [%s] to SNS arn [%s]: %s", queueArn, topicArn, e.getMessage()), e);
            throw new Exception("SubscriptionLimitExceededException creating/subscribing SQS queue to SNS", e);
        } catch (AmazonServiceException e) {
            logger.error(String.format("AmazonServiceException creating/subscribing SQS Queue [%s] to SNS arn [%s]: %s", queueArn, topicArn, e.getMessage()), e);
            throw new Exception("AmazonServiceException creating/subscribing SQS queue to SNS", e);
        } catch (Exception e) {
            logger.error(String.format("Failed creating/subscribing SQS Queue [%s] to SNS arn [%s]: %s", queueArn, topicArn, e.getMessage()), e);
            throw e;
        }
    }

    public static String getTopicArn(final String queueName,
                                     final AmazonSNSClient sns,
                                     final boolean createOnMissing)
        throws Exception {

        ListTopicsResult listTopicsResult = sns.listTopics();
        String topicArn = null;

        for (Topic topic : listTopicsResult.getTopics()) {
            String arn = topic.getTopicArn();

            if (queueName.equals(arn.substring(arn.lastIndexOf(':')))) {
                topicArn = arn;
            }
        }

        if (topicArn == null && createOnMissing) {
            CreateTopicResult createTopicResult = sns.createTopic(queueName);
            topicArn = createTopicResult.getTopicArn();
            logger.info("Created topic with name {} and arn {}", queueName, topicArn);
        }

        return topicArn;
    }

}
