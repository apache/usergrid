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
package org.apache.usergrid.persistence.core.consistency;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.hystrix.HystrixGraphObservable;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;


/**
 * The implementation of asynchronous processing. This is intentionally kept as a 1 processor to 1 event type mapping
 * This way reflection is not used, event dispatching is easier, and has compile time checking
 */
@Singleton
public class AsyncProcessorImpl<T extends Serializable> implements AsyncProcessor<T> {

    /**
     * TODO, run this with hystrix
     */

    private static final Logger LOG = LoggerFactory.getLogger( AsyncProcessor.class );

    protected final TimeoutQueue<T> queue;
//    protected final ConsistencyFig consistencyFig;
    protected final List<MessageListener<T, ?>> listeners = new ArrayList<>();


    protected List<ErrorListener<T>> errorListeners = new ArrayList<ErrorListener<T>>();
    protected List<CompleteListener<T>> completeListeners = new ArrayList<CompleteListener<T>>();


    @Inject
    public AsyncProcessorImpl( final TimeoutQueue<T> queue, final ConsistencyFig consistencyFig ) {
        this.queue = queue;
//        this.consistencyFig = consistencyFig;

        //we purposefully use a new thread.  We don't want to use one of the I/O threads to run this task
        //in the event the scheduler is full, we'll end up rejecting the reschedule of this task
        Schedulers.newThread().createWorker().schedulePeriodically( new TimeoutTask<T>( this, consistencyFig ), consistencyFig.getTaskLoopTime(),
                consistencyFig.getTaskLoopTime(), TimeUnit.MILLISECONDS );
    }


    @Override
    public AsynchronousMessage<T> setVerification( final T event, final long timeout ) {
        return queue.queue( event, timeout );
    }


    @Override
    public void start( final AsynchronousMessage<T> event ) {
        final T data = event.getEvent();
        /**
         * Execute all listeners in parallel
         */
        List<Observable<?>> observables = new ArrayList<Observable<?>>( listeners.size() );

        for ( MessageListener<T, ?> listener : listeners ) {
            observables.add( HystrixGraphObservable.async( listener.receive( data ) ).subscribeOn( Schedulers.io() ) );
        }

        LOG.debug( "About to start {} observables for event {}", listeners.size(), event );

        //run everything in parallel and zip it up
        Observable.zip( observables, new FuncN<AsynchronousMessage<T>>() {
            @Override
            public AsynchronousMessage<T> call( final Object... args ) {
                return event;
            }
        } ).subscribe( new Subscriber<AsynchronousMessage<T>>() {

            @Override
            public void onCompleted() {
                LOG.debug( "Successfully completed processing for event {}", event );
                queue.remove( event );

                for ( CompleteListener<T> listener : completeListeners ) {
                    listener.onComplete( event );
                }
            }


            @Override
            public void onError( final Throwable throwable ) {
                LOG.error( "Unable to process async event", throwable );

                for ( ErrorListener listener : errorListeners ) {
                    listener.onError( event, throwable );
                }
            }


            @Override
            public void onNext( final AsynchronousMessage<T> tAsynchronousMessage ) {
                //no op
            }
        } );
    }


    @Override
    public Collection<AsynchronousMessage<T>> getTimeouts( final int maxCount, final long timeout ) {
        return queue.take( maxCount, timeout );
    }




    @Override
    public <R> void addListener( final MessageListener<T, R> listener ) {
        this.listeners.add( listener );
    }


    /**
     * Add an error listeners
     */
    public void addErrorListener( ErrorListener<T> listener ) {
        this.errorListeners.add( listener );
    }


    @Override
    public void addCompleteListener( final CompleteListener<T> listener ) {
        this.completeListeners.add( listener );
    }
}
