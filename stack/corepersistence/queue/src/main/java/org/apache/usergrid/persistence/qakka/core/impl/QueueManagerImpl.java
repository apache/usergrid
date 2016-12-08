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

package org.apache.usergrid.persistence.qakka.core.impl;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.core.Queue;
import org.apache.usergrid.persistence.qakka.core.QueueManager;
import org.apache.usergrid.persistence.qakka.core.Regions;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queues.DatabaseQueue;
import org.apache.usergrid.persistence.qakka.serialization.queues.QueueSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class QueueManagerImpl implements QueueManager {
    private static final Logger logger = LoggerFactory.getLogger( QueueManagerImpl.class );
    private final ActorSystemFig              actorSystemFig;
    private final QueueSerialization          queueSerialization;
    private final DistributedQueueService     distributedQueueService;
    private final ShardSerialization          shardSerialization;
    private final MessageCounterSerialization messageCounterSerialization;


    @Inject
    public QueueManagerImpl(
        ActorSystemFig              actorSystemFig,
        QueueSerialization          queueSerialization,
        DistributedQueueService     distributedQueueService,
        ShardSerialization          shardSerialization,
        MessageCounterSerialization messageCounterSerialization) {

        this.actorSystemFig              = actorSystemFig;
        this.queueSerialization          = queueSerialization;
        this.distributedQueueService     = distributedQueueService;
        this.shardSerialization          = shardSerialization;
        this.messageCounterSerialization = messageCounterSerialization;
    }

    @Override
    public void  createQueue(Queue queue) {

        logger.info("Creating queue with name: {}", queue.getName());

        List<String> regions = new ArrayList<>();
        if ( Regions.LOCAL.equals( queue.getRegions() ) || StringUtils.isEmpty( queue.getRegions() ) ) {
            regions.add( actorSystemFig.getRegionLocal() );

        } else if ( Regions.ALL.equals( queue.getRegions() )) {
            for ( String region : actorSystemFig.getRegionsList().split(",")) {
                regions.add( region );
            }

        } else {
            for (String region : queue.getRegions().split( "," )) {
                regions.add( region );
            }
        }

        Shard available = new Shard( queue.getName(), actorSystemFig.getRegionLocal(),
            Shard.Type.DEFAULT, 1L, QakkaUtils.getTimeUuid());
        shardSerialization.createShard( available );

        Shard inflight = new Shard( queue.getName(), actorSystemFig.getRegionLocal(),
            Shard.Type.INFLIGHT, 1L, QakkaUtils.getTimeUuid());
        shardSerialization.createShard( inflight );

        // only write the existence of a queue to the database if its dependent initial shards have been written
        queueSerialization.writeQueue(queue.toDatabaseQueue());

        // init counters
        messageCounterSerialization.incrementCounter( queue.getName(), DatabaseQueueMessage.Type.DEFAULT, 0L );
        messageCounterSerialization.incrementCounter( queue.getName(), DatabaseQueueMessage.Type.INFLIGHT, 0L );

        //distributedQueueService.initQueue( queue.getName() );
        distributedQueueService.refreshQueue( queue.getName() );
    }

    @Override
    public void updateQueueConfig(Queue queue) {

        queueSerialization.writeQueue(queue.toDatabaseQueue());

        //distributedQueueService.initQueue( queue.getName() );
        distributedQueueService.refreshQueue( queue.getName() );
    }

    @Override
    public void deleteQueue(String queueName) {

        queueSerialization.deleteQueue(queueName);

        // TODO: implement delete queue for Akka, stop schedulers, etc.
        //qas.deleteQueue(queueName);
    }

    @Override
    public Queue getQueueConfig(String queueName) {

        DatabaseQueue databaseQueue = queueSerialization.getQueue(queueName);
        if ( databaseQueue != null ) {
            return new Queue( databaseQueue );
        }
        return null;
    }

    @Override
    public List<String> getListOfQueues() {
        return queueSerialization.getListOfQueues();
    }
}
