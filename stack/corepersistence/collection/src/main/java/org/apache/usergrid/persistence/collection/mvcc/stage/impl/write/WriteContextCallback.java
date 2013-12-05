package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.connectionpool.OperationResult;


/**
 * Helper class to cause the async execution to continue
 * Not used ATM, just here for demonstration purposes with async astynax invocation on phase proceed
 *
 * @author tnine
 */
public class WriteContextCallback implements FutureCallback<OperationResult<Void>> {

    private final ExecutionContext context;


    /** Create a new callback.  The data will be passed to the next stage */
    private WriteContextCallback( final ExecutionContext context ) {
        this.context = context;
    }


    public void onSuccess( final OperationResult<Void> result ) {

        /**
         * Proceed to the next stage
         */
        context.proceed();
    }


    @Override
    public void onFailure( final Throwable t ) {
//        context.stop();
        throw new CollectionRuntimeException( "Failed to execute write", t );
    }


    /**
     * This encapsulated type of Void in the listenable future is intentional.  If you're not returning void in your
     * future, you shouldn't be using this callback, you should be using a callback that will set the Response value
     * into the next stage and invoke it
     *
     * @param future The listenable future returned by the Astyanax async op
     * @param context The context to signal to continue in the callback
     */
    public static void createCallback( final ListenableFuture<OperationResult<Void>> future,
                                       final ExecutionContext context ) {

        Futures.addCallback( future, new WriteContextCallback( context ) );
    }
}
