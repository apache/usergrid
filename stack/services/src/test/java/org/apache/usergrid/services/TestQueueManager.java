package org.apache.usergrid.services;

import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by ApigeeCorporation on 10/7/14.
 */
public class TestQueueManager implements QueueManager {
    public List<QueueMessage> queue = new ArrayList<>();
    @Override
    public List<QueueMessage> getMessages(int limit, int transactionTimeout, int waitTime, Class klass) {
        List<QueueMessage> returnQueue = new ArrayList<>();
        for(int i=0;i<limit;i++){
            if(!queue.isEmpty()){
                returnQueue.add( queue.remove(0));
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
    public void sendMessages(List bodies) throws IOException {
        for(Object body : bodies){
            String uuid = UUID.randomUUID().toString();
            queue.add(new QueueMessage(uuid,"handle_"+uuid,body));
        }
    }

    @Override
    public void sendMessage(Object body) throws IOException {
        String uuid = UUID.randomUUID().toString();
        queue.add(new QueueMessage(uuid,"handle_"+uuid,body));
    }
}
