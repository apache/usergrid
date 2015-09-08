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

package org.apache.usergrid.services.notifications;

import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.queue.QueueMessage;
import rx.Observable;

import java.util.List;

/**
 * Manages Queues for Applications
 */
public interface ApplicationQueueManager {

    public static final String DEFAULT_QUEUE_PROPERTY = "usergrid.notifications.listener.queue";

    public static final String NOTIFIER_ID_POSTFIX = ".notifier.id";

    public static final  String DEFAULT_QUEUE_NAME = "push"; //keep this short as AWS limits queue name size to 80 chars

    /**
     * send notification to queue
     * @param notification
     * @param jobExecution
     * @throws Exception
     */
    void queueNotification(Notification notification, JobExecution jobExecution) throws Exception;

    /**
     * send notifications to providers
     * @param messages
     * @param queuePath
     * @return
     */
    Observable sendBatchToProviders(List<QueueMessage> messages, String queuePath);

    /**
     * stop processing and send message to providers to stop
     */
    void stop();

    /**
     * check for inactive devices, apple and google require this
     * @throws Exception
     */
    void asyncCheckForInactiveDevices() throws Exception;
}
