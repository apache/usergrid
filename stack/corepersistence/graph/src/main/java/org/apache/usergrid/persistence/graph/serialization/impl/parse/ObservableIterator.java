package org.apache.usergrid.persistence.graph.serialization.impl.parse;


import java.util.Iterator;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

import rx.Observable;
import rx.Subscriber;


/**
 * Converts an iterator to an observable.  Subclasses need to only implement getting the iterator from the data source.
 * This is used in favor of "Observable.just" when the initial fetch of the iterator will require I/O.  This allows us
 * to wrap the iterator in a deferred invocation to avoid the blocking on construction.
 */
public abstract class ObservableIterator<T> implements Observable.OnSubscribe<T> {

    private static final HystrixCommandGroupKey GROUP_KEY = HystrixCommandGroupKey.Factory.asKey( "CassRead" );

    private final int executionTimeout;


    protected ObservableIterator( final int executionTimeout ) {
        this.executionTimeout = executionTimeout;
    }


    @Override
    public void call( final Subscriber<? super T> subscriber ) {


        try {
            //run producing the values within a hystrix command.  This way we'll time out if the read takes too long
            new HystrixCommand<Void>( HystrixCommand.Setter.withGroupKey( GROUP_KEY ).andCommandPropertiesDefaults(
                    HystrixCommandProperties.Setter()
                                            .withExecutionIsolationThreadTimeoutInMilliseconds( executionTimeout ) ) ) {


                @Override
                protected Void run() throws Exception {
                    //get our iterator and push data to the observer
                    final Iterator<T> itr = getIterator();


                    //while we have items to emit and our subscriber is subscribed, we want to keep emitting items
                    while ( itr.hasNext() && !subscriber.isUnsubscribed() ) {
                        subscriber.onNext( itr.next() );
                    }


                    subscriber.onCompleted();

                    return null;
                }
            }.execute();
        }

        //if any error occurs, we need to notify the observer so it can perform it's own error handling
        catch ( Throwable t ) {
            subscriber.onError( t );
        }
    }


    /**
     * Return the iterator to feed data to
     */
    protected abstract Iterator<T> getIterator();
}
