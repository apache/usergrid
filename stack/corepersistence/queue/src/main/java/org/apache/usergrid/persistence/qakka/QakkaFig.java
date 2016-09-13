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

package org.apache.usergrid.persistence.qakka;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;

import java.io.Serializable;


@FigSingleton
public interface QakkaFig extends GuicyFig, Serializable {

    String QUEUE_NUM_ACTORS                       = "queue.num.actors";

    String QUEUE_SENDER_NUM_ACTORS                = "queue.sender.num.actors";

    String QUEUE_WRITER_NUM_ACTORS                = "queue.writer.num.actors";

    String QUEUE_TIMEOUT_SECONDS                  = "queue.timeout.seconds";

    String QUEUE_REFRESH_MILLISECONDS             = "queue.refresh.milliseconds";

    String QUEUE_INMEMORY_SIZE                    = "queue.inmemory.size";

    String QUEUE_SEND_MAX_RETRIES                 = "queue.send.max.retries";

    String QUEUE_SEND_TIMEOUT                     = "queue.send.timeout.seconds";

    String QUEUE_GET_MAX_RETRIES                  = "queue.get.max.retries";

    String QUEUE_GET_TIMEOUT                      = "queue.get.timeout.seconds";

    String QUEUE_MAX_SHARD_COUNTER                = "queue.max.inmemory.shard.counter";

    String QUEUE_SHARD_ALLOCATION_CHECK_FREQUENCY = "queue.shard.allocation.check.frequency.millis";

    String QUEUE_SHARD_ALLOCATION_ADVANCE_TIME    = "queue.shard.allocation.advance.time.millis";

    String QUEUE_SHARD_MAX_SIZE                   = "queue.shard.max.size";


    /** Queue senders send to queue writers */
    @Key(QUEUE_SENDER_NUM_ACTORS)
    @Default("200")
    int getNumQueueSenderActors();

    /** Queue writers write to Cassandra */
    @Key(QUEUE_WRITER_NUM_ACTORS)
    @Default("500")
    int getNumQueueWriterActors();

    /** Queue actors handle get, ack and manage scheduled timeout and refersh tasks */
    @Key(QUEUE_NUM_ACTORS)
    @Default("500")
    int getNumQueueActors();

    /** Time for queue messages to timeout, if not set per queue */
    @Key(QUEUE_TIMEOUT_SECONDS)
    @Default("10")
    int getQueueTimeoutSeconds();

    /** How often to refresh each queue's in-memory data */
    @Key(QUEUE_REFRESH_MILLISECONDS)
    @Default("500")
    int getQueueRefreshMilliseconds();

    /** How many queue messages to keep in-memory */
    @Key(QUEUE_INMEMORY_SIZE)
    @Default("1000")
    int getQueueInMemorySize();

    /** Max number of times to retry call to queue actor for queue get operation */
    @Key(QUEUE_GET_MAX_RETRIES)
    @Default("5")
    int getMaxGetRetries();

    /** How long to wait for response from queue actor before timing out and trying again */
    @Key(QUEUE_GET_TIMEOUT)
    @Default("2")
    int getGetTimeoutSeconds();

    /** Max number of times to retry call to queue writer for queue send operation */
    @Key(QUEUE_SEND_MAX_RETRIES)
    @Default("5")
    int getMaxSendRetries();

    /** How long to wait for response from queue writer before timing out and trying again */
    @Key(QUEUE_SEND_TIMEOUT)
    @Default("2")
    int getSendTimeoutSeconds();

    /** Once counter reaches this value, write it to permanent storage */
    @Key(QUEUE_MAX_SHARD_COUNTER)
    @Default("100")
    long getMaxInMemoryShardCounter();

    /** How often to check whether new shard is needed for each queue */
    @Key(QUEUE_SHARD_ALLOCATION_CHECK_FREQUENCY)
    @Default("5000")
    long getShardAllocationCheckFrequencyMillis();

    /** New shards are created in advance of the time they will be used */
    @Key(QUEUE_SHARD_ALLOCATION_ADVANCE_TIME)
    @Default("5000")
    long getShardAllocationAdvanceTimeMillis();

    /** Max size to allow for a shard */
    @Key(QUEUE_SHARD_MAX_SIZE)
    @Default("400000")
    long getMaxShardSize();
}
