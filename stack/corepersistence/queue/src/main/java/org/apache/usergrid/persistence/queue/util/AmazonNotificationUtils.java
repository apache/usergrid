package org.apache.usergrid.persistence.queue.util;


import java.util.*;

import com.amazonaws.auth.policy.*;
import com.amazonaws.auth.policy.conditions.ArnCondition;
import com.amazonaws.services.sqs.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.queue.LegacyQueueFig;

import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.auth.policy.conditions.ConditionFactory;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQSClient;


/**
 * Created by Jeff West on 5/25/15.
 */
public class AmazonNotificationUtils {

    private static final Logger logger = LoggerFactory.getLogger( AmazonNotificationUtils.class );


    public static String createQueue( final AmazonSQSClient sqs, final String queueName, final LegacyQueueFig fig )
        throws Exception {

        final String deadletterQueueName = String.format( "%s_dead", queueName );
        final Map<String, String> deadLetterAttributes = new HashMap<>( 2 );

        deadLetterAttributes.put( "MessageRetentionPeriod", fig.getDeadletterRetentionPeriod() );

        CreateQueueRequest createDeadLetterQueueRequest =
            new CreateQueueRequest().withQueueName( deadletterQueueName ).withAttributes( deadLetterAttributes );

        final CreateQueueResult deadletterResult = sqs.createQueue( createDeadLetterQueueRequest );

        logger.info( "Created deadletter queue with url {}", deadletterResult.getQueueUrl() );

        final String deadletterArn = AmazonNotificationUtils.getQueueArnByName( sqs, deadletterQueueName );

        String redrivePolicy = String
            .format( "{\"maxReceiveCount\":\"%s\"," + " \"deadLetterTargetArn\":\"%s\"}", fig.getQueueDeliveryLimit(),
                deadletterArn );

        final String visibilityTimeoutInSeconds = String.valueOf(Math.max(1, fig.getVisibilityTimeout() / 1000));

        final Map<String, String> queueAttributes = new HashMap<>( 2 );
        queueAttributes.put( "MessageRetentionPeriod", fig.getRetentionPeriod() );
        queueAttributes.put( "RedrivePolicy", redrivePolicy );
        queueAttributes.put( "VisibilityTimeout", visibilityTimeoutInSeconds );

        CreateQueueRequest createQueueRequest = new CreateQueueRequest().
                                                                            withQueueName( queueName )
                                                                        .withAttributes( queueAttributes );

        CreateQueueResult result = sqs.createQueue( createQueueRequest );

        String url = result.getQueueUrl();

        logger.info( "Created SQS queue with url {}", url );

        return url;
    }


    public static void setQueuePermissionsToReceive( final AmazonSQSClient sqs, final String queueUrl,
                                                     final List<String> topicARNs ) throws Exception {

        // retrieve queue ARN and policy
        List<String> sqsAttrNames = Arrays.asList(QueueAttributeName.QueueArn.toString(),
            QueueAttributeName.Policy.toString());
        GetQueueAttributesRequest getQueueAttributesRequest =
            new GetQueueAttributesRequest( queueUrl ).withAttributeNames( sqsAttrNames );
        GetQueueAttributesResult queueAttributesResult = sqs.getQueueAttributes( getQueueAttributesRequest );
        Map<String, String> sqsAttributeMap = queueAttributesResult.getAttributes();
        String queueARN = sqsAttributeMap.get(QueueAttributeName.QueueArn.toString());
        String policyJson = sqsAttributeMap.get(QueueAttributeName.Policy.toString());

        // cannot send ARN in settings update, so remove it
        sqsAttributeMap.remove(QueueAttributeName.QueueArn.toString());

        // get existing policy from JSON
        Policy policy = policyJson != null && policyJson.length() > 0 ? Policy.fromJson(policyJson) : new Policy();

        // see if permissions already exist, and find ArnLike conditions
        boolean matchingConditionFound = false;
        boolean policyEdited = false;
        for (Statement statement : policy.getStatements()) {
            logger.info("statement id: {}, effect: {}, action: {}, resources:{}",
                statement.getId(), statement.getEffect().name(),
                statement.getActions().get(0).getActionName(),
                statement.getResources().get(0).getId());

            // must be Allow effect
            if (! statement.getEffect().name().equals(Statement.Effect.Allow.name())) {
                continue;
            }

            // must be SendMessage action
            boolean actionFound = false;
            for (Action action : statement.getActions()) {
                // do lower case comparison, since UI adds SQS.SendMessage but SDK uses sqs.SendMessage
                if (action.getActionName().toLowerCase().equals(SQSActions.SendMessage.getActionName().toLowerCase())) {
                    actionFound = true;
                    break;
                }
            }
            if (!actionFound) {
                continue;
            }

            // must be same queue resource
            boolean queueResourceFound = false;
            for (Resource resource : statement.getResources()) {
                if (resource.getId().equals(queueARN)) {
                    queueResourceFound = true;
                    break;
                }
            }
            if (!queueResourceFound) {
                continue;
            }

            // found matching statement, check conditions for source ARN
            for (Condition condition : statement.getConditions()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("condition type: {}, conditionKey: {}", condition.getType(), condition.getConditionKey());
                }
                if (condition.getType().equals(ArnCondition.ArnComparisonType.ArnLike.name()) &&
                    condition.getConditionKey().equals(ConditionFactory.SOURCE_ARN_CONDITION_KEY)) {
                    matchingConditionFound = true;
                    for (String topicARN : topicARNs) {
                        if (! condition.getValues().contains(topicARN)) {
                            // topic doesn't exist, add it
                            policyEdited = true;
                            condition.getValues().add(topicARN);
                        }
                    }
                }
            }
        }

        if (!matchingConditionFound) {
            // never found ArnLike SourceArn condition, need to add a statement
            List<Condition> conditions = new ArrayList<>();

            for (String topicARN : topicARNs) {

                conditions.add(ConditionFactory.newSourceArnCondition(topicARN));
            }

            Statement statement = new Statement(Statement.Effect.Allow)
                .withPrincipals(Principal.AllUsers)
                .withActions(SQSActions.SendMessage)
                .withResources(new Resource(queueARN));
            statement.setConditions(conditions);

            policy.getStatements().add(statement);
            policyEdited = true;
        }

        if (policyEdited) {
            sqsAttributeMap.put(QueueAttributeName.Policy.toString(), policy.toJson());

            // log if permissions are being updated
            logger.info("updating permissions for queueARN: {}, new policy: {}", queueARN, policy.toJson());

            SetQueueAttributesRequest setQueueAttributesRequest = new SetQueueAttributesRequest(queueUrl, sqsAttributeMap);

            try {
                sqs.setQueueAttributes(setQueueAttributesRequest);
            } catch (Exception e) {
                logger.error("Failed to set permissions on QUEUE ARN=[{}] for TOPIC ARNs=[{}]", queueARN,
                    topicARNs.toString(), e);
            }
        }
    }


    public static String getQueueArnByName( final AmazonSQSClient sqs, final String queueName ) throws Exception {

        String queueUrl = null;

        try {
            GetQueueUrlResult result = sqs.getQueueUrl( queueName );
            queueUrl = result.getQueueUrl();
        }
        catch ( QueueDoesNotExistException queueDoesNotExistException ) {
            //no op, swallow
            logger.warn( "Queue {} does not exist", queueName );
            return null;
        }
        catch ( Exception e ) {
            logger.error( "Failed to get URL for Queue [{}] from SQS", queueName, e );
            throw e;
        }

        if ( queueUrl != null ) {

            try {
                GetQueueAttributesRequest queueAttributesRequest =
                    new GetQueueAttributesRequest( queueUrl ).withAttributeNames( "All" );

                GetQueueAttributesResult queueAttributesResult = sqs.getQueueAttributes( queueAttributesRequest );
                Map<String, String> sqsAttributeMap = queueAttributesResult.getAttributes();

                return sqsAttributeMap.get( "QueueArn" );
            }
            catch ( Exception e ) {
                logger.error( "Failed to get queue URL from service", e );
                throw e;
            }
        }

        return null;
    }


    public static String getQueueArnByUrl( final AmazonSQSClient sqs, final String queueUrl ) throws Exception {

        try {
            GetQueueAttributesRequest queueAttributesRequest =
                new GetQueueAttributesRequest( queueUrl ).withAttributeNames( "All" );

            GetQueueAttributesResult queueAttributesResult = sqs.getQueueAttributes( queueAttributesRequest );
            Map<String, String> sqsAttributeMap = queueAttributesResult.getAttributes();

            return sqsAttributeMap.get( "QueueArn" );
        }
        catch ( Exception e ) {
            logger.error( "Failed to get queue URL from service", e );
            throw e;
        }
    }


    public static String getTopicArn( final AmazonSNSClient sns, final String queueName, final boolean createOnMissing )
        throws Exception {

        if ( logger.isTraceEnabled() ) {
            logger.trace( "Looking up Topic ARN: {}", queueName );
        }

        ListTopicsResult listTopicsResult = sns.listTopics();
        String topicArn = null;

        for ( Topic topic : listTopicsResult.getTopics() ) {
            String arn = topic.getTopicArn();

            if ( queueName.equals( arn.substring( arn.lastIndexOf( ':' ) ) ) ) {
                topicArn = arn;

                if (logger.isTraceEnabled()) {
                    logger.trace( "Found existing topic arn=[{}] for queue=[{}]", topicArn, queueName );
                }
            }
        }

        if ( topicArn == null && createOnMissing ) {
            if (logger.isTraceEnabled()) {
                logger.trace("Creating topic for queue=[{}]...", queueName);
            }

            CreateTopicResult createTopicResult = sns.createTopic( queueName );
            topicArn = createTopicResult.getTopicArn();

            if (logger.isTraceEnabled()) {
                logger.trace("Successfully created topic with name {} and arn {}", queueName, topicArn);
            }
        }
        else {
            logger.error( "Error looking up topic ARN for queue=[{}] and createOnMissing=[{}]", queueName,
                createOnMissing );
        }

        if ( logger.isTraceEnabled() ) {
            logger.trace( "Returning Topic ARN=[{}] for Queue=[{}]", topicArn, queueName );
        }


        return topicArn;
    }


    public static String getQueueUrlByName( final AmazonSQSClient sqs, final String queueName ) {

        try {
            GetQueueUrlResult result = sqs.getQueueUrl( queueName );
            return result.getQueueUrl();
        }
        catch ( QueueDoesNotExistException e ) {
            //no op, return null
            logger.error( "Queue {} does not exist", queueName );
            return null;
        }
        catch ( Exception e ) {
            logger.error( "failed to get queue from service", e );
            throw e;
        }
    }
}
