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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.*;

import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.ColumnSearch;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.shard.SmartShard;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.util.RangeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Searcher to be used when performing the search.  Performs I/O transformation as well as parsing for the iterator. If
 * there are more row keys available to seek, the iterator will return true
 *
 * @param <R> The row type
 * @param <C> The column type
 * @param <T> The parsed return type
 */
public abstract class EdgeSearcher<R, C, T> implements ColumnParser<C, T>, ColumnSearch<T>{

    private static final Logger logger = LoggerFactory.getLogger( EdgeSearcher.class );


    protected final Optional<T> last;
    protected final Optional<Long> lastTimestamp;
    protected final long maxTimestamp;
    protected final ApplicationScope scope;
    protected final Collection<Shard> shards;
    protected final SearchByEdgeType.Order order;
    protected final Comparator<T> comparator;


    protected EdgeSearcher( final ApplicationScope scope, final Collection<Shard> shards,
                            final SearchByEdgeType.Order order, final Comparator<T> comparator,
                            final long maxTimestamp, final Optional<T> last, final Optional<Long> lastTimestamp) {

        Preconditions.checkArgument(shards.size() > 0 , "Cannot search with no possible shards");

        this.scope = scope;
        this.maxTimestamp = maxTimestamp;
        this.order = order;
        this.shards = shards;
        this.last = last;
        this.lastTimestamp = lastTimestamp;
        this.comparator = comparator;

    }



    public List<ScopedRowKey<R>> getRowKeys() {

        List<ScopedRowKey<R>> rowKeys = new ArrayList<>(shards.size());

        for(Shard shard : shards){
            final ScopedRowKey< R> rowKey = ScopedRowKey
                    .fromKey( scope.getApplication(), generateRowKey(shard.getShardIndex() ) );

            rowKeys.add( rowKey );
        }


        return rowKeys;
    }

    public List<SmartShard> getRowKeysWithShardEnd(){


        final List<SmartShard> rowKeysWithShardEnd = new ArrayList<>(shards.size());

        for(Shard shard : shards){

            final ScopedRowKey< R> rowKey = ScopedRowKey
                .fromKey( scope.getApplication(), generateRowKey(shard.getShardIndex() ) );


            final T shardEnd;
            if(shard.getShardEnd().isPresent()){
                shardEnd = createEdge((C) shard.getShardEnd().get(), false); // convert DirectedEdge to Edge
            }else{
                shardEnd = null;
            }

            rowKeysWithShardEnd.add(new SmartShard(rowKey, shard.getShardIndex(), shardEnd));
        }

        return rowKeysWithShardEnd;

    }


    @Override
    public boolean skipFirst( final T first ) {
        if(!last.isPresent()){
            return false;
        }

        return last.get().equals( first );
    }


    @Override
    public T parseColumn( final Column<C> column ) {
        final C edge = column.getName();

        return createEdge( edge, column.getBooleanValue() );
    }


    @Override
    public void buildRange(final RangeBuilder rangeBuilder, final T start, T end) {

        if ( start != null){

            C startEdge = createColumn( start );
            rangeBuilder.setStart( startEdge, getSerializer() );
        }else{

            setTimeScan( rangeBuilder );
        }

        if( end != null){

            C endEdge = createColumn( end );
            rangeBuilder.setEnd( endEdge, getSerializer() );

        }

        setRangeOptions( rangeBuilder );
    }


    @Override
    public void buildRange( final RangeBuilder rangeBuilder ) {

        //set our start range since it was supplied to us
        if ( last.isPresent() ) {
            C sourceEdge = createColumn( last.get() );

            rangeBuilder.setStart( sourceEdge, getSerializer() );
        }else {
            setTimeScan( rangeBuilder );
        }

        setRangeOptions( rangeBuilder );


    }

    private void setRangeOptions(final RangeBuilder rangeBuilder){

        //if we're ascending, this is opposite what cassandra sorts, so set the reversed flag
        final boolean reversed = order == SearchByEdgeType.Order.ASCENDING;

        rangeBuilder.setReversed( reversed );

    }

    /**
     * Get the comparator
     * @return
     */
    public Comparator<T> getComparator() {
        return comparator;
    }

    public SearchByEdgeType.Order getOrder(){
        return order;
    }

    public Optional<Long> getLastTimestamp() {
        return lastTimestamp;
    }


    /**
     * Get the column's serializer
     */
    protected abstract Serializer<C> getSerializer();


    /**
     * Create a row key for this search to use
     *
     * @param shard The shard to use in the row key
     */
    protected abstract R generateRowKey( final long shard );


    /**
     * Set the start column to begin searching from.  The last is provided
     */
    protected abstract C createColumn( final T last );

    /**
     * Set the time scan into the range builder
     * @param rangeBuilder
     */
    protected abstract void setTimeScan(final RangeBuilder rangeBuilder);

    /**
     * Create an edge to return to the user based on the directed edge provided
     *
     * @param column The column name
     * @param marked The marked flag in the column value
     */
    protected abstract T createEdge( final C column, final boolean marked );
}
