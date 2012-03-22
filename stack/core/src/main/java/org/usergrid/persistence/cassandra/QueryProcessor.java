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
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.split;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.SortPredicate;
import org.usergrid.persistence.query.ir.AndNode;
import org.usergrid.persistence.query.ir.NotNode;
import org.usergrid.persistence.query.ir.OrNode;
import org.usergrid.persistence.query.ir.QueryNode;
import org.usergrid.persistence.query.ir.SliceNode;
import org.usergrid.persistence.query.ir.WithinNode;
import org.usergrid.persistence.query.tree.AndOperand;
import org.usergrid.persistence.query.tree.ContainsOperand;
import org.usergrid.persistence.query.tree.Equal;
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
import org.usergrid.utils.StringUtils;

public class QueryProcessor {

    private static final Logger logger = LoggerFactory
            .getLogger(QueryProcessor.class);

    private Query query;

    private Operand rootOperand;
    private SortCache sortCache;
    private CursorCache cursorCache;
    private QueryNode rootNode;

    public QueryProcessor(Query query) {
        this.query = query;
        sortCache = new SortCache(query.getSortPredicates());
        cursorCache = new CursorCache(query.getCursor());
        rootOperand = query.getRootOperand();
        process();
    }

    private void process() {

        // no operand. Check for sorts
        if (rootOperand != null) {
            // visit the tree

            TreeEvaluator visitor = new TreeEvaluator();

            rootOperand.visit(visitor);

            rootNode = visitor.getRootNode();

            return;
        }

        if (sortCache.sorts.size() > 0) {
            SliceNode union = new SliceNode(0);

            // TODO create a union with orders union.
        }

    }

    public QueryNode getFirstNode() {
        return rootNode;
    }

    private class TreeEvaluator implements QueryVisitor {

        // stack for nodes that will be used to construct the tree and create
        // objects
        private Stack<QueryNode> nodes = new Stack<QueryNode>();

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
        public void visit(AndOperand op) {

            op.getLeft().visit(this);

            QueryNode leftResult = nodes.peek();

            op.getRight().visit(this);

            QueryNode rightResult = nodes.peek();

            // if the result of the left and right are the same, we don't want
            // to create an AND. We'll use the Union. Do nothing
            if (leftResult == rightResult) {
                return;
            }

            // otherwise create a new AND node from the result of the visit

            AndNode newNode = new AndNode(nodes.pop(), nodes.pop());

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
        public void visit(OrOperand op) {

            op.getLeft().visit(this);

            op.getRight().visit(this);

            // rewrite with the new Or operand
            OrNode orNode = new OrNode(nodes.pop(), nodes.pop());

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
        public void visit(NotOperand op) {

            // create a new context since any child of NOT will need to be
            // evaluated independently
            op.getOperation().visit(this);

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
        public void visit(ContainsOperand op) {
            String fieldName = appendSuffix(op.getProperty().getValue(),
                    "keywords");

            StringLiteral value = (StringLiteral) op.getString();

            SliceNode node = getUnionNode();

            node.setStart(fieldName, value, true);
            node.setFinish(fieldName, value, true);

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

            nodes.push(new WithinNode(propertyName,
                    op.getDistance().getValue(), op.getLattitude().getValue(),
                    op.getLongitude().getValue()));

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.LessThan)
         */
        @Override
        public void visit(LessThan op) {
            getUnionNode().setFinish(op.getProperty().getValue(),
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
        public void visit(LessThanEqual op) {
            getUnionNode().setFinish(op.getProperty().getValue(),
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
        public void visit(Equal op) {
            String fieldName = op.getProperty().getValue();
            Literal<?> literal = op.getLiteral();
            SliceNode node = getUnionNode();

            // this is an edge case. If we get more edge cases, we need to push
            // this down into the literals and let the objects
            // handle this
            if (literal instanceof StringLiteral) {

                StringLiteral stringLiteral = (StringLiteral) literal;

                String endValue = stringLiteral.getEndValue();

                if (endValue != null) {
                    node.setFinish(fieldName, endValue, false);
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
        public void visit(GreaterThan op) {
            getUnionNode().setStart(op.getProperty().getValue(),
                    op.getLiteral().getValue(), false);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.usergrid.persistence.query.tree.QueryVisitor#visit(org.usergrid
         * .persistence.query.tree.GreaterThanEqual)
         */
        @Override
        public void visit(GreaterThanEqual op) {
            getUnionNode().setStart(op.getProperty().getValue(),
                    op.getLiteral().getValue(), true);
        }

        /**
         * Return the current leaf node to add to if it exists. This means that
         * we can compress multile 'AND' operations and ranges into a single
         * node. Otherwise a new node is created and pushed to the stack
         * 
         * @return
         */
        private SliceNode getUnionNode() {

            if (nodes.size() == 0 || !(nodes.peek() instanceof SliceNode)) {
                SliceNode newUnion = new SliceNode(contextCount++);

                nodes.push(newUnion);

                return newUnion;
            }

            return (SliceNode) nodes.peek();

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

    }

    /**
     * Internal cursor parsing
     * 
     * @author tnine
     * 
     */
    public static class CursorCache {

        private Map<Integer, ByteBuffer> cursors = new HashMap<Integer, ByteBuffer>();

        private CursorCache(String cursorString) {

            // nothing to do
            if (cursorString == null || cursorString.indexOf(':') < 0) {
                return;
            }

            String[] cursorTokens = split(cursorString, '|');

            for (String c : cursorTokens) {

                String[] parts = split(c, ':');

                if (parts.length == 2 && isNotBlank(parts[1])) {

                    int hashCode = parseInt(parts[0]);

                    ByteBuffer cursorBytes = ByteBuffer
                            .wrap(decodeBase64(parts[1]));

                    cursors.put(hashCode, cursorBytes);
                }
            }

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
    }

    /**
     * The sort cache
     * 
     * @author tnine
     * 
     */
    public static class SortCache {
        private Map<String, SortPredicate> sorts = new HashMap<String, SortPredicate>();

        private SortCache(List<SortPredicate> sortPredicates) {
            for (SortPredicate sort : sortPredicates) {
                sorts.put(sort.getPropertyName(), sort);
            }
        }

        public SortPredicate getSort(String fieldName) {
            return sorts.get(fieldName);
        }
    }

}
