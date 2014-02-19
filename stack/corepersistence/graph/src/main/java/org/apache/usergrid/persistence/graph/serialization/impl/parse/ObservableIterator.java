package org.apache.usergrid.persistence.graph.serialization.impl.parse;


import java.util.Iterator;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;


/**
 * Converts an iterator to an observable.  Subclasses need to only implement getting the iterator from the data source.
 * This is used in favor of "Observable.just" when the initial fetch of the iterator will require I/O.  This allows
 * us to wrap the iterator in a deferred invocation to avoid the blocking on construction.
 */
public abstract class ObservableIterator<T> implements Observable.OnSubscribeFunc<T> {


    @Override
    public Subscription onSubscribe( final Observer<? super T> observer ) {


        try {
            //get our iterator and push data to the observer
            Iterator<T> itr = getIterator();


            //TODO T.N. when > 0.17 comes out, we need to implement the check with each loop as described here https://github.com/Netflix/RxJava/issues/802
            while ( itr.hasNext()) {
                observer.onNext( itr.next() );
            }

            observer.onCompleted();
        }

        //if any error occurs, we need to notify the observer so it can perform it's own error handling
        catch ( Throwable t ) {
            observer.onError( t );
        }

        return Subscriptions.empty();
    }




    /**
     * Return the iterator to feed data to
     */
    protected abstract Iterator<T> getIterator();
}
