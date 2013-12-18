package org.apache.usergrid.persistence.graph.serialization.impl;


import java.util.Iterator;

import com.netflix.astyanax.model.Column;


/**
 *
 * Simple iterator that wraps columns and returns the name
 *
 */
public class ColumnNameIterator<C> implements Iterable<C>, Iterator<C> {


    private final Iterator<Column<C>> sourceIterator;


    public ColumnNameIterator( final Iterator<Column<C>> sourceIterator ) {
        this.sourceIterator = sourceIterator;}


    @Override
    public Iterator<C> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        return sourceIterator.hasNext();
    }


    @Override
    public C next() {
        return sourceIterator.next().getName();
    }


    @Override
    public void remove() {
       sourceIterator.remove();
    }
}
