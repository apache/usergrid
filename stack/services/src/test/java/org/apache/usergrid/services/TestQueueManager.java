package org.apache.usergrid.services;

import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ApigeeCorporation on 10/7/14.
 */
public class TestQueueManager implements QueueManager {
    public ConcurrentLinkedQueue<QueueMessage> queue = new ConcurrentLinkedQueue<>();
    @Override
    public synchronized List<QueueMessage> getMessages(int limit, int transactionTimeout, int waitTime, Class klass) {
        List<QueueMessage> returnQueue = new ArrayList<>();
        for(int i=0;i<limit;i++){
            if(!queue.isEmpty()){
                returnQueue.add( queue.remove());
            }else{
                break;
            }
        }
        return returnQueue;
    }

    @Override
    public void commitMessage(QueueMessage queueMessage) {
    }

    @Override
    public void commitMessages(List<QueueMessage> queueMessages) {
    }

    @Override
    public synchronized void sendMessages(List bodies) throws IOException {
        for(Object body : bodies){
            String uuid = UUID.randomUUID().toString();
            queue.add(new QueueMessage(uuid,"handle_"+uuid,body));
        }
    }

    @Override
    public synchronized void sendMessage(Object body) throws IOException {
        String uuid = UUID.randomUUID().toString();
        queue.add(new QueueMessage(uuid,"handle_"+uuid,body));
    }
}
