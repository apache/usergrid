package org.apache.usergrid.persistence.queue.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.queue.QueueFig;

import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.auth.policy.conditions.ConditionFactory;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;


/**
 * Created by Jeff West on 5/25/15.
 */
public class AmazonNotificationUtils {

    private static final Logger logger = LoggerFactory.getLogger( AmazonNotificationUtils.class );


    public static String createQueue( final AmazonSQSClient sqs, final String queueName, final QueueFig fig )
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

        String queueARN = getQueueArnByUrl( sqs, queueUrl );

        Statement statement = new Statement( Statement.Effect.Allow ).withActions( SQSActions.SendMessage )
                                                                     .withPrincipals( new Principal( "*" ) )
                                                                     .withResources( new Resource( queueARN ) );

        List<Condition> conditions = new ArrayList<>();

        for ( String topicARN : topicARNs ) {

            conditions.add( ConditionFactory.newSourceArnCondition( topicARN ) );
        }
        statement.setConditions( conditions );

        Policy policy = new Policy( "SubscriptionPermission" ).withStatements( statement );


        final Map<String, String> queueAttributes = new HashMap<>();
        queueAttributes.put( "Policy", policy.toJson() );

        SetQueueAttributesRequest queueAttributesRequest = new SetQueueAttributesRequest( queueUrl, queueAttributes );

        try {
            sqs.setQueueAttributes( queueAttributesRequest );
        }
        catch ( Exception e ) {
            logger.error( "Failed to set permissions on QUEUE ARN=[{}] for TOPIC ARNs=[{}]", queueARN,
                topicARNs.toString(), e );
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
            logger.error( String.format( "Failed to get URL for Queue [%s] from SQS", queueName ), e );
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

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Looking up Topic ARN: {}", queueName );
        }

        ListTopicsResult listTopicsResult = sns.listTopics();
        String topicArn = null;

        for ( Topic topic : listTopicsResult.getTopics() ) {
            String arn = topic.getTopicArn();

            if ( queueName.equals( arn.substring( arn.lastIndexOf( ':' ) ) ) ) {
                topicArn = arn;

                logger.info( "Found existing topic arn=[{}] for queue=[{}]", topicArn, queueName );
            }
        }

        if ( topicArn == null && createOnMissing ) {
            logger.info( "Creating topic for queue=[{}]...", queueName );

            CreateTopicResult createTopicResult = sns.createTopic( queueName );
            topicArn = createTopicResult.getTopicArn();

            logger.info( "Successfully created topic with name {} and arn {}", queueName, topicArn );
        }
        else {
            logger.error( "Error looking up topic ARN for queue=[{}] and createOnMissing=[{}]", queueName,
                createOnMissing );
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Returning Topic ARN=[{}] for Queue=[{}]", topicArn, queueName );
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
