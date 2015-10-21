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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.asyncevents.model.AsyncEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.EdgeDeleteEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.EdgeIndexEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.ElasticsearchIndexEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.EntityDeleteEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.EntityIndexEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.InitializeApplicationIndexEvent;
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
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.index.impl.IndexProducer;
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
public class AmazonAsyncEventService implements AsyncEventService {


    private static final Logger logger = LoggerFactory.getLogger(AmazonAsyncEventService.class);

    // SQS maximum receive messages is 10
    public static int MAX_TAKE = 10;
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
    public AmazonAsyncEventService( final QueueManagerFactory queueManagerFactory,
                                    final IndexProcessorFig indexProcessorFig,
                                    final IndexProducer indexProducer,
                                    final MetricsFactory metricsFactory,
                                    final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                    final IndexLocationStrategyFactory indexLocationStrategyFactory,
                                    final EntityIndexFactory entityIndexFactory,
                                    final EventBuilder eventBuilder,
                                    final MapManagerFactory mapManagerFactory,
                                    final QueueFig queueFig,
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

        this.writeTimer = metricsFactory.getTimer(AmazonAsyncEventService.class, "async_event.write");
        this.readTimer = metricsFactory.getTimer(AmazonAsyncEventService.class, "async_event.read");
        this.ackTimer = metricsFactory.getTimer(AmazonAsyncEventService.class, "async_event.ack");
        this.indexErrorCounter = metricsFactory.getCounter(AmazonAsyncEventService.class, "async_event.error");
        this.messageCycle = metricsFactory.getHistogram(AmazonAsyncEventService.class, "async_event.message_cycle");


        //wire up the gauge of inflight message
        metricsFactory.addGauge(AmazonAsyncEventService.class, "async-event.inflight", new Gauge<Long>() {
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
            return queue.getMessages(MAX_TAKE,
                    indexProcessorFig.getIndexQueueVisibilityTimeout(),
                    indexProcessorFig.getIndexQueueTimeout(),
                    AsyncEvent.class);
        }
        //stop our timer
        finally {
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
            logger.debug("callEventHandlers with {} message", messages.size());
        }

        Stream<IndexEventResult> indexEventResults = messages.stream().map(message -> {
            AsyncEvent event = null;
            try {
                event = (AsyncEvent) message.getBody();
            } catch (ClassCastException cce) {
                logger.error("Failed to deserialize message body", cce);
            }

            if (event == null) {
                logger.error("AsyncEvent type or event is null!");
                return new IndexEventResult(Optional.fromNullable(message), Optional.<IndexOperationMessage>absent(),
                    System.currentTimeMillis());
            }

            final AsyncEvent thisEvent = event;
            if (logger.isDebugEnabled()) {
                logger.debug("Processing {} event", event);
            }

            try {
                //check for empty sets if this is true
                boolean validateEmptySets = true;
                Observable<IndexOperationMessage> indexoperationObservable;
                //merge each operation to a master observable;
                if (event instanceof EdgeDeleteEvent) {
                    indexoperationObservable = handleEdgeDelete(message);
                } else if (event instanceof EdgeIndexEvent) {
                    indexoperationObservable = handleEdgeIndex(message);
                } else if (event instanceof EntityDeleteEvent) {
                    indexoperationObservable = handleEntityDelete(message);
                } else if (event instanceof EntityIndexEvent) {
                    indexoperationObservable = handleEntityIndexUpdate(message);
                } else if (event instanceof InitializeApplicationIndexEvent) {
                    //does not return observable
                    handleInitializeApplicationIndex(event, message);
                    indexoperationObservable = Observable.just(new IndexOperationMessage());
                    validateEmptySets = false; //do not check this one for an empty set b/c it will be empty.
                } else if (event instanceof ElasticsearchIndexEvent) {
                    handleIndexOperation((ElasticsearchIndexEvent) event);
                    indexoperationObservable = Observable.just(new IndexOperationMessage());
                    validateEmptySets = false; //do not check this one for an empty set b/c it will be empty.
                } else {
                    throw new Exception("Unknown EventType");//TODO: print json instead
                }

                //collect all of the
                IndexOperationMessage indexOperationMessage = indexoperationObservable
                    .collect(() -> new IndexOperationMessage(), (collector, single) -> collector.ingest(single))
                    .toBlocking().lastOrDefault(null);

                if (validateEmptySets && (indexOperationMessage == null || indexOperationMessage.isEmpty())) {
                    logger.error("Received empty index sequence message:({}), body:({}) ", message.getMessageId(),
                        message.getStringBody());
                    throw new Exception("Received empty index sequence.");
                }

                //return type that can be indexed and ack'd later
                return new IndexEventResult(Optional.fromNullable(message),
                    Optional.fromNullable(indexOperationMessage), thisEvent.getCreationTime());
            } catch (Exception e) {
                logger.error("Failed to index message: " + message.getMessageId(), message.getStringBody(), e);
                return new IndexEventResult(Optional.absent(), Optional.<IndexOperationMessage>absent(),
                    event.getCreationTime());
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
                                       final Entity entity) {

        offer(new EntityIndexEvent(queueFig.getPrimaryRegion(),new EntityIdScope(applicationScope, entity.getId()), 0));
    }


    public Observable<IndexOperationMessage> handleEntityIndexUpdate(final QueueMessage message) {

        Preconditions.checkNotNull( message, "Queue Message cannot be null for handleEntityIndexUpdate" );

        final AsyncEvent event = ( AsyncEvent ) message.getBody();

        Preconditions.checkNotNull(message, "QueueMessage Body cannot be null for handleEntityIndexUpdate");
        Preconditions.checkArgument(event instanceof EntityIndexEvent, String.format("Event Type for handleEntityIndexUpdate must be ENTITY_INDEX, got %s", event.getClass()));

        final EntityIndexEvent entityIndexEvent = (EntityIndexEvent) event;


        //process the entity immediately
        //only process the same version, otherwise ignore
        final EntityIdScope entityIdScope = entityIndexEvent.getEntityIdScope();
        final ApplicationScope applicationScope = entityIdScope.getApplicationScope();
        final Id entityId = entityIdScope.getId();
        final long updatedAfter = entityIndexEvent.getUpdatedAfter();

        final EntityIndexOperation entityIndexOperation = new EntityIndexOperation( applicationScope, entityId, updatedAfter);

        final Observable<IndexOperationMessage> observable = eventBuilder.buildEntityIndex( entityIndexOperation );
        return observable;
    }


    @Override
    public void queueNewEdge(final ApplicationScope applicationScope,
                             final Entity entity,
                             final Edge newEdge) {

        EdgeIndexEvent operation = new EdgeIndexEvent(queueFig.getPrimaryRegion(), applicationScope, entity.getId(), newEdge);

        offer( operation );
    }

    public Observable<IndexOperationMessage> handleEdgeIndex(final QueueMessage message) {

        Preconditions.checkNotNull( message, "Queue Message cannot be null for handleEdgeIndex" );

        final AsyncEvent event = (AsyncEvent) message.getBody();

        Preconditions.checkNotNull( message, "QueueMessage Body cannot be null for handleEdgeIndex" );
        Preconditions.checkArgument(event instanceof EdgeIndexEvent, String.format("Event Type for handleEdgeIndex must be EDGE_INDEX, got %s", event.getClass()));

        final EdgeIndexEvent edgeIndexEvent = ( EdgeIndexEvent ) event;

        final ApplicationScope applicationScope = edgeIndexEvent.getApplicationScope();
        final Edge edge = edgeIndexEvent.getEdge();



        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager( applicationScope );

        final Observable<IndexOperationMessage> edgeIndexObservable = ecm.load( edgeIndexEvent.getEntityId() ).flatMap(
            entity -> eventBuilder.buildNewEdge(applicationScope, entity, edge));
        return edgeIndexObservable;
    }

    @Override
    public void queueDeleteEdge(final ApplicationScope applicationScope,
                                final Edge edge) {

        offer( new EdgeDeleteEvent( queueFig.getPrimaryRegion(), applicationScope, edge ) );
    }

    public Observable<IndexOperationMessage> handleEdgeDelete(final QueueMessage message) {

        Preconditions.checkNotNull( message, "Queue Message cannot be null for handleEdgeDelete" );

        final AsyncEvent event = (AsyncEvent) message.getBody();

        Preconditions.checkNotNull( message, "QueueMessage Body cannot be null for handleEdgeDelete" );
        Preconditions.checkArgument(event instanceof EdgeDeleteEvent, String.format("Event Type for handleEdgeDelete must be EDGE_DELETE, got %s", event.getClass()));


        final EdgeDeleteEvent edgeDeleteEvent = ( EdgeDeleteEvent ) event;

        final ApplicationScope applicationScope = edgeDeleteEvent.getApplicationScope();
        final Edge edge = edgeDeleteEvent.getEdge();

        if (logger.isDebugEnabled()) logger.debug("Deleting in app scope {} with edge {}", applicationScope, edge);

        final Observable<IndexOperationMessage> observable = eventBuilder.buildDeleteEdge(applicationScope, edge);
        return observable;
    }


    @Override
    public void queueEntityDelete(final ApplicationScope applicationScope, final Id entityId) {

        offer( new EntityDeleteEvent(queueFig.getPrimaryRegion(), new EntityIdScope( applicationScope, entityId ) ) );
    }


    /**
     * Queue up an indexOperationMessage for multi region execution
     * @param indexOperationMessage
     */
    public void queueIndexOperationMessage( final IndexOperationMessage indexOperationMessage ) {

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

    public void handleIndexOperation(final ElasticsearchIndexEvent elasticsearchIndexEvent){
         Preconditions.checkNotNull( elasticsearchIndexEvent, "elasticsearchIndexEvent cannot be null" );

        final UUID messageId = elasticsearchIndexEvent.getIndexBatchId();

        Preconditions.checkNotNull( messageId, "messageId must not be null" );


        //load the entity

        final String message = esMapPersistence.getString( messageId.toString() );

        final IndexOperationMessage indexOperationMessage;

        if(message == null){
            logger.warn( "Received message with id {} to process, unable to find it, reading with higher consistency level",
                messageId);

            final String highConsistency =  esMapPersistence.getStringHighConsistency( messageId.toString() );

            if(highConsistency == null){
                logger.error( "Unable to find the ES batch with id {} to process at a higher consistency level",
                    messageId);

                throw new RuntimeException( "Unable to find the ES batch to process with message id " + messageId );
            }

            indexOperationMessage = ObjectJsonSerializer.INSTANCE.fromString( highConsistency, IndexOperationMessage.class );

        } else{
            indexOperationMessage = ObjectJsonSerializer.INSTANCE.fromString( message, IndexOperationMessage.class );
        }


        //NOTE that we intentionally do NOT delete from the map.  We can't know when all regions have consumed the message
        //so we'll let compaction on column expiration handle deletion

        //read the value from the string

        Preconditions.checkNotNull( indexOperationMessage, "indexOperationMessage cannot be null" );
        Preconditions.checkArgument( !indexOperationMessage.isEmpty() , "queued indexOperationMessage messages should not be empty" );


        //now execute it
        indexProducer.put(indexOperationMessage).toBlocking().last();

    }



    @Override
    public long getQueueDepth() {
        return queue.getQueueDepth();
    }

    public Observable<IndexOperationMessage> handleEntityDelete(final QueueMessage message) {

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


        entityDeleteResults
            .getEntitiesCompacted()
            .collect(() -> new ArrayList<>(), (list, item) -> list.add(item)).toBlocking().lastOrDefault(null);

        return entityDeleteResults.getIndexObservable();
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
                    } )
                            //this won't block our read loop, just reads and proceeds
                            .map( messages -> {
                                if ( messages == null || messages.size() == 0 ) {
                                    return null;
                                }

                                try {
                                    List<IndexEventResult> indexEventResults = callEventHandlers( messages );
                                    List<QueueMessage> messagesToAck = submitToIndex( indexEventResults );
                                    if ( messagesToAck == null || messagesToAck.size() == 0 ) {
                                        logger.error( "No messages came back from the queue operation should have seen "
                                            + messages.size(), messages );
                                        return messagesToAck;
                                    }
                                    if ( messagesToAck.size() < messages.size() ) {
                                        logger.error( "Missing messages from queue post operation", messages,
                                            messagesToAck );
                                    }
                                    //ack each message, but only if we didn't error.
                                    ack( messagesToAck );
                                    return messagesToAck;
                                }
                                catch ( Exception e ) {
                                    logger.error( "failed to ack messages to sqs", e );
                                    return null;
                                    //do not rethrow so we can process all of them
                                }
                            } );

            //start in the background

            final Subscription subscription = consumer.subscribeOn(Schedulers.newThread()).subscribe();

            subscriptions.add(subscription);
        }
    }

    /**
     * Submit results to index and return the queue messages to be ack'd
     * @param indexEventResults
     * @return
     */
    private List<QueueMessage> submitToIndex( List<IndexEventResult> indexEventResults) {
        //if nothing came back then return null
        if(indexEventResults==null){
            return null;
        }

        final IndexOperationMessage combined = new IndexOperationMessage();

        //stream and filer the messages
        List<QueueMessage> messagesToAck = indexEventResults.stream()
            .map(indexEventResult -> {
                //collect into the index submission
                if (indexEventResult.getIndexOperationMessage().isPresent()) {
                    combined.ingest(indexEventResult.getIndexOperationMessage().get());
                }
                return indexEventResult;
            })
                //filter out the ones that need to be ack'd
            .filter(indexEventResult -> indexEventResult.getQueueMessage().isPresent())
            .map(indexEventResult -> {
                //record the cycle time
                messageCycle.update(System.currentTimeMillis() - indexEventResult.getCreationTime());
                return indexEventResult;
            })
                //ack after successful completion of the operation.
            .map(result -> result.getQueueMessage().get())
            .collect(Collectors.toList());

        //only Q it if it's empty
        if(!combined.isEmpty()) {
            queueIndexOperationMessage( combined );
        }

        return messagesToAck;
    }

    public void index(final ApplicationScope applicationScope, final Id id, final long updatedSince) {
        //change to id scope to avoid serialization issues
        offer( new EntityIndexEvent(queueFig.getPrimaryRegion(), new EntityIdScope( applicationScope, id ), updatedSince ) );
    }

    public void indexBatch(final List<EdgeScope> edges, final long updatedSince) {

        List batch = new ArrayList<EdgeScope>();
        for ( EdgeScope e : edges){
            //change to id scope to avoid serialization issues
            batch.add(new EntityIndexEvent(queueFig.getPrimaryRegion(), new EntityIdScope(e.getApplicationScope(), e.getEdge().getTargetNode()), updatedSince));
        }
        offerBatch( batch );
    }


    public class IndexEventResult{
        private final Optional<QueueMessage> queueMessage;
        private final Optional<IndexOperationMessage> indexOperationMessage;
        private final long creationTime;


        public IndexEventResult(Optional<QueueMessage> queueMessage, Optional<IndexOperationMessage> indexOperationMessage, long creationTime){

            this.queueMessage = queueMessage;
            this.indexOperationMessage = indexOperationMessage;

            this.creationTime = creationTime;
        }


        public Optional<QueueMessage> getQueueMessage() {
            return queueMessage;
        }

        public Optional<IndexOperationMessage> getIndexOperationMessage() {
            return indexOperationMessage;
        }

        public long getCreationTime() {
            return creationTime;
        }
    }


}
