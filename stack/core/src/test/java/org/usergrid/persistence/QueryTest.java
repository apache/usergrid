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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query.FilterPredicate;
import org.usergrid.persistence.Query.SortDirection;
import org.usergrid.persistence.cassandra.QueryProcessor;
import org.usergrid.persistence.query.ir.WithinNode;
import org.usergrid.persistence.query.tree.AndOperand;
import org.usergrid.persistence.query.tree.ContainsOperand;
import org.usergrid.persistence.query.tree.Equal;
import org.usergrid.persistence.query.tree.FloatLiteral;
import org.usergrid.persistence.query.tree.GreaterThan;
import org.usergrid.persistence.query.tree.IntegerLiteral;
import org.usergrid.persistence.query.tree.LessThan;
import org.usergrid.persistence.query.tree.StringLiteral;
import org.usergrid.persistence.query.tree.WithinOperand;
import org.usergrid.utils.JsonUtils;

public class QueryTest {

    private static final Logger logger = LoggerFactory
            .getLogger(QueryTest.class);

    @SuppressWarnings("unchecked")
    @Test
    public void testQuery() throws Exception {
        logger.info("testQuery");

        Query q = new Query();

        q.addFilter("blah");
        q.addFilter("a=5");
        q.addFilter("b='hello'");
        q.addFilter("c < 7");
//        q.addFilter("d gt 5");
        q.addFilter("d > 5");
        // q.addFilter("e in 5,6");
        q.addFilter("f = 6.0");
        q.addFilter("g = .05");
        q.addFilter("loc within .05 of 5.0,6.0");

        // check our tree is value.

        AndOperand and = (AndOperand) q.getRootOperand();

        // Iterator<FilterPredicate> i = q.getFilterPredicates().iterator();

        WithinOperand op = (WithinOperand) and.getRight();

        assertEquals("loc", op.getProperty().getValue());
        assertEquals(.05f, op.getDistance().getValue(), 0);
        assertEquals(5f, op.getLattitude().getValue(), 0);
        assertEquals(6f, op.getLongitude().getValue(), 0);

        and = (AndOperand) and.getLeft();
        Equal equal = (Equal) and.getRight();

        assertEquals("g", equal.getProperty().getValue());
        assertEquals(.05f, ((FloatLiteral) equal.getLiteral()).getValue(), 0);

        and = (AndOperand) and.getLeft();
        equal = (Equal) and.getRight();

        assertEquals("f", equal.getProperty().getValue());
        assertEquals(6.0f, ((FloatLiteral) equal.getLiteral()).getValue(), 0);

        and = (AndOperand) and.getLeft();
        GreaterThan gt = (GreaterThan) and.getRight();

        assertEquals("d", gt.getProperty().getValue());
        assertEquals(5, ((IntegerLiteral) gt.getLiteral()).getValue(), 0);
        
        
        and = (AndOperand) and.getLeft();
        LessThan lt = (LessThan) and.getRight();

        assertEquals("c", lt.getProperty().getValue());
        assertEquals(7, ((IntegerLiteral) lt.getLiteral()).getValue(), 0);
        
        
        and = (AndOperand) and.getLeft();
        equal = (Equal) and.getRight();

        assertEquals("b", lt.getProperty().getValue());
        assertEquals("hello", ((StringLiteral) lt.getLiteral()).getValue());
        
        
        and = (AndOperand) and.getLeft();
        equal = (Equal) and.getRight();

        assertEquals("a", lt.getProperty().getValue());
        assertEquals(5, ((IntegerLiteral) lt.getLiteral()).getValue().intValue());



        

        // These tests are now obscelete See GrammarTreeTest
        // q = Query.fromQL("select * where a = 5");
        // i = q.getFilterPredicates().iterator();
        // f = i.next();
        // testPredicate(f, "a", Query.FilterOperator.EQUAL, new Long(5));
        // logger.info(q.toString());
        //
        // q = Query.fromQL("select * where a = 5 and b = \'hello\'");
        // i = q.getFilterPredicates().iterator();
        // f = i.next();
        // testPredicate(f, "a", Query.FilterOperator.EQUAL, new Long(5));
        // f = i.next();
        // testPredicate(f, "b", Query.FilterOperator.EQUAL, "hello");
        // logger.info(q.toString());
        //
        // q = Query.fromQL("select * where a = 5 and b = \'hello\' and c<7");
        // i = q.getFilterPredicates().iterator();
        // f = i.next();
        // testPredicate(f, "a", Query.FilterOperator.EQUAL, new Long(5));
        // f = i.next();
        // testPredicate(f, "b", Query.FilterOperator.EQUAL, "hello");
        // f = i.next();
        // testPredicate(f, "c", Query.FilterOperator.LESS_THAN, new Long(7));
        // logger.info(q.toString());
        //
        // q = Query.fromQL("order by a asc");
        // assertNotNull(q.getSortPredicates());
        // assertEquals(1, q.getSortPredicates().size());
        // assertEquals("a", q.getSortPredicates().get(0).getPropertyName());
        //
        // q = Query.fromQL("order by a,b desc");
        // assertNotNull(q.getSortPredicates());
        // assertEquals(2, q.getSortPredicates().size());
        // assertEquals("a", q.getSortPredicates().get(0).getPropertyName());
        // assertEquals("b", q.getSortPredicates().get(1).getPropertyName());
        // assertEquals(SortDirection.DESCENDING, q.getSortPredicates().get(1)
        // .getDirection());
        //
        // q = Query.fromQL("select * where loc within 5 of 6,7");
        // i = q.getFilterPredicates().iterator();
        // f = i.next();
        // testPredicate(f, "loc.coordinates", Query.FilterOperator.WITHIN,
        // Arrays.asList(new Long(5), new Long(6), new Long(7)));
        // logger.info(q.toString());

    }

    public void testPredicate(FilterPredicate f, String name,
            Query.FilterOperator op, Object val) {
        logger.info("Checking filter: " + f);
        assertEquals("Predicate property name not correct", name,
                f.getPropertyName());
        assertEquals("first predicate operator not correct", op,
                f.getOperator());
        assertEquals("first predicate value not correct", val, f.getValue());

    }

    @Test
    public void testFromJson() {
        String s = "{\"filter\":\"a contains 'ed'\"}";
        Query q = Query.fromJsonString(s);
        assertNotNull(q);
       
        ContainsOperand contains = (ContainsOperand) q.getRootOperand();
        
        assertEquals("a", contains.getProperty().getValue());
        assertEquals("ed", contains.getString().getValue());

//        s = "asdfasdg";
//        q = Query.fromJsonString(s);
//        assertNull(q);
    }

 

  
}
