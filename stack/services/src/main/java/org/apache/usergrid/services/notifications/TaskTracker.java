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
package org.apache.usergrid.services.notifications;


import org.apache.usergrid.persistence.Notifier;
import org.apache.usergrid.persistence.Receipt;

import java.util.UUID;

public class TaskTracker {

    private Notifier notifier;
    private TaskManager taskManager;
    private Receipt receipt;
    private UUID id;

    public TaskTracker(Notifier notifier, TaskManager taskManager, Receipt receipt, UUID id) {
        this.notifier = notifier;
        this.taskManager = taskManager;
        this.receipt = receipt;
        this.id = id;
    }

    public void completed() throws Exception {
        taskManager.completed(notifier, receipt, id, null);
    }

    public void failed(Object code, String message) throws Exception {
        taskManager.failed(notifier, receipt, id, code, message);
    }

    public void completed(String newToken) throws Exception {
        taskManager.completed(notifier, receipt, id, newToken);
    }
}
