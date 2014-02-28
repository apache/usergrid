package org.apache.usergrid.persistence.graph.consistency;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class AsyncProcessorImpl<T> implements AsyncProcessor<T> {

    private static final HystrixCommandGroupKey GRAPH_REPAIR = HystrixCommandGroupKey.Factory.asKey( "Graph_Repair" );

    private final TimeoutQueue<T> queue;
    private final Scheduler scheduler;
    private final List<AsynchronousEventListener<T>> listeners = new ArrayList<AsynchronousEventListener<T>>(  );

    private static final Logger LOG = LoggerFactory.getLogger( AsyncProcessor.class );

    private List<ErrorListener> errorListeners = new ArrayList<ErrorListener>();


    @Inject
    public AsyncProcessorImpl( final TimeoutQueue<T> queue, final Scheduler scheduler ) {
        this.queue = queue;
        this.scheduler = scheduler;
    }



    @Override
    public AsynchronousEvent<T> setVerification( final T event, final long timeout ) {
        return queue.queue( event, timeout );
    }




    @Override
    public void start( final AsynchronousEvent<T> event ) {


        //run this in a timeout command so it doesn't run forever. If it times out, it will simply resume later
        new HystrixCommand<Void>( GRAPH_REPAIR ) {

            @Override
            protected Void run() throws Exception {
                final T rootEvent = event.getEvent();

                for(AsynchronousEventListener<T> listener: listeners){
                    listener.receive( rootEvent );
                }

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

                for ( ErrorListener listener : errorListeners ) {
                    listener.onError( event, throwable );
                }
            }


            @Override
            public void onNext( final Void args ) {
                //nothing to do here
            }
        } );

    }


    @Override
    public void addListener( final AsynchronousEventListener<T> listener ) {
        this.listeners.add( listener );
    }


    /**
     * Add an error listeners
     */
    public void addErrorListener( ErrorListener<T> listener ) {
        this.errorListeners.add( listener );
    }



}
