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


    private final WorkerCoordinator workerCoordinator;


    public GatherIterator( final int pageSize, final QueryNode rootNode, final Collection<SearchVisitor> searchVisitors,
                           final ExecutorService executorService ) {

        this.workerCoordinator = new WorkerCoordinator( executorService, searchVisitors, rootNode, pageSize );

        this.workerCoordinator.start();
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
        return workerCoordinator.getResults().hasResults();
    }


    @Override
    public Set<ScanColumn> next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "No more elements" );
        }

        return workerCoordinator.getResults().copyAndClear();
    }


    /**
     * Coordinator object for all workers
     */
    private final class WorkerCoordinator {

        private final ExecutorService executorService;
        private final Collection<SearchVisitor> searchVisitors;
        private final int pageSize;
        private final QueryNode rootNode;
        private ConcurrentResultMerge merge;
        private ArrayList<Future<Void>> workers;


        private WorkerCoordinator( final ExecutorService executorService,
                                   final Collection<SearchVisitor> searchVisitors, final QueryNode rootNode,
                                   final int pageSize ) {
            this.executorService = executorService;
            this.searchVisitors = searchVisitors;
            this.rootNode = rootNode;
            this.pageSize = pageSize;
        }


        public void start() {
            this.merge = new ConcurrentResultMerge( pageSize );


            this.workers = new ArrayList<Future<Void>>( searchVisitors.size() );


            /**
             * Start our search processing
             */
            for ( SearchVisitor visitor : searchVisitors ) {
                final VisitorExecutor executor = new VisitorExecutor( rootNode, merge, visitor );

//                try {
//                    executor.call();
//                }
//                catch ( Exception e ) {
//                    throw new RuntimeException( e );
//                }
                final Future<Void> result = executorService.submit( executor);
                workers.add( result );
            }
        }


        /**
         * Get the results of the merge, after all workers have finished
         * @return
         */
        public ConcurrentResultMerge getResults() {

            //make sure all our workers are done
            for ( final Future<Void> future : workers ) {
                try {
                    future.get();
                }
                catch ( Exception e ) {
                    throw new RuntimeException( "Unable to aggregate results", e );
                }
            }

            return merge;
        }
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

        private TreeSet<ScanColumn> results;
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
         * Return true if the merge has results
         */
        public boolean hasResults() {
            return results != null && results.size() > 0;
        }


        /**
         * Get the set
         */
        public synchronized Set<ScanColumn> copyAndClear() {
            //create an immutable copy
            final Set<ScanColumn> toReturn = results ;
            results = new TreeSet<ScanColumn>();
            return toReturn;
        }


    }
}
