package org.apache.usergrid.persistence.core.task;


/**
 * Does not perform computation, just returns the value passed to it
 *
 */
public class ImmediateTask<V, I> extends Task<V, I> {

    private final I id;
    private final V returned;


    protected ImmediateTask( final I id, final V returned ) {
        this.id = id;
        this.returned = returned;
    }


    @Override
    public I getId() {
        return id;
    }


    @Override
    public V executeTask() throws Exception {
        return returned;
    }


    @Override
    public void exceptionThrown( final Throwable throwable ) {
          //no op
    }


    @Override
    public void rejected() {
        //no op
    }
}
