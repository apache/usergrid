package org.apache.usergrid.persistence.queue.impl;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.persistence.queue.*;

import java.util.ArrayList;
import java.util.List;

public class QueueManagerImpl implements QueueManager {
    private final AmazonSQSClient sqs;
    private final QueueScope scope;
    private final QueueFig fig;
    private Queue queue;

    @Inject
    public QueueManagerImpl(@Assisted QueueScope scope, QueueFig fig){
        this.fig = fig;
        this.scope = scope;
        EnvironmentVariableCredentialsProvider credsProvider = new EnvironmentVariableCredentialsProvider();
        this.sqs = new AmazonSQSClient(credsProvider.getCredentials());
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
        return scope.getApplication().getUuid().toString()+ scope.getName();
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
        return queue;
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
}
