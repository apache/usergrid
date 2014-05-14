/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import org.apache.usergrid.persistence.schema.CollectionInfo;


import static org.apache.usergrid.persistence.Schema.getDefaultSchema;

import static org.usergrid.persistence.cassandra.Serializers.*;

public class QueryProcessor {

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


    public QueryProcessor( Query query, CollectionInfo collectionInfo, EntityManager em,
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
                            ue.toByteBuffer( startResult ) );
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

public interface QueryProcessor {
    int PAGE_SIZE = 1000;

    /**
     * Apply cursor position and sort order to this slice. This should only be invoke 
     * at evaluation time to ensure that the IR tree has already been fully constructed
     */
    void applyCursorAndSort(QuerySlice slice);

    CollectionInfo getCollectionInfo();

    /**
     * Return the node id from the cursor cache
     * @param nodeId
     * @return
     */
    ByteBuffer getCursorCache(int nodeId);

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
        public void visit( NotOperand op ) throws PersistenceException {

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
                    op.getLattitude().getFloatValue(), op.getLongitude().getFloatValue(), ++contextCount ) );
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

    QueryNode getFirstNode();

    /** @return the pageSizeHint */
    int getPageSizeHint(QueryNode node);

    Query getQuery();

    /** Return the iterator results, ordered if required */
    Results getResults(SearchVisitor visitor) throws Exception;

    void setQuery(Query query);
    
}
