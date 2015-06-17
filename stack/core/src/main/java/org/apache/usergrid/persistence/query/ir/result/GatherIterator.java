package org.apache.usergrid.persistence.query.ir.result;


import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.usergrid.persistence.ResultsIterator;
import org.apache.usergrid.persistence.cassandra.CursorCache;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;


/**
 * Used to gather results from multiple sub iterators
 */
public class GatherIterator implements ResultIterator {


    private final Collection<SearchVisitor> searchVisitors;
    private final QueryNode rootNode;
    private final int pageSize;


    private Set<ScanColumn> next;


    public GatherIterator(final int pageSize, final QueryNode rootNode, final Collection<SearchVisitor> searchVisitors) {
        this.pageSize = pageSize;
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

        if(next() == null){
            advance();
        }

        return next != null;
    }


    @Override
    public Set<ScanColumn> next() {
        if(!hasNext()){
            throw new NoSuchElementException( "No more elements" );
        }

        final Set<ScanColumn> results = next;
        next = null;
        return results;
    }


    /**
     * Advance the iterator
     */
    private void advance(){
        //TODO make this concurrent


        final TreeSet<ScanColumn> results = new TreeSet<ScanColumn>(  );


        for(SearchVisitor visitor: searchVisitors){
              merge(results, visitor);
        }

        this.next = results;
    }


    /**
     * Merge this interator into our final column results
     * @param results
     * @param visitor
     */
    private void merge(final TreeSet<ScanColumn> results, final SearchVisitor visitor){

        final ResultIterator iterator = visitor.getResults();


        //nothing to do, return
        if( !iterator.hasNext()){
            return;
        }


        final Iterator<ScanColumn> nextPage = iterator.next().iterator();


        //only take from the iterator what we need to create a full page.
        for(int i = 0 ; i < pageSize && nextPage.hasNext(); i ++){
            results.add( nextPage.next() );

            //results are too large, trim them
            if(results.size() > pageSize){
                results.pollLast();
            }
        }

    }
}
