package org.apache.usergrid.persistence.core.task;


/**
 * Does not perform computation, just returns the value passed to it
 *
 */
public class ImmediateTask<V> extends Task<V> {


    private final V returned;


    protected ImmediateTask( final V returned ) {
        this.returned = returned;
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
