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
package org.apache.usergrid.persistence.queue;

import rx.Observable;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Manages queues for usergrid.  Current implementation is sqs based.
 */
public interface QueueManager {

    /**
     * Read messages from queue
     * @param limit
     * @param transactionTimeout timeout in seconds
     * @param waitTime wait time for next message in milliseconds
     * @param klass class to cast the return from
     * @return List of Queue Messages
     */
    List<QueueMessage> getMessages(int limit,int transactionTimeout, int waitTime, Class klass);

    /**
     * get the queue depth
     * @return
     */
    long getQueueDepth();

    /**
     * Commit the transaction
     * @param queueMessage
     */
    void commitMessage( QueueMessage queueMessage);

    /**
     * commit multiple messages
     * @param queueMessages
     */
    void commitMessages( List<QueueMessage> queueMessages);

    /**
     * send messages to queue
     * @param bodies body objects must be serializable
     * @throws IOException
     */
    void sendMessages(List bodies) throws IOException;

    /**
     * send a message to queue
     * @param body
     * @throws IOException
     */
    <T extends Serializable> void sendMessage(T body)throws IOException;

    /**
     * Send a messae to the topic to be sent to other queues
     * @param body
     */
    <T extends Serializable> void sendMessageToTopic(T body) throws IOException;

    /**
     * purge messages
     */
    void deleteQueue();
}
