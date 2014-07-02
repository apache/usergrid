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
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.usergrid.persistence.core.rx.OrderedMerge;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.google.common.base.Preconditions;

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


    private InnerIterator<T> iterator;


    public MultiKeyColumnNameIterator( final Collection<ColumnNameIterator<C, T>> columnNameIterators,
                                       final Comparator<T> comparator, final int bufferSize ) {


        Observable<T>[] observables = new Observable[columnNameIterators.size()];

        int i = 0;

        for ( ColumnNameIterator<C, T> columnNameIterator : columnNameIterators ) {

            observables[i] = Observable.from( columnNameIterator, Schedulers.io() );

            i++;
        }


        //merge them into 1 observable, and remove duplicates from the stream
        Observable<T> merged = OrderedMerge.orderedMerge( comparator, bufferSize, observables ).distinctUntilChanged();


        iterator = new InnerIterator(bufferSize);

        merged.subscribe( iterator );
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

        private CountDownLatch startLatch = new CountDownLatch( 1 );

        private final LinkedBlockingQueue<T> queue;


        private Throwable error;
        private boolean done = false;

        private T next;


        private InnerIterator( int maxSize ) {
            queue = new LinkedBlockingQueue<>( maxSize );
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
                queue.put( t );
            }
            catch ( InterruptedException e ) {
                throw new RuntimeException( "Unable to take from queue" );
            }

            startLatch.countDown();
        }
    }
}
