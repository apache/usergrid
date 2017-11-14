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

package org.apache.usergrid.corepersistence.asyncevents;


import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.corepersistence.asyncevents.model.*;
import org.apache.usergrid.corepersistence.index.*;
import org.apache.usergrid.corepersistence.rx.impl.EdgeScope;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.corepersistence.util.ObjectJsonSerializer;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.apache.usergrid.persistence.index.impl.IndexingUtils;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.queue.*;
import org.apache.usergrid.persistence.queue.impl.LegacyQueueScopeImpl;
import org.apache.usergrid.persistence.queue.impl.SNSQueueManagerImpl;
import org.apache.usergrid.persistence.queue.settings.QueueIndexingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * TODO, this whole class is becoming a nightmare.
 * We need to remove all consume from this class and refactor it into the following manner.
 *
 * 1. Produce.  Keep the code in the handle as is
 * 2. Consume:  Move the code into a refactored system
 * 2.1 A central dispatcher
 * 2.2 An interface that produces an observable of type BatchOperation.  Any handler will be refactored into it's own
 *      impl that will then emit a stream of batch operations to perform
 * 2.3 The central dispatcher will then subscribe to these events and merge them.  Handing them off to a batch handler
 * 2.4 The batch handler will roll up the operations into a batch size, and then queue them
 * 2.5 The receive batch handler will execute the batch operations
 *
 * TODO determine how we error handle?
 *
 */
@Singleton
public class AsyncEventServiceImpl implements AsyncEventService {


    private static final Logger logger = LoggerFactory.getLogger(AsyncEventServiceImpl.class);

    // SQS maximum receive messages is 10
    public int MAX_TAKE = 10;
    public static final String QUEUE_NAME = "index"; //keep this short as AWS limits queue name size to 80 chars
    public static final String QUEUE_NAME_UTILITY = "utility"; //keep this short as AWS limits queue name size to 80 chars
    public static final String QUEUE_NAME_DELETE = "delete";


    private final LegacyQueueManager indexQueue;
    private final LegacyQueueManager utilityQueue;
    private final LegacyQueueManager deleteQueue;
    private final LegacyQueueManager indexQueueDead;
    private final LegacyQueueManager utilityQueueDead;
    private final LegacyQueueManager deleteQueueDead;
    private final IndexProcessorFig indexProcessorFig;
    private final LegacyQueueFig queueFig;
    private final IndexProducer indexProducer;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final EventBuilder eventBuilder;
    private final RxTaskScheduler rxTaskScheduler;

    private final Timer readTimer;
    private final Timer writeTimer;
    private final Timer ackTimer;

    /**
     * This mutex is used to start/stop workers to ensure we're not concurrently modifying our subscriptions
     */
    private final Object mutex = new Object();

    private final Counter indexErrorCounter;
    private final AtomicLong counter = new AtomicLong();
    private final AtomicLong counterUtility = new AtomicLong();
    private final AtomicLong counterDelete = new AtomicLong();
    private final AtomicLong counterIndexDead = new AtomicLong();
    private final AtomicLong counterUtilityDead = new AtomicLong();
    private final AtomicLong counterDeleteDead = new AtomicLong();
    private final AtomicLong inFlight = new AtomicLong();
    private final Histogram messageCycle;
    private final MapManager esMapPersistence;

    //the actively running subscription
    private List<Subscription> subscriptions = new ArrayList<>();


    @Inject
    public AsyncEventServiceImpl(final LegacyQueueManagerFactory queueManagerFactory,
                                 final IndexProcessorFig indexProcessorFig,
                                 final IndexProducer indexProducer,
                                 final MetricsFactory metricsFactory,
                                 final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                 final IndexLocationStrategyFactory indexLocationStrategyFactory,
                                 final EntityIndexFactory entityIndexFactory,
                                 final EventBuilder eventBuilder,
                                 final MapManagerFactory mapManagerFactory,
                                 final LegacyQueueFig queueFig,
                                 @EventExecutionScheduler
                                    final RxTaskScheduler rxTaskScheduler ) {
        this.indexProducer = indexProducer;

        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.eventBuilder = eventBuilder;

        final MapScope mapScope = new MapScopeImpl( CpNamingUtils.getManagementApplicationId(),  "indexEvents");

        this.esMapPersistence = mapManagerFactory.createMapManager( mapScope );

        this.rxTaskScheduler = rxTaskScheduler;

        LegacyQueueScope indexQueueScope =
            new LegacyQueueScopeImpl(QUEUE_NAME, LegacyQueueScope.RegionImplementation.ALL);

        LegacyQueueScope utilityQueueScope =
            new LegacyQueueScopeImpl(QUEUE_NAME_UTILITY, LegacyQueueScope.RegionImplementation.ALL);

        LegacyQueueScope deleteQueueScope =
            new LegacyQueueScopeImpl(QUEUE_NAME_DELETE, LegacyQueueScope.RegionImplementation.ALL);

        LegacyQueueScope indexQueueDeadScope =
            new LegacyQueueScopeImpl(QUEUE_NAME, LegacyQueueScope.RegionImplementation.ALL, true);

        LegacyQueueScope utilityQueueDeadScope =
            new LegacyQueueScopeImpl(QUEUE_NAME_UTILITY, LegacyQueueScope.RegionImplementation.ALL, true);

        LegacyQueueScope deleteQueueDeadScope =
            new LegacyQueueScopeImpl(QUEUE_NAME_DELETE, LegacyQueueScope.RegionImplementation.ALL, true);

        this.indexQueue = queueManagerFactory.getQueueManager(indexQueueScope);
        this.utilityQueue = queueManagerFactory.getQueueManager(utilityQueueScope);
        this.deleteQueue = queueManagerFactory.getQueueManager(deleteQueueScope);
        this.indexQueueDead = queueManagerFactory.getQueueManager(indexQueueDeadScope);
        this.utilityQueueDead = queueManagerFactory.getQueueManager(utilityQueueDeadScope);
        this.deleteQueueDead = queueManagerFactory.getQueueManager(deleteQueueDeadScope);

        this.indexProcessorFig = indexProcessorFig;
        this.queueFig = queueFig;

        this.writeTimer = metricsFactory.getTimer(AsyncEventServiceImpl.class, "async_event.write");
        this.readTimer = metricsFactory.getTimer(AsyncEventServiceImpl.class, "async_event.read");
        this.ackTimer = metricsFactory.getTimer(AsyncEventServiceImpl.class, "async_event.ack");
        this.indexErrorCounter = metricsFactory.getCounter(AsyncEventServiceImpl.class, "async_event.error");
        this.messageCycle = metricsFactory.getHistogram(AsyncEventServiceImpl.class, "async_event.message_cycle");


        //wire up the gauge of inflight message
        metricsFactory.addGauge(AsyncEventServiceImpl.class, "async-event.inflight", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return inFlight.longValue();
            }
        });

        start();
    }

    protected Histogram getMessageCycle() {
        return messageCycle;
    }

    private String getQueueName(AsyncEventQueueType queueType) {
        switch (queueType) {
            case REGULAR:
                return QUEUE_NAME;

            case UTILITY:
                return QUEUE_NAME_UTILITY;

            case DELETE:
                return QUEUE_NAME_DELETE;

            default:
                throw new IllegalArgumentException("Invalid queue type: " + queueType.toString());
        }
    }

    private LegacyQueueManager getQueue(AsyncEventQueueType queueType) {
        return getQueue(queueType, false);
    }

    private LegacyQueueManager getQueue(AsyncEventQueueType queueType, boolean isDeadQueue) {
        switch (queueType) {
            case REGULAR:
                return isDeadQueue ? indexQueueDead : indexQueue;

            case UTILITY:
                return isDeadQueue ? utilityQueueDead : utilityQueue;

            case DELETE:
                return isDeadQueue ? deleteQueueDead : deleteQueue;

            default:
                throw new IllegalArgumentException("Invalid queue type: " + queueType.toString());
        }
    }

    private AtomicLong getCounter(AsyncEventQueueType queueType, boolean isDeadQueue) {
        switch (queueType) {
            case REGULAR:
                return isDeadQueue ? counterIndexDead : counter;

            case UTILITY:
                return isDeadQueue ? counterUtilityDead : counterUtility;

            case DELETE:
                return isDeadQueue ? counterDeleteDead : counterDelete;

            default:
                throw new IllegalArgumentException("Invalid queue type: " + queueType.toString());
        }
    }





    /**
     * Offer the EntityIdScope to SQS
     */
    protected void offer(final Serializable operation) {
        offer(operation, AsyncEventQueueType.REGULAR, QueueIndexingStrategy.DIRECT);
    }

    /**
     * Offer the EntityIdScope to SQS
     */
    protected void offer(final Serializable operation, QueueIndexingStrategy queueIndexingStrategy) {
        offer(operation, AsyncEventQueueType.REGULAR, queueIndexingStrategy);
    }

     /**
      * Offer the EntityIdScope to SQS
      */
    private void offer(final Serializable operation, AsyncEventQueueType queueType, QueueIndexingStrategy queueIndexingStrategy) {
        final Timer.Context timer = this.writeTimer.time();

        try {
            //signal to SQS
            Boolean async = (queueIndexingStrategy == QueueIndexingStrategy.ASYNC);
            getQueue(queueType).sendMessageToLocalRegion(operation, async);

        } catch (IOException e) {
            throw new RuntimeException("Unable to queue message", e);
        } finally {
            timer.stop();
        }

    }


    private void offerTopic(final Serializable operation, AsyncEventQueueType queueType) {
        final Timer.Context timer = this.writeTimer.time();

        try {
            //signal to SQS
            getQueue(queueType).sendMessageToAllRegions(operation,null);

        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to queue message", e );
        }
        finally {
            timer.stop();
        }
    }


    private void offerBatch(final List operations, AsyncEventQueueType queueType){
        final Timer.Context timer = this.writeTimer.time();
        try {
            //signal to SQS
            getQueue(queueType).sendMessages(operations);
        } catch (IOException e) {
            throw new RuntimeException("Unable to queue message", e);
        } finally {
            timer.stop();
        }
    }


    /**
     * Take message
     */
    private List<LegacyQueueMessage> take(AsyncEventQueueType queueType, boolean isDeadQueue) {

        final Timer.Context timer = this.readTimer.time();

        try {
            return getQueue(queueType, isDeadQueue).getMessages(MAX_TAKE, AsyncEvent.class);
        }
        finally {
            //stop our timer
            timer.stop();
        }
    }

    private List<LegacyQueueMessage> take(AsyncEventQueueType queueType) {
        return take(queueType, false);
    }


    /**
     * Ack message
     */
    public void ack(final List<LegacyQueueMessage> messages) {

        final Timer.Context timer = this.ackTimer.time();

        try {

            for ( LegacyQueueMessage legacyQueueMessage : messages ) {
                try {
                    indexQueue.commitMessage( legacyQueueMessage );
                    inFlight.decrementAndGet();

                } catch ( Throwable t ) {
                    logger.error("Continuing after error acking message: " + legacyQueueMessage.getMessageId() );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException( "Unable to ack messages", e );

        } finally {
            timer.stop();
        }
    }

    public void ack(final List<LegacyQueueMessage> messages, AsyncEventQueueType queueType, boolean isDeadQueue) {
        if (queueType == AsyncEventQueueType.REGULAR && !isDeadQueue) {
            // different functionality
            ack(messages);
        }
        try {
            getQueue(queueType, isDeadQueue).commitMessages( messages );
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to ack messages", e);
        }
    }

    /**
     * calls the event handlers and returns a result with information on whether
     * it needs to be ack'd and whether it needs to be indexed
     * @param messages
     * @return
     */
    protected List<IndexEventResult> callEventHandlers(final List<LegacyQueueMessage> messages) {

        if (logger.isDebugEnabled()) {
            logger.debug("callEventHandlers with {} message(s)", messages.size());
        }

        Stream<IndexEventResult> indexEventResults = messages.stream().map(message ->

        {
            if(logger.isDebugEnabled()){
                logger.debug("Queue message with ID {} has been received {} time(s)",
                    message.getMessageId(),
                    message.getReceiveCount() );
            }

            AsyncEvent event = null;
            try {
                event = (AsyncEvent) message.getBody();

            } catch (ClassCastException cce) {
                logger.error("Failed to deserialize message body", cce);
                return new IndexEventResult(Optional.absent(), Optional.absent(), System.currentTimeMillis());
            }

            if (event == null) {
                logger.error("AsyncEvent type or event is null!");
                return new IndexEventResult(Optional.absent(), Optional.absent(), System.currentTimeMillis());
            }

            final AsyncEvent thisEvent = event;

            if (logger.isDebugEnabled()) {
                logger.debug("Processing event with type {}", event.getClass().getSimpleName());
            }

            try {

                IndexOperationMessage single = new IndexOperationMessage();

                // normal indexing event for an entity
                if ( event instanceof  EntityIndexEvent ){

                     single = handleEntityIndexUpdate( message );

                }
                // normal indexing event for an edge
                else if ( event instanceof EdgeIndexEvent ){

                    single = handleEdgeIndex( message );

                }
                // deletes are 2-part, actual IO to delete data, then queue up a de-index
                else if ( event instanceof EdgeDeleteEvent ) {

                    single = handleEdgeDelete( message );
                }
                // deletes are 2-part, actual IO to delete data, then queue up a de-index
                else if ( event instanceof EntityDeleteEvent ) {

                    single = handleEntityDelete( message );
                }
                // initialization has special logic, therefore a special event type and no index operation message
                else if ( event instanceof InitializeApplicationIndexEvent ) {

                    handleInitializeApplicationIndex(event, message);
                }
                // this is the main event that pulls the index doc from map persistence and hands to the index producer
                else if (event instanceof ElasticsearchIndexEvent) {

                    handleIndexOperation((ElasticsearchIndexEvent) event);

                } else if (event instanceof DeIndexOldVersionsEvent) {

                    single = handleDeIndexOldVersionEvent((DeIndexOldVersionsEvent) event);

                } else {

                    throw new Exception("Unknown EventType for message: "+ message.getStringBody().trim());
                }


                if( !(event instanceof ElasticsearchIndexEvent)
                    && !(event instanceof InitializeApplicationIndexEvent)
                      && single.isEmpty() ){
                        logger.warn("No index operation messages came back from event processing for eventType: {}, msgId: {}, msgBody: {}",
                            event.getClass().getSimpleName(), message.getMessageId(), message.getStringBody());
                }


                // if no exception happens and the QueueMessage is returned in these results, it will get ack'd
                return new IndexEventResult(Optional.of(single), Optional.of(message), thisEvent.getCreationTime());

            } catch (IndexDocNotFoundException e){

                // this exception is throw when we wait before trying quorum read on map persistence.
                // return empty event result so the event's message doesn't get ack'd
                if(logger.isDebugEnabled()){
                    logger.debug(e.getMessage());
                }
                return new IndexEventResult(Optional.absent(), Optional.absent(), thisEvent.getCreationTime());

            } catch (Exception e) {

                // NPEs don't have a detail message, so add something for our log statement to identify better
                final String errorMessage;
                if( e instanceof NullPointerException ) {
                    errorMessage = "NullPointerException";
                }else{
                    errorMessage = e.getMessage();
                }

                // if the event fails to process, log and return empty message result so it doesn't get ack'd
                logger.error("{}. Failed to process message: {}", errorMessage, message.getStringBody().trim() );
                return new IndexEventResult(Optional.absent(), Optional.absent(), thisEvent.getCreationTime());
            }
        });


        return indexEventResults.collect(Collectors.toList());
    }


    @Override
    public void queueInitializeApplicationIndex( final ApplicationScope applicationScope) {
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(
            applicationScope);


        if (logger.isTraceEnabled()) {
            logger.trace("Offering InitializeApplicationIndexEvent for {}:{}",
                applicationScope.getApplication().getUuid(), applicationScope.getApplication().getType());
        }

        offerTopic( new InitializeApplicationIndexEvent( queueFig.getPrimaryRegion(),
            new ReplicatedIndexLocationStrategy( indexLocationStrategy ) ), AsyncEventQueueType.REGULAR);
    }


    @Override
    public void queueEntityIndexUpdate(final ApplicationScope applicationScope,
                                       final Entity entity, long updatedAfter, QueueIndexingStrategy queueIndexingStrategy) {


        if (logger.isTraceEnabled()) {
            logger.trace("Offering EntityIndexEvent for {}:{}",
                entity.getId().getUuid(), entity.getId().getType());
        }


        EntityIndexEvent event = new EntityIndexEvent(queueFig.getPrimaryRegion(),
            new EntityIdScope(applicationScope, entity.getId()),
            updatedAfter);

        offer(event, queueIndexingStrategy);

    }

    private IndexOperationMessage handleEntityIndexUpdate(final LegacyQueueMessage message) {


        Preconditions.checkNotNull( message, "Queue Message cannot be null for handleEntityIndexUpdate" );

        final AsyncEvent event = ( AsyncEvent ) message.getBody();

        Preconditions.checkNotNull(message, "QueueMessage Body cannot be null for handleEntityIndexUpdate");
        Preconditions.checkArgument(event instanceof EntityIndexEvent,
            String.format("Event Type for handleEntityIndexUpdate must be ENTITY_INDEX, got %s", event.getClass()));

        final EntityIndexEvent entityIndexEvent = (EntityIndexEvent) event;


        //process the entity immediately
        //only process the same version, otherwise ignore
        final EntityIdScope entityIdScope = entityIndexEvent.getEntityIdScope();
        final ApplicationScope applicationScope = entityIdScope.getApplicationScope();
        final Id entityId = entityIdScope.getId();
        final long updatedAfter = entityIndexEvent.getUpdatedAfter();

        final EntityIndexOperation entityIndexOperation =
            new EntityIndexOperation( applicationScope, entityId, updatedAfter);

        // default this observable's return to empty index operation message if nothing is emitted
        return eventBuilder.buildEntityIndex( entityIndexOperation )
            .toBlocking().lastOrDefault(new IndexOperationMessage());
    }


    @Override
    public void queueNewEdge(final ApplicationScope applicationScope,
                             final Id entityId,
                             final Edge newEdge,
                             QueueIndexingStrategy queueIndexingStrategy) {

        if (logger.isTraceEnabled()) {
            logger.trace("Offering EdgeIndexEvent for edge type {} entity {}:{}",
                newEdge.getType(), entityId.getUuid(), entityId.getType());
        }

        offer( new EdgeIndexEvent( queueFig.getPrimaryRegion(), applicationScope, entityId, newEdge ), queueIndexingStrategy);

    }

    private IndexOperationMessage handleEdgeIndex(final LegacyQueueMessage message) {

        Preconditions.checkNotNull( message, "Queue Message cannot be null for handleEdgeIndex" );

        final AsyncEvent event = (AsyncEvent) message.getBody();

        Preconditions.checkNotNull( message, "QueueMessage Body cannot be null for handleEdgeIndex" );
        Preconditions.checkArgument(event instanceof EdgeIndexEvent,
            String.format("Event Type for handleEdgeIndex must be EDGE_INDEX, got %s", event.getClass()));

        final EdgeIndexEvent edgeIndexEvent = ( EdgeIndexEvent ) event;

        final EntityCollectionManager ecm =
            entityCollectionManagerFactory.createCollectionManager( edgeIndexEvent.getApplicationScope() );

        // default this observable's return to empty index operation message if nothing is emitted
        return ecm.load( edgeIndexEvent.getEntityId() )
            .flatMap( loadedEntity ->
                eventBuilder.buildNewEdge(edgeIndexEvent.getApplicationScope(), loadedEntity, edgeIndexEvent.getEdge()))
            .toBlocking().lastOrDefault(new IndexOperationMessage());

    }


    @Override
    public void queueDeleteEdge(final ApplicationScope applicationScope,
                                final Edge edge) {

        if (logger.isTraceEnabled()) {
            logger.trace("Offering EdgeDeleteEvent for type {} to target {}:{}",
                edge.getType(), edge.getTargetNode().getUuid(), edge.getTargetNode().getType());
        }

        // sent in region (not offerTopic) as the delete IO happens in-region, then queues a multi-region de-index op
        offer( new EdgeDeleteEvent( queueFig.getPrimaryRegion(), applicationScope, edge ), AsyncEventQueueType.DELETE , null);
    }

    private IndexOperationMessage  handleEdgeDelete(final LegacyQueueMessage message) {

        Preconditions.checkNotNull( message, "Queue Message cannot be null for handleEdgeDelete" );

        final AsyncEvent event = (AsyncEvent) message.getBody();

        Preconditions.checkNotNull( message, "QueueMessage Body cannot be null for handleEdgeDelete" );
        Preconditions.checkArgument(event instanceof EdgeDeleteEvent,
            String.format("Event Type for handleEdgeDelete must be EDGE_DELETE, got %s", event.getClass()));


        final EdgeDeleteEvent edgeDeleteEvent = ( EdgeDeleteEvent ) event;

        final ApplicationScope applicationScope = edgeDeleteEvent.getApplicationScope();
        final Edge edge = edgeDeleteEvent.getEdge();

        if (logger.isDebugEnabled()) {
            logger.debug("Deleting in app scope {} with edge {}", applicationScope, edge);
        }

        // default this observable's return to empty index operation message if nothing is emitted
        return eventBuilder.buildDeleteEdge(applicationScope, edge);

    }



    /**
     * Queue up an indexOperationMessage for multi region execution
     * @param indexOperationMessage
     * @param queueType
     */
    public void queueIndexOperationMessage(final IndexOperationMessage indexOperationMessage, AsyncEventQueueType queueType) {

        // don't try to produce something with nothing
        if(indexOperationMessage == null || indexOperationMessage.isEmpty()){
            return;
        }

        final String jsonValue = ObjectJsonSerializer.INSTANCE.toString( indexOperationMessage );

        final UUID newMessageId = UUIDGenerator.newTimeUUID();

        final int expirationTimeInSeconds =
            ( int ) TimeUnit.MILLISECONDS.toSeconds( indexProcessorFig.getIndexMessageTtl() );

        //write to the map in ES
        esMapPersistence.putString( newMessageId.toString(), jsonValue, expirationTimeInSeconds );

        //now queue up the index message

        final ElasticsearchIndexEvent elasticsearchIndexEvent =
            new ElasticsearchIndexEvent(queueFig.getPrimaryRegion(), newMessageId );

        //send to the topic so all regions index the batch

        if (logger.isTraceEnabled()) {
            logger.trace("Offering ElasticsearchIndexEvent for message {}", newMessageId);
        }

        offerTopic( elasticsearchIndexEvent, queueType );
    }

    protected ElasticsearchIndexEvent getESIndexEvent(final IndexOperationMessage indexOperationMessage) {

        final String jsonValue = ObjectJsonSerializer.INSTANCE.toString( indexOperationMessage );

        final UUID newMessageId = UUIDGenerator.newTimeUUID();

        final int expirationTimeInSeconds =
            ( int ) TimeUnit.MILLISECONDS.toSeconds( indexProcessorFig.getIndexMessageTtl() );

        //write to the map in ES
        esMapPersistence.putString( newMessageId.toString(), jsonValue, expirationTimeInSeconds );

        return new ElasticsearchIndexEvent(queueFig.getPrimaryRegion(), newMessageId );

    }


    protected void handleIndexOperation(final ElasticsearchIndexEvent elasticsearchIndexEvent)
        throws IndexDocNotFoundException {

        Preconditions.checkNotNull( elasticsearchIndexEvent, "elasticsearchIndexEvent cannot be null" );

        final UUID messageId = elasticsearchIndexEvent.getIndexBatchId();
        Preconditions.checkNotNull( messageId, "messageId must not be null" );


        final String message = esMapPersistence.getString( messageId.toString() );


        final IndexOperationMessage indexOperationMessage;
        if(message == null) {

            // provide some time back pressure before performing a quorum read
            if ( queueFig.getQuorumFallback() && System.currentTimeMillis() >
                elasticsearchIndexEvent.getCreationTime() + queueFig.getLocalQuorumTimeout() ) {

                if(logger.isDebugEnabled()){
                    logger.debug("ES batch with id {} not found, reading with strong consistency", messageId);
                }

                final String highConsistency = esMapPersistence.getStringHighConsistency(messageId.toString());
                if (highConsistency == null) {

                   throw new RuntimeException("ES batch with id " +
                       messageId+" not found when reading with strong consistency");
               }

               indexOperationMessage =
                   ObjectJsonSerializer.INSTANCE.fromString(highConsistency, IndexOperationMessage.class);

           } else if (System.currentTimeMillis() > elasticsearchIndexEvent.getCreationTime() + queueFig.getMapMessageTimeout()) {
                // if esMapPersistence message hasn't been received yet, log and return (will be acked)
                logger.error("ES map message never received, removing message from queue. indexBatchId={}", messageId);
                return;
           } else {
                logger.warn("ES map message not received yet. indexBatchId={} elapsedTimeMsec={}", messageId, System.currentTimeMillis() - elasticsearchIndexEvent.getCreationTime());
                throw new IndexDocNotFoundException(elasticsearchIndexEvent.getIndexBatchId());
           }

        } else {

            indexOperationMessage = ObjectJsonSerializer.INSTANCE.fromString( message, IndexOperationMessage.class );
        }


        // don't let this continue if there's nothing to index
        if (indexOperationMessage == null ||  indexOperationMessage.isEmpty()){
            throw new RuntimeException(
                "IndexOperationMessage cannot be null or empty after retrieving from map persistence");
        }


        // always do a check to ensure the indexes are initialized for the index requests
        initializeEntityIndexes(indexOperationMessage);

        // send it to to be indexed
        indexProducer.put(indexOperationMessage).toBlocking().last();

    }


    @Override
    public void queueDeIndexOldVersion(final ApplicationScope applicationScope, final Id entityId, UUID markedVersion) {

        // queue the de-index of old versions to the topic so cleanup happens in all regions

        if (logger.isTraceEnabled()) {
            logger.trace("Offering DeIndexOldVersionsEvent for app {} {}:{}",
                applicationScope.getApplication().getUuid(), entityId.getUuid(), entityId.getType());
        }

        offerTopic( new DeIndexOldVersionsEvent( queueFig.getPrimaryRegion(),
            new EntityIdScope( applicationScope, entityId), markedVersion), AsyncEventQueueType.DELETE );

    }


    public IndexOperationMessage handleDeIndexOldVersionEvent ( final DeIndexOldVersionsEvent deIndexOldVersionsEvent){


        final ApplicationScope applicationScope = deIndexOldVersionsEvent.getEntityIdScope().getApplicationScope();
        final Id entityId = deIndexOldVersionsEvent.getEntityIdScope().getId();
        final UUID markedVersion = deIndexOldVersionsEvent.getMarkedVersion();

        // default this observable's return to empty index operation message if nothing is emitted
        return eventBuilder.deIndexOldVersions( applicationScope, entityId, markedVersion )
            .toBlocking().lastOrDefault(new IndexOperationMessage());

    }

    /**
     *     this method will call initialize for each message, since we are caching the entity indexes,
     *     we don't worry about aggregating by app id
     * @param indexOperationMessage
     */
    private void initializeEntityIndexes(final IndexOperationMessage indexOperationMessage) {

        // create a set so we can have a unique list of appIds for which we call createEntityIndex
        Set<UUID> appIds = new HashSet<>();

        // loop through all indexRequests and add the appIds to the set
        indexOperationMessage.getIndexRequests().forEach(req -> {
            UUID appId = IndexingUtils.getApplicationIdFromIndexDocId(req.documentId);
            appIds.add(appId);
        });

        // loop through all deindexRequests and add the appIds to the set
        indexOperationMessage.getDeIndexRequests().forEach(req -> {
            UUID appId = IndexingUtils.getApplicationIdFromIndexDocId(req.documentId);
            appIds.add(appId);
        });

        // for each of the appIds in the unique set, call create entity index to ensure the aliases are created
        appIds.forEach(appId -> {
                ApplicationScope appScope = CpNamingUtils.getApplicationScope(appId);
                entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(appScope));
            }
        );
    }


    @Override
    public long getQueueDepth() {
        return indexQueue.getQueueDepth();
    }

    @Override
    public void queueEntityDelete(final ApplicationScope applicationScope, final Id entityId) {

        if (logger.isTraceEnabled()) {
            logger.trace("Offering EntityDeleteEvent for {}:{}", entityId.getUuid(), entityId.getType());
        }

        // sent in region (not offerTopic) as the delete IO happens in-region, then queues a multi-region de-index op
        offer( new EntityDeleteEvent(queueFig.getPrimaryRegion(), new EntityIdScope( applicationScope, entityId ) ),
            AsyncEventQueueType.DELETE , null);
    }

    private IndexOperationMessage handleEntityDelete(final LegacyQueueMessage message) {

        Preconditions.checkNotNull(message, "Queue Message cannot be null for handleEntityDelete");

        final AsyncEvent event = (AsyncEvent) message.getBody();
        Preconditions.checkNotNull( message, "QueueMessage Body cannot be null for handleEntityDelete" );
        Preconditions.checkArgument( event instanceof EntityDeleteEvent,
            String.format( "Event Type for handleEntityDelete must be ENTITY_DELETE, got %s", event.getClass() ) );


        final EntityDeleteEvent entityDeleteEvent = ( EntityDeleteEvent ) event;
        final ApplicationScope applicationScope = entityDeleteEvent.getEntityIdScope().getApplicationScope();
        final Id entityId = entityDeleteEvent.getEntityIdScope().getId();
        final boolean isCollectionDelete = entityDeleteEvent.isCollectionDelete();
        final long updatedBefore = entityDeleteEvent.getUpdatedBefore();

        if (logger.isDebugEnabled()) {
            logger.debug("Deleting entity id from index in app scope {} with entityId {}, isCollectionDelete {}, updatedBefore {}",
                applicationScope, entityId, isCollectionDelete, updatedBefore);
        }

        return eventBuilder.buildEntityDelete( applicationScope, entityId, isCollectionDelete, updatedBefore );

    }


    private void handleInitializeApplicationIndex(final AsyncEvent event, final LegacyQueueMessage message) {
        Preconditions.checkNotNull(message, "Queue Message cannot be null for handleInitializeApplicationIndex");
        Preconditions.checkArgument(event instanceof InitializeApplicationIndexEvent,
            String.format("Event Type for handleInitializeApplicationIndex must be APPLICATION_INDEX, got %s",
                event.getClass()));

        final InitializeApplicationIndexEvent initializeApplicationIndexEvent =
            ( InitializeApplicationIndexEvent ) event;

        final IndexLocationStrategy indexLocationStrategy = initializeApplicationIndexEvent.getIndexLocationStrategy();
        final EntityIndex index = entityIndexFactory.createEntityIndex( indexLocationStrategy );
        index.initialize();
    }

    /**
     * Loop through and start the workers
     */
    public void start() {
        final int indexCount = indexProcessorFig.getWorkerCount();
        final int utilityCount = indexProcessorFig.getWorkerCountUtility();
        final int deleteCount = indexProcessorFig.getWorkerCountDelete();
        final int indexDeadCount = indexProcessorFig.getWorkerCountDeadLetter();
        final int utilityDeadCount = indexProcessorFig.getWorkerCountUtilityDeadLetter();
        final int deleteDeadCount = indexProcessorFig.getWorkerCountDeleteDeadLetter();
        logger.info("Starting queue workers for indexing: index={} indexDLQ={} utility={} utilityDLQ={} delete={} deleteDLQ={}",
            indexCount, indexDeadCount, utilityCount, utilityDeadCount, deleteCount, deleteDeadCount);

        for (int i = 0; i < indexCount; i++) {
            startWorker(AsyncEventQueueType.REGULAR);
        }

        for (int i = 0; i < utilityCount; i++) {
            startWorker(AsyncEventQueueType.UTILITY);
        }

        for (int i = 0; i < deleteCount; i++) {
            startWorker(AsyncEventQueueType.DELETE);
        }

        if( indexQueue instanceof SNSQueueManagerImpl) {
            logger.info("Queue manager implementation supports dead letters, start dead letter queue workers.");
            for (int i = 0; i < indexDeadCount; i++) {
                startDeadQueueWorker(AsyncEventQueueType.REGULAR);
            }

            for (int i = 0; i < utilityDeadCount; i++) {
                startDeadQueueWorker(AsyncEventQueueType.UTILITY);
            }

            for (int i = 0; i < deleteDeadCount; i++) {
                startDeadQueueWorker(AsyncEventQueueType.DELETE);
            }
        }else{
            logger.info("Queue manager implementation does NOT support dead letters, NOT starting dead letter queue workers.");
        }
    }


    /**
     * Stop the workers
     */
    public void stop() {
        synchronized (mutex) {
            //stop consuming

            for (final Subscription subscription : subscriptions) {
                subscription.unsubscribe();
            }
        }
    }


    private void startWorker(final AsyncEventQueueType queueType) {
        synchronized (mutex) {

            String type = getQueueName(queueType);

            Observable<List<LegacyQueueMessage>> consumer =
                Observable.create( new Observable.OnSubscribe<List<LegacyQueueMessage>>() {
                    @Override
                    public void call( final Subscriber<? super List<LegacyQueueMessage>> subscriber ) {

                        //name our thread so it's easy to see
                        long threadNum = getCounter(queueType, false).incrementAndGet();
                        Thread.currentThread().setName( "QueueConsumer_" + type + "_" + threadNum );

                        List<LegacyQueueMessage> drainList = null;

                        do {
                            try {
                                drainList = take(queueType);

                                //emit our list in it's entity to hand off to a worker pool
                                subscriber.onNext(drainList);

                                //take since  we're in flight
                                inFlight.addAndGet( drainList.size() );

                            } catch ( Throwable t ) {

                                final long sleepTime = indexProcessorFig.getFailureRetryTime();

                                // there might be an error here during tests, just clean the cache
                                indexQueue.clearQueueNameCache();

                                if ( t instanceof InvalidQueryException ) {

                                    // don't fill up log with exceptions when keyspace and column
                                    // families are not ready during bootstrap/setup
                                    logger.warn( "Failed to dequeue due to '{}'. Sleeping for {} ms",
                                        t.getMessage(), sleepTime );

                                } else {
                                    logger.error( "Failed to dequeue. Sleeping for {} ms", sleepTime, t);
                                }

                                if ( drainList != null ) {
                                    inFlight.addAndGet( -1 * drainList.size() );
                                }

                                try { Thread.sleep( sleepTime ); } catch ( InterruptedException ie ) {}

                                indexErrorCounter.inc();
                            }
                        }
                        while ( true );
                    }
                } )        //this won't block our read loop, just reads and proceeds
                    .flatMap( sqsMessages -> {

                        //do this on a different schedule, and introduce concurrency
                        // with flatmap for faster processing
                        return Observable.just( sqsMessages )

                            .map( messages -> {
                                if ( messages == null || messages.size() == 0 ) {
                                    // no messages came from the queue, move on
                                    return null;
                                }

                                try {
                                    // process the messages
                                    List<IndexEventResult> indexEventResults =
                                        callEventHandlers( messages );

                                    // submit the processed messages to index producer
                                    List<LegacyQueueMessage> messagesToAck =
                                        submitToIndex( indexEventResults, queueType );

                                    if ( messagesToAck.size() < messages.size() ) {
                                        logger.warn(
                                            "Missing {} message(s) from index processing",
                                            messages.size() - messagesToAck.size() );
                                    }

                                    // ack each message if making it to this point
                                    if( messagesToAck.size() > 0 ){
                                        ack(messagesToAck, queueType, false);
                                    }

                                    return messagesToAck;
                                }
                                catch ( Exception e ) {
                                    logger.error( "Failed to ack messages", e );
                                    return null;
                                    //do not rethrow so we can process all of them
                                }
                            } ).subscribeOn( rxTaskScheduler.getAsyncIOScheduler() );

                        //end flatMap
                    }, indexProcessorFig.getEventConcurrencyFactor() );

            //start in the background

            final Subscription subscription = consumer.subscribeOn(Schedulers.newThread()).subscribe();

            subscriptions.add(subscription);
        }
    }


    private void startDeadQueueWorker(final AsyncEventQueueType queueType) {
        String type = getQueueName(queueType);
        synchronized (mutex) {

            Observable<List<LegacyQueueMessage>> consumer =
                Observable.create( new Observable.OnSubscribe<List<LegacyQueueMessage>>() {
                    @Override
                    public void call( final Subscriber<? super List<LegacyQueueMessage>> subscriber ) {

                        //name our thread so it's easy to see
                        long threadNum = getCounter(queueType, true).incrementAndGet();
                        Thread.currentThread().setName( "QueueDeadLetterConsumer_" + type + "_" + threadNum );

                        List<LegacyQueueMessage> drainList = null;

                        do {
                            try {
                                drainList = take(queueType, true);

                                //emit our list in it's entity to hand off to a worker pool
                                subscriber.onNext(drainList);

                                //take since  we're in flight
                                inFlight.addAndGet( drainList.size() );

                            } catch ( Throwable t ) {

                                final long sleepTime = indexProcessorFig.getDeadLetterFailureRetryTime();

                                // there might be an error here during tests, just clean the cache
                                indexQueueDead.clearQueueNameCache();

                                if ( t instanceof InvalidQueryException ) {

                                    // don't fill up log with exceptions when keyspace and column
                                    // families are not ready during bootstrap/setup
                                    logger.warn( "Failed to dequeue dead letters due to '{}'. Sleeping for {} ms",
                                        t.getMessage(), sleepTime );

                                } else {
                                    logger.error( "Failed to dequeue dead letters. Sleeping for {} ms", sleepTime, t);
                                }

                                if ( drainList != null ) {
                                    inFlight.addAndGet( -1 * drainList.size() );
                                }

                                try { Thread.sleep( sleepTime ); } catch ( InterruptedException ie ) {}
                            }
                        }
                        while ( true );
                    }
                } )        //this won't block our read loop, just reads and proceeds
                    .flatMap( sqsMessages -> {

                        //do this on a different schedule, and introduce concurrency
                        // with flatmap for faster processing
                        return Observable.just( sqsMessages )

                            .map( messages -> {
                                if ( messages == null || messages.size() == 0 ) {
                                    // no messages came from the queue, move on
                                    return null;
                                }

                                try {
                                    // put the dead letter messages back in the appropriate queue
                                    LegacyQueueManager returnQueue = getQueue(queueType, false);

                                    List<LegacyQueueMessage> successMessages = returnQueue.sendQueueMessages(messages);
                                    for (LegacyQueueMessage msg : successMessages) {
                                        logger.warn("Returning message to {} queue: type:{}, messageId:{} body: {}", queueType.toString(), msg.getType(), msg.getMessageId(), msg.getStringBody());
                                    }
                                    int unsuccessfulMessagesSize = messages.size() - successMessages.size();
                                    if (unsuccessfulMessagesSize > 0) {
                                        // some messages couldn't be sent to originating queue, log
                                        Set<String> successMessageIds = new HashSet<>();
                                        for (LegacyQueueMessage msg : successMessages) {
                                            String messageId = msg.getMessageId();
                                            if (successMessageIds.contains(messageId)) {
                                                logger.warn("Found duplicate messageId in returned messages: {}", messageId);
                                            } else {
                                                successMessageIds.add(messageId);
                                            }
                                        }
                                        for (LegacyQueueMessage msg : messages) {
                                            String messageId = msg.getMessageId();
                                            if (!successMessageIds.contains(messageId)) {
                                                logger.warn("Failed to return message to {} queue: type:{} messageId:{} body: {}", queueType.toString(), msg.getType(), messageId, msg.getStringBody());
                                            }
                                        }
                                    }

                                    ack(successMessages, queueType, true);

                                    return messages;
                                }
                                catch ( Exception e ) {
                                    logger.error( "Failed to ack messages", e );
                                    return null;
                                    //do not rethrow so we can process all of them
                                }
                            } ).subscribeOn( rxTaskScheduler.getAsyncIOScheduler() );

                        //end flatMap
                    }, indexProcessorFig.getEventConcurrencyFactor() );

            //start in the background

            final Subscription subscription = consumer.subscribeOn(Schedulers.newThread()).subscribe();

            subscriptions.add(subscription);
        }
    }

    /**
     * Submit results to index and return the queue messages to be ack'd
     *
     */
    private List<LegacyQueueMessage> submitToIndex(List<IndexEventResult> indexEventResults, AsyncEventQueueType queueType) {

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
                messageCycle.update(System.currentTimeMillis() - indexEventResult.getCreationTime());

                // ingest each index op into our combined, single index op for the index producer
                if(indexEventResult.getIndexOperationMessage().isPresent()){
                    combined.ingest(indexEventResult.getIndexOperationMessage().get());
                }

                return indexEventResult.getQueueMessage().get();
            })
            // collect into a list of QueueMessages that can be ack'd later
            .collect(Collectors.toList());

        queueIndexOperationMessage(combined, queueType);

        return queueMessages;
    }

    public void index(final ApplicationScope applicationScope, final Id id, final long updatedSince) {

        EntityIndexOperation entityIndexOperation =
            new EntityIndexOperation( applicationScope, id, updatedSince);

        queueIndexOperationMessage(
            eventBuilder.buildEntityIndex( entityIndexOperation ).toBlocking().lastOrDefault(null), AsyncEventQueueType.REGULAR );
    }

    public void indexBatch(final List<EdgeScope> edges, final long updatedSince, AsyncEventQueueType queueType) {

        final List<EntityIndexEvent> batch = new ArrayList<>();
        edges.forEach(e -> {

            //change to id scope to avoid serialization issues
            batch.add(new EntityIndexEvent(queueFig.getPrimaryRegion(),
                new EntityIdScope(e.getApplicationScope(), e.getEdge().getTargetNode()), updatedSince));

        });

        if (logger.isTraceEnabled()) {
            logger.trace("Offering batch of EntityIndexEvent of size {}", batch.size());
        }

        offerBatch( batch, queueType );
    }

    public void deleteBatch(final List<EdgeScope> edges, final long updatedBefore, AsyncEventQueueType queueType) {

        final List<EntityDeleteEvent> batch = new ArrayList<>();
        edges.forEach(e -> {

            //change to id scope to avoid serialization issues
            batch.add(new EntityDeleteEvent(queueFig.getPrimaryRegion(),
                new EntityIdScope(e.getApplicationScope(), e.getEdge().getTargetNode()), true, updatedBefore));

        });

        offerBatch(batch, queueType);
    }


    public class IndexEventResult{
        private final Optional<IndexOperationMessage> indexOperationMessage;
        private final Optional<LegacyQueueMessage> queueMessage;
        private final long creationTime;

        public IndexEventResult(Optional<IndexOperationMessage> indexOperationMessage,
                                Optional<LegacyQueueMessage> queueMessage, long creationTime){

            this.queueMessage = queueMessage;
            this.creationTime = creationTime;
            this.indexOperationMessage = indexOperationMessage;
        }

        public Optional<IndexOperationMessage> getIndexOperationMessage() {
            return indexOperationMessage;
        }

        public Optional<LegacyQueueMessage> getQueueMessage() {
            return queueMessage;
        }

        public long getCreationTime() {
            return creationTime;
        }
    }

    public String getQueueManagerClass() {

        return indexQueue.getClass().getSimpleName();

    }


}
