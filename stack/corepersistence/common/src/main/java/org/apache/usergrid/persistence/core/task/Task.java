package org.apache.usergrid.persistence.core.task;


import java.util.concurrent.RecursiveTask;


/**
 * The task to execute
 */
public abstract class Task<V, I> extends RecursiveTask<V> {

    /**
     * Get the unique identifier of this task.  This may be used to collapse runnables over a time period in the future
     */
    public abstract I getId();


    @Override
    protected V compute() {
        try {
            return executeTask();
        }
        catch ( Exception e ) {
            exceptionThrown( e );
            throw new RuntimeException( e );
        }
    }


    /**
     * Execute the task
     */
    public abstract V executeTask() throws Exception;

    /**
     * Invoked when this task throws an uncaught exception.
     */
    public abstract void exceptionThrown( final Throwable throwable );

    /**
     * Invoked when we weren't able to run this task by the the thread attempting to schedule the task. If this task
     * MUST be run immediately, you can invoke the call method from within this event to invoke the task in the
     * scheduling thread.  Note that this has performance implications to the user.  If you can drop the request and
     * process later (lazy repair for instance ) do so.
     */
    public abstract void rejected();
}
