package org.apache.usergrid.persistence.graph.consistency;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import rx.Observer;
import rx.Scheduler;


/**
 * The implementation of asynchronous processing
 */
@Singleton
public class AsyncProcessorImpl implements AsyncProcessor {

    private static final HystrixCommandGroupKey GRAPH_REPAIR = HystrixCommandGroupKey.Factory.asKey( "Graph_Repair" );

    private final EventBus bus;
    private final TimeoutQueue queue;
    private final Scheduler scheduler;

    private static final Logger LOG = LoggerFactory.getLogger( AsyncProcessor.class );

    private List<ErrorListener> listeners = new ArrayList<ErrorListener>();


    @Inject
    public AsyncProcessorImpl( final EventBus bus, final TimeoutQueue queue, final Scheduler scheduler ) {
        this.bus = bus;
        this.queue = queue;
        this.scheduler = scheduler;
    }


    @Override
    public <T> TimeoutEvent<T> setVerification( final T event, final long timeout ) {
        return queue.queue( event, timeout );
    }


    @Override
    public <T> void start( final TimeoutEvent<T> event ) {


        //run this in a timeout command so it doesn't run forever. If it times out, it will simply resume later
        new HystrixCommand<Void>( GRAPH_REPAIR ) {

            @Override
            protected Void run() throws Exception {
                final T busEvent = event.getEvent();
                bus.post( busEvent );
                return null;
            }
        }.toObservable( scheduler ).subscribe( new Observer<Void>() {
            @Override
            public void onCompleted() {
                queue.remove( event );
            }


            @Override
            public void onError( final Throwable throwable ) {
                LOG.error( "Unable to process async event", throwable );

                for ( ErrorListener listener : listeners ) {
                    listener.onError( event, throwable );
                }
            }


            @Override
            public void onNext( final Void args ) {
                //nothing to do here
                System.out.print( "next" );
                //To change body of implemented methods use File | Settings | File Templates.
            }
        } );

        //                new Action1<Void>() {
        //                                                   @Override
        //                                                   public void call( final Void timeoutEvent ) {
        //
        //                                                   }
        //                                               }, new Action1<Throwable>() {
        //                                                   @Override
        //                                                   public void call( final Throwable throwable ) {
        //
        //                                                   }
        //                                               }
        //                                             );
    }


    /**
     * Add an error listener
     */
    public void addListener( ErrorListener listener ) {
        this.listeners.add( listener );
    }


    /**
     * Internal listener for errors, really only used for testing.  Can be used to hook into error state
     */
    public static interface ErrorListener {

        public <T> void onError( TimeoutEvent<T> event, Throwable t );
    }
}
