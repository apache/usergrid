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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Default queue manager implementation, uses in memory linked queue
 */
public class DefaultQueueManager implements QueueManager {
    public ArrayBlockingQueue<QueueMessage> queue = new ArrayBlockingQueue<>(10000);
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
            queue.add(new QueueMessage(uuid,"handle_"+uuid,body,"putappriate type here"));
        }
    }

    @Override
    public synchronized void sendMessage(Object body) throws IOException {
        String uuid = UUID.randomUUID().toString();
        queue.add(new QueueMessage(uuid,"handle_"+uuid,body,"put type here"));
    }
}
