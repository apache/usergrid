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
package org.apache.usergrid.services.queues;


import java.util.List;

import org.apache.usergrid.persistence.queue.QueueMessage;


/**
 * The queue processor that type casts the queue messages and does the work reovling around import queue messages.
 */
public class ImportQueueProcessor implements QueueProcessor {

    /**
     * Does the work that is mentioned by the queue messages. Takes the body of the message and decodes back into
     * useful information. Then starts the importing process.
     * @param messages
     */
    @Override
    public void execute( final List<QueueMessage> messages ) {
        //(ImportMessage)messages.get(0).getBody();
        //do work see: ApplicationsQueueMessage for a sample
    }
}
