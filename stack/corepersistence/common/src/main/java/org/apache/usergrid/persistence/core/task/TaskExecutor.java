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
    public <V, I> Task<V, I > submit( Task<V, I> task );
}
