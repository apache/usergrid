package org.apache.usergrid.persistence.graph.consistency;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.graph.GraphFig;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;


/**
 * The implementation of asynchronous processing. This is intentionally kept as a 1 processor to 1 event type mapping
 * This way reflection is not used, event dispatching is easier, and has compile time checking
 */
@Singleton
public class AsyncProcessorImpl<T> implements AsyncProcessor<T> {

    /**
     * TODO, run this with hystrix
     */

    private static final Logger LOG = LoggerFactory.getLogger( AsyncProcessor.class );

    protected final TimeoutQueue<T> queue;
    protected final Scheduler scheduler;
    protected final GraphFig graphFig;
    protected final List<MessageListener<T, T>> listeners = new ArrayList<MessageListener<T, T>>();


    protected List<ErrorListener<T>> errorListeners = new ArrayList<ErrorListener<T>>();
    protected List<CompleteListener<T>> completeListeners = new ArrayList<CompleteListener<T>>();


    @Inject
    public AsyncProcessorImpl( final TimeoutQueue<T> queue, final Scheduler scheduler, final GraphFig graphFig ) {
        this.queue = queue;
        this.scheduler = scheduler;
        this.graphFig = graphFig;

        //we purposefully use a new thread.  We don't want to use one of the I/O threads to run this task
        //in the event the scheduler is full, we'll end up rejecting the reschedule of this task
        Schedulers.newThread().schedulePeriodically( new TimeoutTask<T>(this, graphFig), graphFig.getTaskLoopTime(),  graphFig.getTaskLoopTime(), TimeUnit.MILLISECONDS );
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

        for ( MessageListener<T, T> listener : listeners ) {
            observables.add( listener.receive( data ).subscribeOn( scheduler ) );
        }

        //run everything in parallel and zip it up
        Observable.zip( observables, new FuncN<AsynchronousMessage<T>>() {
            @Override
            public AsynchronousMessage<T> call( final Object... args ) {
                return event;
            }
        } ).doOnError( new Action1<Throwable>() {
            @Override
            public void call( final Throwable throwable ) {
                LOG.error( "Unable to process async event", throwable );

                for ( ErrorListener listener : errorListeners ) {
                    listener.onError( event, throwable );
                }
            }
        } ).doOnCompleted( new Action0() {
            @Override
            public void call() {
                queue.remove( event );

                for ( CompleteListener<T> listener : completeListeners ) {
                    listener.onComplete( event );
                }
            }
        } ).subscribe( new Action1<AsynchronousMessage<T>>() {
            @Override
            public void call( final AsynchronousMessage<T> asynchronousMessage ) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        } );
    }


    @Override
    public Collection<AsynchronousMessage<T>> getTimeouts( final int maxCount, final long timeout ) {
        return queue.take( maxCount, timeout );
    }


    @Override
    public void addListener( final MessageListener<T, T> listener ) {
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
