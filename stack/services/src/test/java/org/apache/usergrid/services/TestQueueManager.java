/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
