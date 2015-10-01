/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.core.astyanax;


import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.rx.OrderedMerge;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;


/**
 * Simple iterator that wraps a collection of ColumnNameIterators.  We do this because we can't page with a
 * multiRangeScan correctly for multiple round trips.  As a result, we do this since only 1 iterator with minimum values
 * could potentially feed the entire result set.
 *
 * Compares the parsed values and puts them in order. If more than one row key emits the same value the first value is
 * selected, and ignored from subsequent iterators.
 */
public class MultiKeyColumnNameIterator<C, T> implements Iterable<T>, Iterator<T> {


    private static final Logger LOG = LoggerFactory.getLogger( MultiKeyColumnNameIterator.class );

    private Iterator<T> iterator;


    public MultiKeyColumnNameIterator( final Collection<ColumnNameIterator<C, T>> columnNameIterators,
                                       final Comparator<T> comparator, final int bufferSize ) {


        //optimization for single use case
        if ( columnNameIterators.size() == 1 ) {
            iterator = columnNameIterators.iterator().next();
            return;
        }


        /**
         * We have more than 1 iterator, subscribe to all of them on their own thread so they can
         * produce in parallel.  This way our inner iterator will be filled and processed the fastest
         */
        Observable<T>[] observables = new Observable[columnNameIterators.size()];

        int i = 0;

        for ( ColumnNameIterator<C, T> columnNameIterator : columnNameIterators ) {



            observables[i] = Observable.from( columnNameIterator ).subscribeOn( Schedulers.io() );

            i++;
        }


        //merge them into 1 observable, and remove duplicates from the stream
        Observable<T> merged = OrderedMerge.orderedMerge( comparator, bufferSize, observables ).distinctUntilChanged();


        InnerIterator innerIterator = new InnerIterator( bufferSize );

        merged.subscribe( innerIterator );

        iterator = innerIterator;
    }


    @Override
    public Iterator<T> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }


    @Override
    public T next() {
        return iterator.next();
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "You cannot remove elements from a merged iterator, it is read only" );
    }


    /**
     * Internal iterator that will put next elements into a blocking queue until it reaches capacity. At this point it
     * will block then emitting thread until more elements are taken.  Assumed the Observable is run on a I/O thread,
     * NOT the current thread.
     */
    private final class InnerIterator<T> extends Subscriber<T> implements Iterator<T> {

        private final CountDownLatch startLatch = new CountDownLatch( 1 );

        /**
         * Use an ArrayBlockingQueue for faster access since our upper bounds is static
         */
        private final ArrayBlockingQueue<T> queue;


        private Throwable error;
        private boolean done = false;

        private T next;


        private InnerIterator( int maxSize ) {
            queue = new ArrayBlockingQueue<>( maxSize );
        }


        @Override
        public boolean hasNext() {


            //we're done
            if ( next != null ) {
                return true;
            }


            try {
                startLatch.await();
            }
            catch ( InterruptedException e ) {
                throw new RuntimeException( "Unable to wait for start of submission" );
            }


            //this is almost a busy wait, and is intentional, if we have nothing to poll, we want to get it as soon
            //as it's available.  We generally only hit this once
            do {
                next = queue.poll();
            }
            while ( next == null && !done );


            return next != null;
        }


        @Override
        public T next() {

            if ( error != null ) {
                throw new RuntimeException( "An error occurred when populating the iterator", error );
            }

            if ( !hasNext() ) {
                throw new NoSuchElementException( "No more elements are present" );
            }


            T toReturn = next;
            next = null;
            return toReturn;
        }


        @Override
        public void remove() {
            throw new UnsupportedOptionException( "Remove is unsupported" );
        }


        @Override
        public void onCompleted() {
            done = true;
            startLatch.countDown();
        }


        @Override
        public void onError( final Throwable e ) {
            error = e;
            done = true;
            startLatch.countDown();
        }


        @Override
        public void onNext( final T t ) {

            //may block if we get full, that's expected behavior

            try {
                LOG.trace( "Received element {}" , t );
                queue.put( t );
            }
            catch ( InterruptedException e ) {
                throw new RuntimeException( "Unable to insert to queue" );
            }

            startLatch.countDown();
        }
    }
}
