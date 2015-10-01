/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.util.Iterator;


/**
 * Converts an iterator to an observable.  Subclasses need to only implement getting the iterator from the data source.
 * This is used in favor of "Observable.just" when the initial fetch of the iterator will require I/O.  This allows us
 * to wrap the iterator in a deferred invocation to avoid the blocking on construction.
 */
public abstract class ObservableIterator<T> implements Observable.OnSubscribe<T> {

    private static final Logger log = LoggerFactory.getLogger(ObservableIterator.class);

    private final String name;


    /**
     * @param name The simple name of the iterator, used for debugging
     */
    protected ObservableIterator( final String name ) {this.name = name;}


    @Override
    public void call( final Subscriber<? super T> subscriber ) {


        try {
            //get our iterator and push data to the observer
            final Iterator<T> itr = getIterator();

            Preconditions.checkNotNull(itr,
                    "The observable must return an iterator.  Null was returned for iterator " + name);


            //while we have items to emit and our subscriber is subscribed, we want to keep emitting items
            while ( itr.hasNext() && !subscriber.isUnsubscribed() ) {
                final T next = itr.next();

                log.trace( "Iterator '{}' emitting item '{}'", name, next );

                subscriber.onNext( next );
            }


            subscriber.onCompleted();
        }

        //if any error occurs, we need to notify the observer so it can perform it's own error handling
        catch ( Throwable t ) {
            log.error( "Unable to emit items from iterator {}", name, t );
            subscriber.onError( t );
        }
    }


    /**
     * Return the iterator to feed data to
     */
    protected abstract Iterator<T> getIterator();
}
