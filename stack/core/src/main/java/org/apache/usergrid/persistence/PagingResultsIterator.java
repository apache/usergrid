/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence;


import java.util.Iterator;
import java.util.List;
import org.apache.usergrid.persistence.Query.Level;


/** iterates over a Results object, crossing page boundaries automatically */
public class PagingResultsIterator implements ResultsIterator, Iterable {

    private Results results;
    private Iterator currentPageIterator;
    private Level level;


    public PagingResultsIterator( Results results ) {
        this( results, results.level );
    }


    /**
     * @param level overrides the default level from the Results - in case you want to return, say, UUIDs where the
     * Query was set for Entities
     */
    public PagingResultsIterator( Results results, Level level ) {
        this.results = results;
        this.level = level;
        initCurrentPageIterator();
    }


    @Override
    public boolean hasNext() {
        if ( currentPageIterator != null ) {
            if ( currentPageIterator.hasNext() ) {
                return true;
            }
            else {
                return loadNextPage();
            }
        }
        return false;
    }

    @Override
    public boolean hasPages(){
        return results != null && results.hasCursor();
    }


    /** @return the next object (type varies according the Results.Level) */
    @Override
    public Object next() {
        return currentPageIterator.next();
    }


    /** not supported */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }


    /**
     * initialize the iterator for the current page of results.
     *
     * @return true if the iterator has more results
     */
    private boolean initCurrentPageIterator() {
        List currentPage;
        if ( results != null ) {
            switch ( level ) {
                case IDS:
                    currentPage = results.getIds();
                    break;
                case REFS:
                    currentPage = results.getRefs();
                    break;
                default:
                    currentPage = results.getEntities();
            }
            if ( currentPage.size() > 0 ) {
                currentPageIterator = currentPage.iterator();
            }
        }
        else {
            currentPageIterator = null;
        }
        return currentPageIterator != null && currentPageIterator.hasNext();
    }


    /**
     * attempts to load the next page
     *
     * @return true if loaded there are more results
     */
    private boolean loadNextPage() {
        try {
            results = results.getNextPageResults();
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return initCurrentPageIterator();
    }


    @Override
    public Iterator iterator() {
        return this;
    }
}
