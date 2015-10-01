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
package org.apache.usergrid.persistence.core.astyanax;


import java.util.Iterator;
import java.util.NoSuchElementException;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.query.RowQuery;


/**
 * Simple iterator that wraps a Row query and will keep executing it's paging until there are no more results to read
 * from cassandra
 */
public class ColumnNameIterator<C, T> implements Iterable<T>, Iterator<T> {


    private final RowQuery<?, C> rowQuery;
    private final ColumnParser<C, T> parser;
    private final boolean skipFirst;

    private Iterator<Column<C>> sourceIterator;


    public ColumnNameIterator( RowQuery<?, C> rowQuery, final ColumnParser<C, T> parser, final boolean skipFirst ) {
        this.rowQuery = rowQuery.autoPaginate( true );
        this.parser = parser;
        this.skipFirst = skipFirst;
    }


    @Override
    public Iterator<T> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {

        if ( sourceIterator == null ) {
            advanceIterator();


            //if we are to skip the first element, we need to advance the iterator
            if ( skipFirst && sourceIterator.hasNext() ) {
                sourceIterator.next();
            }

            return sourceIterator.hasNext();
        }

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
        try {
            sourceIterator = rowQuery.execute().getResult().iterator();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to get next page", e );
        }
    }
}
