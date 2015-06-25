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


import java.math.BigInteger;
import java.util.Iterator;
import java.util.UUID;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.TokenRewriteStream;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.usergrid.mq.QueryFilterLexer;
import org.apache.usergrid.mq.QueryFilterParser;
import org.apache.usergrid.persistence.exceptions.PersistenceException;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.tree.CpQueryFilterLexer;
import org.apache.usergrid.persistence.index.query.tree.CpQueryFilterParser;
import org.apache.usergrid.persistence.query.ir.AndNode;
import org.apache.usergrid.persistence.query.ir.NotNode;
import org.apache.usergrid.persistence.query.ir.OrNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SliceNode;
import org.apache.usergrid.persistence.query.ir.WithinNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author tnine
 */

public class QueryProcessorTest {

    @Test
    public void equality() throws Exception {
        String queryString = "select * where a = 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( BigInteger.valueOf( 5 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 5 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void lessThan() throws Exception {
        String queryString = "select * where a < 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertNull( slice.getStart() );

        assertEquals( BigInteger.valueOf( 5 ), slice.getFinish().getValue() );
        assertFalse( slice.getFinish().isInclusive() );
    }


    @Test
    public void lessThanEquals() throws Exception {
        String queryString = "select * where a <= 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertNull( slice.getStart() );

        assertEquals( BigInteger.valueOf( 5 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void greaterThan() throws Exception {
        String queryString = "select * where a > 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( BigInteger.valueOf( 5 ), slice.getStart().getValue() );
        assertFalse( slice.getStart().isInclusive() );

        assertNull( slice.getFinish() );
    }


    @Test
    public void greaterThanEquals() throws Exception {
        String queryString = "select * where a >= 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( BigInteger.valueOf( 5 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );

        assertNull( slice.getFinish() );
    }


    @Test
    public void contains() throws Exception {
        String queryString = "select * where a contains 'foo'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a.keywords", slice.getPropertyName() );

        assertEquals( "foo", slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );

        assertEquals( "foo", slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void containsLower() throws Exception {
        String queryString = "select * where a contains 'FOO'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a.keywords", slice.getPropertyName() );

        assertEquals( "foo", slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );

        assertEquals( "foo", slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void containsRange() throws Exception, PersistenceException {

        String queryString = "select * where a contains 'foo*'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        if ( !(processor.getEntityManager() instanceof EntityManagerImpl) ) {
            return; // only relevant for old entity manager
        }

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a.keywords", slice.getPropertyName() );

        assertEquals( "foo", slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );

        assertEquals( "foo\uffff", slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void within() throws Exception {
        String queryString = "select * where a within .5 of 157.00, 0.00";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        WithinNode node = ( WithinNode ) processor.getFirstNode();

        assertEquals( "a.coordinates", node.getPropertyName() );
        assertEquals( .5f, node.getDistance(), 0 );
        assertEquals( 157f, node.getLattitude(), 0 );
        assertEquals( 0f, node.getLongitude(), 0 );
    }


    @Test
    public void andEquality() throws Exception {
        assertAndQuery( "select * where a = 1 and b = 2 and c = 3" );
        assertAndQuery( "select * where a = 1 AND b = 2 and c = 3" );
        assertAndQuery( "select * where a = 1 AnD b = 2 and c = 3" );
        assertAndQuery( "select * where a = 1 ANd b = 2 and c = 3" );
        assertAndQuery( "select * where a = 1 anD b = 2 and c = 3" );
        assertAndQuery( "select * where a = 1 ANd b = 2 and c = 3" );
        assertAndQuery( "select * where a = 1 && b = 2 && c = 3" );
    }


    private void assertAndQuery( String queryString ) throws Exception {

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();


        assertEquals( "a", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 1 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 1 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );

        slice = slices.next();

        assertEquals( "b", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 2 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 2 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );

        slice = slices.next();

        assertEquals( "c", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 3 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 3 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void orEquality() throws Exception {
        assertOrQuery( "select * where a = 1 or b = 2" );
        assertOrQuery( "select * where a = 1 OR b = 2" );
        assertOrQuery( "select * where a = 1 oR b = 2" );
        assertOrQuery( "select * where a = 1 Or b = 2" );
        assertOrQuery( "select * where a = 1 || b = 2" );
    }


    private void assertOrQuery( String queryString ) throws Exception {

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        OrNode node = ( OrNode ) processor.getFirstNode();

        SliceNode sliceNode = ( SliceNode ) node.getLeft();

        Iterator<QuerySlice> slices = sliceNode.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 1 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 1 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );

        sliceNode = ( SliceNode ) node.getRight();

        slices = sliceNode.getAllSlices().iterator();

        slice = slices.next();

        assertEquals( "b", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 2 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 2 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    /** Tests that when properties are not siblings, they are properly assigned to a SliceNode */
    @Test
    public void nestedCompression() throws Exception {
        String queryString =
                "select * where (a > 1 and b > 10 and a < 10 and b < 20 ) or ( c >= 20 and d >= 30 and c <= 30 and d "
                        + "<= 40)";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        OrNode node = ( OrNode ) processor.getFirstNode();

        SliceNode sliceNode = ( SliceNode ) node.getLeft();

        Iterator<QuerySlice> slices = sliceNode.getAllSlices().iterator();

        QuerySlice slice = slices.next();


        assertEquals( "a", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 1 ), slice.getStart().getValue() );
        assertFalse( slice.getStart().isInclusive() );

        assertEquals( BigInteger.valueOf( 10 ), slice.getFinish().getValue() );
        assertFalse( slice.getFinish().isInclusive() );


        slice = slices.next();


        assertEquals( "b", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 10 ), slice.getStart().getValue() );
        assertFalse( slice.getStart().isInclusive() );

        assertEquals( BigInteger.valueOf( 20 ), slice.getFinish().getValue() );
        assertFalse( slice.getFinish().isInclusive() );


        sliceNode = ( SliceNode ) node.getRight();

        slices = sliceNode.getAllSlices().iterator();

        slice = slices.next();


        assertEquals( "c", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 20 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 30 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );

        slice = slices.next();

        assertEquals( "d", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 30 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 40 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    /** Tests that when there are multiple or with and clauses, the tree is constructed correctly */
    @Test
    public void nestedOrCompression() throws Exception {
        String queryString =
                "select * where ((a > 1 and  a < 10) or (b > 10 and b < 20 )) or (( c >= 20 and c <= 30 ) or (d >= 30"
                        + "  and d <= 40))";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        OrNode rootNode = ( OrNode ) processor.getFirstNode();

        OrNode node = ( OrNode ) rootNode.getLeft();

        // get the left node of the or

        SliceNode sliceNode = ( SliceNode ) node.getLeft();

        Iterator<QuerySlice> slices = sliceNode.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 1 ), slice.getStart().getValue() );
        assertFalse( slice.getStart().isInclusive() );

        assertEquals( BigInteger.valueOf( 10 ), slice.getFinish().getValue() );
        assertFalse( slice.getFinish().isInclusive() );

        // get our right node
        sliceNode = ( SliceNode ) node.getRight();

        slices = sliceNode.getAllSlices().iterator();

        slice = slices.next();

        assertEquals( "b", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 10 ), slice.getStart().getValue() );
        assertFalse( slice.getStart().isInclusive() );

        assertEquals( BigInteger.valueOf( 20 ), slice.getFinish().getValue() );
        assertFalse( slice.getFinish().isInclusive() );

        node = ( OrNode ) rootNode.getRight();

        sliceNode = ( SliceNode ) node.getLeft();

        slices = sliceNode.getAllSlices().iterator();

        slice = slices.next();

        assertEquals( "c", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 20 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 30 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );

        sliceNode = ( SliceNode ) node.getRight();

        slices = sliceNode.getAllSlices().iterator();

        slice = slices.next();

        assertEquals( "d", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 30 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 40 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    /** Tests that when NOT is not the root operand the tree has a different root */
    @Test
    public void andNot() throws Exception {
        String queryString = "select * where a > 1 and not b = 2";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        AndNode rootNode = ( AndNode ) processor.getFirstNode();

        SliceNode sliceNode = ( SliceNode ) rootNode.getLeft();

        Iterator<QuerySlice> slices = sliceNode.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 1 ), slice.getStart().getValue() );
        assertFalse( slice.getStart().isInclusive() );

        assertNull( slice.getFinish() );

        NotNode notNode = ( NotNode ) rootNode.getRight();

        // now get the child of the not node
        sliceNode = ( SliceNode ) notNode.getSubtractNode();

        slices = sliceNode.getAllSlices().iterator();

        slice = slices.next();

        assertEquals( "b", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 2 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 2 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    /** Tests that when NOT is the root operand, a full scan range is performed. */
    @Test
    public void notRootOperand() throws Exception {
        String queryString = "select * where not b = 2";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        NotNode rootNode = ( NotNode ) processor.getFirstNode();

        SliceNode sliceNode = ( SliceNode ) rootNode.getSubtractNode();

        Iterator<QuerySlice> slices = sliceNode.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "b", slice.getPropertyName() );
        assertEquals( BigInteger.valueOf( 2 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 2 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void stringWithSpaces() throws Exception {
        String queryString = "select * where a = 'foo with bar'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a", slice.getPropertyName() );

        assertEquals( "foo with bar", slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );

        assertEquals( "foo with bar", slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void fieldWithDash() throws Exception {
        String queryString = "select * where a-foo = 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a-foo", slice.getPropertyName() );

        assertEquals( BigInteger.valueOf( 5 ), slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( BigInteger.valueOf( 5 ), slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void stringWithDash() throws Exception {
        String queryString = "select * where a = 'foo-bar'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "a", slice.getPropertyName() );

        assertEquals( "foo-bar", slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );

        assertEquals( "foo-bar", slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    public void uuidParse() throws Exception {

        //    UUID value = UUID.fromString("4b91a9c2-86a1-11e2-b7fa-68a86d52fa56");
        //
        //
        //    String queryString = "select * where uuid = 4b91a9c2-86a1-11e2-b7fa-68a86d52fa56";

        UUID value = UUID.fromString( "c6ee8a1c-3ef4-11e2-8861-02e81adcf3d0" );
        String queryString = "select * where uuid = c6ee8a1c-3ef4-11e2-8861-02e81adcf3d0";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        Query query = parser.ql().query;

        QueryProcessor processor = new QueryProcessorImpl( query, null, null, null );

        SliceNode node = ( SliceNode ) processor.getFirstNode();

        Iterator<QuerySlice> slices = node.getAllSlices().iterator();

        QuerySlice slice = slices.next();

        assertEquals( "uuid", slice.getPropertyName() );

        assertEquals( value, slice.getStart().getValue() );
        assertTrue( slice.getStart().isInclusive() );
        assertEquals( value, slice.getFinish().getValue() );
        assertTrue( slice.getFinish().isInclusive() );
    }


    @Test
    @Ignore("no longer relevant for two-dot-o")
    public void validateHintSizeForOrder() throws Exception {
        String queryString = "order by name desc";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        /**
         * Test set limit
         */

        final int limit = 105;

//        Query query = parser.ql().query;
//        query.setLimit( limit );
//
//        QueryProcessor processor = new QueryProcessor( query, null, null, null );
//
//        OrderByNode node = ( OrderByNode ) processor.getFirstNode();
//
//        assertEquals( limit, processor.getPageSizeHint( node ) );
    }


    @Test
    @Ignore("no longer relevant for two-dot-o")
    public void validateHintSizeForEquality() throws Exception {
        String queryString = "select * where X = 'Foo'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        /**
         * Test set limit
         */

        final int limit = 105;

        Query query = parser.ql().query;
        query.setLimit( limit );

//        QueryProcessor processor = new QueryProcessor( query, null, null, null );
//
//        SliceNode node = ( SliceNode ) processor.getFirstNode();
//
//        assertEquals( limit, processor.getPageSizeHint( node ) );
    }


    @Test
    @Ignore("no longer relevant for two-dot-o")
    public void validateHintSizeForComplexQueries() throws Exception {
        //        String queryString = "select * where y = 'Foo' AND z = 'Bar'";

        String queryString = "select * where y = 'Foo' AND z = 'Bar'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

        /**
         * Test set limit
         */

        final int limit = 105;

        Query query = parser.ql().query;
        query.setLimit( limit );

//        QueryProcessor processor = new QueryProcessor( query, null, null, null );
//
//        QueryNode slice =  processor.getFirstNode();
//
//        assertEquals( 1000, processor.getPageSizeHint( slice ) );
    }
}
