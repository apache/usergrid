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
package org.usergrid.persistence.query.tree;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.junit.Test;
import org.usergrid.persistence.Query;

/**
 * @author tnine
 * 
 */
public class GrammarTreeTest {

    /**
     * Simple test that constructs and AST from the ANTLR generated files
     * 
     * @throws RecognitionException
     */
    @Test
    public void equality() throws RecognitionException {

        String queryString = "select * where a = 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        Equal equal = (Equal) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Simple test that constructs and AST from the ANTLR generated files
     * 
     * @throws RecognitionException
     */
    @Test
    public void lessThan() throws RecognitionException {

        String queryString = "select * where a < 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        LessThan equal = (LessThan) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

        // TODO Todd fix this.

        queryString = "select * where a lt 5";

        in = new ANTLRStringStream(queryString);
        lexer = new QueryFilterLexer(in);
        tokens = new TokenRewriteStream(lexer);
        parser = new QueryFilterParser(tokens);

        query = parser.ql().query;

        root = query.getRootOperand();

        equal = (LessThan) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Simple test that constructs and AST from the ANTLR generated files
     * 
     * @throws RecognitionException
     */
    @Test
    public void lessThanEqual() throws RecognitionException {

        String queryString = "select * where a <= 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        LessThanEqual equal = (LessThanEqual) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

        queryString = "select * where a lte 5";

        in = new ANTLRStringStream(queryString);
        lexer = new QueryFilterLexer(in);
        tokens = new TokenRewriteStream(lexer);
        parser = new QueryFilterParser(tokens);

        query = parser.ql().query;

        root = query.getRootOperand();

        equal = (LessThanEqual) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Simple test that constructs and AST from the ANTLR generated files
     * 
     * @throws RecognitionException
     */
    @Test
    public void greaterThan() throws RecognitionException {

        String queryString = "select * where a > 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        GreaterThan equal = (GreaterThan) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

        queryString = "select * where a gt 5";

        in = new ANTLRStringStream(queryString);
        lexer = new QueryFilterLexer(in);
        tokens = new TokenRewriteStream(lexer);
        parser = new QueryFilterParser(tokens);

        query = parser.ql().query;

        root = query.getRootOperand();

        equal = (GreaterThan) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Simple test that constructs and AST from the ANTLR generated files
     * 
     * @throws RecognitionException
     */
    @Test
    public void greaterThanEqual() throws RecognitionException {

        String queryString = "select * where a >= 5";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        GreaterThanEqual equal = (GreaterThanEqual) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

        queryString = "select * where a gte 5";

        in = new ANTLRStringStream(queryString);
        lexer = new QueryFilterLexer(in);
        tokens = new TokenRewriteStream(lexer);
        parser = new QueryFilterParser(tokens);

        query = parser.ql().query;

        root = query.getRootOperand();

        equal = (GreaterThanEqual) root;

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(5, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Test basic && expression
     * 
     * @throws RecognitionException
     */
    @Test
    public void andExpression() throws RecognitionException {

        String queryString = "select * where a = 1 and b > 2";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        AndOperand and = (AndOperand) root;

        Equal equal = (Equal) and.getLeft();

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(1, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

        GreaterThan greater = (GreaterThan) and.getRight();

        assertEquals("b", greater.getProperty().getValue());
        assertEquals(2, ((LongLiteral) greater.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Test basic || expression
     * 
     * @throws RecognitionException
     */
    @Test
    public void orExpression() throws RecognitionException {

        String queryString = "select * where a = 1 or b > 2";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        OrOperand and = (OrOperand) root;

        Equal equal = (Equal) and.getLeft();

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(1, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

        GreaterThan greater = (GreaterThan) and.getRight();

        assertEquals("b", greater.getProperty().getValue());
        assertEquals(2, ((LongLiteral) greater.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Test basic not expression
     * 
     * @throws RecognitionException
     */
    @Test
    public void notExpression() throws RecognitionException {

        String queryString = "select * where not a = 1";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        NotOperand not = (NotOperand) root;

        Equal equal = (Equal) not.getOperation();

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(1, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Test basic not expression
     * 
     * @throws RecognitionException
     */
    @Test
    public void complexExpression() throws RecognitionException {

        String queryString = "select * where not a = 1";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Operand root = query.getRootOperand();

        NotOperand not = (NotOperand) root;

        Equal equal = (Equal) not.getOperation();

        assertEquals("a", equal.getProperty().getValue());

        assertEquals(1, ((LongLiteral) equal.getLiteral()).getValue()
                .intValue());

    }

    /**
     * Test basic || expression
     * 
     * @throws RecognitionException
     */
    @Test
    public void selectAll() throws RecognitionException {

        String queryString = "select * where a = 1 or b > 2";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Set<String> identifiers = query.getSelectSubjects();

        assertEquals(0, identifiers.size());

    }

    @Test
    public void selectGeo() throws RecognitionException {
        String queryString = "select * where a within .1 of -40.343666, 175.630917";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        WithinOperand operand = (WithinOperand) query.getRootOperand();

        assertEquals("a", operand.getProperty().getValue());
        assertEquals(.1f, operand.getDistance().getFloatValue(), 0);
        assertEquals(-40.343666f, operand.getLattitude().getFloatValue(), 0);
        assertEquals(175.630917f, operand.getLongitude().getFloatValue(), 0);
    }

    @Test
    public void selectGeoWithInt() throws RecognitionException {
        String queryString = "select * where a within 1 of -40.343666, 175.630917";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        WithinOperand operand = (WithinOperand) query.getRootOperand();

        assertEquals("a", operand.getProperty().getValue());
        assertEquals(1, operand.getDistance().getFloatValue(), 0);
        assertEquals(-40.343666f, operand.getLattitude().getFloatValue(), 0);
        assertEquals(175.630917f, operand.getLongitude().getFloatValue(), 0);
    }

    @Test
    public void selectDistance() throws RecognitionException {
        String queryString = "select * where a contains 'foo'";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        ContainsOperand operand = (ContainsOperand) query.getRootOperand();

        assertEquals("a", operand.getProperty().getValue());
        assertEquals("foo", operand.getString().getValue());

    }

    @Test
    public void selectField() throws RecognitionException {

        String queryString = "select c where a = 1 or b > 2";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Set<String> identifiers = query.getSelectSubjects();

        assertTrue(identifiers.contains("c"));
    }

    @Test
    public void selectRename() throws RecognitionException {

        String queryString = "select {source:target} where a = 1 or b > 2";

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;

        Map<String, String> identifiers = query.getSelectAssignments();

        assertEquals("target", identifiers.get("source"));
    }
    
    @Test
    public void containsOr() throws Exception{
        String queryString = "select * where keywords contains 'hot' or title contains 'hot'";
        

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;
        
        OrOperand rootNode = (OrOperand) query.getRootOperand();
        
        assertNotNull(rootNode);
        
        ContainsOperand left = (ContainsOperand) rootNode.getLeft();
        
        assertEquals("keywords", left.getProperty().getValue());
        
        assertEquals("hot", left.getString().getValue());
        assertEquals("hot", left.getString().getEndValue());
        
        ContainsOperand right = (ContainsOperand) rootNode.getRight();
        
        assertEquals("title", right.getProperty().getValue());
        
        assertEquals("hot", right.getString().getValue());
        assertEquals("hot", right.getString().getEndValue());
        
        
        
        
    }

    
    @Test
    public void stringLower() throws Exception{
        String queryString = "select * where  title = 'Hot'";
        

        ANTLRStringStream in = new ANTLRStringStream(queryString);
        QueryFilterLexer lexer = new QueryFilterLexer(in);
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        QueryFilterParser parser = new QueryFilterParser(tokens);

        Query query = parser.ql().query;
        
        Equal rootNode = (Equal) query.getRootOperand();
        
        assertEquals("title", rootNode.getProperty().getValue());
        assertEquals("hot", ((StringLiteral)rootNode.getLiteral()).getValue());
        
        
    }
}
