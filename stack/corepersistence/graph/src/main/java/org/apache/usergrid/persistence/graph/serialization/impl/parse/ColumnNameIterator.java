package org.apache.usergrid.persistence.graph.serialization.impl.parse;


import java.util.Iterator;
import java.util.NoSuchElementException;

import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;


/**
 * Simple iterator that wraps a Row query and will keep executing it's paging until there are no more results to read
 * from cassandra
 */
public class ColumnNameIterator<C, T> implements Iterable<T>, Iterator<T> {


    private static final HystrixCommandGroupKey GROUP_KEY = HystrixCommandGroupKey.Factory.asKey( "CassRead" );

    private final int executionTimeout;


    private final RowQuery<?, C> rowQuery;
    private final ColumnParser<C, T> parser;

    private Iterator<Column<C>> sourceIterator;


    public ColumnNameIterator( RowQuery<?, C> rowQuery, final ColumnParser<C, T> parser, final boolean skipFirst,
                               final int executionTimeout ) {
        this.rowQuery = rowQuery.autoPaginate( true );
        this.parser = parser;
        this.executionTimeout = executionTimeout;

        advanceIterator();

        //if we are to skip the first element, we need to advance the iterator
        if ( skipFirst && sourceIterator.hasNext() ) {
            sourceIterator.next();
        }
    }


    @Override
    public Iterator<T> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        //if we've exhausted this iterator, try to advance to the next set
        if ( sourceIterator.hasNext() ) {
            return true;
        }

        //advance the iterator, to the next page, there could be more
        advanceIterator();

        return sourceIterator.hasNext();
    }


    @Override
    public T next() {

        if ( !hasNext() ) {
            throw new NoSuchElementException();
        }

        return parser.parseColumn( sourceIterator.next() );
    }


    @Override
    public void remove() {
        sourceIterator.remove();
    }


    /**
     * Execute the query again and set the reuslts
     */
    private void advanceIterator() {

        //run producing the values within a hystrix command.  This way we'll time out if the read takes too long
        sourceIterator = new HystrixCommand<Iterator<Column<C>>>( HystrixCommand.Setter.withGroupKey( GROUP_KEY )
                                                                                .andCommandPropertiesDefaults(
                                                                                        HystrixCommandProperties
                                                                                                .Setter()
                                                                                                .withExecutionIsolationThreadTimeoutInMilliseconds(
                                                                                                        executionTimeout ) ) ) {

            @Override
            protected Iterator<Column<C>> run() throws Exception {
                return rowQuery.execute().getResult().iterator();
            }
        }.execute();
    }
}
