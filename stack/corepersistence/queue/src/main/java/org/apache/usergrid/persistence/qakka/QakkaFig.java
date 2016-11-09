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

    String QUEUE_STANDALONE                       = "queue.standalone";

    String QUEUE_NUM_ACTORS                       = "queue.num.actors";

    String QUEUE_SENDER_NUM_ACTORS                = "queue.sender.num.actors";

    String QUEUE_WRITER_NUM_ACTORS                = "queue.writer.num.actors";

    String QUEUE_TIMEOUT_SECONDS                  = "queue.timeout.seconds";

    String QUEUE_REFRESH_MILLISECONDS             = "queue.inmemory.refresh.millis";

    String QUEUE_IN_MEMORY                        = "queue.inmemory.cache";

    String QUEUE_INMEMORY_SIZE                    = "queue.inmemory.cache.size";

    String QUEUE_IN_MEMORY_REFRESH_ASYNC          = "queue.inmemory.cache.async";

    String QUEUE_SEND_MAX_RETRIES                 = "queue.send.max.retries";

    String QUEUE_SEND_TIMEOUT                     = "queue.send.timeout.seconds";

    String QUEUE_GET_MAX_RETRIES                  = "queue.get.max.retries";

    String QUEUE_GET_TIMEOUT                      = "queue.get.timeout.seconds";

    String QUEUE_SHARD_COUNTER_MAX_IN_MEMORY      = "queue.shard.counter.max-in-memory";

    String QUEUE_SHARD_COUNTER_WRITE_TIMEOUT      = "queue.shard.counter.write-timeout";

    String QUEUE_MESSAGE_COUNTER_MAX_IN_MEMORY    = "queue.message.counter.max-in-memory";

    String QUEUE_MESSAGE_COUNTER_WRITE_TIMEOUT    = "queue.message.counter.write-timeout";

    String QUEUE_SHARD_ALLOCATION_CHECK_FREQUENCY = "queue.shard.allocation.check.frequency.millis";

    String QUEUE_SHARD_ALLOCATION_ADVANCE_TIME    = "queue.shard.allocation.advance.time.millis";

    String QUEUE_SHARD_MAX_SIZE                   = "queue.shard.max.size";

    String QUEUE_LONG_POLL_TIME_MILLIS            = "queue.long.polling.time.millis";

    String QUEUE_MAX_TTL                          = "queue.max.ttl";



    /** True if Qakka is running standlone */
    @Key(QUEUE_STANDALONE)
    @Default("false")
    boolean getStandalone();

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
    @Default("30")
    int getQueueTimeoutSeconds();

    /** How often to refresh each queue's in-memory data */
    @Key(QUEUE_REFRESH_MILLISECONDS)
    @Default("1000")
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
    @Default("3")
    int getGetTimeoutSeconds();

    /** Max number of times to retry call to queue writer for queue send operation */
    @Key(QUEUE_SEND_MAX_RETRIES)
    @Default("5")
    int getMaxSendRetries();

    /** How long to wait for response from queue writer before timing out and trying again */
    @Key(QUEUE_SEND_TIMEOUT)
    @Default("5")
    int getSendTimeoutSeconds();

    /** Once counter reaches this value, write it to permanent storage */
    @Key(QUEUE_SHARD_COUNTER_MAX_IN_MEMORY)
    @Default("100")
    long getShardCounterMaxInMemory();

    @Key(QUEUE_SHARD_COUNTER_WRITE_TIMEOUT)
    @Default("5000")
    long getShardCounterWriteTimeoutMillis();

    /** Once counter reaches this value, write it to permanent storage */
    @Key(QUEUE_MESSAGE_COUNTER_MAX_IN_MEMORY)
    @Default("100")
    long getMessageCounterMaxInMemory();

    @Key(QUEUE_MESSAGE_COUNTER_WRITE_TIMEOUT)
    @Default("5000")
    long getMessageCounterWriteTimeoutMillis();

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

    @Key(QUEUE_LONG_POLL_TIME_MILLIS)
    @Default("5000")
    long getLongPollTimeMillis();

    /** Max time-to-live for queue message and payload data */
    @Key(QUEUE_MAX_TTL)
    @Default("1209600") // default is two weeks
    int getMaxTtlSeconds();

    @Key(QUEUE_IN_MEMORY)
    @Default("false")
    boolean getInMemoryCache();

    @Key(QUEUE_IN_MEMORY_REFRESH_ASYNC)
    @Default("true")
    boolean getInMemoryRefreshAsync();
}
