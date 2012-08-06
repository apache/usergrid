/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.cassandra;

import static java.lang.Integer.parseInt;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.usergrid.persistence.Schema.getDefaultSchema;
import static org.usergrid.utils.ConversionUtils.bytes;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.split;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityPropertyComparator;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.SortDirection;
import org.usergrid.persistence.Query.SortPredicate;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.exceptions.NoFullTextIndexException;
import org.usergrid.persistence.exceptions.NoIndexException;
import org.usergrid.persistence.exceptions.PersistenceException;
import org.usergrid.persistence.query.ir.AndNode;
import org.usergrid.persistence.query.ir.NotNode;
import org.usergrid.persistence.query.ir.OrNode;
import org.usergrid.persistence.query.ir.QueryNode;
import org.usergrid.persistence.query.ir.QuerySlice;
import org.usergrid.persistence.query.ir.SliceNode;
import org.usergrid.persistence.query.ir.WithinNode;
import org.usergrid.persistence.query.tree.AndOperand;
import org.usergrid.persistence.query.tree.ContainsOperand;
import org.usergrid.persistence.query.tree.Equal;
import org.usergrid.persistence.query.tree.EqualityOperand;
import org.usergrid.persistence.query.tree.GreaterThan;
import org.usergrid.persistence.query.tree.GreaterThanEqual;
import org.usergrid.persistence.query.tree.LessThan;
import org.usergrid.persistence.query.tree.LessThanEqual;
import org.usergrid.persistence.query.tree.Literal;
import org.usergrid.persistence.query.tree.NotOperand;
import org.usergrid.persistence.query.tree.Operand;
import org.usergrid.persistence.query.tree.OrOperand;
import org.usergrid.persistence.query.tree.QueryVisitor;
import org.usergrid.persistence.query.tree.StringLiteral;
import org.usergrid.persistence.query.tree.WithinOperand;
import org.usergrid.persistence.schema.CollectionInfo;
import org.usergrid.utils.StringUtils;

public class QueryProcessor {

    private static final Logger logger = LoggerFactory
            .getLogger(QueryProcessor.class);

    private Operand rootOperand;
    private SortCache sortCache;
    private CursorCache cursorCache;
    private QueryNode rootNode;
    private String entityType;
    private CollectionInfo collectionInfo;

    public QueryProcessor(Query query, CollectionInfo collectionInfo)
            throws PersistenceException {
        sortCache = new SortCache(query.getSortPredicates());
        cursorCache = new CursorCache(query.getCursor());
        rootOperand = query.getRootOperand();
        entityType = query.getEntityType();
        this.collectionInfo = collectionInfo;
        process();
    }

    private void process() throws PersistenceException {

        // no operand. Check for sorts
        if (rootOperand != null) {
            // visit the tree

            TreeEvaluator visitor = new TreeEvaluator();

            rootOperand.visit(visitor);

            rootNode = visitor.getRootNode();

            return;
        }

        // see if we have sorts, if so, we can add them all as a single node at
        // the root
        if (sortCache.hasSorts()) {
            rootNode = sortCache.generateSorts();
        }

    }

    public QueryNode getFirstNode() {
        return rootNode;
    }

    /**
     * Perform an in memory sort of the entities
     * 
     * @param entities
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Entity> sort(List<Entity> entities) {

        if ((entities != null) && (sortCache.sorts.size() > 0)) {
            // Performing in memory sort
            logger.info("Performing in-memory sort of " + entities.size()
                    + " entities");
            ComparatorChain chain = new ComparatorChain();
            for (SortPredicate sort : sortCache.sorts.values()) {
                chain.addComparator(
                        new EntityPropertyComparator(sort.getPropertyName()),
                        sort.getDirection() == SortDirection.DESCENDING);
            }
            Collections.sort(entities, chain);
        }
        return entities;
    }

    /**
     * Apply cursor position and sort order to this slice. This should only be
     * invoke at evaluation time to ensure that the IR tree has already been
     * fully constructed
     * 
     * @param slice
     */
    public void applyCursorAndSort(QuerySlice slice) {
        // apply the sort first, since this can change the hash code
        SortPredicate sort = sortCache.getSort(slice.getPropertyName());

        if (sort != null) {
            slice.setReversed(sort.getDirection() == SortDirection.DESCENDING);
        }
        // apply the cursor
        ByteBuffer cursor = cursorCache.getCursorBytes(slice.hashCode());

        if (cursor != null) {
            slice.setCursor(cursor);
        }

    }

    /**
     * Update the cursor for the slice with the new value
     * 
     * @param slice
     */
    public void updateCursor(QuerySlice slice, ByteBuffer value) {

        cursorCache.setNextCursor(slice.hashCode(), value);

    }

    /**
     * Get the cursor as a string. This should only be invoked when
     * 
     * @return
     */
    public String getCursor() {
        return cursorCache.asString();
    }

    /**
     * Update the cursor cache with the new cursor value for the given slice and
     * value. The cache can then be serialized to a string and returned to the
     * user after all tree operations have completed.
     * 
     * @param slice
     * @param cursorValue
     */
    public void updateCursor(QuerySlice slice, String cursorValue) {
        // TODO T.N. This is inefficient, we should deal with ByteBuffers
        // internally and and only expose
        // strings at the interface level
        cursorCache.setNextCursor(slice.hashCode(),
                ByteBuffer.wrap(decodeBase64(cursorValue)));
    }

    private class TreeEvaluator implements QueryVisitor {

        // stack for nodes that will be used to construct the tree and create
        // objects
        private Stack<QueryNode> nodes = new Stack<QueryNode>();

        private Schema schema = getDefaultSchema();

        private int contextCount = -1;

        /**
         * Get the root node in our tree for runtime evaluation
         * 
         * @return
         */
        public QueryNode getRootNode() {
            return nodes.pop();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.AndOperand)
         */
        @Override
        public void visit(AndOperand op) throws PersistenceException {

            op.getLeft().visit(this);

            QueryNode leftResult = nodes.peek();

            op.getRight().visit(this);

            QueryNode rightResult = nodes.peek();

            // if the result of the left and right are the same, we don't want
            // to create an AND. We'll use the same SliceNode. Do nothing
            if (leftResult == rightResult) {
                return;
            }

            // otherwise create a new AND node from the result of the visit

            QueryNode right = nodes.pop();
            QueryNode left = nodes.pop();

            AndNode newNode = new AndNode(left, right);

            nodes.push(newNode);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.OrOperand)
         */
        @Override
        public void visit(OrOperand op) throws PersistenceException {

            // we need to create a new slicenode for the children of this
            // operation

            Operand left = op.getLeft();
            Operand right = op.getRight();

            // we only create a new slice node if our children are && and ||
            // operations
            createNewSlice(left);

            left.visit(this);

            // we only create a new slice node if our children are && and ||
            // operations
            createNewSlice(right);

            right.visit(this);

            QueryNode rightResult = nodes.pop();
            QueryNode leftResult = nodes.pop();

            // rewrite with the new Or operand
            OrNode orNode = new OrNode(leftResult, rightResult);

            nodes.push(orNode);

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.NotOperand)
         */
        @Override
        public void visit(NotOperand op) throws PersistenceException {

            // create a new context since any child of NOT will need to be
            // evaluated independently

            Operand child = op.getOperation();

            createNewSlice(child);

            child.visit(this);

            NotNode not = new NotNode(nodes.pop());
            nodes.push(not);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.ContainsOperand)
         */
        @Override
        public void visit(ContainsOperand op) throws NoFullTextIndexException {

            String propertyName = op.getProperty().getValue();

            if (!schema.isPropertyFulltextIndexed(entityType, propertyName)) {
                throw new NoFullTextIndexException(entityType, propertyName);
            }

            String fieldName = appendSuffix(op.getProperty().getValue(),
                    "keywords");

            StringLiteral string = op.getString();

            SliceNode node = getUnionNode(op);

            node.setStart(fieldName, string.getValue(), true);
            node.setFinish(fieldName, string.getEndValue(), true);

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.WithinOperand)
         */
        @Override
        public void visit(WithinOperand op) {

            // change the property name to coordinates
            String propertyName = appendSuffix(op.getProperty().getValue(),
                    "coordinates");

            nodes.push(new WithinNode(propertyName, op.getDistance()
                    .getFloatValue(), op.getLattitude().getFloatValue(), op
                    .getLongitude().getFloatValue()));

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.LessThan)
         */
        @Override
        public void visit(LessThan op) throws NoIndexException {
            String propertyName = op.getProperty().getValue();

            checkIndexed(propertyName);

            getUnionNode(op).setFinish(propertyName,
                    op.getLiteral().getValue(), false);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.LessThanEqual)
         */
        @Override
        public void visit(LessThanEqual op) throws NoIndexException {

            String propertyName = op.getProperty().getValue();

            checkIndexed(propertyName);

            getUnionNode(op).setFinish(propertyName,
                    op.getLiteral().getValue(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.Equal)
         */
        @Override
        public void visit(Equal op) throws NoIndexException {
            String fieldName = op.getProperty().getValue();

            checkIndexed(fieldName);

            Literal<?> literal = op.getLiteral();
            SliceNode node = getUnionNode(op);

            // this is an edge case. If we get more edge cases, we need to push
            // this down into the literals and let the objects
            // handle this
            if (literal instanceof StringLiteral) {

                StringLiteral stringLiteral = (StringLiteral) literal;

                String endValue = stringLiteral.getEndValue();

                if (endValue != null) {
                    node.setFinish(fieldName, endValue, true);
                }
            } else {
                node.setFinish(fieldName, literal.getValue(), true);
            }

            node.setStart(fieldName, literal.getValue(), true);

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.GreaterThan)
         */
        @Override
        public void visit(GreaterThan op) throws NoIndexException {
            String propertyName = op.getProperty().getValue();

            checkIndexed(propertyName);

            getUnionNode(op).setStart(propertyName, op.getLiteral().getValue(),
                    false);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.GreaterThanEqual)
         */
        @Override
        public void visit(GreaterThanEqual op) throws NoIndexException {
            String propertyName = op.getProperty().getValue();

            checkIndexed(propertyName);

            getUnionNode(op).setStart(propertyName, op.getLiteral().getValue(),
                    true);
        }

        /**
         * Return the current leaf node to add to if it exists. This means that
         * we can compress multile 'AND' operations and ranges into a single
         * node. Otherwise a new node is created and pushed to the stack
         * 
         * @param current
         *            The current operand node
         * @return
         */
        private SliceNode getUnionNode(Operand current) {

            /**
             * we only create a new slice node in 3 situations 1. No nodes exist
             * 2. The parent node is not an AND node. Meaning we can't add this
             * slice to the current set of slices 3. Our current top of stack is
             * not a slice node.
             */
            // no nodes exist
            if (nodes.size() == 0 || !(nodes.peek() instanceof SliceNode)) {
                return newSliceNode();
            }

            return (SliceNode) nodes.peek();

        }

        /**
         * The new slice node
         * 
         * @return
         */
        private SliceNode newSliceNode() {
            SliceNode sliceNode = new SliceNode(++contextCount);

            nodes.push(sliceNode);

            return sliceNode;
        }

        /**
         * Create a new slice if one will be required within the context of this
         * node
         * 
         * @param child
         */
        private void createNewSlice(Operand child) {
            if (child instanceof EqualityOperand || child instanceof AndOperand
                    || child instanceof ContainsOperand) {
                newSliceNode();
            }

        }

        /**
         * 
         * @param str
         * @param suffix
         * @return
         */
        private String appendSuffix(String str, String suffix) {
            if (StringUtils.isNotEmpty(str)) {
                if (!str.endsWith("." + suffix)) {
                    str += "." + suffix;
                }
            } else {
                str = suffix;
            }
            return str;
        }

        private void checkIndexed(String propertyName) throws NoIndexException {

            if (!schema.isPropertyIndexed(entityType, propertyName)
                    && collectionInfo != null
                    && !collectionInfo.isSubkeyProperty(propertyName)) {
                throw new NoIndexException(entityType, propertyName);
            }
        }

    }

    /**
     * Internal cursor parsing
     * 
     * @author tnine
     * 
     */
    public static class CursorCache {

        private Map<Integer, ByteBuffer> cursors = new HashMap<Integer, ByteBuffer>();

        /**
         * Create a new cursor cache from the string if passed
         * 
         * @param cursorString
         */
        private CursorCache(String cursorString) {

            // nothing to do
            if (cursorString == null || cursorString.indexOf(':') < 0) {
                return;
            }

            String[] cursorTokens = split(cursorString, '|');

            for (String c : cursorTokens) {

                String[] parts = split(c, ':');

                if (parts.length >= 1) {

                    int hashCode = parseInt(parts[0]);

                    ByteBuffer cursorBytes = null;

                    if (parts.length == 2) {
                        cursorBytes = ByteBuffer.wrap(decodeBase64(parts[1]));
                    } else {
                        cursorBytes = ByteBuffer.allocate(0);
                    }

                    cursors.put(hashCode, cursorBytes);
                }
            }

        }

        /**
         * Set the cursor with the given hash and the new byte buffer
         * 
         * @param sliceHash
         * @param newCursor
         */
        public void setNextCursor(int sliceHash, ByteBuffer newCursor) {
            cursors.put(sliceHash, newCursor);
        }

        /**
         * Get the cursor by the hashcode of the slice
         * 
         * @param sliceHash
         * @return
         */
        public ByteBuffer getCursorBytes(int sliceHash) {
            return cursors.get(sliceHash);
        }

        /**
         * Turn the cursor cache into a string
         * 
         * @return
         */
        public String asString() {
            /**
             * No cursors to return
             */
            if (cursors.size() == 0) {
                return null;
            }

            StringBuffer buff = new StringBuffer();

            for (Entry<Integer, ByteBuffer> entry : cursors.entrySet()) {
                buff.append(entry.getKey());
                buff.append(":");
                buff.append(encodeBase64URLSafeString(bytes(entry.getValue())));
                buff.append("|");
            }

            // trim off the last pipe
            buff.setLength(buff.length() - 1);

            return buff.toString();
        }
    }

    /**
     * The sort cache
     * 
     * @author tnine
     * 
     */
    public static class SortCache {
        private Map<String, SortPredicate> sorts = new HashMap<String, SortPredicate>();
        private List<SortPredicate> originalValue;

        private SortCache(List<SortPredicate> sortPredicates) {
            originalValue = sortPredicates;
            for (SortPredicate sort : sortPredicates) {
                sorts.put(sort.getPropertyName(), sort);
            }
        }

        public SortPredicate getSort(String fieldName) {
            return sorts.get(fieldName);
        }

        /**
         * Return true if we have sorts
         * 
         * @return
         */
        public boolean hasSorts() {
            return sorts.size() > 0;
        }

        /**
         * Generate a slice node with scan ranges for all the properties in our
         * sort cache
         * 
         * @return
         */
        public SliceNode generateSorts() {

            // the value is irrelevant since we'll only ever have 1 slice node
            // if this is called
            SliceNode node = new SliceNode(0);

            for (SortPredicate predicate : originalValue) {
                node.setStart(predicate.getPropertyName(), null, true);
                node.setFinish(predicate.getPropertyName(), null, true);
            }

            return node;
        }
    }

}
