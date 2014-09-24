package org.apache.usergrid.persistence.core.task;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;


/**
 * Implementation of the task executor with a unique name and size
 */
public class NamedTaskExecutorImpl implements TaskExecutor {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( NamedTaskExecutorImpl.class );

    private final ListeningExecutorService executorService;


    /**
     * @param name The name of this instance of the task executor
     * @param poolSize The size of the pool.  This is the number of concurrent tasks that can execute at once.
     * @param queueLength The length of tasks to keep in the queue
     */
    public NamedTaskExecutorImpl( final String name, final int poolSize, final int queueLength ) {
        Preconditions.checkNotNull( name );
        Preconditions.checkArgument( name.length() > 0, "name must have a length" );
        Preconditions.checkArgument( poolSize > 0, "poolSize must be > than 0" );
        Preconditions.checkArgument( queueLength > -1, "queueLength must be 0 or more" );


        final BlockingQueue<Runnable> queue =
                queueLength > 0 ? new ArrayBlockingQueue<Runnable>( queueLength ) : new SynchronousQueue<Runnable>();

        executorService = MoreExecutors.listeningDecorator( new MaxSizeThreadPool( name, poolSize, queue ) );
    }


    @Override
    public <V, I> ListenableFuture<V> submit( final Task<V, I> task ) {

        final ListenableFuture<V> future;

        try {
            future = executorService.submit( task );

            /**
             * Log our success or failures for debugging purposes
             */
            Futures.addCallback( future, new TaskFutureCallBack<V, I>( task ) );
        }
        catch ( RejectedExecutionException ree ) {
            task.rejected();
            return Futures.immediateCancelledFuture();
        }

        return future;
    }


    /**
     * Callback for when the task succeeds or fails.
     */
    private static final class TaskFutureCallBack<V, I> implements FutureCallback<V> {

        private final Task<V, I> task;


        private TaskFutureCallBack( Task<V, I> task ) {
            this.task = task;
        }


        @Override
        public void onSuccess( @Nullable final V result ) {
            LOG.debug( "Successfully completed task ", task );
        }


        @Override
        public void onFailure( final Throwable t ) {
            LOG.error( "Unable to execute task.  Exception is ", t );

            task.exceptionThrown( t );
        }
    }


    /**
     * Create a thread pool that will reject work if our audit tasks become overwhelmed
     */
    private static final class MaxSizeThreadPool extends ThreadPoolExecutor {

        public MaxSizeThreadPool( final String name, final int workerSize, BlockingQueue<Runnable> queue ) {

            super( 1, workerSize, 30, TimeUnit.SECONDS, queue, new CountingThreadFactory( name ),
                    new RejectedHandler() );
        }
    }


    /**
     * Thread factory that will name and count threads for easier debugging
     */
    private static final class CountingThreadFactory implements ThreadFactory {

        private final AtomicLong threadCounter = new AtomicLong();

        private final String name;


        private CountingThreadFactory( final String name ) {this.name = name;}


        @Override
        public Thread newThread( final Runnable r ) {
            final long newValue = threadCounter.incrementAndGet();

            Thread t = new Thread( r, name + "-" + newValue );

            t.setDaemon( true );

            return t;
        }
    }


    /**
     * The handler that will handle rejected executions and signal the interface
     */
    private static final class RejectedHandler implements RejectedExecutionHandler {


        @Override
        public void rejectedExecution( final Runnable r, final ThreadPoolExecutor executor ) {
            LOG.warn( "Audit queue full, rejecting audit task {}", r );

            throw new RejectedExecutionException( "Unable to run task, queue full" );
        }

    }
}
