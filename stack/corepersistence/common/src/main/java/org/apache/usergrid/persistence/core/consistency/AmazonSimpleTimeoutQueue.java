package org.apache.usergrid.persistence.core.consistency;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;


/**
 * The way this should work is that a single AmazonSimpleTimeoutQueue will be created under a single name. Then it will
 * be called from many different clusters storing stuff in the same queue.
 */
public class AmazonSimpleTimeoutQueue<T extends Serializable> implements TimeoutQueue<T> {

    private static final Logger logger = LoggerFactory.getLogger( AmazonSimpleTimeoutQueue.class );


    static final private String VisibilityTimeoutAttr = "VisibilityTimeout";
    static final private int MaxNumberOfMessages = 10;
    //final AmazonSimpleTimeoutQueue simpleTimeoutQueue;

    private String queueEndpoint;


    public static ObjectMapper mapper = new ObjectMapper();


    public String getQueueEndpoint() {
        return queueEndpoint;
    }


    public void setQueueEndpoint( String queueEndpoint ) {
        this.queueEndpoint = queueEndpoint;
    }


    public AmazonSimpleTimeoutQueue( String queueEndpoint ) throws Exception {
        createQueue( queueEndpoint );
    }

    //TODO: dead letter queue.


    /**
     * Takes in the name of the queue and sets the queueEndpoint.
     */
    public void createQueue( String name ) throws Exception {

        AWSCredentials awsCredentials =
                new BasicAWSCredentials( System.getProperty( "accessKey" ), System.getProperty( "secretKey" ) );
        CreateQueueRequest createQueueRequest = new CreateQueueRequest( name );
        createQueueRequest.addAttributesEntry( VisibilityTimeoutAttr,"1" );

        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient( awsCredentials );

        //creates the queue
        CreateQueueResult createQueueResult = sqsAsyncClient.createQueue( createQueueRequest );
        this.queueEndpoint = createQueueResult.getQueueUrl();

        logger.debug( "Created a new queue.  Queue url: " + queueEndpoint );
    }


    /**
     * Adds events to a new queue that is created.
     *
     * @param event The event to queue
     * @param timeout The timeout to set on the queue element
     */
    @Override
    public AsynchronousMessage<T> queue( final T event, final long timeout ) {
        //this needs to be subsituted with the actual credentials which will be given through some unknown method.
        //
        AWSCredentials awsCredentials =
                new BasicAWSCredentials( System.getProperty( "accessKey" ), System.getProperty( "secretKey" ) );
        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient( awsCredentials );

        SimpleAsynchronousMessage<T> asynchronousMessage = new SimpleAsynchronousMessage<T>( event, timeout );
        SendMessageResult sendMessageResult = null;

        SendMessageRequest sendMessageRequest = null; /*encoding of asyncMessage here */
        try {
            sendMessageRequest =
                    new SendMessageRequest( getQueueEndpoint(), mapper.writeValueAsString( asynchronousMessage ) );

            sendMessageRequest.setDelaySeconds( ( int ) timeout );

            sqsAsyncClient.sendMessage( sendMessageRequest );
        }
        catch ( JsonProcessingException e ) {
            logger.error( "Couldn't serialize the following event: " + event.toString() );
            throw new RuntimeException(e.getMessage());
        }
        catch ( IOException e ) {
            logger.error( "Couldn't serialize the following event: " + event.toString() );
            throw new RuntimeException(e.getMessage());
        }

        logger.debug( "Finished adding " + asynchronousMessage.toString() + " to the following queue: " +queueEndpoint );

        return asynchronousMessage;
    }


    @Override
    public Collection<AsynchronousMessage<T>> take( final int maxSize, final long timeout ) {

        /* this will need to be abstracted out */
        AWSCredentials awsCredentials =
                new BasicAWSCredentials( System.getProperty( "accessKey" ), System.getProperty( "secretKey" ) );
        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient( awsCredentials );
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest( getQueueEndpoint() );
        receiveMessageRequest.setVisibilityTimeout( ( int ) timeout );
        receiveMessageRequest.setMaxNumberOfMessages( MaxNumberOfMessages );
        Collection<AsynchronousMessage<T>> asynchronousMessageCollection;
        asynchronousMessageCollection = new ArrayList<>();//new Collection<AsynchronousMessage<T>>();


        //recieves a single message back from the queue because default return is one. For reference
        //max number able to be returned is 10.
        for ( int index = 0; index < (maxSize / 10 + 1); index++ ) {
            ReceiveMessageResult receiveMessageResult = sqsAsyncClient.receiveMessage( receiveMessageRequest );
            //I can only get out from the result using getMessages.
            List<Message> messageList = receiveMessageResult.getMessages();

            if ( messageList.size() != 0 ) {
                //always get the first message because there should only be one

                try {
                    for ( int j = 0; j < messageList.size(); j++ ) {
                        //TODO: make this simplier
                        SimpleAsynchronousMessage<T> simpleAsynchronousMessage =
                                mapper.readValue( messageList.get( j ).getBody(), SimpleAsynchronousMessage.class );

                        AmazonSimpleQueueMessage<T> amazonSimpleQueueMessage =
                                new AmazonSimpleQueueMessage<T>( simpleAsynchronousMessage.getEvent(),
                                        simpleAsynchronousMessage.getTimeout(), messageList.get( j ).getMessageId(),
                                        messageList.get( j ).getReceiptHandle() );


                        asynchronousMessageCollection.add( amazonSimpleQueueMessage );
                    }
                }
                catch ( IOException e ) {
                    e.printStackTrace();
                    return null;
                }
            }
            else {
                break;
            }
        }
        logger.debug( "Took " +asynchronousMessageCollection.size() + " queue elements." );

        return asynchronousMessageCollection;
    }


    @Override
    public boolean remove( final AsynchronousMessage<T> event ) {
        AWSCredentials awsCredentials =
                new BasicAWSCredentials( System.getProperty( "accessKey" ), System.getProperty( "secretKey" ) );
        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient( awsCredentials );
        try {
            AmazonSimpleQueueMessage amazonSimpleQueueMessage = ( AmazonSimpleQueueMessage ) event;
            sqsAsyncClient.deleteMessage( getQueueEndpoint(), amazonSimpleQueueMessage.getReceiptHandle() );
            logger.debug( "Removed the uuid " +amazonSimpleQueueMessage.getMessageId() + " from the queue." );
        }
        catch ( Exception e ) {
            logger.error( "Failed to delete the following event:" + ( ( AmazonSimpleQueueMessage ) event ).getMessageId()  );
            throw new RuntimeException( e.getMessage() );
        }

        return true;
    }
}
