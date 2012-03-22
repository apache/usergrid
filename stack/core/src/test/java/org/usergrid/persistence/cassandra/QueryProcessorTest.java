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

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Iterator;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.junit.Test;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.query.ir.QuerySlice;
import org.usergrid.persistence.query.ir.SliceNode;
import org.usergrid.persistence.query.tree.QueryFilterLexer;
import org.usergrid.persistence.query.tree.QueryFilterParser;

/**
 * @author tnine
 * 
 */
public class QueryProcessorTest {

    @Test
    public void equality() throws RecognitionException {
        String queryString = "select * where a = 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessor(query);

        SliceNode node = (SliceNode) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals(BigInteger.valueOf(5), slice.getStart().getValue());
        assertTrue(slice.getStart().isInclusive());
        assertEquals(BigInteger.valueOf(5), slice.getFinish().getValue());
        assertTrue(slice.getFinish().isInclusive());
    }

    @Test
    public void lessThan() throws RecognitionException {
        String queryString = "select * where a < 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessor(query);

        SliceNode node = (SliceNode) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertNull(slice.getStart());

        assertEquals(BigInteger.valueOf(5), slice.getFinish().getValue());
        assertFalse(slice.getFinish().isInclusive());
    }

    @Test
    public void lessThanEquals() throws RecognitionException {
        String queryString = "select * where a <= 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessor(query);

        SliceNode node = (SliceNode) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertNull(slice.getStart());

        assertEquals(BigInteger.valueOf(5), slice.getFinish().getValue());
        assertTrue(slice.getFinish().isInclusive());
    }

    @Test
    public void greaterThan() throws RecognitionException {
        String queryString = "select * where a > 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessor(query);

        SliceNode node = (SliceNode) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals(BigInteger.valueOf(5), slice.getStart().getValue());
        assertFalse(slice.getStart().isInclusive());

        assertNull(slice.getFinish());
    }

    @Test
    public void greaterThanEquals() throws RecognitionException {
        String queryString = "select * where a >= 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessor(query);

        SliceNode node = (SliceNode) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals(BigInteger.valueOf(5), slice.getStart().getValue());
        assertTrue(slice.getStart().isInclusive());

        assertNull(slice.getFinish());
    }

    @Test
    public void andEquality() throws RecognitionException {
        String queryString = "select * where a = 1 and b = 2 and c = 3";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessor(query);

        SliceNode node = (SliceNode) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals("b", slice.getPropertyName());
        assertEquals(BigInteger.valueOf(2), slice.getStart().getValue());
        assertTrue(slice.getStart().isInclusive());
        assertEquals(BigInteger.valueOf(2), slice.getFinish().getValue());
        assertTrue(slice.getFinish().isInclusive());

        slice = slices.next();

        assertEquals("c", slice.getPropertyName());
        assertEquals(BigInteger.valueOf(3), slice.getStart().getValue());
        assertTrue(slice.getStart().isInclusive());
        assertEquals(BigInteger.valueOf(3), slice.getFinish().getValue());
        assertTrue(slice.getFinish().isInclusive());

        slice = slices.next();

        assertEquals("a", slice.getPropertyName());
        assertEquals(BigInteger.valueOf(1), slice.getStart().getValue());
        assertTrue(slice.getStart().isInclusive());
        assertEquals(BigInteger.valueOf(1), slice.getFinish().getValue());
        assertTrue(slice.getFinish().isInclusive());

    }

}
