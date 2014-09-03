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
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.hystrix.HystrixCassandra;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.RowSliceQuery;
import com.netflix.astyanax.util.RangeBuilder;


/**
 *
 *
 */
public class MultiRowColumnIterator<R, C, T> implements Iterator<T> {

    private static final Logger LOG = LoggerFactory.getLogger( MultiRowColumnIterator.class );

    private final int pageSize;

    private final ColumnFamily<R, C> cf;


    private final ColumnParser<C, T> columnParser;

    private final ColumnSearch<T> columnSearch;

    private final Comparator<T> comparator;


    private final Collection<R> rowKeys;

    private final Keyspace keyspace;

    private final ConsistencyLevel consistencyLevel;


    private T startColumn;

    private boolean moreToFollow;

    private Iterator<T> currentColumnIterator;


    /**
     * Remove after finding bug
     */


    //    private int advanceCount;
    //
    //    private final HashMap<T, SeekPosition> seenResults;

    /**
     * Complete Remove
     */


    /**
     * Create the iterator
     */
    public MultiRowColumnIterator( final Keyspace keyspace, final ColumnFamily<R, C> cf,
                                   final ConsistencyLevel consistencyLevel, final ColumnParser<C, T> columnParser,
                                   final ColumnSearch<T> columnSearch, final Comparator<T> comparator,
                                   final Collection<R> rowKeys, final int pageSize ) {
        this.cf = cf;
        this.pageSize = pageSize;
        this.columnParser = columnParser;
        this.columnSearch = columnSearch;
        this.comparator = comparator;
        this.rowKeys = rowKeys;
        this.keyspace = keyspace;
        this.consistencyLevel = consistencyLevel;
        this.moreToFollow = false;

        //        seenResults = new HashMap<>( pageSize * 10 );
    }


    @Override
    public boolean hasNext() {

        if ( currentColumnIterator == null || ( !currentColumnIterator.hasNext() && moreToFollow ) ) {
            advance();
        }


        return currentColumnIterator.hasNext();
    }


    @Override
    public T next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "No new element exists" );
        }

        final T next = currentColumnIterator.next();

        //        LOG.trace( "Emitting {}", next );

        return next;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported this is a read only iterator" );
    }


    public void advance() {


        /**
         * If the edge is present, we need to being seeking from this
         */


        //TODO, finalize why this isn't working as expected
        final int selectSize = startColumn == null ? pageSize : pageSize + 1;

        final RangeBuilder rangeBuilder = new RangeBuilder();


        //set the range into the search

        if ( startColumn == null ) {
            columnSearch.buildRange( rangeBuilder );
        }
        else {
            columnSearch.buildRange( rangeBuilder, startColumn );
        }


        rangeBuilder.setLimit( selectSize );


        /**
         * Get our list of slices
         */
        final RowSliceQuery<R, C> query =
                keyspace.prepareQuery( cf ).setConsistencyLevel( consistencyLevel ).getKeySlice( rowKeys )
                        .withColumnRange( rangeBuilder.build() );

        final Rows<R, C> result = HystrixCassandra.user( query ).getResult();

        final TreeSet<T> mergedResults = new TreeSet<>( comparator );


        //now aggregate them together

        for ( final R key : result.getKeys() ) {
            final ColumnList<C> columns = result.getRow( key ).getColumns();
            final int size = columns.size();

            int readIndex = 0;

            //skip the first since it's equal and has been set
            if ( startColumn != null && size > 0 ) {
                final T returnedValue = columnParser.parseColumn( columns.getColumnByIndex( 0 ) );

                if ( comparator.compare( returnedValue, startColumn ) == 0 ) {
                    readIndex++;
                }
            }


//            T previous = null;

            for (; readIndex < size; readIndex++ ) {
                final Column<C> column = columns.getColumnByIndex( readIndex );
                final T returnedValue = columnParser.parseColumn( column );

                /**
                 * DO NOT remove this section of code. If you're seeing inconsistent results during shard transition, you'll
                 * need to enable this
                 */
//
//                if ( previous != null && comparator.compare( previous, returnedValue ) == 0 ) {
//                    throw new RuntimeException( String.format(
//                            "Cassandra returned 2 unique columns, but your comparator marked them as equal.  This " +
//                                    "indicates a bug in your comparator.  Previous value was %s and current value is " +
//                                    "%s",
//                            previous, returnedValue ) );
//                }
//
//                previous = returnedValue;

                mergedResults.add( returnedValue );

                //prune the mergedResults
                while ( mergedResults.size() > pageSize ) {
                    mergedResults.pollLast();
                }
            }

            LOG.trace( "Read {} columns from row key {}", readIndex, key );
            LOG.trace( "Candidate result set size is {}", mergedResults.size() );
        }


        //we've parsed everything truncate to the first pageSize, it's all we can ensure is correct without another
        //trip back to cassandra

        if(!mergedResults.isEmpty()) {
            startColumn = mergedResults.last();
        }

        moreToFollow = mergedResults.size() == pageSize;

        currentColumnIterator = mergedResults.iterator();

        LOG.trace( "Finished parsing {} rows for a total of {} results", rowKeys.size(), mergedResults.size() );
    }
}


