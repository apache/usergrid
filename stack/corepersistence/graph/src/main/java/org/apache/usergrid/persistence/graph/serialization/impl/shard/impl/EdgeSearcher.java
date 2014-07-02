package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;

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
public abstract class EdgeSearcher<R, C, T> implements ColumnParser<C, T>, Comparator<T>,
        Iterator<List<ScopedRowKey<ApplicationScope, R>>> {

    protected final Optional<Edge> last;
    protected final long maxTimestamp;
    protected final ApplicationScope scope;
    protected final Iterator<ShardEntryGroup> shards;


    protected EdgeSearcher( final ApplicationScope scope, final long maxTimestamp, final Optional<Edge> last,
                            final Iterator<ShardEntryGroup> shards ) {

        Preconditions.checkArgument(shards.hasNext(), "Cannot search with no possible shards");

        this.scope = scope;
        this.maxTimestamp = maxTimestamp;
        this.last = last;
        this.shards = shards;
    }


    @Override
    public boolean hasNext() {
        return shards.hasNext();
    }


    @Override
    public List<ScopedRowKey<ApplicationScope, R>> next() {
        Collection<Shard> readShards = shards.next().getReadShards();

        List<ScopedRowKey<ApplicationScope, R>> rowKeys = new ArrayList<>(readShards.size());

        for(Shard shard : readShards){

            final ScopedRowKey<ApplicationScope, R> rowKey = ScopedRowKey
                    .fromKey( scope, generateRowKey(shard.getShardIndex() ) );

            rowKeys.add( rowKey );
        }


        return rowKeys;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported" );
    }


    /**
     * Set the range on a search
     */
    public void setRange( final RangeBuilder builder ) {

        //set our start range since it was supplied to us
        if ( last.isPresent() ) {
            C sourceEdge = getStartColumn( last.get() );


            builder.setStart( sourceEdge, getSerializer() );
        }
        else {


        }
    }


    public boolean hasPage() {
        return last.isPresent();
    }


    @Override
    public T parseColumn( final Column<C> column ) {
        final C edge = column.getName();

        return createEdge( edge, column.getBooleanValue() );
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
    protected abstract C getStartColumn( final Edge last );


    /**
     * Create an edge to return to the user based on the directed edge provided
     *
     * @param column The column name
     * @param marked The marked flag in the column value
     */
    protected abstract T createEdge( final C column, final boolean marked );
}
