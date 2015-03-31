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
    void sendMessage(Object body)throws IOException;
}
