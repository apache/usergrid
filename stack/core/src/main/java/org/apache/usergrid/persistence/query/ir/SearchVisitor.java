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
package org.apache.usergrid.persistence.query.ir;


import java.util.Stack;
import java.util.UUID;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.QueryProcessor;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.apache.usergrid.persistence.query.ir.result.EmptyIterator;
import org.apache.usergrid.persistence.query.ir.result.IntersectionIterator;
import org.apache.usergrid.persistence.query.ir.result.OrderByIterator;
import org.apache.usergrid.persistence.query.ir.result.ResultIterator;
import org.apache.usergrid.persistence.query.ir.result.SecondaryIndexSliceParser;
import org.apache.usergrid.persistence.query.ir.result.SliceIterator;
import org.apache.usergrid.persistence.query.ir.result.StaticIdIterator;
import org.apache.usergrid.persistence.query.ir.result.SubtractionIterator;
import org.apache.usergrid.persistence.query.ir.result.UnionIterator;


/**
 * Simple search visitor that performs all the joining in memory for results.
 * <p/>
 * Subclasses will want to implement visiting SliceNode and WithinNode to actually perform the search on the Cassandra
 * indexes. This class can perform joins on all index entries that conform to the Results object
 *
 * @author tnine
 */
public abstract class SearchVisitor implements NodeVisitor {

    private static final SecondaryIndexSliceParser COLLECTION_PARSER = new SecondaryIndexSliceParser();

    protected final Query query;

    protected final QueryProcessor queryProcessor;

    protected final EntityManager em;

    protected final Stack<ResultIterator> results = new Stack<ResultIterator>();

    protected final String bucket;

    protected final EntityRef headEntity;
    protected final CassandraService cassandraService;
    protected final IndexBucketLocator indexBucketLocator;
    protected final UUID applicationId;


    /**
     * @param cassandraService
     * @param indexBucketLocator
     * @param applicationId
     * @param headEntity
     * @param queryProcessor
     */
    public SearchVisitor( final CassandraService cassandraService, final IndexBucketLocator indexBucketLocator,
                          final UUID applicationId, final EntityRef headEntity, QueryProcessor queryProcessor, final String bucket ) {


        this.cassandraService = cassandraService;
        this.indexBucketLocator = indexBucketLocator;
        this.applicationId = applicationId;
        this.headEntity = headEntity;
        this.query = queryProcessor.getQuery();
        this.queryProcessor = queryProcessor;
        this.em = queryProcessor.getEntityManager();
        this.bucket = bucket;
    }





    /** Return the results if they exist, null otherwise */
    public ResultIterator getResults() {
        return results.isEmpty() ? null : results.pop();
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.
     * persistence.query.ir.AndNode)
     */
    @Override
    public void visit( AndNode node ) throws Exception {
        node.getLeft().visit( this );
        node.getRight().visit( this );

        ResultIterator right = results.pop();
        ResultIterator left = results.pop();

        /**
         * NOTE: TN We should always maintain post order traversal of the tree. It
         * is required for sorting to work correctly
         */
        IntersectionIterator intersection = new IntersectionIterator( queryProcessor.getPageSizeHint( node ) );
        intersection.addIterator( left );
        intersection.addIterator( right );

        results.push( intersection );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.
     * persistence.query.ir.NotNode)
     */
    @Override
    public void visit( NotNode node ) throws Exception {
        node.getSubtractNode().visit( this );
        ResultIterator not = results.pop();

        node.getKeepNode().visit( this );
        ResultIterator keep = results.pop();

        SubtractionIterator subtraction = new SubtractionIterator( queryProcessor.getPageSizeHint( node ) );
        subtraction.setSubtractIterator( not );
        subtraction.setKeepIterator( keep );

        results.push( subtraction );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.
     * persistence.query.ir.OrNode)
     */
    @Override
    public void visit( OrNode node ) throws Exception {
        node.getLeft().visit( this );
        node.getRight().visit( this );

        ResultIterator right = results.pop();
        ResultIterator left = results.pop();

        final int nodeId = node.getId();

        UnionIterator union = new UnionIterator( queryProcessor.getPageSizeHint( node ), nodeId, queryProcessor.getCursorCache(nodeId  ) );

        if ( left != null ) {
            union.addIterator( left );
        }
        if ( right != null ) {
            union.addIterator( right );
        }

        results.push( union );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.persistence
     * .query.ir.OrderByNode)
     */
    @Override
    public void visit( OrderByNode orderByNode ) throws Exception {

        QuerySlice slice = orderByNode.getFirstPredicate().getAllSlices().iterator().next();

        queryProcessor.applyCursorAndSort( slice );

        QueryNode subOperations = orderByNode.getQueryOperations();

        ResultIterator subResults = null;

        if ( subOperations != null ) {
            //visit our sub operation
            subOperations.visit( this );

            subResults = results.pop();
        }

        ResultIterator orderIterator;

        /**
         * We have secondary sorts, we need to evaluate the candidate results and sort them in memory
         */
        if ( orderByNode.hasSecondarySorts() ) {

            //only order by with no query, start scanning the first field
            if ( subResults == null ) {
                QuerySlice firstFieldSlice = new QuerySlice( slice.getPropertyName(), -1 );
                subResults =
                        new SliceIterator( slice, secondaryIndexScan( orderByNode, firstFieldSlice ), COLLECTION_PARSER );
            }

            orderIterator = new OrderByIterator( slice, orderByNode.getSecondarySorts(), subResults, em,
                    queryProcessor.getPageSizeHint( orderByNode ) );
        }

        //we don't have multi field sorting, we can simply do intersection with a single scan range
        else {

            IndexScanner scanner;

            if ( slice.isComplete() ) {
                scanner = new NoOpIndexScanner();
            }
            else {
                scanner = secondaryIndexScan( orderByNode, slice );
            }

            SliceIterator joinSlice = new SliceIterator( slice, scanner, COLLECTION_PARSER);

            IntersectionIterator union = new IntersectionIterator( queryProcessor.getPageSizeHint( orderByNode ) );
            union.addIterator( joinSlice );

            if ( subResults != null ) {
                union.addIterator( subResults );
            }

            orderIterator = union;
        }

        // now create our intermediate iterator with our real results
        results.push( orderIterator );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.persistence
     * .query.ir.SliceNode)
     */
    @Override
    public void visit( SliceNode node ) throws Exception {
        IntersectionIterator intersections = new IntersectionIterator( queryProcessor.getPageSizeHint( node ) );

        for ( QuerySlice slice : node.getAllSlices() ) {
            IndexScanner scanner = secondaryIndexScan( node, slice );

            intersections.addIterator( new SliceIterator( slice, scanner, COLLECTION_PARSER) );
        }

        results.push( intersections );
    }


    /**
     * Create a secondary index scan for the given slice node. DOES NOT apply to the "all" case. This should only
     * generate a slice for secondary property scanning
     */
    protected abstract IndexScanner secondaryIndexScan( QueryNode node, QuerySlice slice ) throws Exception;


    @Override
    public void visit( UuidIdentifierNode uuidIdentifierNode ) {
        this.results.push( new StaticIdIterator( uuidIdentifierNode.getUuid() ) );
    }


    @Override
    public void visit( EmailIdentifierNode emailIdentifierNode ) throws Exception {
        EntityRef user = queryProcessor.getEntityManager().getUserByIdentifier( emailIdentifierNode.getIdentifier() );

        if ( user == null ) {
            this.results.push( new EmptyIterator() );
            return;
        }

        this.results.push( new StaticIdIterator( user.getUuid() ) );
    }
}
