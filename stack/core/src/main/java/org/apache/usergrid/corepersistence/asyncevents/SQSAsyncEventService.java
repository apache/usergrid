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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.index.IndexProcessorFig;
import org.apache.usergrid.corepersistence.index.IndexService;
import org.apache.usergrid.exception.NotImplementedException;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
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
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;


@Singleton
public class SQSAsyncEventService implements AsyncEventService {


    private static final Logger log = LoggerFactory.getLogger( SQSAsyncEventService.class );

    /**
     * Set our TTL to 1 month.  This is high, but in the event of a bug, we want these entries to get removed
     */
    public static final int TTL = 60 * 60 * 24 * 30;


    private static final int MAX_TAKE = 10;

    private static final String QUEUE_NAME = "es_queue";

    private final QueueManager queue;
    private final IndexProcessorFig indexProcessorFig;
    private final IndexService indexService;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final RxTaskScheduler rxTaskScheduler;

    private final Timer readTimer;
    private final Timer writeTimer;
    private final Timer messageProcessingTimer;

    private final Object mutex = new Object();


    private final Counter indexErrorCounter;
    private final AtomicLong counter = new AtomicLong();
    private final AtomicLong inFlight = new AtomicLong();

    //the actively running subscription
    private List<Subscription> subscriptions = new ArrayList<>();


    @Inject
    public SQSAsyncEventService( final QueueManagerFactory queueManagerFactory,
                                 final IndexProcessorFig indexProcessorFig, final MetricsFactory metricsFactory,
                                 final IndexService indexService,
                                 final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                 final RxTaskScheduler rxTaskScheduler ) {

        this.indexService = indexService;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.rxTaskScheduler = rxTaskScheduler;

        final QueueScope queueScope = new QueueScopeImpl( QUEUE_NAME );
        this.queue = queueManagerFactory.getQueueManager( queueScope );
        this.indexProcessorFig = indexProcessorFig;

        this.writeTimer = metricsFactory.getTimer( SQSAsyncEventService.class, "write" );
        this.readTimer = metricsFactory.getTimer( SQSAsyncEventService.class, "read" );
        this.messageProcessingTimer = metricsFactory.getTimer( SQSAsyncEventService.class, "message.processing" );
        this.indexErrorCounter = metricsFactory.getCounter( SQSAsyncEventService.class, "error" );


        //wire up the gauge of inflight messages
        metricsFactory.addGauge( SQSAsyncEventService.class, "inflight.meter", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return inFlight.longValue();
            }
        } );

        start();
    }


    /**
     * Offer the EntityIdScope to SQS
     */
    private void offer( final EntityIdScope operation ) {
        final Timer.Context timer = this.writeTimer.time();

        try {
            //signal to SQS
            this.queue.sendMessage( operation );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to queue message", e );
        }
        finally {
            timer.stop();
        }
    }


    /**
     * Take messages from SQS
     */
    public List<QueueMessage> take() {

        //SQS doesn't support more than 10
        final Timer.Context timer = this.readTimer.time();

        try {
            return queue.getMessages( MAX_TAKE, indexProcessorFig.getIndexQueueTimeout(), indexProcessorFig.getIndexQueueTimeout(),
                EntityIdScope.class );
        }
        //stop our timer
        finally {
            timer.stop();
        }
    }


    /**
     * Ack messages in SQS
     */
    public void ack( final List<QueueMessage> messages ) {

        /**
         * No op
         */
        if ( messages.size() == 0 ) {
            return;
        }

        queue.commitMessages( messages );
    }


    @Override
    public void index( final EntityIdScope entityIdScope ) {
        //queue the re-inex operation
        offer( entityIdScope );
    }


    @Override
    public void queueEntityIndexUpdate( final ApplicationScope applicationScope, final Entity entity ) {

        //create our scope
        final EntityIdScope entityIdScope = new EntityIdScope( applicationScope, entity.getId() );

        //send it to SQS  for indexing
        index( entityIdScope );
    }


    @Override
    public void queueNewEdge( final ApplicationScope applicationScope, final Entity entity, final Edge newEdge ) {
       throw new NotImplementedException( "Implement me" );
    }


    @Override
    public void queueDeleteEdge( final ApplicationScope applicationScope, final Edge edge ) {
        throw new NotImplementedException( "Implement me" );
    }


    @Override
    public void queueEntityDelete( final ApplicationScope applicationScope, final Id entityId ) {
        throw new NotImplementedException( "Implement me" );
    }


    /**
     * Index an entity and return an observable of the queue message on success
     */
    private Observable<IndexOperationMessage> indexEntity( final QueueMessage queueMessage ) {
        final EntityIdScope entityIdScope = ( EntityIdScope ) queueMessage.getBody();
        final ApplicationScope applicationScope = entityIdScope.getApplicationScope();
        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );


        //run the index operation from the entity
        return entityCollectionManager.load( entityIdScope.getId() )
            //invoke the indexing and take the last value
            .flatMap( entity -> indexService.indexEntity( applicationScope, entity ).last() );
    }


    /**
     * Do the indexing for a list of queue messages
     */
    private void doIndex( final List<QueueMessage> queueMessages ) {
        //create parallel observables to process all 10 messages
        final Observable<Long> observable = Observable.from( queueMessages ).flatMap( ( QueueMessage queueMessage ) -> {
            return indexEntity( queueMessage ).subscribeOn( rxTaskScheduler.getAsyncIOScheduler() );
        }, MAX_TAKE ).countLong()

            //remove our in flight
            .doOnNext( count -> inFlight.addAndGet( -1 * count ) )

                //do on completed ack messages.  Assumes no expections were thrown
            .doOnCompleted( () -> ack( queueMessages ) );

        //wrap with our timer and fire
        ObservableTimer.time( observable, messageProcessingTimer ).subscribe();
    }


    /**
     * Loop throught and start the workers
     */
    public void start() {
        final int count = indexProcessorFig.getWorkerCount();

        for ( int i = 0; i < count; i++ ) {
            startWorker();
        }
    }


    /**
     * Stop the workers
     */
    public void stop() {
        synchronized ( mutex ) {
            //stop consuming

            for ( final Subscription subscription : subscriptions ) {
                subscription.unsubscribe();
            }
        }
    }


    private void startWorker() {
        synchronized ( mutex ) {

            Observable<List<QueueMessage>> consumer =
                Observable.create( new Observable.OnSubscribe<List<QueueMessage>>() {
                        @Override
                        public void call( final Subscriber<? super List<QueueMessage>> subscriber ) {

                            //name our thread so it's easy to see
                            Thread.currentThread().setName( "QueueConsumer_" + counter.incrementAndGet() );

                            List<QueueMessage> drainList = null;

                            do {
                                Timer.Context timer = readTimer.time();

                                try {
                                    drainList = take();

                                    //emit our list in it's entirity to hand off to a worker pool
                                    subscriber.onNext( drainList );

                                    //take since  we're in flight
                                    inFlight.addAndGet( drainList.size() );
                                }

                                catch ( Throwable t ) {
                                    final long sleepTime = indexProcessorFig.getFailureRetryTime();

                                    log.error( "Failed to dequeue.  Sleeping for {} milliseconds", sleepTime, t );

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

                                finally{
                                    timer.stop();
                                }
                            }
                            while ( true );
                        }
                    } )
                    //this won't block our read loop, just reads and proceeds
                    .doOnNext( messages -> doIndex( messages ) ).subscribeOn( Schedulers.newThread() );

            //start in the background

            final Subscription subscription = consumer.subscribe();

            subscriptions.add( subscription );
        }
    }
}
