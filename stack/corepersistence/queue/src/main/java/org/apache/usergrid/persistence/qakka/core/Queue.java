/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.qakka.core;

import org.apache.usergrid.persistence.qakka.serialization.queues.DatabaseQueue;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Queue {

    // will eventually control these via properties file
    private static final Integer defaultRetryCount = 3;
    private static final Integer defaultHandlingTimeoutSec = 30;
    private static final String defaultDeadLetterQueueExtension = "_DLQ";

    private String name;
    private String queueType;
    private String regions;
    private String defaultDestinations;
    private Long defaultDelayMs;
    private Integer retryCount;
    private Integer handlingTimeoutSec;
    private String deadLetterQueue;

    public Queue() {} // Jackson needs no-arg ctor

    public Queue(String name, String queueType, String regions, String defaultDestinations, Long defaultDelayMs,
                 Integer retryCount, Integer handlingTimeoutSec, String deadLetterQueue) {
        this.name = name;
        this.queueType = queueType;
        this.regions = regions;
        this.defaultDestinations = defaultDestinations;
        this.defaultDelayMs = defaultDelayMs;
        this.retryCount = retryCount;
        this.handlingTimeoutSec = handlingTimeoutSec;
        this.deadLetterQueue = deadLetterQueue;
    }

    public Queue(String name, String queueType, String regions, String defaultDestinations, Long defaultDelayMs) {
        this(name, queueType, regions, defaultDestinations, defaultDelayMs, defaultRetryCount,
                defaultHandlingTimeoutSec, name + defaultDeadLetterQueueExtension);
    }

    public Queue(String name, String queueType, String regions, String defaultDestinations) {
        this(name, queueType, regions, defaultDestinations, 0L, defaultRetryCount,
                defaultHandlingTimeoutSec, name + defaultDeadLetterQueueExtension);
    }

    public Queue(String name) {
        this(name, QueueType.MULTIREGION, Regions.LOCAL, Regions.LOCAL, 0L, defaultRetryCount,
                defaultHandlingTimeoutSec, name + defaultDeadLetterQueueExtension);
    }

    public Queue(DatabaseQueue databaseQueue) {
        this(   databaseQueue.getName(),
                QueueType.MULTIREGION,
                databaseQueue.getRegions(),
                databaseQueue.getDefaultDestinations(),
                databaseQueue.getDefaultDelayMs(),
                databaseQueue.getRetryCount(),
                databaseQueue.getHandlingTimeoutSec(),
                databaseQueue.getDeadLetterQueue());
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getQueueType() {
        return queueType;
    }

    public String getRegions() {
        return regions;
    }
    public void setRegions(String regions) {
        this.regions = regions;
    }

    public String getDefaultDestinations() {
        return defaultDestinations;
    }
    public void setDefaultDestinations(String defaultDestinations) {
        this.defaultDestinations = defaultDestinations;
    }

    public Long getDefaultDelayMs() {
        return defaultDelayMs;
    }
    public void setDefaultDelayMs(Long defaultDelayMs) {
        this.defaultDelayMs = defaultDelayMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getHandlingTimeoutSec() {
        return handlingTimeoutSec;
    }
    public void setHandlingTimeoutSec(Integer handlingTimeoutSec) {
        this.handlingTimeoutSec = handlingTimeoutSec;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }
    public void setDeadLetterQueue(String deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    public DatabaseQueue toDatabaseQueue() {
        return new DatabaseQueue(
                name,
                regions,
                defaultDestinations,
                defaultDelayMs,
                retryCount,
                handlingTimeoutSec,
                deadLetterQueue);
    }
}

