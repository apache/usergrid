package org.apache.usergrid.persistence.collection.rx;


import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.netflix.config.DynamicIntProperty;

import rx.Scheduler;
import rx.schedulers.Schedulers;


/**
 *
 *
 */
public class CassandraThreadScheduler implements Provider<Scheduler> {


    public static final String RX_IO_THREADS = "rx.cassandra.io.threads";


    private final DynamicIntProperty maxThreadCount;


    @Inject
    public CassandraThreadScheduler( @Named(RX_IO_THREADS) final DynamicIntProperty maxThreadCount ) {
        this.maxThreadCount = maxThreadCount;
    }


    @Override
    @Named( "cassandraScheduler" )
    public Scheduler get() {

        //create our thread factory so we can label our threads in case we need to dump them
        final ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread( final Runnable r ) {

                final AtomicLong counter = new AtomicLong();

                Thread t = new Thread( r, "RxCassandraIOThreadPool-" + counter.incrementAndGet() );
                t.setDaemon( true );
                return t;
            }
        };


        /**
         * Create a threadpool that will reclaim unused threads after 60 seconds.  It uses the max thread count set
         * here.
         * It intentionally uses the DynamicProperty, so that when it is updated, the listener updates the pool size
         * Additional allocation is trivial.  Shrinking the size will require all currently executing threads to run
         * to completion, without allowing additional tasks to be queued.
         *
         */
        final ThreadPoolExecutor pool = new ThreadPoolExecutor( 0, maxThreadCount.get(), 60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), factory, new ThreadPoolExecutor.AbortPolicy() );


        //if our max thread count is updated, we want to immediately update the pool.  Per the javadoc
        //if the size is smaller, existing threads will continue to run until they become idle and time out
        maxThreadCount.addCallback( new Runnable() {

            @Override
            public void run() {
                pool.setMaximumPoolSize( maxThreadCount.get() );
            }
        } );

        return Schedulers.executor( pool );
    }
}
