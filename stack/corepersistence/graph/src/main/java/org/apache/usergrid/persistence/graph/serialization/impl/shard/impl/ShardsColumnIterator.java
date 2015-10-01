package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.MultiKeyColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.MultiRowColumnIterator;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.util.RangeBuilder;


/**
 * Internal iterator to iterate over multiple row keys
 *
 * @param <R> The row type
 * @param <C> The column type
 * @param <T> The parsed return type
 */
public class ShardsColumnIterator<R, C, T> implements Iterator<T> {

    private final EdgeSearcher<R, C, T> searcher;

    private final MultiTennantColumnFamily<ScopedRowKey<R>, C> cf;

    private Iterator<T> currentColumnIterator;

    private final Keyspace keyspace;

    private final int pageSize;

    private final ConsistencyLevel consistencyLevel;


    public ShardsColumnIterator( final EdgeSearcher<R, C, T> searcher,
                             final MultiTennantColumnFamily<ScopedRowKey<R>, C> cf, final Keyspace keyspace,
                             final ConsistencyLevel consistencyLevel, final int pageSize ) {
        this.searcher = searcher;
        this.cf = cf;
        this.keyspace = keyspace;
        this.pageSize = pageSize;
        this.consistencyLevel = consistencyLevel;
    }


    @Override
    public boolean hasNext() {

        /**
         * Iterator isn't initialized, start it
         */
        if(currentColumnIterator == null){
            startIterator();
        }

        return currentColumnIterator.hasNext();
    }


    @Override
    public T next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "There are no more rows or columns left to advance" );
        }

        return currentColumnIterator.next();
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported" );
    }


    /**
     * Advance our iterator to the next row (assumes the check for row keys is elsewhere)
     */
    private void startIterator() {


        /**
         * If the edge is present, we need to being seeking from this
         */

        final RangeBuilder rangeBuilder = new RangeBuilder().setLimit( pageSize );


        //set the range into the search
        searcher.buildRange( rangeBuilder );



        /**
         * Get our list of slices
         */
        final List<ScopedRowKey<R>> rowKeys = searcher.getRowKeys();

        currentColumnIterator = new MultiRowColumnIterator<>( keyspace, cf,  consistencyLevel, searcher, searcher, searcher.getComparator(), rowKeys, pageSize);




    }
}
