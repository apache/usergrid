/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.core.astyanax;


import java.util.*;

import org.apache.usergrid.persistence.core.shard.SmartShard;
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

    private static final Logger logger = LoggerFactory.getLogger( MultiRowColumnIterator.class );

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

    private Iterator<SmartShard> currentShardIterator;

    private List<SmartShard> rowKeysWithShardEnd;

    private SmartShard currentShard;

    private List<T> resultsTracking;

    private int skipSize = 0; // used for determining if we've skipped a whole page during shard transition

    private boolean ascending = false;


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
        this.resultsTracking = new ArrayList<>();

    }

    // temporarily use a new constructor for specific searches until we update each caller of this class
    public MultiRowColumnIterator( final Keyspace keyspace, final ColumnFamily<R, C> cf,
                                   final ConsistencyLevel consistencyLevel, final ColumnParser<C, T> columnParser,
                                   final ColumnSearch<T> columnSearch, final Comparator<T> comparator,
                                   final Collection<R> rowKeys, final int pageSize,
                                   final List<SmartShard> rowKeysWithShardEnd,
                                   final boolean ascending) {
        this.cf = cf;
        this.pageSize = pageSize;
        this.columnParser = columnParser;
        this.columnSearch = columnSearch;
        this.comparator = comparator;
        this.rowKeys = rowKeys;
        this.keyspace = keyspace;
        this.consistencyLevel = consistencyLevel;
        this.moreToReturn = true;
        this.rowKeysWithShardEnd = rowKeysWithShardEnd;
        this.resultsTracking = new ArrayList<>();
        this.ascending = ascending;

    }


    @Override
    public boolean hasNext() {

        // if column iterator is null, initialize with first call to advance()
        // advance if we know there more columns exist in the current shard but we've exhausted this page fetch from c*
        if ( currentColumnIterator == null || ( !currentColumnIterator.hasNext() && moreToReturn ) ) {
            advance();
        }

        // when there are no more columns, nothing reported to return, but more shards available, go to the next shard
        if( currentColumnIterator != null && !currentColumnIterator.hasNext() &&
            !moreToReturn && currentShardIterator.hasNext()){

                if(logger.isTraceEnabled()){
                    logger.trace("Advancing shard iterator");
                    logger.trace("Shard before advance: {}", currentShard);
                }


                // advance to the next shard
                currentShard = currentShardIterator.next();

                if(logger.isTraceEnabled()){
                    logger.trace("Shard after advance: {}", currentShard);

                }

                // reset the start column as we'll be seeking a new row, any duplicates will be filtered out
                startColumn = null;

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

        if (logger.isTraceEnabled()) logger.trace( "Advancing multi row column iterator" );

        /**
         * If the edge is present, we need to being seeking from this
         */

        final boolean skipFirstColumn = startColumn != null;

        final int selectSize = skipFirstColumn ? pageSize + 1 : pageSize;

        final RangeBuilder rangeBuilder = new RangeBuilder();




        if(currentShardIterator == null){

            // flip the order of our shards if ascending
            if(ascending){
                Collections.reverse(rowKeysWithShardEnd);
            }

            currentShardIterator = rowKeysWithShardEnd.iterator();

        }

        if(currentShard == null){

            if(logger.isTraceEnabled()){
                logger.trace(Thread.currentThread().getName()+" - currentShard: {}", currentShard);
            }

            currentShard = currentShardIterator.next();

            if(logger.isTraceEnabled()){
                logger.trace(Thread.currentThread().getName()+" - all shards when starting: {}", rowKeysWithShardEnd);
                logger.trace(Thread.currentThread().getName()+" - initializing iterator with shard: {}", currentShard);
            }


        }





        //set the range into the search
        if(logger.isTraceEnabled()){
            logger.trace(Thread.currentThread().getName()+" - startColumn={}", startColumn);
        }

        if ( startColumn == null ) {
            columnSearch.buildRange( rangeBuilder );
        }
        else {
            columnSearch.buildRange( rangeBuilder, startColumn );
        }


        rangeBuilder.setLimit( selectSize );

        if (logger.isTraceEnabled()) logger.trace( "Executing cassandra query with shard {}", currentShard );

        /**
         * Get our list of slices
         */
        final RowSliceQuery<R, C> query =
            keyspace.prepareQuery( cf ).setConsistencyLevel( consistencyLevel ).getKeySlice( (R) currentShard.getRowKey() )
                .withColumnRange( rangeBuilder.build() );

        final Rows<R, C> result;
        try {
            result = query.execute().getResult();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to casandra", e );
        }




        final List<T> mergedResults;

        skipSize = 0;

        mergedResults = processResults( result, selectSize );

        if(logger.isTraceEnabled()){
            logger.trace("skipped amount: {}", skipSize);
        }



        final int size = mergedResults.size();



        if(logger.isTraceEnabled()){
            logger.trace(Thread.currentThread().getName()+" - current shard: {}, retrieved size: {}", currentShard, size);
            logger.trace(Thread.currentThread().getName()+" - selectSize={}, size={}, ", selectSize, size);


        }

        moreToReturn = size == selectSize;

        if(selectSize == 1001 && mergedResults.size() == 1000){
            moreToReturn = true;
        }


        // if a whole page is skipped, this is likely during a shard transition and we should assume there is more to read
        if( skipSize == selectSize || skipSize == selectSize - 1){
            moreToReturn = true;
        }

        //we have a first column to to check
        if( size > 0) {

            final T firstResult = mergedResults.get( 0 );

            //The search has either told us to skip the first element, or it matches our last, therefore we disregard it
            if(columnSearch.skipFirst( firstResult ) || (skipFirstColumn && comparator.compare( startColumn, firstResult ) == 0)){
                if(logger.isTraceEnabled()){
                    logger.trace("removing an entry");

                }
                mergedResults.remove( 0 );
            }

        }


        // set the start column for the enxt query
        if(moreToReturn && mergedResults.size() > 0){
            startColumn = mergedResults.get( mergedResults.size()  - 1 );

        }


        currentColumnIterator = mergedResults.iterator();


        //force an advance of this iterator when there are still shards to read but result set on current shard is 0
        if(size == 0 && currentShardIterator.hasNext()){
            hasNext();
        }

       if(logger.isTraceEnabled()){
           logger.trace(
               Thread.currentThread().getName()+" - currentColumnIterator.hasNext()={}, " +
                   "moreToReturn={}, currentShardIterator.hasNext()={}",
               currentColumnIterator.hasNext(), moreToReturn, currentShardIterator.hasNext());
       }


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

        if (logger.isTraceEnabled()) logger.trace( "Only a single row has columns.  Parsing directly" );

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
     * Process the result set and filter any duplicates that may have already been seen in previous shards.  During
     * a shard transition, there could be the same columns in multiple shards (rows).  This will also allow for
     * filtering the startColumn (the seek starting point) when paging a row in Cassandra.
     *
     * @param result
     * @return
     */
    private List<T> processResults(final Rows<R, C> result, final int maxSize ) {

        final List<T> mergedResults = new ArrayList<>(maxSize);

        for ( final R key : result.getKeys() ) {
            final ColumnList<C> columns = result.getRow( key ).getColumns();


            for (final Column<C> column :columns  ) {

                final T returnedValue = columnParser.parseColumn( column );

                // use an O(log n) search, same as a tree, but with fast access to indexes for later operations
                int searchIndex = Collections.binarySearch( resultsTracking, returnedValue, comparator );


                //we've already seen the column, filter it out as we might be in a shard transition or our start column
                if(searchIndex > -1){
                    if(logger.isTraceEnabled()){
                        logger.trace("skipping column as it was already retrieved before");
                    }
                    skipSize++;
                    continue;
                }


                resultsTracking.add(returnedValue);
                mergedResults.add(returnedValue );


            }

            if (logger.isTraceEnabled()) logger.trace( "Candidate result set size is {}", mergedResults.size() );

        }
        return mergedResults;
    }

}


