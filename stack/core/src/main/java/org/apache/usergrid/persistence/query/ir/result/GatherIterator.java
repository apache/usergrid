package org.apache.usergrid.persistence.query.ir.result;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
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


    private final QueryNode rootNode;
    private final int pageSize;


    private Iterator<ScanColumn> mergedIterators;
    private List<ResultIterator> iterators;


    public GatherIterator(final int pageSize, final QueryNode rootNode, final Collection<SearchVisitor> searchVisitors) {
        this.pageSize = pageSize;
        this.rootNode = rootNode;
        createIterators( searchVisitors );
    }


    @Override
    public void reset() {
        throw new UnsupportedOperationException( "Gather iterators cannot be reset" );
    }



    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        if(mergedIterators == null || !mergedIterators.hasNext()){
            mergeIterators();
        }

        return mergedIterators.hasNext();
    }


    @Override
    public Set<ScanColumn> next() {
        if(!hasNext()){
            throw new NoSuchElementException( "No more elements" );
        }

        return getNextPage();
    }

    private void createIterators(final Collection<SearchVisitor> searchVisitors ){

        this.iterators = new ArrayList<ResultIterator>( searchVisitors.size() );

        for(SearchVisitor visitor: searchVisitors){

            try {
                rootNode.visit( visitor );
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Unable to process query", e );
            }

            final ResultIterator iterator = visitor.getResults();

            iterators.add( iterator );

        }

    }


    /**
     * Get the next page of results
     * @return
     */
    private Set<ScanColumn> getNextPage(){

        //try to take from our PageSize
        LinkedHashSet<ScanColumn> resultSet = new LinkedHashSet<ScanColumn>( pageSize );

        for(int i = 0; i < pageSize && mergedIterators.hasNext(); i ++){
            resultSet.add( mergedIterators.next() );
        }


        return resultSet;
    }

    /**
     * Advance the iterator
     */
    private void mergeIterators(){
        //TODO make this concurrent


        TreeSet<ScanColumn> merged = new TreeSet<ScanColumn>(  );

        for(ResultIterator iterator: this.iterators){
              merge(merged, iterator);
        }

        mergedIterators = merged.iterator();

    }




    /**
     * Merge this interator into our final column results
     * @param results
     * @param iterator
     */
    private void merge(final TreeSet<ScanColumn> results, final ResultIterator iterator){


        //nothing to do, return
        if( !iterator.hasNext()){
            return;
        }

        final Iterator<ScanColumn> nextPage = iterator.next().iterator();

        //only take from the iterator what we need to create a full page.
        for(int i = 0 ; i < pageSize && nextPage.hasNext(); i ++){
            final ScanColumn next = nextPage.next();

            results.add(next);

        }

    }


}
