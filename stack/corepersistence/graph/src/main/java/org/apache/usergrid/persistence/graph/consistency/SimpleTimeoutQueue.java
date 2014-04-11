package org.apache.usergrid.persistence.graph.consistency;


import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyImpl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;


/**
 *
 *The way this should work is that a single SimpleTimeoutQueue will be created under a single name. Then it will be called
 * from many different clusters storing stuff in the same queue.
 */
public class SimpleTimeoutQueue implements  TimeoutQueue {

    static final private String VisibilityTimeoutAttr = "VisibilityTimeout";
    //final SimpleTimeoutQueue simpleTimeoutQueue;

    private String queueEndpoint;

    public static final SmileFactory f = new SmileFactory(  );

    public static ObjectMapper mapper = new ObjectMapper(f);

    public String getQueueEndpoint() {
        return queueEndpoint;
    }

    public void setQueueEndpoint(String queueEndpoint) {
        this.queueEndpoint = queueEndpoint;
    }

    public SimpleTimeoutQueue(String queueEndpoint) throws Exception {
        this.queueEndpoint = queueEndpoint;
        //check to make sure queue isn't null if so create , otherwise just use existing.
       // simpleTimeoutQueue = //
    }
//TODO: my implementation doesn't requie the use of endpoints so I need to see what impact that
    //has on my model.
    //TODO: dead letter queue.

//    /**
//     *
//     * @param name The name of the queue you want to create.
//     * @return
//     * @throws Exception
//     */
//    static public SimpleTimeoutQueue createQueue(String name) throws Exception {
//
//        AWSCredentials awsCredentials = new BasicAWSCredentials("derper","derp" );
//        CreateQueueRequest createQueueRequest = new CreateQueueRequest( name );
//        createQueueRequest.addAttributesEntry( VisibilityTimeoutAttr, )
//
//        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient(awsCredentials);
//
//
//        sqsAsyncClient.createQueue( name );
//
//        //creates the queue
//        CreateQueueResult createQueueResult = sqsAsyncClient.createQueue( name );
//
//        SimpleTimeoutQueue  simpleTimeoutQueue= new SimpleTimeoutQueue( createQueueRequest.getQueueName());
//        //Queue newQueue = new Queue(cqr.getCreateQueueResult().getQueueUrl());
//        System.out.println("Created a new queue.  Queue url: " + simpleTimeoutQueue.getQueueEndpoint());
//        return simpleTimeoutQueue;
//    }


    /**
     * Adds events to a new queue that is created.
     * @param event The event to queue
     * @param timeout The timeout to set on the queue element
     * @return
     */
    @Override
    public AsynchronousMessage queue( final Object event, final long timeout ) {
        //this needs to be subsituted with the actual credentials which will be given through some unknown method.
//
        AWSCredentials awsCredentials = new BasicAWSCredentials("derper","derp" );
        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient(awsCredentials);

        SimpleAsynchronousMessage<Object> asynchronousMessage = new SimpleAsynchronousMessage<>( event,timeout );

//TODO: check if event is instanceof serializable. if not blow up.
        //binary serialize it


        SendMessageRequest sendMessageRequest = new SendMessageRequest( getQueueEndpoint(), mapper.writeValueAsString(
                asynchronousMessage )); /*encoding of asyncMessage here */);
        //should change this back to async send message.
        SendMessageResult sendMessageResult = sqsAsyncClient.sendMessage( sendMessageRequest );

        return asynchronousMessage;
    }

//are we going to want different timeouts for each query command
    //a timeout before the elemtn becomes visible again after you read it.
    @Override
    public Collection<AsynchronousMessage> take( final int maxSize, final long timeout ) {

        /* this will need to be abstracted out */
        AWSCredentials awsCredentials = new BasicAWSCredentials("derper","derp" );
        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient(awsCredentials);
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest( getQueueEndpoint() );
        receiveMessageRequest.setVisibilityTimeout( ( int ) timeout );
        Collection<AsynchronousMessage> asynchronousMessageCollection;


        //recieves a single message back from the queue because default return is one. For reference
        //max number able to be returned is 10.
        for (int index = 0; index < maxSize; index++) {
             ReceiveMessageResult receiveMessageResult = sqsAsyncClient.receiveMessage(receiveMessageRequest);
            //I can only get out from the result using getMessages.
             List<Message> messageList =  receiveMessageResult.getMessages();

            //always get the first message because there should only be one

            try {
                asynchronousMessageCollection.add( mapper.readValue( messageList.get( 0 ).getBody(),AsynchronousMessage.class) );
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }



        return null;
    }


    @Override
    public boolean remove( final AsynchronousMessage event ) {
        AWSCredentials awsCredentials = new BasicAWSCredentials("derper","derp" );
        AmazonSQSAsyncClient sqsAsyncClient = new AmazonSQSAsyncClient(awsCredentials);
        try {
            sqsAsyncClient.deleteMessage( getQueueEndpoint(), event.toString() );
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
