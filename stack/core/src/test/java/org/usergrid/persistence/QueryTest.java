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
package org.usergrid.persistence;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.exceptions.QueryParseException;
import org.usergrid.persistence.query.tree.*;

import static org.junit.Assert.*;

public class QueryTest {

    private static final Logger logger = LoggerFactory.getLogger(QueryTest.class);

    @SuppressWarnings("unchecked")
    @Test
    public void testQueryTree() throws Exception {
        logger.info("testQuery");

        Query q = new Query();

        try {
            q.addFilter("blah");
            fail("'blah' shouldn't be a valid operation.");
        } catch (RuntimeException e) {
            // this is correct
        }

        q.addFilter("a=5");
        q.addFilter("b='hello'");
        q.addFilter("c < 7");
        q.addFilter("d gt 5");
        // q.addFilter("e in 5,6");
        q.addFilter("f = 6.0");
        q.addFilter("g = .05");
        q.addFilter("loc within .05 of 5.0,6.0");
        q.addFilter("not h eq 4");

        AndOperand and = (AndOperand) q.getRootOperand();

        NotOperand not = (NotOperand) and.getRight();
        Equal equal = (Equal) not.getOperation();
        assertEquals("h", equal.getProperty().getValue());
        assertEquals(4.0f, ((LongLiteral) equal.getLiteral()).getValue(), 0);

        and = (AndOperand) and.getLeft();
        WithinOperand op = (WithinOperand) and.getRight();

        assertEquals("loc", op.getProperty().getValue());
        assertEquals(.05f, op.getDistance().getFloatValue(), 0);
        assertEquals(5f, op.getLattitude().getFloatValue(), 0);
        assertEquals(6f, op.getLongitude().getFloatValue(), 0);

        and = (AndOperand) and.getLeft();
        equal = (Equal) and.getRight();

        assertEquals("g", equal.getProperty().getValue());
        assertEquals(.05f, ((FloatLiteral) equal.getLiteral()).getValue(), 0);

        and = (AndOperand) and.getLeft();
        equal = (Equal) and.getRight();

        assertEquals("f", equal.getProperty().getValue());
        assertEquals(6.0f, ((FloatLiteral) equal.getLiteral()).getValue(), 0);

        and = (AndOperand) and.getLeft();
        GreaterThan gt = (GreaterThan) and.getRight();

        assertEquals("d", gt.getProperty().getValue());
        assertEquals(5, ((LongLiteral) gt.getLiteral()).getValue(), 0);

        and = (AndOperand) and.getLeft();
        LessThan lt = (LessThan) and.getRight();

        assertEquals("c", lt.getProperty().getValue());
        assertEquals(7, ((LongLiteral) lt.getLiteral()).getValue(), 0);

        and = (AndOperand) and.getLeft();
        equal = (Equal) and.getRight();

        assertEquals("b", equal.getProperty().getValue());
        assertEquals("hello", ((StringLiteral) equal.getLiteral()).getValue());

        equal = (Equal) and.getLeft();

        assertEquals("a", equal.getProperty().getValue());
        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue().intValue());
    }

    @Test
    public void testCodeEquals() {
        Query query = new Query();
        query.addEqualityFilter("foo", "bar");

        Equal equal = (Equal) query.getRootOperand();

        assertEquals("foo", equal.getProperty().getValue());
        assertEquals("bar", equal.getLiteral().getValue());
    }

    @Test
    public void testCodeLessThan() {
        Query query = new Query();
        query.addLessThanFilter("foo", 5);

        LessThan equal = (LessThan) query.getRootOperand();

        assertEquals("foo", equal.getProperty().getValue());
        assertEquals(5l, equal.getLiteral().getValue());
    }

    @Test
    public void testCodeLessThanEqual() {
        Query query = new Query();
        query.addLessThanEqualFilter("foo", 5);

        LessThanEqual equal = (LessThanEqual) query.getRootOperand();

        assertEquals("foo", equal.getProperty().getValue());
        assertEquals(5l, equal.getLiteral().getValue());
    }

    @Test
    public void testCodeGreaterThan() {
        Query query = new Query();
        query.addGreaterThanFilter("foo", 5);

        GreaterThan equal = (GreaterThan) query.getRootOperand();

        assertEquals("foo", equal.getProperty().getValue());
        assertEquals(5l, equal.getLiteral().getValue());
    }

    @Test
    public void testCodeGreaterThanEqual() {
        Query query = new Query();
        query.addGreaterThanEqualFilter("foo", 5);

        GreaterThanEqual equal = (GreaterThanEqual) query.getRootOperand();

        assertEquals("foo", equal.getProperty().getValue());
        assertEquals(5l, equal.getLiteral().getValue());
    }

    @Test
    public void testFromJson() throws QueryParseException {
        String s = "{\"filter\":\"a contains 'ed'\"}";
        Query q = Query.fromJsonString(s);
        assertNotNull(q);

        ContainsOperand contains = (ContainsOperand) q.getRootOperand();

        assertEquals("a", contains.getProperty().getValue());
        assertEquals("ed", contains.getString().getValue());
    }

    @Test
    public void testCompoundQueryWithNot() throws QueryParseException {
        String s = "name contains 'm' and not name contains 'grover'";
        Query q = Query.fromQL(s);
        assertNotNull(q);

        AndOperand and = (AndOperand) q.getRootOperand();

        ContainsOperand contains = (ContainsOperand) and.getLeft();
        assertEquals("name", contains.getProperty().getValue());
        assertEquals("m", contains.getString().getValue());

        NotOperand not = (NotOperand) and.getRight();
        contains = (ContainsOperand) not.getOperation();
        assertEquals("name", contains.getProperty().getValue());
        assertEquals("grover", contains.getString().getValue());
    }

    @Test
    public void badGrammar() throws QueryParseException {
        // from isn't allowed
        String s = "select * from where name = 'bob'";

        String error = null;

        try {
            Query.fromQL(s);
        } catch (QueryParseException qpe) {
            error = qpe.getMessage();
        }

        assertEquals("The query cannot be parsed.  The token 'from' at column 4 on line 1 cannot be parsed", error);

    }
    
    @Test
    public void testTruncation(){
      
      Query query = new Query();
      query.setLimit(Query.MAX_LIMIT*2);
      
      assertEquals(Query.MAX_LIMIT, query.getLimit());
      
    }
    
    
    @Test
    public void testTruncationFromParams() throws QueryParseException{
      
      HashMap<String, List<String>> params = new HashMap<String, List<String>>();
      
      params.put("limit", Collections.singletonList("2000"));
      
      Query query = Query.fromQueryParams(params);
      
      assertEquals(Query.MAX_LIMIT, query.getLimit());
      
    }

}
