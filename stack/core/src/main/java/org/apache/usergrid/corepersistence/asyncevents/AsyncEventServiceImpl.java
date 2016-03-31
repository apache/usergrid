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


import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.usergrid.corepersistence.asyncevents.model.*;
import org.apache.usergrid.persistence.index.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.index.EntityIndexOperation;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.index.IndexProcessorFig;
import org.apache.usergrid.corepersistence.index.ReplicatedIndexLocationStrategy;
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
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.queue.QueueFig;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.apache.usergrid.persistence.queue.QueueMessage;
import org.apache.usergrid.persistence.queue.QueueScope;
import org.apache.usergrid.persistence.queue.impl.QueueScopeImpl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;


/**
 * TODO, this whole class is becoming a nightmare.  We need to remove all consume from this class and refactor it into the following manner.
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

    private final QueueManager queue;
    private final IndexProcessorFig indexProcessorFig;
    private final QueueFig queueFig;
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
    private final AtomicLong inFlight = new AtomicLong();
    private final Histogram messageCycle;
    private final MapManager esMapPersistence;

    //the actively running subscription
    private List<Subscription> subscriptions = new ArrayList<>();


    @Inject
    public AsyncEventServiceImpl(final QueueManagerFactory queueManagerFactory,
                                 final IndexProcessorFig indexProcessorFig,
                                 final IndexProducer indexProducer,
                                 final MetricsFactory metricsFactory,
                                 final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                 final IndexLocationStrategyFactory indexLocationStrategyFactory,
                                 final EntityIndexFactory entityIndexFactory,
                                 final EventBuilder eventBuilder,
                                 final MapManagerFactory mapManagerFactory,
                                 final QueueFig queueFig,
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

        QueueScope queueScope = new QueueScopeImpl(QUEUE_NAME, QueueScope.RegionImplementation.ALL);
        this.queue = queueManagerFactory.getQueueManager(queueScope);

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


    /**
     * Offer the EntityIdScope to SQS
     */
    private void offer(final Serializable operation) {
        final Timer.Context timer = this.writeTimer.time();

        try {
            //signal to SQS
            this.queue.sendMessage( operation );
        } catch (IOException e) {
            throw new RuntimeException("Unable to queue message", e);
        } finally {
            timer.stop();
        }
    }


    private void offerTopic( final Serializable operation ) {
        final Timer.Context timer = this.writeTimer.time();

        try {
            //signal to SQS
            this.queue.sendMessageToTopic( operation );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to queue message", e );
        }
        finally {
            timer.stop();
        }
    }

    private void offerBatch(final List operations){
        final Timer.Context timer = this.writeTimer.time();

        try {
            //signal to SQS
            this.queue.sendMessages(operations);
        } catch (IOException e) {
            throw new RuntimeException("Unable to queue message", e);
        } finally {
            timer.stop();
        }
    }


    /**
     * Take message from SQS
     */
    private List<QueueMessage> take() {

        final Timer.Context timer = this.readTimer.time();

        try {
            return queue.getMessages(MAX_TAKE, AsyncEvent.class);
        }
        finally {
            //stop our timer
            timer.stop();
        }
    }



    /**
     * Ack message in SQS
     */
    public void ack(final List<QueueMessage> messages) {

        final Timer.Context timer = this.ackTimer.time();

        try{
            queue.commitMessages( messages );

            //decrement our in-flight counter
            inFlight.decrementAndGet();

        }catch(Exception e){
            throw new RuntimeException("Unable to ack messages", e);
        }finally {
            timer.stop();
        }


    }

    /**
     * calls the event handlers and returns a result with information on whether it needs to be ack'd and whether it needs to be indexed
     * @param messages
     * @return
     */
    private List<IndexEventResult> callEventHandlers(final List<QueueMessage> messages) {

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

            IndexOperationMessage indexOperationMessage = null;
            try {

                // deletes are 2-part, actual IO to delete data, then queue up a de-index
                if ( event instanceof EdgeDeleteEvent ) {

                    handleEdgeDelete( message );
                }
                // deletes are 2-part, actual IO to delete data, then queue up a de-index
                else if ( event instanceof EntityDeleteEvent ) {

                    handleEntityDelete( message );
                }
                // application initialization has special logic, therefore a special event type
                else if ( event instanceof InitializeApplicationIndexEvent ) {

                    handleInitializeApplicationIndex(event, message);
                }
                // this is the main event that pulls the index doc from map persistence and hands to the index producer
                else if (event instanceof ElasticsearchIndexEvent) {

                    indexOperationMessage = handleIndexOperation((ElasticsearchIndexEvent) event);

                } else if (event instanceof DeIndexOldVersionsEvent) {

                    indexOperationMessage = handleDeIndexOldVersionEvent((DeIndexOldVersionsEvent) event);

                } else {

                    throw new Exception("Unknown EventType for message: "+ message.getStringBody().trim());
                }


                // returning indexOperationMessage will send that indexOperationMessage to the index producer
                // if no exception happens and the QueueMessage is returned in these results, it will get ack'd
                return new IndexEventResult(Optional.fromNullable(indexOperationMessage), Optional.of(message), thisEvent.getCreationTime());

            } catch (IndexDocNotFoundException e){

                // this exception is throw when we wait before trying quorum read on map persistence.
                // return empty event result so the event's message doesn't get ack'd
                if(logger.isDebugEnabled()){
                    logger.debug(e.getMessage());
                }
                return new IndexEventResult(Optional.absent(), Optional.absent(), thisEvent.getCreationTime());

            } catch (Exception e) {

                // if the event fails to process, log and return empty message result so it doesn't get ack'd
                logger.error("{}. Failed to process message: {}", e.getMessage(), message.getStringBody().trim() );
                return new IndexEventResult(Optional.absent(), Optional.absent(), thisEvent.getCreationTime());
            }
        });


        return indexEventResults.collect(Collectors.toList());
    }

    @Override
    public void queueInitializeApplicationIndex( final ApplicationScope applicationScope) {
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(
            applicationScope);
        offerTopic( new InitializeApplicationIndexEvent( queueFig.getPrimaryRegion(),
            new ReplicatedIndexLocationStrategy( indexLocationStrategy ) ) );
    }


    @Override
    public void queueEntityIndexUpdate(final ApplicationScope applicationScope,
                                       final Entity entity, long updatedAfter) {


        final EntityIndexOperation entityIndexOperation =
            new EntityIndexOperation( applicationScope, entity.getId(), updatedAfter);

        final IndexOperationMessage indexMessage =
            eventBuilder.buildEntityIndex( entityIndexOperation ).toBlocking().lastOrDefault(null);

        queueIndexOperationMessage( indexMessage );

    }


    @Override
    public void queueNewEdge(final ApplicationScope applicationScope,
                             final Entity entity,
                             final Edge newEdge) {

        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager( applicationScope );

        final IndexOperationMessage indexMessage = ecm.load( entity.getId() )
            .flatMap( loadedEntity -> eventBuilder.buildNewEdge(applicationScope, entity, newEdge) )
            .toBlocking().lastOrDefault(null);

        queueIndexOperationMessage( indexMessage );

    }


    @Override
    public void queueDeleteEdge(final ApplicationScope applicationScope,
                                final Edge edge) {

        // sent in region (not offerTopic) as the delete IO happens in-region, then queues a multi-region de-index op
        offer( new EdgeDeleteEvent( queueFig.getPrimaryRegion(), applicationScope, edge ) );
    }

    public void handleEdgeDelete(final QueueMessage message) {

        Preconditions.checkNotNull( message, "Queue Message cannot be null for handleEdgeDelete" );

        final AsyncEvent event = (AsyncEvent) message.getBody();

        Preconditions.checkNotNull( message, "QueueMessage Body cannot be null for handleEdgeDelete" );
        Preconditions.checkArgument(event instanceof EdgeDeleteEvent, String.format("Event Type for handleEdgeDelete must be EDGE_DELETE, got %s", event.getClass()));


        final EdgeDeleteEvent edgeDeleteEvent = ( EdgeDeleteEvent ) event;

        final ApplicationScope applicationScope = edgeDeleteEvent.getApplicationScope();
        final Edge edge = edgeDeleteEvent.getEdge();

        if (logger.isDebugEnabled()) {
            logger.debug("Deleting in app scope {} with edge {}", applicationScope, edge);
        }

        IndexOperationMessage indexMessage =
            eventBuilder.buildDeleteEdge(applicationScope, edge).toBlocking().lastOrDefault(null);

        queueIndexOperationMessage(indexMessage);

    }



    /**
     * Queue up an indexOperationMessage for multi region execution
     * @param indexOperationMessage
     */
    public void queueIndexOperationMessage( final IndexOperationMessage indexOperationMessage ) {

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

        offerTopic( elasticsearchIndexEvent );
    }

    public IndexOperationMessage handleIndexOperation(final ElasticsearchIndexEvent elasticsearchIndexEvent){

        Preconditions.checkNotNull( elasticsearchIndexEvent, "elasticsearchIndexEvent cannot be null" );

        final UUID messageId = elasticsearchIndexEvent.getIndexBatchId();
        Preconditions.checkNotNull( messageId, "messageId must not be null" );


        final String message = esMapPersistence.getString( messageId.toString() );


        final IndexOperationMessage indexOperationMessage;
        if(message == null) {

            // provide some time back pressure before performing a quorum read
            if ( System.currentTimeMillis() > elasticsearchIndexEvent.getCreationTime() + queueFig.getLocalQuorumTimeout() ) {

                if(logger.isDebugEnabled()){
                    logger.debug("ES batch with id {} not found, reading with strong consistency", messageId);
                }

                final String highConsistency = esMapPersistence.getStringHighConsistency(messageId.toString());
                if (highConsistency == null) {

                   throw new RuntimeException("ES batch with id "+messageId+" not found when reading with strong consistency");
               }

               indexOperationMessage = ObjectJsonSerializer.INSTANCE.fromString(highConsistency, IndexOperationMessage.class);

           } else {

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

        return indexOperationMessage;

    }


    @Override
    public void queueDeIndexOldVersion(final ApplicationScope applicationScope, final Id entityId) {

        // queue the de-index of old versions to the topic so cleanup happens in all regions
        offerTopic( new DeIndexOldVersionsEvent( queueFig.getPrimaryRegion(),
            new EntityIdScope( applicationScope, entityId)) );

    }


    public IndexOperationMessage handleDeIndexOldVersionEvent ( final DeIndexOldVersionsEvent deIndexOldVersionsEvent){


        ApplicationScope applicationScope = deIndexOldVersionsEvent.getEntityIdScope().getApplicationScope();
        Id entityId = deIndexOldVersionsEvent.getEntityIdScope().getId();

        return eventBuilder.deIndexOlderVersions( applicationScope, entityId )
            .toBlocking().lastOrDefault(null);


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
        return queue.getQueueDepth();
    }

    @Override
    public void queueEntityDelete(final ApplicationScope applicationScope, final Id entityId) {

        // sent in region (not offerTopic) as the delete IO happens in-region, then queues a multi-region de-index op
        offer( new EntityDeleteEvent(queueFig.getPrimaryRegion(), new EntityIdScope( applicationScope, entityId ) ) );
    }

    public void handleEntityDelete(final QueueMessage message) {

        Preconditions.checkNotNull(message, "Queue Message cannot be null for handleEntityDelete");

        final AsyncEvent event = (AsyncEvent) message.getBody();
        Preconditions.checkNotNull( message, "QueueMessage Body cannot be null for handleEntityDelete" );
        Preconditions.checkArgument( event instanceof EntityDeleteEvent,
            String.format( "Event Type for handleEntityDelete must be ENTITY_DELETE, got %s", event.getClass() ) );


        final EntityDeleteEvent entityDeleteEvent = ( EntityDeleteEvent ) event;
        final ApplicationScope applicationScope = entityDeleteEvent.getEntityIdScope().getApplicationScope();
        final Id entityId = entityDeleteEvent.getEntityIdScope().getId();

        if (logger.isDebugEnabled())
            logger.debug("Deleting entity id from index in app scope {} with entityId {}", applicationScope, entityId);

        final EventBuilderImpl.EntityDeleteResults
            entityDeleteResults = eventBuilder.buildEntityDelete( applicationScope, entityId );


        // Delete the entities and remove from graph separately
        entityDeleteResults.getEntitiesDeleted().toBlocking().lastOrDefault(null);

        entityDeleteResults.getCompactedNode().toBlocking().lastOrDefault(null);

        IndexOperationMessage indexMessage = entityDeleteResults.getIndexObservable().toBlocking().lastOrDefault(null);

        queueIndexOperationMessage(indexMessage);

    }


    public void handleInitializeApplicationIndex(final AsyncEvent event, final QueueMessage message) {
        Preconditions.checkNotNull(message, "Queue Message cannot be null for handleInitializeApplicationIndex");
        Preconditions.checkArgument(event instanceof InitializeApplicationIndexEvent, String.format("Event Type for handleInitializeApplicationIndex must be APPLICATION_INDEX, got %s", event.getClass()));

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
        final int count = indexProcessorFig.getWorkerCount();

        for (int i = 0; i < count; i++) {
            startWorker();
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


    private void startWorker() {
        synchronized (mutex) {

            Observable<List<QueueMessage>> consumer =
                    Observable.create( new Observable.OnSubscribe<List<QueueMessage>>() {
                        @Override
                        public void call( final Subscriber<? super List<QueueMessage>> subscriber ) {

                            //name our thread so it's easy to see
                            Thread.currentThread().setName( "QueueConsumer_" + counter.incrementAndGet() );

                            List<QueueMessage> drainList = null;

                            do {
                                try {
                                    drainList = take();
                                    //emit our list in it's entity to hand off to a worker pool
                                        subscriber.onNext(drainList);

                                    //take since  we're in flight
                                    inFlight.addAndGet( drainList.size() );
                                }
                                catch ( Throwable t ) {
                                    final long sleepTime = indexProcessorFig.getFailureRetryTime();

                                    logger.error( "Failed to dequeue.  Sleeping for {} milliseconds", sleepTime, t );

                                    if ( drainList != null ) {
                                        inFlight.addAndGet( -1 * drainList.size() );
                                    }


                                    try {
                                        Thread.sleep( sleepTime );
                                    }
                                    catch ( InterruptedException ie ) {
                                        //swallow
                                    }

                                    indexErrorCounter.inc();
                                }
                            }
                            while ( true );
                        }
                    } )        //this won't block our read loop, just reads and proceeds
                        .flatMap( sqsMessages -> {

                            //do this on a different schedule, and introduce concurrency with flatmap for faster processing
                            return Observable.just( sqsMessages )

                                             .map( messages -> {
                                                 if ( messages == null || messages.size() == 0 ) {
                                                     // no messages came from the queue, move on
                                                     return null;
                                                 }

                                                 try {
                                                     // process the messages
                                                     List<IndexEventResult> indexEventResults = callEventHandlers( messages );

                                                     // submit the processed messages to index producer
                                                     List<QueueMessage> messagesToAck = submitToIndex( indexEventResults );

                                                     if ( messagesToAck.size() < messages.size() ) {
                                                         logger.warn( "Missing {} message(s) from index processing",
                                                            messages.size() - messagesToAck.size() );
                                                     }

                                                     // ack each message if making it to this point
                                                     if( messagesToAck.size() > 0 ){
                                                         ack( messagesToAck );
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

    /**
     * Submit results to index and return the queue messages to be ack'd
     *
     */
    private List<QueueMessage> submitToIndex(List<IndexEventResult> indexEventResults) {

        // if nothing came back then return empty list
        if(indexEventResults==null){
            return new ArrayList<>(0);
        }

        IndexOperationMessage combined = new IndexOperationMessage();
        List<QueueMessage> queueMessages = indexEventResults.stream()

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

        // sumbit the requests to Elasticsearch
        indexProducer.put(combined).toBlocking().last();

        return queueMessages;
    }

    public void index(final ApplicationScope applicationScope, final Id id, final long updatedSince) {

        EntityIndexOperation entityIndexOperation =
            new EntityIndexOperation( applicationScope, id, updatedSince);

        queueIndexOperationMessage(eventBuilder.buildEntityIndex( entityIndexOperation ).toBlocking().lastOrDefault(null));
    }

    public void indexBatch(final List<EdgeScope> edges, final long updatedSince) {

        IndexOperationMessage batch = new IndexOperationMessage();

        for ( EdgeScope e : edges){

            EntityIndexOperation entityIndexOperation =
                new EntityIndexOperation( e.getApplicationScope(), e.getEdge().getTargetNode(), updatedSince);

            IndexOperationMessage indexOperationMessage =
                eventBuilder.buildEntityIndex( entityIndexOperation ).toBlocking().lastOrDefault(null);

            if (indexOperationMessage != null){
                batch.ingest(indexOperationMessage);
            }

        }

        queueIndexOperationMessage(batch);
    }


    public class IndexEventResult{
        private final Optional<IndexOperationMessage> indexOperationMessage;
        private final Optional<QueueMessage> queueMessage;
        private final long creationTime;

        public IndexEventResult(Optional<IndexOperationMessage> indexOperationMessage, Optional<QueueMessage> queueMessage, long creationTime){

            this.queueMessage = queueMessage;
            this.creationTime = creationTime;
            this.indexOperationMessage = indexOperationMessage;
        }

        public Optional<IndexOperationMessage> getIndexOperationMessage() {
            return indexOperationMessage;
        }

        public Optional<QueueMessage> getQueueMessage() {
            return queueMessage;
        }

        public long getCreationTime() {
            return creationTime;
        }
    }

    public String getQueueManagerClass() {

        return queue.getClass().getSimpleName();

    }


}
