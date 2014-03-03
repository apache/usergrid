package org.apache.usergrid.persistence.graph.consistency;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.hystrix.HystrixCommandGroupKey;

import rx.Observable;
import rx.Scheduler;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.FuncN;


/**
 * The implementation of asynchronous processing
 */
@Singleton
public class AsyncProcessorImpl<T> implements AsyncProcessor<T> {

    private static final HystrixCommandGroupKey GRAPH_REPAIR = HystrixCommandGroupKey.Factory.asKey( "Graph_Repair" );

    private final TimeoutQueue<T> queue;
    private final Scheduler scheduler;
    private final List<MessageListener<T, T>> listeners = new ArrayList<MessageListener<T, T>>();

    private static final Logger LOG = LoggerFactory.getLogger( AsyncProcessor.class );

    private List<ErrorListener> errorListeners = new ArrayList<ErrorListener>();


    @Inject
    public AsyncProcessorImpl( final TimeoutQueue<T> queue, final Scheduler scheduler ) {
        this.queue = queue;
        this.scheduler = scheduler;
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
            }
        } ).subscribe( new Action1<AsynchronousMessage<T>>() {
            @Override
            public void call( final AsynchronousMessage<T> tAsynchronousMessage ) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        } );
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
}
