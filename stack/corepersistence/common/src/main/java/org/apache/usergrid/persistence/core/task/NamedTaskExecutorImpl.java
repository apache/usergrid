package org.apache.usergrid.persistence.core.task;


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    private final String name;
    private final int poolSize;


    /**
     * @param name The name of this instance of the task executor
     * @param poolSize The size of the pool.  This is the number of concurrent tasks that can execute at once.
     * @param queueLength The length of tasks to keep in the queue
     */
    public NamedTaskExecutorImpl( final String name, final int poolSize, final int queueLength ) {
        Preconditions.checkNotNull( name );
        Preconditions.checkArgument( name.length() > 0, "name must have a length" );
        Preconditions.checkArgument( poolSize > -1, "poolSize must be > than -1" );
        Preconditions.checkArgument( queueLength > -1, "queueLength must be 0 or more" );

        this.name = name;
        this.poolSize = poolSize;

        //The user has chosen to disable asynchronous execution, to create an executor service that will reject all requests
        if(poolSize == 0){
            executorService = MoreExecutors.listeningDecorator( new RejectingExecutorService());
        }

        //queue executions as normal
        else {
            final BlockingQueue<Runnable> queue =
                    queueLength > 0 ? new ArrayBlockingQueue<Runnable>( queueLength ) : new SynchronousQueue<Runnable>();

            executorService = MoreExecutors.listeningDecorator( new MaxSizeThreadPool( queue ) );
        }
    }


    @Override
    public <V> ListenableFuture<V> submit( final Task<V> task ) {

        final ListenableFuture<V> future;

        try {
            future = executorService.submit( task );

            /**
             * Log our success or failures for debugging purposes
             */
            Futures.addCallback( future, new TaskFutureCallBack<V>( task ) );
        }
        catch ( RejectedExecutionException ree ) {
            return Futures.immediateFuture( task.rejected());
        }

        return future;
    }


    @Override
    public void shutdown() {
        this.executorService.shutdownNow();
    }


    /**
     * Callback for when the task succeeds or fails.
     */
    private static final class TaskFutureCallBack<V> implements FutureCallback<V> {

        private final Task<V> task;


        private TaskFutureCallBack( Task<V> task ) {
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
    private final class MaxSizeThreadPool extends ThreadPoolExecutor {

        public MaxSizeThreadPool( BlockingQueue<Runnable> queue ) {

            super( 1, poolSize, 30, TimeUnit.SECONDS, queue, new CountingThreadFactory( ),
                    new RejectedHandler() );
        }
    }


    /**
     * Thread factory that will name and count threads for easier debugging
     */
    private final class CountingThreadFactory implements ThreadFactory {

        private final AtomicLong threadCounter = new AtomicLong();


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
    private final class RejectedHandler implements RejectedExecutionHandler {


        @Override
        public void rejectedExecution( final Runnable r, final ThreadPoolExecutor executor ) {
            LOG.warn( "{} task queue full, rejecting task {}", name, r );

            throw new RejectedExecutionException( "Unable to run task, queue full" );
        }

    }


    /**
     * Executor implementation that simply rejects all incoming tasks
     */
    private static final class RejectingExecutorService implements ExecutorService{

        @Override
        public void shutdown() {

        }


        @Override
        public List<Runnable> shutdownNow() {
            return Collections.EMPTY_LIST;
        }


        @Override
        public boolean isShutdown() {
            return false;
        }


        @Override
        public boolean isTerminated() {
            return false;
        }


        @Override
        public boolean awaitTermination( final long timeout, final TimeUnit unit ) throws InterruptedException {
            return false;
        }


        @Override
        public <T> Future<T> submit( final Callable<T> task ) {
            throw new RejectedExecutionException("No Asynchronous tasks allowed");
        }


        @Override
        public <T> Future<T> submit( final Runnable task, final T result ) {
            throw new RejectedExecutionException("No Asynchronous tasks allowed");
        }


        @Override
        public Future<?> submit( final Runnable task ) {
            throw new RejectedExecutionException("No Asynchronous tasks allowed");
        }


        @Override
        public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> tasks )
                throws InterruptedException {
            throw new RejectedExecutionException("No Asynchronous tasks allowed");
        }


        @Override
        public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> tasks, final long timeout,
                                              final TimeUnit unit ) throws InterruptedException {
            throw new RejectedExecutionException("No Asynchronous tasks allowed");
        }


        @Override
        public <T> T invokeAny( final Collection<? extends Callable<T>> tasks )
                throws InterruptedException, ExecutionException {
            throw new RejectedExecutionException("No Asynchronous tasks allowed");
        }


        @Override
        public <T> T invokeAny( final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit )
                throws InterruptedException, ExecutionException, TimeoutException {
            throw new RejectedExecutionException("No Asynchronous tasks allowed");
        }


        @Override
        public void execute( final Runnable command ) {
            throw new RejectedExecutionException("No Asynchronous tasks allowed");
        }
    }
}
