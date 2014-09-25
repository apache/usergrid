package org.apache.usergrid.persistence.core.task;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;


/**
 * Implementation of the task executor with a unique name and size
 */
public class NamedTaskExecutorImpl implements TaskExecutor {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( NamedTaskExecutorImpl.class );

    private final NamedForkJoinPool executorService;

    private final String name;
    private final int poolSize;


    /**
     * @param name The name of this instance of the task executor
     * @param poolSize The size of the pool.  This is the number of concurrent tasks that can execute at once.
     */
    public NamedTaskExecutorImpl( final String name, final int poolSize) {
        Preconditions.checkNotNull( name );
        Preconditions.checkArgument( name.length() > 0, "name must have a length" );
        Preconditions.checkArgument( poolSize > 0, "poolSize must be > than 0" );

        this.name = name;
        this.poolSize = poolSize;

//        final BlockingQueue<Runnable> queue =
//                queueLength > 0 ? new ArrayBlockingQueue<Runnable>( queueLength ) : new SynchronousQueue<Runnable>();
//
//        executorService = MoreExecutors.listeningDecorator( new MaxSizeThreadPool( queue ) );

       this.executorService =  new NamedForkJoinPool(poolSize);
    }


    @Override
    public <V, I> Task<V, I> submit( final Task<V, I> task ) {

        try {
           executorService.submit( task );
        }
        catch ( RejectedExecutionException ree ) {
            task.rejected();
        }

        return task;

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


    private final class NamedForkJoinPool extends ForkJoinPool{

        private NamedForkJoinPool( final int workerThreadCount ) {
            //TODO, verify the scheduler at the end
            super( workerThreadCount, defaultForkJoinWorkerThreadFactory, new TaskExceptionHandler(), true );
        }



    }

    private final class TaskExceptionHandler implements Thread.UncaughtExceptionHandler{

        @Override
        public void uncaughtException( final Thread t, final Throwable e ) {
            LOG.error( "Uncaught exception on thread {} was {}", t, e );
        }
    }




    private final class NamedWorkerThread extends ForkJoinWorkerThread{

        /**
         * Creates a ForkJoinWorkerThread operating in the given pool.
         *
         * @param pool the pool this thread works in
         *
         * @throws NullPointerException if pool is null
         */
        protected NamedWorkerThread(final String name,  final ForkJoinPool pool ) {
            super( pool );
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
}
