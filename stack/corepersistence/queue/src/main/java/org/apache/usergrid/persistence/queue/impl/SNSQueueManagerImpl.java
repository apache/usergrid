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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import com.amazonaws.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.executor.TaskExecutorFactory;
import org.apache.usergrid.persistence.core.guicyfig.ClusterFig;
import org.apache.usergrid.persistence.queue.LegacyQueue;
import org.apache.usergrid.persistence.queue.LegacyQueueFig;
import org.apache.usergrid.persistence.queue.LegacyQueueManager;
import org.apache.usergrid.persistence.queue.LegacyQueueMessage;
import org.apache.usergrid.persistence.queue.LegacyQueueScope;
import org.apache.usergrid.persistence.queue.util.AmazonNotificationUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


public class SNSQueueManagerImpl implements LegacyQueueManager {

    private static final Logger logger = LoggerFactory.getLogger( SNSQueueManagerImpl.class );

    private final LegacyQueueScope scope;
    private final LegacyQueueFig fig;
    private final ClusterFig clusterFig;
    private final CassandraConfig cassandraConfig;
    private final ClientConfiguration clientConfiguration;
    private final AmazonSQSClient sqs;
    private final AmazonSNSClient sns;
    private final AmazonSNSAsyncClient snsAsync;
    private final AmazonSQSAsyncClient sqsAsync;


    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper( JSON_FACTORY );
    private static final int MIN_CLIENT_SOCKET_TIMEOUT = 5000; // millis
    private static final int MIN_VISIBILITY_TIMEOUT = 1; //seconds

    static {

        /**
         * Because of the way SNS escapes all our json, we have to tell jackson to accept it.  See the documentation
         * here for how SNS borks the message body
         *
         *  http://docs.aws.amazon.com/sns/latest/dg/SendMessageToHttp.html
         */
        mapper.configure( JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true );
    }


    private final LoadingCache<String, String> writeTopicArnMap =
        CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<String, String>() {
            @Override
            public String load( String queueName ) throws Exception {

                return setupTopics( queueName );
            }
        } );

    private final LoadingCache<String, LegacyQueue> readQueueUrlMap =
        CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<String, LegacyQueue>() {
            @Override
            public LegacyQueue load(String queueName ) throws Exception {

                LegacyQueue queue = null;

                try {
                    GetQueueUrlResult result = sqs.getQueueUrl( queueName );
                    queue = new LegacyQueue( result.getQueueUrl() );
                }
                catch ( QueueDoesNotExistException queueDoesNotExistException ) {
                    logger.error( "Queue {} does not exist, will create", queueName );
                }
                catch ( Exception e ) {
                    logger.error( "failed to get queue from service", e );
                    throw e;
                }

                if ( queue == null ) {
                    String url = AmazonNotificationUtils.createQueue( sqs, queueName, fig );
                    queue = new LegacyQueue( url );
                }

                setupTopics( queueName );

                return queue;
            }
        } );


    @Inject
    public SNSQueueManagerImpl(@Assisted LegacyQueueScope scope, LegacyQueueFig fig, ClusterFig clusterFig,
                               CassandraConfig cassandraConfig, LegacyQueueFig queueFig ) {
        this.scope = scope;
        this.fig = fig;
        this.clusterFig = clusterFig;
        this.cassandraConfig = cassandraConfig;


        // create our own executor which has a bounded queue w/ caller runs policy for rejected tasks
        final ExecutorService executor = TaskExecutorFactory
            .createTaskExecutor( "amazon-async-io", queueFig.getAsyncMaxThreads(), queueFig.getAsyncQueueSize(),
                TaskExecutorFactory.RejectionAction.CALLERRUNS );


        final Region region = getRegion();

        this.clientConfiguration = new ClientConfiguration()
            .withConnectionTimeout(queueFig.getQueueClientConnectionTimeout())
            // don't let the socket timeout be configured less than 5 sec (network delays do happen)
            .withSocketTimeout(Math.max(MIN_CLIENT_SOCKET_TIMEOUT, queueFig.getQueueClientSocketTimeout()))
            .withGzip(true);

        try {
            sqs = createSQSClient( region );
            sns = createSNSClient( region );
            snsAsync = createAsyncSNSClient( region, executor );
            sqsAsync = createAsyncSQSClient( region, executor );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Error setting up mapper", e );
        }
    }


    private String setupTopics( final String queueName ) throws Exception {

        logger.info( "Setting up setupTopics SNS/SQS..." );

        String primaryTopicArn = AmazonNotificationUtils.getTopicArn( sns, queueName, true );

        if ( logger.isTraceEnabled() ) {
            logger.trace( "SNS/SQS Setup: primaryTopicArn={}", primaryTopicArn );
        }

        String queueUrl = AmazonNotificationUtils.getQueueUrlByName( sqs, queueName );
        String primaryQueueArn = AmazonNotificationUtils.getQueueArnByName( sqs, queueName );

        if ( logger.isTraceEnabled() ) {
            logger.trace( "SNS/SQS Setup: primaryQueueArn={}", primaryQueueArn );
        }

        if ( primaryQueueArn == null ) {
            if ( logger.isTraceEnabled() ) {
                logger.trace( "SNS/SQS Setup: primaryQueueArn is null, creating queue..." );
            }

            queueUrl = AmazonNotificationUtils.createQueue( sqs, queueName, fig );
            primaryQueueArn = AmazonNotificationUtils.getQueueArnByUrl( sqs, queueUrl );

            if ( logger.isTraceEnabled() ) {
                logger.trace( "SNS/SQS Setup: New Queue URL=[{}] ARN=[{}]", queueUrl, primaryQueueArn );
            }
        }

        try {

            SubscribeRequest primarySubscribeRequest = new SubscribeRequest( primaryTopicArn, "sqs", primaryQueueArn );
            sns.subscribe( primarySubscribeRequest );

            // ensure the SNS primary topic has permission to send to the primary SQS queue
            List<String> primaryTopicArnList = new ArrayList<>();
            primaryTopicArnList.add( primaryTopicArn );
            AmazonNotificationUtils.setQueuePermissionsToReceive( sqs, queueUrl, primaryTopicArnList );
        }
        catch ( AmazonServiceException e ) {
            logger.error(
                "Unable to subscribe PRIMARY queue=[{}] to topic=[{}]", queueUrl, primaryTopicArn, e );
        }

        if ( fig.isMultiRegion() && scope.getRegionImplementation() == LegacyQueueScope.RegionImplementation.ALL ) {

            String multiRegion = fig.getRegionList();

            if ( logger.isTraceEnabled() ) {
                logger.trace( "MultiRegion Setup specified, regions: [{}]", multiRegion );
            }

            String[] regionNames = multiRegion.split( "," );

            final Map<String, String> arrQueueArns = new HashMap<>( regionNames.length + 1 );
            final Map<String, String> topicArns = new HashMap<>( regionNames.length + 1 );

            arrQueueArns.put(primaryQueueArn, fig.getPrimaryRegion());
            topicArns.put(primaryTopicArn, fig.getPrimaryRegion());

            for ( String regionName : regionNames ) {

                regionName = regionName.trim();
                Regions regions = Regions.fromName( regionName );
                Region region = Region.getRegion( regions );

                AmazonSQSClient sqsClient = createSQSClient( region );
                AmazonSNSClient snsClient = createSNSClient( region ); // do this stuff synchronously

                // getTopicArn will create the SNS topic if it doesn't exist
                String topicArn = AmazonNotificationUtils.getTopicArn( snsClient, queueName, true );
                topicArns.put( topicArn, regionName );

                // create the SQS queue if it doesn't exist
                String queueArn = AmazonNotificationUtils.getQueueArnByName( sqsClient, queueName );
                if ( queueArn == null ) {
                    queueUrl = AmazonNotificationUtils.createQueue( sqsClient, queueName, fig );
                    queueArn = AmazonNotificationUtils.getQueueArnByUrl( sqsClient, queueUrl );
                }

                arrQueueArns.put( queueArn, regionName );
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Creating Subscriptions...");
            }

            for ( Map.Entry<String, String> queueArnEntry : arrQueueArns.entrySet() ) {
                String queueARN = queueArnEntry.getKey();
                String strSqsRegion = queueArnEntry.getValue();

                Regions sqsRegions = Regions.fromName( strSqsRegion );
                Region sqsRegion = Region.getRegion( sqsRegions );

                AmazonSQSClient subscribeSqsClient = createSQSClient( sqsRegion );

                // ensure the URL used to subscribe is for the correct name/region
                String subscribeQueueUrl = AmazonNotificationUtils.getQueueUrlByName( subscribeSqsClient, queueName );

                // this list used later for adding permissions to queues
                List<String> topicArnList = new ArrayList<>();

                for ( Map.Entry<String, String> topicArnEntry : topicArns.entrySet() ) {

                    String topicARN = topicArnEntry.getKey();
                    topicArnList.add( topicARN );

                    String strSnsRegion = topicArnEntry.getValue();
                    Regions snsRegions = Regions.fromName( strSnsRegion );
                    Region snsRegion = Region.getRegion( snsRegions );

                    AmazonSNSClient subscribeSnsClient = createSNSClient( snsRegion ); // do this stuff synchronously
                    SubscribeRequest subscribeRequest = new SubscribeRequest( topicARN, "sqs", queueARN );

                    try {

                        logger.info( "Subscribing Queue ARN/Region=[{} / {}] and Topic ARN/Region=[{} / {}]", queueARN,
                            strSqsRegion, topicARN, strSnsRegion );

                        SubscribeResult subscribeResult = subscribeSnsClient.subscribe( subscribeRequest );
                        String subscriptionARN = subscribeResult.getSubscriptionArn();
                        if ( logger.isTraceEnabled() ) {
                            logger.trace(
                                "Successfully subscribed Queue ARN=[{}] to Topic ARN=[{}], subscription ARN=[{}]",
                                queueARN, topicARN, subscriptionARN );
                        }
                    }
                    catch ( Exception e ) {
                        logger.error( "ERROR Subscribing Queue ARN/Region=[{} / {}] and Topic ARN/Region=[{} / {}]",
                                queueARN, strSqsRegion, topicARN, strSnsRegion , e );
                    }
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("Adding permission to receive messages...");
                }
                // add permission to each queue, providing a list of topics that it's subscribed to
                AmazonNotificationUtils
                    .setQueuePermissionsToReceive( subscribeSqsClient, subscribeQueueUrl, topicArnList );
            }
        }

        return primaryTopicArn;
    }


    /**
     * The Asynchronous SNS client is used for publishing events to SNS.
     */

    private AmazonSNSAsyncClient createAsyncSNSClient( final Region region, final ExecutorService executor ) {

        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        final AmazonSNSAsyncClient sns =
            new AmazonSNSAsyncClient( ugProvider.getCredentials(), clientConfiguration, executor );

        sns.setRegion( region );

        return sns;
    }


    /**
     * Create the async sqs client
     */
    private AmazonSQSAsyncClient createAsyncSQSClient( final Region region, final ExecutorService executor ) {

        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        final AmazonSQSAsyncClient sqs =
            new AmazonSQSAsyncClient( ugProvider.getCredentials(),clientConfiguration,  executor );

        sqs.setRegion( region );

        return sqs;
    }


    /**
     * The Synchronous SNS client is used for creating topics and subscribing queues.
     */
    private AmazonSNSClient createSNSClient( final Region region ) {

        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        final AmazonSNSClient sns =
            new AmazonSNSClient( ugProvider.getCredentials(), clientConfiguration );

        sns.setRegion( region );

        return sns;
    }


    private String getName() {
        String name =
            clusterFig.getClusterName() + "_" + cassandraConfig.getApplicationKeyspace() + "_" + scope.getName() + "_"
                + scope.getRegionImplementation();
        name = name.toLowerCase(); //user lower case values
        Preconditions.checkArgument( name.length() <= 80, "Your name must be < than 80 characters" );

        return name;
    }


    public LegacyQueue getReadQueue() {
        String queueName = getName();

        try {
            return readQueueUrlMap.get( queueName );
        }
        catch ( ExecutionException ee ) {
            throw new RuntimeException( ee );
        }
    }


    public String getWriteTopicArn() {
        try {
            return writeTopicArnMap.get( getName() );
        }
        catch ( ExecutionException ee ) {
            throw new RuntimeException( ee );
        }
    }


    @Override
    public List<LegacyQueueMessage> getMessages(final int limit, final Class klass) {

        if ( sqs == null ) {
            logger.error( "SQS is null - was not initialized properly" );
            return new ArrayList<>(0);
        }

        String url = getReadQueue().getUrl();

        if ( logger.isTraceEnabled() ) {
            logger.trace( "Getting up to {} messages from {}", limit, url );
        }

        ArrayList<String> requestMessageAttributeNames = new ArrayList<String>(1);
        requestMessageAttributeNames.add("ApproximateReceiveCount");


        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest( url );
        receiveMessageRequest.setAttributeNames(requestMessageAttributeNames);
        receiveMessageRequest.setMaxNumberOfMessages( limit );
        receiveMessageRequest.setVisibilityTimeout(
            Math.max( MIN_VISIBILITY_TIMEOUT, fig.getVisibilityTimeout() / 1000 ) );


        int longPollTimeout = Math.min(20000, fig.getQueuePollTimeout()); // 20000 is the SQS maximum

        // ensure the client's socket timeout is not less than the configure long poll timeout
        if( fig.getQueueClientSocketTimeout() < longPollTimeout){

            longPollTimeout = Math.max(0, fig.getQueueClientSocketTimeout() - 1000);

        }

        receiveMessageRequest.setWaitTimeSeconds( longPollTimeout / 1000 ); // convert to seconds

        try {
            ReceiveMessageResult result = sqs.receiveMessage( receiveMessageRequest );
            List<Message> messages = result.getMessages();

            if ( logger.isTraceEnabled() ) {
                logger.trace( "Received {} messages from {}", messages.size(), url );
            }

            List<LegacyQueueMessage> queueMessages = new ArrayList<>( messages.size() );

            for ( Message message : messages ) {

                Object payload;
                final String originalBody = message.getBody();

                try {
                    final JsonNode bodyNode = mapper.readTree( message.getBody() );

                    /**
                     * When a message originates from SNS it has a "Message"  we have to extract
                     * it and then process it separately
                     */


                    if ( bodyNode.has( "Message" ) ) {
                        final String snsNode = bodyNode.get( "Message" ).asText();

                        payload = deSerializeSQSMessage( snsNode, klass );
                    }
                    else {
                        payload = deSerializeSQSMessage( originalBody, klass );
                    }
                }
                catch ( Exception e ) {
                    logger.error( "failed to deserialize message: {}", message.getBody(), e );
                    throw new RuntimeException( e );
                }

                LegacyQueueMessage queueMessage = new LegacyQueueMessage( message.getMessageId(), message.getReceiptHandle(), payload,
                    message.getAttributes().get( "type" ) );
                queueMessage.setStringBody( originalBody );
                int receiveCount = Integer.valueOf(message.getAttributes().get("ApproximateReceiveCount"));
                queueMessage.setReceiveCount( receiveCount );
                queueMessages.add( queueMessage );
            }

            return  queueMessages ;
        }
        catch ( com.amazonaws.services.sqs.model.QueueDoesNotExistException dne ) {
            logger.error( "Queue does not exist! [{}]", url , dne );
        }
        catch ( Exception e ) {
            logger.error( "Programming error getting messages from queue=[{}] exist!", url, e );
        }

        return  new ArrayList<>( 0 ) ;
    }


    /**
     * Take a string, possibly escaped via SNS, and run it through our mapper to create an object)
     */
    private Object deSerializeSQSMessage( final String message, final Class type ) {
        try {
            final Object o = mapper.readValue( message, type );
            return o;
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to deserialize message " + message + " for class " + type, e );
        }
    }


    @Override
    public long getQueueDepth() {
        String key = "ApproximateNumberOfMessages";
        try {
            GetQueueAttributesResult result =
                sqs.getQueueAttributes( getReadQueue().getUrl(), Collections.singletonList( key ) );
            String depthString = result.getAttributes().get( key );
            return depthString != null ? Long.parseLong( depthString ) : 0;
        }
        catch ( Exception e ) {
            logger.error( "Exception getting queue depth", e );
            return -1;
        }
    }


    @Override
    public <T extends Serializable> void sendMessageToAllRegions(final T body ) throws IOException {
        if ( snsAsync == null ) {
            logger.error( "SNS client is null, perhaps it failed to initialize successfully" );
            return;
        }

        final String stringBody = toString( body );

        String topicArn = getWriteTopicArn();

        if ( logger.isTraceEnabled() ) {
            logger.trace( "Publishing Message...{} to arn: {}", stringBody, topicArn );
        }

        PublishRequest publishRequest = new PublishRequest( topicArn, stringBody );

        snsAsync.publishAsync( publishRequest, new AsyncHandler<PublishRequest, PublishResult>() {
            @Override
            public void onError( Exception e ) {
                logger.error( "Error publishing message... {}", e );
            }


            @Override
            public void onSuccess( PublishRequest request, PublishResult result ) {
                if ( logger.isTraceEnabled() ) {
                    logger.trace( "Successfully published... messageID=[{}],  arn=[{}]", result.getMessageId(),
                        request.getTopicArn() );
                }
            }
        } );
    }


    @Override
    public void sendMessages( final List bodies ) throws IOException {

        if ( sqsAsync == null ) {
            logger.error( "SQS client is null, perhaps it failed to initialize successfully" );
            return;
        }

        for ( Object body : bodies ) {
            sendMessageToLocalRegion( ( Serializable ) body );
        }
    }


    @Override
    public <T extends Serializable> void sendMessageToLocalRegion(final T body ) throws IOException {

        if ( sqsAsync == null ) {
            logger.error( "SQS client is null, perhaps it failed to initialize successfully" );
            return;
        }
        final String stringBody = toString( body );

        String url = getReadQueue().getUrl();

        if ( logger.isTraceEnabled() ) {
            logger.trace( "Publishing Message...{} to url: {}", stringBody, url );
        }

        SendMessageRequest request = new SendMessageRequest( url, stringBody );

        sqsAsync.sendMessageAsync( request, new AsyncHandler<SendMessageRequest, SendMessageResult>() {

            @Override
            public void onError( final Exception e ) {

                logger.error( "Error sending message... {}", e );
            }


            @Override
            public void onSuccess( final SendMessageRequest request, final SendMessageResult sendMessageResult ) {
                if ( logger.isTraceEnabled() ) {
                    logger.trace( "Successfully send... messageBody=[{}],  url=[{}]", request.getMessageBody(),
                        request.getQueueUrl() );
                }
            }
        } );
    }


    @Override
    public void deleteQueue() {
        logger.warn( "Deleting queue: " + getReadQueue().getUrl() );
        sqs.deleteQueue( new DeleteQueueRequest().withQueueUrl( getReadQueue().getUrl() ) );
        logger.warn( "Deleting queue: " + getReadQueue().getUrl() + "_dead" );
        sqs.deleteQueue( new DeleteQueueRequest().withQueueUrl( getReadQueue().getUrl() + "_dead" ) );
    }


    @Override
    public void commitMessage( final LegacyQueueMessage queueMessage ) {
        String url = getReadQueue().getUrl();
        if ( logger.isTraceEnabled() ) {
            logger.trace( "Commit message {} to queue {}", queueMessage.getMessageId(), url );
        }

        sqs.deleteMessage(
            new DeleteMessageRequest().withQueueUrl( url ).withReceiptHandle( queueMessage.getHandle() ) );
    }


    @Override
    public void commitMessages( final List<LegacyQueueMessage> queueMessages ) {
        String url = getReadQueue().getUrl();

        if ( logger.isTraceEnabled() ) {
            logger.trace( "Commit messages {} to queue {}", queueMessages.size(), url );
        }

        List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();

        for ( LegacyQueueMessage message : queueMessages ) {
            entries.add( new DeleteMessageBatchRequestEntry( message.getMessageId(), message.getHandle() ) );
        }

        DeleteMessageBatchRequest request = new DeleteMessageBatchRequest( url, entries );
        DeleteMessageBatchResult result = sqs.deleteMessageBatch( request );

        boolean successful = result.getFailed().size() <= 0;

        if ( !successful ) {
            for ( BatchResultErrorEntry failed : result.getFailed() ) {
                logger.error( "Commit failed reason: {} messages id: {}", failed.getMessage(), failed.getId() );
            }
        }
    }


    /**
     * Write the object to a Base64 string.
     */
    private String toString( final Object o ) throws IOException {
        return mapper.writeValueAsString( o );
    }


    /**
     * Get the region
     */
    private Region getRegion() {
        Regions regions = Regions.fromName(fig.getPrimaryRegion());
        return Region.getRegion(regions);
    }


    /**
     * Create the SQS client for the specified settings
     */
    private AmazonSQSClient createSQSClient( final Region region ) {

        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        final AmazonSQSClient sqs =
            new AmazonSQSClient( ugProvider.getCredentials(), clientConfiguration );

        sqs.setRegion( region );

        return sqs;
    }

    @Override
    public void clearQueueNameCache(){
       //no-op
    }
}
