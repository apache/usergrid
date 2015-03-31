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
package org.apache.usergrid.persistence.cassandra;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.PersistenceException;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.exceptions.NoFullTextIndexException;
import org.apache.usergrid.persistence.index.exceptions.NoIndexException;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Query.SortDirection;
import org.apache.usergrid.persistence.index.query.Query.SortPredicate;
import org.apache.usergrid.persistence.index.query.tree.AndOperand;
import org.apache.usergrid.persistence.index.query.tree.ContainsOperand;
import org.apache.usergrid.persistence.index.query.tree.Equal;
import org.apache.usergrid.persistence.index.query.tree.EqualityOperand;
import org.apache.usergrid.persistence.index.query.tree.GreaterThan;
import org.apache.usergrid.persistence.index.query.tree.GreaterThanEqual;
import org.apache.usergrid.persistence.index.query.tree.LessThan;
import org.apache.usergrid.persistence.index.query.tree.LessThanEqual;
import org.apache.usergrid.persistence.index.query.tree.Literal;
import org.apache.usergrid.persistence.index.query.tree.NotOperand;
import org.apache.usergrid.persistence.index.query.tree.Operand;
import org.apache.usergrid.persistence.index.query.tree.OrOperand;
import org.apache.usergrid.persistence.index.query.tree.QueryVisitor;
import org.apache.usergrid.persistence.index.query.tree.StringLiteral;
import org.apache.usergrid.persistence.index.query.tree.WithinOperand;
import org.apache.usergrid.persistence.query.ir.AllNode;
import org.apache.usergrid.persistence.query.ir.AndNode;
import org.apache.usergrid.persistence.query.ir.EmailIdentifierNode;
import org.apache.usergrid.persistence.query.ir.NameIdentifierNode;
import org.apache.usergrid.persistence.query.ir.NotNode;
import org.apache.usergrid.persistence.query.ir.OrNode;
import org.apache.usergrid.persistence.query.ir.OrderByNode;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import org.apache.usergrid.persistence.query.ir.SliceNode;
import org.apache.usergrid.persistence.query.ir.UuidIdentifierNode;
import org.apache.usergrid.persistence.query.ir.WithinNode;
import org.apache.usergrid.persistence.query.ir.result.ResultIterator;
import org.apache.usergrid.persistence.query.ir.result.ResultsLoader;
import org.apache.usergrid.persistence.query.ir.result.ResultsLoaderFactory;
import org.apache.usergrid.persistence.query.ir.result.ScanColumn;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueryProcessorImpl implements QueryProcessor {

    public static final int PAGE_SIZE = 1000;
    private static final Logger logger = LoggerFactory.getLogger( QueryProcessor.class );

    private static final Schema SCHEMA = getDefaultSchema();

    private final CollectionInfo collectionInfo;
    private final EntityManager em;
    private final ResultsLoaderFactory loaderFactory;

    private Operand rootOperand;
    private List<SortPredicate> sorts;
    private CursorCache cursorCache;
    private QueryNode rootNode;
    private String entityType;

    private int size;
    private Query query;
    private int pageSizeHint;


    public QueryProcessorImpl( Query query, CollectionInfo collectionInfo, EntityManager em,
            ResultsLoaderFactory loaderFactory ) throws PersistenceException {
        setQuery( query );
        this.collectionInfo = collectionInfo;
        this.em = em;
        this.loaderFactory = loaderFactory;
        process();
    }


    public Query getQuery() {
        return query;
    }


    public void setQuery( Query query ) {
        this.sorts = query.getSortPredicates();
        this.cursorCache = new CursorCache( query.getCursor() );
        this.rootOperand = query.getRootOperand();
        this.entityType = query.getEntityType();
        this.size = query.getLimit();
        this.query = query;
    }


    public CollectionInfo getCollectionInfo() {
        return collectionInfo;
    }


    private void process() throws PersistenceException {

        int opCount = 0;

        // no operand. Check for sorts
        if ( rootOperand != null ) {
            // visit the tree

            TreeEvaluator visitor = new TreeEvaluator();

            rootOperand.visit( visitor );

            rootNode = visitor.getRootNode();

            opCount = visitor.getSliceCount();
        }

        // see if we have sorts, if so, we can add them all as a single node at
        // the root
        if ( sorts.size() > 0 ) {

            OrderByNode order = generateSorts( opCount );

            opCount += order.getFirstPredicate().getAllSlices().size();

            rootNode = order;
        }


        //if we still don't have a root node, no query nor order by was specified,
        // just use the all node or the identifiers
        if ( rootNode == null ) {


            //a name alias or email alias was specified
            if ( query.containsSingleNameOrEmailIdentifier() ) {

                Identifier ident = query.getSingleIdentifier();

                //an email was specified.  An edge case that only applies to users.  This is fulgy to put here,
                // but required
                if ( query.getEntityType().equals( User.ENTITY_TYPE ) && ident.isEmail() ) {
                    rootNode = new EmailIdentifierNode( ident );
                }

                //use the ident with the default alias.  could be an email
                else {
                    rootNode = new NameIdentifierNode( ident.getName() );
                }
            }
            //a uuid was specified
            else if ( query.containsSingleUuidIdentifier() ) {
                rootNode = new UuidIdentifierNode( query.getSingleUuidIdentifier() );
            }


            //nothing was specified, order it by uuid
            else {


                //this is a bit ugly, but how we handle the start parameter
                UUID startResult = query.getStartResult();

                boolean startResultSet = startResult != null;

                AllNode allNode = new AllNode( 0, startResultSet );

                if ( startResultSet ) {
                    cursorCache.setNextCursor( allNode.getSlice().hashCode(),
                            Serializers.ue.toByteBuffer( startResult ) );
                }

                rootNode = allNode;
            }
        }

        if ( opCount > 1 ) {
            pageSizeHint = PAGE_SIZE;
        }
        else {
            pageSizeHint = Math.min( size, PAGE_SIZE );
        }
    }


    public QueryNode getFirstNode() {
        return rootNode;
    }


    /**
     * Apply cursor position and sort order to this slice. This should only be invoke at evaluation time to ensure that
     * the IR tree has already been fully constructed
     */
    public void applyCursorAndSort( QuerySlice slice ) {
        // apply the sort first, since this can change the hash code
        SortPredicate sort = getSort( slice.getPropertyName() );

        if ( sort != null ) {
            boolean isReversed = sort.getDirection() == SortDirection.DESCENDING;

            //we're reversing the direction of this slice, reverse the params as well
            if ( isReversed != slice.isReversed() ) {
                slice.reverse();
            }
        }
        // apply the cursor
        ByteBuffer cursor = cursorCache.getCursorBytes( slice.hashCode() );

        if ( cursor != null ) {
            slice.setCursor( cursor );
        }
    }


    /**
     * Return the node id from the cursor cache
     * @param nodeId
     * @return
     */
    public ByteBuffer getCursorCache(int nodeId){
        return cursorCache.getCursorBytes( nodeId );
    }


    private SortPredicate getSort( String propertyName ) {
        for ( SortPredicate sort : sorts ) {
            if ( sort.getPropertyName().equals( propertyName ) ) {
                return sort;
            }
        }
        return null;
    }


    /** Return the iterator results, ordered if required */
    public Results getResults( SearchVisitor visitor ) throws Exception {
        // if we have no order by just load the results

        if ( rootNode == null ) {
            return null;
        }

        rootNode.visit( visitor );

        ResultIterator itr = visitor.getResults();

        List<ScanColumn> entityIds = new ArrayList<ScanColumn>( Math.min( size, Query.MAX_LIMIT ) );

        CursorCache resultsCursor = new CursorCache();

        while ( entityIds.size() < size && itr.hasNext() ) {
            entityIds.addAll( itr.next() );
        }

        //set our cursor, we paged through more entities than we want to return
        if ( entityIds.size() > 0 ) {
            int resultSize = Math.min( entityIds.size(), size );
            entityIds = entityIds.subList( 0, resultSize );

            if ( resultSize == size ) {
                itr.finalizeCursor( resultsCursor, entityIds.get( resultSize - 1 ).getUUID() );
            }
        }
        if (logger.isDebugEnabled()) {
        	logger.debug("Getting result for query: [{}],  returning entityIds size: {}",
                    getQuery(), entityIds.size());
        }

        final ResultsLoader loader = loaderFactory.getResultsLoader( em, query, query.getResultsLevel() );
        final Results results = loader.getResults( entityIds, query.getEntityType() );

        if ( results == null ) {
            return null;
        }

        // now we need to set the cursor from our tree evaluation for return
        results.setCursor( resultsCursor.asString() );

        results.setQuery( query );
//        results.setQueryProcessor( this );
//        results.setSearchVisitor( visitor );

        return results;
    }


    private class TreeEvaluator implements QueryVisitor {

        // stack for nodes that will be used to construct the tree and create
        // objects
        private CountingStack<QueryNode> nodes = new CountingStack<QueryNode>();


        private int contextCount = -1;


        /** Get the root node in our tree for runtime evaluation */
        public QueryNode getRootNode() {
            return nodes.peek();
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.AndOperand)
         */
        @Override
        public void visit( AndOperand op ) throws IndexException {

            op.getLeft().visit( this );

            QueryNode leftResult = nodes.peek();

            op.getRight().visit( this );

            QueryNode rightResult = nodes.peek();

            // if the result of the left and right are the same, we don't want
            // to create an AND. We'll use the same SliceNode. Do nothing
            if ( leftResult == rightResult ) {
                return;
            }

            // otherwise create a new AND node from the result of the visit

            QueryNode right = nodes.pop();
            QueryNode left = nodes.pop();

            AndNode newNode = new AndNode( left, right );

            nodes.push( newNode );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.OrOperand)
         */
        @Override
        public void visit( OrOperand op ) throws IndexException {

            // we need to create a new slicenode for the children of this
            // operation

            Operand left = op.getLeft();
            Operand right = op.getRight();

            // we only create a new slice node if our children are && and ||
            // operations
            createNewSlice( left );

            left.visit( this );

            // we only create a new slice node if our children are && and ||
            // operations
            createNewSlice( right );

            right.visit( this );

            QueryNode rightResult = nodes.pop();
            QueryNode leftResult = nodes.pop();

            // rewrite with the new Or operand
            OrNode orNode = new OrNode( leftResult, rightResult,  ++contextCount );

            nodes.push( orNode );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.NotOperand)
         */
        @Override
        public void visit( NotOperand op ) throws IndexException {

            // create a new context since any child of NOT will need to be
            // evaluated independently
            Operand child = op.getOperation();
            createNewSlice( child );
            child.visit( this );

            nodes.push( new NotNode( nodes.pop(), new AllNode( ++contextCount, false ) ) );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.ContainsOperand)
         */
        @Override
        public void visit( ContainsOperand op ) throws NoFullTextIndexException {

            String propertyName = op.getProperty().getValue();

            if ( !SCHEMA.isPropertyFulltextIndexed( entityType, propertyName ) ) {
                throw new NoFullTextIndexException( entityType, propertyName );
            }

            StringLiteral string = op.getString();

            String indexName = op.getProperty().getIndexedValue();

            SliceNode node = null;

            // sdg - if left & right have same field name, we need to create a new
            // slice
            if ( !nodes.isEmpty() && nodes.peek() instanceof SliceNode
                    && ( ( SliceNode ) nodes.peek() ).getSlice( indexName ) != null ) {
                node = newSliceNode();
            }
            else {
                node = getUnionNode( op );
            }

            String fieldName = op.getProperty().getIndexedValue();

            node.setStart( fieldName, string.getValue(), true );
            node.setFinish( fieldName, string.getEndValue(), true );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.WithinOperand)
         */
        @Override
        public void visit( WithinOperand op ) {

            // change the property name to coordinates
            nodes.push( new WithinNode( op.getProperty().getIndexedName(), op.getDistance().getFloatValue(),
                    op.getLatitude().getFloatValue(), op.getLongitude().getFloatValue(), ++contextCount ) );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.LessThan)
         */
        @Override
        public void visit( LessThan op ) throws NoIndexException {
            String propertyName = op.getProperty().getValue();

            checkIndexed( propertyName );

            getUnionNode( op ).setFinish( propertyName, op.getLiteral().getValue(), false );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.LessThanEqual)
         */
        @Override
        public void visit( LessThanEqual op ) throws NoIndexException {

            String propertyName = op.getProperty().getValue();

            checkIndexed( propertyName );

            getUnionNode( op ).setFinish( propertyName, op.getLiteral().getValue(), true );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.Equal)
         */
        @Override
        public void visit( Equal op ) throws NoIndexException {
            String fieldName = op.getProperty().getValue();

            checkIndexed( fieldName );

            Literal<?> literal = op.getLiteral();
            SliceNode node = getUnionNode( op );

            // this is an edge case. If we get more edge cases, we need to push
            // this down into the literals and let the objects
            // handle this
            if ( literal instanceof StringLiteral ) {

                StringLiteral stringLiteral = ( StringLiteral ) literal;

                String endValue = stringLiteral.getEndValue();

                if ( endValue != null ) {
                    node.setFinish( fieldName, endValue, true );
                }
            }
            else {
                node.setFinish( fieldName, literal.getValue(), true );
            }

            node.setStart( fieldName, literal.getValue(), true );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.GreaterThan)
         */
        @Override
        public void visit( GreaterThan op ) throws NoIndexException {
            String propertyName = op.getProperty().getValue();

            checkIndexed( propertyName );

            getUnionNode( op ).setStart( propertyName, op.getLiteral().getValue(), false );
        }


        /*
         * (non-Javadoc)
         *
         * @see org.apache.usergrid.persistence.query.tree.QueryVisitor#visit(org.apache.usergrid
         * .persistence.query.tree.GreaterThanEqual)
         */
        @Override
        public void visit( GreaterThanEqual op ) throws NoIndexException {
            String propertyName = op.getProperty().getValue();

            checkIndexed( propertyName );

            getUnionNode( op ).setStart( propertyName, op.getLiteral().getValue(), true );
        }


        /**
         * Return the current leaf node to add to if it exists. This means that we can compress multiple 'AND'
         * operations and ranges into a single node. Otherwise a new node is created and pushed to the stack
         *
         * @param current The current operand node
         */
        private SliceNode getUnionNode( EqualityOperand current ) {

            /**
             * we only create a new slice node in 3 situations 1. No nodes exist 2.
             * The parent node is not an AND node. Meaning we can't add this slice to
             * the current set of slices 3. Our current top of stack is not a slice
             * node.
             */
            // no nodes exist
            if ( nodes.size() == 0 || !( nodes.peek() instanceof SliceNode ) ) {
                return newSliceNode();
            }

            return ( SliceNode ) nodes.peek();
        }


        /** The new slice node */
        private SliceNode newSliceNode() {
            SliceNode sliceNode = new SliceNode( ++contextCount );

            nodes.push( sliceNode );

            return sliceNode;
        }


        /** Create a new slice if one will be required within the context of this node */
        private void createNewSlice( Operand child ) {
            if ( child instanceof EqualityOperand || child instanceof AndOperand || child instanceof ContainsOperand ) {
                newSliceNode();
            }
        }


        public int getSliceCount() {
            return nodes.getSliceCount();
        }

        @Override
        public QueryBuilder getQueryBuilder() {
            throw new UnsupportedOperationException("Not supported by this vistor implementation.");
        }

        @Override
        public FilterBuilder getFilterBuilder() {
            throw new UnsupportedOperationException("Not supported by this vistor implementation.");
        }
    }


    private static class CountingStack<T> extends Stack<T> {

        private int count = 0;
        private static final long serialVersionUID = 1L;


        /* (non-Javadoc)
         * @see java.util.Stack#pop()
         */
        @Override
        public synchronized T pop() {
            T entry = super.pop();

            if ( entry instanceof SliceNode ) {
                count += ( ( SliceNode ) entry ).getAllSlices().size();
            }

            return entry;
        }


        public int getSliceCount() {

            Iterator<T> itr = this.iterator();

            T entry;

            while ( itr.hasNext() ) {
                entry = itr.next();

                if ( entry instanceof SliceNode ) {
                    count += ( ( SliceNode ) entry ).getAllSlices().size();
                }
            }

            return count;
        }
    }


    /** @return the pageSizeHint */
    public int getPageSizeHint( QueryNode node ) {
        /*****
         * DO NOT REMOVE THIS PIECE OF CODE!!!!!!!!!!!
         * It is crucial that the root iterator only needs the result set size per page
         * otherwise our cursor logic will fail when passing cursor data to the leaf nodes
         *******/
        if(node == rootNode){
            return size;
        }

        return pageSizeHint;
    }


    /** Generate a slice node with scan ranges for all the properties in our sort cache */
    private OrderByNode generateSorts( int opCount ) throws NoIndexException {

        // the value is irrelevant since we'll only ever have 1 slice node
        // if this is called
        SliceNode slice = new SliceNode( opCount );

        SortPredicate first = sorts.get( 0 );

        String propertyName = first.getPropertyName();

        checkIndexed( propertyName );

        slice.setStart( propertyName, null, true );
        slice.setFinish( propertyName, null, true );


        for ( int i = 1; i < sorts.size(); i++ ) {
            checkIndexed( sorts.get( i ).getPropertyName() );
        }


        return new OrderByNode( slice, sorts.subList( 1, sorts.size() ), rootNode );
    }


    private void checkIndexed( String propertyName ) throws NoIndexException {

        if ( propertyName == null || propertyName.isEmpty() || ( !SCHEMA.isPropertyIndexed( entityType, propertyName )
                && collectionInfo != null ) ) {
            throw new NoIndexException( entityType, propertyName );
        }
    }


    public EntityManager getEntityManager() {
        return em;
    }
}
