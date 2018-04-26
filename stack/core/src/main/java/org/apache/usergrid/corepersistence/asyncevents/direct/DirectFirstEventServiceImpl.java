/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.corepersistence.asyncevents.direct;

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventServiceImpl;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.asyncevents.model.ElasticsearchIndexEvent;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.index.IndexProcessorFig;
import org.apache.usergrid.corepersistence.util.CpCollectionUtils;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.queue.LegacyQueueFig;
import org.apache.usergrid.persistence.queue.LegacyQueueManagerFactory;
import org.apache.usergrid.persistence.queue.LegacyQueueMessage;
import org.apache.usergrid.persistence.queue.settings.QueueIndexingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the AsyncEventService that writes first directly to ES
 * and then submits to ASW as a backup.
 *
 * Created by peterajohnson on 8/29/17.
 */
public class DirectFirstEventServiceImpl extends AsyncEventServiceImpl {


    private static final Logger logger = LoggerFactory.getLogger(DirectFirstEventServiceImpl.class);

    private QueueIndexingStrategy configQueueIndexingStrategy = QueueIndexingStrategy.ASYNC;

    private BufferedQueue<Serializable> bufferedBatchQueue = new BufferedQueueNOP<>();

    public DirectFirstEventServiceImpl(LegacyQueueManagerFactory queueManagerFactory, IndexProcessorFig indexProcessorFig, IndexProducer indexProducer, MetricsFactory metricsFactory, EntityCollectionManagerFactory entityCollectionManagerFactory, IndexLocationStrategyFactory indexLocationStrategyFactory, EntityIndexFactory entityIndexFactory, EventBuilder eventBuilder, MapManagerFactory mapManagerFactory, LegacyQueueFig queueFig, RxTaskScheduler rxTaskScheduler) {
        super(queueManagerFactory, indexProcessorFig, indexProducer, metricsFactory, entityCollectionManagerFactory, indexLocationStrategyFactory, entityIndexFactory, eventBuilder, mapManagerFactory, queueFig, rxTaskScheduler);

        bufferedBatchQueue.setConsumer((c) -> { dispatchToES(c); });

        configQueueIndexingStrategy = QueueIndexingStrategy.get(queueFig.getQueueStrategy());

        boolean indexDebugMode = Boolean.valueOf(queueFig.getQueueDebugMode());
        CpCollectionUtils.setDebugMode(indexDebugMode);

    }

    protected void dispatchToES(final List<Serializable> bodies) {

        List<LegacyQueueMessage> messages = new ArrayList<>();
        for (Serializable body : bodies) {
            String uuid = UUID.randomUUID().toString();
            LegacyQueueMessage message = new LegacyQueueMessage(uuid, "handle_" + uuid, body, "put type here");
            messages.add(message);
        }

        List<IndexEventResult> result = callEventHandlers(messages);

        // failed to dispatch send to SQS
        try {
            List<LegacyQueueMessage> indexedMessages = submitToIndex(result, false);
            if (logger.isDebugEnabled()) {
                logger.debug("Sent {} messages to ES ", indexedMessages.size());
            }
        } catch (Exception e) {
            for (Serializable body : bodies) {
                super.offer(body);
            }
        }


    }

    /**
     * Offer the EntityIdScope to SQS
     *
     * The body will be an implementation of one of the following:
     *    EntityIndexEvent
     *    EntityDeleteEvent
     *    EdgeIndexEvent
     *    EdgeDeleteEvent
     */
    protected void offer(final Serializable body) {
        List<LegacyQueueMessage> messages = getMessageArray(body);
        List<IndexEventResult> result = callEventHandlers(messages);
        submitToIndex( result, false );
        super.offer(body);
    }

    private List<LegacyQueueMessage> getMessageArray(final Serializable body) {
        String uuid = UUID.randomUUID().toString();

        LegacyQueueMessage message = new LegacyQueueMessage(uuid, "handle_" + uuid, body, "put type here");

        if (logger.isDebugEnabled()) {
            logger.debug("Sync Handler called for body of class {} ", body.getClass().getSimpleName());
        }

        List<LegacyQueueMessage> messages = new ArrayList<>();
        messages.add(message);
        return messages;
    }


    protected void offer(final Serializable operation, QueueIndexingStrategy queueIndexingStrategy) {
        queueIndexingStrategy = resolveIndexingStrategy(queueIndexingStrategy);
        if  (queueIndexingStrategy.shouldSendDirectToES()) {
            List<LegacyQueueMessage> messages = getMessageArray(operation);
            List<IndexEventResult> result = callEventHandlers(messages);
            submitToIndex( result, false );
        }

        if (queueIndexingStrategy.shouldSendToAWS()) {
            super.offer(operation, queueIndexingStrategy);
        }
    }


    protected List<LegacyQueueMessage> submitToIndex(List<IndexEventResult> indexEventResults, boolean forUtilityQueue) {

        // if nothing came back then return empty list
        if(indexEventResults==null){
            return new ArrayList<>(0);
        }

        IndexOperationMessage combined = new IndexOperationMessage();
        List<LegacyQueueMessage> queueMessages = indexEventResults.stream()

            // filter out messages that are not present, they were not processed and put into the results
            .filter( result -> result.getQueueMessage().isPresent() )
            .map(indexEventResult -> {

                //record the cycle time
                getMessageCycle().update(System.currentTimeMillis() - indexEventResult.getCreationTime());

                // ingest each index op into our combined, single index op for the index producer
                if(indexEventResult.getIndexOperationMessage().isPresent()){
                    combined.ingest(indexEventResult.getIndexOperationMessage().get());
                }

                return indexEventResult.getQueueMessage().get();
            })
            // collect into a list of QueueMessages that can be ack'd later
            .collect(Collectors.toList());


        // dispatch to ES
        ElasticsearchIndexEvent elasticsearchIndexEvent = getESIndexEvent(combined);
        handleIndexOperation(elasticsearchIndexEvent);
        return queueMessages;
    }

    // If the collection has not defined an indexing strategy then use the default from the fig.
    // only allow NOINDEX or DIRECTONLY when in debug mode
    private QueueIndexingStrategy resolveIndexingStrategy(QueueIndexingStrategy queueIndexingStrategy) {
        switch (queueIndexingStrategy) {
            case CONFIG:
                return configQueueIndexingStrategy;
            case NOINDEX:
            case DIRECTONLY:
                if (!CpCollectionUtils.getDebugMode()) {
                    return configQueueIndexingStrategy;
                }
            default:
                return queueIndexingStrategy;
        }
    }
}
