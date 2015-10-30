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
package org.apache.usergrid.persistence.queue.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.queue.*;

import java.util.HashMap;
import java.util.Map;

/**
 * manages whether we take in an external in memory override for queues.
 */
@Singleton
public class QueueManagerFactoryImpl implements QueueManagerFactory {


    private final QueueFig queueFig;
    private final QueueManagerInternalFactory queuemanagerInternalFactory;
    private final Map<String,QueueManager> defaultManager;

    @Inject
    public QueueManagerFactoryImpl(final QueueFig queueFig, final QueueManagerInternalFactory queuemanagerInternalFactory){
        this.queueFig = queueFig;
        this.queuemanagerInternalFactory = queuemanagerInternalFactory;
        this.defaultManager = new HashMap<>(10);
    }
    @Override
    public QueueManager getQueueManager(QueueScope scope) {
        if(queueFig.overrideQueueForDefault()){
            QueueManager manager = defaultManager.get(scope.getName());
            if(manager==null){
                manager = new LocalQueueManager();
                defaultManager.put(scope.getName(),manager);
            }
            return manager;
        }else{
            return queuemanagerInternalFactory.getQueueManager(scope);
        }
    }
}
