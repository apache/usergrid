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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
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


    private boolean moreToReturn;


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
        this.moreToReturn = true;

        //        seenResults = new HashMap<>( pageSize * 10 );
    }


    @Override
    public boolean hasNext() {

        if ( currentColumnIterator == null || ( !currentColumnIterator.hasNext() && moreToReturn ) ) {
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

        final boolean skipFirstColumn = startColumn != null;



        //TODO, finalize why this isn't working as expected
        final int selectSize = skipFirstColumn ? pageSize + 1 : pageSize;

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

        final Rows<R, C> result;
        try {
            result = query.execute().getResult();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to casandra", e );
        }


        //now aggregate them together

        //this is an optimization.  It's faster to see if we only have values for one row,
        // then return the iterator of those columns than
        //do a merge if only one row has data.


        final List<T> mergedResults;

        if ( containsSingleRowOnly( result ) ) {
            mergedResults = singleRowResult( result );
        }
        else {
            mergedResults = mergeResults( result, selectSize );
        }





        //we've parsed everything truncate to the first pageSize, it's all we can ensure is correct without another
        //trip back to cassandra

        //discard our first element (maybe)



        final int size = mergedResults.size();

        moreToReturn = size == selectSize;

        //we have a first column to to check
        if( size > 0) {

            final T firstResult = mergedResults.get( 0 );

            //The search has either told us to skip the first element, or it matches our last, therefore we disregard it
            if(columnSearch.skipFirst( firstResult ) || (skipFirstColumn && comparator.compare( startColumn, firstResult ) == 0)){
                mergedResults.remove( 0 );
            }

        }


        if(moreToReturn && mergedResults.size() > 0){
            startColumn = mergedResults.get( mergedResults.size()  - 1 );
        }


        currentColumnIterator = mergedResults.iterator();

        LOG.trace( "Finished parsing {} rows for results", rowKeys.size() );
    }


    /**
     * Return true if we have < 2 rows with columns, false otherwise
     */
    private boolean containsSingleRowOnly( final Rows<R, C> result ) {

        int count = 0;

        for ( R key : result.getKeys() ) {
            if ( result.getRow( key ).getColumns().size() > 0 ) {
                count++;

                //we have more than 1 row with values, return them
                if ( count > 1 ) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * A single row is present, only parse the single row
     * @param result
     * @return
     */
    private List<T> singleRowResult( final Rows<R, C> result ) {


        for ( R key : result.getKeys() ) {
            final ColumnList<C> columnList = result.getRow( key ).getColumns();

            final int size = columnList.size();

            if ( size > 0 ) {

                final List<T> results = new ArrayList<>(size);

                for(Column<C> column: columnList){
                    results.add(columnParser.parseColumn( column ));
                }

                return results;


            }
        }

        //we didn't have any results, just return nothing
        return Collections.<T>emptyList();
    }


    /**
     * Multiple rows are present, merge them into a single result set
     * @param result
     * @return
     */
    private List<T> mergeResults( final Rows<R, C> result, final int maxSize ) {


        final List<T> mergedResults = new ArrayList<>(maxSize);




        for ( final R key : result.getKeys() ) {
            final ColumnList<C> columns = result.getRow( key ).getColumns();


            for (final Column<C> column :columns  ) {

                final T returnedValue = columnParser.parseColumn( column );

                //Use an O(log n) search, same as a tree, but with fast access to indexes for later operations
                int searchIndex = Collections.binarySearch( mergedResults, returnedValue, comparator );

                /**
                 * DO NOT remove this section of code. If you're seeing inconsistent results during shard transition,
                 * you'll
                 * need to enable this
                 */
                //
                //                if ( previous != null && comparator.compare( previous, returnedValue ) == 0 ) {
                //                    throw new RuntimeException( String.format(
                //                            "Cassandra returned 2 unique columns,
                // but your comparator marked them as equal.  This " +
                //                                    "indicates a bug in your comparator.  Previous value was %s and
                // current value is " +
                //                                    "%s",
                //                            previous, returnedValue ) );
                //                }
                //
                //                previous = returnedValue;

                //we've already seen it, no-op
                if(searchIndex > -1){
                    continue;
                }

                final int insertIndex = (searchIndex+1)*-1;

                //it's at the end of the list, don't bother inserting just to remove it
                if(insertIndex >= maxSize){
                    continue;
                }

                mergedResults.add( insertIndex, returnedValue );


                //prune the mergedResults
                while ( mergedResults.size() > maxSize ) {
                    //just remove from our tail until the size falls to the correct value
                    mergedResults.remove(mergedResults.size()-1);
                }
            }

            LOG.trace( "Candidate result set size is {}", mergedResults.size() );

        }
        return mergedResults;
    }


    /**
     * Iterator wrapper that parses as it iterates for single row cases
     */
    private class SingleRowIterator implements Iterator<T> {

        private Iterator<Column<C>> columnIterator;

        private SingleRowIterator (final ColumnList<C> columns){
            this.columnIterator = columns.iterator();
        }
        @Override
        public boolean hasNext() {
            return columnIterator.hasNext();
        }


        @Override
        public T next() {
            return columnParser.parseColumn( columnIterator.next() );
        }


        @Override
        public void remove() {
          throw new UnsupportedOperationException( "Unable to remove single row" );
        }
    }
}


