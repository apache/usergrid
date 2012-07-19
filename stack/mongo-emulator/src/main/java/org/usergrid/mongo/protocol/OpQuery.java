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
package org.usergrid.mongo.protocol;

import static org.apache.commons.collections.MapUtils.getIntValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.antlr.runtime.ClassicToken;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.usergrid.mongo.utils.BSONUtils;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.SortDirection;
import org.usergrid.persistence.query.tree.AndOperand;
import org.usergrid.persistence.query.tree.Equal;
import org.usergrid.persistence.query.tree.GreaterThan;
import org.usergrid.persistence.query.tree.GreaterThanEqual;
import org.usergrid.persistence.query.tree.LessThan;
import org.usergrid.persistence.query.tree.LessThanEqual;
import org.usergrid.persistence.query.tree.Operand;
import org.usergrid.persistence.query.tree.OrOperand;

public class OpQuery extends OpCrud {

    int flags;
    int numberToSkip;
    int numberToReturn;
    BSONObject query;
    BSONObject returnFieldSelector;

    static Set<String> operators = new HashSet<String>();
    {
        operators.add("all");
        operators.add("and");
        operators.add("elemMatch");
        operators.add("exists");
        operators.add("gt");
        operators.add("gte");
        operators.add("in");
        operators.add("lt");
        operators.add("lte");
        operators.add("mod");
        operators.add("ne");
        operators.add("nin");
        operators.add("nor");
        operators.add("not");
        operators.add("or");
        operators.add("regex");
        operators.add("size");
        operators.add("type");
        operators.add("where");
    }

    public OpQuery() {
        opCode = OP_QUERY;
    }

    public Query toNativeQuery() {
        if (query == null) {
            return null;
        }

        BasicBSONObject query_expression = null;
        BasicBSONObject sort_order = null;

        Object o = query.get("$query");
        if (!(o instanceof BasicBSONObject)) {
            o = query.get("query");
        }
        if (o instanceof BasicBSONObject) {
            query_expression = (BasicBSONObject) o;
        }

        o = query.get("$orderby");
        if (!(o instanceof BasicBSONObject)) {
            o = query.get("orderby");
        }
        if (o instanceof BasicBSONObject) {
            sort_order = (BasicBSONObject) o;
        }

        if ((query_expression == null) && (query instanceof BasicBSONObject)) {
            query_expression = (BasicBSONObject) query;
            query_expression.removeField("$orderby");
            query_expression.removeField("$max");
            query_expression.removeField("$min");
        }

        if ((query_expression == null) && (sort_order == null)) {
            return null;
        }

        if (query_expression.size() == 0 && sort_order != null) {
            if (sort_order.size() == 0) {
                return null;
            }
            if ((sort_order.size() == 1) && sort_order.containsField("_id")) {
                return null;
            }
        }

        Query q = new Query();
        if (getNumberToReturn() > 0) {
            q.setLimit(getNumberToReturn());
        }

        if (query_expression != null) {
            Operand root = eval(query_expression);
            q.setRootOperand(root);
        }

        if (sort_order != null) {
            for (String sort : sort_order.keySet()) {
                if (!"_id".equals(sort)) {
                    int s = getIntValue(sort_order.toMap(), "_id", 1);
                    q.addSort(sort, s >= 0 ? SortDirection.ASCENDING
                            : SortDirection.DESCENDING);
                }
            }
        }

        return q;
    }

    private Operand eval(BSONObject exp) {
        Operand current = null;
        Object fieldValue = null;

        for (String field : exp.keySet()) {
            fieldValue = exp.get(field);

            if (field.startsWith("$")) {
                // same as OR with multiple values

                // same as OR with multiple values
                if ("$or".equals(field)) {
                    BasicBSONList values = (BasicBSONList) fieldValue;

                    int size = values.size();

                    Stack<Operand> expressions = new Stack<Operand>();

                    for (int i = 0; i < size; i++) {
                        expressions.push(eval((BSONObject) values.get(i)));
                    }

                    // we need to build a tree of expressions
                    while (expressions.size() > 1) {
                        OrOperand or = new OrOperand();
                        or.addChild(expressions.pop());
                        or.addChild(expressions.pop());
                        expressions.push(or);
                    }

                    current = expressions.pop();

                }

                else if ("$and".equals(field)) {

                    BasicBSONList values = (BasicBSONList) fieldValue;

                    int size = values.size();

                    Stack<Operand> expressions = new Stack<Operand>();

                    for (int i = 0; i < size; i++) {
                        expressions.push(eval((BSONObject) values.get(i)));
                    }

                    while (expressions.size() > 1) {
                        AndOperand and = new AndOperand();
                        and.addChild(expressions.pop());
                        and.addChild(expressions.pop());
                        expressions.push(and);
                    }

                    current = expressions.pop();
                }

            }
            // we have a nested object
            else if (fieldValue instanceof BSONObject) {
                current = handleOperand(field, (BSONObject) fieldValue);
            }

            else if (!field.equals("_id")) {
                Equal equality = new Equal(new ClassicToken(0, "="));
                equality.setProperty(field);
                equality.setLiteral(exp.get(field));

                current = equality;
            }
        }
        return current;
    }

    private Operand handleOperand(String sourceField, BSONObject exp) {

        Operand current = null;
        Object value = null;

        for (String field : exp.keySet()) {
            if (field.startsWith("$")) {
                if ("$gt".equals(field)) {
                    value = exp.get(field);

                    GreaterThan gt = new GreaterThan();
                    gt.setProperty(sourceField);
                    gt.setLiteral(value);

                    current = gt;
                } else if ("$gte".equals(field)) {
                    value = exp.get(field);

                    GreaterThanEqual gte = new GreaterThanEqual();
                    gte.setProperty(sourceField);
                    gte.setLiteral(exp.get(field));

                    current = gte;
                    // http://www.mongodb.org/display/DOCS/Advanced+Queries#AdvancedQueries-%3C%2C%3C%3D%2C%3E%2C%3E%3D
                    // greater than equals
                    // { "field" : { $gte: value } }
                } else if ("$lt".equals(field)) {
                    value = exp.get(field);

                    LessThan lt = new LessThan();
                    lt.setProperty(sourceField);
                    lt.setLiteral(value);

                    current = lt;
                } else if ("$lte".equals(field)) {
                    value = exp.get(field);

                    LessThanEqual lte = new LessThanEqual();
                    lte.setProperty(sourceField);
                    lte.setLiteral(value);

                    current = lte;
                } else if ("$in".equals(field)) {
                    value = exp.get(field);

                    BasicBSONList values = (BasicBSONList) value;

                    int size = values.size();

                    Stack<Operand> expressions = new Stack<Operand>();

                    for (int i = 0; i < size; i++) {
                        Equal equal = new Equal();
                        equal.setProperty(sourceField);
                        equal.setLiteral(values.get(i));

                        expressions.push(equal);
                    }

                    // we need to build a tree of expressions
                    while (expressions.size() > 1) {
                        OrOperand or = new OrOperand();
                        or.addChild(expressions.pop());
                        or.addChild(expressions.pop());
                        expressions.push(or);
                    }

                    current = expressions.pop();

                }

            }
        }

        return current;
    }

    // if ("$or".equals(field)) {
    // // http://www.mongodb.org/display/DOCS/OR+operations+in+query+expressions
    // // or
    // // { $or : [ { a : 1 } , { b : 2 } ] }
    // OrOperand or = new OrOperand(new ClassicToken(0, "="));
    //
    // if (parent != null) {
    // parent.addChild(or);
    // }
    //
    // append(or, (BSONObject) exp.get(field));
    //
    // } else if ("$not".equals(field)) {
    // //
    // http://www.mongodb.org/display/DOCS/Advanced+Queries#AdvancedQueries-Metaoperator%3A{{%24not}}
    // // not
    // // { name : { $not : /acme.*corp/i } }
    // } else if ("$ne".equals(field)) {
    // //
    // http://www.mongodb.org/display/DOCS/Advanced+Queries#AdvancedQueries-%24ne
    // // not equals
    // // { x : { $ne : 3 } }
    // }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getNumberToSkip() {
        return numberToSkip;
    }

    public void setNumberToSkip(int numberToSkip) {
        this.numberToSkip = numberToSkip;
    }

    public int getNumberToReturn() {
        return numberToReturn;
    }

    public void setNumberToReturn(int numberToReturn) {
        this.numberToReturn = numberToReturn;
    }

    public BSONObject getQuery() {
        return query;
    }

    public void setQuery(BSONObject query) {
        this.query = query;
    }

    public void setQuery(Map<?, ?> map) {
        query = new BasicBSONObject();
        query.putAll(map);
    }

    public BSONObject getReturnFieldSelector() {
        return returnFieldSelector;
    }

    public void setReturnFieldSelector(BSONObject returnFieldSelector) {
        this.returnFieldSelector = returnFieldSelector;
    }

    public void setReturnFieldSelector(Map<?, ?> map) {
        returnFieldSelector = new BasicBSONObject();
        returnFieldSelector.putAll(map);
    }

    @Override
    public void decode(ChannelBuffer buffer) throws IOException {
        super.decode(buffer);
        flags = buffer.readInt();
        fullCollectionName = readCString(buffer);
        numberToSkip = buffer.readInt();
        numberToReturn = buffer.readInt();
        query = BSONUtils.decoder().readObject(
                new ChannelBufferInputStream(buffer));
        if (buffer.readable()) {
            returnFieldSelector = BSONUtils.decoder().readObject(
                    new ChannelBufferInputStream(buffer));
        }
    }

    @Override
    public ChannelBuffer encode(ChannelBuffer buffer) {
        int l = 28; // 7 ints * 4 bytes

        ByteBuffer fullCollectionNameBytes = getCString(fullCollectionName);
        l += fullCollectionNameBytes.capacity();

        ByteBuffer queryBytes = encodeDocument(query);
        l += queryBytes.capacity();

        ByteBuffer returnFieldSelectorBytes = encodeDocument(returnFieldSelector);
        l += returnFieldSelectorBytes.capacity();

        messageLength = l;

        buffer = super.encode(buffer);

        buffer.writeInt(flags);

        buffer.writeBytes(fullCollectionNameBytes);

        buffer.writeInt(numberToSkip);
        buffer.writeInt(numberToReturn);

        buffer.writeBytes(queryBytes);

        buffer.writeBytes(returnFieldSelectorBytes);

        return buffer;
    }

    @Override
    public String toString() {
        return "OpQuery [flags=" + flags + ", fullCollectionName="
                + fullCollectionName + ", numberToSkip=" + numberToSkip
                + ", numberToReturn=" + numberToReturn + ", query=" + query
                + ", returnFieldSelector=" + returnFieldSelector + "]";
    }

}
