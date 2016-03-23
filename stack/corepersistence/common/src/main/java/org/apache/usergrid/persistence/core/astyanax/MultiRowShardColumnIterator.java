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
public class MultiRowShardColumnIterator<R, C, T> implements Iterator<T> {

    private static final Logger logger = LoggerFactory.getLogger( MultiRowShardColumnIterator.class );

    private final int pageSize;

    private final ColumnFamily<R, C> cf;


    private final ColumnParser<C, T> columnParser;

    private final ColumnSearch<T> columnSearch;

    private final Comparator<T> comparator;


    private final Keyspace keyspace;

    private final ConsistencyLevel consistencyLevel;

    private T startColumn;

    private boolean moreToReturn;

    private Iterator<T> currentColumnIterator;

    private Iterator<SmartShard> currentShardIterator;

    private List<SmartShard> rowKeysWithShardEnd;

    private SmartShard currentShard;

    private List<T> resultsTracking; // use for de-duping results that are possible during shard transition

    private int skipSize = 0; // used for determining if we've skipped a whole page during shard transition

    private boolean ascending = false;


    public MultiRowShardColumnIterator( final Keyspace keyspace, final ColumnFamily<R, C> cf,
                                        final ConsistencyLevel consistencyLevel, final ColumnParser<C, T> columnParser,
                                        final ColumnSearch<T> columnSearch, final Comparator<T> comparator,
                                        final int pageSize, final List<SmartShard> rowKeysWithShardEnd,
                                        final boolean ascending) {
        this.cf = cf;
        this.pageSize = pageSize;
        this.columnParser = columnParser;
        this.columnSearch = columnSearch;
        this.comparator = comparator;
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
                logger.trace("currentShard: {}", currentShard);
            }

            currentShard = currentShardIterator.next();

            if(logger.isTraceEnabled()){
                logger.trace("all shards when starting: {}", rowKeysWithShardEnd);
                logger.trace("initializing iterator with shard: {}", currentShard);
            }


        }



        // initial request, build the range with no start and no end
        if ( startColumn == null && currentShard.getShardEnd() == null ){

            columnSearch.buildRange( rangeBuilder );

            if(logger.isTraceEnabled()){
                logger.trace("initial search (no start or shard end)");
            }

        }
        // if there's only a startColumn set the range start startColumn always
        else if ( startColumn != null && currentShard.getShardEnd() == null ){

            columnSearch.buildRange( rangeBuilder, startColumn, null );

            if(logger.isTraceEnabled()){
                logger.trace("search (no shard end) with start: {}", startColumn);
            }

        }
        // if there's only a shardEnd, set the start/end according based on the search order
        else if ( startColumn == null && currentShard.getShardEnd() != null ){

            T shardEnd = (T) currentShard.getShardEnd();

            // if we have a shardEnd and it's not an ascending search, use the shardEnd as a start
            if(!ascending) {

                columnSearch.buildRange(rangeBuilder, shardEnd, null);

                if(logger.isTraceEnabled()){
                    logger.trace("search descending with start: {}", shardEnd);
                }

            }
            // if we have a shardEnd and it is an ascending search, use the shardEnd as the end
            else{

                columnSearch.buildRange( rangeBuilder, null, shardEnd );

                if(logger.isTraceEnabled()){
                    logger.trace("search ascending with end: {}", shardEnd);
                }

            }

        }
        // if there's both a startColumn and a shardEnd, decide which should be used as start/end based on search order
        else if ( startColumn != null && currentShard.getShardEnd() != null) {

            T shardEnd = (T) currentShard.getShardEnd();


            // if the search is not ascending, set the start to be the older edge
            if(!ascending){

                T searchStart = comparator.compare(shardEnd, startColumn) > 0 ? shardEnd : startColumn;
                columnSearch.buildRange( rangeBuilder, searchStart, null);

                if(logger.isTraceEnabled()){
                    logger.trace("search descending with start: {} in shard", searchStart, currentShard);
                }

            }
            // if the search is ascending, then always use the startColumn for the start and shardEnd for the range end
            else{

                columnSearch.buildRange( rangeBuilder, startColumn , shardEnd);

                if(logger.isTraceEnabled()){
                    logger.trace("search with start: {}, end: {}", startColumn, shardEnd);
                }



            }

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
            logger.trace("current shard: {}, retrieved size: {}", currentShard, size);
            logger.trace("selectSize={}, size={}, ", selectSize, size);


        }

        moreToReturn = size == selectSize;

        if(selectSize == 1001 && mergedResults.size() == 1000){
            moreToReturn = true;
        }


        // if a whole page is skipped OR the result size equals the the difference of what's skipped,
        // it is likely during a shard transition and we should assume there is more to read
        if( skipSize == selectSize || skipSize == selectSize - 1 || size == selectSize - skipSize || size == (selectSize -1) - skipSize ){
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
            logger.trace("currentColumnIterator.hasNext()={}, " +
                    "moreToReturn={}, currentShardIterator.hasNext()={}",
                currentColumnIterator.hasNext(), moreToReturn, currentShardIterator.hasNext());
        }


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


