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
package org.apache.usergrid.persistence.query.tree;


import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.junit.Test;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.exceptions.QueryParseException;
import org.apache.usergrid.persistence.query.ir.OrNode;

import antlr.NoViableAltException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/** @author tnine */
public class GrammarTreeTest {

    /** Simple test that constructs and AST from the ANTLR generated files */
    @Test
    public void equality() throws RecognitionException {

        String queryString = "select * where a = 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        Equal equal = ( Equal ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );
    }


    /** Simple test that constructs and AST from the ANTLR generated files */
    @Test
    public void lessThan() throws RecognitionException {

        String queryString = "select * where a < 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        LessThan equal = ( LessThan ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );

        // TODO Todd fix this.

        queryString = "select * where a lt 5";

        in = new ANTLRStringStream( queryString );
        lexer = new QueryFilterLexer( in );
        tokens = new TokenRewriteStream( lexer );
        parser = new QueryFilterParser( tokens );

        query = parser.ql().query;

        root = query.getRootOperand();

        equal = ( LessThan ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );
    }


    /** Simple test that constructs and AST from the ANTLR generated files */
    @Test
    public void lessThanEqual() throws RecognitionException {

        String queryString = "select * where a <= 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        LessThanEqual equal = ( LessThanEqual ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );

        queryString = "select * where a lte 5";

        in = new ANTLRStringStream( queryString );
        lexer = new QueryFilterLexer( in );
        tokens = new TokenRewriteStream( lexer );
        parser = new QueryFilterParser( tokens );

        query = parser.ql().query;

        root = query.getRootOperand();

        equal = ( LessThanEqual ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );
    }


    /** Simple test that constructs and AST from the ANTLR generated files */
    @Test
    public void greaterThan() throws RecognitionException {

        String queryString = "select * where a > 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        GreaterThan equal = ( GreaterThan ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );

        queryString = "select * where a gt 5";

        in = new ANTLRStringStream( queryString );
        lexer = new QueryFilterLexer( in );
        tokens = new TokenRewriteStream( lexer );
        parser = new QueryFilterParser( tokens );

        query = parser.ql().query;

        root = query.getRootOperand();

        equal = ( GreaterThan ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );
    }


    /** Simple test that constructs and AST from the ANTLR generated files */
    @Test
    public void greaterThanEqual() throws RecognitionException {

        String queryString = "select * where a >= 5";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        GreaterThanEqual equal = ( GreaterThanEqual ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );

        queryString = "select * where a gte 5";

        in = new ANTLRStringStream( queryString );
        lexer = new QueryFilterLexer( in );
        tokens = new TokenRewriteStream( lexer );
        parser = new QueryFilterParser( tokens );

        query = parser.ql().query;

        root = query.getRootOperand();

        equal = ( GreaterThanEqual ) root;

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 5, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );
    }


    /** Test basic && expression */
    @Test
    public void andExpression() throws RecognitionException {

        String queryString = "select * where a = 1 and b > 2";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        AndOperand and = ( AndOperand ) root;

        Equal equal = ( Equal ) and.getLeft();

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 1, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );

        GreaterThan greater = ( GreaterThan ) and.getRight();

        assertEquals( "b", greater.getProperty().getValue() );
        assertEquals( 2, ( ( LongLiteral ) greater.getLiteral() ).getValue().intValue() );
    }


    /** Test basic || expression */
    @Test
    public void orExpression() throws RecognitionException {

        String queryString = "select * where a = 1 or b > 2";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        OrOperand and = ( OrOperand ) root;

        Equal equal = ( Equal ) and.getLeft();

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 1, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );

        GreaterThan greater = ( GreaterThan ) and.getRight();

        assertEquals( "b", greater.getProperty().getValue() );
        assertEquals( 2, ( ( LongLiteral ) greater.getLiteral() ).getValue().intValue() );
    }


    /** Test basic not expression */
    @Test
    public void notExpression() throws RecognitionException {

        String queryString = "select * where not a = 1";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        NotOperand not = ( NotOperand ) root;

        Equal equal = ( Equal ) not.getOperation();

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 1, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );
    }


    /** Test basic not expression */
    @Test
    public void complexExpression() throws RecognitionException {

        String queryString = "select * where not a = 1";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        NotOperand not = ( NotOperand ) root;

        Equal equal = ( Equal ) not.getOperation();

        assertEquals( "a", equal.getProperty().getValue() );

        assertEquals( 1, ( ( LongLiteral ) equal.getLiteral() ).getValue().intValue() );
    }


    /** Test basic || expression */
    @Test
    public void selectAll() throws RecognitionException {

        String queryString = "select * where a = 1 or b > 2";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Set<String> identifiers = query.getSelectSubjects();

        assertEquals( 0, identifiers.size() );
    }


    @Test
    public void selectGeo() throws RecognitionException {
        String queryString = "select * where a within .1 of -40.343666, 175.630917";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        WithinOperand operand = ( WithinOperand ) query.getRootOperand();

        assertEquals( "a", operand.getProperty().getValue() );
        assertEquals( .1f, operand.getDistance().getFloatValue(), 0 );
        assertEquals( -40.343666f, operand.getLattitude().getFloatValue(), 0 );
        assertEquals( 175.630917f, operand.getLongitude().getFloatValue(), 0 );
    }


    @Test
    public void selectGeoWithInt() throws RecognitionException {
        String queryString = "select * where a within 1 of -40.343666, 175.630917";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        WithinOperand operand = ( WithinOperand ) query.getRootOperand();

        assertEquals( "a", operand.getProperty().getValue() );
        assertEquals( 1, operand.getDistance().getFloatValue(), 0 );
        assertEquals( -40.343666f, operand.getLattitude().getFloatValue(), 0 );
        assertEquals( 175.630917f, operand.getLongitude().getFloatValue(), 0 );
    }


    @Test
    public void selectDistance() throws RecognitionException {
        String queryString = "select * where a contains 'foo'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        ContainsOperand operand = ( ContainsOperand ) query.getRootOperand();

        assertEquals( "a", operand.getProperty().getValue() );
        assertEquals( "foo", operand.getString().getValue() );
    }


    @Test
    public void selectField() throws RecognitionException {

        String queryString = "select c where a = 1 or b > 2";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Set<String> identifiers = query.getSelectSubjects();

        assertTrue( identifiers.contains( "c" ) );
    }


    @Test
    public void selectRename() throws RecognitionException {

        String queryString = "select {source:target} where a = 1 or b > 2";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Map<String, String> identifiers = query.getSelectAssignments();

        assertEquals( "target", identifiers.get( "source" ) );
    }


    @Test
    public void containsOr() throws Exception {
        String queryString = "select * where keywords contains 'hot' or title contains 'hot'";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        OrOperand rootNode = ( OrOperand ) query.getRootOperand();

        assertNotNull( rootNode );

        ContainsOperand left = ( ContainsOperand ) rootNode.getLeft();

        assertEquals( "keywords", left.getProperty().getValue() );

        assertEquals( "hot", left.getString().getValue() );
        assertEquals( "hot", left.getString().getEndValue() );

        ContainsOperand right = ( ContainsOperand ) rootNode.getRight();

        assertEquals( "title", right.getProperty().getValue() );

        assertEquals( "hot", right.getString().getValue() );
        assertEquals( "hot", right.getString().getEndValue() );
    }


    @Test
    public void stringLower() throws Exception {
        String queryString = "select * where  title = 'Hot'";


        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Equal rootNode = ( Equal ) query.getRootOperand();

        assertEquals( "title", rootNode.getProperty().getValue() );
        assertEquals( "hot", ( ( StringLiteral ) rootNode.getLiteral() ).getValue() );
    }


    @Test
    public void nestedBooleanLogic() throws Exception {
        String queryString = "select * where field1 = 'foo' AND (field2 = 'bar' OR field2 = 'baz')";


        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        AndOperand rootNode = ( AndOperand ) query.getRootOperand();

        //left should be field1
        Equal field1Equal = ( Equal ) rootNode.getLeft();

        assertEquals( "field1", field1Equal.getProperty().getValue() );
        assertEquals( "foo", ( ( StringLiteral ) field1Equal.getLiteral() ).getValue() );


        OrOperand orNode = ( OrOperand ) rootNode.getRight();

        Equal field2Bar = ( Equal ) orNode.getLeft();
        Equal field2Baz = ( Equal ) orNode.getRight();

        assertEquals( "field2", field2Bar.getProperty().getValue() );
        assertEquals( "bar", ( ( StringLiteral ) field2Bar.getLiteral() ).getValue() );

        assertEquals( "field2", field2Baz.getProperty().getValue() );
        assertEquals( "baz", ( ( StringLiteral ) field2Baz.getLiteral() ).getValue() );
    }


    @Test
    public void uuidParse() throws RecognitionException {
        String queryString = "select * where  title = c6ee8a1c-3ef4-11e2-8861-02e81adcf3d0";

        ANTLRStringStream in = new ANTLRStringStream( queryString );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Query query = parser.ql().query;

        Equal rootNode = ( Equal ) query.getRootOperand();

        assertEquals( "title", rootNode.getProperty().getValue() );
        assertEquals( UUID.fromString( "c6ee8a1c-3ef4-11e2-8861-02e81adcf3d0" ),
                ( ( UUIDLiteral ) rootNode.getLiteral() ).getValue() );
    }


    @Test
    public void badOrderByGrammar() throws QueryParseException {
        // from isn't allowed
        String s = "select * where name = 'bob' order by";

        String error = null;

        try {
            Query.fromQL( s );
        }
        catch ( QueryParseException qpe ) {
            error = qpe.getMessage();
        }

        assertEquals( "The query cannot be parsed. The token '<EOF>' " + "at column 13 on line 1 cannot be parsed",
                error );
    }

    @Test
       public void badOperand() throws QueryParseException {
           // from isn't allowed
           String s = "select * where name != 'bob'";

           String error = null;

           try {
               Query.fromQL( s );
               fail("should throw an exception");
           }
           catch ( RuntimeException qpe ) {
               error = qpe.getMessage();
           }

           assertEquals( "NoViableAltException('!'@[1:1: Tokens : ( T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | T__40 | LT | LTE | EQ | GT | GTE | BOOLEAN | AND | OR | NOT | ASC | DESC | CONTAINS | WITHIN | OF | UUID | ID | LONG | FLOAT | STRING | WS );])",
                   error );
       }



}
