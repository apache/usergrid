package org.apache.usergrid.persistence.core.task;


import java.util.concurrent.Callable;



/**
 * The task to execute
 */
public interface Task<V, I> extends Callable<V> {

    /**
     * Get the unique identifier of this task.  This may be used to collapse runnables over a time period in the future
     *
     * @return
     */
    I getId();

    /**
     * Invoked when this task throws an uncaught exception.
     * @param throwable
     */
    void exceptionThrown(final Throwable throwable);

    /**
     * Invoked when we weren't able to run this task by the the thread attempting to schedule the task.
     * If this task MUST be run immediately, you can invoke the call method from within this event to invoke the
     * task in the scheduling thread.  Note that this has performance implications to the user.  If you can drop the
     * request and process later (lazy repair for instance ) do so.
     *
     */
    void rejected();



}
