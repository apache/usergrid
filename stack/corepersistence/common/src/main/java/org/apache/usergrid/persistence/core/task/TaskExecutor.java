package org.apache.usergrid.persistence.core.task;


import com.google.common.util.concurrent.ListenableFuture;


/**
 * An interface for execution of tasks
 */
public interface TaskExecutor {

    /**
     * Submit the task asynchronously
     * @param task
     */
    public <V> Task<V > submit( Task<V> task );

    /**
     * Stop the task executor without waiting for scheduled threads to run
     */
    public void shutdown();
}
