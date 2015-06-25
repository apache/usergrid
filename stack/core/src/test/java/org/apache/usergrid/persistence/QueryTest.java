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
package org.apache.usergrid.persistence;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.index.exceptions.QueryParseException;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Query.SortDirection;
import org.apache.usergrid.persistence.index.query.Query.SortPredicate;
import org.apache.usergrid.persistence.index.query.tree.AndOperand;
import org.apache.usergrid.persistence.index.query.tree.ContainsOperand;
import org.apache.usergrid.persistence.index.query.tree.Equal;
import org.apache.usergrid.persistence.index.query.tree.FloatLiteral;
import org.apache.usergrid.persistence.index.query.tree.GreaterThan;
import org.apache.usergrid.persistence.index.query.tree.GreaterThanEqual;
import org.apache.usergrid.persistence.index.query.tree.LessThan;
import org.apache.usergrid.persistence.index.query.tree.LessThanEqual;
import org.apache.usergrid.persistence.index.query.tree.LongLiteral;
import org.apache.usergrid.persistence.index.query.tree.NotOperand;
import org.apache.usergrid.persistence.index.query.tree.StringLiteral;
import org.apache.usergrid.persistence.index.query.tree.WithinOperand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;



public class QueryTest {

    private static final Logger LOG = LoggerFactory.getLogger( QueryTest.class );


    @Test
    public void testQueryTree() throws Exception {
        LOG.info( "testQuery" );

        Query q = new Query();

        try {
            q.addFilter( "blah" );
            fail( "'blah' shouldn't be a valid operation." );
        }
        catch ( RuntimeException e ) {
            // this is correct
        }

        q.addFilter( "a=5" );
        q.addFilter( "b='hello'" );
        q.addFilter( "c < 7" );
        q.addFilter( "d gt 5" );
        // q.addFilter("e in 5,6");
        q.addFilter( "f = 6.0" );
        q.addFilter( "g = .05" );
        q.addFilter( "loc within .05 of 5.0,6.0" );
        q.addFilter( "not h eq 4" );

        AndOperand and = ( AndOperand ) q.getRootOperand();

        NotOperand not = ( NotOperand ) and.getRight();
        Equal equal = ( Equal ) not.getOperation();
        assertEquals( "h", equal.getProperty().getValue() );
        assertEquals( 4.0f, ( ( LongLiteral ) equal.getLiteral() ).getValue(), 0 );

        and = ( AndOperand ) and.getLeft();
        WithinOperand op = ( WithinOperand ) and.getRight();

        assertEquals( "loc", op.getProperty().getValue() );
        assertEquals( .05f, op.getDistance().getFloatValue(), 0 );
        assertEquals( 5f, op.getLatitude().getFloatValue(), 0 );
        assertEquals( 6f, op.getLongitude().getFloatValue(), 0 );

        and = ( AndOperand ) and.getLeft();
        equal = ( Equal ) and.getRight();

        assertEquals( "g", equal.getProperty().getValue() );
        assertEquals( .05f, ( ( FloatLiteral ) equal.getLiteral() ).getValue(), 0 );

        and = ( AndOperand ) and.getLeft();
        equal = ( Equal ) and.getRight();

        assertEquals( "f", equal.getProperty().getValue() );
        assertEquals( 6.0f, ( ( FloatLiteral ) equal.getLiteral() ).getValue(), 0 );

        and = ( AndOperand ) and.getLeft();
        GreaterThan gt = ( GreaterThan ) and.getRight();

        assertEquals( "d", gt.getProperty().getValue() );
        assertEquals( 5, ( ( LongLiteral ) gt.getLiteral() ).getValue(), 0 );

        and = ( AndOperand ) and.getLeft();
        LessThan lt = ( LessThan ) and.getRight();

        assertEquals( "c", lt.getProperty().getValue() );
        assertEquals( 7, ( ( LongLiteral ) lt.getLiteral() ).getValue(), 0 );

        and = ( AndOperand ) and.getLeft();
        equal = ( Equal ) and.getRight();

        assertEquals( "b", equal.getProperty().getValue() );
        assertEquals( "hello", ( ( StringLiteral ) equal.getLiteral() ).getValue() );

        equal = ( Equal ) and.getLeft();

        assertEquals( "a", equal.getProperty().getValue() );
        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );
    }

    @Test
    public void withinDistanceCorrect(){
        final Query query = Query.fromQL( "location within 2000 of 37.776753, -122.407846" );

        WithinOperand withinOperand = ( WithinOperand ) query.getRootOperand();

        final float distance = withinOperand.getDistance().getFloatValue();
        final float lat = withinOperand.getLatitude().getFloatValue();
        final float lon = withinOperand.getLongitude().getFloatValue();

        assertEquals( 2000f, distance, 0f );
        assertEquals( 37.776753f, lat, 0f );
        assertEquals( -122.407846f, lon, 0f );
    }


    @Test
    public void testCodeEquals() {
        Query query = new Query();
        query.addEqualityFilter( "foo", "bar" );

        Equal equal = ( Equal ) query.getRootOperand();

        assertEquals( "foo", equal.getProperty().getValue() );
        assertEquals( "bar", equal.getLiteral().getValue() );
    }


    @Test
    public void testCodeLessThan() {
        Query query = new Query();
        query.addLessThanFilter( "foo", 5 );

        LessThan equal = ( LessThan ) query.getRootOperand();

        assertEquals( "foo", equal.getProperty().getValue() );
        assertEquals( 5l, equal.getLiteral().getValue() );
    }


    @Test
    public void testCodeLessThanEqual() {
        Query query = new Query();
        query.addLessThanEqualFilter( "foo", 5 );

        LessThanEqual equal = ( LessThanEqual ) query.getRootOperand();

        assertEquals( "foo", equal.getProperty().getValue() );
        assertEquals( 5l, equal.getLiteral().getValue() );
    }


    @Test
    public void testCodeGreaterThan() {
        Query query = new Query();
        query.addGreaterThanFilter( "foo", 5 );

        GreaterThan equal = ( GreaterThan ) query.getRootOperand();

        assertEquals( "foo", equal.getProperty().getValue() );
        assertEquals( 5l, equal.getLiteral().getValue() );
    }


    @Test
    public void testCodeGreaterThanEqual() {
        Query query = new Query();
        query.addGreaterThanEqualFilter( "foo", 5 );

        GreaterThanEqual equal = ( GreaterThanEqual ) query.getRootOperand();

        assertEquals( "foo", equal.getProperty().getValue() );
        assertEquals( 5l, equal.getLiteral().getValue() );
    }


    @Test
    public void testFromJson() throws QueryParseException {
        String s = "{\"filter\":\"a contains 'ed'\"}";
        Query q = Query.fromJsonString( s );
        assertNotNull( q );

        ContainsOperand contains = ( ContainsOperand ) q.getRootOperand();

        assertEquals( "a", contains.getProperty().getValue() );
        assertEquals( "ed", contains.getString().getValue() );
    }


    @Test
    public void testCompoundQueryWithNot() throws QueryParseException {
        String s = "name contains 'm' and not name contains 'grover'";
        Query q = Query.fromQL( s );
        assertNotNull( q );

        AndOperand and = ( AndOperand ) q.getRootOperand();

        ContainsOperand contains = ( ContainsOperand ) and.getLeft();
        assertEquals( "name", contains.getProperty().getValue() );
        assertEquals( "m", contains.getString().getValue() );

        NotOperand not = ( NotOperand ) and.getRight();
        contains = ( ContainsOperand ) not.getOperation();
        assertEquals( "name", contains.getProperty().getValue() );
        assertEquals( "grover", contains.getString().getValue() );
    }


    @Test
    public void badGrammar() throws QueryParseException {
        // from isn't allowed
        String s = "select * from where name = 'bob'";

        String error = null;

        try {
            Query.fromQL( s );
        }
        catch ( QueryParseException qpe ) {
            error = qpe.getMessage();
        }

        assertEquals( "The query cannot be parsed. The token 'from' at column 4 on line 1 cannot be parsed", error );
    }


    @Test
    public void testTruncation() {

        Query query = new Query();
        query.setLimit( Query.MAX_LIMIT * 2 );

        assertEquals( Query.MAX_LIMIT, query.getLimit() );
    }


    @Test
    public void testTruncationFromParams() throws QueryParseException {

        HashMap<String, List<String>> params = new HashMap<String, List<String>>();

        params.put( "limit", Collections.singletonList( "2000" ) );

        Query query = Query.fromQueryParams( params );

        assertEquals( Query.MAX_LIMIT, query.getLimit() );
    }


    @Test
    public void badOrderByBadGrammar() throws QueryParseException {
        // from isn't allowed
        String s = "select * where name = 'bob' order by";

        String error = null;

        try {
            Query.fromQL( s );
        }
        catch ( QueryParseException qpe ) {
            error = qpe.getMessage();
        }

        assertEquals( "The query cannot be parsed. The token '<EOF>' at column 13 on line 1 cannot be parsed", error );
    }


    @Test
    public void badOrderByGrammarAsc() throws QueryParseException {
        // from isn't allowed
        String s = "select * where name = 'bob' order by name asc";

        Query q = Query.fromQL( s );

        List<SortPredicate> sorts = q.getSortPredicates();

        assertEquals( 1, sorts.size() );

        assertEquals( "name", sorts.get( 0 ).getPropertyName() );
        assertEquals( SortDirection.ASCENDING, sorts.get( 0 ).getDirection() );
    }


    @Test
    public void badOrderByGrammarDesc() throws QueryParseException {
        // from isn't allowed
        String s = "select * where name = 'bob' order by name desc";

        Query q = Query.fromQL( s );

        List<SortPredicate> sorts = q.getSortPredicates();

        assertEquals( 1, sorts.size() );

        assertEquals( "name", sorts.get( 0 ).getPropertyName() );
        assertEquals( SortDirection.DESCENDING, sorts.get( 0 ).getDirection() );
    }
}
