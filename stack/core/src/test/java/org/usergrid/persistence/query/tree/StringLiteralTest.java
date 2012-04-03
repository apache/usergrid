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

import org.antlr.runtime.CommonToken;
import org.junit.Test;

/**
 * @author tnine
 *
 */
public class StringLiteralTest {

    @Test
    public void exactToken() {
        
      StringLiteral literal = new StringLiteral(new CommonToken(0,  "'value'"));
      
      assertEquals("value", literal.getValue());
      assertEquals("value", literal.getEndValue());
      
      
    }
    
    @Test
    public void exactString() {
        
      StringLiteral literal = new StringLiteral("value");
      
      assertEquals("value", literal.getValue());
      assertEquals("value", literal.getEndValue());
      
      
    }
    
    
    @Test
    public void wildcardToken() {
        
      StringLiteral literal = new StringLiteral(new CommonToken(0,  "'*'"));
      
      assertNull(literal.getValue());
      assertNull(literal.getEndValue());
      
      
    }
    
    @Test
    public void wildcardString() {
        
      StringLiteral literal = new StringLiteral("*");
      
      assertNull(literal.getValue());
      assertNull(literal.getEndValue());
      
      
      
      
    }
    

    @Test
    public void wildcardEndToken() {
        
      StringLiteral literal = new StringLiteral(new CommonToken(0,  "'value*'"));
      
      assertEquals("value", literal.getValue());
      assertEquals("value\uffff", literal.getEndValue());
    
      
      
    }
    
    @Test
    public void wildcardEndString() {
        
      StringLiteral literal = new StringLiteral("value*");
     
      assertEquals("value", literal.getValue());
      assertEquals("value\uffff", literal.getEndValue());
      
      
      
    }

}
