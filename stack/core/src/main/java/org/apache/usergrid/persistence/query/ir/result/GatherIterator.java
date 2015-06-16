package org.apache.usergrid.persistence.query.ir.result;


import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.cassandra.CursorCache;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;


/**
 * Used to gather results from multiple sub iterators
 */
public class GatherIterator implements ResultIterator {


    private final Collection<SearchVisitor> searchVisitors;
    private final QueryNode rootNode;


    public GatherIterator(  final QueryNode rootNode, final Collection<SearchVisitor> searchVisitors) {
        this.rootNode = rootNode;
        this.searchVisitors = searchVisitors;
    }


    @Override
    public void reset() {
        throw new UnsupportedOperationException( "Gather iterators cannot be reset" );
    }


    @Override
    public void finalizeCursor( final CursorCache cache, final UUID lastValue ) {
        //find the last value in the tree, and return it's cursor
    }


    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        return false;
    }


    @Override
    public Set<ScanColumn> next() {
        return null;
    }
}
