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

package org.apache.usergrid.persistence.core.rx;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeMultimap;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;


/**
 * Produces a single Observable from multiple ordered source observables.  The same as the "merge" step in a merge sort.
 * Ensure that your comparator matches the ordering of your inputs, or you may get strange results. The current
 * implementation requires each Observable to be running in it's own thread.  Once backpressure in RX is implemented,
 * this requirement can be removed.
 */
public final class OrderedMerge<T> implements Observable.OnSubscribe<T> {

    private static Logger log = LoggerFactory.getLogger( OrderedMerge.class );

    //the comparator to compare items
    private final Comparator<T> comparator;

    private final Observable<? extends T>[] observables;


    //The max amount to buffer before blowing up
    private final int maxBufferSize;


    private OrderedMerge( final Comparator<T> comparator, final int maxBufferSize,
                          Observable<? extends T>... observables ) {
        this.comparator = comparator;
        this.maxBufferSize = maxBufferSize;
        this.observables = observables;
    }


    @Override
    public void call( final Subscriber<? super T> outerOperation ) {


        CompositeSubscription csub = new CompositeSubscription();


        //when a subscription is received, we need to subscribe on each observable
        SubscriberCoordinator coordinator = new SubscriberCoordinator( comparator, outerOperation, observables.length );

        InnerObserver<T>[] innerObservers = new InnerObserver[observables.length];


        //we have to do this in 2 steps to get the synchronization correct.  We must set up our total inner observers
        //before starting subscriptions otherwise our assertions for completion or starting won't work properly
        for ( int i = 0; i < observables.length; i++ ) {
            //subscribe to each one and add it to the composite
            //create a new inner and subscribe
            final InnerObserver<T> inner = new InnerObserver<T>( coordinator, maxBufferSize, i );

            coordinator.add( inner );

            innerObservers[i] = inner;
        }
        /**
         * Once we're set up, begin the subscription to sub observables
         */
        for ( int i = 0; i < observables.length; i++ ) {
            //subscribe after setting them up
            //add our subscription to the composite for future cancellation
            Subscription subscription = observables[i].subscribe( innerObservers[i] );

            csub.add( subscription );

            //add the internal composite subscription
            outerOperation.add( csub );
        }
    }


    /**
     * Our coordinator.  It coordinates all the
     */
    private static final class SubscriberCoordinator<T> {


        private final AtomicInteger completedCount = new AtomicInteger();
        private volatile boolean readyToProduce = false;


        private final Subscriber<? super T> subscriber;
        private final TreeMultimap<T, InnerObserver<T>> nextValues;
        private final List<InnerObserver<T>> innerSubscribers;
        private final ArrayDeque<InnerObserver<T>> toProduce;


        private SubscriberCoordinator( final Comparator<T> comparator, final Subscriber<? super T> subscriber,
                                       final int innerSize ) {
            //we only want to emit events serially
            this.subscriber = new SerializedSubscriber( subscriber );
            this.innerSubscribers = new ArrayList<>( innerSize );
            this.nextValues = TreeMultimap.create( comparator, InnerObserverComparator.INSTANCE );
            this.toProduce = new ArrayDeque<>( innerSize );
        }


        public void onCompleted() {

            /**
             * Invoke next to remove any elements from other Q's from this event
             */
            next();

            final int completed = completedCount.incrementAndGet();


            //we're done, just drain the queue since there are no more running producers
            if ( completed == innerSubscribers.size() ) {

                log.trace( "Completing Observable.  Draining elements from the subscribers", innerSubscribers.size() );

                //Drain the queues
                while ( !subscriber.isUnsubscribed() && (!nextValues.isEmpty() || !toProduce.isEmpty()) ) {
                    next();
                }

                //signal completion
                subscriber.onCompleted();
            }
        }


        public void add( InnerObserver<T> inner ) {
            this.innerSubscribers.add( inner );
            this.toProduce.add( inner );
        }


        public void onError( Throwable e ) {
            subscriber.onError( e );
        }


        public void next() {

            //we want to emit items in order, so we synchronize our next
            synchronized ( this ) {
                /**
                 * Init before our loop
                 */
                while ( !toProduce.isEmpty() ) {

                    InnerObserver<T> inner = toProduce.pop();

                    //This has nothing left to produce, skip it
                    if ( inner.drained ) {
                        continue;
                    }

                    final T nextKey = inner.peek();

                    //we can't produce, not everything has an element to inspect, leave it in the set to produce next
                    // time
                    if ( nextKey == null ) {
                        toProduce.push( inner );
                        return;
                    }

                    //add it to our fast access set
                    nextValues.put( nextKey, inner );
                }


                //take as many elements as we can until we hit a case where we can't take anymore
                while ( !nextValues.isEmpty() ) {


                    /**
                     * Get our lowest key and begin producing until we can't produce any longer
                     */
                    final T lowestKey = nextValues.keySet().first();


                    //we need to create a copy, otherwise we receive errors. We use ArrayDque

                    NavigableSet<InnerObserver<T>> nextObservers = nextValues.get( lowestKey );

                    while ( !nextObservers.isEmpty() ) {

                        final InnerObserver<T> inner = nextObservers.pollFirst();

                        nextValues.remove( lowestKey, inner );

                        final T value = inner.pop();

                        log.trace( "Emitting value {}", value );

                        subscriber.onNext( value );

                        final T nextKey = inner.peek();

                        //nothing to peek, it's either drained or slow
                        if ( nextKey == null ) {

                            //it's drained, nothing left to do
                            if ( inner.drained ) {
                                continue;
                            }

                            //it's slow, we can't process because we don't know if this is another min value without
                            // inspecting it. Stop emitting and try again next pass through
                            toProduce.push( inner );
                            return;
                        }

                        //we have a next value, insert it and keep running
                        nextValues.put( nextKey, inner );
                    }
                }
            }
        }


//        /**
//         * Return true if every inner observer has been drained
//         */
//        private boolean drained() {
//            //perform an audit
//            for ( InnerObserver<T> inner : innerSubscribers ) {
//                if ( !inner.drained ) {
//                    return false;
//                }
//            }
//
//            return true;
//        }
    }


    private static final class InnerObserverComparator implements Comparator<InnerObserver> {

        private static final InnerObserverComparator INSTANCE = new InnerObserverComparator();


        @Override
        public int compare( final InnerObserver o1, final InnerObserver o2 ) {
            return Integer.compare( o1.id, o2.id );
        }
    }


    private static final class InnerObserver<T> extends Subscriber<T> {

        private final SubscriberCoordinator<T> coordinator;
        private final Deque<T> items = new LinkedList<>();
        private final int maxQueueSize;
        /**
         * TODO: T.N. Once backpressure makes it into RX Java, this needs to be remove and should use backpressure
         */
        private final Semaphore semaphore;

        /**
         * Our id so we have something unique to compare in the multimap
         */
        public final int id;


        /**
         * Flags for synchronization with coordinator. Multiple threads may be used, so volatile is required
         */
        private volatile boolean started = false;
        private volatile boolean completed = false;
        private volatile boolean drained = false;


        public InnerObserver( final SubscriberCoordinator<T> coordinator, final int maxQueueSize, final int id ) {
            this.coordinator = coordinator;
            this.maxQueueSize = maxQueueSize;
            this.id = id;

            this.semaphore = new Semaphore( maxQueueSize );
        }


        @Override
        public void onCompleted() {
            started = true;
            completed = true;
            checkDrained();

            /**
             * release this semaphore and invoke next.  Both these calls can be removed when backpressure is added.
             * We need the next to force removal of other inner consumers
             */
            coordinator.onCompleted();
        }


        @Override
        public void onError( Throwable e ) {
            coordinator.onError( e );
        }


        @Override
        public void onNext( T a ) {

            try {
                this.semaphore.acquire();
            }
            catch ( InterruptedException e ) {
                onError( e );
            }


            items.add( a );

            started = true;

            //for each subscriber, emit to the parent wrapper then evaluate calling on next
            coordinator.next();
        }


        public T peek() {
            return items.peekFirst();
        }


        public T pop() {
            T item = items.pollFirst();

            //release the semaphore since we just took an item
            this.semaphore.release();

            checkDrained();

            return item;
        }


        /**
         * if we've started and finished, and this is the last element, we want to mark ourselves as completely drained
         */
        private void checkDrained() {
            drained = started && completed && items.size() == 0;
        }
    }


    /**
     * Create our ordered merge
     */
    public static <T> Observable<T> orderedMerge( Comparator<T> comparator, int maxBufferSize,
                                                  Observable<? extends T>... observables ) {

        return Observable.create( new OrderedMerge<T>( comparator, maxBufferSize, observables ) );
    }
}
