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
package org.apache.usergrid.persistence.query.ir.result;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;


/**
 * Used to gather results from multiple sub iterators
 */
public class GatherIterator implements ResultIterator {


    private List<Future<Void>> iterators;
    private final ConcurrentResultMerge merge;
    private boolean merged;


    public GatherIterator( final int pageSize, final QueryNode rootNode, final Collection<SearchVisitor> searchVisitors,
                           final ExecutorService executorService ) {
        this.merge = new ConcurrentResultMerge( pageSize );


        this.iterators = new ArrayList<Future<Void>>( searchVisitors.size() );
        this.merged = false;


        /**
         * Start our search processing
         */
        for ( SearchVisitor visitor : searchVisitors ) {
            final Future<Void> result = executorService.submit( new VisitorExecutor( rootNode, merge, visitor ) );
            iterators.add( result );
        }
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
        waitForCompletion();

        return merge.results.size() > 0;
    }


    private void waitForCompletion() {
        if ( merged ) {
            return;
        }

        for ( final Future<Void> future : iterators ) {
            try {
                future.get();
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Unable to aggregate results", e );
            }
        }

        merged = true;
    }


    @Override
    public Set<ScanColumn> next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "No more elements" );
        }

        return merge.copyAndClear();
    }


    /**
     * A visitor that will visit and get the first page of an set and return them.
     */
    private final class VisitorExecutor implements Callable<Void> {

        private final QueryNode rootNode;
        private final SearchVisitor visitor;
        private final ConcurrentResultMerge merge;


        private VisitorExecutor( final QueryNode rootNode, final ConcurrentResultMerge merge,
                                 final SearchVisitor visitor ) {
            this.rootNode = rootNode;
            this.visitor = visitor;
            this.merge = merge;
        }


        @Override
        public Void call() throws Exception {


            try {
                rootNode.visit( visitor );
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Unable to process query", e );
            }

            final ResultIterator iterator = visitor.getResults();


            if ( iterator.hasNext() ) {
                merge.merge( iterator.next() );
            }

            return null;
        }
    }


    /**
     * Class used to synchronize our treeSet access
     */
    private final class ConcurrentResultMerge {

        private final TreeSet<ScanColumn> results;
        private final int maxSize;


        private ConcurrentResultMerge( final int maxSize ) {
            this.maxSize = maxSize;
            results = new TreeSet<ScanColumn>();
        }


        /**
         * Merge this set into the existing set
         */
        public synchronized void merge( final Set<ScanColumn> columns ) {
            for ( ScanColumn scanColumn : columns ) {
                results.add( scanColumn );

                //results are too large remove the last
                while ( results.size() > maxSize ) {
                    results.pollLast();
                }
            }
        }


        /**
         * Get the set
         */
        public Set<ScanColumn> copyAndClear() {
            //create an immutable copy
            final Set<ScanColumn> toReturn = new LinkedHashSet<ScanColumn>( results );
            results.clear();
            return toReturn;
        }
    }
}
