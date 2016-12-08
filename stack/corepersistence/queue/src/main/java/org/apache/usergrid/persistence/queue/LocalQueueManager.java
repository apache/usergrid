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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Default queue manager implementation, uses in memory linked queue
 */
public class LocalQueueManager implements LegacyQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalQueueManager.class);

    public ArrayBlockingQueue<LegacyQueueMessage> queue = new ArrayBlockingQueue<>(10000);

    private LegacyQueueScope scope;

    @Inject
    public LocalQueueManager(@Assisted LegacyQueueScope scope){
        this.scope = scope;
    }

    @Override
    public    List<LegacyQueueMessage> getMessages(int limit, Class klass) {
        List<LegacyQueueMessage> returnQueue = new ArrayList<>();
        try {
            LegacyQueueMessage message=null;
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
    public void commitMessage(LegacyQueueMessage queueMessage) {
    }

    @Override
    public void commitMessages(List<LegacyQueueMessage> queueMessages) {
    }

    @Override
    public  void sendMessages(List bodies) throws IOException {
        for(Object body : bodies){
            String uuid = UUID.randomUUID().toString();
            try {
                queue.put(new LegacyQueueMessage(uuid, "handle_" + uuid, body, "put type here"));
            }catch (InterruptedException ie){
                throw new RuntimeException(ie);
            }
        }
    }


    @Override
    public <T extends Serializable> void sendMessageToLocalRegion(final T body ) throws IOException {
        String uuid = UUID.randomUUID().toString();
        try {
            queue.offer(new LegacyQueueMessage(uuid, "handle_" + uuid, body, "put type here"),5000,TimeUnit.MILLISECONDS);
        }catch (InterruptedException ie){
            throw new RuntimeException(ie);
        }
    }



    @Override
    public <T extends Serializable> void sendMessageToAllRegions(final T body ) throws IOException {
       sendMessageToLocalRegion( body );
    }


    @Override
    public void deleteQueue() {
        //no-op
    }

    @Override
    public void clearQueueNameCache(){
        //no-op
    }
}
