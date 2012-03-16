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
package org.usergrid.persistence.query;

import static org.junit.Assert.*;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.junit.Test;
import org.usergrid.persistence.Query;

/**
 * @author apigee
 *
 */
public class GrammerTreeTest {

  /**
   * Simple test that constructs and AST from the ANTLR generated files
   * @throws RecognitionException 
   */
  @Test
  public void syntaxTree() throws RecognitionException {
    
    String query  = "select * where a = 5";
    
    ANTLRStringStream in = new ANTLRStringStream(query);
    QueryFilterLexer lexer = new QueryFilterLexer(in);
    TokenRewriteStream tokens = new TokenRewriteStream(lexer);
    QueryFilterParser parser = new QueryFilterParser(tokens);
    
    Object tree = parser.ql().tree;
    
    System.out.println("tree");
    
  }

}
