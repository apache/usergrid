package org.apache.usergrid.persistence.graph.serialization.impl.parse;


import java.util.Iterator;

import org.apache.usergrid.persistence.graph.serialization.impl.parse.ColumnParser;

import com.netflix.astyanax.model.Column;


/**
 *
 * Simple iterator that wraps columns and returns the value from the parser
 *
 *  C
 *
 *
 */
public class ColumnNameIterator<C, T> implements Iterable<T>, Iterator<T> {


    private final Iterator<Column<C>> sourceIterator;
    private final ColumnParser<C, T> parser;



    public ColumnNameIterator( final Iterator<Column<C>> sourceIterator, final ColumnParser<C, T> parser ) {
        this.sourceIterator = sourceIterator;
        this.parser = parser;
    }


    @Override
    public Iterator<T> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        return sourceIterator.hasNext();
    }


    @Override
    public T next() {
        return parser.parseColumn(sourceIterator.next());
    }


    @Override
    public void remove() {
       sourceIterator.remove();
    }
}
