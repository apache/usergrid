/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.persistence.queue;

import rx.Observable;

import java.io.IOException;
import java.util.AbstractQueue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Default queue manager implementation, uses in memory linked queue
 */
public class LocalQueueManager implements QueueManager {
    public ArrayBlockingQueue<QueueMessage> queue = new ArrayBlockingQueue<>(10000);

    @Override
    public    List<QueueMessage> getMessages(int limit, int transactionTimeout, int waitTime, Class klass) {
        List<QueueMessage> returnQueue = new ArrayList<>();
        try {
            QueueMessage message=null;
            int count = 5;
            do {
                message = queue.poll(100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    returnQueue.add(message);
                }
            }while(message!=null && count-->0);
        }catch (InterruptedException ie){
            throw new RuntimeException(ie);
        }
        return returnQueue;
    }

    @Override
    public long getQueueDepth() {
        return queue.size();
    }

    @Override
    public void commitMessage(QueueMessage queueMessage) {
    }

    @Override
    public void commitMessages(List<QueueMessage> queueMessages) {
    }

    @Override
    public  void sendMessages(List bodies) throws IOException {
        for(Object body : bodies){
            String uuid = UUID.randomUUID().toString();
            try {
                queue.put(new QueueMessage(uuid, "handle_" + uuid, body, "put type here"));
            }catch (InterruptedException ie){
                throw new RuntimeException(ie);
            }
        }
    }


    @Override
    public <T extends Serializable> void sendMessage( final T body ) throws IOException {
        String uuid = UUID.randomUUID().toString();
        try {
            queue.offer(new QueueMessage(uuid, "handle_" + uuid, body, "put type here"),5000,TimeUnit.MILLISECONDS);
        }catch (InterruptedException ie){
            throw new RuntimeException(ie);
        }
    }



    @Override
    public <T extends Serializable> void sendMessageToTopic( final T body ) throws IOException {
       sendMessage( body );
    }


    @Override
    public void deleteQueue() {

    }
}
