/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.query.ir;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.apache.usergrid.persistence.exceptions.NoFullTextIndexException;
import org.apache.usergrid.persistence.exceptions.NoIndexException;
import org.apache.usergrid.persistence.exceptions.PersistenceException;
import org.apache.usergrid.persistence.query.Query;
import org.apache.usergrid.persistence.query.Query.SortDirection;
import org.apache.usergrid.persistence.query.tree.AndOperand;
import org.apache.usergrid.persistence.query.tree.ContainsOperand;
import org.apache.usergrid.persistence.query.tree.Equal;
import org.apache.usergrid.persistence.query.tree.GreaterThan;
import org.apache.usergrid.persistence.query.tree.GreaterThanEqual;
import org.apache.usergrid.persistence.query.tree.LessThan;
import org.apache.usergrid.persistence.query.tree.LessThanEqual;
import org.apache.usergrid.persistence.query.tree.NotOperand;
import org.apache.usergrid.persistence.query.tree.OrOperand;
import org.apache.usergrid.persistence.query.tree.QueryFilterLexer;
import org.apache.usergrid.persistence.query.tree.QueryFilterParser;
import org.apache.usergrid.persistence.query.tree.QueryVisitor;
import org.apache.usergrid.persistence.query.tree.WithinOperand;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SearchVisitorTest {
    private static final Logger logger = LoggerFactory.getLogger( SearchVisitorTest.class );

    @Test
    public void testBasicOperation() throws Exception {
        testQuery( "select c where (a = 1 and c > 5)" );
        testQuery( "select c where a contains 'fred'" );
        testQuery( "location within 2000 of 37.776753, -122.407846");
        testQuery( "select * where index >= 5 and index < 10 order by index" );
        testQuery( "name <= 'foxtrot' and name >= 'bravo' order by name desc" );
    }

    private void testQuery( String ugquery ) throws RecognitionException, PersistenceException {

        ANTLRStringStream in = new ANTLRStringStream( ugquery );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;
        TestQueryVisitor v = new TestQueryVisitor();
        query.getRootOperand().visit( v );
        if ( query.isSortSet() ) {
            String sep = "";
            v.sb.append(" order by ");
            for ( Query.SortPredicate sp : query.getSortPredicates() ) {
                String dir = SortDirection.ASCENDING.equals( sp.getDirection() ) ? "asc" : "desc";
                v.sb.append( sep ).append( sp.getPropertyName() ).append(" ").append( dir );
                sep = ","; 
            }
        }

        String esquery = v.sb.toString();
        logger.info("---------------------------------------------------------------------------");
        logger.info( "UG query: " + ugquery );
        logger.info( "ES query: " + esquery );
    }

    class TestQueryVisitor implements QueryVisitor {
        StringBuilder sb = new StringBuilder();

        public void visit( AndOperand op ) throws PersistenceException {
            sb.append(" (" );
            op.getLeft().visit( this );
            sb.append(" and " );
            op.getRight().visit( this );
            sb.append(" )" );
        }

        public void visit( OrOperand op ) throws PersistenceException {
            sb.append(" (" );
            op.getLeft().visit( this );
            sb.append(" or " );
            op.getRight().visit( this );
            sb.append(") " );
        }

        public void visit( NotOperand op ) throws PersistenceException {
            sb.append( " not (");
            op.getOperation().visit( this );
            sb.append( ") ");
        }

        public void visit( LessThan op ) throws NoIndexException {
            String name = op.getProperty().getValue();
            Object value = op.getLiteral().getValue();
            sb.append( name );
            sb.append( " < " );
            sb.append( value.toString() );
        }

        public void visit( ContainsOperand op ) throws NoFullTextIndexException {
            String name = op.getProperty().getValue();
            Object value = op.getLiteral().getValue();
            sb.append( " " );
            sb.append( name );
            sb.append( ":" );
            sb.append( "\"" + value.toString() + "\" " );
        }

        public void visit( WithinOperand op ) {
            sb.append( " within (");
            sb.append( op.getLattitude() );
            sb.append( "," );
            sb.append( op.getLongitude() );
            sb.append( ") ");
        }

        public void visit( LessThanEqual op ) throws NoIndexException {
            String name = op.getProperty().getValue();
            Object value = op.getLiteral().getValue();
            sb.append( name );
            sb.append( " <= " );
            sb.append( value.toString() );
        }

        public void visit( Equal op ) throws NoIndexException {
            String name = op.getProperty().getValue();
            Object value = op.getLiteral().getValue();
            sb.append( name );
            sb.append( " = " );
            sb.append( value.toString() );
        }

        public void visit( GreaterThan op ) throws NoIndexException {
            String name = op.getProperty().getValue();
            Object value = op.getLiteral().getValue();
            sb.append( name );
            sb.append( " > " );
            sb.append( value.toString() );
        }

        public void visit( GreaterThanEqual op ) throws NoIndexException {
            String name = op.getProperty().getValue();
            Object value = op.getLiteral().getValue();
            sb.append( name );
            sb.append( " >= " );
            sb.append( value.toString() );
        }

    }
}