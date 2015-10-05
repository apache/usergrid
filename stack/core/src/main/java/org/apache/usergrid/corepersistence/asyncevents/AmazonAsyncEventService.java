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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Optional;
import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.asyncevents.model.AsyncEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.EdgeDeleteEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.EdgeIndexEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.EntityDeleteEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.EntityIndexEvent;
import org.apache.usergrid.corepersistence.asyncevents.model.InitializeApplicationIndexEvent;
import org.apache.usergrid.corepersistence.index.EntityIndexOperation;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.index.IndexProcessorFig;
import org.apache.usergrid.corepersistence.index.ReplicatedIndexLocationStrategy;
import org.apache.usergrid.corepersistence.rx.impl.EdgeScope;
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
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.apache.usergrid.persistence.queue.QueueMessage;
import org.apache.usergrid.persistence.queue.QueueScope;
import org.apache.usergrid.persistence.queue.impl.QueueScopeImpl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


@Singleton
public class AmazonAsyncEventService implements AsyncEventService {


    private static final Logger logger = LoggerFactory.getLogger(AmazonAsyncEventService.class);

    // SQS maximum receive messages is 10
    private static final int MAX_TAKE = 10;
    public static final String QUEUE_NAME = "index"; //keep this short as AWS limits queue name size to 80 chars

    private final QueueManager queue;
    private final QueueScope queueScope;
    private final IndexProcessorFig indexProcessorFig;
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
                                    final RxTaskScheduler rxTaskScheduler ) {
        this.indexProducer = indexProducer;

        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.eventBuilder = eventBuilder;
        this.rxTaskScheduler = rxTaskScheduler;

        this.queueScope = new QueueScopeImpl(QUEUE_NAME, QueueScope.RegionImplementation.ALL);
        this.queue = queueManagerFactory.getQueueManager(queueScope);
        this.indexProcessorFig = indexProcessorFig;

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
    private void offer(final Object operation) {
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

    private void offerBatch(final List operations){
        final Timer.Context timer = this.writeTimer.time();

        try {
            //signal to SQS
            this.queue.sendMessages( operations );
        } catch (IOException e) {
            throw new RuntimeException("Unable to queue message", e);
        } finally {
            timer.stop();
        }
    }


    /**
     * Take message from SQS
     */
    private Observable<QueueMessage> take() {

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
    public void ack(final QueueMessage message) {

        final Timer.Context timer = this.ackTimer.time();

        try{
            queue.commitMessage(message);

            //decrement our in-flight counter
            inFlight.decrementAndGet();

        }catch(Exception e){
            throw new RuntimeException("Unable to ack messages", e);
        }finally {
            timer.stop();
        }


    }


    private Observable<IndexEventResult> handleMessages( final List<QueueMessage> messages ) {
        if (logger.isDebugEnabled()) {
            logger.debug("handleMessages with {} message", messages.size());
        }

        Observable<IndexEventResult> masterObservable = Observable.from(messages).flatMap(message -> {
            final AsyncEvent event = (AsyncEvent) message.getBody();

            logger.debug("Processing {} event", event);

            if (event == null) {
                logger.error("AsyncEvent type or event is null!");
                return Observable.just(new IndexEventResult(message, Optional.<IndexOperationMessage>absent(), true));
            }
            try {
                //merge each operation to a master observable;
                if (event instanceof EdgeDeleteEvent) {
                    return handleIndexOperation(message, queueMessage -> handleEdgeDelete(queueMessage));
                } else if (event instanceof EdgeIndexEvent) {
                    return handleIndexOperation(message, queueMessage -> handleEdgeIndex(queueMessage));
                } else if (event instanceof EntityDeleteEvent) {
                    return handleIndexOperation(message, queueMessage -> handleEntityDelete(queueMessage));
                } else if (event instanceof EntityIndexEvent) {
                    return handleIndexOperation(message, queueMessage -> handleEntityIndexUpdate(queueMessage));
                } else if (event instanceof InitializeApplicationIndexEvent) {
                    //does not return observable
                    handleInitializeApplicationIndex(message);
                    return Observable.just(new IndexEventResult(message, Optional.<IndexOperationMessage>absent(), false));
                } else {
                    logger.error("Unknown EventType: {}", event);
                    return Observable.just(new IndexEventResult(message, Optional.<IndexOperationMessage>absent(), true));
                }
            }catch (Exception e){
                logger.error("Failed to index entity", e,message);
                return Observable.just(new IndexEventResult(message, Optional.<IndexOperationMessage>absent(), false));
            }finally {
                messageCycle.update(System.currentTimeMillis() - event.getCreationTime());

            }
        });

        return masterObservable
            //remove unsuccessful
            .filter( indexEventResult -> indexEventResult.shouldProcess()  )
            //take the max
            .buffer( MAX_TAKE )
            //map them to index results and return them
            .flatMap( indexEventResults -> {
                IndexOperationMessage combined = new IndexOperationMessage();
                indexEventResults.stream().forEach(
                    indexEventResult ->{
                        if(indexEventResult.getIndexOperationMessage().isPresent()) {
                            combined.ingest(indexEventResult.getIndexOperationMessage().get());
                        }
                    } );


                //ack after successful completion of the operation.
                return indexProducer.put(combined)
                    .flatMap(operationResult -> Observable.from(indexEventResults));

            } );

    }

    //transform index operation to
    private Observable<IndexEventResult> handleIndexOperation(QueueMessage queueMessage,
                                                              Func1<QueueMessage, Observable<IndexOperationMessage>> operation
    ){
        try{
            return operation.call(queueMessage)
                .map(indexOperationMessage -> new IndexEventResult(queueMessage, Optional.fromNullable(indexOperationMessage), true));
        }catch (Exception e){
            logger.error("failed to run index",e);
            return Observable.just( new IndexEventResult(queueMessage, Optional.<IndexOperationMessage>absent(),false));
        }
    }


    @Override
    public void queueInitializeApplicationIndex( final ApplicationScope applicationScope) {
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(
            applicationScope );
        offer(new InitializeApplicationIndexEvent(new ReplicatedIndexLocationStrategy(indexLocationStrategy)));
    }


    @Override
    public void queueEntityIndexUpdate(final ApplicationScope applicationScope,
                                       final Entity entity) {

        offer(new EntityIndexEvent(new EntityIdScope(applicationScope, entity.getId()), 0));
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

        EdgeIndexEvent operation = new EdgeIndexEvent(applicationScope, entity.getId(), newEdge);

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

        final Observable<IndexOperationMessage> edgeIndexObservable = ecm.load(edgeIndexEvent.getEntityId()).flatMap( entity -> eventBuilder.buildNewEdge(
            applicationScope, entity, edge ) );
        return edgeIndexObservable;
    }

    @Override
    public void queueDeleteEdge(final ApplicationScope applicationScope,
                                final Edge edge) {

        offer( new EdgeDeleteEvent( applicationScope, edge ) );
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

        final Observable<IndexOperationMessage> observable = eventBuilder.buildDeleteEdge( applicationScope, edge );
        return observable;
    }


    @Override
    public void queueEntityDelete(final ApplicationScope applicationScope, final Id entityId) {

        offer( new EntityDeleteEvent( new EntityIdScope( applicationScope, entityId ) ) );
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


        final Observable merged = Observable.merge( entityDeleteResults.getEntitiesCompacted(),
            entityDeleteResults.getIndexObservable() );
        return merged;
    }


    public void handleInitializeApplicationIndex(final QueueMessage message) {
        Preconditions.checkNotNull(message, "Queue Message cannot be null for handleInitializeApplicationIndex");

        final AsyncEvent event = (AsyncEvent) message.getBody();
        Preconditions.checkNotNull( message, "QueueMessage Body cannot be null for handleInitializeApplicationIndex" );
        Preconditions.checkArgument(event instanceof InitializeApplicationIndexEvent, String.format("Event Type for handleInitializeApplicationIndex must be APPLICATION_INDEX, got %s", event.getClass()));

        final InitializeApplicationIndexEvent initializeApplicationIndexEvent =
            ( InitializeApplicationIndexEvent ) event;

        final IndexLocationStrategy indexLocationStrategy = initializeApplicationIndexEvent.getIndexLocationStrategy();
        final EntityIndex index = entityIndexFactory.createEntityIndex( indexLocationStrategy );
        index.initialize();
        ack( message );
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

            Observable<IndexEventResult> consumer =
                    Observable.create(new Observable.OnSubscribe<List<QueueMessage>>() {
                        @Override
                        public void call(final Subscriber<? super List<QueueMessage>> subscriber) {

                            //name our thread so it's easy to see
                            Thread.currentThread().setName("QueueConsumer_" + counter.incrementAndGet());

                            List<QueueMessage> drainList = null;

                            do {
                                try {
                                    drainList = take().toList().toBlocking().lastOrDefault(null);
                                    //emit our list in it's entity to hand off to a worker pool
                                    subscriber.onNext(drainList);

                                    //take since  we're in flight
                                    inFlight.addAndGet(drainList.size());
                                } catch (Throwable t) {
                                    final long sleepTime = indexProcessorFig.getFailureRetryTime();

                                    logger.error("Failed to dequeue.  Sleeping for {} milliseconds", sleepTime, t);

                                    if (drainList != null) {
                                        inFlight.addAndGet(-1 * drainList.size());
                                    }


                                    try {
                                        Thread.sleep(sleepTime);
                                    } catch (InterruptedException ie) {
                                        //swallow
                                    }

                                    indexErrorCounter.inc();
                                }
                            }
                            while (true);
                        }
                    })
                            //this won't block our read loop, just reads and proceeds
                            .map(messages ->
                                    handleMessages(messages)
                                        .map(indexEventResult -> {
                                            ack( indexEventResult.getQueueMessage() );
                                            return indexEventResult;
                                        })
                                        .toBlocking().lastOrDefault(null)
                            )//ack each message, but only if we didn't error.  If we did, we'll want to log it and
                            .subscribeOn( Schedulers.newThread() );

            //start in the background

            final Subscription subscription = consumer.subscribe();

            subscriptions.add(subscription);
        }
    }

    public void index(final ApplicationScope applicationScope, final Id id, final long updatedSince) {
        //change to id scope to avoid serialization issues
        offer( new EntityIndexEvent( new EntityIdScope( applicationScope, id ), updatedSince ) );
    }

    public void indexBatch(final List<EdgeScope> edges, final long updatedSince) {

        List batch = new ArrayList<EdgeScope>();
        for ( EdgeScope e : edges){
            //change to id scope to avoid serialization issues
            batch.add(new EntityIndexEvent(new EntityIdScope(e.getApplicationScope(), e.getEdge().getTargetNode()), updatedSince));
        }
        offerBatch( batch );
    }


    public class IndexEventResult{
        private final QueueMessage queueMessage;
        private final Optional<IndexOperationMessage> indexOperationMessage;
        private final boolean shouldProcess;

        public IndexEventResult(QueueMessage queueMessage, Optional<IndexOperationMessage> indexOperationMessage ,boolean shouldProcess){

            this.queueMessage = queueMessage;
            this.indexOperationMessage = indexOperationMessage;
            this.shouldProcess = shouldProcess;
        }

        public QueueMessage getQueueMessage() {
            return queueMessage;
        }

        public boolean shouldProcess() {
            return shouldProcess;
        }

        public Optional<IndexOperationMessage> getIndexOperationMessage() {
            return indexOperationMessage;
        }
    }
}
