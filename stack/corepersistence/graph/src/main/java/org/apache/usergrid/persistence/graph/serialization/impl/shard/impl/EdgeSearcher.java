package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.ColumnSearch;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.util.RangeBuilder;


/**
 * Searcher to be used when performing the search.  Performs I/O transformation as well as parsing for the iterator. If
 * there are more row keys available to seek, the iterator will return true
 *
 * @param <R> The row type
 * @param <C> The column type
 * @param <T> The parsed return type
 */
public abstract class EdgeSearcher<R, C, T> implements ColumnParser<C, T>, ColumnSearch<T>{

    protected final Optional<T> last;
    protected final long maxTimestamp;
    protected final ApplicationScope scope;
    protected final Collection<Shard> shards;
    protected final SearchByEdgeType.Order order;
    protected final Comparator<T> comparator;


    protected EdgeSearcher( final ApplicationScope scope, final Collection<Shard> shards,  final SearchByEdgeType.Order order, final Comparator<T> comparator,  final long maxTimestamp, final Optional<T> last) {

        Preconditions.checkArgument(shards.size() > 0 , "Cannot search with no possible shards");

        this.scope = scope;
        this.maxTimestamp = maxTimestamp;
        this.order = order;
        this.shards = shards;
        this.last = last;
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
    public void buildRange( final RangeBuilder rangeBuilder, final T value ) {

        C edge = createColumn( value );

        rangeBuilder.setStart( edge, getSerializer() );

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
