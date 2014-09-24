package org.apache.usergrid.persistence.core.task;


/**
 * An interface for execution of tasks
 */
public interface TaskExecutor {

    /**
     * Submit the task asynchronously
     * @param task
     */
    public <V, I> com.google.common.util.concurrent.ListenableFuture<V> submit( Task<V, I> task );
}
